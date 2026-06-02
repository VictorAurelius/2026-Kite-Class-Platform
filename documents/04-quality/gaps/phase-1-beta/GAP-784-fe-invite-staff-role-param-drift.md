---
audience: dev
---

# GAP-784 — FE InviteStaffPage role param missing — Wave 80 FE vs Wave meta-6 BE drift

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-05-28 (Wave meta-6 human walk RST cycle)
**Phase:** phase-1-beta

## Problem

FE `kitehub/kitehub-frontend/src/app/(admin)/admin/staff/invite/page.tsx` (Wave 80 era) submits `{email, fullName}` to `POST /api/v1/staff-invitations`. Wave meta-6 BE `InviteStaffRequest` DTO requires `role` field (not blank) → BE returns 400 VALIDATION_ERROR.

```
{"success":false,"code":"VALIDATION_ERROR","fieldErrors":{"role":["must not be blank"]}}
```

## Root Cause

API contract drift between Wave 80 FE page (shipped Wave 80, sends 2 fields) and Wave meta-6 BE (shipped 2026-05-27 PR #1904, requires 3 fields including role). 

api-contract-audit Wave meta-6 (PR #1907, 94/100) didn't catch — audit scope BE-only:
- Verified: BE controller @RequestMapping URL ↔ api-contract.md endpoint match ✅
- Verified: BE DTO schema ↔ api-contract.md schema match ✅
- **NOT verified**: FE call sites payload shape ↔ BE DTO required fields

## Proposed Fix

### Bucket A — Inline scope (FE fix)

Update `InviteStaffPage`:
1. Add role state + UI dropdown (TEACHER / MANAGER / role enum per Wave meta-6 BR-STAFF-INVITE-003)
2. Include role trong request body `apiClient.post(endpoints.staffInvitations.create, {email, fullName, role})`
3. Default role = TEACHER (most common Owner-invites-teacher use case)

### Bucket B — Meta: extend api-contract-audit skill

`audit-skill-rubric-api-contract-audit.md` Cat 2 Request Schema audit:
- Current: BE DTO field/type match doc
- Extension: grep FE call sites for endpoint → parse payload shape → diff vs BE DTO required fields → FAIL if FE missing required field

Per `meta-gap-priority.md` §3 — META P0 force-multiplier (fix audit skill 1 lần → catch ALL future FE-BE drift).

## Current State (verified 2026-06-02 — fix-time state-check per audit-to-gap-pipeline.md §2.8)

Gap diagnostic **không khớp codebase thực tế**. Empirical state-check Wave local-doable-12 Bucket B:

- BE DTO là `CreateStaffInvitationRequest` (KHÔNG phải `InviteStaffRequest`), chỉ có `{email, fullName}` — **KHÔNG có `role` field, KHÔNG có `@NotBlank role` validation** (`kitehub-subscription/.../staff/dto/CreateStaffInvitationRequest.java:23-33`).
- Entity `StaffInvitation` **KHÔNG có column `role`**; role hardcode `"STAFF"` lúc accept (`StaffInvitationController.java:238`).
- Service signature `service.create(tenantId, ownerId, email, fullName)` — không nhận role.
- MSW handler request type chỉ `{email, fullName}`; response role luôn `'STAFF'` (`staff-invitations.ts:30-31,43-45`).
- Business doc `documents/01-business/roles/api-contract.md:9,19-20` chốt **Phase 1 BETA = 2-role MVP (OWNER + STAFF)**; staff invitation → STAFF only. TEACHER/MANAGER = Phase 2+ scope.

→ Diagnostic "BE requires role not blank → 400 VALIDATION_ERROR" **SAI**. Thêm `role` vào body sẽ là field thừa (Jackson ignore, no effect) + vi phạm Phase 1 BETA 2-role scope.

**FE bug thực tế:** page có dropdown 3-option TEACHER/STAFF/MANAGER (default STAFF) → expectation mismatch (user chọn "Giáo viên" nhưng BE luôn tạo STAFF). Fix đúng = bỏ dropdown gây hiểu lầm, thay bằng read-only field minh bạch "Nhân viên trung tâm (STAFF)" + note Phase 1 BETA.

## Acceptance Criteria

- [x] FE InviteStaffPage role affordance khớp BE 2-role MVP (bỏ dropdown 3-option gây hiểu lầm, thay read-only "Nhân viên trung tâm (STAFF)" + note Phase 1 BETA)
- [x] Request body khớp BE contract `{email, fullName}` (KHÔNG gửi `role` thừa)
- [x] `pnpm --filter kitehub-frontend build` PASS local (prerender clean)
- [ ] Owner submit invite → 201 + STAFF user created (live walk — DEFER stack down per feature-ship-runtime-walk-mandate §5)
- [ ] (Phase 2+) api-contract-audit skill extension cho FE-BE drift detection (separate META gap — out of Bucket A scope)

## Related

- RST artifact: `documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-human-walk-rst.md` Finding #7 + Class B
- Wave meta-6 BE PR #1904 (StaffInvitationController)
- Wave meta-6 api-contract audit PR #1907 (94/100 — missed FE-BE drift)

## Log

- **2026-05-28** — Found qua Wave meta-6 RST walk. Bucket A FE-only fix ~15 phút unblock walk + paired E2E spec per `e2e-rst-test-layer-boundary.md` §3. Bucket B meta scope (audit skill extension) — separate gap if pursuit.
- **2026-06-02** (Wave local-doable-12 Bucket B) — Fix-time state-check (per `audit-to-gap-pipeline.md` §2.8) revealed gap diagnostic SAI: BE `CreateStaffInvitationRequest` chỉ có `{email, fullName}`, KHÔNG có role field/validation; Phase 1 BETA = 2-role MVP (STAFF hardcode). Scope revised: KHÔNG thêm role param (sẽ thừa + vi phạm scope). FE fix = bỏ dropdown 3-option TEACHER/STAFF/MANAGER gây expectation mismatch → read-only "Nhân viên trung tâm (STAFF)" + note Phase 1 BETA. `pnpm --filter kitehub-frontend build` PASS (✓ Compiled + 90/90 static, exit 0). Status → PARTIAL: FE code done; live 201 walk DEFER (stack down) per `feature-ship-runtime-walk-mandate.md` §5. Bucket B audit-skill extension = separate META gap (out of inline scope).
