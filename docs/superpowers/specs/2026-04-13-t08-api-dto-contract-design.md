# T08 收敛 API 注释与后端 DTO 语义设计

**日期**：2026-04-13  
**任务编号**：T08  
**任务名称**：收敛 API 注释与后端 DTO 语义

## 1. 目标

在 **T07 已完成前端契约边界收口** 的前提下，T08 继续解决“接口文档语义”和“后端 DTO / Controller 实际语义”之间的漂移问题，目标如下：

- 让前端 API 注释、前端调用方式、后端 DTO 字段语义、Controller 参数命名、真实运行行为保持一致
- 明确当前项目的**唯一主契约**：
  - 前端内部：`camelCase`
  - 前端发往后端：由 `request.js` 统一序列化为 `snake_case`
  - 后端 Java 内部字段：`camelCase`
  - 后端 JSON 输出：由 Jackson 全局策略统一为 `snake_case`
- 清理“看起来像契约定义、实际只是历史兼容残留”的注解、注释和参数写法
- 在不引入 UI 改造、不改变业务功能的前提下，为后续 T09-T13 / T24-T29 的逻辑治理建立更稳定的语义基线

## 2. 当前现状

### 2.1 已经完成的基础

T07 已建立如下边界：

- `frontend/src/shared/utils/transform.js`：负责通用 key 转换
- `frontend/src/core/api/request.js`：
  - 请求 `params/data` 统一转为 `snake_case`
  - 响应 `res.data` 统一转为 `camelCase`
- 前端 feature API 和代表性页面已基本改为直接消费 `camelCase`

因此，T08 不再讨论“要不要统一命名”，而是继续把**注释、DTO、Controller、兼容策略**全部收束到同一套规则上。

### 2.2 当前存在的漂移点

当前后端与前端仍存在以下语义漂移：

1. **后端 DTO 混用 `@JsonProperty` 与 `@JsonAlias`**
   - 一部分字段同时声明 `@JsonProperty("snake_case") + @JsonAlias("camelCase")`
   - 一部分字段只声明 `@JsonAlias("snake_case")`
   - 一部分分页 / 查询 DTO 上存在 Jackson 注解，但这些类主要用于 **query 参数绑定**，并不能自然成为 query contract 的唯一来源

2. **Controller 仍有显式 legacy 命名**
   - `DashboardController.topology(...)` 仍显式使用 `@RequestParam(name = "trace_code")`
   - 其它 query param / list filter 端点虽然代码里用 `camelCase`，但实际前端请求会被 `request.js` 转成 `snake_case`
   - 这意味着：**query 参数绑定是否真的稳定，需要单独验证，不能只看字段名或注解臆断**

3. **前端 API 注释与实际契约还有历史惯性**
   - feature API 注释虽已大体转向 `camelCase`，但仍需统一其叙述方式与边界说明
   - 认证 API（如 `frontend/src/core/api/auth.js`）也应纳入同一套语义说明，不再把 legacy 命名当作主要描述

4. **兼容逻辑缺少明确白名单**
   - 当前项目已从“全局到处兼容”进入“边界统一 + 少量兼容保留”阶段
   - 但哪些兼容点必须保留、哪些应该删除，还没有一份明确的、可执行的白名单

## 3. 设计结论

### 3.1 总体方向

T08 采用用户已确认的方案：

> **强标准化 + 兼容白名单**

即：

- 主契约只保留一套
- 兼容只允许出现在少量、被明确点名的边界
- 不再接受“为了保险到处双写 / 到处 alias / 到处注释两套字段名”的做法

### 3.2 主契约

T08 之后，以下规则作为本任务范围内的主契约：

| 层次 | 规范 |
|---|---|
| 前端页面 / store / composable / feature API 内部字段 | `camelCase` |
| 前端 API 注释中描述的参数 / 返回字段 | `camelCase` |
| 前端 HTTP 发出前 | 由 `request.js` 统一转为 `snake_case` |
| 后端 DTO / Java 字段 / Service 内部字段 | `camelCase` |
| 后端 JSON 响应 | 由 Jackson 全局 `SNAKE_CASE` 输出为 `snake_case` |
| 兼容输入 | 只保留白名单中的 `@JsonAlias` / 显式参数别名 |

### 3.3 契约来源规则

为了避免同一字段在多个地方“各说各话”，T08 明确以下来源优先级：

#### JSON Body 契约

- **真实线上的 body wire contract**：`snake_case`
- **前端编程接口**：`camelCase`
- **后端 Java 字段**：`camelCase`
- `@JsonAlias`：只作为临时兼容输入，不再作为主契约文档来源
- `@JsonProperty`：如果只是重复全局命名策略，不应继续作为“主定义”

#### Query 参数契约

- query contract 的主来源是：
  1. 前端 API 调用方式
  2. Controller 参数绑定方式
  3. 自动化测试验证结果
