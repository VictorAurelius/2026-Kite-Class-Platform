---
title: 02-architecture — Technical Architecture
audience: mixed
created: 2026-04-18
last-reviewed: 2026-05-19
status: living
---

# 02-architecture — Technical Architecture

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../.claude/rules/docs-folder-structure.md)

Tài liệu technical architecture — system design, tương tác component, data flow, cross-cutting concern, và Architectural Decision Record (ADR). Chứa "what + how" của system ở tầng architecture; "why" (rationale của decision) thuộc [`adr/`](adr/).

**Audience:** Backend engineer, Frontend engineer, SRE/DevOps, Tech Lead, Architect. Secondary: thesis reviewer, contributor mới onboarding.

> 📅 **Last reviewed:** **2026-05-19** · Wave 99B B5 — Onboarding Tour orchestrator landing

---

## 🚀 Thứ tự đọc — Golden-Path Onboarding Tour

Đọc theo thứ tự 7 bước này để build mental model architecture của Kite Platform end-to-end (~60-90 phút):

| # | Bước | File | Bạn sẽ học |
|---|---|---|---|
| 1 | **System boundary (L1) + Container topology (L2)** | [`c4-context-container.md`](c4-context-container.md) | 8 persona actor + 6 hệ thống ngoài (Resend/SES/VietQR/Zalo/CF/Statuspage); 2 FE + 1 gateway + 7 service + 4 infra subgraph |
| 2 | **Service catalog + Dependency graph + Auth flow** | [`service-catalog-and-auth-flow.md`](service-catalog-and-auth-flow.md) | 18-service catalog (8 BE + 2 FE + 8 infra); Mermaid dependency graph ~37 edge; auth sequence ~24 bước; matrix role-guard 10-controller |
| 3 | **Database entity catalog + FK graph + RLS map + lịch sử Flyway** | [`database-architecture-map.md`](database-architecture-map.md) | 91 entity (32 kh-subscription + 59 kc-core); RLS coverage 51/91; lịch sử migration 114 V-file; top-10 driver row Phase 1 BETA |
| 4 | **Strategy multi-tenant + cô lập DB-level + triển khai RLS** | [`multi-tenant-architecture.md`](multi-tenant-architecture.md) | Propagation tenant_id qua 4 cluster RLS; lifecycle tenant (trial → subscription → off-boarding); routing subdomain per-tenant |
| 5 | **Compliance × Code Map + SLO Registry + NFR + Risk Register** | [`compliance-control-map.md`](compliance-control-map.md) | 19 row compliance (PDPL Art 7 + Luật ANM + ISO27001); SLO registry 11 service + 5 composite SLO platform-wide; 35+ row NFR; Risk Register 5 row |
| 6 | **Why-decision — ADR index** | [`adr/README.md`](adr/README.md) | 31 ADR (MADR format): K12 data model, role hierarchy, instance lifecycle, AWS Singapore Free Tier, FE self-host EC2, kiteclass-gateway removal |
| 7 | **Threat model per domain** | [`threat-models/`](threat-models/) | Threat model per-domain — phân tích STRIDE cho auth, payment, AI branding, tenant isolation |
| 8 | **Tenant → Domain → Landing end-to-end** | [`tenant-domain-landing-architecture.md`](tenant-domain-landing-architecture.md) | Chuỗi domain → gateway resolve (subdomain + custom domain) → core RLS → FE render landing; trạng thái implement vs gap (GAP-811/812/813/814); ops note Redis cache + 1-tenant-per-deploy |

**Tổng thời gian đọc:** ~60-90 phút (tuỳ persona — xem Per-Persona Reading List bên dưới). Sau khi đọc xong 8 bước, bạn có thể trace 1 user request end-to-end qua mọi tầng architecture.

---

## 🔍 Trace One Request — End-to-End Tutorial

Walk-through cụ thể: **"Anonymous prospect submit beta-access form trên `kitehub.me/request-beta` → DB row `beta_request` được create với status PENDING + email confirm gửi tới user"**

Mỗi tầng architecture có file để đọc:

