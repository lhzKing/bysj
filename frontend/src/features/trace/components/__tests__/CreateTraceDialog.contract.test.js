import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import CreateTraceDialog from '@/features/trace/components/CreateTraceDialog.vue'

const createTraceMock = vi.fn()
const getPartsMock = vi.fn()

vi.mock('@/features/trace/api', () => ({
  createTrace: (...args) => createTraceMock(...args)
}))

vi.mock('@/features/part/api', () => ({
  getParts: (...args) => getPartsMock(...args)
}))

describe('CreateTraceDialog contract', () => {
  beforeEach(() => {
    createTraceMock.mockReset()
    getPartsMock.mockReset()
    getPartsMock.mockResolvedValue({
      list: [{ id: 1, partCode: 'P-001', partName: '轴承' }]
    })
  })

  it('renders camelCase part data and submits camelCase payload', async () => {
    createTraceMock.mockResolvedValue({ traceCodes: ['TRACE-001'] })

    const wrapper = mount(CreateTraceDialog, {
      props: { modelValue: true },
      global: {
        stubs: {
          LogOut: true,
          X: true,
          Factory: true,
          teleport: true
        }
      }
    })

    await flushPromises()
    await nextTick()

    const optionTexts = wrapper.findAll('option').map(option => option.text())
    expect(optionTexts.some(text => text.startsWith('P-001 -'))).toBe(true)

    await wrapper.find('select').setValue('P-001')
    await wrapper.find('input[type="number"]').setValue('2')

    const textInputs = wrapper.findAll('input[type="text"]')
    await textInputs[0].setValue('工厂A')
    await textInputs[1].setValue('北京')
    await textInputs[2].setValue('北京市')

    await wrapper.find('[data-test="create-trace-submit"]').trigger('click')
    await flushPromises()

    expect(createTraceMock).toHaveBeenCalledWith({
      partCode: 'P-001',
      quantity: 2,
      manufacturerNode: '工厂A',
      province: '北京',
      city: '北京市'
    })
    expect(wrapper.emitted('success')[0][0]).toEqual(['TRACE-001'])
  })
})
