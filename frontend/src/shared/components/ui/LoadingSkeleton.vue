<script setup>
import { computed } from 'vue'

/**
 * LoadingSkeleton —— Linear-light 原生骨架屏。
 *
 * 视觉契约：surface-1 卡 + 1px hairline + 12px 圆角；shimmer 行用 surface-2/3
 * 灰阶 1.4s ease-in-out 循环；行高基于 13/14px 文本节奏。
 *
 * 仅实现两种调用方现存使用的 type：
 *   - table：dense 表格态（RoleList.vue:243）。header 一行 + body N 行，rows 控制行数。
 *   - card：堆叠卡片态（UserTable.vue:157）。count 控制卡片数。
 */
const props = defineProps({
  type: {
    type: String,
    default: 'table',
    validator: (v) => ['card', 'table'].includes(v)
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

    <div v-else class="skel-card skel-card--flush">
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

@keyframes skel-shimmer {
  0% { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}

@media (prefers-reduced-motion: reduce) {
  .skel-bar { animation: none; }
}
</style>
