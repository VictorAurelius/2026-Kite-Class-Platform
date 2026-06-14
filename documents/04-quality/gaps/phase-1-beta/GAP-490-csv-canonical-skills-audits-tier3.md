# GAP-490: CSV-canonical pattern Tier 3 — skills + audits indexes

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (meta-improvement; not blocking Release 1)
**Domain:** Meta / Governance
**Found:** 2026-05-12 (Wave 65 Bucket C closure — GAP-485 PARTIAL exit ramp)
**Affects:** `.claude/skills/**`, `documents/04-quality/audits/**`
**Parent:** [GAP-485](GAP-485-csv-canonical-for-meta-enumerations.md) Tier 3 deferral

## Problem

GAP-485 Wave 65 Bucket C shipped Tier 1 (ADRs index) + Tier 2 (Rules index) per the proven CSV-canonical pattern from `gap-architecture-v2.md`, but deferred Tier 3 (Skills index + Audits index) because:

- **Skills:** ~50 SKILL.md files spread across deep folder hierarchy (`.claude/skills/<category>/<skill>/SKILL.md` + `.claude/skills/<category>/<skill>.md` simple form). Frontmatter heterogeneous (some skills predate `skill-conventions.md` v1.0.0). Estimated 3h to extract, validate, and ship 4 paired artifacts per `meta-csv-index-pattern.md` §3.
- **Audits:** `documents/04-quality/audits/<category>/YYYY-MM-DD-<topic>.md` files. Heterogeneous category folders (`ui/`, `security/`, `aws-verification/`, `cloudflare-verification/`, etc.). Schema requires score (variable scale per category: /100, /128, "n/a"), findings count, related gaps. ~1h to ship.

Wave 65 Bucket C budget (~2.5h Tier 1+2) consumed the available time; Tier 3 must ship in a follow-up wave to maintain quality + state-check rigor per `meta-csv-index-pattern.md` §3 mandate (all 4 paired artifacts per index).

## Proposed Fix

### Tier 3A: Skills index (~3h)

1. Extract metadata from every `.claude/skills/**/SKILL.md` + simple `.claude/skills/**/*.md` skill files
2. Schema per `meta-csv-index-pattern.md` §4 + GAP-485 §Schemas draft:
   ```csv
   # Schema: name,file,user_invocable,category,description_short,last_modified
   name,file,user_invocable,category,description_short,last_modified
   quality-audit,quality/quality-audit/SKILL.md,true,quality,Score /110 across 11 categories,2026-04-29
   ```
3. Ship 4 paired artifacts:
   - `.claude/skills/skills-index.csv`
   - `scripts/query-skills.sh`
   - `scripts/check-skills-index-csv.sh`
   - CI wire in `.github/workflows/script-quality.yml`
4. Update `meta-csv-index-pattern.md` §6 registry — flip Skills row from DEFERRED → DONE

### Tier 3B: Audits index (~1h)

1. Scan `documents/04-quality/audits/**/*.md` (sample first to confirm schema fit)
2. Schema:
   ```csv
   # Schema: date,category,topic,scope,score,findings_count,file,related_gaps
   ```
3. Same 4 paired artifacts pattern.

## Acceptance Criteria

- [ ] `.claude/skills/skills-index.csv` with 100% coverage of skill files (count matches `find .claude/skills -name '*.md' | wc -l` minus reference/data subfolders)
- [ ] `scripts/query-skills.sh` (pretty + `--count` + `--grep` flags)
- [ ] `scripts/check-skills-index-csv.sh` validates enums + coverage
- [ ] `documents/04-quality/audits/audits-index.csv` (heterogeneous score schema OK)
- [ ] `scripts/query-audits.sh` + `scripts/check-audits-index-csv.sh`
- [ ] CI `meta-csv-indexes` job extended (or 2 new jobs) covering both
- [ ] `meta-csv-index-pattern.md` §6 registry updated (Skills + Audits rows flipped to DONE)
- [ ] `output-review-mandate.md` §3 row updated to include skills + audits coverage
- [ ] Self-test: every query helper returns expected row on at least 2 filter combinations

## Out-of-scope

- Migrating other heterogeneous enumerations (memory entries, wave history — already JSONL)
- Bulk migrator scripts à la `migrate-gaps-to-csv.py` — only build if a 4th index emerges that needs scale

## Related

- **Parent gap:** [GAP-485](GAP-485-csv-canonical-for-meta-enumerations.md) (Tier 1+2 shipped 2026-05-12)
- **Canonical pattern rule:** `.claude/rules/meta-csv-index-pattern.md` v1.0.0
- **Proof-of-concept:** `gap-architecture-v2.md` v1.0.3 (gap-status.csv, 289 rows)
- **Shipped Tier 1+2 indexes:** `documents/02-architecture/adr/adrs-index.csv` (28 rows) + `.claude/rules/rules-index.csv` (36 rows)

## Effort estimate

- Tier 3A skills: ~3h (extract + heterogeneous frontmatter handling + 4 artifacts)
- Tier 3B audits: ~1h (smaller set + simpler schema)
- Doc/registry/cross-link updates: ~30min
- **Total: ~4.5h — Wave 66+ candidate**

## Log


- 2026-06-14: phase re-triage — n/a→phase-1-beta (CSV-canonical Tier 3 skills+audits; meta tooling).
- **2026-05-12:** Filed as PARTIAL exit ramp for GAP-485 Tier 3 deferral per `gap-done-discipline.md` §3. Tier 1+2 shipped same wave; Tier 3 deferred to maintain quality + state-check rigor for ~50-skill + heterogeneous-audit indexes.
