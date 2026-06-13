# GAP-1272: Migration reversal-window clock-origin doc-vs-code drift (+ ref Phase-2 deferrals)

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Backend
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-subscription` reversal-window logic + rules.md (T2P-04)

## Problem

Failure-mode audit (FM-10): `isWithinReversalWindow:72` đo cửa sổ đảo ngược (reversal window) tính từ `migrationCompletedAt`, trong khi rule T2P-04 ghi 'after PAYMENT_CAPTURED'. Doc và code lệch mốc thời gian → cửa sổ thực tế khác với spec.

Ghi chú các deferral Phase 2 phát hiện cùng đợt audit (KHÔNG fix ở gap này):
- FM-9: scheduler leader-election cho multi-instance (Phase 2, dep GAP-123 / GAP-479)
- FM-11: webhook replay nonce protection (Phase 2, dep GAP-039)

## Proposed Fix

Reconcile rules.md (T2P-04) ↔ code: thống nhất mốc tính reversal-window (migrationCompletedAt vs PAYMENT_CAPTURED), sửa bên sai cho khớp.

## Acceptance Criteria

- [ ] Mốc clock-origin của reversal-window thống nhất giữa rules.md và code
- [ ] T2P-04 doc + `isWithinReversalWindow` cùng định nghĩa cửa sổ

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-failure-mode.md` (FM-10; refs FM-9, FM-11 Phase 2)
- Phase 2 deferrals: GAP-123, GAP-479 (leader-election), GAP-039 (webhook replay nonce)
