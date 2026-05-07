<script setup>
import { computed } from 'vue'

/**
 * BaseCard —— Linear-light 原生卡片，零 PrimeVue 依赖。
 *
 * 视觉契约（与 frontend/preview/linear-*.html 中 .card 1:1）：
 *   - 背景：var(--surface-1) = #ffffff
 *   - 描边：1px var(--hairline) = #e6e6e8
 *   - 圆角：12px（rounded-lg）；不允许出现 32 / 40 / 48 巨型圆角
 *   - 内边距：默认 md（24px）；提供 none / sm (16px) / md (24px) / lg (32px) 四档
 *
 * 槽位：
 *   - header：完全自定义 header 区域（出现时自带 1px 底线 + 内边距）
 *   - title / subtitle：仅传 title prop 时启用简版 header（22px display + 12px caption）
 *   - default：卡片内容
 *   - footer：底部操作区（自带 1px 顶线 + 内边距）
 */
const props = defineProps({
  title: String,
  subtitle: String,
  padding: {
    type: String,
    default: 'md',
    validator: (v) => ['none', 'sm', 'md', 'lg'].includes(v)
  },
  // 兼容旧用法：noPadding=true 等价 padding='none'
  noPadding: Boolean,
  interactive: Boolean
})

const effectivePadding = computed(() => (props.noPadding ? 'none' : props.padding))

const bodyClass = computed(() => [
  'base-card__body',
  `base-card__body--${effectivePadding.value}`
])

const rootClass = computed(() => [
  'base-card',
  props.interactive && 'base-card--interactive'
])
</script>

<template>
  <div :class="rootClass">
    <header v-if="$slots.header" class="base-card__header">
      <slot name="header" />
    </header>

    <header
      v-else-if="$slots.title || title || $slots.subtitle || subtitle"
      class="base-card__header base-card__header--simple"
    >
      <div v-if="$slots.title || title" class="base-card__title">
        <slot name="title">{{ title }}</slot>
      </div>
      <div v-if="$slots.subtitle || subtitle" class="base-card__subtitle">
        <slot name="subtitle">{{ subtitle }}</slot>
      </div>
    </header>

    <div :class="bodyClass">
      <slot />
    </div>

    <footer v-if="$slots.footer" class="base-card__footer">
      <slot name="footer" />
    </footer>
  </div>
</template>

<style scoped>
.base-card {
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  color: var(--ink-muted);
  overflow: hidden;
}
.base-card--interactive {
  transition: border-color 0.15s;
  cursor: pointer;
}
.base-card--interactive:hover {
  border-color: var(--ink-subtle);
}

.base-card__header {
  padding: 16px 24px;
  border-bottom: 1px solid var(--hairline);
}
.base-card__header--simple {
  padding: 20px 24px 16px;
  border-bottom: 1px solid var(--hairline);
}
.base-card__title {
  font-size: 22px;
  font-weight: 500;
  letter-spacing: -0.4px;
  color: var(--ink);
  line-height: 1.25;
}
.base-card__subtitle {
  font-size: 12px;
  color: var(--ink-subtle);
  margin-top: 4px;
}

.base-card__body--none {
  padding: 0;
}
.base-card__body--sm {
  padding: 16px;
}
.base-card__body--md {
  padding: 24px;
}
.base-card__body--lg {
  padding: 32px;
}

.base-card__footer {
  padding: 12px 24px;
  border-top: 1px solid var(--hairline);
  background: transparent;
}
</style>
