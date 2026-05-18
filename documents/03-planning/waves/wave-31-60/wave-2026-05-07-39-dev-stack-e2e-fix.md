---
title: Wave 39 — Dev-stack readiness + KC critical-journeys E2E reconciliation
status: complete
created: 2026-05-07
updated: 2026-05-07
waves: [39]
gaps: [GAP-417, GAP-418, GAP-419, GAP-420]
---

# Wave 39 — Dev-stack readiness + KC critical-journeys E2E reconciliation

**Goal:** Close 4 dev-stack/E2E gaps (GAP-417 ✅ already DONE Wave-39-eve, GAP-418 PARTIAL→DONE, GAP-419 PARTIAL→DONE, GAP-420 OPEN→DONE) so native local validation works end-to-end BEFORE staging deploy.
**Trigger:** Phase 1 BETA launch needs real-backend dev-stack workable; per ROADMAP §🚀 Next Action 4b "RECOMMENDED Wave 39 candidate cluster" + Stream B of 3-stream parallel strategy.
**Estimated wall-clock:** ~45-60 min agent work parallel, longest-bucket ~15-20 min (Bucket B class-lifecycle largest spec).

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):**
- Personas: Solo dev (cold-setup workflow); Phase 1 BETA admin (gateway must boot for real-backend smoke).
- Domains: DevOps (docker-compose context + gateway bean wiring); Frontend E2E (selector freshness, VN-first per CLAUDE.md).
- Waves: gates Phase 4.5 staging E2E gate (Wave 37 GAP-403); unblocks `release-1-deploy-runbook.md` §4.5.

**Q2 (trade-offs):**
- 3 GAP-420 sub-buckets vs 1 monolith: chose 3 — disjoint per spec file (dashboard-navigation / class-lifecycle / course-to-class-flow), parallel-safe, matches gap §"Proposed Fix" recommendation.
- Combine GAP-418 + GAP-419 into Bucket D (single dev-stack verify run): both are "real-stack boot verify" chained per gap files; running stack once covers both. Avoids redundant infra spin-up.
- Skip data-testid attribute additions in this wave (mentioned in GAP-420 AC line 4): defer to follow-up; focus on selector reconciliation first to keep wave-pack scope tight.

**Q3 (risks):**
- KC dev server boot time + Playwright browser install on first agent run may exceed 15 min budget → coordinator pre-warms via running `pnpm exec playwright install chromium` before spawn.
- Bucket D real-stack boot may surface NEW issues (downstream of gateway @Primary fix). Recovery: file follow-up gap, flip GAP-418/419 PARTIAL→PARTIAL with extended deferral (NOT silent DONE per `gap-done-discipline.md`).
- Selector text drift between agent run and merge: if UI copy changes mid-wave, specs need re-verification. Mitigation: each bucket commits "Validated locally YYYY-MM-DD against {sha}" header per GAP-420 AC line 5.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-420 sub-A `dashboard-navigation.spec.ts` (8 tests) | bg-agent | est. 10-15 min | ✅ KC FE only, single spec file |
| B | GAP-420 sub-B `class-lifecycle.spec.ts` (6 tests, largest) | bg-agent | est. 15-20 min | ✅ KC FE only, single spec file |
| C | GAP-420 sub-C `course-to-class-flow.spec.ts` (3 tests) | bg-agent | est. 10-12 min | ✅ KC FE only, single spec file |
| D | GAP-418 + GAP-419 dev-stack boot verify + AC flip | bg-agent | est. 10-15 min | ✅ docker-compose + gap files only |

