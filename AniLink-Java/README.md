# AniLink Java 后端（微服务版）

AniLink 是一个仿 B 站（bilibili）的视频社区全栈项目。本目录是后端 Maven 多模块工程，基于 Spring Boot 3.0.5 + Spring Cloud Alibaba，包根 `com.video`。

微服务拆分 **Phase 0–4 已完成**：原单进程 `AL-web` 已删除，业务域拆成 9 个可独立部署的服务 + 管理端 + 网关；对外 `/main/**` 契约不变。拆分设计与任务见 [OpenSpec 变更](../openspec/changes/refactor-backend-service-split/) 与 [设计文档](../docs/superpowers/specs/2026-11-24-backend-service-split-design.md)。

## 1. 当前架构

```text
浏览器 / AniLink-web (5173) / AniLink-Vue3 (8080) / AniLink-admin (5174)
        │  /api 反代
        ▼
AL-gateway (8090) 只做路由与 access token 校验
        │
        ├─ /main/login、/main/info、/main/logout、/main/public/refresh、/main/user/**  → AL-user (8025)
        ├─ /main/video/**、/main/file/**                                                → AL-video (8026)
        ├─ /main/history/**、/main/playback/**、/main/video/interact/record/add          → AL-playback (8027)
        ├─ /main/comment/**                                                             → AL-comment (8028)
        ├─ /main/danmu/**                                                               → AL-danmaku (8029 + WS 8030)
        ├─ /main/fans/**、/main/favorite/**                                             → AL-social (8031)
        ├─ /main/dataCenter/**、实时推荐、弹幕标签/推荐接口                              → AL-datacenter (8032)
        ├─ /main/admin/**                                                               → AL-admin (8040)
        ├─ /remark/**                                                                   → AL-remark (8021)
        └─ /main/chat/**                                                                → AL-communication (8023 + WS 8024)

MySQL / Redis / RabbitMQ / MinIO / Nacos 由各服务共享
```

- 服务间只走 `/internal/v1/**` 内部契约（Feign + `AL-*-api` 模块），网关不声明该路由，外部不可达。
- 鉴权走双令牌：access token 放请求头，refresh token 放 HttpOnly Cookie。
- 点赞、播放进度走 Redis + RabbitMQ 异步落库；视频投稿后异步转 HLS。
- 推荐/画像由 `AniLink-AlgorithmEngine`（Flask，5000）提供，`AL-datacenter` 通过 HTTP 调用。

## 2. 模块与端口

### 2.1 可启动的服务（11 个）

| 模块 | 端口 | 服务名 | 作用 | 启动类 |
| --- | --- | --- | --- | --- |
| `AL-gateway` | 8090 | — | 网关、鉴权、路由 | `com.video.gateway.GatewayApplication` |
| `AL-user` | 8025 | `userservice` | 登录/注册/刷新/登出/改密、用户资料与统计、标签 | `com.video.UserApplication` |
| `AL-video` | 8026 | `videoservice` | 视频元数据、投稿上传、HLS 转码、视频统计、投币 | `com.video.VideoApplication` |
| `AL-playback` | 8027 | `playbackservice` | 观看历史、播放进度 | `com.video.PlaybackApplication` |
| `AL-comment` | 8028 | `commentservice` | 评论与回复 | `com.video.CommentApplication` |
| `AL-danmaku` | 8029 / WS 8030 | `danmakuservice` | 弹幕、视频房间 WebSocket | `com.video.DanmakuApplication` |
| `AL-social` | 8031 | `socialservice` | 关注/粉丝、收藏夹 | `com.video.SocialApplication` |
| `AL-datacenter` | 8032 | `datacenterservice` | 统计、推荐、标签聚合 BFF（无自有表） | `com.video.DataCenterApplication` |
| `AL-admin` | 8040 | `adminservice` | 管理端：审核、用户管理、对象存储、封面回填 | `com.video.AdminApplication` |
| `AL-remark` | 8021 | `remarkservice` | 点赞（Redis + MQ） | `com.video.remark.RemarkApplication` |
| `AL-communication` | 8023 / WS 8024 | `communicationservice` | 聊天室与私信 | `com.video.communication.CommunicationApplication` |

