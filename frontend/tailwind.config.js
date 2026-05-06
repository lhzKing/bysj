/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#6366f1',
          glow: 'rgba(99, 102, 241, 0.15)'
        },
        surface: {
          DEFAULT: '#ffffff',
          glass: 'rgba(255, 255, 255, 0.6)'
        },
        bg: {
          light: '#fdfdff',
          DEFAULT: '#fdfdff'
        },
        accent: {
          green: '#10B981',
          blue: '#3B82F6',
          indigo: '#6366f1',
          emerald: '#10b981',
        },
        text: {
          primary: '#0f172a',
          secondary: '#64748b',
          muted: '#94a3b8',
        }
      },
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'Inter', 'system-ui', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'monospace'],
      },
      borderRadius: {
        '4xl': '32px',
        '5xl': '40px',
        '6xl': '48px',
        '7xl': '56px',
      },
      boxShadow: {
        'glass': '0 10px 40px -6px rgba(0, 0, 0, 0.02), 0 20px 80px -12px rgba(99, 102, 241, 0.08)',
        'glass-hover': '0 30px 100px -15px rgba(99, 102, 241, 0.15)',
      }
    },
  },
  plugins: [],
}
