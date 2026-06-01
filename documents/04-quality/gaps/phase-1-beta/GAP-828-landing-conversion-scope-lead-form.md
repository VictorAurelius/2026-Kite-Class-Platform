---
id: GAP-828
title: Landing conversion scope — lead-form/CTA + giá + lịch (text+banner thiếu cơ chế convert)
status: OPEN
priority: P1
phase: phase-1-beta
domain: Mixed
created: 2026-06-01
---

# GAP-828 — Landing conversion scope (lead-form/CTA + giá + lịch)

> Surfaced bởi outside-in persona + benchmark audit (2026-06-01, 2/3 agent landing-input state-check) — đồng thuận: inside-out "text+banner only" tối ưu *dễ build*, KHÔNG tối ưu *convert*. Landing tuyển sinh thiếu cơ chế chuyển đổi = brochure tĩnh, 0 lead.

## Problem

Dev đề xuất chốt landing input = text + banner. 2 agent outside-in độc lập kết luận thiếu nghiêm trọng:

- **Lead-capture form/CTA (gap #1 chí mạng):** 9/9 nền tảng benchmark có; landing 1-CTA convert +202%. Phụ huynh KHÔNG có nút để lại SĐT/đăng ký tư vấn → 0 đăng ký. Đây là *mục tiêu* của landing. `contactPhone` có nhưng KHÔNG có form structured.
- **Giá/học phí (`pricingTiers`):** câu hỏi #1 của phụ huynh VN; 8/9 benchmark có.
- **Lịch/khóa học (`programs`):** "dạy lớp mấy, lịch nào, online/tại nhà".
- **Social proof (testimonials + stats):** trụ cột niềm tin (9/9) — Phase 1.5 acceptable nhưng impact cao.

Persona blind spots: cô Hà (ít tech) bị field kỹ thuật (hex/JSONB) làm khó → editor phải ẩn + default; thầy Nhì cần social proof; TT Sky cần teachers array + multi-user.

## Proposed Fix — "conversion-complete minimum" scope (locked 2026-06-01)

**Phase 1 MUST:** hero text + banner + **lead-capture form/CTA** + giá (≥1-3 gói text) + lịch/khóa (list text) + contact (Zalo/SĐT). Editor ẩn hex/JSONB cho GV ít tech + default sẵn.
**Phase 1.5 defer:** testimonials, instructor array, multi-banner carousel (GAP-826).
**Phase 2 defer:** FAQ, video, gallery, badge, countdown.

## Acceptance Criteria

- [ ] Lead-capture form (tên + SĐT + lời nhắn → lưu DB / gửi tenant) + CTA rõ ở hero
- [ ] Pricing block render từ `pricingTiers` (tối thiểu text tier)
- [ ] Programs/lịch block render từ `programs`
- [ ] Contact gồm Zalo (field `zaloUrl`/`zaloPhone` — VN dùng Zalo > email)
- [ ] Editor (GAP-815) ẩn field kỹ thuật cho `personal` template, default sẵn
- [ ] Lead-form spam/validation (rate-limit, honeypot) — phối GAP-827 safety

## Related

- Outside-in persona + benchmark audit 2026-06-01 (3-agent)
- GAP-815 (editor UI) — nơi nhập các field này
- GAP-827 (input safety) — lead-form input cũng phải sanitize/validate
- GAP-826 (multi-banner) — defer Ph1.5
- GAP-660 (Zalo CTA) + `thesis-as-future-state-mandate.md` — Zalo contact minimum interpretation
