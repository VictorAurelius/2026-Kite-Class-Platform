---
title: Wave beta-readiness-5 — Beta signup E2E unblock (4 P0 gap cluster)
status: complete
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

### ⚠️ Pre-spawn state-check refinement 2026-05-25 (Wave meta-3 follow-up session)

Empirical state-check per `audit-to-gap-pipeline.md` §2.8 + `release-fix-retry-budget.md` §3.5 Investigation phase mandate caught 3 scope errors trong wave plan original draft (8d gap age → stale hypothesis):

1. **Bucket A obsolete:** GAP-606 template ALREADY EXISTS (`kitehub-email/src/main/resources/templates/emails/admin-new-login-alert.html` 192 LOC) — shipped Wave 91 Bucket C PR #1486 (ac2dd2f2) 2026-05-17. Gap CSV row stale 8 days. Flipped 🟢 DONE same-PR-as-this-refinement. **Bucket A REMOVED from wave scope.**
2. **Bucket C wrong module + service name:** Plan said `kitehub-platform/.../BetaSignupService.java`; actual location `kitehub-subscription/.../beta/service/BetaAccessService.java` (Beta**Access** not Beta**Signup**). `validateToken(UUID token)` method at line 531 calls `repository.findByInviteToken(token)`; TOKEN_NOT_FOUND returned at line 535 (Optional empty) AND line 541 (lifecycle gating — APPROVED status check). Real RLS hypothesis needs targeted re-investigation.
3. **Bucket D wrong endpoint path:** Plan said `POST /api/v1/beta-signup`; actual route `POST /api/v1/auth/beta-signup` (with `/auth/` prefix). Likely root cause của 404 — FE call route mismatch. Need verify FE BetaSignupForm component target URL + gateway route patterns.

Refined 3-bucket scope below (B + C + D, A skipped):

| # | Bucket | Gap(s) | Priority | Files (corrected paths) | Spawn order |
|:-:|--------|--------|:--------:|--------------------------|:-----------:|
| 1 | ~~A~~ | ~~GAP-606~~ | — | **SKIPPED** — already DONE Wave 91 PR #1486 | — |
| 2 | **B** | GAP-608 SES IAM permission | 🔴 P0 | `infrastructure/terraform-aws/iam.tf` — `kitehub-production-ec2-app` role + `ses:SendEmail` action (terraform code only, no AWS apply per GAP-612) | parallel batch 1 |
| 3 | **C** | GAP-610 RLS TOKEN_NOT_FOUND bug | 🔴 P0 | `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/service/BetaAccessService.java` (method `validateToken` line 531) + `BetaAccessRequestRepository.findByInviteToken` + Flyway V6X__fix RLS migration nếu cần | parallel batch 1 |
| 4 | **D** | GAP-611 gateway 404 | 🔴 P0 | `kitehub/kite-gateway/.../SecurityConfig.java` route audit + verify FE `BetaSignupForm.tsx` call URL vs actual `POST /api/v1/auth/beta-signup` (note `/auth/` prefix) | parallel batch 1 |
| 5 | **Closure** | 5-target sync + E2E verify + follow-up gaps | 🔴 P0 | After 3 buckets verify | sequential after B/C/D |

### ~~Bucket A — GAP-606 email template~~ — SKIPPED (already DONE)

Empirical: `find kitehub/kitehub-email -name "admin-new-login-alert.html"` returns `src/main/resources/templates/emails/admin-new-login-alert.html` (192 LOC). Shipped Wave 91 Bucket C PR #1486 commit ac2dd2f2 (`feat(wave-91 bucket C): admin-new-login-alert email template`). Gap CSV stale, flipped DONE in this refinement PR.

### Bucket B — GAP-608 SES IAM permission

- Files: `infrastructure/terraform-aws/iam.tf` — `kitehub-production-ec2-app` role gain `ses:SendEmail` action
- Empirical confirmed: `grep ses:SendEmail infrastructure/terraform-aws/iam.tf` → 0 hits = genuinely missing
- Acceptance: Terraform plan diff shows IAM policy update; code parallel ship (no AWS apply per GAP-612 suspension); follow-up gap `GAP-NEW-ses-iam-live-verify-post-restore` filed referencing GAP-612 unblock

### Bucket C — GAP-610 RLS TOKEN_NOT_FOUND (refined paths)

