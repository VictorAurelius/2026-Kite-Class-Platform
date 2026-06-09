# GAP-595: Landing CTA hierarchy + demo entry path before signup commit

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (defer Wave 87 — gate Phase 1 beta conversion rate)
**Domain:** Frontend / UX
**Phase:** phase-1-beta
**Found:** 2026-05-15 (Wave 86 Bucket A persona-outside-in audit cell 1.3)
**Affects:** Anonymous prospect (Vy) conversion path → Phase 1 beta cohort pipeline

## Problem

Persona cell 1.3 (Vy — sinh viên sư phạm / giáo viên freelance arrive từ Google search "phần mềm quản lý lớp học"):
- KHÔNG muốn bị bắt đăng ký ngay khi vừa landing
- Cần "Xem demo" hoặc "Tìm hiểu giá" trước commit signup
- Signup form ≤5 fields

Wave 86 không cover signup form audit / demo entry / pricing transparency. Forcing signup-before-demo = bounce → impact cohort pipeline post-5 invites khi need scale tới >20 tenants Phase 1.5.

## Root Cause

Landing CTA hierarchy hiện tại có thể đơn giản "Sign Up" duy nhất; thiếu nested CTAs alternative paths (Watch Demo / View Pricing / Read Docs).

## Proposed Fix

1. **Landing CTA hierarchy** `kitehub-frontend/src/app/page.tsx`:
   - Primary CTA: "Đăng ký Beta" (Sign Up)
   - Secondary CTA: "Xem demo 2 phút" (video modal hoặc link `/demo`)
   - Tertiary link: "Xem bảng giá" (link `/pricing`)
   - Quaternary link: "Đọc hướng dẫn" (link `/help/anonymous`)
2. **Demo video page** `/demo`:
   - 2-min YouTube embedded video (P2 owner walkthrough)
   - Below video: 3-screenshot carousel + key feature bullet
   - CTA after video: "Tôi muốn thử Beta" → signup form
3. **Signup form audit**:
   - Reduce to 5 fields: name, email, phone, role (P1/P2/P3), tenant_name
   - Inline validation per `pre-handoff-self-test-completeness.md` §2.1
4. **A/B test framework** (defer Wave 88+): CTR delta direct-signup vs demo-first

## Acceptance Criteria

- [ ] Landing CTA hierarchy implemented với 4 levels
- [ ] Demo video page live
- [ ] Signup form reduced ≤ 5 fields với inline validation
- [ ] Defer A/B test Wave 88+
- [ ] Conversion rate measured post-Wave 87 (baseline cho Phase 1.5)

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-persona-outside-in.md` §3.1 cell 1.3 + §4 rank 6 + §6 NEW gap proposal #6
- Wave 87 scope (defer)
- GAP-596 landing form inline validation (paired Wave 87)

## Log
- **2026-06-09 DONE:** Wave landing-100 shipped (bucket 595) — G1-headless verified (FE build green + curl render 200 + ?tenant= data-binding proven). Full browser-G2 + subdomain resolution gated GAP-811/1077; BE per-tenant fields GAP-1083.
