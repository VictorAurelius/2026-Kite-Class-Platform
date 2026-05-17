---
title: Wave 91 — Production Restore + Email Infra + Beta Signup Cluster
status: draft
created: 2026-05-18
updated: 2026-05-18
waves: [91]
gaps: [GAP-605, GAP-606, GAP-607, GAP-608, GAP-609, GAP-610, GAP-611, GAP-612]
---

# Wave 91 — Production Restore + Email Infra + Beta Signup Cluster

**Goal:** Unblock beta cohort onboarding end-to-end sau Wave 90 walkthrough surfaced 8 bugs. 4 parallel buckets fix code + infra; coordinator deploy + verify khi AWS restored.

**Trigger:** Wave 90 walkthrough phase 2 (user submit beta + admin approve = PASS, email + signup = FAIL) phát hiện 7 infra bugs + 1 account-level blocker (AWS suspension GAP-612). Production stack down kể từ ~16:50 UTC 2026-05-17. AWS Support case 177903869600100 mở 17:25 UTC; awaiting AWS reply 24-72h.

**Estimated wall-clock:** ~8-10h agent work × 4 buckets parallel ≈ longest bucket ~3-4h. Coordinator deploy + verify post-AWS-restore +1h.

**BLOCKED until GAP-612 resolved:** Wave 91 plan + code-only PRs CAN ship offline (this docs PR + 4 bucket code PRs); deploy + live verify BLOCKED until AWS reactivates account.

---

## 1. Brainstorm

### Q1: Inside-out + outside-in completeness per `inside-out-completeness-trigger.md` §3

**Inside-out source 1 — ROADMAP §🚀 Next Action:**
- 8 gaps Wave 90 walkthrough surfaced (GAP-605..612)
- 3 long-term P2/P3 follow-ups (uptime monitoring + DR plan + AWS health dashboard) — defer Wave 92+

**Inside-out source 2 — `documents/03-planning/inside-out-queue.md` (5 items):**
- Premium plan → Phase 1.5+ (n/a Wave 91)
- Feedback channel — consumed Wave 78 (n/a)
- Email content audit — consumed Wave 78 (n/a)
- User manual VN — consumed Wave 79 (n/a)
- Manual split professional vs end-user — Wave 92+ candidate (doc work, không match Wave 91 infra scope)

**Inside-out source 3 — CSV query phase-1-beta non-DONE overlap check:**
- GAP-525/514/524/515/521 pre-tenant cluster — defer Wave 92+ (depend on Wave 91 stability)
- GAP-257 P0 restore drill, GAP-144 P1 AlertManager — long-term, không match
- No overlap candidates with Wave 91 scope

**Outside-in NEW per `outside-in-coverage-trigger.md` §4 exception:**
- SKIPPED — Wave 91 scope = backend infra fixes (gateway / email / IAM / FE component), không user-facing flow mới. Wave 90 walkthrough đã serve as outside-in audit (real persona Nguyễn Thùy Dương DG Edu → surfaced 8 bugs concretely).

### Q2: Trade-offs considered

- **Single mega-bucket (1 PR all 7 code gaps)** — rejected: 4 disjoint paths exist; parallel = ~4x speedup; merge conflict risk khi 7 changes overlap kitehub-subscription
- **Skip GAP-607 RMQ DLQ Wave 91 (defer Wave 92)** — rejected: poison message infinite retry already proven harmful Wave 90 evidence; DLQ small config change ~1h pair with cluster
- **Skip GAP-609 FE claim page (email-driven path enough)** — rejected: email infra fix-cycle có thể fail again (SES quota, deliverability); FE fallback path essential
- **Fix GAP-610 RLS via DROP POLICY (simplest)** — rejected: violates A01 defense-in-depth (per `pre-launch-owasp-rest-hardening-checklist.md` §2.1); proper public-bypass policy preserves tenant isolation
- **Spawn agents NOW (offline)** — rejected: user picked "Draft Wave 91 plan PR offline"; agents wait AWS active để deploy + verify; spawn pre-AWS = code lands without verify path = PARTIAL trap

