import { beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive } from 'vue'
import { flushPromises } from '@vue/test-utils'
import { renderWithPrime } from '@/test/renderWithPrime'
import ScanHub from '@/features/trace/views/ScanHub.vue'

const { pushMock } = vi.hoisted(() => ({
  pushMock: vi.fn()
}))

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

const traceScannerStub = {
  template: `
    <div data-test="trace-scanner-stub">
      <button data-test="emit-scan" @click="$emit('scan', 'TRACE-001')">emit-scan</button>
    </div>
  `
}

const scanFlowDialogStub = {
  template: '<div data-test="scan-flow-dialog-stub"></div>'
}

const scanExceptionDialogStub = {
  template: '<div data-test="scan-exception-dialog-stub"></div>'
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
  })

  it('does not expose scan actions to a view-only user', async () => {
    const wrapper = await openActionMatrix()

    expect(wrapper.text()).toContain('全链路审计详情')
    expect(wrapper.text()).not.toContain('入库登记')
    expect(wrapper.text()).not.toContain('出库登记')
    expect(wrapper.text()).not.toContain('物流流转')
    expect(wrapper.text()).not.toContain('异常上报')
  })

  it('allows trace:scan to expose all scan actions including 异常上报', async () => {
    currentUser.permissions = ['trace:scan', 'trace:view']
    const wrapper = await openActionMatrix()

    expect(wrapper.text()).toContain('入库登记')
    expect(wrapper.text()).toContain('出库登记')
    expect(wrapper.text()).toContain('物流流转')
    expect(wrapper.text()).toContain('异常上报')
  })

  it('keeps trace:inbound limited to the matching action', async () => {
    currentUser.permissions = ['trace:inbound', 'trace:view']
    const wrapper = await openActionMatrix()

    expect(wrapper.text()).toContain('入库登记')
    expect(wrapper.text()).not.toContain('出库登记')
    expect(wrapper.text()).not.toContain('物流流转')
    expect(wrapper.text()).not.toContain('异常上报')
  })

  it('never renders the legacy 生产记录 button regardless of permissions', async () => {
    currentUser.permissions = ['trace:scan', 'trace:create', 'trace:view']
    const wrapper = await openActionMatrix()

    expect(wrapper.text()).not.toContain('生产记录')
  })

  it('exposes the 生产赋码 entry on the welcome screen only when trace:create is granted', async () => {
    currentUser.permissions = ['trace:create']
    const wrapper = mountScanHub()
    await flushPromises()

    expect(wrapper.text()).toContain('或者：直接生产赋码')
  })

  it('hides the 生产赋码 entry when trace:create is missing', async () => {
    currentUser.permissions = ['trace:scan']
    const wrapper = mountScanHub()
    await flushPromises()

    expect(wrapper.text()).not.toContain('或者：直接生产赋码')
  })

  it('exposes 异常上报 button after scanning when trace:scan is granted', async () => {
    currentUser.permissions = ['trace:scan']
    const wrapper = await openActionMatrix()

    expect(wrapper.text()).toContain('异常上报')
  })

  it('hides 异常上报 button when only trace:inbound is granted', async () => {
    currentUser.permissions = ['trace:inbound']
    const wrapper = await openActionMatrix()

    expect(wrapper.text()).not.toContain('异常上报')
  })
})
