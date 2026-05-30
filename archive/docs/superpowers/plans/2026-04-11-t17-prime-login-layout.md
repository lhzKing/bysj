# T17 Prime Login & Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成登录页与主布局的 PrimeVue 风格重构：登录页升级为企业级单卡片入口页，主布局升级为悬浮侧栏 + 分层顶栏 + 统一内容框架，同时保留现有认证/权限逻辑与路由结构。

**Architecture:** 先用 Vitest 锁定 T17 的页面文案、登录提交流程、移动端抽屉与退出登录流程，再实施页面重构。登录页继续使用 T16 产出的 `BaseCard / BaseInput / BaseButton` 作为基础控件层；主布局拆为 `AppSidebarNav / AppTopbar / AppMobileNavDrawer / AppContentFrame` 四个布局组件，`MainLayout.vue` 仅保留导航过滤、标题推导、移动端状态和退出逻辑。视觉层通过 `src/style.css` 中的语义化 shell class 落地，避免继续依赖 Element Plus 容器骨架。

**Tech Stack:** Vue 3、PrimeVue 4、Pinia、Vue Router 4、Vitest、Vue Test Utils、Tailwind CSS、自定义 shell CSS class

---

## File Map

**Create:**
- `frontend/src/shared/components/__tests__/Login.test.js` - 登录页文案、提交流程、错误内联提示回归测试
- `frontend/src/shared/composables/__tests__/useToast.test.js` - Toast 中文 summary 回归测试
- `frontend/src/shared/components/layout/layoutNavigation.js` - 主布局导航定义与 activePath 解析
- `frontend/src/shared/components/layout/AppSidebarNav.vue` - 桌面端悬浮侧栏
- `frontend/src/shared/components/layout/AppTopbar.vue` - 顶栏标题/面包屑/用户信息区
- `frontend/src/shared/components/layout/AppMobileNavDrawer.vue` - 移动端抽屉导航
- `frontend/src/shared/components/layout/AppContentFrame.vue` - 白色大圆角内容承载层
- `frontend/src/shared/components/layout/__tests__/MainLayout.test.js` - 主布局 desktop/mobile/logout 回归测试

**Modify:**
- `frontend/src/shared/components/Login.vue` - 登录页视觉与交互重构
- `frontend/src/shared/composables/useToast.js` - 修正 Toast summary 中文文案
- `frontend/src/style.css` - 登录页与主布局 shell 样式语义层
- `frontend/src/shared/components/layout/MainLayout.vue` - 主布局逻辑收口并接入新子组件
- `项目整改执行任务表.md` - T17 完成后更新状态与验证记录

**Workspace note:** 当前工作区未检测到 `.git`，本计划里的“Checkpoint”步骤统一使用“更新任务表 / 保存当前进度”替代 commit。

---

### Task 1: 锁定登录页与 Toast 的 T17 回归测试

**Files:**
- Create: `frontend/src/shared/components/__tests__/Login.test.js`
- Create: `frontend/src/shared/composables/__tests__/useToast.test.js`
- Test: `frontend/src/shared/components/__tests__/Login.test.js`
- Test: `frontend/src/shared/composables/__tests__/useToast.test.js`

- [ ] **Step 1: 先写登录页回归测试，明确新文案、成功跳转和失败内联提示**

```js
// frontend/src/shared/components/__tests__/Login.test.js
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import { renderWithPrime } from '@/test/renderWithPrime'
import Login from '@/shared/components/Login.vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import BaseCard from '@/shared/components/ui/BaseCard.vue'
import BaseInput from '@/shared/components/ui/BaseInput.vue'

const loginMock = vi.fn()
const pushMock = vi.fn()
const toastSuccessMock = vi.fn()
const toastErrorMock = vi.fn()
const routeMock = { query: { redirect: '/parts' } }

vi.mock('@/core/stores/user', () => ({
  useUserStore: () => ({
    login: loginMock
  })
}))

vi.mock('vue-router', () => ({
  useRoute: () => routeMock,
  useRouter: () => ({
    push: pushMock
  })
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({
    success: toastSuccessMock,
    error: toastErrorMock
  })
}))

describe('Login view', () => {
  beforeEach(() => {
    routeMock.query = { redirect: '/parts' }
    loginMock.mockReset()
    pushMock.mockReset()
    toastSuccessMock.mockReset()
    toastErrorMock.mockReset()
  })

  it('renders the approved enterprise copy and base controls', () => {
    const wrapper = renderWithPrime(Login)

    expect(wrapper.text()).toContain('工业配件供应链溯源系统')
    expect(wrapper.text()).toContain('毕业设计展示 · 企业级业务后台原型')
    expect(wrapper.text()).toContain('演示账号请联系管理员或查看项目说明')
    expect(wrapper.text()).toContain('© 2026 工业配件供应链溯源系统')
    expect(wrapper.find('[data-test="login-card"]').exists()).toBe(true)
    expect(wrapper.findComponent(BaseCard).exists()).toBe(true)
    expect(wrapper.findAllComponents(BaseInput)).toHaveLength(2)
    expect(wrapper.findComponent(BaseButton).exists()).toBe(true)
  })

  it('submits credentials and redirects to the requested route on success', async () => {
    loginMock.mockResolvedValue(true)
    const wrapper = renderWithPrime(Login)

    await wrapper.find('[data-test="login-username"] input').setValue('admin')
    await wrapper.find('[data-test="login-password"] input').setValue('123456')
    await wrapper.find('[data-test="remember-me"]').setValue(true)
    await wrapper.find('form').trigger('submit.prevent')
    await Promise.resolve()
    await nextTick()

    expect(loginMock).toHaveBeenCalledWith('admin', '123456', true)
    expect(pushMock).toHaveBeenCalledWith('/parts')
    expect(toastSuccessMock).toHaveBeenCalledWith('登录成功')
  })

  it('shows inline error feedback when login fails', async () => {
    loginMock.mockRejectedValue(new Error('用户名或密码错误'))
    const wrapper = renderWithPrime(Login)

    await wrapper.find('[data-test="login-username"] input').setValue('admin')
    await wrapper.find('[data-test="login-password"] input').setValue('bad-pass')
    await wrapper.find('form').trigger('submit.prevent')
    await Promise.resolve()
    await nextTick()

    expect(wrapper.find('[data-test="login-error"]').text()).toContain('用户名或密码错误')
    expect(toastErrorMock).toHaveBeenCalledWith('用户名或密码错误')
  })
})
```

