import { beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive } from 'vue'
import { flushPromises } from '@vue/test-utils'
import { renderWithPrime } from '@/test/renderWithPrime'
import ScanHub from '@/features/trace/views/ScanHub.vue'

const { pushMock } = vi.hoisted(() => ({
  pushMock: vi.fn()
}))

const getTraceAvailableActionsMock = vi.fn()

const currentUser = reactive({
  permissions: ['trace:view']
})

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    useRouter: () => ({
      push: pushMock
    })
  }
})

vi.mock('@/core/stores/user', () => ({
  useUserStore: () => ({
    user: currentUser
  })
}))

vi.mock('@/features/trace/api', () => ({
  getTraceAvailableActions: (...args) => getTraceAvailableActionsMock(...args)
}))

const traceScannerStub = {
  template: `
    <div data-test="trace-scanner-stub">
      <button data-test="emit-scan" @click="$emit('scan', 'TRACE-001')">emit-scan</button>
    </div>
  `
}

const scanFlowDialogStub = {
  props: ['modelValue', 'actionType'],
  template: '<div data-test="scan-flow-dialog-stub" :data-open="modelValue" :data-action="actionType"></div>'
}

const scanExceptionDialogStub = {
  props: ['modelValue'],
  template: '<div data-test="scan-exception-dialog-stub" :data-open="modelValue"></div>'
}

const createTraceDialogStub = {
  template: '<div data-test="create-trace-dialog-stub"></div>'
}

const stubs = {
  TraceScanner: traceScannerStub,
  ScanFlowDialog: scanFlowDialogStub,
  ScanExceptionDialog: scanExceptionDialogStub,
  CreateTraceDialog: createTraceDialogStub
}

function mountScanHub() {
  return renderWithPrime(ScanHub, {
    global: { stubs }
  })
}

async function openActionMatrix() {
  const wrapper = mountScanHub()
  await wrapper.find('button').trigger('click')
  await wrapper.find('[data-test="emit-scan"]').trigger('click')
  await flushPromises()
  return wrapper
}

