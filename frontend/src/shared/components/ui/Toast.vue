<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { onToastMessage } from '@/shared/composables/useToast'

/**
 * Toast —— Linear-light 原生 toast，零 PrimeVue 依赖。
 *
 * 视觉契约（与 frontend/preview/linear-*.html 中同源 token 1:1）：
 *   - 桌面：右下角 stack，纵向最多 3 条；超出排队。
 *   - 移动端 <768px：底部居中，铺满 100vw - 32px。
 *   - 卡片：surface-1 + 1px hairline + 12px 圆角 + 12px / 16px padding；shadow 软阴影。
 *   - 左 6×6 severity dot；中标题 13/600 + 详情 12.5/ink-subtle；右 14×14 close。
 *   - 进出：opacity + translateY(8px) 0.18s ease-out。
 *
 * 接口契约：与 useToast 桥接 message {severity, summary, detail, life} 不变。
 */
const MAX_VISIBLE = 3

const toasts = ref([])
let stopListening = null
let idCounter = 0

const toneMap = {
  success: 'success',
  error: 'error',
  warn: 'warn',
  info: 'primary'
}

const visibleToasts = computed(() => toasts.value.slice(0, MAX_VISIBLE))

onMounted(() => {
  stopListening = onToastMessage((message) => {
    const id = idCounter++
    const tone = toneMap[message.severity] || 'primary'
    toasts.value.push({ id, tone, ...message })

    if (message.life !== 0) {
      setTimeout(() => removeToast(id), message.life || 3000)
    }
  })
})

onBeforeUnmount(() => {
  stopListening?.()
})

function removeToast(id) {
  toasts.value = toasts.value.filter((t) => t.id !== id)
}
</script>

<template>
  <Teleport to="body">
    <div class="toast-host" data-test="toast-host">
      <TransitionGroup name="toast-stack" tag="div" class="toast-host__stack">
        <div
          v-for="toast in visibleToasts"
          :key="toast.id"
          class="toast"
          :class="`toast--${toast.tone}`"
          role="status"
          data-test="toast-item"
        >
          <span class="toast__dot" :class="`toast__dot--${toast.tone}`" />
          <div class="toast__body">
            <p class="toast__title">{{ toast.summary }}</p>
            <p v-if="toast.detail" class="toast__detail">{{ toast.detail }}</p>
          </div>
          <button
            type="button"
            class="toast__close"
            aria-label="关闭"
            @click="removeToast(toast.id)"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-host {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 2000;
  pointer-events: none;
  display: flex;
  justify-content: flex-end;
}
.toast-host__stack {
  display: flex;
  flex-direction: column-reverse;
  gap: 8px;
  width: 320px;
  max-width: calc(100vw - 32px);
}
.toast {
  pointer-events: auto;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  box-shadow: 0 6px 20px -10px rgba(15, 23, 42, 0.18);
}
.toast__dot {
  flex-shrink: 0;
  margin-top: 6px;
  width: 6px;
  height: 6px;
  border-radius: 9999px;
  background: var(--ink-subtle);
}
.toast__dot--success { background: var(--success); }
.toast__dot--warn { background: var(--warn); }
.toast__dot--error { background: var(--error); }
.toast__dot--primary { background: var(--primary); }
.toast__body {
  flex: 1 1 auto;
  min-width: 0;
}
.toast__title {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: -0.1px;
  line-height: 1.35;
}
.toast__detail {
  margin: 2px 0 0;
  font-size: 12.5px;
  color: var(--ink-subtle);
  line-height: 1.45;
  word-break: break-word;
}
.toast__close {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  background: transparent;
  color: var(--ink-tertiary);
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.toast__close:hover {
  background: var(--surface-2);
  color: var(--ink);
}

.toast-stack-enter-active,
.toast-stack-leave-active {
  transition: opacity 0.18s ease-out, transform 0.18s ease-out;
}
.toast-stack-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.toast-stack-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
.toast-stack-leave-active {
  position: absolute;
  right: 0;
  width: 100%;
}

@media (max-width: 767.98px) {
  .toast-host {
    right: 16px;
    left: 16px;
    bottom: 16px;
    justify-content: center;
  }
  .toast-host__stack {
    width: 100%;
    max-width: 100%;
  }
}
</style>
