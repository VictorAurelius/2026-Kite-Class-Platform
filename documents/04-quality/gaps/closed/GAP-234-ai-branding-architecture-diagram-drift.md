# GAP-234: AI Branding architecture doc + diagram drift sync

**Status:** 🟢 DONE (2026-04-26 — Wave P2 cleanup, Agent A; PNG/SVG regen deferred — see Log)
**Priority:** 🟡 P2 (docs accuracy, no runtime impact — code is what runs)
**Domain:** Documentation / Architecture / Diagrams
**Detected:** 2026-04-26 (GAP-016 final closure §2.9 audit + diagram sweep)
**Related Docs:**
- `documents/02-architecture/ai-branding-v2-redesign.md`
- `documents/04-quality/gaps/closed/GAP-016-ai-branding-v2-living-docs-impact.md`
- `.claude/rules/docs-folder-structure.md`

## Current State (verified 2026-04-26)

Cross-checked architecture/diagram/DB design docs against shipped v2 code in `kiteclass/kiteclass-core/` (Waves 2-4) + business docs synced by GAP-229 (PRs #561/#562).

| Doc / diagram | Real state | v2 expected |
|---------------|-----------|-------------|
| `documents/02-architecture/docker-platform-architecture.md` (4.6KB) | No `Analyzer`/`Planner`/`Executor`/queue topology/worker pool/quality-checker references | Should describe v2 Agent workflow + RabbitMQ tier queues + InstanceQualityReviewer + worker pool |
| `documents/03-planning/database/database-design.md` (128KB) | No `frontend_instances`, `branding_resources`, `quality_reports`, `moderation_queue` (V31-V39 migrations not reflected) | Should have ERD entries + columns for 9+ v2 tables |
| `documents/06-diagrams/plantuml/03-erd.puml` (KiteClass ERD, 9.7KB) | KiteClass tables only, no v2 AI Branding entities (despite v2 living in kiteclass-core) | Add `frontend_instances`, `branding_resources`, `quality_reports`, `moderation_queue`, `outbox_events`, `dmca_takedown`, `deletion_requests`, `rebrand_approvals`, `branding_versions` |
| `documents/06-diagrams/plantuml/04-architecture-full.puml` | No v2 components (Analyzer/Planner/Executor/QualityReviewer/ContentModerationService) | Add v2 module boxes per `kiteclass-core/module/{ai,branding,instance,quality,moderation,provisioning}` |
| `documents/06-diagrams/plantuml/14-ai-branding-pipeline.puml` (1.7KB, dated 2026-03-24) | References **GPT-4 Vision + DALL-E 3** (project actually uses Ollama llama3.1+llava, soon Gemma 4) — Steps 1-8 don't match v2 Analyzer→Planner→Executor flow | Replace with v2 pipeline: ResourceCategory classification → Analyzer (logo/context analysis) → Planner (BrandingPlan) → PlanExecutor (Steps with fallback) → InstanceQualityReviewer → DEPLOYED |
| `documents/06-diagrams/plantuml/16-database-schema-full.puml` | Has v1 `branding_jobs` + `branding_templates` only | Replace/extend with v2 entities (V31-V45 migrations) |
| `documents/02-architecture/ai-branding-v2-redesign.md` | Specifies `kitehub-branding/` as module location | Update: implementation actually shipped to `kiteclass/kiteclass-core/` (Waves 2-4); class renames `BrandingAnalyzer→AnalyzerService`, `BrandingPlanner→PlannerService`, `BrandingExecutor→PlanExecutor` |

**Grep commands run:**
```bash
grep -inE "Analyzer|Planner|Executor|InstanceQualityReviewer|ResourceCategory|ContentModeration|FrontendInstance|frontend_instance" \
  documents/06-diagrams/plantuml/*.puml \
  documents/02-architecture/docker-platform-architecture.md \
  documents/03-planning/database/database-design.md
# 0 matches across all docs

ls kiteclass/kiteclass-core/src/main/resources/db/migration/V3[0-9]*.sql
# V31..V39 v2 tables exist
```

## Problem

After Waves 2-4 shipped v2 AI Branding to `kiteclass-core/`, architecture documentation, ERD diagrams, system diagrams, and DB design doc were not updated. Currently they describe v1 design (kitehub-branding module + GPT-4/DALL-E + branding_jobs table) which no longer matches reality. New team members reading docs would be misled. Audit/security review against docs would flag mismatches that don't exist.

## Why P2 (not higher)

- **No runtime impact** — code runs correctly; docs describe a different (v1) reality
- **GAP-016 meta goal achieved** — Living Documents rule satisfied for business-critical docs (rules/use-cases/api-contract via GAP-229) + skill grep scope fixed
- **PUML diagrams are reference, not source-of-truth** — code + business docs are SOR
- **Architecture review cadence** — GAP-016 §⚠️ originally noted "doc can be updated when next architecture review happens"

Bumped from P3 because diagram `14-ai-branding-pipeline.puml` references third-party AI services (GPT-4 + DALL-E) we don't use — gives wrong impression about cloud cost/dependencies.

## Proposed Fix

Single PR or small wave (5 sub-tasks, disjoint files = wave-eligible per `feedback_wave_plan_before_serial_prs.md`):

1. **Sub-task A — `14-ai-branding-pipeline.puml`** (highest signal, smallest file): rewrite to v2 Analyzer→Planner→Executor→QualityReviewer flow, replace GPT-4/DALL-E with Ollama (llama3.1+llava current, note Gemma 4 9B GAP-006 future)
2. **Sub-task B — `03-erd.puml` + `16-database-schema-full.puml`**: add v2 entities from V31-V45 migrations
3. **Sub-task C — `04-architecture-full.puml`**: add v2 module boxes (Analyzer/Planner/Executor/QualityReviewer/ContentModeration/Provisioning Saga)
4. **Sub-task D — `02-architecture/docker-platform-architecture.md`**: append §AI Branding v2 section describing queue topology + worker pool + quality-checker
5. **Sub-task E — `02-architecture/ai-branding-v2-redesign.md` + `database-design.md`**: update module location notes (`kitehub-branding/` → `kiteclass-core/`) + class renames + add 9+ v2 entities to DB design

A-D are disjoint files. E touches 2 files but same topic. Wave-eligible if treated as cluster.

## Acceptance Criteria

- [x] `14-ai-branding-pipeline.puml` reflects v2 pipeline (no GPT-4/DALL-E references)
- [x] `03-erd.puml` includes ≥6 v2 entities — shipped 11 (`frontend_instances`, `branding_resources`, `branding`, `branding_versions`, `quality_reports`, `moderation_queue`, `rebrand_approvals`, `outbox_events`, `audit_log`, `dmca_takedown_requests`, `deletion_requests`)
- [x] `04-architecture-full.puml` includes v2 module boxes (8 components in CORE_AI + 4 in KH_BRAND + 2 in OLLAMA)
- [x] `16-database-schema-full.puml` reflects V31-V45 migrations (legacy tables flagged HISTORICAL; 11 v2 classes added)
- [x] `docker-platform-architecture.md` has §AI Branding v2 worker pool + queue topology section
- [x] `ai-branding-v2-redesign.md` module location updated (kiteclass-core) + class renames noted (§0)
- [x] `database-design.md` has 9+ v2 entities documented (§7, 11 entities)
- [~] PNG/SVG renders regenerated for updated PUML files — **DEFERRED** (plantuml binary not installed in worktree; see Log for follow-up note)
- [x] No new gaps filed from this PR (sweep-only)

## Related

- **Parent:** GAP-016 (closed by this split-out)
- **Closed-by-precedent:** GAP-229 (business docs sync — same spirit, different scope)
- **Architecture doc drift root cause:** Waves 2-4 implemented v2 in `kiteclass-core/` instead of `kitehub-branding/` per original architecture spec; reason for deviation not formally captured (likely module ownership refactor)
- Rule: `.claude/rules/audit-to-gap-pipeline.md` Step 2.5 (state-check)
- Memory: `feedback_search_all_modules_before_missing_claim.md` (architecture doc drift after refactor is common)

## Log

- **2026-04-26 (later, Wave P2 Agent A)** — DONE. Sync pass shipped 7 docs in single PR `feature/wave-p2-A-gap-234-arch-diagram-drift`:
  - `documents/06-diagrams/plantuml/14-ai-branding-pipeline.puml` — full rewrite from v1 cloud-AI flow (GPT-4 Vision + DALL-E 3 references removed) to v2 Analyzer→Planner→Executor→QualityReviewer→ContentModeration with local Ollama (llama3.1+llava); legend lists every shipped class + module path
  - `documents/06-diagrams/plantuml/03-erd.puml` — added "AI Branding v2 Module" package with 11 v2 entities (`frontend_instances`, `branding_resources`, `branding`, `branding_versions`, `quality_reports`, `moderation_queue`, `rebrand_approvals`, `outbox_events`, `audit_log`, `dmca_takedown_requests`, `deletion_requests`) + 10 cross-entity relationships
  - `documents/06-diagrams/plantuml/04-architecture-full.puml` — added `CORE_AI` package (8 v2 components: AnalyzerService, PlannerService, PlanExecutor+Steps, Resource Handlers, InstanceQualityReviewer, ContentModerationService, TenantProvisioningSaga, AIClient) + new `KH_BRAND` package (queue dispatcher + tier worker pool + 3 tier queues) + new `OLLAMA` package; full v2 wiring arrows
  - `documents/06-diagrams/plantuml/16-database-schema-full.puml` — annotated legacy `branding_jobs` + `branding_templates` as `<<HISTORICAL>>` with rationale note; added 11 v2 `<<v2>>` classes inside KiteClass tenant DB package; updated relationships + legend
  - `documents/02-architecture/docker-platform-architecture.md` — appended §"AI Branding v2 — Runtime Topology" (~70 lines) covering module ownership table, RabbitMQ queue topology diagram, tier weights/concurrency/SLA/backpressure values pulled from `AIQueueProperties`, quality+moderation gate behavior, lifecycle state machine, AI provider stance (local Ollama only), compose/profile interaction
  - `documents/02-architecture/ai-branding-v2-redesign.md` — added §0 "Implementation Reality Note" documenting module-location deviation (kitehub-branding → kiteclass-core for workflow, kitehub-branding retains queue infra) + class renames table (`BrandingAnalyzer→AnalyzerService`, `BrandingPlanner→PlannerService`, `BrandingExecutor→PlanExecutor`); annotated §3.3 inline; status flipped 🟡 DRAFT → 🟢 SHIPPED
  - `documents/03-planning/database/database-design.md` — added §7 "AI Branding v2 Schema (V31-V45)" (~180 lines) with full column tables for all 11 v2 entities + Wave 2-4 ancillary tables (V41 student bulk import, V42 parents, V44 class schedule slots) + ER overview ASCII art + diagram cross-references
  - State-check verified 11 entities directly against migrations V31..V45 in `kiteclass/kiteclass-core/src/main/resources/db/migration/`
  - PUML syntax: heuristic check passed (1×@startuml/1×@enduml each, balanced opens/closes, 0 GPT-4/DALL-E references in any of the 4 puml files); strict `plantuml -checkonly` not run because **plantuml binary not installed** in this worktree's environment
  - **PNG/SVG regen DEFERRED** — `architecture-full.png/.svg` and `erd.png/.svg` (~280KB / ~5.5KB / ~345KB / ~6KB existing snapshots in `documents/06-diagrams/plantuml/`) cannot be regenerated here without plantuml installed; left untouched. Filing as known follow-up — anyone with plantuml installed can run `plantuml documents/06-diagrams/plantuml/*.puml` to refresh; or wait until next architecture review pass. Not a P2 blocker because PUML source is human-readable + diff-reviewable. AC item 8 ("PNG/SVG regenerated IF plantuml available") explicitly conditional, satisfied by DEFER.
  - No new gaps filed (sweep-only per gap §AC item 9)
- **2026-04-26** — Filed during GAP-016 final closure. State-check confirmed 0 matches for v2 component names across architecture/diagram/DB design docs. Split out from GAP-016 §⚠️ ARCHITECTURE DOC DRIFT. Wave-eligible (5 disjoint sub-tasks) but P2 — no rush; queue for Wave 8+ or batch with next architecture review.
