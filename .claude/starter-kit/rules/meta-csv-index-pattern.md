# Meta CSV Index Pattern — canonical CSV for enumerations

**Priority:** 🟠 MANDATORY — codifies CSV-canonical pattern for meta enumerations
**Version:** 1.0.0
**Created:** 2026-05-12
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Every repo-wide enumeration of meta artifacts (rules, skills, ADRs, audits, runbooks, gaps) where: (a) the set is queried programmatically more than ~5×/week, (b) each item has a stable identifier + small fixed metadata schema, (c) reading the source files costs ≥10× the CSV row read

---

## 1. The Rule

> **For meta enumerations (rules / ADRs / audits / skills / gaps / similar) that have a stable identifier + small fixed metadata schema, a single canonical CSV (`<scope>-index.csv` or `<scope>-status.csv`) is the source of truth for status / priority / version / date fields. Per-item markdown files remain canonical for narrative content (Problem, Rationale, Log).**

Markdown frontmatter drifts (multiple sessions touch it without coordination, scrubbers strip fields, emoji formats vary). A flat CSV is the lowest-overhead structured source: machines parse with `awk`, humans read with `cat`, validators check with `bash`. The token cost to query "all CRITICAL rules" drops ~50× vs reading every rule file (proven by `gap-architecture-v2.md` §5 on gap-status.csv).

This rule generalizes the architecture proven by `gap-architecture-v2.md` v1.0.x for gaps. The same pattern applies to other meta enumerations once the §2 trigger conditions hold.

---

## 2. When to apply (trigger conditions)

Apply CSV-canonical pattern when ALL three conditions hold:

1. **Set has ≥10 items** with stable identifier (rule name, ADR number, gap ID, audit date+topic)
2. **Programmatic queries common** — agents/scripts answer questions like "list all CRITICAL rules with last_reviewed older than 30 days" or "show ADRs ACCEPTED in last month"
3. **Reading source files is expensive** — each item is ≥500 tokens; reading 30 items ≈ 15k tokens vs 30 CSV rows ≈ 600 tokens

Counter-indications (do NOT apply):

| Case | Why exempt |
|------|-----------|
| Set < 10 items | Index overhead exceeds query savings |
| Each item's metadata schema highly variable | CSV columns force commonality that doesn't exist |
| Source files already structured (JSONL, YAML frontmatter strictly validated) | Adding CSV duplicates the source |
| Items are queried < once/week | Drift cost > query cost |
| Personas catalog (already structured + read often as narrative) | Per GAP-485 out-of-scope rationale |
| Memory entries | Outside repo, different lifecycle |
| Wave history | Already JSONL (similar machine-readable) |

---

## 3. Required artifacts per index

A CSV-canonical index ships with FOUR artifacts in the same PR (per `rule-change-process.md` §6.5 Enforcement Parity):

| Artifact | Path pattern | Purpose |
|----------|--------------|---------|
| **CSV** | `<canonical-folder>/<scope>-index.csv` OR `<scope>-status.csv` | Source of truth |
| **Query helper** | `scripts/query-<scope>.sh` | `awk`-based filter; pretty + `--count` + `--grep` |
| **CI validator** | `scripts/check-<scope>-index-csv.sh` | Enum + format + file-exists + coverage |
| **CI wire** | Step in `.github/workflows/script-quality.yml` | Block PR on CSV malformed / coverage gap |

Plus cross-link entries:

- `output-review-mandate.md` §3 row (review standard documented)
- This rule's §6 registry (record the new index)

---

## 4. Schema conventions

| Field | Convention |
|-------|-----------|
| **Primary key column 1** | Stable identifier matching filename prefix (`GAP-NNN`, `ADR-NNN`, rule slug) |
| **Enum columns** | UPPERCASE values; comma-separated allowed list documented in CSV header comment |
| **Date columns** | ISO-8601 `YYYY-MM-DD` (UTC); never relative |
| **`file` column** | Last column; relative path from CSV folder (`GAP-NNN-foo.md` or `closed/GAP-XXX.md`) |
| **Header line** | First non-comment row; column names lowercase snake_case |
| **Comments** | Start with `#`; document schema + enum allowed values + pattern source |
| **Empty cells** | Use `n/a` (lowercase) or leave empty; validators must tolerate both consistently |

Example header:

