<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import {
  ClipboardCopy,
  ExternalLink,
  Factory,
  FileSearch,
  PackageCheck,
  Printer,
  QrCode,
  RefreshCw,
  ScanLine
} from 'lucide-vue-next'
import PageHeader from '@/shared/components/ui/PageHeader.vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import BaseInput from '@/shared/components/ui/BaseInput.vue'
import StatusPill from '@/shared/components/ui/StatusPill.vue'
import TraceCodeChip from '@/shared/components/ui/TraceCodeChip.vue'
import EmptyState from '@/shared/components/ui/EmptyState.vue'
import LoadingSkeleton from '@/shared/components/ui/LoadingSkeleton.vue'
import KbdShortcut from '@/shared/components/ui/KbdShortcut.vue'
import QRScanner from '@/shared/components/QRScanner.vue'
import PrintLabelDialog from '@/features/trace/components/PrintLabelDialog.vue'
import { useConfirm } from '@/shared/composables/useConfirm'
import { useToast } from '@/shared/composables/useToast'
import { usePrompt } from '@/shared/composables/usePrompt'
import { getParts } from '@/features/part/api'
import {
  activateTraceCode,
  createTrace,
  getTraceBatch,
  getTraceBatchCodes,
  getTraceCodeByCode,
  getTraceNodes,
  printTraceCode,
  reprintTraceCode,
  voidTraceCode
} from '@/features/trace/api'
import { canActivate, canPrint, canReprint, canVoid } from '@/features/trace/utils/codeActions'
import { logger } from '@/shared/utils/logger'

const router = useRouter()
const toast = useToast()
const { confirm } = useConfirm()
const { prompt } = usePrompt()

const parts = ref([])
const nodes = ref([])
const loadingOptions = ref(false)
const creating = ref(false)
const loadingBatch = ref(false)
const actionLoadingKey = ref('')
const scannerOpen = ref(false)
const selectedTraceCode = ref('')
const batchDetail = ref(null)
const batchCodes = ref([])
const createError = ref('')
const lookupBatchId = ref('')
const lookupTraceCode = ref('')
const loadingLookupTraceCode = ref(false)
const activationDeviceId = ref('')

// 打印对话框状态。pendingAction 描述确认后要发起的请求：
//   { kind: 'print' | 'reprint' | 'batch-print', codes, reason? }
const printDialogOpen = ref(false)
const printDialogTitle = ref('打印标签')
const printDialogConfirmText = ref('打印')
const printDialogMode = ref('print')
const printDialogCodes = ref([])
const pendingPrintAction = ref(null)

const createForm = reactive({
  partCode: '',
  quantity: 1,
  batchNo: '',
  productionOrderNo: '',
  manufacturerNodeId: '',
  manufacturerNode: '',
  province: '',
  city: ''
})

const STATUS_TEXT = {
  GENERATED: '已生成',
  PRINTED: '已打印',
  ACTIVATED: '已激活',
  IN_STOCK: '已入库',
  IN_TRANSIT: '运输中',
  TRANSFERRED: '已交付',
  EXCEPTION: '异常',
  VOIDED: '已作废',
  SCRAPPED: '已报废'
}

const RECONCILIATION_TONE = {
  CONSISTENT: 'success',
  PASS: 'success',
  OK: 'success',
  COMPLETE: 'success',
  DISCREPANCY: 'warn',
  MISMATCH: 'warn',
  WARNING: 'warn',
  IN_PROGRESS: 'mute',
  PENDING: 'mute'
}

const hasBatch = computed(() => Boolean(batchDetail.value?.batchId))
// 按追溯码反查 v11 历史回填的码可能没有批次——批次卡片不能渲染，但码列表要能显示这一行
const hasCodesToShow = computed(() => hasBatch.value || sortedBatchCodes.value.length > 0)

const sortedBatchCodes = computed(() =>
  [...batchCodes.value].sort((left, right) => {
    const serialDiff = Number(left.serialNo || 0) - Number(right.serialNo || 0)
    if (serialDiff !== 0) return serialDiff
    return String(left.traceCode || '').localeCompare(String(right.traceCode || ''))
  })
)

const activeCode = computed(() =>
  sortedBatchCodes.value.find((code) => code.traceCode === selectedTraceCode.value) || null
)

const generatedCount = computed(() => sortedBatchCodes.value.length)
const printedCount = computed(
  () => sortedBatchCodes.value.filter((code) => Number(code.printCount || 0) > 0).length
)
const activatedCount = computed(
  () => sortedBatchCodes.value.filter((code) => isActivatedStatus(code.codeStatus)).length
)
const voidedCount = computed(
  () => sortedBatchCodes.value.filter((code) => code.codeStatus === 'VOIDED').length
)

const selectedNode = computed(
  () => nodes.value.find((node) => String(node.id) === String(createForm.manufacturerNodeId)) || null
)

const reconciliationCards = computed(() => [
  {
    key: 'requested',
    label: '计划',
    value: pickNumber(batchDetail.value?.quantityRequested, Number(createForm.quantity || 0)),
    hint: '本次目标数量'
  },
  {
    key: 'generated',
    label: '已生成',
    value: pickNumber(batchDetail.value?.quantityGenerated, generatedCount.value),
    hint: '落库单品码'
  },
  {
    key: 'printed',
    label: '已打印',
    value: pickNumber(batchDetail.value?.quantityPrinted, printedCount.value),
    hint: `${pickNumber(batchDetail.value?.printOperationCount, 0)} 次操作`
  },
  {
    key: 'activated',
    label: '已激活',
    value: pickNumber(batchDetail.value?.quantityActivated, activatedCount.value),
    hint: '贴码后扫码复核'
  },
  {
    key: 'inbound',
    label: '已入库',
    value: pickNumber(batchDetail.value?.quantityInbound, 0),
    hint: '进入库存'
  },
  {
    key: 'voided',
    label: '已作废',
    value: pickNumber(batchDetail.value?.quantityVoided, voidedCount.value),
    hint: '损坏 / 余码作废',
    tone: 'danger'
  }
])

