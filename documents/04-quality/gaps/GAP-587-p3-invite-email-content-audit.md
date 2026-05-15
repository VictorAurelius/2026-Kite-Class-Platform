# GAP-587: P3 invite email content audit — P2 owner name + center context + role explicit

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend / Email / Content
**Phase:** phase-1-beta
**Found:** 2026-05-15 (Wave 86 Bucket A persona-outside-in audit cell 4.1)
**Affects:** P3 Manager invite email (P2-to-P3 invite flow)

## Problem

Persona cell 4.1 (anh Tâm — P3 Manager invited bởi chị Hằng P2): P3 invite email từ chị Hằng KHÔNG phải từ `support@kitehub.me`. Click → cần biết:
- "Ai mời tôi?" — P2 owner name explicit (chị Hằng)
- "Trung tâm nào?" — center context (Trung tâm Anh ngữ Sky Education)
- "Vai trò gì?" — role explicit (Manager với quyền điểm danh + nhập điểm, KHÔNG có quyền billing)

Missing context = P3 confusion → spam-folder rate tăng → invite acceptance damage. Wave 86 không verify P3 invite email content audit.

## Root Cause

Wave 86 Bucket G scope cover "invite send mechanism" generic, không split P1 vs P2 vs P3 cohort email templates.

## Proposed Fix

1. **P3 invite email template** `resend/templates/beta-invite-p3-manager.html`:
   - Subject: "[Tên P2 owner] mời bạn làm Manager tại [Tên trung tâm]"
   - Sender: `support@kitehub.me` reply-to=[P2 owner email] (P3 reply goes to P2)
   - Body opening: "Chào anh Tâm, chị Hằng (chủ Trung tâm Anh ngữ Sky Education) đã mời anh làm Manager"
   - Role disclosure explicit:
     - "Bạn có quyền: nhập điểm, điểm danh, xem lịch lớp"
     - "Bạn KHÔNG có quyền: xóa lớp, sửa giá, xem doanh thu"
   - Permission matrix link → `/help/p3-manager/permissions`
   - Magic link accept invite
   - Footer "Nếu chưa nhận lời mời, bạn có thể từ chối hoặc bỏ qua email này"
2. **Backend invite trigger** `InviteService.sendP3ManagerInvite()`:
   - Pull P2 owner.name + center.name vào email context
   - Set reply-to = P2 owner email cho support escalation chain
3. **Self-test send** to internal QA email với sample P2 "chị Hằng" + P3 "anh Tâm"

## Acceptance Criteria

- [ ] P3 invite template separate từ P1/P2 template
- [ ] Email body cite P2 owner name + center name + role explicit
- [ ] Permission matrix link working
- [ ] reply-to set correctly to P2 owner email
- [ ] Mail-Tester score ≥ 8/10
- [ ] Wave 86 Bucket G — first P3 invite (chờ P2 chị Hằng invite) uses này

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-persona-outside-in.md` §3.4 cell 4.1 + §4 rank 3 + §6 NEW gap proposal #3
- Wave 86 plan §3 Bucket G AC G-AC5 (paired)
- GAP-586 P1 invite email (sibling)
- C-AC2 P3 first-login permission matrix (paired Bucket C)