### 2.2 跨服务契约模块

| 模块 | 作用 |
| --- | --- |
| `AL-user-api` | 用户域内部契约：批量摘要、资料、角色、统计、会话吊销；标签内部契约 |
| `AL-video-api` | 视频域内部契约：批量摘要、单视频统计、UP 主视频统计、日汇总 |
| `AL-social-api` | 社交域内部契约：粉丝 id 集合、用户关注统计 |

### 2.3 公共模块与测试模块

| 模块 | 作用 |
| --- | --- |
| `AL-common-core` | 常量、`Result`、异常、登录用户上下文、JWT/JSON/密码工具 |
| `AL-common-web` | 全局异常处理、Cookie/Web 工具、MinIO/OSS 配置属性 |
| `AL-common-mq` | RabbitMQ 配置、`RabbitMqHelper`、延迟消息处理器 |
| `AL-common-redis` | `RedisTemplate` 配置 |
| `AL-common-mybatis` | MyBatis-Plus 分页插件（不写死 Mapper 包名） |
| `AL-api` | 跨服务共享的 entity / DTO |
| `AL-auth` / `AL-auth-resource-sdk` | 鉴权 starter：登录拦截、`user-id` 透传、Feign 透传 |
| `AL-architecture-test` | ArchUnit 边界规则与 Mapper 表归属检查 |

## 3. 技术栈

- JDK 17；Maven 3.9+（仓库没有 `mvnw`）
- Spring Boot 3.0.5；Spring Cloud 2022.0.4；Spring Cloud Alibaba 2022.0.0.0-RC2
- MyBatis-Plus 3.5.3.1；MySQL 8.0.33；Redis；RabbitMQ
- MinIO 8.5.12；Netty 4.1.100；JJWT 0.11.2；Knife4j 4.1.0；Hutool 5.8.16
- ffmpeg / ffprobe（视频转 HLS、图片派生）

## 4. 环境准备

### 4.1 基础中间件

- MySQL 8.x：新环境按 `../db/schema/` 分库脚本初始化（`al_user` / `al_video` / `al_playback` / `al_comment` / `al_danmaku` / `al_social` / `al_communication` / `al_remark`）；已有 `video` 单库按 `../db/migrations/phase5/README.md` 执行分库迁移（停机窗口 + 全库快照 + 同实例 `RENAME TABLE`）
- Redis 6.x+：使用 5 号库
- RabbitMQ：vhost `AniLink`
- MinIO：桶 `avatar`、`video`、`video-cover`
- Nacos：注册中心 + 配置中心（默认端口 8848）
- ffmpeg：本机可执行文件，或用 `AL_FFMPEG_PATH` 指定

### 4.2 Nacos 共享配置

各服务的 `bootstrap.yml` 会拉取以下 `dataId`（`DEFAULT_GROUP`，`yaml` 格式）：

| dataId | 内容 |
| --- | --- |
| `shared-spring.yaml` | 公共 Spring 配置（含服务发现） |
| `shared-mysql.yaml` | 数据源账号/口令/驱动 |
| `shared-redis.yaml` | Redis 地址、端口、口令、`database: 5` |
| `shared-mq.yaml` | RabbitMQ 地址、端口、`virtual-host: AniLink`、账号 |

示例（把地址与口令换成你自己的，不要提交真实口令）：

```yaml
# shared-mysql.yaml
spring:
  datasource:
    username: <mysql-user>
    password: <mysql-password>
    driver-class-name: com.mysql.cj.jdbc.Driver
```

```yaml
# shared-redis.yaml
spring:
  data:
    redis:
      host: <redis-host>
      port: 6379
      password: <redis-password>
      database: 5
```

```yaml
# shared-mq.yaml
spring:
  rabbitmq:
    host: <rabbitmq-host>
    port: 5672
    virtual-host: AniLink
    username: <mq-user>
    password: <mq-password>
```

