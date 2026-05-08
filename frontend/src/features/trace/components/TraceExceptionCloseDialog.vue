<script setup>
import { reactive, ref, watch } from 'vue'
import { ShieldCheck } from 'lucide-vue-next'
import dayjs from 'dayjs'
import BaseDialog from '@/shared/components/ui/BaseDialog.vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import { closeTraceException } from '@/features/trace/api'
import { useToast } from '@/shared/composables/useToast'
import { logger } from '@/shared/utils/logger'

/**
 * TraceExceptionCloseDialog —— 解除异常冻结对话框（Linear-light）。
 *
 * 视觉契约：BaseDialog 外壳；hero 用 var(--success-soft) + success 描边强调"恢复冻结前状态"语义。
 * 接口契约（api-doc.md 2.8）：POST /api/traces/{traceCode}/exception/close，必填 remark。
 * 测试契约：保留 `formData` reactive + `handleSubmit` async（被 TraceExceptionCloseDialog.contract.test.js 用 setupState 断言）。
 */
const props = defineProps({
  modelValue: { type: Boolean, required: true },
  traceCode: { type: String, required: true }
})

const emit = defineEmits(['update:modelValue', 'success'])
const toast = useToast()
const submitting = ref(false)

const currentTime = () => dayjs().format('YYYY-MM-DDTHH:mm')
const formatToBackend = (value) =>
  value ? dayjs(value).format('YYYY-MM-DDTHH:mm:ss') : dayjs().format('YYYY-MM-DDTHH:mm:ss')

const formData = reactive({
  remark: '',
  eventTime: currentTime()
})

function resetForm() {
  formData.remark = ''
  formData.eventTime = currentTime()
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) resetForm()
  }
)

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

<template>
  <BaseDialog
    :model-value="modelValue"
    title="解除异常冻结"
    subtitle="将写入 EXCEPTION_CLOSE 事件，系统会恢复冻结前状态并清除冻结恢复字段。"
    :icon="ShieldCheck"
    size="sm"
    :persistent="submitting"
    @update:model-value="handleClose"
  >
    <div class="trace-close">
      <div class="trace-close__hero" data-test="trace-close-hero">
        <span class="trace-close__eyebrow">追溯码</span>
        <span class="trace-close__code mono">{{ traceCode }}</span>
      </div>

      <div class="trace-close__field">
        <label class="trace-close__label">解除原因 <span class="trace-close__star">*</span></label>
        <textarea
          v-model="formData.remark"
          rows="4"
          maxlength="255"
          class="trace-close__textarea"
          placeholder="请填写复核结论，例如：外包装已复核无误，解除冻结"
        />
      </div>

      <div class="trace-close__field">
        <label class="trace-close__label">处理时间</label>
        <input
          v-model="formData.eventTime"
          type="datetime-local"
          class="trace-close__input trace-close__input--mono"
        />
      </div>
    </div>

    <template #footer>
      <BaseButton variant="secondary" size="md" :disabled="submitting" @click="handleClose">
        取消
      </BaseButton>
      <BaseButton
        variant="primary"
        size="md"
        :loading="submitting"
        :disabled="submitting"
        @click="handleSubmit"
      >
        {{ submitting ? '提交中…' : '确认解除' }}
      </BaseButton>
    </template>
  </BaseDialog>
</template>

<style scoped>
.trace-close {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.trace-close__hero {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 10px;
  background: var(--success-soft);
  border: 1px solid color-mix(in srgb, var(--success) 18%, transparent);
}
.trace-close__eyebrow {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: var(--success);
}
.trace-close__code {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: -0.2px;
}
.trace-close__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.trace-close__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
}
.trace-close__star {
  color: var(--error);
  margin-left: 2px;
}
.trace-close__input {
  height: 36px;
  padding: 0 12px;
  border: 1px solid var(--hairline);
  border-radius: 8px;
  background: var(--surface-1);
  color: var(--ink);
  font-size: 14px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
  width: 100%;
}
.trace-close__input--mono {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
  font-size: 13px;
}
.trace-close__input:focus {
  border-color: var(--primary-focus);
  box-shadow: 0 0 0 3px var(--primary-ring);
}
.trace-close__textarea {
  min-height: 96px;
  padding: 10px 12px;
  border: 1px solid var(--hairline);
  border-radius: 8px;
  background: var(--surface-1);
  color: var(--ink);
  font-size: 14px;
  font-family: inherit;
  outline: none;
  resize: vertical;
  transition: border-color 0.15s, box-shadow 0.15s;
  width: 100%;
  line-height: 1.55;
}
.trace-close__textarea:focus {
  border-color: var(--primary-focus);
  box-shadow: 0 0 0 3px var(--primary-ring);
}
</style>
