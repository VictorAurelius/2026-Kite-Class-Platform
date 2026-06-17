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
const IS_PRODUCTION = process.env.NODE_ENV === 'production';

export const ALLOWED_ORIGINS = [
  // Local development
  'http://localhost:4701', // KiteHub local
  'http://localhost:4700', // KiteClass local (for testing)

  // Production
  'https://kitehub.me', // KiteHub apex (marketing + customer portal)

  // Dev-only origins — the actual `next dev` server runs on :3000, so theme
  // preview / iframe embedding from a sibling dev server was being rejected
  // with "untrusted origin" console spam. Strict allowlist still applies in
  // production (these entries are dropped when NODE_ENV === 'production').
  ...(IS_PRODUCTION
    ? []
    : [
        'http://localhost:3000', // KiteClass dev server (next dev)
        'http://127.0.0.1:3000', // loopback IP variant
        'http://localhost:3001', // KiteHub dev server (next dev, sibling port)
        'http://127.0.0.1:3001',
      ]),

  // Add custom origin from env if provided
  ...(process.env.NEXT_PUBLIC_PARENT_ORIGIN
    ? [process.env.NEXT_PUBLIC_PARENT_ORIGIN]
    : []),
];

/**
 * Dev-only loopback / nip.io matcher. nip.io hosts are dynamic
 * (`<tenant>.127.0.0.1.nip.io:3000`) so they can't be enumerated in a static
 * allowlist; accept them on any localhost/loopback/nip.io origin ONLY in dev.
 * Production stays strict (returns false → falls back to the static allowlist).
 */
function isDevOrigin(origin: string): boolean {
  if (IS_PRODUCTION) return false;
  try {
    const { hostname } = new URL(origin);
    return (
      hostname === 'localhost' ||
      hostname === '127.0.0.1' ||
      hostname.endsWith('.nip.io') ||
      hostname.endsWith('.localhost')
    );
  } catch {
    return false;
  }
}

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
  // Same-origin is inherently trusted: a page posting a message to itself
  // (sendThemeToChild defaults to window.parent, which === window on a standalone
  // tenant landing → self-post). Allowing it fixes the "untrusted origin" console
  // spam on production builds (where isDevOrigin is disabled) AND correctly permits
  // real production tenant subdomains (e.g. <slug>.kitehub.me) to self-preview theme
  // without enumerating every subdomain in the static allowlist.
  if (typeof window !== 'undefined' && origin === window.location.origin) return true;
  return ALLOWED_ORIGINS.includes(origin) || isDevOrigin(origin);
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
 * sendThemeToChild(myTheme, iframe.contentWindow, 'http://localhost:4700');
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
