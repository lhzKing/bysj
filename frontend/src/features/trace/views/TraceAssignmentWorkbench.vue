
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import {
  Activity,
  AlertTriangle,
  CheckCircle2,
  ClipboardList,
  ExternalLink,
  Factory,
  FileText,
  PackageCheck,
  Printer,
  QrCode,
  RefreshCw,
  ScanLine,
  Search,
  ShieldX,
  XCircle
} from 'lucide-vue-next'
import QRScanner from '@/shared/components/QRScanner.vue'
import { useConfirm } from '@/shared/composables/useConfirm'
import { useToast } from '@/shared/composables/useToast'
import { getParts } from '@/features/part/api'
import {
  activateTraceCode,
  createTrace,
  getTraceBatch,
  getTraceBatchCodes,
  getTraceNodes,
  printTraceCode,
  reprintTraceCode,
  voidTraceCode
} from '@/features/trace/api'
import { logger } from '@/shared/utils/logger'

const router = useRouter()
const toast = useToast()
const { confirm } = useConfirm()

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
const activationDeviceId = ref('')

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

const statusTextMap = {
  GENERATED: '已生成',
  PRINTED: '已打印',
  ACTIVATED: '已激活',
  IN_STOCK: '已入库',
  IN_TRANSIT: '运输中',
  EXCEPTION: '异常',
  VOIDED: '已作废',
  SCRAPPED: '已报废'
}

const hasBatch = computed(() => Boolean(batchDetail.value?.batchId))
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
const printedCount = computed(() => sortedBatchCodes.value.filter((code) => Number(code.printCount || 0) > 0).length)
const activatedCount = computed(() => sortedBatchCodes.value.filter((code) => isActivatedStatus(code.codeStatus)).length)
const voidedCount = computed(() => sortedBatchCodes.value.filter((code) => code.codeStatus === 'VOIDED').length)
const selectedNode = computed(() =>
  nodes.value.find((node) => String(node.id) === String(createForm.manufacturerNodeId)) || null
)
const selectedProgressText = computed(() => {
  if (!hasBatch.value) return '尚未创建批次'
  const total = typeof batchDetail.value?.quantityRequested === 'number' ? batchDetail.value.quantityRequested : sortedBatchCodes.value.length
  return `${activatedCount.value}/${total} 已激活`
})

