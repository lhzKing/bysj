# T07 前端契约收口与 `transform.js` 统一转换设计

**日期**：2026-04-11  
**任务编号**：T07  
**任务名称**：让 `transform.js` 成为前端唯一契约收口层

## 1. 目标

在不继续推进任何 UI 改造的前提下，T07 聚焦前端与后端的数据契约边界，完成以下目标：

- 让前端**内部数据模型统一使用 camelCase**
- 让前端与后端交互时，由**单一契约层**统一处理 snake_case / camelCase 转换
- 移除页面、组件、API 调用层中分散的 `a_b || aB` 双格式兼容写法
- 为后续 T08、T09、T10、T12、T13 的逻辑拆分与契约治理提供稳定基础

T07 不改视觉样式，不重做页面结构，不调整现有业务流程本身，只治理“命名契约”和“字段转换责任”。

---

## 2. 当前现状

### 2.1 后端现状

后端已具备较明确的契约方向：

- `backend/src/main/java/com/example/trace/config/JacksonConfig.java` 已确认：**全局输出使用 snake_case**
- 多个 DTO 已通过 `@JsonAlias` 兼容 camelCase 输入
- 这意味着后端对前端输入已有一定兼容，但**输出规范已经确定为 snake_case**

因此，前端最合理的做法不是继续在页面中混用两种命名，而是明确：

- 边界对外：接受/发送 snake_case
- 边界对内：统一使用 camelCase

### 2.2 前端现状

当前前端存在三个明显问题：

1. `frontend/src/shared/utils/transform.js` 仅覆盖少量对象：
   - `transformSnapshot`
   - `transformTraceLog`
   - `transformUser`
   - `transformArray`

2. 页面层仍然大量手写双格式兜底，例如：
   - `part.part_code || part.partCode`
   - `role.role_name || role.roleName`
   - `res.total_logs || res.totalLogs`

3. 请求侧没有统一收口：
   - 有些页面内部状态仍然直接用 snake_case
   - 有些请求 payload 直接传 snake_case
   - 有些查询参数和返回值约定散落在 feature API 或页面中

这会导致：

- 页面逻辑可读性差
- 相同字段在不同页面写法不一致
- 后续拆分页面/服务逻辑时，容易继续复制命名兼容代码

---

## 3. 设计结论

### 3.1 总体方案

采用**方案一：在 `transform.js` 做统一契约层，并在 `request.js` 接入统一转换**。

也就是说：

- **请求发出前**
  - `params`、`data` 中的普通对象统一转换为 snake_case
- **响应返回后**
  - 将后端返回的 `data` 统一递归转换为 camelCase
- **前端内部**
  - 页面、组件、组合式函数、feature API 默认只使用 camelCase

最终边界如下：

```text
Vue 页面 / 组件 / composables / feature API
            ↓（全部 camelCase）
       core/api/request.js
            ↓
     shared/utils/transform.js
            ↓（统一 snake_case）
            后端 API
```

### 3.2 统一命名原则

T07 后的统一约束：

- 前端内部对象字段：camelCase
- 前端表单状态：camelCase
- 前端查询对象：camelCase
- 前端 API 返回值：camelCase
- 前端写给后端的对象：通过契约层自动转换为 snake_case

例子：

- `role_id` → `roleId`
- `part_code` → `partCode`
- `trace_code` → `traceCode`
- `total_logs` → `totalLogs`
- `hash_verified_count` → `hashVerifiedCount`

---

## 4. 契约层职责划分

### 4.1 `transform.js` 的新职责

`frontend/src/shared/utils/transform.js` 从“少量实体转换工具”升级为“前端唯一契约转换层”，至少承担以下职责：

1. **key 转换能力**
   - `snake_case -> camelCase`
   - `camelCase -> snake_case`

2. **递归对象转换**
   - 支持普通对象
   - 支持数组
   - 支持嵌套对象
   - 保持原始标量值不变

3. **边界保护**
   - 不处理 `FormData`
   - 不处理 `Blob`
   - 不处理 `File`
   - 不处理 `Date`
   - 不处理 Axios config 中非数据业务字段

4. **兼容保留**
   - `transformSnapshot`
   - `transformTraceLog`
   - `transformUser`
   - `transformArray`

这些现有函数可以保留，但实现上应尽量复用新的通用转换函数，而不是继续维护独立字段映射表。

### 4.2 `request.js` 的职责

`frontend/src/core/api/request.js` 成为统一接入点：

#### 请求阶段

- 将 `config.params` 转为 snake_case
- 将 `config.data` 转为 snake_case
- 保留现有 token 注入逻辑
- 保留现有错误提示逻辑

#### 响应阶段

- 对成功响应中的 `res.data` 做 camelCase 转换
- 再返回业务层使用
- 错误响应优先保持 `message` 可读，不要求页面再自行兼容 snake_case

#### 例外边界

以下情况不自动深转：

