# AniLink 后端微服务拆分设计

> 本文是 brainstorming 后确认的设计基线，供实施计划引用。变更跟踪、任务清单与增量规格以 `openspec/changes/refactor-backend-service-split/` 为准；本文负责解释“为什么这样拆、边界怎么定”。

## 1. 目标与范围

把 `AL-web` 单进程里的 11 个业务域按领域边界拆成 9 个可独立部署的业务服务 + 网关，保持对外 `/main/**` 契约不变，保留点赞与播放进度两条高并发链路，并用绞杀者方式逐域迁移。

不在本次范围：数据库 schema 拆分、依赖版本升级、推荐引擎（Flask）重写、前端页面重写、WebSocket 鉴权改造。

## 2. 服务目录

| 服务 | 端口 | 职责 | 拥有的表 |
| --- | --- | --- | --- |
| al-gateway | 8090 | 路由、access token 校验、身份透传 | — |
| al-user | 8025 | 登录/注册/刷新/登出/改密、资料、统计、标签 | `user`、`user_stats`、`user_tag` |
| al-video | 8026 | 视频元数据、投稿上传、HLS 转码、统计、投币 | `video`、`video_stats`、`user_video` |
| al-playback | 8027 | 观看历史、播放进度 | `bh_video` |
| al-social | 8031 | 关注/粉丝、收藏夹 | `follow`、`favorite`、`favorite_video` |
| al-comment | 8028 | 评论与回复 | `comment` |
| al-danmaku | 8029 / WS 8030 | 弹幕 HTTP、视频房间 WebSocket | `danmu`、`sub_danmu` |
| al-datacenter | 8032 | 统计、推荐、标签聚合 BFF | — |
| al-remark | 8021 | 点赞 | —（Redis） |
| al-communication | 8023 / WS 8024 | 聊天室 HTTP、私信 WebSocket | `chat`、`chat_message` |
| AL-web（迁移期） | 8022 | 未迁移域兜底，Phase 4 后删除 | 迁移中的表 |

## 3. 模块与包约定

- 服务实现包：`com.video.<service>`（如 `com.video.user`、`com.video.video`）；实体、Mapper、Service 都在本服务内。
- 契约模块包：`com.video.api.<service>`（如 `com.video.api.user`），只放 Feign 客户端与跨服务 DTO，不放实体。
- Phase 0 只保留 5 个公共模块；服务模块与契约模块随各自 Phase 创建，避免空脚手架被误读为已存在服务。
- 公共包保持 `com.video.common.*`，按模块拆分后包名不变，避免全仓 import 改写。
- 服务启动类使用 `@SpringBootApplication(scanBasePackages = "com.video")`；Mapper 扫描由各服务自己的 `@MapperScan("com.video.<service>.mapper")` 声明，共享的 MyBatis 配置不再写死 `com.video.web.*.mapper`。
- 服务之间只依赖对方的 `-api` 契约模块，不依赖实现模块。ArchUnit 规则据此检查。

## 4. 读模型与 BFF

- 前端继续直连各域，`/main/**` 路径不变；`al-datacenter` 只承担统计、推荐、标签等跨域聚合，不做全量 BFF。
- 跨域读统一“本域查询 → 批量 Feign → Caffeine 60s 本地缓存”。用户服务不可用时返回缓存旧值或空值，列表接口不得 500。
- 用户资料是最高频跨域数据：`al-user` 提供 `POST /internal/v1/users/batch`，调用方按 id 集合批量取，禁止循环单查。
- 视频信息同理：`al-video` 提供 `POST /internal/v1/videos/batch`。
- 数据中心不再 JOIN：`al-video` 暴露按 UP 主与时间范围的日汇总，`al-social` 暴露粉丝 id 集合，`al-datacenter` 组合输出，保持 `DataResVo` 结构。

## 5. 数据归属与跨域写

- 一表一主，见第 2 节；非属主服务不得出现该表的 SQL、Mapper 或实体。
- 一阶段共享 `video` 库，归属是逻辑约束；二阶段按服务拆 schema 时只改数据源与 SQL，不改契约。
- 点赞事件沿用 `like.record.topic` / `{bizType}.times.changed`，消息体为绝对计数，`al-video` 消费后批量更新 `video_stats.good`。
- 收藏事件新增 `social.favorite.topic` / `favorite.changed`，消息体 `{eventId, uid, vid, collected, delta, occurredAt}`；`al-video` 以 `eventId` 去重，更新 `user_video.collect` 与 `video_stats.collect`。Phase 3 前保留 `UpdateStats` 对 `main:video:favorite:{vid}` 的扫描作为过渡。

## 6. 高并发链路

### 6.1 点赞（al-remark）

- 保持现有结构：`likes:set:biz:{bizId}` 去重、SCARD 计数、`likes:times:type:{bizType}` 缓存、每 20 秒 `popMin(30)`、MQ 批量回写。
- 不改变既有缓存 key，避免已有点赞状态丢失。
- 补故障补偿：`popMin` 取出的 tuple 在消息发送失败时按原 score `ZADD` 回 ZSet，下一周期重试；消息体保持绝对计数，消费幂等。

