import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import TraceList from '@/features/trace/views/TraceList.vue'

const { routerPushMock, toastWarning, toastError, listTracesMock } = vi.hoisted(() => ({
  routerPushMock: vi.fn(),
  toastWarning: vi.fn(),
  toastError: vi.fn(),
  listTracesMock: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPushMock }),
  useRoute: () => ({ query: {}, path: '/traces' }),
  RouterLink: {
    props: ['to'],
    template: '<a :data-to="to"><slot /></a>'
  }
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({
    warning: toastWarning,
    error: toastError,
    success: vi.fn(),
    info: vi.fn()
  })
}))

vi.mock('@/shared/utils/logger', () => ({
  logger: { error: vi.fn(), info: vi.fn(), warn: vi.fn() }
}))

vi.mock('@/features/trace/api/trace', () => ({
  listTraces: listTracesMock
}))

// TraceList now reads userStore.hasAnyPermission to gate the header
// scan/assign action buttons (Finding #2 fix). The smoke test isn't a
// Pinia-aware harness, so stub the store with a permissive default.
vi.mock('@/core/stores/user', () => ({
  useUserStore: () => ({ hasAnyPermission: () => true })
}))

const fakeRow = (overrides = {}) => ({
  traceCode: 'TC-260505-A8F3K2',
  spuId: 1,
  spuPartCode: 'SPU-VALVE-001',
  spuPartName: '工业高压阀门',
  currentStatus: 'IN_STOCK',
  currentNode: '上海仓库',
  currentOwner: 'warehouse',
  province: '上海市',
  city: '上海市',
  lastEventTime: '2026-05-08T10:00:00',
  lastLogId: 95,
  lastActionType: 'INBOUND',
  batchId: 12,
  batchNo: 'ASSIGN-20260507-0001',
  codeStatus: 'IN_STOCK',
  ...overrides
})

const fakePage = (rows = [fakeRow()], overrides = {}) => ({
  list: rows,
  total: rows.length,
  page: 1,
  size: 20,
  totalPages: rows.length === 0 ? 0 : 1,
  ...overrides
})

const mountTraceList = () => mount(TraceList, {
  global: {
    stubs: {
      QRScanner: true,
      CreateTraceDialog: {
        props: ['modelValue'],
        template: '<div data-testid="create-trace-dialog" :data-open="modelValue"></div>'
      },
      'router-link': {
        props: ['to'],
        template: '<a :data-to="to"><slot /></a>'
      }
    }
  }
})

