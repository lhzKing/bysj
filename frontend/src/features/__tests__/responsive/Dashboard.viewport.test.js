import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import Dashboard from '@/features/dashboard/views/Dashboard.vue'
import dashboardSource from '@/features/dashboard/views/Dashboard.vue?raw'

const { routerPushMock, getKPIMock, getMapDataMock, getTrendMock, loggerMock } = vi.hoisted(() => ({
  routerPushMock: vi.fn(),
  getKPIMock: vi.fn(),
  getMapDataMock: vi.fn(),
  getTrendMock: vi.fn(),
  loggerMock: { error: vi.fn(), info: vi.fn(), warn: vi.fn() }
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPushMock })
}))

vi.mock('@/features/dashboard/api', () => ({
  getKPI: getKPIMock,
  getMapData: getMapDataMock,
  getTrend: getTrendMock
}))

vi.mock('@/shared/utils/logger', () => ({
  logger: loggerMock
}))

const stubsConfig = {
  DashboardKPI: { template: '<div data-testid="dashboard-kpi-stub" />' },
  DashboardTrend: { template: '<div data-testid="dashboard-trend-stub" />' },
  DashboardWorkload: { template: '<div data-testid="dashboard-workload-stub" />' },
  DashboardExceptions: { template: '<div data-testid="dashboard-exceptions-stub" />' }
}

function setViewport(width) {
  Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: width })
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    configurable: true,
    value: vi.fn().mockImplementation((query) => {
      // Naively parse "(max-width: NNNpx)" against current width
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

describe('Dashboard viewport responsive contract', () => {
  beforeEach(() => {
    routerPushMock.mockReset()
    getKPIMock.mockReset()
    getMapDataMock.mockReset()
    getTrendMock.mockReset()
    getKPIMock.mockResolvedValue({ totalTraces: 100, todayNew: 1, totalLogs: 50, exceptionCount: 0, range: '30d' })
    getMapDataMock.mockResolvedValue({ items: [], total: 0, range: '30d' })
    getTrendMock.mockResolvedValue({ items: [], total: 0, range: '30d' })
  })

  describe('Source-level @media contract', () => {
    it('declares two responsive breakpoints (1023 tablet + 640 mobile) in scoped CSS', () => {
      expect(dashboardSource).toMatch(/@media\s*\(\s*max-width:\s*1023px\s*\)/)
      expect(dashboardSource).toMatch(/@media\s*\(\s*max-width:\s*640px\s*\)/)
    })

    it('collapses .dashboard__row-2 to single column at tablet breakpoint', () => {
      expect(dashboardSource).toMatch(/@media\s*\(\s*max-width:\s*1023px\s*\)\s*\{[^}]*\.dashboard__row-2[\s\S]*?grid-template-columns:\s*minmax\(0,\s*1fr\)/)
    })

    it('shrinks padding and segment buttons at mobile breakpoint', () => {
      const mobileBlock = dashboardSource.match(/@media\s*\(\s*max-width:\s*640px\s*\)\s*\{([\s\S]*?)^\}/m)
      expect(mobileBlock).toBeTruthy()
      expect(mobileBlock[1]).toMatch(/\.dashboard\b[\s\S]*?padding/)
      expect(mobileBlock[1]).toMatch(/\.dashboard__seg/)
    })
  })

  describe('Desktop viewport (1280×800)', () => {
    beforeEach(() => setViewport(1280))

    it('renders all 4 dashboard sub-components + range segment + KPI grid markup', async () => {
      const wrapper = mount(Dashboard, { global: { stubs: stubsConfig } })
      await flushPromises()

      expect(wrapper.find('[data-testid="dashboard-kpi-stub"]').exists()).toBe(true)
      expect(wrapper.find('[data-testid="dashboard-trend-stub"]').exists()).toBe(true)
      expect(wrapper.find('[data-testid="dashboard-workload-stub"]').exists()).toBe(true)
      expect(wrapper.find('[data-testid="dashboard-exceptions-stub"]').exists()).toBe(true)

      expect(wrapper.find('[data-testid="dashboard-range-30d"]').exists()).toBe(true)
      expect(wrapper.find('[data-testid="dashboard-range-7d"]').exists()).toBe(true)

      expect(wrapper.find('.dashboard__row-2').exists()).toBe(true)
      expect(wrapper.find('.dashboard__col-2').exists()).toBe(true)
      expect(wrapper.find('.dashboard__col-1').exists()).toBe(true)
    })
  })

  describe('Mobile viewport (390×844)', () => {
    beforeEach(() => setViewport(390))

    it('renders identical structural anchors at 390px (CSS handles visual collapse via @media)', async () => {
      const wrapper = mount(Dashboard, { global: { stubs: stubsConfig } })
      await flushPromises()

      // Same 4 sub-components rendered
      expect(wrapper.find('[data-testid="dashboard-kpi-stub"]').exists()).toBe(true)
      expect(wrapper.find('[data-testid="dashboard-trend-stub"]').exists()).toBe(true)
      expect(wrapper.find('[data-testid="dashboard-workload-stub"]').exists()).toBe(true)
      expect(wrapper.find('[data-testid="dashboard-exceptions-stub"]').exists()).toBe(true)

      // .dashboard__row-2 still present; CSS @media at 1023 collapses to single column
      expect(wrapper.find('.dashboard__row-2').exists()).toBe(true)
      expect(wrapper.find('.dashboard__seg').exists()).toBe(true)
    })

    it('matchMedia mock reports tablet/mobile breakpoints as matched at 390px', () => {
      expect(window.matchMedia('(max-width: 1023px)').matches).toBe(true)
      expect(window.matchMedia('(max-width: 640px)').matches).toBe(true)
    })
  })
})
