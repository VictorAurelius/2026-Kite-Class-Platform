# GAP-1463: KH-5 downgrade — FE không hiển thị pending_tier sau khi hạ gói

**Status:** 🔵 OPEN
**Priority:** 🟠 P2
**Domain:** Frontend
**Found:** 2026-06-16 (KH-5 human G2 walk)
**Affects:** kitehub-frontend billing CurrentPlanCard

## Problem

KH-5 human walk: downgrade (Nâng cấp → chọn gói thấp hơn) hoạt động đúng ở DB (subscriptions.pending_tier=BASIC, đổi cuối kỳ) + toast "đã lên lịch", NHƯNG FE billing page KHÔNG hiển thị pending downgrade ("Gói sẽ đổi sang BASIC vào <ngày>"). User báo "không có gì thay đổi, không biết check ở đâu". CurrentPlanCard chỉ show tier hiện tại, không show pending_tier.

## Acceptance Criteria

- [ ] CurrentPlanCard hiển thị badge/note "Sẽ đổi sang {pending_tier} vào {current_period_end}" khi pending_tier != null
- [ ] Cancel CTA deep-link tới tab Nguy hiểm (vấn đề 2 — minor, hiện redirect /settings phải tự click)

## Related
- Discovered in: 2026-06-16 KH-5 G2 walk
