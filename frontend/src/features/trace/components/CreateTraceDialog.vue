<template>
  <teleport to="body">
    <div 
      v-if="modelValue" 
      class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50 backdrop-blur-sm"
      @click.self="handleClose"
    >
      <div class="bg-white rounded-lg shadow-xl w-full max-w-lg mx-4">
        <!-- Header -->
        <div class="bg-gradient-to-r from-indigo-500 to-purple-500 text-white px-6 py-4 flex items-center justify-between rounded-t-lg">
          <h3 class="text-lg font-semibold flex items-center gap-2">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            生产赋码
          </h3>
          <div 
            @click="handleClose"
            class="text-white/80 hover:text-white transition-colors cursor-pointer"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
        </div>

        <!-- Body -->
        <div class="px-6 py-4 space-y-4">
          <div class="bg-indigo-50 border border-indigo-200 rounded-lg p-3 flex items-start gap-3">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" class="flex-shrink-0 mt-0.5 text-indigo-500">
              <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
              <path d="M12 16V12M12 8H12.01" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
            <div class="text-sm text-indigo-700">
              <p class="font-medium mb-1">赋码说明</p>
              <p class="text-indigo-600">为新生产的零配件批次生成唯一溯源码，系统将自动记录生产信息和初始哈希值。</p>
            </div>
          </div>

          <!-- 错误提示 -->
          <div v-if="error" class="bg-red-50 border border-red-200 rounded-lg p-3 text-sm text-red-700">
            {{ error }}
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">
              产品（选择配件） <span class="text-red-500">*</span>
            </label>
            <div v-if="partsLoading" class="text-sm text-gray-500">正在加载配件列表...</div>
            <select v-else v-model="formData.partCode" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500">
              <option value="">请选择配件（按编码/名称）</option>
              <option v-for="p in parts" :key="p.id" :value="p.partCode">{{ p.partCode }} - {{ p.partName }}</option>
            </select>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">
              生产数量 <span class="text-red-500">*</span>
            </label>
            <input 
              v-model="formData.quantity" 
              type="number"
              min="1"
              placeholder="请输入生产数量"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">
              生产节点 <span class="text-red-500">*</span>
            </label>
            <input 
              v-model="formData.manufacturerNode" 
              type="text"
              placeholder="例如：北京工厂、上海生产线A"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">
                省份 <span class="text-red-500">*</span>
              </label>
              <input 
                v-model="formData.province" 
                type="text"
                placeholder="例如：北京"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">
                城市 <span class="text-red-500">*</span>
              </label>
              <input 
                v-model="formData.city" 
                type="text"
                placeholder="例如：北京市"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="bg-gray-50 border-t border-gray-200 px-6 py-4 flex justify-end gap-3 rounded-b-lg">
          <button 
            @click="handleClose"
            class="px-4 py-2 text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
          >
            取消
          </button>
          <button 
            @click="handleSubmit" 
            :disabled="submitting"
            class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
          >
            <svg v-if="submitting" width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" class="animate-spin">
              <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" stroke-opacity="0.25"/>
              <path d="M12 2C6.47715 2 2 6.47715 2 12" stroke="currentColor" stroke-width="4"/>
            </svg>
            <span>{{ submitting ? '生成中...' : '生成溯源码' }}</span>
          </button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { ref, watch } from 'vue'
import { createTrace } from '../api'
import { getParts } from '@/features/part/api'

const props = defineProps({
  modelValue: Boolean
})

/**
 * 组件向外暴露的事件契约：
 * - `update:modelValue`：v-model 同步弹窗显隐（参数：boolean）
 * - `success`：赋码请求成功后回传**新生成的溯源码字符串数组**——
 *   后端 createTrace 接口 (ProduceAssignRequest) 支持 quantity>=1 批量生成，故签名为 `string[]`，
 *   消费方按需取首条或全部展示。已被 `CreateTraceDialog.contract.test.js` 锁定。
 *   示例：emit('success', ['TRACE-001', 'TRACE-002'])
 */
const emit = defineEmits(['update:modelValue', 'success'])

const submitting = ref(false)
const error = ref('')

const formData = ref({
  spuId: '', // 兼容老字段，优先使用 partCode
  partCode: '',
  quantity: 1,
  manufacturerNode: '',
  province: '',
  city: ''
})

const parts = ref([])
const partsLoading = ref(false)

const loadParts = async () => {
  partsLoading.value = true
  try {
    const res = await getParts({ page: 1, size: 100 })
    // res 格式为 { list: [...], total, page, size }
    parts.value = res.list || []
  } catch (e) {
    // ignore - 前端不阻塞赋码窗口
    console.error('加载配件失败', e)
  } finally {
    partsLoading.value = false
  }
}

const validateForm = () => {
  // 校验配件标识（支持 partCode 或 spuId）
  const hasPart = (formData.value.partCode && formData.value.partCode.trim()) || (formData.value.spuId && !isNaN(Number(formData.value.spuId)));
  if (!hasPart) {
    error.value = '请选择一个配件或输入有效的产品 ID'
    return false
  }
  if (!formData.value.quantity || Number(formData.value.quantity) < 1) {
    error.value = '数量必须大于 0'
    return false
  }
  if (!formData.value.manufacturerNode?.trim()) {
    error.value = '请输入生产节点'
    return false
  }
  if (!formData.value.province?.trim()) {
    error.value = '请输入省份'
    return false
  }
  if (!formData.value.city?.trim()) {
    error.value = '请输入城市'
    return false
  }
  return true
}

const resetForm = () => {
  formData.value = {
    spuId: '',
    partCode: '',
    quantity: 1,
    manufacturerNode: '',
    province: '',
    city: ''
  }
  error.value = ''
}

const handleClose = () => {
  if (!submitting.value) {
    emit('update:modelValue', false)
    setTimeout(resetForm, 300)
  }
}

const handleSubmit = async () => {
  error.value = ''
  
  if (!validateForm()) {
    return
  }
  
  submitting.value = true
  try {
    const payload = {
      quantity: Number(formData.value.quantity),
      manufacturerNode: formData.value.manufacturerNode.trim(),
      province: formData.value.province.trim(),
      city: formData.value.city.trim()
    }

    // Prefer partCode; fall back to legacy spuId when partCode is not selected.
    if (formData.value.partCode && formData.value.partCode.trim()) {
      payload.partCode = formData.value.partCode.trim()
    } else if (formData.value.spuId) {
      payload.spuId = Number(formData.value.spuId)
    }

    const res = await createTrace(payload)
    
    const traceCodes = res.traceCodes || []
    emit('success', traceCodes)
    emit('update:modelValue', false)
    
    setTimeout(resetForm, 300)
  } catch (err) {
    error.value = err.message || '生成失败，请重试'
  } finally {
    submitting.value = false
  }
}

// 监听弹窗打开，重置表单
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    resetForm()
    // 弹窗打开时加载配件列表
    loadParts()
  }
}, { immediate: true })
</script>
