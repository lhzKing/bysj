<script setup>
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import VChart from 'vue-echarts'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent])

const props = defineProps({
  trendItems: {
    type: Array,
    default: () => []
  },
  trendLabel: String
})

const trendChartOption = computed(() => {
  const data = Array.isArray(props.trendItems) ? props.trendItems : []
  
  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#e2e8f0',
      textStyle: { color: '#1e293b', fontWeight: 'bold' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.map(item => item.label || item.date || item.hour || '-'),
      axisLine: { lineStyle: { color: '#cbd5e1' } },
      axisLabel: { color: '#64748b', fontWeight: 'bold', rotate: 45 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { type: 'dashed', color: '#f1f5f9' } },
      axisLabel: { color: '#64748b', fontWeight: 'bold' }
    },
    series: [
      {
        name: '溯源活动',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: { color: '#6366f1', borderWidth: 2, borderColor: '#fff' },
        lineStyle: { width: 4, shadowColor: 'rgba(99, 102, 241, 0.3)', shadowBlur: 10 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(99, 102, 241, 0.2)' },
            { offset: 1, color: 'rgba(99, 102, 241, 0)' }
          ])
        },
        data: data.map(item => item.count ?? 0)
      }
    ]
  }
})
</script>

<template>
  <div class="flex flex-col h-[500px]">
    <div class="mb-4">
      <h3 class="text-2xl font-black text-slate-900 tracking-tight">活动趋势</h3>
      <p class="text-sm text-slate-500 font-medium">{{ trendLabel }} Analytics</p>
    </div>
    <div class="flex-1 w-full relative min-h-[400px]">
       <v-chart class="w-full h-full absolute inset-0" :option="trendChartOption" autoresize />
    </div>
  </div>
</template>
