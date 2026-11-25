# _*_ coding : utf-8 _*_
# 引擎联通性自检：请求 /health 并打印各组件状态。
# 用法：python scripts/check_connectivity.py
# 环境变量：ENGINE_BASE_URL（默认 http://localhost:5000）

import os
import sys

import requests

BASE_URL = os.getenv("ENGINE_BASE_URL", "http://localhost:5000").rstrip("/")


def main():
    session = requests.Session()
    session.trust_env = False
    try:
        response = session.get(f"{BASE_URL}/health", timeout=8)
    except Exception as error:
        print(f"❌ 无法连接引擎 {BASE_URL}: {error}")
        return 2

    try:
        payload = response.json()
    except ValueError:
        print(f"❌ /health 返回非 JSON（HTTP {response.status_code}）")
        return 2

    components = payload.get("components", {})
    print(f"引擎: {BASE_URL}")
    print(f"整体: {payload.get('status', 'unknown')}  HTTP {response.status_code}")
    print(f"{'组件':<10}{'状态':<10}{'耗时(ms)':<10}错误")
    for name, item in components.items():
        print(
            f"{name:<10}{item.get('status', 'unknown'):<10}"
            f"{item.get('latency_ms', '-'):<10}{item.get('error', '')}"
        )

    if response.status_code == 200 and payload.get("status") != "down":
        return 0
    return 1


if __name__ == "__main__":
    sys.exit(main())
