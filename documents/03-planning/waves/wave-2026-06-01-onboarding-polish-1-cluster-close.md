---
title: Wave onboarding-polish-1 — Multi-tenant onboarding cluster close (6 gap GAP-534/535/536/538/599/610)
wave: 1
waves: [onboarding-polish-1]
tag_primary: onboarding-polish
tags_secondary: [phase-1-beta-gate, track-b]
counter: 1
created: 2026-06-01
date_launch: 2026-06-01
status: draft
---

# Wave onboarding-polish-1 — Multi-tenant onboarding cluster close

**Trigger:** `path-to-thesis-goal.md` §4 Track B Phase 1 BETA gate ≥80 — onboarding-polish cluster close 6 gap PARTIAL targeting BETA gate +5-10 score points. Sister wave email-finalize-1 (just shipped same session).

**Goal:** Close 6 multi-tenant onboarding gap với phần work AWS-free trong session này hoặc next; defer phần cần AWS deploy + live verify sang AWS-up session.

**State-check baseline (per audit `2026-06-01-wave-onboarding-polish-1-state-check.md`):**

| Gap | Pre-pct | AWS-free post-pct | AWS-required final pct |
|---|---|---|---|
| GAP-534 Invite token single-use | 90% | 90% (deploy-only) | 100% |
| GAP-535 Tenant slug normalize | 70% | 85% (wire normalizer) | 100% |
| GAP-536 Idempotency key | 65% | 80% (HandlerInterceptor) | 100% |
| GAP-538 Day-1 onboarding | 96% | 98% (VN seed) | 100% |
| GAP-599 JWT 2-tab storage | 90% | 93% (AC tick + docs) | 100% |
| GAP-610 Beta-signup validate | 75% | 85% (root cause) | 100% |
| **Aggregate** | **81%** | **~90%** | **100%** |

**Status:** DRAFT — execution defer next session per user direction state-check-only this session.

---

## 1. Brainstorm Q1

**Inside-out 3 buckets (per `inside-out-completeness-trigger.md`):**

- **ROADMAP §🚀:** `path-to-thesis-goal.md` §4 Track B — Wave onboarding-polish next in sequence after Wave email-finalize-1
- **inside-out-queue.md:** Track B Phase 1 BETA gate close-out cluster
- **State-check audit:** 6 gap canonical state empirical via CSV + per-gap AC scan — 4 of 6 có "wiring/interceptor/seed defer follow-up" notes = real code work
- **Outside-in:** SKIP per `outside-in-coverage-trigger.md` §4 (Wave 100 outside-in ≤30 ngày covers onboarding cluster; internal close-out scope)

**Q2 Risks:**
- AWS stack stopped → all 6 gap live verify defer next session
- GAP-535/536 wiring touches `InstanceService.createInstance` — production code path; need backend tests + careful review per `audit-service-isolation.md`
- GAP-610 root cause hypothesis 1/2/3 (RLS bypass / public endpoint / instances/payments 404 companion) — may surface deeper gaps
- GAP-534 may already DONE via Wave meta-8 PR #2007 catalog flip — verify before duplicating work

**Q3 Out-of-scope:**
- Wave thesis-2 NFR (AWS-blocked, separate critical path)
- Wave ops-mature (Track B parallel, separate wave)
- FE submit-button debounce (defer Bucket G after AWS bucket-F live verify)
- Companion gap GAP-611 instances/payments/revenue 404 (defer Bucket E sub-task if root cause identifies as same RLS)

---

## 2. Task Breakdown

| Bucket | Scope | Owner | Effort | AWS? |
|---|---|---|---|---|
| **A** | GAP-599 AC tick refresh + auth-storage.md + concurrent-browser-session mitigation note | coordinator-inline | ~30min | ❌ no |
| **B** | GAP-535 wire `TenantSlugNormalizer` vào `InstanceService.createInstance` + 10-retry loop + test | coordinator-inline | ~1h | ❌ no |
| **C** | GAP-536 `IdempotencyHandlerInterceptor` + wire POST `/api/platform/instances` + IT | coordinator-inline | ~1.5h | ❌ no |
| **D** | GAP-538 VN sample seed data (student/course names) + worker test | coordinator-inline | ~30min | ❌ no |
| **E** | GAP-610 root cause investigation (Testcontainers reproduce 3 hypotheses) | coordinator-inline | ~1h | ❌ no (test env) |
| **F** | All 6 gap live verify post-deploy (curl smoke + multi-tab DevTools inspect) | DEFER next session | ~2h | ✅ yes |
| **G** | FE submit-button debounce + UUID v4 idempotency-key generation (GAP-536 FE half) | DEFER next session | ~1h | ⚠️ partial (FE work, deploy verify needs AWS) |

Total AWS-free buckets: ~4.5h coordinator-inline (Bucket A-E). Plus ~3h AWS bucket (F+G) defer.

Realistic split: this session ship state-check + plan only (~30min) per user direction; Bucket A-E execute next session (cleaner context).

---

## 3. Scope

### Bucket A — GAP-599 AC refresh

Update `documents/04-quality/gaps/phase-1-beta/GAP-599-jwt-tab-collide-storage-isolation.md`:
- Tick 6 AC checkbox per Wave 92 Bucket B PR #1515 evidence (17 unit + 3 sim tests PASS jsdom isolated)
- Add Log entry "Wave onboarding-polish-1 Bucket A AC tick refresh"
- Create `documents/02-architecture/frontend/auth-storage.md` documenting sessionStorage facade

CSV: pct 90 → 93.

