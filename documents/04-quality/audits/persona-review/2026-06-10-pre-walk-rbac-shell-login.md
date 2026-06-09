---
audience: dev
created: 2026-06-10
type: pre-walk-persona-simulation
wave: rbac-shell-1
mandate: pre-walk-persona-simulation-mandate.md §3
flow: KC login → role-redirect → per-role shell
---

# Pre-Walk Persona Simulation — Wave RBAC-Shell 1 (KC login → role-redirect → shell)

> **Mục đích (per `pre-walk-persona-simulation-mandate.md`):** mô phỏng luồng login từng role TRƯỚC khi walk thật, surface ≥5 failure mode/role kèm điểm phát sinh + triệu chứng + cách check sớm. Feed cho Bucket A (role-guard foundation) + Bucket C (cross-product SSO).

## 0. State-check tóm tắt (đã verify code @ origin/main 2f1af1aa)

| Layer | Sự thật |
|---|---|
| **KC token** | `AuthTokenService:70` → `.claim("role", credential.getEntityType())` + `tenantId` + `referenceId`. Login KC-native chỉ cho **PARENT/TEACHER/STUDENT** (`AuthController` Wave auth-1 Option B; `entity_type CHECK ∈ {PARENT,TEACHER,STUDENT}`). |
| **OWNER/STAFF** | KHÔNG có trong KC `auth_credentials` — login KiteHub `:3001` (`kitehub-subscription /api/v1/auth/**`); role tới KC qua **gateway header** `X-User-Roles` ("OWNER" / "OWNER,ADMIN") — `GatewayHeaderAuthenticationFilter`. |
| **FE middleware** | `kiteclass-frontend/src/middleware.ts` **chỉ** host→tenant resolver (GAP-811). **0** role-redirect, **0** role-guard (grep `RoleGuard\|useRole\|hasRole` → 0 hit logic guard). |
| **FE route groups** | `(dashboard)` [branding/billing/students/settings/classes/reports/admin/attendance/courses/teachers/overview/**parent**/**student**], `(teacher)`, `(auth)`, `(public)`. Các group **không** bị gate theo role. |
| **Role-name literal** | BE KC entity_type: `TEACHER`/`PARENT`/`STUDENT` (`AuthCredentialProvisioningService:37-39`). Gateway role: `OWNER`/`ADMIN`. FlywayEndpointAuthFilter `REQUIRED_ROLE="ADMIN"`. → **FE guard literal phải khớp đúng 5 giá trị + nguồn kép (entity_type vs gateway header)**. |

**Hệ quả nền:** bất kỳ user login (bất kỳ role) hiện gần như thấy cùng 1 shell + reach route bất kỳ → **IDOR-by-navigation** (security, không chỉ UX).

---

## 1. OWNER — login KH `:3001` → SSO sang KC `:3000` school-mgmt

| # | (a) Where | (b) Symptom | (c) Pre-walk check |
|---|---|---|---|
| O1 | Cross-product handoff KH→KC (Bucket C, chưa tồn tại) | Owner login KH thành công nhưng sang KC `:3000` bị bật về `/login` (KC không có session) → re-login vô vọng (KC không có credential OWNER) | `curl` KC route với token KH-minted → verify gateway chấp nhận `role=OWNER` header; grep có handoff endpoint/redirect KH→KC chưa (hiện chưa) |
| O2 | KC token `role` claim (`AuthTokenService:70`) | Nếu owner thử login thẳng KC → 401 (không có entity_type OWNER trong KC) → tưởng "sai mật khẩu" | Verify `entity_type CHECK` reject OWNER; UI phải chỉ owner sang KH login, không hiện form KC |
| O3 | FE role-redirect (Bucket A, chưa có) | Sau handoff, owner đáp `(dashboard)` generic thay vì owner-home với full nav (course/GV/students/billing/payroll/branding/settings/analytics/role-assign) | grep login success handler có switch theo role chưa; check `(dashboard)/overview` có phân biệt owner vs staff |
| O4 | RoleGuard (Bucket A, chưa có) | Owner-home render nhưng route `(dashboard)/admin` hoặc platform-route lọt nếu guard chỉ check "đã login" | Liệt kê route trong `(dashboard)` cần OWNER-only; verify chưa có guard nào chặn |
| O5 | Gateway `X-User-Roles` "OWNER,ADMIN" parse | Nếu FE chỉ đọc role đầu tiên / không split comma → owner có ADMIN bị mất quyền admin-route | `GatewayHeaderAuthenticationFilter:100` comment "OWNER,ADMIN" → verify FE guard split multi-role, không so sánh string nguyên |
| O6 | SSO token TTL / refresh | Token KH-minted hết hạn khi đang ở KC → KC không refresh được (refresh thuộc KH) → mất việc giữa chừng | Check refresh flow cross-product; TTL KH token vs phiên làm việc KC |

