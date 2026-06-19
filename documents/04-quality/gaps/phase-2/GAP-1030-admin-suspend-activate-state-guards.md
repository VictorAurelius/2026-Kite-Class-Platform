# GAP-1030: Admin suspend/activate thiếu state guard — double-suspend trả 200 thay vì 409

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-06 (KH-9 admin console G1 walk)
**Affects:** `AdminController.suspendInstance` + `activateInstance` (kitehub-admin)

## Problem

KH-9 G1 walk: suspend một instance đã SUSPENDED → **HTTP 200** thay vì 409 Conflict (no-op idempotent nhưng client không phân biệt được). Walk evidence: suspend #1 → 200 (SUSPENDED); suspend #2 → 200 (vẫn SUSPENDED). Tương tự activate một instance đã ACTIVE.

Không phải bug nghiêm trọng (idempotent), nhưng thiếu state-machine guard → admin không nhận feedback "instance đã ở trạng thái này"; cũng có thể che lỗi (suspend tưởng thành công trên instance đã suspended bởi người khác).

## Root Cause

suspend/activate flip status không check current state precondition.

## Proposed Fix

State guard: suspend instance đã SUSPENDED → 409 (hoặc 200 với flag `alreadyInState`); activate đã ACTIVE → 409. Document idempotency semantics.

## Acceptance Criteria

- [ ] Double-suspend → 409 (hoặc documented idempotent response phân biệt được)
- [ ] Activate already-active → 409
- [ ] IT cover state precondition

## Related

- Discovered in: KH-9 G1 walk — `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh9-admin-console.md` (FM-5)
- Pattern giống GAP-1026 (purge non-deleted 409)
