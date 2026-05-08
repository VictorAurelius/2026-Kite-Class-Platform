---
title: Wave 47 — E2E CI Activation (Phase A trial flag flip)
status: complete
outcome: phase-a-deferred-to-phase-b
created: 2026-05-08
updated: 2026-05-09
waves: [47]
gaps: []
followups: [GAP-453]
scope_correction: 2026-05-09 — original frontmatter listed [GAP-403, GAP-404, GAP-420] but those gaps cover TAG-TIME pre-release E2E (workflow `e2e-pre-release.yml`, DONE Wave 37). Wave 47 actual scope was PR-TIME E2E gates in `frontend-ci.yml`/`kitehub-frontend-ci.yml`. That work is now scoped under GAP-453.
---

# Wave 47 — E2E CI Activation (Phase A trial flag flip)

**Goal:** Bật E2E CI gate cho cả KH+KC frontends bằng cách flip `if: false` → `if: true` trong 2 workflows; verify route-mock setup hoạt động trong GHA runner mà KHÔNG cần stack-in-CI.
**Trigger:** Wave 47 candidate selection 2026-05-08 sau Wave 45 spawn; Explore agent recon ngày 2026-05-08 phát hiện E2E specs cả 2 frontends đã route-mocked qua Playwright `page.route()` — không thực sự cần backend stack despite comment.
**Estimated wall-clock:** ~30-45min (Phase A trial); fallback Phase B docker-compose +1-2h chỉ khi Phase A fail.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):** Phase 1 BETA pre-launch checklist + GAP-403 E2E pre-release gate. E2E activation gates Wave 47+ (catches regressions sớm cho mọi feature wave từ giờ trở đi). Force multiplier per `meta-gap-priority.md` §3 — meta gap (workflow infra) trump feature gaps cùng P-level.

**Q2 (trade-offs):**
- **Phase A trial vs full Option A docker-compose ngay:** Recon §3 cho thấy KC 17/17 + KH 5/5 specs pass locally **với route-mock only** (no backend). Comment `if: false` "require full stack" là stale từ trước khi specs route-mock. Trial flag flip = 30 phút; full docker-compose-in-CI = 1-2h. Trial first per "ship smallest reversible thing" principle.
- **Phase A failure recovery:** Nếu Phase A fail, KH `NEXT_PUBLIC_API_URL: localhost:9000` trong `test:e2e:ci` script (recon §1) là smoking gun cho real-API dependency hidden. Fall back Option A docker-compose-in-CI.
- **Skip Option B (AWS staging deploy):** Duplicate infra, AWS cost +$21/mo, không đáng cho E2E gate khi route-mock đủ.
- **Skip Option C (MSW migration):** Refactor 2-4h KC + 1h KH; route-mock đang work nên không cần thiết. Defer follow-up.

**Q3 (risks):**
- **Phase A flag flip → full CI red sau merge nếu specs fail trong runner:** Mitigation = chạy CI on PR branch trước khi merge, full visibility. Per `release-fix-retry-budget.md` §2 row "Different gate fails each retry" — nếu fail, mỗi retry = different gate fix (separate budget, không phải same-gate retry spiral).
- **Test flake do parallel/timing trong runner:** KC `fullyParallel: false` đã set; KH defaults to parallel — nếu KH flake → set `fullyParallel: false` trong Phase A bucket scope.
- **Vercel rate-limit blocking merge:** Wave 45 đang gặp; per `admin-merge-discipline.md` qualifies override trailer. Same playbook here.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-403/404/420 (Phase A trial flag flip) | bg-agent | est. 30min | ✅ 2 workflow files only |

Single-bucket wave — không cần Bucket 0 Foundation (no contract change, no FE+BE coupling).

---

## 3. Scope (compact schema)

**Stake tier:** **LOW** (mechanical flag flip, fully reversible; verify locally + CI before merge) → model: **Opus medium effort** (per `feedback_sonnet_baseline_context_thrash.md` — Sonnet thrash với nested rules).
**Cross-layer? NO** → skip Bucket 0 Foundation. Workflow YAML changes only; không touch FE/BE source.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-403/404/420 | 🟠 P1 | `.github/workflows/frontend-ci.yml` + `.github/workflows/kitehub-frontend-ci.yml` | single |

### Bucket A — Phase A trial flag flip + verify

- Files:
  - `.github/workflows/frontend-ci.yml:90` — flip `if: false` → `if: true` (KC E2E job)
  - `.github/workflows/kitehub-frontend-ci.yml:237` — flip `if: false` → `if: true` (KH E2E job)
  - Update comment lines for both — replace stale "require full stack" với "route-mocked specs, no backend dependency"
  - **NO docker-compose step added** — that's Phase B fallback only

