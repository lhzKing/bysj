import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import TraceFlowTaskWorkbench from '@/features/trace/views/TraceFlowTaskWorkbench.vue'

const {
  routerPushMock,
  toastSuccess,
  toastError,
  toastWarning,
  confirmMock,
  promptMock,
  getTraceNodesMock,
  getTraceFlowTasksMock,
  getTraceFlowTaskMock,
  createTraceFlowTaskMock,
  scanTraceFlowTaskMock,
  completeTraceFlowTaskMock
} = vi.hoisted(() => ({
  routerPushMock: vi.fn(),
  toastSuccess: vi.fn(),
  toastError: vi.fn(),
  toastWarning: vi.fn(),
  confirmMock: vi.fn(),
  promptMock: vi.fn(),
  getTraceNodesMock: vi.fn(),
  getTraceFlowTasksMock: vi.fn(),
  getTraceFlowTaskMock: vi.fn(),
  createTraceFlowTaskMock: vi.fn(),
  scanTraceFlowTaskMock: vi.fn(),
  completeTraceFlowTaskMock: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPushMock })
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({ success: toastSuccess, error: toastError, warning: toastWarning, info: vi.fn() })
}))

vi.mock('@/shared/composables/useConfirm', () => ({
  useConfirm: () => ({ confirm: confirmMock })
}))

vi.mock('@/shared/composables/usePrompt', () => ({
  usePrompt: () => ({ prompt: promptMock })
}))

vi.mock('@/shared/utils/logger', () => ({
  logger: { error: vi.fn(), warn: vi.fn(), info: vi.fn() }
}))

vi.mock('@/features/trace/api', () => ({
  getTraceNodes: getTraceNodesMock,
  getTraceFlowTasks: getTraceFlowTasksMock,
  getTraceFlowTask: getTraceFlowTaskMock,
  createTraceFlowTask: createTraceFlowTaskMock,
  scanTraceFlowTask: scanTraceFlowTaskMock,
  completeTraceFlowTask: completeTraceFlowTaskMock
}))

const qrScannerStub = {
  template: `<div data-test="qr-scanner-stub"></div>`
}

const nodes = [
  { id: 1, nodeCode: 'BJ_WH', nodeName: '北京仓库' },
  { id: 2, nodeCode: 'SH_WH', nodeName: '上海仓库' }
]

function buildTask(overrides = {}) {
  return {
    id: 18,
    taskNo: 'SHIP-018',
    taskType: 'OUTBOUND',
    taskTypeLabel: '出库任务',
    status: 'PROCESSING',
    statusLabel: '处理中',
    sourceNodeId: 1,
    sourceNodeName: '北京仓库',
    targetNodeId: 2,
    targetNodeName: '上海仓库',
    expectedQuantity: 2,
    actualQuantity: 1,
    remainingQuantity: 1,
    discrepancyType: 'NONE',
    discrepancyTypeLabel: '无差异',
    ...overrides
  }
}

function mountWorkbench() {
  return mount(TraceFlowTaskWorkbench, {
    global: {
      stubs: { QRScanner: qrScannerStub }
    }
  })
}

beforeEach(() => {
  routerPushMock.mockReset()
  toastSuccess.mockReset()
  toastError.mockReset()
  toastWarning.mockReset()
  confirmMock.mockReset()
  confirmMock.mockResolvedValue(true)
  promptMock.mockReset()
  promptMock.mockResolvedValue('少件待补扫')
  getTraceNodesMock.mockReset()
  getTraceFlowTasksMock.mockReset()
  getTraceFlowTaskMock.mockReset()
  createTraceFlowTaskMock.mockReset()
  scanTraceFlowTaskMock.mockReset()
  completeTraceFlowTaskMock.mockReset()

  getTraceNodesMock.mockResolvedValue(nodes)
  getTraceFlowTasksMock.mockImplementation(({ status }) => {
    if (status === 'CREATED') return Promise.resolve([buildTask({ id: 17, taskNo: 'SHIP-017', status: 'CREATED', actualQuantity: 0, remainingQuantity: 2 })])
    if (status === 'PROCESSING') return Promise.resolve([buildTask()])
    if (status === 'COMPLETED') return Promise.resolve([])
    if (status === 'EXCEPTION') return Promise.resolve([])
    return Promise.resolve([])
  })
  getTraceFlowTaskMock.mockResolvedValue(buildTask())
})

afterEach(() => {
  document.body.style.overflow = ''
})

