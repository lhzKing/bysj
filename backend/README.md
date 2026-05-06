# 工业溯源系统 - 后端服务

基于 **Spring Boot 3 + MyBatis-Plus** 的工业零配件供应链溯源系统后端服务。

---

## 📋 目录

- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [HTTPS 开发环境配置](#https-开发环境配置)
- [数据库初始化](#数据库初始化)
- [API 文档](#api-文档)
- [目录结构](#目录结构)
- [安全机制](#安全机制)
- [常见问题](#常见问题)

---

## 🛠️ 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2+ | 核心框架 |
| Spring Security | 6.2+ | 认证授权（自定义 JWT） |
| MyBatis-Plus | 3.5+ | ORM 框架 + 乐观锁 |
| MySQL | 8.0+ | 业务数据持久化 |
| Redis | 7.0+ | Token 黑名单 + 缓存 |
| JWT (jjwt) | 0.12+ | Token 生成与验证 |
| Bouncy Castle | 1.77+ | RSA 签名（溯源防篡改） |
| Lombok | 1.18+ | 简化 POJO 代码 |

---

## 🚀 快速开始

### 环境要求

- **JDK**: 17+
- **Maven**: 3.8+
- **MySQL**: 8.0+
- **Redis**: 7.0+ (无密码配置)

### 1. 克隆项目

```bash
cd backend
```

### 2. 配置 profile 与数据库

配置文件已按 profile 拆分：

- 公共配置：`src/main/resources/application.yml`
- 开发：`application-dev.yml`（默认 profile）
- 测试：`application-test.yml`
- 生产：`application-prod.yml`（缺少 JWT secret、数据库凭据、签名密钥会 fail-fast）

本地开发建议通过环境变量覆盖数据库连接，而不是把密码写入源码文件：

```powershell
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:TRACE_DB_URL = 'jdbc:mysql://localhost:3306/trace_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai'
$env:TRACE_DB_USERNAME = '<your-mysql-user>'
$env:TRACE_DB_PASSWORD = '<your-mysql-password>'
```

生产环境请以 `SPRING_PROFILES_ACTIVE=prod` 启动，并按 `backend/.env.example` 注入所有必需变量。

### 3. 初始化数据库

执行 SQL 脚本（按顺序）：

```bash
# 1. 创建数据库和表结构
mysql -u root -p < sql/init_schema.sql

# 2. 插入测试数据（可选）
mysql -u root -p < sql/sample_data.sql
```

### 4. Configure RSA signing keys

**Dev/test: fixed external backup key**

- `application-dev.yml` and `application-test.yml` default to `D:/trace-runtime/keys/private_key.pem` and `D:/trace-runtime/keys/public_key.pem`.
- The files are outside the source workspace, so restarts keep the same key and historical signatures remain verifiable.
- Use `TRACE_SIGNATURE_AUTO_GENERATE=true` only for throwaway experiments; that mode changes keys after restart.

**Production: fixed external mounted key**

```bash
install -d -m 700 /etc/trace/signing
openssl genpkey -algorithm RSA -out /etc/trace/signing/private_key.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in /etc/trace/signing/private_key.pem -out /etc/trace/signing/public_key.pem
chmod 400 /etc/trace/signing/private_key.pem
```

### 5. 启动后端

**方法 A：Maven 启动（开发）**

```bash
mvn spring-boot:run
```

**方法 B：打包后启动（生产）**

```bash
mvn clean package -DskipTests
java -jar target/trace-backend-0.0.1-SNAPSHOT.jar
```

启动成功后，访问：http://localhost:8080

---

## ⚙️ 配置说明

### profile 与核心配置

#### 服务器配置

```yaml
server:
  port: ${TRACE_SERVER_PORT:8080}
```

#### JWT 配置

```yaml
jwt:
  secret: ${TRACE_JWT_SECRET:}          # prod 必填，至少 32 bytes；dev/test 在 profile 文件中提供开发专用值
  expiration: ${TRACE_JWT_EXPIRATION_HOURS:2}
  remember-expiration: ${TRACE_JWT_REMEMBER_EXPIRATION_DAYS:1}
```

⚠️ `prod` profile 下缺少 `TRACE_JWT_SECRET`、使用旧默认/dev/test secret 或 secret 过短都会启动失败；不要把生产 secret 写入源码配置。

Token 默认有效期已作为 localStorage 风险补偿措施收敛为 2 小时 / 记住登录 1 天，前端 CSP 与存储决策见 `docs/security/token-storage-and-csp.md`。

#### CORS 跨域配置（重要）

```yaml
cors:
  # 精确匹配的源列表
  allowed-origins:
    - http://localhost:5173
    - https://localhost:5173
    - http://127.0.0.1:5173
    - https://127.0.0.1:5173
  
  # 支持通配符的源模式（用于局域网开发）
  allowed-origin-patterns:
    - https://192.168.*:5173  # 局域网 192.168.x.x
    - https://10.*:5173       # 局域网 10.x.x.x
    - https://172.*:5173      # 手机热点 172.x.x.x
    - http://192.168.*:5173
    - http://10.*:5173
    - http://172.*:5173
  
  # 允许携带凭据（Cookie/Authorization Header）
  allow-credentials: true
```

**说明**：
- `allowed-origins`：精确匹配，适用于固定域名（如生产环境）
- `allowed-origin-patterns`：支持通配符 `*`，适用于开发环境的动态 IP
- `allow-credentials: true`：允许前端发送 Cookie 和 Authorization 请求头（JWT 认证必需）

#### Trace signature config

> Rotation note: the old `backend/keys/private_key.pem` / `public_key.pem` files were treated as leaked demo keys and removed from the workspace. The current local backup key pair is stored outside the repo at `D:/trace-runtime/keys`.

```yaml
trace:
  signature:
    private-key-path: ${TRACE_SIGNATURE_PRIVATE_KEY_PATH:/etc/trace/signing/private_key.pem}  # external RSA private key path
    public-key-path: ${TRACE_SIGNATURE_PUBLIC_KEY_PATH:/etc/trace/signing/public_key.pem}    # external RSA public key path
    key-id: ${TRACE_SIGNATURE_KEY_ID:default}
    key-version: ${TRACE_SIGNATURE_KEY_VERSION:1}
    auto-generate: false  # production must use a fixed external key pair
```

#### Signature key metadata

- New logs store `trace_lifecycle_log.signature_key_id` and `signature_key_version` from `TRACE_SIGNATURE_KEY_ID` / `TRACE_SIGNATURE_KEY_VERSION`.
- Chain verification checks the key metadata stored on each log before verifying the signature; it no longer blindly uses the current public key for all historical rows.
- Run `backend/sql/migrate_v3_signature_key_metadata.sql` to backfill existing rows to `default` / `1`, matching the current backup key pair in `D:/trace-runtime/keys`.
- `/api/traces/public-key` returns `publicKey`, `keyId`, `keyVersion`, and algorithm metadata.

⚠️ **生产环境注意事项**：
- `auto-generate` 必须设为 `false`
- 密钥文件路径使用绝对路径，并指向源码目录之外的部署挂载位置
- 私钥文件妥善保管，不要提交到版本控制；仓库内 `backend/keys/*.pem` 已废弃并移除

---

## 🔐 HTTPS 开发环境配置

### 为什么需要 HTTPS？

前端使用摄像头扫描二维码需要 **HTTPS 环境**，否则浏览器会阻止摄像头访问。

### 后端配置（保持 HTTP）

后端服务器继续监听 **HTTP 8080 端口**，无需配置 SSL 证书。

前端 Vite 开发服务器配置 HTTPS，通过代理转发到后端 HTTP 服务：

```javascript
// frontend/vite.config.js
export default defineConfig({
  server: {
    https: {
      key: fs.readFileSync('./certs/localhost.key'),
      cert: fs.readFileSync('./certs/localhost.crt'),
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  // 后端 HTTP 地址
        changeOrigin: true,
        secure: false,  // 允许 HTTPS -> HTTP
        ws: true        // 支持 WebSocket
      }
    }
  }
})
```

### CORS 白名单配置

已在 `application.yml` 中配置支持 HTTPS 前端：

```yaml
cors:
  allowed-origins:
    - https://localhost:5173  # ✅ 电脑本地 HTTPS 访问
  allowed-origin-patterns:
    - https://192.168.*:5173  # ✅ 局域网 HTTPS 访问
    - https://10.*:5173       # ✅ 局域网 HTTPS 访问
    - https://172.*:5173      # ✅ 手机热点 HTTPS 访问
```

### 移动端测试配置

**方法 A：手机连接电脑所在 WiFi**

1. 电脑和手机连接同一 WiFi
2. 查看电脑局域网 IP：
   ```powershell
   # Windows
   ipconfig
   # 找到 "无线局域网适配器 WLAN" 的 IPv4 地址，例如 192.168.1.100
   ```
3. 手机浏览器访问：`https://192.168.1.100:5173`
4. 接受自签名证书警告

**方法 B：手机热点共享（推荐）**

1. 手机开启个人热点
2. 电脑连接手机热点
3. 查看电脑 IP：
   ```powershell
   Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.InterfaceAlias -like "*WLAN*" }
   # 通常是 172.20.10.x
   ```
4. 手机浏览器访问：`https://172.20.10.x:5173`

**注意**：手机热点的 IP 段通常是 `172.*`，已在 CORS 白名单中。

### CORS 过滤器工作原理

后端包含一个高优先级 `CorsFilter`：

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {
    // 1. 在所有拦截器之前执行
    // 2. 检查 Origin 是否在白名单（支持通配符匹配）
    // 3. 设置 CORS 响应头
    // 4. OPTIONS 预检请求直接返回 200
}
```

**优势**：
- 作为项目唯一 CORS 响应头写入点，不再同时维护 `WebMvcConfig.addCorsMappings`
- 优先级高于登录/权限拦截器，拦截器拒绝响应也能带上同一套 CORS Header
- 支持灵活的 IP 段匹配（192.168.*, 10.*, 172.*）
- 自动处理 OPTIONS 预检请求

---

## 💾 数据库初始化

### SQL 脚本说明

| 脚本文件 | 用途 | 执行顺序 |
|----------|------|----------|
| `sql/init_schema.sql` | 创建数据库和表结构 | 1️⃣ 首次执行 |
| `sql/sample_data.sql` | 插入测试数据（6个默认用户） | 2️⃣ 开发/测试 |
| `sql/sample_data_full.sql` | 插入完整测试数据（含溯源记录） | 3️⃣ 功能演示 |
| `sql/migrate_v2_security_enhance.sql` | 安全增强迁移（Token 版本控制） | 升级脚本 |
| `sql/migrate_v3_fine_grained_permissions.sql` | 扫码细粒度权限与权限继承收敛 | 升级脚本 |
| `sql/assign_v3_permissions_to_roles.sql` | 为既有 WAREHOUSE / LOGISTICS 角色补齐细粒度扫码权限 | 升级辅助脚本 |
| `sql/migrate_v3_signature_key_metadata.sql` | 为溯源日志补充签名 keyId/keyVersion 并回填历史数据 | 升级脚本 |
| `sql/migrate_v4_admin_demo_permissions.sql` | 示例数据生成/清理专用权限迁移 | 升级脚本 |
| `sql/migrate_v5_event_remark.sql` | 生命周期日志 remark 审计字段迁移 | 升级脚本 |
| `sql/migrate_v6_part_reference_constraints.sql` | 为溯源表补充 `spu_id` 索引和外键限制，防止删除已被引用的配件 | 升级脚本 |
| `sql/cleanup_old_data.sql` | 清理过期数据 | 定期维护 |

### 默认用户账号

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| superadmin | superadmin123456 | 超级管理员 | 拥有最高权限 |
| admin | admin123456 | 系统管理员 | 可管理普通用户 |
| producer | producer123456 | 生产人员 | 可进行生产赋码 |
| warehouse | warehouse123456 | 仓库人员 | 可进行入库/出库 |
| logistics | logistics123456 | 物流人员 | 可进行流转操作 |
| user | user123456 | 普通用户 | 仅可查询溯源 |

---

## 📚 API 文档

详细接口文档请参考：[../api-doc.md](../api-doc.md)

### 接口分类

| 模块 | 路径前缀 | 说明 |
|------|----------|------|
| 认证模块 | `/api/auth` | 登录、注册、登出、刷新 Token |
| 溯源模块 | `/api/traces` | 赋码（单次 `quantity<=500`）、扫码、查询、验链、公钥获取 |
| 用户管理 | `/api/users` | 用户 CRUD、角色分配 |
| 角色管理 | `/api/roles` | 角色 CRUD、权限配置 |
| 配件管理 | `/api/parts` | 配件规格 CRUD |
| 看板统计 | `/api/dashboard` | KPI、地图、趋势图 |

### 快速测试

**方法 A：使用 Postman**

导入 `postman/` 目录下的集合文件：

```
postman/
├── admin-api-tests.postman_collection.json  # 管理端 API 测试
├── trace-api-full-tests.postman_collection.json  # 溯源核心流程测试
├── trace-v2-security-tests.postman_collection.json  # 安全增强测试
└── trace-local.postman_environment.json  # 本地环境变量
```

详细使用说明：[../postman-guide.md](../postman-guide.md)

**方法 B：使用 curl**

```bash
# 登录获取 Token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123456"}'

# 响应示例：
# {"code":0,"status":200,"message":"登录成功","data":{"token":"eyJhbGc..."}}

# 使用 Token 访问受保护接口
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer eyJhbGc..."
```

---

## 📂 目录结构

```
backend/
├── src/main/
│   ├── java/com/example/trace/
│   │   ├── TraceApplication.java          # 🚀 启动类
│   │   ├── annotation/                    # 📝 自定义注解
│   │   │   └── RequirePermission.java     #    权限校验注解
│   │   ├── common/                        # 🛠️ 通用组件
│   │   │   ├── ApiResponse.java           #    统一响应封装
│   │   │   ├── BizCode.java               #    业务错误码
│   │   │   ├── BizException.java          #    业务异常
│   │   │   └── GlobalExceptionHandler.java#    全局异常处理
│   │   ├── config/                        # ⚙️ 配置类
│   │   │   ├── CorsFilter.java            #    CORS 跨域过滤器 ⭐
│   │   │   ├── CorsProperties.java        #    CORS 外置配置绑定
│   │   │   ├── CorsOriginMatcher.java     #    CORS Origin 匹配
│   │   │   ├── JacksonConfig.java         #    JSON 序列化配置
│   │   │   ├── MybatisPlusConfig.java     #    乐观锁插件
│   │   │   └── WebMvcConfig.java          #    登录/权限拦截器配置
│   │   ├── controller/                    # 🎮 控制器层
│   │   │   ├── AuthController.java        #    认证接口
│   │   │   ├── TraceController.java       #    溯源接口
│   │   │   ├── UserController.java        #    用户管理
│   │   │   ├── RoleController.java        #    角色管理
│   │   │   ├── PartController.java        #    配件管理
│   │   │   └── DashboardController.java   #    看板统计
│   │   ├── dto/                           # 📦 数据传输对象
│   │   │   ├── *Request.java              #    请求体 DTO
│   │   │   └── *Response.java             #    响应体 DTO
│   │   ├── entity/                        # 🗄️ 数据库实体
│   │   │   ├── SysUser.java               #    用户表
│   │   │   ├── SysRole.java               #    角色表
│   │   │   ├── TraceSnapshot.java         #    溯源快照（乐观锁）
│   │   │   ├── TraceLifecycleLog.java     #    溯源日志（Hash 链）
│   │   │   └── BasePartSpec.java          #    配件规格
│   │   ├── enums/                         # 🏷️ 枚举类型
│   │   │   ├── RoleCode.java              #    角色代码
│   │   │   ├── NodeType.java              #    节点类型
│   │   │   └── TraceStatus.java           #    溯源状态
│   │   ├── mapper/                        # 🗺️ MyBatis Mapper
│   │   │   ├── SysUserMapper.java
│   │   │   ├── TraceMapper.java
│   │   │   └── DashboardMapper.java
│   │   ├── security/                      # 🔐 安全模块 ⭐
│   │   │   ├── JwtUtil.java               #    JWT 生成/解析
│   │   │   ├── TokenStore.java            #    Token 黑名单（Redis）
│   │   │   ├── LoginInterceptor.java      #    登录拦截器
│   │   │   ├── PermissionInterceptor.java #    权限拦截器
│   │   │   └── PermissionService.java     #    权限校验服务
│   │   ├── service/                       # 💼 服务层
│   │   │   ├── impl/                      #    服务实现类
│   │   │   ├── policy/                    #    业务策略组件（角色层级等）
│   │   │   ├── TraceService.java          #    溯源核心业务
│   │   │   ├── UserService.java           #    用户管理业务
│   │   │   └── DashboardService.java      #    统计分析业务
│   │   └── util/                          # 🧰 工具类
│   │       ├── PasswordEncoder.java       #    BCrypt 密码加密
│   │       ├── SignatureUtil.java         #    RSA 签名工具
│   │       └── HashUtil.java              #    SHA-256 哈希工具
│   └── resources/
│       ├── application.yml                # 🔧 公共配置 ⭐
│       ├── application-dev.yml            #    开发 profile
│       ├── application-test.yml           #    测试 profile
│       ├── application-prod.yml           #    生产 profile（fail-fast）
│       └── mapper/                        # 📜 MyBatis XML 映射
│           ├── TraceMapper.xml
│           └── DashboardMapper.xml
├── sql/                                   # 💾 数据库脚本
│   ├── init_schema.sql                    #    建表脚本
│   ├── sample_data.sql                    #    测试数据
│   ├── migrate_v2_security_enhance.sql    #    安全增强迁移
│   ├── migrate_v3_fine_grained_permissions.sql   # 细粒度扫码权限
│   ├── migrate_v3_signature_key_metadata.sql     # 签名 key metadata
│   ├── migrate_v4_admin_demo_permissions.sql     # 示例数据专用权限
│   ├── migrate_v5_event_remark.sql               # 事件备注字段
│   └── migrate_v6_part_reference_constraints.sql # 配件引用保护迁移
├── keys/                                  # 🔑 本地密钥目录已废弃；不要存放/提交 *.pem 私钥
├── pom.xml                                # 📦 Maven 依赖配置
└── target/                                # 🏗️ 编译输出目录
```

**标记说明**：
- ⭐ 表示关键文件，HTTPS 开发环境配置的核心
- 🚀🎮🔐💼 表示不同分层组件

---

## 🔒 安全机制

### 1. JWT 认证体系

| 特性 | 实现方式 |
|------|----------|
| Token 生成 | `JwtUtil.generateToken()` |
| Token 验证 | `JwtUtil.validateToken()` |
| Token 黑名单 | Redis Set + TTL（过期自动删除）；Redis 异常时 fail-closed 返回 503 |
| Token 版本控制 | `user.token_version` 递增，强制失效旧 Token |
| 密码加密 | BCrypt（自动加盐，防彩虹表攻击） |

### 2. 拦截器链

```
请求 → CorsFilter (CORS 处理)
     → LoginInterceptor (JWT 验证)
     → PermissionInterceptor (权限校验)
     → Controller (业务处理)
```

**拦截器职责**：

| 拦截器 | 职责 | 失败响应 |
|--------|------|----------|
| `LoginInterceptor` | 验证 JWT Token | 401 Unauthorized |
| `PermissionInterceptor` | 校验用户权限 | 403 Forbidden |

**放行路径**：

```java
// LoginInterceptor 放行
"/api/auth/login"
"/api/auth/register"
"/api/traces/public-key"

// PermissionInterceptor 放行
"/api/auth/**"
```

### 3. 权限校验方式

**方式 A：注解声明式权限**

```java
@RequirePermission("USER_WRITE")  // 需要"用户写入"权限
public void updateUser(...) { ... }
```

**方式 B：API 路径权限**

配置在 `sys_role_permission` 表中：

```sql
INSERT INTO sys_role_permission (role_id, api_path, http_method) 
VALUES (1, '/api/users', 'POST');  -- ADMIN 角色可以 POST /api/users
```

### 4. 乐观锁防并发

```java
@TableName("trace_snapshot")
public class TraceSnapshot {
    @Version
    private Integer version;  // MyBatis-Plus 乐观锁字段
}
```

**并发场景**：
- 多个仓库同时扫码同一零件
- 防止数量计算错误

**重试策略**：
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void updateSnapshot(...) {
    // 内层事务：单次更新
}

public void processTrace(...) {
    for (int i = 0; i < 3; i++) {  // 外层：重试 3 次
        try {
            updateSnapshot(...);
            break;
        } catch (OptimisticLockingFailureException e) {
            // 重试
        }
    }
}
```

### 5. 溯源防篡改机制

#### Hash 链验证

每条日志包含：
```java
current_hash = SHA256(
    trace_code + action_type +
    from_node + to_node +
    province + city +
    event_time + ingest_time +
    prev_hash + correction_of +
    remark               // 非空事件备注
)
```

`trace_lifecycle_log.remark` 为可选审计备注，最长 255 字符。新写入的非空备注会随日志落库，并参与当前日志的 Hash 与 RSA 签名载荷；历史空备注日志保持兼容。

扫码事件 `event_time` 可省略；非空时必须使用 ISO-8601 本地时间（如 `2026-01-16T10:30:00`）。非法格式返回 400，不再由后端容错解析为服务器当前时间。

配件规格删除前会检查 `trace_snapshot.spu_id` 与 `trace_lifecycle_log.spu_id` 引用；已参与溯源的 SPU 返回 409，批量删除整批拒绝。新库 `init_schema.sql` 已包含 `spu_id` 索引与 `ON DELETE RESTRICT` 外键；既有库可执行 `sql/migrate_v6_part_reference_constraints.sql`。

验证逻辑：
```java
// 1. 重新计算当前记录的 Hash
String recalculated = hashUtil.calculateHash(log);

// 2. 对比数据库中的 Hash
if (!recalculated.equals(log.getCurrentHash())) {
    throw new BizException("溯源数据已被篡改");
}

// 3. 验证链式关系
String nextRecordPrevHash = nextLog.getPrevHash();
if (!current.getCurrentHash().equals(nextRecordPrevHash)) {
    throw new BizException("Hash 链断裂");
}
```

#### RSA 签名验证

```java
// 生成签名（后端私钥）：载荷包含日志业务字段、current_hash、correction_of 和非空 remark
String signatureData = SignatureUtil.buildSignatureData(..., currentHash, correctionOf, remark);
String signature = signatureUtil.sign(signatureData);
log.setSignature(signature);

// 验证签名（前端/客户端使用公钥）
GET /api/traces/public-key  // 获取公钥
boolean valid = signatureUtil.verify(signatureData, log.getSignature(), publicKey);
```

---

## ❓ 常见问题

### Q1: 启动时报错 "Access denied for user 'root'@'localhost'"

**原因**：MySQL 密码配置错误。

**解决**：通过环境变量或对应 profile 配置修正数据库密码：
```powershell
$env:TRACE_DB_PASSWORD = '<your-mysql-password>'
```

---

### Q2: Redis 连接失败 "Unable to connect to Redis"

**原因**：Redis 服务未启动或配置错误。Redis 黑名单是认证安全依赖，连接失败会导致受保护接口返回 503，避免认证链路失败即放行。

**解决**：

Windows:
```bash
# 启动 Redis（如果已安装为服务）
redis-server

# 或使用 WSL
wsl redis-server
```

Linux/Mac:
```bash
# 启动 Redis
sudo service redis-server start
```

确认配置：
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:  # 无密码留空
```

---

### Q3: 前端请求被 CORS 阻止

**症状**：浏览器控制台显示：
```
Access to XMLHttpRequest at 'http://localhost:8080/api/...' from origin 'https://192.168.x.x:5173' 
has been blocked by CORS policy
```

**原因**：前端 Origin 不在后端 CORS 白名单中。

**解决**：

1. 确认前端访问地址（例如 `https://192.168.1.100:5173`）
2. 修改 `application.yml` 添加到白名单：
   ```yaml
   cors:
     allowed-origin-patterns:
       - https://192.168.*:5173  # 已包含
   ```
3. 重启后端服务

**验证**：查看后端日志是否有：
```
CORS Filter: method=OPTIONS, origin=https://192.168.1.100:5173, path=/api/auth/login
CORS headers set for origin: https://192.168.1.100:5173
```

---

### Q4: 手机访问提示"没有权限访问资源"

**原因**：手机热点 IP 段不在 CORS 白名单。

**解决**：

1. 查看电脑连接手机热点后的 IP：
   ```powershell
   Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.InterfaceAlias -like "*WLAN*" }
   # 例如：172.20.10.7
   ```

2. 确认 `application.yml` 包含该 IP 段：
   ```yaml
   cors:
     allowed-origin-patterns:
       - https://172.*:5173  # ✅ 已配置
   ```

3. 重启后端服务

---

### Q5: JWT Token 失效问题

**场景 A：修改密码后旧 Token 仍有效**

**原因**：需要递增 `token_version` 强制失效旧 Token。

**解决**：确认修改密码的代码包含：
```java
user.setTokenVersion(user.getTokenVersion() + 1);
userMapper.updateById(user);
```

**场景 B：登出后 Token 仍能访问**

**原因**：Token 未加入黑名单。

**解决**：检查登出逻辑：
```java
tokenStore.blacklistToken(token, expirationTime);
```

---

### Q6: 乐观锁并发更新失败

**症状**：高并发下报错 "OptimisticLockingFailureException"。

**原因**：多个请求同时更新同一记录，version 冲突。

**解决**：已实现自动重试机制（3 次），无需额外处理。如需调整重试次数：

```java
private static final int MAX_RETRIES = 5;  // 改为 5 次重试
```

---

### Q7: RSA 密钥文件找不到

**症状**：启动时报错 "FileNotFoundException: private_key.pem"。

**解决**：

**方法 A：开发/测试使用固定外部备份密钥（推荐）**
```powershell
$env:TRACE_SIGNATURE_PRIVATE_KEY_PATH = 'D:/trace-runtime/keys/private_key.pem'
$env:TRACE_SIGNATURE_PUBLIC_KEY_PATH = 'D:/trace-runtime/keys/public_key.pem'
$env:TRACE_SIGNATURE_AUTO_GENERATE = 'false'
```

> 仅临时实验可设置 `TRACE_SIGNATURE_AUTO_GENERATE=true`；该模式只在内存生成密钥，不落盘，重启后历史签名无法继续用新公钥验证。

**方法 B：生产环境挂载外部密钥**
```bash
install -d -m 700 /etc/trace/signing
openssl genpkey -algorithm RSA -out /etc/trace/signing/private_key.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in /etc/trace/signing/private_key.pem -out /etc/trace/signing/public_key.pem
chmod 400 /etc/trace/signing/private_key.pem
```

确认路径正确：
```yaml
trace:
  signature:
    private-key-path: ${TRACE_SIGNATURE_PRIVATE_KEY_PATH:/etc/trace/signing/private_key.pem}  # 外部绝对路径
    public-key-path: ${TRACE_SIGNATURE_PUBLIC_KEY_PATH:/etc/trace/signing/public_key.pem}
    key-id: ${TRACE_SIGNATURE_KEY_ID:default}
    key-version: ${TRACE_SIGNATURE_KEY_VERSION:1}
```

---

### Q8: 端口 8080 被占用

**症状**：启动时报错 "Port 8080 was already in use"。

**解决**：

**方法 A：修改端口**
```yaml
server:
  port: 8081  # 改为其他端口
```

**方法 B：查找并关闭占用进程**

Windows:
```powershell
# 查找占用 8080 端口的进程
netstat -ano | findstr :8080

# 关闭进程（假设 PID 为 12345）
taskkill /PID 12345 /F
```

Linux/Mac:
```bash
# 查找占用 8080 端口的进程
lsof -i :8080

# 关闭进程
kill -9 <PID>
```

---

## 📞 技术支持

### 文档索引

| 文档 | 路径 | 内容 |
|------|------|------|
| 项目总览 | `../README.md` | 项目简介、整体架构 |
| API 接口文档 | `../api-doc.md` | 详细 API 说明、请求示例 |
| Postman 测试指南 | `../postman-guide.md` | 接口测试集合使用说明 |

### 日志调试

开启 DEBUG 日志查看详细信息：

```yaml
logging:
  level:
    com.example.trace: DEBUG  # 应用日志
    com.example.trace.security: TRACE  # 安全模块日志
    org.springframework.web: DEBUG  # Spring Web 日志
```

---

## 📄 License

MIT License - 详见 LICENSE 文件。

---

**最后更新**: 2026-01-21
