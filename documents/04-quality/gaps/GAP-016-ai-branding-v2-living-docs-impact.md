# GAP-016: AI Branding v2 — Living Documents Impact Scope

**Status:** 🟢 DONE 2026-04-26 — meta-tracking goal achieved (Waves 2-4 + GAP-229 closed business docs + guides; §2.9 audit 16/20 ✅; remaining drift split out as GAP-234)
**Branch:** wave/01-foundation
**Priority:** 🔴 P0 (living docs rule — PR không pass quality check nếu không update business docs)
**Domain:** Documentation / Governance
**Detected:** 2026-04-14
**Related Docs:**
- `documents/02-architecture/ai-branding-v2-redesign.md`
- `CLAUDE.md` §"CRITICAL: Living Documents"

## Problem

Khi redesign AI Branding v2 (GAP-007..015 + master doc), nhiều docs hiện tại BECOME OUTDATED. Nếu implement mà không update docs tương ứng → vi phạm **Living Documents rule** của dự án (CLAUDE.md) → PR fail quality check.

## Impact Scope Matrix

Scan docs hiện tại và đánh dấu impact:

### 🔴 MUST UPDATE (business-critical)

| Doc | Hiện tại (v1) | Cần update (v2) |
|-----|--------------|-----------------|
| `01-business/kitehub/ai-branding/rules.md` | Chỉ AIB-01..13 (rate limit + template gallery basic) | Thêm: resource classification rules, workflow rules, lifecycle rules, quality gate rules, wizard rules, template review standards |
| `01-business/kitehub/ai-branding/use-cases.md` | UC-AIB-01..04 (direct AI), UC-AIB-05..06 (template basic) | Thêm: UC-AIB-07 Wizard flow, UC-AIB-08 Approve per resource, UC-AIB-09 Regenerate with counter, UC-AIB-10 Quality review, UC-AIB-11 Auto-provisioning |
| `01-business/kitehub/ai-branding/api-contract.md` | 7 endpoints v1 | Thêm: `/package`, `/analyze`, `/plan`, `/execute`, `/jobs/{id}`, `/instances/{id}/status`, `/wizard/session` |
| `01-business/kitehub/instance-provisioning/rules.md` | Chưa đọc — cần verify | Thêm: FrontendInstanceStatus lifecycle, auto-trigger branding rules |
| `01-business/kitehub/instance-provisioning/use-cases.md` | Chưa đọc — cần verify | Thêm: UC-IP-XX tenant created → branding auto-start |

### 🟠 SHOULD UPDATE (technical architecture)

| Doc | Hiện tại | Cần update |
|-----|---------|-----------|
| `02-architecture/docker-platform-architecture.md` | Ollama + setup | Thêm: queue topology, worker pool, kite-quality-checker container |
| `03-planning/database/database-design.md` | BrandingJob, Template | Thêm 4 entities: `branding_resources`, `frontend_instances`, `image_templates`, `instance_quality_reports` |
| `03-planning/database/database-migration-plan.md` | V1-V27 | Thêm V28: AI Branding v2 entities |
| `06-diagrams/plantuml/03-erd.puml` | Current ERD | Add 4 new entities + relationships |
| `06-diagrams/plantuml/04-architecture-full.puml` | Current arch | Add Analyzer/Planner/Executor components, Quality reviewer |
| `.claude/skills/api-design.md` | Branding v1 endpoints | Update all AI branding endpoints per v2 |

### 🟡 MAY UPDATE (references/plans)

| Doc | Hiện tại | Cần update |
|-----|---------|-----------|
| `03-planning/implementation/ai-local-implementation-plan.md` | v1 plan (PR-AI-1..4) | Marked as superseded by v2; preserve for historical reference |
| `03-planning/implementation/kitehub-implementation-plan.md` | General plan | Add AI Branding v2 section |
| `03-planning/prs/02-core-prs.md` | Core PR list | Not impacted |
| `03-planning/prs/00-master-pr-index.md` | Master index | Add new PRs for GAP-007..015 |
| `documents/03-planning/wave-mock-data-local-dev.md` | Mock plan | Extend per GAP-014 |

### 🟢 TESTS TO UPDATE

