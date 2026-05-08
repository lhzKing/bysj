<script setup>
import { computed } from 'vue'
import EmptyState from '@/shared/components/ui/EmptyState.vue'
import StatusPill from '@/shared/components/ui/StatusPill.vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import { AlertTriangle, ArrowRight } from 'lucide-vue-next'

/**
 * DashboardExceptions —— Linear-light 异常待处理表（占位版）。
 *
 * 视觉契约（与 frontend/preview/linear-dashboard.html "异常待处理" 1:1）：
 *   - card 容器 + 顶部 header（标题 + 计数 pill + 筛选/查看全部）
 *   - 6 列 dense table：追溯码 / 配件 / 异常类型 / 上报 / SLA / 状态
 *   - 行 hover surface-2，cursor pointer，整行可跳转
 *   - 空态：EmptyState"暂无未处理异常"或"接口待开放"
 *
 * 数据来源现状：后端尚未提供 GET /api/dashboard/exceptions 列表端点（仅 KPI 返回 exceptionCount 总数）。
 * 当前实现：
 *   - exceptionCount > 0：显示占位卡片（待 B34 端点上线后接真实数据）
 *   - exceptionCount === 0：显示"全部已处理"绿色 EmptyState
 *   - 永远显示"前往异常处理页"链接（路由 /traces?status=EXCEPTION）
 *
 * F14 任务表已记录该 gap 为遗留项，待 B34 后端端点落地后补完。
 */
const props = defineProps({
  exceptionCount: {
    type: Number,
    default: 0
  },
  // 预留：B34 上线后填入 [{ traceCode, partName, exceptionType, raisedAt, raisedBy, sla, status }]
  items: {
    type: Array,
    default: () => []
  }
})

defineEmits(['view-all'])

const hasItems = computed(() => Array.isArray(props.items) && props.items.length > 0)
const hasCount = computed(() => Number(props.exceptionCount ?? 0) > 0)
</script>

<template>
  <section class="exc-card" data-testid="dashboard-exceptions">
    <header class="exc-card__head">
      <div class="exc-card__title-wrap">
        <h2 class="exc-card__title">异常待处理</h2>
        <StatusPill v-if="hasCount" tone="error" :dot="false">{{ exceptionCount }} 项</StatusPill>
        <StatusPill v-else tone="success" :dot="false">已全部处理</StatusPill>
      </div>
      <div class="exc-card__actions">
        <BaseButton variant="text" @click="$emit('view-all')" data-testid="dashboard-exceptions-view-all">
          查看全部
          <template #icon>
            <ArrowRight :size="13" :stroke-width="2" />
          </template>
        </BaseButton>
      </div>
    </header>

    <table v-if="hasItems" class="exc-card__tbl" data-testid="dashboard-exceptions-table">
      <thead>
        <tr>
          <th class="exc-card__th-first">追溯码</th>
          <th>配件</th>
          <th>异常类型</th>
          <th>上报</th>
          <th>SLA</th>
          <th class="exc-card__th-last">状态</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="row in items"
          :key="row.traceCode"
          class="exc-card__row"
          data-testid="dashboard-exceptions-row"
        >
          <td class="exc-card__td-first">
            <span class="mono">{{ row.traceCode }}</span>
          </td>
          <td>{{ row.partName || '—' }}</td>
          <td>{{ row.exceptionType || '—' }}</td>
          <td>
            <span class="exc-card__sub">{{ row.raisedAt }} · {{ row.raisedBy }}</span>
          </td>
          <td>
            <StatusPill v-if="row.slaTone" :tone="row.slaTone" :dot="false">{{ row.sla }}</StatusPill>
            <span v-else class="exc-card__sub">{{ row.sla || '—' }}</span>
          </td>
          <td class="exc-card__td-last">
            <StatusPill tone="mute">{{ row.status || '—' }}</StatusPill>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-else-if="hasCount" class="exc-card__placeholder" data-testid="dashboard-exceptions-placeholder">
      <EmptyState
        :icon="AlertTriangle"
        subtitle="近期异常列表 API 待 B34 落地（GET /api/dashboard/exceptions）；当前可前往异常处理页查看全部"
      >
        <template #title>共有 {{ exceptionCount }} 项未处理异常</template>
      </EmptyState>
    </div>

    <div v-else class="exc-card__placeholder" data-testid="dashboard-exceptions-empty">
      <EmptyState
        :icon="AlertTriangle"
        title="所选周期暂无异常"
        subtitle="所有溯源链路均通过系统校验"
      />
    </div>
  </section>
</template>

<style scoped>
.exc-card {
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  overflow: hidden;
}

.exc-card__head {
  padding: 14px 20px;
  border-bottom: 1px solid var(--hairline);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.exc-card__title-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}
.exc-card__title {
  margin: 0;
  font-size: 22px;
  line-height: 1.25;
  letter-spacing: -0.4px;
  font-weight: 500;
  color: var(--ink);
}
.exc-card__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.exc-card__tbl {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.exc-card__tbl th {
  text-align: left;
  font-weight: 500;
  color: var(--ink-subtle);
  font-size: 11.5px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  padding: 10px 16px;
  border-bottom: 1px solid var(--hairline);
}
.exc-card__th-first {
  padding-left: 20px !important;
}
.exc-card__th-last {
  padding-right: 20px !important;
}
.exc-card__tbl td {
  padding: 11px 16px;
  border-bottom: 1px solid var(--hairline);
  color: var(--ink);
}
.exc-card__td-first {
  padding-left: 20px !important;
}
.exc-card__td-last {
  padding-right: 20px !important;
}
.exc-card__row {
  cursor: pointer;
}
.exc-card__row:hover td {
  background: var(--surface-2);
}
.exc-card__row:last-child td {
  border-bottom: 0;
}

.exc-card__sub {
  color: var(--ink-subtle);
}
.mono {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
  font-size: 12.5px;
  color: var(--ink);
}

.exc-card__placeholder {
  padding: 8px 0 16px;
}

@media (max-width: 640px) {
  .exc-card__tbl thead {
    display: none;
  }
  .exc-card__tbl,
  .exc-card__tbl tbody,
  .exc-card__tbl tr,
  .exc-card__tbl td {
    display: block;
    width: 100%;
  }
  .exc-card__row {
    border-bottom: 1px solid var(--hairline);
    padding: 12px 16px 14px;
  }
  .exc-card__row:last-child {
    border-bottom: 0;
  }
  .exc-card__tbl td {
    padding: 4px 0 !important;
    border-bottom: 0;
  }
}
</style>
