# 后端接口对接文档（RESTful API）

> 基于 [olivewind/restful-api-specification](https://github.com/olivewind/restful-api-specification) 规范

## 基础信息

| 项目 | 说明 |
|------|------|
| Base URL (后端) | http://localhost:8080 |
| Base URL (前端开发) | https://localhost:5173 |
| Base URL (移动端测试) | https://[局域网IP]:5173 |
| 统一前缀 | `/api` |
| 鉴权方式 | Header `Authorization: Bearer <jwt-token>` |
| 放行接口 | `/api/auth/**`（登录、注册、刷新Token等） |

## 🌐 开发环境配置

### 前后端通信架构

```
移动设备 (手机浏览器)
     ↓ HTTPS
https://172.20.10.7:5173  ←─ 前端 Vite Dev Server (HTTPS)
     ↓ HTTP 代理
http://localhost:8080     ←─ 后端 Spring Boot (HTTP)
```

### HTTPS 开发环境说明

**为什么需要 HTTPS？**
- 摄像头扫码功能需要 HTTPS 环境（浏览器安全策略）
- 局域网测试（手机访问电脑）需要 HTTPS

**前端配置**：
- Vite 开发服务器启用 HTTPS（自签名证书）
- 通过 `proxy` 代理转发到后端 HTTP 服务

**后端配置**：
- 保持 HTTP 监听 8080 端口
- CORS 配置允许 HTTPS 前端跨域请求

### 跨域配置 (CORS)

后端已配置支持以下来源：

| Origin 类型 | 示例 | 说明 |
|------------|------|------|
| 本地开发 | `https://localhost:5173` | 电脑本地 HTTPS 访问 |
| 局域网 WiFi | `https://192.168.1.100:5173` | 手机连接电脑所在 WiFi |
| 手机热点 | `https://172.20.10.7:5173` | 电脑连接手机热点 |

**CORS 响应头示例**：
```
Access-Control-Allow-Origin: https://172.20.10.7:5173
Access-Control-Allow-Credentials: true
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Authorization, Content-Type, Accept, X-Requested-With
Access-Control-Max-Age: 3600
```

**注意事项**：
- ✅ 前端请求必须携带 `credentials: true`（axios 默认配置）
- ✅ 预检请求 (OPTIONS) 会自动返回 200
- ✅ 支持通配符 IP 段：`192.168.*`, `10.*`, `172.*`

### 移动端测试步骤

#### 方法 A：手机连接电脑所在 WiFi

1. 电脑和手机连接同一 WiFi
2. 查看电脑局域网 IP：
   ```bash
   # Windows
   ipconfig
   # 找到 "无线局域网适配器 WLAN" 的 IPv4 地址
   # 例如：192.168.1.100
   ```
3. 手机浏览器访问：`https://192.168.1.100:5173`
4. 接受自签名证书警告：
   - Safari: 点击"高级" → "继续前往"
   - Chrome: 点击"高级" → "继续访问"

#### 方法 B：使用手机热点（推荐）

1. 手机开启个人热点
2. 电脑连接手机热点
3. 查看电脑 IP：
   ```powershell
   # Windows PowerShell
   Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.InterfaceAlias -like "*WLAN*" }
   # 通常是 172.20.10.x 或 172.20.x.x
   ```
4. 手机浏览器访问：`https://172.20.10.x:5173`

**常见问题**：

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 连接超时 | Windows 防火墙阻止 | 添加入站规则允许 TCP 5173 |
| 证书警告不出现 | 浏览器未正确加载页面 | 清除缓存/强制刷新 |
| CORS 错误 | IP 段不在白名单 | 联系后端管理员添加 IP 段 |
| 401 Unauthorized | Token 未携带 | 检查 axios 配置 `withCredentials: true` |

## 认证机制

本系统使用 **JWT (JSON Web Token)** 进行身份认证：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| Token 有效期 | 2 小时 | 普通登录 |
| 记住登录有效期 | 1 天 | `rememberMe: true` 时 |
| 密码加密 | BCrypt | 数据库存储加密后的密码 |
| Token 黑名单 | Redis + TTL | 登出/刷新后旧 Token 立即失效 |
| Token 版本控制 | token_version | 密码/角色变更后强制失效所有 Token |

> Token 存储策略：当前 SPA 继续使用 `localStorage` + `Authorization` Header，并以 2h/1d 短有效期、Redis 黑名单 fail-closed 与前端 CSP 作补偿；详见 `docs/security/token-storage-and-csp.md`。

### JWT Token 结构

```json
{
  "sub": "username",
  "role": "ADMIN",
  "token_version": 0,
  "jti": "550e8400-e29b-41d4-a716-446655440000",
  "iat": 1737100000,
  "exp": 1737186400
}
```

| Claim | 说明 |
|-------|------|
| sub | 用户名 |
| role | 角色代码 |
| token_version | Token 版本号，用于强制失效 |
| jti | Token 唯一标识，用于黑名单 |
| iat | 签发时间 |
| exp | 过期时间 |

### Token 失效场景

| 场景 | 机制 | 说明 |
|------|------|------|
| 用户登出 | jti 加入 Redis 黑名单 | 立即失效，TTL = 剩余有效期 |
| Token 刷新 | 旧 jti 加入黑名单 | 防止旧 Token 继续使用 |
| 修改密码 | token_version 递增 | 所有旧 Token 失效 |
| 角色变更 | token_version 递增 | 管理员修改用户角色后生效 |
| 用户被禁用 | 登录拦截器校验 | 实时检查用户状态 |

## 角色体系

| 角色代码 | 角色名称 | 权限说明 |
|----------|----------|----------|
| SUPER_ADMIN | 超级管理员 | 拥有最高权限，可管理所有用户（包括其他管理员） |
| ADMIN | 系统管理员 | 可管理普通用户（不能管理其他管理员） |
| PRODUCER | 生产人员 | 可进行生产赋码、溯源查询 |
| WAREHOUSE | 仓库人员 | 可进行入库/出库操作、溯源查询 |
| LOGISTICS | 物流人员 | 可进行流转操作、溯源查询 |
| USER | 普通用户 | 仅可查询溯源信息 |

**默认账号**（密码规则：用户名 + 123456）：
- `superadmin` / `superadmin123456` - 超级管理员
- `admin` / `admin123456` - 系统管理员
- `producer` / `producer123456` - 生产人员
- `warehouse` / `warehouse123456` - 仓库人员
- `logistics` / `logistics123456` - 物流人员
- `user` / `user123456` - 普通用户

## 统一响应结构

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
| message | 可读提示信息 |
| data | 业务数据（失败时为 null） |

## 规范说明

- **请求字段**：支持 `camelCase` 和 `snake_case` 两种格式（如 `spuId` 或 `spu_id`）
- **响应字段**：统一使用 `snake_case`（如 `trace_code`、`spu_id`）
- **时间格式**：ISO-8601（如 `2026-01-16T10:30:00`）
- **空数组**：返回 `[]`，不返回 `null`
- **列表结构**：`{ "items": [], "total": 0 }`

---

## 1. 认证模块 `/api/auth`

### 1.1 用户注册

- **POST** `/api/auth/register`
- **Request Body**
  ```json
  {
    "username": "string (3-20字符，字母开头，只含字母数字下划线)",
    "password": "string (6-50字符，必须含字母和数字)"
  }
  ```
- **Response** (HTTP 201)
  ```json
  {
    "code": 0,
    "status": 201,
    "message": "注册成功",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "username": "admin",
      "role": "USER",
      "permissions": []
    }
  }
  ```

### 1.2 用户登录

- **POST** `/api/auth/login`
- **Request Body**
  ```json
  {
    "username": "string",
    "password": "string",
    "rememberMe": false
  }
  ```
  
  | 字段 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | username | string | ✅ | 用户名 |
  | password | string | ✅ | 密码 |
  | rememberMe | boolean | ❌ | 记住登录（true=1天有效，false=2小时有效） |
  
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "登录成功",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "username": "warehouse",
      "role": "WAREHOUSE",
      "permissions": ["trace:inbound", "trace:outbound", "trace:view", "dashboard:view", "part:view"]
    }
  }
  ```

  | 响应字段 | 类型 | 说明 |
  |----------|------|------|
  | token | string | JWT Token，后续请求放入 Authorization Header |
  | username | string | 用户名 |
  | role | string | 角色代码 |
  | permissions | string[] | 权限代码列表，用于前端控制按钮/菜单显示 |

  **权限代码说明：**
  | 权限代码 | 说明 | 对应功能 |
  |----------|------|----------|
  | trace:view | 溯源查询 | 查询扫码、溯源详情 |
  | trace:create | 历史兼容生产/管理权限 | 兼容旧生产赋码、节点管理等入口 |
  | trace:batch:create | 创建赋码批次 | 生产赋码批次与单品码生成 |
  | trace:code:print | 标签打印管理 | 打印、重打/补打、作废 |
  | trace:code:activate | 单品码激活 | 贴码后扫码激活/复核 |
  | trace:scan | 超级扫码权限 | 可执行入库/出库/流转/异常等所有扫码动作 |
  | trace:inbound | 入库扫码 | 仅允许入库 |
  | trace:outbound | 出库扫码 | 仅允许出库 |
  | trace:transfer | 物流流转扫码 | 仅允许流转 |
  | trace:task:create | 流转任务创建 | 仓库/物流发货、入库、接收任务 |
  | trace:task:scan | 任务内扫码 | 单品码/箱码/托盘码连续扫码 |
  | trace:task:complete | 任务完成 | 完成任务并处理少扫/多扫差异 |
  | trace:audit:view | 审计完整视图 | `GET /api/traces/{traceCode}?view=audit` |
  | trace:exception:handle | 异常处理 | 异常冻结、解除、红冲蓝补纠错 |
  | dashboard:view | 看板查看 | Dashboard 数据 |
  | user:view | 查看用户 | 用户列表 |
  | user:manage | 管理用户 | 增删改用户 |
  | role:view | 查看角色 | 角色列表 |
  | role:manage | 管理角色 | 增删改角色 |
  | part:view | 查看配件 | 配件列表 |
  | part:manage | 管理配件 | 增删改配件 |

  > **权限继承规则**：`xxx:manage` 权限自动包含 `xxx:view`；`trace:create` / `trace:scan` / `trace:inbound` / `trace:outbound` / `trace:transfer` 都自动包含 `trace:view`，但细粒度扫码权限不会自动升级为 `trace:scan`。

### 1.3 用户登出

- **POST** `/api/auth/logout`
- **Headers**: `Authorization: Bearer <token>`
- **说明**: 将 Token 加入黑名单，使其立即失效
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "登出成功",
    "data": null
  }
  ```

