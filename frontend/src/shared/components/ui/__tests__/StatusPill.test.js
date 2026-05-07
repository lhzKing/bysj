import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import StatusPill from '@/shared/components/ui/StatusPill.vue'

describe('StatusPill', () => {
  it('renders default mute tone with dot', () => {
    const wrapper = mount(StatusPill, { slots: { default: '在库' } })
    const root = wrapper.find('.status-pill')
    expect(root.classes()).toContain('status-pill--mute')
    expect(root.classes()).toContain('status-pill--sm')
    expect(wrapper.find('.status-pill__dot').exists()).toBe(true)
    expect(wrapper.text()).toBe('在库')
  })

  it.each([
    ['success'],
    ['warn'],
    ['error'],
    ['mute'],
    ['primary']
  ])('applies %s tone class', (tone) => {
    const wrapper = mount(StatusPill, {
      props: { tone },
      slots: { default: tone }
    })
    expect(wrapper.find('.status-pill').classes()).toContain(`status-pill--${tone}`)
    expect(wrapper.find('.status-pill__dot').classes()).toContain(`status-pill__dot--${tone}`)
  })

  it('hides dot when dot=false', () => {
    const wrapper = mount(StatusPill, {
      props: { dot: false, tone: 'error' },
      slots: { default: '超时 1h 23m' }
    })
    expect(wrapper.find('.status-pill__dot').exists()).toBe(false)
  })

  it('honors size=xs for sidebar badges', () => {
    const wrapper = mount(StatusPill, {
      props: { tone: 'error', size: 'xs' },
      slots: { default: '3' }
    })
    expect(wrapper.find('.status-pill').classes()).toContain('status-pill--xs')
  })
})
