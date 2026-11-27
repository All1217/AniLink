# Design

## Context

现状与约束（详见 `proposal.md`）：

- `AL-web` 是单进程（8022）承载 11 个 Controller、15 个 Service 实现，共用 `video` 库；网关把 `/main/**` 整体路由到 `webservice`，`/remark/**` 路由到 `remarkservice`。
- 鉴权链路：网关 `AccountAuthFilter` 校验 access token 并尝试注入 `user-id` 头，`AL-auth-resource-sdk` 的 `UserInfoInterceptor` 读该头写入 `ThreadLocal`，`LoginAuthInterceptor` 在缺失时返回 305；登录/刷新逻辑目前仍在 `AL-web` 的 `LoginService`。
- 两条高并发链路已成型：点赞走 `al-remark` 的 Redis Set/ZSet + 20s 批量 MQ + 消费方批量回写；播放进度走 `AL-web` 的 Redis Hash + JVM `DelayQueue`（10s）延迟落库。
- 跨域耦合证据：`DataCenterMapper` 直接 JOIN `video`/`user_video`/`follow`；`VideoMapper`、`CommentMapper`、`HistoryMapper`、`ChatMapper` 均 JOIN `user`；收藏服务直接读 `user_video` 与 `video`。
- 既有约束：Spring Boot 3.0.5 / JDK 17，不升级既有依赖；Nacos 共享配置集中管理数据源与中间件；`AniLink-web` 的 `src/api` 全部使用 `/main/**` 路径；转码依赖本机 ffmpeg、MinIO 与本地临时目录，只能单实例运行。
- 测试现状：仅 `CodeTest`、`LoginTest`、`MinioTest` 三个手工验证类，没有可回归的接口契约测试。

## Goals / Non-Goals

**Goals:**

- 给出可直接落地的服务目录、端口、Nacos 服务名、网关路由表与依赖规则。
- 定义表归属与跨域读写契约，使拆分后不再出现跨服务 SQL。
- 原样保留点赞与播放进度两条高并发链路的吞吐与一致性语义。
- 给出分阶段绞杀迁移路径，每阶段可单独切流、单独回滚。
- 建立能防止边界回退的自动检查（契约测试 + 架构规则）。

**Non-Goals:**

- 不在本变更内拆分数据库 schema（共享 `video` 库，二阶段独立变更）。
- 不升级 Spring Boot / Spring Cloud / MyBatis-Plus 等既有依赖版本。
- 不重写推荐引擎（Flask）与前端页面。
- 不顺带修复与拆分无关的功能缺陷（如 `main:video:play` 无写入方、评论点赞消息无消费者）；仅在拆分必须处理时记录并最小修复。
- 不改变 WebSocket 客户端直连的部署形态，也不在本变更内为 WS 增加鉴权。

## Decisions

### D1 服务目录与端口

采用 9 个业务服务 + 网关的中等粒度。服务名、端口与职责如下：

| 服务 | Nacos 名 | 端口 | 职责 | 拥有的表 |
| --- | --- | --- | --- | --- |
| al-gateway | gatewayservice | 8090 | 路由、access token 校验、身份透传 | — |
| al-user | userservice | 8025 | 登录/注册/刷新/登出/改密、用户资料、用户统计 | `user`、`user_stats`（`user_tag` Phase 1–3 暂留 AL-web，Phase 4 随 datacenter 迁移） |
| al-video | videoservice | 8026 | 视频元数据、投稿上传、HLS 转码、视频统计、投币 | `video`、`video_stats`、`user_video` |
| al-playback | playbackservice | 8027 | 观看历史、播放进度 | `bh_video` |
| al-social | socialservice | 8031 | 关注/粉丝、收藏夹 | `follow`、`favorite`、`favorite_video` |
| al-comment | commentservice | 8028 | 评论与回复 | `comment` |
| al-danmaku | danmakuservice | 8029 / WS 8030 | 弹幕 HTTP 与视频房间 WebSocket | `danmu`、`sub_danmu` |
| al-datacenter | datacenterservice | 8032 | 数据中心统计、推荐与标签聚合 BFF | — |
| al-remark | remarkservice | 8021 | 点赞 | —（仅 Redis） |
| al-communication | communicationservice | 8023 / WS 8024 | 聊天室 HTTP 与私信 WebSocket | `chat`、`chat_message` |
| AL-web（迁移期） | webservice | 8022 | 未迁移域兜底，Phase 4 后删除 | 迁移中的表 |

选择中等粒度而非 11 个更细服务：评论/弹幕/收藏/关注各自独立会显著增加 Nacos、端口与联调成本，而它们之间没有必须拆开的性能或团队边界；也不选 4 个粗服务，否则评论、弹幕、点赞仍会混在一起。备选方案与取舍在“Risks / Trade-offs”中说明。

