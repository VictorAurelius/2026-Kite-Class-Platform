# GAP-610 — `GET /api/v1/auth/beta-signup/validate` returns TOKEN_NOT_FOUND cho valid token (RLS suspect)

**Status:** 🟡 PARTIAL (95% — H4 fix shipped + local RST walk PASS toàn bộ 4 lifecycle state trên rebuilt image; còn lại = production deploy verify (AWS account state) + FE deep-link page render qua email-link thật cần admin approve, sister flow GAP-772)
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-05-17 (Wave 90 walkthrough — FE deep-link page 404)
**Affects:** Toàn bộ beta signup deep-link flow — invitee không thể proceed dù email + token đúng

## Problem

DB row tồn tại đúng:
```
id=7, email=mvann1207@gmail.com, status=APPROVED,
invite_token=98446443-e5cc-43e9-9498-6799d460d2db,
approved_at=2026-05-17 16:36:08, invite_sent_at=2026-05-17 16:36:08
```

Token chưa expired (TTL 24h, query 5 phút sau approve).

Curl evidence:
```bash
$ curl -s "https://api.kitehub.me/api/v1/auth/beta-signup/validate?token=98446443-e5cc-43e9-9498-6799d460d2db"
{"valid":false,"email":null,"name":null,"orgName":null,"persona":null,"errorCode":"TOKEN_NOT_FOUND"}
HTTP: 404
```

`BetaAccessService.validateToken(UUID)` calls `repository.findByInviteToken(token)` → empty Optional. Return TOKEN_NOT_FOUND.

## Root cause hypothesis (need verify — AWS suspended, blocked)

### Hypothesis 1 (most likely): RLS policy blocks anonymous query

`beta_access_request` table likely has Row-Level Security policy similar pattern to `email_sent_log`:
```sql
USING (COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
    OR instance_id = current_setting('app.current_tenant_id', true)::uuid)
```

Public anonymous endpoint (no JWT, no tenant context) → no SET LOCAL `app.*` settings → RLS hides ALL rows → findByInviteToken returns empty.

### Hypothesis 2: Token UUID encoding mismatch

`invite_token` column type might be `uuid` but JPA send as `varchar` or vice versa. PostgreSQL strict on UUID format match.

### Hypothesis 3: Query method signature mismatch

`repository.findByInviteToken(UUID token)` — if JPA inferring SQL `WHERE invite_token = ?1::varchar` instead of `::uuid`, no match.

## Verify steps (resume when AWS restored)

```bash
# 1. Check table RLS policies
SSM → psql → \d beta_access_request → check Policies section
psql → SELECT polname, polcmd, pg_get_expr(polqual, polrelid) FROM pg_policy WHERE polrelid = 'beta_access_request'::regclass;

# 2. Query as admin role bypass RLS
psql → SET ROLE postgres; SELECT id, invite_token FROM beta_access_request WHERE invite_token = '98446443-e5cc-43e9-9498-6799d460d2db';

# 3. Check JPA query SQL
docker logs kitehub-subscription | grep "select.*beta_access_request" | tail -5
# Look for parameter binding type
```

## Production impact

🔴 100% beta signup deep-link flow broken. NO invitee can complete signup via email link (when email infra fixed per GAP-605/606/608).

## Proposed Fix

### If Hypothesis 1 (RLS):
- Either drop RLS on beta_access_request (public access required for unauthenticated signup), OR
- Add public-bypass policy: `USING (status IN ('PENDING', 'APPROVED') AND invite_token_expiry > NOW())` — allows public lookup of active invite tokens only
- Per `security-audit/SKILL.md` A01 — RLS must not break legitimate unauthenticated flows

### If Hypothesis 2/3 (UUID encoding):
- Add explicit `@Type(type = "pg-uuid")` annotation on entity field
- OR query method signature `findByInviteToken(@Param("token") UUID token)` with explicit `@Query("SELECT b FROM BetaAccessRequest b WHERE b.inviteToken = :token")`

## Acceptance Criteria

- [x] Root cause identified (verify hypothesis 1/2/3) — Wave onboarding-polish-2 Bucket E IT confirms H1/H2/H3 REJECTED; H4 (lifecycle-collapse) CONFIRMED
- [x] Fix applied (local) — `validateToken` split `TOKEN_NOT_FOUND` (row absent) vs `TOKEN_NOT_APPROVED` (PENDING/REJECTED/ABORTED); `BetaAccessService.java:549-559`. Production deploy verify ⏳ defer (AWS account state, GAP-612)
- [x] Curl validate with known-good token → returns `{valid:true, email, name, orgName, persona}` HTTP 200 — RST walk Step 2b PASS trên rebuilt local image
- [ ] FE `/beta-signup?token=<UUID>` page loads + pre-fills form correctly — FE error-map updated (`BetaSignupForm.tsx` thêm `TOKEN_NOT_APPROVED` message); full deep-link render qua email-link cần admin approve (sister flow GAP-772)
- [x] Integration test cover: public endpoint + RLS bypass scenario — `BetaSignupTokenReproIT` (H4 PENDING test updated → TOKEN_NOT_APPROVED) + `BetaAccessServiceTest` (4 new tests: PENDING/REJECTED/ABORTED → TOKEN_NOT_APPROVED, absent → TOKEN_NOT_FOUND)
- [x] Companion gaps GAP-611 ID instances/payments/revenue 404 if same RLS root cause — GAP-824 filed for sister `exchangeClaimCode` collapse (same domain, same anti-pattern; cross-flow sweep confirms SAME class still present, DEFER to GAP-824)

