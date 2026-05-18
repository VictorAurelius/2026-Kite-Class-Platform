---
title: Wave 78 — Beta Invite Launch Retain (UX/trust scope — 6 buckets / 14 P0 items)
status: complete
created: 2026-05-14
updated: 2026-05-14
waves: [78]
gaps: [GAP-428, GAP-480, GAP-508, GAP-514, GAP-515, GAP-518, GAP-527, GAP-531, GAP-538, GAP-539, GAP-540, GAP-541, GAP-542, GAP-543]
---

# Wave 78 — Beta Invite Launch Retain (UX/trust)

**Goal:** Phase 1 BETA invite-ready cho RETAIN layer — beta user nhận invite → onboarding mượt (sample data + checklist) → tin tưởng môi trường (beta disclaimer + /beta-status + support discoverable) → backend close-out (rate limit + Retry-After UX + admin role) → email content tone Việt + tenant init handoff → feedback channel + 5 email types content audit.

**Trigger:** Wave 77 (SEND foundation — PR #1339 pending merge) đóng 4 inside-out P0 PARTIAL (GAP-370/502/525/530) + 4 outside-in NEW (GAP-533-536). Inside-out completeness audit 2026-05-14 surface 5 BLOCKING items missed (GAP-480, GAP-527, GAP-531, GAP-040, PDPL DSAR). Outside-in 3-agent audit cùng 2026-05-14 surface 4 NEW P0 cho RETAIN scope (N1/N2/N7/N8). User confirm 3 inside-out additions: Premium plan DEFER Wave 79, Feedback channel vào Wave 78, Email content audit vào Wave 78.

**Estimated wall-clock:** ~6-8h. Bucket 0 Foundation ~30 min sequential (api-contract.md + MSW handlers); 6 buckets parallel ~3-5h longest bucket (Bucket B onboarding checklist + sample data + Bucket E email content audit có nhiều prose review).

---

## 1. Brainstorm

**Q1 (alignment):**
- Persona Phase 1 BETA: P2 Trung tâm Owner + P3 Manager + Tier 1 audience đã verified Wave 77 SEND. Wave 78 phục vụ RETAIN sau invite — user click email → đăng nhập lần đầu → kỳ vọng UX không bị bỏ rơi.
- Domain coverage: FE (Bucket A polish + Bucket B onboarding + Bucket F feedback widget) + BE (Bucket C rate limit + Bucket E email send + Bucket D admin role) + Content (Bucket E email content audit + Bucket A i18n).
- Cross-cut: 3-layer business docs (rules + use-cases + api-contract) cho new endpoints (feedback / beta-status / onboarding-progress / support-tickets) — Bucket 0 Foundation MERGE FIRST.

**Q2 (trade-offs):**
- **Feedback widget — in-app vs email survey:** chọn **CẢ HAI** (in-app cho real-time bug report + email survey day-7/14 cho retention insight). Per outside-in agent: Tier 1 beta tenant cần kênh feedback NHANH; survey-only chậm.
- **Beta disclaimer — banner persistent vs dismissible:** chọn **dismissible với cookie persist** (persistent gây UX noise; user dismiss = user đã đọc). Banner nội dung "Beta — dữ liệu có thể reset; phản hồi gửi support@".
- **/beta-status page — static doc vs live status:** chọn **static MVP** (manual update khi có outage/announcement) — live status page có cost overhead Phase 2. Page route đơn giản `/beta-status` render markdown content.
- **Onboarding checklist persistence — localStorage vs BE table:** chọn **BE table `onboarding_progress`** (cross-device persistence + admin có thể track completion %). API endpoint `GET/PUT /api/v1/onboarding-progress`.
- **Email content audit — translate ALL hay sample 5 critical:** chọn **5 critical email types** (welcome / approve-tenant / reset-password / beta-invite / day-7-survey) — full audit Wave 79+.
- **Cross-layer enforcement:** mọi NEW endpoint trong Wave 78 (feedback / beta-status / onboarding-progress / support-tickets nếu in-house) cần api-contract.md TRƯỚC khi FE/BE bucket spawn. Per `contract-first-for-cross-layer.md` v1.0.1.

**Q3 (risks):**
- **Bucket A (Prospects UI kit) effect on GAP-428:** GAP-428 hiện P1 nhưng "P0 effective" cho RETAIN layer vì homepage/pricing là first-touch sau invite click. Risk: scope creep nếu agent rebuild toàn bộ Prospects pages — narrow scope vào landing + pricing + signup-funnel polish.
- **Bucket B sample data seed** — risk dữ liệu seed leak vào prod tenant. Mitigation: seed gated bởi tenant.metadata.is_beta_demo_data flag; explicit user opt-in trên onboarding step 1.
- **Bucket C GAP-514 live 429 verify** — cần `/api/auth/password-reset-request` route exists; GAP-514 PARTIAL note đã flag. Bucket C bao gồm route addition nếu thiếu.
- **Bucket D GAP-518 role mismatch** — Wave 72a Bucket C shipped PARTIAL 80% (BE seed PLATFORM_ADMIN vs FE guard ADMIN reconciled?). State-check verify; nếu vẫn mismatch cần FE guard update.
- **Bucket E email content audit** — 5 email types có nhiều prose. Per `dev-readable-doc-language.md`: VN narrative, English identifier (template name + i18n key giữ English).
- **Bucket F feedback widget** — in-app widget cần BE endpoint + DB table `feedback_submissions`. Per `concurrent-production-mutation-ops.md`: serialize migration trước deploy.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| 0 Foundation | api-contract.md NEW endpoints + MSW handlers | bg-agent | ~30 min | ✅ contract docs + MSW only |
| A FE Polish | GAP-428 (effective P0 RETAIN), GAP-541 (N8 customer-facing VN i18n) | bg-agent | ~2-3h | ✅ kitehub-frontend prospects pages + i18n keys |
| B UX onboarding | GAP-538 (N1 onboarding checklist + sample data), GAP-539 (N2 beta disclaimer + /beta-status) | bg-agent | ~3-4h | ✅ FE onboarding component + minimal BE `onboarding_progress` endpoint |
| C Backend close-out | GAP-508 (env config registry meta), GAP-514 (rate limit live 429 + reset route), GAP-515 (FE Retry-After UX) | bg-agent | ~2-3h | ✅ gateway config + auth controller + FE auth error UX |
| D Admin/security close-out | GAP-518 (role mismatch live walkthrough), GAP-480 (beta invite flow doc) | bg-agent | ~1-2h | ✅ FE admin role guard + runbook doc |
| E Email + smoke | GAP-527 (kitehub-email actuator + E2E smoke), GAP-543 (5 email types content audit), GAP-531 (tenant init handoff post-approve walkthrough) | bg-agent | ~3-4h | ✅ kitehub-email module + i18n template files + walkthrough runbook |
| F Beta business | GAP-542 (feedback widget + email survey day-7/14), GAP-540 (N7 support channel discoverability) | bg-agent | ~2-3h | ✅ FE feedback widget + footer + new BE `/feedback` endpoint |

**Disjoint check:**
- Bucket 0: only `documents/01-business/{onboarding,feedback,beta-status,support}/api-contract.md` + `kitehub-frontend/src/test/msw/handlers/` (NEW files)
- Bucket A: only `kitehub-frontend/src/app/(public)/` + i18n keys (no overlap với Bucket B onboarding component)
- Bucket B: only `kitehub-frontend/src/app/(dashboard)/onboarding/` + `(public)/beta-status/` + `kitehub-subscription/src/main/java/.../onboarding/` BE module (new module)
- Bucket C: only `kitehub-gateway/application.yml` rate limit config + `kitehub-subscription/.../auth/` controller (no overlap với Bucket B onboarding)
- Bucket D: only `kitehub-frontend/src/lib/auth-guard.ts` + `documents/05-guides/operations/beta-invite-flow.md` (new runbook)
- Bucket E: only `kitehub-email/` module + `documents/01-business/email/templates/*.md` audit notes + `documents/05-guides/operations/tenant-init-handoff-runbook.md`
- Bucket F: only `kitehub-frontend/src/components/feedback-widget/` + `kitehub-subscription/.../feedback/` BE module (new) + footer component

Cross-bucket coordination: Bucket 0 ships first (api-contract.md), then Buckets A-F spawn parallel. Bucket E email content audit references Bucket 0 contract for content/tone alignment.

---

## 3. Scope (compact schema)

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** **HIGH** (production-touch FE + BE + content audit) → model: **Opus 4.7 full** for all buckets
**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** **YES → Bucket 0 Foundation required** per `contract-first-for-cross-layer.md` v1.0.1 (FE+BE chạm + NEW endpoints `/feedback` + `/api/v1/beta-status` + `/api/v1/onboarding-progress` + `/api/v1/support-tickets`)

> **Gap referencing convention** (per `.claude/rules/gap-architecture-v2.md`): mọi gap ID dưới đây đã verified qua `bash scripts/query-gaps.sh` hoặc grep `gap-status.csv` — xem §4 State-Check Evidence.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 0 | **Foundation** | api-contract.md + MSW infra cho 4 NEW endpoints | 🟠 P1 | `documents/01-business/{onboarding,feedback,beta-status,support}/api-contract.md` + `kitehub-frontend/src/test/msw/handlers/` | MERGE FIRST |
| 1 | **A FE Polish** | GAP-428, GAP-541 | 🔴 P0 (effective) / 🔴 P0 | `kitehub-frontend/src/app/(public)/` + `kitehub-frontend/src/i18n/locales/vi/` | parallel after Bucket 0 |
| 2 | **B UX onboarding** | GAP-538, GAP-539 | 🔴 P0 / 🔴 P0 | `kitehub-frontend/src/app/(dashboard)/onboarding/` + `(public)/beta-status/` + `kitehub-subscription/src/main/java/.../onboarding/` | parallel after Bucket 0 |
| 3 | **C Backend close-out** | GAP-508, GAP-514, GAP-515 | 🔴 P0 / 🔴 P0 / 🔴 P0 | `kitehub/kitehub-gateway/src/main/resources/application.yml` + `kitehub-subscription/.../auth/AuthController.java` + `kitehub-frontend/src/components/auth/` | parallel after Bucket 0 |
| 4 | **D Admin/security close-out** | GAP-518, GAP-480 | 🔴 P0 / 🟠 P1 | `kitehub-frontend/src/lib/auth-guard.ts` + `documents/05-guides/operations/beta-invite-flow.md` | parallel after Bucket 0 |
| 5 | **E Email + smoke** | GAP-527, GAP-543, GAP-531 | 🟠 P1 / 🔴 P0 / 🟠 P1 | `kitehub/kitehub-email/` + `documents/01-business/email/` + `documents/05-guides/operations/tenant-init-handoff-runbook.md` | parallel after Bucket 0 |
| 6 | **F Beta business** | GAP-542, GAP-540 | 🔴 P0 / 🔴 P0 | `kitehub-frontend/src/components/feedback-widget/` + `kitehub-subscription/.../feedback/` + `kitehub-frontend/src/components/layout/footer.tsx` | parallel after Bucket 0 |

### Bucket 0 — Foundation (Contract + Mock Infrastructure)

Per `.claude/rules/contract-first-for-cross-layer.md` v1.0.1:

- Files: 4 NEW `documents/01-business/{domain}/api-contract.md` CREATE — `onboarding`, `feedback`, `beta-status`, `support` domains
- Endpoints documented (FE+BE consume in same wave):
  - `GET /api/v1/onboarding-progress` — current tenant's checklist state
  - `PUT /api/v1/onboarding-progress` — update step completion
  - `POST /api/v1/feedback` — in-app feedback widget submission
  - `GET /api/v1/beta-status` — public beta status content (markdown payload)
  - `POST /api/v1/support-tickets` — support inquiry (in-house route OR external Zendesk-like; Bucket F decides + reflects in contract)
- MSW handlers: `kitehub-frontend/src/test/msw/handlers/{onboarding,feedback,beta-status,support}.ts` setup
- Acceptance: api-contract.md tồn tại + endpoint shape rõ ràng (method + path + request/response/errors); MSW handlers consumable by FE bucket tests
- Spawn order: **MERGE FIRST** trước khi Buckets A-F spawn parallel

### Bucket A — FE Polish (Prospects UI kit + customer-facing VN i18n)

- Files: `kitehub-frontend/src/app/(public)/` (landing/pricing/signup-funnel only — RELATIVE paths per `feedback_worktree_absolute_path_contamination.md`) + `kitehub-frontend/src/i18n/locales/vi/` keys
- Tests: `kitehub-frontend/src/app/(public)/__tests__/` snapshot + i18n key coverage
- Acceptance:
  - GAP-428: Prospects landing/pricing pages match Prospects UI kit ≥105/128 per `quality/ui-review/SKILL.md`
  - GAP-541: customer-facing VN i18n strings (TOS link + approval email subject + dashboard banner) reviewed + reflect Vietnamese tone per `dev-readable-doc-language.md` §4
- (Cross-layer FE bucket): "Endpoint consumption tuân thủ schema trong `documents/01-business/{onboarding,feedback,beta-status,support}/api-contract.md`"

### Bucket B — UX onboarding (checklist + sample data + beta disclaimer + /beta-status)

- Files: `kitehub-frontend/src/app/(dashboard)/onboarding/` (FE component, NEW) + `kitehub-frontend/src/app/(public)/beta-status/page.tsx` (NEW route) + `kitehub-subscription/src/main/java/.../onboarding/OnboardingProgressController.java` (NEW BE module) + Flyway migration `V[N]__create_onboarding_progress_table.sql`
- Tests: FE onboarding component test + BE controller integration test + migration applied verify
- Acceptance:
  - GAP-538: tenant đăng nhập lần đầu → checklist 5 bước hiển thị + opt-in sample/demo data seed → step completion persists qua endpoint `PUT /api/v1/onboarding-progress`
  - GAP-539: beta disclaimer banner dismissible với cookie persist trên dashboard + `/beta-status` route render markdown content (static MVP)
- (Cross-layer FE bucket): contract schema cho onboarding + beta-status endpoints
- (Cross-layer BE bucket): Controller signature + DTO match `documents/01-business/onboarding/api-contract.md` schema; integration test verify response shape

### Bucket C — Backend close-out (env config registry + rate limit + Retry-After UX)

- Files: `kitehub/kitehub-gateway/src/main/resources/application.yml` (rate limit config) + `kitehub-subscription/src/main/java/.../auth/AuthController.java` (password-reset-request route nếu thiếu) + `kitehub-frontend/src/components/auth/` (Retry-After UX)
- Tests: integration test 429 returned khi vượt threshold + FE component shows Retry-After countdown
- Acceptance:
  - GAP-508: env config registry doc updated với 4 NEW endpoints (onboarding/feedback/beta-status/support) + env var matrix
  - GAP-514: live 429 smoke verify trên `/api/auth/login` + `/api/auth/password-reset-request` (route thêm nếu thiếu) — DONE flip 100%
  - GAP-515: FE Retry-After UX hiển thị countdown timer + retry button disabled until reset — DONE flip 100%

### Bucket D — Admin/security close-out (role mismatch + beta invite flow doc)

- Files: `kitehub-frontend/src/lib/auth-guard.ts` (FE role guard align với BE seed) + `documents/05-guides/operations/beta-invite-flow.md` (NEW runbook)
- Tests: FE role guard unit test cover PLATFORM_ADMIN + ADMIN aliases
- Acceptance:
  - GAP-518: BE seed `PLATFORM_ADMIN` vs FE guard accepts both PLATFORM_ADMIN + ADMIN (compat layer) — live walkthrough verify per `pre-handoff-self-test-completeness.md` §2.4 admin checklist (a)+(b)+(c)
  - GAP-480: beta invite flow runbook ship — covers entry trigger / approve workflow / email send / tenant init handoff (end-to-end pipeline doc)

### Bucket E — Email + smoke (actuator health + content audit + tenant init handoff)

- Files: `kitehub/kitehub-email/src/main/resources/application.yml` (actuator endpoints expose) + 5 email template files audit `documents/01-business/email/templates/{welcome,approve-tenant,reset-password,beta-invite,day-7-survey}-audit.md` (NEW notes) + `documents/05-guides/operations/tenant-init-handoff-runbook.md` (NEW)
- Tests: actuator `/health` returns 200 + E2E smoke email send (Resend mock OR live staging)
- Acceptance:
  - GAP-527: kitehub-email actuator `/health` + `/info` exposed + E2E smoke send 1 email via API → Resend delivery confirm — DONE 100%
  - GAP-543: 5 email types content audit báo cáo VN tone correctness (per `dev-readable-doc-language.md` §4 mixed-language rule) + 0 PII leak in subject lines
  - GAP-531: tenant init handoff post admin-approve walked end-to-end (admin approves request → tenant subdomain provisioned → welcome email sent → owner login → onboarding checklist visible) — runbook ship + 1 live walkthrough evidence

### Bucket F — Beta business (feedback widget + support channel discoverability)

- Files: `kitehub-frontend/src/components/feedback-widget/` (NEW component) + `kitehub-subscription/src/main/java/.../feedback/FeedbackController.java` (NEW BE module) + Flyway migration `V[N]__create_feedback_submissions_table.sql` + `kitehub-frontend/src/components/layout/footer.tsx` (support@ + chat widget anchor)
- Tests: FE widget submit + BE controller integration test + footer link visibility
- Acceptance:
  - GAP-542: in-app feedback widget hiển thị floating button góc phải dashboard + form submit (rating 1-5 + text) → `POST /api/v1/feedback` → DB row + email digest day-7/14 (cron job hoặc scheduled task)
  - GAP-540: footer hiển thị `support@kitehub.me` + chat widget (Crisp/Tawk.to embed OR mailto: link MVP) + Help/FAQ link discoverable trong dashboard nav

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `GAP-428` | Existing gap | `bash scripts/query-gaps.sh \| grep GAP-428` | OPEN P1 n/a 0% "Prospects / Public Pages Have No UI Kit Coverage" | ✅ exists (P1 in CSV; treated P0 effective for RETAIN per task spec) |
| `GAP-480` | Existing gap | `grep "^GAP-480," documents/04-quality/gaps/gap-status.csv` | OPEN P1 phase-1-beta 0% "Beta invitation flow undefined" | ✅ exists |
| `GAP-508` | Existing gap | `grep "^GAP-508," gap-status.csv` | PARTIAL P0 phase-1-beta 60% "Production env config registry meta-gap" | ✅ exists |
| `GAP-514` | Existing gap | `grep "^GAP-514," gap-status.csv` | PARTIAL P0 phase-1-beta 66% "Auth endpoints missing gateway rate limit" | ✅ exists |
| `GAP-515` | Existing gap | `grep "^GAP-515," gap-status.csv` | PARTIAL P0 phase-1-beta 80% "Account lockout missing" | ✅ exists |
| `GAP-518` | Existing gap | `grep "^GAP-518," gap-status.csv` | PARTIAL P0 phase-1-beta 80% "BE seed PLATFORM_ADMIN vs FE guard ADMIN mismatch" | ✅ exists |
| `GAP-527` | Existing gap | `grep "^GAP-527," gap-status.csv` | OPEN P1 phase-1-beta 0% "kitehub-email actuator health + email send end-to-end smoke" | ✅ exists |
| `GAP-531` | Existing gap | `grep "^GAP-531," gap-status.csv` | OPEN P1 phase-1-beta 0% "Tenant init handoff post admin-approve walked end-to-end" | ✅ exists |
| `GAP-538` | Wave 78 NEW gap | (no CSV row yet — created in this PR) | filed in same PR | 🆕 to-be-created (Bucket B owner) |
| `GAP-539` | Wave 78 NEW gap | (no CSV row yet — created in this PR) | filed in same PR | 🆕 to-be-created (Bucket B owner) |
| `GAP-540` | Wave 78 NEW gap | (no CSV row yet — created in this PR) | filed in same PR | 🆕 to-be-created (Bucket F owner) |
| `GAP-541` | Wave 78 NEW gap | (no CSV row yet — created in this PR) | filed in same PR | 🆕 to-be-created (Bucket A owner) |
| `GAP-542` | Wave 78 NEW gap | (no CSV row yet — created in this PR) | filed in same PR | 🆕 to-be-created (Bucket F owner) |
| `GAP-543` | Wave 78 NEW gap | (no CSV row yet — created in this PR) | filed in same PR | 🆕 to-be-created (Bucket E owner) |
| `documents/01-business/onboarding/api-contract.md` | API contract doc | `ls documents/01-business/onboarding/api-contract.md 2>&1` | absent (domain folder NEW) | 🆕 to-be-created (Bucket 0 Foundation) |
| `documents/01-business/feedback/api-contract.md` | API contract doc | `ls documents/01-business/feedback/api-contract.md 2>&1` | absent (domain folder NEW) | 🆕 to-be-created (Bucket 0 Foundation) |
| `documents/01-business/beta-status/api-contract.md` | API contract doc | `ls documents/01-business/beta-status/api-contract.md 2>&1` | absent (domain folder NEW) | 🆕 to-be-created (Bucket 0 Foundation) |
| `documents/01-business/support/api-contract.md` | API contract doc | `ls documents/01-business/support/api-contract.md 2>&1` | absent (domain folder NEW) | 🆕 to-be-created (Bucket 0 Foundation) |
| `kitehub-frontend/src/components/feedback-widget/` | FE component folder | `ls kitehub/kitehub-frontend/src/components/feedback-widget/ 2>&1` | absent | 🆕 to-be-created (Bucket F) |
| `kitehub-subscription/src/main/java/.../onboarding/OnboardingProgressController.java` | BE controller | `find kitehub/kitehub-subscription -name "OnboardingProgressController*"` | 0 matches | 🆕 to-be-created (Bucket B) |
| `kitehub-subscription/src/main/java/.../feedback/FeedbackController.java` | BE controller | `find kitehub/kitehub-subscription -name "FeedbackController*"` | 0 matches | 🆕 to-be-created (Bucket F) |
| `kitehub-frontend/src/app/(public)/beta-status/page.tsx` | FE route | `find kitehub/kitehub-frontend/src/app -path "*beta-status*"` | 0 matches | 🆕 to-be-created (Bucket B) |

**Banned shortcuts (mirror §2.5/§2.6) — confirmed not used:**
- ✅ no `| head` truncation on grep/find
- ✅ no skipped verification
- ✅ all 🆕 references have owning bucket flag

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| 0 Foundation | `bash scripts/check-docs.sh` + `cd kitehub-frontend && pnpm test:msw` | script-quality + frontend-ci |
| A FE Polish | `pnpm -F kitehub-frontend test --run && pnpm -F kitehub-frontend build && pnpm -F kitehub-frontend lint` | frontend-ci (kitehub-frontend-ci.yml) |
| B UX onboarding | `pnpm -F kitehub-frontend test --run && cd kitehub && ./mvnw -pl kitehub-subscription verify -P strict-warnings` | frontend-ci + kitehub-ci |
| C Backend close-out | `cd kitehub && ./mvnw -pl kitehub-gateway,kitehub-subscription verify -P strict-warnings && pnpm -F kitehub-frontend test --run` | kitehub-ci + frontend-ci |
| D Admin/security | `pnpm -F kitehub-frontend test --run && bash scripts/check-docs.sh` | frontend-ci + script-quality |
| E Email + smoke | `cd kitehub && ./mvnw -pl kitehub-email verify && bash scripts/check-docs.sh` | kitehub-ci + script-quality |
| F Beta business | `pnpm -F kitehub-frontend test --run && cd kitehub && ./mvnw -pl kitehub-subscription verify -P strict-warnings` | frontend-ci + kitehub-ci |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- All buckets spawned with `run_in_background: true`
- Worktree isolation (`isolation: worktree`) for parallel safety
- RELATIVE paths in agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merges sequentially after all background completions
- Bucket 0 Foundation MERGE FIRST per `contract-first-for-cross-layer.md` v1.0.1 §3.1 — sau khi Bucket 0 merge, spawn Buckets A-F parallel
- Stake tier HIGH (production-touch) → Opus 4.7 full per `wave-pack-planner/SKILL.md` §Step 4.6

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:
- Each bucket PR updates affected GAP file Log + status (CSV canonical per `gap-architecture-v2.md` — markdown frontmatter is cache)
- ROADMAP §🚀 Next Action updated in closure PR
- Wave plan frontmatter `status: complete` flip in closure PR
- `wave-history.jsonl` append in closure PR (Rule 15 enforcement)
- Sub-gaps filed for any deferral; PARTIAL exit-ramp per `gap-done-discipline.md` §3
- Run `bash scripts/prune-merged-worktrees.sh --yes` to prune worktree husks + merged branches per `post-wave-cleanup.md` (after all bucket PRs merged, before drafting closure PR)
- **`## Release Plan Progress` section in closure PR body** — per `feedback_wave_closure_release_progress_report.md` rules #1-6: current Phase + milestone progress + wave contribution + trigger gates + estimated remaining wall-clock + **Waves Remaining table** (3 rows: strict-min v0.9.0-beta / practical v0.9.0-beta / v1.0.0 PROD với explicit wave numbers + GAP IDs + PR #s)
- **Pre-handoff self-test** per `pre-handoff-self-test-completeness.md` §2.1 (auth-gated user-flow) cho gaps GAP-518 + GAP-538 + GAP-531; §2.3 (email-driven flow) cho gaps GAP-527 + GAP-531 + GAP-543; §2.6 (payment N/A — no payment scope) — checklist trong PR body

---

## 8. Log

- **2026-05-14** (draft): Plan created. Branch `wave/78-beta-invite-launch-retain` từ origin/main (Wave 77 PR #1339 pending merge — Wave 78 pipelined on top, draft until Wave 77 lands). 6 buckets + Bucket 0 Foundation (cross-layer YES per `contract-first-for-cross-layer.md` v1.0.1 — 4 NEW endpoints). 8 existing gaps state-checked via CSV (GAP-428/480/508/514/515/518/527/531); 6 NEW gaps filed (GAP-538/539/540/541/542/543) covering inside-out additions (Premium plan DEFER Wave 79, Feedback channel, Email content audit) + outside-in additions (N1/N2/N7/N8). Stake tier HIGH → Opus 4.7 full all buckets. Estimated wall-clock ~6-8h longest bucket.

---

## 9. Post-Wave Audit Mandate

Per `.claude/rules/post-wave-audit-mandate.md` §2.1 (file-pattern → audit matching) — Wave 78 closure triggers within 3 days:

| File pattern in scope | Required audit | Skill |
|-----------------------|----------------|-------|
| `kitehub-frontend/src/**` (Buckets A/B/D/F) | UI /128 | `quality/ui-review/SKILL.md` |
| `kitehub-subscription/.../auth/Controller.java` + new `OnboardingProgressController.java` + `FeedbackController.java` (Buckets B/C/F) | API Contract /100 | `quality/api-contract-audit/SKILL.md` |
| `pom.xml` / `package.json` if deps changed (likely) | Security /100 | `quality/security-audit/SKILL.md` |
| `documents/01-business/{onboarding,feedback,beta-status,support}/api-contract.md` + `rules.md` (Bucket 0) | Business Logic /100 | `quality/business-logic-audit/SKILL.md` |
| Cross-cutting (all buckets) | Quality /100 weekly | `quality-audit/SKILL.md` |

Domain-milestone deferral (§2.4) **NOT eligible** — Wave 78 spans multiple domains (FE polish + UX onboarding + BE rate limit + admin security + email + beta business) → per-wave audit required within 3 days of closure merge.
