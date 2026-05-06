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
  | rememberMe | boolean | ❌ | 记住登录（true=7天有效，false=24小时有效） |
  
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
  | trace:create | 生产赋码 | 生产赋码 |
  | trace:scan | 超级扫码权限 | 可执行入库/出库/流转/异常等所有扫码动作 |
  | trace:inbound | 入库扫码 | 仅允许入库 |
  | trace:outbound | 出库扫码 | 仅允许出库 |
  | trace:transfer | 物流流转扫码 | 仅允许流转 |
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

## 2. 溯源模块 `/api/traces`

### 2.1 生产赋码（创建溯源实例）

- **POST** `/api/traces`
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**
  ```json
  {
    "spu_id": 1,
    "part_code": "PART-001",
    "quantity": 100,
    "manufacturer_node": "工厂A",
    "province": "浙江省",
    "city": "杭州市"
  }
  ```
  
  | 字段 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | spu_id | number | ❌* | 配件ID（spuId 和 part_code 至少提供一个） |
  | part_code | string | ❌* | 配件编码（推荐使用，后端自动解析为 spuId） |
  | quantity | number | ✅ | 生产数量，范围 `1 ~ 500`；超限返回 `10001` / HTTP 400 |
  | manufacturer_node | string | ❌ | 生产节点名称 |
  | province | string | ❌ | 省份 |
  | city | string | ❌ | 城市 |
  
  > **注意**：`spu_id` 和 `part_code` 至少提供一个。如果两者都提供，优先使用 `part_code` 解析的 ID。推荐前端使用 `part_code`，更加直观易用。
  >
  > **批量持久化语义（T-P1-01）**：本接口已改为"内存组装 + 分批 REQUIRES_NEW 提交"。每片大小由 `TRACE_BATCH_COMMIT_SIZE`（默认 50）控制，因此 **不再保证"全或无"**——若第 N 片提交失败，前 1..N-1 片已写入数据库且不会回滚。前端在错误处理时应假定可能出现部分写入；后端日志会记录已提交片数。详见 `README.md` "批量持久化语义"段。

- **Response** (HTTP 201)
  ```json
  {
    "code": 0,
    "status": 201,
    "message": "赋码成功",
    "data": {
      "generated_count": 100,
      "trace_codes": ["TRC-20260116-001", "TRC-20260116-002"]
    }
  }
  ```

### 2.2 扫码流转（创建流转事件）

- **POST** `/api/traces/{trace_code}/events`
- **Headers**: `Authorization: Bearer <token>`
- **Path Params**: `trace_code` - 溯源码
- **权限要求**：
  | action_type | 需要权限 | 说明 |
  |-------------|----------|------|
  | INBOUND | `trace:inbound` 或 `trace:scan` | 入库操作 |
  | OUTBOUND | `trace:outbound` 或 `trace:scan` | 出库操作 |
  | TRANSFER | `trace:transfer` 或 `trace:scan` | 物流流转 |
  | 其他 | `trace:scan` | 通用扫码权限 |
  
- **Request Body**
  ```json
  {
    "action_type": "INBOUND",
    "from_node": "物流A",
    "to_node": "仓库B",
    "province": "江苏省",
    "city": "苏州市",
    "event_time": "2026-01-16T10:30:00",
    "correction_of": null,
    "remark": "到货外观完好"
  }
  ```

  | 字段 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | action_type | string | ✅ | 动作类型，见下方枚举 |
  | from_node | string | ❌ | 上游/来源节点，最长 64 字符 |
  | to_node | string | ❌ | 下游/目标节点，最长 64 字符 |
  | province | string | ❌ | 省份，最长 32 字符 |
  | city | string | ❌ | 城市，最长 32 字符 |
  | event_time | string | ❌ | 业务发生时间。非空时必须为 ISO-8601 本地时间，如 `2026-01-16T10:30:00`；省略或空值时后端使用服务器当前时间；非法格式返回 400 |
  | correction_of | number/null | ❌ | 修正原日志 ID，仅 CORRECTION 使用 |
  | remark | string | ❌ | 事件备注，最长 255 字符；非空备注会持久化并纳入 Hash/签名载荷 |