### 1.4 刷新 Token

- **POST** `/api/auth/refresh`
- **Headers**: `Authorization: Bearer <token>`
- **Query Params**: `remember_me` (可选，默认 false)
- **说明**: 在 Token 即将过期时获取新 Token，旧 Token 会被加入黑名单
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "Token 刷新成功",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...(新Token)",
      "username": "admin",
      "role": "ADMIN",
      "permissions": []
    }
  }
  ```

### 1.5 获取当前用户信息

- **GET** `/api/auth/me`
- **Headers**: `Authorization: Bearer <token>`
- **说明**: 获取当前登录用户的完整信息，包括权限列表
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "获取用户信息成功",
    "data": {
      "id": 1,
      "username": "admin",
      "role_code": "ADMIN",
      "role_name": "系统管理员",
      "permissions": ["user:view", "user:manage", "role:view", "role:manage"],
      "status": 1,
      "create_time": "2024-01-01 10:00:00"
    }
  }
  ```

### 1.6 修改当前用户密码

- **PUT** `/api/auth/password`
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**
  ```json
  {
    "old_password": "string (当前密码)",
    "new_password": "string (6-100字符，必须含字母和数字)"
  }
  ```
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "密码修改成功",
    "data": null
  }
  ```
- **错误情况**
  - 原密码错误：`code: 1001, message: "原密码错误"`
- **注意**：修改密码成功后，当前 Token 会立即失效，需要重新登录

---

## 2. 溯源核心业务模块

新版溯源码核心业务已从“自由扫码填表”扩展为“批次赋码 + 单品码状态 + 结构化节点 + 任务驱动连续扫码 + 箱码/托盘码聚合 + 异常/纠错审计”。接口响应仍统一使用 `ApiResponse`，前端请求可使用 `camelCase` 或 `snake_case`，响应统一为 `snake_case`。

### 2.0 追溯码分页列表（多条件筛选）

- **GET** `/api/traces`
- **权限**：`trace:view`
- **Headers**：`Authorization: Bearer <token>`
- **Query Params**

  | 参数 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | keyword | string | ❌ | 模糊匹配 trace_code / SPU 名称 / SPU 编码 / current_owner |
  | status | string | ❌ | 状态精确匹配；多值用逗号分隔（INIT/IN_STOCK/IN_TRANSIT/TRANSFERRED/EXCEPTION） |
  | spu_id | number | ❌ | SPU 主键精确匹配 |
  | batch_no | string | ❌ | 赋码批次号精确匹配 |
  | current_node | string | ❌ | 当前节点（模糊） |
  | current_owner | string | ❌ | 当前持有方（模糊） |
  | province | string | ❌ | 省份精确匹配 |
  | event_time_from | string | ❌ | last_event_time 起始（含），ISO-8601 `yyyy-MM-ddTHH:mm:ss` |
  | event_time_to | string | ❌ | last_event_time 截止（含） |
  | page | number | ❌ | 页码（默认 1） |
  | size | number | ❌ | 每页数量（默认 10，最大 200） |
  | sort | string | ❌ | 排序列：`last_event_time`(默认) / `trace_code` / `update_time` / `current_status` |
  | order | string | ❌ | `asc` / `desc`（默认 desc） |

  > 入参非法（如未知 `status` 值或 `event_time_*` 不是 ISO-8601）将返回 `code=400`，错误消息会指出具体字段。

- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": {
      "list": [
        {
          "trace_code": "TRC-20260507-000001",
          "spu_id": 1,
          "spu_part_code": "SPU-VALVE-001",
          "spu_part_name": "工业高压阀门",
          "spu_part_type": "阀门类",
          "current_status": "IN_STOCK",
          "current_node": "上海仓库",
          "current_owner": "warehouse",
          "province": "上海市",
          "city": "上海市",
          "last_event_time": "2026-05-07 11:00:00",
          "last_log_id": 95,
          "last_action_type": "INBOUND",
          "batch_id": 12,
          "batch_no": "ASSIGN-20260507-0001",
          "code_status": "IN_STOCK",
          "update_time": "2026-05-07 11:00:00"
        }
      ],
      "total": 1,
      "page": 1,
      "size": 10,
      "total_pages": 1
    }
  }
  ```

> **链验证说明**：列表接口为聚合查询，**不**返回 `prev_hash` / `current_hash` / `signature`，不在列表层执行链完整性校验。需校验某条码请走 `GET /api/traces/{trace_code}/verify`（见 2.12）。

### 2.1 生产赋码（创建赋码批次并生成单品码）

- **POST** `/api/traces`
- **权限**：`trace:batch:create` 或历史兼容 `trace:create`
- **Headers**：`Authorization: Bearer <token>`
- **Request Body**
  ```json
  {
    "spu_id": 1,
    "part_code": "SPU-VALVE-001",
    "batch_no": "ASSIGN-20260507-0001",
    "production_order_no": "PO-20260507-01",
    "quantity": 100,
    "manufacturer_node_id": 1,
    "manufacturer_node": "北京工厂",
    "province": "北京市",
    "city": "北京市"
  }
  ```

  | 字段 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | spu_id | number | ❌* | 配件ID；`spu_id` 与 `part_code` 至少提供一个 |
  | part_code | string | ❌* | 配件编码；两者都传时优先按 `part_code` 解析 |
  | batch_no | string | ❌ | 赋码批次号，最长 64；不传由后端生成 |
  | production_order_no | string | ❌ | 生产计划/工单号，最长 64 |
  | quantity | number | ✅ | 单次生成数量，范围 `1 ~ 500` |
  | manufacturer_node_id | number | ❌ | 结构化生产节点ID；传入时必须是启用的 FACTORY 节点 |
  | manufacturer_node | string | ❌ | 历史兼容的生产节点文本 |
  | province/city | string | ❌ | 生产区域；未传时可由节点资料补齐 |

- **Response** (HTTP 201)
  ```json
  {
    "code": 0,
    "status": 201,
    "message": "赋码成功",
    "data": {
      "batch_id": 12,
      "batch_no": "ASSIGN-20260507-0001",
      "requested_count": 100,
      "generated_count": 100,
      "trace_codes": ["TRC-20260507-000001", "TRC-20260507-000002"],
      "batch_status": "GENERATED",
      "partial_failure": false,
      "warning": null
    }
  }
  ```

> **一物一码说明**：批次只是管理容器；每个返回的 `trace_code` 都会写入 `trace_code` 单品码状态表，并拥有独立 `trace_snapshot` 与 `trace_lifecycle_log` 链。
> **批量持久化语义**：本接口按 `TRACE_BATCH_COMMIT_SIZE`（默认 50）分片独立提交；部分失败时前面已提交分片不会回滚，响应只返回已落库码，并通过 `partial_failure/batch_status/warning` 告知调用方。

### 2.2 标签打印、重打、作废

| 操作 | 方法与路径 | 权限 | 说明 |
|---|---|---|---|
| 打印标签 | `POST /api/traces/{trace_code}/print` | `trace:code:print` 或 `trace:create` | 写入 `PRINT_CODE`，`print_count + 1` |
| 重打/补打 | `POST /api/traces/{trace_code}/reprint` | `trace:code:print` 或 `trace:create` | 写入 `REPRINT_CODE`，必须记录补打事实 |
| 作废标签 | `POST /api/traces/{trace_code}/void` | `trace:code:print` 或 `trace:create` | 写入 `VOID_CODE`；作废码不能激活和流转 |

- **Request Body**
  ```json
  {
    "event_time": "2026-05-07T10:30:00",
    "remark": "标签损坏，补打"
  }
  ```
