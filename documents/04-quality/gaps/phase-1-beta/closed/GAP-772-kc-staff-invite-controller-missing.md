---
audience: dev
---

# GAP-772 — KC staff invite controller missing (Mảng B13 + C blocker)

**Status:** 🟢 DONE — SUPERSEDED (scope relocated to kitehub-subscription per GAP-786 Wave A Bucket B 2026-05-28)
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-05-27 (Wave 106 RST Mảng B13 + Mảng C catalog probe)
**Affects:** B13 Owner mời nhân viên + C1 Staff nhận thư mời + C2 Staff đăng nhập (cascade)
**Phase:** phase-1-beta

## Problem

Wave 106 plan §3 mô tả:
- B13 "Mời Nhân viên qua thư + xem trạng thái lời mời"
- C1 "Nhân viên nhận thư mời → đăng ký tài khoản"

Catalog probe `grep @RequestMapping` trong `kiteclass/kiteclass-core/src/main/java/**/*Controller.java` ra:

```
@RequestMapping("/api/v1/parent-invitations")    # PARENT only
# KHÔNG có /api/v1/staff/invitations OR /api/v1/staff/invite
```

Toàn bộ B13 luồng + Mảng C (3 luồng C1+C2+C3) blocked tại BE layer — không có endpoint nào cho Owner gửi invite cho Staff.

Đối chiếu users table: `staff.test@test.vn` đã seed với role STAFF (đăng nhập 200), nhưng không có code path để Owner provision STAFF role through invite flow.

## Root Cause

Suy đoán: Phase 1 BETA scope decision deferred staff invite implementation. Plan Đợt 106 surface bị ẩn vì luồng B13/C tham chiếu placeholder.

## Proposed Fix

Option A — Implement: `StaffInviteController` + entity + invite token + email template + claim endpoint + Owner role-guard. Mirror `parent-invitations` pattern (đã có code template).

Option B — Defer Phase 1.5+: Update plan §3 đánh dấu B13/C scope explicit out-of-scope Phase 1 BETA + sửa B12 (Cài đặt) gộp invite UI placeholder "coming soon".

## Resolution — SUPERSEDED by GAP-786 (Wave A Bucket B, 2026-05-28)

State-check 2026-06-01 (Wave beta-readiness-9 Bucket B, per `audit-to-gap-pipeline.md` §2.8 fix-time state-check + `outside-in-coverage-trigger.md` §2.1 architecture-decision keyword): kiểm tra empirical codebase trước khi build → phát hiện scope của gap này **đã bị đảo ngược + relocate** sang `kitehub-subscription` (KHÔNG phải kiteclass-core).

Tóm tắt diễn biến:

1. **Wave meta-6 Bucket A (V71, GAP-772 original scope)** — đã ship staff invitation MVP trong kiteclass-core, nhưng dính Bug #17 (accept không create user).
2. **Wave A Bucket B (V72, GAP-786, 2026-05-28)** — Day 1 investigation phát hiện kiteclass-core (DB `kiteclass_dev` port 5432) **không thể share JPA `UserRepository`** với kitehub-subscription (DB `kitehub` port 5433); 3 options gốc (A direct inject / B outbox / C sync HTTP) đều non-viable vì kitehub-platform là shared library (no HTTP server). User-confirmed reversal (Option D): **kitehub-subscription là canonical staff-invitation owner** (production-proven từ Wave 80).
3. Wave A Bucket B đã: revert gateway routing `/api/v1/staff-invitations/**` → kitehub-subscription, **xóa toàn bộ staff module trong kiteclass-core** (13 files), thêm V72 deprecate `staff_invitations` table trong kiteclass-core. RST walk PASS end-to-end 5 bước.

→ Build staff-invite trong kiteclass-core như scope gốc của GAP-772 sẽ **tái lập đúng cross-DB Bug #17** mà GAP-786 đã tốn ~2-3 eng-days để đảo ngược. GAP-772 là OPEN-stale survivor của reversal đó (filed 2026-05-27 Wave 106, trước reversal 2026-05-28; chưa được close khi GAP-786 đóng).

