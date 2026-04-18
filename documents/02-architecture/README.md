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
| [`ai-branding-v2-redesign.md`](ai-branding-v2-redesign.md) | AI Branding system architecture (key feature) | 1 |
| [`ai-branding-design-patterns.md`](ai-branding-design-patterns.md) | Design patterns applied in AI Branding | 1 |
| [`kiteclass-architecture.md`](kiteclass-architecture.md) | KiteClass core module architecture | 1 |
| [`docker-platform-architecture.md`](docker-platform-architecture.md) | Docker stack + service topology | 1 |
| [`domain-management.md`](domain-management.md) | Domain/DNS architecture (kitehub.vn + tenant subdomains) | 1 |
| [`email-lifecycle.md`](email-lifecycle.md) | Email sending architecture + RabbitMQ flow | 1 |
| [`backup-strategy.md`](backup-strategy.md) | Backup architecture (PostgreSQL, MinIO, Redis) | 1 |
| [`data-retention-policy.md`](data-retention-policy.md) | Data retention + deletion architecture | 1 |
| [`ssl-automation.md`](ssl-automation.md) | SSL cert automation (Let's Encrypt wildcard) | 1 |
| [`living-docs-audit-2026-04.md`](living-docs-audit-2026-04.md) | Living docs audit snapshot (Apr 2026) | 1 |

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

- **AI Branding (key feature):** [`ai-branding-v2-redesign.md`](ai-branding-v2-redesign.md) → [`ai-branding-design-patterns.md`](ai-branding-design-patterns.md)
- **Service topology:** [`docker-platform-architecture.md`](docker-platform-architecture.md)
- **Domain/DNS:** [`domain-management.md`](domain-management.md)

---

## ADR Process

`adr/` chứa 14 ADRs (Michael Nygard format). Index: [`adr/README.md`](adr/README.md). Template: [`adr/_TEMPLATE.md`](adr/_TEMPLATE.md).

**Status:** ADRs 001-013 shipped 2026-04-14 (initial architecture sweep). ADR-014 (Async Jobs Queue over Batch) shipped 2026-04-18 retroactively capturing Wave 1/3 decisions (GAP-102 kickoff).

**Planned:** ADR-015 — AWS Agent Plugins evaluation (GAP-103).

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
