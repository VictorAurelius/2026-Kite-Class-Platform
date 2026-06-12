# Theme System Documentation

**Version:** PR-THEME-1
**Date:** 2026-03-20

---

## Overview

The KiteClass Theme System enables per-instance branding through CSS variables, React Context, and postMessage communication. Each KiteClass instance can have unique colors, fonts, and visual styles set via AI Branding in KiteHub.

### Key Features

- **CSS Variables**: Theme applies via CSS custom properties (no JavaScript runtime overhead)
- **React Context**: Centralized theme state management with `useTheme()` hook
- **Persistence**: Automatic localStorage persistence across sessions
- **Live Preview**: postMessage API for real-time theme updates in iframe
- **SSR-Safe**: All functions handle server-side rendering gracefully
- **Type-Safe**: Full TypeScript support with runtime validation

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    KiteClass Frontend                    │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌────────────────────────────────────────────┐         │
│  │           ThemeProvider (Context)          │         │
│  │  - State management                        │         │
│  │  - localStorage persistence                │         │
│  │  - Applies CSS variables on mount/update  │         │
│  └────────────────────────────────────────────┘         │
│                        ↓                                 │
│  ┌────────────────────────────────────────────┐         │
│  │         ThemeReceiver (postMessage)        │         │
│  │  - Listens for parent window messages      │         │
│  │  - Validates origin + message structure    │         │
│  │  - Updates theme when valid message        │         │
│  └────────────────────────────────────────────┘         │
│                        ↓                                 │
│  ┌────────────────────────────────────────────┐         │
│  │           :root CSS Variables              │         │
│  │  --theme-primary                           │         │
│  │  --theme-secondary                         │         │
│  │  --theme-accent                            │         │
│  │  --theme-background                        │         │
│  │  --theme-font-heading                      │         │
│  │  --theme-font-body                         │         │
│  │  --theme-border-radius                     │         │
│  │  --theme-shadow-*                          │         │
│  └────────────────────────────────────────────┘         │
│                        ↓                                 │
│  ┌────────────────────────────────────────────┐         │
│  │         Tailwind Utilities                 │         │
│  │  bg-theme-primary                          │         │
│  │  text-theme-accent                         │         │
│  │  shadow-theme-md                           │         │
│  │  font-theme-heading                        │         │
│  └────────────────────────────────────────────┘         │
│                        ↓                                 │
│  ┌────────────────────────────────────────────┐         │
│  │            Components                      │         │
│  │  Use theme colors in UI                    │         │
│  └────────────────────────────────────────────┘         │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## Usage

### 1. Using the `useTheme` Hook

```tsx
'use client';

import { useTheme } from '@/contexts/ThemeContext';

export function MyComponent() {
  const { theme, setTheme, resetTheme, isDefaultTheme } = useTheme();

  return (
    <div>
      <p>Current primary color: {theme.colors.primary}</p>

      <button
        onClick={() => {
          setTheme({
            ...theme,
            colors: { ...theme.colors, primary: '#DC2626' },
          });
        }}
      >
        Set Red Theme
      </button>

      <button onClick={resetTheme}>Reset to Default</button>

      <p>{isDefaultTheme ? 'Using default' : 'Using custom theme'}</p>
    </div>
  );
}
```

### 2. Using Tailwind Utilities

```tsx
export function BrandedComponent() {
  return (
    <div className="bg-theme-primary text-white">
      <h1 className="font-theme-heading">Branded Heading</h1>
      <p className="font-theme-body text-theme-accent">
        Text with accent color
      </p>
      <div className="shadow-theme-md rounded-theme">
        Card with theme shadow and border radius
      </div>
    </div>
  );
}
```

### 3. Using CSS Variables Directly

```tsx
export function CustomStyledComponent() {
  return (
    <div
      style={{
        backgroundColor: 'var(--theme-background)',
        color: 'var(--theme-primary)',
        borderRadius: 'var(--theme-border-radius)',
        boxShadow: 'var(--theme-shadow-md)',
      }}
    >
      Custom styled with theme variables
    </div>
  );
}
```

### 4. Setting Up postMessage Receiver (for iframe preview)

```tsx
// In parent window (KiteHub)
import { sendThemeToChild } from '@/lib/postMessage/themeReceiver';

const iframeElement = document.querySelector('iframe');
const myTheme = { /* ThemeConfig */ };

sendThemeToChild(
  myTheme,
  iframeElement.contentWindow,
  'http://localhost:4700'
);
```

The KiteClass iframe automatically listens via `<ThemeReceiver />` in root layout.

---

## Theme Configuration Structure

### TypeScript Interface

```typescript
interface ThemeConfig {
  colors: {
    primary: string;    // Brand primary color
    secondary: string;  // Brand secondary color
    accent: string;     // Accent/highlight color
    background: string; // Page background
  };
  fonts: {
    heading: string; // Font for h1-h6
    body: string;    // Font for body text
  };
  borderRadius: string; // e.g., '8px', '12px'
  shadows: {
    sm: string; // Small shadow
    md: string; // Medium shadow
    lg: string; // Large shadow
  };
}
```

### Example JSON

```json
{
  "colors": {
    "primary": "#1E40AF",
    "secondary": "#3B82F6",
    "accent": "#F59E0B",
    "background": "#FFFBF5"
  },
  "fonts": {
    "heading": "Inter",
    "body": "Inter"
  },
  "borderRadius": "12px",
  "shadows": {
    "sm": "0 1px 2px rgba(0,0,0,0.05)",
    "md": "0 4px 6px rgba(0,0,0,0.07)",
    "lg": "0 10px 15px rgba(0,0,0,0.1)"
  }
}
```

