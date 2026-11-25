# 数据库初始化

`db/schema.sql` 是整个 AniLink 的 MySQL 初始化脚本，覆盖当前代码实际使用的三个库：

| 库 | 用途 | 表 |
| --- | --- | --- |
| `video` | AL-web / AL-gateway / 画像引擎共用的主库 | 16 张业务表 |
| `al_remark` | AL-remark 配置里的库；当前服务只用 Redis，库保持为空 | 0 |
| `al_communication` | AL-communication 配置里的库；当前服务只用 Netty + 内存，库保持为空 | 0 |

## 为什么放在根目录 `db/`

这份 schema 不是某一个服务的私有资源：`AL-web`、`AL-gateway`、`AL-remark`、`AL-communication` 和 Python 画像引擎都会读取其中的库/表。放在根目录可以避免把它误认为 `AL-web` 的自动初始化脚本，也方便部署时统一执行。

## 用法

```powershell
mysql -u root -p < db/schema.sql
```

脚本特性：

- `CREATE DATABASE IF NOT EXISTS`：不会删除已有库。
- `CREATE TABLE IF NOT EXISTS`：空库可直接初始化；已有表不会被修改。
- 不包含 `DROP TABLE`，不会清空数据。
- 不含业务种子数据；管理员账号需自行注册后调整 `user.role`。

## `video` 表清单

| 表 | 用途 | 主要使用方 |
| --- | --- | --- |
| `user` | 用户、角色、状态 | AL-web、网关鉴权 |
| `user_stats` | 粉丝/关注/获赞等统计 | AL-web |
| `user_tag` | 用户画像语义标签 | 画像引擎、AL-web |
| `video` | 视频稿件与转码/审核状态 | AL-web |
| `video_stats` | 播放/弹幕/点赞等统计 | AL-web |
| `category` | 主/子分区 | 画像引擎向量化 |
| `bh_video` | 浏览历史 | AL-web |
| `user_video` | 用户-视频互动 | AL-web、画像引擎 |
| `favorite` | 收藏夹 | AL-web |
| `favorite_video` | 收藏夹-视频关系 | AL-web |
| `follow` | 关注关系 | AL-web |
| `comment` | 评论 | AL-web |
| `danmu` | 弹幕 | AL-web、画像引擎 |
| `sub_danmu` | 弹幕测试表，`DanmuMapper` 仍引用 | AL-web |
| `chat` | 聊天会话 | AL-web |
| `chat_message` | 聊天消息 | AL-web |

## 未纳入的表

- `video.user_bak_20261122`：开发库中的备份表，代码无引用，未导出。
- `al_remark` / `al_communication` 当前没有业务表；服务只使用 Redis/内存。建空库是为了 datasource 能启动。

## 维护

- 本文档与 `schema.sql` 以当前开发库结构为准；新增/修改表后应同步更新，并优先通过迁移脚本管理已有库。
- 旧文件 `AniLink-AlgorithmEngine/Assets/sql.sql` 只覆盖部分早期表，已不是完整初始化来源；保留用于历史参考。
