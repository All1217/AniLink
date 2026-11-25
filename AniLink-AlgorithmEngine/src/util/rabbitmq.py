# _*_ coding : utf-8 _*_
# @Time : 2026/2/16
# @Author : Morton
# @File : rabbitmq.py
# @Project : algorithm-engine

import pika
import json
import traceback
import threading
from src.util.profileBuilder import buildOne
from src.util.retryPolicy import decide_retry
from concurrent.futures import ThreadPoolExecutor
import src.config.application as config


PROFILE_EXCHANGE = 'userProfile.exchange'
PROFILE_ROUTING_KEY = 'userProfile'
PROFILE_QUEUE = 'userProfile.queue.v2'
PROFILE_DLX = 'userProfile.dlx'
PROFILE_DLQ = 'userProfile.dlq'
PROFILE_DEAD_ROUTING_KEY = 'userProfile.dead'


class RabbitMQListener:
    # RabbitMQ监听器
    def __init__(self, prefetch_count, max_workers):
        self.config = {
            'username': config.RABBIT_USERNAME,
            'password': config.RABBIT_PASS,
            'host': config.RABBIT_HOST,
            'port': config.RABBIT_PORT,
            'virtual-host': config.RABBIT_VIRTUAL_HOST
        }
        self.prefetch_count = prefetch_count
        self.connection = None
        self.channel = None
        self.should_stop = False
        self.thread = None
        self.executor = ThreadPoolExecutor(max_workers=max_workers)
        self.active_tasks = 0
        self.task_lock = threading.Lock()

    def connect(self):
        """建立RabbitMQ连接"""
        try:
            credentials = pika.PlainCredentials(self.config['username'], self.config['password'])
            parameters = pika.ConnectionParameters(
                host=self.config['host'],
                port=self.config['port'],
                virtual_host=self.config['virtual-host'],
                credentials=credentials,
                heartbeat=60,
                connection_attempts=3,
                retry_delay=5,
                socket_timeout=10,
                blocked_connection_timeout=300,
            )
            self.connection = pika.BlockingConnection(parameters)
            self.channel = self.connection.channel()
            self.channel.basic_qos(prefetch_count=self.prefetch_count)
            print(f"✅ RabbitMQ连接成功")
            return True
        except Exception as e:
            print(f"❌ RabbitMQ连接失败: {e}")
            return False

    def processerAsync(self, ch, method, properties, body):
        """
        异步处理任务（在线程池中执行）；ack/nack/publish 回到消费者线程执行。
        """

        def task():
            uid = None
            try:
                # 解析消息
                if isinstance(body, bytes):
                    message = json.loads(body.decode('utf-8'))
                else:
                    message = json.loads(body)
                uid = message.get('uid')
                # 执行画像构建
                buildOne(uid, save_to_db=True)
                # 成功确认
                self._schedule(lambda: ch.basic_ack(delivery_tag=method.delivery_tag))
            except Exception as e:
                print(f"❌ 处理用户 {uid if uid is not None else 'unknown'} 失败: {e}")
                traceback.print_exc()
                error = e
                self._schedule(
                    lambda err=error: self._handle_failure(ch, method, properties, body, uid, err)
                )
            finally:
                with self.task_lock:
                    self.active_tasks -= 1

        # 更新活跃任务计数
        with self.task_lock:
            self.active_tasks += 1
        # 提交到线程池
        self.executor.submit(task)

    def _schedule(self, callback):
        """把 ack/nack/publish 调度回消费者线程，避免工作线程直接操作 pika channel。"""
        try:
            if self.connection is None or not self.connection.is_open:
                raise RuntimeError("RabbitMQ connection is closed")
            self.connection.add_callback_threadsafe(callback)
        except Exception as e:
            print(f"❌ 无法调度 RabbitMQ 回调: {e}")

    def _publish(self, ch, exchange, routing_key, body, headers):
        ch.basic_publish(
            exchange=exchange,
            routing_key=routing_key,
            body=body,
            properties=pika.BasicProperties(
                headers=headers,
                delivery_mode=2,
                content_type='application/json'
            )
        )

    def _handle_failure(self, ch, method, properties, body, uid, error):
        """有界重试；超过上限进入死信队列，然后确认原消息。"""
        headers = dict(getattr(properties, 'headers', None) or {})
        action, next_retry = decide_retry(headers.get('x-retry', 0), config.PROFILE_MAX_RETRY)
        headers['x-retry'] = next_retry
        try:
            if action == 'retry':
                self._publish(ch, PROFILE_EXCHANGE, PROFILE_ROUTING_KEY, body, headers)
                print(f"🔁 用户 {uid} 画像消息第 {next_retry} 次重试: {error}")
            else:
                headers['x-error'] = str(error)[:500]
                self._publish(ch, PROFILE_DLX, PROFILE_DEAD_ROUTING_KEY, body, headers)
                print(f"🪦 用户 {uid} 画像消息进入死信（已重试 {next_retry} 次）: {error}")
            ch.basic_ack(delivery_tag=method.delivery_tag)
        except Exception as publish_error:
            print(f"❌ 用户 {uid} 画像消息重投/死信失败: {publish_error}")
            try:
                # 队列已配置 DLX，nack 不重回原队列时由 RabbitMQ 死信
                ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
            except Exception as nack_error:
                print(f"❌ 用户 {uid} 消息 nack 失败: {nack_error}")

    def _declare_topology(self):
        """声明主交换机、死信交换机/队列与带死信参数的 v2 队列。"""
        self.channel.exchange_declare(exchange=PROFILE_EXCHANGE, exchange_type='direct', durable=True)
        self.channel.exchange_declare(exchange=PROFILE_DLX, exchange_type='direct', durable=True)
        self.channel.queue_declare(queue=PROFILE_DLQ, durable=True)
        self.channel.queue_bind(exchange=PROFILE_DLX, queue=PROFILE_DLQ,
                                routing_key=PROFILE_DEAD_ROUTING_KEY)
        # v2 队列带死信参数；旧 userProfile.queue1 需在迁移时解绑/删除，避免重复消费
        self.channel.queue_declare(
            queue=PROFILE_QUEUE,
            durable=True,
            arguments={
                'x-dead-letter-exchange': PROFILE_DLX,
                'x-dead-letter-routing-key': PROFILE_DEAD_ROUTING_KEY,
            }
        )
        self.channel.queue_bind(exchange=PROFILE_EXCHANGE, queue=PROFILE_QUEUE,
                                routing_key=PROFILE_ROUTING_KEY)

    def startListen(self):
        """开始监听队列"""
        if not self.connect():
            print("❌ 无法连接RabbitMQ")
            return
        try:
            # 声明主交换机、死信交换机/队列与 v2 队列
            self._declare_topology()
            # 设置消费者
            self.channel.basic_consume(queue=PROFILE_QUEUE, on_message_callback=self.processerAsync,
                                       auto_ack=False)
            print(f"RabbitMQ监听器已启动")
            self.channel.start_consuming()
        except Exception as e:
            print(f"❌ 监听过程出错: {e}")
        finally:
            self.stop()

    def start(self):
        """在后台线程中启动监听器"""
        self.thread = threading.Thread(target=self.startListen, daemon=True)
        self.thread.start()
        print("🔄 RabbitMQ监听器已在后台线程启动")

    def stop(self):
        """停止监听器"""
        self.should_stop = True
        # 关闭线程池
        self.executor.shutdown(wait=True, cancel_futures=False)
        print("🔄 线程池已关闭")

        # 关闭RabbitMQ连接
        if self.channel and self.channel.is_open:
            self.channel.stop_consuming()
        if self.connection and self.connection.is_open:
            self.connection.close()
        print("🛑 RabbitMQ监听器已停止")


# 全局单例
_rabbitmq_listener = None


def getRabbitmqListener(prefetch=10, maxWorkers=5):
    """获取RabbitMQ监听器单例"""
    global _rabbitmq_listener
    if _rabbitmq_listener is None:
        try:
            _rabbitmq_listener = RabbitMQListener(config.RABBIT_PREFETCH, config.RABBIT_MAX_WORKERS)
        except Exception as e:
            print(e)
            print('从配置读取prefetch和maxWorkers失败，尝试传入默认值')
            _rabbitmq_listener = RabbitMQListener(prefetch, maxWorkers)
    return _rabbitmq_listener


def startRabbitmq():
    """启动RabbitMQ监听器（后台线程）"""
    listener = getRabbitmqListener()
    listener.start()
    return listener


def stopRabbitmq():
    listener = getRabbitmqListener()
    listener.stop()
