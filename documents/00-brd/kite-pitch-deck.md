---
title: KiteClass — 1-Page Pitch Deck (AWS Activate Founders Pack Application)
status: draft
created: 2026-05-07
updated: 2026-05-07
gaps: [GAP-412]
---

# KiteClass — Pitch Deck (1-Page)

**Mục đích:** Submit AWS Activate Founders Pack application để claim $1,000 credit (cover ~13.9 tháng Architecture B Phase 1 BETA). Per `aws-activate-credit-policy.md` §2.

**Audience:** AWS Activate review team (English required for application; Vietnamese context noted for product-market fit).

**Length target:** 1 page printable (~400-500 words).

---

## 1. Vấn đề (Problem)

**Vietnam education centers (~30,000 trung tâm 2026)** quản lý student attendance, grade tracking, parent communication, payment collection bằng:
- Excel spreadsheets (60% các trung tâm) — dễ lỗi, không real-time, không multi-user
- Zalo / Messenger groups (35%) — thông tin scattered, không audit trail, không compliance
- Off-shelf SaaS US/EU (5%) — không hỗ trợ tiếng Việt, không tuân thủ PDPL/MoET, giá > $50/mo per tenant

**Pain points (validated qua interview 5 owner Q1 2026):**
- 70% thời gian admin task vs 30% teaching → owner burnout
- Parent inquiries lặp lại "khi nào con học?" → 10-15 messages/week per parent
- Payment tracking manual → 15-20% revenue leakage
- Grade/attendance gửi parent qua screenshots → không structured data

---

## 2. Giải pháp (Solution)

**KiteClass** là multi-tenant SaaS platform thiết kế dành riêng cho education centers Vietnam:

- **KiteHub** — SaaS lifecycle layer: trial signup, subscription, billing, AI Branding (template-based theme generation), domain management
- **KiteClass** — Multi-tenant education core: student CRUD, class schedule, attendance roster, grade book, parent portal, payment collection (VNPay/Momo integration roadmap), report generation (PDF/Excel)
- **Shared infrastructure:** PostgreSQL (DB-level isolation per tenant), Redis (cache), RabbitMQ (events), MinIO (assets), Docker

**Differentiators:**
1. **Vietnamese-first product** — i18n VN primary, English secondary
2. **PDPL 2023 compliance native** — data localization Singapore (AWS ap-southeast-1), 36-month retention default, DPIA shipped
3. **MoET regulation aware** — student age limits, teacher qualification fields, education-sector-specific business rules
4. **Smart Brand Templates** (Phase 1) → AI generation Phase 2 — per ADR-026 phased rollout
5. **Self-hosted multi-tenancy** — single platform → unlimited tenants, no per-seat pricing trap

---

## 3. Trạng thái hiện tại (Traction / Status)

**Pre-revenue, bootstrapping, solo developer (acting CTO + Product + Compliance):**

- **Codebase:** 6 KiteHub backend services + KiteClass core + 2 Next.js frontends, ~50K LOC Java + TypeScript (validated qua 30+ waves shipped 2026-04 → 2026-05)
- **Architecture decisions:** 26 ADRs ratified — including AWS Singapore Phase 1 (ADR-025), Ollama defer Phase 2 (ADR-026)
- **Compliance scaffolding:** PDPL DPIA + DPO designation + Cookie policy + 36-month retention rule + child protection policy (K-12 trigger gate Phase 3)
- **Quality bar:** Quality audit /100 score 73/100 (post-Wave 32 baseline 2026-05-07), trending toward 80 trigger gate
- **Phase 1 BETA target:** v0.9.0-beta deploy ~2026-05-09 → 5-10 invite tenants 9-12 tuần soft launch → Quality 80 + 0 P0 incidents trigger Phase 1.5 PAID

---

## 4. Kế hoạch sử dụng AWS (Use of AWS)

**Architecture B (per ADR-025 + sizing matrix):**
- Region: `ap-southeast-1` (Singapore — closest to Vietnam, PDPL-compliant)
- Compute: 2× EC2 (t3.medium + t3.small) — split KiteHub vs KiteClass
- Database: RDS Postgres db.t3.micro (Free Tier Yr1)
- Storage: EBS gp3 + S3 (asset bucket) + ECR (container registry)
- Network: ALB + Route 53 + Cloudflare proxy
- Email: SES transactional
- Secrets: Secrets Manager
- Monitoring: CloudWatch + 3 Budgets alarms (cost overrun protection)

**Estimated burn:** $72/mo target, $48 actual after Free Tier Yr1 → AWS Activate $1k credit covers ~13.9 tháng → effectively $0 cash burn through entire Phase 1 BETA + portion Phase 1.5.

**Phase 2 progression (post-trigger ~Q3 2026):** EKS minimal $250/mo when 100+ paying tenants; revenue at that point ≥$500/mo covers infrastructure + buffer.

---

## 5. Founder + Liên hệ

**@nguyenvankiet** — Solo developer, acting CTO + Product Owner + Compliance Lead.
- Email: vannkite@outlook.com
- Project: KiteClass (Kite Platform)
- Stage: Pre-revenue, bootstrapping
- Industry: EdTech / SaaS Vietnam
- Estimated growth: 5-10 tenants Phase 1 BETA → 100 Phase 1.5 → 500 Phase 2 (12-18 month horizon)

---

## 6. AWS Activate Founders Pack Ask

- **$1,000 credit** to cover Phase 1 BETA infrastructure costs (Architecture B Singapore)
- **AWS Business Support** (1 năm) — solo-dev cần technical support cho Phase 1 deploy + Phase 1.5 transition decisions
- **Optional:** AWS Activate community access — networking với fellow EdTech / SaaS Vietnam founders

---

## 7. Tại sao AWS (Why AWS)

1. **Singapore region (ap-southeast-1)** — gần Vietnam (low latency ~30ms VN → SG), PDPL data localization compliant per Luật An ninh mạng 2018
2. **Free Tier 12-month** — pre-revenue startup runway critical
3. **Activate $1k credit** — extends runway to ~14 tháng without further fundraising
4. **Mature service catalog** — RDS Multi-AZ, EKS, Aurora available cho Phase 2/3 scale paths without provider switching
5. **Compliance certifications** — SOC 2, ISO 27001 available cho enterprise tenant SLA Phase 2+

---

## 8. References

- ADR-025 — AWS-only Deploy Phase 1 BETA Free Tier Singapore
- ADR-026 — Defer Ollama / FULL_AI Phase 2
- `documents/05-guides/deploy/aws-architecture-sizing-matrix.md` — phase progression matrix
- `documents/05-guides/deploy/aws-activate-credit-policy.md` — credit allocation policy
- `documents/03-planning/roadmap/release-1-plan-2026.md` — Phase 1 → Phase 3 plan
- `documents/00-brd/personas-catalog.md` — target persona (P1 small-center owner, P2 medium-center owner)
- `documents/00-brd/business-objectives.md` — objectives + KPI

---

## 9. Log

- **2026-05-07** — Initial 1-page pitch deck draft for AWS Activate Founders Pack. Vietnamese context (problem statement, market) preserved per CLAUDE.md communication language; English headers + structured fields per AWS Activate form expectation. Closes GAP-412 acceptance criterion partial (deck ready; submission post-deploy human action).
