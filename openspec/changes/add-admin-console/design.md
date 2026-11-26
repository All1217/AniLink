# Design

## Context

- `AL-web` 目前同时承载用户、视频、评论、弹幕、收藏、历史、聊天与数据中心；`al-user`/`al-video` 等服务尚未创建。
- `user` 表已有 `status`（0 正常 / 1 封禁 / 2 注销）与 `role`（0 普通 / 1 管理员 / 2 超级管理员），但登录与现有接口都没有校验 role。
- `video` 表已有 `status`（0 审核中 / 1 过审 / 2 未通过 / 3 已删除）、`visible`（0 公开 / 1 仅自己）、`delete_date`，以及转码状态字段。
- MinIO 已有 `avatar`、`video`、`video-cover` 三个桶；`FileServiceImpl` 只做上传与拼接公开 URL，没有列表/删除/引用检查能力。
- 新前端 `AniLink-web` 没有 admin 路由；旧 `AniLink-Vue3` 也没有管理页面。管理员目前只能手工改库或进 MinIO 控制台。

## Goals / Non-Goals

**Goals:**

- 独立管理前端，管理员登录后只能看到管理功能。
- 管理接口按 `user.role` 做服务端校验，普通用户无法调用。
- 视频稿件按逻辑状态审核/下架/可见性管理，不触碰 MinIO 与转码。
- 对象存储可浏览、预览、删除；删除前做业务引用检查，避免误删播放资源。
- 不新增数据库表/字段，不新增可部署服务。

**Non-Goals:**

- 不在本变更内新建 `AL-admin` 微服务；等 `al-user`/`al-video` 落地后再抽取。
- 不管理评论/弹幕/聊天，不做内容编辑、密码重置、用户物理删除。
- 不做桶创建/删除、对象批量删除、生命周期策略、CDN 刷新。
- 不引入审计表；操作日志只写应用日志。

## Decisions

### D1 服务落点：先放 AL-web，后续抽 AL-admin

**结论：现在不单独成立 `AL-admin` 微服务。**

理由：

1. 当前 `al-user`/`al-video` 尚未创建。独立 `AL-admin` 要么直接访问 `video` 库（与 AL-web 双写、违反“一表一主”），要么依赖尚不存在的域服务接口。
2. 管理员是跨域 BFF 场景（用户 + 视频 + 存储），不是新的业务域；现在抽服务只会增加网关、端口、配置和跨服务调用，没有可拆的独立数据。
3. 管理端与用户端受众不同，但后端可以先用 `com.video.web.admin` 包隔离：controller/service/mapper 独立、路由 `/main/admin/**`、独立拦截器。等域服务就绪后，把该包整体迁到 `AL-admin`，接口契约不变。

抽取触发条件：`al-user` 与 `al-video` 已提供内部查询/状态变更接口，且管理端需要组合两个以上域的数据。届时 `AL-admin` 作为 BFF 调用域服务；MinIO 操作可留在 `AL-admin` 或独立 storage 适配器。

备选：
- 现在建 `AL-admin`：需要临时共享库或先在 AL-web 补管理接口，收益低、风险高，否决。
- 管理接口拆到各域服务：每个服务都要做管理员鉴权与分页，前端要聚合多次请求，否决。
- 不做管理端：继续手工改库/控制台，误操作与权限风险更高，否决。

### D2 管理员鉴权

- 复用 `user.role`：0 普通、1 管理员、2 超级管理员；不新增角色表。
- 网关仍只校验 access token 并注入 `user-id`；`/main/admin/**` 由 AL-web 的 `AdminAuthInterceptor` 再做一次角色检查（`UserInfoService.getById(uid).role >= 1`），失败抛 `ADMIN_ACCESS_FORBIDDEN(309)`。
- 管理端登录复用 `POST /main/login`；登录后调用 `GET /main/admin/me` 验证角色。role=0 时提示“无管理员权限”，前端清除本地 token，不进入管理页。
- 角色修改接口在 service 层再次校验 `role==2`，并拒绝修改自己的 role，避免自我提权/降级。
- 不把 role 写进 JWT：角色可能被超级管理员实时调整，JWT 内角色会滞后；管理接口低频，按请求查库可接受。

