# GAP-485: Extend CSV-canonical pattern to meta enumerations (rules, skills, audits, ADRs)

**Status:** 🟡 PARTIAL (Tier 1+2 DONE 2026-05-12 Wave 65 Bucket C; Tier 3 → GAP-490)
**Priority:** 🟡 P2 (meta-improvement; not blocking Release 1)
**Domain:** Meta / Governance
**Found:** 2026-05-12 (user-flagged during Wave 64 session close)
**Affects:** `.claude/rules/`, `.claude/skills/`, `documents/04-quality/audits/`, `documents/02-architecture/adr/`

## Problem

`gap-architecture-v2.md` v1.0.0 proved CSV-canonical pattern delivers ~50× token savings for status queries vs reading markdown files. Same pattern could apply to other meta enumerations currently scattered across many markdown files without central index.

Affected domains:

| Domain | Current state | Query cost | CSV opportunity |
|--------|---------------|-----------|-----------------|
| Rules | 30+ `.claude/rules/*.md` files; no index | Read each file for priority/version/applies-to | `rules-index.csv` |
| Skills | 50+ `.claude/skills/**/SKILL.md` files; `_README-skills-index.md` markdown index (stale-prone) | Read each SKILL.md frontmatter | `skills-index.csv` |
| Audits | Scattered `documents/04-quality/audits/**/*.md`; no central | Glob + read frontmatter | `audits-index.csv` |
| ADRs | `documents/02-architecture/adr/*.md`; convention numbered but no status index | Read each ADR | `adrs-index.csv` |

## Proposed Fix

### Design: Per-domain CSV (option A)

Each domain gets its own CSV with domain-specific schema. Pattern matches `gap-status.csv`:

```
.claude/rules/rules-index.csv
.claude/skills/skills-index.csv
documents/04-quality/audits/audits-index.csv
documents/02-architecture/adr/adrs-index.csv
```

### Schemas (draft)

**rules-index.csv:**
```csv
name,file,priority,version,last_reviewed,applies_to_glob,status
pre-mutation-state-check,.claude/rules/pre-mutation-state-check.md,CRITICAL,1.1.0,2026-05-12,infrastructure/terraform-aws/**,ACTIVE
rule-change-process,.claude/rules/rule-change-process.md,CRITICAL,1.1.0,2026-04-27,.claude/rules/**,ACTIVE
...
```

**skills-index.csv:**
```csv
name,file,user_invocable,category,description_short,last_modified
quality-audit,.claude/skills/quality-audit/SKILL.md,true,quality,Score /110 across 11 categories,2026-04-29
simulation-gap-finder,.claude/skills/quality/simulation-gap-finder.md,true,quality,3-axis matrix simulation,2026-04-14
...
```

**audits-index.csv:**
```csv
date,category,topic,scope,score,findings_count,file,related_gaps
2026-05-12,aws-verification,wave-64-pre-apply-plan-investigation,terraform plan 11/14/4,n/a,3 cascading bugs,documents/04-quality/audits/aws-verification/2026-05-12-wave-64-pre-apply-plan-investigation.md,GAP-482;GAP-483;GAP-484
2026-05-11,ui,wave-53-phase-4-kit-ports-milestone,144 screens 7 kits,111.7/128,0 new gaps,documents/04-quality/audits/ui/2026-05-11-wave-53-phase-4-kit-ports-milestone.md,GAP-429
...
```

**adrs-index.csv:**
```csv
number,title,status,decision_summary,date,file
015,AWS Agent Plugins Evaluation,ACCEPTED,Defer to Q3 2026,2026-04-15,documents/02-architecture/adr/ADR-015-aws-agent-plugins-evaluation.md
021,Per-module Outbox Pattern,ACCEPTED,Domain outbox per module instead of shared lib,2026-04-26,documents/02-architecture/adr/ADR-021-per-module-outbox.md
025,AWS Singapore Free Tier as Phase 1 BETA,ACCEPTED,Use AWS over Oracle Cloud,2026-05-07,documents/02-architecture/adr/ADR-025-aws-singapore-free-tier.md
...
```

## Acceptance Criteria

- [ ] New rule `.claude/rules/meta-csv-index-pattern.md` v1.0.0 — codifies when to use CSV vs Markdown for meta enumerations + schema conventions
- [ ] 4 CSVs created with full population (rules + skills + audits + ADRs)
- [ ] Query helpers `scripts/query-rules.sh`, `scripts/query-skills.sh`, `scripts/query-audits.sh`, `scripts/query-adrs.sh` (pattern from `query-gaps.sh`)
- [ ] CI validators `scripts/check-rules-index-csv.sh` (etc.) — verify every CSV row has corresponding file
- [ ] PR template + reviewer checklist: when editing rules/skills/audits/ADRs, update both file + CSV
- [ ] Memory cross-link `feedback_csv_canonical_meta_enumerations.md`
- [ ] Worked self-test: query each CSV for ≥1 use case (e.g. "list all P0 rules", "find audits with score < 70")

## Out-of-scope

- Migrating existing markdown indexes (e.g. `_README-skills-index.md`) — keep both during transition, mark markdown as "human-readable summary; CSV canonical"
- Personas catalog — already structured in `personas-catalog.md`, low value to CSV-ify
- Memory entries — outside repo, different lifecycle
- Wave history — already `.jsonl`, similar machine-readable

## Related

- **Parent pattern:** `.claude/rules/gap-architecture-v2.md` v1.0.0 (originating CSV-canonical concept)
- **Surfaced by:** Wave 64 session close 2026-05-12 — user asked if generalize pattern
- **Reference rules:** `pre-mutation-state-check.md`, `rule-change-process.md`, `output-review-mandate.md`
- **Reference skills:** `scripts/query-gaps.sh` (template for query helpers)

## Effort estimate

- Rules CSV + helper + validator: ~2h (30 rules to enumerate)
- Skills CSV: ~3h (50 skills + scan SKILL.md frontmatter)
- Audits CSV: ~1h (auto-generate from filenames + frontmatter)
- ADRs CSV: ~30min (small set)
- Meta rule + memory: ~1h
- **Total: ~1 wave (~7h) — Wave 65+ candidate**

## Log

- **2026-05-12 Wave 65 Bucket C — Tier 1+2 SHIPPED (PARTIAL).** Files added:
  - `.claude/rules/meta-csv-index-pattern.md` v1.0.0 (codifies CSV-canonical pattern + trigger conditions + schema + 4 paired artifacts mandate)
  - `documents/02-architecture/adr/adrs-index.csv` (28 rows, 100% ADR coverage)
  - `.claude/rules/rules-index.csv` (36 rows, 100% rule coverage)
  - `scripts/query-adrs.sh` + `scripts/query-rules.sh` (filter + grep + count)
  - `scripts/check-adrs-index-csv.sh` + `scripts/check-rules-index-csv.sh` (enum + format + coverage validators)
  - `.github/workflows/script-quality.yml` job `meta-csv-indexes` (CI wire)
  - `.github/PULL_REQUEST_TEMPLATE.md` Output Review row (PR template enforcement)
  - `.claude/rules/output-review-mandate.md` §3 matrix row "Meta CSV indexes"
  - Self-test PASS: 28 ADR + 36 rule rows validated; 4 query commands verified.
  - **Tier 3 deferred to GAP-490:** Skills index (~50 SKILL.md) + Audits index (heterogeneous categories). PARTIAL exit ramp per `gap-done-discipline.md` §3.
- **2026-05-12:** Filed at user request during Wave 64 session close. Pattern proven by gap-architecture-v2.md; design proposal phase.