const reconciliationTone = computed(() => {
  if (!batchDetail.value) return 'mute'
  if (batchDetail.value.consistent) return 'success'
  return RECONCILIATION_TONE[String(batchDetail.value.reconciliationStatus || '').toUpperCase()] || 'warn'
})

const reconciliationStatusLabel = computed(() => batchDetail.value?.reconciliationStatus || '-')
const reconciliationConsistent = computed(() => Boolean(batchDetail.value?.consistent))
const discrepancyReasons = computed(() => batchDetail.value?.discrepancyReasons || [])
const batchPillLabel = computed(() => {
  if (!batchDetail.value) return ''
  return batchDetail.value.batchNo || `#${batchDetail.value.batchId}`
})

const headerSubtitle = computed(() => {
  if (!hasBatch.value) {
    return '按 SPU 创建赋码批次，生成单品追溯码并完成打印 / 激活 / 作废对账。'
  }
  const batch = batchDetail.value
  const total = pickNumber(batch?.quantityRequested, generatedCount.value)
  return `批次 ${batch.batchNo || batch.batchId} · ${activatedCount.value}/${total} 已激活 · ${printedCount.value}/${total} 已打印`
})

onMounted(() => {
  loadOptions()
})

async function loadOptions() {
  loadingOptions.value = true
  try {
    const [partResponse, nodeResponse] = await Promise.all([
      getParts({ page: 1, size: 200 }),
      getTraceNodes().catch((error) => {
        logger.warn('加载结构化节点失败，回退手填生产节点', error)
        return []
      })
    ])
    parts.value = partResponse?.list || []
    nodes.value = Array.isArray(nodeResponse) ? nodeResponse : []
  } catch (error) {
    logger.error('加载生产赋码选项失败', error)
    toast.error(error?.message || '加载生产赋码选项失败')
  } finally {
    loadingOptions.value = false
  }
}

function handleNodeChange() {
  const node = selectedNode.value
  if (!node) return
  createForm.manufacturerNode = node.nodeName || node.nodeCode || ''
  createForm.province = node.province || ''
  createForm.city = node.city || ''
}

function validateCreateForm() {
  if (!createForm.partCode) return '请选择产品 / 配件'
  const quantityNumber = Number(createForm.quantity)
  if (!quantityNumber || quantityNumber < 1 || quantityNumber > 500) {
    return '生产数量必须在 1 到 500 之间'
  }
  if (!createForm.manufacturerNode?.trim()) return '请选择或填写生产节点'
  if (!createForm.province?.trim()) return '请填写省份'
  if (!createForm.city?.trim()) return '请填写城市'
  return ''
}

async function handleCreateBatch() {
  createError.value = ''
  const validationMessage = validateCreateForm()
  if (validationMessage) {
    createError.value = validationMessage
    toast.error(validationMessage)
    return
  }
  creating.value = true
  try {
    const payload = {
      partCode: createForm.partCode,
      quantity: Number(createForm.quantity),
      manufacturerNode: createForm.manufacturerNode.trim(),
      province: createForm.province.trim(),
      city: createForm.city.trim()
    }
    if (createForm.batchNo?.trim()) payload.batchNo = createForm.batchNo.trim()
    if (createForm.productionOrderNo?.trim()) payload.productionOrderNo = createForm.productionOrderNo.trim()
    if (createForm.manufacturerNodeId) payload.manufacturerNodeId = Number(createForm.manufacturerNodeId)

    const response = await createTrace(payload)
    toast.success(`赋码批次 ${response.batchNo || ''} 创建成功`)
    await loadBatch(response.batchId, response.traceCodes || [])
  } catch (error) {
    logger.error('创建赋码批次失败', error)
    createError.value = error?.message || '创建赋码批次失败'
    toast.error(createError.value)
  } finally {
    creating.value = false
  }
}

async function handleLookupBatch() {
  if (!lookupBatchId.value) {
    toast.error('请输入批次 ID')
    return
  }
  await loadBatch(lookupBatchId.value)
}

async function handleLookupTraceCode() {
  const code = String(lookupTraceCode.value || '').trim()
  if (!code) {
    toast.error('请输入追溯码')
    return
  }
  loadingLookupTraceCode.value = true
  try {
    const record = await getTraceCodeByCode(code)
    if (record?.batchId) {
      // 走批次：复用现有 loadBatch 完整对账加载，再把 selectedTraceCode 高亮到目标码
      await loadBatch(record.batchId)
      lookupTraceCode.value = record.traceCode || code
      selectedTraceCode.value = record.traceCode || code
    } else {
      // v11 历史回填的码 batch_id 为 NULL——没有对账批次可加载，单行展示，行内按钮按状态启用
      batchDetail.value = null
      batchCodes.value = [{
        serialNo: record?.serialNo || 1,
        printCount: record?.printCount || 0,
        codeStatus: record?.codeStatus || 'GENERATED',
        batchId: null,
        traceCode: record?.traceCode || code,
        spuId: record?.spuId,
        qrPayload: record?.qrPayload || '',
        activatedTime: record?.activatedTime,
        activatedByUsername: record?.activatedByUsername
      }]
      selectedTraceCode.value = record?.traceCode || code
      lookupBatchId.value = ''
      lookupTraceCode.value = record?.traceCode || code
    }
  } catch (error) {
    logger.error('按追溯码查询失败', error)
    // request.js 已经按 HTTP 状态自动 toast 错误信息（404 时是后端"追溯码不存在: X"）；
    // 这里只在 catch 里做兜底，避免双 toast
  } finally {
    loadingLookupTraceCode.value = false
  }
}

