# T16 基础 UI 组件层 PrimeVue 化设计

**日期**：2026-04-10  
**任务编号**：T16  
**任务名称**：重构基础 UI 组件层为 PrimeVue 风格

## 1. 目标

在 T14「PrimeVue 主标准 + Element Plus 过渡保留」和 T15「基于 PrimeVue 自建企业主题与统一设计令牌体系」的前提下，完成基础 UI 组件层的 PrimeVue 化重构，为后续 T17-T20 页面级迁移提供统一、稳定、可复用的基础能力。

本任务的目标是：

- 建立一层新的 PrimeVue 标准基础组件
- 将现有 `shared/components/ui/*` 收敛为兼容适配层
- 将基础交互服务从 Element Plus 切换到 PrimeVue 或项目自建 PrimeVue 风格实现
- 在不一次性推翻所有现有页面的前提下，完成基础 UI 层的底座切换
- 同时补上最小前端测试基础，降低基础层重构风险

## 2. 当前现状

当前前端基础 UI 层存在以下情况：

### 2.1 组件层状态

当前 `frontend/src/shared/components/ui/` 下已有：

- `BaseButton.vue`
- `BaseInput.vue`
- `BaseCard.vue`
- `ConfirmDialog.vue`
- `PromptDialog.vue`
- `Toast.vue`
- `LoadingSkeleton.vue`

其中：

- `BaseButton`、`BaseInput`、`BaseCard` 是项目自定义壳组件
- 样式主要由 Tailwind 类和项目内自定义风格驱动
- 尚未真正切换到 PrimeVue 底层实现

### 2.2 服务型交互状态

当前这些 composable 仍直接依赖 Element Plus：

- `useToast.js` → `ElMessage`
- `useConfirm.js` → `ElMessageBox.confirm`
- `usePrompt.js` → `ElMessageBox.prompt`

这意味着即使页面上已经引入 PrimeVue，基础交互能力的核心实现仍然绑定在 Element Plus 上。

### 2.3 调用面现状

- `BaseCard` 已被多个业务页和管理页广泛直接使用
- `BaseButton` / `BaseInput` 的直接调用面相对集中
- `useToast` / `useConfirm` / `usePrompt` 已被多个页面与 API 请求逻辑依赖
- `PromptDialog.vue` 与 `usePrompt.js` 当前存在“组件宿主”和“Element Plus prompt”双轨并存现象，职责不够统一

因此，T16 不能简单粗暴替换，而必须设计可控的过渡策略。

## 3. 方案对比与最终选择

### 方案 A：原文件就地 PrimeVue 化

做法：
- 直接改写 `BaseButton` / `BaseInput` / `BaseCard` 等旧组件
- 以最少兼容逻辑保留现有调用方式

优点：
- import 路径不变
- 文件结构变化最小

缺点：
- 新旧职责混杂
- 旧历史接口与新标准接口会长期缠绕
- 不利于后续清理兼容层

### 方案 B：新增 Prime 标准层，旧 Base 层做兼容转发（采用）

做法：
- 新增 `shared/components/prime/*` 作为 PrimeVue 标准基础组件层
- 旧 `shared/components/ui/*` 继续保留，但只负责兼容转发
- 页面先可继续使用 `Base*`，后续任务逐步切换到 `Prime*`

优点：
- 新旧边界清晰
- 后续迁移和清理路径明确
- 可以降低对现有页面的直接冲击
- 有利于 T20 最终收口

缺点：
- 会在过渡期形成双层结构
- 需要设计明确的兼容边界，避免两层都继续扩张

### 方案 C：先改服务型交互，再改视觉型基础组件

优点：
- 依赖链更稳
- 调试更容易

缺点：
- 用户可见收益慢
- 容易拉长任务周期
- 与当前希望“一次性完成基础层切换”的目标不完全一致

### 最终选择

采用 **方案 B：新增 Prime 标准层，旧 Base 层做兼容转发**。

并在同一任务中一次性完成全部基础组件的切换，但实现顺序可按风险高低分批推进。

## 4. 分层设计

T16 完成后，前端基础 UI 将形成四层结构：

### 4.1 Theme 层

由 T15 定义的统一主题与令牌体系提供底层视觉规则。

核心职责：
- 提供企业后台风的语义令牌
- 提供 PrimeVue 主题对象
- 为共享组件层提供唯一视觉来源

### 4.2 Prime 标准组件层

新增目录：

- `frontend/src/shared/components/prime/`

建议包含：

- `PrimeButton.vue`
- `PrimeInput.vue`
- `PrimeCard.vue`
- `PrimeLoadingSkeleton.vue`
- `PrimeConfirmDialog.vue`
- `PrimePromptDialog.vue`
- `PrimeToastHost.vue`

这一层是 T16 之后的**真正标准层**，其设计原则是：

