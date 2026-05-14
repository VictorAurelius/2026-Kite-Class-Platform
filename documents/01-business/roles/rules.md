# RBAC Roles — Business Rules

**Domain:** Role-Based Access Control role definitions + scope (Wave 79 Bucket B — GAP-562 Phase 1 MVP 2-role)
**Last verified:** 2026-05-14 (Wave 79 Bucket 0 Foundation)
**Config prefix:** `kitehub.rbac`

File này document business values cho 2-role MVP (OWNER + STAFF) Phase 1 BETA. Manager + Teacher + Accountant + Receptionist roles deferred Wave 80+. Mỗi rule có 5 attributes theo `.claude/rules/business-logic-review.md` §2.

> **Wave 79 Bucket B context:** Hiện tại tenant chỉ có 1 role per user (`PLATFORM_ADMIN` + `ADMIN` FE alias per Wave 78 GAP-518). Wave 79 Bucket B introduce 2-role tenant scope: OWNER (full Customer scope) + STAFF (restricted Customer scope; KHÔNG thấy billing/branding/AI Branding). Backward-compat alias `PLATFORM_ADMIN ↔ ADMIN ↔ OWNER` 30 ngày.

---

## BR-ROLE-001 — Phase 1 MVP 2 roles: OWNER + STAFF

- **Value:** Phase 1 BETA hỗ trợ ĐÚNG 2 tenant roles:
  - `OWNER` — đại diện tenant (Customer chính), full access to Customer scope (dashboard, billing, branding, AI Branding, settings, user management, all data).
  - `STAFF` — staff member của tenant, restricted access: KHÔNG thấy billing, KHÔNG thấy branding/AI Branding (Owner-only), thấy core operational data (students/classes/schedules nếu KiteClass instance đã provision).
- **Source:** Wave 79 plan §1 Brainstorm Q2 trade-off decision — "MVP 2-role vs full matrix (Owner/Manager/Teacher/Accountant/Receptionist) → chọn MVP 2-role Phase 1; full matrix Wave 80+".
- **Rationale:** Phase 1 BETA cohort 5-10 tenant, organizational complexity thấp. Full 5-role matrix tăng implementation cost 3-5x (per-role permission table + UI tabs ẩn-hiện per role) trong khi outside-in persona audit (Wave 78) chỉ surface 2 distinct personas (P2 Owner + P3 Manager). Manager role hiện được merge vào OWNER cho Phase 1 — STAFF cover các non-owner persona. Khi P3 cohort (medium-center 5-15 staff) reach >5 tenants → unlock Manager role Wave 80+.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-14). Formal persona review queued post-Wave-79.
- **Compliance check:** N/A — role taxonomy là internal access control, không phải PII.
- **Review cadence:** Quarterly. **Next review:** 2026-08-14. Event triggers: Phase 2 trigger reached (≥5 beta tenant live + Quality audit ≥80) → revisit role expansion.
- **Code reference:** (planned Wave 79 Bucket B) `users.role` column enum + Spring Security `@PreAuthorize("hasRole('OWNER')")` / `@PreAuthorize("hasAnyRole('OWNER','STAFF')")` annotations.

## BR-ROLE-002 — Backward-compat alias PLATFORM_ADMIN ↔ ADMIN ↔ OWNER (30 ngày)

- **Value:** Tenant users hiện có role `PLATFORM_ADMIN` (BE seed) hoặc `ADMIN` (FE guard per Wave 78 GAP-518) sẽ được migrate sang `OWNER` qua Flyway V46. Trong window 30 ngày (Wave 79 launch → 2026-06-14):
  - JWT issued với claim `role=PLATFORM_ADMIN` vẫn pass `@PreAuthorize("hasRole('OWNER')")` (alias mapping trong custom `RoleHierarchy` bean).
  - FE role guard accept cả `'PLATFORM_ADMIN'`, `'ADMIN'`, `'OWNER'` cho cùng Owner UI surface.
  - Sau 30 ngày: alias logged WARN; sau 90 ngày remove alias entirely.
