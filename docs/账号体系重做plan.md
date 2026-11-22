# 账号体系重做方案（注册 / 登录 / 密码）

> 状态：**仅方案，未改代码**。审阅后按决策项执行。
> 关联：`AL-web` 登录注册链路、`AL-gateway` 鉴权、`AniLink-web` 登录弹窗、`video.user` 表。

---

## 0. 结论速览

1. 现在的注册接口**丢弃用户填写的「账号」**：`RegisterVo` 里根本没有 username 字段，后端把 `username` 写成 `"" + uid`（一串系统生成的数字）。
2. 前端注册成功后立刻拿表单里的「账号」去登录 → 后端按 `username` 查 → 查不到 → 提示「账号不存在」；再点一次注册 → `user.nickname` 上有唯一索引 → 「昵称重复」。
3. 重做方案：**username = 用户自选的登录名（唯一、不可改）**，**nickname = 显示名（允许重复）**，**密码用 bcrypt 存储（旧 MD5 登录时自动升级）**，登录失败统一文案 + Redis 限流。参考 NIST SP 800-63B-4、OWASP Authentication / Password Storage / ASVS v6。

---

## 1. 现状取证

### 1.1 日志证据（`logs/AL-web-log.txt`，2026-11-22 17:31-17:35）

| 时刻 | 日志 | 说明 |
|---|---|---|
| 17:31:21 | `INSERT INTO user ( uid, username, password, nickname, avatar, create_date )` | 注册插入成功 |
| 17:31:21 | `loginOneByNickname ... where username = ?` / `Parameters: 221188(String)` | 紧接着自动登录，用的是用户填的「账号」221188 |
| 17:31:21 | `<== Total: 0` → `LoginServiceImpl.java:55` | 库里没有 username=221188 → 抛「账号不存在」 |
| 17:31:58 | `Duplicate entry 'Yuigahama_Yui' for key 'user.nickname'` | 第二次注册，昵称唯一索引冲突 |

### 1.2 代码证据

| 位置 | 现状 | 问题 |
|---|---|---|
| `RegisterVo` | 只有 `nickname`、`password` | **没有 username**，用户填的「账号」在 DTO 层就丢了 |
| `UserInfoServiceImpl.insertOne` | `sequence = serialOps.increment(1)` → `uid = CodeUtil.geneVisibleCode(...)` → `username = "" + uid` → `nickname = 输入昵称` | 登录名被系统生成的数字取代；返回 `"" + uid` |
| `LoginServiceImpl.login` | `userInfoMapper.loginOneByNickname(loginVo.getUsername())` → 查不到抛 `ADMIN_ACCOUNT_NOT_EXIST_ERROR(306)` | 方法名叫 nickname，实际按 username 查；错误文案暴露"账号是否存在" |
| `UserInfoMapper.loginOneByNickname` | `select ... from user where username = ?` | 命名误导，需要重命名 |
| `UserInfoServiceImpl.getById(Long id)` | `uiMapper.selectOneByUsername(id.toString())` | 用 uid 当 username 查（现在恰好相等，迁移后必坏） |
| `AniLink-web/LoginDialog.vue` | 注册只发 `{nickname, password}`，随后 `signIn({username, password})` | 前端注册丢失账号；自动登录必然失败 |

### 1.3 数据与表结构证据（`video.user`）

```sql
CREATE TABLE `user` (
  `uid` bigint unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '用户账号',
  `password` varchar(255) NOT NULL COMMENT '用户密码',
  `nickname` varchar(32) NOT NULL COMMENT '用户昵称',
  ...
  PRIMARY KEY (`uid`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `nickname` (`nickname`)   -- ← 冗余约束，本次要去掉
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
```

- 密码列：当前存 **无盐 MD5**（`DigestUtils.md5DigestAsHex`）。
- 历史用户 `username` 全部等于 `uid`（数字串），这就是"要登录得找管理员要一串码"的由来。
- 实测数据（2026-11-22）：`user` 共 **22 行**，`SUM(username = CAST(uid AS CHAR)) = 22`（全部是系统码）；昵称**无重复**；表排序规则 `utf8mb3_general_ci`（大小写不敏感）；唯一索引为 `uid` / `username` / `nickname`。
- 昵称字符集：中英日混用（如「超级管理员」「朝日さん素晴らしい」），且有 2 个昵称是**纯数字**（`123123123`、`317764274`）→ 迁移时不能无脑 `username := nickname`，纯数字昵称会被 `uid` 兼容查询干扰。

