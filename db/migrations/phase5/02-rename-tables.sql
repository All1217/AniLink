-- =====================================================================
-- Phase 5 分库 · 02 表归属迁移（同实例 RENAME TABLE）
--
-- 作用：把 video 库中的 16 张业务表原地改名到各服务库。
--       RENAME TABLE 只改元数据，不复制数据、不重建表，秒级完成；
--       单条语句内多表改名在 InnoDB 下是原子的，全部成功或全部不生效。
--
-- 前提（必须全部满足，否则先停下）：
--   1. 已执行 01-create-databases.sql，8 个目标库存在；
--   2. 已完成 00-snapshot.md 的全库快照并校验通过；
--   3. 业务停机窗口内：11 个 Java 服务 + Python 画像引擎已停止，
--      无任何会话写入/读取 video 库（否则会因表被改名而报 Table doesn't exist）；
--   4. 没有指向这些表的视图/触发器/外键约束（本项目 schema 无外键，见 db/schema.sql）。
--
-- 执行：mysql -h <mysql-host> -P 3306 -u <admin-user> -p --default-character-set=utf8mb4 < 02-rename-tables.sql
-- 回滚：执行 99-rollback.sql（反向改名），不要手工单表移动。
--
-- 表归属（16 张，与 db/schema.sql 一致）：
--   al_user        : user, user_stats, user_tag                     （3）
--   al_video       : video, video_stats, user_video, category       （4）
--   al_playback    : bh_video                                       （1）
--   al_comment     : comment                                        （1）
--   al_danmaku     : danmu, sub_danmu                               （2）
--   al_social      : follow, favorite, favorite_video               （3）
--   al_communication: chat, chat_message                            （2）
-- 不迁移：video.user_bak_20261122（开发库备份表，代码无引用，留在旧库）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0. 迁移前自检（只读，执行后人工确认输出）
--    期望：列出 16 张待迁移表；若有缺失或多出来的表，先停下核对。
-- ---------------------------------------------------------------------
SELECT table_name
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
  )
ORDER BY table_name;
-- 期望 16 行。行数不符时不要继续执行下面的 RENAME。

-- ---------------------------------------------------------------------
-- 1. 原子改名：一次 RENAME 完成 16 张表的跨库迁移
--    注意：反引号必须保留；库名不存在、表名拼错都会导致整条语句失败且不生效。
-- ---------------------------------------------------------------------
RENAME TABLE
  `video`.`user`                 TO `al_user`.`user`,
  `video`.`user_stats`           TO `al_user`.`user_stats`,
  `video`.`user_tag`             TO `al_user`.`user_tag`,

  `video`.`video`                TO `al_video`.`video`,
  `video`.`video_stats`          TO `al_video`.`video_stats`,
  `video`.`user_video`           TO `al_video`.`user_video`,
  `video`.`category`             TO `al_video`.`category`,

  `video`.`bh_video`             TO `al_playback`.`bh_video`,

  `video`.`comment`              TO `al_comment`.`comment`,

  `video`.`danmu`                TO `al_danmaku`.`danmu`,
  `video`.`sub_danmu`            TO `al_danmaku`.`sub_danmu`,

  `video`.`follow`               TO `al_social`.`follow`,
  `video`.`favorite`             TO `al_social`.`favorite`,
  `video`.`favorite_video`       TO `al_social`.`favorite_video`,

  `video`.`chat`                 TO `al_communication`.`chat`,
  `video`.`chat_message`         TO `al_communication`.`chat_message`;

-- ---------------------------------------------------------------------
-- 2. 迁移后自检（只读）
-- ---------------------------------------------------------------------
-- 2.1 每个库的表数量与表名；期望 al_user=3, al_video=4, al_playback=1,
--     al_comment=1, al_danmaku=2, al_social=3, al_communication=2，
--     旧库 video 只剩 user_bak_20261122（0 张业务表）
SELECT table_schema, COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema IN (
  'video', 'al_user', 'al_video', 'al_playback', 'al_comment',
  'al_danmaku', 'al_social', 'al_communication'
)
GROUP BY table_schema
ORDER BY table_schema;

SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema IN (
  'al_user', 'al_video', 'al_playback', 'al_comment',
  'al_danmaku', 'al_social', 'al_communication'
)
ORDER BY table_schema, table_name;

-- 2.2 行数与快照前基线比对（可选：先用 00-snapshot.md 第 4 节记录基线）
-- 说明：information_schema.table_rows 是 InnoDB 估算值，精确行数用 COUNT(*)。
-- SELECT COUNT(*) FROM al_user.`user`;
-- SELECT COUNT(*) FROM al_video.`video`;
-- SELECT COUNT(*) FROM al_playback.`bh_video`;
-- SELECT COUNT(*) FROM al_comment.`comment`;
-- SELECT COUNT(*) FROM al_danmaku.`danmu`;
-- SELECT COUNT(*) FROM al_social.`follow`;
-- SELECT COUNT(*) FROM al_communication.`chat_message`;
