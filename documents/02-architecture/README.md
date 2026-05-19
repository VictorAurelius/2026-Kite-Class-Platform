---
title: 02-architecture — Technical Architecture
audience: mixed
created: 2026-04-18
last-reviewed: 2026-05-19
status: living
---

# 02-architecture — Technical Architecture

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../.claude/rules/docs-folder-structure.md)

Technical architecture documentation — system design, component interactions, data flow, cross-cutting concerns, và Architectural Decision Records (ADRs). Chứa "what + how" của system ở tầng architecture; "why" (rationale decisions) thuộc [`adr/`](adr/).

**Audience:** Backend engineers, Frontend engineers, SRE/DevOps, Tech Leads, Architects. Secondary: thesis reviewers, new contributors onboarding.

> 📅 **Last reviewed:** **2026-05-19** · Wave 99B B5 — Onboarding Tour orchestrator landing

---

## 🚀 Reading Order — Golden-Path Onboarding Tour

Đọc theo thứ tự 7 bước này để build mental model architecture của Kite Platform end-to-end (~60-90 phút):

| # | Step | File | What you'll learn |
|---|---|---|---|
| 1 | **System boundary (L1) + Container topology (L2)** | [`c4-context-container.md`](c4-context-container.md) | 8 actor personas + 6 external systems (Resend/SES/VietQR/Zalo/CF/Statuspage); 2 FE + 1 gateway + 7 services + 4 infra subgraph |
| 2 | **Service catalog + Dependency graph + Auth flow** | [`service-catalog-and-auth-flow.md`](service-catalog-and-auth-flow.md) | 18-service catalog (8 BE + 2 FE + 8 infra); Mermaid dependency graph ~37 edges; auth sequence ~24 steps; 10-controller role-guard matrix |
| 3 | **Database entity catalog + FK graph + RLS map + Flyway history** | [`database-architecture-map.md`](database-architecture-map.md) | 91 entities (32 kh-subscription + 59 kc-core); RLS coverage 51/91; 114 V-files migration history; top-10 row drivers Phase 1 BETA |
| 4 | **Multi-tenant strategy + DB-level isolation + RLS implementation** | [`multi-tenant-architecture.md`](multi-tenant-architecture.md) | tenant_id propagation 4 RLS clusters; tenant lifecycle (trial → subscription → off-boarding); per-tenant subdomain routing |
| 5 | **Compliance × Code Map + SLO Registry + NFR + Risk Register** | [`compliance-control-map.md`](compliance-control-map.md) | 19 compliance rows (PDPL Art 7 + Luật ANM + ISO27001); 11-service SLO registry + 5 platform-wide composite SLO; 35+ NFR rows; 5-row Risk Register |
| 6 | **Why-decisions — ADR index** | [`adr/README.md`](adr/README.md) | 31 ADRs (MADR format): K12 data model, role hierarchy, instance lifecycle, AWS Singapore Free Tier, FE self-host EC2, kiteclass-gateway removal |
| 7 | **Threat models per domain** | [`threat-models/`](threat-models/) | Per-domain threat models — STRIDE analysis for auth, payment, AI branding, tenant isolation |

**Total reading:** ~60-90 phút (depends persona — see Per-Persona Reading List dưới). Sau khi đọc xong 7 bước, bạn có thể trace 1 user request end-to-end qua mọi tầng architecture.

---

## 🔍 Trace One Request — End-to-End Tutorial

Walk-through cụ thể: **"Anonymous prospect submit beta-access form trên `kitehub.me/request-beta` → DB row `beta_request` được create với status PENDING + email confirm gửi tới user"**

Mỗi tầng architecture có file để đọc:

