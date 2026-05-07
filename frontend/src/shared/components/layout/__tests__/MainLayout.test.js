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

function setCompactViewport(matches) {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    configurable: true,
    value: vi.fn().mockImplementation(() => ({
      matches,
      media: '(max-width: 1023.98px)',
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
      username: 'demo-admin',
      roleName: '管理员'
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
    setCompactViewport(false)
  })

  it('renders the desktop shell with sidebar, topbar and routed content', () => {
    const wrapper = renderWithPrime(MainLayout, {
      global: {
        stubs: {
          RouterView: routerViewStub
        }
      }
    })

    expect(wrapper.find('[data-test="app-shell"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="app-sidebar"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="app-topbar"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="app-content"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="layout-view"]').text()).toContain('Layout page')

    const sidebarText = wrapper.find('[data-test="app-sidebar"]').text()
    expect(sidebarText).toContain('trace.')
    expect(sidebarText).toContain('仪表盘')
    expect(sidebarText).toContain('扫码工位')
    expect(sidebarText).toContain('生产赋码')
    expect(sidebarText).toContain('仓库物流')
    expect(sidebarText).toContain('追溯查询')
    expect(sidebarText).toContain('配件管理')
    expect(sidebarText).not.toContain('用户管理')
    expect(sidebarText).not.toContain('角色管理')

    expect(wrapper.find('[data-test="page-title"]').text()).toBe('仪表盘')
  })

  it('hides desktop sidebar on compact viewport and exposes hamburger toggle', async () => {
    setCompactViewport(true)
    const wrapper = renderWithPrime(MainLayout, {
      global: {
        stubs: {
          Teleport: true,
          RouterView: routerViewStub
        }
      }
    })

    expect(wrapper.find('[data-test="app-sidebar"]').exists()).toBe(false)

    const toggle = wrapper.find('[data-test="mobile-nav-toggle"]')
    expect(toggle.exists()).toBe(true)
    await toggle.trigger('click')

    const drawer = wrapper.find('[data-test="mobile-sidebar-drawer"]')
    expect(drawer.exists()).toBe(true)

    const tracesEntry = drawer.find('[data-nav-path="/traces"]')
    expect(tracesEntry.exists()).toBe(true)
    await tracesEntry.trigger('click')
    await flushPromises()

    expect(pushMock).toHaveBeenCalledWith('/traces')
    expect(wrapper.find('[data-test="mobile-sidebar-drawer"]').exists()).toBe(false)
  })

  it('confirms before logging out and redirects back to /login', async () => {
    confirmMock.mockResolvedValue(true)
    const wrapper = renderWithPrime(MainLayout, {
      global: {
        stubs: {
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
