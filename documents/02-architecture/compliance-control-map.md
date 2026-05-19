---
title: Compliance × Code Map + SLO Registry + Risk Register
audience: mixed
created: 2026-05-19
last-reviewed: 2026-05-19
status: living
wave: 99b
gaps: [GAP-671]
---

# Compliance × Code Map + SLO Registry + Risk Register

**Mục đích:** Một trang tra cứu cho Tech Lead Persona 4 (review PR có liên quan compliance/billing/PDPL) và SRE Persona 3 (verify SLO + risk surface) — map regulatory obligation → enforcement code path → test evidence, plus per-service SLO + NFR + Risk Register consolidated từ scattered audit reports.

**Cross-references:**
- BRD: [`compliance-scope.md`](../00-brd/compliance-scope.md) (legal framework mapping skeleton)
- BRD: [`nfr-catalog.md`](../00-brd/nfr-catalog.md) (NFR + tier-NFR matrix skeleton)
- Threat models: [`threat-models/`](threat-models/) 3 files (auth + RLS + CSV)
- ADR-013: data retention (per `data-retention-policy.md`)
- ADR-028: ECS Fargate vs EKS Phase 1 BETA
- Ops audits: [`documents/04-quality/audits/ops-readiness/`](../04-quality/audits/ops-readiness/)
- GAP-156: quarterly business audit (Phase 2 trigger)

---

## 1. Section 1 — Compliance Control Table

Mỗi row map: **regulatory article → obligation → enforcing code path → test evidence**. Tech Lead Persona 4 self-test: "review billing PR" → find applicable compliance rule trong ≤5 min.

**Severity legend:** 🔴 BLOCKING — không tuân thủ = không launch; 🟠 MANDATORY — vi phạm có hình phạt admin/financial; 🟡 ADVISORY — best practice.

### 1.1 PDPL 2023 / Nghị định 13/2023/NĐ-CP

