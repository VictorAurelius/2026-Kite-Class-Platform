---
title: Wave 45 — Beta Access Closure (GAP-372 + GAP-370)
status: complete
created: 2026-05-08
updated: 2026-05-08
waves: [45]
gaps: [GAP-372, GAP-370]
---

# Wave 45 — Beta Access Closure (GAP-372 + GAP-370)

**Goal:** Close 2 P0 BLOCKING gaps cho Phase 1 BETA invite-only model — wire tenant provisioning vào `completeBetaSignup` + disable public `/register` toggle + verify SES production integration smoke path.
**Trigger:** Wave 45 candidate selection 2026-05-08; GAP-372 + GAP-370 đều PARTIAL từ Wave 33, là 2 P0 BLOCKING duy nhất còn lại trong Phase 1 BETA pre-launch checklist.
**Estimated wall-clock:** ~30-45min agent work, longest-bucket ~25min.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):** Phase 1 BETA invite-only soft launch (chốt 2026-05-06). Personas P1 (Solo teacher) + P2 (Small center owner) cần gate cứng:
- Public `/register` MUST disable → user thấy "Request Beta Access" thay vì self-signup
- `completeBetaSignup` redeem token MUST tạo tenant thực sự (hiện chỉ flip status SIGNED_UP)
- SES production approval MUST verified end-to-end (sandbox → out-of-sandbox)

**Q2 (trade-offs):**
- **Bundle với E2E activation?** Đã evaluate + reject (state-check 2026-05-08): E2E cần stack-in-CI infra, scope open-ended; tách Wave 46 riêng.
- **Auto-provision tenant trong `completeBetaSignup` vs delegate to controller?** Per existing javadoc line 420-424 — controller gọi registration pipeline AFTER `completeBetaSignup` returns success. Bucket A wire vào controller layer, không touch service (mỗi class một trách nhiệm).
- **FE register disable: feature-flag vs hard redirect?** Hard redirect tới `/auth/request-beta-access` đơn giản hơn + ship được trong scope wave; feature-flag = scope creep (config infra).
- **GAP-370 closure scope:** AWS SES sandbox→production approval là user-executed (per `release-deploy-standard.md` §9 "Deploy execution = ⚠️ Human-in-the-loop"). Bucket C scope = integration smoke test code path + runbook verification, không phải agent run AWS console.

**Q3 (risks):**
- **Bucket A (tenant provisioning):** chạm identity flow (subdomain reservation, owner creation, password hashing) — nếu break, signup chết. Mitigation: TDD, không touch existing `RegistrationService` core, chỉ wire endpoint chain từ controller.
- **Bucket B (FE disable):** rủi ro thấp; redirect logic. Mitigation: smoke test cả 2 frontends manual.
- **Bucket C (SES):** AWS sandbox limit (200 emails/day) — nếu integration test gửi nhiều email sẽ trigger throttle. Mitigation: 1 smoke email max + verify bounce/complaint webhook only.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| 0 | (Foundation light — verify api-contract.md no drift) | coordinator | est. 5min | ✅ docs only |
| A | GAP-372 closure #1 (BE tenant wire-up) | bg-agent | est. 25min | ✅ kitehub-subscription/beta/ + kitehub-platform/registration/ |
| B | GAP-372 closure #2 (FE register disable) | bg-agent | est. 15min | ✅ kitehub-frontend + kiteclass-frontend (auth)/register |
| C | GAP-370 closure (SES smoke test + runbook verify) | bg-agent | est. 20min | ✅ kitehub-email integration test + runbook doc |

Disjoint check: Bucket A touch BE service+controller; Bucket B touch FE pages; Bucket C touch separate kitehub-email module + runbook docs. Zero overlap.

---

## 3. Scope (compact schema)

**Stake tier:** **MEDIUM** → model: **Opus medium effort** (per `feedback_sonnet_parallel_agent_crash.md` — Sonnet rejected cho MEDIUM-stakes; Wave 33 đã ship 90% nên closure mechanical nhưng identity flow risk).
**Cross-layer? YES** → Bucket 0 Foundation per `contract-first-for-cross-layer.md` v1.0.0.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 0 | **Foundation (light)** | api-contract verify | 🟠 P1 | `documents/01-business/kitehub/beta-access/api-contract.md` (verify-only, no edits expected) | MERGE FIRST (or skip if no drift detected at coordinator pre-flight) |
| 1 | **A** | GAP-372 BE wire-up | 🔴 P0 | `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/controller/BetaAccessController.java` + `kitehub/kitehub-platform/src/main/java/...registration/` integration | parallel after Bucket 0 |
| 2 | **B** | GAP-372 FE disable | 🔴 P0 | `kitehub/kitehub-frontend/src/app/(auth)/register/page.tsx` + `kiteclass/kiteclass-frontend/src/app/(auth)/register/page.tsx` | parallel after Bucket 0 |
| 3 | **C** | GAP-370 SES smoke | 🔴 P0 | `kitehub/kitehub-email/src/test/java/...integration/` + `documents/05-guides/deploy/email-ses-setup-runbook.md` | parallel after Bucket 0 |