### D3 视频稿件管理

- 列表：`GET /main/admin/videos?current&size&status&uid&keyword`，默认排除 `status=3`；显式传 `status=3` 才查已删除。
- 详情：`GET /main/admin/videos/{vid}`，返回逻辑字段与作者信息；不返回 MinIO 密钥。
- 状态流转：`PUT /main/admin/videos/{vid}/status`，只允许 `0→1/2`、`1/2→3`；非法流转返回 409。下架写 `delete_date=now()`。
- 可见性：`PUT /main/admin/videos/{vid}/visible`，只允许 0/1。
- 该模块只更新 `video.status/visible/delete_date`；不修改 `title/descr/tags/video_url/cover_url/hls_url/transcode_status`，不调用 MinIO。
- 作者信息在 AL-web 内可直接 JOIN `user`；抽取 `AL-admin` 后改为批量调用用户服务。

### D4 用户管理

- 列表：`GET /main/admin/users?current&size&keyword&status&role`，按 uid/username/nickname 模糊搜索；响应 DTO 不含 password。
- 封禁/解封：`PUT /main/admin/users/{uid}/status`，只允许 0/1；拒绝操作自己。
- 角色：`PUT /main/admin/users/{uid}/role`，只允许 0/1/2，仅 role=2 可调用，拒绝修改自己。
- 不做物理删除、不重置密码、不展示 `password`/`delete_date` 以外的敏感字段。

### D5 对象存储管理

- 桶白名单：`avatar`、`video`、`video-cover`；其他 bucket 直接 400。
- 列表：`GET /main/admin/storage/objects?bucket&prefix&cursor&size`，使用 MinIO `listObjects`（递归或按分隔符），返回 key、size、lastModified；不返回对象内容。
- 预览/下载：`GET /main/admin/storage/objects/presign?bucket&key`，返回 5 分钟有效的预签名 GET URL；不返回 MinIO 永久密钥。
- 删除：`DELETE /main/admin/storage/objects?bucket&key`。
  - 先把对象 key 转成引用后缀 `/{bucket}/{key}`，检查：
    - `video.video_url`、`video.cover_url` 是否等于该 URL；
    - `user.avatar`、`user.background` 是否等于该 URL；
    - 若 key 位于 `video` 桶的 `hls/{vid}/` 下，检查是否存在 `vid` 且 `hls_url` 非空的稿件（HLS 目录整体视为被引用）。
  - 有任何引用：返回 409 + 引用列表（类型、vid/uid、标题/昵称、字段），不调用 MinIO 删除。
  - 无引用：调用 `removeObject`；失败返回 500，不报告成功。
- 一键清理：`POST /main/admin/storage/cleanup?bucket&prefix`。
  - 按当前 bucket + prefix 递归分页扫描全部对象，复用同一套引用检查；只删除无引用对象，被引用对象跳过。
  - 单个对象删除失败不中断整体，计入 `failed`/`failedKeys`；返回 `scanned/deleted/skipped/failed`。
  - HLS 按 vid 缓存引用结果，避免同一目录每个分片重复查稿件。
- 单对象删除只针对指定 key；一键清理只按引用检查删除无引用对象；不做桶创建/删除、对象覆盖上传，也不提供绕过引用检查的强制删除。
- 检查与删除之间存在竞态（检查后新稿件引用该对象）；管理员低频操作可接受，文档标注。

### D6 管理前端

