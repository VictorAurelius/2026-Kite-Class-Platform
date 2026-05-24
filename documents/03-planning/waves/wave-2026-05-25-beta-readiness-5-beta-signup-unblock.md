---
title: Wave beta-readiness-5 — Beta signup E2E unblock (4 P0 gap cluster)
status: draft
created: 2026-05-25
updated: 2026-05-25
wave: 5
tag_primary: beta-readiness
tags_secondary: [phase-1-beta-gate, beta-signup, gap-606, gap-608, gap-610, gap-611]
counter: 5
date_launch: 2026-05-25
waves: [beta-readiness-5]
gaps: [GAP-606, GAP-608, GAP-610, GAP-611]
---

# Wave beta-readiness-5 — Beta signup E2E unblock (4 P0 gap cluster)

**Goal:** Unblock beta signup flow end-to-end để 5 beta tenants có thể onboard. Fix 4 P0 gap blocking signup pipeline (email template + IAM permission + RLS bug + gateway route 404).
**Trigger:** Phase 1 BETA exit gate requires 5 beta tenants live. Currently 0 tenants can complete signup do 4 P0 gap chặn end-to-end pipeline. Highest leverage cho Phase 1 BETA → Phase 2 transition.
**Estimated wall-clock:** ~6-8h (4 agents Opus 1M parallel); ~25-30h serial → ~4x speedup.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment — inside-out 4-bucket per `inside-out-completeness-trigger.md` §3):**

- **Inside-out từ session handoff** `2026-05-24-wave-beta-readiness-4-closure.md` §"Wave 3/5": 4-bucket scope (Bucket A GAP-606 email template + B GAP-608 SES IAM + C GAP-610 RLS bug + D GAP-611 gateway 404)
- **Inside-out từ queue file** `documents/03-planning/inside-out-queue.md`: verify "beta signup unblock" trong queue Phase 1 BETA scope
- **Inside-out từ audit:** Wave 91 Bucket F live verify cluster references GAP-606/608/610/611 cluster
- **Outside-in NEW:** Cân nhắc spawn outside-in audit nếu user-facing surface change (signup landing + email template UX) — defer trừ khi reviewer flag missing persona scope

Persona phục vụ: Anonymous Prospect (Vy persona) + P2 Center Owner (Hằng) + P3 Center Manager (Tâm) + Platform Admin (Mai). Domain: signup pipeline cross-service (kitehub-platform + kitehub-email + kitehub-subscription + gateway).

**Q2 (trade-offs):**

| Rejected option | Reason |
|---|---|
| Ship Bucket B IAM live verify cùng wave | GAP-612 AWS suspended → live verify blocked. Ship code path parallel, defer live verify Wave audit-2+ post-restore |
| Combine 4 buckets vào 1 mega-PR | 4 disjoint scope (email template + IAM + RLS + gateway) → 4 PRs parallel cleaner; merge sequential |
| Defer GAP-611 gateway route audit Wave 4/5 | GAP-611 BLOCKING signup endpoint accessible (404) — must fix Wave 3/5 |
| Skip outside-in audit cho signup UX | User-facing scope nhưng change = bug fix (existing UX không thay đổi tone matrix); audit overkill |

**Q3 (risks):**

| Risk | Recovery |
|---|---|
| Bucket B IAM live verify gated GAP-612 AWS restore | Code path ship parallel; explicit follow-up gap `GAP-NEW-resend-live-verify-post-restore` linked GAP-612 unblock |
| Bucket C RLS suspect bug — root cause unclear pre-spawn | Bucket C agent debug session document — fix khi root cause identified; nếu out-of-scope → file follow-up gap + ship partial |
| 4 P0 gaps inter-dependency (signup E2E only works khi ALL 4 fixed) | Closure verify E2E flow: anonymous lands → submits form → email arrives → click verify → tenant provisioned |
| Sonnet thrash recurrence trên 4 agents parallel | Default Opus 1M cho impl agents per Wave br-4 lesson |

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-606 admin-new-login-alert.html template | bg-agent Opus | ~1-2h | ✅ kitehub-email/templates/ + email service handler |
| B | GAP-608 SES IAM ses:SendEmail permission | bg-agent Opus | ~2-3h | ✅ infrastructure/terraform-aws/iam.tf — code parallel, live verify blocked GAP-612 |
| C | GAP-610 GET /beta-signup/validate/{token} RLS bug | bg-agent Opus | ~3-4h | ✅ kitehub-platform/.../BetaSignupService + RLS policy SQL |
| D | GAP-611 POST /beta-signup gateway 404 | bg-agent Opus | ~2-3h | ✅ kite-gateway/SecurityConfig + route audit |
| Closure | 5-target sync + E2E verify + 5 follow-up gaps | coordinator inline | ~45-60 min | After 4 buckets |

