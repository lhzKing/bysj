# T25 拆分 `PermissionService` 职责

- 任务编号：T25
- 创建时间：2026-04-14 02:53
- 当前状态：DOING
- 任务来源：`项目整改执行任务表.md`

## 1. 任务定义

在 T24 完成用户/角色查询治理后，当前项目的下一项非 UI 逻辑任务为 **T25 拆分 `PermissionService` 职责**。  
本任务采用 **Spec-Driven Develop** 流程推进，旧的 `docs/superpowers/*` 文档仅作为历史参考，不再作为当前实现流程的主线。

## 2. 目标

1. 将当前 `PermissionService` 中混合的职责拆开，至少明确分离：
   - 角色权限查询
   - 权限继承展开
   - API 权限匹配
   - 缓存管理
   - 角色编码解析
2. 保持现有外部调用语义稳定，避免扩大到前端 UI 或无关链路。
3. 先补齐自动化测试，再进行服务拆分与调用方接线。
4. 保证后续对话中断后，可仅通过 `docs/progress/MASTER.md` 与本任务文档继续。

## 3. 范围

### In Scope

- `backend/src/main/java/com/example/trace/security/PermissionService.java`
- `backend/src/main/java/com/example/trace/security/PermissionInterceptor.java`
- `backend/src/main/java/com/example/trace/security/LoginInterceptor.java`
- `backend/src/main/java/com/example/trace/controller/AuthController.java`
- `backend/src/main/java/com/example/trace/controller/TraceController.java`
- `backend/src/main/java/com/example/trace/service/impl/RoleServiceImpl.java`
- `backend/src/main/java/com/example/trace/mapper/SysPermissionMapper.java`
- `backend/src/main/java/com/example/trace/mapper/SysRoleMapper.java`
- `backend/src/test/java/com/example/trace/security/*`

### Out of Scope

- 任意前端界面文件（views / components）
- UI / 视觉 / 风格统一改造
- 权限模型本身的大范围重设计
- 数据库 schema 调整
- 非权限主链路的顺手重构

## 4. 当前问题概览

当前 `PermissionService` 同时承担：

- 权限码加载与继承展开
- API 权限列表加载
- API method/path 匹配
- 本地缓存
- `roleCode -> roleId` 查询

带来的直接问题：

1. 单类膨胀，读写责任耦合。
2. 缓存与查询逻辑混在一起，难以分别测试。
3. `LoginInterceptor`、`PermissionInterceptor`、`AuthController`、`TraceController`、`RoleServiceImpl` 都依赖该类，改动时回归面较大。
4. 当前测试只锁定了继承链路，覆盖面不足。

## 5. 约束

- 继续遵守任务表中的“前端界面文件默认冻结”。
- 不再沿用旧 superpowers 流程继续扩写 T25 文档。
- 外部暴露方法优先保持兼容，除非在计划文档中明确说明替换路径。
- 每完成一个阶段或子任务，都必须同步更新：
  - `docs/progress/MASTER.md`
  - 对应 phase progress 文件
  - `项目整改执行任务表.md`

## 6. 初步验收标准

- `PermissionService` 降为较薄 façade 或编排层，不再直接承载全部权限职责。
- 权限继承、API 匹配、查询与缓存管理均有可独立理解/测试的协作组件。
- 至少补齐以下回归验证：
  - 权限继承传递
  - `matchAll` / `matchAny` 判定
  - API method/path 匹配
  - 缓存命中与清理
  - `roleCode -> roleId` 解析
  - 角色权限变更后的缓存失效调用点
- 不修改前端界面文件。
