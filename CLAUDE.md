# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概览

工业零配件供应链溯源系统。后端 Spring Boot 3 + MyBatis-Plus（`backend/`），前端 Vue 3 + Vite（`frontend/`）。核心特色是**哈希链 + RSA 数字签名 + 乐观锁重试**三重保证的溯源不可篡改。

## 常用命令

### 后端（`backend/`，JDK 19+、Maven 3.9+）

```bash
mvn spring-boot:run                                      # 启动（http://localhost:8080）
mvn test                                                 # 运行全部测试
mvn test -Dtest=TraceServiceImplTest                     # 运行单个测试类
mvn test -Dtest=TraceServiceImplTest#methodName         # 运行单个测试方法
mvn package -DskipTests                                  # 打包
```

依赖 MySQL 8+ 与 Redis 6+；启动前需建库并执行 `backend/sql/init_schema.sql`，可选 `sample_data_full.sql`。

### 前端（`frontend/`，Node 18+）

```bash
npm install
npm run dev          # 默认 HTTPS（摄像头扫码要求）→ https://localhost:5173
npm run build
npm run test         # Vitest watch
npm run test:run     # Vitest 单次
npx vitest run src/features/__tests__/api-contracts.test.js   # 单文件
```

首次运行 HTTPS dev server 前需先生成自签证书：`node generate-cert.js` 或 `generate-cert.ps1`，输出到 `frontend/certs/`。要走 HTTP，设 `VITE_DEV_HTTPS=false`。Vite 已配置 `/api` 代理到 `http://localhost:8080`。

### 生成演示数据

需先登录拿 Token（默认 `superadmin / superadmin123456`），然后：

```bash
curl -X POST "http://localhost:8080/api/admin/generate-sample-data?count=500" \
  -H "Authorization: Bearer <token>"
```

## 架构核心

### 后端分层（`backend/src/main/java/com/example/trace/`）

请求链路：`LoginInterceptor` → `PermissionInterceptor` → `Controller` → `Service` → `Mapper`。

- **`security/`**：自定义 JWT 鉴权，不用 Spring Security。`JwtUtil` 生成/解析；`TokenStore`（Redis）管理黑名单与 `token_version`；`PermissionService` + `permission/ApiPermissionMatcher` 做 RBAC。密码变更/角色变更通过递增 DB 中 `token_version`，使已发 Token 下次请求即失效。
  - **注册路径**：登录页自助注册调用 `POST /api/auth/register`（`AuthController` → `AuthServiceImpl.register()`），仅接受 `username/password`，默认绑定 `USER` 角色 + `status=1`，roleId 由后端从 `sys_role` 查 `role_code='USER'` 决定，**不**接受前端传入的 `roleId/status`；后台管理员另走 `POST /api/users`（受 `user:manage` 保护，可指定任意 roleId）。
- **`common/ApiResponse`** + **`config/JacksonConfig`**：统一响应格式 `{code,status,message,data}`；请求体接受 `camelCase` 或 `snake_case`，响应统一 `snake_case`。全局异常由 `common/GlobalExceptionHandler` 兜底。
- **`annotation/RequirePermission`**：方法/类级注解，被 `PermissionInterceptor` 扫描匹配用户权限。
- **`service/impl/support/`**：`TraceServiceImpl` 的拆分件——`TraceScanRetryExecutor`（无事务的外层重试）调用 `TraceScanTransactionService`（`REQUIRES_NEW` 原子单元）；`TraceChainVerifyService` 做链验；`TraceLogFactory` 组装日志+哈希+签名。

### 扫码流转（并发关键路径）

```
scan(外层，无 @Transactional) ── while retry≤3
  └─ doScanOnce(@Transactional(REQUIRES_NEW))
       1. SELECT snapshot (读 version)
       2. hash = SHA256(data || prevHash)
       3. signature = RSA_SIGN(私钥, data)
       4. INSERT trace_lifecycle_log
       5. UPDATE trace_snapshot ... WHERE version=?  (MyBatis-Plus @Version)
       6. 影响行数=0 → 抛 TraceOptimisticLockException → 事务回滚
  ↳ 捕获后退避 50ms*retry 重试
```

**重要**：重试不能放在同一事务内——DB 需要旧事务回滚后才能看到其他事务的新 `version`。所以外层方法不能标 `@Transactional`，内层必须 `REQUIRES_NEW`。

### 验链

