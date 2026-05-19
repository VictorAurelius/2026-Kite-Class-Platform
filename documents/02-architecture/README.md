# 02-architecture — Technical Architecture

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../.claude/rules/docs-folder-structure.md)

Technical architecture documentation — system design, component interactions, data flow, cross-cutting concerns, và Architectural Decision Records (ADRs). Chứa "what + how" của system ở tầng architecture; "why" (rationale decisions) thuộc [`adr/`](adr/).

**Audience:** Architects, Tech Leads, Senior Engineers. Secondary: new developers onboarding, thesis reviewers.

---

## Directory Map

| Path | Purpose | Typical files |
|------|---------|---------------|
| `README.md` | This index | 1 |
| [`adr/`](adr/) | Architectural Decision Records (MADR format) | `ADR-NNN-*.md` + `TEMPLATE.md` + `README.md` |
| [`threat-models/`](threat-models/) | Per-domain threat models | 4 |
| [`design-system/`](design-system/) | UI kits + design dossier | (nested) |
| [`integrations/`](integrations/) | External integration architecture (Resend, MISA, etc.) | (varies) |
| [`kitehub-architecture.md`](kitehub-architecture.md) | KiteHub SaaS platform architecture (Wave 96 PR2) | 1 |
| [`kiteclass-architecture.md`](kiteclass-architecture.md) | KiteClass core module architecture (Wave 96 PR2) | 1 |
| [`multi-tenant-architecture.md`](multi-tenant-architecture.md) | Multi-tenant strategy DB-level isolation + RLS (Wave 96 PR2) | 1 |
| [`email-architecture.md`](email-architecture.md) | Email vendor architecture SES + Resend dual-vendor | 1 |
| [`domain-management.md`](domain-management.md) | Domain/DNS architecture (kitehub.vn + tenant subdomains) | 1 |
| [`data-retention-policy.md`](data-retention-policy.md) | Data retention + deletion architecture | 1 |
| [`ssl-automation.md`](ssl-automation.md) | SSL cert automation (Let's Encrypt wildcard) | 1 |
| [`deployment-strategy.md`](deployment-strategy.md) | Deployment philosophy single-source (5 principles + env matrix) | 1 |
| [`env-vars-registry.md`](env-vars-registry.md) | Production env config registry (single source of truth) | 1 |

---

## File Placement Rules

- ✅ **Belongs here:**
  - System architecture (how services interact)
  - Cross-cutting concerns (SSL, email, backup, retention, domains)
  - Design patterns catalog (applied patterns per feature)
  - Technology stack decisions + component topology

- ✅ **Belongs in [`adr/`](adr/):**
  - Why-decisions với alternatives considered (MADR format)
  - Example: "Why RabbitMQ over Spring Batch", "Why Helm over plain K8s manifests"

- ❌ **Does NOT belong here:**
  - Operational runbooks → [`documents/05-guides/`](../05-guides/) (how to operate, not how it's designed)
  - Implementation plans per wave → [`documents/03-planning/waves/`](../03-planning/waves/)
  - Per-domain business rules → [`documents/01-business/`](../01-business/)
  - Diagrams source → [`documents/06-diagrams/`](../06-diagrams/) (PlantUML, rendered PNG)

- Naming: `kebab-case.md`, ADRs `ADR-NNN-kebab-title.md` (zero-padded 3-digit)

---

## Key Documents (start here)

- **KiteHub SaaS platform:** [`kitehub-architecture.md`](kitehub-architecture.md) (canonical Wave 96 PR2)
- **KiteClass tenant platform:** [`kiteclass-architecture.md`](kiteclass-architecture.md) (canonical Wave 96 PR2)
- **Multi-tenant isolation:** [`multi-tenant-architecture.md`](multi-tenant-architecture.md) (canonical Wave 96 PR2)
- **Email vendor topology:** [`email-architecture.md`](email-architecture.md)
- **Deployment philosophy:** [`deployment-strategy.md`](deployment-strategy.md)
- **Domain/DNS:** [`domain-management.md`](domain-management.md)
- **Production env config:** [`env-vars-registry.md`](env-vars-registry.md)

> **AI Branding architecture:** archived to [`07-archived/architecture-2026-Q2/`](../07-archived/architecture-2026-Q2/) (Wave 99B B6 sweep) — shipped implementation reality now reflected in `kitehub-architecture.md` + `kiteclass-architecture.md` per-module ownership; design patterns enforced via [`.claude/rules/design-patterns.md`](../../.claude/rules/design-patterns.md). Older snapshots (`docker-platform-architecture.md`, `email-lifecycle.md`, `backup-strategy.md`, `living-docs-audit-2026-04.md`) also moved there — content now covered by Wave 96 PR2 reports + operations runbooks + business-layer 3-layer docs.

---

## ADR Process

`adr/` chứa 14 ADRs (Michael Nygard format). Index: [`adr/README.md`](adr/README.md). Template: [`adr/_TEMPLATE.md`](adr/_TEMPLATE.md).

**Status:** ADRs 001-013 shipped 2026-04-14 (initial architecture sweep). ADR-014 (Async Jobs Queue over Batch) + ADR-015 (AWS Agent Plugins defer) shipped 2026-04-18 (GAP-102, GAP-103).

Mọi architectural decision với ≥2 options considered PHẢI có ADR mới.

---

## Archive Policy

Move to `documents/07-archived/architecture-YYYY/` khi:
- Architecture superseded (vd. AI Branding v2 → v3) — keep both until v3 merged, then archive v2
- Component removed (vd. service decommissioned)
- Audit snapshot >180 days old (living-docs-audit-*.md files)

ADRs NEVER archived — append `superseded_by:` trong frontmatter, keep in place.

---

## Related

- **Rules:** [`.claude/rules/design-patterns.md`](../../.claude/rules/design-patterns.md) enforces patterns trong code; this folder documents WHERE they apply
- **Rules:** [`.claude/rules/ai-branding-guidelines.md`](../../.claude/rules/ai-branding-guidelines.md) developer guidelines; `ai-branding-v2-redesign.md` is architecture
- **Diagrams:** [`documents/06-diagrams/`](../06-diagrams/) PlantUML source for visualizations referenced here
- **GAP-046** — design patterns applied systematically (consumer of `ai-branding-design-patterns.md`)
- **GAP-102** — ADR kickoff (populates `adr/`)