### Q3: Risks + recovery

| Risk | Bucket | Recovery |
|---|---|---|
| GAP-612 AWS suspension >7 days → account deletion 1/6/2026 | Coordinator | Backup plan Wave 92 docs Oracle Cloud / Vercel migration (defer until AWS Day 5 silent) |
| GAP-605 outbox dispatcher fix breaks existing event flows (admin login alert, audit, migration) | A | Integration test cover all existing event types; pair with GAP-606 template fix to clear poison queue |
| GAP-606 admin-new-login-alert template variables mismatch producer code | A | Read AuthService event producer to extract exact variable names; align template |
| GAP-608 terraform IAM apply WILL replace EC2 if user_data hash changes (per Wave 89 lesson) | B | Use targeted apply `aws_iam_role_policy.ec2_secrets_s3` only; verify diff = in-place IAM only |
| GAP-610 RLS bypass policy too broad → cross-tenant leak | D | Scope policy: `status IN ('APPROVED') AND invite_token_expiry > NOW()` — only active tokens visible publicly |
| GAP-611 fix conflicts with Wave 89 JwtAuthenticationGatewayFilter | D | Verify filter `isPublicPath()` matches `/api/v1/auth/beta-signup` POST; add unit test |
| GAP-609 FE claim page implementation drift from BE exchangeClaimCode contract | C | Read `BetaClaimCodeExchangeResponse` shape; FE form follows verbatim |
| Bucket A + D both touch kitehub-subscription → merge conflict | A/D | Disjoint files: A = SubscriptionEventEmitter + EmailQueueConfig; D = BetaAccessController + BetaAccessService + RLS migration |

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-605 + GAP-607 | bg-agent (Opus) | ~180min | ✅ `kitehub-subscription` event/queue config — outbox dispatcher + DLQ + fast-path RMQ |
| B | GAP-608 | bg-agent (Sonnet) | ~45min | ✅ `infrastructure/terraform-aws/iam.tf` only — SES SendEmail statement |
| C | GAP-606 | bg-agent (Sonnet) | ~60min | ✅ `kitehub-email/src/main/resources/templates/emails/` only — admin-new-login-alert.html |
| D | GAP-610 + GAP-611 | bg-agent (Opus) | ~150min | ⚠️ `kitehub-subscription` BetaAccessController + Service + RLS migration + Gateway filter audit (overlap với A on subscription module — coordinate via different package paths) |
| E | GAP-609 | bg-agent (Sonnet) | ~120min | ✅ `kitehub-frontend/src/app/(auth)/beta-signup/code/` only — claim code redemption page |

Disjoint check:
- A (subscription event/queue) ≠ D (subscription beta) — different packages (`outbox/` vs `beta/`)
- B (terraform) standalone
- C (email templates) standalone
- E (frontend) standalone
- All 5 buckets can spawn parallel

---

