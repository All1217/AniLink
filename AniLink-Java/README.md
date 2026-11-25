# AniLink Java 后端（微服务版）

AniLink 是一个仿 B 站（bilibili）的视频社区全栈项目。本目录是后端 Maven 多模块工程，基于 Spring Boot 3.0.5 + Spring Cloud Alibaba，包根 `com.video`。

当前处于微服务拆分 **Phase 0 之后**：公共模块与网关基座已完成，`AL-web` 仍承载全部业务域；`al-user`、`al-video`、`al-playback`、`al-social`、`al-comment`、`al-danmaku`、`al-datacenter` 在 Phase 1–4 按需创建。完整的拆分设计见 [OpenSpec 变更](../openspec/changes/refactor-backend-service-split/) 与 [设计文档](../docs/superpowers/specs/2026-11-24-backend-service-split-design.md)。

## 1. 当前架构

```text
浏览器 / AniLink-web (5173, /api 代理)
        │
        ▼
AL-gateway (8090)  ── /main/**   ──▶ AL-web (8022)
        │          ── /remark/** ──▶ AL-remark (8021)
        │
        └── 直接连接：AL-communication (8023 + Netty WS 8024)

MySQL / Redis / RabbitMQ / MinIO / Nacos 由各服务共享
```

- 网关只做路由与 access token 校验；`/main/**`、`/remark/**` 之外的路径不对外。
- 鉴权走双令牌：access token 放请求头，refresh token 放 HttpOnly Cookie。
- 点赞、播放进度走 Redis + RabbitMQ 异步落库；视频投稿后异步转 HLS。
- 推荐/画像由 `AniLink-AlgorithmEngine`（Flask，5000）提供，后端通过 HTTP 调用。

## 2. 模块与端口

### 2.1 当前可启动的服务（4 个）

| 模块 | 端口 | 作用 | 启动类 |
| --- | --- | --- | --- |
| `AL-gateway` | 8090 | 网关、鉴权、路由 | `com.video.gateway.GatewayApplication` |
| `AL-communication` | 8023 / WS 8024 | 聊天与私信 WebSocket | `com.video.communication.CommunicationApplication` |
| `AL-remark` | 8021 | 点赞 | `com.video.remark.RemarkApplication` |
| `AL-web` | 8022 | 视频/用户/评论/弹幕/粉丝/收藏/历史/数据中心（迁移期兜底） | `com.video.VideoWebApplication` |

### 2.2 规划中的服务（Phase 1–4 创建）

| 服务 | 端口 | 作用 | 计划 |
| --- | --- | --- | --- |
| `AL-user` | 8025 | 账号、资料、统计、标签 | Phase 1 |
| `AL-video` | 8026 | 视频、投稿、转码、统计、投币 | Phase 2 |
| `AL-playback` | 8027 | 历史、播放进度 | Phase 2 |
| `AL-comment` | 8028 | 评论与回复 | Phase 3 |
| `AL-danmaku` | 8029 / WS 8030 | 弹幕、视频房间 WebSocket | Phase 3 |
| `AL-social` | 8031 | 关注/粉丝、收藏夹 | Phase 3 |
| `AL-datacenter` | 8032 | 统计、推荐、标签聚合 BFF | Phase 4 |

> 这些模块目前**不存在**，不会出现在 Maven reactor 或 IDE 模块列表中；对应 Phase 开始时再创建，避免空脚手架。

### 2.3 公共模块与测试模块

| 模块 | 作用 |
| --- | --- |
| `AL-common-core` | 常量、`Result`、异常、登录用户上下文、JWT/JSON/密码工具 |
| `AL-common-web` | 全局异常处理、Cookie/Web 工具、MinIO/OSS 配置属性 |
| `AL-common-mq` | RabbitMQ 配置、`RabbitMqHelper`、延迟消息处理器 |
| `AL-common-redis` | `RedisTemplate` 配置 |
| `AL-common-mybatis` | MyBatis-Plus 分页插件（不写死 Mapper 包名） |
| `AL-api` | 跨服务共享的 entity / DTO（迁移期保留） |
| `AL-auth` / `AL-auth-resource-sdk` | 鉴权 starter：登录拦截、`user-id` 透传、Feign 透传 |
| `AL-architecture-test` | ArchUnit 边界规则（服务包不得互相依赖） |

