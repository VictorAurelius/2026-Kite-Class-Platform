---
title: Wave 83 — Hot-fix Wave 82 post-deploy bugs + Launch Blockers (PDPL + Email production)
status: draft
created: 2026-05-15
phase: phase-1-beta
wave: 83
waves: [83]
risk_profile: MEDIUM (FE-deployed, hot-fix surface + legal deadline)
trigger: Wave 82 FE self-host CLOSED + post-deploy smoke surfaced 4 P0 hot-fix bugs; PDPL deadline 2026-07-01 cho cookie consent
estimated_wall_clock: 8-12h
---

# Wave 83 — Hot-fix Wave 82 post-deploy + Launch Blockers

## 1. Brainstorm

**Q1 (goal):** Resolve 4 P0/P1 hot-fix bugs surfaced Wave 82 post-deploy + 2 launch blockers (cookie consent PDPL + email production E2E) trước khi tiếp tục ops infra wave 84.

**Q2 (decision context):** Wave 82 ship được FE rebuild + DNS cutover + Wave 81 follow-up bucket F, NHƯNG post-deploy smoke surface 4 mới bugs (GAP-570/571 + beta-status 400 + gateway routing 404 GAP-481). 3/4 là controller exception handling drift; 1/4 là Spring Cloud Gateway config drift. Cùng wave là launch blockers GAP-558 (cookie consent PDPL Art 11 deadline 2026-07-01 ~6 tuần) + GAP-370 95% remaining 5% production E2E email smoke.

**Q3 (risks):**
- Validation 500 → user signup form đứng → BLOCK Phase 1 BETA invite
- Gateway 404 → một số routes không reachable từ public ALB → contract drift dev không phát hiện
- Cookie consent PDPL violation → legal risk cho launch public
- Email production NOT fully tested → welcome/verify/invite email could fail silently
- Outside-in audit per `outside-in-coverage-trigger.md` §4 — Wave 83 = bug-fix scope, không lock new user-facing scope, SKIP audit per §4 exception

## 2. Task Breakdown

| Bucket | Item | Owner | Effort | Sequential? |
|---|---|---|---|---|
| **A** | GAP-571 validation endpoints return 500 (beta-signup/validate + auth/verify-email) | coordinator | 2h | First (block user signup) |
| **B** | GAP-570 F5 fix incomplete — POST non-existent path STILL 500 post-deploy (gateway error wrapping?) | coordinator | 1h | Parallel A |
| **C** | beta-status 400 empty body fix (BetaStatusController investigation) | coordinator | 1h | Parallel A,B |
| **D** | GAP-481 Spring Cloud Gateway routing `/kitehub-subscription/*` 404 | coordinator | 1h | Parallel A,B,C |
| **E** | GAP-558 Cookie consent banner FE + PDPL Art 11 compliance | coordinator | 3-4h | After A-D |
| **F** | GAP-370 Email production E2E smoke (Resend production key configure + welcome/verify/invite delivery test) | user-action + coordinator verify | 2h | Parallel E |
| **G** | Post-fix audit suite + ROADMAP/CSV/handoff sync | coordinator | 1h | After E,F |

## 3. Scope — Bucket detail

### Bucket A — GAP-571 validation 500 → 400/401

- Files: `kitehub-subscription/src/main/java/com/kitehub/subscription/exception/GlobalExceptionHandler.java`
- Root cause: missing `@ExceptionHandler(ValidationException.class)` → falls through to Internal Server Error 500
- Fix: add handlers for `MethodArgumentNotValidException` (400) + `BadCredentialsException` (401) + `IllegalArgumentException` (400) — return RFC 7807 Problem Detail
- Test: integration tests cho beta-signup/validate + auth/verify-email với invalid inputs

### Bucket B — GAP-570 F5 incomplete

- Files: `kitehub-gateway/src/main/resources/application.yml` (gateway error filter)
- Root cause: `spring.web.resources.add-mappings=false` chỉ disable static resource serving trên backend services; gateway vẫn wrap 404 thành 500
- Fix: gateway-level error filter trả 404 cho không-existing routes; OR catch-all route → 404 response
- Test: `curl -X POST https://api.kitehub.me/api/v1/nonexistent` → 404 (không phải 500)

### Bucket C — beta-status 400

- Files: `kitehub-subscription/src/main/java/com/kitehub/subscription/betastatus/controller/BetaStatusController.java`
- Hypothesis: (a) Missing required `Accept-Language` header default; (b) Gateway routing predicate `/api/v1/admin/**` matches `/api/v1/beta-status` (admin route shadows beta-status); (c) Custom rate-limit filter not initialized production
- Fix: trace request via gateway logs → identify which layer returns 400 empty → fix at source
- Test: `curl https://api.kitehub.me/api/v1/beta-status` → 200 + JSON BetaStatusResponse

### Bucket D — GAP-481 gateway routing

- Files: `kitehub-gateway/src/main/resources/application.yml`
- Root cause: Spring Cloud Gateway route predicate `Path=/api/v1/subscription/**` thiếu service URI; route fall through default → 404
- Fix: verify all route predicates → service URIs; smoke test mọi mapped path
- Test: `curl https://api.kitehub.me/kitehub-subscription/health` → 200 (hoặc 404 nếu intentionally unmapped)