### 1.4 问题清单

| # | 问题 | 严重度 |
|---|---|---|
| P1 | 注册丢弃用户自选账号，登录名变成系统码 | 阻断级（本次核心） |
| P2 | 昵称唯一索引是冗余约束（uid/username 已保证唯一） | 高 |
| P3 | 登录错误区分"账号不存在 / 密码错"，可枚举用户 | 中（安全） |
| P4 | MD5 无盐存储密码，离线爆破成本极低 | 高（安全） |
| P5 | 无失败次数限制 / 无锁定，可撞库 | 中（安全） |
| P6 | 密码策略仅前端 6 位，后端几乎不校验 | 中 |
| P7 | 注册后前端再调 login（两次请求，且账号字段丢失） | 中（体验/正确性） |
| P8 | `getById` 拿 uid 当 username 查、mapper 命名误导 | 低（重构债） |

---

## 2. 目标与非目标

**目标**

1. 注册：用户填写「登录名 + 密码（+ 可选昵称）」，注册成功即登录；**登录名就是以后登录用的账号**。
2. 昵称只用于展示，允许重复、可随时改。
3. 密码按现代标准存储，旧账号无感升级。
4. 登录失败不给攻击者提供"账号是否存在"的信息；有基础限流。
5. 保持现有双 token（access 30 分钟 + refresh 7 天滑动）与会话上限逻辑不变。

**非目标（本期不做）**

- 手机号 / 邮箱验证码登录、第三方 OAuth（GitHub/微信）、Passkey。
- 邮件找回密码（需要 SMTP 基础设施，留作阶段 2，见 D5）。
- 多因素认证（MFA）。

---

## 3. 参考了哪些成熟方案

| 参考 | 采纳的具体做法 |
|---|---|
| **NIST SP 800-63B-4 · Password Verifiers**（<https://pages.nist.gov/800-63-4/sp800-63b.html#passwordver>） | 长度优先：最短 8（无 MFA 时建议 ≥15），最长至少允许 64；**不做大小写/数字/符号的组合规则**；不静默截断；允许 unicode 与空格；不强制定期改密；用黑名单挡常见/已泄露密码 |
| **OWASP Authentication Cheat Sheet**（<https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html>） | 用户名是"用户自选、可记忆"的登录标识（邮箱也可，但需先验证）；登录失败返回**统一错误文案**；登录限流 / 账户锁定；改密必须校验旧密码 |
| **OWASP Password Storage Cheat Sheet**（<https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html>） | 首选 Argon2id（m=19456 KiB、t=2、p=1）；不可用时 bcrypt（cost ≥10，密码 ≤72 字节）；旧 MD5 在用户下次登录时重哈希升级；用恒定时间比较 |
| **OWASP ASVS v5 · V6 Authentication**（<https://github.com/OWASP/ASVS/blob/master/5.0/en/0x15-V6-Authentication.md>） | 密码安全条款、通用错误响应、认证通道要求（TLS） |
| **产品惯例：GitHub / Google / B 站** | `username` 是唯一 handle（登录用、可读、受规则约束），`nickname/display name` 是展示名（可重复、可改）；登录框明确写"用户名"，注册时实时校验占用 |
| **Spring Security 的 `BCryptPasswordEncoder` 模式** | 只借"编码器接口 + bcrypt 哈希 + 登录时按需升级"的设计，不引入 Spring Security 全家桶 |

> 为什么不用 Argon2id：需要新增 `argon2-jvm`/BouncyCastle 依赖，违反"不新增依赖需先确认"的约定；bcrypt（cost 10）在本项目体量下足够，且 **Hutool 5.8.16 自带 `cn.hutool.crypto.digest.BCrypt`**（已核对 jar 内容，该类存在；同包还有 `PBKDF2` 可作为备选），零新增依赖。若你要求 Argon2id，见 D3。

---

## 4. 新体系设计

### 4.1 身份模型

