# Postman 测试指导

## 1. 环境准备

### 1.1 创建 Postman 环境变量

在 Postman 中创建新环境（如 `溯源系统-本地`），添加以下变量：

| 变量名 | 初始值 | 说明 |
|--------|--------|------|
| `base_url` | `http://localhost:8080` | 后端地址 |
| `token` | （留空） | 登录后自动填充（JWT Token） |

### 1.2 认证机制说明

本系统使用 **JWT (JSON Web Token)** 进行身份认证：

| 配置 | 说明 |
|------|------|
| Token 格式 | JWT 标准格式（Header.Payload.Signature） |
| 默认有效期 | 2 小时 |
| 记住登录有效期 | 1 天 |
| 密码加密 | BCrypt |

> 短 Token 默认值与前端 localStorage/CSP 补偿决策见 `docs/security/token-storage-and-csp.md`。

### 1.3 启动后端服务

1. **初始化数据库**（首次或重置时执行）：
   ```bash
   mysql -u root -p trace_db < backend/sql/init_schema.sql
   ```

2. **默认测试账号**（密码规则：用户名 + `123456`）：
   | 账号 | 密码 | 角色 | 说明 |
   |------|------|------|------|
   | superadmin | superadmin123456 | SUPER_ADMIN | 超级管理员，可管理所有用户 |
   | admin | admin123456 | ADMIN | 系统管理员，可管理普通用户 |
   | producer | producer123456 | PRODUCER | 生产人员 |
   | warehouse | warehouse123456 | WAREHOUSE | 仓库人员 |
   | logistics | logistics123456 | LOGISTICS | 物流人员 |
   | user | user123456 | USER | 普通用户 |

3. **启动后端**：
   ```bash
   cd backend
   mvn spring-boot:run
   ```

### 1.4 字段命名说明

| 请求（输入） | 响应（输出） |
|--------------|--------------|
| 支持 `camelCase` 或 `snake_case` | 统一使用 `snake_case` |
| 如 `spuId` 或 `spu_id` 均可 | 如 `spu_id`, `trace_code` |

---

## 2. 接口测试流程

### 2.1 用户注册

**请求配置：**
- Method: `POST`
- URL: `{{base_url}}/api/auth/register`
- Headers: 无需
- Body (raw JSON):
```json
{
  "username": "testuser",
  "password": "Test123456"
}
```

> **注意**：密码必须包含至少一个字母和一个数字，长度 6-50 字符

**预期响应 (HTTP 201)：**
```json
{
  "code": 0,
  "status": 201,
  "message": "注册成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "username": "testuser",
    "role": "USER",
    "permissions": []
  }
}
```

**自动保存 Token（Tests 脚本）：**
```javascript
if (pm.response.code === 201) {
    const jsonData = pm.response.json();
    pm.environment.set("token", jsonData.data.token);
}
```

---

### 2.2 用户登录

**请求配置：**
- Method: `POST`
- URL: `{{base_url}}/api/auth/login`
- Headers: 无需
- Body (raw JSON):
```json
{
  "username": "admin",
  "password": "admin123456",
  "rememberMe": false
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | ✅ | 用户名 |
| password | string | ✅ | 密码（默认账号密码规则：用户名 + `123456`） |
| rememberMe | boolean | ❌ | true=1天有效，false=2小时有效 |

**预期响应 (HTTP 200)：**
```json
{
  "code": 0,
  "status": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "username": "admin",
    "role": "ADMIN",
    "permissions": ["user:view", "user:manage", "role:view", "role:manage"]
  }
}
```

**自动保存 Token（Tests 脚本）：**
```javascript
if (pm.response.code === 200) {
    const jsonData = pm.response.json();
    pm.environment.set("token", jsonData.data.token);
}
```

---

### 2.3 刷新 Token

**请求配置：**
- Method: `POST`
- URL: `{{base_url}}/api/auth/refresh?remember_me=false`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |

**预期响应 (HTTP 200)：**
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

**自动更新 Token（Tests 脚本）：**
```javascript
if (pm.response.code === 200) {
    const jsonData = pm.response.json();
    pm.environment.set("token", jsonData.data.token);
}
```

---

### 2.4 获取当前用户信息

**请求配置：**
- Method: `GET`
- URL: `{{base_url}}/api/auth/me`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |

**预期响应 (HTTP 200)：**
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
    "permissions": ["user:view", "user:manage", "role:view", "role:manage", "part:view", "part:manage"],
    "status": 1,
    "create_time": "2024-01-01T10:00:00"
  }
}
```