- **DTO 上的 Jackson 注解不是 query 参数契约的可靠来源**

这意味着：

- `UserListRequest` / `PartListRequest` / `PageRequest` 上的 Jackson 注解，不能再被当作“query 参数一定这样绑定”的证明
- 是否保留这些注解，要以“真实是否仍有价值”为准，而不是为了看起来统一

## 4. 兼容白名单

### 4.1 白名单判定规则

只有满足以下条件之一的兼容点，才允许保留：

1. **当前真实调用链仍依赖它**
2. **它是必要的临时外部兼容入口**
3. **删除后会直接破坏当前稳定行为，且短期内不能同步迁移**

不满足以上条件的兼容点，应在 T08 中删除或降级为非主契约实现细节。

### 4.2 本轮默认保留的兼容点

#### A. Request Body DTO 的 `@JsonAlias`

本轮对 `@JsonAlias` 不做“一刀切保留”，而是区分两类：

##### A1. 暂时保留的 camelCase 输入兼容 alias

以下 DTO 当前普遍是 `@JsonProperty("snake_case") + @JsonAlias("camelCase")` 组合，这里的 `@JsonAlias` 可以在 T08 中作为**临时兼容白名单**保留：

- `PartCreateRequest`
- `PartUpdateRequest`
- `RoleCreateRequest`
- `RoleUpdateRequest`
- `UserCreateRequest`
- `UserUpdateRequest`
- `AssignPermissionsRequest`
- `ResetPasswordRequest`
- `ChangePasswordRequest`
- `LoginRequest`

保留理由：

- 这些类的 Java 字段本身就是 `camelCase`
- 当前前端编程接口也使用 `camelCase`
- `request.js` 虽然会把真实请求转成 `snake_case`，但保留 `camelCase` alias 仍可兼容少量历史或手工调用

##### A2. 优先复核、默认不进入白名单的 snake_case alias

以下 DTO 当前主要是 `camelCase` 字段 + `@JsonAlias("snake_case")`：

- `ProduceAssignRequest`
- `ScanTraceRequest`

这类 alias **不是 camelCase 兼容入口**，而更像是对全局 snake_case 反序列化能力的重复描述。  
因此它们在 T08 中应被优先复核，默认视为**清理候选项**，而不是默认保留项。

#### B. Controller 显式参数别名

`DashboardController.topology(...)` 上的 `@RequestParam(name = "trace_code")` 视为 **T08 临时白名单**：

- 当前前端 `getTopology(traceCode, range)` 的编程接口使用 `camelCase`
- 真实请求会被 `request.js` 转成 `trace_code`
- 该点已是明确的稳定调用链，不应在没有测试兜底前直接删除

### 4.3 本轮优先清理的冗余兼容点

#### A. Request Body DTO 上仅为重复说明的 `@JsonProperty`

对于以下 body DTO，如果 `@JsonProperty("snake_case")` 只是重复 Jackson 全局命名策略，则应优先评估删除：

- `PartCreateRequest`
- `PartUpdateRequest`
- `RoleCreateRequest`
- `RoleUpdateRequest`
- `UserCreateRequest`
- `UserUpdateRequest`
- `AssignPermissionsRequest`
- `ResetPasswordRequest`
- `ChangePasswordRequest`
- `LoginRequest`

目标状态：

- Java 字段保持 `camelCase`
- JSON 输出 / 输入 `snake_case` 继续由 Jackson 全局策略承担
- `@JsonAlias` 只保留兼容输入所需部分
- 不再通过成对的 `@JsonProperty + @JsonAlias` 人工重复描述同一件事

#### B. 被当作“query 契约说明”的 Jackson 注解

以下类上的 Jackson 注解需要单独复核，不允许继续“名义上定义 query 契约，实际上无人验证”：

- `PageRequest`
- `UserListRequest`
- `PartListRequest`

这三类要么：

- 被证明没有实际价值并清理；要么
- 被更明确的 Controller 绑定方式或测试替代

但无论最终采用哪种实现，**T08 之后都不再把这些 Jackson 注解当作 query 语义文档本体**。

## 5. 本次改造范围

### 5.1 前端范围

#### Feature / core API

- `frontend/src/features/user/api/users.js`
- `frontend/src/features/user/api/roles.js`
- `frontend/src/features/part/api/parts.js`
- `frontend/src/features/trace/api/trace.js`
- `frontend/src/features/dashboard/api/dashboard.js`
- `frontend/src/core/api/auth.js`

#### 本轮前端处理目标

- 统一 JSDoc 注释，让页面侧只看到 `camelCase` 编程接口
- 注释中明确“HTTP 发出前会统一转为 `snake_case`”，但不把 `snake_case` 当作页面层主语义
- 不再把 legacy 字段名写成注释主描述