- **Response** (HTTP 201)
  ```json
  {
    "code": 0,
    "status": 201,
    "message": "流转记录成功",
    "data": null
  }
  ```

**action_type 枚举值**：
- `INIT` - 初始化/生产
- `INBOUND` - 入库
- `OUTBOUND` - 出库
- `TRANSFER` - 转移
- `EXCEPTION` - 异常
- `CORRECTION` - 修正（红冲蓝补）

> `event_time` 不再容错解析：例如 `not-a-date` 或 `2026-01-16 10:30:00` 会返回 400，不会静默写入当前时间。

### 2.3 溯源详情

- **GET** `/api/traces/{trace_code}`
- **Headers**: `Authorization: Bearer <token>`
- **Path Params**: `trace_code` - 溯源码
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": {
      "snapshot": {
        "trace_code": "TRC-20260116-001",
        "spu_id": 1,
        "current_status": "IN_STOCK",
        "current_node": "仓库B",
        "current_owner": "admin",
        "province": "江苏省",
        "city": "苏州市",
        "last_event_time": "2026-01-16T10:30:00",
        "last_log_id": 5,
        "last_hash": "a1b2c3d4...",
        "update_time": "2026-01-16T10:30:00"
      },
      "history": [
        {
          "id": 1,
          "trace_code": "TRC-20260116-001",
          "action_type": "INIT",
          "from_node": null,
          "to_node": "工厂A",
          "province": "浙江省",
          "city": "杭州市",
          "remark": "生产赋码初始化",
          "event_time": "2026-01-15T09:00:00",
          "prev_hash": "GENESIS",
          "current_hash": "abc123...",
          "signature": "Base64EncodedRSASignature...",
          "create_time": "2026-01-15T09:00:00"
        }
      ]
    }
  }
  ```

### 2.4 验证溯源链完整性

验证溯源链的完整性，包括 Hash 链连续性验证和 RSA 数字签名验证。

- **GET** `/api/traces/{trace_code}/verify`
- **Headers**: `Authorization: Bearer <token>`
- **Path Params**: `trace_code` - 溯源码
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
      "verify_time": "2026-01-18T15:30:00",
      "verify_duration_ms": 125,
      "errors": []
    }
  }
  ```
- **Response - 验证失败** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": {
      "valid": false,
      "total_logs": 5,
      "hash_verified_count": 3,
      "signature_verified_count": 4,
      "anchor_hash": null,
      "anchor_signature": null,
      "public_key": null,
      "verify_time": "2026-01-18T15:30:00",
      "verify_duration_ms": 150,
      "errors": [
        {
          "log_id": 4,
          "error_type": "HASH_MISMATCH",
          "message": "Hash 不匹配：数据可能被篡改",
          "expected": "abc123...",
          "actual": "def456...",
          "event_time": "2026-01-16T10:30:00",
          "from_node": "物流A",
          "to_node": "仓库B",
          "action_type": "INBOUND"
        },
        {
          "log_id": 5,
          "error_type": "CHAIN_BROKEN",
          "message": "Hash 链断裂：prevHash 不匹配",
          "expected": "abc123...",
          "actual": "xyz789...",
          "event_time": "2026-01-17T14:00:00",
          "from_node": "仓库B",
          "to_node": "门店C",
          "action_type": "OUTBOUND"
        }
      ]
    }
  }
  ```

**错误类型说明**：
| error_type | 说明 |
|------------|------|
| `CHAIN_BROKEN` | Hash 链断裂，prevHash 不等于上一条的 currentHash |
| `HASH_MISMATCH` | Hash 不匹配，重算 Hash 与存储的不一致，数据可能被篡改 |
| `SIGNATURE_INVALID` | 数字签名验证失败，数据可能被篡改 |
| `SIGNATURE_MISSING` | 缺少数字签名 |
| `NO_LOGS` | 未找到任何溯源日志 |

### 2.5 获取验证公钥

获取 RSA 公钥，供第三方独立验证数字签名。

- **GET** `/api/traces/public-key`
- **Headers**: 无需认证（公开接口）
- **Response** (HTTP 200)
  ```json
  {
    "code": 0,
    "status": 200,
    "message": "success",
    "data": {
      "public_key": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...",
      "algorithm": "RSA",
      "signature_algorithm": "SHA256withRSA"
    }
  }
  ```

**第三方验证流程**：
1. 调用本接口获取公钥
2. 调用 `/api/traces/{trace_code}` 获取溯源日志
3. 构建签名数据字符串（格式见下）
4. 使用公钥验证每条日志的 `signature` 字段

**签名数据格式**：
```
traceCode={traceCode}|actionType={actionType}|fromNode={fromNode}|toNode={toNode}|province={province}|city={city}|eventTime={eventTime}|ingestTime={ingestTime}|prevHash={prevHash}|currentHash={currentHash}|correctionOf={correctionOf}|remark={remark}
```

`remark` 仅在日志备注非空时追加到签名数据；空备注历史日志不追加该片段。

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

生成可通过 Hash 链和 RSA 签名验证的完整示例数据。

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
      "partSpecs": 19,
      "traceCodes": 500,
      "lifecycleLogs": 1742
    }
  }
  ```