- [ ] **Step 2: 再写 Toast 中文 summary 测试，确保 T17 的提示文案不再乱码**

```js
// frontend/src/shared/composables/__tests__/useToast.test.js
import { beforeEach, describe, expect, it } from 'vitest'
import { __resetToastBridge, onToastMessage, useToast } from '@/shared/composables/useToast'

describe('useToast bridge', () => {
  beforeEach(() => {
    __resetToastBridge()
  })

  it('emits readable Chinese summary labels', () => {
    const messages = []
    const stop = onToastMessage((payload) => messages.push(payload))
    const toast = useToast()

    toast.success('保存成功')
    toast.error('保存失败')

    stop()

    expect(messages[0]).toMatchObject({
      severity: 'success',
      summary: '成功',
      detail: '保存成功'
    })
    expect(messages[1]).toMatchObject({
      severity: 'error',
      summary: '错误',
      detail: '保存失败'
    })
  })
})
```

- [ ] **Step 3: 运行新测试，先确认当前实现处于 RED**

Run: `cd frontend; npm run test -- --run src/shared/components/__tests__/Login.test.js src/shared/composables/__tests__/useToast.test.js`
Expected: FAIL；至少会出现以下一种失败：
- 登录页仍显示旧 Element Plus 文案或乱码，无法匹配新标题/副标题
- 当前登录页没有 `data-test="login-error"` 的内联错误区
- `useToast` 的 `summaryMap` 仍是乱码字符串，无法通过中文 summary 断言

- [ ] **Step 4: Checkpoint 当前进度**

更新任务表“最近一次更新摘要”为“已补 T17 登录页 / Toast 回归测试，实施中”，避免中断后丢失测试上下文。

---

### Task 2: 实现登录页重构与 Toast 中文文案修正

**Files:**
- Modify: `frontend/src/shared/components/Login.vue`
- Modify: `frontend/src/shared/composables/useToast.js`
- Modify: `frontend/src/style.css`
- Test: `frontend/src/shared/components/__tests__/Login.test.js`
- Test: `frontend/src/shared/composables/__tests__/useToast.test.js`

- [ ] **Step 1: 先修正 Toast summary 文案，让全局成功/错误提示变成正常中文**

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

- [ ] **Step 2: 用 Base 兼容层重写登录页，保留登录逻辑但替换成 T17 单卡片视觉骨架**