- 底层完全以 PrimeVue 为主
- API 以 PrimeVue 风格和项目新标准为主
- 样式只消费 T15 的企业主题令牌
- 不为了旧历史用法继续堆积兼容逻辑

### 4.3 Base 兼容适配层

保留原目录：

- `frontend/src/shared/components/ui/`

但职责改为：

- 接收旧页面历史调用方式
- 将旧 props / events / slot 尽量桥接到 `Prime*` 组件
- 只保留必要兼容能力
- 不再作为长期标准层继续演进

这一层是**过渡层**，不是未来标准层。

### 4.4 页面与业务层

现有页面和业务组件在 T16 完成后仍可先继续使用：

- `BaseButton`
- `BaseInput`
- `BaseCard`
- `useToast`
- `useConfirm`
- `usePrompt`

但从 T16 完成后开始：

- 新增基础 UI 优先使用 `Prime*` 标准层
- 旧页面后续在 T17-T19 中逐步去掉对兼容层的依赖

## 5. 组件迁移边界

### 5.1 Button

#### 新标准层
新增 `PrimeButton.vue`：
- 底层使用 PrimeVue `Button`
- 统一支持：`variant`、`size`、`loading`、`disabled`、`block`
- 支持 icon / label / slot
- 风格完全对齐 T15 企业主题令牌

#### 兼容层
保留 `BaseButton.vue`：
- 继续兼容当前 `variant / size / block / loading / disabled / class`
- 内部转发到 `PrimeButton`
- 不再继续累积旧样式逻辑

### 5.2 Input

#### 新标准层
新增 `PrimeInput.vue`：
- 文本输入基于 PrimeVue `InputText`
- 密码输入基于 PrimeVue `Password`
- 支持：`modelValue`、`label`、`placeholder`、`type`、`error`、`disabled`
- 支持前置图标能力

#### 兼容层
保留 `BaseInput.vue`：
- 保持现有 `modelValue / label / placeholder / type / error / icon`
- 内部映射到 `PrimeInput`
- 只负责桥接旧调用方式

### 5.3 Card

#### 新标准层
新增 `PrimeCard.vue`：
- 底层使用 PrimeVue `Card`
- 支持：`title`、`subtitle`、`noPadding`、`variant`、`contentClass`
- 提供 header / default / footer slot

#### 兼容层
保留 `BaseCard.vue`：
- 保持当前 `class`、`noPadding`
- 内部转发到 `PrimeCard`
- 对当前页面直接堆叠 class 的用法保持尽量保守兼容

### 5.4 Loading Skeleton

#### 新标准层
新增 `PrimeLoadingSkeleton.vue`：
- 底层使用 PrimeVue `Skeleton`
- 继续支持：`card / table / chart / kpi / detail / list / default`
- 以 PrimeVue Skeleton 组合出项目所需模板

#### 兼容层
保留 `LoadingSkeleton.vue`：
- 保持 `type`、`rows`
- 内部转发到 `PrimeLoadingSkeleton`

### 5.5 Toast

#### 新标准层
新增：
- `PrimeToastHost.vue`
- PrimeVue `ToastService` 接入

#### 服务层
`useToast.js` 改为：
- 不再依赖 `ElMessage`
- 改由 PrimeVue ToastService 提供：
  - `success`
  - `error`
  - `warning`
  - `info`

#### 兼容层
`Toast.vue` 可保留为宿主壳组件，或仅保留极薄封装；真正消息提示能力统一转入 PrimeVue Toast 服务体系。

### 5.6 Confirm Dialog

#### 新标准层
新增 `PrimeConfirmDialog.vue`：
- 底层使用 PrimeVue `ConfirmDialog`
- 结合 PrimeVue `ConfirmationService`

#### 服务层
`useConfirm.js` 改为：
- 不再依赖 `ElMessageBox.confirm`
- 返回 Promise / 布尔值风格，保持当前调用习惯可延续

#### 兼容层
保留 `ConfirmDialog.vue`：
- 若当前仍有组件式调用，继续作为薄适配层
- 长期标准入口以 `useConfirm + PrimeConfirmDialog` 为主

### 5.7 Prompt Dialog

#### 新标准层
新增 `PrimePromptDialog.vue`：
- 底层使用 PrimeVue `Dialog + InputText / Password`
- 由项目自建 prompt 宿主能力
- 支持输入值、校验、确认、取消、占位符、默认值

#### 服务层
`usePrompt.js` 改为：
- 不再依赖 `ElMessageBox.prompt`
- 改为驱动项目自建 Prompt 宿主
- 继续返回 Promise，确保调用习惯保持稳定

#### 兼容层
保留 `PromptDialog.vue`：
- 若作为宿主组件存在，则内部完全转到 PrimePrompt 实现
- 去掉与 Element Plus prompt 双轨并存的职责混乱

