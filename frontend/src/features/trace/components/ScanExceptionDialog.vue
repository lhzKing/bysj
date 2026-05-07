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
          <!-- Header：rose 色系，与正常流转 indigo 区分 -->
          <div class="flex items-center justify-between mb-8">
            <h3 class="text-2xl font-black text-slate-900 tracking-tight flex items-center gap-3">
              <AlertTriangle class="w-6 h-6 text-rose-500" />
              异常状态上报
            </h3>
            <button
              type="button"
              @click="handleClose"
              class="size-10 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-500 flex items-center justify-center transition-colors"
            >
              <X class="w-5 h-5" />
            </button>
          </div>

          <!-- Body -->
          <div class="space-y-6">
            <div class="bg-rose-50 border border-rose-100 rounded-2xl p-4 flex items-start gap-4 shadow-inner">
              <Info class="w-6 h-6 text-rose-500 flex-shrink-0 mt-0.5" />
              <div class="text-sm">
                <p class="font-black text-slate-900 mb-1 tracking-tight">Exception Report</p>
                <p class="text-slate-600 font-medium leading-relaxed">
                  为溯源码 "<span class="font-mono text-rose-600 font-bold">{{ traceCode }}</span>" 上报异常状态。请填写当前位置与异常描述，系统将记录为 EXCEPTION_OPEN 类型事件并冻结常规流转。
                </p>
              </div>
            </div>

            <!-- 省份 -->
            <div>
              <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1">
                省份 <span class="text-rose-500">*</span>
              </label>
              <select
                v-model="formData.province"
                @change="handleProvinceChange"
                class="w-full px-4 py-3 bg-slate-50/50 rounded-2xl border border-slate-200 text-sm font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-rose-400 transition-shadow appearance-none"
              >
                <option value="" disabled class="bg-white text-slate-900">请选择省份</option>
                <option
                  v-for="region in regions"
                  :key="region.value"
                  :value="region.value"
                  class="bg-white text-slate-900"
                >
                  {{ region.label }}
                </option>
              </select>
            </div>

            <!-- 城市 -->
            <div>
              <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1">
                城市 <span class="text-rose-500">*</span>
              </label>
              <select
                v-model="formData.city"
                :disabled="!formData.province"
                class="w-full px-4 py-3 bg-slate-50/50 rounded-2xl border border-slate-200 text-sm font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-rose-400 transition-shadow appearance-none disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <option value="" disabled class="bg-white text-slate-900">请选择城市</option>
                <option
                  v-for="city in availableCities"
                  :key="city"
                  :value="city"
                  class="bg-white text-slate-900"
                >
                  {{ city }}
                </option>
              </select>
            </div>

            <!-- 异常描述（必填） -->
            <div>
              <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1">
                异常描述 <span class="text-rose-500">*</span>
              </label>
              <textarea
                v-model="formData.remark"
                rows="3"
                maxlength="255"
                placeholder="请描述异常情况，如&quot;外包装破损&quot;、&quot;扫码无法识别&quot;、&quot;温度超标&quot;等"
                class="w-full px-4 py-3 bg-slate-50/50 rounded-2xl border border-slate-200 text-sm font-medium text-slate-700 focus:outline-none focus:ring-2 focus:ring-rose-400 transition-shadow resize-none"
              ></textarea>
            </div>

            <!-- 上报时间 -->
            <div>
              <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 ml-1">
                上报时间 <span class="text-rose-500">*</span>
              </label>
              <input
                v-model="formData.eventTime"
                type="datetime-local"
                class="w-full px-4 py-3 bg-slate-50/50 rounded-2xl border border-slate-200 text-sm font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-rose-400 transition-shadow font-mono"
              />
            </div>
          </div>

          <!-- Footer -->
          <div class="mt-10 flex justify-end gap-4">
            <BaseButton
              variant="secondary"
              @click="handleClose"
              class="px-6 py-3 rounded-xl font-bold text-slate-500 hover:bg-slate-100 transition-colors"
            >
              取消
            </BaseButton>
            <BaseButton
              variant="primary"
              :disabled="submitting"
              @click="handleSubmit"
              class="px-8 py-3 rounded-xl font-bold bg-rose-500 text-white shadow-lg shadow-rose-200 hover:bg-rose-600 hover:shadow-xl hover:shadow-rose-300 transition-all flex items-center disabled:opacity-70 disabled:cursor-not-allowed"
            >
              <Loader v-if="submitting" class="w-4 h-4 animate-spin mr-2" />
              <span>{{ submitting ? '提交中...' : '提交异常' }}</span>
            </BaseButton>
          </div>
        </div>
      </div>
    </Transition>
  </teleport>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { AlertTriangle, X, Info, Loader } from 'lucide-vue-next'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import { REGIONS } from '@/shared/data/regions'
import { useToast } from '@/shared/composables/useToast'
import { createEvent } from '@/features/trace/api'
import { logger } from '@/shared/utils/logger'
import dayjs from 'dayjs'

const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true
  },
  traceCode: {
    type: String,
    required: true
  }
})

/**
 * 组件向外暴露的事件契约：
 * - `update:modelValue`：v-model 同步弹窗显隐（参数：boolean）
 * - `success`：异常上报成功后通知父组件刷新（无参）
 *
 * 与 ScanFlowDialog 的差异：
 * - 不含 fromNode/toNode（异常上报无"流转方向"语义）；
 * - 不含 correctionOf（CORRECTION 在独立入口处理，见 T-P1-08 follow-up）；
 * - actionType 固定为 'EXCEPTION_OPEN'，由本组件内部封装，不通过 prop 暴露。
 */
const emit = defineEmits(['update:modelValue', 'success'])

const toast = useToast()
const regions = REGIONS

const getCurrentDateTime = () => dayjs().format('YYYY-MM-DDTHH:mm')
const formatToBackend = (datetimeLocal) => {
  if (!datetimeLocal) return dayjs().format('YYYY-MM-DDTHH:mm:ss')
  return dayjs(datetimeLocal).format('YYYY-MM-DDTHH:mm:ss')
}

const submitting = ref(false)
const formData = reactive({
  province: '',
  city: '',
  remark: '',
  eventTime: getCurrentDateTime()
})

const availableCities = computed(() => {
  const region = regions.find((r) => r.value === formData.province)
  return region ? region.cities : []
})

const handleProvinceChange = () => {
  formData.city = ''
}

const resetForm = () => {
  formData.province = ''
  formData.city = ''
  formData.remark = ''
  formData.eventTime = getCurrentDateTime()
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      resetForm()
    }
  }
)

const handleClose = () => {
  if (submitting.value) return
  emit('update:modelValue', false)
}

const handleSubmit = async () => {
  if (!formData.province) {
    toast.error('请选择省份')
    return
  }
  if (!formData.city) {
    toast.error('请选择城市')
    return
  }
  const trimmedRemark = formData.remark?.trim() || ''
  if (!trimmedRemark) {
    toast.error('请填写异常描述')
    return
  }
  if (!formData.eventTime) {
    toast.error('请选择上报时间')
    return
  }

  submitting.value = true
  try {
    const payload = {
      actionType: 'EXCEPTION_OPEN',
      province: formData.province,
      city: formData.city,
      remark: trimmedRemark,
      eventTime: formatToBackend(formData.eventTime)
    }
    await createEvent(props.traceCode, payload)
    toast.success('异常上报已提交')
    emit('update:modelValue', false)
    emit('success')
  } catch (error) {
    logger.error('Submit exception error:', error)
    toast.error(error?.message || '异常上报提交失败')
  } finally {
    submitting.value = false
  }
}
</script>
