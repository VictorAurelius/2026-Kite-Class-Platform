---
title: Invite Flow Redesign — Discussion (multi-role + bulk) — USER DECISION REQUIRED
status: approved
created: 2026-06-09
updated: 2026-06-14
tag_primary: rbac-shell
tags_secondary: [invite, kitehub, kiteclass, beta-prep]
gaps: [GAP-1119]
audience: dev
---

# Thiết kế lại luồng Invite — Thảo luận (đa-role + bulk)

> **⚠️ ĐÂY LÀ TÀI LIỆU THẢO LUẬN — KHÔNG TỰ CHỐT.** Trình bày trạng thái hiện tại + vấn đề + 3 OPTIONS với trade-off + RECOMMENDATION. **User quyết định scope.** Liên kết: `wave-rbac-shell-1.md` Bucket E (doc invite split).

## TL;DR cho người bận
- **Luồng invite hiện tại CHỈ mời "staff" + CHỈ mời từng người một.** 2 vấn đề user nêu là đúng.
- **Mời teacher:** hiện teacher được tạo qua KC admin-set-password (thủ công, owner phải tự đặt mật khẩu cho GV), KHÔNG có email-invite self-serve. Vì invite KH = 2-role MVP (OWNER+STAFF) by design (BR-ROLE-001/005).
- **Bulk:** đã có pattern bulk-import sẵn cho **student** (`student/bulkimport/` + `XlsxParser`, preview/commit/template/error-download) — tái dùng pattern này cho bulk-invite.
- **Đề xuất (RECOMMEND):** **Option 1 — giữ split KH/KC + thêm KC-native teacher/manager email-invite + bulk-invite tái dùng student-bulkimport pattern.** Owner KHÔNG cảm nhận split nhờ cross-product SSO (RBAC-Shell Bucket C) surface cả 2 entry-point trong KC owner-shell. Phase 2 cân nhắc hợp nhất.

---

## 1. Trạng thái hiện tại (đã verify code — design-first investigation)

