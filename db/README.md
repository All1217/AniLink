# 数据库初始化与分库

AniLink 的 MySQL 方案分两层：

| 位置 | 用途 | 状态 |
| --- | --- | --- |
| `db/schema.sql` | **分库前基线**：`video` 单库 16 张业务表 + `al_remark` / `al_communication` 空库 | 保留，作为历史初始化与迁移对照 |
| `db/schema/` | **Phase 5 分库后目标 schema**：按服务拆成 7 个业务库 + `al_remark`，每库一个文件 | 新环境按此建库建表 |
| `db/migrations/phase5/` | **现有库的迁移脚本与 runbook**：同实例 `RENAME TABLE` 原地分库，带快照与回滚 | 待 lead/用户在停机窗口执行，不自动跑 |

## 1. 目标库与表归属（16 张业务表）

| 库 | 表 | 使用方 | 端口 |
| --- | --- | --- | --- |
| `al_user` | `user`、`user_stats`、`user_tag` | AL-user | 8025 |
| `al_video` | `video`、`video_stats`、`user_video`、`category` | AL-video | 8026 |
| `al_playback` | `bh_video` | AL-playback | 8027 |
| `al_comment` | `comment` | AL-comment | 8028 |
| `al_danmaku` | `danmu`、`sub_danmu` | AL-danmaku | 8029 / WS 8030 |
| `al_social` | `follow`、`favorite`、`favorite_video` | AL-social | 8031 |
| `al_communication` | `chat`、`chat_message` | AL-communication | 8023 / WS 8024 |
| `al_remark` | 0 张（点赞走 Redis + MQ） | AL-remark | 8021 |
| — | 不建业务库 | AL-admin（内部契约）、AL-datacenter（BFF 无自有表） | 8040 / 8032 |

> 每个服务只允许访问自己的库/表；跨域数据一律走 `/internal/v1/**` 内部契约（Feign），禁止跨库 JOIN。
> 分库后各服务 `application.yml` 的 `spring.datasource.url` 已指向自己的库；账号/口令仍由 Nacos
> `shared-mysql.yaml` 注入，本地不写口令。

## 2. 新环境：按分库 schema 初始化

```powershell
# 在仓库根目录，逐个库建库建表（脚本内已含 CREATE DATABASE IF NOT EXISTS）
mysql -u root -p < db/schema/al_user.sql
mysql -u root -p < db/schema/al_video.sql
mysql -u root -p < db/schema/al_playback.sql
mysql -u root -p < db/schema/al_comment.sql
mysql -u root -p < db/schema/al_danmaku.sql
mysql -u root -p < db/schema/al_social.sql
mysql -u root -p < db/schema/al_communication.sql
mysql -u root -p < db/schema/al_remark.sql
```

特性与旧 `schema.sql` 一致：只 `CREATE DATABASE/TABLE IF NOT EXISTS`，不 `DROP`、不清数据、不含业务种子数据；
管理员账号需自行注册后调整 `user.role`。

## 3. 已有库：Phase 5 分库迁移（不自动执行）

现有环境（表都在 `video` 单库）用迁移脚本原地分库，**不复制数据**：

| 文件 | 内容 |
| --- | --- |
| [migrations/phase5/README.md](migrations/phase5/README.md) | **runbook**：停机窗口、执行顺序、验证 SQL、回滚步骤、遗留事项 |
| [migrations/phase5/00-snapshot.md](migrations/phase5/00-snapshot.md) | `mysqldump` 全库快照、校验、恢复演练、行数基线 |
| [migrations/phase5/01-create-databases.sql](migrations/phase5/01-create-databases.sql) | 创建 8 个目标库（7 业务库 + `al_remark`） |
| [migrations/phase5/02-rename-tables.sql](migrations/phase5/02-rename-tables.sql) | 单条原子 `RENAME TABLE` 把 16 张表改名到对应库 |
| [migrations/phase5/03-grants.sql](migrations/phase5/03-grants.sql) | 最小权限账号模板（口令占位符，执行前必须替换） |
| [migrations/phase5/99-rollback.sql](migrations/phase5/99-rollback.sql) | 反向 `RENAME TABLE` 回滚到 `video` 单库 |

执行顺序一句话：**停写 → 全库快照 → 建库 → 改名 → 验证 → （可选）建账号 → 发布配置 → 启动冒烟**；
出现异常按 runbook「回滚步骤」先 `99-rollback.sql` 反向改名，再回滚配置，最后用快照兜底。
本仓库**只提供脚本，不连接数据库执行任何 DDL/DML**，由 lead/用户人工执行。

## 4. 旧视频库表清单（`db/schema.sql` 基线，分库前）

| 表 | 用途 | 分库后归属 |
| --- | --- | --- |
| `user` | 用户、角色、状态 | `al_user` |
| `user_stats` | 粉丝/关注/获赞等统计 | `al_user` |
| `user_tag` | 用户画像语义标签 | `al_user` |
| `video` | 视频稿件与转码/审核状态 | `al_video` |
| `video_stats` | 播放/弹幕/点赞等统计 | `al_video` |
| `user_video` | 用户-视频互动 | `al_video` |
| `category` | 主/子分区 | `al_video` |
| `bh_video` | 浏览历史 | `al_playback` |
| `comment` | 评论 | `al_comment` |
| `danmu` | 弹幕 | `al_danmaku` |
| `sub_danmu` | 弹幕测试表（代码当前无引用） | `al_danmaku` |
| `follow` | 关注关系 | `al_social` |
| `favorite` | 收藏夹 | `al_social` |
| `favorite_video` | 收藏夹-视频关系 | `al_social` |
| `chat` | 聊天会话 | `al_communication` |
| `chat_message` | 聊天消息 | `al_communication` |

## 5. 账号与权限

- 分库前：所有服务共用 Nacos `shared-mysql.yaml` 里的一个账号（对 `video`/`al_remark`/`al_communication` 有权限）。
- 分库后：目标是最小权限账号（每个服务只读写自己的库，无 DDL、无跨库授权），模板见
  [migrations/phase5/03-grants.sql](migrations/phase5/03-grants.sql)，口令一律使用占位符，**不要提交真实口令**。
- 只读账号 `al_engine_ro` 供画像引擎/数据核对使用；引擎的跨库读取改造见 runbook「遗留事项」。
- 库账号/口令只放在 Nacos 共享配置或环境变量里，不写进仓库、日志、截图。

## 6. 维护

- 新环境初始化以 `db/schema/` 为准；`db/schema.sql` 只作分库前基线与迁移对照，不再追加新表。
- 已有库的结构变更继续放 `db/migrations/` 增量脚本（命名 `YYYY-MM-DD-简述.sql`），执行前备份。
- 分库迁移完成后若有新表，按归属加到对应 `db/schema/al_*.sql` 的增量脚本里，保持文档与结构一致。
- 旧文件 `AniLink-AlgorithmEngine/Assets/sql.sql` 只覆盖部分早期表，已不是初始化来源；保留用于历史参考。
- 旧库 `video` 在观察期（≥7 天）且确认不回滚后再清理；`video.user_bak_20261122` 是开发库备份表，代码无引用，未纳入 schema。
