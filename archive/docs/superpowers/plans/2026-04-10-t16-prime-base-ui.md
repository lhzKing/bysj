# T16 Prime Base UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成基础 UI 组件层的 PrimeVue 标准化重构：新增 `Prime*` 标准层、将旧 `Base*` 收敛为兼容层、移除基础交互对 Element Plus 的依赖，并补齐最小前端测试基础。

**Architecture:** 以 T15 的企业主题与设计令牌为唯一视觉来源，新增 `shared/components/prime/*` 作为新的标准基础组件层；旧 `shared/components/ui/*` 只负责兼容历史调用方式。通过 PrimeVue Toast/Confirmation 服务与项目自建 Prompt 宿主，统一基础交互能力，并用 Vitest + Vue Test Utils 为本次重构提供最小自动化验证。

**Tech Stack:** Vue 3、PrimeVue 4、@primeuix/themes、Vitest、@vue/test-utils、jsdom、Vite

---

## File Map

**Create:**
- `frontend/src/shared/theme/tokens.js` - 企业主题令牌定义
- `frontend/src/shared/theme/primevue-theme.js` - PrimeVue 主题对象导出
- `frontend/src/shared/components/prime/PrimeButton.vue` - Prime 按钮标准层
- `frontend/src/shared/components/prime/PrimeInput.vue` - Prime 输入标准层
- `frontend/src/shared/components/prime/PrimeCard.vue` - Prime 卡片标准层
- `frontend/src/shared/components/prime/PrimeLoadingSkeleton.vue` - Prime 骨架屏标准层
- `frontend/src/shared/components/prime/PrimeToastHost.vue` - Prime Toast 宿主
- `frontend/src/shared/components/prime/PrimeConfirmDialog.vue` - Prime Confirm 宿主
- `frontend/src/shared/components/prime/PrimePromptDialog.vue` - Prime Prompt 宿主
- `frontend/src/shared/composables/promptState.js` - Prompt 全局状态与解析器
- `frontend/src/test/setup.js` - Vitest 全局测试初始化
- `frontend/src/test/renderWithPrime.js` - 测试挂载 PrimeVue/Pinia/Router 的辅助方法
- `frontend/src/shared/theme/__tests__/tokens.test.js`
- `frontend/src/shared/components/prime/__tests__/prime-basic-controls.test.js`
- `frontend/src/shared/components/ui/__tests__/base-basic-controls.test.js`
- `frontend/src/shared/components/prime/__tests__/prime-toast.test.js`
- `frontend/src/shared/components/prime/__tests__/prime-card.test.js`
- `frontend/src/shared/components/ui/__tests__/base-card.test.js`
- `frontend/src/shared/composables/__tests__/useConfirm.test.js`
- `frontend/src/shared/composables/__tests__/usePrompt.test.js`
- `frontend/src/App.test.js`

**Modify:**
- `frontend/package.json` - 添加测试依赖与脚本
- `frontend/vite.config.js` - 添加 Vitest 配置
- `frontend/src/main.js` - 切换到项目自定义 PrimeVue 主题并注册服务插件
- `frontend/src/App.vue` - 挂载 Prime 基础交互宿主
- `frontend/src/shared/components/ui/BaseButton.vue` - 改为兼容转发
- `frontend/src/shared/components/ui/BaseInput.vue` - 改为兼容转发
- `frontend/src/shared/components/ui/BaseCard.vue` - 改为兼容转发
- `frontend/src/shared/components/ui/LoadingSkeleton.vue` - 改为兼容转发
- `frontend/src/shared/components/ui/Toast.vue` - 改为兼容壳
- `frontend/src/shared/components/ui/ConfirmDialog.vue` - 改为兼容壳
- `frontend/src/shared/components/ui/PromptDialog.vue` - 改为兼容壳
- `frontend/src/shared/composables/useToast.js` - 改为 PrimeVue ToastService 适配
- `frontend/src/shared/composables/useConfirm.js` - 改为 PrimeVue ConfirmationService 适配
- `frontend/src/shared/composables/usePrompt.js` - 改为驱动 Prompt 宿主
- `项目整改执行任务表.md` - 任务完成后更新状态与记录

**Workspace note:** 当前工作区未检测到 `.git`，本计划中的“Checkpoint”步骤用“更新任务表/保存本地进度”替代 git commit。

---

### Task 1: 建立最小前端测试基础

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/vite.config.js`
- Create: `frontend/src/test/setup.js`
- Create: `frontend/src/test/renderWithPrime.js`
- Create: `frontend/src/shared/theme/__tests__/tokens.test.js`

- [ ] **Step 1: 先写主题令牌测试，故意让它因测试脚本缺失而失败**

```js
// frontend/src/shared/theme/__tests__/tokens.test.js
import { describe, expect, it } from 'vitest'
import { enterpriseTokens } from '@/shared/theme/tokens'
import { enterpriseTheme } from '@/shared/theme/primevue-theme'

describe('enterprise theme tokens', () => {
  it('exports semantic groups for brand, background and text', () => {
    expect(enterpriseTokens.semantic.primary).toBeDefined()
    expect(enterpriseTokens.semantic.bg).toBeDefined()
    expect(enterpriseTokens.semantic.text).toBeDefined()
  })

  it('exports a PrimeVue theme object', () => {
    expect(enterpriseTheme.semantic).toBeDefined()
  })
})
```

- [ ] **Step 2: 运行测试，确认先因 test 脚本缺失而 RED**

Run: `cd frontend; npm run test -- --run src/shared/theme/__tests__/tokens.test.js`
Expected: FAIL，报错应类似 `Missing script: "test"`。

- [ ] **Step 3: 补测试脚本、Vitest 配置与测试辅助文件**

```json
// frontend/package.json（节选）
{
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview",
    "test": "vitest",
    "test:run": "vitest run"
  },
  "devDependencies": {
    "@vue/test-utils": "^2.4.6",
    "jsdom": "^26.1.0",
    "vitest": "^3.2.4"
  }
}
```

```js
// frontend/vite.config.js（新增 test 段）
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.js',
    css: true
  },
  server: {
    https: {
      key: fs.readFileSync(path.resolve(__dirname, 'certs/localhost.key')),
      cert: fs.readFileSync(path.resolve(__dirname, 'certs/localhost.crt'))
    }
  }
})
```

```js
// frontend/src/test/setup.js
import { config } from '@vue/test-utils'

config.global.stubs = {
  transition: false,
  teleport: true
}

class ResizeObserverMock {
  observe() {}
  unobserve() {}
  disconnect() {}
}

