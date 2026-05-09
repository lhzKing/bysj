# 工业零配件供应链溯源系统

## 项目简介

基于 Spring Boot + Vue 3 的工业零配件供应链全链路溯源系统，支持批次赋码、一物一码、扫码激活、任务驱动出入库/接收、箱码/托盘码批量流转、溯源查询、哈希链防篡改、异常冻结与红冲蓝补纠错等核心功能。

## 目录结构

```
├── backend/        # 后端（Spring Boot 3 + MyBatis-Plus）
├── frontend/       # 前端（Vue 3 + Vite）
├── api-doc.md      # API 接口文档
├── postman-guide.md # Postman 测试指导
└── china.json      # 中国地图数据（ECharts 用）
```

## 后端架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              客户端请求                                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                     ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Spring Boot 应用                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                      拦截器层 (security/)                            │    │
│  │  LoginInterceptor → PermissionInterceptor → Controller              │    │
│  │  • JWT 解析验证     • RBAC 权限校验       • 业务处理                  │    │
│  │  • Token 黑名单     • @RequirePermission                            │    │
│  │  • 用户状态检查                                                       │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                    ↓                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                      控制器层 (controller/)                          │    │
│  │  AuthController │ TraceController │ UserController │ ...            │    │
│  │  • 参数校验 (@Valid)                                                  │    │
│  │  • 调用 Service                                                       │    │
│  │  • 统一响应封装 (ApiResponse)                                         │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                    ↓                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                      服务层 (service/)                               │    │
│  │  TraceService │ UserService │ RoleService │ DashboardService        │    │
│  │  • 核心业务逻辑                                                       │    │
│  │  • 事务管理 (@Transactional)                                         │    │
│  │  • 乐观锁重试 (REQUIRES_NEW + 外层循环)                               │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                    ↓                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                      数据层 (mapper/ + entity/)                      │    │
│  │  MyBatis-Plus Mapper │ XML 映射 │ 实体类                             │    │
│  │  • 自动 CRUD                                                          │    │
│  │  • 乐观锁 @Version                                                    │    │
│  │  • 自动填充 (create_time, update_time)                               │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                    ↓                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                      MySQL + Redis                                           │
│  • MySQL: 业务数据持久化                                                     │
│  • Redis: Token 黑名单 (TTL 自动过期)                                        │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 后端目录结构

```
backend/src/main/java/com/example/trace/
├── TraceApplication.java          # 启动类
├── annotation/                    # 自定义注解
│   └── RequirePermission.java     # 权限校验注解
├── common/                        # 通用组件
│   ├── ApiResponse.java           # 统一响应封装
│   ├── BizCode.java               # 业务错误码
│   ├── BizException.java          # 业务异常
│   └── GlobalExceptionHandler.java# 全局异常处理
├── config/                        # 配置类
│   ├── JacksonConfig.java         # JSON 序列化配置 (snake_case)
│   ├── CorsFilter.java            # 唯一 CORS 响应头写入点
│   ├── CorsProperties.java        # CORS 外置配置绑定
│   ├── CorsOriginMatcher.java     # CORS 精确/通配 Origin 匹配
│   ├── MybatisMetaObjectHandler.java # 字段自动填充
│   ├── MybatisPlusConfig.java     # 乐观锁插件配置
│   └── WebMvcConfig.java          # 登录/权限拦截器配置
├── controller/                    # 控制器 (接收请求)
│   ├── AuthController.java        # 认证 (登录/注册/登出)
│   ├── TraceController.java       # 溯源 (赋码/扫码/验链/异常/纠错)
│   ├── TraceAssignBatchController.java # 赋码批次对账/码列表
│   ├── TraceCodeController.java   # 单品码激活
│   ├── TraceFlowTaskController.java # 仓库/物流流转任务
│   ├── TraceAggregationController.java # 箱码/托盘码聚合
│   ├── TraceNodeController.java   # 结构化业务节点
│   ├── UserController.java        # 用户管理
│   ├── RoleController.java        # 角色管理
│   ├── PartController.java        # 配件管理
│   └── DashboardController.java   # 统计看板
├── dto/                           # 数据传输对象
│   ├── *Request.java              # 请求体
│   └── *Response.java             # 响应体
├── entity/                        # 数据库实体
│   ├── User.java                  # 用户表
│   ├── Role.java                  # 角色表
│   ├── TraceSnapshot.java         # 溯源快照表 (@Version 乐观锁)
│   ├── TraceLifecycleLog.java     # 溯源日志表 (Hash 链 + 签名)
│   ├── TraceAssignBatch.java      # 赋码批次表
│   ├── TraceCode.java             # 单品码状态表
│   ├── TraceNode.java             # 结构化业务节点
│   ├── TraceUserNodeBinding.java  # 用户-节点绑定
│   ├── TraceFlowTask.java         # 流转任务/发货单
│   ├── TraceFlowTaskScan.java     # 任务内扫码明细
│   ├── TraceAggregation.java      # 箱码/托盘码聚合关系
│   ├── TraceScanIdempotency.java  # 扫码幂等记录
│   └── BasePartSpec.java          # 配件规格表
├── enums/                         # 枚举类 (v2.0 新增)
│   ├── ActionType.java            # 操作类型 (INIT/PRINT_CODE/INBOUND/...)
│   ├── TraceStatus.java           # 物流快照状态
│   ├── TraceCodeStatus.java       # 单品码状态
│   ├── TraceFlowTaskType.java     # 任务类型
│   └── TraceAggregationRelationType.java # 聚合关系类型
├── mapper/                        # MyBatis 映射接口
│   └── *Mapper.java               # 继承 BaseMapper<T>
├── security/                      # 安全组件
│   ├── JwtUtil.java               # JWT 生成/解析
│   ├── LoginInterceptor.java      # 登录拦截器
│   ├── PermissionInterceptor.java # 权限拦截器
│   └── UserContext.java           # 用户上下文 (ThreadLocal)
├── service/                       # 业务逻辑
│   ├── TraceService.java          # 溯源服务接口
│   ├── policy/
│   │   ├── RolePolicy.java        # 角色层级/系统角色/授权策略
│   │   ├── TraceTransitionPolicy.java # 溯源码状态机
│   │   └── TraceActionPermissionPolicy.java # 业务动作权限策略
│   └── impl/
│       └── TraceServiceImpl.java  # 溯源服务实现
└── util/                          # 工具类
    ├── HashUtil.java              # SHA256 哈希计算
    ├── SignatureUtil.java         # RSA 数字签名 (v2.0)
    └── DateTimeUtil.java          # 时间处理
```

### 核心数据流

```
                    批次赋码 + 单品码激活流程
┌────────────────────────────────────────────────────────────┐
│  1. POST /api/traces                                        │
│  2. INSERT trace_assign_batch（生产/工单/批次数量）          │
│  3. 为批次生成 N 个唯一 trace_code                           │
│  4. INSERT trace_code（GENERATED，批次内 serial_no）         │
│  5. INSERT trace_lifecycle_log（INIT + Hash + RSA 签名）     │
│  6. INSERT trace_snapshot（历史兼容 INIT 快照）              │
│  7. 打印/重打/作废/激活分别写入 PRINT/REPRINT/VOID/ACTIVATE │
└────────────────────────────────────────────────────────────┘

                    普通扫码流转流程 (状态机 + 幂等 + 乐观锁)
┌────────────────────────────────────────────────────────────┐
│  1. POST /api/traces/{code}/events                          │
│  2. 可选占用 trace_scan_idempotency 幂等键                   │
│  3. SELECT snapshot + trace_code（状态、版本、码可用性）     │
│  4. TraceTransitionPolicy 校验当前状态 + 动作是否合法        │
│  5. 后端推导/校验 fromNode、地区、目标节点                   │
│  6. 计算 Hash/RSA 签名（operator 与非空 remark 已保护）       │
│  7. INSERT trace_lifecycle_log，UPDATE snapshot/version      │
│  8. 同步 trace_code 物流态；重复幂等键不重复写日志           │
└────────────────────────────────────────────────────────────┘

                    任务驱动连续扫码流程
┌────────────────────────────────────────────────────────────┐
│  1. POST /api/trace-flow-tasks 创建发货/入库/接收任务        │
│  2. 任务固定 source_node_id、target_node_id、expected_qty    │
│  3. POST /api/trace-flow-tasks/{id}/scan 连续扫码            │
│  4. 扫父码时从 trace_aggregation 展开箱/托盘下所有子码       │
│  5. trace_flow_task_scan 保证单码只计数一次并记录重复次数    │
│  6. complete 时数量一致直接完成；少扫/多扫必须填写差异原因   │
└────────────────────────────────────────────────────────────┘

                    验链流程
┌────────────────────────────────────────────────────────────┐
│  GET /api/traces/{code}/verify                              │
│  ┌────────────────────────────────────────────────────────┐│
│  │  for each log in chain:                                ││
│  │    1. 验证 prevHash == 上一条的 currentHash (链连续性) ││
│  │    2. 重算 Hash，对比存储值 (数据完整性)                ││
│  │    3. 用公钥验证签名 (不可伪造性)                       ││
│  │  返回 valid=true/false + 错误详情                       ││
│  └────────────────────────────────────────────────────────┘│
└────────────────────────────────────────────────────────────┘
```

