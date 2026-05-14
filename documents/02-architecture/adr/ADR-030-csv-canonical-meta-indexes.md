# ADR-030: CSV canonical for meta indexes

**Status:** ACCEPTED
**Date:** 2026-05-14
**Reviewer:** @nguyenvankiet (solo-dev)
**Supersedes:** none
**Superseded-by:** none

> **Note on numbering:** Wave 76 Bucket E plan originally referenced "ADR-029"; that number was already taken (`ADR-029-jvm-container-memory-budget.md`, 2026-05-13). This ADR uses next available number ADR-030. Content and intent unchanged from plan spec.

## Context

KiteHub meta-governance requires canonical store for gap status / rule status / ADR status / audit status. Each artifact has markdown file as narrative source of truth, but status/priority/version/date fields drift across multiple sources (frontmatter + ROADMAP + plan docs).

Industry standard (MADR ADR-0013 Architecture decision tracking): YAML frontmatter + Pandoc-compatible markdown.

KiteHub initial pilot 2026-05-11 (`gap-architecture-v2.md`) chose CSV. This ADR documents rationale (contrarian decision vs MADR) for future-team.

## Decision

Use **CSV format** for meta enumeration indexes:
- `documents/04-quality/gaps/gap-status.csv` (350 rows)
- `.claude/rules/rules-index.csv` (56 rows + 3 lifecycle columns Wave 76 Bucket A)
- `documents/02-architecture/adr/adrs-index.csv` (29 rows — soon 30 with this ADR)
- `documents/04-quality/audits/audits-index.csv` (Wave 76 Bucket A — ≥30 rows)

Each CSV row mirrors metadata of 1 artifact. Markdown file remains narrative source of truth; CSV is the canonical structured store.

## Consequences

### Positive
- **Awk-queryable**: `bash scripts/query-gaps.sh P0` < 50 tokens; reading 50 gap files = 25k tokens
- **Git-diffable**: line-oriented; conflicts merge cleanly
- **Low overhead**: no parser dependency (Python `csv` module sufficient)
- **Validation simple**: per-column enum check via shell scripts
- **Pattern proven**: Wave 75 closure achieved 50× token cost savings for status queries

### Negative
- **Schema validation limited**: no native typed schema (vs YAML schema validators)
- **No nested structures**: CSV is flat; deep metadata pushed to markdown body
- **Mitigations**:
  - `scripts/check-*-index-csv.sh` validators enforce per-column rules
  - Markdown body remains narrative SOT (CSV is cache)
  - `meta-csv-index-pattern.md` documents universal schema convention

## Alternatives considered

### YAML frontmatter (MADR standard)
- ✅ Industry standard
- ❌ Drift-prone (1 file per metadata source)
- ❌ Heavier toolchain (PyYAML, schema validators)
- **Verdict:** rejected — flat CSV simpler for 99% of use cases

### JSON
- ✅ Machine-readable + nested
- ❌ Not git-diff friendly (bracket noise)
- ❌ No human edit affordance
- **Verdict:** rejected

### SQLite
- ✅ Strong schema + queries
- ❌ Binary file (git-blob noise)
- ❌ Tool overhead disproportionate to scale
- **Verdict:** overkill

### Per-item frontmatter only (no canonical index)
- ✅ Single source per file
- ❌ Drift: 350 gap files manually scanned = expensive query
- ❌ No cross-cutting view (status × priority × phase)
- **Verdict:** rejected — bypassed Wave 75 token savings

## Related rules / artifacts

- `.claude/rules/gap-architecture-v2.md` — CSV pattern for gaps (Phase 1 pilot)
- `.claude/rules/meta-csv-index-pattern.md` — generalized CSV-canonical pattern (Wave 73 follow-up)
- 4 canonical CSV indexes shipped Wave 73-76
- Wave 75 outside-in benchmark documents contrarian choice vs MADR ADR-0013

## Log
- 2026-05-14 — ADR created Wave 76 Bucket E per outside-in benchmark ARCH-2 recommendation (document contrarian CSV choice for future-team). Number adjusted from plan-spec "ADR-029" to "ADR-030" due to pre-existing ADR-029 (JVM memory budget, 2026-05-13).
