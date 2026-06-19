# GAP-894: Per-tenant tables không có FK tới `instances` — audit orphan + integrity check thủ công

**Status:** ⚪ WONTFIX (wave-gap-audit-p1-1 2026-06-19 — current state intentional per OWASP A09 retention design; logical reference convention accepted, no FK-to-`instances` by design)
**Priority:** 🟡 P2
**Domain:** Backend / DB / Compliance
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KH auth/user/instance)
**Affects:** `kitehub-platform` tables `onboarding_progress`, `staff_invitations`, `staff_invitation_audit_log`, `impersonation_audit_log`

## Problem

4 bảng dùng `tenant_id UUID` trỏ logic tới `instances.id` nhưng KHÔNG khai báo FK constraint. Lý do interpolate: bảng thiết kế chịu được khi `instances` PURGE — FK CASCADE sẽ xóa audit trail (vi phạm OWASP A09 retention 7 năm).

Hậu quả: audit row trở thành "orphan" sau khi instance PURGED → cần documented purge policy. Rename `instances.id` (hiếm) → integrity check thủ công.

`migration_outbox.instance_id` lại CÓ FK thật — khác biệt: transient (publish xong xóa), không phải audit trail 7 năm.

## Proposed Fix

Document purge policy formal trong runbook + add integrity check script chạy định kỳ (orphan tenant_id detection). Cân nhắc soft FK với ON DELETE SET NULL thay vì CASCADE.

## Acceptance Criteria

- [ ] Runbook documented purge policy
- [ ] Integrity check script cho orphan tenant_id
- [ ] Reference cluster doc KH 01-auth-user-instance §A4

## Discovered in

`documents/02-architecture/database/kitehub/01-auth-user-instance.md` §A4