### 安全架构

```
┌─────────────────────────────────────────────────────────────┐
│                      安全防护层次                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 认证层 - JWT + Redis 黑名单                              │
│     └── 无状态认证，登出即失效                               │
│                                                             │
│  2. 授权层 - RBAC 权限控制                                   │
│     └── @RequirePermission 注解校验                          │
│                                                             │
│  3. 数据层 - Hash 链 + RSA 签名                              │
│     ├── Hash 链：保证数据连续性                              │
│     └── 数字签名：保证数据不可伪造                           │
│                                                             │
│  4. 并发层 - 乐观锁 + 独立事务重试                           │
│     └── 防止并发导致的数据不一致                             │
│                                                             │
│  5. 输入层 - 枚举校验 + 跨链攻击防御                         │
│     └── ActionType 枚举、correctionOf 校验                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| 后端 | Spring Boot 3.x + MyBatis-Plus | RESTful API |
| 数据库 | MySQL 8.0 | 业务数据存储 |
| 缓存 | Redis | Token 黑名单存储 |
| 前端 | Vue 3 + Vite + ECharts + 高德地图 + vue-qrcode-reader | 可视化界面与扫码/追踪前端，2026-05 完成 [Linear 设计系统](DESIGN.md) 全量重构（详见 [`前端重构执行任务表_20260507.md`](前端重构执行任务表_20260507.md)） |
| 认证 | JWT + BCrypt + Redis | 无状态认证 |

## 环境要求

- JDK 19+
- Maven 3.9+
- MySQL 8.0+
- **Redis 6.0+**（Token 黑名单必需）
- Node.js 18+（前端）

## 后端快速启动

### 1. 创建数据库

```sql
CREATE DATABASE trace_db DEFAULT CHARACTER SET utf8mb4;
```

执行初始化脚本：[backend/sql/init_schema.sql](backend/sql/init_schema.sql)

**已有数据库升级路径**：如果不是全新库，请按迁移编号顺序执行 `backend/sql/migrate_*.sql`。与新版溯源码核心业务直接相关的迁移包括：

| 迁移 | 说明 |
|---|---|
| `migrate_v8_trace_scan_idempotency.sql` | 普通扫码幂等表 |
| `migrate_v9_trace_audit_view_permission.sql` | `trace:audit:view` 审计视图权限 |
| `migrate_v10_trace_assign_batch.sql` | 赋码批次表 |
| `migrate_v11_trace_code_status.sql` | 单品码状态表，并回填历史快照码 |
| `migrate_v12_trace_node.sql` | 结构化业务节点表 |
| `migrate_v13_trace_user_node_binding.sql` | 用户-节点绑定表 |
| `migrate_v14_trace_flow_task.sql` | 流转任务表 |
| `migrate_v15_trace_flow_task_scan.sql` | 任务内连续扫码明细表 |
| `migrate_v16_trace_flow_task_discrepancy.sql` | 任务完成差异字段 |
| `migrate_v17_trace_aggregation.sql` | 箱码/托盘码聚合关系表 |
| `migrate_v18_trace_aggregation_events.sql` | 聚合事件动作类型注释同步 |
| `migrate_v19_trace_business_action_permissions.sql` | 新版业务动作权限 |
| `migrate_v20_trace_exception_workflow.sql` | 异常冻结恢复字段与 EXCEPTION_CLOSE 动作 |

### 2. 启动 Redis

**Windows**（使用 WSL 或 Memurai）:
```bash
# WSL 方式
wsl redis-server

# 或使用 Docker
docker run -d --name redis -p 6379:6379 redis:alpine
```

**Linux/macOS**:
```bash
redis-server
```

验证 Redis 运行：
```bash
redis-cli ping
# 返回 PONG 表示正常
```

### 3. 选择 profile 并配置运行环境

后端配置已拆分为公共 `application.yml` 与 `application-dev.yml` / `application-test.yml` / `application-prod.yml`：

- 默认 profile 为 `dev`，适合本地开发；如本机 MySQL/Redis 与默认值不同，请通过环境变量覆盖。
- `prod` 不提供默认 JWT secret、数据库账号密码或签名密钥路径；缺少必需配置会启动失败。
- 生产密钥和数据库凭据不要写入源码配置文件，参考 `backend/.env.example` 在部署环境注入。

本地开发覆盖示例（PowerShell）：

```powershell
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:TRACE_DB_URL = 'jdbc:mysql://localhost:3306/trace_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai'
$env:TRACE_DB_USERNAME = '<your-mysql-user>'
$env:TRACE_DB_PASSWORD = '<your-mysql-password>'
$env:TRACE_REDIS_HOST = 'localhost'
$env:TRACE_REDIS_PORT = '6379'
```

### 4. 启动后端

```bash
cd backend
mvn spring-boot:run
```

或直接运行 [TraceApplication.java](backend/src/main/java/com/example/trace/TraceApplication.java)

后端启动后访问：http://localhost:8080

## 前端启动与环境变量

> 前端模块完整文档（目录结构 / 设计系统 / 断点策略 / 测试 / 鉴权细节）见 [`frontend/README.md`](frontend/README.md)。本节仅覆盖最小启动路径与敏感凭据规范。
>
> **设计系统**：所有 view 严格对齐 [`DESIGN.md`](DESIGN.md) Linear 设计语言（lavender `#5e6ad2` 单 accent + Inter / JetBrains Mono + 4/6/8/12/16 圆角 + 4/8/12/16/24/32/48 间距）。视觉契约存放在 [`frontend/preview/linear-*.html`](frontend/preview/) 共 5 张（登录 / 仪表盘 / 扫码 / 列表 / 详情），是新组件开发的 1:1 对照基线。

### 1. 安装依赖与生成自签证书

```bash
cd frontend
npm install

# 首次启动 HTTPS dev server 需先生成自签证书（摄像头扫码要求 HTTPS）
node generate-cert.js          # 或 PowerShell 用户：./generate-cert.ps1
# 如需走 HTTP，设 VITE_DEV_HTTPS=false
```

证书输出到 `frontend/certs/` 后由 `vite.config.js` 加载。

> ⚠️ **`frontend/certs/` 目录下的 `*.key` / `*.crt` 必须本地生成，绝不能提交到版本库。**
> 已通过 [`frontend/.gitignore`](frontend/.gitignore) 中的 `certs/*` + `!certs/openssl.cnf` 强制忽略：所有证书材料（包括未来可能新增的 `.pfx` / `.p12` 等格式）默认排除，仅保留 `openssl.cnf`（证书生成模板，无密钥材料）入库供其他开发者参考。
> 如发现 `.key` / `.crt` 被误纳入历史 commit，需用 `git filter-repo` 或 BFG 移除并强推（参考 README "前端启动与环境变量 → 敏感凭据守则"段的应急流程）。

### 2. 配置前端环境变量（**含敏感凭据规范**）

前端的环境变量分两类文件：

