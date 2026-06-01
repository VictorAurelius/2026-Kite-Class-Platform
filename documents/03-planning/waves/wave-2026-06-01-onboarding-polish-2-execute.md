---
title: Wave onboarding-polish-2-execute — Ship 3 deferred buckets C+D+E + META punt Bucket B
wave: 2
waves: [onboarding-polish-2]
tag_primary: onboarding-polish
tags_secondary: [phase-1-beta-gate, track-b]
counter: 2
created: 2026-06-01
date_launch: 2026-06-01
status: complete
---

# Wave onboarding-polish-2-execute — Ship C+D+E + META Punt B

**Trigger:** Wave onboarding-polish-1 closure (2026-06-01) shipped 1/5 bucket + deferred 4 với explicit triad-drift findings. User direction this session: execute C+D+E, punt Bucket B (GAP-535) to META wave because state-check surfaced architectural drift larger than original plan estimated.

**Goal:** Ship 3 AWS-free buckets (Idempotency interceptor + VN seed pivot + GAP-610 root cause investigation) + file GAP-823 META cho Bucket B triad reconstruction. Defer all live AWS verify to follow-up wave per `local-self-test-before-aws-deploy.md` §5 (AWS stack stopped).

**Status:** planning — bucket spawn pending plan PR merge.

---

## 1. Brainstorm Q1

**Inside-out 3 sources (per `inside-out-completeness-trigger.md`):**

1. **ROADMAP §🚀:** continuation of Track B Phase 1 BETA gate close-out
2. **inside-out-queue.md:** Track B onboarding cluster (consumed by Wave onboarding-polish-1, residual = this wave)
3. **AskUserQuestion explicit (this session):** user chose "C+D+E only, punt B to META wave" after state-check surfaced triad drift

**Outside-in:** SKIP per `outside-in-coverage-trigger.md` §4 (Wave 100 outside-in ≤30 ngày covers onboarding cluster + this is internal close-out).

**Q2 Risks:**
- **Bucket D scope mismatch:** Wave onboarding-polish-1 plan §3.4 claimed VN sample names go into "seed worker"; state-check shows `ProductionSeedRunner` only seeds admin user + system_config — không seed students/courses. Real VN-data target likely `BrandingDataSeeder` (kiteclass-core) OR script-based `kitehub/scripts/seed-data.sh`. → Bucket D includes state-check sub-step before code edit.
- **Bucket C `MigrationIdempotencyKeyService` parallel API:** existing `TrialToPaidService` uses different idempotency service. New HandlerInterceptor must use `IdempotencyService` (the canonical one); not conflict but reviewer must verify wiring.
- **Bucket E investigation may surface deeper bug:** 3 hypotheses (RLS bypass / public endpoint missing / companion 404) each leads to different fix scope; investigation deliverable only — fix wave defers.
- All 3 buckets touch different files → safe for parallel agents.

**Q3 Out-of-scope:**
- **Bucket B (GAP-535) triad reconstruction** — punted to GAP-823 META wave (this PR files GAP-823 to track)
- Wave thesis-2 NFR (AWS-blocked separate critical path)
- FE submit-button debounce (defer post-AWS-restore)
- Companion GAP-611 instances/payments/revenue 404 (may surface in Bucket E findings)

---

## 2. Task Breakdown

| Bucket | Scope | Owner | Effort | AWS? | Parallelizable? |
|---|---|---|---|---|---|
| **C** | GAP-536 IdempotencyHandlerInterceptor + WebMvcConfig wire + IT | Opus 4.7 bg-agent (worktree-isolated) | ~1.5h | ❌ no | ✅ yes |
| **D** | GAP-538 VN seed — state-check (10min) + ship (~30min) | Opus 4.7 bg-agent | ~45min | ❌ no | ✅ yes |
| **E** | GAP-610 root cause investigation — Testcontainers reproduce 3 hypotheses | Opus 4.7 bg-agent | ~1h | ❌ no (test env) | ✅ yes |
| **B-META-punt** | File GAP-823 META gap documenting triad drift (inline, coordinator) | coordinator-inline | ~30min | ❌ no | sequential |
| **F-defer** | AWS live verify (3 buckets) — defer post-AWS-restore | DEFER next wave | ~2h | ✅ yes | n/a |

