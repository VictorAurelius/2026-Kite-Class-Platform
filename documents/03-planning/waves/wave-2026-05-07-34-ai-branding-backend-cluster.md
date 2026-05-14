---
title: Wave 34 — AI Branding Wizard Backend Cluster (contract-first foundation + 7 endpoints + lifecycle service + FE refactor)
status: complete
created: 2026-05-07
updated: 2026-05-07
waves: [34]
gaps: [GAP-272c, GAP-272d, GAP-272e, GAP-272h, GAP-272i, GAP-272j, GAP-272k, GAP-272l]
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave 34 — AI Branding Wizard Backend Cluster

**Goal:** Close 8 sub-letter follow-ups từ Wave 32 v1+rework (GAP-272c/d/e/h/i/j/k/l) — 7 backend endpoints + 1 service integration + FE refactor inline-mocks → MSW handlers. **First wave thực sự áp dụng `.claude/rules/contract-first-for-cross-layer.md` v1.0.0** (rule §7.2 forward-looking self-test → real). Phase D của locked Post-Wave-32 sequence A→B→C→D (per `project_post_wave_32_sequence_plan.md`).

**Trigger:** Wave 32 v1 endpoint proliferation incident → 8 sub-gaps filed → Phase A meta-update PR #894 shipped contract-first rule + Bucket 0 Foundation pattern → Phase C Wave 33 deploy cluster shipped 4/4 P0 BLOCKING gaps → **Phase D unblocked**. Drafted PIPELINED per `feedback_pipelined_wave_planning.md` §Step 5.5 (5th consecutive — waves 28→29, 29→30, 30→31, 31→32, 32→33; Wave 34 = 6th).

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** **HIGH** → model: **Opus 4.7 full effort**. AI Branding feature scope, multi-file refactor, §6 InstanceLifecycleService compliance hinge (P0 GAP-272l). Sonnet crash recurrence pattern không acceptable cho scope này (per `feedback_sonnet_parallel_agent_crash.md` + `feedback_opus_rework_validation.md`).

**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** **YES → Bucket 0 Foundation required** per `contract-first-for-cross-layer.md` v1.0.0. Buckets A/B/C touch BE (`kitehub-branding/**`); Bucket D touch FE (`kitehub-frontend/**`) consume BE endpoints + MSW handlers.

**Estimated wall-clock:** ~30-40 min/agent parallel (Bucket C heaviest = lifecycle service refactor + AuditLog wiring).

---

## 1. Brainstorm

**Q1 (alignment):**
- **Persona:** P2 Center Owner (AI Branding wizard end-user). Wave 34 chuyển wizard từ "scaffold + inline mocks" sang "real endpoints + real lifecycle". P3 Platform Coordinator (admin) cũng benefit qua AuditLog visibility (GAP-272l).
- **Domain:** AI Branding (single-domain backend cluster + cross-layer foundation). Per `post-wave-audit-mandate.md` §2.4 Domain-Milestone Cadence — ELIGIBLE cho `AUDIT_DEFER_DOMAIN_MILESTONE: ai-branding-wizard` IF Wave 34 closes the umbrella; nhưng Phase 1 BETA pressure khuyến nghị run audit ≤3 ngày anyway.
- **Character of work:** Spring Boot REST endpoints (mostly @GetMapping + 1 SSE) + JPA queries + AuditLog wiring (Bucket C) + FE hook abstractions với MSW handlers (Bucket D). Mỗi BE bucket ship 2-3 endpoints — disjoint paths.
- **Why contract-first matters here:** Wave 32 v1 inline mocks (`MOCK_TAKEN_SLUGS`, `TEMPLATE_TO_COLORS`, `STUB_JOB_ID`, mock SSE log) đã đẻ ra 8 sub-gaps vì FE design endpoint shape phỏng đoán. Bucket 0 Foundation locks contract TRƯỚC → Bucket A/B/C implement matching contract → Bucket D consume MSW handlers + real endpoints with shape guarantee.