| 文件 | 是否进版本库 | 用途 |
|---|---|---|
| `frontend/.env.example` | 是（追踪） | 公开的占位符模板，列出所有需要的变量名 |
| `frontend/.env.development` | 是（追踪） | 仅含**占位符**与引导注释，不放真值 |
| `frontend/.env.development.local` | 否（`.gitignore` 忽略） | 本地真值；高德 Key 等敏感凭据**仅放这里** |
| `frontend/.env.production.local` | 否（`.gitignore` 忽略） | 生产构建本地覆盖（CI 中应改用 Secret 注入） |

**首次配置步骤**：

```bash
cd frontend
cp .env.example .env.development.local
# 编辑 .env.development.local，填入你自己的高德 Web Key 等敏感值
```

**敏感凭据守则（必读）**：

- **高德 Web Key（`VITE_AMAP_KEY`）** 等任何敏感值**只能写到 `frontend/.env.development.local` 或 CI Secret**，不得提交到任何被追踪的 `.env*` 文件。
- 已被追踪的 `.env.development` 仅保留占位符；如发现误提交真值，应立刻：
  1. 在高德开放平台控制台**吊销旧 Key**；
  2. 创建启用了**白名单域名 / Referer**（生产域名 + `localhost` / `127.0.0.1` / 局域网调试段）的新 Key；
  3. 把新 Key 写到 `frontend/.env.development.local`；
  4. 把 `frontend/.env.development` 改回占位符（如本仓库现状）；
  5. 若已 push 至远端，需用 `git filter-repo` 或 BFG 移除历史记录并强推。
- Vite 加载顺序：当 `.env.development` 与 `.env.development.local` 同时存在时，**`.local` 优先**，不会回退到追踪版本的占位符——这一性质让"占位符在仓库 + 真值在本地"成为安全且可发现的组合。

### 3. 启动前端 dev server

```bash
npm run dev          # 默认 https://localhost:5173（HTTPS）
```

Vite 已配置 `/api` 代理到 `VITE_API_PROXY_TARGET`（默认 `http://localhost:8080`），与后端配合无需额外 CORS 调整。

## 示例数据生成

系统提供了管理接口用于生成**可通过验证的示例数据**（包含完整的 Hash 链和 RSA 签名），方便开发测试和演示。

> ⚠️ **安全边界**：
> - 这些接口默认仅在 `dev/test` 环境启用；生产/其他环境默认关闭，需显式设置 `TRACE_DEMO_DATA_ENABLED=true` 才可访问。
> - `POST /api/admin/generate-sample-data` 需要 `trace:data:generate` 权限，单次 `count` 硬上限为 **500**；`TRACE_DEMO_DATA_MAX_GENERATE_COUNT` 只能用于下调环境上限。
> - `DELETE /api/admin/clear-trace-data` 需要 `trace:data:clear` 权限，且必须携带 `confirm=DELETE_TRACE_DATA` 二次确认参数。

> ⚠ **批量持久化语义（T-P1-01）**：`POST /api/traces`（批量赋码）与 `POST /api/admin/generate-sample-data` 的写入路径已改为"两阶段"——
> 1. **阶段 1（无事务）**：在内存里完成 hash 计算 + RSA 签名 + 实体组装；
> 2. **阶段 2（分批 REQUIRES_NEW）**：按 `TRACE_BATCH_COMMIT_SIZE`（默认 50）切片提交，每个分片独立事务。
>
> 因此**这些接口不再保证"全或无"**：若第 N 个分片失败，前 1..N-1 个分片**已提交且不会回滚**。前端调用方在错误处理时应假定可能出现部分写入；后端日志（`TraceBatchCommitter`）会输出已提交分片数。该改造的动机是把 RSA 签名移出事务、避免 HikariCP 连接池被长事务打满，详见 `项目审查整改任务表_20260503.md` T-P1-01。

### 生成示例数据

```bash
# 1. 登录获取 Token（使用超管账号）
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "superadmin", "password": "superadmin123456"}'

# 2. 生成 500 条示例数据
curl -X POST "http://localhost:8080/api/admin/generate-sample-data?count=500" \
  -H "Authorization: Bearer <token>"
```

**生成的数据包括**：
- 19 种零部件规格（阀门、轴承、电机、传感器、管件）
- 500 条溯源快照
- 约 1700+ 条生命周期日志（每条溯源码 3-5 条日志）
- 覆盖 31 个区域的真实地理分布

### 验证数据完整性

```bash
# 验证任意溯源码
curl http://localhost:8080/api/traces/TC-20260119-0001/verify \
  -H "Authorization: Bearer <token>"
```

返回示例：
```json
{
  "valid": true,
  "total_logs": 3,
  "hash_verified_count": 3,
  "signature_verified_count": 3
}
```

### 清空数据（谨慎操作）

```bash
curl -X DELETE "http://localhost:8080/api/admin/clear-trace-data?confirm=DELETE_TRACE_DATA" \
  -H "Authorization: Bearer <token>"
```

### 导出为 SQL 文件

已预生成的示例数据 SQL 文件：[backend/sql/sample_data_full.sql](backend/sql/sample_data_full.sql)

```bash
# 导入到新数据库
mysql -u root -p trace_db < backend/sql/sample_data_full.sql
```

> **注意**：导入前需先执行 [init_schema.sql](backend/sql/init_schema.sql) 创建表结构。

## API 接口概览

| 模块 | 接口 | 说明 |
|------|------|------|
| 认证 | `POST /api/auth/register` | 用户注册 |
| 认证 | `POST /api/auth/login` | 用户登录（支持记住登录） |
| 认证 | `POST /api/auth/logout` | 用户登出 |
| 认证 | `POST /api/auth/refresh` | 刷新 Token |
| 认证 | `GET /api/auth/me` | 获取当前用户信息 |
| 溯源 | `POST /api/traces` | 创建赋码批次并生成单品码（单次 `quantity<=500`） |
| 溯源 | `GET /api/traces?keyword&status&spu_id&batch_no&page&size&sort&order` | 追溯码分页列表（多条件筛选） |
| 溯源 | `POST /api/traces/{traceCode}/events` | 普通扫码流转（状态机校验 + 幂等） |
| 溯源 | `GET /api/traces/{traceCode}/available-actions` | 扫码后可执行动作推荐 |
| 溯源 | `GET /api/traces/{traceCode}?view=effective|audit` | 溯源详情；audit 需 `trace:audit:view` |
| 标签 | `POST /api/traces/{traceCode}/print|reprint|void` | 打印、重打/补打、作废标签 |
| 单品码 | `POST /api/trace-codes/{traceCode}/activate` | 贴码后扫码激活/复核 |
| 批次 | `GET /api/trace-batches/{batchId}` | 赋码批次数量对账 |
| 批次 | `GET /api/trace-batches/{batchId}/codes` | 批次单品码列表 |
| 任务 | `GET/POST /api/trace-flow-tasks` | 仓库/物流流转任务列表与创建 |
| 任务 | `POST /api/trace-flow-tasks/{id}/scan|complete|cancel` | 任务内连续扫码、完成、取消 |
| 聚合 | `POST /api/trace-aggregations` | 箱码/托盘码绑定 |
| 聚合 | `POST /api/trace-aggregations/{relationId}/release` | 解除箱码/托盘码绑定 |
| 聚合 | `GET /api/trace-aggregations/children|parents|history/*` | 聚合有效关系与历史 |
| 节点 | `GET/POST/PUT/DELETE /api/trace-nodes` | 结构化业务节点 |
| 节点 | `GET /api/users/me/trace-nodes`、`GET/PUT /api/users/{id}/trace-nodes` | 用户可操作节点绑定 |
| 异常/纠错 | `POST /api/traces/{traceCode}/exception/close` | 解除异常冻结 |
| 异常/纠错 | `POST /api/traces/{traceCode}/corrections` | 红冲蓝补式审计纠错 |
| 溯源 | `GET /api/traces/{traceCode}/verify` | **验证溯源链完整性**（v2.0） |
| 溯源 | `GET /api/traces/public-key` | **获取公钥**（v2.0，公开接口） |
| Dashboard | `GET /api/dashboard/kpi` | KPI 统计 |
| Dashboard | `GET /api/dashboard/map` | 地图数据 |
| Dashboard | `GET /api/dashboard/trend` | 趋势数据 |
| Dashboard | `GET /api/dashboard/topology` | 拓扑图数据 |
| 管理 | `POST /api/admin/generate-sample-data` | **生成可验证的示例数据**（需 `trace:data:generate`，`count<=500`） |
| 管理 | `DELETE /api/admin/clear-trace-data` | 清空溯源数据（危险，需 `trace:data:clear` + `confirm=DELETE_TRACE_DATA`） |
| 用户管理 | `GET/POST/PUT/DELETE /api/users` | 用户 CRUD |
| 角色管理 | `GET/POST/PUT/DELETE /api/roles` | 角色和权限管理 |
| 配件管理 | `GET/POST/PUT/DELETE /api/parts` | 配件规格 CRUD |