### Bucket B — GAP-535 normalizer wiring

Wire `TenantSlugNormalizer` vào `InstanceService.createInstance`:
- Read current InstanceService implementation
- Add normalizer call before persist + collision-recovery loop (10-retry → 409)
- Backend integration test: VN diacritic name → 201 + DB row slug normalized
- Unit test for collision-suffix path

CSV: pct 70 → 85.

### Bucket C — GAP-536 HandlerInterceptor wiring

Ship `IdempotencyHandlerInterceptor`:
- Read existing `IdempotencyService` API + `IdempotencyKey` entity
- Implement HandlerInterceptor: reads `Idempotency-Key` header → `findValidReplay()` → replay cached OR proceed
- Wire vào `WebMvcConfigurer` for POST `/api/platform/instances`
- IT: 2 sequential POSTs same key → 1 row + replay; mismatch hash → 422

CSV: pct 65 → 80.

### Bucket D — GAP-538 VN seed data

Update seed worker với VN-friendly sample data:
- Student names: "Nguyễn Văn An", "Trần Thị Hoa", "Lê Quang Minh", etc.
- Course names: "Toán 6", "Văn 7", "Anh văn nâng cao", etc.
- Tenant context appropriate (K-12 vs language center)
- Unit test seed output matches VN pattern

CSV: pct 96 → 98.

### Bucket E — GAP-610 root cause investigation

3 hypotheses per gap §Problem:
1. RLS bypass — `validate_token` query bypasses tenant RLS
2. Public endpoint missing — controller not registered or wrong path
3. Companion 404 — instances/payments/revenue same root cause

Testcontainers reproduce:
- Seed beta-signup token
- Direct repo query → expected row
- Service-layer call → expected row
- Controller layer call → maybe 404? = root cause located

Document finding + propose fix.

CSV: pct 75 → 85.

### Bucket F — AWS live verify (DEFER)

Per `pre-handoff-self-test-completeness.md` §2.x checklist for each gap. Requires AWS stack up + Flyway V39/V40/V41 applied + service deploy.

### Bucket G — FE work (DEFER)

`kitehub-frontend` submit-button debounce + UUID v4 generation cho POST forms touching idempotency-protected endpoints.

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Verify | Verdict |
|---|---|---|
| `documents/04-quality/gaps/gap-status.csv` | 640 rows valid | ✅ |
| 6 gap files exist | All under `phase-1-beta/` | ✅ |
| `TenantSlugNormalizer` class | Wave 77 Bucket D — `kitehub/kitehub-platform/src/main/java/...` | ⚠️ verify Bucket B |
| `InstanceService` class | Production code — Wave 77 era | ⚠️ verify Bucket B |
| `IdempotencyService` + `IdempotencyKey` entity | Wave 77 Bucket D | ⚠️ verify Bucket C |
| `IdempotencyHandlerInterceptor` | NEW | 🆕 Bucket C owns |
| seed worker class | Wave 98 B2 foundation | ⚠️ verify Bucket D |
| AWS stack state | EOD stopped per session collector | ❌ blocks Bucket F+G |
| GAP-612 unblock state | DONE 2026-05-26 per Wave aws-restore-1 | ✅ |
| Wave meta-8 PR #2007 | Pending merge — may flip GAP-534 DONE in catalog | ⚠️ verify post-merge |

---

## 5. Verification Gates (per bucket)

| Bucket | Gate |
|---|---|
| A | GAP-599 6 AC ticked + auth-storage.md created + CSV pct 90→93 + check-gap-status-csv PASS |
| B | InstanceService wires normalizer + IT PASS VN diacritic case + CSV 70→85 |
| C | HandlerInterceptor wired + IT 2-POST replay + CSV 65→80 |
| D | Seed worker VN data + unit test + CSV 96→98 |
| E | Root cause documented in gap file + CSV 75→85 |
| F | DEFER documented + 6 gap last_verified bumped pre-flip-DONE |
| G | DEFER documented |

## 6. Agent Spawn Pattern

Single coordinator-inline (NO parallel agents — same production code path InstanceService for Bucket B+C; sequential safer). Estimated ~4.5h AWS-free buckets coordinator-inline; defer to next session for cleaner context start.

## 7. Closure Protocol

Per `wave-closure-scope-completeness.md` v1.0.1 + `post-merge-sync-completeness.md`:
- Single coordinator-inline closure PR (5 AWS-free bucket + 2 DEFER documented trong same commit)
- 4-target post-merge sync (CSV + ROADMAP + wave-history + handoff)
- Scope-Completeness Reconciliation table trong closure PR body
- Wave plan frontmatter `status: complete`
- PR docs-only auto-merge eligible per `docs-only-pr-auto-merge.md` §2 (assuming only docs/code touched, no workflow)

---

## 8. Log

- **2026-06-01** (draft): Plan created via state-check audit Bucket A approach. State-check baseline (this session): 6 gap aggregate 81% → AWS-free advance possible ~90% → AWS-required final 100%. Execution defer next session per user direction "state-check audit this session, ship plan + audit only, ship execution next session". Outside-in audit SKIP per `outside-in-coverage-trigger.md` §4 (Wave 100 ≤30 ngày + internal cluster close-out). State-Check Evidence §4 verified — 9 symbols (4 ✅ + 4 ⚠️ verify-on-bucket + 1 🆕 NEW). Cross-layer NO. Single coordinator no parallel agents (sequential safer cho production code). Estimate ~30min docs ship this session + ~4.5h AWS-free buckets next session.
