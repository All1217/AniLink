# _*_ coding : utf-8 _*_
# 画像消息重试决策测试：纯函数，不依赖 RabbitMQ。

import unittest

from src.util.retryPolicy import decide_retry
from src.util.rabbitmq import (
    RabbitMQListener,
    PROFILE_EXCHANGE,
    PROFILE_QUEUE,
    PROFILE_DLX,
    PROFILE_DLQ,
    PROFILE_DEAD_ROUTING_KEY,
)


class FakeChannel:
    def __init__(self):
        self.exchanges = []
        self.queues = []
        self.bindings = []
        self.published = []
        self.acks = []
        self.nacks = []

    def exchange_declare(self, **kwargs):
        self.exchanges.append(kwargs)

    def queue_declare(self, **kwargs):
        self.queues.append(kwargs)

    def queue_bind(self, **kwargs):
        self.bindings.append(kwargs)

    def basic_publish(self, **kwargs):
        self.published.append(kwargs)

    def basic_ack(self, **kwargs):
        self.acks.append(kwargs)

    def basic_nack(self, **kwargs):
        self.nacks.append(kwargs)


class FakeConnection:
    is_open = True

    def __init__(self):
        self.callbacks = []

    def add_callback_threadsafe(self, callback):
        self.callbacks.append(callback)

    def drain(self):
        while self.callbacks:
            self.callbacks.pop(0)()


class FakeMethod:
    delivery_tag = 7


class FakeProperties:
    def __init__(self, headers=None):
        self.headers = headers or {}


class RetryPolicyTest(unittest.TestCase):
    def test_retry_before_limit(self):
        self.assertEqual(("retry", 1), decide_retry(0, 3))
        self.assertEqual(("retry", 2), decide_retry(1, 3))
        self.assertEqual(("retry", 3), decide_retry(2, 3))

    def test_dead_at_limit(self):
        self.assertEqual(("dead", 3), decide_retry(3, 3))
        self.assertEqual(("dead", 4), decide_retry(4, 3))

    def test_accepts_string_header(self):
        self.assertEqual(("retry", 1), decide_retry("0", 3))
        self.assertEqual(("dead", 3), decide_retry("3", 3))

    def test_invalid_header_treated_as_zero(self):
        self.assertEqual(("retry", 1), decide_retry(None, 3))
        self.assertEqual(("retry", 1), decide_retry("bad", 3))

    def test_topology_declares_dlx_dlq_and_v2_queue(self):
        listener = RabbitMQListener(prefetch_count=1, max_workers=1)
        listener.channel = FakeChannel()

        listener._declare_topology()

        exchanges = {item["exchange"] for item in listener.channel.exchanges}
        self.assertIn(PROFILE_EXCHANGE, exchanges)
        self.assertIn(PROFILE_DLX, exchanges)

        queues = {item["queue"]: item for item in listener.channel.queues}
        self.assertIn(PROFILE_DLQ, queues)
        self.assertIn(PROFILE_QUEUE, queues)
        self.assertEqual(
            PROFILE_DLX,
            queues[PROFILE_QUEUE]["arguments"]["x-dead-letter-exchange"],
        )
        self.assertEqual(
            PROFILE_DEAD_ROUTING_KEY,
            queues[PROFILE_QUEUE]["arguments"]["x-dead-letter-routing-key"],
        )

    def test_failure_is_rescheduled_to_consumer_thread_and_retried(self):
        listener = RabbitMQListener(prefetch_count=1, max_workers=1)
        listener.connection = FakeConnection()
        channel = FakeChannel()

        listener._schedule(
            lambda: listener._handle_failure(
                channel,
                FakeMethod(),
                FakeProperties(),
                b'{"uid": 1}',
                1,
                RuntimeError("boom"),
            )
        )
        listener.connection.drain()

        self.assertEqual(PROFILE_EXCHANGE, channel.published[0]["exchange"])
        self.assertEqual(1, channel.published[0]["properties"].headers["x-retry"])
        self.assertEqual([{"delivery_tag": 7}], channel.acks)

    def test_failure_after_limit_goes_to_dead_letter(self):
        listener = RabbitMQListener(prefetch_count=1, max_workers=1)
        listener.connection = FakeConnection()
        channel = FakeChannel()

        listener._schedule(
            lambda: listener._handle_failure(
                channel,
                FakeMethod(),
                FakeProperties({"x-retry": 3}),
                b'{"uid": 1}',
                1,
                RuntimeError("permanent"),
            )
        )
        listener.connection.drain()

        self.assertEqual(PROFILE_DLX, channel.published[0]["exchange"])
        self.assertIn("x-error", channel.published[0]["properties"].headers)
        self.assertEqual([{"delivery_tag": 7}], channel.acks)


if __name__ == "__main__":
    unittest.main()
