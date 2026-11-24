# Spec Delta

## Purpose

保证服务拆分对外部客户端零感知：既有路径、报文结构、鉴权流程与状态码保持兼容，同时明确网关的路由与身份透传契约。

## ADDED Requirements

### Requirement: 外部路径与报文兼容
所有既有 `/main/**` 接口 SHALL 保持 HTTP 方法、路径、请求参数与响应结构不变；新增服务禁止改变既有字段含义。

#### Scenario: 逐域契约回放
- **WHEN** 用拆分前录制的请求与响应快照回放用户、视频、评论、弹幕、粉丝、收藏、历史、数据中心接口
- **THEN** 拆分后的响应状态码、关键字段与字段类型与快照一致

#### Scenario: 播放进度路径调整
- **WHEN** 前端调用播放进度上报
- **THEN** 新别名路径与旧路径同时可用，旧路径至少保留一个发布周期并返回相同结果

### Requirement: 鉴权与状态码语义
网关 SHALL 只校验 access token 并向下游透传身份；未登录返回 305、可续约返回 601、令牌非法返回 602 的语义与触发条件保持不变。

#### Scenario: 无令牌访问受保护接口
- **WHEN** 请求未携带 access token 访问非白名单接口
- **THEN** 系统返回 305，且请求不会到达业务服务

#### Scenario: 刷新令牌流程
- **WHEN** access token 过期但 refresh token 有效
- **THEN** 刷新接口按现有轮换规则返回新的 access token，过期与重放场景分别返回 601 与 602

### Requirement: 身份透传与授权来源
网关 MUST 在鉴权通过后注入可信的登录用户标识；业务服务写操作 MUST 以该标识为授权依据，不得把请求参数中的 uid 当作已授权身份。

#### Scenario: 伪造 uid 写入
- **WHEN** 已登录用户 A 在请求体中传入用户 B 的 uid 调用写接口
- **THEN** 系统以 A 的身份执行或拒绝，绝不写入 B 的数据

#### Scenario: 服务间调用透传
- **WHEN** 服务 A 以当前登录用户身份调用服务 B 的内部接口
- **THEN** 服务 B 能获得同一用户标识，且该标识不来自请求参数

### Requirement: 网关路由完备
每个 `/main/**` 子路径 MUST 路由到唯一属主服务，`/remark/**` 路由到点赞服务；内部接口 MUST NOT 通过网关对公网暴露。

#### Scenario: 子路径分流
- **WHEN** 请求 `/main/favorite/**` 与 `/main/user/**`
- **THEN** 前者只到达 al-social，后者只到达 al-user，不会落到其他服务

#### Scenario: 内部接口不可达
- **WHEN** 外部客户端请求 `/internal/**`
- **THEN** 网关返回 404 或拒绝，请求不进入任何业务服务

### Requirement: 内部契约版本化
服务间 HTTP 契约 MUST 使用 `/internal/v1/**` 前缀；新增字段与新增接口 MUST 保持向后兼容，破坏性变更 MUST 通过新版本前缀提供。

#### Scenario: 契约新增字段
- **WHEN** al-user 的用户批量接口新增返回字段
- **THEN** 既有调用方无需修改即可继续工作