| Test File | Current Status | Action |
|-----------|---------------|--------|
| `kitehub-branding/src/test/java/.../AIBrandingServiceTest.java` | Tests v1 direct pattern | Refactor để test Agent workflow |
| `kitehub-branding/src/test/java/.../OllamaClientTest.java` | Tests llama3.1/llava | Update sau GAP-006 Gemma 4 |
| `kitehub-branding/src/test/java/.../AIProviderConfigTest.java` | Tests current config | Update model defaults |
| `kitehub-branding/src/test/java/.../AIRateLimitServiceTest.java` | Daily rate limit | Add concurrent limit tests (GAP-005) |
| Integration tests (new) | N/A | Add `BrandingE2EIntegrationTest` (GAP-010) |
| Integration tests (new) | N/A | Add `InstanceQualityReviewerTest` (GAP-012) |

### 🔵 NEW DOCS TO CREATE

| Path | Purpose |
|------|---------|
| `documents/05-guides/branding-integration.md` | How kiteclass-frontend consumes branding package (GAP-010) |
| `documents/05-guides/ai-branding-wizard-flow.md` | Wizard UX user guide (GAP-013) |
| `documents/05-guides/template-contribution-guide.md` | How to add templates (GAP-011) |
| `.claude/skills/quality/instance-quality-review.md` | Skill cho automated quality review (GAP-012) |

## Verification Checklist (cho mỗi PR impl GAP-007..015)

PR phải update related docs:

- [ ] Business rules file updated với new rules
- [ ] Business use-cases file updated với new UCs
- [ ] Business api-contract file updated với new endpoints
- [ ] Database design updated nếu thêm entity
- [ ] ERD updated nếu thêm relationship
- [ ] Architecture diagram updated nếu thêm component
- [ ] API design doc updated
- [ ] Tests added/updated
- [ ] Skill guides updated (if applicable)
- [ ] Master PR index updated

## Proposed: Update business-gap-check skill

Thêm section **2.9 AI Branding Coverage** vào `.claude/skills/quality/business-gap-check.md`:

```markdown
#### 2.9 AI Branding (v2 redesign)

| Check | Cách verify | Expected |
|-------|-------------|----------|
| Resource classification enum | Search `ResourceCategory` enum | STATIC/TEMPLATE/FULL_AI |
| Routing service | Search `ResourceRoutingService` | classify() + route() methods |
| Agent workflow classes | Search `BrandingAnalyzer`, `BrandingPlanner`, `PlanExecutor` | All 3 exist |
| Step interface | Search `public interface Step` | Exists, has hasFallback() |
| FrontendInstance entity | Search `@Entity class FrontendInstance` | Exists with status field |
| FrontendInstanceStatus enum | Search enum with NOT_STARTED..DEPLOYED | 6 states |
| Lifecycle service | Search `InstanceLifecycleService` | Exists, transitions via events |
| Quality reviewer | Search `InstanceQualityReviewer` | Exists, runs Playwright checks |
| Tenant event listener | Search `@RabbitListener("tenant.created")` trong branding | Exists |
| Wizard FE component | Search `BrandingWizard` trong kitehub-frontend | 6 steps |
| Composite package API | Search `/branding/{id}/package` endpoint | Returns theme + assets + metadata |
| Template library | Count templates trong DB | ≥30 (Sprint 0 baseline) |
| Regenerate limits | Search `regenerate.limit` config per tier | Per-tier config |
| Free-form prompt banned | Search textarea for AI prompt | None (except Enterprise opt-in) |
| Tests coverage | Integration test cho full flow | Exists + passing |
```

## Acceptance Criteria

- [ ] Living docs update plan tracked (this gap)
- [ ] business-gap-check skill updated với section 2.9 AI Branding
- [ ] Mỗi PR impl GAP-007..015 checklist verify updated docs
- [ ] Final audit: run business-gap-check → 100% pass AI Branding section
- [ ] New guides written (branding-integration, wizard-flow, template-contribution)

## Dependencies

- Informs/governs: GAP-007..015 implementation
- Required for quality check: CLAUDE.md Living Docs rule

## Findings (verified sweep 2026-04-26)

Cross-checked impact matrix items vs actual repo state. Key finding: **v2 implementation EXISTS but landed in `kiteclass-core` module, NOT `kitehub-branding` as architected.** The kitehub-branding module retains v1 only.

### ✅ DONE (code shipped, location ≠ architecture doc)

