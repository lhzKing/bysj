<script setup>
import { computed } from 'vue'

/**
 * DashboardKPI —— Linear-light 4-up flat KPI cards.
 *
 * 视觉契约（与 frontend/preview/linear-dashboard.html "4-up stat row" 1:1）：
 *   - grid 4 列 / gap 16px
 *   - 单卡：surface-1 / 1px hairline / 12px 圆角 / padding 20px
 *   - caption 12px ink-subtle eyebrow + d-num 32px / 600 / -1px tracking + footnote 12px
 *   - 异常待处理卡：数字色用 var(--error)
 *   - 响应式：≥1024px 4 列；768-1023px 2 列；<768px 1 列
 *
 * 字段映射（后端 API GET /api/dashboard/kpi 返回 snake_case，request 拦截器自动转 camelCase）：
 *   - 今日扫码 ← todayNew
 *   - 累计追溯 ← totalTraces
 *   - 流转记录 ← totalLogs
 *   - 异常待处理 ← exceptionCount
 */
const props = defineProps({
  kpiData: {
    type: Object,
    required: true
  }
})

const formatNum = (n) => {
  const v = Number(n ?? 0)
  if (!Number.isFinite(v)) return '0'
  return v.toLocaleString('zh-CN')
}

const today = computed(() => formatNum(props.kpiData?.todayNew))
const total = computed(() => formatNum(props.kpiData?.totalTraces))
const logs = computed(() => formatNum(props.kpiData?.totalLogs))
const exceptions = computed(() => formatNum(props.kpiData?.exceptionCount))
const exceptionCount = computed(() => Number(props.kpiData?.exceptionCount ?? 0))
</script>

<template>
  <section class="kpi-grid" data-testid="dashboard-kpi">
    <div class="kpi-card lift" data-testid="dashboard-kpi-today">
      <div class="kpi-card__caption">今日扫码</div>
      <div class="kpi-card__num">{{ today }}</div>
      <div class="kpi-card__foot">
        <span class="kpi-card__hint">今日新增追溯码</span>
      </div>
    </div>

    <div class="kpi-card lift" data-testid="dashboard-kpi-total">
      <div class="kpi-card__caption">累计追溯</div>
      <div class="kpi-card__num">{{ total }}</div>
      <div class="kpi-card__foot">
        <span class="kpi-card__hint">系统累计追溯码总数</span>
      </div>
    </div>

    <div class="kpi-card lift" data-testid="dashboard-kpi-logs">
      <div class="kpi-card__caption">流转记录</div>
      <div class="kpi-card__num">{{ logs }}</div>
      <div class="kpi-card__foot">
        <span class="kpi-card__hint">所选周期内流转日志数</span>
      </div>
    </div>

    <div class="kpi-card lift" data-testid="dashboard-kpi-exception">
      <div class="kpi-card__caption">异常待处理</div>
      <div class="kpi-card__num kpi-card__num--error">{{ exceptions }}</div>
      <div class="kpi-card__foot">
        <span v-if="exceptionCount > 0" class="kpi-card__hint kpi-card__hint--error">需立即处理</span>
        <span v-else class="kpi-card__hint">当前无未处理异常</span>
      </div>
    </div>
  </section>
</template>

<style scoped>
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.kpi-card {
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.lift {
  transition: border-color 0.15s;
}
.lift:hover {
  border-color: var(--ink-subtle);
}

.kpi-card__caption {
  font-size: 12px;
  color: var(--ink-subtle);
}
.kpi-card__num {
  font-size: 32px;
  line-height: 1.1;
  letter-spacing: -1px;
  font-weight: 600;
  color: var(--ink);
}
.kpi-card__num--error {
  color: var(--error);
}
.kpi-card__foot {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  font-size: 12px;
}
.kpi-card__hint {
  color: var(--ink-tertiary);
}
.kpi-card__hint--error {
  color: var(--error);
  font-weight: 500;
}

@media (max-width: 1023px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 640px) {
  .kpi-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
