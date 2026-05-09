<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import {
  ArrowRight,
  CheckCircle2,
  ClipboardList,
  ExternalLink,
  Plus,
  QrCode,
  RefreshCw,
  ScanLine,
  Search
} from 'lucide-vue-next'
import PageHeader from '@/shared/components/ui/PageHeader.vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import StatusPill from '@/shared/components/ui/StatusPill.vue'
import EmptyState from '@/shared/components/ui/EmptyState.vue'
import LoadingSkeleton from '@/shared/components/ui/LoadingSkeleton.vue'
import KbdShortcut from '@/shared/components/ui/KbdShortcut.vue'
import QRScanner from '@/shared/components/QRScanner.vue'
import { useConfirm } from '@/shared/composables/useConfirm'
import { usePrompt } from '@/shared/composables/usePrompt'
import { useToast } from '@/shared/composables/useToast'
import {
  completeTraceFlowTask,
  createTraceFlowTask,
  getTraceFlowTask,
  getTraceFlowTasks,
  getTraceNodes,
  scanTraceFlowTask
} from '@/features/trace/api'
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

const createForm = reactive({
  taskType: 'OUTBOUND',
  taskNo: '',
  sourceNodeId: '',
  targetNodeId: '',
  expectedQuantity: 1,
  remark: ''
})

const STATUS_OPTIONS = [
  { value: 'OPEN', label: '待办', statuses: ['CREATED', 'PROCESSING'] },
  { value: 'CREATED', label: '已创建', statuses: ['CREATED'] },
  { value: 'PROCESSING', label: '处理中', statuses: ['PROCESSING'] },
  { value: 'COMPLETED', label: '已完成', statuses: ['COMPLETED'] },
  { value: 'EXCEPTION', label: '异常', statuses: ['EXCEPTION'] }
]
const TASK_TYPE_OPTIONS = [
  { value: 'OUTBOUND', label: '出库任务' },
  { value: 'INBOUND', label: '入库/接收任务' },
  { value: 'RECEIVE', label: '接收确认任务' }
]
const STATUS_TEXT = {
  CREATED: '已创建',
  PROCESSING: '处理中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  EXCEPTION: '异常'
}
const TASK_TYPE_TEXT = {
  OUTBOUND: '出库',
  TRANSFER: '运输',
  INBOUND: '入库',
  RECEIVE: '接收'
}
const DISCREPANCY_TEXT = { NONE: '无差异', SHORTAGE: '少扫', OVERAGE: '多扫' }

const selectedTask = computed(
  () => taskList.value.find((task) => String(task.id) === String(selectedTaskId.value)) || null
)
const openTaskCount = computed(() => taskList.value.filter((task) => isOpenTask(task)).length)
const exceptionTaskCount = computed(() => taskList.value.filter((task) => task.status === 'EXCEPTION').length)
const completedTaskCount = computed(() => taskList.value.filter((task) => task.status === 'COMPLETED').length)
const selectedTaskProgress = computed(() => progressPercent(selectedTask.value))

const filteredTasks = computed(() => {
  const query = taskSearch.value.trim().toLowerCase()
  const statuses = STATUS_OPTIONS.find((option) => option.value === activeStatusFilter.value)?.statuses || []
  return taskList.value.filter((task) => {
    if (statuses.length && !statuses.includes(task.status)) return false
    if (!query) return true
    return [task.taskNo, task.sourceNodeName, task.sourceNodeCode, task.targetNodeName, task.targetNodeCode]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(query))
  })
})

