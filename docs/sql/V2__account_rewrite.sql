-- =============================================================================
-- 账号体系重做：数据迁移（MySQL 8 / video 库）
-- 执行：mysql --host=127.0.0.1 --port=3306 --user=root --default-character-set=utf8mb4 < docs/sql/V2__account_rewrite.sql
-- 注意：本脚本非幂等（索引/列已存在时会报错），重复执行前请先人工确认。
-- =============================================================================

-- 0) 备份（保留迁移前的 user 表快照）
CREATE TABLE IF NOT EXISTS video.user_bak_20261122 AS SELECT * FROM video.user;

-- 1) 昵称改为"仅展示"，去掉冗余的唯一约束（唯一性由 uid / username 保证）
ALTER TABLE video.user DROP INDEX nickname;

-- 2) 记录密码最后修改时间，便于审计与后续强制重置
ALTER TABLE video.user ADD COLUMN password_updated_at datetime NULL AFTER password;

-- 3) 历史用户：把系统生成的数字码用户名换成注册时填的昵称
--    只迁移满足用户名规则的昵称（2-20 字符、非纯数字、不含空白）；
--    纯数字/超长昵称保留原数字码，登录时仍可走 uid 兼容查询。
UPDATE video.user
   SET username = nickname
 WHERE username = CAST(uid AS CHAR)
   AND CHAR_LENGTH(nickname) BETWEEN 2 AND 20
   AND nickname NOT REGEXP '^[0-9]+$'
   AND nickname NOT REGEXP '[[:space:]]';

-- 4) 校验
SELECT
    (SELECT COUNT(*) FROM video.user)                                                   AS total,
    (SELECT COUNT(*) FROM video.user WHERE username = CAST(uid AS CHAR))                AS still_code,
    (SELECT COUNT(*) FROM video.user WHERE username = nickname)                         AS username_is_nickname;
