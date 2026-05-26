import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import fs from 'fs'
import path from 'path'
const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

/**
 * 读取本地自签 HTTPS 证书；找不到时给出可操作的中文提示后让 Vite 启动失败。
 * 不要 fallback 到 https=false——那会让 npm run dev 看似启动成功但摄像头扫码失效，
 * 反而比"明确报错指向修复路径"更难排查。
 */
function loadHttpsCertOrFail(rootDir, keyPath, certPath) {
  const resolvedKey = path.resolve(rootDir, keyPath)
  const resolvedCert = path.resolve(rootDir, certPath)
  try {
    return {
      key: fs.readFileSync(resolvedKey),
      cert: fs.readFileSync(resolvedCert),
    }
  } catch (e) {
    if (e && e.code === 'ENOENT') {
      const banner = '\n' + '='.repeat(72)
      console.error(
        banner +
        '\n[vite] 未找到 HTTPS 证书：' +
        `\n  - key:  ${resolvedKey}` +
        `\n  - cert: ${resolvedCert}` +
        '\n\n请先在 frontend/ 目录下生成自签证书：' +
        '\n  Windows  PowerShell:   ./generate-cert.ps1' +
        '\n  macOS / Linux / 其它:  node generate-cert.js' +
        '\n\n如本次开发不需要 HTTPS（注意：摄像头扫码必须 HTTPS），' +
        '\n可在 frontend/.env.development.local 中设置 VITE_DEV_HTTPS=false 改走 HTTP。' +
        banner + '\n'
      )
      throw new Error('vite dev server: HTTPS 证书缺失，详见上方提示。')
    }
    throw e
  }
}

// https://vitejs.dev/config/
export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, __dirname, '')
  const httpsEnabled = env.VITE_DEV_HTTPS !== 'false'
  const keyPath = env.VITE_DEV_CERT_KEY || 'certs/localhost.key'
  const certPath = env.VITE_DEV_CERT_CERT || 'certs/localhost.crt'
  // 证书仅 dev server (`vite`) 启动 HTTPS 时需要；`vite build` 产物是纯静态文件，不读证书。
  // 在 build/CI/Docker 镜像里 certs/ 通常被 .gitignore 排除（自签证书不入库），
  // 必须用 command 守卫，否则 build 阶段会因找不到证书直接失败。
  const isDevServer = command === 'serve'

  return {
    plugins: [
      vue(),
      // 不再使用 basicSsl()，改用自定义证书
    ],
    // 仅在 production 构建时让 esbuild 兜底剥离 console.* 与 debugger——
    // 1. 业务代码应优先走 @/shared/utils/logger（dev 输出 / prod noop）；
    // 2. 这里是双保险：如有遗漏的直接 console.* 调用，esbuild 会一并清掉，确保 prod bundle 干净；
    // 3. 仅 production 触发，dev / vitest（mode='test'）时 console.* 完全保留，便于调试与单元测试断言。
    esbuild: {
      drop: mode === 'production' ? ['console', 'debugger'] : [],
    },
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    test: {
      environment: 'jsdom',
      globals: true,
      setupFiles: './src/test/setup.js',
      css: true
    },
    server: {
      https: isDevServer && httpsEnabled ? loadHttpsCertOrFail(__dirname, keyPath, certPath) : false,
      host: env.VITE_DEV_HOST || '0.0.0.0', // 允许局域网访问
      port: Number(env.VITE_DEV_PORT || 5173),
      proxy: {
        '/api': {
          target: env.VITE_API_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
          secure: false, // 允许 HTTPS 代理到 HTTP
          ws: true // 支持 WebSocket
        }
      }
    },
    build: {
      // 默认 500 kB 阈值对 echarts 单体过于严苛——echarts/core + 已注册的 charts/components 经过
      // tree-shaking 后单 chunk ~600 kB 是常态。把警告阈值上调到 800 kB，超出仍会警告，
      // 进一步下降需要按图表类型路由级懒加载（成本/收益不划算，留作 follow-up）。
      chunkSizeWarningLimit: 800,
      rollupOptions: {
        output: {
          // 显式 vendor 拆分；让 echarts、primevue、vue 三大类各占独立 chunk，
          // 避免被合并到首屏路由 chunk 里推高初始下载量。
          // 业务代码本身已通过 router 动态 import + Dashboard 内 defineAsyncComponent 走路由级懒加载，
          // 这里只处理共享 vendor 部分。
          manualChunks(id) {
            if (!id.includes('node_modules')) return undefined
            if (id.includes('echarts') || id.includes('vue-echarts') || id.includes('zrender')) {
              return 'vendor-echarts'
            }
            if (id.includes('primevue') || id.includes('@primeuix')) {
              return 'vendor-prime'
            }
            if (id.includes('vue-qrcode-reader') || id.includes('barcode-detector')) {
              return 'vendor-qrcode'
            }
            if (
              id.includes('/vue/') ||
              id.includes('/@vue/') ||
              id.includes('vue-router') ||
              id.includes('pinia') ||
              id.includes('@vueuse')
            ) {
              return 'vendor-vue'
            }
            return undefined
          }
        }
      }
    }
  }
})
