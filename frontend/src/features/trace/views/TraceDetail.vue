<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  Boxes,
  ChevronDown,
  FilePenLine,
  Loader2,
  Package as PackageIn,
  PackageOpen as PackageOut,
  Navigation,
  QrCode,
  ShieldAlert,
  ShieldCheck,
  ScanLine
} from 'lucide-vue-next'
import dayjs from 'dayjs'
import ScanFlowDialog from '@/features/trace/components/ScanFlowDialog.vue'
import TraceCorrectionDialog from '@/features/trace/components/TraceCorrectionDialog.vue'
import TraceExceptionCloseDialog from '@/features/trace/components/TraceExceptionCloseDialog.vue'
import PrintLabelDialog from '@/features/trace/components/PrintLabelDialog.vue'
import TraceRouteMap from '@/features/trace/components/TraceRouteMap.vue'
import TraceSummary from '@/features/trace/components/TraceSummary.vue'
import TraceTimeline from '@/features/trace/components/TraceTimeline.vue'
import TraceVerificationPanel from '@/features/trace/components/TraceVerificationPanel.vue'
import { getTraceCodeByCode, getTraceDetail, verifyTraceChain } from '@/features/trace/api'
import { canPrint, canReprint } from '@/features/trace/utils/codeActions'
import { useUserStore } from '@/core/stores/user'
import { PERMISSIONS } from '@/shared/constants'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import StatusPill from '@/shared/components/ui/StatusPill.vue'
import TraceCodeChip from '@/shared/components/ui/TraceCodeChip.vue'

const STATUS_TONE = {
  IN_STOCK: 'success',
  ACTIVATED: 'success',
  IN_TRANSIT: 'primary',
  EXCEPTION: 'error',
  VOIDED: 'mute'
}

const STATUS_LABEL = {
  IN_STOCK: '在库',
  ACTIVATED: '已激活',
  IN_TRANSIT: '运输中',
  EXCEPTION: '异常冻结',
  VOIDED: '已作废'
}

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
const verifiedAt = ref(null)
const verifying = ref(false)
const showScanDialog = ref(false)
const showExceptionCloseDialog = ref(false)
const showCorrectionDialog = ref(false)
const showLabelDialog = ref(false)
const codeRecord = ref(null)
const selectedActionType = ref('transfer')
const isMenuOpen = ref(false)
const activeTab = ref('flow')

const canViewAudit = computed(() => userStore.hasPermission(PERMISSIONS.TRACE.AUDIT_VIEW))
const canHandleException = computed(() =>
  userStore.hasPermission(PERMISSIONS.TRACE.EXCEPTION_HANDLE)
    || userStore.hasPermission(PERMISSIONS.TRACE.SCAN)
)
const canCodePrint = computed(() => userStore.hasPermission(PERMISSIONS.TRACE.CODE_PRINT))
const codeIsTerminal = computed(
  () => codeRecord.value && ['VOIDED', 'SCRAPPED'].includes(codeRecord.value.codeStatus)
)
// 按钮 v-if="canCodePrint" 已经先把无 trace:code:print 权限的用户挡在外面——
// 进到这里 mode 只在 print / reprint / view（终态）之间切。
const labelDialogMode = computed(() => {
  if (codeIsTerminal.value) return 'view'
  if (canPrint(codeRecord.value)) return 'print'
  if (canReprint(codeRecord.value)) return 'reprint'
  return 'view'
})
const labelButtonText = computed(() => {
  if (labelDialogMode.value === 'print') return '打印标签'
  if (labelDialogMode.value === 'reprint') return '重打标签'
  return '查看二维码'
})
const labelButtonTooltip = computed(() => {
  if (codeIsTerminal.value) return '该码已作废或已报废，仅支持预览二维码'
  return ''
})
const labelDialogCodes = computed(() => {
  // dialog 内部 qrValueOf 会用 window.location.origin 兜底，无需提前补全 URL
  const fallback = { traceCode: traceCode.value, qrPayload: '' }
  if (!codeRecord.value) return [fallback]
  return [{
    traceCode: codeRecord.value.traceCode || traceCode.value,
    serialNo: codeRecord.value.serialNo,
    qrPayload: codeRecord.value.qrPayload || '',
    codeStatus: codeRecord.value.codeStatus
  }]
})
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
      description: '展示完整 Hash 日志链，包含被纠错覆盖的原始记录'
    }
  }
  return {
    label: '业务有效视图',
    description: '默认隐藏已被纠错覆盖的原始记录，只呈现当前业务有效历史'
  }
})

