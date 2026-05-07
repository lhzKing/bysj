<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import BaseInput from '@/shared/components/ui/BaseInput.vue'
import { usePrompt } from '@/shared/composables/usePrompt'

/**
 * PromptDialog —— Linear-light 原生输入对话框，零 PrimeVue 依赖。
 *
 * 视觉契约同 ConfirmDialog（蒙层 / 12px 圆角 / hairline / footer 右对齐）；
 * 主体在标题 + 描述下追加 BaseInput（支持 text / password 两种类型 + 内联 error）。
 *
 * 移动端 ≤640px：dialog 满屏 + footer 粘性吸底。
 *
 * 接口契约：usePrompt 不变；resolver 在 confirm() / cancel() 内已自动调用。
 */
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

const inputRef = ref(null)
const inputId = 'prompt-dialog-input'

const inputLocalType = computed(() => inputType.value || 'text')

watch(visible, async (open) => {
  if (open) {
    document.body.style.overflow = 'hidden'
    await nextTick()
    const native = inputRef.value?.$el?.querySelector?.('input')
      || inputRef.value?.querySelector?.('input')
    native?.focus?.()
  } else {
    document.body.style.overflow = ''
  }
})

function handleKeydown(event) {
  if (event.key === 'Escape') {
    event.stopPropagation()
    cancel()
  }
}

function handleEnter(event) {
  event.stopPropagation()
  confirm()
}
</script>

<template>
  <Teleport to="body">
    <Transition name="prompt-fade">
      <div
        v-if="visible"
        class="prompt-root"
        role="dialog"
        aria-modal="true"
        data-test="prompt-dialog"
        @keydown="handleKeydown"
      >
        <div class="prompt-backdrop" data-test="prompt-backdrop" @click="cancel" />
        <div class="prompt-card">
          <header class="prompt-card__header">
            <h2 class="prompt-card__title" data-test="prompt-title">{{ title }}</h2>
            <p v-if="message" class="prompt-card__message" data-test="prompt-message">
              {{ message }}
            </p>
          </header>
          <div class="prompt-card__body">
            <BaseInput
              ref="inputRef"
              v-model="inputValue"
              :type="inputLocalType"
              :placeholder="placeholder"
              :error="error"
              :input-id="inputId"
              size="md"
              data-test="prompt-input"
              @keydown.enter="handleEnter"
            />
          </div>
          <footer class="prompt-card__footer">
            <BaseButton
              variant="secondary"
              size="md"
              data-test="prompt-cancel"
              @click="cancel"
            >
              {{ cancelText }}
            </BaseButton>
            <BaseButton
              variant="primary"
              size="md"
              data-test="prompt-confirm"
              @click="confirm"
            >
              {{ confirmText }}
            </BaseButton>
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.prompt-root {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.prompt-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
}
.prompt-card {
  position: relative;
  width: 100%;
  max-width: 440px;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  box-shadow: 0 12px 32px -12px rgba(15, 23, 42, 0.18);
  display: flex;
  flex-direction: column;
}
.prompt-card__header {
  padding: 24px 24px 12px;
}
.prompt-card__title {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.4px;
  color: var(--ink);
}
.prompt-card__message {
  margin: 0;
  font-size: 14px;
  line-height: 1.55;
  color: var(--ink-muted);
}
.prompt-card__body {
  padding: 4px 24px 4px;
}
.prompt-card__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 16px 24px 20px;
}

.prompt-fade-enter-active,
.prompt-fade-leave-active {
  transition: opacity 0.18s ease-out;
}
.prompt-fade-enter-from,
.prompt-fade-leave-to {
  opacity: 0;
}
.prompt-fade-enter-active .prompt-card,
.prompt-fade-leave-active .prompt-card {
  transition: transform 0.18s ease-out, opacity 0.18s ease-out;
}
.prompt-fade-enter-from .prompt-card,
.prompt-fade-leave-to .prompt-card {
  transform: scale(0.97);
  opacity: 0;
}

@media (max-width: 639.98px) {
  .prompt-root {
    padding: 0;
    align-items: stretch;
  }
  .prompt-card {
    max-width: 100%;
    width: 100%;
    height: 100%;
    border-radius: 0;
    border: 0;
    box-shadow: none;
  }
  .prompt-card__header {
    padding: 24px 20px 12px;
  }
  .prompt-card__body {
    padding: 4px 20px 12px;
    flex: 1 1 auto;
    overflow-y: auto;
  }
  .prompt-card__footer {
    position: sticky;
    bottom: 0;
    background: var(--surface-1);
    border-top: 1px solid var(--hairline);
    padding: 12px 16px calc(12px + env(safe-area-inset-bottom, 0px));
  }
  .prompt-card__footer :deep(.base-btn) {
    flex: 1 1 0;
  }
}
</style>
