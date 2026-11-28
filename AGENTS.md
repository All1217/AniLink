# AGENTS.md — AniLink

本文件写给 AI 编码助手。动手前先读「项目速览」「仓库结构」「硬性约束」，再按任务查对应章节。命令均为 Windows PowerShell，路径以本文件所在目录（仓库根）为基准。

## 项目速览

AniLink 是一个仿 B 站（bilibili）的视频社区全栈项目，原为三个独立仓库（`AniLink-Java` / `AniLink-Vue3` / `AniLink-AlgorithmEngine`），现已合并为本单仓库，另带一个本地数据库 MCP 工具。面向作者自用与学习，不做通用产品。

默认以资深 Spring Cloud + Vue 3 + Python 算法工程师的标准判断与实现。

- 主链路：浏览器 → Vue 3 SPA（旧前端 dev 8080，新前端 `AniLink-web` dev 5173，`/api` 反代）→ 网关 `AL-gateway`（8090）按子路径分流：`/main/login`、`/main/info`、`/main/logout`、`/main/public/refresh`、`/main/user/**` → `AL-user`（8025），`/main/video/**`、`/main/file/**` → `AL-video`（8026），`/main/history/**`、`/main/playback/**`、播放进度 → `AL-playback`（8027），`/main/comment/**` → `AL-comment`（8028），`/main/danmu/**` → `AL-danmaku`（8029 + Netty WS 8030），`/main/fans/**`、`/main/favorite/**` → `AL-social`（8031），`/main/dataCenter/**` 与推荐/标签接口 → `AL-datacenter`（8032），`/main/admin/**` → `AL-admin`（8040），`/remark/**` → `AL-remark`（8021），`/main/chat/**` 与私信 WS → `AL-communication`（8023 + Netty 8024）；点赞/评论/收藏/历史走 MySQL + Redis + RabbitMQ。`AL-web` 已在 Phase 4 删除，网关不再有 `/main/**` 兜底路由。
- 画像/推荐：`AniLink-AlgorithmEngine`（Flask，5000）消费 RabbitMQ 消息、共享 Redis 缓存、按 REST 提供 `/recommendations/<user_id>`，定时任务（APScheduler，120s 一轮）刷新行为标签、质量阈值与标签向量。
- 鉴权：双令牌（`access-token` 30 分钟放请求头 + `refresh-token` 7 天滑动放 HttpOnly Cookie），两者用 `typ` claim 区分；网关 `AccountAuthFilter` 只认 access，`AL-user` 的 `/main/public/refresh` 只认 refresh 并按 jti 轮换（5 秒宽限、重用即吊销全端）；`AL-auth` 的 resource-sdk 拦截器（`LoginAuthInterceptor` / `UserInfoInterceptor` / `FeignRelayUserInterceptor`）只依赖网关透传的 `user-id` 头，`al.auth.resource.enable` 控制是否启用，免登录路径由各业务服务的 `al.auth.resource.exclude-login-paths` 声明。状态码：305 未登录 / 601 过期可续约 / 602 非法需重登。
- 账号体系：`user.username` 是用户自选的登录名（唯一、大小写不敏感、本期不可改，2-20 位中英文/数字/下划线且不能纯数字），`user.nickname` 仅用于展示（可重复、可改）；密码用 bcrypt（Hutool `BCrypt`，cost 10）存储，历史 MD5 在登录成功时就地升级；登录失败统一返回 307 且按「账号 5 次 / IP 20 次（15 分钟窗口）」限流（212），改密接口需校验旧密码并吊销全部会话。
- 视频播放：投稿后 `AL-video` 用 ffmpeg 异步转 HLS（单线程队列，`al.transcode.*`，ffmpeg 路径由 `AL_FFMPEG_PATH` 覆盖），产物在 MinIO 的 `video` 桶 `hls/{vid}/`（6s fMP4 分片 + `index.m3u8`），状态写 `video.transcode_status`（0 待转 / 1 转中 / 2 完成 / 3 失败），完成后写 `video.hls_url`；前端 `hls.js`（动态 import，优先 MSE；只有不支持 MSE 的浏览器才退回原生 HLS，Safari 原生 HLS 只在该分支生效）优先播 `hlsUrl`，为空或致命错误回退 `videoUrl`（MP4 + Range）。转码还会把原 MP4 重封装成 faststart（moov 前置），对象名不变；视频码率按「源码率 ×1.15 封顶，且不超过 `al.transcode.max-bitrate-kbps`」控制（ffprobe 读源码率，缺失时退回配置上限）。启动时自动补跑 `transcode_status=0` 的稿件，所以**不要手工改这一列**。
- 依赖服务：Nacos（注册 + 共享配置）、MySQL、Redis、RabbitMQ、MinIO、Elasticsearch；地址与口令不在文档里复述，见各服务配置。
- 图片链路：封面/头像上传后由 `AL-video` 用 ffmpeg 生成 WebP 派生图（封面 480w/960w，头像 192 方图），派生失败回退原图；`video.cover_url` 存 960 主图、`video.cover_thumb_url` 存 480 缩略图，列表接口返回 `coverThumbUrl`，前端列表用缩略图 + `srcset`，首行 `eager` + `fetchpriority=high`。头像未设置时由前端内置占位图兜底，`user.avatar` 不再写入内网默认地址。配置项 `al.image.*`，ffmpeg 路径复用 `al.transcode.ffmpeg-path`。

