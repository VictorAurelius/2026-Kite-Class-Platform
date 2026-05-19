# GAP-672: Wave 99B B3 — Database Architecture Map (consolidated entity catalog + FK graph + RLS map + Flyway history)

**Status:** 🟢 DONE 2026-05-19 — Consolidated DB architecture map shipped (`documents/02-architecture/database-architecture-map.md`); 91 entities cataloged (32 kh-sub + 59 kc-core); 51 RLS-enabled (56% coverage); FK graph Mermaid erDiagram 25-entity sample; migration history 114 V-files indexed; tenant_id propagation map; sizing baseline Phase 1 BETA; Postgres-specific type inventory (15 JSONB) + Testcontainers IT coverage gap analysis
**Priority:** 🔴 P0
**Domain:** Architecture / Database
**Phase:** phase-1-beta
**Wave:** 99b
**Created:** 2026-05-19
**Closed:** 2026-05-19
**Sister gaps:** GAP-668 (B6 archive sweep — DONE), GAP-669 (B0 Last-Reviewed backfill — DONE), GAP-670 (B1 Service catalog), GAP-671 (B2 Compliance map), GAP-673 (B4 C4 diagram), GAP-674 (B5 README rewrite)

---

## Problem

Wave 99B Bucket B3 plan (`documents/03-planning/waves/wave-2026-05-19-99b-architecture-docs-sweep-expansion.md` §3 B3) mandate: ship NEW consolidated DB architecture report unblocking 3 personas:

- **Persona 1 (new backend dev debug 403):** "Table X có RLS chưa?" answer scattered across V58/V34/V50/V60 migrations + multi-tenant arch §3 narrative — answer time >5 min
- **Persona 3 (SRE on-call):** capacity planning + sizing baseline scattered across audit reports + ad-hoc gap notes — no single source
- **Persona 4 (tech lead review):** compliance review needs entity-level RLS map + FK graph + Postgres-specific type inventory — no consolidated doc

Per outside-in audit Persona Top 3 + External Benchmark Auto-Gen Backstage pattern (wave plan §1 Brainstorm Q1) — Database Map convergent recommendation cross all 3 audit agents.

## Root Cause

