# Project Rules — `.claude/rules/`

Index + governance hướng dẫn cho mọi rule file trong folder này. Rules là **project DNA** — mỗi rule edit force-multiplies mọi PR tương lai trong scope của rule đó (per [`meta-gap-priority.md`](meta-gap-priority.md)).

---

## Purpose

Folder này chứa toàn bộ project rules — mỗi `*.md` file = 1 rule với enforcement parity (rule + hook/CI/template/skill ship cùng PR per [`rule-change-process.md`](rule-change-process.md) §6.5).

**Source of truth cho status / priority / version metadata:** [`rules-index.csv`](rules-index.csv) (per [`meta-csv-index-pattern.md`](meta-csv-index-pattern.md)). Markdown frontmatter là cache informational, có thể drift; CSV là canonical.

Query helper: `bash scripts/query-rules.sh <priority> [date-prefix]` (vd `bash scripts/query-rules.sh CRITICAL` → list all CRITICAL rules).

---

## Tier convention (Wave 73 outcome target — ~54 rules / ~250k → ~100k base context)

Để giảm base context auto-load mỗi session, rules chia thành 3 tier theo cách load:

| Tier | Cách load | Số rules (Wave 73 target) | Khi nào dùng |
|---|---|---|---|
| **CRITICAL auto-load** | Đọc mỗi session start (mặc định Anthropic) | ~14 rules | Governance + meta + critical-path rules apply mọi turn (vd `meta-gap-priority`, `rule-change-process`, `incident-to-rule-pipeline`, `pre-handoff-self-test-completeness`) |
| **MANDATORY path-scoped** | Auto-load qua native `paths:` frontmatter khi Claude đọc file matching glob | ~30 rules | Domain-specific rules trigger khi user touch file trong scope (vd `aws-sg-description-ascii` chỉ load khi đọc `*.tf`; `audit-skill-rubric-*` chỉ load khi đọc audit reports) |
| **Hook-covered** | KHÔNG auto-load — deterministic enforcement qua `.claude/hooks/*.py` | ~10 rules | Rules không có natural file-scope trigger; hook detect pattern + BLOCK/WARN (vd `admin-merge-discipline` qua PreToolUse Bash; `concurrent-production-mutation-ops` qua workflow scan) |

**Wave 73 status (2026-05-14):** Bucket 0 pilot path-scopes `aws-sg-description-ascii` để verify Anthropic `paths:` mechanism. Buckets A1-A5 path-scope ~30 MANDATORY rules; Bucket B wires hooks; Bucket C dynamic UserPromptSubmit injection; Bucket D governance (`context-budget-mandate.md` rule).

---

## How auto-load works (Anthropic native feature)