各 `bootstrap.yml` 里的 `spring.cloud.nacos.server-addr` 默认是本机/内网开发地址，换环境时改成你的 Nacos 地址。

### 4.3 环境变量

| 变量 | 必填 | 说明 |
| --- | --- | --- |
| `MINIO_ENDPOINT` | AL-video / AL-admin 必填 | MinIO 地址，如 `http://localhost:9000` |
| `MINIO_ACCESSKEY` | AL-video / AL-admin 必填 | MinIO access key |
| `MINIO_SECRETKEY` | AL-video / AL-admin 必填 | MinIO secret key |
| `AUTH_JWT_SECRET` | 建议必填 | HS256 密钥，≥32 字节；**AL-gateway 与 AL-user 必须完全一致** |
| `AUTH_ACCESS_TTL` | 否 | access token 有效期，秒，默认 `1800` |
| `AUTH_REFRESH_TTL` | 否 | refresh token 有效期，秒，默认 `604800` |
| `AUTH_PREV_GRACE` | 否 | 刷新轮换宽限，秒，默认 `5` |
| `AUTH_MAX_SESSIONS` | 否 | 单用户最大会话数，默认 `3` |
| `AUTH_SECURE_COOKIE` | 否 | HTTPS 部署置 `true` |
| `AL_FFMPEG_PATH` | 否 | ffmpeg 路径；不填用配置里的默认值（AL-video / AL-admin） |
| `AL_FFPROBE_PATH` | 否 | ffprobe 路径；为空时取 ffmpeg 同目录 |
| `AL_TRANSCODE_ENABLED` | 否 | 是否启用转码，默认 `true` |
| `AL_TRANSCODE_RECOVER` | 否 | 启动时补跑待转码稿件，默认 `true` |
| `AL_VIDEO_URL` / `AL_SOCIAL_URL` 等 | 否 | 内部契约直连地址；不填走 Nacos 服务发现 |

PowerShell 示例：

```powershell
$env:MINIO_ENDPOINT="http://localhost:9000"
$env:MINIO_ACCESSKEY="<access-key>"
$env:MINIO_SECRETKEY="<secret-key>"
$env:AUTH_JWT_SECRET="<32-bytes-or-longer-random-string>"
$env:AL_FFMPEG_PATH="C:\ffmpeg\bin\ffmpeg.exe"
```

## 5. 构建与启动

### 5.1 构建

```powershell
cd AniLink-Java
mvn -q -DskipTests install
```

首次或改过 `AL-api` / `AL-common-*` / `AL-auth` / `AL-*-api` 后需要重新 `install`，否则其他模块可能用到旧 jar。

### 5.2 启动顺序

#### 5.2.1 先起基础设施

顺序不能反：**Nacos 必须先于所有业务服务**（注册与共享配置），MySQL/Redis/RabbitMQ/MinIO 必须先于使用它们的服务。

| 依赖 | 默认地址 | 被谁使用 |
| --- | --- | --- |
| Nacos | `192.168.150.102:8848` | 全部服务（注册 + `shared-*.yaml`） |
| MySQL | `127.0.0.1:3306`，库见第 8 节 | user / video / playback / comment / danmaku / social / communication / remark |
| Redis | DB 5 | user / video / playback / social / danmaku / communication |
| RabbitMQ | vhost `AniLink` | video / social / remark / communication |
| MinIO | `MINIO_*` 环境变量 | video（封面/头像/HLS）、admin（存储管理） |
| ffmpeg | PATH 或 `AL_FFMPEG_PATH` | video（HLS 转码、图片派生）、admin（封面回填） |

#### 5.2.2 再起业务服务（11 个）