**Q2 (trade-offs):**
- **Bucket 0 ship FIRST mandate:** Per rule §3 Foundation Bucket — Bucket 0 PR merges TRƯỚC khi spawn A/B/C/D. NOT parallel-with-A. This is the **first time** wave plan enforces non-parallel foundation. Cost: +1 PR cycle (~10 min). Benefit: 4 BE/FE buckets ship matching contract with zero shape conflict.
- **GAP-272l P0 vs others P1:** GAP-272l (real `InstanceLifecycleService` integration) là P0 vì §6 compliance hinge — `LifecycleInline` hiện render `buildMockEvents()` violation `ai-branding-guidelines.md` §6 "State transitions via InstanceLifecycleService ONLY". Mọi bucket khác P1. Coordinator priority: ship C song song với A+B nếu possible.
- **GAP-272k extension vs new endpoint:** GAP-272k extends `GET /api/v1/branding/jobs/{jobId}` response thay vì new endpoint — chỉ thêm `brandColors` field vào existing DTO. Bucket B sẽ touch existing controller (chia sẻ với Bucket A nếu A cũng touch — coordinator verify state-check trước spawn).
- **MSW infra setup là one-time cost:** Bucket 0 ship `kitehub-frontend/src/test/msw/{handlers,server,browser}.ts` skeleton + vitest integration. Subsequent waves consume; Wave 34 D là first consumer.
- **`@AuditLog` discipline (GAP-272l):** lifecycle events POST → AuditLog với actor + entity + timestamp. Pattern existing trong `kiteclass-core` và `kitehub-subscription` (`AuditLogService`); reuse, không invent.
- **Test counts:** mỗi BE bucket ≥3 tests (controller + service + integration); FE Bucket D ≥4 tests (hook abstraction smoke + MSW handler integration smoke).

**Q3 (risks):**
- **R1: AIBrandingController shared edit (Bucket A vs B vs C).** Bucket A adds slug-availability + regenerate (POST + GET) + SSE deploy-stream. Bucket B adds quality-score + preview (HTML) + extends jobs/{id} response (k). Bucket C adds lifecycle/events. Coordinator merge sequential A→B→C; OR split controllers per domain (e.g. `BrandingWizardController` for A endpoints, keep `AIBrandingController` for legacy). **Decision:** ship NEW `BrandingWizardController.java` for Wave 34 endpoints, leaves `AIBrandingController.java` untouched (legacy generate-* endpoints). Disjoint by file.
- **R2: SSE endpoint (272e) infra.** Spring `SseEmitter` pattern. Existing precedent? Check `kitehub-subscription` for any SSE — likely none, this is first. Bucket A agent state-check at runtime; if absent, ship `SseEmitter` + RabbitMQ subscription pattern (deploy events from existing `branding.deploy.*` queue).
- **R3: MSW infra v0 quality.** First MSW setup cho KH-frontend; reference `kiteclass-frontend/src/test/msw/` structure. Bucket 0 agent must read kiteclass version for parity.
- **R4: V29 migration?** `BrandingRegenerateUsage` entity (272d) needs persistence — V29 slot reserved. AuditLog entry shape (272l) chưa rõ — check existing AuditLog table; có thể reuse, không cần V30. Pre-spawn coordinator verify.
- **R5: GAP-272l `kitehub-branding` `InstanceLifecycleService` reference.** Service may live in `kitehub-subscription` (instance lifecycle) thay vì branding. Bucket C state-check at runtime — agent reads existing service layer + reuses, không invent duplicate.

---

## 2. Task Breakdown

| Bucket | Scope | Owner | Effort | Disjoint? |
|--------|-------|-------|--------|-----------|
| **0 Foundation** | api-contract.md update (7 endpoints + 1 extension) + KH-frontend MSW infra (`src/test/msw/{handlers,server,browser}.ts`) + vitest integration | bg-agent (Opus) | ~15-20 min | ✅ `documents/01-business/kitehub/ai-branding/api-contract.md` + `kitehub/kitehub-frontend/src/test/msw/**` + `kitehub-frontend/vitest.config.ts` (MSW setup line) |
| **A BE Bộ 1** | GAP-272i slug-availability + GAP-272d regenerate quota (entity + V29 + endpoints) + GAP-272e SSE deploy-stream | bg-agent (Opus) | ~25-30 min | ✅ `kitehub-branding/src/main/java/.../wizard/{controller,service,dto}/**` + `V29__create_branding_regenerate_usage.sql` |
| **B BE Bộ 2** | GAP-272c quality-score aggregator + GAP-272j iframe preview (HTML) + GAP-272k brandColors extension to jobs/{id} response | bg-agent (Opus) | ~25-30 min | ✅ same `wizard/controller` package (different files: `QualityScoreController`, `PreviewController`); GAP-272k extends existing `BrandingJobResponse` DTO (Bucket B owns DTO edit) |
| **C BE service** | GAP-272l real `InstanceLifecycleService` integration + lifecycle/events endpoint + AuditLog wiring | bg-agent (Opus) | ~30-35 min | ✅ `kitehub-branding/src/main/java/.../wizard/lifecycle/**` + `LifecycleEventsController.java` + AuditLog reuse from existing service |
| **D FE refactor** | GAP-272h convert inline mocks → MSW handlers + custom hooks (`useSlugAvailability`, `useRegenerateQuota`, `useDeployStream`, `useQualityScore`, `usePreview`, `useLifecycleEvents`) | bg-agent (Opus) | ~25-30 min | ✅ `kitehub-frontend/src/components/branding/wizard/**` (replace inline mocks; do NOT touch wizard-shared.tsx reducer signatures) + `src/test/msw/handlers/branding.ts` consume |

