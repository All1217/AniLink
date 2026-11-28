-- =====================================================================
-- Phase 5 分库 · 99 回滚（反向 RENAME TABLE）
--
-- 适用场景：02-rename-tables.sql 已执行后需要回到「所有业务表都在 video 库」的状态。
-- 方式：把 16 张表原样改回 video 库，数据不复制、不丢失；
--       同时需要把各服务 application.yml 的数据源 url 回滚到 video 库并重启服务
--       （回滚顺序见 README.md「回滚步骤」）。
--
-- 前提（必须全部满足）：
--   1. 业务停机窗口内：11 个 Java 服务 + Python 引擎已停止，没有会话连接新库；
--   2. 旧库 video 仍存在（本方案不删 video 库，改名后它是空库，仍可承接改名）；
--   3. 目标库中的表名与下面一致（若迁移后又做了改表/重建，先评估再回滚）；
--   4. 不接受数据损失的场景，回滚前再做一次全库快照（同 00-snapshot.md）。
--
-- 执行：mysql -h <mysql-host> -P 3306 -u <admin-user> -p --default-character-set=utf8mb4 < 99-rollback.sql
-- 校验：执行末尾的只读 SQL，确认 video 库重新有 16 张业务表、各 al_* 库为空。
-- 兜底：若表已被误改/误删，反向改名无效，改用 00-snapshot.md 的全库快照恢复。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0. 回滚前自检（只读）
--    期望：16 张表都在对应的 al_* 库；video 库存在。
-- ---------------------------------------------------------------------
SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema IN (
  'al_user', 'al_video', 'al_playback', 'al_comment',
  'al_danmaku', 'al_social', 'al_communication'
)
ORDER BY table_schema, table_name;
-- 期望 16 行；缺表说明迁移后有变更，先停下核对，不要盲目改名。

SHOW DATABASES LIKE 'video';
-- 期望 1 行；旧库被删则反向改名无法进行，只能走快照恢复。

-- ---------------------------------------------------------------------
-- 1. 原子反向改名：16 张表全部改回 video 库
-- ---------------------------------------------------------------------
RENAME TABLE
  `al_user`.`user`                 TO `video`.`user`,
  `al_user`.`user_stats`           TO `video`.`user_stats`,
  `al_user`.`user_tag`             TO `video`.`user_tag`,

  `al_video`.`video`               TO `video`.`video`,
  `al_video`.`video_stats`         TO `video`.`video_stats`,
  `al_video`.`user_video`          TO `video`.`user_video`,
  `al_video`.`category`            TO `video`.`category`,

  `al_playback`.`bh_video`         TO `video`.`bh_video`,

  `al_comment`.`comment`           TO `video`.`comment`,

  `al_danmaku`.`danmu`             TO `video`.`danmu`,
  `al_danmaku`.`sub_danmu`         TO `video`.`sub_danmu`,

  `al_social`.`follow`             TO `video`.`follow`,
  `al_social`.`favorite`           TO `video`.`favorite`,
  `al_social`.`favorite_video`     TO `video`.`favorite_video`,

  `al_communication`.`chat`        TO `video`.`chat`,
  `al_communication`.`chat_message` TO `video`.`chat_message`;

-- ---------------------------------------------------------------------
-- 2. 回滚后自检（只读）
-- ---------------------------------------------------------------------
-- 2.1 video 库应重新有 16 张业务表（如开发库仍有 user_bak_20261122，则共 17 张）
SELECT COUNT(*) AS video_table_count
FROM information_schema.tables
WHERE table_schema = 'video'
  AND table_name IN (
    'user', 'user_stats', 'user_tag',
    'video', 'video_stats', 'user_video', 'category',
    'bh_video',
    'comment',
    'danmu', 'sub_danmu',
    'follow', 'favorite', 'favorite_video',
    'chat', 'chat_message'
  );
-- 期望 16

-- 2.2 各 al_* 库应为空（al_remark 除外）
SELECT table_schema, COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema IN (
  'al_user', 'al_video', 'al_playback', 'al_comment',
  'al_danmaku', 'al_social', 'al_communication'
)
GROUP BY table_schema
ORDER BY table_schema;
-- 期望无行（或全为 0）；目标库本身可保留，供下次迁移复用，不必删库。

-- ---------------------------------------------------------------------
-- 3. 账号回滚（仅当 03-grants.sql 已执行且需要撤销时，逐个子确认后执行）
-- ---------------------------------------------------------------------
-- DROP USER IF EXISTS 'al_user_app'@'%';
-- DROP USER IF EXISTS 'al_video_app'@'%';
-- DROP USER IF EXISTS 'al_playback_app'@'%';
-- DROP USER IF EXISTS 'al_comment_app'@'%';
-- DROP USER IF EXISTS 'al_danmaku_app'@'%';
-- DROP USER IF EXISTS 'al_social_app'@'%';
-- DROP USER IF EXISTS 'al_communication_app'@'%';
-- DROP USER IF EXISTS 'al_engine_ro'@'%';
-- 注意：'al_remark_app' 是既有库 al_remark 的账号，只在确认无人使用时才删除。
