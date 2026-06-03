<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { Package, Plus, ScanLine, X } from 'lucide-vue-next'
import BaseDialog from '@/shared/components/ui/BaseDialog.vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import QRScanner from '@/shared/components/QRScanner.vue'
import { useToast } from '@/shared/composables/useToast'
import { logger } from '@/shared/utils/logger'
import { bindAggregationBatch } from '@/features/trace/api'

/**
 * AggregationBindDialog —— 装箱 / 装托盘绑定对话框（Linear-light）。
 *
 * 两种用法：
 *   1. 新建装箱 / 装托：父码可填、类型可选，连续扫码 / 逐个录入多个子码。
 *   2. 向已有箱/托盘「添加成员」：传 presetParentCode → 父码与类型锁定，只录子码。
 *
 * 子码采用「列表 + 连续扫码」模式：
 *   - QRScanner 每帧 emit('scan')，取景器保持常开，逐个累加；靠列表去重消化重复帧
 *     （vue-qrcode-reader 会对同一码持续触发，set 成员判断即天然防抖）。
 *   - 提交走批量端点 POST /api/trace-aggregations/batch（「跳过失败继续」语义）。
 *   - 部分/全部失败时不关弹窗：失败子码连同原因保留在列表里，去掉已成功的，便于改完重试。
 *
 * 接口契约：
 *   - POST /api/trace-aggregations/batch，权限 trace:scan / trace:task:scan / trace:outbound / trace:transfer / trace:create
 *   - 成功（至少一条）后 emit('success', result) 让父组件刷新列表；全部失败不 emit。
 *
 * Props：
 *   - modelValue: 显隐双向绑定
 *   - defaultRelationType: 'CARTON' | 'PALLET'（新建时由「新建装箱」/「新建装托」决定；添加成员时由所属分组决定）
 *   - presetParentCode: 非空 → 添加成员模式，锁定父码 + 类型
 */
const props = defineProps({
  modelValue: { type: Boolean, required: true },
  defaultRelationType: {
    type: String,
    default: 'CARTON',
    validator: (v) => ['CARTON', 'PALLET'].includes(v)
  },
  presetParentCode: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'success'])
const toast = useToast()

const submitting = ref(false)
const scannerOpen = ref(false)
const childInputRef = ref(null)

const formData = reactive({
  relationType: props.defaultRelationType,
  parentCode: '',
  remark: ''
})

// 已添加的子码（统一大写去重；后端 normalizeChildCodes 也大写归一，口径一致）。
const childCodes = ref([])
const childInput = ref('')
// 上一批提交被拒绝的子码 + 原因，用于在弹窗内回显并支持「改完重试」。
const failedItems = ref([])

const parentLocked = computed(() => !!props.presetParentCode)

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

const dialogTitle = computed(() => {
  if (parentLocked.value) return `向 ${props.presetParentCode} 添加成员`
  return titleMap[formData.relationType] || '新建聚合关系'
})
const childHintText = computed(() => childHint[formData.relationType] || '')
const parentHintText = computed(() => parentPrefixHint[formData.relationType] || '')
const childCount = computed(() => childCodes.value.length)

function normalizeCode(code) {
  return typeof code === 'string' ? code.trim().toUpperCase() : ''
}

function resetForm() {
  formData.relationType = props.defaultRelationType
  formData.parentCode = props.presetParentCode || ''
  formData.remark = ''
  childCodes.value = []
  childInput.value = ''
  failedItems.value = []
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
  },
  // immediate：弹窗若以「已打开」状态挂载（如直接传 modelValue=true），也要立刻套用 presetParentCode。
  { immediate: true }
)

watch(
  () => props.defaultRelationType,
  (next) => {
    if (props.modelValue && !parentLocked.value) {
      formData.relationType = next
    }
  }
)

function failureFor(code) {
  return failedItems.value.find((item) => item.childCode === code) || null
}

/**
 * 添加一个子码到列表。返回是否真的加入（用于决定是否清空输入框）。
 * fromScan=true 时不弹「重复/为空」错误，避免连续扫码刷屏。
 */
function addChild(rawCode, { fromScan = false } = {}) {
  const code = normalizeCode(rawCode)
  if (!code) {
    if (!fromScan) toast.error('请输入子码')
    return false
  }
  if (code.length > 64) {
    toast.error('子码长度不能超过 64')
    return false
  }
  const parent = normalizeCode(formData.parentCode)
  if (parent && code === parent) {
    toast.error('子码不能与父码相同')
    return false
  }
  if (childCodes.value.includes(code)) {
    if (!fromScan) toast.info('该子码已在列表中')
    return false
  }
  childCodes.value.push(code)
  // 重新加入则清掉它此前的失败标记。
  failedItems.value = failedItems.value.filter((item) => item.childCode !== code)
  if (fromScan) toast.success(`已扫码添加：${code}`)
  return true
}