### D2 网关路由与外部路径兼容

网关按更具体的子路径优先匹配，保持 `/main/**` 不变：

| 路径 | 目标 |
| --- | --- |
| `/main/login/**`、`/main/info`、`/main/logout`、`/main/user/**` | userservice |
| `/main/video/interact/record/add`、`/main/playback/**` | playbackservice（order 更高） |
| `/main/video/public/getRealTimeRecommend` | datacenterservice（order 更高） |
| `/main/video/**`、`/main/file/**` | videoservice |
| `/main/history/**` | playbackservice |
| `/main/comment/**` | commentservice |
| `/main/danmu/getSimilarUsers`、`/main/danmu/filterUserByTags`、`/main/danmu/getCommonTags`、`/main/danmu/getTagRankList` | datacenterservice（order 更高） |
| `/main/danmu/**` | danmakuservice |
| `/main/fans/**`、`/main/favorite/**` | socialservice |
| `/main/chat/**` | communicationservice |
| `/main/dataCenter/**` | datacenterservice |
| `/remark/**` | remarkservice |

- 播放进度新增别名 `/main/playback/record/add`，旧路径 `/main/video/interact/record/add` 继续由网关转到 playbackservice；`AniLink-web` 只改 `src/api/history.ts` 一处，旧路径保留一个发布周期。
- 内部接口统一 `/internal/v1/**`，网关不声明对应路由，因此外部不可达。
- 跨域组合接口集中到 `al-datacenter`：`getRealTimeRecommend`（Flask 推荐 → al-video 批量取视频）、`getSimilarUsers`（Flask → al-user 批量取用户）、`getCommonTags`/`getTagRankList`/`filterUserByTags`（al-user 标签 + al-danmaku 数据）。这些接口保留原路径，由网关按更具体规则分流，避免把推荐逻辑塞回视频或弹幕服务；在对应 Phase 实现前，它们的路由暂留 `webservice`，避免切流后 404。
- 鉴权白名单模式集中到 `AL-common-core` 的常量类，网关与 `AL-auth-resource-sdk` 共同引用，避免两处维护漂移；各服务仍可通过 `al.auth.resource.exclude-login-paths` 追加本域公开路径。
- 备选：给每个服务独立前缀（`/user/**`、`/video/**`）。路径更清晰，但要改前端 70+ 处调用并承担回归风险，否决。

### D3 数据归属与跨域读取

- “一表一主”见 D1 表；非属主服务不得出现该表的 SQL、Mapper 或实体。
- 跨域读取统一走内部批量接口，禁止 N+1：列表页先取主数据，再按 id 集合批量补全。
- 用户资料是最高频的跨域数据（视频、评论、聊天、粉丝列表都需要），`al-user` 提供 `POST /internal/v1/users/batch`，调用方用 Caffeine 做 60s 本地缓存，避免列表页放大。
- 视频信息被收藏、历史、数据中心依赖，`al-video` 提供 `POST /internal/v1/videos/batch` 与 `GET /internal/v1/videos/{vid}/stats`。
- 数据中心不再 JOIN：`al-video` 提供按 UP 主与时间范围的播放/投币日汇总接口，`al-social` 提供粉丝 id 集合，`al-datacenter` 负责组合，保留“上一周期对比”的现有输出结构。
- 共享库阶段用 ArchUnit 规则约束：服务模块不得依赖其他服务实现模块，Mapper XML 不得出现非属主表名；二阶段拆 schema 时只改数据源与 SQL，不改契约。

### D4 跨域写与事件

- 保留 `like.record.topic` 交换机与 `{bizType}.times.changed` 路由键，`al-remark` 仍是唯一生产方，`al-video` 消费并批量更新 `video_stats.good`；消息体继续携带绝对计数，重投幂等。
- 新增收藏事件：交换机 `social.favorite.topic`、路由键 `favorite.changed`，消息体 `{eventId, uid, vid, collected, delta, occurredAt}`。`al-social` 在收藏/取消提交成功后发送；`al-video` 消费后以 `eventId` 去重，更新 `user_video.collect` 与 `video_stats.collect`。该事件在 Phase 3 上线；在此之前 `UpdateStats` 继续扫描 `main:video:favorite:{vid}`，保证迁移过程中收藏计数不中断。
- Phase 3 事件上线后取消 `UpdateStats` 对 `main:video:favorite:{vid}` 的扫描，避免事件与轮询双重计数；`main:video:coin` 仍由 `al-video` 内部聚合。
- 备选：继续让 `al-social` 写 `al-video` 的 Redis 计数 key。改动小，但跨服务共享可变 key 无法保证归属，也阻碍二阶段拆库，否决。
- 备选：同步 Feign 调用更新计数。会引入跨服务事务与故障放大，否决。

### D5 高并发链路