## 3. Scope

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** HIGH — Wave 91 unblocks production beta cohort. Bucket A (outbox dispatcher) + Bucket D (RLS policy) = cross-cutting architecture risk → Opus 4.7. Bucket B/C/E lower risk → Sonnet.
**Cross-layer? (per `contract-first-for-cross-layer.md` §2):** NO — Bucket E FE consumes existing `BetaClaimCodeExchangeResponse` endpoint (already in api-contract per Wave 36 GAP-388). No new BE endpoint. Skip Bucket 0 Foundation.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** Outbox dispatcher + RMQ DLQ | GAP-605, GAP-607 | 🔴 P0 | `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/migration/SubscriptionEventEmitter.java` + NEW `outbox/SubscriptionOutboxDispatcher.java` + `config/EmailQueueConfig.java` + tests | parallel batch 1 |
| 2 | **B** EC2 IAM SES permission | GAP-608 | 🔴 P0 | `infrastructure/terraform-aws/iam.tf` (kitehub-ec2-secrets-s3 policy) | parallel batch 1 |
| 3 | **C** Admin login alert email template | GAP-606 | 🔴 P0 | NEW `kitehub/kitehub-email/src/main/resources/templates/emails/admin-new-login-alert.html` + producer audit | parallel batch 1 |
| 4 | **D** Beta signup BE bugs | GAP-610, GAP-611 | 🔴 P0 | `kitehub-subscription/src/main/java/com/kitehub/subscription/beta/{controller,service}/` + NEW Flyway migration `V60__beta_access_request_public_bypass_rls.sql` + Gateway filter unit test | parallel batch 1 |
| 5 | **E** FE claim code redemption page | GAP-609 | 🟠 P1 | NEW `kitehub-frontend/src/app/(auth)/beta-signup/code/page.tsx` + `components/auth/BetaClaimCodeForm.tsx` + landing CTA link | parallel batch 1 |
| F | **Coordinator** deploy + live verify | (all 7 code gaps) | 🔴 P0 | post-AWS-restore: tag staging.22 → deploy → terraform apply → curl verify | sequential AFTER batch 1 merged |

### Bucket A — Outbox dispatcher + RMQ DLQ (GAP-605 + GAP-607)

