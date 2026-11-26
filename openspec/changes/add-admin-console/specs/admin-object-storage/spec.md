# Spec Delta

## Purpose

让管理员浏览、预览并删除 MinIO 中的真实对象，同时在删除前检查业务引用，避免删掉正在播放的视频或正在使用的头像/封面。

## ADDED Requirements

### Requirement: 存储浏览范围
管理端 MUST 只允许浏览 `avatar`、`video`、`video-cover` 三个桶；MUST 支持按前缀分页列出对象及大小、更新时间。

#### Scenario: 浏览视频桶
- **WHEN** 管理员选择 `video` 桶与前缀 `hls/`
- **THEN** 返回该前缀下的对象分页，包含 key、大小、最后修改时间

#### Scenario: 访问未授权桶
- **WHEN** 请求 `bucket` 不在三个白名单内
- **THEN** 返回参数错误，不访问 MinIO

### Requirement: 预览与下载
管理端 MUST 通过短时预签名 URL 预览/下载对象，不得把 MinIO 的长期密钥返回给前端。

#### Scenario: 预览图片
- **WHEN** 管理员点击某个封面对象
- **THEN** 返回带有效期的预签名 GET URL，前端用该 URL 展示

### Requirement: 删除前引用检查
删除对象前 MUST 检查 `video.video_url`、`video.cover_url`、`video.hls_url`、`user.avatar`、`user.background` 的引用；存在引用时 MUST 返回冲突并列出引用方，不删除对象。

#### Scenario: 删除被稿件引用的视频
- **WHEN** 管理员删除被某 `video.video_url` 引用的对象
- **THEN** 返回 409 与引用稿件列表，MinIO 对象保持不变

#### Scenario: 删除被 HLS 引用的分片
- **WHEN** 删除 `video` 桶 `hls/{vid}/` 下的对象，而该 vid 的 `hls_url` 仍指向该目录
- **THEN** 返回 409，不删除分片

#### Scenario: 删除未被引用的孤儿对象
- **WHEN** 对象没有被任何视频或用户引用
- **THEN** 删除成功，MinIO 中对象消失

### Requirement: 删除范围、一键清理与失败处理
管理端 MUST 支持单对象删除和按当前 `bucket + prefix` 一键清理冗余对象。单对象删除 MUST 只删除指定对象；一键清理 MUST 递归扫描当前范围，逐个检查引用，只删除没有引用的对象，跳过被引用对象，并返回扫描/删除/跳过/失败计数。两种操作 MUST NOT 创建或删除桶，MUST NOT 删除有引用的对象；MinIO 删除失败 MUST 记录为失败，不得报告为成功。

#### Scenario: 删除失败
- **WHEN** MinIO 返回删除错误
- **THEN** 接口返回失败，管理端提示对象未被删除

#### Scenario: 一键清理跳过被引用对象
- **WHEN** 管理员对当前桶/前缀执行一键清理，范围内同时有无引用对象和被引用对象
- **THEN** 无引用对象被删除，被引用对象被跳过，响应返回正确的 scanned/deleted/skipped 计数

#### Scenario: 一键清理部分失败
- **WHEN** 清理过程中某个对象删除失败
- **THEN** 该对象计入 failed，其余对象继续处理，响应不把失败对象报告为已删除
