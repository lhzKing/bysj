# T03 AdminController 服务化改造设计

**日期**：2026-04-09
**任务编号**：T03
**任务名称**：重构 `AdminController` 为服务化实现

## 1. 目标

在不改变现有接口路径、请求方式、返回结构和外部行为的前提下，将 `AdminController` 中的示例数据生成逻辑迁移到可复用的通用服务 `TraceDemoDataService` 中，使 Controller 仅保留接口入口职责。

## 2. 改造范围

### 涉及文件
- 修改：`backend/src/main/java/com/example/trace/controller/AdminController.java`
- 新增：`backend/src/main/java/com/example/trace/service/TraceDemoDataService.java`
- 新增：`backend/src/main/java/com/example/trace/service/impl/TraceDemoDataServiceImpl.java`
- 新增/修改测试：围绕 Controller 委托与 Service 逻辑补充单元测试

### 不做的内容
- 不修改对外 API
- 不修改数据库结构
- 不顺带处理其他控制器重构
- 不额外引入新的初始化入口或脚本入口

## 3. 方案对比

### 方案 A：仅抽 `AdminSampleDataService`
优点：改动最小。
缺点：语义仍绑定 Admin，不利于后续复用。

### 方案 B：抽 `TraceDemoDataService`（采用）
优点：语义清晰，既可服务 AdminController，也可为后续测试、演示数据初始化等场景复用。
缺点：需要多建一层 service 接口与实现。

### 方案 C：进一步抽象为更泛化编排器
优点：长期扩展性更强。
缺点：当前属于过度设计，超出 T03 范围。

## 4. 最终设计

### 4.1 控制层职责
`AdminController` 仅负责：
- 接收请求参数
- 调用 `TraceDemoDataService`
- 返回接口结果

控制层中原本的：
- 示例配件定义
- 区域定义
- 随机链路生成
- 日志/快照写入流程
- 状态推进逻辑

都迁移到 service 层。

### 4.2 服务层职责
新增 `TraceDemoDataService`，负责：
- 生成示例配件规格
- 生成示例溯源链路
- 生成日志与快照
- 封装随机规则与演示数据策略

### 4.3 边界要求
- Controller 不再直接拼装示例业务数据
- 生成逻辑保留现有行为与结果风格
- 保持现有 mapper 调用链路可用
- 若 service 内部仍偏长，可优先以内聚私有方法拆分，不在 T03 中继续扩散为更多 public service

## 5. 数据流

1. 请求进入 `AdminController`
2. Controller 调用 `TraceDemoDataService`
3. Service 执行示例数据生成
4. Service 返回结果
5. Controller 统一封装响应

## 6. 测试策略

### 先补测试
- Controller 委托测试：确认 Controller 只负责参数接收与 service 调用
- Service 测试：确认示例数据生成关键路径仍可执行

### 回归验证
- `cd backend && mvn test`

## 7. 风险与控制

### 风险
- 搬迁代码时行为偏移
- 随机生成逻辑迁移后细节遗漏
- Controller 与 Service 职责边界不彻底

### 控制措施
- 先补测试再迁移
- 以“复制后收敛”的方式迁移，避免边改边猜
- 最终检查 Controller 是否只剩接口入口与参数处理逻辑

## 8. 完成标准映射

任务表要求：
> Controller 仅保留接口入口，生成逻辑迁移到 service

本设计完成后应满足：
- `AdminController` 不再直接承担示例数据生成细节
- 示例数据生成逻辑集中在 `TraceDemoDataService`
- 对外接口保持不变