- Files (corrected):
  - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/service/BetaAccessService.java` — `validateToken(UUID token)` method line 531 (NOT `kitehub-platform/.../BetaSignupService.java` as original plan)
  - `BetaAccessRequestRepository.findByInviteToken(UUID token)` — verify repository query has no implicit tenant filter that excludes anonymous access
  - Flyway migration `V6X__fix-beta-access-rls.sql` nếu RLS policy update needed
- Pattern: Investigation phase first — read `validateToken` method full body (line 524-555+) để confirm 2 TOKEN_NOT_FOUND return paths (Optional empty vs lifecycle gating). Then debug RLS policy với `EXPLAIN ANALYZE` on prod-equivalent Postgres + verify `current_setting('app.current_tenant')` GUC set cho anonymous flow.
- Acceptance: GET `/api/v1/auth/beta-signup/validate?token={valid-token}` returns 200 với valid token (NOT `validate/{token}` path); local smoke test (live verify deferred GAP-612)
- Endpoint correction: `GET /api/v1/auth/beta-signup/validate?token=...` (RequestParam, NOT PathVariable per BetaAccessController line 101)

### Bucket D — GAP-611 gateway 404 (refined paths + endpoint)

- Files (corrected):
  - `kitehub/kitehub-frontend/src/components/auth/BetaSignupForm.tsx` — verify FE submit target URL (current call may use `/api/v1/beta-signup` missing `/auth/` prefix → cause 404)
  - `kitehub/kite-gateway/.../SecurityConfig.java` + `application*.yml` route mapping
  - `kitehub/kitehub-subscription/.../beta/controller/BetaAccessController.java` line 112 (`@PostMapping("/api/v1/auth/beta-signup")`)
- Pattern: First verify FE call URL matches BE route (`/api/v1/auth/beta-signup` with `/auth/` prefix); then check gateway public-endpoint matcher + Wave 89 JWT filter regression hypothesis
- Acceptance: POST `/api/v1/auth/beta-signup` returns 201 (not 404); curl smoke verify; FE form submit succeeds
- Endpoint correction: `POST /api/v1/auth/beta-signup` (with `/auth/` prefix per BetaAccessController line 112)

---

## 4. State-Check Evidence (refreshed 2026-05-25 Wave meta-3 follow-up session)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| GAP-606 | Gap file | `bash scripts/query-gaps.sh 606` | **DONE 2026-05-25** (state-check stale closure — template shipped Wave 91 PR #1486) | ✅ obsolete, Bucket A skipped |
| GAP-608 | Gap file | `bash scripts/query-gaps.sh 608` | OPEN P0 | ✅ accurate |
| GAP-610 | Gap file | `bash scripts/query-gaps.sh 610` | OPEN P0 (path/service-name in gap body needs refine post-investigation) | ⚠️ accurate but original gap body refs wrong module |
| GAP-611 | Gap file | `bash scripts/query-gaps.sh 611` | OPEN P0 (endpoint path in gap body refs wrong route) | ⚠️ accurate but original gap body refs wrong endpoint |
| `admin-new-login-alert.html` | Email template | `find kitehub/kitehub-email -name "admin-new-login-alert.html"` | **EXISTS** at `kitehub-email/src/main/resources/templates/emails/admin-new-login-alert.html` 192 LOC (Wave 91 PR #1486 ac2dd2f2) | ✅ exists — Bucket A NO-OP |
| `kitehub-production-ec2-app` IAM role | Terraform resource | `grep "kitehub-production-ec2-app" infrastructure/terraform-aws/iam.tf` | Role exists, but `grep ses:SendEmail iam.tf` = 0 hits | ✅ exists, missing permission confirmed |
| ~~`BetaSignupService`~~ → `BetaAccessService` | Java service | `find kitehub -name "BetaAccessService.java"` | EXISTS at `kitehub-subscription/src/main/java/com/kitehub/subscription/beta/service/BetaAccessService.java` (NOT kitehub-platform; Beta**Access** not Beta**Signup**) | ✅ correct path identified — see Bucket C refined |
| `BetaAccessController` | Java controller | `find kitehub -name "BetaAccessController.java"` | EXISTS at `kitehub-subscription/src/main/java/com/kitehub/subscription/beta/controller/BetaAccessController.java` line 101 GET validate + line 112 POST signup | ✅ correct path identified — see Bucket C/D refined |
| Endpoint route `/api/v1/auth/beta-signup` | Spring `@PostMapping` | grep `@PostMapping.*beta-signup` kitehub-subscription | EXISTS at BetaAccessController line 112 (`POST /api/v1/auth/beta-signup` với `/auth/` prefix) | ✅ correct route identified — Bucket D verify FE call URL match |
| `BetaSignupForm.tsx` | FE component | `find kitehub/kitehub-frontend -name "BetaSignupForm.tsx"` | EXISTS at `kitehub-frontend/src/components/auth/BetaSignupForm.tsx` | ✅ exists |
| `documents/01-business/beta-signup/api-contract.md` | API contract | `ls documents/01-business/beta-signup/api-contract.md` | (defer-verify next session pre-spawn) | ⚠️ pre-spawn check |

---

## 5. Verification Gates (refined 3-bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| ~~A~~ | — | SKIPPED (DONE Wave 91) |
| B | `cd infrastructure/terraform-aws && terraform plan -target=aws_iam_role_policy.app_role` + GAP-612 follow-up file | None (no apply pre-restore) |
| C | `cd kitehub && ./mvnw -pl kitehub-subscription verify -P strict-warnings -Dtest=BetaAccessServiceTest*` | kitehub-ci |
| D | `cd kitehub && ./mvnw -pl kite-gateway verify -P strict-warnings && cd kitehub-frontend && pnpm test BetaSignupForm` + curl POST /api/v1/auth/beta-signup | gateway-ci + kitehub-frontend-ci |
| E2E Closure | Manual smoke: anonymous lands → form → email → verify link → tenant provisioned | None (pre-prod env required, GAP-612 gated) |

---

## 6. Agent Spawn Pattern (refined 3-bucket)

3 agents parallel batch 1 (all Opus 4.7 1M per `agent-model-opus-default.md` v1.0.0 — Sonnet thrash recurrence ≥2 waves established):

```
Bucket B: subagent_type=general-purpose, model=opus, isolation=worktree, run_in_background=true
Bucket C: subagent_type=general-purpose, model=opus, isolation=worktree, run_in_background=true
Bucket D: subagent_type=general-purpose, model=opus, isolation=worktree, run_in_background=true

