# _*_ coding : utf-8 _*_
# 只读健康检查：MySQL / Redis / RabbitMQ / Elasticsearch / 后端网关。

import datetime
import re
import time
from concurrent.futures import ThreadPoolExecutor, TimeoutError as FuturesTimeoutError

import requests
from flask import jsonify

import src.config.application as config
from src.util.database import getES, get_mysql_conn, get_redis_client

CHECK_TIMEOUT_SECONDS = 2
CORE_COMPONENTS = ("mysql", "redis", "rabbitmq")
OTHER_COMPONENTS = ("es", "backend")


def _safe_error(error):
    """只保留简短原因，并去掉可能出现的口令片段。"""
    message = f"{type(error).__name__}: {error}"
    message = re.sub(r"(?i)(password|passwd|pwd)=[^,\s;]+", r"\1=***", message)
    message = re.sub(r"://[^:/\s]+:[^@/\s]+@", "://***@", message)
    return message[:160]


def _check_mysql():
    conn = get_mysql_conn()
    try:
        cursor = conn.cursor()
        try:
            cursor.execute("SELECT 1")
            cursor.fetchone()
        finally:
            cursor.close()
    finally:
        conn.close()


def _check_redis():
    get_redis_client().ping()


def _make_rabbit_check(listener):
    def _check():
        if listener is None or listener.connection is None or listener.channel is None:
            raise RuntimeError("RabbitMQ listener is not connected")
        if not listener.connection.is_open or not listener.channel.is_open:
            raise RuntimeError("RabbitMQ connection or channel is closed")

    return _check


def _check_es():
    getES().options(request_timeout=CHECK_TIMEOUT_SECONDS).ping()


def _check_backend():
    session = requests.Session()
    session.trust_env = False
    try:
        response = session.get(
            f"{config.BACKEND_GATEWAY_URL.rstrip('/')}/main/video/public/pageVideo",
            params={"current": 1, "size": 1},
            timeout=CHECK_TIMEOUT_SECONDS,
        )
        if response.status_code != 200:
            raise RuntimeError(f"HTTP {response.status_code}")
    finally:
        session.close()


def _run_check(name, check):
    started = time.perf_counter()
    try:
        check()
        return name, {
            "status": "ok",
            "latency_ms": round((time.perf_counter() - started) * 1000),
        }
    except Exception as error:
        return name, {
            "status": "down",
            "latency_ms": round((time.perf_counter() - started) * 1000),
            "error": _safe_error(error),
        }


def check_health(listener=None):
    """返回 (payload, http_status)；不做任何写操作。"""
    checks = {
        "mysql": _check_mysql,
        "redis": _check_redis,
        "rabbitmq": _make_rabbit_check(listener),
        "es": _check_es,
        "backend": _check_backend,
    }

    components = {}
    executor = ThreadPoolExecutor(max_workers=len(checks), thread_name_prefix="health")
    try:
        futures = {
            name: executor.submit(_run_check, name, check)
            for name, check in checks.items()
        }
        for name, future in futures.items():
            try:
                _, result = future.result(timeout=CHECK_TIMEOUT_SECONDS)
            except FuturesTimeoutError:
                result = {
                    "status": "down",
                    "latency_ms": CHECK_TIMEOUT_SECONDS * 1000,
                    "error": "timeout",
                }
            except Exception as error:
                result = {
                    "status": "down",
                    "latency_ms": 0,
                    "error": _safe_error(error),
                }
            components[name] = result
    finally:
        executor.shutdown(wait=False)

    core_ok = all(components[name]["status"] == "ok" for name in CORE_COMPONENTS)
    other_ok = all(components[name]["status"] == "ok" for name in OTHER_COMPONENTS)
    if not core_ok:
        overall, http_status = "down", 503
    elif not other_ok:
        overall, http_status = "degraded", 200
    else:
        overall, http_status = "ok", 200

    payload = {
        "status": overall,
        "components": components,
        "timestamp": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
    }
    return payload, http_status


def register_health_route(app, listener_provider):
    """把 /health 注册到 Flask 应用；listener_provider 返回 RabbitMQListener 或 None。"""

    @app.route("/health", methods=["GET"])
    def health():
        listener = listener_provider() if callable(listener_provider) else listener_provider
        payload, http_status = check_health(listener)
        return jsonify(payload), http_status