- Pre-flight verify (REQUIRED by agent before flag flip):
  1. `cd kiteclass/kiteclass-frontend && pnpm test:e2e` → confirm 17/17 pass locally
  2. `cd kitehub/kitehub-frontend && pnpm test:e2e:ci` → confirm 5/5 beta-funnel pass locally (or whatever current count is)
  3. Read `kitehub-frontend/package.json` line 20 — verify `NEXT_PUBLIC_API_URL: http://localhost:9000` setting; if KH e2e scripts genuinely depend on Gateway running, abort Phase A trial + file Phase B sub-gap

- Acceptance:
  - [ ] Both workflows have `if: true` (or removed `if: false` line)
  - [ ] Comment updated reflecting route-mock reality
  - [ ] KC E2E CI job triggers on PR và pass
  - [ ] KH E2E CI job triggers on PR và pass
  - [ ] Total CI time per PR không tăng quá 5 phút (route-mock = no docker build needed)
  - [ ] Local verify documented trong PR body với pass count
  - [ ] Phase A trial result reported: ✅ green → close GAP-403/404/420; ❌ red → file Phase B sub-gap with specific failure mode (real-API call detected, runner timeout, flake, etc.)

- Phase B fallback trigger conditions (file sub-gap nếu hit):
  - Specs reference `localhost:9000` mà không có route-mock → real Gateway dependency
  - Runner timeout >20 phút consistently → need Playwright shard or browser optimization
  - Test flake rate >10% → need `fullyParallel: false` HOẶC stack-in-CI

---

## 4. State-Check Evidence (BẮT BUỘC)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `.github/workflows/frontend-ci.yml:90` `if: false` | YAML config | `grep -n "if: false" .github/workflows/frontend-ci.yml` | line 90 confirmed | ✅ exists |
| `.github/workflows/kitehub-frontend-ci.yml:237` `if: false` | YAML config | `grep -n "if: false" .github/workflows/kitehub-frontend-ci.yml` | line 237 confirmed | ✅ exists |
| KC critical-journeys 17 tests | E2E specs | `find kiteclass/kiteclass-frontend/e2e/critical-journeys -name "*.spec.ts" \| xargs grep -c "test("` | 3 files: dashboard-navigation 8 + class-lifecycle 6 + course-to-class-flow 3 = 17 | ✅ exists (recon §1) |
| KH beta-funnel 5 tests | E2E specs | `ls kitehub/kitehub-frontend/e2e/beta-funnel/*.spec.ts` | 3 files (request-flow + admin-approve + signup-with-claim-code) | ✅ exists (recon §1) |
| KC `playwright.config.ts` baseURL 4700 + fullyParallel:false | Config | `grep -n "baseURL\|fullyParallel" kiteclass/kiteclass-frontend/playwright.config.ts` | line 21 baseURL + line 14 fullyParallel | ✅ exists |
| KH `playwright.config.ts` baseURL 4701 | Config | `grep -n "baseURL" kitehub/kitehub-frontend/playwright.config.ts` | line 34 | ✅ exists |
| KH `test:e2e:ci` script `NEXT_PUBLIC_API_URL: localhost:9000` | package.json | `grep "test:e2e:ci" kitehub/kitehub-frontend/package.json` | line 20 | ⚠️ **risk indicator** — verify pre-flight (recon §1 footnote) |
| `kitehub/docker-compose.kitehub.yml` | Phase B fallback infra | `ls kitehub/docker-compose.kitehub.yml` | 737 lines, fully wired | ✅ exists (Phase B-only) |

Note: 1 ⚠️ row = pre-flight blocker check. Agent must verify KH e2e scripts' real API dependency BEFORE flag flip; nếu detected → abort Phase A + file Phase B sub-gap thay vì improvise.

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `cd kiteclass/kiteclass-frontend && pnpm test:e2e` (17/17 expected) + `cd kitehub/kitehub-frontend && pnpm test:e2e:ci` (5/5 expected) | frontend-ci E2E job + kitehub-frontend-ci E2E job (the very gates this wave activates) |