describe('TraceList — paged list backed by GET /api/traces', () => {
  beforeEach(() => {
    routerPushMock.mockReset()
    toastWarning.mockReset()
    toastError.mockReset()
    listTracesMock.mockReset()
    listTracesMock.mockResolvedValue(fakePage())
    localStorage.clear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  it('renders page header, filter bar and a row from the API on mount', async () => {
    const wrapper = mountTraceList()
    await flushPromises()

    expect(wrapper.text()).toContain('追溯查询')
    expect(wrapper.find('[data-testid="trace-list-search-input"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="trace-list-status"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="trace-list-batch-no"]').exists()).toBe(true)

    expect(listTracesMock).toHaveBeenCalledTimes(1)
    expect(listTracesMock.mock.calls[0][0]).toMatchObject({
      page: 1,
      size: 20,
      sort: 'last_event_time',
      order: 'desc'
    })

    const rows = wrapper.findAll('[data-testid="trace-list-row"]')
    expect(rows).toHaveLength(1)
    expect(rows[0].attributes('data-code')).toBe('TC-260505-A8F3K2')
  })

  it('renders empty state when the API returns no rows', async () => {
    listTracesMock.mockResolvedValue(fakePage([], { totalPages: 0 }))
    const wrapper = mountTraceList()
    await flushPromises()

    expect(wrapper.find('[data-testid="trace-list-empty"]').exists()).toBe(true)
    expect(wrapper.findAll('[data-testid="trace-list-row"]')).toHaveLength(0)
  })

  it('shows error state when the API rejects', async () => {
    listTracesMock.mockRejectedValueOnce(new Error('boom'))
    const wrapper = mountTraceList()
    await flushPromises()

    expect(wrapper.find('[data-testid="trace-list-error"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('boom')
  })

  it('passes status filter to the API and resets page to 1', async () => {
    const wrapper = mountTraceList()
    await flushPromises()
    listTracesMock.mockClear()

    await wrapper.find('[data-testid="trace-list-status"]').setValue('IN_STOCK')
    await flushPromises()

    expect(listTracesMock).toHaveBeenCalledTimes(1)
    expect(listTracesMock.mock.calls[0][0]).toMatchObject({
      page: 1,
      status: 'IN_STOCK'
    })
  })

  it('passes spuId, batchNo and currentOwner to the API', async () => {
    const wrapper = mountTraceList()
    await flushPromises()
    listTracesMock.mockClear()

    await wrapper.find('[data-testid="trace-list-spu-id"]').setValue('5')
    await flushPromises()

    expect(listTracesMock.mock.calls.at(-1)[0]).toMatchObject({ spuId: 5 })

    await wrapper.find('[data-testid="trace-list-batch-no"]').setValue('ASSIGN-001')
    await flushPromises()
    expect(listTracesMock.mock.calls.at(-1)[0]).toMatchObject({ batchNo: 'ASSIGN-001' })

    await wrapper.find('[data-testid="trace-list-owner"]').setValue('warehouse')
    await flushPromises()
    expect(listTracesMock.mock.calls.at(-1)[0]).toMatchObject({ currentOwner: 'warehouse' })
  })

  it('navigates to detail when clicking a row and pushes recents', async () => {
    const wrapper = mountTraceList()
    await flushPromises()

    await wrapper.find('[data-testid="trace-list-row"]').trigger('click')

    expect(routerPushMock).toHaveBeenCalledWith('/traces/TC-260505-A8F3K2')
    const stored = JSON.parse(localStorage.getItem('recent_traces'))
    expect(stored[0].code).toBe('TC-260505-A8F3K2')
  })

  it('warns when Enter is pressed on empty search input', async () => {
    const wrapper = mountTraceList()
    await flushPromises()

    await wrapper.find('[data-testid="trace-list-search-input"]').trigger('keydown', { key: 'Enter' })

    expect(toastWarning).toHaveBeenCalledWith('请输入追溯码或关键词')
  })

  it('jumps directly to detail when Enter matches a single result by exact trace code', async () => {
    listTracesMock.mockResolvedValue(fakePage([
      fakeRow({ traceCode: 'TRC-EXACT' })
    ]))
    const wrapper = mountTraceList()
    await flushPromises()

    const input = wrapper.find('[data-testid="trace-list-search-input"]')
    await input.setValue('TRC-EXACT')
    await new Promise((r) => setTimeout(r, 320))
    await flushPromises()

    await input.trigger('keydown', { key: 'Enter' })

    expect(routerPushMock).toHaveBeenCalledWith('/traces/TRC-EXACT')
  })

  it('persists and renders recents from localStorage', async () => {
    localStorage.setItem('recent_traces', JSON.stringify([
      { code: 'TC-A', time: '2026-05-08T10:00:00.000Z' }
    ]))

    const wrapper = mountTraceList()
    await flushPromises()

    const chips = wrapper.findAll('[data-testid="trace-list-recent-chip"]')
    expect(chips).toHaveLength(1)
    expect(chips[0].attributes('data-code')).toBe('TC-A')
  })

  it('removes a recent chip without navigating', async () => {
    localStorage.setItem('recent_traces', JSON.stringify([
      { code: 'TC-A', time: '2026-05-08T10:00:00.000Z' },
      { code: 'TC-B', time: '2026-05-08T09:00:00.000Z' }
    ]))

    const wrapper = mountTraceList()
    await flushPromises()

    const removes = wrapper.findAll('[data-testid="trace-list-recent-remove"]')
    await removes[0].trigger('click')
    await nextTick()

    expect(routerPushMock).not.toHaveBeenCalled()
    const stored = JSON.parse(localStorage.getItem('recent_traces'))
    expect(stored).toHaveLength(1)
    expect(stored[0].code).toBe('TC-B')
  })

  it('clears all recents via the clear button', async () => {
    localStorage.setItem('recent_traces', JSON.stringify([
      { code: 'TC-A', time: '2026-05-08T10:00:00.000Z' }
    ]))

    const wrapper = mountTraceList()
    await flushPromises()

    await wrapper.find('[data-testid="trace-list-clear-recent"]').trigger('click')
    await nextTick()

    expect(JSON.parse(localStorage.getItem('recent_traces'))).toEqual([])
  })

  it('paginates: clicking next button calls API with page=2', async () => {
    listTracesMock.mockResolvedValue(fakePage(
      [fakeRow(), fakeRow({ traceCode: 'TC-2' })],
      { total: 50, totalPages: 3, page: 1 }
    ))
    const wrapper = mountTraceList()
    await flushPromises()

    listTracesMock.mockClear()
    listTracesMock.mockResolvedValue(fakePage(
      [fakeRow({ traceCode: 'TC-3' })],
      { total: 50, totalPages: 3, page: 2 }
    ))
    await wrapper.find('[data-testid="trace-list-page-next"]').trigger('click')
    await flushPromises()

    expect(listTracesMock).toHaveBeenCalledTimes(1)
    expect(listTracesMock.mock.calls[0][0]).toMatchObject({ page: 2 })
  })

  it('shows match count summary from total', async () => {
    listTracesMock.mockResolvedValue(fakePage([fakeRow()], { total: 1234 }))
    const wrapper = mountTraceList()
    await flushPromises()

    const matchCount = wrapper.find('[data-testid="trace-list-match-count"]')
    expect(matchCount.text()).toContain('1,234')
  })
})
