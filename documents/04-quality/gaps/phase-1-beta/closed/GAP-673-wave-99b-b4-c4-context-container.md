# GAP-673: Wave 99B B4 — C4 Context + Container Diagram (L1+L2) cho Kite Platform

**Status:** 🟢 DONE 2026-05-19 — `documents/02-architecture/c4-context-container.md` shipped với Mermaid L1 (System Context, 8 actors + 6 external systems) + Mermaid L2 (Container, 9 backend services + 4 shared infra) + explicit L3+L4 defer rationale
**Priority:** 🟠 P1
**Domain:** Meta / Architecture documentation
**Phase:** phase-1-beta
**Wave:** 99b
**Created:** 2026-05-19
**Closed:** 2026-05-19
**Sister gaps:** GAP-668 (B6 foundation — archive sweep), GAP-669 (B0 — Last-Reviewed backfill), GAP-670 (B1 — Service Catalog), GAP-671 (B2 — Compliance Map), GAP-672 (B3 — Database Map), GAP-674 (B5 — README golden-path)

---

## Problem

Wave 99B outside-in External Benchmark agent surfaced top-1 finding: "KiteHub MISSING C4 industry pillar — không có diagram cho new dev onboarding 'system mental model' check". 3 existing arch docs (`kitehub-architecture.md`, `kiteclass-architecture.md`, `multi-tenant-architecture.md` — Wave 96 PR2) cover per-product + per-concern deep-dives nhưng KHÔNG có cross-product L1 (system in environment) hoặc L2 (deployable unit overview).

Result: new dev / SRE on-call / tech lead reviewer phải đọc 3+ files để reconstruct mental model = friction cao + onboarding time tăng. Industry standard (C4 model by Simon Brown) addresses exactly này class.

## Root Cause

Pre-Wave-99B: arch folder evolved organically per-feature/per-product. No explicit cross-cutting "where does the system fit in the world" doc. Benchmark agent identified pattern: 9/9 modern SaaS reference architectures (Atlassian, GitLab, Linear, Notion, etc.) ship C4 L1+L2 diagrams cho onboarding. KiteHub absent.

Trigger: Wave 99B 3-agent outside-in audit (Persona + External Benchmark + Failure-Mode Matrix) convergent recommendation: ship C4 L1+L2 only (defer L3+L4 to docstrings per Benchmark anti-pattern "over-doc maintenance waste").

## Proposed Fix

Ship `documents/02-architecture/c4-context-container.md` với:

1. **L1 System Context diagram** (Mermaid `flowchart TB`)
   - 8 actors: P1/P2/P3/P5 + Anonymous Prospect (Vy) + Platform Admin (Mai) + Student + Parent
   - 6 external systems: Resend + AWS SES + VietQR + Zalo OA + Cloudflare + Statuspage
   - Brief narrative per actor + per external system

2. **L2 Container diagram** (Mermaid `flowchart TB` with subgraph clusters)
   - Frontend Cluster: kitehub-frontend + kiteclass-frontend (Next.js, EC2 self-host)
   - Gateway Cluster: kite-gateway (Spring Cloud Gateway, port 9000)
   - Service Cluster: 6 KiteHub services (subscription/branding/email/admin/platform/other) + kiteclass-core
   - Shared Infra Cluster: kite-postgres + kite-redis + kite-rabbitmq + kite-minio
   - Brief narrative per cluster explaining responsibility

3. **Explicit L3 + L4 defer rationale** (per Benchmark Wave 99B recommendation)
   - L3 Component → lives in per-service arch docs + ADRs + threat models
   - L4 Code-level → lives in Java javadoc + TypeScript types + OpenAPI specs
   - When L3+L4 diagrams justified (rare edge cases listed)

4. **Mermaid syntax** per `diagram-format-selection.md` v1.0.0 §2.2 row "Architecture (box + arrow)" — Mermaid `flowchart TB` default (PlantUML C4 defer per "Mermaid default + GitHub native render")

## Acceptance Criteria

- [x] `documents/02-architecture/c4-context-container.md` NEW file created với frontmatter (audience: dev, last-reviewed: 2026-05-19, wave: 99b, gaps: [GAP-673])
- [x] L1 System Context Mermaid diagram: 8 actors + 6 external systems + relationships labeled (HTTPS / HTTP POST / SDK / etc.)
- [x] L2 Container Mermaid diagram: 2 frontend + 1 gateway + 7 services + 4 infra trong subgraph clusters
- [x] Brief narrative per actor + cluster
- [x] L3+L4 defer rationale section với pointers tới docstrings + ADRs + threat models
- [x] Source verified at-spawn: `kitehub/docker-compose.kitehub.yml` inspected cho container list + ports + dependencies
- [x] Personas verified at-spawn: `documents/02-architecture/design-system/dossier/01-personas.md` cited (Tier 1+2 personas)
- [x] Maintenance section (§4): when-to-update triggers + protocol
- [x] Related section (§5) cross-links sister bucket docs (B1/B2/B3/B5) + existing arch baseline (Wave 96 PR2)
- [x] Mermaid render verified locally via syntax check (GitHub native render verify post-merge per `output-review-mandate.md` §3 row "HTML/JSX prototypes" integration smoke pattern adapted to Mermaid)

## Verification

- **State-check (pre-write per `audit-to-gap-pipeline.md` Step 2.5):**
  - `kitehub/docker-compose.kitehub.yml` Tier 1 source verified at spawn — 6 KiteHub services + 2 frontends + 4 infra confirmed
  - `documents/02-architecture/design-system/dossier/01-personas.md` Tier 1 source — 8 personas (P1/P2/P3/P5 + Vy + Mai + Student + Parent) verified
  - Existing arch baseline (Wave 96 PR2 3 reports) cross-referenced — no duplication risk
- **Frontmatter:** `last-reviewed: 2026-05-19` matches session date per `session-currentdate-check.md` (no forward-date)
- **Filename:** Tier 5 plain slug (`c4-context-container.md`) per `docs-filename-prefix-convention.md` §2.5 — descriptive 3-word slug
- **Folder discipline:** placed in `documents/02-architecture/` root per `docs-subfolder-maturity.md` §2 (NOT creating `c4/` subdir for 1 file)
- **Volume cap:** post-archive sweep (B6 GAP-668) root-level count 16→10; +1 NEW (this file) = 11/50 cap per `docs-folder-volume-budget.md` §2 OK
- **Diagram format:** Mermaid per `diagram-format-selection.md` v1.0.0 §2.2 Architecture row mandate
- **Language:** Vietnamese narrative + English technical token (HTTPS, JWT, RLS, Next.js, Spring Cloud Gateway) per `dev-readable-doc-language.md` §4 mixed-language rule

## Log

- **2026-05-19:** GAP filed + closed same session (Wave 99B B4 spawn agent). C4 L1+L2 doc shipped per outside-in Benchmark recommendation. L3+L4 explicitly deferred per Benchmark anti-pattern guard. Sister bucket B1 (Service Catalog) owns full dependency graph; B3 (Database Map) owns entity catalog; B5 (README golden-path) will cross-link this doc post-merge. Foundation B6 + B0 already shipped (volume cap compliant + Last-Reviewed backfill). Status flip DONE per `gap-done-discipline.md` §2 — all AC checked, no banned phrases ("deferred to manual" only used for L3+L4 scope which is explicit Benchmark recommendation not silent deferral).
