# GAP-577: Platform admin hardening — MFA mandatory + IP allowlist + 30min session + immutable admin audit

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (chặn GA Phase 2 — admin role = highest privilege, catastrophic blast radius)
**Domain:** Backend / Security
**Found:** 2026-05-15 (Wave 85 Bucket A persona outside-in audit cell 4.3)
**Affects:** Platform admin role authentication + session management; `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-85-persona-outside-in.md` §3.4 cell 4.3

## Problem

Wave 85 inside-out scope không cover hardening cho platform admin role. Bucket A persona audit cell 4.3 surface critical gap:

- Admin role = highest-privilege (cross-tenant access + investigation + tenant support).
- Hiện tại admin login chỉ password + JWT — KHÔNG có MFA, KHÔNG IP allowlist, session TTL default 24h, admin audit logs có thể UPDATE/DELETE bởi chính admin.
- Catastrophic blast radius nếu admin credential leak: attacker có thể access mọi tenant data + clean up audit trail.

Persona Mai (Platform Admin) expect: MFA mandatory + IP allowlist (KiteHub office + VPN ranges only) + session timeout ≤30min idle + admin audit log immutable (append-only, even admin không thể delete own log).

## Root Cause

- Phase 1 BETA scope focus tenant-side security (RLS Wave 85 Bucket B). Admin-side hardening = inside-out blind spot (no canonical gap until Bucket A outside-in audit caught it).
- Wave 80 RBAC FE/BE PARTIAL — admin role exists nhưng KHÔNG có per-role auth hardening tier.

## Proposed Fix

Wave 86 scope (4 sub-tasks):

1. **MFA mandatory cho admin role** — TOTP enroll trên first login; cannot dismiss; backup codes generated.
2. **IP allowlist** — config `kitehub.admin.ip-allowlist` (CIDR list); admin login từ IP ngoài list → reject 403 + alert via SNS.
3. **Session TTL 30min idle** — JWT refresh-token rotation cho admin tier shorter than tenant default; idle >30min → force re-auth.
4. **Immutable admin audit log** — Wave 85 Bucket B B-AC7 ships `admin_audit_logs` table với RLS policy chặn UPDATE/DELETE cho mọi role. GAP-577 sub-task = FE dashboard cho admin to VIEW audit log (read-only); add retention policy 3 năm per PDPL Art 11.

## Acceptance Criteria

- [ ] MFA mandatory cho admin role — TOTP setup flow + verify trên mọi login
- [ ] IP allowlist config + reject 403 + SNS alert khi IP outside list
- [ ] Admin session TTL 30min idle (separate config từ tenant default)
- [ ] Backup codes generation + recovery flow tested
- [ ] FE admin audit log dashboard (read-only, paginated, filter by date/action)
- [ ] PDPL Art 11 compliance — retention policy 3 năm documented + enforced
- [ ] Integration test: admin login từ IP outside list → 403 + SNS event captured
- [ ] Integration test: admin session idle 31min → JWT invalidated + force re-auth
- [ ] Pre-handoff verify per `pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist

## Related

- Wave 85 Bucket A persona audit: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-85-persona-outside-in.md` cell 4.3 + 4.5
- Wave 85 Bucket B B-AC7 (admin_audit_logs table immutability) — prerequisite for FE dashboard sub-task
- Wave 86 scope (planned)
- `pre-launch-auth-hardening-checklist.md` (parent rule for auth tier)
- GAP-578 (sister P0 — P2 owner 2FA mandatory)

## Log

- **2026-05-15** Filed via Wave 85 Bucket A persona outside-in audit integration (PR wave-85-bucket-a-integration). Defer Wave 86 — Wave 85 scope locked + this is admin-tier scope (separate persona). Status OPEN. Will be tracked trong ROADMAP §🚀 Next Action Wave 86 section.
- **2026-05-18 — Cross-ref Wave 93 GAP-625 KYC dependency** per Wave 93 re-triage audit (`documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-26-gaps-re-triage.md`). GAP-625 (Phase 1.5a P0 — Owner KYC + multi-tenant binding + immutable mark-paid audit log) shares **immutable audit log infrastructure** với GAP-577 admin_audit_logs immutability (Wave 85 Bucket B B-AC7 prerequisite). Recommended ordering: GAP-625's immutable log infrastructure ships first Phase 1.5a → GAP-577 admin_audit_logs leverages same Postgres trigger-enforced pattern + audit table schema. Cross-ref complementary, NOT duplicate (admin-tier vs Owner-tier — different actors, shared infra). Sequential dependency tracked in wave plan §6.