`GET /api/traces/{code}/verify` 遍历 `trace_lifecycle_log`，逐条检查：(1) `prevHash` 与上一条 `currentHash` 链式一致；(2) 重算 hash 对比存储值；(3) 公钥验签。任一失败返回 `valid=false`。公钥可通过 `GET /api/traces/public-key`（无需认证）获取以供第三方自验。

### RBAC 与角色优先级

6 个预置角色（`SUPER_ADMIN`=3，`ADMIN`=2，业务角色=1）。规则：
- 用户只能操作优先级**低于**自己的用户；
- `ADMIN` 看不到 `SUPER_ADMIN` 和其他 `ADMIN`；
- `superadmin` 账号不可删不可禁。
权限可在后台动态配置；`xxx:manage` 自动继承 `xxx:view`（由 `PermissionInheritanceResolver` 实现）。

### 前端结构（`frontend/src/`）

采用 **feature-based** 组织：
- `core/`：全局基础设施——`api/request.js`（axios 实例，拦截器处理 Token 与 `code!==0`）、`router/`（路由守卫读 `meta.permissions`）、`stores/user.js`（Pinia）。
- `features/{trace,dashboard,user,part}/`：每个 feature 内部独立划分 `api/`、`components/`、`composables/`、`views/`。
- `shared/`：跨 feature 复用的 `components/`、`constants/`（包括 `PERMISSIONS` 常量）、`theme/`。
路径别名 `@` → `src`。UI 用 PrimeVue 4 + Tailwind；地图 ECharts + 高德；扫码 `vue-qrcode-reader`。

## 关键约定

- **字段命名**：Java 实体 `camelCase`，DB/JSON 响应 `snake_case`，由 `JacksonConfig` + MyBatis-Plus `map-underscore-to-camel-case`（在 `application.yml` 显式声明 `mybatis-plus.configuration.map-underscore-to-camel-case=true`，避免依赖上游默认值兜底）双向映射。写请求 DTO 时两种风格都要支持（Jackson 已配置 `PropertyNamingStrategies.SNAKE_CASE` + 其它 naming alias）。
- **时间**：ISO-8601（`2026-01-16T10:30:00`）；`entity/` 用 `LocalDateTime`，`MybatisMetaObjectHandler` 自动填 `create_time`/`update_time`。
- **乐观锁**：在需要并发保护的实体上用 `@Version`（如 `TraceSnapshot`）；由 `MybatisPlusConfig` 注册的 `OptimisticLockerInnerInterceptor` 生效。写该类逻辑时务必参考现有「外层重试 + REQUIRES_NEW」模式，不要在单事务内 while-retry。
- **CORS**：`config/CorsFilter` + `CorsProperties`（`cors.allowed-origin-patterns` 支持 `192.168.*` 局域网通配，为手机扫码测试留口子）。
- **RSA 密钥**：`application-dev.yml` 与 `application-test.yml` 默认 `auto-generate=true` 且 path 为空——开箱即用，跨平台（Windows / macOS / Linux 任何机器都能直接 `mvn spring-boot:run -Dspring-boot.run.profiles=dev` 启动），代价是每次重启 key 变化、旧签名失效。要稳定 key，通过 `TRACE_SIGNATURE_PRIVATE_KEY_PATH` / `TRACE_SIGNATURE_PUBLIC_KEY_PATH` 指向外部文件 + `TRACE_SIGNATURE_AUTO_GENERATE=false`。`application-prod.yml` 已**清空**默认值且 `ProdProfileConfigGuard` 会显式拒绝指向 `backend/keys/*.pem` / 仓库工作区路径或 `auto-generate=true`，强制 prod 必须挂载外部密钥。详见 `application-{dev,test,prod}.yml` 与 `config/ProdProfileConfigGuard.java`。

## 配置与环境

- 后端全部可通过 `TRACE_*` 环境变量覆盖（见 `backend/.env.example`），包括 DB、Redis、JWT secret、CORS、签名密钥路径。
- 前端见 `frontend/.env.example`：HTTPS 开关、证书路径、`VITE_API_PROXY_TARGET`、高德 `VITE_AMAP_KEY`。
- 日志收紧情况：`application.yml` **不**默认开启 MyBatis stdout SQL 日志，仅 `application-dev.yml` 与 `application-test.yml` 通过 `mybatis-plus.configuration.log-impl: StdOutImpl` 启用，prod profile 不会输出明文 SQL。安全日志级别基线 `INFO`，dev/test 为 `DEBUG`，`application-prod.yml` 强制 `WARN`。

## 测试

