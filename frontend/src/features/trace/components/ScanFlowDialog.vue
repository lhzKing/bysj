<template>
  <teleport to="body">
    <Transition name="dialog-fade">
      <div 
        v-if="modelValue" 
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
      >
        <div 
          class="absolute inset-0 bg-slate-900/40 backdrop-blur-sm"
          @click.self="handleClose"
        ></div>
        <div class="relative premium-card rounded-[40px] w-full max-w-lg mx-4 transform transition-all p-8 max-h-[90vh] overflow-y-auto">
          <!-- Header -->
          <div class="flex items-center justify-between mb-8">
            <h3 class="text-2xl font-black text-slate-900 tracking-tight flex items-center gap-3">
              <QrCode class="w-6 h-6 text-indigo-600" />
              {{ dialogTitle }}
            </h3>
            <button 
              @click="handleClose"
              class="size-10 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-500 flex items-center justify-center transition-colors"
            >
              <X class="w-5 h-5" />
            </button>
          </div>

          <!-- Body -->
          <div class="space-y-6">
            <div class="bg-indigo-50 border border-indigo-100 rounded-2xl p-4 flex items-start gap-4 shadow-inner">
              <Info class="w-6 h-6 text-indigo-600 flex-shrink-0 mt-0.5" />
              <div class="text-sm">
                <p class="font-black text-slate-900 mb-1 tracking-tight">{{ operationTitle }} Operation</p>
                <p class="text-slate-600 font-medium leading-relaxed">为溯源码 "<span class="font-mono text-indigo-600 font-bold">{{ traceCode }}</span>" 记录{{ operationDescription }}事件，系统将自动记录时间戳和区块链哈希值。</p>
              </div>
            </div>

            <!-- 动态表单：根据操作类型渲染统一组件 -->
            <BaseFlowForm
              :action-type="actionType"
              :model-value="formData"
              @update:modelValue="Object.assign(formData, $event)"
              class="space-y-4"
            />
          </div>

          <!-- Footer -->
          <div class="mt-10 flex justify-end gap-4">
            <BaseButton variant="secondary" @click="handleClose" class="px-6 py-3 rounded-xl font-bold text-slate-500 hover:bg-slate-100 transition-colors">
              取消
            </BaseButton>
            <BaseButton variant="primary" @click="handleSubmit" :disabled="submitting" class="px-8 py-3 rounded-xl font-bold bg-indigo-600 text-white shadow-lg shadow-indigo-200 hover:bg-indigo-700 hover:shadow-xl hover:shadow-indigo-300 transition-all flex items-center disabled:opacity-70 disabled:cursor-not-allowed">
              <Loader v-if="submitting" class="w-4 h-4 animate-spin mr-2" />
              <span v-else>提交{{ operationTitle }}</span>
            </BaseButton>
          </div>
        </div>
      </div>
    </Transition>
  </teleport>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue'
import { QrCode, X, Info, Loader } from 'lucide-vue-next'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import BaseFlowForm from './BaseFlowForm.vue'
import { useToast } from '@/shared/composables/useToast'
import { createEvent } from '@/features/trace/api'
import dayjs from 'dayjs'

const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true
  },
  traceCode: {
    type: String,
    required: true
  },
  actionType: {
    type: String,
    required: true,
    validator: (value) => ['inbound', 'outbound', 'transfer'].includes(value)
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const toast = useToast()
const submitting = ref(false)

// 对话框标题映射
const dialogTitleMap = {
  'inbound': '入库登记',
  'outbound': '出库登记',
  'transfer': '物流流转'
}

// 操作标题映射
const operationTitleMap = {
  'inbound': '入库',
  'outbound': '出库',
  'transfer': '流转'
}

// 操作描述映射
const operationDescriptionMap = {
  'inbound': '入库',
  'outbound': '出库',
  'transfer': '物流流转'
}

// Map frontend actionType values to backend API enum values.
const actionTypeApiMap = {
  'inbound': 'INBOUND',
  'outbound': 'OUTBOUND',
  'transfer': 'TRANSFER'
}

const dialogTitle = computed(() => dialogTitleMap[props.actionType] || '扫码流转')
const operationTitle = computed(() => operationTitleMap[props.actionType] || '流转')
const operationDescription = computed(() => operationDescriptionMap[props.actionType] || '流转')

// 获取当前时间，格式化为 datetime-local 输入框格式
const getCurrentDateTime = () => {
  return dayjs().format('YYYY-MM-DDTHH:mm')
}

// 将 datetime-local 格式转换为后端接受的 ISO-8601 本地时间格式（YYYY-MM-DDTHH:mm:ss）
const formatToBackend = (datetimeLocal) => {
  if (!datetimeLocal) return dayjs().format('YYYY-MM-DDTHH:mm:ss')
  return dayjs(datetimeLocal).format('YYYY-MM-DDTHH:mm:ss')
}

const formData = reactive({
  fromNode: '',
  toNode: '',
  province: '',
  city: '',
  eventTime: getCurrentDateTime(),
  correctionOf: null,
  actionType: actionTypeApiMap[props.actionType],
  remark: ''
})

// 重置表单
const resetForm = () => {
  formData.fromNode = ''
  formData.toNode = ''
  formData.province = ''
  formData.city = ''
  formData.eventTime = getCurrentDateTime()
  formData.correctionOf = null
  formData.actionType = actionTypeApiMap[props.actionType]
  formData.remark = ''
}

// 当弹窗打开时重置表单
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    resetForm()
  }
})

const handleClose = () => {
  emit('update:modelValue', false)
}

const handleSubmit = async () => {
  // 表单验证
  if (!formData.fromNode) {
    toast.error('请输入起始节点')
    return
  }
  if (!formData.toNode) {
    toast.error('请输入目标节点')
    return
  }
  if (!formData.province) {
    toast.error('请选择省份')
    return
  }
  if (!formData.city) {
    toast.error('请选择城市')
    return
  }
  if (!formData.eventTime) {
    toast.error('请选择时间')
    return
  }

  submitting.value = true
  try {
    // 构建API请求数据
    const apiData = {
      actionType: formData.actionType,
      fromNode: formData.fromNode,
      toNode: formData.toNode,
      province: formData.province,
      city: formData.city,
      eventTime: formatToBackend(formData.eventTime),
      correctionOf: formData.correctionOf,
      remark: formData.remark?.trim() || ''
    }
    await createEvent(props.traceCode, apiData)
    toast.success(`${operationTitle.value}记录提交成功`)
    emit('update:modelValue', false)
    emit('success')
  } catch (error) {
    console.error('Submit flow error:', error)
    toast.error(error.message || `${operationTitle.value}记录提交失败`)
  } finally {
    submitting.value = false
  }
}
</script>
