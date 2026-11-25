# Spec Delta

## Purpose

让管理员查询用户、封禁/解封并调整角色，同时保证不能查看密码、不能物理删除用户、也不能通过自我提权绕过权限。

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

### Requirement: 用户数据安全
用户管理 MUST NOT 物理删除用户、MUST NOT 修改密码、MUST NOT 返回密码或敏感凭据。

#### Scenario: 管理端查看用户详情
- **WHEN** 管理员打开用户详情
- **THEN** 只展示 uid、账号、昵称、状态、角色、创建时间等资料，不展示密码
