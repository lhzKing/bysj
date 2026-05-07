<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ScanFlowDialog from '@/features/trace/components/ScanFlowDialog.vue'
import TraceCorrectionDialog from '@/features/trace/components/TraceCorrectionDialog.vue'
import TraceExceptionCloseDialog from '@/features/trace/components/TraceExceptionCloseDialog.vue'
import TraceRouteMap from '@/features/trace/components/TraceRouteMap.vue'
import TraceSummary from '@/features/trace/components/TraceSummary.vue'
import TraceTimeline from '@/features/trace/components/TraceTimeline.vue'
import TraceVerificationPanel from '@/features/trace/components/TraceVerificationPanel.vue'
import { getTraceDetail, verifyTraceChain } from '@/features/trace/api'
import { useUserStore } from '@/core/stores/user'
import { PERMISSIONS } from '@/shared/constants'
import { ArrowLeft, Boxes, FilePenLine, Loader2, Package as PackageIn, PackageOpen as PackageOut, Navigation, ShieldCheck } from 'lucide-vue-next'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const traceCode = computed(() => route.params.code)

const loading = ref(true)
const error = ref('')
const snapshot = ref(null)
const history = ref([])
const aggregationHistory = ref([])
const detailView = ref('effective')
const loadedView = ref('effective')
const viewError = ref('')
const switchingView = ref(false)
const verification = ref(null)
const verifying = ref(false)
const showScanDialog = ref(false)
const showExceptionCloseDialog = ref(false)
const showCorrectionDialog = ref(false)
const selectedActionType = ref('transfer')
const isMenuOpen = ref(false)

const canViewAudit = computed(() => userStore.hasPermission(PERMISSIONS.TRACE.AUDIT_VIEW))
const canHandleException = computed(() =>
  userStore.hasPermission(PERMISSIONS.TRACE.EXCEPTION_HANDLE)
    || userStore.hasPermission(PERMISSIONS.TRACE.SCAN)
)
const isExceptionHeld = computed(() => snapshot.value?.currentStatus === 'EXCEPTION')
const historyCount = computed(() => history.value.length)
const aggregationHistoryCount = computed(() => aggregationHistory.value.length)
const correctedLogIds = computed(() => new Set(
  history.value
    .map((log) => log.correctionOf)
    .filter((id) => id !== null && id !== undefined)
    .map((id) => String(id))
))
const correctedOriginalCount = computed(() =>
  history.value.filter((log) => correctedLogIds.value.has(String(log.id))).length
)
const detailViewMeta = computed(() => {
  if (loadedView.value === 'audit') {
    return {
      label: '审计完整视图',
      description: '展示完整 Hash 日志链，包含被纠错覆盖的原始记录。'
    }
  }

  return {
    label: '业务有效视图',
    description: '默认隐藏已被纠错覆盖的原始记录，只呈现当前业务有效历史。'
  }
})

const closeMenuDelay = () => {
  setTimeout(() => {
    isMenuOpen.value = false
  }, 200)
}

const normalizeDetailView = (view) => (view === 'audit' ? 'audit' : 'effective')

const applyDetailData = (data, requestedView) => {
  snapshot.value = data.snapshot
  history.value = (data.history || [])
    .sort((a, b) => new Date(b.eventTime) - new Date(a.eventTime))
  aggregationHistory.value = data.aggregationHistory || []
  loadedView.value = normalizeDetailView(data.view || requestedView)
  detailView.value = loadedView.value
}

const loadDetail = async (code = traceCode.value, view = detailView.value) => {
  const requestedView = normalizeDetailView(view)
  try {
    verification.value = null
    viewError.value = ''
    const data = await getTraceDetail(code, requestedView)

    applyDetailData(data, requestedView)

    await verifyChain(code)
  } catch (err) {
    error.value = err.message || '获取详情失败'
  } finally {
    loading.value = false
  }
}

const switchDetailView = async (view) => {
  const requestedView = normalizeDetailView(view)
  if (requestedView === loadedView.value || switchingView.value) {
    detailView.value = loadedView.value
    return
  }
  if (requestedView === 'audit' && !canViewAudit.value) {
    detailView.value = loadedView.value
    viewError.value = '审计完整视图需要 trace:audit:view 权限'
    return
  }

  const previousView = loadedView.value
  detailView.value = requestedView
  switchingView.value = true
  viewError.value = ''
  verification.value = null
  try {
    const data = await getTraceDetail(traceCode.value, requestedView)
    applyDetailData(data, requestedView)
    await verifyChain(traceCode.value)
  } catch (err) {
    detailView.value = previousView
    loadedView.value = previousView
    verification.value = null
    viewError.value = err.message || '切换详情视图失败'
  } finally {
    switchingView.value = false
  }
}