- **Response** (HTTP 201)
  ```json
  {
    "code": 0,
    "status": 201,
    "message": "标签重打成功",
    "data": {
      "trace_code": "TRC-20260507-000001",
      "action_type": "REPRINT_CODE",
      "code_status": "PRINTED",
      "print_count": 2,
      "lifecycle_log_id": 88,
      "event_time": "2026-05-07T10:30:00",
      "remark": "标签损坏，补打",
      "current_status": "INIT"
    }
  }
  ```

### 2.3 单品码扫码激活/复核

- **POST** `/api/trace-codes/{trace_code}/activate`
- **权限**：`trace:code:activate` 或 `trace:create`
- **Request Body**
  ```json
  {
    "event_time": "2026-05-07T10:35:00",
    "activation_node": "北京工厂",
    "device_id": "PDA-01",
    "remark": "贴码后扫码复核"
  }
  ```
- **Response** (HTTP 201)
  ```json
  {
    "code": 0,
    "status": 201,
    "message": "单品码激活成功",
    "data": {
      "trace_code": "TRC-20260507-000001",
      "action_type": "ACTIVATE_CODE",
      "code_status": "ACTIVATED",
      "activation_node": "北京工厂",
      "device_id": "PDA-01",
      "activated_by_username": "producer",
      "activated_time": "2026-05-07T10:35:00",
      "lifecycle_log_id": 89,
      "remark": "贴码后扫码复核"
    }
  }
  ```

> 未激活码（`GENERATED/PRINTED`）不能执行常规入库、出库、流转；`VOIDED/SCRAPPED` 为终态，不可复用。

### 2.4 赋码批次对账与码列表

| 方法与路径 | 权限 | 说明 |
|---|---|---|
| `GET /api/trace-batches/{batch_id}` | `trace:view` | 批次数量对账详情 |
| `GET /api/trace-batches/{batch_id}/codes` | `trace:view` | 批次下单品码列表 |

- **批次详情响应示例**
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": {
      "batch_id": 12,
      "batch_no": "ASSIGN-20260507-0001",
      "production_order_no": "PO-20260507-01",
      "spu_id": 1,
      "batch_status": "GENERATED",
      "quantity_requested": 100,
      "quantity_generated": 100,
      "quantity_printed": 80,
      "quantity_activated": 70,
      "quantity_inbound": 60,
      "quantity_voided": 2,
      "print_operation_count": 83,
      "consistent": false,
      "reconciliation_status": "DISCREPANCY",
      "discrepancy_reasons": ["激活数量少于生成数量"]
    }
  }
  ```

### 2.5 扫码流转（普通生命周期事件）

- **POST** `/api/traces/{trace_code}/events`
- **权限**：

  | action_type | 需要权限 | 说明 |
  |-------------|----------|------|
  | INBOUND | `trace:inbound` 或 `trace:scan` | 入库 |
  | OUTBOUND | `trace:outbound` 或 `trace:scan` | 出库 |
  | TRANSFER | `trace:transfer` 或 `trace:scan` | 流转/运输 |
  | EXCEPTION / EXCEPTION_OPEN | `trace:exception:handle` 或 `trace:scan` | 异常冻结 |
  | EXCEPTION_CLOSE / CORRECTION | 推荐使用专用接口 | 异常解除/审计纠错 |

- **Request Body**
  ```json
  {
    "action_type": "INBOUND",
    "to_node": "上海仓库",
    "province": "上海市",
    "city": "上海市",
    "event_time": "2026-05-07T11:00:00",
    "idempotency_key": "scan-TRC-000001-INBOUND-001",
    "remark": "到货外观完好"
  }
  ```

  | 字段 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | action_type | string | ✅ | 动作类型，见 2.13 枚举 |
  | from_node | string | ❌ | 来源节点；常规流转可省略，后端默认来自快照当前节点；伪造不一致来源会被拒绝 |
  | to_node | string | ❌ | 目标节点；任务内扫码时由任务目标节点推导 |
  | province/city | string | ❌ | 未传时沿用快照或节点资料 |
  | event_time | string | ❌ | ISO-8601 本地时间，如 `2026-05-07T11:00:00`；非法格式返回 400 |
  | idempotency_key | string | ❌ | 幂等键；同一 `trace_code + action_type + idempotency_key` 不重复写日志 |
  | correction_of | number/null | ❌ | 仅历史兼容；新纠错使用 `/corrections` |
  | remark | string | ❌ | 最长 255；非空会落库并纳入 Hash/签名 |

- **Response** (HTTP 201)
  ```json
  {
    "code": 0,
    "status": 201,
    "message": "流转记录成功",
    "data": null
  }
  ```

### 2.6 扫码后可执行动作

- **GET** `/api/traces/{trace_code}/available-actions`
- **权限**：`trace:view`
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": {
      "trace_code": "TRC-20260507-000001",
      "current_status": "IN_TRANSIT",
      "current_status_label": "运输中",
      "current_node": "北京工厂",
      "recommended_action": "INBOUND",
      "available_actions": [
        {
          "action_type": "INBOUND",
          "label": "确认入库",
          "requires_remark": false,
          "next_status": "IN_STOCK",
          "next_status_label": "在库",
          "permission_hint": "trace:inbound"
        }
      ],
      "no_action_reason": null
    }
  }
  ```

### 2.7 溯源详情：业务有效视图 / 审计完整视图

- **GET** `/api/traces/{trace_code}?view=effective|audit`
- **权限**：`trace:view`；`view=audit` 额外要求 `trace:audit:view`
- **说明**：
  - `effective` 为默认视图，会隐藏被后续 `CORRECTION` 覆盖的原始日志。
  - `audit` 返回完整日志链，并保留纠错关系。
  - 响应包含 `aggregation_history`，用于展示单品曾经所在箱码/托盘码及直接/间接关系。

- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": {
      "view": "effective",
      "snapshot": {
        "trace_code": "TRC-20260507-000001",
        "spu_id": 1,
        "current_status": "IN_STOCK",
        "current_node": "上海仓库",
        "current_owner": "warehouse",
        "province": "上海市",
        "city": "上海市",
        "last_event_time": "2026-05-07T11:00:00",
        "last_log_id": 95,
        "last_hash": "a1b2c3d4..."
      },
      "history": [
        {
          "id": 89,
          "trace_code": "TRC-20260507-000001",
          "action_type": "ACTIVATE_CODE",
          "from_node": null,
          "to_node": "北京工厂",
          "remark": "贴码后扫码复核",
          "operator": "producer",
          "signature_key_id": "default",
          "signature_key_version": 1,
          "prev_hash": "GENESIS",
          "current_hash": "abc123..."
        }
      ],
      "aggregation_history": [
        {
          "relation_id": 5,
          "parent_code": "CARTON-001",
          "child_code": "TRC-20260507-000001",
          "relation_type": "CARTON",
          "relation_type_label": "箱码",
          "active": true,
          "direct": true,
          "level": 1,
          "via_code": null
        }
      ]
    }
  }
  ```

### 2.8 异常冻结、解除与审计纠错

| 操作 | 方法与路径 | 权限 | 说明 |
|---|---|---|---|
| 异常冻结 | `POST /api/traces/{trace_code}/events`，`action_type=EXCEPTION_OPEN` | `trace:exception:handle` 或 `trace:scan` | 冻结常规流转，记录冻结前状态 |
| 解除异常 | `POST /api/traces/{trace_code}/exception/close` | `trace:exception:handle` 或 `trace:scan` | 写入 `EXCEPTION_CLOSE`，恢复冻结前状态 |
| 审计纠错 | `POST /api/traces/{trace_code}/corrections` | `trace:exception:handle` 或 `trace:scan` | 写入 `CORRECTION`，不删除原始日志 |

- **解除异常 Request**
  ```json
  {
    "remark": "复核通过，解除冻结",
    "event_time": "2026-05-07T12:00:00",
    "idempotency_key": "close-exception-001"
  }
  ```
- **审计纠错 Request**
  ```json
  {
    "correction_of": 95,
    "remark": "原事件节点录入错误，追加修正说明",
    "from_node": "北京工厂",
    "to_node": "上海仓库",
    "province": "上海市",
    "city": "上海市",
    "event_time": "2026-05-07T12:05:00",
    "idempotency_key": "correction-95-001"
  }
  ```

### 2.9 流转任务（仓库/物流工作台）

| 方法与路径 | 权限 | 说明 |
|---|---|---|
| `GET /api/trace-flow-tasks?task_type=OUTBOUND&status=CREATED` | `trace:view` | 任务列表 |
| `GET /api/trace-flow-tasks/{id}` | `trace:view` | 按ID查询任务 |
| `GET /api/trace-flow-tasks/no/{task_no}` | `trace:view` | 按任务号查询 |
| `POST /api/trace-flow-tasks` | `trace:task:create` 或兼容扫码权限 | 创建任务 |
| `POST /api/trace-flow-tasks/{id}/scan` | `trace:task:scan` 或兼容扫码权限 | 任务内连续扫码 |
| `POST /api/trace-flow-tasks/{id}/complete` | `trace:task:complete` 或兼容扫码权限 | 完成任务；数量不一致必须填写差异原因 |
| `POST /api/trace-flow-tasks/{id}/cancel` | `trace:task:create/complete` 或兼容扫码权限 | 取消任务 |

- **创建任务 Request**
  ```json
  {
    "task_no": "TASK-OUT-20260507-001",
    "task_type": "OUTBOUND",
    "source_node_id": 1,
    "target_node_id": 2,
    "expected_quantity": 100,
    "remark": "北京工厂发往上海仓"
  }
  ```
- **任务扫码 Request**
  ```json
  {
    "trace_code": "CARTON-001",
    "event_time": "2026-05-07T13:00:00",
    "idempotency_key": "task-1-scan-carton-001",
    "remark": "任务内扫码"
  }
  ```
- **任务响应关键字段**
  ```json
  {
    "id": 1,
    "task_no": "TASK-OUT-20260507-001",
    "task_type": "OUTBOUND",
    "status": "PROCESSING",
    "source_node_id": 1,
    "source_node_name": "北京工厂",
    "target_node_id": 2,
    "target_node_name": "上海仓库",
    "expected_quantity": 100,
    "actual_quantity": 20,
    "remaining_quantity": 80,
    "last_scan_trace_code": "CARTON-001",
    "duplicate_scan": false,
    "batch_scan": true,
    "batch_parent_code": "CARTON-001",
    "batch_expanded_quantity": 20,
    "batch_created_quantity": 20,
    "batch_duplicate_quantity": 0,
    "batch_skipped_quantity": 0,
    "scan_message": "批量扫码成功"
  }
  ```

### 2.10 箱码/托盘码聚合

| 方法与路径 | 权限 | 说明 |
|---|---|---|
| `GET /api/trace-aggregations[?relation_type=CARTON\|PALLET\|BATCH]` | `trace:view` | 列出全部 active 聚合（前端聚合工作台一次性加载，可按类型筛选） |
| `POST /api/trace-aggregations` | `trace:task:scan` 或兼容扫码权限 | 创建箱码/托盘码绑定 |
| `POST /api/trace-aggregations/{relation_id}/release` | `trace:task:scan` 或兼容扫码权限 | 解除聚合关系 |
| `GET /api/trace-aggregations/children?parent_code=CARTON-001` | `trace:view` | 查询有效子码 |
| `GET /api/trace-aggregations/parents?child_code=TRC-001` | `trace:view` | 查询有效父码 |
| `GET /api/trace-aggregations/history/by-parent?parent_code=CARTON-001` | `trace:view` | 父码聚合历史 |
| `GET /api/trace-aggregations/history/by-child?child_code=TRC-001` | `trace:view` | 单品聚合历史 |

- **绑定 Request**
  ```json
  {
    "parent_code": "CARTON-001",
    "child_code": "TRC-20260507-000001",
    "relation_type": "CARTON",
    "remark": "装箱"
  }
  ```
- **解除 Request**
  ```json
  {
    "remark": "拆箱复核"
  }
  ```

### 2.11 节点与用户节点绑定

| 方法与路径 | 权限 | 说明 |
|---|---|---|
| `GET /api/trace-nodes?keyword=&node_type=&enabled=` | `trace:view` | 节点列表 |
| `GET /api/trace-nodes/selectable` | `trace:view` | 可选启用节点 |
| `GET /api/trace-nodes/{id}` | `trace:view` | 节点详情 |
| `GET /api/trace-nodes/code/{node_code}` | `trace:view` | 按节点编码查询 |
| `POST /api/trace-nodes` | `trace:create` | 创建节点 |
| `PUT /api/trace-nodes/{id}` | `trace:create` | 更新节点 |
| `DELETE /api/trace-nodes/{id}` | `trace:create` | 删除节点 |
| `GET /api/users/me/trace-nodes` | `trace:view` | 当前用户自己的可操作节点 |
| `GET /api/users/{id}/trace-nodes` | `user:view` | 管理员查看用户节点绑定 |
| `PUT /api/users/{id}/trace-nodes` | `user:manage` | 管理员替换用户节点绑定 |

- **节点 Request**
  ```json
  {
    "node_code": "BJ-FACTORY-01",
    "node_name": "北京工厂",
    "node_type": "FACTORY",
    "org_id": 1001,
    "province": "北京市",
    "city": "北京市",
    "address": "工业园区1号",
    "enabled": true
  }
  ```
- **用户节点绑定 Request**
  ```json
  {
    "node_ids": [1, 2],
    "default_node_id": 1
  }
  ```

### 2.12 验证溯源链完整性与公钥

#### 2.12.1 验链

- **GET** `/api/traces/{trace_code}/verify`
- **权限**：`trace:view`
- **Response - 验证通过** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": {
      "valid": true,
      "total_logs": 5,
      "hash_verified_count": 5,
      "signature_verified_count": 5,
      "anchor_hash": "a1b2c3d4e5f6...",
      "anchor_signature": "Base64RSASignature...",
      "public_key": "MIIBIjANBgkqhkiG9w0BAQEFAAO...",
      "verify_time": "2026-05-07T15:30:00",
      "verify_duration_ms": 125,
      "errors": []
    }
  }
  ```

**错误类型说明**：
| error_type | 说明 |
|------------|------|
| `CHAIN_BROKEN` | Hash 链断裂，prevHash 不等于上一条的 currentHash |
| `HASH_MISMATCH` | Hash 不匹配，重算 Hash 与存储的不一致 |
| `SIGNATURE_INVALID` | 数字签名验证失败 |
| `SIGNATURE_MISSING` | 缺少数字签名 |
| `NO_LOGS` | 未找到任何溯源日志 |

#### 2.12.2 获取验证公钥

- **GET** `/api/traces/public-key`
- **Headers**：无需认证（公开接口）
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": {
      "public_key": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...",
      "key_id": "default",
      "key_version": "1",
      "algorithm": "RSA",
      "signature_algorithm": "SHA256withRSA"
    }
  }
  ```

**当前签名数据格式**：
```
traceCode={traceCode}|actionType={actionType}|fromNode={fromNode}|toNode={toNode}|province={province}|city={city}|eventTime={eventTime}|ingestTime={ingestTime}|prevHash={prevHash}|currentHash={currentHash}|correctionOf={correctionOf}|operator={operator}|remark={remark}
```

`operator` 已纳入新日志签名载荷；`remark` 仅在日志备注非空时追加。验链服务兼容历史未保护 `operator` 的旧签名格式。

### 2.13 当前核心枚举

| 枚举 | 取值 |
|---|---|
| action_type | `INIT`、`PRINT_CODE`、`REPRINT_CODE`、`ACTIVATE_CODE`、`VOID_CODE`、`PACK`、`UNPACK`、`PALLETIZE`、`UNPALLETIZE`、`INBOUND`、`OUTBOUND`、`TRANSFER`、`EXCEPTION`、`EXCEPTION_OPEN`、`EXCEPTION_CLOSE`、`CORRECTION` |
| trace_code.code_status | `GENERATED`、`PRINTED`、`ACTIVATED`、`IN_STOCK`、`IN_TRANSIT`、`EXCEPTION`、`VOIDED`、`SCRAPPED` |
| trace_flow_task.task_type | `OUTBOUND`、`TRANSFER`、`INBOUND`、`RECEIVE` |
| trace_flow_task.status | `CREATED`、`PROCESSING`、`COMPLETED`、`CANCELLED`、`EXCEPTION` |
| trace_aggregation.relation_type | `CARTON`、`PALLET`、`BATCH` |

---

## 3. Dashboard 模块 `/api/dashboard`

> 所有 Dashboard 接口支持 `range` 参数进行时间范围筛选

**时间范围参数说明：**

| range 值 | 说明 |
|----------|------|
| `today` | 当天数据 |
| `7d` | 近7天 |
| `30d` | 近30天（**默认值**） |
| `180d` | 近半年 |
| `all` | 全部数据 |

### 3.1 KPI 统计

- **GET** `/api/dashboard/kpi`
- **Headers**: `Authorization: Bearer <token>`
- **Query Params**:
  | 参数 | 类型 | 必填 | 默认值 | 说明 |
  |------|------|------|--------|------|
  | range | string | ❌ | 30d | 时间范围 |
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": {
      "total_traces": 500,
      "today_new": 150,
      "total_logs": 1742,
      "exception_count": 5,
      "range": "30d"
    }
  }
  ```

### 3.2 地图数据

- **GET** `/api/dashboard/map`
- **Headers**: `Authorization: Bearer <token>`
- **Query Params**:
  | 参数 | 类型 | 必填 | 默认值 | 说明 |
  |------|------|------|--------|------|
  | range | string | ❌ | 30d | 时间范围 |
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": {
      "items": [
        { "name": "浙江", "value": 120 },
        { "name": "江苏", "value": 80 }
      ],
      "total": 2,
      "range": "30d"
    }
  }
  ```
  > **性能契约（T-P1-03）**：`items` 按 `value` 降序硬上限 **50** 行；中国省级行政区共 34 个，50 留有冗余但可防止异常数据让响应体爆炸。后端使用 `idx_event_time` + `idx_province` 索引覆盖时间范围 + 省份聚合。

### 3.3 趋势数据

- **GET** `/api/dashboard/trend`
- **Headers**: `Authorization: Bearer <token>`
- **Query Params**:
  | 参数 | 类型 | 必填 | 默认值 | 说明 |
  |------|------|------|--------|------|
  | range | string | ❌ | 30d | 时间范围 |
- **说明**: `range=today` 时按小时统计，其他按天统计
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": {
      "items": [
        { "label": "01-15", "count": 45 },
        { "label": "01-16", "count": 78 }
      ],
      "total": 7,
      "range": "7d"
    }
  }
  ```
  > 注意：`range=today` 时 label 为小时（0-23），其他情况为日期

