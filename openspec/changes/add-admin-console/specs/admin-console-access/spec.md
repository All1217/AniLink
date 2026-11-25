# Spec Delta

## Purpose

只允许具备管理员角色的用户进入管理端，并按角色限制敏感操作，避免普通用户凭有效 token 调用后台接口。

## ADDED Requirements

### Requirement: 管理接口角色校验
所有 `/main/admin/**` 接口 MUST 校验登录用户 `role` 为 1 或 2；普通用户或未登录请求 MUST 被拒绝且不得读取或修改管理数据。

#### Scenario: 普通用户访问管理接口
- **WHEN** role=0 的用户携带有效 access token 请求 `/main/admin/videos`
- **THEN** 返回 309（无访问权限），不返回任何稿件数据

#### Scenario: 未登录访问管理接口
- **WHEN** 请求未携带 access token
- **THEN** 网关或服务返回未登录状态，管理接口不执行

### Requirement: 管理端登录校验
管理前端 MUST 在登录后调用管理端身份接口确认角色；非管理员 MUST NOT 进入管理页面。

#### Scenario: 管理员登录
- **WHEN** role=1 或 2 的用户在管理端登录
- **THEN** 管理端展示视频、用户、存储三个入口

#### Scenario: 普通用户尝试登录管理端
- **WHEN** role=0 的用户在管理端输入正确账号密码
- **THEN** 管理端提示无管理员权限，不进入管理页面

### Requirement: 超级管理员专属操作
角色调整 MUST 只允许 role=2 执行；role=1 调用角色调整接口 MUST 返回 309。

#### Scenario: 管理员尝试改角色
- **WHEN** role=1 的用户请求修改另一个用户的 role
- **THEN** 请求被拒绝，目标用户角色不变

### Requirement: 操作日志
所有管理端写操作 MUST 记录操作者 uid、动作、目标与结果；日志 MUST NOT 包含密码、令牌或完整连接串。

#### Scenario: 审核稿件
- **WHEN** 管理员通过某稿件
- **THEN** 应用日志包含操作者 uid、vid、动作 approve
