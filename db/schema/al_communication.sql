-- =====================================================================
-- AniLink Phase 5 分库 schema · al_communication
-- 使用方：AL-communication（8023 / WS 8024）
-- 表（2 张）：`chat`、`chat_message`
-- 来源：db/schema.sql（分库前 video 单库基线，2026-11-25 mysqldump --no-data）
-- 用法：mysql -u root -p < db/schema/al_communication.sql（新环境初始化；已有库用 db/migrations/phase5/ 迁移）
-- 注意：CREATE TABLE IF NOT EXISTS 只保证空库初始化，已有表不会被迁移。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `al_communication`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE `al_communication`;

-- ---------------------------------------------------------------------
-- chat
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '唯一主键',
  `target_id` bigint unsigned NOT NULL COMMENT '对象UID',
  `sender_id` bigint unsigned NOT NULL COMMENT '用户UID',
  `latest_time` datetime NOT NULL COMMENT '最近接收消息的时间或最近打开聊天窗口的时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `from_to` (`target_id`,`sender_id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天表';

-- ---------------------------------------------------------------------
-- chat_message
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat_message` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '唯一主键',
  `sender_id` bigint unsigned NOT NULL COMMENT '消息发送者',
  `target_id` bigint unsigned NOT NULL COMMENT '消息接收者',
  `content` varchar(500) NOT NULL COMMENT '消息内容',
  `sender_del` tinyint NOT NULL DEFAULT '0' COMMENT '发送者是否删除',
  `target_del` tinyint NOT NULL DEFAULT '0' COMMENT '接受者是否删除',
  `withdraw` tinyint NOT NULL DEFAULT '0' COMMENT '是否撤回',
  `time` datetime NOT NULL COMMENT '消息发送时间',
  `message_type` tinyint NOT NULL DEFAULT '0' COMMENT '消息类型，0文本，1图片，2文件',
  `read_status` tinyint NOT NULL DEFAULT '0' COMMENT '是否已读，0未读1已读',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天记录表';

