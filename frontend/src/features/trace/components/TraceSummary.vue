<script setup>
import { computed } from 'vue'
import dayjs from 'dayjs'

const props = defineProps({
  snapshot: {
    type: Object,
    required: true
  },
  historyCount: {
    type: Number,
    default: 0
  },
  layout: {
    type: String,
    default: 'compact',
    validator: (v) => ['compact', 'full'].includes(v)
  }
})

const formatDate = (date) => (date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-')

const formatLocation = (s) => {
  if (!s) return '-'
  const parts = [s.province, s.city].filter(Boolean)
  return parts.length ? parts.join(' · ') : '-'
}

const items = computed(() => [
  { key: 'spu', label: 'SPU', value: props.snapshot?.spuId ? `SPU-${props.snapshot.spuId}` : '-', mono: true },
  { key: 'status', label: '当前状态', value: props.snapshot?.currentStatus || '-' },
  { key: 'node', label: '所在节点', value: props.snapshot?.currentNode || '-' },
  { key: 'location', label: '当前位置', value: formatLocation(props.snapshot) },
  { key: 'owner', label: '持有方', value: props.snapshot?.currentOwner || '-' },
  { key: 'lastEvent', label: '最近更新', value: formatDate(props.snapshot?.lastEventTime), mono: true },
  { key: 'history', label: '累计流转', value: `${props.historyCount} 次` },
  { key: 'lastHash', label: '最后哈希', value: props.snapshot?.lastHash ? `${props.snapshot.lastHash.slice(0, 12)}…` : '-', mono: true, hideInCompact: true }
])

const visibleItems = computed(() =>
  props.layout === 'full' ? items.value : items.value.filter((i) => !i.hideInCompact)
)
</script>

<template>
  <dl class="trace-summary" :data-layout="layout">
    <div v-for="item in visibleItems" :key="item.key" class="trace-summary__row">
      <dt class="trace-summary__label">{{ item.label }}</dt>
      <dd class="trace-summary__value" :class="{ 'mono': item.mono }">{{ item.value }}</dd>
    </div>
  </dl>
</template>

<style scoped>
.trace-summary {
  margin: 0;
  display: grid;
  gap: 2px;
}
.trace-summary[data-layout='full'] {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 32px;
}

.trace-summary__row {
  display: grid;
  grid-template-columns: 96px 1fr;
  align-items: baseline;
  font-size: 13px;
  padding: 6px 0;
}
.trace-summary__label {
  color: var(--ink-subtle);
  font-weight: 400;
}
.trace-summary__value {
  color: var(--ink);
  font-weight: 500;
  margin: 0;
  word-break: break-all;
}
.trace-summary__value.mono {
  font-family: 'JetBrains Mono', ui-monospace, 'SF Mono', Menlo, monospace;
  font-size: 12px;
}

@media (max-width: 640px) {
  .trace-summary[data-layout='full'] {
    grid-template-columns: minmax(0, 1fr);
  }
  .trace-summary__row {
    grid-template-columns: 88px 1fr;
  }
}
</style>
