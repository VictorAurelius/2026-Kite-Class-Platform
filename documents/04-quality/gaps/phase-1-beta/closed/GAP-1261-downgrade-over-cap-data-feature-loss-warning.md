# GAP-1261: Downgrade vượt-cap — cảnh báo mất dữ liệu/tính năng + confirm (students/storage/custom-domain)

**Status:** 🟢 DONE 2026-06-14
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-frontend` plan-change flow + `kitehub-subscription` downgrade logic

## Problem

Persona audit (F3) + benchmark audit (F5): khi hạ tier (vd PREMIUM 200 → BASIC 50 students, 8GB → 2GB storage, mất custom domain), hệ thống KHÔNG xử lý phần vượt-cap và KHÔNG cảnh báo owner trước. Owner có thể mất quyền truy cập học viên / dữ liệu / domain mà không được báo trước → mất niềm tin + rủi ro mất dữ liệu.

## Proposed Fix

Thêm bước pre-downgrade hiển thị impact summary (số học viên vượt cap, dung lượng vượt, mất custom domain) + yêu cầu confirm + soft-lock phần vượt thay vì xóa cứng.

## Acceptance Criteria

- [x] Downgrade vượt cap → hiển thị impact summary trước khi xác nhận
- [x] Owner phải confirm mới hạ tier
- [x] Dữ liệu vượt cap bị soft-lock (không xóa cứng), khôi phục được nếu nâng lại

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-persona.md` (F3)
- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-benchmark.md` (F5)
- Sister: GAP-1262 (prorated breakdown), GAP-1269 (tier recommender)

## Closure — wave-kitehub-biz-100 (2026-06-14)

🟢 DONE — engineering-complete + G3 production-parity walk verified. G3 walk #3 — downgrade-preview over-cap warning via tier-caps (4 cảnh báo tiếng Việt + usageDataNote).

- G3 walk: `documents/04-quality/audits/persona-review/2026-06-13-g3-walk-kitehub-biz-100.md` (8 PASS / 1 PASS-with-P1 closed via GAP-1273 / 1 win-back async by-design).
- Tests: 963 backend (kitehub-subscription) + 906 frontend green.
- Consolidated into PR branch `wave/kitehub-biz-100`.


## Out-of-scope (Phase 1.5+)

| Item | Tracking |
|---|---|
| Real usage-count từ kiteclass-core per-tenant DB | Phase 1.5 |
