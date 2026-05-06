# R-P2-08 启动路径与异常场景测试覆盖清单

## 目标

把本轮审查中已经修复的 P0/P1 风险纳入可复跑验证，避免后续重构时出现启动路径、认证权限、异常处理和前后端契约回归。

## 本轮新增覆盖

| 风险点 | 覆盖文件 | 覆盖内容 |
|---|---|---|
| `/api/auth/register` 必须允许匿名访问 | `backend/src/test/java/com/example/trace/config/WebMvcSecurityPathContractTest.java` | `LoginInterceptor` 对 `/api/auth/register` 不匹配 |
| `/api/users` 必须受登录拦截器保护 | `backend/src/test/java/com/example/trace/config/WebMvcSecurityPathContractTest.java` | `LoginInterceptor` 对 `GET/POST /api/users` 匹配 |
| 权限拦截器必须在登录拦截器之后执行 | `backend/src/test/java/com/example/trace/config/WebMvcSecurityPathContractTest.java` | `WebMvcConfig` 注册顺序为 login → permission |
| `/api/auth/**` 不应被权限拦截器二次拦截 | `backend/src/test/java/com/example/trace/config/WebMvcSecurityPathContractTest.java` | `PermissionInterceptor` 对 `/api/auth/register`、`/api/auth/refresh` 不匹配 |
| `/api/traces/public-key` 必须保持匿名可访问 | `backend/src/test/java/com/example/trace/config/WebMvcSecurityPathContractTest.java` | 登录拦截器和权限拦截器均排除 public-key 路径 |

## 已有关键覆盖映射

| 建议覆盖项 | 已有覆盖文件 |
|---|---|
| prod 配置缺少 JWT secret 时 fail-fast | `ProdProfileConfigGuardTest` |
| prod 配置缺少外部签名密钥或使用默认密钥时 fail-fast | `ProdProfileConfigGuardTest`、`SignatureUtilTest` |
| Redis 异常时 Token 黑名单 fail-closed | `TokenStoreTest`、`LoginInterceptorTest`、`ControllerQueryBindingTest` |
| `/api/auth/register` 自助注册契约 | `AuthControllerTest`、`AuthServiceImplTest`、`frontend/src/shared/components/__tests__/Login.test.js`、`frontend/src/features/__tests__/api-contracts.test.js` |
| `/api/users` 管理入口与查询参数契约 | `ControllerQueryBindingTest`、`UserServiceImplTest`、`frontend/src/features/__tests__/api-contracts.test.js` |
| 扫码权限矩阵 | `TraceControllerTest`、`PermissionServiceTest`、`PermissionInheritanceResolverTest`、`frontend/src/features/trace/views/__tests__/ScanHub.permissions.test.js` |
| 非法 `eventTime` 返回 400 | `DateTimeUtilTest`、`ScanTraceRequestValidationTest`、`TraceScanTransactionServiceTest` |
| 删除已引用配件失败 | `PartServiceImplTest` |
| 地图 XSS 字段安全渲染 | `frontend/src/features/trace/components/__tests__/TraceRouteMap.info-window.test.js` |
| 前端 401 处理不直接依赖 router | `frontend/src/core/api/__tests__/request.test.js` |

## 复跑命令

```powershell
# 文本编码和乱码防回归
python tools/check_text_encoding.py
python tools/check_text_encoding.py --include-runtime

# 后端启动路径/异常场景相关测试
cd backend
mvn test "-Dtest=WebMvcSecurityPathContractTest,ProdProfileConfigGuardTest,TokenStoreTest,LoginInterceptorTest,ControllerQueryBindingTest,AuthControllerTest,TraceControllerTest,PartServiceImplTest,ScanTraceRequestValidationTest" -q
mvn test -q

# 前端契约与 UI 安全回归
cd frontend
npm run test:run
```
