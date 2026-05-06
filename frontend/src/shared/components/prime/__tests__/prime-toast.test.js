import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'

const add = vi.fn()

vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add })
}))

import { renderWithPrime } from '@/test/renderWithPrime'
import PrimeToastHost from '@/shared/components/prime/PrimeToastHost.vue'
import { useToast, __resetToastBridge } from '@/shared/composables/useToast'

describe('PrimeToastHost', () => {
  beforeEach(() => {
    add.mockClear()
    __resetToastBridge()
  })

  it('bridges success messages to PrimeVue ToastService', async () => {
    renderWithPrime(PrimeToastHost)
    await nextTick()

    useToast().success('Saved successfully', 1200)

    expect(add).toHaveBeenCalledWith(expect.objectContaining({
      group: 'app-toast',
      severity: 'success',
      detail: 'Saved successfully',
      life: 1200
    }))
  })
})