function handleAddFromInput() {
  if (addChild(childInput.value)) {
    childInput.value = ''
    nextTick(() => childInputRef.value?.focus?.())
  }
}

function removeChild(code) {
  childCodes.value = childCodes.value.filter((c) => c !== code)
  failedItems.value = failedItems.value.filter((item) => item.childCode !== code)
}

function handleScannerOpen() {
  scannerOpen.value = true
}

function handleScannerClose() {
  scannerOpen.value = false
}

function handleScanResult(code) {
  // 连续扫码：取景器保持常开，逐个累加，靠列表去重消化重复帧；用户点扫码器关闭按钮结束。
  addChild(code, { fromScan: true })
}

function validate() {
  if (!formData.relationType) return '请选择聚合类型'
  if (!normalizeCode(formData.parentCode)) return '请输入父码（箱码 / 托盘码）'
  if (childCodes.value.length === 0) return '请至少添加一个子码'
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
      parentCode: normalizeCode(formData.parentCode),
      childCodes: [...childCodes.value]
    }
    const remark = formData.remark.trim()
    if (remark) payload.remark = remark

    const result = await bindAggregationBatch(payload)
    const successCount = result?.successCount ?? 0
    const failureCount = result?.failureCount ?? 0
    const failed = Array.isArray(result?.failed) ? result.failed : []

    if (failureCount === 0) {
      toast.success(`成功绑定 ${successCount} 个子码`)
      emit('update:modelValue', false)
      emit('success', result)
      return
    }

    // 部分 / 全部失败：保留失败子码 + 原因，剔除已成功的，弹窗不关，便于改完重试。
    failedItems.value = failed.map((item) => ({
      childCode: normalizeCode(item.childCode),
      message: item.message || '绑定失败'
    }))
    const failedSet = new Set(failedItems.value.map((item) => item.childCode))
    childCodes.value = childCodes.value.filter((code) => failedSet.has(code))

    if (successCount > 0) {
      toast.warning(`成功 ${successCount} 个，失败 ${failureCount} 个；失败项已保留，请修正后重试`)
      emit('success', result)
    } else {
      toast.error(`全部 ${failureCount} 个绑定失败，请检查后重试`)
    }
  } catch (error) {
    logger.error('批量聚合绑定失败', error)
    // request.js 已 toast 网络 / 4xx 错误（如整批参数非法），这里不重复
  } finally {
    submitting.value = false
  }
}