## 2. STAFF — login KH `:3001` → SSO KC (subset owner permission)

| # | (a) Where | (b) Symptom | (c) Pre-walk check |
|---|---|---|---|
| S1 | SSO handoff (Bucket C) | Giống O1 — staff kẹt `/login` KC | như O1 |
| S2 | Permission-bundle enforce (route + API) | Staff thấy/đụng được route ngoài bundle (vd payroll/branding/settings) vì FE chỉ check role=STAFF chứ không check permission con | Liệt kê route STAFF được phép (enrollment/attendance/invoice/staff) vs cấm; verify guard hiện chưa phân biệt |
| S3 | Role literal STAFF vs gateway header | Gateway header có "STAFF" không? (chỉ thấy "OWNER"/"ADMIN" trong filter comment) → STAFF có thể chưa được mint role → guard fail-open/closed sai | grep BE mint role STAFF ở KH subscription; reconcile literal với FE |
| S4 | RoleGuard owner-vs-staff phân tách | Staff reach owner-only route (role-assign / analytics) nếu guard gộp owner+staff chung "school-admin" | check thiết kế guard có tách OWNER ≠ STAFF |
| S5 | Stale session sau đổi permission | Owner gỡ quyền staff nhưng token cũ còn quyền tới khi hết hạn | check token chứa permission snapshot hay query live; pre-walk: đổi permission → thử route ngay |

## 3. TEACHER — login KC `:3000` native (rủi ro cao nhất cho Bucket A)

| # | (a) Where | (b) Symptom | (c) Pre-walk check |
|---|---|---|---|
| T1 | FE login success → redirect (Bucket A) | Teacher login đáp `(dashboard)` owner-shell thay vì teacher-home (my courses/classes + LMS authoring + attendance + grade) | grep login handler; verify chưa switch role → teacher thấy nav owner |
| T2 | RoleGuard (chưa có) — **IDOR-by-nav** | Teacher gõ URL `/billing` `/settings` `/students` `/admin` → **vào được** (route không gate role) → xem dữ liệu ngoài quyền | `curl`/browser teacher token tới owner route → kỳ vọng 403/redirect, hiện **lọt** |
| T3 | Role literal `TEACHER` (entity_type) vs FE guard | FE guard so sánh "Teacher"/"teacher"/"ROLE_TEACHER" thay vì "TEACHER" → guard fail-open hoặc khóa nhầm | grep FE literal vs BE `ROLE_TEACHER="TEACHER"` (`AuthCredentialProvisioningService:38`) |
| T4 | `tenantId` claim → cross-tenant | Teacher tenant A đọc data tenant B nếu FE/route không bám `tenantId` claim (chỉ dựa header bị spoof) | verify gateway strip client `X-Tenant-Id`, BE bám claim; pre-walk: teacher A gọi resource tenant B → 403 |
| T5 | `(teacher)` group vs `(dashboard)` group trùng | 2 nơi cùng render teacher surface (`(teacher)/teacher`, `(teacher)/attendance` vs `(dashboard)/attendance`) → guard chỉ phủ 1 group, group kia lọt | liệt kê route teacher ở cả 2 group; guard phải phủ cả hai |
| T6 | Token thiếu/expired → blank vs redirect | Token hết hạn giữa phiên → teacher thấy blank shell thay vì bật `/login` | check 401 handler FE; pre-walk: xóa token → expect redirect login |

## 4. PARENT — login KC `:3000` native (read-only child)

| # | (a) Where | (b) Symptom | (c) Pre-walk check |
|---|---|---|---|
| P1 | redirect → `parent-shell.tsx` vs `(dashboard)/parent` | Parent đáp `(dashboard)` chung thay vì parent-home read-only (progress/grades/attendance/fees/notify) | grep redirect; verify `components/parent/parent-shell.tsx` được mount đúng role |
| P2 | RoleGuard read-only enforce | Parent reach route mutation (enrollment/grade-entry/settings) → thấy nút sửa (không chỉ ẩn — route phải chặn) | parent token → owner/teacher route → expect 403; verify guard read-only |
| P3 | Child-scope (hasAccessToChild) | Parent A xem được child của parent B nếu authz chỉ check role=PARENT không check quan hệ phụ huynh-học sinh (liên hệ GAP-798 user_id UUID bridge) | pre-walk: parent A gọi `/children/{B-child-id}` → expect 403; check `hasAccessToChild` |
| P4 | Role literal `PARENT` parity | FE guard "PARENT" vs BE `ROLE_PARENT="PARENT"` (`:37`) — verify khớp | grep |
| P5 | Cross-tenant `tenantId` | Parent A (tenant A) reach data tenant B | như T4 |