| 字段 | 语义 | 唯一 | 可改 | 规则 |
|---|---|---|---|---|
| `uid` | 内部主键，对外暴露的用户 ID | 是 | 否 | 保持现有 `CodeUtil.geneVisibleCode` 生成 |
| `username` | **登录名**（"账号"） | 是（大小写不敏感） | 否（本期） | 见 4.5 |
| `nickname` | 显示名 | **否** | 是 | 2-20 字符，允许重名 |
| `password` | 密码哈希 | - | 是 | bcrypt（`$2a$10$...` 60 字符） |

> 为什么允许中文登录名：现有 22 个用户的昵称几乎全是中文，若强制 ASCII，迁移后大家仍只能拿数字码登录，等于没解决痛点。取「允许中文、禁止纯数字」的折中（见 D1）。

### 4.2 数据库改动（`docs/sql/` 新增迁移脚本）

```sql
-- 1) 去掉冗余的昵称唯一约束
ALTER TABLE video.user DROP INDEX nickname;

-- 2)（可选，D6）记录改密时间，便于审计与强制重置
ALTER TABLE video.user ADD COLUMN password_updated_at datetime NULL AFTER password;

-- 3) 历史用户：把"系统码"换成注册时填的昵称，作为登录名
--    只迁移满足用户名规则的昵称（2-20 字符、非纯数字、无空格/emoji）；
--    不满足的（纯数字昵称、超长）保留原数字码，登录时仍可走 uid 兼容查询。
UPDATE video.user
   SET username = nickname
 WHERE username = CAST(uid AS CHAR)
   AND CHAR_LENGTH(nickname) BETWEEN 2 AND 20
   AND nickname NOT REGEXP '^[0-9]+$'
   AND nickname NOT REGEXP '[[:space:]]';
-- 预检（先跑这两条看结果再执行 UPDATE）：
--   SELECT uid, nickname FROM video.user WHERE username = CAST(uid AS CHAR);
--   SELECT uid, nickname FROM video.user
--    WHERE username = CAST(uid AS CHAR)
--      AND (CHAR_LENGTH(nickname) NOT BETWEEN 2 AND 20
--           OR nickname REGEXP '^[0-9]+$'
--           OR nickname REGEXP '[[:space:]]');
-- 实测预检结果（22 个用户）：20 个可迁移，2 个纯数字昵称保留原数字码
--   uid=567443364  nickname=123123123
--   uid=941140327  nickname=317764274
```

> `utf8mb3` 默认排序规则大小写不敏感，`username` 唯一索引本身即"大小写不敏感唯一"；应用层仍统一 `trim + toLowerCase` 做占用检查与查询，避免依赖数据库排序规则。

### 4.3 注册流程

```
POST /main/user/register   { username, password, nickname? }
  1. 规则校验（4.5）；nickname 缺省 = username
  2. Redis/DB 检查 username 占用（大小写不敏感）→ 冲突返回 210 USERNAME_TAKEN
  3. 密码策略校验 + 常见密码黑名单 → 弱密码返回 211 PASSWORD_WEAK
  4. bcrypt 哈希 → INSERT user（username/nickname/password/avatar 默认值/create_date）
  5. 事务内初始化 user_stats + 默认收藏夹
  6. 捕获 DuplicateKeyException（并发抢名）→ 返回 210
  7. 复用 LoginService 的 issueTokens() 直接签发 access + refresh Cookie（注册即登录）
  8. 返回 { uid, username, nickname }
```

- 注册**不发**画像 MQ 消息（用户还没行为）；登录成功时才 `notifyProfile`（保持现状）。
- 事务边界：user + user_stats + favorite 三写在同一个 `@Transactional` 里（现状已如此）。

### 4.4 登录流程

```
POST /main/login   { username, password }
  1. 取 username 规范化值；限流检查（见 4.6）→ 超限返回 212 TOO_MANY_ATTEMPTS
  2. 查询用户：
     a. 先按 username（大小写不敏感）
     b. 兼容期：若输入是纯数字且 a 未命中，再按 uid 查（让老用户用旧数字码也能登）
  3. 用户不存在 → 仍然执行一次 bcrypt 校验（dummy hash），再返回统一错误
  4. 用户存在 → 校验密码：
     - `$2a$/$2b$/$2y$` 前缀 → bcrypt 校验
     - 32 位十六进制 → 视为历史 MD5：md5 校验通过后**立即重哈希为 bcrypt 并写回**
  5. status 校验：封禁 308 / 注销 310（这两类可以给出明确文案，不涉及"是否存在"的信息泄露，因为要密码正确后才会到达）
  6. 清零失败计数 → notifyProfile → issueTokens()（保持现有双 token 与会话上限）
  失败统一返回 307「用户名或密码错误」，且不记录"账号不存在"到响应
```