**Verdict per `audit-to-gap-pipeline.md` §2.8 decision matrix:** "Symptom no longer present (superseded) → flip DONE with findings; NO build PR." Build = over-engineering anti-pattern mà rule §2.8 + `outside-in-coverage-trigger.md` được tạo ra để chặn.

### Canonical implementation đang vận hành (verified 2026-06-01)

| Layer | Location | Evidence |
|---|---|---|
| BE controller | `kitehub-subscription/.../staff/controller/StaffInvitationController.java` @ `/api/v1/staff-invitations` | create/list/resend + public `GET /by-token/{token}` + public `POST /{token}/accept` (accept **tạo user**, dòng 244) + audit |
| BE service | `kitehub-subscription/.../staff/service/StaffInvitationService.java` + `InvitationTokenService.java` | token issue/validate + SHA-256 hash |
| BE entity/repo/migration | `staff/entity/StaffInvitation.java`, `StaffInvitationRepository.java`, `V45__create_staff_invitations.sql` + `V49__create_staff_invitation_audit_log.sql` + `V46__create_rbac_roles.sql` | — |
| BE tests | `StaffInvitationServiceTest.java`, `InvitationControllerIntegrationTest.java`, `StaffInvitationEmailDispatchTest.java` | — |
| FE accept route | `kitehub-frontend/src/app/(public)/staff/accept-invite/page.tsx` | preview qua by-token + password-set form (A07 ≥12 ký tự + mixed-case + digit) → accept |
| FE Owner invite UI | `kitehub-frontend/src/app/(admin)/admin/staff/invite/page.tsx` | — |
| Gateway routing | `kitehub-gateway/.../application.yml:584-608` | public-token route + Owner-scoped TenantResolver route (GAP-790 DONE) |

## Acceptance Criteria

- [x] Decision logged: ~~Option A/B~~ → Option D (re-host kitehub-subscription) per GAP-786; logged in §Resolution
- [x] Canonical `StaffInvitationController` + DB schema + email + claim flow tồn tại + RST walk PASS — trong kitehub-subscription (per GAP-786 walk evidence, NOT kiteclass-core)
- [x] kiteclass-core scope đánh dấu superseded — staff module đã xóa Wave A Bucket B + table deprecated V72

## Related

- **Supersedes this gap:** GAP-786 (Wave A Bucket B re-host kitehub-subscription canonical) — `closed/GAP-786-staff-invite-accept-user-provision-missing.md`
- GAP-790 (gateway staff-invitations route + TenantResolver) — DONE 2026-05-28
- Wave 106 plan §3 B13 + C1-C3 — `documents/03-planning/waves/wave-2026-05-23-106-rst-phase-1-beta-full-walk.md`
- Migration trail: `kiteclass-core/.../V71__create_staff_invitations.sql` (MVP) + `V72__deprecate_staff_invitations_table.sql` (deprecation)
- Sister: GAP-773 (FE route — closed cùng PR này, same superseded reason)

## Log

- **2026-06-01** (Wave beta-readiness-9 Bucket B) — flip 🟢 DONE SUPERSEDED. State-check empirical (per `audit-to-gap-pipeline.md` §2.8) trước khi build phát hiện scope kiteclass-core đã bị reversal sang kitehub-subscription bởi GAP-786 (Wave A Bucket B, 2026-05-28). Canonical impl đầy đủ + vận hành (controller + service + entity + 3 migration + RBAC + tests + FE route `/staff/accept-invite` + Owner invite UI + gateway routing GAP-790). Build trong kiteclass-core như scope gốc sẽ tái lập cross-DB Bug #17 đã được GAP-786 đảo ngược. NO build PR — flip DONE với findings per §2.8 decision matrix. Không có in-scope code AC nào còn lại cho kiteclass-core (module đã xóa). git mv → phase-1-beta/closed/.
