<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { QrCode } from 'lucide-vue-next'
import dayjs from 'dayjs'
import BaseDialog from '@/shared/components/ui/BaseDialog.vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import BaseFlowForm from './BaseFlowForm.vue'
import { useToast } from '@/shared/composables/useToast'
import { logger } from '@/shared/utils/logger'
import { REGIONS } from '@/shared/data/regions'
import { createEvent, getTraceCandidateFlowTasks, scanTraceFlowTask } from '@/features/trace/api'

/**
 * ScanFlowDialog —— 入库 / 出库 / 中转流转 / 最终交付共用的扫码登记对话框（Linear-light）。
 *
 * 视觉契约：
 *   - 外壳走 BaseDialog（rgba(15,23,42,0.45) 蒙层 + 12px 圆角 + 1px hairline + 移动端全屏 + 粘性吸底 footer）
 *   - 业务字段由 BaseFlowForm 负责（fromNode / toNode / province / city / eventTime / remark）
 *   - 顶部 hero 卡用 var(--primary-soft) + 1px var(--primary)/15% 描边 + 12 caption 引导文案，承载 traceCode mono chip
 *
 * 接口契约（api-doc.md 2.5）：
 *   - POST /api/traces/{traceCode}/events，actionType ∈ {INBOUND, OUTBOUND, TRANSFER, DELIVER}
 *   - 由 ScanHub 透传 idempotencyKey（crypto.randomUUID()）；本组件透传到 createEvent payload
 *   - 运单驱动模式：选定 candidate task 后改走 POST /api/trace-flow-tasks/{taskId}/scan，
 *     由后端按 task source/target + 当前 snapshot 派生 fromNode/toNode/province/city，
 *     联动 trace_flow_task.actualQuantity 与状态。
 *
 * 测试契约：保留 `formData` reactive + `handleSubmit` async，供 ScanFlowDialog.contract.test.js 通过 setupState 断言。
 */
const props = defineProps({
  modelValue: { type: Boolean, required: true },
  traceCode: { type: String, required: true },
  actionType: {
    type: String,
    required: true,
    validator: (v) => ['', 'inbound', 'outbound', 'transfer', 'deliver'].includes(v)
  },
  idempotencyKey: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'success'])
const toast = useToast()
const submitting = ref(false)

const titleMap = {
  inbound: '入库登记',
  outbound: '出库登记',
  transfer: '中转流转',
  deliver: '最终交付'
}
const operationMap = {
  inbound: '入库',
  outbound: '出库',
  transfer: '中转',
  deliver: '交付'
}
const apiActionMap = {
  inbound: 'INBOUND',
  outbound: 'OUTBOUND',
  transfer: 'TRANSFER',
  deliver: 'DELIVER'
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

// 运单候选状态
const candidateTasks = ref([])
const candidatesLoading = ref(false)
const selectedTaskId = ref('')

const expectedActionType = computed(() => apiActionMap[props.actionType] || '')

/**
 * 仅展示与用户当前选定动作（INBOUND/OUTBOUND/TRANSFER/DELIVER）类型一致的候选任务。
 * 后端按 task type + snapshot status 派生 compatibleActionType，前端按这个字段过滤。
 */
const filteredCandidateTasks = computed(() => {
  if (!expectedActionType.value) return []
  return candidateTasks.value.filter((t) => t.compatibleActionType === expectedActionType.value)
})

const selectedTask = computed(() =>
  filteredCandidateTasks.value.find((t) => String(t.id) === String(selectedTaskId.value)) || null
)

function resetForm() {
  formData.fromNode = ''
  formData.toNode = ''
  formData.province = ''
  formData.city = ''
  formData.eventTime = currentDateTime()
  formData.correctionOf = null
  formData.actionType = apiActionMap[props.actionType]
  formData.remark = ''
  selectedTaskId.value = ''
}

async function loadCandidates() {
  if (!props.traceCode) return
  candidatesLoading.value = true
  try {
    const list = await getTraceCandidateFlowTasks(props.traceCode)
    candidateTasks.value = Array.isArray(list) ? list : []
  } catch (error) {
    // 静默失败：候选任务功能不应阻断常规扫码
    logger.warn('加载运单候选失败，回退到普通扫码模式', error)
    candidateTasks.value = []
  } finally {
    candidatesLoading.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      resetForm()
      loadCandidates()
    } else {
      candidateTasks.value = []
    }
  }
)

watch(
  () => props.actionType,
  (next) => {
    formData.actionType = apiActionMap[next] || formData.actionType
    selectedTaskId.value = ''
  }
)

watch(selectedTaskId, (newId) => {
  if (!newId) return
  const task = filteredCandidateTasks.value.find((t) => String(t.id) === String(newId))
  if (!task) return
  if (task.prefillFromNode) formData.fromNode = task.prefillFromNode
  if (task.prefillToNode) formData.toNode = task.prefillToNode
  // 后端 prefill_province/city 可能是简称（"江苏"/"南京"）或带 mojibake 字节（demo seed
  // 数据 city 列存在替换符 + 孤立 UTF-16 低代理位）；先归一化到 REGIONS 的标准值再写
  // 回 formData，避免 <option> 找不到对应值导致 select 显示为空。
  const normalizedProvince = normalizeProvinceToRegionValue(task.prefillProvince)
  if (normalizedProvince) {
    formData.province = normalizedProvince
    const normalizedCity = normalizeCityToRegionValue(normalizedProvince, task.prefillCity)
    if (normalizedCity) formData.city = normalizedCity
  }
})