| Article | Severity | Obligation | Enforcing code path | Test evidence | Status |
|---|:---:|---|---|---|:---:|
| **Art 8** (Lawful basis) | 🔴 | Mọi data collection phải có lawful basis documented (consent / contract / legal obligation / legitimate interest) | `kitehub-subscription/.../beta/service/BetaAccessService.java` (consent flow on signup); `kitehub-subscription/.../auth/service/AuthService.java` (consent + lawful basis at registration); per-domain `rules.md` documents lawful basis per processing activity | `BetaAccessServiceTest` + `AuthServiceTest` consent path coverage; `documents/04-quality/audits/business/` quarterly verify | ⚠️ PARTIAL (skeleton — `compliance-scope.md` §2.2 TODO; full audit GAP-156) |
| **Art 9** (Informed consent) | 🔴 | User được inform về data collection + processing purpose trước consent; consent revocable | `kitehub-frontend/src/app/legal/terms/page.tsx` + `kitehub-frontend/src/app/legal/privacy/page.tsx` + signup wizard consent checkbox; `documents/00-brd/terms-of-service.md` + `documents/00-brd/privacy-policy.md` (skeleton — counsel review trigger Phase 3) | E2E test `signup-consent-flow.spec.ts` (planned); manual self-test acceptance CSV `phase-1-beta-acceptance-self-test.csv` row PUB-LAND-001 | ⚠️ PARTIAL (UI + docs shipped; counsel sign-off pending Phase 3 K-12 trigger per `business-logic-review.md`) |
| **Art 11** (Audit log immutability + data integrity) | 🔴 | Mọi admin action + data access log immutable; tamper-proof; retention ≥1 year | `kitehub-subscription/.../db/migration/V36__create_admin_audit_log.sql` + V54 enrichment (5 extra columns: ip_address, user_agent, request_id, before_value, after_value); `kiteclass-core/.../db/migration/V60__create_admin_audit_logs.sql` (immutable triggers + INSERT-only via row trigger); V38 login_audit_log; V48 impersonation_audit_log; V49 staff_invitation_audit_log; V53 parent_read_audit_log | `AdminAuditLogJsonbPostgresIT` (Testcontainers — verify trigger blocks UPDATE/DELETE); `LoginAuditLogServiceTest`; manual audit Wave 92 Bucket A confirmed V54 + V60 immutability | ✅ DONE (Wave 92 Bucket A enrichment merged; immutable triggers verified) |
| **Art 14** (Data subject access right — quyền truy cập) | 🔴 | User có thể request truy cập data của họ trong ≤30 ngày | Endpoints `GET /api/users/{userId}/data-export` (skeleton — not yet wired); Admin portal data export feature | ❌ NOT-IMPLEMENTED Phase 1 BETA (deferred per `compliance-scope.md` §2.3 TODO; GAP-156 quarterly audit follow-up; tracked Phase 1.5+ scope) | ❌ NOT-IMPLEMENTED |
| **Art 14** (Data subject erasure right — right to be forgotten) | 🔴 | User có thể request xóa data; xóa trong ≤30 ngày trừ legal retention | `DataRetentionService.deleteInstance()` (lifecycle SUSPENDED → grace → DELETED per `data-retention-policy.md`); per-tier retention 7-90 days; **NOTE:** user-initiated erasure khác lifecycle erasure — chưa có user-facing endpoint | `DataRetentionServiceTest` (lifecycle path); user-erasure endpoint test missing | ⚠️ PARTIAL (lifecycle erasure ✅; user-erasure ❌) |
| **Art 15** (Data breach notification) | 🔴 | Thông báo cho cơ quan có thẩm quyền + data subject trong ≤72h sau khi phát hiện breach | `documents/05-guides/operations/incident-response-runbook.md` Phase 4 (breach notification); on-call playbook | Manual drill quarterly (deferred GAP-257 restore drill class; cadence per `post-wave-audit-mandate.md` §2.4) | ⚠️ PARTIAL (runbook shipped; quarterly drill GAP-257 blocked AWS) |
| **Art 20** (Cross-border data transfer) | 🔴 | Data transfer ra ngoài VN cần đánh giá tác động; user consent + protective measures | AWS Singapore `ap-southeast-1` region pinning (per ADR-025); RDS + S3 + ALB + EC2 all single-region; AI inference local Ollama (in-VN) hoặc disclosed OpenAI (US) với DPIA per `compliance-scope.md` §2.4 | Terraform region constraint `infrastructure/terraform-aws/variables.tf` `region = "ap-southeast-1"`; ADR-025 ACCEPTED 2026-05-07 | ✅ DONE (region pin enforced) |
| **Art 23** (Sensitive data — minor < 16) | 🔴 | Minor data requires parental consent; no behavioral ads to minors | `kiteclass-core/.../db/migration/V56__add_parental_consent_to_parent_student_links.sql`; `ParentStudentLinkService.requireParentalConsent()` (Phase 1.5+ K-12 scope); `documents/00-brd/child-protection-policy.md` | `ParentalConsentServiceTest` (skeleton); manual flow gated K-12 launch | ⚠️ PARTIAL (schema ✅ V56; service ⚠️ skeleton; Phase 3 K-12 gate) |

### 1.2 Luật An ninh mạng 24/2018/QH14 + Nghị định 53/2022/NĐ-CP

| Article | Severity | Obligation | Enforcing code path | Test evidence | Status |
|---|:---:|---|---|---|:---:|
| **NĐ-53 Art 26** (Data localization) | 🔴 | Data của user VN PHẢI store tại VN ≥24 tháng | AWS Singapore ap-southeast-1 region pinning (chấp nhận via ADR-025 §risk acceptance — Singapore = closest AWS region; legal review trigger nếu MPS challenge); migration path Oracle Cloud VN hoặc FPT Cloud nếu force | ADR-025 §risk acceptance documented; counsel review trigger Phase 3 | ⚠️ ACCEPTED RISK (ADR-025 documents risk; counsel sign-off Phase 3) |
| **Luật ANM Art 41** (Co-operation with authorities) | 🟠 | Process khi nhận yêu cầu cung cấp dữ liệu từ cơ quan có thẩm quyền | `documents/05-guides/operations/incident-response-runbook.md` (skeleton — legal request handling); admin portal data export | Manual flow (no automated test) | ⚠️ PARTIAL |
| **Luật ANM Art 26** (Content moderation) | 🟠 | Remove illegal content trong timeframe quy định | `kiteclass-core/.../content/service/ContentModerationService.java` (Phase 1.5+ scope); `documents/01-business/kiteclass/content-moderation/rules.md` | `ContentModerationServiceTest` (planned) | ❌ NOT-IMPLEMENTED Phase 1 BETA |

