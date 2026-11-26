# Design

## Context

- 图片存储：MinIO 三个桶 `avatar`、`video-cover`、`video`，公开读；对象名为 `UUID + 扩展名`，访问地址为 `{endpoint}/{bucket}/{object}`（`FileServiceImpl.doUpload`）。上传时已写入 `Content-Type` 与 `Cache-Control: public, max-age=31536000, immutable`。
- 已有 ffmpeg 能力：`al.transcode.ffmpeg-path` 与 `FfmpegRunner`（`ProcessBuilder`、持续读取输出、超时控制），目前只用于 HLS 转码。
- 表结构：`video.cover_url` 为 `varchar(500) NOT NULL`；`user.avatar` 为 `varchar(500) NULL`。
- 前端：`VideoCard` 五列栅格，卡片宽约 240px，容器已用 `aspect-ratio: 16/10`；`HistoryEntry`、`RelatedPanel` 用同类 `<img loading="lazy">`；播放器海报取 `video.coverUrl`；头像走 Element Plus `el-avatar`，不具备懒加载能力。
- 默认头像有两处外部依赖：后端 `MinioConstant.DEFAULT_AVATAR` 硬编码 `http://192.168.150.102:9000/avatar/...`，前端 `AppHeader` 兜底到 `static.hdslb.com`。
- 管理端孤儿清理通过 `AdminStorageRefMapper.findVideoRefs` 匹配 `video_url / cover_url / hls_url`，`findUserRefs` 匹配 `avatar / background`。
- 分页一页 20 条，图片请求直连 MinIO，不经过网关。

