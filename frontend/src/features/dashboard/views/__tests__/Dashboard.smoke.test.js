import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import Dashboard from '@/features/dashboard/views/Dashboard.vue'

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

const fakeKpi = (overrides = {}) => ({
  totalTraces: 14832,
  todayNew: 243,
  totalLogs: 1742,
  exceptionCount: 3,
  range: '30d',
  ...overrides
})

const fakeMap = (items = [{ name: '浙江', value: 120 }, { name: '江苏', value: 80 }]) => ({
  items,
  total: items.length,
  range: '30d'
})

const fakeTrend = (items = [{ label: '05-01', count: 12 }, { label: '05-02', count: 18 }]) => ({
  items,
  total: items.length,
  range: '30d'
})

const stubsConfig = {
  DashboardKPI: {
    props: ['kpiData'],
    template:
      '<div data-testid="dashboard-kpi-stub">' +
      '<span data-testid="kpi-today">{{ kpiData?.todayNew }}</span>' +
      '<span data-testid="kpi-total">{{ kpiData?.totalTraces }}</span>' +
      '<span data-testid="kpi-logs">{{ kpiData?.totalLogs }}</span>' +
      '<span data-testid="kpi-exception">{{ kpiData?.exceptionCount }}</span>' +
      '</div>'
  },
  DashboardTrend: {
    props: ['trendItems', 'trendLabel'],
    template:
      '<div data-testid="dashboard-trend-stub" :data-len="trendItems?.length || 0" :data-label="trendLabel"></div>'
  },
  DashboardWorkload: {
    props: ['items', 'rangeLabel'],
    template:
      '<div data-testid="dashboard-workload-stub" :data-len="items?.length || 0" :data-label="rangeLabel"></div>'
  },
  DashboardExceptions: {
    props: ['exceptionCount', 'items'],
    emits: ['view-all'],
    template:
      '<div data-testid="dashboard-exceptions-stub" :data-count="exceptionCount" @click="$emit(\'view-all\')"></div>'
  },
  LoadingSkeleton: {
    props: ['type', 'count', 'rows'],
    template: '<div data-testid="dashboard-loading-stub" :data-type="type" :data-count="count"></div>'
  }
}

const mountDashboard = () =>
  mount(Dashboard, {
    global: {
      stubs: stubsConfig
    }
  })

