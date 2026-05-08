import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import TraceAssignmentWorkbench from '@/features/trace/views/TraceAssignmentWorkbench.vue'

const {
  routerPushMock,
  toastSuccess,
  toastError,
  confirmMock,
  promptMock,
  getPartsMock,
  getTraceNodesMock,
  createTraceMock,
  getTraceBatchMock,
  getTraceBatchCodesMock,
  printTraceCodeMock,
  reprintTraceCodeMock,
  voidTraceCodeMock,
  activateTraceCodeMock
} = vi.hoisted(() => ({
  routerPushMock: vi.fn(),
  toastSuccess: vi.fn(),
  toastError: vi.fn(),
  confirmMock: vi.fn(),
  promptMock: vi.fn(),
  getPartsMock: vi.fn(),
  getTraceNodesMock: vi.fn(),
  createTraceMock: vi.fn(),
  getTraceBatchMock: vi.fn(),
  getTraceBatchCodesMock: vi.fn(),
  printTraceCodeMock: vi.fn(),
  reprintTraceCodeMock: vi.fn(),
  voidTraceCodeMock: vi.fn(),
  activateTraceCodeMock: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPushMock })
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({ success: toastSuccess, error: toastError, info: vi.fn(), warning: vi.fn() })
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

vi.mock('@/features/part/api', () => ({
  getParts: getPartsMock
}))

vi.mock('@/features/trace/api', () => ({
  getTraceNodes: getTraceNodesMock,
  createTrace: createTraceMock,
  getTraceBatch: getTraceBatchMock,
  getTraceBatchCodes: getTraceBatchCodesMock,
  printTraceCode: printTraceCodeMock,
  reprintTraceCode: reprintTraceCodeMock,
  voidTraceCode: voidTraceCodeMock,
  activateTraceCode: activateTraceCodeMock
}))

const qrScannerStub = {
  template: `<div data-test="qr-scanner-stub"></div>`
}

function mountWorkbench() {
  return mount(TraceAssignmentWorkbench, {
    global: {
      stubs: { QRScanner: qrScannerStub }
    }
  })
}

function defaultBatch() {
  getTraceBatchMock.mockResolvedValue({
    batchId: 9,
    batchNo: 'ASSIGN-009',
    quantityRequested: 2,
    quantityGenerated: 2,
    quantityPrinted: 1,
    quantityActivated: 0,
    quantityInbound: 0,
    quantityVoided: 0,
    printOperationCount: 1,
    consistent: false,
    reconciliationStatus: 'DISCREPANCY',
    discrepancyReasons: ['激活数量少于生成数量']
  })
  getTraceBatchCodesMock.mockResolvedValue([
    { batchId: 9, traceCode: 'TRACE-001', serialNo: 1, codeStatus: 'PRINTED', printCount: 1 },
    { batchId: 9, traceCode: 'TRACE-002', serialNo: 2, codeStatus: 'GENERATED', printCount: 0 }
  ])
}

beforeEach(() => {
  routerPushMock.mockReset()
  toastSuccess.mockReset()
  toastError.mockReset()
  confirmMock.mockReset()
  confirmMock.mockResolvedValue(true)
  promptMock.mockReset()
  promptMock.mockResolvedValue('测试原因')
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
  defaultBatch()
})

afterEach(() => {
  document.body.style.overflow = ''
})

describe('TraceAssignmentWorkbench (Linear shell)', () => {
  it('renders page header with default subtitle when no batch is loaded', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()

    const header = wrapper.find('[data-testid="assignment-page-header"]')
    expect(header.exists()).toBe(true)
    expect(header.text()).toContain('生产赋码工作台')
    expect(header.text()).toContain('按 SPU 创建赋码批次')

    expect(wrapper.find('[data-test="assignment-recon-empty"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="assignment-batch-print"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-test="assignment-scan-activate"]').attributes('disabled')).toBeDefined()
  })

  it('renders 6-up reconciliation cards with snake_case API field mapping', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()

    await wrapper.find('[data-test="assignment-lookup-input"]').setValue('9')
    await wrapper.find('[data-test="assignment-lookup-submit"]').trigger('click')
    await flushPromises()

    const keys = ['requested', 'generated', 'printed', 'activated', 'inbound', 'voided']
    for (const key of keys) {
      const card = wrapper.find(`[data-testid="assignment-recon-card-${key}"]`)
      expect(card.exists()).toBe(true)
    }
    expect(wrapper.find('[data-testid="assignment-recon-card-requested"]').text()).toContain('2')
    expect(wrapper.find('[data-testid="assignment-recon-card-printed"]').text()).toContain('1')
    expect(wrapper.find('[data-testid="assignment-recon-card-activated"]').text()).toContain('0')

    const status = wrapper.find('[data-test="assignment-reconciliation-status"]')
    expect(status.exists()).toBe(true)
    expect(status.text()).toContain('对账状态：DISCREPANCY')
    expect(status.text()).toContain('激活数量少于生成数量')
  })

  it('renders dense table with status pill, print count and 4 action buttons per row', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()

    await wrapper.find('[data-test="assignment-lookup-input"]').setValue('9')
    await wrapper.find('[data-test="assignment-lookup-submit"]').trigger('click')
    await flushPromises()

    const row = wrapper.find('[data-test="assignment-code-row-TRACE-001"]')
    expect(row.exists()).toBe(true)
    expect(row.text()).toContain('TRACE-001')
    expect(row.text()).toContain('已打印')

    expect(wrapper.find('[data-testid="assignment-action-print-TRACE-001"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="assignment-action-reprint-TRACE-001"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="assignment-action-activate-TRACE-001"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="assignment-action-void-TRACE-001"]').exists()).toBe(true)
  })

  it('reprint flow uses usePrompt instead of window.prompt to capture reason', async () => {
    promptMock.mockResolvedValueOnce('标签丢失')
    const wrapper = mountWorkbench()
    await flushPromises()

    await wrapper.find('[data-test="assignment-lookup-input"]').setValue('9')
    await wrapper.find('[data-test="assignment-lookup-submit"]').trigger('click')
    await flushPromises()

    await wrapper.find('[data-testid="assignment-action-reprint-TRACE-001"]').trigger('click')
    await flushPromises()

    expect(promptMock).toHaveBeenCalledTimes(1)
    expect(promptMock.mock.calls[0][0]).toEqual(expect.objectContaining({ title: '重打标签' }))
    expect(reprintTraceCodeMock).toHaveBeenCalledWith('TRACE-001', { remark: '标签丢失' })
  })

  it('void flow opens confirm dialog with danger type then prompts for reason', async () => {
    promptMock.mockResolvedValueOnce('包装破损')
    const wrapper = mountWorkbench()
    await flushPromises()

    await wrapper.find('[data-test="assignment-lookup-input"]').setValue('9')
    await wrapper.find('[data-test="assignment-lookup-submit"]').trigger('click')
    await flushPromises()

    await wrapper.find('[data-testid="assignment-action-void-TRACE-002"]').trigger('click')
    await flushPromises()

    expect(confirmMock).toHaveBeenCalledTimes(1)
    expect(confirmMock.mock.calls[0][0]).toEqual(expect.objectContaining({
      title: '作废标签',
      type: 'danger'
    }))
    expect(promptMock).toHaveBeenCalledTimes(1)
    expect(voidTraceCodeMock).toHaveBeenCalledWith('TRACE-002', { remark: '包装破损' })
  })

  it('cancels void when prompt resolves to null and never calls voidTraceCode', async () => {
    promptMock.mockResolvedValueOnce(null)
    const wrapper = mountWorkbench()
    await flushPromises()

    await wrapper.find('[data-test="assignment-lookup-input"]').setValue('9')
    await wrapper.find('[data-test="assignment-lookup-submit"]').trigger('click')
    await flushPromises()

    await wrapper.find('[data-testid="assignment-action-void-TRACE-002"]').trigger('click')
    await flushPromises()

    expect(voidTraceCodeMock).not.toHaveBeenCalled()
  })

  it('selecting a row shows it in the active code panel and routes to detail on view', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()

    await wrapper.find('[data-test="assignment-lookup-input"]').setValue('9')
    await wrapper.find('[data-test="assignment-lookup-submit"]').trigger('click')
    await flushPromises()

    await wrapper.find('[data-test="assignment-code-row-TRACE-002"]').trigger('click')
    await flushPromises()

    const panel = wrapper.find('[data-test="assignment-active-code-panel"]')
    expect(panel.exists()).toBe(true)
    expect(panel.text()).toContain('TRACE-002')
    expect(panel.text()).toContain('已生成')

    await wrapper.find('[data-test="assignment-active-go-detail"]').trigger('click')
    expect(routerPushMock).toHaveBeenCalledWith('/traces/TRACE-002')
  })

  it('batch print confirms then iterates over GENERATED-only codes', async () => {
    printTraceCodeMock.mockResolvedValue({})
    const wrapper = mountWorkbench()
    await flushPromises()

    await wrapper.find('[data-test="assignment-lookup-input"]').setValue('9')
    await wrapper.find('[data-test="assignment-lookup-submit"]').trigger('click')
    await flushPromises()

    await wrapper.find('[data-test="assignment-batch-print"]').trigger('click')
    await flushPromises()

    expect(confirmMock).toHaveBeenCalledTimes(1)
    expect(confirmMock.mock.calls[0][0]).toEqual(expect.objectContaining({ title: '批量打印标签' }))
    expect(printTraceCodeMock).toHaveBeenCalledTimes(1)
    expect(printTraceCodeMock).toHaveBeenCalledWith('TRACE-002', { remark: '生产工作台批量打印标签' })
  })
})
