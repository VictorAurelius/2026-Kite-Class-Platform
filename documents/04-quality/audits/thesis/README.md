---
title: Thesis academic audit reports
audience: dev
---

# Thesis academic audit reports

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../../.claude/rules/docs-folder-structure.md), [`.claude/rules/thesis-content-standard.md`](../../../../.claude/rules/thesis-content-standard.md)

Folder chứa audit artifacts cho academic deliverable scope (`documents/08-thesis/**`). Khác với `persona-review/` chuyên cho outside-in persona simulation audits, folder này tập trung audit thesis-content focus (citation evidence, academic tone, format compliance, draft-marker scrub, project-internal reference scrub) per `thesis-content-standard.md` 9-category rubric.

---

## Directory Map

| Path | Purpose | Typical files |
|------|---------|---------------|
| `README.md` | This index | 1 |
| `YYYY-MM-DD-wave-NNN-bucket-X-<topic>.md` | Per-wave per-bucket audit reports (citations / academic tone / format / etc.) | N |

---

## File Placement Rules

- ✅ **Belongs here:** thesis-content audits (citation evidence per `thesis-content-standard.md` C3 + S4, academic tone per C4, project-internal scrub per C5, draft-marker scrub per C6, format compliance per C1)
- ❌ **Does NOT belong here:** outside-in persona simulation audits (use `persona-review/`); thesis defense failure-mode matrix (use `persona-review/`); UTC benchmark audits (use `persona-review/` or `external-benchmark/`)
- Naming: `YYYY-MM-DD-wave-NN.N.N-bucket-X-<topic>.md` (date-prefix per `docs-filename-prefix-convention.md` Tier 2; bucket suffix for traceability vào wave plan)

---

## Archive Policy

Move to `documents/07-archived/thesis-audits-YYYY/` khi:
- Audit doc >90 ngày tuổi per `docs-archival-cadence.md` Rule 1
- Thesis cho mùa academic đã ship + defense complete
- File >180 ngày + no recent reference từ wave plans hiện hành

---

## Key Documents

(Populated as audits accumulate)