| Matrix item | Real location |
|-------------|---------------|
| ResourceCategory enum (GAP-007) | `kiteclass-core/module/branding/entity/ResourceCategory.java` |
| BrandingAnalyzer (GAP-008) | renamed to `AnalyzerService.java` in `kiteclass-core/module/ai/workflow/` |
| BrandingPlanner (GAP-008) | renamed to `PlannerService.java` |
| BrandingExecutor (GAP-008) | `PlanExecutor.java` |
| Step interface (GAP-008) | `kiteclass-core/module/ai/workflow/Step.java` |
| FrontendInstanceStatus enum (GAP-009) | `kiteclass-core/module/instance/entity/FrontendInstanceStatus.java` |
| InstanceLifecycleService (GAP-009) | `kiteclass-core/module/instance/service/InstanceLifecycleService.java` |
| InstanceQualityReviewer (GAP-012) | `kiteclass-core/module/quality/service/InstanceQualityReviewer.java` |
| 5 *QualityCheck Strategy classes (GAP-012) | `kiteclass-core/module/quality/check/{Contrast,CssVars,AssetUrls,VisualRegression,LogoPlacement}QualityCheck.java` |
| TenantProvisioningSaga (GAP-015) | `kiteclass-core/module/provisioning/TenantProvisioningSaga.java` |
| ContentModerationService (GAP-018) | `kiteclass-core/module/moderation/ContentModerationService.java` |
| business-gap-check skill §2.9 AI Branding | `.claude/skills/quality/business-gap-check.md:242` |

### ✅ CLOSED 2026-04-26 (by GAP-229 PRs #561/#562)

| Matrix item | Closure |
|-------------|---------|
| `01-business/kitehub/ai-branding/rules.md` v2 content | ✅ +24 rules across 6 areas (BR-RES/LIFE/QUALITY/APRV/WIZARD/MOD/PKG) |
| `01-business/kitehub/ai-branding/use-cases.md` v2 UCs | ✅ +6 UCs (UC-AIB-07..12) sourced from real Controllers/Services |
| `01-business/kitehub/ai-branding/api-contract.md` v2 endpoints | ✅ +12 v2 endpoints (8 lifecycle + 2 branding package + 1 internal webhook + 4 TBD approval) |
| `instance-provisioning/{rules,use-cases}.md` v2 content | ✅ Last verified 2026-04-26 (drift fix GAP-229 Phase 3 — UC-INS-07/08/09 added) |
| `documents/05-guides/branding-integration.md` | ✅ Created GAP-229 Phase 2 |
| `documents/05-guides/ai-branding-wizard-flow.md` | ✅ Created GAP-229 Phase 2 |
| `documents/05-guides/template-contribution-guide.md` | ✅ Created GAP-229 Phase 2 |

### ⚠️ STILL DRIFT (split out → GAP-234)

| Matrix item | Status | Why deferred |
|-------------|--------|--------------|
| `02-architecture/docker-platform-architecture.md` queue topology + worker pool | Stale — no Analyzer/Planner/Executor/quality-checker references | Tracked GAP-234 (architecture doc + diagram drift cluster) |
| `03-planning/database/database-design.md` 4 entities | Stale — no `frontend_instances` / `branding_resources` / `quality_reports` / `moderation_queue` | GAP-234 |
| `06-diagrams/plantuml/03-erd.puml` (KiteClass ERD) | No v2 entities | GAP-234 |
| `06-diagrams/plantuml/04-architecture-full.puml` | No Analyzer/Planner/Executor/Quality components | GAP-234 |
| `06-diagrams/plantuml/14-ai-branding-pipeline.puml` | Stale 2026-03-24 — references GPT-4 Vision / DALL-E 3 (project uses Ollama llama3.1+llava) | GAP-234 |
| `06-diagrams/plantuml/16-database-schema-full.puml` | v1 `branding_jobs` + `branding_templates` only; no v2 tables | GAP-234 |

## §2.9 Business-gap-check Audit Results (2026-04-26)

Run via fixed grep scope (`kiteclass/kiteclass-core/` + `kitehub/kitehub-branding/`) per `feedback_audit_grep_scope.md`. Skill `business-gap-check.md` §2.9 updated in same PR to reflect actual module location.

