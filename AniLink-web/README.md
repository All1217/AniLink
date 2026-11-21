# AniLink-web

AniLink 的新前端：Vite 6 + Vue 3.5 + TypeScript + SCSS。重构目标与阶段计划见 [`../docs/AniLink-web重构计划.md`](../docs/AniLink-web重构计划.md)。

旧的 `AniLink-Vue3`（Vue CLI + Less）保留作对照，不再改动。

## 技术栈

- 构建：Vite 6（`@vitejs/plugin-vue`）
- 框架：Vue 3.5（`<script setup lang="ts">`）+ TS 5.8（`strict`）
- 样式：SCSS（`sass-embedded`），设计令牌集中在 `src/styles/_tokens.scss`
- UI：Element Plus 2.9（`unplugin-vue-components` 按需 + CSS 变量覆盖）
- 状态：Pinia 2 + `pinia-plugin-persistedstate`
- 请求：axios（`src/api/client.ts` 统一解包与续约）
- 其他：vue-router 4（hash 模式）、ECharts（按需）、artplayer + danmuku 插件

## 启动

```powershell
cd AniLink-web
pnpm install      # 首次
pnpm dev          # http://localhost:5173
pnpm typecheck    # vue-tsc --noEmit
pnpm build        # typecheck + 生产构建
```

依赖安装需要 node_modules 的构建脚本权限，已在 `pnpm-workspace.yaml` 的 `allowBuilds` 中放行 `esbuild` / `vue-demi` / `@parcel/watcher`。

## 环境变量

| 变量 | 默认 | 说明 |
| --- | --- | --- |
| `VITE_API_BASE` | `/api` | 请求前缀；开发期由 Vite 反代并剥掉 `/api` |
| `VITE_PROXY_TARGET` | `http://localhost:8090` | 网关地址（`AL-gateway`） |
| `VITE_WS_BASE` | `ws://localhost:8024` | `AL-communication` 的 Netty WebSocket 基址 |

## 目录

```
src/
├─ api/          # 按域拆分的接口层（auth/video/comment/danmu/interaction/favorite/history/chat/user/upload/stats）
├─ components/   # base（AppHeader/SearchBox/LoginDialog/VideoCard…）、video、user、chat
├─ composables/  # useWebSocket 等可复用逻辑
├─ config/       # 站点静态配置（分区、轮播占位、分页大小）
├─ layouts/      # DefaultLayout（顶栏 + 内容 + 页脚）
├─ router/       # 路由表与守卫
├─ stores/       # user / search / player
├─ styles/       # tokens / mixins / reset / element 覆盖
├─ types/        # 领域模型
├─ utils/        # 格式化、cookie
└─ views/        # home / search / video / user / message / upload / history / account
```

## 当前进度

- ✅ M0 脚手架：Vite/TS/SCSS、设计令牌、`client.ts`、路由守卫、顶栏与登录弹窗
- ✅ M1 首页：轮播、分区入口、无限流推荐、骨架屏、空/错状态；搜索页接同一接口
- ✅ API 层：71 个 REST 端点全部封装完毕（未接页面的部分在 M2–M5 逐步消费）
- ⏳ M2 播放页 / M3 个人空间与历史 / M4 投稿中心 / M5 消息中心
