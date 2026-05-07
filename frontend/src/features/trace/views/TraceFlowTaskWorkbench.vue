<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { AlertTriangle, ArrowRight, CheckCircle2, ClipboardList, ExternalLink, Loader2, PackageCheck, PackageOpen, Plus, QrCode, RefreshCw, ScanLine, Search, Truck, Warehouse } from 'lucide-vue-next'
import QRScanner from '@/shared/components/QRScanner.vue'
import { useConfirm } from '@/shared/composables/useConfirm'
import { usePrompt } from '@/shared/composables/usePrompt'
import { useToast } from '@/shared/composables/useToast'
import { completeTraceFlowTask, createTraceFlowTask, getTraceFlowTask, getTraceFlowTasks, getTraceNodes, scanTraceFlowTask } from '@/features/trace/api'
import { logger } from '@/shared/utils/logger'

const router = useRouter()
const toast = useToast()
const { confirm } = useConfirm()
const { prompt } = usePrompt()

const loadingTasks = ref(false)
const loadingNodes = ref(false)
const creating = ref(false)
const scanning = ref(false)
const completing = ref(false)
const scannerOpen = ref(false)
const taskError = ref('')
const selectedTaskId = ref(null)
const taskList = ref([])
const nodes = ref([])
const scanInput = ref('')
const scanRemark = ref('')
const taskSearch = ref('')
const activeStatusFilter = ref('OPEN')

const createForm = reactive({ taskType: 'OUTBOUND', taskNo: '', sourceNodeId: '', targetNodeId: '', expectedQuantity: 1, remark: '' })

const statusOptions = [
  { value: 'OPEN', label: '待办任务', statuses: ['CREATED', 'PROCESSING'] },
  { value: 'CREATED', label: '已创建', statuses: ['CREATED'] },
  { value: 'PROCESSING', label: '处理中', statuses: ['PROCESSING'] },
  { value: 'COMPLETED', label: '已完成', statuses: ['COMPLETED'] },
  { value: 'EXCEPTION', label: '异常', statuses: ['EXCEPTION'] }
]
const taskTypeOptions = [
  { value: 'OUTBOUND', label: '出库任务' },
  { value: 'INBOUND', label: '入库/接收任务' },
  { value: 'RECEIVE', label: '接收确认任务' }
]
const statusTextMap = { CREATED: '已创建', PROCESSING: '处理中', COMPLETED: '已完成', CANCELLED: '已取消', EXCEPTION: '异常' }
const taskTypeTextMap = { OUTBOUND: '出库任务', TRANSFER: '运输/流转任务', INBOUND: '入库任务', RECEIVE: '接收确认任务' }
const discrepancyTextMap = { NONE: '无差异', SHORTAGE: '少扫', OVERAGE: '多扫' }

const selectedTask = computed(() => taskList.value.find((task) => String(task.id) === String(selectedTaskId.value)) || null)
const openTaskCount = computed(() => taskList.value.filter((task) => isOpenTask(task)).length)
const exceptionTaskCount = computed(() => taskList.value.filter((task) => task.status === 'EXCEPTION').length)
const completedTaskCount = computed(() => taskList.value.filter((task) => task.status === 'COMPLETED').length)
const selectedTaskProgress = computed(() => progressPercent(selectedTask.value))
const filteredTasks = computed(() => {
  const query = taskSearch.value.trim().toLowerCase()
  const statuses = statusOptions.find((option) => option.value === activeStatusFilter.value)?.statuses || []
  return taskList.value.filter((task) => {
    if (statuses.length && !statuses.includes(task.status)) return false
    if (!query) return true
    return [task.taskNo, task.sourceNodeName, task.sourceNodeCode, task.targetNodeName, task.targetNodeCode].filter(Boolean).some((value) => String(value).toLowerCase().includes(query))
  })
})

onMounted(async () => {
  await Promise.all([loadNodes(), loadTasks()])
})

async function loadNodes() {
  loadingNodes.value = true
  try {
    const response = await getTraceNodes()
    nodes.value = Array.isArray(response) ? response : []
  } catch (error) {
    logger.error('加载仓库/物流节点失败', error)
    toast.error(error?.message || '加载仓库/物流节点失败')
  } finally {
    loadingNodes.value = false
  }
}