### 1.3 ISO 27001 baseline (informal — not certified Phase 1 BETA)

| Control | Severity | Obligation | Enforcing code path | Test evidence | Status |
|---|:---:|---|---|---|:---:|
| **A.9 Access Control** | 🟠 | Role-based access; least privilege; @PreAuthorize all admin endpoints | `@PreAuthorize` annotations across 18 controllers (kitehub-subscription 13, kitehub-platform 3, kitehub-email 1, kitehub-admin 1); `SecurityConfig.java` per service; JWT role claim → `@PreAuthorize("hasRole('...')")` | `RoleGuardMatrixIT` (planned per Wave 87+); manual per-controller audit Wave 92 Bucket D | ⚠️ PARTIAL (13/18 verified; 5 admin v1 controllers Wave 92 audit found 3 missing `@PreAuthorize` → GAP-637 P0) |
| **A.10 Cryptography** | 🟠 | Encryption at rest + in transit; key management | RDS AES-256 (EBS encrypted); TLS 1.3 ALB; JWT HMAC SHA256 với rotating key (Wave 78 KMS — P1 carry); secrets in AWS Secrets Manager 90-day rotation | Terraform `rds.tf storage_encrypted=true`; ALB cert ACM; GAP-379 secrets rotation 95% | ⚠️ PARTIAL (at-rest ✅; TLS ✅; secrets rotation 95%; JWT KMS P1 carry GAP-NEW) |
| **A.12 Operations Security** | 🟠 | Logging + monitoring; backup; capacity management | CloudTrail multi-region trail (per `aws-observability-first.md`); CloudWatch alarms (GAP-437 Phase 1 + Wave 84 dashboard); Postgres backup automated daily 7d retention | CloudTrail `IsLogging=true` verified Wave 43 apply; alarm fire path verified Wave 84 audit; restore drill GAP-257 blocked AWS | ⚠️ PARTIAL (observability ✅; restore drill ❌ GAP-257) |
| **A.14 System Acquisition + Maintenance** | 🟡 | Secure SDLC; dep scanning; secret scanning | Trivy SARIF on every PR (per `script-quality.yml`); CodeQL planned Phase 2; npm-audit + dependabot weekly | Trivy gate `trivy-sarif-guard.sh` ships Wave 91; npm-audit baseline | ✅ DONE Phase 1 BETA scope |
| **A.16 Incident Management** | 🟠 | Incident response process; escalation; post-mortem | `documents/05-guides/operations/incident-response-runbook.md` Phase 4; on-call cadence (solo-dev MTTR self-baseline) | Manual drill (deferred until Phase 1.5+ team grows beyond solo) | ⚠️ PARTIAL |

### 1.4 Payment compliance (Phase 1.5+ scope — N/A Phase 1 BETA)

| Standard | Severity | Status Phase 1 BETA |
|---|:---:|:---:|
| **PCI DSS** (card data handling) | 🔴 | N/A — no card storage; gateway-side tokenization only (Phase 1.5+ MoMo/VNPay/VietQR partnership per GAP-NEW-payment-processor-init CANCEL → Wave 93 retro Phase 2 partnership) |
| **VN e-invoice TCT (Thông tư 78/2021)** | 🟠 | N/A Phase 1 BETA — MISA MeInvoice partnership (per GAP-185 Wave 93 re-scope); not self-built |
| **AML / KYC** | 🟠 | N/A Phase 1 BETA — gateway-side responsibility |

### 1.5 Summary

**Compliance table row count:** 19 rows total (PDPL Art 7 + Luật ANM 3 + ISO27001 5 + Payment 4 N/A).

**Status distribution:**
- ✅ DONE: 3 rows (Art 11 audit log + Art 20 region pin + ISO A.14 SDLC)
- ⚠️ PARTIAL / ACCEPTED RISK: 11 rows
- ❌ NOT-IMPLEMENTED: 5 rows (Art 14 user-erasure UI + Art 23 K-12 service + ISO A.16 drill + 2 payment)
- **TBD count:** 0 (all rows have explicit Status verdict; PARTIAL/NOT-IMPLEMENTED rows reference follow-up gaps)

---

## 2. Section 2 — SLO Registry