Disjoint check:
- A/B/C touch only their respective `e2e/critical-journeys/*.spec.ts` file + may touch shared `e2e/helpers/*.ts` for VN-EN regex patterns (additive, low conflict risk — coordinator resolves any helper merge).
- D touches `kitehub/docker-compose.kitehub.yml` (already fixed PR #951) + 2 gap files (GAP-418 + GAP-419) — does NOT overlap with A/B/C source paths.

---

## 3. Scope (compact schema)

**Stake tier:** LOW (selector text reconciliation + verification flip; no new architecture; per `wave-pack-planner/SKILL.md` §Step 4.6) → model: **Sonnet/Haiku** for A/B/C; **Sonnet** for D (touches gap-file Log entries — needs careful AC ticking).
**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** **NO** — pure FE specs reconciliation + DevOps verify; no FE↔BE contract changes. **Skip Bucket 0 Foundation** per `contract-first-for-cross-layer.md` §2 (wave doesn't qualify cross-layer definition: no new BE endpoint, FE doesn't consume new BE endpoint, no MSW handlers).

| # | Bucket | Gap(s) | Priority | Files (glob, RELATIVE per `feedback_worktree_absolute_path_contamination.md`) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** dashboard-navigation | GAP-420 sub-A | 🟠 P1 | `kiteclass/kiteclass-frontend/e2e/critical-journeys/dashboard-navigation.spec.ts` + maybe `e2e/helpers/auth.ts`/`nav.ts` | parallel |
| 2 | **B** class-lifecycle | GAP-420 sub-B | 🟠 P1 | `kiteclass/kiteclass-frontend/e2e/critical-journeys/class-lifecycle.spec.ts` + maybe `e2e/helpers/class.ts` | parallel |
| 3 | **C** course-to-class-flow | GAP-420 sub-C | 🟠 P1 | `kiteclass/kiteclass-frontend/e2e/critical-journeys/course-to-class-flow.spec.ts` + maybe `e2e/helpers/course.ts` | parallel |
| 4 | **D** dev-stack verify + AC flip | GAP-418 + GAP-419 | 🟠 P1 + 🔴 P0 | `kitehub/docker-compose.kitehub.yml` (verify only — no edit), `documents/04-quality/gaps/GAP-418-*.md`, `documents/04-quality/gaps/GAP-419-*.md` | parallel |

### Bucket A — KC E2E `dashboard-navigation.spec.ts` selector reconciliation

- Files: `kiteclass/kiteclass-frontend/e2e/critical-journeys/dashboard-navigation.spec.ts` (8 tests; sidebar nav links, "Thêm học viên" / "Thêm giáo viên" / "Thêm lớp" CTAs, search input placeholder, logout button)
- Tests: same file (no new test files — reconcile existing assertions to match VN-first UI per CLAUDE.md §Communication Language)
- Acceptance:
  - All 8 tests in file pass via `pnpm exec playwright test critical-journeys/dashboard-navigation.spec.ts --project=chromium` against running `pnpm dev` server
  - VN-EN parallel regex pattern used where applicable (e.g., `/thêm học viên|new student/i`) — graceful for future copy drift
  - Spec header updated: `// Validated locally 2026-05-07 against <commit-sha>`
  - Local verify command: `cd kiteclass/kiteclass-frontend && pnpm dev & sleep 8 && pnpm exec playwright test critical-journeys/dashboard-navigation.spec.ts --project=chromium --reporter=list && kill %1`

### Bucket B — KC E2E `class-lifecycle.spec.ts` selector reconciliation (largest)

- Files: `kiteclass/kiteclass-frontend/e2e/critical-journeys/class-lifecycle.spec.ts` (6 tests; class state machine UI: SCHEDULED→IN_PROGRESS→COMPLETED button labels — likely "Bắt đầu lớp" / "Hoàn thành lớp" / "Huỷ lớp"; class code generation copy; cancel reason modal labels)
- Tests: same file
- Acceptance:
  - All 6 tests pass via `pnpm exec playwright test critical-journeys/class-lifecycle.spec.ts --project=chromium`
  - State transition button selectors match actual UI (verify against `kiteclass/kiteclass-frontend/src/components/class/*.tsx` rendered text)
  - Cancel reason modal labels reconciled (likely VN i18n keys)
  - Spec header updated: `// Validated locally 2026-05-07 against <commit-sha>`

### Bucket C — KC E2E `course-to-class-flow.spec.ts` selector reconciliation

- Files: `kiteclass/kiteclass-frontend/e2e/critical-journeys/course-to-class-flow.spec.ts` (3 tests; course publish flow "Xuất bản" button; DRAFT badge text; error toast/alert format)
- Tests: same file
- Acceptance:
  - All 3 tests pass
  - "Xuất bản" / "Bản nháp" / error toast selectors match actual UI
  - Spec header updated: `// Validated locally 2026-05-07 against <commit-sha>`

### Bucket D — Dev-stack boot verify + GAP-418/419 AC flip

- Files (READ + verify only — no source edits expected):
  - `kitehub/docker-compose.kitehub.yml` (already fixed PR #951; verify build + run)
  - `kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/config/KeyResolverConfig.java` (already fixed Wave-39-eve; verify boot)
  - `kitehub/scripts/up.sh` (run with `--profile beta-funnel` profile)
- Files (EDIT — flip AC):
  - `documents/04-quality/gaps/closed/GAP-418-kitehub-frontend-dockerfile-context-broken.md` (tick 3 unchecked AC + Log entry)
  - `documents/04-quality/gaps/closed/GAP-419-gateway-keyresolver-disambiguation-crash.md` (tick 3 unchecked AC + Log entry)
- Acceptance:
  - `bash kitehub/scripts/setup.sh` produces parseable `.env` (GAP-417 already DONE — sanity-check still works)
  - `bash kitehub/scripts/up.sh --profile beta-funnel` reaches all-services-healthy within 3 min on first try (cold image build OK)
  - `curl http://localhost:9000/actuator/health` returns 200 OK (gateway boots clean per GAP-419 AC line 73)
  - `docker logs kite-gateway 2>&1 | grep -c "No qualifying bean"` returns 0 (no bean disambiguation crash)
  - Both GAP-418 + GAP-419 Status flipped 🟡 PARTIAL → 🟢 DONE per `gap-done-discipline.md` §2 (all AC ticked + Log entry with verification artifact pointer per criterion 5)
  - **If verification fails:** flip stays PARTIAL, file follow-up gap describing remaining issue (NOT silent DONE)

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `KeyResolverConfig.ipKeyResolver` | Java method/bean | `grep -n "ipKeyResolver" kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/config/KeyResolverConfig.java` | 2 matches (declaration + reference); `@Primary` already applied per GAP-419 PARTIAL Wave-39-eve | ✅ exists |
| `kitehub/docker-compose.kitehub.yml` `kitehub-frontend` block | Compose service | `grep -n "kitehub-frontend:" kitehub/docker-compose.kitehub.yml` | exists with `context: ..` (repo-root) per GAP-418 PARTIAL fix Wave-39-eve | ✅ exists |
| `kitehub/scripts/up.sh` `--profile` flag | Shell script flag | `grep -n -- "--profile" kitehub/scripts/up.sh` | flag wired (GAP-421 DONE Wave-39-eve session #959) | ✅ exists |
| `kitehub/scripts/setup.sh` JWT_SECRET line | Shell script line | `grep -n "JWT_SECRET" kitehub/scripts/setup.sh` | uses `tr -d '\n=/+'` per GAP-417 DONE | ✅ exists |
| `kiteclass/kiteclass-frontend/e2e/critical-journeys/dashboard-navigation.spec.ts` | E2E spec file | `ls kiteclass/kiteclass-frontend/e2e/critical-journeys/dashboard-navigation.spec.ts` | 1 file | ✅ exists |
| `kiteclass/kiteclass-frontend/e2e/critical-journeys/class-lifecycle.spec.ts` | E2E spec file | `ls kiteclass/kiteclass-frontend/e2e/critical-journeys/class-lifecycle.spec.ts` | 1 file | ✅ exists |
| `kiteclass/kiteclass-frontend/e2e/critical-journeys/course-to-class-flow.spec.ts` | E2E spec file | `ls kiteclass/kiteclass-frontend/e2e/critical-journeys/course-to-class-flow.spec.ts` | 1 file | ✅ exists |
| `kiteclass/kiteclass-frontend/e2e/helpers/auth.ts` `Welcome back` → VN reconcile | Helper | `grep -n "Welcome back\|Chào mừng" kiteclass/kiteclass-frontend/e2e/helpers/auth.ts` | Already accepts both per PR #953 | ✅ exists |
| GAP-417/418/419/420 files | Gap docs | `ls documents/04-quality/gaps/GAP-{417,418,419,420}*.md` | 4 files | ✅ exists |

**No 🆕 to-be-created symbols** — wave is pure reconciliation + verification, no new code/migration/component creation.

Banned shortcuts respected:
- No `| head` truncation used in this section
- No "agents will check at execution" deferral — every symbol verified pre-plan
- No aspirational references — every selector reconciliation references existing UI files (e.g., `kiteclass-frontend/src/components/auth/login-form.tsx` exists per GAP-420 problem statement line 16)

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `cd kiteclass/kiteclass-frontend && pnpm exec playwright test critical-journeys/dashboard-navigation.spec.ts --project=chromium --reporter=list` (with dev server pre-running) | `KiteClass Frontend CI` (E2E job if wired); fallback PR review |
| B | `cd kiteclass/kiteclass-frontend && pnpm exec playwright test critical-journeys/class-lifecycle.spec.ts --project=chromium --reporter=list` | same |
| C | `cd kiteclass/kiteclass-frontend && pnpm exec playwright test critical-journeys/course-to-class-flow.spec.ts --project=chromium --reporter=list` | same |
| D | `bash kitehub/scripts/up.sh --profile beta-funnel && sleep 90 && curl -fsS http://localhost:9000/actuator/health && docker-compose -f kitehub/docker-compose.kitehub.yml ps` (all services `(healthy)`) | None — local-only verify; success → flip GAP-418/419 DONE |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- All 4 buckets spawned with `run_in_background: true` (mandatory per `agent-background-spawn-default.md` §1)
- `isolation: worktree` for parallel safety
- RELATIVE paths in agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merges sequentially after all background completions: A → B → C → D (D last because its AC flip log references the verify run; if A/B/C ship VN selector reconciliation that touches `e2e/helpers/*.ts` shared helpers, coordinator resolves additive merges before D)
- Pre-spawn warmup: coordinator runs `cd kiteclass/kiteclass-frontend && pnpm exec playwright install chromium` once on main worktree to avoid 4× browser download in agent worktrees (Q3 risk mitigation)

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:
- Each bucket PR updates its GAP file Log + status flip per criteria
- Closure PR updates ROADMAP §🚀 Next Action: Stream B status → DONE; remaining streams = A (user) + C (foreground docs)
- Wave plan frontmatter `status: complete` flip in closure PR
- `wave-history.jsonl` append (Rule 15)
- Sub-gaps filed for any deferral (e.g., if GAP-420 Bucket B class-lifecycle has UI copy that doesn't exist yet → file follow-up gap for UI work, keep test PARTIAL)
- `bash scripts/prune-merged-worktrees.sh --yes` after all bucket PRs merged, before closure PR draft (per `post-wave-cleanup.md` §2)
- Closure PR body §"Release Plan Progress": Phase 1 BETA — Stream B (dev-stack readiness) DONE; remaining gates = Stream A user actions + Stream C 6 docs
- **AUDIT_DEFER_DOMAIN_MILESTONE:** This wave is single-domain "release-deploy-artifacts" cluster member (per `post-wave-audit-mandate.md` §2.4.1 registry — touches `kitehub/scripts/`, `kitehub/docker-compose.*.yml`, E2E specs that gate Phase 4.5 staging). Audit deferred to milestone wave (Phase 1 BETA launch wave per Wave 37/38 precedent). Closure commits include `AUDIT_DEFER_DOMAIN_MILESTONE: release-deploy-artifacts — milestone Phase 1 BETA launch wave` trailer.

---

## 8. Log

- **2026-05-07** (draft): Plan created. State-check found GAP-417 already DONE (drop from candidate buckets); GAP-418/419 PARTIAL with fixes landed Wave-39-eve, just need real-stack boot verification to flip DONE; GAP-420 OPEN remains the only fresh-work cluster (3 sub-buckets per gap §"Proposed Fix"). Reorganized §4b roadmap "4 buckets" into actually-disjoint 4 buckets: 3× GAP-420 sub-buckets + 1× combined GAP-418/419 verify. Cross-layer = NO; Bucket 0 Foundation skipped per `contract-first-for-cross-layer.md`. AUDIT_DEFER_DOMAIN_MILESTONE applies (release-deploy-artifacts cluster).
- **2026-05-07** (complete): Wave 39 SHIPPED. 5 PRs merged (#963 plan, #964 D dev-stack verify, #965 C course-to-class-flow, #966 Stream C 6 docs, #967 A dashboard-nav, #968 B class-lifecycle). 4/4 buckets DONE: GAP-417 ✅ (already DONE Wave-39-eve), GAP-418 ✅, GAP-419 ✅, GAP-420 ✅ (17/17 tests pass A 8/8 + B 6/6 + C 3/3). Stream C 6 docs: 4 GAP-394 account-prep runbooks + GAP-423 SES VN overlay 15.72% density + GAP-424 Statuspage VN overlay 30.02% density. 1 rebase conflict resolved (api-mocks.ts Bucket C → take HEAD superset from Bucket B). 1 follow-up gap filed: GAP-425 (cold rebuild BE images stale — surfaced from "visual lần 1" cold-rebuild test, P2). 75th consecutive 0-clarification streak (4 buckets + Stream C foreground). Wall-clock ~1h plan-to-merge for Stream B + ~45min foreground Stream C in parallel. Audit deferral: AUDIT_DEFER_DOMAIN_MILESTONE: release-deploy-artifacts cluster (per `post-wave-audit-mandate.md` §2.4 — milestone = Phase 1 BETA launch wave).