async function loadBatch(batchId, fallbackCodes = []) {
  if (!batchId) return
  loadingBatch.value = true
  try {
    const [detail, codes] = await Promise.all([
      getTraceBatch(batchId),
      getTraceBatchCodes(batchId).catch((error) => {
        logger.warn('加载批次码列表失败，使用创建响应兜底', error)
        return []
      })
    ])
    batchDetail.value = detail
    lookupBatchId.value = String(detail?.batchId || batchId)
    batchCodes.value = normalizeBatchCodes(codes, fallbackCodes, detail)
    selectedTraceCode.value = batchCodes.value[0]?.traceCode || ''
  } catch (error) {
    logger.error('加载赋码批次失败', error)
    toast.error(error?.message || '加载赋码批次失败')
  } finally {
    loadingBatch.value = false
  }
}

function normalizeBatchCodes(codes = [], fallbackCodes = [], detail = null) {
  if (Array.isArray(codes) && codes.length > 0) {
    return codes.map((code, index) => ({
      serialNo: code.serialNo || index + 1,
      printCount: code.printCount || 0,
      codeStatus: code.codeStatus || 'GENERATED',
      batchId: code.batchId || detail?.batchId,
      ...code
    }))
  }
  return (fallbackCodes || []).map((traceCode, index) => ({
    batchId: detail?.batchId,
    traceCode,
    serialNo: index + 1,
    qrPayload: traceCode,
    codeStatus: 'GENERATED',
    printCount: 0
  }))
}

async function refreshCurrentBatch(options = {}) {
  if (!batchDetail.value?.batchId) return
  const previous = selectedTraceCode.value
  await loadBatch(batchDetail.value.batchId)
  selectedTraceCode.value = options.selectTraceCode || previous || batchCodes.value[0]?.traceCode || ''
}

function selectCode(traceCode) {
  selectedTraceCode.value = traceCode
}

const selectedPart = computed(() =>
  parts.value.find((p) => p.partCode === createForm.partCode) || null
)
const partLabel = computed(() => {
  const p = selectedPart.value || (batchDetail.value?.partCode
    ? parts.value.find((x) => x.partCode === batchDetail.value.partCode)
    : null)
  if (!p) return batchDetail.value?.partCode || ''
  return `${p.partCode} · ${p.partName}`
})

async function runCodeAction(code, action) {
  if (!code?.traceCode) return
  if (action === 'print') {
    openPrintDialog({
      kind: 'print',
      codes: [code],
      title: '打印标签',
      confirmText: '打印',
      mode: 'print'
    })
    return
  }
  if (action === 'reprint') {
    const reason = await promptReason('重打标签', `请填写 ${code.traceCode} 的重打原因`)
    if (!reason) return
    openPrintDialog({
      kind: 'reprint',
      codes: [code],
      reason,
      title: '重打标签',
      confirmText: '打印（重打）',
      mode: 'reprint'
    })
    return
  }

  const key = `${action}:${code.traceCode}`
  actionLoadingKey.value = key
  try {
    if (action === 'void') {
      const accepted = await confirm({
        title: '作废标签',
        message: `确定作废 ${code.traceCode} 吗？作废后不能激活或流转。`,
        confirmText: '确认作废',
        cancelText: '取消',
        type: 'danger'
      })
      if (!accepted) return
      const reason = await promptReason('作废标签', '请填写标签丢失、损坏或余码作废原因')
      if (!reason) return
      await voidTraceCode(code.traceCode, { remark: reason })
      toast.success('标签作废已记录')
    } else if (action === 'activate') {
      await activateTraceCode(code.traceCode, buildActivationPayload('生产工作台手动激活'))
      toast.success('单品码已激活')
    }
    await refreshCurrentBatch({ selectTraceCode: code.traceCode })
  } catch (error) {
    logger.error('单品码操作失败', error)
    toast.error(error?.message || '单品码操作失败')
  } finally {
    actionLoadingKey.value = ''
  }
}

function openPrintDialog({ kind, codes, reason, title, confirmText, mode }) {
  pendingPrintAction.value = { kind, codes, reason: reason || '' }
  printDialogCodes.value = codes
  printDialogTitle.value = title
  printDialogConfirmText.value = confirmText
  printDialogMode.value = mode
  printDialogOpen.value = true
}

function closePrintDialog() {
  printDialogOpen.value = false
  pendingPrintAction.value = null
  printDialogCodes.value = []
}

async function handlePrintDialogConfirm() {
  const action = pendingPrintAction.value
  if (!action) {
    printDialogOpen.value = false
    return
  }
  const { kind, codes, reason } = action
  // 关闭对话框前先把状态清掉，避免 window.print() 时 dialog 还遮罩；
  // dialog 自身的 doPrint 已触发浏览器打印，下面只负责记录链上事件
  printDialogOpen.value = false
  pendingPrintAction.value = null

  const loadingKey = kind === 'batch-print' ? 'batch:print' : `${kind}:${codes[0]?.traceCode}`
  actionLoadingKey.value = loadingKey
  let succeeded = 0
  try {
    if (kind === 'print') {
      await printTraceCode(codes[0].traceCode, { remark: '生产工作台打印标签' })
      toast.success('标签打印已记录')
    } else if (kind === 'reprint') {
      await reprintTraceCode(codes[0].traceCode, { remark: reason })
      toast.success('标签重打已记录')
    } else if (kind === 'batch-print') {
      for (const code of codes) {
        await printTraceCode(code.traceCode, { remark: '生产工作台批量打印标签' })
        succeeded += 1
      }
      toast.success(`批量打印完成：${succeeded}/${codes.length}`)
    }
    await refreshCurrentBatch({
      selectTraceCode: kind === 'batch-print' ? selectedTraceCode.value : codes[0]?.traceCode
    })
  } catch (error) {
    logger.error('打印链上事件记录失败', error)
    if (kind === 'batch-print') {
      toast.error(`批量打印中断：${succeeded}/${codes.length}，${error?.message || '请重试'}`)
      await refreshCurrentBatch()
    } else {
      toast.error(error?.message || '打印事件上链失败')
    }
  } finally {
    actionLoadingKey.value = ''
  }
}

