# GAP-1115: AI Branding wizard thiếu trục user-type (GV đơn lẻ / trung tâm) — input độc lập với audience

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-10 (discuss wizard 6-bước với user — design critique)
**Affects:** `kitehub-frontend` branding wizard (`WelcomeStep` / `AudienceStep` / `wizard-shared.tsx` WizardState) + `kitehub-branding` BrandingGenerationRequest

## Problem

Wizard hiện bắt **audience** (4 phân khúc: mầm non / THCS-THPT / trung tâm tiếng Anh / lớp luyện thi — `AudienceStep.tsx` `AUDIENCE_OPTIONS`) để quyết **màu/hình/ngôn ngữ** của sản phẩm branding. Đó là **trục theming** — đúng mục đích wizard (per `ai-branding-guidelines.md` §2.1 constrained-preset).

NHƯNG thiếu **trục thứ 2 orthogonal: user-type / cấu trúc tổ chức** (GV đơn lẻ / trung tâm nhỏ / trung tâm lớn). Trục này KHÔNG quyết theme (1 GV tiếng Anh đơn lẻ và 1 trung tâm tiếng Anh lớn đều muốn thẩm mỹ "tiếng Anh") — nó quyết:
- **Chiến lược asset**: số chân dung upload (đơn lẻ = 1 người, trung tâm = nhiều người) → xem [[GAP-1116]]
- **Gợi ý tier** + feature set

Hệ quả: wizard không tailor được "1 chân dung vs nhiều", không có dữ liệu user-type để các bước sau (portrait, banner-compose) dùng.

## Proposed Fix

1. Thêm field/bước **user-type** (`SOLO_TEACHER` / `SMALL_CENTER` / `LARGE_CENTER`) — đặt ở `WelcomeStep` (cùng tên + slug) HOẶC 1 bước nhẹ ngay sau Audience. Constrained preset (không free-text).
2. Lưu vào `WizardState` (`wizard-shared.tsx` reducer) + gửi qua `BrandingGenerationRequest` (thêm field `orgType`).
3. Dùng user-type làm input cho: portrait count ([[GAP-1116]]) + tier-hint gợi ý nâng cấp.

## Acceptance Criteria

- [ ] Wizard capture user-type (constrained preset) + persist trong WizardState
- [ ] `BrandingGenerationRequest` mang `orgType`; BE nhận + lưu (entity/migration nếu cần per `design-patterns.md` §3.12 triad)
- [ ] User-type feed sang portrait step ([[GAP-1116]]) quyết count
- [ ] Build + test xanh

## Related

- Discovered in: discuss wizard 6-bước 2026-06-10 (user design critique câu 1)
- Cluster wizard-redesign: [[GAP-1116]] (portrait) + [[GAP-1117]] (banner render + AI keys) + [[GAP-1118]] (full-screen preview)
- Design: `ai-branding-guidelines.md` §2.1 (constrained preset), `AudienceStep.tsx` (trục audience hiện có)

## Log

- **2026-06-10:** Filed từ discuss wizard với user — audience axis (theme) đúng nhưng thiếu user-type axis (asset strategy). Per `discovery-to-gap-inline-filing.md`. GAP-ID từ block reserve 1115-1118 (per `multi-session-concurrency-coordination.md` GAP-1114).
