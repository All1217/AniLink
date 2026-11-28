-- =====================================================================
-- AniLink Phase 5 分库 schema · al_social
-- 使用方：AL-social（8031）
-- 表（3 张）：`follow`、`favorite`、`favorite_video`
-- 来源：db/schema.sql（分库前 video 单库基线，2026-11-25 mysqldump --no-data）
-- 用法：mysql -u root -p < db/schema/al_social.sql（新环境初始化；已有库用 db/migrations/phase5/ 迁移）
-- 注意：CREATE TABLE IF NOT EXISTS 只保证空库初始化，已有表不会被迁移。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `al_social`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE `al_social`;

-- ---------------------------------------------------------------------
-- follow
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `follow` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '关注与被关注关系ID',
  `follower_id` bigint unsigned NOT NULL COMMENT '关注者ID',
  `followee_id` bigint unsigned NOT NULL COMMENT '被关注者ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `er_ee__index` (`follower_id`,`followee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='关注与被关注关系表';

-- ---------------------------------------------------------------------
-- favorite
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `favorite` (
  `fid` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '收藏夹ID',
  `uid` bigint unsigned NOT NULL COMMENT '所属用户ID',
  `type` tinyint NOT NULL DEFAULT '1' COMMENT '收藏夹类型 1默认收藏夹 2用户创建',
  `visible` tinyint NOT NULL DEFAULT '1' COMMENT '是否公开，0-隐藏，1-公开',
  `cover` varchar(255) DEFAULT NULL COMMENT '收藏夹封面',
  `title` varchar(20) NOT NULL COMMENT '标题',
  `description` varchar(200) DEFAULT '' COMMENT '简介',
  `count` int NOT NULL DEFAULT '0' COMMENT '收藏夹中视频数量',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除 0否 1已删除',
  PRIMARY KEY (`fid`),
  UNIQUE KEY `fid` (`fid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收藏夹';

-- ---------------------------------------------------------------------
-- favorite_video
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `favorite_video` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '唯一标识',
  `vid` bigint NOT NULL COMMENT '视频ID',
  `fid` int NOT NULL COMMENT '收藏夹ID',
  `time` datetime NOT NULL COMMENT '收藏时间',
  `is_remove` tinyint NOT NULL DEFAULT '0' COMMENT '是否移除 0否 1已移除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `vid_fid__index` (`vid`,`fid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='视频收藏夹关系表';

