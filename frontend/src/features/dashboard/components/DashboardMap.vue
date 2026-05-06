<script setup>
import { computed, shallowRef, onMounted } from 'vue'
import { use, registerMap } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { MapChart } from 'echarts/charts'
import { TooltipComponent, VisualMapComponent } from 'echarts/components'
import VChart from 'vue-echarts'

use([CanvasRenderer, MapChart, TooltipComponent, VisualMapComponent])

// china.json ~580KB；保留为路由级动态 import，确保不会被打进 dashboard 主 chunk。
// 仅在本组件挂载时拉取一次，注册成功后 v-chart 才允许渲染。
const mapReady = shallowRef(false)
onMounted(async () => {
  const { default: chinaMap } = await import('@/assets/map/china.json')
  registerMap('china', chinaMap)
  mapReady.value = true
})

const props = defineProps({
  mapItems: {
    type: Array,
    default: () => []
  }
})

const mapChartOption = computed(() => {
  const data = Array.isArray(props.mapItems) ? props.mapItems.map(item => ({
    // 与 china.json 地图名称保持一致（例如“广东省”“北京市”），不要去后缀
    name: item.name || item.province || '',
    value: item.value || 0
  })) : []

  return {
    tooltip: {
      trigger: 'item',
      formatter: (params) => `${params.name}: ${params.value || 0} 次流转`,
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#e2e8f0',
      textStyle: { color: '#1e293b', fontWeight: 'bold' }
    },
    visualMap: {
      min: 0,
      max: data.length > 0 ? Math.max(...data.map(item => item.value), 10) : 100,
      left: 'left',
      top: 'bottom',
      text: ['高', '低'],
      calculable: true,
      inRange: {
        color: ['#eff6ff', '#6366f1', '#312e81'] // light blue to deep indigo
      }
    },
    geo: {
      map: 'china',
      roam: false,
      zoom: 1.2,
      label: {
        show: false
      },
      itemStyle: {
        areaColor: '#f8fafc',
        borderColor: '#cbd5e1',
        borderWidth: 1
      },
      emphasis: {
        label: { show: true, color: '#6366f1' },
        itemStyle: {
          areaColor: '#e0e7ff'
        }
      }
    },
    series: [
      {
        name: '分布',
        type: 'map',
        map: 'china',
        geoIndex: 0,
        data: data
      }
    ]
  }
})
</script>

<template>
  <div class="flex flex-col h-[500px]">
    <div class="mb-4">
      <h3 class="text-2xl font-black text-slate-900 tracking-tight">全球流转热度</h3>
      <p class="text-sm text-slate-500 font-medium">Global Traceability Heatmap</p>
    </div>
    <div class="flex-1 w-full relative min-h-[400px]">
       <v-chart v-if="mapReady" class="w-full h-full absolute inset-0" :option="mapChartOption" autoresize />
       <div v-else class="absolute inset-0 flex items-center justify-center text-sm text-slate-400 font-medium">
         地图数据加载中…
       </div>
    </div>
  </div>
</template>
