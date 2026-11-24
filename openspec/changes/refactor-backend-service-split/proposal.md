# Proposal

## Why

`AL-web` 目前在一个 Spring Boot 进程里承载了账号、视频与转码、播放历史、评论、弹幕、聊天、粉丝、收藏、数据中心等 11 个业务域：94 个 Java 文件、11 个 Controller、15 个 Service 实现。不同域直接跨表 JOIN（`user`、`follow`、`video_stats` 等被反复关联），Controller 里直接编排转码与 Flask 推荐调用，`AL-api` 让每个服务都能看到全部实体；聊天与弹幕的 WebSocket 又放在 `AL-communication`，形成同一业务域两处实现。结果是改动牵一发动全身、无法按业务独立扩容、边界难以维护。

当前前端正在重写（`AniLink-web`）、鉴权与转码链路刚稳定，是拆分成本最低的窗口期；点赞与播放进度两条高并发链路已有成熟实现（Redis + 批量 MQ / 延迟落库），可以在拆分中原样保留。

## What Changes

- 按领域边界把 `AL-web` 拆成 7 个新服务，保留并收敛 2 个既有服务，形成 9 个业务服务 + 1 个网关：
  - `al-user`：登录/注册/刷新/登出/改密、用户资料、用户统计、标签；
  - `al-video`：视频元数据、投稿上传、HLS 转码、视频统计、投币；
  - `al-playback`：观看历史与播放进度；
  - `al-social`：关注/粉丝、收藏夹；
  - `al-comment`：评论与回复；
  - `al-danmaku`：弹幕 HTTP 与视频房间 WebSocket；
  - `al-datacenter`：数据中心统计、推荐与标签聚合 BFF（无独立库表）；
  - `al-remark`（保留）：点赞；
  - `al-communication`（保留并收敛）：聊天室 HTTP + 私信 WebSocket。
- 对外保持 `/main/**` 路径、响应结构与 305/601/602 状态码不变，网关按子路径分流到新服务；仅播放进度这类跨域冲突接口增加新路径别名并同步 `AniLink-web` 一处调用。
- 建立“一表一主”：每张表只有一个属主服务，禁止跨服务 SQL；跨域读使用内部批量接口（OpenFeign），跨域写使用领域事件（RabbitMQ）。
- 拆分公共模块：`AL-common` 按 core/web/mq/redis/mybatis 拆为按需依赖；新增各服务 `-api` 契约模块，实体下沉到属主服务，`AL-api` 最终退场。
- 保留两条高并发链路：点赞（Redis Set/ZSet + 20s 批量 MQ + 批量回写）保留在 `al-remark`；播放进度迁移到 `al-playback`，并用 Redis ZSet 持久化延迟队列替代 JVM 队列，重启不丢待落库任务、多实例可消费。
- **BREAKING**（仅内部）：`AL-web` 最终下线，模块坐标/包名/Nacos 服务名新增；对外 HTTP 契约不变。
- 采用绞杀者迁移：按用户 → 视频/播放 → 互动/社交 → 数据中心分阶段切换，每阶段 `AL-web` 可兜底、可单独回滚。
- 本变更不做：依赖版本升级、数据库 schema 拆分（留到二阶段独立变更）、非本目标业务的功能改造、前端页面重写。

## Capabilities

### New Capabilities

- `service-decomposition`：服务目录、每个服务的职责边界、允许的依赖方向与独立启动/注册要求。
- `api-compatibility`：`/main/**` 外部契约、网关路由与鉴权白名单、用户身份透传、305/601/602 语义。
- `data-ownership`：表归属矩阵、跨服务读写规则、共享库阶段的约束与二阶段 schema 隔离的迁移要求。
- `playback-progress`：播放进度上报的缓存合并、延迟持久化、并发与重启语义。
- `like-counter`：点赞/取消点赞、计数聚合与最终一致的时延与幂等语义。

### Modified Capabilities

无（仓库此前没有 OpenSpec 能力规格）。

## Impact

- 代码：`AniLink-Java` 全部 7 个模块；新增 7 个服务模块、4 个契约模块、5 个公共子模块；`AL-web` 迁移完成后删除。
- 配置：`AL-gateway` 路由表与鉴权白名单；各服务 `bootstrap.yml`/`application.yml`、Nacos 服务名与共享配置引用；新增端口 8025–8032。
- 数据：一阶段仍用 `video` 库，仅调整归属与访问方式，不做表结构变更；二阶段再按服务拆 schema。
- 消息：保留 `like.record.topic` 契约；新增 `favorite.changed` 事件（收藏状态变化 → `al-video` 单写计数器）。
- 依赖：新增 OpenFeign、Caffeine、ArchUnit（测试）；不升级既有依赖版本。
- 前端：`AniLink-web` 仅播放进度接口路径需同步一处；其余 API 与鉴权流程不变。
- 运维：启动顺序与端口增加；转码仍要求单实例 + 本地 ffmpeg/MinIO，需在部署文档中固化。
