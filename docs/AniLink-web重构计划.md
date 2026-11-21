# AniLink-web 重构计划（Vite + SCSS 版前端）

> 状态：**仅方案，未写任何代码**。本轮产出 = 本文件。
> 目标目录：`AniLink-web/`（新建，与本仓库现有 `AniLink-Vue3/` 并存）
> 参考对象：`E:\Program files\AniLink\AniLink-Vue3`（只读对照，不修改）
> 硬边界：**不改后端**（`AniLink-Java`、`AniLink-AlgorithmEngine`），不改网关路由与接口契约；新前端通过同一网关 `localhost:8090` 联调。

---

## 1. 目标与验收总纲

把现有 `AniLink-Vue3`（Vue CLI 5 + Less + 39 个 SFC / 11 518 行）重写为一个**结构清晰、视觉仿 B 站、代码量明显收缩**的 Vite 工程，且**后端 API 业务线一条不丢**。

三条硬指标：

1. **业务等价**：现网 71 个 REST 端点 + 2 条 WebSocket 通道，在新前端每一条都有可达的 UI 入口与交互（对照表见 §7、§8）。
2. **结构可读**：单文件上限 250 行（SFC 的 template+script+style 合计），单目录职责唯一，`api → store → view` 单向依赖。
3. **视觉一致**：B 站式浅色主题（粉 `#fb7299` / 蓝 `#00aeec` / 灰阶 `#18191c`…`#f1f2f3`），Element Plus 作为基础控件但外观统一改造，不做"Element Plus 默认样子套 B 站文案"。

---

## 2. 旧前端体检（问题清单，均有文件证据）

总量：`src` 134 个文件；`.vue` 39 个共 11 518 行；`.ts` 34 个共 2 891 行；`.less` 11 个共 4 211 行；`.css` 3 个共 1 634 行。下列问题直接决定新结构。

| # | 问题 | 证据 |
|---|---|---|
| 1 | **`api/` 层职责被污染**：接口函数、业务常量、假数据、类型、图表配置混在一起 | `api/app.ts` 混放 34 条分区常量 + 默认用户；`api/MainWindow/index.ts` 放了 `deRVL`（6 条假视频）与 9 条轮播图；`api/UploadManager/echartsOptions.ts` 585 行纯图表配置 |
| 2 | **同一接口重复封装** | `GET /main/video/public/pageVideo` 在 `api/Video/index.ts`、`api/MainWindow/index.ts`、`api/UserPage/index.ts` 各写一遍；收藏列表被 `getVideoInfavor` / `getFVByUidAndVid` / `getVideoFavoritePage` 三套命名覆盖 |
| 3 | **巨型组件** | `App.vue` 711 行（导航 + 搜索面板 + 热搜 + 分区弹窗 + 用户卡 + 登录框）；`views/BrowseHistory/BrowseHistory.vue` 768 行；`components/UserPage/FavList.vue` 700 行；`components/Video/Comments.vue` 756 行 |
| 4 | **样式与视图分家且不同步** | `views/Video/Video.vue` 的样式在 `styles/Video/video.less`，`MainWindow` 同理；改一处要跨两个目录，导致 3 600 行 Less 与 SFC 内 `scoped` 样式并存 |
| 5 | **全局污染** | `styles/index.css` 里 `span { color: var(--font-color-gray1) }`、`* { margin:0; padding:0 }` 属于全局选择器，Element Plus 组件内部也被波及，只能再用 `!important` 掩盖 |
| 6 | **硬编码环境地址** | `ws://localhost:8024/video`、`ws://localhost:8024/chat` 写死在组件里；轮播图图片是内网 MinIO `http://192.168.150.102:9000/...`；`ip: '北京'`、`device: 'PC'` 写死（`Player.vue`） |
| 7 | **类型不严谨** | `shallowRef(null)`、`context: any`（`api/Video/Barrager.ts`）、大量 `as any`；`User.password` 出现在前端实体 |
| 8 | **死代码/未使用能力** | `views/Test/Test.vue`（337 行，注释写明"备份原版 whisper"，数据全硬编码）；`api/Video/Barrager.ts`（110 行自研 canvas 弹幕引擎，全仓库无引用）；`components/UserPage/fans/Fans.vue`、`Follow.vue`、`VideoBox.vue` 空壳无引用 |
| 9 | **依赖未发挥** | `element-plus` 全量引入 + 全量图标（`main.ts` 遍历注册所有图标）；`echarts` 全量引入；`pinia` 仅 2 个 store（user/file）+ 1 个通知 store，状态基本靠组件内 `ref` |
| 10 | **无设计令牌体系** | 只有 `styles/index.css:103` 的 10 个零散 CSS 变量；颜色/字号/圆角在 1 200 行 `app.less` 里裸写，同一灰色出现 4 种写法 |
| 11 | **交互状态手工同步** | `Video.vue` 用 `Set<number>` + 手写 `count++/--` 维护收藏、点赞、硬币，多个面板独立改同一份数据，容易与实际后端不一致 |
| 12 | **未完成项散落** | 上传进度 `TODO`、完播判断 `TODO`（`isFinish` 只比时长）、token 刷新整段被注释（`util/http.ts` 第 98–100 行）、`Article.vue`/`Series.vue`/`System.vue`/搜索-用户 tab 无接口、纯静态 |

