<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { QrCode } from 'lucide-vue-next'
import dayjs from 'dayjs'
import BaseDialog from '@/shared/components/ui/BaseDialog.vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import BaseFlowForm from './BaseFlowForm.vue'
import { useToast } from '@/shared/composables/useToast'
import { createEvent } from '@/features/trace/api'

/**
 * ScanFlowDialog —— 入库 / 出库 / 流转三态共用的扫码登记对话框（Linear-light）。
 *
 * 视觉契约：
 *   - 外壳走 BaseDialog（rgba(15,23,42,0.45) 蒙层 + 12px 圆角 + 1px hairline + 移动端全屏 + 粘性吸底 footer）
 *   - 业务字段由 BaseFlowForm 负责（fromNode / toNode / province / city / eventTime / remark）
 *   - 顶部 hero 卡用 var(--primary-soft) + 1px var(--primary)/15% 描边 + 12 caption 引导文案，承载 traceCode mono chip
 *
 * 接口契约（api-doc.md 2.5）：
 *   - POST /api/traces/{traceCode}/events，actionType ∈ {INBOUND, OUTBOUND, TRANSFER}
 *   - 由 ScanHub 透传 idempotencyKey（crypto.randomUUID()）；本组件透传到 createEvent payload
 *
 * 测试契约：保留 `formData` reactive + `handleSubmit` async，供 ScanFlowDialog.contract.test.js 通过 setupState 断言。
 */
const props = defineProps({
  modelValue: { type: Boolean, required: true },
  traceCode: { type: String, required: true },
  actionType: {
    type: String,
    required: true,
    validator: (v) => ['', 'inbound', 'outbound', 'transfer'].includes(v)
  },
  idempotencyKey: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'success'])
const toast = useToast()
const submitting = ref(false)

const titleMap = {
  inbound: '入库登记',
  outbound: '出库登记',
  transfer: '物流流转'
}
const operationMap = {
  inbound: '入库',
  outbound: '出库',
  transfer: '流转'
}
const apiActionMap = {
  inbound: 'INBOUND',
  outbound: 'OUTBOUND',
  transfer: 'TRANSFER'
}

const dialogTitle = computed(() => titleMap[props.actionType] || '扫码登记')
const operationName = computed(() => operationMap[props.actionType] || '登记')

const currentDateTime = () => dayjs().format('YYYY-MM-DDTHH:mm')
const formatToBackend = (datetimeLocal) =>
  datetimeLocal ? dayjs(datetimeLocal).format('YYYY-MM-DDTHH:mm:ss') : dayjs().format('YYYY-MM-DDTHH:mm:ss')

const formData = reactive({
  fromNode: '',
  toNode: '',
  province: '',
  city: '',
  eventTime: currentDateTime(),
  correctionOf: null,
  actionType: apiActionMap[props.actionType],
  remark: ''
})

function resetForm() {
  formData.fromNode = ''
  formData.toNode = ''
  formData.province = ''
  formData.city = ''
  formData.eventTime = currentDateTime()
  formData.correctionOf = null
  formData.actionType = apiActionMap[props.actionType]
  formData.remark = ''
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) resetForm()
  }
)

watch(
  () => props.actionType,
  (next) => {
    formData.actionType = apiActionMap[next] || formData.actionType
  }
)

function handleClose() {
  if (submitting.value) return
  emit('update:modelValue', false)
}

async function handleSubmit() {
  // 仅校验 eventTime 必填——其他字段（fromNode / toNode / province / city）
  // 故意不再前端强制必填，让空字段透传到后端，由 TraceUserNodeBindingServiceImpl
  // .authorizeAndResolveRoute 自动补齐：
  //   - INBOUND 没传 toNode   → 用用户唯一/默认绑定节点补齐
  //   - OUTBOUND/TRANSFER 没传 fromNode → 用用户唯一/默认绑定节点补齐
  //   - 没传 fromNode 还会从 trace_snapshot.current_node 兜底补齐
  //   - 没传 province/city 拿到操作节点后从 trace_node 取
  // 后端补齐失败会抛 BizException；用户看到 toast 即可知道得手动填了。
  if (!formData.eventTime) {
    toast.error('请选择时间')
    return
  }

  submitting.value = true
  try {
    const apiData = {
      actionType: formData.actionType,
      eventTime: formatToBackend(formData.eventTime),
      correctionOf: formData.correctionOf,
      remark: formData.remark?.trim() || ''
    }
    // 只把"用户主动填了的"字段加进去，空字段不发，让后端走补齐分支
    const fromNode = formData.fromNode?.trim()
    const toNode = formData.toNode?.trim()
    const province = formData.province?.trim()
    const city = formData.city?.trim()
    if (fromNode) apiData.fromNode = fromNode
    if (toNode) apiData.toNode = toNode
    if (province) apiData.province = province
    if (city) apiData.city = city
    if (props.idempotencyKey) {
      apiData.idempotencyKey = props.idempotencyKey
    }
    await createEvent(props.traceCode, apiData)
    toast.success(`${operationName.value}记录提交成功`)
    emit('update:modelValue', false)
    emit('success')
  } catch (error) {
    toast.error(error?.message || `${operationName.value}记录提交失败`)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <BaseDialog
    :model-value="modelValue"
    :title="dialogTitle"
    :icon="QrCode"
    size="md"
    :persistent="submitting"
    @update:model-value="handleClose"
  >
    <div class="scan-flow">
      <div class="scan-flow__hero" data-test="scan-flow-hero">
        <span class="scan-flow__eyebrow">追溯码</span>
        <span class="scan-flow__code mono">{{ traceCode }}</span>
        <p class="scan-flow__hint">
          系统会以 RSA 数字签名 + SHA-256 哈希链生成不可篡改记录；幂等键已注入，重复提交不会产生第二条日志。
        </p>
      </div>

      <BaseFlowForm
        :action-type="actionType"
        :model-value="formData"
        @update:modelValue="Object.assign(formData, $event)"
      />
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
        {{ submitting ? '提交中…' : `提交${operationName}` }}
      </BaseButton>
    </template>
  </BaseDialog>
</template>

<style scoped>
.scan-flow {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.scan-flow__hero {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  padding: 14px 16px;
  border-radius: 10px;
  background: var(--primary-soft);
  border: 1px solid color-mix(in srgb, var(--primary) 15%, transparent);
}
.scan-flow__eyebrow {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: var(--primary);
}
.scan-flow__code {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: -0.2px;
}
.scan-flow__hint {
  margin: 4px 0 0;
  flex: 1 1 100%;
  font-size: 12.5px;
  color: var(--ink-subtle);
  line-height: 1.55;
}
</style>
