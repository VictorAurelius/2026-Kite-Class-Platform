# GAP-1119: KiteClass role-based login routing + per-role dashboard shell + RBAC management

**Status:** 🟡 PARTIAL (85% — item (d) RBAC assign-UI shipped FE; G2 browser-walk pending; student-shell gated KC-9)
**Priority:** 🟠 P1
**Domain:** Mixed (FE kiteclass-frontend + BE kiteclass-core + cross-product auth)
**Found:** 2026-06-10 (user-flagged khi scope FE LMS wave — outside-in audit GAP-1113)
**Affects:** Toàn bộ KC user-facing surface (LMS, course/class, grade, attendance, billing học phí, parent/student portal) — đều cắm lên tầng role-shell này

## Problem

FE LMS wave (GAP-1113) và mọi surface KC khác giả định một **tầng nền role-based shell** chưa tồn tại đầy đủ. State hiện tại:

- **BE — RBAC + auth-by-role đã thiết kế tách KH/KC:**
  - `kiteclass-core/module/role` có `Role`, `Permission`, `RoleService` + role-hierarchy (BR-ROLE Level 1-10, role bundle permissions, custom role per-tenant, seeded `RoleSeederService`) → **dynamic-capable**.
  - Auth split (per `tenant-auth/rules.md` BR-AUTH-002 + BR-AUTH-HDR-002): **OWNER/STAFF login KiteHub** (`kitehub-subscription` `/api/v1/auth/**`, FE `:3001`, không nằm trong `auth_credentials` KC); **TEACHER/PARENT/STUDENT login KiteClass** (tenant-auth Option B, `/api/v1/tenant-auth/login`, FE `:3000`, `entity_type CHECK ∈ {PARENT,TEACHER,STUDENT}` V89:22).
  - STAFF invite = KH-side (`kitehub-subscription.staff.StaffInvitationController`); TEACHER provision = KC-side (admin set-password `AuthCredentialProvisioningService.setPassword`).

- **FE — role-shell YẾU/thiếu:**
  - Có route group `(dashboard)`/`(teacher)`/`(public)`/`(auth)` nhưng **không có** login→role-based-redirect, **không có** role-guard component (grep `RoleGuard`/`useRole`/`hasRole` → 0 hit), **không có** cross-product handoff KH `:3001` → KC `:3000` cho owner/staff. → mọi role login gần như thấy cùng 1 shell; route không bị chặn theo role (rủi ro: bất kỳ user login nào với tới route bất kỳ).

## Quyết định đã chốt (2026-06-10)

1. **RBAC depth = fixed-curated cho beta** — ship 5 role template seeded (OWNER/STAFF/TEACHER/PARENT/STUDENT); owner CHỈ gán user→role; KHÔNG dựng UI owner-sửa-permission-per-role; BE giữ dynamic-capable; defer permission-edit UI Phase 3.
2. **Owner/Staff auth = cross-product SSO KH→KC** — giữ split thiết kế: OWNER/STAFF login KH `:3001`; token KH-minted handoff sang KC `:3000` qua shared gateway cho school-mgmt. TEACHER/PARENT/STUDENT login thẳng KC.
3. **Route quản-quyền (assign user→role) nằm ở KC owner-shell** — role-hierarchy là KC domain (per-tenant school roles); KHÔNG ở KH (KH = subscription/platform).
4. **Invite split giữ nguyên + document rõ:** STAFF mời qua KH (staff-invitation); TEACHER provision qua KC (owner/admin tạo teacher + set-password). Reconcile = doc hoá split, không gộp.

## Proposed Fix (Phase 2a foundation — TRƯỚC/CÙNG FE LMS surfaces)

- **(a) KC login → role-redirect + role-guard:** post-login đọc role từ JWT claim → redirect role-home + guard mọi route group theo role (teacher/owner/staff/parent shells buildable ngay; **student-shell chặn bởi KC-9 student-auth pending**).
- **(b) Per-role nav + dashboard home** theo bảng dưới.
- **(c) Cross-product SSO handoff** owner/staff KH `:3001` → KC `:3000` (share token qua gateway / SSO flow).
- **(d) KC owner-shell RBAC management UI** — gán user→role (5 template), không edit-permission UI (per quyết định 1).
- **(e) Document invite split** STAFF(KH)/TEACHER(KC).

### Per-role dashboard (grounded 5 role + KC domain)
| Role | Login → thấy |
|---|---|
| OWNER | toàn quyền: mọi course/class + gán GV + students + billing học phí + payroll + branding + settings + analytics + role-assign |
| STAFF | subset owner theo permission bundle (enrollment + attendance + invoice + staff) |
| TEACHER | my courses/classes + LMS authoring + attendance + grade entry + completion roster |
| STUDENT | chỉ học tập: my classes + lesson player + assignments + grades + progress + attendance + payments (own) — **chờ KC-9** |
| PARENT | child read-only: progress/grades/attendance/fees + notify (Zalo) |

## Acceptance Criteria

- [ ] KC login mint token → redirect đúng role-home; role-guard chặn route ngoài quyền (teacher không vào owner route, v.v.)
- [ ] 4 role-shell (owner/staff/teacher/parent) có nav + dashboard home riêng; student-shell scaffold + gated KC-9
- [ ] Cross-product SSO: owner/staff login KH `:3001` → vào được KC `:3000` school-mgmt không re-login
- [ ] KC owner-shell có màn assign user→role (5 template seeded); không expose permission-edit UI
- [ ] Doc hoá invite split STAFF(KH)/TEACHER(KC) trong business doc + architecture
- [ ] LMS surfaces (GAP-1113) cắm đúng shell: authoring→teacher, player→student

## Related

- Discovered in: session 2026-06-10 (FE LMS wave scope discussion)
- GAP-1113 (FE LMS headless — surfaces cắm lên shell này)
- KC-9 student-auth pending (chặn student-shell Increment B) — memory `project_parent_student_portal_phase2_gated`
- Design: `documents/01-business/kiteclass/role-hierarchy/` + `tenant-auth/` + `staff-invitation/` + ADR-003-role-hierarchy + rule `kitehub-kiteclass-boundary.md` §2
- Outside-in audit: `2026-06-10-pre-wave-lms-fe-outside-in.md` (persona lens flagged owner/parent/admin scope)

## G1 runtime walk (2026-06-14) — gateway BE-contract: ✅ PASS (status giữ PARTIAL)

Per `documents/04-quality/audits/rst-html/2026-06-14-g1-runtime-walk-rbac-lms.md`. RoleGuard BE authz verify: cross-role 403 đủ 4 chiều (TEACHER/STUDENT/PARENT→`/roles/templates`=403; TEACHER→`/enrollments/me`=403); role-home BE-area reachable đúng role (OWNER→roles 200, STUDENT→/me 200, TEACHER→lms 200). Bucket D assign/revoke: `GET/POST /roles/assignments` 200/201, revoke 204, invalid-role 400, TEACHER-assign 403 (sau fix GAP-1298 LazyInit 500). **Còn lại G2★ human:** browser-walk role-home redirect (`roles.ts ROLE_HOME`) + `/admin/roles` assign UI trên FE `:3000` (FE image stale, cần rebuild). KHÔNG flip DONE.