| Layer | What happens | Read this |
|---|---|---|
| **1. Browser (FE)** | User mở `https://kitehub.me/request-beta` → React `RequestBetaForm` component validates input (email, fullName, organizationName, organizationType, message) | [`c4-context-container.md`](c4-context-container.md) L2 — locates `kitehub-frontend` container |
| **2. CDN → EC2 self-host** | Cloudflare proxy (`A record → 54.179.70.37`) serves static FE assets; form submit → `POST /api/platform/auth/beta-signup/validate` via api.kitehub.me | [`adr/ADR-031-fe-self-host-aws-ec2.md`](adr/) — FE hosting decision; [`ssl-automation.md`](ssl-automation.md) — TLS cert flow |
| **3. Gateway** | `kitehub-gateway` routes `/api/platform/auth/beta-signup/*` → `kitehub-subscription` service (port 4710); JWT propagation per [`adr/ADR-021-gateway-jwt-propagation.md`](adr/) | [`service-catalog-and-auth-flow.md`](service-catalog-and-auth-flow.md) §Gateway routing |
| **4. Service** | `BetaSignupController.submitRequest()` → `BetaSignupService.create()` validates business rule (max 3 requests per email per 7 days per `business/subscription/rules.md`) → emits `BetaRequestCreated` event | [`service-catalog-and-auth-flow.md`](service-catalog-and-auth-flow.md) §kitehub-subscription |
| **5. Database** | INSERT vào `beta_request` table (RLS bypassed — anonymous public endpoint; row visible to admin only via service role) with status='PENDING' + audit_log entry | [`database-architecture-map.md`](database-architecture-map.md) §kh-subscription entities (32 entities catalog) + §RLS map (51/91 coverage) |
| **6. Async — Email** | RabbitMQ outbox dispatcher picks `BetaRequestCreated` event → kitehub-email service → SES `kite-noreply@kitehub.me` template `beta-request-confirmation` → tenant inbox | [`email-architecture.md`](email-architecture.md) — dual-vendor SES + Resend topology + DKIM signing |
| **7. Compliance + Audit** | PDPL Art 7 (lawful processing): consent flag stored; admin_audit_log row immutable (V60 migration); GDPR-equivalent retention 7 năm per data-retention-policy | [`compliance-control-map.md`](compliance-control-map.md) §PDPL + [`data-retention-policy.md`](data-retention-policy.md) |
| **8. SLO + Risk** | P99 endpoint latency target ≤500ms tracked per [`compliance-control-map.md`](compliance-control-map.md) §SLO Registry; failure mode = email queue depth alert (R1 Risk Register row) | [`compliance-control-map.md`](compliance-control-map.md) §Risk Register R1 |

**Hands-on follow-up:** sau khi đọc 8 layer trên, mở [`service-catalog-and-auth-flow.md`](service-catalog-and-auth-flow.md) §Auth Flow sequenceDiagram để xem 24-step authenticated equivalent (login + role-guard + RLS) — same pattern khác là endpoint authenticated thay vì anonymous.

---

## 👥 Per-Persona Reading List

Recommended reading depending on your role + onboarding goal:

### P1 — Backend Engineer (joining team to write Java services)

**Goal:** understand service boundaries + data model + write your first endpoint within 1 week.

**Priority reading (~3-4 hours):**
1. [`service-catalog-and-auth-flow.md`](service-catalog-and-auth-flow.md) — 18 service catalog (mandatory; know which service owns what)
2. [`database-architecture-map.md`](database-architecture-map.md) — 91 entity catalog + FK + RLS (mandatory; know which table belongs where)
3. [`multi-tenant-architecture.md`](multi-tenant-architecture.md) — tenant_id propagation pattern (mandatory; every query needs RLS context)
4. [`adr/README.md`](adr/README.md) — skim 31 ADRs (mandatory; understand prior decisions)
5. [`kitehub-architecture.md`](kitehub-architecture.md) — KiteHub SaaS platform specifics
6. [`kiteclass-architecture.md`](kiteclass-architecture.md) — KiteClass tenant platform specifics

**Skip first pass:** UI design system, threat models (revisit Week 2+)

### P2 — Frontend Engineer (joining team to write React/Next.js)

**Goal:** understand gateway API surface + tenant subdomain routing + auth flow within 1 week.

