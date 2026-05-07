<template>
  <div class="scan-hub relative z-10 flex flex-col items-center justify-center py-12 px-4 min-h-[80vh]">
    <div class="absolute inset-0 grid-accent pointer-events-none opacity-50 -z-20"></div>

    <!-- 扫码器界面 -->
    <TraceScanner 
      v-if="isCameraActive" 
      :currentAction="currentAction"
      @close="stopCamera"
      @scan="onScanSuccess"
      @camera-error="onCameraError"
    />

    <!-- 操作选择界面（扫码成功后）：由后端 available-actions 决定可执行动作 -->
    <div v-else-if="scannedCode" class="w-full max-w-4xl flex flex-col gap-8 relative z-10" data-test="scan-action-panel">
      <div class="flex items-center justify-between premium-card rounded-[32px] p-6 mb-4">
        <button @click="resetScanner" class="flex items-center text-sm font-bold text-slate-400 hover:text-indigo-600 transition-colors" data-test="scan-reset">
          <ArrowLeft class="w-4 h-4 mr-2" /> 重新扫描
        </button>
        <h2 class="text-xl font-black text-slate-900 tracking-tight">系统推荐动作</h2>
        <button v-if="!availableActionsLoading" type="button" class="text-xs font-black text-indigo-500 hover:text-indigo-700 transition-colors" data-test="available-actions-refresh" @click="loadAvailableActions(scannedCode)">
          刷新判断
        </button>
        <div v-else class="w-16"></div>
      </div>

      <div class="premium-card rounded-[48px] p-8 md:p-10 relative overflow-hidden group">
        <div class="absolute -right-12 -top-12 size-40 bg-emerald-200 rounded-full blur-[60px] opacity-40 group-hover:scale-150 transition-transform"></div>
        <div class="relative z-10 flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div class="min-w-0">
            <div class="size-16 bg-emerald-500 text-white rounded-3xl flex items-center justify-center mb-5 shadow-lg shadow-emerald-200 group-hover:rotate-12 transition-transform">
              <Check class="w-8 h-8" />
            </div>
            <p class="text-[10px] font-black text-emerald-600 uppercase tracking-widest mb-2">Scan Successful</p>
            <p class="break-all text-2xl md:text-4xl font-mono font-black text-slate-900 tracking-tighter" data-test="scanned-code">{{ scannedCode }}</p>
          </div>
          <div v-if="actionDecision" class="grid min-w-[260px] gap-3 rounded-[28px] bg-white/70 p-4 shadow-inner shadow-emerald-50">
            <div>
              <p class="text-[10px] font-black uppercase tracking-widest text-slate-400">当前状态</p>
              <p class="mt-1 text-sm font-black text-slate-900" data-test="available-current-status">{{ actionDecision.currentStatusLabel || actionDecision.currentStatus || '-' }}</p>
            </div>
            <div>
              <p class="text-[10px] font-black uppercase tracking-widest text-slate-400">当前节点</p>
              <p class="mt-1 text-sm font-black text-slate-900" data-test="available-current-node">{{ actionDecision.currentNode || '-' }}</p>
            </div>
          </div>
        </div>
      </div>

      <div v-if="availableActionsLoading" class="premium-card rounded-[32px] p-10 text-center" data-test="available-actions-loading">
        <Loader2 class="mx-auto h-10 w-10 animate-spin text-indigo-500" />
        <p class="mt-4 text-sm font-black text-slate-500">正在根据当前状态、权限和节点判断可执行动作...</p>
      </div>

      <div v-else class="grid gap-6">
        <div v-if="executableActions.length > 0" class="grid gap-5 md:grid-cols-2" data-test="available-action-list">
          <button
            v-for="action in executableActions"
            :key="action.actionType"
            type="button"
            class="rounded-[32px] p-6 text-left transition-all hover:-translate-y-1 hover:shadow-xl active:scale-95"
            :class="actionCardClass(action)"
            :data-test="`available-action-${action.actionType}`"
            @click="handleAvailableAction(action)"
          >
            <div class="mb-5 flex items-start justify-between gap-4">
              <div class="flex items-center gap-4">
                <div class="size-14 rounded-2xl flex items-center justify-center" :class="actionIconClass(action.actionType)">
                  <component :is="actionIcon(action.actionType)" class="w-6 h-6" />
                </div>
                <div>
                  <p class="text-lg font-black text-slate-900">{{ action.label || formatActionType(action.actionType) }}</p>
                  <p class="mt-1 text-[10px] font-black uppercase tracking-widest text-slate-400">{{ action.actionType }}</p>
                </div>
              </div>
              <span v-if="isRecommendedAction(action)" class="rounded-full bg-indigo-600 px-3 py-1 text-[10px] font-black text-white shadow-lg shadow-indigo-100" data-test="recommended-action-badge">推荐</span>
            </div>
            <p class="text-sm font-bold leading-6 text-slate-500">执行后状态：{{ action.nextStatusLabel || action.nextStatus || '-' }}</p>
            <p class="mt-1 text-xs font-medium text-slate-400">权限要求：{{ action.permissionHint || '后端已校验' }}</p>
            <p v-if="action.requiresRemark" class="mt-3 rounded-2xl bg-amber-50 px-3 py-2 text-xs font-bold text-amber-700">该动作需要填写说明后提交。</p>
          </button>
        </div>

        <div v-else class="premium-card rounded-[32px] border border-amber-100 bg-amber-50/70 p-8" data-test="no-available-actions">
          <div class="flex items-start gap-4">
            <AlertTriangle class="mt-0.5 h-7 w-7 flex-shrink-0 text-amber-600" />
            <div>
              <h3 class="text-xl font-black text-slate-900">当前无可执行扫码动作</h3>
              <p class="mt-2 text-sm font-bold leading-7 text-amber-800" data-test="no-action-reason">{{ noActionMessage }}</p>
            </div>
          </div>
        </div>

        <div class="grid gap-4 md:grid-cols-2">
          <button
            v-if="hasPermission(PERMISSIONS.TRACE.VIEW)"
            class="rounded-[28px] bg-slate-900 p-6 text-white transition-all hover:-translate-y-1 hover:bg-indigo-700 hover:shadow-xl hover:shadow-indigo-100 active:scale-95"
            data-test="scan-view-detail"
            @click="handleViewDetail"
          >
            <Search class="mb-4 w-8 h-8" />
            <span class="block text-lg font-black tracking-tight">查看溯源详情</span>
            <span class="mt-1 block text-xs font-bold text-slate-300 uppercase tracking-widest">Trace Detail</span>
          </button>
          <button
            type="button"
            class="rounded-[28px] border border-slate-200 bg-white/80 p-6 text-left text-slate-600 transition-all hover:-translate-y-1 hover:border-indigo-300 hover:text-indigo-600 hover:shadow-xl active:scale-95"
            data-test="scan-open-task-workbench"
            @click="router.push('/trace-flow-tasks')"
          >
            <Truck class="mb-4 w-8 h-8" />
            <span class="block text-lg font-black tracking-tight text-slate-900">进入任务工作台</span>
            <span class="mt-1 block text-xs font-bold text-slate-400 uppercase tracking-widest">Batch Scan Tasks</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 欢迎界面（初始状态） -->
    <div v-else class="w-full max-w-3xl premium-card rounded-[56px] p-12 text-center relative overflow-hidden group">
      <div class="absolute -left-20 -top-20 size-80 bg-indigo-200 rounded-full blur-[100px] opacity-30 group-hover:opacity-50 transition-opacity"></div>
      
      <div class="relative z-10 flex flex-col items-center justify-center py-10">
        <div class="relative size-40 mb-10 flex items-center justify-center">
            <div class="absolute inset-0 border-[4px] border-dashed border-indigo-200 rounded-[40px] animate-[spin_20s_linear_infinite]"></div>
            <div class="absolute inset-2 border-[2px] border-indigo-100 rounded-[32px] animate-[spin_15s_linear_infinite_reverse]"></div>
            <div class="size-24 bg-gradient-to-br from-indigo-500 to-indigo-600 text-white rounded-3xl flex items-center justify-center shadow-2xl shadow-indigo-300">
                <QrCode class="w-12 h-12" stroke-width="2" />
            </div>
        </div>
        
        <h2 class="text-4xl font-black text-slate-900 tracking-tighter mb-4">光学纹理扫描器</h2>
        <p class="text-lg text-slate-500 font-medium mb-12 max-w-md">Neural Optical Scanner. 点击下方按钮启动物理摄像头模块捕获现实数据。</p>
        
        <button @click="startCamera" class="px-12 py-5 bg-indigo-600 text-white border-none rounded-3xl cursor-pointer flex items-center gap-4 shadow-xl shadow-indigo-200 transition-all hover:-translate-y-1 hover:shadow-2xl hover:shadow-indigo-300 active:scale-95 group/btn">
          <Camera class="w-6 h-6 group-hover/btn:scale-110 transition-transform" />
          <span class="text-lg font-black tracking-tight">唤醒扫描模块</span>
        </button>

        <!-- 次要入口：生产赋码（创建新 traceCode），与扫码（消费已有 traceCode）语义独立 -->
        <div v-if="hasPermission(PERMISSIONS.TRACE.CREATE)" class="mt-8 flex flex-col items-center gap-2">
          <p class="text-xs text-slate-400 font-medium">生产线赋码、批量入库新批次</p>
          <button
            type="button"
            @click="handleOpenCreateTrace"
            class="text-sm font-bold text-indigo-600 underline-offset-4 hover:underline transition-colors flex items-center gap-1.5"
          >
            <Factory class="w-4 h-4" />
            或者：直接生产赋码
          </button>
        </div>
      </div>
    </div>

    <ScanFlowDialog
      v-model="showFlowDialog"
      :trace-code="scannedCode"
      :action-type="currentAction"
      @success="onFlowSuccess"
    />

    <ScanExceptionDialog
      v-model="showExceptionDialog"
      :trace-code="scannedCode"
      @success="onFlowSuccess"
    />

    <CreateTraceDialog
      v-model="showCreateModal"
      @success="onCreateTraceSuccess"
    />
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/core/stores/user'
import TraceScanner from '../components/TraceScanner.vue'
import ScanFlowDialog from '../components/ScanFlowDialog.vue'
import ScanExceptionDialog from '../components/ScanExceptionDialog.vue'
import CreateTraceDialog from '../components/CreateTraceDialog.vue'
import { getTraceAvailableActions } from '@/features/trace/api'
import { useToast } from '@/shared/composables/useToast'
import { logger } from '@/shared/utils/logger'
import { PERMISSIONS } from '@/shared/constants'
import {
  ArrowLeft, Check, Search, Factory, Package as PackageIn,
  PackageOpen as PackageOut, Truck, AlertTriangle, QrCode, Camera, Loader2
} from 'lucide-vue-next'