| Layer | Chuyện gì xảy ra | Đọc cái này |
|---|---|---|
| **1. Browser (FE)** | User mở `https://kitehub.me/request-beta` → React component `RequestBetaForm` validate input (email, fullName, organizationName, organizationType, message) | [`c4-context-container.md`](c4-context-container.md) L2 — định vị container `kitehub-frontend` |
| **2. CDN → EC2 self-host** | Cloudflare proxy (`A record → 54.179.70.37`) serve static FE asset; submit form → `POST /api/platform/auth/beta-signup/validate` qua api.kitehub.me | [`adr/ADR-031-fe-self-host-aws-ec2.md`](adr/) — quyết định FE hosting; [`ssl-automation.md`](ssl-automation.md) — flow TLS cert |
| **3. Gateway** | `kitehub-gateway` route `/api/platform/auth/beta-signup/*` → service `kitehub-subscription` (port 4710); JWT propagation per [`adr/ADR-021-gateway-jwt-propagation.md`](adr/) | [`service-catalog-and-auth-flow.md`](service-catalog-and-auth-flow.md) §Gateway routing |
| **4. Service** | `BetaSignupController.submitRequest()` → `BetaSignupService.create()` validate business rule (tối đa 3 request mỗi email trong 7 ngày per `business/subscription/rules.md`) → emit event `BetaRequestCreated` | [`service-catalog-and-auth-flow.md`](service-catalog-and-auth-flow.md) §kitehub-subscription |
| **5. Database** | INSERT vào table `beta_request` (RLS bypass — endpoint public anonymous; row chỉ admin thấy được qua service role) với status='PENDING' + entry `audit_log` | [`database-architecture-map.md`](database-architecture-map.md) §kh-subscription entity (catalog 32 entity) + §RLS map (coverage 51/91) |
| **6. Async — Email** | RabbitMQ outbox dispatcher pick event `BetaRequestCreated` → service kitehub-email → SES `kite-noreply@kitehub.me` template `beta-request-confirmation` → inbox tenant | [`email-architecture.md`](email-architecture.md) — topology dual-vendor SES + Resend + DKIM signing |
| **7. Compliance + Audit** | PDPL Art 7 (lawful processing): lưu consent flag; row `admin_audit_log` immutable (V60 migration); retention 7 năm tương đương GDPR per data-retention-policy | [`compliance-control-map.md`](compliance-control-map.md) §PDPL + [`data-retention-policy.md`](data-retention-policy.md) |
| **8. SLO + Risk** | Target P99 endpoint latency ≤500ms tracked per [`compliance-control-map.md`](compliance-control-map.md) §SLO Registry; failure mode = alert email queue depth (row R1 Risk Register) | [`compliance-control-map.md`](compliance-control-map.md) §Risk Register R1 |

**Hands-on follow-up:** sau khi đọc 8 layer trên, mở [`service-catalog-and-auth-flow.md`](service-catalog-and-auth-flow.md) §Auth Flow sequenceDiagram để xem 24-step authenticated tương đương (login + role-guard + RLS) — cùng pattern nhưng cho endpoint authenticated thay vì anonymous.

---

## 👥 Per-Persona Reading List

Khuyến nghị reading tuỳ vào role + mục tiêu onboarding:

### P1 — Backend Engineer (gia nhập team viết service Java)

**Mục tiêu:** hiểu boundary service + data model + viết được endpoint đầu tiên trong 1 tuần.

**Reading ưu tiên (~3-4 giờ):**
1. [`service-catalog-and-auth-flow.md`](service-catalog-and-auth-flow.md) — catalog 18 service (bắt buộc; biết service nào owns cái gì)
2. [`database-architecture-map.md`](database-architecture-map.md) — catalog 91 entity + FK + RLS (bắt buộc; biết table nào thuộc đâu)
3. [`multi-tenant-architecture.md`](multi-tenant-architecture.md) — pattern propagation tenant_id (bắt buộc; mọi query cần RLS context)
4. [`adr/README.md`](adr/README.md) — skim 31 ADR (bắt buộc; hiểu decision đã có trước)
5. [`kitehub-architecture.md`](kitehub-architecture.md) — specific cho KiteHub SaaS platform
6. [`kiteclass-architecture.md`](kiteclass-architecture.md) — specific cho KiteClass tenant platform