---

## 3. 技术选型

| 维度 | 旧 | 新 | 说明 |
|---|---|---|---|
| 构建 | Vue CLI 5（webpack 5） | **Vite 6**（`@vitejs/plugin-vue`） | 冷启动从十几秒降到 <1s；`server.proxy` 复刻现有 `/api` 反代 |
| 框架 | Vue 3.2 | **Vue 3.5**（`<script setup lang="ts">`） | 仍是 Vue 3，API 兼容；使用 `defineModel` 简化 v-model 组件 |
| 语言 | TS 5.8 | **TS 5.8**（`strict: true`） | 旧工程 `strict` 未开，新工程全量严格模式 |
| 样式 | Less 4 + 全局 CSS | **SCSS**（`sass-embedded`） | 统一 `@use` + 设计令牌 + `:deep()` 局部覆盖；彻底去掉 Less |
| 路由 | vue-router 4（hash） | vue-router 4（**hash 保留**） | 保留 hash 可零配置部署（不需 nginx rewrite） |
| 状态 | pinia 2 + 持久化插件 | pinia 2 + persistedstate（**3 个 store**） | user / search / player 分离，见 §9 |
| 请求 | axios 1.7 | axios 1.7 | 保留，重写拦截器与解包 |
| UI | Element Plus 2.9（全量） | Element Plus 2.9 + **按需自动导入** | `unplugin-vue-components` + `ElementPlusResolver` + SCSS 变量覆盖 |
| 图表 | ECharts 5.6（全量） | ECharts 5.6 + **按需注册** | 只注册 Line/Bar + Tooltip/Grid/Legend |
| 播放器 | artplayer 5.2 + danmuku 插件 | 同左（**去掉自研 Barrager**） | 弹幕能力由插件承担，减少 110 行 canvas 代码 |
| 图标 | iconfont（1 262 行 json + 731 行 css + 539 行 demo.css + 字体） | **Element Plus Icons 按需 + 内联 SVG 子集** | 只保留实际用到的 ~12 个分区/功能图标，删掉整套字体 |
| 代码质量 | 无 lint | **ESLint 9 (flat) + Prettier** | `vue/essential` + `@typescript-eslint` 基础规则 |

**明确不引入**：Tailwind / UnoCSS（与 Element Plus 主题体系重叠，且会让"信息密度高"的排版变成一堆原子类）、lodash（工具函数自写 5 个即可）、vueuse（只用得到 `useIntervalFn`/`useIntersectionObserver` 两个，自写约 40 行）、SSR/Nuxt（本项目纯内网 SPA，无 SEO 需求）。

---

## 4. 目录结构与分层

