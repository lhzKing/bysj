import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { renderWithPrime } from '@/test/renderWithPrime'
import Login from '@/shared/components/Login.vue'

const {
  loginMock,
  hasAnyPermissionMock,
  pushMock,
  promptMock,
  toastSuccessMock,
  toastErrorMock,
  toastInfoMock
} = vi.hoisted(() => ({
  loginMock: vi.fn(),
  hasAnyPermissionMock: vi.fn(),
  pushMock: vi.fn(),
  promptMock: vi.fn(),
  toastSuccessMock: vi.fn(),
  toastErrorMock: vi.fn(),
  toastInfoMock: vi.fn()
}))
const routeMock = { query: { redirect: '/parts' } }

vi.mock('@/core/stores/user', () => ({
  useUserStore: () => ({
    login: loginMock,
    hasAnyPermission: hasAnyPermissionMock
  })
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

vi.mock('@/shared/composables/usePrompt', () => ({
  usePrompt: () => ({
    prompt: promptMock
  })
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({
    success: toastSuccessMock,
    error: toastErrorMock,
    info: toastInfoMock
  })
}))

describe('Login view', () => {
  beforeEach(() => {
    routeMock.query = { redirect: '/parts' }
    loginMock.mockReset()
    hasAnyPermissionMock.mockReset()
    hasAnyPermissionMock.mockReturnValue(true)
    pushMock.mockReset()
    promptMock.mockReset()
    toastSuccessMock.mockReset()
    toastErrorMock.mockReset()
    toastInfoMock.mockReset()
  })

  it('renders Linear-style login shell with brand, title and audit links', () => {
    const wrapper = renderWithPrime(Login)
    const pageText = wrapper.text()

    expect(pageText).toContain('trace.')
    expect(pageText).toContain('登录到 trace.')
    expect(pageText).toContain('输入工号与密码访问溯源系统')
    expect(pageText).toContain('14 天内保持登录')
    expect(pageText).toContain('外部审计 · 无需登录')
    expect(pageText).toContain('通过追溯码自助验签')
    expect(pageText).toContain('下载 RSA 公钥')
    expect(pageText).toContain('© 2026 工业零配件溯源 · 内部系统')

    expect(wrapper.find('[data-test="login-username"] input').exists()).toBe(true)
    expect(wrapper.find('[data-test="login-password"] input').exists()).toBe(true)
    expect(wrapper.find('[data-test="login-submit"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="audit-public-key"]').attributes('href')).toBe('/api/traces/public-key')
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

  it('maps backend status codes to Linear-style inline error messages', async () => {
    const error401 = new Error('server')
    error401.response = { status: 401 }
    loginMock.mockRejectedValueOnce(error401)
    const wrapper = renderWithPrime(Login)

    await wrapper.find('[data-test="login-username"] input').setValue('admin')
    await wrapper.find('[data-test="login-password"] input').setValue('wrong')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()
    expect(wrapper.find('[data-test="login-error"]').text()).toContain('用户名或密码错误')

    const error403 = new Error('server')
    error403.response = { status: 403 }
    loginMock.mockRejectedValueOnce(error403)
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()
    expect(wrapper.find('[data-test="login-error"]').text()).toContain('账号已禁用')

    const error429 = new Error('server')
    error429.response = { status: 429 }
    loginMock.mockRejectedValueOnce(error429)
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()
    expect(wrapper.find('[data-test="login-error"]').text()).toContain('登录尝试过于频繁')
  })

  it('locks submission for 5 seconds after three consecutive failures', async () => {
    vi.useFakeTimers()
    const error401 = new Error('server')
    error401.response = { status: 401 }
    loginMock.mockRejectedValue(error401)
    const wrapper = renderWithPrime(Login)

    await wrapper.find('[data-test="login-username"] input').setValue('admin')
    await wrapper.find('[data-test="login-password"] input').setValue('wrong')

    for (let attempt = 0; attempt < 3; attempt += 1) {
      await wrapper.find('form').trigger('submit.prevent')
      await flushPromises()
    }

    const submit = wrapper.find('[data-test="login-submit"]')
    expect(submit.attributes('disabled')).toBeDefined()
    expect(submit.text()).toContain('请等待 5s 后重试')

    vi.advanceTimersByTime(5000)
    await flushPromises()
    expect(wrapper.find('[data-test="login-submit"]').text()).toContain('登录')
    vi.useRealTimers()
  })

  it('opens the audit verify prompt and routes to public trace verify view', async () => {
    promptMock.mockResolvedValue('TC-260505-A8F3K2')
    const wrapper = renderWithPrime(Login)

    await wrapper.find('[data-test="audit-verify"]').trigger('click')
    await flushPromises()

    expect(promptMock).toHaveBeenCalledWith(expect.objectContaining({
      title: '追溯码自助验签'
    }))
    expect(pushMock).toHaveBeenCalledWith({
      path: '/public/traces/TC-260505-A8F3K2'
    })
  })

  it('removes the "需要帮助？" header link (cleanup)', () => {
    const wrapper = renderWithPrime(Login)
    expect(wrapper.text()).not.toContain('需要帮助？')
  })

  it('shows an info toast when "联系管理员注册" is clicked (no longer silent)', async () => {
    const wrapper = renderWithPrime(Login)

    const contact = wrapper.find('[data-test="login-contact-admin"]')
    expect(contact.exists()).toBe(true)
    expect(contact.text()).toContain('联系管理员注册')
    await contact.trigger('click')

    expect(toastInfoMock).toHaveBeenCalledTimes(1)
    expect(toastInfoMock.mock.calls[0][0]).toMatch(/管理员/)
  })

  it('shows a no-access inline error when redirected back with ?error=no-access', async () => {
    routeMock.query = { error: 'no-access' }
    const wrapper = renderWithPrime(Login)
    await flushPromises()

    expect(wrapper.find('[data-test="login-error"]').text()).toContain('没有任何可访问的页面')
  })

  it('falls back to the first accessible route when no redirect query is set', async () => {
    routeMock.query = {}
    loginMock.mockResolvedValue(true)
    // 模拟用户只有 trace:view（USER 角色） —— /scan-trace 是新的首个匹配候选
    hasAnyPermissionMock.mockImplementation((perms) => perms.includes('trace:view'))
    const wrapper = renderWithPrime(Login)

    await wrapper.find('[data-test="login-username"] input').setValue('user')
    await wrapper.find('[data-test="login-password"] input').setValue('pwd')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(pushMock).toHaveBeenCalledWith('/scan-trace')
  })
})
