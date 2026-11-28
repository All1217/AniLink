-- =====================================================================
-- AniLink Phase 5 分库 schema · al_playback
-- 使用方：AL-playback（8027）
-- 表（1 张）：`bh_video`
-- 来源：db/schema.sql（分库前 video 单库基线，2026-11-25 mysqldump --no-data）
-- 用法：mysql -u root -p < db/schema/al_playback.sql（新环境初始化；已有库用 db/migrations/phase5/ 迁移）
-- 注意：CREATE TABLE IF NOT EXISTS 只保证空库初始化，已有表不会被迁移。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `al_playback`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE `al_playback`;

-- ---------------------------------------------------------------------
-- bh_video
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bh_video` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `uid` bigint unsigned NOT NULL COMMENT '用户ID',
  `vid` bigint unsigned NOT NULL COMMENT '视频ID',
  `view_time` datetime NOT NULL COMMENT '浏览时间',
  `duration` double NOT NULL DEFAULT '0' COMMENT '上次观看到哪个节点',
  `is_finish` tinyint NOT NULL DEFAULT '0' COMMENT '是否看完 0未看完 1已看完',
  `device` varchar(64) DEFAULT NULL COMMENT '设备信息 如手机/PC等',
  `ip` varchar(35) DEFAULT NULL COMMENT 'IPv4地址',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除 0否 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_uid_vid` (`uid`,`vid`),
  KEY `idx_uid` (`uid`),
  KEY `idx_uid_viewtime` (`uid`,`view_time` DESC),
  KEY `idx_vid` (`vid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户视频浏览历史';

