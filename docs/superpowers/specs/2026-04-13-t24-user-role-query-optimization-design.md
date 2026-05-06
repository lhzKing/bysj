# T24 用户/角色列表查询模式优化设计

**日期**：2026-04-13  
**任务编号**：T24  
**任务名称**：优化 `UserServiceImpl` / `RoleServiceImpl` 查询模式

---

## 1. 目标

在**不做 UI/视觉改造**、且只在用户特别授权范围内修改极少量前端契约的前提下，消除以下两个列表链路中的 N+1 查询问题：

1. `UserServiceImpl.listUsers()`
2. `RoleServiceImpl.listRoles()`

本任务的目标不是“统一风格”或“重构整个用户/角色模块”，而是：

- 降低列表查询的数据库往返次数
- 保持现有业务语义稳定
- 把角色列表接口从“过重列表接口”收敛为“轻量列表接口”
- 仅在必要范围内调整前端角色列表数据契约

---

## 2. 当前问题

## 2.1 `UserServiceImpl.listUsers()` 的 N+1

当前流程：

1. 先分页查询 `sys_user`
2. 将结果逐条传入 `convertToResponse()`
3. `convertToResponse()` 内部再次调用 `roleMapper.selectById(user.getRoleId())`

结果：

- 1 次用户分页查询
- + N 次角色查询

当单页用户数量增加时，数据库查询次数线性增长，属于典型 N+1。

## 2.2 `RoleServiceImpl.listRoles()` 的 N+1

当前流程：

1. 先查询全部 `sys_role`
2. 对每个角色执行 `permissionMapper.selectByRoleId(role.getId())`
3. 把完整权限列表塞入 `RoleResponse.permissions`

结果：

- 1 次角色查询
- + N 次角色权限查询

这同样是典型 N+1。

## 2.3 角色列表接口语义偏重

额外盘点后发现，当前 `GET /api/roles` 列表接口不仅返回角色基础信息，还返回每个角色的完整 `permissions`。

但角色列表页当前实际主要只消费：

- 角色基础信息
- 权限数量

完整权限明细只在“权限配置”场景中才真正需要。

这意味着当前列表接口存在两个问题：

1. 查询实现重
2. 接口语义也偏重

因此，如果只做“后端批量查询但仍返回完整 permissions”，虽然能治 N+1，但不能解决“列表接口过重”的设计问题。

---

## 3. 设计结论

T24 采用以下方案：

> **用户列表保持契约不变 + 角色列表改为轻量列表 + 角色详情按需返回完整权限**

这是在当前约束下，收益最高且改动面仍可控的方案。

---

## 4. 方案说明

## 4.1 用户列表：契约保持不变

接口：

- `GET /api/users`

策略：

- 前端不改
- Controller / DTO 契约不改
- 仅优化 `UserServiceImpl.listUsers()` 内部查询实现

优化方式：

1. 分页查出用户列表
2. 收集本页所有非空 `roleId`
3. 使用批量角色查询一次取回所有角色
4. 在内存中构建 `Map<Long, SysRole>`
5. 批量组装 `UserResponse`

优化后查询模式：

- 1 次用户分页查询
- 1 次角色批量查询

而不是：

- 1 次用户分页查询
- N 次角色逐条查询

## 4.2 角色列表：契约轻量化

接口：

- `GET /api/roles`

设计调整：

- 列表接口不再返回完整 `permissions`
- 改为返回角色基础信息 + `permissionCount`

即：

- `id`
- `roleCode`
- `roleName`
- `remark`
- `createTime`
- `permissionCount`

不再要求 `listRoles()` 对每个角色都携带完整权限集合。

## 4.3 角色详情：继续承担完整权限语义

接口：

- `GET /api/roles/{id}`

保持现状：

- 返回完整 `permissions`

这意味着：

- 列表页只拿轻量数据
- 点击“权限配置”或需要详情时，再单独请求 `getRole(id)` 获取完整权限明细

这样能把“列表”和“详情”职责重新对齐。

---

## 5. 后端设计

## 5.1 `UserServiceImpl.listUsers()`

### 改造原则

- 不改变返回结构
- 不改变过滤逻辑
- 不改变排序/分页逻辑
- 只消除逐条角色查询

### 拟新增/调整

- 使用 `roleMapper.selectBatchIds(roleIds)` 或等价批量查询方式
- 在 service 内新增批量转换路径，而不是复用会触发逐条查询的旧 `convertToResponse()`

### 推荐实现形态

- 保留现有 `convertToResponseWithRole(SysUser user)` 供详情链路复用
- 新增面向列表批量映射的辅助方法，例如：
  - `convertToResponse(SysUser user, SysRole role)`
  - 或 `buildUserResponses(List<SysUser> users, Map<Long, SysRole> roleMap)`

