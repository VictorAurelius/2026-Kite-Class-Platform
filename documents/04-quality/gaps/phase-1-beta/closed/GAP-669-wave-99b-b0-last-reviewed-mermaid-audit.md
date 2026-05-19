# GAP-669: Wave 99B B0 — Last-Reviewed backfill + Mermaid migration audit cho non-ADR arch files

**Status:** 🟢 DONE 2026-05-19 — 10/10 root-level `documents/02-architecture/*.md` files có `last-reviewed: 2026-05-19` frontmatter; Mermaid migration audit complete (16/16 code-fenced blocks Mermaid, zero PlantUML); ASCII heuristic flagged 1 file (`domain-management.md` 39 lines) for future migration follow-up
**Priority:** 🟠 P1 META
**Domain:** Meta / Documentation hygiene
**Phase:** phase-1-beta
**Wave:** 99b
**Created:** 2026-05-19
**Closed:** 2026-05-19
**Sister gaps:** GAP-668 (B6 foundation — archive sweep), GAP-670..674 (B1-B5 sister buckets Wave 99B)

---

## Problem

Wave 99B Bucket B0 plan (`documents/03-planning/waves/wave-2026-05-19-99b-architecture-docs-sweep-expansion.md` §3 B0) mandate:

1. **Last-Reviewed frontmatter backfill** — chỉ 3/10 non-ADR arch files (Wave 96 trio: `kitehub-architecture.md` / `kiteclass-architecture.md` / `multi-tenant-architecture.md`) có frontmatter date field. Per Persona Risk D + Benchmark anti-pattern (Wave 99B outside-in audit), files thiếu freshness signal → reader không biết doc còn relevant không → trust erosion. Per `rule-change-process.md` §3.5 staleness policy pattern (adapted), arch docs cần `last-reviewed:` để CI/reviewer flag staleness ≥60d WARN / ≥180d FAIL trong tương lai (CI script future scope).

2. **Mermaid migration audit** — Wave 96 PR2 shipped 3 arch reports với Mermaid Native (GitHub render); Wave 99C `diagram-format-selection.md` v1.0.0 codifies Mermaid default. Audit cần verify (a) zero PlantUML/legacy ASCII >5 nodes vi phạm rule §2; (b) PR #1562 stateDiagram render fix vẫn clean post-B6 archive.

## Root Cause

Pre-Wave-99B: arch folder evolved organically (oldest file 2026-03-23 = `domain-management.md`; newest 2026-05-19 = `multi-tenant-architecture.md` fix). Frontmatter convention varied per author/era:
- Old era (2026-03 → 2026-04): body markdown markers `**Last updated:**`, `**Ngày tạo:**` (no frontmatter)
- Wave 96 era (2026-05-18+): proper frontmatter with `audience:` + `last-updated:`/`last-reviewed:` mixed

No project-wide rule mandated `last-reviewed:` cho arch scope until Wave 99B plan §3 B0. Result: inconsistent freshness signal across folder.

## Proposed Fix

**Single PR ships:**

1. Backfill `last-reviewed: 2026-05-19` frontmatter to 10/10 root-level non-ADR arch files (preserve existing body markers — not a content rewrite)
2. Audit Mermaid compliance (code-fenced blocks) + ASCII heuristic >30-line detector per `diagram-format-selection.md` §5.3
3. Audit artifact `documents/04-quality/audits/meta/2026-05-19-wave-99b-b0-sweep.md` documenting findings + counts + follow-up candidates
4. File this GAP-669 with Status DONE + closure log
5. Add gap row to `gap-status.csv` + audit row to `audits-index.csv`

**Out of scope (avoid scope creep):**
- ASCII → Mermaid migration of flagged files (`domain-management.md` 39 lines + `deployment-strategy.md` 20 lines borderline) — track as P2 follow-up for Wave 100+ polish wave
- Content rewrites / outdated content review (only frontmatter touched this bucket)
- ADR files + threat-models + sub-folders out of B0 scope per plan §3

## Acceptance Criteria

- [x] 10/10 root-level `documents/02-architecture/*.md` files có `last-reviewed:` frontmatter field
- [x] Mermaid migration audit complete (every code-fenced diagram block classified Mermaid/PlantUML/other)
- [x] PR #1562 stateDiagram render fix verified still clean (multi-tenant §2 line 76)
- [x] ASCII heuristic scan completed per `diagram-format-selection.md` §5.3 detector pattern (>30 lines threshold)
- [x] Audit artifact `documents/04-quality/audits/meta/2026-05-19-wave-99b-b0-sweep.md` filed với scope + commands + findings + verdict + prior actions + recommendations + references per `pre-mutation-state-check.md` §3 format (adapted for docs-only)
- [x] `audits-index.csv` row added (AUDIT-2026-05-19-wave-99b-b0-sweep)
- [x] `gap-status.csv` row added (GAP-669) với status:DONE + phase:phase-1-beta + wave:99b + completion_pct:100
- [x] PR auto-merge eligible per `docs-only-pr-auto-merge.md` v1.0.2 §2 (diff = `documents/02-architecture/*.md` frontmatter + new gap + new audit + 2 CSV row syncs, all in `documents/**`)
- [x] No content/code edits outside frontmatter (B1+B3 + Track B agents may run parallel; avoid path collision)

## Follow-up scope

| Item | Priority | Target wave |
|---|---|---|
| `domain-management.md` ASCII → Mermaid migration (39 box-drawing lines, exceeds `diagram-format-selection.md` §5.3 >30 threshold) | P2 | Wave 100+ polish wave |
| `deployment-strategy.md` borderline ASCII architecture box at line 109+ — consider Mermaid for cleaner GitHub render | P2 | Wave 100+ polish wave (batch with `domain-management.md`) |
| CI script `scripts/check-rule-staleness.sh` extension to scan `documents/02-architecture/**` for stale `last-reviewed:` per quarter | P2 | Wave 100+ ops hygiene |

## Log

- **2026-05-19 (closure):** GAP-669 filed + flipped Status:DONE in single PR. Per `gap-done-discipline.md` §2 — AC fully verified: 10/10 frontmatter backfilled (`grep -c "^last-reviewed:" documents/02-architecture/*.md` all = 1); 16/16 Mermaid blocks confirmed (`grep -rn "flowchart\|sequenceDiagram\|stateDiagram\|@startuml" documents/02-architecture/`); PR #1562 stateDiagram render fix preserved; ASCII heuristic 1 hit flagged for P2 follow-up. No deferral inside DONE flip. Audit artifact `documents/04-quality/audits/meta/2026-05-19-wave-99b-b0-sweep.md` shipped same PR. Per `audit-to-gap-pipeline.md` §3 — findings documented, follow-up gap recommended but explicitly out-of-scope this bucket. Per `gap-folder-organization.md` v2.0.0 §3.3 — DONE gap placed at `phase-1-beta/closed/`. Per `docs-only-pr-auto-merge.md` v1.0.2 §2 — eligible auto-merge (docs-only scope).