**Disjoint check:** Bucket A/B/C all in `kitehub-branding/src/main/java/.../wizard/` but different sub-packages + different files. Shared edit RISK = `BrandingJobResponse` DTO (Bucket B owns the extension; A+C do NOT touch). Migration: V29 = Bucket A only (Bucket C reuses AuditLog table). Foundation Bucket 0's api-contract.md = no other bucket touches — single source of truth.

**Cross-bucket dependency:** Bucket 0 → A+B+C parallel → D last. D depends on A/B/C endpoint code + Bucket 0 MSW handlers (D consumes). Coordinator merge: 0 → (A∥B∥C) → D.

---

## 3. Scope (compact §3 schema — Strategy B+C proven Wave 33)

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** HIGH → model: Opus 4.7 full effort
**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** YES → Bucket 0 Foundation required per `contract-first-for-cross-layer.md`

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 0 | **Foundation** | (api-contract + MSW infra) | 🟠 P1 | `documents/01-business/kitehub/ai-branding/api-contract.md` + `kitehub/kitehub-frontend/src/test/msw/**` + `kitehub-frontend/vitest.config.ts` | **MERGE FIRST** |
| 1 | **A BE Bộ 1** | GAP-272i + GAP-272d + GAP-272e | 🟠 P1 | `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/wizard/{controller,service,entity,dto}/**` + `V29__*.sql` | parallel after Bucket 0 |
| 2 | **B BE Bộ 2** | GAP-272c + GAP-272j + GAP-272k | 🟠 P1 | `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/wizard/quality/**` + `wizard/preview/**` + `dto/BrandingJobResponse.java` (extension) | parallel after Bucket 0 |
| 3 | **C BE service** | GAP-272l (P0 — §6 compliance hinge) | 🔴 P0 | `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/wizard/lifecycle/**` + `LifecycleEventsController.java` | parallel after Bucket 0 |
| 4 | **D FE refactor** | GAP-272h | 🟠 P1 | `kitehub/kitehub-frontend/src/components/branding/wizard/**` (mocks → hooks) + `kitehub-frontend/src/test/msw/handlers/branding.ts` (consume) | LAST (after A+B+C merge) |

### Bucket 0 — Foundation (Contract + Mock Infrastructure)

Per `.claude/rules/contract-first-for-cross-layer.md` v1.0.0:

- **`documents/01-business/kitehub/ai-branding/api-contract.md`** — UPDATE (file exists per state-check). Add 7 endpoints + 1 extension:
  | Method | Path | Purpose | Source GAP |
  |--------|------|---------|:-:|
  | GET | `/api/v1/branding/slug-availability?slug={slug}` | Check slug taken; return `{available: bool, suggestions: string[]}` | 272i |
  | GET | `/api/v1/branding/regenerate-quota` | Current user quota usage; return `{tier, used, limit, resetAt}` | 272d |
  | POST | `/api/v1/branding/jobs/{jobId}/regenerate` | Idempotent regenerate (consumes 1 quota) | 272d |
  | GET | `/api/v1/branding/jobs/{jobId}/deploy-stream` | text/event-stream — SSE deploy log | 272e |
  | GET | `/api/v1/branding/jobs/{jobId}/quality-score` | Aggregator return `{score, contrast, brokenLinks, ...}` | 272c |
  | GET | `/api/v1/branding/jobs/{jobId}/preview` | text/html — iframe-safe live preview | 272j |
  | GET | `/api/v1/branding/instances/{instanceId}/lifecycle/events` | Lifecycle event log (replace `buildMockEvents`) | 272l |
  | (extend) | `GET /api/v1/branding/jobs/{jobId}` | response DTO add `brandColors: BrandColours` field | 272k |

