# 溯源码核心模型映射与迁移说明（B01）

> 任务来源：`溯源码核心业务重设计执行任务表_20260505.md` / B01  
> 依据文档：`docs/trace-core-business-redesign.md`  
> 完成日期：2026-05-05  
> 目标：明确现有核心模型如何平滑演进到“批次赋码、一物一码、任务驱动扫码流转”的新业务模型。

---

## 1. 当前实现基线

当前后端核心链路已经具备“赋码生成、生命周期日志、状态快照、Hash 链、RSA 签名、纠错审计”的基础能力，核心文件如下：

| 类型 | 当前文件 | 当前职责 |
|---|---|---|
| 状态快照 | `backend/src/main/java/com/example/trace/entity/TraceSnapshot.java` | 以 `trace_code` 为主键保存当前状态、当前节点、链尾 Hash、最新日志 ID、乐观锁版本 |
| 生命周期日志 | `backend/src/main/java/com/example/trace/entity/TraceLifecycleLog.java` | 记录每次赋码/扫码事件，包含上下游节点、地区、备注、Hash、签名、纠错关系 |
| 状态枚举 | `backend/src/main/java/com/example/trace/enums/TraceStatus.java` | 当前支持 `INIT / IN_STOCK / IN_TRANSIT / TRANSFERRED / EXCEPTION` |
| 事件枚举 | `backend/src/main/java/com/example/trace/enums/ActionType.java` | 当前支持 `INIT / INBOUND / OUTBOUND / TRANSFER / EXCEPTION / CORRECTION` |
| 生产赋码 | `TraceCodeAssignmentService` | 一次生成 N 个 `traceCode`，写入 `INIT` 日志和 snapshot |
| 扫码写入 | `TraceScanTransactionService` | 追加日志，按 `TraceStatus.deriveFromAction(...)` 更新 snapshot |
| 防篡改 | `HashUtil` / `SignatureUtil` / `TraceLogFactory` / `TraceChainVerifyService` | 对日志字段计算 Hash 与签名，并验链 |
| 当前 SQL | `backend/sql/init_schema.sql` | 定义 `trace_snapshot`、`trace_lifecycle_log` 及权限等基础表 |

当前主要差距：

1. `TraceStatus.deriveFromAction(...)` 是简单映射，不校验“当前状态 + 事件”的合法性。
2. 当前 `INIT` 同时承载“已生成码 / 已赋码 / 可后续流转”的混合语义，尚未区分 `GENERATED / PRINTED / ACTIVATED`。
3. 当前 `fromNode / toNode / currentNode / currentOwner` 均为自由文本，尚未接入结构化 `trace_node`。
4. 当前扫码接口没有幂等键，重复扫码可能重复写日志。
5. 当前 Hash/签名覆盖了 traceCode、action、节点、地区、时间、纠错、备注等字段，但未覆盖 `operator`，也未预留 `taskId / deviceId / payloadJson` 等扩展字段。
6. 当前详情接口实际返回 `selectEffectiveHistory(...)`，已有 `selectFullChain(...)`，但 API 尚未显式区分 `effective / audit` 视图。

---

## 2. 保留、新增、后续弱化字段

### 2.1 第一阶段继续保留的字段

第一阶段（B02-B07）不推翻当前表结构，以下字段继续作为兼容主干：

| 字段 | 所属模型 | 保留原因 |
|---|---|---|
| `traceCode` / `trace_code` | snapshot、log | 当前一物一码身份主键，后续仍是单品追溯核心标识 |
| `spuId` / `spu_id` | snapshot、log | 当前配件规格关联，批次/单品表仍需引用 |
| `currentStatus` / `current_status` | snapshot | 第一阶段状态机仍基于该字段落地 |
| `currentNode` / `current_node` | snapshot | B04 自动推导 `fromNode` 的来源 |
| `currentOwner` / `current_owner` | snapshot | 暂作为责任方展示字段 |
| `lastLogId` / `last_log_id` | snapshot | 链尾日志指针，详情和验链仍依赖 |
| `lastHash` / `last_hash` | snapshot | 下一条日志的 `prevHash` 来源 |
| `version` | snapshot | 扫码并发控制，B05 幂等前仍保留乐观锁重试 |
| `actionType` / `action_type` | log | 生命周期事实事件主字段 |
| `fromNode` / `toNode` | log | 第一阶段仍记录文本节点；B04 先自动填充，B14 后再升级为结构化节点 ID |
| `province` / `city` | snapshot、log | 地图和地域展示继续使用 |
| `correctionOf` / `correction_of` | log | B07 audit/effective 视图区分的基础 |
| `operator` | log | B06 应优先纳入 Hash/签名保护 |
| `signatureKeyId` / `signatureKeyVersion` | log | 已支持签名密钥轮换，继续保留 |

