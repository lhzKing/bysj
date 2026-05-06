<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import ScanFlowDialog from '@/features/trace/components/ScanFlowDialog.vue'
import TraceRouteMap from '@/features/trace/components/TraceRouteMap.vue'
import TraceSummary from '@/features/trace/components/TraceSummary.vue'
import TraceTimeline from '@/features/trace/components/TraceTimeline.vue'
import TraceVerificationPanel from '@/features/trace/components/TraceVerificationPanel.vue'
import { getTraceDetail, verifyTraceChain } from '@/features/trace/api'
import { ArrowLeft, Loader2, Package as PackageIn, PackageOpen as PackageOut, Navigation } from 'lucide-vue-next'
import Menu from 'primevue/menu'

const route = useRoute()
const router = useRouter()
const traceCode = computed(() => route.params.code)

const loading = ref(true)
const error = ref('')
const snapshot = ref(null)
const history = ref([])
const verification = ref(null)
const verifying = ref(false)
const showScanDialog = ref(false)
const selectedActionType = ref('transfer')
const isMenuOpen = ref(false)

const closeMenuDelay = () => {
  setTimeout(() => {
    isMenuOpen.value = false
  }, 200)
}

const loadDetail = async (code = traceCode.value) => {
  try {
    verification.value = null
    const data = await getTraceDetail(code)

    snapshot.value = data.snapshot
    history.value = (data.history || [])
      .sort((a, b) => new Date(b.eventTime) - new Date(a.eventTime))

    await verifyChain(code)
  } catch (err) {
    error.value = err.message || '获取详情失败'
  } finally {
    loading.value = false
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
  loadDetail(traceCode.value)
}

const handleActionSelect = (actionType) => {
  selectedActionType.value = actionType
  showScanDialog.value = true
}

const menu = ref();
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
]);

const toggleMenu = (event) => {
    menu.value.toggle(event);
};

watch(
  () => route.params.code,
  async (newCode) => {
    if (!newCode) {
      error.value = '\u672a\u63d0\u4f9b\u6eaf\u6e90\u7801'
      loading.value = false
      snapshot.value = null
      history.value = []
      verification.value = null
      return
    }

    loading.value = true
    error.value = ''
    await loadDetail(newCode)
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

        <!-- 流转轨迹地图 (Full Width) -->
        <div class="premium-card rounded-[40px] overflow-hidden p-2">
            <TraceRouteMap v-if="history.length > 0" :history="history" class="border-none shadow-none bg-transparent" />
        </div>

        <!-- 生命周期事件流 (Full Width) -->
        <div class="premium-card rounded-[40px] p-6 md:p-10">
          <h3 class="text-2xl font-black text-slate-900 mb-10 tracking-tight">生命周期事件流</h3>
          <TraceTimeline :history="history" />
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
  </div>
</template>
