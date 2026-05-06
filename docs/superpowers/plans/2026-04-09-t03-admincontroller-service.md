# T03 AdminController 服务化实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不改变 `/api/admin/generate-sample-data` 与 `/api/admin/clear-trace-data` 外部行为的前提下，将示例数据生成逻辑从 `AdminController` 迁移到通用服务 `TraceDemoDataService`。

**Architecture:** 新增 `TraceDemoDataService` 接口与 `TraceDemoDataServiceImpl` 实现，承接原控制器中的示例配件、区域、生命周期生成与清理逻辑；`AdminController` 收敛为参数接收、服务调用和响应返回三段式入口。测试先覆盖控制器委托与服务核心编排，再进行迁移，确保行为不漂移。

**Tech Stack:** Java 19, Spring Boot 3, MyBatis-Plus, JUnit 5, Mockito

---

### Task 1: 标记 T03 开始并补充实施文档

**Files:**
- Modify: `项目整改执行任务表.md`
- Create: `docs/superpowers/plans/2026-04-09-t03-admincontroller-service.md`

- [ ] **Step 1: 将任务表中的 T03 标记为进行中**
- [ ] **Step 2: 保存实施计划文档**
- [ ] **Step 3: 自检计划与任务表约束一致**

### Task 2: 为控制器委托行为先写失败测试

**Files:**
- Create: `backend/src/test/java/com/example/trace/controller/AdminControllerTest.java`
- Modify: `backend/src/main/java/com/example/trace/controller/AdminController.java`
- Create: `backend/src/main/java/com/example/trace/service/TraceDemoDataService.java`

- [ ] **Step 1: 写 `generateSampleData` 的委托测试**
- [ ] **Step 2: 写 `clearTraceData` 的委托测试**
- [ ] **Step 3: 运行控制器测试并确认 RED**

Run: `cd backend; mvn -Dtest=AdminControllerTest test`
Expected: FAIL，原因应为 `TraceDemoDataService` 尚未存在且 `AdminController` 仍直接依赖 mapper/工具类。

### Task 3: 增加服务接口与最小控制器改造让测试转绿

**Files:**
- Create: `backend/src/main/java/com/example/trace/service/TraceDemoDataService.java`
- Modify: `backend/src/main/java/com/example/trace/controller/AdminController.java`
- Test: `backend/src/test/java/com/example/trace/controller/AdminControllerTest.java`

- [ ] **Step 1: 新增服务接口**
- [ ] **Step 2: 将控制器改为依赖服务并直接返回 `ApiResponse.success(...)`**
- [ ] **Step 3: 再跑控制器测试确认 GREEN**

### Task 4: 为服务编排写失败测试

**Files:**
- Create: `backend/src/test/java/com/example/trace/service/impl/TraceDemoDataServiceImplTest.java`
- Create: `backend/src/main/java/com/example/trace/service/impl/TraceDemoDataServiceImpl.java`

- [ ] **Step 1: 写清理数据服务测试**
- [ ] **Step 2: 写生成示例数据服务测试**
- [ ] **Step 3: 运行服务测试并确认 RED**

Run: `cd backend; mvn -Dtest=TraceDemoDataServiceImplTest test`
Expected: FAIL，原因应为 `TraceDemoDataServiceImpl` 尚未存在。

### Task 5: 迁移生成/清理逻辑到服务实现

**Files:**
- Create: `backend/src/main/java/com/example/trace/service/impl/TraceDemoDataServiceImpl.java`
- Modify: `backend/src/main/java/com/example/trace/controller/AdminController.java`
- Test: `backend/src/test/java/com/example/trace/service/impl/TraceDemoDataServiceImplTest.java`

- [ ] **Step 1: 创建服务实现并注入依赖**
- [ ] **Step 2: 迁移私有生成逻辑**
- [ ] **Step 3: 将对外方法落到服务接口上**
- [ ] **Step 4: 运行定向测试确认 GREEN**

Run: `cd backend; mvn -Dtest=AdminControllerTest,TraceDemoDataServiceImplTest test`
Expected: PASS

### Task 6: 全量回归并更新任务表

**Files:**
- Modify: `项目整改执行任务表.md`

- [ ] **Step 1: 跑后端测试回归**
- [ ] **Step 2: 更新任务表为完成态**
- [ ] **Step 3: 自检只完成 T03**

Run: `cd backend; mvn test`
Expected: PASS，全部测试通过。