```vue
<!-- frontend/src/shared/components/Login.vue -->
<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/core/stores/user'
import { useToast } from '@/shared/composables/useToast'
import BaseCard from '@/shared/components/ui/BaseCard.vue'
import BaseInput from '@/shared/components/ui/BaseInput.vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const toast = useToast()

const loginForm = ref({
  username: '',
  password: '',
  rememberMe: false
})
const loading = ref(false)
const errorMessage = ref('')

const canSubmit = computed(() =>
  loginForm.value.username.trim().length > 0 && loginForm.value.password.length > 0
)

const handleLogin = async () => {
  if (loading.value || !canSubmit.value) return

  errorMessage.value = ''
  loading.value = true

  try {
    await userStore.login(
      loginForm.value.username,
      loginForm.value.password,
      loginForm.value.rememberMe
    )

    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.push(redirect)
    toast.success('登录成功')
  } catch (error) {
    errorMessage.value = error?.message || '登录失败，请检查用户名和密码后重试。'
    toast.error(errorMessage.value)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-shell" data-test="login-shell">
    <BaseCard class="auth-card w-full max-w-[28rem]" data-test="login-card">
      <div class="auth-card__body">
        <div class="space-y-3 text-center">
          <p class="auth-badge">毕业设计展示</p>
          <div class="space-y-2">
            <h1 class="auth-title">工业配件供应链溯源系统</h1>
            <p class="auth-subtitle">毕业设计展示 · 企业级业务后台原型</p>
          </div>
        </div>

        <form class="space-y-5" @submit.prevent="handleLogin">
          <BaseInput
            v-model="loginForm.username"
            inputId="login-username"
            label="用户名"
            placeholder="请输入用户名"
            :icon="User"
            data-test="login-username"
          />

          <BaseInput
            v-model="loginForm.password"
            inputId="login-password"
            type="password"
            label="密码"
            placeholder="请输入密码"
            :icon="Lock"
            data-test="login-password"
          />

          <label class="auth-checkbox" for="remember-me">
            <input id="remember-me" v-model="loginForm.rememberMe" data-test="remember-me" type="checkbox" />
            <span>记住登录状态</span>
          </label>

          <p v-if="errorMessage" class="auth-error" data-test="login-error">
            {{ errorMessage }}
          </p>

          <BaseButton
            type="submit"
            size="lg"
            block
            :loading="loading"
            :disabled="!canSubmit"
            data-test="login-submit"
          >
            登录系统
          </BaseButton>
        </form>

        <div class="auth-footer">
          <p>演示账号请联系管理员或查看项目说明</p>
          <p>© 2026 工业配件供应链溯源系统</p>
        </div>
      </div>
    </BaseCard>
  </div>
</template>
```

- [ ] **Step 3: 在全局样式里补登录页语义 class，落地 T17 的渐变背景、玻璃感白卡和错误提示区**

```css
/* frontend/src/style.css：保留原有 import / tailwind 指令，在文件底部追加以下内容 */

@layer base {
  :root {
    --shell-brand-strong: #0F4C81;
    --shell-primary: #1890FF;
    --shell-bg: #F8FAFC;
    --shell-border: rgba(148, 163, 184, 0.18);
    --shell-shadow-soft: 0 10px 40px -10px rgba(15, 23, 42, 0.08);
  }

  html,
  body,
  #app {
    min-height: 100%;
  }

  body {
    @apply text-slate-900 antialiased font-sans;
    background-color: var(--shell-bg);
  }
}

@layer components {
  .auth-shell {
    @apply relative flex min-h-screen items-center justify-center overflow-hidden px-4 py-10;
    background:
      radial-gradient(circle at top, rgba(24, 144, 255, 0.18), transparent 34%),
      linear-gradient(135deg, #f8fafc 0%, #eef4ff 50%, #f8fafc 100%);
  }

  .auth-shell::before {
    content: '';
    position: absolute;
    inset: 0;
    pointer-events: none;
    background-image:
      linear-gradient(rgba(15, 76, 129, 0.05) 1px, transparent 1px),
      linear-gradient(90deg, rgba(15, 76, 129, 0.05) 1px, transparent 1px);
    background-size: 32px 32px;
    mask-image: linear-gradient(to bottom, transparent, black 15%, black 85%, transparent);
  }

  .auth-card.prime-base-card {
    border-radius: 24px;
    border: 1px solid rgba(255, 255, 255, 0.8);
    background: rgba(255, 255, 255, 0.86);
    backdrop-filter: blur(16px);
    box-shadow: var(--shell-shadow-soft);
  }

  .auth-card__body {
    @apply space-y-6 px-7 py-8;
  }

  .auth-badge {
    @apply inline-flex items-center rounded-full bg-slate-900/5 px-3 py-1 text-xs font-medium text-slate-600;
  }

  .auth-title {
    @apply text-3xl font-semibold tracking-tight;
    color: var(--shell-brand-strong);
  }

  .auth-subtitle {
    @apply text-sm leading-6 text-slate-500;
  }

  .auth-checkbox {
    @apply inline-flex items-center gap-3 text-sm text-slate-600;
  }

  .auth-checkbox input {
    accent-color: var(--shell-primary);
  }

  .auth-error {
    @apply rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700;
  }

  .auth-footer {
    @apply space-y-2 pt-2 text-center text-xs text-slate-400;
  }
}
```

- [ ] **Step 4: 回跑登录页与 Toast 测试，确认转为 GREEN**

Run: `cd frontend; npm run test -- --run src/shared/components/__tests__/Login.test.js src/shared/composables/__tests__/useToast.test.js`
Expected: PASS；登录页 3 个断言与 Toast 中文 summary 测试全部通过。

- [ ] **Step 5: Checkpoint 当前进度**

更新任务表“最近一次更新摘要”为“已完成 T17 登录页重构与 Toast 文案修正，主布局待改造”。

---

### Task 3: 锁定主布局 desktop / mobile / logout 行为回归测试

**Files:**
- Create: `frontend/src/shared/components/layout/__tests__/MainLayout.test.js`
- Test: `frontend/src/shared/components/layout/__tests__/MainLayout.test.js`

- [ ] **Step 1: 先写 MainLayout 集成测试，锁定导航过滤、移动端抽屉和退出登录链路**

