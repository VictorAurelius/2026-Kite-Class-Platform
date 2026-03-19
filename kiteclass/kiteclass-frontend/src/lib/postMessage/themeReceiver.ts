/**
 * postMessage Theme Receiver
 *
 * Listens for theme updates from parent window (KiteHub iframe parent).
 * Validates origin and message structure for security.
 *
 * Used for live theme preview when user is configuring branding in KiteHub.
 *
 * @since PR-THEME-1
 */

import type { ThemeConfig } from '@/lib/theme/types';
import { isThemeMessage } from '@/lib/theme/types';

/**
 * Allowed origins for postMessage.
 * Only messages from these origins will be processed.
 *
 * SECURITY: This is critical for preventing XSS attacks.
 * Only add trusted origins.
 */
export const ALLOWED_ORIGINS = [
  // Local development
  'http://localhost:3001', // KiteHub local
  'http://localhost:3000', // KiteClass local (for testing)

  // Production
  'https://kitehub.kiteclass.com', // KiteHub production
  'https://kiteclass.com', // Main site

  // Add custom origin from env if provided
  ...(process.env.NEXT_PUBLIC_PARENT_ORIGIN
    ? [process.env.NEXT_PUBLIC_PARENT_ORIGIN]
    : []),
];

/**
 * Callback function type for theme updates.
 */
export type ThemeUpdateCallback = (theme: ThemeConfig) => void;

/**
 * Validates if a message origin is trusted.
 *
 * @param origin - Origin to validate
 * @returns True if origin is in allowed list
 */
function isAllowedOrigin(origin: string): boolean {
  return ALLOWED_ORIGINS.includes(origin);
}

/**
 * Initializes postMessage listener for theme updates.
 *
 * Listens for messages with type 'APPLY_THEME' from trusted origins.
 * Validates message structure before calling callback.
 *
 * @param onThemeUpdate - Callback function when valid theme received
 * @returns Cleanup function to remove event listener
 *
 * @example
 * ```typescript
 * const cleanup = initThemeReceiver((theme) => {
 *   console.log('Received theme:', theme);
 *   applyThemeVariables(theme);
 * });
 *
 * // Later: cleanup when component unmounts
 * cleanup();
 * ```
 */
export function initThemeReceiver(onThemeUpdate: ThemeUpdateCallback): () => void {
  const handleMessage = (event: MessageEvent) => {
    // SECURITY: Validate origin first
    if (!isAllowedOrigin(event.origin)) {
      console.warn(
        `[Theme Receiver] Rejected message from untrusted origin: ${event.origin}`
      );
      return;
    }

    // Validate message structure
    if (!isThemeMessage(event.data)) {
      // Not a theme message or invalid structure - ignore silently
      return;
    }

    // Valid theme message from trusted origin
    console.log('[Theme Receiver] Received valid theme update from:', event.origin);

    // Call callback with theme
    onThemeUpdate(event.data.theme);
  };

  // Add event listener
  window.addEventListener('message', handleMessage);

  // Return cleanup function
  return () => {
    window.removeEventListener('message', handleMessage);
  };
}

/**
 * Sends a theme to parent window (for testing/debugging).
 * Use this from KiteHub to send theme to KiteClass iframe.
 *
 * @param theme - Theme to send
 * @param targetWindow - Target window (default: parent)
 * @param targetOrigin - Target origin (default: '*' for testing)
 *
 * @example
 * ```typescript
 * // From KiteHub parent window
 * const iframe = document.querySelector('iframe');
 * sendThemeToChild(myTheme, iframe.contentWindow, 'http://localhost:3000');
 * ```
 */
export function sendThemeToChild(
  theme: ThemeConfig,
  targetWindow: Window = window.parent,
  targetOrigin: string = '*'
): void {
  const message: import('@/lib/theme/types').ThemeMessage = {
    type: 'APPLY_THEME',
    theme,
  };

  targetWindow.postMessage(message, targetOrigin);
}