### 4.5 校验规则

| 项 | 规则 | 失败提示 |
|---|---|---|
| username | 2-20 字符；允许中文、字母、数字、下划线；必须含至少一个非数字字符（禁止纯数字，避免与 uid 兼容查询混淆）；保留字（admin/root/system/anilink/official/support/admin123…）；不区分大小写 | 「用户名 2-20 位，可含中英文、数字和下划线」/「用户名不能是纯数字」/「该用户名已被占用」 |
| password | 8-72 字符（bcrypt 72 字节上限）；允许任意字符含 unicode/空格；不做组合规则；拒绝内置常用密码表（本地 top 1000） | 「密码至少 8 位」/「密码太常见，请换一个」 |
| nickname | 2-20 字符；trim 后非空；禁止控制字符；允许重名 | 「昵称 2-20 个字符」 |

> 若后续切 Argon2id，密码上限可放宽到 128；bcrypt 下必须 ≤72 字节，且**不做静默截断**（超长直接报错）。

### 4.6 安全措施

- **错误统一**：账号不存在 / 密码错误 → 同一 code（307）+ 同一文案 + 大致相同耗时（dummy bcrypt）。
- **限流**（Redis，沿用现有 `StringRedisTemplate`）：
  - `auth:fail:user:{username小写}`：15 分钟窗口，≥5 次 → 锁定 15 分钟（返回 212，文案「尝试过于频繁，请稍后再试」）
  - `auth:fail:ip:{ip}`：15 分钟窗口，≥20 次 → 同样拒绝
  - 登录成功清空两个 key；锁定状态不影响已登录用户的 token 续约
  - 网关透传真实 IP（`X-Real-IP`），否则退化为 gateway 的 IP（见 D7）
- **密码存储**：bcrypt cost=10（可配 `al.auth.password.bcrypt-cost`，默认 10）。
- **日志**：绝不打印密码/哈希；登录成功/失败记 uid + IP + 结果（不含凭据）。
- **改密**（本期可做最小实现）：`POST /main/user/changePassword { oldPassword, newPassword }`，要求登录态 + 旧密码校验，成功后**吊销该用户全部会话**（复用双 token 的 revokeAll）。

### 4.7 错误码

| code | 常量 | 文案 |
|---|---|---|
| 307 | 复用 `ADMIN_ACCOUNT_ERROR` | 用户名或密码错误（账号不存在也用它） |
| 210 | 新增 `USERNAME_TAKEN` | 该用户名已被占用 |
| 211 | 新增 `PASSWORD_WEAK` | 密码强度不足 |
| 212 | 新增 `TOO_MANY_ATTEMPTS` | 尝试过于频繁，请稍后再试 |
| 308 / 310 | 复用 | 该用户已被禁用 / 该账号已注销 |

> `306 ADMIN_ACCOUNT_NOT_EXIST_ERROR` 保留常量但**不再返回给前端**（避免枚举）。

---

## 5. 改动清单

### 后端 `AniLink-Java`

| 文件 | 改动 | 预估 |
|---|---|---|
| `AL-common/.../result/ResultCodeEnum.java` | 新增 210/211/212 | 5 行 |
| `AL-common/.../utils/PasswordUtil.java`（新增） | `encode / matches / isLegacyMd5 / upgradeIfNeeded`，基于 `cn.hutool.crypto.digest.BCrypt` | ~70 行 |
| `AL-web/.../vo/RegisterVo.java` | 增加 `username`；`nickname` 改为可选 | 3 行 |
| `AL-web/.../vo/ChangePasswordVo.java`（新增） | oldPassword / newPassword | ~10 行 |
| `AL-web/.../service/impl/UserInfoServiceImpl.java` | 重写 `insertOne`：校验 → 占用检查 → bcrypt → 插库（username 用用户输入）；修 `getById` 改按 uid 查 | ~80 行 |
| `AL-web/.../controller/UserController.java` | `register` 返回 `{uid, username, nickname}`，内部调用 `LoginService.issueTokens` 直接下发 token；新增 `changePassword` | ~30 行 |
| `AL-web/.../service/impl/LoginServiceImpl.java` | 登录改为 `loginByUsername` + 兼容 uid + bcrypt/MD5 升级 + 限流 + 统一文案；`issueTokens` 改为 public 供注册复用；新增 `changePassword` | ~90 行 |
| `AL-web/.../mapper/UserInfoMapper.java` + XML | `loginOneByNickname` → `loginByUsername`；新增 `selectByUid`、`updatePassword` | ~30 行 |
| `AL-web/.../service/LoginService.java` | 接口同步 | 5 行 |
| `AL-web/.../config/AuthTokenProperties` 或新增 `PasswordProperties` | bcrypt cost、限流阈值可配 | ~20 行 |
| `docs/sql/V2__account_rewrite.sql`（新增） | DROP INDEX nickname + 历史 username 归一 + 可选 password_updated_at | ~20 行 |

