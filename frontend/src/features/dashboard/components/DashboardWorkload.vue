<script setup>
import { computed } from 'vue'
import EmptyState from '@/shared/components/ui/EmptyState.vue'
import { Map as MapIcon } from 'lucide-vue-next'

/**
 * DashboardWorkload —— Linear-light 区域流转分布。
 *
 * 视觉契约（与 frontend/preview/linear-dashboard.html "工位分布" 1:1）：
 *   - 单条：第一行 (label / mono count)；第二行 4px 高 surface-2 底 + lavender 进度条
 *   - 数据为空时进度条为 ink-tertiary，0% 宽度
 *   - 列表竖排 14px gap，最多取前 6 条（更多请走详情）
 *
 * 字段映射（后端 GET /api/dashboard/map 返回 snake_case）：
 *   - items: [{ name: '浙江', value: 120 }] —— 已按 value 降序，硬上限 50。
 *
 * 命名调整说明：F14 重构前该卡承载"中国地图"视觉（DashboardMap.vue + 580KB china.json），
 * F14 后预览页改为按地区聚合的横向 bar list，新文件名更贴合视觉，
 * 仍复用同一后端端点 GET /api/dashboard/map（按 province 聚合）。
 */
const props = defineProps({
  items: {
    type: Array,
    default: () => []
  },
  rangeLabel: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['view-detail', 'view-province'])

const TOP_N = 6

const topItems = computed(() => {
  if (!Array.isArray(props.items)) return []
  return props.items.slice(0, TOP_N).map((item) => ({
    name: item.name ?? item.province ?? '',
    value: Number(item.value ?? item.count ?? 0)
  }))
})

const maxValue = computed(() => {
  const list = topItems.value
  if (list.length === 0) return 0
  return Math.max(...list.map((it) => it.value), 1)
})

const formatNum = (n) => Number(n ?? 0).toLocaleString('zh-CN')

const progressWidth = (value) => {
  if (maxValue.value === 0) return '0%'
  const pct = Math.max(2, Math.round((value / maxValue.value) * 100))
  return `${Math.min(100, pct)}%`
}
</script>

<template>
  <section class="workload-card" data-testid="dashboard-workload">
    <header class="workload-card__head">
      <div>
        <h2 class="workload-card__title">区域流转分布</h2>
        <p v-if="rangeLabel" class="workload-card__sub">{{ rangeLabel }} · 按省份聚合 · Top {{ TOP_N }}</p>
      </div>
      <a
        v-if="topItems.length > 0"
        class="workload-card__link"
        href="#"
        data-testid="dashboard-workload-link"
        @click.prevent="emit('view-detail')"
      >详情</a>
    </header>

    <ul v-if="topItems.length > 0" class="workload-card__list" data-testid="dashboard-workload-list">
      <li
        v-for="item in topItems"
        :key="item.name"
        class="workload-card__row"
        data-testid="dashboard-workload-row"
        :data-name="item.name"
        @click="emit('view-province', item.name)"
        @keydown.enter.prevent="emit('view-province', item.name)"
        tabindex="0"
        role="button"
        :title="`查看 ${item.name} 的追溯码列表`"
      >
        <div class="workload-card__row-head">
          <span class="workload-card__name">{{ item.name || '未署名' }}</span>
          <span class="workload-card__value mono">{{ formatNum(item.value) }}</span>
        </div>
        <div class="workload-card__bar">
          <div
            class="workload-card__bar-fill"
            :class="{ 'workload-card__bar-fill--mute': item.value === 0 }"
            :style="{ width: progressWidth(item.value) }"
          />
        </div>
      </li>
    </ul>

    <div v-else class="workload-card__empty" data-testid="dashboard-workload-empty">
      <EmptyState
        title="所选周期内尚无地区流转记录"
        subtitle="切换为更长周期或检查数据采集端"
        :icon="MapIcon"
      />
    </div>
  </section>
</template>

<style scoped>
.workload-card {
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.workload-card__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
}
.workload-card__title {
  margin: 0;
  font-size: 22px;
  line-height: 1.25;
  letter-spacing: -0.4px;
  font-weight: 500;
  color: var(--ink);
}
.workload-card__sub {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--ink-subtle);
}
.workload-card__link {
  font-size: 12px;
  font-weight: 500;
  color: var(--primary);
  text-decoration: none;
}
.workload-card__link:hover {
  color: var(--primary-hover);
}

.workload-card__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.workload-card__row {
  cursor: pointer;
  transition: background 0.12s;
  border-radius: 6px;
  padding: 4px 6px;
  margin: -4px -6px;
}
.workload-card__row:hover,
.workload-card__row:focus-visible {
  background: var(--surface-2);
  outline: none;
}
.workload-card__row-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 13px;
}
.workload-card__name {
  color: var(--ink);
  font-weight: 500;
}
.workload-card__value {
  font-size: 12px;
  color: var(--ink-subtle);
}
.mono {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
}

.workload-card__bar {
  height: 4px;
  background: var(--surface-2);
  border-radius: 9999px;
  overflow: hidden;
}
.workload-card__bar-fill {
  height: 100%;
  background: var(--primary);
  transition: width 0.25s ease;
}
.workload-card__bar-fill--mute {
  background: var(--ink-tertiary);
}

.workload-card__empty {
  padding: 8px 0;
}
</style>
