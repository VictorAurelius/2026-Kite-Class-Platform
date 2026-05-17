---
title: Wave 87 — Dev Self-Test Enablement
status: draft
created: 2026-05-17
updated: 2026-05-17
waves: [87]
gaps: [GAP-518, GAP-519, GAP-523]
---

# Wave 87 — Dev Self-Test Enablement

**Goal:** Cho phép dev (solo) tự walk-through 94 USER-VERIFY rows + tick-pass 27 INSUFFICIENT_SPEC rows trong `phase-1-beta-acceptance-self-test.csv` trong 1-2 buổi, không bị stuck creds/seed/state.

**Trigger:** Wave 86 pre-tag acceptance self-test (PR #1462) classify 126 rows → 5 PASS / 94 USER-VERIFY / 27 INSUFFICIENT_SPEC. Toàn bộ Layer 3 (UI flow completeness) + Layer 4 (external integration) per `pre-handoff-self-test-completeness.md` §8 cần user walk-through. Audit identified 3 P0 blockers (admin role, admin nav, CORS) + thiếu seed/creds/preflight scripts làm walkthrough bất khả thi.

**Estimated wall-clock:** ~6-8h agent work × 6 buckets parallel ≈ longest bucket ~120-150 min.

---

## 1. Brainstorm

### Q1: 4-bucket inside-out + outside-in completeness

**Inside-out from ROADMAP §🚀 Next Action:**
- 94 USER-VERIFY rows (Wave 86 self-test results)
- 27 INSUFFICIENT_SPEC rows (vague `verify_via='UI'` cần refine)
- 8 known blocker gaps: GAP-518 (admin role) / GAP-519 (admin nav) / GAP-514 (rate-limit) / GAP-523 (CORS) / GAP-524 (email verify) / GAP-525 (beta signup) / GAP-515 (lockout) / GAP-521 (audit log entity)

**Inside-out from `documents/03-planning/inside-out-queue.md`:**
- 4 queued items (Premium plan / Feedback channel / Email content audit / User manual) — phase-1-beta scope nhưng KHÔNG match Wave 87 self-test scope → defer Wave 88+

**Inside-out audit (CSV `phase: phase-1-beta` non-DONE):**
- Skip — user explicit AskUserQuestion picked scope 4 buckets recommended + Bucket E/F + defer pre-tenant gaps Wave 88; no need to surface more

**Outside-in NEW (per `outside-in-coverage-trigger.md` 3-agent parallel run 2026-05-17):**
- Agent #1 Persona: 5 new blockers (B1 smoke creds retrieval / B2 multi-persona seed / B4 test inbox / B5 XLSX render) + 3 enablers
- Agent #2 External benchmark: Playwright `storageState` pattern cắt 70% login overhead × 7 personas; seed-personas.sh + impersonate endpoint convention
- Agent #3 Failure-mode 4×2 matrix: top 3 P0 outside known gaps = admin role mismatch (overlap GAP-518), **concurrent JWT tab collide** (NEW), **DB state pollution mid-walkthrough** (NEW); 2 helper scripts gợi ý

### Q2: Trade-offs considered

- **All-in fix 13 gaps trong 1 wave** — rejected: scope creep, 6 bucket đã đủ, pre-tenant gaps GAP-525/514/524/515/521 defer Wave 88 sau dev real walk-through để prioritize đúng
- **Skip seed script, dev tự signup mỗi persona** — rejected: GAP-525 block beta signup → không thể tự tạo P2 Owner qua flow; phải DB insert
- **Skip Playwright storageState** — included as Bucket F: 4-6h investment unblocks rc.2 re-walkthrough + future cohort onboarding; net positive
- **Patch CSV `kitehub.vercel.app`** (Bucket C) vs wait CF cutover — chosen: CF cutover (PR #1466 5-gate) external dependency dev-execute; CSV patch independent, ship now; revert URL khi CF green

### Q3: Risks + recovery

| Risk | Bucket | Recovery |
|---|---|---|
| Seed script không idempotent → DB pollute | A | Bucket B reset script truncates state; pair tests in Bucket A AC |
| Bucket A & B race nếu modify cùng `scripts/dev/` README | A/B | Disjoint files: A creates seed-personas.sh + smoke-creds.sh; B creates self-test-preflight.sh + self-test-reset.sh; shared README owned by B |
| Bucket D GAP-518 BE seed value change → migration tx → fail | D | State-check evidence §4 verify Flyway V<N> existence; rollback script |
| Bucket E new gaps surface scope creep | E | E scope = FILE 2 gaps + doc mitigation only; no code fix this wave |
| Bucket F Playwright capture stale storageState nếu Bucket A seed re-runs | F | F spawn AFTER A merge; storageState capture as last step |
| CF apex still timeout when dev walk-through (Bucket C URL patch insufficient) | C | Vercel direct URL works; CSV patch unblocks; CF cutover separate track |

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | (none — enabler) | bg-agent | ~90min | ✅ `scripts/dev/seed-personas.sh` + `scripts/dev/smoke-creds.sh` |
| B | (none — enabler) | bg-agent | ~75min | ✅ `scripts/dev/self-test-preflight.sh` + `scripts/dev/self-test-reset.sh` + `scripts/dev/README.md` |
| C | (CSV refine — docs) | bg-agent | ~60min | ✅ `documents/05-guides/operations/acceptance-tests/*.csv` + rendered XLSX |
| D | GAP-518, GAP-519, GAP-523 | bg-agent | ~150min | ⚠️ Cross-layer GAP-518 (BE seed enum literal ↔ FE role-guard literal sync) + GAP-519 FE nav + GAP-523 BE CORS config |
| E | (NEW GAP-XXX, GAP-YYY) | bg-agent | ~45min | ✅ `documents/04-quality/gaps/GAP-*.md` only |
| F | (Playwright scaffold) | bg-agent | ~120min | ✅ `kitehub-frontend/tests/e2e/personas/*` (post-Bucket-A) |

Disjoint check:
- A + B + C + D + E touch different paths
- F depends on A merge (storageState capture cần seeded data) — spawn AFTER A

---

## 3. Scope

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** MEDIUM → model: Opus 4.7 (Bucket D cross-layer enum sync risk), Sonnet sufficient cho A/B/C/E/F
**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** NO — GAP-518 là enum literal reconciliation không tạo endpoint mới; api-contract.md không cần Bucket 0 Foundation

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** Seed + creds | — | 🔴 P0 | `scripts/dev/seed-personas.sh`, `scripts/dev/smoke-creds.sh`, `.env.test.example` | parallel batch 1 |
| 2 | **B** Preflight + reset | — | 🔴 P0 | `scripts/dev/self-test-preflight.sh`, `scripts/dev/self-test-reset.sh`, `scripts/dev/README.md` | parallel batch 1 |
| 3 | **C** CSV refinement | — | 🟠 P1 | `documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv`, `.xlsx` snapshot | parallel batch 1 |
| 4 | **D** P0 fixes | GAP-518, GAP-519, GAP-523 | 🔴 P0 | BE seed migration + FE `role-guard.ts` + admin sidebar component + gateway CORS config | parallel batch 1 |
| 5 | **E** New gap files | — | 🟠 P1 | `documents/04-quality/gaps/GAP-XXX-jwt-tab-collide.md` + `GAP-YYY-beta-request-abort-cleanup.md` | parallel batch 1 |
| 6 | **F** Playwright scaffold | — | 🟡 P2 | `kitehub-frontend/tests/e2e/personas/*.storageState.json` + `playwright.dev.config.ts` | spawn AFTER A merge |

### Bucket A — Seed personas + smoke creds delivery

- Files: `scripts/dev/seed-personas.sh`, `scripts/dev/smoke-creds.sh`, `.env.test.example`
- `seed-personas.sh` idempotent: create 1 PLATFORM_ADMIN + 1 approved tenant "Sky Education" (slug `sky-education`) + 1 P2 Owner (`owner@sky-education.test`) + 2 Teachers + 3 Parents + 3 Students + 1 class "Lớp 5A1" + 1 sample payment row. Use UPSERT pattern (ON CONFLICT). Output: credential table.
- `smoke-creds.sh`: wrap `aws secretsmanager get-secret-value` cho secret `kite/dev/smoke-creds`; print 1-screen table (email + password + role + tenant slug). Fallback `.env.test` nếu secret chưa exist (instruction để dev tạo).
- `.env.test.example`: document required fields + Resend verified-recipient aliases (`+admin@kitehub.me` v.v.) + AWS profile pointer
- Tests: shellcheck pass + script `--dry-run` mode prints plan without DB write
- Acceptance:
  - [ ] `bash scripts/dev/seed-personas.sh --dry-run` exit 0 + print plan
  - [ ] `bash scripts/dev/seed-personas.sh` chạy 2 lần liên tiếp không duplicate (idempotent)
  - [ ] `bash scripts/dev/smoke-creds.sh` print credentials trong 1 màn hình
  - [ ] README pointer trong `scripts/dev/README.md` (owned by Bucket B; A writes only seed-personas.sh + smoke-creds.sh sections, B merges)

### Bucket B — Preflight + reset scripts + dev README

- Files: `scripts/dev/self-test-preflight.sh`, `scripts/dev/self-test-reset.sh`, `scripts/dev/README.md`
- `self-test-preflight.sh` checks:
  - Docker stack health (6 containers `kite-postgres`/`kite-redis`/`kite-rabbitmq`/`kite-minio`/`kite-gateway` + 1 service)
  - Flyway version latest (`SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1`)
  - Admin role canonical match: `grep "PLATFORM_ADMIN" kitehub-admin/src/main/.../*.java` + `grep "PLATFORM_ADMIN" kitehub-frontend/src/.../role-guard.ts` (both literal)
  - ALB HTTPS:443 reachable (`curl -sI -k https://kitehub-alb-...amazonaws.com/`)
  - CF DNS resolve `api.kitehub.me`
  - Resend API key valid + verified recipients list non-empty
- Exit non-zero + named gate fail; exit 0 + "All gates green" if pass
- `self-test-reset.sh`: TRUNCATE `beta_requests`, `admin_audit_logs`, `tenant_invitations`, `parent_invitations` + Redis `FLUSHDB` keys matching `auth:*` `ratelimit:*` + re-invoke `seed-personas.sh`
- `README.md`: 4 sections per `docs-folder-structure.md` §3 (Purpose / Directory map / File placement rules / Archive policy) + how to use cycle (preflight → seed → walkthrough → reset)
- Tests: shellcheck pass + each script `--help` outputs usage
- Acceptance:
  - [ ] `bash scripts/dev/self-test-preflight.sh` chạy clean state → exit 0 với 6 gate green
  - [ ] `bash scripts/dev/self-test-reset.sh --dry-run` print plan
  - [ ] `bash scripts/dev/self-test-reset.sh` + verify DB tables empty + Redis keys cleared + seed re-run
  - [ ] README có 4 sections + reference rule

### Bucket C — CSV refinement + XLSX prebuilt

- Files:
  - `documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv` (edit)
  - `documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.xlsx` (regen)
  - `documents/05-guides/operations/acceptance-tests/README.md` (edit — note Vercel URL Phase 1)
- Patch `verify_via` column 94 USER-VERIFY rows: replace `https://kitehub.me` → `https://kitehub.vercel.app` cho Phase 1 BETA self-test (note: revert sau CF cutover Wave 88+)
- Refine 27 INSUFFICIENT_SPEC rows:
  - `verify_via='UI'` → cụ thể "URL bar `/path` + element X visible + button Y enabled"
  - `verify_via='Network + DB'` → "Network tab POST `/api/v1/...` → 201 + SQL `SELECT FROM beta_requests WHERE email=...`"
  - `verify_via='Tương tự ...'` → expand inline
  - `verify_via='N/A'` (STU-LOGIN-001) → mark `deferred: phase-1.5` trong notes column
- Render XLSX: `bash scripts/render-acceptance-test-xlsx.sh phase-1-beta-acceptance-self-test`
- Tests: row count 126 preserved; UTF-8 BOM preserved per `test-artifact-format-standard.md` §1
- Acceptance:
  - [ ] 94 rows patched URL (grep `kitehub.me` trong CSV → 0 hit ngoài cột notes lưu lý do)
  - [ ] 27 INSUFFICIENT_SPEC rows refined với verify_via actionable
  - [ ] XLSX regenerated + commit
  - [ ] README cite Phase 1 Vercel URL note + revert plan

### Bucket D — Fix 3 P0 blockers (GAP-518, GAP-519, GAP-523)

- Files (multi-service):
  - **GAP-518:** BE seed migration (verify literal `PLATFORM_ADMIN`) + FE `kitehub-frontend/src/lib/auth/role-guard.ts` + `src/components/admin/*` role check sites
  - **GAP-519:** FE `kitehub-frontend/src/components/admin/AdminSidebar.tsx` (or similar) + admin nav links cho `/admin/beta-requests`, `/admin/instances`, `/admin/payments`, `/admin/revenue`
  - **GAP-523:** `kitehub-gateway/src/main/resources/application.yml` (or `WebSecurityConfig`) CORS allow `https://kitehub.me`, `https://app.kitehub.me`, `https://*.kitehub.vercel.app` for preflight
- Tests:
  - GAP-518: BE unit test seed value + FE unit test `role-guard.allow('PLATFORM_ADMIN')` returns true cho admin nav
  - GAP-519: FE component test AdminSidebar render 4 nav items khi role = PLATFORM_ADMIN
  - GAP-523: BE integration test preflight OPTIONS `Origin: https://kitehub.me` → 200 với Access-Control-Allow-Origin
- Acceptance:
  - [ ] GAP-518: admin login → land `/admin` (NOT `/dashboard`); 7 ADM-LOGIN rows pass smoke
  - [ ] GAP-519: admin sidebar visible 4 nav links; 14 ADM-NAV rows unblocked
  - [ ] GAP-523: `curl -X OPTIONS -H "Origin: https://kitehub.me" -H "Access-Control-Request-Method: POST" https://api.kitehub.me/api/v1/auth/request-beta-access` → 200 + CORS headers
  - [ ] 3 gap files flipped DONE per `gap-done-discipline.md` §2

### Bucket E — File 2 new gaps + doc mitigation

- Files:
  - `documents/04-quality/gaps/GAP-XXX-jwt-tab-collide.md` (P0 NEW — concurrent browser tab JWT storage key collision; outside-in #3 finding)
  - `documents/04-quality/gaps/GAP-YYY-beta-request-abort-cleanup.md` (P1 NEW — DB state pollution khi dev abort mid-walkthrough; outside-in #3 finding)
  - `documents/04-quality/gaps/gap-status.csv` (add 2 rows)
  - `documents/04-quality/gaps/ROADMAP.md` (link new gaps trong Phase 1 BETA section)
  - `documents/05-guides/operations/acceptance-tests/README.md` (mitigation note: "dùng 2 browser profiles riêng, không phải 2 tabs")
- Acceptance:
  - [ ] 2 gap files created với template per `audit-to-gap-pipeline.md` §3
  - [ ] State-check §2.5 evidence trong each gap (greenfield verified)
  - [ ] gap-status.csv 2 rows added per `gap-architecture-v2.md`
  - [ ] ROADMAP updated
  - [ ] Mitigation note in acceptance-tests README

### Bucket F — Playwright storageState scaffold (spawn AFTER A merge)

- Files:
  - `kitehub-frontend/playwright.dev.config.ts` (new — dev-mode config separate từ prod E2E)
  - `kitehub-frontend/tests/e2e/personas/setup.ts` (login + capture storageState per persona)
  - `kitehub-frontend/tests/e2e/personas/{admin,owner,teacher,parent}.storageState.json` (gitignored, regen)
  - `kitehub-frontend/tests/e2e/personas/README.md` (how to capture + use)
- Login script reads credentials từ `smoke-creds.sh` output OR `.env.test`; captures storageState via Playwright `page.context().storageState()`
- Codegen quick start: `npx playwright codegen --load-storage=admin.storageState.json https://kitehub.vercel.app`
- Tests: setup script run end-to-end + each persona storageState file regenerated khi seed change
- Acceptance:
  - [ ] 4 persona storageState files captured (admin/owner/teacher/parent)
  - [ ] README documents codegen recipe + when to regen
  - [ ] `.gitignore` adds `*.storageState.json` (capture from local seed, not commit)
  - [ ] 1 sample E2E spec `tests/e2e/personas/admin-smoke.spec.ts` chạy với storageState

---

## 4. State-Check Evidence

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `scripts/dev/` | Folder | `ls -d scripts/dev/ 2>/dev/null` | not yet checked | 🆕 to-be-created (Bucket A+B) |
| `scripts/render-acceptance-test-xlsx.sh` | Script | `ls scripts/render-acceptance-test-xlsx.sh` | exists per Wave 72b Bucket G | ✅ exists |
| `phase-1-beta-acceptance-self-test.csv` | CSV | `ls documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv` | exists per Wave 72b | ✅ exists |
| `GAP-518` (admin login role mismatch) | Gap file | `bash scripts/query-gaps.sh GAP-518` | per Wave 71b filing | ✅ exists (status to verify) |
| `GAP-519` (admin nav missing) | Gap file | `bash scripts/query-gaps.sh GAP-519` | per Wave 72a Bucket B | ✅ exists |
| `GAP-523` (CORS request-beta-access) | Gap file | `bash scripts/query-gaps.sh GAP-523` | per Wave 71b | ✅ exists |
| `PLATFORM_ADMIN` (BE enum literal) | Java constant | `grep -rn "PLATFORM_ADMIN" kitehub/kitehub-admin/src/main/java` | not yet verified | ⚠️ verify-at-spawn (Bucket D) |
| `role-guard` (FE module) | TS module | `grep -rn "role-guard\|roleGuard\|RoleGuard" kitehub/kitehub-frontend/src` | not yet verified | ⚠️ verify-at-spawn (Bucket D) |
| `AdminSidebar` (FE component) | TS component | `grep -rn "AdminSidebar\|admin.*[Ss]idebar" kitehub/kitehub-frontend/src` | not yet verified | ⚠️ verify-at-spawn (Bucket D) |
| `kitehub-gateway` CORS config | YAML/Java | `grep -rn "allowed-origins\|AllowedOrigins\|CorsConfig" kitehub/kitehub-gateway` | not yet verified | ⚠️ verify-at-spawn (Bucket D) |
| `kite/dev/smoke-creds` AWS Secret | Secrets Manager secret | (Tier 1 read-only verify at spawn) | not yet verified | ⚠️ verify-at-spawn (Bucket A) — `aws secretsmanager describe-secret --secret-id kite/dev/smoke-creds` may need create-first |
| `phase-1-beta-acceptance-self-test.xlsx` | XLSX render target | `ls documents/05-guides/operations/acceptance-tests/*.xlsx 2>/dev/null` | not yet checked | ⚠️ verify-at-spawn (Bucket C will regen) |

Banned shortcuts:
- `| head` truncation on grep/find
- Skipping verification "agents will check at execution"
- Aspirational references without 🆕 flag

**verify-at-spawn**: bucket agents PHẢI run grep + ls commands listed trên trước khi propose changes; nếu absent → file sub-gap thay vì cascade fix.

---

## 5. Verification Gates

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `shellcheck scripts/dev/seed-personas.sh scripts/dev/smoke-creds.sh && bash scripts/dev/seed-personas.sh --dry-run` | script-quality |
| B | `shellcheck scripts/dev/self-test-*.sh && bash scripts/dev/self-test-preflight.sh --help` | script-quality |
| C | `python3 -c "import csv; rows=list(csv.reader(open('documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv'))); assert len(rows)==127, len(rows)"` (126 data + 1 header) | none (docs) |
| D | `cd kitehub && ./mvnw -pl kitehub-admin,kitehub-gateway verify -P strict-warnings && pnpm -F kitehub-frontend test --run && pnpm -F kitehub-frontend build` | core-ci + frontend-ci + gateway-ci |
| E | `bash scripts/query-gaps.sh GAP-XXX && bash scripts/query-gaps.sh GAP-YYY && python3 -c "import csv; assert any(r[0]=='GAP-XXX' for r in csv.reader(open('documents/04-quality/gaps/gap-status.csv')))"` | gap-status-csv |
| F | `cd kitehub/kitehub-frontend && pnpm playwright install --with-deps && pnpm exec playwright test --config=playwright.dev.config.ts --grep "admin-smoke"` | none (dev-only config) |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:

**Batch 1 (parallel, simultaneous):** A + B + C + D + E — 5 agents `run_in_background: true`, `isolation: worktree`
**Batch 2 (sequential, after A merge):** F — 1 agent post-merge

Coordinator (this session OR next session) handles:
- Verify CI green per bucket
- Sequential merge to wave/87 branch (or squash-direct to main per bucket if disjoint OK)
- Bucket D coordinate cross-service test (admin + gateway + frontend)
- Final closure PR includes: ROADMAP update + wave plan `status: complete` + wave-history.jsonl append + `bash scripts/prune-merged-worktrees.sh --yes`

---

## 7. Closure Protocol

Per `post-wave-cleanup.md` + `gap-done-discipline.md` + `post-merge-sync-completeness.md`:

- [ ] All 6 buckets merged (or 5 + F deferred Wave 88 if effort overruns)
- [ ] GAP-518, GAP-519, GAP-523 flipped DONE với AC checked + no banned phrases
- [ ] 2 new gaps (Bucket E) appended to gap-status.csv + ROADMAP
- [ ] Wave plan `status: complete` + `updated:` bumped
- [ ] `documents/03-planning/wave-history.jsonl` append entry
- [ ] ROADMAP §🚀 Next Action updated (queue Wave 88 pre-tenant gap cluster GAP-524/525/514/515/521)
- [ ] `bash scripts/prune-merged-worktrees.sh --yes` clean
- [ ] Inside-out-queue.md unchanged (Wave 87 không consume queued items)
- [ ] Handoff message: "Wave 87 ✅ ship. Next dev action: `bash scripts/dev/self-test-preflight.sh && bash scripts/dev/seed-personas.sh && bash scripts/render-acceptance-test-xlsx.sh phase-1-beta-acceptance-self-test` → mở XLSX → walk-through theo persona order Anonymous → Admin → Pre-tenant → P2 Owner → Teacher → Parent."

---

## 8. Log

- **2026-05-17:** Wave 87 plan drafted. Scope locked via AskUserQuestion explicit (4 buckets recommended + Bucket E + F, defer pre-tenant gaps Wave 88). Outside-in audit 3-agent parallel run (persona / external-benchmark / failure-matrix) consolidated trong §1 Brainstorm Q1. Inside-out queue file (4 items) cross-referenced — non-overlap với Wave 87 scope, defer Wave 88+. CF apex cutover (PR #1466 5-gate) separate dev-execute track, not in this wave.