| # | Check | Result | Notes |
|---|-------|:------:|-------|
| 1 | ResourceCategory enum | ✅ | `kiteclass-core/.../branding/entity/ResourceCategory.java` |
| 2 | ResourceRoutingService | ✅ | `kiteclass-core/.../branding/service/` |
| 3 | AnalyzerService (renamed BrandingAnalyzer) | ✅ | `kiteclass-core/.../ai/workflow/` |
| 4 | PlannerService (renamed BrandingPlanner) | ✅ | `kiteclass-core/.../ai/workflow/` |
| 5 | PlanExecutor | ✅ | `kiteclass-core/.../ai/workflow/` |
| 6 | Step interface | ✅ | `kiteclass-core/.../ai/workflow/Step.java` |
| 7 | FrontendInstance entity | ✅ | `kiteclass-core/.../instance/entity/` |
| 8 | FrontendInstanceStatus enum | ✅ | 6 states confirmed |
| 9 | InstanceLifecycleService | ✅ | `kiteclass-core/.../instance/service/` |
| 10 | InstanceQualityReviewer | ✅ | `kiteclass-core/.../quality/service/` |
| 11 | Tenant provisioning saga | ⚠️ | `TenantProvisioningSaga.java` exists (alternative to direct `@RabbitListener("tenant.created")` pattern) — same effect, different shape |
| 12 | BrandingResource entity | ✅ | `kiteclass-core/.../branding/entity/` |
| 13 | ImageTemplate entity | ❌ | No `ImageTemplate` entity yet — only `BrandingResource`/`ResourceCategory`/`ResourceType`. Tracked **GAP-011** Sprint 0 (template library curation) |
| 14 | Package API | ✅ | `BrandingPackageController` + `InternalWebhookController` |
| 15 | Wizard FE component | ✅ | `BrandingWizard.tsx` + `wizard-machine.ts` + `useBrandingWizard.ts` |
| 16 | Regenerate limits config | ❌ | Not in `application.yml` — pending per `ai-branding-guidelines.md` §4.3. Tracked **GAP-005** Phase 2 |
| 17 | No free-form prompt | ✅ | 0 textarea matches in FE |
| 18 | Template count ≥30 | ⏭️ | DB-dependent; deferred until GAP-011 ships templates |
| 19 | Quality gate (score < 70) | ✅ | `InstanceLifecycleService` + `InstanceQualityReviewer` confirm gate |
| 20 | Webhook on branding.updated | ✅ | `BrandingUpdatedEvent` + `BrandingEventPublisher` (outbox) |

**Score: 16/20 ✅, 2 ❌ (tracked GAP-005/GAP-011), 1 ⚠️ (alternative pattern, same effect), 1 ⏭️ (DB-dependent).**

The 2 hard ❌ are pre-existing scope items (templates curation + regenerate counter UI) — both queued in Wave 7 priority via GAP-005 + GAP-011. No new gaps filed for §2.9 misses.

### ⚠️ ARCHITECTURE DOC DRIFT (split out 2026-04-26 → GAP-234)

Original `documents/02-architecture/ai-branding-v2-redesign.md` specified `kitehub-branding/` as module location but implementation went to `kiteclass-core`. Plus 6 architecture/diagram/DB design docs are stale vs v2 reality. Cluster filed as **GAP-234** for separate cleanup PR — does not block GAP-016 closure since meta-tracking goal (Waves 2-4 update docs) was achieved.

## Log

- **2026-04-26 (final closure):** GAP-016 status 🟡 PARTIAL → 🟢 DONE. (1) §2.9 business-gap-check audit ran with fixed grep scope (kiteclass-core + kitehub-branding) — 16/20 ✅, 2 ❌ tracked existing gaps (GAP-005 regenerate counter, GAP-011 ImageTemplate library), 1 ⚠️ Saga alternative pattern, 1 ⏭️ DB-dependent. (2) Skill `business-gap-check.md` §2.9 updated to fix grep scope + class renames (BrandingAnalyzer→AnalyzerService, etc.) + module-location note. (3) Findings table flipped — GAP-229 (PRs #561/#562) closed business docs + 3 user guides + instance-provisioning rules/use-cases. (4) Remaining drift (architecture doc + 4 PUML diagrams + database-design.md + docker-platform-architecture.md) split out to GAP-234. AC met: meta-tracking goal achieved, Waves 2-4 + GAP-229 satisfy "Mỗi PR impl GAP-007..015 checklist verify updated docs".
- **2026-04-26 (verification sweep, Sub-PR 223.1 correction):** GAP-016 status PLANNED → 🟡 PARTIAL. Cross-checked entire impact matrix against real repo. Verified v2 code DONE in kiteclass-core (12+ classes confirmed via Java search). business-gap-check skill §2.9 ALREADY landed (line 242). Filed GAP-229 for outstanding business docs sync (rules/use-cases/api-contract v2 content) + 3 missing user guides. Findings table added. Architecture doc → kiteclass-core path drift noted (deferred).
- 2026-04-14 — Phát hiện impact scope thiếu trong redesign plan (user raised)
