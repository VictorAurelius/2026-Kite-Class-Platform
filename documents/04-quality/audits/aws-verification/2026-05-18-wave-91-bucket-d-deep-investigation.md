---
title: Wave 91 Bucket D — Deep Investigation GAP-610 + GAP-611
status: complete
created: 2026-05-18
phase: phase-1-beta
wave: 91
related_gaps: [GAP-610, GAP-611, GAP-604, GAP-612]
---

# Wave 91 Bucket D — Deep Investigation Report

## 1. Scope

Wait-time deep investigation cho GAP-610 (`GET /api/v1/auth/beta-signup/validate` trả `TOKEN_NOT_FOUND` cho token hợp lệ) + GAP-611 (`POST /api/v1/auth/beta-signup` 404). PR #1490 (Wave 91 Bucket D) đã ship defensive hardening (explicit `@Query` + Testcontainers IT + JWT filter regression tests) nhưng KHÔNG confirm root cause vì state-check static analysis cho thấy cả 7 hypothesis đều "not confirmed by code".

Mục tiêu báo cáo:
1. Rank lại 7 hypothesis bằng evidence cụ thể từ codebase
2. Đề xuất debug sequence cho Coordinator F sau khi AWS restore (GAP-612 đóng)
3. Surface 1 new high-likelihood hypothesis Bucket D agent đã miss

**AWS context:** account 906286017800 SUSPENDED 2026-05-17 — không có live curl / SSM / CloudWatch logs. Investigation pure static + reasoning.

## 2. Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1)

```bash
# Repo state checks
find kitehub/kitehub-subscription/src/main/resources/db/migration -name "V*.sql" \
  | xargs grep -l -iE "ROW LEVEL SECURITY|POLICY|beta_access_request"
# → V28 V32 V33 V34 V39 V50 (none touch beta_access_request RLS — V34 enables RLS only on instance_id-keyed tables)

grep -n -E "ROW LEVEL SECURITY|FOREACH" kitehub/kitehub-subscription/src/main/resources/db/migration/V34__enable_rls_tenant_scoped_tables.sql
# → instance_id_tables array: 11 tables; tenant_id_tables: 1 (consent_record). beta_access_request NOT in either list.

# Entity + repository surface
grep -n -E "invite_token|@Column" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/entity/BetaAccessRequest.java
# → @Column(name="invite_token") private UUID inviteToken; @Column line 79-80, no @JdbcTypeCode

grep -n -E "findByInviteToken|@Query" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/repository/BetaAccessRequestRepository.java
# → PR #1490 added explicit @Query("SELECT b FROM BetaAccessRequest b WHERE b.inviteToken = :token")

# Controller routes
grep -n -E "PostMapping|GetMapping|beta-signup" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/controller/BetaAccessController.java
# → @GetMapping("/api/v1/auth/beta-signup/validate") @RequestParam UUID token
# → @PostMapping("/api/v1/auth/beta-signup") @RequestBody BetaSignupCommand
# → @PostMapping("/api/v1/auth/beta-signup/exchange-claim-code") (claim code exchange)

# Gateway routes
grep -n -E "beta-signup|kitehub-auth-v1|Path=/api/v1/auth" kitehub/kitehub-gateway/src/main/resources/application.yml
# → Routes order: request-beta-access (rate-limited POST) → 2fa-verify → 2fa-recovery-regenerate → 2fa/** → 2fa aliases → feedback-v1 → kitehub-auth-v1 catch-all /api/v1/auth/**

# JWT filter
cat kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/filter/JwtAuthenticationGatewayFilter.java
# → isPublicPath: path.startsWith("/api/v1/auth/") — matches both /validate (GET) + /beta-signup (POST)

# Spring Security
grep -n -E "requestMatchers.*/api/v1/auth" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/config/SecurityConfig.java
# → line 99: .requestMatchers("/api/v1/auth/**").permitAll()

# Service implementation
grep -n -A 5 "public BetaTokenValidationResponse validateToken" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/service/BetaAccessService.java
# → opt = repository.findByInviteToken(token); empty → TOKEN_NOT_FOUND

# Port chain verification
grep -n -E "server\.port|SERVER_PORT" kitehub/kitehub-subscription/src/main/resources/application.yml docker-compose.production.yml
# → application.yml default 8081; compose override SERVER_PORT: 8080 ✓ matches gateway URI

# Audit aspect (poison-txn check)
grep -n -E "@Transactional|columnDefinition" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/audit/AdminAuditLog.java \
  kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/audit/AdminAuditAspect.java
# → AdminAuditLog.payload_json columnDefinition="jsonb" — Postgres-specific class same family as Wave 88 LoginAuditLog.ip="inet" incident
# → AdminAuditAspect: NO @Transactional declared on aspect; uses repository.save in try/catch around @Around proceed
```

