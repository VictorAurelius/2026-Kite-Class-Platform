---
id: GAP-815
title: Landing content editor UI — GV tự tạo nội dung 6 section (không chỉ seed SQL)
status: OPEN
priority: P3
phase: phase-1-beta
domain: KiteClass
created: 2026-05-30
---

# GAP-815 — Landing content editor UI (self-service 6 section)

> Surfaced bởi wave-thesis-4 data-driven landing refactor (2026-05-30). Sau khi landing thành data-driven (V76 + 7 JSONB field: aboutText/teachers/programs/pricingTiers/testimonials/faqs/stats), nội dung per-tenant hiện CHỈ tạo được qua `seed-landing-content.sql` (SQL trực tiếp) hoặc `PUT /api/v1/tenants/{id}/landing` (API thô). **Không có FE editor page** cho GV tự nhập.

## Problem

Backend đã có `PUT /api/v1/tenants/{id}/landing` (LandingPageController) + entity 7 field section. Nhưng FE thiếu trang editor để giảng viên (P2 owner / teacher) tự tạo + chỉnh nội dung landing:
- About (giới thiệu)
- Teachers (đội ngũ GV)
- Programs (môn/chương trình)
- Pricing tiers (bảng giá)
- Testimonials (phản hồi)
- FAQ
- Stats (số liệu)

AI Branding wizard hiện có (GAP-013 / GAP-726) chỉ cover **visual identity** (logo / theme / banner), KHÔNG cover **nội dung section**. Hệ quả: GV mới onboard thấy landing fallback default (nội dung demo tiếng-Anh-center) cho đến khi có người seed thủ công → blank-page friction, low completion.

## Proposed Fix — Hybrid theo độ rủi ro tính chính trực

Phân loại cách nhập theo mức rủi ro (đánh giá 2026-05-30):

| Field | Cách nhập | Lý do |
|---|---|---|
| **about / programs / pricing / faq** | AI draft + GV edit | Marketing copy low-stakes; GV nhập vài fact (môn/cấp/năm KN/học phí) → AI render đúng format + tone VN; giải blank-page; khớp pattern AI Branding "template-first, AI-when-needed" |
| **teachers** | Manual / prefill từ teacher profile | Dữ kiện thật; hệ thống đã có teacher record → prefill |
| **stats** (số HS, % đỗ) | AUTO từ data thật hệ thống | Hệ thống đã có student count / enrollment thật → tự tính. CẤM AI/nhập-tay bịa số |
| **testimonials** | Manual / import review thật | CẤM AI sinh — bịa review = rủi ro pháp lý + vi phạm `vn-localization-audit-checklist.md` §3 (sample data thật) + mất niềm tin |

**Ranh giới chính trực:** AI chỉ render copy thấp rủi ro (about/programs/pricing/faq). Số liệu (stats) + testimonials PHẢI từ data thật, không bịa — nhất quán với cách AI Branding hiện tại (AI cho visual, không bịa fact).

## Acceptance Criteria

- [ ] FE editor page `(dashboard)/settings/landing` (hoặc tương đương) cho GV chỉnh 7 field section
- [ ] AI-draft flow cho about/programs/pricing/faq: GV nhập input ngắn → gọi AI (OpenAI, qua kitehub-branding hoặc service phù hợp) → render JSON đúng contract → GV review + edit trước publish
- [ ] teachers prefill từ teacher records của tenant
- [ ] stats auto-compute từ student/enrollment data thật (read-only hoặc opt-in)
- [ ] testimonials manual entry (+ lộ trình import review thật); KHÔNG có nút "AI sinh testimonial"
- [ ] PUT /landing wired từ FE editor; optimistic preview qua `?primary=&secondary=` đã có
- [ ] VN-localization: VND format, tiếng Việt, không placeholder English

## Scope note

Execution **Phase 1.5+** (self-service UX enhancement, không phải P1 MVP blocker — data-driven landing đã hoạt động qua seed/PUT). P3.

## Related

- `GAP-013-guided-branding-wizard-ux` — AI Branding wizard (visual; pattern tham khảo cho AI-draft flow)
- `GAP-726-kc-branding-wizard-blank-render-econnrefused-8080` — branding wizard render
- wave-thesis-4 — data-driven landing refactor (V76 + 7 JSONB field) tạo nền cho gap này
- `kitehub/scripts/seed-landing-content.sql` — seed thủ công hiện tại (sẽ được thay bằng UI)
- `.claude/rules/vn-localization-audit-checklist.md` §3 — ranh giới sample data thật (testimonials/stats)

## Log

- **2026-06-12:** Design source (基本設計) shipped per GAP-1234 — 3 màn editor trong kit `ui_kits/ai-branding-wizard-v2/v3/screens/`: `editor-overview.html` (7 section + nguồn nhập), `editor-section-ai-draft.html` (AI draft + GV duyệt — pattern cho about/pain/pricing/faq), `editor-testimonials.html` (manual-only, không nút AI). Wizard cũng thêm `step2-info.html` thu facts thật (nguồn prefill cho editor). FE implementation vẫn OPEN scope gap này.
- **2026-06-11:** GAP-826 lớp 3 advance một phần: card "Banner landing" trong branding-settings — pattern editor section đầu tiên. Editor 6 section đầy đủ vẫn là scope gap này.
