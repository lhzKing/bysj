import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import BaseInput from '@/shared/components/ui/BaseInput.vue'
import LoadingSkeleton from '@/shared/components/ui/LoadingSkeleton.vue'

describe('BaseButton', () => {
  it('renders default slot text', () => {
    const wrapper = mount(BaseButton, { slots: { default: '提交' } })
    expect(wrapper.text()).toContain('提交')
  })

  it('applies variant + size class on root', () => {
    const wrapper = mount(BaseButton, {
      props: { variant: 'secondary', size: 'md' },
      slots: { default: 'Cancel' }
    })
    const root = wrapper.find('button')
    expect(root.classes()).toContain('base-btn--secondary')
    expect(root.classes()).toContain('base-btn--md')
  })

  it('emits click when clicked', async () => {
    const wrapper = mount(BaseButton, { slots: { default: 'Go' } })
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it('disables button on loading and renders spinner', () => {
    const wrapper = mount(BaseButton, {
      props: { loading: true },
      slots: { default: 'Loading' }
    })
    const button = wrapper.find('button')
    expect(button.attributes('disabled')).toBeDefined()
    expect(button.find('.base-btn__spinner').exists()).toBe(true)
  })

  it('does not emit click when disabled', async () => {
    const wrapper = mount(BaseButton, {
      props: { disabled: true },
      slots: { default: 'X' }
    })
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('click')).toBeUndefined()
  })

  it('renders icon and kbd slots', () => {
    const wrapper = mount(BaseButton, {
      slots: {
        default: '入库登记',
        icon: '<svg data-test="icon" />',
        kbd: 'F1'
      }
    })
    expect(wrapper.find('[data-test="icon"]').exists()).toBe(true)
    expect(wrapper.find('.base-btn__kbd').text()).toBe('F1')
  })

  it('hides label when variant=icon', () => {
    const wrapper = mount(BaseButton, {
      props: { variant: 'icon' },
      slots: { default: 'Hidden', icon: '<svg data-test="ico" />' }
    })
    expect(wrapper.find('.base-btn__label').exists()).toBe(false)
    expect(wrapper.find('[data-test="ico"]').exists()).toBe(true)
    expect(wrapper.find('button').classes()).toContain('base-btn--icon-only')
  })

  it('applies block class for full width', () => {
    const wrapper = mount(BaseButton, {
      props: { block: true },
      slots: { default: 'Full' }
    })
    expect(wrapper.find('button').classes()).toContain('base-btn--block')
  })
})

describe('BaseInput', () => {
  it('renders label and binds for=id', () => {
    const wrapper = mount(BaseInput, {
      props: { label: '工号', inputId: 'login-username' }
    })
    const label = wrapper.find('label')
    expect(label.text()).toBe('工号')
    expect(label.attributes('for')).toBe('login-username')
    expect(wrapper.find('input').attributes('id')).toBe('login-username')
  })

  it('emits update:modelValue on input', async () => {
    const wrapper = mount(BaseInput, { props: { modelValue: '' } })
    await wrapper.find('input').setValue('admin')
    expect(wrapper.emitted('update:modelValue')[0]).toEqual(['admin'])
  })

  it('shows error state and message', () => {
    const wrapper = mount(BaseInput, {
      props: { label: '密码', error: '密码至少 6 位' }
    })
    const field = wrapper.find('.base-input__field')
    expect(field.classes()).toContain('base-input__field--error')
    expect(wrapper.find('.base-input__hint').text()).toBe('密码至少 6 位')
    expect(wrapper.find('.base-input__hint').classes()).toContain('base-input__hint--error')
    expect(wrapper.find('input').attributes('aria-invalid')).toBe('true')
  })

  it('renders helperText when no error', () => {
    const wrapper = mount(BaseInput, {
      props: { helperText: '至少 6 位字母数字' }
    })
    expect(wrapper.find('.base-input__hint').text()).toBe('至少 6 位字母数字')
    expect(wrapper.find('.base-input__hint').classes()).not.toContain('base-input__hint--error')
  })

  it('toggles password visibility', async () => {
    const wrapper = mount(BaseInput, {
      props: { type: 'password', modelValue: 'secret' }
    })
    expect(wrapper.find('input').attributes('type')).toBe('password')
    await wrapper.find('.base-input__toggle').trigger('click')
    expect(wrapper.find('input').attributes('type')).toBe('text')
  })

  it('honors size=sm to render 32px control', () => {
    const wrapper = mount(BaseInput, { props: { size: 'sm' } })
    expect(wrapper.find('.base-input__field').classes()).toContain('base-input__field--sm')
  })

  it('disables input', () => {
    const wrapper = mount(BaseInput, { props: { disabled: true } })
    expect(wrapper.find('input').attributes('disabled')).toBeDefined()
    expect(wrapper.find('.base-input__field').classes()).toContain('base-input__field--disabled')
  })
})

describe('LoadingSkeleton', () => {
  it('renders rows for type=table', () => {
    const wrapper = mount(LoadingSkeleton, {
      props: { type: 'table', rows: 2 }
    })
    expect(wrapper.findAll('[data-test="skeleton-row"]')).toHaveLength(2)
    expect(wrapper.find('[data-skeleton-type="table"]').exists()).toBe(true)
  })

  it('renders cards for type=card with count', () => {
    const wrapper = mount(LoadingSkeleton, {
      props: { type: 'card', count: 3 }
    })
    expect(wrapper.findAll('[data-test="skeleton-card"]')).toHaveLength(3)
  })

  it('defaults to table layout when no type is provided', () => {
    const wrapper = mount(LoadingSkeleton)
    expect(wrapper.find('[data-skeleton-type="table"]').exists()).toBe(true)
    expect(wrapper.findAll('[data-test="skeleton-row"]').length).toBe(5)
  })
})