const hasNumber = (value) => typeof value === 'number'
const reconciliationCards = computed(() => [
  { key: 'requested', label: '计划数量', value: hasNumber(batchDetail.value?.quantityRequested) ? batchDetail.value.quantityRequested : Number(createForm.quantity || 0), hint: '本次赋码批次目标', icon: ClipboardList, tone: 'slate' },
  { key: 'generated', label: '生成数量', value: hasNumber(batchDetail.value?.quantityGenerated) ? batchDetail.value.quantityGenerated : generatedCount.value, hint: '已落库单品码', icon: QrCode, tone: 'indigo' },
  { key: 'printed', label: '打印数量', value: hasNumber(batchDetail.value?.quantityPrinted) ? batchDetail.value.quantityPrinted : printedCount.value, hint: `${hasNumber(batchDetail.value?.printOperationCount) ? batchDetail.value.printOperationCount : 0} 次打印操作`, icon: Printer, tone: 'cyan' },
  { key: 'activated', label: '激活数量', value: hasNumber(batchDetail.value?.quantityActivated) ? batchDetail.value.quantityActivated : activatedCount.value, hint: '贴码后扫码复核', icon: PackageCheck, tone: 'emerald' },
  { key: 'inbound', label: '入库数量', value: hasNumber(batchDetail.value?.quantityInbound) ? batchDetail.value.quantityInbound : 0, hint: '进入库存的实物', icon: CheckCircle2, tone: 'blue' },
  { key: 'voided', label: '作废数量', value: hasNumber(batchDetail.value?.quantityVoided) ? batchDetail.value.quantityVoided : voidedCount.value, hint: '未使用/损坏标签', icon: ShieldX, tone: 'rose' }
])

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
  if (!createForm.partCode) return '请选择产品/配件'
  if (!createForm.quantity || Number(createForm.quantity) < 1 || Number(createForm.quantity) > 500) {
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

async function runCodeAction(code, action) {
  if (!code?.traceCode) return
  const key = `${action}:${code.traceCode}`
  actionLoadingKey.value = key
  try {
    if (action === 'print') {
      await printTraceCode(code.traceCode, { remark: '生产工作台打印标签' })
      toast.success('标签打印已记录')
    } else if (action === 'reprint') {
      const reason = promptReason('重打标签', `请填写 ${code.traceCode} 的重打原因`)
      if (!reason) return
      await reprintTraceCode(code.traceCode, { remark: reason })
      toast.success('标签重打已记录')
    } else if (action === 'void') {
      const accepted = await confirm({ title: '作废标签', message: `确定作废 ${code.traceCode} 吗？作废后不能激活或流转。`, confirmText: '确认作废', cancelText: '取消', type: 'danger' })
      if (!accepted) return
      const reason = promptReason('作废标签', '请填写标签丢失、损坏或余码作废原因')
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
async function handleBatchPrint() {
  const printableCodes = sortedBatchCodes.value.filter((code) => canPrint(code))
  if (printableCodes.length === 0) {
    toast.error('当前批次没有可打印的 GENERATED 单品码')
    return
  }
  const accepted = await confirm({
    title: '批量打印标签',
    message: `将为 ${printableCodes.length} 个未打印单品码记录 PRINT_CODE 事件。`,
    confirmText: '开始打印',
    cancelText: '取消'
  })
  if (!accepted) return

  actionLoadingKey.value = 'batch:print'
  let succeeded = 0
  try {
    for (const code of printableCodes) {
      await printTraceCode(code.traceCode, { remark: '生产工作台批量打印标签' })
      succeeded += 1
    }
    toast.success(`批量打印完成：${succeeded}/${printableCodes.length}`)
    await refreshCurrentBatch()
  } catch (error) {
    logger.error('批量打印失败', error)
    toast.error(`批量打印中断：${succeeded}/${printableCodes.length}，${error?.message || '请重试'}`)
    await refreshCurrentBatch()
  } finally {
    actionLoadingKey.value = ''
  }
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

function promptReason(title, message) {
  const reason = window.prompt(`${title}\n${message || ''}`, '')
  const normalizedReason = reason?.trim() || ''
  if (!normalizedReason) {
    toast.error('必须填写原因')
    return null
  }
  return normalizedReason
}

function canPrint(code) {
  return code?.codeStatus === 'GENERATED'
}
function canReprint(code) {
  return code && code.codeStatus !== 'VOIDED' && code.codeStatus !== 'SCRAPPED'
}
function canVoid(code) {
  return code && ['GENERATED', 'PRINTED'].includes(code.codeStatus)
}
function canActivate(code) {
  return code && ['GENERATED', 'PRINTED'].includes(code.codeStatus)
}
function isActivatedStatus(status) {
  return ['ACTIVATED', 'IN_STOCK', 'IN_TRANSIT', 'EXCEPTION'].includes(status)
}
function formatStatus(status) {
  return statusTextMap[status] || status || '未知'
}
function rowClass(code) {
  if (code.traceCode === selectedTraceCode.value) return 'border-indigo-300 bg-indigo-50/80 shadow-indigo-100'
  if (code.codeStatus === 'VOIDED') return 'border-rose-100 bg-rose-50/70'
  if (isActivatedStatus(code.codeStatus)) return 'border-emerald-100 bg-emerald-50/60'
  if (Number(code.printCount || 0) > 0) return 'border-cyan-100 bg-cyan-50/60'
  return 'border-slate-200 bg-white/70'
}
function statusBadgeClass(status) {
  if (status === 'VOIDED') return 'bg-rose-100 text-rose-700 ring-rose-200'
  if (isActivatedStatus(status)) return 'bg-emerald-100 text-emerald-700 ring-emerald-200'
  if (status === 'PRINTED') return 'bg-cyan-100 text-cyan-700 ring-cyan-200'
  return 'bg-slate-100 text-slate-600 ring-slate-200'
}
function cardToneClass(tone) {
  const toneMap = {
    slate: 'from-slate-50 to-white text-slate-600',
    indigo: 'from-indigo-50 to-white text-indigo-600',
    cyan: 'from-cyan-50 to-white text-cyan-600',
    emerald: 'from-emerald-50 to-white text-emerald-600',
    blue: 'from-blue-50 to-white text-blue-600',
    rose: 'from-rose-50 to-white text-rose-600'
  }
  return toneMap[tone] || toneMap.slate
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
  <div class="relative z-10 mx-auto flex w-full max-w-7xl flex-col gap-8 px-4 py-8 sm:px-6 lg:px-8">
    <header class="premium-card overflow-hidden rounded-[40px] p-8 lg:p-10">
      <div class="flex flex-col gap-8 lg:flex-row lg:items-end lg:justify-between">
        <div class="max-w-3xl">
          <p class="mb-3 flex items-center gap-2 text-xs font-black uppercase tracking-[0.24em] text-indigo-500">
            <Factory class="h-4 w-4" /> Production Assignment Workbench
          </p>
          <h1 class="text-3xl font-black tracking-tight text-slate-950 sm:text-5xl">生产赋码工作台</h1>
          <p class="mt-4 text-base font-medium leading-8 text-slate-500">
            按赋码批次完成生成、打印、重打/作废、贴码激活和数量对账。生产人员只围绕批次和单品码操作，不需要理解底层生命周期日志。
          </p>
        </div>
        <div class="grid min-w-[260px] grid-cols-2 gap-3 rounded-[32px] bg-white/70 p-4 shadow-inner shadow-indigo-50">
          <div>
            <p class="text-[10px] font-black uppercase tracking-widest text-slate-400">当前批次</p>
            <p class="mt-1 font-mono text-sm font-black text-slate-900">{{ batchDetail?.batchNo || '未创建' }}</p>
          </div>
          <div>
            <p class="text-[10px] font-black uppercase tracking-widest text-slate-400">进度</p>
            <p class="mt-1 text-sm font-black text-indigo-600">{{ selectedProgressText }}</p>
          </div>
        </div>
      </div>
    </header>

    <section class="grid gap-6 lg:grid-cols-[420px_minmax(0,1fr)]">
      <form class="premium-card rounded-[36px] p-6 lg:p-8" data-test="assignment-create-form" @submit.prevent="handleCreateBatch">
        <div class="mb-6 flex items-start justify-between gap-4">
          <div>
            <p class="text-xs font-black uppercase tracking-[0.2em] text-slate-400">Step 1</p>
            <h2 class="mt-2 text-2xl font-black text-slate-900">创建赋码批次</h2>
          </div>
          <div class="rounded-2xl bg-indigo-50 p-3 text-indigo-600"><FileText class="h-6 w-6" /></div>
        </div>
        <div v-if="createError" class="mb-5 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-bold text-rose-700">{{ createError }}</div>

        <div class="space-y-5">
          <label class="block">
            <span class="mb-2 block text-sm font-bold text-slate-700">产品/配件 <span class="text-rose-500">*</span></span>
            <select v-model="createForm.partCode" :disabled="loadingOptions" class="h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm font-bold text-slate-700 outline-none transition focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 disabled:opacity-60" data-test="assignment-part-select">
              <option value="">请选择配件</option>
              <option v-for="part in parts" :key="part.id || part.partCode" :value="part.partCode">{{ part.partCode }} - {{ part.partName }}</option>
            </select>
          </label>
          <div class="grid gap-4 sm:grid-cols-2">
            <label class="block">
              <span class="mb-2 block text-sm font-bold text-slate-700">生产数量 <span class="text-rose-500">*</span></span>
              <input v-model="createForm.quantity" type="number" min="1" max="500" class="h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm font-bold text-slate-700 outline-none transition focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100" data-test="assignment-quantity-input" />
            </label>
            <label class="block">
              <span class="mb-2 block text-sm font-bold text-slate-700">生产工单号</span>
              <input v-model="createForm.productionOrderNo" type="text" maxlength="64" placeholder="PO-20260507-001" class="h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm font-bold text-slate-700 outline-none transition focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100" />
            </label>
          </div>
          <label class="block">
            <span class="mb-2 block text-sm font-bold text-slate-700">批次号（可选）</span>
            <input v-model="createForm.batchNo" type="text" maxlength="64" placeholder="留空则后端自动生成" class="h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm font-bold text-slate-700 outline-none transition focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100" />
          </label>
          <label class="block">
            <span class="mb-2 block text-sm font-bold text-slate-700">结构化生产节点</span>
            <select v-model="createForm.manufacturerNodeId" :disabled="loadingOptions" class="h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm font-bold text-slate-700 outline-none transition focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 disabled:opacity-60" data-test="assignment-node-select" @change="handleNodeChange">
              <option value="">不选择，手填节点</option>
              <option v-for="node in nodes" :key="node.id" :value="node.id">{{ node.nodeCode }} - {{ node.nodeName }}</option>
            </select>
          </label>
          <label class="block">
            <span class="mb-2 block text-sm font-bold text-slate-700">生产节点 <span class="text-rose-500">*</span></span>
            <input v-model="createForm.manufacturerNode" type="text" placeholder="北京工厂 / BJ_FACTORY" class="h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm font-bold text-slate-700 outline-none transition focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100" data-test="assignment-node-input" />
          </label>
          <div class="grid gap-4 sm:grid-cols-2">
            <label class="block">
              <span class="mb-2 block text-sm font-bold text-slate-700">省份 <span class="text-rose-500">*</span></span>
              <input v-model="createForm.province" type="text" placeholder="北京" class="h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm font-bold text-slate-700 outline-none transition focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100" />
            </label>
            <label class="block">
              <span class="mb-2 block text-sm font-bold text-slate-700">城市 <span class="text-rose-500">*</span></span>
              <input v-model="createForm.city" type="text" placeholder="北京市" class="h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm font-bold text-slate-700 outline-none transition focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100" />
            </label>
          </div>
        </div>
        <button type="submit" :disabled="creating" class="mt-8 flex min-h-[52px] w-full items-center justify-center gap-3 rounded-2xl bg-indigo-600 px-5 py-4 text-sm font-black text-white shadow-xl shadow-indigo-200 transition hover:bg-indigo-700 active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-60" data-test="assignment-create-submit">
          <RefreshCw v-if="creating" class="h-5 w-5 animate-spin" /><QrCode v-else class="h-5 w-5" />{{ creating ? '正在创建批次...' : '创建批次并生成单品码' }}
        </button>
      </form>
      <div class="flex flex-col gap-6">
        <section class="premium-card rounded-[36px] p-6 lg:p-8">
          <div class="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p class="text-xs font-black uppercase tracking-[0.2em] text-slate-400">Step 2</p>
              <h2 class="mt-2 text-2xl font-black text-slate-900">数量对账</h2>
            </div>
            <div class="flex gap-2">
              <input v-model="lookupBatchId" type="number" min="1" placeholder="批次 ID" class="h-11 w-32 rounded-2xl border border-slate-200 bg-white px-4 text-sm font-bold text-slate-700 outline-none transition focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100" data-test="assignment-lookup-input" @keyup.enter="handleLookupBatch" />
              <button type="button" class="flex min-h-[44px] items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 text-sm font-black text-slate-600 transition hover:border-indigo-300 hover:text-indigo-600 disabled:opacity-60" :disabled="loadingBatch" data-test="assignment-lookup-submit" @click="handleLookupBatch"><Search class="h-4 w-4" /> 查询</button>
            </div>
          </div>

          <div v-if="!hasBatch" class="rounded-[28px] border border-dashed border-slate-200 bg-white/50 p-8 text-center">
            <Activity class="mx-auto h-10 w-10 text-slate-300" />
            <p class="mt-3 text-sm font-bold text-slate-500">创建或查询一个赋码批次后，这里会展示计划、生成、打印、激活、入库和作废数量。</p>
          </div>
          <div v-else class="space-y-5">
            <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              <div v-for="card in reconciliationCards" :key="card.key" class="rounded-[28px] border border-white/80 bg-gradient-to-br p-5 shadow-sm" :class="cardToneClass(card.tone)">
                <div class="mb-5 flex items-center justify-between"><span class="text-xs font-black uppercase tracking-widest opacity-70">{{ card.label }}</span><component :is="card.icon" class="h-5 w-5" /></div>
                <p class="text-4xl font-black tracking-tight text-slate-950">{{ card.value }}</p>
                <p class="mt-2 text-xs font-bold text-slate-400">{{ card.hint }}</p>
              </div>
            </div>
            <div class="rounded-[28px] border p-5" :class="batchDetail?.consistent ? 'border-emerald-200 bg-emerald-50/70' : 'border-amber-200 bg-amber-50/70'" data-test="assignment-reconciliation-status">
              <div class="flex items-start gap-4">
                <CheckCircle2 v-if="batchDetail?.consistent" class="mt-0.5 h-6 w-6 flex-shrink-0 text-emerald-600" />
                <AlertTriangle v-else class="mt-0.5 h-6 w-6 flex-shrink-0 text-amber-600" />
                <div>
                  <p class="font-black text-slate-900">对账状态：{{ batchDetail?.reconciliationStatus || '-' }}</p>
                  <p v-if="batchDetail?.consistent" class="mt-1 text-sm font-medium text-emerald-700">当前批次数量闭环一致。</p>
                  <ul v-else class="mt-2 list-disc space-y-1 pl-5 text-sm font-medium text-amber-800">
                    <li v-for="reason in batchDetail?.discrepancyReasons || []" :key="reason">{{ reason }}</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section class="premium-card rounded-[36px] p-6 lg:p-8">
          <div class="mb-6 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p class="text-xs font-black uppercase tracking-[0.2em] text-slate-400">Step 3</p>
              <h2 class="mt-2 text-2xl font-black text-slate-900">打印 / 激活 / 作废</h2>
            </div>
            <div class="flex flex-wrap gap-2">
              <button type="button" :disabled="!hasBatch || actionLoadingKey === 'batch:print'" class="flex min-h-[44px] items-center gap-2 rounded-2xl bg-cyan-600 px-4 py-3 text-sm font-black text-white shadow-lg shadow-cyan-100 transition hover:bg-cyan-700 disabled:cursor-not-allowed disabled:opacity-50" data-test="assignment-batch-print" @click="handleBatchPrint"><RefreshCw v-if="actionLoadingKey === 'batch:print'" class="h-4 w-4 animate-spin" /><Printer v-else class="h-4 w-4" />批量打印未打印码</button>
              <button type="button" :disabled="!hasBatch" class="flex min-h-[44px] items-center gap-2 rounded-2xl bg-emerald-600 px-4 py-3 text-sm font-black text-white shadow-lg shadow-emerald-100 transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-50" data-test="assignment-scan-activate" @click="scannerOpen = true"><ScanLine class="h-4 w-4" />扫码激活</button>
              <button type="button" :disabled="!hasBatch || sortedBatchCodes.length === 0" class="flex min-h-[44px] items-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm font-black text-slate-600 transition hover:border-indigo-300 hover:text-indigo-600 disabled:cursor-not-allowed disabled:opacity-50" @click="copyCodes"><ClipboardList class="h-4 w-4" />复制码列表</button>
            </div>
          </div>

          <label class="mb-5 block max-w-sm">
            <span class="mb-2 block text-sm font-bold text-slate-700">激活设备号（可选）</span>
            <input v-model="activationDeviceId" type="text" maxlength="64" placeholder="PDA-01 / CAMERA-01" class="h-11 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm font-bold text-slate-700 outline-none transition focus:border-emerald-400 focus:ring-4 focus:ring-emerald-100" />
          </label>

          <div v-if="!hasBatch" class="rounded-[28px] border border-dashed border-slate-200 bg-white/50 p-8 text-center">
            <QrCode class="mx-auto h-10 w-10 text-slate-300" />
            <p class="mt-3 text-sm font-bold text-slate-500">暂无批次码列表。</p>
          </div>
          <div v-else class="grid gap-5 xl:grid-cols-[minmax(0,1fr)_340px]">
            <div class="max-h-[620px] space-y-3 overflow-y-auto pr-1" data-test="assignment-code-list">
              <article v-for="code in sortedBatchCodes" :key="code.traceCode" class="cursor-pointer rounded-[24px] border p-4 transition hover:-translate-y-0.5 hover:shadow-lg" :class="rowClass(code)" :data-test="`assignment-code-row-${code.traceCode}`" @click="selectCode(code.traceCode)">
                <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
                  <div class="min-w-0">
                    <div class="mb-2 flex flex-wrap items-center gap-2">
                      <span class="rounded-full bg-white/80 px-2.5 py-1 text-[10px] font-black uppercase tracking-widest text-slate-400">#{{ code.serialNo || '-' }}</span>
                      <span class="rounded-full px-2.5 py-1 text-[10px] font-black ring-1" :class="statusBadgeClass(code.codeStatus)">{{ formatStatus(code.codeStatus) }}</span>
                      <span class="rounded-full bg-white/80 px-2.5 py-1 text-[10px] font-black text-slate-500">打印 {{ code.printCount || 0 }} 次</span>
                    </div>
                    <p class="truncate font-mono text-sm font-black text-slate-900 sm:text-base">{{ code.traceCode }}</p>
                  </div>
                  <div class="flex flex-wrap gap-2">
                    <button type="button" :disabled="!canPrint(code) || actionLoadingKey === `print:${code.traceCode}`" class="min-h-[40px] rounded-xl bg-cyan-600 px-3 py-2 text-xs font-black text-white transition hover:bg-cyan-700 disabled:cursor-not-allowed disabled:opacity-40" @click.stop="runCodeAction(code, 'print')">打印</button>
                    <button type="button" :disabled="!canReprint(code) || actionLoadingKey === `reprint:${code.traceCode}`" class="min-h-[40px] rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-black text-slate-600 transition hover:border-indigo-300 hover:text-indigo-600 disabled:cursor-not-allowed disabled:opacity-40" @click.stop="runCodeAction(code, 'reprint')">重打</button>
                    <button type="button" :disabled="!canActivate(code) || actionLoadingKey === `activate:${code.traceCode}`" class="min-h-[40px] rounded-xl bg-emerald-600 px-3 py-2 text-xs font-black text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-40" @click.stop="runCodeAction(code, 'activate')">激活</button>
                    <button type="button" :disabled="!canVoid(code) || actionLoadingKey === `void:${code.traceCode}`" class="min-h-[40px] rounded-xl border border-rose-200 bg-white px-3 py-2 text-xs font-black text-rose-600 transition hover:bg-rose-50 disabled:cursor-not-allowed disabled:opacity-40" @click.stop="runCodeAction(code, 'void')">作废</button>
                  </div>
                </div>
              </article>
            </div>

            <aside class="rounded-[28px] border border-slate-200 bg-white/80 p-5" data-test="assignment-active-code-panel">
              <p class="text-xs font-black uppercase tracking-[0.2em] text-slate-400">当前单品</p>
              <template v-if="activeCode">
                <h3 class="mt-3 break-all font-mono text-lg font-black text-slate-950">{{ activeCode.traceCode }}</h3>
                <div class="mt-5 space-y-3 text-sm font-medium text-slate-600">
                  <div class="flex justify-between gap-4"><span>批内序号</span><b class="text-slate-900">{{ activeCode.serialNo || '-' }}</b></div>
                  <div class="flex justify-between gap-4"><span>码状态</span><b class="text-slate-900">{{ formatStatus(activeCode.codeStatus) }}</b></div>
                  <div class="flex justify-between gap-4"><span>打印次数</span><b class="text-slate-900">{{ activeCode.printCount || 0 }}</b></div>
                  <div class="flex justify-between gap-4"><span>激活人</span><b class="text-slate-900">{{ activeCode.activatedByUsername || '-' }}</b></div>
                  <div class="flex justify-between gap-4"><span>激活时间</span><b class="text-right text-slate-900">{{ activeCode.activatedTime || '-' }}</b></div>
                </div>
                <div class="mt-6 grid gap-2">
                  <button type="button" class="flex min-h-[44px] items-center justify-center gap-2 rounded-2xl bg-slate-900 px-4 py-3 text-sm font-black text-white transition hover:bg-indigo-700" @click="goTraceDetail(activeCode.traceCode)"><ExternalLink class="h-4 w-4" /> 查看溯源详情</button>
                </div>
              </template>
              <template v-else>
                <XCircle class="mt-8 h-10 w-10 text-slate-300" />
                <p class="mt-3 text-sm font-bold text-slate-500">请选择一个单品码。</p>
              </template>
            </aside>
          </div>
        </section>
      </div>
    </section>

    <QRScanner v-if="scannerOpen" @scan="handleScanActivate" @close="scannerOpen = false" />
  </div>
</template>