global.ResizeObserver = ResizeObserverMock
window.matchMedia = window.matchMedia || (() => ({
  matches: false,
  addEventListener() {},
  removeEventListener() {}
}))
```

```js
// frontend/src/test/renderWithPrime.js
import { mount } from '@vue/test-utils'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import { enterpriseTheme } from '@/shared/theme/primevue-theme'

export function renderWithPrime(component, options = {}) {
  return mount(component, {
    ...options,
    global: {
      plugins: [
        [PrimeVue, { theme: { preset: enterpriseTheme } }],
        ToastService,
        ConfirmationService,
        ...(options.global?.plugins || [])
      ],
      ...(options.global || {})
    }
  })
}
```

- [ ] **Step 4: 安装新增测试依赖**

Run: `cd frontend; npm install`
Expected: PASS，`vitest`、`@vue/test-utils`、`jsdom` 被写入锁文件并成功安装。

- [ ] **Step 5: 再跑一次测试，确认它继续因主题模块不存在而 RED**

Run: `cd frontend; npm run test -- --run src/shared/theme/__tests__/tokens.test.js`
Expected: FAIL，报错应类似 `Failed to resolve import '@/shared/theme/tokens'`。

- [ ] **Step 6: Checkpoint 当前进度**

更新任务表的“最近一次更新摘要”为“已补前端测试基础，T16 主题实现待补”，避免中断后丢失上下文。

---

### Task 2: 落地企业主题令牌与 PrimeVue 主题入口

**Files:**
- Create: `frontend/src/shared/theme/tokens.js`
- Create: `frontend/src/shared/theme/primevue-theme.js`
- Modify: `frontend/src/main.js`
- Test: `frontend/src/shared/theme/__tests__/tokens.test.js`

- [ ] **Step 1: 保持 tokens 测试不变，直接运行确认 RED 原因正确**

Run: `cd frontend; npm run test -- --run src/shared/theme/__tests__/tokens.test.js`
Expected: FAIL，错误应来自 `tokens.js` / `primevue-theme.js` 不存在，而不是 Vitest 环境异常。
- [ ] **Step 2: 写最小主题令牌与主题对象实现**

```js
// frontend/src/shared/theme/tokens.js
export const enterpriseTokens = {
  primitive: {
    blue: {
      50: '#eff6ff',
      100: '#dbeafe',
      500: '#2563eb',
      600: '#1d4ed8',
      700: '#1e40af',
      900: '#0f172a'
    },
    slate: {
      0: '#ffffff',
      50: '#f8fafc',
      100: '#f1f5f9',
      200: '#e2e8f0',
      300: '#cbd5e1',
      500: '#64748b',
      700: '#334155',
      900: '#0f172a'
    },
    green: { 50: '#ecfdf5', 500: '#16a34a', 700: '#15803d' },
    amber: { 50: '#fffbeb', 500: '#d97706', 700: '#b45309' },
    red: { 50: '#fef2f2', 500: '#dc2626', 700: '#b91c1c' }
  },
  semantic: {
    primary: { color: '{blue.600}', hoverColor: '{blue.700}', activeColor: '{blue.700}', contrastColor: '{slate.0}' },
    bg: { app: '{slate.50}', page: '{slate.100}', surface: '{slate.0}', elevated: '{slate.0}', overlay: 'rgba(15, 23, 42, 0.45)' },
    text: { primary: '{slate.900}', secondary: '{slate.700}', muted: '{slate.500}', inverse: '{slate.0}', link: '{blue.600}' },
    border: { subtle: '{slate.100}', default: '{slate.200}', strong: '{slate.300}', focus: '{blue.600}' },
    state: {
      success: '{green.500}', successBg: '{green.50}',
      warning: '{amber.500}', warningBg: '{amber.50}',
      danger: '{red.500}', dangerBg: '{red.50}',
      info: '{blue.500}', infoBg: '{blue.50}'
    }
  },
  controls: {
    radius: { sm: '6px', md: '10px', lg: '14px', xl: '18px' },
    shadow: { sm: '0 1px 2px rgba(15, 23, 42, 0.06)', md: '0 10px 20px rgba(15, 23, 42, 0.08)', lg: '0 16px 32px rgba(15, 23, 42, 0.12)' },
    height: { sm: '32px', md: '40px', lg: '48px' }
  }
}
```

```js
// frontend/src/shared/theme/primevue-theme.js
import { enterpriseTokens } from './tokens'

export const enterpriseTheme = {
  primitive: enterpriseTokens.primitive,
  semantic: {
    primary: {
      50: '{blue.50}',
      100: '{blue.100}',
      500: '{blue.500}',
      600: '{blue.600}',
      700: '{blue.700}'
    },
    colorScheme: {
      light: {
        surface: {
          0: '{slate.0}',
          50: '{slate.50}',
          100: '{slate.100}',
          200: '{slate.200}',
          900: '{slate.900}'
        },
        formField: {
          background: '{slate.0}',
          borderColor: '{slate.200}',
          hoverBorderColor: '{blue.500}',
          focusBorderColor: '{blue.600}',
          color: '{slate.900}',
          placeholderColor: '{slate.500}'
        }
      }
    },
    focusRing: {
      width: '2px',
      style: 'solid',
      color: '{blue.600}',
      offset: '1px'
    }
  }
}
```

```js
// frontend/src/main.js
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import PrimeVue from 'primevue/config'
import pinia from './core/stores'
import router from './core/router'
import App from './App.vue'
import './style.css'
import { enterpriseTheme } from './shared/theme/primevue-theme'

const app = createApp(App)
app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.use(PrimeVue, {
  theme: {
    preset: enterpriseTheme
  }
})
app.mount('#app')
```

- [ ] **Step 3: 跑主题测试，确认 GREEN**

Run: `cd frontend; npm run test -- --run src/shared/theme/__tests__/tokens.test.js`
Expected: PASS

- [ ] **Step 4: 跑一次构建，确认自定义主题入口不破坏现有编译**

Run: `cd frontend; npm run build`
Expected: PASS

- [ ] **Step 5: Checkpoint 当前进度**

将任务表“最近一次更新摘要”改为“已完成主题令牌和 PrimeVue 主题入口落地，继续实现标准层组件”。

---

### Task 3: 实现 Prime 标准层中的 Button / Input / LoadingSkeleton

**Files:**
- Create: `frontend/src/shared/components/prime/PrimeButton.vue`
- Create: `frontend/src/shared/components/prime/PrimeInput.vue`
- Create: `frontend/src/shared/components/prime/PrimeLoadingSkeleton.vue`
- Create: `frontend/src/shared/components/prime/__tests__/prime-basic-controls.test.js`
- Test: `frontend/src/test/renderWithPrime.js`

- [ ] **Step 1: 先写标准层组件测试**

```js
// frontend/src/shared/components/prime/__tests__/prime-basic-controls.test.js
import { describe, expect, it } from 'vitest'
import { renderWithPrime } from '@/test/renderWithPrime'
import PrimeButton from '@/shared/components/prime/PrimeButton.vue'
import PrimeInput from '@/shared/components/prime/PrimeInput.vue'
import PrimeLoadingSkeleton from '@/shared/components/prime/PrimeLoadingSkeleton.vue'

