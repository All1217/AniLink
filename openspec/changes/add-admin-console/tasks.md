# Tasks

## 1. 后端 admin 基座与鉴权

- [x] 1.1 在 `AL-web` 新增 `com.video.web.admin` 包与 `AdminAuthInterceptor`，对 `/main/admin/**` 校验 `user.role>=1`，普通用户抛 309；验证：MockMvc 中 role=0 请求返回 309，role=1 放行
- [x] 1.2 新增 `GET /main/admin/me` 返回当前管理员 uid、username、nickname、role；验证：role=1/2 返回 200，role=0 返回 309
- [x] 1.3 新增 admin 鉴权测试（未登录、role=0、role=1、role=2）并接入 `mvn -q -pl AL-web -am test -Dtest=AdminAccessTest`；验证：测试通过

## 2. 视频稿件管理

- [x] 2.1 新增 admin 视频分页查询与详情接口，支持 `status`/`uid`/`keyword` 筛选，默认排除 `status=3`；验证：`AdminVideoTest` 覆盖分页、筛选、详情
- [x] 2.2 新增 `PUT /main/admin/videos/{vid}/status`，只允许 `0→1/2` 与 `1/2→3`，非法流转返回 409，下架写 `delete_date`；验证：单元测试覆盖全部流转与非法分支
- [x] 2.3 新增 `PUT /main/admin/videos/{vid}/visible`，只允许 0/1；验证：测试覆盖合法值与非法值
- [x] 2.4 确认视频管理不修改 `transcode_status`/`hls_url`/MinIO，操作日志包含操作者 uid、vid、动作；验证：代码审查 + 日志断言
- [x] 2.5 新增 `AdminVideoTest`（MockMvc + mock service），覆盖 role=0 拒绝、通过、驳回、下架、重复审核；验证：测试通过

## 3. 用户管理

- [x] 3.1 新增 admin 用户分页/筛选/详情接口，DTO 不含 `password`；验证：`AdminUserTest` 断言响应无 password 字段
- [x] 3.2 新增 `PUT /main/admin/users/{uid}/status`，允许 0/1，拒绝封禁自己；验证：测试覆盖正常封禁、解封、自我封禁拒绝
- [x] 3.3 新增 `PUT /main/admin/users/{uid}/role`，只允许 role=2 调用，值只能是 0/1/2，拒绝修改自己；验证：测试覆盖 role=1 拒绝、role=2 成功、自我修改拒绝
- [x] 3.4 新增 `AdminUserTest` 并覆盖关键字/状态/角色筛选；验证：测试通过

## 4. 对象存储管理

- [x] 4.1 新增 MinIO admin service：桶白名单（avatar/video/video-cover）、对象分页/前缀、5 分钟预签名 URL；验证：单元测试覆盖白名单非法桶与 URL 生成
- [x] 4.2 实现对象 key 解析与引用检查：`video_url`/`cover_url`/`user.avatar`/`user.background` 精确匹配，`hls/{vid}/` 目录按稿件 `hls_url` 判断；验证：`AdminStorageTest` 覆盖被引用、未引用、HLS 分片、非法 bucket
- [x] 4.3 新增 `DELETE /main/admin/storage/objects`，命中引用返回 409 + 引用列表，未引用才调用 `removeObject`；验证：测试断言引用时 MinIO 删除未被调用
- [x] 4.4 MinIO 删除失败返回错误、不报告成功，并记录操作日志；验证：mock 删除抛错时接口返回 500
- [x] 4.5 新增 `AdminStorageTest`（MockMvc + mock service）；验证：测试通过

## 5. 独立管理前端 AniLink-admin

- [x] 5.1 新建 `AniLink-admin/`（Vite 6 + Vue 3.5 + Element Plus + Pinia），端口 5174，`/api` 代理到 8090；验证：`pnpm --dir AniLink-admin build` 通过
- [x] 5.2 实现登录页与路由守卫：复用 `/main/login`，登录后调 `/main/admin/me`，role=0 提示无权限并清 token；验证：typecheck/build 通过
- [x] 5.3 实现视频稿件页：筛选、分页、详情、通过/驳回/下架/可见性操作；验证：build 通过，手工对照接口
- [x] 5.4 实现用户管理页：筛选、分页、封禁/解封、角色调整（仅超管可见）；验证：build 通过，普通管理员看不到角色操作
- [x] 5.5 实现对象存储页：桶切换、前缀、对象表、预览、删除二次确认、409 引用列表展示；验证：build 通过，409 时不发送删除
- [x] 5.6 新增 `AniLink-admin/README.md`，说明端口、代理、登录角色与启动命令；验证：按 README 启动成功

## 6. 集成验证与文档

- [x] 6.1 启动 AL-web/网关与管理前端，role=0 调用 `/main/admin/me` 返回 309；role=1 登录进入管理页；验证：手工冒烟记录
- [ ] 6.2 审核一条 `status=0` 稿件为通过/驳回，再下架；核对 `video.status`/`delete_date` 与列表；验证：数据库查询与页面一致
- [x] 6.3 封禁/解封一个测试用户、用超管调整其角色；验证：目标用户登录/权限变化符合预期
- [ ] 6.4 删除一个孤儿对象成功；删除被 `video_url`/`avatar`/`hls` 引用的对象返回 409 且 MinIO 对象仍在；验证：MinIO 列表与接口结果一致
- [x] 6.5 更新 `AniLink-Java/README.md` 与根 `AGENTS.md`，补充管理端端口、启动方式与“admin 暂不独立成服务”的现状；验证：文档命令可执行
- [x] 6.6 回归用户端：`AniLink-web` build 通过，`/main/**` 非 admin 接口不受影响；验证：构建与冒烟通过

## Workflow follow-up

- 评审通过后归档本变更；归档前确认 role 校验与对象引用检查的集成结果已记录。
- `al-user`/`al-video` 落地后，另立变更把 `com.video.web.admin` 抽成 `AL-admin` BFF。