**生成的数据说明**：

| 数据类型 | 说明 |
|----------|------|
| 零部件规格 | 19 种（阀门 6 种、轴承 4 种、电机 3 种、传感器 3 种、管件 3 种） |
| 溯源快照 | 每条溯源码一个快照，包含当前状态 |
| 生命周期日志 | 每条溯源码 3-5 条日志（INIT + 随机流转操作） |
| 地理分布 | 覆盖 31 个省市区域，模拟真实供应链 |

**数据特点**：
- ✅ 完整的 Hash 链（每条日志的 `prev_hash` 指向上一条的 `current_hash`）
- ✅ 有效的 RSA 数字签名（使用系统私钥签名）
- ✅ 可通过 `/api/traces/{traceCode}/verify` 接口验证
- ✅ 状态包括：已入库、已出库、运输中、异常等

### 8.2 清空溯源数据

清除所有溯源相关数据（trace_lifecycle_log 和 trace_snapshot 表）。

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
      "deletedLogs": 1742,
      "deletedSnapshots": 500
    }
  }
  ```

> ⚠️ **危险操作**：此操作不可恢复，会删除所有溯源日志和快照数据！零部件规格（base_part_spec）不受影响。

### 8.3 使用示例

```bash
# 1. 获取 Token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "superadmin", "password": "superadmin123456"}' \
  | jq -r '.data.token')

# 2. 清空旧数据（可选）
curl -X DELETE "http://localhost:8080/api/admin/clear-trace-data?confirm=DELETE_TRACE_DATA" \
  -H "Authorization: Bearer $TOKEN"

# 3. 生成 500 条示例数据
curl -X POST "http://localhost:8080/api/admin/generate-sample-data?count=500" \
  -H "Authorization: Bearer $TOKEN"

# 4. 验证生成的数据
curl http://localhost:8080/api/traces/TC-20260119-0001/verify \
  -H "Authorization: Bearer $TOKEN"
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

---

## 10. 前端对接示例（Vue 3 + Axios）

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
    spuId: 1,           // camelCase
    quantity: 10,        // 1 ~ 500
    manufacturerNode: '工厂A',
    province: '浙江省',
    city: '杭州市'
  })
  // result: { generatedCount: 10, traceCodes: [...] }  // 前端 request.js 已转换为 camelCase
}

// 扫码流转
const handleScan = async (traceCode) => {
  await createEvent(traceCode, {
    actionType: 'INBOUND',
    fromNode: '工厂A',
    toNode: '仓库B',
    province: '江苏省',
    city: '苏州市',
    eventTime: '2026-01-16T10:30:00',
    remark: '到货外观完好'
  })
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
}

// 赋码响应
interface ProduceResult {
  generated_count: number
  trace_codes: string[]
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
  create_time: string
}

// 溯源详情
interface TraceDetail {
  snapshot: TraceSnapshot
  history: TraceLog[]
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