| 顺序 | 模块 | 端口 | Nacos 名 | 启动前置 |
| --- | --- | --- | --- | --- |
| 1 | `AL-user` | 8025 | `userservice` | `al_user` 库、Redis |
| 2 | `AL-video` | 8026 | `videoservice` | `al_video` 库、MinIO、ffmpeg、MQ |
| 3 | `AL-playback` | 8027 | `playbackservice` | `al_playback` 库、Redis |
| 4 | `AL-social` | 8031 | `socialservice` | `al_social` 库、MQ |
| 5 | `AL-comment` | 8028 | `commentservice` | `al_comment` 库 |
| 6 | `AL-danmaku` | 8029 / WS 8030 | `danmakuservice` | `al_danmaku` 库 |
| 7 | `AL-datacenter` | 8032 | `datacenterservice` | 无自有库；可选 Flask 引擎 |
| 8 | `AL-admin` | 8040 | `adminservice` | 无业务库；MinIO、ffmpeg |
| 9 | `AL-remark` | 8021 | `remarkservice` | `al_remark` 库、MQ |
| 10 | `AL-communication` | 8023 / WS 8024 | `communicationservice` | `al_communication` 库、Redis、MQ |
| 11 | `AL-gateway` | 8090 | `gatewayservice` | Nacos；其余服务已注册 |

**顺序限制说明**：

- 1–10 之间**没有强制先后**：服务间调用走 Nacos 服务名 + Feign，启动期不做强依赖调用；`UserBriefService`/`VideoBriefService` 这类摘要缓存失败会降级为空，不阻塞启动。
- **网关放最后**最稳：它本身只依赖 Nacos，但服务尚未注册时请求会得到 503；先起网关也能运行，等服务注册后自动可路由。
- `AL-video` **只能单副本**：ffmpeg 转码队列在进程内单线程，多副本会重复转码、争用 `transcode_status`。
- `AL-admin` 没有 `spring.datasource`，不要给它配置业务库；它通过 `AL-user`/`AL-video` 的 `/internal/v1/admin/**` 读写。
- `AL-datacenter` 没有自有表；Flask 引擎未启动时推荐/标签接口按设计返回空结果，不影响其它链路。
- `AL-api`、`AL-*-api`、`AL-common-*`、`AL-auth` 是库模块，没有启动类。
- Python 引擎（`AniLink-AlgorithmEngine`，5000）当前**暂不要启动**：它跨 `al_user`/`al_video`/`al_danmaku` 读取，分库后需先完成跨库改造或只读账号（Phase 5-C）。

#### 5.2.3 一键启动脚本

**双击启动**：直接双击 `AniLink-Java/start-all.bat` 即可（等价于下面的 PowerShell 命令）；双击 `stop-all.bat` 停止本脚本启动的服务。首次启动会先 `mvn install`，需要几分钟，窗口会显示进度；服务在后台运行，关闭窗口不影响。

命令行参数（`start-all.bat` 会把参数原样传给 `scripts/start-all.ps1`）：

```powershell
cd AniLink-Java
.\start-all.bat                          # 构建 + 最多 3 个并发启动（默认）
.\start-all.bat -SkipBuild               # jar 已构建，跳过 mvn install
.\start-all.bat -MaxParallel 2           # 更保守，最多 2 个并发
.\start-all.bat -Only AL-user,AL-video
.\stop-all.bat                           # 停止
```

底层脚本：`scripts/start-all.ps1`，封装「构建 → 分批启动 → 端口检查 → 结果汇总」。

- `-MaxParallel` 被**硬限制在 1–3**（`ValidateRange`），每批服务端口就绪后才进入下一批，避免 11 个 JVM 同时初始化打爆内存；
- 编译是一次 Maven 进程（`mvn -q -DskipTests install`），不会按模块并发编译；启动才是最多 3 个并发；
- 每个服务默认 `-Xmx160m`，可用 `-HeapMb` 调整；
- 日志：`logs/start-all/<模块>.out.log` / `.err.log`，PID 文件 `logs/start-all/pids.json`；
- 端口已占用时视为“已启动”直接跳过，不会重复拉起；
- 启动失败会打印该服务日志尾部并以非 0 退出，便于 CI/脚本判断。

#### 5.2.4 手动启动示例

```powershell
# 任选 IDEA 运行对应启动类，或命令行：
mvn -q -pl AL-user spring-boot:run
mvn -q -pl AL-video spring-boot:run
mvn -q -pl AL-datacenter spring-boot:run
mvn -q -pl AL-gateway spring-boot:run

# 低内存机器可限制堆（11 个服务 × 128m 约 1.4GB）：
java -Xmx128m -jar AL-user/target/AL-user-1.0-SNAPSHOT.jar
```

