<script setup>
import { computed, ref, useId } from 'vue'

/**
 * BaseInput —— Linear-light 原生输入框，零 PrimeVue 依赖。
 *
 * 视觉契约（与 frontend/preview/linear-login.html 中 .input + .label 1:1）：
 *   - 高度：sm = 32px（dense 表单）；md = 36px（默认，登录 / 主表单）
 *   - 圆角：8px；描边 1px var(--hairline)；背景 var(--surface-1)
 *   - 字号：13px / 400；focus 后边框转 var(--primary-focus) + 3px lavender 15% ring
 *   - label：13px / 500 / ink，下方 6px gap
 *   - helperText：12px / ink-subtle；invalid 时改为 var(--error)
 *   - icon：左侧粘性图标（lucide 组件 / svg），text 缩进 36px
 *   - password：右侧 👁 切换显示
 *
 * 槽位：
 *   - label / helper：覆盖同名 prop（用于带链接的 label / 富文本 helper）
 *   - prefix / suffix：左右内联装饰（替代 icon prop）
 */
const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  },
  label: String,
  helperText: String,
  error: String,
  placeholder: String,
  type: {
    type: String,
    default: 'text'
  },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md'].includes(v)
  },
  disabled: Boolean,
  inputId: String,
  icon: [Object, Function],
  autocomplete: String,
  autofocus: Boolean,
  name: String
})

const emit = defineEmits(['update:modelValue', 'focus', 'blur', 'keydown'])

const fallbackId = useId()
const id = computed(() => props.inputId || `base-input-${fallbackId}`)

const showPassword = ref(false)
const isPasswordType = computed(() => props.type === 'password')
const inputType = computed(() =>
  isPasswordType.value ? (showPassword.value ? 'text' : 'password') : props.type
)

const hasError = computed(() => Boolean(props.error))
const hint = computed(() => props.error || props.helperText || '')

const onInput = (event) => {
  emit('update:modelValue', event.target.value)
}

const wrapperClass = computed(() => [
  'base-input__field',
  `base-input__field--${props.size}`,
  hasError.value && 'base-input__field--error',
  props.disabled && 'base-input__field--disabled'
])
</script>

<template>
  <div class="base-input">
    <label v-if="$slots.label || label" :for="id" class="base-input__label">
      <slot name="label">{{ label }}</slot>
    </label>

    <div :class="wrapperClass">
      <span v-if="$slots.prefix || icon" class="base-input__decor base-input__decor--left">
        <slot name="prefix">
          <component :is="icon" v-if="icon" :size="14" />
        </slot>
      </span>

      <input
        :id="id"
        :name="name"
        :value="modelValue"
        :type="inputType"
        :placeholder="placeholder"
        :disabled="disabled"
        :autocomplete="autocomplete"
        :autofocus="autofocus"
        :aria-invalid="hasError"
        :aria-describedby="hint ? `${id}-hint` : undefined"
        class="base-input__control"
        @input="onInput"
        @focus="$emit('focus', $event)"
        @blur="$emit('blur', $event)"
        @keydown="$emit('keydown', $event)"
      />

      <button
        v-if="isPasswordType"
        type="button"
        class="base-input__decor base-input__decor--right base-input__toggle"
        :aria-label="showPassword ? '隐藏密码' : '显示密码'"
        @click="showPassword = !showPassword"
      >
        <svg v-if="showPassword" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24" />
          <line x1="1" y1="1" x2="23" y2="23" />
        </svg>
        <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
          <circle cx="12" cy="12" r="3" />
        </svg>
      </button>

      <span v-else-if="$slots.suffix" class="base-input__decor base-input__decor--right">
        <slot name="suffix" />
      </span>
    </div>

    <p
      v-if="$slots.helper || hint"
      :id="`${id}-hint`"
      class="base-input__hint"
      :class="{ 'base-input__hint--error': hasError }"
    >
      <slot name="helper">{{ hint }}</slot>
    </p>
  </div>
</template>

<style scoped>
.base-input {
  display: flex;
  flex-direction: column;
}
.base-input__label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
  margin-bottom: 6px;
  line-height: 1.3;
}

.base-input__field {
  position: relative;
  display: flex;
  align-items: center;
  height: 36px;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 8px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.base-input__field--sm {
  height: 32px;
}
.base-input__field:focus-within {
  border-color: var(--primary-focus);
  box-shadow: 0 0 0 3px var(--primary-ring);
}
.base-input__field--error {
  border-color: var(--error);
}
.base-input__field--error:focus-within {
  box-shadow: 0 0 0 3px rgba(229, 72, 77, 0.15);
}
.base-input__field--disabled {
  background: var(--surface-2);
  cursor: not-allowed;
}

.base-input__control {
  flex: 1 1 auto;
  width: 100%;
  height: 100%;
  padding: 0 12px;
  border: 0;
  background: transparent;
  font-size: 14px;
  color: var(--ink);
  outline: none;
  font-family: inherit;
}
.base-input__field--sm .base-input__control {
  font-size: 13px;
}
.base-input__control::placeholder {
  color: var(--ink-tertiary);
}
.base-input__control:disabled {
  color: var(--ink-tertiary);
  cursor: not-allowed;
}

.base-input__decor {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-tertiary);
  flex-shrink: 0;
}
.base-input__decor--left {
  padding-left: 10px;
}
.base-input__decor--left + .base-input__control {
  padding-left: 8px;
}
.base-input__decor--right {
  padding-right: 10px;
}
.base-input__toggle {
  background: transparent;
  border: 0;
  cursor: pointer;
  padding: 0 10px 0 4px;
  height: 100%;
  color: var(--ink-tertiary);
}
.base-input__toggle:hover {
  color: var(--ink-muted);
}

.base-input__hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--ink-subtle);
  line-height: 1.4;
}
.base-input__hint--error {
  color: var(--error);
}
</style>
