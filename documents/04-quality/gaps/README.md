# Gaps Queue — Design & Implementation Issues

Hàng đợi các design gaps / implementation gaps được phát hiện. Mỗi gap = 1 file, được fix theo priority. Không fix ngay trong session phát hiện, tránh scope creep.

> **📋 Đang quá nhiều gaps?** Đọc **[ROADMAP.md](ROADMAP.md)** để xem epic grouping, dependencies, sprint plan thay vì flat list.

## Workflow

1. **Phát hiện** — gap được tạo file `GAP-XXX-title.md` với status `OPEN`
2. **Prioritize** — user review, assign priority (P0 blocker → P3 nice-to-have)
3. **Plan** — gap có thể trở thành: PR riêng, wave, hoặc task trong wave có sẵn
4. **Track** — update status trong file khi tiến triển
5. **Close** — khi merged, đổi status `DONE` + link PR

## Status Legend

| Status | Ý nghĩa |
|--------|---------|
| 🔵 OPEN | Đã document, chưa có plan |
| 🟡 PLANNED | Có PR/wave nhận xử lý |
| 🟠 IN_PROGRESS | Đang implement |
| 🟢 DONE | Đã merged |
| ⚫ WONTFIX | Quyết định không fix (lý do ghi trong file) |

## Priority Legend

| Priority | Ý nghĩa |
|----------|---------|
| 🔴 P0 | Blocker — phải fix trước khi ship |
| 🟠 P1 | High — fix trong sprint tới |
| 🟡 P2 | Medium — fix khi có resource |
| 🟢 P3 | Low — nice-to-have |

## Active Queue

