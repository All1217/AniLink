-- =====================================================================
-- AniLink Phase 5 分库 schema · al_danmaku
-- 使用方：AL-danmaku（8029 / WS 8030）
-- 表（2 张）：`danmu`、`sub_danmu`
-- 来源：db/schema.sql（分库前 video 单库基线，2026-11-25 mysqldump --no-data）
-- 用法：mysql -u root -p < db/schema/al_danmaku.sql（新环境初始化；已有库用 db/migrations/phase5/ 迁移）
-- 注意：CREATE TABLE IF NOT EXISTS 只保证空库初始化，已有表不会被迁移。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `al_danmaku`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE `al_danmaku`;

-- ---------------------------------------------------------------------
-- danmu
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `danmu` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '弹幕ID',
  `vid` bigint unsigned NOT NULL COMMENT '视频ID',
  `uid` bigint unsigned NOT NULL COMMENT '用户ID',
  `content` varchar(100) NOT NULL COMMENT '弹幕内容',
  `fontsize` tinyint NOT NULL DEFAULT '25' COMMENT '字体大小',
  `mode` tinyint NOT NULL DEFAULT '1' COMMENT '弹幕模式 0滚动 1顶部 2底部',
  `color` varchar(7) NOT NULL DEFAULT '#FFFFFF' COMMENT '弹幕颜色 6位十六进制标准格式',
  `time_point` double NOT NULL COMMENT '弹幕所在视频的时间点',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '弹幕状态 1默认过审 2被举报审核中 3删除',
  `create_date` datetime NOT NULL COMMENT '发送弹幕的日期时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='弹幕表';

-- ---------------------------------------------------------------------
-- sub_danmu
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sub_danmu` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '弹幕ID',
  `vid` bigint unsigned NOT NULL COMMENT '视频ID',
  `uid` bigint unsigned NOT NULL COMMENT '用户ID',
  `content` varchar(100) NOT NULL COMMENT '弹幕内容',
  `fontsize` tinyint NOT NULL DEFAULT '25' COMMENT '字体大小',
  `mode` tinyint NOT NULL DEFAULT '1' COMMENT '弹幕模式 0滚动 1顶部 2底部',
  `color` varchar(7) NOT NULL DEFAULT '#FFFFFF' COMMENT '弹幕颜色 6位十六进制标准格式',
  `time_point` double NOT NULL COMMENT '弹幕所在视频的时间点',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '弹幕状态 1默认过审 2被举报审核中 3删除',
  `create_date` datetime NOT NULL COMMENT '发送弹幕的日期时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='弹幕表-测试表';