## 6. 服务型基础能力收口规则

T16 除了改造视觉型基础组件，还必须同步完成基础交互服务的切换。

即：

- `useToast`
- `useConfirm`
- `usePrompt`

必须全部脱离 Element Plus。

原因：

- 否则基础 UI 层即使换了 PrimeVue，交互基础能力仍会保留 Element Plus 深层依赖
- 后续 T17-T20 页面迁移会持续背负双栈负担
- 这与 T14/T15 的统一视觉和统一标准目标相冲突

因此，T16 的完成标准中必须包含：

> 基础 UI 层与基础交互层都不再依赖 Element Plus 作为底层实现。

## 7. 实现顺序建议

虽然本任务要求“一起做”，但实现时建议按风险高低分两批推进。

### 第一批（低风险高收益）

- `PrimeButton.vue`
- `PrimeInput.vue`
- `PrimeLoadingSkeleton.vue`
- `PrimeToastHost.vue`
- `useToast.js`
- 对应兼容层改写

### 第二批（兼容压力更高）

- `PrimeCard.vue`
- `PrimeConfirmDialog.vue`
- `PrimePromptDialog.vue`
- `useConfirm.js`
- `usePrompt.js`
- 对应兼容层改写

强调：

- 这是实现顺序建议，不是拆成两个独立任务
- T16 仍以“一次性交付全部基础 UI 层切换”为目标

## 8. 测试与验证策略

### 8.1 现状判断

当前前端项目没有项目级测试基础，只有构建脚本和人工回归手段。

这意味着 T16 若不补最小测试基础，将很难保证基础层重构稳定。

### 8.2 本任务接受补最小前端测试基础

本任务明确接受并纳入：

- `Vitest`
- `@vue/test-utils`
- 必要的 `jsdom` 测试环境

这不是扩 scope，而是保障 T16 基础层重构质量的必要支撑。

### 8.3 自动化测试范围

#### Prime 标准层测试
验证：
- 基本渲染
- 关键 props 生效
- 事件透传
- slot / label / icon 等核心能力工作正常

#### Base 兼容层测试
验证：
- 旧 props 仍能工作
- 内部确实转发到 `Prime*`
- 当前页面的主要调用方式不会因为兼容失败而整体失效

#### composable / 宿主能力测试
验证：
- `useToast / useConfirm / usePrompt` 不再依赖 Element Plus
- Promise 返回语义稳定
- confirm / prompt 的确认和取消分支行为正确

### 8.4 构建验证

至少要求：

- `cd frontend && npm run build`

### 8.5 人工回归页面

至少应回归：

- `UserList.vue`
- `PartList.vue`
- `TraceList.vue`
- `Dashboard.vue`

重点检查：

- 按钮点击/禁用/loading
- 输入框和密码框输入与报错
- 卡片结构是否塌陷
- Toast 是否可正常弹出
- Confirm / Prompt 是否可正常确认与取消
- Skeleton 是否能在关键页面正常显示

## 9. 文件职责建议

### 主题层
- `frontend/src/shared/theme/tokens.js`
- `frontend/src/shared/theme/primevue-theme.js`

### Prime 标准组件层
- `frontend/src/shared/components/prime/*`

### 兼容适配层
- `frontend/src/shared/components/ui/*`

### 服务层
- `frontend/src/shared/composables/useToast.js`
- `frontend/src/shared/composables/useConfirm.js`
- `frontend/src/shared/composables/usePrompt.js`

### 测试层
- `frontend/src/shared/components/prime/__tests__/*`
- `frontend/src/shared/components/ui/__tests__/*`
- `frontend/src/shared/composables/__tests__/*`

后续实现时应保持职责清晰，不要把主题、标准组件、兼容桥接和服务逻辑再次混写在一起。

## 10. 完成标准映射

任务表要求：

> 基础 UI 层统一为 PrimeVue 风格或以 PrimeVue 为底层实现

本方案对应的完成标准为：

- 已建立 `Prime*` 标准基础组件层
- 旧 `Base*` 组件已转为兼容适配层
- 基础 UI 视觉与交互底层已统一切换到 PrimeVue / Prime 风格实现
- `useToast / useConfirm / usePrompt` 已脱离 Element Plus
- 已具备最小可运行的前端测试基础
- 构建通过，关键页面人工回归通过

## 11. 本任务不包含的内容

本任务明确不包含：

- 不在 T16 中直接全面重写业务页面
- 不在 T16 中完成布局层 PrimeVue 化（属于 T17）
- 不在 T16 中完成管理页和业务页视觉统一（属于 T18 / T19）
- 不处理与 UI 无关的数据接口、权限、状态管理问题
- 不在本任务中直接删除所有旧兼容层

兼容层会在 T17-T20 的迁移过程中逐步收缩，最终再评估是否移除。