describe('ScanHub permission model', () => {
  beforeEach(() => {
    currentUser.permissions = ['trace:view']
    pushMock.mockReset()
    getTraceAvailableActionsMock.mockReset()
    getTraceAvailableActionsMock.mockResolvedValue({
      traceCode: 'TRACE-001',
      currentStatus: 'IN_STOCK',
      currentStatusLabel: '在库',
      currentNode: '北京仓库',
      recommendedAction: null,
      availableActions: [],
      noActionReason: '当前角色没有该状态下的扫码动作权限；状态允许动作: OUTBOUND'
    })
  })

  it('does not expose scan actions to a view-only user and shows backend no-action reason', async () => {
    const wrapper = await openActionMatrix()

    expect(getTraceAvailableActionsMock).toHaveBeenCalledWith('TRACE-001')
    expect(wrapper.text()).toContain('查看溯源详情')
    expect(wrapper.find('[data-test="no-available-actions"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('当前角色没有该状态下的扫码动作权限')
    expect(wrapper.text()).not.toContain('确认出库')
  })

  it('renders only backend returned executable actions and highlights recommendation', async () => {
    getTraceAvailableActionsMock.mockResolvedValueOnce({
      traceCode: 'TRACE-001',
      currentStatus: 'IN_STOCK',
      currentStatusLabel: '在库',
      currentNode: '北京仓库',
      recommendedAction: 'OUTBOUND',
      availableActions: [
        { actionType: 'OUTBOUND', label: '确认出库', nextStatus: 'IN_TRANSIT', nextStatusLabel: '运输中', requiresRemark: false, permissionHint: 'trace:outbound or trace:scan' },
        { actionType: 'EXCEPTION', label: '上报异常', nextStatus: 'EXCEPTION', nextStatusLabel: '异常', requiresRemark: true, permissionHint: 'trace:scan' }
      ]
    })
    currentUser.permissions = ['trace:scan', 'trace:view']
    const wrapper = await openActionMatrix()

    expect(wrapper.text()).toContain('确认出库')
    expect(wrapper.text()).toContain('上报异常')
    expect(wrapper.text()).toContain('推荐')
    expect(wrapper.text()).not.toContain('确认入库')
    expect(wrapper.find('[data-test="recommended-action-badge"]').exists()).toBe(true)
  })

  it('keeps trace:inbound limited to the backend returned matching action', async () => {
    getTraceAvailableActionsMock.mockResolvedValueOnce({
      traceCode: 'TRACE-001',
      currentStatus: 'IN_TRANSIT',
      currentStatusLabel: '运输中',
      currentNode: '上海仓库',
      recommendedAction: 'INBOUND',
      availableActions: [
        { actionType: 'INBOUND', label: '确认接收/入库', nextStatus: 'IN_STOCK', nextStatusLabel: '在库', requiresRemark: false, permissionHint: 'trace:inbound or trace:scan' }
      ]
    })
    currentUser.permissions = ['trace:inbound', 'trace:view']
    const wrapper = await openActionMatrix()

    expect(wrapper.text()).toContain('确认接收/入库')
    expect(wrapper.text()).not.toContain('确认出库')
    expect(wrapper.text()).not.toContain('确认流转')
    expect(wrapper.text()).not.toContain('上报异常')
  })

  it('never renders the legacy 生产记录 button regardless of permissions', async () => {
    currentUser.permissions = ['trace:scan', 'trace:create', 'trace:view']
    const wrapper = await openActionMatrix()

    expect(wrapper.text()).not.toContain('生产记录')
  })

  it('exposes the 生产赋码 entry on the welcome screen when assignment permission is granted', async () => {
    currentUser.permissions = ['trace:batch:create']
    const wrapper = mountScanHub()
    await flushPromises()

    expect(wrapper.text()).toContain('或者：直接生产赋码')
  })

  it('keeps the 生产赋码 entry visible for legacy trace:create', async () => {
    currentUser.permissions = ['trace:create']
    const wrapper = mountScanHub()
    await flushPromises()

    expect(wrapper.text()).toContain('或者：直接生产赋码')
  })

  it('hides the 生产赋码 entry when assignment permissions are missing', async () => {
    currentUser.permissions = ['trace:scan']
    const wrapper = mountScanHub()
    await flushPromises()

    expect(wrapper.text()).not.toContain('或者：直接生产赋码')
  })

  it('opens the matching flow dialog when the recommended normal action is clicked', async () => {
    getTraceAvailableActionsMock.mockResolvedValueOnce({
      traceCode: 'TRACE-001',
      currentStatus: 'IN_STOCK',
      currentStatusLabel: '在库',
      currentNode: '北京仓库',
      recommendedAction: 'OUTBOUND',
      availableActions: [
        { actionType: 'OUTBOUND', label: '确认出库', nextStatus: 'IN_TRANSIT', nextStatusLabel: '运输中', requiresRemark: false }
      ]
    })
    currentUser.permissions = ['trace:outbound']
    const wrapper = await openActionMatrix()

    await wrapper.find('[data-test="available-action-OUTBOUND"]').trigger('click')
    await flushPromises()

    const dialog = wrapper.find('[data-test="scan-flow-dialog-stub"]')
    expect(dialog.attributes('data-open')).toBe('true')
    expect(dialog.attributes('data-action')).toBe('outbound')
  })

  it('opens exception dialog only when backend returns EXCEPTION as executable', async () => {
    getTraceAvailableActionsMock.mockResolvedValueOnce({
      traceCode: 'TRACE-001',
      currentStatus: 'IN_STOCK',
      currentStatusLabel: '在库',
      currentNode: '北京仓库',
      recommendedAction: 'OUTBOUND',
      availableActions: [
        { actionType: 'EXCEPTION', label: '上报异常', nextStatus: 'EXCEPTION', nextStatusLabel: '异常', requiresRemark: true }
      ]
    })
    currentUser.permissions = ['trace:scan']
    const wrapper = await openActionMatrix()

    await wrapper.find('[data-test="available-action-EXCEPTION"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-test="scan-exception-dialog-stub"]').attributes('data-open')).toBe('true')
  })
})