const router = useRouter()
const userStore = useUserStore()
const toast = useToast()

const user = computed(() => userStore.user)
const permissions = computed(() => user.value?.permissions || [])

const hasPermission = (perm) => {
  return permissions.value.includes(perm)
}

const executableActions = computed(() => actionDecision.value?.availableActions || [])
const noActionMessage = computed(() =>
  availableActionsError.value
    || actionDecision.value?.noActionReason
    || '当前状态、权限或节点不允许执行扫码动作。'
)

const actionTypeToDialogAction = {
  INBOUND: 'inbound',
  OUTBOUND: 'outbound',
  TRANSFER: 'transfer'
}

const isCameraActive = ref(false)
const scannedCode = ref('')
const currentAction = ref('') 
const cameraInitError = ref('') // 新增：专门记录初始化错误
const availableActionsLoading = ref(false)
const actionDecision = ref(null)
const availableActionsError = ref('')

const showFlowDialog = ref(false)
const showExceptionDialog = ref(false)
const showCreateModal = ref(false)

const startCamera = () => {
  cameraInitError.value = ''
  isCameraActive.value = true
  currentAction.value = ''
}

const stopCamera = () => {
  isCameraActive.value = false
  scannedCode.value = ''
  cameraInitError.value = ''
  actionDecision.value = null
  availableActionsError.value = ''
}

