# GAP-597: P2 invite management — resend / revoke / 24h reminder

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (defer Wave 87)
**Domain:** Backend / Frontend
**Phase:** phase-1-beta
**Found:** 2026-05-15 (Wave 86 Bucket A persona-outside-in audit cell 3.5)
**Affects:** P2 Center Owner invite management UX

## Problem

Persona cell 3.5 (chị Hằng P2 failure recovery):
- Invite Manager email typo → cần resend với edited email
- Manager không xác nhận trong 24h → P2 cần nhận reminder
- Cần revoke invite path nếu mời nhầm người

Wave 86 không cover invite management UX. Hiện chỉ có create-invite path.

## Root Cause

InviteService scope minimal (create only). FE dashboard không có invite management table.

## Proposed Fix

1. **Backend invite management endpoints**:
   - `GET /api/v1/invites?status=pending` — list pending invites cho tenant
   - `POST /api/v1/invites/{id}/resend` — resend với optional edited email
   - `DELETE /api/v1/invites/{id}` — revoke invite
2. **Cron reminder job** `kitehub-email/jobs/InviteReminderJob.java`:
   - Daily 9am VN time scan pending_invites where created_at < NOW() - 24h AND reminded_at IS NULL
   - Send reminder email cho P2 owner: "Manager [name] chưa accept invite"
   - Update reminded_at
3. **FE invite management** `kitehub-frontend/src/app/(p2)/team/invites/page.tsx`:
   - Table: email / role / status (pending/accepted/expired) / created_at / actions (resend, revoke)
   - Inline edit email cho resend
4. **Notification**: Manager không accept → P2 nhận reminder Day 3 + Day 7

## Acceptance Criteria

- [ ] 3 backend endpoints shipped
- [ ] Cron reminder job shipped + tested
- [ ] FE invite management page shipped
- [ ] Resend với edit working
- [ ] Revoke marks invite invalid (token unusable)
- [ ] Defer to Wave 87

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-persona-outside-in.md` §3.3 cell 3.5 + §6 NEW gap proposal #8
- Wave 87 scope (defer)