### 2.2 后续新增字段/表

| 新模型/字段 | 推荐任务 | 说明 |
|---|---|---|
| `TraceTransitionPolicy` | B02 | 代码级策略服务，不需要新增表 |
| `available-actions` 响应 DTO | B03 | 用于扫码后返回推荐动作和不可操作原因 |
| `idempotency_key` 或 `trace_scan_idempotency` 表 | B05 | 推荐独立表或日志唯一键，保证重复扫码不重复落日志 |
| `operator_user_id` / `operator_org_id` | B06/B15/B29 | 比当前 `operator` 文本更适合审计和权限追踪 |
| `from_node_id` / `to_node_id` | B14/B16 | 结构化节点 ID，替代自由文本作为业务判断依据 |
| `task_id` | B16-B20 | 关联流转任务，支撑任务内连续扫码 |
| `device_id` | B12/B19 | 记录扫码枪/PDA/浏览器设备来源 |
| `payload_json` | B06/B30 | 保存业务扩展载荷，进入 Hash/签名保护 |
| `trace_assign_batch` | B08 | 赋码批次表，承载生产计划/工单/数量对账 |
| `trace_code` | B09 | 单品码表，承载码状态、批次、打印次数、激活信息 |
| `trace_node` | B14 | 工厂、仓库、物流、客户等结构化节点 |
| `trace_flow_task` | B16 | 出库、运输、接收等任务流模型 |
| `trace_aggregation` | B25 | 箱码/托盘码与单品码的聚合关系 |

### 2.3 后续弱化或废弃的字段语义

| 当前字段/枚举 | 后续处理 |
|---|---|
| `TraceStatus.INIT` | 保留为历史兼容启动状态；新增码状态后，由 `GENERATED / PRINTED / ACTIVATED` 承担真实贴码流程 |
| `TraceStatus.TRANSFERRED` | 后续建议映射到 `RECEIVED`，表示目标节点已接收；运输过程由 `IN_TRANSIT` 承担 |
| `ActionType.TRANSFER` | 后续拆成 `TRANSFER_START / TRANSFER_ARRIVE / RECEIVE`，当前先保留为历史物流流转事件 |
| 自由文本 `fromNode / toNode / currentNode / currentOwner` | B14 后改为结构化节点 ID 为准，文本字段作为展示冗余或历史兼容 |
| `operator` 文本 | 继续展示；审计判断逐步转向 `operator_user_id` 和权限上下文 |

---

## 3. 新旧状态映射

### 3.1 当前状态到目标状态

| 当前状态 | 当前含义 | 目标模型映射 | 迁移建议 |
|---|---|---|---|
| `INIT` | 已初始化/已生产赋码 | 历史兼容态；新码应从 `GENERATED` 开始 | B02-B07 不批量迁移；B09 引入 `trace_code` 后，新生成码使用 `GENERATED`，历史 `INIT` 码按“已可流转的旧码”处理 |
| `IN_STOCK` | 在库 | `IN_STOCK` | 保持不变 |
| `IN_TRANSIT` | 运输中 | `IN_TRANSIT` | 保持不变 |
| `TRANSFERRED` | 已交接 | `RECEIVED` / 已到达目标节点 | 第一阶段保留 `TRANSFERRED`；B18 接收闭环时再新增或替换为 `RECEIVED` |
| `EXCEPTION` | 异常 | `EXCEPTION_HELD` | 第一阶段保留 `EXCEPTION`；B30 细化为异常冻结/解除 |

### 3.2 目标新增状态与落地任务

