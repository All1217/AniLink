# Phase 5 数据库分库 · 迁移 Runbook

本目录是 Phase 5「按服务分库」的迁移脚本与操作手册。**脚本只在这里产出，不自动执行**：
由 lead / 用户在停机窗口内按本文顺序人工执行，执行人负责备份、校验与回滚决策。

- 方案：**同一个 MySQL 实例内用 `RENAME TABLE` 把表原地改名**到各服务库，不复制数据、不重建表、不换实例。
- 回滚：`99-rollback.sql` 反向改名（秒级、无损）；全库快照（`00-snapshot.md`）作为兜底。
- 不包含：删旧库、删旧账号、Python 画像引擎跨库改造（见「遗留事项」）。

## 1. 目标库与表归属（16 张业务表）

| 目标库 | 表 | 使用方 | 服务端口 |
| --- | --- | --- | --- |
| `al_user` | `user`、`user_stats`、`user_tag` | AL-user | 8025 |
| `al_video` | `video`、`video_stats`、`user_video`、`category` | AL-video | 8026 |
| `al_playback` | `bh_video` | AL-playback | 8027 |
| `al_comment` | `comment` | AL-comment | 8028 |
| `al_danmaku` | `danmu`、`sub_danmu` | AL-danmaku | 8029 / WS 8030 |
| `al_social` | `follow`、`favorite`、`favorite_video` | AL-social | 8031 |
| `al_communication` | `chat`、`chat_message` | AL-communication | 8023 / WS 8024 |
| `al_remark` | （无业务表，保持空库） | AL-remark | 8021 |
| 不建库 | — | AL-admin（走内部契约）、AL-datacenter（BFF 无自有表） | 8040 / 8032 |

说明：

- `category`（主/子分区）技术上是视频域的字典表，归 `al_video`；当前 Java 代码未直接查询它，保留随迁。
- `sub_danmu` 是弹幕测试表，代码当前无引用，按服务归属一并迁到 `al_danmaku`。
- `video.user_bak_20261122` 是开发库备份表，代码无引用，**不迁移**，留在旧库 `video`（旧库在本方案中不删除，改名后为空壳）。
- 表数总计 16，与 `db/schema.sql`（分库前基线）一致；分库后建表脚本见 `db/schema/`。

## 2. 停机窗口与前置条件

预计窗口：**20–40 分钟**（改名本身秒级，时间主要花在停服、快照、改配置、重启与冒烟）。

前置检查（缺一不可）：

- [ ] MySQL 8.0.33 实例可达；有管理员账号，权限满足：`CREATE DATABASE`、`ALTER/DROP/CREATE/INSERT`（`RENAME TABLE` 需要源表 `ALTER, DROP` 与目标库 `CREATE, INSERT`）、`CREATE USER`/`GRANT`（若执行 03）。
- [ ] 磁盘：快照目录可用空间 ≥ 当前数据量 2 倍。
- [ ] 已确认**所有**写入方清单：11 个 Java 服务（AL-gateway/AL-user/AL-video/AL-playback/AL-comment/AL-danmaku/AL-social/AL-datacenter/AL-admin/AL-remark/AL-communication）＋ `AniLink-AlgorithmEngine`（Python，会写 `user_tag`）＋ 任何本地 MCP / Navicat 会话。
- [ ] 已通知使用方停机；无定时任务/压测在跑。
- [ ] 已阅读 `db/schema.sql` 与 `db/schema/`，确认表结构无外键（无 `FOREIGN KEY`，改名不受约束阻挡）。
- [ ] 数据库变更走审批：本仓库仅提供脚本，**不连库执行**。

## 3. 执行顺序