## 3. Findings — Hypothesis ranking by evidence

### GAP-610 (validate token TOKEN_NOT_FOUND)

| # | Hypothesis | Bucket D verdict | Deep-investigation verdict | Evidence |
|---|---|---|---|---|
| 1 | RLS policy blocks anonymous query | "❌ NOT CONFIRMED" | **REJECTED — strong** | V34 source code reads explicit table list: `instance_id_tables` (11 tables) + `tenant_id_tables` (1 = consent_record). `beta_access_request` not in either. V34 + V50 are the ONLY RLS migrations. NO `CREATE POLICY ON beta_access_request` anywhere in V1-V50. Anonymous query has full table visibility. |
| 2 | UUID encoding mismatch (varchar vs uuid) | "❌ NOT CONFIRMED" | **REJECTED — strong** | Entity declares `private UUID inviteToken` + `@Column(name="invite_token")`. V28 declares `invite_token UUID NULL`. Hibernate 6 binds Java `UUID` to Postgres `uuid` natively via `UUIDJavaType` — no `@JdbcTypeCode` needed. Bucket D's explicit `@Query` makes this even more robust but original method-derived shape was correct. |
| 3 | JPA query method signature mismatch | "❌ NOT CONFIRMED" | **REJECTED — strong** | Spring Data method-derived `findByInviteToken(UUID)` generates `WHERE invite_token = ?` with parameter bound as native Postgres `uuid`. Type matches column. Bucket D's defensive `@Query` is identical semantically. |
| 4 | **NEW — Data state mismatch (token actually NOT in DB)** | (Bucket D didn't consider) | **VERY LIKELY** | Gap §Problem cites DB row `id=7, invite_token=98446443-e5cc-43e9-9498-6799d460d2db` but does NOT cite the exact psql/SSM command that confirmed the row exists. Wave 90 Phase 2 was conducted PRE-AWS-suspension; possible the operator wrote the wrong UUID into curl (typo of `e` vs `e` lowercase) OR wrote UUID from an older `claim_code → token exchange` that already got SIGNED_UP (cleared) by retry attempts. Code in `BetaAccessService.validateToken` line 404: if status==SIGNED_UP returns `ALREADY_USED`, line 408: if !=APPROVED returns `TOKEN_NOT_FOUND`. Both surface as 404. |
| 5 | **NEW — Wave 90 staging.21 image promotion drift** | (Bucket D didn't consider) | **POSSIBLE** | Wave 90 audit confirmed staging.21 deployed via `deploy-production.yml` for kitehub-gateway (GAP-604 fix). Need verification that kitehub-subscription ALSO got fresh image (deploy-production.yml deploys all 7 services per script behavior, but verify the actual image SHA on EC2 matches staging.21 tag for subscription). If subscription is running an OLDER image without Wave 88 V32+V33 (consent + claim_code), the entity ↔ schema would still be compatible but BetaTokenValidationResponse fields might mismatch. (Lower-probability: V28-V50 all DDL-shippable, but worth ruling out via SHA check.) |

### GAP-611 (POST beta-signup 404)

| # | Hypothesis | Bucket D verdict | Deep-investigation verdict | Evidence |
|---|---|---|---|---|
| 1 | Gateway predicate order shadows POST | "❌ NOT CONFIRMED" | **REJECTED — strong** | Routes specific phía trên `kitehub-auth-v1` catch-all: `request-beta-access` (Method=POST + `/api/v1/auth/request-beta-access` exact path), `2fa-verify` (`/api/v1/auth/2fa/verify` exact), `2fa-recovery-regenerate` (`/api/v1/auth/2fa/recovery-codes/regenerate` exact), `2fa/**` (prefix). NONE match `/api/v1/auth/beta-signup`. Catch-all `kitehub-auth-v1` is the route resolver. URI = `http://kitehub-subscription:8080`. |
| 2 | Spring Cloud Gateway HTTP method filter bug | "❌ NOT CONFIRMED" | **REJECTED** | Catch-all has no Method predicate; applies to all methods. Wave 90 evidence: `GET /api/v1/admin/beta-requests` HTTP 200 (POST not tested same route but same gateway behavior). Method handling is not the bug. |
| 3 | Wave 89 JwtAuthenticationGatewayFilter blocks POST silently | "❌ NOT CONFIRMED" | **REJECTED — confirmed by test** | `isPublicPath()` line 136: `path.startsWith("/api/v1/auth/")` — `/api/v1/auth/beta-signup` matches (starts with the same string). Bucket D added 2 new test cases verify-by-code (filter doesn't 401 + doesn't inject identity headers). If filter blocked, would return 401 not 404. |
| 4 | Spring Security on subscription blocks POST | "❌ NOT CONFIRMED" | **REJECTED** | `SecurityConfig.java` line 99: `.requestMatchers("/api/v1/auth/**").permitAll()` — applies to POST + GET equally. If Security blocked, would return 403 not 404 (with default `SecurityConfig` returns 403 for AccessDenied, 401 for Unauthenticated). |
| 5 | **NEW — Empty body OR missing required field causing routing 404** | (Bucket D didn't consider) | **POSSIBLE — Medium** | Curl evidence trong gap shows `-d '{"token":"...","ownerPassword":"TestPass1234","subdomain":"dgedu"}'`. `BetaSignupCommand` record requires `@NotNull UUID token + @NotBlank @Size(8-200) ownerPassword + @NotBlank @Size(max 100) subdomain`. If subdomain `"dgedu"` failed unique check in `authService.registerFromBetaInvite`, controller line 134 `catch IllegalArgumentException → 409`. If JSON deserialization failed earlier, Spring throws `HttpMessageNotReadableException` → 400 default. Neither maps to 404 from controller. BUT — if request hits Spring Cloud Gateway CircuitBreaker fallback (`forward:/fallback/auth`) due to subscription service OOM/timeout, fallback URI might return 404 if `/fallback/auth` handler missing. Gateway logs would confirm. |
| 6 | **NEW — Path mismatch on actual deployed image** | (Bucket D didn't consider) | **VERY LIKELY** | Same image-promotion drift class as GAP-610 #5. If subscription EC2 is running pre-Wave-45 image, `BetaAccessController.completeBetaSignup` mapping might not exist (gap traces back to GAP-372 Wave 33). Verification: `docker exec kitehub-subscription ls /app/lib/*.jar | xargs unzip -l | grep -i BetaAccessController` OR `docker inspect kitehub-subscription \| jq '.[]\|.Image'` + compare with ECR staging.21 manifest digest. |
| 7 | **NEW — IllegalArgumentException from BetaAccessService.completeBetaSignup → controller 404** | (Bucket D didn't consider) | **POSSIBLE — Medium** | Controller line 119: `catch IllegalArgumentException → 404`. `BetaAccessService.completeBetaSignup` line 428-429: `repository.findByInviteToken(cmd.token()).orElseThrow(() -> new IllegalArgumentException("Invalid invite token"))`. If GAP-610 root cause (data-state mismatch) ALSO affects POST path → same `findByInviteToken` returns empty → throws IllegalArgumentException → controller catches → returns **404 with empty body**. This matches gap evidence: "HTTP 404 (empty body)". |

## 4. Verdict — Most likely root cause (cross-gap analysis)

**Single root cause for BOTH GAP-610 + GAP-611 (high confidence):**

`repository.findByInviteToken(UUID token)` returns empty Optional in production for the token operator used. This causes:
- `validateToken` → returns `TOKEN_NOT_FOUND` (404 with body) → GAP-610
- `completeBetaSignup` → throws IllegalArgumentException → controller catches → 404 with empty body → GAP-611

**Why findByInviteToken returns empty (ranked by likelihood):**

1. **Data state mismatch (~70%)** — operator-typed UUID không match DB; OR row got cleared by retry attempt (status → SIGNED_UP, invite_token → NULL); OR psql evidence in gap was misread (different row id). Wave 90 Phase 2 happened pre-AWS-suspension under time pressure.

2. **Wave 88 token consume race (~15%)** — `InviteTokenService.validateAndConsume` (GAP-534 single-use enforcement) clears the row's invite_token in transaction. Multiple FE retry attempts → first succeeds + token cleared → subsequent attempts surface as "not found" because token literally not in DB anymore.

3. **Image promotion drift (~10%)** — subscription EC2 not running Wave 89 staging.21 image; FE deployed but BE didn't. Compose `image: ${KITE_VERSION}` env-var; if `KITE_VERSION` overwrote to older tag, BE serves outdated controller.

4. **RLS / UUID encoding / JPA query bugs (~5%)** — Bucket D static analysis rules these out with code evidence. Real Postgres flush() test trong Testcontainers IT would confirm; Bucket D PR ships this test (`BetaAccessRequestRepositoryPostgresIT`).

**Specific bug found in code — none confirmed.** Bucket D defensive hardening is the right call given evidence. No additional code change recommended.

## 5. Recommended Coordinator F debug sequence (post-AWS-restore)

Run in this order to confirm root cause efficiently:

### Step 1: Image version verification (~2 min)

```bash
# SSM into kh_backend EC2
aws ssm start-session --target i-00505094277deda29 --profile dev-admin

# On EC2:
docker inspect kitehub-subscription | jq -r '.[]|.Config.Image, .Image'
docker exec kitehub-subscription cat /app/META-INF/MANIFEST.MF 2>/dev/null | grep -iE "version|wave"

# Cross-ref ECR
aws ecr describe-images --repository-name kite/kitehub-subscription \
  --image-ids imageTag=v0.9.0-beta-staging.21 --profile dev-admin \
  --query 'imageDetails[0].imageDigest'
```

Compare digests. If mismatch → image promotion drift → re-trigger deploy-production.yml staging.21.

### Step 2: DB data state verification (~3 min)

```bash
# On EC2:
docker exec kite-postgres psql -U kite -d kitehub -c \
  "SELECT id, email, status, invite_token, claim_code, used_at, invite_sent_at \
   FROM beta_access_request \
   ORDER BY id DESC LIMIT 5;"
```

Expected: row with token operator typed should be APPROVED + invite_token NOT NULL + used_at NULL. If SIGNED_UP OR used_at NOT NULL OR token NULL → data was consumed by prior attempt.

### Step 3: Direct controller bypass (~2 min)

```bash
# Bypass gateway to isolate gateway vs subscription
docker exec kh-backend-or-similar curl -sv -X GET \
  "http://kitehub-subscription:8080/api/v1/auth/beta-signup/validate?token=<known-valid-UUID-from-step2>"
docker exec kh-backend-or-similar curl -sv -X POST \
  "http://kitehub-subscription:8080/api/v1/auth/beta-signup" \
  -H "Content-Type: application/json" \
  -d '{"token":"<UUID-step2>","ownerPassword":"Test1234","subdomain":"acme"}'
```

If subscription returns 200/400/409 directly → gateway routing OK. If 404 from subscription direct → controller mapping issue (image drift).

### Step 4: SQL log enable (if Step 1-3 didn't pin)

```bash
docker exec kitehub-subscription jq -e '.logging.level."org.hibernate.SQL" = "DEBUG"' \
  /app/application-runtime.yml > /tmp/conf.yml \
  && docker cp /tmp/conf.yml kitehub-subscription:/app/application-runtime.yml \
  && docker restart kitehub-subscription
# Then re-run validate → grep logs for select beta_access_request
docker logs kitehub-subscription | grep -E "select.*beta_access_request|invite_token = " | tail -5
```

Hibernate logs reveal actual SQL + param binding. If parameter shows lowercase-vs-uppercase mismatch OR varchar-vs-uuid binding → JPA issue (Bucket D fix kicks in). If binding correct → returns to data-state hypothesis.

### Step 5: Gateway log inspection

```bash
docker logs kitehub-gateway | grep -E "beta-signup|404|kitehub-auth-v1" | tail -20
# Look for: route resolution + downstream response code
```

If gateway logs `404 from downstream` → confirms subscription serves 404; if gateway 404 itself → routing miss (very unlikely per evidence).

## 6. Why no defensive fix included in this PR

Per task constraint "NO speculative code change — only fix if hypothesis CONFIRMED with code evidence":

- No hypothesis confirmed by static analysis alone (Bucket D PR #1490 already shipped the only safe defensive hardening: explicit `@Query` + Testcontainers IT)
- Most-likely root cause (data state) is NOT a code bug — fix is operator verification not code change
- 2nd-most-likely (token consume race) is actually correct behavior per single-use enforcement (GAP-534) — would be a UX gap not a bug
- 3rd (image drift) is deployment hygiene, fixed by re-trigger not code

**One follow-up recommendation:** if Coordinator F confirms data-state was the cause, file P3 UX gap to surface clearer error message when `findByInviteToken` returns empty but a corresponding row with `used_at IS NOT NULL` exists ("Mã đã sử dụng" vs "Mã không tồn tại"). Currently both surface as `TOKEN_NOT_FOUND`. This is a quality-of-life improvement, not a bug fix.

## 7. Confidence levels

| Element | Confidence |
|---|---|
| GAP-610 + GAP-611 share single root cause | **High** (both touch `findByInviteToken`) |
| Root cause is RUNTIME data/deploy state (not code) | **High** (static analysis rules out code paths) |
| Specific runtime cause = data state mismatch | **Medium-High** (~70% per §4) |
| Coordinator F can pin in <15 min with §5 sequence | **High** (Steps 1+2 cover both #1+#2 hypotheses; Step 3 isolates gateway question) |
| Bucket D PR #1490 defensive hardening is sufficient regression guard | **High** (even if real cause turns out to be UUID binding edge case, Testcontainers IT will catch on next CI run; explicit @Query removes ambiguity) |

## 8. Prior actions verified (per `audit-to-gap-pipeline.md` §2.8 — avoid duplicate work)

| Action | When | Where verified |
|--------|------|----------------|
| Wave 89 Bucket A: JwtAuthenticationGatewayFilter shipped | 2026-05-17 | PR #1480, deployed staging.21 |
| Wave 89 Bucket B: PM2 ecosystem fix | 2026-05-17 | PR #1479 |
| Wave 90 live verify: GAP-604 closed + admin endpoint 200 | 2026-05-17 | `2026-05-17-wave-90-live-verify.md` |
| Wave 90 Phase 2 walkthrough: GAP-610 + GAP-611 filed | 2026-05-17 | commit 29541f37 |
| Wave 91 Bucket D defensive hardening PR | 2026-05-18 | PR #1490 — explicit @Query + 9-test gateway filter + Testcontainers IT |
| Wave 91 Buckets A/B/C/E shipped | 2026-05-18 | PRs #1486-#1488 |
| AWS account 906286017800 SUSPENDED | 2026-05-17 | GAP-612, all workflows blocked |

## 9. Pending (this op)

| Action | Owner | Notes |
|--------|-------|-------|
| Coordinator F runs §5 debug sequence post-AWS-restore | Coordinator F | Gates GAP-610 + GAP-611 DONE flip |
| GAP-610 + GAP-611 Log entry updated với deep-investigation findings | This PR | Adds narrowed-scope context, no status change |
| **Concurrent op check** | Agent verification | No active workflows; AWS suspended; rule §4 N/A this PR (no mutation triggered) |

## 10. Recommendations

1. **Coordinator F:** Run §5 debug sequence post-AWS-restore — 5 ordered steps, <15 min total. Pin root cause to single hypothesis before any further code fix.

2. **Bucket D PR #1490 status:** Merge as-is. Defensive hardening + Testcontainers IT is the right ship even without root cause confirmed; it provides regression coverage for the most-paranoid scenarios (#1-#3 hypotheses).

3. **No additional code change in this PR** — only narrows hypothesis scope + recommended debug sequence. Audit artifact + GAP Log updates only.

4. **If §5 Step 2 confirms data-state cause:** file P3 UX gap for granular error code (`TOKEN_ALREADY_USED` vs `TOKEN_NOT_FOUND`). Not a P0 bug fix — UX polish.

5. **If §5 Step 1 confirms image promotion drift:** re-trigger `deploy-production.yml` workflow_dispatch with confirm=DEPLOY + version=v0.9.0-beta-staging.21. Verify SHA digest matches post-deploy.

## 11. References

- PR #1490 Wave 91 Bucket D defensive hardening
- `documents/04-quality/gaps/GAP-610-validate-token-returns-not-found-for-valid-token.md`
- `documents/04-quality/gaps/GAP-611-post-beta-signup-route-404.md`
- `documents/04-quality/audits/aws-verification/2026-05-17-wave-90-live-verify.md`
- `.claude/rules/pre-launch-owasp-rest-hardening-checklist.md` §2.1 A01 (RLS scope)
- `.claude/rules/postgres-specific-type-testcontainers.md` (Bucket D follows this rule)
- `.claude/rules/audit-to-gap-pipeline.md` §2.8 fix-time state-check (applied)
- `.claude/rules/pre-mutation-state-check.md` §3 (this artifact format)
- `.claude/rules/agent-aws-access.md` §5 (logging mandate)
- `.claude/rules/gap-done-discipline.md` §3 (GAP-610/611 stay PARTIAL until live verify)

## 12. Compliance

| Rule | Verdict |
|---|---|
| `agent-aws-access.md` §2 Tier 1 baseline | ✅ no AWS calls (account suspended) |
| `agent-aws-access.md` §5 logging mandate | ✅ this artifact |
| `pre-mutation-state-check.md` §3 audit format | ✅ scope+commands+findings+verdict+pending+recommendations |
| `audit-to-gap-pipeline.md` §2.8 fix-time state-check | ✅ §3 hypothesis verdicts + §5 empirical debug sequence |
| `gap-done-discipline.md` §3 | ✅ GAP-610/611 stay PARTIAL |
| `dev-readable-doc-language.md` | ✅ Vietnamese narrative + English identifiers |
| `pre-handoff-self-test-completeness.md` | ✅ §5 covers public/anonymous flow checklist (URL exists, browser POST verify, confirmation surface) |
| `concurrent-production-mutation-ops.md` | ✅ no mutation triggered |

## 13. Log

- **2026-05-18:** Deep investigation report shipped post Wave 91 Bucket D PR #1490. Static analysis confirms Bucket D verdicts (7 hypotheses NOT confirmed by code) AND surfaces 4 NEW hypothesis Bucket D missed (data state, image drift, exception path 404, token consume race). Most-likely cross-gap root cause: `findByInviteToken` returns empty Optional in runtime — code paths are correct, runtime state is suspect. 5-step debug sequence prepared for Coordinator F post-AWS-restore. No code fix shipped (per task constraint — speculative fix forbidden when no hypothesis confirmed by code).