---

### 2.5 修改当前用户密码

**请求配置：**
- Method: `PUT`
- URL: `{{base_url}}/api/auth/password`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |
  | Content-Type | `application/json` |
- Body (raw JSON):
```json
{
  "oldPassword": "admin123456",
  "newPassword": "NewPass123"
}
```

> **注意**：支持 `old_password`/`new_password`（snake_case）和 `oldPassword`/`newPassword`（camelCase）两种格式

**预期响应 (HTTP 200)：**
```json
{
  "code": 0,
  "status": 200,
  "message": "密码修改成功，请重新登录",
  "data": null
}
```

> ⚠️ **重要**：修改密码成功后，当前 Token 会立即失效，需要重新登录获取新 Token

---

### 2.6 生产赋码（需要登录）

**请求配置：**
- Method: `POST`
- URL: `{{base_url}}/api/traces`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |
  | Content-Type | `application/json` |
- Body (raw JSON):
```json
{
  "spu_id": 1,
  "quantity": 5,
  "manufacturer_node": "工厂A",
  "province": "浙江省",
  "city": "杭州市"
}
```

> `quantity` 取值范围为 `1 ~ 500`；超过上限会返回 `10001` / HTTP 400，避免单次赋码事务过大。

**预期响应 (HTTP 201)：**
```json
{
  "code": 0,
  "status": 201,
  "message": "赋码成功",
  "data": {
    "generated_count": 5,
    "trace_codes": [
      "TRC-20260116-000001",
      "TRC-20260116-000002",
      "..."
    ]
  }
}
```

**保存溯源码（Tests 脚本）：**
```javascript
if (pm.response.code === 201) {
    const jsonData = pm.response.json();
    if (jsonData.data.trace_codes && jsonData.data.trace_codes.length > 0) {
        pm.environment.set("trace_code", jsonData.data.trace_codes[0]);
    }
}
```

---

### 2.7 扫码流转（需要登录）

**请求配置：**
- Method: `POST`
- URL: `{{base_url}}/api/traces/{{trace_code}}/events`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |
  | Content-Type | `application/json` |
- Body (raw JSON):
```json
{
  "action_type": "INBOUND",
  "from_node": "工厂A",
  "to_node": "仓库B",
  "province": "江苏省",
  "city": "苏州市",
  "event_time": "2026-01-16T10:30:00",
  "correction_of": null,
  "remark": "到货外观完好"
}
```

> `remark` 为可选事件备注，最长 255 字符；非空备注会随日志落库，并纳入该日志的 Hash 与 RSA 签名载荷。
> `event_time` 可省略；非空时必须使用 ISO-8601 本地时间（示例：`2026-01-16T10:30:00`），非法格式会返回 400，不会自动写入当前时间。

**预期响应 (HTTP 201)：**
```json
{
  "code": 0,
  "status": 201,
  "message": "流转记录成功",
  "data": null
}
```

**action_type 可选值：**
- `INIT` - 初始化/生产
- `INBOUND` - 入库
- `OUTBOUND` - 出库
- `TRANSFER` - 转移
- `EXCEPTION` - 异常
- `CORRECTION` - 修正

---

### 2.8 溯源详情查询（需要登录）

**请求配置：**
- Method: `GET`
- URL: `{{base_url}}/api/traces/{{trace_code}}`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |

**预期响应 (HTTP 200)：**
```json
{
  "code": 0,
  "status": 200,
  "message": "success",
  "data": {
    "snapshot": {
      "trace_code": "TRC-20260116-000001",
      "spu_id": 1,
      "current_status": "IN_STOCK",
      "current_node": "仓库B",
      "..."
    },
    "history": [
      {
        "id": 1,
        "action_type": "INIT",
        "..."
      }
    ]
  }
}
```

---

### 2.9 Dashboard KPI（需要登录）

**请求配置：**
- Method: `GET`
- URL: `{{base_url}}/api/dashboard/kpi`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |

**预期响应 (HTTP 200)：**
```json
{
  "code": 0,
  "status": 200,
  "message": "success",
  "data": {
    "total": 5,
    "exception_count": 0
  }
}
```

