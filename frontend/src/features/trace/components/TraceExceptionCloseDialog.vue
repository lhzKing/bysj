<template>
  <teleport to="body">
    <Transition name="dialog-fade">
      <div v-if="modelValue" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-slate-900/40 backdrop-blur-sm" @click.self="handleClose"></div>
        <div class="relative premium-card w-full max-w-lg rounded-[40px] p-8">
          <div class="mb-8 flex items-center justify-between">
            <h3 class="flex items-center gap-3 text-2xl font-black tracking-tight text-slate-900">
              <ShieldCheck class="h-6 w-6 text-emerald-500" />
              解除异常冻结
            </h3>
            <button type="button" class="flex size-10 items-center justify-center rounded-full bg-slate-100 text-slate-500 hover:bg-slate-200" @click="handleClose">
              <X class="h-5 w-5" />
            </button>
          </div>

          <div class="space-y-5">
            <div class="rounded-2xl border border-emerald-100 bg-emerald-50 p-4 text-sm font-bold leading-7 text-slate-600">
              将为 <span class="font-mono text-emerald-600">{{ traceCode }}</span> 写入 EXCEPTION_CLOSE 事件，系统会恢复冻结前状态并清除冻结恢复字段。
            </div>

            <label class="block">
              <span class="mb-2 block text-[10px] font-black uppercase tracking-widest text-slate-400">解除原因 <span class="text-rose-500">*</span></span>
              <textarea v-model="formData.remark" rows="4" maxlength="255" class="w-full rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-3 text-sm font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-emerald-400" placeholder="请填写复核结论，例如：外包装已复核无误，解除冻结"></textarea>
            </label>

            <label class="block">
              <span class="mb-2 block text-[10px] font-black uppercase tracking-widest text-slate-400">处理时间</span>
              <input v-model="formData.eventTime" type="datetime-local" class="w-full rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-3 font-mono text-sm font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-emerald-400" />
            </label>
          </div>

          <div class="mt-8 flex justify-end gap-4">
            <button type="button" class="rounded-xl px-6 py-3 text-sm font-bold text-slate-500 hover:bg-slate-100" @click="handleClose">取消</button>
            <button type="button" :disabled="submitting" class="rounded-xl bg-emerald-500 px-8 py-3 text-sm font-bold text-white shadow-lg shadow-emerald-100 hover:bg-emerald-600 disabled:opacity-70" @click="handleSubmit">
              {{ submitting ? '提交中...' : '确认解除' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </teleport>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ShieldCheck, X } from 'lucide-vue-next'
import dayjs from 'dayjs'
import { closeTraceException } from '@/features/trace/api'
import { useToast } from '@/shared/composables/useToast'
import { logger } from '@/shared/utils/logger'

const props = defineProps({
  modelValue: { type: Boolean, required: true },
  traceCode: { type: String, required: true }
})

const emit = defineEmits(['update:modelValue', 'success'])
const toast = useToast()
const submitting = ref(false)

const currentTime = () => dayjs().format('YYYY-MM-DDTHH:mm')
const formatToBackend = (value) => value ? dayjs(value).format('YYYY-MM-DDTHH:mm:ss') : dayjs().format('YYYY-MM-DDTHH:mm:ss')

const formData = reactive({
  remark: '',
  eventTime: currentTime()
})

function resetForm() {
  formData.remark = ''
  formData.eventTime = currentTime()
}

watch(() => props.modelValue, (open) => {
  if (open) resetForm()
})

function handleClose() {
  if (submitting.value) return
  emit('update:modelValue', false)
}

async function handleSubmit() {
  const remark = formData.remark?.trim() || ''
  if (!remark) {
    toast.error('请填写解除原因')
    return
  }
  submitting.value = true
  try {
    await closeTraceException(props.traceCode, {
      remark,
      eventTime: formatToBackend(formData.eventTime)
    })
    toast.success('异常冻结已解除')
    emit('update:modelValue', false)
    emit('success')
  } catch (error) {
    logger.error('Close exception error:', error)
    toast.error(error?.message || '解除异常冻结失败')
  } finally {
    submitting.value = false
  }
}
</script>
