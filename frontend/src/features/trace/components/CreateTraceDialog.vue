<script setup>
import { ref, watch } from 'vue'
import { Factory } from 'lucide-vue-next'
import BaseDialog from '@/shared/components/ui/BaseDialog.vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import { createTrace } from '../api'
import { getParts } from '@/features/part/api'

/**
 * CreateTraceDialog —— 生产赋码（批量创建溯源码）对话框（Linear-light）。
 *
 * 视觉契约：BaseDialog 外壳；hero 用 var(--primary-soft) + primary 描边引导"为新生产批次生成唯一溯源码"。
 * 接口契约（api-doc.md 2.1）：POST /api/traces，必填 partCode|spuId / quantity / manufacturerNode / province / city。
 *
 * 测试契约：
 *   - 保留 footer 中按钮顺序：第 0 个=取消，第 1 个=提交（CreateTraceDialog.contract.test.js 用 wrapper.findAll('button')[1] 命中）
 *   - 保留 3 个 text 输入按 manufacturerNode / province / city 顺序（同测试用 wrapper.findAll('input[type="text"]') 索引断言）
 *   - 保留 1 个 select（partCode）+ 1 个 number（quantity）
 *   - emit('success', traceCodes) 仍是 string[]
 */
const props = defineProps({
  modelValue: Boolean
})

/**
 * 事件契约：
 * - update:modelValue：v-model 同步弹窗显隐
 * - success：赋码请求成功后回传**新生成的溯源码字符串数组**（quantity≥1，由后端批量生成；签名为 string[]，被 CreateTraceDialog.contract.test.js 锁定）
 */
const emit = defineEmits(['update:modelValue', 'success'])

// 与后端 ProduceAssignRequest.MAX_QUANTITY 对齐：单次最多生成 500 条溯源码。
// 前端在此卡上限，避免明显越界的请求白白走一次签名 / 上链流程被后端拒掉。
const MAX_QUANTITY = 500
const MIN_QUANTITY = 1

const submitting = ref(false)
const error = ref('')

const formData = ref({
  spuId: '',
  partCode: '',
  quantity: 1,
  manufacturerNode: '',
  province: '',
  city: ''
})

const parts = ref([])
const partsLoading = ref(false)

async function loadParts() {
  partsLoading.value = true
  try {
    const res = await getParts({ page: 1, size: 100 })
    parts.value = res.list || []
  } catch (e) {
    console.error('加载配件失败', e)
  } finally {
    partsLoading.value = false
  }
}