## 3. 技术栈

- JDK 17；Maven 3.9+（仓库没有 `mvnw`）
- Spring Boot 3.0.5；Spring Cloud 2022.0.4；Spring Cloud Alibaba 2022.0.0.0-RC2
- MyBatis-Plus 3.5.3.1；MySQL 8.0.33；Redis；RabbitMQ
- MinIO 8.5.12；Netty 4.1.100；JJWT 0.11.2；Knife4j 4.1.0；Hutool 5.8.16
- ffmpeg / ffprobe（视频转 HLS）

## 4. 环境准备

### 4.1 基础中间件

- MySQL 8.x：建库 `video`、`al_remark`、`al_communication`
- Redis 6.x+：使用 5 号库
- RabbitMQ：vhost `AniLink`
- MinIO：桶 `avatar`、`video`、`video-cover`
- Nacos：注册中心 + 配置中心（默认端口 8848）
- ffmpeg：本机可执行文件，或用 `AL_FFMPEG_PATH` 指定

### 4.2 Nacos 共享配置

各服务的 `bootstrap.yml` 会拉取以下 `dataId`（`DEFAULT_GROUP`，`yaml` 格式）：

| dataId | 内容 |
| --- | --- |
| `shared-spring.yaml` | 公共 Spring 配置 |
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
| `MINIO_ENDPOINT` | AL-web 必填 | MinIO 地址，如 `http://localhost:9000` |
| `MINIO_ACCESSKEY` | AL-web 必填 | MinIO access key |
| `MINIO_SECRETKEY` | AL-web 必填 | MinIO secret key |
| `AUTH_JWT_SECRET` | 建议必填 | HS256 密钥，≥32 字节；**AL-gateway 与 AL-web 必须完全一致** |
| `AUTH_ACCESS_TTL` | 否 | access token 有效期，秒，默认 `1800` |
| `AUTH_REFRESH_TTL` | 否 | refresh token 有效期，秒，默认 `604800` |
| `AUTH_PREV_GRACE` | 否 | 刷新轮换宽限，秒，默认 `5` |
| `AUTH_MAX_SESSIONS` | 否 | 单用户最大会话数，默认 `3` |
| `AUTH_SECURE_COOKIE` | 否 | HTTPS 部署置 `true` |
| `AL_FFMPEG_PATH` | 否 | ffmpeg 路径；不填用配置里的本机默认值 |
| `AL_FFPROBE_PATH` | 否 | ffprobe 路径；为空时取 ffmpeg 同目录 |
| `AL_TRANSCODE_ENABLED` | 否 | 是否启用转码，默认 `true` |
| `AL_TRANSCODE_RECOVER` | 否 | 启动时补跑待转码稿件，默认 `true` |

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

首次或改过 `AL-api` / `AL-common-core` / `AL-auth` 后需要重新 `install`，否则其他模块可能用到旧 jar。

### 5.2 启动顺序

先启动 Nacos、MySQL、Redis、RabbitMQ、MinIO，然后：

1. `AL-gateway`（8090）
2. `AL-communication`（8023 / WS 8024）
3. `AL-remark`（8021）
4. `AL-web`（8022）

用 Maven 启动示例：

```powershell
mvn -q -pl AL-gateway spring-boot:run
mvn -q -pl AL-communication spring-boot:run
mvn -q -pl AL-remark spring-boot:run
mvn -q -pl AL-web spring-boot:run
```

也可以在 IDEA 中直接运行第 2 节的四个启动类。`AL-web` 启动前确认 `MINIO_*` 与 `AUTH_JWT_SECRET` 已设置。

### 5.3 前端联调

用户端：`AniLink-web`（5173）通过 `/api` 代理到网关 8090。

