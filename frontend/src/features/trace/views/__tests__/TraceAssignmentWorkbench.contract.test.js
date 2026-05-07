import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { renderWithPrime } from '@/test/renderWithPrime'
import TraceAssignmentWorkbench from '@/features/trace/views/TraceAssignmentWorkbench.vue'

const pushMock = vi.fn()
const confirmMock = vi.fn()
const toastSuccessMock = vi.fn()
const toastErrorMock = vi.fn()
const getPartsMock = vi.fn()
const getTraceNodesMock = vi.fn()
const createTraceMock = vi.fn()
const getTraceBatchMock = vi.fn()
const getTraceBatchCodesMock = vi.fn()
const printTraceCodeMock = vi.fn()
const reprintTraceCodeMock = vi.fn()
const voidTraceCodeMock = vi.fn()
const activateTraceCodeMock = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock })
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({ success: toastSuccessMock, error: toastErrorMock })
}))

vi.mock('@/shared/composables/useConfirm', () => ({
  useConfirm: () => ({ confirm: confirmMock })
}))

vi.mock('@/features/part/api', () => ({
  getParts: (...args) => getPartsMock(...args)
}))

vi.mock('@/features/trace/api', () => ({
  getTraceNodes: (...args) => getTraceNodesMock(...args),
  createTrace: (...args) => createTraceMock(...args),
  getTraceBatch: (...args) => getTraceBatchMock(...args),
  getTraceBatchCodes: (...args) => getTraceBatchCodesMock(...args),
  printTraceCode: (...args) => printTraceCodeMock(...args),
  reprintTraceCode: (...args) => reprintTraceCodeMock(...args),
  voidTraceCode: (...args) => voidTraceCodeMock(...args),
  activateTraceCode: (...args) => activateTraceCodeMock(...args)
}))

const qrScannerStub = {
  template: `
    <div data-test="qr-scanner-stub">
      <button data-test="scan-code" @click="$emit('scan', 'TRACE-001')">scan</button>
      <button data-test="scan-unknown-code" @click="$emit('scan', 'TRACE-404')">scan-unknown</button>
    </div>
  `
}

function mountWorkbench() {
  return renderWithPrime(TraceAssignmentWorkbench, {
    global: {
      stubs: {
        QRScanner: qrScannerStub
      }
    }
  })
}

function mockBatch({ codes = null } = {}) {
  getTraceBatchMock.mockResolvedValue({
    batchId: 9,
    batchNo: 'ASSIGN-009',
    quantityRequested: 2,
    quantityGenerated: 2,
    quantityPrinted: 0,
    quantityActivated: 0,
    quantityInbound: 0,
    quantityVoided: 0,
    printOperationCount: 0,
    consistent: false,
    reconciliationStatus: 'MISMATCH',
    discrepancyReasons: ['已打印数量 + 作废数量未覆盖实际生成数量']
  })
  getTraceBatchCodesMock.mockResolvedValue(codes || [
    { batchId: 9, traceCode: 'TRACE-001', serialNo: 1, codeStatus: 'GENERATED', printCount: 0 },
    { batchId: 9, traceCode: 'TRACE-002', serialNo: 2, codeStatus: 'GENERATED', printCount: 0 }
  ])
}

describe('TraceAssignmentWorkbench', () => {
  beforeEach(() => {
    pushMock.mockReset()
    confirmMock.mockReset()
    confirmMock.mockResolvedValue(true)
    toastSuccessMock.mockReset()
    toastErrorMock.mockReset()
    getPartsMock.mockReset()
    getTraceNodesMock.mockReset()
    createTraceMock.mockReset()
    getTraceBatchMock.mockReset()
    getTraceBatchCodesMock.mockReset()
    printTraceCodeMock.mockReset()
    reprintTraceCodeMock.mockReset()
    voidTraceCodeMock.mockReset()
    activateTraceCodeMock.mockReset()

    getPartsMock.mockResolvedValue({ list: [{ id: 1, partCode: 'P-001', partName: '轴承' }] })
    getTraceNodesMock.mockResolvedValue([
      { id: 7, nodeCode: 'BJ_FACTORY', nodeName: '北京工厂', province: '北京', city: '北京市' }
    ])
    mockBatch()
  })

  it('creates an assignment batch and loads generated codes for print/activation', async () => {
    createTraceMock.mockResolvedValue({
      batchId: 9,
      batchNo: 'ASSIGN-009',
      traceCodes: ['TRACE-001', 'TRACE-002']
    })

    const wrapper = mountWorkbench()
    await flushPromises()

    await wrapper.find('[data-test="assignment-part-select"]').setValue('P-001')
    await wrapper.find('[data-test="assignment-quantity-input"]').setValue('2')
    await wrapper.find('[data-test="assignment-node-select"]').setValue('7')
    await wrapper.find('[data-test="assignment-create-form"]').trigger('submit')
    await flushPromises()

    expect(createTraceMock).toHaveBeenCalledWith(expect.objectContaining({
      partCode: 'P-001',
      quantity: 2,
      manufacturerNodeId: 7,
      manufacturerNode: '北京工厂',
      province: '北京',
      city: '北京市'
    }))
    expect(getTraceBatchMock).toHaveBeenCalledWith(9)
    expect(getTraceBatchCodesMock).toHaveBeenCalledWith(9)
    expect(wrapper.text()).toContain('TRACE-001')
    expect(wrapper.text()).toContain('对账状态：MISMATCH')
  })

  it('prints a single generated code and refreshes batch detail', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()

    await wrapper.find('[data-test="assignment-lookup-input"]').setValue('9')
    await wrapper.find('[data-test="assignment-lookup-submit"]').trigger('click')
    await flushPromises()

    await wrapper.find('[data-test="assignment-code-row-TRACE-001"] button').trigger('click')
    await flushPromises()

    expect(printTraceCodeMock).toHaveBeenCalledWith('TRACE-001', { remark: '生产工作台打印标签' })
    expect(getTraceBatchMock).toHaveBeenCalledTimes(2)
  })

  it('scan-activates only codes that belong to the current batch', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()

    await wrapper.find('[data-test="assignment-lookup-input"]').setValue('9')
    await wrapper.find('[data-test="assignment-lookup-submit"]').trigger('click')
    await flushPromises()

    await wrapper.find('[data-test="assignment-scan-activate"]').trigger('click')
    await wrapper.find('[data-test="scan-code"]').trigger('click')
    await flushPromises()

    expect(activateTraceCodeMock).toHaveBeenCalledWith('TRACE-001', expect.objectContaining({
      remark: '生产工作台扫码激活'
    }))
  })

  it('rejects scanned codes outside the loaded assignment batch before activation api call', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()

    await wrapper.find('[data-test="assignment-lookup-input"]').setValue('9')
    await wrapper.find('[data-test="assignment-lookup-submit"]').trigger('click')
    await flushPromises()

    await wrapper.find('[data-test="assignment-scan-activate"]').trigger('click')
    await wrapper.find('[data-test="scan-unknown-code"]').trigger('click')
    await flushPromises()

    expect(activateTraceCodeMock).not.toHaveBeenCalled()
    expect(toastErrorMock).toHaveBeenCalledWith('扫码结果不属于当前赋码批次')
  })
})