**Files:**
- EDIT: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/migration/SubscriptionEventEmitter.java` — add fast-path `rabbitTemplate.convertAndSend` post outbox save (per `design-patterns.md §3.5.1` Exception A)
- NEW: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/outbox/SubscriptionOutboxDispatcher.java` — `@Scheduled(fixedDelay=10000)` poll `WHERE dispatched_at IS NULL` → publish to RMQ → UPDATE dispatched_at
- EDIT: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/config/EmailQueueConfig.java` — add DLX `email.dlx` + DLQ `email.dlq` + queue arguments `x-dead-letter-exchange` + `x-dead-letter-routing-key` + `RetryInterceptor` max 3 attempts exponential backoff
- NEW: `kitehub/kitehub-subscription/src/test/java/.../outbox/SubscriptionOutboxDispatcherTest.java` — 5 cases: poll picks NULL rows / publishes RMQ / updates dispatched_at / handles publish failure / metric exposed
- NEW: `kitehub/kitehub-subscription/src/test/java/.../config/EmailQueueDlqIntegrationTest.java` — send poison message → 3 retries → land in DLQ
- EDIT: `application.yml` config `outbox.dispatcher.poll-interval-ms: 10000` + `outbox.dispatcher.max-backoff-min: 5`

**Implementation per GAP-605 §"Proposed Fix" Phase 1+2:**
- Fast-path: best-effort publish + log warn if fails
- Dispatcher: `@Scheduled(fixedDelay = 10000)` + `@Async` per thread pool; skip rows with last-publish-attempt < 5min ago (backoff)
- Metrics: `outbox_undispatched_count{event_type}` + `outbox_dispatcher_lag_seconds` exposed `/actuator/prometheus`

**Pre-implementation state-check (mandatory, in agent prompt):**
```bash
# Verify existing outbox infrastructure
grep -rn "subscription_outbox" kitehub/kitehub-subscription/src/main --include="*.java" --include="*.yml"
# Read EmailQueueConfig + EmailServiceClient (fast-path pattern reference)
cat kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/config/EmailQueueConfig.java
# Verify @EnableScheduling already present
grep -rn "@EnableScheduling" kitehub/kitehub-subscription/src/main/java
```

**Tests:**
- Unit: dispatcher poll logic (mock JPA repo + RabbitTemplate)
- Integration: real RMQ + real Postgres testcontainer (per `postgres-specific-type-testcontainers.md`)
- ArchUnit: enforce all outbox writes go through SubscriptionEventEmitter (no direct `outboxRepository.save`)

**Acceptance:**
- [ ] Fast-path publish: emit event → RMQ message visible within 100ms
- [ ] Dispatcher poll: stuck row (dispatched_at NULL >10s) → published + marked dispatched within next poll cycle
- [ ] DLQ: poison message (template missing → 500 retry) → 3 attempts → land in DLQ → consumer log "moved to DLQ"
- [ ] Backfill Wave 90 stuck rows: SQL `UPDATE subscription_outbox SET dispatched_at = NULL WHERE event_type IN (...)` after deploy → dispatcher catches up
- [ ] Metric exposed: `outbox_undispatched_count` baseline 0 + `dlq_depth` baseline 0
- [ ] `cd kitehub && ./mvnw -pl kitehub-subscription verify -P strict-warnings` pass

### Bucket B — EC2 IAM SES permission (GAP-608)

**Files:**
- EDIT: `infrastructure/terraform-aws/iam.tf` — extend `kitehub-ec2-secrets-s3` policy với new statement `SesSendEmail` per GAP-608 §"Proposed Fix" Phase 1

**Pre-implementation state-check:**
```bash
# Locate existing IAM policy
grep -rn "kitehub-ec2-secrets-s3\|ec2_secrets_s3" infrastructure/terraform-aws/
# Read current statements
cat infrastructure/terraform-aws/iam.tf | grep -A 30 "ec2_secrets_s3"
# Verify role attachment
aws iam list-attached-role-policies --role-name kitehub-production-ec2-app  # AWS-blocked until restore
```

**Implementation:**
```hcl
statement {
  sid    = "SesSendEmail"
  effect = "Allow"
  actions = [
    "ses:SendEmail",
    "ses:SendRawEmail",
    "ses:SendTemplatedEmail",
    "ses:GetSendQuota",
  ]
  resources = [
    "arn:aws:ses:ap-southeast-1:906286017800:identity/kitehub.me",
    "arn:aws:ses:ap-southeast-1:906286017800:identity/*@kitehub.me",
    "arn:aws:ses:ap-southeast-1:906286017800:identity/*",
    "arn:aws:ses:ap-southeast-1:906286017800:configuration-set/*",
  ]
}
```

**Tests:**
- `terraform fmt -check` pass
- `terraform validate` pass

**Acceptance:**
- [ ] iam.tf includes SES statement với correct ARNs
- [ ] Terraform fmt + validate pass
- [ ] PR body documents deploy plan: targeted apply `aws_iam_role_policy.ec2_secrets_s3` post-AWS-restore (Coordinator F responsibility)
- [ ] Live verify deferred to coordinator F: `aws iam simulate-principal-policy --action-names ses:SendEmail` returns Allow

### Bucket C — Admin login alert email template (GAP-606)

**Files:**
- NEW: `kitehub/kitehub-email/src/main/resources/templates/emails/admin-new-login-alert.html`
- AUDIT: producer side `grep -rn "admin-new-login-alert" kitehub/kitehub-admin/src kitehub/kitehub-subscription/src` để confirm exact template name + variables
- NEW: `kitehub/kitehub-email/src/test/java/com/kitehub/email/service/AdminNewLoginAlertTemplateTest.java`

**Implementation:**
- Vietnamese narrative per `dev-readable-doc-language.md`
- Brand fallback pattern from existing templates (beta-invite.html / welcome.html)
- 40-point checklist per `email-template-review/SKILL.md`
- Required variables (verify-at-spawn): `adminEmail`, `loginTime`, `ipAddress`, `userAgent` (optional: `geolocation`, `deviceType`)
- Subject (sent by producer): "🚨 Đăng nhập mới vào tài khoản Admin KiteHub"

**Tests:**
- Thymeleaf render test với sample variables → no exception
- Smoke: POST kitehub-email/api/platform/emails/send với templateName="admin-new-login-alert" → 200 (no Template-InputException)

**Acceptance:**
- [ ] Template file ships với correct path + variables
- [ ] Render test pass
- [ ] Wave 90 admin-new-login-alert poison queue clear post-deploy (consumer log shows "processed successfully" not retry loop)
- [ ] Phase 2 CI check `scripts/check-email-template-coverage.sh` filed as follow-up GAP-613 (deferred Wave 92)

### Bucket D — Beta signup BE bugs (GAP-610 + GAP-611)

**Files:**
- VERIFY: `kitehub-subscription/.../beta/controller/BetaAccessController.java` — confirm `@PostMapping("/api/v1/auth/beta-signup")` exists + signature
- VERIFY: `kitehub-subscription/.../beta/service/BetaAccessService.java` — confirm `validateToken` + `completeBetaSignup` impl
- NEW Flyway: `kitehub/kitehub-subscription/src/main/resources/db/migration/V60__beta_access_request_public_bypass_rls.sql` — RLS policy allowing public read of APPROVED + not-expired rows
- VERIFY: `kitehub/kitehub-gateway/src/main/java/.../filter/JwtAuthenticationGatewayFilter.java` — `isPublicPath` matches `/api/v1/auth/beta-signup` (Wave 89 regression check)
- NEW: `kitehub-subscription/src/test/.../BetaAccessRepositoryRlsIntegrationTest.java` — RLS bypass scenario test
- NEW: `kitehub-gateway/src/test/.../JwtAuthenticationGatewayFilterPublicPathTest.java` — verify `/api/v1/auth/beta-signup` POST bypasses filter

**Implementation Hypothesis-driven:**

Per GAP-610 hypotheses:
1. **Hypothesis 1 (RLS):** Likely. V60 migration:
   ```sql
   ALTER TABLE beta_access_request ENABLE ROW LEVEL SECURITY;
   -- Existing policy (tenant_isolation) preserved
   CREATE POLICY beta_access_public_token_lookup ON beta_access_request
     FOR SELECT
     USING (status = 'APPROVED' AND invite_token_expiry > NOW());
   -- Allows anonymous endpoints (no current_tenant_id set) to find active tokens
   ```
2. **Hypothesis 2 (UUID encoding):** Backup. Add `@Type(type = "pg-uuid")` if Hypothesis 1 doesn't fix.
3. **Hypothesis 3 (JPA query):** Backup. Replace `findByInviteToken(UUID)` with explicit `@Query`.

Per GAP-611 hypotheses:
1. **Hypothesis 1 (gateway route order):** Verify `kitehub-auth-v1` catch-all not shadowed by earlier specific route
2. **Hypothesis 3 (JWT filter regression):** Unit test `isPublicPath("/api/v1/auth/beta-signup")` returns true for POST
3. **Hypothesis 4 (Security config):** Verify `SecurityFilterChain` `permitAll` for `/api/v1/auth/beta-signup`

**Pre-implementation state-check:**
```bash
# RLS on beta_access_request
grep -rn "beta_access_request" kitehub/kitehub-subscription/src/main/resources/db/migration/*.sql | head -10
# Existing RLS policies
grep -B 2 -A 10 "CREATE POLICY.*beta_access" kitehub/kitehub-subscription/src/main/resources/db/migration/*.sql
# Security config
grep -B 2 -A 5 "beta-signup" kitehub/kitehub-subscription/src/main/java/.../config/SecurityConfig.java
# Gateway filter coverage
grep -A 10 "isPublicPath" kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/filter/JwtAuthenticationGatewayFilter.java
```

**Tests:**
- RLS integration: testcontainer Postgres + RLS enabled → anonymous query returns APPROVED rows ✓ / hides PENDING ✓ / hides SIGNED_UP ✓ / hides expired ✓
- Gateway filter unit: 4 paths × 2 methods × {valid JWT, no JWT, malformed JWT} matrix
- E2E test: POST `/api/v1/auth/beta-signup` với valid token → 200 + tenant created

**Acceptance:**
- [ ] V60 RLS migration ships + applied
- [ ] GET `/api/v1/auth/beta-signup/validate?token=<valid>` → 200 với pre-fill data (was 404 Wave 90)
- [ ] POST `/api/v1/auth/beta-signup` → 200 + tenant created (was 404 Wave 90)
- [ ] Wave 89 JwtAuthenticationGatewayFilter `isPublicPath` test cover beta-signup POST explicitly
- [ ] Integration test E2E pass

### Bucket E — FE claim code redemption page (GAP-609)

**Files:**
- NEW: `kitehub/kitehub-frontend/src/app/(auth)/beta-signup/code/page.tsx` (route `/beta-signup/code`)
- NEW: `kitehub/kitehub-frontend/src/components/auth/BetaClaimCodeForm.tsx`
- NEW: `kitehub/kitehub-frontend/src/components/auth/__tests__/BetaClaimCodeForm.test.tsx`
- EDIT: landing page `kitehub-frontend/src/app/(public)/page.tsx` — add "Tôi đã có mã invite" CTA link
- EDIT: `kitehub-frontend/src/lib/api/endpoints.ts` — add `exchangeClaimCode` endpoint
- EDIT: `kitehub-frontend/src/test/msw/handlers/beta-access.ts` — add handler for exchange-claim-code mock

**Implementation per GAP-609 §"Phase 1":**
- 6-digit input (numeric only, validate length)
- Vietnamese narrative + annotated UI per `user-manual-content-standard.md`
- Error map (Vietnamese): CODE_NOT_FOUND="Mã không hợp lệ" / CODE_EXPIRED="Mã đã hết hạn" / ALREADY_USED="Mã đã được sử dụng"
- Success → redirect `/beta-signup?token=<returned-token>` (reuse existing BetaSignupForm)

**Pre-implementation state-check:**
```bash
# Verify exchangeClaimCode BE endpoint shape
grep -A 20 "exchangeClaimCode\|BetaClaimCodeExchange" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/
# Existing FE patterns
cat kitehub/kitehub-frontend/src/components/auth/BetaSignupForm.tsx | head -30
# Landing page structure
ls kitehub/kitehub-frontend/src/app/'(public)'/
```

**Tests:**
- Component test: render + 4 cases (valid/not-found/expired/already-used)
- E2E (deferred per Wave 87 Bucket F pattern — Playwright)

**Acceptance:**
- [ ] `/beta-signup/code` route renders + accepts 6-digit code
- [ ] Valid code → redirect `/beta-signup?token=<UUID>` với pre-fill
- [ ] Invalid code → Vietnamese error toast
- [ ] Landing page CTA link visible
- [ ] 4 component test cases pass

### Bucket F — Coordinator deploy + live verify (POST-AWS-RESTORE)

**Sequential after Batch 1 all merged + AWS active:**
1. Verify AWS active: `aws sts get-caller-identity` returns identity (not InvalidClientTokenId)
1b. **GAP-613 Phase 1 (CloudWatch reduce)** — login Billing console → identify overage service(s) → if alarms >10 disable non-critical → shorten Logs retention 30d→7d → set $5 Budget alarm. Execute BEFORE start-stack để cost baseline clean trước beta cohort load.
2. Start stack: `bash scripts/aws/start-stack.sh`
3. Bucket B terraform apply: `gh workflow run terraform-apply.yml -f targets='aws_iam_role_policy.ec2_secrets_s3' -f confirm=APPLY -f dry_run=true` → review → `dry_run=false`
4. Tag staging.22 + docker-build-push wait
5. Deploy: `gh workflow run deploy-production.yml -f version=v0.9.0-beta-staging.22 -f confirm=DEPLOY`
6. Backfill stuck outbox rows: SSM SQL `UPDATE subscription_outbox SET dispatched_at = NULL WHERE created_at < NOW()` (one-time)
7. Live verify each gap:
   - GAP-605: emit beta.invite event → email arrives within 60s
   - GAP-606: trigger admin login → admin-new-login-alert email arrives, no template error in logs
   - GAP-607: check RMQ Management UI DLQ baseline 0
   - GAP-608: `aws iam simulate-principal-policy --action-names ses:SendEmail` Allow
   - GAP-610: curl GET validate token returns 200 with pre-fill
   - GAP-611: curl POST signup completes, tenant created
   - GAP-609: browser open `/beta-signup/code` → enter 190563 → redirect
8. Flip all 7 gaps DONE per `gap-done-discipline.md` §2
9. GAP-612 flip DONE (account restored = AC met)
10. Closure docs PR

---

## 4. State-Check Evidence

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `SubscriptionEventEmitter.emit()` | Java method | `grep -A 15 "public void emit.*UUID" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/migration/SubscriptionEventEmitter.java` | per Wave 90 GAP-605 evidence — no fast-path RMQ publish | ✅ exists (needs extension Bucket A) |
| `SubscriptionOutboxDispatcher` | Java class | `find kitehub/kitehub-subscription/src/main -name "*Dispatcher*"` | 0 results | 🆕 to-be-created (Bucket A) |
| `EmailQueueConfig` | Java class | `find kitehub/kitehub-subscription/src/main -name "EmailQueueConfig.java"` | per Wave 90 grep evidence | ✅ exists (needs DLX/DLQ extension Bucket A) |
| `admin-new-login-alert.html` | Thymeleaf template | `ls kitehub/kitehub-email/src/main/resources/templates/emails/admin-new-login-alert.html` | not exists | 🆕 to-be-created (Bucket C) |
| `iam.tf kitehub-ec2-secrets-s3` | Terraform policy | `grep -A 20 "kitehub-ec2-secrets-s3" infrastructure/terraform-aws/iam.tf` | verify-at-spawn | ⚠️ verify-at-spawn (Bucket B) |
| `beta_access_request` RLS | DB policy | `grep -A 5 "POLICY.*beta_access" kitehub/kitehub-subscription/src/main/resources/db/migration/*.sql` | not yet checked | ⚠️ verify-at-spawn (Bucket D — hypothesis 1) |
| `JwtAuthenticationGatewayFilter` | Java class | `ls kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/filter/JwtAuthenticationGatewayFilter.java` | per Wave 89 PR #1480 | ✅ exists (audit `isPublicPath` Bucket D) |
| `BetaAccessController.completeBetaSignup` | Java method | `grep -B 1 -A 5 "completeBetaSignup\|@PostMapping.*beta-signup\"" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/controller/BetaAccessController.java` | per Wave 90 grep evidence | ✅ exists (debug Bucket D) |
| `BetaSignupForm.tsx` | React component | `ls kitehub/kitehub-frontend/src/components/auth/BetaSignupForm.tsx` | per Wave 90 evidence | ✅ exists (reference for Bucket E) |
| `BetaClaimCodeExchangeResponse` DTO | Java DTO | `grep -rn "BetaClaimCodeExchangeResponse" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/dto/` | per Wave 36 GAP-388 | ✅ exists (FE contract Bucket E) |
| `endpoints.ts auth` | TypeScript | `grep -A 5 "auth:" kitehub/kitehub-frontend/src/lib/api/endpoints.ts` | per Wave 90 grep evidence | ✅ exists (extend Bucket E) |
| AWS account state | account-level | `aws sts get-caller-identity` | InvalidClientTokenId (suspended) | ❌ blocked by GAP-612 |

**Banned shortcuts:**
- `| head` truncation
- Skipping verify-at-spawn (Bucket B, D both have ⚠️ entries needing live grep)
- Aspirational symbol references without 🆕 flag

**verify-at-spawn:** bucket agents PHẢI run grep/ls commands listed trước khi propose changes.

---

## 5. Verification Gates

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `cd kitehub && ./mvnw -pl kitehub-subscription verify -P strict-warnings` (incl new dispatcher + DLQ integration tests) | core-ci |
| B | `cd infrastructure/terraform-aws && terraform fmt -check && terraform validate` | terraform-plan |
| C | `cd kitehub && ./mvnw -pl kitehub-email verify -P strict-warnings` + thymeleaf render test | core-ci |
| D | `cd kitehub && ./mvnw -pl kitehub-subscription,kitehub-gateway verify -P strict-warnings` (incl RLS integration test + filter unit test) | core-ci + gateway-ci |
| E | `cd kitehub/kitehub-frontend && pnpm test --run BetaClaimCodeForm && pnpm build` | frontend-ci |
| F | (post-AWS-restore live verify per §3 Bucket F sequence) | manual coordinator |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:

**Batch 1 (parallel, spawn after this plan PR merge):** A + B + C + D + E — 5 agents `run_in_background: true`, `isolation: worktree`

⚠️ **Spawn timing:** OK to spawn code agents NOW even khi AWS suspended (code changes don't need AWS). Live verify (Bucket F) sequential AFTER AWS restored.

Coordinator (this session OR next) handles:
- Verify CI green per bucket
- Sequential merge to main (A first, then B/C/D/E in any order)
- Bucket F coordinator phase post-AWS-restore
- Closure PR includes: ROADMAP update + wave plan `status: complete` + wave-history.jsonl + `bash scripts/prune-merged-worktrees.sh --yes`

---

## 7. Closure Protocol

Per `post-wave-cleanup.md` + `gap-done-discipline.md` + `post-merge-sync-completeness.md`:

- [ ] All 5 batch-1 buckets merged
- [ ] Bucket F coordinator phase: AWS active + stack started + deploy + terraform apply + 7 live verifies complete
- [ ] GAP-605, GAP-606, GAP-607, GAP-608, GAP-609, GAP-610, GAP-611, GAP-612 flipped DONE per `gap-done-discipline.md` §2 (OR PARTIAL với follow-up filed per §3 PARTIAL exit ramp)
- [ ] Wave plan `status: complete` + `updated:` bumped
- [ ] `wave-history.jsonl` append entry
- [ ] ROADMAP §🚀 Next Action updated — queue Wave 92 (pre-tenant cluster GAP-525/514/524/515/521 + GAP-613 CloudWatch Free Tier reduce + long-term P2/P3 follow-ups: uptime monitoring + DR plan + AWS health dashboard + Manual split queue 5th item)
- [ ] `bash scripts/prune-merged-worktrees.sh --yes` clean
- [ ] AWS Support case `177903869600100` flip RESOLVED (account active)
- [ ] AWS Support case `177857212400418` flip RESOLVED (SES production access OR document defer)
- [ ] Handoff message: "Wave 91 ✅ ship. Production fully restored + beta cohort onboarding unblocked end-to-end. Next: invite first 3-5 beta tenants per ROADMAP queue."

---

## 8. Log

- **2026-05-18:** Wave 91 plan drafted. Scope locked via AskUserQuestion explicit (option "Draft Wave 91 plan PR offline" — code agents can ship while AWS suspended; deploy + verify gated by AWS restoration). Inside-out audit 3-source (ROADMAP + inside-out-queue.md + CSV phase-1-beta non-DONE) consulted: 8 walkthrough gaps in scope, 5 queue items non-match, pre-tenant cluster defer Wave 92. Outside-in audit SKIPPED per `outside-in-coverage-trigger.md` §4 exception — Wave 90 walkthrough (real persona Nguyễn Thùy Dương) served as concrete outside-in evidence (8 bugs surfaced). Cross-layer check: NOT cross-layer (Bucket E FE consumes existing `BetaClaimCodeExchangeResponse` contract per Wave 36). Concurrent ops check: Bucket B terraform IAM + future Bucket F deploy = serial per `concurrent-production-mutation-ops.md` §3.1. Coordinator F BLOCKED until GAP-612 AWS suspension resolved (case 177903869600100 pending AWS reply 24-72h).