const verifyChain = async (code = traceCode.value) => {
  verifying.value = true
  try {
    const res = await verifyTraceChain(code)
    verification.value = {
      valid: res.valid,
      totalLogs: res.totalLogs,
      hashVerifiedCount: res.hashVerifiedCount,
      signatureVerifiedCount: res.signatureVerifiedCount,
      errors: res.errors || []
    }
  } catch (e) {
    console.error('Verification failed:', e)
  } finally {
    verifying.value = false
  }
}

const handleScanSuccess = () => {
  loading.value = true
  error.value = ''
  loadDetail(traceCode.value, loadedView.value)
}

const handleActionSelect = (actionType) => {
  selectedActionType.value = actionType
  showScanDialog.value = true
}

const openExceptionCloseDialog = () => {
  showExceptionCloseDialog.value = true
}

const openCorrectionDialog = () => {
  showCorrectionDialog.value = true
}

const formatAggregationTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '至今')

const aggregationStatusClass = (item) => item?.active
  ? 'bg-emerald-50 text-emerald-600 border-emerald-100'
  : 'bg-slate-100 text-slate-500 border-slate-200'

const aggregationScopeLabel = (item) => {
  if (!item) return '-'
  return item.direct ? '直接绑定' : `经 ${item.viaCode || '上级包装'} 关联`
}

const menuItems = ref([
  {
    label: '入库登记',
    icon: PackageIn,
    command: () => handleActionSelect('inbound')
  },
  {
    label: '出库登记',
    icon: PackageOut,
    command: () => handleActionSelect('outbound')
  },
  {
    label: '物流流转',
    icon: Navigation,
    command: () => handleActionSelect('transfer')
  }
])


watch(
  () => route.params.code,
  async (newCode) => {
    if (!newCode) {
      error.value = '\u672a\u63d0\u4f9b\u6eaf\u6e90\u7801'
      loading.value = false
      snapshot.value = null
      history.value = []
      aggregationHistory.value = []
      detailView.value = 'effective'
      loadedView.value = 'effective'
      viewError.value = ''
      verification.value = null
      return
    }

    loading.value = true
    error.value = ''
    detailView.value = 'effective'
    loadedView.value = 'effective'
    viewError.value = ''
    await loadDetail(newCode, 'effective')
  },
  { immediate: true }
)
</script>

