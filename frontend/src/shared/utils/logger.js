/**
 * 统一前端日志通道。
 *
 * 两层防御保证 prod 构建产物不含 console 调用：
 * 1. 本文件内：dev 分支走 console.*，prod 分支是 noop——`import.meta.env.DEV` 是 Vite 编译时常量，
 *    esbuild 在 prod build 时会把整个 dev 分支视作 dead code 并移除；
 * 2. `vite.config.js` 中 `esbuild.drop: ['console', 'debugger']`（仅 production）会兜底
 *    清理用户代码里直接调用 `console.*` 的残留——以防有人忘了走 logger。
 *
 * 使用：
 *   import { logger } from '@/shared/utils/logger'
 *   logger.error('Failed to load KPI:', error)
 *
 * 后续接入 Sentry / 自定义上报：
 *   把下面的 PROD_LOGGER 三个 noop 替换为对应的上报函数即可。
 */

const noop = () => {}

const DEV_LOGGER = {
  info: (...args) => console.info(...args),
  warn: (...args) => console.warn(...args),
  error: (...args) => console.error(...args),
}

const PROD_LOGGER = {
  info: noop,
  warn: noop,
  // 未来可在此处对接 Sentry / 自建告警；目前 noop 让 prod 控制台完全干净。
  error: noop,
}

export const logger = import.meta.env.DEV ? DEV_LOGGER : PROD_LOGGER
