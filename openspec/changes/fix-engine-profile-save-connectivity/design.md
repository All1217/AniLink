# Design

## Context

- `profileBuilder.saveToDB` 先 `DELETE FROM user_tag WHERE uid=%s`，再 `executemany` 插入；MySQL 连接池 `autocommit=True`，每条语句立即提交。
- `buildOne` 会被 RabbitMQ 监听器（`RABBIT_MAX_WORKERS=5`）、`/userProfile`、`/userProfile/<uid>/async` 与批量构建同时调用；同一 uid 可能并发进入 `saveToDB`。
- `rabbitmq.py` 消费失败时 `basic_nack(requeue=True)`，失败消息会无限回到 `userProfile.queue1`；工作线程还直接调用 pika channel 的 ack/nack，存在线程安全风险。
- 服务目前没有 `/health`；ES 客户端默认超时 30 秒，不能直接用于健康检查。
- 日志中 Flask、MySQL、Redis、RabbitMQ 均已连接成功，唯一错误是 `user_tag.uk_uid_tag` 重复键。

## Goals / Non-Goals

**Goals:**

- 同一 uid 的标签保存幂等、串行、失败不丢旧标签。
- 画像消息失败有界重试并进入死信，不再无限刷屏。
- `/health` 在 2 秒级别返回各依赖与后端网关状态，可区分 down/degraded。
- 不改变推荐、画像算法与后端业务代码。

**Non-Goals:**

- 不重构推荐/画像计算逻辑，不改 `user_tag` 表结构与唯一键。
- 不解决后端每次登录/刷新都发送画像消息的重复问题（另立变更）。
- 不支持多引擎实例的分布式写锁（当前单进程部署；若多实例，另用 MySQL `GET_LOCK` 或队列分区）。

## Decisions

### D1 标签保存：事务内 upsert + 清理旧标签 + 按 uid 串行

- 用 64 个 `threading.Lock` 做条带锁，按 `hash(uid) % 64` 选择；同一 uid 串行，不同 uid 仍可并发。
- 在一个显式事务内执行：
  1. 对每个标签 `INSERT ... ON DUPLICATE KEY UPDATE weight=VALUES(weight), updated_date=CURRENT_TIMESTAMP`；
  2. `DELETE FROM user_tag WHERE uid=%s AND tag_name NOT IN (...)` 清理旧标签；
  3. 新标签为空时直接 `DELETE FROM user_tag WHERE uid=%s`。
- 关闭该连接的 autocommit（`conn.begin()` / `commit()` / `rollback()`），避免中途失败留下“删了一半”的状态。
- 备选：只加锁、保留 DELETE+INSERT。能解决并发，但多实例或进程崩溃仍可能空标签；upsert 更稳。
- 备选：改表去掉唯一键。会破坏“每用户每标签一条”的约束，否决。
- 备选：MySQL `GET_LOCK`。支持多实例，但增加连接占用与超时处理，当前单进程不需要。

### D2 MQ：有界重试 + 死信 + 线程安全确认

- 新增死信交换机 `userProfile.dlx`（direct）与队列 `userProfile.dlq`，绑定 routing key `userProfile.dead`。
- 新队列名 `userProfile.queue.v2`，声明参数 `x-dead-letter-exchange=userProfile.dlx`、`x-dead-letter-routing-key=userProfile.dead`；旧 `userProfile.queue1` 保留用于排空，不直接改参数（RabbitMQ 对已存在队列的参数变更会 `PRECONDITION_FAILED`）。
- 消息头 `x-retry` 记录已重试次数（默认上限 3）。消费失败时：
  - `x-retry < 3`：重新发布到 `userProfile.exchange` / `userProfile`，头 `x-retry+1`，然后 ack 原消息；
  - 达到上限：发布到 `userProfile.dlx` / `userProfile.dead`，保留原消息体与 `x-error`，然后 ack 原消息。
- 所有 ack/nack/publish 通过 `connection.add_callback_threadsafe(...)` 回到消费者线程执行；工作线程只负责构建画像。
- 备选：`basic_nack(requeue=False)` 直接进 DLQ。实现更短，但瞬时故障没有重试机会。
- 备选：完全同步消费。避免线程安全问题，但 LLM/DB 构建会阻塞消费者，吞吐明显下降，否决。

