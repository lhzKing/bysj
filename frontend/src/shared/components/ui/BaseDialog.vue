<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { X } from 'lucide-vue-next'

/**
 * BaseDialog —— Linear-light 通用对话框外壳（form / wizard / 长内容 dialog）。
 *
 * 视觉契约（与 ConfirmDialog / PromptDialog 同源）：
 *   - 蒙层 rgba(15, 23, 42, 0.45) + 0.18s opacity 渐入；不带 backdrop-filter blur
 *   - 卡片：var(--surface-1) + 1px var(--hairline) + 12px 圆角 + box-shadow 12px ink/18%；transform scale(0.97)→1
 *   - header：左 icon（可选）+ title + 可选 subtitle；右 close icon button（28×28，hover surface-2）
 *   - body：默认 padding 24px；slot=default
 *   - footer：默认 padding 16px 24px，右对齐双按钮；slot=footer
 *   - <640px：dialog 占满全屏（border-radius 0 / border 0），footer sticky bottom + safe-area 兼容
 *
 * 接口：
 *   - modelValue / @update:modelValue 双向绑定显隐
 *   - title / subtitle / icon（lucide 组件）/ size: sm | md | lg
 *   - persistent=true 时点击 backdrop 不关闭（form dirty 状态使用）
 *   - dismissOnEsc=true（默认）
 *   - 槽位：title（覆盖 title prop）/ subtitle / default（body）/ footer
 */
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  icon: { type: [Object, Function], default: null },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg'].includes(v)
  },
  persistent: { type: Boolean, default: false },
  dismissOnEsc: { type: Boolean, default: true },
  closable: { type: Boolean, default: true }
})

const emit = defineEmits(['update:modelValue', 'close'])

const cardRef = ref(null)

const sizeClass = computed(() => `base-dialog__card--${props.size}`)

function close() {
  emit('update:modelValue', false)
  emit('close')
}

function onBackdropClick() {
  if (props.persistent) return
  close()
}

function onCloseButton() {
  if (!props.closable) return
  close()
}

function onKeydown(event) {
  if (event.key === 'Escape' && props.dismissOnEsc && !props.persistent) {
    event.stopPropagation()
    close()
  }
}

watch(
  () => props.modelValue,
  async (open) => {
    if (open) {
      document.body.style.overflow = 'hidden'
      await nextTick()
      cardRef.value?.focus?.()
    } else {
      document.body.style.overflow = ''
    }
  },
  { immediate: true }
)
</script>

<template>
  <Teleport to="body">
    <Transition name="base-dialog-fade">
      <div
        v-if="modelValue"
        class="base-dialog"
        role="dialog"
        aria-modal="true"
        data-test="base-dialog"
        @keydown="onKeydown"
      >
        <div class="base-dialog__backdrop" data-test="base-dialog-backdrop" @click="onBackdropClick" />
        <div
          ref="cardRef"
          :class="['base-dialog__card', sizeClass]"
          tabindex="-1"
        >
          <header v-if="$slots.title || title || $slots.subtitle || subtitle || icon || closable" class="base-dialog__header">
            <div class="base-dialog__heading">
              <span v-if="icon" class="base-dialog__icon">
                <component :is="icon" :size="16" />
              </span>
              <div class="base-dialog__title-stack">
                <h2 class="base-dialog__title" data-test="base-dialog-title">
                  <slot name="title">{{ title }}</slot>
                </h2>
                <p v-if="$slots.subtitle || subtitle" class="base-dialog__subtitle" data-test="base-dialog-subtitle">
                  <slot name="subtitle">{{ subtitle }}</slot>
                </p>
              </div>
            </div>
            <button
              v-if="closable"
              type="button"
              class="base-dialog__close"
              data-test="base-dialog-close"
              aria-label="关闭"
              @click="onCloseButton"
            >
              <X :size="14" />
            </button>
          </header>

          <div class="base-dialog__body" data-test="base-dialog-body">
            <slot />
          </div>

          <footer v-if="$slots.footer" class="base-dialog__footer" data-test="base-dialog-footer">
            <slot name="footer" />
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.base-dialog {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.base-dialog__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
}
.base-dialog__card {
  position: relative;
  width: 100%;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  box-shadow: 0 12px 32px -12px rgba(15, 23, 42, 0.18);
  display: flex;
  flex-direction: column;
  outline: none;
  max-height: calc(100vh - 48px);
  overflow: hidden;
}
.base-dialog__card--sm {
  max-width: 420px;
}
.base-dialog__card--md {
  max-width: 560px;
}
.base-dialog__card--lg {
  max-width: 720px;
}

.base-dialog__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 24px 14px;
  border-bottom: 1px solid var(--hairline);
}
.base-dialog__heading {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 0;
}
.base-dialog__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--primary-soft);
  color: var(--primary);
  flex-shrink: 0;
}
.base-dialog__title-stack {
  min-width: 0;
}
.base-dialog__title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: -0.2px;
  color: var(--ink);
  line-height: 1.4;
}
.base-dialog__subtitle {
  margin: 4px 0 0;
  font-size: 12.5px;
  line-height: 1.5;
  color: var(--ink-subtle);
}
.base-dialog__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--ink-subtle);
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
}
.base-dialog__close:hover {
  background: var(--surface-2);
  color: var(--ink);
}
.base-dialog__close:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px var(--primary-ring);
}

.base-dialog__body {
  padding: 20px 24px;
  overflow-y: auto;
  flex: 1 1 auto;
}

.base-dialog__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 14px 24px 16px;
  border-top: 1px solid var(--hairline);
  background: var(--surface-1);
}

.base-dialog-fade-enter-active,
.base-dialog-fade-leave-active {
  transition: opacity 0.18s ease-out;
}
.base-dialog-fade-enter-from,
.base-dialog-fade-leave-to {
  opacity: 0;
}
.base-dialog-fade-enter-active .base-dialog__card,
.base-dialog-fade-leave-active .base-dialog__card {
  transition: transform 0.18s ease-out, opacity 0.18s ease-out;
}
.base-dialog-fade-enter-from .base-dialog__card,
.base-dialog-fade-leave-to .base-dialog__card {
  transform: scale(0.97);
  opacity: 0;
}

@media (max-width: 639.98px) {
  .base-dialog {
    padding: 0;
    align-items: stretch;
  }
  .base-dialog__card,
  .base-dialog__card--sm,
  .base-dialog__card--md,
  .base-dialog__card--lg {
    max-width: 100%;
    width: 100%;
    height: 100%;
    max-height: 100%;
    border-radius: 0;
    border: 0;
    box-shadow: none;
  }
  .base-dialog__header {
    padding: 16px 16px 12px;
  }
  .base-dialog__body {
    padding: 16px;
  }
  .base-dialog__footer {
    position: sticky;
    bottom: 0;
    padding: 12px 16px calc(12px + env(safe-area-inset-bottom, 0px));
  }
  .base-dialog__footer :deep(.base-btn) {
    flex: 1 1 0;
  }
}
</style>