```js
// frontend/src/shared/components/layout/__tests__/MainLayout.test.js
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive } from 'vue'
import { renderWithPrime } from '@/test/renderWithPrime'
import MainLayout from '@/shared/components/layout/MainLayout.vue'

const routeMock = reactive({
  path: '/',
  meta: { title: '仪表盘' }
})

const pushMock = vi.fn()
const replaceMock = vi.fn()
const logoutMock = vi.fn()
const confirmMock = vi.fn()
const toastSuccessMock = vi.fn()

const grantedPermissions = reactive({
  values: ['dashboard:view', 'trace:view', 'part:view']
})

function setMobile(matches) {
  window.matchMedia = vi.fn().mockImplementation(() => ({
    matches,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn()
  }))
}

vi.mock('vue-router', () => ({
  useRoute: () => routeMock,
  useRouter: () => ({
    push: pushMock,
    replace: replaceMock
  })
}))

vi.mock('@/core/stores/user', () => ({
  useUserStore: () => ({
    user: {
      username: 'demo-admin'
    },
    logout: logoutMock,
    hasAnyPermission: (requiredPermissions = []) =>
      requiredPermissions.some((permission) => grantedPermissions.values.includes(permission))
  })
}))

vi.mock('@/shared/composables/useConfirm', () => ({
  useConfirm: () => ({
    confirm: confirmMock
  })
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({
    success: toastSuccessMock
  })
}))

const routerViewStub = {
  template: '<slot :Component="{ template: `<div data-test=\"layout-view\">Layout page</div>` }" />'
}

describe('MainLayout', () => {
  beforeEach(() => {
    routeMock.path = '/'
    routeMock.meta = { title: '仪表盘' }
    grantedPermissions.values = ['dashboard:view', 'trace:view', 'part:view']
    pushMock.mockReset()
    replaceMock.mockReset()
    logoutMock.mockReset()
    confirmMock.mockReset()
    toastSuccessMock.mockReset()
    setMobile(false)
  })

  it('renders the desktop shell with filtered navigation and current title', () => {
    const wrapper = renderWithPrime(MainLayout, {
      global: {
        stubs: {
          RouterView: routerViewStub
        }
      }
    })

    expect(wrapper.find('[data-test="app-shell"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="desktop-sidebar"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="page-title"]').text()).toContain('仪表盘')
    expect(wrapper.text()).toContain('溯源管理')
    expect(wrapper.text()).toContain('配件管理')
    expect(wrapper.text()).not.toContain('用户管理')
  })

  it('opens the mobile drawer and closes it after navigation', async () => {
    setMobile(true)
    const wrapper = renderWithPrime(MainLayout, {
      global: {
        stubs: {
          RouterView: routerViewStub
        }
      }
    })

    await wrapper.find('[data-test="mobile-nav-toggle"]').trigger('click')
    expect(wrapper.find('[data-test="mobile-drawer"]').exists()).toBe(true)

    await wrapper.find('[data-nav-path="/traces"]').trigger('click')

    expect(pushMock).toHaveBeenCalledWith('/traces')
    expect(wrapper.find('[data-test="mobile-drawer"]').exists()).toBe(false)
  })

  it('confirms before logging out and redirects back to /login', async () => {
    confirmMock.mockResolvedValue(true)
    const wrapper = renderWithPrime(MainLayout, {
      global: {
        stubs: {
          RouterView: routerViewStub
        }
      }
    })

    await wrapper.find('[data-test="logout-action"]').trigger('click')
    await Promise.resolve()

    expect(confirmMock).toHaveBeenCalledWith(expect.objectContaining({
      title: '退出登录',
      message: '确定要退出当前账号吗？'
    }))
    expect(logoutMock).toHaveBeenCalled()
    expect(replaceMock).toHaveBeenCalledWith('/login')
    expect(toastSuccessMock).toHaveBeenCalledWith('已退出登录')
  })
})
```

- [ ] **Step 2: 运行 MainLayout 测试，确认当前布局仍处于 RED**

Run: `cd frontend; npm run test -- --run src/shared/components/layout/__tests__/MainLayout.test.js`
Expected: FAIL；当前 `MainLayout.vue` 仍使用 `el-container / el-menu / el-drawer / ElMessageBox`，无法满足新的 `data-test` 结构、移动端抽屉流程和 composable 退出确认链路。

- [ ] **Step 3: Checkpoint 当前进度**

更新任务表“最近一次更新摘要”为“已补 T17 主布局回归测试，等待布局子组件与主壳重构”。

---

### Task 4: 落地主布局拆分与新的 app shell 视觉骨架

**Files:**
- Create: `frontend/src/shared/components/layout/layoutNavigation.js`
- Create: `frontend/src/shared/components/layout/AppSidebarNav.vue`
- Create: `frontend/src/shared/components/layout/AppTopbar.vue`
- Create: `frontend/src/shared/components/layout/AppMobileNavDrawer.vue`
- Create: `frontend/src/shared/components/layout/AppContentFrame.vue`
- Modify: `frontend/src/shared/components/layout/MainLayout.vue`
- Modify: `frontend/src/style.css`
- Test: `frontend/src/shared/components/layout/__tests__/MainLayout.test.js`

- [ ] **Step 1: 先抽出导航定义和 activePath 解析，避免布局子组件重复拼接菜单数据**

```js
// frontend/src/shared/components/layout/layoutNavigation.js
import {
  Document,
  Box,
  Search,
  User,
  Lock,
  Camera
} from '@element-plus/icons-vue'
import { PERMISSIONS } from '@/shared/constants'

