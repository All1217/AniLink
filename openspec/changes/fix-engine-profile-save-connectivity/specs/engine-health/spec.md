# Spec Delta

## Purpose

提供一个只读的健康检查接口，快速区分“MySQL/Redis/RabbitMQ/ES 或后端网关不可用”与“画像写入等业务错误”，并给出逐项耗时。

## ADDED Requirements

### Requirement: 组件健康检查
`GET /health` MUST 检查 MySQL、Redis、RabbitMQ、Elasticsearch 与后端网关，并在响应中逐项返回 `status`、耗时与错误摘要。

#### Scenario: 全部依赖可用
- **WHEN** 五类依赖都能正常响应
- **THEN** 返回 HTTP 200，整体 `status=ok`，每项 `status=ok`

#### Scenario: 核心依赖不可用
- **WHEN** MySQL、Redis 或 RabbitMQ 任意一项检查失败
- **THEN** 返回 HTTP 503，整体 `status=down`，并标出失败项

#### Scenario: 非核心依赖不可用
- **WHEN** MySQL/Redis/RabbitMQ 正常，但 Elasticsearch 或后端网关失败
- **THEN** 返回 HTTP 200，整体 `status=degraded`，并标出失败项

### Requirement: 检查超时
每项检查 MUST 设置不超过 2 秒的超时；单个依赖无响应 MUST NOT 让整个 `/health` 请求一直挂起。

#### Scenario: 后端网关无响应
- **WHEN** 后端网关连接超时
- **THEN** 该检查在超时后返回失败，其余检查结果照常返回

### Requirement: 只读与脱敏
健康检查 MUST 只做探活（如 `SELECT 1`、`PING`、连接状态、HTTP GET），不得写入或删除业务数据；响应 MUST NOT 包含口令、Cookie 或完整连接串。

#### Scenario: 检查不产生副作用
- **WHEN** 连续调用 `/health`
- **THEN** 数据库、Redis、RabbitMQ 与 ES 的业务数据不变

#### Scenario: 错误信息脱敏
- **WHEN** 某组件连接失败
- **THEN** 响应只包含组件名与简短错误原因，不包含账号口令

### Requirement: 后端联通性检查
后端网关检查 MUST 调用一个公开、无副作用的接口，并在响应中单独标记后端状态，便于区分“引擎正常但后端不可用”。

#### Scenario: 后端不可用
- **WHEN** 网关 8090 未启动
- **THEN** `/health` 的后端项为失败，整体仍返回其余依赖的真实状态