const headerSubtitle = computed(() => {
  if (selectedTask.value) {
    const task = selectedTask.value
    return `任务 ${task.taskNo} · ${task.actualQuantity || 0}/${task.expectedQuantity || 0} 已扫 · ${formatStatus(task.status)}`
  }
  return '仓库与物流人员从待办任务进入连续扫码，系统按任务自动填充起点、终点、动作和时间。'
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
    const merged = mergeTasks(responses.flatMap((item) => (Array.isArray(item) ? item : [])))
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
  tasks.forEach((task) => {
    if (task?.id != null) map.set(String(task.id), task)
  })
  return Array.from(map.values()).sort((a, b) => Number(b.id || 0) - Number(a.id || 0))
}

async function refreshSelectedTask() {
  if (!selectedTaskId.value) return
  try {
    upsertTask(await getTraceFlowTask(selectedTaskId.value))
  } catch (error) {
    logger.warn('刷新当前流转任务失败', error)
    await loadTasks({ selectTaskId: selectedTaskId.value })
  }
}

function upsertTask(task) {
  if (!task?.id) return
  const index = taskList.value.findIndex((item) => String(item.id) === String(task.id))
  if (index >= 0) taskList.value.splice(index, 1, task)
  else taskList.value.unshift(task)
  selectedTaskId.value = task.id
}

function selectTask(task) {
  selectedTaskId.value = task?.id || null
}

function validateCreateForm() {
  if (!createForm.taskType) return '请选择任务类型'
  if (!createForm.sourceNodeId) return '请选择起点节点'
  if (!createForm.targetNodeId) return '请选择终点节点'
  if (String(createForm.sourceNodeId) === String(createForm.targetNodeId)) return '起点和终点不能相同'
  const expected = Number(createForm.expectedQuantity)
  if (!expected || expected < 1 || expected > 100000) return '预计数量必须在 1 到 100000 之间'
  return ''
}

async function handleCreateTask() {
  const validationMessage = validateCreateForm()
  if (validationMessage) {
    toast.error(validationMessage)
    return
  }
  creating.value = true
  try {
    const payload = {
      taskType: createForm.taskType,
      sourceNodeId: Number(createForm.sourceNodeId),
      targetNodeId: Number(createForm.targetNodeId),
      expectedQuantity: Number(createForm.expectedQuantity)
    }
    if (createForm.taskNo.trim()) payload.taskNo = createForm.taskNo.trim()
    if (createForm.remark.trim()) payload.remark = createForm.remark.trim()
    const response = await createTraceFlowTask(payload)
    upsertTask(response)
    Object.assign(createForm, {
      taskType: createForm.taskType || 'OUTBOUND',
      taskNo: '',
      sourceNodeId: '',
      targetNodeId: '',
      expectedQuantity: 1,
      remark: ''
    })
    toast.success(`流转任务 ${response.taskNo || ''} 创建成功`)
  } catch (error) {
    logger.error('创建流转任务失败', error)
    toast.error(error?.message || '创建流转任务失败')
  } finally {
    creating.value = false
  }
}

async function submitManualScan() {
  await handleTaskScan(scanInput.value)
}

async function handleTaskScan(rawCode) {
  const task = selectedTask.value
  const traceCode = rawCode?.trim()
  if (!task?.id) {
    toast.error('请先选择一个流转任务')
    return
  }
  if (!isOpenTask(task)) {
    toast.error('当前任务不是待扫码状态')
    return
  }
  if (!traceCode) {
    toast.error('请输入或扫描溯源码')
    return
  }
  scanning.value = true
  scannerOpen.value = false
  try {
    const payload = { traceCode, eventTime: dayjs().format('YYYY-MM-DDTHH:mm:ss') }
    if (scanRemark.value.trim()) payload.remark = scanRemark.value.trim()
    const response = await scanTraceFlowTask(task.id, payload)
    upsertTask(response)
    scanInput.value = ''
    if (response.duplicateScan) {
      toast.warning(response.scanMessage || '该码已在当前任务内扫码，不重复计数')
    } else {
      toast.success(response.scanMessage || '扫码成功，任务进度已更新')
    }
  } catch (error) {
    logger.error('任务内扫码失败', error)
    toast.error(error?.message || '任务内扫码失败')
  } finally {
    scanning.value = false
  }
}

async function handleCompleteTask() {
  const task = selectedTask.value
  if (!task?.id) {
    toast.error('请先选择一个流转任务')
    return
  }
  if (!isOpenTask(task)) {
    toast.error('只有已创建或处理中的任务可以完成')
    return
  }
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
      validator: (value) => (value?.trim() ? true : '数量不一致时必须填写差异原因')
    })
    if (!result) return
    discrepancyReason = result.trim()
  } else {
    const accepted = await confirm({
      title: '完成流转任务',
      message: `确认完成任务 ${task.taskNo}？当前已扫数量与预计数量一致。`,
      confirmText: '确认完成',
      cancelText: '取消'
    })
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

function handleStatusFilterChange(value) {
  activeStatusFilter.value = value
  selectedTaskId.value = null
  loadTasks()
}

function goTraceDetail(traceCode) {
  if (traceCode) router.push(`/traces/${traceCode}`)
}

function isOpenTask(task) {
  return Boolean(task) && ['CREATED', 'PROCESSING'].includes(task.status)
}

function progressPercent(task) {
  return task?.expectedQuantity
    ? Math.min(100, Math.round(((task.actualQuantity || 0) / task.expectedQuantity) * 100))
    : 0
}

function formatTaskType(task) {
  return task?.taskTypeLabel || TASK_TYPE_TEXT[task?.taskType] || task?.taskType || '-'
}
function formatStatus(status) {
  return STATUS_TEXT[status] || status || '-'
}
function formatDiscrepancy(task) {
  return task?.discrepancyTypeLabel || DISCREPANCY_TEXT[task?.discrepancyType] || task?.discrepancyType || '无差异'
}

function statusTone(status) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'EXCEPTION') return 'error'
  if (status === 'PROCESSING') return 'primary'
  if (status === 'CANCELLED') return 'mute'
  return 'mute'
}

