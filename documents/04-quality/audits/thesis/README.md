# Thesis Audit Reports

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../../.claude/rules/docs-folder-structure.md), [`.claude/rules/thesis-content-standard.md`](../../../../.claude/rules/thesis-content-standard.md)

Subfolder chứa audit reports cho thesis V1+ deliverables — fix passes, persona reviews, measurement methodology, academic integrity scrubs. Sibling cho `persona-review/`, `quality/`, `aws-verification/`, etc. Sister-pattern: thesis scope tách riêng vì lifecycle khác (academic submission cycle vs operational/quality audit cycle).

---

## Directory Map

| Path | Purpose | Typical files |
|------|---------|---------------|
| `README.md` | This index | 1 |
| `YYYY-MM-DD-wave-XX.X.X-bucket-Y-*.md` | Wave-bucket audit artifacts (fix passes per Wave 102.7.x series) | 3-N per wave series |

---

## File Placement Rules

- ✅ **Belongs here:** thesis-related audit reports — fix passes per chapter/bucket, persona reviews thesis-specific, measurement methodology scrubs, academic integrity verifications
- ❌ **Does NOT belong here:** persona simulation broader project scope (use `../persona-review/`), product quality audits (use `../quality/`), AWS infra verification (use `../aws-verification/`)
- Naming: `YYYY-MM-DD-wave-NN.N.N-bucket-{a|b|c|d}-{topic-slug}.md` per `docs-filename-prefix-convention.md` Tier 2 time-bound

---

## Archive Policy

Move to `documents/07-archived/thesis-audits-YYYY/` khi:
- Thesis V1 đã ship final submission + audit reports >90 ngày
- Audit superseded bởi V2+ scope audit cùng category
- Per `docs-archival-cadence.md` cadence table 90-day rule cho audit reports

---

## Key Documents

(populated as wave-bucket audits ship — currently genesis 2026-05-20 Wave 102.7.3)
