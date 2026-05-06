export const enterpriseTokens = {
  primitive: {
    indigo: {
      50: '#e0e7ff',
      100: '#e0e7ff',
      500: '#6366f1',
      600: '#4f46e5',
      700: '#4338ca',
      900: '#312e81'
    },
    slate: {
      0: '#ffffff',
      50: '#f8fafc',
      100: '#f1f5f9',
      200: '#e2e8f0',
      300: '#cbd5e1',
      500: '#64748b',
      700: '#334155',
      900: '#0f172a'
    },
    emerald: { 50: '#ecfdf5', 500: '#10b981', 700: '#047857' },
    amber: { 50: '#fffbeb', 500: '#f59e0b', 700: '#b45309' },
    rose: { 50: '#fff1f2', 500: '#f43f5e', 700: '#be123c' }
  },
  semantic: {
    primary: { color: '{indigo.600}', hoverColor: '{indigo.700}', activeColor: '{indigo.700}', contrastColor: '{slate.0}' },
    bg: { app: '#fdfdff', page: '#fdfdff', surface: 'rgba(255, 255, 255, 0.6)', elevated: 'rgba(255, 255, 255, 0.8)', overlay: 'rgba(15, 23, 42, 0.45)' },
    text: { primary: '{slate.900}', secondary: '{slate.500}', muted: '{slate.400}', inverse: '{slate.0}', link: '{indigo.600}' },
    border: { subtle: 'rgba(255, 255, 255, 0.7)', default: 'rgba(255, 255, 255, 0.8)', strong: '{indigo.200}', focus: '{indigo.600}' },
    state: {
      success: '{emerald.500}', successBg: '{emerald.50}',
      warning: '{amber.500}', warningBg: '{amber.50}',
      danger: '{rose.500}', dangerBg: '{rose.50}',
      info: '{indigo.600}', infoBg: '{indigo.50}'
    }
  },
  controls: {
    radius: { sm: '12px', md: '16px', lg: '32px', xl: '40px' },
    shadow: { sm: '0 4px 12px rgba(0, 0, 0, 0.02)', md: '0 10px 40px -6px rgba(0, 0, 0, 0.02), 0 20px 80px -12px rgba(99, 102, 241, 0.08)', lg: '0 30px 100px -15px rgba(99, 102, 241, 0.15)' },
    height: { sm: '36px', md: '48px', lg: '56px' }
  }
}