详见 [api-doc.md](api-doc.md)

> 配件删除会先检查 `trace_snapshot` 与 `trace_lifecycle_log` 的 `spu_id` 引用；已参与溯源的配件返回 409，批量删除会整批拒绝，避免孤儿溯源数据。

> 扫码流转 `POST /api/traces/{traceCode}/events` 支持可选 `idempotencyKey` 与 `remark`（最长 255 字符）。非空备注和操作人 `operator` 会随 `trace_lifecycle_log` 落库，并纳入新日志的 Hash 与 RSA 签名载荷；验链服务兼容历史未保护 `operator` 的旧日志。

## 新版核心业务流程

### 生产赋码与扫码激活

1. 生产人员在“生产赋码工作台”创建赋码批次（`POST /api/traces`），一次生成 N 个唯一单品码。
2. 系统写入 `trace_assign_batch`、`trace_code`、`trace_lifecycle_log` 和 `trace_snapshot`。
3. 生产人员打印/重打/作废标签（`PRINT_CODE/REPRINT_CODE/VOID_CODE`）。
4. 贴码后扫码激活（`POST /api/trace-codes/{traceCode}/activate`），码状态变为 `ACTIVATED` 后才能进入常规入库/出库/流转。
5. 批次详情（`GET /api/trace-batches/{batchId}`）展示计划、生成、打印、激活、入库、作废数量和差异原因。

### 快递式仓库/物流任务流

1. 创建流转任务（`POST /api/trace-flow-tasks`），提前确定起点、终点和预计数量。
2. 仓库/物流用户在任务工作台连续扫码（`POST /api/trace-flow-tasks/{id}/scan`），系统按任务自动推导 `fromNode/toNode`。
3. 重复扫码返回“已扫”反馈，不重复计数；扫箱码/托盘码时自动展开有效子码批量流转。
4. 完成任务（`POST /api/trace-flow-tasks/{id}/complete`）时，数量一致直接完成；少扫/多扫必须填写差异原因，任务进入异常状态并留下审计字段。

### 箱码/托盘码聚合

- `POST /api/trace-aggregations` 创建箱码/托盘码与单品码的有效绑定，并写入 `PACK/PALLETIZE` 生命周期事件。
- `POST /api/trace-aggregations/{relationId}/release` 解除绑定，并写入 `UNPACK/UNPALLETIZE` 生命周期事件。
- 运输中、已交接、异常冻结等状态下禁止随意修改聚合关系。
- 单品详情会返回 `aggregationHistory`，展示直接/间接箱码、托盘码历史。

### 详情视图与异常/纠错

- `GET /api/traces/{traceCode}?view=effective` 是默认业务有效视图，会隐藏被后续 `CORRECTION` 覆盖的原始日志。
- `GET /api/traces/{traceCode}?view=audit` 是审计完整视图，需要 `trace:audit:view`。
- 异常冻结通过 `EXCEPTION_OPEN` 暂停常规流转；`POST /api/traces/{traceCode}/exception/close` 写入 `EXCEPTION_CLOSE` 并恢复冻结前状态。
- 红冲蓝补纠错使用 `POST /api/traces/{traceCode}/corrections`，只追加审计记录，不物理删除历史。

## 鉴权说明

- 登录成功返回 JWT `token`
- 后续请求需携带 Header：`Authorization: Bearer <token>`
- 放行接口：`/api/auth/login`、`/api/auth/register`
- Token 有效期：2 小时（记住登录：1 天）
- 前端暂继续使用 `localStorage` 保存 Token，并通过短 Token、Redis 黑名单、CSP 基线作补偿；决策记录见 `docs/security/token-storage-and-csp.md`
- 密码使用 BCrypt 加密存储
- 登出后 Token 加入黑名单立即失效

### 前端 Token 存储威胁模型

> ⚠️ 当前前端 JWT 保存在 `localStorage`，**对 XSS 不抗**——任何成功执行在本站点上的脚本都能读取 Token 并冒用会话。这是已知风险，已通过多层补偿控制压低被利用的概率与影响面，并已规划彻底迁移路径。完整决策记录见 [`docs/security/token-storage-and-csp.md`](docs/security/token-storage-and-csp.md)。

**当前决策**：保留 `Authorization: Bearer <jwt>` + `localStorage`，理由：①后端契约稳定、移动端/扫码端/Postman 都沿用；②直接切 Cookie 会扩大 CORS / refresh token / CSRF / 测试 / 文档改造面，超出短期成本预算。

**已落地的补偿控制（不是"打算做"，是当前已运行的代码与配置）**：

| 控制层 | 实现 | 证据 |
|---|---|---|
| 短 Token | `TRACE_JWT_EXPIRATION_HOURS=2`、`TRACE_JWT_REMEMBER_EXPIRATION_DAYS=1`（旧默认 24h/7d） | `application.yml`、`backend/src/test/java/com/example/trace/security/JwtUtilTest.java` |
| 立即失效 | 登出 / 改密 / 角色变更通过 Redis 黑名单 + `token_version` 让旧 Token 下次请求即拒；Redis 故障 fail-closed 返回 503 | `security/TokenStore.java`、`security/LoginInterceptor.java` |
| 存储适配层 | 所有 token / user 的 localStorage 读写集中在 `frontend/src/core/auth/authStorage.js` ——后续切到内存 token / Cookie 只改这一处 | `frontend/src/core/auth/authStorage.js`、对应 vitest 用例 |
| CSP 基线 | `frontend/index.html` 内 `<meta http-equiv="Content-Security-Policy" ...>` 仅放行同源 + 高德地图脚本 / 图片 / 连接域；style 暂含 `'unsafe-inline'` 兼容 Vue 动态样式 | `frontend/index.html` |
| 不用 v-html / innerHTML 注入用户内容 | 全仓 grep `v-html`/`innerHTML` 在生产代码中零匹配；`TraceRouteMap.vue` InfoWindow 走 `createElement` DOM API，单元测试 `TraceRouteMap.info-window.test.js` 锁定该约束 | `frontend/src/features/trace/components/traceRouteMapInfoWindow.js`、`__tests__/TraceRouteMap.info-window.test.js` |
| 异常缓存清理 | 损坏的 `user` 缓存会被自动清除，避免启动时 JSON 解析异常导致前端整体不可用 | `frontend/src/core/auth/__tests__/authStorage.test.js` |

**生产部署额外要求**：CSP 应在网关 / Nginx / CDN 上以 HTTP Header 形式补齐（meta CSP 不能可靠设置 `frame-ancestors`）。决策文档第 3 节给出了完整 nginx 模板。

**后续彻底迁移路径**（独立大改造，不计入本任务）：

1. 后端引入 access token + 轮换 refresh token；refresh token 用 `httpOnly; Secure; SameSite` Cookie；
2. 前端仅在内存中保存 access token，刷新页面通过 refresh Cookie 换取新 access token；
3. 修改状态的接口补充 CSRF Token / SameSite 验证；
4. CORS 从 `Authorization` Header 凭证模型切到 Cookie 凭证模型，并补端到端测试。

> 当前不做该迁移的取舍：项目处于本地开发 + 单实例部署阶段，短 Token + 黑名单 + CSP + 适配层已经把"localStorage 中 Token 被 XSS 读取"的最坏窗口压到 ≤ 2 小时；与一次性引入 Cookie + CSRF + refresh 大改造相比，性价比更优。决策可在 [`docs/security/token-storage-and-csp.md`](docs/security/token-storage-and-csp.md) 第 4 节查阅，需要时再单独排期。

## 用户管理权限控制

### 溯源业务权限