| 步骤 | 动作 | 是否可回滚 | 执行物 |
| --- | --- | --- | --- |
| ① | 停写：按【网关 → 业务服务 → 引擎】顺序停 11 个服务 + Python 引擎 | 重启即恢复 | — |
| ② | 全库快照 + 校验 + 基线行数留档 | 保留即可 | [00-snapshot.md](00-snapshot.md) |
| ③ | 建 8 个目标库 | 可重复执行 | [01-create-databases.sql](01-create-databases.sql) |
| ④ | 表改名（16 张） | [99-rollback.sql](99-rollback.sql) | [02-rename-tables.sql](02-rename-tables.sql) |
| ⑤ | 验证表/行数（第 5 节 SQL） | 改回即可 | — |
| ⑥ | 建最小权限账号（按需） | DROP USER | [03-grants.sql](03-grants.sql) |
| ⑦ | 发布配置：各服务 `application.yml` 数据源 url 已改到对应库（本次提交内容） | 改回 `video` 即回滚 | 各服务 `application.yml` |
| ⑧ | 启动服务并按第 5.3 节冒烟 | — | — |
| ⑨ | 观察期（建议 ≥ 7 天）后清理旧库/旧账号 | — | 见「遗留事项」 |

### 3.1 停机与快照

```powershell
# 顺序：先停网关断流量，再停业务服务，最后停 Python 引擎（避免引擎继续写 user_tag）
# 使用你们实际的启动方式（IDEA / java -jar / mvn spring-boot:run）
# 示例：按端口确认已无监听
foreach ($p in 8090,8025,8026,8027,8028,8029,8030,8031,8032,8040,8021,8023,8024) {
  Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue |
    Select-Object LocalPort, OwningProcess
}
# 期望：无输出（全部已停）
```

然后严格按 [00-snapshot.md](00-snapshot.md) 完成快照、校验和基线记录，再继续。

### 3.2 建库、改名

```powershell
# 全部使用 mysqldump/mysql 客户端；PowerShell 下输入重定向 `-p` 会交互提示口令
mysql -h <mysql-host> -P 3306 -u <admin-user> -p --default-character-set=utf8mb4 `
  -e "source db/migrations/phase5/01-create-databases.sql"

mysql -h <mysql-host> -P 3306 -u <admin-user> -p --default-character-set=utf8mb4 `
  -e "source db/migrations/phase5/02-rename-tables.sql"

# 或（在仓库根目录、cmd 环境）：
# cmd /c "mysql -h <mysql-host> -P 3306 -u <admin-user> -p --default-character-set=utf8mb4 < db\migrations\phase5\01-create-databases.sql"
```

`02-rename-tables.sql` 是**一条原子 `RENAME TABLE`**：16 张表要么全部改成，要么全部不改；
如果报 `Unknown database` / `Table 'video.xxx' doesn't exist`，说明建库或前提没满足，修正后重跑即可（该语句失败不产生部分迁移）。

### 3.3 建账号（可选，按第 6 节决策）

```powershell
# 先替换 03-grants.sql 里所有 <CHANGE_ME_...> 占位口令，再执行；不要把真实口令写回仓库
mysql -h <mysql-host> -P 3306 -u <admin-user> -p --default-character-set=utf8mb4 `
  -e "source db/migrations/phase5/03-grants.sql"
```

### 3.4 发布配置并启动

本次提交已把 7 个服务的数据源 url 改到对应库（数据库口令仍由 Nacos `shared-mysql.yaml` 注入，本地 yml 不写口令）。
启动顺序与日常一致：Nacos/MySQL/Redis/RabbitMQ/MinIO → 网关 → AL-user → AL-video → AL-playback →
AL-comment → AL-danmaku → AL-social → AL-datacenter → AL-admin → AL-remark → AL-communication → Python 引擎。

> `AL-admin` 已改为内部契约，不再配置业务库数据源；`AL-datacenter` 本来就没有数据源。
> `AL-communication` 自本条起连 `al_communication`（`chat`/`chat_message` 已改名过去）。

## 4. 配置对照表（分库后）

| 服务 | `spring.datasource.url` 的库 | 说明 |
| --- | --- | --- |
| AL-user | `al_user` | 账号/资料/标签 |
| AL-video | `al_video` | 稿件/统计/互动/分区 |
| AL-playback | `al_playback` | 观看历史 |
| AL-comment | `al_comment` | 评论 |
| AL-danmaku | `al_danmaku` | 弹幕 |
| AL-social | `al_social` | 关注/收藏 |
| AL-communication | `al_communication` | 聊天会话与消息 |
| AL-remark | `al_remark` | 不变（无业务表） |
| AL-admin | 无数据源 | 走 AL-user/AL-video 管理契约 |
| AL-datacenter | 无数据源 | BFF，聚合走 Feign |

账号/口令仍在 Nacos 的 `shared-mysql.yaml`，本地 `application.yml` 只写 `url`。
如果把 Nacos 账号切换成 03 模板里的 per-service 账号，需要给每个服务单独覆盖 `spring.datasource.username/password`
（当前共享配置是“一份账号全部服务共用”，这是额外的配置改造，建议放到验收后单独做）。

## 5. 验证 SQL 与冒烟

### 5.1 表归属与数量（只读）

```sql
-- 期望：al_user=3, al_video=4, al_playback=1, al_comment=1,
--       al_danmaku=2, al_social=3, al_communication=2，合计 16
SELECT table_schema, COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema IN (
  'al_user','al_video','al_playback','al_comment','al_danmaku','al_social','al_communication'
)
GROUP BY table_schema ORDER BY table_schema;