---

## Available Tailwind Utilities

### Colors
- `bg-theme-primary` - Background with primary color
- `bg-theme-secondary` - Background with secondary color
- `bg-theme-accent` - Background with accent color
- `bg-theme-background` - Background with theme background color
- `text-theme-primary` - Text with primary color
- `text-theme-secondary` - Text with secondary color
- `text-theme-accent` - Text with accent color
- `border-theme-primary` - Border with primary color

### Shadows
- `shadow-theme-sm` - Small theme shadow
- `shadow-theme-md` - Medium theme shadow
- `shadow-theme-lg` - Large theme shadow

### Border Radius
- `rounded-theme` - Border radius from theme

### Fonts
- `font-theme-heading` - Heading font from theme
- `font-theme-body` - Body font from theme

---

## API Reference

### `useTheme()` Hook

Returns theme context value:

```typescript
{
  theme: ThemeConfig;        // Current theme
  setTheme: (theme: ThemeConfig) => void;  // Update theme
  resetTheme: () => void;    // Reset to default
  isDefaultTheme: boolean;   // True if using default
}
```

**Throws:** Error if used outside `<ThemeProvider>`

### `applyThemeVariables(theme: ThemeConfig): void`

Applies theme to document root. Called automatically by `ThemeProvider`.

### `removeThemeVariables(): void`

Removes all theme CSS variables. Called by `resetTheme()`.

### `initThemeReceiver(callback): () => void`

Initializes postMessage listener. Returns cleanup function.

```typescript
const cleanup = initThemeReceiver((theme) => {
  console.log('Received theme:', theme);
});

// Later:
cleanup();
```

---

## Security Considerations

### postMessage Origin Validation

The theme receiver validates message origins against a whitelist:

```typescript
const ALLOWED_ORIGINS = [
  'http://localhost:4701',           // KiteHub local
  'https://kitehub.me',   // KiteHub production
  // ... other trusted origins
];
```

**CRITICAL**: Only add trusted origins to prevent XSS attacks.

### Message Structure Validation

All postMessage data is validated with type guards before processing:

```typescript
if (!isThemeMessage(event.data)) {
  // Reject silently
  return;
}
```

This prevents malicious payloads from being processed.

---

## Default Theme

The default theme is applied when:
- Instance has not configured AI Branding
- AI Branding is still processing
- User resets theme
- Stored theme is invalid

```typescript
DEFAULT_THEME = {
  colors: {
    primary: '#3B82F6',    // Tailwind blue-500
    secondary: '#8B5CF6',  // Tailwind violet-500
    accent: '#F59E0B',     // Tailwind amber-500
    background: '#FFFFFF', // White
  },
  fonts: {
    heading: 'Inter',
    body: 'Inter',
  },
  borderRadius: '8px',
  shadows: {
    sm: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
    md: '0 4px 6px -1px rgba(0, 0, 0, 0.1), ...',
    lg: '0 10px 15px -3px rgba(0, 0, 0, 0.1), ...',
  },
};
```

---

## localStorage Persistence

Theme is automatically saved to localStorage when `setTheme()` is called:

**Key:** `kiteclass_theme`
**Value:** JSON-stringified `ThemeConfig`

The theme is loaded on app initialization and validated before use. Invalid themes fall back to `DEFAULT_THEME`.

---

## Testing

### Unit Tests

```bash
pnpm test src/lib/theme/__tests__/
pnpm test src/contexts/__tests__/ThemeContext.test.tsx
```

### E2E Tests

```bash
pnpm test:e2e -- e2e/theme.spec.ts
```

### Visual Testing

1. Run app: `pnpm dev`
2. Open browser DevTools
3. Execute in console:
   ```javascript
   // Test theme change
   window.postMessage({
     type: 'APPLY_THEME',
     theme: {
       colors: { primary: '#DC2626', secondary: '#EF4444', accent: '#F59E0B', background: '#FFF' },
       fonts: { heading: 'Inter', body: 'Inter' },
       borderRadius: '12px',
       shadows: { sm: '...', md: '...', lg: '...' }
     }
   }, '*');
   ```

---

## Troubleshooting

### Theme not applying

**Check:**
1. Is component wrapped in `<ThemeProvider>`?
2. Are CSS variables defined in `globals.css`?
3. Check browser console for errors
4. Verify theme structure matches `ThemeConfig` interface

### postMessage not working

**Check:**
1. Is `ThemeReceiver` component mounted?
2. Is message origin in `ALLOWED_ORIGINS`?
3. Does message match `ThemeMessage` structure?
4. Check browser console for validation warnings

### localStorage not persisting

**Check:**
1. Is localStorage available (not in incognito mode)?
2. Is localStorage quota exceeded?
3. Check browser console for storage errors

---

## Future Enhancements

- [ ] Dark mode support (PR-THEME-2)
- [ ] Theme templates (PR-THEME-3)
- [ ] Color palette generator (PR-THEME-4)
- [ ] Theme export/import (PR-THEME-5)
- [ ] Visual theme editor UI (PR-THEME-6)

---

## Related Documentation

- [Design Document](../../../documents/03-planning/kiteclass-theme-system-design.md)
- [Tailwind Config](../tailwind.config.ts)
- [Global Styles](../src/app/globals.css)

---

**Questions?** Contact the KiteClass Team or check GitHub Issues.
