'use client';

/**
 * ConsentGatedAnalytics — gate Google Analytics injection on user consent.
 *
 * PDPL 2023 Art 11 + Decree 13/2023/NĐ-CP Art 4 require explicit opt-in BEFORE
 * tracking cookies / non-essential analytics fire. The root layout used to mount
 * `<GoogleAnalytics />` unconditionally when `NEXT_PUBLIC_GA_ID` was set — that
 * triggered GA's gtag.js before the user had a chance to opt-in via the
 * `<ConsentBanner />` shipped by GAP-353. This wrapper closes that gap by
 * reading the consent state from `useConsent()` and only mounting GA when the
 * `analytics` category has been granted.
 *
 * SSR safety: `useConsent()` returns `hydrated=false, state=null` on the server,
 * so this component renders `null` until the client has hydrated and read the
 * persisted consent record from LocalStorage. No GA script tag is emitted in
 * the server-rendered HTML — confirmed via Network tab inspection (essential
 * cookies only on first paint).
 *
 * Once the user clicks "Đồng ý tất cả" or saves a customized selection that
 * includes analytics, `useConsent()` triggers a re-render and GA mounts. This
 * matches the GA4-recommended pattern of "deferred load on consent" without
 * needing the more invasive `gtag('consent', 'update', ...)` flow (which still
 * loads gtag.js even in denied state — banned under PDPL).
 *
 * Closes GAP-558 (Wave 83 Bucket E — PDPL Art 11 + Decree 13/2023 Art 4 compliance).
 * Builds on GAP-353 (Wave 23 Bucket BC ConsentBanner) + GAP-368 (Wave 23 Bucket F
 * cookie policy page).
 *
 * @since Wave 83 — GAP-558
 */

import { GoogleAnalytics } from '@next/third-parties/google';
import { useConsent } from '@kite/shared-ui';

export type ConsentGatedAnalyticsProps = {
  /**
   * Google Analytics 4 measurement ID (format `G-XXXXXXXXXX`).
   * When omitted / undefined, the component renders nothing — the
   * caller normally wires this from `process.env.NEXT_PUBLIC_GA_ID`.
   */
  gaId: string | undefined;
};

export function ConsentGatedAnalytics({ gaId }: ConsentGatedAnalyticsProps) {
  const { hydrated, analytics } = useConsent();

  // PDPL Art 11 gate: never mount GA until (a) client hydrated, (b) measurement
  // ID is configured, and (c) user explicitly opted into analytics. Order
  // matters — short-circuit on hydration first to keep server output empty.
  if (!hydrated) return null;
  if (!gaId) return null;
  if (!analytics) return null;

  return <GoogleAnalytics gaId={gaId} />;
}