Per Anthropic docs (https://code.claude.com/docs/en/memory) — "Path-specific rules":

- Rule với `paths:` YAML frontmatter chỉ load vào context khi Claude đọc file matching glob
- Rule không có `paths:` (hoặc CRITICAL): auto-load mỗi session
- Glob patterns hỗ trợ: `**/*.ext`, `path/{a,b}/**`, `*.csv`, etc.
- Multi-glob: list under `paths:` hoặc array form

Example pilot rule (`aws-sg-description-ascii.md`):

```yaml
---
paths:
  - "infrastructure/**/*.tf"
---

# AWS Security Group Description ASCII-Only
...
```

→ Rule này KHÔNG xuất hiện trong base context khi session không động đến `*.tf` file. Khi Claude đọc bất kỳ file `infrastructure/**/*.tf`, rule auto-loads.

**Verify pilot works after merge:** fresh `/start-session` should NOT show `aws-sg-description-ascii.md` trong auto-load list (vì không có `infrastructure/**/*.tf` đang được Claude đọc).

---

## How `paths:` frontmatter works

### Format

```yaml
---
paths:
  - "<glob-1>"
  - "<glob-2>"
---

# Rule Title

**Priority:** ...
**Version:** ...
...
```

### Rules

1. **YAML block ở TOP** (trước `# Rule Title`).
2. **`paths:` field** chỉ ảnh hưởng loading mechanism — KHÔNG thay đổi rule content.
3. **Markdown header frontmatter** (`**Priority:**`, `**Version:**`, ...) vẫn giữ nguyên — đó là human-readable metadata per `rule-change-process.md` §3.
4. **`rules-index.csv` `path_trigger` column** PHẢI khớp với `paths:` value của rule (single source of truth — CSV cache).

### Bumping version when adding `paths:`

Adding `paths:` frontmatter = PATCH bump (clarification, no constraint change). Rule applies same scope; chỉ khác cách Claude load nó.

---

## How to add new rule

Per [`rule-change-process.md`](rule-change-process.md) §6.5 Enforcement Parity Mandate:

1. Create `<name>.md` với markdown-header frontmatter (Priority + Version 1.0.0 + Created + Last-Reviewed + Reviewer-Approver + Applies to)
2. Determine tier:
   - **CRITICAL** → no `paths:` frontmatter (auto-load)
   - **MANDATORY path-scoped** → add `paths:` YAML frontmatter at top
   - **Hook-covered** → add hook in `.claude/hooks/` SAME PR
3. Add row to `rules-index.csv` (path_trigger column = exact `paths:` glob OR empty)
4. Ship same-PR enforcement: hook / CI check / PR template / skill / reviewer-checklist
5. Self-test: synthetic fixture demonstrating rule fires on originating incident

CI validators (auto-block PR on miss):
- `bash scripts/check-rule-frontmatter.sh` — verify required fields present
- `bash scripts/check-rules-index-csv.sh` — verify CSV row exists for every rule file
- `bash scripts/check-meta-csv-indexes.sh` — schema/format validation

---

## Index

Canonical: [`rules-index.csv`](rules-index.csv) (54 rows as of Wave 73 Bucket 0).

Quick queries (after Wave 73 Bucket 0 ships `path_trigger` column):

```bash
# All CRITICAL rules (auto-load)
bash scripts/query-rules.sh CRITICAL

# Count rules by priority
bash scripts/query-rules.sh --count CRITICAL
bash scripts/query-rules.sh --count MANDATORY

# Find AWS-related rules
bash scripts/query-rules.sh --grep aws
```

---

## Cross-references

- [`rule-change-process.md`](rule-change-process.md) — semver + review matrix + enforcement parity mandate (master process for rule edits)
- [`meta-csv-index-pattern.md`](meta-csv-index-pattern.md) — CSV-canonical pattern (rules-index.csv is one of 3 indexes: gaps, ADRs, rules)
- [`meta-gap-priority.md`](meta-gap-priority.md) — meta-rule prioritization (rules/skills/workflow gaps trump feature gaps at same P-level)
- [`incident-to-rule-pipeline.md`](incident-to-rule-pipeline.md) — 5-stage pipeline (Detect → Classify → Rule+Enforce → Self-Test → Retro Log) — every user-flagged miss converts to permanent guard
- [`output-review-mandate.md`](output-review-mandate.md) §3 — review standards matrix; rules-index.csv tracked here (Meta CSV indexes row)

---

## Wave 73 context optimization

Wave 73 — Meta Context Optimization (2026-05-14) targets reducing base context auto-load từ ~250k → ~100k tokens (saved ~45% context window). Bucket 0 (this PR) is the foundation:

- ✅ `path_trigger` column added to `rules-index.csv` (this PR)
- ✅ Pilot path-scope `aws-sg-description-ascii.md` (this PR — empirical test of Anthropic `paths:` mechanism)
- ✅ This README explaining tier convention (this PR)
- ⏳ Buckets A1-A5: path-scope ~30 MANDATORY rules (parallel after Bucket 0 merges)
- ⏳ Bucket B: 8 hooks for non-path-scopable rules
- ⏳ Bucket C: UserPromptSubmit dynamic injection
- ⏳ Bucket D: `context-budget-mandate.md` governance rule
- ⏳ Bucket E: verification + baseline measurement

Wave plan: [`documents/03-planning/waves/wave-2026-05-14-73-meta-context-optimization.md`](../../documents/03-planning/waves/wave-2026-05-14-73-meta-context-optimization.md)
