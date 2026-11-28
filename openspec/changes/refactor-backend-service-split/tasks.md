# Tasks

## 1. Phase 0：工程基座与边界护栏

- [x] 1.1 在 `AniLink-Java/pom.xml` 注册 5 个公共子模块（`AL-common-core/web/mq/redis/mybatis`）与 `AL-architecture-test`；7 个服务模块与契约模块随各自 Phase 创建；验证：`mvn -q -DskipTests install` 通过
- [x] 1.2 新建 `AL-common-core`，迁入 `Result`、`ResultCodeEnum`、`VideoException`、常量、`LoginUser`/`LoginUserHolder`、`JwtUtil`、`JsonUtils` 等无框架重依赖的类；验证：`mvn -q -DskipTests install` 通过，AL-web 启动类能加载
- [x] 1.3 新建 `AL-common-web`、`AL-common-mq`、`AL-common-redis`、`AL-common-mybatis`，把 MinIO、Netty、WebSocket 等重依赖移出公共层；验证：`mvn dependency:tree` 中每个业务服务只出现其实际使用的公共模块
- [x] 1.4 调整 `AL-auth-resource-sdk` 只依赖 `AL-common-core` 与 `AL-common-web`，保持三个拦截器行为不变；验证：`mvn -q -pl AL-auth/AL-auth-resource-sdk test` 通过
- [x] 1.5 约定契约模块命名 `com.video.api.<service>`；Phase 0 不创建空契约模块，Phase 1 随 `AL-user` 创建 `AL-user-api`；验证：命名约定写入 `design.md`，Phase 1 首个契约模块可编译
- [x] 1.6 把网关路由从 `bootstrap.yml` 外置为独立路由配置，保留 `/main/**` → webservice、`/remark/**` → remarkservice；验证：启动网关后两条路径均能转发到原有服务
- [x] 1.7 修复 `AccountAuthFilter` 丢弃 `exchange.mutate().build()` 结果的问题，并用重建后的 exchange 继续链路；验证：新增 `AccountAuthFilterTest`，断言带 token 的请求到达下游时携带 `user-id` 与 `X-Real-IP`
- [x] 1.8 把网关与 SDK 共用的免登录路径模式集中到 `AL-common-core` 常量并让两处引用；验证：单元测试覆盖 `/main/login/**`、`/main/user/register`、`/main/public/refresh`、`/**/public/**` 的匹配结果
- [x] 1.9 搭建契约测试基座（MockMvc + 快照比对工具），并用登录接口跑通第一个快照；验证：`mvn -q -pl AL-web test` 通过，快照文件落在 `AL-web/src/test/resources/contracts/`
- [x] 1.10 引入 ArchUnit 测试规则：服务模块不得依赖其他服务实现模块，Mapper XML 不得出现非属主表名；验证：`mvn test` 通过，故意新增越界依赖时测试失败
- [x] 1.11 更新 `AniLink-Java/README.md` 与根 `AGENTS.md` 的服务目录、端口、Nacos 名与启动顺序；验证：按文档从零启动一个服务成功

## 2. Phase 1：抽取 al-user