- 后端：JUnit 5 + Spring Boot Test（`backend/src/test/java/...`）。覆盖 Controller 契约、拦截器、权限匹配、乐观锁重试、链验证。
- 前端：Vitest + `@vue/test-utils`（`setupFiles: src/test/setup.js`）。`src/features/__tests__/api-contracts.test.js` 用于前后端字段契约校验。

## 文档交叉引用

- `README.md`：业务特性全景、默认账号、Redis 黑名单原理、RSA 密钥管理、**"严格代码审查"提示词与流程**（包含本仓库当前已识别的高优先级审查入口清单）。
- `api-doc.md`：API 详细字段表。
- `postman-guide.md` + `postman/*.json`：Postman 测试集。
- `CAMERA_SCAN_GUIDE.md`：摄像头扫码（HTTPS + 证书）排障。
- `backend/README.md`：后端单独的启动细节与常见问题。

修改 API 行为或字段时，同步检查 `README.md`、`api-doc.md`、`postman/` 集合与前端 `features/*/api/` —— 该项目有较明显的「文档漂移」风险，是 README 中审查清单里的条目之一。

## 已修复点参考（避免重复审查）

> 下列历史问题已修复，**重新审查时不必再次列入 P0/P1**（除非有新证据推翻）。完整证据与最新一轮（2026-05-03）审查发现见仓库根 `项目审查记录_20260503.md` 与配套整改任务表 `项目审查整改任务表_20260503.md`。

| 历史问题 | 已修复证据 |
|---|---|
| 自助注册曾走 `POST /api/users` 硬编码 `roleId/status` | 已改为 `POST /api/auth/register`，DTO 仅含 `username/password`；roleId 后端从 `sys_role.role_code='USER'` 查得，`status=1` 写死。证据：`frontend/src/core/api/auth.js:25`、`backend/src/main/java/com/example/trace/service/impl/AuthServiceImpl.java:78` |
| 签名私钥与仓库一起提交、默认指向 `d:/bysj/backend/keys/...` | `backend/keys/*.pem` 已从工作区移除；dev/test 默认指向 `D:/trace-runtime/keys/...`（运行时目录）；`application-prod.yml` 默认值已清空；`ProdProfileConfigGuard.isWorkspaceDefaultKeyPath` 显式拒绝 `backend/keys` 路径。证据：`backend/src/main/java/com/example/trace/config/ProdProfileConfigGuard.java:89-115` |
| `application.yml` 默认开启 stdout SQL 日志 + `DEBUG` 级安全日志 | 基础 `application.yml` 不再设置 `log-impl`；仅 `application-dev.yml` / `application-test.yml` 启用 stdout SQL；`application-prod.yml` 强制 `TRACE_SECURITY_LOG_LEVEL=WARN`。证据：`backend/src/main/resources/application*.yml` |
| 配件删除不校验业务引用 | 已加 `trace_snapshot.spu_id` / `trace_lifecycle_log.spu_id` 引用检查，已参与溯源的 SPU 返回 409。证据：`backend/src/main/java/com/example/trace/service/impl/PartServiceImpl.java`、`backend/sql/migrate_v6_part_reference_constraints.sql` |
| Token 黑名单 Redis 故障 fail-open | 已 fail-closed：`TokenStoreException` 由 `LoginInterceptor` 转 503。证据：`backend/src/main/java/com/example/trace/security/TokenStore.java`、`backend/src/main/java/com/example/trace/security/LoginInterceptor.java` |
| `ProdProfileConfigGuard` 不校验 JWT secret 强度与 DB 凭据缺省 | 已 fail-fast：禁用默认 secret、要求 ≥32 字节、root/root 拒绝、签名 key 路径必须外置。证据：`backend/src/main/java/com/example/trace/config/ProdProfileConfigGuard.java:34-108` |
| ⚠ 前端 JWT 存于 `localStorage`，对 XSS 不抗（**已知风险，已文档化为补偿模式**） | 现状已落地：短 Token（2h/1d）+ Redis 黑名单 fail-closed + `token_version` 立即失效 + 存储适配层 + `frontend/index.html` CSP meta + 不用 v-html/innerHTML（InfoWindow 走 DOM API）。完整决策与"后续 httpOnly Cookie + CSRF 迁移路径"见 `docs/security/token-storage-and-csp.md`；README "前端 Token 存储威胁模型"段提供面向审阅者的总览。 |

> 仍在跟踪的待修复项见 `项目审查整改任务表_20260503.md` 的"五、当前任务状态总览"。

