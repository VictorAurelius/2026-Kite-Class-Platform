/**
 * Analytics SDK lifecycle handler — Wave br-4 Bucket B (GAP-353b).
 *
 * Per Wave plan §3.2 + Agent 3 Cell 6 spec + PDPL Decree 13/2023 Art 14
 * ("rút lại sự đồng ý dễ dàng như cho đồng ý"): revoke must be effective ≤5s.
 *
 * This module fires Google Analytics + ad-storage gates SYNCHRONOUSLY via
 * `gtag('consent', 'update', ...)` BEFORE the server-side API call. By doing
 * the SDK update first, even if the server POST is slow or fails (offline),
 * downstream tracking calls in the same tick already see the new gates.
 *
 * Defensive: if `gtag` not defined globally (no analytics loaded, test env,
 * SSR), all functions no-op. No throw.
 */

import type { ConsentCategory } from './types';

type GtagFn = (...args: unknown[]) => void;

declare global {
  interface Window {
    gtag?: GtagFn;
    dataLayer?: unknown[];
  }
}

type GoogleConsentState = 'granted' | 'denied';

export type ConsentMap = Record<ConsentCategory, boolean>;

/**
 * Push a consent update to gtag synchronously. SSR / no-gtag safe.
 *
 * Mapping:
 *   analytics_storage    ← categories.analytics
 *   ad_storage           ← categories.marketing
 *   ad_user_data         ← categories.marketing
 *   ad_personalization   ← categories.marketing
 *   security_storage     ← always granted (essential locked-on)
 *   functionality_storage ← always granted (essential)
 *   personalization_storage ← always denied unless marketing granted
 */
export function applyAnalyticsConsent(categories: ConsentMap): void {
  if (typeof window === 'undefined') return;

  const gtag = window.gtag;
  if (typeof gtag !== 'function') {
    // Capture call to dataLayer for queued analytics SDKs that load later.
    if (!Array.isArray(window.dataLayer)) {
      try {
        window.dataLayer = window.dataLayer ?? [];
      } catch {
        return;
      }
    }
    try {
      window.dataLayer?.push({
        event: 'consent_update',
        consentSnapshot: snapshotForLayer(categories),
      });
    } catch {
      /* defensive — corrupt dataLayer */
    }
    return;
  }

  const map: Record<string, GoogleConsentState> = {
    analytics_storage: categories.analytics ? 'granted' : 'denied',
    ad_storage: categories.marketing ? 'granted' : 'denied',
    ad_user_data: categories.marketing ? 'granted' : 'denied',
    ad_personalization: categories.marketing ? 'granted' : 'denied',
    personalization_storage: categories.marketing ? 'granted' : 'denied',
    functionality_storage: 'granted',
    security_storage: 'granted',
  };

  try {
    // gtag synchronous push. Must complete before caller's next line per Wave spec.
    gtag('consent', 'update', map);
  } catch {
    /* defensive — bad gtag implementation */
  }
}

function snapshotForLayer(categories: ConsentMap): Record<string, GoogleConsentState> {
  return {
    analytics_storage: categories.analytics ? 'granted' : 'denied',
    ad_storage: categories.marketing ? 'granted' : 'denied',
  };
}