function validateForm() {
  const hasPart =
    (formData.value.partCode && formData.value.partCode.trim()) ||
    (formData.value.spuId && !isNaN(Number(formData.value.spuId)))
  if (!hasPart) {
    error.value = '请选择一个配件或输入有效的产品 ID'
    return false
  }
  const quantity = Number(formData.value.quantity)
  if (!quantity || quantity < MIN_QUANTITY) {
    error.value = `数量必须不小于 ${MIN_QUANTITY}`
    return false
  }
  if (quantity > MAX_QUANTITY) {
    error.value = `单次生成数量不能超过 ${MAX_QUANTITY}，如需更多请分批提交`
    return false
  }
  if (!Number.isInteger(quantity)) {
    error.value = '数量必须是整数'
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

function resetForm() {
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

function handleClose() {
  if (submitting.value) return
  emit('update:modelValue', false)
  setTimeout(resetForm, 300)
}

async function handleSubmit() {
  error.value = ''
  if (!validateForm()) return

  submitting.value = true
  try {
    const payload = {
      quantity: Number(formData.value.quantity),
      manufacturerNode: formData.value.manufacturerNode.trim(),
      province: formData.value.province.trim(),
      city: formData.value.city.trim()
    }
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

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      resetForm()
      loadParts()
    }
  },
  { immediate: true }
)
</script>

<template>
  <BaseDialog
    :model-value="modelValue"
    title="生产赋码"
    subtitle="为新生产的零配件批次生成唯一溯源码，系统将自动记录生产信息和初始哈希值。"
    :icon="Factory"
    size="md"
    :persistent="submitting"
    @update:model-value="handleClose"
  >
    <div class="create-trace">
      <div v-if="error" class="create-trace__error" data-test="create-trace-error">{{ error }}</div>

      <div class="create-trace__field">
        <label class="create-trace__label">产品（选择配件） <span class="create-trace__star">*</span></label>
        <div v-if="partsLoading" class="create-trace__loading">正在加载配件列表...</div>
        <select v-else v-model="formData.partCode" class="create-trace__select">
          <option value="">请选择配件（按编码/名称）</option>
          <option v-for="p in parts" :key="p.id" :value="p.partCode">
            {{ p.partCode }} - {{ p.partName }}
          </option>
        </select>
      </div>

      <div class="create-trace__field">
        <label class="create-trace__label">
          生产数量 <span class="create-trace__star">*</span>
          <span class="create-trace__hint">（{{ MIN_QUANTITY }}–{{ MAX_QUANTITY }}）</span>
        </label>
        <input
          v-model.number="formData.quantity"
          type="number"
          inputmode="numeric"
          :placeholder="`请输入生产数量（${MIN_QUANTITY}–${MAX_QUANTITY}）`"
          class="create-trace__input create-trace__input--mono"
        />
      </div>

      <div class="create-trace__field">
        <label class="create-trace__label">生产节点 <span class="create-trace__star">*</span></label>
        <input
          v-model="formData.manufacturerNode"
          type="text"
          placeholder="例如：北京工厂、上海生产线A"
          class="create-trace__input"
        />
      </div>

      <div class="create-trace__row">
        <div class="create-trace__field">
          <label class="create-trace__label">省份 <span class="create-trace__star">*</span></label>
          <input
            v-model="formData.province"
            type="text"
            placeholder="例如：北京"
            class="create-trace__input"
          />
        </div>
        <div class="create-trace__field">
          <label class="create-trace__label">城市 <span class="create-trace__star">*</span></label>
          <input
            v-model="formData.city"
            type="text"
            placeholder="例如：北京市"
            class="create-trace__input"
          />
        </div>
      </div>
    </div>

    <template #footer>
      <BaseButton
        variant="secondary"
        size="md"
        :disabled="submitting"
        data-test="create-trace-cancel"
        @click="handleClose"
      >
        取消
      </BaseButton>
      <BaseButton
        variant="primary"
        size="md"
        :loading="submitting"
        :disabled="submitting"
        data-test="create-trace-submit"
        @click="handleSubmit"
      >
        {{ submitting ? '生成中…' : '生成溯源码' }}
      </BaseButton>
    </template>
  </BaseDialog>
</template>

<style scoped>
.create-trace {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.create-trace__error {
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--error-soft);
  border: 1px solid color-mix(in srgb, var(--error) 18%, transparent);
  color: var(--error);
  font-size: 13px;
  line-height: 1.55;
}
.create-trace__row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.create-trace__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.create-trace__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
}
.create-trace__hint {
  margin-left: 4px;
  font-size: 12px;
  font-weight: 400;
  color: var(--ink-subtle);
}
.create-trace__star {
  color: var(--error);
  margin-left: 2px;
}
.create-trace__loading {
  font-size: 13px;
  color: var(--ink-subtle);
}
.create-trace__select,
.create-trace__input {
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
  appearance: none;
  -webkit-appearance: none;
}
.create-trace__select {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2371717a' stroke-width='2'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 10px center;
  background-size: 14px 14px;
  padding-right: 32px;
}
.create-trace__input--mono {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
  font-size: 13px;
}
.create-trace__select:focus,
.create-trace__input:focus {
  border-color: var(--primary-focus);
  box-shadow: 0 0 0 3px var(--primary-ring);
}
@media (max-width: 639.98px) {
  .create-trace__row {
    grid-template-columns: 1fr;
  }
}
</style>
