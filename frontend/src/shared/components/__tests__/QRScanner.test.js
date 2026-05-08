import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

vi.mock('vue-qrcode-reader', () => ({
  QrcodeStream: {
    name: 'QrcodeStream',
    template: `<div data-test="qrcode-stream-stub"></div>`,
    emits: ['detect', 'camera-on', 'camera-off', 'error'],
    props: ['constraints', 'torch']
  }
}))

import QRScanner from '@/shared/components/QRScanner.vue'

const flush = async () => {
  await nextTick()
  await nextTick()
}

describe('QRScanner', () => {
  beforeEach(() => {
    document.body.style.overflow = ''
    Object.defineProperty(navigator, 'mediaDevices', {
      configurable: true,
      value: {
        enumerateDevices: vi.fn().mockResolvedValue([
          { kind: 'videoinput', deviceId: 'cam-front', label: 'Front' },
          { kind: 'videoinput', deviceId: 'cam-rear', label: 'Rear' }
        ])
      }
    })
  })

  afterEach(() => {
    document.body.style.overflow = ''
    vi.restoreAllMocks()
  })

  it('renders modal shell with backdrop and close button by default', () => {
    const wrapper = mount(QRScanner, { attachTo: document.body })
    expect(wrapper.find('[data-test="qr-scanner"]').classes()).toContain('qr-scanner--modal')
    expect(wrapper.find('[data-test="qr-scanner-backdrop"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="qr-scanner-close"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('renders inline shell without backdrop or close button when inline prop is set', () => {
    const wrapper = mount(QRScanner, {
      props: { inline: true },
      attachTo: document.body
    })
    expect(wrapper.find('[data-test="qr-scanner"]').classes()).toContain('qr-scanner--inline')
    expect(wrapper.find('[data-test="qr-scanner-backdrop"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="qr-scanner-close"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('emits scan(code) when QrcodeStream detects a value', () => {
    const wrapper = mount(QRScanner, { attachTo: document.body })
    const stream = wrapper.findComponent({ name: 'QrcodeStream' })
    stream.vm.$emit('detect', [{ rawValue: 'TC-260505-A8F3K2' }])
    expect(wrapper.emitted('scan')).toEqual([['TC-260505-A8F3K2']])
    wrapper.unmount()
  })

  it('emits close on backdrop click and on close button click', async () => {
    const wrapper = mount(QRScanner, { attachTo: document.body })
    await wrapper.find('[data-test="qr-scanner-backdrop"]').trigger('click')
    await wrapper.find('[data-test="qr-scanner-close"]').trigger('click')
    expect(wrapper.emitted('close')?.length).toBe(2)
    wrapper.unmount()
  })

  it('shows permission error with guide link and emits error event', async () => {
    const wrapper = mount(QRScanner, { attachTo: document.body })
    const stream = wrapper.findComponent({ name: 'QrcodeStream' })
    const err = new DOMException('denied', 'NotAllowedError')
    stream.vm.$emit('error', err)
    await flush()
    const errBlock = wrapper.find('[data-test="qr-scanner-error"]')
    expect(errBlock.exists()).toBe(true)
    expect(errBlock.text()).toContain('无法访问摄像头')
    expect(errBlock.text()).toContain('刷新页面')
    expect(wrapper.find('a.qr-card__state-btn--ghost').exists()).toBe(true)
    expect(wrapper.emitted('error')?.[0]?.[0]).toBe(err)
    wrapper.unmount()
  })

  it('maps NotSupportedError to insecure guidance', async () => {
    const wrapper = mount(QRScanner, { attachTo: document.body })
    wrapper.findComponent({ name: 'QrcodeStream' }).vm.$emit(
      'error',
      new DOMException('https only', 'NotSupportedError')
    )
    await flush()
    expect(wrapper.find('[data-test="qr-scanner-error"]').text()).toContain('HTTPS')
    wrapper.unmount()
  })

  it('retry button restores streaming attempt and clears error state', async () => {
    const wrapper = mount(QRScanner, { attachTo: document.body })
    wrapper.findComponent({ name: 'QrcodeStream' }).vm.$emit(
      'error',
      new DOMException('blocked', 'NotAllowedError')
    )
    await flush()
    expect(wrapper.find('[data-test="qr-scanner-error"]').exists()).toBe(true)
    await wrapper.find('[data-test="qr-scanner-retry"]').trigger('click')
    expect(wrapper.find('[data-test="qr-scanner-error"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="qrcode-stream-stub"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="qr-scanner-loading"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('switch camera button is enabled after camera-on enumerates multiple devices', async () => {
    const wrapper = mount(QRScanner, { attachTo: document.body })
    wrapper.findComponent({ name: 'QrcodeStream' }).vm.$emit('camera-on', { torch: false })
    await flush()
    const switchBtn = wrapper.find('[data-test="qr-scanner-switch"]')
    expect(switchBtn.attributes('disabled')).toBeUndefined()
    const torchBtn = wrapper.find('[data-test="qr-scanner-torch"]')
    expect(torchBtn.attributes('disabled')).toBeDefined()
    wrapper.unmount()
  })

  it('torch button activates when capabilities expose torch', async () => {
    const wrapper = mount(QRScanner, { attachTo: document.body })
    wrapper.findComponent({ name: 'QrcodeStream' }).vm.$emit('camera-on', { torch: true })
    await flush()
    expect(wrapper.find('[data-test="qr-scanner-torch"]').attributes('disabled')).toBeUndefined()
    await wrapper.find('[data-test="qr-scanner-torch"]').trigger('click')
    await flush()
    expect(wrapper.find('[data-test="qr-scanner-torch"]').classes()).toContain('is-active')
    wrapper.unmount()
  })
})