### Bucket E — GAP-558 Cookie consent (PDPL Art 11)

- Files: `kitehub-frontend/src/components/CookieConsentBanner.tsx` (NEW) + layout integration + cookie domain config
- Compliance: PDPL 2023 Art 11 + Decree 13/2023 Art 4 — explicit opt-in cho cookie analytics/marketing; strict-necessary auto-allow
- UI: bottom-fixed banner với 3 buttons (Accept all / Reject all / Customize); preferences page link
- BE: `kitehub-subscription` save consent state per user/session để audit trail
- Test: PDPL compliance checklist + accessibility (focus-trap + ARIA)

### Bucket F — GAP-370 Email production E2E

- USER ACTION: configure Resend API key trong AWS Secrets Manager `kitehub/production/resend-api-key`
- USER ACTION: verify DKIM + SPF + DMARC records propagated qua `dig +short`
- Coordinator: trigger smoke test welcome email → verify delivery + open tracking
- Coordinator: trigger invite email (P3 Manager flow) → end-to-end
- Coordinator: trigger 2FA verify email → arrives < 30s

### Bucket G — Closure audit + sync

- Post-wave audit suite per `post-wave-audit-mandate.md` §2.1 (Backend changes → Business Logic /100 + API Contract /100 + Security /100 v2 format)
- ROADMAP §🎯 Snapshot prepend Wave 83
- gap-status.csv 4 OPEN → DONE + 2 PARTIAL → DONE
- wave-history.jsonl append
- Session handoff `2026-05-XX-post-wave-83-handoff.md`

## 4. State-Check Evidence

| Symbol | Verification | Verdict |
|---|---|---|
| GAP-571 (validation 500) | `curl https://api.kitehub.me/api/v1/auth/verify-email -d '{}'` → 500 actual | ✅ exists (bug confirmed) |
| GAP-570 (POST non-existent 500) | `curl -X POST https://api.kitehub.me/api/v1/nonexistent` → 500 actual | ✅ exists |
| beta-status 400 | `curl https://api.kitehub.me/api/v1/beta-status` → 400 empty | ✅ exists (Wave 81 Bucket G spot check) |
| GAP-481 gateway routing | `curl https://api.kitehub.me/kitehub-subscription/*` → 404 | ✅ exists |
| GAP-558 cookie consent code | `find kitehub-frontend -iname "*cookie*"` → empty | ✅ to-be-created |
| GAP-370 Resend production key | `aws secretsmanager get-secret-value --secret-id kitehub/production/resend-api-key` → metadata | ⏳ verify Bucket F runtime |

## 5. Acceptance Gate

| Criterion | Met when |
|---|---|
| GAP-571 validation endpoints | 400/401 thay vì 500 cho invalid input |
| GAP-570 framework noise | POST non-existent path → 404 (không 500) |
| beta-status 200 | `GET /api/v1/beta-status` → 200 + BetaStatusResponse JSON |
| GAP-481 gateway routing | All Spring Cloud Gateway routes verified reachable + return correct status |
| GAP-558 cookie consent | PDPL compliance verified (3 button options + audit log) |
| GAP-370 email E2E | Welcome + invite + 2FA emails delivered < 30s via Resend production |
| Post-wave audit suite | Per §2.1 categories pass /80 minimum |

## 6. Cross-link

- Wave 82 closure: `wave-2026-05-15-82-fe-self-host.md`
- Wave 81 Bucket G spot check: `documents/04-quality/audits/pre-self-test/2026-05-15-wave-81-spot-check.md` (4 surface bugs)
- PDPL compliance: `documents/01-business/kitehub/legal/pdpl-compliance-checklist.md`
- Wave 83 closure handoff (post-ship): `documents/03-planning/session-handoffs/2026-05-XX-post-wave-83-handoff.md`

## 5. Verification Gates

See §5 Acceptance Gate table above — bucket-level criteria. Post-wave audit per `post-wave-audit-mandate.md` §2.1 (Backend/FE/Security/Performance categories) per bucket scope.

## 6. Agent Spawn Pattern

Sequential coordinator execution where buckets share files (deploy state, gateway config). Parallel background agents for isolated FE work (cookie consent banner, screenshots capture) per `agent-background-spawn-default.md` §1. Outside-in audit agents (per `outside-in-coverage-trigger.md` §3) spawn parallel background when wave triggers (Wave 85/86 mark §1 Q4).

## 7. Closure Protocol

Per `gap-done-discipline.md` + `post-wave-cleanup.md` + `post-merge-sync-completeness.md`:
- Wave plan frontmatter `status: complete` flip
- `wave-history.jsonl` append (Rule 15)
- ROADMAP §🎯 Snapshot prepend
- gap-status.csv sync per bucket DONE flips
- `bash scripts/prune-merged-worktrees.sh --yes` cleanup
- Session handoff `2026-05-XX-post-wave-NN-handoff.md` NEW