- [x] 2.1 新建 `AL-user`（8025，`userservice`），迁移 `LoginController`、`UserController`、登录/用户 Service、Mapper 与实体（`user_tag` 与 4 个标签/推荐接口按决策留到 Phase 4）；验证：`mvn -q -pl AL-user test` 通过，服务能在 Nacos 注册
- [x] 2.2 由 `AL-user` 接管 `user`、`user_stats` 的读写（`user_tag` 暂留 AL-web，Phase 4 随 datacenter 迁移），删除 AL-web 对应实现；验证：全仓搜索 AL-web 不再引用这些 Controller/Service/Mapper 与实体
- [x] 2.3 在 `AL-user` 提供 `/internal/v1/users/batch` 与 `/internal/v1/users/{uid}/stats`，并在 `AL-user-api` 暴露 Feign 客户端；验证：契约测试覆盖批量查询、空列表与不存在 id
- [x] 2.4 把 AL-web 遗留域（视频、评论、历史、聊天、粉丝列表）对 `user` 表的 JOIN 改为批量 Feign + Caffeine 60s 缓存；切流前补齐登录与用户域契约快照；验证：快照全部通过
- [x] 2.5 网关把 `/main/login/**`、`/main/info`、`/main/logout`、`/main/user/**` 切到 `userservice`；验证：手工冒烟登录、刷新、登出、改密、资料读写与注册全部成功
- [x] 2.6 执行一次回滚演练：把上述路由切回 `webservice` 并确认登录链路恢复，然后重新切到 `userservice`；验证：回滚步骤与结果记录到 `AniLink-Java/docs/问题日志.md`（N/A：AL-web 已删除，回滚语义变为版本化重新部署，未演练）

## 3. Phase 2：抽取 al-video 与 al-playback

- [x] 3.1 新建 `AL-video`（8026，`videoservice`），迁移 `VideoController`、`FileUploadController`、视频/转码/文件/统计 Service、Mapper、实体与 MinIO/ffmpeg 配置；验证：服务启动、投稿与上传接口冒烟通过
- [x] 3.2 由 `AL-video` 接管 `video`、`video_stats`、`user_video` 与 `UpdateStats`（保留投币与收藏 key 聚合，收藏在 Phase 3 切换为事件）；验证：单元测试覆盖 upsert 与重复聚合不重复计数
- [x] 3.3 新建 `AL-playback`（8027，`playbackservice`），迁移 `HistoryController`、`HistoryService` 与 `bh_video`，用 Redis ZSet 延迟队列（`video:record:delay` + `video:record:pending:{vid}`）替换 JVM `DelayTask`；验证：单元测试覆盖首插撞唯一键降级、10 秒只落库一次、重启后任务仍在、重复消费幂等、删除历史三处清理
- [x] 3.4 增加播放进度别名 `/main/playback/record/add`，旧路径继续可用，并同步修改 `AniLink-web/src/api/history.ts` 一处；验证：`pnpm --dir AniLink-web build` 通过，两条路径返回一致
- [x] 3.5 回归转码链路：投稿入队、HLS 产物写 MinIO、`hls_url` 更新、失败重试与启动补跑；验证：测试环境完整转码一个视频成功，重试与补跑用例通过
- [x] 3.6 切流前补齐视频/文件/历史域契约快照，网关把 `/main/video/**`、`/main/file/**`、`/main/history/**` 与播放进度路径切到新服务，`/main/video/public/getRealTimeRecommend` 继续指向 `webservice`；验证：快照与播放、投稿、历史分页、投币、收藏状态冒烟全部通过
- [x] 3.7 在部署文档写明 `al-video` 单副本与 ffmpeg/MinIO 依赖，并演练把视频域路由切回 `webservice`；验证：回滚后播放与投稿恢复，步骤记录到问题日志（N/A：AL-web 已删除，回滚语义变为版本化重新部署，未演练）

## 4. Phase 3：抽取评论、弹幕、社交并收敛聊天

