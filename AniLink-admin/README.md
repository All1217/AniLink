# AniLink-admin

AniLink 的独立管理端（Vite + Vue 3 + Element Plus），与用户端 `AniLink-web` 分开部署。

## 启动

```powershell
cd AniLink-admin
npm install
npm run dev
```

- 开发地址：`http://localhost:5174`
- `/api` 由 Vite 代理到网关 `http://localhost:8090`（去掉 `/api` 前缀）
- 需要先启动网关与 AL-web；管理员账号的 `user.role` 必须为 1 或 2

## 构建

```powershell
npm run build
```

## 页面

- 登录：复用 `/main/login`，随后调用 `/main/admin/me` 校验角色；role=0 会提示无权限并清理本地 token。
- 视频稿件：筛选、分页、详情、通过/驳回/下架、公开/仅自己可见；不会修改 MinIO 或转码状态。
- 用户管理：筛选、封禁/解封、角色调整（仅超级管理员，且不能操作自己）。
- 对象存储：只允许 `avatar` / `video` / `video-cover` 三个桶；支持前缀分页、预签名预览、带引用检查的单对象删除，以及按当前桶/前缀“一键清除冗余存储”（只删除无业务引用的对象，被引用对象自动跳过）。

## 权限

- access token 存在 `localStorage` 的 `anilink-admin-token`，请求头 `access-token`。
- 管理接口在服务端再次校验 `user.role >= 1`；前端隐藏菜单不能替代服务端校验。
- 角色调整需要 `role == 2`；普通管理员看不到角色下拉框。