const statusTone = computed(() => STATUS_TONE[snapshot.value?.currentStatus] || 'mute')
const statusLabel = computed(() => STATUS_LABEL[snapshot.value?.currentStatus] || snapshot.value?.currentStatus || '-')

const auditHistory = computed(() =>
  history.value.filter((log) => {
    const t = log.actionType
    return t === 'CORRECTION' || t === 'EXCEPTION' || t === 'EXCEPTION_OPEN' || t === 'EXCEPTION_CLOSE'
  })
)

const tabs = computed(() => [
  { key: 'flow', label: '流转链路', count: historyCount.value },
  { key: 'part', label: '配件信息', count: null },
  { key: 'chain', label: '链上证明', count: verification.value?.totalLogs || null },
  { key: 'aggregation', label: '关联聚合', count: aggregationHistoryCount.value },
  { key: 'audit', label: '变更记录', count: auditHistory.value.length }
])

const closeMenuDelay = () => {
  setTimeout(() => { isMenuOpen.value = false }, 200)
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
    verifiedAt.value = null
    viewError.value = ''
    const data = await getTraceDetail(code, requestedView)
    applyDetailData(data, requestedView)
    await Promise.all([verifyChain(code), loadCodeRecord(code)])
  } catch (err) {
    error.value = err.message || '获取详情失败'
  } finally {
    loading.value = false
  }
}

const loadCodeRecord = async (code) => {
  // 详情页能展示意味着 trace_snapshot 存在，trace_code 通常也存在；
  // 但 v11 历史回填 / 中途数据修复场景下可能缺失，所以失败时静默退化为 view 模式。
  try {
    codeRecord.value = await getTraceCodeByCode(code)
  } catch (e) {
    codeRecord.value = null
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
  verifiedAt.value = null
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
    verifiedAt.value = new Date()
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
  isMenuOpen.value = false
}

const openExceptionCloseDialog = () => { showExceptionCloseDialog.value = true }
const openCorrectionDialog = () => { showCorrectionDialog.value = true }

const formatAggregationTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '至今')
const aggregationScopeLabel = (item) => {
  if (!item) return '-'
  return item.direct ? '直接绑定' : `经 ${item.viaCode || '上级包装'} 关联`
}

const menuItems = [
  // 每条动作都标 perms：用户至少有其一才会看到这条菜单项；trace:scan 是 super 权限覆盖所有扫码动作
  { key: 'inbound', label: '入库登记', icon: PackageIn, perms: [PERMISSIONS.TRACE.INBOUND, PERMISSIONS.TRACE.SCAN] },
  { key: 'outbound', label: '出库登记', icon: PackageOut, perms: [PERMISSIONS.TRACE.OUTBOUND, PERMISSIONS.TRACE.SCAN] },
  { key: 'transfer', label: '物流流转', icon: Navigation, perms: [PERMISSIONS.TRACE.TRANSFER, PERMISSIONS.TRACE.SCAN] }
]
// 没有任何登记权限的用户（如 USER 只读角色）不应该看到"登记动作"按钮，
// 否则点开是个空 dropdown 或后端 403 上链失败，体验和安全都不好
const visibleMenuItems = computed(() =>
  menuItems.filter((item) => userStore.hasAnyPermission(item.perms))
)
const canRegisterAction = computed(() => visibleMenuItems.value.length > 0)

watch(
  () => route.params.code,
  async (newCode) => {
    if (!newCode) {
      error.value = '未提供溯源码'
      loading.value = false
      snapshot.value = null
      history.value = []
      aggregationHistory.value = []
      detailView.value = 'effective'
      loadedView.value = 'effective'
      viewError.value = ''
      verification.value = null
      verifiedAt.value = null
      codeRecord.value = null
      return
    }
    loading.value = true
    error.value = ''
    detailView.value = 'effective'
    loadedView.value = 'effective'
    viewError.value = ''
    activeTab.value = 'flow'
    codeRecord.value = null
    await loadDetail(newCode, 'effective')
  },
  { immediate: true }
)
</script>

