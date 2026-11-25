# _*_ coding : utf-8 _*_
# /health 契约测试：用 Flask test client + monkeypatch，不访问真实中间件。

import unittest
from unittest.mock import Mock, patch

from flask import Flask

import src.server.health as health


class HealthRouteTest(unittest.TestCase):
    def _client(self):
        app = Flask(__name__)
        health.register_health_route(app, lambda: None)
        return app.test_client()

    def _patches(self, mysql=None, redis=None, rabbit=None, es=None, backend=None):
        return patch.multiple(
            health,
            _check_mysql=mysql or Mock(),
            _check_redis=redis or Mock(),
            _make_rabbit_check=rabbit or Mock(return_value=lambda: None),
            _check_es=es or Mock(),
            _check_backend=backend or Mock(),
        )

    def test_all_components_ok(self):
        with self._patches():
            response = self._client().get("/health")

        body = response.get_json()
        self.assertEqual(200, response.status_code)
        self.assertEqual("ok", body["status"])
        self.assertEqual({"mysql", "redis", "rabbitmq", "es", "backend"}, set(body["components"]))
        self.assertTrue(all(item["status"] == "ok" for item in body["components"].values()))

    def test_core_dependency_down_returns_503(self):
        with self._patches(mysql=Mock(side_effect=RuntimeError("mysql down"))):
            response = self._client().get("/health")

        body = response.get_json()
        self.assertEqual(503, response.status_code)
        self.assertEqual("down", body["status"])
        self.assertEqual("down", body["components"]["mysql"]["status"])

    def test_non_core_dependency_down_is_degraded(self):
        with self._patches(es=Mock(side_effect=RuntimeError("es down"))):
            response = self._client().get("/health")

        body = response.get_json()
        self.assertEqual(200, response.status_code)
        self.assertEqual("degraded", body["status"])
        self.assertEqual("down", body["components"]["es"]["status"])

    def test_errors_are_redacted(self):
        with self._patches(redis=Mock(side_effect=RuntimeError("password=secret"))):
            response = self._client().get("/health")

        self.assertNotIn("secret", response.get_data(as_text=True))


if __name__ == "__main__":
    unittest.main()