Disjoint check:
- Bucket A: `kitehub/kitehub-email/src/main/resources/templates/email/admin-new-login-alert.html` + EmailService consumer
- Bucket B: `infrastructure/terraform-aws/iam.tf` IAM policy `kitehub-production-ec2-app` role permissions
- Bucket C: `kitehub/kitehub-platform/.../BetaSignupService` + Flyway migration cho RLS policy update
- Bucket D: `kitehub/kite-gateway/.../SecurityConfig` + `application*.yml` route mapping
- 4 disjoint service scopes; no shared code mutation

---

## 3. Scope

**Stake tier:** HIGH → Opus 4.7 (1M) cho all 4 impl agents (P0 blocking critical-path).
**Cross-layer? :** YES — Bucket A consumes email API; Bucket C touches FE signup form contract; Bucket D affects gateway routing.

**Bucket 0 Foundation needed?** NO — endpoints đã exist (gap chỉ fix bug). `documents/01-business/beta-signup/api-contract.md` đã có schema declaration (verify pre-spawn).

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-606 email template MISSING | 🔴 P0 | `kitehub/kitehub-email/src/main/resources/templates/email/admin-new-login-alert.html` (NEW) + EmailService consumer | parallel batch 1 |
| 2 | **B** | GAP-608 SES IAM permission | 🔴 P0 | `infrastructure/terraform-aws/iam.tf` policy update + paired live verify Wave audit-2+ | parallel batch 1 |
| 3 | **C** | GAP-610 RLS TOKEN_NOT_FOUND bug | 🔴 P0 | `kitehub/kitehub-platform/.../BetaSignupService.java` + Flyway V6X__fix-beta-signup-rls.sql (potentially) | parallel batch 1 |
| 4 | **D** | GAP-611 gateway 404 | 🔴 P0 | `kitehub/kite-gateway/.../SecurityConfig.java` + route audit | parallel batch 1 |
| 5 | **Closure** | 5-target sync + E2E verify + follow-up gaps | 🔴 P0 | After 4 buckets verify | sequential after A/B/C/D |

### Bucket A — GAP-606 email template

- Files: `kitehub/kitehub-email/src/main/resources/templates/email/admin-new-login-alert.html` (NEW Thymeleaf template)
- Acceptance: Email service render template không HTTP 500; consumer infinite retry loop fixed; smoke test verify rendering với sample VN data per `vn-localization-audit-checklist.md`

### Bucket B — GAP-608 SES IAM permission

- Files: `infrastructure/terraform-aws/iam.tf` — `kitehub-production-ec2-app` role gain `ses:SendEmail` action
- Acceptance: Terraform plan diff shows IAM policy update; code parallel ship (no AWS apply); follow-up gap `GAP-NEW-ses-iam-live-verify-post-restore` filed referencing GAP-612

### Bucket C — GAP-610 RLS TOKEN_NOT_FOUND

- Files: `kitehub/kitehub-platform/.../BetaSignupService.java` + Flyway migration nếu RLS policy update
- Pattern: Debug RLS policy with `EXPLAIN ANALYZE` on prod-equivalent Postgres + verify `current_setting('app.current_tenant')` GUC set
- Acceptance: GET `/api/v1/beta-signup/validate/{valid-token}` returns 200 với valid token; smoke test E2E

### Bucket D — GAP-611 gateway 404

