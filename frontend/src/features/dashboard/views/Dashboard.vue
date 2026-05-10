<script setup>
import { computed, defineAsyncComponent, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import PageHeader from '@/shared/components/ui/PageHeader.vue'
import LoadingSkeleton from '@/shared/components/ui/LoadingSkeleton.vue'
import { getKPI, getMapData, getTrend } from '@/features/dashboard/api'
import { logger } from '@/shared/utils/logger'

/**
 * Dashboard —— Linear-light 仪表盘总览。
 *
 * 视觉契约（与 frontend/preview/linear-dashboard.html 1:1）：
 *   - PageHeader「总览」+ 副标题（实时日期 + 周几 + HH:mm）
 *   - 时间范围 segmented 三档（今日 / 本周 / 本月 → today / 7d / 30d）
 *   - 4 段堆叠：4-up KPI / 2-up (趋势 col-span-2 + 区域分布 col-span-1) / 异常表
 *
 * 子组件均按现有后端契约对齐：
 *   - DashboardKPI ← GET /api/dashboard/kpi
 *   - DashboardTrend ← GET /api/dashboard/trend
 *   - DashboardWorkload ← GET /api/dashboard/map（按 province 聚合）
 *   - DashboardExceptions ← KPI.exceptionCount 占位 + 链接到异常处理页
 *     （近期异常列表完整数据待后端 B34 端点上线）
 */

const DashboardKPI = defineAsyncComponent(() => import('../components/DashboardKPI.vue'))
const DashboardTrend = defineAsyncComponent(() => import('../components/DashboardTrend.vue'))
const DashboardWorkload = defineAsyncComponent(() => import('../components/DashboardWorkload.vue'))
const DashboardExceptions = defineAsyncComponent(() => import('../components/DashboardExceptions.vue'))

const RANGE_OPTIONS = [
  { value: 'today', label: '今日' },
  { value: '7d', label: '本周' },
  { value: '30d', label: '本月' }
]

const RANGE_LABEL = {
  today: '今日',
  '7d': '本周',
  '30d': '本月',
  '180d': '近半年',
  all: '全部'
}

const router = useRouter()

const selectedRange = ref('30d')
const loading = ref(false)
const errorMessage = ref('')

const kpiData = ref({
  totalTraces: 0,
  todayNew: 0,
  totalLogs: 0,
  exceptionCount: 0
})
const mapItems = ref([])
const trendItems = ref([])

const headerSubtitle = ref('')

const trendLabel = computed(() => RANGE_LABEL[selectedRange.value] || '所选周期')
const rangeLabel = computed(() => RANGE_LABEL[selectedRange.value] || '所选周期')

const updateHeaderSubtitle = () => {
  const now = dayjs()
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  headerSubtitle.value = `${now.format('YYYY 年 M 月 D 日')} · ${weekdays[now.day()]} · ${now.format('HH:mm')}`
}

const loadDashboardData = async () => {
  loading.value = true
  errorMessage.value = ''
  const range = selectedRange.value
  try {
    const [kpiResult, mapResult, trendResult] = await Promise.allSettled([
      getKPI(range),
      getMapData(range),
      getTrend(range)
    ])

    if (kpiResult.status === 'fulfilled' && kpiResult.value) {
      kpiData.value = {
        totalTraces: Number(kpiResult.value.totalTraces ?? 0),
        todayNew: Number(kpiResult.value.todayNew ?? 0),
        totalLogs: Number(kpiResult.value.totalLogs ?? 0),
        exceptionCount: Number(kpiResult.value.exceptionCount ?? 0)
      }
    } else if (kpiResult.status === 'rejected') {
      logger.error('仪表盘 KPI 加载失败:', kpiResult.reason)
      errorMessage.value = kpiResult.reason?.message || '仪表盘加载失败'
    }

    if (mapResult.status === 'fulfilled') {
      const items = Array.isArray(mapResult.value?.items) ? mapResult.value.items : []
      mapItems.value = items.map((item) => ({
        name: item.name ?? item.province ?? '',
        value: Number(item.value ?? item.count ?? 0)
      }))
    } else {
      logger.error('仪表盘地区分布加载失败:', mapResult.reason)
      mapItems.value = []
    }

    if (trendResult.status === 'fulfilled') {
      trendItems.value = Array.isArray(trendResult.value?.items) ? trendResult.value.items : []
    } else {
      logger.error('仪表盘趋势加载失败:', trendResult.reason)
      trendItems.value = []
    }
  } catch (err) {
    logger.error('仪表盘加载未知错误:', err)
    errorMessage.value = err?.message || '仪表盘加载失败'
  } finally {
    loading.value = false
  }
}

const handleRangeSelect = (value) => {
  if (value === selectedRange.value) return
  selectedRange.value = value
}

const handleViewAllExceptions = () => {
  router.push({ path: '/traces', query: { status: 'EXCEPTION' } })
}

const handleWorkloadDetail = () => {
  router.push({ path: '/traces' })
}

const handleWorkloadProvince = (provinceName) => {
  if (!provinceName) return
  router.push({ path: '/traces', query: { location: provinceName } })
}

watch(selectedRange, () => {
  loadDashboardData()
})

onMounted(() => {
  updateHeaderSubtitle()
  loadDashboardData()
})
</script>

<template>
  <div class="dashboard">
    <PageHeader title="总览" :subtitle="headerSubtitle">
      <template #actions>
        <div class="dashboard__seg" role="group" aria-label="时间范围">
          <button
            v-for="opt in RANGE_OPTIONS"
            :key="opt.value"
            type="button"
            class="dashboard__seg-btn"
            :class="{ 'dashboard__seg-btn--active': selectedRange === opt.value }"
            :data-testid="`dashboard-range-${opt.value}`"
            @click="handleRangeSelect(opt.value)"
          >{{ opt.label }}</button>
        </div>
      </template>
    </PageHeader>

    <div v-if="errorMessage" class="dashboard__error" data-testid="dashboard-error">
      {{ errorMessage }}
    </div>

    <div v-if="loading" class="dashboard__loading" data-testid="dashboard-loading">
      <LoadingSkeleton type="card" :count="4" />
    </div>

    <template v-else>
      <DashboardKPI :kpi-data="kpiData" />

      <section class="dashboard__row-2">
        <div class="dashboard__col-2">
          <DashboardTrend :trend-items="trendItems" :trend-label="trendLabel" />
        </div>
        <div class="dashboard__col-1">
          <DashboardWorkload
            :items="mapItems"
            :range-label="rangeLabel"
            @view-detail="handleWorkloadDetail"
            @view-province="handleWorkloadProvince"
          />
        </div>
      </section>

      <DashboardExceptions
        :exception-count="kpiData.exceptionCount"
        @view-all="handleViewAllExceptions"
      />
    </template>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.dashboard__seg {
  display: flex;
  align-items: center;
  border: 1px solid var(--hairline);
  border-radius: 6px;
  overflow: hidden;
  height: 32px;
}
.dashboard__seg-btn {
  appearance: none;
  background: var(--surface-1);
  color: var(--ink-subtle);
  height: 100%;
  padding: 0 10px;
  font-size: 12.5px;
  font-weight: 500;
  border: 0;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s, color 0.15s;
  line-height: 1;
}
.dashboard__seg-btn + .dashboard__seg-btn {
  border-left: 1px solid var(--hairline);
}
.dashboard__seg-btn:hover {
  background: var(--surface-2);
}
.dashboard__seg-btn--active {
  background: var(--surface-2);
  color: var(--ink);
}

.dashboard__error {
  padding: 12px 16px;
  background: var(--error-soft);
  color: var(--error);
  border: 1px solid color-mix(in srgb, var(--error) 18%, transparent);
  border-radius: 8px;
  font-size: 13px;
}

.dashboard__loading {
  padding: 4px 0;
}

.dashboard__row-2 {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  min-width: 0;
}
.dashboard__col-2,
.dashboard__col-1 {
  min-width: 0;
}

@media (max-width: 1023px) {
  .dashboard__row-2 {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 640px) {
  .dashboard {
    padding: 16px 12px;
    gap: 16px;
  }
  .dashboard__seg {
    height: 30px;
  }
  .dashboard__seg-btn {
    padding: 0 8px;
    font-size: 12px;
  }
}
</style>
