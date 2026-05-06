import { describe, expect, it } from 'vitest'
import { renderWithPrime } from '@/test/renderWithPrime'
import App from '@/App.vue'
import Toast from '@/shared/components/ui/Toast.vue'
import ConfirmDialog from '@/shared/components/ui/ConfirmDialog.vue'
import PromptDialog from '@/shared/components/ui/PromptDialog.vue'

describe('App shell', () => {
  it('mounts RouterView and interaction hosts together', () => {
    const wrapper = renderWithPrime(App, {
      global: {
        stubs: {
        LogOut: true,
        X: true,
          RouterView: { template: '<div data-test="route-view" />' }
        }
      }
    })

    expect(wrapper.find('[data-test="route-view"]').exists()).toBe(true)
    expect(wrapper.findComponent(Toast).exists()).toBe(true)
    expect(wrapper.findComponent(ConfirmDialog).exists()).toBe(true)
    expect(wrapper.findComponent(PromptDialog).exists()).toBe(true)
  })
})