- Files: `kitehub/kite-gateway/.../SecurityConfig.java` + `application*.yml` (route mapping nếu Spring Cloud Gateway)
- Pattern: Verify POST `/api/v1/beta-signup` reaches kitehub-platform; check gateway route + SecurityConfig public-endpoint matcher
- Acceptance: POST `/api/v1/beta-signup` returns 201 (not 404); curl smoke verify

---

## 4. State-Check Evidence

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| GAP-606 | Gap file | `bash scripts/query-gaps.sh 606` | OPEN P0 | ✅ exists |
| GAP-608 | Gap file | `bash scripts/query-gaps.sh 608` | OPEN P0 | ✅ exists |
| GAP-610 | Gap file | `bash scripts/query-gaps.sh 610` | OPEN P0 | ✅ exists |
| GAP-611 | Gap file | `bash scripts/query-gaps.sh 611` | OPEN P0 | ✅ exists |
| `admin-new-login-alert.html` | Email template | `find kitehub/kitehub-email -name "admin-new-login-alert.html"` | (verify pre-spawn) | 🆕 to-be-created (Bucket A) |
| `kitehub-production-ec2-app` IAM role | Terraform resource | `grep "kitehub-production-ec2-app" infrastructure/terraform-aws/iam.tf` | (verify pre-spawn) | ✅ expected to exist |
| `BetaSignupService` | Java service | `find kitehub/kitehub-platform -name "BetaSignupService.java"` | (verify pre-spawn) | ✅ expected to exist |
| `documents/01-business/beta-signup/api-contract.md` | API contract | `ls documents/01-business/beta-signup/api-contract.md` | (verify pre-spawn) | ✅ expected to exist |

---

## 5. Verification Gates

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `cd kitehub && ./mvnw -pl kitehub-email verify -P strict-warnings` + smoke template render | kitehub-ci |
| B | `cd infrastructure/terraform-aws && terraform plan -target=aws_iam_role_policy.app_role` + GAP-612 follow-up file | None (no apply pre-restore) |
| C | `cd kitehub && ./mvnw -pl kitehub-platform verify -P strict-warnings -Dtest=BetaSignupServiceTest*` | kitehub-ci |
| D | `cd kitehub && ./mvnw -pl kite-gateway verify -P strict-warnings` + curl POST /api/v1/beta-signup | gateway-ci |
| E2E Closure | Manual smoke: anonymous lands → form → email → verify link → tenant provisioned | None (pre-prod env required) |

---

## 6. Agent Spawn Pattern

4 agents parallel batch 1 (all Opus per Wave br-4 thrash lesson):

```
Bucket A: subagent_type=general-purpose, model=opus, isolation=worktree, run_in_background=true
Bucket B: subagent_type=general-purpose, model=opus, isolation=worktree, run_in_background=true
Bucket C: subagent_type=general-purpose, model=opus, isolation=worktree, run_in_background=true
Bucket D: subagent_type=general-purpose, model=opus, isolation=worktree, run_in_background=true

After 4 verify PASS:
  - Coordinator E2E smoke verify
  - File follow-up gap `GAP-NEW-ses-iam-live-verify-post-restore` (gated GAP-612)
  - 5-target sync + GAP-606/608/610/611 DONE flip
```

---

## 7. Closure Protocol

1. All 4 buckets SHIPPED với local verify PASS
2. E2E smoke verify: anonymous signup flow end-to-end pre-prod
3. 4 P0 gaps flipped (GAP-606/608/610/611) per `gap-done-discipline.md`
4. Bucket B GAP-608 PARTIAL (code ship + live verify deferred) — follow-up gap filed
5. 5-target sync + handoff
6. Phase 1 BETA gate progress entry: "0 → N tenants signup-eligible"
7. Worktree cleanup

---

## 8. Log

- **2026-05-25 (status: draft):** Wave plan drafted per session handoff `2026-05-24-wave-beta-readiness-4-closure.md` §"Wave 3/5" pickup. Counter `beta-readiness-5` = next monotonic (1/2/3/4 đã consume). Highest leverage cho Phase 1 BETA → Phase 2 gate transition. Bucket B GAP-608 ship code-only — live verify blocked GAP-612 AWS suspension. Outside-in audit deferred (user-facing nhưng change = bug fix, không UX redesign). Author: @nguyenvankiet (solo-dev coordinator).
