/**
 * LocalStorage adapter for ConsentBanner persistence.
 *
 * SSR-safe: every public function checks `typeof window` and returns no-op / null on server.
 * Schema-validating: reads with malformed JSON or wrong shape are dropped (treated as
 * "never consented") rather than throwing. This protects against:
 *  - corrupt entries from older app versions
 *  - users tampering with LocalStorage manually
 *  - schema bumps where old versions must trigger re-prompt
 */

import type { ConsentState, ConsentCategory } from './types';

export const DEFAULT_STORAGE_KEY = 'kite.consent.v1';

const VALID_CATEGORIES: ReadonlyArray<ConsentCategory> = [
  'essential',
  'analytics',
  'marketing',
];

/**
 * Read consent state from LocalStorage. Returns `null` when:
 *  - running on server (SSR)
 *  - LocalStorage unavailable (private mode, quota errors)
 *  - key missing
 *  - JSON parse fails
 *  - schema does not validate (wrong version, missing fields, wrong types)
 */
export function readConsent(storageKey: string = DEFAULT_STORAGE_KEY): ConsentState | null {
  if (typeof window === 'undefined') return null;
  let raw: string | null = null;
  try {
    raw = window.localStorage.getItem(storageKey);
  } catch {
    return null;
  }
  if (!raw) return null;

  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    return null;
  }

  return validate(parsed);
}

/** Persist consent state. No-op on SSR. Swallows quota / serialization errors. */
export function writeConsent(
  state: ConsentState,
  storageKey: string = DEFAULT_STORAGE_KEY,
): void {
  if (typeof window === 'undefined') return;
  try {
    window.localStorage.setItem(storageKey, JSON.stringify(state));
  } catch {
    // Quota exceeded / disabled storage — caller treats as ephemeral consent.
  }
}

/** Remove consent record (revocation flow per BR-PDPL-CONSENT-004). */
export function clearConsent(storageKey: string = DEFAULT_STORAGE_KEY): void {
  if (typeof window === 'undefined') return;
  try {
    window.localStorage.removeItem(storageKey);
  } catch {
    // ignore
  }
}

/**
 * Schema validator — returns the validated `ConsentState` or `null` if any
 * required field is missing / wrong type / out of range.
 *
 * Exported (named) for unit tests; the runtime API uses it via `readConsent`.
 */
export function validate(raw: unknown): ConsentState | null {
  if (!raw || typeof raw !== 'object') return null;
  const obj = raw as Record<string, unknown>;

  if (obj.version !== 1) return null;
  if (typeof obj.timestamp !== 'number' || !Number.isFinite(obj.timestamp)) return null;
  if (typeof obj.expiresAt !== 'number' || !Number.isFinite(obj.expiresAt)) return null;
  if (!obj.categories || typeof obj.categories !== 'object') return null;

  const cats = obj.categories as Record<string, unknown>;
  const validatedCats: Record<ConsentCategory, boolean> = {
    essential: true, // always-on, ignore stored value for safety
    analytics: false,
    marketing: false,
  };
  for (const key of VALID_CATEGORIES) {
    const value = cats[key];
    if (typeof value !== 'boolean') {
      // Missing or wrong type => fall back to safe default, but only DROP entirely
      // if `essential` itself is corrupt (sanity check).
      if (key === 'essential') {
        // tolerate — we always force essential=true above
        continue;
      }
      // analytics / marketing missing => treat as opted-out (safe default).
      continue;
    }
    validatedCats[key] = value;
  }
  // essential is always true regardless of stored value.
  validatedCats.essential = true;

  return {
    version: 1,
    timestamp: obj.timestamp,
    expiresAt: obj.expiresAt,
    categories: validatedCats,
  };
}