async function handleBatchPrint() {
  const printableCodes = sortedBatchCodes.value.filter((code) => canPrint(code))
  if (printableCodes.length === 0) {
    toast.error('当前批次没有可打印的 GENERATED 单品码')
    return
  }
  const accepted = await confirm({
    title: '批量打印标签',
    message: `将弹出 ${printableCodes.length} 张标签的打印预览，确认后浏览器打印对话框会出现，同时为每张码记录 PRINT_CODE 事件。`,
    confirmText: '继续',
    cancelText: '取消'
  })
  if (!accepted) return

  openPrintDialog({
    kind: 'batch-print',
    codes: printableCodes,
    title: '批量打印标签',
    confirmText: `打印 ${printableCodes.length} 张`,
    mode: 'print'
  })
}

async function handleScanActivate(traceCode) {
  scannerOpen.value = false
  const normalizedCode = traceCode?.trim()
  if (!normalizedCode) return

  const code = sortedBatchCodes.value.find((item) => item.traceCode === normalizedCode)
  if (!code) {
    selectedTraceCode.value = normalizedCode
    toast.error('扫码结果不属于当前赋码批次')
    return
  }
  if (!canActivate(code)) {
    selectedTraceCode.value = normalizedCode
    toast.error(`当前状态 ${formatStatus(code.codeStatus)} 不可激活`)
    return
  }

  actionLoadingKey.value = `activate:${normalizedCode}`
  try {
    await activateTraceCode(normalizedCode, buildActivationPayload('生产工作台扫码激活'))
    toast.success('扫码激活成功')
    await refreshCurrentBatch({ selectTraceCode: normalizedCode })
  } catch (error) {
    logger.error('扫码激活失败', error)
    toast.error(error?.message || '扫码激活失败')
  } finally {
    actionLoadingKey.value = ''
  }
}

function buildActivationPayload(remark) {
  const payload = { eventTime: dayjs().format('YYYY-MM-DDTHH:mm:ss'), remark }
  if (createForm.manufacturerNode?.trim()) payload.activationNode = createForm.manufacturerNode.trim()
  if (activationDeviceId.value?.trim()) payload.deviceId = activationDeviceId.value.trim()
  return payload
}

async function promptReason(title, message) {
  const reason = await prompt({
    title,
    message,
    placeholder: '请输入原因（必填）',
    confirmText: '提交',
    cancelText: '取消',
    validator: (value) => (String(value || '').trim().length > 0 ? true : '必须填写原因')
  })
  return reason ? String(reason).trim() : null
}

function pickNumber(value, fallback) {
  return typeof value === 'number' ? value : fallback
}

function isActivatedStatus(status) {
  return ['ACTIVATED', 'IN_STOCK', 'IN_TRANSIT', 'EXCEPTION', 'TRANSFERRED'].includes(status)
}
function formatStatus(status) {
  return STATUS_TEXT[status] || status || '未知'
}

function statusTone(status) {
  if (status === 'VOIDED' || status === 'SCRAPPED' || status === 'TRANSFERRED') return 'mute'
  if (status === 'EXCEPTION') return 'error'
  if (status === 'IN_TRANSIT') return 'warn'
  if (isActivatedStatus(status)) return 'success'
  if (status === 'PRINTED') return 'primary'
  return 'mute'
}

function formatTime(value) {
  if (!value) return '-'
  const stamp = dayjs(value)
  if (!stamp.isValid()) return String(value)
  return stamp.format('MM-DD HH:mm')
}

function copyCodes() {
  const text = sortedBatchCodes.value.map((code) => code.traceCode).join('\n')
  if (!text) return
  navigator.clipboard?.writeText(text)
  toast.success('批次码列表已复制')
}

function goTraceDetail(traceCode) {
  if (!traceCode) return
  router.push(`/traces/${traceCode}`)
}
</script>