describe('prime basic controls', () => {
  it('renders PrimeButton with forwarded label and block class', async () => {
    const wrapper = renderWithPrime(PrimeButton, { props: { label: '保存', block: true, variant: 'primary' } })
    expect(wrapper.text()).toContain('保存')
    expect(wrapper.classes()).toContain('w-full')
  })

  it('updates PrimeInput model and shows error text', async () => {
    const wrapper = renderWithPrime(PrimeInput, { props: { modelValue: '', error: '必填项', label: '用户名' } })
    expect(wrapper.text()).toContain('用户名')
    expect(wrapper.text()).toContain('必填项')
  })

  it('renders a table skeleton layout', () => {
    const wrapper = renderWithPrime(PrimeLoadingSkeleton, { props: { type: 'table', rows: 3 } })
    expect(wrapper.findAll('[data-test="skeleton-row"]')).toHaveLength(3)
  })
})
```

- [ ] **Step 2: 运行测试，确认组件缺失导致 RED**

Run: `cd frontend; npm run test -- --run src/shared/components/prime/__tests__/prime-basic-controls.test.js`
Expected: FAIL，错误应类似 `Failed to resolve import '@/shared/components/prime/PrimeButton.vue'`。

- [ ] **Step 3: 写最小标准层实现，让 Button / Input / Skeleton 先可用**

```vue
// frontend/src/shared/components/prime/PrimeButton.vue
<script setup>
import { computed, useAttrs } from 'vue'
import Button from 'primevue/button'
import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

const props = defineProps({
  label: String,
  variant: {
    type: String,
    default: 'primary'
  },
  size: {
    type: String,
    default: 'md'
  },
  block: Boolean,
  loading: Boolean,
  disabled: Boolean,
  icon: String,
  iconPos: {
    type: String,
    default: 'left'
  },
  class: [String, Array, Object]
})

const attrs = useAttrs()

const sizeMap = {
  sm: 'small',
  md: undefined,
  lg: 'large'
}

const severityMap = {
  primary: undefined,
  secondary: 'secondary',
  outline: 'secondary',
  ghost: 'secondary',
  danger: 'danger'
}

const primeVariant = computed(() => {
  if (props.variant === 'outline') return 'outlined'
  if (props.variant === 'ghost') return 'text'
  return undefined
})

const classes = computed(() =>
  twMerge(
    clsx(
      'prime-base-button',
      props.block && 'w-full',
      props.class
    )
  )
)
</script>

<template>
  <Button
    v-bind="attrs"
    :label="label"
    :icon="icon"
    :iconPos="iconPos"
    :loading="loading"
    :disabled="disabled || loading"
    :size="sizeMap[size]"
    :severity="severityMap[variant]"
    :variant="primeVariant"
    :fluid="block"
    :class="classes"
  >
    <slot />
  </Button>
</template>
```

```vue
// frontend/src/shared/components/prime/PrimeInput.vue
<script setup>
import { computed } from 'vue'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

const props = defineProps({
  modelValue: [String, Number],
  label: String,
  placeholder: String,
  type: {
    type: String,
    default: 'text'
  },
  error: String,
  disabled: Boolean,
  icon: Object,
  inputId: String
})

const emit = defineEmits(['update:modelValue'])

const value = computed({
  get: () => props.modelValue ?? '',
  set: (nextValue) => emit('update:modelValue', nextValue)
})

const isPassword = computed(() => props.type === 'password')

const inputClass = computed(() =>
  twMerge(
    clsx(
      'w-full',
      props.icon && 'pl-10',
      props.error && 'p-invalid'
    )
  )
)
</script>

<template>
  <div class="space-y-1.5">
    <label v-if="label" :for="inputId" class="block text-sm font-medium text-slate-700">
      {{ label }}
    </label>

    <div class="relative">
      <div v-if="icon" class="pointer-events-none absolute inset-y-0 left-3 flex items-center text-slate-400">
        <component :is="icon" :size="18" />
      </div>

      <Password
        v-if="isPassword"
        v-model="value"
        :inputId="inputId"
        :placeholder="placeholder"
        :disabled="disabled"
        :feedback="false"
        toggleMask
        fluid
        :invalid="Boolean(error)"
        :inputClass="inputClass"
      />
      <InputText
        v-else
        v-model="value"
        :id="inputId"
        :placeholder="placeholder"
        :disabled="disabled"
        fluid
        :invalid="Boolean(error)"
        :class="inputClass"
      />
    </div>

    <p v-if="error" class="text-xs text-red-600">{{ error }}</p>
  </div>
</template>
```

```vue
// frontend/src/shared/components/prime/PrimeLoadingSkeleton.vue
<script setup>
import { computed } from 'vue'
import Skeleton from 'primevue/skeleton'

const props = defineProps({
  type: {
    type: String,
    default: 'default',
    validator: (value) => ['card', 'table', 'chart', 'kpi', 'detail', 'list', 'default'].includes(value)
  },
  rows: {
    type: Number,
    default: 5
  }
})

const rowIndexes = computed(() => Array.from({ length: props.rows }, (_, index) => index))
const chartHeights = ['35%', '58%', '42%', '76%', '54%']
</script>