```
AniLink-web/
├─ index.html
├─ package.json
├─ vite.config.ts
├─ tsconfig.json / tsconfig.node.json
├─ .env.development / .env.production
├─ eslint.config.js / .prettierrc
└─ src/
   ├─ main.ts                     # 只做 createApp + 插件装配
   ├─ App.vue                     # 只放 <RouterView/> 与全局壳（≤40 行）
   ├─ config/site.ts              # 站点常量：轮播图、分区表、页脚、(env 读取)
   ├─ api/
   │  ├─ client.ts                # axios 实例 + 拦截器 + 解包 + 错误上报
   │  ├─ types.ts                 # ResultData / PageRes / 通用 Query
   │  ├─ auth.ts  video.ts  comment.ts  danmu.ts  interaction.ts
   │  ├─ favorite.ts  history.ts  chat.ts  user.ts  upload.ts  stats.ts
   │  └─ index.ts                 # 仅 re-export，禁止写逻辑
   ├─ stores/
   │  ├─ user.ts                  # token / refreshToken / userInfo / 登录登出
   │  ├─ search.ts                # 搜索历史（本地 20 条，持久化）
   │  └─ player.ts                # 当前视频交互态（like/coin/collect/follow）
   ├─ router/
   │  ├─ index.ts                 # 路由表
   │  └─ guard.ts                 # 白名单 + 登录跳转
   ├─ layouts/
   │  ├─ DefaultLayout.vue        # 顶部导航 + <RouterView/>
   │  └─ UploadLayout.vue         # 投稿中心左侧栏 + 顶栏
   ├─ components/
   │  ├─ base/                    # AppHeader / SearchBox / VideoCard / Avatar
   │  │                           # RatioBox / EmptyState / InfiniteList / CountText
   │  ├─ video/                   # BiliPlayer / DanmakuBar / CommentTree
   │  │                           # CommentEditor / FavoriteModal / CoinModal
   │  ├─ user/                    # ProfileCard / StatsBar / FollowButton / FavFolderCard
   │  └─ chat/                    # ChatList / ChatWindow / MessageBubble
   ├─ views/
   │  ├─ home/HomeView.vue        # 首页
   │  ├─ search/SearchView.vue    # 搜索结果（综合 / 用户）
   │  ├─ video/VideoView.vue      # 播放页
   │  ├─ user/UserView.vue        # 个人空间（投稿/合集/收藏夹/粉丝）
   │  ├─ message/MessageView.vue  # 消息中心（私信 / 系统）
   │  ├─ upload/                  # 投稿中心（上传 / 稿件管理 / 数据中心）
   │  ├─ history/HistoryView.vue  # 历史记录
   │  └─ account/AccountView.vue  # 账号设置（资料 / 头像）
   ├─ composables/                # useWebSocket / usePlayer / usePagination / useCountdown
   ├─ styles/
   │  ├─ index.scss
   │  ├─ _tokens.scss             # 设计令牌（唯一颜色/字号/间距来源）
   │  ├─ _mixins.scss             # 单行/多行省略、卡片、滚动条
   │  ├─ _reset.scss
   │  └─ element/                 # Element Plus 变量与组件外观覆盖
   ├─ types/                      # 领域模型（Video/User/Comment/Danmu/Chat…）
   ├─ utils/                      # request 以外的纯函数：format/time/dom/id
   └─ assets/                     # logo、占位图、少量 SVG
```

分层铁律：

- `views/` 只能调用 `stores/`、`composables/`、`api/`（经 store 优先），不得直接写 axios。
- `components/` 不感知路由；需要跳转时 `emit` 或接受 `to` 属性。
- `api/` 不引用 store（鉴权头由 `client.ts` 从 store 读取，避免循环依赖）。
- 单文件 ≤ 250 行；超过即拆组件或抽 composable。

---

## 5. 设计系统（B 站视觉）

`styles/_tokens.scss` 作为唯一来源，同时映射为 CSS 变量供 Element Plus 与运行时渐变使用。

```scss
// 主色（B 站粉）与辅助色
$brand:        #fb7299;   $brand-hover: #fc8bab;  $brand-active: #e05c81;
$link:         #00aeec;   $link-hover:  #33bff0;
// 中性色（B 站灰阶）
$text-1:       #18191c;   $text-2:      #61666d;  $text-3: #9499a0;
$line:         #e3e5e7;   $bg-page:     #f1f2f3;  $bg-card: #ffffff;
// 语义色
$success:#2ac25b; $warning:#ffb027; $danger:#f56c6c;
// 排版栅格（B 站网页端 8px 基准）
$space: 4px 8px 12px 16px 20px 24px 32px 40px;
$radius: 4px 6px 8px 12px 16px;    $font: 12px 13px 14px 16px 18px 24px;
```

