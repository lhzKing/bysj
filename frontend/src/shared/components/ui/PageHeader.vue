<script setup>
/**
 * PageHeader —— 业务页顶部标题区。
 *
 * 视觉契约（与 frontend/preview/linear-dashboard.html 中 "Page header" 1:1）：
 *   - 容器：flex items-end justify-between
 *   - 左：display d-md（28px / 600 / -0.6px tracking）+ 13px ink-subtle 副标题
 *   - 右：actions slot，水平排列若干 BaseButton / KbdShortcut
 *
 * 槽位：
 *   - title / subtitle：覆盖同名 prop（用于带图标 / 链接的标题）
 *   - actions：右侧操作区
 *   - default：插入到 actions 上方（如 tab 切换）
 *
 * 用法：
 *   <PageHeader title="总览" subtitle="2026 年 5 月 5 日 · 周三 · 14:32">
 *     <template #actions>
 *       <BaseButton variant="secondary"><template #icon>...</template>导出</BaseButton>
 *     </template>
 *   </PageHeader>
 */
defineProps({
  title: String,
  subtitle: String
})
</script>

<template>
  <header class="page-header">
    <div class="page-header__lead">
      <h1 v-if="$slots.title || title" class="page-header__title">
        <slot name="title">{{ title }}</slot>
      </h1>
      <p v-if="$slots.subtitle || subtitle" class="page-header__subtitle">
        <slot name="subtitle">{{ subtitle }}</slot>
      </p>
    </div>

    <div v-if="$slots.actions" class="page-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.page-header__lead {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.page-header__title {
  font-size: 28px;
  font-weight: 600;
  letter-spacing: -0.6px;
  line-height: 1.2;
  color: var(--ink);
  margin: 0;
}
.page-header__subtitle {
  font-size: 13px;
  line-height: 1.5;
  color: var(--ink-subtle);
  margin: 0;
}
.page-header__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

@media (max-width: 640px) {
  .page-header {
    align-items: flex-start;
  }
  .page-header__title {
    font-size: 24px;
    letter-spacing: -0.4px;
  }
  .page-header__actions {
    width: 100%;
  }
}
</style>