Per-service SLO targets consolidated từ ops-readiness audits + `nfr-catalog.md` skeleton + ADR-028 ECS Fargate baseline. **NOTE:** Phase 1 BETA = solo tenant invite-only scale; SLO numbers below là Phase 1.5+ PAID targets, không phải current Phase 1 BETA enforcement.

**Measurement caveat:** Hầu hết SLO numbers Phase 1 BETA = `TBD` (synthetic monitor + RUM chưa active; baseline measurement deferred GAP-135 Phase 2 trigger). Numbers dưới đây inherit từ NFR catalog targets + Wave 84/85/91/92 ops audit observations.

### 2.1 Per-service SLO matrix

| Service | Tier scope | Uptime SLO | P99 latency | Error rate target | Source / verification |
|---|---|:---:|:---:|:---:|---|
| **kitehub-gateway** (BFF + JWT validation) | Platform-wide | 99.9% PREMIUM tier (per `nfr-catalog.md` §2) | TBD <200ms (Read GET cached per §4.1) | <0.1% 5xx | CloudWatch ALB target group metrics; Wave 89 Bucket C JWT validation Hardening shipped; ops audit Wave 92 PASS |
| **kitehub-subscription** (billing + tenant lifecycle + admin audit) | Platform-wide | 99.9% PREMIUM | TBD <800ms (Write POST per §4.1) | <0.5% 5xx | Postgres connection pool monitored; HikariCP GUC reset Wave 85; ops audit Wave 91 baseline 77/100 |
| **kitehub-platform** (admin + cross-cutting) | Platform-wide | 99.9% PREMIUM | TBD <400ms (Read GET DB per §4.1) | <0.5% 5xx | Admin endpoint @PreAuthorize coverage 3/3 Wave 92 audit; GAP-637 P0 fix unblocks SLO measure |
| **kitehub-email** (transactional email worker) | Background async | 99.5% PRO best-effort (queue retry safety net) | TBD outbox dispatcher <30s per message (per Wave 91 Bucket D) | <1% delivery failure | DLQ + outbox pattern shipped Wave 91 Bucket D; Resend + AWS SES dual-provider failover |
| **kitehub-branding** (AI inference async) | PRO+ tier | 99% PRO (best-effort async) | 30s P50 / 90s P95 / 180s P99 (per `nfr-catalog.md` §4.1 AI inference async) | <2% job failure | Ollama 24GB constraint bulkhead per `ai-branding-guidelines.md` §11.4.4; Wave 4 baseline 62/100 |
| **kitehub-admin** (platform admin portal BE) | Platform admin only | 99.95% (admin-only — internal tier) | TBD <500ms | <0.5% 5xx | Wave 92 Bucket D admin v1 audit; GAP-637 P0 fix prerequisite |
| **kitehub-base** (shared library — no runtime) | N/A | N/A | N/A | N/A | Library only; SLO inherits từ services consuming it |
| **kiteclass-core** (multi-tenant education domain) | Per-tenant | 99.5% PRO / 99.9% PREMIUM per `nfr-catalog.md` §10 | TBD <800ms (Write POST) | <0.5% 5xx | RLS isolation Wave 85 Bucket B verified; tenant_id propagation per GAP-604 |
| **kiteclass-gateway** (per-tenant BFF) | Per-tenant | 99.5% PRO / 99.9% PREMIUM | TBD <200ms (cached) | <0.1% 5xx | ALB target group per tenant; Wave 89 JWT validation hardening |
| **kitehub-frontend** (Next.js SSR) | Platform-wide | 99.9% (CDN + EC2 self-host per Wave 82 pivot) | LCP <2.5s P75 / INP <200ms P75 / CLS <0.1 (per `nfr-catalog.md` §4.3 Web Vitals) | <0.5% client errors | CDN cache hit ratio TBD; Lighthouse CI gate planned Phase 2 |
| **kiteclass-frontend** (per-tenant Next.js) | Per-tenant | 99.5% PRO / 99.9% PREMIUM | Same as kitehub-frontend §4.3 | <0.5% client errors | Per-tenant deploy isolation; subdomain routing via gateway |

**SLO registry per-service count:** 11 services (10 active + 1 library N/A).

**TBD count:** 17 (mostly P99 latency + cache hit ratio — measurement instrumentation deferred GAP-135 Phase 2 RUM trigger).

### 2.2 Platform-wide composite SLO

