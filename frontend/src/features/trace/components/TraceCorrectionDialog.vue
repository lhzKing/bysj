<template>
  <teleport to="body">
    <Transition name="dialog-fade">
      <div v-if="modelValue" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-slate-900/40 backdrop-blur-sm" @click.self="handleClose"></div>
        <div class="relative premium-card max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-[40px] p-8">
          <div class="mb-8 flex items-center justify-between">
            <h3 class="flex items-center gap-3 text-2xl font-black tracking-tight text-slate-900">
              <FilePenLine class="h-6 w-6 text-amber-500" />
              提交审计纠错
            </h3>
            <button type="button" class="flex size-10 items-center justify-center rounded-full bg-slate-100 text-slate-500 hover:bg-slate-200" @click="handleClose">
              <X class="h-5 w-5" />
            </button>
          </div>

          <div class="space-y-5">
            <div class="rounded-2xl border border-amber-100 bg-amber-50 p-4 text-sm font-bold leading-7 text-slate-600">
              CORRECTION 只追加红冲蓝补审计记录，不删除原始日志；业务有效视图会隐藏被纠错覆盖的原始记录。
            </div>

            <label class="block">
              <span class="mb-2 block text-[10px] font-black uppercase tracking-widest text-slate-400">原始日志 ID <span class="text-rose-500">*</span></span>
              <input v-model="formData.correctionOf" type="number" min="1" class="w-full rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-3 font-mono text-sm font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-amber-400" placeholder="例如：18" />
            </label>

            <label class="block">
              <span class="mb-2 block text-[10px] font-black uppercase tracking-widest text-slate-400">纠错原因 <span class="text-rose-500">*</span></span>
              <textarea v-model="formData.remark" rows="4" maxlength="255" class="w-full rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-3 text-sm font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-amber-400" placeholder="请填写纠错原因和正确业务含义"></textarea>
            </label>

            <div class="grid gap-4 md:grid-cols-2">
              <label class="block">
                <span class="mb-2 block text-[10px] font-black uppercase tracking-widest text-slate-400">修正起点</span>
                <input v-model="formData.fromNode" class="w-full rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-3 text-sm font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-amber-400" />
              </label>
              <label class="block">
                <span class="mb-2 block text-[10px] font-black uppercase tracking-widest text-slate-400">修正终点</span>
                <input v-model="formData.toNode" class="w-full rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-3 text-sm font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-amber-400" />
              </label>
            </div>

            <label class="block">
              <span class="mb-2 block text-[10px] font-black uppercase tracking-widest text-slate-400">处理时间</span>
              <input v-model="formData.eventTime" type="datetime-local" class="w-full rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-3 font-mono text-sm font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-amber-400" />
            </label>
          </div>

          <div class="mt-8 flex justify-end gap-4">
            <button type="button" class="rounded-xl px-6 py-3 text-sm font-bold text-slate-500 hover:bg-slate-100" @click="handleClose">取消</button>
            <button type="button" :disabled="submitting" class="rounded-xl bg-amber-500 px-8 py-3 text-sm font-bold text-white shadow-lg shadow-amber-100 hover:bg-amber-600 disabled:opacity-70" @click="handleSubmit">
              {{ submitting ? '提交中...' : '提交纠错' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </teleport>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { FilePenLine, X } from 'lucide-vue-next'
import dayjs from 'dayjs'
import { createTraceCorrection } from '@/features/trace/api'
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
  correctionOf: '',
  remark: '',
  fromNode: '',
  toNode: '',
  eventTime: currentTime()
})

function resetForm() {
  formData.correctionOf = ''
  formData.remark = ''
  formData.fromNode = ''
  formData.toNode = ''
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
  const correctionOf = Number(formData.correctionOf)
  if (!Number.isInteger(correctionOf) || correctionOf <= 0) {
    toast.error('请输入有效的原始日志 ID')
    return
  }
  const remark = formData.remark?.trim() || ''
  if (!remark) {
    toast.error('请填写纠错原因')
    return
  }
  submitting.value = true
  try {
    await createTraceCorrection(props.traceCode, {
      correctionOf,
      remark,
      fromNode: formData.fromNode?.trim() || undefined,
      toNode: formData.toNode?.trim() || undefined,
      eventTime: formatToBackend(formData.eventTime)
    })
    toast.success('审计纠错已提交')
    emit('update:modelValue', false)
    emit('success')
  } catch (error) {
    logger.error('Create correction error:', error)
    toast.error(error?.message || '审计纠错提交失败')
  } finally {
    submitting.value = false
  }
}
</script>