const resetScanner = () => {
  scannedCode.value = ''
  cameraInitError.value = ''
  actionDecision.value = null
  availableActionsError.value = ''
  startCamera()
}

const onScanSuccess = async (code) => {
  scannedCode.value = code
  isCameraActive.value = false
  cameraInitError.value = ''
  await loadAvailableActions(code)
}

// 修改：增加对子组件传出错误的监听
const onCameraError = (msg) => {
  cameraInitError.value = msg
  isCameraActive.value = false
}

async function loadAvailableActions(traceCode) {
  const normalizedCode = traceCode?.trim()
  if (!normalizedCode) return
  availableActionsLoading.value = true
  availableActionsError.value = ''
  actionDecision.value = null
  try {
    actionDecision.value = await getTraceAvailableActions(normalizedCode)
  } catch (error) {
    logger.error('加载扫码可执行动作失败', error)
    availableActionsError.value = error?.message || '加载扫码可执行动作失败，请稍后重试'
    toast.error(availableActionsError.value)
  } finally {
    availableActionsLoading.value = false
  }
}

function isRecommendedAction(action) {
  return action?.actionType && action.actionType === actionDecision.value?.recommendedAction
}

function formatActionType(actionType) {
  const labelMap = {
    INBOUND: '确认入库',
    OUTBOUND: '确认出库',
    TRANSFER: '确认流转',
    EXCEPTION: '上报异常',
    CORRECTION: '审计纠错'
  }
  return labelMap[actionType] || actionType || '未知动作'
}

