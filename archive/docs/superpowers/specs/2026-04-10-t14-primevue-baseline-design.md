# T14 PrimeVue UI 重构基线方案设计

**日期**：2026-04-10  
**任务编号**：T14  
**任务名称**：制定 PrimeVue UI 重构基线方案

## 1. 目标

在不直接改动现有业务页面实现的前提下，为后续 T15-T20 提供统一的 PrimeVue UI 重构基线，明确：

- PrimeVue 在项目中的定位
- Element Plus 的过渡期保留边界
- 共享 UI 组件层的收口策略
- 后续页面与组件的迁移顺序

本任务只产出基线方案文档与任务表更新，不进行实际组件替换。

## 2. 当前前端现状

基于当前代码结构，项目已具备以下条件：

- `primevue` 与 `@primeuix/themes` 已安装
- `frontend/src/main.js` 已注册 `PrimeVue`，并启用 `Aura` 主题
- `element-plus` 仍在全局注册，且现有页面和布局层仍大量依赖 Element Plus
- 共享 UI 层已有一套自定义组件封装，如：
  - `BaseButton.vue`
  - `BaseInput.vue`
  - `BaseCard.vue`
  - `ConfirmDialog.vue`
  - `Toast.vue`
- 页面层与布局层仍存在“Element Plus + 自定义组件 + Tailwind 风格类”混用的情况

这意味着项目已经具备 PrimeVue 引入条件，但尚未完成“组件标准、视觉标准、封装标准”的统一。

## 3. 方案结论

本项目采用：**PrimeVue 主标准 + Element Plus 过渡保留** 的双轨过渡方案。

### 3.1 为什么不采用激进替换

如果直接将全部页面和组件一次性切换到 PrimeVue，会带来以下问题：

- 当前页面中 Element Plus 使用面较广，短期内改动面过大
- 容易把 T14 从“定基线”扩散成“大规模重构”
- 后续 T16-T20 将失去渐进迁移的节奏控制

### 3.2 为什么不采用仅视觉统一方案

如果只约定设计风格、不约定组件边界，会导致后续继续混搭：

- 新代码可能继续直接写 Element Plus
- 共享组件层无法真正成为 UI 契约入口
- T20 “清理混用”会变得不可执行

### 3.3 最终选择

因此采用双轨方案：

- **PrimeVue 是未来主标准**
- **Element Plus 在过渡期内允许受控保留**
- **共享 UI 组件层是唯一推荐收口层**
- **页面层逐步迁移，而不是立即推翻现有实现**

## 4. UI 分层基线

后续 UI 重构统一按三层治理：

### 4.1 基础 UI 组件层

职责：提供按钮、输入框、卡片、弹窗、消息、确认框、分页、表格等基础交互能力。  
后续标准：**优先以 PrimeVue 为底层实现**。

这一层包括但不限于：

- `frontend/src/shared/components/ui/BaseButton.vue`
- `frontend/src/shared/components/ui/BaseInput.vue`
- `frontend/src/shared/components/ui/BaseCard.vue`
- `frontend/src/shared/components/ui/ConfirmDialog.vue`
- `frontend/src/shared/components/ui/Toast.vue`
- `frontend/src/shared/components/ui/PromptDialog.vue`
- `frontend/src/shared/components/ui/LoadingSkeleton.vue`

原则：

- 页面层不直接依赖 PrimeVue 的具体组件 API 作为主调用方式
- 优先通过共享 UI 组件层暴露统一接口
- 若短期无法封装完全，可允许少量页面级直连 PrimeVue，但应作为过渡而非常态

### 4.2 业务组合组件层

职责：封装带业务语义的交互组合，如扫码流程弹窗、创建溯源码弹窗、表单组合组件等。

这一层的要求是：

- 不直接绑定 Element Plus 作为长期依赖
- 优先消费共享 UI 组件层
- 迁移时保持业务接口稳定，减少页面级改动面

代表组件包括：

- `ScanFlowDialog.vue`
- `CreateTraceDialog.vue`
- `InboundForm.vue`
- `OutboundForm.vue`
- `TransferForm.vue`

### 4.3 页面与布局层

职责：负责页面编排、数据请求、权限控制、场景组织。  
要求：**页面不应成为 UI 库直接耦合的主战场**。

意味着后续迁移中：

- 页面层优先减少对具体 UI 库的直接使用
- 视觉和交互差异优先下沉到共享组件层与业务组合组件层
- 布局层允许作为过渡期保留 Element Plus，但需要纳入迁移清单

## 5. 保留 / 替换 / 适配策略

### 5.1 直接替换为 PrimeVue 底层实现的范围

这部分属于 T16 的重点：

- 按钮
- 输入框
- 选择器
- 卡片
- Dialog / Confirm / Toast
- 表单基础控件
- Skeleton / Empty / Tag / Badge 等常用状态组件

策略：**优先替换底层，不先破坏现有外部组件接口**。