function feedbackTone(task) {
  if (!task?.scanMessage) return 'mute'
  if (task.duplicateScan) return 'warn'
  return 'success'
}

function nodeLabel(node) {
  if (!node) return '-'
  const code = node.nodeCode || ''
  const name = node.nodeName || ''
  if (code && name) return `${code} - ${name}`
  return code || name || '-'
}
</script>

<template>
  <div class="flow-task">
    <PageHeader
      title="仓库/物流任务工作台"
      :subtitle="headerSubtitle"
      data-testid="flow-task-page-header"
    >
      <template #actions>
        <BaseButton
          variant="secondary"
          :loading="loadingTasks"
          :disabled="loadingTasks"
          data-test="flow-task-refresh"
          @click="loadTasks({ selectTaskId: selectedTaskId })"
        >
          <template #icon><RefreshCw :size="13" /></template>
          刷新
        </BaseButton>
        <BaseButton
          variant="primary"
          :loading="completing"
          :disabled="!isOpenTask(selectedTask) || completing"
          data-test="flow-task-complete"
          @click="handleCompleteTask"
        >
          <template #icon><CheckCircle2 :size="13" /></template>
          完成任务
          <template #kbd><KbdShortcut keys="Enter" tone="inverse" /></template>
        </BaseButton>
      </template>
    </PageHeader>

    <section class="flow-task__layout">
      <!-- 左栏：创建任务 form -->
      <form
        class="flow-task__form"
        data-test="flow-task-create-form"
        @submit.prevent="handleCreateTask"
      >
        <div class="flow-task__form-head">
          <div class="flow-task__eyebrow">
            <Plus :size="13" />
            <span>STEP 1 · 任务配置</span>
          </div>
          <h2 class="flow-task__title">创建发货 / 接收任务</h2>
          <p class="flow-task__subtitle">
            按任务一次写入起点、终点、动作和时间，连续扫码无需手填节点。
          </p>
        </div>

        <div class="flow-task__field">
          <label class="flow-task__label" for="flow-task-type">任务类型 <span class="flow-task__required">*</span></label>
          <select
            id="flow-task-type"
            v-model="createForm.taskType"
            class="flow-task__control"
            data-test="flow-task-type-select"
          >
            <option v-for="option in TASK_TYPE_OPTIONS" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </div>

        <div class="flow-task__field">
          <label class="flow-task__label" for="flow-task-no">任务单号（可选）</label>
          <input
            id="flow-task-no"
            v-model="createForm.taskNo"
            type="text"
            maxlength="64"
            placeholder="SHIP-20260507-001"
            class="flow-task__control flow-task__control--mono"
          />
        </div>

        <div class="flow-task__field">
          <label class="flow-task__label" for="flow-task-source">起点节点 <span class="flow-task__required">*</span></label>
          <select
            id="flow-task-source"
            v-model="createForm.sourceNodeId"
            :disabled="loadingNodes"
            class="flow-task__control"
            data-test="flow-task-source-select"
          >
            <option value="">请选择起点</option>
            <option v-for="node in nodes" :key="`source-${node.id}`" :value="node.id">
              {{ nodeLabel(node) }}
            </option>
          </select>
        </div>

        <div class="flow-task__field">
          <label class="flow-task__label" for="flow-task-target">终点节点 <span class="flow-task__required">*</span></label>
          <select
            id="flow-task-target"
            v-model="createForm.targetNodeId"
            :disabled="loadingNodes"
            class="flow-task__control"
            data-test="flow-task-target-select"
          >
            <option value="">请选择终点</option>
            <option v-for="node in nodes" :key="`target-${node.id}`" :value="node.id">
              {{ nodeLabel(node) }}
            </option>
          </select>
        </div>

        <div class="flow-task__field">
          <label class="flow-task__label" for="flow-task-expected">预计数量 <span class="flow-task__required">*</span></label>
          <input
            id="flow-task-expected"
            v-model="createForm.expectedQuantity"
            type="number"
            min="1"
            max="100000"
            class="flow-task__control flow-task__control--mono"
            data-test="flow-task-expected-input"
          />
        </div>

        <div class="flow-task__field">
          <label class="flow-task__label" for="flow-task-remark">任务备注（可选）</label>
          <textarea
            id="flow-task-remark"
            v-model="createForm.remark"
            maxlength="255"
            rows="3"
            placeholder="运输车次 / 交接说明等"
            class="flow-task__control flow-task__control--textarea"
          ></textarea>
        </div>

        <BaseButton
          type="submit"
          variant="primary"
          size="md"
          block
          :loading="creating"
          :disabled="creating"
          data-test="flow-task-create-submit"
        >
          <template #icon><ClipboardList :size="14" /></template>
          {{ creating ? '正在创建任务…' : '创建流转任务' }}
        </BaseButton>
      </form>

      <!-- 右栏：任务队列 + 当前任务 -->
      <div class="flow-task__main">
        <!-- 任务队列卡 -->
        <section class="flow-task__queue">
          <header class="flow-task__queue-head">
            <div>
              <div class="flow-task__eyebrow">
                <ClipboardList :size="13" />
                <span>STEP 2 · 任务队列</span>
              </div>
              <h2 class="flow-task__title">待办任务</h2>
            </div>
            <div class="flow-task__queue-stats">
              <span class="flow-task__stat" data-testid="flow-task-stat-open">
                <span class="flow-task__stat-label">待办</span>
                <span class="flow-task__stat-value mono">{{ openTaskCount }}</span>
              </span>
              <span class="flow-task__stat" data-testid="flow-task-stat-completed">
                <span class="flow-task__stat-label">已完成</span>
                <span class="flow-task__stat-value mono">{{ completedTaskCount }}</span>
              </span>
              <span class="flow-task__stat" data-testid="flow-task-stat-exception">
                <span class="flow-task__stat-label">异常</span>
                <span class="flow-task__stat-value mono">{{ exceptionTaskCount }}</span>
              </span>
            </div>
          </header>

          <div class="flow-task__filter-row">
            <div class="flow-task__chips" role="tablist">
              <button
                v-for="option in STATUS_OPTIONS"
                :key="option.value"
                type="button"
                class="flow-task__chip"
                :class="activeStatusFilter === option.value && 'flow-task__chip--active'"
                :data-test="`flow-task-filter-${option.value}`"
                @click="handleStatusFilterChange(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
            <div class="flow-task__search">
              <Search :size="13" />
              <input
                v-model="taskSearch"
                type="text"
                placeholder="按任务单号 / 节点筛选"
                data-test="flow-task-search"
              />
            </div>
          </div>

          <div v-if="taskError" class="flow-task__error" role="alert">{{ taskError }}</div>

          <div v-if="loadingTasks && taskList.length === 0" class="flow-task__loading">
            <LoadingSkeleton type="table" :rows="4" />
          </div>

          <div v-else-if="filteredTasks.length === 0" class="flow-task__empty" data-test="flow-task-empty">
            <EmptyState
              :icon="ClipboardList"
              title="当前筛选下暂无任务"
              subtitle="切换筛选条件、刷新队列或在左侧创建一个新的流转任务。"
            />
          </div>

          <div v-else class="flow-task__list-wrap" data-test="flow-task-list">
            <table class="flow-task__list">
              <thead>
                <tr>
                  <th class="flow-task__col-no">任务编号</th>
                  <th class="flow-task__col-type">类型</th>
                  <th class="flow-task__col-status">状态</th>
                  <th class="flow-task__col-route">起点 → 终点</th>
                  <th class="flow-task__col-progress">进度</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="task in filteredTasks"
                  :key="task.id"
                  :class="[
                    'flow-task__row',
                    String(task.id) === String(selectedTaskId) && 'flow-task__row--active'
                  ]"
                  :data-test="`flow-task-row-${task.taskNo}`"
                  @click="selectTask(task)"
                >
                  <td class="mono flow-task__cell-no">{{ task.taskNo }}</td>
                  <td class="flow-task__cell-type">{{ formatTaskType(task) }}</td>
                  <td>
                    <StatusPill :tone="statusTone(task.status)">{{ formatStatus(task.status) }}</StatusPill>
                  </td>
                  <td class="flow-task__cell-route">
                    <span>{{ task.sourceNodeName || task.sourceNodeCode || '-' }}</span>
                    <ArrowRight :size="12" class="flow-task__arrow" />
                    <span>{{ task.targetNodeName || task.targetNodeCode || '-' }}</span>
                  </td>
                  <td class="flow-task__cell-progress">
                    <div class="flow-task__progress-row">
                      <span class="mono flow-task__progress-text">
                        {{ task.actualQuantity || 0 }}/{{ task.expectedQuantity || 0 }}
                      </span>
                      <div class="flow-task__progress-bar" :title="`${progressPercent(task)}%`">
                        <span :style="{ width: progressPercent(task) + '%' }"></span>
                      </div>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- 当前任务详情 -->
        <section class="flow-task__detail" data-test="flow-task-detail">
          <template v-if="selectedTask">
            <header class="flow-task__detail-head">
              <div>
                <div class="flow-task__eyebrow">
                  <ScanLine :size="13" />
                  <span>STEP 3 · 连续扫码</span>
                </div>
                <h2 class="flow-task__title mono">{{ selectedTask.taskNo }}</h2>
                <p class="flow-task__subtitle">
                  {{ formatTaskType(selectedTask) }} ·
                  {{ selectedTask.sourceNodeName || selectedTask.sourceNodeCode || '-' }} →
                  {{ selectedTask.targetNodeName || selectedTask.targetNodeCode || '-' }}
                </p>
              </div>
              <div class="flow-task__detail-actions">
                <BaseButton variant="text" size="sm" @click="refreshSelectedTask">
                  <template #icon><RefreshCw :size="13" /></template>
                  刷新详情
                </BaseButton>
                <BaseButton
                  variant="secondary"
                  size="sm"
                  :disabled="!isOpenTask(selectedTask)"
                  data-test="flow-task-open-scanner"
                  @click="scannerOpen = true"
                >
                  <template #icon><ScanLine :size="13" /></template>
                  摄像头扫码
                </BaseButton>
              </div>
            </header>

            <!-- 4-up KPI -->
            <div class="flow-task__kpi-grid">
              <article class="flow-task__kpi" data-testid="flow-task-kpi-actual">
                <span class="flow-task__kpi-label">已扫 / 应扫</span>
                <span class="flow-task__kpi-value mono">
                  {{ selectedTask.actualQuantity || 0 }}<span class="flow-task__kpi-divider">/</span>{{ selectedTask.expectedQuantity || 0 }}
                </span>
                <span class="flow-task__kpi-hint">本任务累计扫码数</span>
              </article>
              <article class="flow-task__kpi" data-testid="flow-task-kpi-remaining">
                <span class="flow-task__kpi-label">剩余</span>
                <span class="flow-task__kpi-value mono">{{ selectedTask.remainingQuantity || 0 }}</span>
                <span class="flow-task__kpi-hint">仍需扫码件数</span>
              </article>
              <article class="flow-task__kpi" data-testid="flow-task-kpi-percent">
                <span class="flow-task__kpi-label">完成率</span>
                <span class="flow-task__kpi-value mono">{{ selectedTaskProgress }}%</span>
                <span class="flow-task__kpi-hint">实际 / 预计</span>
              </article>
              <article
                class="flow-task__kpi"
                :class="selectedTask.discrepancyType && selectedTask.discrepancyType !== 'NONE' && 'flow-task__kpi--danger'"
                data-testid="flow-task-kpi-discrepancy"
              >
                <span class="flow-task__kpi-label">差异</span>
                <span class="flow-task__kpi-value flow-task__kpi-value--text">{{ formatDiscrepancy(selectedTask) }}</span>
                <span class="flow-task__kpi-hint">完成时若不一致需填差异原因</span>
              </article>
            </div>

            <!-- 进度条 + 起终点 -->
            <div class="flow-task__progress-card">
              <div class="flow-task__progress-meta">
                <span class="flow-task__progress-label">
                  {{ selectedTask.actualQuantity || 0 }}/{{ selectedTask.expectedQuantity || 0 }} 已扫，剩余 {{ selectedTask.remainingQuantity || 0 }}
                </span>
                <span class="mono flow-task__progress-percent">{{ selectedTaskProgress }}%</span>
              </div>
              <div class="flow-task__progress-track">
                <span :style="{ width: selectedTaskProgress + '%' }"></span>
              </div>
              <p
                v-if="selectedTask.discrepancyReason"
                class="flow-task__discrepancy-reason"
              >
                差异原因：{{ selectedTask.discrepancyReason }}
              </p>
            </div>

            <!-- 连续扫码 form -->
            <div class="flow-task__scan-grid">
              <form
                class="flow-task__scan-form"
                data-test="flow-task-scan-form"
                @submit.prevent="submitManualScan"
              >
                <label class="flow-task__label" for="flow-task-scan-input">扫码 / 输入溯源码</label>
                <div class="flow-task__scan-row">
                  <input
                    id="flow-task-scan-input"
                    v-model="scanInput"
                    :disabled="!isOpenTask(selectedTask) || scanning"
                    type="text"
                    placeholder="单品码 / 箱码 / 托盘码"
                    class="flow-task__control flow-task__control--mono"
                    data-test="flow-task-scan-input"
                  />
                  <BaseButton
                    type="submit"
                    variant="primary"
                    size="sm"
                    :loading="scanning"
                    :disabled="!isOpenTask(selectedTask) || scanning"
                    data-test="flow-task-scan-submit"
                  >
                    <template #icon><QrCode :size="13" /></template>
                    提交
                  </BaseButton>
                </div>

                <label class="flow-task__label flow-task__label--mt" for="flow-task-scan-remark">扫码备注（可选）</label>
                <input
                  id="flow-task-scan-remark"
                  v-model="scanRemark"
                  maxlength="255"
                  type="text"
                  placeholder="默认由任务号自动生成备注"
                  class="flow-task__control"
                />

                <div
                  class="flow-task__feedback"
                  :class="`flow-task__feedback--${feedbackTone(selectedTask)}`"
                  data-test="flow-task-scan-feedback"
                >
                  <template v-if="selectedTask.scanMessage">
                    <p>{{ selectedTask.scanMessage }}</p>
                    <button
                      v-if="selectedTask.lastScanTraceCode"
                      type="button"
                      class="flow-task__feedback-link"
                      @click="goTraceDetail(selectedTask.lastScanTraceCode)"
                    >
                      <span class="mono">{{ selectedTask.lastScanTraceCode }}</span>
                      <ExternalLink :size="11" />
                    </button>
                  </template>
                  <template v-else>
                    <p>扫码后这里会显示「成功累计」或「重复已扫」反馈。</p>
                  </template>
                </div>
              </form>

              <aside class="flow-task__guide">
                <p class="flow-task__guide-eyebrow">操作提示</p>
                <ul class="flow-task__guide-list">
                  <li>出库任务自动写出库事件，目标节点扫码自动写接收入库事件。</li>
                  <li>扫描箱码或托盘码时会自动展开单品码批量处理。</li>
                  <li>重复扫码会提示「已扫」，不会重复计数。</li>
                  <li>完成时若数量不一致，必须填写差异原因，任务转为异常关闭。</li>
                </ul>
              </aside>
            </div>
          </template>
          <template v-else>
            <div class="flow-task__detail-empty">
              <EmptyState
                :icon="ClipboardList"
                title="请选择一个流转任务"
                subtitle="从左侧任务队列中选择待办任务进入连续扫码，或在表单中创建新任务。"
              />
            </div>
          </template>
        </section>
      </div>
    </section>

    <QRScanner v-if="scannerOpen" @scan="handleTaskScan" @close="scannerOpen = false" />
  </div>