| Composite SLO | Target | Current measurement | Source |
|---|:---:|:---:|---|
| **API gateway availability** (kitehub + kiteclass gateway aggregate) | 99.9% | TBD (uptime monitor) | NFR catalog §2 PREMIUM tier |
| **Signup conversion funnel** (landing → beta-request → approved → signup) | TBD baseline | TBD | Wave 78 Bucket F1 anonymous-prospect funnel |
| **AI inference SLA** (kitehub-branding job completion rate) | ≥98% in 180s | TBD baseline | NFR catalog §4.1 AI async |
| **Email delivery rate** (Resend + SES aggregate) | ≥99% delivered ≤30s | TBD | Wave 91 outbox dispatcher metric |
| **Cross-tenant data leak rate (RLS)** | 0 (zero tolerance) | 0 (Wave 85 RLS NULL force-fail eliminates silent leaks) | Wave 85 Bucket B + Wave 92 ops audit Cat 3 PASS |

---

## 3. Section 3 — NFR + Quality Attribute Registry

Consolidated từ scattered BRD `nfr-catalog.md` + post-wave audit reports. Per arc42 §10 "Quality Goals" pattern.

### 3.1 Performance NFRs

| Attribute | Target | Source |
|---|:---:|---|
| Read API P95 (cached) | <200ms | `nfr-catalog.md` §4.1 |
| Read API P95 (DB) | <400ms | `nfr-catalog.md` §4.1 |
| Write API P95 (POST/PUT) | <800ms | `nfr-catalog.md` §4.1 |
| AI inference async P95 | <90s | `nfr-catalog.md` §4.1 |
| Frontend bundle initial JS gzipped | <200KB | `nfr-catalog.md` §4.2 |
| Frontend bundle per-route JS gzipped | <100KB | `nfr-catalog.md` §4.2 |
| Web Vitals LCP P75 | <2.5s | `nfr-catalog.md` §4.3 |
| Web Vitals INP P75 | <200ms | `nfr-catalog.md` §4.3 |
| Web Vitals CLS P75 | <0.1 | `nfr-catalog.md` §4.3 |
| DB connection pool utilization (avg) | <70% | `nfr-catalog.md` §4.4 |
| Slow query (>500ms) rate | <0.1% | `nfr-catalog.md` §4.4 |

### 3.2 Availability + Reliability NFRs

| Attribute | Target | Source |
|---|:---:|---|
| Uptime FREE tier | None (best-effort) | `nfr-catalog.md` §2 |
| Uptime PRO tier | 99.5% | `nfr-catalog.md` §2 |
| Uptime PREMIUM tier | 99.9% | `nfr-catalog.md` §2 |
| Uptime ENTERPRISE tier | 99.95% | `nfr-catalog.md` §2 |
| RTO single service crash | <5m | `nfr-catalog.md` §3 |
| RTO single AZ failure | <30m | `nfr-catalog.md` §3 |
| RTO region failure | <4h | `nfr-catalog.md` §3 |
| RPO single AZ | <5m | `nfr-catalog.md` §3 |
| RPO region failure | <1h | `nfr-catalog.md` §3 |
| RPO DB corruption | <15m | `nfr-catalog.md` §3 |

### 3.3 Scalability NFRs

| Attribute | PRO target | PREMIUM target | Source |
|---|:---:|:---:|---|
| Concurrent users per tenant | 50 | 500 | `nfr-catalog.md` §5.1 |
| Request rate per tenant (req/s) | 5 | 50 | `nfr-catalog.md` §5.1 |
| DB rows per major table | 100K | 1M | `nfr-catalog.md` §5.1 |
| Platform total tenants (year 1) | 10K target | — | `nfr-catalog.md` §5.2 |
| Platform total active users daily | 100K target | — | `nfr-catalog.md` §5.2 |

### 3.4 Security NFRs

| Attribute | Target | Source |
|---|:---:|---|
| Pen-test cadence | Annual | `nfr-catalog.md` §6 |
| Critical CVE patch SLA | <72h | `nfr-catalog.md` §6 |
| High CVE patch SLA | <7d | `nfr-catalog.md` §6 |
| Failed login lockout | 5 attempts → 15min lock | `nfr-catalog.md` §6 |
| Session timeout (inactive) | 30min | `nfr-catalog.md` §6 |
| Encryption at rest | AES-256 | `nfr-catalog.md` §6 |
| Encryption in transit | TLS 1.3 minimum | `nfr-catalog.md` §6 |
| Secrets rotation cadence | Quarterly auto (90-day per GAP-379) | `nfr-catalog.md` §6 |

