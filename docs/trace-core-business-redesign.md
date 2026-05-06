# 溯源码核心业务重设计分析文档

> 日期：2026-05-05  
> 范围：工业零配件供应链溯源系统的生产赋码、扫码流转、批次管理、单品码一致性、前线用户操作体验  
> 状态：讨论稿 v0.1

## 1. 背景与核心问题

当前系统已经具备较完整的技术链路：

- 生产赋码：创建溯源码、初始化日志、写入快照。
- 扫码流转：追加生命周期事件、更新当前快照。
- 防篡改：Hash 链、RSA 签名、验链接口。
- 纠错：通过 `CORRECTION / correctionOf` 支持红冲蓝补思路。

但从真实工厂和供应链落地角度看，还需要进一步澄清三个问题：

1. **批次赋码是否能贴合真实厂家生产流程？**
2. **一个物品一个溯源码的原则与批次赋码是否冲突？**
3. **扫码流转操作是否足够便捷，前线用户是否愿意使用？**

本文件围绕这三个问题进行业务重设计分析，并给出分阶段落地方案。

---

## 2. 关键结论

### 2.1 批次赋码可以落地，但“批次”不能等同于“一个批次一个码”

在实际生产中，批次赋码非常常见，但它的含义应是：

> **按生产计划、工单或打印任务一次性生成 N 个唯一单品码。**

而不是：

> 一个批次共用一个溯源码。

因此，系统应该明确区分：

| 概念 | 含义 | 示例 |
|---|---|---|
| 生产批次 | 工厂生产管理单位 | 20260505-BJ-001 |
| 赋码批次 | 一次生成/打印码的任务 | ASSIGN-20260505-0001 |
| 单品溯源码 | 每个物品唯一身份 | TRACE-UUID-001 |
| 箱码/托盘码 | 聚合包装单位 | CARTON-001 / PALLET-001 |

批次赋码是为了提高生产效率和管理可追溯性；单品溯源码是为了保证每个物品可独立追踪。二者并不冲突。

### 2.2 一物一码可以保证，但不能只靠数据库唯一约束

当前 `traceCode` 主键可以保证系统内“码唯一”，但真实世界的“一物一码”还需要流程配合：

- 每生成一个码，都必须有独立 `trace_code` 记录。
- 每个码只能被激活一次。
- 贴码后需要扫码激活或扫码复核。
- 打印损坏、漏贴、错贴、重贴都必须有状态和事件记录。
- 出库前应做数量对账：生产数量、打印数量、激活数量、入库数量必须一致或有异常说明。

也就是说：

> 软件可以保证“一个码只对应一个系统身份”；流程才能保证“一个实物只贴一个正确码”。

### 2.3 当前扫码流转偏“自由填表”，对一线用户还不够友好

当前前端流程大致是：

1. 用户打开扫码模块。
2. 扫到 traceCode。
3. 选择入库、出库、流转、异常。
4. 手动填写 fromNode、toNode、省份、城市、时间、备注。
5. 提交流转事件。

这个流程对演示系统足够，但对真实仓库/物流人员偏重。真实场景中，一线用户更希望：

- 扫一下就知道该做什么。
- 系统自动识别当前节点和用户角色。
- 系统自动填充起点、终点、时间、地区。
- 大批量操作时不需要逐件填表。
- 出错时只提示“下一步怎么做”，而不是暴露复杂字段。

因此推荐从“自由填表式扫码”改为“任务驱动式扫码”。

---

## 3. 真实厂家生产赋码落地流程设计

### 3.1 推荐落地流程

生产赋码建议设计为以下闭环：

```text
生产计划/工单
   ↓
创建赋码批次
   ↓
生成 N 个唯一单品溯源码
   ↓
打印二维码标签
   ↓
贴码
   ↓
扫码激活/复核
   ↓
质检通过
   ↓
成品入库
```

对应系统动作：

| 业务步骤 | 系统事件 | 说明 |
|---|---|---|
| 创建赋码任务 | `ASSIGN_BATCH_CREATED` | 记录工单、产品、数量、生产线 |
| 生成单品码 | `ASSIGN_CODE` | 为每个物品生成唯一 traceCode |
| 打印标签 | `PRINT_CODE` | 记录打印任务、打印人、打印次数 |
| 贴码复核 | `ACTIVATE_CODE` | 扫描标签，确认该码已绑定实物 |
| 质检通过 | `QUALITY_PASS` | 可选，适合工业配件 |
| 入库 | `INBOUND` | 进入生产成品库 |

### 3.2 批次赋码的四种落地模式

#### 模式 A：预生成标签，再贴到实物

流程：