After 3 verify PASS:
  - Coordinator E2E smoke verify (local-only per GAP-612 AWS suspended)
  - File follow-up gap `GAP-NEW-ses-iam-live-verify-post-restore` (gated GAP-612)
  - 5-target sync + GAP-608/610/611 DONE flip (GAP-606 already DONE same-PR-as-refinement)
```

**Investigation phase first per `release-fix-retry-budget.md` §3.5:** Bucket C + D agents PHẢI empirical-read full validateToken method body + FE BetaSignupForm submit URL TRƯỚC khi propose fix. Wave meta-3 lesson recurrence applies: stale hypothesis = wasted retry cycles.

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

- **2026-05-25 (refined scope, Wave meta-3 follow-up session):** Pre-spawn state-check per `audit-to-gap-pipeline.md` §2.8 + `release-fix-retry-budget.md` §3.5 Investigation phase mandate caught 3 scope errors trong original draft:
  1. **Bucket A obsolete** — GAP-606 template ALREADY EXISTS Wave 91 PR #1486 (8d stale gap CSV) → flipped 🟢 DONE same PR → Bucket A removed from scope
  2. **Bucket C wrong module** — plan said `kitehub-platform/.../BetaSignupService.java`; actual `kitehub-subscription/.../beta/service/BetaAccessService.java` (Beta**Access** không Beta**Signup**) → corrected §3 + §4 paths
  3. **Bucket D wrong endpoint** — plan said `POST /api/v1/beta-signup`; actual `POST /api/v1/auth/beta-signup` với `/auth/` prefix → likely root cause 404 = FE call URL mismatch → corrected investigation pattern
  Refined scope: 3 buckets (B + C + D), 3 Opus 1M agents parallel. Saved ~1-2h wasted spawn on Bucket A obsolete + ~30min retry on wrong-path C/D. Wave meta-3 (this session) Investigation phase mandate retroactive precedent. Ready for next-session spawn execution.
- **2026-05-25 (status: draft):** Wave plan drafted per session handoff `2026-05-24-wave-beta-readiness-4-closure.md` §"Wave 3/5" pickup. Counter `beta-readiness-5` = next monotonic (1/2/3/4 đã consume). Highest leverage cho Phase 1 BETA → Phase 2 gate transition. Bucket B GAP-608 ship code-only — live verify blocked GAP-612 AWS suspension. Outside-in audit deferred (user-facing nhưng change = bug fix, không UX redesign). Author: @nguyenvankiet (solo-dev coordinator).
