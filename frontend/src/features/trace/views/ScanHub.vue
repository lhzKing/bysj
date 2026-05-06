<template>
  <div class="scan-hub relative z-10 flex flex-col items-center justify-center py-12 px-4 min-h-[80vh]">
    <div class="absolute inset-0 grid-accent pointer-events-none opacity-50 -z-20"></div>

    <!-- 扫码器界面 -->
    <TraceScanner 
      v-if="isCameraActive" 
      :currentAction="currentAction"
      @close="stopCamera"
      @scan="onScanSuccess"
    />

    <!-- 操作选择界面（扫码成功后） -->
    <div v-else-if="scannedCode" class="w-full max-w-3xl flex flex-col gap-8 relative z-10">
      <div class="flex items-center justify-between premium-card rounded-[32px] p-6 mb-4">
        <button @click="resetScanner" class="flex items-center text-sm font-bold text-slate-400 hover:text-indigo-600 transition-colors">
          <ArrowLeft class="w-4 h-4 mr-2" /> 重新扫描
        </button>
        <h2 class="text-xl font-black text-slate-900 tracking-tight">操作流转矩阵</h2>
        <div class="w-20"></div>
      </div>

      <div class="premium-card rounded-[48px] p-10 text-center relative overflow-hidden group">
        <div class="absolute -right-12 -top-12 size-40 bg-emerald-200 rounded-full blur-[60px] opacity-40 group-hover:scale-150 transition-transform"></div>
        <div class="relative z-10">
            <div class="size-20 bg-emerald-500 text-white rounded-3xl flex items-center justify-center mx-auto mb-6 shadow-lg shadow-emerald-200 group-hover:rotate-12 transition-transform">
              <Check class="w-10 h-10" />
            </div>
            <p class="text-[10px] font-black text-emerald-600 uppercase tracking-widest mb-2">Neural Scan Successful</p>
            <p class="text-4xl font-mono font-black text-slate-900 tracking-tighter">{{ scannedCode }}</p>
        </div>
      </div>

      <div class="grid grid-cols-2 md:grid-cols-3 gap-6">
        <!-- 查询详情 -->
        <button 
          v-if="hasPermission(PERMISSIONS.TRACE.VIEW)"
          class="col-span-2 md:col-span-3 bg-indigo-600 text-white rounded-[32px] p-8 flex flex-col items-center justify-center gap-3 cursor-pointer transition-all hover:-translate-y-1 hover:shadow-2xl hover:shadow-indigo-200 active:scale-95 group"
          @click="handleViewDetail"
        >
          <Search class="w-10 h-10 group-hover:scale-110 transition-transform" />
          <span class="text-xl font-black tracking-tight">全链路审计详情</span>
          <span class="text-xs font-bold text-indigo-200 uppercase tracking-widest">Full Audit Trail</span>
        </button>

        <!-- 入库 -->
        <button 
          v-if="canUseTraceAction(PERMISSIONS.TRACE.INBOUND)"
          class="premium-card rounded-[32px] p-8 flex flex-col items-center gap-4 cursor-pointer transition-all hover:-translate-y-1 hover:shadow-xl hover:border-emerald-300 active:scale-95 group"
          @click="handleInboundAction"
        >
          <div class="size-14 rounded-2xl bg-emerald-50 flex items-center justify-center text-emerald-600 group-hover:bg-emerald-500 group-hover:text-white transition-colors">
              <PackageIn class="w-6 h-6" />
          </div>
          <div class="text-center">
              <span class="block text-base font-black text-slate-900">入库登记</span>
              <span class="block text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-1">Inbound</span>
          </div>
        </button>

        <!-- 出库 -->
        <button 
          v-if="canUseTraceAction(PERMISSIONS.TRACE.OUTBOUND)"
          class="premium-card rounded-[32px] p-8 flex flex-col items-center gap-4 cursor-pointer transition-all hover:-translate-y-1 hover:shadow-xl hover:border-cyan-300 active:scale-95 group"
          @click="handleOutboundAction"
        >
          <div class="size-14 rounded-2xl bg-cyan-50 flex items-center justify-center text-cyan-600 group-hover:bg-cyan-500 group-hover:text-white transition-colors">
              <PackageOut class="w-6 h-6" />
          </div>
          <div class="text-center">
              <span class="block text-base font-black text-slate-900">出库登记</span>
              <span class="block text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-1">Outbound</span>
          </div>
        </button>

        <!-- 流转 -->
        <button 
          v-if="canUseTraceAction(PERMISSIONS.TRACE.TRANSFER)"
          class="premium-card rounded-[32px] p-8 flex flex-col items-center gap-4 cursor-pointer transition-all hover:-translate-y-1 hover:shadow-xl hover:border-amber-300 active:scale-95 group"
          @click="handleTransferAction"
        >
          <div class="size-14 rounded-2xl bg-amber-50 flex items-center justify-center text-amber-600 group-hover:bg-amber-500 group-hover:text-white transition-colors">
              <Truck class="w-6 h-6" />
          </div>
          <div class="text-center">
              <span class="block text-base font-black text-slate-900">物流流转</span>
              <span class="block text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-1">Transfer</span>
          </div>
        </button>

        <!-- 异常上报 -->
        <button 
          v-if="hasPermission(PERMISSIONS.TRACE.SCAN)"
          class="premium-card rounded-[32px] p-8 flex flex-col items-center gap-4 cursor-pointer transition-all hover:-translate-y-1 hover:shadow-xl hover:border-rose-300 active:scale-95 group"
          @click="handleAlertAction"
        >
          <div class="size-14 rounded-2xl bg-rose-50 flex items-center justify-center text-rose-500 group-hover:bg-rose-500 group-hover:text-white transition-colors">
              <AlertTriangle class="w-6 h-6" />
          </div>
          <div class="text-center">
              <span class="block text-base font-black text-slate-900">异常上报</span>
              <span class="block text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-1">Alert</span>
          </div>
        </button>
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
import { PERMISSIONS } from '@/shared/constants'
import {
  ArrowLeft, Check, Search, Factory, Package as PackageIn,
  PackageOpen as PackageOut, Truck, AlertTriangle, QrCode, Camera
} from 'lucide-vue-next'