```text
先生成 500 个码 → 打印 500 张标签 → 生产完成后贴标 → 扫码复核
```

优点：

- 简单，适合毕业设计和小型工厂。
- 打印、贴码、入库流程清晰。

风险：

- 标签可能丢失。
- 标签可能贴错。
- 生产实际数量可能少于生成数量。

必须配套：

- 标签状态：`GENERATED / PRINTED / ACTIVATED / VOIDED`
- 激活扫描：未激活的码不能出库。
- 余码作废：未贴的标签要批量作废。

#### 模式 B：生产完成后再生成码

流程：

```text
生产完成并清点数量 → 按实际数量生成码 → 打印贴标 → 入库
```

优点：

- 不容易出现多余标签。
- 数量更准确。

缺点：

- 贴标动作后置，生产线效率可能低。

适用：

- 小批量、定制化、贵重配件。

#### 模式 C：边生产边打印贴码

流程：

```text
每下线一个产品 → 自动生成/领取一个码 → 打印贴标 → 扫码激活
```

优点：

- 最贴近“一物一码”。
- 错贴概率最低。

缺点：

- 需要产线设备或扫码枪配合。
- 系统复杂度更高。

适用：

- 正式生产系统、自动化产线。

#### 模式 D：混合模式

流程：

```text
先按计划生成码池 → 生产线按需领取码 → 贴码后激活 → 未使用码退回/作废
```

优点：

- 兼顾效率和准确性。

缺点：

- 需要码池、领取、回收、作废状态。

适用：

- 中大型工厂。

### 3.3 对当前项目最合适的落地方案

考虑当前项目复杂度，建议采用：

> **模式 A 的简化版 + 激活复核机制。**

即：

1. 生产人员选择配件、数量、生产节点。
2. 系统创建一个赋码批次。
3. 系统为每件物品生成唯一 traceCode。
4. 打印二维码。
5. 贴码后，生产人员用扫码枪/摄像头扫描每个码，执行 `ACTIVATE_CODE`。
6. 只有已激活的码才能进入后续入库、出库、流转。

这样既能体现真实生产流程，也不会让毕业项目复杂度失控。

---

## 4. 一物一码与批次赋码的关系

### 4.1 正确定义

一物一码不是说“一次只能生成一个码”，而是说：

> 每一个实物最终必须绑定一个唯一、不可复用的溯源码。

批次赋码是说：

> 系统可以一次性生成多个唯一溯源码，以适配生产批量操作。

二者关系如下：

```text
赋码批次 ASSIGN-001
├── traceCode A → 实物 1
├── traceCode B → 实物 2
├── traceCode C → 实物 3
└── traceCode D → 实物 4
```

批次只是管理容器，不是物品身份。

### 4.2 需要新增的码状态

建议新增单品码状态：

| 状态 | 含义 |
|---|---|
| `GENERATED` | 已生成，未打印 |
| `PRINTED` | 已打印 |
| `ACTIVATED` | 已贴到实物并扫码确认 |
| `IN_STOCK` | 已入库 |
| `IN_TRANSIT` | 流转中 |
| `EXCEPTION` | 异常 |
| `VOIDED` | 作废 |
| `SCRAPPED` | 报废 |

当前系统直接从 `INIT` 进入流转，缺少 `GENERATED / PRINTED / ACTIVATED` 这几个很关键的生产落地状态。

### 4.3 防止一物多码/一码多物的机制

建议从四层保证：

#### 数据层

- `trace_code` 主键唯一。
- `batch_id + serial_no` 唯一。
- `trace_code.status` 不允许非法回退。
- 已激活码不能再次激活到另一个实物。

#### 业务层

- 激活前必须扫码。
- 激活时记录操作者、生产线、设备、时间。
- 出库前必须校验该码已激活。
- 作废码不能再次使用。

#### 操作层

- 贴码后进行扫码复核。
- 生产数量、打印数量、激活数量、入库数量做对账。
- 异常数量必须填写原因。

#### 审计层

- 所有激活、作废、重打、纠错都进入不可篡改日志链。
- 管理员可以查看完整审计历史。

### 4.4 标签损坏、重打、错贴如何处理

真实落地中一定会出现这些情况，因此必须设计业务规则。

| 场景 | 推荐处理 |
|---|---|
| 未激活标签损坏 | 允许重打同一 traceCode，记录 `REPRINT_CODE` |
| 未激活标签丢失 | 作废该 traceCode，记录 `VOID_CODE` |
| 已激活标签损坏 | 允许补打同一 traceCode，但必须记录补打事件 |
| 错贴但未流转 | 解绑/作废并重新贴码，记录纠错 |
| 已出库后发现错贴 | 不直接删除，使用 `CORRECTION` + `EXCEPTION` |