- **MSW infra** — CREATE `kitehub/kitehub-frontend/src/test/msw/{server.ts,browser.ts,handlers/index.ts,handlers/branding.ts}`. Reference `kiteclass/kiteclass-frontend/src/test/msw/` for parity. Vitest integration via `vitest.config.ts setupFiles: ['./src/test/msw/server.ts']`.

- **Acceptance:** api-contract.md có 7 endpoints + 1 extension row, mỗi row schema đầy đủ + error codes; MSW setup compiles + `pnpm test --run` baseline green.

- **Spawn order:** **MERGE FIRST** trước Bucket A/B/C/D.

### Bucket A — BE Bộ 1 (slug + regenerate + SSE)

- Files: `kitehub-branding/src/main/java/com/kitehub/branding/wizard/{controller,service,entity,dto}/` (RELATIVE paths only per `feedback_worktree_absolute_path_contamination.md`)
- New: `BrandingWizardController.java` (3 endpoints), `SlugAvailabilityService.java`, `RegenerateQuotaService.java`, `DeployStreamController.java` (separate vì SSE), `BrandingRegenerateUsage` `@Entity` + repo, `V29__create_branding_regenerate_usage.sql`
- Tests: `BrandingWizardControllerTest` (3 endpoints), `RegenerateQuotaServiceTest` (tier limits FREE 3/PRO 10/PREMIUM 30/ENTERPRISE -1), `DeployStreamSSEIntegrationTest` (mock RabbitMQ subscriber → SSE emit)
- Acceptance: `mvn verify -pl kitehub-branding -am` green; 3 endpoints reachable; V29 Flyway clean
- Cross-layer: endpoint consumption tuân thủ `documents/01-business/kitehub/ai-branding/api-contract.md` (Bucket 0 ship trước)

### Bucket B — BE Bộ 2 (quality + preview + brandColors extension)

- Files: `kitehub-branding/src/main/java/com/kitehub/branding/wizard/quality/**`, `wizard/preview/**`, `dto/BrandingJobResponse.java` (add `brandColors` field)
- New: `QualityScoreController.java`, `QualityScoreAggregator.java` (combine WCAG + asset URL check + visual regression delta from existing service stubs — real measurement deferred GAP-226/227/228), `PreviewController.java` (Thymeleaf-rendered HTML), `BrandColours` value object
- Tests: `QualityScoreControllerTest` (returns aggregated score), `PreviewControllerTest` (HTML response with iframe-safe headers `X-Frame-Options: SAMEORIGIN`), `BrandingJobResponseTest` (brandColors serialization)
- Acceptance: `mvn verify -pl kitehub-branding -am` green; 3 endpoints reachable
- Cross-layer: endpoint consumption tuân thủ contract; controller signature match contract

### Bucket C — BE service (lifecycle integration P0)

- Files: `kitehub-branding/src/main/java/com/kitehub/branding/wizard/lifecycle/**` + `LifecycleEventsController.java`
- New: `LifecycleEventsController.java` (1 endpoint), `LifecycleEventDto.java` (timestamp + state + actor + meta), real lookup via existing `InstanceLifecycleService` (state-check trong `kitehub-subscription` hoặc shared module — agent reads + reuses, NOT duplicates)
- Wiring: AuditLog reuse existing `AuditLogService.recordEvent(actor, entityType, entityId, action, meta)` pattern (state-check at runtime)
- Tests: `LifecycleEventsControllerTest` (returns chronological events), `LifecycleAuditLogIntegrationTest` (verify AuditLog row written on state transition), unit test for DTO mapping
- Acceptance: `mvn verify -pl kitehub-branding -am` green; endpoint replaces `buildMockEvents()` mock pattern in FE LifecycleInline (Bucket D consumes)
- §6 compliance: lifecycle events sourced from `InstanceLifecycleService` ONLY (per `ai-branding-guidelines.md` §6); zero fabrication

### Bucket D — FE refactor (inline mocks → MSW + hooks)