### 3.4 拓扑图数据

- **GET** `/api/dashboard/topology`
- **Headers**: `Authorization: Bearer <token>`
- **Query Params**:
  | 参数 | 类型 | 必填 | 默认值 | 说明 |
  |------|------|------|--------|------|
  | trace_code | string | ❌ | - | 溯源码（不传则查全局拓扑） |
  | range | string | ❌ | 30d | 时间范围（仅在不传 trace_code 时生效） |
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": {
      "nodes": [
        { "name": "杭州电机厂", "symbolSize": 40 },
        { "name": "上海物流中心", "symbolSize": 40 },
        { "name": "南京仓库", "symbolSize": 40 }
      ],
      "links": [
        { "source": "杭州电机厂", "target": "上海物流中心" },
        { "source": "上海物流中心", "target": "南京仓库" }
      ],
      "range": "30d"
    }
  }
  ```
  > **性能契约（T-P1-03）**：`links` 按出现频次降序硬上限 **200** 条边；后端 SQL 聚合 `(from_node, to_node)` 加 `COUNT(*) AS edge_weight ORDER BY edge_weight DESC LIMIT 200`，避免在大体量 lifecycle log 上返回数万条边。`nodes` 由实际出现的端点去重得出，因此节点数自然受 200 边的上限约束。如要查看完整拓扑，需通过 `trace_code` 参数限定单条溯源码。

---

## 4. 用户管理模块 `/api/users`

> 权限要求：`user:view`（查看）、`user:manage`（管理）

### 权限控制规则

#### 角色优先级

| 角色代码 | 优先级 | 说明 |
|---------|-------|------|
| SUPER_ADMIN | 3 | 最高权限 |
| ADMIN | 2 | 管理权限 |
| PRODUCER / WAREHOUSE / LOGISTICS / USER | 1 | 业务角色 |

#### 用户列表可见范围

| 当前登录角色 | 可见用户 |
|-------------|---------|
| SUPER_ADMIN | 所有用户 |
| ADMIN | 仅 PRODUCER、WAREHOUSE、LOGISTICS、USER |

#### 操作权限

- 只能操作**优先级低于自己**的用户
- `superadmin` 账号不能被任何人删除或禁用
- 操作违规返回 **403 Forbidden**

### 4.1 分页查询用户列表

- **GET** `/api/users`
- **Headers**: `Authorization: Bearer <token>`
- **Query Params**

  | 参数 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | username | string | ❌ | 用户名（模糊搜索） |
  | roleId | number | ❌ | 角色ID |
  | status | number | ❌ | 状态：1-启用，0-禁用 |
  | page | number | ❌ | 页码（默认1） |
  | size | number | ❌ | 每页数量（默认10） |

> ⚠️ **注意**：返回结果根据当前用户角色自动过滤，ADMIN 看不到 SUPER_ADMIN 和 ADMIN 用户

- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": {
      "list": [
        {
          "id": 1,
          "username": "admin",
          "role_id": 1,
          "role_code": "ADMIN",
          "role_name": "系统管理员",
          "status": 1,
          "create_time": "2024-01-01 10:00:00",
          "update_time": "2024-01-01 10:00:00"
        }
      ],
      "total": 5,
      "page": 1,
      "size": 10,
      "total_pages": 1
    }
  }
  ```

### 4.2 获取用户详情

- **GET** `/api/users/{id}`
- **Headers**: `Authorization: Bearer <token>`
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": {
      "id": 1,
      "username": "admin",
      "role_id": 1,
      "role_code": "ADMIN",
      "role_name": "系统管理员",
      "status": 1,
      "create_time": "2024-01-01 10:00:00",
      "update_time": "2024-01-01 10:00:00"
    }
  }
  ```

### 4.3 创建用户

- **POST** `/api/users`
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**
  ```json
  {
    "username": "newuser",
    "password": "Pass123",
    "roleId": 5,
    "status": 1
  }
  ```

  | 字段 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | username | string | ✅ | 用户名（3-50字符） |
  | password | string | ✅ | 密码（6-100字符，含字母和数字） |
  | roleId | number | ✅ | 角色ID |
  | status | number | ❌ | 状态（默认1启用） |

> ⚠️ **权限限制**：只能创建优先级低于自己的角色用户，ADMIN 不能创建 ADMIN/SUPER_ADMIN

- **Response** (HTTP 200): 返回创建的用户信息

### 4.4 更新用户

- **PUT** `/api/users/{id}`
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**（所有字段可选）
  ```json
  {
    "username": "updatedname",
    "password": "NewPass456",
    "roleId": 2,
    "status": 0
  }
  ```

### 4.5 修改用户角色

- **PATCH** `/api/users/{id}/role`
- **Headers**: `Authorization: Bearer <token>`
- **Query Params**: `roleId` (必填)

> ⚠️ **权限限制**：只能修改优先级低于自己的用户，且只能设置为优先级低于自己的角色

- **Response** (HTTP 200): 返回更新后的用户信息（同 4.2）

### 4.6 启用/禁用用户

- **PATCH** `/api/users/{id}/status`
- **Headers**: `Authorization: Bearer <token>`
- **Query Params**: `status` (1=启用，0=禁用)

> ⚠️ **权限限制**：只能操作优先级低于自己的用户，`superadmin` 账号不能被禁用

- **Response** (HTTP 200): 返回更新后的用户信息（同 4.2）

### 4.7 删除用户

- **DELETE** `/api/users/{id}`
- **Headers**: `Authorization: Bearer <token>`

> ⚠️ **权限限制**：只能删除优先级低于自己的用户，`superadmin` 账号不能被删除

- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": null
  }
  ```

### 4.8 批量删除用户

- **DELETE** `/api/users/batch`
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**
  ```json
  {
    "ids": [2, 3, 5]
  }
  ```

> ⚠️ **权限限制**：自动过滤无权限操作的用户，`superadmin` 账号不能被删除

- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": 3
  }
  ```
- **说明**: 
  - 返回实际删除的数量
  - 自动跳过 admin 账户（不会报错，只是不删除）

### 4.9 重置用户密码

- **POST** `/api/users/{id}/reset-password`
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**
  ```json
  {
    "newPassword": "NewPass123"
  }
  ```
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": null
  }
  ```
- **权限说明**: 
  - `SUPER_ADMIN`：可以重置任何用户的密码
  - `ADMIN`：只能重置非管理员（PRODUCER/WAREHOUSE/LOGISTICS/USER）的密码
  - 尝试越权操作会返回 403 错误

---

## 5. 角色管理模块 `/api/roles`

> 权限要求：`role:view`（查看）、`role:manage`（管理）

### 5.1 查询所有角色

- **GET** `/api/roles`
- **Headers**: `Authorization: Bearer <token>`
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": [
      {
        "id": 1,
        "role_code": "ADMIN",
        "role_name": "系统管理员",
        "remark": "拥有所有权限",
        "create_time": "2024-01-01 10:00:00"
      }
    ]
  }
  ```

### 5.2 获取角色详情（含权限列表）

- **GET** `/api/roles/{id}`
- **Headers**: `Authorization: Bearer <token>`
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": {
      "id": 1,
      "role_code": "ADMIN",
      "role_name": "系统管理员",
      "remark": "拥有所有权限",
      "permissions": [
        {
          "id": 1,
          "perm_code": "trace:create",
          "perm_name": "生产赋码",
          "api_method": "POST",
          "api_pattern": "/api/traces"
        }
      ],
      "create_time": "2024-01-01 10:00:00"
    }
  }
  ```

### 5.3 创建角色

- **POST** `/api/roles`
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**
  ```json
  {
    "roleCode": "INSPECTOR",
    "roleName": "质检员",
    "remark": "负责质量检查"
  }
  ```

### 5.4 更新角色

- **PUT** `/api/roles/{id}`
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**
  ```json
  {
    "roleName": "高级质检员",
    "remark": "负责高级质量检查"
  }
  ```

### 5.5 删除角色

- **DELETE** `/api/roles/{id}`
- **Headers**: `Authorization: Bearer <token>`
- **说明**: 不能删除系统预置角色，不能删除有用户关联的角色

### 5.6 分配权限给角色

- **PUT** `/api/roles/{id}/permissions`
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**
  ```json
  {
    "permissionIds": [1, 2, 3, 4]
  }
  ```

### 5.7 查询所有权限