function actionIcon(actionType) {
  if (actionType === 'INBOUND') return PackageIn
  if (actionType === 'OUTBOUND') return PackageOut
  if (actionType === 'TRANSFER') return Truck
  if (actionType === 'EXCEPTION') return AlertTriangle
  return Check
}

function actionIconClass(actionType) {
  if (actionType === 'INBOUND') return 'bg-emerald-50 text-emerald-600'
  if (actionType === 'OUTBOUND') return 'bg-cyan-50 text-cyan-600'
  if (actionType === 'TRANSFER') return 'bg-amber-50 text-amber-600'
  if (actionType === 'EXCEPTION') return 'bg-rose-50 text-rose-500'
  return 'bg-indigo-50 text-indigo-600'
}

function actionCardClass(action) {
  const recommended = isRecommendedAction(action)
  if (recommended) return 'premium-card border-2 border-indigo-300 bg-indigo-50/80 shadow-xl shadow-indigo-100'
  if (action?.actionType === 'EXCEPTION') return 'premium-card border border-rose-100 hover:border-rose-300'
  return 'premium-card border border-slate-200 hover:border-indigo-200'
}

async function handleAvailableAction(action) {
  const actionType = action?.actionType
  if (!actionType) return
  if (actionType === 'EXCEPTION') {
    handleAlertAction()
    return
  }
  if (actionType === 'CORRECTION') {
    toast.error('审计纠错请进入溯源详情的审计流程处理')
    return
  }
  const dialogAction = actionTypeToDialogAction[actionType]
  if (!dialogAction) {
    toast.error(`当前前端暂不支持动作: ${actionType}`)
    return
  }
  currentAction.value = dialogAction
  showFlowDialog.value = true
}

const handleViewDetail = () => {
  router.push(`/traces/${scannedCode.value}`)
}

const handleOpenCreateTrace = () => {
  showCreateModal.value = true
}

const onCreateTraceSuccess = (traceCodes) => {
  showCreateModal.value = false
  const firstCode = Array.isArray(traceCodes) ? traceCodes[0] : null
  if (firstCode) {
    router.push(`/traces/${firstCode}`)
  }
}

const handleAlertAction = () => {
  showExceptionDialog.value = true
}

const onFlowSuccess = () => {
  scannedCode.value = ''
  currentAction.value = ''
  actionDecision.value = null
  availableActionsError.value = ''
  setTimeout(() => {
    startCamera()
  }, 1500)
}

onUnmounted(() => {
  if (isCameraActive.value) {
    stopCamera()
  }
})
</script>