</template>

<style scoped>
.flow-task {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 12px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.flow-task__layout {
  display: grid;
  grid-template-columns: 380px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.flow-task__form,
.flow-task__queue,
.flow-task__detail {
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.flow-task__form {
  position: sticky;
  top: 68px;
}

.flow-task__main {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.flow-task__form-head,
.flow-task__queue-head,
.flow-task__detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--hairline);
}
.flow-task__form-head {
  flex-direction: column;
  gap: 4px;
}

.flow-task__eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.4px;
  color: var(--ink-tertiary);
  text-transform: uppercase;
}
.flow-task__eyebrow svg {
  color: var(--primary);
}
.flow-task__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.3px;
  color: var(--ink);
  word-break: break-all;
}
.flow-task__subtitle {
  margin: 4px 0 0;
  font-size: 12.5px;
  color: var(--ink-subtle);
  line-height: 1.5;
}

.flow-task__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.flow-task__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
}
.flow-task__label--mt {
  margin-top: 12px;
}
.flow-task__required {
  color: var(--error);
  margin-left: 2px;
}

.flow-task__control {
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
.flow-task__control--mono {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
}
.flow-task__control--textarea {
  height: auto;
  padding: 8px 10px;
  resize: vertical;
  line-height: 1.5;
}
.flow-task__control:focus {
  border-color: var(--primary-focus, #5e69d1);
  box-shadow: 0 0 0 3px rgba(94, 106, 210, 0.15);
}
.flow-task__control:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
select.flow-task__control {
  appearance: none;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10' viewBox='0 0 24 24' fill='none' stroke='%2371717a' stroke-width='2'><path d='M6 9l6 6 6-6'/></svg>");
  background-repeat: no-repeat;
  background-position: right 10px center;
  padding-right: 28px;
}

.flow-task__error {
  background: var(--error-soft);
  color: var(--error);
  border: 1px solid #f8c8ca;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 12.5px;
  line-height: 1.45;
}

.flow-task__queue-stats {
  display: flex;
  align-items: center;
  gap: 16px;
}
.flow-task__stat {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}
.flow-task__stat-label {
  font-size: 11px;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: var(--ink-tertiary);
}
.flow-task__stat-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: -0.3px;
}

.flow-task__filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.flow-task__chips {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: var(--surface-2);
  border-radius: 8px;
  padding: 3px;
}
.flow-task__chip {
  height: 26px;
  padding: 0 10px;
  border-radius: 6px;
  background: transparent;
  border: 0;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--ink-subtle);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.flow-task__chip:hover {
  color: var(--ink);
}
.flow-task__chip--active {
  background: var(--surface-1);
  color: var(--ink);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
}
.flow-task__search {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 32px;
  padding: 0 10px;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 8px;
  min-width: 220px;
  flex: 1 1 240px;
}
.flow-task__search svg {
  color: var(--ink-tertiary);
  flex-shrink: 0;
}
.flow-task__search input {
  flex: 1;
  border: 0;
  background: transparent;
  font-size: 13px;
  color: var(--ink);
  outline: none;
}
.flow-task__search input::placeholder {
  color: var(--ink-tertiary);
}

.flow-task__loading,
.flow-task__empty {
  padding: 4px 0;
}

.flow-task__list-wrap {
  overflow-x: auto;
  border: 1px solid var(--hairline);
  border-radius: 8px;
  background: var(--surface-1);
}
.flow-task__list {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.flow-task__list th {
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
.flow-task__list td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--hairline);
  color: var(--ink);
  vertical-align: middle;
}
.flow-task__row {
  cursor: pointer;
  transition: background 0.12s;
}
.flow-task__row:hover td {
  background: var(--surface-2);
}
.flow-task__row--active td {
  background: var(--primary-soft);
}
.flow-task__row:last-child td {
  border-bottom: 0;
}
.flow-task__cell-no {
  color: var(--ink);
  white-space: nowrap;
  font-weight: 500;
}
.flow-task__cell-type {
  color: var(--ink-muted);
  white-space: nowrap;
}
.flow-task__cell-route {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--ink-muted);
  font-size: 12.5px;
}
.flow-task__arrow {
  color: var(--ink-tertiary);
  flex-shrink: 0;
}
.flow-task__cell-progress {
  width: 200px;
}
.flow-task__progress-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.flow-task__progress-text {
  font-size: 12px;
  color: var(--ink-muted);
  white-space: nowrap;
  min-width: 40px;
}
.flow-task__progress-bar {
  flex: 1;
  height: 4px;
  border-radius: 9999px;
  background: var(--surface-2);
  overflow: hidden;
}
.flow-task__progress-bar span {
  display: block;
  height: 100%;
  background: var(--primary);
  transition: width 0.2s;
}

