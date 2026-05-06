# T25 项目分析总览

## 1. 任务背景

- 当前工作区：`d:\bysj`
- 当前整改主线：按 `项目整改执行任务表.md` 继续推进非 UI 逻辑任务
- 已完成前置任务：T24 `优化 UserServiceImpl / RoleServiceImpl 查询模式`
- 当前目标任务：T25 `拆分 PermissionService 职责`

## 2. 项目结构与技术栈

| 目录 | 技术栈 | 说明 |
| --- | --- | --- |
| `backend/` | Spring Boot 3.2.2、MyBatis-Plus 3.5.7、MySQL、Redis、JWT | 主要整改实施区域 |
| `frontend/` | Vue 3、Vite 5、Vitest、PrimeVue / Element Plus 混用 | 当前默认冻结界面文件 |
| `docs/` | 旧 `superpowers` 文档 + 新 spec-driven 文档 | 本次会新增 `analysis/plan/progress/specs` |

后端构建入口：

- `backend/pom.xml`
- Java 版本：19
- 重点测试命令：`cd backend && mvn test`

前端构建入口（本任务仅作背景）：

- `frontend/package.json`
- 重点命令：`cd frontend && npm run build`

## 3. T25 相关入口与调用链

### 3.1 权限读取与鉴权调用链

1. `LoginInterceptor`
   - 从 JWT 解析 `roleCode`
   - 调用 `permissionService.getRoleIdByCode(roleCode)` 写入 request attribute
2. `PermissionInterceptor`
   - 优先检查 `@RequirePermission`
   - 无注解时回退到 `permissionService.hasApiPermission(roleId, method, path)`
3. `AuthController`
   - 登录与 `/me` 接口通过 `permissionService.getPermissionCodes(roleId)` 返回权限集合
4. `TraceController`
   - 通过 `permissionService.hasPermission(roleId, requiredPerm)` 做细粒度动作权限判定
5. `RoleServiceImpl`
   - 在 `assignPermissions` / `deleteRole` 后调用 `permissionService.clearCache()`

### 3.2 当前核心类

- `backend/src/main/java/com/example/trace/security/PermissionService.java`

当前类中同时包含：

- `getPermissionCodes(Long roleId)`
- `getApiPermissions(Long roleId)`
- `hasPermission(...)`
- `hasApiPermission(...)`
- `getRoleIdByCode(String roleCode)`
- `clearCache()` / `clearCache(Long roleId)`
- 内部静态继承规则与 API path 匹配实现

## 4. 当前实现特征

### 优点

- 对外 API 简单，调用方较少改动即可使用
- 已有本地缓存，基本避免同会话重复查库
- 权限继承链支持传递展开

### 缺点

- 查询、匹配、缓存、规则、角色解析混在一个 Service 中
- 测试覆盖偏薄，仅锁定继承展开
- `PermissionInterceptor` 的“注解优先 / API fallback”行为与匹配器逻辑紧耦合
- 缓存失效调用点分散，缺少显式行为约束

## 5. T25 建议拆分方向

建议拆成“薄 façade + 明确协作组件”：

1. **角色权限码读取器**
   - 负责从 mapper 加载角色权限码
2. **权限继承展开器**
   - 负责继承规则与传递闭包展开
3. **角色 API 权限读取器**
   - 负责按角色加载 API 权限条目
4. **API 权限匹配器**
   - 负责 method/path 匹配
5. **角色解析器**
   - 负责 `roleCode -> roleId`
6. **缓存协调层**
   - 负责 permission/apiPermission 缓存生命周期
7. **PermissionService façade**
   - 仅保留外部兼容方法，委派给上述组件

## 6. 构建与验证基线

建议优先使用的命令：

- `cd backend && mvn test -Dtest=PermissionServiceTest`
- `cd backend && mvn test -Dtest=PermissionServiceTest,AuthControllerTest`
- 如新增拦截器测试，再扩展到对应测试类

## 7. 当前结论

T25 不应直接“大改 PermissionService”；正确路径应为：

1. 先补行为测试
2. 明确拆分边界
3. 抽取协作组件
4. 再把 `PermissionService` 收敛为 façade
5. 最后回归调用方与缓存失效点
