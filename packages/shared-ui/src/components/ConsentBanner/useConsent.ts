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
 *
 * GAP-353b Wave 25 — also syncs to server-side API (best-effort, non-fatal).
 * LocalStorage stays the primary truth for offline/cross-tab; API is the cross-device
 * audit trail per BR-PDPL-CONSENT-003.
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import type { ConsentState, PartialCategories } from './types';
import {
  DEFAULT_STORAGE_KEY,
  VISITOR_ID_STORAGE_KEY,
  clearConsent,
  getOrCreateVisitorId,
  readConsent,
  writeConsent,
} from './storage';
import {
  buildPayload,
  recordConsent as apiRecordConsent,
  revokeConsent as apiRevokeConsent,
} from './api';
import { applyAnalyticsConsent } from './analytics';

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
  /** Pseudonymous visitor id (UUID v4). `null` until hydrated. */
  visitorId: string | null;
  /** Persist a partial category opt-in map. Missing keys default to `false`. */
  give: (categories: PartialCategories) => void;
  /** Reject all non-essential categories. */
  reject: () => void;
  /** Clear consent record entirely — re-prompts on next mount. */
  revoke: () => void;
};

export type UseConsentOptions = {
  /** LocalStorage key for the consent state. Defaults to `kite.consent.v1`. */
  storageKey?: string;
  /** LocalStorage key for the visitor id. Defaults to `kite_visitor_id`. */
  visitorIdKey?: string;
  /** Toggle server-side API sync (default `true`). Set `false` for unit tests / offline contexts. */
  syncToServer?: boolean;
};

export function useConsent(
  optionsOrStorageKey: string | UseConsentOptions = DEFAULT_STORAGE_KEY,
): UseConsentResult {
  const opts: UseConsentOptions =
    typeof optionsOrStorageKey === 'string'
      ? { storageKey: optionsOrStorageKey }
      : optionsOrStorageKey;
  const storageKey = opts.storageKey ?? DEFAULT_STORAGE_KEY;
  const visitorIdKey = opts.visitorIdKey ?? VISITOR_ID_STORAGE_KEY;
  const syncToServer = opts.syncToServer ?? true;

  const [state, setState] = useState<ConsentState | null>(null);
  const [hydrated, setHydrated] = useState(false);
  const [visitorId, setVisitorId] = useState<string | null>(null);
  // Track in-flight server calls so unit tests can await them via the public surface.
  const inFlight = useRef(0);

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
    setVisitorId(getOrCreateVisitorId(visitorIdKey));
    setHydrated(true);
  }, [storageKey, visitorIdKey]);

  const syncRecord = useCallback(
    (next: ConsentState) => {
      if (!syncToServer) return;
      const id = visitorId ?? getOrCreateVisitorId(visitorIdKey);
      inFlight.current += 1;
      // Best-effort: API failures must NOT throw to the caller (LocalStorage primary).
      apiRecordConsent(buildPayload(id, next.categories))
        .catch(() => {
          /* swallowed — non-fatal */
        })
        .finally(() => {
          inFlight.current -= 1;
        });
    },
    [syncToServer, visitorId, visitorIdKey],
  );

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
      applyAnalyticsConsent(next.categories);
      writeConsent(next, storageKey);
      setState(next);
      syncRecord(next);
    },
    [storageKey, syncRecord],
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
    applyAnalyticsConsent(next.categories);
    writeConsent(next, storageKey);
    setState(next);
    syncRecord(next);
  }, [storageKey, syncRecord]);

  const revoke = useCallback(() => {
    // Wave br-4 Bucket B (GAP-353b): SDK lifecycle handler fires SYNCHRONOUSLY
    // BEFORE async work — PDPL Art 14 revoke ≤5s effective.
    applyAnalyticsConsent({ essential: true, analytics: false, marketing: false });
    clearConsent(storageKey);
    setState(null);
    if (syncToServer) {
      const id = visitorId ?? getOrCreateVisitorId(visitorIdKey);
      inFlight.current += 1;
      apiRevokeConsent(id)
        .catch(() => {
          /* swallowed — non-fatal */
        })
        .finally(() => {
          inFlight.current -= 1;
        });
    }
  }, [storageKey, syncToServer, visitorId, visitorIdKey]);

  return {
    state,
    hydrated,
    analytics: state?.categories.analytics === true,
    marketing: state?.categories.marketing === true,
    visitorId,
    give,
    reject,
    revoke,
  };
}