function handleClose() {
  if (submitting.value) return
  emit('update:modelValue', false)
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
          可一次绑定多个子码（连续扫码 / 逐个录入）。每个子码独立上链写入 PACK / PALLETIZE 事件；
          个别子码失败（已在别的箱里、状态 IN_TRANSIT / 异常等）会被跳过并提示，不影响其他子码。
        </p>
      </div>

      <div class="agg-dialog__field">
        <span class="agg-dialog__label">聚合类型 <span class="agg-dialog__required">*</span></span>
        <div class="agg-dialog__chips" role="radiogroup" aria-label="聚合类型">
          <label
            class="agg-dialog__chip"
            :class="{
              'agg-dialog__chip--active': formData.relationType === 'CARTON',
              'agg-dialog__chip--locked': parentLocked
            }"
            data-test="agg-dialog-relation-carton"
          >
            <input
              v-model="formData.relationType"
              type="radio"
              value="CARTON"
              :disabled="submitting || parentLocked"
              name="relation-type"
            />
            <span>箱码（CARTON）</span>
          </label>
          <label
            class="agg-dialog__chip"
            :class="{
              'agg-dialog__chip--active': formData.relationType === 'PALLET',
              'agg-dialog__chip--locked': parentLocked
            }"
            data-test="agg-dialog-relation-pallet"
          >
            <input
              v-model="formData.relationType"
              type="radio"
              value="PALLET"
              :disabled="submitting || parentLocked"
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
          :disabled="submitting || parentLocked"
          :placeholder="parentHintText"
          class="agg-dialog__control agg-dialog__control--mono"
          data-test="agg-dialog-parent-input"
        />
        <p class="agg-dialog__field-hint">
          {{ parentLocked ? '添加成员模式：父码与类型已锁定。' : parentHintText }}
        </p>
      </div>

      <div class="agg-dialog__field">
        <div class="agg-dialog__label-row">
          <label class="agg-dialog__label" for="agg-dialog-child-code">
            子码 <span class="agg-dialog__required">*</span>
          </label>
          <span class="agg-dialog__count" data-test="agg-dialog-child-count">已添加 {{ childCount }}</span>
        </div>
        <div class="agg-dialog__scan-row">
          <input
            id="agg-dialog-child-code"
            ref="childInputRef"
            v-model="childInput"
            type="text"
            maxlength="64"
            :disabled="submitting"
            placeholder="单品码 / 箱码，回车或点「添加」"
            class="agg-dialog__control agg-dialog__control--mono"
            data-test="agg-dialog-child-input"
            @keyup.enter.prevent="handleAddFromInput"
          />
          <BaseButton
            variant="secondary"
            size="sm"
            :disabled="submitting"
            data-test="agg-dialog-add-child"
            @click="handleAddFromInput"
          >
            <template #icon><Plus :size="13" /></template>
            添加
          </BaseButton>
          <BaseButton
            variant="secondary"
            size="sm"
            :disabled="submitting"
            data-test="agg-dialog-open-scanner"
            @click="handleScannerOpen"
          >
            <template #icon><ScanLine :size="13" /></template>
            扫码
          </BaseButton>
        </div>
        <p class="agg-dialog__field-hint">{{ childHintText }}；可连续扫码，重复码自动去重。</p>

        <ul v-if="childCount" class="agg-dialog__chips-list" data-test="agg-dialog-child-list">
          <li
            v-for="code in childCodes"
            :key="code"
            class="agg-dialog__code-chip"
            :class="{ 'agg-dialog__code-chip--error': failureFor(code) }"
            :data-test="`agg-dialog-child-chip-${code}`"
          >
            <span class="agg-dialog__code-chip-text mono">{{ code }}</span>
            <span
              v-if="failureFor(code)"
              class="agg-dialog__code-chip-error"
              :title="failureFor(code).message"
            >
              {{ failureFor(code).message }}
            </span>
            <button
              type="button"
              class="agg-dialog__code-chip-remove"
              :disabled="submitting"
              aria-label="移除子码"
              :data-test="`agg-dialog-child-remove-${code}`"
              @click="removeChild(code)"
            >
              <X :size="12" />
            </button>
          </li>
        </ul>
        <p v-else class="agg-dialog__empty-hint" data-test="agg-dialog-child-empty">
          还没有子码——逐个录入或连续扫码添加。
        </p>
      </div>

      <div class="agg-dialog__field">
        <label class="agg-dialog__label" for="agg-dialog-remark">备注（可选）</label>
        <textarea
          id="agg-dialog-remark"
          v-model="formData.remark"
          maxlength="255"
          rows="2"
          :disabled="submitting"
          placeholder="例如：发运批次 / 客户单号（对本批所有子码生效）"
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
        :disabled="submitting || childCount === 0"
        data-test="agg-dialog-submit"
        @click="handleSubmit"
      >
        {{ submitting ? '提交中…' : `提交绑定（${childCount}）` }}
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
.agg-dialog__label-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}
.agg-dialog__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
}
.agg-dialog__count {
  font-size: 11.5px;
  font-weight: 500;
  color: var(--primary);
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
.agg-dialog__empty-hint {
  margin: 4px 0 0;
  font-size: 11.5px;
  color: var(--ink-tertiary);
  font-style: italic;
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
.agg-dialog__chip--locked {
  cursor: not-allowed;
  opacity: 0.75;
}
.agg-dialog__chip input {
  margin: 0;
  accent-color: var(--primary);
}
.agg-dialog__scan-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 8px;
  align-items: center;
}

.agg-dialog__chips-list {
  list-style: none;
  margin: 6px 0 0;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.agg-dialog__code-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  height: 28px;
  padding: 0 4px 0 10px;
  border: 1px solid var(--hairline);
  border-radius: 8px;
  background: var(--surface-2);
}
.agg-dialog__code-chip--error {
  border-color: #f8c8ca;
  background: var(--error-soft);
}
.agg-dialog__code-chip-text {
  font-size: 12px;
  color: var(--ink);
  white-space: nowrap;
}
.agg-dialog__code-chip-error {
  font-size: 11px;
  color: var(--error);
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.agg-dialog__code-chip-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--ink-tertiary);
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s, color 0.15s;
}
.agg-dialog__code-chip-remove:hover:not(:disabled) {
  background: var(--surface-1);
  color: var(--ink);
}
.agg-dialog__code-chip-remove:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.mono {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
}

@media (max-width: 639.98px) {
  .agg-dialog__scan-row {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
