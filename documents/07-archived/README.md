# 07-archived — Archived Documentation

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../.claude/rules/docs-folder-structure.md)

Historical / superseded / completed documentation. **Read-only by convention** — append new subdirs, never rewrite archived content. Kept for historical traceability, thesis references, và post-mortem learning.

**Audience:** Developers investigating "why did we do X 3 months ago", thesis author citing prior work, auditors reviewing compliance history.

---

## Directory Map

| Path | Purpose | Period |
|------|---------|--------|
| `README.md` | This index | — |
| [`academic/`](academic/) | Academic thesis artifacts (chapters, word reports) | 2025-2026 |
| [`compliance/`](compliance/) | Superseded compliance documentation | Pre-2026-04 |
| [`early-ideas/`](early-ideas/) | Early brainstorming / action logs | Pre-2026-03 |
| [`implementation/`](implementation/) | PR completion reports + old implementation plans | 2025-Q4 → 2026-Q1 |
| [`kiteclass-legacy-docs/`](kiteclass-legacy-docs/) | KiteClass docs before platform split | Pre-2026-03 |
| [`logs/`](logs/) | Session / pilot / weekly completion logs | 2025-Q4 → 2026-Q1 |
| [`old-plans/`](old-plans/) | Superseded plans (docs-refactor, superpowers-integration, survey-v1) | 2026-Q1 |
| [`research/`](research/) | Research notes (architecture, competitive, services, technology) | Pre-2026-03 |

---

## File Placement Rules

- ✅ **Move HERE when:**
  - Wave merged + all gaps in wave marked DONE → wave plan archived
  - Plan superseded by newer plan (add `superseded_by:` to new plan frontmatter)
  - Doc >90 days old AND no recent reference (trigger quarterly bulk archival)
  - PR-specific log where PR is merged
  - Research snapshot no longer driving decisions

- ❌ **Do NOT archive:**
  - ADRs — always keep in `02-architecture/adr/`, mark superseded in-place
  - Gap files — closed gaps stay in `04-quality/gaps/` với status 🟢 DONE
  - Living docs (business rules, architecture current state) — update in-place, never archive
  - CLAUDE.md, rules files — evolve in-place with changelog

- Naming convention for new subdirs: `{category}-YYYY[-Qn]/` (vd. `planning-2026-Q1/`, `architecture-2026/`)

---

## Historical Subdirs Context

- **`academic/`** — Thesis chapters, word reports submitted. Traceable source for `08-thesis/`.
- **`compliance/`** — Pre-2026 compliance attempts before formal legal engagement (Wave 0).
- **`early-ideas/`** — Pre-project brainstorming, action logs. Low signal, kept for provenance.
- **`implementation/`** — PR-3.x completion reports from early KiteClass-only phase. Superseded by `03-planning/pr-logs/` auto-generated files.
- **`kiteclass-legacy-docs/`** — KiteClass-only era (before KiteHub platform split). Architecture + gateway guides pre-consolidation.
- **`logs/`** — Weekly / pilot / PR completion logs. Rich post-mortem content; referenced for "superpowers adoption metrics" analysis.
- **`old-plans/`** — Plans replaced by newer versions (refactor v2, superpowers v2, survey v2).
- **`research/`** — Technology evaluations before stack lock-in (2025 research).

---

## Retrieval Guidance

Find archived doc:
```bash
# By keyword
grep -rl "keyword" documents/07-archived/

# By period
ls documents/07-archived/*/ | grep "YYYY"
```

If a archived doc becomes actively referenced again → consider **revival**: copy to appropriate active folder, update, commit separately from archive removal.

---

## Archive Cadence

Bulk archival happens **end-of-quarter**:
- Reviewer: Tech Lead (quarterly cleanup session)
- Move criteria: docs matching rules above
- Commit style: `chore(docs): archive Q{n} {YYYY} planning + logs`

Ad-hoc archival allowed for: superseded plans, completed PR logs, merged waves.

---

## Related

- **Rule:** [`.claude/rules/planning-docs-structure.md`](../../.claude/rules/planning-docs-structure.md) §5 Archival Rule
- **Rule:** [`.claude/rules/docs-folder-structure.md`](../../.claude/rules/docs-folder-structure.md) §3 generic retention guidance
- **Consumer:** [`08-thesis/`](../08-thesis/) cites archived research + academic subdirs
