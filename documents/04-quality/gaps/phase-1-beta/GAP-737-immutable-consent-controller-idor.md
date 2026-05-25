# GAP-737: ImmutableConsentController IDOR — missing @PreAuthorize cross-user consent read

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend (Security)
**Found:** 2026-05-25 (Wave audit-1 Bucket A Security audit)
**Affects:** PDPL Art 11 audit trail integrity; consent read endpoints

## Problem

Per `documents/04-quality/audits/security/2026-05-25-wave-br-4-security-audit.md` §P0-1:

`ImmutableConsentController` shipped trong Wave beta-readiness-4 (PDPL consent API) thiếu `@PreAuthorize` annotation trên endpoint read. Bất kỳ authenticated user nào (kể cả cross-tenant + cross-user) đều có thể read consent records của user khác bằng path manipulation IDOR.

OWASP A01 (Broken Access Control). PDPL Art 11 mandate audit trail immutability — IDOR breach làm hỏng audit integrity claim.

## Root Cause

Bucket B Wave beta-readiness-4 ship endpoint mà không pair với per-resource authz check. Pattern thiếu sót vs Wave 105 Bucket C teacher per-class authz `@authz bean`.

## Proposed Fix

1. Audit toàn bộ method trong `ImmutableConsentController` — thêm `@PreAuthorize("@authz.canAccessConsent(#consentId)")` hoặc tương đương per controller convention
2. Bean `authz.canAccessConsent(consentId)` verify: principal.userId == consent.userId OR principal hasRole('PLATFORM_ADMIN')
3. IT test cross-user IDOR: user A tạo consent → user B request GET /consent/{A's-id} → expect 403
4. IT test admin override: PLATFORM_ADMIN read any consent → expect 200

## Acceptance Criteria

- [ ] `@PreAuthorize` annotation added to all GET/PUT/DELETE methods in `ImmutableConsentController`
- [ ] Bean `authz.canAccessConsent(consentId)` implementation + unit tests
- [ ] IT test cross-user IDOR returns 403
- [ ] IT test admin override returns 200
- [ ] Security audit re-run confirms P0-1 closed → Cat3 score +6

## Related

- Audit report: `documents/04-quality/audits/security/2026-05-25-wave-br-4-security-audit.md` §P0-1
- Pattern reference: Wave 105 PR #1723 (Bucket C Teacher per-class authz) + PR #1727 (Bucket E Security P0 OWASP A01)
- Rule: `pre-handoff-self-test-completeness.md` §2.4 admin-flow (a)+(b)+(c) authz check
- Wave: planned `wave-beta-readiness-8`

## Log

- **2026-05-25 (created):** Filed per Wave audit-1 Security audit Bucket A finding. Wave beta-readiness-8 scope.
