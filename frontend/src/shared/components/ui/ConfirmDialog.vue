<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import { useConfirm } from '@/shared/composables/useConfirm'

/**
 * ConfirmDialog —— Linear-light 原生确认对话框，零 PrimeVue 依赖。
 *
 * 视觉契约：
 *   - 蒙层 rgba(15, 23, 42, 0.45) + opacity 0.18s。
 *   - 卡片：surface-1 + 1px hairline + 12px 圆角 + 24px / 28px padding；
 *     max-width 420px；scale(0.97)→1 + opacity 0.18s 进入。
 *   - 标题 18 / 600 / -0.4px；正文 14 / ink-muted。
 *   - footer 右对齐双按钮（取消 secondary + 确认 primary/danger）。
 *
 * 移动端 ≤640px：dialog 满屏 + footer 粘性吸底。
 *
 * 接口契约：useConfirm 不变，options.{title,message,confirmText,cancelText,type} 沿用。
 */
const { isVisible, options, accept, reject } = useConfirm()

const confirmBtnRef = ref(null)

const confirmVariant = computed(() =>
  options.value?.type === 'danger' ? 'danger' : 'primary'
)

watch(isVisible, async (open) => {
  if (open) {
    document.body.style.overflow = 'hidden'
    await nextTick()
    confirmBtnRef.value?.$el?.focus?.()
  } else {
    document.body.style.overflow = ''
  }
})

function handleKeydown(event) {
  if (event.key === 'Escape') {
    event.stopPropagation()
    reject()
  } else if (event.key === 'Enter') {
    event.stopPropagation()
    accept()
  }
}
</script>

<template>
  <Teleport to="body">
    <Transition name="confirm-fade">
      <div
        v-if="isVisible"
        class="confirm-root"
        role="dialog"
        aria-modal="true"
        data-test="confirm-dialog"
        @keydown="handleKeydown"
      >
        <div class="confirm-backdrop" data-test="confirm-backdrop" @click="reject" />
        <div class="confirm-card" tabindex="-1">
          <header class="confirm-card__header">
            <h2 class="confirm-card__title" data-test="confirm-title">{{ options.title }}</h2>
            <p v-if="options.message" class="confirm-card__message" data-test="confirm-message">
              {{ options.message }}
            </p>
          </header>
          <footer class="confirm-card__footer">
            <BaseButton
              variant="secondary"
              size="md"
              data-test="confirm-cancel"
              @click="reject"
            >
              {{ options.cancelText || '取消' }}
            </BaseButton>
            <BaseButton
              ref="confirmBtnRef"
              :variant="confirmVariant"
              size="md"
              data-test="confirm-accept"
              @click="accept"
            >
              {{ options.confirmText || '确认' }}
            </BaseButton>
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.confirm-root {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.confirm-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
}
.confirm-card {
  position: relative;
  width: 100%;
  max-width: 420px;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  box-shadow: 0 12px 32px -12px rgba(15, 23, 42, 0.18);
  display: flex;
  flex-direction: column;
  outline: none;
}
.confirm-card__header {
  padding: 24px 24px 16px;
}
.confirm-card__title {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.4px;
  color: var(--ink);
}
.confirm-card__message {
  margin: 0;
  font-size: 14px;
  line-height: 1.55;
  color: var(--ink-muted);
}
.confirm-card__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 16px 24px 20px;
}

.confirm-fade-enter-active,
.confirm-fade-leave-active {
  transition: opacity 0.18s ease-out;
}
.confirm-fade-enter-from,
.confirm-fade-leave-to {
  opacity: 0;
}
.confirm-fade-enter-active .confirm-card,
.confirm-fade-leave-active .confirm-card {
  transition: transform 0.18s ease-out, opacity 0.18s ease-out;
}
.confirm-fade-enter-from .confirm-card,
.confirm-fade-leave-to .confirm-card {
  transform: scale(0.97);
  opacity: 0;
}

@media (max-width: 639.98px) {
  .confirm-root {
    padding: 0;
    align-items: stretch;
  }
  .confirm-card {
    max-width: 100%;
    width: 100%;
    height: 100%;
    border-radius: 0;
    border: 0;
    box-shadow: none;
  }
  .confirm-card__header {
    padding: 24px 20px 16px;
    flex: 1 1 auto;
    overflow-y: auto;
  }
  .confirm-card__footer {
    position: sticky;
    bottom: 0;
    background: var(--surface-1);
    border-top: 1px solid var(--hairline);
    padding: 12px 16px calc(12px + env(safe-area-inset-bottom, 0px));
  }
  .confirm-card__footer :deep(.base-btn) {
    flex: 1 1 0;
  }
}
</style>
