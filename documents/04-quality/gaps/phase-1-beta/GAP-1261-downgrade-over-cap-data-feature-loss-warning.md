# GAP-1261: Downgrade vượt-cap — cảnh báo mất dữ liệu/tính năng + confirm (students/storage/custom-domain)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-frontend` plan-change flow + `kitehub-subscription` downgrade logic

## Problem

Persona audit (F3) + benchmark audit (F5): khi hạ tier (vd PREMIUM 200 → BASIC 50 students, 8GB → 2GB storage, mất custom domain), hệ thống KHÔNG xử lý phần vượt-cap và KHÔNG cảnh báo owner trước. Owner có thể mất quyền truy cập học viên / dữ liệu / domain mà không được báo trước → mất niềm tin + rủi ro mất dữ liệu.

## Proposed Fix

Thêm bước pre-downgrade hiển thị impact summary (số học viên vượt cap, dung lượng vượt, mất custom domain) + yêu cầu confirm + soft-lock phần vượt thay vì xóa cứng.

## Acceptance Criteria

- [ ] Downgrade vượt cap → hiển thị impact summary trước khi xác nhận
- [ ] Owner phải confirm mới hạ tier
- [ ] Dữ liệu vượt cap bị soft-lock (không xóa cứng), khôi phục được nếu nâng lại

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-persona.md` (F3)
- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-benchmark.md` (F5)
- Sister: GAP-1262 (prorated breakdown), GAP-1269 (tier recommender)