| ID | Title | Domain | Priority | Status |
|----|-------|--------|:--------:|:------:|
| [GAP-001](GAP-001-kiteclass-gateway-decision.md) | Quyết định giữ/xóa kiteclass-gateway service | Architecture | 🟡 P2 | 🔵 OPEN |
| [GAP-002](GAP-002-ai-async-pipeline.md) | Async pipeline cho heavy AI tasks (image gen) | AI/Backend | 🟠 P1 | 🔵 OPEN |
| [GAP-003](GAP-003-ai-multi-tier-image-generation.md) | Multi-tier image generation strategy | AI/Backend | 🟡 P2 | 🔵 OPEN |
| [GAP-004](GAP-004-template-based-image-composition.md) | Template-based image composition (Canva-like) | AI/Frontend | 🟡 P2 | 🔵 OPEN |
| [GAP-005](GAP-005-ai-queue-fair-scheduling.md) | AI queue fair scheduling + capacity plan (100 users feasible on Oracle Free) | AI/Backend/DevOps | 🔴 P0 | 🔵 OPEN |
| [GAP-006](GAP-006-upgrade-to-gemma-4.md) | Upgrade AI models Llama 3.1 + LLaVA → Gemma 4 E4B | AI/Backend | 🟠 P1 | 🔵 OPEN |
| [GAP-007](GAP-007-resource-classification-pipeline.md) | Resource classification pipeline (static/template/full-AI) | AI/Backend | 🔴 P0 | 🔵 OPEN |
| [GAP-008](GAP-008-ai-agent-workflow.md) | AI Agent workflow (analyzer→planner→executor) thay direct gen | AI/Backend | 🟠 P1 | 🔵 OPEN |
| [GAP-009](GAP-009-instance-provisioning-lifecycle.md) | Frontend instance provisioning lifecycle (6 states) | Backend/DevOps | 🟠 P1 | 🔵 OPEN |
| [GAP-010](GAP-010-branding-package-api-integration.md) | Branding package API + KiteClass integration + E2E test | Backend/Frontend | 🟠 P1 | 🔵 OPEN |
| [GAP-011](GAP-011-template-library-curation-plan.md) | Template library curation plan + review standards | Design/Product | 🔴 P0 | 🔵 OPEN |
| [GAP-012](GAP-012-frontend-instance-quality-review.md) | Automated frontend instance quality review (post-deploy) | Quality/AI/FE | 🟠 P1 | 🔵 OPEN |
| [GAP-013](GAP-013-guided-branding-wizard-ux.md) | Guided branding wizard UX (closed-loop with flexibility) | FE/UX/Product | 🟠 P1 | 🔵 OPEN |
| [GAP-014](GAP-014-wave-mock-include-ai-branding.md) | Wave mock plan missing AI branding workflow | Mock Data/Wave | 🔴 P0 | 🔵 OPEN |
| [GAP-015](GAP-015-tenant-provisioning-auto-trigger-branding.md) | Tenant provisioning thiếu auto-trigger AI branding (event-driven) | Backend/FE/Integration | 🔴 P0 | 🔵 OPEN |
| [GAP-016](GAP-016-ai-branding-v2-living-docs-impact.md) | AI Branding v2 living docs impact scope (business/API/ERD/tests) | Docs/Governance | 🔴 P0 | 🟢 DONE |
| [GAP-017](GAP-017-ai-usage-billing-integration.md) | AI usage → billing integration (upsell, cost attribution) | Billing/AI | 🟠 P1 | 🔵 OPEN |
| [GAP-018](GAP-018-content-safety-compliance.md) | Content safety & compliance (moderation, audit, GDPR) | Security/Compliance | 🔴 P0 | 🔵 OPEN |
| [GAP-019](GAP-019-ai-observability-cost-monitoring.md) | AI observability & cost monitoring (Grafana, alerts) | DevOps/Monitoring | 🟠 P1 | 🔵 OPEN |
| [GAP-020](GAP-020-wizard-state-persistence.md) | Wizard state persistence & error recovery | Frontend/UX | 🟠 P1 | 🔵 OPEN |
| [GAP-021](GAP-021-branding-propagation-email-services.md) | Branding propagation to email + other services | Backend/Integration | 🟠 P1 | 🔵 OPEN |
| [GAP-022](GAP-022-template-analytics-optimization.md) | Template analytics & A/B optimization | Analytics/Product | 🟡 P2 | 🔵 OPEN |
| [GAP-023](GAP-023-admin-moderation-tools.md) | Admin moderation tools (review, flag, take-down) | Admin/Compliance | 🟠 P1 | 🔵 OPEN |
| [GAP-024](GAP-024-asset-lifecycle-storage-cleanup.md) | Asset lifecycle & storage cleanup (archive, quota) | DevOps/Storage | 🟡 P2 | 🔵 OPEN |
| [GAP-025](GAP-025-mobile-first-wizard-ux.md) | Mobile-first wizard UX (camera, swipe, offline) | Frontend/UX | 🟡 P2 | 🔵 OPEN |
| [GAP-026](GAP-026-trial-freemium-ai-mechanics.md) | Trial/freemium AI mechanics (budget, conversion) | Product/Billing | 🟠 P1 | 🔵 OPEN |
| [GAP-027](GAP-027-multi-brand-per-tenant.md) | Multi-brand per tenant (franchise, branches) | Product/Backend | 🟡 P2 | 🔵 OPEN |
| [GAP-028](GAP-028-model-versioning-migration-strategy.md) | AI model versioning & migration strategy | AI/DevOps | 🟡 P2 | 🔵 OPEN |
| [GAP-029](GAP-029-quality-gate-calibration.md) | Quality gate calibration & feedback loop | Quality/AI | 🟡 P2 | 🔵 OPEN |
| [GAP-030](GAP-030-disaster-recovery-ai-branding.md) | Disaster recovery for AI branding (RTO/RPO, runbooks) | DevOps/Reliability | 🟡 P2 | 🔵 OPEN |
| [GAP-031](GAP-031-expand-wizard-inputs-beyond-logo.md) | Expand wizard inputs beyond logo (rich brand context) | UX/Product/AI | 🔴 P0 | 🔵 OPEN |
| [GAP-032](GAP-032-branded-error-pages.md) | Branded error pages (404/500/maintenance) | Frontend/UX | 🟠 P1 | 🔵 OPEN |
| [GAP-033](GAP-033-branding-version-history-rollback.md) | Branding version history & rollback (user-facing) | Product/Backend | 🟠 P1 | 🔵 OPEN |
| [GAP-034](GAP-034-branding-export-pack.md) | Branding export pack (ZIP + PDF style guide) | Product/Backend | 🟡 P2 | 🔵 OPEN |
| [GAP-035](GAP-035-wizard-team-collaboration.md) | Wizard team collaboration (multi-user edit) | FE/Backend | 🟡 P2 | 🔵 OPEN |
| [GAP-036](GAP-036-tier-upgrade-reveal-ux.md) | Tier upgrade UX (reveal, teaser, unlock) | Product/Conversion | 🟠 P1 | 🔵 OPEN |
| [GAP-037](GAP-037-branded-auth-flows.md) | Branded auth flows (verify email, reset pwd pages) | Frontend/Integration | 🟠 P1 | 🔵 OPEN |
| [GAP-038](GAP-038-developer-api-docs-sdk.md) | Developer API docs + SDK / client library | DevExp/Docs | 🟠 P1 | 🔵 OPEN |
| [GAP-039](GAP-039-webhook-reliability-versioning.md) | Webhook reliability (retry, idempotency, versioning) | Integration/Backend | 🟠 P1 | 🔵 OPEN |
| [GAP-040](GAP-040-support-impersonation-tools.md) | Support impersonation & troubleshooting tools | Support/Ops | 🟠 P1 | 🔵 OPEN |
| [GAP-041](GAP-041-security-hardening-injection.md) | Security hardening — SVG XSS, SSRF, CSRF, prompt injection | Security | 🔴 P0 | 🔵 OPEN |
| [GAP-042](GAP-042-legal-ip-protection.md) | Legal/IP protection — trademark, DMCA, copyright | Legal/Compliance | 🔴 P0 | 🔵 OPEN |
| [GAP-043](GAP-043-performance-cache-stampede-protection.md) | Performance — cache stampede + thundering herd | Performance/Backend | 🟠 P1 | 🔵 OPEN |
| [GAP-044](GAP-044-synthetic-monitoring-feature-flags.md) | Synthetic monitoring + feature flags system | DevOps/Release | 🟡 P2 | 🔵 OPEN |
| [GAP-045](GAP-045-template-marketplace.md) | Template marketplace (community contributions) | Product/Community | 🟡 P2 | 🔵 OPEN |
| [GAP-046](GAP-046-apply-design-patterns-systematically.md) | Apply design patterns systematically (17 patterns catalog) | Architecture | 🟠 P1 | 🔵 OPEN |
| [GAP-047](GAP-047-document-generation-skills.md) | Document generation skills + infrastructure (Excel/Word/PDF/PPT from MiniMax) | Skills/Backend | 🔴 P0 | 🔵 OPEN |
| [GAP-048](GAP-048-output-review-standards-coverage.md) | Output review standards coverage (9 violations: gaps, rules, ADR, migrations, scripts, APIs, emails, legal, logs) | Governance | 🔴 P0 | 🔵 OPEN |
| [GAP-049](GAP-049-business-logic-correctness-review.md) | Business logic CORRECTNESS review (implementation reviewed, market/law correctness NOT) | Product/Business/Legal | 🔴 P0 | 🔵 OPEN |
| [GAP-050](GAP-050-persona-based-business-review.md) | Persona-based business review process (master) | Product/Business | 🔴 P0 | 🔵 OPEN |
| [GAP-051](GAP-051-bulk-import-users-xlsx.md) | Bulk import users via xlsx (user's school 500-student example) | Backend/Product | 🔴 P0 | 🔵 OPEN |
| [GAP-052](GAP-052-parent-portal.md) | Parent portal + accounts (K-12 critical) | Product/FE/BE | 🔴 P0 | 🔵 OPEN |
| [GAP-053](GAP-053-academic-year-semester-structure.md) | Academic year + semester structure (VN K-12) | Backend/Product | 🔴 P0 | 🔵 OPEN |
| [GAP-054](GAP-054-multi-subject-per-student.md) | Multi-subject per student (K-12 data model) | Backend/Data | 🔴 P0 | 🔵 OPEN |
| [GAP-055](GAP-055-official-report-card-vn.md) | Official report card VN format (bảng điểm MOE) | Backend/PDF | 🟠 P1 | 🔵 OPEN |
| [GAP-056](GAP-056-homeroom-teacher-gvcn.md) | Homeroom teacher (GVCN) concept | Backend/Product | 🟠 P1 | 🔵 OPEN |
| [GAP-057](GAP-057-payroll-teacher-commission.md) | Teacher payroll + commission calculation | Backend/Finance | 🟠 P1 | 🔵 OPEN |
| [GAP-058](GAP-058-role-hierarchy-org-chart.md) | Role hierarchy + organizational chart | Backend/Security | 🟠 P1 | 🔵 OPEN |
| [GAP-059](GAP-059-student-conduct-tracking.md) | Student conduct tracking (hạnh kiểm) | Product/Backend | 🟡 P2 | 🔵 OPEN |
| [GAP-060](GAP-060-period-based-attendance.md) | Period-based attendance (multiple slots/day) | Backend/Product | 🟡 P2 | 🔵 OPEN |
| [GAP-061](GAP-061-promotion-retention-logic.md) | Promotion/retention logic (lên lớp / ở lại) | Backend/Product | 🟡 P2 | 🔵 OPEN |
| [GAP-062](GAP-062-payroll-bank-integration.md) | Payroll bank integration (batch transfer) | Backend/Integration | 🟡 P2 | 🔵 OPEN |
| [GAP-063](GAP-063-sms-zalo-notification-integration.md) | SMS + Zalo notification integration (VN critical) | Backend/Integration | 🟠 P1 | 🔵 OPEN |
| [GAP-064](GAP-064-scorm-xapi-compliance.md) | SCORM/xAPI compliance (corporate training) | Backend/LMS | 🟡 P2 | 🔵 OPEN |

## Summary by Priority

- 🔴 **P0 (20 gaps):** 005, 007, 011, 014, 015, 016, 018, 031, 041, 042, 047, 048, 049, 050, 051, 052, 053, 054
- 🟠 **P1 (24 gaps):** 002, 006, 008, 009, 010, 012, 013, 017, 019, 020, 021, 023, 026, 032, 033, 036, 037, 038, 039, 040, 043, 046, 055, 056, 057, 058, 063
- 🟡 **P2 (20 gaps):** 001, 003, 004, 022, 024, 025, 027, 028, 029, 030, 034, 035, 044, 045, 059, 060, 061, 062, 064

**Total: 64 gaps** — AI Branding + doc generation + governance + business correctness + **persona-based features**.

## Persona Coverage Status

Reference: `documents/00-brd/personas-catalog.md`

| Persona | Coverage | Critical Missing |
|---------|:--------:|------------------|
| P1 Solo Teacher | 🟡 60% | Simple flows |
| P2 Small Center | 🟢 75% | Mostly ready |
| P3 Medium Center | 🟡 65% | Payroll, roles |
| **P5 K-12 School** | 🔴 **30%** | **Bulk import, parent portal, academic year, multi-subject, report card, GVCN** |
| P4 Chain | 🔴 20% | Multi-brand (GAP-027) |
| P7 Corporate | 🔴 10% | SCORM (GAP-064) |

## Simulation Methodology

Gaps found via `.claude/skills/quality/simulation-gap-finder.md` — 3-axis matrix:
- **Personas:** Owner, End User, Platform Admin, Developer, Support
- **Stages:** Discovery, Signup, Config, Provisioning, Daily Use, Edge/Error, Evolution, Termination
- **Categories:** Functional, UX, Data, Performance, Security, Compliance, Ops, Integration, Commercial, Evolution

## Developer Rules

- **[`.claude/rules/ai-branding-guidelines.md`](../../../.claude/rules/ai-branding-guidelines.md)** — MANDATORY rules cho developers làm AI branding feature

## AI Branding Master Design

Key feature redesign doc: **[`documents/02-architecture/ai-branding-v2-redesign.md`](../../02-architecture/ai-branding-v2-redesign.md)** (supersedes old `ai-local-implementation-plan.md` §architecture)

GAP-005, 007, 008, 009, 010 đều derive từ redesign doc này.

## File Naming Convention

`GAP-XXX-short-kebab-title.md` where XXX is zero-padded sequential ID.

## Template

Dùng template `_TEMPLATE.md` khi tạo gap mới.

---

**Last updated:** 2026-04-14
