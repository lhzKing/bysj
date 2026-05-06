import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import { flushPromises } from '@vue/test-utils'
import { renderWithPrime } from '@/test/renderWithPrime'
import ScanFlowDialog from '@/features/trace/components/ScanFlowDialog.vue'
import { REGIONS } from '@/shared/data/regions'

const createEventMock = vi.fn()
const messageErrorMock = vi.fn()
const messageSuccessMock = vi.fn()

vi.mock('@/features/trace/api', () => ({
  createEvent: (...args) => createEventMock(...args)
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({
    success: (...args) => messageSuccessMock(...args),
    error: (...args) => messageErrorMock(...args)
  })
}))

const baseButtonStub = {
  name: 'BaseButton',
  props: ['disabled', 'variant'],
  template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>'
}

describe('ScanFlowDialog contract', () => {
  beforeEach(() => {
    createEventMock.mockReset()
    messageErrorMock.mockReset()
    messageSuccessMock.mockReset()
    createEventMock.mockResolvedValue({})
  })

  it('submits camelCase event payload including remark', async () => {
    const wrapper = renderWithPrime(ScanFlowDialog, {
      props: {
        modelValue: true,
        traceCode: 'TRACE-001',
        actionType: 'inbound'
      },
      global: {
        stubs: {
          LogOut: true,
          X: true,
          teleport: true,
          BaseButton: baseButtonStub,
          QrCode: true,
          Info: true,
          Loader: true
        }
      }
    })

    await flushPromises()
    await nextTick()

    const { formData, handleSubmit } = wrapper.vm.$.setupState
    const province = REGIONS[0].value
    const city = REGIONS[0].cities[0]
    formData.fromNode = 'Source Node'
    formData.toNode = 'Target Node'
    formData.province = province
    formData.city = city
    formData.eventTime = '2026-04-12T00:00'
    formData.remark = 'remark-1'
    await nextTick()

    await handleSubmit()
    await flushPromises()

    expect(createEventMock).toHaveBeenCalledWith(
      'TRACE-001',
      expect.objectContaining({
        actionType: 'INBOUND',
        fromNode: 'Source Node',
        toNode: 'Target Node',
        province,
        city,
        eventTime: '2026-04-12T00:00:00',
        remark: 'remark-1'
      })
    )
  })
})