- `FormData`
- 二进制内容
- 明确声明需跳过转换的特殊调用

本次 T07 若项目内尚未出现这些调用，只需在实现中保留防御性分支即可，不额外扩展新接口设计。

---

## 5. 页面/API 层的收口范围

### 5.1 本次必须清理的典型页面

为证明“契约已统一收口”，T07 至少应清理以下已发现的双格式兼容点：

- `frontend/src/features/user/views/UserList.vue`
- `frontend/src/features/user/views/RoleList.vue`
- `frontend/src/features/part/views/PartList.vue`
- `frontend/src/features/trace/views/TraceDetail.vue`
- `frontend/src/features/trace/components/CreateTraceDialog.vue`
- `frontend/src/features/dashboard/views/Dashboard.vue`

这些文件清理后，应满足：

- 页面内部不再依赖 `snake || camel` 读法
- 表单与查询对象改为 camelCase
- 页面中读取 API 数据时直接使用 camelCase

### 5.2 feature API 的处理原则

以下 API 文件作为边界适配入口，需要同步到 camelCase 语义：

- `frontend/src/features/user/api/users.js`
- `frontend/src/features/user/api/roles.js`
- `frontend/src/features/part/api/parts.js`
- `frontend/src/features/trace/api/trace.js`
- `frontend/src/features/dashboard/api/dashboard.js`

要求：

- 对外暴露给页面层的方法，注释语义以 camelCase 为准
- 若入参来自页面对象，应允许页面直接传 camelCase
- 由 `request.js + transform.js` 负责最终 snake_case 序列化

T07 不要求把所有注释完全重写到最终形态；更完整的注释/DTO 对齐在 T08 继续完成。但 T07 至少要让**代码真实行为**先统一。

---

## 6. 非目标与边界控制

T07 明确不做以下内容：

- 不继续推进任何 UI 风格改造
- 不重构页面视觉层
- 不拆分页面职责（这是 T09-T13 的工作）
- 不改后端 DTO 结构
- 不改后端接口 URL 或业务语义
- 不在本任务中做大规模 API 文档重写（留给 T08）

本任务只解决：

- 前端内部命名不统一
- 字段转换责任分散
- 页面层重复兼容后端返回格式

---

## 7. 风险与化解

### 7.1 风险：全局请求转换误伤特殊对象

风险点：

- 若未来请求中出现 `FormData` 或特殊非 plain object 数据，递归转换可能破坏请求结构

化解方式：

- 在 `transform.js` 明确识别 plain object
- 对 `FormData`、`Blob`、`File`、`Date` 直接跳过转换

### 7.2 风险：页面局部仍残留旧命名

风险点：

- 若页面只改了 API 层但没改内部字段命名，仍会继续出现混用

化解方式：

- T07 实现必须同时清理代表性页面中的 `snake || camel` 写法
- 用测试锁定关键对象转换和请求/响应转换行为

### 7.3 风险：通用转换影响错误处理

风险点：

- 若错误分支中的响应对象结构变化，可能影响 toast 或页面错误展示

化解方式：

- 保留现有 `message` 提取优先级
- 对错误对象只做最小必要兼容，不重写整个异常模型

---

## 8. 测试策略

T07 必须采用 TDD，至少覆盖以下三层：

### 8.1 `transform.js` 单元测试

验证内容：

- camelCase 转 snake_case
- snake_case 转 camelCase
- 深层对象与数组递归转换
- `FormData` / `Date` / 非 plain object 不被误转换

### 8.2 `request.js` 契约测试

验证内容：

- 请求 `params` 会被序列化为 snake_case
- 请求 `data` 会被序列化为 snake_case
- 成功响应会被转换为 camelCase 后返回
- 错误响应仍能保留正确 message

### 8.3 受影响页面最小回归测试

至少选取一个页面或一个调用链，验证：

- 页面或组件不再依赖 snake_case 返回值
- 经过 API 层后，页面拿到的是 camelCase 数据

这部分测试目标不是覆盖 UI，而是证明“页面逻辑已经摆脱双命名兼容”。

---

## 9. 实施顺序

推荐实施顺序：

1. 先为 `transform.js` 写通用转换失败测试
2. 实现通用 key / deep transform 能力
3. 为 `request.js` 写请求/响应转换失败测试
4. 在 Axios 拦截器中接入统一转换
5. 清理代表性页面中的 `snake || camel` 兼容逻辑
6. 运行相关单测、前端全量测试与构建
7. 更新任务表状态与验证记录

---

## 10. 完成标准

T07 完成时必须满足：

- `transform.js` 已升级为通用契约转换层
- `request.js` 已接入请求/响应统一转换
- 页面层不再大面积出现 `a_b || aB` 双格式兼容写法
- 至少已清理本设计中列出的代表性页面
- 关键转换路径具备自动化测试
- 不引入新的 UI 改造范围

只要页面仍然普遍手写 `snake || camel`，就不能视为 T07 完成。