-- 期望：video 库只剩 user_bak_20261122（0 张业务表）
SELECT table_schema, COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema = 'video'
GROUP BY table_schema;
```

### 5.2 行数与基线比对（只读）

用 `00-snapshot.md` 第 4 节记录的基线逐项对比。建议精确核对「业务上最不能丢」的表：

```sql
SELECT 'al_user.user' AS tbl, COUNT(*) FROM al_user.`user`
UNION ALL SELECT 'al_user.user_stats', COUNT(*) FROM al_user.`user_stats`
UNION ALL SELECT 'al_video.video', COUNT(*) FROM al_video.`video`
UNION ALL SELECT 'al_video.user_video', COUNT(*) FROM al_video.`user_video`
UNION ALL SELECT 'al_playback.bh_video', COUNT(*) FROM al_playback.`bh_video`
UNION ALL SELECT 'al_comment.comment', COUNT(*) FROM al_comment.`comment`
UNION ALL SELECT 'al_danmaku.danmu', COUNT(*) FROM al_danmaku.`danmu`
UNION ALL SELECT 'al_social.follow', COUNT(*) FROM al_social.`follow`
UNION ALL SELECT 'al_social.favorite', COUNT(*) FROM al_social.`favorite`
UNION ALL SELECT 'al_communication.chat_message', COUNT(*) FROM al_communication.`chat_message`;
```

判定：与迁移前基线一致（迁移期间已停写，应完全相等；若不等，先停下来查原因，不要继续启动服务）。

### 5.3 服务连接库核对

服务启动后，在 MySQL 侧确认每个数据源真的连到新库：

```sql
-- 期望：al_user/al_video/al_playback/al_comment/al_danmaku/al_social/al_communication/al_remark
--       各出现相应用户的连接；不应再看到任何连接停留在 video 库
SELECT db, user, COUNT(*) AS conns
FROM information_schema.processlist
GROUP BY db, user
ORDER BY db;
```

### 5.4 启动后冒烟（最小集）

按网关 8090 走一遍，确认配置解析与读写链路：

- [ ] 登录 / 刷新 / 登出（AL-user → `al_user`）
- [ ] 首页视频列表 + 单个视频详情（AL-video → `al_video`）
- [ ] 上报播放进度 + 历史列表（AL-playback → `al_playback`）
- [ ] 发评论 / 评论列表（AL-comment → `al_comment`）
- [ ] 弹幕列表 + 发送弹幕（AL-danmaku → `al_danmaku`）
- [ ] 关注 / 收藏 / 收藏夹列表（AL-social → `al_social`）
- [ ] 私信会话 + 发送消息（AL-communication → `al_communication`）
- [ ] 管理端登录 + 用户分页 + 视频审核（AL-admin，走契约，无业务库）
- [ ] 数据中心统计/推荐（AL-datacenter，走 Feign）
- [ ] 画像引擎启动、RabbitMQ 消费、`user_tag` 写入（**见遗留事项，可能仍是旧库逻辑**）

## 6. 账号与 Nacos 决策（执行前确认）

现状：所有服务共用 Nacos `shared-mysql.yaml` 里的同一个数据库账号/口令，没有 per-service 账号。

两个选项：

1. **推荐（低风险）**：迁移当下不动账号，只保证该共享账号对 7 个新库有 DML；
   验收后再按 `03-grants.sql` 逐个服务切账号（每服务需要独立覆盖 `spring.datasource.username/password`，属配置改造）。
2. **本次一起切**：执行 `03-grants.sql`，并在 Nacos 为每个服务加独立数据源账号配置（需要改 Nacos，非本仓库脚本能完成）。

不论选哪个，都**不要**授予任一服务账号 DDL，也不要跨库授权（`02` 迁移期间用的管理员账号除外）。

## 7. 回滚步骤

### 7.1 触发条件

出现下列任一情况即回滚，不要“边跑边修”：

- `02` 改名前发现快照不可用 / 基线对不上；
- 改名后行数不等于基线；
- 服务启动持续失败且 10 分钟内定位不到配置以外的问题；
- 冒烟发现核心链路（登录/视频/历史/评论/弹幕/收藏/私信）任何一条不可用且无法快速修复。

### 7.2 回滚操作（预计 < 15 分钟）

```powershell
# ① 停全部服务（含引擎），确认端口无监听（同 3.1）
# ② 反向改名：16 张表改回 video 库
mysql -h <mysql-host> -P 3306 -u <admin-user> -p --default-character-set=utf8mb4 `
  -e "source db/migrations/phase5/99-rollback.sql"

# ③ 回滚配置：把本次提交的 7 个 application.yml 数据源 url 改回
#    jdbc:mysql://localhost:3306/video?...（AL-communication 也回 video）
#    若已发布到测试/生产环境，回退配置版本并重启
# ④ 启动服务，按 5.3/5.4 验证
```