.flow-task__detail-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.flow-task__kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.flow-task__kpi {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 16px;
  border: 1px solid var(--hairline);
  border-radius: 8px;
  background: var(--surface-1);
}
.flow-task__kpi--danger {
  border-color: #f8c8ca;
  background: var(--error-soft);
}
.flow-task__kpi-label {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.4px;
  color: var(--ink-tertiary);
  text-transform: uppercase;
}
.flow-task__kpi-value {
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -0.4px;
  color: var(--ink);
  line-height: 1.1;
}
.flow-task__kpi-value--text {
  font-size: 16px;
  letter-spacing: -0.2px;
}
.flow-task__kpi-divider {
  color: var(--ink-tertiary);
  margin: 0 2px;
  font-weight: 500;
}
.flow-task__kpi-hint {
  font-size: 11.5px;
  color: var(--ink-subtle);
  line-height: 1.35;
}

.flow-task__progress-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 16px;
  border: 1px solid var(--hairline);
  border-radius: 8px;
  background: var(--surface-1);
}
.flow-task__progress-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12.5px;
  color: var(--ink-muted);
}
.flow-task__progress-percent {
  color: var(--ink);
  font-weight: 500;
}
.flow-task__progress-track {
  height: 6px;
  border-radius: 9999px;
  background: var(--surface-2);
  overflow: hidden;
}
.flow-task__progress-track span {
  display: block;
  height: 100%;
  background: var(--primary);
  transition: width 0.2s;
}
.flow-task__discrepancy-reason {
  margin: 0;
  padding: 8px 12px;
  border-radius: 6px;
  background: var(--error-soft);
  color: var(--error);
  font-size: 12.5px;
  line-height: 1.45;
}

