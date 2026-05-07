<script setup>
/**
 * EmptyState —— Linear-light 空数据态。
 *
 * 视觉契约：居中堆叠；上 36×36 lucide icon（surface-2 + ink-tertiary）；
 * 14/600 ink title + 13/ink-subtle subtitle；底部 actions 槽位。
 *
 * 槽位：
 *   - icon：覆盖 props.icon 自定义图标。
 *   - title / subtitle：覆盖同名 prop，支持富文本（如 inline 链接）。
 *   - actions：右侧 / 底部操作（用 BaseButton 装载"创建第一条"等 CTA）。
 *
 * 调用方：列表 / 表格 / 详情子区域，均在 isLoading=false && rows.length=0 时显示。
 */
defineProps({
  icon: {
    type: [Object, Function],
    default: null
  },
  title: {
    type: String,
    default: '暂无数据'
  },
  subtitle: {
    type: String,
    default: ''
  }
})
</script>

<template>
  <div class="empty-state" data-test="empty-state">
    <div v-if="$slots.icon || icon" class="empty-state__icon" data-test="empty-state-icon">
      <slot name="icon">
        <component :is="icon" v-if="icon" :size="20" :stroke-width="1.6" />
      </slot>
    </div>
    <h3 class="empty-state__title" data-test="empty-state-title">
      <slot name="title">{{ title }}</slot>
    </h3>
    <p v-if="$slots.subtitle || subtitle" class="empty-state__subtitle" data-test="empty-state-subtitle">
      <slot name="subtitle">{{ subtitle }}</slot>
    </p>
    <div v-if="$slots.actions" class="empty-state__actions" data-test="empty-state-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 48px 24px;
  text-align: center;
}
.empty-state__icon {
  width: 36px;
  height: 36px;
  border-radius: 9999px;
  background: var(--surface-2);
  border: 1px solid var(--hairline);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-tertiary);
  margin-bottom: 12px;
}
.empty-state__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: -0.1px;
}
.empty-state__subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--ink-subtle);
  line-height: 1.5;
  max-width: 360px;
}
.empty-state__actions {
  margin-top: 16px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
</style>
