# Tasks

## 1. 修复画像标签保存

- [x] 1.1 在 `src/util/profileBuilder.py` 增加按 uid 的条带锁（64 个 `threading.Lock`），把 `saveToDB` 改为显式事务内的 `INSERT ... ON DUPLICATE KEY UPDATE` + 清理旧标签；验证：`python -m unittest test.test_profile_save` 通过
- [x] 1.2 新增 `test/test_profile_save.py`，用 fake connection/cursor 断言 SQL 含 upsert、旧标签清理、空集合清空，并用两个线程验证同一 uid 不交错；验证：测试通过且故意去掉锁时并发用例失败
- [x] 1.3 在 `src/util/profileBuilder.py` 的保存异常日志中补充 uid 与当前标签数量（不打印口令）；验证：单元测试覆盖异常分支，日志输出可读

## 2. 画像消息有界重试与死信

- [x] 2.1 在 `src/util/rabbitmq.py` 声明 `userProfile.dlx`、`userProfile.dlq` 与带死信参数的 `userProfile.queue.v2`，绑定 `userProfile.exchange`；验证：启动引擎后 RabbitMQ 管理端能看到新交换机、队列与绑定
- [x] 2.2 抽出重试决策函数（`x-retry` 上限 3），消费失败时重新发布到原队列或死信交换机；验证：`python -m unittest test.test_message_retry` 覆盖 0→1→2→3 与超限分支
- [x] 2.3 把 ack/nack/publish 改为 `connection.add_callback_threadsafe(...)` 回调，工作线程不直接操作 pika channel；验证：单元测试断言回调路径，手工发送一条失败消息确认不抛线程异常
- [x] 2.4 新增 `test/test_message_retry.py`，覆盖重试计数、达到上限进死信、重复投递幂等；验证：测试通过

## 3. 健康检查与联通性脚本

- [x] 3.1 新增 `src/server/health.py`，实现 MySQL/Redis/RabbitMQ/ES/后端网关五项并行探活（单项超时 2 秒）与 `ok/degraded/down` 映射；验证：`python -m unittest test.test_health` 通过
- [x] 3.2 在 `src/server/app.py` 注册 `GET /health`，在 `src/config/application.py` 增加 `BACKEND_GATEWAY_URL`（默认 `http://localhost:8090`，环境变量可覆盖）；验证：本地启动后 `curl http://localhost:5000/health` 返回五项状态
- [x] 3.3 新增 `test/test_health.py`，用 Flask test client + monkeypatch 覆盖全 ok、ES/后端失败 degraded、MySQL/Redis/MQ 失败 down 三种情况；验证：测试通过
- [x] 3.4 新增 `scripts/check_connectivity.py`，请求 `/health` 并打印组件状态表；验证：引擎运行时脚本输出五项状态，退出码与整体状态一致
- [x] 3.5 在引擎 `README.md` 补充 `/health` 用法、状态码含义与脚本命令；验证：按 README 命令执行成功

## 4. 集成验证与问题记录

- [x] 4.1 依赖全部启动后运行引擎，确认 Flask 5000、RabbitMQ 监听、`/health` 全绿；验证：`/health` 返回 200 且 `status=ok`
- [x] 4.2 对同一 uid 并发调用两次 `/userProfile/<uid>`，再连发两条同 uid MQ 消息，确认无 `Duplicate entry`、`user_tag` 只有一套；验证：日志无 1062，SQL 查询每个 `(uid, tag_name)` 唯一
- [x] 4.3 发送一条无效消息模拟消费失败（不停共享 MySQL），确认最多重试 3 次后进入 `userProfile.dlq`，原队列不再堆积重投；验证：DLQ 消息头 `x-retry=3` 且含 `x-error`
- [x] 4.4 回归 `/recommendations/<uid>`、`/findSimilar/<vid>/<uid>`、`/userProfile/<uid>`，确认路径与响应结构未变；验证：三个接口均返回 200/原有 JSON 结构
- [x] 4.5 在 `AniLink-AlgorithmEngine/Assets/docs/问题日志.md` 记录根因（并发删插 + 保存异常被吞导致 MQ 不重试）、修复与验证命令；验证：文档包含本次日志中的 1062 错误与 `/health` 用法

## Workflow follow-up

- 评审通过后归档本变更；归档前确认 `/health` 与并发画像验证结果已记录。
- 后端登录/刷新重复发送画像消息的去重/节流作为独立变更，不在本次范围内。