**Skip lần đọc đầu:** UI design system, threat model (revisit Week 2+)

### P2 — Frontend Engineer (gia nhập team viết React/Next.js)

**Mục tiêu:** hiểu surface API gateway + routing subdomain tenant + auth flow trong 1 tuần.

**Reading ưu tiên (~2-3 giờ):**
1. [`c4-context-container.md`](c4-context-container.md) L1+L2 — boundary hệ thống + topology container FE (bắt buộc)
2. [`service-catalog-and-auth-flow.md`](service-catalog-and-auth-flow.md) §Gateway routing + §Auth flow sequence (bắt buộc; biết endpoint contract + lifecycle JWT)
3. [`domain-management.md`](domain-management.md) — pattern DNS + tenant subdomain (bắt buộc; biết strategy URL)
4. [`design-system/dossier/01-personas.md`](design-system/dossier/01-personas.md) — catalog persona UI (P1/P2/P3 + Anonymous Vy + Platform Admin Mai)
5. [`design-system/`](design-system/) UI kit + dossier — reference design system

**Skip lần đọc đầu:** Service backend internal, threat model, ADR (revisit Week 2+)

### P3 — SRE / DevOps Engineer (gia nhập team vận hành Phase 1 BETA)

**Mục tiêu:** hiểu target SLO + pipeline deployment + incident response trong 1 tuần.

**Reading ưu tiên (~3-4 giờ):**
1. [`compliance-control-map.md`](compliance-control-map.md) §SLO Registry + §NFR + §Risk Register (bắt buộc; biết target + gap measurement)
2. [`deployment-strategy.md`](deployment-strategy.md) — 5 nguyên tắc + matrix env (bắt buộc; hiểu philosophy deploy)
3. [`adr/ADR-025-aws-singapore-free-tier.md`](adr/) + [`adr/ADR-031-fe-self-host-aws-ec2.md`](adr/) — quyết định topology AWS (bắt buộc)
4. [`ssl-automation.md`](ssl-automation.md) — Let's Encrypt wildcard + cadence renewal cert
5. [`env-vars-registry.md`](env-vars-registry.md) — config env production (bắt buộc; nguồn dữ liệu chính thức cho mọi env var)
6. [`../05-guides/operations/`](../05-guides/operations/) — runbook operation (incident response, secrets rotation, restore drill)

**Skip lần đọc đầu:** Business logic domain, UI design, ADR không liên quan ops (revisit khi on-call)

### P4 — Tech Lead / Architect (gia nhập team dẫn dắt decision architecture)

**Mục tiêu:** view cross-cutting toàn diện + năng lực đóng góp ADR trong 2 tuần.

**Reading ưu tiên (~6-8 giờ, full sweep):**
1. **Cả 7 bước của Reading Order Tour ở trên** — mental model end-to-end
2. [`adr/`](adr/) — đọc TẤT CẢ 31 ADR cover-to-cover (rationale + alternative + consequence)
3. [`threat-models/`](threat-models/) — toàn bộ threat model per-domain (phân tích STRIDE)
4. [`../03-planning/roadmap/release-1-plan-2026.md`](../03-planning/roadmap/release-1-plan-2026.md) — strategy Release 1 Phase 1+2+3
5. [`../04-quality/audits/`](../04-quality/audits/) — audit report gần đây (Quality 90/110 + Security 93/100 + Ops 77/100 + Performance 86/100)
6. [`../../.claude/rules/`](../../.claude/rules/) — 70 rule governance (skim tier CRITICAL + MANDATORY)

**Không skip:** Tech Lead cần full picture.

### Anonymous / Thesis Reviewer (duyệt repo lần đầu)

Bắt đầu với [`c4-context-container.md`](c4-context-container.md) L1 (system boundary) → overview [`kitehub-architecture.md`](kitehub-architecture.md) → deep-dive tuỳ chọn theo curiosity.

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

## Quy tắc đặt file