---

### 2.10 Dashboard 地图数据（需要登录）

**请求配置：**
- Method: `GET`
- URL: `{{base_url}}/api/dashboard/map`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |

**预期响应 (HTTP 200)：**
```json
{
  "code": 0,
  "status": 200,
  "message": "success",
  "data": {
    "items": [
      { "name": "浙江省", "value": 5 }
    ],
    "total": 1
  }
}
```

---

### 2.11 用户登出（需要登录）

**请求配置：**
- Method: `POST`
- URL: `{{base_url}}/api/auth/logout`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |

**预期响应 (HTTP 200)：**
```json
{
  "code": 0,
  "status": 200,
  "message": "登出成功",
  "data": null
}
```

---

## 3. 错误响应测试

### 3.1 未授权（无 Token）

访问任何需要登录的接口时不携带 Token：

**预期响应 (HTTP 401)：**
```json
{
  "code": 10002,
  "status": 401,
  "message": "未登录或 token 已失效",
  "data": null
}
```

### 3.2 溯源码不存在

**请求：**
- Method: `GET`
- URL: `{{base_url}}/api/traces/NOT-EXIST-CODE`

**预期响应 (HTTP 404)：**
```json
{
  "code": 20001,
  "status": 404,
  "message": "溯源码不存在",
  "data": null
}
```

### 3.3 用户名已存在（注册重复用户）

**预期响应 (HTTP 400)：**
```json
{
  "code": 11003,
  "status": 400,
  "message": "用户名已存在",
  "data": null
}
```

### 3.4 密码错误

**预期响应 (HTTP 401)：**
```json
{
  "code": 11002,
  "status": 401,
  "message": "密码错误",
  "data": null
}
```

---

## 4. 用户管理测试（需要 ADMIN 角色）

> ⚠️ 以下接口需要 `user:view` 或 `user:manage` 权限，请先用 admin 账号登录

### 4.1 分页查询用户列表

**请求配置：**
- Method: `GET`
- URL: `{{base_url}}/api/users?page=1&size=10`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |

**可选查询参数：**
| 参数 | 说明 |
|------|------|
| username | 用户名模糊搜索 |
| roleId | 角色ID筛选 |
| status | 状态筛选（1=启用，0=禁用） |
| page | 页码（默认1） |
| size | 每页数量（默认10） |

**预期响应 (HTTP 200)：**
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

---

### 4.2 获取用户详情

**请求配置：**
- Method: `GET`
- URL: `{{base_url}}/api/users/1`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |

---

### 4.3 创建用户

**请求配置：**
- Method: `POST`
- URL: `{{base_url}}/api/users`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |
  | Content-Type | `application/json` |
- Body (raw JSON):
```json
{
  "username": "newuser",
  "password": "Pass123",
  "roleId": 5,
  "status": 1
}
```

**预期响应 (HTTP 200)：**
```json
{
  "code": 0,
  "status": 200,
  "message": "success",
  "data": {
    "id": 6,
    "username": "newuser",
    "role_id": 5,
    "role_code": "USER",
    "role_name": "普通用户",
    "status": 1,
    "create_time": "2026-01-17 10:00:00",
    "update_time": "2026-01-17 10:00:00"
  }
}
```

---

### 4.4 更新用户信息

**请求配置：**
- Method: `PUT`
- URL: `{{base_url}}/api/users/6`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |
  | Content-Type | `application/json` |
- Body (raw JSON):
```json
{
  "username": "updateduser",
  "roleId": 2
}
```

---

### 4.5 修改用户角色

**请求配置：**
- Method: `PATCH`
- URL: `{{base_url}}/api/users/6/role?roleId=3`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |

---

### 4.6 启用/禁用用户

**请求配置：**
- Method: `PATCH`
- URL: `{{base_url}}/api/users/6/status?status=0`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |

---

### 4.7 重置用户密码

**请求配置：**
- Method: `POST`
- URL: `{{base_url}}/api/users/6/reset-password`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |
  | Content-Type | `application/json` |
- Body (raw JSON):
```json
{
  "newPassword": "NewPass123"
}
```

---

### 4.8 删除用户

**请求配置：**
- Method: `DELETE`
- URL: `{{base_url}}/api/users/6`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |

> ⚠️ 注意：不能删除 admin 账户