## Related

- GAP-611 (sister — `POST /api/v1/auth/beta-signup` 404 — likely same root cause class)
- `security-audit/SKILL.md` A01 — RLS scope
- BetaAccessController.java + BetaAccessRequestRepository.java
- Wave 90 walkthrough evidence — DB row confirmed via direct psql `findByInviteToken` returns empty via JPA

## Root Cause Analysis (2026-06-01)

**Investigation method:** `BetaSignupTokenReproIT` (Wave onboarding-polish-2 Bucket E) — 7 tests trên Testcontainers Postgres 16, mỗi hypothesis một test branch + happy-path baseline. Run: `./mvnw -pl kitehub-subscription test -Dtest=BetaSignupTokenReproIT -P strict-warnings` → **Tests run: 7, Failures: 0, Errors: 0, Time elapsed: 14.84s ✓**.

### Per-hypothesis verdict (empirical evidence)

| Hypothesis | Test method | Verdict | Layer |
|---|---|---|---|
| **H1** RLS hides anonymous query | `h1_rls_does_not_hide_approved_row` | ❌ **REJECTED** — APPROVED row visible to anonymous JPA call (no SecurityContext / no MDC) | Repository |
| **H2** Endpoint not registered / path mismatch | `h2_endpoint_is_registered_at_expected_path` | ❌ **REJECTED** — `RequestMappingHandlerMapping` confirms `BetaAccessController.validateToken` registered at exact path `/api/v1/auth/beta-signup/validate` | Controller |
| **H3** UUID type-binding mismatch | `h3_uuid_binding_round_trip_works` | ❌ **REJECTED** — UUID `98446443-e5cc-43e9-9498-6799d460d2db` round-trips correctly qua explicit `@Query` + Postgres `uuid` column | Repository |
| **H4 (NEW)** Lifecycle-collapse / data-state mismatch | `h4_service_layer_lifecycle_collapse_for_pending_row` + `h4_signed_up_clears_token_returns_not_found` + `h4_expired_returns_distinct_code` | ✅ **CONFIRMED** — Service layer flattens MULTIPLE distinct lifecycle states (PENDING + SIGNED_UP) into SAME `TOKEN_NOT_FOUND` response code | **Service (BetaAccessService.validateToken)** |

### Root cause — what's actually happening

