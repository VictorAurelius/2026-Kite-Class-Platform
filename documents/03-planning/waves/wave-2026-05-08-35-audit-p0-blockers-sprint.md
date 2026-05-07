---
title: Wave 35 — Audit P0 Blockers Sprint (5 P0 from 2026-05-07 audit cluster)
status: complete
created: 2026-05-07
updated: 2026-05-07
waves: [35]
gaps: [GAP-384, GAP-385, GAP-386, GAP-387, GAP-392, GAP-393]
---

# Wave 35 — Audit P0 Blockers Sprint

**Goal:** Resolve 5 P0 BLOCKERS từ 2026-05-07 audit cluster để unblock Phase 1 BETA launch (Quality 73→80, 0 P0 incidents gate).
**Trigger:** Audit cluster (PR #913) phát hiện 5 P0 chặn `release-1-plan-2026.md` Phase 1 trigger gate; PDPL deadline 2026-07-01 (~7 tuần countdown).
**Estimated wall-clock:** ~12-15h dev parallel, longest-bucket Bucket B PDPL consent ~3h. With 5 background agents Opus 4.7 → ~30-45min wall-clock.

---

## 1. Brainstorm

**Q1 (alignment):** Phase 1 BETA launch v0.9.0-beta — 4 Tier 1 personas (P1/P2/P3/P5) đều cần beta-signup flow secure + observable; Wave 33 ship code-complete nhưng audit phát hiện 4 deploy-blocking gaps + 1 production-critical Performance N+1.

**Q2 (trade-offs):**
- **Reject** "fix sequentially per-PR" — 5 disjoint scope, parallel agents tiết kiệm 70% wall-clock per `feedback_parallel_agent_strategy.md`
- **Reject** combine với P1 hardening (Wave 36) — context heavy, mix priority sẽ delay P0 ship
- **Reject** defer GAP-385 PDPL consent — PDPL hard deadline 2026-07-01, không thể trượt
- **Accept** Bucket 0 Foundation cho api-contract.md vì GAP-385 cross-layer (FE form + BE migration) per `contract-first-for-cross-layer.md` v1.0.0

**Q3 (risks):**
- **R1:** GAP-385 PDPL consent có thể conflict với GAP-372 existing beta-signup flow trong cùng `BetaAccessController` → Bucket B cần coordinate carefully với existing tests
- **R2:** GAP-392 N+1 fix cần V31 migration; collision với GAP-393 (P1 cluster Wave 36) cùng V31 — mitigated bằng cách Wave 35 V31 includes BOTH `organization_name` + `status` indexes (gộp scope)
- **R3:** GAP-387 metric counters yêu cầu MeterRegistry inject — verify existing pattern trong `kitehub-subscription`
- **R4:** Token-quota-hit risk khi spawn 5 agents cuối session — spawn EARLY trong fresh session (per `feedback_token_quota_spawn_timing.md`)

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| 0 | api-contract.md update + MSW handler stub cho consent field | bg-agent | 30min | ✅ docs only |
| A | GAP-384 admin auth `@PreAuthorize` | bg-agent (Opus) | 1h | ✅ kitehub-subscription Java only |
| B | GAP-385 PDPL consent flow (FE form + BE migration + DTO) | bg-agent (Opus) | 3h | ✅ cross-layer FE+BE đồng module |
| C | GAP-386 quality threshold externalize | bg-agent (Opus) | 2h | ✅ kitehub-branding Java + application.yml |
| D | GAP-387 beta metric counters | bg-agent (Opus) | 3h | ✅ kitehub-subscription BetaAccessService |
| E | GAP-392 N+1 slug findAll() + V31 index migration | bg-agent (Opus) | 2h | ✅ kitehub-branding Java + V31 SQL |

Disjoint check: Bucket A/B/D touch `kitehub-subscription`; A=controller method-level, B=DTO+migration+FE form, D=service-level. Bucket C/E touch `kitehub-branding` different files (C=QualityScoreAggregator, E=SlugAvailabilityService+repo). No file-level conflicts.

---

## 3. Scope

**Stake tier:** HIGH (5 P0 BLOCKERS Phase 1 launch + production-critical N+1) → model: **Opus 4.7 full** per `feedback_sonnet_parallel_agent_crash.md`
**Cross-layer?** YES (GAP-385 cross-layer FE+BE) → Bucket 0 Foundation required per `contract-first-for-cross-layer.md`

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 0 | **Foundation** | api-contract update + MSW stub | 🟠 P1 | `documents/01-business/kitehub/beta-access/api-contract.md` (CREATE if absent) + `kitehub-frontend/src/test/msw/handlers/beta-access.ts` extend | **MERGE FIRST** |
| 1 | **A** | GAP-384 admin auth | 🔴 P0 | `kitehub-subscription/.../beta/controller/BetaAccessController.java` (3 endpoints) + Spring config | parallel after Bucket 0 |
| 2 | **B** | GAP-385 PDPL consent | 🔴 P0 | `kitehub-subscription/.../beta/dto/BetaRequestDto.java` + `entity/BetaAccessRequest.java` + `db/migration/V31__beta_request_add_consent.sql` + `kitehub-frontend/src/components/beta/BetaRequestForm.tsx` | parallel after Bucket 0 |
| 3 | **C** | GAP-386 quality threshold | 🔴 P0 | `kitehub-branding/.../wizard/quality/QualityScoreAggregator.java` + `kitehub-branding/src/main/resources/application.yml` + `infrastructure/helm/.../values.yaml` + `documents/01-business/kitehub/ai-branding/rules.md` | parallel after Bucket 0 |
| 4 | **D** | GAP-387 beta metrics | 🔴 P0 | `kitehub-subscription/.../beta/service/BetaAccessService.java` (Micrometer Counter) + alert rules in `infrastructure/helm/.../alerts.yaml` | parallel after Bucket 0 |
| 5 | **E** | GAP-392 N+1 slug findAll() | 🔴 P0 | `kitehub-branding/.../wizard/service/SlugAvailabilityService.java` + `BrandingJobRepository.java` + `db/migration/V31__index_branding_job_organization_name_and_status.sql` (combine với GAP-393 status index để tránh V31 conflict) | parallel after Bucket 0 |

### Bucket 0 — Foundation (Contract + Mock Infrastructure)

Per `contract-first-for-cross-layer.md` v1.0.0 — only GAP-385 cross-layer:
- Files: `documents/01-business/kitehub/beta-access/api-contract.md` (verify existence; add `consent_given` field schema + error code `BETA_CONSENT_REQUIRED`); `kitehub-frontend/src/test/msw/handlers/beta-access.ts` (verify exists; add stub for consent field)
- **Acceptance:** api-contract.md liệt kê field `consent_given: boolean` trong `BetaRequestDto` schema + error code; MSW handler returns 400 `BETA_CONSENT_REQUIRED` khi missing
- **Spawn order:** MERGE FIRST → sau đó A/B/C/D/E parallel

### Bucket A — Admin endpoint authentication guard (GAP-384)

- Files (RELATIVE):
  - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/controller/BetaAccessController.java` — add `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` trên `listRequests`, `approve`, `reject`
  - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/config/SecurityConfig.java` (or equivalent) — `@EnableMethodSecurity`
  - `kitehub/kitehub-subscription/src/test/java/com/kitehub/subscription/beta/controller/BetaAccessControllerTest.java` — add tests cho 401/403/200 paths
- Tests: 3 new tests (unauth 401, wrong role 403, PLATFORM_ADMIN 200)
- Acceptance: GAP-384 §AC bullets 1-5 ticked
- Update controller javadoc replacing "Guarded at gateway level" → "Guarded at controller via @PreAuthorize"

### Bucket B — PDPL 2023 consent flow (GAP-385)

- Files (RELATIVE):
  - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/dto/BetaRequestDto.java` — add `consentGiven: boolean` + `@AssertTrue(message="BETA_CONSENT_REQUIRED")`
  - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/entity/BetaAccessRequest.java` — add `consent_given: boolean NOT NULL` + `consent_at: OffsetDateTime`
  - `kitehub/kitehub-subscription/src/main/resources/db/migration/V31__beta_request_add_consent.sql` — ALTER TABLE ADD COLUMN
  - `kitehub/kitehub-frontend/src/components/beta/BetaRequestForm.tsx` (or equivalent) — required checkbox + privacy/terms link + submit gate
  - `kitehub/kitehub-subscription/src/test/.../BetaAccessControllerTest.java` — test missing consent → 400 BETA_CONSENT_REQUIRED
- Tests: 2 new BE (missing consent 400, true 201) + 1 new FE (checkbox required → submit disabled)
- Acceptance: GAP-385 §AC bullets 1-9 ticked
- Audit log entry `beta.consent.given` qua existing outbox emitter
- Endpoint consumption tuân thủ schema trong `documents/01-business/kitehub/beta-access/api-contract.md` (Bucket 0 ship trước)

### Bucket C — Quality gate threshold externalize (GAP-386)

- Files (RELATIVE):
  - `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/wizard/quality/QualityScoreAggregator.java` — replace `private static final int THRESHOLD = 70;` với `@Value("${quality-gate.pass-threshold:70}") private int threshold;`
  - `kitehub/kitehub-branding/src/main/resources/application.yml` — add `quality-gate.pass-threshold: 70`
  - `infrastructure/helm/kitehub-branding/values.yaml` — expose `qualityGate.passThreshold` override
  - `documents/01-business/kitehub/ai-branding/rules.md` — append BR-QUALITY-001 5-attribute compliance block
  - `kitehub/kitehub-branding/src/test/.../QualityScoreAggregatorTest.java` — 2 new tests (custom threshold 80 fail/pass; missing config fallback 70)
- Tests: 2 new
- Acceptance: GAP-386 §AC bullets 1-7 ticked

### Bucket D — Beta metric counters (GAP-387)

- Files (RELATIVE):
  - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/service/BetaAccessService.java` — inject `MeterRegistry`; build 4 Counters (signup_requests / approvals / rejections / honeypot_rejections); `persona` tag dimension
  - `infrastructure/helm/kitehub-subscription/templates/alerts.yaml` (or equivalent) — 2-3 alert rules với runbook_url placeholders
  - `kitehub/kitehub-subscription/src/test/.../BetaAccessServiceTest.java` — verify counters increment per service call
- Tests: 4 new (1 per counter)
- Acceptance: GAP-387 §AC bullets 1-6 ticked

### Bucket E — N+1 slug findAll() + V31 index (GAP-392 + index portion of GAP-393)

- Files (RELATIVE):
  - `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/wizard/service/SlugAvailabilityService.java` — replace `findAll().stream()...` với `jobRepository.existsBySlug(normalized)`
  - `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/repository/BrandingJobRepository.java` — `@Query("... existsBySlug")`
  - `kitehub/kitehub-branding/src/main/resources/db/migration/V31__index_branding_job_organization_name_and_status.sql` — CREATE INDEX trên `LOWER(organization_name)` + `status` (gộp với GAP-393 status index để tránh V31 collision)
  - `kitehub/kitehub-branding/src/test/.../SlugAvailabilityServiceTest.java` — performance test (1000 jobs → <50ms)
- Tests: 1 new perf test + verify EXPLAIN ANALYZE hit index
- Acceptance: GAP-392 §AC bullets 1-7 ticked + portion of GAP-393 §393-C ticked (status index)

⚠️ **V31 collision avoidance:** Bucket E V31 includes BOTH `organization_name` + `status` indexes. Bucket B uses V32 cho consent migration (offset từ V31).

| Bucket | Migration version |
|--------|:-----:|
| B (consent) | V32 |
| E (indexes) | V31 |

Coordinator confirms version order at agent spawn time.

---

## 4. State-Check Evidence

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `BetaAccessController` | Java class | `grep -rn "BetaAccessController" kitehub/kitehub-subscription/src` | 2 files (controller + test) | ✅ exists |
| `@PreAuthorize` | Spring annotation | `grep -rn "@PreAuthorize" kitehub/kitehub-subscription/src/main` | 0 matches | 🆕 to-be-created (Bucket A) |
| `BetaRequestDto.consentGiven` | DTO field | `grep -rn "consentGiven\|consent_given" kitehub/kitehub-subscription/src` | 0 matches | 🆕 to-be-created (Bucket B) |
| `V31__*` migration | Flyway | `ls kitehub/kitehub-subscription/src/main/resources/db/migration/V31* kitehub/kitehub-branding/src/main/resources/db/migration/V31*` | 0 files | 🆕 to-be-created (Bucket E V31; Bucket B V32) |
| `QualityScoreAggregator.THRESHOLD` | Java constant | `grep -rn "THRESHOLD\s*=\s*70" kitehub/kitehub-branding/src/main/java` | 1 match `wizard/quality/QualityScoreAggregator.java:33` | ✅ exists (to refactor) |
| `quality-gate.pass-threshold` | Config key | `grep -rn "quality-gate.pass-threshold" kitehub/kitehub-branding/src/main/resources` | 0 matches | 🆕 to-be-created (Bucket C) |
| `BetaAccessService` | Java class | `grep -rn "class BetaAccessService" kitehub/kitehub-subscription/src/main/java` | 1 match | ✅ exists |
| `MeterRegistry` import | Micrometer | `grep -rn "import io.micrometer.core.instrument.MeterRegistry" kitehub/kitehub-subscription/src` | TBD-verify-at-spawn | ✅ likely (Spring Boot Actuator standard) |
| `SlugAvailabilityService.isTaken` | Java method | `grep -rn "isTaken\|findAll().stream" kitehub/kitehub-branding/src/main/java/com/kitehub/branding/wizard/service/SlugAvailabilityService.java` | 1 match line 85 | ✅ exists (to refactor) |
| `BrandingJob.organization_name` index | DB index | `grep -i "organization_name" kitehub/kitehub-branding/src/main/resources/db/migration/V*.sql` | 0 matches | 🆕 to-be-created (Bucket E V31) |
| `BrandingJob.status` index | DB index | `grep -i "idx.*status\|index.*status" kitehub/kitehub-branding/src/main/resources/db/migration/V*.sql` | 0 matches | 🆕 to-be-created (Bucket E V31) |
| `documents/01-business/kitehub/beta-access/api-contract.md` | Cross-layer contract | `ls documents/01-business/kitehub/beta-access/api-contract.md` | TBD-verify-at-spawn | ✅ likely existing OR 🆕 to-be-created (Bucket 0 Foundation) |
| `kitehub-frontend/src/test/msw/handlers/beta-access.ts` | MSW handler | `ls kitehub/kitehub-frontend/src/test/msw/handlers/beta-access*` | TBD-verify-at-spawn | ✅ likely existing OR 🆕 to-be-created (Bucket 0 Foundation) |

Banned shortcuts confirmed avoided: full grep output read (no `| head`); cross-checked file name + class name + JSX selector + i18n key per `audit-to-gap-pipeline.md` §2.6.

---

## 5. Verification Gates

| Bucket | Local verify | CI gate |
|--------|---------------|---------|
| 0 | `pnpm -F kitehub-frontend test:unit -- handlers/beta-access` | frontend-ci |
| A | `mvn -pl kitehub-subscription test -Dtest='BetaAccessControllerTest'` | core-ci |
| B | `mvn -pl kitehub-subscription verify` + `pnpm -F kitehub-frontend test:unit -- BetaRequestForm` + `pnpm -F kitehub-frontend build` | core-ci + frontend-ci |
| C | `mvn -pl kitehub-branding test -Dtest='QualityScoreAggregator*'` | core-ci |
| D | `mvn -pl kitehub-subscription test -Dtest='BetaAccessService*'` | core-ci |
| E | `mvn -pl kitehub-branding test -Dtest='SlugAvailability*'` + Flyway local verify | core-ci |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md` + `feedback_sonnet_parallel_agent_crash.md`:
- Bucket 0 spawn FIRST (sequential), wait for merge, then A/B/C/D/E parallel
- All 5 P0 buckets: **Opus 4.7** model (HIGH-stakes per audit cluster recurrence pattern)
- `run_in_background: true` mọi agents
- `isolation: worktree` cho parallel safety
- RELATIVE paths in agent prompts
- Coordinator merges sequentially A→B→C→D→E sau khi all 5 background complete

---

## 7. Closure Protocol

- Each bucket PR updates GAP file Log + status flip 🔵 OPEN → 🟢 DONE (per `gap-done-discipline.md` §2)
- ROADMAP §🚀 Next Action updated trong closure PR
- Wave plan `status: complete` flip
- `wave-history.jsonl` append (Rule 15 enforcement)
- `bash scripts/prune-merged-worktrees.sh --yes` after all 6 PRs (0+A+B+C+D+E) merged
- **Re-run audit cluster** (5 specialist + Quality + Performance) post-Wave-35 merge → verify ≥80 Quality

---

## 8. Log

- **2026-05-07** (draft): Plan created post-audit-cluster (PR #913). Pairs với Wave 36 plan (P1 hardening) trong cùng plan PR. Spawn ETA: next session early (per `feedback_token_quota_spawn_timing.md` spawn EARLY).
- **2026-05-07** (SHIPPED + complete): All 5 P0 BLOCKERS DONE in single session ~95min wall-clock. PRs: #916 Bucket 0 + #922 A + #921 B + #919 C + #920 D + #918 E. Side PRs: #923 refactor Oracle→AWS Singapore (ADR-025) + #924 CI path-filter fix. 1 rebase conflict (Bucket D BetaAccessService constants — additive resolve). 1 worktree contamination during Bucket C (recovered cleanly per `feedback_session_resume_cross_contamination.md`). Cross-layer wave (GAP-385) validated `contract-first-for-cross-layer.md` v1.0.0 self-test §7.2 again: 0 ad-hoc sub-gap follow-ups. GAP-393 stays 🟡 PARTIAL (status index portion shipped here; 393-A/B/D scheduled Wave 36 Bucket D). Compliance posture flipped: Oracle (VN data localization) → AWS Singapore (compliance debt accepted Phase 1 invite-only). 71st consecutive 0-clarification streak. Counts: 169 → 168 OPEN.
