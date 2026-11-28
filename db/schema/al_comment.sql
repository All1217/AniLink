-- =====================================================================
-- AniLink Phase 5 分库 schema · al_comment
-- 使用方：AL-comment（8028）
-- 表（1 张）：`comment`
-- 来源：db/schema.sql（分库前 video 单库基线，2026-11-25 mysqldump --no-data）
-- 用法：mysql -u root -p < db/schema/al_comment.sql（新环境初始化；已有库用 db/migrations/phase5/ 迁移）
-- 注意：CREATE TABLE IF NOT EXISTS 只保证空库初始化，已有表不会被迁移。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `al_comment`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE `al_comment`;

-- ---------------------------------------------------------------------
-- comment
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论主id',
  `vid` bigint unsigned NOT NULL COMMENT '评论的视频id',
  `uid` bigint unsigned NOT NULL COMMENT '发送者id',
  `root_id` bigint NOT NULL DEFAULT '0' COMMENT '根节点评论的id,如果为0表示为根节点',
  `parent_id` bigint NOT NULL COMMENT '被回复的评论id，只有root_id为0时才允许为0，表示根评论',
  `to_user_id` bigint unsigned NOT NULL COMMENT '回复目标用户id',
  `content` varchar(2000) NOT NULL COMMENT '评论内容',
  `love` int NOT NULL DEFAULT '0' COMMENT '点赞数',
  `bad` int DEFAULT '0' COMMENT '点踩数',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `is_top` tinyint NOT NULL DEFAULT '0' COMMENT '是否置顶 0普通 1置顶',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '软删除 0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论表';

