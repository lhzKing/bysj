import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { renderWithPrime } from '@/test/renderWithPrime'
import TraceCorrectionDialog from '@/features/trace/components/TraceCorrectionDialog.vue'

const createTraceCorrectionMock = vi.fn()
const toastErrorMock = vi.fn()
const toastSuccessMock = vi.fn()

vi.mock('@/features/trace/api', () => ({
  createTraceCorrection: (...args) => createTraceCorrectionMock(...args)
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({
    success: (...args) => toastSuccessMock(...args),
    error: (...args) => toastErrorMock(...args)
  })
}))

vi.mock('@/shared/utils/logger', () => ({
  logger: { error: vi.fn() }
}))

function mountDialog() {
  return renderWithPrime(TraceCorrectionDialog, {
    props: {
      modelValue: true,
      traceCode: 'TRACE-001'
    },
    global: {
      stubs: {
        teleport: true,
        FilePenLine: true,
        X: true
      }
    }
  })
}

describe('TraceCorrectionDialog contract', () => {
  beforeEach(() => {
    createTraceCorrectionMock.mockReset()
    createTraceCorrectionMock.mockResolvedValue({})
    toastErrorMock.mockReset()
    toastSuccessMock.mockReset()
  })

  it('submits correction payload with original log id and reason', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    const { formData, handleSubmit } = wrapper.vm.$.setupState
    formData.correctionOf = '18'
    formData.remark = '更正错误节点'
    formData.fromNode = '节点A'
    formData.toNode = '节点B'
    formData.eventTime = '2026-05-07T12:00'

    await handleSubmit()
    await flushPromises()

    expect(createTraceCorrectionMock).toHaveBeenCalledWith('TRACE-001', {
      correctionOf: 18,
      remark: '更正错误节点',
      fromNode: '节点A',
      toNode: '节点B',
      eventTime: '2026-05-07T12:00:00'
    })
    expect(wrapper.emitted('success')).toBeTruthy()
  })

  it('blocks missing correctionOf', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    const { formData, handleSubmit } = wrapper.vm.$.setupState
    formData.correctionOf = ''
    formData.remark = '更正错误节点'

    await handleSubmit()

    expect(createTraceCorrectionMock).not.toHaveBeenCalled()
    expect(toastErrorMock).toHaveBeenCalledWith('请输入有效的原始日志 ID')
  })
})