<template>
  <div class="space-y-4">
    <div v-if="type === 'card'" class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <Skeleton width="40%" height="1rem" class="mb-4" />
      <Skeleton width="28%" height="2rem" class="mb-3" />
      <Skeleton width="100%" height="0.75rem" />
    </div>

    <div v-else-if="type === 'table'" class="rounded-xl border border-slate-200 bg-white shadow-sm">
      <div class="border-b border-slate-200 p-4">
        <div class="mb-4 flex items-center justify-between">
          <Skeleton width="8rem" height="2rem" />
          <Skeleton width="6rem" height="2.5rem" />
        </div>
      </div>
      <div class="p-4">
        <div
          v-for="row in rowIndexes"
          :key="row"
          data-test="skeleton-row"
          class="flex items-center justify-between border-b border-slate-100 py-4 last:border-b-0"
        >
          <div class="flex-1 space-y-2">
            <Skeleton width="70%" height="1rem" />
            <Skeleton width="44%" height="0.75rem" />
          </div>
          <div class="ml-4 flex gap-2">
            <Skeleton width="4rem" height="2rem" />
            <Skeleton width="4rem" height="2rem" />
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="type === 'chart'" class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <Skeleton width="25%" height="1.25rem" class="mb-4" />
      <div class="flex h-64 items-end justify-between gap-3 rounded-lg bg-slate-50 p-4">
        <Skeleton
          v-for="(height, index) in chartHeights"
          :key="index"
          width="12%"
          :height="height"
        />
      </div>
    </div>

    <div v-else-if="type === 'kpi'" class="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
      <div v-for="cardIndex in 4" :key="cardIndex" class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <div class="mb-4 flex items-start justify-between">
          <Skeleton width="6rem" height="1rem" />
          <Skeleton shape="circle" size="2.75rem" />
        </div>
        <Skeleton width="7rem" height="2rem" class="mb-2" />
        <Skeleton width="4rem" height="0.75rem" />
      </div>
    </div>

    <div v-else-if="type === 'detail'" class="space-y-6 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <div class="border-b border-slate-200 pb-4">
        <Skeleton width="34%" height="2rem" class="mb-2" />
        <Skeleton width="46%" height="0.875rem" />
      </div>
      <div class="grid grid-cols-1 gap-6 md:grid-cols-2">
        <div v-for="fieldIndex in 6" :key="fieldIndex" class="space-y-2">
          <Skeleton width="5rem" height="0.75rem" />
          <Skeleton width="100%" height="1.25rem" />
        </div>
      </div>
      <div class="border-t border-slate-200 pt-4">
        <Skeleton width="7rem" height="1.25rem" class="mb-4" />
        <Skeleton width="100%" height="12rem" />
      </div>
    </div>

    <div v-else-if="type === 'list'" class="space-y-4">
      <div v-for="row in rowIndexes" :key="row" class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <div class="flex items-center gap-4">
          <Skeleton shape="circle" size="3rem" />
          <div class="flex-1 space-y-2">
            <Skeleton width="72%" height="1rem" />
            <Skeleton width="48%" height="0.75rem" />
          </div>
          <Skeleton width="5rem" height="2rem" />
        </div>
      </div>
    </div>

    <div v-else class="space-y-3">
      <Skeleton width="75%" height="1rem" />
      <Skeleton width="100%" height="1rem" />
      <Skeleton width="84%" height="1rem" />
    </div>
  </div>
</template>
```

- [ ] **Step 4: 跑目标测试，确认标准层组件变 GREEN**

Run: `cd frontend; npm run test -- --run src/shared/components/prime/__tests__/prime-basic-controls.test.js`
Expected: PASS

- [ ] **Step 5: Checkpoint 当前进度**

将任务表“最近一次更新摘要”改为“已完成 Prime 标准层基础控件首批实现，开始改写 Base 兼容层”。

---
### Task 4: 将 BaseButton / BaseInput / LoadingSkeleton 改为兼容转发层

**Files:**
- Modify: `frontend/src/shared/components/ui/BaseButton.vue`
- Modify: `frontend/src/shared/components/ui/BaseInput.vue`
- Modify: `frontend/src/shared/components/ui/LoadingSkeleton.vue`
- Create: `frontend/src/shared/components/ui/__tests__/base-basic-controls.test.js`
- Test: `frontend/src/shared/components/prime/PrimeButton.vue`
- Test: `frontend/src/shared/components/prime/PrimeInput.vue`
- Test: `frontend/src/shared/components/prime/PrimeLoadingSkeleton.vue`

- [ ] **Step 1: 先写兼容层测试，锁定旧 API 继续可用**

```js
// frontend/src/shared/components/ui/__tests__/base-basic-controls.test.js
import { describe, expect, it } from 'vitest'
import { renderWithPrime } from '@/test/renderWithPrime'
import PrimeButton from '@/shared/components/prime/PrimeButton.vue'
import PrimeInput from '@/shared/components/prime/PrimeInput.vue'
import PrimeLoadingSkeleton from '@/shared/components/prime/PrimeLoadingSkeleton.vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import BaseInput from '@/shared/components/ui/BaseInput.vue'
import LoadingSkeleton from '@/shared/components/ui/LoadingSkeleton.vue'

describe('base compatibility controls', () => {
  it('keeps BaseButton slot and variant API', () => {
    const wrapper = renderWithPrime(BaseButton, {
      props: { variant: 'danger', block: true },
      slots: { default: '删除' }
    })

    const primeButton = wrapper.findComponent(PrimeButton)
    expect(primeButton.exists()).toBe(true)
    expect(primeButton.props('variant')).toBe('danger')
    expect(wrapper.text()).toContain('删除')
  })

  it('keeps BaseInput label and update:modelValue API', async () => {
    const wrapper = renderWithPrime(BaseInput, {
      props: { modelValue: '', label: '账号', placeholder: '请输入账号' }
    })

    await wrapper.find('input').setValue('admin')

    const primeInput = wrapper.findComponent(PrimeInput)
    expect(primeInput.exists()).toBe(true)
    expect(wrapper.emitted('update:modelValue')[0]).toEqual(['admin'])
  })

  it('keeps LoadingSkeleton type and rows API', () => {
    const wrapper = renderWithPrime(LoadingSkeleton, {
      props: { type: 'table', rows: 2 }
    })

    const primeSkeleton = wrapper.findComponent(PrimeLoadingSkeleton)
    expect(primeSkeleton.exists()).toBe(true)
    expect(primeSkeleton.props('rows')).toBe(2)
  })
})
```

- [ ] **Step 2: 运行兼容层测试，确认旧组件还未转发而 RED**

Run: `cd frontend; npm run test -- --run src/shared/components/ui/__tests__/base-basic-controls.test.js`
Expected: FAIL，断言应显示未找到 `PrimeButton` / `PrimeInput` / `PrimeLoadingSkeleton`。

- [ ] **Step 3: 将旧 UI 组件改为 Prime 标准层转发壳**

```vue
// frontend/src/shared/components/ui/BaseButton.vue
<script setup>
import PrimeButton from '@/shared/components/prime/PrimeButton.vue'

defineOptions({
  inheritAttrs: false
})

defineProps({
  variant: {
    type: String,
    default: 'primary'
  },
  size: {
    type: String,
    default: 'md'
  },
  block: Boolean,
  loading: Boolean,
  disabled: Boolean,
  class: [String, Array, Object]
})
</script>

<template>
  <PrimeButton
    v-bind="$attrs"
    :variant="variant"
    :size="size"
    :block="block"
    :loading="loading"
    :disabled="disabled"
    :class="$props.class"
  >
    <slot />
  </PrimeButton>
</template>
```

```vue
// frontend/src/shared/components/ui/BaseInput.vue
<script setup>
import PrimeInput from '@/shared/components/prime/PrimeInput.vue'

defineOptions({
  inheritAttrs: false
})

