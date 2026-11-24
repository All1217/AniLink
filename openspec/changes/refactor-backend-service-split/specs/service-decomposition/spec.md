# Spec Delta

## Purpose

把后端按业务域拆成职责单一、可独立部署的服务，并明确服务之间只允许通过契约通信，以支撑按业务扩容与独立演进。

## ADDED Requirements

### Requirement: 服务目录与域归属
系统 SHALL 由 al-gateway、al-user、al-video、al-playback、al-social、al-comment、al-danmaku、al-datacenter、al-remark、al-communication 十个可部署单元组成；每个业务域 MUST 只由一个服务拥有。

#### Scenario: 全量启动注册
- **WHEN** 全部服务以默认配置启动
- **THEN** Nacos 中注册十个服务名，且每个服务只暴露其所属域的 HTTP/WebSocket 端点

#### Scenario: 域边界可判定
- **WHEN** 审查任一业务接口的实现位置
- **THEN** 它只出现在服务目录声明的属主服务内，不出现跨域 Controller

### Requirement: 跨服务依赖方向
服务间调用 MUST 只通过内部 HTTP 契约或消息事件；任何服务 MUST NOT 依赖其他服务的实现模块、Mapper 或实体类。

#### Scenario: 构建期依赖检查
- **WHEN** 执行架构规则检查
- **THEN** 不存在服务模块对其他服务实现模块的 Maven 依赖，也不存在跨服务 Mapper/Entity 引用

#### Scenario: 运行期跨域读取
- **WHEN** 服务 A 需要服务 B 拥有的数据
- **THEN** A 调用 B 的内部批量接口或订阅 B 的事件，不直接查询 B 的表

### Requirement: 独立启动与故障隔离
每个业务服务 SHALL 在依赖的中间件可用时独立启动；单个非网关服务不可用 MUST NOT 阻止其他服务启动或处理其自身域的请求。

#### Scenario: 单服务重启
- **WHEN** 重启 al-comment 且其他服务保持运行
- **THEN** 评论接口短暂不可用，视频、用户、播放等接口不受影响

#### Scenario: 聚合服务降级
- **WHEN** al-datacenter 无法访问其依赖的域服务
- **THEN** 它返回明确的降级错误，不拖垮被调用服务

### Requirement: 后台任务归属
转码、计数聚合、点赞计数投递、播放进度落库等后台任务 MUST 只在其数据属主服务内运行，同一任务不得在多个服务重复执行。

#### Scenario: 任务唯一执行者
- **WHEN** 查看任一定时或异步任务的运行进程
- **THEN** 该任务只由拥有其目标表的服务执行，其他服务不注册同名任务

### Requirement: 部署形态约束
需要本地算力或长连接的服务 SHALL 在部署文档中声明副本与端口约束：转码服务单实例运行，WebSocket 服务使用固定端口并对客户端直连。

#### Scenario: 转码扩容保护
- **WHEN** 运维尝试把 al-video 扩容到多副本
- **THEN** 部署文档与健康检查明确提示转码队列为进程内单实例模型，需先改造才可扩容