| 目标状态 | 含义 | 落地任务 |
|---|---|---|
| `GENERATED` | 单品码已生成，未打印 | B09 |
| `PRINTED` | 标签已打印 | B09/B11 |
| `ACTIVATED` | 标签已贴到实物并扫码复核 | B09/B12 |
| `OUTBOUND_PENDING` | 已进入出库任务但未出库 | B16/B17 |
| `RECEIVED` | 目标节点已接收 | B18 |
| `INSTALLED` | 已安装/投入使用 | 后续可选 |
| `EXCEPTION_HELD` | 异常冻结 | B30 |
| `VOIDED` | 未激活码作废 | B11 |
| `SCRAPPED` | 报废 | B30 或后续 |

---

## 4. 新旧事件映射

| 当前事件 | 当前写入位置 | 目标事件映射 | 迁移建议 |
|---|---|---|---|
| `INIT` | `TraceCodeAssignmentService` 创建初始日志 | `ASSIGN_CODE` | 第一阶段继续写 `INIT`；B08/B09 后新赋码链路改为 `ASSIGN_CODE` 并写入 `trace_code` |
| `INBOUND` | 扫码入库 | `INBOUND` | 保持不变；B04 起 `fromNode` 默认 snapshot 当前节点，`toNode` 由任务或用户节点推导 |
| `OUTBOUND` | 扫码出库 | `OUTBOUND` / `TRANSFER_START` | 第一阶段保留 `OUTBOUND`；B17 任务出库后进入 `IN_TRANSIT` |
| `TRANSFER` | 物流流转/交接 | `TRANSFER_ARRIVE` / `RECEIVE` | 当前语义偏宽；B18 拆成到达和接收确认 |
| `EXCEPTION` | 异常记录 | `EXCEPTION_OPEN` | 第一阶段保留；B30 细化开启/关闭异常 |
| `CORRECTION` | 红冲蓝补 | `CORRECTION` | 保持；B07 audit 视图必须展示完整链，effective 视图隐藏被覆盖记录 |
| 无 | 无 | `PRINT_CODE / REPRINT_CODE / VOID_CODE / ACTIVATE_CODE` | B11/B12 新增 |
| 无 | 无 | `PACK / UNPACK / PALLETIZE / UNPALLETIZE` | B26 新增 |

---

## 5. 第一阶段数据库迁移结论

第一阶段任务是 B02-B07，建议拆成“无迁移可先做”和“需要加法迁移”的两类。

### 5.1 B02-B04、B07 可不做数据库迁移

| 任务 | 是否需要迁移 | 说明 |
|---|---|---|
| B02 状态机策略 | 否 | 基于当前 `TraceStatus / ActionType` 增加合法流转校验即可 |
| B03 可执行动作判断 | 否 | 基于 snapshot、权限和暂存文本节点判断；节点/任务模型未接入前先返回降级动作 |
| B04 后端自动推导节点 | 否 | `fromNode` 可直接来自 `TraceSnapshot.currentNode`；`toNode` 仍来自请求或默认规则 |
| B07 effective/audit 视图 | 否 | 当前已有 `selectEffectiveHistory(...)` 和 `selectFullChain(...)`，主要是接口参数、权限和响应语义改造 |

### 5.2 B05-B06 需要谨慎做加法迁移

| 任务 | 推荐迁移 | 原因 |
|---|---|---|
| B05 扫码幂等 | 推荐新增 `trace_scan_idempotency` 表，或在 `trace_lifecycle_log` 增加 `idempotency_key` 并建立唯一约束 | 幂等结果需要持久化，否则服务重启或并发请求仍可能重复写入 |
| B06 Hash/签名扩展 | 第一小步可不迁移：先把现有 `operator` 纳入 Hash/签名；后续扩展 `operator_user_id / task_id / device_id / payload_json` 时再加字段 | 当前日志已有 `operator` 字段但未进入 Hash/签名，属于可立即修复的审计缺口 |

结论：

> B02-B04、B07 可先纯代码落地；B05 必须配套幂等持久化；B06 应先把现有 `operator` 纳入保护，再按 B14-B20 的节点/任务字段逐步扩展。

---

