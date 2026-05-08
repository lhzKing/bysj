<script setup>
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, MarkLineComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import EmptyState from '@/shared/components/ui/EmptyState.vue'
import { LineChart as LineIcon } from 'lucide-vue-next'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, MarkLineComponent])

/**
 * DashboardTrend —— Linear-light 折线图。
 *
 * 视觉契约（与 frontend/preview/linear-dashboard.html "扫码量 · 本周" 1:1）：
 *   - 主线：lavender #5e6ad2 (var(--primary)) + 1.5 stroke + 渐变 area (15% → 0% opacity)
 *   - 网格：水平 dashed 线 var(--surface-2)
 *   - x 轴标签：JetBrains Mono 10px var(--ink-tertiary)
 *   - y 轴隐藏（视觉对齐预览页极简风）
 *   - 图例：右上角横排 mini swatch + 12px label
 *   - tooltip：surface-1 背景 + 1px hairline + ink 字
 *
 * 数据契约：trendItems = [{ label, count }]，由后端 GET /api/dashboard/trend 返回。
 *   - range=today 时 label 为小时 0-23
 *   - 其他 range 时 label 为日期 (MM-DD 或 YYYY-MM-DD)
 */
const props = defineProps({
  trendItems: {
    type: Array,
    default: () => []
  },
  trendLabel: {
    type: String,
    default: ''
  }
})

const hasData = computed(() => Array.isArray(props.trendItems) && props.trendItems.length > 0)

const trendChartOption = computed(() => {
  const data = Array.isArray(props.trendItems) ? props.trendItems : []
  const labels = data.map((it) => String(it.label ?? it.date ?? it.hour ?? ''))
  const counts = data.map((it) => Number(it.count ?? 0))

  return {
    grid: {
      left: 8,
      right: 8,
      top: 24,
      bottom: 28,
      containLabel: true
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#ffffff',
      borderColor: '#e6e6e8',
      borderWidth: 1,
      padding: [8, 12],
      textStyle: { color: '#18181b', fontSize: 12, fontWeight: 500 },
      extraCssText: 'box-shadow: 0 4px 16px rgba(15,23,42,0.06); border-radius: 8px;'
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels,
      axisLine: { lineStyle: { color: '#e6e6e8' } },
      axisTick: { show: false },
      axisLabel: {
        color: '#a1a1aa',
        fontFamily: 'JetBrains Mono, ui-monospace, monospace',
        fontSize: 10,
        margin: 12
      }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: '#a1a1aa',
        fontFamily: 'JetBrains Mono, ui-monospace, monospace',
        fontSize: 10
      },
      splitLine: {
        lineStyle: { type: 'dashed', color: '#f4f4f5' }
      }
    },
    series: [
      {
        name: '流转事件',
        type: 'line',
        smooth: true,
        showSymbol: false,
        symbol: 'circle',
        symbolSize: 6,
        sampling: 'lttb',
        lineStyle: {
          width: 1.5,
          color: '#5e6ad2'
        },
        itemStyle: {
          color: '#5e6ad2',
          borderColor: '#fff',
          borderWidth: 1.5
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(94, 106, 210, 0.15)' },
              { offset: 1, color: 'rgba(94, 106, 210, 0)' }
            ]
          }
        },
        emphasis: {
          focus: 'series',
          lineStyle: { width: 2 }
        },
        data: counts
      }
    ]
  }
})
</script>

<template>
  <section class="trend-card" data-testid="dashboard-trend">
    <header class="trend-card__head">
      <div>
        <h2 class="trend-card__title">扫码量 · {{ trendLabel || '所选周期' }}</h2>
        <p class="trend-card__sub">流转事件按 {{ trendLabel || '日期' }} 聚合</p>
      </div>
      <div v-if="hasData" class="trend-card__legend">
        <span class="trend-card__swatch" />
        <span>流转事件</span>
      </div>
    </header>

    <div v-if="hasData" class="trend-card__body" data-testid="dashboard-trend-chart">
      <v-chart class="trend-card__chart" :option="trendChartOption" autoresize />
    </div>
    <div v-else class="trend-card__empty" data-testid="dashboard-trend-empty">
      <EmptyState
        title="所选周期暂无流转记录"
        subtitle="缩短周期或切换为近 7 天 / 近 30 天后再试"
        :icon="LineIcon"
      />
    </div>
  </section>
</template>

<style scoped>
.trend-card {
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.trend-card__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 4px;
}
.trend-card__title {
  font-size: 22px;
  line-height: 1.25;
  letter-spacing: -0.4px;
  font-weight: 500;
  color: var(--ink);
  margin: 0;
}
.trend-card__sub {
  font-size: 12px;
  color: var(--ink-subtle);
  margin: 4px 0 0;
}
.trend-card__legend {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--ink-muted);
}
.trend-card__swatch {
  display: inline-block;
  width: 8px;
  height: 2px;
  background: var(--primary);
  border-radius: 1px;
}

.trend-card__body {
  height: 240px;
  position: relative;
}
.trend-card__chart {
  width: 100%;
  height: 100%;
}

.trend-card__empty {
  height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 640px) {
  .trend-card__body,
  .trend-card__empty {
    height: 200px;
  }
}
</style>
