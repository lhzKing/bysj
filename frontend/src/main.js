import { createApp } from 'vue'
import { setZXingModuleOverrides } from 'vue-qrcode-reader'
import PrimeVue from 'primevue/config'
import ConfirmationService from 'primevue/confirmationservice'
import ToastService from 'primevue/toastservice'
import pinia from './core/stores'
import router from './core/router'
import App from './App.vue'
import { enterpriseTheme } from './shared/theme/primevue-theme'
import './style.css'

// vue-qrcode-reader 把 zxing-wasm 整体 inline 进了自己的 dist（line 370），并暴露了
// setZXingModuleOverrides 让外部覆盖 wasm 加载路径。默认 locateFile 指向
// https://fastly.jsdelivr.net/npm/zxing-wasm@1.1.3/dist/reader/zxing_reader.wasm
// 项目 CSP 的 connect-src 不放外部 CDN，那条 fetch 永远被拦截 → 扫码识别拿不到结果。
// 注意：必须从 vue-qrcode-reader 主包引（不能从 zxing-wasm/reader 或 barcode-detector/pure 引——
// 那是各自独立的模块实例，覆盖不到 vue-qrcode-reader 内嵌的那份 zxing-wasm）。
// 改为同源 /zxing/，CSP 'self' 直接放行，演示完全离线可用。必须在任何 QRScanner 挂载之前调用。
setZXingModuleOverrides({
  locateFile: (path, prefix) => (path.endsWith('.wasm') ? `/zxing/${path}` : prefix + path)
})

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
