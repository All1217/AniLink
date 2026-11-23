-- 视频播放分片加载优化（HLS）：
-- 1) hls_url：转码完成后的播放列表地址，播放器优先使用它，为空则回退原 MP4（Range 伪流式）
-- 2) transcode_status：转码队列状态，0 待转码（存量数据默认 0，服务启动时自动补跑）
-- 3) transcode_error / transcode_updated_at：失败原因与更新时间，便于排查与重试
-- 说明：本脚本只加列，不改动既有数据；转码由 AL-web 异步执行，期间照常播放原 MP4。
ALTER TABLE video
  ADD COLUMN hls_url varchar(500) NULL COMMENT 'HLS 播放列表地址，空表示暂用 MP4' AFTER video_url,
  ADD COLUMN transcode_status tinyint NOT NULL DEFAULT 0 COMMENT '转码状态 0待转码 1转码中 2已完成 3失败' AFTER hls_url,
  ADD COLUMN transcode_error varchar(500) NULL COMMENT '最近一次转码失败原因' AFTER transcode_status,
  ADD COLUMN transcode_updated_at datetime NULL COMMENT '转码状态更新时间' AFTER transcode_error,
  ADD KEY idx_transcode_status (transcode_status);
