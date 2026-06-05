---
title: Wave auth-1 — KC-native login (Option B) for PARENT/TEACHER/STUDENT
status: draft
created: 2026-06-06
updated: 2026-06-06
waves: [auth-1]
tag_primary: auth
tags_secondary: [login, parent, student, teacher, reference-id, gateway, jwt, kiteclass, phase1-pull-forward]
counter: 1
gaps: [GAP-725, GAP-798b, GAP-269b]
---

# Wave auth-1 — KC-native login (Option B)

**Goal:** kiteclass-core tự cấp `/api/v1/auth/login` cho PARENT/TEACHER/STUDENT (email+password, ký HS512 cùng khóa gateway), JWT mang `role` + `tenantId` + `referenceId` claims → gateway inject `X-User-Reference-Id` → unblock **KC-8 parent portal G3** + **KC-9 student portal** end-to-end qua gateway production-equivalent. Pull-forward từ Phase 2 (user decision 2026-06-06, vẫn ở Phase 1 BETA formally). Defer OTP (Hướng C Zalo/SMS) sang Phase 2 thật.

**Trigger:** KC-8 G3 (2026-06-05) xác nhận parent/student production access bị gate Phase 2 (GAP-725 login + GAP-798b reference_id producer). User chọn pull-forward Option B (KC-native login) thay vì chờ Phase 2 / OTP vendor.

**⚠️ Security-sensitive cross-service per GAP-798b — dedicated clean-context session. Do NOT rush. Mỗi bucket walk-verify trước DONE (no unverifiable security code per `feature-ship-runtime-walk-mandate.md`).**

## 1. Brainstorm — vì sao Option B + đơn giản hóa GAP-798b

**State-check (2026-06-06):**
- KH `PlatformRole` = chỉ OWNER/STAFF (ADMIN alias). Không có PARENT/TEACHER/STUDENT → KH không issue được JWT các role này (lý do GAP-725).
- `AuthService` (subscription) = users table + BCrypt, chỉ tạo OWNER.
- `ParentInvitation.redeem` (core) = tạo `parents` row + link, **KHÔNG tạo credential/login**.
- Student = không có auth concept.
- Gateway `JwtAuthenticationGatewayFilter` validate HS512 + inject X-User-Id/Roles/Email; `TenantResolver` đã có JWT `tenantId` fallback (GAP-711). **KHÔNG inject X-User-Reference-Id** (GAP-798b).
- Consumer-side authz (`@authz.hasAccessToChild` / student `requireStudentId`) đã ship (GAP-798) + verified KC-8 G1.

**Option B đơn giản hóa GAP-798b producer side:** Vì KC tự authenticate + tự mint JWT, KC **biết trực tiếp** `parents.id`/`students.id`/`teachers.id` (chủ credential) → đặt `referenceId` vào claim NGAY. **KHÔNG cần** cross-service population (`users.reference_id` ở subscription) — cái GAP-798b coi là "hard part" biến mất. Gateway chỉ cần đọc `referenceId` claim → inject header. Credential sống ở KC (auth_credentials), không đụng KH users table.

## 2. Task Breakdown (cross-layer, contract-first per `contract-first-for-cross-layer.md`)

| Bucket | Scope | Service |
|---|---|---|
| **0 (Foundation)** | api-contract `/api/v1/auth/login` (KC) + JWT claim shape `{sub, role, email, tenantId, referenceId, type:access, exp, HS512}` + credential model design. MERGE FIRST. | docs + kiteclass-core |
| **A (KC login)** | `auth/AuthController` + `AuthService` trong kiteclass-core: POST `/api/v1/auth/login` (email+password) → BCrypt verify → mint HS512 JWT với referenceId=domain entity id + tenantId=instance + role. Migration: `auth_credentials` (entity_type, entity_id, email UNIQUE/tenant, password_hash, instance_id) | kiteclass-core |
| **B (Provisioning)** | Parent: redeem set password (set-password token OR initial password trong invite). Teacher: TeacherController create → credential. Student: **DECISION §3** | kiteclass-core |
| **C (Gateway)** | `JwtAuthenticationGatewayFilter`: inject `X-User-Reference-Id` từ referenceId claim. Verify role PARENT/TEACHER/STUDENT accepted + tenantId resolution (GAP-711 reuse) works các role này. Register test instance trong gateway registry cho walk | kitehub-gateway |
| **D (Walk G3)** | Parent login → JWT → gateway :9000 → parent facet 200 (KC-8 G3 thật). Sweep 4 deferred controllers (Storage/Assignment/LessonProgress/Lms) header X-User-Id→X-User-Reference-Id + test sweep (GAP-798b item 5-6) | walk + core |
| **E (KC-9 build)** | Sau auth land: build KC-9 student portal BE joins (StudentPortalServiceImpl) + FE wiring (GAP-269b) + G1 walk. Có thể tách wave riêng | kiteclass-core + FE |

