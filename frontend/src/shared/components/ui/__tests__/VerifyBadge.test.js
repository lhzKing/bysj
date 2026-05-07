import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import VerifyBadge from '@/shared/components/ui/VerifyBadge.vue'

describe('VerifyBadge', () => {
  it('renders ok variant by default with N/N + algo', () => {
    const wrapper = mount(VerifyBadge, {
      props: { valid: true, validNodes: 4, totalNodes: 4 }
    })
    const root = wrapper.find('.verify-badge')
    expect(root.classes()).toContain('verify-badge--ok')
    expect(wrapper.find('.verify-badge__headline').text()).toBe('链上完整 · 4 / 4 节点验签通过')
    expect(wrapper.find('.verify-badge__subline').text()).toBe('RSA-2048 · SHA-256 链式哈希')
  })

  it('renders fail variant when valid=false', () => {
    const wrapper = mount(VerifyBadge, {
      props: { valid: false, validNodes: 3, totalNodes: 4 }
    })
    expect(wrapper.find('.verify-badge').classes()).toContain('verify-badge--fail')
    expect(wrapper.find('.verify-badge__headline').text()).toContain('链上不完整')
  })

  it('formats verifiedAt to hh:mm:ss', () => {
    const wrapper = mount(VerifyBadge, {
      props: {
        valid: true,
        validNodes: 4,
        totalNodes: 4,
        verifiedAt: new Date('2026-05-05T14:32:08')
      }
    })
    expect(wrapper.find('.verify-badge__subline').text()).toContain('最后验证于 14:32:08')
  })

  it('renders actions slot for view-key / third-party verify links', () => {
    const wrapper = mount(VerifyBadge, {
      props: { valid: true, validNodes: 4, totalNodes: 4 },
      slots: {
        actions: '<a data-test="vk">查看公钥</a>'
      }
    })
    expect(wrapper.find('[data-test="vk"]').exists()).toBe(true)
    expect(wrapper.find('.verify-badge__actions').exists()).toBe(true)
  })
})
