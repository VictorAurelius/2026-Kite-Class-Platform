---
id: GAP-720
title: Multi-branch routing — Phase 1 BETA single-branch limitation FAQ + Wave 106 design candidate
status: OPEN
priority: P2
phase: phase-1-beta
audience: dev
found: 2026-05-22
last_verified: 2026-05-22
completion_pct: 0
related: [GAP-286, GAP-721, GAP-722]
---

# GAP-720 — Multi-branch routing Phase 1 BETA single-branch FAQ + Wave 106 design

## Problem

Wave 105 Bucket B Owner persona walk (per `documents/04-quality/audits/persona-review/2026-05-22-wave-105-bucket-b-owner-walk.md` §1 Step 9) surfaced Phase 1 BETA assumption "single-branch only":

- `tenant_id` 1:1 với center (no branch sub-scope)
- Student model thiếu `branchId` column
- Class model thiếu `branchId` column
- Onboarding wizard không prompt for branch selection

**Persona impact (Hằng — Sky Edu Anh ngữ):**
- 2 chi nhánh × 160 học viên = 320 tổng
- Hằng forced workaround: tạo 2 separate tenants (Sky Edu Quận 1 + Sky Edu Quận 3)
- Hệ quả: user accounts duplicated (Hằng + admin trong cả 2 tenant), invoice trùng, không cross-branch report

**VN edu market reality:** Trung tâm 2-5 chi nhánh là norm cho center scale-up (per `vn-saas-benchmark.md` audit) — Misa, DotB, EduSpace đều có multi-branch.

## Root cause

Phase 1 BETA scope deliberate narrow per `release-1-plan-2026.md` — minimize schema complexity cho 5 beta tenant cohort. Multi-branch design defer until paid release + business case validation.

## Proposed Fix

### Phase 1 BETA scope (this gap)

User-facing FAQ entry trong help docs giải thích single-branch limitation + workaround guidance:

```markdown
**Q: Trung tâm tôi có 2 chi nhánh — KiteHub có hỗ trợ không?**

A: Phase 1 BETA hiện hỗ trợ single-branch only (1 tenant = 1 trung tâm). Nếu trung tâm bạn có nhiều chi nhánh, có 2 cách:

1. **Workaround Phase 1 BETA:** Tạo mỗi chi nhánh thành 1 tenant riêng (vd: "Sky Edu Quận 1" + "Sky Edu Quận 3"). Hằn chế: user accounts duplicate, không cross-branch report.

2. **Defer Wave 106 (paid release):** Multi-branch native đang trong roadmap Phase 1.5+ paid release. Liên hệ support@kitehub.me để nhận thông báo khi tính năng available.
```

### Wave 106 design candidate (out-of-scope this gap — for tracking)

- Schema: add `branches` table + `branch_id` FK to students/classes/teachers
- RLS update: tenant_id + branch_id composite isolation
- Onboarding: prompt for branch count + names tại Step PROFILE_SETUP
- Dashboard: cross-branch report aggregation

## Acceptance Criteria

- [ ] FAQ entry shipped trong `documents/05-guides/user-manual/p2-owner/faq.md` (Wave 106 user manual ship per GAP-537 Bucket F2)
- [ ] Wave 106 plan §3 includes "Multi-branch native design" candidate bucket
- [ ] `documents/01-business/kitehub/onboarding/rules.md` BR-ONBOARD-NEW added cho branch model decision

## Related

- Persona walk: `documents/04-quality/audits/persona-review/2026-05-22-wave-105-bucket-b-owner-walk.md` §1 Step 9
- Audit cross-ref: `documents/04-quality/audits/persona-review/2026-05-22-wave-105-persona-simulation.md` §Bucket B "Missing steps — multi-branch setup CRITICAL"
- Wave plan defer: `documents/03-planning/waves/wave-2026-05-22-105-persona-walk-beta-readiness.md` §Open Items "Multi-branch routing Wave 106"
- Sister gaps Wave 105 Bucket B: GAP-721 (Zalo OA owner-notify stub), GAP-722 (VietQR live Phase 1.5+)

## Log

- **2026-05-22:** Gap filed. Wave 105 Bucket B Owner persona walk identified single-branch limitation cần FAQ + Wave 106 design candidate. Defer per wave plan §Open Items.
