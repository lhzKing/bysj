import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import TraceAggregationWorkbench from '@/features/trace/views/TraceAggregationWorkbench.vue'

const {
  routerPushMock,
  toastSuccess,
  toastError,
  confirmMock,
  listActiveAggregationsMock,
  releaseAggregationMock,
  bindAggregationMock,
  hasAnyPermissionMock
} = vi.hoisted(() => ({
  routerPushMock: vi.fn(),
  toastSuccess: vi.fn(),
  toastError: vi.fn(),
  confirmMock: vi.fn(),
  listActiveAggregationsMock: vi.fn(),
  releaseAggregationMock: vi.fn(),
  bindAggregationMock: vi.fn(),
  hasAnyPermissionMock: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPushMock })
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({ success: toastSuccess, error: toastError, warning: vi.fn(), info: vi.fn() })
}))

vi.mock('@/shared/composables/useConfirm', () => ({
  useConfirm: () => ({ confirm: confirmMock })
}))

vi.mock('@/shared/utils/logger', () => ({
  logger: { error: vi.fn(), warn: vi.fn(), info: vi.fn() }
}))

vi.mock('@/core/stores/user', () => ({
  useUserStore: () => ({
    hasAnyPermission: (...args) => hasAnyPermissionMock(...args)
  })
}))

vi.mock('@/features/trace/api', () => ({
  listActiveAggregations: listActiveAggregationsMock,
  releaseAggregation: releaseAggregationMock,
  bindAggregation: bindAggregationMock
}))

const aggregationBindDialogStub = {
  name: 'AggregationBindDialog',
  props: ['modelValue', 'defaultRelationType'],
  emits: ['update:modelValue', 'success'],
  template: `<div v-if="modelValue" :data-test="'aggregation-bind-dialog-' + defaultRelationType"></div>`
}

function buildRows() {
  return [
    {
      id: 1,
      parentCode: 'CARTON-EXT-001-abc',
      childCode: 'TC-001',
      relationType: 'CARTON',
      relationTypeLabel: '箱码',
      active: true,
      createByUsername: 'producer',
      bindTime: '2026-05-20 09:30:00',
      remark: '发往北京仓'
    },
    {
      id: 2,
      parentCode: 'CARTON-EXT-001-abc',
      childCode: 'TC-002',
      relationType: 'CARTON',
      relationTypeLabel: '箱码',
      active: true,
      createByUsername: 'producer',
      bindTime: '2026-05-20 09:30:00',
      remark: '发往北京仓'
    },
    {
      id: 3,
      parentCode: 'PALLET-EXT-001-xyz',
      childCode: 'CARTON-EXT-001-abc',
      relationType: 'PALLET',
      relationTypeLabel: '托盘码',
      active: true,
      createByUsername: 'warehouse',
      bindTime: '2026-05-21 14:10:00',
      remark: null
    }
  ]
}

function mountWorkbench() {
  return mount(TraceAggregationWorkbench, {
    global: {
      stubs: {
        AggregationBindDialog: aggregationBindDialogStub
      }
    }
  })
}

beforeEach(() => {
  routerPushMock.mockReset()
  toastSuccess.mockReset()
  toastError.mockReset()
  confirmMock.mockReset()
  confirmMock.mockResolvedValue(true)
  listActiveAggregationsMock.mockReset()
  releaseAggregationMock.mockReset()
  bindAggregationMock.mockReset()
  hasAnyPermissionMock.mockReset()
  hasAnyPermissionMock.mockReturnValue(true)

  listActiveAggregationsMock.mockResolvedValue(buildRows())
  releaseAggregationMock.mockResolvedValue({})
})

afterEach(() => {
  document.body.style.overflow = ''
})

