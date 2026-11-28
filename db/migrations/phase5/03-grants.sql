-- =====================================================================
-- Phase 5 分库 · 03 最小权限账号模板
--
-- 作用：为 7 个业务库 + al_remark 准备「只够本服务用」的账号模板。
--       最终目标：每个服务账号只拥有自己库的 SELECT/INSERT/UPDATE/DELETE，
--       不授予 DDL（建表/改表）、不授予其他库权限、不授予 GRANT OPTION。
--
-- 重要：
--   1. 本文件是模板，含 `<CHANGE_ME_...>` 占位口令，**执行前必须替换**；
--      不要把真实口令写回本文件、提交 git 或贴进文档。
--   2. 执行前先确认账号策略：当前各服务的数据源账号/口令来自 Nacos 共享配置
--      `shared-mysql.yaml`（所有服务共用一份），没有 per-service 账号机制。
--      因此分账号需要额外的 Nacos 配置改造（见 db/README.md「账号与 Nacos」），
--      本文件的模板用于「已决定按库分账号」的环境；过渡期可只执行第 3 节的只读账号。
--   3. 主机名 `%` 仅示例，生产请改成应用服务器 IP/CIDR（如 '10.0.0.%'）。
--   4. AL-admin 走内部契约，不建业务库、不需要业务库账号；
--      AL-datacenter 无自有表、无数据源。
--
-- 执行：mysql -h <mysql-host> -P 3306 -u <admin-user> -p --default-character-set=utf8mb4 < 03-grants.sql
-- 说明：MySQL 8 的 CREATE USER / GRANT 立即生效，不需要 FLUSH PRIVILEGES。
-- 回滚：DROP USER IF EXISTS '<account>'@'<host>';（仅在账号切换失败时使用，见 README）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 各服务读写账号（只授予自有库的 DML；表已由 02 改名到位）
-- ---------------------------------------------------------------------
CREATE USER IF NOT EXISTS 'al_user_app'@'%'          IDENTIFIED BY '<CHANGE_ME_AL_USER_APP_PWD>';
CREATE USER IF NOT EXISTS 'al_video_app'@'%'         IDENTIFIED BY '<CHANGE_ME_AL_VIDEO_APP_PWD>';
CREATE USER IF NOT EXISTS 'al_playback_app'@'%'      IDENTIFIED BY '<CHANGE_ME_AL_PLAYBACK_APP_PWD>';
CREATE USER IF NOT EXISTS 'al_comment_app'@'%'       IDENTIFIED BY '<CHANGE_ME_AL_COMMENT_APP_PWD>';
CREATE USER IF NOT EXISTS 'al_danmaku_app'@'%'       IDENTIFIED BY '<CHANGE_ME_AL_DANMAKU_APP_PWD>';
CREATE USER IF NOT EXISTS 'al_social_app'@'%'        IDENTIFIED BY '<CHANGE_ME_AL_SOCIAL_APP_PWD>';
CREATE USER IF NOT EXISTS 'al_communication_app'@'%' IDENTIFIED BY '<CHANGE_ME_AL_COMMUNICATION_APP_PWD>';
CREATE USER IF NOT EXISTS 'al_remark_app'@'%'        IDENTIFIED BY '<CHANGE_ME_AL_REMARK_APP_PWD>';

-- 账号已存在时改口令（首次执行可跳过；重复执行模板时用它替换 CREATE 的 IF NOT EXISTS 分支）
-- ALTER USER 'al_user_app'@'%' IDENTIFIED BY '<CHANGE_ME_AL_USER_APP_PWD>';

GRANT SELECT, INSERT, UPDATE, DELETE ON `al_user`.*           TO 'al_user_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `al_video`.*          TO 'al_video_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `al_playback`.*       TO 'al_playback_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `al_comment`.*        TO 'al_comment_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `al_danmaku`.*        TO 'al_danmaku_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `al_social`.*         TO 'al_social_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `al_communication`.*  TO 'al_communication_app'@'%';
-- al_remark 当前无业务表，保留账号为将来做准备
GRANT SELECT, INSERT, UPDATE, DELETE ON `al_remark`.*         TO 'al_remark_app'@'%';

-- ---------------------------------------------------------------------
-- 2. 过渡期共享账号（可选）
--    如果暂不改造 Nacos 的 per-service 账号，就把现有 shared-mysql.yaml 账号
--    收紧为「只对 7 个新库 + al_remark 有 DML」，不授予 DDL、不授予其他库。
--    把 <shared-app-user> 换成实际账号名后再执行：
-- ---------------------------------------------------------------------
-- REVOKE ALL PRIVILEGES, GRANT OPTION FROM '<shared-app-user>'@'<shared-app-host>';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON `al_user`.*          TO '<shared-app-user>'@'<shared-app-host>';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON `al_video`.*         TO '<shared-app-user>'@'<shared-app-host>';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON `al_playback`.*      TO '<shared-app-user>'@'<shared-app-host>';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON `al_comment`.*       TO '<shared-app-user>'@'<shared-app-host>';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON `al_danmaku`.*       TO '<shared-app-user>'@'<shared-app-host>';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON `al_social`.*        TO '<shared-app-user>'@'<shared-app-host>';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON `al_communication`.* TO '<shared-app-user>'@'<shared-app-host>';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON `al_remark`.*        TO '<shared-app-user>'@'<shared-app-host>';

-- ---------------------------------------------------------------------
-- 3. 只读账号（画像引擎 / 数据核对 / BI 用）
--    Python 画像引擎同时读 user_tag、user_video、video、danmu，跨 3 个库；
--    给它一个多库只读账号。注意：引擎当前代码用未加库名的表名（见
--    AniLink-AlgorithmEngine/src/util/database.py），单库连接跨库读需要
--    Phase 5-C 补「按表加库名 / 多数据源」改造，本账号只为改造后准备。
-- ---------------------------------------------------------------------
CREATE USER IF NOT EXISTS 'al_engine_ro'@'%' IDENTIFIED BY '<CHANGE_ME_AL_ENGINE_RO_PWD>';
GRANT SELECT ON `al_user`.*    TO 'al_engine_ro'@'%';
GRANT SELECT ON `al_video`.*   TO 'al_engine_ro'@'%';
GRANT SELECT ON `al_danmaku`.* TO 'al_engine_ro'@'%';
GRANT SELECT ON `al_social`.*  TO 'al_engine_ro'@'%';

-- ---------------------------------------------------------------------
-- 4. 权限自检（只读）
--    期望：每个 *_app 账号只出现在自己的库；al_engine_ro 出现 4 行只读。
-- ---------------------------------------------------------------------
SELECT user, host, db, GROUP_CONCAT(DISTINCT privilege_type ORDER BY privilege_type) AS privileges
FROM mysql.db
WHERE user LIKE 'al\_%'
GROUP BY user, host, db
ORDER BY user, db;