3 parallel Opus agents (~1.5h wall-clock max bucket) + coordinator inline (~30min file gap + draft closure) = **~2h total wall-clock**, vs ~3.25h serial.

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Verify cmd | Verdict |
|---|---|---|
| `IdempotencyService` API stable | grep `findValidReplay\|cacheResponse\|hashRequest` | ✅ 3 public methods canonical |
| `IdempotencyKeyRepository` exists | find | ✅ exists |
| `WebMvcConfig` exists để wire interceptor | find | ✅ exists `kitehub-subscription/.../config/WebMvcConfig.java` |
| HandlerInterceptor pattern proven | grep `implements HandlerInterceptor` | ✅ 2 existing (`AdminApiKeyInterceptor`, `MagicLinkCacheControlInterceptor`) |
| `IdempotencyHandlerInterceptor` (new) | find | 🆕 Bucket C owns |
| `ProductionSeedRunner` scope | read | ⚠️ Seeds admin user + system_config ONLY — không match GAP-538 "VN sample student/course names" scope |
| `BrandingDataSeeder` (alternate target) | find | ⚠️ exists `kiteclass/kiteclass-core/.../dev/seeder/BrandingDataSeeder.java` — Bucket D needs read trước commit |
| `GAP-610` gap file | ls | ✅ `documents/04-quality/gaps/phase-1-beta/GAP-610-validate-token-returns-not-found-for-valid-token.md` |
| Bucket B triad drift confirmed | this PR §3 below | 🔴 Punted GAP-823 META wave |

---

## 3. Scope (Brainstorm + Tasks → concrete buckets)

### Bucket C — GAP-536 IdempotencyHandlerInterceptor (~1.5h, AWS-free)

**Owner:** Opus 4.7 background agent (worktree-isolated)
**Files:**
- NEW `kitehub-subscription/src/main/java/com/kitehub/subscription/idempotency/interceptor/IdempotencyHandlerInterceptor.java`
- EDIT `kitehub-subscription/.../config/WebMvcConfig.java` — register interceptor on POST `/api/platform/instances`
- NEW IT `kitehub-subscription/src/test/java/com/kitehub/subscription/idempotency/IdempotencyInterceptorIT.java`

**AC:**
- [ ] HandlerInterceptor reads `Idempotency-Key` header → `IdempotencyService.findValidReplay()` → replay HTTP 200 cached body OR proceed
- [ ] Wire vào `WebMvcConfig` for POST `/api/platform/instances` path
- [ ] IT: 2 sequential POSTs same key + same body hash → 1 row in `idempotency_keys` + replay response
- [ ] IT: mismatch request hash → HTTP 422 + no row mutation
- [ ] GAP-536 file Status: PARTIAL→DONE (pct 65→80; live verify defer Bucket F)
- [ ] CSV row updated

### Bucket D — GAP-538 VN seed (state-check + ship, ~45min, AWS-free)

**Owner:** Opus 4.7 background agent (worktree-isolated)
**Step 1 — state-check (10min):**
- Read `BrandingDataSeeder.java` (kiteclass-core) — confirm if it seeds student/course names
- Read `kitehub/scripts/seed-data.sh` — confirm if it has VN sample data target
- Identify CANONICAL target for VN names per GAP-538 actual scope

**Step 2 — ship (~30min):**
- Update identified seeder với VN sample names:
  - Students: "Nguyễn Văn An", "Trần Thị Hoa", "Lê Quang Minh", "Phạm Thị Mai", "Hoàng Văn Tùng"
  - Courses: "Toán 6", "Văn 7", "Anh văn nâng cao", "Khoa học tự nhiên"
- Unit test verify seed output matches VN pattern
- GAP-538 file Status updated với chosen target + Log entry

**AC:**
- [ ] State-check finding documented (canonical seeder identified)
- [ ] VN sample names committed (Vietnamese narrative per `dev-readable-doc-language.md` + VN-localization per `vn-localization-audit-checklist.md`)
- [ ] Unit test PASS
- [ ] GAP-538 pct 96→98 + CSV updated

### Bucket E — GAP-610 root cause investigation (~1h, test-only)

**Owner:** Opus 4.7 background agent (worktree-isolated)
**Files:**
- NEW `kitehub-subscription/src/test/java/com/kitehub/subscription/beta/BetaSignupTokenReproIT.java` (Testcontainers)
- EDIT `documents/04-quality/gaps/phase-1-beta/GAP-610-validate-token-returns-not-found-for-valid-token.md` (findings section + root cause)

**Method:** 3 hypotheses reproduction trên Testcontainers (real PostgreSQL):
1. **H1 RLS bypass:** `validate_token` repo query bypasses tenant RLS context
2. **H2 Public endpoint missing:** controller not registered OR path mismatch
3. **H3 Companion 404:** instances/payments/revenue all return 404 (same root cause)