- **GET** `/api/roles/permissions`
- **Headers**: `Authorization: Bearer <token>`
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": [
      {
        "id": 1,
        "perm_code": "trace:create",
        "perm_name": "生产赋码",
        "api_method": "POST",
        "api_pattern": "/api/traces",
        "remark": "创建溯源实例"
      }
    ]
  }
  ```

---

## 6. 配件管理模块 `/api/parts`

> 权限要求：`part:view`（查看）、`part:manage`（管理）

### 6.1 分页查询配件列表

- **GET** `/api/parts`
- **Headers**: `Authorization: Bearer <token>`
- **Query Params**

  | 参数 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | keyword | string | ❌ | **统一搜索关键词**（同时搜索 partCode 和 partName） |
  | partCode | string | ❌ | 配件编码（模糊） |
  | partName | string | ❌ | 配件名称（模糊） |
  | partType | string | ❌ | 配件类型 |
  | manufacturer | string | ❌ | 厂商（模糊） |
  | enabled | boolean | ❌ | 启停过滤；`true` 只看启用、`false` 只看禁用、不传则不过滤 |
  | page | number | ❌ | 页码（默认1） |
  | size | number | ❌ | 每页数量（默认10） |
  
  > **搜索说明**：`keyword` 是推荐的通用搜索方式，会同时匹配配件编码和配件名称。如果需要精确筛选，可以使用 `partCode`、`partName` 等独立字段。

- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": {
      "list": [
        {
          "id": 1,
          "part_code": "SPU-VALVE-001",
          "part_name": "工业高压阀门",
          "part_type": "阀门类",
          "model": "V-2024001",
          "manufacturer": "示例制造商",
          "unit": "件",
          "remark": "演示数据",
          "enabled": true,
          "create_time": "2024-01-01 10:00:00"
        }
      ],
      "total": 1,
      "page": 1,
      "size": 10,
      "total_pages": 1
    }
  }
  ```

### 6.2 获取配件详情

- **GET** `/api/parts/{id}`
- **Headers**: `Authorization: Bearer <token>`
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": {
      "id": 1,
      "part_code": "SPU-VALVE-001",
      "part_name": "工业高压阀门",
      "part_type": "阀门类",
      "model": "V-2024001",
      "manufacturer": "示例制造商",
      "unit": "件",
      "remark": "演示数据",
      "enabled": true,
      "create_time": "2024-01-01T10:00:00"
    }
  }
  ```

### 6.3 根据编码获取配件

- **GET** `/api/parts/code/{partCode}`
- **Headers**: `Authorization: Bearer <token>`
- **Response** (HTTP 200): 同 6.2

### 6.4 创建配件

- **POST** `/api/parts`
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**
  ```json
  {
    "partCode": "SPU-MOTOR-001",
    "partName": "工业电机",
    "partType": "动力设备",
    "model": "M-2024001",
    "manufacturer": "电机制造厂",
    "unit": "台",
    "remark": "备注信息"
  }
  ```

  | 字段 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | partCode | string | ✅ | 配件编码（唯一） |
  | partName | string | ✅ | 配件名称 |
  | partType | string | ✅ | 配件类型 |
  | model | string | ❌ | 型号规格 |
  | manufacturer | string | ❌ | 生产厂商 |
  | unit | string | ❌ | 计量单位 |
  | remark | string | ❌ | 备注 |

### 6.5 更新配件

- **PUT** `/api/parts/{id}`
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**（所有字段可选）
- **Response** (HTTP 200): 返回更新后的配件信息（同 6.2）

### 6.6 删除配件

- **DELETE** `/api/parts/{id}`
- **Headers**: `Authorization: Bearer <token>`
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": null
  }
  ```
- **冲突响应** (HTTP 409): 若配件已被 `trace_snapshot` 或 `trace_lifecycle_log` 引用，删除会被拒绝。
  ```json
  {
    "code": 10007,
    "status": 409,
    "message": "配件已参与溯源记录，不能删除: ids=[2]",
    "data": null
  }
  ```

### 6.7 批量删除配件

- **DELETE** `/api/parts/batch`
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**
  ```json
  {
    "ids": [2, 3, 5]
  }
  ```
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": 3
  }
  ```
- **说明**: 返回实际删除的数量；若请求 ID 中任一配件已参与溯源，整批拒绝并返回 HTTP 409，不会部分删除。

### 6.8 获取配件类型列表

- **GET** `/api/parts/types`
- **Headers**: `Authorization: Bearer <token>`
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": ["阀门类", "动力设备", "传感器"]
  }
  ```

### 6.9 获取厂商列表

- **GET** `/api/parts/manufacturers`
- **Headers**: `Authorization: Bearer <token>`
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": ["示例制造商", "电机制造厂"]
  }
  ```

### 6.10 启用配件

- **POST** `/api/parts/{id}/enable`
- **Headers**: `Authorization: Bearer <token>`
- **权限**: `part:manage`
- **说明**: 软上线已禁用的配件。已是启用状态时直接返回当前数据，不会再写一次 UPDATE。
- **Response** (HTTP 200): 返回更新后的配件信息（同 6.2，`enabled: true`）。
- **错误响应**: 配件不存在返回 HTTP 404 `{ "code": 10004, ... }`。

### 6.11 禁用配件

- **POST** `/api/parts/{id}/disable`
- **Headers**: `Authorization: Bearer <token>`
- **权限**: `part:manage`
- **说明**: 软下线配件。禁用后历史溯源数据保持不变，但**生产赋码与流转任务侧应拒绝该 SPU**（业务约束，不在数据库层硬约束）。已是禁用状态时直接返回当前数据，不会再写一次 UPDATE。
- **Response** (HTTP 200): 返回更新后的配件信息（同 6.2，`enabled: false`）。
- **错误响应**: 配件不存在返回 HTTP 404 `{ "code": 10004, ... }`。
- **与删除的关系**: 删除是物理删除，已参与溯源的 SPU 会返回 409。当 409 时推荐改用本接口禁用，保留溯源链不被打断。

---

## 7. 错误码说明

| code | status | 说明 |
|------|--------|------|
| 0 | 200/201 | 成功 |
| 10001 | 400 | 参数校验失败 |
| 10002 | 401 | 未授权/登录失效 |
| 10003 | 403 | 操作被拒绝/无权限 |
| 10004 | 404 | 资源不存在 |
| 10005 | 500 | 服务器内部错误 |
| 10006 | 400 | 请求参数错误 |
| 10007 | 409 | 资源冲突（如重复创建） |
| 11001 | 401 | 用户不存在 |
| 11002 | 401 | 密码错误 |
| 11003 | 400 | 用户名已存在 |
| 11004 | 401 | Token 无效或已过期 |
| 20001 | 404 | 溯源码不存在 |
| 20002 | 400 | 溯源码已存在 |
| 20003 | 400 | 无效的操作类型 |
| 20004 | 404 | 配件规格不存在 |

---

## 8. 管理模块 `/api/admin`

管理模块提供**示例数据生成**和**数据清理**功能，用于开发测试和系统演示。

> ⚠️ **环境要求**：这些接口默认仅在 `dev/test` 环境启用；生产/其他环境默认关闭，需显式设置 `TRACE_DEMO_DATA_ENABLED=true` 才可访问。
>
> ⚠️ **权限要求**：
> - `POST /api/admin/generate-sample-data`：需要 `trace:data:generate`
> - `DELETE /api/admin/clear-trace-data`：需要 `trace:data:clear`

### 8.1 生成示例数据

生成可通过 Hash 链和 RSA 签名验证的完整示例数据。生命周期日志使用动态近期时间线生成，默认落在仪表盘 `range=30d` 可查询窗口内，避免重新生成后地图/趋势/KPI 为空。生成逻辑同时遵循当前生命周期状态机：

```text
码状态：GENERATED -> PRINTED -> ACTIVATED
商品状态：INIT -> INBOUND -> IN_STOCK -> OUTBOUND -> IN_TRANSIT -> TRANSFER*(仍为 IN_TRANSIT) -> DELIVER -> TRANSFERRED
说明：TRANSFER 仅表示运输中转/位置更新，不代表最终交付；只有 DELIVER 会进入 TRANSFERRED 终态，终态不再允许入库。
```

- **POST** `/api/admin/generate-sample-data`
- **Headers**: `Authorization: Bearer <token>`
- **Query Params**

  | 参数 | 类型 | 必填 | 默认值 | 说明 |
  |------|------|------|--------|------|
  | count | int | ❌ | 100 | 生成的溯源码数量，范围 `1 ~ 500`；`TRACE_DEMO_DATA_MAX_GENERATE_COUNT` 可下调环境上限但不能超过硬上限 500 |

- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": {
      "batches": 25,
      "traceCodes": 500,
      "lifecycleLogs": 2860,
      "snapshots": 500,
      "flowTasks": 60,
      "flowTaskScans": 600,
      "aggregations": 330,
      "durationMillis": 1234,
      "lifecycleValidation": "OK",
      "lifecycleValidationErrors": [],
      "lifecycleModel": "码状态 GENERATED -> PRINTED -> ACTIVATED；商品状态 INIT -> INBOUND -> IN_STOCK -> OUTBOUND -> IN_TRANSIT -> TRANSFER*(仍为 IN_TRANSIT) -> INBOUND 循环 或 DELIVER -> TRANSFERRED(终态)",
      "coreLifecyclePrefix": ["INIT", "PRINT_CODE", "ACTIVATE_CODE", "INBOUND"],
      "actionCounts": {
        "INIT": 500,
        "PRINT_CODE": 500,
        "ACTIVATE_CODE": 500,
        "INBOUND": 500,
        "OUTBOUND": 350,
        "TRANSFER": 210,
        "DELIVER": 190,
        "PACK": 300,
        "PALLETIZE": 180
      },
      "snapshotStatusCounts": {
        "IN_STOCK": 150,
        "IN_TRANSIT": 160,
        "TRANSFERRED": 190
      },
      "codeStatusCounts": {
        "IN_STOCK": 150,
        "IN_TRANSIT": 160,
        "TRANSFERRED": 190
      },
      "terminalSummary": {
        "totalChains": 500,
        "finishedGoodsInboundBeforeOutboundChains": 500,
        "inStockChains": 150,
        "inTransitChains": 160,
        "deliveredTerminalChains": 190,
        "exceptionChains": 0,
        "transferredTerminalBlockedFromFurtherInbound": true,
        "transferMeansTransitOnly": true,
        "deliverMeansFinalTransferred": true
      },
      "sampleLifecyclePaths": [
        "INIT -> PRINT_CODE -> ACTIVATE_CODE -> INBOUND",
        "INIT -> PRINT_CODE -> ACTIVATE_CODE -> INBOUND -> OUTBOUND -> TRANSFER",
        "INIT -> PRINT_CODE -> ACTIVATE_CODE -> INBOUND -> OUTBOUND -> TRANSFER -> DELIVER"
      ],
      "demoTimeWindow": {
        "recentWindow": true,
        "dashboardRangeDays": 30,
        "batchCount": 25,
        "firstBatchTime": "2026-05-04T08:13:00",
        "latestBatchTime": "2026-05-27T08:13:00",
        "flowTaskBaseTime": "2026-05-19T08:13:00",
        "aggregationBaseTime": "2026-05-13T00:30:00"
      }
    }
  }
  ```