describe('Dashboard — overview view', () => {
  beforeEach(() => {
    routerPushMock.mockReset()
    getKPIMock.mockReset()
    getMapDataMock.mockReset()
    getTrendMock.mockReset()
    loggerMock.error.mockClear()

    getKPIMock.mockResolvedValue(fakeKpi())
    getMapDataMock.mockResolvedValue(fakeMap())
    getTrendMock.mockResolvedValue(fakeTrend())
  })

  afterEach(() => {
    routerPushMock.mockReset()
  })

  it('renders page header and calls 3 dashboard APIs with default range=30d on mount', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.text()).toContain('总览')
    expect(getKPIMock).toHaveBeenCalledTimes(1)
    expect(getKPIMock).toHaveBeenCalledWith('30d')
    expect(getMapDataMock).toHaveBeenCalledWith('30d')
    expect(getTrendMock).toHaveBeenCalledWith('30d')
  })

  it('renders 3 range segmented buttons (today/7d/30d) with 30d active', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('[data-testid="dashboard-range-today"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="dashboard-range-7d"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="dashboard-range-30d"]').exists()).toBe(true)
    const active = wrapper.find('[data-testid="dashboard-range-30d"]')
    expect(active.classes()).toContain('dashboard__seg-btn--active')
  })

  it('switches range to 7d and re-fetches all 3 APIs with new range', async () => {
    const wrapper = mountDashboard()
    await flushPromises()
    getKPIMock.mockClear()
    getMapDataMock.mockClear()
    getTrendMock.mockClear()

    await wrapper.find('[data-testid="dashboard-range-7d"]').trigger('click')
    await flushPromises()

    expect(getKPIMock).toHaveBeenCalledWith('7d')
    expect(getMapDataMock).toHaveBeenCalledWith('7d')
    expect(getTrendMock).toHaveBeenCalledWith('7d')
  })

  it('does not refetch when clicking the already-active range button', async () => {
    const wrapper = mountDashboard()
    await flushPromises()
    getKPIMock.mockClear()
    getMapDataMock.mockClear()
    getTrendMock.mockClear()

    await wrapper.find('[data-testid="dashboard-range-30d"]').trigger('click')
    await flushPromises()

    expect(getKPIMock).not.toHaveBeenCalled()
    expect(getMapDataMock).not.toHaveBeenCalled()
    expect(getTrendMock).not.toHaveBeenCalled()
  })

  it('passes KPI fields (todayNew, totalTraces, totalLogs, exceptionCount) into DashboardKPI', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('[data-testid="kpi-today"]').text()).toBe('243')
    expect(wrapper.find('[data-testid="kpi-total"]').text()).toBe('14832')
    expect(wrapper.find('[data-testid="kpi-logs"]').text()).toBe('1742')
    expect(wrapper.find('[data-testid="kpi-exception"]').text()).toBe('3')
  })

  it('forwards exceptionCount to DashboardExceptions and routes /traces?status=EXCEPTION on view-all', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    const stub = wrapper.find('[data-testid="dashboard-exceptions-stub"]')
    expect(stub.attributes('data-count')).toBe('3')

    await stub.trigger('click')
    expect(routerPushMock).toHaveBeenCalledWith({
      path: '/traces',
      query: { status: 'EXCEPTION' }
    })
  })

  it('passes mapped map items (name + value) to DashboardWorkload', async () => {
    getMapDataMock.mockResolvedValue(
      fakeMap([
        { name: '广东', value: 200 },
        { name: '上海', value: 150 },
        { name: '北京', value: 90 }
      ])
    )
    const wrapper = mountDashboard()
    await flushPromises()

    const stub = wrapper.find('[data-testid="dashboard-workload-stub"]')
    expect(stub.attributes('data-len')).toBe('3')
    expect(stub.attributes('data-label')).toBe('本月')
  })

  it('renders trend with correct trendLabel matching the active range', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    let stub = wrapper.find('[data-testid="dashboard-trend-stub"]')
    expect(stub.attributes('data-label')).toBe('本月')

    await wrapper.find('[data-testid="dashboard-range-today"]').trigger('click')
    await flushPromises()
    stub = wrapper.find('[data-testid="dashboard-trend-stub"]')
    expect(stub.attributes('data-label')).toBe('今日')
  })

  it('keeps zeroed KPI defaults when KPI API rejects and surfaces error message', async () => {
    getKPIMock.mockRejectedValueOnce(new Error('KPI 服务暂不可用'))
    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('[data-testid="dashboard-error"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="dashboard-error"]').text()).toContain('KPI 服务暂不可用')
    expect(wrapper.find('[data-testid="kpi-today"]').text()).toBe('0')
    expect(wrapper.find('[data-testid="kpi-total"]').text()).toBe('0')
    expect(wrapper.find('[data-testid="kpi-logs"]').text()).toBe('0')
    expect(wrapper.find('[data-testid="kpi-exception"]').text()).toBe('0')
  })

  it('still renders KPI block when map/trend APIs reject (graceful fallback)', async () => {
    getMapDataMock.mockRejectedValueOnce(new Error('map down'))
    getTrendMock.mockRejectedValueOnce(new Error('trend down'))
    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('[data-testid="dashboard-kpi-stub"]').exists()).toBe(true)
    const workloadStub = wrapper.find('[data-testid="dashboard-workload-stub"]')
    expect(workloadStub.attributes('data-len')).toBe('0')
    const trendStub = wrapper.find('[data-testid="dashboard-trend-stub"]')
    expect(trendStub.attributes('data-len')).toBe('0')
  })

  it('does not render the export button (placeholder removed)', async () => {
    const wrapper = mountDashboard()
    await flushPromises()
    const btn = wrapper.find('[data-testid="dashboard-export"]')
    expect(btn.exists()).toBe(false)
  })
})