## 仓库结构

| 路径 | 作用 |
| --- | --- |
| `AniLink-Java/` | 后端微服务（Maven 多模块），`Spring Boot 3.0.5` + `Spring Cloud Alibaba`，包根 `com.video` |
| `AniLink-Vue3/` | 旧前端 SPA（Vue CLI 5 + Vue 3 + TS + Less，dev 8080），**只作对照，不再改动** |
| `AniLink-web/` | 新前端（Vite 6 + Vue 3.5 + SCSS，dev 5173），重写中，进度见 `docs/AniLink-web重构计划.md` |
| `AniLink-admin/` | 独立管理端（Vite 6 + Vue 3.5 + Element Plus，dev 5174），入口 `/main/admin/**`，仅管理员可登录 |
| `AniLink-AlgorithmEngine/` | 用户画像与推荐引擎（Flask），入口 `src/main.py` |
| `db/` | MySQL 初始化与分库：`schema.sql` 为分库前 `video` 单库基线，`schema/` 为 Phase 5 分库目标（`al_user` 等 7 个业务库 + `al_remark`），`migrations/phase5/` 为已有库的分库迁移 runbook（快照 / 建库 / RENAME / 授权 / 回滚） |
| `mcp-server-database/` | 本地/内网库元数据与 SQL MCP（Spring AI Streamable HTTP，`http://localhost:8102/mcp`）；**已 gitignore，不入库** |
| `docs/` | 根级说明与操作日志（`operations-log.md`） |
| `.dsh-bundles/mcp-database/` | DSH 的配置型插件，把上面的 MCP 接进 DeepSeek Harness |
| `logs/` | 本地命令输出（gitignore），如 `git-bash-log.txt` |

后端模块：`AL-gateway`（8090 网关，reactive）、`AL-user`（8025 `userservice`：登录/注册/刷新/登出/改密、资料、统计、标签）、`AL-video`（8026 `videoservice`：视频元数据、投稿上传、HLS 转码、统计、投币、`user_video`）、`AL-playback`（8027 `playbackservice`：历史与播放进度）、`AL-comment`（8028 `commentservice`：评论）、`AL-danmaku`（8029 + WS 8030 `danmakuservice`：弹幕与视频房间）、`AL-social`（8031 `socialservice`：关注/粉丝、收藏夹）、`AL-datacenter`（8032 `datacenterservice`：统计/推荐/标签聚合 BFF）、`AL-admin`（8040 `adminservice`：管理端）、`AL-remark`（8021 点赞）、`AL-communication`（8023 + WS 8024 聊天）、`AL-auth`（`AL-auth-resource-sdk` 鉴权 starter）、`AL-api`（共享 entity/dto）；契约模块 `AL-user-api` / `AL-video-api` / `AL-social-api` 存放各服务 `/internal/v1/**` 的 Feign 客户端与 DTO。`AL-common` 已拆为 `AL-common-core` / `AL-common-web` / `AL-common-mq` / `AL-common-redis` / `AL-common-mybatis`。`AL-web` 已在 Phase 4 删除，对应模块与父 pom entry 均已移除；网关按子路径分流，对外 `/main/**` 不变。拆分设计与任务见 `openspec/changes/refactor-backend-service-split/`。

