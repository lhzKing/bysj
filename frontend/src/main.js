import { createApp } from 'vue'
import PrimeVue from 'primevue/config'
import ConfirmationService from 'primevue/confirmationservice'
import ToastService from 'primevue/toastservice'
import pinia from './core/stores'
import router from './core/router'
import App from './App.vue'
import { enterpriseTheme } from './shared/theme/primevue-theme'
import './style.css'

const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(PrimeVue, {
  theme: {
    preset: enterpriseTheme
  }
})
app.use(ToastService)
app.use(ConfirmationService)

app.mount('#app')
