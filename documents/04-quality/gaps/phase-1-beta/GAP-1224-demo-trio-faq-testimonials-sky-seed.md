# GAP-1224: FAQ + Testimonials không seed cho demo-trio + sky sparse → landing thiếu 2 section (re-score −4)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-11 (landing-100 production re-score 81/100 — delta #1 vs kit, DATA không phải code)
**Affects:** `BrandingDataSeeder` landing constants (faqs/testimonials JSONB) + sky-education sections

## Problem

Component FAQ/Testimonials đã port + wire (hide-when-empty đúng GAP-958) nhưng demo-trio không có data → section ẩn → landing mỏng hơn kit (−3-4 điểm). Sky-education sparse hơn nữa (72/100 lowest bar).

## Proposed Fix

Seeder upsert thêm per-tenant: faqs (4-5 câu thật giọng phụ huynh hỏi) + testimonials (2-3, tên VN + vai trò phụ huynh/học viên, KHÔNG bịa số liệu) cho Hà/Nhì/Khánh; sky bổ sung sections thiếu cho đủ bar ≥90 mọi tenant.

## Acceptance Criteria

- [ ] FAQ + Testimonials render cả 3 tenant (browser verify)
- [ ] Sky-education re-score ≥90 cùng bar