defineProps({
  modelValue: [String, Number],
  label: String,
  placeholder: String,
  type: {
    type: String,
    default: 'text'
  },
  error: String,
  disabled: Boolean,
  icon: Object,
  inputId: String
})

defineEmits(['update:modelValue'])
</script>

<template>
  <PrimeInput
    v-bind="$attrs"
    :modelValue="modelValue"
    :label="label"
    :placeholder="placeholder"
    :type="type"
    :error="error"
    :disabled="disabled"
    :icon="icon"
    :inputId="inputId"
    @update:modelValue="$emit('update:modelValue', $event)"
  />
</template>
```

```vue
// frontend/src/shared/components/ui/LoadingSkeleton.vue
<script setup>
import PrimeLoadingSkeleton from '@/shared/components/prime/PrimeLoadingSkeleton.vue'

defineOptions({
  inheritAttrs: false
})

defineProps({
  type: {
    type: String,
    default: 'default'
  },
  rows: {
    type: Number,
    default: 5
  }
})
</script>

<template>
  <PrimeLoadingSkeleton v-bind="$attrs" :type="type" :rows="rows" />
</template>
```

- [ ] **Step 4: 跑兼容层测试，确认 GREEN**

Run: `cd frontend; npm run test -- --run src/shared/components/ui/__tests__/base-basic-controls.test.js`
Expected: PASS

- [ ] **Step 5: Checkpoint 当前进度**

将任务表“最近一次更新摘要”改为“BaseButton/BaseInput/LoadingSkeleton 已切到 Prime 兼容层，下一步处理 Toast”。

---
### Task 5: 收口 Toast 到 PrimeVue ToastService

**Files:**
- Create: `frontend/src/shared/components/prime/PrimeToastHost.vue`
- Create: `frontend/src/shared/components/prime/__tests__/prime-toast.test.js`
- Modify: `frontend/src/shared/composables/useToast.js`
- Modify: `frontend/src/shared/components/ui/Toast.vue`
- Modify: `frontend/src/main.js`

- [ ] **Step 1: 先写 Toast 桥接测试，锁定“模块级调用也能出消息”**

```js
// frontend/src/shared/components/prime/__tests__/prime-toast.test.js
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'

const add = vi.fn()

vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add })
}))

import { renderWithPrime } from '@/test/renderWithPrime'
import PrimeToastHost from '@/shared/components/prime/PrimeToastHost.vue'
import { useToast, __resetToastBridge } from '@/shared/composables/useToast'

describe('PrimeToastHost', () => {
  beforeEach(() => {
    add.mockClear()
    __resetToastBridge()
  })

  it('bridges success messages to PrimeVue ToastService', async () => {
    renderWithPrime(PrimeToastHost)
    await nextTick()

    useToast().success('保存成功', 1200)

    expect(add).toHaveBeenCalledWith(expect.objectContaining({
      group: 'app-toast',
      severity: 'success',
      detail: '保存成功',
      life: 1200
    }))
  })
})
```

- [ ] **Step 2: 运行测试，确认当前仍依赖 Element Plus 而 RED**

Run: `cd frontend; npm run test -- --run src/shared/components/prime/__tests__/prime-toast.test.js`
Expected: FAIL，错误应来自 `PrimeToastHost.vue` 缺失或 `__resetToastBridge` 未导出。

- [ ] **Step 3: 加 Toast 桥接层、Prime 宿主和插件注册**

```js
// frontend/src/shared/composables/useToast.js
const listeners = new Set()

const summaryMap = {
  success: '成功',
  error: '错误',
  warn: '警告',
  info: '提示'
}

function emitToast(payload) {
  listeners.forEach((listener) => listener(payload))
}

