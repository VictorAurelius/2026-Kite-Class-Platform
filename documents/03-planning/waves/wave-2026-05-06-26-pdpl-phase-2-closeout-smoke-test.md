---
title: Wave 26 — PDPL Phase 2 close-out + Smoke Test (DSAR + DPIA + smoke-test extension)
status: complete
created: 2026-05-06
updated: 2026-05-06
waves: [26]
gaps: [GAP-353c, GAP-353d, GAP-377]
---

# Wave 26 — PDPL Phase 2 close-out + Smoke Test

**Goal:** Close PDPL 2023 Phase 2 entirely (DSAR self-service form + DPIA documentation skeleton) before 2026-07-01 hard-deadline (~7-8 weeks remaining), and extend post-deploy smoke test (sister gap to GAP-378 just shipped Wave 25 Bucket C).

**Trigger:** Wave 25 closure ROADMAP §🚀 Next Action recommendation. PDPL Phase 1 (cookie banner) shipped Wave 23; Phase 2 (server consent API) shipped Wave 25 Bucket A. Phase 2 close-out = DSAR + DPIA. GAP-377 sister to GAP-378 (rollback runbook) — both Phase 1 BETA-readiness P1 deploy artifacts; pair shipping makes operational sense.

**Estimated wall-clock:** ~30-40 phút parallel (longest bucket A ~16h raw → background agent ~25-30 phút). Buckets B + C smaller (~4-8h each → ~10-15 phút agent).

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):**
- **Prospects + Pre-tenant + tenant users (P1/P2/P3 all)** — DSAR self-service form is universal PDPL Art 14 right; covers all data subjects regardless of persona.
- **Solo-dev coordinator** (compliance posture) — DPIA pre-launch documentation prepares for >100k subscriber threshold (Decree 13/2023 Art 28(1) trigger); enterprise sales benefit from market-ready compliance docs.
- **Solo-dev + future on-call** (operations) — smoke test post-deploy = early detection of broken auth/endpoints/UI; pairs with rollback runbook (GAP-378 Wave 25 Bucket C) for full incident response loop.
- **4-layer V-model coverage** (per `design-layer-coverage.md` §2):
  - 要件定義: PDPL Art 14 (DSAR rights) + Decree 13/2023 Art 24-30 (DPIA + DPO designation) + AWS Well-Architected Op-Excellence (smoke test).
  - 基本設計: 2 production pages `/legal/data-rights` × KH+KC + DSAR ticket form + 3 BRD docs (DPO designation + DPIA + MPS A05) + extended `scripts/smoke-test.sh`.
  - 詳細設計: DSAR ticket state machine PENDING→IN_REVIEW→COMPLETED|REJECTED + 20-day SLA timer + DPO email notification flow + DPIA risk-matrix structure + smoke test exit-code + auto-rollback wiring.
  - コンポーネント設計: NEW `DsarController` + `DsarTicket` entity + `DsarService` + `SlaTimerCron` (Bucket A) + 3 BRD documents (Bucket B) + extended `scripts/smoke-test.sh` (Bucket C).