Pre-Wave-99B database knowledge fragmented:
- `multi-tenant-architecture.md` §3 covers RLS narrative + 5-layer defense (excellent) but NOT entity-level table list
- `kitehub-architecture.md` + `kiteclass-architecture.md` describe service responsibilities, NOT DB schema/sizing
- Flyway V*.sql files self-document but require reading 114 files to compose mental model
- GAP-466 (RLS impl Phase 1) closure log mentions V58/V34 but no consolidated table-by-table coverage map
- No sizing baseline anywhere (audit reports don't cover capacity dimension)

Result: backend dev / SRE / tech lead each repeat manual discovery work each session — clear case for consolidated reference doc.

## Proposed Fix

**Single PR ships:**

1. New file `documents/02-architecture/database-architecture-map.md` với 9 sections:
   - §1 Entity Catalog (91 tables, both services)
   - §2 FK Graph (Mermaid erDiagram, 25-entity sample)
   - §3 Migration History Index (114 V-files, breaking changes)
   - §4 Tenant_id Propagation Map (4 RLS clusters)
   - §5 DB Sizing Baseline (Phase 1 BETA top-10 row drivers)
   - §6 Postgres-Specific Type Inventory (15 JSONB + Testcontainers IT coverage)
   - §7 Auto-gen Follow-up Wave 100+ proposal
   - §8 Related Documents
   - §9 Log

2. File this GAP-672 with Status DONE + closure log
3. Add gap row to `gap-status.csv`
4. Path cross-reference với multi-tenant-architecture.md + ADR-001 (don't duplicate, extend)
5. Mermaid erDiagram per `diagram-format-selection.md` v1.0.0 §2 (ER type recommendation)

**Out of scope (avoid scope creep):**
- Full 91-entity FK graph rendering — sample 25 sufficient cho v1; auto-gen tracked Wave 100+ per §7
- Per-column data dictionary — too granular cho architecture map scope
- Migration patch-PR analysis (delta per wave) — separate audit category
- Performance/index audit — already covered `performance-audit/SKILL.md`
- Schema rewrite recommendations — out of B3 scope (B3 = describe current state)

## Acceptance Criteria

- [x] New file `documents/02-architecture/database-architecture-map.md` shipped với 9 sections per Proposed Fix
- [x] Entity catalog count = 91 (32 kh-sub + 59 kc-core verified via `grep CREATE TABLE` migrations)
- [x] RLS coverage map: 51 enabled (12 non-forced kh-sub + 39 forced kc-core); 56% baseline / 89% if exclude auto-excluded scope
- [x] FK graph Mermaid `erDiagram` block renders correctly on GitHub (≥25 entities + ≥20 relationships)
- [x] Migration history table per-service (kh-sub 54 / kc-core 60 / 6 stateless = 0)
- [x] Breaking changes flagged (5 V-files identified: V15/V22/V42/V46/V52 kh-sub)
- [x] Tenant_id propagation: 4 RLS clusters documented với SQL policy snippet
- [x] Sizing baseline top-10 row drivers per tenant + total 10-tenant estimate
- [x] Postgres-specific type inventory: 15 JSONB usages + 6 Testcontainers IT coverage gap analysis
- [x] Auto-gen follow-up proposal §7 (Wave 100+ — Backstage / Flyway parser pattern)
- [x] Cross-reference ADR-001 k12 data model + multi-tenant-architecture.md + GAP-466 RLS impl
- [x] `last-reviewed: 2026-05-19` frontmatter present
- [x] Vietnamese narrative per `dev-readable-doc-language.md` §2; technical tokens English (table/column names, SQL keywords, types)
- [x] Diagram per `diagram-format-selection.md` Mermaid erDiagram (no PlantUML / no plain ASCII >5 nodes)
- [x] PR auto-merge eligible per `docs-only-pr-auto-merge.md` v1.0.2 §2 (diff = 1 new doc + 1 new gap + 1 CSV row sync, all in `documents/**`)
- [x] No content/code edits outside B3 scope (parallel agents B1/B2/B4 OK — disjoint paths)

## Follow-up scope

| Item | Priority | Target wave |
|---|---|---|
| **Auto-gen DB architecture map from Flyway parser** (Backstage pattern) | 🟡 P2 | Wave 100+ — file GAP-XXX post-merge per §7 |
| **Full 91-entity FK graph render** (subgraph chunked per domain) | 🟡 P2 | Wave 100+ paired with auto-gen tool |
| **`students(instance_id, id)` composite index verify + add** if missing | 🟠 P1 | Wave 100+ performance pass |
| **`tenant_id` → `instance_id` column rename unification** | 🟡 P2 | Wave 100+ tech debt sweep |
| **Add Testcontainers IT cho `moderation_queue` / `submissions` / `students.parental_consent` / `class_schedule_slots.recurrence_rule`** | 🟠 P1 | Wave 100+ per `postgres-specific-type-testcontainers.md` mandate |
| **Verify class_schedules / class_sessions / course_prerequisites RLS coverage** (V58 list excluded — confirm intent) | 🟠 P1 | Wave 100+ RLS gap audit |

## Closure Verification

- File rendered correctly on GitHub: Mermaid erDiagram block displays as visual diagram (not text)
- Per Persona Risk test: "Table `students` có RLS chưa?" → §1.2 row 45 answers in <30 sec ✅
- Per Persona 4: "RLS coverage % overall?" → §1.3 answer 56% / 89% adjusted in single section ✅
- Per Persona 3: "Top sizing driver Phase 1 BETA?" → §5.1 answer `attendance` 100k-1M rows in single table ✅

## Log

- **2026-05-19 (closure):** GAP-672 closed. Database architecture map shipped 100% per AC. Per `wave-closure-scope-completeness.md` §3 reconciliation — Wave 99B Bucket B3 scope ✅ DONE. Per `gap-done-discipline.md` §2 closure protocol — AC checked, no banned phrases, audit artifact embedded in main doc §3 + §5 (no separate audit artifact needed for B3 — content IS the audit). Per `docs-only-pr-auto-merge.md` v1.0.2 §2 — auto-merge eligible. Per outside-in benchmark Backstage pattern §1 Brainstorm Q1 satisfied. Persona test 3/3 PASS. Reviewer: @nguyenvankiet (Wave 99B B3 agent worktree isolation).