Phase A success criteria: PR's own E2E CI runs (the newly-activated gates) → green.
Phase A failure → revert flag flip in same PR + file Phase B sub-gap with failure mode evidence.

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- Single bucket → 1 background agent với `run_in_background: true`
- Worktree isolation (`isolation: worktree`)
- RELATIVE paths
- Coordinator merge sau agent completion
- Per `feedback_token_quota_spawn_timing.md` — spawn early; small scope nên không lo context bloat

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:
- Bucket A PR updates GAP-403/404/420 Log với "Phase A activated 2026-05-08" entry
- Pass count documented in PR body (e.g., "KC 17/17 + KH 5/5 in CI run #XXX")
- ROADMAP §🚀 Next Action updated
- Wave plan frontmatter `status: complete`
- `wave-history.jsonl` append (Rule 15)
- `bash scripts/prune-merged-worktrees.sh --yes` sau merge
- **`## Release Plan Progress` section** — Phase 1 BETA: E2E gate active = signal-to-noise tăng cho mọi wave subsequent; Waves Remaining table không thay đổi (E2E activation = quality multiplier, không phải MVP feature)
- Nếu Phase A failed → status `🟡 PARTIAL` + Phase B sub-gap filed; đừng flip 🟢 DONE prematurely per `gap-done-discipline.md` §2

---

## 8. Log

- **2026-05-09 (closure — Phase A DEFERRED to Phase B):** Wave 47 Bucket A agent ran pre-flight verify per plan §3 §4 → ABORT before any flag flip. Local verify on 2026-05-09 disproved plan recon at 4 points:
  1. **Scope mismatch** — plan §1.3 expected `pnpm test:e2e` (KC) + `pnpm test:e2e:ci` (KH) to run narrow critical-journeys/beta-funnel subsets; reality is they run ENTIRE `e2e/` folder (KC: 625 tests × 5 browser projects; KH: timeouts at 5min). Most specs depend on real backend.
  2. **Within KC critical-journeys:** chromium-only 11/17 pass (plan expected 17/17). 6 failures = `expect(page).toHaveURL('/dashboard')` after login form submit; login flow not route-mocked — only 2 `page.route` hits in entire `critical-journeys/` folder, all in `class-lifecycle.spec.ts`.
  3. **`fullyParallel` config error** — plan §4 row claimed KC `playwright.config.ts:14 fullyParallel:false`; reality is `fullyParallel: true` (line 9). 5 browser projects compound test count.
  4. **KC workflow has `|| true` + `continue-on-error: true`** (`frontend-ci.yml:117-118`) — flipping `if: false` would only add a non-blocking advisory step, semantically misleading vs the "activate gate" intent.

  **Outcome:**
  - PR #1072 created for the trial branch + closed without merge (per task spec §5 path)
  - Sub-gap GAP-453 filed (P1, 🔵 OPEN) with 3 Phase B options (B.1 docker-compose-in-CI / B.2 narrow subset / B.3 MSW migration); recommendation B.2 as smallest reversible step
  - **Scope-correction (coordinator-applied 2026-05-09):** original plan frontmatter `gaps: [GAP-403, GAP-404, GAP-420]` was misleading — those gaps cover TAG-TIME pre-release E2E (workflow `e2e-pre-release.yml`, DONE Wave 37 2026-05-07) and remain 🟢 DONE for their actual scope. Wave 47 actual scope was PR-TIME E2E gate activation in `frontend-ci.yml`/`kitehub-frontend-ci.yml`. Frontmatter `gaps:` corrected to empty + `followups: [GAP-453]` cited; GAP-453 IS the gap that scopes the PR-time activation work.
  - No workflow YAML touched on main; KC + KH E2E gates in `frontend-ci.yml`/`kitehub-frontend-ci.yml` remain `if: false`
  - Frontmatter `status: complete` + `outcome: phase-a-deferred-to-phase-b` — trial CONCLUDED with negative verdict; not partial-execution. Plan-recon-was-wrong is a complete trial outcome.
  - Agent kỷ luật tốt — surfaced ambiguity (4 plan recon errors documented in GAP-453) thay vì ép sai fix; matches `release-fix-retry-budget.md` §3 spirit (STOP at first signal, document, defer to redesign rather than patch).

  Coordinator closure: GAP-453 cherry-picked from worktree to main via closure PR; ROADMAP §🚀 Next Action updated; `wave-history.jsonl` appended (outcome=partial-deferred); worktree pruned.

- **2026-05-08 (draft):** Plan created. Wave 47 = single-bucket (renamed from Wave 46 due to number collision; java-deps-bump took Wave 46 slot) Phase A trial flag flip in 2 workflows. Recon Explore agent 2026-05-08 phát hiện specs đã route-mocked → trial-first thay vì full Option A docker-compose-in-CI ngay. Stake LOW (mechanical, reversible), Opus medium effort. Phase B fallback (docker-compose) chỉ trigger nếu Phase A fail với specific failure mode.
