/**
 * Auth role helpers — unify backend role names (PLATFORM_ADMIN vs legacy ADMIN)
 * so FE role-guards accept both. Backend AuthService seeds `PLATFORM_ADMIN`;
 * legacy `ADMIN` retained for backward compatibility.
 *
 * GAP-518: FE role-guard unification (Wave 72a Bucket C).
 */

/**
 * Returns true if the role grants platform-admin privileges
 * (kitehub-frontend `/admin/**` routes, beta-request approval, etc.).
 *
 * Note: This is distinct from tenant-scoped `ADMIN` used by
 * `(school-admin)/layout.tsx` — that route group uses a different layout
 * + authenticates by login alone (no role gate). See that layout's javadoc.
 */
export function isPlatformAdmin(role: string | undefined | null): boolean {
  return role === 'PLATFORM_ADMIN' || role === 'ADMIN';
}
