<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Clock, Download } from 'lucide-vue-next'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import BaseInput from '@/shared/components/ui/BaseInput.vue'
import { useUserStore } from '@/core/stores/user'
import { usePrompt } from '@/shared/composables/usePrompt'
import { useToast } from '@/shared/composables/useToast'
import { resolveAccessibleRoute } from '@/core/router/access'

const FAILURE_THRESHOLD = 3
const LOCKOUT_SECONDS = 5

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const toast = useToast()
const { prompt } = usePrompt()

const username = ref('')
const password = ref('')
const rememberMe = ref(false)
const errorMessage = ref('')
const loading = ref(false)
const failureCount = ref(0)
const lockoutSeconds = ref(0)
let lockoutTimer = null

const lockoutActive = computed(() => lockoutSeconds.value > 0)

const canSubmit = computed(() =>
  username.value.trim().length > 0 &&
  password.value.length > 0 &&
  !loading.value &&
  !lockoutActive.value
)

const submitLabel = computed(() =>
  lockoutActive.value ? `请等待 ${lockoutSeconds.value}s 后重试` : '登录'
)

const stopLockout = () => {
  if (lockoutTimer) {
    clearInterval(lockoutTimer)
    lockoutTimer = null
  }
}

const startLockout = () => {
  stopLockout()
  lockoutSeconds.value = LOCKOUT_SECONDS
  lockoutTimer = setInterval(() => {
    lockoutSeconds.value -= 1
    if (lockoutSeconds.value <= 0) {
      stopLockout()
      failureCount.value = 0
    }
  }, 1000)
}

const mapErrorToMessage = (error) => {
  if (!error) return '登录失败，请稍后重试'
  if (error.request && !error.response) {
    return '网络连接失败，请检查网络后重试'
  }
  const status = error.response?.status
  if (status === 401) return '用户名或密码错误'
  if (status === 403) return '账号已禁用，请联系管理员'
  if (status === 429) return '登录尝试过于频繁，请稍后再试'
  return error.message || '登录失败，请稍后重试'
}

const handleSubmit = async () => {
  if (!canSubmit.value) return
  loading.value = true
  errorMessage.value = ''

  try {
    await userStore.login(username.value.trim(), password.value, rememberMe.value)
  } catch (error) {
    errorMessage.value = mapErrorToMessage(error)
    failureCount.value += 1
    if (failureCount.value >= FAILURE_THRESHOLD) {
      startLockout()
    }
    if (!error?.response && !error?.request && !error?.toastShown) {
      toast.error(errorMessage.value)
    }
    loading.value = false
    return
  }

  failureCount.value = 0
  try {
    // 落地优先级：URL 上的 redirect → 用户能进的第一个页面 → /login（兜底；route guard 会 logout 再回来带 no-access）
    const fallback = resolveAccessibleRoute(userStore) || '/login'
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : fallback
    await router.push(redirect)
    toast.success('登录成功')
  } finally {
    loading.value = false
  }
}

const openAuditVerify = async () => {
  const code = await prompt({
    title: '追溯码自助验签',
    message: '输入追溯码即可在无需登录的情况下查看链上签名与节点完整性。',
    confirmText: '验签',
    cancelText: '取消',
    placeholder: 'TC-260505-A8F3K2'
  })
  if (!code) return
  await router.push({ path: `/public/traces/${code.trim()}` })
}

const handleContactAdmin = () => {
  // 演示系统没接邮件 / 工单，用 toast 给出明确指引；
  // 用 info 级别避免被误读为"登录出错"
  toast.info('请联系系统管理员开通账号（默认超级管理员账号：superadmin）')
}

onMounted(() => {
  // 路由守卫检测到"登录态但无任何可访问页面"时会把人踢回来并带 ?error=no-access
  if (route.query.error === 'no-access') {
    errorMessage.value = '当前账号没有任何可访问的页面，请联系管理员分配权限'
  }
})

