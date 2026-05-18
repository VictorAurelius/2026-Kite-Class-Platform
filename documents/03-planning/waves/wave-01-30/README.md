# Wave 01-30 Archive

**Last Updated:** 2026-05-18
**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../../.claude/rules/docs-folder-structure.md) + [`.claude/rules/docs-folder-volume-budget.md`](../../../../.claude/rules/docs-folder-volume-budget.md)

Wave plans cho waves 01-30 (project 2026-04 timeframe). Wave 94b refactor 2026-05-18 moved here từ `documents/03-planning/waves/` root để satisfy Rule 3 (per-folder volume budget — root vượt 50-file cap với 108 files).

## Directory Map

| Path | Purpose | Typical files |
|------|---------|---------------|
| `README.md` | This index | 1 |
| `wave-NN-*.md` | Wave plans 01-30 | 23 |

## File Placement Rules

- ✅ **Belongs here:** Wave plan với wave number ≤30 (numeric extracted từ filename)
- ❌ **Does NOT belong here:** Wave 31+ (`../wave-31-60/`), Wave 61+ (`../wave-61-90/`), Wave 91+ (root `..`)
- Naming: `wave-NN-{topic}.md` (legacy short format) hoặc `wave-YYYY-MM-DD-NN-{topic}.md` (date-prefix format)

## Archive Policy

Per `.claude/rules/docs-archival-cadence.md` §2 — wave plans với `status: complete` >60 ngày POST closure date archive sang `documents/07-archived/planning-{year}/waves/`. Active wave plans (status: draft/in-progress) stay here.

## Key Documents

- [`../README.md`](../README.md) — parent waves folder index (Wave 91+ active scope)
- [`../wave-31-60/README.md`](../wave-31-60/README.md) — sister subdir Wave 31-60
- [`../wave-61-90/README.md`](../wave-61-90/README.md) — sister subdir Wave 61-90
- [`.claude/rules/planning-docs-structure.md`](../../../../.claude/rules/planning-docs-structure.md) — planning docs governance
