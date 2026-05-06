import { mount } from '@vue/test-utils'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import { enterpriseTheme } from '@/shared/theme/primevue-theme'

export function renderWithPrime(component, options = {}) {
  return mount(component, {
    ...options,
    global: {
      plugins: [
        [PrimeVue, { theme: { preset: enterpriseTheme } }],
        ToastService,
        ConfirmationService,
        ...(options.global?.plugins || [])
      ],
      ...(options.global || {})
    }
  })
}