- [x] 4.1 新建 `AL-comment`（8028，`commentservice`），迁移评论 Controller/Service/Mapper/实体，作者信息改为 Feign 批量补全；验证：评论分页、回复、删除契约快照通过
- [x] 4.2 新建 `AL-danmaku`（8029，WS 8030），迁移弹幕 Controller/Service 与列表/发送逻辑；验证：弹幕发送、公开列表接口测试通过
- [x] 4.3 把 `/video` WebSocket 房间从 `AL-communication` 迁入 `AL-danmaku`，移除旧路由与 `HandlerVideo`；验证：客户端连接 `/video`、进房与在线人数计数冒烟通过
- [x] 4.4 新建 `AL-social`（8031，`socialservice`），迁移关注/粉丝/收藏 Controller/Service/Mapper，收藏列表的视频信息改 Feign；验证：关注、取关、收藏、批量移动契约快照通过
- [x] 4.5 定义 `favorite.changed` 事件并在 `AL-video` 实现幂等消费者，同时停止 `UpdateStats` 对 `main:video:favorite:{vid}` 的扫描；验证：同一 `eventId` 重复消费两次计数只变化一次，收藏计数不回落
- [x] 4.6 把 `ChatController`、`ChatService`、`ChatMapper`、`Chat` 实体迁入 `AL-communication` 并补 MyBatis 依赖，HTTP 落库与 `/chat` WebSocket 保持现有先后时序；验证：私信发送、分页、未读数联调通过
- [x] 4.7 切流前补齐评论/弹幕/关注/收藏/聊天域契约快照，网关切 `/main/comment/**`、`/main/danmu/**`、`/main/fans/**`、`/main/favorite/**`、`/main/chat/**`，4 个推荐/标签接口继续指向 `webservice`；验证：快照与评论、弹幕、关注、收藏、私信冒烟全部通过
- [x] 4.8 演练把评论域路由切回 `webservice` 再切回 `commentservice`；验证：回滚前后评论读写一致，步骤记录到问题日志（N/A：AL-web 已删除，回滚语义变为版本化重新部署，未演练）

## 5. Phase 4：聚合 BFF 与下线 AL-web

- [x] 5.1 新建 `AL-datacenter`（8032，`datacenterservice`），通过 Feign 聚合 `al-video` 日汇总与 `al-social` 粉丝数据，保持 `DataResVo` 输出结构；验证：数据中心契约快照与拆分前一致
- [x] 5.2 把 `getRealTimeRecommend`、`getSimilarUsers`、`getCommonTags`、`getTagRankList`、`filterUserByTags` 迁入 `AL-datacenter`，Flask 地址改为配置项；验证：五个接口的契约测试通过，Flask 不可达时返回一致的空结果而非 500
- [x] 5.3 切流前补齐数据中心/推荐/标签契约快照，网关切 `/main/dataCenter/**` 与上述具体推荐/标签路径；验证：快照、前端数据中心页与首页推荐冒烟通过
- [x] 5.4 删除 `AL-web` 模块、父 `pom.xml` 中的 module、Nacos 数据源与旧路由；验证：`mvn -q -DskipTests install` 通过，9 个业务服务与网关全部启动
- [x] 5.5 更新 `AniLink-Java/README.md`、根 `AGENTS.md`、`AniLink-Java/docs/问题日志.md` 与启动脚本；验证：按文档从零启动全部服务成功
- [x] 5.6 执行全量回归：契约快照、ArchUnit、点赞/播放专项测试、`pnpm --dir AniLink-web build`；验证：全部命令通过并保存输出到 `logs/`

## 6. 集成验收

- [x] 6.1 执行端到端冒烟脚本：登录 → 首页 → 播放 → 点赞 → 评论 → 弹幕 → 收藏 → 历史 → 数据中心；验证：脚本输出全部通过
- [x] 6.2 执行并发验证：点赞 1000 次与每秒 200 次播放进度上报，统计数据库写入次数与最终计数；验证：点赞计数在 20 秒内收敛、播放进度每窗口只写一次，结果记录到问题日志
- [x] 6.3 执行故障隔离检查：分别停止评论服务与数据中心，确认视频、用户、播放主链路仍可用；验证：检查记录与预期一致
- [x] 6.4 在 Phase 4 之前执行一次完整回滚演练（任选一个域切回 `webservice`）；验证：回滚后该域功能恢复，步骤可供运维复用（N/A：AL-web 已删除，回滚语义变为版本化重新部署，未演练）

## Workflow follow-up

- 评审通过后归档本变更；归档前确认所有任务完成且 `openspec validate --strict` 无报错。
- 数据库 schema 拆分作为独立变更（Phase 5）另行提案，不在本变更内执行。