### 前端 `AniLink-web`

| 文件 | 改动 |
|---|---|
| `api/auth.ts` | `RegisterPayload` 增加 `username`；新增 `changePassword`；`register` 返回类型 `{uid, username, nickname}` + token |
| `components/base/LoginDialog.vue` | 登录/注册两态分栏：注册字段「用户名 / 密码 / 昵称(可选)」；用户名实时规则与占用提示；密码规则提示（8-72，无组合要求）；注册成功直接进已登录态，不再二次 login |
| `stores/user.ts` | `signIn` 支持"注册返回 token"的路径（或复用 setToken） |
| `views/account/AccountView.vue`（可选） | 增加"修改密码"入口 |
| `README.md` | 登录注册规则说明 |

### 不需要改

- `AL-gateway`（登录/注册是 `/main/login`、`/main/user/register` 公开路径，白名单已覆盖）。
- `AL-remark` / `AL-communication` / `AniLink-AlgorithmEngine`（不涉及账号模型）。
- 双 token 机制（4.3/4.4 直接复用 `issueTokens`）。

---

## 6. 数据库与 MQ 影响

- **DDL**：仅 `user` 表 `DROP INDEX nickname`（数据量 < 100 行，秒级）；可选新增列。
- **数据迁移**：把 `username = CAST(uid)` 的行改成 `username = nickname`；执行前先备份 `video.user`。
- **MQ**：注册不新增消息；登录仍发画像构建消息（现状）。若 RabbitMQ 不可用，登录/注册不应失败——建议 `notifyProfile` 内部 try/catch（现状是否已捕获需确认，实现时补）。
- **Redis**：新增限流 key（自动过期），不影响现有 `auth:*` 会话键。

---

## 7. 验证计划

| # | 场景 | 期望 |
|---|---|---|
| 1 | 注册 `alice / Passw0rd!` | 200，返回 uid+username；库里 `username='alice'`、`password` 以 `$2a$` 开头；已登录 |
| 2 | 用 alice + 密码登录 | 200 |
| 3 | 再注册 `Alice`（大写） | 210 该用户名已被占用 |
| 4 | 注册昵称与他人相同 | 成功（昵称不再唯一） |
| 5 | 注册弱密码 `123456` / 6 位 | 211 密码强度不足 |
| 6 | 非法用户名 `1abc`、`a`、`admin` | 202/210 按规则拒绝 |
| 7 | 密码错 / 用户不存在 | 均为 307「用户名或密码错误」，响应耗时相近 |
| 8 | 连续错 5 次 | 第 6 次 212，锁 15 分钟；成功登录后计数清零 |
| 9 | 历史 MD5 用户登录 | 200，且 `password` 自动变为 `$2a$...` |
| 10 | 历史用户用原数字码登录 | 200（uid 兼容查询） |
| 11 | 迁移后老用户用昵称登录（20 个可迁移用户） | 200 |
| 11.1 | 两个纯数字昵称用户用原数字码登录 | 200，username 仍为数字码 |
| 12 | 并发注册同名 2 次 | 1 成功 1 返回 210（唯一索引兜底） |
| 13 | 注册后 refresh token 正常续约 | 601→续约链路不变 |
| 14 | 前端弹窗：注册成功不再弹"账号不存在" | 目视 |
| 15 | 改密后旧会话全部失效 | 用旧 access 调接口 → 602 |

---

## 8. 风险与回滚

