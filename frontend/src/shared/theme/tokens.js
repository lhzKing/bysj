// Linear-light design tokens.
// 三层结构：primitive（原色阶）→ semantic（语义化引用）→ controls（控件参数）。
// 与 frontend/preview/linear-*.html、DESIGN.md 保持 1:1 对齐。
export const enterpriseTokens = {
  primitive: {
    // Linear lavender —— 唯一 accent，仅用于品牌、focus、主 CTA、链路当前节点
    lavender: {
      50: '#eef0fb',
      100: '#e0e3f5',
      200: '#c1c8eb',
      300: '#9aa3df',
      400: '#7c87d6',
      500: '#5e6ad2',
      600: '#5e69d1',
      700: '#4a55b8',
      800: '#3a4399',
      900: '#2c3478',
      hover: '#828fff'
    },
    // Zinc 灰阶 —— 整套 ink / surface / hairline 的基底（替代旧 slate）
    zinc: {
      0: '#ffffff',
      50: '#fafafa',
      100: '#f4f4f5',
      200: '#ebebec',
      300: '#e6e6e8',
      400: '#d4d4d8',
      500: '#a1a1aa',
      600: '#71717a',
      700: '#3f3f46',
      800: '#27272a',
      900: '#18181b'
    },
    // 语义色 —— 仅用于 pill / dot，不染按钮主色
    success: { soft: '#e7f6ea', base: '#27a644', strong: '#1f8235' },
    warn: { soft: '#fef3e6', base: '#d97706', strong: '#b45309' },
    error: { soft: '#fdecec', base: '#e5484d', strong: '#c92a2a' },
    // 摄像头取景区专用 dark 面（light 系统中唯一允许的暗面）
    dark: {
      canvas: '#010102',
      surface1: '#0f1011',
      surface2: '#141516',
      surface3: '#18191a',
      hairline: '#23252a',
      ink: '#f7f8f8',
      inkSubtle: '#8a8f98'
    }
  },
  semantic: {
    primary: {
      color: '{lavender.500}',
      hoverColor: '{lavender.hover}',
      activeColor: '{lavender.600}',
      contrastColor: '{zinc.0}',
      soft: '{lavender.50}',
      focusRing: 'rgba(94,106,210,0.15)'
    },
    bg: {
      app: '#fafafa',
      page: '#fafafa',
      canvas: '#fafafa',
      surface: '{zinc.0}',
      surface1: '{zinc.0}',
      surface2: '{zinc.100}',
      surface3: '{zinc.200}',
      elevated: '{zinc.0}',
      overlay: 'rgba(15,23,42,0.45)',
      // 摄像头取景：唯一暗面
      darkCanvas: '{dark.canvas}',
      darkSurface: '{dark.surface1}'
    },
    text: {
      ink: '{zinc.900}',
      primary: '{zinc.900}',
      secondary: '{zinc.700}',
      muted: '{zinc.700}',
      subtle: '{zinc.600}',
      tertiary: '{zinc.500}',
      inverse: '{zinc.0}',
      link: '{lavender.500}',
      onDark: '{dark.ink}'
    },
    border: {
      hairline: '{zinc.300}',
      subtle: '{zinc.300}',
      default: '{zinc.300}',
      strong: '{zinc.400}',
      focus: '{lavender.600}',
      darkHairline: '{dark.hairline}'
    },
    state: {
      success: '{success.base}',
      successBg: '{success.soft}',
      warning: '{warn.base}',
      warningBg: '{warn.soft}',
      danger: '{error.base}',
      dangerBg: '{error.soft}',
      info: '{lavender.500}',
      infoBg: '{lavender.50}'
    }
  },
  controls: {
    // 圆角阶梯 —— 不允许 32/40/48/56 这些巨型圆角
    radius: {
      xs: '4px',
      sm: '6px',
      md: '8px',
      lg: '12px',
      xl: '16px',
      pill: '9999px'
    },
    // 间距阶梯 —— section 间距 ≤48
    spacing: {
      xxs: '4px',
      xs: '8px',
      sm: '12px',
      md: '16px',
      lg: '24px',
      xl: '32px',
      xxl: '48px'
    },
    // 控件高度 —— 32 / 36 双档（默认 32 紧凑、36 略松）
    height: {
      xs: '28px',
      sm: '32px',
      md: '36px',
      lg: '40px'
    },
    // 字号阶梯（与 DESIGN.md 对齐，display 用负字距）
    typography: {
      displayLg: { size: '48px', lineHeight: '1.1', tracking: '-1.5px', weight: '600' },
      displayMd: { size: '36px', lineHeight: '1.15', tracking: '-1px', weight: '600' },
      headline: { size: '28px', lineHeight: '1.2', tracking: '-0.6px', weight: '600' },
      cardTitle: { size: '22px', lineHeight: '1.25', tracking: '-0.4px', weight: '500' },
      body: { size: '14px', lineHeight: '1.5', tracking: '0', weight: '400' },
      bodySm: { size: '13px', lineHeight: '1.5', tracking: '0', weight: '400' },
      caption: { size: '12px', lineHeight: '1.4', tracking: '0', weight: '400' },
      eyebrow: { size: '11px', lineHeight: '1.3', tracking: '0.4px', weight: '500' }
    },
    // 字体族
    fontFamily: {
      sans: '"Inter", -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif',
      mono: '"JetBrains Mono", ui-monospace, "SF Mono", Menlo, monospace'
    }
  }
}