关键规则（与 `AGENTS.md` 的"信息密度高、少留白、视觉充实"对齐）：

1. **版心 1200px**，首页两侧留白随视口收缩；卡片间距 16px，视频卡栅格 5/4 列（≥1440px 5 列）。
2. **封面比例统一 16:10**（B 站首页），播放页右侧推荐 16:9 或 16:10 二选一——取 16:10 与首页一致。
3. **鼠标悬停态**：封面轻微上浮 + 标题变 `$link`，不使用缩放动画（避免抖动）。
4. **数字格式化**：>10000 显示"x.x 万"，>1 亿显示"x.x 亿"（复用旧 `formatCount` 语义）。
5. **Element Plus 改造点**：按钮圆角 6px、主色替换为 `$brand`、`el-dialog` 去掉默认 header 阴影、`el-input` 聚焦边框改 `$brand`、`el-tabs` 下划线 2px 主色、`el-pagination` 简化。
6. 只做**浅色主题**（与 B 站网页端一致），不引入暗色切换，避免双主题维护成本。

---

## 6. 路由与页面

| 路径 | 页面 | 对应旧文件 | 数据来源 | 保留策略 |
|---|---|---|---|---|
| `/` → `/home` | 首页 | `MainWindow.vue` + `MainWindow/*` | `pageVideo`、`hotSearch` | 重构：轮播/分区/无限流三段拆组件 |
| `/search` | 搜索结果（综合/用户） | `SearchResult.vue` + `All.vue`/`User.vue` | `pageVideo`；用户 tab **无接口** | 综合保留真接口；用户 tab 降级为"综合结果 + 提示"（见 §12-D1） |
| `/video/:vid` | 播放页 | `Video.vue` + `Player.vue` + `Video/{Comments,DanmuBox,Collection}` | 20 个端点（见 §7） | 完整保留 |
| `/user/:uid` | 个人空间 | `UserPage.vue` + `Submit/Dynamic/Series/FavList/FansList` | 12 个端点 | 投稿/收藏夹/粉丝保留；动态、合集无接口 → 见 §12-D2 |
| `/message` | 消息中心（私信/系统） | `MC.vue` + `Whisper.vue`/`System.vue` | 8 个端点 + WS `/chat` | 私信完整保留；系统通知无接口 → 静态占位 |
| `/upload` | 投稿中心（上传/稿件/数据中心） | `UploadManager.vue` + 4 个子组件 | 6 个端点 + 2 个图表端点 | 上传与数据中心保留；稿件管理无接口 → 见 §12-D3 |
| `/history` | 历史记录 | `BrowseHistory.vue` | 3 个端点 | 完整保留（含清空/单删/继续播放） |
| `/account` | 账号设置 | `Account.vue` + `UserSetting/ChangeAvatar` | 4 个端点 | 完整保留 |
| —（删除） | 测试页 | `Test.vue` | 无（硬编码） | **不迁移**（死代码） |

路由守卫沿用旧逻辑：`/video`、`/search` 免登录；其余无 token 跳首页并带 `redirect`（旧代码把"首页"当 LOGIN_URL，实际是"登录弹窗 + 停留首页"，新版本用 `/home` + 登录弹窗，`redirect` 参数恢复原页面）。**不新增路由入口文案**，导航层级与 B 站一致。

---

## 7. API 层设计与端点清单

### 7.1 封装约定

- `client.ts`：`baseURL = import.meta.env.VITE_API_BASE ?? '/api'`，`withCredentials: true`，超时 25s（上传单独 10min）。
- 请求头：`access-token: <token>`（沿用后端契约，不改成 `Authorization`）。
- 响应：`code === 200` 视为成功，直接返回 `data`；`code !== 200` 统一 `ElMessage.error` + reject 带 `code/message`。**解包在拦截器里做一次**，业务层不再判 `res.code`（旧代码到处判，重复且易漏）。
- 401/305/601/602：本次**真正接通**刷新链路——命中后调 `GET /main/public/refresh` 换 token，单飞（`canRefresh` 锁），失败则清 store + 跳首页。旧代码这两段被注释掉了，是已知 bug。
- `refresh-token`：响应头带回 → 写 cookie（兼容旧行为），请求时回填。
- 上传：`upload.ts` 暴露带 `onProgress` 的函数，进度条用 `el-progress`，不再 `console.log`。