| 风险 | 说明 | 缓解 |
|---|---|---|
| 迁移把 username 改成昵称 | 若昵称含空格/超长/重复会失败 | 迁移前 `SELECT` 预检；冲突或非法时保留数字串；脚本幂等 |
| 老用户不知道新登录名 | 迁移后登录名=昵称，用户熟悉；同时保留"数字码=uid"兼容 | 登录兼容期至少保留一个版本 |
| bcrypt 性能 | cost=10 每次约 50-100ms | 单机开发量级无压力；可配置调整 |
| 限流误伤 | 同 IP 多人共用 NAT | 阈值按 IP 放宽（20/15min），账号维度 5/15min |
| **登录依赖 MQ** | `notifyProfile` 直接 `rabbitTemplate.convertAndSend`，RabbitMQ 不可用会让登录抛 500（现状如此） | 本次顺手用 try/catch 包住并降级为 warn 日志，画像消息丢了不影响登录 |
| 密码长度 72 字节上限 | 超长密码会被 bcrypt 截断 | 校验层直接拒绝 >72 字节，不静默截断 |
| 回滚 | DDL 是 DROP INDEX，可重新 `ADD UNIQUE`（若已有重复昵称会失败） | 迁移前备份；回滚脚本同时提供 |

---

## 9. 待确认决策

| # | 决策点 | 建议默认 |
|---|---|---|
| D1 | 用户名规则：仅 ASCII（字母开头+数字/下划线）还是允许中文？ | **允许中文、禁止纯数字**（现有用户昵称全是中文，迁移后能直接用昵称登录；纯数字昵称保留原数字码） |
| D2 | 是否要求邮箱（用于找回密码）？ | **不要求**；邮箱留到阶段 2（D5） |
| D3 | 密码哈希：bcrypt(cost 10，零新增依赖) 还是 Argon2id（需加依赖）？ | **bcrypt** |
| D4 | 历史用户迁移：`username := nickname` 自动改，还是保留数字码 + 登录后引导设置用户名？ | **自动改**（迁移后立即可用昵称登录），非法/冲突则保留数字码 |
| D5 | 本期是否做「忘记密码」？ | **不做**（无邮件服务）；先提供改密接口 + 管理员重置（可选） |
| D6 | 是否新增 `password_updated_at` 列？ | **加**（审计/强制重置用，成本低） |
| D7 | 限流维度与阈值 | **账号 5 次/15 分钟 + IP 20 次/15 分钟** |
| D8 | 注册是否直接返回 token（注册即登录）？ | **是**（消灭注册后二次登录的失败点） |
| D9 | `username` 是否允许修改？ | **本期不允许**（GitHub 允许改但要处理链接与占用，成本高） |

---

## 10. 工作量估算

| 阶段 | 内容 | 估时 |
|---|---|---|
| 1 | `PasswordUtil` + 错误码 + DDL/迁移脚本 | 0.5 天 |
| 2 | 注册链路重写（后端） | 0.5 天 |
| 3 | 登录链路（bcrypt/升级/限流/统一文案） | 0.5-1 天 |
| 4 | 前端登录弹窗重做 + 改密入口 | 0.5-1 天 |
| 5 | 验证（15 条用例）+ 迁移演练 | 0.5 天 |
| 合计 | | **2.5-3.5 天** |

---

## 附录 A：为什么这样设计（自问自答）

- **为什么保留 uid 与 username 分离？** uid 是内部主键、对外 ID（视频/评论/关注都引用它）；username 是可读登录名，允许改名（本期不改）而不影响引用。
- **为什么昵称可以重复？** 昵称是展示名，B 站/GitHub 都允许重名；唯一性由 uid/username 保证。去掉唯一索引同时解决"注册同名昵称报 500"。
- **为什么登录不用昵称？** 昵称可重复、可改；登录标识必须稳定唯一。
- **为什么不直接上 Spring Security？** 本项目已有自己的 JWT + 网关鉴权体系，引入完整框架会重写链路；只借 bcrypt 编码器思路 + Hutool 实现，改动面最小。
- **为什么错误码不区分"账号不存在"？** 区分会把注册过的账号暴露给撞库者（OWASP 明确指出）。

---

## 附录 B：执行结果（2026-11-22 已实施，未提交）

### B.1 实际改动

