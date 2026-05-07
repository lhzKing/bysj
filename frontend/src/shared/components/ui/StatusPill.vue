<script setup>
import { computed } from 'vue'

/**
 * StatusPill —— Linear-light 状态徽标。
 *
 * 视觉契约（与 frontend/preview/linear-*.html 中 .pill / .pill-success / .pill-error 1:1）：
 *   - 高度 22px / 字号 11.5px / 圆角 9999px / padding 0 8px
 *   - dot 在前（6×6，圆角 9999px）+ 文本在后
 *   - 5 种 tone：success / warn / error / mute / primary
 *   - size="xs"：用于侧栏"异常 3"等小角标（高 18px / 字 10.5px / padding 0 6px）
 *
 * 用法：
 *   <StatusPill tone="success">在库</StatusPill>
 *   <StatusPill tone="error" :dot="false">超时 1h 23m</StatusPill>
 *   <StatusPill tone="error" size="xs">3</StatusPill>
 */
const props = defineProps({
  tone: {
    type: String,
    default: 'mute',
    validator: (v) => ['success', 'warn', 'error', 'mute', 'primary'].includes(v)
  },
  size: {
    type: String,
    default: 'sm',
    validator: (v) => ['xs', 'sm'].includes(v)
  },
  dot: {
    type: Boolean,
    default: true
  }
})

const rootClass = computed(() => [
  'status-pill',
  `status-pill--${props.tone}`,
  `status-pill--${props.size}`
])

const dotClass = computed(() => `status-pill__dot status-pill__dot--${props.tone}`)
</script>

<template>
  <span :class="rootClass">
    <span v-if="dot" :class="dotClass" />
    <slot />
  </span>
</template>

<style scoped>
.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 22px;
  padding: 0 8px;
  border-radius: 9999px;
  font-size: 11.5px;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
}
.status-pill--xs {
  height: 18px;
  padding: 0 6px;
  font-size: 10.5px;
  gap: 4px;
}

.status-pill--mute {
  background: var(--surface-2);
  color: var(--ink-muted);
}
.status-pill--success {
  background: var(--success-soft);
  color: var(--success);
}
.status-pill--warn {
  background: var(--warn-soft);
  color: var(--warn);
}
.status-pill--error {
  background: var(--error-soft);
  color: var(--error);
}
.status-pill--primary {
  background: var(--primary-soft);
  color: var(--primary);
}

.status-pill__dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 9999px;
  flex-shrink: 0;
}
.status-pill--xs .status-pill__dot {
  width: 5px;
  height: 5px;
}
.status-pill__dot--success {
  background: var(--success);
}
.status-pill__dot--warn {
  background: var(--warn);
}
.status-pill__dot--error {
  background: var(--error);
}
.status-pill__dot--mute {
  background: var(--ink-tertiary);
}
.status-pill__dot--primary {
  background: var(--primary);
}
</style>
