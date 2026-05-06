import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import { flushPromises } from '@vue/test-utils'
import { renderWithPrime } from '@/test/renderWithPrime'
import ScanExceptionDialog from '@/features/trace/components/ScanExceptionDialog.vue'
import { REGIONS } from '@/shared/data/regions'

const createEventMock = vi.fn()
const messageErrorMock = vi.fn()
const messageSuccessMock = vi.fn()
const loggerErrorMock = vi.fn()

vi.mock('@/features/trace/api', () => ({
  createEvent: (...args) => createEventMock(...args)
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({
    success: (...args) => messageSuccessMock(...args),
    error: (...args) => messageErrorMock(...args)
  })
}))

vi.mock('@/shared/utils/logger', () => ({
  logger: {
    info: vi.fn(),
    warn: vi.fn(),
    error: (...args) => loggerErrorMock(...args)
  }
}))

const baseButtonStub = {
  name: 'BaseButton',
  props: ['disabled', 'variant'],
  template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>'
}

const stubs = {
  AlertTriangle: true,
  X: true,
  Info: true,
  Loader: true,
  teleport: true,
  BaseButton: baseButtonStub
}

function mountDialog() {
  return renderWithPrime(ScanExceptionDialog, {
    props: {
      modelValue: true,
      traceCode: 'TRACE-001'
    },
    global: { stubs }
  })
}

describe('ScanExceptionDialog contract', () => {
  beforeEach(() => {
    createEventMock.mockReset()
    messageErrorMock.mockReset()
    messageSuccessMock.mockReset()
    loggerErrorMock.mockReset()
    createEventMock.mockResolvedValue({})
  })

  it('submits camelCase EXCEPTION payload without fromNode/toNode/correctionOf', async () => {
    const wrapper = mountDialog()
    await flushPromises()
    await nextTick()

    const province = REGIONS[0].value
    const city = REGIONS[0].cities[0]
    const { formData, handleSubmit } = wrapper.vm.$.setupState
    formData.province = province
    formData.city = city
    formData.remark = '外包装破损'
    formData.eventTime = '2026-04-12T00:00'
    await nextTick()

    await handleSubmit()
    await flushPromises()

    expect(createEventMock).toHaveBeenCalledTimes(1)
    expect(createEventMock).toHaveBeenCalledWith('TRACE-001', {
      actionType: 'EXCEPTION',
      province,
      city,
      remark: '外包装破损',
      eventTime: '2026-04-12T00:00:00'
    })

    const payload = createEventMock.mock.calls[0][1]
    expect(payload).not.toHaveProperty('fromNode')
    expect(payload).not.toHaveProperty('toNode')
    expect(payload).not.toHaveProperty('correctionOf')

    expect(wrapper.emitted('success')).toBeTruthy()
    const updates = wrapper.emitted('update:modelValue') || []
    expect(updates.at(-1)).toEqual([false])
  })

  it.each([
    ['province', { city: '北京市朝阳区暂时不需要', remark: '说明', eventTime: '2026-04-12T00:00' }],
    ['city', { province: '北京市', remark: '说明', eventTime: '2026-04-12T00:00' }],
    ['remark', { province: '北京市', city: '朝阳区', remark: '   ', eventTime: '2026-04-12T00:00' }]
  ])('blocks submit when %s is missing', async (_field, fixture) => {
    const wrapper = mountDialog()
    await flushPromises()
    await nextTick()

    const { formData, handleSubmit } = wrapper.vm.$.setupState
    Object.assign(formData, {
      province: '',
      city: '',
      remark: '',
      eventTime: ''
    }, fixture)
    await nextTick()

    await handleSubmit()
    await flushPromises()

    expect(createEventMock).not.toHaveBeenCalled()
    expect(messageErrorMock).toHaveBeenCalledTimes(1)
  })

  it('routes API errors through logger.error and surfaces toast', async () => {
    createEventMock.mockRejectedValueOnce(new Error('boom'))
    const wrapper = mountDialog()
    await flushPromises()
    await nextTick()

    const { formData, handleSubmit } = wrapper.vm.$.setupState
    formData.province = '北京市'
    formData.city = '朝阳区'
    formData.remark = '温度超标'
    formData.eventTime = '2026-04-12T00:00'
    await nextTick()

    await handleSubmit()
    await flushPromises()

    expect(loggerErrorMock).toHaveBeenCalledTimes(1)
    expect(messageErrorMock).toHaveBeenCalledWith('boom')
    expect(wrapper.emitted('success')).toBeFalsy()
  })
})
