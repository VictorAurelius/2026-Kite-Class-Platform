'use client';

/**
 * useConsent — React hook exposing PDPL-compliant consent state machine.
 *
 * State machine (per gap GAP-353 spec):
 *   NOT_PROMPTED → PROMPTED →
 *     { CONSENT_GIVEN[essential|analytics|marketing] | REJECTED }
 *     → REVOKED → RE_PROMPTED
 *
 * Re-prompt triggers: 12-month expiry OR material policy change (latter manual via revoke()).
 *
 * SSR contract: returns `state: null` on server; hydrates on first client effect.
 * Consumers should branch on `state === null` to know "not yet hydrated OR not consented."
 */

import { useCallback, useEffect, useState } from 'react';
import type { ConsentState, PartialCategories } from './types';
import {
  DEFAULT_STORAGE_KEY,
  clearConsent,
  readConsent,
  writeConsent,
} from './storage';

const TWELVE_MONTHS_MS = 365 * 24 * 60 * 60 * 1000;

export type UseConsentResult = {
  /** Current state. `null` = not consented OR not hydrated yet (SSR-safe). */
  state: ConsentState | null;
  /** True once the hook has read from LocalStorage on the client. */
  hydrated: boolean;
  /** Convenience: analytics opt-in (false until hydrated + opt-in). */
  analytics: boolean;
  /** Convenience: marketing opt-in. */
  marketing: boolean;
  /** Persist a partial category opt-in map. Missing keys default to `false`. */
  give: (categories: PartialCategories) => void;
  /** Reject all non-essential categories. */
  reject: () => void;
  /** Clear consent record entirely — re-prompts on next mount. */
  revoke: () => void;
};

export function useConsent(storageKey: string = DEFAULT_STORAGE_KEY): UseConsentResult {
  const [state, setState] = useState<ConsentState | null>(null);
  const [hydrated, setHydrated] = useState(false);

  // Hydrate on mount (client only). Drop expired records.
  useEffect(() => {
    const stored = readConsent(storageKey);
    if (stored && stored.expiresAt > Date.now()) {
      setState(stored);
    } else if (stored && stored.expiresAt <= Date.now()) {
      // Expired — wipe and re-prompt.
      clearConsent(storageKey);
      setState(null);
    }
    setHydrated(true);
  }, [storageKey]);

  const give = useCallback(
    (categories: PartialCategories) => {
      const now = Date.now();
      const next: ConsentState = {
        version: 1,
        timestamp: now,
        expiresAt: now + TWELVE_MONTHS_MS,
        categories: {
          essential: true,
          analytics: categories.analytics === true,
          marketing: categories.marketing === true,
        },
      };
      writeConsent(next, storageKey);
      setState(next);
    },
    [storageKey],
  );

  const reject = useCallback(() => {
    const now = Date.now();
    const next: ConsentState = {
      version: 1,
      timestamp: now,
      expiresAt: now + TWELVE_MONTHS_MS,
      categories: {
        essential: true,
        analytics: false,
        marketing: false,
      },
    };
    writeConsent(next, storageKey);
    setState(next);
  }, [storageKey]);

  const revoke = useCallback(() => {
    clearConsent(storageKey);
    setState(null);
  }, [storageKey]);

  return {
    state,
    hydrated,
    analytics: state?.categories.analytics === true,
    marketing: state?.categories.marketing === true,
    give,
    reject,
    revoke,
  };
}