export const layoutNavigation = [
  { name: '仪表盘', path: '/', icon: Document, permissions: [PERMISSIONS.DASHBOARD.VIEW] },
  {
    name: '扫码中心',
    path: '/scan',
    icon: Camera,
    permissions: [
      PERMISSIONS.TRACE.VIEW,
      PERMISSIONS.TRACE.CREATE,
      PERMISSIONS.TRACE.SCAN,
      PERMISSIONS.TRACE.INBOUND,
      PERMISSIONS.TRACE.OUTBOUND,
      PERMISSIONS.TRACE.TRANSFER
    ]
  },
  { name: '溯源管理', path: '/traces', icon: Search, permissions: [PERMISSIONS.TRACE.VIEW] },
  { name: '用户管理', path: '/users', icon: User, permissions: [PERMISSIONS.USER.VIEW] },
  { name: '角色管理', path: '/roles', icon: Lock, permissions: [PERMISSIONS.ROLE.VIEW] },
  { name: '配件管理', path: '/parts', icon: Box, permissions: [PERMISSIONS.PART.VIEW] }
]

export function resolveActivePath(currentPath) {
  if (!currentPath || currentPath === '/') return '/'

  const nestedMatch = layoutNavigation.find(
    (item) => item.path !== '/' && (currentPath === item.path || currentPath.startsWith(`${item.path}/`))
  )

  return nestedMatch?.path || currentPath
}
```

- [ ] **Step 2: 创建桌面侧栏与顶栏组件，把视觉骨架从 MainLayout 模板里拆出去**

```vue
<!-- frontend/src/shared/components/layout/AppSidebarNav.vue -->
<script setup>
import { computed } from 'vue'

const props = defineProps({
  items: {
    type: Array,
    default: () => []
  },
  activePath: {
    type: String,
    default: '/'
  },
  username: {
    type: String,
    default: '用户'
  }
})

const emit = defineEmits(['navigate', 'logout'])

const userInitial = computed(() => props.username.slice(0, 1).toUpperCase())
</script>

<template>
  <aside class="app-sidebar-shell" data-test="desktop-sidebar">
    <div class="app-sidebar-shell__brand">
      <p class="app-sidebar-shell__eyebrow">毕业设计展示</p>
      <h1 class="app-sidebar-shell__title">工业配件供应链溯源系统</h1>
      <p class="app-sidebar-shell__subtitle">企业级业务后台原型</p>
    </div>

    <nav class="app-sidebar-shell__nav" aria-label="主导航">
      <button
        v-for="item in items"
        :key="item.path"
        :data-nav-path="item.path"
        type="button"
        class="app-nav-item"
        :class="{ 'is-active': item.path === activePath }"
        @click="emit('navigate', item.path)"
      >
        <component :is="item.icon" class="h-4 w-4 shrink-0" />
        <span>{{ item.name }}</span>
      </button>
    </nav>

    <div class="app-sidebar-shell__footer">
      <div class="app-user-chip">
        <span class="app-user-chip__avatar">{{ userInitial }}</span>
        <div>
          <p class="app-user-chip__name">{{ username }}</p>
          <p class="app-user-chip__meta">当前登录用户</p>
        </div>
      </div>

      <button type="button" class="app-secondary-action" data-test="logout-action" @click="emit('logout')">
        退出登录
      </button>
    </div>
  </aside>
</template>
```

```vue
<!-- frontend/src/shared/components/layout/AppTopbar.vue -->
<script setup>
const props = defineProps({
  title: {
    type: String,
    default: '仪表盘'
  },
  eyebrow: {
    type: String,
    default: '系统工作台'
  },
  username: {
    type: String,
    default: '用户'
  },
  isMobile: Boolean
})

const emit = defineEmits(['toggle-menu'])
</script>

<template>
  <header class="app-topbar" data-test="app-topbar">
    <div class="flex items-center gap-3">
      <button
        v-if="isMobile"
        type="button"
        class="app-icon-button"
        data-test="mobile-nav-toggle"
        @click="emit('toggle-menu')"
      >
        ≡
      </button>

      <div class="space-y-1">
        <p class="app-topbar__eyebrow">{{ eyebrow }}</p>
        <h2 class="app-topbar__title" data-test="page-title">{{ title }}</h2>
      </div>
    </div>

    <div class="app-topbar__actions">
      <span class="app-topbar__meta">当前用户：{{ username }}</span>
    </div>
  </header>
</template>
```

- [ ] **Step 3: 创建移动端抽屉导航与统一内容承载组件**

```vue
<!-- frontend/src/shared/components/layout/AppMobileNavDrawer.vue -->
<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: Boolean,
  items: {
    type: Array,
    default: () => []
  },
  activePath: {
    type: String,
    default: '/'
  },
  username: {
    type: String,
    default: '用户'
  }
})