async function loadTasks(options = {}) {
  loadingTasks.value = true
  taskError.value = ''
  try {
    const statuses = activeStatusFilter.value === 'OPEN' ? ['CREATED', 'PROCESSING'] : [activeStatusFilter.value]
    const responses = await Promise.all(statuses.map((status) => getTraceFlowTasks({ status })))
    const merged = mergeTasks(responses.flatMap((item) => Array.isArray(item) ? item : []))
    taskList.value = merged
    selectedTaskId.value = options.selectTaskId || selectedTask.value?.id || merged[0]?.id || null
  } catch (error) {
    logger.error('加载流转任务失败', error)
    taskError.value = error?.message || '加载流转任务失败'
    toast.error(taskError.value)
  } finally {
    loadingTasks.value = false
  }
}

function mergeTasks(tasks) {
  const map = new Map()
  tasks.forEach((task) => { if (task?.id != null) map.set(String(task.id), task) })
  return Array.from(map.values()).sort((a, b) => Number(b.id || 0) - Number(a.id || 0))
}

async function refreshSelectedTask() {
  if (!selectedTaskId.value) return
  try { upsertTask(await getTraceFlowTask(selectedTaskId.value)) } catch (error) { logger.warn('刷新当前流转任务失败', error); await loadTasks({ selectTaskId: selectedTaskId.value }) }
}

function upsertTask(task) {
  if (!task?.id) return
  const index = taskList.value.findIndex((item) => String(item.id) === String(task.id))
  if (index >= 0) taskList.value.splice(index, 1, task)
  else taskList.value.unshift(task)
  selectedTaskId.value = task.id
}

function selectTask(task) { selectedTaskId.value = task?.id || null }

function validateCreateForm() {
  if (!createForm.taskType) return '请选择任务类型'
  if (!createForm.sourceNodeId) return '请选择起点节点'
  if (!createForm.targetNodeId) return '请选择终点节点'
  if (String(createForm.sourceNodeId) === String(createForm.targetNodeId)) return '起点和终点不能相同'
  if (!createForm.expectedQuantity || Number(createForm.expectedQuantity) < 1 || Number(createForm.expectedQuantity) > 100000) return '预计数量必须在 1 到 100000 之间'
  return ''
}

async function handleCreateTask() {
  const validationMessage = validateCreateForm()
  if (validationMessage) { toast.error(validationMessage); return }
  creating.value = true
  try {
    const payload = { taskType: createForm.taskType, sourceNodeId: Number(createForm.sourceNodeId), targetNodeId: Number(createForm.targetNodeId), expectedQuantity: Number(createForm.expectedQuantity) }
    if (createForm.taskNo.trim()) payload.taskNo = createForm.taskNo.trim()
    if (createForm.remark.trim()) payload.remark = createForm.remark.trim()
    const response = await createTraceFlowTask(payload)
    upsertTask(response)
    Object.assign(createForm, { taskType: createForm.taskType || 'OUTBOUND', taskNo: '', sourceNodeId: '', targetNodeId: '', expectedQuantity: 1, remark: '' })
    toast.success(`流转任务 ${response.taskNo || ''} 创建成功`)
  } catch (error) {
    logger.error('创建流转任务失败', error)
    toast.error(error?.message || '创建流转任务失败')
  } finally {
    creating.value = false
  }
}

async function submitManualScan() { await handleTaskScan(scanInput.value) }

async function handleTaskScan(rawCode) {
  const task = selectedTask.value
  const traceCode = rawCode?.trim()
  if (!task?.id) { toast.error('请先选择一个流转任务'); return }
  if (!isOpenTask(task)) { toast.error('当前任务不是待扫码状态'); return }
  if (!traceCode) { toast.error('请输入或扫描溯源码'); return }
  scanning.value = true
  scannerOpen.value = false
  try {
    const payload = { traceCode, eventTime: dayjs().format('YYYY-MM-DDTHH:mm:ss') }
    if (scanRemark.value.trim()) payload.remark = scanRemark.value.trim()
    const response = await scanTraceFlowTask(task.id, payload)
    upsertTask(response)
    scanInput.value = ''
    if (response.duplicateScan) toast.warning(response.scanMessage || '该码已在当前任务内扫码，不重复计数')
    else toast.success(response.scanMessage || '扫码成功，任务进度已更新')
  } catch (error) {
    logger.error('任务内扫码失败', error)
    toast.error(error?.message || '任务内扫码失败')
  } finally {
    scanning.value = false
  }
}