```csv
# Rules Index — canonical CSV per `.claude/rules/meta-csv-index-pattern.md`
# Schema: name,priority,version,created,last_reviewed,file
# Priority enums: CRITICAL | MANDATORY | ADVISORY
name,priority,version,created,last_reviewed,file
admin-merge-discipline,CRITICAL,1.0.0,2026-05-07,2026-05-07,admin-merge-discipline.md
```

---

## 5. Authority delineation

For fields covered by the CSV, **CSV beats markdown frontmatter**. The CSV is what skills, scripts, and reviewers read. Markdown frontmatter remains for human readability + manual editing — but its values are advisory cache, not canonical.

| Field class | Canonical |
|-------------|-----------|
| Status / priority / version / dates / completion% | **CSV** |
| Title + Problem + Rationale + Log + narrative sections | **Markdown** |

When the two drift, the CSV wins. Bulk migrators (`scripts/migrate-<scope>-to-csv.py` style) can be used to re-sync from markdown into CSV when frontmatter has been the source of truth historically (see `gap-architecture-v2.md` §4 Phase 2 for the gap example).

---

## 6. Index registry

Current canonical CSV indexes in this repo:

| Scope | CSV | Query helper | Validator | Status | Rule |
|-------|-----|--------------|-----------|--------|------|
| **Gaps** | `documents/04-quality/gaps/gap-status.csv` | `scripts/query-gaps.sh` | `scripts/check-gap-status-csv.sh` | 100% coverage (289 rows) | `gap-architecture-v2.md` |
| **ADRs** | `documents/02-architecture/adr/adrs-index.csv` | `scripts/query-adrs.sh` | `scripts/check-adrs-index-csv.sh` | 100% coverage (28 rows) | this rule (GAP-485 Tier 1) |
| **Rules** | `.claude/rules/rules-index.csv` | `scripts/query-rules.sh` | `scripts/check-rules-index-csv.sh` | 100% coverage (35 rows) | this rule (GAP-485 Tier 2) |
| **Skills** | DEFERRED to follow-up gap | DEFERRED | DEFERRED | not yet shipped (~50 SKILL.md files) | future scope |
| **Audits** | DEFERRED to follow-up gap | DEFERRED | DEFERRED | not yet shipped | future scope |

Adding a new index to this registry requires same-PR landing of all 4 artifacts per §3 + entry in this table.

---

## 7. Worked examples

### 7.1 ADR query — "list ACCEPTED ADRs from May 2026"

```bash
bash scripts/query-adrs.sh ACCEPTED 2026-05
# Expected output (4 rows): ADR-025/026/027/028
```

Token cost: ~30 tokens (4 CSV rows + filter). Alternative: read 4 ADR files = ~30k+ tokens.

### 7.2 Rules query — "count CRITICAL rules"

```bash
bash scripts/query-rules.sh --count CRITICAL
# Expected: 11
```

### 7.3 Rules grep — "find AWS-related rules"

```bash
bash scripts/query-rules.sh --grep aws
# Expected (3 rows): agent-aws-access / aws-observability-first / aws-sg-description-ascii
```

### 7.4 Coverage check — adding a new rule

When adding `new-rule.md`, the same PR MUST add a CSV row to `rules-index.csv`. CI fails otherwise:

```
FAIL: new-rule.md missing CSV row (100%-coverage mode)
```

This forces the index to stay in sync — drift impossible.

---

## 8. Enforcement

### 8.1 CI gate (active for shipped indexes)

`.github/workflows/script-quality.yml` runs `bash scripts/check-<scope>-index-csv.sh` for each shipped index. Failure → PR blocked.

### 8.2 PR template checkbox (lands same PR)

`.github/PULL_REQUEST_TEMPLATE.md` Output Review section:

> - [ ] **Meta CSV index sync** — if PR adds/renames/deletes a rule (`.claude/rules/*.md`), ADR (`documents/02-architecture/adr/ADR-*.md`), or other tracked enumeration, the matching `*-index.csv` row was added/updated/removed in the same commit per `.claude/rules/meta-csv-index-pattern.md`. CI `check-<scope>-index-csv.sh` validates.

### 8.3 Reviewer-checklist

When reviewing a PR that touches enumerated meta artifacts:

- Does each new/renamed/deleted item have a CSV row update?
- Is the row schema-conformant (enum values, ISO dates)?
- Did CI `check-*-index-csv.sh` pass?

### 8.4 Override mechanism

For genuine cases where CSV update can't ship in the same PR (e.g., emergency hotfix renaming a rule):

```
git commit -m "...
META_CSV_INDEX_DEFER: <scope> <reason — follow-up PR link>"
```

