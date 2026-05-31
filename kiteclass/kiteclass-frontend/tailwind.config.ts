import type { Config } from 'tailwindcss';

const config: Config = {
  darkMode: ['class'],
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        // shadcn/ui colors (existing)
        border: 'hsl(var(--border))',
        input: 'hsl(var(--input))',
        ring: 'hsl(var(--ring))',
        background: 'hsl(var(--background))',
        foreground: 'hsl(var(--foreground))',
        primary: {
          DEFAULT: 'hsl(var(--primary))',
          foreground: 'hsl(var(--primary-foreground))',
        },
        secondary: {
          DEFAULT: 'hsl(var(--secondary))',
          foreground: 'hsl(var(--secondary-foreground))',
        },
        destructive: {
          DEFAULT: 'hsl(var(--destructive))',
          foreground: 'hsl(var(--destructive-foreground))',
        },
        muted: {
          DEFAULT: 'hsl(var(--muted))',
          foreground: 'hsl(var(--muted-foreground))',
        },
        accent: {
          DEFAULT: 'hsl(var(--accent))',
          foreground: 'hsl(var(--accent-foreground))',
        },
        popover: {
          DEFAULT: 'hsl(var(--popover))',
          foreground: 'hsl(var(--popover-foreground))',
        },
        card: {
          DEFAULT: 'hsl(var(--card))',
          foreground: 'hsl(var(--card-foreground))',
        },
        // Theme system colors (new - PR-THEME-1)
        // CSS vars store RGB values (e.g., '59 130 246') for opacity support
        'theme-primary': 'rgb(var(--theme-primary) / <alpha-value>)',
        'theme-secondary': 'rgb(var(--theme-secondary) / <alpha-value>)',
        'theme-accent': 'rgb(var(--theme-accent) / <alpha-value>)',
        'theme-cta': 'rgb(var(--theme-cta) / <alpha-value>)',
        'theme-background': 'rgb(var(--theme-background) / <alpha-value>)',
      },
      borderRadius: {
        lg: 'var(--radius)',
        md: 'calc(var(--radius) - 2px)',
        sm: 'calc(var(--radius) - 4px)',
        // Theme system border radius
        theme: 'var(--theme-border-radius)',
      },
      boxShadow: {
        // Theme system shadows
        'theme-sm': 'var(--theme-shadow-sm)',
        'theme-md': 'var(--theme-shadow-md)',
        'theme-lg': 'var(--theme-shadow-lg)',
      },
      fontFamily: {
        // Theme system fonts
        'theme-heading': 'var(--theme-font-heading)',
        'theme-body': 'var(--theme-font-body)',
      },
    },
  },
  plugins: [require('tailwindcss-animate')],
};

export default config;