onUnmounted(() => {
  stopLockout()
})
</script>

<template>
  <div class="login-page">
    <header class="login-header">
      <a class="login-brand">
        <span class="login-brand__logo" aria-hidden="true">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.4" stroke-linecap="round">
            <path d="M3 9h18M3 15h18M9 3v18M15 3v18" />
          </svg>
        </span>
        <span class="login-brand__text">trace.</span>
      </a>
    </header>

    <main class="login-main">
      <div class="login-shell">
        <div class="login-headline">
          <h1 class="login-title">登录到 trace.</h1>
          <p class="login-subtitle">输入工号与密码访问溯源系统</p>
        </div>

        <form class="login-card" novalidate @submit.prevent="handleSubmit">
          <div data-test="login-username">
            <BaseInput
              v-model="username"
              input-id="login-username"
              label="工号 / 用户名"
              type="text"
              placeholder="superadmin"
              autocomplete="username"
              autofocus
              :disabled="loading || lockoutActive"
              name="username"
            />
          </div>

          <div data-test="login-password">
            <BaseInput
              v-model="password"
              input-id="login-password"
              label="密码"
              type="password"
              placeholder="••••••••"
              autocomplete="current-password"
              :disabled="loading || lockoutActive"
              name="password"
            />
          </div>

          <label class="login-remember">
            <input
              v-model="rememberMe"
              type="checkbox"
              data-test="remember-me"
              :disabled="loading || lockoutActive"
              class="login-remember__input"
            />
            <span class="login-remember__box" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                <path d="M5 13l4 4L19 7" />
              </svg>
            </span>
            <span>14 天内保持登录</span>
          </label>

          <Transition name="login-error-fade">
            <div
              v-if="errorMessage"
              class="login-error"
              data-test="login-error"
              role="alert"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <circle cx="12" cy="12" r="9" />
                <path d="M12 8v4M12 16h.01" />
              </svg>
              <span>{{ errorMessage }}</span>
            </div>
          </Transition>

          <BaseButton
            type="submit"
            variant="primary"
            size="md"
            block
            :loading="loading"
            :disabled="!canSubmit"
            data-test="login-submit"
            class="login-submit"
          >
            {{ submitLabel }}
          </BaseButton>
        </form>

        <p class="login-footnote">
          没有账号？<button
            type="button"
            class="login-link login-link--accent"
            data-test="login-contact-admin"
            @click="handleContactAdmin"
          >联系管理员注册</button>
        </p>

        <section class="login-audit" aria-label="外部审计入口">
          <div class="login-audit__eyebrow">外部审计 · 无需登录</div>
          <div class="login-audit__actions">
            <button
              type="button"
              class="login-link login-link--muted login-audit__action"
              data-test="audit-verify"
              @click="openAuditVerify"
            >
              <Clock :size="13" :stroke-width="2" />
              通过追溯码自助验签
            </button>
            <span class="login-audit__divider" aria-hidden="true">·</span>
            <a
              class="login-link login-link--muted login-audit__action"
              href="/api/traces/public-key"
              target="_blank"
              rel="noopener"
              data-test="audit-public-key"
            >
              <Download :size="13" :stroke-width="2" />
              下载 RSA 公钥
            </a>
          </div>
        </section>
      </div>
    </main>

    <footer class="login-footer">
      <span>© 2026 工业零配件溯源 · 内部系统</span>
      <span class="login-footer__version mono">v2.4.1</span>
    </footer>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--canvas);
  color: var(--ink-muted);
  position: relative;
  overflow-x: hidden;
}

.login-page::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background: radial-gradient(60% 50% at 50% 0%, rgba(94, 106, 210, 0.04), transparent 70%);
}

.login-header {
  padding: 24px 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 1;
}

.login-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: -0.6px;
  text-decoration: none;
}

.login-brand__logo {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--primary);
  display: grid;
  place-items: center;
}

.login-brand__text {
  line-height: 1;
}