### Bucket 0 — Foundation (Contract verify, light)

Per `contract-first-for-cross-layer.md` v1.0.0 — Wave 45 không tạo endpoint mới (Wave 33 đã ship 6 REST endpoints + Wave 35 đã update consent contract). Coordinator pre-flight verify api-contract.md không drift; nếu pass → Bucket 0 trivial commit (touch timestamp `Last verified`); nếu drift detected → expand thành full Bucket 0.

- Files: `documents/01-business/kitehub/beta-access/api-contract.md`
- Acceptance: `Last verified: 2026-05-08 (Wave 45)` line updated; no schema changes
- Spawn order: MERGE FIRST trước A/B/C parallel

### Bucket A — BE tenant provisioning wire-up (GAP-372 closure #1)

- Files: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/controller/BetaAccessController.java` (modify `completeBetaSignup` endpoint handler) + integration với `kitehub-platform` registration service
- Tests: `BetaAccessControllerTest` — 1 happy-path IT verify tenant created sau redeem token + 2 failure-path (token invalid, registration fails → rollback)
- Acceptance:
  - [ ] `POST /api/v1/auth/beta-signup` redeem token → trigger tenant registration pipeline (subdomain reservation, owner-user creation, password hashing) → return signup success
  - [ ] Failure trong registration pipeline → rollback `BetaAccessRequest` status từ SIGNED_UP back to APPROVED (token vẫn dùng được lần nữa)
  - [ ] Endpoint contract match `documents/01-business/kitehub/beta-access/api-contract.md` schema (POST /auth/beta-signup)
  - [ ] Local verify: `cd kitehub && ./mvnw -pl kitehub-subscription verify -P strict-warnings`
- (Cross-layer BE bucket): Controller signature unchanged (matches existing api-contract.md); internal wiring only

### Bucket B — FE public /register disable (GAP-372 closure #2)

- Files:
  - `kitehub/kitehub-frontend/src/app/(auth)/register/page.tsx` (replace form với redirect to `/auth/request-beta-access`)
  - `kiteclass/kiteclass-frontend/src/app/(auth)/register/page.tsx` (same)
- Tests: 1 React Testing Library test mỗi frontend verify redirect rendered (or `next/navigation` redirect called)
- Acceptance:
  - [ ] Cả 2 `/register` pages render redirect notice + link tới `/auth/request-beta-access` (hoặc auto-redirect via `next/navigation`)
  - [ ] Existing `/register/student` (KC) KHÔNG affect — chỉ owner self-signup gốc bị disable
  - [ ] Local verify: `pnpm -F kitehub-frontend test --run register && pnpm -F kitehub-frontend build` + same cho KC frontend
- (Cross-layer FE bucket): Endpoint consumption unchanged (FE chỉ redirect, không call API)

### Bucket C — GAP-370 SES smoke test + runbook verify

- Files:
  - `kitehub/kitehub-email/src/test/java/com/kitehub/email/integration/SesIntegrationSmokeTest.java` (NEW — gated profile `aws-ses-real`, default skip)
  - `documents/05-guides/deploy/email-ses-setup-runbook.md` (verify each step current; add "Wave 45 verification 2026-05-08" log section)
- Tests: smoke test sends 1 templated email via real SES client, verifies HTTP 200 + `MessageId` returned (skip default; manual run via Maven profile khi user đã setup SES production)
- Acceptance:
  - [ ] `SesIntegrationSmokeTest` exists, profile-gated, skip in default `mvn verify`
  - [ ] Runbook verified accurate: 7-step SES setup (domain verify → DKIM → DMARC → sandbox request → production approval → kitehub-email config → smoke test) — each step có "Wave 45 verified" check
  - [ ] Local verify: `cd kitehub && ./mvnw -pl kitehub-email verify -P strict-warnings` (smoke test should be SKIPPED, not failed)
- (Cross-layer): N/A — backend integration only

---

## 4. State-Check Evidence (BẮT BUỘC)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `BetaAccessService.completeBetaSignup` | Java method | `grep -rn "completeBetaSignup" kitehub/kitehub-subscription/src/main/java` | found at `BetaAccessService.java:427` (current impl flips status to SIGNED_UP, javadoc line 420-424 explicit về wire-up follow-up) | ✅ exists |
| `BetaAccessController` | Java class | `grep -rn "BetaAccessController" kitehub/kitehub-subscription/src/main/java` | `BetaAccessController.java` — 6 REST endpoints shipped Wave 33 | ✅ exists |
| `BetaAccessRequest.setStatus(SIGNED_UP)` | State transition | (already verified line 439 above) | line 439 in service | ✅ exists |
| `kitehub-platform` registration service | Java module | `find kitehub/kitehub-platform/src/main/java -path "*registration*" -name "*.java"` | (Bucket A pre-flight task — agent verifies + cites class name) | 🟡 to-be-verified-by-agent |
| `kitehub-frontend/(auth)/register/page.tsx` | Next.js page | `ls kitehub/kitehub-frontend/src/app/\(auth\)/register/page.tsx` | 1 file exists | ✅ exists |
| `kiteclass-frontend/(auth)/register/page.tsx` | Next.js page | `ls kiteclass/kiteclass-frontend/src/app/\(auth\)/register/page.tsx` | 1 file exists | ✅ exists |
| `kiteclass-frontend/(auth)/register/student/page.tsx` | Next.js page (out-of-scope, MUST NOT touch) | `ls kiteclass/kiteclass-frontend/src/app/\(auth\)/register/student/page.tsx` | 1 file exists | ✅ exists (preserve) |
| `documents/01-business/kitehub/beta-access/api-contract.md` | API contract doc | `ls documents/01-business/kitehub/beta-access/api-contract.md` | exists, "Last verified: 2026-05-08 (Wave 35 Bucket 0 Foundation)" | ✅ exists |
| `documents/05-guides/deploy/email-ses-setup-runbook.md` | Runbook | `ls documents/05-guides/deploy/email-ses-setup-runbook.md` | (Bucket C pre-flight — agent verifies path) | 🟡 to-be-verified-by-agent |
| `kitehub-email` module integration test path | Test directory | `ls kitehub/kitehub-email/src/test/java/com/kitehub/email/` | (Bucket C pre-flight) | 🟡 to-be-verified-by-agent |
| `BetaAccessRequestStatus.APPROVED` rollback target | Enum value | (verified Wave 33 — entity has APPROVED state) | from existing `BetaAccessRequestStatus.java` | ✅ exists |
| `SesIntegrationSmokeTest` | Test class | (Bucket C creates) | 0 matches | 🆕 to-be-created (Bucket C) |

Note: 3 rows marked 🟡 to-be-verified-by-agent vì pre-flight cost (full grep tree of kitehub-platform/registration + email module) cao tại plan time; agent verify trước khi apply changes per `audit-to-gap-pipeline.md` §2.5 hardened protocol. Agent nào không verify được + symbol absent → file sub-gap thay vì proceed.

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| 0 | (manual — coordinator inspects api-contract.md diff) | docs-only path |
| A | `cd kitehub && ./mvnw -pl kitehub-subscription verify -P strict-warnings` | kitehub-ci |
| B | `pnpm -F kitehub-frontend test --run register && pnpm -F kitehub-frontend build && pnpm -F kiteclass-frontend test --run register && pnpm -F kiteclass-frontend build` | frontend-ci + kitehub-frontend-ci |
| C | `cd kitehub && ./mvnw -pl kitehub-email verify -P strict-warnings` (smoke test SKIPPED expected) | kitehub-ci |

Per `feedback_admin_merge_bypass_test_compile.md` — KHÔNG `--admin` merge sau force-push trừ khi local verify ran clean trên rebased HEAD.

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- 3 buckets (A/B/C) spawn parallel với `run_in_background: true` sau khi Bucket 0 merge
- Worktree isolation (`isolation: worktree`)
- RELATIVE paths trong agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merge sequential A→B→C sau all background completions
- Per `feedback_token_quota_spawn_timing.md` — spawn EARLY trong session (trước context coordinator >150k); current ~30k OK

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:
- Each bucket PR updates GAP file Log + status (GAP-372 + GAP-370 — final closure flips to 🟢 DONE chỉ khi AC checked, không banned phrase)
- ROADMAP §🚀 Next Action updated
- Wave plan frontmatter `status: complete` flip
- `wave-history.jsonl` append (Rule 15)
- `bash scripts/prune-merged-worktrees.sh --yes` sau all bucket PRs merged
- **`## Release Plan Progress` section** — Phase 1 BETA progression: GAP-372 + GAP-370 đóng → BETA pre-launch checklist countdown; Waves Remaining table cập nhật