### 7.2 端点→模块对照（71 条，全部要有 UI 入口）

| 模块 | 端点 | 新位置 |
|---|---|---|
| auth (7) | `POST /main/login`、`POST /main/logout`、`GET /main/info`、`GET /main/public/refresh`、`POST /main/user/register`、`GET /main/chat/getWhisperUnread`、`GET /main/dataCenter/mainWindow/get/hotSearch` | `api/auth.ts`、`api/chat.ts`、`api/stats.ts` |
| video (4) | `GET /main/video/public/getVideoById`、`GET /main/video/public/getVideoStatsById`、`GET /main/user/public/getById`、`GET /main/user/public/getUserStatById` | `api/video.ts`、`api/user.ts` |
| video-list (3) | `GET /main/video/public/pageVideo`、`GET /main/video/public/getRealTimeRecommend`、`GET /main/video/pageVideo` | `api/video.ts`（两参数签名，消除三处重复） |
| comment (4) | `GET /main/comment/pageCommentVo`、`GET /main/comment/pageReply`、`POST /main/comment/insert`、`PUT /main/comment/logicDelete` | `api/comment.ts` |
| danmu (6) | `GET /main/danmu/public/getDanmuList`、`POST /main/danmu/post`、`GET /main/danmu/getSimilarUsers`、`POST /main/danmu/filterUserByTags`、`GET /main/danmu/getCommonTags`、`GET /main/danmu/getTagRankList`（+ WS `/video`） | `api/danmu.ts`、`composables/useWebSocket` |
| interaction (9) | `POST /main/video/public/play`、`POST /main/video/interact/coin`、`GET /main/video/getInterActionCoin`、`GET /main/favorite/getInterActionFavorite`、`GET /remark/get/like`、`POST /remark/like`、`GET /main/fans/get/follow`、`POST /main/fans/follow`、`DELETE /main/fans/unfollow` | `api/interaction.ts` |
| history (5) | `POST /main/history/add`、`POST /main/video/interact/record/add`、`GET /main/history/getHistory/page`、`DELETE /main/history/deleteByUid`、`DELETE /main/history/deleteSingle` | `api/history.ts` |
| favorite (7) | `PUT /main/favorite/collect`、`GET /main/favorite/getFavoriteListPage`、`GET /main/favorite/getVideoByFavor`、`GET /main/favorite/getFVByUidAndVid`、`POST /main/favorite/add`、`PUT /main/favorite/update`、`PUT /main/favorite/unCollect` | `api/favorite.ts` |
| favorite-batch (2) | `PUT /main/favorite/update/batchMove`、`PUT /main/favorite/update/batchCopy` | `api/favorite.ts` |
| favorite-page (1) | `GET /main/favorite/getVideoFavoritePage` | `api/favorite.ts` |
| chat (7) | `GET /main/chat/getChatVoPage`、`DELETE /main/chat/deleteChat`、`GET /main/chat/getChatMessagePage`、`POST /main/chat/insertOneChat`、`POST /main/chat/insertOneChatMessage`、`PUT /main/chat/update`、`PUT /main/chat/update/unread`（+ WS `/chat`） | `api/chat.ts` |
| user (10) | `GET /main/user/getById`、`GET /main/user/getUserStatsByUid`、`GET /main/user/getUserVoByUid`、`POST /main/user/updateNotice`、`POST /main/user/updateDescription`、`POST /main/user/updateAvatar`、`GET /main/user/updateMultiStats`、`POST /main/user/updateUserInfo`、`GET /main/fans/get/followList`、`GET /main/fans/get/fansList` | `api/user.ts` |
| upload (4) | `POST /main/file/upload/video`、`POST /main/file/upload/avatar`、`POST /main/file/upload/video/cover`、`POST /main/file/submit` | `api/upload.ts` |
| stats (2) | `GET /main/dataCenter/total/get/play`、`GET /main/dataCenter/total/get/like` | `api/stats.ts` |