export function onToastMessage(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function __resetToastBridge() {
  listeners.clear()
}

function createMessage(severity, message, duration) {
  return {
    group: 'app-toast',
    severity,
    summary: summaryMap[severity] || '提示',
    detail: message,
    life: duration
  }
}

export function useToast() {
  return {
    success: (message, duration = 3000) => emitToast(createMessage('success', message, duration)),
    error: (message, duration = 3000) => emitToast(createMessage('error', message, duration)),
    warning: (message, duration = 3000) => emitToast(createMessage('warn', message, duration)),
    info: (message, duration = 3000) => emitToast(createMessage('info', message, duration))
  }
}
```

```vue
// frontend/src/shared/components/prime/PrimeToastHost.vue
<script setup>
import { onBeforeUnmount, onMounted } from 'vue'
import Toast from 'primevue/toast'
import { useToast as usePrimeToast } from 'primevue/usetoast'
import { onToastMessage } from '@/shared/composables/useToast'

const toast = usePrimeToast()
let stopListening = null

onMounted(() => {
  stopListening = onToastMessage((message) => {
    toast.add(message)
  })
})

onBeforeUnmount(() => {
  stopListening?.()
})
</script>

<template>
  <Toast group="app-toast" position="top-center" />
</template>
```

```vue
// frontend/src/shared/components/ui/Toast.vue
<script setup>
import PrimeToastHost from '@/shared/components/prime/PrimeToastHost.vue'
</script>

<template>
  <PrimeToastHost />
</template>
```

```js
// frontend/src/main.js
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import pinia from './core/stores'
import router from './core/router'
import App from './App.vue'
import './style.css'
import { enterpriseTheme } from './shared/theme/primevue-theme'

const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.use(PrimeVue, {
  theme: {
    preset: enterpriseTheme
  }
})
app.use(ToastService)

app.mount('#app')
```

- [ ] **Step 4: 跑 Toast 测试，确认桥接 GREEN**

Run: `cd frontend; npm run test -- --run src/shared/components/prime/__tests__/prime-toast.test.js`
Expected: PASS

- [ ] **Step 5: Checkpoint 当前进度**

将任务表“最近一次更新摘要”改为“Toast 已脱离 Element Plus 并切到 PrimeVue ToastService，下一步处理 Card/Confirm/Prompt”。

---
### Task 6: 实现 PrimeCard 并改写 BaseCard 兼容层

**Files:**
- Create: `frontend/src/shared/components/prime/PrimeCard.vue`
- Create: `frontend/src/shared/components/prime/__tests__/prime-card.test.js`
- Create: `frontend/src/shared/components/ui/__tests__/base-card.test.js`
- Modify: `frontend/src/shared/components/ui/BaseCard.vue`

- [ ] **Step 1: 先写 PrimeCard 与 BaseCard 测试**

```js
// frontend/src/shared/components/prime/__tests__/prime-card.test.js
import { describe, expect, it } from 'vitest'
import { renderWithPrime } from '@/test/renderWithPrime'
import PrimeCard from '@/shared/components/prime/PrimeCard.vue'

describe('PrimeCard', () => {
  it('renders title, content and footer slot', () => {
    const wrapper = renderWithPrime(PrimeCard, {
      props: {
        title: '库存概览',
        subtitle: '今日更新'
      },
      slots: {
        default: '<div>主体内容</div>',
        footer: '<div>底部操作</div>'
      }
    })

    expect(wrapper.text()).toContain('库存概览')
    expect(wrapper.text()).toContain('今日更新')
    expect(wrapper.text()).toContain('主体内容')
    expect(wrapper.text()).toContain('底部操作')
  })
})
```

```js
// frontend/src/shared/components/ui/__tests__/base-card.test.js
import { describe, expect, it } from 'vitest'
import { renderWithPrime } from '@/test/renderWithPrime'
import PrimeCard from '@/shared/components/prime/PrimeCard.vue'
import BaseCard from '@/shared/components/ui/BaseCard.vue'

describe('BaseCard compatibility', () => {
  it('forwards noPadding and custom class to PrimeCard', () => {
    const wrapper = renderWithPrime(BaseCard, {
      props: {
        noPadding: true,
        class: 'custom-shell'
      },
      slots: {
        default: '<div>内容区</div>'
      }
    })

    const primeCard = wrapper.findComponent(PrimeCard)
    expect(primeCard.exists()).toBe(true)
    expect(primeCard.props('noPadding')).toBe(true)
    expect(primeCard.props('class')).toContain('custom-shell')
  })
})
```

- [ ] **Step 2: 运行测试，确认组件缺失导致 RED**

Run: `cd frontend; npm run test -- --run src/shared/components/prime/__tests__/prime-card.test.js src/shared/components/ui/__tests__/base-card.test.js`
Expected: FAIL，错误应来自 `PrimeCard.vue` 缺失或 `BaseCard` 仍是旧实现。

- [ ] **Step 3: 写 PrimeCard 与 BaseCard 兼容转发**

```vue
// frontend/src/shared/components/prime/PrimeCard.vue
<script setup>
import { computed } from 'vue'
import Card from 'primevue/card'
import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

const props = defineProps({
  title: String,
  subtitle: String,
  noPadding: Boolean,
  variant: {
    type: String,
    default: 'default'
  },
  contentClass: [String, Array, Object],
  class: [String, Array, Object]
})

const rootClass = computed(() =>
  twMerge(
    clsx(
      'prime-base-card overflow-hidden border border-slate-200 shadow-sm',
      props.variant === 'muted' ? 'bg-slate-50' : 'bg-white',
      props.class
    )
  )
)

const contentClassName = computed(() =>
  twMerge(
    clsx(!props.noPadding && 'p-6', props.contentClass)
  )
)
</script>

<template>
  <Card
    :class="rootClass"
    :pt="{
      body: { class: 'p-0' },
      caption: { class: 'px-6 pt-5' },
      content: { class: contentClassName },
      footer: { class: 'px-6 pb-5 pt-0' }
    }"
  >
    <template v-if="$slots.header" #header>
      <slot name="header" />
    </template>

    <template v-if="$slots.title || title" #title>
      <slot name="title">{{ title }}</slot>
    </template>

    <template v-if="$slots.subtitle || subtitle" #subtitle>
      <slot name="subtitle">{{ subtitle }}</slot>
    </template>

    <template #content>
      <slot />
    </template>

    <template v-if="$slots.footer" #footer>
      <slot name="footer" />
    </template>
  </Card>
</template>
```

```vue
// frontend/src/shared/components/ui/BaseCard.vue
<script setup>
import PrimeCard from '@/shared/components/prime/PrimeCard.vue'

defineOptions({
  inheritAttrs: false
})

defineProps({
  class: [String, Array, Object],
  noPadding: Boolean
})
</script>

<template>
  <PrimeCard v-bind="$attrs" :class="$props.class" :noPadding="noPadding">
    <template v-if="$slots.header" #header>
      <slot name="header" />
    </template>
    <template v-if="$slots.title" #title>
      <slot name="title" />
    </template>
    <template v-if="$slots.subtitle" #subtitle>
      <slot name="subtitle" />
    </template>

    <slot />

    <template v-if="$slots.footer" #footer>
      <slot name="footer" />
    </template>
  </PrimeCard>
</template>
```

- [ ] **Step 4: 跑 Card 测试，确认 GREEN**

Run: `cd frontend; npm run test -- --run src/shared/components/prime/__tests__/prime-card.test.js src/shared/components/ui/__tests__/base-card.test.js`
Expected: PASS

- [ ] **Step 5: Checkpoint 当前进度**

将任务表“最近一次更新摘要”改为“PrimeCard 与 BaseCard 兼容层已完成，继续处理 Confirm/Prompt 宿主与服务层”。

---
### Task 7: 收口 Confirm / Prompt 到 Prime 宿主与 Promise 服务

**Files:**
- Create: `frontend/src/shared/components/prime/PrimeConfirmDialog.vue`
- Create: `frontend/src/shared/components/prime/PrimePromptDialog.vue`
- Create: `frontend/src/shared/composables/promptState.js`
- Create: `frontend/src/shared/composables/__tests__/useConfirm.test.js`
- Create: `frontend/src/shared/composables/__tests__/usePrompt.test.js`
- Create: `frontend/src/App.test.js`
- Modify: `frontend/src/shared/composables/useConfirm.js`
- Modify: `frontend/src/shared/composables/usePrompt.js`
- Modify: `frontend/src/shared/components/ui/ConfirmDialog.vue`
- Modify: `frontend/src/shared/components/ui/PromptDialog.vue`
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/main.js`

- [ ] **Step 1: 先写 Confirm / Prompt / App 宿主测试**

```js
// frontend/src/shared/composables/__tests__/useConfirm.test.js
import { beforeEach, describe, expect, it, vi } from 'vitest'

const requireMock = vi.fn()

vi.mock('primevue/useconfirm', () => ({
  useConfirm: () => ({
    require: requireMock,
    close: vi.fn()
  })
}))

import { useConfirm } from '@/shared/composables/useConfirm'

describe('useConfirm', () => {
  beforeEach(() => {
    requireMock.mockClear()
  })

  it('resolves true when accept callback is triggered', async () => {
    const { confirm } = useConfirm()
    const resultPromise = confirm({ title: '删除用户', message: '确认删除？', type: 'danger' })

    const options = requireMock.mock.calls[0][0]
    options.accept()

    await expect(resultPromise).resolves.toBe(true)
  })

  it('resolves false when reject callback is triggered', async () => {
    const { confirm } = useConfirm()
    const resultPromise = confirm({ title: '删除用户', message: '确认删除？' })

    const options = requireMock.mock.calls[0][0]
    options.reject()

    await expect(resultPromise).resolves.toBe(false)
  })
})
```

