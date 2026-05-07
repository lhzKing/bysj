/** @type {import('tailwindcss').Config} */
// Tailwind 配置直接引用 src/style.css 中暴露的 Linear-light CSS 变量。
// 颜色映射 var() 形式，便于业务页面用 bg-canvas / text-ink / border-hairline 等语义类。
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // surface ladder
        canvas: 'var(--canvas)',
        'surface-1': 'var(--surface-1)',
        'surface-2': 'var(--surface-2)',
        'surface-3': 'var(--surface-3)',
        hairline: {
          DEFAULT: 'var(--hairline)',
          strong: 'var(--hairline-strong)'
        },
        // ink ladder
        ink: {
          DEFAULT: 'var(--ink)',
          muted: 'var(--ink-muted)',
          subtle: 'var(--ink-subtle)',
          tertiary: 'var(--ink-tertiary)'
        },
        // lavender accent
        primary: {
          DEFAULT: 'var(--primary)',
          hover: 'var(--primary-hover)',
          focus: 'var(--primary-focus)',
          soft: 'var(--primary-soft)'
        },
        // semantic
        success: {
          DEFAULT: 'var(--success)',
          soft: 'var(--success-soft)'
        },
        warn: {
          DEFAULT: 'var(--warn)',
          soft: 'var(--warn-soft)'
        },
        error: {
          DEFAULT: 'var(--error)',
          soft: 'var(--error-soft)'
        },
        // dark surface（仅供 QRScanner 使用）
        'dark-canvas': 'var(--dark-canvas)',
        'dark-surface-1': 'var(--dark-surface-1)',
        'dark-surface-2': 'var(--dark-surface-2)',
        'dark-hairline': 'var(--dark-hairline)',
        'dark-ink': 'var(--dark-ink)',
        'dark-ink-subtle': 'var(--dark-ink-subtle)'
      },
      fontFamily: {
        sans: ['Inter', '-apple-system', '"PingFang SC"', '"Microsoft YaHei"', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'ui-monospace', '"SF Mono"', 'Menlo', 'monospace']
      },
      borderRadius: {
        // Linear 圆角阶梯：4 / 6 / 8 / 12 / 16
        xs: '4px',
        sm: '6px',
        md: '8px',
        lg: '12px',
        xl: '16px'
      },
      // 间距阶梯：4 / 8 / 12 / 16 / 24 / 32 / 48
      spacing: {
        xxs: '4px',
        xxl: '48px'
      },
      ringColor: {
        focus: 'var(--primary-ring)'
      }
    },
  },
  plugins: [],
}
