<script setup>
import { computed } from 'vue'

/**
 * LoadingSkeleton —— Linear-light 原生骨架屏，零 PrimeVue 依赖。
 *
 * 视觉契约：surface-1 卡 + 1px hairline + 12px 圆角；shimmer 行用 surface-2 底色 +
 * surface-3 高亮，linear-gradient 1.4s ease-in-out 无限循环；行高基于 13/14px 文本节奏。
 *
 * 兼容契约：保留原 PrimeLoadingSkeleton 的 type / rows / count props 与 `data-test=skeleton-card` /
 * `skeleton-row` 选择器，以便 RoleList、UserTable 等已有调用点零代码改动。
 */
const props = defineProps({
  type: {
    type: String,
    default: 'default',
    validator: (v) => ['default', 'card', 'table', 'chart', 'kpi', 'detail', 'list'].includes(v)
  },
  rows: {
    type: Number,
    default: 5
  },
  count: {
    type: Number,
    default: 1
  }
})

const rowIndexes = computed(() => Array.from({ length: props.rows }, (_, i) => i))
const cardIndexes = computed(() => Array.from({ length: props.count }, (_, i) => i))
const chartHeights = ['35%', '58%', '42%', '76%', '54%']
</script>

<template>
  <div class="skeleton-host" :data-skeleton-type="type">
    <template v-if="type === 'card'">
      <div
        v-for="cardIndex in cardIndexes"
        :key="cardIndex"
        class="skel-card"
        data-test="skeleton-card"
      >
        <span class="skel-bar" style="width: 40%; height: 14px;" />
        <span class="skel-bar" style="width: 28%; height: 22px; margin-top: 12px;" />
        <span class="skel-bar" style="width: 100%; height: 10px; margin-top: 14px;" />
      </div>
    </template>

    <div v-else-if="type === 'table'" class="skel-card skel-card--flush">
      <div class="skel-card__header">
        <span class="skel-bar" style="width: 8rem; height: 22px;" />
        <span class="skel-bar" style="width: 6rem; height: 28px;" />
      </div>
      <div class="skel-card__body">
        <div
          v-for="row in rowIndexes"
          :key="row"
          class="skel-row"
          data-test="skeleton-row"
        >
          <div class="skel-row__main">
            <span class="skel-bar" style="width: 70%; height: 14px;" />
            <span class="skel-bar" style="width: 44%; height: 10px; margin-top: 8px;" />
          </div>
          <div class="skel-row__actions">
            <span class="skel-bar" style="width: 4rem; height: 24px;" />
            <span class="skel-bar" style="width: 4rem; height: 24px;" />
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="type === 'chart'" class="skel-card">
      <span class="skel-bar" style="width: 25%; height: 16px;" />
      <div class="skel-chart">
        <span
          v-for="(height, index) in chartHeights"
          :key="index"
          class="skel-bar skel-bar--bar"
          :style="{ width: '12%', height }"
        />
      </div>
    </div>

    <div v-else-if="type === 'kpi'" class="skel-kpi-grid">
      <div v-for="cardIndex in 4" :key="cardIndex" class="skel-card">
        <div class="skel-row__head">
          <span class="skel-bar" style="width: 6rem; height: 12px;" />
          <span class="skel-bar skel-bar--circle" style="width: 32px; height: 32px;" />
        </div>
        <span class="skel-bar" style="width: 7rem; height: 24px; margin-top: 14px;" />
        <span class="skel-bar" style="width: 4rem; height: 10px; margin-top: 8px;" />
      </div>
    </div>

    <div v-else-if="type === 'detail'" class="skel-card">
      <div class="skel-card__detail-header">
        <span class="skel-bar" style="width: 34%; height: 22px;" />
        <span class="skel-bar" style="width: 46%; height: 12px; margin-top: 10px;" />
      </div>
      <div class="skel-detail-grid">
        <div v-for="fieldIndex in 6" :key="fieldIndex" class="skel-detail-field">
          <span class="skel-bar" style="width: 5rem; height: 10px;" />
          <span class="skel-bar" style="width: 100%; height: 14px; margin-top: 8px;" />
        </div>
      </div>
      <div class="skel-card__detail-footer">
        <span class="skel-bar" style="width: 7rem; height: 14px;" />
        <span class="skel-bar" style="width: 100%; height: 10rem; margin-top: 14px;" />
      </div>
    </div>

    <div v-else-if="type === 'list'" class="skel-list">
      <div v-for="row in rowIndexes" :key="row" class="skel-card skel-card--list">
        <span class="skel-bar skel-bar--circle" style="width: 36px; height: 36px;" />
        <div class="skel-row__main">
          <span class="skel-bar" style="width: 72%; height: 14px;" />
          <span class="skel-bar" style="width: 48%; height: 10px; margin-top: 8px;" />
        </div>
        <span class="skel-bar" style="width: 5rem; height: 24px;" />
      </div>
    </div>

    <div v-else class="skel-default">
      <span class="skel-bar" style="width: 75%; height: 14px;" />
      <span class="skel-bar" style="width: 100%; height: 14px;" />
      <span class="skel-bar" style="width: 84%; height: 14px;" />
    </div>
  </div>
</template>

<style scoped>
.skeleton-host {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skel-card {
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
}
.skel-card--flush {
  padding: 0;
}
.skel-card__header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--hairline);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.skel-card__body {
  padding: 4px 20px 12px;
}
.skel-card__detail-header {
  padding-bottom: 16px;
  border-bottom: 1px solid var(--hairline);
  margin-bottom: 16px;
}
.skel-card__detail-footer {
  padding-top: 16px;
  border-top: 1px solid var(--hairline);
  margin-top: 16px;
}
.skel-card--list {
  flex-direction: row;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
}

.skel-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0;
  border-bottom: 1px solid var(--hairline);
}
.skel-row:last-child {
  border-bottom: 0;
}
.skel-row__main {
  flex: 1 1 auto;
  min-width: 0;
}
.skel-row__actions {
  display: flex;
  gap: 8px;
  margin-left: 16px;
}
.skel-row__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.skel-chart {
  margin-top: 16px;
  height: 220px;
  padding: 16px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  background: var(--surface-2);
  border-radius: 8px;
}

.skel-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}
@media (max-width: 1023.98px) {
  .skel-kpi-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 639.98px) {
  .skel-kpi-grid { grid-template-columns: 1fr; }
}

.skel-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
@media (max-width: 639.98px) {
  .skel-detail-grid { grid-template-columns: 1fr; }
}
.skel-detail-field {
  display: flex;
  flex-direction: column;
}

.skel-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skel-default {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skel-bar {
  display: block;
  border-radius: 6px;
  background: linear-gradient(
    100deg,
    var(--surface-2) 0%,
    var(--surface-3) 50%,
    var(--surface-2) 100%
  );
  background-size: 200% 100%;
  animation: skel-shimmer 1.4s ease-in-out infinite;
}
.skel-bar--circle {
  border-radius: 9999px;
}
.skel-bar--bar {
  border-radius: 4px;
}

@keyframes skel-shimmer {
  0% { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}

@media (prefers-reduced-motion: reduce) {
  .skel-bar { animation: none; }
}
</style>
