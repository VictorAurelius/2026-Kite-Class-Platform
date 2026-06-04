# GAP-969: VN phone format validation thiếu pattern check (BR-SET-16)

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (TenantSettings contact phone validation)
**Defer-to:** After Wave flow-kh3 finish

## Problem

BR-SET-16 "Contact phone — max 20 chars" — KHÔNG validate Vietnamese phone format (`0xxxxxxxxx` 10 digits OR `+84xxx`). Bác Hùng gõ `0974.567.890` (định dạng với dấu chấm cũ) → save OK → khi SMS notification (future) fail vì gateway expect `+84974567890`. Surfaced: persona Finding 3.5.

## Proposed Fix

Add VN phone regex validator `^(0|\\+84)[0-9]{9,10}$` HOẶC dùng libphonenumber với VN region. Normalize input (remove dots/spaces/dashes) trước khi save. FE input shows hint "Định dạng: 0xxxxxxxxx hoặc +84xxx".

## Acceptance Criteria

- [ ] `@Pattern` annotation cho phone field hoặc libphonenumber check
- [ ] Walk: input `0974.567.890` → normalized save `0974567890` hoặc reject với hint
- [ ] FE input mask hỗ trợ VN phone format

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-tenant-provisioning.md Finding 3.5
- Sister rule: `.claude/rules/vn-localization-audit-checklist.md` v1.0.0
- Flow Verification Campaign §4 row KC-1
