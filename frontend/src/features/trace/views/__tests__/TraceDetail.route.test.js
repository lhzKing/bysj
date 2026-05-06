import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive, nextTick } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import TraceDetail from '@/features/trace/views/TraceDetail.vue'

const routeMock = reactive({
  params: {
    code: 'TRACE-001'
  }
})

const routerPushMock = vi.fn()
const routerBackMock = vi.fn()
const getTraceDetailMock = vi.fn()
const verifyTraceChainMock = vi.fn()
let consoleErrorSpy

vi.mock('vue-router', () => ({
  useRoute: () => routeMock,
  useRouter: () => ({
    push: routerPushMock,
    back: routerBackMock
  })
}))

vi.mock('@/features/trace/api', () => ({
  getTraceDetail: (...args) => getTraceDetailMock(...args),
  verifyTraceChain: (...args) => verifyTraceChainMock(...args)
}))

const passthroughStub = {
  template: '<div><slot /></div>'
}

const dropdownStub = {
  template: '<div><slot /><slot name="dropdown" /></div>'
}

describe('TraceDetail route reuse', () => {
  beforeEach(() => {
    consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    routeMock.params.code = 'TRACE-001'
    routerPushMock.mockReset()
    routerBackMock.mockReset()
    getTraceDetailMock.mockReset()
    verifyTraceChainMock.mockReset()

    getTraceDetailMock.mockImplementation((code) =>
      Promise.resolve({
        snapshot: {
          traceCode: code,
          currentStatus: 'ACTIVE',
          spuId: 1,
          province: 'Province',
          city: 'City',
          currentNode: 'Node',
          lastEventTime: '2026-04-12 00:00:00',
          currentOwner: 'Owner'
        },
        history: []
      })
    )

    verifyTraceChainMock.mockResolvedValue({
      valid: true,
      totalLogs: 0,
      hashVerifiedCount: 0,
      signatureVerifiedCount: 0,
      errors: []
    })
  })

  afterEach(() => {
    consoleErrorSpy?.mockRestore()
  })

  it('reloads detail data when route params code changes', async () => {
    const wrapper = mount(TraceDetail, {
      global: {
        stubs: {
        LogOut: true,
        X: true,
          BaseCard: passthroughStub,
          ScanFlowDialog: true,
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

    await flushPromises()

    expect(getTraceDetailMock).toHaveBeenCalledWith('TRACE-001')
    expect(wrapper.text()).toContain('TRACE-001')

    routeMock.params.code = 'TRACE-002'
    await nextTick()
    await flushPromises()

    expect(getTraceDetailMock).toHaveBeenCalledWith('TRACE-002')
    expect(verifyTraceChainMock).toHaveBeenCalledWith('TRACE-002')
    expect(wrapper.text()).toContain('TRACE-002')
  })

  it('clears stale verification state before reloading a new trace', async () => {
    verifyTraceChainMock.mockImplementation((code) => {
      if (code === 'TRACE-002') {
        return Promise.reject(new Error('verify failed'))
      }

      return Promise.resolve({
        valid: true,
        totalLogs: 0,
        hashVerifiedCount: 0,
        signatureVerifiedCount: 0,
        errors: []
      })
    })

    const wrapper = mount(TraceDetail, {
      global: {
        stubs: {
        LogOut: true,
        X: true,
          BaseCard: passthroughStub,
          ScanFlowDialog: true,
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

    await flushPromises()
    expect(wrapper.text()).toContain('链上数据已验证')

    routeMock.params.code = 'TRACE-002'
    await nextTick()
    await flushPromises()

    expect(wrapper.text()).not.toContain('链上数据已验证')
  })
})