**Priority reading (~2-3 hours):**
1. [`c4-context-container.md`](c4-context-container.md) L1+L2 — system boundary + FE container topology (mandatory)
2. [`service-catalog-and-auth-flow.md`](service-catalog-and-auth-flow.md) §Gateway routing + §Auth flow sequence (mandatory; know endpoint contracts + JWT lifecycle)
3. [`domain-management.md`](domain-management.md) — DNS + tenant subdomain pattern (mandatory; know URL strategy)
4. [`design-system/dossier/01-personas.md`](design-system/dossier/01-personas.md) — UI persona catalog (P1/P2/P3 + Anonymous Vy + Platform Admin Mai)
5. [`design-system/`](design-system/) UI kits + dossier — design system reference

**Skip first pass:** Backend service internals, threat models, ADRs (revisit Week 2+)

### P3 — SRE / DevOps Engineer (joining team to operate Phase 1 BETA)

**Goal:** understand SLO targets + deployment pipeline + incident response within 1 week.

**Priority reading (~3-4 hours):**
1. [`compliance-control-map.md`](compliance-control-map.md) §SLO Registry + §NFR + §Risk Register (mandatory; know targets + measurement gaps)
2. [`deployment-strategy.md`](deployment-strategy.md) — 5 principles + env matrix (mandatory; understand deploy philosophy)
3. [`adr/ADR-025-aws-singapore-free-tier.md`](adr/) + [`adr/ADR-031-fe-self-host-aws-ec2.md`](adr/) — AWS topology decisions (mandatory)
4. [`ssl-automation.md`](ssl-automation.md) — Let's Encrypt wildcard + cert renewal cadence
5. [`env-vars-registry.md`](env-vars-registry.md) — production env config (mandatory; canonical source for all env vars)
6. [`../05-guides/operations/`](../05-guides/operations/) — operational runbooks (incident response, secrets rotation, restore drill)

**Skip first pass:** Domain business logic, UI design, ADRs unrelated to ops (revisit when on-call)

### P4 — Tech Lead / Architect (joining team to lead architecture decisions)

**Goal:** comprehensive cross-cutting view + ADR contribution capability within 2 weeks.

**Priority reading (~6-8 hours, full sweep):**
1. **All 7 steps of Reading Order Tour above** — full mental model end-to-end
2. [`adr/`](adr/) — read ALL 31 ADRs cover-to-cover (rationale + alternatives + consequences)
3. [`threat-models/`](threat-models/) — all per-domain threat models (STRIDE analysis)
4. [`../03-planning/roadmap/release-1-plan-2026.md`](../03-planning/roadmap/release-1-plan-2026.md) — Release 1 Phase 1+2+3 strategy
5. [`../04-quality/audits/`](../04-quality/audits/) — recent audit reports (Quality 90/110 + Security 93/100 + Ops 77/100 + Performance 86/100)
6. [`../../.claude/rules/`](../../.claude/rules/) — 70 governance rules (skim CRITICAL + MANDATORY tier)

**No skip:** Tech Lead needs full picture.

### Anonymous / Thesis Reviewer (browsing repo for first time)

Start với [`c4-context-container.md`](c4-context-container.md) L1 (system boundary) → [`kitehub-architecture.md`](kitehub-architecture.md) overview → optional deep-dive theo curiosity.

---

## Directory Map