Trailer logged. Pattern frequency >5% per quarter triggers meta-review.

---

## 9. Anti-patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Add new rule/ADR/audit without updating CSV in same PR | CI fails on coverage gap; treat as a hard merge gate |
| Read source files to answer "how many X?" | Use `query-<scope>.sh --count` (50× cheaper) |
| Duplicate canonical fields across CSV + JSON + YAML | Pick one canonical (CSV here); others are caches |
| Apply pattern to sets < 10 items | Overhead exceeds savings; keep them in markdown only |
| Apply pattern when item metadata schema is too variable | CSV columns force false commonality |
| Trust markdown frontmatter Status over CSV | CSV is canonical per §5 |
| Forget cross-link updates (`output-review-mandate.md`, this §6 registry) | Same-PR landing per §6.5 Enforcement Parity Mandate |

---

## 10. Self-test (worked example — this PR)

This PR ships the rule + 2 new indexes (ADRs + Rules) as concrete worked examples:

| Validation | Command | Expected | Actual |
|-----------|---------|----------|--------|
| ADRs CSV valid | `bash scripts/check-adrs-index-csv.sh` | PASS 28 rows | ✅ PASS |
| Rules CSV valid | `bash scripts/check-rules-index-csv.sh` | PASS 35 rows | ✅ PASS |
| ADR query — ACCEPTED count | `bash scripts/query-adrs.sh --count ACCEPTED` | 26 | ✅ 26 |
| Rules query — CRITICAL count | `bash scripts/query-rules.sh --count CRITICAL` | 11 | ✅ 11 |
| Coverage parity — ADRs | every `ADR-*.md` has CSV row | 28/28 | ✅ |
| Coverage parity — Rules | every `.claude/rules/*.md` has CSV row | 35/35 | ✅ |

Rule fires correctly on both originating worked examples. Self-test PASS. ✅

---

## 11. Relationship to other rules

- **`gap-architecture-v2.md`** — sister rule (specialized for gaps); originated the CSV-canonical pattern. This rule generalizes that pattern.
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — this rule + 2 CSV indexes + 2 query scripts + 2 validators + CI wire + PR template row all ship same PR
- **`output-review-mandate.md`** §3 — adds new row "Meta CSV index" tracking review standard for each index
- **`incident-to-rule-pipeline.md`** — this rule is direct output of GAP-485 (generalize CSV pattern beyond gaps) applied through 5-stage pipeline
- **`audit-to-gap-pipeline.md`** §2.8 step 0 — recommends `query-gaps.sh` as canonical-status lookup; this rule extends to `query-rules.sh`, `query-adrs.sh`, and future `query-<scope>.sh`
- **`docs-only-pr-auto-merge.md`** §2 — CSV files at `documents/**/*.csv` or `.claude/rules/*.csv` qualify as docs-only scope when changed alone

---

## 12. Open items / future scope

Tier 3 indexes deferred to follow-up gap GAP-490 per task spec:

- [ ] **Skills index** (`.claude/skills/skills-index.csv` + `scripts/query-skills.sh` + `scripts/check-skills-index-csv.sh`) — ~50 SKILL.md files; ~3h work
- [ ] **Audits index** (`documents/04-quality/audits/audits-index.csv` + helpers) — heterogeneous category folders; ~1h work

When 2nd recurrence (i.e., a 4th index becomes worthwhile), the bulk-migrator pattern from `gap-architecture-v2.md` Phase 2 (`scripts/migrate-<scope>-to-csv.py`) should be considered to scale to larger sets.

---

## 13. Log

- **2026-05-12 (v1.0.0):** Rule created. Closes GAP-485 Tier 1 + Tier 2 (rule + ADRs CSV + Rules CSV + helpers + validators + CI wire + PR template). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (GAP-485 filed 2026-05-12 user-flagged generalization opportunity from proven gap pattern) → Classify ✓ (no existing rule generalizes CSV-canonical pattern; `gap-architecture-v2.md` covers gaps only) → Rule+Enforce ✓ (this file + 2 CSV indexes + 2 query helpers + 2 validators + CI wire + PR template row + `output-review-mandate.md` §3 cross-link per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§10 worked example — 2 validators PASS + 4 query commands verified) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new rule with built-in enforcement, no constraint loosening for prior work; existing rule/ADR markdown frontmatter grandfathered; rule applies prospectively to new index additions). Skills + Audits indexes (Tier 3) deferred to GAP-490 per §12.