管理端：`AniLink-admin`（5174）同样通过 `/api` 代理到 8090，入口 `/main/admin/**`；管理员账号 `user.role` 必须为 1 或 2。当前 admin 是 `AL-web` 内的 `com.video.web.admin` 模块，不单独启动服务。

- 新前端 `AniLink-web`：`pnpm dev`（5173），`/api` 由 Vite 代理到网关 8090。
- 旧前端 `AniLink-Vue3`：`npm run serve`（8080），只作对照。
- 直连网关时请求路径是 `/main/**` 或 `/remark/**`，不要带前端的 `/api` 前缀。

## 6. 网关与接口

网关路由配置在 `AL-gateway/src/main/resources/routes.yml`，由 `application.yml` 的 `spring.config.import` 加载：

| 路径 | 目标服务 |
| --- | --- |
| `/main/**` | `lb://webservice`（AL-web） |
| `/remark/**` | `lb://remarkservice`（AL-remark） |

当前 `AL-web` 内的接口分组：

| 前缀 | 作用 |
| --- | --- |
| `/main/login`、`/main/info`、`/main/logout`、`/main/public/refresh` | 登录、当前用户、登出、刷新令牌 |
| `/main/user/**` | 注册、资料、统计、标签 |
| `/main/video/**` | 视频分页/详情、推荐、投币、播放进度上报 |
| `/main/history/**` | 观看历史 |
| `/main/comment/**` | 评论与回复 |
| `/main/danmu/**` | 弹幕发送/列表/相似用户/标签过滤 |
| `/main/fans/**` | 关注、粉丝列表 |
| `/main/favorite/**` | 收藏夹与收藏关系 |
| `/main/chat/**` | 聊天室与私信消息 |
| `/main/file/**` | 头像/封面/视频上传与投稿 |
| `/main/dataCenter/**` | 数据中心与热搜 |

`AL-remark` 提供 `/remark/like`（点赞/取消）与 `/remark/get/like`（点赞状态）。

## 7. 鉴权

- 网关只认 access token 并注入 `user-id`；`/main/admin/**` 由 `AdminAuthInterceptor` 再查一次 `user.role`，普通用户返回 309。
- 管理端角色调整只允许 role=2，且不能修改自己。

- **access-token**：请求头 `access-token`，默认 30 分钟；网关校验签名、过期与 `typ=access`，通过后向下游注入 `user-id`。
- **refresh-token**：HttpOnly Cookie `refresh-token`，默认 7 天；只能用于 `/main/public/refresh`，按 jti 轮换（5 秒宽限，重用即吊销全端）。
- **状态码**：305 未登录、601 过期可续约、602 非法需重登。网关以 JSON `code` 返回；下游 `LoginAuthInterceptor` 在缺少身份时也会返回 HTTP 305。
- 网关会清除客户端自带的 `user-id`、`X-Real-IP`、`X-Forwarded-For`，只透传校验后的值；业务服务不要信任请求参数里的 uid 作为授权依据。
- 免登录路径统一在 `AL-common-core` 的 `AuthPaths` 维护：`/main/login/**`、`/main/user/register`、`/main/public/refresh`、`/**/public/**`。
- `AL-auth-resource-sdk` 通过 `al.auth.resource.enable` 开关启用登录拦截；`AL-gateway` 与 `AL-web` 的 JWT 密钥/有效期必须一致。

## 8. 数据与存储

| 类型 | 位置 | 用途 |
| --- | --- | --- |
| MySQL | `video` | AL-web：用户、视频、评论、弹幕、收藏、历史、聊天等 |
| MySQL | `al_remark` | AL-remark（当前无实际表读写） |
| MySQL | `al_communication` | AL-communication（当前无实际表读写） |
| Redis | 5 号库 | 登录态/黑名单、验证码、点赞集合与计数、播放进度缓存、热搜 |
| RabbitMQ | vhost `AniLink` | 点赞计数、异常消息、用户画像 |
| MinIO | `avatar` / `video` / `video-cover` | 头像、视频原文件与 HLS、封面 |

HLS 产物在 `video` 桶的 `hls/{vid}/index.m3u8`，前端优先播 `hlsUrl`，未转完或播放失败回退 `videoUrl`（MP4 + Range）。