<template>
  <div class="trace-detail">
    <button type="button" class="trace-detail__back" @click="router.back()">
      <ArrowLeft class="trace-detail__back-icon" />
      返回
    </button>

    <div v-if="loading" class="trace-detail__state">
      <Loader2 class="trace-detail__state-icon trace-detail__state-icon--spin" />
      <p class="trace-detail__state-title">正在校验链上凭证…</p>
      <p class="trace-detail__state-subtitle">将依次拉取详情、流转记录与签名校验结果。</p>
    </div>

    <div v-else-if="error" class="trace-detail__state trace-detail__state--error">
      <ShieldAlert class="trace-detail__state-icon trace-detail__state-icon--error" />
      <p class="trace-detail__state-title">{{ error }}</p>
      <BaseButton variant="primary" size="md" @click="router.push('/traces')">返回追溯查询</BaseButton>
    </div>

    <template v-else>
      <!-- Header card -->
      <section class="trace-detail__header">
        <div class="trace-detail__header-row">
          <div class="trace-detail__header-lead">
            <div class="trace-detail__header-pills">
              <StatusPill :tone="statusTone">{{ statusLabel }}</StatusPill>
              <StatusPill v-if="snapshot?.currentNode" tone="mute" :dot="false">{{ snapshot.currentNode }}</StatusPill>
              <span v-if="snapshot?.lastEventTime" class="trace-detail__header-meta">
                最近更新 {{ dayjs(snapshot.lastEventTime).format('YYYY-MM-DD HH:mm') }}
              </span>
            </div>
            <div class="trace-detail__header-code">
              <TraceCodeChip :code="snapshot?.traceCode || ''" size="xl" />
            </div>
            <p v-if="snapshot?.spuId" class="trace-detail__header-spu mono">
              SPU-{{ snapshot.spuId }}<span v-if="snapshot.currentOwner"> · 持有 {{ snapshot.currentOwner }}</span>
            </p>
          </div>

          <div class="trace-detail__header-actions">
            <div class="trace-detail__view-toggle" role="group" aria-label="详情视图切换">
              <button
                type="button"
                class="trace-detail__view-tab"
                :class="{ 'trace-detail__view-tab--active': loadedView === 'effective' }"
                :disabled="switchingView"
                data-testid="trace-detail-effective-tab"
                @click="switchDetailView('effective')"
              >
                业务有效
              </button>
              <button
                v-if="canViewAudit"
                type="button"
                class="trace-detail__view-tab"
                :class="{ 'trace-detail__view-tab--active': loadedView === 'audit' }"
                :disabled="switchingView"
                data-testid="trace-detail-audit-tab"
                @click="switchDetailView('audit')"
              >
                审计完整
              </button>
            </div>

            <div v-if="canRegisterAction" class="trace-detail__menu">
              <BaseButton variant="primary" size="sm" @click="isMenuOpen = !isMenuOpen" @blur="closeMenuDelay">
                <template #icon><ScanLine class="trace-detail__menu-icon" /></template>
                登记动作
                <template #kbd><ChevronDown class="trace-detail__menu-icon" /></template>
              </BaseButton>
              <Transition name="trace-detail-dropdown">
                <div v-if="isMenuOpen" class="trace-detail__menu-pop">
                  <button
                    v-for="item in visibleMenuItems"
                    :key="item.key"
                    type="button"
                    class="trace-detail__menu-item"
                    @mousedown.prevent="handleActionSelect(item.key)"
                  >
                    <component :is="item.icon" class="trace-detail__menu-icon" />
                    {{ item.label }}
                  </button>
                </div>
              </Transition>
            </div>

            <BaseButton
              v-if="canCodePrint"
              variant="secondary"
              size="sm"
              :title="labelButtonTooltip || undefined"
              data-testid="trace-detail-label-button"
              :data-mode="labelDialogMode"
              @click="showLabelDialog = true"
            >
              <template #icon><QrCode class="trace-detail__menu-icon" /></template>
              {{ labelButtonText }}
            </BaseButton>

            <BaseButton
              v-if="canHandleException && isExceptionHeld"
              variant="secondary"
              size="sm"
              data-testid="trace-exception-close-button"
              @click="openExceptionCloseDialog"
            >
              <template #icon><ShieldCheck class="trace-detail__menu-icon" /></template>
              解除冻结
            </BaseButton>

            <BaseButton
              v-if="canHandleException"
              variant="secondary"
              size="sm"
              data-testid="trace-correction-button"
              @click="openCorrectionDialog"
            >
              <template #icon><FilePenLine class="trace-detail__menu-icon" /></template>
              审计纠错
            </BaseButton>
          </div>
        </div>

        <p class="trace-detail__view-meta">
          <span class="trace-detail__view-meta-label">{{ detailViewMeta.label }}</span>
          <span class="trace-detail__view-meta-desc">{{ detailViewMeta.description }}</span>
        </p>

        <p v-if="viewError" class="trace-detail__view-error" data-testid="trace-detail-view-error">
          {{ viewError }}
        </p>

        <div class="trace-detail__verify">
          <TraceVerificationPanel
            v-if="verification"
            :verification="verification"
            :verified-at="verifiedAt"
          />
          <div v-else-if="verifying" class="trace-detail__verify-loading">
            <Loader2 class="trace-detail__verify-loading-icon" />
            <span>正在校验链上哈希与 RSA 签名…</span>
          </div>
        </div>
      </section>

      <!-- Tabs card -->
      <section class="trace-detail__tabs-card">
        <nav class="trace-detail__tabs" role="tablist">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            type="button"
            role="tab"
            class="trace-detail__tab"
            :class="{ 'trace-detail__tab--active': activeTab === tab.key }"
            :aria-selected="activeTab === tab.key"
            :data-testid="`trace-detail-tab-${tab.key}`"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
            <span v-if="tab.count !== null" class="trace-detail__tab-count">{{ tab.count }}</span>
          </button>
        </nav>

        <div v-if="switchingView" class="trace-detail__loading-bar">
          <Loader2 class="trace-detail__verify-loading-icon" />
          <span>正在切换{{ detailView === 'audit' ? '审计完整视图' : '业务有效视图' }}…</span>
        </div>

        <!-- 流转链路 tab -->
        <div v-show="activeTab === 'flow'" class="trace-detail__tab-body trace-detail__flow-grid">
          <div class="trace-detail__flow-main">
            <div class="trace-detail__flow-header">
              <div>
                <p class="trace-detail__eyebrow">流转链路</p>
                <p class="trace-detail__caption">
                  当前返回 {{ historyCount }} 条{{ loadedView === 'audit' ? '完整审计' : '业务有效' }}记录<template v-if="loadedView === 'audit' && correctedOriginalCount > 0">，其中 {{ correctedOriginalCount }} 条原始记录已被纠错覆盖</template>
                </p>
              </div>
            </div>
            <TraceTimeline :history="history" :view="loadedView" />
          </div>
          <aside class="trace-detail__flow-aside">
            <div class="trace-detail__aside-block">
              <p class="trace-detail__eyebrow">配件信息</p>
              <TraceSummary :snapshot="snapshot" :history-count="historyCount" layout="compact" />
            </div>
            <div class="trace-detail__aside-block trace-detail__aside-block--bordered">
              <TraceRouteMap v-if="history.length > 0" :history="history" />
              <div v-else class="trace-detail__map-empty">
                <Boxes class="trace-detail__map-empty-icon" />
                <p>暂无可绘制的流转坐标</p>
              </div>
            </div>
          </aside>
        </div>

        <!-- 配件信息 tab -->
        <div v-show="activeTab === 'part'" class="trace-detail__tab-body">
          <p class="trace-detail__eyebrow">配件信息</p>
          <TraceSummary :snapshot="snapshot" :history-count="historyCount" layout="full" />
        </div>

        <!-- 链上证明 tab -->
        <div v-show="activeTab === 'chain'" class="trace-detail__tab-body">
          <div class="trace-detail__chain-stripe">
            <TraceVerificationPanel
              v-if="verification"
              :verification="verification"
              :verified-at="verifiedAt"
            />
            <div v-else class="trace-detail__verify-loading">
              <Loader2 class="trace-detail__verify-loading-icon" />
              <span>正在校验链上哈希与 RSA 签名…</span>
            </div>
          </div>
          <p class="trace-detail__eyebrow trace-detail__chain-title">每条事件的哈希凭证</p>
          <TraceTimeline :history="history" :view="loadedView" />
        </div>

        <!-- 关联聚合 tab -->
        <div v-show="activeTab === 'aggregation'" class="trace-detail__tab-body" data-testid="trace-aggregation-history">
          <div class="trace-detail__aggregation-header">
            <div>
              <p class="trace-detail__eyebrow">箱码 / 托盘码聚合历史</p>
              <p class="trace-detail__caption">
                当前单品共返回 {{ aggregationHistoryCount }} 条聚合关系，包含直接装箱与经箱码关联托盘的历史。
              </p>
            </div>
            <StatusPill tone="primary" :dot="false">
              <Boxes class="trace-detail__aggregation-icon" />
              一物一码 + 批量流转
            </StatusPill>
          </div>

          <ul v-if="aggregationHistory.length" class="trace-detail__aggregation-list">
            <li
              v-for="item in aggregationHistory"
              :key="`${item.relationId}-${item.level}-${item.parentCode}`"
              class="trace-detail__aggregation-item"
              data-testid="trace-aggregation-history-item"
            >
              <div class="trace-detail__aggregation-row">
                <div class="trace-detail__aggregation-pills">
                  <StatusPill :tone="item.active ? 'success' : 'mute'">
                    {{ item.active ? '当前有效' : '历史解除' }}
                  </StatusPill>
                  <StatusPill tone="mute" :dot="false">
                    {{ item.relationTypeLabel || item.relationType }}
                  </StatusPill>
                  <StatusPill tone="primary" :dot="false">
                    {{ aggregationScopeLabel(item) }}
                  </StatusPill>
                </div>
                <p class="trace-detail__aggregation-parent mono">{{ item.parentCode }}</p>
                <p class="trace-detail__aggregation-child">
                  子码：<span class="mono">{{ item.childCode }}</span>
                </p>
                <p v-if="item.remark" class="trace-detail__aggregation-remark">{{ item.remark }}</p>
              </div>
              <dl class="trace-detail__aggregation-meta">
                <div><dt>绑定</dt><dd class="mono">{{ formatAggregationTime(item.bindTime) }}</dd></div>
                <div><dt>解除</dt><dd class="mono">{{ formatAggregationTime(item.releaseTime) }}</dd></div>
              </dl>
            </li>
          </ul>

          <div v-else class="trace-detail__aggregation-empty">
            当前单品暂无箱码或托盘码聚合历史。
          </div>
        </div>

        <!-- 变更记录 tab -->
        <div v-show="activeTab === 'audit'" class="trace-detail__tab-body">
          <p class="trace-detail__eyebrow">变更记录</p>
          <p class="trace-detail__caption">仅展示 CORRECTION / EXCEPTION 类事件，便于审计追溯异常与纠错链路。</p>
          <div v-if="auditHistory.length" class="trace-detail__audit-block">
            <TraceTimeline :history="auditHistory" :view="loadedView" />
          </div>
          <div v-else class="trace-detail__aggregation-empty">
            该追溯码暂无异常或纠错事件。
          </div>
        </div>
      </section>
    </template>

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

    <PrintLabelDialog
      v-model="showLabelDialog"
      :codes="labelDialogCodes"
      :mode="labelDialogMode"
      :title="labelButtonText"
      data-testid="trace-detail-label-dialog"
    />
  </div>