| Path | Purpose | Typical files |
|------|---------|---------------|
| `README.md` | This index (orchestrator) | 1 |
| [`adr/`](adr/) | Architectural Decision Records (MADR format) | `ADR-NNN-*.md` + `_TEMPLATE.md` + `README.md` + `adrs-index.csv` |
| [`threat-models/`](threat-models/) | Per-domain threat models (STRIDE) | 4 |
| [`design-system/`](design-system/) | UI kits + design dossier | (nested) |
| [`integrations/`](integrations/) | External integration architecture (Resend, MISA, etc.) | (varies) |
| [`c4-context-container.md`](c4-context-container.md) | C4 L1+L2 system boundary + container topology (Wave 99B B4) | 1 |
| [`service-catalog-and-auth-flow.md`](service-catalog-and-auth-flow.md) | 18-service catalog + dependency graph + auth flow (Wave 99B B1) | 1 |
| [`database-architecture-map.md`](database-architecture-map.md) | 91-entity catalog + FK graph + RLS map + Flyway history (Wave 99B B3) | 1 |
| [`compliance-control-map.md`](compliance-control-map.md) | Compliance × Code + SLO Registry + NFR + Risk Register (Wave 99B B2) | 1 |
| [`kitehub-architecture.md`](kitehub-architecture.md) | KiteHub SaaS platform architecture (Wave 96 PR2) | 1 |
| [`kiteclass-architecture.md`](kiteclass-architecture.md) | KiteClass core module architecture (Wave 96 PR2) | 1 |
| [`multi-tenant-architecture.md`](multi-tenant-architecture.md) | Multi-tenant strategy DB-level isolation + RLS (Wave 96 PR2) | 1 |
| [`email-architecture.md`](email-architecture.md) | Email vendor architecture SES + Resend dual-vendor | 1 |
| [`domain-management.md`](domain-management.md) | Domain/DNS architecture (kitehub.me + tenant subdomains) | 1 |
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

## ADR Process

`adr/` chứa 31 ADRs (Michael Nygard format). Index: [`adr/README.md`](adr/README.md). CSV: [`adr/adrs-index.csv`](adr/adrs-index.csv). Template: [`adr/_TEMPLATE.md`](adr/_TEMPLATE.md).

**Status:** ADRs 001-013 shipped 2026-04-14 (initial architecture sweep). ADR-014 (Async Jobs Queue over Batch) + ADR-015 (AWS Agent Plugins defer) shipped 2026-04-18. Recent: ADR-025 AWS Singapore Free Tier, ADR-028 Phase 1 BETA scale acceptance, ADR-031 FE self-host AWS EC2, ADR-032 kiteclass-gateway removal.

Mọi architectural decision với ≥2 options considered PHẢI có ADR mới.

---

## Archive Policy

Move to `documents/07-archived/architecture-YYYY-QN/` khi:
- Architecture superseded (vd. AI Branding v2 → v3) — keep both until v3 merged, then archive v2
- Component removed (vd. service decommissioned — see kiteclass-gateway per ADR-032)
- Audit snapshot >180 days old (living-docs-audit-*.md files)

**Recent archive batch (Wave 99B B6, 2026-05-19):** 6 stale/superseded files moved to [`07-archived/architecture-2026-Q2/`](../07-archived/architecture-2026-Q2/) — `living-docs-audit-2026-04` + `ai-branding-v2-redesign` + `ai-branding-design-patterns` + `backup-strategy` + `docker-platform-architecture` + `email-lifecycle`. Root-level count 16 → 10 (volume cap 50 compliant per `docs-folder-volume-budget.md`).

ADRs NEVER archived — append `superseded_by:` trong frontmatter, keep in place.

---

## Related

- **Rules:** [`.claude/rules/design-patterns.md`](../../.claude/rules/design-patterns.md) enforces patterns trong code; this folder documents WHERE they apply
- **Rules:** [`.claude/rules/diagram-format-selection.md`](../../.claude/rules/diagram-format-selection.md) — Mermaid default for architecture diagrams (GitHub native render)
- **Diagrams:** [`documents/06-diagrams/`](../06-diagrams/) PlantUML source for visualizations referenced here
- **Quality audits:** [`documents/04-quality/audits/`](../04-quality/audits/) — Quality /110 + Security /100 + Performance /100 + Ops /100 reports tracking architecture health
- **Planning:** [`documents/03-planning/`](../03-planning/) — wave plans + roadmap; current wave: 99B (this orchestrator)
- **GAP-046** — design patterns applied systematically
- **GAP-102** — ADR kickoff (populates `adr/`)
- **Wave 99B B1-B5 origin gaps:** GAP-670 (B1 Service Catalog) · GAP-671 (B2 Compliance Map) · GAP-672 (B3 Database Map) · GAP-673 (B4 C4 Diagram) · GAP-674 (B5 this Onboarding Tour)
