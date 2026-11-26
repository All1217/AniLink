# Proposal

## Why

全站图片（首页/搜索/历史/相关推荐的封面、头像、主页背景、播放器海报）目前全部由 MinIO 直出**用户上传的原图**：没有缩略图、没有格式转换、上传也不限制尺寸。列表卡片实际显示宽度约 240px，却要下载几百 KB 到数 MB 的原图；首页一次 20 张封面，图片就是首屏流量的大头。

默认头像还在 `MinioConstant.DEFAULT_AVATAR` 里硬编码了内网 MinIO 地址，前端兜底又指向外站 `static.hdslb.com`，换网络环境就会裂图，也把内网地址写进了代码。

现在补这块的时机合适：投稿、HLS 转码、管理端存储清理都已稳定，图片是唯一还在“原样搬运”的资源类型；继续做搜索和推荐只会放大同一问题。

## What Changes

- 上传即生成派生图：封面生成 480w（列表缩略图）与 960w（详情海报），头像生成 192×192 方图，统一 WebP；成功时只保存派生图，派生失败不阻塞上传、保存原图并回退。
- `video` 表新增可空列 `cover_thumb_url`；列表接口返回缩略图地址，旧数据为空时回退 `cover_url`。
- 前端按用途取图：列表/历史/相关推荐用缩略图并配合 `srcset`/`sizes`，播放器海报用 960w，头像统一用 192 方图。
- 首屏与懒加载：第一行封面 `loading="eager"` + `fetchpriority="high"`，其余 `loading="lazy"` + `decoding="async"`；图片容器固定宽高比，加载完成淡入，失败回退占位图。
- 去掉硬编码的内网默认头像与外站默认头像，改为前端内置占位图。
- 上传体积校验：封面 ≤ 5MB、头像 ≤ 2MB，超限直接拒绝。
- 管理端存储引用检查纳入新增的缩略图地址，避免一键清理误删仍在使用的对象。
- 存量封面提供一次性回填入口（管理端触发），未回填的旧数据继续走原图回退。
- 不引入 CDN、imgproxy、Thumbor 等新中间件，也不做 AVIF；派生图复用项目已有的 ffmpeg。

## Capabilities

### New Capabilities

- `image-delivery`: 站内图片从上传、派生、存储到前端加载的行为约束，包括尺寸档位与格式、原图回退、加载优先级与占位、缓存与默认头像策略。

### Modified Capabilities

（无：项目当前 `openspec/specs` 下还没有已归档的能力规格。）

## Impact

- 后端 `AL-web`：文件上传服务、`Video`/`VideoVo`/`VideoRecordFormDTO` 及对应 mapper、管理端存储引用检查、`db/schema.sql`。
- 前端 `AniLink-web`：`VideoCard`、`HistoryEntry`、`RelatedPanel`、`BiliPlayer`、`AppHeader` 及头像相关组件。
- 存储：`video-cover`、`avatar` 桶新增派生对象；对象存储清理逻辑需同步识别。
- 配置/依赖：复用已有 ffmpeg（`AL_FFMPEG_PATH`）与 `al.transcode.*` 同机部署，新增 `al.image.*` 配置；不新增中间件。
- 兼容性：接口路径不变，仅响应新增字段；旧数据回退原图，前端无“必须回填”的硬依赖。
- 风险：ffmpeg 构建缺 libwebp 时派生失败，需降级为原图并记录日志。