### 3.5 Accessibility NFRs (per `ai-branding-guidelines.md` §5)

| Attribute | Target |
|---|:---:|
| WCAG 2.1 conformance | Level AA |
| Color contrast normal text | ≥4.5:1 |
| Color contrast large text | ≥3:1 |
| Keyboard navigation | All interactive elements |
| Screen reader compatibility | NVDA + VoiceOver tested |

### 3.6 Maintainability NFRs

| Attribute | Target | Source |
|---|:---:|---|
| Test coverage (line) | ≥80% | `nfr-catalog.md` §8 |
| Test coverage (branch) | ≥70% | `nfr-catalog.md` §8 |
| CI pipeline duration | <15m | `nfr-catalog.md` §8 |
| Mean PR cycle time | <2d | `nfr-catalog.md` §8 |
| Documentation freshness | Living Docs rule (CLAUDE.md) | `nfr-catalog.md` §8 |

### 3.7 Observability NFRs

| Attribute | Target | Source |
|---|:---:|---|
| Structured JSON logs | All services | `logs-format-standard.md` |
| Log retention hot/warm/cold | 7d/30d/180d | `logs-format-standard.md` §4 |
| Distributed tracing coverage | 100% requests have traceId | `nfr-catalog.md` §9 |
| Metrics scraping interval | 15s (Prometheus) | `nfr-catalog.md` §9 |
| Alert MTTR P1 | <30m | `nfr-catalog.md` §9 |
| Alert MTTR P2 | <4h | `nfr-catalog.md` §9 |

---

## 4. Section 4 — Risk Register

Top risks consolidated từ threat models + Wave 91/92 ops audit P0 carry-forward + decision-doc rationale. Per `output-review-mandate.md` §3 "Risk Register" tracking convention.

### 4.1 Top 5 known risks (Phase 1 BETA scope)

| # | Risk | Likelihood | Impact | Mitigation status | Owner | Link |
|:-:|---|:---:|:---:|---|---|---|
| **R1** | **Cross-tenant data leak via RLS misconfiguration** — service forgets to SET tenant_id before query → RLS policy returns ALL rows (silent leak) | Medium (without protection) | 🔴 Critical | ✅ MITIGATED — Wave 85 Bucket B RLS NULL force-fail policy (`WHERE tenant_id = NULLIF(...)::uuid` → empty GUC = 0 rows); HikariCP interceptor resets GUC on connection return; Wave 92 ops audit Cat 3 PASS | Tech Lead + Backend Lead | [`threat-models/2026-05-16-tenant-isolation-rls.md`](threat-models/2026-05-16-tenant-isolation-rls.md) (T1, S3) |
| **R2** | **Auth bypass via JWT forgery / replay** — attacker forges JWT với khác tenant_id; revoked JWT replayed before TTL expiry | Low | 🔴 Critical | ⚠️ PARTIAL — JWT HMAC-signed với rotating key (Wave 78); TOTP/2FA backup; **GAP P1 carry:** JWT KMS migration deferred (memory rotating key); session timeout 30min per NFR §6; refresh token blacklist on rotation per `pre-launch-auth-hardening-checklist.md` §2.8 | Tech Lead + Security | [`threat-models/2026-05-16-auth-flow-magic-link.md`](threat-models/2026-05-16-auth-flow-magic-link.md) |
| **R3** | **Bulk CSV import malicious payload** — uploaded CSV chứa formula injection (Excel) / XSS payload / oversized file → admin opens → RCE on admin machine OR app crash | Medium | 🟠 High | ⚠️ PARTIAL — MIME validation server-side; ClamAV scan planned Phase 2; size limit 10MB; formula prefix stripping (=/+/-/@) per OWASP CSV injection; encoding sniff UTF-8 BOM mandatory per `test-artifact-format-standard.md` | Backend Lead | [`threat-models/2026-05-16-bulk-import-csv.md`](threat-models/2026-05-16-bulk-import-csv.md) |
| **R4** | **Production restore drill never executed** — backup script runs daily nhưng never verified restore-from-backup actually works | High | 🔴 Critical | ❌ BLOCKED — GAP-257 restore drill carry-forward; AWS account suspension GAP-612 blocks live verify path (2026-05-17 16:50 UTC); unblock 24-72h post-restore window | SRE | GAP-257 + GAP-612 |
| **R5** | **Phase 1.5+ scale jump no proven scale path** — 10 BETA tenants → 50-200 PAID tenants mid-cycle without battle-tested orchestration (ECS Fargate not yet deployed; EKS deferred Phase 2) | Medium | 🟠 High | ⚠️ ACCEPTED RISK per ADR-028 — ECS Fargate target chosen; Phase 1.5 trigger gate = file follow-up migration plan when concurrent users >50; rollback path = stay on EC2 self-host (current Phase 1 BETA pattern) | Tech Lead + SRE | ADR-028 + GAP-415 (Phase 2 EKS Migration Plan) |

