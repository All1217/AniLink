-- =====================================================================
-- Phase 5 分库 · 01 创建目标库
-- 作用：在同一个 MySQL 实例上创建 7 个按服务划分的业务库。
-- 前提：MySQL 8.0.33，utf8mb4 / utf8mb4_0900_ai_ci（与现有 video 库一致）。
-- 说明：
--   1. 只建库，不建表；表由 02-rename-tables.sql 从 video 库改名迁入。
--   2. al_remark 是 Phase 5 之前就存在的库，本脚本一并幂等创建，便于新环境一次到位。
--   3. AL-admin 走内部契约，不建 al_admin 业务库；AL-datacenter 无自有表，也不建库。
--   4. 本脚本可重复执行（IF NOT EXISTS），不影响已有库。
-- 执行：mysql -h <mysql-host> -P 3306 -u <admin-user> -p --default-character-set=utf8mb4 < 01-create-databases.sql
-- 注意：不要在本脚本里 DROP/CREATE 覆盖已有库，避免误删数据。
-- =====================================================================

-- 用户域：user / user_stats / user_tag（AL-user）
CREATE DATABASE IF NOT EXISTS `al_user`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 视频域：video / video_stats / user_video / category（AL-video）
CREATE DATABASE IF NOT EXISTS `al_video`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 播放历史域：bh_video（AL-playback）
CREATE DATABASE IF NOT EXISTS `al_playback`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 评论域：comment（AL-comment）
CREATE DATABASE IF NOT EXISTS `al_comment`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 弹幕域：danmu / sub_danmu（AL-danmaku）
CREATE DATABASE IF NOT EXISTS `al_danmaku`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 社交域：follow / favorite / favorite_video（AL-social）
CREATE DATABASE IF NOT EXISTS `al_social`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 聊天域：chat / chat_message（AL-communication）
CREATE DATABASE IF NOT EXISTS `al_communication`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 点赞域（Phase 5 之前已独立，当前无业务表，保持空库）：AL-remark
CREATE DATABASE IF NOT EXISTS `al_remark`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 执行后自检：应看到上面 8 个库（不包含 video / al_admin）
-- SHOW DATABASES LIKE 'al\_%';
