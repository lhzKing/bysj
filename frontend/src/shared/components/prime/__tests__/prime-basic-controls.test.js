import { describe, expect, it } from 'vitest'
import { renderWithPrime } from '@/test/renderWithPrime'
import PrimeButton from '@/shared/components/prime/PrimeButton.vue'
import PrimeInput from '@/shared/components/prime/PrimeInput.vue'
import PrimeLoadingSkeleton from '@/shared/components/prime/PrimeLoadingSkeleton.vue'

describe('prime basic controls', () => {
  it('renders PrimeButton with forwarded label and block class', async () => {
    const wrapper = renderWithPrime(PrimeButton, {
      props: { label: 'Save', block: true, variant: 'primary' }
    })

    expect(wrapper.text()).toContain('Save')
    expect(wrapper.classes()).toContain('w-full')
  })

  it('updates PrimeInput model and shows error text', async () => {
    const wrapper = renderWithPrime(PrimeInput, {
      props: { modelValue: '', error: 'Required field', label: 'Username' }
    })

    expect(wrapper.text()).toContain('Username')
    expect(wrapper.text()).toContain('Required field')
  })

  it('renders a table skeleton layout', () => {
    const wrapper = renderWithPrime(PrimeLoadingSkeleton, {
      props: { type: 'table', rows: 3 }
    })

    expect(wrapper.findAll('[data-test="skeleton-row"]')).toHaveLength(3)
  })
})