> 说明：旧代码用 `http.get/getVideoById` 这类"函数名 = 语义"的写法，新层统一 `动词 + 资源`（`fetchVideo`、`fetchVideoStats`、`createComment`…），并全部带返回类型。

---

## 8. WebSocket 通道（2 条，必须保留）

两条都指向 `AL-communication` 的 Netty（旧代码写死 `ws://localhost:8024`，新代码走 `VITE_WS_BASE`，默认 `ws://localhost:8024`；开发期也可用 Vite `server.proxy` 的 `ws: true` 转 `/ws` 避免跨域）。

| 通道 | 用途 | 信封协议 | 心跳 |
|---|---|---|---|
| `/video` | 播放页"x 人正在看" | 上行 `{vid, type:'join'|'heartbeat'}`；下行 `{type:'cnt', count}` | 5s |
| `/chat` | 私信实时收发 | 上行 `{from,to,type:'auth'|'private'|'heartbeat', content?}`；下行 `{type:'private', from, to, content}` | 5s |

`composables/useWebSocket.ts` 统一封装：`connect(path)` / `send(payload)` / `onMessage` / 自动重连（1s→2s→4s→8s，上限 30s，最多 10 次）/ 断线提示 / 组件卸载清理。旧代码在两个组件里各写一遍（`Player.vue` 与 `Whisper.vue`），新代码只写一次。

---

## 9. 状态管理

| store | 字段 | 持久化 | 说明 |
|---|---|---|---|
| `user` | `token`、`refreshToken`、`userInfo`、`isLogin` | 是（`localStorage`） | 登录/登出/拉 `/main/info`；登出清空并清 cookie |
| `search` | `history: {content, time}[]`（上限 20） | 是 | 旧代码存在 `user.sh` 里并跟用户信息混在一起，拆开 |
| `player` | `vid`、`userVideo`（like/coin/collect/follow 四态）、`stats` | 否 | 播放页多面板共享的**唯一真相**，解决旧代码"多份计数各自加减" |

规则：能由后端返回的（如 `userStats`）不放进 store，交给页面 `ref`；store 只放**跨组件、跨路由**且需要持久化的状态。

---

## 10. 关键交互实现要点

1. **播放器**：`BiliPlayer.vue` 包 `artplayer` + `artplayer-plugin-danmuku`。`play` 事件触发 `POST /main/video/public/play`（每会话一次）与 `POST /main/history/add`；每 3s `POST /main/video/interact/record/add` 上报进度（暂停停止）；弹幕发送走 `beforeEmit` 校验（未登录拦截、空内容拦截）后 `POST /main/danmu/post`。完播判断改为"累计观看 ≥ duration × 0.9"，替换旧代码的 `time >= duration - 2`。
2. **点赞/投币/收藏/关注**：`player` store 里统一 `applyOptimistic` + 失败回滚，避免旧代码 `Set` 与计数不同步；投币用 `el-dialog` 选 1/2 枚（对应 `Interaction.COIN_ONE/COIN_TWO`）。
3. **评论**：两级懒加载（根评论分页 → 展开 `pageReply`），支持按"最新/最热"（`CommentFilter`）、点赞、逻辑删除（`logicDelete`）、回复。旧 `Comments.vue` 683 行拆为 `CommentTree` / `CommentItem` / `CommentEditor`。
4. **收藏夹**：播放页弹窗勾选多个收藏夹 → `PUT /main/favorite/collect`（`fids[]`）；个人空间收藏夹页支持新建/改名/批量移动/复制/取消收藏。
5. **上传**：拖拽选文件 → 先 `upload/video` 拿 URL（进度条）→ 封面 `upload/video/cover` → 表单（标题/分区/标签/简介）→ `file/submit`。分区选项来自 `config/site.ts`（旧代码从 `api/app.ts` 的 `avaPar` 取）。
6. **私信**：左侧会话列表（分页 + 未读红点）、右侧消息流、发送即上屏（乐观）+ `insertOneChatMessage` 落库；WS 收消息时按"当前会话/其他会话"分流（复用旧逻辑，但抽成 composable）。
7. **历史记录**：按日期分组、悬停显示删除、支持单删/清空、点击跳转对应视频并定位进度。
8. **数据中心**：两张折线图（播放/点赞），日期范围选择 → `getTotalPlay/getLike`；ECharts 按需注册，`resize` 用 `ResizeObserver`（旧代码手写 window resize）。
9. **搜索**：`SearchBox` 组件统一：本地历史（store）+ 当前热搜（`hotSearch`）+ 回车跳 `/search?keyword=`；热搜 10 条两列对齐（复用旧版视觉）。