<template>
  <div class="assignment">
    <PageHeader
      title="生产赋码工作台"
      :subtitle="headerSubtitle"
      data-testid="assignment-page-header"
    >
      <template #actions>
        <BaseButton
          variant="secondary"
          :disabled="!hasBatch || sortedBatchCodes.length === 0"
          @click="copyCodes"
        >
          <template #icon><ClipboardCopy :size="13" /></template>
          复制码列表
        </BaseButton>
        <BaseButton
          variant="secondary"
          :disabled="!hasBatch"
          data-test="assignment-scan-activate"
          @click="scannerOpen = true"
        >
          <template #icon><ScanLine :size="13" /></template>
          扫码激活
        </BaseButton>
        <BaseButton
          variant="primary"
          :disabled="!hasBatch || actionLoadingKey === 'batch:print'"
          :loading="actionLoadingKey === 'batch:print'"
          data-test="assignment-batch-print"
          @click="handleBatchPrint"
        >
          <template #icon><Printer :size="13" /></template>
          批量打印
          <template #kbd><KbdShortcut keys="P" tone="inverse" /></template>
        </BaseButton>
      </template>
    </PageHeader>

    <section class="assignment__layout">

      <!-- 左栏：创建批次 form -->
      <form
        class="assignment__form"
        data-test="assignment-create-form"
        @submit.prevent="handleCreateBatch"
      >
        <div class="assignment__form-head">
          <div class="assignment__form-eyebrow">
            <Factory :size="13" />
            <span>STEP 1 · 创建批次</span>
          </div>
          <h2 class="assignment__form-title">创建赋码批次</h2>
          <p class="assignment__form-subtitle">
            一次最多生成 500 个单品码；批次落库后立即可打印 / 激活。
          </p>
        </div>

        <div v-if="createError" class="assignment__error" role="alert" data-testid="assignment-create-error">
          {{ createError }}
        </div>

        <div class="assignment__field">
          <label class="assignment__label" for="assignment-part">产品 / 配件 <span class="assignment__required">*</span></label>
          <select
            id="assignment-part"
            v-model="createForm.partCode"
            :disabled="loadingOptions"
            class="assignment__control"
            data-test="assignment-part-select"
          >
            <option value="">请选择配件</option>
            <option v-for="part in parts" :key="part.id || part.partCode" :value="part.partCode">
              {{ part.partCode }} · {{ part.partName }}
            </option>
          </select>
        </div>

        <div class="assignment__row">
          <div class="assignment__field">
            <label class="assignment__label" for="assignment-quantity">生产数量 <span class="assignment__required">*</span></label>
            <input
              id="assignment-quantity"
              v-model="createForm.quantity"
              type="number"
              inputmode="numeric"
              placeholder="1 – 500"
              class="assignment__control assignment__control--mono"
              data-test="assignment-quantity-input"
            />
          </div>
          <div class="assignment__field">
            <label class="assignment__label" for="assignment-order">生产工单号</label>
            <input
              id="assignment-order"
              v-model="createForm.productionOrderNo"
              type="text"
              maxlength="64"
              placeholder="PO-20260507-001"
              class="assignment__control"
            />
          </div>
        </div>

        <div class="assignment__field">
          <label class="assignment__label" for="assignment-batchno">批次号（可选）</label>
          <input
            id="assignment-batchno"
            v-model="createForm.batchNo"
            type="text"
            maxlength="64"
            placeholder="留空则后端自动生成"
            class="assignment__control"
          />
        </div>

        <div class="assignment__field">
          <label class="assignment__label" for="assignment-node">结构化生产节点</label>
          <select
            id="assignment-node"
            v-model="createForm.manufacturerNodeId"
            :disabled="loadingOptions"
            class="assignment__control"
            data-test="assignment-node-select"
            @change="handleNodeChange"
          >
            <option value="">不选择，手填节点</option>
            <option v-for="node in nodes" :key="node.id" :value="node.id">
              {{ node.nodeCode }} · {{ node.nodeName }}
            </option>
          </select>
        </div>

        <div class="assignment__field">
          <label class="assignment__label" for="assignment-node-text">生产节点 <span class="assignment__required">*</span></label>
          <input
            id="assignment-node-text"
            v-model="createForm.manufacturerNode"
            type="text"
            placeholder="北京工厂 / BJ_FACTORY"
            class="assignment__control"
            data-test="assignment-node-input"
          />
        </div>

        <div class="assignment__row">
          <div class="assignment__field">
            <label class="assignment__label" for="assignment-province">省份 <span class="assignment__required">*</span></label>
            <input
              id="assignment-province"
              v-model="createForm.province"
              type="text"
              placeholder="北京"
              class="assignment__control"
            />
          </div>
          <div class="assignment__field">
            <label class="assignment__label" for="assignment-city">城市 <span class="assignment__required">*</span></label>
            <input
              id="assignment-city"
              v-model="createForm.city"
              type="text"
              placeholder="北京市"
              class="assignment__control"
            />
          </div>
        </div>

        <BaseButton
          type="submit"
          variant="primary"
          size="md"
          block
          :loading="creating"
          :disabled="creating"
          data-test="assignment-create-submit"
        >
          <template #icon><QrCode :size="14" /></template>
          {{ creating ? '正在创建批次…' : '创建批次并生成单品码' }}
        </BaseButton>
      </form>

      <!-- 右栏：批次详情 + 码列表 -->
      <div class="assignment__main">

        <!-- 批次查询条 -->
        <section class="assignment__lookup-card">
          <div class="assignment__lookup-head">
            <div class="assignment__form-eyebrow">
              <FileSearch :size="13" />
              <span>STEP 2 · 数量对账</span>
            </div>
            <span v-if="hasBatch" class="assignment__batch-pill mono">
              {{ batchPillLabel }}
            </span>
          </div>
          <div class="assignment__lookup-row">
            <input
              id="assignment-lookup"
              v-model="lookupBatchId"
              type="number"
              inputmode="numeric"
              placeholder="输入批次 ID 查询历史批次"
              class="assignment__control assignment__control--mono assignment__lookup-input"
              data-test="assignment-lookup-input"
              @keydown.enter="handleLookupBatch"
            />
            <BaseButton
              variant="secondary"
              :loading="loadingBatch"
              :disabled="loadingBatch"
              data-test="assignment-lookup-submit"
              @click="handleLookupBatch"
            >
              <template #icon><FileSearch :size="13" /></template>
              查询
            </BaseButton>
            <BaseButton
              v-if="hasBatch"
              variant="text"
              :loading="loadingBatch"
              :disabled="loadingBatch"
              data-test="assignment-refresh"
              @click="refreshCurrentBatch()"
            >
              <template #icon><RefreshCw :size="13" /></template>
              刷新
            </BaseButton>
          </div>
          <div class="assignment__lookup-row">
            <input
              id="assignment-lookup-trace-code"
              v-model="lookupTraceCode"
              type="text"
              maxlength="64"
              placeholder="或粘贴追溯码反查（v11 历史码也支持）"
              class="assignment__control assignment__control--mono assignment__lookup-input"
              data-test="assignment-lookup-trace-code-input"
              @keydown.enter="handleLookupTraceCode"
            />
            <BaseButton
              variant="secondary"
              :loading="loadingLookupTraceCode"
              :disabled="loadingLookupTraceCode"
              data-test="assignment-lookup-trace-code-submit"
              @click="handleLookupTraceCode"
            >
              <template #icon><QrCode :size="13" /></template>
              按追溯码查询
            </BaseButton>
          </div>
        </section>

        <!-- 对账卡 -->
        <section class="assignment__recon">
          <div v-if="!hasBatch" class="assignment__recon-empty" data-test="assignment-recon-empty">
            <EmptyState
              :icon="PackageCheck"
              title="尚未选定批次"
              subtitle="创建一个新批次或查询历史批次后，这里会展示计划 / 生成 / 打印 / 激活 / 入库 / 作废 6 项数量对账。"
            />
          </div>
          <template v-else>
            <div class="assignment__recon-grid">
              <article
                v-for="card in reconciliationCards"
                :key="card.key"
                class="assignment__recon-card"
                :data-testid="`assignment-recon-card-${card.key}`"
              >
                <span class="assignment__recon-label">{{ card.label }}</span>
                <span
                  class="assignment__recon-value mono"
                  :class="{ 'assignment__recon-value--danger': card.tone === 'danger' && card.value > 0 }"
                >
                  {{ card.value }}
                </span>
                <span class="assignment__recon-hint">{{ card.hint }}</span>
              </article>
            </div>
            <div
              class="assignment__recon-status"
              :class="`assignment__recon-status--${reconciliationTone}`"
              data-test="assignment-reconciliation-status"
            >
              <StatusPill :tone="reconciliationTone">
                对账状态：{{ reconciliationStatusLabel }}
              </StatusPill>
              <p v-if="reconciliationConsistent" class="assignment__recon-note">
                当前批次数量闭环一致，可以放心继续打印 / 激活。
              </p>
              <ul v-else-if="discrepancyReasons.length" class="assignment__recon-list">
                <li v-for="reason in discrepancyReasons" :key="reason">{{ reason }}</li>
              </ul>
              <p v-else class="assignment__recon-note">
                数量尚未闭环，按提示修正后再次刷新对账。
              </p>
            </div>
          </template>
        </section>

        <!-- 码列表 -->
        <section class="assignment__codes">
          <header class="assignment__codes-head">
            <div>
              <div class="assignment__form-eyebrow">
                <QrCode :size="13" />
                <span>STEP 3 · 单品码操作</span>
              </div>
              <h2 class="assignment__form-title">单品码列表</h2>
            </div>
            <div class="assignment__codes-tools">
              <BaseInput
                v-model="activationDeviceId"
                size="sm"
                placeholder="激活设备号（可选）"
                input-id="assignment-device"
              />
            </div>
          </header>

          <div v-if="loadingBatch && !sortedBatchCodes.length" class="assignment__codes-loading">
            <LoadingSkeleton type="table" :rows="4" />
          </div>

          <div v-else-if="!hasCodesToShow" class="assignment__codes-empty">
            <EmptyState
              :icon="QrCode"
              title="暂无批次码列表"
              subtitle="先创建批次或查询历史批次以加载单品码。"
            />
          </div>

          <div v-else-if="!sortedBatchCodes.length" class="assignment__codes-empty">
            <EmptyState
              :icon="QrCode"
              title="该批次尚未生成单品码"
              subtitle="后端可能仍在分片提交，稍后点击「刷新」按钮再试。"
            />
          </div>

          <div v-else class="assignment__codes-body">
            <aside
              v-if="activeCode"
              class="assignment__active assignment__active--strip"
              data-test="assignment-active-code-panel"
            >
              <div class="assignment__active-strip-head">
                <span class="assignment__active-eyebrow">当前选中</span>
                <TraceCodeChip :code="activeCode.traceCode" size="md" />
              </div>
              <dl class="assignment__active-strip-list">
                <div class="assignment__active-strip-item">
                  <dt>批内序号</dt>
                  <dd class="mono">{{ activeCode.serialNo || '-' }}</dd>
                </div>
                <div class="assignment__active-strip-item">
                  <dt>码状态</dt>
                  <dd>
                    <StatusPill :tone="statusTone(activeCode.codeStatus)">{{ formatStatus(activeCode.codeStatus) }}</StatusPill>
                  </dd>
                </div>
                <div class="assignment__active-strip-item">
                  <dt>打印</dt>
                  <dd class="mono">{{ activeCode.printCount || 0 }} 次</dd>
                </div>
                <div class="assignment__active-strip-item">
                  <dt>激活人</dt>
                  <dd>{{ activeCode.activatedByUsername || '-' }}</dd>
                </div>
                <div class="assignment__active-strip-item">
                  <dt>激活时间</dt>
                  <dd class="mono">{{ activeCode.activatedTime || '-' }}</dd>
                </div>
              </dl>
              <BaseButton
                variant="secondary"
                size="sm"
                data-test="assignment-active-go-detail"
                @click="goTraceDetail(activeCode.traceCode)"
              >
                <template #icon><ExternalLink :size="13" /></template>
                查看溯源详情
              </BaseButton>
            </aside>

            <div class="assignment__table-wrap" data-test="assignment-code-list">
              <table class="assignment__table">
                <thead>
                  <tr>
                    <th class="assignment__col-no">#</th>
                    <th class="assignment__col-code">追溯码</th>
                    <th class="assignment__col-status">状态</th>
                    <th class="assignment__col-print">打印</th>
                    <th class="assignment__col-time">激活时间</th>
                    <th class="assignment__col-actions">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="code in sortedBatchCodes"
                    :key="code.traceCode"
                    :class="['assignment__code-row', code.traceCode === selectedTraceCode && 'assignment__code-row--active']"
                    :data-test="`assignment-code-row-${code.traceCode}`"
                    @click="selectCode(code.traceCode)"
                  >
                    <td class="mono assignment__cell-no">{{ code.serialNo || '-' }}</td>
                    <td class="assignment__cell-code">
                      <TraceCodeChip :code="code.traceCode" size="md" :copyable="false" truncate />
                    </td>
                    <td>
                      <StatusPill :tone="statusTone(code.codeStatus)">
                        {{ formatStatus(code.codeStatus) }}
                      </StatusPill>
                    </td>
                    <td class="mono assignment__cell-print">{{ code.printCount || 0 }} 次</td>
                    <td class="mono assignment__cell-time">{{ formatTime(code.activatedTime) }}</td>
                    <td class="assignment__cell-actions">
                      <div class="assignment__cell-actions-row">
                        <button
                          type="button"
                          class="assignment__action assignment__action--primary"
                          :disabled="!canPrint(code) || actionLoadingKey === `print:${code.traceCode}`"
                          :data-testid="`assignment-action-print-${code.traceCode}`"
                          @click.stop="runCodeAction(code, 'print')"
                        >打印</button>
                        <button
                          type="button"
                          class="assignment__action"
                          :disabled="!canReprint(code) || actionLoadingKey === `reprint:${code.traceCode}`"
                          :data-testid="`assignment-action-reprint-${code.traceCode}`"
                          @click.stop="runCodeAction(code, 'reprint')"
                        >重打</button>
                        <button
                          type="button"
                          class="assignment__action assignment__action--success"
                          :disabled="!canActivate(code) || actionLoadingKey === `activate:${code.traceCode}`"
                          :data-testid="`assignment-action-activate-${code.traceCode}`"
                          @click.stop="runCodeAction(code, 'activate')"
                        >激活</button>
                        <button
                          type="button"
                          class="assignment__action assignment__action--danger"
                          :disabled="!canVoid(code) || actionLoadingKey === `void:${code.traceCode}`"
                          :data-testid="`assignment-action-void-${code.traceCode}`"
                          @click.stop="runCodeAction(code, 'void')"
                        >作废</button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <p v-if="!activeCode" class="assignment__active-hint">请在表格中选择一个单品码，查看详情和最近一次激活状态。</p>
          </div>
        </section>
      </div>
    </section>

    <QRScanner v-if="scannerOpen" @scan="handleScanActivate" @close="scannerOpen = false" />

    <PrintLabelDialog
      v-model="printDialogOpen"
      :codes="printDialogCodes"
      :batch="batchDetail"
      :part-label="partLabel"
      :title="printDialogTitle"
      :confirm-text="printDialogConfirmText"
      :mode="printDialogMode"
      data-test="assignment-print-dialog"
      @confirm="handlePrintDialogConfirm"
      @cancel="closePrintDialog"
    />
  </div>
