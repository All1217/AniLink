# Phase 5 分库 · 00 全库快照（回滚兜底）

分库采用「同实例 `RENAME TABLE`」，不搬数据、不重建表，正常情况下 `99-rollback.sql` 反向改名即可回滚。
但改名后表结构/行数若被误改、或 `RENAME` 中途出现意料之外的失败，反向改名救不回来，所以**执行任何 DDL 之前必须先做一次全库快照**。
快照是最后一道回滚手段，和 `99-rollback.sql` 组成双保险。

## 1. 前置检查

在**业务停机窗口内、所有 Java 服务与 Python 引擎停止写入之后**执行（顺序见 [README.md](README.md)）。

```powershell
# 1) 确认连的是目标实例（库名/版本/端口），避免快照错实例
mysql -h <mysql-host> -P 3306 -u <admin-user> -p -e "SELECT VERSION(), @@hostname, @@port; SHOW DATABASES;"

# 2) 记录本次快照标识，后续文件名/恢复演练都用它
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupDir = "E:\backup\anilink-phase5\$stamp"
New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
Write-Output "backupDir=$backupDir"
```

要求：

- 快照目录所在磁盘剩余空间 ≥ 当前数据量的 2 倍（`mysqldump` 文本 + 后续压缩/恢复演练）。
- `mysqldump` 客户端版本尽量与 MySQL 服务器一致（本项目 MySQL 8.0.33）。
- 账号至少需要 `SELECT`、`SHOW VIEW`、`TRIGGER`、`LOCK TABLES`（`--single-transaction` 下主要需要 `SELECT`）。
- 记录当前表数量与行数基线（迁移后逐项比对，见第 4 节）。

## 2. 全库快照命令（推荐：单文件、含建库语句）

`--databases` 会额外写入 `CREATE DATABASE` / `USE`，恢复时无需手建库。
**Windows 下不要用 PowerShell 的 `>` 重定向**：PowerShell 默认写 UTF-16LE，生成的文件 `mysql` 无法直接恢复。
优先用 `mysqldump --result-file=` 让 mysqldump 自己写文件；必须用管道时走 `cmd /c`。

```powershell
# 推荐写法：由 mysqldump 直接写文件，避免 PowerShell 重定向编码问题
mysqldump -h <mysql-host> -P 3306 -u <admin-user> -p `
  --single-transaction --quick --routines --triggers --events `
  --set-gtid-purged=OFF --no-tablespaces --hex-blob `
  --databases video al_remark al_communication `
  --result-file="$backupDir\anilink-phase5-before.sql"

# 压缩留存（文本很大，压缩后再归档）
Compress-Archive -Path "$backupDir\anilink-phase5-before.sql" -DestinationPath "$backupDir\anilink-phase5-before.zip"

# 记录校验和，恢复前核对同一份文件
Get-FileHash "$backupDir\anilink-phase5-before.zip" -Algorithm SHA256 |
  Format-List | Out-File -Encoding utf8 "$backupDir\sha256.txt"
Get-Content "$backupDir\sha256.txt"
```

### 参数说明

| 参数 | 作用 |
| --- | --- |
| `--single-transaction` | InnoDB 下开启一致性快照，不锁表（仍需先停写，避免长事务回滚段膨胀） |
| `--quick` | 逐行取数，避免大表一次性读进内存 |
| `--routines --triggers --events` | 连存储过程/触发器/事件一起备份（本项目目前没有，留作兜底） |
| `--set-gtid-purged=OFF` | 避免恢复到普通实例时报 GTID 相关的导入错误 |
| `--no-tablespaces` | 非 root 账号没有 `PROCESS` 权限时避免 `SHOW TABLESPACE` 报错 |
| `--hex-blob` | 二进制列安全导出 |
| `--databases video al_remark al_communication` | 本次分库涉及的三个库；`al_user` 等目标库此时还是空的，无需备份 |

> 不要用 `--all-databases`：会把 `mysql` 系统库（账号/权限）一起导出，恢复时风险更大。
> 本机开发环境若口令为空，`-p` 后可省略交互；命令里的 `<...>` 全是占位符，**不要把真实口令写进脚本或文档**。

### 备选：按库拆成多个文件

```powershell
foreach ($db in @('video', 'al_remark', 'al_communication')) {
  mysqldump -h <mysql-host> -P 3306 -u <admin-user> -p `
    --single-transaction --quick --routines --triggers --events `
    --set-gtid-purged=OFF --no-tablespaces --hex-blob `
    --databases $db --result-file="$backupDir\$db-before.sql"
}
```

## 3. 快照完整性校验

```powershell
# 3.1 文件存在且大小合理（空库导出通常也有几十 KB）
Get-ChildItem $backupDir | Select-Object Name, Length, LastWriteTime

# 3.2 结尾必须包含 mysqldump 完成标记；缺失说明导出中断，快照不可用
Get-Content "$backupDir\anilink-phase5-before.sql" -Tail 5

# 3.3 头部包含三个库的建库语句
Select-String -Path "$backupDir\anilink-phase5-before.sql" -Pattern 'CREATE DATABASE' |
  Select-Object -ExpandProperty Line
```

判定标准：

- 文件尾必须是 `-- Dump completed on ...`，否则**不要继续**，重新导出。
- 三个库的 `CREATE DATABASE` 都在（`video`、`al_remark`、`al_communication`）。
- `Compress-Archive` + `Get-FileHash` 成功，归档文件与 SHA256 一起保留到验收结束之后。

### 恢复演练（可选但强烈建议）

在**另一个空实例**（或临时实例）上验证快照能导入，不要在生产库上演练：

```powershell
# 临时实例示例：docker 起一个 MySQL 8.0，端口 3307
docker run --rm -d --name anilink-restore-test -e MYSQL_ROOT_PASSWORD=<temp-pass> -p 3307:3306 mysql:8.0.33

# 导入快照（cmd /c 让 cmd 处理重定向，文件本身由 mysqldump 写成 UTF-8）
cmd /c "mysql -h 127.0.0.1 -P 3307 -u root -p<temp-pass> < ""$backupDir\anilink-phase5-before.sql"""

# 抽查：三个库都在、video 有 16 张表
mysql -h 127.0.0.1 -P 3307 -u root -p<temp-pass> -e "SHOW DATABASES; SHOW TABLES FROM video;"

docker stop anilink-restore-test
```

## 4. 迁移前基线（留档，供迁移后比对）

```sql
-- 4.1 各库表数量：video 应为 16 张业务表（另含 user_bak_20261122 备份表时以实际为准）
SELECT table_schema, COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema IN ('video', 'al_remark', 'al_communication')
GROUP BY table_schema
ORDER BY table_schema;

-- 4.2 每张表的行数估算（InnoDB 统计值，用于迁移后是否近似一致的快速判断）
SELECT table_schema, table_name, table_rows
FROM information_schema.tables
WHERE table_schema IN ('video', 'al_remark', 'al_communication')
ORDER BY table_schema, table_name;
```

执行后把第 4 节的输出**原样贴进执行记录**（不要只写“已备份”），迁移完成后用同样的 SQL 对比。

## 5. 快照保留策略

- 快照与 SHA256 至少保留到 Phase 5 验收完成、且旧库 `video` 确认可删除之后。
- 旧库清理前再确认一次：`99-rollback.sql` 已不再需要（观察期内无人回滚）且快照文件仍可读。
- 快照包含用户数据，按内部资料管理，不要提交到 git、不要放入前端静态目录。
