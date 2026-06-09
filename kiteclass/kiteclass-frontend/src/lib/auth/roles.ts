/**
 * KiteClass role model — single source of truth for FE role-based routing + guards.
 *
 * Wave RBAC-Shell 1 Bucket A (GAP-1122). Closes the role-name parity gap (Risk #3,
 * Wave 78 GAP-518 precedent `PLATFORM_ADMIN` vs `ADMIN`): the backend emits role
 * names in several vocabularies and the FE MUST normalize them into ONE canonical
 * set before deciding redirects or access:
 *
 *   - tenant-auth Option B (`AuthCredentialProvisioningService`, BR-AUTH-002):
 *     JWT `role` claim ∈ {PARENT, TEACHER, STUDENT}.
 *   - KiteHub subscription (OWNER/STAFF SSO, Bucket C): role OWNER / OWNER,ADMIN.
 *   - ADR-003 / V30 hierarchical `Role` entity: TENANT_OWNER, PRINCIPAL,
 *     VICE_PRINCIPAL, DEPT_HEAD, HOMEROOM_TEACHER, SUBJECT_TEACHER, ACCOUNTANT,
 *     RECEPTIONIST, STUDENT, PARENT, PLATFORM_ADMIN.
 *   - @PreAuthorize literals across controllers: OWNER, ADMIN, PRINCIPAL, TEACHER,
 *     PARENT, STUDENT.
 *
 * `normalizeRole` collapses all of the above into {@link UserType}. This is a
 * FE-only adapter — it does NOT change any BE rule (see GAP-1122 §Parity finding
 * for the recommended BE-side @PreAuthorize alignment follow-up).
 *
 * @author KiteClass Team
 */

import { UserType } from '@/types/auth';

/** Canonical FE role (subset of {@link UserType} used for routing + guards). */
export type KcRole = UserType;

/** Canonical role tokens — exact match (already in FE vocabulary). */
const CANONICAL: Record<string, KcRole> = {
  OWNER: UserType.OWNER,
  ADMIN: UserType.ADMIN,
  STAFF: UserType.STAFF,
  TEACHER: UserType.TEACHER,
  PARENT: UserType.PARENT,
  STUDENT: UserType.STUDENT,
};

/**
 * BE drift literals → FE canonical (Risk #3 reconcile). Maps the ADR-003
 * hierarchical names + platform-admin variants into the 6-role FE set.
 */
const ALIASES: Record<string, KcRole> = {
  TENANT_OWNER: UserType.OWNER,
  PLATFORM_ADMIN: UserType.ADMIN,
  PRINCIPAL: UserType.ADMIN,
  VICE_PRINCIPAL: UserType.ADMIN,
  DEPT_HEAD: UserType.STAFF,
  HOMEROOM_TEACHER: UserType.TEACHER,
  SUBJECT_TEACHER: UserType.TEACHER,
  ACCOUNTANT: UserType.STAFF,
  RECEPTIONIST: UserType.STAFF,
};

/**
 * Normalize any backend role token to the canonical FE {@link KcRole}.
 *
 * @param raw role string from the JWT `role` claim / login response (any vocabulary).
 * @returns the canonical role, or `null` when the token is empty/unrecognized
 *          (callers redirect such users to a neutral landing, never guess).
 */
export function normalizeRole(raw?: string | null): KcRole | null {
  if (!raw) return null;
  const key = raw.trim().toUpperCase();
  return CANONICAL[key] ?? ALIASES[key] ?? null;
}

/**
 * Per-role landing route. Role-specific personas (teacher/parent/student) get
 * their own shell; owner/staff/admin share the `(dashboard)` shell for Bucket A
 * (per-role owner/staff nav is Bucket B). STUDENT home is scaffolded but gated by
 * KC-9 (student auth) — the route resolves, the login path is not yet functional.
 */
const ROLE_HOME: Record<KcRole, string> = {
  [UserType.OWNER]: '/dashboard',
  [UserType.ADMIN]: '/dashboard',
  [UserType.STAFF]: '/dashboard',
  [UserType.TEACHER]: '/teacher',
  [UserType.PARENT]: '/parent',
  [UserType.STUDENT]: '/student',
};

/**
 * @param role canonical role.
 * @returns the role's home route (falls back to the shared `/dashboard` shell).
 */
export function roleHome(role: KcRole): string {
  return ROLE_HOME[role] ?? '/dashboard';
}

/**
 * @param role the actor's canonical role (may be null/undefined when unresolved).
 * @param allowed the route group's allow-list.
 * @returns true only when a known role is present in the allow-list.
 */
export function canAccess(
  role: KcRole | null | undefined,
  allowed: readonly KcRole[],
): boolean {
  if (!role) return false;
  return allowed.includes(role);
}