**生成的数据说明**：

| 数据类型 | 说明 |
|----------|------|
| 零部件规格 | 19 种（阀门 6 种、轴承 4 种、电机 3 种、传感器 3 种、管件 3 种） |
| 溯源快照 | 每条溯源码一个快照，包含当前状态；`TRANSFERRED` 只由 `DELIVER` 产生 |
| 生命周期日志 | 每条溯源码至少包含 `INIT -> PRINT_CODE -> ACTIVATE_CODE -> INBOUND`，后续随机 `OUTBOUND / TRANSFER / DELIVER / EXCEPTION_OPEN`；`TRANSFER` 保持 `IN_TRANSIT`，`DELIVER` 才进入终态 |
| 时间窗口 | 以调用时服务器时间动态回推，主生命周期、流转任务、装箱/上托盘日志都在最近 30 天内；默认 500 条会给仪表盘地图、趋势、KPI 提供可见数据 |
| 聚合关系 | 仅从仍在 `IN_STOCK` 的单品中挑选装箱/上托盘，避免已运输或已交付商品再被装箱 |
| 地理分布 | 覆盖多个省市区域，模拟真实供应链 |

**数据特点**：
- ✅ 完整的 Hash 链（每条日志的 `prev_hash` 指向上一条的 `current_hash`）
- ✅ 有效的 RSA 数字签名（使用系统私钥签名）
- ✅ 可通过 `/api/traces/{traceCode}/verify` 接口验证
- ✅ 状态包括：在库、运输中、已交付、异常等
- ✅ `lifecycleValidation=OK` 表示生成前已校验：不会出现 `ACTIVATE_CODE -> OUTBOUND`、不会出现 `TRANSFERRED` 后再次入库、不会把 `TRANSFER` 当最终交付

### 8.2 清空溯源数据

清除所有溯源业务数据，删除顺序按外键依赖从子表到父表执行：`trace_flow_task_scan -> trace_flow_task -> trace_aggregation -> trace_scan_idempotency -> trace_lifecycle_log -> trace_snapshot -> trace_code -> trace_assign_batch`。主数据（`trace_node` / `base_part_spec` / 用户 / 角色）不受影响。

- **DELETE** `/api/admin/clear-trace-data`
- **Headers**: `Authorization: Bearer <token>`
- **Query Params**

  | 参数 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | confirm | string | ✅ | 必须为 `DELETE_TRACE_DATA`，用于二次确认危险操作 |

- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": {
      "deletedFlowTaskScans": 700,
      "deletedFlowTasks": 60,
      "deletedAggregations": 330,
      "deletedIdempotencyKeys": 0,
      "deletedLogs": 2860,
      "deletedSnapshots": 500,
      "deletedTraceCodes": 500,
      "deletedBatches": 25
    }
  }
  ```

> ⚠️ **危险操作**：此操作不可恢复，会删除所有溯源业务数据（日志、快照、码状态、任务、聚合关系、赋码批次等）！主数据（base_part_spec / trace_node / 用户角色）不受影响。

### 8.3 使用示例

```bash
# 1. 获取 Token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "superadmin", "password": "superadmin123456"}' \
  | jq -r '.data.token')

# 2. 确保主数据存在（可重复调用，已存在会跳过）
curl -X POST "http://localhost:8080/api/admin/seed-master-data" \
  -H "Authorization: Bearer $TOKEN"

# 3. 清空旧溯源业务数据
curl -X DELETE "http://localhost:8080/api/admin/clear-trace-data?confirm=DELETE_TRACE_DATA" \
  -H "Authorization: Bearer $TOKEN"

# 4. 重新生成 500 条符合生命周期的示例数据
curl -X POST "http://localhost:8080/api/admin/generate-sample-data?count=500" \
  -H "Authorization: Bearer $TOKEN"

# 5. 查看返回 data.lifecycleValidation，应为 OK；data.demoTimeWindow.recentWindow 应为 true；也可取一个返回/列表里的 traceCode 调 verify
curl http://localhost:8080/api/traces/<traceCode>/verify \
  -H "Authorization: Bearer $TOKEN"

# 6. 仪表盘 30 天窗口应有数据
curl "http://localhost:8080/api/dashboard/kpi?range=30d" \
  -H "Authorization: Bearer $TOKEN"
curl "http://localhost:8080/api/dashboard/trend?range=30d" \
  -H "Authorization: Bearer $TOKEN"
curl "http://localhost:8080/api/dashboard/map?range=30d" \
  -H "Authorization: Bearer $TOKEN"
```

生命周期快速 SQL 自检：

```sql
SELECT current_status, COUNT(*) FROM trace_snapshot GROUP BY current_status;
SELECT code_status, COUNT(*) FROM trace_code GROUP BY code_status;
SELECT action_type, COUNT(*) FROM trace_lifecycle_log GROUP BY action_type;

-- 已交付快照必须存在 DELIVER 日志，返回 0 行才正常
SELECT s.trace_code
FROM trace_snapshot s
LEFT JOIN trace_lifecycle_log l
  ON l.trace_code = s.trace_code AND l.action_type = 'DELIVER'
WHERE s.current_status = 'TRANSFERRED'
GROUP BY s.trace_code
HAVING COUNT(l.id) = 0;

-- DELIVER 之后不应再有 INBOUND，返回 0 行才正常
SELECT d.trace_code
FROM trace_lifecycle_log d
JOIN trace_lifecycle_log i
  ON i.trace_code = d.trace_code
 AND i.action_type = 'INBOUND'
 AND i.event_time > d.event_time
