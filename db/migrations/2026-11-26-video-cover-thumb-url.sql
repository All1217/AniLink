-- 已有开发/部署库的增量迁移：视频封面增加 480w 缩略图地址
-- 执行一次即可；重复执行会因列已存在而报错，属预期。
ALTER TABLE `video`
  ADD COLUMN `cover_thumb_url` varchar(500) DEFAULT NULL
  COMMENT '封面缩略图url（480w WebP），为空时回退 cover_url'
  AFTER `cover_url`;