---

## 5. 扫码流转用户体验分析

### 5.1 当前体验问题

当前用户扫码后，需要手动完成较多输入：

- 选择动作。
- 填写来源节点。
- 填写目标节点。
- 选择省份。
- 选择城市。
- 选择时间。
- 填写备注。

这对于后台管理人员可以接受，但对仓库、物流、生产线人员不够友好。

主要问题：

1. **输入项太多**：扫码本应降低录入成本，但现在扫码后仍像填表。
2. **字段含义偏技术化**：fromNode/toNode 对一线人员不一定直观。
3. **容易填错节点**：节点靠手输，无法保证与用户所在组织一致。
4. **批量操作效率低**：如果一批 500 件逐一扫码并填表，用户不可接受。
5. **动作选择依赖用户理解**：用户需要知道什么时候选入库、出库、流转。

### 5.2 推荐交互原则

#### 原则 1：扫码后系统判断“你能做什么”

扫码后，系统根据：

- 当前码状态
- 当前用户角色
- 用户所属节点
- 是否存在待处理任务
- 当前货物所在节点

自动给出可执行动作，而不是让用户随意选择。

例如：

```text
当前用户：上海仓库人员
当前码状态：IN_TRANSIT
目标节点：上海仓库

扫码后系统显示：
【确认入库】按钮
```

而不是显示入库、出库、流转、异常全部选项。

#### 原则 2：能自动填的字段不要让用户填

| 字段 | 推荐来源 |
|---|---|
| eventTime | 默认当前时间，可高级修改 |
| operator | 登录用户 |
| fromNode | 当前快照或待办任务 |
| toNode | 用户所属节点或任务目标 |
| province/city | 节点基础资料 |
| actionType | 根据任务和状态推导 |

用户只应填写：

- 异常备注
- 必要的业务单号
- 少量确认信息

#### 原则 3：常规操作一键完成，异常操作再填说明

常规入库：

```text
扫码 → 系统识别待入库 → 点击确认 → 完成
```

异常上报：

```text
扫码 → 点击异常 → 选择异常类型 → 填写说明 → 提交
```

把复杂度留给异常场景，不要让每次正常流转都很复杂。

---

## 6. 推荐的扫码流转业务模式

### 6.1 任务驱动模式

建议把扫码流转从“自由录入事件”改为“处理任务”。

核心表：

```text
trace_flow_task
- id
- task_no
- task_type: INBOUND / OUTBOUND / TRANSFER / RECEIVE
- source_node_id
- target_node_id
- expected_quantity
- actual_quantity
- status: CREATED / PROCESSING / COMPLETED / EXCEPTION
- operator_id
- create_time
```

用户不是直接创建任意事件，而是完成任务。

示例：

```text
仓库管理员创建出库任务：北京仓 → 上海仓，数量 100
仓库人员扫描商品码或箱码
系统自动记录 OUTBOUND
物流或上海仓扫码确认 RECEIVE / INBOUND
任务完成后，系统校验实际数量是否等于 100
```

### 6.2 单品码 + 箱码/托盘码聚合

为了兼顾“一物一码”和“操作便捷”，推荐引入码层级：

```text
托盘码 PALLET-001
└── 箱码 CARTON-001
    ├── 单品码 TRACE-001
    ├── 单品码 TRACE-002
    └── 单品码 TRACE-003
```

业务规则：

- 每个物品仍有唯一单品码。
- 箱码/托盘码只是聚合操作入口。
- 扫箱码出库时，系统自动给箱内所有单品追加流转事件。
- 消费者或审计员查单品码时，仍能看到完整历史。

这样可以解决大批量流转效率问题：

> 用户不用对 500 个配件逐一操作，可以扫箱码或托盘码完成批量流转，同时不牺牲单品追溯能力。

### 6.3 推荐的一线用户流程

#### 生产人员

```text
创建赋码批次
打印标签
贴码
扫码激活
批量提交入库
```

用户感知：

- 主要操作是“创建批次”和“扫码确认”。
- 不需要理解 hash、签名、节点流转。

#### 仓库人员

```text
查看待办任务
选择出库/入库任务
扫码商品码/箱码
系统自动累计数量
点击完成任务
```

用户感知：

- 类似快递/仓储 PDA 操作。
- 不需要每次手填 fromNode/toNode。

#### 物流人员

```text
扫描运输单或箱码
确认装车
到达中转点扫码
到达目标仓扫码交接
```

用户感知：

- 以运输单、箱码为主。
- 单品码作为异常追查入口。