### D3 `/health`：并行探活 + 超时 + 状态映射

- 在 Flask 应用注册 `GET /health`，响应：
  ```json
  {
    "status": "ok|degraded|down",
    "components": {
      "mysql":    {"status": "ok", "latency_ms": 3},
      "redis":    {"status": "ok", "latency_ms": 1},
      "rabbitmq": {"status": "ok", "latency_ms": 0},
      "es":       {"status": "ok", "latency_ms": 5},
      "backend":  {"status": "ok", "latency_ms": 12}
    },
    "timestamp": "2026-11-25T21:30:00+08:00"
  }
  ```
- 每项检查放在线程池并行执行，单项超时 2 秒；MySQL 用 `SELECT 1`，Redis 用 `PING`，RabbitMQ 看 `connection.is_open && channel.is_open`，ES 用独立短超时客户端 `ping`，后端用 `requests.get` 访问 `BACKEND_GATEWAY_URL + /main/video/public/pageVideo?current=1&size=1`。
- 状态映射：MySQL/Redis/RabbitMQ 任一失败 → `down` + HTTP 503；仅 ES/后端失败 → `degraded` + HTTP 200；全部成功 → `ok` + HTTP 200。
- 新增 `BACKEND_GATEWAY_URL` 配置（默认 `http://localhost:8090`，可用环境变量覆盖）。
- 备选：只返回一个布尔值。无法定位是哪一项挂了，否决。
- 备选：健康检查里调用 `/userProfile` 触发真实构建。有写副作用且慢，否决。

### D4 测试与观测

- `test/test_profile_save.py`：用 fake connection/cursor 捕获 SQL，断言包含 `ON DUPLICATE KEY UPDATE` 与清理旧标签；用两个线程调用同一 uid，断言锁使 SQL 不交错。
- `test/test_message_retry.py`：把“重试/进死信”的决策抽成纯函数，断言 `x-retry` 0→1→2→3 与达到上限后的动作。
- `test/test_health.py`：Flask `test_client` + monkeypatch 检查函数，覆盖 ok/degraded/down 三种状态码。
- `scripts/check_connectivity.py`：请求 `/health` 并以表格打印各组件状态，供启动后手工自检。
- 日志：重试与进入死信时打印 uid、retry 次数与简短原因；不打印口令。

## Risks / Trade-offs

- **条带锁让不同 uid 偶尔共享一把锁** → 64 个锁足够覆盖当前规模；写库耗时可接受。
- **多实例仍可能竞态** → 当前单进程；多实例前改用 MySQL 命名锁或按 uid 分区消费。
- **队列参数变更失败** → 使用新队列 `userProfile.queue.v2`，旧队列保留排空，不原地改参数。
- **健康检查线程超时后仍占用线程** → 各客户端都设置连接/读取超时；线程池最多 5 个，可接受。
- **DLQ 需要 RabbitMQ 权限** → 启动前确认账号可声明 exchange/queue；失败时记录日志并让原消息回到队列。
- **`requests` 访问后端可能受代理影响** → 健康检查不使用系统代理（`trust_env=False`），只访问配置的网关地址。

## Migration Plan

1. 停止 AlgorithmEngine，确认 `userProfile.queue1` 已排空（避免切换时丢消息）。
2. 部署新代码；启动时声明 `userProfile.dlx`、`userProfile.dlq`、`userProfile.queue.v2` 并绑定。
3. 调用 `/health`，确认 MySQL/Redis/RabbitMQ/ES/后端均为 ok；若不是，先修依赖。
4. 用同一 uid 连续调用两次 `/userProfile/<uid>`，确认无 `Duplicate entry`、标签只保留一套。
5. 人为制造一次消费失败（如临时停 MySQL），确认最多重试 3 次后进入 `userProfile.dlq`，不再刷屏。
6. 回滚：恢复旧代码与旧队列绑定；`user_tag` 写入语义向后兼容，无需回滚数据。

## Open Questions

- 后端是否应在登录/刷新时对画像消息做去重或节流？当前 upsert 已保证幂等，重复消息只增加计算量；可作为后续独立优化。
- 未来引擎多实例部署时，是否改用 MySQL `GET_LOCK` 或按 uid 分区队列？本变更不处理。
