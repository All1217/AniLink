# Proposal

## Why

`AniLink-AlgorithmEngine` 的启动日志显示 Flask、MySQL、Redis、RabbitMQ、ES 客户端都已初始化成功，但画像构建持续抛出 `Duplicate entry '<uid>-<tag>' for key 'user_tag.uk_uid_tag'`。根因不是中间件断连，而是 `saveToDB` 的 `DELETE` + `INSERT` 在并发画像构建下不幂等，MQ 消费失败后又 `requeue=True` 无限重投，形成持续刷屏；同时引擎没有 `/health` 或联通性自检，难以快速区分“依赖不可用”和“写入冲突”。

## What Changes

- `user_tag` 持久化改为幂等 upsert，并按 uid 串行化：并发画像构建不报唯一键，也不会出现“标签已删、插入失败”导致的空画像。
- 画像消息失败改为有界重试，超过上限进入死信队列，不再无限 `requeue`；ack/nack 与重投回到消费者线程执行，避免 pika 连接跨线程使用。
- 新增 `GET /health`：检查 MySQL、Redis、RabbitMQ、Elasticsearch 与后端网关，返回逐项状态、耗时与错误摘要，超时可控。
- 新增可重复执行的联通性检查脚本：覆盖引擎自身接口、后端网关与四类中间件。
- 更新引擎 README 与问题日志，记录启动方式、健康检查与本次根因。
- 不改变推荐/画像算法、不调整数据库 schema、不改后端业务代码；后端仅作为健康检查目标。

## Capabilities

### New Capabilities

- `engine-profile-save`：画像标签写入的幂等、并发安全与失败原子性。
- `engine-message-retry`：画像消息的有界重试、死信投递与 ack/nack 线程约束。
- `engine-health`：`/health` 的组件检查、超时与 HTTP 状态契约。

### Modified Capabilities

无（仓库此前没有 OpenSpec 能力规格）。

## Impact

- 代码：`src/util/profileBuilder.py`、`src/util/rabbitmq.py`、`src/server/app.py`、`src/config/application.py`；可能新增 `src/server/health.py` 与 `scripts/` 下的检查脚本。
- 中间件：RabbitMQ 新增 `userProfile.dlx` / `userProfile.dlq` 声明；MySQL `user_tag` 写入语义由“删后插”改为“upsert + 清理旧标签”；Redis/ES 仅探活。
- 后端：`/health` 调用网关 `/main/video/public/pageVideo`（公开接口），不需要改 AL-web/AL-gateway。
- 运维：启动后可用 `/health` 自检；失败消息可在 DLQ 查看，不再刷屏。
- 兼容性：`/recommendations`、`/userProfile`、`/findSimilar` 的路径与响应结构不变。