| 端 | 文件 |
|---|---|
| AL-common | `utils/PasswordUtil.java`（新增，bcrypt + MD5 兼容 + dummy 校验）、`result/ResultCodeEnum.java`（210/211/212） |
| AL-gateway | `filter/AccountAuthFilter.java`（透传 `X-Real-IP`，供登录限流用） |
| AL-web | `util/AccountRules.java`（新增）、`vo/RegisterVo.java`、`vo/ChangePasswordVo.java`（新增）、`vo/LoginResultVo.java`（新增）、`mapper/UserInfoMapper.java`（新增 `updatePassword`，删除失效的 `loginOneByNickname`）、`service/UserInfoService.java` + `impl/UserInfoServiceImpl.java`（注册重写、`getById` 改按 uid）、`service/LoginService.java` + `impl/LoginServiceImpl.java`（登录重写、限流、改密、issueTokens 公开、MQ 降级）、`controller/UserController.java`（注册返回 token、新增 changePassword） |
| DB | `docs/sql/V2__account_rewrite.sql`（DROP INDEX nickname + 新增 password_updated_at + 历史 username 迁移） |
| 前端 | `api/auth.ts`、`stores/user.ts`（applySession）、`components/base/LoginDialog.vue`（登录/注册重做） |

### B.2 迁移结果（已执行）

- 备份：`video.user_bak_20261122`（22 行）；
- `user.nickname` 唯一索引已删除；
- 新增 `password_updated_at`；
- 22 个用户中 **20 个** `username` 改为昵称，**2 个纯数字昵称**（`123123123`、`317764274`）保留原数字码；
- 迁移后 `video.user` 仍为 22 行。

### B.3 实测结果（临时 8025 实例）

| # | 场景 | 结果 |
|---|---|---|
| 1 | 注册中文用户名（昵称缺省） | 200，返回 access + uid/username/nickname；库内 `password` 为 `$2a$10$…`（60 字符），user_stats 与默认收藏夹已创建 |
| 2 | 用该中文用户名登录 | 200 |
| 3 | 同名用户名（大小写不同） | 210 该用户名已被占用 |
| 4 | 两个用户用相同昵称注册 | 均 200（昵称不再唯一） |
| 5 | 弱密码 `12345678` / 3 位 | 211 |
| 6 | 纯数字 / admin / 1 位 / 带空格用户名 | 202，提示分别正确 |
| 7 | 密码错误 vs 用户不存在 | 均为 307 |
| 8 | 连续错 5 次后第 6 次 | 212 尝试过于频繁 |
| 9 | 历史 MD5 用户登录（超级管理员/123456） | 200，库内哈希升级为 `$2a$10$`，`password_updated_at` 写入 |
| 10 | 用历史数字 uid 登录（123123123/123456） | 200 |
| 11 | 改密（旧密码校验） | 200；改密后用旧 access 调 `/main/info` → 602；新密码登录 200、旧密码 307 |
| 12 | 并发同名注册 ×2 | 一个 200、一个 210，库中仅 1 行 |

测试账号（`测试用户_01`、`Alice_Test`、`nick_user_a/b`、`conc_user`）已清理，用户数回到 22。

### B.4 与计划的差异

1. **登录接口仍返回 access 字符串**（不改已有契约）；**注册接口**返回 `LoginResultVo`（accessToken + uid + username + nickname），前端用 `applySession` 落库，不再二次登录。
2. **网关新增 `X-Real-IP` 透传**：计划里 D7 的 IP 维度限流需要真实 IP，否则只有账号维度生效。**需要重启网关**才生效。
3. **新增 `password_updated_at`**（D6），在 `updatePassword` 时写 `now()`。
4. 历史纯数字昵称的 2 个用户，`username` 保留数字码，登录仍用数字码（不走昵称）。

### B.5 上线注意

- 需要**同时重启 AL-web 与 AL-gateway**（限流 IP 头 + 新接口）。
- 迁移脚本已在本机执行；其他环境执行前请先备份 `user` 表（脚本已包含 `CREATE TABLE … AS SELECT`）。
- 老用户登录名已变为昵称（20 个），管理员需通知；2 个纯数字昵称用户仍用原数字码。
- 若前端仍显示旧弹窗，刷新页面即可（`api/auth.ts` 与 `LoginDialog.vue` 已更新）。
