import { config } from '@vue/test-utils'

config.global.stubs = {
  transition: false,
  teleport: true
}

class ResizeObserverMock {
  observe() {}
  unobserve() {}
  disconnect() {}
}

global.ResizeObserver = ResizeObserverMock
window.matchMedia = window.matchMedia || (() => ({
  matches: false,
  addEventListener() {},
  removeEventListener() {}
}))
