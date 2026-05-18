/**
 * useOnboardingPhase — shared state hook for staggered UI reveal (Wave 98 GAP-656).
 *
 * Derives an OnboardingPhase enum from JWT claims (when available) + cookie
 * dismissal markers. Used by OnboardingCoordinator to decide which UI surface
 * is active at first-login (banner → modal → support menu).
 *
 * Per GAP-656 §Proposed Fix Step 1: 5 phases.
 *  - anonymous:    user chua dang nhap (public landing pages)
 *  - first-login:  authenticated lan dau (JWT.createdAt within 5min of now)
 *  - day-1:        within 24h of first-login (onboarding checklist priority)
 *  - day-7:        7-day mark (feedback survey reminder window)
 *  - steady:       moi phase con lai (chi `?` button visible)
 *
 * Per Step 5: dismissal state persists via httpOnly cookie set by
 * PreferencesController. Client doc qua `document.cookie` cho banner-dismissed
 * markers (cookie KHONG httpOnly cho banner state per simplicity v1; future
 * may upgrade to httpOnly + same-doc fetch endpoint).
 *
 * @since Wave 98 — GAP-656
 */

'use client';

import { useEffect, useState } from 'react';

export type OnboardingPhase = 'anonymous' | 'first-login' | 'day-1' | 'day-7' | 'steady';

export interface UseOnboardingPhaseResult {
  phase: OnboardingPhase;
  /** Cookie-readable dismissal markers keyed by banner key. */
  dismissed: Record<string, boolean>;
}

const FIRST_LOGIN_WINDOW_MS = 5 * 60 * 1000; // 5 phut
const ONE_DAY_MS = 24 * 60 * 60 * 1000;
const SEVEN_DAYS_MS = 7 * ONE_DAY_MS;

/** Cookie prefix for dismissal markers — synced với PreferencesController. */
export const DISMISSAL_COOKIE_PREFIX = 'kite-banner-dismissed-';

interface JwtClaims {
  /** Unix timestamp (seconds) when user account was created. */
  createdAt?: number;
  /** Unix timestamp (seconds) of last login. */
  lastLogin?: number;
  /** Authenticated role enum (e.g. 'PLATFORM_ADMIN', 'P2_CENTER_OWNER'). */
  role?: string;
  /** Authenticated user id (UUID). */
  sub?: string;
}

/**
 * Read JWT from sessionStorage facade or fallback to window-level token.
 * Returns null when no token present (anonymous).
 */
function readJwtClaims(): JwtClaims | null {
  if (typeof window === 'undefined') {
    return null;
  }
  try {
    const token = window.sessionStorage.getItem('kitehub-jwt') ?? window.sessionStorage.getItem('jwt');
    if (!token) {
      return null;
    }
    const parts = token.split('.');
    if (parts.length !== 3) {
      return null;
    }
    const payloadBase = parts[1];
    if (!payloadBase) {
      return null;
    }
    const payload = JSON.parse(atob(payloadBase.replace(/-/g, '+').replace(/_/g, '/')));
    return {
      createdAt: typeof payload.createdAt === 'number' ? payload.createdAt : payload.iat,
      lastLogin: typeof payload.lastLogin === 'number' ? payload.lastLogin : payload.iat,
      role: typeof payload.role === 'string' ? payload.role : undefined,
      sub: typeof payload.sub === 'string' ? payload.sub : undefined,
    };
  } catch {
    return null;
  }
}

/**
 * Read non-httpOnly cookies for dismissal state. PreferencesController sets
 * `kite-banner-dismissed-{bannerKey}=1` server-side; client can detect via
 * document.cookie when SameSite=Lax allows visibility.
 */
function readDismissalCookies(): Record<string, boolean> {
  if (typeof document === 'undefined') {
    return {};
  }
  const result: Record<string, boolean> = {};
  const cookies = document.cookie ? document.cookie.split('; ') : [];
  for (const c of cookies) {
    const [key, value] = c.split('=');
    if (key && key.startsWith(DISMISSAL_COOKIE_PREFIX)) {
      const bannerKey = key.slice(DISMISSAL_COOKIE_PREFIX.length);
      result[bannerKey] = value === '1';
    }
  }
  return result;
}

function derivePhase(claims: JwtClaims | null, now: number): OnboardingPhase {
  if (!claims) {
    return 'anonymous';
  }
  const createdAtMs = claims.createdAt ? claims.createdAt * 1000 : null;
  if (!createdAtMs) {
    return 'steady';
  }
  const ageMs = now - createdAtMs;
  if (ageMs < FIRST_LOGIN_WINDOW_MS) {
    return 'first-login';
  }
  if (ageMs < ONE_DAY_MS) {
    return 'day-1';
  }
  if (ageMs < SEVEN_DAYS_MS) {
    return 'day-7';
  }
  return 'steady';
}

export function useOnboardingPhase(): UseOnboardingPhaseResult {
  const [phase, setPhase] = useState<OnboardingPhase>('anonymous');
  const [dismissed, setDismissed] = useState<Record<string, boolean>>({});

  useEffect(() => {
    const claims = readJwtClaims();
    setPhase(derivePhase(claims, Date.now()));
    setDismissed(readDismissalCookies());

    // Re-check dismissal markers on focus (cross-tab sync via cookie).
    const handleFocus = () => {
      setDismissed(readDismissalCookies());
    };
    if (typeof window !== 'undefined') {
      window.addEventListener('focus', handleFocus);
      return () => window.removeEventListener('focus', handleFocus);
    }
    return undefined;
  }, []);

  return { phase, dismissed };
}
