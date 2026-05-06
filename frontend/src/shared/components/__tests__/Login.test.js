import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { renderWithPrime } from '@/test/renderWithPrime'
import Login from '@/shared/components/Login.vue'

const {
  loginMock,
  registerMock,
  pushMock,
  toastSuccessMock,
  toastErrorMock
} = vi.hoisted(() => ({
  loginMock: vi.fn(),
  registerMock: vi.fn(),
  pushMock: vi.fn(),
  toastSuccessMock: vi.fn(),
  toastErrorMock: vi.fn()
}))
const routeMock = { query: { redirect: '/parts' } }

vi.mock('@/core/stores/user', () => ({
  useUserStore: () => ({
    login: loginMock
  })
}))

vi.mock('@/core/api/auth', () => ({
  register: registerMock
}))

vi.mock('vue-router', () => ({
  useRoute: () => routeMock,
  useRouter: () => ({
    push: pushMock,
    currentRoute: {
      value: routeMock
    }
  })
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({
    success: toastSuccessMock,
    error: toastErrorMock
  })
}))

describe('Login view', () => {
  beforeEach(() => {
    routeMock.query = { redirect: '/parts' }
    loginMock.mockReset()
    registerMock.mockReset()
    pushMock.mockReset()
    toastSuccessMock.mockReset()
    toastErrorMock.mockReset()
  })

  it('renders the approved enterprise copy and login controls', () => {
    const wrapper = renderWithPrime(Login)
    const pageText = wrapper.text()

    expect(pageText).toContain('工业配件供应链溯源系统')
    expect(pageText).toContain('毕业设计展示')
    expect(pageText).toContain('演示账号请联系管理员或查看项目说明')
    expect(pageText).toContain('© 2026 工业配件供应链溯源系统')
    expect(wrapper.find('[data-test="login-username"] input').exists()).toBe(true)
    expect(wrapper.find('[data-test="login-password"] input').exists()).toBe(true)
    expect(wrapper.find('[data-test="login-submit"]').exists()).toBe(true)
  })

  it('submits credentials and redirects to the requested route on success', async () => {
    loginMock.mockResolvedValue(true)
    const wrapper = renderWithPrime(Login)

    await wrapper.find('[data-test="login-username"] input').setValue('admin')
    await wrapper.find('[data-test="login-password"] input').setValue('123456')
    await wrapper.find('[data-test="remember-me"]').setValue(true)
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(loginMock).toHaveBeenCalledWith('admin', '123456', true)
    expect(pushMock).toHaveBeenCalledWith('/parts')
    expect(toastSuccessMock).toHaveBeenCalledWith('登录成功')
  })

  it('shows inline error feedback when login fails', async () => {
    loginMock.mockRejectedValue(new Error('用户名或密码错误'))
    const wrapper = renderWithPrime(Login)

    await wrapper.find('[data-test="login-username"] input').setValue('admin')
    await wrapper.find('[data-test="login-password"] input').setValue('bad-pass')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(wrapper.find('[data-test="login-error"]').text()).toContain('用户名或密码错误')
    expect(pushMock).not.toHaveBeenCalled()
    expect(toastErrorMock).toHaveBeenCalledWith('用户名或密码错误')
  })

  it('submits self-service registration via auth register instead of management create-user api', async () => {
    registerMock.mockResolvedValue({
      token: 'register-token',
      username: 'new_user',
      role: 'USER'
    })
    const wrapper = renderWithPrime(Login)

    const modeToggleButton = wrapper.findAll('button[type="button"]')[1]
    await modeToggleButton.trigger('click')
    await wrapper.find('[data-test="login-username"] input').setValue('new_user')
    await wrapper.find('[data-test="login-password"] input').setValue('abc123')
    await wrapper.find('[data-test="register-confirm-password"] input').setValue('abc123')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(registerMock).toHaveBeenCalledWith('new_user', 'abc123')
    expect(loginMock).not.toHaveBeenCalled()
    expect(pushMock).not.toHaveBeenCalled()
    expect(toastSuccessMock).toHaveBeenCalledWith('注册成功，请登录')
    expect(wrapper.find('[data-test="register-confirm-password"]').exists()).toBe(false)
  })
})