| 权限代码 | 说明 |
|---|---|
| `trace:view` | 查看溯源详情、节点、任务、批次和聚合关系 |
| `trace:create` | 历史兼容生产/管理权限 |
| `trace:batch:create` | 创建赋码批次并生成单品码 |
| `trace:code:print` | 打印、重打/补打、作废标签 |
| `trace:code:activate` | 单品码扫码激活/复核 |
| `trace:scan` | 超级扫码权限，可执行全部扫码动作 |
| `trace:inbound` / `trace:outbound` / `trace:transfer` | 入库、出库、流转细粒度权限 |
| `trace:task:create` | 创建仓库/物流流转任务 |
| `trace:task:scan` | 任务内连续扫码、扫箱码/托盘码 |
| `trace:task:complete` | 完成任务并处理数量差异 |
| `trace:audit:view` | 查看 `view=audit` 审计完整历史 |
| `trace:exception:handle` | 异常冻结、解除和红冲蓝补纠错 |

默认授权策略：

- `SUPER_ADMIN` / `ADMIN` 拥有全部权限。
- `PRODUCER` 侧重赋码、打印、激活和异常上报。
- `WAREHOUSE` 侧重入库/出库、流转任务创建/扫码/完成和异常上报。
- `LOGISTICS` 侧重物流流转、任务扫码/完成和异常上报。
- `USER` 仅具备查询类能力。

### 角色优先级体系

| 角色代码 | 角色名称 | 优先级 | 说明 |
|---------|---------|-------|------|
| SUPER_ADMIN | 超级管理员 | 3 (最高) | 系统最高权限 |
| ADMIN | 系统管理员 | 2 | 日常管理权限 |
| PRODUCER | 生产人员 | 1 | 业务角色 |
| WAREHOUSE | 仓库人员 | 1 | 业务角色 |
| LOGISTICS | 物流人员 | 1 | 业务角色 |
| USER | 普通用户 | 1 | 基础角色 |

### 用户列表可见范围

| 当前登录角色 | 可见用户范围 |
|-------------|------------|
| SUPER_ADMIN | 所有用户（包括 ADMIN） |
| ADMIN | 仅 PRODUCER、WAREHOUSE、LOGISTICS、USER（看不到 SUPER_ADMIN 和 ADMIN） |

### 用户操作权限规则

1. **只能操作比自己优先级低的用户**
   - SUPER_ADMIN 可操作 ADMIN 及以下
   - ADMIN 可操作 PRODUCER/WAREHOUSE/LOGISTICS/USER

2. **创建用户时的角色限制**
   - SUPER_ADMIN 可创建 ADMIN 及以下角色
   - ADMIN 只能创建 PRODUCER/WAREHOUSE/LOGISTICS/USER 角色

3. **特殊保护**
   - `superadmin` 账号不能被任何人删除或禁用
   - 操作违规会返回 403 Forbidden

## 默认账号

密码规则：**用户名 + 123456**

| 用户名 | 密码 | 角色 |
|--------|------|------|
| superadmin | superadmin123456 | 超级管理员 |
| admin | admin123456 | 系统管理员 |
| producer | producer123456 | 生产人员 |
| warehouse | warehouse123456 | 仓库人员 |
| logistics | logistics123456 | 物流人员 |
| user | user123456 | 普通用户 |

## Redis 的作用

本项目使用 Redis 实现 **Token 黑名单机制**，解决 JWT 无状态特性带来的安全问题：

### 为什么需要 Token 黑名单？

JWT 是无状态的，一旦签发就在有效期内始终有效。但以下场景需要让 Token 提前失效：

| 场景 | 问题 | 解决方案 |
|------|------|----------|
| 用户登出 | Token 仍可使用直到过期 | 将 Token 的 `jti` 加入 Redis 黑名单 |
| Token 刷新 | 旧 Token 仍可使用 | 刷新时将旧 Token 加入黑名单 |
| 密码修改 | 所有已签发 Token 应失效 | 递增 `token_version`，拦截器校验版本 |
| 角色变更 | 权限应立即更新 | 同上，递增 `token_version` |
| 账号被禁 | 用户应立即无法访问 | 拦截器实时检查用户状态 |

### Redis 黑名单实现原理

```
┌─────────────────────────────────────────────────────────────┐
│                     Token 验证流程                           │
├─────────────────────────────────────────────────────────────┤
│  1. 用户请求携带 Token                                        │
│  2. 解析 Token，提取 jti（唯一标识）和 token_version          │
│  3. 查询 Redis：key = "token:blacklist:{jti}"               │
│     - 存在 → 返回 401（Token 已在黑名单）                     │
│     - 不存在 → 继续验证                                       │
│     - Redis 查询异常 → 返回 503（认证状态存储不可用）          │
│  4. 查询数据库用户的 token_version                           │
│     - Token 版本 < 用户版本 → 返回 401（密码/角色已变更）      │
│     - 版本匹配 → 继续验证                                     │
│  5. 检查用户状态                                              │
│     - 已禁用 → 返回 401                                       │
│     - 正常 → 放行请求                                         │
└─────────────────────────────────────────────────────────────┘
```

### Redis 数据结构

| Key 格式 | Value | TTL |
|----------|-------|-----|
| `token:blacklist:{jti}` | "1" | Token 剩余有效期 |

示例：
```
token:blacklist:550e8400-e29b-41d4-a716-446655440000 = "1"  TTL: 3600s
```

### 为什么用 Redis 而不是内存？

| 方案 | 优点 | 缺点 |
|------|------|------|
| 内存 Map | 简单、无依赖 | 重启丢失、不支持集群 |
| **Redis** | 持久化、支持集群、自动过期 | 需要额外部署 |
| 数据库 | 持久化 | 查询慢、需定期清理 |

Redis 的 TTL 特性非常适合黑名单场景：Token 过期后黑名单记录自动清理，无需手动维护。

> Redis 黑名单是认证安全依赖：查询或写入异常会 fail-closed，相关请求返回 503，不会在基础设施异常时默认放行 Token。

## 统一响应格式

```json
{
  "code": 0,
  "status": 200,
  "message": "success",
  "data": { ... }
}
```

| 字段 | 说明 |
|------|------|
| code | 业务码（0=成功，其它=业务错误） |
| status | HTTP 状态码 |
| message | 提示信息 |
| data | 业务数据 |

## 字段命名规范

| 场景 | 命名 |
|--------|--------|
| 原始 HTTP 请求体 | 后端支持 `camelCase` 或 `snake_case` |
| 原始 HTTP 响应体 | 后端 JSON 使用 `snake_case` |
| 前端 API 模块入参 | 使用 `camelCase`，`request.js` 会序列化为 `snake_case` |
| 前端 API 模块返回 | `request.js` 会转换为 `camelCase`，如 `batchId`、`traceCode` |

## 核心功能

- ✅ 批次赋码 - 创建赋码批次并批量生成唯一单品码（单次 `quantity<=500`）
- ✅ 一物一码 - `trace_code` 区分 `GENERATED/PRINTED/ACTIVATED/IN_STOCK/IN_TRANSIT/EXCEPTION/VOIDED/SCRAPPED`
- ✅ 标签管理 - 打印、重打/补打、作废、扫码激活均进入生命周期审计链
- ✅ 扫码流转 - 状态机校验 + 节点自动推导 + 幂等键防重复提交
- ✅ 可执行动作推荐 - 扫码后由后端返回当前用户可做什么
- ✅ 任务驱动出入库 - 仓库/物流按任务连续扫码、自动累计数量、一键完成
- ✅ 差异处理 - 少扫/多扫必须填写原因并形成异常任务
- ✅ 箱码/托盘码聚合 - 支持装箱/拆箱/托盘绑定和扫父码批量流转子码
- ✅ 溯源查询 - 支持 `effective` 业务有效视图与 `audit` 审计完整视图
- ✅ 哈希链防篡改 - 每条记录包含前一条的哈希
- ✅ **RSA 数字签名** - 非对称加密确保数据不可伪造（v2.0 新增）
- ✅ **验证溯源链** - 公开接口验证 Hash 链和数字签名完整性（v2.0 新增）
- ✅ 快照表加速 - 当前状态快速查询
- ✅ 异常冻结与红冲蓝补纠错 - `EXCEPTION_OPEN/CLOSE` 冻结/恢复，`CORRECTION` 追加审计修正
- ✅ **跨链攻击防御** - 防止修正其他溯源码的记录（v2.0 新增）
- ✅ **乐观锁并发控制** - 高并发场景下的数据一致性（v2.0 新增）
- ✅ Dashboard 统计 - KPI/地图热力图/趋势图/拓扑图
- ✅ JWT 认证 - 安全的无状态身份认证
- ✅ Redis Token 黑名单 - 登出/刷新后 Token 立即失效
- ✅ Token 版本控制 - 密码/角色变更后强制重新登录
- ✅ 动态权限控制 - 基于角色的细粒度权限管理（RBAC）
- ✅ 用户管理 - 用户 CRUD、角色分配、状态管理
- ✅ 角色管理 - 角色 CRUD、权限分配
- ✅ 配件管理 - 配件规格 CRUD

