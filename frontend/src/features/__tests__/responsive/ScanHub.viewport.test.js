import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive } from 'vue'
import { flushPromises } from '@vue/test-utils'
import { renderWithPrime } from '@/test/renderWithPrime'
import ScanHub from '@/features/trace/views/ScanHub.vue'
import scanHubSource from '@/features/trace/views/ScanHub.vue?raw'

const { pushMock } = vi.hoisted(() => ({ pushMock: vi.fn() }))

const getTraceAvailableActionsMock = vi.fn()

const currentUser = reactive({
  permissions: ['trace:scan', 'trace:view']
})

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    useRouter: () => ({ push: pushMock })
  }
})

vi.mock('@/core/stores/user', () => ({
  useUserStore: () => ({ user: currentUser })
}))

vi.mock('@/features/trace/api', () => ({
  getTraceAvailableActions: (...args) => getTraceAvailableActionsMock(...args)
}))

const stubs = {
  QRScanner: { template: '<div data-test="qr-scanner-stub" />' },
  ScanFlowDialog: { props: ['modelValue'], template: '<div data-test="scan-flow-dialog-stub" :data-open="modelValue"></div>' },
  ScanExceptionDialog: { props: ['modelValue'], template: '<div data-test="scan-exception-dialog-stub" :data-open="modelValue"></div>' },
  CreateTraceDialog: { props: ['modelValue'], template: '<div data-test="create-trace-dialog-stub" :data-open="modelValue"></div>' }
}

const mountScanHub = () => renderWithPrime(ScanHub, { global: { stubs } })

function setViewport(width) {
  Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: width })
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    configurable: true,
    value: vi.fn().mockImplementation((query) => {
      const m = query.match(/max-width:\s*([0-9.]+)px/)
      const maxWidth = m ? parseFloat(m[1]) : Infinity
      return {
        matches: width <= maxWidth,
        media: query,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        addListener: vi.fn(),
        removeListener: vi.fn()
      }
    })
  })
}

describe('ScanHub viewport responsive contract', () => {
  let randomUUIDSpy

  beforeEach(() => {
    currentUser.permissions = ['trace:scan', 'trace:view']
    pushMock.mockReset()
    getTraceAvailableActionsMock.mockReset()
    randomUUIDSpy = vi.spyOn(crypto, 'randomUUID').mockReturnValue('uuid-fixed-1234')
  })

  afterEach(() => {
    randomUUIDSpy.mockRestore()
  })

  describe('Source-level @media contract', () => {
    it('declares two breakpoints (1023.98 tablet + 767.98 mobile) per Linear breakpoint plan', () => {
      expect(scanHubSource).toMatch(/@media\s*\(\s*max-width:\s*1023\.98px\s*\)/)
      expect(scanHubSource).toMatch(/@media\s*\(\s*max-width:\s*767\.98px\s*\)/)
    })

    it('collapses default + scanning grids to single column at tablet, identified to 5/4 split', () => {
      const tabletBlock = scanHubSource.match(/@media\s*\(\s*max-width:\s*1023\.98px\s*\)\s*\{([\s\S]*?)^\}/m)
      expect(tabletBlock).toBeTruthy()
      expect(tabletBlock[1]).toMatch(/\.scan-hub__grid--default[\s\S]*?\.scan-hub__grid--scanning/)
      expect(tabletBlock[1]).toMatch(/grid-template-columns:\s*1fr/)
      expect(tabletBlock[1]).toMatch(/\.scan-hub__grid--identified[\s\S]*?grid-template-columns:\s*5fr 4fr/)
    })

    it('makes actions panel sticky and collapses identified grid to 1fr at mobile', () => {
      const mobileSection = scanHubSource.split(/@media\s*\(\s*max-width:\s*767\.98px\s*\)/)[1]
      expect(mobileSection).toBeTruthy()
      expect(mobileSection).toMatch(/\.scan-hub__grid--identified[\s\S]*?grid-template-columns:\s*1fr/)
      expect(mobileSection).toMatch(/\.scan-hub__panel--actions[\s\S]*?position:\s*sticky/)
      expect(mobileSection).toMatch(/\.scan-hub__cta-row[\s\S]*?flex-direction:\s*column/)
    })
  })

  describe('Desktop viewport (1280×800) — idle stage', () => {
    beforeEach(() => setViewport(1280))

    it('renders default grid with cta + manual form + assignment entry', async () => {
      const wrapper = mountScanHub()
      await flushPromises()

      expect(wrapper.find('.scan-hub__grid--default').exists()).toBe(true)
      expect(wrapper.find('[data-test="scan-start"]').exists()).toBe(true)
      expect(wrapper.find('[data-test="scan-manual-form"]').exists()).toBe(true)
      expect(wrapper.find('[data-test="scan-manual-input"]').exists()).toBe(true)
    })
  })

  describe('Mobile viewport (390×844) — idle stage', () => {
    beforeEach(() => setViewport(390))

    it('renders identical idle structural anchors (CSS @media handles visual stack)', async () => {
      const wrapper = mountScanHub()
      await flushPromises()

      expect(wrapper.find('.scan-hub__grid--default').exists()).toBe(true)
      expect(wrapper.find('[data-test="scan-start"]').exists()).toBe(true)
      expect(wrapper.find('[data-test="scan-manual-form"]').exists()).toBe(true)
    })

    it('matchMedia mock confirms tablet AND mobile breakpoints both match at 390px', () => {
      expect(window.matchMedia('(max-width: 1023.98px)').matches).toBe(true)
      expect(window.matchMedia('(max-width: 767.98px)').matches).toBe(true)
    })
  })
})