---

## 11. 与旧工程的对应关系（迁移清单）

| 旧 | 新 | 处理 |
|---|---|---|
| `App.vue`（711 行） | `layouts/DefaultLayout.vue` + `components/base/AppHeader.vue` + `SearchBox.vue` + `LoginDialog.vue` + `PartitionMenu.vue` | 拆 5 个文件 |
| `api/*`（19 个文件） | `api/*`（12 个文件） | 合并重复、剥离常量与假数据 |
| `api/UploadManager/echartsOptions.ts`（585 行） | `components/upload/charts/*.ts`（2 个 ≤80 行配置） | 只保留用到的两份配置，其余删除 |
| `api/Video/Barrager.ts` | 删除 | 由 danmuku 插件替代 |
| `views/Test/Test.vue` | 删除 | 死代码 |
| `styles/*.less`（11 个 4 211 行） | `styles/*.scss`（≤6 个，合计目标 ≤900 行） | 合并进各自 SFC（`<style scoped lang="scss">`），全局只留 reset + tokens + element 覆盖 |
| `assets/fonts/iconfont.*` | 删除字体，保留 12 个 SVG | 打包体积显著下降 |
| `components/UserPage/fans/{Fans,Follow}.vue`、`VideoBox.vue` | 删除 | 空壳 |
| `api/app.ts` 的 `partitonList/avaPar` | `config/site.ts` | 数据保留，位置归位 |
| `api/MainWindow/index.ts` 的 `slide_picture` 9 条 | `config/site.ts`（可配置、可关） | 内网 MinIO 地址改为 `VITE_IMG_BASE` 拼接，缺省用本地占位图 |
| `deRVL`（6 条假视频）、`defaultVideo/defaultUser…` | 删除 | 占位改由组件内 `skeleton` 承担 |

---

## 12. 需要拍板的 4 个决策（已给默认方案，你不反对即按此执行）

- **D1 搜索"用户"tab（旧 `MainWindow/User.vue`）**：后端无用户搜索接口 → 新版本**不提供该 tab**，搜索页只保留"综合"，并在输入为空时展示热搜与历史。若你要保留 tab，需要后端新增接口。
- **D2 个人空间"动态/合集"（旧 `Dynamic.vue`/`Series.vue`）**：均无接口、纯静态 → 新版本**只保留投稿/收藏夹/粉丝三个 tab**，去掉动态与合集，避免"看起来能点、其实假数据"。
- **D3 投稿中心"稿件管理"（旧 `Article.vue`）**：无接口 → 新版本该页改为**空状态 + 说明**（"后端暂未提供稿件列表接口"），保留入口但不造假数据。
- **D4 轮播图与分区图标**：B 站官方素材与内网图片不适合长期留在代码里 → 轮播图改为 `config/site.ts` 中 3 条**本地占位**（可一键关闭），分区图标改用 Element Plus + 自制 SVG，不再引用 iconfont。

> 如果你希望 D1–D3 保持"页面存在但提示未开放"，只需改一处开关（`config/feature.ts` 的 `showPlaceholderTabs`），我按你的选择实现。

---

## 13. 分阶段实施计划

