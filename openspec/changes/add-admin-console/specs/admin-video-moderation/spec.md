# Spec Delta

## Purpose

让管理员按逻辑状态管理视频稿件（审核、下架、可见性），同时明确不触碰 MinIO 对象和转码链路。

## ADDED Requirements

### Requirement: 稿件分页与筛选
管理端 MUST 支持按状态、作者 uid、标题关键字分页查询稿件，并默认排除 `status=3`（已删除）的记录。

#### Scenario: 按审核状态筛选
- **WHEN** 管理员筛选 `status=0`
- **THEN** 只返回审核中的稿件，分页信息与总数正确

#### Scenario: 查看已删除稿件
- **WHEN** 管理员显式选择 `status=3`
- **THEN** 返回逻辑删除的稿件，普通列表默认不包含它们

### Requirement: 审核与下架状态流转
稿件状态 MUST 按允许的方向流转：`0→1/2`（通过/驳回）、`1/2→3`（下架/逻辑删除）；其他方向 MUST 返回业务错误且不修改数据。

#### Scenario: 审核通过
- **WHEN** 管理员对 `status=0` 的稿件执行通过
- **THEN** `video.status=1`，记录操作日志

#### Scenario: 非法重复审核
- **WHEN** 管理员对 `status=1` 的稿件再次执行通过
- **THEN** 返回冲突错误，状态保持 1

#### Scenario: 下架已过审稿件
- **WHEN** 管理员下架 `status=1` 的稿件
- **THEN** `video.status=3` 且写入 `delete_date`

### Requirement: 可见性管理
管理员 MUST 能把稿件 `visible` 设为 0（公开）或 1（仅自己可见）；只允许这两个值。

#### Scenario: 改为仅自己可见
- **WHEN** 管理员把某稿件 visible 设为 1
- **THEN** 公开列表不再返回该稿件，作者仍可在自己的稿件中看到

### Requirement: 逻辑管理边界
稿件管理 MUST NOT 删除或修改 MinIO 对象、MUST NOT 修改 `transcode_status`/`hls_url`、MUST NOT 修改标题/简介/标签等正文内容。

#### Scenario: 下架稿件
- **WHEN** 管理员下架稿件
- **THEN** 只更新 `video.status`/`delete_date`，MinIO 中的视频、封面与 HLS 对象保持不变
