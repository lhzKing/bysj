# T25 模块清单

> 复杂度评级说明：Low / Medium / High

| 模块 | 当前位置 | 当前职责 | 主要依赖 | 复杂度 | T25 变更预期 |
| --- | --- | --- | --- | --- | --- |
| `PermissionService` | `backend/src/main/java/com/example/trace/security/PermissionService.java` | 权限查询、继承展开、API 匹配、缓存、角色解析、清缓存 | `SysPermissionMapper`、`SysRoleMapper`、`AntPathMatcher` | High | 主拆分对象，收敛为 façade |
| `PermissionInterceptor` | `backend/src/main/java/com/example/trace/security/PermissionInterceptor.java` | 注解权限检查、API fallback 检查 | `PermissionService`、`RequirePermission` | Medium | 保持行为不变，仅适配 façade / 新协作接口 |
| `LoginInterceptor` | `backend/src/main/java/com/example/trace/security/LoginInterceptor.java` | JWT 校验、tokenVersion 校验、角色解析写入 request | `JwtUtil`、`TokenStore`、`PermissionService`、`SysUserMapper` | Medium | 保持行为不变，关注 `getRoleIdByCode` 调用兼容性 |
| `AuthController` | `backend/src/main/java/com/example/trace/controller/AuthController.java` | 登录、注册、刷新 token、返回当前用户信息 | `SysUserMapper`、`SysRoleMapper`、`PermissionService` | Medium | 依赖 `getPermissionCodes`，需确保返回契约不变 |
| `TraceController` | `backend/src/main/java/com/example/trace/controller/TraceController.java` | 溯源创建、扫码流转、细粒度权限校验 | `TraceService`、`PermissionService` | Medium | 依赖 `hasPermission`，保持细粒度权限判定不变 |
| `RoleServiceImpl` | `backend/src/main/java/com/example/trace/service/impl/RoleServiceImpl.java` | 角色 CRUD、权限分配、清缓存 | `SysRoleMapper`、`SysPermissionMapper`、`SysRolePermissionMapper`、`PermissionService` | Medium | 继续作为缓存失效调用点 |
| `SysPermissionMapper` | `backend/src/main/java/com/example/trace/mapper/SysPermissionMapper.java` | 通过角色 ID / roleCode 查询权限列表 | MyBatis SQL | Low | 可能继续复用，必要时增加更清晰的查询命名 |
| `SysRoleMapper` | `backend/src/main/java/com/example/trace/mapper/SysRoleMapper.java` | 角色基础查询 | MyBatis-Plus BaseMapper | Low | 支撑 `roleCode -> roleId` 查询 |
| `RequirePermission` | `backend/src/main/java/com/example/trace/annotation/RequirePermission.java` | 声明式权限元数据 | Spring 注解扫描 | Low | 不应修改语义 |
| `PermissionServiceTest` | `backend/src/test/java/com/example/trace/security/PermissionServiceTest.java` | 当前只覆盖继承链路 | Mockito、JUnit 5 | Low | 需扩展为 T25 的主要回归基线 |

## 重点观察

### 1. 改动高风险中心

`PermissionService` 是唯一高复杂度中心类，几乎所有 T25 风险都围绕它展开。

### 2. 外围调用方特征

- 调用方数量不多，但都处在登录 / 鉴权 / 核心业务入口上
- 一旦行为漂移，影响会直接表现为：
  - 登录后权限列表错误
  - 接口误放行 / 误拒绝
  - 扫码动作权限错误
  - 角色调整后缓存未失效

### 3. 测试缺口

当前缺少以下自动化锁定：

- `hasPermission(roleId, String[], boolean)` 的 `matchAll` / `matchAny`
- `hasApiPermission(roleId, method, path)` 的 method/path 组合匹配
- 缓存命中与 `clearCache` 行为
- `getRoleIdByCode` 的空值 / miss / 命中场景

## 建议写入顺序

1. 先扩展 `PermissionServiceTest`
2. 再抽取协作类并保留 `PermissionService` 对外方法
3. 最后补外围调用点回归测试
