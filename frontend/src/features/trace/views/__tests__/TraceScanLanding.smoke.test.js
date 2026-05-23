import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import TraceScanLanding from '@/features/trace/views/TraceScanLanding.vue'

const { routerPushMock, toastErrorMock } = vi.hoisted(() => ({
  routerPushMock: vi.fn(),
  toastErrorMock: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPushMock })
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({ error: toastErrorMock, success: vi.fn(), info: vi.fn(), warning: vi.fn() })
}))

// QRScanner 用摄像头 API，在 jsdom 里跑不动 —— stub 掉只看交互。
// 加 name 字段让 findComponent({ name: 'QRScanner' }) 能命中，从而触发 emit。
const qrScannerStub = {
  name: 'QRScanner',
  template: `<div data-test="qr-scanner-stub"></div>`,
  emits: ['scan', 'close']
}

function mountLanding() {
  return mount(TraceScanLanding, {
    global: {
      stubs: { QRScanner: qrScannerStub }
    }
  })
}

describe('TraceScanLanding (USER 角色个人扫码落地页)', () => {
  beforeEach(() => {
    routerPushMock.mockReset()
    toastErrorMock.mockReset()
  })

  it('renders hero + manual-input sections with both data-test hooks', () => {
    const wrapper = mountLanding()
    expect(wrapper.find('[data-testid="scan-landing-hero"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="scan-landing-manual"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="scan-landing-open-camera"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="scan-landing-manual-input"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="scan-landing-manual-submit"]').exists()).toBe(true)
  })

  it('clicking camera button mounts the QRScanner; emitting scan routes to /traces/<code>', async () => {
    const wrapper = mountLanding()

    // 默认未打开
    expect(wrapper.find('[data-test="qr-scanner-stub"]').exists()).toBe(false)

    await wrapper.find('[data-test="scan-landing-open-camera"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-test="qr-scanner-stub"]').exists()).toBe(true)

    // 通过 stub 引用查找（stubs option 替换 QRScanner 后，name 仍指向 stub.name='QRScanner'）
    const scannerComponent = wrapper.findComponent(qrScannerStub)
    expect(scannerComponent.exists()).toBe(true)
    scannerComponent.vm.$emit('scan', 'TC-260505-A8F3K2')
    await flushPromises()

    expect(routerPushMock).toHaveBeenCalledWith('/traces/TC-260505-A8F3K2')
    // scanner 自动关闭
    expect(wrapper.find('[data-test="qr-scanner-stub"]').exists()).toBe(false)
  })

  it('manual-input submission routes to /traces/<code>', async () => {
    const wrapper = mountLanding()

    await wrapper.find('[data-test="scan-landing-manual-input"] input').setValue('TC-MANUAL-001')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(routerPushMock).toHaveBeenCalledWith('/traces/TC-MANUAL-001')
  })

  it('rejects empty manual input with a toast and does NOT navigate', async () => {
    const wrapper = mountLanding()
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(routerPushMock).not.toHaveBeenCalled()
    expect(toastErrorMock).toHaveBeenCalledWith('请输入追溯码')
  })

  it('trims whitespace from manual input before navigating', async () => {
    const wrapper = mountLanding()
    await wrapper.find('[data-test="scan-landing-manual-input"] input').setValue('  TC-PADDED  ')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(routerPushMock).toHaveBeenCalledWith('/traces/TC-PADDED')
  })
})