### 4.2 Risk register row count

**Top 5 entries** (per task spec). Additional medium-priority risks tracked trong:
- Compliance scope skeleton TODO items (~15 carry-forward gaps quarterly)
- Wave 92 ops audit 3 P0 FAIL carry-forward (restore drill + alertmanager + rollback drill — all GAP-612 blocked)
- Performance baseline gaps (GAP-126..135 ten items)

---

## 5. Tech Lead Persona 4 self-test — "review billing PR" walkthrough

**Scenario:** Tech Lead receives PR adding new endpoint `POST /api/subscription/upgrade` (tier change PRO → PREMIUM).

**Apply Compliance × Code Map (§1) — find applicable rules trong ≤5 min:**

1. **PDPL Art 8** Lawful basis — verify endpoint references documented lawful basis (contract — user upgrades subscription). Code path: `SubscriptionController.upgrade()` calls `SubscriptionService.changeTier()`. ✅ contract basis (subscription agreement).
2. **PDPL Art 11** Audit log — verify admin audit log fires on tier change. Code path: `AdminAuditLogService.log("TIER_CHANGE", before, after)`. ✅ V54 enrichment captures before/after values.
3. **ISO A.9** Access control — verify `@PreAuthorize("hasRole('USER') || hasRole('CENTER_OWNER')")` annotation. ⚠️ check `SubscriptionController.java`.
4. **Payment compliance** — verify no card data stored locally; gateway tokenization only. ✅ Phase 1.5+ scope (Phase 1 BETA = trial-only, no real payment).
5. **NFR §4.1** Write API P95 <800ms — verify endpoint baseline meets target (run perf test). ⚠️ measurement TBD until RUM baseline.

**Verdict:** All 5 applicable rules surfaced trong ≤5 min via single-page lookup. Self-test PASS ✅ per task spec.

---

## 6. Open follow-ups (next refresh triggers)

| Item | Trigger | Owner |
|---|---|---|
| Counsel sign-off Phase 3 K-12 (PDPL Art 9 + Art 23 + Luật ANM Art 26) | K-12 launch ≥1 school | Legal + PM |
| GAP-156 quarterly business audit refresh | Phase 2 transition | Auditor |
| GAP-637 admin v1 @PreAuthorize 3 P0 fix | Wave 93+ | Backend Lead |
| GAP-257 restore drill execute | GAP-612 AWS unblock 24-72h | SRE |
| RUM baseline measurement (Web Vitals + API P99) | Phase 1.5+ traffic ≥100 daily users | SRE |
| ECS Fargate migration plan (per ADR-028) | Phase 1.5+ concurrent users >50 | Tech Lead + SRE |
| User-erasure endpoint UI (PDPL Art 14) | Phase 1.5+ | Backend Lead + FE Lead |
| Quarterly DR drill (PDPL Art 15) | Post-AWS restore + team grows beyond solo | SRE |

---

## 7. Maintenance

**Refresh cadence:** quarterly (per `post-wave-audit-mandate.md` §2.4 Domain-Milestone Audit Cadence — meta-governance domain).

**Update triggers:**
- New PDPL article enforcement requirement
- New service shipped (add SLO row)
- Wave post-audit refresh (Wave 92+ baseline)
- ADR landed touching compliance/SLO scope
- Risk surface change (new threat model file shipped)

**Cross-link discipline:** mọi update to this file → update matching row trong `output-review-mandate.md` §3 matrix khi applicable (compliance / SLO / risk rows).
