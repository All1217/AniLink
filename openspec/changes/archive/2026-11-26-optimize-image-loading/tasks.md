# Tasks

## 1. 数据模型与配置

- [x] 1.1 在 `db/schema.sql` 的 `video` 表增加可空列 `cover_thumb_url varchar(500)`，并准备同样的 `ALTER TABLE`；在开发库执行前先与用户确认。验证：`mcp__database__describe_table` 能看到该列且 nullable，`db/schema.sql` 与库结构一致。
- [x] 1.2 给 `Video` 实体与 `VideoVo` 增加 `coverThumbUrl` 字段，`VideoMapper.insertOne` 写入 `cover_thumb_url`，`pageItem`/`batchSelect` 用 `coalesce(cover_thumb_url, cover_url)` 输出。验证：`mvn -pl AL-web test` 通过，且实测 `GET /main/video/public/pageVideo` 每条记录含 `coverThumbUrl`。
- [x] 1.3 新增 `al.image.*` 配置类（`enabled`、`cover-quality`、`avatar-size`、`max-cover-bytes`、`max-avatar-bytes`、`timeout-seconds`）并在 `application.yml` 写默认值。验证：新增绑定测试断言默认值，`mvn -pl AL-web test` 通过。

## 2. 派生图生成

- [x] 2.1 新增 `ImageUrls` 工具：`thumbOf(coverUrl)` 把 `_960.webp` 推导成 `_480.webp`，非约定地址返回 `null`。验证：新增 `ImageUrlsTest` 覆盖 `_960.webp`、`.jpg` 原图、空值三类输入并通过。
- [x] 2.2 新增 `ImageProcessService`：用现有 `FfmpegRunner` 生成封面 480/960 与头像 192 方图，信号量限制 2 并发、单张 30 秒超时、临时文件清理。验证：新增测试用真实 ffmpeg 生成 2000×1200 输入并断言输出为 WebP 且宽度分别为 480/960/192；无 ffmpeg 环境时按条件跳过。
- [x] 2.3 `FileServiceImpl` 接入派生流程：封面保存 480+960、头像保存 192，派生失败时上传原图并回退；同时校验图片类型与体积上限（封面 5MB、头像 2MB）。验证：新增测试覆盖超限拒绝、非图片拒绝、派生失败回退三种分支，`mvn -pl AL-web test` 通过。
- [x] 2.4 注册不再写入 `MinioConstant.DEFAULT_AVATAR`，`user.avatar` 留空。验证：注册接口返回 `avatar` 为空，`LoginTest`/`AdminUserTest` 等既有测试仍通过。

## 3. 列表接口与前端取图

- [x] 3.1 新增 `AniLink-web/src/utils/media.ts`：缩略图取值、`srcset` 拼接、共享封面占位常量，替换 `VideoCard`/`HistoryEntry`/`RelatedPanel` 里重复的内联 SVG。验证：`pnpm typecheck` 通过，三个组件不再各自定义占位常量。
- [x] 3.2 改造 `VideoCard`：`src` 用 480 缩略图，`srcset` 提供 480/960，`sizes` 按五列栅格，新增 `priority` prop 控制 `eager + fetchpriority=high`；`HomeView` 给前 5 张传 `priority`。验证：`pnpm build` 通过，首页首行 5 张为 eager 且带高优先级，其余为 lazy。
- [x] 3.3 `HistoryEntry`、`RelatedPanel` 改用缩略图并加 `decoding="async"`。验证：`pnpm build` 通过，历史页与详情页相关推荐请求的是 480 地址。
- [x] 3.4 `AppHeader` 等处的默认头像改为本地内置占位，删除 `static.hdslb.com` 引用；头像加载失败回退占位。验证：`pnpm build` 通过，无头像用户打开首页时网络面板不出现外站或内网图片请求。
- [x] 3.5 确认播放器海报取 `coverUrl`（新上传即 960），必要时补注释说明。验证：详情页 Network 中海报地址为 `_960.webp`。

## 4. 管理端清理与回填

- [x] 4.1 `AdminStorageRefMapper.findVideoRefs` 增加 `cover_thumb_url` 匹配并返回对应字段名。验证：`AdminStorageTest` 新增用例——480 派生对象被稿件引用时删除返回 409 且对象保留。
- [x] 4.2 新增存量回填服务与接口（分批 50 条、只处理 `cover_thumb_url is null`、非本 MinIO 域名跳过、返回扫描/成功/跳过/失败数量）。验证：`AdminStorageTest` 覆盖成功写回、幂等跳过、外部域名跳过三种情况。
- [x] 4.3 管理端存储页增加「重建封面缩略图」按钮与结果提示。验证：`npm run build` 通过，点击后展示统计数字。

## 5. 集成验证与文档

- [x] 5.1 本地全链路实测：上传 2000px 以上封面，MinIO 出现 `_480.webp` 与 `_960.webp`，列表返回 480 地址，首页 20 张封面总字节明显低于改造前（记录实测数值）。
- [x] 5.2 更新 `db/README.md` 与 `AGENTS.md`：新列、图片派生链路、`al.image.*` 配置、默认头像策略。验证：文档描述的字段与命令和代码一致。
- [x] 5.3 收尾回归：`mvn -pl AL-web test` 与 `AniLink-web` 的 `pnpm build` 全绿，并手动过一遍首页、搜索、历史、详情、个人主页。

## Workflow follow-up

- 评审通过后归档该 change，确认 `openspec/specs/image-delivery/spec.md` 已生成。
- 归档后复核 `openspec validate --strict` 无新增告警。
