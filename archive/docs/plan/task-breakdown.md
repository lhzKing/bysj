# T25 任务拆解

## 目标

在不触碰前端界面文件的前提下，完成 `PermissionService` 职责拆分，并通过测试锁定权限主链路行为。

## Phase 1：Baseline & Safety Net

### Lane 1（顺序执行，Merge Risk: Low）

#### T25-1.1 扩展权限服务基线测试

- 优先级：P0
- 工作量：M
- 依赖：无
- 说明：扩展 `PermissionServiceTest`，必要时新增关联测试，锁定拆分前的核心行为。
- 验收标准：
  - 覆盖权限继承传递链
  - 覆盖 `matchAll=false` 与 `matchAll=true`
  - 覆盖 API method/path/wildcard 匹配
  - 覆盖缓存命中与 `clearCache`
  - 覆盖 `getRoleIdByCode` 的命中 / miss / null

#### T25-1.2 确认拆分边界与命名骨架

- 优先级：P0
- 工作量：S
- 依赖：T25-1.1
- 说明：确定拆分后的协作组件、包路径、保留的 façade 方法与调用边界。
- 验收标准：
  - 文档与代码命名一致
  - `PermissionService` 仍保留现有对外入口
  - 新类职责互不重叠

## Phase 2：Read Path Split

### Lane A（可并行，Merge Risk: Medium）

#### T25-2.1 抽取权限码读取与继承展开

- 优先级：P0
- 工作量：M
- 依赖：T25-1.2
- 说明：把“按角色加载权限码 + 继承展开”从 `PermissionService` 中拆出。
- 验收标准：
  - 形成独立协作类
  - `getPermissionCodes` 行为不变
  - 新类可被单独测试

### Lane B（可并行，Merge Risk: Medium）

#### T25-2.2 抽取 API 权限读取与匹配器

- 优先级：P0
- 工作量：M
- 依赖：T25-1.2
- 说明：把“按角色加载 API 权限 + method/path 匹配”从 `PermissionService` 中拆出。
- 验收标准：
  - 形成独立 API 权限读取器与匹配器
  - `hasApiPermission` 行为不变
  - wildcard 与 method 判定可独立测试

## Phase 3：Facade Integration

### Lane 1（顺序执行，Merge Risk: Medium）

#### T25-3.1 将 `PermissionService` 收敛为 façade

- 优先级：P0
- 工作量：M
- 依赖：T25-2.1、T25-2.2
- 说明：保留兼容的外部方法，把内部实现委派给新协作组件，并收拢缓存管理。
- 验收标准：
  - `PermissionService` 仅承担编排 / 缓存 / 兼容接口
  - 查询、继承、匹配逻辑不再堆叠在单类中

#### T25-3.2 对齐调用方与缓存失效点

- 优先级：P0
- 工作量：S
- 依赖：T25-3.1
- 说明：验证 `LoginInterceptor`、`PermissionInterceptor`、`AuthController`、`TraceController`、`RoleServiceImpl` 的调用仍正确。
- 验收标准：
  - 现有调用方无需感知内部拆分
  - 角色权限变更后缓存清理入口仍可用

## Phase 4：Verification & Closeout

### Lane 1（顺序执行，Merge Risk: Low）

#### T25-4.1 执行聚焦回归验证

- 优先级：P0
- 工作量：S
- 依赖：T25-3.2
- 说明：运行 T25 涉及的后端测试，必要时补做组合回归。
- 验收标准：
  - T25 相关测试通过
  - 无新增前端改动
  - 无已知权限主链路回归

#### T25-4.2 文档收口并回写任务表

- 优先级：P0
- 工作量：S
- 依赖：T25-4.1
- 说明：更新 progress 文档与任务表，把 T25 标记为 DONE。
- 验收标准：
  - `docs/progress/MASTER.md` 与 phase 文件同步
  - `项目整改执行任务表.md` 状态、摘要、更新记录同步
  - 可以在下一次对话中直接续接

## 并行执行说明

- 当前平台未获得用户显式授权使用子代理并行开发，因此实际执行默认按顺序推进。
- 文档仍保留 Lane 划分，便于未来在用户明确授权后做受控并行。