- Files: `kitehub-frontend/src/components/branding/wizard/**` (replace inline mocks; do NOT touch wizard-shared.tsx reducer signatures), `src/test/msw/handlers/branding.ts` (consume Bucket 0 MSW infra)
- Targets to replace: `MOCK_TAKEN_SLUGS` (272i), `TEMPLATE_TO_COLORS` (272k → use brandColors from API), `mockChecks()` quality (272c → call quality-score endpoint), iframe data: URI (272j → fetch preview endpoint), mock SSE log (272e → connect deploy-stream), `buildMockEvents()` (272l → call lifecycle/events endpoint), regenerate-quota mock (272d)
- New hooks: `useSlugAvailability`, `useRegenerateQuota`, `useDeployStream` (EventSource), `useQualityScore`, `usePreview`, `useLifecycleEvents`
- MSW handlers: `branding.ts` cover all 7 endpoints with happy-path + 1 error case each
- Tests: hook abstraction smoke (each hook returns expected shape against MSW), wizard component integration smoke (7 components consume hooks instead of inline mocks)
- Acceptance: `pnpm exec tsc --noEmit` clean; `pnpm test --run` wizard tests pass (target ≥67 = Wave 32 baseline + integration); `pnpm build` clean
- Cross-layer: endpoint consumption tuân thủ contract; mock data via MSW handlers, KHÔNG inline

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Lookup | Verdict |
|--------|--------|---------|
| `documents/01-business/kitehub/ai-branding/api-contract.md` | `find documents/01-business -name api-contract.md \| grep ai-branding` | ✅ exists — Bucket 0 UPDATE (add 7 endpoints + 1 extension) |
| `kitehub-branding/src/main/java/.../AIBrandingController.java` | grep — has 4 POST endpoints (analyze-logo, generate-image, generate-text, generate-theme) | ✅ exists; Wave 34 buckets ship NEW `BrandingWizardController.java` separate file (no shared edit) |
| `kitehub-frontend/src/test/msw/` | `ls kitehub/kitehub-frontend/src/test/` | ❌ ABSENT — Bucket 0 🆕 to-be-created (per `feedback_kitehub_frontend_msw_missing.md`) |
| `kiteclass-frontend/src/test/msw/` | `ls kiteclass/kiteclass-frontend/src/test/msw/` | ✅ exists (verify at agent runtime — reference pattern for KH parity) |
| `BrandingWizardController.java` | grep | ❌ → 🆕 Bucket A creates |
| `BrandingRegenerateUsage` entity + V29 | `ls kitehub-branding/src/main/resources/db/migration/V*.sql` | ⚠️ agent verify — V29 likely free; if collision use V30 (briefing must mention) |
| `LifecycleEventsController.java` | grep | ❌ → 🆕 Bucket C |
| `InstanceLifecycleService` | grep `kitehub-subscription/`, `kitehub-branding/` | ⚠️ agent verify location at runtime; reuse, do not duplicate |
| `AuditLogService` reusable | grep — exists in shared module | ⚠️ agent verify at runtime |
| `QualityScoreController.java` + `PreviewController.java` | grep | ❌ → 🆕 Bucket B |
| GAP-272{c,d,e,h,i,j,k,l} files | `ls documents/04-quality/gaps/GAP-272[c-l]*.md` | ✅ all 8 exist |
| `kitehub-frontend/vitest.config.ts` | `ls` | ✅ exists — Bucket 0 extends with MSW setupFiles |
| `BrandingJobResponse` DTO | grep `kitehub-branding/` for response classes | ⚠️ agent verify name; Bucket B extends with `brandColors` field |

Pre-spawn coordinator verify: Wave 33 closure SHIPPED + Phase A meta-update PR #894 merged + `mvn verify` baseline clean.

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | Notes |
|--------|---------------------|-------|
| 0 | `pnpm -F @kite/kitehub-frontend test --run` (baseline before MSW handler implementations) + `bash scripts/check-docs.sh` (api-contract.md format) | Foundation must compile + tests baseline pass |
| A | `mvn verify -pl kitehub/kitehub-branding -am` | Focus: 3 new controllers + V29 migration + tier-limit quota tests |
| B | `mvn verify -pl kitehub/kitehub-branding -am` | Focus: quality aggregator + preview HTML + brandColors DTO extension |
| C | `mvn verify -pl kitehub/kitehub-branding -am` | Focus: AuditLog integration test + InstanceLifecycleService reuse (no duplicate) |
| D | `pnpm -F @kite/kitehub-frontend type-check && test --run && build` | Both layers per `feedback_agent_local_verify_both_layers.md`; focus: 6 hook abstractions + MSW handler integration smoke |