const emit = defineEmits(['close', 'navigate', 'logout'])

const userInitial = computed(() => props.username.slice(0, 1).toUpperCase())
</script>

<template>
  <teleport to="body">
    <transition name="app-drawer-fade">
      <div v-if="visible" class="app-mobile-drawer" data-test="mobile-drawer">
        <button type="button" class="app-mobile-drawer__backdrop" @click="emit('close')" />

        <aside class="app-mobile-drawer__panel">
          <div class="app-mobile-drawer__header">
            <div>
              <p class="app-sidebar-shell__eyebrow">毕业设计展示</p>
              <h2 class="app-sidebar-shell__title">工业配件供应链溯源系统</h2>
            </div>

            <button type="button" class="app-icon-button" data-test="drawer-close" @click="emit('close')">
              ×
            </button>
          </div>

          <nav class="app-sidebar-shell__nav" aria-label="移动端主导航">
            <button
              v-for="item in items"
              :key="item.path"
              :data-nav-path="item.path"
              type="button"
              class="app-nav-item"
              :class="{ 'is-active': item.path === activePath }"
              @click="emit('navigate', item.path)"
            >
              <component :is="item.icon" class="h-4 w-4 shrink-0" />
              <span>{{ item.name }}</span>
            </button>
          </nav>

          <div class="app-mobile-drawer__footer">
            <div class="app-user-chip">
              <span class="app-user-chip__avatar">{{ userInitial }}</span>
              <div>
                <p class="app-user-chip__name">{{ username }}</p>
                <p class="app-user-chip__meta">当前登录用户</p>
              </div>
            </div>

            <button type="button" class="app-secondary-action" @click="emit('logout')">
              退出登录
            </button>
          </div>
        </aside>
      </div>
    </transition>
  </teleport>
</template>
```

```vue
<!-- frontend/src/shared/components/layout/AppContentFrame.vue -->
<template>
  <main class="app-content-frame" data-test="content-frame">
    <div class="app-content-frame__body">
      <slot />
    </div>
  </main>
</template>
```

- [ ] **Step 4: 重写 MainLayout.vue，只保留状态编排与退出登录逻辑，不再直接使用 Element Plus 布局容器**

```vue
<!-- frontend/src/shared/components/layout/MainLayout.vue -->
<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/core/stores/user'
import { useConfirm } from '@/shared/composables/useConfirm'
import { useToast } from '@/shared/composables/useToast'
import { layoutNavigation, resolveActivePath } from './layoutNavigation'
import AppSidebarNav from './AppSidebarNav.vue'
import AppTopbar from './AppTopbar.vue'
import AppMobileNavDrawer from './AppMobileNavDrawer.vue'
import AppContentFrame from './AppContentFrame.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { confirm } = useConfirm()
const toast = useToast()

const drawerVisible = ref(false)
const isMobile = ref(false)
let mediaQuery = null

const navigation = computed(() =>
  layoutNavigation.filter((item) => userStore.hasAnyPermission(item.permissions))
)
const activePath = computed(() => resolveActivePath(route.path))
const currentTitle = computed(() => route.meta?.title || navigation.value.find((item) => item.path === activePath.value)?.name || '仪表盘')
const currentEyebrow = computed(() => navigation.value.find((item) => item.path === activePath.value)?.name || '系统工作台')
const username = computed(() => userStore.user?.username || '用户')

function syncViewport() {
  if (!mediaQuery) return
  isMobile.value = mediaQuery.matches

  if (!isMobile.value) {
    drawerVisible.value = false
  }
}

async function handleNavigate(path) {
  drawerVisible.value = false

  if (path !== route.path) {
    await router.push(path)
  }
}

async function handleLogout() {
  const accepted = await confirm({
    title: '退出登录',
    message: '确定要退出当前账号吗？',
    confirmText: '退出',
    cancelText: '取消',
    type: 'danger'
  })

  if (!accepted) return

  await userStore.logout()
  await router.replace('/login')
  toast.success('已退出登录')
}

onMounted(() => {
  if (typeof window === 'undefined') return

  mediaQuery = window.matchMedia('(max-width: 767px)')
  syncViewport()
  mediaQuery.addEventListener('change', syncViewport)
})

onUnmounted(() => {
  mediaQuery?.removeEventListener('change', syncViewport)
})
</script>

<template>
  <div class="app-shell" data-test="app-shell">
    <div class="app-shell__grid">
      <AppSidebarNav
        v-if="!isMobile"
        :items="navigation"
        :active-path="activePath"
        :username="username"
        @navigate="handleNavigate"
        @logout="handleLogout"
      />

      <div class="app-shell__main">
        <AppTopbar
          :title="currentTitle"
          :eyebrow="currentEyebrow"
          :username="username"
          :is-mobile="isMobile"
          @toggle-menu="drawerVisible = true"
        />

        <AppContentFrame>
          <router-view v-slot="{ Component }">
            <transition name="app-shell-fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </AppContentFrame>
      </div>
    </div>

    <AppMobileNavDrawer
      :visible="drawerVisible"
      :items="navigation"
      :active-path="activePath"
      :username="username"
      @close="drawerVisible = false"
      @navigate="handleNavigate"
      @logout="handleLogout"
    />
  </div>
