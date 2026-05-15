/**
 * useRole hook — exposes current user's canonical platform role + role-check helpers.
 *
 * <p>GAP-562b (Wave 80 Bucket C): canonical OWNER / STAFF role separation enforced
 * at the FE. Legacy `PLATFORM_ADMIN` and `ADMIN` aliases resolve to OWNER until the
 * Wave 81 cutoff (2026-06-14), matching backend {@code PlatformRole.fromStoredValue}.</p>
 *
 * <p>Pattern: read from `useAuthStore` (zustand persist); `isLoading` becomes false
 * once zustand hydrates from localStorage. Components MUST wait for `isLoading=false`
 * before making redirect decisions to avoid flicker / false-negative role mismatch.</p>
 */

import { useEffect, useState } from 'react';
import { useAuthStore } from '@/stores/auth-store';

/** Canonical platform roles. Legacy `PLATFORM_ADMIN` / `ADMIN` map to OWNER. */
export type PlatformRole = 'OWNER' | 'STAFF';

/** Stored role values accepted from backend (canonical + legacy aliases). */
export type StoredRole = 'OWNER' | 'STAFF' | 'PLATFORM_ADMIN' | 'ADMIN';

const OWNER_ALIASES: ReadonlySet<string> = new Set(['OWNER', 'PLATFORM_ADMIN', 'ADMIN']);
const STAFF_ALIASES: ReadonlySet<string> = new Set(['STAFF']);

/**
 * Resolve a stored role value (canonical OR legacy alias) to the canonical
 * `PlatformRole` enum value. Returns `null` if unknown.
 *
 * Mirrors backend `PlatformRole.fromStoredValue` so FE / BE role checks stay
 * in lockstep through the Wave 79 → Wave 81 alias window.
 */
export function resolveStoredRole(stored: string | null | undefined): PlatformRole | null {
  if (!stored) return null;
  const upper = stored.trim().toUpperCase();
  if (OWNER_ALIASES.has(upper)) return 'OWNER';
  if (STAFF_ALIASES.has(upper)) return 'STAFF';
  return null;
}

export interface UseRoleResult {
  /** Canonical role (OWNER | STAFF) or null if unauthenticated / unknown. */
  role: PlatformRole | null;
  /** True until zustand store hydrates from localStorage (SSR-safe). */
  isLoading: boolean;
  /** Returns true if current role is in the allowed set. */
  hasRole: (allowedRoles: readonly PlatformRole[]) => boolean;
}

/**
 * Read the current user's role from the auth store and resolve to canonical.
 *
 * <p>Returns `{ isLoading: true }` until the underlying zustand store has
 * hydrated from localStorage (one render tick after mount). Components
 * using this hook for role-guard decisions MUST gate the decision on
 * `isLoading === false`.</p>
 */
export function useRole(): UseRoleResult {
  const user = useAuthStore((state) => state.user);
  const [isHydrated, setIsHydrated] = useState(false);

  useEffect(() => {
    setIsHydrated(true);
  }, []);

  const role = isHydrated ? resolveStoredRole(user?.role) : null;

  const hasRole = (allowedRoles: readonly PlatformRole[]): boolean => {
    if (!isHydrated || role === null) return false;
    return allowedRoles.includes(role);
  };

  return {
    role,
    isLoading: !isHydrated,
    hasRole,
  };
}