即：

- 先保持 `BaseButton`、`BaseInput` 等项目内调用方式尽量稳定
- 再把其内部实现逐步切到 PrimeVue

### 5.2 过渡期保留 Element Plus 的范围

过渡期允许保留以下区域中的 Element Plus：

- `MainLayout.vue` 中的侧栏、Header、Drawer、Menu 等布局容器
- 后台管理页中的大型表格、分页、筛选栏、弹窗
- 已经稳定运行、短期替换收益不高的页面级容器组件

但这些保留必须满足两个条件：

1. 不新增扩散式依赖
2. 必须在 T17-T20 中有明确迁移归宿

### 5.3 仅做风格适配、不立即替换的范围

对于短期内不值得立刻替换、但会影响整体观感统一的组件，可采用“先适配，后替换”的策略：

- 调整间距、圆角、颜色、阴影、禁用态、表单态
- 让其视觉尽量贴近 PrimeVue 主题基线
- 在页面层先降低混搭感，再择机替换底层实现

## 6. 对后续任务的约束关系

### T15：建立 PrimeVue 统一主题与设计令牌映射

T15 应负责：

- 建立色彩、字号、间距、圆角、阴影、边框、状态色映射
- 明确 PrimeVue Theme 与项目现有 Tailwind 风格类之间的令牌关系
- 为后续页面迁移提供一致视觉基础

### T16：重构基础 UI 组件层为 PrimeVue 风格

T16 应负责：

- 优先改造共享基础组件层
- 尽量保持项目内部现有调用方式稳定
- 建立“页面调用共享组件，而不是直接调用 UI 库”的基线

### T17-T19：页面级迁移

页面迁移顺序建议为：

1. 登录页与主布局
2. 后台管理页
3. 溯源业务页

原因：

- 登录页和主布局决定系统整体第一视觉
- 管理页的表格/表单模式最适合验证共享组件层可用性
- 溯源业务页交互更复杂，适合在基础设施稳定后推进

### T20：清理混用

T20 不应从零开始讨论“要不要保留 Element Plus”，而应基于本基线完成最终清理：

- 删除已完成迁移的旧依赖
- 收敛剩余不合理混用
- 明确保留项与最终弃用项

## 7. 新增开发约束

从 T14 完成后开始，建议默认遵守以下规则：

1. 新增基础交互组件优先使用 PrimeVue 方案
2. 页面层避免新增 Element Plus 直接依赖
3. 若必须使用 Element Plus，应说明其属于过渡性使用
4. 公共交互优先沉淀到 `shared/components/ui` 层
5. 业务组合组件优先依赖共享 UI 层，而不是直接堆叠第三方库 API
6. 后续任务中的视觉统一，应以 PrimeVue 主题基线为准，而不是继续叠加零散样式补丁

## 8. 当前文件映射与迁移优先级建议

### 第一优先级（基础 UI 层）

- `frontend/src/shared/components/ui/BaseButton.vue`
- `frontend/src/shared/components/ui/BaseInput.vue`
- `frontend/src/shared/components/ui/BaseCard.vue`
- `frontend/src/shared/components/ui/ConfirmDialog.vue`
- `frontend/src/shared/components/ui/Toast.vue`
- `frontend/src/shared/components/ui/PromptDialog.vue`
- `frontend/src/shared/components/ui/LoadingSkeleton.vue`

### 第二优先级（布局与入口）

- `frontend/src/shared/components/layout/MainLayout.vue`
- `frontend/src/shared/components/Login.vue`

### 第三优先级（后台管理页）

- `frontend/src/features/user/views/UserList.vue`
- `frontend/src/features/user/views/RoleList.vue`
- `frontend/src/features/part/views/PartList.vue`

### 第四优先级（业务页）

- `frontend/src/features/trace/views/TraceList.vue`
- `frontend/src/features/trace/views/TraceDetail.vue`
- `frontend/src/features/trace/views/ScanHub.vue`
- `frontend/src/features/trace/components/ScanFlowDialog.vue`

## 9. T14 完成标准映射

任务表要求：

> 明确哪些组件保留、哪些替换、哪些只做风格适配

本方案已明确：

- **保留**：布局层和部分稳定页面级 Element Plus 组件可过渡期保留
- **替换**：共享基础 UI 组件层后续应优先切换到 PrimeVue 底层实现
- **适配**：短期不替换但影响观感统一的组件，先做视觉适配再择机迁移

因此，T14 交付边界清晰，且能直接支撑 T15-T20。

## 10. 本任务不包含的内容

为避免范围膨胀，本任务明确不包含：

- 不修改任何 Vue 页面组件实现
- 不替换任何现有 Element Plus 调用
- 不新增 PrimeVue 组件封装代码
- 不修改主题令牌落地代码
- 不处理页面拆分类任务（如 T09 / T10 / T11 / T13）

这些内容分别在 T15-T20 中继续推进。