<script setup>
import { computed } from 'vue'

/**
 * BaseButton —— Linear-light 原生按钮，零 PrimeVue 依赖。
 *
 * 视觉契约（与 frontend/preview/linear-*.html 中 .btn-primary / .btn-secondary / kbd 1:1）：
 *   - 高度：sm = 32px（默认，dense table / topbar / 表单）；md = 36px（紧凑稍松，登录 / 主 CTA）
 *   - 圆角：8px（rounded-md）
 *   - 字号：13px / 500
 *   - primary：lavender 背景 + 白字；hover 切到 #828fff
 *   - secondary：白底 + ink 字 + 1px hairline；hover 描边升至 ink-subtle
 *   - text：透明背景 + ink 字；hover 灌 surface-2
 *   - icon：方形图标按钮（≈ height × height），可叠加任一 variant 的视觉
 *   - danger：error 背景 + 白字（保留给删除/作废确认按钮使用）
 *
 * 槽位：
 *   - default：按钮文本
 *   - icon：左侧图标（svg / lucide 组件）
 *   - kbd：右侧快捷键提示（如 F1 / ⌘ K / Esc），primary 上自动转为白色半透 inverse 调
 */
const props = defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: (v) => ['primary', 'secondary', 'text', 'icon', 'danger'].includes(v)
  },
  size: {
    type: String,
    default: 'sm',
    validator: (v) => ['sm', 'md'].includes(v)
  },
  type: {
    type: String,
    default: 'button'
  },
  block: Boolean,
  loading: Boolean,
  disabled: Boolean,
  iconOnly: Boolean
})

defineEmits(['click'])

const isDisabled = computed(() => props.disabled || props.loading)

const iconOnly = computed(() => props.iconOnly || props.variant === 'icon')

const rootClass = computed(() => [
  'base-btn',
  `base-btn--${props.variant === 'icon' ? 'secondary' : props.variant}`,
  `base-btn--${props.size}`,
  iconOnly.value && 'base-btn--icon-only',
  props.block && 'base-btn--block',
  isDisabled.value && 'base-btn--disabled'
])
</script>

<template>
  <button
    :type="type"
    :disabled="isDisabled"
    :class="rootClass"
    @click="$emit('click', $event)"
  >
    <span v-if="loading" class="base-btn__spinner" aria-hidden="true">
      <svg viewBox="0 0 24 24" width="14" height="14">
        <circle
          cx="12"
          cy="12"
          r="9"
          fill="none"
          stroke="currentColor"
          stroke-width="2.4"
          stroke-linecap="round"
          stroke-dasharray="14 42"
        />
      </svg>
    </span>
    <span v-else-if="$slots.icon" class="base-btn__icon">
      <slot name="icon" />
    </span>

    <span v-if="!iconOnly" class="base-btn__label">
      <slot />
    </span>

    <span v-if="$slots.kbd" class="base-btn__kbd">
      <slot name="kbd" />
    </span>
  </button>
</template>

<style scoped>
.base-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  height: 32px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid transparent;
  font-size: 13px;
  font-weight: 500;
  line-height: 1;
  letter-spacing: 0;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s, box-shadow 0.15s;
  white-space: nowrap;
  user-select: none;
  font-family: inherit;
}
.base-btn:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px var(--primary-ring);
}
.base-btn--md {
  height: 36px;
  padding: 0 14px;
  font-size: 14px;
  gap: 8px;
}
.base-btn--block {
  width: 100%;
}
.base-btn--disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}

/* primary —— lavender CTA */
.base-btn--primary {
  background: var(--primary);
  color: #fff;
}
.base-btn--primary:hover {
  background: var(--primary-hover);
}
.base-btn--primary:active {
  background: var(--primary-focus);
}

/* secondary —— 白底 hairline */
.base-btn--secondary {
  background: var(--surface-1);
  color: var(--ink);
  border-color: var(--hairline);
}
.base-btn--secondary:hover {
  border-color: var(--ink-subtle);
}

/* text —— 透明背景 ghost */
.base-btn--text {
  background: transparent;
  color: var(--ink);
}
.base-btn--text:hover {
  background: var(--surface-2);
}

/* danger —— error 背景白字（保留给销毁/作废按钮） */
.base-btn--danger {
  background: var(--error);
  color: #fff;
}
.base-btn--danger:hover {
  filter: brightness(1.05);
}

/* icon-only —— 方形图标按钮 */
.base-btn--icon-only {
  padding: 0;
  width: 32px;
  gap: 0;
}
.base-btn--icon-only.base-btn--md {
  width: 36px;
}

/* kbd 子槽 —— 默认走 surface-2，primary 上自动转为白色 inverse */
.base-btn__kbd {
  margin-left: 4px;
  font-family: 'JetBrains Mono', ui-monospace, 'SF Mono', Menlo, monospace;
  font-size: 11px;
  line-height: 1;
  color: var(--ink-tertiary);
  background: var(--surface-2);
  border: 1px solid var(--hairline);
  padding: 1px 5px;
  border-radius: 4px;
}
.base-btn--primary .base-btn__kbd,
.base-btn--danger .base-btn__kbd {
  color: rgba(255, 255, 255, 0.85);
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.2);
}

/* icon slot —— 与文字基线对齐 */
.base-btn__icon,
.base-btn__spinner {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.base-btn__spinner svg {
  animation: base-btn-spin 0.7s linear infinite;
}
.base-btn__label {
  display: inline-flex;
  align-items: center;
}

@keyframes base-btn-spin {
  to { transform: rotate(360deg); }
}
</style>