启动前确认：`AL-video`/`AL-admin` 已设置 `MINIO_*`；`AL-gateway` 与 `AL-user` 使用同一份 `AUTH_JWT_SECRET`；`AL_FFMPEG_PATH` 指向可执行的 ffmpeg。

### 5.3 前端联调

- 新前端 `AniLink-web`：`pnpm dev`（5173），`/api` 由 Vite 代理到网关 8090。
- 管理端 `AniLink-admin`：`npm run dev`（5174），同样通过 `/api` 代理到 8090，入口 `/main/admin/**`；管理员 `user.role` 必须为 1 或 2。
- 旧前端 `AniLink-Vue3`：`npm run serve`（8080），只作对照。
- 直连网关时请求路径是 `/main/**` 或 `/remark/**`，不要带前端的 `/api` 前缀。

## 6. 网关与接口

网关路由配置在 `AL-gateway/src/main/resources/routes.yml`，由 `application.yml` 的 `spring.config.import` 加载，共 12 条：

| 路径 | 目标服务 | order |
| --- | --- | --- |
| `/main/login`、`/main/login/**`、`/main/info`、`/main/logout`、`/main/user/**`、`/main/public/refresh` | `lb://userservice` | -1 |
| `/main/history/**`、`/main/playback/**`、`/main/video/interact/record/add` | `lb://playbackservice` | -2 |
| `/main/video/public/getRealTimeRecommend` | `lb://datacenterservice` | -2 |
| `/main/video/**`、`/main/file/**` | `lb://videoservice` | -1 |
| `/main/danmu/getSimilarUsers`、`/main/danmu/filterUserByTags`、`/main/danmu/getCommonTags`、`/main/danmu/getTagRankList` | `lb://datacenterservice` | -2 |
| `/main/comment/**` | `lb://commentservice` | -1 |
| `/main/danmu/**` | `lb://danmakuservice` | -1 |
| `/main/fans/**`、`/main/favorite/**` | `lb://socialservice` | -1 |
| `/main/chat/**` | `lb://communicationservice` | -1 |
| `/main/dataCenter/**` | `lb://datacenterservice` | -1 |
| `/main/admin/**` | `lb://adminservice` | -1 |
| `/remark/**` | `lb://remarkservice` | 0 |

`AL-remark` 提供 `/remark/like`（点赞/取消）与 `/remark/get/like`（点赞状态）。

服务间内部契约（网关不暴露）：

| 服务 | 契约 | 调用方 |
| --- | --- | --- |
| `AL-user` | `/internal/v1/users/**`（批量摘要、资料、角色、统计、会话吊销） | AL-video / AL-social / AL-datacenter / AL-admin |
| `AL-video` | `/internal/v1/videos/**`（批量摘要、单视频统计、UP 主统计、日汇总） | AL-social / AL-datacenter / AL-admin / AL-user |
| `AL-social` | `/internal/v1/social/**`（粉丝 id、关注统计、默认收藏夹） | AL-datacenter / AL-user |

## 7. 鉴权

- 网关只认 access token 并注入 `user-id`；`/main/admin/**` 由 `AL-admin` 的 `AdminAuthInterceptor` 再查一次 `user.role`，普通用户返回 309。
- 管理端角色调整只允许 role=2，且不能修改自己。
- **access-token**：请求头 `access-token`，默认 30 分钟；网关校验签名、过期与 `typ=access`，通过后向下游注入 `user-id`。
- **refresh-token**：HttpOnly Cookie `refresh-token`，默认 7 天；只能用于 `/main/public/refresh`，按 jti 轮换（5 秒宽限，重用即吊销全端）。
- **状态码**：305 未登录、601 过期可续约、602 非法需重登。网关以 JSON `code` 返回；下游 `LoginAuthInterceptor` 在缺少身份时也会返回 HTTP 305。
- 网关会清除客户端自带的 `user-id`、`X-Real-IP`、`X-Forwarded-For`，只透传校验后的值；业务服务不要信任请求参数里的 uid 作为授权依据。
- 免登录路径统一在 `AL-common-core` 的 `AuthPaths` 维护：`/main/login/**`、`/main/user/register`、`/main/public/refresh`、`/**/public/**`。
- `AL-auth-resource-sdk` 通过 `al.auth.resource.enable` 开关启用登录拦截；`/internal/v1/**` 由各服务 `al.auth.resource.exclude-login-paths` 放行给服务间 Feign。
- `AL-gateway` 与 `AL-user` 的 JWT 密钥/有效期必须一致。