</template>
```

- [ ] **Step 5: 为 app shell 追加布局语义样式，落地悬浮侧栏、分层顶栏、白色内容容器和移动端抽屉**

```css
/* frontend/src/style.css：在 Task 2 的 auth 样式后继续追加 */

@layer components {
  .app-shell {
    @apply min-h-screen p-3 md:p-4;
    background:
      radial-gradient(circle at top right, rgba(24, 144, 255, 0.12), transparent 28%),
      linear-gradient(180deg, #f8fafc 0%, #eef4ff 100%);
  }

  .app-shell__grid {
    @apply mx-auto flex min-h-[calc(100vh-1.5rem)] max-w-[1600px] gap-4;
  }

  .app-shell__main {
    @apply flex min-w-0 flex-1 flex-col gap-4;
  }

  .app-sidebar-shell {
    @apply hidden w-[264px] shrink-0 rounded-[16px] border bg-white/92 p-4 lg:flex lg:flex-col;
    border-color: var(--shell-border);
    box-shadow: var(--shell-shadow-soft);
  }

  .app-sidebar-shell__brand {
    @apply mb-6 space-y-2 border-b border-slate-100 pb-5;
  }

  .app-sidebar-shell__eyebrow {
    @apply text-xs font-medium uppercase tracking-[0.16em] text-slate-400;
  }

  .app-sidebar-shell__title {
    @apply text-lg font-semibold leading-6;
    color: var(--shell-brand-strong);
  }

  .app-sidebar-shell__subtitle {
    @apply text-sm text-slate-500;
  }

  .app-sidebar-shell__nav {
    @apply flex flex-1 flex-col gap-2;
  }

  .app-sidebar-shell__footer,
  .app-mobile-drawer__footer {
    @apply mt-6 space-y-4 border-t border-slate-100 pt-4;
  }

  .app-nav-item {
    @apply flex items-center gap-3 rounded-2xl px-4 py-3 text-sm font-medium text-slate-600 transition-all duration-200;
  }

  .app-nav-item:hover {
    background: rgba(24, 144, 255, 0.08);
    color: var(--shell-brand-strong);
  }

  .app-nav-item.is-active {
    background: linear-gradient(135deg, rgba(24, 144, 255, 0.15), rgba(15, 76, 129, 0.08));
    color: var(--shell-brand-strong);
    box-shadow: inset 0 0 0 1px rgba(24, 144, 255, 0.08);
  }

  .app-secondary-action {
    @apply inline-flex w-full items-center justify-center rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-700 transition-colors;
  }

  .app-secondary-action:hover {
    @apply border-slate-300 bg-slate-50;
  }

  .app-user-chip {
    @apply flex items-center gap-3;
  }

  .app-user-chip__avatar {
    @apply inline-flex h-10 w-10 items-center justify-center rounded-full bg-slate-900 text-sm font-semibold text-white;
  }

  .app-user-chip__name {
    @apply text-sm font-medium text-slate-800;
  }

  .app-user-chip__meta {
    @apply text-xs text-slate-500;
  }

  .app-topbar {
    @apply flex items-center justify-between rounded-[20px] border bg-white/86 px-4 py-4 md:px-6;
    border-color: var(--shell-border);
    box-shadow: var(--shell-shadow-soft);
    backdrop-filter: blur(14px);
  }

  .app-topbar__eyebrow {
    @apply text-xs font-medium uppercase tracking-[0.14em] text-slate-400;
  }

  .app-topbar__title {
    @apply text-2xl font-semibold tracking-tight text-slate-900;
  }

  .app-topbar__actions {
    @apply flex items-center gap-3;
  }

  .app-topbar__meta {
    @apply hidden text-sm text-slate-500 md:inline;
  }

  .app-icon-button {
    @apply inline-flex h-10 w-10 items-center justify-center rounded-full border border-slate-200 bg-white text-slate-600 transition-colors;
  }

  .app-icon-button:hover {
    @apply border-slate-300 bg-slate-50 text-slate-800;
  }

  .app-content-frame {
    @apply flex-1 rounded-[24px] border bg-white/94;
    border-color: var(--shell-border);
    box-shadow: var(--shell-shadow-soft);
  }

  .app-content-frame__body {
    @apply h-full min-h-[calc(100vh-10rem)] overflow-x-hidden overflow-y-auto p-4 md:p-6 lg:p-8;
  }

  .app-mobile-drawer {
    @apply fixed inset-0 z-50 flex;
  }

  .app-mobile-drawer__backdrop {
    @apply absolute inset-0 bg-slate-900/35;
  }

  .app-mobile-drawer__panel {
    @apply relative flex h-full w-[min(22rem,calc(100vw-2rem))] flex-col gap-4 rounded-r-[24px] bg-white px-5 py-5;
    box-shadow: 0 16px 48px -12px rgba(15, 23, 42, 0.18);
  }

  .app-mobile-drawer__header {
    @apply flex items-start justify-between gap-3 border-b border-slate-100 pb-4;
  }
}

.app-shell-fade-enter-active,
.app-shell-fade-leave-active,
.app-drawer-fade-enter-active,
.app-drawer-fade-leave-active {
  transition: opacity 180ms ease, transform 180ms ease;
}

.app-shell-fade-enter-from,
.app-shell-fade-leave-to,
.app-drawer-fade-enter-from,
.app-drawer-fade-leave-to {
  opacity: 0;
  transform: translateY(4px);
}
```

- [ ] **Step 6: 回跑 MainLayout 测试，确认主布局由 RED 转 GREEN**

Run: `cd frontend; npm run test -- --run src/shared/components/layout/__tests__/MainLayout.test.js`
Expected: PASS；desktop shell、mobile drawer、logout flow 三个测试全部通过。

- [ ] **Step 7: Checkpoint 当前进度**

更新任务表“最近一次更新摘要”为“已完成 T17 主布局拆分与 shell 重构，待做全量验证”。

---

### Task 5: 做 T17 全量验证与任务表收口

**Files:**
- Modify: `项目整改执行任务表.md`
- Test: `frontend/src/shared/components/__tests__/Login.test.js`
- Test: `frontend/src/shared/composables/__tests__/useToast.test.js`
- Test: `frontend/src/shared/components/layout/__tests__/MainLayout.test.js`

- [ ] **Step 1: 先跑 T17 的定向测试集合，确认登录页 / Toast / 主布局三条主线都稳定**

Run: `cd frontend; npm run test -- --run src/shared/components/__tests__/Login.test.js src/shared/composables/__tests__/useToast.test.js src/shared/components/layout/__tests__/MainLayout.test.js`
Expected: PASS；T17 新增测试全部通过。

- [ ] **Step 2: 再跑完整前端测试，避免影响 T16 已完成的基础 UI 组件层**

Run: `cd frontend; npm run test:run`
Expected: PASS；现有 `App / prime / ui / composables / theme` 测试与 T17 新增测试全部通过。

- [ ] **Step 3: 跑一次构建，确认登录页和主布局的样式改造没有破坏生产构建**

Run: `cd frontend; npm run build`
Expected: PASS；Vite 正常输出生产包，无新的构建错误。

- [ ] **Step 4: 启动本地 dev server 做一次视觉烟雾回归**

Run: `cd frontend; npm run dev`
Expected: Vite 启动成功，并打印本地访问地址（通常为 `http://127.0.0.1:5173/` 或相近端口）。

检查清单：
- `/login` 页面为单卡片居中、背景有轻层次、中文无乱码
- 登录按钮、输入框、记住登录状态均为新风格
- 进入 `/` 后桌面端显示悬浮侧栏、分层顶栏、统一白色内容容器
- 将视口缩到 `< 768px` 时出现移动端抽屉导航，菜单按钮与抽屉可正常开合
- 退出登录仍会弹确认，再返回 `/login`

- [ ] **Step 5: 完成后更新任务表，把 T17 状态切到 DONE，并推荐下一个任务为 T18**

```md
<!-- 项目整改执行任务表.md：完成后按以下内容更新 -->

- 当前任务编号：无
- 当前任务名称：无
- 当前状态：无
- 开始时间：无
- 当前执行人/会话说明：Codex 当前会话（T17 已完成，等待切换下一任务）
- 当前目标：等待切换到下一优先任务
- 当前阻塞点：无
- 下一步动作：执行 T18 重构后台管理页为 PrimeVue 风格

- 最近更新日期：2026-04-11
- 最近完成任务：T17 重构登录页与主布局为 PrimeVue 风格（完成）
- 最近修改文件：`frontend/src/shared/components/Login.vue`、`frontend/src/shared/components/layout/MainLayout.vue`、`frontend/src/shared/components/layout/*`、`frontend/src/style.css`、`项目整改执行任务表.md`
- 最近验证结果：`cd frontend && npm run test:run` 已通过，`cd frontend && npm run build` 已通过，登录页与主布局桌面/移动端视觉回归通过
- 当前整体进度：16 / 29（T17 已完成）
- 下一推荐任务：T18 重构后台管理页为 PrimeVue 风格
| T17 | 重构登录页与主布局为 PrimeVue 风格 | P1 | DONE | 已完成登录页与主布局 PrimeVue 风格重构 |
```

- [ ] **Step 6: 最终 Checkpoint**

确认任务表中：
- `T17` 行状态已改为 `DONE`
- “当前进行中任务” 已清空
- “最近一次更新摘要” 已写入 T17 的测试与构建结果

---

## Plan Self-Check Map

- 登录页文案 / 单卡片结构 / 内联错误提示：Task 1 + Task 2
- Toast 中文文案修正：Task 1 + Task 2
- 主布局拆分边界：Task 3 + Task 4
- 桌面端悬浮侧栏 / 分层顶栏 / 内容框：Task 4
- 移动端抽屉：Task 3 + Task 4
- 退出登录确认与跳转：Task 3 + Task 4
- 自动化验证 + 构建验证 + 视觉烟雾回归：Task 5

