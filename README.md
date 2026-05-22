# 工业零配件供应链溯源系统

> 阅读对象：希望"看一遍就能掌握整套项目"的开发者、答辩评审、运维同学
>
> 本文自上而下分章：先看「这是什么」，再看「整体结构」，再钻入「核心模块」（占文档 50% 篇幅），最后是「数据库 / API / 部署 / 安全 / 测试 / 演进史」。核心溯源链路（哈希链 + RSA + 乐观锁 + 状态机 + 幂等 + 聚合 + 任务）会用代码引用 + 时序图 + 状态图详细讲解。

---

## 目录

- [一、项目是什么](#一项目是什么)
- [二、技术栈与代码地图](#二技术栈与代码地图)
- [三、整体架构总览](#三整体架构总览)
- [四、核心模块详解（重点）](#四核心模块详解重点)
  - [4.1 溯源数据模型：四张关键表如何协同](#41-溯源数据模型四张关键表如何协同)
  - [4.2 哈希链 + RSA 数字签名：不可篡改的实现](#42-哈希链--rsa-数字签名不可篡改的实现)
  - [4.3 乐观锁 + 独立事务重试：并发扫码的正确性保证](#43-乐观锁--独立事务重试并发扫码的正确性保证)
  - [4.4 扫码流转五道门：状态机 + 角色 + 节点 + 码状态 + 幂等](#44-扫码流转五道门状态机--角色--节点--码状态--幂等)
  - [4.5 生产赋码（批量）：两阶段写入与部分失败](#45-生产赋码批量两阶段写入与部分失败)
  - [4.6 仓库 / 物流任务驱动连续扫码](#46-仓库--物流任务驱动连续扫码)
  - [4.7 箱码 / 托盘码聚合](#47-箱码--托盘码聚合)
  - [4.8 异常冻结与红冲蓝补纠错](#48-异常冻结与红冲蓝补纠错)
  - [4.9 验链流程：第三方如何自验](#49-验链流程第三方如何自验)
  - [4.10 安全体系：JWT + Token 黑名单 + RBAC + token_version](#410-安全体系jwt--token-黑名单--rbac--token_version)
- [五、后端各模块速览（非核心部分）](#五后端各模块速览非核心部分)
- [六、前端各模块速览](#六前端各模块速览)
- [七、API 全量索引（按模块）](#七api-全量索引按模块)
- [八、数据库表全表清单与字段说明](#八数据库表全表清单与字段说明)
- [九、运行与部署](#九运行与部署)
- [十、测试与质量基线](#十测试与质量基线)
- [十一、安全威胁模型与已修复点](#十一安全威胁模型与已修复点)
- [十二、文档地图（项目内文档怎么找）](#十二文档地图项目内文档怎么找)
- [十三、严格代码审查提示词与流程](#十三严格代码审查提示词与流程)
- [十四、更新日志](#十四更新日志)
- [末：如何在 10 分钟里掌握这个项目](#末如何在-10-分钟里掌握这个项目)

---

## 一、项目是什么

**一句话**：这是一个把"工业零配件从生产→打码→入库→出库→流转→交付"整条链路用"二维码 + 数字签名"绑死的系统，能保证任何节点的扫码记录被任何第三方独立验证而无法被偷偷篡改。

### 解决什么问题

工业供应链里"假货"和"窜货"难追，根本原因是：
1. 各节点的进出记录分散在各家公司私有系统里，**互相不信任**；
2. 出问题后没人能拿出"完整、连续、可证明未被修改过"的历史；
3. 实物上没有"一物一码"的稳定锚点，扫一下就能调出全链路。

本系统的解法是：
- **一物一码**：每个零配件出厂时由生产端调 `POST /api/traces` 拿到一个全局唯一的 `traceCode`（UUID），打印成二维码贴在零件上；
- **哈希链**：每条事件日志的 `current_hash = SHA256(本条字段 || 上一条 current_hash)`，像区块链一样首尾相扣，**改任何一条都会让后面所有条对不上**；
- **RSA 数字签名**：每条日志再用服务器私钥 SHA256withRSA 签名，公钥公开。第三方拿到日志列表 + 公钥，本地就能验完整性，**伪造一条需要拿到私钥**；
- **乐观锁 + 独立事务重试**：高并发扫同一码不会出现"两条日志同时写、链断了"——失败重试，最终只允许一条胜出；
- **状态机 + 五道门**：扫码动作必须满足"鉴权 / 角色 / 状态 / 节点绑定 / 码可用"五个条件，少一个都不能落库，业务不会乱。

### 适用场景

- 工业零配件（阀门 / 轴承 / 电机 / 传感器 / 管件，系统内置 19 种 SPU 演示数据）
- 任何需要"赋码→入库→出库→流转→交付"且要求强审计的离散制造业供应链

### 系统六大角色 + 默认账号

| 用户 | 角色 code | 角色优先级 | 主要能力 |
|---|---|---|---|
| `superadmin / superadmin123456` | `SUPER_ADMIN` | 3 | 全权限；不可删不可禁 |
| `admin / admin123456` | `ADMIN` | 2 | 全权限；看不到 SUPER_ADMIN 和其他 ADMIN |
| `producer / producer123456` | `PRODUCER` | 1 | 生产赋码、打印、激活、异常上报 |
| `warehouse / warehouse123456` | `WAREHOUSE` | 1 | 入库、出库、流转任务创建/扫码/完成、异常上报 |
| `logistics / logistics123456` | `LOGISTICS` | 1 | 物流流转、任务扫码/完成、异常上报 |
| `user / user123456` | `USER` | 1 | 只读查询；自助注册默认为该角色 |

定义来源：[backend/sql/init_schema.sql:381-450](backend/sql/init_schema.sql#L381)。

---

## 二、技术栈与代码地图

### 后端

| 层 | 技术 | 用途 |
|---|---|---|
| 运行环境 | JDK 19+ | 启动 JVM |
| 构建 | Maven 3.9+ | 依赖、打包 |
| Web 框架 | Spring Boot 3 | RESTful 控制器、拦截器、事务 |
| 持久层 | MyBatis-Plus | CRUD、`@Version` 乐观锁、字段填充 |
| 数据库 | MySQL 8.0 | 业务持久化（utf8mb4） |
| 缓存 | Redis 6+ | Token 黑名单（必需，否则启动失败） |
| 鉴权 | **自实现** JWT（jjwt 库） + BCrypt | 无状态认证；**不使用** Spring Security |
| 加密 | Hutool 4 + JDK Crypto | SHA-256 哈希、RSA-2048 + SHA256withRSA 签名 |
| 工具 | Lombok / Jackson / Hutool | 模板代码精简、JSON、ID 生成 |

### 前端

| 层 | 技术 | 用途 |
|---|---|---|
| 框架 | Vue 3（Composition API） | 视图渲染 |
| 构建 | Vite 5 + ESM | HMR、HTTPS dev server |
| 状态 | Pinia | `useUserStore` 保存 token / 用户 / 权限 |
| 路由 | Vue Router 4 | `meta.permissions` 路由守卫 |
| UI | PrimeVue 4 + Tailwind CSS | 表单 / 表格 / 对话框 |
| 设计系统 | Linear 视觉契约（5 张静态 HTML） | lavender `#5e6ad2` 单 accent + Inter / JetBrains Mono |
| 可视化 | ECharts + 高德地图 API | 仪表盘、地理分布 |
| 扫码 | `vue-qrcode-reader` | 摄像头实时扫码（要求 HTTPS） |
| HTTP | Axios + 拦截器 | 自动加 `Authorization` + `snake_case` / `camelCase` 转换 |
| 测试 | Vitest + `@vue/test-utils` | 50 文件 / 301 用例 / ~12s |

### 顶层目录地图

```
d:/bysj/
├── backend/                       后端 Spring Boot 工程
│   ├── pom.xml
│   ├── sql/                       建表 + 21 个 migrate 迁移 + 演示数据
│   │   ├── init_schema.sql        全新建库脚本（含 6 角色 + 23 权限）
│   │   ├── migrate_v8 ~ v21.sql   逐步加表/字段：幂等表 / 节点 / 任务 / 聚合 / 异常 ...
│   │   └── sample_data_full.sql   500 条带 hash+RSA 的可验证示例
│   └── src/main/
│       ├── java/com/example/trace/   见下文「后端架构」章
│       └── resources/
│           ├── application.yml           公共配置
│           ├── application-{dev,test,prod}.yml  profile 覆盖
│           └── mapper/*.xml              复杂 SQL（Dashboard / TraceLog 全链查询）
│
├── frontend/                      前端 Vue 3 工程
│   ├── vite.config.js             HTTPS + /api 代理 + 别名 @
│   ├── DESIGN.md / preview/*.html Linear 设计契约 5 张
│   └── src/                       见下文「前端结构」章
│
├── deploy/                        Docker Compose 部署脚本（生产）
│   ├── docker-compose.yml
│   ├── .env.example
│   └── README.md                  Cloudflare + OpenResty 反代部署方案
│
├── api-doc.md                     全部 API 字段表（最准接口文档）
├── postman/                       Postman 集合 + Environment
├── postman-guide.md               Postman 用法
├── README.md                      入门 / 启动 / 鉴权 / 业务流程
├── CLAUDE.md                      AI 协作指南（项目约定汇编）
├── CAMERA_SCAN_GUIDE.md           摄像头扫码与自签证书排障
├── docs/                          深度设计文档与决策记录
│   ├── security/token-storage-and-csp.md
│   ├── trace-core-business-redesign.md
│   └── 数据库设计说明.md
├── 工业企业配件供应链溯源系统任务书.md / 开题报告.md   论文配套材料
├── china.json                     ECharts 中国地图
└── 项目审查记录_20260503.md       历史审查与已修复点档案
```

---

## 三、整体架构总览

### 3.1 部署拓扑

```
                         浏览器 (Vue 3 SPA)
                           │
                           │  HTTPS (摄像头需要)
                           ▼
                ┌─────────────────────┐
                │  Vite Dev / Nginx   │
                │   /api → 反向代理   │
                └─────────────────────┘
                           │
                           ▼
                ┌──────────────────────────┐
                │  Spring Boot 3 (8080)    │
                │  ─ Login/Permission 拦截 │
                │  ─ TraceController etc.  │
                │  ─ Service + 乐观锁重试  │
                │  ─ MyBatis-Plus Mapper   │
                └─────┬───────────────┬────┘
                      │               │
                      ▼               ▼
                ┌──────────┐    ┌────────────┐
                │  MySQL 8 │    │   Redis 6  │
                │ 业务数据  │    │ Token 黑名单│
                └──────────┘    └────────────┘
                      ▲
                      │
              外部挂载的 RSA 密钥
              （dev 默认 auto-generate，
                prod 强制外部文件 + Guard 校验）
```

生产部署版本见 [deploy/README.md](deploy/README.md)：复用现有 OpenResty + Cloudflare，前端容器跑 nginx 托管 `dist/`，后端容器跑 Spring Boot，MySQL/Redis 走 compose 内部网络，宿主机端口仅绑定 127.0.0.1。

### 3.2 请求 → 落库的全链路

```
浏览器
  │  Axios request.js → 自动加 Bearer + transform snake_case
  ▼
LoginInterceptor (order=1)        ← 验签 / 黑名单 / token_version / 用户启用
  │  request.attribute = {userId, username, role, roleId}
  ▼
PermissionInterceptor (order=2)   ← @RequirePermission 注解 / API pattern 匹配
  │
  ▼
Controller (e.g. TraceController)
  │  @Valid DTO + 调 Service
  ▼
Service / impl (TraceServiceImpl)
  │  分发到 support/*Service
  ▼
TraceScanRetryExecutor (外层 while 重试，不带事务)
  │
  ▼
TraceScanTransactionService (REQUIRES_NEW 事务)
  │  1. 读 snapshot + version
  │  2. 校验状态机 + 节点 + 码状态 + remark
  │  3. TraceLogFactory: hash + RSA sign
  │  4. INSERT trace_lifecycle_log
  │  5. UPDATE trace_snapshot WHERE version=?  ← 失败抛 OptimisticLockException
  │  6. UPDATE trace_code 物流态
  │  7. 标记 idempotency 成功
  ▼
ApiResponse{code,status,message,data:snake_case}
```

### 3.3 五大「核心动作」一图看清

```
┌──────────────────────────────────────────────────────────────────┐
│ 1. 生产赋码 POST /api/traces                                      │
│    → trace_assign_batch + N × trace_code + N × INIT 日志 + 快照   │
│    → 两阶段：先内存 RSA 签名，再分片 REQUIRES_NEW 提交（50/批）  │
│                                                                  │
│ 2. 单品扫码 POST /api/traces/{code}/events                       │
│    → 状态机校验 + 节点绑定 + 码可用 + 幂等键                       │
│    → 写一条 INBOUND/OUTBOUND/TRANSFER/EXCEPTION/CORRECTION 日志   │
│    → 乐观锁更新 snapshot + 同步 trace_code 物流态                 │
│                                                                  │
│ 3. 任务连续扫 POST /api/trace-flow-tasks/{id}/scan                │
│    → 任务固定 source→target，自动推 fromNode/toNode               │
│    → 扫父码自动展开聚合下的有效子码                                │
│    → 同码同 action 只计数一次，duplicate_count 自增                │
│                                                                  │
│ 4. 聚合绑定 POST /api/trace-aggregations                          │
│    → CARTON / PALLET / BATCH 三类父子关系                         │
│    → 同步追加 PACK/PALLETIZE 生命周期事件                          │
│                                                                  │
│ 5. 验链 GET /api/traces/{code}/verify                             │
│    → 逐条 prevHash 连续性 + 重算 hash + 公钥验签                   │
│    → 任一失败 valid=false，否则返回 valid=true                     │
└──────────────────────────────────────────────────────────────────┘
```

---

## 四、核心模块详解（重点）

> 本章是整个文档的灵魂，占了大约一半篇幅。
> 如果你只读一节，读 [4.4 五道门](#44-扫码流转五道门状态机--角色--节点--码状态--幂等)。

### 4.1 溯源数据模型：四张关键表如何协同

整套溯源系统的数据底座由 **8 张表** 组成，其中真正"承担"溯源不可篡改语义的是前 4 张：

```
┌─────────────────────┐   1:N   ┌──────────────────────┐
│ trace_assign_batch  │ ───────▶│      trace_code      │  ← 单品码状态、批次、序号
│ 一次生产订单的批次   │         │  GENERATED/PRINTED/   │
└─────────────────────┘         │  ACTIVATED/IN_STOCK/  │
                                │  IN_TRANSIT/VOIDED... │
                                └──────────┬───────────┘
                                           │ 1:1 trace_code (PK)
                                           ▼
                                ┌──────────────────────┐
                                │   trace_snapshot     │  ← 当前状态视图（快照）
                                │  current_status      │   只保留最新一条
                                │  current_node        │
                                │  last_hash           │   ← 哈希链头
                                │  version (乐观锁)    │
                                └──────────┬───────────┘
                                           │ 1:N
                                           ▼
                                ┌──────────────────────┐
                                │ trace_lifecycle_log  │  ← 全部历史事件（追加）
                                │  prev_hash → cur_hash│     哈希链 + RSA 签名
                                │  signature_key_id/ver│
                                │  correction_of       │
                                │  operator            │
                                └──────────────────────┘
```

辅助 4 张表：

| 表 | 作用 |
|---|---|
| `trace_node` | 结构化业务节点（FACTORY/WAREHOUSE/LOGISTICS/CUSTOMER/SERVICE），任务起终点 |
| `trace_user_node_binding` | 用户 ↔ 节点的多对多绑定，控制"谁能在哪个节点扫码" |
| `trace_flow_task` + `trace_flow_task_scan` | 仓库/物流的"发货单"，固定起终点、连续扫码、差异处理 |
| `trace_aggregation` | 箱码 / 托盘码 / 批次的父子聚合关系（含历史版本，由 `active=1` 唯一） |
| `trace_scan_idempotency` | 幂等键 → 业务日志 ID 的映射，防重复提交 |

**为什么要 `trace_snapshot` 和 `trace_lifecycle_log` 拆开？**

- `trace_lifecycle_log` 是"完整真相"（append-only，永远不改、不删）→ 给审计和验链用；
- `trace_snapshot` 是"当前状态"（一码一行，可以 UPDATE）→ 给前端列表、Dashboard、`available-actions` 等高频读用；
- 两者通过 `last_log_id` + `last_hash` 关联，且 snapshot 的 `version` 字段是并发安全的命脉（见 4.3）。

### 4.2 哈希链 + RSA 数字签名：不可篡改的实现

#### 4.2.1 哈希链是什么

每条 `trace_lifecycle_log` 都有：
- `prev_hash`：上一条的 `current_hash`（第一条用常量 `"GENESIS"`）；
- `current_hash = SHA256(本条所有受保护字段 || prev_hash)`。

受保护字段（按位置拼接，`|` 分隔）：
```
traceCode | actionType | fromNode | toNode | province | city
        | eventTime  | ingestTime | prevHash | correctionOf | operator
        | remark (可选)
```

源码：[backend/src/main/java/com/example/trace/util/HashUtil.java:110-146](backend/src/main/java/com/example/trace/util/HashUtil.java#L110)。

**关键性质**：
- 改任何字段 → `current_hash` 变 → 与存储值不符；
- 改某一条 hash → 后一条 `prev_hash` 对不上，链断；
- 改一条同时改后续所有条 → 仍可被 RSA 签名挡住（因为签名内容也包含 hash）。

时间精度坑：`event_time` / `ingest_time` 都 `truncatedTo(ChronoUnit.SECONDS)`，避免 Java 微秒精度与 MySQL `DATETIME` 秒级精度不一致导致重算 hash 不一致。

#### 4.2.2 RSA 数字签名

每条日志额外存：
- `signature` (Base64, ≤512 字符) — `SHA256withRSA(privateKey, signatureData)`；
- `signature_key_id` + `signature_key_version` — 标识用哪把私钥签的（支持密钥轮换）。

签名载荷与哈希载荷**字段相同但格式略不同**（key=value 形式）：见 [SignatureUtil.buildSignatureData](backend/src/main/java/com/example/trace/util/SignatureUtil.java#L262)。

密钥管理：
- **dev / test**：默认 `trace.signature.auto-generate=true`，启动时在内存生成 RSA-2048，每次重启都换 — 开箱即用，代价是旧签名失效；
- **prod**：[ProdProfileConfigGuard](backend/src/main/java/com/example/trace/config/ProdProfileConfigGuard.java) 强制拒绝 auto-generate=true，必须挂外部 PEM 文件（PKCS#8 私钥 + X.509 公钥）。

公钥任何人可拿：`GET /api/traces/public-key`（无需登录）。

#### 4.2.3 验签由谁做

服务器内部由 [TraceChainVerifyService](backend/src/main/java/com/example/trace/service/impl/support/TraceChainVerifyService.java) 做（见 4.9）；任何第三方拿到 `(logs[], signature, publicKeyBase64)` 后，本地也能验：

```java
SignatureUtil.verifyWithPublicKey(signatureData, signature, publicKeyBase64);
```

历史兼容性：早期版本 hash/sign 载荷不含 operator，所以 `HashUtil.calculateLegacyHash` / `SignatureUtil.buildLegacySignatureData` 还保留，验链时如果"新载荷"算不上，会回落到 legacy 试一次。

### 4.3 乐观锁 + 独立事务重试：并发扫码的正确性保证

#### 4.3.1 为什么不能用悲观锁

如果两个仓库同时扫同一码：
- 悲观锁（SELECT ... FOR UPDATE）会让第二个等第一个，但**长事务在 RSA 签名期间会持有数据库连接**，HikariCP 连接池被打满；
- 而且锁等待超时调起来很麻烦。

所以选择**乐观锁 + 外层重试**模式。

#### 4.3.2 实现要点

`TraceSnapshot.version` 字段 + MyBatis-Plus `@Version` 注解：
- `UPDATE trace_snapshot SET ... , version = version + 1 WHERE trace_code=? AND version = ?`；
- 如果有人比我先更新过，影响行数 = 0；
- 配置：[MybatisPlusConfig](backend/src/main/java/com/example/trace/config/MybatisPlusConfig.java) 注册 `OptimisticLockerInnerInterceptor`。

代码分层（关键）：

```java
// 外层：TraceScanRetryExecutor —— 无 @Transactional，否则重试在同一事务里看不到新 version
public boolean executeAndReturnCreated(ScanTraceRequest request, String operator) {
    int retryCount = 0;
    while (true) {
        try {
            return traceScanExecutor.executeAndReturnCreated(request, operator);
        } catch (TraceOptimisticLockException e) {
            retryCount++;
            if (retryCount > MAX_RETRY_COUNT) {  // 3
                throw new BizException(BizCode.CONCURRENT_CONFLICT, "并发冲突，请稍后重试");
            }
            Thread.sleep(50L * retryCount);  // 50ms / 100ms / 150ms 退避
        }
    }
}
```

源码：[TraceScanRetryExecutor.java:22-46](backend/src/main/java/com/example/trace/service/impl/support/TraceScanRetryExecutor.java#L22)。

```java
// 内层:TraceScanTransactionService —— @Transactional(REQUIRES_NEW)
@Transactional(propagation = Propagation.REQUIRES_NEW)
public boolean executeAndReturnCreated(ScanTraceRequest request, String operator) {
    // ... 读 snapshot / 校验 / 写 log ...
    int updated = traceSnapshotMapper.updateById(snapshot);
    if (updated == 0) {
        throw new TraceOptimisticLockException("乐观锁冲突, traceCode: " + traceCode);
    }
    // ...
}
```

源码：[TraceScanTransactionService.java:83-179](backend/src/main/java/com/example/trace/service/impl/support/TraceScanTransactionService.java#L83)。

#### 4.3.3 为什么必须是 `REQUIRES_NEW`

如果同一个事务里 while 重试：
1. 第一次失败 → 事务标记 rollback；
2. 第二次 SELECT 读不到其他事务刚提交的新 `version`（还在自己的旧事务里）；
3. 永远死循环。

`REQUIRES_NEW` 让内层**每次开新事务**，旧事务回滚后新事务能看到最新数据。这是设计的关键命门，CLAUDE.md 里特别标了 ⚠️。

#### 4.3.4 时序图

```
线程 A                                  线程 B
  │ SELECT v=5
  │                                      │ SELECT v=5
  │ 校验+签名（CPU 密集）                  │ 校验+签名
  │ INSERT log                            │
  │ UPDATE WHERE v=5 → OK, v=6           │
  │ COMMIT ✓                              │ INSERT log
  │                                       │ UPDATE WHERE v=5 → 0 行
  │                                       │ ROLLBACK ✗
  │                                       │ sleep 50ms
  │                                       │ SELECT v=6 (新事务)
  │                                       │ 校验+签名
  │                                       │ UPDATE WHERE v=6 → OK, v=7
  │                                       │ COMMIT ✓
```

### 4.4 扫码流转五道门：状态机 + 角色 + 节点 + 码状态 + 幂等

这是答辩里最容易被问"为什么这个账号扫码看不到入库动作？" 的环节。系统用 **5 道串行 AND 过滤门**回答：

| 门 | 维度 | 数据源 | 控制点 | 不通过的现象 |
|---|---|---|---|---|
| 1 | HTTP 鉴权 | JWT + token_version | [LoginInterceptor](backend/src/main/java/com/example/trace/security/LoginInterceptor.java) | 整个接口 401 |
| 2 | 角色权限 | `sys_role_permission` | [PermissionInterceptor](backend/src/main/java/com/example/trace/security/PermissionInterceptor.java) + `@RequirePermission` | 接口本身 403（缺 `trace:view`） |
| 3 | 状态机 | [TraceTransitionPolicy](backend/src/main/java/com/example/trace/service/policy/TraceTransitionPolicy.java) | `allowedActions(currentStatus)` | INIT 只能 INBOUND / EXCEPTION_OPEN；TRANSFERRED 终态 |
| 4 | 节点绑定 | `trace_user_node_binding` | `TraceUserNodeBindingService.canExecuteActionAtCurrentNode` | 当前码停在杭州仓，用户没绑定杭州仓节点 |
| 5 | 码可用性 | `trace_code.code_status` | `TraceCodeStatusService.ensureLifecycleMovementAllowed` | 码还没 ACTIVATED 就想入库 → 拒 |

外加幂等键 → 防止用户多次提交同一动作：`trace_scan_idempotency` 表唯一键 `(trace_code, action_type, idempotency_key)`。

#### 4.4.1 状态机详图

```
                  ┌────────────────────┐
                  │  EXCEPTION (冻结)  │ ◀─── EXCEPTION / EXCEPTION_OPEN
                  └─────────┬──────────┘      （任何状态都可上报）
                            │
                            │ EXCEPTION_CLOSE
                            │ 恢复到冻结前状态
                            ▼
   ┌──────────┐   INBOUND   ┌────────────┐  OUTBOUND  ┌────────────┐  TRANSFER  ┌──────────────┐
   │   INIT   │ ──────────▶ │  IN_STOCK  │ ─────────▶ │ IN_TRANSIT │ ─────────▶ │ TRANSFERRED  │
   └──────────┘             └────────────┘            └─────┬──────┘            └──────┬───────┘
                                  ▲                         │                          │
                                  │  INBOUND（到货确认）    │                          │
                                  └─────────────────────────┘                          │
                                  ▲                                                    │
                                  │                          INBOUND（最终入库）        │
                                  └────────────────────────────────────────────────────┘
```

源码：[TraceTransitionPolicy.createTransitions:133-164](backend/src/main/java/com/example/trace/service/policy/TraceTransitionPolicy.java#L133)。

#### 4.4.2 动作清单（ActionType 枚举 16 种）

[ActionType.java](backend/src/main/java/com/example/trace/enums/ActionType.java)：

| code | 名 | 触发场景 |
|---|---|---|
| `INIT` | 初始化 | 生产赋码自动写，**用户不能直接调用** |
| `PRINT_CODE` / `REPRINT_CODE` / `VOID_CODE` | 打印 / 重打 / 作废 | 标签生命周期，不改物流态 |
| `ACTIVATE_CODE` | 扫码激活 | 贴码后扫一下，code_status → ACTIVATED |
| `PACK` / `UNPACK` / `PALLETIZE` / `UNPALLETIZE` | 装/拆箱 / 上/下托盘 | 配合 trace_aggregation 自动写 |
| `INBOUND` / `OUTBOUND` / `TRANSFER` | 入/出库 / 流转 | 主流程；状态机受限 |
| `EXCEPTION` / `EXCEPTION_OPEN` / `EXCEPTION_CLOSE` | 异常冻结 / 解除 | 必填 remark |
| `CORRECTION` | 红冲蓝补 | 必填 correctionOf；不改 snapshot |

#### 4.4.3 接口：`GET /api/traces/{code}/available-actions`

前端扫到码后会先调它询问"这个码在我这个角色 + 这个节点下能做什么"，返回 `availableActions[]` + `recommendedAction`。逻辑全在 [TraceAvailableActionService.availableActions](backend/src/main/java/com/example/trace/service/impl/support/TraceAvailableActionService.java#L66)：

```
allowedActions = TraceTransitionPolicy.allowedActions(状态)     ← 门 3
            ∩ TraceActionPermissionPolicy.filterExecutable(roleId)  ← 门 2
            ∩ filterByUserNode(userId, currentNode)            ← 门 4
            如果 code blocked 且不在 EXCEPTION → 整体清空（门 5）
```

返回为空时还会贴心地给 `noActionReason` 解释为什么没有，比"沉默失败"友好得多。

### 4.5 生产赋码（批量）：两阶段写入与部分失败

#### 4.5.1 为什么要两阶段

赋码请求里 `quantity` 最大 500。如果整批放在一个事务里：
1. RSA-2048 签 500 次大约要 1~2 秒（CPU 密集）；
2. 整个时间 HikariCP 连接被占住；
3. 万一中间失败，全部回滚，前端体验差。

T-P1-01 整改后改为两阶段（[TraceCodeAssignmentService.produceAssign:74-182](backend/src/main/java/com/example/trace/service/impl/support/TraceCodeAssignmentService.java#L74)）：

**阶段 1（无事务）**：内存里准备好所有 `AssignmentUnit{initLog, snapshot, traceCode}`，包括 hash 和 RSA 签名。

**阶段 2（分片 `REQUIRES_NEW`）**：[TraceBatchCommitter](backend/src/main/java/com/example/trace/service/impl/support/TraceBatchCommitter.java) 按 `trace.batch.commit-size`（默认 50）切片，每片独立事务。

#### 4.5.2 语义合同（重要！）

**不再保证「全或无」**：

- 假设要 500 条，分 10 片 ×50；
- 第 7 片失败 → 第 1-6 片（300 条）已提交，**不会回滚**；
- 返回 `committedCount=300`、`partialFailure=true`、`batchStatus=PARTIAL_FAILED`；
- 响应只返回**已落库的 300 个 traceCode**，避免前端把"未持久化的码"当成可打印的标签。

前端必须把这种部分失败展示给用户（看 `TraceAssignmentWorkbench.vue` 的 toast）。批次表的 `quantity_generated` 也会反映真实落库数。

#### 4.5.3 打印标签：QR 真的能扫

赋码后的"打印"按钮不再只是写一条 `PRINT_CODE` 链上事件，而是先弹 `PrintLabelDialog.vue` 标签预览：每张标签含 36mm QR + 追溯码 + 批内序号 + 批次号 + 产品名（重打时多一个 RP 角标）。

- QR 内容由 `trace_code.qr_payload` 字段决定，**默认是完整 URL**（如 `https://你的域名/public/traces/<code>`），手机原生扫码可直接跳前端 `/public/traces/:code` 公开溯源页（无需登录）；
- 后端 [TraceQrProperties](backend/src/main/java/com/example/trace/config/TraceQrProperties.java) 控制：`trace.qr.public-base-url` 空时回退裸 traceCode（与历史数据兼容）；`path-template` 默认 `/public/traces/{code}`；
- 三处入口（行内打印 / 重打 / 头部批量打印）共用同一个 dialog，单码 / 批量统一渲染。**用户点 dialog 内"打印"才同步调浏览器 `window.print()`，关闭后才发 `PRINT_CODE` / `REPRINT_LABEL` 上链事件**（顺序关键：emit 在前会让父组件销毁 dialog，print 抓到空 DOM 输出白纸）；
- 系统内 [QRScanner.vue](frontend/src/shared/components/QRScanner.vue) 加 `extractTraceCode`，自动从 `/public/traces/<code>` 或 `/traces/<code>` 路径正则剥出 traceCode 再 emit，下游 4 个扫码入口零改动，新老两种 QR 载荷通吃。

### 4.6 仓库 / 物流任务驱动连续扫码

让仓库 / 物流人员一次只关心"扫几十个码进同一辆车"，不必每次都手动选起终点。

#### 4.6.1 数据模型

```
trace_flow_task (task_no PK; task_type ∈ OUTBOUND/TRANSFER/INBOUND/RECEIVE;
                 source_node_id → target_node_id; expected_quantity)
   └ trace_flow_task_scan (task_id, trace_code, action_type 唯一)
                          counted=0/1, duplicate_count, idempotency_key
```

#### 4.6.2 流程

1. **创建任务** `POST /api/trace-flow-tasks`：固定起点 / 终点 / 预计数量，状态 `CREATED`；
2. **连续扫** `POST /api/trace-flow-tasks/{id}/scan`：
   - 任务自动推 `fromNode/toNode`，用户传的若不一致直接拒；
   - 扫的是箱码或托盘码 → 通过 `trace_aggregation` 展开下面所有 active 子码（最大递归深度 8）；
   - 同一 `(task, code, action)` 第一次写 `counted=1`，之后只 `duplicate_count++`；
   - 任务状态 → `PROCESSING`；
3. **完成** `POST /api/trace-flow-tasks/{id}/complete`：
   - `expected_quantity == actual_quantity` → `COMPLETED`；
   - 少扫 → `discrepancy_type=SHORTAGE`，必须填差异原因；
   - 多扫 → `OVERAGE`；
   - `status → EXCEPTION`，留下审计字段。

源码：[TraceFlowTaskServiceImpl.java](backend/src/main/java/com/example/trace/service/impl/TraceFlowTaskServiceImpl.java)。

### 4.7 箱码 / 托盘码聚合

#### 4.7.1 模型

```
trace_aggregation:
  parent_code   ── 箱/托盘父码（前缀 CARTON- / PALLET-）
  child_code    ── 子码（可以是单品码，也可以是另一个箱码）
  relation_type ── CARTON / PALLET / BATCH
  active        ── 1 启用 / 0 已解除（保留历史）
  active_marker ── 生成列：active=1 时为 1，否则 NULL
  UNIQUE (parent_code, child_code, active_marker)  ← 同一对父子同时只能有一条 active
```

`active_marker` 这个生成列设计很巧妙：UNIQUE 索引在 NULL 处不冲突，所以同一对父子可以解绑再绑，留下任意多条历史。

#### 4.7.2 操作

- **绑定** `POST /api/trace-aggregations` → 写 active=1 关系 + 同步写 `PACK` / `PALLETIZE` 生命周期；
- **解除** `POST /api/trace-aggregations/{relationId}/release` → active=0 + 写 `UNPACK` / `UNPALLETIZE`；
- **查询有效**：`/api/trace-aggregations/children?parentCode=...` 或 `parents?childCode=...`；
- **查询历史**：`/api/trace-aggregations/history/*`；
- **运输中 / 异常状态禁止操作**（业务保护）。

源码：[TraceAggregationServiceImpl.java](backend/src/main/java/com/example/trace/service/impl/TraceAggregationServiceImpl.java)。

### 4.8 异常冻结与红冲蓝补纠错

两种修正手段的语义**完全不同**，不要搞混：

| 维度 | 异常冻结 EXCEPTION_OPEN/CLOSE | 红冲蓝补 CORRECTION |
|---|---|---|
| 目的 | 暂停一个码的常规流转（怀疑损坏 / 报警） | 修正一条**历史**日志的字段值 |
| 是否新增日志 | 是（EXCEPTION_OPEN 一条 + EXCEPTION_CLOSE 一条） | 是（追加一条 CORRECTION 日志） |
| 是否物理修改原日志 | **绝不** | **绝不** |
| 状态机影响 | 状态 → EXCEPTION，CLOSE 时恢复 `exception_restore_status` 保存的旧值 | 不改状态 |
| 必填 | remark | remark + correctionOf |

#### 4.8.1 异常恢复机制

EXCEPTION_OPEN 触发时，snapshot 把当时的 `currentStatus / currentNode / currentOwner` 拷贝到 `exception_restore_*` 三个字段。EXCEPTION_CLOSE 时再读回去。

源码：[TraceScanTransactionService.java:138-159](backend/src/main/java/com/example/trace/service/impl/support/TraceScanTransactionService.java#L138)。

#### 4.8.2 CORRECTION 的防御

防"跨链修正攻击"：[TraceScanTransactionService.validateCorrection:308-344](backend/src/main/java/com/example/trace/service/impl/support/TraceScanTransactionService.java#L308) 检查 4 个不变量：

1. `correctionOf` 不为 null 但 actionType 不是 CORRECTION → 拒；
2. 被修正日志不属于当前 traceCode → 拒（跨链攻击）；
3. 被修正日志本身就是 CORRECTION → 拒（链式修正）；
4. 原日志已被另一条 CORRECTION 修正过 → 拒（一条只能修正一次）；
5. CORRECTION 但没传 correctionOf → 拒。

#### 4.8.3 详情视图：effective vs audit

`GET /api/traces/{code}?view=effective` 默认隐藏被 CORRECTION 覆盖的原始日志；
`GET /api/traces/{code}?view=audit` 需要 `trace:audit:view` 权限，返回**全部**历史包括被修正的。

### 4.9 验链流程：第三方如何自验

`GET /api/traces/{code}/verify` 实现：[TraceChainVerifyService.verify](backend/src/main/java/com/example/trace/service/impl/support/TraceChainVerifyService.java#L24)。

```
1. selectFullChain(traceCode) 按时间升序拉所有日志
2. expectedPrevHash = "GENESIS"
3. for each log:
     a) 链连续性：expectedPrevHash == log.prevHash ?
        否 → errors += CHAIN_BROKEN
     b) 重算 hash：HashUtil.calculateHash(log.allFields, ..., operator) == log.currentHash ?
        否 → 尝试 legacy 载荷（不含 operator）→ 否则 errors += HASH_MISMATCH
     c) 签名：
        - 缺签名 → SIGNATURE_MISSING
        - 缺 key 元数据 → SIGNATURE_KEY_MISSING
        - 当前运行时未加载这个 key_id+version → SIGNATURE_KEY_UNAVAILABLE
        - signatureUtil.verify(signatureData, signature, keyId, keyVersion) 失败 → SIGNATURE_INVALID
     expectedPrevHash = log.currentHash
4. errors 为空 → success + 公钥 Base64 + totalLogs + duration
   否则 → failure + 错误列表（精确到 logId 和错误类型）
```

返回示例（成功）：
```json
{
  "valid": true,
  "total_logs": 5,
  "hash_verified_count": 5,
  "signature_verified_count": 5,
  "last_hash": "ab12...ef",
  "public_key": "MIIBIjANBg...",
  "signature_key_id": "default",
  "signature_key_version": 1,
  "duration_ms": 47
}
```

**第三方自验三步**：
```bash
# 1. 拿公钥
curl http://api.example.com/api/traces/public-key
# 2. 拿日志全链（GET /api/traces/{code}?view=audit）
# 3. 本地用任何 RSA-SHA256 库（Java/Python/Go/Node 都行）按相同 buildSignatureData 拼字符串 verify
```

### 4.10 安全体系：JWT + Token 黑名单 + RBAC + token_version

#### 4.10.1 JWT 与 token_version

[JwtUtil.java](backend/src/main/java/com/example/trace/security/JwtUtil.java)：HS256，Claims 包含 `username/role/token_version/jti`，**默认 2 小时**（rememberMe=1 天）。

`token_version` 是巧妙的强制失效手段：
- DB `sys_user.token_version` 默认 0；
- 改密码 → `token_version++`；
- 角色变更 → `token_version++`；
- LoginInterceptor 每次请求都比较 Token 里的版本 < DB 里的 → 立即失效，不需要清 Redis；
- 副作用：每次请求都查一次 `sys_user`（接受这个开销换简单性）。

#### 4.10.2 Redis 黑名单

[TokenStore.java](backend/src/main/java/com/example/trace/security/TokenStore.java)：
- Key：`token:blacklist:{jti}`；
- 值：`"1"`；
- TTL：Token 剩余有效期（自动过期，不用清理）；
- 加入场景：登出、刷新（旧 token 进黑名单）、强制踢人。

**fail-closed**：Redis 异常时不能放行，[LoginInterceptor:71-79](backend/src/main/java/com/example/trace/security/LoginInterceptor.java#L71) 把 `TokenStoreException` 转 503。这是过去的整改成果。

#### 4.10.3 RBAC 与权限注解

权限模型：

```
sys_role (id, role_code)  ← 6 个内置
   │
   │ N:N
   ▼
sys_role_permission (role_id, permission_id)
   │
   │ N:N
   ▼
sys_permission (perm_code, api_method, api_pattern)
                "trace:view", "GET", "/api/traces/*"
```

校验方式（[PermissionInterceptor](backend/src/main/java/com/example/trace/security/PermissionInterceptor.java)）：
1. `@RequirePermission(allowAnonymous=true)` → 直接放行（如公钥接口）；
2. 方法级 `@RequirePermission("trace:view")` → 检查 roleId 是否有该 perm；
3. 类级 `@RequirePermission(...)` → 同上；
4. 都没有 → 走 `api_pattern` 路径匹配兜底。

特性：
- **多权限**支持 `matchAll=false`（OR）/`true`（AND）；
- **权限继承**：`xxx:manage` 自动继承 `xxx:view`（[PermissionInheritanceResolver](backend/src/main/java/com/example/trace/security/permission/PermissionInheritanceResolver.java)）；
- **细粒度扫码**：`trace:scan`（超级）/`trace:inbound`/`trace:outbound`/`trace:transfer`，由 [TraceActionPermissionPolicy](backend/src/main/java/com/example/trace/service/policy/TraceActionPermissionPolicy.java) 把动作映射到权限。

#### 4.10.4 角色优先级

- `SUPER_ADMIN=3`、`ADMIN=2`、业务角色（PRODUCER/WAREHOUSE/LOGISTICS/USER）=1；
- [RolePolicy](backend/src/main/java/com/example/trace/service/policy/RolePolicy.java) 强制：用户只能操作"优先级**低于**自己"的目标；
- `ADMIN` 看不见 `SUPER_ADMIN` 和其他 `ADMIN`；
- `superadmin` 账号不可删不可禁用（业务规则）。

---

## 五、后端各模块速览（非核心部分）

### 5.1 包结构

```
backend/src/main/java/com/example/trace/
├── TraceApplication.java            启动类
├── annotation/RequirePermission     权限注解
├── common/                          通用响应/异常
│   ├── ApiResponse  {code,status,message,data,timestamp}
│   ├── BizCode      业务错误码常量
│   ├── BizException 业务异常
│   └── GlobalExceptionHandler  @RestControllerAdvice 兜底
├── config/                          配置类
│   ├── JacksonConfig            snake_case 序列化 + 多 naming 反序列化
│   ├── MybatisMetaObjectHandler create_time/update_time 自动填充
│   ├── MybatisPlusConfig        分页 + 乐观锁拦截器
│   ├── CorsFilter + CorsOriginMatcher + CorsProperties  CORS（支持 192.168.* 通配）
│   ├── ProdProfileConfigGuard   prod 环境强制安全配置（拒默认密码/默认 JWT secret/工作区签名 key）
│   ├── TraceBatchProperties     trace.batch.commit-size
│   ├── TraceDemoDataProperties  trace.demo-data.enabled / max-generate-count
│   └── WebMvcConfig             注册两个拦截器（order=1, order=2）
├── controller/                      13 个 REST 入口
│   ├── AuthController       /api/auth/{login,register,logout,refresh,me}
│   ├── TraceController      /api/traces** （生产赋码、扫码、详情、验链）
│   ├── TraceAssignBatchController  /api/trace-batches/**
│   ├── TraceCodeController       /api/trace-codes/{code}/activate
│   ├── TraceFlowTaskController   /api/trace-flow-tasks/**
│   ├── TraceAggregationController /api/trace-aggregations/**
│   ├── TraceNodeController       /api/trace-nodes
│   ├── UserController            /api/users
│   ├── RoleController            /api/roles
│   ├── PartController            /api/parts
│   ├── DashboardController       /api/dashboard/{kpi,map,trend,topology}
│   ├── AdminController           /api/admin/{generate-sample-data,clear-trace-data}
│   └── PublicTraceController     /api/public/traces/{code}  无需登录的公开自助验签
├── dto/                             约 60 个 Request/Response
├── entity/                          15 个 MyBatis-Plus 实体
├── enums/                           9 个枚举（ActionType / TraceStatus / TraceCodeStatus / TaskType / TaskStatus / RelationType / ...）
├── mapper/                          16 个 BaseMapper + XML 复杂查询
├── security/                        见 §4.10
│   └── permission/                  ApiPermissionMatcher / PermissionCache / RolePermissionQueryService / PermissionInheritanceResolver
├── service/
│   ├── policy/
│   │   ├── RolePolicy                角色层级 / 系统角色保护
│   │   ├── TraceTransitionPolicy     状态机
│   │   └── TraceActionPermissionPolicy  动作-权限映射
│   └── impl/
│       └── support/                  TraceServiceImpl 拆分件
│           ├── TraceScanRetryExecutor       外层重试
│           ├── TraceScanTransactionService  REQUIRES_NEW
│           ├── TraceCodeAssignmentService   生产赋码两阶段
│           ├── TraceBatchCommitter          分片提交
│           ├── TraceLogFactory              组装日志+hash+签名
│           ├── TraceChainVerifyService      验链
│           ├── TraceAvailableActionService  五道门
│           ├── TraceCodeStatusService       单品码状态机
│           ├── TraceCodeActivationService   贴码激活
│           ├── TraceCodeLabelService        打印/重打/作废
│           ├── TraceExceptionWorkflowService  异常 OPEN/CLOSE
│           ├── TraceAssignBatchService/ReconciliationService/CodeQueryService
│           └── TraceOptimisticLockException  自定义重试信号
├── util/                            HashUtil / SignatureUtil / DateTimeUtil / ProvinceUtil
└── validation/TraceLocationFieldConstraints  统一字段规范化（trim / 大小写 / 长度）
```

### 5.2 全局响应契约

```json
{
  "code": 0,           // 0=成功，其它=业务错误码（BizCode）
  "status": 200,       // HTTP 状态码（与响应行同步）
  "message": "成功",
  "data": { ... },     // 永远 snake_case
  "timestamp": "..."
}
```

请求体 / Query 参数 **同时支持 camelCase 与 snake_case**（Jackson 配 `PropertyNamingStrategies.SNAKE_CASE` + alias）。

### 5.3 配置文件

- `application.yml`：公共默认（端口 8080、Hikari 池、MyBatis-Plus、Jackson、CORS 模板、JWT 占位、`trace.qr.path-template`）；
- `application-dev.yml`：本地 MySQL/Redis、`auto-generate=true`、DEBUG 日志、stdout SQL、`trace.qr.public-base-url=https://localhost:5173`（QR 默认指向本地前端）；
- `application-test.yml`：测试库、同 dev，但 `trace.qr.public-base-url` 留空保持历史契约；
- `application-prod.yml`：全部敏感项清空 + WARN 日志，`trace.qr.public-base-url` 必须通过 `TRACE_PUBLIC_BASE_URL` 注入（真实前端域名），由 `ProdProfileConfigGuard` 启动时拦截不合规配置。

### 5.4 通用工具

- `JacksonConfig`：核心配置类，保证后端 ↔ 前端字段命名风格统一；
- `GlobalExceptionHandler`：把 `MethodArgumentNotValidException`、`BizException`、`DuplicateKeyException`、未捕获 `Exception` 都映射为 `ApiResponse.fail(...)`；
- `MybatisMetaObjectHandler`：插入时填 `create_time`，更新时填 `update_time`，无侵入。

---

## 六、前端各模块速览

### 6.1 目录结构（feature-based）

```
frontend/src/
├── main.js                  应用入口：Pinia + Router + PrimeVue + ToastService + zxing-wasm 路径覆盖
├── App.vue
├── style.css                Tailwind 入口
├── core/                    全局基础设施（跨 feature）
│   ├── api/
│   │   ├── request.js       Axios 实例 + 拦截器（snake_case ↔ camelCase）
│   │   ├── auth.js          /api/auth/* 调用
│   │   └── publicTrace.js   公开验签接口
│   ├── auth/authStorage.js  唯一的 localStorage 读写门面
│   ├── router/index.js      11 条路由 + meta.permissions 守卫
│   └── stores/
│       ├── index.js         Pinia 装载
│       └── user.js          useUserStore（token/user/permissions + login/logout/hasPermission）
├── features/                业务功能（按"特性"分目录）
│   ├── trace/
│   │   ├── api/
│   │   │   ├── index.js
│   │   │   └── trace.js     拼装 /api/traces, /trace-codes, /trace-flow-tasks, /trace-aggregations
│   │   ├── views/
│   │   │   ├── TraceAssignmentWorkbench.vue   生产赋码工作台
│   │   │   ├── TraceFlowTaskWorkbench.vue     仓库/物流任务工作台
│   │   │   ├── ScanHub.vue                    扫码工位（综合）
│   │   │   ├── TraceList.vue                  追溯列表（筛选 + 分页）
│   │   │   ├── TraceDetail.vue                单码详情（含时间线、路径地图、验签面板）
│   │   │   └── TracePublicView.vue            /public/traces/:code 公开页（免登录）
│   │   └── components/
│   │       ├── CreateTraceDialog.vue
│   │       ├── PrintLabelDialog.vue           标签打印预览（QR + 元信息 + @media print，单/批量复用）
│   │       ├── ScanFlowDialog.vue             核心扫码对话框（含可用动作下拉）
│   │       ├── ScanExceptionDialog.vue
│   │       ├── TraceCorrectionDialog.vue
│   │       ├── TraceExceptionCloseDialog.vue
│   │       ├── TraceTimeline.vue              历史事件时间轴
│   │       ├── TraceRouteMap.vue              高德地图路径（InfoWindow 走 DOM API 抗 XSS）
│   │       ├── TraceVerificationPanel.vue     /verify 结果可视化
│   │       └── TraceSummary.vue
│   ├── dashboard/views/Dashboard.vue + components/{KPI,Trend,Workload,Exceptions}.vue
│   ├── user/views/{UserList,RoleList}.vue
│   └── part/views/PartList.vue
└── shared/                  跨 feature 复用
    ├── components/
    │   ├── Login.vue / NotFound.vue / QRScanner.vue
    │   └── layout/MainLayout.vue + ui/
    ├── composables/         useToast 等
    ├── constants/
    │   ├── permissions.js   PERMISSIONS 常量树（与后端 perm_code 一一对齐）
    │   └── actionTypes.js
    ├── data/                china.json 等静态数据
    ├── theme/primevue-theme.js   Linear 主题
    └── utils/transform.js   transformKeysToSnake / transformKeysToCamel
```

### 6.2 关键文件解读

**`core/api/request.js`**：所有 HTTP 请求的唯一入口（已读到，[详见此处](frontend/src/core/api/request.js)）。要点：
- 请求拦截器自动附加 `Authorization: Bearer <token>`、`transformKeysToSnake`；
- 响应拦截器：`res.code !== 0` 抛 Error + `toast.error`（除非 config 标 `hideErrorToast`），并做 `transformKeysToCamel`；
- 401 区分"登录请求"/"密码错"/"用户不存在"，否则视为会话过期 → 清 storage + 跳登录。

**`core/auth/authStorage.js`**：唯一的 localStorage 读写位置，未来若切到 httpOnly Cookie 只改这一个文件。

**`core/router/index.js`**：路由守卫读 `meta.permissions`，调 `store.hasAnyPermission()`。没有权限直接跳 `/`。

**`features/trace/views/ScanHub.vue`**：扫码工位综合页面，根据 `available-actions` 接口动态显示按钮，调用 `ScanFlowDialog` 完成动作。

**`features/trace/components/TraceRouteMap.vue`**：在高德地图上绘制流转路径，InfoWindow 使用 `createElement` 而非 v-html，单元测试 `TraceRouteMap.info-window.test.js` 锁定该 XSS 防御不可回退。

**`main.js` 中的 `setZXingModuleOverrides`**：`vue-qrcode-reader` 把整套 `zxing-wasm` inline 进了自己的 dist，默认 `locateFile` 指向 jsdelivr CDN —— 项目 CSP `connect-src` 不放外部 CDN 直接拦截，扫码识别会拿不到结果。`main.js` 启动时把 wasm 路径覆盖为同源 `/zxing/zxing_reader.wasm`（仓库随 `frontend/public/zxing/` 目录交付，演示完全离线可用）。**关键**：必须从 `'vue-qrcode-reader'` 主包引 `setZXingModuleOverrides`，不能从 `zxing-wasm/reader` 或 `barcode-detector/pure` 引（三者各自独立模块实例）。

### 6.3 设计系统约束

- 颜色：单 accent 紫色 `#5e6ad2`（Linear lavender）；
- 字体：英数 Inter / 等宽 JetBrains Mono；
- 间距 / 圆角：4/6/8/12/16 圆角，4/8/12/16/24/32/48 间距；
- 5 张视觉契约：`frontend/preview/linear-{login,dashboard,scan,traces,trace-detail}.html`，新组件必须 1:1 对齐。

### 6.4 安全约束

- JWT 存 `localStorage`（已知风险，多层补偿，决策见 `docs/security/token-storage-and-csp.md`）；
- 全仓 `v-html` / `innerHTML` 零生产匹配（单测锁定）；
- `frontend/index.html` 内置 CSP meta（只放行同源 + 高德域名）；
- 扫码 wasm 走自托管 `/zxing/zxing_reader.wasm`，不依赖外部 CDN（CSP `connect-src 'self'` 直接覆盖，演示离线可用）；
- `.env*.local` 不入库；高德 Key 仅放 `.env.development.local`。

---

## 七、API 全量索引（按模块）

详细字段见 `api-doc.md`。本表只给"看一眼就能记住"的概览。

### 7.1 认证

| 方法 | 路径 | 权限 |
|---|---|---|
| POST | `/api/auth/register` | 匿名（自助注册，固定 USER 角色） |
| POST | `/api/auth/login` | 匿名（支持 `rememberMe`） |
| POST | `/api/auth/logout` | 登录 |
| POST | `/api/auth/refresh` | 登录 |
| GET  | `/api/auth/me` | 登录 |

### 7.2 溯源核心

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/traces` | `trace:batch:create` / `trace:create` | 生产赋码批次（quantity ≤ 500） |
| GET  | `/api/traces` | `trace:view` | 分页（多条件筛选） |
| POST | `/api/traces/{code}/events` | scan/inbound/outbound/transfer | 扫码流转 |
| GET  | `/api/traces/{code}/available-actions` | `trace:view` | 五道门后推荐动作 |
| GET  | `/api/traces/{code}` | `trace:view` (+`trace:audit:view` if `view=audit`) | 详情 |
| GET  | `/api/traces/{code}/verify` | `trace:view` | 验链 |
| GET  | `/api/traces/public-key` | 匿名 | 公钥 Base64 |
| POST | `/api/traces/{code}/exception/close` | `trace:exception:handle` | 解除冻结 |
| POST | `/api/traces/{code}/corrections` | `trace:exception:handle` | 红冲蓝补 |
| POST | `/api/traces/{code}/{print,reprint,void}` | `trace:code:print` | 标签生命周期 |

### 7.3 单品码 / 批次 / 任务 / 聚合 / 节点

| 方法 | 路径 | 权限 |
|---|---|---|
| POST | `/api/trace-codes/{code}/activate` | `trace:code:activate` |
| GET  | `/api/trace-batches/{id}` | `trace:view` |
| GET  | `/api/trace-batches/{id}/codes` | `trace:view` |
| GET/POST | `/api/trace-flow-tasks` | `trace:task:create` (POST) / `trace:view` |
| POST | `/api/trace-flow-tasks/{id}/{scan,complete,cancel}` | `trace:task:scan` / `complete` |
| POST | `/api/trace-aggregations` | `trace:scan` (+) |
| POST | `/api/trace-aggregations/{id}/release` | 同上 |
| GET  | `/api/trace-aggregations/{children,parents,history/*}` | `trace:view` |
| GET/POST/PUT/DELETE | `/api/trace-nodes` | `role:view` (GET) / `role:manage` (写) |
| GET  | `/api/users/me/trace-nodes` | 登录 |
| GET/PUT | `/api/users/{id}/trace-nodes` | `user:view` / `user:manage` |

### 7.4 用户 / 角色 / 配件 / 仪表盘 / 管理

| 方法 | 路径 | 权限 |
|---|---|---|
| GET/POST/PUT/DELETE | `/api/users` | `user:view` / `user:manage` |
| GET/POST/PUT/DELETE | `/api/roles` | `role:view` / `role:manage` |
| GET/POST/PUT/DELETE | `/api/parts` | `part:view` / `part:manage` |
| GET | `/api/dashboard/{kpi,map,trend,topology}` | `dashboard:view` |
| POST | `/api/admin/generate-sample-data?count=N(≤500)` | `trace:data:generate` |
| DELETE | `/api/admin/clear-trace-data?confirm=DELETE_TRACE_DATA` | `trace:data:clear` |

### 7.5 公开接口

| 方法 | 路径 | 权限 |
|---|---|---|
| GET | `/api/public/traces/{code}` | 匿名（脱敏视图，给消费者扫码追溯） |

---

## 八、数据库表全表清单与字段说明

> 完整 DDL：[backend/sql/init_schema.sql](backend/sql/init_schema.sql)。
> 表关系图见 [docs/数据库设计说明.md](docs/数据库设计说明.md)。

### 8.1 身份与权限

| 表 | 关键列 | 说明 |
|---|---|---|
| `sys_role` | id / role_code / role_name | 6 内置：SUPER_ADMIN, ADMIN, PRODUCER, WAREHOUSE, LOGISTICS, USER |
| `sys_permission` | perm_code / api_method / api_pattern | 23 权限点 |
| `sys_role_permission` | role_id + permission_id | 多对多 |
| `sys_user` | username / password (BCrypt) / role_id / **token_version** / status | token_version 是强制失效命脉 |

### 8.2 物料主数据

| 表 | 关键列 | 说明 |
|---|---|---|
| `base_part_spec` | part_code (UK) / part_name / part_type / model / manufacturer / unit / enabled | SPU 主数据；删除前会校验是否被 trace 引用 |

### 8.3 结构化节点

| 表 | 关键列 | 说明 |
|---|---|---|
| `trace_node` | node_code (UK) / node_type / province / city / enabled | FACTORY / WAREHOUSE / LOGISTICS / CUSTOMER / SERVICE |
| `trace_user_node_binding` | user_id + node_id (UK) / default_node / enabled | 用户能在哪些节点扫码 |

### 8.4 任务（仓库/物流）

| 表 | 关键列 | 说明 |
|---|---|---|
| `trace_flow_task` | task_no (UK) / task_type / source_node_id ↔ target_node_id / expected_quantity / actual_quantity / status / discrepancy_* | OUTBOUND / TRANSFER / INBOUND / RECEIVE |
| `trace_flow_task_scan` | (task_id, trace_code, action_type) UK / counted / duplicate_count / idempotency_key | 任务扫码明细 |

### 8.5 聚合

| 表 | 关键列 | 说明 |
|---|---|---|
| `trace_aggregation` | (parent_code, child_code, active_marker) UK / relation_type / active | CARTON / PALLET / BATCH |

### 8.6 溯源核心

| 表 | 关键列 | 说明 |
|---|---|---|
| `trace_assign_batch` | batch_no (UK) / spu_id / quantity_{requested,generated,printed,activated} / status | CREATED → GENERATING → GENERATED / PARTIAL_FAILED / FAILED |
| `trace_code` | trace_code (PK) / batch_id / spu_id / serial_no / **qr_payload** / code_status / activated_* | 一物一码状态机；`qr_payload` 存完整 URL（`{trace.qr.public-base-url}/public/traces/<code>`），手机原生扫码直跳公开溯源页；空 baseUrl 时回退裸 traceCode |
| `trace_snapshot` | trace_code (PK) / current_status / current_node / current_owner / last_event_time / last_log_id / **last_hash** / **version** | 当前状态视图（乐观锁） |
| `trace_lifecycle_log` | id / trace_code / action_type / from_node / to_node / event_time / ingest_time / **prev_hash / current_hash** / correction_of / **operator** / **signature / signature_key_id / signature_key_version** | 哈希链 + 签名（append-only） |
| `trace_scan_idempotency` | (trace_code, action_type, idempotency_key) UK / lifecycle_log_id / status | 幂等键 |

### 8.7 关键枚举速查

| 枚举 | 取值 |
|---|---|
| `ActionType` | INIT / PRINT_CODE / REPRINT_CODE / ACTIVATE_CODE / VOID_CODE / PACK / UNPACK / PALLETIZE / UNPALLETIZE / INBOUND / OUTBOUND / TRANSFER / EXCEPTION / EXCEPTION_OPEN / EXCEPTION_CLOSE / CORRECTION |
| `TraceStatus` | INIT / IN_STOCK / IN_TRANSIT / TRANSFERRED / EXCEPTION |
| `TraceCodeStatus` | GENERATED / PRINTED / ACTIVATED / IN_STOCK / IN_TRANSIT / EXCEPTION / VOIDED / SCRAPPED |
| `TraceAssignBatchStatus` | CREATED / GENERATING / GENERATED / PARTIAL_FAILED / FAILED / CANCELLED |
| `TraceFlowTaskType` | OUTBOUND / TRANSFER / INBOUND / RECEIVE |
| `TraceFlowTaskStatus` | CREATED / PROCESSING / COMPLETED / CANCELLED / EXCEPTION |
| `TraceFlowTaskDiscrepancyType` | NONE / SHORTAGE / OVERAGE |
| `TraceAggregationRelationType` | CARTON / PALLET / BATCH |
| `TraceNodeType` | FACTORY / WAREHOUSE / LOGISTICS / CUSTOMER / SERVICE |

### 8.8 迁移脚本 v2 → v21

| 迁移 | 内容 |
|---|---|
| v2 | 安全增强（token_version、status 字段） |
| v3 | 细粒度权限 + 签名密钥元数据 |
| v4 | admin demo 权限 |
| v5 | 事件 remark |
| v6 | part 引用约束（防孤儿） |
| v7 | Dashboard 索引 |
| v8 | scan 幂等表 |
| v9 | trace:audit:view 权限 |
| v10 | trace_assign_batch |
| v11 | trace_code 状态表 |
| v12 | trace_node |
| v13 | trace_user_node_binding |
| v14 | trace_flow_task |
| v15 | trace_flow_task_scan |
| v16 | task 差异字段 |
| v17 | trace_aggregation |
| v18 | aggregation 事件动作类型 |
| v19 | 新版业务动作权限 |
| v20 | 异常工作流 |
| v21 | part.enabled |

升级现有库按编号顺序执行即可（idempotent 已尽量保证）。

---

## 九、运行与部署

### 9.1 一次性准备

1. JDK 19+；Maven 3.9+；MySQL 8+；Redis 6+；Node.js 18+；
2. 建库：`CREATE DATABASE trace_db DEFAULT CHARACTER SET utf8mb4;`；
3. 跑 `backend/sql/init_schema.sql`（含 6 角色、23 权限、6 默认账号、1 SPU）；
4. （可选）`backend/sql/sample_data_full.sql` 灌入 500 条带 hash/RSA 的演示数据。

### 9.2 启动后端

```powershell
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:TRACE_DB_URL = 'jdbc:mysql://localhost:3306/trace_db?...'
$env:TRACE_DB_USERNAME = 'xxx'
$env:TRACE_DB_PASSWORD = 'xxx'
$env:TRACE_REDIS_HOST = 'localhost'
$env:TRACE_REDIS_PORT = '6379'
cd backend
mvn spring-boot:run
# → http://localhost:8080
```

dev 环境不需要 RSA 密钥文件（自动生成）。prod 必须：

```powershell
$env:TRACE_SIGNATURE_PRIVATE_KEY_PATH = 'D:/keys/trace-private.pem'  # PKCS#8
$env:TRACE_SIGNATURE_PUBLIC_KEY_PATH  = 'D:/keys/trace-public.pem'   # X.509
$env:TRACE_SIGNATURE_AUTO_GENERATE    = 'false'
$env:TRACE_JWT_SECRET                 = '至少32字节的强随机字符串'
```

否则 `ProdProfileConfigGuard` 直接 fail-fast 不让启动。

### 9.3 启动前端

```bash
cd frontend
npm install
node generate-cert.js        # 生成自签证书到 certs/
cp .env.example .env.development.local
# 编辑 .env.development.local，填高德 Key
npm run dev
# → https://localhost:5173（首次访问浏览器会警告自签）
```

`/api` 已经被 Vite 代理到 `VITE_API_PROXY_TARGET`（默认 `http://localhost:8080`）。

### 9.4 生产打包

```bash
# 后端
cd backend && mvn clean package -DskipTests
# → target/trace-1.0.0.jar
java -jar target/trace-1.0.0.jar --spring.profiles.active=prod

# 前端
cd frontend && npm run build
# → dist/ ; 用 Nginx 托管，/api 反代到 8080
```

### 9.5 Docker Compose 部署（推荐）

仓库已带完整 compose 编排：[deploy/docker-compose.yml](deploy/docker-compose.yml) + [deploy/README.md](deploy/README.md)。

```bash
cd deploy
cp .env.example .env
# 编辑 .env 填好 DB / Redis / JWT secret / 域名 / 高德 Key
docker compose up -d
# → frontend 127.0.0.1:18080, backend 127.0.0.1:18081
# → 上层用 OpenResty/Nginx + Cloudflare HTTPS 反代到域名
```

特点：
- MySQL/Redis **不**对外暴露，只在 compose 内部网络访问；
- 后端容器挂 `./keys:/app/keys:ro`，prod 强制外部密钥；
- 端口默认绑定到 `127.0.0.1` 避免裸暴露，依赖上层反代统一入口。

### 9.6 演示数据接口（仅 dev/test）

```bash
# 1. 登录拿 token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin","password":"superadmin123456"}'

# 2. 生成 500 条
curl -X POST "http://localhost:8080/api/admin/generate-sample-data?count=500" \
  -H "Authorization: Bearer <token>"

# 3. 验任意码
curl http://localhost:8080/api/traces/<code>/verify \
  -H "Authorization: Bearer <token>"

# 4. 清空（要二次确认）
curl -X DELETE "http://localhost:8080/api/admin/clear-trace-data?confirm=DELETE_TRACE_DATA" \
  -H "Authorization: Bearer <token>"
```

prod 默认禁用，除非 `TRACE_DEMO_DATA_ENABLED=true`。

---

## 十、测试与质量基线

### 10.1 后端

- JUnit 5 + Spring Boot Test；
- 关键测试覆盖：
  - `JwtUtilTest`：Token 长度、过期、版本号；
  - `TokenStoreTest`：fail-closed；
  - `PermissionInterceptorTest`：allowAnonymous / order / fail-defensive；
  - `TraceServiceImplTest`：扫码流转主路径；
  - `TraceTransitionPolicyTest`：每一组合法/非法 transition；
  - `TraceChainVerifyServiceTest`：链断、hash 篡改、签名失效；
  - `TraceScanTransactionServiceTest`：correctionOf 跨链攻击 / 重复修正；
  - `ProdProfileConfigGuardTest`：拒绝弱配置；
  - `TraceCodeAssignmentServiceTest`：部分失败语义；
  - `TraceFlowTaskServiceImplTest`：差异类型 / 聚合展开；
- 跑：`mvn test`（dev 用内嵌 MySQL/test 库；CI 用真实 docker compose）。

### 10.2 前端

- Vitest（jsdom）+ `@vue/test-utils`；
- `setupFiles: src/test/setup.js` 注入 PrimeVue Toast 等全局 mock；
- 50 文件 / 301 用例 / ~12s；
- 关键：
  - `core/api/__tests__/request.test.js` 拦截器；
  - `features/__tests__/api-contracts.test.js` 前后端字段契约对齐；
  - `core/auth/__tests__/authStorage.test.js` 异常缓存清理；
  - `features/trace/components/__tests__/TraceRouteMap.info-window.test.js` 锁定不用 v-html；
  - 三档 viewport 25 个 matchMedia 1023.98 切换测试。
- 跑：`npm run test:run`。

### 10.3 手动测试

Postman 集合 `postman/*.json` + [postman-guide.md](postman-guide.md)；运单驱动扫码端到端走法见 [方案B运单驱动扫码完整测试指南_20260511.md](方案B运单驱动扫码完整测试指南_20260511.md)。

---

## 十一、安全威胁模型与已修复点

> 完整证据链见 [docs/security/token-storage-and-csp.md](docs/security/token-storage-and-csp.md) 与 [项目审查整改任务表_20260503.md](项目审查整改任务表_20260503.md)。

### 11.1 当前已落地的防护

| 维度 | 防护 |
|---|---|
| 认证 | JWT HS256 + ≥32 字节 secret + jti + token_version + 2h 默认有效 |
| 失效 | 登出 / 改密 / 角色变更 → Redis 黑名单 + token_version++；Redis 故障 fail-closed |
| 授权 | RBAC 注解 + 路径匹配 + 权限继承 + 角色优先级 + superadmin 保护 |
| 数据完整 | SHA-256 哈希链 + RSA-2048 + SHA256withRSA + 公钥公开自验 |
| 并发 | `@Version` 乐观锁 + REQUIRES_NEW 重试 |
| 输入 | DTO `@Valid` + `TraceLocationFieldConstraints` 统一规范化 |
| 注入 | MyBatis 全参数化；前端无 v-html / innerHTML |
| 配置 | ProdProfileConfigGuard 拒弱密码 / 默认 secret / 工作区 key |
| 业务 | correctionOf 跨链攻击校验 / 一次修正限制 / EXCEPTION_CLOSE 状态恢复 |
| Token 存储 | localStorage + 短 Token + 适配层 + CSP（待迁 httpOnly Cookie） |

### 11.2 已知风险

| 风险 | 当前缓解 | 长期方案 |
|---|---|---|
| XSS → Token 被读 | 短 Token + 适配层 + CSP + 测试锁定 | httpOnly Cookie + CSRF Token |
| dev 重启换 RSA key 旧签失效 | 文档化 + prod 强制外部 key | 引入 key 轮换 + history |
| 批量赋码部分失败 | 分片提交 + 响应明示 partial | 不打算改（性能权衡） |

### 11.3 历史已修复点（不重复审查）

- 自助注册曾允许指定 roleId/status → 已锁死 USER；
- RSA 私钥曾入库 → 已删除 + Guard 拒绝；
- application.yml 曾默认 stdout SQL → 已拆 profile；
- 配件删除不校验引用 → 已加 409；
- Token 黑名单 fail-open → 已改 fail-closed (503)；
- ProdProfileConfigGuard 不校验 JWT secret 强度 → 已加 ≥32 字节硬性要求；
- 「打印」按钮只写链上事件没真出标签 → 新增 `PrintLabelDialog.vue` 弹真实可打印 QR 预览（2026-05-23）；
- 单品码 `qr_payload` 列与 `trace_code` 列内容重复（占位字段没填业务含义）→ 改为完整 URL，手机原生扫码可直跳公开溯源页（2026-05-23）；
- 内部扫码识别永远为空（`vue-qrcode-reader` inline 的 `zxing-wasm` 默认走 jsdelivr CDN 被 CSP 拦）→ wasm 自托管 `/zxing/` + `setZXingModuleOverrides` 同源化（2026-05-23）。

---

## 十二、文档地图（项目内文档怎么找）

| 文档 | 写给谁 | 内容 |
|---|---|---|
| `README.md` | 第一次进项目的人 | 业务介绍 / 启动 / API 概览 / 鉴权 / 演示数据 / 严格审查提示词 |
| `CLAUDE.md` | AI 协作者 / 资深开发 | 项目约定、关键路径、易踩坑、已修复点 |
| `api-doc.md` | 接口对接方 | 全字段表（最准） |
| `postman-guide.md` + `postman/` | 测试人员 | Postman 用法 + 集合 |
| `CAMERA_SCAN_GUIDE.md` | 现场调试 | 摄像头扫码 + HTTPS 自签证书排障 |
| `deploy/README.md` | 运维 | Docker Compose + Cloudflare + OpenResty 部署方案 |
| `backend/README.md` | 后端单独看 | 后端启动、Maven 命令、常见问题 |
| `frontend/README.md` | 前端单独看 | 11 路由 / 50 测试 / 设计系统 / 5 角色 FAQ |
| `frontend/DESIGN.md` | UI 设计 | Linear 设计契约（颜色 / 字体 / 间距 / 圆角） |
| `docs/security/token-storage-and-csp.md` | 安全审阅 | localStorage 决策与未来 Cookie 迁移 |
| `docs/trace-core-business-redesign.md` | 核心业务理解 | 溯源核心重设计的最新版 |
| `docs/数据库设计说明.md` | DBA / 新人 | 表关系图、字段说明 |
| `项目审查记录_20260503.md` | 代码审查 | 历史审查发现 + 已修复点 |
| `工业企业配件供应链溯源系统任务书.md` / `开题报告.md` | 论文配套 | 学术材料 |
| `方案B运单驱动扫码完整测试指南_20260511.md` | 答辩演练 | 任务驱动扫码端到端走法 |

---

## 十三、严格代码审查提示词与流程

如果你想让 AI 针对 **当前项目本身** 做一次严格、系统、可落地的代码审查，推荐直接使用下面这套提示词与流程。

### 13.1 可直接复制的提示词

```text
请你对当前项目做一次"严格代码审查"。

注意：
1. 这不是只看最近 diff，也不是只看单个文件；请从"整个项目当前状态"出发审查。
2. 必须基于当前仓库里的真实代码、配置、测试、README 和前后端调用关系来分析，不要泛泛而谈。
3. 分析时请同时覆盖：架构边界、设计模式、一致性、技术债、可维护性、安全、性能、并发、配置与环境隔离、前后端契约、测试覆盖、文档一致性。

我需要你重点回答这几类内容：
- 哪些地方做得好，为什么好；
- 哪些地方可能有问题，为什么可能有问题；
- 哪些属于明确的设计缺陷或实现缺陷；
- 哪些属于高风险问题，应该优先修；
- 哪些只是次要问题、技术债或文档债务；
- 哪些地方你暂时不能确定，需要标记为"待验证"。

输出要求：
- 尽量给出明确证据，优先使用 `file:line` 引用；
- 每条问题都要标注严重级别：严重 / 主要 / 次要 / 建议；
- 每条问题都要写清：现象、影响、原因分析、修复建议；
- 如果发现优点，也不要只写"代码不错"，而要说明具体优点和正面影响；
- 必须给出"优先修复顺序"，而不是只罗列问题；
- 如果 README、配置、环境变量、测试与代码实现不一致，也要算作问题；
- 如果某处只是推测，必须明确写"推测/待验证"，不要伪装成已证实结论。

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

### 13.2 推荐审查流程

1. **先确认范围**
   - 默认审查仓库根目录，而不是只看单个模块。
   - 如果仓库太大，先按 `backend / frontend / docs / config / tests` 分层审查。

2. **先看运行入口和配置**
   - 后端入口、前端入口、环境变量、密钥路径、第三方依赖、日志级别、代理配置。
   - 先找"配置层风险"和"环境隔离问题"，因为这类问题经常影响全局。

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
   - 不要只说"有风险"，要说明"风险在哪里、为何成立、优先级多少、怎么改"。

### 13.3 本项目建议优先核查的点

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

### 13.4 建议输出格式

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

### 13.5 一个更适合当前仓库的简短版本

如果你不想输入太长提示词，可以直接用这一段：

```text
请对当前仓库做一次严格的项目级代码审查，不要只看 diff。重点检查后端鉴权与权限链路、溯源业务链、并发与事务边界、配置与密钥外置化、前后端接口契约、测试覆盖、README 与代码是否一致。必须同时指出优点、问题、设计缺陷和待验证项，并尽量给出 file:line、严重级别、影响和修复建议，最后给出 P0/P1/P2 修复顺序。
```

---

## 十四、更新日志

### 2026-05-23

**Docker 部署与公开溯源端点：**
- ✅ 新增 `deploy/docker-compose.yml`：MySQL/Redis/Backend/Frontend 一键部署，宿主机端口仅绑 127.0.0.1
- ✅ 新增 `backend/Dockerfile` + `frontend/Dockerfile` + `frontend/nginx.conf` 多阶段构建
- ✅ 后端新增 `PublicTraceController` + `PublicTraceResponse`：脱敏视图，给消费者扫码自助溯源
- ✅ 前端新增 `TracePublicView.vue` + `publicTrace` API，路由 `/public/traces/:code` 免登录

### 2026-05-22

**生产赋码与扫码可用性：**
- ✅ 打印标签真出 QR：`PrintLabelDialog.vue` 36mm QR + 元信息 + `@media print`，单/批量复用
- ✅ 单品码 `qr_payload` 改为完整 URL，手机原生扫码直跳公开溯源页
- ✅ `zxing-wasm` 同源化绕 CSP：自托管 `/zxing/zxing_reader.wasm` + `setZXingModuleOverrides`

### 2026-05-10/11

**第二轮手动测试修复：**
- ✅ 任务下拉选定后省/市自动填充
- ✅ 5 维度门 README 完善 + 运单驱动 ScanFlowDialog

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

---

## 末：如何在 10 分钟里掌握这个项目

如果你只有 10 分钟，按这个顺序读：

1. **本文 §1** 了解项目要解决的问题（1 分钟）；
2. **本文 §3.3** 看五大核心动作图（1 分钟）；
3. **本文 §4.2 + §4.3** 理解哈希链 + 乐观锁两个命门（4 分钟）；
4. **本文 §4.4** 看五道门（2 分钟）；
5. **跑一次演示**：
   ```bash
   mvn spring-boot:run    # 后端
   npm run dev            # 前端
   # 用 superadmin 登录 → 生成 500 条 → 任意点一个码 → 看时间线和验签结果
   ```
6. 卡住时回到本文目录定向查。

祝答辩 / 上线 / 接手顺利。

---

*文档以 2026-05-13 为基线，2026-05-23 增量更新打印标签 / QR URL / 扫码 wasm 同源化 / Docker 部署 / 公开溯源端点五项内容。如有偏差请优先信任源码与 [api-doc.md](api-doc.md)。*