## 5. STUDENT — gated KC-9 (chỉ scaffold)

> Student-auth **chưa ship** (KC-9 pending, memory `project_parent_student_portal_phase2_gated`). Bucket B chỉ scaffold student-shell, KHÔNG functional. Failure mode chính khi unblock: (a) login student entity_type `STUDENT` redirect đúng student-home (my classes/lesson player/assignments/grades/progress/payments-own); (b) guard chặn student khỏi owner/teacher/parent route; (c) student chỉ thấy data của chính mình (own enrollment/grade). Walk thật defer tới KC-9.

---

## 6. Top-5 risk toàn cục (ưu tiên xử lý)

1. **[P0 security] IDOR-by-navigation** (T2/P2/S2/O4) — route group KHÔNG gate role → bất kỳ user login reach route bất kỳ. **Bucket A RoleGuard = security fix bắt buộc trước mọi shell.**
2. **[P0 HIGH] Cross-product SSO KH→KC chưa tồn tại** (O1/S1) — owner/staff không vào được KC `:3000`. Bucket C risk-isolated; nếu > 1 bucket → tách wave + tạm KC-native fallback.
3. **[P1] Role-name parity nguồn kép** (T3/S3/O5/P4) — entity_type (`TEACHER/PARENT/STUDENT`) **vs** gateway header (`OWNER/ADMIN`, multi-value "OWNER,ADMIN"). FE guard phải khớp đúng 5 literal + xử lý comma-split, không so string nguyên.
4. **[P1] Cross-tenant leak qua `tenantId` claim** (T4/P5) — guard/route phải bám `tenantId` claim (gateway strip client header), không tin `X-Tenant-Id` client.
5. **[P1] Double route-group coverage** (T5) — teacher surface ở cả `(teacher)` lẫn `(dashboard)`; guard phải phủ MỌI group, không sót.

## 7. Khuyến nghị cho Bucket A + Bucket C

**Bucket A (role-guard foundation):**
- RoleGuard đọc role từ **2 nguồn hợp nhất**: KC token `role` claim (entity_type) + gateway `X-User-Roles` (owner/staff). Chuẩn hoá về 1 enum 5 giá trị `OWNER|STAFF|TEACHER|PARENT|STUDENT` + helper `hasRole`/`hasAnyRole` xử lý multi-value.
- Map route-group → allowed-roles (vd `(dashboard)/billing` → OWNER|STAFF-with-invoice; `(teacher)/*` → TEACHER; `(dashboard)/parent` → PARENT). Guard chặn ở **cả** middleware (edge) lẫn layout (defense-in-depth), KHÔNG chỉ ẩn nav.
- Login success handler: `switch(role)` → redirect role-home. Reconcile literal BE↔FE **trước** (grep `ROLE_TEACHER/PARENT/STUDENT` + gateway "OWNER/ADMIN").
- Test (TDD): mỗi role login → redirect đúng home; mỗi role gõ route ngoài quyền → 403/redirect (đặc biệt T2 teacher→/billing).

**Bucket C (SSO KH→KC):**
- Thiết kế token handoff TRƯỚC impl (gateway shared JWT secret OR redirect-with-token). Phòng O1/O6 (token TTL/refresh cross-product). Nếu phức tạp > 1 bucket → owner/staff dùng KC-native fallback tạm cho beta (nếu khả thi), tách SSO sang wave riêng.

---

## 8. Cross-link
- Plan: `documents/03-planning/waves/wave-rbac-shell-1.md` (Bucket A/C, §1 Risk, §4 State-Check)
- Mandate: `pre-walk-persona-simulation-mandate.md` §3
- Sister checklist: `pre-handoff-self-test-completeness.md` §2.1 (auth-gated) + §2.4 (admin/role flow)
- Related gaps: GAP-798 (user_id UUID bridge authz — P3 child-scope), GAP-886 (RBAC user_id BIGINT vs UUID drift), GAP-938 (admin auth pattern doc-vs-code)