### 5.2 后端 DTO 范围

- `ProduceAssignRequest`
- `ScanTraceRequest`
- `PartCreateRequest`
- `PartUpdateRequest`
- `RoleCreateRequest`
- `RoleUpdateRequest`
- `UserCreateRequest`
- `UserUpdateRequest`
- `UserListRequest`
- `PartListRequest`
- `AssignPermissionsRequest`
- `ResetPasswordRequest`
- `ChangePasswordRequest`
- `LoginRequest`
- `PageRequest`

#### 本轮 DTO 处理目标

- 明确哪些字段的 alias 是兼容白名单
- 清理冗余 `@JsonProperty`
- 明确 query DTO 与 JSON body DTO 的职责差异
- 让注释、字段名、运行行为的描述一致

### 5.3 Controller 范围

- `DashboardController`
- `TraceController`
- `UserController`
- `RoleController`
- `PartController`
- `AuthController`

#### 本轮 Controller 处理目标

- 复核 query / path / body 的参数命名是否与前端真实调用一致
- 只保留必要的 snake_case 显式绑定
- 不改变 URL 结构，不改变业务语义，不移除 `TraceController` 中已有 legacy 路径兼容接口

## 6. 非目标

T08 明确不做以下工作：

- 不做任何 UI 改造
- 不做页面结构拆分
- 不改变接口 URL
- 不改变响应模型结构
- 不修改业务流程、权限模型、服务实现策略
- 不处理日志中文乱码、终端编码、Actuator 健康检查等与本任务无关的问题
- 不清理 `TraceController` 里的 legacy 路径兼容接口（那属于单独的接口治理工作）

## 7. 风险与控制

### 7.1 风险：删掉注解后影响 body 兼容输入

控制：

- 先加测试，再删冗余注解
- 默认只删冗余 `@JsonProperty`
- `@JsonAlias` 是否保留，必须按照白名单规则逐项判断

### 7.2 风险：query 参数绑定行为被误判

控制：

- 不依赖“看代码感觉应该能绑定”
- 对当前前端真实会发出的 query 形式做 Controller 级验证
- 对有疑问的端点，优先让测试给出结论，再决定是否补显式绑定

重点验证点至少包括：

- `GET /api/dashboard/topology?trace_code=...`
- `PATCH /api/users/{id}/role?role_id=...`
- `GET /api/users?page=1&size=10&role_id=...`
- `GET /api/parts?page=1&size=10&part_type=...`

### 7.3 风险：注释收敛后，代码行为却没同步

控制：

- 注释修改必须跟随测试与代码同步推进
- 不允许出现“注释写 camelCase，底层仍只接受别的写法”的情况
- 注释不能领先于真实实现

## 8. 测试策略

T08 的验证重点不是 UI，而是**契约语义**。

### 8.1 前端验证

延续并补强前端契约测试：

- 复用 / 扩展 `frontend/src/features/__tests__/api-contracts.test.js`
- 必要时补充 `frontend/src/core/api/__tests__/request.test.js` 或认证 API 相关测试

验证目标：

- feature / core API 对页面层仍暴露 `camelCase` 接口
- 请求发出时的 query / body 仍按预期转为 `snake_case`
- 不因为注释清理而引入新的字段漂移

### 8.2 后端验证

新增或补充两类测试：

#### A. DTO / Jackson 契约测试

验证：

- 主契约 `snake_case` body 仍可正确反序列化
- 白名单内 camelCase 输入是否仍可兼容
- 删除冗余 `@JsonProperty` 后行为不回退

#### B. Controller 参数绑定测试

验证：

- 当前前端真实发出的 query 参数格式是否能稳定绑定
- 若不能，测试先失败，再用最小修复使其通过

### 8.3 最终验证

至少完成以下验证组合：

- 相关前端契约测试通过
- 相关后端 DTO / Controller 测试通过
- 前端构建通过
- 若改动影响到真实联调链路，再做最小 smoke 验证

## 9. 推荐实施顺序

1. 盘点当前 body DTO / query DTO / controller 参数的兼容点
2. 先写后端 DTO / controller 契约测试
3. 写前端 API 契约补充测试
4. 清理前端 API 注释与命名说明
5. 清理冗余 `@JsonProperty`
6. 收口 query 参数绑定语义，只保留必要白名单
7. 跑完整体验证并更新任务表

## 10. 完成标准

T08 完成时必须满足：

- 前端 API 注释以 `camelCase` 为唯一编程语义
- 后端 body DTO 不再普遍使用冗余的 `@JsonProperty + @JsonAlias` 双写组合
- 兼容点有明确白名单，不再“默认全部兼容”
- query 参数契约由真实绑定行为和测试说话，不再依赖无效注解充当文档
- 不引入新的 UI 改造
- 相关自动化验证通过
