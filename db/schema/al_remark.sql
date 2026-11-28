-- =====================================================================
-- AniLink Phase 5 分库 schema · al_remark
-- 使用方：AL-remark（8021，点赞走 Redis + MQ）
-- 表：0 张。当前服务不使用 MySQL 表；建空库仅为满足 datasource 启动。
-- 来源：db/schema.sql（分库前基线）
-- 用法：mysql -u root -p < db/schema/al_remark.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `al_remark`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE `al_remark`;