describe('TraceFlowTaskWorkbench (Linear shell)', () => {
  it('renders PageHeader with Linear title and refresh / complete actions', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()

    const header = wrapper.find('[data-testid="flow-task-page-header"]')
    expect(header.exists()).toBe(true)
    expect(header.text()).toContain('仓库/物流任务工作台')

    expect(wrapper.find('[data-test="flow-task-refresh"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="flow-task-complete"]').exists()).toBe(true)
  })

  it('renders dense task list rows with status pill, type and progress', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()

    const list = wrapper.find('[data-test="flow-task-list"]')
    expect(list.exists()).toBe(true)

    const row = wrapper.find('[data-test="flow-task-row-SHIP-018"]')
    expect(row.exists()).toBe(true)
    expect(row.text()).toContain('SHIP-018')
    expect(row.text()).toContain('1/2')
    expect(row.text()).toContain('处理中')
    expect(row.text()).toContain('北京仓库')
    expect(row.text()).toContain('上海仓库')
  })

  it('renders 4-up KPI grid with mono numerics for the selected task', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()

    const actual = wrapper.find('[data-testid="flow-task-kpi-actual"]')
    const remaining = wrapper.find('[data-testid="flow-task-kpi-remaining"]')
    const percent = wrapper.find('[data-testid="flow-task-kpi-percent"]')
    const discrepancy = wrapper.find('[data-testid="flow-task-kpi-discrepancy"]')

    expect(actual.exists()).toBe(true)
    expect(actual.text()).toContain('1')
    expect(actual.text()).toContain('2')
    expect(remaining.text()).toContain('1')
    expect(percent.text()).toContain('50%')
    expect(discrepancy.text()).toContain('无差异')
  })

  it('shows duplicate scan feedback with warn tone when duplicateScan=true', async () => {
    scanTraceFlowTaskMock.mockResolvedValue(buildTask({
      duplicateScan: true,
      lastScanTraceCode: 'TRACE-001',
      scanMessage: '该码已在当前任务内扫码，不重复计数'
    }))
    const wrapper = mountWorkbench()
    await flushPromises()

    await wrapper.find('[data-test="flow-task-scan-input"]').setValue('TRACE-001')
    await wrapper.find('[data-test="flow-task-scan-form"]').trigger('submit')
    await flushPromises()

    expect(scanTraceFlowTaskMock).toHaveBeenCalledWith(18, expect.objectContaining({ traceCode: 'TRACE-001' }))
    const calledPayload = scanTraceFlowTaskMock.mock.calls[0][1]
    expect(calledPayload).not.toHaveProperty('fromNode')
    expect(calledPayload).not.toHaveProperty('toNode')
    expect(toastWarning).toHaveBeenCalledWith('该码已在当前任务内扫码，不重复计数')
    const feedback = wrapper.find('[data-test="flow-task-scan-feedback"]')
    expect(feedback.exists()).toBe(true)
    expect(feedback.classes()).toContain('flow-task__feedback--warn')
    expect(feedback.text()).toContain('该码已在当前任务内扫码，不重复计数')
    expect(feedback.text()).toContain('TRACE-001')
  })

  it('renders batch scan success message when backend returns batch_scan=true', async () => {
    scanTraceFlowTaskMock.mockResolvedValue(buildTask({
      actualQuantity: 2,
      remainingQuantity: 0,
      batchScan: true,
      batchParentCode: 'CARTON-001',
      batchExpandedQuantity: 2,
      batchCreatedQuantity: 2,
      duplicateScan: false,
      lastScanTraceCode: 'CARTON-001',
      scanMessage: '父码 CARTON-001 展开 2 个单品码，新增 2 个，重复 0 个，跳过 0 个，本次累计 2 件'
    }))
    const wrapper = mountWorkbench()
    await flushPromises()

    await wrapper.find('[data-test="flow-task-scan-input"]').setValue('CARTON-001')
    await wrapper.find('[data-test="flow-task-scan-form"]').trigger('submit')
    await flushPromises()

    expect(toastSuccess).toHaveBeenCalledWith(expect.stringContaining('父码 CARTON-001 展开 2 个单品码'))
    const feedback = wrapper.find('[data-test="flow-task-scan-feedback"]')
    expect(feedback.classes()).toContain('flow-task__feedback--success')
    expect(feedback.text()).toContain('父码 CARTON-001 展开 2 个单品码')
  })

  it('uses usePrompt to capture discrepancy reason and closes task as exception', async () => {
    completeTraceFlowTaskMock.mockResolvedValue(buildTask({
      status: 'EXCEPTION',
      discrepancyType: 'SHORTAGE',
      discrepancyReason: '少件待补扫'
    }))
    const wrapper = mountWorkbench()
    await flushPromises()

    await wrapper.find('[data-test="flow-task-complete"]').trigger('click')
    await flushPromises()

    expect(promptMock).toHaveBeenCalledTimes(1)
    expect(promptMock.mock.calls[0][0]).toEqual(expect.objectContaining({ title: '填写少扫差异原因' }))
    expect(completeTraceFlowTaskMock).toHaveBeenCalledWith(18, { discrepancyReason: '少件待补扫' })
    expect(toastSuccess).toHaveBeenCalledWith('任务已按异常差异关闭')
  })

  it('cancels completion when prompt resolves to null and never calls completeTraceFlowTask', async () => {
    promptMock.mockResolvedValueOnce(null)
    const wrapper = mountWorkbench()
    await flushPromises()

    await wrapper.find('[data-test="flow-task-complete"]').trigger('click')
    await flushPromises()

    expect(completeTraceFlowTaskMock).not.toHaveBeenCalled()
  })

  it('routes to trace detail when clicking lastScanTraceCode link in feedback', async () => {
    scanTraceFlowTaskMock.mockResolvedValue(buildTask({
      lastScanTraceCode: 'TRACE-777',
      scanMessage: '扫码成功'
    }))
    const wrapper = mountWorkbench()
    await flushPromises()

    await wrapper.find('[data-test="flow-task-scan-input"]').setValue('TRACE-777')
    await wrapper.find('[data-test="flow-task-scan-form"]').trigger('submit')
    await flushPromises()

    const link = wrapper.find('[data-test="flow-task-scan-feedback"] button')
    expect(link.exists()).toBe(true)
    await link.trigger('click')
    expect(routerPushMock).toHaveBeenCalledWith('/traces/TRACE-777')
  })

  it('switches status filter and reloads tasks for the selected status', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()
    getTraceFlowTasksMock.mockClear()

    await wrapper.find('[data-test="flow-task-filter-COMPLETED"]').trigger('click')
    await flushPromises()

    expect(getTraceFlowTasksMock).toHaveBeenCalledWith({ status: 'COMPLETED' })
    expect(wrapper.find('[data-test="flow-task-empty"]').exists()).toBe(true)
  })
})
