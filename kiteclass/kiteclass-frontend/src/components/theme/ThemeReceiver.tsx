/**
 * Theme Receiver Component
 *
 * Initializes postMessage listener for theme updates from parent window.
 * Used when KiteClass is embedded in KiteHub for live preview.
 *
 * @since PR-THEME-1
 */

'use client';

import { useEffect } from 'react';
import { initThemeReceiver } from '@/lib/postMessage/themeReceiver';
import { useTheme } from '@/contexts/ThemeContext';

/**
 * Theme Receiver Component
 *
 * Automatically sets up postMessage listener on mount.
 * Cleans up listener on unmount.
 *
 * Place this component at root level to enable theme preview.
 *
 * @example
 * ```tsx
 * <ThemeProvider>
 *   <ThemeReceiver />
 *   <App />
 * </ThemeProvider>
 * ```
 */
export function ThemeReceiver() {
  const { setTheme } = useTheme();

  useEffect(() => {
    // Initialize postMessage listener
    const cleanup = initThemeReceiver((theme) => {
      console.log('[ThemeReceiver] Applying theme from parent window:', theme);
      setTheme(theme);
    });

    // Cleanup on unmount
    return cleanup;
  }, [setTheme]);

  // This component doesn't render anything
  return null;
}
