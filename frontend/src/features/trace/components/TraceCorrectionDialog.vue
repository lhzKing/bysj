<script setup>
import { reactive, ref, watch } from 'vue'
import { FilePenLine } from 'lucide-vue-next'
import dayjs from 'dayjs'
import BaseDialog from '@/shared/components/ui/BaseDialog.vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import { createTraceCorrection } from '@/features/trace/api'
import { useToast } from '@/shared/composables/useToast'
import { logger } from '@/shared/utils/logger'

/**
 * TraceCorrectionDialog —— 审计纠错对话框（Linear-light）。
 *
 * 视觉契约：BaseDialog 外壳；hero 用 var(--warn-soft) + warn 描边强调"红冲蓝补，不删除原始日志"语义。
 * 接口契约（api-doc.md 2.8）：POST /api/traces/{traceCode}/corrections，必填 correctionOf + remark。
 * 测试契约：保留 `formData` reactive + `handleSubmit` async（被 TraceCorrectionDialog.contract.test.js 用 setupState 断言）。
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

<template>
  <BaseDialog
    :model-value="modelValue"
    title="提交审计纠错"
    subtitle="CORRECTION 仅追加红冲蓝补审计记录，不删除原始日志；业务有效视图会隐藏被纠错覆盖的原始记录。"
    :icon="FilePenLine"
    size="md"
    :persistent="submitting"
    @update:model-value="handleClose"
  >
    <div class="trace-corr">
      <div class="trace-corr__hero" data-test="trace-corr-hero">
        <span class="trace-corr__eyebrow">追溯码</span>
        <span class="trace-corr__code mono">{{ traceCode }}</span>
      </div>

      <div class="trace-corr__field">
        <label class="trace-corr__label">原始日志 ID <span class="trace-corr__star">*</span></label>
        <input
          v-model="formData.correctionOf"
          type="number"
          min="1"
          class="trace-corr__input trace-corr__input--mono"
          placeholder="例如：18"
        />
      </div>

      <div class="trace-corr__field">
        <label class="trace-corr__label">纠错原因 <span class="trace-corr__star">*</span></label>
        <textarea
          v-model="formData.remark"
          rows="4"
          maxlength="255"
          class="trace-corr__textarea"
          placeholder="请填写纠错原因和正确业务含义"
        />
      </div>

      <div class="trace-corr__row">
        <div class="trace-corr__field">
          <label class="trace-corr__label">修正起点</label>
          <input v-model="formData.fromNode" class="trace-corr__input" />
        </div>
        <div class="trace-corr__field">
          <label class="trace-corr__label">修正终点</label>
          <input v-model="formData.toNode" class="trace-corr__input" />
        </div>
      </div>

      <div class="trace-corr__field">
        <label class="trace-corr__label">处理时间</label>
        <input
          v-model="formData.eventTime"
          type="datetime-local"
          class="trace-corr__input trace-corr__input--mono"
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
        {{ submitting ? '提交中…' : '提交纠错' }}
      </BaseButton>
    </template>
  </BaseDialog>
</template>

<style scoped>
.trace-corr {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.trace-corr__hero {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 10px;
  background: var(--warn-soft);
  border: 1px solid color-mix(in srgb, var(--warn) 18%, transparent);
}
.trace-corr__eyebrow {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: var(--warn);
}
.trace-corr__code {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: -0.2px;
}
.trace-corr__row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.trace-corr__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.trace-corr__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
}
.trace-corr__star {
  color: var(--error);
  margin-left: 2px;
}
.trace-corr__input {
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
.trace-corr__input--mono {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
  font-size: 13px;
}
.trace-corr__input:focus {
  border-color: var(--primary-focus);
  box-shadow: 0 0 0 3px var(--primary-ring);
}
.trace-corr__textarea {
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
.trace-corr__textarea:focus {
  border-color: var(--primary-focus);
  box-shadow: 0 0 0 3px var(--primary-ring);
}
@media (max-width: 639.98px) {
  .trace-corr__row {
    grid-template-columns: 1fr;
  }
}
</style>
