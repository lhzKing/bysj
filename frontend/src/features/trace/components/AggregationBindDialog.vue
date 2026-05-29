<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { Package, ScanLine } from 'lucide-vue-next'
import BaseDialog from '@/shared/components/ui/BaseDialog.vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import QRScanner from '@/shared/components/QRScanner.vue'
import { useToast } from '@/shared/composables/useToast'
import { logger } from '@/shared/utils/logger'
import { bindAggregation } from '@/features/trace/api'

/**
 * AggregationBindDialog —— 装箱 / 装托盘绑定对话框（Linear-light）。
 *
 * 视觉契约：
 *   - 复用 BaseDialog（rgba(15,23,42,0.45) 蒙层 + 12px 圆角 + 1px hairline）
 *   - 顶部 hero 卡：var(--primary-soft) + 1px primary/15% 描边 + caption 引导文案
 *   - relationType radio chips（CARTON / PALLET 单选，二选一）
 *   - parentCode / childCode 两个 mono 输入，childCode 旁有「摄像头扫码」按钮
 *   - remark textarea 可选
 *
 * 接口契约：
 *   - POST /api/trace-aggregations，权限 trace:scan / trace:task:scan / trace:outbound / trace:transfer / trace:create
 *   - childCode 扫码复用 QRScanner（同 ScanHub 的 inline 取景器 + 同源 wasm）
 *   - 成功后 emit('success', response) + 父组件刷新列表 + 关闭 dialog
 *
 * Props：
 *   - modelValue: 显隐双向绑定
 *   - defaultRelationType: 'CARTON' | 'PALLET'（外层「新建装箱」/「新建装托」按钮决定）
 */
const props = defineProps({
  modelValue: { type: Boolean, required: true },
  defaultRelationType: {
    type: String,
    default: 'CARTON',
    validator: (v) => ['CARTON', 'PALLET'].includes(v)
  }
})

const emit = defineEmits(['update:modelValue', 'success'])
const toast = useToast()

const submitting = ref(false)
const scannerOpen = ref(false)

const formData = reactive({
  relationType: props.defaultRelationType,
  parentCode: '',
  childCode: '',
  remark: ''
})

const titleMap = {
  CARTON: '新建装箱聚合',
  PALLET: '新建装托盘聚合'
}
const parentPrefixHint = {
  CARTON: '推荐以 CARTON- 开头，如 CARTON-EXT-001',
  PALLET: '推荐以 PALLET- 开头，如 PALLET-EXT-001'
}
const childHint = {
  CARTON: '子码须为已激活的单品溯源码（IN_STOCK / IN_TRANSIT 状态）',
  PALLET: '子码可以是箱码（CARTON-…）或单品码；托盘不能嵌套托盘'
}

const dialogTitle = computed(() => titleMap[formData.relationType] || '新建聚合关系')
const childHintText = computed(() => childHint[formData.relationType] || '')
const parentHintText = computed(() => parentPrefixHint[formData.relationType] || '')

function resetForm(nextRelationType) {
  formData.relationType = nextRelationType || props.defaultRelationType
  formData.parentCode = ''
  formData.childCode = ''
  formData.remark = ''
  scannerOpen.value = false
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      resetForm()
    } else {
      scannerOpen.value = false
    }
  }
)

watch(
  () => props.defaultRelationType,
  (next) => {
    if (props.modelValue) {
      formData.relationType = next
    }
  }
)

function handleClose() {
  if (submitting.value) return
  emit('update:modelValue', false)
}

function handleScannerOpen() {
  scannerOpen.value = true
}

function handleScannerClose() {
  scannerOpen.value = false
}

function handleScanResult(code) {
  scannerOpen.value = false
  if (typeof code === 'string' && code.trim()) {
    formData.childCode = code.trim()
  }
}

function validate() {
  if (!formData.relationType) return '请选择聚合类型'
  const parent = formData.parentCode.trim()
  if (!parent) return '请输入父码（箱码 / 托盘码）'
  const child = formData.childCode.trim()
  if (!child) return '请输入或扫描子码'
  if (parent === child) return '父码和子码不能相同'
  if (parent.length > 64 || child.length > 64) return '父码 / 子码长度不能超过 64'
  return ''
}