## 数字签名配置（RSA 密钥）

系统使用 RSA-SHA256 非对称加密对每条溯源日志进行签名，确保**即使数据库管理员也无法伪造数据**。

### Signature key management

| Mode | Scope | Configuration |
|------|-------|---------------|
| **In-memory auto-generate（dev/test 默认 / 开箱即用 / 跨平台）** | local dev、CI 测试、临时演示 | `application-dev.yml` 与 `application-test.yml` 默认 `auto-generate=true` 且 path 为空——首次启动无需任何操作即可在 Windows / macOS / Linux 上跑起来。**代价**：每次重启会生成新内存密钥，旧 lifecycle log 的签名将无法验证。 |
| **Fixed external key（dev/test 想保留历史签名 / 生产强制要求）** | local 开发希望稳定 key、所有 production 部署 | 通过环境变量指向外部 key 文件：`TRACE_SIGNATURE_PRIVATE_KEY_PATH` + `TRACE_SIGNATURE_PUBLIC_KEY_PATH`，并把 `TRACE_SIGNATURE_AUTO_GENERATE=false`。Production 由 `ProdProfileConfigGuard` 强制要求该模式。 |

> ⚠️ Production 永远走"Fixed external key"模式。`application-prod.yml` 已清空所有默认值，`ProdProfileConfigGuard` 会校验：path 必须非空、不指向 `backend/keys/*` 工作区路径、`auto-generate` 必须 false——任何一项不合格直接 fail-fast 拒绝启动。

### Signature config

dev/test 默认配置（无需任何环境变量即可启动）：

```yaml
# 摘自 application-dev.yml / application-test.yml
trace:
  signature:
    private-key-path: ${TRACE_SIGNATURE_PRIVATE_KEY_PATH:}     # 默认空
    public-key-path:  ${TRACE_SIGNATURE_PUBLIC_KEY_PATH:}      # 默认空
    key-id:           ${TRACE_SIGNATURE_KEY_ID:default}
    key-version:      ${TRACE_SIGNATURE_KEY_VERSION:1}
    auto-generate:    ${TRACE_SIGNATURE_AUTO_GENERATE:true}    # 默认 true → 内存密钥
```

**要稳定 key？** 在 dev/test 上覆盖三个环境变量即可（无需改 yml）：

```bash
# Windows PowerShell
$env:TRACE_SIGNATURE_PRIVATE_KEY_PATH = "D:/trace-runtime/keys/private_key.pem"
$env:TRACE_SIGNATURE_PUBLIC_KEY_PATH  = "D:/trace-runtime/keys/public_key.pem"
$env:TRACE_SIGNATURE_AUTO_GENERATE    = "false"

# macOS / Linux
export TRACE_SIGNATURE_PRIVATE_KEY_PATH="$HOME/.trace/keys/private_key.pem"
export TRACE_SIGNATURE_PUBLIC_KEY_PATH="$HOME/.trace/keys/public_key.pem"
export TRACE_SIGNATURE_AUTO_GENERATE="false"
```

提示：`SignatureUtil` 优先选 path，其次 `auto-generate`；只要环境变量里 path 非空就会读文件，`auto-generate` 会被忽略。

### 生产环境配置步骤

1. **生成 RSA 密钥对**（使用 OpenSSL）：
   ```bash
   # 在部署机或密钥管理流程中生成，输出目录必须位于源码工作区之外
   install -d -m 700 /etc/trace/signing
   openssl genpkey -algorithm RSA -out /etc/trace/signing/private_key.pem -pkeyopt rsa_keygen_bits:2048
   
   # 从私钥导出公钥
   openssl rsa -pubout -in /etc/trace/signing/private_key.pem -out /etc/trace/signing/public_key.pem
   ```

2. **配置密钥路径**（通过环境变量或部署挂载，不要放入工作区 `backend/keys`）：
   ```yaml
   trace:
     signature:
       auto-generate: false
       private-key-path: ${TRACE_SIGNATURE_PRIVATE_KEY_PATH:/etc/trace/signing/private_key.pem}
       public-key-path: ${TRACE_SIGNATURE_PUBLIC_KEY_PATH:/etc/trace/signing/public_key.pem}
       key-id: ${TRACE_SIGNATURE_KEY_ID:default}
       key-version: ${TRACE_SIGNATURE_KEY_VERSION:1}
   ```

3. **保护私钥文件**：
   ```bash
   chmod 400 /etc/trace/signing/private_key.pem
   chown appuser:appgroup /etc/trace/signing/private_key.pem
   ```

### 已废弃密钥轮换说明

本仓库曾包含演示用 `backend/keys/private_key.pem` / `public_key.pem`，现已按泄露密钥处理并从工作区移除。生产环境需要重新生成 RSA 密钥对，并将私钥、公钥以部署挂载或环境变量路径方式注入；历史演示密钥不得继续使用。

### Signature key metadata

Each `trace_lifecycle_log` row stores `signature_key_id` and `signature_key_version`. Chain verification first checks the key metadata declared by the log, instead of blindly verifying all historical signatures with the currently loaded public key. `GET /api/traces/public-key` also returns the current `keyId` and `keyVersion`. Existing rows can be backfilled with `backend/sql/migrate_v3_signature_key_metadata.sql` to `default` / `1`, matching the backup key pair in `D:/trace-runtime/keys`.

新写入日志的 Hash/RSA 签名载荷还包含 `operator` 和非空 `remark`；验链服务会优先按当前 operator-protected 载荷校验，失败后再尝试历史未保护 `operator` 的 legacy 载荷，以兼容旧演示数据。

### 验证接口

| 接口 | 说明 | 认证 |
|------|------|------|
| `GET /api/traces/{traceCode}/verify` | 验证溯源链完整性 | 需要登录 |
| `GET /api/traces/public-key` | 获取公钥（供第三方验证） | **无需认证** |

### 安全架构

```
┌─────────────────────────────────────────────────────────────┐
│                     数字签名验证流程                          │
├─────────────────────────────────────────────────────────────┤
│  1. 写入时：用私钥对日志数据签名 → 存入数据库 signature 字段  │
│  2. 验证时：用公钥验证签名 → 任何篡改都会导致验证失败         │
│  3. 安全保障：私钥不公开 → 无法伪造签名 → 数据不可抵赖        │
└─────────────────────────────────────────────────────────────┘
```

**为什么即使 DBA 也无法篡改数据？**
- DBA 可以修改数据库中的数据和 Hash
- 但 DBA 没有私钥，无法生成匹配的签名
- 验证接口会检测到 Hash/签名不匹配，暴露篡改行为

## 生产部署须知

> ⚠️ **以下条目是 prod 环境上线前必须确认的"硬性配置"——任何一条遗漏都会让服务在第一次真实请求到来时立刻失败或带病运行。** 完整可注入变量见 [backend/.env.example](backend/.env.example)。

### 必填环境变量（缺失即 fail-fast 拒绝启动）

