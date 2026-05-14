# Session Handoffs

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md) + [`.claude/rules/planning-docs-structure.md`](../../../.claude/rules/planning-docs-structure.md)

Mỗi file trong folder này là một **session handoff log** — viết khi context heavy / wave chuyển giao / multi-stream work đang dang dở. Mục đích: session mới đọc 1 file là pick up được mọi pending work + decision context.

---

## Directory map

| Path | Purpose | Typical files |
|------|---------|---------------|
| `README.md` | This index | 1 |
| `YYYY-MM-DD-session-handoff.md` | Session handoff log on YYYY-MM-DD | 1 per handoff |

---

## File placement rules

- ✅ **Belongs here:** session-end handoff capturing pending PRs / open work / audit findings / next-session pickup order
- ❌ **Does NOT belong here:** wave plans (→ `../waves/`), feature plans (→ `../plans/`), gap files (→ `../../04-quality/gaps/`)
- Naming: `YYYY-MM-DD-session-handoff.md` (ISO date + descriptor)

---

## Archive policy

Move handoff log → `documents/07-archived/session-handoffs-YYYY/` khi:
- Handoff items đã all DONE (verified bằng git log / gap-status.csv)
- ≥ 30 ngày từ ngày handoff
- Handoff superseded bởi handoff mới hơn (chuyển thông tin)

Solo-dev: archive ngay khi handoff superseded (hand-off "tin tức cũ" không có giá trị retention).

---

## Key documents

- [2026-05-14 session handoff](2026-05-14-session-handoff.md) — Wave 72a/72b shipped + 3 outside-in audits + outside-in-coverage-trigger rule
