import { definePreset } from '@primeuix/themes'
import Aura from '@primeuix/themes/aura'

/*
 * Linear-light PrimeVue preset.
 * 基于 Aura 做 surgical override —— 仅覆盖颜色/圆角/阴影等视觉字段；
 * 行为字段（focus 顺序、键盘交互、遮罩动画等）继承 Aura 原生。
 *
 * 三个核心改写：
 *   1. semantic.primary 调色盘 → Linear lavender 阶（{primary.500}=#5e6ad2）。
 *   2. colorScheme.light.surface 数值阶 → 我的 zinc/hairline 阶；
 *      PrimeVue 默认引用 {surface.X} 的所有字段（border / hover bg / text /
 *      placeholder / list option focus 等）都自动接上 Linear-light。
 *   3. overlay.modal / overlay.select / list.option / mask / formField /
 *      focusRing 显式覆盖：圆角 8/12px、白底、1px hairline、无玻璃模糊滤镜、
 *      lavender focus ring 50% 不透明度。
 *
 * Toast 6 种 severity 按 Linear 语义色 (success/warn/error 软底 + 主色描边) 覆盖；
 * 默认 dark / blur 形式被显式擦除。
 */

// Linear lavender 阶 —— 与 frontend/src/shared/theme/tokens.js 同源
const lavenderPalette = {
  50: '#eef0fb',
  100: '#e0e3f5',
  200: '#c1c8eb',
  300: '#9aa3df',
  400: '#828fff', // 即 primary-hover，hover 状态走更亮
  500: '#5e6ad2', // 主 lavender
  600: '#5e69d1', // primary-focus / pressed
  700: '#4a55b8',
  800: '#3a4399',
  900: '#2c3478',
  950: '#1f2658'
}

// Linear-light surface 阶 —— 与 :root 中 --canvas/--surface-1/2/3/--hairline/--ink* 同源
const surfacePalette = {
  0: '#ffffff', // surface-1（默认 content / Dialog / input 背景）
  50: '#fafafa', // canvas
  100: '#f4f4f5', // surface-2（hover bg / option focus）
  200: '#ebebec', // surface-3
  300: '#e6e6e8', // hairline（默认 border）
  400: '#d4d4d8', // hairline-strong
  500: '#a1a1aa', // ink-tertiary（placeholder / icon）
  600: '#71717a', // ink-subtle（muted text）
  700: '#3f3f46', // ink-muted（默认正文）
  800: '#27272a',
  900: '#18181b', // ink（headline）
  950: '#09090b'
}