#### 查询用户/客户

```text
扫码
查看产品基本信息
查看关键节点
查看验链结果
```

用户感知：

- 不需要登录也能查公开信息。
- 敏感信息做脱敏。

---

## 7. 业务状态机建议

### 7.1 状态定义

建议从当前 `INIT / IN_STOCK / IN_TRANSIT / TRANSFERRED / EXCEPTION` 扩展为：

| 状态 | 含义 |
|---|---|
| `GENERATED` | 码已生成 |
| `PRINTED` | 标签已打印 |
| `ACTIVATED` | 已贴码激活 |
| `IN_STOCK` | 在库 |
| `OUTBOUND_PENDING` | 已创建出库任务 |
| `IN_TRANSIT` | 运输中 |
| `RECEIVED` | 已接收 |
| `INSTALLED` | 已安装/投入使用 |
| `EXCEPTION_HELD` | 异常冻结 |
| `VOIDED` | 作废 |
| `SCRAPPED` | 报废 |

### 7.2 事件定义

| 事件 | 含义 |
|---|---|
| `ASSIGN_CODE` | 生成单品码 |
| `PRINT_CODE` | 打印标签 |
| `REPRINT_CODE` | 重打标签 |
| `ACTIVATE_CODE` | 贴码激活 |
| `QUALITY_PASS` | 质检通过 |
| `INBOUND` | 入库 |
| `OUTBOUND` | 出库 |
| `TRANSFER_START` | 开始运输 |
| `TRANSFER_ARRIVE` | 到达 |
| `RECEIVE` | 接收确认 |
| `INSTALL` | 安装 |
| `EXCEPTION_OPEN` | 异常开启 |
| `EXCEPTION_CLOSE` | 异常关闭 |
| `CORRECTION` | 红冲蓝补 |
| `VOID_CODE` | 作废码 |
| `SCRAP` | 报废 |

### 7.3 状态流转规则示例

| 当前状态 | 允许事件 | 新状态 |
|---|---|---|
| `GENERATED` | `PRINT_CODE` | `PRINTED` |
| `PRINTED` | `ACTIVATE_CODE` | `ACTIVATED` |
| `ACTIVATED` | `INBOUND` | `IN_STOCK` |
| `IN_STOCK` | `OUTBOUND` | `IN_TRANSIT` |
| `IN_TRANSIT` | `RECEIVE` | `RECEIVED` |
| `RECEIVED` | `INBOUND` | `IN_STOCK` |
| 任意正常状态 | `EXCEPTION_OPEN` | `EXCEPTION_HELD` |
| `EXCEPTION_HELD` | `EXCEPTION_CLOSE` | 恢复原状态 |
| 任意状态 | `CORRECTION` | 状态不变 |
| `GENERATED / PRINTED` | `VOID_CODE` | `VOIDED` |

---

## 8. 数据模型改造建议

### 8.1 赋码批次表

```sql
trace_assign_batch
- id
- batch_no
- production_order_no
- spu_id
- quantity_requested
- quantity_generated
- quantity_printed
- quantity_activated
- manufacturer_node_id
- status
- operator_id
- create_time
- update_time
```

### 8.2 单品码表

```sql
trace_code
- trace_code
- batch_id
- spu_id
- serial_no
- qr_payload
- code_status
- print_count
- activated_time
- activated_by
- current_snapshot_id
- create_time
- update_time
```

### 8.3 节点表

```sql
trace_node
- id
- node_code
- node_name
- node_type: FACTORY / WAREHOUSE / LOGISTICS / CUSTOMER / SERVICE
- org_id
- province
- city
- address
- enabled
```

### 8.4 流转任务表

```sql
trace_flow_task
- id
- task_no
- task_type
- source_node_id
- target_node_id
- expected_quantity
- actual_quantity
- status
- create_by
- create_time
- complete_time
```

### 8.5 聚合关系表

```sql
trace_aggregation
- parent_code
- child_code
- relation_type: CARTON / PALLET / BATCH
- active
- create_time
```

### 8.6 生命周期日志扩展字段

当前 `trace_lifecycle_log` 建议扩展：

```text
event_id
batch_id
task_id
from_node_id
to_node_id
operator_user_id
operator_org_id
device_id
biz_step
disposition
payload_json
idempotency_key
```

Hash 和签名应尽量覆盖这些关键字段。

---

## 9. API 改造建议

### 9.1 赋码

```http
POST /api/trace-batches
```

创建赋码批次。

```http
POST /api/trace-batches/{batchId}/codes
```

为批次生成 N 个单品码。

```http
POST /api/trace-codes/{traceCode}/activate
```