| 阶段 | 交付物 | 验收标准 | 预估 |
|---|---|---|---|
| **M0 脚手架** | `AniLink-web` 工程、Vite/TS/ESLint/SCSS tokens、`client.ts`、路由骨架、`DefaultLayout`（顶部导航 + 搜索 + 登录弹窗） | `pnpm dev` 起得来；能登录拿到 token 并持久化；`/main/info` 回显头像昵称 | 0.5 天 |
| **M1 首页与视频卡** | `HomeView`（轮播 + 分区 + 无限流）、`VideoCard`、`pageVideo` 分页加载 | 首屏 20 条、滚动加载、hover 态、骨架屏 | 1 天 |
| **M2 播放页** | `VideoView` + `BiliPlayer` + 弹幕 + 点赞/投币/收藏/关注 + 评论树 + 右侧推荐 | 20 个端点全通；弹幕发送/加载；WS 在线人数；刷新后进度可续 | 2 天 |
| **M3 个人空间与历史** | `UserView`（投稿/收藏夹/粉丝）、`HistoryView`、`AccountView` | 收藏夹增删改批量、关注/取关、历史单删/清空 | 1.5 天 |
| **M4 投稿中心** | 上传流程、稿件管理空态、数据中心图表 | 视频+封面+提交全链路（进度条）、两张图表可切换时间范围 | 1 天 |
| **M5 消息中心与收尾** | `MessageView`（私信 + 系统占位）、WS `/chat`、错误/空态统一、性能与体积预算达标 | 私信实时收发、未读红点、`pnpm build` 通过、产物 ≤ 1.5MB（gzip） | 1 天 |

顺序理由：M0→M1 打通"能跑能看"，M2 是业务重心（20 个端点、最复杂交互），M3/M4 依赖登录态与上传能力，M5 收尾（WS 双通道 + 全局体验）。

---

## 14. 验证方案

1. **接口冒烟**：按 §7.2 表逐条点一次，记录 `code=200` 与实际渲染；写一份 `AniLink-web/docs/api-smoke.md` 勾选表。
2. **页面回归**：8 个页面 × {未登录, 已登录, 空数据, 长列表} 四种状态截图对照。
3. **构建**：`tsc --noEmit` 零错误、`eslint` 零 error、`vite build` 成功；首屏 JS ≤ 400KB（gzip）、总产物 ≤ 1.5MB（gzip）。
4. **跨端**：Chrome/Edge 最新版 + 1366/1440/1920 三档宽度；不做移动端适配（与旧工程一致，避免范围膨胀）。
5. **联调前提**：网关 8090、AL-web 8022、AL-remark 8021、AL-communication 8023/8024、Nacos 可用；数据库可用 `mcp__database__*` 自检。

---

## 15. 风险

| 风险 | 影响 | 应对 |
|---|---|---|
| 后端接口语义与旧前端假设不一致（如 `pageVideo` 与 `getRealTimeRecommend` 的排序字段） | 列表展示偏差 | M0 阶段用 MCP 查库 + 实际调用比对，先固化请求参数再写 UI |
| WS 直连 8024 存在跨域/端口暴露 | 生产不可用 | 通过 `VITE_WS_BASE` + 反代解决；开发期可走 Vite proxy |
| 图片/视频域名（MinIO 内网）在开发机不可达 | 封面全裂 | 统一 `v-lazy` + 占位图 + `onerror` 兜底 |
| Element Plus 按需引入导致部分函数式组件样式缺失 | 样式错乱 | M0 先验证 `ElMessage/ElMessageBox/ElLoading` 三件套，缺样式则退回全量引入 |
| 旧代码里被注释掉的 token 刷新逻辑其实还有后端约束 | 401 死循环 | 刷新失败即登出，单飞锁 + 一次重试上限 |
| 需求边界（D1–D4）反复 | 返工 | 先把决策确认下来再进 M2 |

---

## 16. 本轮不做的事

- 不创建 `AniLink-web` 目录、不写任何组件代码（按你要求"先计划"）。
- 不改 `AniLink-Vue3`（保留为对照与回退）。
- 不改后端、不改网关、不改 Nacos。
- 不做移动端、不做暗色主题、不做 i18n。

---

## 17. 下一步

**进度（2026-11-21 更新）**：M0 脚手架与 M1 首页已完成并验证（`pnpm typecheck`/`pnpm build` 通过，dev server + `/api` 反代实测拿到真实数据）；API 层 71 个端点已全部封装。下一步进入 **M2 播放页**（播放器 + 弹幕 + 评论树 + 四态交互 + WS 在线人数）。

D1–D4 决策已按默认方案执行：搜索页只有"综合"、个人空间只保留投稿/收藏夹/粉丝、稿件管理走空状态、轮播图用 3 条本地占位（`src/config/site.ts`）。