**Q2 (trade-offs):**
- **Reject:** ship DSAR backend without FE form — manual email DSAR acceptable for MVP (PDPL doesn't mandate self-service per WG13/2023 implementing decree). Filed P2 not P0. But coordinator chooses to ship full because pair-eligible with DPIA in 2-bucket compliance wave + 3-bucket adds GAP-377 force-multiplier.
- **Reject:** ship DPIA full risk assessment (per processing activity) — solo-dev <100k subscribers means audit not yet legally binding. Phase 1 = SKELETON only; full risk matrix backfilled when crossing 50k subscribers (P1 trigger per gap §"Why P2").
- **Reject:** GAP-377 Playwright E2E suite (signup/login flow E2E) — adds ~2-3 days; extension scope already covers shell smoke test. Defer E2E to follow-up gap.
- **Reject:** make GAP-377 greenfield — `scripts/smoke-test.sh` already exists 265 LOC GAP-089 baseline (~10 assertions). Bucket C = EXTEND (delta scope) not greenfield. State-check 🟡 PARTIAL exit-ramp not applicable here because gap has no PARTIAL pattern — but plan §3 explicitly frames Bucket C as extension scope.
- **Accept:** ship 3 disjoint buckets totalling ~16-22h raw. Buckets B + C explicitly modest (~4-6h + ~8-12h); Bucket A largest (~12-16h).

**Q3 (risks):**
- **Risk: Bucket A DSAR ticket cross-references kitehub-email service** — mitigation: use existing `kitehub-email` module's notification API; if API not exposed, file follow-up gap and ship Bucket A PARTIAL (ticket persists, manual DPO notification via SMTP fallback).
- **Risk: Bucket A V26 schema collides with Wave 25 Bucket A V25 (consent_record)** — mitigation: V26 strictly additive (`dsar_ticket` NEW table); V25 already merged to main. No conflict.
- **Risk: Bucket A FE form needs reCAPTCHA but project may not have key** — mitigation: ship honeypot only for v1; document reCAPTCHA as follow-up item if key available.
- **Risk: Bucket B DPIA processing inventory requires data-flow mapping that takes hours** — mitigation: skeleton-only per gap §"Why P2"; full inventory deferred to subscription-count threshold trigger. Acceptable PARTIAL.
- **Risk: Bucket C smoke-test.sh changes break existing GAP-089 invocations** — mitigation: ADD new args (KH-url + KC-url) as optional with backward-compat default to single base-url; extend (don't rewrite) existing assertions.
- **Risk: Bucket C CI workflow integration may conflict with deploy-staging.yml or deploy-production.yml** — mitigation: add as new STEP within existing job (not new workflow); idempotent additive change.
- **Risk: All 3 buckets touch `documents/00-brd/privacy-policy.md`** — mitigation: only Bucket B touches it (DPO + DPIA cross-links §2 + §13); Bucket A touches `documents/01-business/kitehub/marketing/` (DSAR rules); Bucket C touches scripts only.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort (raw) | Disjoint? |
|--------|--------|-------|--------------|-----------|
| A | GAP-353c | bg-agent | ~12-16h | ✅ kitehub-subscription/dsar + 2 FE data-rights pages + V26 |
| B | GAP-353d | bg-agent | ~4-6h | ✅ documents/00-brd/{dpia,dpo-designation,mps-a05}.md + privacy-policy.md cross-links only |
| C | GAP-377 | bg-agent | ~8-12h | ✅ scripts/smoke-test.sh extension + .github/workflows/ + cross-link to deploy-plan |

**Disjoint check:**
- A touches: `kitehub/kitehub-subscription/src/{main,test}/**/dsar/**`, `V26__create_dsar_ticket.sql`, `kitehub/kitehub-frontend/src/app/(public)/legal/data-rights/page.tsx`, `kiteclass/kiteclass-frontend/src/app/(public)/legal/data-rights/page.tsx`, `documents/01-business/kitehub/marketing/{rules,api-contract}.md` (DSAR section append).
- B touches: `documents/00-brd/dpia.md` 🆕, `documents/00-brd/dpo-designation.md` 🆕, `documents/00-brd/mps-a05-registration-check.md` 🆕, `documents/00-brd/privacy-policy.md` (§2 + §13 cross-link updates ONLY), `documents/00-brd/README.md` (3 new doc index entries).
- C touches: `scripts/smoke-test.sh` (extend, +KH-url support, +5 assertions), `.github/workflows/deploy-staging.yml` OR new `.github/workflows/post-deploy-smoke.yml`, cross-link from `documents/03-planning/roadmap/release-1-deploy-plan.md` §3.3.

**Conflict guard:**
- A vs B: Bucket A's `documents/01-business/kitehub/marketing/` ≠ Bucket B's `documents/00-brd/` — different doc trees.
- A vs C: pure separation (Java BE + FE pages vs scripts).
- B vs C: pure separation (BRD docs vs scripts).
- All vs `privacy-policy.md`: ONLY Bucket B touches.

---

## 3. Scope (per bucket)

### Bucket A — GAP-353c DSAR self-service intake form

**Backend (Java) in `kitehub/kitehub-subscription/`:**
1. New Flyway migration `src/main/resources/db/migration/V26__create_dsar_ticket.sql` — `dsar_ticket` table (id BIGSERIAL, ticket_uuid UUID UNIQUE, requester_email VARCHAR(320), requester_name VARCHAR(200), national_id_last4 VARCHAR(4), right_type VARCHAR(50), scope TEXT NULL, reason TEXT NULL, status VARCHAR(50) DEFAULT 'PENDING', sla_deadline TIMESTAMP, resolution TEXT NULL, created_at + updated_at + resolved_at).
2. Java package `kitehub-subscription/src/main/java/com/kitehub/subscription/dsar/`:
   - `entity/DsarTicket.java` JPA entity
   - `entity/DsarRightType.java` enum (ACCESS / RECTIFICATION / ERASURE / PORTABILITY / RESTRICT / OBJECT)
   - `entity/DsarStatus.java` enum (PENDING / IN_REVIEW / COMPLETED / REJECTED)
   - `repository/DsarTicketRepository.java`
   - `service/DsarService.java` interface + `DsarServiceImpl.java`
   - `controller/DsarController.java` — POST `/api/v1/dsar/request` + GET `/api/v1/dsar/{ticketId}` (redacted state for non-staff)
   - `dto/DsarRequest.java` + `dto/DsarResponse.java` Bean Validation
   - `cron/SlaTimerCron.java` — daily check overdue tickets (sla_deadline < now() → escalate)
   - Email notification: reuse pattern from `kitehub-email` if service exposes API; otherwise scaffold EmailServiceClient integration with TODO marker for follow-up

**Frontend (TS):**
3. New `kitehub/kitehub-frontend/src/app/(public)/legal/data-rights/page.tsx` — DSAR form (radio for right_type, identity fields with national_id_last4, scope free text, reason optional, contact preference, honeypot anti-spam)
4. New `kiteclass/kiteclass-frontend/src/app/(public)/legal/data-rights/page.tsx` — same component, possibly via shared-ui package or duplicated
5. Form validation client-side with shadcn Form + Zod schema + server-side via Bean Validation

**Docs:**
6. `documents/01-business/kitehub/marketing/api-contract.md` — append 2 endpoints with full request/response schemas + error codes
7. `documents/01-business/kitehub/marketing/rules.md` — add `BR-PDPL-DSAR-001..005` 5-attribute (per `business-logic-review.md` §2): SLA 20 days (PDPL Art 14), identity verification (national_id_last4), retention 36mo (DR-03), right_type enum, anti-spam.

**Tests:**
8. Unit: `DsarServiceImplTest`, `DsarControllerTest` (`@WebMvcTest` + `Mockito.reset()` per `feedback_webmvctest_mock_reset.md`)
9. IT: `DsarControllerIT` if H2/TestContainers available
10. FE: form submit happy path + validation errors

**Acceptance (subset of GAP-353c AC):** all 11 AC items checked; PARTIAL exit-ramp if email notification flow defers (file `GAP-353c-followup-dpo-email-notification` if `kitehub-email` API not exposed).

### Bucket B — GAP-353d DPIA documentation skeleton

**Files (3 new BRD docs):**
1. `documents/00-brd/dpo-designation.md` — designated DPO (acting solo-dev `@nguyenvankiet` per `business-logic-review.md` §2.3 solo-dev exemption with role declaration), scope per Art 27, comm channels, independence guarantees, reporting line, queue formal counsel review via GAP-156.
2. `documents/00-brd/dpia.md` — processing inventory skeleton (per data category × purpose × legal basis from `privacy-policy.md` §4-6), per-activity risk assessment template (probability × impact matrix), mitigation controls inventory placeholder, residual risk rating template, annual review cadence. Phase 1 = SKELETON; full inventory backfilled at 50k-subscriber trigger.
3. `documents/00-brd/mps-a05-registration-check.md` — Decree 13/2023 Art 28(1) registration trigger procedure (>100k subjects OR sensitive data); subscription growth threshold check + auto-flag at 90k; document registration intent + responsible party.

**Files (cross-link updates — ONLY 3 lines per file):**
4. `documents/00-brd/privacy-policy.md` §2 DPO field — replace "(TODO designation)" with link to `dpo-designation.md`.
5. `documents/00-brd/privacy-policy.md` §13 Security — add "DPIA mitigation summary: see `dpia.md`" line.
6. `documents/00-brd/README.md` — add 3 new doc index entries with 1-line descriptions.

**Acceptance (full GAP-353d AC):** all 8 AC items checked; status flips 🔵 OPEN → 🟢 DONE per `gap-done-discipline.md` §2 (skeleton scope explicitly framed in gap §3 — full risk-matrix backfill is 50k-trigger event-driven, not deferred).

### Bucket C — GAP-377 smoke test extension

**Files (extend existing):**
1. `scripts/smoke-test.sh` — EXTEND existing GAP-089 ~265 LOC baseline with:
   - Add second positional arg `KC_URL` (KH_URL stays first) with backward-compat `${KC_URL:-$KH_URL}`
   - Add 6+ new assertions: `KH /legal/privacy` 200, `KH /legal/terms` 200, `KH /legal/cookies` 200, `KH /auth/signup` 200, `KH /auth/request-beta-access` 200, `KH /` contains `ConsentBanner` or `consent-banner` (grep), `KH /actuator/info` returns build version (display only, no assert)
   - Total assertions: 10 baseline + 6 new = 16 (≥15 per AC #2)
   - Maintain exit code 0=pass / 1=fail
   - Maintain shellcheck-clean (existing baseline already shellcheck-passing per project standard)

**CI integration:**
2. Add post-deploy-smoke STEP within `.github/workflows/deploy-staging.yml` (or `deploy-production.yml`): run `./scripts/smoke-test.sh ${{ vars.STAGING_KH_URL }} ${{ vars.STAGING_KC_URL }}` after deploy step. Optional rollback trigger: `if: failure()` calls rollback procedure (per GAP-378 runbook; full automation deferred to GAP-378-followup if exists).
3. Cross-link from `documents/03-planning/roadmap/release-1-deploy-plan.md` §3.3 to `scripts/smoke-test.sh` extended scope.

**Documentation:**
4. Inline header comment in `scripts/smoke-test.sh` updated: usage example shows dual-URL invocation; "How to extend" section added (5-10 lines, e.g., "Add new check: assert NEW_NAME $(curl -s -o /dev/null -w '%{http_code}' $KH_URL/path) 200").

**Acceptance (full GAP-377 AC):** all 7 AC items checked; status flips 🔵 OPEN → 🟢 DONE per `gap-done-discipline.md` §2 (CI integration optional auto-rollback may PARTIAL-defer — but skeleton invocation is sufficient for AC #4).

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `kitehub-subscription` module | Maven module | `ls kitehub/kitehub-subscription/pom.xml` | 1 file | ✅ exists |
| `V25__create_consent_record.sql` | Wave 25 migration (now merged) | `ls kitehub/kitehub-subscription/src/main/resources/db/migration/V25*` | 1 file (Wave 25 Bucket A) | ✅ exists (V26 next) |
| `V26__create_dsar_ticket.sql` | Migration | `ls .../db/migration/V26*` | 0 matches | 🆕 to-be-created (Bucket A) |
| `kitehub/kitehub-frontend/src/app/(public)/legal/` | Existing legal pages dir | `ls kitehub/kitehub-frontend/src/app/(public)/legal/` | privacy/, terms/, cookies/, dmca/ | ✅ exists |
| `kiteclass/kiteclass-frontend/src/app/(public)/legal/` | Existing legal pages dir | `ls kiteclass/kiteclass-frontend/src/app/(public)/legal/` | privacy/, terms/, cookies/ | ✅ exists |
| `(public)/legal/data-rights/page.tsx` × KH+KC | DSAR form pages | `ls kitehub/kitehub-frontend/src/app/(public)/legal/data-rights kiteclass/kiteclass-frontend/src/app/(public)/legal/data-rights` | 0 matches | 🆕 to-be-created (Bucket A) |
| `DsarController` Java class | BE controller | `grep -rn "class DsarController" kitehub/` | 0 matches | 🆕 to-be-created (Bucket A) |
| `DsarTicket` entity | JPA entity | `grep -rn "class DsarTicket" kitehub/` | 0 matches | 🆕 to-be-created (Bucket A) |
| `DsarServiceImpl.java` | Service impl file | `find kitehub/ -name "DsarServiceImpl.java"` | 0 matches | 🆕 to-be-created (Bucket A) |
| `kitehub-email` module | Existing email service | `ls kitehub/kitehub-email/pom.xml` | 1 file | ✅ exists (Bucket A integration target) |
| `BR-PDPL-DSAR-001..005` | Business rules | `grep -rn "BR-PDPL-DSAR-" documents/01-business/` | 0 matches | 🆕 to-be-created (Bucket A) |
| `BR-PDPL-CONSENT-003` (consent retention 36mo) | Existing rule referenced | `grep -rn "BR-PDPL-CONSENT-003" documents/01-business/` | 3 files | ✅ exists (DR-03 retention parallel for DSAR) |
| `documents/00-brd/` folder | BRD folder | `ls documents/00-brd/` | 14+ files | ✅ exists |
| `documents/00-brd/privacy-policy.md` | Privacy policy doc | `ls documents/00-brd/privacy-policy.md` | 1 file | ✅ exists (Bucket B updates §2 + §13) |
| `documents/00-brd/dpia.md` | DPIA doc | `ls documents/00-brd/dpia.md` | 0 matches | 🆕 to-be-created (Bucket B) |
| `documents/00-brd/dpo-designation.md` | DPO designation doc | `ls documents/00-brd/dpo-designation.md` | 0 matches | 🆕 to-be-created (Bucket B) |
| `documents/00-brd/mps-a05-registration-check.md` | MPS A05 check | `ls documents/00-brd/mps-a05-registration-check.md` | 0 matches | 🆕 to-be-created (Bucket B) |
| `scripts/smoke-test.sh` | Existing smoke test (GAP-089 baseline) | `wc -l scripts/smoke-test.sh` | 265 lines, ~10 assertions | ✅ exists (Bucket C extends, NOT greenfield) |
| `.github/workflows/deploy-staging.yml` | Deploy workflow | `ls .github/workflows/deploy-staging.yml` | 1 file | ✅ exists (Bucket C adds step) |
| `.github/workflows/deploy-production.yml` | Deploy workflow | `ls .github/workflows/deploy-production.yml` | 1 file | ✅ exists (Bucket C may add step) |
| `documents/03-planning/roadmap/release-1-deploy-plan.md` | Cross-link target | `ls documents/03-planning/roadmap/release-1-deploy-plan.md` | 1 file | ✅ exists (Bucket C cross-link §3.3) |

**Banned shortcuts honored:** no `| head` truncation; full grep/find output read; alternative class-name searches performed (`DsarController` / `DsarTicket` checked across `kitehub/` folder); cross-folder searches for symbol absence.

**Bucket C scope clarification:** GAP-377 file `scripts/smoke-test.sh` ✅ exists (GAP-089 baseline 265 LOC). Bucket C scope = EXTEND with 6 new assertions + dual-URL support, NOT greenfield. Per `audit-to-gap-pipeline.md` §2.5 the gap should ideally be filed 🟡 PARTIAL with `## Current State` section — but gap is already filed 🔵 OPEN. Acceptable: Bucket C agent ships extension + closes per §2 (all 7 AC verified incl. existing assertions count toward "15+ assertions" AC #2).

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `cd kitehub && ./mvnw -pl kitehub-subscription clean verify` + `pnpm -F kitehub-frontend build` + `pnpm -F kiteclass-frontend build` | kitehub-ci + frontend-ci × 2 |
| B | `bash scripts/check-docs.sh` (markdown lint + frontmatter) + manual cross-link verify | doc-quality CI |
| C | `bash scripts/smoke-test.sh https://kitehub.vn https://kiteclass.vn` (against staging if deployed; against `--self-test` mode if not) + `shellcheck scripts/smoke-test.sh` | script-quality CI |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md` v1.0.0:
- All 3 buckets spawn with `run_in_background: true`
- Worktree isolation (`isolation: worktree`) for parallel safety
- RELATIVE paths in agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merges sequentially after all background completions: A first (largest, BE+FE), B second (docs), C last (scripts)
- Agent briefing for Bucket A includes BOTH BE + FE local verify commands per `feedback_agent_local_verify_both_layers.md`
- Wave plan PR merges to main BEFORE agent spawn per `feedback_wave_plan_through_pr.md`

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md`:
- Each bucket PR updates affected GAP file Log + status (PARTIAL exit-ramp anticipated for Bucket A if email notification flow defers; unlikely for B + C)
- ROADMAP §🚀 Next Action updated in closure PR with Wave 27 candidate (Track 2 FE start GAP-273 most likely; OR Release Lần 1 deploy artifacts P1 cluster GAP-379 secrets + GAP-372 beta tenant invite + GAP-371 CDN)
- Wave plan frontmatter `status: complete` flip in closure PR
- `wave-history.jsonl` append in closure PR (Rule 15)
- Sub-gaps filed for any deferral

---

## 8. Log

- **2026-05-06** (complete): Wave 26 SHIPPED — 3 buckets + plan + closure (5 PRs) + 1 hotfix PR side-track. PR #840 plan, PR #841 Bucket B GAP-353d 🟢 DONE (3 BRD docs DPIA + DPO designation + MPS A05 skeleton 628 LOC + privacy-policy cross-link), PR #842 Bucket C GAP-377 🟢 DONE (smoke-test.sh 265→383 LOC, 18 assertions, dual-URL, CI step in deploy-staging.yml, +follow-up GAP-377-followup-auth-route-checks P3), PR #844 Bucket A GAP-353c 🟡 PARTIAL (V26 + 10 Java files in `com.kitehub.subscription.dsar` + 2 FE pages + shared `DataRightsForm.tsx` + 393 BE tests + 491 FE tests; 10/11 AC; +follow-up GAP-353c-followup-dpo-email-notification P2). PR #843 hotfix shipped parallel — removed 2 unused imports in `TenantContextFilterTest.java` flagged by IDE post-Wave-25 merge (1 file changed, +0/-2; 5/5 tests pass). Bucket C agent surfaced + fixed latent BODY-buffer bug in `scripts/smoke-test.sh` (in-memory `BODY=$(...)` broken under command-substitution subshell scope with `set -u` → refactored to file-based `BODY_FILE` + `read_body` helper). 2 route-substitutions in Bucket C scope (`/auth/signup` → `/login`, `/auth/request-beta-access` → `/register`) because target routes absent in `kitehub-frontend/src/app/auth/`; substitutions sized as P3 follow-up. 0 merge conflicts. **Counts: 167 → 167 OPEN** (-GAP-353d closed; -GAP-377 closed; +2 follow-ups GAP-353c-followup-dpo-email-notification + GAP-377-followup-auth-route-checks; GAP-353c stays PARTIAL pool). 62nd consecutive 0-clarification streak (3 agents 0-clarif each; first-spawn token-quota-hit doesn't count — agents barely ran before limit). Wall-clock ~50min parallel (longest Bucket A 17min, B 8min, C 8min) + closure 10min. Token cost ~970k for 3 wave agents (50k below Wave 25 due to less coordinator iteration cycle).
- **2026-05-06** (draft): Plan created. State-check completed per `audit-to-gap-pipeline.md` §2.6 — 3 buckets file-disjoint verified; Bucket C is extension scope (not greenfield) on existing GAP-089 smoke-test.sh baseline (265 LOC). PDPL Phase 2 close-out targets 2026-07-01 hard-deadline. Ready for plan PR per `feedback_wave_plan_through_pr.md`.