| 变量 | 用途 | 缺失/默认值的后果 |
|---|---|---|
| `TRACE_JWT_SECRET` | JWT 签名密钥（≥32 字节） | 由 `ProdProfileConfigGuard` 拦截：空值、内置 dev/test 默认值、`changeme` 类占位符与 `<32` 字节都会启动失败 |
| `TRACE_DB_URL` / `TRACE_DB_USERNAME` / `TRACE_DB_PASSWORD` | 数据库连接 | 任一为空，或用 `root/root` 默认凭据，启动失败 |
| `TRACE_SIGNATURE_PRIVATE_KEY_PATH` / `TRACE_SIGNATURE_PUBLIC_KEY_PATH` | RSA 签名私/公钥路径 | 必须指向**外部挂载文件**；`backend/keys/*` 工作区默认路径会被显式拒绝 |
| `TRACE_SIGNATURE_KEY_ID` / `TRACE_SIGNATURE_KEY_VERSION` | 签名 key 元数据，写入每条 lifecycle log | 缺失会导致历史签名校验时找不到对应 key（详见 `### Signature key metadata`） |
| **`TRACE_CORS_ALLOWED_ORIGINS`** 或 **`TRACE_CORS_ALLOWED_ORIGIN_PATTERNS`** | 浏览器跨域白名单 | **二者至少一个必须显式设置**；都空时浏览器前端跨域调用会全部失败；用 `*` 单独全开会被显式拒绝 |
| `TRACE_SIGNATURE_AUTO_GENERATE` | 必须显式 `false` | `true` 会在生产环境生成临时密钥导致历史签名永远无法验证 |

> 注：`application-prod.yml` 默认值已全部清空，`config/ProdProfileConfigGuard` 在容器启动期就会校验上述变量；任何一项不合格都会抛 `IllegalStateException` 阻断启动，避免带"配置漂移"的进程对外提供服务。

### 单实例部署假设（暂时性约束）

> 🔒 **当前权限缓存为 JVM 本地（`security/permission/PermissionCache`，基于 `ConcurrentHashMap`）；多实例部署下 A 实例上的角色/权限变更不会立即同步到 B、C 实例的本地副本。**
> 因此当前架构 **仅支持单实例 prod 部署**。如需水平扩展（Pod 多副本 / 负载均衡多节点），必须先完成 `项目审查整改任务表_20260503.md` 中 **T-P1-02 中期方案**：把 `PermissionCache` 切到 Redis Hash + 短 TTL，并在角色/权限变更点 publish 失效消息。
> 在该改造完成前，多实例部署等同于"用户在 A 实例改密码 / 改权限后，B 实例上仍可能用旧 token / 旧权限继续操作"——**视同安全降级**，请勿启用。

### CORS 配置示例（生产）

```bash
# 仅允许公开前端域名（推荐）
export TRACE_CORS_ALLOWED_ORIGINS="https://trace.example.com"

# 如有多个域名，逗号分隔
export TRACE_CORS_ALLOWED_ORIGINS="https://trace.example.com,https://admin.trace.example.com"

# 移动端/扫码场景需要 IP 段时，用 patterns（仅在受信任的内网启用）
export TRACE_CORS_ALLOWED_ORIGIN_PATTERNS="https://192.168.10.*:5173"

# ❌ 错误示例：通配 * 会被 ProdProfileConfigGuard 拒绝
export TRACE_CORS_ALLOWED_ORIGINS="*"   # 启动会失败
```

## 权限系统

系统预置 6 个角色：

| 角色 | 说明 | 主要权限 |
|------|------|----------|
| SUPER_ADMIN | 超级管理员 | 所有权限，可管理其他管理员 |
| ADMIN | 系统管理员 | 所有权限，不可管理超管 |
| PRODUCER | 生产员 | 赋码批次创建、标签打印/重打/作废、扫码激活、异常上报、溯源查询、看板、配件查看 |
| WAREHOUSE | 仓管员 | 入库/出库、流转任务创建/扫码/完成、异常上报、溯源查询、看板、配件查看 |
| LOGISTICS | 物流员 | 物流流转、任务扫码/完成、异常上报、溯源查询、看板 |
| USER | 普通用户 | 溯源查询、看板 |

权限可在后台动态配置，修改后立即生效（用户需重新登录）。

## Postman 测试

项目提供完整的 Postman 测试集合：

| 文件 | 说明 |
|------|------|
| [postman/trace-api-full-tests.postman_collection.json](postman/trace-api-full-tests.postman_collection.json) | 完整 API 测试（8 模块，40+ 请求） |
| [postman/trace-local.postman_environment.json](postman/trace-local.postman_environment.json) | 本地环境变量 |

**使用步骤：**
1. 在 Postman 中导入两个文件
2. 选择 `溯源系统-本地环境` 环境
3. 按文件夹顺序运行测试

详见 [postman-guide.md](postman-guide.md)

## 前端对接提示

1. **Base URL**: `http://localhost:8080` 或 `https://localhost:8080`（摄像头扫码需要 HTTPS）
2. **请求拦截器**: 自动添加 `Authorization: Bearer <token>` 头
3. **响应拦截器**: 处理 `code !== 0` 的业务错误
4. **时间格式**: 使用 ISO-8601（如 `2026-01-16T10:30:00`）
5. **字段命名**: 前端 API 模块使用 camelCase；HTTP 层由 `request.js` 自动完成请求 snake_case 序列化和响应 camelCase 反序列化

### 登录后权限控制

登录接口现在返回 `permissions` 数组，前端可根据权限控制按钮显示：

```javascript
// 登录响应示例
{
  "token": "xxx",
  "username": "warehouse",
  "role": "WAREHOUSE",
  "permissions": ["trace:inbound", "trace:outbound", "trace:task:scan", "trace:task:complete", "trace:view", "dashboard:view", "part:view"]
}

// 前端权限判断
const canAccessScanHub = ['trace:scan', 'trace:inbound', 'trace:outbound', 'trace:transfer', 'trace:task:scan']
  .some((permission) => permissions.includes(permission))
const canInbound = permissions.includes('trace:scan') || permissions.includes('trace:inbound')
const canOutbound = permissions.includes('trace:scan') || permissions.includes('trace:outbound')
const canTransfer = permissions.includes('trace:scan') || permissions.includes('trace:transfer')
const canCreateBatch = permissions.includes('trace:batch:create') || permissions.includes('trace:create')
const canPrintCode = permissions.includes('trace:code:print') || permissions.includes('trace:create')
const canActivateCode = permissions.includes('trace:code:activate') || permissions.includes('trace:create')
const canCreateTask = permissions.includes('trace:task:create')
const canScanTask = permissions.includes('trace:task:scan')
const canViewAudit = permissions.includes('trace:audit:view')
const canHandleException = permissions.includes('trace:exception:handle') || permissions.includes('trace:scan')
const canView = permissions.includes('trace:view')      // 查询扫码
```

### 扫码流转接口

```javascript
// 普通入库扫码：fromNode 可省略，后端从快照当前节点推导
POST /api/traces/{traceCode}/events
{
  "action_type": "INBOUND",
  "to_node": "华东仓库",
  "province": "江苏省",
  "event_time": "2026-05-07T10:30:00",
  "idempotency_key": "scan-inbound-001",
  "remark": "到货外观完好"
}

// 任务内连续扫码：推荐仓库/物流日常操作使用该入口
POST /api/trace-flow-tasks/{taskId}/scan
{
  "trace_code": "CARTON-001",
  "event_time": "2026-05-07T14:30:00",
  "idempotency_key": "task-1-carton-001",
  "remark": "任务工作台扫码"
}

// 扫码后动作推荐：扫码页先调用该接口，再展示可执行按钮
GET /api/traces/{traceCode}/available-actions
```

### 赋码批次与标签接口

```javascript
// 创建赋码批次并生成单品码
POST /api/traces
{
  "part_code": "SPU-VALVE-001",
  "batch_no": "ASSIGN-20260507-0001",
  "production_order_no": "PO-20260507-01",
  "quantity": 100,
  "manufacturer_node_id": 1
}

// 打印 / 重打 / 作废 / 激活
POST /api/traces/{traceCode}/print
POST /api/traces/{traceCode}/reprint
POST /api/traces/{traceCode}/void
POST /api/trace-codes/{traceCode}/activate

// 批次对账与码列表
GET /api/trace-batches/{batchId}
GET /api/trace-batches/{batchId}/codes
```

### 聚合码接口

```javascript
// 装箱或托盘绑定
POST /api/trace-aggregations
{
  "parent_code": "CARTON-001",
  "child_code": "TRC-20260507-000001",
  "relation_type": "CARTON",
  "remark": "装箱"
}

// 解除绑定
POST /api/trace-aggregations/{relationId}/release
```

