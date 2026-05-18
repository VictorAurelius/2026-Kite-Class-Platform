# GAP-561: invite-staff email template + BE endpoint missing → P3 Manager flow blocker

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Mixed
**Detected:** 2026-05-14
**Related Audits:** `documents/04-quality/audits/persona-review/2026-05-14-pre-wave-79-outside-in.md` (Persona 2 N2-P0)
**Related Gaps:** GAP-562 (RBAC role separation — sister; both block P3 Manager flow)

## Current State (verified 2026-05-14)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Email template `invite-staff.html` | `kitehub/kitehub-email/src/main/resources/templates/emails/invite-staff.html` | ❌ missing |
| 17 email templates ship | welcome / beta-invite / beta-request-confirmation / email-verification / data-retention-warning / data-retention-final-warning / data-deleted / dsar-acknowledgement-requester / dsar-new-ticket-dpo / onboarding-tips / subscription-* (5) / trial-* (3) | ✅ shipped |
| BE endpoint `POST /api/v1/tenant/invite-staff` | unknown — likely missing | ⚠️ verify required |
| FE "Invite teammate" UI | `kitehub-frontend/src/app/(customer)/settings/team/page.tsx` hoặc tương tự | ❌ likely missing (Sidebar customerNav không có Team entry) |
| Tenant member role enum | DB schema `tenant_users.role` | ⚠️ verify required |

**Grep commands run:**
```bash
find kitehub/kitehub-email/src/main/resources/templates/emails -iname "*invite*staff*" -o -iname "*staff*invite*" -o -iname "*invite*member*"
# Result: 0 hits — only beta-invite.html exists (different scope — beta access invite)
find kitehub/kitehub-frontend/src/app -iname "*team*" -o -iname "*staff*"
# Result: TBD — verify
grep -rn "InviteStaff\|inviteStaff\|invite-staff" kitehub/kitehub-subscription/src/main/java 2>/dev/null
# Result: TBD — likely 0
```

## Problem

P3 Center Manager flow (manager được P2 Owner invite vào tenant để cùng vận hành trung tâm) là **core multi-user use case Phase 1 BETA**. Per persona walkthrough 2026-05-14 outside-in audit Persona 2:
- P3 Manager flow **không thể start** vì `invite-staff` email template + BE endpoint + FE UI đều missing
- P2 Owner login → Settings → KHÔNG có "Team" tab → không thể invite manager
- Beta invite cohort thực tế: 5-10 trung tâm × avg 3 nhân viên = 15-30 P3 Manager users → tất cả blocked

So với 2026-05-14 audit cũ (PR-THUY-7) đã flag tương tự cho P2 Center Owner. Inside-out queue Wave 79 KHÔNG có gap cover invite-staff flow.

Hậu quả:
- P2 Owner login solo → thấy 1 user account → "tôi và nhân viên dùng chung tài khoản?" → bad practice (audit trail mất, RBAC fail)
- Multi-user trung tâm fail → giảm ARPU + retention
- Compete vs Misa/Smile (đều có team invite mặc định)

## Context

Outside-in audit Persona 2 Anh Tâm (Center Manager) walkthrough Bước 1 confirm: `invite-staff.html` thực sự không tồn tại. Inside-out queue tập trung internal hardening (auth/feedback/support); KHÔNG ai trong dev brainstorm flag multi-user flow vì dev tự test với single PLATFORM_ADMIN account.

Reference business doc: `documents/01-business/kitehub/auth/` rules.md likely chưa mention tenant-member invite flow.

## Evidence

- `kitehub-email/src/main/resources/templates/emails/` listing 17 files (xác nhận trong report) — KHÔNG có invite-staff
- `Sidebar.tsx` customerNav 4 entries (Tổng quan / Thanh toán / AI Branding / Cài đặt) — KHÔNG có Team
- Sister audit 2026-05-14 PR-THUY-7 đã list Center Owner cần "Invite teacher/staff" → P0 outstanding

## Proposed Fix

### Backend (kitehub-subscription)

1. **Add DB migration** `V[N]__create_tenant_member_invitations.sql`:
   ```sql
   CREATE TABLE tenant_member_invitations (
     id UUID PRIMARY KEY,
     tenant_id UUID REFERENCES tenants(id),
     invited_email VARCHAR(320),
     invited_role VARCHAR(50), -- MANAGER, TEACHER, ACCOUNTANT
     invited_by_user_id UUID,
     invite_token UUID UNIQUE,
     claim_code CHAR(6),
     status VARCHAR(20), -- PENDING, ACCEPTED, EXPIRED, REVOKED
     expires_at TIMESTAMP,
     created_at TIMESTAMP DEFAULT NOW()
   );
   ```
2. **Add controller** `TenantMemberController` với:
   - `POST /api/v1/tenant/members/invite` — Owner-only; body: email + role + optional message
   - `GET /api/v1/tenant/members` — list active + pending invites
   - `POST /api/v1/tenant/members/invite/{token}/accept` — staff click email link
   - `DELETE /api/v1/tenant/members/{id}` — Owner remove member