```js
// frontend/src/shared/composables/__tests__/usePrompt.test.js
import { beforeEach, describe, expect, it } from 'vitest'
import { nextTick } from 'vue'
import { renderWithPrime } from '@/test/renderWithPrime'
import PrimePromptDialog from '@/shared/components/prime/PrimePromptDialog.vue'
import { usePrompt } from '@/shared/composables/usePrompt'
import { resetPromptState } from '@/shared/composables/promptState'

describe('usePrompt', () => {
  beforeEach(() => {
    resetPromptState()
  })

  it('resolves entered value after confirm', async () => {
    const wrapper = renderWithPrime(PrimePromptDialog)
    const { prompt } = usePrompt()

    const resultPromise = prompt({
      title: '重置密码',
      message: '请输入新密码',
      placeholder: '请输入新密码',
      type: 'password'
    })

    await nextTick()
    await wrapper.find('input').setValue('new-password')
    await wrapper.find('[data-test="prompt-confirm"]').trigger('click')

    await expect(resultPromise).resolves.toBe('new-password')
  })

  it('returns null when prompt is cancelled', async () => {
    const wrapper = renderWithPrime(PrimePromptDialog)
    const { prompt } = usePrompt()

    const resultPromise = prompt({ title: '请输入备注' })

    await nextTick()
    await wrapper.find('[data-test="prompt-cancel"]').trigger('click')

    await expect(resultPromise).resolves.toBe(null)
  })
})
```

```js
// frontend/src/App.test.js
import { describe, expect, it } from 'vitest'
import { renderWithPrime } from '@/test/renderWithPrime'
import App from '@/App.vue'
import Toast from '@/shared/components/ui/Toast.vue'
import ConfirmDialog from '@/shared/components/ui/ConfirmDialog.vue'
import PromptDialog from '@/shared/components/ui/PromptDialog.vue'

describe('App shell', () => {
  it('mounts RouterView and interaction hosts together', () => {
    const wrapper = renderWithPrime(App, {
      global: {
        stubs: {
          RouterView: { template: '<div data-test="route-view" />' }
        }
      }
    })

    expect(wrapper.find('[data-test="route-view"]').exists()).toBe(true)
    expect(wrapper.findComponent(Toast).exists()).toBe(true)
    expect(wrapper.findComponent(ConfirmDialog).exists()).toBe(true)
    expect(wrapper.findComponent(PromptDialog).exists()).toBe(true)
  })
})
```

- [ ] **Step 2: 运行测试，确认服务层和宿主尚未切换而 RED**

Run: `cd frontend; npm run test -- --run src/shared/composables/__tests__/useConfirm.test.js src/shared/composables/__tests__/usePrompt.test.js src/App.test.js`
Expected: FAIL，错误应来自 `primevue/useconfirm` 桥接未实现、`PrimePromptDialog.vue` 缺失或 `App.vue` 尚未挂载宿主。

- [ ] **Step 3: 写 Confirm / Prompt 服务层、宿主与 App 挂载**

```js
// frontend/src/shared/composables/useConfirm.js
import { useConfirm as usePrimeConfirm } from 'primevue/useconfirm'

export function useConfirm() {
  const confirmService = usePrimeConfirm()

  function confirm(options = {}) {
    const {
      title = '确认操作',
      message = '确定要执行此操作吗？',
      confirmText = '确认',
      cancelText = '取消',
      type = 'warning'
    } = options

    return new Promise((resolve) => {
      let settled = false

      const finalize = (value) => {
        if (settled) return
        settled = true
        resolve(value)
      }

      confirmService.require({
        group: 'app-confirm',
        header: title,
        message,
        icon: type === 'danger' ? 'pi pi-exclamation-triangle' : 'pi pi-info-circle',
        acceptLabel: confirmText,
        rejectLabel: cancelText,
        rejectProps: {
          severity: 'secondary',
          variant: 'outlined'
        },
        acceptProps: {
          severity: type === 'danger' ? 'danger' : 'primary'
        },
        accept: () => finalize(true),
        reject: () => finalize(false),
        onHide: () => finalize(false)
      })
    })
  }

  return { confirm }
}
```

```js
// frontend/src/shared/composables/promptState.js
import { reactive } from 'vue'

export const promptState = reactive({
  visible: false,
  title: '请输入',
  message: '',
  value: '',
  type: 'text',
  placeholder: '',
  confirmText: '确定',
  cancelText: '取消',
  error: '',
  validator: null,
  resolver: null
})

export function resetPromptState() {
  promptState.visible = false
  promptState.title = '请输入'
  promptState.message = ''
  promptState.value = ''
  promptState.type = 'text'
  promptState.placeholder = ''
  promptState.confirmText = '确定'
  promptState.cancelText = '取消'
  promptState.error = ''
  promptState.validator = null
  promptState.resolver = null
}

export function openPrompt(options = {}) {
  resetPromptState()
  promptState.visible = true
  promptState.title = options.title ?? '请输入'
  promptState.message = options.message ?? ''
  promptState.value = options.defaultValue ?? ''
  promptState.type = options.type === 'password' ? 'password' : 'text'
  promptState.placeholder = options.placeholder ?? ''
  promptState.confirmText = options.confirmText ?? '确定'
  promptState.cancelText = options.cancelText ?? '取消'
  promptState.validator = options.validator ?? null

  return new Promise((resolve) => {
    promptState.resolver = resolve
  })
}

export function updatePromptValue(value) {
  promptState.value = value
  if (promptState.error) {
    promptState.error = ''
  }
}

export function confirmPrompt() {
  const validationResult = typeof promptState.validator === 'function'
    ? promptState.validator(promptState.value)
    : true

  if (validationResult !== true) {
    promptState.error = typeof validationResult === 'string' ? validationResult : '输入校验未通过'
    return false
  }

  const resolve = promptState.resolver
  const result = promptState.value
  resetPromptState()
  resolve?.(result)
  return true
}

export function cancelPrompt() {
  const resolve = promptState.resolver
  resetPromptState()
  resolve?.(null)
}
```

```js
// frontend/src/shared/composables/usePrompt.js
import { computed } from 'vue'
import {
  cancelPrompt,
  confirmPrompt,
  openPrompt,
  promptState,
  updatePromptValue
} from './promptState'

export function usePrompt() {
  const visible = computed({
    get: () => promptState.visible,
    set: (value) => {
      if (!value && promptState.visible) {
        cancelPrompt()
      }
    }
  })

  return {
    visible,
    title: computed(() => promptState.title),
    message: computed(() => promptState.message),
    inputType: computed(() => promptState.type),
    placeholder: computed(() => promptState.placeholder),
    error: computed(() => promptState.error),
    confirmText: computed(() => promptState.confirmText),
    cancelText: computed(() => promptState.cancelText),
    inputValue: computed({
      get: () => promptState.value,
      set: (value) => updatePromptValue(value)
    }),
    prompt: openPrompt,
    confirm: confirmPrompt,
    cancel: cancelPrompt
  }
}
```

