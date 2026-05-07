import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { renderWithPrime } from '@/test/renderWithPrime'
import TraceExceptionCloseDialog from '@/features/trace/components/TraceExceptionCloseDialog.vue'

const closeTraceExceptionMock = vi.fn()
const toastErrorMock = vi.fn()
const toastSuccessMock = vi.fn()

vi.mock('@/features/trace/api', () => ({
  closeTraceException: (...args) => closeTraceExceptionMock(...args)
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
  return renderWithPrime(TraceExceptionCloseDialog, {
    props: {
      modelValue: true,
      traceCode: 'TRACE-001'
    },
    global: {
      stubs: {
        teleport: true,
        ShieldCheck: true,
        X: true
      }
    }
  })
}

describe('TraceExceptionCloseDialog contract', () => {
  beforeEach(() => {
    closeTraceExceptionMock.mockReset()
    closeTraceExceptionMock.mockResolvedValue({})
    toastErrorMock.mockReset()
    toastSuccessMock.mockReset()
  })

  it('submits required remark for EXCEPTION_CLOSE workflow', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    const { formData, handleSubmit } = wrapper.vm.$.setupState
    formData.remark = '复核无误'
    formData.eventTime = '2026-05-07T12:00'

    await handleSubmit()
    await flushPromises()

    expect(closeTraceExceptionMock).toHaveBeenCalledWith('TRACE-001', {
      remark: '复核无误',
      eventTime: '2026-05-07T12:00:00'
    })
    expect(wrapper.emitted('success')).toBeTruthy()
  })

  it('blocks missing remark', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    const { formData, handleSubmit } = wrapper.vm.$.setupState
    formData.remark = '  '

    await handleSubmit()

    expect(closeTraceExceptionMock).not.toHaveBeenCalled()
    expect(toastErrorMock).toHaveBeenCalledWith('请填写解除原因')
  })
})
