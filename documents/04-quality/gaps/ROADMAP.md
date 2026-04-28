# Gaps Roadmap — Epic-Based Organization

**Mục tiêu:** Biến 103 gaps thành actionable roadmap với epics + dependencies + sprints.

> **Khi nào đọc file này thay vì README.md?**
> - README: flat index, tra cứu 1 gap
> - ROADMAP: execution planning, sprint planning, dependency check

---

## 🎯 Current Status Snapshot (2026-04-24)

**2026-04-28 (Triage — 4 follow-up gaps filed post-Wave GAP-236 + IDE warning incident):** Per `audit-to-gap-pipeline.md` Step 2.5 state-check:
- **GAP-245** P1 Meta — CI does not enforce IDE warnings (deprecation/unused/raw types). Process gap surfaced after PR #605 closed 8 shipped warnings; memory rule alone is insufficient enforcement layer per `feedback_incident_to_rule_pipeline.md` 5-stage pipeline.
- **GAP-246** P3 — delete unused `kiteclass-frontend/src/components/ui/calendar.tsx` (dead post-Wave 7-Perf attendance migration; Agent B finding). 1-line PR.
- **GAP-247** P2 — HCaptcha `next/dynamic` wrapper with forwardRef + useImperativeHandle for KH `/register` (~80 KB potential First Load JS win; Agent D revert documented).
- **GAP-248** P2 — KC `(auth)/layout.tsx` provider chunk hoist refactor (131 KB common chunk Agent A flagged); investigate-then-decide via `bundle-analyzer-baseline-kc.html` trace.

Counts: **86 OPEN → 90 OPEN** (+GAP-245/246/247/248 filed; all post-wave triage).

