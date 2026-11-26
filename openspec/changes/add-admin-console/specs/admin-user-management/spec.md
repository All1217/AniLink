# Spec Delta

## Purpose

让管理员查询用户、封禁/解封、调整角色并重置密码；原密码始终不可查看，重置只生成一次性临时密码，且不能通过自我操作绕过权限。

## ADDED Requirements

### Requirement: 用户分页与筛选
管理端 MUST 支持按 uid、用户名/昵称关键字、账号状态与角色分页查询用户；响应 MUST NOT 包含密码哈希或令牌。

#### Scenario: 按用户名搜索
- **WHEN** 管理员输入关键字查询用户
- **THEN** 返回匹配的用户分页信息，不包含 `password` 字段

#### Scenario: 按封禁状态筛选
- **WHEN** 管理员筛选 `status=1`
- **THEN** 只返回被封禁用户

### Requirement: 封禁与解封
管理员 MUST 能把用户 `status` 在 0（正常）与 1（封禁）之间切换；管理员 MUST NOT 封禁自己。

#### Scenario: 封禁用户
- **WHEN** 管理员封禁一个普通用户
- **THEN** `user.status=1`，该用户后续登录被拒绝

#### Scenario: 封禁自己
- **WHEN** 管理员尝试封禁自己的 uid
- **THEN** 请求被拒绝，账号状态不变

### Requirement: 角色调整
角色调整 MUST 只允许 role=2（超级管理员）执行，目标角色只能是 0/1/2；管理员 MUST NOT 修改自己的角色。

#### Scenario: 超级管理员设置管理员
- **WHEN** role=2 用户把普通用户设为 role=1
- **THEN** 目标用户获得管理员权限

#### Scenario: 自我降级
- **WHEN** 超级管理员尝试修改自己的 role
- **THEN** 请求被拒绝，角色不变

### Requirement: 管理员重置密码
管理员 MUST 能重置用户密码：服务端生成临时密码并以 bcrypt 存储，临时密码只在响应中返回一次；重置后 MUST 吊销目标用户的 refresh 会话。role=1 只能重置 role=0 用户，role=2 可重置 role=0/1 用户；任何管理员 MUST NOT 重置自己的密码。管理员 MUST NOT 查看原密码或密码哈希。

#### Scenario: 普通管理员重置普通用户
- **WHEN** role=1 管理员对 role=0 用户执行重置密码
- **THEN** 返回一次性临时密码，数据库只保存 bcrypt 哈希，目标用户 refresh 会话被吊销

#### Scenario: 普通管理员重置管理员
- **WHEN** role=1 管理员尝试重置 role=1 或 role=2 用户的密码
- **THEN** 返回 309，密码与会话不变

#### Scenario: 重置自己的密码
- **WHEN** 管理员尝试重置自己的密码
- **THEN** 请求被拒绝，应改用普通改密流程

### Requirement: 用户数据安全
用户管理 MUST NOT 物理删除用户、MUST NOT 返回密码哈希或明文；密码只能通过重置接口生成一次性临时密码，管理员不能查看原密码。

#### Scenario: 管理端查看用户详情
- **WHEN** 管理员打开用户详情
- **THEN** 只展示 uid、账号、昵称、状态、角色、创建时间等资料，不展示密码

#### Scenario: 点击密码列眼睛
- **WHEN** 管理员点击某行密码列的查看图标
- **THEN** 不展示原密码，而是进入“重置密码”确认流程，成功后仅展示一次临时密码
