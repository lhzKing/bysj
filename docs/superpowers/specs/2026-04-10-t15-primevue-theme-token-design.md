# T15 PrimeVue 企业主题与设计令牌映射设计

**日期**：2026-04-10  
**任务编号**：T15  
**任务名称**：建立 PrimeVue 统一主题与设计令牌映射

## 1. 目标

在 T14 已确定的 PrimeVue 主标准基线上，为项目建立一套**基于 PrimeVue 自建企业主题**的统一设计令牌体系，用于约束后续 T16-T20 的基础组件改造与页面迁移。

本任务关注以下内容：

- 定义企业后台风的视觉目标
- 建立从基础色板到语义令牌的完整层次
- 明确 PrimeVue 主题对象与项目令牌之间的映射关系
- 规定 Tailwind 与共享 UI 组件如何消费这套令牌

本任务不直接实现组件替换，也不改现有页面代码。

## 2. 设计定位

### 2.1 视觉方向

主题风格统一收敛为：**企业后台风 / 中性稳重 / 标准可维护**。

核心特征：

- 低饱和度
- 中性色主导
- 品牌色仅作为强调，而非大面积装饰
- 对表格、表单、状态、层级的可读性优先
- 视觉噪音低，减少“展示型页面”的装饰倾向

### 2.2 与现有风格的关系

当前前端存在较明显的 Tailwind 低饱和极简风痕迹，但色板更偏“产品展示感”。T15 后，视觉基线需要从“轻量极简”切向“企业后台稳重”。

这意味着：

- 保留现有简洁、留白充足的优点
- 弱化过于轻飘的视觉表达
- 引入更稳定的中性色、边框、层级和控件状态
- 为表格、筛选栏、弹窗、表单提供更标准的企业后台视觉基础

## 3. 方案结论

本任务采用：**基于 PrimeVue 自建企业主题**。

### 3.1 为什么不用现成 Aura 微调

虽然项目当前已注册 `Aura`，但如果继续在其基础上做轻量覆盖，容易出现以下问题：

- 主题来源不够单一，后续难判断“默认值”和“项目值”的责任边界
- 视觉语言仍残留 Aura 的默认气质
- T16 以后共享组件层会面临“项目设计令牌”和“Aura 默认语义”混杂的问题

### 3.2 自建主题的收益

基于 PrimeVue 自建主题的收益在于：

- 主题来源唯一，后续维护边界更清晰
- 语义令牌可完全围绕项目业务场景定义
- Tailwind 与共享 UI 可以围绕同一套语义命名收敛
- 为 T20 清理混用提供更坚实的视觉标准依据

因此，T15 不再以 Aura 为承接基底，而是明确采用**项目级自定义 PrimeVue 企业主题**。

## 4. 令牌分层设计

统一将令牌分为四层。

### 4.1 基础色板层（Primitive Tokens）

这一层只定义原始颜色，不直接给页面使用。

推荐方向：

- 主品牌蓝：偏深、偏稳重的蓝色系
- 中性灰：Slate / Zinc 风格的中性灰体系
- 成功色：克制的绿色
- 警告色：清晰但不过分刺眼的琥珀色
- 危险色：标准企业后台红色
- 信息色：偏冷静的蓝青色

原则：

- Primitive token 只描述“色值”，不描述用途
- 不允许在页面层直接引用原始色值
- 后续使用必须经由语义层完成

### 4.2 语义令牌层（Semantic Tokens）

语义层是 T15 的核心，是后续所有组件和页面唯一应该依赖的颜色语言。

建议至少包含以下分组：

#### 品牌语义
- `primary`
- `primary-hover`
- `primary-active`
- `primary-contrast`

#### 背景语义
- `bg-app`
- `bg-page`
- `bg-surface`
- `bg-elevated`
- `bg-overlay`

#### 文本语义
- `text-primary`
- `text-secondary`
- `text-muted`
- `text-inverse`
- `text-link`

#### 边框语义
- `border-subtle`
- `border-default`
- `border-strong`
- `border-focus`

#### 状态语义
- `success`
- `warning`
- `danger`
- `info`

以及对应：
- `success-bg`
- `warning-bg`
- `danger-bg`
- `info-bg`
- `success-text`
- `warning-text`
- `danger-text`
- `info-text`

#### 交互语义
- `focus-ring`
- `disabled-bg`
- `disabled-border`
- `disabled-text`
- `hover-bg`
- `active-bg`

### 4.3 尺寸与形态层（Shape / Size Tokens）

除颜色外，还应统一以下令牌：

#### 圆角
- `radius-sm`
- `radius-md`
- `radius-lg`
- `radius-xl`

企业后台建议：
- 默认以 `md` / `lg` 为主
- 不使用过度圆润的胶囊感设计

#### 阴影
- `shadow-sm`
- `shadow-md`
- `shadow-lg`

企业后台建议：
- 阴影弱化，优先依赖边框和背景层级区分结构
- 只在卡片浮层、弹窗、下拉层使用更明显阴影

#### 控件高度
- `control-sm`
- `control-md`
- `control-lg`

建议：
- 输入框、按钮、选择器、分页控件高度统一
- 减少不同页面不同高度造成的视觉漂移

#### 间距
- `space-1` 到 `space-8` 等统一步进

原则：
- 页面间距与组件内部间距都应围绕统一尺度系统展开

### 4.4 组件语义层（Component Tokens）

在通用语义令牌之上，可以按组件类型再定义一层映射，以便 T16 直接落地。

例如：

- `button-primary-bg`
- `button-primary-text`
- `button-secondary-border`
- `input-bg`
- `input-border`
- `input-focus-ring`
- `card-bg`
- `card-border`
- `dialog-header-bg`
- `table-header-bg`
- `table-row-hover-bg`

