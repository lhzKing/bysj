import { enterpriseTokens } from './tokens'

export const enterpriseTheme = {
  primitive: enterpriseTokens.primitive,
  semantic: {
    primary: {
      50: '{blue.50}',
      100: '{blue.100}',
      500: '{blue.500}',
      600: '{blue.600}',
      700: '{blue.700}'
    },
    colorScheme: {
      light: {
        surface: {
          0: '{slate.0}',
          50: '{slate.50}',
          100: '{slate.100}',
          200: '{slate.200}',
          900: '{slate.900}'
        },
        formField: {
          background: '{slate.0}',
          borderColor: '{slate.200}',
          hoverBorderColor: '{blue.500}',
          focusBorderColor: '{blue.600}',
          color: '{slate.900}',
          placeholderColor: '{slate.500}'
        }
      }
    },
    focusRing: {
      width: '2px',
      style: 'solid',
      color: '{blue.600}',
      offset: '1px'
    }
  }
}
