# i18n — Audit reports cho internationalization và Vietnamese content quality

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../../.claude/rules/docs-folder-structure.md), [`.claude/rules/dev-readable-doc-language.md`](../../../../.claude/rules/dev-readable-doc-language.md)

Folder chứa audit reports đánh giá chất lượng tiếng Việt + i18n coverage cho các customer-facing surface (landing, pricing, TOS, email templates, dashboard banner). Audit reports ship cùng PR vá nội dung tiếng Việt hoặc thêm locale mới.

---

## Directory Map

| Path | Purpose | Typical files |
|------|---------|---------------|
| `README.md` | This index | 1 |
| `YYYY-MM-DD-<topic>.md` | Per-wave / per-audit Vietnamese content + i18n coverage report | growing |

---

## File Placement Rules

- ✅ **Belongs here:** customer-facing Vietnamese content quality, i18n locale coverage %, mixed-language compliance per `dev-readable-doc-language.md`, locale switcher behavior audit
- ❌ **Does NOT belong here:** UI screen `/128` scoring (→ `../ui-review/`); accessibility WCAG audit (→ `../ui/`); business logic translation accuracy (→ `../business-logic/`)
- Naming: `YYYY-MM-DD-<topic>.md` — date prefix sortable; topic kebab-case

---

## Archive Policy

Move to `documents/07-archived/i18n-YYYY/` khi:
- Audit >180 days old AND no recent reference
- Locale dropped from supported list (vd EN drop nếu Phase 1 BETA vi-only confirmed)

---

## Key Documents

- [2026-05-14 Customer-facing Vietnamese audit (Wave 78 Bucket A)](2026-05-14-customer-facing-vi-audit.md) — landing + pricing + TOS placeholder + signup redirect audit; closes GAP-541 PARTIAL
