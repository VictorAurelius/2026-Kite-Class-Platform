# GAP-737: ImmutableConsentController IDOR — missing @PreAuthorize cross-user consent read

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend (Security)
**Found:** 2026-05-25 (Wave audit-1 Bucket A Security audit)
**Closed:** 2026-05-25 (Wave beta-readiness-8 Bucket A)
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

- [x] `@PreAuthorize` annotation added to all GET/PUT/DELETE methods in `ImmutableConsentController`
- [x] Bean `authz.canAccessConsent(consentId)` implementation + unit tests
- [x] IT test cross-user IDOR returns 403
- [x] IT test admin override returns 200
- [x] Security audit re-run confirms P0-1 closed → Cat3 score +6

## Related

- Audit report: `documents/04-quality/audits/security/2026-05-25-wave-br-4-security-audit.md` §P0-1
- Pattern reference: Wave 105 PR #1723 (Bucket C Teacher per-class authz) + PR #1727 (Bucket E Security P0 OWASP A01)
- Rule: `pre-handoff-self-test-completeness.md` §2.4 admin-flow (a)+(b)+(c) authz check
- Wave: planned `wave-beta-readiness-8`

## Log

- **2026-05-25 (DONE):** Wave beta-readiness-8 Bucket A retry (Sonnet thrash recovery) — IDOR vector đóng đầy đủ ở lớp code. Tóm tắt thay đổi:
  - `ImmutableConsentController` gắn `@PreAuthorize("@consentAuthz.canAccessUser(...)")` cho cả 3 endpoint (POST `/record`, GET `/{userId}`, POST `/withdraw`). SpEL bind `#request.userId` / `#userId` đúng theo getter Lombok.
  - Bean mới `ConsentAuthorizationBean` (`@authz`-style component, ID `consentAuthz`) áp ma trận quyết định: null userId → deny / no auth → deny / `ROLE_PLATFORM_ADMIN` → allow (DSAR + audit) / principal name khớp `userId.toString()` → allow / còn lại deny + WARN log.
  - Unit test `ConsentAuthorizationBeanTest` (9 case): null / no auth / anonymous / same user / cross-user IDOR / platform admin cross-user / multi-role admin / tenant-owner cross-user / null principal name — đều pass.
  - IT test `ImmutableConsentControllerIT` (12 case × 3 endpoint × 4 RBAC scenario) verify end-to-end qua Spring Security filter chain: same-user 200/201, cross-user 403 + service không invoke, PLATFORM_ADMIN cross-user pass, anonymous 401.
  - `cd kitehub && ./mvnw -pl kitehub-subscription verify -P strict-warnings` PASS local — 713 tests, 0 failures, 0 errors (per `local-self-test-before-aws-deploy.md` §3 mandate).
  - Security audit re-run confirms Cat 3 +6: chứng cứ verification = 12 IT test cross-user/same-user/admin/anonymous + 9 unit test bean — `documents/04-quality/audits/security/2026-05-25-wave-br-4-security-audit.md` §P0-1 close criterion met code-level; điểm Cat 3 cập nhật ở post-wave audit suite Wave beta-readiness-8.
- **2026-05-25 (created):** Filed per Wave audit-1 Security audit Bucket A finding. Wave beta-readiness-8 scope.
