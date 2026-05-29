import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { renderWithPrime } from '@/test/renderWithPrime'
import TraceAggregationWorkbench from '@/features/trace/views/TraceAggregationWorkbench.vue'

const pushMock = vi.fn()
const confirmMock = vi.fn()
const toastSuccessMock = vi.fn()
const toastErrorMock = vi.fn()
const hasAnyPermissionMock = vi.fn()
const listActiveAggregationsMock = vi.fn()
const releaseAggregationMock = vi.fn()
const bindAggregationMock = vi.fn()

vi.mock('vue-router', () => ({ useRouter: () => ({ push: pushMock }) }))
vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({ success: toastSuccessMock, error: toastErrorMock, warning: vi.fn() })
}))
vi.mock('@/shared/composables/useConfirm', () => ({ useConfirm: () => ({ confirm: confirmMock }) }))
vi.mock('@/core/stores/user', () => ({
  useUserStore: () => ({
    hasAnyPermission: (...args) => hasAnyPermissionMock(...args)
  })
}))
vi.mock('@/features/trace/api', () => ({
  listActiveAggregations: (...args) => listActiveAggregationsMock(...args),
  releaseAggregation: (...args) => releaseAggregationMock(...args),
  bindAggregation: (...args) => bindAggregationMock(...args)
}))

const aggregationBindDialogStub = {
  name: 'AggregationBindDialog',
  props: ['modelValue', 'defaultRelationType'],
  emits: ['update:modelValue', 'success'],
  template: `<div v-if="modelValue" :data-test="'aggregation-bind-dialog-' + defaultRelationType"></div>`
}

function row(overrides = {}) {
  return {
    id: 1,
    parentCode: 'CARTON-EXT-007-abc',
    childCode: 'TC-EXT-007',
    relationType: 'CARTON',
    relationTypeLabel: '箱码',
    active: true,
    createByUsername: 'producer',
    bindTime: '2026-05-22 11:20:00',
    remark: null,
    ...overrides
  }
}

function mountWorkbench() {
  return renderWithPrime(TraceAggregationWorkbench, {
    global: {
      stubs: { AggregationBindDialog: aggregationBindDialogStub }
    }
  })
}

describe('TraceAggregationWorkbench contract', () => {
  beforeEach(() => {
    pushMock.mockReset()
    confirmMock.mockReset()
    toastSuccessMock.mockReset()
    toastErrorMock.mockReset()
    hasAnyPermissionMock.mockReset()
    listActiveAggregationsMock.mockReset()
    releaseAggregationMock.mockReset()
    bindAggregationMock.mockReset()

    confirmMock.mockResolvedValue(true)
    hasAnyPermissionMock.mockReturnValue(true)
    listActiveAggregationsMock.mockResolvedValue([
      row({ id: 10, parentCode: 'CARTON-EXT-001-aaa', childCode: 'TC-EXT-001' }),
      row({ id: 11, parentCode: 'CARTON-EXT-001-aaa', childCode: 'TC-EXT-002' }),
      row({
        id: 20,
        parentCode: 'PALLET-EXT-001-bbb',
        childCode: 'CARTON-EXT-001-aaa',
        relationType: 'PALLET',
        relationTypeLabel: '托盘码'
      })
    ])
    releaseAggregationMock.mockResolvedValue({})
  })

  it('initial load calls listActiveAggregations with no relation_type filter', async () => {
    mountWorkbench()
    await flushPromises()

    expect(listActiveAggregationsMock).toHaveBeenCalledTimes(1)
    expect(listActiveAggregationsMock).toHaveBeenCalledWith({})
  })

  it('renders both carton and pallet groups grouped by parent_code', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()

    expect(wrapper.text()).toContain('CARTON-EXT-001-aaa')
    expect(wrapper.text()).toContain('PALLET-EXT-001-bbb')
    expect(wrapper.text()).toContain('TC-EXT-001')
    expect(wrapper.text()).toContain('TC-EXT-002')
  })

  it('switching to CARTON filter calls listActiveAggregations({ relationType: "CARTON" })', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()
    listActiveAggregationsMock.mockClear()

    await wrapper.find('[data-test="aggregation-filter-CARTON"]').trigger('click')
    await flushPromises()

    expect(listActiveAggregationsMock).toHaveBeenCalledWith({ relationType: 'CARTON' })
  })

  it('switching to PALLET filter calls listActiveAggregations({ relationType: "PALLET" })', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()
    listActiveAggregationsMock.mockClear()

    await wrapper.find('[data-test="aggregation-filter-PALLET"]').trigger('click')
    await flushPromises()

    expect(listActiveAggregationsMock).toHaveBeenCalledWith({ relationType: 'PALLET' })
  })

  it('release flow goes through confirm + POST /{id}/release + reload', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()
    listActiveAggregationsMock.mockClear()

    await wrapper.find('[data-test="aggregation-release-10"]').trigger('click')
    await flushPromises()

    expect(confirmMock).toHaveBeenCalled()
    expect(releaseAggregationMock).toHaveBeenCalledWith(10)
    expect(toastSuccessMock).toHaveBeenCalledWith('聚合关系已解除')
    expect(listActiveAggregationsMock).toHaveBeenCalledTimes(1)
  })

  it('hides bind / release controls without trace write permission', async () => {
    hasAnyPermissionMock.mockReturnValue(false)
    const wrapper = mountWorkbench()
    await flushPromises()

    expect(wrapper.find('[data-test="aggregation-create-carton"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="aggregation-create-pallet"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="aggregation-release-10"]').exists()).toBe(false)
  })
})