**2026-04-28 (Wave GAP-236 SHIPPED — 4 parallel agents, ~18 min wall-clock):** Per `feedback_parallel_agent_strategy.md` + `feedback_wave_plan_through_pr.md` (wave plan PR-first landed in #599). 4 worktree-isolated agents on disjoint FE buckets, 0 file conflicts (only additive Log conflicts on GAP-236 file resolved by parent rebase):

| Agent | Bucket | PR | Pages |
|:-----:|--------|:--:|:-----:|
| A | KC `(auth)` + `(public)` | #601 | 7 (top 3 auth routes −119 KB First Load JS) |
| B | KC `(dashboard)/{admin,attendance,billing}` | #600 | 5 (incl. `/attendance/reports` 417-LOC) |
| C | KC `(dashboard)/{classes,courses,students,teachers}` | #602 | 11 (largest bucket — form/attendance lazy) |
| D | KH all groups + Sub-PR C analyzer baselines | #603 | 10 (incl. `/admin/instances/[id]` 452-LOC) |

**Wave validation:**
- Total **33 pages converted** (≥30 AC threshold ✅)
- Per-app post-wave max First Load JS: KC 217 KB / KH 200 KB (well under 250 KB CI ceiling)
- All 90 routes (52 KC + 38 KH) within bundle budget
- 565 KC tests + 484 KH tests pass; 0 regression
- Sub-PR C: analyzer baseline HTMLs committed (KC 749 KB + KH 876 KB raw, both <5 MB so no compression)
- 3 follow-up findings surfaced for triage (unused `ui/calendar.tsx`, HCaptcha ref-forwarding gap, `(auth)/layout.tsx` chunk hoist)
- ~18 min wall-clock for 4 agents (vs estimated 1-2h serial)

GAP-127 PARTIAL → 🟢 effectively closed via GAP-236 closure. GAP-236 status: 🟡 PARTIAL → 🟢 DONE. Counts: **87 OPEN → 86 OPEN** (-GAP-236 closed).

**2026-04-28 (GAP-244 SHIPPED + dev profile cleanup):** Path A migration `V46__align_audit_columns_to_bigint.sql` ALTERs `created_by` / `updated_by` from VARCHAR to BIGINT across 19 V28..V44 tables, matching `BaseEntity` (Long). Idempotent DO block keyed on `information_schema.columns.data_type`; Wave02MigrationsTest extended with column-type assertion. PR #597 + PR #598 (revert `application-dev.yml` Flyway+create-drop workaround now Flyway+validate path is viable). 1123/1123 kiteclass-core tests green. Counts: **88 OPEN → 87 OPEN** (-GAP-244 closed).

**2026-04-27 (GAP-235 wave SHIPPED — 4 sub-PRs serial in single session, ~3h):** AI Branding mock-data wave fully closed. Sub-PR E1 #588 (OpenAPI export pipeline + `kiteclass/shared/` fixtures starter, fixed MockMvc-vs-springdoc + test-resources application.yml override bugs in test), Sub-PR F #589 (BE `BrandingDataSeeder` `@Profile("dev")` idempotent, 4 unit tests), Sub-PR E2 #590 (FE MSW v2 handlers — 11 endpoints, lifecycle state machine, ETag/304, 15 vitest tests), Sub-PR G #591 (`local-dev-mock-data.md` guide + `smoke-ai-branding-dev.sh` shellcheck-clean + `ai-branding-demo.spec.ts` Playwright spec gated by `AI_BRANDING_DEMO_RUN=1`). Live screenshot capture deferred — surfaced **GAP-244** (V29+ migrations declare `created_by VARCHAR(100)` while `BaseEntity.createdBy` is `Long`, sibling case to `feedback_jpa_jsonb_jdbctypecode.md`); **PR #592 ships dev-profile workaround** (application-dev.yml ddl-auto override + dev-start.sh dev-profile activation + `INTERNAL_API_SECRET` default) so Core boots in ~7s on fresh DB; root canonicalization tracked in GAP-244. Counts: **89 OPEN → 90 OPEN** (-GAP-235 closed; +GAP-244 filed). GAP-235 had 4 sub-PRs all merged in this session (E1/E2/F/G), GAP-244 is followup work.

**2026-04-27 (Wave P2-Cleanup SHIPPED — 3 parallel agents, ~12 min wall-clock):** Per `feedback_parallel_agent_strategy.md` + `feedback_wave_plan_through_pr.md` (wave plan via PR first — landed in #581). 3 worktree-isolated agents disjoint files, 0 conflicts:

| Agent | Gap | PR | Result |
|:-----:|-----|:--:|--------|
| A | GAP-234 architecture/diagram drift | #582 | 🟢 DONE — 8 files updated; 11 v2 entities added to ERD; 4 PUMLs synced (PNG/SVG regen deferred — needs plantuml binary) |
| B | GAP-236 FE bundle budget CI | #583 | 🟡 PARTIAL — CI guardrail shipped (250KB threshold + override mechanics); 13 unit tests; baseline KC <236KB / KH <194KB; 44+ page conversions still deferred |
| C | GAP-237 admin AMQP cache invalidation | #584 | 🟢 DONE — TopicExchange + 2 listener queues; 6 new tests; admin 29/29; subscription 355/355 (no regression); feature-flagged off until subscription-side dispatcher lands (informational ADR-021 follow-up) |

**Wave validation:**
- Zero merge conflicts (disjoint files honored ✅)
- Zero rule violations from agents
- ~93% wall-clock reduction (~12 min parallel vs estimated 4-6h serial)
- Wave plan PR-first per `feedback_wave_plan_through_pr.md` — no rule violation this time

Counts: **89 OPEN → 87 OPEN** (-GAP-234 -GAP-237 closed; GAP-236 stays PARTIAL but advanced).

**2026-04-27 (GAP-243 SHIPPED — flips GAP-241 + GAP-242 to DONE):** GAP-243 status 🔵 OPEN → 🟢 DONE same day. Option A (least invasive): extend AdminControllerTest's `@DynamicPropertySource` with S3 mock properties + `@MockBean RabbitTemplate` for Mockito proxy. Verification: AdminControllerTest **7/7 ✅**, admin full suite **23/23 ✅**, subscription **355/355 ✅** (no regression). `kitehub-ci.yml` admin job exclusion removed — full admin suite now runs in CI. Cascade closure: **GAP-241 PARTIAL → DONE** (CI exclusion gone), **GAP-242 PARTIAL → DONE** (downstream test path now green). Counts: **92 OPEN → 89 OPEN** (-GAP-241/242/243 closed). Wave 7 admin module cleanup chain fully resolved (GAP-238 → 240 → 241 → 242 → 243, 5 gaps closed in same session).

**2026-04-27 (GAP-242 PARTIAL — V11 Postgres SQL fixed):** GAP-242 status 🔵 OPEN → 🟡 PARTIAL. Root production bug resolved: V11 had `UNIQUE (..., (sent_at::date))` constraint with expression — Postgres rejects (SQL state 42601, only column names allowed in UNIQUE CONSTRAINT). Split into table CREATE + separate `CREATE UNIQUE INDEX` (which DOES support expressions). V11 had never run successfully against any Postgres → safe in-place edit. Subscription tests use Hibernate `ddl-auto=create-drop` (Flyway disabled) so 355/355 still pass. AdminControllerTest's deeper test-infra gaps (S3 mock, RabbitMQ mock for full @SpringBootTest) refiled as **GAP-243** (P2). Counts: **91 OPEN → 92 OPEN** (+GAP-243 filed; GAP-242 stays PARTIAL). GAP-241 also stays PARTIAL pending GAP-243.

**2026-04-27 (GAP-241 PARTIAL — admin/email/gateway CI jobs added):** GAP-241 status 🔵 OPEN → 🟡 PARTIAL. Added 3 jobs to `kitehub-ci.yml`: `test-admin` (excludes `AdminControllerTest` pending GAP-242 Flyway fix), `test-email` (20/20 pass), `test-gateway` (10/10 pass). `code-quality` job needs all 5 module tests. CI no longer blind to admin/email/gateway regressions. Re-enable `AdminControllerTest` once GAP-242 closes → flip GAP-241 to DONE. Counts: **91 OPEN → 91 OPEN** (-0; GAP-241 stays PARTIAL).

**2026-04-27 (GAP-240 SHIPPED + GAP-242 filed):** GAP-240 status 🔵 OPEN → 🟢 DONE same-day. Fix in same PR as GAP-238 hardening. (1) Admin's `@EnableJpaRepositories` + `@EntityScan` extended to include subscription's `outbox/idempotency/domain` packages. (2) GAP-238 fix hardened — `@ConditionalOnMissingBean` insufficient for user-code @Configuration ordering across modules; replaced with explicit `@Bean(name="adminCacheManager")` + `@Primary` and `@Bean(name="subscriptionCacheManager")`. Both beans coexist (distinct names); admin's @Primary wins for @Cacheable. Verification: `KiteHubAdminApplicationTest.contextLoads` ✅ passes (was failing); subscription full suite 355/355 still pass; admin unit tests 15/15. **Surfaced GAP-242**: 7 `AdminControllerTest` still fail with Flyway V11 SQL incompatibility in test DB (separate test-infra concern, P2). Counts: **91 OPEN → 91 OPEN** (-GAP-240 closed; +GAP-242 filed).

**2026-04-27 (GAP-238 SHIPPED + 2 follow-ups filed):** GAP-238 status 🔵 OPEN → 🟢 DONE same day filed. Fix: `@ConditionalOnMissingBean(CacheManager.class)` on subscription's bean + admin's manager declares transitive cache names + `@Configuration` rename for defensive uniqueness. Verification: admin unit tests 15/15 pass, subscription full suite 355/355 pass, BeanDefinitionOverrideException no longer in admin context startup. **Surfaced 2 deeper pre-existing issues** (not GAP-238 scope, filed as follow-ups): **GAP-240 P1** — admin JPA repository scan misses `SubscriptionOutboxRepository` (8 admin @SpringBootTest still fail context load); **GAP-241 P1** — `kitehub-ci.yml` doesn't test admin/email/gateway modules at all (CI blind spot — that's why GAP-238 + GAP-240 shipped to main invisibly). Counts: **90 OPEN → 91 OPEN** (-GAP-238 closed; +GAP-240 +GAP-241 filed).

**2026-04-27 (Wave 7-Perf SHIPPED — 4 parallel agents, ~16 min wall-clock vs 9-17h serial estimate):** 4 parallel `isolation: worktree` agents closed/advanced 4 perf gaps in disjoint scope. Per `feedback_parallel_agent_strategy.md` rule #5 (sequence merges) + rule #6 (manual worktree cleanup): 4 PRs merged, 4 worktrees force-removed, 4 local + 4 remote branches deleted.

| Agent | Gap | PR | Result |
|:-----:|-----|:--:|--------|
| A | GAP-126 admin dashboard cache | #569 | 🟢 DONE — @Cacheable + Pageable + in-process Spring event invalidation; 15/15 tests |
| B | GAP-127 FE code-splitting | #570 | 🟡 PARTIAL — bundle analyzer + 10 pages/app + optimizePackageImports; baseline <250KB; 1034/1034 tests |
| C | GAP-130 docker resource limits | #568 | 🟢 DONE — 4 compose files, 114 limit declarations; runbook in 05-guides |
| D | GAP-135 SLO instrumentation | #571 | 🟡 PARTIAL — 16/29 controllers @Timed; 5 Prom rules + 8 Grafana panels |

**4 follow-up gaps filed (Agent return findings):**
- **GAP-236** P2 — FE code-splitting completion (44+ pages) + CI bundle budget guardrail
- **GAP-237** P2 — Cross-service Outbox cache invalidation (kitehub-admin AMQP integration)
- **GAP-238** P1 — `cacheConfig` bean collision admin↔subscription (pre-existing, latent CI flake hazard)
- **GAP-239** P2 — API SLO coverage completion (13 + admin controllers) + PR template SLO declaration

**Wave validation:**
- Zero merge conflicts (disjoint files honored ✅)
- Zero rule violations from agents (worktree path discipline maintained ✅)
- Pre-existing CI bug surfaced (GAP-238) — would have remained latent without Wave 7-Perf
- Memory `feedback_wave_plan_through_pr.md` filed earlier same session for parent direct-push violation

Counts: **88 OPEN → 90 OPEN** (-GAP-126 -GAP-130 closed; +GAP-236/237/238/239 filed; GAP-127/135 stay PARTIAL but progressed). Wave 7 Meta+Feature P0 queue narrowed: GAP-005 + GAP-011 still infra/designer-blocked.

**2026-04-26 (GAP-014 planning portion v2-aligned — Wave 7 Meta-P0):** GAP-014 status PLANNED → 🟡 PARTIAL. Wave plan `wave-mock-data-local-dev.md` §7 rewritten end-to-end against shipped v2 controllers in `kiteclass-core` (NOT kitehub-branding per architecture doc drift). Replaced 12 aspirational endpoints with 10 real ones (InstanceController 8 + BrandingPackageController 1 + PublicBrandingController 1 + InternalWebhookController 1). Internal services (Analyzer/Planner/Executor/QualityReviewer/ContentModeration/Saga) called out as non-REST. Added §7.7 Out-of-scope với 6 deferred items (GAP-005/006/011/012/020/070). Implementation portion (MSW handlers + DataSeeder + demo) split to **GAP-235** (P1, wave-eligible 4 sub-PRs). Counts: **87 OPEN → 88 OPEN** (+GAP-235; GAP-014 stays PARTIAL). Wave 7 Meta-P0 queue narrowed: GAP-005 + GAP-011 remain (GAP-014 moved to PARTIAL).

**2026-04-26 (GAP-016 final closure — Wave 7 Meta-P0):** GAP-016 status 🟡 PARTIAL → 🟢 DONE. Final actions: (1) §2.9 business-gap-check audit ran with fixed grep scope (kiteclass-core + kitehub-branding) — 16/20 ✅, 2 ❌ tracked existing gaps (GAP-005 regenerate counter, GAP-011 ImageTemplate library), 1 ⚠️ Saga alternative pattern, 1 ⏭️ DB-dependent. (2) Skill `business-gap-check.md` §2.9 updated: grep scope `kitehub-branding` → `kiteclass-core` + class renames `BrandingAnalyzer→AnalyzerService`/`BrandingPlanner→PlannerService` + module-location note. (3) GAP-016 Findings table flipped — 7 items closed by GAP-229 (PRs #561/#562); 6 stale items split out as **GAP-234** (architecture doc + 4 PUML diagrams + database-design.md + docker-platform-architecture.md drift, P2 deferred). Per memory `feedback_audit_grep_scope.md`: skill grep scope correction is the kind of force-multiplier fix that prevents future false-positives like GAP-107. Counts: **87 OPEN → 87 OPEN** (-GAP-016 +GAP-234 net 0). Wave 7 Meta-P0 queue narrowed: GAP-005 + GAP-011 + GAP-014 remain.

**2026-04-26 (Wave session-followups — 3 parallel agents):** Closed loose ends từ session 5-PR. (1) **Skill bug fix:** `session-docs-check/scripts/check-docs.sh` Rule 8 logic — chỉ flag truly-new folders qua `git ls-tree -r --name-only $BASE_REF -- $dir` check, không flag pre-existing folders nhận file mới (3 audit dirs WARN false-positive). Retest cumulative session: 4 PASS / 0 WARN / 0 FAIL (was 5/3/0). (2) **GAP-229 closed:** Status 🟡 PARTIAL → 🟢 DONE. All 6 AC ticked. Phase 2/3 closure log entry references PRs #561/#562 + cite specific files (3 user guides + 3 instance-provisioning docs + 05-guides README index). Counts: **88 OPEN → 87 OPEN** (-GAP-229). (3) **3 audit gaps filed — GAP-231 (payment-invoice), GAP-232 (attendance), GAP-233 (student-enrollment):** API contract drift cluster from post-wave-7 audit. **Audit calibration finding** (per `feedback_audit_calibration.md`): audit Agent C over-stated severity — claimed "13 domains zero-doc" with "0 documented" cells; verification shows all 3 worst domains (payment-invoice, attendance, student-enrollment) **have existing api-contract.md files** with substantial content. Real drift is depth (auth blocks, error matrices, DTO schemas, UC linkage, side-effect cross-refs) NOT greenfield. GAP-231 also re-counted endpoints: audit said 23, real = 32 across 5 controllers. Gaps re-framed as "drift completion" — keeping P0 priority but scope reduced from "write from scratch" to "fill in gaps". Counts: **87 OPEN → 90 OPEN** (+GAP-231/232/233). **Wave validation:** 3 parallel agents returned in ~3 min wall-clock vs estimated ~30 min serial — pattern from `feedback_wave_plan_before_serial_prs.md` working as designed. Parent owned ROADMAP per `feedback_parallel_agent_strategy.md` rule #2 → zero merge conflicts despite 3 agents.

**2026-04-26 (GAP-229 Phase 1 SHIPPED — AI Branding business docs v2 sync):** 3 docs in `documents/01-business/kitehub/ai-branding/` synced from real `kiteclass-core` Waves 2-4 implementation. `rules.md` +24 v2 rules across 6 areas (BR-RES/LIFE/QUALITY/APRV/WIZARD/MOD/PKG) each with code reference + config key. `use-cases.md` +6 UCs (UC-AIB-07..12) sourced from real Controllers + Services. `api-contract.md` +12 v2 endpoints (8 lifecycle + 2 branding package + 1 internal webhook + 4 TBD approval) with schemas from real `InstanceController` + `BrandingPackageController` + `PublicBrandingController` + `InternalWebhookController`. Per memory `feedback_search_all_modules_before_missing_claim.md`: documented REAL impl not aspiration; gated features (tier counter, ENTERPRISE Advanced Mode) noted as scaffold/TBD where code lacks. Phase 2 (3 user guides) + Phase 3 (instance-provisioning verify) deferred to separate sessions. GAP-229 status 🔵 OPEN → 🟡 PARTIAL. No counts change (still PARTIAL).

**2026-04-26 (GAP-222c SHIPPED — Option B generalize migration_outbox → subscription_outbox):** Final outbox-cluster migration. V22 Flyway: rename `migration_outbox` → `subscription_outbox`, drop FK + drop NOT NULL on `instance_id`. Renamed `MigrationOutboxEvent`/`Repository`/`MigrationEventEmitter` → `Subscription*` (emitter now `@Component`); added `emit(UUID, ...)` overload for nullable instance_id (email pre-provisioning case). `InstancePurgeService` (line 188) + `EmailServiceClient.publishToQueue` (line 588) migrated to §3.5.1 Exception A: outbox.emit first + try/catch best-effort `rabbitTemplate.convertAndSend` with marker comment "outbox is the reliability net". `EmailServiceClient` class-level `@Transactional` to ensure outbox + EmailSentLog save share txn (private dispatchEmail couldn't be self-call proxied). `ObjectMapper` injected (Spring Boot's auto-configured one with JSR-310). `TrialToPaidService` constructor refactored to take emitter bean. 6 new tests (3 InstancePurgeService Exception A + 3 EmailServiceClient Exception A) — **355/355 kitehub-subscription tests green**. GAP-222c status 🔵 OPEN → 🟢 DONE. Counts: **89 OPEN → 88 OPEN** (-GAP-222c).

**2026-04-26 (GAP-222b SHIPPED — ParentInvitationServiceImpl outbox migration):** kiteclass-core internal migration applied as §3.5.1 Exception A (matches BrandingEventPublisher precedent in same module): outbox.enqueue first + existing fast-path try/catch with marker comment. Constructor expanded with OutboxEventWriter + ObjectMapper; test ObjectMapper uses findAndRegisterModules() for JavaTimeModule (matches Spring Boot default — initial omission caused Instant serialization failure in test, fixed). 13/13 ParentInvitationServiceTest + **1117/1117 full kiteclass-core suite green**. GAP-222b status 🔵 OPEN → 🟢 DONE. Counts: **90 OPEN → 89 OPEN** (-GAP-222b).

**2026-04-26 (GAP-230 SHIPPED — Exception D rule + AIQueueDispatcher marker):** Rule extension landed `design-patterns.md` v1.2.0 → v1.3.0: §3.5.1 Exception D (dedicated dispatcher infrastructure) with 4-criterion test (naming + caller-persists-first + no-business-logic + marker phrase) + AIQueueDispatcher example. Marker applied to `AIQueueDispatcher` class-level javadoc. Triage of 5 audit Cat 5 hits: 1 D (AIQueueDispatcher), 2 A (BrandingEventPublisher already documented + BrandingJobService closed by GAP-222a Phase 2), 2 still need Exception A migration (EmailServiceClient + InstancePurgeService) — re-scoped under existing **GAP-222c** which was UNBLOCKED + reduced from L (4 services) → M (2 services). GAP-230 status 🔵 OPEN → 🟢 DONE same day. Counts: **90 OPEN → 90 OPEN** (-GAP-230 net 0; GAP-222c stays open with revised scope).

**2026-04-26 (GAP-222a Phase 2 SHIPPED — kitehub-branding domain outbox):** Per ADR-021 (PROPOSED #556) per-module pattern executed: created `BrandingOutboxEvent` + `BrandingOutboxRepository` + `BrandingEventEmitter` in `kitehub-branding/outbox/`; Flyway `V21__create_branding_outbox.sql` in `kitehub-subscription`; `BrandingJobService.createJob()` migrated to outbox-first + best-effort fast-path (Exception A pattern). New `BrandingEventEmitterTest` (4 cases) + updated `BrandingJobServiceTest`. Full module suite **153/153 green**. `design-patterns.md` v1.1.0 → v1.2.0 (§3.5.1 default-rule paragraph cites both per-module precedents). AIQueueDispatcher case NOT migrated — class is dedicated dispatcher infrastructure, not domain-event source; needs §3.5.1 Exception D → filed **GAP-230** (Meta-P1, rule clarification). GAP-222a status 🟡 PARTIAL → 🟢 DONE. Counts: **90 OPEN → 90 OPEN** (-GAP-222a +GAP-230 = net 0).

**2026-04-26 (Wave 7 queue staleness fix — docs-only):** State-check trước khi pick Wave 7 next-action phát hiện priority queue line 4 stale — `PowerPoint format (Feature-P0)` đã DONE từ Wave 5 (GAP-047 closed Sub-PR 5.6b #532, 2026-04-25; PowerPoint deferred per Q6 scope-lock với Canva/Slides alternative justification). Removed stale entry; added GAP-229 (BL-P1 docs sync) per matrix-strict ordering; updated GAP-006 status BLOCKED → unblocked (Sub-PR 223.1 shipped 2026-04-26 #553/#554 means GAP-006 = Sub-PR 223.2 actionable). Pattern: lặp lại memory `feedback_gap_state_check_required.md` — ROADMAP cần state-check trước khi consume queue. No gap counts change (cleanup only).

**2026-04-26 (Sub-PR 223.1 CORRECTION — module path fix):** GAP-016 verification sweep phát hiện audit-gate.py rule patterns + skill SKILL.md + baseline audit references trong PR #553 đều dùng `kitehub-branding/` paths với class names từ architecture doc (BrandingPlanner/BrandingAnalyzer/BrandingExecutor) — KHÔNG match implementation thực tế. V2 code đã ship Waves 2-4 nhưng landed trong **`kiteclass/kiteclass-core/`** (NOT `kitehub-branding/`) với real names: `AnalyzerService`/`PlannerService`/`PlanExecutor`. Correction PR fixes: (1) audit-gate.py patterns + class names corrected, (2) skill SKILL.md updated, (3) baseline audit references updated (score 62/100 stays — calibration đúng), (4) GAP-225 cluster cells corrected, (5) GAP-016 status PLANNED → 🟡 PARTIAL với Findings table verified-real. Filed GAP-229 (P1 biz-logic) cho business docs v2 sync + 3 missing user guides — Living Documents rule violation từ Waves 2-4. Counts: **89 OPEN → 90 OPEN** (+GAP-229).

**2026-04-26 (Sub-PR 223.1 SHIPPED, Wave 7 governance scaffold landed):** GAP-223 Option C executed — single PR delivered: (1) skill `quality/ai-branding-quality-gate/` (manual checklist 5 sections × 20 = /100), (2) baseline audit `2026-04-26-baseline.md` 62/100 ⚠️ BASELINE, (3) `audit-gate.py` AUDIT_RULES + AUDIT_DIRS extended cho `kitehub-branding/` Java patterns, (4) `ai-branding-guidelines.md` v1.1.0 với §11.4 Migration test checklist + frontmatter backfill, (5) `output-review-mandate.md` v1.0.2 matrix line 75 re-sync, (6) 3 follow-up gaps GAP-226/227/228 cho real WCAG/vrg/ML (Wave 8+ scope). GAP-223 status 🔵 OPEN → 🟡 PARTIAL (Sub-PR 223.2 = GAP-006 Gemma 4 9B migration unblocked, queued separate session). Counts: **86 OPEN → 89 OPEN** (+GAP-226/227/228).

**2026-04-26 (afternoon, cross-gap audit triggered by GAP-223 Wave 7 kickoff):** Explore agent quét 220+ gap files + matrix + audit-gate.py + skill catalog → phát hiện **systemic scaffold-as-DONE governance debt**. 5 gaps (GAP-008/009/012/015/018) shipped Waves 2-4 marked DONE despite explicit deferred items + missing audit-gate rules + missing dedicated skills + matrix mismatches. **Filed GAP-225** (umbrella, 🟠 P1 meta, docs-only this PR) capturing pattern + 3 cluster fix plan (C1 AI agent, C2 Saga, C3 AI branding — last covered by GAP-223). `output-review-mandate.md` line 75 synced from "PLANNED" → "PARTIAL". 5 affected gap files cross-linked to GAP-225 in their Log sections (Status preserved DONE for audit trail). User decision: docs-only truth-up, không Wave 7 commitment. Phase 2-4 implementation deferred until scheduled. Counts: **84 OPEN → 86 OPEN** (+GAP-224 collector regex, +GAP-225 umbrella).

**2026-04-24 update:** ROADMAP coverage refresh — prior state had 141/186 gaps referenced (24% missing). This refresh brings coverage to 100% by adding Epic 15 (Vietnam K-12 Education, 14 gaps), appending 9 observability/ops gaps to Epic 6, 5 frontend P2 gaps to Epic 13, and 8 meta/CI gaps to Epic 14. Accurate counts now: **81/186 gaps DONE (44%)**, 84 OPEN, 14 PARTIAL/PLANNED, 7 IN_PROGRESS. Also: CI history policy tightened via PR #471 (soft cap 500→50, hard cap 1000→100, feature-branch failure age 7d→1d) and executed cleanup went 538→52 runs. Session skill fixes GAP-206 (wave+blockers accuracy, PR #468) + GAP-207 (Vietnamese output per CLAUDE.md, PR #470) CLOSED. GAP-205 CI retention automation CLOSED.

**2026-04-24 (later, Wave 5 kickoff):** PR #474 Sub-PR 5.0 opened; Core Service CI surfaced pre-existing flaky test `DefaultUrlAllowlistValidatorTest.allowsTenantListedHost` — `api.partner.com` resolving to `::1` on WSL2 + CI runners triggers validator's DNS-rebind guard. Confirmed on `main` with no Sub-PR 5.0 changes. **Filed GAP-212 (P1)** — test-only fix using RFC-2606 `.invalid` domain; blocks PR #474 merge and every future Core CI run. Counts: **82 OPEN → 83 OPEN** (+GAP-212).

**2026-04-24 (Wave 5 generator trio SHIPPED):** Sub-PRs 5.0 (#474 foundation + ADR-019), 5.1 (#476 PDF + invoice), 5.2 (#477 Excel + attendance), 5.3 (#478 Word + teacher contract) all merged to main same day. **GAP-047 status 🔵 OPEN → 🟡 PARTIAL.** PowerPoint deferred to Wave 6 per scope-lock (PR #473 Q6). Remaining before GAP-047 closes 🟢 DONE: Sub-PR 5.5 branding integration + HTTP endpoints, Sub-PR 5.6 wave completion. Counts: **84 OPEN → 84 OPEN, 14 → 15 PARTIAL** (GAP-047 reclassified). Recommend continuing Wave 5 (Sub-PR 5.5 next) before pivoting to GAP-046 or Wave 10.

**2026-04-24 (afternoon, Dependabot full-expansion):** PR #515 landed 1-PR-per-service Dependabot config (after PR #486 full-groups expansion produced 28 PRs, all closed). Fresh run created 4 all-deps group PRs; 2 failed with Spring Cloud BOM resolution error on Boot bumps (kiteclass-gateway #517, kitehub #518 which touches kitehub-gateway pom). **Filed GAP-213 (P1)** — pom BOM fix needed before Dependabot can ship Spring-touching PRs for these 2 services. Boot 3.5.13 → 3.5.14 for 7 kitehub poms + 1 gateway pom blocked until GAP-213 closed. Counts: **83 OPEN → 84 OPEN** (+GAP-213).

**2026-04-23 update:** Continuation of 2026-04-21 security session. Enabled Dependabot via `gh api PUT .../vulnerability-alerts` after GAP-202 skill exposed it was disabled. **Surfaced 89 npm alerts** (8 CRITICAL + 32 HIGH + 45 medium + 4 low). Initial triage incorrectly flagged 8 CRITICAL as false-positive (shallow jq query on only first vulnerable range); corrected analysis shows **all 8 CRITICAL are real** on `next@15.1.6` (GHSA-9qr9 fix 15.1.9, GHSA-f82v fix 15.2.3). Bump attempts (15.1.11, 15.3.9, 15.5.15) all broke `/pricing` + `/blog/[slug]` prerender via `Array.toJSON` regression in next 15.1.7+. Filed **GAP-204** P0 with Stage A (docs) + Stage B (RSC compat investigation) + Stage C (bump + close CRITICAL) + Stage D (triage remaining HIGH) + Stage E (re-enable auto-security-fixes). `/repo-status` reports **BLACK** — skill working correctly.

**2026-04-21 update:** During post-Wave-9.5 `/repo-status` session, user flagged skill missing GitHub Security checks. `gh api` probe surfaced **3 HIGH CVEs** + 4 medium on main (Dependabot silently disabled). Filed **GAP-202** (meta — skill blindspot, Meta-P1) + **GAP-203** (security — CVE fixes, BL-P0). Both re-open previously-closed Epic 5 (Security) + Epic 12 (Process). Priority: GAP-202 first per meta-gap rule, GAP-203 second (skill fix enables continuous detection; CVE fix closes current exposure). PRs #423/#424/#453/#454 shipped 2026-04-21. CVEs auto-closed by Trivy post-merge. Case study: `documents/04-quality/analyses/2026-04-21-dependabot-first-run-incident.md`.

---

## 🎯 Previous Status Snapshot (2026-04-20)

**Progress:** 81/186 gaps CLOSED (44%) — recount 2026-04-24 after coverage sync; prior "73/178" was stale. Waves 1-4 + **Wave 8b SHIPPED** 2026-04-20 (6 parallel agents, PRs #401-#406) + **Wave 9 SHIPPED** 2026-04-21 (6 parallel agents, PRs #408-#413) + **Wave 9.5 SHIPPED** 2026-04-21 (4 parallel agents, PRs #415-#418: GAP-192 Phase 4b-i backend completeness with 45 new tests, GAP-132 fan-out → DONE, GAP-134 expand → DONE; GAP-043 fan-out attempted but 4/5 reverted due to Redis+Jackson typing regression — only BrandingPackage proxy retains sync=true). **Audit catch-up Part A — 5/5 COMPLETE** 2026-04-19. **Part B top-5 priorities — 5/5 SHIPPED** 2026-04-20 (PRs #371–#375) closing 9 gaps. **Re-audit validated 2026-04-20:** business-logic 65→**72** (+7), performance 58→**64** (+6). **Master plan merged PR #382** covers 92 open gaps across 12 waves (~2-3 months). **6 meta gaps tracked** (GAP-170–175) from output-review-mandate §4 VIOLATIONS → Wave 8b. **Part C Sprint 0 CLOSED** 2026-04-20 — GAP-149 (audit grep scope fix) closed, 5 audit skills hardened against multi-module false positives. **Business-logic tier added to priority matrix** 2026-04-20 (`meta-gap-priority.md` §3) — 3 new gaps GAP-150/151/152 track BRD completion + persona AC + persona review execution. **12 new gaps filed 2026-04-20 (GAP-190..201)** from action-1 + simulation; **GAP-196 dropped same-day** (user decision — 9router ADR not effective); **GAP-190 + GAP-197 scope-revised** to 🟡 PARTIAL after state-check found existing infrastructure (sitemap/robots/OG/JsonLd/blog MDX + enhanced-attendance-calendar PR 3.8.1). Net: 11 active new gaps — 1 BL-P0 (GAP-192), 3 BL-P1 (GAP-190/191/200), 4 Meta-P1 (GAP-193/194/199/201), 2 Meta-P2 (GAP-195/198), 1 Feature-P2 (GAP-197). Quality audit baseline 77/100 pending next refresh (due 2026-04-26).

**Priority order (updated 2026-04-20):** Meta-P0 → **Business-Logic-P0** → Feature-P0 → Meta-P1 → Business-Logic-P1 → Feature-P1 → ... Reference `.claude/rules/meta-gap-priority.md` §3 for tier definitions + tie-breakers.

> **Recently closed (do NOT count as blockers):** GAP-046 Wave 6 2026-04-26 (audit 82/100 + ADR-020); GAP-047 Wave 5 2026-04-25 (#532 doc-gen trio).

**GA Blockers remaining: 5 — ordered per `meta-gap-priority.md` (meta before feature within P0).**

| # | Gap | Title | Type | Status | Effort |
|:-:|-----|-------|:----:|:------:|:------:|
| 1 | **GAP-223** | AI Branding migration verification governance — Sub-PR 223.1 SHIPPED 2026-04-26 (skill + audit-gate rule + §11.4 + baseline 62/100); Sub-PR 223.2 = GAP-006 unblock remaining | 🔴 P0 Meta (governance) | 🟡 PARTIAL | Sub-PR 223.2 = GAP-006 (Feature-P1) |
| 2 | ~~GAP-222a~~ | ~~Extract Outbox infra to shared lib~~ — superseded by ADR-021 per-module pattern; closed via GAP-222a Phase 2 + GAP-222b + GAP-222c (all DONE 2026-04-26) | 🟠 Meta (infra) | ✅ DONE | — |
| 3 | **GAP-016** | Living docs impact scope (3-layer sweep) | 🔴 Meta (docs contract) | 🟡 PLANNED | S |
| 4 | GAP-011 | Template library curation (30 templates) | Feature | 🟡 PLANNED | L |
| 5 | GAP-014 | Wave mock plan include AI branding | Feature | 🟡 PLANNED | M |
| 6 | GAP-005 | AI queue fair scheduling (Phase 2) | Feature | 🟡 IN_PROGRESS | M |

> **Priority rule:** Meta-gaps (skills/rules/workflow) go first at each P-level — 1 broken skill/rule affects every future PR, so force multiplier first. Ref `.claude/rules/meta-gap-priority.md`.

**Epics fully closed:** Epic 5 (Security/Compliance), Epic 11 (SaaS Lifecycle Hardening), Epic 12 (Process/DevOps Maturity), Epic 13 (Frontend Quality — 4/5).

**Next recommended wave:** Wave 6 **CLOSED 2026-04-26**. Wave 7 priority queue (per `meta-gap-priority.md` Meta > Feature):

1. **GAP-223** (Meta-P0 PARTIAL) — AI Branding migration verification governance. Sub-PR 223.1 SHIPPED 2026-04-26 (skill + audit-gate rule + §11.4 + baseline 62/100 — PRs #553/#554). Sub-PR 223.2 = GAP-006 unblock.
2. **GAP-222a + GAP-222b + GAP-222c + GAP-230 SHIPPED 2026-04-26** ✅ → Outbox migration cluster fully closed (kitehub-branding domain outbox + parent-invitation outbox + subscription_outbox generalized + Exception D dispatcher policy)
3. **GAP-229** (BL-P1) — AI Branding v2 business docs sync (rules/use-cases/api-contract) + 3 user guides; tracks GAP-016 PARTIAL remainder
4. **GAP-006** (Feature-P1, unblocked 2026-04-26 by Sub-PR 223.1) — Gemma 4 9B migration with VN A/B test
5. **GAP-055** (BL-P0, Wave 10 candidate) — report-card VN format

---

## 1. Epic Taxonomy

186 gaps được group thành **15 epics** (updated 2026-04-24):

| Epic | Theme | Gaps | Priority |
|------|-------|------|:--------:|
| [E1](#epic-1-foundation-infrastructure) | Foundation Infrastructure | 5 | 🔴 MUST FIRST |
| [E2](#epic-2-core-ai-branding-pipeline) | Core AI Branding Pipeline | 6 | 🔴 CORE |
| [E3](#epic-3-ai-infrastructure) | AI Infrastructure (model + queue) | 5 | 🟠 SCALE |
| [E4](#epic-4-integration--delivery) | Integration & Delivery | 5 | 🟠 DEPLOY |
| [E5](#epic-5-security--compliance) | Security & Compliance | 6 | 🔴 NON-NEG |
| [E6](#epic-6-operations--scale) | Operations & Scale | 17 | 🟠 PRODUCTION |
| [E7](#epic-7-ux--conversion) | UX & Conversion | 9 | 🟠 GROWTH |
| [E8](#epic-8-admin--support) | Admin & Support | 7 | 🟡 INTERNAL |
| [E9](#epic-9-developer-experience) | Developer Experience | 3 | 🟡 FUTURE |
| [E10](#epic-10-cross-cutting--architecture) | Cross-cutting & Architecture | 5 | 🟡 CLEANUP |
| [E11](#epic-11-saas-lifecycle-hardening) | SaaS Lifecycle Hardening | 7 | 🔴 BLOCK GA |
| [E12](#epic-12-process--devops-maturity) | Process & DevOps Maturity | 11 | 🟠 PRODUCTION |
| [E13](#epic-13-frontend-quality) | Frontend Quality | 10 | 🟠 GROWTH |
| [E14](#epic-14-quality-governance) | Quality Governance | 35 | 🟡 INTERNAL |
| [E15](#epic-15-vietnam-k-12-education-features) | Vietnam K-12 Education Features | 14 | 🟠 DOMAIN |

---

## 2. Epics Detailed

### Epic 1: Foundation Infrastructure
**Goal:** Setup prerequisites cho AI Branding implementation.
**Why first:** Các epic khác depend vào này.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-011 | Template library curation plan + review standards | 🔴 P0 | L |
| GAP-014 🟡 | Wave mock plan include AI branding — planning v2-aligned 2026-04-26; impl split to GAP-235 | 🟡 PARTIAL | M |
| GAP-015 ✅ | Tenant provisioning auto-trigger (event-driven) — DONE Wave 3 | 🟢 DONE | M |
| GAP-016 ✅ | Living docs impact scope — DONE Wave 7 (2026-04-26, §2.9 audit 16/20 + skill scope fix; GAP-234 split out for diagram drift) | 🟢 DONE | S |
| GAP-046 ✅ | Design patterns applied systematically — DONE Wave 6 (2026-04-26, audit 82/100 Grade B + ADR-020) | 🟢 DONE | M |

**Dependencies:** None — starts immediately.

**Blocks:** Epic 2, Epic 4.

---

### Epic 2: Core AI Branding Pipeline
**Goal:** Build the actual AI branding feature (MVP).

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-007 ✅ | Resource classification pipeline — DONE Wave 2+3 | 🟢 DONE | L |
| GAP-008 ✅ | AI Agent workflow (analyzer/planner/executor) — DONE Wave 3 | 🟢 DONE | XL |
| GAP-009 ✅ | Instance provisioning lifecycle (6 states) — DONE Wave 2 | 🟢 DONE | L |
| GAP-013 ✅ | Guided branding wizard UX — DONE Wave 3 | 🟢 DONE | L |
| GAP-031 ✅ | Expand wizard inputs beyond logo — DONE Wave 3 | 🟢 DONE | M |
| GAP-004 | Template-based image composition (Canva-like) | 🟡 P2 | L |

**Dependencies:** Epic 1 (GAP-011 templates must exist).
**Blocks:** Epic 3, Epic 4.

---

### Epic 3: AI Infrastructure
**Goal:** Scale, reliability, model management.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-005 🟡 | AI queue fair scheduling — Phase 1 DONE 2026-04-18, Phase 2 open | 🟡 IN_PROGRESS | L |
| GAP-002 ✅ | Async pipeline for heavy AI tasks — DONE Wave 3 (2026-04-18) | 🟢 DONE | M |
| GAP-006 | Upgrade AI models — primary **Gemma 4 9B** (revised 2026-04-26 after candidate research vs Qwen 3.6/MixSura) + VN A/B test | 🟠 P1 | S-M (added pre-migration A/B step) |
| GAP-003 | Multi-tier image generation | 🟡 P2 | M |
| GAP-028 | AI model versioning & migration | 🟡 P2 | M |

**Dependencies:** Epic 2 (core pipeline).
**Blocks:** Epic 6 (ops).

---

### Epic 4: Integration & Delivery
**Goal:** Branding reaches users via multiple channels.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-010 ✅ | Branding package API + KiteClass integration — DONE Wave 3 | 🟢 DONE | M |
| GAP-021 ✅ | Branding propagation to email + services — DONE Wave 4 | 🟢 DONE | M |
| GAP-037 ✅ | Branded auth flows (verify, reset pwd) — DONE Wave 4 | 🟢 DONE | S |
| GAP-032 ✅ | Branded error pages (404/500) — DONE Wave 4 | 🟢 DONE | S |
| GAP-039 | Webhook reliability (retry, idempotency) | 🟠 P1 | M |

**Dependencies:** Epic 2 (branding data), Epic 1 (infrastructure).

---

### Epic 5: Security & Compliance
**Goal:** Non-negotiable legal/security requirements.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-018 ✅ | Content safety & compliance — DONE Wave 4 (MVP) | 🟢 DONE | L |
| GAP-041 ✅ | Security hardening (SVG XSS, SSRF, CSRF) — DONE Wave 4 | 🟢 DONE | M |
| GAP-042 ✅ | Legal/IP protection (DMCA workflow) — DONE Wave 4 | 🟢 DONE | M |
| GAP-012 ✅ | Automated instance quality review — DONE Wave 4 | 🟢 DONE | M |
| **GAP-203** | Fix 7 open CVEs in transitive Maven deps (3 HIGH) + enable Dependabot | 🔴 P0 | M |
| **GAP-204** | 89 npm alerts — 8 CRITICAL (next.js) + 32 HIGH + 45 medium + 4 low (5 stages A-E) | 🟡 P2 | XL |

**Dependencies:** Can parallelize với Epic 2. GAP-203 pairs with GAP-202 (detection skill fix). GAP-204 depends on GAP-202 (detection exposed scope) + compatibility work on JsonLd RSC serialization.
**Status:** 🟡 PARTIAL 2026-04-24 — All 8 CRITICAL + 32 HIGH + 39/45 medium CLOSED (92% resolved) via PRs #457/#458/#459/#460. Only 6 medium remain (axios 4 + follow-redirects 2 transitive) handled by Stage E auto-flow. Epic 5 **back to GREEN** (no CRITICAL/HIGH live on main). GAP-203 shipped 2026-04-21 (PR #424), GAP-202 shipped 2026-04-21 (PR #423/#453). Security session 2026-04-21 → 2026-04-24: total 8 PRs, 82/89 alerts closed.

---

### Epic 6: Operations & Scale
**Goal:** Production readiness.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-019 | AI observability & cost monitoring | 🟠 P1 | M |
| GAP-043 | Performance protection (cache stampede) | 🟠 P1 | M |
| GAP-030 | Disaster recovery for AI branding | 🟡 P2 | M |
| GAP-044 | Synthetic monitoring + feature flags | 🟡 P2 | M |
| GAP-024 | Asset lifecycle & storage cleanup | 🟡 P2 | S |

**Dependencies:** Epic 3 (need real traffic to monitor).

---

### Epic 7: UX & Conversion
**Goal:** User experience + revenue optimization.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-020 | Wizard state persistence | 🟠 P1 | S |
| GAP-017 | AI usage → billing integration | 🟠 P1 | M |
| GAP-026 | Trial/freemium AI mechanics | 🟠 P1 | M |
| GAP-036 | Tier upgrade UX (reveal, teaser) | 🟠 P1 | M |
| GAP-033 | Branding version history & rollback (user) | 🟡 IN_PROGRESS (Wave 4 partial — manual rollback done; auto + A/B deferred) | M |
| GAP-034 | Branding export pack (ZIP + PDF) | 🟡 P2 | M |
| GAP-025 | Mobile-first wizard UX | 🟡 P2 | M |

**Dependencies:** Epic 2, Epic 4.

---

### Epic 8: Admin & Support
**Goal:** Internal tools for operations team.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-023 | Admin moderation tools | 🟠 P1 | L |
| GAP-040 | Support impersonation & diagnostics | 🟠 P1 | M |
| GAP-022 | Template analytics & A/B | 🟡 P2 | M |
| GAP-029 | Quality gate calibration | 🟡 P2 | S |

**Dependencies:** Epic 5 (audit logs), Epic 6 (monitoring infra).

---

### Epic 9: Developer Experience
**Goal:** Open ecosystem for integrations.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-038 | Developer API docs + SDK libraries | 🟠 P1 | L |
| GAP-045 | Template marketplace (community) | 🟡 P2 | XL |

**Dependencies:** Epic 4 (stable APIs).
**Note:** Can defer until post-GA.

---

### Epic 10: Cross-cutting & Architecture
**Goal:** Platform-wide concerns, cleanup.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-047 🟢 | Document generation — Wave 5 DONE 2026-04-25 (#474/#476/#477/#478/#529/#530 + 5.6b). PPT deferred Wave 6. | 🔴 P0 | DONE |
| GAP-001 | kiteclass-gateway decision | 🟡 P2 | S |
| GAP-027 | Multi-brand per tenant (franchise) | 🟡 P2 | XL |
| GAP-035 | Wizard team collaboration | 🟡 P2 | L |
| GAP-221 | GitNexus pilot — code-intelligence MCP for multi-module audits | 🟡 P2 Meta | M (1-day pilot) |
| GAP-222 | Outbox bypass policy + migrate 5 direct-publish services | 🟡 PARTIAL | Policy + detector ✅ Sub-PR 6.4; migration → 222a/b/c |
| GAP-222a | Extract Outbox infra to shared lib (kitehub-* unblocker) | 🟠 P1 | S-M (~2-3h) — blocks 222c |
| GAP-222b | Migrate ParentInvitationServiceImpl to OutboxEventWriter (kiteclass-core internal, NOT blocked) | 🟠 P1 | S-M (~1-2h) |
| GAP-222c | Migrate 4 kitehub direct-publish sites (BrandingJobService + AIQueueDispatcher + InstancePurgeService + EmailServiceClient) | 🟠 P1 | L (~4-6h) — BLOCKED on 222a |

**Dependencies:** Mixed — document gen crosses all, multi-brand ties to all. GAP-221 is opt-in pilot (mirror RTK PR #531 pattern) — if ADOPT, becomes audit-skill force-multiplier; if REJECT, contained rollback.

---

## 3. Dependency Graph

```
                ┌──────────────────┐
                │ Epic 1 Foundation │ ←── MUST START FIRST
                └─────────┬────────┘
                          │
              ┌───────────┴───────────┐
              ▼                       ▼
    ┌─────────────────┐    ┌──────────────────┐
    │  Epic 2 Core    │    │ Epic 5 Security  │ ←── PARALLEL
    │  Pipeline       │    │ & Compliance     │
    └────────┬────────┘    └─────────┬────────┘
             │                       │
   ┌─────────┼─────────┐             │
   ▼         ▼         ▼             │
 ┌────┐   ┌────┐    ┌────┐          │
 │ E3 │   │ E4 │    │ E7 │          │
 │ AI │   │Int.│    │ UX │          │
 │Inf.│   │    │    │    │          │
 └─┬──┘   └──┬─┘    └──┬─┘          │
   │         │          │            │
   └────┬────┴──────────┴────────────┘
        ▼
   ┌──────────────┐
   │ Epic 6 Ops   │ ←── Needs Epic 3, 4
   │ & Scale      │
   └──────┬───────┘
          │
          ▼
   ┌──────────────┐     ┌──────────────┐
   │ Epic 8 Admin │     │ Epic 9 DX    │
   │ & Support    │     │ (defer)      │
   └──────────────┘     └──────────────┘

   ┌──────────────┐
   │ Epic 10 X-cut│ ←── Can parallelize with most
   └──────────────┘
```

---

## 4. Sprint Roadmap

### 🚀 Sprint 0: Foundation (2 weeks) — MUST DO FIRST

**Goal:** Unblock all future work.
**Gaps:** GAP-011, 014, 016, 046
**Deliverables:**
- 30 initial templates curated
- Wave mock plan finalized
- Business docs updated
- Design pattern rules enforced

### 🚀 Sprint 1: MVP Pipeline (3 weeks)

**Goal:** End-to-end branding generation works.
**Gaps:** GAP-007, 008 (partial), 013, 031, 015
**Deliverables:**
- Resource router working
- Wizard with rich inputs
- Tenant created → auto-provision triggered
- First template-first branding generated

### 🚀 Sprint 2: Core Delivery (2 weeks)

**Goal:** Branding reaches users.
**Gaps:** GAP-009, 010, 032, 037
**Deliverables:**
- Lifecycle state machine
- Package API with ETag caching
- Branded error pages, auth flows
- Integration tests pass

### 🚀 Sprint 3: Security + Quality Gate (2 weeks) — PARALLEL with S1/S2

**Goal:** Non-negotiable compliance.
**Gaps:** GAP-018, 041, 012
**Deliverables:**
- Content moderation integrated
- Security hardening (SVG sanitize, SSRF protection, CSRF)
- Automated quality review in pipeline

### 🚀 Sprint 4: AI Scale (3 weeks)

**Goal:** Handle 100+ concurrent users.
**Gaps:** GAP-005, 002, 006 (Gemma 4 upgrade), 008 (finish)
**Deliverables:**
- RabbitMQ fair queue per tier
- Async image generation
- Gemma 4 in production

### 🚀 Sprint 5: UX Polish (2 weeks)

**Goal:** Conversion optimization.
**Gaps:** GAP-020, 021, 017, 026, 036
**Deliverables:**
- Wizard autosave/resume
- Email branding propagation
- Billing integration
- Trial mechanics + upgrade UX

### 🚀 Sprint 6: Ops Readiness (2 weeks)

**Goal:** Production launch ready.
**Gaps:** GAP-019, 043, 023, 042
**Deliverables:**
- Grafana dashboards
- Cache stampede protection
- Admin moderation UI
- Legal/IP framework

### 🚀 Sprint 7: Extended Features (flexible)

**Goal:** Enhancements based on feedback.
**Gaps:** Remaining P2 items (GAP-024, 025, 030, etc.)

### 🚀 Sprint 8+: Future / Nice-to-have

**Gaps:** GAP-027 (multi-brand), GAP-035 (collab), GAP-045 (marketplace), GAP-038 (SDK)

**Document Generation (GAP-047) — cross-cutting:**
Inject into Sprint 4-5 (invoice for billing, certificate for completion).

---

## 5. Critical Path

```
GAP-011 (templates) →
  GAP-007 (classification) →
    GAP-008 (agent) →
      GAP-009 (lifecycle) →
        GAP-010 (package API) →
          GAP-012 (quality gate) →
            [GA LAUNCH]
```

**Bottleneck:** GAP-011 (external dependency — designer) và GAP-008 (XL effort).

---

## 6. Effort Summary

| Size | Days | Gaps |
|------|------|------|
| S (Small, 1-3 days) | 3 | 5 gaps |
| M (Medium, 4-7 days) | 6 | 24 gaps |
| L (Large, 8-14 days) | 12 | 13 gaps |
| XL (Extra Large, 15+ days) | 20 | 5 gaps |

**Total estimated effort:** ~300 person-days (~6 months with 1 dev, ~2 months với 3 devs parallel).

---

## 7. Consolidation Opportunities

Some gaps có overlap, có thể merge:

| Candidates | Rationale |
|-----------|-----------|
| GAP-012 + GAP-029 | Both about quality review. Keep separate but implement together. |
| GAP-019 + GAP-044 | Both observability. Parts of same dashboard project. |
| GAP-032 + GAP-037 | Both branded pages (404/auth). Implement in 1 sprint together. |
| GAP-003 + GAP-028 | Both model versioning concerns. Unify when tackling. |
| GAP-018 + GAP-042 | Content safety + legal IP. Shared admin UI (GAP-023). |

**Don't merge** — track separately for clarity but implement in combined sprints.

---

## 8. Priority Tier Simplification

> **Superseded by refreshed tier table lower in file ("Updated Priority Tiers (103 gaps, refreshed 2026-04-18)").**
> Original Sprint 0-6 planning preserved here for historical context.

Original mapping (Wave 1 planning, pre-execution):

| Tier | Count (original plan) |
|------|-----------------------|
| 🟥 Block GA | 17 gaps |
| 🟨 Block GROWTH | 18 gaps |
| 🟦 Block SCALE | 12 gaps |

See refreshed counts + remaining-open list in §"Updated Priority Tiers" below.

---

## 9. Recommended Execution Model

**Team size scenarios:**

### Solo (1 dev, 6 months to GA)
- Strict sequential: Sprint 0 → 1 → 2 → 3 → 4 → 5 → 6
- Can't parallelize Epic 5 security
- Launch with 17 GA-blocker gaps closed

### Small team (3 devs, 2-3 months to GA)
- Parallel streams:
  - **Stream A (backend):** E1 → E2 → E3 → E6
  - **Stream B (frontend):** E1 → E2 wizard → E4 integration → E7 UX
  - **Stream C (security/ops):** E5 → E6 operations
- Launch with 25 gaps closed (GA + early growth)

### Full team (5+ devs, 1-2 months)
- All streams parallel
- Dedicated security team for Epic 5
- Launch with 30+ gaps closed

---

## 10. What To Do Right Now (Action Items)

1. **Approve roadmap** — user review this doc
2. **Assign Sprint 0 tasks** — GAP-011 (hire designer), GAP-014/016 (docs), GAP-046 (architecture)
3. **Set launch target date** — based on team size scenario
4. **Create tracking** — Linear/Jira/GitHub project với epics as milestones
5. **Cadence** — weekly sprint review, biweekly retro
6. **Dependency watchers** — alert when blocker resolved

---

## 11. Related Files

- `README.md` — flat index of all 47 gaps
- `_TEMPLATE.md` — template for new gaps
- Per-gap details: `GAP-XXX-*.md`
- AI Branding master design: `documents/02-architecture/ai-branding-v2-redesign.md`
- Design patterns: `documents/02-architecture/ai-branding-design-patterns.md`
- MiniMax skills analysis: `documents/04-quality/skills-gap-analysis-vs-minimax.md`

---

## 12. Progress Log

### Wave 2 — Data Model Foundation — 🟢 COMPLETE (2026-04-14)

7 sub-PRs merged sequentially:

| Sub-PR | PR | Gap | Status |
|--------|----|-----|--------|
| 2.1 ADRs (5 architectural decisions) | #271 | — | 🟢 |
| 2.2 Academic Year + Semester + Holiday | #273 | GAP-053 | 🟢 |
| 2.3 K-12 Multi-Subject Model | #275 | GAP-054 | 🟢 |
| 2.4 Role Hierarchy + Permissions | #276 | GAP-058 | 🟢 |
| 2.5 Instance Provisioning Lifecycle | #277 | GAP-009 | 🟢 |
| 2.6 Resource Classification Pipeline | #278 | GAP-007 | 🟢 |
| 2.7 Integration + Wave Completion | (this PR) | — | 🟢 |

**Wave 2 Gaps closed:** GAP-053, GAP-054, GAP-058, GAP-009, GAP-007

Deferred items from Wave 2 all landed in Wave 3: REST controllers (3.4), outbox foundation (3.1), concrete resource handlers (3.3), MinIO layout (3.3), internal webhooks (3.4).

### Wave 3 — AI Branding Core Pipeline — 🟢 COMPLETE (2026-04-14)

8 sub-PRs merged sequentially:

| Sub-PR | PR | Gaps addressed |
|--------|----|----|
| 3.1 ADRs (006-009) + Transactional Outbox foundation | #284 | — |
| 3.2 AI Provider adapter + Resilience4j | #285 | — |
| 3.3 Resource Handlers + MinIO storage layout | #286 | GAP-007 (completed) |
| 3.4 REST + Package API + webhook | #287 | GAP-010 ✅ |
| 3.5 AI Agent workflow + GAP-070 rebrand approval | #288 | GAP-008 ✅ GAP-070 ✅ |
| 3.6 Tenant Provisioning Saga | #289 | GAP-015 ✅ |
| 3.7 Guided Wizard UX | #290 | GAP-013 ✅ GAP-031 ✅ GAP-069 ✅ |
| 3.8 Integration + Wave Completion | (this PR) | 🟢 all closed |

**Wave 3 Gaps closed:** GAP-007 (full), GAP-008, GAP-010, GAP-013, GAP-015, GAP-031, GAP-069, GAP-070

Patterns landed: Outbox, Adapter, Strategy, Decorator, Command, Composite, Saga, State Pattern (×2), Builder, Proxy, Optimistic Lock, XState-style FSM (FE reducer).

Deferred to follow-up PRs / later waves (see `03-planning/wave-03-ai-branding-core.md` §Deferred): RabbitMQ consumer wiring, async generate Steps, real Ollama HTTP, REST for rebrand-approvals, Playwright E2E, SSE live progress.

### Wave 4 — Security & Compliance — 🟢 COMPLETE (2026-04-14, parallel-agent)

**First wave at this repo using parallel-agent execution** (worktree-isolated). 6 sub-PRs:

| Sub-PR | PR | Mode | Gaps addressed |
|--------|----|------|----------------|
| 4.0 Foundation + ADRs 010-013 | #294 | serialized (lead) | — |
| 4.1 Content Moderation | #297 | parallel agent #1 | GAP-018 ✅ |
| 4.2 Security Hardening (SVG/SSRF/CSRF) | #296 | parallel agent #2 | GAP-041 ✅ |
| 4.3 Legal/IP (DMCA + trademark) | #295 | parallel agent #3 | GAP-042 ✅ |
| 4.4 GDPR Deletion + retention | #298 | parallel agent #4 | GAP-073 ✅ |
| 4.5 Quality Gate | #299 | serialized (depends on 4.1) | GAP-012 ✅ |
| 4.6 Integration + Wave Completion | (this PR) | serialized | 🟢 all closed |

**Wave 4 Gaps closed:** GAP-012, GAP-018, GAP-041, GAP-042, GAP-073

Wall-clock vs serial: 4 middle sub-PRs took ~20min agent work + ~90min human sequencing vs estimated ~5 days serial. 3 application.yml conflicts during sequencing (resolved each time). 1 CI failure (CSRF test-profile secret) — trivially fixed.

Patterns landed: AuditLog, State Pattern (×3 new — Moderation, DMCA, Deletion), Strategy (Quality checks ×5), Adapter (CSRF), Saga (DMCA workflow), Decorator/Sanitizer (SVG XSS), Validator (URL allowlist).

Deferred (see `03-planning/wave-04-security-compliance.md` §Deferred): real ML NSFW classifier, USPTO API, MinIO streaming export, scheduled expiry job, real contrast/screenshot/URL-ping checks, KiteHub admin UI hookups (slated for Wave 8).

**Next Wave:** Wave 5 K-12 Critical Features (unblocked from Wave 2) OR Wave 6 Ops Readiness OR quality-audit refresh.

---

## NEW EPICS (added 2026-04-16)

### Epic 11: SaaS Lifecycle Hardening
**Goal:** Business logic cho subscription/trial/retention THẬT SỰ hoạt động đúng.
**Why:** Deep audit phát hiện rules có nhưng code thiếu enforcement.

| Gap | Title | Priority | Effort | Dependency |
|-----|-------|:--------:|:------:|:----------:|
| GAP-092 | Re-trial prevention (TR-07 not in code) | 🔴 P0 | S | — |
| GAP-093 | Database backup only logs (not functional) | 🟢 DONE | L | — |
| GAP-091 | Email idempotency guard (2/13 types) | 🟢 DONE | S | — |
| GAP-094 | Hard delete not implemented | 🟢 DONE | M | GAP-093 |
| GAP-095 | Email failure retry mechanism | 🟢 DONE | M | GAP-097 |
| GAP-096 | Email admin controls + monitoring dashboard | 🟢 DONE | L | GAP-097 |
| GAP-097 | Email queue via RabbitMQ (replace direct HTTP) | 🟢 DONE | M | — |

**Dependencies:**
- GAP-093 → GAP-094 (backup trước, hard delete sau)
- GAP-097 → GAP-095, GAP-096 (queue infrastructure trước, retry + admin sau)
**Critical:** MUST complete before GA. Without GAP-093, data loss. Without GAP-097, emails unreliable.

---

### Epic 12: Process & DevOps Maturity
**Goal:** Process gaps cho production readiness — scripts, migrations, CI, deploy, incidents.

| Gap | Title | Priority | Effort | When |
|-----|-------|:--------:|:------:|:----:|
| GAP-081 ✅ | Script review checklist — DONE | 🟢 DONE | S | — |
| GAP-082 ✅ | Migration review checklist — DONE | 🟢 DONE | S | — |
| GAP-086 ✅ | Incident response runbook — DONE | 🟢 DONE | M | — |
| GAP-087 ✅ | Deploy go/no-go checklist — DONE | 🟢 DONE | M | — |
| GAP-088 ✅ | Rollback procedure per service — DONE | 🟢 DONE | L | — |
| GAP-083 ✅ | Gap triage process — DONE | 🟢 DONE | S | — |
| GAP-084 ✅ | CI failure triage — DONE | 🟢 DONE | M | — |
| GAP-085 ✅ | Cross-app consistency check — DONE | 🟢 DONE | M | — |
| GAP-089 ✅ | Post-deploy smoke test — DONE | 🟢 DONE | M | — |
| GAP-090 ✅ | API contract tests — DONE | 🟢 DONE | L | — |
| **GAP-202** | `/repo-status` skill blind to GitHub Security (Dependabot, code-scanning, secret-scanning) | 🟠 P1 Meta | S | Wave 10 Sprint 0 |

**Status:** 🟠 Re-opened 2026-04-21 — GAP-202 filed after `/repo-status` reported GREEN while 3 HIGH CVEs were live on main. Meta-P1 per `meta-gap-priority.md` §3 (skill blindspot = force multiplier). 10/11 gaps DONE; 1 OPEN.

---

### Epic 13: Frontend Quality
**Goal:** Fix UI issues từ UI audit.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-076 ✅ | KiteHub capture mock auth — DONE | 🟢 DONE | M |
| GAP-077 ✅ | KiteClass dev error overlay — DONE | 🟢 DONE | S |
| GAP-078 ✅ | KiteHub dark mode not switching — DONE | 🟢 DONE | M |
| GAP-079 ✅ | KiteClass i18n gaps — DONE | 🟢 DONE | M |
| GAP-080 | KiteHub dashboard loading/error UX | 🟡 P2 | M |

**Status:** 4/5 DONE. Only P2 GAP-080 open.

---

### Epic 14: Quality Governance
**Goal:** Meta-process — review standards cho outputs mà chưa có review process.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-048 ✅ | Output review standards coverage — DONE | 🟢 DONE | M |
| GAP-049 | Business logic correctness (stakeholder review) | 🟠 P1 | M |
| GAP-050 | Persona-based business review process | 🟡 PLANNED | S |
| GAP-101 ✅ | Docs folder README standardization (4 folders) — DONE PR #349 | 🟢 P3 | S |
| GAP-102 🟡 | 05-guides completion + ADR kickoff — PARTIAL (Part 2 DONE #350, Part 1 P2 DONE #352, Part 1 P1 open) | 🟡 P2 | M |
| GAP-103 ✅ | Deploy philosophy consolidation + AWS Agent Plugins ADR — DONE PR #351 | 🟢 P3 | M |
| GAP-149 ✅ | Audit skill grep scope multi-module (prevent GAP-107 false positive) — DONE 2026-04-20 Part C Sprint 0 | 🟢 DONE | S |
| GAP-150 | BRD docs completion (5 skeleton files: business-objectives, compliance-scope, pricing-model, nfr-catalog, go-to-market) | 🟠 P1 biz-logic | M |
| GAP-151 | Persona-specific acceptance criteria template + 4 Tier 1 AC docs (P1/P2/P3/P5) | 🔴 P0 biz-logic | M |
| GAP-152 | Execute persona review round 1 — role-play 4 Tier 1 personas + reports | 🔴 P0 biz-logic | L |
| GAP-153 | Secondary persona AC (Student/Parent/Teacher/Admin × tenant contexts — 8 P0 cells) | 🔴 P0 biz-logic | M |
| GAP-154 | **BRD scope expansion umbrella** — 22 missing BRD docs via simulation (7 P0, 7 P1, 5 P2, 3 P3); Phase 1 sub-gaps FILED 2026-04-20 | 🔴 P0 biz-logic | XL (phased) |
| GAP-180 | **Terms of Service** (customer legal contract) — Wave 8 | 🔴 P0 biz-logic | M |
| GAP-181 | **Acceptable Use Policy** (AUP) — Wave 8 | 🔴 P0 biz-logic | M |
| GAP-182 | **Privacy Policy** — VN PDPL mandatory — Wave 8 | 🔴 P0 biz-logic | L |
| GAP-183 | **Refund + Dispute Resolution** — VN Consumer Protection mandatory — Wave 8 | 🔴 P0 biz-logic | M |
| GAP-184 | **Data Retention + Deletion Policy** — VN PDPL Art 6 mandatory — Wave 8 | 🔴 P0 biz-logic | M |
| GAP-185 | **Billing Terms + VAT/TCT compliance** — Circular 78/2021 mandatory — Wave 8 | 🔴 P0 biz-logic | L |
| GAP-186 | **Child Protection Policy** (K-12 P5 blocker) — Law on Children 2016 — Wave 8 | 🔴 P0 biz-logic | L |
| GAP-190 🟡 | KiteHub SEO — infra shipped (sitemap/robots/OG/JsonLd/blog); gap narrowed to pricing SSR, canonical schemas, GA4, content plan, Lighthouse CI — **Wave 9** | 🟠 P1 biz-logic | M |
| GAP-191 | Domain Registration + DNS Strategy (kitehub.vn + per-instance + custom CNAME) — **Wave 9** | 🟠 P1 biz-logic | M |
| GAP-192 | **Trial → Paid Zero-Downtime Migration** (state machine + outbox + rollback; layers under GAP-026) — **Wave 9 (Agent 9-A, first priority)** | 🔴 P0 biz-logic | L |
| GAP-193 | Session Orchestration + /start-session skill + multi-session lock — **Wave 8b (Agent 8b-E)** | 🟠 P1 meta | M |
| GAP-194 | Bash/Python Script Compliance (shellcheck + ruff in CI; no .husky exists yet) — **Wave 8b (Agent 8b-D)** | 🟠 P1 meta | S |
| GAP-195 | Starter-Kit Bulk Retro-Sync (export learnings to remote kit) — **Wave 8b (Agent 8b-F)** | 🟡 P2 meta | M |
| GAP-197 🟡 | Attendance Calendar — component shipped (PR 3.8.1); gap narrowed to parent/student variants + a11y + week view + UI review + E2E — **Wave 11** (parent variant blocked by GAP-052 Wave 10) | 🟡 P2 feature | S |
| GAP-198 | FE↔BE Decoupled Consumer-Side Contract (producer-side DONE via GAP-090/InstanceApiContractTest) — **Wave 8b (Agent 8b-F)** | 🟡 P2 meta | M |
| GAP-199 | Rework Audit for Context-Degraded PRs (Wave 6-8 era) — **Wave 8b (Agent 8b-E)** | 🟠 P1 meta | M |
| GAP-200 | School MIS/SMS Integration (VNEDU + SMAS + Base.vn) — **Wave 9 (Agent 9-C)** | 🟠 P1 biz-logic | XL |
| GAP-201 | Tenant Off-boarding Runbook (cancel UX + export bundle + purge; consumes GAP-073 deferred) — **Wave 8b (Agent 8b-F)** | 🟠 P1 meta | M |

**Dropped:** GAP-196 (9router ADR) — user decision 2026-04-20, not effective for project scope.

**Dependencies:** GAP-101 → GAP-102 (needs 05-guides README) → GAP-103 (needs ADR template + 02-architecture README). GAP-151 blocks GAP-152. GAP-153 blocks GAP-152 P5 review (Student/Parent AC critical). GAP-150 Phase 2 (content fill) blocked on stakeholder engagement. GAP-190/191 block GTM (GAP-150 Phase 2). GAP-192 depends on GAP-108 (trial config hardcoded); aligns with GAP-026 AI-budget layer. GAP-197 parent-variant blocked by GAP-052. GAP-199 consumes GAP-193 detection heuristic. GAP-201 consumes GAP-073 deferred items.
**Split:** GAP-101 standalone PR. GAP-102 split Part 1 (guides) + Part 2 (ADR kickoff). GAP-103 after 101+102.

**Part C Sprint 0 (meta-skills calibration):** GAP-149 closed. 5 audit skills (business-logic, performance, ops-readiness, security, api-contract) now document safe grep scope patterns. Retroactive check confirmed GAP-106/108/110 are valid (not false positives).

**BRD + persona governance wave (2026-04-20):** GAP-150/151/152 bundled with `meta-gap-priority.md` §3 update adding Business-Logic tier. GAP-049 + GAP-050 AC scope-split for clarity (process vs content vs framework vs execution).

**Coverage sync 2026-04-24:** Added 8 previously-missing meta gaps to this epic:

| Gap | Title | Status | Epic rationale |
|-----|-------|:------:|----------------|
| GAP-170 | Gap review template + skill | 🟢 DONE (Wave 8b-A) | governance |
| GAP-171 | Rules docs ADR-like review process | 🟢 DONE (Wave 8b-A) | governance |
| GAP-172 | Architecture ADR process | 🟢 DONE (Wave 8b-B) | governance |
| GAP-173 | Email template review checklist | 🟢 DONE (Wave 8b-C) | governance |
| GAP-174 | Marketing + legal docs review | 🟢 DONE (Wave 8b-C) | governance |
| GAP-175 | Logs format standard (spec only; impl Wave 7) | 🟢 DONE (Wave 8b-D) | governance spec |
| GAP-176 | UI/UX Pro Max skill integration | 🔵 OPEN | skill upgrade |
| GAP-205 | CI history retention policy + automation (50-run cap) | 🟢 DONE (2026-04-24 PR #471) | CI governance |
| GAP-206 | `/start-session` skill accuracy fix | 🟢 DONE (2026-04-24 PR #468) | skill fix |
| GAP-207 | `/start-session` VN language per CLAUDE.md | 🟢 DONE (2026-04-24 PR #470) | skill fix |
| GAP-212 | Fix `DefaultUrlAllowlistValidatorTest` flaky DNS of `api.partner.com` → loopback (blocks every Core CI run; pre-existing surfaced by PR #474) | 🔵 OPEN 🟠 P1 | test-only fix (RFC-2606 `.invalid`) |
| GAP-213 | Spring Cloud BOM resolution fails on Dependabot all-deps PRs that bump Boot parent (kiteclass-gateway + kitehub-gateway poms) — blocks weekly Spring-touching Dependabot PRs | 🔵 OPEN 🟠 P1 | pom BOM fix (likely explicit `spring-cloud.version` bump alongside Boot, or root-pom BOM import) |
| GAP-214 | Wave 5 post-wave audit suite refresh — API contract + security + performance + ops + quality stale during Wave 5 sprint; closed by Sub-PR 5.6 wave completion. Used as `AUDIT_OVERRIDE` link for Sub-PR 5.5 PR #529. | 🟢 DONE (5.6a 2026-04-25) — 5 audits committed: api 95/100, sec 85/100, perf 63/100, ops 52/100, quality 78/100 | governance / audit refresh |
| GAP-215 | `BrandingService.getBranding()` not `@Cacheable` — DB hit per document render (Wave 5 perf audit P0-1). | 🟢 DONE (Sub-PR 5.6b 2026-04-25) — `@Cacheable("branding-by-tenant", sync=true)` + `@CacheEvict` on mutators + `BrandingCacheIntegrationTest` (5 cases) | backend / cache wiring |
| GAP-216 | PDF/XLSX/DOCX p95 micro-benchmark + soft-cap regression assertion (Wave 5 perf audit P0-2). | 🟢 DONE (Sub-PR 5.6b 2026-04-25) — soft-cap timing assertions in 3 generator tests (PDF <4s, XLSX/DOCX <2s); full JMH suite is a Wave 7 follow-up | testing / perf canary |
| GAP-217 | Alert rules for `/api/v1/documents/*` (p95, error rate, cache miss storm) — Wave 5 ops audit P0. | 🟡 PARTIAL (Sub-PR 5.6b 2026-04-25 filed 3 rules in helm + docker prometheus configs); routing deferred — blocked-by GAP-120 Alertmanager | ops / alerting |
| GAP-218 | PDF font-missing runbook + image-build validation step (Wave 5 ops audit P0). | 🟢 DONE (Sub-PR 5.6b 2026-04-25) — Dockerfile font-presence assertion + `documents/05-guides/runbooks/pdf-generation-font-not-found.md` | ops / runbook + CI |
| GAP-219 | Wave 5 audit follow-ups umbrella — 5 P1 + 8 P2/P3 sub-bullets across api/sec/perf/ops categories. Tracking-only; sub-bullets split into individual gaps when scheduled. | 🔵 OPEN 🟠 P1 | umbrella / maintenance |
| GAP-220 | `BrandingVersionService.snapshot` JSONB column type mismatch — `branding_versions.snapshot_json` column is jsonb but JDBC sends varchar. Wave 4 latent bug surfaced by Sub-PR 5.6b `BrandingCacheIntegrationTest`. Production tenants updating branding will 500. Workaround: `@MockBean` skips path in test; real fix requires `@JdbcTypeCode(SqlTypes.JSON)` on entity. | 🔵 OPEN 🟠 P1 | backend / persistence |
| GAP-224 | `collect-state.sh` blocker regex — sub-IDs (GAP-222a) collapse, prose cross-refs (BLOCKS GAP-006) pollute output, `sort -u` breaks priority order. Cosmetic accuracy fix; affects every `/start-session`. | 🔵 OPEN 🟡 P3 | skill fix (single-file) |
| GAP-225 | **Scaffolded-as-DONE Governance Closure Umbrella** — 5 gaps (008/009/012/015/018) shipped Wave 2-4 marked DONE despite explicit deferred items + missing audit-gate rules + missing skills + matrix mismatches. Captures systemic pattern + 3 cluster fix plan (C1 AI agent, C2 Saga, C3 AI branding — last covered by GAP-223 Sub-PR 223.1). Docs-only umbrella; Phase 2-3 (C1+C2) deferred until scheduled. | 🔵 OPEN 🟠 P1 meta | XL (phased — C3 done via GAP-223; C1+C2 future) |
| GAP-226 | Real WCAG contrast measurement (replace `ContrastCheck` scaffold pass) — implements WCAG 2.1 §1.4.3 luminance formula on theme JSON pairs; baseline §3 8/20 → ≥16/20 target | 🔵 OPEN 🟠 P1 feature | M (Wave 8+) |
| GAP-227 | Real visual regression diff (replace `VisualRegressionCheck` scaffold pass) — needs screenshot service + MinIO baseline store + pixel-diff engine; baseline §3 → ≥16/20 target | 🔵 OPEN 🟠 P1 feature | L (Wave 8+; depends on screenshot service) |
| GAP-228 | Real ML content classifier (replace `ContentModerationService` 3-stage scaffold) — toxicity/NSFW/brand-safety models + admin review queue; closes GAP-018 deferred scope | 🔵 OPEN 🟠 P1 feature | L (Wave 8+; depends on ML inference infra) |
| GAP-229 | AI Branding business docs v2 sync + 3 missing user guides — surfaced by GAP-016 verification sweep; v2 implementation in kiteclass-core but business `01-business/kitehub/ai-branding/{rules,use-cases,api-contract}.md` still v1; 3 user guides (branding-integration, wizard-flow, template-contribution) DO NOT EXIST | 🔵 OPEN 🟠 P1 biz-logic | L phased (~5-6h: Phase 1 docs ~2h + Phase 2 guides ~3h + Phase 3 verify ~30min) |

---

### Epic 15: Vietnam K-12 Education Features

**Goal:** Vietnamese K-12 school operational features — attendance models, reports, payroll, integrations specific to VN education context. Most gaps filed 2026-04-15..17 from deep K-12 domain analysis.

**Why domain-specific epic:** These touch Vietnamese education law (Thông tư 22, Luật Giáo dục), local vendors (VNEDU, VietQR, Zalo, Viettel SMS), and cultural patterns (Hạnh kiểm, GVCN, lên lớp/ở lại lớp). Distinct from generic K-12 or SaaS patterns.

| Gap | Title | Priority | Status | Effort |
|-----|-------|:--------:|:------:|:------:|
| GAP-051 | Bulk Import Users via xlsx/CSV | 🟠 P1 | 🟢 DONE Wave 1 MVP | M |
| GAP-055 | Official Report Card (Bảng điểm VN format, Thông tư 22) | 🔴 P0 biz-logic | 🔵 OPEN | L |
| GAP-056 | Homeroom Teacher (GVCN) concept | 🟠 P1 | 🔵 OPEN | M |
| GAP-057 | Teacher Payroll + Commission Calculation | 🟠 P1 | 🔵 OPEN | L |
| GAP-059 | Student Conduct / Hạnh kiểm tracking | 🟠 P1 | 🔵 OPEN | M |
| GAP-060 | Period-based Attendance (nhiều tiết/ngày) | 🟠 P1 | 🔵 OPEN | M |
| GAP-061 | Promotion / Retention Logic (Lên lớp / Ở lại lớp) | 🟠 P1 | 🔵 OPEN | M |
| GAP-062 | Payroll Bank Integration (Batch Transfer) | 🟡 P2 | 🔵 OPEN | L |
| GAP-063 | SMS + Zalo Notification Integration | 🟠 P1 | 🔵 OPEN | M |
| GAP-064 | SCORM / xAPI Compliance (Corporate Training variant) | 🟡 P2 | 🔵 OPEN | L |
| GAP-066 | KiteHub Unified Reports / Analytics Dashboard | 🟡 P2 | 🔵 OPEN | L |
| GAP-067 | KiteHub Instance Control Plane (AWS-/Vercel-style ops console) | 🟡 P2 | 🔵 OPEN | XL |
| GAP-068 | KiteHub Admin AI-Branding Console | 🟡 P2 | 🔵 OPEN | L |
| GAP-109 | Student bulk-import rules undocumented | 🟠 P1 | 🟢 DONE Wave 9-D | S |

**Dependencies:**
- GAP-055 depends on Wave 2 academic year/semester model (DONE via GAP-053)
- GAP-060 depends on period-based scheduling (partial via GAP-099)
- GAP-061 depends on GAP-055 (report card gates promotion)
- GAP-063 pairs with GAP-200 (school MIS integration, broader scope)
- GAP-066/067/068 depend on KiteHub subscription + instance ops stability (Wave 9 shipped)

**Status:** 2/14 DONE. Remaining 12 OPEN are split across 3 domains: reporting/grades (055, 061, 066), teacher ops (056, 057), attendance/conduct (059, 060), integrations (062, 063, 064), admin (067, 068).

**Suggested wave assignment:**
- Wave 10 candidate: GAP-055 (P0) + GAP-056/060/061 cluster (VN K-12 core)
- Wave 11 candidate: GAP-057/059 + GAP-063 (teacher + comms)
- Wave 12+: GAP-062/064/066/067/068 (P2 tier)

---

### Coverage additions to existing epics (2026-04-24 sync)

**Epic 6 (Operations & Scale) += 12 gaps** (observability + ops hardening, Part A audit follow-ups):

| Gap | Title | Priority | Notes |
|-----|-------|:--------:|-------|
| GAP-112 | Distributed tracing missing | 🟠 P1 | Wave 7 observability |
| GAP-113 | Frontend error tracking missing | 🟠 P1 | Sentry/Rollbar |
| GAP-114 | Structured JSON logging + MDC propagation | 🟠 P1 | Wave 7 (standard shipped via GAP-175) |
| GAP-115 | Log aggregation pipeline (ELK/Loki) | 🟠 P1 | Wave 7 |
| GAP-116 | PII scrubbing in logs | 🔴 P0 | VN PDPL Art 6 |
| GAP-118 | MinIO backup + replication strategy | 🔴 P0 | DR foundation |
| GAP-119 | Platform-wide DR runbook + RTO/RPO | 🔴 P0 | Ops readiness |
| GAP-121 | Per-alert runbooks library | 🟠 P1 | Consumes GAP-120 |
| GAP-122 | Missing platform-critical alerts | 🟠 P1 | Extends GAP-120 |
| GAP-123 | HPA for KiteHub services | 🟠 P1 | Scale readiness |
| GAP-124 | PodDisruptionBudget + NetworkPolicy hardening | 🟠 P1 | k8s hardening |
| GAP-130 | Docker compose zero resource limits (host OOM risk) | 🟡 P2 | Dev/staging only |

**Epic 13 (Frontend Quality) += 5 gaps** (2026-04-20 ui-review P2 findings):

| Gap | Title | Priority |
|-----|-------|:--------:|
| GAP-137 | Bulk import frontend UI missing (Wave 1 backend inaccessible) | 🟠 P1 |
| GAP-138 | KiteClass landing hero — duplicated "Chuyên nghiệp" copy | 🟡 P2 |
| GAP-139 | Parent dashboard MVP is placeholder-only | 🟠 P1 |
| GAP-140 | `form-select` default placeholder hardcoded English | 🟡 P2 |
| GAP-141 | Register-student date input locale-forced dd/mm/yyyy | 🟡 P2 |

**Epic 7 (UX & Conversion) += 3 gaps:**
- GAP-071 — Branding migration on tier upgrade/downgrade (🟡 P2, OPEN)
- GAP-072 — Scheduled rebrand + academic-year-tied branding refresh (🟡 P2, OPEN)
- GAP-074 — AI-generated alt-text for accessibility (a11y) (🟠 P1, OPEN)

**Epic 9 (Developer Experience) += 1 gap:**
- GAP-075 — Developer sandbox tenant environment (🟡 P2, OPEN)

**Epic 10 (Cross-cutting) += 1 gap:**
- GAP-065 — Migration chain not fresh-deploy safe (🟢 DONE, meta/ops fix)

---

## Updated Priority Tiers (186 gaps, refreshed 2026-04-24)

| Tier | Description | Count |
|------|-------------|-------|
| 🟥 **Block GA** (remaining open) | Core pipeline foundation + doc gen + K-12 core + observability P0 | ~12 gaps |
| 🟨 **Block GROWTH** (open) | UX, conversion, ops, webhooks, VN integrations | ~30 gaps |
| 🟦 **Block SCALE** (open) | Multi-brand, marketplace, advanced, admin consoles | ~18 gaps |
| ⬜ **Process/Internal** (open) | Advanced governance, persona review, skills | ~14 gaps |
| 🟡 **PARTIAL/PLANNED** | Scope-verified, waiting on wave assignment | 14 gaps |
| 🟠 **IN_PROGRESS** | Active wave or session work | 7 gaps |
| ✅ **CLOSED** | Completed Waves 1-9.5 + Part A/B/C audits + 2026-04-24 session | **81 gaps (44%)** |

### 🟥 Block GA — Only 6 remain open (refresh 2026-04-18)

| Gap | Title | Status | Effort |
|-----|-------|:------:|:------:|
| GAP-005 | AI queue fair scheduling | 🟡 Phase 2 open | M remaining |
| GAP-011 | Template library curation (30 templates) | 🟡 PLANNED Sprint 0 | L |
| GAP-014 🟡 | Wave mock plan include AI branding — planning v2-aligned 2026-04-26; impl GAP-235 | 🟡 PARTIAL | M |
| GAP-016 ✅ | Living docs impact scope — DONE Wave 7 (2026-04-26) | 🟢 DONE | — |
| GAP-046 | Design patterns applied systematically | 🟡 PLANNED Sprint 0 | M |
| GAP-047 | Document generation — Wave 5 DONE 2026-04-25; PPT deferred Wave 6 | 🟢 DONE | — |

**Previously listed GA blockers now CLOSED:** GAP-007, 008, 009, 010, 012, 013, 015, 018, 031, 041, 042, 081, 082, 086, 087, 088, 092, 093.

---

**Last Updated:** 2026-04-25 (**Wave 5 DONE** — Sub-PR 5.6b shipped wave closure + 4 P0 audit fixes from 5.6a. **GAP-047 → 🟢 DONE.** Wave 5 ledger: #474 5.0 + #476 5.1 PDF + #477 5.2 Excel + #478 5.3 Word + #529 5.5 branding + HTTP + #530 5.6a audit suite + 5.6b closure. Audit suite scores: api 95 / sec 85 / perf 63 / ops 52 / quality 78. P0 closures: GAP-215 cache, GAP-216 soft-cap canary, GAP-218 font runbook + Dockerfile assertion. GAP-217 PARTIAL (rules filed, routing deferred to GAP-120 Alertmanager). PPT deferred to Wave 6 per scope-lock. **Recommended next action:** **GAP-046 design-pattern audit** (next Meta-P0). Or Wave 10 GAP-055 report-card VN if business priority shifts. RTK pilot scaffolded (#531) — opt-in single-day measurement before any team-wide rollout.)

**Prior:** 2026-04-21 (**Wave 9.5 SHIPPED** via 4 parallel agents — PRs #415-#418. Pushed 2 PARTIALs → DONE (GAP-132 caching fan-out, GAP-134 @EntityGraph expand 3→9 repos). GAP-192 Phase 4b-i backend completeness shipped (45 new tests, 330 total in kitehub-subscription: webhook HMAC + scheduler + idempotency + retry + admin ops); stays 🟡 PARTIAL until FE integration Phase 4c. GAP-043 fan-out attempted 5 caches but 4/5 reverted after Redis+Jackson typing regression caught in integration tests; BrandingPackage proxy retained sync=true. Follow-up gap: harden CacheConfig serializer before re-attempt.)

### Session 3 refresh 2026-04-18 — ROADMAP status audit

Discrepancies fixed:
- GAP-081, 082, 083, 084, 085, 086, 087, 088, 089, 090 — were listed as P0 Block GA / P1 pending, actually all DONE → Epic 12 fully closed
- GAP-076, 077, 078, 079 — were listed P0/P1, actually DONE → Epic 13 reduced to 1 open (P2)
- GAP-048 — Epic 14 governance, actually DONE
- GAP-007, 008, 009, 010, 012, 013, 015, 018, 031, 041, 042 — core AI branding + security gaps DONE Wave 2-4, epic tables updated inline
- GAP-002 — async pipeline DONE Wave 3 (2026-04-18)
- GAP-015 — tenant provisioning auto-trigger DONE Wave 3 (was in Epic 1 as open)
- Priority Tier counts: 95 → 103 total, Block GA 24 → 6 actual open, CLOSED 15 → 48

Triggered by: status check found 6+ "Block GA" gaps already merged but ROADMAP not refreshed since 2026-04-14 wave log entries.

### New gaps 2026-04-18 (TODO audit post Wave 4)

- **GAP-098** (P2) — Notification settings API not implemented — `InstanceTab.tsx:57`
- **GAP-099** (P2) — Structured class schedule (replace free-form text) — `SubjectSection.java:24`
- **GAP-100** (P3) — Lunar calendar for VN holidays — `VnHolidayProvider.java`

### New gaps 2026-04-18 (docs folder governance audit)

- **GAP-101** (P3) — Docs folder README standardization (4 folders: 00-brd, 02-architecture, 05-guides, 07-archived)
- **GAP-102** (P2) — 05-guides completion (6 operational guides) + ADR kickoff (template + ADR-001 jobs+RabbitMQ)
- **GAP-103** (P3) — Deploy philosophy consolidation + ADR-002 AWS Agent Plugins evaluation

### Planning docs added 2026-04-18

- `documents/03-planning/plans/plan-ui-ux-design-system-integration.md` — 3-PR plan to adopt ui-ux-pro-max reasoning rules + upgrade ui-review skill to /148 scoring
- `documents/03-planning/waves/wave-05-document-generation.md` — Wave 5 plan for GAP-047. **Status: 🟢 APPROVED 2026-04-24 → IN PROGRESS (4/6 sub-PRs SHIPPED)** — Sub-PR 5.0 foundation + ADR-019 (#474), 5.1 PDF + invoice (#476), 5.2 Excel + attendance (#477), 5.3 Word + teacher contract (#478) all merged 2026-04-24. Remaining: Sub-PR 5.5 (branding integration) + 5.6 (wave completion). ADR-019 PROPOSED → ACCEPTED on Sub-PR 5.6 merge.

### Rules added 2026-04-18

- `.claude/rules/docs-folder-structure.md` — generic rule extending `planning-docs-structure.md` pattern to all `documents/` folders (GAP-101)

**Prior:** 2026-04-16 (added Epics 11-14, 48 new gaps from UI/process/SaaS audits)

### Audit Catch-up 2026-04-19 — 3 baselines shipped (Part A 3/5) — 🟢 COMPLETE

Parallel-agent execution (3 worktree-isolated agents, ~10-11 min wall-clock each, zero conflicts). Conflict-control applied per `feedback_parallel_agent_strategy.md`: pre-assigned GAP ranges, parent-owned shared files (ROADMAP + output-review-mandate + MEMORY consolidated in this PR), parent-sequenced merges (3 clean FF merges).

| Audit | PR | Score | Grade | Gaps (range) |
|-------|:--:|:-----:|:-----:|--------------|
| business-logic /100 (refresh, 27d stale) | #366 | 65/100 | D | GAP-104 → GAP-110 (7) |
| ops-readiness /100 (first-ever baseline) | #365 | 49/100 | F | GAP-111 → GAP-125 (15) |
| performance /100 (first-ever baseline) | #364 | 58/100 | F | GAP-126 → GAP-135 (10) |

**32 new gaps created (GAP-104 → GAP-135).**

Top P0 findings (meta-gaps listed first per `meta-gap-priority.md`):
- **GAP-104** (P0 meta) — Wave 3 fair-queue Phase 1 shipped 8+ config keys, 0 BR-QUEUE-* rules. Living Docs contract broken.
- **GAP-105** (P0 meta) — `parent-portal` domain missing 3-layer docs despite `ParentPortalProperties.java:16` referencing `BR-PARENT-003` (ghost rule ID).
- **GAP-111** (P0) — Monitoring stack (Prometheus/Grafana) only in dev docker-compose; production Helm/k8s deploys blind.
- **GAP-120** (P0) — Alertmanager has 7 alert rules but 0 receiver configured — alerts would fire silent.
- **GAP-117** (P0) — Backup restore never tested (GAP-093 shipped pg_dump but no restore drill/runbook).
- **GAP-126** (P0) — Admin dashboard calls `findAll() × 2` on Instance + Subscription tables no-cache, 6 stream aggregations per request.
- **GAP-127** (P0) — Frontend 0 code-splitting across 64 pages; framer-motion (~130KB) + recharts (~180KB) in initial bundle (~400-550KB First Load JS).
- **GAP-129** (P0) — `BrandingPackage` accepts `instanceId` param but ignores it, returns cross-tenant findAll — perf + multi-tenancy bug.

Status changes applied in this consolidation PR (`.claude/rules/output-review-mandate.md` §3):
- business-logic: stale (27d) → CURRENT (2026-04-19)
- ops-readiness: VIOLATION (never audited) → BASELINE_CAPTURED (2026-04-19, 49/100)
- performance: PLANNED → BASELINE_CAPTURED (2026-04-19, 58/100)

**Remaining Part A audits (per plan `documents/03-planning/plans/plan-audit-catchup-2026-04-19.md`):**
- Audit 4: ui-review /128 (8d stale)
- Audit 5: quality-audit /100 refresh (depends on Audits 1-4 findings)

### Audit Catch-up Part A — 5/5 COMPLETE (2026-04-19) — 🟢 COMPLETE

Continuation of 3/5 entry above. Audits 4+5 shipped in same session:

| Audit | PR | Score | Gaps |
|-------|:--:|:-----:|------|
| ui-review /128 (refresh, 8d stale) | #368 | KC 81/128, KH 59/128 (+1 each) | GAP-136 → GAP-142 (7) |
| quality-audit /100 (refresh, final) | #369 | **77/100 C+** (Δ −18 vs 95/100) | — (no new gaps per plan §3.5) |

**Total Part A gaps: 39** (GAP-104 → GAP-142). Running total 48/142 closed (34%).

**Calibration insight (Audit 5 report):** −18 delta is NOT a regression in 5 days. The 95/100 on 2026-04-14 was optimistic self-audit without specialist data (ops, perf were never audited). The 77/100 today is the FIRST HONEST BASELINE with ground-truth evidence from 4 specialist audits. Future deltas measure genuine improvement against 77, not inflated 95.

**Top 5 next-wave priorities (meta-boost per `meta-gap-priority.md`):**
1. **GAP-104** Wave 3 BR-QUEUE rules (Meta P0, 4-6h) — Living Docs contract broken
2. **GAP-105** parent-portal 3-layer docs (Meta P0, 4-6h) — ghost rule reference
3. **GAP-136** KiteHub custom error pages (Feature P0, 2-3h) — 5+ routes return English 404
4. **GAP-111 + GAP-120** monitoring + alertmanager prod Helm (Feature P0, 1-2d) — ops visibility
5. **GAP-128/129/133/131 batch** perf quick wins (Feature P0/P1, 1d)

Expected recovery per Audit 5: 77 → 85 (B+) end Week 2, → 90 (A) end Week 4.

**Governance turnaround COMPLETE:** hook (PR #362) enforces freshness; 5 audits now FRESH; baselines captured for 2 never-audited categories (ops, perf). Part B (fix waves) tracked via top-5 priorities above.

### Audit Catch-up Part B — 5/5 top priorities SHIPPED (2026-04-20) — 🟢 COMPLETE

Parallel-agent execution continued from Part A. 5 worktree-isolated agents fixed the Audit 5 top-5 priorities simultaneously. Wall-clock: Agent A 6 min, C 7 min, B 8 min, D 15 min, E 69 min (Maven + testcontainers). Zero merge conflicts — disjoint file sets.

| PR | Gap(s) closed | Agent | Highlights |
|:--:|---------------|:-----:|------------|
| #371 | GAP-104 (Meta P0) | A | 18 BR-QUEUE rules + 4 UC-AGENT-08..11 + metrics catalogue |
| #373 | GAP-105 (Meta P0) | B | parent-portal 3-layer: 30 BR-PARENT + 6 UC-PARENT + 5 endpoints; BR-PARENT-003 verified |
| #372 | GAP-136 (P0) | C | 3 error pages (not-found/error/global-error) + 13/13 tests green, dark-mode + Vietnamese |
| #374 | GAP-111 + GAP-120 (P0, foundation) | D | Prometheus + Alertmanager Helm deps + ServiceMonitors; 3 follow-up gaps (GAP-143/144/145) |
| #375 | GAP-128 + GAP-129 + GAP-131 + GAP-133 (P0/P1) | E | Installment scan fix, BrandingPackage tenant isolation, 6/9 HTTP timeouts, Hibernate batch=50; 5 new test files, ~1430 tests green |

**Gaps closed in Part B: 9** (GAP-104, 105, 111, 120, 128, 129, 131, 133, 136) → progress 48/142 → 57/147 (39%).

**New follow-up gaps created: 5**
- GAP-143 Grafana Dashboards Helm (P1, from D)
- GAP-144 Alertmanager Production Receivers (P0, from D)
- GAP-145 Loki Tracing Stack (P2, from D)
- GAP-146 HTTP timeouts remainder — payment/email/captcha (P2, from E)
- GAP-147 KiteHub Admin OpenAPI bean conflict — pre-existing (P2, discovered by E)

**Top-3 residual GA risks** (to review next wave):
- GAP-144 Alertmanager receivers (needed before prod deploy — alerts still silent)
- GAP-127 FE code-splitting (64 pages, ~400-550KB First Load JS) — not in Part B scope
- GAP-126 Admin dashboard findAll cache — not in Part B scope

**Superpowers adherence:** All 5 agents followed brainstorm + task-breakdown + (TDD where code) + implementation + self-review. Agent C and E delivered tests alongside code (TDD). Agents D and E self-caught writing to main worktree by mistake (hard rule 3 from `feedback_parallel_agent_strategy.md`) — no contamination landed on main.

**Conflict-control effectiveness:** 4/5 agents zero-collision auto-FF merge. Agent E merged with local leftover from worktree-root confusion (cosmetic, discarded before pull). No PR-level conflicts.

### Re-audit 2026-04-20 — Part B impact validation — 🟢 COMPLETE

Ran 2 parallel re-audit agents after Part B merge to measure delta. First attempt crashed silently (both agents stopped ~21 min post-spawn, coincident with `mcp__ide__*` disconnect — unrelated infra issue). Respawn succeeded cleanly.

| Category | Baseline 2026-04-19 | Refresh 2026-04-20 | Δ | PR |
|----------|:-------------------:|:------------------:|:-:|:--:|
| business-logic /100 | 65 D | **72 C** | +7 | #379 |
| performance /100 | 58 F | **64 D** | +6 | #378 |

**Business-logic findings (PR #379):**
- 2 CLOSED: GAP-104 (Wave 3 BR-QUEUE verified), GAP-105 (parent-portal 3-layer verified)
- 1 FALSE POSITIVE retracted: **GAP-107** — baseline grep scope missed `kiteclass/kiteclass-core/`; `ResilientAIClient` + `MockAIClient` + `OllamaAIClient` all exist with correct `@Profile("ai-live")` wiring
- 1 NEW: **GAP-148** (P2) — `BR-QUEUE-015..018` circuit breaker config exists in kitehub-branding but 0 `@CircuitBreaker` annotation (dead config)
- 7 unchanged (GAP-106/108/109/110 + 3 minor)

**Performance findings (PR #378):**
- 3 CLOSED: GAP-128 (installment PK lookup), GAP-129 (BrandingPackage tenant + V45 index + regression test), GAP-133 (Hibernate batch=50 × 5 services)
- 1 PARTIAL: GAP-131 (6/9 sites; remainder → GAP-146)
- 6 UNCHANGED: GAP-126, 127, 130, 132, 134, 135 (not in Part B scope)
- 0 new gaps, 0 regressions
- Category deltas: DB +3, API +2, Cache 0, FE 0, Resource +1

**Lessons learned added to skill roadmap (future work):**
- Business-logic-audit skill needs explicit broader grep scope (not just `kitehub/` + `kiteclass/` top-level) — risked false-positive like GAP-107
- Re-audit pattern works: shows calibrated delta + flags regressions; took ~5-8 min per agent

**Cumulative progress after re-audit:**
- Progress 57/147 → 58/148 (GAP-107 closed, GAP-148 added)
- Quality-audit 77/100 unchanged (not refreshed this round)
- Next recovery milestone: 77 → ~80 B- after next sprint closing GAP-148 + GAP-146 + GAP-132 (1-2 days)