async function handleCompleteTask() {
  const task = selectedTask.value
  if (!task?.id) { toast.error('请先选择一个流转任务'); return }
  if (!isOpenTask(task)) { toast.error('只有已创建或处理中的任务可以完成'); return }
  const expected = Number(task.expectedQuantity || 0)
  const actual = Number(task.actualQuantity || 0)
  let discrepancyReason = ''
  if (actual !== expected) {
    const result = await prompt({
      title: actual < expected ? '填写少扫差异原因' : '填写多扫差异原因',
      message: `预计 ${expected} 件，当前实扫 ${actual} 件。数量不一致时任务会以异常状态关闭，请填写原因。`,
      placeholder: '例如：少件待补扫 / 实物多发需复核',
      confirmText: '提交并关闭任务',
      cancelText: '取消',
      validator: (value) => value?.trim() ? true : '数量不一致时必须填写差异原因'
    })
    if (!result) return
    discrepancyReason = result.trim()
  } else {
    const accepted = await confirm({ title: '完成流转任务', message: `确认完成任务 ${task.taskNo}？当前已扫数量与预计数量一致。`, confirmText: '确认完成', cancelText: '取消' })
    if (!accepted) return
  }
  completing.value = true
  try {
    const payload = {}
    if (discrepancyReason) payload.discrepancyReason = discrepancyReason
    const response = await completeTraceFlowTask(task.id, payload)
    upsertTask(response)
    toast.success(response.status === 'EXCEPTION' ? '任务已按异常差异关闭' : '流转任务已完成')
  } catch (error) {
    logger.error('完成流转任务失败', error)
    toast.error(error?.message || '完成流转任务失败')
  } finally {
    completing.value = false
  }
}

function handleStatusFilterChange(value) { activeStatusFilter.value = value; selectedTaskId.value = null; loadTasks() }
function goTraceDetail(traceCode) { if (traceCode) router.push(`/traces/${traceCode}`) }
function isOpenTask(task) { return task && ['CREATED', 'PROCESSING'].includes(task.status) }
function progressPercent(task) { return task?.expectedQuantity ? Math.min(100, Math.round(((task.actualQuantity || 0) / task.expectedQuantity) * 100)) : 0 }
function formatTaskType(task) { return task?.taskTypeLabel || taskTypeTextMap[task?.taskType] || task?.taskType || '-' }
function formatStatus(status) { return statusTextMap[status] || status || '-' }
function formatDiscrepancy(task) { return task?.discrepancyTypeLabel || discrepancyTextMap[task?.discrepancyType] || task?.discrepancyType || '无差异' }
function statusBadgeClass(status) {
  if (status === 'COMPLETED') return 'bg-emerald-100 text-emerald-700 ring-emerald-200'
  if (status === 'EXCEPTION') return 'bg-rose-100 text-rose-700 ring-rose-200'
  if (status === 'PROCESSING') return 'bg-indigo-100 text-indigo-700 ring-indigo-200'
  if (status === 'CANCELLED') return 'bg-slate-100 text-slate-500 ring-slate-200'
  return 'bg-cyan-100 text-cyan-700 ring-cyan-200'
}
function taskCardClass(task) {
  if (String(task.id) === String(selectedTaskId.value)) return 'border-indigo-300 bg-indigo-50/80 shadow-indigo-100'
  if (task.status === 'EXCEPTION') return 'border-rose-100 bg-rose-50/70'
  if (task.status === 'COMPLETED') return 'border-emerald-100 bg-emerald-50/60'
  return 'border-slate-200 bg-white/75'
}
function scanFeedbackClass(task) {
  if (!task?.scanMessage) return 'border-slate-200 bg-slate-50 text-slate-600'
  return task.duplicateScan ? 'border-amber-200 bg-amber-50 text-amber-800' : 'border-emerald-200 bg-emerald-50 text-emerald-800'
}
function nodeLabel(node) { return node ? `${node.nodeCode || ''}${node.nodeCode && node.nodeName ? ' - ' : ''}${node.nodeName || ''}` || '-' : '-' }
</script>