async function handleSubmit() {
  const validationMessage = validate()
  if (validationMessage) {
    toast.error(validationMessage)
    return
  }
  submitting.value = true
  try {
    const payload = {
      relationType: formData.relationType,
      parentCode: formData.parentCode.trim(),
      childCode: formData.childCode.trim()
    }
    const remark = formData.remark.trim()
    if (remark) payload.remark = remark
    const response = await bindAggregation(payload)
    toast.success(
      formData.relationType === 'PALLET'
        ? '托盘绑定成功'
        : '装箱聚合成功'
    )
    emit('update:modelValue', false)
    emit('success', response)
  } catch (error) {
    logger.error('聚合绑定失败', error)
    // request.js 已经 toast 过错误，这里不重复
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <BaseDialog
    :model-value="modelValue"
    :title="dialogTitle"
    :icon="Package"
    size="md"
    :persistent="submitting"
    @update:model-value="handleClose"
  >
    <div class="agg-dialog">
      <div class="agg-dialog__hero" data-test="agg-dialog-hero">
        <span class="agg-dialog__eyebrow">聚合关系</span>
        <p class="agg-dialog__hint">
          绑定后自动写入 PACK / PALLETIZE 上链事件；同一对父子只能存在一条 active 关系。
          状态为 IN_TRANSIT / TRANSFERRED / EXCEPTION 的子码不可参与聚合。
        </p>
      </div>

      <div class="agg-dialog__field">
        <span class="agg-dialog__label">聚合类型 <span class="agg-dialog__required">*</span></span>
        <div class="agg-dialog__chips" role="radiogroup" aria-label="聚合类型">
          <label
            class="agg-dialog__chip"
            :class="{ 'agg-dialog__chip--active': formData.relationType === 'CARTON' }"
            data-test="agg-dialog-relation-carton"
          >
            <input
              v-model="formData.relationType"
              type="radio"
              value="CARTON"
              :disabled="submitting"
              name="relation-type"
            />
            <span>箱码（CARTON）</span>
          </label>
          <label
            class="agg-dialog__chip"
            :class="{ 'agg-dialog__chip--active': formData.relationType === 'PALLET' }"
            data-test="agg-dialog-relation-pallet"
          >
            <input
              v-model="formData.relationType"
              type="radio"
              value="PALLET"
              :disabled="submitting"
              name="relation-type"
            />
            <span>托盘码（PALLET）</span>
          </label>
        </div>
      </div>

      <div class="agg-dialog__field">
        <label class="agg-dialog__label" for="agg-dialog-parent-code">
          父码（箱码 / 托盘码） <span class="agg-dialog__required">*</span>
        </label>
        <input
          id="agg-dialog-parent-code"
          v-model="formData.parentCode"
          type="text"
          maxlength="64"
          :disabled="submitting"
          :placeholder="parentHintText"
          class="agg-dialog__control agg-dialog__control--mono"
          data-test="agg-dialog-parent-input"
        />
        <p class="agg-dialog__field-hint">{{ parentHintText }}</p>
      </div>

      <div class="agg-dialog__field">
        <label class="agg-dialog__label" for="agg-dialog-child-code">
          子码 <span class="agg-dialog__required">*</span>
        </label>
        <div class="agg-dialog__scan-row">
          <input
            id="agg-dialog-child-code"
            v-model="formData.childCode"
            type="text"
            maxlength="64"
            :disabled="submitting"
            placeholder="单品码 / 箱码"
            class="agg-dialog__control agg-dialog__control--mono"
            data-test="agg-dialog-child-input"
          />
          <BaseButton
            variant="secondary"
            size="sm"
            :disabled="submitting"
            data-test="agg-dialog-open-scanner"
            @click="handleScannerOpen"
          >
            <template #icon><ScanLine :size="13" /></template>
            摄像头扫码
          </BaseButton>
        </div>
        <p class="agg-dialog__field-hint">{{ childHintText }}</p>
      </div>

      <div class="agg-dialog__field">
        <label class="agg-dialog__label" for="agg-dialog-remark">备注（可选）</label>
        <textarea
          id="agg-dialog-remark"
          v-model="formData.remark"
          maxlength="255"
          rows="2"
          :disabled="submitting"
          placeholder="例如：发运批次 / 客户单号"
          class="agg-dialog__control agg-dialog__control--textarea"
          data-test="agg-dialog-remark-input"
        ></textarea>
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
        data-test="agg-dialog-submit"
        @click="handleSubmit"
      >
        {{ submitting ? '提交中…' : '提交绑定' }}
      </BaseButton>
    </template>
  </BaseDialog>

  <QRScanner
    v-if="scannerOpen"
    @scan="handleScanResult"
    @close="handleScannerClose"
  />
</template>

<style scoped>
.agg-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.agg-dialog__hero {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 10px;
  background: var(--primary-soft);
  border: 1px solid color-mix(in srgb, var(--primary) 15%, transparent);
}
.agg-dialog__eyebrow {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: var(--primary);
}
.agg-dialog__hint {
  margin: 0;
  font-size: 12.5px;
  color: var(--ink-subtle);
  line-height: 1.55;
}
.agg-dialog__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.agg-dialog__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
}
.agg-dialog__required {
  color: var(--error);
  margin-left: 2px;
}
.agg-dialog__field-hint {
  margin: 0;
  font-size: 11.5px;
  color: var(--ink-tertiary);
  line-height: 1.45;
}
.agg-dialog__control {
  height: 32px;
  width: 100%;
  border-radius: 8px;
  border: 1px solid var(--hairline);
  background: var(--surface-1);
  padding: 0 10px;
  font-size: 13px;
  color: var(--ink);
  font-family: inherit;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.agg-dialog__control--mono {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
}
.agg-dialog__control--textarea {
  height: auto;
  padding: 8px 10px;
  resize: vertical;
  line-height: 1.5;
}
.agg-dialog__control:focus {
  border-color: var(--primary-focus, #5e69d1);
  box-shadow: 0 0 0 3px rgba(94, 106, 210, 0.15);
}
.agg-dialog__control:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.agg-dialog__chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.agg-dialog__chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 32px;
  padding: 0 12px;
  border: 1px solid var(--hairline);
  border-radius: 8px;
  background: var(--surface-1);
  color: var(--ink-muted);
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s, color 0.15s;
  font-size: 12.5px;
  font-weight: 500;
}
.agg-dialog__chip:hover {
  border-color: var(--ink-tertiary);
  color: var(--ink);
}
.agg-dialog__chip--active {
  background: var(--primary-soft);
  border-color: color-mix(in srgb, var(--primary) 40%, transparent);
  color: var(--primary);
}
.agg-dialog__chip input {
  margin: 0;
  accent-color: var(--primary);
}
.agg-dialog__scan-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}

@media (max-width: 639.98px) {
  .agg-dialog__scan-row {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