- 点赞：路径与数据结构不变（`likes:set:biz:{bizId}`、`likes:times:type:{bizType}`、20 秒 `popMin(30)`、MQ 批量回写）。不修改 key 结构，避免已有点赞状态丢失；`bizId` 未带 `bizType` 的既有缺陷记录为后续独立变更。补发送失败补偿：`popMin` 取出的 tuple 在消息发送失败时按原 score `ZADD` 回 ZSet，下个周期重试。
- 播放进度：用 Redis 持久化延迟队列替代 JVM `DelayQueue`。读缓存继续用 `video:record:{vid}`（Hash，TTL 60s）；新增待落库载荷 `video:record:pending:{vid}`（Hash，TTL 24h）与延迟 ZSet `video:record:delay`（member `vid:uid`，score=到期时间）。上报时 HSET + ZADD（同一 member 更新 score，天然合并）；工作线程每秒批量取到期成员（≤200），更新 `bh_video` 后 HDEL + ZREM。重启不丢待落库任务，多实例可消费，重复更新幂等。
- 备选：保留 JVM `DelayQueue`（改动最小，但重启丢窗口、多实例各自为政）；改用 Redisson `RDelayedQueue`（代码更少，但新增较重依赖）。两者均不采用，前者不满足故障恢复，后者超出依赖约束。
- 计数聚合仍归属 `al-video`，但只处理本域 key；收藏计数在 Phase 3 事件上线前保留旧 key 扫描过渡。

### D6 公共模块与契约模块

- `AL-common` 拆为 `AL-common-core`（Result、错误码、常量、登录用户上下文、JWT/Json 工具）、`AL-common-web`（全局异常、Web/Cookie 工具、Knife4j 配置）、`AL-common-mq`（RabbitMQ 配置与 Helper）、`AL-common-redis`（Redis 配置）、`AL-common-mybatis`（MyBatis-Plus 配置）。各服务按需引入，不再被 MinIO/Netty/WebSocket 等重依赖污染。
- `AL-auth-resource-sdk` 只依赖 core/web，保持自动配置不变。
- 每个需要被调用的服务提供 `-api` 契约模块：`AL-user-api`、`AL-video-api`、`AL-playback-api`、`AL-social-api`，内含 Feign 客户端与 DTO；实体下沉到属主服务的 `model` 包。契约模块随对应服务在 Phase 1–4 创建，Phase 0 只建公共模块，避免空脚手架。
- 命名约定：契约模块固定为 `AL-<service>-api`，Java 包名 `com.video.api.<service>`（如 `com.video.api.user`）；实现模块包名 `com.video.<service>`，两者不得混用。
- `AL-api` 最终只保留跨服务共享的少量 DTO（如 `LikedTimesDTO`），迁移完成后删除；`AL-remark`/`AL-communication` 同步改用新契约。
- 备选：保留单一 `AL-common`，只按包拆分。改动小，但依赖仍然全量传递，无法体现“复用性与扩展性”，否决。

### D7 鉴权与身份透传

- 网关在 `AccountAuthFilter` 中用重建后的 exchange 继续链路：`exchange.mutate().request(...).build()` 的返回值必须赋回并传给 `chain.filter(...)`；当前实现丢弃了 `build()` 结果，`user-id` 与 `X-Real-IP` 实际未注入，Phase 0 必须用网关单元测试固定该行为。
- 业务服务只信任 `user-id` 头；写操作禁止以请求体/查询参数中的 uid 作为授权依据（读取他人主页等只读场景可按参数过滤，但写操作必须校验与登录身份一致）。
- Feign 调用沿用 `FeignRelayUserInterceptor` 透传 `user-id`；`AL-common-web` 提供统一的 `CurrentUser` 读取入口，避免各服务重复实现。
- 备选：每个服务自行解析 JWT。会让密钥扩散到 9 个服务，且与网关的过期/吊销语义不一致，否决。

### D8 测试与防回退

- 迁移前先补“特征测试”：用 MockMvc 对现有 11 个 Controller 生成请求/响应快照，覆盖成功、305、601、602 与主要分页参数，作为拆分后的契约基线。
- 点赞与播放进度各补一组并发/时序测试：重复点赞幂等、`popMin` 批量与消息体、10 秒窗口只落库一次、旧任务被丢弃、首插撞唯一键降级更新。
- 引入 ArchUnit（仅测试依赖）：禁止服务模块互相依赖实现、禁止 `com.video.<service>` 跨域引用、禁止 Mapper XML 出现非属主表名。
- 备选：用 Testcontainers 起 MySQL/Redis/RabbitMQ 做端到端测试。需要本机 Docker，当前开发环境未确认，本变更不引入；端到端用一份手工冒烟脚本并记录结果。

### D9 迁移策略（绞杀者）

按域切流，不一次性替换：

