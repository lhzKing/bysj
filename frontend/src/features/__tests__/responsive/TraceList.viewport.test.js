import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import TraceList from '@/features/trace/views/TraceList.vue'
import traceListSource from '@/features/trace/views/TraceList.vue?raw'

const { routerPushMock, toastWarning, toastError, listTracesMock } = vi.hoisted(() => ({
  routerPushMock: vi.fn(),
  toastWarning: vi.fn(),
  toastError: vi.fn(),
  listTracesMock: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPushMock }),
  RouterLink: { props: ['to'], template: '<a :data-to="to"><slot /></a>' }
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({ warning: toastWarning, error: toastError, success: vi.fn(), info: vi.fn() })
}))

vi.mock('@/shared/utils/logger', () => ({
  logger: { error: vi.fn(), info: vi.fn(), warn: vi.fn() }
}))

vi.mock('@/features/trace/api/trace', () => ({
  listTraces: listTracesMock
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

const fakePage = (rows = [fakeRow()]) => ({
  list: rows,
  total: rows.length,
  page: 1,
  size: 20,
  totalPages: rows.length === 0 ? 0 : 1
})

const mountTraceList = () => mount(TraceList, {
  global: {
    stubs: {
      QRScanner: true,
      CreateTraceDialog: { props: ['modelValue'], template: '<div data-testid="create-trace-dialog" :data-open="modelValue"></div>' },
      'router-link': { props: ['to'], template: '<a :data-to="to"><slot /></a>' }
    }
  }
})

function setViewport(width) {
  Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: width })
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    configurable: true,
    value: vi.fn().mockImplementation((query) => {
      const m = query.match(/max-width:\s*([0-9.]+)px/)
      const maxWidth = m ? parseFloat(m[1]) : Infinity
      return {
        matches: width <= maxWidth,
        media: query,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        addListener: vi.fn(),
        removeListener: vi.fn()
      }
    })
  })
}

describe('TraceList viewport responsive contract', () => {
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

  describe('Source-level @media contract', () => {
    it('declares two breakpoints (1023 tablet for chip wrap + 640 mobile for table→cards)', () => {
      expect(traceListSource).toMatch(/@media\s*\(\s*max-width:\s*1023px\s*\)/)
      expect(traceListSource).toMatch(/@media\s*\(\s*max-width:\s*640px\s*\)/)
    })

    it('caps filter input + search box widths at tablet for chip wrap', () => {
      const tabletBlock = traceListSource.match(/@media\s*\(\s*max-width:\s*1023px\s*\)\s*\{([\s\S]*?)^\}/m)
      expect(tabletBlock).toBeTruthy()
      expect(tabletBlock[1]).toMatch(/\.trace-list__filter-input/)
      expect(tabletBlock[1]).toMatch(/\.trace-list__search-box/)
    })

    it('hides table-wrapper and shows card list at mobile breakpoint', () => {
      const mobileSection = traceListSource.split(/@media\s*\(\s*max-width:\s*640px\s*\)/)[1]
      expect(mobileSection).toBeTruthy()
      expect(mobileSection).toMatch(/\.trace-list__table-wrapper\s*\{\s*display:\s*none/)
      expect(mobileSection).toMatch(/\.trace-list__cards\s*\{[\s\S]*?display:\s*flex/)
    })
  })

  describe('Desktop viewport (1280×800)', () => {
    beforeEach(() => setViewport(1280))

    it('renders both .trace-list__table-wrapper and .trace-list__cards in DOM (CSS hides cards via display:none default)', async () => {
      const wrapper = mountTraceList()
      await flushPromises()

      expect(wrapper.find('.trace-list__table-wrapper').exists()).toBe(true)
      expect(wrapper.find('.trace-list__cards').exists()).toBe(true)
    })

    it('renders dense filter bar with search box, filter chips, and sort menu', async () => {
      const wrapper = mountTraceList()
      await flushPromises()

      expect(wrapper.find('[data-testid="trace-list-search-box"]').exists()).toBe(true)
      expect(wrapper.find('[data-testid="trace-list-status"]').exists()).toBe(true)
      expect(wrapper.find('[data-testid="trace-list-spu-id"]').exists()).toBe(true)
      expect(wrapper.find('[data-testid="trace-list-batch-no"]').exists()).toBe(true)
      expect(wrapper.find('[data-testid="trace-list-sort-toggle"]').exists()).toBe(true)
    })
  })

  describe('Mobile viewport (390×844)', () => {
    beforeEach(() => setViewport(390))

    it('renders both .trace-list__table-wrapper and .trace-list__cards (CSS swaps which is visible)', async () => {
      const wrapper = mountTraceList()
      await flushPromises()

      expect(wrapper.find('.trace-list__table-wrapper').exists()).toBe(true)
      expect(wrapper.find('.trace-list__cards').exists()).toBe(true)
      // Card item present so mobile view has tappable rows
      expect(wrapper.find('.trace-list__card-item').exists()).toBe(true)
    })

    it('matchMedia mock confirms tablet AND mobile breakpoints both match at 390px', () => {
      expect(window.matchMedia('(max-width: 1023px)').matches).toBe(true)
      expect(window.matchMedia('(max-width: 640px)').matches).toBe(true)
    })
  })
})