For each H: seed beta-signup token → call layer-by-layer (repo → service → controller) → identify which layer returns "not found".

**AC:**
- [ ] IT reproduces 404 symptom
- [ ] Root cause located trong specific layer (one of 3 hypotheses confirmed OR all 3 ruled out → 4th hypothesis filed)
- [ ] GAP-610 file Log entry với finding + proposed fix scope
- [ ] GAP-610 pct 75→85
- [ ] If H3 confirmed → file companion gap for instances/payments/revenue 404 cluster

### Bucket B (GAP-535) — META PUNT to GAP-823 wave

**Punted scope:**
- V40 migration ships `instances.slug` column in `kitehub-subscription/src/main/resources/db/migration/`
- Instance entity (lives in `kitehub-**platform**` package — different service) thiếu `slug` field
- No `InstanceRepository` exists either package
- No `InstanceService` exists either package
- `TenantSlugNormalizer` (kitehub-subscription) has ZERO production callers — dead class
- Cross-service ownership of `instances` table → architectural decision required

**Action this PR:** File GAP-823 META P0 documenting full triad drift + audit-catalog trust-pass anti-pattern (Wave meta-7 flipped GAP-535 → DONE incorrectly per `gap-done-discipline.md` §2 AC unchecked at flip time). Re-open GAP-535 từ closed/ if needed OR cross-link.

### Bucket F — AWS live verify (DEFER next wave)

Per `local-self-test-before-aws-deploy.md` §5 (`LOCAL_SMOKE_SKIP: AWS stack stopped`) + `feature-ship-runtime-walk-mandate.md` (RST walk before DONE flip for user-facing). 3 buckets need live verify post-AWS-restore:
- Bucket C: live POST `/api/platform/instances` idempotency replay verify
- Bucket D: post-deploy seed run + DB query VN names rendering
- Bucket E: fix-PR walk (Bucket E ships investigation only; fix-PR separate)

---

## 5. Verification Gates per bucket

| Bucket | Pre-merge gate |
|---|---|
| C | IT PASS replay + 422 cases + CSV updated + GAP-536 Log |
| D | State-check finding documented + seeder VN names + unit test PASS + GAP-538 Log |
| E | IT reproduces + root cause documented + GAP-610 Log |
| GAP-823 | File created với full triad drift evidence + P0 + cross-link GAP-535 closed file |

---

## 6. Agent Spawn Pattern

**3 parallel Opus 4.7 background agents** (per `agent-model-opus-default.md` v1.0.0 + `agent-background-spawn-default.md` v1.0.1):
- Worktree-isolated (3 worktrees `.claude/worktrees/agent-c-idempotency`, `agent-d-vn-seed`, `agent-e-gap610-investigation`)
- All 3 touch different files (zero conflict surface)
- `run_in_background: true` + `model: "opus"` mandatory

Coordinator (inline):
- Files GAP-823 META gap same plan PR
- Updates `gap-status.csv` cho GAP-823 + planned C/D/E rows status changes
- Reviews + merges 3 bucket PRs in completion order
- Ships closure PR per `wave-closure-scope-completeness.md` v1.0.1 §3 Scope-Completeness Reconciliation table

---

## 7. Closure Protocol

Per `wave-closure-scope-completeness.md` v1.0.1 + `post-merge-sync-completeness.md`:
- Single closure PR với Scope-Completeness Reconciliation table cho 4 items (C/D/E + Bucket B punt + Bucket F defer)
- 4-target sync: CSV + ROADMAP + wave-history.jsonl + session-handoff
- Wave plan frontmatter `status: planning → in-progress → complete`
- Auto-merge eligible per `docs-only-pr-auto-merge.md` (closure PR = docs-only)

---

## 8. Log

- **2026-06-01** (planning): Plan created post state-check audit. Initial Wave onboarding-polish-1 plan assumed Bucket B = ~1h normalizer wire; state-check surfaced full triad drift (V40 in subscription, Instance in platform, no Repository/Service, normalizer dead) = ~4-6h architectural scope. User chose scope C+D+E only này wave, punt B to GAP-823 META wave. Bucket D scope revised: includes state-check sub-step because `ProductionSeedRunner` (original plan assumption target) only seeds admin user + system_config, NOT students/courses — real VN-name target needs identification. Outside-in audit SKIP per Wave 100 ≤30 ngày coverage. Single coordinator inline + 3 parallel Opus background agents spawn pattern. Estimate ~3.25h wall-clock (parallel) vs ~5h serial.
