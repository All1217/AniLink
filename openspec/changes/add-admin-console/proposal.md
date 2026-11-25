# Proposal

## Why

目前没有任何面向管理员的后台：视频审核、用户封禁、MinIO 对象清理都只能手工改库或进存储控制台，既没有权限校验，也无法保证删除对象不会破坏正在播放的视频。需要一个独立的管理端，把“逻辑稿件状态”和“真实存储对象”分开管理，并且只允许管理员访问。

## What Changes

- 新增独立管理前端 `AniLink-admin`（Vite + Vue 3 + Element Plus），包含登录、视频稿件、用户管理、对象存储四个页面。
- 在 `AL-web` 内新增 `com.video.web.admin` 模块，提供 `/main/admin/**` 接口；不新增可部署服务，后续 `al-user`/`al-video` 落地后再抽成 `AL-admin` BFF。
- 管理员身份复用 `user.role`（0 普通 / 1 管理员 / 2 超级管理员）；普通用户访问管理接口返回 309（无访问权限），角色变更仅超级管理员可操作。
- 视频稿件管理：分页/筛选/详情、审核通过/驳回（`video.status` 1/2）、下架/逻辑删除（3 + `delete_date`）、公开/仅自己可见（`visible`）；不修改标题正文、不触发转码、不改 MinIO。
- 用户管理：分页/筛选/详情、封禁/解封（`user.status` 1/0）、角色调整（0/1/2，仅超级管理员）；不展示密码、不物理删除用户。
- 对象存储管理：列出 `avatar` / `video` / `video-cover` 桶与对象、预览/下载（预签名 URL）、删除真实对象；删除前检查 `video.video_url` / `cover_url` / `hls_url` 与 `user.avatar` / `background` 引用，命中则返回冲突并列出引用方。
- 所有管理操作记录操作者 uid、动作、目标与结果到应用日志；不新增审计表。
- 不新增数据库表或字段；不改网关路由（`/main/**` 已指向 AL-web）；不改推荐/转码链路。

## Capabilities

### New Capabilities

- `admin-console-access`：管理员登录后的角色校验、普通用户拒绝、超级管理员专属操作。
- `admin-video-moderation`：视频稿件的查询、审核、下架/删除与可见性管理。
- `admin-user-management`：用户查询、封禁/解封与角色调整。
- `admin-object-storage`：对象浏览、预览与带引用检查的真实删除。

### Modified Capabilities

无（仓库目前没有 admin 相关规格）。

## Impact

- 后端：`AL-web` 新增 admin controller/service/mapper/vo 与角色拦截器；复用现有 `UserInfoService`、`VideoMapper`、`MinioClient`；可能新增 admin 分页查询与引用检查 SQL。
- 前端：新增 `AniLink-admin/` 应用（5174，`/api` 代理到网关 8090）；复用登录/刷新接口，不修改 `AniLink-web`。
- 网关：`/main/**` 已覆盖 `/main/admin/**`，无需新增路由；管理接口不在免登录白名单内。
- 数据：只更新 `user.status/role`、`video.status/visible/delete_date`；不新增表；MinIO 对象按管理员操作真实删除。
- 权限：普通用户即使持有有效 token 也无法调用 admin 接口；超级管理员才能改角色。
- 风险：对象删除是真实删除，必须依赖引用检查与二次确认；管理端误操作影响面大，需保留操作日志。