### 6.2 播放进度（al-playback）

用 Redis 持久化延迟队列替代 JVM `DelayQueue`：

| key | 类型 | 内容 | TTL |
| --- | --- | --- | --- |
| `video:record:{vid}` | Hash | uid → `{id, moment}`，读路径缓存 | 60s |
| `video:record:pending:{vid}` | Hash | uid → `{id, moment, commitTime}`，待落库载荷 | 24h |
| `video:record:delay` | ZSet | member `vid:uid`，score = 到期时间 | — |

上报流程：

1. `HSET video:record:pending:{vid} uid payload`；
2. `ZADD video:record:delay now+10s vid:uid`（同一 member 更新 score，天然合并）；
3. 同时更新读缓存 `video:record:{vid}`（TTL 60s）。

落库工作线程（每 1 秒，单批 ≤200）：

1. `ZRANGEBYSCORE video:record:delay 0 now LIMIT 0 200`；
2. 对每个 member：`HGET pending`；载荷为空则 `ZREM`；否则 `updateById(bh_video)`；
3. 更新成功后 `HDEL pending` + `ZREM delay`。

正确性：

- 同一 (uid, vid) 的多次上报只保留一个 pending 载荷与一个 ZSet member；
- 多实例可同时消费，重复更新写入相同值，幂等；
- 进程重启后 ZSet 与 pending 仍在，待落库任务不丢失；
- 删除历史时同时清理三处：`video:record:{vid}`、`video:record:pending:{vid}`、`video:record:delay` 中对应 member；
- pending 载荷 24 小时过期是已知边界，写入文档。

## 7. 鉴权与路由

- 网关 `AccountAuthFilter` 必须把 `exchange.mutate()...build()` 的返回值传给 `chain.filter(...)`，否则 `user-id` 与 `X-Real-IP` 不会真正注入；Phase 0 用单测固定该行为。
- 免登录路径模式集中到 `com.video.common.constant.AuthPaths`，网关与 `AL-auth-resource-sdk` 共用，避免两处漂移。
- 业务服务只信任 `user-id` 头；写操作不得以请求参数中的 uid 作为授权依据。
- 内部接口统一 `/internal/v1/**`，网关不声明路由，外部不可达。
- 路由表见 OpenSpec `design.md` 的 D2；播放进度新增 `/main/playback/record/add` 别名，旧路径至少保留一个发布周期。

## 8. 迁移与回滚

- Phase 0 基座 → Phase 1 al-user → Phase 2 al-video + al-playback → Phase 3 al-comment + al-danmaku + al-social + 聊天收敛 → Phase 4 al-datacenter 与下线 AL-web。
- 每个域“停旧写入口 → 起新服务 → 切网关路由”，同一时刻只有一个写方；自用项目接受秒级到分钟级不可用。
- 回滚只改网关路由，数据格式不变；新服务与 AL-web 读写同一库表。
- Phase 5 数据库 schema 拆分作为独立变更。

## 9. 测试与护栏

- 契约测试按域落地：每个域切流前，用 MockMvc 固化该域 Controller 的请求/响应快照，覆盖成功、305、601、602。
- 点赞补幂等、补偿重投测试；播放补“重启不丢、重复消费幂等、删除清理”测试。
- ArchUnit 守住：服务包不依赖其他服务实现包；Mapper XML 不出现非属主表名（后者在服务包就位后启用）。
- 不引入 Testcontainers；端到端用一份手工冒烟脚本，记录在问题日志。

## 10. 风险与取舍

- 共享库阶段仍可能出现跨域 SQL → ArchUnit + Review 清单 + 二阶段独立账号权限。
- 批量 Feign 放大列表页延迟 → 只提供批量接口、Caffeine 短 TTL、超时与降级空值。
- 事件最终一致使用户短暂看到旧计数 → 点赞沿用 20 秒语义，收藏事件秒级；提供按 `favorite_video` 重算的对账脚本。
- 转码留在 al-video 且单副本 → 文档与健康检查标注，队列/临时目录外置后再考虑扩容。
- WebSocket 仍直连、无鉴权 → 本变更不改连接方式，安全改造另立变更。
- pending 载荷 24h 过期 → 文档标注；如需更强保证，后续改为独立持久化任务表。

## 11. 与 OpenSpec 产物的关系

- `openspec/changes/refactor-backend-service-split/proposal.md`：动机与范围。
- `specs/`：5 个能力的增量规格，是验收依据。
- `design.md`：服务/路由/事件的技术设计。
- `tasks.md`：42 条实施任务，按 Phase 分组。
- 本文相对 OpenSpec 的两处细化：播放进度改为 Redis 持久化延迟队列；点赞补发送失败补偿。两处已同步到增量规格与任务。
