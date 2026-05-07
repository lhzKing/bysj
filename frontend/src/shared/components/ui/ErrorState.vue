<script setup>
import BaseButton from '@/shared/components/ui/BaseButton.vue'

/**
 * ErrorState —— Linear-light 错误态。
 *
 * 视觉契约：与 EmptyState 同骨架；icon 圆底切到 error-soft + error 描边 + error 字色；
 * 默认 icon 是 ⚠（自带 SVG）；底部默认 BaseButton primary 重试按钮（可关闭）。
 *
 * Props：title / subtitle / retryText / showRetry。
 * Emits：retry（用户点重试时触发）。
 * Slots：icon / title / subtitle / actions（actions 提供时屏蔽默认重试按钮）。
 */
defineProps({
  title: {
    type: String,
    default: '加载失败'
  },
  subtitle: {
    type: String,
    default: '请稍后重试或联系管理员。'
  },
  retryText: {
    type: String,
    default: '重试'
  },
  showRetry: {
    type: Boolean,
    default: true
  }
})

defineEmits(['retry'])
</script>

<template>
  <div class="error-state" data-test="error-state">
    <div class="error-state__icon" data-test="error-state-icon">
      <slot name="icon">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0Z" />
          <line x1="12" y1="9" x2="12" y2="13" />
          <line x1="12" y1="17" x2="12.01" y2="17" />
        </svg>
      </slot>
    </div>
    <h3 class="error-state__title" data-test="error-state-title">
      <slot name="title">{{ title }}</slot>
    </h3>
    <p v-if="$slots.subtitle || subtitle" class="error-state__subtitle" data-test="error-state-subtitle">
      <slot name="subtitle">{{ subtitle }}</slot>
    </p>
    <div v-if="$slots.actions || showRetry" class="error-state__actions" data-test="error-state-actions">
      <slot name="actions">
        <BaseButton
          v-if="showRetry"
          variant="primary"
          size="sm"
          data-test="error-state-retry"
          @click="$emit('retry')"
        >
          {{ retryText }}
        </BaseButton>
      </slot>
    </div>
  </div>
</template>

<style scoped>
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 48px 24px;
  text-align: center;
}
.error-state__icon {
  width: 36px;
  height: 36px;
  border-radius: 9999px;
  background: var(--error-soft);
  border: 1px solid #f8c8ca;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--error);
  margin-bottom: 12px;
}
.error-state__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: -0.1px;
}
.error-state__subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--ink-subtle);
  line-height: 1.5;
  max-width: 360px;
}
.error-state__actions {
  margin-top: 16px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
</style>
