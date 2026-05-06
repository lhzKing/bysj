<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import BaseCard from '@/shared/components/ui/BaseCard.vue'
import BaseInput from '@/shared/components/ui/BaseInput.vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import QRScanner from '@/shared/components/QRScanner.vue'
import CreateTraceDialog from '@/features/trace/components/CreateTraceDialog.vue'
import { Search, Plus, Expand, Camera } from 'lucide-vue-next'
import { useToast } from '@/shared/composables/useToast'
import { logger } from '@/shared/utils/logger'

const router = useRouter()
const toast = useToast()
const searchQuery = ref('')
const recentTraces = ref([])
const showScanner = ref(false)
const showCreateModal = ref(false)

onMounted(() => {
  const saved = localStorage.getItem('recent_traces')
  if (saved) {
    recentTraces.value = JSON.parse(saved)
  }
})

const handleSearch = () => {
  if (!searchQuery.value) return
  router.push(`/traces/${searchQuery.value}`)
}

// 处理扫码结果
const handleScan = async (traceCode) => {
  try {
    showScanner.value = false
    router.push(`/traces/${traceCode}`)
  } catch (error) {
    logger.error('扫码处理失败:', error)
    toast.error('扫码处理失败: ' + error.message)
  }
}

/**
 * CreateTraceDialog 通过 emit('success', traceCodes) 回传新生成的溯源码数组。
 * 此处只取首条作为"最近交互记录"展示项，并跳转详情页。
 * @param {string[]} traceCodes
 */
const onCreateSuccess = (traceCodes) => {
  if (!traceCodes || traceCodes.length === 0) return
  const newCode = traceCodes[0]
  const newItem = { code: newCode, time: new Date().toISOString() }
  recentTraces.value = [newItem, ...recentTraces.value].slice(0, 20)
  localStorage.setItem('recent_traces', JSON.stringify(recentTraces.value))
  router.push(`/traces/${newCode}`)
}
</script>

<template>
  <div class="max-w-4xl mx-auto py-12 px-4 relative z-10">
    <div class="text-center mb-16 relative">
      <div class="absolute left-1/2 -top-12 -translate-x-1/2 size-40 bg-indigo-200 rounded-full blur-[80px] opacity-40 -z-10"></div>
      <h1 class="text-5xl font-black tracking-tighter text-slate-900 leading-[1.1] mb-6">全链路 <span class="text-indigo-600">溯源引擎</span></h1>
      <p class="text-lg text-slate-500 font-medium">输入核心编码或扫描光学纹理，唤醒全生命周期数字档案</p>
    </div>

    <!-- Search Box -->
    <div class="relative max-w-2xl mx-auto mb-16 premium-card rounded-full p-3 flex gap-3 items-center shadow-lg shadow-indigo-100/50">
      <BaseInput
        v-model="searchQuery"
        placeholder="输入 Trace Code..."
        :icon="Search"
        class="flex-1 bg-transparent border-0 shadow-none text-lg font-mono placeholder:text-slate-400 focus:ring-0 px-4"
        @keyup.enter="handleSearch"
      />
      <button @click="handleSearch" class="h-12 px-8 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-full transition-all shadow-md shadow-indigo-200 hover:shadow-indigo-300">
        神经元检索
      </button>
      <button @click="showScanner = true" class="h-12 w-12 bg-slate-100 hover:bg-indigo-50 hover:text-indigo-600 text-slate-500 font-bold rounded-full transition-all flex items-center justify-center">
        <Camera class="w-5 h-5" />
      </button>
    </div>

    <div class="flex justify-center mb-16">
        <button @click="showCreateModal = true" class="h-12 px-8 bg-white border border-slate-200 hover:border-indigo-300 hover:text-indigo-600 text-slate-600 font-bold rounded-full transition-all shadow-sm flex items-center gap-2">
          <Plus class="w-4 h-4" />
          <span>注入新节点</span>
        </button>
    </div>

    <!-- QR Scanner -->
    <QRScanner
      v-if="showScanner"
      @scan="handleScan"
      @close="showScanner = false"
    />

    <!-- Recent History (Local) -->
    <div v-if="recentTraces.length > 0">
      <div class="flex items-center justify-between mb-6">
        <h3 class="text-sm font-black text-slate-400 uppercase tracking-[0.2em]">最近交互记录 (Recent Traces)</h3>
      </div>
      <div class="space-y-4">
        <div
            v-for="item in recentTraces"
            :key="item.code"
            class="premium-card hover-lift rounded-[32px] p-6 flex items-center justify-between cursor-pointer group"
            @click="router.push('/traces/' + item.code)"
        >
          <div class="flex items-center gap-6">
            <div class="size-14 rounded-2xl bg-indigo-50 flex items-center justify-center text-indigo-600 group-hover:scale-110 transition-transform">
              <Expand class="w-6 h-6" />
            </div>
            <div>
              <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Identifier</p>
              <p class="font-mono text-lg font-black text-slate-900 group-hover:text-indigo-600 transition-colors">{{ item.code }}</p>
            </div>
          </div>
          <div class="flex items-center gap-8">
            <div class="hidden sm:block text-right">
              <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Timestamp</p>
              <p class="text-sm font-bold text-slate-600">{{ new Date(item.time).toLocaleString() }}</p>
            </div>
            <div class="size-10 rounded-full bg-slate-50 flex items-center justify-center text-slate-400 group-hover:bg-indigo-600 group-hover:text-white transition-colors">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path></svg>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 赋码弹窗：复用统一组件 CreateTraceDialog（字段、校验、API 调用都在该组件内） -->
    <CreateTraceDialog
      v-model="showCreateModal"
      @success="onCreateSuccess"
    />
  </div>
</template>

