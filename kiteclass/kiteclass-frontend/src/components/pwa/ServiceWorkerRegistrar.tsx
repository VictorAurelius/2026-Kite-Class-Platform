/**
 * Service Worker registration shim.
 *
 * Wave 49 Bucket 0 (Track 2 Phase 4 PWA infra). Mounted once in root
 * layout. Registers `/sw.js` after page load when in production-like
 * environments. Skips registration in:
 *   - SSR (typeof window === 'undefined')
 *   - Test (NODE_ENV === 'test')
 *   - Browsers without Service Worker support
 *
 * Doesn't render any UI. Logs failures quietly so a SW registration
 * problem never blocks page paint.
 */

'use client';

import { useEffect } from 'react';

export function ServiceWorkerRegistrar(): null {
  useEffect(() => {
    if (process.env.NODE_ENV === 'test') return;
    if (typeof window === 'undefined') return;
    if (!('serviceWorker' in navigator)) return;

    // Defer registration past load so it doesn't compete with critical
    // resources for bandwidth on first visit.
    const register = () => {
      navigator.serviceWorker
        .register('/sw.js', { scope: '/' })
        .catch((err) => {
          // eslint-disable-next-line no-console
          console.warn('[pwa] Service worker registration failed:', err);
        });
    };

    if (document.readyState === 'complete') {
      register();
      return;
    }

    window.addEventListener('load', register, { once: true });
    return () => window.removeEventListener('load', register);
  }, []);

  return null;
}
