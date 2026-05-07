import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import BaseCard from '@/shared/components/ui/BaseCard.vue'

describe('BaseCard', () => {
  it('renders default slot inside body with default md padding', () => {
    const wrapper = mount(BaseCard, {
      slots: { default: '<p data-test="content">hello</p>' }
    })
    const body = wrapper.find('.base-card__body')
    expect(body.exists()).toBe(true)
    expect(body.classes()).toContain('base-card__body--md')
    expect(wrapper.find('[data-test="content"]').exists()).toBe(true)
  })

  it('renders title + subtitle from props', () => {
    const wrapper = mount(BaseCard, {
      props: { title: '总览', subtitle: '本周扫码' }
    })
    expect(wrapper.find('.base-card__title').text()).toBe('总览')
    expect(wrapper.find('.base-card__subtitle').text()).toBe('本周扫码')
  })

  it('honors padding=none and noPadding alias', () => {
    const noneWrapper = mount(BaseCard, {
      props: { padding: 'none' },
      slots: { default: '<div />' }
    })
    expect(noneWrapper.find('.base-card__body').classes()).toContain('base-card__body--none')

    const aliasWrapper = mount(BaseCard, {
      props: { noPadding: true },
      slots: { default: '<div />' }
    })
    expect(aliasWrapper.find('.base-card__body').classes()).toContain('base-card__body--none')
  })

  it('renders header / footer slots', () => {
    const wrapper = mount(BaseCard, {
      slots: {
        header: '<div data-test="hd">HD</div>',
        default: '<p>body</p>',
        footer: '<div data-test="ft">FT</div>'
      }
    })
    expect(wrapper.find('[data-test="hd"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="ft"]').exists()).toBe(true)
    expect(wrapper.find('.base-card__header').exists()).toBe(true)
    expect(wrapper.find('.base-card__footer').exists()).toBe(true)
  })

  it('adds interactive class when interactive prop is set', () => {
    const wrapper = mount(BaseCard, {
      props: { interactive: true },
      slots: { default: '<div />' }
    })
    expect(wrapper.find('.base-card').classes()).toContain('base-card--interactive')
  })
})