- 新建独立 `AniLink-admin/`（Vite 6 + Vue 3.5 + Element Plus + Pinia；端口 5174；`/api` 代理到网关 8090）。
- 页面：登录、视频稿件（筛选/分页/详情/通过/驳回/下架/可见性）、用户（筛选/分页/封禁/解封/角色）、对象存储（桶切换/前缀/对象表/预览/删除确认/一键清理）。
- 复用登录/刷新协议：access token 放请求头，refresh token 走 HttpOnly Cookie；管理端只存 access token，不存 refresh token。
- 删除对象必须二次确认，并在 409 时展示引用方，不提供“强制删除”；一键清理必须二次确认，执行期间表格 loading 且禁止其他操作。
- 不做用户端页面改动；`AniLink-web` 保持原样。

### D7 API 一览

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| GET | `/main/admin/me` | 当前管理员身份与角色 |
| GET | `/main/admin/videos` | 稿件分页/筛选 |
| GET | `/main/admin/videos/{vid}` | 稿件详情 |
| PUT | `/main/admin/videos/{vid}/status` | 通过/驳回/下架 |
| PUT | `/main/admin/videos/{vid}/visible` | 公开/仅自己 |
| GET | `/main/admin/users` | 用户分页/筛选 |
| PUT | `/main/admin/users/{uid}/status` | 封禁/解封 |
| PUT | `/main/admin/users/{uid}/role` | 角色调整（仅超管） |
| GET | `/main/admin/storage/objects` | 对象列表 |
| GET | `/main/admin/storage/objects/presign` | 预览 URL |
| DELETE | `/main/admin/storage/objects` | 删除对象（引用检查） |
| POST | `/main/admin/storage/cleanup` | 一键清理当前桶/前缀下无引用对象 |

### D8 测试

- 后端：MockMvc + mock service，覆盖 role=0 访问 309、role=1 改角色 309、role=2 成功、视频状态非法流转 409、封禁自己/改自己角色拒绝、存储引用检查 409、bucket 白名单 400、未引用删除成功。
- 单元测试：对象 URL → `(bucket,key)` 解析、HLS 目录引用判断、状态流转校验。
- 前端：`vue-tsc --noEmit` + `vite build`；仓库暂无前端测试框架，不引入。
- 手工冒烟：普通用户 token 调 admin 接口 309；管理员审核/封禁/删除孤儿对象；删除被引用对象 409。

## Risks / Trade-offs

- **管理端误删对象** → 引用检查 + 二次确认 + 409 不提供强制删除；日志记录操作者/对象。
- **检查与删除竞态** → 管理员低频操作；后续可加“待清理”标记或延迟删除。
- **普通用户拿到 token 后调管理接口** → 服务端 role 拦截，不依赖前端隐藏菜单。
- **管理员角色调整影响登录** → 角色按请求查库；无需重新签发 token。
- **管理端与用户端 Cookie 共享** → 同域部署时 refresh cookie 共享；管理端只存 access token，退出不影响用户端（后续可拆分域名）。
- **AL-web 继续变重** → admin 包隔离，抽取 `AL-admin` 时整体迁移；本变更不新增表/服务。
- **大表分页性能** → `user`/`video` 当前数据量小；后续可加索引或迁移到独立查询服务。

## Migration Plan

1. 后端新增 admin 包与拦截器，部署 AL-web；`/main/admin/**` 对普通用户返回 309。
2. 新建 `AniLink-admin`，配置代理到网关；用 role=1/2 账号登录验证。
3. 先只上线只读列表/详情，再开放审核、封禁、删除；删除功能上线前确认引用检查覆盖 `video_url`/`cover_url`/`hls_url`/`avatar`/`background`。
4. 回滚：移除 admin 路由/前端入口即可；管理操作只改现有字段，数据可人工恢复（对象删除不可逆，依赖引用检查与确认）。

## Open Questions

- 是否需要在后续版本增加管理员操作审计表（当前只写应用日志）？
- `al-user`/`al-video` 落地后，`AL-admin` 是独立 BFF 还是并入 `al-datacenter`？本变更先按独立 BFF 预留包边界。
