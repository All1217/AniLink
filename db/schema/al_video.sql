-- =====================================================================
-- AniLink Phase 5 分库 schema · al_video
-- 使用方：AL-video（8026）
-- 表（4 张）：`video`、`video_stats`、`user_video`、`category`
-- 来源：db/schema.sql（分库前 video 单库基线，2026-11-25 mysqldump --no-data）
-- 用法：mysql -u root -p < db/schema/al_video.sql（新环境初始化；已有库用 db/migrations/phase5/ 迁移）
-- 注意：CREATE TABLE IF NOT EXISTS 只保证空库初始化，已有表不会被迁移。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `al_video`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE `al_video`;

-- ---------------------------------------------------------------------
-- video
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `video` (
  `vid` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '视频ID',
  `uid` bigint unsigned NOT NULL COMMENT '投稿用户ID',
  `title` varchar(80) NOT NULL COMMENT '标题',
  `type` tinyint NOT NULL DEFAULT '1' COMMENT '类型 1自制 2转载',
  `auth` tinyint NOT NULL DEFAULT '0' COMMENT '作者声明 0不声明 1未经允许禁止转载',
  `duration` double NOT NULL DEFAULT '0' COMMENT '播放总时长 单位秒',
  `mc_id` varchar(20) NOT NULL COMMENT '主分区ID',
  `sc_id` varchar(20) NOT NULL COMMENT '子分区ID',
  `tags` varchar(500) DEFAULT NULL COMMENT '标签，不同标签之间用空格分隔',
  `descr` varchar(2000) DEFAULT NULL COMMENT '简介',
  `cover_url` varchar(500) NOT NULL COMMENT '封面url',
  `cover_thumb_url` varchar(500) DEFAULT NULL COMMENT '封面缩略图url（480w WebP），为空时回退 cover_url',
  `video_url` varchar(500) NOT NULL COMMENT '视频url',
  `hls_url` varchar(500) DEFAULT NULL COMMENT 'HLS 播放列表地址，空表示暂用 MP4',
  `transcode_status` tinyint NOT NULL DEFAULT '0' COMMENT '转码状态 0待转码 1转码中 2已完成 3失败',
  `transcode_error` varchar(500) DEFAULT NULL COMMENT '最近一次转码失败原因',
  `transcode_updated_at` datetime DEFAULT NULL COMMENT '转码状态更新时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0审核中 1已过审 2未通过 3已删除',
  `upload_date` datetime NOT NULL COMMENT '上传时间',
  `delete_date` datetime DEFAULT NULL COMMENT '删除时间',
  `visible` tinyint DEFAULT '0' COMMENT '可见范围：0公开，1仅自己可见',
  PRIMARY KEY (`vid`),
  UNIQUE KEY `vid` (`vid`),
  KEY `idx_transcode_status` (`transcode_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='视频表';

-- ---------------------------------------------------------------------
-- video_stats
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `video_stats` (
  `vid` bigint unsigned NOT NULL COMMENT '视频ID',
  `play` int NOT NULL DEFAULT '0' COMMENT '播放量',
  `danmu` int NOT NULL DEFAULT '0' COMMENT '弹幕数',
  `good` int NOT NULL DEFAULT '0' COMMENT '点赞数',
  `bad` int NOT NULL DEFAULT '0' COMMENT '点踩数',
  `coin` int NOT NULL DEFAULT '0' COMMENT '投币数',
  `collect` int NOT NULL DEFAULT '0' COMMENT '收藏数',
  `share` int NOT NULL DEFAULT '0' COMMENT '分享数',
  `comment` int DEFAULT '0' COMMENT '评论数量统计',
  PRIMARY KEY (`vid`),
  UNIQUE KEY `vid` (`vid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='视频数据统计表';

-- ---------------------------------------------------------------------
-- user_video
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_video` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '唯一标识',
  `uid` bigint unsigned NOT NULL COMMENT '观看视频的用户UID',
  `vid` bigint unsigned NOT NULL COMMENT '视频ID',
  `play` int NOT NULL DEFAULT '0' COMMENT '播放次数',
  `love` tinyint NOT NULL DEFAULT '0' COMMENT '点赞 0没赞 1已点赞',
  `unlove` tinyint NOT NULL DEFAULT '0' COMMENT '是否点踩：0-是；1-否',
  `coin` tinyint NOT NULL DEFAULT '0' COMMENT '投币数，默认0，最高为2',
  `collect` tinyint NOT NULL DEFAULT '0' COMMENT '是否收藏：1-已收藏；2-未收藏',
  `play_time` datetime NOT NULL COMMENT '最近播放时间',
  `love_time` datetime DEFAULT NULL COMMENT '最近点赞时间',
  `coin_time` datetime DEFAULT NULL COMMENT '最近投币时间',
  `collect_time` datetime DEFAULT NULL COMMENT '最近收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uid_vid__index` (`uid`,`vid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户视频关联表';

-- ---------------------------------------------------------------------
-- category
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `category` (
  `mc_id` varchar(20) NOT NULL COMMENT '主分区ID',
  `sc_id` varchar(20) NOT NULL COMMENT '子分区ID',
  `mc_name` varchar(20) NOT NULL COMMENT '主分区名称',
  `sc_name` varchar(20) NOT NULL COMMENT '子分区名称',
  `descr` varchar(200) DEFAULT NULL COMMENT '描述',
  `rcm_tag` varchar(500) DEFAULT NULL COMMENT '推荐标签',
  PRIMARY KEY (`mc_id`,`sc_id`),
  KEY `mc_id` (`mc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='分区表';