.flow-task__scan-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 240px;
  gap: 16px;
  align-items: start;
}
.flow-task__scan-form {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px;
  border: 1px solid var(--hairline);
  border-radius: 8px;
  background: var(--surface-1);
}
.flow-task__scan-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.flow-task__feedback {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 6px;
  border: 1px solid var(--hairline);
  background: var(--surface-2);
  color: var(--ink-muted);
  font-size: 12.5px;
  line-height: 1.5;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.flow-task__feedback--success {
  background: var(--success-soft);
  border-color: #bef0c7;
  color: var(--success);
}
.flow-task__feedback--warn {
  background: var(--warn-soft);
  border-color: #fcd9b6;
  color: var(--warn);
}
.flow-task__feedback p {
  margin: 0;
}
.flow-task__feedback-link {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: transparent;
  border: 0;
  font-size: 12px;
  font-weight: 500;
  color: inherit;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.flow-task__guide {
  border: 1px solid var(--hairline);
  border-radius: 8px;
  padding: 16px;
  background: var(--surface-1);
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.flow-task__guide-eyebrow {
  margin: 0;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.4px;
  color: var(--ink-tertiary);
  text-transform: uppercase;
}
.flow-task__guide-list {
  margin: 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12.5px;
  color: var(--ink-muted);
  line-height: 1.5;
}

.flow-task__detail-empty {
  padding: 4px 0;
}

.mono {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
}

@media (max-width: 1023.98px) {
  .flow-task__layout {
    grid-template-columns: minmax(0, 1fr);
  }
  .flow-task__form {
    position: static;
  }
  .flow-task__scan-grid {
    grid-template-columns: minmax(0, 1fr);
  }
  .flow-task__kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .flow-task__cell-progress {
    width: 160px;
  }
}

@media (max-width: 639.98px) {
  .flow-task {
    padding: 16px 8px;
  }
  .flow-task__form,
  .flow-task__queue,
  .flow-task__detail {
    padding: 16px;
    border-radius: 10px;
  }
  .flow-task__queue-head,
  .flow-task__detail-head {
    flex-direction: column;
    align-items: flex-start;
  }
  .flow-task__queue-stats {
    width: 100%;
    justify-content: space-between;
  }
  .flow-task__stat {
    align-items: flex-start;
  }
  .flow-task__filter-row {
    flex-direction: column;
    align-items: stretch;
  }
  .flow-task__search {
    min-width: 0;
  }
  .flow-task__kpi-grid {
    grid-template-columns: minmax(0, 1fr);
  }
  .flow-task__list th:nth-child(2),
  .flow-task__list td:nth-child(2) {
    display: none;
  }
  .flow-task__cell-progress {
    width: 120px;
  }
  .flow-task__detail-actions {
    width: 100%;
  }
}
</style>