## 8. 数据与存储

表归属（Phase 5 分库：每张表只有一个属主服务，一个服务只连自己的库）：

| 服务 | 目标库 | 自有表 |
| --- | --- | --- |
| `AL-user` | `al_user` | `user`、`user_stats`、`user_tag` |
| `AL-video` | `al_video` | `video`、`video_stats`、`user_video`、`category` |
| `AL-playback` | `al_playback` | `bh_video` |
| `AL-comment` | `al_comment` | `comment` |
| `AL-danmaku` | `al_danmaku` | `danmu`、`sub_danmu` |
| `AL-social` | `al_social` | `follow`、`favorite`、`favorite_video` |
| `AL-communication` | `al_communication` | `chat`、`chat_message` |
| `AL-remark` | `al_remark` | 无（Redis + MQ） |
| `AL-admin` | 无业务库 | 走 `AL-user` / `AL-video` 的 `/internal/v1/admin/**` 管理契约 |
| `AL-datacenter` | 无业务库 | BFF，聚合走 Feign |

**分库已在本机开发库执行**（2026-11-28，16 张表同实例 `RENAME`，行数逐表核对一致；迁移前快照在 `db/temp/`，已 gitignore）。
脚本与 runbook 保留在 `../db/migrations/phase5/`，用于新环境初始化、其它环境迁移与回滚；新环境也可直接用 `../db/schema/` 建空库。

| 类型 | 位置 | 用途 |
| --- | --- | --- |
| MySQL | 各服务自有库（见上表） | 用户、视频、评论、弹幕、收藏、历史、聊天等 |
| MySQL | `al_remark` | AL-remark（当前无实际表读写） |
| Redis | 5 号库 | 登录态/黑名单、验证码、点赞集合与计数、播放进度缓存、热搜 |
| RabbitMQ | vhost `AniLink` | 点赞计数、收藏事件、异常消息、用户画像 |
| MinIO | `avatar` / `video` / `video-cover` | 头像、视频原文件与 HLS、封面 |

HLS 产物在 `video` 桶的 `hls/{vid}/index.m3u8`，前端优先播 `hlsUrl`，未转完或播放失败回退 `videoUrl`（MP4 + Range）。

## 9. 视频转码

- 由 `AL-video` 执行，投稿后异步转码，单线程队列，避免并发 ffmpeg 拖垮机器。
- 状态写 `video.transcode_status`：`0` 待转、`1` 转中、`2` 完成、`3` 失败；完成后写 `video.hls_url`。
- 启动时会自动补跑 `transcode_status=0` 的稿件，**不要手工改这一列**。
- 码率按“源码率 × 1.15，且不超过 `al.transcode.max-bitrate-kbps`”控制；ffprobe 读不到源码率时用配置上限。
- 相关配置在 `AL-video/src/main/resources/application.yml` 的 `al.transcode.*`，路径可用 `AL_FFMPEG_PATH` 覆盖。

## 10. 测试与架构护栏

```powershell
cd AniLink-Java
mvn test
```

- 当前测试覆盖：JWT/白名单、网关身份头与路由配置、登录契约快照（成功/307/601）、用户内部契约、视频统计/收藏事件/转码、社交收藏事件、ArchUnit 服务边界、Mapper 表归属。
- `AL-architecture-test` 用 ArchUnit 约束服务包不得依赖其他服务的实现包，并检查 Mapper XML 只引用自有表。
- 契约测试快照在 `AL-user/src/test/resources/contracts/`，新增/修改接口时同步更新。

