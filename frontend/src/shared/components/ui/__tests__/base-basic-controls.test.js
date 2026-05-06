import { describe, expect, it } from 'vitest'
import { renderWithPrime } from '@/test/renderWithPrime'
import PrimeButton from '@/shared/components/prime/PrimeButton.vue'
import PrimeInput from '@/shared/components/prime/PrimeInput.vue'
import PrimeLoadingSkeleton from '@/shared/components/prime/PrimeLoadingSkeleton.vue'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import BaseInput from '@/shared/components/ui/BaseInput.vue'
import LoadingSkeleton from '@/shared/components/ui/LoadingSkeleton.vue'

describe('base compatibility controls', () => {
  it('keeps BaseButton slot and variant API', () => {
    const wrapper = renderWithPrime(BaseButton, {
      props: { variant: 'danger', block: true },
      slots: { default: 'Delete' }
    })

    const primeButton = wrapper.findComponent(PrimeButton)

    expect(primeButton.exists()).toBe(true)
    expect(primeButton.props('variant')).toBe('danger')
    expect(wrapper.text()).toContain('Delete')
  })

  it('keeps BaseInput label and update:modelValue API', async () => {
    const wrapper = renderWithPrime(BaseInput, {
      props: { modelValue: '', label: 'Account', placeholder: 'Please enter account' }
    })

    await wrapper.find('input').setValue('admin')

    const primeInput = wrapper.findComponent(PrimeInput)

    expect(primeInput.exists()).toBe(true)
    expect(wrapper.emitted('update:modelValue')[0]).toEqual(['admin'])
  })

  it('keeps LoadingSkeleton type and rows API', () => {
    const wrapper = renderWithPrime(LoadingSkeleton, {
      props: { type: 'table', rows: 2 }
    })

    const primeSkeleton = wrapper.findComponent(PrimeLoadingSkeleton)

    expect(primeSkeleton.exists()).toBe(true)
    expect(primeSkeleton.props('rows')).toBe(2)
  })

  it('keeps legacy LoadingSkeleton count API for card layouts', () => {
    const wrapper = renderWithPrime(LoadingSkeleton, {
      props: { type: 'card', count: 3 }
    })

    expect(wrapper.findAll('[data-test="skeleton-card"]')).toHaveLength(3)
  })
})
