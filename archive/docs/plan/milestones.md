# T25 里程碑

## M1：Baseline Locked

- 对应任务：T25-1.1、T25-1.2
- 达成标准：
  - 核心权限行为已被测试锁定
  - 拆分边界、类命名、调用兼容策略已明确

## M2：Read Paths Extracted

- 对应任务：T25-2.1、T25-2.2
- 达成标准：
  - 权限码读取/继承展开 与 API 权限读取/匹配 已拆为独立协作组件
  - `PermissionService` 对外行为仍保持兼容

## M3：Facade Integrated

- 对应任务：T25-3.1、T25-3.2
- 达成标准：
  - `PermissionService` 已收敛为 façade / 编排层
  - 登录、鉴权、角色授权变更调用点已完成适配

## M4：T25 Closed

- 对应任务：T25-4.1、T25-4.2
- 达成标准：
  - T25 相关测试通过
  - 任务表、MASTER、phase progress 文档已回写
  - 下一次对话可直接从文档续接
