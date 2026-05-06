<script setup>
import { onMounted, ref, watch, defineAsyncComponent } from 'vue'
import { getKPI, getMapData, getTrend, getTopology } from '@/features/dashboard/api'
import { logger } from '@/shared/utils/logger'

// 异步加载图表组件
const DashboardKPI = defineAsyncComponent(() => import('../components/DashboardKPI.vue'))
const DashboardMap = defineAsyncComponent(() => import('../components/DashboardMap.vue'))
const DashboardTrend = defineAsyncComponent(() => import('../components/DashboardTrend.vue'))
const DashboardTopology = defineAsyncComponent(() => import('../components/DashboardTopology.vue'))

const rangeOptions = [
  { value: 'today', label: '今天' },
  { value: '7d', label: '7天' },
  { value: '30d', label: '30天' },
  { value: '180d', label: '180天' },
  { value: 'all', label: '全部' }
]

const selectedRange = ref('30d')
const loading = ref(false)

const kpiData = ref({
  totalTraces: 0,
  todayNew: 0,
  totalLogs: 0,
  exceptionCount: 0
})

const mapItems = ref([])
const trendItems = ref([])
const topologyData = ref({ nodes: [], links: [] })

const getTrendLabel = () => {
  if (selectedRange.value === 'today') return '小时趋势'
  if (selectedRange.value === '7d') return '日趋势 (7天)'
  return `${rangeOptions.find(option => option.value === selectedRange.value)?.label || ''} 趋势`
}

const loadDashboardData = async () => {
  loading.value = true
  try {
    const range = selectedRange.value
    const [kpiResult, mapResult, trendResult, topologyResult] = await Promise.allSettled([
      getKPI(range),
      getMapData(range),
      getTrend(range),
      getTopology(null, range)
    ])

    if (kpiResult.status === 'fulfilled' && kpiResult.value) {
      kpiData.value = {
        totalTraces: kpiResult.value.totalTraces ?? 0,
        todayNew: kpiResult.value.todayNew ?? 0,
        totalLogs: kpiResult.value.totalLogs ?? 0,
        exceptionCount: kpiResult.value.exceptionCount ?? 0
      }
    } else if (kpiResult.status === 'rejected') {
      logger.error('Failed to load KPI:', kpiResult.reason)
    }

    if (mapResult.status === 'fulfilled') {
      mapItems.value = Array.isArray(mapResult.value?.items)
        ? mapResult.value.items.map(item => ({
            // 兼容后端新旧字段:
            // - 新版: { name, value }
            // - 旧版: { province, count }
            name: item.name ?? item.province ?? '',
            value: item.value ?? item.count ?? 0
          }))
        : []
    } else {
      logger.error('Failed to load map data:', mapResult.reason)
    }

    if (trendResult.status === 'fulfilled') {
      trendItems.value = Array.isArray(trendResult.value?.items) ? trendResult.value.items : []
    } else {
      logger.error('Failed to load trend data:', trendResult.reason)
    }

    if (topologyResult.status === 'fulfilled') {
      topologyData.value = {
        nodes: Array.isArray(topologyResult.value?.nodes) ? topologyResult.value.nodes : [],
        links: Array.isArray(topologyResult.value?.links) ? topologyResult.value.links : []
      }
    } else {
      logger.error('Failed to load topology data:', topologyResult.reason)
    }
  } catch (error) {
    logger.error('Failed to load dashboard data', error)
  } finally {
    loading.value = false
  }
}

watch(selectedRange, () => {
  loadDashboardData()
})

onMounted(() => {
  loadDashboardData()
})
</script>

<template>
  <div class="space-y-8 relative">
    <!-- Header -->
    <div class="mb-12 relative">
        <div class="absolute -left-12 -top-12 size-40 bg-indigo-200 rounded-full blur-[80px] opacity-30"></div>
        <div class="flex flex-col md:flex-row md:items-end justify-between gap-6">
          <div>
            <h1 class="text-5xl font-extrabold tracking-tight text-slate-900 leading-[1.1]">
                数字化供应 <span class="text-indigo-600">生命周期</span>
            </h1>
            <p class="text-lg text-slate-500 mt-4 max-w-2xl font-medium leading-relaxed">
                基于神经元拓扑逻辑的精密溯源工作站。实时监控全球供应链流动，捕捉每一个节点的微小波动。
            </p>
          </div>
          <div class="flex items-center gap-2 premium-card rounded-2xl p-1 z-10">
            <button
              v-for="opt in rangeOptions"
              :key="opt.value"
              @click="selectedRange = opt.value"
              :class="[
                'px-4 py-2 text-sm rounded-xl transition-all font-bold',
                selectedRange === opt.value 
                  ? 'bg-indigo-600 text-white shadow-md shadow-indigo-200' 
                  : 'text-slate-500 hover:bg-slate-50'
              ]"
            >
              {{ opt.label }}
            </button>
          </div>
        </div>
    </div>

    <DashboardKPI :kpiData="kpiData" />

    <div class="grid grid-cols-12 gap-8 mt-8">
      <div class="col-span-12 lg:col-span-8 premium-card rounded-[40px] p-8">
        <DashboardMap :mapItems="mapItems" />
      </div>
      <div class="col-span-12 lg:col-span-4 premium-card rounded-[40px] p-8">
        <DashboardTrend :trendItems="trendItems" :trendLabel="getTrendLabel()" />
      </div>
    </div>

    <div class="premium-card rounded-[40px] p-8 mt-8">
      <DashboardTopology :topology="topologyData" />
    </div>
  </div>
</template>