`event_time` 可省略，后端会使用服务器当前时间；一旦提供，必须是 ISO-8601 本地时间（如 `2026-05-07T10:30:00`），非法格式返回 400，不再容错解析为当前时间。

## 严格代码审查提示词与流程

如果你想让 AI 针对 **当前项目本身** 做一次严格、系统、可落地的代码审查，推荐直接使用下面这套提示词与流程。

### 可直接复制的提示词

```text
请你对当前项目做一次“严格代码审查”。

注意：
1. 这不是只看最近 diff，也不是只看单个文件；请从“整个项目当前状态”出发审查。
2. 必须基于当前仓库里的真实代码、配置、测试、README 和前后端调用关系来分析，不要泛泛而谈。
3. 分析时请同时覆盖：架构边界、设计模式、一致性、技术债、可维护性、安全、性能、并发、配置与环境隔离、前后端契约、测试覆盖、文档一致性。

我需要你重点回答这几类内容：
- 哪些地方做得好，为什么好；
- 哪些地方可能有问题，为什么可能有问题；
- 哪些属于明确的设计缺陷或实现缺陷；
- 哪些属于高风险问题，应该优先修；
- 哪些只是次要问题、技术债或文档债务；
- 哪些地方你暂时不能确定，需要标记为“待验证”。

输出要求：
- 尽量给出明确证据，优先使用 `file:line` 引用；
- 每条问题都要标注严重级别：严重 / 主要 / 次要 / 建议；
- 每条问题都要写清：现象、影响、原因分析、修复建议；
- 如果发现优点，也不要只写“代码不错”，而要说明具体优点和正面影响；
- 必须给出“优先修复顺序”，而不是只罗列问题；
- 如果 README、配置、环境变量、测试与代码实现不一致，也要算作问题；
- 如果某处只是推测，必须明确写“推测/待验证”，不要伪装成已证实结论。

请按以下结构输出：
1. 总体结论
2. 做得好的地方
3. 严重问题
4. 主要设计缺陷
5. 次要问题与技术债
6. 待验证项
7. 建议的修复优先级（P0 / P1 / P2）
8. 是否建议立即重构，以及重构范围
```

### 推荐审查流程

1. **先确认范围**
   - 默认审查仓库根目录，而不是只看单个模块。
   - 如果仓库太大，先按 `backend / frontend / docs / config / tests` 分层审查。

2. **先看运行入口和配置**
   - 后端入口、前端入口、环境变量、密钥路径、第三方依赖、日志级别、代理配置。
   - 先找“配置层风险”和“环境隔离问题”，因为这类问题经常影响全局。

3. **再看核心业务链路**
   - 登录 / 鉴权 / 权限；
   - 溯源赋码 / 扫码流转 / 验链；
   - 用户、角色、配件等核心管理模块；
   - 前后端接口契约是否一致。

4. **再看架构和边界**
   - Controller 是否过薄/过厚；
   - Service 是否职责清晰；
   - 配置、权限、缓存、并发逻辑是否分层明确；
   - 是否有硬编码、跨层耦合、职责漂移。

5. **再看质量属性**
   - 安全：密钥、Token、黑名单、权限绕过、第三方脚本、敏感信息；
   - 性能：重复查询、缓存策略、日志开销、无必要的同步/阻塞；
   - 并发：乐观锁、重试、事务边界是否正确；
   - 可维护性：重复代码、TODO、命名、模块耦合、文档过期。

6. **最后输出结构化结论**
   - 不要只说“有风险”，要说明“风险在哪里、为何成立、优先级多少、怎么改”。

### 本项目建议优先核查的点

下面这些点是基于当前仓库初步识别出的 **高优先级审查入口**，建议在严格审查时优先确认：

> **节省时间提示**：表中标记为"已整改"的条目，已在 `项目审查记录_20260503.md`、`项目审查整改任务表_20260503.md` 与 `CLAUDE.md` 的"已修复点参考"段中给出证据；新一轮审查除非能推翻这些证据，否则不必重复列入 P0/P1。仍在跟踪的整改项见 `项目审查整改任务表_20260503.md` 的"五、当前任务状态总览"。

| 审查点 | 说明 | 参考位置 |
|------|------|------|
| 自助注册链路与权限边界 | 已整改：登录页自助注册调用 `POST /api/auth/register`，仅提交用户名和密码；后台创建用户仍使用受 `user:manage` 保护的 `POST /api/users`。 | `frontend/src/shared/components/Login.vue`、`frontend/src/core/api/auth.js`、`backend/src/main/java/com/example/trace/controller/AuthController.java` |
| 密钥、凭据与环境隔离 | 已整改：配置拆分为 dev/test/prod profile；prod 对 JWT secret、数据库凭据、签名密钥路径和 key metadata 做 fail-fast 校验。 | `backend/src/main/resources/application-*.yml`、`backend/.env.example` |
| 签名私钥管理 | 已按泄露密钥处理：`backend/keys/*.pem` 已从工作区移除，默认配置不再指向本地工作区密钥；生产需通过 `TRACE_SIGNATURE_PRIVATE_KEY_PATH` / `TRACE_SIGNATURE_PUBLIC_KEY_PATH` 指向外部挂载密钥。 | `backend/src/main/resources/application-prod.yml`、`backend/.env.example` |
| Token 黑名单失效语义 | 已整改：Redis 黑名单读写异常通过 `TokenStoreException` fail-closed，认证链路返回 503，不再失败即放行。 | `backend/src/main/java/com/example/trace/security/TokenStore.java`、`backend/src/main/java/com/example/trace/security/LoginInterceptor.java` |
| 配件删除的业务完整性 | 已整改：删除前检查 `trace_snapshot.spu_id` 与 `trace_lifecycle_log.spu_id` 引用；已参与溯源的 SPU 返回 409。 | `backend/src/main/java/com/example/trace/service/impl/PartServiceImpl.java`、`backend/sql/migrate_v6_part_reference_constraints.sql` |
| 前端第三方地图依赖与 Key 暴露 | 地图 Key 已出现在环境文件中，组件会直接加载第三方脚本，需要审查密钥暴露范围、降级策略和外部依赖失败时的行为。 | `frontend/.env.example:14`、`frontend/.env.development:1`、`frontend/src/features/trace/components/TraceRouteMap.vue:59` |
| 文档与代码是否持续同步 | README 已经补充了一轮审查流程说明，但后续仍应把 `README.md`、`api-doc.md`、`postman-guide.md` 与代码实现一起审查，避免再次出现文档漂移。 | `README.md`、`api-doc.md`、`postman-guide.md` |

### 建议输出格式

推荐让审查结果至少包含下面这些字段，避免结论太虚：

```text
[级别] 标题
- 位置：file:line
- 现象：
- 影响：
- 原因分析：
- 修复建议：
- 是否建议立即处理：是 / 否 / 待验证
```

### 一个更适合当前仓库的简短版本

如果你不想输入太长提示词，可以直接用这一段：

```text
请对当前仓库做一次严格的项目级代码审查，不要只看 diff。重点检查后端鉴权与权限链路、溯源业务链、并发与事务边界、配置与密钥外置化、前后端接口契约、测试覆盖、README 与代码是否一致。必须同时指出优点、问题、设计缺陷和待验证项，并尽量给出 file:line、严重级别、影响和修复建议，最后给出 P0/P1/P2 修复顺序。
```

## 更新日志

### 2026-01-21

**摄像头扫码支持：**
- ✅ CORS 配置支持 HTTPS（摄像头访问需要 HTTPS）
- ✅ 支持局域网 IP 通配符访问（手机扫码测试）
- ✅ 登录接口返回 `permissions` 权限列表（前端控制按钮显示）
- ✅ 权限继承机制（`xxx:manage` 自动拥有 `xxx:view`）

**Dashboard 时间范围：**
- ✅ 新增 `range` 参数支持（today/7d/30d/180d/all）

**数据修复：**
- ✅ 省份名称统一为全称（如"江苏省"而非"江苏"）
- ✅ 分页 total 字段修复

**安全增强：**
- ✅ 管理员无法禁用/删除超级管理员
- ✅ 管理员只能查看和管理权限低于自己的用户
