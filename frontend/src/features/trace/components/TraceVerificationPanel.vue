<script setup>
import { ref } from 'vue'
import BaseCard from '@/shared/components/ui/BaseCard.vue'
import { CheckCircle2, XCircle, ChevronDown, ChevronUp } from 'lucide-vue-next'

const props = defineProps({
  verification: {
    type: Object,
    required: true
  }
})

const showErrorDetails = ref(false)
</script>

<template>
  <div>
    <!-- 验证状态触发器 -->
    <div
      :class="[
        'flex items-center px-4 py-2 rounded-lg text-sm font-medium border transition-colors cursor-pointer',
        verification.valid
          ? 'bg-green-50 text-green-700 border-green-100 hover:bg-green-100'
          : 'bg-red-50 text-red-700 border-red-100 hover:bg-red-100'
      ]"
      @click="!verification.valid && (showErrorDetails = !showErrorDetails)"
    >
      <div class="mr-2">
        <CheckCircle2 v-if="verification.valid" class="w-5 h-5" />
        <XCircle v-else class="w-5 h-5" />
      </div>
      <div class="flex flex-col">
        <span>{{ verification.valid ? '链上数据已验证' : '数据校验失败' }}</span>
        <span class="text-xs opacity-80 font-normal">
          Hash: {{ verification.hashVerifiedCount }}/{{ verification.totalLogs }} 通过
          | 签名: {{ verification.signatureVerifiedCount }}/{{ verification.totalLogs }} 通过
        </span>
      </div>
      <div class="ml-2" v-if="!verification.valid && verification.errors?.length > 0">
        <ChevronUp v-if="showErrorDetails" class="w-5 h-5" />
        <ChevronDown v-else class="w-5 h-5" />
      </div>
    </div>

    <!-- 错误详情面板 -->
    <transition name="slide-down">
      <BaseCard v-if="!verification.valid && showErrorDetails && verification.errors?.length > 0" class="border-l-4 border-red-500 mt-4">
        <div class="flex items-start gap-3 mb-4">
          <XCircle class="w-5 h-5 text-red-500 flex-shrink-0 mt-1" />
          <div>
            <h3 class="text-lg font-semibold text-red-700">证据链校验失败详情</h3>
            <p class="text-sm text-red-600 mt-1">检测到 {{ verification.errors.length }} 个节点存在数据完整性问题</p>
          </div>
        </div>
        
        <div class="space-y-3">
          <div 
            v-for="(err, idx) in verification.errors" 
            :key="idx"
            class="bg-red-50 border border-red-200 rounded-lg p-4"
          >
            <div class="flex items-center justify-between mb-2">
              <span class="font-mono text-sm font-medium text-red-800">
                节点 #{{ err.logId || idx + 1 }}
              </span>
              <span class="text-xs px-2 py-1 bg-red-200 text-red-800 rounded">
                {{ err.errorType || '未知错误' }}
              </span>
            </div>
            
            <p class="text-sm text-red-700 mb-3">{{ err.message || '数据校验失败' }}</p>
            
            <div class="grid grid-cols-1 gap-2 text-xs">
              <div v-if="err.expectedHash" class="flex items-start gap-2">
                <span class="text-red-600 font-medium whitespace-nowrap">期望Hash:</span>
                <span class="font-mono text-red-800 break-all">{{ err.expectedHash }}</span>
              </div>
              <div v-if="err.actualHash" class="flex items-start gap-2">
                <span class="text-red-600 font-medium whitespace-nowrap">实际Hash:</span>
                <span class="font-mono text-red-800 break-all">{{ err.actualHash }}</span>
              </div>
              <div v-if="err.traceCode" class="flex items-start gap-2">
                <span class="text-red-600 font-medium whitespace-nowrap">溯源码:</span>
                <span class="font-mono text-red-800">{{ err.traceCode }}</span>
              </div>
            </div>
          </div>
        </div>
        
        <div class="mt-4 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
          <p class="text-sm text-yellow-800">
            <strong>⚠️ 安全提示：</strong>
            检测到的Hash不匹配或签名失败表明该溯源记录可能已被篡改。建议立即联系管理员进行进一步调查，并暂停该批次产品的流转。
          </p>
        </div>
      </BaseCard>
    </transition>
  </div>
</template>

<style scoped>
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}
.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  max-height: 0;
  transform: translateY(-10px);
}
.slide-down-enter-to,
.slide-down-leave-from {
  opacity: 1;
  max-height: 1000px;
  transform: translateY(0);
}
</style>