describe('TraceAggregationWorkbench (Linear shell)', () => {
  it('renders page header with title + actions', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()

    const header = wrapper.find('[data-testid="aggregation-page-header"]')
    expect(header.exists()).toBe(true)
    expect(header.text()).toContain('箱码 / 托盘码聚合')

    expect(wrapper.find('[data-test="aggregation-refresh"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="aggregation-create-carton"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="aggregation-create-pallet"]').exists()).toBe(true)
  })

  it('renders 3 KPIs with mono numerics counted from list response', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()

    const total = wrapper.find('[data-testid="aggregation-kpi-total"]')
    const carton = wrapper.find('[data-testid="aggregation-kpi-carton"]')
    const pallet = wrapper.find('[data-testid="aggregation-kpi-pallet"]')

    expect(total.exists()).toBe(true)
    expect(total.text()).toContain('3')
    expect(carton.text()).toContain('2')
    expect(pallet.text()).toContain('1')
  })

  it('groups rows by parent_code with relation pill in group header', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()

    const groups = wrapper.find('[data-test="aggregation-groups"]')
    expect(groups.exists()).toBe(true)

    const cartonGroup = wrapper.find('[data-test="aggregation-group-CARTON-EXT-001-abc"]')
    expect(cartonGroup.exists()).toBe(true)
    expect(cartonGroup.text()).toContain('CARTON-EXT-001-abc')
    expect(cartonGroup.text()).toContain('共 2 个子码')
    expect(cartonGroup.text()).toContain('TC-001')
    expect(cartonGroup.text()).toContain('TC-002')

    const palletGroup = wrapper.find('[data-test="aggregation-group-PALLET-EXT-001-xyz"]')
    expect(palletGroup.exists()).toBe(true)
    expect(palletGroup.text()).toContain('共 1 个子码')
  })

  it('switches relation_type filter and re-fetches with relationType param', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()
    listActiveAggregationsMock.mockClear()

    await wrapper.find('[data-test="aggregation-filter-CARTON"]').trigger('click')
    await flushPromises()

    expect(listActiveAggregationsMock).toHaveBeenCalledTimes(1)
    expect(listActiveAggregationsMock).toHaveBeenCalledWith({ relationType: 'CARTON' })
  })

  it('filters rows client-side by parent_code keyword search', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()

    await wrapper.find('[data-test="aggregation-search"]').setValue('PALLET-EXT')
    await flushPromises()

    expect(wrapper.find('[data-test="aggregation-group-PALLET-EXT-001-xyz"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="aggregation-group-CARTON-EXT-001-abc"]').exists()).toBe(false)
  })

  it('opens bind dialog with CARTON default when 新建装箱 is clicked', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()

    await wrapper.find('[data-test="aggregation-create-carton"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-test="aggregation-bind-dialog-CARTON"]').exists()).toBe(true)
  })

  it('opens bind dialog with PALLET default when 新建装托 is clicked', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()

    await wrapper.find('[data-test="aggregation-create-pallet"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-test="aggregation-bind-dialog-PALLET"]').exists()).toBe(true)
  })

  it('confirms then releases an aggregation and reloads the list', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()
    listActiveAggregationsMock.mockClear()

    await wrapper.find('[data-test="aggregation-release-1"]').trigger('click')
    await flushPromises()

    expect(confirmMock).toHaveBeenCalledTimes(1)
    expect(confirmMock.mock.calls[0][0]).toEqual(
      expect.objectContaining({ title: '解除聚合关系', type: 'danger' })
    )
    expect(releaseAggregationMock).toHaveBeenCalledWith(1)
    expect(toastSuccess).toHaveBeenCalledWith('聚合关系已解除')
    expect(listActiveAggregationsMock).toHaveBeenCalledTimes(1)
  })

  it('aborts release when user cancels the confirm dialog', async () => {
    confirmMock.mockResolvedValueOnce(false)
    const wrapper = mountWorkbench()
    await flushPromises()

    await wrapper.find('[data-test="aggregation-release-1"]').trigger('click')
    await flushPromises()

    expect(releaseAggregationMock).not.toHaveBeenCalled()
  })

  it('hides bind / release buttons when user lacks write permissions', async () => {
    hasAnyPermissionMock.mockReturnValue(false)
    const wrapper = mountWorkbench()
    await flushPromises()

    expect(wrapper.find('[data-test="aggregation-create-carton"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="aggregation-create-pallet"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="aggregation-release-1"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="aggregation-refresh"]').exists()).toBe(true)
  })

  it('navigates to trace detail when clicking a single-item child link', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()

    const link = wrapper.find('[data-test="aggregation-child-link-1"]')
    expect(link.exists()).toBe(true)
    await link.trigger('click')

    expect(routerPushMock).toHaveBeenCalledWith('/traces/TC-001')
  })

  it('does not render a clickable link for carton parent codes used as children', async () => {
    const wrapper = mountWorkbench()
    await flushPromises()

    // Row id=3: pallet group, child is CARTON-EXT-001-abc — should NOT be a link.
    const link = wrapper.find('[data-test="aggregation-child-link-3"]')
    expect(link.exists()).toBe(false)
  })

  it('shows empty state when the API returns no rows', async () => {
    listActiveAggregationsMock.mockResolvedValueOnce([])
    const wrapper = mountWorkbench()
    await flushPromises()

    expect(wrapper.find('[data-test="aggregation-empty"]').exists()).toBe(true)
  })
})