const router = useRouter()
const userStore = useUserStore()

const user = computed(() => userStore.user)
const permissions = computed(() => user.value?.permissions || [])

const hasPermission = (perm) => {
  return permissions.value.includes(perm)
}

const hasAnyPermission = (requiredPermissions = []) => {
  return requiredPermissions.some((permission) => permissions.value.includes(permission))
}

const canUseTraceAction = (specificPermission) => {
  return hasAnyPermission([PERMISSIONS.TRACE.SCAN, specificPermission])
}

const isCameraActive = ref(false)
const scannedCode = ref('')
const currentAction = ref('') 
const cameraInitError = ref('') // 新增：专门记录初始化错误

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
}

const resetScanner = () => {
  scannedCode.value = ''
  cameraInitError.value = ''
  startCamera()
}

const onScanSuccess = (code) => {
  scannedCode.value = code
  isCameraActive.value = false
  cameraInitError.value = ''
}

// 修改：增加对子组件传出错误的监听
const onCameraError = (msg) => {
  cameraInitError.value = msg
  isCameraActive.value = false
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

const handleInboundAction = () => {
  currentAction.value = 'inbound'
  showFlowDialog.value = true
}

const handleOutboundAction = () => {
  currentAction.value = 'outbound'
  showFlowDialog.value = true
}

const handleTransferAction = () => {
  currentAction.value = 'transfer'
  showFlowDialog.value = true
}

const handleAlertAction = () => {
  showExceptionDialog.value = true
}

const onFlowSuccess = () => {
  scannedCode.value = ''
  currentAction.value = ''
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
