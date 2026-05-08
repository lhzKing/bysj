<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ShieldAlert } from 'lucide-vue-next'
import dayjs from 'dayjs'
import BaseDialog from '@/shared/components/ui/BaseDialog.vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import { REGIONS } from '@/shared/data/regions'
import { useToast } from '@/shared/composables/useToast'
import { createEvent } from '@/features/trace/api'
import { logger } from '@/shared/utils/logger'

/**
 * ScanExceptionDialog —— 异常上报 / 冻结对话框（Linear-light）。
 *
 * 视觉契约：
 *   - 外壳走 BaseDialog；hero 卡用 var(--error-soft) + error 描边强调"异常冻结后常规流转将被锁定"
 *   - 主 CTA 用 BaseButton variant=danger
 *
 * 接口契约（api-doc.md 2.5 / 2.8）：
 *   - POST /api/traces/{traceCode}/events，actionType=EXCEPTION_OPEN
 *   - 不含 fromNode / toNode / correctionOf；province/city/remark/eventTime 必填
 *   - idempotencyKey 由 ScanHub 透传
 *
 * 测试契约：保留 `formData` reactive + `handleSubmit` async；payload 不得包含 fromNode/toNode/correctionOf。
 */
const props = defineProps({
  modelValue: { type: Boolean, required: true },
  traceCode: { type: String, required: true },
  idempotencyKey: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'success'])

const toast = useToast()
const regions = REGIONS

const submitting = ref(false)

const currentDateTime = () => dayjs().format('YYYY-MM-DDTHH:mm')
const formatToBackend = (datetimeLocal) =>
  datetimeLocal ? dayjs(datetimeLocal).format('YYYY-MM-DDTHH:mm:ss') : dayjs().format('YYYY-MM-DDTHH:mm:ss')

const formData = reactive({
  province: '',
  city: '',
  remark: '',
  eventTime: currentDateTime()
})

const availableCities = computed(() => {
  const region = regions.find((r) => r.value === formData.province)
  return region ? region.cities : []
})

function handleProvinceChange() {
  formData.city = ''
}

function resetForm() {
  formData.province = ''
  formData.city = ''
  formData.remark = ''
  formData.eventTime = currentDateTime()
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
    if (props.idempotencyKey) {
      payload.idempotencyKey = props.idempotencyKey
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

<template>
  <BaseDialog
    :model-value="modelValue"
    title="异常状态上报"
    :icon="ShieldAlert"
    size="md"
    :persistent="submitting"
    @update:model-value="handleClose"
  >
    <div class="scan-exc">
      <div class="scan-exc__hero" data-test="scan-exc-hero">
        <span class="scan-exc__eyebrow">追溯码</span>
        <span class="scan-exc__code mono">{{ traceCode }}</span>
        <p class="scan-exc__hint">
          为该追溯码写入 EXCEPTION_OPEN 事件。提交后该单品的常规流转会被冻结，需走解除流程恢复。
        </p>
      </div>

      <div class="scan-exc__row">
        <div class="scan-exc__field">
          <label class="scan-exc__label">省份 <span class="scan-exc__star">*</span></label>
          <select
            v-model="formData.province"
            class="scan-exc__select"
            @change="handleProvinceChange"
          >
            <option value="" disabled>请选择省份</option>
            <option v-for="region in regions" :key="region.value" :value="region.value">
              {{ region.label }}
            </option>
          </select>
        </div>

        <div class="scan-exc__field">
          <label class="scan-exc__label">城市 <span class="scan-exc__star">*</span></label>
          <select
            v-model="formData.city"
            :disabled="!formData.province"
            class="scan-exc__select"
          >
            <option value="" disabled>请选择城市</option>
            <option v-for="city in availableCities" :key="city" :value="city">{{ city }}</option>
          </select>
        </div>
      </div>

      <div class="scan-exc__field">
        <label class="scan-exc__label">异常描述 <span class="scan-exc__star">*</span></label>
        <textarea
          v-model="formData.remark"
          rows="3"
          maxlength="255"
          class="scan-exc__textarea"
          placeholder='请描述异常情况，如"外包装破损"、"扫码无法识别"、"温度超标"等'
        />
      </div>

      <div class="scan-exc__field">
        <label class="scan-exc__label">上报时间 <span class="scan-exc__star">*</span></label>
        <input
          v-model="formData.eventTime"
          type="datetime-local"
          class="scan-exc__input scan-exc__input--mono"
        />
      </div>
    </div>

    <template #footer>
      <BaseButton variant="secondary" size="md" :disabled="submitting" @click="handleClose">
        取消
      </BaseButton>
      <BaseButton
        variant="danger"
        size="md"
        :loading="submitting"
        :disabled="submitting"
        @click="handleSubmit"
      >
        {{ submitting ? '提交中…' : '提交异常' }}
      </BaseButton>
    </template>
  </BaseDialog>
</template>

<style scoped>
.scan-exc {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.scan-exc__hero {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  padding: 14px 16px;
  border-radius: 10px;
  background: var(--error-soft);
  border: 1px solid color-mix(in srgb, var(--error) 18%, transparent);
}
.scan-exc__eyebrow {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: var(--error);
}
.scan-exc__code {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: -0.2px;
}
.scan-exc__hint {
  margin: 4px 0 0;
  flex: 1 1 100%;
  font-size: 12.5px;
  color: var(--ink-subtle);
  line-height: 1.55;
}

.scan-exc__row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.scan-exc__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.scan-exc__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
  line-height: 1.3;
}
.scan-exc__star {
  color: var(--error);
  margin-left: 2px;
}

.scan-exc__select,
.scan-exc__input {
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
  appearance: none;
  -webkit-appearance: none;
  width: 100%;
}
.scan-exc__select {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2371717a' stroke-width='2'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 10px center;
  background-size: 14px 14px;
  padding-right: 32px;
}
.scan-exc__input--mono {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
  font-size: 13px;
}
.scan-exc__select:focus,
.scan-exc__input:focus {
  border-color: var(--primary-focus);
  box-shadow: 0 0 0 3px var(--primary-ring);
}
.scan-exc__select:disabled {
  background-color: var(--surface-2);
  color: var(--ink-tertiary);
  cursor: not-allowed;
}

.scan-exc__textarea {
  min-height: 84px;
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
.scan-exc__textarea:focus {
  border-color: var(--primary-focus);
  box-shadow: 0 0 0 3px var(--primary-ring);
}

@media (max-width: 639.98px) {
  .scan-exc__row {
    grid-template-columns: 1fr;
  }
}
</style>
