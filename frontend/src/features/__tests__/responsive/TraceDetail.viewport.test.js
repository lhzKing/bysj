import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import TraceDetail from '@/features/trace/views/TraceDetail.vue'
import traceDetailSource from '@/features/trace/views/TraceDetail.vue?raw'

const routeMock = reactive({ params: { code: 'TRACE-001' } })
const currentUser = reactive({ permissions: ['trace:view'] })

const routerPushMock = vi.fn()
const routerBackMock = vi.fn()
const getTraceDetailMock = vi.fn()
const verifyTraceChainMock = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => routeMock,
  useRouter: () => ({ push: routerPushMock, back: routerBackMock })
}))

vi.mock('@/core/stores/user', () => ({
  useUserStore: () => ({
    hasPermission: (p) => currentUser.permissions.includes(p)
  })
}))

vi.mock('@/features/trace/api', () => ({
  getTraceDetail: (...args) => getTraceDetailMock(...args),
  verifyTraceChain: (...args) => verifyTraceChainMock(...args),
  closeTraceException: vi.fn(),
  createTraceCorrection: vi.fn()
}))

const passthroughStub = { template: '<div><slot /></div>' }
const dropdownStub = { template: '<div><slot /><slot name="dropdown" /></div>' }

const fakeDetail = {
  view: 'effective',
  snapshot: {
    traceCode: 'TRACE-001',
    currentStatus: 'IN_STOCK',
    spuId: 1,
    province: '上海市',
    city: '上海市',
    currentNode: '上海仓库',
    lastEventTime: '2026-04-12 00:00:00',
    currentOwner: 'warehouse'
  },
  history: [
    {
      id: 1,
      actionType: 'INBOUND',
      eventTime: '2026-04-12T10:00:00',
      currentHash: 'abcdef1234567890',
      operator: 'warehouse'
    }
  ],
  aggregationHistory: []
}

const mountTraceDetail = () => mount(TraceDetail, {
  global: {
    stubs: {
      LogOut: true,
      X: true,
      BaseCard: passthroughStub,
      ScanFlowDialog: true,
      TraceExceptionCloseDialog: { props: ['modelValue'], template: '<div data-testid="exception-close-dialog" />' },
      TraceCorrectionDialog: { props: ['modelValue'], template: '<div data-testid="correction-dialog" />' },
      TraceRouteMap: true,
      'el-button': passthroughStub,
      'el-dropdown': dropdownStub,
      'el-dropdown-menu': passthroughStub,
      'el-dropdown-item': passthroughStub,
      'el-icon': passthroughStub,
      transition: false
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

describe('TraceDetail viewport responsive contract', () => {
  let consoleErrorSpy

  beforeEach(() => {
    consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    routeMock.params.code = 'TRACE-001'
    currentUser.permissions = ['trace:view']
    routerPushMock.mockReset()
    routerBackMock.mockReset()
    getTraceDetailMock.mockReset()
    verifyTraceChainMock.mockReset()
    getTraceDetailMock.mockResolvedValue(fakeDetail)
    verifyTraceChainMock.mockResolvedValue({
      valid: true, totalLogs: 1, hashVerifiedCount: 1, signatureVerifiedCount: 1, errors: []
    })
  })

  afterEach(() => {
    consoleErrorSpy?.mockRestore()
  })

  describe('Source-level @media contract', () => {
    it('declares two breakpoints (1023 tablet for flow-grid collapse + 640 mobile for header padding)', () => {
      expect(traceDetailSource).toMatch(/@media\s*\(\s*max-width:\s*1023px\s*\)/)
      expect(traceDetailSource).toMatch(/@media\s*\(\s*max-width:\s*640px\s*\)/)
    })

    it('collapses flow-grid to single column at tablet', () => {
      const tabletBlock = traceDetailSource.match(/@media\s*\(\s*max-width:\s*1023px\s*\)\s*\{([\s\S]*?)^\}/m)
      expect(tabletBlock).toBeTruthy()
      expect(tabletBlock[1]).toMatch(/\.trace-detail__flow-grid[\s\S]*?grid-template-columns:\s*1fr/)
    })

    it('shrinks padding and stacks header actions at mobile', () => {
      const mobileSection = traceDetailSource.split(/@media\s*\(\s*max-width:\s*640px\s*\)/)[1]
      expect(mobileSection).toBeTruthy()
      expect(mobileSection).toMatch(/\.trace-detail\b[\s\S]*?padding:\s*16px\s+12px/)
      expect(mobileSection).toMatch(/\.trace-detail__header-actions[\s\S]*?width:\s*100%/)
    })
  })

  describe('Desktop viewport (1280×800)', () => {
    beforeEach(() => setViewport(1280))

    it('renders header, view toggle, and structural anchors after detail loads', async () => {
      const wrapper = mountTraceDetail()
      await flushPromises()

      expect(wrapper.find('.trace-detail__header').exists()).toBe(true)
      expect(wrapper.find('.trace-detail__header-actions').exists()).toBe(true)
      expect(wrapper.find('[data-testid="trace-detail-effective-tab"]').exists()).toBe(true)
      expect(wrapper.text()).toContain('TRACE-001')
    })
  })

  describe('Mobile viewport (390×844)', () => {
    beforeEach(() => setViewport(390))

    it('renders identical structural anchors at 390px (CSS handles visual collapse via @media)', async () => {
      const wrapper = mountTraceDetail()
      await flushPromises()

      expect(wrapper.find('.trace-detail__header').exists()).toBe(true)
      expect(wrapper.find('.trace-detail__header-actions').exists()).toBe(true)
      expect(wrapper.find('[data-testid="trace-detail-effective-tab"]').exists()).toBe(true)
      expect(wrapper.text()).toContain('TRACE-001')
    })

    it('matchMedia mock confirms tablet AND mobile breakpoints both match at 390px', () => {
      expect(window.matchMedia('(max-width: 1023px)').matches).toBe(true)
      expect(window.matchMedia('(max-width: 640px)').matches).toBe(true)
    })
  })
})