1. **Phase 0 基座**：模块骨架、公共模块拆分、契约模块、网关路由外置、身份透传修复、契约测试与 ArchUnit；不改变任何线上路由。
2. **Phase 1 al-user**：迁移登录/用户/标签；网关把 `/main/login/**`、`/main/info`、`/main/logout`、`/main/user/**` 切到 userservice；其余域暂时通过 Feign 或继续由 AL-web 查询用户信息，保持可回滚。
3. **Phase 2 al-video + al-playback**：迁移视频/上传/转码/统计/投币与历史/进度；切 `/main/video/**`、`/main/file/**`、`/main/history/**` 与播放进度别名，其中 `/main/video/public/getRealTimeRecommend` 暂留 `webservice`；AL-web 不再写这两域的表。
4. **Phase 3 al-comment + al-danmaku + al-social + 聊天收敛**：评论、弹幕（含 WS `/video`）、关注/收藏切流；4 个推荐/标签接口暂留 `webservice`；`ChatController` 迁入 al-communication，`al-danmaku` 接管 `HandlerVideo`。
5. **Phase 4 al-datacenter 与下线**：数据中心切到 BFF；删除 AL-web 模块、路由与 Nacos 配置；更新 README、AGENTS.md、问题日志。
6. **Phase 5（独立变更）**：按服务拆 schema 与数据迁移脚本。

每阶段结束条件：该域契约测试全绿、网关切流后前端冒烟通过、AL-web 对应代码删除且无反向依赖。

## Risks / Trade-offs

- **网关身份头未真正注入** → Phase 0 先写失败测试复现，改为 `ServerWebExchange mutated = exchange.mutate()...build(); return chain.filter(mutated);`，并用集成测试断言下游收到 `user-id`。
- **共享库下仍可能写出跨域 SQL** → ArchUnit + Mapper XML 表名检查 + Code Review 清单；二阶段拆库时用独立账号权限兜底。
- **Feign 放大列表页延迟** → 只提供批量接口、Caffeine 短 TTL、设置连接/读取超时与降级空值；禁止在循环中逐条调用。
- **事件最终一致使用户看到旧计数** → 点赞沿用 20s 语义、收藏事件秒级；前端本就容忍计数延迟；提供对账脚本作为运维手段。
- **转码单实例与本地磁盘** → `al-video` 文档标注单副本，健康检查暴露队列深度；多副本需先把队列与临时目录外置，列为后续变更。
- **9 个服务的本地启动成本** → 提供一键启动脚本与启动顺序文档；开发期可只启动网关 + 目标域 + 依赖域。
- **WebSocket 仍直连、无鉴权** → 本变更不改客户端连接方式，但把 `/video` 房间收敛到 `al-danmaku`、`/chat` 留在 `al-communication`；鉴权与网关 WS 代理列为后续安全变更。
- **前端播放进度路径调整** → 新别名与旧路径并存一个发布周期，前端只改一处；回滚时旧路径仍可用。
- **既有测试缺失导致回归不可见** → 先补契约快照再迁移；每阶段以“AL-web 与目标服务返回一致”为切流门槛。
- **端口/服务名漂移** → 端口表与 Nacos 名写入 README 和启动脚本；网关路由集中在一个 YAML，禁止散落。
- **收藏计数事件丢失导致漂移** → 事件带 `eventId` 去重并提供按 `favorite_video` 重算的对账脚本；对账频率低，不影响主链路。

## Migration Plan

- **切流**：每个域只改网关路由，保留 AL-web 路由作为回滚项；同一张表在同一时刻只允许一个写方，切换前先停旧写入口（自用项目可接受秒级不可用）。
- **回滚**：把网关路由改回 `webservice` 并重启对应服务即可；新服务与 AL-web 读写同一库表，数据格式不变。
- **数据**：Phase 1–4 不改表结构；`favorite.changed` 上线前先部署消费方，避免消息无人消费。
- **发布顺序**：Nacos/共享配置 → 公共模块与契约模块 `mvn install` → 目标服务 → 网关路由 → 前端。启动顺序：gateway → communication → remark → user → video → playback → social → comment → danmaku → datacenter（gateway 最后切流）。
- **验证**：每阶段执行契约快照、ArchUnit、点赞/播放专项测试与前端冒烟；Phase 4 后执行全量回归并删除 AL-web。

## Open Questions

- `al_remark`、`al_communication` 两个已配置但无实际读写的数据库，是否在 Phase 4 一并从 Nacos 数据源中移除，需要确认没有外部脚本依赖。
- `user_video.play` 目前没有写入方，数据中心“总播放/粉丝播放”实际长期为 0；是否在本重构后单独补播放计数链路，留待后续变更决定。
- 弹幕 WebSocket 是否在后续版本收口到网关统一入口并补鉴权，涉及前端连接地址与握手协议，暂不纳入本变更。
