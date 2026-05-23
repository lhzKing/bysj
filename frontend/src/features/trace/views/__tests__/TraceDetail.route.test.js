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
const closeTraceExceptionMock = vi.fn()
const createTraceCorrectionMock = vi.fn()
const getTraceCodeByCodeMock = vi.fn()
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
    hasPermission: (permission) => currentUser.permissions.includes(permission),
    // 登记动作按钮的可见性走 hasAnyPermission 走多权限 OR 检测（含 trace:scan super 权限）
    hasAnyPermission: (required = []) => required.some((p) => currentUser.permissions.includes(p))
  })
}))

vi.mock('@/features/trace/api', () => ({
  getTraceDetail: (...args) => getTraceDetailMock(...args),
  verifyTraceChain: (...args) => verifyTraceChainMock(...args),
  closeTraceException: (...args) => closeTraceExceptionMock(...args),
  createTraceCorrection: (...args) => createTraceCorrectionMock(...args),
  getTraceCodeByCode: (...args) => getTraceCodeByCodeMock(...args)
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
    currentStatus: code === 'TRACE-EX' ? 'EXCEPTION' : 'ACTIVE',
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
      TraceExceptionCloseDialog: {
        props: ['modelValue'],
        template: '<div data-testid="exception-close-dialog" :data-open="modelValue"></div>'
      },
      TraceCorrectionDialog: {
        props: ['modelValue'],
        template: '<div data-testid="correction-dialog" :data-open="modelValue"></div>'
      },
      TraceRouteMap: true,
      PrintLabelDialog: true,
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
    closeTraceExceptionMock.mockReset()
    createTraceCorrectionMock.mockReset()
    getTraceCodeByCodeMock.mockReset()

    getTraceDetailMock.mockImplementation((code, view = 'effective') =>
      Promise.resolve(traceDetailResponse(code, view))
    )

    // 默认让 codeRecord 拿到一个 PRINTED 状态的单品码，避免详情页 mode 计算到边界态
    getTraceCodeByCodeMock.mockImplementation((code) =>
      Promise.resolve({
        traceCode: code,
        batchId: 9,
        codeStatus: 'PRINTED',
        printCount: 1,
        qrPayload: `http://localhost/public/traces/${code}`
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
    expect(wrapper.text()).toContain('链上完整')

    routeMock.params.code = 'TRACE-002'
    await nextTick()
    await flushPromises()

    expect(wrapper.text()).not.toContain('链上完整')
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

  it('shows exception close and correction workflow entries for exception handlers', async () => {
    routeMock.params.code = 'TRACE-EX'
    currentUser.permissions = ['trace:view', 'trace:exception:handle']
    const wrapper = mountTraceDetail()

    await flushPromises()

    expect(wrapper.find('[data-testid="trace-exception-close-button"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="trace-correction-button"]').exists()).toBe(true)

    await wrapper.find('[data-testid="trace-exception-close-button"]').trigger('click')
    await wrapper.find('[data-testid="trace-correction-button"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="exception-close-dialog"]').attributes('data-open')).toBe('true')
    expect(wrapper.find('[data-testid="correction-dialog"]').attributes('data-open')).toBe('true')
  })

  it('renders verify stripe driven by verification result counts', async () => {
    verifyTraceChainMock.mockResolvedValue({
      valid: true,
      totalLogs: 4,
      hashVerifiedCount: 4,
      signatureVerifiedCount: 4,
      errors: []
    })

    const wrapper = mountTraceDetail()
    await flushPromises()

    expect(wrapper.text()).toContain('链上完整 · 4 / 4 节点验签通过')
    expect(wrapper.text()).toContain('RSA-2048 · SHA-256 链式哈希')
  })

  it('toggles hash row visibility per timeline event', async () => {
    const wrapper = mountTraceDetail()
    await flushPromises()

    expect(wrapper.find('[data-testid="trace-hash-row"]').exists()).toBe(false)

    const toggle = wrapper.find('[data-testid="trace-hash-toggle-2"]')
    expect(toggle.exists()).toBe(true)
    await toggle.trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="trace-hash-row"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('currentHash')
  })

  it('switches between flow / part / aggregation / audit tabs without remount', async () => {
    const wrapper = mountTraceDetail()
    await flushPromises()

    const aggregationTab = wrapper.find('[data-testid="trace-detail-tab-aggregation"]')
    expect(aggregationTab.exists()).toBe(true)
    await aggregationTab.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('箱码 / 托盘码聚合历史')

    const auditTab = wrapper.find('[data-testid="trace-detail-tab-audit"]')
    expect(auditTab.exists()).toBe(true)
    await auditTab.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('仅展示 CORRECTION / EXCEPTION 类事件')
  })

  it('label button reads "打印标签" with mode=print when user has print permission and code is GENERATED', async () => {
    currentUser.permissions = ['trace:view', 'trace:code:print']
    getTraceCodeByCodeMock.mockResolvedValueOnce({
      traceCode: 'TRACE-001',
      batchId: 9,
      codeStatus: 'GENERATED',
      printCount: 0
    })
    const wrapper = mountTraceDetail()
    await flushPromises()

    const btn = wrapper.find('[data-testid="trace-detail-label-button"]')
    expect(btn.exists()).toBe(true)
    expect(btn.text()).toContain('打印标签')
    expect(btn.attributes('data-mode')).toBe('print')
  })

  it('label button reads "重打标签" with mode=reprint for non-terminal printed/activated codes', async () => {
    currentUser.permissions = ['trace:view', 'trace:code:print']
    getTraceCodeByCodeMock.mockResolvedValueOnce({
      traceCode: 'TRACE-001',
      batchId: 9,
      codeStatus: 'ACTIVATED',
      printCount: 1
    })
    const wrapper = mountTraceDetail()
    await flushPromises()

    const btn = wrapper.find('[data-testid="trace-detail-label-button"]')
    expect(btn.text()).toContain('重打标签')
    expect(btn.attributes('data-mode')).toBe('reprint')
  })

  it('label button forces view mode for terminal (VOIDED) codes even with print permission', async () => {
    currentUser.permissions = ['trace:view', 'trace:code:print']
    getTraceCodeByCodeMock.mockResolvedValueOnce({
      traceCode: 'TRACE-001',
      batchId: 9,
      codeStatus: 'VOIDED',
      printCount: 2
    })
    const wrapper = mountTraceDetail()
    await flushPromises()

    const btn = wrapper.find('[data-testid="trace-detail-label-button"]')
    expect(btn.text()).toContain('查看二维码')
    expect(btn.attributes('data-mode')).toBe('view')
    // tooltip 应当说明"已作废"
    expect(btn.attributes('title')).toContain('作废')
  })

  it('label button is hidden entirely for users without trace:code:print permission', async () => {
    currentUser.permissions = ['trace:view']
    getTraceCodeByCodeMock.mockResolvedValueOnce({
      traceCode: 'TRACE-001',
      batchId: 9,
      codeStatus: 'GENERATED',
      printCount: 0
    })
    const wrapper = mountTraceDetail()
    await flushPromises()

    // 没有 trace:code:print 权限的普通用户根本看不到按钮 —— 也就没法触发任何打印 / 预览 dialog
    expect(wrapper.find('[data-testid="trace-detail-label-button"]').exists()).toBe(false)
  })

  it('hides 登记动作 dropdown entirely for users without any scan write permission (USER role)', async () => {
    // USER 只有 trace:view —— 既没 inbound/outbound/transfer，也没 super 的 trace:scan
    currentUser.permissions = ['trace:view']
    const wrapper = mountTraceDetail()
    await flushPromises()

    // 整个"登记动作"按钮都不应该渲染，避免用户点开看到空 dropdown 或点击触发 403
    expect(wrapper.text()).not.toContain('登记动作')
  })

  it('shows 登记动作 with only the menu items the user actually has permission for', async () => {
    // 模拟 WAREHOUSE 角色：有 inbound / outbound，没 transfer
    currentUser.permissions = ['trace:view', 'trace:inbound', 'trace:outbound']
    const wrapper = mountTraceDetail()
    await flushPromises()

    expect(wrapper.text()).toContain('登记动作')

    const menuTrigger = wrapper.findAll('button').find((b) => b.text().includes('登记动作'))
    expect(menuTrigger).toBeTruthy()
    await menuTrigger.trigger('click')
    await flushPromises()

    // 看到 inbound/outbound 两条，看不到 transfer（物流流转）
    expect(wrapper.text()).toContain('入库登记')
    expect(wrapper.text()).toContain('出库登记')
    expect(wrapper.text()).not.toContain('物流流转')
  })

  it('shows all 3 menu items when user has trace:scan super permission', async () => {
    // trace:scan 是 super 权限，应当覆盖 inbound/outbound/transfer 三条菜单
    currentUser.permissions = ['trace:view', 'trace:scan']
    const wrapper = mountTraceDetail()
    await flushPromises()

    const menuTrigger = wrapper.findAll('button').find((b) => b.text().includes('登记动作'))
    expect(menuTrigger).toBeTruthy()
    await menuTrigger.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('入库登记')
    expect(wrapper.text()).toContain('出库登记')
    expect(wrapper.text()).toContain('物流流转')
  })
})