各业务服务内部分层跟随 `AL-video`：`controller`（含 `controller/internal` 内部契约）/ `service` / `mapper` / `vo` / `mq` / `task` / `config`；跨服务共享的实体与 DTO 在 `AL-api`，跨服务契约在 `AL-*-api` 模块。
前端目录：`views`（BrowseHistory / MainWindow / MessageCenter / UploadManager / UserPage / Video）/ `components` / `api`（`Models.ts`、`index.ts`、`app.ts`）/ `router` / `styles` / `util`（`http` 封装）。

## 技术栈（以仓库 `pom.xml`、`package.json`、`requirements.txt` 为准）

- 后端：Spring Boot 3.0.5；JDK 17；Spring Cloud 2022.0.4；Spring Cloud Alibaba 2022.0.0.0-RC2（Nacos）；MyBatis-Plus 3.5.3.1；MySQL 8.0.33；JJWT 0.11.2；Knife4j 4.1.0；MinIO 8.5.12；Netty 4.1.100；Hutool 5.8.16；Fastjson 1.2.76；RabbitMQ；Redis。
- 前端：Vue 3.2；Vue CLI 5（`vue-cli-service`，不是 Vite）；TypeScript 5.8；Element Plus 2.9；Pinia 2.3 + persistedstate；vue-router 4；axios 1.7；ECharts 5.6；artplayer 5.2 + danmuku 插件；Less 4。
- 引擎：Flask 3.1.3；pandas 3.0；numpy 2.4；scikit-learn 1.8；jieba；redis 7.4；pika（RabbitMQ）；pymysql + dbutils；elasticsearch 9.3；apscheduler 3.11；openai 2.30（走 DashScope 兼容端点，`text-embedding-v4`，1024 维）。
- MCP 工具：Spring Boot 4.1.0；JDK 25；Spring AI 2.0.0（streamable HTTP）。
- **升级依赖前先与用户确认**，不要顺手改版本。

## 常用命令

```powershell
# 后端：仓库没有 mvnw（只有 pom.xml），用 IDEA 直接运行各模块启动类，或本机 mvn
cd AniLink-Java
mvn -q -DskipTests install                   # 首次或改过 AL-api / AL-common-core / AL-auth / AL-*-api 后
mvn -q -pl AL-video spring-boot:run          # 启动类 VideoApplication
# 其它启动类：GatewayApplication / UserApplication / PlaybackApplication / CommentApplication /
#             DanmakuApplication / SocialApplication / DataCenterApplication / AdminApplication /
#             RemarkApplication / CommunicationApplication
# 启动顺序：Nacos/MySQL/Redis/RabbitMQ/MinIO → AL-user → AL-video → AL-playback → AL-social →
#           AL-comment → AL-danmaku → AL-datacenter → AL-admin → AL-remark → AL-communication → AL-gateway
#           业务服务之间无强依赖，网关放最后最稳（服务未注册时请求 503）；AL-video 只能单副本
mvn -q test

# 前端
cd AniLink-Vue3
npm install
npm run serve        # dev 8080；构建校验：npm run build

# 新前端（重写目标，日常开发走这里）
cd AniLink-web
pnpm install
pnpm dev             # dev 5173，/api 反代到网关 8090
pnpm build           # typecheck + 生产构建

# 管理端
cd AniLink-admin
npm install
npm run dev          # dev 5174，/api 反代到网关 8090
npm run build

# 画像引擎（虚拟环境可复用已有 .venv 或 anaconda 环境）
cd AniLink-AlgorithmEngine
pip install -r requirements.txt
python src\main.py   # Flask 5000，需可达 MySQL/Redis/RabbitMQ/ES

# 数据库 MCP（本地工具，不入库；先复制 .env.example → .env）
cd mcp-server-database
.\scripts\start-postgres.bat     # 或 .\scripts\start-mysql.bat（切换先 stop.bat）
.\scripts\stop.bat
```

## 工作方式

