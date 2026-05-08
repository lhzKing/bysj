import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive, nextTick } from 'vue'
import { flushPromises } from '@vue/test-utils'
import { renderWithPrime } from '@/test/renderWithPrime'
import ScanHub from '@/features/trace/views/ScanHub.vue'

const { pushMock } = vi.hoisted(() => ({ pushMock: vi.fn() }))

const getTraceAvailableActionsMock = vi.fn()

const currentUser = reactive({
  permissions: ['trace:scan', 'trace:view']
})

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    useRouter: () => ({ push: pushMock })
  }
})

vi.mock('@/core/stores/user', () => ({
  useUserStore: () => ({ user: currentUser })
}))

vi.mock('@/features/trace/api', () => ({
  getTraceAvailableActions: (...args) => getTraceAvailableActionsMock(...args)
}))

const qrScannerStub = {
  template: `
    <div data-test="qr-scanner-stub">
      <button data-test="emit-scan" @click="$emit('scan', 'TRC-260507-A8F3K2')">emit-scan</button>
      <button data-test="emit-close" @click="$emit('close')">emit-close</button>
    </div>
  `
}

const scanFlowDialogStub = {
  props: ['modelValue', 'actionType', 'idempotencyKey', 'traceCode'],
  template:
    '<div data-test="scan-flow-dialog-stub" :data-open="modelValue" :data-action="actionType" :data-idem="idempotencyKey" :data-trace="traceCode"></div>'
}

const scanExceptionDialogStub = {
  props: ['modelValue', 'idempotencyKey', 'traceCode'],
  template:
    '<div data-test="scan-exception-dialog-stub" :data-open="modelValue" :data-idem="idempotencyKey" :data-trace="traceCode"></div>'
}

const createTraceDialogStub = {
  props: ['modelValue'],
  template: '<div data-test="create-trace-dialog-stub" :data-open="modelValue"></div>'
}

const stubs = {
  QRScanner: qrScannerStub,
  ScanFlowDialog: scanFlowDialogStub,
  ScanExceptionDialog: scanExceptionDialogStub,
  CreateTraceDialog: createTraceDialogStub
}

function mountScanHub() {
  return renderWithPrime(ScanHub, { global: { stubs } })
}

async function gotoScanned() {
  const wrapper = mountScanHub()
  await wrapper.find('[data-test="scan-start"]').trigger('click')
  await flushPromises()
  await wrapper.find('[data-test="emit-scan"]').trigger('click')
  await flushPromises()
  return wrapper
}