- ✅ **Thuộc đây:**
  - System architecture (cách service tương tác)
  - Cross-cutting concern (SSL, email, backup, retention, domain)
  - Catalog design pattern (pattern áp dụng per feature)
  - Quyết định technology stack + topology component

- ✅ **Thuộc [`adr/`](adr/):**
  - Why-decision với các alternative đã cân nhắc (MADR format)
  - Ví dụ: "Why RabbitMQ over Spring Batch", "Why Helm over plain K8s manifests"

- ❌ **KHÔNG thuộc đây:**
  - Runbook operation → [`documents/05-guides/`](../05-guides/) (cách operate, không phải cách design)
  - Plan implementation per wave → [`documents/03-planning/waves/`](../03-planning/waves/)
  - Business rule per-domain → [`documents/01-business/`](../01-business/)
  - Source diagram → [`documents/06-diagrams/`](../06-diagrams/) (PlantUML, rendered PNG)

- Naming: `kebab-case.md`, ADR `ADR-NNN-kebab-title.md` (zero-padded 3-digit)

---

## ADR Process

`adr/` chứa 31 ADR (Michael Nygard format). Index: [`adr/README.md`](adr/README.md). CSV: [`adr/adrs-index.csv`](adr/adrs-index.csv). Template: [`adr/_TEMPLATE.md`](adr/_TEMPLATE.md).

**Status:** ADR 001-013 ship 2026-04-14 (initial architecture sweep). ADR-014 (Async Jobs Queue over Batch) + ADR-015 (AWS Agent Plugins defer) ship 2026-04-18. Gần đây: ADR-025 AWS Singapore Free Tier, ADR-028 chấp nhận scale Phase 1 BETA, ADR-031 FE self-host AWS EC2, ADR-032 kiteclass-gateway removal.

Mọi architectural decision với ≥2 option cân nhắc PHẢI có ADR mới.

---

## Archive Policy

Move sang `documents/07-archived/architecture-YYYY-QN/` khi:
- Architecture superseded (vd. AI Branding v2 → v3) — giữ cả 2 cho tới khi v3 merge, sau đó archive v2
- Component removed (vd. service decommissioned — xem kiteclass-gateway per ADR-032)
- Audit snapshot >180 ngày tuổi (file `living-docs-audit-*.md`)

**Archive batch gần đây (Wave 99B B6, 2026-05-19):** 6 file stale/superseded move sang [`07-archived/architecture-2026-Q2/`](../07-archived/architecture-2026-Q2/) — `living-docs-audit-2026-04` + `ai-branding-v2-redesign` + `ai-branding-design-patterns` + `backup-strategy` + `docker-platform-architecture` + `email-lifecycle`. Count root-level 16 → 10 (compliant với volume cap 50 per `docs-folder-volume-budget.md`).

ADR KHÔNG BAO GIỜ archive — append `superseded_by:` trong frontmatter, giữ in place.

---

## Related

- **Rule:** [`.claude/rules/design-patterns.md`](../../.claude/rules/design-patterns.md) enforce pattern trong code; folder này document NƠI pattern áp dụng
- **Rule:** [`.claude/rules/diagram-format-selection.md`](../../.claude/rules/diagram-format-selection.md) — Mermaid default cho architecture diagram (GitHub native render)
- **Diagram:** [`documents/06-diagrams/`](../06-diagrams/) source PlantUML cho visualization được reference ở đây
- **Quality audit:** [`documents/04-quality/audits/`](../04-quality/audits/) — report Quality /110 + Security /100 + Performance /100 + Ops /100 tracking sức khoẻ architecture
- **Planning:** [`documents/03-planning/`](../03-planning/) — wave plan + roadmap; wave hiện tại: 99B (orchestrator này)
- **GAP-046** — design pattern áp dụng có hệ thống
- **GAP-102** — ADR kickoff (populate `adr/`)
- **Gap gốc Wave 99B B1-B5:** GAP-670 (B1 Service Catalog) · GAP-671 (B2 Compliance Map) · GAP-672 (B3 Database Map) · GAP-673 (B4 C4 Diagram) · GAP-674 (B5 Onboarding Tour)