Coordinator post-merge: `mvn -f kitehub/pom.xml verify --fail-at-end` (multi-module) + `pnpm -F @kite/kitehub-frontend build`.

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md` + `contract-first-for-cross-layer.md`:

- **Spawn order:** Bucket 0 FIRST (sequential merge) → Bucket A∥B∥C parallel after Bucket 0 merged → Bucket D LAST after A+B+C merged
- **Total agent spawns:** 1 + 3 + 1 = **5 agents across 3 spawn batches** (not 5 in single batch)
- All `run_in_background: true` + `isolation: worktree`
- **RELATIVE paths only** + **CWD guard mandate** (per `feedback_worktree_absolute_path_contamination.md` recurrence #2 + Phase A meta-update)
- Coordinator briefing per bucket include: V29 migration assignment (Bucket A only), `BrandingJobResponse` DTO ownership (Bucket B only — A+C don't touch), `InstanceLifecycleService` reuse mandate (Bucket C — no duplicate)
- Model tier: **Opus 4.7 full effort** (HIGH stake per §1 brainstorm)
- Token timing: spawn AFTER Wave 33 closure SHIPPED + token budget verify per `feedback_token_quota_spawn_timing.md`. `/clear` recommended nếu cùng session với Wave 33 closure.

**Domain-milestone audit:** Wave 34 single-domain (AI Branding) → ELIGIBLE `AUDIT_DEFER_DOMAIN_MILESTONE: ai-branding-wizard` IF Wave 34 closes umbrella. **Decision:** run audit ≤3 ngày anyway because Phase 1 BETA pressure + first contract-first wave (validate rule §7.2 self-test became real).

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:

- Mỗi bucket PR update sub-gap file Log entry tương ứng
- **Status flips post-Wave-34:**
  - GAP-272i: 🔵 OPEN → 🟢 DONE (slug-availability endpoint shipped + FE consumes)
  - GAP-272d: 🔵 OPEN → 🟢 DONE (regenerate quota tracking + endpoint + tier limits)
  - GAP-272e: 🔵 OPEN → 🟢 DONE (SSE deploy-stream endpoint)
  - GAP-272c: 🔵 OPEN → 🟢 DONE (quality-score aggregator endpoint)
  - GAP-272j: 🔵 OPEN → 🟢 DONE (iframe preview render endpoint)
  - GAP-272k: 🔵 OPEN → 🟢 DONE (brandColors extension to jobs/{id})
  - GAP-272l: 🔵 OPEN → 🟢 DONE (real lifecycle integration + AuditLog wiring)
  - GAP-272h: 🔵 OPEN → 🟢 DONE (inline mocks → MSW handlers + hook abstractions)
  - **GAP-272 umbrella:** 🟡 PARTIAL → ⚠️ stays PARTIAL until 272f (visual regression baseline) + 272g (E2E happy path test) — those are testing follow-ups, separate scope
- ROADMAP §🚀 Next Action update — Wave 34 row prepended; previous Wave 33 demoted
- **Release Plan Progress section** (per `feedback_wave_closure_release_progress_report.md`):
  - Current Phase: Phase 1 BETA (v0.9.0-beta target)
  - Cluster status: AI Branding wizard backend = ✅ DONE (8 sub-letters c-l closed; only 272f+272g testing follow-ups remain)
  - Track 2: 2/7 kits (status unchanged)
  - PDPL deadline countdown 2026-07-01 (~7 tuần from 2026-05-07)
  - Next gate: Phase 1 BETA P0 BLOCKING gaps now PARTIAL post Wave 33 (user execution pending) + Wave 35 candidate = beta tenant onboarding OR remaining kit ports
- Wave plan frontmatter `status: complete` flip
- `wave-history.jsonl` append (per `feedback_wave_history_append_required.md`)
- `bash scripts/prune-merged-worktrees.sh --yes` (post all 5 PRs merged)
- AUDIT trailer: optional `AUDIT_DEFER_DOMAIN_MILESTONE: ai-branding-wizard` IF Wave 34 closes umbrella (decision at closure time); else run audit ≤3 ngày

**Follow-up gaps (intentional carry-over):**
- GAP-272f — visual regression baseline (testing follow-up; out of Wave 34 backend scope)
- GAP-272g — E2E happy path welcome→deploy (testing follow-up)
- Wave 35 candidates: GAP-371 CDN Cloudflare + GAP-373 status page + GAP-380 staging environment + GAP-374 tag-CI automation

**Self-test validation (rule contract-first-for-cross-layer.md §7.2):**
Wave 34 = first wave thực sự áp dụng rule. Closure PR §"Self-test validation" so sánh:
- Predicted (rule §7.2 forward-looking): "≤1-2 sub-gap follow-up expected (vs 8 ad-hoc của Wave 32)"
- Actual: số sub-gap follow-up filed sau Wave 34 = ?
- Outcome: rule effective khi actual ≤2

---

## 8. Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| R1 — V29 collision | Coordinator pre-spawn verify `ls db/migration/V*.sql`; brief A explicitly "V29 reserved" |
| R2 — `InstanceLifecycleService` location ambiguity | Bucket C agent state-check at runtime — search `kitehub-subscription/` + `kitehub-branding/` + shared modules; reuse, don't duplicate |
| R3 — `BrandingJobResponse` DTO shared edit | Bucket B explicit owner; A+C state-check at runtime + skip if existing field name conflicts |
| R4 — MSW infra v0 quality | Bucket 0 reference `kiteclass-frontend/src/test/msw/` for parity (state-check verify at runtime) |
| R5 — SSE infra absent | Bucket A SSE handler uses `SseEmitter` + RabbitMQ subscription pattern; if absent, ship pattern + 1 integration test |
| R6 — Token quota mid-wave | Spawn LATE in fresh session post-`/clear`; HIGH-stakes Opus = larger token budget per agent. Per `feedback_token_quota_spawn_timing.md` |
| R7 — Phase A rule misinterpreted | Bucket 0 agent prompt cite rule §3 explicitly; State-Check Evidence section §4 above pre-validates eligibility |

---

## 9. Log

- **2026-05-07 (SHIPPED):** All 5 buckets merged: PR #905 (Bucket 0), #907 (A), #906 (B), #908 (C), #910 (D). Self-test §7.2 of `contract-first-for-cross-layer.md` rule: predicted ≤2 sub-gap follow-ups vs Wave 32 v1's 8 — actual = 2 new (GAP-272n shape mismatch P2, GAP-272o orchestrator wiring P1). Rule effectiveness confirmed. 5 sub-letters DONE (272d/h/i/j/l), 3 PARTIAL (272c/e/k). Side-tasks same session: GAP-382 admin scan drift detector PR #909 + GAP-383 worktree-prune detached-HEAD PR #903. Wall-clock ~52min total (Bucket 0 6min + A∥B∥C 11min longest + D 19.5min + coordinator overhead). All buckets Opus 4.7 full effort — zero scaffold-as-DONE, zero CI fail on first push.
- **2026-05-07 (draft):** Plan drafted PIPELINED trong khi Wave 33 Bucket C agent in-flight + Bucket A/B/D đã merge. 6th consecutive `wave-pack-planner` §Step 5.5 pipelined application (waves 28→29, 29→30, 30→31, 31→32, 32→33, 33→34). **First wave thực sự áp dụng `contract-first-for-cross-layer.md` v1.0.0** — Bucket 0 Foundation pattern locked; rule §7.2 forward-looking self-test → real validation post-closure. State-check confirmed: api-contract.md AI Branding exists (UPDATE not CREATE), KH-frontend MSW absent (Bucket 0 creates), AIBrandingController has 4 legacy endpoints (Wave 34 ships NEW BrandingWizardController separate file — no shared edit). 8 sub-letters c-l mapped to 5 buckets (0/A/B/C/D); GAP-272f+272g testing follow-ups deferred. Stake HIGH → Opus 4.7 full effort mandate per `feedback_opus_rework_validation.md`. Spawn pattern non-standard: 0 → (A∥B∥C) → D with 3 sequential spawn batches (not 5 parallel single batch) per contract-first rule mandate. Reviewer: @nguyenvankiet (solo-dev — wave plan PR follows `feedback_wave_plan_through_pr.md`, not direct push).