### 1.1 Luồng invite production = KH-side, 2-role MVP, single-only
- **Code:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/staff/` — `StaffInvitationController` + `StaffInvitationServiceImpl` + entity + token service + audit.
- **DTO `CreateStaffInvitationRequest`:** CHỈ `email` + `fullName`. **KHÔNG có field `role`.** → mọi người được mời đều thành generic STAFF.
- **FE form** `(customer)/admin/staff/invite/page.tsx` comment xác nhận: *"Role fixed to STAFF. Phase 1 BETA = 2-role MVP (OWNER + STAFF). TEACHER/MANAGER roles are Phase 2+ scope — GAP-784 confirmed BE does NOT accept a role param, so we do NOT send one (avoids a misleading role picker)."* + *"1 email per tenant at a time"*.
- **Endpoints hiện có:** `POST /api/v1/staff-invitations` (create single) · `GET` (list) · `POST /{id}/resend` · `DELETE /{id}` (revoke) · `GET /by-token/{token}` · `POST /{token}/accept`.
- **Business doc thật:** `documents/01-business/roles/` (BR-ROLE-001 Phase 1 MVP 2-role OWNER+STAFF; BR-ROLE-005 Manager/Teacher/Accountant/Receptionist deferred Wave 80+; BR-ROLE-004 invite-staff Owner-only).
- **GAP-784 (DONE):** "FE InviteStaffPage role affordance vs BE 2-role MVP drift" — đã resolve bằng cách làm FE read-only STAFF (KHÔNG thêm role picker BE không honor). → multi-role invite là Phase 2+ scope **có chủ đích**, không phải bug bỏ sót.

### 1.2 Teacher hiện được provision thế nào (KHÔNG qua invite)
- **KC-side admin-provision:** `kiteclass-core/module/auth/service/AuthCredentialProvisioningService.setPassword` + `module/teacher/`. Owner/admin tạo teacher entity rồi **set-password thủ công** → KHÔNG có email-invite link cho teacher tự onboard.
- Hệ quả: owner mời 1 GV mới phải tự đặt mật khẩu + báo GV thủ công (qua Zalo/điện thoại) — UX kém so với staff (staff được email link tự set password).

### 1.3 Doc drift cần lưu ý (DISCOVERY — chưa file gap, xem §6)
- `documents/01-business/kiteclass/staff-invitation/{rules,use-cases,api-contract}.md` (v1.0, Wave meta-6 Bucket A, 2026-05-28) mô tả **KC-side** staff-invitation với roles STAFF/TEACHER/MANAGER + migration `V71__create_staff_invitations.sql` + source `kiteclass-core/.../module/staff/`.
- **NHƯNG `kiteclass-core/.../module/staff/` KHÔNG tồn tại trên main** — đây là thiết kế **planned/aspirational chưa ship**. Implementation thật = KH-side (`documents/01-business/roles/`).
- → Doc drift: business doc mô tả KC staff-invitation 3-role chưa code. Cần reconcile (xem §6).

### 1.4 Pattern bulk-import có sẵn (tái dùng cho bulk-invite)
- **Student bulk-import (KC):** `kiteclass-core/module/student/bulkimport/` — `BulkImportController` endpoints: `POST /preview` (multipart XLSX → BulkImportResult validation) · `POST /commit` · `GET /template` (download mẫu) · `POST /download-errors` · `XlsxParser`. Pattern preview→fix→commit + error-download = chuẩn cho bulk.
- **Enrollment bulk-import (KC):** `module/enrollment/bulkimport/` — pattern tương tự.
- **Parent invitation (KC):** `module/parent/ParentInvitationServiceImpl` — single invite (no bulk, no role param).

---

## 2. Vấn đề (user nêu — confirmed)
1. **Chưa mời được teacher** — invite KH chỉ STAFF (no role field); teacher = admin-set-password thủ công, không email self-serve.
2. **Chỉ invite-from-one-at-a-time** — `CreateStaffInvitationRequest` 1 email/request; không bulk multi-email/CSV.

Phụ (surface khi điều tra):
3. **Doc drift** KC staff-invitation 3-role (planned chưa code) vs KH 2-role (shipped) — §1.3.
4. **Teacher onboarding UX kém** — owner phải set-password thủ công + báo GV ngoài hệ thống.

---

## 3. Câu hỏi thiết kế cần quyết

### Q1 — Role nào nên invite-able?
| Role | Invite-able? | Provisioning khác biệt |
|---|---|---|
| OWNER | ❌ KHÔNG (single-owner-per-tenant invariant; owner tạo lúc tenant provision) | n/a |
| STAFF | ✅ (đang có, KH-side) | KH user + auth KH `/api/v1/auth/**` |
| TEACHER | ✅ (NÊN thêm — vấn đề #1) | KC `auth_credentials` (entity_type TEACHER), tenant-auth Option B |
| MANAGER | ⚠️ (defer? — BR-ROLE-005 Phase 2; cohort P3 medium-center mới cần) | KH OR KC tùy quyết Q-boundary |
| PARENT | ✅ (đã có pattern riêng `ParentInvitationServiceImpl`, có student linkage) | KC, không trong scope invite-này |
| STUDENT | ⚠️ (gated KC-9 student-auth; bulk-enroll đã có) | KC, qua enrollment không qua invite |

**Khác biệt provisioning then-chốt:** STAFF/OWNER auth ở **KH**; TEACHER/PARENT/STUDENT auth ở **KC** (per `kitehub-kiteclass-boundary.md` §2 + tenant-auth split). → mọi option phải xử lý ranh giới này.

### Q2 — Bulk như nào?
- **Multi-email textarea** (paste 5-50 email, mỗi dòng 1 email + tên) — nhẹ, đủ cho đầu kỳ nhỏ.
- **CSV/XLSX upload** (tái dùng `BulkImportController` preview/commit/template/error-download pattern) — robust, đủ cho 20-100 GV/staff đầu kỳ, validate trước commit.
- Recommend: **cả hai** — textarea cho quick (≤10), file-upload cho large (>10). Single-invite giữ làm quick-path 1 người.

### Q3 — Single vs batch UX
- Single-invite (quick-path) GIỮ nguyên — mời 1 người giữa kỳ.
- Bulk-invite = surface riêng (tab/nút "Mời hàng loạt") → preview validation (email trùng / sai format / vượt quota `staff-max-per-tenant`) → commit → kết quả per-row (thành công / lỗi + lý do).

---

## 4. Outside-in persona (per `outside-in-coverage-trigger.md` — invite = user-facing flow)
| Persona / job | Nhu cầu | Implication |
|---|---|---|
| **Owner mời 1 GV giữa kỳ** | Nhanh, GV tự set password qua email (không phải gọi điện đọc mật khẩu) | TEACHER email-invite self-serve (vấn đề #1) + single quick-path |
| **Owner setup đầu kỳ — 20 staff + 15 GV** | Import 1 lần từ sheet HR sẵn có, thấy lỗi trước khi gửi, không gửi 35 email tay | Bulk CSV/XLSX + preview-validate + per-row result (vấn đề #2) |
| **Owner ở trung tâm nhỏ (P2 solo)** | Chỉ vài người, không cần phân biệt phức tạp role | STAFF 2-role MVP đủ — đừng over-engineer role matrix |
| **Trung tâm vừa (P3, Phase 2)** | Cần MANAGER role + phân quyền chi tiết | MANAGER defer Phase 2 hợp lý (BR-ROLE-005) |

Benchmark (Slack/Notion/Linear invite): tất cả đều có (a) single quick-invite + (b) bulk (multi-email/CSV) + (c) role picker tại invite-time. → role-at-invite + bulk là industry-standard; project đang thiếu cả hai cho non-STAFF.

---

## 5. 3 OPTIONS (trade-off) — USER CHỌN

### Option 1 — Giữ split KH/KC + thêm KC-native teacher invite + bulk (⭐ RECOMMEND)
- STAFF invite GIỮ KH-side (như hiện tại).
- Thêm **KC-native TEACHER (+ MANAGER nếu cần) email-invite** — build phần KC staff-invitation đã doc (§1.3) nhưng scope TEACHER+MANAGER (STAFF vẫn KH). Mirror staff-invite pattern: token + email + accept + set-password → tạo KC `auth_credentials` (entity_type TEACHER).
- Thêm **bulk-invite** (multi-email + CSV/XLSX) tái dùng `BulkImportController` pattern, áp cho cả KH-staff-invite và KC-teacher-invite.
- **Cross-product SSO (RBAC-Shell Bucket C)** surface cả 2 entry-point ("Mời nhân viên" + "Mời giáo viên") trong **KC owner-shell** → owner KHÔNG cảm nhận split (1 nơi mời tất cả).

| ✅ Pro | ❌ Con |
|---|---|
| Tôn trọng KH/KC boundary đã chốt (GAP-1119 + `kitehub-kiteclass-boundary.md`) | 2 invite backend (KH staff + KC teacher) — duplicate token/email/accept logic |
| TEACHER onboarding email self-serve (fix vấn đề #1 + #4) | Cần build KC staff-invitation (planned doc → real code) |
| Bulk tái dùng pattern có sẵn (low risk) | Owner-shell phải merge 2 invite UI (nhưng SSO làm trong suốt) |
| Boundary risk thấp nhất | |

### Option 2 — Unified KC invite hub (role param, hợp nhất về KC)
- 1 luồng invite KC-side role ∈ {STAFF, TEACHER, MANAGER}, route provisioning per role.
- **Vấn đề:** STAFF auth hiện ở KH (`auth_credentials` KC có CHECK chỉ {PARENT,TEACHER,STUDENT}). Để STAFF invite ở KC phải hoặc (a) re-home STAFF auth sang KC, hoặc (b) KC gọi internal KH provision STAFF.

| ✅ Pro | ❌ Con |
|---|---|
| 1 invite UX duy nhất "mời người + chọn role" (đẹp nhất cho owner) | Vi phạm KH/KC auth boundary cho STAFF — scope lớn |
| 1 invite codebase (không duplicate) | Cần re-home STAFF auth OR cross-product write KC→KH |
| Khớp benchmark (role-at-invite) | Risk cao, đụng auth-split vừa ổn định (PR #2186) |

### Option 3 — KH invite + role param + cross-product provisioning
- Extend `/api/v1/staff-invitations` thêm `role`; STAFF→KH user, TEACHER→KC provision qua internal call.

| ✅ Pro | ❌ Con |
|---|---|
| 1 invite UI ở KH (nơi owner login gốc) | KH viết KC domain (boundary violation ngược) — couples 2 product |
| Reuse KH invite infra | Internal KH→KC provisioning call = thêm coupling + fail mode |
| | Trái GAP-1119 decision "route quản-quyền ở KC" |

---

## 6. RECOMMENDATION (chờ user duyệt)

**Chọn Option 1** cho Phase 1 BETA, lý do:
1. **Tôn trọng boundary đã chốt** — GAP-1119 quyết "invite split STAFF(KH)/TEACHER(KC) giữ nguyên + document"; `kitehub-kiteclass-boundary.md` §2 mandate KH=lifecycle, KC=nghiệp-vụ. Teacher là KC-domain → teacher-invite thuộc KC tự nhiên.
2. **Planned design đã có** — `documents/01-business/kiteclass/staff-invitation/` đã doc KC 3-role invite (chỉ chưa code) → build TEACHER+MANAGER phần của nó.
3. **Boundary risk thấp nhất** — không đụng auth-split STAFF vừa ổn định.
4. **Bulk = pattern reuse** — `BulkImportController` (student) đã proven → áp cho invite ít risk.
5. **Owner UX vẫn liền mạch** — cross-product SSO (RBAC-Shell Bucket C) cho phép KC owner-shell surface cả "Mời nhân viên"(KH) + "Mời giáo viên"(KC) → owner thấy 1 trang, không cảm nhận split.

**Scope đề xuất nếu Option 1 (để user duyệt):**
- **Phase 1 (wave riêng, sau RBAC-Shell):**
  - KC TEACHER email-invite (token + email + accept + set-password → KC auth_credentials).
  - Bulk-invite (textarea + CSV/XLSX preview/commit) cho cả KH-staff + KC-teacher, reuse `BulkImportController` pattern.
  - Owner-shell surface 2 entry-point (post-SSO).
  - GAP-597 (resend/revoke/reminder) gộp vào nếu cùng wave.
- **Phase 2 (defer):** MANAGER role invite (BR-ROLE-005, cohort P3) + cân nhắc hợp nhất Option 2 nếu owner UX feedback đòi 1-hub.

**Câu hỏi cuối cho user quyết:**
- Q-A: MANAGER có invite-able Phase 1 không, hay defer Phase 2 (theo BR-ROLE-005)?
- Q-B: Bulk dùng textarea, CSV/XLSX, hay cả hai?
- Q-C: Chấp nhận Option 1 (split + KC teacher-invite) hay muốn Option 2 (hợp nhất KC, scope lớn)?
- Q-D: Doc drift §1.3 — reconcile bằng cách nào: (a) sửa KC staff-invitation doc xuống mô tả planned-Phase-1 scope, hay (b) build KC staff-invitation cho khớp doc (Option 1 làm phần này)?

## 7. Discovery cần file (per `discovery-to-gap-inline-filing.md`)
> Tôi (agent draft) KHÔNG tự file gap để TRÁNH gap-ID collision với phiên song song (handoff cảnh báo GAP-1111/1112 đã collide). Surface để coordinator/user file với ID reserved (`scripts/reserve-gap-block.sh`):
- **Discovery 1 — Doc drift KC staff-invitation:** `documents/01-business/kiteclass/staff-invitation/` mô tả KC 3-role invite + V71 migration KHÔNG tồn tại trên main (planned/aspirational). Cần reconcile doc↔code. (Domain: Docs/Backend, P2.)
- **Discovery 2 — TEACHER onboarding UX gap:** teacher provision = admin-set-password thủ công, không email self-serve (vấn đề #1 + #4). Là feature gap cho invite wave. (Domain: Mixed, P1.)
- **Discovery 3 — Bulk-invite missing:** invite single-only; bulk pattern có sẵn (`BulkImportController`) nhưng chưa áp cho invite (vấn đề #2). (Domain: Mixed, P1.)
- (GAP-597 OPEN P2 — invite resend/revoke/reminder — đã filed, gộp khi build invite wave.)

## 8. Log
- **2026-06-09:** Doc tạo từ session điều tra invite flow (design-first: business `roles/` + code `kitehub-subscription/staff/` + KC `bulkimport`/`AuthCredentialProvisioning`). Trình bày 2 vấn đề user nêu + 3 options + RECOMMENDATION Option 1. Pointer từ `wave-rbac-shell-1.md` Bucket E. Chờ user quyết Q-A..Q-D.