<template>
  <div class="relative z-10 mx-auto flex w-full max-w-7xl flex-col gap-8 px-4 py-8 sm:px-6 lg:px-8">
    <header class="premium-card overflow-hidden rounded-[40px] p-8 lg:p-10">
      <div class="flex flex-col gap-8 lg:flex-row lg:items-end lg:justify-between">
        <div class="max-w-3xl">
          <p class="mb-3 flex items-center gap-2 text-xs font-black uppercase tracking-[0.24em] text-indigo-500"><Warehouse class="h-4 w-4" /> Warehouse Logistics Workbench</p>
          <h1 class="text-3xl font-black tracking-tight text-slate-950 sm:text-5xl">仓库/物流任务工作台</h1>
          <p class="mt-4 text-base font-medium leading-8 text-slate-500">仓库和物流人员从待办任务进入连续扫码，系统按任务自动填充起点、终点、动作和时间；一线操作只需要扫码、看进度、完成任务。</p>
        </div>
        <div class="grid min-w-[300px] grid-cols-3 gap-3 rounded-[32px] bg-white/70 p-4 shadow-inner shadow-indigo-50">
          <div><p class="text-[10px] font-black uppercase tracking-widest text-slate-400">待办</p><p class="mt-1 text-2xl font-black text-indigo-600">{{ openTaskCount }}</p></div>
          <div><p class="text-[10px] font-black uppercase tracking-widest text-slate-400">已完成</p><p class="mt-1 text-2xl font-black text-emerald-600">{{ completedTaskCount }}</p></div>
          <div><p class="text-[10px] font-black uppercase tracking-widest text-slate-400">异常</p><p class="mt-1 text-2xl font-black text-rose-600">{{ exceptionTaskCount }}</p></div>
        </div>
      </div>
    </header>

    <section class="grid gap-6 xl:grid-cols-[380px_minmax(0,1fr)]">
      <aside class="flex flex-col gap-6">
        <form class="premium-card rounded-[36px] p-6" data-test="flow-task-create-form" @submit.prevent="handleCreateTask">
          <div class="mb-6 flex items-start justify-between gap-4"><div><p class="text-xs font-black uppercase tracking-[0.2em] text-slate-400">Task Setup</p><h2 class="mt-2 text-2xl font-black text-slate-900">创建发货/接收任务</h2></div><div class="rounded-2xl bg-indigo-50 p-3 text-indigo-600"><Plus class="h-6 w-6" /></div></div>
          <div class="space-y-5">
            <label class="block"><span class="mb-2 block text-sm font-bold text-slate-700">任务类型</span><select v-model="createForm.taskType" class="h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm font-bold text-slate-700 outline-none transition focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100" data-test="flow-task-type-select"><option v-for="option in taskTypeOptions" :key="option.value" :value="option.value">{{ option.label }}</option></select></label>
            <label class="block"><span class="mb-2 block text-sm font-bold text-slate-700">任务单号（可选）</span><input v-model="createForm.taskNo" type="text" maxlength="64" placeholder="SHIP-20260507-001" class="h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm font-bold text-slate-700 outline-none transition focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100" /></label>
            <label class="block"><span class="mb-2 block text-sm font-bold text-slate-700">起点节点</span><select v-model="createForm.sourceNodeId" :disabled="loadingNodes" class="h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm font-bold text-slate-700 outline-none transition focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 disabled:opacity-60" data-test="flow-task-source-select"><option value="">请选择起点</option><option v-for="node in nodes" :key="node.id" :value="node.id">{{ nodeLabel(node) }}</option></select></label>
            <label class="block"><span class="mb-2 block text-sm font-bold text-slate-700">终点节点</span><select v-model="createForm.targetNodeId" :disabled="loadingNodes" class="h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm font-bold text-slate-700 outline-none transition focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 disabled:opacity-60" data-test="flow-task-target-select"><option value="">请选择终点</option><option v-for="node in nodes" :key="node.id" :value="node.id">{{ nodeLabel(node) }}</option></select></label>
            <label class="block"><span class="mb-2 block text-sm font-bold text-slate-700">预计数量</span><input v-model="createForm.expectedQuantity" type="number" min="1" max="100000" class="h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm font-bold text-slate-700 outline-none transition focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100" data-test="flow-task-expected-input" /></label>
            <label class="block"><span class="mb-2 block text-sm font-bold text-slate-700">任务备注（可选）</span><textarea v-model="createForm.remark" maxlength="255" rows="3" placeholder="运输车次、交接说明等" class="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm font-bold text-slate-700 outline-none transition focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100"></textarea></label>
          </div>
          <button type="submit" :disabled="creating" class="mt-6 flex min-h-[52px] w-full items-center justify-center gap-3 rounded-2xl bg-indigo-600 px-5 py-4 text-sm font-black text-white shadow-xl shadow-indigo-200 transition hover:bg-indigo-700 active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-60" data-test="flow-task-create-submit"><Loader2 v-if="creating" class="h-5 w-5 animate-spin" /><ClipboardList v-else class="h-5 w-5" />{{ creating ? '正在创建任务...' : '创建流转任务' }}</button>
        </form>
      </aside>

      <main class="flex flex-col gap-6">
        <section class="premium-card rounded-[36px] p-6 lg:p-8">
          <div class="mb-6 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div><p class="text-xs font-black uppercase tracking-[0.2em] text-slate-400">Todo Queue</p><h2 class="mt-2 text-2xl font-black text-slate-900">待办任务</h2></div>
            <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
              <div class="flex rounded-2xl bg-slate-100 p-1"><button v-for="option in statusOptions" :key="option.value" type="button" class="min-h-[40px] rounded-xl px-3 text-xs font-black transition" :class="activeStatusFilter === option.value ? 'bg-white text-indigo-600 shadow-sm' : 'text-slate-500 hover:text-slate-800'" :data-test="`flow-task-filter-${option.value}`" @click="handleStatusFilterChange(option.value)">{{ option.label }}</button></div>
              <button type="button" :disabled="loadingTasks" class="flex min-h-[42px] items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 text-sm font-black text-slate-600 transition hover:border-indigo-300 hover:text-indigo-600 disabled:opacity-60" data-test="flow-task-refresh" @click="loadTasks({ selectTaskId: selectedTaskId })"><RefreshCw class="h-4 w-4" :class="loadingTasks ? 'animate-spin' : ''" />刷新</button>
            </div>
          </div>
          <div class="mb-5 flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3"><Search class="h-4 w-4 text-slate-400" /><input v-model="taskSearch" type="text" placeholder="按任务单号/节点筛选" class="h-8 flex-1 border-0 bg-transparent text-sm font-bold text-slate-700 outline-none" data-test="flow-task-search" /></div>
          <div v-if="taskError" class="mb-5 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-bold text-rose-700">{{ taskError }}</div>
          <div v-if="loadingTasks" class="rounded-[28px] border border-dashed border-slate-200 bg-white/50 p-8 text-center"><Loader2 class="mx-auto h-10 w-10 animate-spin text-indigo-500" /><p class="mt-3 text-sm font-bold text-slate-500">正在加载流转任务...</p></div>
          <div v-else-if="filteredTasks.length === 0" class="rounded-[28px] border border-dashed border-slate-200 bg-white/50 p-8 text-center" data-test="flow-task-empty"><ClipboardList class="mx-auto h-10 w-10 text-slate-300" /><p class="mt-3 text-sm font-bold text-slate-500">当前筛选下暂无任务。</p></div>
          <div v-else class="grid gap-3" data-test="flow-task-list">
            <article v-for="task in filteredTasks" :key="task.id" class="cursor-pointer rounded-[24px] border p-4 transition hover:-translate-y-0.5 hover:shadow-lg" :class="taskCardClass(task)" :data-test="`flow-task-row-${task.taskNo}`" @click="selectTask(task)">
              <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                <div class="min-w-0"><div class="mb-2 flex flex-wrap items-center gap-2"><span class="rounded-full bg-white/80 px-2.5 py-1 text-[10px] font-black uppercase tracking-widest text-slate-400">{{ formatTaskType(task) }}</span><span class="rounded-full px-2.5 py-1 text-[10px] font-black ring-1" :class="statusBadgeClass(task.status)">{{ formatStatus(task.status) }}</span></div><p class="truncate font-mono text-sm font-black text-slate-900 sm:text-base">{{ task.taskNo }}</p><p class="mt-2 flex flex-wrap items-center gap-2 text-xs font-bold text-slate-500"><span>{{ task.sourceNodeName || task.sourceNodeCode || '-' }}</span><ArrowRight class="h-3.5 w-3.5" /><span>{{ task.targetNodeName || task.targetNodeCode || '-' }}</span></p></div>
                <div class="min-w-[180px]"><div class="mb-2 flex items-center justify-between text-xs font-black text-slate-500"><span>进度</span><span>{{ task.actualQuantity || 0 }}/{{ task.expectedQuantity || 0 }}</span></div><div class="h-2 overflow-hidden rounded-full bg-white/80"><div class="h-full rounded-full bg-indigo-500 transition-all" :style="{ width: progressPercent(task) + '%' }"></div></div></div>
              </div>
            </article>
          </div>
        </section>

        <section class="premium-card rounded-[36px] p-6 lg:p-8" data-test="flow-task-detail">
          <template v-if="selectedTask">
            <div class="mb-6 flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
              <div><p class="text-xs font-black uppercase tracking-[0.2em] text-slate-400">Task Detail</p><h2 class="mt-2 break-all font-mono text-2xl font-black text-slate-900">{{ selectedTask.taskNo }}</h2><p class="mt-2 text-sm font-bold text-slate-500">{{ formatTaskType(selectedTask) }} · {{ formatStatus(selectedTask.status) }}</p></div>
              <div class="flex flex-wrap gap-2"><button type="button" :disabled="!isOpenTask(selectedTask) || completing" class="flex min-h-[44px] items-center gap-2 rounded-2xl bg-emerald-600 px-4 py-3 text-sm font-black text-white shadow-lg shadow-emerald-100 transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-50" data-test="flow-task-complete" @click="handleCompleteTask"><Loader2 v-if="completing" class="h-4 w-4 animate-spin" /><CheckCircle2 v-else class="h-4 w-4" />一键完成任务</button><button type="button" class="flex min-h-[44px] items-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm font-black text-slate-600 transition hover:border-indigo-300 hover:text-indigo-600" @click="refreshSelectedTask"><RefreshCw class="h-4 w-4" />刷新详情</button></div>
            </div>
            <div class="mb-6 grid gap-4 md:grid-cols-4">
              <div class="rounded-[26px] bg-indigo-50 p-5 text-indigo-700"><p class="text-xs font-black uppercase tracking-widest opacity-70">已扫/应扫</p><p class="mt-3 text-3xl font-black text-slate-950">{{ selectedTask.actualQuantity || 0 }}/{{ selectedTask.expectedQuantity || 0 }}</p></div>
              <div class="rounded-[26px] bg-cyan-50 p-5 text-cyan-700"><p class="text-xs font-black uppercase tracking-widest opacity-70">剩余</p><p class="mt-3 text-3xl font-black text-slate-950">{{ selectedTask.remainingQuantity ?? 0 }}</p></div>
              <div class="rounded-[26px] bg-emerald-50 p-5 text-emerald-700"><p class="text-xs font-black uppercase tracking-widest opacity-70">完成率</p><p class="mt-3 text-3xl font-black text-slate-950">{{ selectedTaskProgress }}%</p></div>
              <div class="rounded-[26px] bg-rose-50 p-5 text-rose-700"><p class="text-xs font-black uppercase tracking-widest opacity-70">差异</p><p class="mt-3 text-lg font-black text-slate-950">{{ formatDiscrepancy(selectedTask) }}</p></div>
            </div>
            <div class="mb-6 rounded-[28px] border border-slate-200 bg-white/80 p-5">
              <div class="mb-3 flex items-center justify-between text-xs font-black text-slate-500"><span>{{ selectedTask.actualQuantity || 0 }}/{{ selectedTask.expectedQuantity || 0 }} 已扫，剩余 {{ selectedTask.remainingQuantity ?? 0 }}</span><span>{{ selectedTaskProgress }}%</span></div>
              <div class="h-3 overflow-hidden rounded-full bg-slate-100"><div class="h-full rounded-full bg-gradient-to-r from-indigo-500 to-cyan-500 transition-all" :style="{ width: selectedTaskProgress + '%' }"></div></div>
              <div class="mt-4 grid gap-4 text-sm font-bold text-slate-600 md:grid-cols-2"><div class="flex items-center gap-3"><PackageOpen class="h-5 w-5 text-cyan-500" /><span>起点：{{ selectedTask.sourceNodeName || selectedTask.sourceNodeCode || '-' }}</span></div><div class="flex items-center gap-3"><PackageCheck class="h-5 w-5 text-emerald-500" /><span>终点：{{ selectedTask.targetNodeName || selectedTask.targetNodeCode || '-' }}</span></div></div>
              <p v-if="selectedTask.discrepancyReason" class="mt-4 rounded-2xl bg-rose-50 px-4 py-3 text-sm font-bold text-rose-700">差异原因：{{ selectedTask.discrepancyReason }}</p>
            </div>
            <div class="grid gap-5 lg:grid-cols-[minmax(0,1fr)_320px]">
              <div class="rounded-[28px] border border-slate-200 bg-white/80 p-5">
                <div class="mb-5 flex items-start justify-between gap-4"><div><p class="text-xs font-black uppercase tracking-[0.2em] text-slate-400">Continuous Scan</p><h3 class="mt-2 text-xl font-black text-slate-900">连续扫码</h3></div><button type="button" :disabled="!isOpenTask(selectedTask)" class="flex min-h-[44px] items-center gap-2 rounded-2xl bg-indigo-600 px-4 py-3 text-sm font-black text-white shadow-lg shadow-indigo-100 transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50" data-test="flow-task-open-scanner" @click="scannerOpen = true"><ScanLine class="h-4 w-4" />摄像头扫码</button></div>
                <form class="grid gap-3 sm:grid-cols-[minmax(0,1fr)_auto]" data-test="flow-task-scan-form" @submit.prevent="submitManualScan"><input v-model="scanInput" :disabled="!isOpenTask(selectedTask) || scanning" type="text" placeholder="扫描或输入单品码 / 箱码 / 托盘码" class="h-12 rounded-2xl border border-slate-200 bg-white px-4 font-mono text-sm font-black text-slate-700 outline-none transition focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100 disabled:opacity-60" data-test="flow-task-scan-input" /><button type="submit" :disabled="!isOpenTask(selectedTask) || scanning" class="flex min-h-[48px] items-center justify-center gap-2 rounded-2xl bg-slate-900 px-5 py-3 text-sm font-black text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50" data-test="flow-task-scan-submit"><Loader2 v-if="scanning" class="h-4 w-4 animate-spin" /><QrCode v-else class="h-4 w-4" />提交扫码</button></form>
                <label class="mt-4 block"><span class="mb-2 block text-sm font-bold text-slate-700">扫码备注（可选）</span><input v-model="scanRemark" maxlength="255" type="text" placeholder="默认由任务号自动生成备注" class="h-11 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm font-bold text-slate-700 outline-none transition focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100" /></label>
                <div class="mt-5 rounded-2xl border px-4 py-3 text-sm font-bold" :class="scanFeedbackClass(selectedTask)" data-test="flow-task-scan-feedback"><template v-if="selectedTask.scanMessage"><p>{{ selectedTask.scanMessage }}</p><button v-if="selectedTask.lastScanTraceCode" type="button" class="mt-2 inline-flex items-center gap-1 text-xs underline-offset-4 hover:underline" @click="goTraceDetail(selectedTask.lastScanTraceCode)">{{ selectedTask.lastScanTraceCode }} <ExternalLink class="h-3.5 w-3.5" /></button></template><template v-else>扫码后这里会显示“成功累计”或“重复已扫”反馈。</template></div>
              </div>
              <aside class="rounded-[28px] border border-slate-200 bg-white/80 p-5"><p class="text-xs font-black uppercase tracking-[0.2em] text-slate-400">Operator Guide</p><h3 class="mt-2 text-lg font-black text-slate-900">一线操作提示</h3><ul class="mt-5 space-y-3 text-sm font-bold text-slate-500"><li class="flex gap-3"><Truck class="mt-0.5 h-5 w-5 flex-shrink-0 text-indigo-500" />出库任务会自动写出库事件。</li><li class="flex gap-3"><Warehouse class="mt-0.5 h-5 w-5 flex-shrink-0 text-emerald-500" />目标节点扫码会自动写接收入库事件。</li><li class="flex gap-3"><QrCode class="mt-0.5 h-5 w-5 flex-shrink-0 text-cyan-500" />扫描箱码或托盘码时会自动展开单品码批量处理。</li><li class="flex gap-3"><AlertTriangle class="mt-0.5 h-5 w-5 flex-shrink-0 text-amber-500" />重复扫码提示“已扫”，不会重复计数。</li></ul></aside>
            </div>
          </template>
          <template v-else><div class="rounded-[28px] border border-dashed border-slate-200 bg-white/50 p-10 text-center"><ClipboardList class="mx-auto h-12 w-12 text-slate-300" /><p class="mt-4 text-sm font-bold text-slate-500">请选择或创建一个仓库/物流流转任务。</p></div></template>
        </section>
      </main>
    </section>
    <QRScanner v-if="scannerOpen" @scan="handleTaskScan" @close="scannerOpen = false" />
  </div>
</template>