/**
 * 把后端返回的省份字符串归一化到 REGIONS 里的标准 value。
 * 兼容：
 *   - 已是全称（"江苏省" / "上海市" / "广西壮族自治区"）→ 直接返回
 *   - 简称（"江苏" / "上海" / "广西"）→ 加后缀匹配
 *   - 无法匹配 → 返回 ''（让用户自己选）
 */
function normalizeProvinceToRegionValue(raw) {
  if (!raw) return ''
  const trimmed = String(raw).trim()
  if (!trimmed) return ''
  if (REGIONS.some((r) => r.value === trimmed)) return trimmed
  for (const suffix of ['省', '市', '自治区']) {
    const candidate = trimmed + suffix
    if (REGIONS.some((r) => r.value === candidate)) return candidate
  }
  // 处理民族自治区简称
  const specialMap = {
    内蒙古: '内蒙古自治区',
    广西: '广西壮族自治区',
    西藏: '西藏自治区',
    宁夏: '宁夏回族自治区',
    新疆: '新疆维吾尔自治区',
    香港: '香港特别行政区',
    澳门: '澳门特别行政区'
  }
  return specialMap[trimmed] || ''
}

/**
 * 把后端返回的城市字符串归一化到 REGIONS 里对应省份下的标准 value。
 * 先剥离替换符 U+FFFD 和孤立 UTF-16 surrogate（demo 数据 mojibake 兜底）后再匹配；
 * 兜底加 "市" 后缀重试一次。
 * 直辖市（北京/上海/天津/重庆）下 REGIONS 存的是区名，市本身无法 prefill，返回 ''。
 */
function normalizeCityToRegionValue(provinceFullName, raw) {
  if (!provinceFullName || !raw) return ''
  const region = REGIONS.find((r) => r.value === provinceFullName)
  if (!region) return ''
  const cleaned = String(raw).replace(/[�\uD800-\uDFFF]/g, '').trim()
  if (!cleaned) return ''
  if (region.cities.includes(cleaned)) return cleaned
  if (region.cities.includes(cleaned + '市')) return cleaned + '市'
  const stripped = cleaned.replace(/(市|区)$/, '')
  if (stripped && region.cities.includes(stripped + '市')) return stripped + '市'
  return ''
}

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
    if (selectedTask.value) {
      // 运单驱动路径：路由到 /trace-flow-tasks/{id}/scan，由后端按任务上下文派生节点
      const taskPayload = {
        traceCode: props.traceCode,
        eventTime: formatToBackend(formData.eventTime),
        remark: formData.remark?.trim() || ''
      }
      if (props.idempotencyKey) {
        taskPayload.idempotencyKey = props.idempotencyKey
      }
      await scanTraceFlowTask(selectedTask.value.id, taskPayload)
      toast.success(`${operationName.value}记录提交成功（已计入运单 ${selectedTask.value.taskNo}）`)
      emit('update:modelValue', false)
      emit('success')
      return
    }

    // 普通扫码路径：events 接口
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

      <div
        v-if="filteredCandidateTasks.length > 0"
        class="scan-flow__task-picker"
        data-test="scan-flow-task-picker"
      >
        <label class="scan-flow__task-label" for="scan-flow-task-select">
          关联运单（可选）
        </label>
        <select
          id="scan-flow-task-select"
          v-model="selectedTaskId"
          class="scan-flow__task-select"
          data-test="scan-flow-task-select"
        >
          <option value="">不关联运单（仅写溯源事件）</option>
          <option v-for="task in filteredCandidateTasks" :key="task.id" :value="task.id">
            {{ task.taskNo }} · {{ task.taskTypeLabel }} ·
            {{ task.sourceNodeName }} → {{ task.targetNodeName }}
            · 剩余 {{ task.remainingQuantity }}/{{ task.expectedQuantity }}
          </option>
        </select>
        <p v-if="selectedTask" class="scan-flow__task-hint" data-test="scan-flow-task-hint">
          已选运单 <strong>{{ selectedTask.taskNo }}</strong>，节点 / 省 / 市 字段已自动填充；
          提交时会同时累计到运单 actualQuantity。
        </p>
        <p v-else class="scan-flow__task-hint scan-flow__task-hint--muted">
          选择一条运单可一键填表并把这次扫码计入运单进度（接通普通扫码 ↔ 任务工作台两条链路）。
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
.scan-flow__task-picker {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  border: 1px dashed color-mix(in srgb, var(--primary) 35%, transparent);
  border-radius: 10px;
  background: color-mix(in srgb, var(--primary-soft) 60%, transparent);
}
.scan-flow__task-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
}
.scan-flow__task-select {
  height: 36px;
  padding: 0 12px;
  border: 1px solid var(--hairline);
  border-radius: 8px;
  background: var(--surface-1);
  color: var(--ink);
  font-size: 13.5px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
  appearance: none;
  -webkit-appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2371717a' stroke-width='2'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 10px center;
  background-size: 14px 14px;
  padding-right: 32px;
}
.scan-flow__task-select:focus {
  border-color: var(--primary-focus);
  box-shadow: 0 0 0 3px var(--primary-ring);
}
.scan-flow__task-hint {
  margin: 0;
  font-size: 12px;
  color: var(--ink-muted);
  line-height: 1.55;
}
.scan-flow__task-hint--muted {
  color: var(--ink-subtle);
}
.scan-flow__task-hint strong {
  color: var(--primary);
  font-weight: 600;
}
</style>