</template>

<style scoped>
.trace-detail {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 24px 48px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.trace-detail__back {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: 0;
  padding: 4px 8px;
  margin: -4px -8px 0;
  font-size: 12.5px;
  color: var(--ink-subtle);
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.15s, color 0.15s;
}
.trace-detail__back:hover {
  color: var(--ink);
  background: var(--surface-2);
}
.trace-detail__back-icon {
  width: 14px;
  height: 14px;
}

.trace-detail__state {
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  padding: 56px 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  text-align: center;
}
.trace-detail__state--error {
  border-color: #f8c8ca;
  background: var(--error-soft);
}
.trace-detail__state-icon {
  width: 32px;
  height: 32px;
  color: var(--ink-subtle);
}
.trace-detail__state-icon--spin { animation: trace-spin 0.9s linear infinite; color: var(--primary); }
.trace-detail__state-icon--error { color: var(--error); }
.trace-detail__state-title {
  margin: 4px 0 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--ink);
}
.trace-detail__state-subtitle {
  margin: 0 0 4px;
  font-size: 12.5px;
  color: var(--ink-subtle);
}

.trace-detail__header {
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.trace-detail__header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.trace-detail__header-lead {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.trace-detail__header-pills {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.trace-detail__header-meta {
  font-size: 12px;
  color: var(--ink-subtle);
}

.trace-detail__header-code {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.trace-detail__header-spu {
  margin: 0;
  font-size: 12px;
  color: var(--ink-subtle);
}

.trace-detail__header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.trace-detail__view-toggle {
  display: inline-flex;
  background: var(--surface-2);
  border: 1px solid var(--hairline);
  border-radius: 8px;
  padding: 2px;
}
.trace-detail__view-tab {
  height: 28px;
  padding: 0 10px;
  border-radius: 6px;
  background: transparent;
  border: 0;
  font-size: 12px;
  font-weight: 500;
  color: var(--ink-subtle);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.trace-detail__view-tab:disabled { cursor: not-allowed; opacity: 0.6; }
.trace-detail__view-tab--active {
  background: var(--surface-1);
  color: var(--ink);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
}

.trace-detail__menu {
  position: relative;
  display: inline-flex;
}
.trace-detail__menu-icon {
  width: 13px;
  height: 13px;
}
.trace-detail__menu-pop {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  min-width: 180px;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 8px;
  padding: 4px;
  box-shadow: 0 6px 20px -8px rgba(15, 23, 42, 0.15);
  z-index: 30;
  display: flex;
  flex-direction: column;
}
.trace-detail__menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: transparent;
  border: 0;
  border-radius: 6px;
  font-size: 13px;
  color: var(--ink);
  cursor: pointer;
  text-align: left;
  transition: background 0.15s;
}
.trace-detail__menu-item:hover {
  background: var(--surface-2);
}

.trace-detail-dropdown-enter-active,
.trace-detail-dropdown-leave-active {
  transition: opacity 0.15s, transform 0.15s;
}
.trace-detail-dropdown-enter-from,
.trace-detail-dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.trace-detail__view-meta {
  margin: 0;
  display: flex;
  align-items: baseline;
  gap: 10px;
  font-size: 12.5px;
  color: var(--ink-subtle);
}
.trace-detail__view-meta-label {
  color: var(--ink);
  font-weight: 500;
}
.trace-detail__view-meta-desc {
  color: var(--ink-subtle);
}

.trace-detail__view-error {
  margin: 0;
  font-size: 12.5px;
  color: var(--error);
  background: var(--error-soft);
  border: 1px solid #f8c8ca;
  border-radius: 6px;
  padding: 6px 10px;
}

.trace-detail__verify {
  margin-top: 4px;
}
.trace-detail__verify-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: var(--primary);
  background: var(--primary-soft);
  border: 1px solid #d9def5;
  border-radius: 8px;
  padding: 10px 12px;
}
.trace-detail__verify-loading-icon {
  width: 14px;
  height: 14px;
  animation: trace-spin 0.9s linear infinite;
}
@keyframes trace-spin { to { transform: rotate(360deg); } }

.trace-detail__tabs-card {
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  overflow: hidden;
}

.trace-detail__tabs {
  padding: 0 16px;
  border-bottom: 1px solid var(--hairline);
  display: flex;
  align-items: center;
  gap: 4px;
  overflow-x: auto;
  scrollbar-width: none;
  /* On narrow viewports the 5 tabs overflow; the right-edge fade hints
     that the strip is horizontally scrollable. */
  -webkit-mask-image: linear-gradient(to right, #000 0, #000 calc(100% - 24px), transparent 100%);
  mask-image: linear-gradient(to right, #000 0, #000 calc(100% - 24px), transparent 100%);
}
.trace-detail__tabs::-webkit-scrollbar { display: none; }

.trace-detail__tab {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 12px 12px 11px;
  background: transparent;
  border: 0;
  border-bottom: 2px solid transparent;
  font-size: 13px;
  font-weight: 500;
  color: var(--ink-subtle);
  cursor: pointer;
  white-space: nowrap;
  transition: color 0.15s, border-color 0.15s;
}
.trace-detail__tab:hover { color: var(--ink); }
.trace-detail__tab--active {
  color: var(--ink);
  border-bottom-color: var(--ink);
}
.trace-detail__tab-count {
  font-size: 11px;
  color: var(--ink-subtle);
  background: var(--surface-2);
  padding: 1px 6px;
  border-radius: 9999px;
  font-weight: 500;
}
.trace-detail__tab--active .trace-detail__tab-count {
  background: var(--primary-soft);
  color: var(--primary);
}

.trace-detail__loading-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: var(--primary);
  background: var(--primary-soft);
  padding: 10px 24px;
  border-bottom: 1px solid var(--hairline);
}

.trace-detail__tab-body {
  padding: 20px 24px;
}

.trace-detail__flow-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  gap: 0;
  padding: 0;
}
.trace-detail__flow-main {
  padding: 20px 24px;
  border-right: 1px solid var(--hairline);
  min-width: 0;
}
.trace-detail__flow-aside {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.trace-detail__aside-block {
  padding: 20px 24px;
}
.trace-detail__aside-block--bordered {
  border-top: 1px solid var(--hairline);
}

.trace-detail__flow-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.trace-detail__eyebrow {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: var(--ink-subtle);
  margin: 0 0 8px 0;
}
.trace-detail__caption {
  margin: 0;
  font-size: 12.5px;
  color: var(--ink-muted);
}

.trace-detail__chain-stripe {
  margin-bottom: 18px;
}
.trace-detail__chain-title {
  margin-top: 12px;
}

.trace-detail__map-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 32px 16px;
  border: 1px dashed var(--hairline);
  border-radius: 8px;
  background: var(--surface-2);
  color: var(--ink-subtle);
  font-size: 12.5px;
}
.trace-detail__map-empty-icon {
  width: 24px;
  height: 24px;
  color: var(--ink-tertiary);
}

.trace-detail__aggregation-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.trace-detail__aggregation-icon {
  width: 12px;
  height: 12px;
  margin-right: 4px;
}
.trace-detail__aggregation-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.trace-detail__aggregation-item {
  display: flex;
  align-items: stretch;
  gap: 16px;
  padding: 16px;
  border: 1px solid var(--hairline);
  border-radius: 8px;
  background: var(--surface-1);
}
.trace-detail__aggregation-row {
  flex: 1 1 auto;
  min-width: 0;
}
.trace-detail__aggregation-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}
.trace-detail__aggregation-parent {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  margin: 0 0 4px 0;
  word-break: break-all;
}
.trace-detail__aggregation-child {
  font-size: 12px;
  color: var(--ink-muted);
  margin: 0;
}
.trace-detail__aggregation-remark {
  font-size: 12.5px;
  color: var(--ink-muted);
  margin: 8px 0 0 0;
}

.trace-detail__aggregation-meta {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 12px;
  background: var(--surface-2);
  border: 1px solid var(--hairline);
  border-radius: 6px;
  font-size: 11.5px;
  color: var(--ink-muted);
  margin: 0;
}
.trace-detail__aggregation-meta div {
  display: flex;
  gap: 6px;
}
.trace-detail__aggregation-meta dt {
  color: var(--ink-subtle);
  flex-shrink: 0;
}
.trace-detail__aggregation-meta dd {
  margin: 0;
  color: var(--ink-muted);
}

.trace-detail__aggregation-empty {
  border: 1px dashed var(--hairline);
  border-radius: 8px;
  padding: 32px 24px;
  background: var(--surface-2);
  text-align: center;
  font-size: 12.5px;
  color: var(--ink-subtle);
}

.trace-detail__audit-block {
  padding-top: 12px;
}

.mono {
  font-family: 'JetBrains Mono', ui-monospace, 'SF Mono', Menlo, monospace;
}

@media (max-width: 1023px) {
  .trace-detail__flow-grid {
    grid-template-columns: 1fr;
  }
  .trace-detail__flow-main {
    border-right: 0;
    border-bottom: 1px solid var(--hairline);
  }
}

@media (max-width: 640px) {
  .trace-detail {
    padding: 16px 12px 32px;
  }
  .trace-detail__header,
  .trace-detail__tab-body,
  .trace-detail__flow-main,
  .trace-detail__aside-block {
    padding-left: 16px;
    padding-right: 16px;
  }
  .trace-detail__header-actions {
    width: 100%;
  }
  .trace-detail__view-toggle {
    flex: 1 1 auto;
  }
  .trace-detail__view-tab {
    flex: 1 1 0;
  }
  .trace-detail__aggregation-item {
    flex-direction: column;
  }
  .trace-detail__aggregation-meta {
    flex-direction: row;
    flex-wrap: wrap;
  }
}
</style>