---

### 4.9 批量删除用户

**请求配置：**
- Method: `DELETE`
- URL: `{{base_url}}/api/users/batch`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |
  | Content-Type | `application/json` |
- Body (raw JSON):
```json
{
  "ids": [7, 8, 9]
}
```

**预期响应 (HTTP 200)：**
```json
{
  "code": 0,
  "status": 200,
  "message": "success",
  "data": 3
}
```

> ⚠️ 注意：
> - 返回实际删除的数量
> - 自动跳过 admin 账户（不会报错，只是不删除）

---

## 5. 角色管理测试（需要 ADMIN 角色）

> ⚠️ 以下接口需要 `role:view` 或 `role:manage` 权限

### 5.1 查询所有角色

**请求配置：**
- Method: `GET`
- URL: `{{base_url}}/api/roles`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |

**预期响应 (HTTP 200)：**
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
    },
    {
      "id": 2,
      "role_code": "PRODUCER",
      "role_name": "生产员",
      "remark": "负责生产赋码",
      "create_time": "2024-01-01 10:00:00"
    }
  ]
}
```

---

### 5.2 获取角色详情（含权限列表）

**请求配置：**
- Method: `GET`
- URL: `{{base_url}}/api/roles/1`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |

**预期响应 (HTTP 200)：**
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

---

### 5.3 查询所有权限

**请求配置：**
- Method: `GET`
- URL: `{{base_url}}/api/roles/permissions`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |

**预期响应 (HTTP 200)：**
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

### 5.4 创建角色

**请求配置：**
- Method: `POST`
- URL: `{{base_url}}/api/roles`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |
  | Content-Type | `application/json` |
- Body (raw JSON):
```json
{
  "roleCode": "INSPECTOR",
  "roleName": "质检员",
  "remark": "负责质量检查"
}
```

---

### 5.5 更新角色

**请求配置：**
- Method: `PUT`
- URL: `{{base_url}}/api/roles/6`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |
  | Content-Type | `application/json` |
- Body (raw JSON):
```json
{
  "roleName": "高级质检员",
  "remark": "负责高级质量检查"
}
```

---

### 5.6 分配权限给角色

**请求配置：**
- Method: `PUT`
- URL: `{{base_url}}/api/roles/6/permissions`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |
  | Content-Type | `application/json` |
- Body (raw JSON):
```json
{
  "permissionIds": [1, 3, 4]
}
```

> 说明：分配 trace:create, trace:view, dashboard:view 权限

---

### 5.7 删除角色

**请求配置：**
- Method: `DELETE`
- URL: `{{base_url}}/api/roles/6`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |

> ⚠️ 注意：不能删除系统预置角色（ADMIN, PRODUCER, WAREHOUSE, LOGISTICS, USER）

---

## 6. 配件管理测试

> ⚠️ 以下接口需要 `part:view` 或 `part:manage` 权限

### 6.1 分页查询配件列表

**请求配置：**
- Method: `GET`
- URL: `{{base_url}}/api/parts?page=1&size=10`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |

**可选查询参数：**
| 参数 | 说明 |
|------|------|
| partCode | 配件编码模糊搜索 |
| partName | 配件名称模糊搜索 |
| partType | 配件类型筛选 |
| manufacturer | 厂商模糊搜索 |
| page | 页码（默认1） |
| size | 每页数量（默认10） |

**预期响应 (HTTP 200)：**
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

---

### 6.2 获取配件详情

**请求配置：**
- Method: `GET`
- URL: `{{base_url}}/api/parts/1`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |

---

### 6.3 根据编码获取配件

**请求配置：**
- Method: `GET`
- URL: `{{base_url}}/api/parts/code/SPU-VALVE-001`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |

---

### 6.4 获取配件类型列表

**请求配置：**
- Method: `GET`
- URL: `{{base_url}}/api/parts/types`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |

**预期响应 (HTTP 200)：**
```json
{
  "code": 0,
  "status": 200,
  "message": "success",
  "data": ["阀门类", "动力设备", "传感器"]
}
```

---

### 6.5 获取厂商列表

**请求配置：**
- Method: `GET`
- URL: `{{base_url}}/api/parts/manufacturers`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |

---

### 6.6 创建配件

**请求配置：**
- Method: `POST`
- URL: `{{base_url}}/api/parts`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |
  | Content-Type | `application/json` |
- Body (raw JSON):
```json
{
  "partCode": "SPU-MOTOR-001",
  "partName": "工业电机",
  "partType": "动力设备",
  "model": "M-2024001",
  "manufacturer": "电机制造厂",
  "unit": "台",
  "remark": "测试数据"
}
```

---

### 6.7 更新配件

**请求配置：**
- Method: `PUT`
- URL: `{{base_url}}/api/parts/2`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |
  | Content-Type | `application/json` |
- Body (raw JSON):
```json
{
  "partName": "高效工业电机",
  "model": "M-2024002"
}
```

---

### 6.8 删除配件

**请求配置：**
- Method: `DELETE`
- URL: `{{base_url}}/api/parts/2`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |

**冲突响应 (HTTP 409)：**
若该配件已经被溯源快照或生命周期日志引用：
```json
{
  "code": 10007,
  "status": 409,
  "message": "配件已参与溯源记录，不能删除: ids=[2]",
  "data": null
}
```

---

### 6.9 批量删除配件

**请求配置：**
- Method: `DELETE`
- URL: `{{base_url}}/api/parts/batch`
- Headers:
  | Key | Value |
  |-----|-------|
  | Authorization | `Bearer {{token}}` |
  | Content-Type | `application/json` |
- Body (raw JSON):
```json
{
  "ids": [3, 4, 5]
}
```

**预期响应 (HTTP 200)：**
```json
{
  "code": 0,
  "status": 200,
  "message": "success",
  "data": 3
}
```

> 返回实际删除的数量
> 若任一 ID 已参与溯源，后端会整批拒绝并返回 HTTP 409，不会部分删除。

---

## 7. 权限测试场景

### 7.1 无权限访问

用普通用户（USER 角色）登录后，尝试访问管理接口：

**请求：**
- Method: `GET`
- URL: `{{base_url}}/api/users`

**预期响应 (HTTP 403)：**
```json
{
  "code": 10003,
  "status": 403,
  "message": "无权限访问此接口",
  "data": null
}
```

### 7.2 角色权限对照表

| 角色 | trace:create | trace:scan | trace:inbound | trace:outbound | trace:transfer | trace:view | dashboard:view | user:* | role:* | part:view | part:manage |
|------|:------------:|:----------:|:-------------:|:--------------:|:--------------:|:----------:|:--------------:|:------:|:------:|:---------:|:-----------:|
| ADMIN | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| PRODUCER | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ | ❌ | ✅ | ❌ |
| WAREHOUSE | ❌ | ❌ | ✅ | ✅ | ❌ | ✅ | ✅ | ❌ | ❌ | ✅ | ❌ |
| LOGISTICS | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| USER | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |

---

## 8. 测试流程建议

推荐按以下顺序执行完整测试：

### 8.1 基础流程测试

1. **注册** → 获取 token（新用户）
2. **登录** → 用 admin 账号登录
3. **生产赋码** → 创建溯源实例
4. **扫码流转** → 添加入库/出库/物流记录
5. **溯源详情** → 查询完整链路
6. **Dashboard 系列** → 验证统计数据

### 8.2 管理功能测试

7. **用户管理** → CRUD 测试（需 admin 登录）
8. **角色管理** → CRUD 和权限分配测试
9. **配件管理** → CRUD 测试

### 8.3 权限测试

10. **权限验证** → 用不同角色登录测试权限控制
11. **错误场景** → 验证异常处理

### 8.4 会话测试

12. **刷新 Token** → 测试 Token 刷新
13. **登出** → 结束会话并验证 Token 失效

---

## 9. Postman Collection 导入提示

如果需要快速导入，可以创建 Postman Collection 并设置：

1. **Collection Variables**：
   - `base_url`: `http://localhost:8080`
   - `token`: 留空

2. **Pre-request Script**（Collection 级别，自动添加 Bearer Token）：
```javascript
const token = pm.environment.get("token");
if (token) {
    pm.request.headers.add({
        key: "Authorization",
        value: "Bearer " + token
    });
}
```

3. **Tests**（Collection 级别，通用响应验证）：
```javascript
pm.test("Response has correct structure", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property("code");
    pm.expect(jsonData).to.have.property("status");
    pm.expect(jsonData).to.have.property("message");
});
```
