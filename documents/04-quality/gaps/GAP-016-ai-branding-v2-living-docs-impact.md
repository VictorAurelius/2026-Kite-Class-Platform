# GAP-016: AI Branding v2 — Living Documents Impact Scope

**Status:** 🟡 PLANNED (Wave 1 Sprint 0)
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

Thêm section **2.9 AI Branding Coverage** vào `.claude/skills/business-gap-check.md`:

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

## Log

- 2026-04-14 — Phát hiện impact scope thiếu trong redesign plan (user raised)
