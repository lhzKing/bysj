import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { h, reactive } from 'vue'
import { renderWithPrime } from '@/test/renderWithPrime'
import MainLayout from '@/shared/components/layout/MainLayout.vue'

const routeMock = reactive({
  path: '/',
  meta: { title: '仪表盘' }
})

const pushMock = vi.fn()
const replaceMock = vi.fn()
const logoutMock = vi.fn()
const confirmMock = vi.fn()
const toastSuccessMock = vi.fn()

const grantedPermissions = reactive({
  values: ['dashboard:view', 'trace:view', 'part:view']
})

function setMobile(matches) {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    configurable: true,
    value: vi.fn().mockImplementation(() => ({
      matches,
      media: '(max-width: 767px)',
      addEventListener: vi.fn(),
      removeEventListener: vi.fn()
    }))
  })
}

vi.mock('vue-router', () => ({
  useRoute: () => routeMock,
  useRouter: () => ({
    push: pushMock,
    replace: replaceMock
  })
}))

vi.mock('@/core/stores/user', () => ({
  useUserStore: () => ({
    user: {
      username: 'demo-admin'
    },
    logout: logoutMock,
    hasAnyPermission: (requiredPermissions = []) =>
      requiredPermissions.some((permission) => grantedPermissions.values.includes(permission))
  })
}))

vi.mock('@/shared/composables/useConfirm', () => ({
  useConfirm: () => ({
    confirm: confirmMock
  })
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({
    success: toastSuccessMock
  })
}))

const routerViewStub = {
  render() {
    return this.$slots.default?.({
      Component: {
        render() {
          return h('div', { 'data-test': 'layout-view' }, 'Layout page')
        }
      }
    })
  }
}

describe('MainLayout', () => {
  beforeEach(() => {
    routeMock.path = '/'
    routeMock.meta = { title: '仪表盘' }
    grantedPermissions.values = ['dashboard:view', 'trace:view', 'trace:create', 'trace:outbound', 'part:view']
    pushMock.mockReset()
    replaceMock.mockReset()
    logoutMock.mockReset()
    confirmMock.mockReset()
    toastSuccessMock.mockReset()
    setMobile(false)
  })

  it('renders the desktop shell with filtered navigation and routed content', () => {
    const wrapper = renderWithPrime(MainLayout, {
      global: {
        stubs: {
          LogOut: true,
          X: true,
          RouterView: routerViewStub
        }
      }
    })

    expect(wrapper.find('[data-test="app-shell"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="floating-nav"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="desktop-nav"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="content-frame"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="layout-view"]').text()).toContain('Layout page')
    expect(wrapper.text()).toContain('仪表盘')
    expect(wrapper.text()).toContain('生产赋码')
    expect(wrapper.text()).toContain('仓库物流')
    expect(wrapper.text()).toContain('溯源管理')
    expect(wrapper.text()).toContain('配件管理')
    expect(wrapper.text()).not.toContain('用户管理')
    expect(wrapper.text()).not.toContain('角色管理')
    expect(wrapper.text()).toContain('扫码中心')
  })

  it('opens the mobile drawer and closes it after navigation', async () => {
    setMobile(true)
    const wrapper = renderWithPrime(MainLayout, {
      global: {
        stubs: {
          LogOut: true,
          X: true,
          Teleport: true,
          RouterView: routerViewStub
        }
      }
    })

    const mobileNavToggle = wrapper.find('[data-test="mobile-nav-toggle"]')
    expect(mobileNavToggle.exists()).toBe(true)
    await mobileNavToggle.trigger('click')
    expect(wrapper.find('[data-test="mobile-drawer"]').exists()).toBe(true)

    const tracesNavItem = wrapper.find('[data-nav-path="/traces"]')
    expect(tracesNavItem.exists()).toBe(true)
    await tracesNavItem.trigger('click')
    await flushPromises()

    expect(pushMock).toHaveBeenCalledWith('/traces')
    expect(wrapper.find('[data-test="mobile-drawer"]').exists()).toBe(false)
  })

  it('confirms before logging out and redirects back to /login', async () => {
    confirmMock.mockResolvedValue(true)
    const wrapper = renderWithPrime(MainLayout, {
      global: {
        stubs: {
          LogOut: true,
          X: true,
          RouterView: routerViewStub
        }
      }
    })

    const logoutAction = wrapper.find('[data-test="logout-action"]')
    expect(logoutAction.exists()).toBe(true)
    await logoutAction.trigger('click')
    await flushPromises()

    expect(confirmMock).toHaveBeenCalledWith(expect.objectContaining({
      title: '退出登录',
      message: '确定要退出当前账号吗？'
    }))
    expect(logoutMock).toHaveBeenCalled()
    expect(replaceMock).toHaveBeenCalledWith('/login')
    expect(toastSuccessMock).toHaveBeenCalledWith('已退出登录')
  })
})
