<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/core/stores/user'
import BaseButton from '@/shared/components/ui/BaseButton.vue'

const router = useRouter()
const userStore = useUserStore()

function goDashboard() {
  router.push('/')
}

function goLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="not-found-page" data-test="not-found-page">
    <header class="not-found-header">
      <a class="not-found-brand" @click.prevent="goDashboard">
        <span class="not-found-brand__logo" aria-hidden="true">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.4" stroke-linecap="round">
            <path d="M3 9h18M3 15h18M9 3v18M15 3v18" />
          </svg>
        </span>
        <span class="not-found-brand__text">trace.</span>
      </a>
    </header>

    <main class="not-found-main">
      <div class="not-found-shell">
        <p class="not-found-eyebrow mono" data-test="not-found-code">404 · NOT FOUND</p>
        <h1 class="not-found-title">页面未找到</h1>
        <p class="not-found-subtitle">
          抱歉，您访问的页面不存在或已被移除。可能的原因：链接拼写错误、页面已下线，或当前账号没有访问权限。
        </p>

        <div class="not-found-actions">
          <BaseButton
            variant="primary"
            size="md"
            data-test="not-found-go-dashboard"
            @click="goDashboard"
          >
            回到仪表盘
          </BaseButton>
          <BaseButton
            v-if="!userStore.isLoggedIn"
            variant="secondary"
            size="md"
            data-test="not-found-go-login"
            @click="goLogin"
          >
            返回登录
          </BaseButton>
        </div>

        <p class="not-found-foot">
          如认为这是系统问题，请联系系统管理员。
        </p>
      </div>
    </main>

    <footer class="not-found-footer">
      <span>© 2026 工业零配件溯源 · 内部系统</span>
      <span class="not-found-footer__version mono">v2.4.1</span>
    </footer>
  </div>
</template>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--canvas);
  color: var(--ink-muted);
  position: relative;
  overflow-x: hidden;
}

.not-found-page::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background: radial-gradient(60% 50% at 50% 0%, rgba(94, 106, 210, 0.04), transparent 70%);
}

.not-found-header {
  padding: 24px 32px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  position: relative;
  z-index: 1;
}

.not-found-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: -0.6px;
  text-decoration: none;
  cursor: pointer;
}

.not-found-brand__logo {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--primary);
  display: grid;
  place-items: center;
}

.not-found-brand__text {
  line-height: 1;
}

.not-found-main {
  flex: 1 1 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 24px;
  margin-top: -48px;
  position: relative;
  z-index: 1;
}

.not-found-shell {
  width: 100%;
  max-width: 460px;
  text-align: center;
}

.not-found-eyebrow {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 1.4px;
  color: var(--ink-tertiary);
  margin: 0 0 16px;
  text-transform: uppercase;
}

.not-found-title {
  font-size: 32px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: -0.8px;
  line-height: 1.15;
  margin: 0 0 12px;
}

.not-found-subtitle {
  font-size: 14px;
  line-height: 1.6;
  color: var(--ink-subtle);
  margin: 0 0 28px;
}

.not-found-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.not-found-foot {
  margin: 32px 0 0;
  font-size: 12.5px;
  color: var(--ink-tertiary);
}

.not-found-footer {
  padding: 24px 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--ink-subtle);
  position: relative;
  z-index: 1;
}

.not-found-footer__version {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
}

@media (max-width: 640px) {
  .not-found-header,
  .not-found-footer {
    padding: 16px;
  }

  .not-found-main {
    padding: 0 16px;
    margin-top: -16px;
  }

  .not-found-title {
    font-size: 26px;
    letter-spacing: -0.4px;
  }

  .not-found-actions {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
  }

  .not-found-actions :deep(.base-btn) {
    width: 100%;
  }

  .not-found-footer {
    flex-direction: column;
    gap: 4px;
    text-align: center;
  }
}
</style>