---

## 8. Log

- **2026-05-08 (draft):** Plan created.
- **2026-05-08 (complete):** Wave SHIPPED. PRs merged sequential A→B→C: #1051 (Bucket A BE tenant wire-up — wired `AuthService.registerFromBetaInvite` into `BetaAccessController.completeBetaSignup` with conflict-rollback), #1052 (Bucket B FE register disable — BetaInviteOnlyNotice card pattern KH+KC, coordinator-applied finalization sau agent terminated mid-build), #1050 (Bucket C SES smoke test profile-gated + Wave 45 runbook verify). Bucket 0 Foundation skipped (api-contract.md no drift). All 3 PRs merged với `ADMIN_MERGE_OVERRIDE: Vercel rate-limit external 24h block` trailer per `admin-merge-discipline.md` §2 (Vercel rate-limited 24h = qualified override). GAP-372 → 🟢 DONE (10/10 ACs); GAP-370 → 🟡 PARTIAL (code complete, AWS SES production approval = user-executed). Wall-clock ~75min spawn → close (vs 30-45min plan estimate; +30min coordinator-applied Bucket B finalization + cleanup recovery from premature prune incident). 81st 0-clarification streak. Wave 45 = GAP-372 closure (3 follow-ups) + GAP-370 closure (SES smoke test + runbook verify). Cross-layer YES, Bucket 0 light (api-contract no drift expected). Stake MEDIUM, Opus medium effort. Estimated 30-45min wall-clock, longest bucket ~25min (Bucket A BE wire-up).