- **Plan 模式**：只产出方案，不改文件。方案须含改动点（按模块/文件）、数据库与消息队列影响、验证方式、风险与待确认项。
- **Agent/Build 模式**：按方案直接改代码，结束后写清「改了什么 + 怎么验证」，不要只描述思路。
- **Debug 模式**：先复现取证（日志、堆栈、接口返回、`mcp__database__*` 查到的结构/数据），再最小改动修复；不顺手重构无关代码，不改变既有行为。
- 非平凡需求先做三件事：① 给方案；② 列出无法自主决定的点并请用户确认；③ 检索同类优秀实现（或联网搜索）后借鉴，不照搬。
- 需求含糊或存在多种合理解读时先问，不猜着写。并且涉及到具体业务时追问：这个功能是否区分登录态和非登录态？若区分，具体有什么区别？
- 改完接口/字段后，同步检查网关路由、`AL-api` 的 DTO/实体、前端 `src/api` 与调用方，一并更新。
- 非平凡问题排查完，把结论追加到对应子项目的 `问题日志.md`（`AniLink-Java/docs/`、`AniLink-AlgorithmEngine/Assets/docs/`）。

## 代码与界面规范

- 简洁干练，不过度设计；不引入新依赖、新中间件、新抽象层，除非需求确实需要。
- 每个类、每个方法前加简短注释；方法体达到 30 行及以上时，内部按步骤加注释（沿用现有代码的中文注释风格）。
- 命名与分层跟随现有代码：接口沿用 `/main/**`（对外业务）、`/remark/**`（remark）、`/internal/v1/**`（服务间内部契约，网关不暴露）前缀；前端请求统一走 `src/api`，不要在各页面里直接 `axios`。
- 前端页面用现代简洁风：信息密度高、少留白；沿用 Element Plus 与 `src/styles` 里现有主题变量，不自造设计系统。
- 只改与需求相关的文件，不做无关格式化、重排或重命名；源码与文档统一 UTF-8（命令行读中文请显式 `Get-Content -Encoding UTF8`）。

## 数据库访问（MCP 优先）

需要查库、核表结构或核对开发库数据时，优先调用已接入本地 MCP 的 `mcp__database__*` 工具（或 Cursor 里的 `mcp-database-server`）：

1. `connection_info` — 开局自检，确认 `dialect` 与 `jdbcHost` 符合预期
2. `list_schemas` / `list_tables` / `describe_table(s)` — 摸清结构（注释、主键、索引、外键）
3. `execute_sql` — 查数据，默认只读；要写库先说明原因并取得确认
4. `flyway_status` — 本项目未用 Flyway；表存在时才返回版本信息，别当作结构依据

使用要点与坑：

- **MySQL 下 `list_tables` 必须传 `schema`**（等于库名，如 `wxfbsir`、`al_remark`），否则报「未选定 database」；`execute_sql` 可用 `SHOW TABLES FROM <库>`。
- 一个服务进程只连一种库：切 PG/MySQL 要 `stop.bat` 后重开另一个 start 脚本，客户端地址不变。
- 只连本机/内网开发库，**不要把内网库当生产库排查**。
- 工具不可用时按序排查：`8102` 是否已启动 → `.env` 的 `PGSQL_PASS` / `MYSQL_PASS` → `connection_info` 的 `dialect`/`jdbcHost` → DSH 插件行（`.dsh-bundles/mcp-database`）或 Cursor 的 MCP 是否刷新。
- 仍不可用时，退回到 `AniLink-AlgorithmEngine/Assets/sql.sql`、各服务 `src/main/resources/mapper/*.xml` 与 `src/main/java/**/mapper/` 下的 Mapper 接口确认字段，**禁止臆造表名与字段**。

## 配置与外部依赖

