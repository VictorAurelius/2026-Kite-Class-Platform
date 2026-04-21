# Integrations — Architecture Docs

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md)

Architecture docs for third-party system integrations (MIS/SMS, payment
gateways, identity providers, messaging). Companion to
`documents/01-business/kiteclass/{domain}/` which holds the 3-layer business
docs per integration domain.

---

## Directory Map

| Path | Purpose | Typical files |
|------|---------|---------------|
| `README.md` | This index | 1 |
| `school-mis-catalog.md` | K-12 MIS/SMS comparison + selection | 1 |

---

## File Placement Rules

- ✅ **Belongs here:** vendor comparison tables, API capability matrices, auth
  mode summaries, data-residency notes that inform an ADR.
- ❌ **Does NOT belong here:** adapter code (→ `kiteclass-core/.../integration/`),
  business rules (→ `01-business/{domain}/rules.md`), ADRs (→ `adr/`).
- Naming: `{category}-catalog.md` for vendor catalogs,
  `{vendor}-integration-notes.md` for vendor-specific deep dives.

---

## Archive Policy

Move to `documents/07-archived/integrations-YYYY/` when:
- Vendor replaced by a successor catalog (add `SUPERSEDED_BY`)
- Integration deprecated and no longer maintained
- Doc >180 days old AND no recent PR reference

---

## Key Documents

- [school-mis-catalog.md](./school-mis-catalog.md) — VNEDU, SMAS, Base.vn, MS SDS, Google Classroom comparison (GAP-200).