## 11. 当前迁移状态与注意事项

- Phase 0–5 已完成：公共模块拆分、网关身份透传与路由外置、11 个可启动服务（含网关与管理端）拆分、`AL-web` 删除、MySQL 按服务分库。
- `/internal/v1/**` 只用于服务间 Feign，网关不暴露；新增跨域数据一律走内部契约 + 批量接口，禁止跨域 JOIN。
- MySQL 已按服务分库（Phase 5）：各服务 `application.yml` 指向自己的库，跨域数据只走内部契约；库名/表归属、账号模板与迁移 runbook 见 `../db/README.md` 与 `../db/migrations/phase5/README.md`。
- 当前开发库已执行分库（16 表 RENAME，快照在 `db/temp/`）；脚本保留用于新环境与回滚，不再需要重复执行。
- 画像引擎仍按单库连接读多域表（`user_tag` / `user_video` / `video` / `danmu`），分库后需要单独的跨库改造（Phase 5-C），在此之前不要启动引擎写入画像标签。
- 新环境按 `../db/schema/` 初始化分库后，直接以各服务 `application.yml` 的默认数据源启动即可；只有未迁移的旧环境才需要 `SPRING_DATASOURCE_URL` 临时指回 `video` 库。
- `AL-video` 的转码队列是进程内单实例模型，**不要同时启动多个 AL-video 实例**；投稿只能由 `AL-video` 处理。
- 对外只暴露网关 8090；业务服务端口（8021–8040）仅供本机/内网开发与调试。
- 升级依赖版本、改 Nacos 共享配置或公共口令前先确认影响面。

## 12. 常见问题

| 现象 | 排查 |
| --- | --- |
| 启动卡在 Nacos / 报配置拉取失败 | 确认 Nacos 已启动、`server-addr` 正确、四个 `shared-*.yaml` 已创建 |
| 登录后访问接口返回 305/601/602 | 检查 `AUTH_JWT_SECRET` 在 gateway 与 user 是否一致；access token 是否过期；refresh Cookie 是否带上 |
| 服务间 Feign 调用 404/503 | 确认被调服务已启动并注册到 Nacos，或配置 `AL_VIDEO_URL` / `AL_SOCIAL_URL` 等直连地址 |
| 上传/头像/视频失败 | 检查 `MINIO_*` 环境变量、桶是否创建、MinIO 是否可达（AL-video / AL-admin） |
| 投稿后一直不转码 | 检查 `AL_FFMPEG_PATH`、ffmpeg 是否可执行、`video.transcode_status` 与 AL-video 日志 |
| `mvn` 命令不存在 | 用 IDEA 内置 Maven，或把 Maven `bin` 加入 `PATH` |
| 端口冲突 | 8021/8023/8024/8025/8026/8027/8028/8029/8030/8031/8032/8040/8090 是否被占用 |

## 13. 相关文档

- 问题日志：[docs/问题日志.md](docs/问题日志.md)
- 数据库初始化与分库：[../db/README.md](../db/README.md)、分库前基线 [../db/schema.sql](../db/schema.sql)、分库后建表 [../db/schema/](../db/schema/)、迁移 runbook [../db/migrations/phase5/README.md](../db/migrations/phase5/README.md)
- 架构与设计图：[docs/Assets/AniLink-cloud/](docs/Assets/AniLink-cloud/)
- 微服务拆分设计：[../docs/superpowers/specs/2026-11-24-backend-service-split-design.md](../docs/superpowers/specs/2026-11-24-backend-service-split-design.md)
- 拆分变更与任务：[../openspec/changes/refactor-backend-service-split/](../openspec/changes/refactor-backend-service-split/)
- 新前端：[../AniLink-web/README.md](../AniLink-web/README.md)
- 管理端：[../AniLink-admin/README.md](../AniLink-admin/README.md)
- 仓库总说明：[../AGENTS.md](../AGENTS.md)