## 3. Scope

BE kiteclass-core: new `auth/` module (`AuthController` + `AuthService` + `auth_credentials` entity + Flyway migration) + JWT mint HS512. kitehub-gateway: `X-User-Reference-Id` injection filter (`JwtAuthenticationGatewayFilter`). Provisioning hooks (parent redeem / teacher create set credential). FE: login form các role (Bucket E+). Consumer authz (GAP-798 `@authz.hasAccessToChild` / student `requireStudentId`) đã ship — reuse. Gateway tenant resolution (GAP-711 JWT tenantId fallback) + HS512 validate (GAP-705) — reuse.

### 3.1 Open questions (chốt ở Bucket 0)

1. **Student account model:** own login (email+password) HAY inherited-via-parent (student data xem qua parent account)? GAP-725 ghi "tùy mô hình K-12 vs trung tâm". → Đề xuất: own login email+password cho consistency; parent-inherited là Phase 2 polish.
2. **Teacher credential provisioning:** TeacherController create có set password không, hay teacher invite flow? (GAP-725 Teacher=Hướng B email+pass).
3. **Credential store location:** KC `auth_credentials` table mới (đề xuất) vs extend existing. Email uniqueness scope: per-tenant hay global?
4. **Test instance registration:** instance test `aaaabbbb-…0001` chưa trong gateway registry (G3 walk thấy "unknown instance"). Cần seed 1 registered instance cho walk, hoặc dùng tenant thật (sky).
5. **HS512 vs HS256:** gateway access key HS512 (GAP-705). KC mint phải HS512 cùng JWT_SECRET.

## 4. State-Check Evidence

| Symbol | Verdict |
|---|---|
| KH PlatformRole = OWNER/STAFF only | ✅ confirmed (no PARENT/TEACHER/STUDENT) |
| ParentInvitation.redeem creates credential | ❌ no — chỉ parents row + link |
| Gateway inject X-User-Reference-Id | ❌ no (GAP-798b) — Bucket C target |
| Gateway TenantResolver JWT tenantId fallback | ✅ GAP-711 (reuse cho Option B) |
| Gateway JWT validate HS512 | ✅ JwtAuthenticationGatewayFilter (GAP-705 dual-secret) |
| Consumer authz reference-id | ✅ shipped GAP-798 (KC-8 G1 verified) |

## 5. Verification Gates

| Gate | Criteria |
|---|---|
| Bucket A | curl `/api/v1/auth/login` (core direct) → JWT với đúng claims; BCrypt verify đúng/sai |
| Bucket C | gateway :9000 forward X-User-Reference-Id (log + downstream nhận) |
| Bucket D (KC-8 G3) | parent login → gateway → `/api/v1/parent/me/children` 200 + facet 200 (NO manual headers) — đóng KC-8 G3 |
| Bucket E (KC-9) | student portal G1 walk (sau build) |

## 6. Agent Spawn Pattern

_(cross-service wave: Bucket 0 contract merge FIRST per `contract-first-for-cross-layer.md` → A/B/C parallel agents (worktree-isolated, Opus per `agent-model-opus-default.md`, background per `agent-background-spawn-default.md`) → D walk coordinator → E KC-9 tách wave riêng nếu lớn. Security-sensitive: mỗi bucket walk-verify trước DONE per `feature-ship-runtime-walk-mandate.md` §3.4 + `pre-handoff-self-test-completeness.md` §3 — no unverifiable security code.)_

## 7. Closure Protocol

1. Bucket 0 contract merge first → A/B/C parallel → D walk → E (KC-9 tách wave nếu lớn).
2. Per-bucket walk-verify trước DONE (security code).
3. Flip GAP-725 → DONE (Option B shipped) + GAP-798b → DONE (Option B simplification) khi KC-8 G3 walk PASS.
4. KC-8 campaign §4 G3 ⛔→✅; KC-9 ⛔→ build.
5. CSV + ROADMAP + wave-history sync.

## 8. Log

- **2026-06-06 (plan draft):** User chọn pull-forward parent/student auth (Phase 1, không đổi phase formally) + Option B (KC-native login email+password) sau KC-8 G3 phát hiện Phase-2 gate. Option B đơn giản hóa GAP-798b (KC mint token → referenceId trực tiếp, no cross-service population). Gateway reuse GAP-711 tenantId fallback + GAP-705 HS512 validate; chỉ thêm X-User-Reference-Id injection. Plan draft — **build trong session sạch riêng per GAP-798b "do not rush" mandate.** Defer OTP Hướng C Phase 2 thật.