扫码激活。

### 9.2 流转任务

```http
POST /api/trace-flow-tasks
```

创建流转任务。

```http
POST /api/trace-flow-tasks/{taskId}/scan
```

任务内扫码。

```http
POST /api/trace-flow-tasks/{taskId}/complete
```

完成任务。

### 9.3 单品详情

```http
GET /api/traces/{traceCode}?view=effective
GET /api/traces/{traceCode}?view=audit
```

分别返回业务有效视图和审计完整视图。

---

## 10. 前端交互改造建议

### 10.1 当前扫码页保留，但定位调整

当前扫码页可以保留为“快捷扫码入口”，但建议加一层后端判断：

```text
扫码 traceCode
   ↓
请求 /api/traces/{traceCode}/available-actions
   ↓
后端返回当前用户可执行动作
   ↓
前端展示一键操作按钮
```

### 10.2 新增任务工作台

建议新增：

```text
生产赋码工作台
仓库任务工作台
物流流转工作台
异常处理工作台
```

一线用户优先从任务进入，而不是从“自由扫码填表”进入。

### 10.3 批量扫码体验

推荐支持：

- 连续扫码模式。
- 扫箱码批量加入。
- 扫重复码提示“已扫描”。
- 实时显示已扫数量 / 应扫数量。
- 完成时一键提交。

---

## 11. 分阶段落地计划

### 阶段 1：最小业务增强

目标：不大改表结构，先提升业务合理性。

建议任务：

1. 新增 `TraceTransitionPolicy`，校验状态流转是否合法。
2. 后端根据 snapshot 自动推导 `fromNode`。
3. 扫码接口增加幂等键，防止重复提交。
4. Hash/签名加入 operator。
5. 详情接口支持 `effective / audit` 两种视图。
6. 前端减少必填项，默认填充时间、来源节点、位置。

### 阶段 2：引入批次与码状态

目标：让赋码能贴合真实生产。

建议任务：

1. 新增赋码批次表。
2. 新增单品码状态。
3. 增加打印、激活、作废事件。
4. 前端新增赋码批次管理页面。
5. 生产扫码激活闭环。

### 阶段 3：引入节点和任务流

目标：让流转操作贴合仓库和物流。

建议任务：

1. 新增节点表。
2. 用户绑定组织/节点。
3. 新增流转任务。
4. 扫码根据任务自动生成事件。
5. 入库/出库形成发出 + 接收闭环。

### 阶段 4：聚合码与批量流转

目标：解决大批量操作效率。

建议任务：

1. 新增箱码/托盘码。
2. 新增聚合关系。
3. 扫父码批量流转子码。
4. 单品详情展示聚合历史。

---

## 12. 当前项目可优先修改的点

结合现有代码，优先级建议如下：

| 优先级 | 修改点 | 原因 |
|---|---|---|
| P0 | 状态机校验 | 当前流转规则过宽 |
| P0 | 后端推导 fromNode | 减少用户手填，降低错填 |
| P0 | 扫码幂等 | 防止二维码识别多次触发重复事件 |
| P1 | 赋码批次 | 支撑真实生产计划 |
| P1 | 激活状态 | 保证贴码后才可流转 |
| P1 | audit/effective 双视图 | 保留纠错审计完整性 |
| P2 | 节点/组织模型 | 支撑权限和业务校验 |
| P2 | 箱码/托盘码 | 支撑批量流转 |

---

## 13. 设计原则总结

1. **批次是管理单位，单品码是追溯单位。**
2. **生成码不等于贴码生效，必须有激活/复核。**
3. **扫码不是填表，扫码应触发系统自动判断。**
4. **一物一码需要软件唯一性 + 现场流程控制。**
5. **常规操作要一键完成，复杂信息留给异常和审计。**
6. **单品追溯和批量操作不冲突，可通过箱码/托盘码聚合解决。**
7. **业务有效视图和审计完整视图要分开。**

---

## 14. 参考方向

- GS1 Digital Link：将识别键放入 Web URI，并连接到在线信息和服务。
- EPCIS / OpenTraceability：强调事件、业务步骤、状态、地点、来源、去向、纠错声明等事件模型。
- 本项目当前实现：`TraceCodeAssignmentService`、`TraceScanTransactionService`、`TraceStatus`、`TraceLifecycleLog`、`TraceSnapshot`。

相关链接：

- https://ref.gs1.org/standards/digital-link/
- https://ref.gs1.org/standards/digital-link/uri-syntax/
- https://github.com/ift-gftc/opentraceability
- https://raw.githubusercontent.com/ift-gftc/opentraceability/main/docs/epcis/epcis_schema.json