**File:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/service/BetaAccessService.java` lines 540–557.

```java
public BetaTokenValidationResponse validateToken(UUID token) {
    Optional<BetaAccessRequest> opt = repository.findByInviteToken(token);
    if (opt.isEmpty()) {
        return BetaTokenValidationResponse.invalid("TOKEN_NOT_FOUND");  // case A: row truly missing
    }
    BetaAccessRequest entity = opt.get();
    if (entity.getStatus() == BetaAccessRequestStatus.SIGNED_UP) {
        return BetaTokenValidationResponse.invalid("ALREADY_USED");
    }
    if (entity.getStatus() != BetaAccessRequestStatus.APPROVED) {
        return BetaTokenValidationResponse.invalid("TOKEN_NOT_FOUND");  // case B: row exists but PENDING/REJECTED/ABORTED
    }
    if (entity.isTokenExpired()) {
        return BetaTokenValidationResponse.invalid("TOKEN_EXPIRED");
    }
    return BetaTokenValidationResponse.ok(...);
}
```

**Two semantically distinct failure modes collapse to the SAME error code** (`TOKEN_NOT_FOUND`):
1. Row truly missing (line 543) — token never issued OR row deleted
2. Row exists but `status != APPROVED && status != SIGNED_UP` (line 550) — token bound to PENDING / REJECTED / ABORTED request

Plus a third path emerges from `completeBetaSignup` (line 581-584): SIGNED_UP transition NULL-clears `invite_token`, so post-signup replays of email link see `repository.findByInviteToken(token)` empty → `TOKEN_NOT_FOUND` (case A). Operator/UI cannot distinguish "consumed" from "invalid".

### Production behavior alignment

The rst-cascade-1 cluster-3 walk-through (2026-05-26) confirmed: valid UUID input → HTTP 404 + `TOKEN_NOT_FOUND`. Empirical IT confirms that response can arise from MULTIPLE upstream states (truly-missing / PENDING / SIGNED_UP-cleared / REJECTED / ABORTED). The orig GAP-610 production token `98446443-...` could match any of those — operator could not tell from the response.

### Proposed fix scope (HIGH-LEVEL estimate — for fix wave)

**Effort:** S (~30-60 min code + ~30 min test + ~15 min FE alignment).

1. **Service-layer fix:** Split `TOKEN_NOT_FOUND` into 3 distinct codes for diagnostic clarity:
   - `TOKEN_NOT_FOUND` — row truly missing (preserves current case A)
   - `TOKEN_NOT_APPROVED` — row exists but PENDING/REJECTED/ABORTED (new code; case B)
   - `TOKEN_CONSUMED` — row exists but SIGNED_UP (new code; replaces ALREADY_USED OR reuses it post-cleanup) — note: requires the SIGNED_UP path to NOT null-clear `invite_token` (entity refactor) OR add a `consumed_at`-keyed lookup
2. **Optional richer service trace:** Add internal logging với row.id + row.status when returning the failure code so operator can correlate quickly.
3. **FE alignment:** `BetaSignupForm.tsx` `BetaTokenStatus.errorCode` enum extended với new codes; UI messages updated per `vn-localization-audit-checklist.md` §2 Vietnamese label rule.
4. **Same fix in sister method `exchangeClaimCode`** — see GAP-824 (cluster gap filed này wave).

### Cluster finding — sister bug

Same collapse pattern exists trong `BetaAccessService.exchangeClaimCode` (lines 367 + 374): both "code not found" AND "code in non-APPROVED state" return `CODE_NOT_FOUND`. Identical anti-pattern, same domain. → **GAP-824 filed** (Wave onboarding-polish-2 Bucket E cluster spin-off).

## Log

- **2026-06-02 (autonomous gap-fix — H4 lifecycle-collapse fix + local RST walk):** Fix shipped TDD-first per H4 root cause. `BetaAccessService.validateToken` (lines 540-565) split collapsed `TOKEN_NOT_FOUND` thành 3 distinct codes: `TOKEN_NOT_FOUND` (row truly absent — preserved case A), `TOKEN_NOT_APPROVED` (NEW — row exists PENDING/REJECTED/ABORTED), `ALREADY_USED` (SIGNED_UP), `TOKEN_EXPIRED` (APPROVED+expired). Added internal `log.info` với row.id+status khi return TOKEN_NOT_APPROVED cho operator correlation.

  **TDD:** 4 new tests trong `BetaAccessServiceTest` (PENDING/REJECTED/ABORTED → TOKEN_NOT_APPROVED, absent → TOKEN_NOT_FOUND) — RED 3/4 fail pre-fix → GREEN sau fix. Existing `BetaSignupTokenReproIT` H4 PENDING test updated TOKEN_NOT_FOUND → TOKEN_NOT_APPROVED. Full module `./mvnw -pl kitehub-subscription verify -P strict-warnings` → BUILD SUCCESS, 765 tests 0 fail.

  **State-check (live, pre-fix):** confirmed H1 REJECTED empirically (no RLS policy on `beta_access_request` qua `pg_policy` query). PENDING token via gateway/direct → `TOKEN_NOT_FOUND` HTTP 404 reproduced. Happy-path (APPROVED) → valid:true HTTP 200 (endpoint not wholly broken).

  **RST walk (rebuilt local image, 4 lifecycle states):** kitehub-subscription:latest rebuilt from worktree code + container restarted. (a) PENDING row + token → `TOKEN_NOT_APPROVED` HTTP 404 ✅ (was TOKEN_NOT_FOUND pre-fix); (b) APPROVED + non-expired → `valid:true` HTTP 200 + pre-fill (VN diacritic `Trần Thị Hồng` preserved) ✅; (c) SIGNED_UP token-cleared → `TOKEN_NOT_FOUND` HTTP 404 ✅ (preserved); (d) random absent → `TOKEN_NOT_FOUND` HTTP 404 ✅. Step 1 POST request-beta-access → HTTP 201 + DB row id=9 PENDING.

  **Cross-flow sweep (per cross-flow-bug-class-sweep.md):** bug class = "row-found-but-wrong-state collapsed into not-found code". `exchangeClaimCode` (BetaAccessService:365-374) → SAME class present (CODE_NOT_FOUND for empty + non-APPROVED) → **DEFER to GAP-824** (already filed, sister cluster). `StaffInvitationService.acceptInvitation` → EXEMPT (already distinguishes INVITATION_NOT_FOUND vs INVITATION_ALREADY_USED vs INVITATION_EXPIRED).

  **3-layer doc sync:** `api-contract.md` GET validate updated với 4-code errorCode table. FE `BetaSignupForm.tsx` error-map thêm Vietnamese message cho TOKEN_NOT_APPROVED. DTO javadoc updated.

  completion_pct 85 → 95. Remaining 5%: production deploy verify (AWS account state — GAP-612 blocker) + FE deep-link page render qua email-link thật (cần admin approve, sister flow GAP-772). Status stays PARTIAL per `gap-done-discipline.md` §2/§3 — không false-DONE vì 2 AC item chưa verify được local.

- **2026-06-01 (Wave onboarding-polish-2 Bucket E — root cause investigation):** Testcontainers IT `BetaSignupTokenReproIT` shipped với 7 tests covering H1/H2/H3/H4 layer-by-layer. Empirical evidence confirms H1/H2/H3 REJECTED (consistent với Wave 91 Bucket D static-analysis verdict) + **H4 CONFIRMED**: `BetaAccessService.validateToken` collapses MULTIPLE distinct lifecycle states into same `TOKEN_NOT_FOUND` response code (lines 540-551). Production rst-cascade-1 walk (2026-05-26) HTTP 404 + TOKEN_NOT_FOUND aligns với this — token's row may be PENDING/REJECTED/ABORTED/post-SIGNED_UP-cleared NOT truly missing. Fix scope S effort, queued cho fix wave. Sister cluster GAP-824 filed for `exchangeClaimCode` same anti-pattern. completion_pct 75 → 85. AC items 1/4/6 ticked (root cause + IT cover + companion gap).

- **2026-05-17:** Gap filed during Wave 90 walkthrough. FE deep-link returns "Token không hợp lệ hoặc đã hết hạn" mặc dù token tồn tại đúng. AWS account suspended mid-investigation; Hypothesis 1 (RLS) priority because identical pattern observed in `email_sent_log` policy (verified Wave 90 \d query).
- **2026-05-18 (Wave 91 Bucket D deep investigation):** Status PARTIAL. PR #1490 đã ship defensive hardening (explicit `@Query` + Testcontainers IT). Wait-time deep investigation (`documents/04-quality/audits/aws-verification/2026-05-18-wave-91-bucket-d-deep-investigation.md`) **REJECTED all 3 original hypotheses** với code evidence: (1) V34 RLS migration explicit table list KHÔNG include `beta_access_request` — anonymous query full visibility; (2) entity declares `UUID inviteToken` + Hibernate 6 native UUID binding — no encoding mismatch; (3) Spring Data method-derived query bound correctly. Surfaced 2 NEW hypothesis Bucket D missed: **#4 data state mismatch (operator-typed UUID không match DB OR row consumed by retry) — ~70% likely**, #5 image promotion drift — ~10%. Cross-gap analysis với GAP-611: same `findByInviteToken` empty-Optional root cause explains both gaps. Coordinator F debug sequence post-AWS-restore: 5 steps (~15 min). Gap stays PARTIAL until live verify confirms root cause.
- **2026-05-25 (Wave beta-readiness-5 Bucket C — investigation + IT unblock):** Investigation phase per `release-fix-retry-budget.md` §3.5 confirmed Wave 91 Bucket D findings (3 original hypotheses REJECTED via empirical state-check: `V34__enable_rls_tenant_scoped_tables.sql` explicit `instance_id_tables` array does NOT include `beta_access_request`; `BetaAccessRequest.inviteToken` declared `private UUID` + `V28__create_beta_access_request.sql` column type `uuid`; `BetaAccessRequestRepository.findByInviteToken` uses explicit JPQL `@Query` with `:token` named param). **Test infra unblock:** `BetaAccessRequestRepositoryPostgresIT` previously failed ApplicationContext loading because `EmailServiceClient` requires `RabbitTemplate` non-optional constructor param while `application-test.yml` excludes `RabbitAutoConfiguration`. Added `@MockBean RabbitTemplate` to IT class → 4/4 GAP-610 tests PASS on real Postgres 16 via Testcontainers: (a) `findByInviteToken_returnsApprovedRow` ✅ APPROVED row visible via anonymous JPA query, (b) `findByInviteToken_returnsEmptyForUnknown` ✅ no false positive, (c) `findByInviteToken_findsRegardlessOfStatus` ✅ PENDING row retrievable for lifecycle filter, (d) `countStalePending_returnsAccurateCount` ✅. Note: 4 GAP-600 cleanup tests in same class still fail with `InvalidDataAccessApiUsage Executing an update/delete query` (separate pre-existing main HEAD issue — needs `@Transactional` on `@Modifying` query call sites). GAP-610 stays PARTIAL 75% until live verify post-GAP-612 AWS restore confirms Hypothesis #4 (data state mismatch) on production data.