- Nacos 地址、共享配置（`shared-spring.yaml` / `shared-mysql.yaml` / `shared-redis.yaml` / `shared-mq.yaml`）写在各服务 `bootstrap.yml`；库账号与口令在 Nacos 的共享配置里，不写进本地 `application.yml`。
- `AL-video` 与 `AL-admin` 的 MinIO 走环境变量 `MINIO_ENDPOINT` / `MINIO_ACCESSKEY` / `MINIO_SECRETKEY`；引擎的大模型 key 走 `DASHSCOPE_API_KEY`。
- 库名（Phase 5 分库）：每个服务一个库，各服务 `application.yml` 的 `spring.datasource.url` 已指向自己的库——`al_user`（AL-user：user/user_stats/user_tag）、`al_video`（AL-video：video/video_stats/user_video/category）、`al_playback`（AL-playback：bh_video）、`al_comment`（AL-comment：comment）、`al_danmaku`（AL-danmaku：danmu/sub_danmu）、`al_social`（AL-social：follow/favorite/favorite_video）、`al_communication`（AL-communication：chat/chat_message）、`al_remark`（AL-remark，空库）；AL-admin 走内部契约、AL-datacenter 为 BFF，均不需要业务库。数据库账号/口令仍由 Nacos `shared-mysql.yaml` 注入（目标是最小权限 per-service 账号，模板见 `db/migrations/phase5/03-grants.sql`）。分库已在当前开发库执行（16 表 RENAME，快照在 `db/temp/`）；脚本与 runbook 保留用于新环境初始化/其它环境迁移/回滚，见 [db/migrations/phase5/README.md](db/migrations/phase5/README.md)。Redis 用 5 号库；RabbitMQ vhost `AniLink`。
- 未经确认不要改公共配置、Nacos 共享配置、`.env`，也不要升级中间件版本。

## 硬性约束

- 不臆造表结构、字段、接口、配置项或第三方能力；不确定就查 MCP / 源码 / 文档，或直接问。
- 不手工改库来「对齐」代码；不删改他人已有数据与迁移产物。
- 未经确认不做大范围重构、不升级依赖、不动公共配置与密钥。
- 不在代码、日志、文档、提交信息里写入真实密钥、口令、Cookie 或内网地址；发现遗留明文只报告，不扩散。
- `target/`、`node_modules/`、`__pycache__/`、`dist/` 是产物目录，不当源码修改。
- `mcp-server-database/` 已被 `.gitignore` 忽略，是本地工具：不要 `git add -f`，不要提交它的 `.env`。
- 三个子项目原本是独立仓库，历史仍在各自 GitHub 仓库上；不要重新 `git init`、不要删除或移动 `.git`。

## 参考文档

- 各子项目 README 与部署前提：`AniLink-Java/README.md`、`AniLink-Vue3/README.md`、`AniLink-AlgorithmEngine/README.md`、`mcp-server-database/README.md`、`AniLink-web/README.md`、`AniLink-admin/README.md`
- 数据库初始化与分库：[db/README.md](db/README.md)、分库前基线 [db/schema.sql](db/schema.sql)、分库后建表 [db/schema/](db/schema/)、分库迁移 runbook [db/migrations/phase5/README.md](db/migrations/phase5/README.md)
- 前端重构方案与进度：`docs/AniLink-web重构计划.md`
- 问题日志与备忘录：`AniLink-Java/docs/问题日志.md`、`AniLink-AlgorithmEngine/Assets/docs/问题日志.md`、`AniLink-AlgorithmEngine/Assets/docs/备忘录.md`
- 架构与设计图（drawio/PNG）：`AniLink-Java/docs/Assets/AniLink-cloud/`（系统整体架构、模块拆分、E-R 图、标签体系、时序图等）
- 引擎结构说明：`AniLink-AlgorithmEngine/project_structure.txt`；建表参考：`AniLink-AlgorithmEngine/Assets/sql.sql`
- 前端待办与备忘：`AniLink-Vue3/public/TODO.md`、`AniLink-Vue3/public/AI备忘录前端.md`；根级操作日志：`docs/operations-log.md`
- 本地 MCP 接入：`.dsh-bundles/mcp-database/cordis.patch.yml`
- 外部文档：Spring Boot（https://docs.spring.io/spring-boot/index.html）、Spring Cloud Alibaba（https://github.com/alibaba/spring-cloud-alibaba）、MyBatis-Plus（https://baomidou.com）、Element Plus（https://element-plus.org/zh-CN/component/overview）、Spring AI MCP（https://docs.spring.io/spring-ai/reference/）

## 本文件的维护

仓库结构、端口、启动方式、依赖版本或关键约定变化时，请同步更新本文件；它只描述已验证的事实，不写未落地的设想。