- **Source:** GAP-518 (Wave 71b admin role mismatch incident) + GAP-562 acceptance criteria.
- **Rationale:** Wave 71b ship `PLATFORM_ADMIN` BE seed; Wave 78 GAP-518 add FE `'ADMIN'` alias để fix immediate UI redirect issue. Wave 79 Bucket B introduce semantic correct role `OWNER` (tenant scope) vs `PLATFORM_ADMIN` (cross-tenant scope — superuser). 30-ngày alias ensure existing beta tenant sessions không break + zero downtime migration. Per `pre-handoff-self-test-completeness.md` §2.4 (admin flow) — role name consistency BE+FE guarded.
- **Reviewer:** @nguyenvankiet (acting Product Owner + Security scout, solo-dev, 2026-05-14).
- **Compliance check:** N/A.
- **Review cadence:** Once at 2026-06-14 (enforce alias deprecation). Default Quarterly.
- **Code reference:** Wave 79 Bucket B — V46 migration + `RoleHierarchy` Spring Security config + FE `RoleGuard` component.

## BR-ROLE-003 — STAFF scope restrictions

- **Value:** STAFF role hidden from access:
  - Billing pages (`/dashboard/billing/*`, `/dashboard/subscription/*`)
  - Branding settings (`/dashboard/branding/*`)
  - AI Branding workflow (`/dashboard/ai-branding/*`)
  - User management (`/dashboard/staff/*` — invite/revoke staff is Owner-only)
  - Tenant settings + DNS / domain (`/dashboard/settings/domain/*`)
  - Sensitive financial reports
- STAFF visible: core dashboard, students/classes (when KiteClass live), schedules, day-to-day operational tools.
- **Source:** Wave 79 outside-in persona audit P3 Manager finding — "Manager persona expects scope separation từ Owner; staff không nên thấy billing của Owner".
- **Rationale:** Org hierarchy industry standard: Manager / Staff không touch billing (Owner's contract); branding decisions (Owner-led marketing); user management (HR-ish, Owner-only). Confining STAFF reduces blast radius nếu staff credential leak + matches mental model của Owner persona ("staff làm việc, tôi quản lý").
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-14).
- **Compliance check:** **Considered** — STAFF có thể access student data (PII per PDPL Art 2.3). Cần tenant data access audit log (per `pre-launch-auth-hardening-checklist.md` §2.7). Wave 79 Bucket C target.
- **Review cadence:** Quarterly. **Next review:** 2026-08-14. Event triggers: Manager role unlock (Wave 80+) sẽ thay đổi STAFF scope (Manager = STAFF + read-only billing?).
- **Code reference:** Wave 79 Bucket B — FE `RoleGuard` + nav menu conditional + BE `@PreAuthorize` per controller.

## BR-ROLE-004 — Invite-staff flow Owner-only

- **Value:** Endpoint `POST /api/v1/staff-invitations` (Wave 79 Bucket B target) require role OWNER. STAFF cannot invite peers. Invitation email contains setup link với short-lived token (TTL 7 ngày).
- **Source:** BR-ROLE-003 (STAFF scope restriction); industry standard (Slack/Notion: only Workspace Owner invites).
- **Rationale:** Tenant user management = trust + commit decision (new staff = compute + license usage). Restricting to Owner maintains accountability. Invitation flow async (email link) avoids password reset complexity for newly-invited users; 7-day TTL strikes balance security (short enough to invalidate stale invites) vs UX (long enough for staff to check email).
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-14).
- **Compliance check:** **Considered** — invite email contains tenant name + invite link với JWT token (no PII của invitee in token claims). PDPL N/A cho invitation flow per se; PII collection sau khi invitee accepts (sets password + name).
- **Review cadence:** Quarterly. **Next review:** 2026-08-14.
- **Code reference:** Wave 79 Bucket B — `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/staff/StaffInvitationController.java` + `StaffInvitationService.java`.

## BR-ROLE-005 — Manager + Teacher + Accountant + Receptionist roles deferred Wave 80+