这一层不应直接定义新风格，而应基于语义令牌组合得出。

## 5. PrimeVue 主题映射策略

### 5.1 单一主题入口

后续落地时，应以一个项目内自定义主题入口作为唯一来源，例如：

- `frontend/src/shared/theme/primevue-theme.js`
- `frontend/src/shared/theme/tokens.js`

要求：

- `main.js` 只消费项目自定义主题
- 不再直接使用第三方默认主题 preset 作为最终输出
- 所有 PrimeVue 组件视觉行为最终都以该入口为准

### 5.2 PrimeVue 语义映射原则

PrimeVue 的 theme semantic tokens 应映射到项目定义的语义令牌，而不是相反。

也就是说：

- 先定义项目自己的设计语言
- 再把 PrimeVue 所需 token 对应到项目语义令牌
- 避免直接围绕 PrimeVue 默认命名反向塑造整个项目风格

### 5.3 与 Tailwind 的关系

后续 Tailwind 扩展配置应对齐同一套语义命名。

原则：

- Tailwind 不再继续维护一套独立“审美色板”
- `bg-*`、`text-*`、`border-*` 等扩展命名应围绕同一组语义令牌收敛
- 页面写样式时优先写语义，而不是写孤立色值

例如，后续应更倾向于：

- `bg-surface`
- `text-primary`
- `border-default`

而不是继续任意写零散石板灰色值或页面专属品牌色。

## 6. 组件级主题约束

### 6.1 Button

按钮应区分：

- Primary
- Secondary
- Outline
- Ghost
- Danger

要求：

- Primary 作为强调动作，数量受控
- Secondary / Outline 用于普通操作
- Danger 只用于危险操作
- 不允许在不同页面出现多个互不一致的主按钮色系

### 6.2 Input / Select / Form Controls

输入类控件必须统一：

- 默认边框
- hover 边框
- focus ring
- placeholder 颜色
- disabled 状态
- error 状态

企业后台风下，重点是：

- 输入区清晰但不过度装饰
- focus 明确可见，但不刺眼
- error 与 warning 状态层级清晰

### 6.3 Card / Panel / Surface

卡片与内容容器应统一：

- 背景色
- 边框色
- 圆角
- 阴影级别
- 标题区与内容区层级关系

原则：

- 以轻边框 + 浅背景为主
- 避免过重阴影导致页面漂浮感过强

### 6.4 Dialog / Drawer / Overlay

浮层组件要统一：

- 遮罩层透明度
- 浮层背景
- 标题样式
- 分割线与底部按钮区
- 关闭按钮与焦点态

目标：

- 在复杂管理流程中保持稳定、专业、可读的操作体验

### 6.5 Table / Paginator / Tag / Toast

后台页常见组件必须纳入设计约束：

- 表头背景与文本层级
- 行 hover 态
- 选中态
- 分页按钮尺寸和焦点态
- Tag / Badge 的状态色层级
- Toast 的信息区、图标区和严重级别样式

这部分是企业后台风落地的关键组成，不能只关注按钮和输入框。

## 7. 后续任务约束关系

### T16 的约束

T16 必须基于本任务定义的令牌体系改造共享基础组件层：

- `BaseButton`
- `BaseInput`
- `BaseCard`
- `ConfirmDialog`
- `Toast`
- `PromptDialog`
- `LoadingSkeleton`

要求：

- 优先替换视觉与交互基线
- 尽量保持组件对外 API 稳定
- 不允许 T16 脱离 T15 令牌体系自行定义样式

### T17-T19 的约束

页面迁移时：

- 登录页、布局、管理页、业务页都必须引用同一套视觉令牌
- 页面层不得重新发明颜色、间距、圆角和表单态标准
- 即使保留 Element Plus 过渡组件，也要按这套令牌做风格贴齐

### T20 的约束

T20 清理混用时，应以 T15 的令牌体系作为最终标准，而不是仅凭“是否用了 PrimeVue / Element Plus”来判断完成度。

判断标准应是：

- 是否统一视觉语言
- 是否统一交互状态
- 是否统一组件层级与语义命名

## 8. 建议落地文件边界

虽然本任务当前不写实现代码，但为后续落地建议文件职责如下：

- `frontend/src/shared/theme/tokens.js`
  - 定义项目设计令牌
- `frontend/src/shared/theme/primevue-theme.js`
  - 将项目令牌映射为 PrimeVue 主题对象
- `frontend/tailwind.config.js`
  - 将 Tailwind 扩展命名对齐到同一套语义令牌
- `frontend/src/main.js`
  - 使用项目自定义主题入口替换默认 preset 接入方式

这些职责划分要在后续实现中保持清晰，不应把主题逻辑分散到多个无关组件文件里。

## 9. 完成标准映射

任务表要求：

> 统一色彩、间距、圆角、阴影、表单态

本方案已经明确：

- 色彩：通过 primitive + semantic token 建立完整体系
- 间距：通过统一 spacing 尺度收敛
- 圆角：通过 shape token 收敛
- 阴影：通过 elevation token 收敛
- 表单态：通过 input / form control / state token 统一

因此，T15 的交付物边界清晰，并能直接支撑 T16 的基础组件改造。

## 10. 本任务不包含的内容

本任务明确不包含：

- 不直接替换 `BaseButton` / `BaseInput` / `BaseCard` 等组件实现
- 不修改页面中的 Element Plus 调用
- 不处理具体页面迁移
- 不调整业务流程或页面结构
- 不讨论与 UI 无关的数据接口和状态管理问题

这些内容将在 T16-T20 中继续推进。