3. **Add business doc** `documents/01-business/kitehub/tenant-membership/rules.md` + `use-cases.md` + `api-contract.md` (3-layer per CLAUDE.md mandate)

### Email (kitehub-email)

4. **Add template** `invite-staff.html` theo pattern `beta-invite.html`:
   - Header: "Bạn được mời tham gia [tên trung tâm] trên KiteHub"
   - Body: Owner name + role invited + 6-digit claim code (per GAP-388 secure token pattern)
   - CTA: "Chấp nhận lời mời" → `/auth/accept-invite?token=...`
   - Expiry: 7 days
5. **Wire** EmailServiceClient với event `tenant.member.invited`

### Frontend (kitehub-frontend)

6. **Add Sidebar nav** `customerNav` entry "Thành viên" → `/settings/team` (Owner-only conditional render)
7. **Create page** `src/app/(customer)/settings/team/page.tsx`:
   - List active members (name + email + role + invited date)
   - List pending invites (email + role + claim code + revoke action)
   - Form "Mời thành viên mới" — email + role dropdown (MANAGER/TEACHER/ACCOUNTANT) + optional message + submit
8. **Create page** `src/app/(auth)/accept-invite/page.tsx`:
   - Read token from query string → fetch invite details
   - Show "Bạn được mời tham gia [tenant name]" + Owner name
   - Set password form (P3 first login) OR redirect to login (existing user)

### Acceptance Criteria

- [ ] DB migration `V[N]__create_tenant_member_invitations.sql` shipped
- [ ] 4 endpoints active: invite / list / accept / remove
- [ ] Business docs 3-layer: `documents/01-business/kitehub/tenant-membership/rules.md` + use-cases.md + api-contract.md
- [ ] Email template `invite-staff.html` ship + render với brand variables
- [ ] EmailServiceClient event `tenant.member.invited` wired
- [ ] FE Sidebar entry "Thành viên" Owner-only
- [ ] FE pages `/settings/team` + `/auth/accept-invite` shipped với Playwright E2E
- [ ] Persona test: P2 Owner login → Settings → Team → invite anh Tâm@email.com role=MANAGER → email arrives → Tâm click → set password → login → see customer dashboard (NOT admin)
- [ ] Cross-check với GAP-562 RBAC: invited Manager KHÔNG access /billing /branding scope

## Related

- GAP-562 (RBAC role separation — sister; must ship together để Manager login an toàn)
- GAP-554 (Onboarding tenant header JWT cross-check — partial overlap, multi-user JWT scope)
- GAP-388 (claim code 6-digit pattern Wave 36) — reuse design
- Audit: `documents/04-quality/audits/persona-review/2026-05-14-pre-wave-79-outside-in.md`
- Sister audit 2026-05-14 phase-1-beta-walkthrough PR-THUY-7

## Log

- **2026-05-15 (Wave 80 Bucket B):** 🟢 DONE 100% (upgrade from PARTIAL 50%) — GAP-561b closed in this wave via wave-80-b/invite-staff-flow branch: email templates + `InvitationTokenService` HMAC + real InvitationController endpoints (replaces 501 stubs) + audit log table V49 + FE routes (`/admin/staff`, `/admin/staff/invite`, `/staff/accept-invite`) + 9 BE integration tests + Playwright E2E + smoke-email template variant. See [GAP-561b](GAP-561b-invite-staff-email-template-and-fe-routes.md) §Log.
- **2026-05-14:** PARTIAL 50% — Wave 79 Bucket B closure. V45__create_staff_invitations.sql + StaffInvitation entity + InvitationController skeleton stubs (501 NOT_IMPLEMENTED) shipped via PR #1366. Email template + FE invite-staff UI + actual implementation deferred to GAP-561b (Wave 80+). KitehubSubscriptionApplication + KiteHubAdminApplication @EntityScan extended.

- 2026-05-14 — Filed via Wave 79 outside-in audit (Persona 2 N2-P0). State-check confirmed all 4 layers (DB / BE / Email template / FE UI) missing. Priority P0 vì block Phase 1 BETA P3 Manager flow ngay từ ngày đầu invite cohort.

- **2026-05-15:** PARTIAL 50% → DONE 100% — Wave 80 Bucket B shipped via GAP-561b PR #1383: invite-staff email templates (vi-VN .html + .txt) + InvitationTokenService HMAC-SHA256 TTL 7d + `@PostConstruct` fail-fast + 5 endpoint real impl (replace 501 stubs) + idempotent re-invite (revoke-old + create-new) + V49 audit log migration + 3 FE routes (`/admin/staff` list + `/admin/staff/invite` form + `/staff/accept-invite` public landing với Suspense boundary) + 9 BE integration tests (testcontainers) + 4 Playwright E2E scenarios + smoke test extension `--template invite-staff`. 630/630 BE module + 738/738 FE module tests pass post-fix.