WHERE d.action_type = 'DELIVER';
```

---

## 9. 旧接口路径状态

当前后端仅提供 RESTful 新路径，旧版 `/api/trace/*` 路径不再作为兼容入口暴露。

| 旧接口路径 | 当前接口路径 | 说明 |
|------------|------------|------|
| POST /api/trace/produce/assign | POST /api/traces | 生产赋码 |
| POST /api/trace/scan | POST /api/traces/{trace_code}/events | 扫码流转 |
| GET /api/trace/detail/{traceCode} | GET /api/traces/{trace_code} | 溯源详情 |

> 若仍调用旧路径，将命中 404/路由不存在；请迁移到当前接口路径。

### 9.1 新版核心业务新增路径速查

| 能力 | 当前接口路径 |
|---|---|
| 追溯码分页列表（多条件筛选） | `GET /api/traces?keyword&status&spu_id&batch_no&current_node&current_owner&province&event_time_from&event_time_to&page&size&sort&order` |
| 扫码后动作推荐 | `GET /api/traces/{trace_code}/available-actions` |
| 打印 / 重打 / 作废 | `POST /api/traces/{trace_code}/print` / `reprint` / `void` |
| 单品码激活 | `POST /api/trace-codes/{trace_code}/activate` |
| 批次对账 / 批次码列表 | `GET /api/trace-batches/{batch_id}` / `codes` |
| 流转任务 | `GET/POST /api/trace-flow-tasks`、`POST /api/trace-flow-tasks/{id}/scan|complete|cancel` |
| 箱码/托盘码聚合 | `GET /api/trace-aggregations[?relation_type=]`、`POST /api/trace-aggregations`、`GET /api/trace-aggregations/children\|parents\|history/*` |
| 结构化节点 | `GET/POST/PUT/DELETE /api/trace-nodes` |
| 用户节点绑定 | `GET /api/users/me/trace-nodes`、`GET/PUT /api/users/{id}/trace-nodes` |
| 异常解除 / 审计纠错 | `POST /api/traces/{trace_code}/exception/close`、`POST /api/traces/{trace_code}/corrections` |

---

## 10. 前端对接示例（Vue 3 + Axios）

> 以下示例是**最小化教学版**，与生产代码 `frontend/src/core/api/request.js` 在以下点上简化：
>
> - 真代码使用 `core/auth/authStorage.js` 抽象层封装 token 读写（`readToken / writeToken / clearAuthSession`），便于后续平滑迁移到 httpOnly Cookie；示例直接用 `localStorage.getItem/setItem` 演示概念
> - 真代码额外有 401/403/404/500 全局 toast、`hideErrorToast` 跳过开关、`unwrapBusinessResponse` 抽离的可测拦截器函数
> - 真代码的 `transformKeysToSnake / transformKeysToCamel` 默认覆盖**所有** query params 与 request body / response data，无需在每个 API 模块手动转换字段大小写
>
> 以教学示例为准学习「拦截器 + camelCase↔snake_case 双向转换 + 401 跳登录」的核心模式即可，工程落地直接复用 `request.js` 已经写好的逻辑，**不要把示例 copy 到生产代码里**。

### 10.1 Axios 实例配置

```javascript
// frontend/src/core/api/request.js
import axios from 'axios'
import { transformKeysToCamel, transformKeysToSnake } from '@/shared/utils/transform'

const request = axios.create({
  // Vite 代理会把 /api 转发到后端；业务 API 模块不要重复拼接 /api 前缀
  baseURL: '/api',
  timeout: 10000,
})

let unauthorizedHandler = null

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = typeof handler === 'function' ? handler : null
}

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  const nextConfig = {
    ...config,
    headers: { ...(config.headers || {}) },
  }

  if (token) {
    nextConfig.headers.Authorization = `Bearer ${token}`
  }

  if (nextConfig.params) nextConfig.params = transformKeysToSnake(nextConfig.params)
  if (nextConfig.data) nextConfig.data = transformKeysToSnake(nextConfig.data)

  return nextConfig
})

request.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data
    if (code !== 0) {
      return Promise.reject(new Error(message || '请求失败'))
    }
    return transformKeysToCamel(data)
  },
  (error) => {
    if (error.response?.status === 401 && !error.config?.url?.includes('/auth/login')) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      unauthorizedHandler?.()
    }
    return Promise.reject(error)
  }
)

export default request
```

### 10.2 API 模块封装

```javascript
// frontend/src/core/api/auth.js
import request from '@/core/api/request'

export const login = (username, password, rememberMe = false) =>
  request.post('/auth/login', { username, password, rememberMe })
export const register = (username, password) => request.post('/auth/register', { username, password })
export const getUserInfo = () => request.get('/auth/me')
export const logout = () => request.post('/auth/logout')
```

```javascript
// frontend/src/features/trace/api/trace.js
import request from '@/core/api/request'

// request.js 的 baseURL 是 /api，因此这里不再重复 /api 前缀
export const createTrace = (data) => request.post('/traces', data)
export const createEvent = (traceCode, data) => request.post(`/traces/${traceCode}/events`, data)
export const getTraceDetail = (traceCode) => request.get(`/traces/${traceCode}`)
export const verifyTraceChain = (traceCode) => request.get(`/traces/${traceCode}/verify`)
```

```javascript
// frontend/src/features/dashboard/api/dashboard.js
import request from '@/core/api/request'

export const getKPI = (range = '30d') => request.get('/dashboard/kpi', { params: { range } })
export const getMapData = (range = '30d') => request.get('/dashboard/map', { params: { range } })
export const getTrend = (range = '30d') => request.get('/dashboard/trend', { params: { range } })
export const getTopology = (traceCode, range = '30d') =>
  request.get('/dashboard/topology', { params: { traceCode, range } })
```

### 10.3 使用示例

```javascript
// 登录
const handleLogin = async () => {
  const { token, username, role, permissions } = await login('admin', 'admin123456', false)
  userStore.setUser({ username, role, permissions }, token)
}

// 生产赋码
const handleProduce = async () => {
  const result = await createTrace({
    partCode: 'SPU-VALVE-001',
    batchNo: 'ASSIGN-20260507-0001',
    productionOrderNo: 'PO-20260507-01',
    quantity: 10,              // 1 ~ 500
    manufacturerNodeId: 1
  })
  // request.js 会把响应转换为 camelCase：
  // { batchId, batchNo, requestedCount, generatedCount, traceCodes, batchStatus, partialFailure, warning }
}

// 扫码流转
const handleScan = async (traceCode) => {
  await createEvent(traceCode, {
    actionType: 'INBOUND',
    // 常规流转 fromNode 可省略，后端从当前快照或任务推导
    toNode: '上海仓库',
    province: '江苏省',
    city: '苏州市',
    eventTime: '2026-05-07T10:30:00',
    idempotencyKey: `scan-${traceCode}-inbound-001`,
    remark: '到货外观完好'
  })
}

// 任务内连续扫码
const handleTaskScan = async (taskId, traceCode) => {
  const task = await scanTraceFlowTask(taskId, {
    traceCode,
    idempotencyKey: `task-${taskId}-${traceCode}`,
    remark: '任务工作台扫码'
  })
  // task.duplicateScan / task.batchScan 可直接用于提示重复码或父码批量展开结果
}
```

### 10.4 TypeScript 类型定义

```typescript
// src/types/api.d.ts

// 统一响应结构
interface ApiResponse<T> {
  code: number
  status: number
  message: string
  data: T
}

// 登录响应
interface LoginResult {
  token: string
  username: string
  role: string
  permissions: string[]
}

// 赋码响应
interface ProduceResult {
  batch_id: number
  batch_no: string
  requested_count: number
  generated_count: number
  trace_codes: string[]
  batch_status: string
  partial_failure: boolean
  warning?: string | null
}

// 溯源快照
interface TraceSnapshot {
  trace_code: string
  spu_id: number
  current_status: string
  current_node: string
  current_owner: string
  province: string
  city: string
  last_event_time: string
  last_log_id: number
  last_hash: string
  exception_restore_status?: string | null
  exception_restore_node?: string | null
  exception_restore_owner?: string | null
  update_time: string
}

// 流转日志
interface TraceLog {
  id: number
  trace_code: string
  action_type: string
  from_node: string | null
  to_node: string
  province: string
  city: string
  remark?: string
  event_time: string
  prev_hash: string
  current_hash: string
  correction_of?: number | null
  operator: string
  signature_key_id: string
  signature_key_version: number
  create_time: string
}

// 溯源详情
interface TraceDetail {
  view: 'effective' | 'audit'
  snapshot: TraceSnapshot
  history: TraceLog[]
  aggregation_history: Array<{
    relation_id: number
    parent_code: string
    child_code: string
    relation_type: 'CARTON' | 'PALLET' | 'BATCH'
    active: boolean
    direct: boolean
    level: number
    via_code?: string | null
  }>
}

interface TraceFlowTask {
  id: number
  task_no: string
  task_type: 'OUTBOUND' | 'TRANSFER' | 'INBOUND' | 'RECEIVE'
  status: 'CREATED' | 'PROCESSING' | 'COMPLETED' | 'CANCELLED' | 'EXCEPTION'
  source_node_id: number
  target_node_id: number
  expected_quantity: number
  actual_quantity: number
  remaining_quantity: number
  duplicate_scan?: boolean
  batch_scan?: boolean
  batch_expanded_quantity?: number
  scan_message?: string
}
```


### [Archive] T24 Task 3 user role query optimization update

- Updated at: 2026-04-14 00:41
- Status: DOING at the time of this archived note
- Completed work:
  - Added TDD coverage in `frontend/src/features/user/views/__tests__/RoleList.contract.test.js` for role `permissionCount` and `getRole(id)` detail loading.
  - Updated `frontend/src/features/user/api/roles.js` so `getRoles()` and `getRole(id)` contracts are documented clearly.
  - Updated `frontend/src/features/user/views/RoleList.vue` to use `role.permissionCount` for list display and load detail `permissions` into `selectedPermissions`.
  - Added API contract assertions in `frontend/src/features/__tests__/api-contracts.test.js`.
- Files touched:
  - `frontend/src/features/user/views/__tests__/RoleList.contract.test.js`
  - `frontend/src/features/user/api/roles.js`
  - `frontend/src/features/user/views/RoleList.vue`
  - `frontend/src/features/__tests__/api-contracts.test.js`
  - legacy project remediation task table
- Verification:
  - `cd frontend && npm run test -- --run --pool=threads src/features/user/views/__tests__/RoleList.contract.test.js src/features/__tests__/api-contracts.test.js`
- Next:
  - Continue with T24 Task 4 and keep T24 marked as `DOING` until the remaining subtasks are complete.
- Reference:
  - See `docs/superpowers/plans/2026-04-13-t24-user-role-query-optimization.md` for the original T24 plan.
