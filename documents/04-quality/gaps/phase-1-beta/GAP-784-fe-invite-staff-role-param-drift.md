---
audience: dev
---

# GAP-784 — FE InviteStaffPage role param missing — Wave 80 FE vs Wave meta-6 BE drift

**Status:** 🔵 OPEN
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

## Acceptance Criteria

- [ ] FE InviteStaffPage role dropdown UI added
- [ ] Owner POST `{email, fullName, role}` → 201 (post-GAP-783 fix)
- [ ] api-contract-audit skill extension proposed (separate META gap)

## Related

- RST artifact: `documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-human-walk-rst.md` Finding #7 + Class B
- Wave meta-6 BE PR #1904 (StaffInvitationController)
- Wave meta-6 api-contract audit PR #1907 (94/100 — missed FE-BE drift)

## Log

- **2026-05-28** — Found qua Wave meta-6 RST walk. Bucket A FE-only fix ~15 phút unblock walk + paired E2E spec per `e2e-rst-test-layer-boundary.md` §3. Bucket B meta scope (audit skill extension) — separate gap if pursuit.
