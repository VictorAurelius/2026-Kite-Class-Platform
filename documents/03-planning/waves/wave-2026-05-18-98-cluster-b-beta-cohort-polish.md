---
title: Wave 98 — Cluster B Beta-Cohort Polish (closure run cho 6 PARTIAL gaps + 5 NEW outside-in gaps)
status: draft
created: 2026-05-18
updated: 2026-05-18
waves: [98]
gaps: [GAP-538, GAP-539, GAP-540, GAP-541, GAP-542, GAP-543, GAP-656, GAP-657, GAP-658, GAP-659, GAP-660]
---

# Wave 98 — Cluster B Beta-Cohort Polish

**Goal:** Finishing-stroke 6 PARTIAL beta-cohort gaps (538-543) bằng cách ship 5 NEW gaps (656-660) outside-in audit surfaced → Phase 1 BETA invite-ready state cho first cohort (≤5 tenants).

**Trigger:** Wave 97 closure shipped audit P0/P1 gate. 6 PARTIAL gaps Cluster B (538-543) đã ở 40-90% nhưng chưa close vì cross-cutting blocker (UI widget collision + email deliverability + VN cultural fit). Outside-in 3-agent audit (PR #1546) consolidated 5 NEW P0 gaps + 3 audit artifacts → unlock finishing strokes.

**Estimated wall-clock:** ~3-4 agent days (longest bucket B0 ~6h + B1 email ~8h; parallel buckets compress to ~1.5 day with 5 concurrent agents).

---

## 1. Brainstorm (per `inside-out-completeness-trigger.md` 4-source pull + `outside-in-coverage-trigger.md` synthesis)

### Q1 (alignment): which personas / domains / waves does this serve?

Wave 98 scope sources (per `inside-out-completeness-trigger.md` §3 mandatory 4-source pull):

**Inside-out — from ROADMAP §🚀 Next Action (canonical):**
- 6 PARTIAL P0 phase-1-beta gaps queued post-Wave-97 closure (GAP-538/539/540/541/542/543)

**Inside-out — from `inside-out-queue.md` (user-flagged beyond ROADMAP):**
- 2026-05-14 "Feedback channel" — consumed Wave 78 (GAP-542 PARTIAL 80%)
- 2026-05-14 "Email content audit 5 types VN" — consumed Wave 78 (GAP-543 PARTIAL 40%)
- 2026-05-14 "User manual VN screenshots-based" — partially consumed Wave 79 Bucket F1 anonymous-only; P2/P3 deferred Wave 80+ Bucket F2 (NOT Wave 98 scope per queue file)
- 2026-05-17 "Manual split professional vs end-user" — consumed Wave 92 Bucket D (NOT Wave 98 scope)

→ No NEW queue items relevant cho Wave 98 (queue items either already consumed or explicitly deferred).

**Inside-out — from gap-status.csv P0 phase-1-beta (catch ROADMAP miss):**
Cross-reference 23 OPEN + 27 PARTIAL P0 items. Cluster B (538-543) cluster largest cohort. Other clusters identified:
- AWS-blocked cluster (GAP-605/606/608/610/611) — defer post-GAP-612 restore
- Thesis cluster (GAP-646/647/648/649/650/651/652/653) — separate wave
- Documents/perf cluster (GAP-215/216/217/218) — defer Wave 99 candidate
- API contracts cluster (GAP-231/232/233) — defer Wave 99 candidate

→ Wave 98 = Cluster B beta-cohort polish only; disjoint từ other clusters.

**Outside-in — from 3-agent audit 2026-05-18 (PR #1546):**
3 audit artifacts consolidated 9 NEW gap candidates → 5 P0 filed (656-660):
- F-NEW-1/B-NEW-2 → GAP-660 Zalo OA fast-path
- F-NEW-2/F-NEW-4/M-NEW-7 → GAP-656 UI Coordinator (B0 PREREQ)
- F-NEW-3/M-NEW-15 → GAP-658 VN sample seed worker
- F-NEW-5/F-NEW-6/M-NEW-1/M-NEW-2/M-NEW-14 → GAP-657 + GAP-659 email layer + persona-tone
- F-NEW-7 → P3 role-guard live verify (B7, no new gap — extends GAP-518 PARTIAL 90%)

Outside-in items NEW not in inside-out scope: 5 gap files.
Outside-in items overlap reinforcing inside-out: GAP-538 (VN seed), GAP-540 (Zalo OA), GAP-542 (collision), GAP-543 (email content + tone).

### Q2 (trade-offs): what alternatives were considered and rejected?

| Alternative | Rejected because |
|---|---|
| Ship 6 PARTIAL Cluster B finishing strokes WITHOUT new gaps | Outside-in audit surfaced UI collision + email deliverability blockers that prevent meaningful close (GAP-540+542 collision physical block; GAP-543 deliverability silent churn) |
| Defer Cluster B entirely to Wave 99 + focus on docs/perf/API | Cluster B = beta-invite-blocking. Phase 1 BETA gate requires beta cohort live; can't ship invite without Cluster B closed |
| Mega-wave 8 buckets cluster B + Cluster C (perf) + Cluster D (API) | Too wide scope. 8 buckets ≤5 parallel agents per `feedback_parallel_agent_strategy.md` rule #9 — would force serial execution defeating wave-pack benefit |
| Sub-wave 3 buckets only (B0/B1/B7) | User accepted 8-bucket scope explicitly via AskUserQuestion 2026-05-18 |
| Skip B6 Zalo OA Phase 1.5 defer | Outside-in benchmark surfaced VN edu market expects Zalo OA Phase 1 BETA; defer = trust signal miss |

### Q3 (risks): what could go wrong; how does each bucket recover?

| Risk | Bucket affected | Recovery |
|---|---|---|
| B0 UI Coordinator scope creep — `useOnboardingPhase` hook complexity grows | B0 | Time-box: if >2h beyond estimate, simplify to "show banner-only first; defer staggered reveal to follow-up gap" |
| B1 Email layer — Resend HTTP API doesn't accept `List-Unsubscribe` header format | B1 | Fallback: SES-only routing for headers-requiring transactional; Resend reserved cho marketing tier later |
| B2 VN sample seed native copywriter unavailable | B2 + B7 | Use AI-generated VN content cho Wave 98; flag follow-up gap "native copywriter review pass Wave 99" |
| B6 Zalo OA business verification ≥2 weeks | B6 | Fast-path founder personal OA per gap §Step 1 alternative |
| B7 P3 role-guard verify gated AWS suspended | B7 | Per `pre-handoff-self-test-completeness.md` §5.4 — PARTIAL with `PRE_HANDOFF_PARTIAL: AWS-blocked` trailer + follow-up gap |
| 5 agents parallel — context overlap mid-wave | All | Each bucket worktree-isolated per `agent-background-spawn-default.md`; coordinator merges sequential |

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| **B0** | GAP-656 (NEW) | bg-agent | ~6h | ✅ kitehub-frontend hooks + components |
| **B1** | GAP-657 + GAP-659 (NEW) — paired email layer | bg-agent | ~8h | ✅ kitehub-email only |
| **B2** | GAP-658 (NEW) + extends GAP-538 | bg-agent | ~5h | ✅ kitehub-platform seed-worker |
| **B3** | GAP-539 finishing stroke | bg-agent | ~3h | ✅ kitehub-frontend banner |
| **B4** | GAP-541 finishing stroke | bg-agent | ~4h | ✅ kitehub-frontend i18n |
| **B5** | GAP-540 + GAP-542 (merged into SupportMenu) | bg-agent | ~4h | ❌ depends B0 |
| **B6** | GAP-660 (NEW) + extends GAP-540 | bg-agent | ~3h | ✅ DevOps + frontend independent |
| **B7** | GAP-518 P3 extension | bg-agent | ~2h | ✅ kitehub-admin + frontend |

**Disjoint check:** B0 + B5 sequential (B0 ships SupportMenu component → B5 wires GAP-540/542). All other buckets parallel-safe.

**Parallel cap per `feedback_parallel_agent_strategy.md` rule #9 (max 5 concurrent):**
- Wave 1: B0 + B1 + B2 + B3 + B4 (5 concurrent)
- Wave 2: B5 + B6 + B7 (3 concurrent, after B0 merges)

---

## 3. Scope (compact schema)

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** MEDIUM → model: Opus medium (default; Opus full reserved cho cross-cutting B0)
**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** NO — each bucket owns end-to-end slice; no separate FE/BE bucket per gap. B0 introduces 1 new endpoint `POST /api/preferences/dismiss-banner-state` but same agent owns both BE + FE same bucket → no Bucket 0 Foundation needed per `contract-first-for-cross-layer.md` §2 cross-layer-definition (FE+BE in SEPARATE buckets); single-bucket full-stack OK.

> **Gap referencing convention** (per `.claude/rules/gap-architecture-v2.md`): canonical CSV ids verified via `bash scripts/query-gaps.sh <prefix>` 2026-05-18.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **B0** UI Coordinator (PREREQ) | GAP-656 | 🔴 P0 | `kitehub/kitehub-frontend/src/hooks/`, `.../components/support/`, `.../components/onboarding/`, `.../playwright/`, `kitehub/kitehub-platform/.../PreferencesController.java` | Wave 1 parallel (blocks B5) |
| 2 | **B1** Email layer hardening | GAP-657 + GAP-659 | 🔴 P0 | `kitehub/kitehub-email/src/main/java/.../service/`, `.../resources/templates/emails/` | Wave 1 parallel |
| 3 | **B2** VN sample seed | GAP-658 (+ GAP-538 AC7 close) | 🔴 P0 | `kitehub/kitehub-platform/src/main/java/.../seed/`, `.../resources/seed-data/vn-friendly/` | Wave 1 parallel |
| 4 | **B3** Beta disclaimer banner close | GAP-539 (90% → 100%) | 🔴 P0 | `kitehub/kitehub-frontend/src/components/beta/BetaDisclaimerBanner.tsx`, `.../app/(dashboard)/layout.tsx`, `.../app/beta-status/page.tsx` | Wave 1 parallel |
| 5 | **B4** Vietnamese i18n close | GAP-541 (60% → 100%) | 🔴 P0 | `kitehub/kitehub-frontend/messages/vi/`, `kitehub/kitehub-frontend/src/app/legal/**`, `kitehub/kitehub-email/src/main/resources/templates/emails/*.html` | Wave 1 parallel |
| 6 | **B5** Support+Feedback widget merge | GAP-540 (80% → 100%) + GAP-542 (80% → 100%) | 🔴 P0 | `kitehub/kitehub-frontend/src/components/support/SupportMenu.tsx` (created B0), `.../feedback/FeedbackForm.tsx` | Wave 2 (after B0) |
| 7 | **B6** Zalo OA fast-path | GAP-660 (+ GAP-540 95% close) | 🔴 P0 | `kitehub/kitehub-frontend/src/components/support/`, `kitehub/kitehub-frontend/src/components/layout/Footer.tsx`, `kitehub/kitehub-email/.../templates/emails/footer.html` | Wave 1 parallel (or Wave 2) |
| 8 | **B7** P3 role-guard live verify | extends GAP-518 (90% → 95%) | 🟠 P1 | `kitehub/kitehub-admin/src/main/java/.../security/`, `kitehub/kitehub-frontend/src/middleware.ts`, `.../components/auth/RoleGuard.tsx` | Wave 1 parallel |

### Bucket B0 — UI Coordinator (PREREQ — blocks B5)

- Files: `kitehub/kitehub-frontend/src/hooks/useOnboardingPhase.ts` (🆕 create), `.../components/support/SupportMenu.tsx` (🆕 create), `.../components/onboarding/OnboardingCoordinator.tsx` (🆕 create), `kitehub/kitehub-platform/.../PreferencesController.java` (🆕 create), `kitehub/kitehub-frontend/playwright/onboarding-mobile.spec.ts` (🆕 create)
- Tests: Playwright spec mobile 375×812 + 360×640 viewports + Zalo WebView UA simulation
- Acceptance: GAP-656 AC §all checked
- Endpoint contract: `POST /api/preferences/dismiss-banner-state` documented inline trong `documents/01-business/kitehub/preferences/api-contract.md` (🆕 create)

### Bucket B1 — Email layer hardening + persona-tone

- Files: `kitehub/kitehub-email/src/main/java/.../service/SESEmailService.java`, `.../service/ResendEmailService.java`, `.../service/EmailTemplateRenderer.java`, `.../resources/templates/emails/{beta-invite,welcome,verify-email,password-reset,staff-invite}.{html,txt}`
- Tests: `SchedulerEmailWireIT.java` integration test + manual 2-client render verify (gmail.com + outlook.com)
- Acceptance: GAP-657 AC §all + GAP-659 AC §all checked
- API contract: `documents/01-business/kitehub/email/api-contract.md` (🆕 create — list 5 send endpoints + headers)

### Bucket B2 — VN sample seed worker

- Files: `kitehub/kitehub-platform/src/main/java/.../seed/VietnamSampleDataGenerator.java` (🆕 create), `.../seed/SeedWorkerService.java` (modify), `.../resources/seed-data/vn-friendly/*.csv` (6 CSV files: student/teacher/center/class/addresses/subjects)
- Tests: Unit tests cho `VietnamSampleDataGenerator` (random + locale)
- Acceptance: GAP-658 AC §all + GAP-538 AC7 checked

### Bucket B3 — Beta disclaimer banner close

- Files: `kitehub/kitehub-frontend/src/components/beta/BetaDisclaimerBanner.tsx` (mount on dashboard layout), `.../app/(dashboard)/layout.tsx` (wire banner), `.../app/beta-status/page.tsx` (freshness signal — "Cập nhật lần cuối: {ISO_DATE}")
- Tests: Banner render test + PDPL consent copy review
- Acceptance: GAP-539 AC §all + version chip `v0.9.0-beta` top-right + PDPL consent language added

### Bucket B4 — Vietnamese i18n close

- Files: `kitehub/kitehub-frontend/messages/vi/{common,legal,beta,email-preview}.json`, `kitehub/kitehub-frontend/src/app/legal/{terms,privacy}/page.tsx`, `kitehub/kitehub-email/src/main/resources/templates/emails/*.html` (Vietnamese narrative review)
- Tests: i18n key coverage check + missing-key warning (no raw `t('xxx')` fallback strings)
- Acceptance: GAP-541 AC §all + TOS + Privacy + 6 email templates all Vietnamese-narrative

### Bucket B5 — Support+Feedback widget merge (after B0)

- Files: `kitehub/kitehub-frontend/src/components/support/SupportMenu.tsx` (extend từ B0), `.../components/feedback/FeedbackForm.tsx` (🆕 create), `.../api/feedback-submit/route.ts` (🆕 create or extend), `kitehub/kitehub-platform/.../FeedbackController.java` (🆕 create or extend)
- Tests: SupportMenu dropdown integration test + FeedbackForm submit happy path
- Acceptance: GAP-540 §all + GAP-542 §all checked; standalone widgets removed (consolidated into SupportMenu)

### Bucket B6 — Zalo OA fast-path

- Files: Zalo OA setup (DevOps task per gap §Step 1), `kitehub/kitehub-frontend/src/components/support/SupportMenu.tsx` (add Zalo item), `.../components/layout/Footer.tsx` (add Zalo OA reference), `kitehub/kitehub-email/.../templates/emails/footer.html` (add Zalo OA QR + link)
- Tests: Mobile deep-link `zalo://chat?oa_id={oa_id}` + desktop web fallback + QR rendering
- Acceptance: GAP-660 AC §all + GAP-540 AC4 Zalo OA Phase 1 BETA checked

### Bucket B7 — P3 role-guard live verify

- Files: `kitehub/kitehub-admin/src/main/java/.../security/SecurityConfig.java` (verify CENTER_MANAGER role mapping), `kitehub/kitehub-frontend/src/middleware.ts` (verify role check), `.../components/auth/RoleGuard.tsx` (verify P3 access matrix)
- Tests: Integration test BE seed → JWT claim → FE guard accepts P3 (Mockito + Playwright)
- Acceptance: GAP-518 AC P3-role verify checked (P0 PLATFORM_ADMIN portion already 90%)
- Note: Live browser verify may stay PARTIAL pending GAP-612 AWS restore → use `PRE_HANDOFF_PARTIAL: AWS-blocked` trailer

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `useOnboardingPhase` | FE hook | `grep -rn "useOnboardingPhase" kitehub/kitehub-frontend/src` | 0 matches | 🆕 to-be-created (B0) |
| `SupportMenu` | FE component | `grep -rn "SupportMenu" kitehub/kitehub-frontend/src` | 0 matches | 🆕 to-be-created (B0) |
| `OnboardingCoordinator` | FE component | `grep -rn "OnboardingCoordinator" kitehub/kitehub-frontend/src` | 0 matches | 🆕 to-be-created (B0) |
| `PreferencesController` | BE controller | `find kitehub/kitehub-platform -name "PreferencesController.java"` | 0 matches | 🆕 to-be-created (B0) |
| `VietnamSampleDataGenerator` | BE service | `find kitehub/kitehub-platform -name "VietnamSampleDataGenerator.java"` | 0 matches | 🆕 to-be-created (B2) |
| `SchedulerEmailWireIT` | BE IT class | `find kitehub/kitehub-email -name "SchedulerEmailWireIT.java"` | 0 matches | 🆕 to-be-created (B1) |
| `SESEmailService.java` | BE service | `find kitehub/kitehub-email -name "SESEmailService.java"` | will verify in agent | ✅ exists (extends per B1) |
| `ResendEmailService.java` | BE service | `find kitehub/kitehub-email -name "ResendEmailService.java"` | will verify in agent | ✅ exists (extends per B1) |
| `BetaDisclaimerBanner` | FE component | `grep -rn "BetaDisclaimerBanner" kitehub/kitehub-frontend/src` | will verify in agent | ✅ exists 90% (extends per B3) |
| `staff-invite.html` template | BE template | `ls kitehub/kitehub-email/src/main/resources/templates/emails/staff-invite.html` | 0 (only `.txt` sibling exists) | 🆕 to-be-created (B1 GAP-659) |
| `Tone` enum | BE enum | `grep -rn "enum Tone" kitehub/kitehub-email/src/main/java` | 0 matches | 🆕 to-be-created (B1) |
| `documents/01-business/kitehub/email/api-contract.md` | API contract | `ls documents/01-business/kitehub/email/api-contract.md` | 0 (folder exists, file missing) | 🆕 to-be-created (B1) |
| `documents/01-business/kitehub/preferences/api-contract.md` | API contract | `ls documents/01-business/kitehub/preferences/` | folder doesn't exist | 🆕 to-be-created (B0) |
| GAP-538/539/540/541/542/543 | Gap CSV rows | `bash scripts/query-gaps.sh GAP-54 PARTIAL phase-1-beta` | 6 PARTIAL rows confirmed | ✅ canonical CSV verified |
| GAP-656/657/658/659/660 | Gap CSV rows | `bash scripts/query-gaps.sh GAP-65 OPEN phase-1-beta` | 5 OPEN rows confirmed (PR #1546 merged) | ✅ canonical CSV verified |

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| B0 | `pnpm -F kitehub-frontend test --run && pnpm -F kitehub-frontend build && pnpm -F kitehub-frontend playwright test onboarding-mobile && cd kitehub && ./mvnw -pl kitehub-platform verify -P strict-warnings` | frontend-ci + kitehub-ci |
| B1 | `cd kitehub && ./mvnw -pl kitehub-email verify -P strict-warnings` + manual 2-client render verify | kitehub-ci |
| B2 | `cd kitehub && ./mvnw -pl kitehub-platform verify -P strict-warnings` | kitehub-ci |
| B3 | `pnpm -F kitehub-frontend test --run && pnpm -F kitehub-frontend build` | frontend-ci |
| B4 | `pnpm -F kitehub-frontend test --run && pnpm -F kitehub-frontend lint` | frontend-ci |
| B5 | `pnpm -F kitehub-frontend test --run && cd kitehub && ./mvnw -pl kitehub-platform verify -P strict-warnings` | frontend-ci + kitehub-ci |
| B6 | `pnpm -F kitehub-frontend test --run && pnpm -F kitehub-frontend build` + manual Zalo deep-link verify mobile | frontend-ci |
| B7 | `cd kitehub && ./mvnw -pl kitehub-admin verify -P strict-warnings && pnpm -F kitehub-frontend test --run` | kitehub-ci + frontend-ci |

---

## 6. Agent Spawn Pattern

Per `agent-background-spawn-default.md` v1.0.1 + `feedback_parallel_agent_strategy.md`:
- All buckets spawned `run_in_background: true`
- Worktree isolation (`isolation: "worktree"`) for parallel safety
- RELATIVE paths in agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Wave 1 (5 concurrent): B0 + B1 + B2 + B3 + B4
- Wave 2 (3 concurrent, after B0 merges): B5 + B6 + B7
- Coordinator merges sequentially: B0 → B1+B2 (parallel) → B3+B4 (parallel) → B5 → B6 → B7

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `wave-closure-scope-completeness.md` v1.0.0 + `post-merge-sync-completeness.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:

- Each bucket PR updates affected GAP file Log + CSV row status + completion_pct
- ROADMAP §🚀 Next Action updated trong closure PR
- Wave plan frontmatter `status: complete` flip trong closure PR
- `wave-history.jsonl` append trong closure PR (Rule 15)
- **Scope-Completeness Reconciliation table per `wave-closure-scope-completeness.md` §3** — every §3 bucket categorized ✅/🟡/❌ + follow-up gap link
- Sub-gaps filed cho any deferral; PARTIAL exit-ramp per `gap-done-discipline.md` §3
- Run `bash scripts/prune-merged-worktrees.sh --yes` after all bucket PRs merged
- `## Release Plan Progress` section trong closure PR body per `feedback_wave_closure_release_progress_report.md`

**Post-wave audit suite cadence per `post-wave-audit-mandate.md` §2.2:** within 3 days post-Wave-98-closure — UI /128 (3-screen sample) + Quality /100 refresh.

---

## 8. Log

- **2026-05-18** (draft): Plan created. Triggered by Wave 97 closure + user accepted Cluster B 8-bucket scope after 3-agent outside-in audit (PR #1546 5 new gaps + 3 audit artifacts). Inside-out 4-source pull confirmed no queue items missed. Cross-layer = NO per single-bucket full-stack pattern. Reviewer: @nguyenvankiet (solo-dev).