参考的网络实践：预生成与按需处理两种路线（[imgproxy vs Thumbor](https://www.bigiron.cc/guides/imgproxy-vs-thumbor-self-hosted-image-resizing)）、`srcset`/`fetchpriority` 与 LCP 处理（[image loading optimization](https://github.com/sujeet-pro/sujeet.pro/blob/main/content/articles/image-loading-optimization/README.md)）、按显示尺寸选图与避免二次缩放（[bilibili 小程序资源加载优化](https://miniapp.bilibili.com/miniprogram-docs/framework/performance/tips/runtime_resource.html)）。

## Goals / Non-Goals

**Goals:**

- 列表封面从“原图直出”降到 480w WebP，典型卡片封面 ≤ 60KB；首页 20 张封面总字节控制在 1.2MB 量级。
- 派生图在上传时一次生成，读取链路零额外延迟、零额外服务。
- 接口路径与前端调用方式不变；旧数据不做强制迁移也能正常显示。
- 默认头像不再依赖内网地址或第三方站点。

**Non-Goals:**

- 不引入 CDN、imgproxy、Thumbor 等新中间件或动态裁剪服务。
- 不做 AVIF、LQIP/blurhash、按 DPR 生成多档位（480/960 两档已够用）。
- 不改上传交互（不做前端裁剪、不做拖拽选区）。
- 不处理视频、HLS 分片与弹幕，不动管理端图片预览。

## Decisions

### D1 上传时预生成派生图，而不是运行时动态处理

封面与头像在上传请求内同步生成派生图并写入 MinIO。备选方案：引入 imgproxy/Thumbor 按 URL 参数动态裁剪（读取零存储冗余，但要多维护一个服务与签名密钥，自用规模不划算）；首次访问时生成并回填缓存（MinIO 本身不支持缩放，需要额外接口与并发去重，冷启动延迟明显）。预生成实现最轻，且项目已有 ffmpeg。

### D2 复用 ffmpeg 生成 WebP，不新增 Java 图像库

`FfmpegRunner` 已封装进程调用与超时，图片转换只需新增命令模板：

- 封面 480：`-vf "scale='min(480,iw)':-2" -c:v libwebp -quality 78 -compression_level 4`
- 封面 960：同上，宽度改 960
- 头像：`-vf "scale=192:192:force_original_aspect_ratio=increase,crop=192:192"`

`min(w,iw)` 保证小图不被放大；`-2` 保证高度为偶数。备选方案 Thumbnailator + webp-imageio 需要新增两个依赖与本地 native 库，收益不明显，放弃。风险是部分 ffmpeg 构建未编译 libwebp，因此生成失败必须降级而不是抛错。

图片处理是 CPU 密集操作，用 `Semaphore(2)` 限制并发 ffmpeg 进程，避免与 HLS 转码同时抢占 CPU；单张超时 30 秒。

### D3 派生对象命名与同桶存储

封面派生对象为 `{uuid}_480.webp` 与 `{uuid}_960.webp`，头像为 `{uuid}_192.webp`，与原图同桶。同桶可以复用现有的公开读策略、`immutable` 缓存头和管理端按桶清理逻辑，不新增桶也不改部署配置。

派生成功时**只保存派生图**（封面 480 + 960，头像 192），不再上传原图：原图仅用于本次转换，避免产生无人引用的大对象被清理任务反复扫描。派生失败时才上传原图并返回原地址，保证投稿不中断。

### D4 只加一列 `cover_thumb_url`，缩略图地址由后端推导后落库

投稿保存时，后端用统一工具 `ImageUrls.thumbOf(coverUrl)` 把 `{uuid}_960.webp` 推导成 `{uuid}_480.webp` 写入新列；列表查询用 `coalesce(v.cover_thumb_url, v.cover_url) as cover_thumb_url`，旧数据为 `NULL` 时自动回退。

备选方案：不加列，由前端按命名约定推导（接口契约里没有缩略图字段，约定会散落到多个前端组件，管理端无法核对）；加 `cover_480/cover_960` 两列（`cover_url` 已经存 960，多一列纯冗余）。推导规则集中在后端一处并配单测，上传与落库必须使用同一实现，避免两边写错后缀。

头像不新增列：上传后 `user.avatar` 直接保存 192 方图地址，旧头像保持原值直到用户重新上传。

### D5 上传接口形状不变，列表接口新增字段

`/main/file/upload/video/cover` 仍返回 960 地址字符串，`/main/file/upload/avatar` 仍返回 192 地址字符串，`src/api/upload.ts` 与 `UploadForm` 不需要改。列表 `VideoVo` 新增 `coverThumbUrl`，属于响应新增字段，旧前端忽略即可。

### D6 前端按用途取图，首屏单独提权

- 新增 `src/utils/media.ts`：`coverThumb(video)`、`coverSrcset(video)`（480w/960w）、共享占位图常量，替换现有三处重复的内联 SVG 占位。
- `VideoCard` 增加 `priority` prop：首页前 5 张（首行）用 `loading="eager"` + `fetchpriority="high"`，其余保持 `lazy` + `decoding="async"`；`src` 用 480 缩略图，`srcset` 提供 480/960 两档，`sizes="(max-width: 1240px) 25vw, 240px"`。
- `HistoryEntry`、`RelatedPanel` 改为缩略图 + `decoding="async"`。
- 播放器海报继续用 `coverUrl`（新上传即 960），不额外加字段。
- 头像保持 `el-avatar`：192 方图通常 ≤ 15KB，没必要为它引入懒加载；旧头像加载失败时 `el-avatar` 的默认插槽（昵称首字）自然兜底。

### D7 默认头像本地化

后端注册时不再写入 `DEFAULT_AVATAR`，`user.avatar` 留空；前端 `AppHeader` 等处以本地内置 SVG 占位替代 `static.hdslb.com`。旧用户记录里的内网地址不改库，由图片 `onerror` 回退到占位。

### D8 管理端清理补充引用列

`AdminStorageRefMapper.findVideoRefs` 增加 `cover_thumb_url` 匹配，并在 `case` 中返回 `cover_thumb_url` 字段名；这样新上传的 480 对象与存量回填生成的 480 对象都不会被判成孤儿。`user.background` 目前没有上传入口，保持原样。

### D9 存量回填做成管理端手动动作

管理端存储页新增「重建封面缩略图」按钮，接口遍历 `cover_thumb_url is null and cover_url is not null` 的稿件（每批 50 条）：按 `cover_url` 从 MinIO 取原对象 → 生成 480 WebP → 上传 `{uuid}_480.webp` → 写回 `cover_thumb_url`。返回扫描/成功/跳过/失败数量，只处理空值，天然幂等。`cover_url` 不属于本 MinIO 域名的记录计入跳过。

### D10 配置项

新增 `al.image.*`：`enabled`（默认 true）、`cover-quality`（78）、`avatar-size`（192）、`max-cover-bytes`（5MB）、`max-avatar-bytes`（2MB）、`timeout-seconds`（30）。ffmpeg 路径复用 `al.transcode.ffmpeg-path`，临时目录复用 `al.transcode.tmp-dir` 下的 `images/`。

### D11 上传校验放在业务层

`spring.servlet.multipart.max-file-size` 为 1024MB，不能拦住图片。在文件服务入口校验：扩展名与 `Content-Type` 必须属于图片白名单，封面 ≤ 5MB、头像 ≤ 2MB，超限抛业务异常返回可读提示，保证不写入对象存储。

## Risks / Trade-offs

- [ffmpeg 缺少 libwebp 编码器] → 生成失败时上传原图并记 `warn` 日志；可配置 `al.image.enabled=false` 整体关闭派生。
- [图片转换抢占 CPU，影响 HLS 转码] → 信号量限制 2 个并发、单张 30 秒超时；图片通常在 300ms 内完成。
- [派生图与原图二选一，原图不再保留] → 自用项目接受；如后续需要重新裁剪，存量回填可再次从原图生成（回填前原图仍在）。
- [存量稿件仍下载原图] → 回填按钮可一次性补齐；未回填也不影响功能，只是没有体积收益。
- [上传耗时增加一次 ffmpeg 调用] → 封面与头像上传本来就是异步等待，新增 100–300ms 可接受。
- [推导规则与上传命名不一致会导致 404] → `ImageUrls` 单一实现 + 单测覆盖 `_960.webp`、无后缀、空值三种输入。

## Migration Plan

1. 执行 `ALTER TABLE video ADD COLUMN cover_thumb_url varchar(500) NULL AFTER cover_url;` 并同步更新 `db/schema.sql`。
2. 发布 `AL-web`（新增派生逻辑、列表字段、管理端引用列与回填接口）。
3. 发布 `AniLink-web`（缩略图、srcset、首屏优先级、本地占位头像）。
4. 可选：在管理端存储页点一次「重建封面缩略图」，补齐存量稿件。
5. 回滚：前端回滚后仍能读 `cover_url`；后端回滚时新增列可保留，不影响旧代码读写。

## Open Questions

（无：编码质量、档位宽度都可在实现后按实测微调，不改变规格与任务拆分。）