</template>

<style scoped>
.assignment {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 12px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.assignment__layout {
  display: grid;
  grid-template-columns: 380px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.assignment__form,
.assignment__lookup-card,
.assignment__recon,
.assignment__codes {
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.assignment__form {
  position: sticky;
  top: 68px;
}

.assignment__main {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.assignment__form-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--hairline);
}
.assignment__form-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.4px;
  color: var(--ink-tertiary);
  text-transform: uppercase;
}
.assignment__form-eyebrow svg {
  color: var(--primary);
}
.assignment__form-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.3px;
  color: var(--ink);
}
.assignment__form-subtitle {
  margin: 0;
  font-size: 12.5px;
  color: var(--ink-subtle);
  line-height: 1.5;
}

.assignment__error {
  background: var(--error-soft);
  color: var(--error);
  border: 1px solid #f8c8ca;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 12.5px;
  line-height: 1.45;
}

.assignment__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.assignment__row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.assignment__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
}
.assignment__required {
  color: var(--error);
  margin-left: 2px;
}
.assignment__control {
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
.assignment__control--mono {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
}
.assignment__control:focus {
  border-color: var(--primary-focus, #5e69d1);
  box-shadow: 0 0 0 3px rgba(94, 106, 210, 0.15);
}
.assignment__control:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
select.assignment__control {
  appearance: none;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10' viewBox='0 0 24 24' fill='none' stroke='%2371717a' stroke-width='2'><path d='M6 9l6 6 6-6'/></svg>");
  background-repeat: no-repeat;
  background-position: right 10px center;
  padding-right: 28px;
}

.assignment__lookup-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.assignment__lookup-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}
.assignment__lookup-row :deep(.base-input) {
  flex: 1 1 200px;
  min-width: 180px;
}
.assignment__batch-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 9999px;
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 12px;
  font-weight: 500;
}