describe('ScanHub /available-actions contract', () => {
  let randomUUIDSpy

  beforeEach(() => {
    currentUser.permissions = ['trace:scan', 'trace:view']
    pushMock.mockReset()
    getTraceAvailableActionsMock.mockReset()
    randomUUIDSpy = vi.spyOn(crypto, 'randomUUID').mockReturnValue('uuid-fixed-1234')
  })

  afterEach(() => {
    randomUUIDSpy.mockRestore()
  })

  it('calls /available-actions with the scanned code immediately after scan', async () => {
    getTraceAvailableActionsMock.mockResolvedValueOnce({
      traceCode: 'TRC-260507-A8F3K2',
      currentStatus: 'IN_TRANSIT',
      currentStatusLabel: '运输中',
      currentNode: '上海仓库',
      recommendedAction: 'INBOUND',
      availableActions: [
        { actionType: 'INBOUND', label: '确认入库', nextStatus: 'IN_STOCK', nextStatusLabel: '在库', requiresRemark: false }
      ]
    })

    const wrapper = await gotoScanned()

    expect(getTraceAvailableActionsMock).toHaveBeenCalledTimes(1)
    expect(getTraceAvailableActionsMock).toHaveBeenCalledWith('TRC-260507-A8F3K2')
    expect(wrapper.find('[data-test="available-action-INBOUND"]').exists()).toBe(true)
  })

  it('renders only backend availableActions and no hardcoded inbound/outbound/transfer', async () => {
    getTraceAvailableActionsMock.mockResolvedValueOnce({
      traceCode: 'TRC-260507-A8F3K2',
      currentStatus: 'IN_STOCK',
      recommendedAction: 'OUTBOUND',
      availableActions: [
        { actionType: 'OUTBOUND', label: '确认出库', nextStatus: 'IN_TRANSIT' }
      ]
    })

    const wrapper = await gotoScanned()

    expect(wrapper.find('[data-test="available-action-OUTBOUND"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="available-action-INBOUND"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="available-action-TRANSFER"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="available-action-EXCEPTION_OPEN"]').exists()).toBe(false)
  })

  it('orders the recommended action first regardless of API list order', async () => {
    getTraceAvailableActionsMock.mockResolvedValueOnce({
      traceCode: 'TRC-260507-A8F3K2',
      currentStatus: 'IN_STOCK',
      recommendedAction: 'OUTBOUND',
      availableActions: [
        { actionType: 'EXCEPTION_OPEN', label: '上报异常并冻结', nextStatus: 'EXCEPTION', requiresRemark: true },
        { actionType: 'OUTBOUND', label: '确认出库', nextStatus: 'IN_TRANSIT' }
      ]
    })

    const wrapper = await gotoScanned()
    const buttons = wrapper.findAll('[data-test^="available-action-"]')

    expect(buttons.length).toBe(2)
    expect(buttons[0].attributes('data-test')).toBe('available-action-OUTBOUND')
    expect(buttons[1].attributes('data-test')).toBe('available-action-EXCEPTION_OPEN')
  })

  it('passes a fresh crypto.randomUUID idempotencyKey to ScanFlowDialog when an action is picked', async () => {
    getTraceAvailableActionsMock.mockResolvedValueOnce({
      traceCode: 'TRC-260507-A8F3K2',
      currentStatus: 'IN_STOCK',
      recommendedAction: 'OUTBOUND',
      availableActions: [
        { actionType: 'OUTBOUND', label: '确认出库', nextStatus: 'IN_TRANSIT' }
      ]
    })

    const wrapper = await gotoScanned()
    expect(randomUUIDSpy).not.toHaveBeenCalled()

    await wrapper.find('[data-test="available-action-OUTBOUND"]').trigger('click')
    await flushPromises()

    expect(randomUUIDSpy).toHaveBeenCalledTimes(1)
    const dialog = wrapper.find('[data-test="scan-flow-dialog-stub"]')
    expect(dialog.attributes('data-open')).toBe('true')
    expect(dialog.attributes('data-action')).toBe('outbound')
    expect(dialog.attributes('data-idem')).toBe('uuid-fixed-1234')
    expect(dialog.attributes('data-trace')).toBe('TRC-260507-A8F3K2')
  })

  it('passes the same fresh idempotencyKey to ScanExceptionDialog for EXCEPTION_OPEN', async () => {
    getTraceAvailableActionsMock.mockResolvedValueOnce({
      traceCode: 'TRC-260507-A8F3K2',
      currentStatus: 'IN_STOCK',
      recommendedAction: 'EXCEPTION_OPEN',
      availableActions: [
        { actionType: 'EXCEPTION_OPEN', label: '上报异常并冻结', nextStatus: 'EXCEPTION', requiresRemark: true }
      ]
    })

    const wrapper = await gotoScanned()

    await wrapper.find('[data-test="available-action-EXCEPTION_OPEN"]').trigger('click')
    await flushPromises()

    expect(randomUUIDSpy).toHaveBeenCalledTimes(1)
    const dialog = wrapper.find('[data-test="scan-exception-dialog-stub"]')
    expect(dialog.attributes('data-open')).toBe('true')
    expect(dialog.attributes('data-idem')).toBe('uuid-fixed-1234')
    expect(dialog.attributes('data-trace')).toBe('TRC-260507-A8F3K2')
  })

  it('regenerates idempotencyKey for each action pick (no key reuse across submissions)', async () => {
    getTraceAvailableActionsMock.mockResolvedValue({
      traceCode: 'TRC-260507-A8F3K2',
      currentStatus: 'IN_STOCK',
      recommendedAction: 'OUTBOUND',
      availableActions: [
        { actionType: 'OUTBOUND', label: '确认出库', nextStatus: 'IN_TRANSIT' }
      ]
    })

    randomUUIDSpy.mockReturnValueOnce('uuid-call-A').mockReturnValueOnce('uuid-call-B')

    const wrapper = await gotoScanned()

    await wrapper.find('[data-test="available-action-OUTBOUND"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-test="scan-flow-dialog-stub"]').attributes('data-idem')).toBe('uuid-call-A')

    // second pick re-uses the panel and should rotate the key
    await wrapper.find('[data-test="available-action-OUTBOUND"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-test="scan-flow-dialog-stub"]').attributes('data-idem')).toBe('uuid-call-B')
  })

  it('binds F1-F4 to the first 4 ordered executable actions', async () => {
    getTraceAvailableActionsMock.mockResolvedValueOnce({
      traceCode: 'TRC-260507-A8F3K2',
      currentStatus: 'IN_STOCK',
      recommendedAction: 'OUTBOUND',
      availableActions: [
        { actionType: 'OUTBOUND', label: '确认出库', nextStatus: 'IN_TRANSIT' },
        { actionType: 'EXCEPTION_OPEN', label: '上报异常并冻结', nextStatus: 'EXCEPTION', requiresRemark: true }
      ]
    })

    const wrapper = await gotoScanned()
    // F1 → OUTBOUND (recommended, first slot)
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'F1' }))
    await nextTick()
    await flushPromises()

    expect(wrapper.find('[data-test="scan-flow-dialog-stub"]').attributes('data-open')).toBe('true')
    expect(wrapper.find('[data-test="scan-flow-dialog-stub"]').attributes('data-action')).toBe('outbound')

    wrapper.unmount()
  })

  it('F2 dispatches the second ordered action when present (EXCEPTION_OPEN here)', async () => {
    getTraceAvailableActionsMock.mockResolvedValueOnce({
      traceCode: 'TRC-260507-A8F3K2',
      currentStatus: 'IN_STOCK',
      recommendedAction: 'OUTBOUND',
      availableActions: [
        { actionType: 'OUTBOUND', label: '确认出库', nextStatus: 'IN_TRANSIT' },
        { actionType: 'EXCEPTION_OPEN', label: '上报异常并冻结', nextStatus: 'EXCEPTION', requiresRemark: true }
      ]
    })

    const wrapper = await gotoScanned()
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'F2' }))
    await nextTick()
    await flushPromises()

    expect(wrapper.find('[data-test="scan-exception-dialog-stub"]').attributes('data-open')).toBe('true')

    wrapper.unmount()
  })

  it('Esc on identified state resets back to idle (default CTA visible again)', async () => {
    getTraceAvailableActionsMock.mockResolvedValueOnce({
      traceCode: 'TRC-260507-A8F3K2',
      currentStatus: 'IN_STOCK',
      recommendedAction: 'OUTBOUND',
      availableActions: [
        { actionType: 'OUTBOUND', label: '确认出库', nextStatus: 'IN_TRANSIT' }
      ]
    })

    const wrapper = await gotoScanned()
    expect(wrapper.find('[data-test="scan-action-panel"]').exists()).toBe(true)

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await nextTick()

    expect(wrapper.find('[data-test="scan-action-panel"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="scan-start"]').exists()).toBe(true)

    wrapper.unmount()
  })

  it('manual code submission also calls /available-actions and skips camera', async () => {
    getTraceAvailableActionsMock.mockResolvedValueOnce({
      traceCode: 'TRC-MANUAL-001',
      currentStatus: 'IN_STOCK',
      recommendedAction: 'OUTBOUND',
      availableActions: [
        { actionType: 'OUTBOUND', label: '确认出库', nextStatus: 'IN_TRANSIT' }
      ]
    })

    const wrapper = mountScanHub()
    await wrapper.find('[data-test="scan-manual-input"] input').setValue('TRC-MANUAL-001')
    await wrapper.find('[data-test="scan-manual-form"]').trigger('submit.prevent')
    await flushPromises()

    expect(getTraceAvailableActionsMock).toHaveBeenCalledWith('TRC-MANUAL-001')
    expect(wrapper.find('[data-test="scan-action-panel"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="qr-scanner-stub"]').exists()).toBe(false)
  })
})
