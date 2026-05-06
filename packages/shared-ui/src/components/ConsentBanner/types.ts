/**
 * Type definitions for ConsentBanner — PDPL 2023 Articles 11-13 cookie/consent component.
 *
 * Versioned schema (storage `version: 1`) so future migrations can detect
 * legacy state and re-prompt safely.
 */

export type ConsentCategory = 'essential' | 'analytics' | 'marketing';

/**
 * Persisted consent state. Stored in LocalStorage under `storageKey` (default `kite.consent.v1`).
 *
 * - `version` — schema version, MUST be `1` for current shape; bump on breaking changes.
 * - `timestamp` — Unix ms recorded when user gave/rejected consent.
 * - `expiresAt` — Unix ms after which we re-prompt (12 months from `timestamp` per BR-PDPL-CONSENT-002).
 * - `categories` — boolean opt-in per category. `essential` is always `true` (locked).
 */
export type ConsentState = {
  version: 1;
  timestamp: number;
  expiresAt: number;
  categories: Record<ConsentCategory, boolean>;
};

/**
 * Public props for the `<ConsentBanner>` component.
 *
 * - `privacyHref` / `cookieHref` / `termsHref` — URLs to legal pages (rendered inside banner body).
 * - `lang` — UI locale; only `'vi'` is fully translated for v1. `'en'` falls back to `'vi'`.
 * - `storageKey` — override for tests / multi-tenant cases. Defaults to `'kite.consent.v1'`.
 */
export type ConsentBannerProps = {
  privacyHref: string;
  cookieHref: string;
  termsHref: string;
  lang?: 'vi' | 'en';
  storageKey?: string;
};

/**
 * Internal — partial category map accepted by `useConsent().give(...)`.
 * Missing keys default to `false` (except `essential`, always `true`).
 */
export type PartialCategories = Partial<Record<ConsentCategory, boolean>>;
