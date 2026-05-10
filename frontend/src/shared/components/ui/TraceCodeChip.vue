<script setup>
import { computed, ref } from 'vue'

/**
 * TraceCodeChip —— 追溯码 mono 标签 + 一键复制。
 *
 * 视觉契约（与 frontend/preview/linear-*.html 中表格 mono 列 + scan-detail header 1:1）：
 *   - 字体：JetBrains Mono；ink；可选 mono size=12 / 12.5 / 14 / 19 / 26
 *   - copyable=true 时整个 chip 可点击；点击后 1.5s 显示"已复制"反馈，再恢复
 *   - hover：light surface-2 底
 *   - 失败时不阻塞 UI，仅在事件中抛出
 *
 * 用法：
 *   <TraceCodeChip code="TC-260505-A8F3K2" />
 *   <TraceCodeChip code="TC-260505-A8F3K2" :copyable="false" size="lg" />
 *   <TraceCodeChip code="..." @copy="onCopied" />
 */
const props = defineProps({
  code: {
    type: String,
    required: true
  },
  size: {
    type: String,
    default: 'sm',
    validator: (v) => ['xs', 'sm', 'md', 'lg', 'xl'].includes(v)
  },
  copyable: {
    type: Boolean,
    default: true
  },
  // 当 copyable 时，是否显示尾随复制图标（false 用于 dense 表格）
  showCopyIcon: {
    type: Boolean,
    default: false
  },
  // 长 UUID 截断显示（前 8 + … + 后 4），完整码仍由 title / 复制保留
  truncate: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['copy', 'copy-error'])

const copied = ref(false)
const tooltipOpen = ref(false)
let resetTimer = null

const displayCode = computed(() => {
  if (!props.truncate || !props.code || props.code.length <= 14) return props.code
  return `${props.code.slice(0, 8)}…${props.code.slice(-4)}`
})

const sizeStyle = computed(() => {
  const map = {
    xs: { fontSize: '11.5px' },
    sm: { fontSize: '12px' },
    md: { fontSize: '12.5px' },
    lg: { fontSize: '14px' },
    xl: { fontSize: '19px', fontWeight: '600', letterSpacing: '-0.3px' }
  }
  return map[props.size] || map.sm
})

const onClick = async () => {
  if (!props.copyable) return
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(props.code)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = props.code
      textarea.setAttribute('readonly', '')
      textarea.style.position = 'absolute'
      textarea.style.left = '-9999px'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    copied.value = true
    emit('copy', props.code)
    if (resetTimer) clearTimeout(resetTimer)
    resetTimer = setTimeout(() => { copied.value = false }, 1500)
  } catch (err) {
    emit('copy-error', err)
  }
}
</script>

<template>
  <span
    class="trace-chip"
    :class="{ 'trace-chip--copyable': copyable, 'trace-chip--copied': copied }"
    :style="sizeStyle"
    :role="copyable ? 'button' : undefined"
    :tabindex="copyable ? 0 : undefined"
    :title="code"
    @click="onClick"
    @keydown.enter.prevent="onClick"
    @keydown.space.prevent="onClick"
    @mouseenter="tooltipOpen = true"
    @mouseleave="tooltipOpen = false"
    @focus="tooltipOpen = true"
    @blur="tooltipOpen = false"
  >
    <span class="trace-chip__code">{{ displayCode }}</span>
    <svg
      v-if="copyable && showCopyIcon && !copied"
      class="trace-chip__icon"
      width="11"
      height="11"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      aria-hidden="true"
    >
      <rect x="9" y="9" width="13" height="13" rx="2" />
      <path d="M5 15V5a2 2 0 012-2h10" />
    </svg>
    <svg
      v-else-if="copied"
      class="trace-chip__icon trace-chip__icon--success"
      width="11"
      height="11"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2.4"
      aria-hidden="true"
    >
      <path d="M5 12l5 5L20 7" />
    </svg>
    <span v-if="copyable && (copied || tooltipOpen)" class="trace-chip__tooltip" role="status">
      {{ copied ? '已复制' : code }}
    </span>
  </span>
</template>

<style scoped>
.trace-chip {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-family: 'JetBrains Mono', ui-monospace, 'SF Mono', Menlo, monospace;
  color: var(--ink);
  line-height: 1;
}
.trace-chip--copyable {
  cursor: pointer;
  padding: 2px 4px;
  margin: -2px -4px;
  border-radius: 4px;
  transition: background 0.15s;
}
.trace-chip--copyable:hover,
.trace-chip--copyable:focus-visible {
  background: var(--surface-2);
  outline: none;
}
.trace-chip--copied {
  background: var(--success-soft);
  color: var(--success);
}

.trace-chip__icon {
  color: var(--ink-tertiary);
  flex-shrink: 0;
}
.trace-chip__icon--success {
  color: var(--success);
}

.trace-chip__tooltip {
  position: absolute;
  left: 50%;
  bottom: calc(100% + 6px);
  transform: translateX(-50%);
  font-family: 'Inter', -apple-system, sans-serif;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
  background: var(--ink);
  color: var(--surface-1);
  padding: 4px 8px;
  border-radius: 4px;
  pointer-events: none;
  z-index: 10;
}
</style>