.login-main {
  flex: 1 1 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 24px;
  margin-top: -48px;
  position: relative;
  z-index: 1;
}

.login-shell {
  width: 100%;
  max-width: 400px;
}

.login-headline {
  text-align: center;
  margin-bottom: 28px;
}

.login-title {
  font-size: 28px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: -0.6px;
  line-height: 1.2;
  margin: 0 0 8px;
}

.login-subtitle {
  font-size: 14px;
  line-height: 1.5;
  color: var(--ink-subtle);
  margin: 0;
}

.login-card {
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  padding: 32px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.login-remember {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--ink-subtle);
  cursor: pointer;
  user-select: none;
  padding-top: 4px;
}

.login-remember__input {
  position: absolute;
  width: 1px;
  height: 1px;
  margin: -1px;
  padding: 0;
  border: 0;
  clip: rect(0 0 0 0);
  overflow: hidden;
  white-space: nowrap;
}

.login-remember__box {
  width: 14px;
  height: 14px;
  border-radius: 4px;
  border: 1px solid var(--hairline-strong);
  background: var(--surface-1);
  display: grid;
  place-items: center;
  color: transparent;
  transition: background-color 0.12s, border-color 0.12s, color 0.12s;
}

.login-remember__box svg {
  width: 10px;
  height: 10px;
}

.login-remember__input:checked + .login-remember__box {
  background: var(--primary);
  border-color: var(--primary);
  color: #fff;
}

.login-remember__input:focus-visible + .login-remember__box {
  outline: none;
  box-shadow: 0 0 0 3px rgba(94, 106, 210, 0.15);
}

.login-remember:hover .login-remember__box {
  border-color: var(--ink-subtle);
}

.login-error {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--error-soft);
  border: 1px solid #f8c8ca;
  color: var(--error);
  font-size: 13px;
  line-height: 1.45;
}

.login-error svg {
  flex-shrink: 0;
  margin-top: 2px;
}

.login-submit {
  margin-top: 4px;
}

.login-footnote {
  margin: 24px 0 0;
  text-align: center;
  font-size: 13px;
  color: var(--ink-subtle);
}

.login-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  cursor: pointer;
  background: none;
  border: 0;
  padding: 0;
  font-family: inherit;
  transition: color 0.12s;
}

.login-link--muted {
  color: var(--ink-subtle);
}

.login-link--muted:hover,
.login-link--muted:focus-visible {
  color: var(--ink);
  outline: none;
}

.login-link--accent {
  color: var(--primary);
}

.login-link--accent:hover {
  color: var(--primary-hover);
}

.login-audit {
  margin-top: 48px;
  padding-top: 24px;
  border-top: 1px solid var(--hairline);
}

.login-audit__eyebrow {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: var(--ink-subtle);
  text-align: center;
  margin-bottom: 12px;
}

.login-audit__actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  font-size: 13px;
  flex-wrap: wrap;
}

.login-audit__divider {
  color: var(--ink-tertiary);
}

.login-audit__action {
  white-space: nowrap;
}

.login-footer {
  padding: 24px 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--ink-subtle);
  position: relative;
  z-index: 1;
}

.login-footer__version {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
}

.login-error-fade-enter-active,
.login-error-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.login-error-fade-enter-from,
.login-error-fade-leave-to {
  opacity: 0;
  transform: translateY(-2px);
}

@media (max-width: 767.98px) {
  .login-header,
  .login-footer {
    padding: 16px;
  }

  .login-main {
    padding: 0 16px;
    margin-top: -16px;
  }

  .login-card {
    padding: 24px;
  }

  .login-shell {
    max-width: none;
  }

  .login-audit {
    margin-top: 32px;
  }

  .login-audit__actions {
    flex-direction: column;
    gap: 8px;
  }

  .login-audit__divider {
    display: none;
  }

  .login-footer {
    flex-direction: column;
    gap: 4px;
    text-align: center;
  }
}
</style>