- **Value:** Phase 1 KHÔNG ship 4 additional roles (Manager / Teacher / Accountant / Receptionist). Documented backlog cho Phase 2+ (Wave 80+). Backend `users.role` enum trong V46 chỉ include `OWNER` + `STAFF` + `PLATFORM_ADMIN` (cross-tenant superuser).
- **Source:** BR-ROLE-001 rationale (Phase 1 MVP trade-off).
- **Rationale:** YAGNI — Phase 1 BETA 5-10 tenant không cần 5-role granularity. Premature expansion tăng surface bug + UI complexity (5 menu variants × N pages). Wait for real persona signal từ P3 medium-center cohort.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-14).
- **Compliance check:** N/A.
- **Review cadence:** Phase 2 trigger evaluation; Quarterly. **Next review:** 2026-08-14.
- **Code reference:** Backlog ROADMAP §Wave 80+. Wave 79 V46 enum reserve future values comment.

## BR-ROLE-006 — Spring Security @PreAuthorize matrix (BE) + FE RoleGuard parity (FE)

- **Value:** Mọi privileged endpoint trong `kitehub-subscription` MUST have explicit `@PreAuthorize` annotation matching scope from BR-ROLE-003. FE `RoleGuard` component wraps protected routes with same role checks. Role names match exact strings (`OWNER`, `STAFF`, `PLATFORM_ADMIN`) — no FE-only aliases (per Wave 71b GAP-518 lesson).
- **Source:** `pre-launch-owasp-rest-hardening-checklist.md` §2.1 (A01 Broken Access Control); `pre-handoff-self-test-completeness.md` §2.4 (admin flow check).
- **Rationale:** Defense in depth — gateway path filter alone insufficient. Per-resource authz annotation prevents accidental endpoint leakage on refactor. Exact-match role string between BE seed + FE guard prevents Wave 71b incident repeat (BE issued `PLATFORM_ADMIN`, FE guard checked `'ADMIN'` → admin user got redirected to `/dashboard`).
- **Reviewer:** @nguyenvankiet (acting Security scout, solo-dev, 2026-05-14).
- **Compliance check:** **Compliant** — OWASP A01.
- **Review cadence:** Quarterly + per-PR (reviewer checklist per `pre-launch-owasp-rest-hardening-checklist.md` §5.3).
- **Code reference:** Wave 79 Bucket B — every new staff/billing/branding controller; FE `kitehub-frontend/src/components/RoleGuard/RoleGuard.tsx`.

---

## Role matrix

| Resource / Action | OWNER | STAFF | PLATFORM_ADMIN |
|-------------------|:-----:|:-----:|:--------------:|
| View tenant dashboard | ✅ | ✅ | ✅ (cross-tenant) |
| View billing / subscription | ✅ | ❌ | ✅ |
| View branding / AI Branding | ✅ | ❌ | ✅ |
| Manage staff (invite/revoke) | ✅ | ❌ | ✅ |
| View students / classes / schedules | ✅ | ✅ | ✅ |
| View core operational data | ✅ | ✅ | ✅ |
| Domain / DNS settings | ✅ | ❌ | ✅ |
| Approve/reject beta requests | ❌ | ❌ | ✅ |
| Suspend tenant instances | ❌ | ❌ | ✅ |
| Disable 2FA cho self | ✅ | ✅ | ❌ (BR-AUTH-2FA-005) |

---

## Config

| Key | Default | Purpose | Wired |
|-----|---------|---------|:-----:|
| `kitehub.rbac.alias-cutoff-date` | `2026-06-14` | Backward-compat alias PLATFORM_ADMIN/ADMIN/OWNER hết hạn | 🆕 Wave 79 Bucket B target |
| `kitehub.rbac.staff-invitation-ttl-days` | `7` | TTL cho staff invitation token | 🆕 Wave 79 Bucket B target |
| `kitehub.rbac.staff-max-per-tenant` | `50` | Soft cap số STAFF / tenant (Phase 1; Phase 2 reconsider) | 🆕 Wave 79 Bucket B target |

Config keys nằm `application.yml` của `kitehub-subscription`.
