import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderWithPrime } from '@/test/renderWithPrime'
import NotFound from '@/shared/components/NotFound.vue'

const { pushMock, isLoggedInRef } = vi.hoisted(() => ({
  pushMock: vi.fn(),
  isLoggedInRef: { value: false }
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: pushMock
  })
}))

vi.mock('@/core/stores/user', () => ({
  useUserStore: () => ({
    get isLoggedIn() {
      return isLoggedInRef.value
    }
  })
}))

describe('NotFound page', () => {
  beforeEach(() => {
    pushMock.mockReset()
    isLoggedInRef.value = false
  })

  it('renders Linear-style 404 shell with brand, title and dual CTA when logged out', () => {
    const wrapper = renderWithPrime(NotFound)
    const text = wrapper.text()

    expect(text).toContain('trace.')
    expect(text).toContain('404 · NOT FOUND')
    expect(text).toContain('页面未找到')
    expect(text).toContain('抱歉，您访问的页面不存在或已被移除')
    expect(text).toContain('© 2026 工业零配件溯源 · 内部系统')

    expect(wrapper.find('[data-test="not-found-page"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="not-found-code"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="not-found-go-dashboard"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="not-found-go-login"]').exists()).toBe(true)
  })

  it('routes to dashboard when primary CTA is clicked', async () => {
    const wrapper = renderWithPrime(NotFound)
    await wrapper.find('[data-test="not-found-go-dashboard"]').trigger('click')

    expect(pushMock).toHaveBeenCalledWith('/')
  })

  it('routes to login when secondary CTA is clicked while logged out', async () => {
    const wrapper = renderWithPrime(NotFound)
    await wrapper.find('[data-test="not-found-go-login"]').trigger('click')

    expect(pushMock).toHaveBeenCalledWith('/login')
  })

  it('hides "返回登录" CTA when user is already logged in', () => {
    isLoggedInRef.value = true
    const wrapper = renderWithPrime(NotFound)

    expect(wrapper.find('[data-test="not-found-go-dashboard"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="not-found-go-login"]').exists()).toBe(false)
  })
})
