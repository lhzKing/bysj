import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { renderWithPrime } from '@/test/renderWithPrime'
import TraceFlowTaskWorkbench from '@/features/trace/views/TraceFlowTaskWorkbench.vue'

const pushMock = vi.fn()
const confirmMock = vi.fn()
const promptMock = vi.fn()
const toastSuccessMock = vi.fn()
const toastErrorMock = vi.fn()
const toastWarningMock = vi.fn()
const getTraceNodesMock = vi.fn()
const getTraceFlowTasksMock = vi.fn()
const getTraceFlowTaskMock = vi.fn()
const createTraceFlowTaskMock = vi.fn()
const scanTraceFlowTaskMock = vi.fn()
const completeTraceFlowTaskMock = vi.fn()

vi.mock('vue-router', () => ({ useRouter: () => ({ push: pushMock }) }))
vi.mock('@/shared/composables/useToast', () => ({ useToast: () => ({ success: toastSuccessMock, error: toastErrorMock, warning: toastWarningMock }) }))
vi.mock('@/shared/composables/useConfirm', () => ({ useConfirm: () => ({ confirm: confirmMock }) }))
vi.mock('@/shared/composables/usePrompt', () => ({ usePrompt: () => ({ prompt: promptMock }) }))
vi.mock('@/features/trace/api', () => ({
  getTraceNodes: (...args) => getTraceNodesMock(...args),
  getTraceFlowTasks: (...args) => getTraceFlowTasksMock(...args),
  getTraceFlowTask: (...args) => getTraceFlowTaskMock(...args),
  createTraceFlowTask: (...args) => createTraceFlowTaskMock(...args),
  scanTraceFlowTask: (...args) => scanTraceFlowTaskMock(...args),
  completeTraceFlowTask: (...args) => completeTraceFlowTaskMock(...args)
}))

const qrScannerStub = { template: `<div data-test="qr-scanner-stub"><button data-test="scan-code" @click="$emit('scan', 'TRACE-002')">scan</button></div>` }

const nodes = [
  { id: 1, nodeCode: 'BJ_WH', nodeName: '北京仓库' },
  { id: 2, nodeCode: 'SH_WH', nodeName: '上海仓库' }
]

function task(overrides = {}) {
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
  return renderWithPrime(TraceFlowTaskWorkbench, { global: { stubs: { QRScanner: qrScannerStub } } })
}

describe('TraceFlowTaskWorkbench', () => {
  beforeEach(() => {
    pushMock.mockReset()
    confirmMock.mockReset()
    promptMock.mockReset()
    toastSuccessMock.mockReset()
    toastErrorMock.mockReset()
    toastWarningMock.mockReset()
    getTraceNodesMock.mockReset()
    getTraceFlowTasksMock.mockReset()
    getTraceFlowTaskMock.mockReset()
    createTraceFlowTaskMock.mockReset()
    scanTraceFlowTaskMock.mockReset()
    completeTraceFlowTaskMock.mockReset()

    confirmMock.mockResolvedValue(true)
    promptMock.mockResolvedValue('少件待补扫')
    getTraceNodesMock.mockResolvedValue(nodes)
    getTraceFlowTasksMock.mockImplementation(({ status }) => {
      if (status === 'CREATED') return Promise.resolve([task({ id: 17, taskNo: 'SHIP-017', status: 'CREATED', actualQuantity: 0, remainingQuantity: 2 })])
      if (status === 'PROCESSING') return Promise.resolve([task()])
      return Promise.resolve([])
    })
    getTraceFlowTaskMock.mockResolvedValue(task())
  })

  it('loads open warehouse/logistics tasks and shows selected task progress', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()

    expect(getTraceFlowTasksMock).toHaveBeenCalledWith({ status: 'CREATED' })
    expect(getTraceFlowTasksMock).toHaveBeenCalledWith({ status: 'PROCESSING' })
    expect(wrapper.text()).toContain('SHIP-018')
    expect(wrapper.text()).toContain('1/2')
    expect(wrapper.find('[data-test="flow-task-scan-form"]').exists()).toBe(true)
  })

  it('creates a flow task with nodes and expected quantity', async () => {
    createTraceFlowTaskMock.mockResolvedValue(task({ id: 20, taskNo: 'SHIP-020', status: 'CREATED', actualQuantity: 0, remainingQuantity: 3, expectedQuantity: 3 }))
    const wrapper = mountWorkbench()
    await flushPromises()

    await wrapper.find('[data-test="flow-task-source-select"]').setValue('1')
    await wrapper.find('[data-test="flow-task-target-select"]').setValue('2')
    await wrapper.find('[data-test="flow-task-expected-input"]').setValue('3')
    await wrapper.find('[data-test="flow-task-create-form"]').trigger('submit')
    await flushPromises()

    expect(createTraceFlowTaskMock).toHaveBeenCalledWith(expect.objectContaining({
      taskType: 'OUTBOUND',
      sourceNodeId: 1,
      targetNodeId: 2,
      expectedQuantity: 3
    }))
    expect(wrapper.text()).toContain('SHIP-020')
  })

  it('scans trace codes continuously and displays duplicate feedback without manual node fields', async () => {
    scanTraceFlowTaskMock.mockResolvedValue(task({
      actualQuantity: 1,
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
    expect(scanTraceFlowTaskMock.mock.calls[0][1]).not.toHaveProperty('fromNode')
    expect(scanTraceFlowTaskMock.mock.calls[0][1]).not.toHaveProperty('toNode')
    expect(toastWarningMock).toHaveBeenCalledWith('该码已在当前任务内扫码，不重复计数')
    expect(wrapper.text()).toContain('该码已在当前任务内扫码，不重复计数')
  })

  it('accepts carton or pallet parent codes and displays backend batch scan feedback', async () => {
    scanTraceFlowTaskMock.mockResolvedValue(task({
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

    expect(scanTraceFlowTaskMock).toHaveBeenCalledWith(18, expect.objectContaining({ traceCode: 'CARTON-001' }))
    expect(toastSuccessMock).toHaveBeenCalledWith(expect.stringContaining('父码 CARTON-001 展开 2 个单品码'))
    expect(wrapper.text()).toContain('父码 CARTON-001 展开 2 个单品码')
  })

  it('requires discrepancy reason when one-click completion has quantity mismatch', async () => {
    completeTraceFlowTaskMock.mockResolvedValue(task({ status: 'EXCEPTION', discrepancyType: 'SHORTAGE', discrepancyReason: '少件待补扫' }))
    const wrapper = mountWorkbench()
    await flushPromises()

    await wrapper.find('[data-test="flow-task-complete"]').trigger('click')
    await flushPromises()

    expect(promptMock).toHaveBeenCalled()
    expect(completeTraceFlowTaskMock).toHaveBeenCalledWith(18, { discrepancyReason: '少件待补扫' })
    expect(toastSuccessMock).toHaveBeenCalledWith('任务已按异常差异关闭')
  })
})
