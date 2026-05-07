import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive, nextTick } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import TraceDetail from '@/features/trace/views/TraceDetail.vue'

const routeMock = reactive({
  params: {
    code: 'TRACE-001'
  }
})

const currentUser = reactive({
  permissions: ['trace:view']
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

vi.mock('@/core/stores/user', () => ({
  useUserStore: () => ({
    hasPermission: (permission) => currentUser.permissions.includes(permission)
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

const traceDetailResponse = (code, view = 'effective') => ({
  view,
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
  history: view === 'audit'
    ? [
        {
          id: 1,
          actionType: 'OUTBOUND',
          eventTime: '2026-04-12T10:00:00',
          currentHash: 'abcdef1234567890',
          operator: 'alice'
        },
        {
          id: 2,
          actionType: 'CORRECTION',
          correctionOf: 1,
          eventTime: '2026-04-12T11:00:00',
          currentHash: '1234567890abcdef',
          operator: 'auditor',
          remark: '修正错误节点'
        }
      ]
    : [
        {
          id: 2,
          actionType: 'CORRECTION',
          correctionOf: 1,
          eventTime: '2026-04-12T11:00:00',
          currentHash: '1234567890abcdef',
          operator: 'auditor',
          remark: '修正错误节点'
        }
      ],
  aggregationHistory: [
    {
      relationId: 11,
      parentCode: 'CARTON-001',
      childCode: code,
      relationType: 'CARTON',
      relationTypeLabel: '箱码',
      active: true,
      direct: true,
      level: 1,
      bindTime: '2026-05-07 10:00:00',
      releaseTime: null,
      remark: '生产装箱'
    },
    {
      relationId: 12,
      parentCode: 'PALLET-001',
      childCode: 'CARTON-001',
      relationType: 'PALLET',
      relationTypeLabel: '托盘码',
      active: true,
      direct: false,
      level: 2,
      viaCode: 'CARTON-001',
      bindTime: '2026-05-07 10:10:00',
      releaseTime: null,
      remark: '整托发运'
    }
  ]
})

const mountTraceDetail = () => mount(TraceDetail, {
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

describe('TraceDetail route reuse and detail views', () => {
  beforeEach(() => {
    consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    routeMock.params.code = 'TRACE-001'
    currentUser.permissions = ['trace:view']
    routerPushMock.mockReset()
    routerBackMock.mockReset()
    getTraceDetailMock.mockReset()
    verifyTraceChainMock.mockReset()

    getTraceDetailMock.mockImplementation((code, view = 'effective') =>
      Promise.resolve(traceDetailResponse(code, view))
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

  it('loads effective detail by default and reloads when route params code changes', async () => {
    const wrapper = mountTraceDetail()

    await flushPromises()

    expect(getTraceDetailMock).toHaveBeenCalledWith('TRACE-001', 'effective')
    expect(wrapper.text()).toContain('TRACE-001')
    expect(wrapper.text()).toContain('业务有效视图')
    expect(wrapper.find('[data-testid="trace-detail-audit-tab"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="trace-aggregation-history"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('箱码 / 托盘码聚合历史')
    expect(wrapper.text()).toContain('CARTON-001')
    expect(wrapper.text()).toContain('PALLET-001')
    expect(wrapper.text()).toContain('经 CARTON-001 关联')

    routeMock.params.code = 'TRACE-002'
    await nextTick()
    await flushPromises()

    expect(getTraceDetailMock).toHaveBeenCalledWith('TRACE-002', 'effective')
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

    const wrapper = mountTraceDetail()

    await flushPromises()
    expect(wrapper.text()).toContain('链上数据已验证')

    routeMock.params.code = 'TRACE-002'
    await nextTick()
    await flushPromises()

    expect(wrapper.text()).not.toContain('链上数据已验证')
  })

  it('allows audit-permitted users to switch to full audit history and marks corrected records', async () => {
    currentUser.permissions = ['trace:view', 'trace:audit:view']
    const wrapper = mountTraceDetail()

    await flushPromises()

    expect(wrapper.find('[data-testid="trace-detail-audit-tab"]').exists()).toBe(true)
    await wrapper.find('[data-testid="trace-detail-audit-tab"]').trigger('click')
    await flushPromises()

    expect(getTraceDetailMock).toHaveBeenLastCalledWith('TRACE-001', 'audit')
    expect(wrapper.text()).toContain('审计完整视图')
    expect(wrapper.text()).toContain('当前返回 2 条完整审计记录')
    expect(wrapper.text()).toContain('已被纠错覆盖')
    expect(wrapper.text()).toContain('本记录修正原始日志 #1')
    expect(wrapper.find('[data-corrected-original="true"]').exists()).toBe(true)
  })
})
