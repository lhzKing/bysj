import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { renderWithPrime } from '@/test/renderWithPrime'
import CameraTest from '@/views/CameraTest.vue'

const { pushMock, backMock } = vi.hoisted(() => ({
  pushMock: vi.fn(),
  backMock: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: pushMock,
    back: backMock
  })
}))

const enumerateMock = vi.fn()
const getUserMediaMock = vi.fn()

describe('CameraTest page', () => {
  let originalHistoryLength

  beforeEach(() => {
    pushMock.mockReset()
    backMock.mockReset()
    enumerateMock.mockReset()
    getUserMediaMock.mockReset()

    enumerateMock.mockResolvedValue([
      { kind: 'videoinput', deviceId: 'cam-front', label: 'Front Camera' },
      { kind: 'videoinput', deviceId: 'cam-rear', label: 'Rear Camera' },
      { kind: 'audioinput', deviceId: 'mic', label: 'Mic' }
    ])
    getUserMediaMock.mockRejectedValue(
      Object.assign(new Error('denied'), { name: 'NotAllowedError' })
    )

    Object.defineProperty(navigator, 'mediaDevices', {
      configurable: true,
      value: {
        enumerateDevices: enumerateMock,
        getUserMedia: getUserMediaMock
      }
    })

    originalHistoryLength = window.history.length
    Object.defineProperty(window.history, 'length', {
      configurable: true,
      value: 5
    })
  })

  afterEach(() => {
    Object.defineProperty(window.history, 'length', {
      configurable: true,
      value: originalHistoryLength
    })
    vi.restoreAllMocks()
  })

  it('renders Linear-style header, viewer card and side panels', async () => {
    const wrapper = renderWithPrime(CameraTest, { attachTo: document.body })
    await flushPromises()
    const text = wrapper.text()

    expect(text).toContain('trace.')
    expect(text).toContain('CAMERA · DEVICE TEST')
    expect(text).toContain('摄像头探测')
    expect(text).toContain('控制台')
    expect(text).toContain('设备信息')
    expect(text).toContain('未启动')
    expect(text).toContain('后置摄像头')

    expect(wrapper.find('[data-test="camera-test-page"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="camera-test-viewer"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="camera-test-controls"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="camera-test-info"]').exists()).toBe(true)

    wrapper.unmount()
  })

  it('lists available video inputs from enumerateDevices on mount', async () => {
    const wrapper = renderWithPrime(CameraTest, { attachTo: document.body })
    await flushPromises()

    expect(enumerateMock).toHaveBeenCalled()
    expect(wrapper.find('[data-test="camera-test-camera-count"]').text()).toBe('2')
    wrapper.unmount()
  })

  it('shows error block with permission guidance when getUserMedia is denied', async () => {
    const wrapper = renderWithPrime(CameraTest, { attachTo: document.body })
    await flushPromises()

    await wrapper.find('[data-test="camera-test-start"]').trigger('click')
    await flushPromises()

    const errBlock = wrapper.find('[data-test="camera-test-error"]')
    expect(errBlock.exists()).toBe(true)
    expect(errBlock.text()).toContain('摄像头权限被拒绝')
    expect(getUserMediaMock).toHaveBeenCalledTimes(1)
    wrapper.unmount()
  })

  it('disables stop / switch / capture controls until camera is active', async () => {
    const wrapper = renderWithPrime(CameraTest, { attachTo: document.body })
    await flushPromises()

    expect(wrapper.find('[data-test="camera-test-stop"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-test="camera-test-switch"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-test="camera-test-capture"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-test="camera-test-start"]').attributes('disabled')).toBeUndefined()

    wrapper.unmount()
  })

  it('navigates back via router.back when history allows', async () => {
    const wrapper = renderWithPrime(CameraTest, { attachTo: document.body })
    await flushPromises()

    await wrapper.find('[data-test="camera-test-back"]').trigger('click')

    expect(backMock).toHaveBeenCalledTimes(1)
    expect(pushMock).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('falls back to dashboard push when history is empty', async () => {
    Object.defineProperty(window.history, 'length', {
      configurable: true,
      value: 1
    })
    const wrapper = renderWithPrime(CameraTest, { attachTo: document.body })
    await flushPromises()

    await wrapper.find('[data-test="camera-test-back"]').trigger('click')

    expect(pushMock).toHaveBeenCalledWith('/')
    expect(backMock).not.toHaveBeenCalled()
    wrapper.unmount()
  })
})
