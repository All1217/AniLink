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

五个阶段全部完成：

- ✅ M0 脚手架：Vite/TS/SCSS、设计令牌、`client.ts`（含 token 续约与 305 静默）、路由守卫、顶栏与登录弹窗
- ✅ M1 首页：轮播、分区入口、无限流推荐、骨架屏、空/错状态；搜索页接同一接口
- ✅ M2 播放页：artplayer + 弹幕插件、播放/进度上报、弹幕发送、点赞/投币/收藏/分享、评论树（根评论 + 懒加载回复 + 删除）、弹幕画像（标签热度/相近用户/共同标签/标签筛选）、右侧相关推荐、`ws://…/video` 在线人数
- ✅ M3 个人空间与历史：投稿/收藏夹/粉丝三个 tab、收藏夹新建与编辑、收藏夹内批量取消/移动/复制、关注与取关、观看历史（时长筛选、按日期分组、单删/清空）、账号设置（资料、头像、公告）
- ✅ M4 投稿中心：拖拽/点击上传视频（真实进度条）、封面、标题/类型/分区/标签/简介/可见范围、提交投稿；稿件管理空状态；数据中心按天折线图（播放/点赞）
- ✅ M5 消息中心：会话列表（未读角标/删除/分页）、消息分页与发送（乐观上屏 + 落库 + WS 推送）、已读与最近打开时间、`ws://…/chat` 实时收发与心跳重连；系统通知空状态
- ✅ API 层：旧前端用到的端点全部封装（页面均已消费到对应业务线）；唯一例外是 `POST /main/history/add` 不再调用——它与 `POST /main/video/interact/record/add` 语义重叠，而后者是 upsert（`HistoryServiceImpl.addVideoRecord` 先查后插/更新），且 `bh_video` 上有唯一索引 `(uid, vid)`，两者都调会在重复播放时撞唯一键

### 登录依赖

后端把评论、弹幕画像（标签/相近用户）、热搜、关注态、收藏夹、历史、私信、上传、数据中心等接口放在鉴权之后（匿名调用返回 `code=305`）。因此这些区域在未登录时展示「去登录」引导，不发起请求，也不会弹「登录已过期」。

### 双令牌与自动续约

后端自 2026-11 起改为双令牌：

- `access-token`：默认 30 分钟，放在请求头 `access-token`；
- `refresh-token`：默认 7 天（滑动），只存在于服务端下发的 **HttpOnly + SameSite=Lax** Cookie 中，前端 JS 不可读也不写入；
- 状态码约定：`601` = access 过期 → 自动续约并重放原请求；`602` = 令牌非法/刷新令牌失效/检测到重用 → 直接清会话并唤起登录；`305` = 未登录；
- 续约单飞：并发请求共享同一个刷新 Promise，多标签页同时续约只会产生一次轮换；
- 续约时服务端会轮换 refresh 并重置有效期，活跃用户长期不掉线。

后端配置项在 `AL-web` 与 `AL-gateway` 的 `application.yml` 的 `al.auth.jwt.*`；其中 `secret`（环境变量 `AUTH_JWT_SECRET`）**必须 ≥32 字节且两端一致**，否则服务启动失败或全站 602。

### 账号与注册登录

- **用户名**（`username`）是注册时自选的登录名：唯一、不区分大小写、本期不可改；规则 2-20 位中英文/数字/下划线，**不能纯数字**（纯数字留给历史账号兼容）。
- **昵称**（`nickname`）只用于展示，可重复、可修改；注册不填时默认与用户名相同。
- 注册成功即登录（后端直接返回 access-token 并写 refresh Cookie），不会再出现"注册成功后登录又说账号不存在"。
- 密码至少 8 位、最多 72 字节，允许任意字符；后端用 bcrypt 存储，历史 MD5 账号首次登录会自动升级。
- 登录失败统一提示「用户名或密码错误」，连续失败会触发限流（账号 5 次 / 15 分钟）。

相关实现见 `src/api/client.ts`（拦截器与单飞续约）与 `src/stores/user.ts`（只持久化 access，不保存 refresh）。

### 视频播放（HLS 分片 + MP4 兜底）

- 播放地址：`vid.hlsUrl`（m3u8）优先，没有转码产物时回退 `videoUrl`（MP4 + HTTP Range 伪流式）。
- 播放内核：**优先 hls.js（MSE）**；新版 Chrome/Edge 的 `canPlayType('application/vnd.apple.mpegurl')` 也返回 `maybe`，但原生 HLS 的缓冲与 seek 策略不可控（实测缓冲只有 ~18s、远距离 seek 要 300–800ms），所以只有 MSE 不受支持（iOS Safari 等）时才把 m3u8 交给浏览器原生播放。`hls.js` 动态 `import`，单独 chunk，不进首屏。
- 缓冲策略：`maxBufferLength: 120` / `backBufferLength: 300`（约 120MB 上限），让拖进度条大概率落在已缓冲区间；分片出现致命错误或浏览器不支持 MSE 时自动回退 MP4，不会白屏。
- 拖动进度条：`useSeekThrottle` 在拖拽期间只让进度条跟手、松手才 seek 一次，并**吃掉拖动结束后补发的 click**（否则 Artplayer 会重复 seek 并把提示覆盖成时间）；拖动中每 250ms 用 `hls.latestLevelDetails` 预热落点分片（分片是 `immutable` 缓存，松手后 hls.js 直接命中）；松手后 `useSeekFeedback` 在播放器上显示「缓冲中 mm:ss」，出画面自动收起。
- `BiliPlayer.vue` 另外做了三件事：`poster` 用封面、`preload: 'auto'` 提前缓冲、按媒体域名做 `preconnect` 预热 DNS/TCP。
- 转码在后端异步完成（`video.transcode_status`），投稿后先播 MP4，转完自动切 HLS；详见根目录 `AGENTS.md`。

