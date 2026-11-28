-- =====================================================================
-- AniLink Phase 5 分库 schema · al_user
-- 使用方：AL-user（8025）
-- 表（3 张）：`user`、`user_stats`、`user_tag`
-- 来源：db/schema.sql（分库前 video 单库基线，2026-11-25 mysqldump --no-data）
-- 用法：mysql -u root -p < db/schema/al_user.sql（新环境初始化；已有库用 db/migrations/phase5/ 迁移）
-- 注意：CREATE TABLE IF NOT EXISTS 只保证空库初始化，已有表不会被迁移。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `al_user`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE `al_user`;

-- ---------------------------------------------------------------------
-- user
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `uid` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户账号',
  `password` varchar(255) NOT NULL COMMENT '用户密码',
  `password_updated_at` datetime DEFAULT NULL,
  `nickname` varchar(32) NOT NULL COMMENT '用户昵称',
  `avatar` varchar(500) DEFAULT NULL COMMENT '用户头像url',
  `background` varchar(500) DEFAULT NULL COMMENT '主页背景图url',
  `gender` tinyint NOT NULL DEFAULT '2' COMMENT '性别 0女 1男 2未知',
  `description` varchar(100) DEFAULT NULL COMMENT '个性签名',
  `exp` int NOT NULL DEFAULT '0' COMMENT '经验值',
  `coin` double NOT NULL DEFAULT '0' COMMENT '硬币数',
  `vip` tinyint NOT NULL DEFAULT '0' COMMENT '会员类型 0普通用户 1月度大会员 2季度大会员 3年度大会员',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0正常 1封禁 2注销',
  `role` tinyint NOT NULL DEFAULT '0' COMMENT '角色类型 0普通用户 1管理员 2超级管理员',
  `auth` tinyint NOT NULL DEFAULT '0' COMMENT '官方认证 0普通用户 1个人认证 2机构认证',
  `auth_msg` varchar(30) DEFAULT NULL COMMENT '认证说明',
  `create_date` datetime NOT NULL COMMENT '创建时间',
  `delete_date` datetime DEFAULT NULL COMMENT '注销时间',
  PRIMARY KEY (`uid`),
  UNIQUE KEY `uid` (`uid`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='用户表';

-- ---------------------------------------------------------------------
-- user_stats
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_stats` (
  `fans_count` bigint DEFAULT '0' COMMENT '粉丝数',
  `follow_count` int DEFAULT '0' COMMENT '关注数',
  `like_count` bigint DEFAULT '0' COMMENT '获赞数',
  `play_count` bigint DEFAULT '0' COMMENT '播放数',
  `video_count` int DEFAULT '0' COMMENT '投稿数',
  `collection_count` int DEFAULT '0' COMMENT '合集数',
  `collect_count` int DEFAULT '0' COMMENT '收藏数',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `school` varchar(64) DEFAULT NULL COMMENT '学校信息',
  `notice` varchar(500) DEFAULT NULL COMMENT '公告',
  `uid` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `masterpiece` bigint DEFAULT NULL COMMENT '置顶视频',
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户信息表';

-- ---------------------------------------------------------------------
-- user_tag
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `uid` bigint unsigned NOT NULL COMMENT '用户ID',
  `tag_name` varchar(50) NOT NULL COMMENT '语义标签名，如“二次元”',
  `weight` float NOT NULL DEFAULT '0' COMMENT '标签权重',
  `version` int DEFAULT '1' COMMENT '版本号（用于乐观锁）',
  `created_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次创建时间',
  `updated_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uid_tag` (`uid`,`tag_name`) COMMENT '每个用户对每个标签只有一条记录',
  KEY `idx_uid` (`uid`) COMMENT '按用户查询加速',
  KEY `idx_tag` (`tag_name`) COMMENT '按标签查询加速',
  KEY `uid_name_weight` (`uid`,`tag_name`,`weight` DESC) COMMENT 'uid&tag_name&weight联合索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户语义标签表（用于用户画像）';