关键点：

- `listUsers()` 不再调用会触发 `roleMapper.selectById()` 的逻辑

## 5.2 `RoleServiceImpl.listRoles()`

### 改造原则

- 保持角色列表页所需信息不变
- 去掉列表接口上的完整权限载荷
- 把权限相关查询从“逐角色查明细”改为“批量统计”

### 推荐实现

新增一个按角色分组统计权限数量的查询能力，例如：

- 在 `SysRolePermissionMapper` 中新增按 `roleIds` 聚合统计的方法

返回语义类似：

- `roleId -> permissionCount`

随后在 `RoleServiceImpl.listRoles()` 中：

1. 一次性查询所有角色
2. 一次性查询所有这些角色的权限数
3. 用 `Map<Long, Integer>` 回填到 `RoleResponse.permissionCount`

而不再逐条查询完整 `permissions`

## 5.3 `RoleResponse`

为了控制改动范围，本轮不新建新的列表 DTO。

采取最小变更策略：

- 在现有 `RoleResponse` 上新增 `permissionCount`

并约定：

- 列表接口填充 `permissionCount`
- 详情接口填充 `permissions`

这样既不需要引入新 DTO，也能完成语义收口。

---

## 6. 前端设计（仅限本任务特批范围）

## 6.1 允许改动的前端文件

本轮前端改动严格限制在角色链路：

- `frontend/src/features/user/api/roles.js`
- `frontend/src/features/user/views/RoleList.vue`

不扩散到其他页面或组件。

## 6.2 `roles.js`

调整内容：

- 更新 JSDoc / 契约说明
- 明确：
  - `getRoles()` 返回轻量角色列表
  - `getRole(id)` 返回完整角色详情（含 `permissions`）

## 6.3 `RoleList.vue`

调整内容：

### 列表展示

- 权限数量显示由：
  - `role.permissions?.length || 0`
- 改为：
  - `role.permissionCount || 0`

### 权限配置弹窗

当前做法：

- 直接依赖列表接口返回的 `role.permissions`

调整后：

1. 点击“权限配置”
2. 调用 `getRole(role.id)`
3. 用详情接口返回的完整 `permissions` 初始化 `selectedPermissions`

这样角色列表页仍能正常工作，但不会再要求列表接口携带完整权限明细。

---

## 7. 测试策略

## 7.1 后端测试

新增/补充 service 层测试，至少覆盖：

### A. `UserServiceImpl.listUsers()`

验证：

- 返回结果中的角色信息仍正确
- 不再对每个用户逐条调用 `roleMapper.selectById()`
- 角色查询改为批量方式

### B. `RoleServiceImpl.listRoles()`

验证：

- 返回结果包含正确的 `permissionCount`
- 列表接口不再依赖逐条 `permissionMapper.selectByRoleId()`
- `getRoleById()` 仍然能够返回完整 `permissions`

## 7.2 前端测试

新增或补充角色列表链路测试，至少覆盖：

- `getRoles()` 轻量列表契约说明
- `RoleList.vue` 使用 `permissionCount` 展示权限数量
- 点击权限配置时，会额外调用 `getRole(id)` 获取完整权限详情

---

## 8. 风险与控制

## 8.1 风险：角色列表前端依赖旧字段

风险：

- 角色列表页之前直接依赖 `role.permissions`

控制：

- 本轮允许在 `RoleList.vue` 上做最小前端契约调整
- 用自动化测试锁定“列表显示走 `permissionCount`，详情弹窗走 `getRole(id)`”

## 8.2 风险：范围扩散到风格统一

风险：

- 很容易顺手继续统一 mapper 风格、DTO 风格、service 风格

控制：

- 本轮明确不做 T26
- 只做查询模式收口与必要契约调整

## 8.3 风险：误伤详情接口语义

风险：

- 若误把 `getRoleById()` 也改成轻量返回，会破坏权限配置链路

控制：

- 明确区分：
  - `listRoles()`：轻量
  - `getRoleById()`：完整

---

## 9. 非目标

T24 本轮明确不做：

- 不统一 mapper 风格
- 不重构 `getUserById()`
- 不重构 `getRoleById()` 的整体结构
- 不扩展到 `PermissionService`
- 不修改其他前端页面
- 不做 UI 改造

---

## 10. 完成标准

T24 完成时必须满足：

- `UserServiceImpl.listUsers()` 消除角色查询 N+1
- `RoleServiceImpl.listRoles()` 消除权限查询 N+1
- `GET /api/users` 契约保持不变
- `GET /api/roles` 调整为轻量列表契约，返回 `permissionCount`
- `GET /api/roles/{id}` 继续返回完整 `permissions`
- `RoleList.vue` 改为按需加载角色权限详情
- 相关后端/前端测试通过
- 不引入 UI 改动

