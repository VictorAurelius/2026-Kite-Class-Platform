# GAP-1013: Auth credential hardening cluster (auth-1)

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-06 (Wave auth-1 post-wave audit suite — business-logic P2×4 + P3×3 + ops-readiness P2)
**Affects:** `kiteclass-core` auth module (AuthService / AuthCredentialProvisioningService / AuthCredential / AuthTokenService)

## Problem

Cluster of non-blocking auth hardening items surfaced by the post-wave audit suite (none P0/P1; defense-in-depth + hygiene):

1. **setPassword cross-entity rotation** — `AuthCredentialProvisioningService.setPassword` looks up by email, ignores entityType/entityId mismatch → admin setting a teacher password on an email that belongs to a PARENT rotates the PARENT credential (`:88-100`).
2. **No credential-disable on entity deactivate** — soft-deleting/INACTIVE parent/teacher entity does not disable the `auth_credentials` row → login still works (`AuthCredential.java:67-69`).
3. **Password policy asymmetry** — `SetPasswordRequest` (teacher) is weaker than BR-PARENT-PWD-002 (parent), same `auth_credentials` table (`SetPasswordRequest.java:15-17`).
4. **Login logs email plaintext (PII)** — `AuthService.java:48` (scrubber GAP-116 not active yet).
5. **JWT missing jti/iss/aud** — no revocation handle, no issuer/audience validation (`AuthTokenService.java:65-75`).
6. **Timing side-channel user-enumeration** — BCrypt skipped when email not found → response-time oracle (`AuthService.java:43-50`).

## Proposed Fix

(1) setPassword reject/409 on entityType/entityId mismatch. (2) Disable credential when entity → INACTIVE/deleted. (3) Unify one password policy across parent+teacher. (4) Mask email in login-fail log per logs-format-standard §3. (5) Add `jti` (revocation roadmap) + iss/aud. (6) Dummy BCrypt compare on email-not-found to flatten timing.

## Acceptance Criteria

- [x] setPassword rejects cross-entity mismatch (test)
- [x] Credential disabled when owning entity deactivated (test)
- [x] Single password policy enforced both endpoints
- [x] Login-fail log masks email; JWT has jti; timing flattened

## Related

- Audit reports: `documents/04-quality/audits/business-logic/2026-06-06-wave-auth-1-business-logic.md` + `../ops-readiness/2026-06-06-wave-auth-1-ops-readiness.md`
- GAP-116 (PII scrubber, deferred Wave 7); logs-format-standard.md §3

## Log

- **2026-06-06:** DONE 2026-06-06 PR #2193 — setPassword cross-entity reject + disableCredential + AuthPasswordPolicy + PII mask + jti + timing.