<template>
  <div class="max-w-5xl mx-auto py-12 px-4 relative z-10">
    <button
      @click="router.back()"
      class="flex items-center text-sm font-bold text-slate-400 hover:text-indigo-600 transition-colors cursor-pointer mb-8"
    >
      <ArrowLeft class="w-4 h-4 mr-2" /> 返回矩阵
    </button>

    <div v-if="loading" class="text-center py-32 premium-card rounded-[40px] flex flex-col items-center justify-center">
      <Loader2 class="w-10 h-10 text-indigo-600 animate-spin mb-4" />
      <p class="text-sm font-bold text-slate-500 uppercase tracking-widest">Neural Link Connecting...</p>
    </div>

    <div v-else-if="error" class="text-center py-32 premium-card rounded-[40px] border-rose-200">
      <p class="text-rose-500 font-bold mb-6 text-lg">{{ error }}</p>
      <button @click="router.push('/traces')" class="px-8 py-3 bg-indigo-600 text-white rounded-xl font-bold shadow-lg shadow-indigo-200 hover:bg-indigo-700 transition-colors">返回检索</button>
    </div>

    <template v-else>
      <div class="relative mb-12 premium-card rounded-[48px] p-10">
        <div class="absolute -right-12 -top-12 size-60 bg-indigo-200 rounded-full blur-[80px] opacity-40"></div>
        <div class="flex flex-col lg:flex-row lg:items-center justify-between gap-8 relative z-10">
          <div>
            <div class="flex items-center gap-4 mb-4">
              <h1 class="text-3xl md:text-4xl font-black tracking-tight text-slate-900 font-mono break-all">{{ snapshot.traceCode }}</h1>
              <span class="whitespace-nowrap px-4 py-1 rounded-full text-xs font-black bg-indigo-50 text-indigo-600 uppercase tracking-widest border border-indigo-100">
                {{ snapshot.currentStatus }}
              </span>
            </div>
            <p class="text-slate-500 font-bold">关联配件模型: <span class="font-mono text-slate-900 ml-1">SPU-{{ snapshot.spuId }}</span></p>
            <div class="mt-4 flex flex-wrap items-center gap-3">
              <span class="px-4 py-2 rounded-2xl text-xs font-black bg-slate-900 text-white uppercase tracking-widest">
                {{ detailViewMeta.label }}
              </span>
              <span class="text-xs font-bold text-slate-500">{{ detailViewMeta.description }}</span>
            </div>
          </div>

          <div class="flex flex-wrap items-center gap-4">
            <!-- 扫码流转操作 (Custom Dropdown) -->
            <div class="relative">
              <button @click="isMenuOpen = !isMenuOpen" @blur="closeMenuDelay" class="h-12 px-8 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-2xl transition-all shadow-md shadow-indigo-200 flex items-center active:scale-95">
                触发新节点流转
              </button>
              <Transition name="dropdown-fade">
                <div v-if="isMenuOpen" class="absolute right-0 lg:right-auto lg:left-0 top-14 w-48 bg-white/95 backdrop-blur-xl border border-white rounded-[24px] shadow-2xl overflow-hidden z-[100] p-2">
                  <button v-for="item in menuItems" :key="item.label" @click="item.command(); isMenuOpen = false" class="w-full flex items-center px-4 py-3 rounded-xl hover:bg-indigo-50 hover:text-indigo-600 transition-colors text-slate-600 font-bold text-sm">
                    <component :is="item.icon" class="w-4 h-4 mr-3" />
                    <span>{{ item.label }}</span>
                  </button>
                </div>
              </Transition>
            </div>

            <button
              v-if="canHandleException && isExceptionHeld"
              type="button"
              class="h-12 px-6 rounded-2xl bg-emerald-500 hover:bg-emerald-600 text-white font-bold transition-all shadow-md shadow-emerald-100 flex items-center active:scale-95"
              data-testid="trace-exception-close-button"
              @click="openExceptionCloseDialog"
            >
              <ShieldCheck class="mr-2 h-4 w-4" /> 解除冻结
            </button>

            <button
              v-if="canHandleException"
              type="button"
              class="h-12 px-6 rounded-2xl bg-amber-500 hover:bg-amber-600 text-white font-bold transition-all shadow-md shadow-amber-100 flex items-center active:scale-95"
              data-testid="trace-correction-button"
              @click="openCorrectionDialog"
            >
              <FilePenLine class="mr-2 h-4 w-4" /> 审计纠错
            </button>

            <!-- 验证状态 -->
            <TraceVerificationPanel v-if="verification" :verification="verification" />

            <div v-else-if="verifying" class="flex items-center px-6 py-3 rounded-2xl text-sm font-bold text-indigo-600 bg-indigo-50 border border-indigo-100">
              <Loader2 class="w-4 h-4 mr-3 animate-spin" /> 区块链哈希共识校验中...
            </div>
          </div>
        </div>
      </div>

      <div class="flex flex-col gap-8">
        <!-- 摘要区 -->
        <TraceSummary :snapshot="snapshot" />

        <!-- 聚合历史 -->
        <div class="premium-card rounded-[40px] p-6 md:p-10" data-testid="trace-aggregation-history">
          <div class="mb-8 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div>
              <p class="text-xs font-black uppercase tracking-[0.2em] text-slate-400">Aggregation History</p>
              <h3 class="mt-2 text-2xl font-black tracking-tight text-slate-900">箱码 / 托盘码聚合历史</h3>
              <p class="mt-2 text-sm font-bold text-slate-500">
                当前单品共返回 {{ aggregationHistoryCount }} 条聚合关系，包含直接装箱和经箱码关联托盘的历史。
              </p>
            </div>
            <span class="inline-flex w-fit items-center gap-2 rounded-2xl bg-cyan-50 px-4 py-2 text-xs font-black uppercase tracking-widest text-cyan-600">
              <Boxes class="h-4 w-4" /> 一物一码 + 批量流转
            </span>
          </div>

          <div v-if="aggregationHistory.length" class="grid gap-4">
            <article
              v-for="item in aggregationHistory"
              :key="`${item.relationId}-${item.level}-${item.parentCode}`"
              class="rounded-[28px] border border-slate-100 bg-slate-50/70 p-5"
              data-testid="trace-aggregation-history-item"
            >
              <div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
                <div class="min-w-0">
                  <div class="mb-3 flex flex-wrap items-center gap-2">
                    <span class="rounded-full border px-3 py-1 text-[10px] font-black uppercase tracking-widest" :class="aggregationStatusClass(item)">
                      {{ item.active ? '当前有效' : '历史解除' }}
                    </span>
                    <span class="rounded-full bg-white px-3 py-1 text-[10px] font-black text-slate-500 ring-1 ring-slate-100">
                      {{ item.relationTypeLabel || item.relationType }}
                    </span>
                    <span class="rounded-full bg-indigo-50 px-3 py-1 text-[10px] font-black text-indigo-600">
                      {{ aggregationScopeLabel(item) }}
                    </span>
                  </div>
                  <p class="break-all font-mono text-base font-black text-slate-900">
                    {{ item.parentCode }}
                  </p>
                  <p class="mt-2 text-xs font-bold text-slate-500">
                    子码：<span class="font-mono text-slate-700">{{ item.childCode }}</span>
                  </p>
                  <p v-if="item.remark" class="mt-3 text-sm font-bold text-slate-600">
                    {{ item.remark }}
                  </p>
                </div>
                <div class="rounded-2xl bg-white px-4 py-3 text-xs font-bold text-slate-500 shadow-sm md:text-right">
                  <p>绑定：{{ formatAggregationTime(item.bindTime) }}</p>
                  <p class="mt-1">解除：{{ formatAggregationTime(item.releaseTime) }}</p>
                </div>
              </div>
            </article>
          </div>

          <div v-else class="rounded-[28px] border border-dashed border-slate-200 bg-slate-50/70 px-6 py-8 text-center text-sm font-bold text-slate-500">
            当前单品暂无箱码或托盘码聚合历史。
          </div>
        </div>

        <!-- 流转轨迹地图 (Full Width) -->
        <div class="premium-card rounded-[40px] overflow-hidden p-2">
            <TraceRouteMap v-if="history.length > 0" :history="history" class="border-none shadow-none bg-transparent" />
        </div>

        <!-- 生命周期事件流 (Full Width) -->
        <div class="premium-card rounded-[40px] p-6 md:p-10">
          <div class="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-6 mb-10">
            <div>
              <h3 class="text-2xl font-black text-slate-900 tracking-tight">生命周期事件流</h3>
              <p class="mt-2 text-sm font-bold text-slate-500">
                当前返回 {{ historyCount }} 条{{ loadedView === 'audit' ? '完整审计' : '业务有效' }}记录
                <template v-if="loadedView === 'audit' && correctedOriginalCount > 0">
                  ，其中 {{ correctedOriginalCount }} 条原始记录已被纠错覆盖
                </template>
              </p>
            </div>

            <div class="w-full lg:w-auto">
              <div class="flex rounded-[24px] bg-slate-100/80 p-1 border border-slate-200" role="group" aria-label="详情视图切换">
                <button
                  type="button"
                  class="flex-1 lg:flex-none px-5 py-3 rounded-[20px] text-xs font-black uppercase tracking-widest transition-all"
                  :class="loadedView === 'effective' ? 'bg-white text-indigo-600 shadow-sm' : 'text-slate-500 hover:text-slate-900'"
                  :disabled="switchingView"
                  data-testid="trace-detail-effective-tab"
                  @click="switchDetailView('effective')"
                >
                  业务有效
                </button>
                <button
                  v-if="canViewAudit"
                  type="button"
                  class="flex-1 lg:flex-none px-5 py-3 rounded-[20px] text-xs font-black uppercase tracking-widest transition-all"
                  :class="loadedView === 'audit' ? 'bg-white text-indigo-600 shadow-sm' : 'text-slate-500 hover:text-slate-900'"
                  :disabled="switchingView"
                  data-testid="trace-detail-audit-tab"
                  @click="switchDetailView('audit')"
                >
                  审计完整
                </button>
              </div>
              <p v-if="!canViewAudit" class="mt-2 text-xs font-bold text-slate-400 text-right">
                审计完整视图需要 trace:audit:view 权限
              </p>
              <p v-if="viewError" class="mt-2 text-xs font-bold text-rose-500 text-right" data-testid="trace-detail-view-error">
                {{ viewError }}
              </p>
            </div>
          </div>

          <div v-if="switchingView" class="mb-6 flex items-center gap-3 rounded-[24px] border border-indigo-100 bg-indigo-50 px-5 py-4 text-sm font-bold text-indigo-600">
            <Loader2 class="w-4 h-4 animate-spin" /> 正在切换{{ detailView === 'audit' ? '审计完整视图' : '业务有效视图' }}...
          </div>

          <TraceTimeline :history="history" :view="loadedView" />
        </div>
      </div>

    </template>
    
    <!-- 扫码流转弹窗 -->
    <ScanFlowDialog 
      v-model="showScanDialog" 
      :trace-code="traceCode"
      :action-type="selectedActionType"
      @success="handleScanSuccess"
    />

    <TraceExceptionCloseDialog
      v-model="showExceptionCloseDialog"
      :trace-code="traceCode"
      @success="handleScanSuccess"
    />

    <TraceCorrectionDialog
      v-model="showCorrectionDialog"
      :trace-code="traceCode"
      @success="handleScanSuccess"
    />
  </div>
</template>
