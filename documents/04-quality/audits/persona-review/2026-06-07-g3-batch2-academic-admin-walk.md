# G3 production-parity walk — batch 2 (academic cluster + admin + staff + payroll)

**Ngày:** 2026-06-07
**Loại:** G3 production-parity runtime walk — cross-tenant isolation + happy-path via gateway :9000 (Postgres+Flyway+RLS, minted HS512 JWT).
**Trigger:** P3 G3 batch 2 clean re-walks per `g3-production-parity-plan.md` P3. These flows had no OPEN P1 blockers (unlike KC-11); G3 verifies G1-passed flows hold cross-tenant isolation at production-parity.

## Stack-up

- All backend services fresh from main (gateway/subscription/kiteclass-core/branding rebuilt this session).
- Fixtures (kiteclass_shared): Tenant A `aaaabbbb-…0001` (skytest; 1 course, 2 classes), Tenant B `0edaee10-…` (sky-education; 6 courses, 6 classes, 78 students, 13 grades, 6 attendance). Cross-tenant probe = ownerA/adminA token (tenantId=A) → tenant-B resource by id → expect 403/404.
- Pre-walk drift fix: course id=1 (tenant A) had polluted fixture status `active` (invalid — `CourseStatus` enum = DRAFT/PUBLISHED/ARCHIVED) → caused 500 on list/get. Fixed row `active`→`PUBLISHED` (test-data pollution, not a code defect; 9 legit DRAFT rows unaffected).

## Walk evidence (gateway :9000)

| Flow | Probe | HTTP | Verdict |
|---|---|---|---|
| **KC-3** academic course/class | ownerA → course B id=2 / class B id=8 | 404 / 404 | ✅ isolation |
| KC-3 | ownerA → own course id=1 (post fixture-fix) | 200 | ✅ happy |
| KC-3 | ownerB → tenantA course id=1 | 404 | ✅ isolation (reverse) |
| KC-3 list scoping | ownerA list `totalElements` / ownerB list | 1 / 6 | ✅ tenant-scoped, no leak |
| **KC-4** student/enrollment | ownerA → student B id=4 | 404 | ✅ isolation |
| **KC-5** attendance | ownerA → attendance B id=6 / ownerB → own | 404 / 200 | ✅ isolation + happy |
| **KC-6** grade | adminB → own grade id=1 / adminA → grade B id=1 | 200 / 404 | ✅ custom `@authz.hasAccessToGrade` + tenant isolation (even ADMIN can't cross) |
| **KH-9** admin console | ownerA → /api/v1/admin/beta-requests / PLATFORM_ADMIN → same | 403 / 200 | ✅ PLATFORM_ADMIN gate (inverse authz) |
| **KC-2** staff invitation | ownerA → /api/v1/staff-invitations | 200 | ✅ tenant-scoped reachable (STAFF-tenant fix GAP-981 from G1) |
| **KC-12** payroll routing (GAP-1041 fix) | adminA → /api/v1/admin/payroll/configs | 200 | ✅ routes to kiteclass-core (not admin 404); /summary → kiteclass-core RESOURCE_NOT_FOUND shape confirms routing |

## Flows acknowledged G3 (verified elsewhere / already passed)

- **KH-3** subscription: security (cross-tenant IDOR) verified via KH-5 batch 1 (GAP-1015 → 403). Functional trial→paid is non-security; G1-passed.
- **KH-2a** admin auth: PLATFORM_ADMIN minted token reaches `/api/v1/admin/**` → 200 (auth chain works via gateway).
- **KC-1** provisioning: saga verified LIVE production-parity walk#3 2026-06-07 (handoff) — signup→saga→DEPLOYED→audit→email.
- **KC-7 / KC-8**: G3 PASS earlier (2026-06-05/06).

## Verdict

**Batch 2 academic cluster (KC-3/4/5/6) + KH-9 + KC-2 + KC-12 G3 PASS** — cross-tenant isolation holds via RLS + app guard + custom authz through the real gateway JWT→header chain. Combined with batch 1 (6 security flows) + KC-11 (P1 fixes) + carve-outs, the campaign's G3 production-parity gate is satisfied for the verified flows. Each advances `🔄 walk-pass-pending-human` with **G3 ✅** — chờ G2 human.

KC-9 stays DEFERRED Phase 2. Residual non-blocking: KC-12 GAP-1043 (P2 past-date), GAP-721 (Zalo stub).

Deploy note: all fixes on main; production rebuild service images from main (ECR) when deploying AWS.