.assignment__recon-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}
.assignment__recon-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  border: 1px solid var(--hairline);
  border-radius: 8px;
  background: var(--surface-1);
}
.assignment__recon-label {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.4px;
  color: var(--ink-tertiary);
  text-transform: uppercase;
}
.assignment__recon-value {
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -0.4px;
  color: var(--ink);
  line-height: 1.1;
}
.assignment__recon-value--danger {
  color: var(--error);
}
.assignment__recon-hint {
  font-size: 11.5px;
  color: var(--ink-subtle);
  line-height: 1.35;
}
.assignment__recon-status {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 8px;
  border: 1px solid var(--hairline);
}
.assignment__recon-status--success {
  background: var(--success-soft);
  border-color: #bef0c7;
}
.assignment__recon-status--warn {
  background: var(--warn-soft);
  border-color: #fcd9b6;
}
.assignment__recon-status--mute {
  background: var(--surface-2);
}
.assignment__recon-note {
  margin: 0;
  font-size: 12.5px;
  color: var(--ink-muted);
  line-height: 1.5;
}
.assignment__recon-list {
  margin: 0;
  padding-left: 18px;
  font-size: 12.5px;
  color: var(--ink-muted);
  line-height: 1.55;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.assignment__recon-empty {
  padding: 4px 0;
}

.assignment__codes-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--hairline);
}
.assignment__codes-tools {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  min-width: 220px;
}
.assignment__codes-tools :deep(.base-input) {
  width: 220px;
}
.assignment__codes-loading,
.assignment__codes-empty {
  padding: 4px 0;
}

