# GAP-1258: Auto-renew (SUB-03) mâu thuẫn VietQR thủ công (SUB-11) — relabel / mặc-định-tắt Phase 1

**Status:** 🟢 DONE 2026-06-14
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-frontend` billing settings + subscription auto-renew config

## Problem

Persona audit (F2): UI cho bật 'Tự động gia hạn' (rule SUB-03) nhưng hệ thống KHÔNG lưu thẻ và KHÔNG auto-capture — thanh toán Phase 1 là VietQR thủ công (rule SUB-11). Owner bật auto-renew rồi kỳ vọng được trừ tiền tự động → tới hạn không có gì xảy ra → involuntary churn vì hiểu nhầm.

## Proposed Fix

Đổi nhãn 'Tự động gia hạn' → 'Tự động nhắc gia hạn (cần CK thủ công)' HOẶC tắt mặc định toggle auto-renew trong Phase 1 cho đến khi có payment processor.

## Acceptance Criteria

- [x] Toggle auto-renew relabel rõ là chỉ nhắc, cần CK thủ công — HOẶC ẩn/tắt mặc định Phase 1
- [x] Không còn UI ngụ ý hệ thống tự trừ tiền

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-persona.md` (F2)
- Sister: GAP-1259 (grace dunning), GAP-1270 (trial conversion cadence)

## Closure — wave-kitehub-biz-100 (2026-06-14)

🟢 DONE — engineering-complete + G3 production-parity walk verified. BE/FE — auto-renew (SUB-03) vs VietQR thủ công (SUB-11): relabel + default-off Phase 1.

- G3 walk: `documents/04-quality/audits/persona-review/2026-06-13-g3-walk-kitehub-biz-100.md` (8 PASS / 1 PASS-with-P1 closed via GAP-1273 / 1 win-back async by-design).
- Tests: 963 backend (kitehub-subscription) + 906 frontend green.
- Consolidated into PR branch `wave/kitehub-biz-100`.
