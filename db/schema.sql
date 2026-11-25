-- AniLink 初始化 schema
-- 覆盖 MySQL 库：video、al_remark、al_communication
-- 来源：本机开发库 mysqldump --no-data（2026-11-25）
-- 排除：video.user_bak_20261122（备份表，无代码引用）
-- 说明：al_remark / al_communication 当前只使用 Redis/内存，建空库以兼容服务 datasource
-- 用法：mysql -u root -p < db/schema.sql
-- 注意：CREATE TABLE IF NOT EXISTS 只保证空库初始化；已有表不会被迁移

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `video` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `video`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `chat` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '唯一主键',
  `target_id` bigint unsigned NOT NULL COMMENT '对象UID',
  `sender_id` bigint unsigned NOT NULL COMMENT '用户UID',
  `latest_time` datetime NOT NULL COMMENT '最近接收消息的时间或最近打开聊天窗口的时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `from_to` (`target_id`,`sender_id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `follow` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '关注与被关注关系ID',
  `follower_id` bigint unsigned NOT NULL COMMENT '关注者ID',
  `followee_id` bigint unsigned NOT NULL COMMENT '被关注者ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `er_ee__index` (`follower_id`,`followee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='关注与被关注关系表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `al_remark` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `al_remark`;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `al_communication` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `al_communication`;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