```vue
// frontend/src/shared/components/prime/PrimeConfirmDialog.vue
<script setup>
import ConfirmDialog from 'primevue/confirmdialog'
</script>

<template>
  <ConfirmDialog
    group="app-confirm"
    :draggable="false"
    :breakpoints="{ '960px': '32rem', '640px': 'calc(100vw - 2rem)' }"
    :pt="{
      header: { class: 'px-6 pt-6 pb-0' },
      content: { class: 'px-6 py-4 text-slate-600' },
      footer: { class: 'px-6 pb-6 pt-0 flex justify-end gap-3' },
      mask: { class: 'backdrop-blur-[1px]' }
    }"
  />
</template>
```

```vue
// frontend/src/shared/components/prime/PrimePromptDialog.vue
<script setup>
import { computed } from 'vue'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import PrimeButton from '@/shared/components/prime/PrimeButton.vue'
import { usePrompt } from '@/shared/composables/usePrompt'

const {
  visible,
  title,
  message,
  inputValue,
  inputType,
  placeholder,
  error,
  confirmText,
  cancelText,
  confirm,
  cancel
} = usePrompt()

const isPassword = computed(() => inputType.value === 'password')
</script>

<template>
  <Dialog
    v-model:visible="visible"
    modal
    :header="title"
    :dismissableMask="true"
    :breakpoints="{ '960px': '32rem', '640px': 'calc(100vw - 2rem)' }"
    @hide="cancel"
  >
    <div class="space-y-4">
      <p v-if="message" class="text-sm text-slate-600">{{ message }}</p>

      <Password
        v-if="isPassword"
        v-model="inputValue"
        :feedback="false"
        toggleMask
        fluid
        :invalid="Boolean(error)"
        :placeholder="placeholder"
        @keyup.enter="confirm"
      />
      <InputText
        v-else
        v-model="inputValue"
        fluid
        :invalid="Boolean(error)"
        :placeholder="placeholder"
        @keyup.enter="confirm"
      />

      <p v-if="error" class="text-xs text-red-600">{{ error }}</p>
    </div>

    <template #footer>
      <PrimeButton data-test="prompt-cancel" variant="outline" @click="cancel">
        {{ cancelText }}
      </PrimeButton>
      <PrimeButton data-test="prompt-confirm" @click="confirm">
        {{ confirmText }}
      </PrimeButton>
    </template>
  </Dialog>
</template>
```

```vue
// frontend/src/shared/components/ui/ConfirmDialog.vue
<script setup>
import PrimeConfirmDialog from '@/shared/components/prime/PrimeConfirmDialog.vue'
</script>

<template>
  <PrimeConfirmDialog />
</template>
```

```vue
// frontend/src/shared/components/ui/PromptDialog.vue
<script setup>
import PrimePromptDialog from '@/shared/components/prime/PrimePromptDialog.vue'
</script>

<template>
  <PrimePromptDialog />
</template>
```

```vue
// frontend/src/App.vue
<script setup>
import { RouterView } from 'vue-router'
import Toast from '@/shared/components/ui/Toast.vue'
import ConfirmDialog from '@/shared/components/ui/ConfirmDialog.vue'
import PromptDialog from '@/shared/components/ui/PromptDialog.vue'
</script>

<template>
  <Toast />
  <ConfirmDialog />
  <PromptDialog />
  <RouterView />
</template>
```

```js
// frontend/src/main.js
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import pinia from './core/stores'
import router from './core/router'
import App from './App.vue'
import './style.css'
import { enterpriseTheme } from './shared/theme/primevue-theme'

const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.use(PrimeVue, {
  theme: {
    preset: enterpriseTheme
  }
})
app.use(ToastService)
app.use(ConfirmationService)

app.mount('#app')
```

- [ ] **Step 4: 跑 Confirm / Prompt / App 测试，确认 GREEN**

Run: `cd frontend; npm run test -- --run src/shared/composables/__tests__/useConfirm.test.js src/shared/composables/__tests__/usePrompt.test.js src/App.test.js`
Expected: PASS

- [ ] **Step 5: Checkpoint 当前进度**

将任务表“最近一次更新摘要”改为“Confirm/Prompt 服务与宿主已切到 Prime 实现，进入 T16 最终联验阶段”。

---
### Task 8: 做 T16 总联验并完成任务表更新

**Files:**
- Modify: `项目整改执行任务表.md`
- Test: `frontend/package.json`
- Test: `frontend/src/App.vue`
- Test: `frontend/src/features/user/views/UserList.vue`
- Test: `frontend/src/features/part/views/PartList.vue`
- Test: `frontend/src/features/trace/views/TraceList.vue`
- Test: `frontend/src/features/dashboard/views/Dashboard.vue`

- [ ] **Step 1: 跑全量前端测试**

Run: `cd frontend; npm run test:run`
Expected: PASS，包含 `tokens`、`prime-basic-controls`、`base-basic-controls`、`prime-toast`、`prime-card`、`useConfirm`、`usePrompt`、`App` 全部通过。

- [ ] **Step 2: 跑前端构建**

Run: `cd frontend; npm run build`
Expected: PASS

- [ ] **Step 3: 按页面做人工回归**

Run: `cd frontend; npm run dev`
Expected: Dev Server 正常启动，能进入系统页面。

人工回归清单：
- `UserList.vue`：查询输入框、按钮禁用/加载、重置密码 Prompt、删除 Confirm、列表骨架屏
- `PartList.vue`：筛选按钮、确认删除、消息提示、列表骨架屏
- `TraceList.vue`：查询卡片、消息提示、基础卡片结构、骨架屏
- `Dashboard.vue`：概览卡片、骨架模板、卡片内容区间距是否稳定

- [ ] **Step 4: 更新任务表，正式结束 T16**

将 `项目整改执行任务表.md` 同步更新为：
- “当前进行中任务”改为 `无`，或直接切到下一优先任务
- T16 状态从 `DOING` 改为 `DONE`
- “最近一次更新摘要”写明 Prime 标准层、Base 兼容层、Toast/Confirm/Prompt 服务切换、最小前端测试基础、验证结果
- 在“更新记录”新增一条 T16 完成记录，列出本次新增/修改的核心文件与测试命令

- [ ] **Step 5: 保存最终检查点**

保存本地工作区，确保下个会话可直接从 T17 或其他下一优先任务继续推进。