.assignment__codes-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.assignment__active--strip {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  padding: 10px 14px;
  background: var(--primary-soft);
  border: 1px solid color-mix(in srgb, var(--primary) 18%, transparent);
}
.assignment__active-strip-head {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.assignment__active-strip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 18px;
  margin: 0;
  min-width: 0;
}
.assignment__active-strip-item {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  font-size: 12px;
  color: var(--ink-muted);
  min-width: 0;
}
.assignment__active-strip-item dt {
  color: var(--ink-subtle);
  flex-shrink: 0;
}
.assignment__active-strip-item dd {
  margin: 0;
  color: var(--ink);
  font-weight: 500;
}

.assignment__table-wrap {
  overflow-x: auto;
  border: 1px solid var(--hairline);
  border-radius: 8px;
  background: var(--surface-1);
}
.assignment__table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  table-layout: fixed;
  min-width: 720px;
}
.assignment__table .assignment__col-no { width: 48px; }
.assignment__table .assignment__col-code { width: 230px; }
.assignment__table .assignment__col-status { width: 96px; }
.assignment__table .assignment__col-print { width: 76px; }
.assignment__table .assignment__col-time { width: 120px; }
.assignment__table .assignment__col-actions { width: auto; }
.assignment__table th {
  text-align: left;
  font-weight: 500;
  font-size: 11.5px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--ink-subtle);
  padding: 10px 12px;
  border-bottom: 1px solid var(--hairline);
  background: var(--surface-2);
}
.assignment__table td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--hairline);
  color: var(--ink);
  vertical-align: middle;
}
.assignment__code-row {
  cursor: pointer;
  transition: background 0.12s;
}
.assignment__code-row:hover td {
  background: var(--surface-2);
}
.assignment__code-row--active td {
  background: var(--primary-soft);
}
.assignment__code-row:last-child td {
  border-bottom: 0;
}
.assignment__cell-no {
  color: var(--ink-tertiary);
  width: 36px;
}
.assignment__cell-print,
.assignment__cell-time {
  color: var(--ink-muted);
  font-size: 12px;
  white-space: nowrap;
}
.assignment__cell-actions {
  text-align: right;
  vertical-align: middle;
  word-break: normal;
  white-space: normal;
}
.assignment__cell-actions-row {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: flex-end;
}
.assignment__cell-code {
  word-break: break-all;
  vertical-align: middle;
}

.assignment__action {
  height: 26px;
  padding: 0 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid var(--hairline);
  background: var(--surface-1);
  color: var(--ink-muted);
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
  white-space: nowrap;
  flex: 0 0 auto;
}
.assignment__action:hover:not(:disabled) {
  border-color: var(--ink-subtle);
  color: var(--ink);
}
.assignment__action:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.assignment__action--primary:not(:disabled) {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
}
.assignment__action--primary:hover:not(:disabled) {
  background: var(--primary-hover);
  border-color: var(--primary-hover);
  color: #fff;
}
.assignment__action--success:not(:disabled) {
  background: var(--success);
  color: #fff;
  border-color: var(--success);
}
.assignment__action--success:hover:not(:disabled) {
  filter: brightness(1.05);
}
.assignment__action--danger {
  color: var(--error);
  border-color: #f8c8ca;
}
.assignment__action--danger:hover:not(:disabled) {
  background: var(--error-soft);
  color: var(--error);
}

.assignment__active {
  border: 1px solid var(--hairline);
  border-radius: 8px;
  padding: 16px;
  background: var(--surface-1);
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.assignment__active-eyebrow {
  margin: 0;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.4px;
  color: var(--ink-tertiary);
  text-transform: uppercase;
}
.assignment__active-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 0;
}
.assignment__active-row {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  font-size: 12.5px;
  color: var(--ink-muted);
}
.assignment__active-row dt {
  color: var(--ink-subtle);
}
.assignment__active-row dd {
  margin: 0;
  color: var(--ink);
  word-break: break-all;
}
.assignment__active-hint {
  margin: 0;
  font-size: 12.5px;
  color: var(--ink-subtle);
  line-height: 1.5;
}

.mono {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
}

@media (max-width: 1023.98px) {
  .assignment__layout {
    grid-template-columns: minmax(0, 1fr);
  }
  .assignment__form {
    position: static;
  }
  .assignment__recon-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .assignment__active--strip {
    grid-template-columns: minmax(0, 1fr);
    gap: 8px;
  }
}

@media (max-width: 639.98px) {
  .assignment {
    padding: 16px 8px;
  }
  .assignment__form,
  .assignment__lookup-card,
  .assignment__recon,
  .assignment__codes {
    padding: 16px;
    border-radius: 10px;
  }
  .assignment__row {
    grid-template-columns: minmax(0, 1fr);
  }
  .assignment__recon-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .assignment__cell-actions {
    justify-content: flex-start;
  }
}
</style>