## 9. 视频转码

- 投稿后异步执行，单线程队列，避免并发 ffmpeg 拖垮机器。
- 状态写 `video.transcode_status`：`0` 待转、`1` 转中、`2` 完成、`3` 失败；完成后写 `video.hls_url`。
- 启动时会自动补跑 `transcode_status=0` 的稿件，**不要手工改这一列**。
- 码率按“源码率 × 1.15，且不超过 `al.transcode.max-bitrate-kbps`”控制；ffprobe 读不到源码率时用配置上限。
- 相关配置在 `AL-web/src/main/resources/application.yml` 的 `al.transcode.*`，路径可用 `AL_FFMPEG_PATH` 覆盖。

## 10. 测试与架构护栏

```powershell
cd AniLink-Java
mvn test
```

- 当前测试覆盖：JWT/白名单、网关身份头与路由配置、登录契约快照（成功/307/601）、MapperScan 约束、ArchUnit 服务边界。
- `AL-architecture-test` 用 ArchUnit 约束服务包不得依赖其他服务的实现包；随着 Phase 1–4 服务模块创建，逐步把对应模块加进测试依赖。
- 契约测试快照在 `AL-web/src/test/resources/contracts/`，新增/修改接口时同步更新。

## 11. 当前迁移状态与注意事项

- Phase 0 已完成：`AL-common` 拆分为 5 个模块、网关身份透传修复、免登录路径集中、路由外置、契约测试基座、ArchUnit 规则。
- 管理端已加入 `AL-web` 的 `com.video.web.admin` 包（`/main/admin/**`），暂不独立成服务；等 `al-user`/`al-video` 落地后再抽成 `AL-admin` BFF。
- **服务拆分尚未开始**：`/main/**` 仍全部由 `AL-web` 处理；规划中的 7 个服务模块在 Phase 1–4 创建。
- `AL-web` 监听 `0.0.0.0:8022`，仅供本机/内网开发；对外只暴露网关 8090，避免绕过鉴权直连 8022。
- 不要手工修改 `video.transcode_status`；不要让多个实例同时跑 `AL-web` 的转码队列（当前是进程内单实例模型）。
- 升级依赖版本、改 Nacos 共享配置或公共口令前先确认影响面。

## 12. 常见问题

| 现象 | 排查 |
| --- | --- |
| 启动卡在 Nacos / 报配置拉取失败 | 确认 Nacos 已启动、`server-addr` 正确、四个 `shared-*.yaml` 已创建 |
| 登录后访问接口返回 305/601/602 | 检查 `AUTH_JWT_SECRET` 在 gateway 与 web 是否一致；access token 是否过期；refresh Cookie 是否带上 |
| 上传/头像/视频失败 | 检查 `MINIO_*` 环境变量、桶是否创建、MinIO 是否可达 |
| 投稿后一直不转码 | 检查 `AL_FFMPEG_PATH`、ffmpeg 是否可执行、`video.transcode_status` 与 AL-web 日志 |
| `mvn` 命令不存在 | 用 IDEA 内置 Maven，或把 Maven `bin` 加入 `PATH` |
| 端口冲突 | 8021/8022/8023/8024/8090 是否被占用；新服务端口见第 2 节 |

## 13. 相关文档

- 问题日志：[docs/问题日志.md](docs/问题日志.md)
- 架构与设计图：[docs/Assets/AniLink-cloud/](docs/Assets/AniLink-cloud/)
- 微服务拆分设计：[../docs/superpowers/specs/2026-11-24-backend-service-split-design.md](../docs/superpowers/specs/2026-11-24-backend-service-split-design.md)
- 拆分变更与任务：[../openspec/changes/refactor-backend-service-split/](../openspec/changes/refactor-backend-service-split/)
- 新前端：[../AniLink-web/README.md](../AniLink-web/README.md)
- 管理端：[../AniLink-admin/README.md](../AniLink-admin/README.md)
- 仓库总说明：[../AGENTS.md](../AGENTS.md)