- 账号不需要回滚也能用（旧库 `video` 仍在；如果第 6 节选了方案 2，确认共享账号对 video 仍有权限）。
- 目标库 `al_*` 可以保留（空库），下次迁移直接复用，不必删库。
- 若表已被误改/误删，反向改名无效：**停止一切操作**，用 `00-snapshot.md` 的全库快照恢复，再核对基线。

### 7.3 回滚演练建议

在预发/本地实例上先完整走一遍「快照 → 建库 → 改名 → 验证 → 99 回滚 → 再验证」，
确认 `99-rollback.sql` 输出为 16 行、各 `al_*` 库为空，再上真实环境。

## 8. 遗留事项（不在本次脚本范围）

1. **Python 画像引擎跨库读取**：`AniLink-AlgorithmEngine` 当前连单库、SQL 用未加库名的表名，同时读
   `user_tag`（al_user）、`user_video`/`video`（al_video）、`danmu`（al_danmaku）。
   分库后需要 Phase 5-C 改造（按表加库名 / 多数据源 / 只读视图），本次**不改引擎代码与配置**。
   在那之前，引擎的画像写入会失败，需要在 `02` 之后、启动引擎之前确认其影响或暂缓启动。
2. **旧库清理**：观察期（≥7 天）且确认不回滚后，再 `DROP DATABASE video`；清理前再导出一次快照。
   `video.user_bak_20261122` 是否保留由数据所有方决定。
3. **本地 MCP / Navicat 连接**：`mcp-server-database` 的 `.env`、各人本地连接串里若写死 `video` 库，
   分库后需要改成对应新库；这不属于服务配置，不在本次提交内。
4. **`AL-admin` 数据源**：已改为内部契约，无业务库；历史 `spring.datasource`/`mybatis-plus` 配置已删除。
5. **`AL-datacenter`**：无自有表，不需要数据源，本次无改动。
6. **迁移前跑 `@SpringBootTest` 用例**：配置已先行指向新库，但迁移未执行时，会创建 DataSource 的全上下文用例
   （如 `AL-video` 的 `MinioTest`）会报 `Unknown database 'al_video'`。这是**预期现象**，不是代码缺陷；
   过渡期可用环境变量临时覆盖（不改仓库配置）：
   `$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/video?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=GMT%2b8"`
   最终验收应以「迁移完成后 `mvn test` 全绿」为准。