## 6. 对后续任务的实施边界

### B02 状态机策略边界

建议先在当前枚举上建立最小合法流转：

| 当前状态 | 允许事件 | 新状态 |
|---|---|---|
| `INIT` | `INBOUND` | `IN_STOCK` |
| `INIT` | `EXCEPTION` | `EXCEPTION` |
| `IN_STOCK` | `OUTBOUND` | `IN_TRANSIT` |
| `IN_STOCK` | `EXCEPTION` | `EXCEPTION` |
| `IN_TRANSIT` | `TRANSFER` | `TRANSFERRED` |
| `IN_TRANSIT` | `INBOUND` | `IN_STOCK` |
| `TRANSFERRED` | `INBOUND` | `IN_STOCK` |
| 任意状态 | `CORRECTION` 且 `correctionOf` 合法 | 状态不变 |

是否允许 `INIT -> OUTBOUND` 需要谨慎：从真实业务看应先入库/激活再出库；若当前演示数据依赖该路径，可在 B02 测试中明确兼容或拒绝。

### B03 可执行动作边界

节点/任务模型未落地前，推荐先返回“降级动作”：

- `INIT`：生产/仓库权限用户可 `INBOUND`。
- `IN_STOCK`：仓库权限用户可 `OUTBOUND`。
- `IN_TRANSIT`：物流权限用户可 `TRANSFER`，仓库权限用户可 `INBOUND`。
- `EXCEPTION`：仅超级扫码或审计/管理员可 `CORRECTION`。

### B04 自动推导节点边界

第一阶段默认规则：

1. 如果请求未传 `fromNode`，使用 `snapshot.currentNode`。
2. 如果请求传了 `fromNode` 且与 `snapshot.currentNode` 不一致，默认拒绝，除非后续增加“异常/纠错”场景白名单。
3. 如果请求未传 `province/city`，沿用 snapshot 当前地区。
4. `toNode` 在 B16 任务模型前仍允许请求传入，但 B04 应集中校验和规范化。

### B05 幂等边界

推荐独立表方案，避免直接把幂等语义混入事实日志：

```sql
trace_scan_idempotency
- id
- trace_code
- action_type
- idempotency_key
- request_hash
- log_id
- status
- create_time
- update_time

UNIQUE(trace_code, action_type, idempotency_key)
```

重复请求命中唯一键时，应返回第一次处理结果或明确“已处理”的业务响应。

### B06 Hash/签名边界

第一步先把当前已有 `operator` 纳入：

- `HashUtil.calculateHash(...)`
- `SignatureUtil.buildSignatureData(...)`
- `TraceLogFactory.createLog(...)`
- `TraceChainVerifyService.verify(...)`

注意：这会影响新旧数据兼容。推荐在 B06 中明确兼容策略：

1. 新日志使用新版字段集；
2. 老日志按旧字段集验链；
3. 可通过 `signature_key_version` 或新增 `hash_schema_version` 区分；若不新增字段，则需要在验链时双算法兼容。

---

## 7. 推荐实施顺序

1. **B02**：新增 `TraceTransitionPolicy`，先只基于当前状态/事件做严格校验。
2. **B03**：新增 available-actions 判断服务和 DTO，先做无节点/无任务的降级版本。
3. **B04**：扫码写入默认推导 `fromNode`、地区，并拒绝伪造来源节点。
4. **B05**：加幂等表或幂等字段，补并发/重复扫码测试。
5. **B06**：扩展 Hash/签名字段，先覆盖 `operator` 并处理老数据兼容。
6. **B07**：详情接口增加 `view=effective|audit`，使用现有 mapper 能力切换历史视图。

---

## 8. B01 结论

- 当前核心模型可平滑演进，不需要先推翻 `trace_snapshot` 和 `trace_lifecycle_log`。
- B02-B04、B07 可以先以代码改造推进，数据库保持不变。
- B05 开始需要幂等持久化迁移。
- B06 可以先利用现有 `operator` 字段提升审计保护，再为后续节点/任务字段做加法扩展。
- 新业务中的批次、单品码状态、结构化节点、流转任务、聚合码，应在 B08 以后分表落地，避免第一阶段一次性扩大变更面。