export const enterpriseTheme = definePreset(Aura, {
  primitive: {
    // 注入 lavender 阶供 semantic.primary 引用；Aura 默认 emerald/blue/zinc 等保留
    lavender: lavenderPalette
  },
  semantic: {
    // 把 primary 全阶映射到 lavender
    primary: {
      50: '{lavender.50}',
      100: '{lavender.100}',
      200: '{lavender.200}',
      300: '{lavender.300}',
      400: '{lavender.400}',
      500: '{lavender.500}',
      600: '{lavender.600}',
      700: '{lavender.700}',
      800: '{lavender.800}',
      900: '{lavender.900}',
      950: '{lavender.950}'
    },
    // 表单控件视觉：32-36px 高、8px 圆角、hairline 描边、lavender focus 环
    formField: {
      paddingX: '0.75rem',
      paddingY: '0.5rem',
      sm: { fontSize: '0.8125rem', paddingX: '0.625rem', paddingY: '0.375rem' },
      lg: { fontSize: '0.9375rem', paddingX: '0.875rem', paddingY: '0.5625rem' },
      borderRadius: '8px',
      focusRing: {
        width: '0',
        style: 'none',
        color: 'transparent',
        offset: '0',
        shadow: 'none'
      },
      transitionDuration: '0.15s'
    },
    // 全局 focus ring：2px lavender 描边 + 3px 15% 不透明度光晕（与预览页 input :focus 一致）
    focusRing: {
      width: '2px',
      style: 'solid',
      color: '{primary.color}',
      offset: '0',
      shadow: '0 0 0 3px rgba(94,106,210,0.15)'
    },
    // 圆角阶梯：与 tokens.js controls.radius 一致
    borderRadius: {
      none: '0',
      xs: '4px',
      sm: '6px',
      md: '8px',
      lg: '12px',
      xl: '16px'
    },
    overlay: {
      // Dialog：白底 + 12px 圆角 + 1px hairline + 极轻阴影；不要 16px+ 巨型圆角
      modal: {
        borderRadius: '12px',
        padding: '1.5rem',
        shadow: '0 12px 32px -12px rgba(15,23,42,0.18)'
      },
      // Select 下拉：白底 + 8px 圆角 + 1px hairline + 中等阴影
      select: {
        borderRadius: '8px',
        shadow: '0 6px 20px -8px rgba(15,23,42,0.15)'
      },
      // Popover：8px 圆角，padding 收紧
      popover: {
        borderRadius: '8px',
        padding: '0.75rem',
        shadow: '0 6px 20px -8px rgba(15,23,42,0.15)'
      },
      navigation: {
        shadow: '0 6px 20px -8px rgba(15,23,42,0.15)'
      }
    },
    list: {
      // 下拉项：6px 圆角 + 4px 内边距，紧凑
      padding: '0.25rem',
      gap: '2px',
      option: {
        padding: '0.5rem 0.75rem',
        borderRadius: '6px'
      }
    },
    colorScheme: {
      // === light 模式 —— Linear 唯一允许的标准模式 ===
      light: {
        // 把 surface 数值阶替换为 Linear-light zinc/hairline 阶
        surface: surfacePalette,
        // primary：lavender，hover 走更亮（{primary.400}），active 走更深（{primary.600}）
        primary: {
          color: '{primary.500}',
          contrastColor: '#ffffff',
          hoverColor: '{primary.400}',
          activeColor: '{primary.600}'
        },
        // 选中高亮：lavender soft 背景 + lavender 700 文字
        highlight: {
          background: '{primary.50}',
          focusBackground: '{primary.100}',
          color: '{primary.700}',
          focusColor: '{primary.800}'
        },
        // Dialog 遮罩：rgba(15,23,42,.45)（与 tokens.js semantic.bg.overlay 一致）
        mask: {
          background: 'rgba(15,23,42,0.45)',
          color: '{surface.200}'
        },
        // 表单控件：白底 + hairline 描边 + lavender focus；删除旧紫色高亮
        formField: {
          background: '{surface.0}',
          disabledBackground: '{surface.100}',
          filledBackground: '{surface.50}',
          filledHoverBackground: '{surface.50}',
          filledFocusBackground: '{surface.50}',
          borderColor: '{surface.300}',
          hoverBorderColor: '{surface.400}',
          focusBorderColor: '{primary.color}',
          invalidBorderColor: '#e5484d',
          color: '{surface.900}',
          disabledColor: '{surface.500}',
          placeholderColor: '{surface.500}',
          invalidPlaceholderColor: '#e5484d',
          floatLabelColor: '{surface.500}',
          floatLabelFocusColor: '{primary.color}',
          floatLabelActiveColor: '{surface.500}',
          floatLabelInvalidColor: '#e5484d',
          iconColor: '{surface.500}',
          shadow: 'none'
        },
        // 文字阶：ink / ink-muted / ink-subtle
        text: {
          color: '{surface.900}',
          hoverColor: '{surface.900}',
          mutedColor: '{surface.600}',
          hoverMutedColor: '{surface.700}'
        },
        // 容器：白底 + hairline border + ink 文字
        content: {
          background: '{surface.0}',
          hoverBackground: '{surface.100}',
          borderColor: '{surface.300}',
          color: '{surface.900}',
          hoverColor: '{surface.900}'
        },
        // overlay：所有弹层颜色统一白底 + hairline + ink
        overlay: {
          select: {
            background: '{surface.0}',
            borderColor: '{surface.300}',
            color: '{surface.900}'
          },
          popover: {
            background: '{surface.0}',
            borderColor: '{surface.300}',
            color: '{surface.900}'
          },
          modal: {
            background: '{surface.0}',
            borderColor: '{surface.300}',
            color: '{surface.900}'
          }
        },
        // 列表项：hover/focus 走 surface-2，选中走 lavender soft；不要旧紫色强高亮
        list: {
          option: {
            focusBackground: '{surface.100}',
            selectedBackground: '{primary.50}',
            selectedFocusBackground: '{primary.100}',
            color: '{surface.900}',
            focusColor: '{surface.900}',
            selectedColor: '{primary.700}',
            selectedFocusColor: '{primary.800}',
            icon: { color: '{surface.500}', focusColor: '{surface.600}' }
          },
          optionGroup: {
            background: 'transparent',
            color: '{surface.600}'
          }
        },
        // 导航项：hover/active 走 surface-2 + ink；图标 hover 走 ink-subtle
        navigation: {
          item: {
            focusBackground: '{surface.100}',
            activeBackground: '{surface.100}',
            color: '{surface.900}',
            focusColor: '{surface.900}',
            activeColor: '{surface.900}',
            icon: {
              color: '{surface.500}',
              focusColor: '{surface.600}',
              activeColor: '{surface.600}'
            }
          },
          submenuLabel: { background: 'transparent', color: '{surface.600}' },
          submenuIcon: {
            color: '{surface.500}',
            focusColor: '{surface.600}',
            activeColor: '{surface.600}'
          }
        }
      }
    }
  },
  components: {
    // Toast 6 种 severity —— 全部走 Linear 语义色，软底 + 主色描边，无玻璃模糊滤镜
    toast: {
      colorScheme: {
        light: {
          root: { blur: '0' },
          info: {
            background: '{primary.50}',
            borderColor: '{primary.200}',
            color: '{primary.700}',
            detailColor: '{surface.700}',
            shadow: '0 8px 24px -10px rgba(94,106,210,0.25)',
            closeButton: {
              hoverBackground: '{primary.100}',
              focusRing: { color: '{primary.600}', shadow: 'none' }
            }
          },
          success: {
            background: '#e7f6ea',
            borderColor: '#bef0c7',
            color: '#1f8235',
            detailColor: '{surface.700}',
            shadow: '0 8px 24px -10px rgba(39,166,68,0.25)',
            closeButton: {
              hoverBackground: '#d2efd9',
              focusRing: { color: '#27a644', shadow: 'none' }
            }
          },
          warn: {
            background: '#fef3e6',
            borderColor: '#fcd9b6',
            color: '#b45309',
            detailColor: '{surface.700}',
            shadow: '0 8px 24px -10px rgba(217,119,6,0.25)',
            closeButton: {
              hoverBackground: '#fbe4c8',
              focusRing: { color: '#d97706', shadow: 'none' }
            }
          },
          error: {
            background: '#fdecec',
            borderColor: '#f8c8ca',
            color: '#c92a2a',
            detailColor: '{surface.700}',
            shadow: '0 8px 24px -10px rgba(229,72,77,0.25)',
            closeButton: {
              hoverBackground: '#fad6d8',
              focusRing: { color: '#e5484d', shadow: 'none' }
            }
          },
          secondary: {
            background: '{surface.100}',
            borderColor: '{surface.300}',
            color: '{surface.700}',
            detailColor: '{surface.700}',
            shadow: '0 8px 24px -10px rgba(15,23,42,0.12)',
            closeButton: {
              hoverBackground: '{surface.200}',
              focusRing: { color: '{surface.700}', shadow: 'none' }
            }
          },
          contrast: {
            background: '{surface.900}',
            borderColor: '{surface.900}',
            color: '{surface.50}',
            detailColor: '{surface.0}',
            shadow: '0 8px 24px -10px rgba(15,23,42,0.35)',
            closeButton: {
              hoverBackground: '{surface.800}',
              focusRing: { color: '{surface.50}', shadow: 'none' }
            }
          }
        }
      }
    }
  }
})
