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

/**
 * LocalStorage key for the pseudonymous visitor identifier.
 * Used by the GAP-353b server-side consent API to correlate cross-device records.
 */
export const VISITOR_ID_STORAGE_KEY = 'kite_visitor_id';

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
 * Read or generate the pseudonymous visitor id (UUID v4) from LocalStorage.
 *
 * SSR-safe: returns a fresh UUID without persisting on the server. Subsequent
 * client-side calls then return the persisted value once the hook hydrates.
 *
 * Falls back to a random `Math.random`-based UUID when `crypto.randomUUID` is
 * unavailable (older browsers / non-secure contexts).
 */
export function getOrCreateVisitorId(
  storageKey: string = VISITOR_ID_STORAGE_KEY,
): string {
  if (typeof window === 'undefined') {
    return generateUuid();
  }
  let stored: string | null = null;
  try {
    stored = window.localStorage.getItem(storageKey);
  } catch {
    return generateUuid();
  }
  if (stored && isValidUuid(stored)) {
    return stored;
  }
  const fresh = generateUuid();
  try {
    window.localStorage.setItem(storageKey, fresh);
  } catch {
    // Quota / disabled storage — return ephemeral value.
  }
  return fresh;
}

function generateUuid(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  // RFC 4122 v4 fallback (low-entropy but acceptable for pseudonymous id).
  const hex = '0123456789abcdef';
  let out = '';
  for (let i = 0; i < 36; i++) {
    if (i === 8 || i === 13 || i === 18 || i === 23) {
      out += '-';
      continue;
    }
    if (i === 14) {
      out += '4';
      continue;
    }
    if (i === 19) {
      out += hex[8 + Math.floor(Math.random() * 4)];
      continue;
    }
    out += hex[Math.floor(Math.random() * 16)];
  }
  return out;
}

function isValidUuid(value: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value);
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
