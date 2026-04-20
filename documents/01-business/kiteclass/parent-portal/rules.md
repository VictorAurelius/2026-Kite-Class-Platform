# Parent Portal — Business Rules

**Domain:** KiteClass Core / Parent Portal
**Version:** 1.0 (Wave 2 MVP — GAP-052a + GAP-105)
**Updated:** 2026-04-19
**Source code:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/parent/`

---

## 1. Scope

Wave 2 MVP ship identity + invitation flow:
- `Parent` entity (profile, lifecycle status)
- `ParentInvitation` (token-based onboarding)
- `ParentStudentLink` (many-to-many parent ↔ student)
- Self-service endpoints (`/me`, `/me/children`)
- Internal endpoint cho Gateway JWT enrichment

Wave 5 sẽ thêm: messaging, fee payment widgets, attendance/grade/invoice projection, push notifications, PDPL consent UX. Các rule cho phần đó **không** thuộc tài liệu này.

---

## 2. Parent Account Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-PARENT-001 | Email duy nhất per tenant | Unique constraint `uk_parents_email_tenant` trên `(instance_id, email)`. Cùng email có thể là parent ở nhiều tenant khác nhau (rare but legal). |
| BR-PARENT-002 | Default status = PENDING | Parent rows mặc định `PENDING` cho đến khi redemption hoàn tất → `ACTIVE`. PENDING parents không login được. |
| BR-PARENT-003 | Invitation token TTL mặc định 24 giờ | Cấu hình qua `kiteclass.parent-portal.invitation-ttl-hours` (default 24). Sau TTL, sweeper chuyển `PENDING → EXPIRED`. **Code reference:** `ParentPortalProperties.java:15` javadoc + `ParentInvitationServiceImpl.java:115`. |
| BR-PARENT-004 | Feature flag `enabled` mặc định false | `kiteclass.parent-portal.enabled` (env: `PARENT_PORTAL_ENABLED`). Khi false, các invite + self-service endpoint trả `503 PARENT_PORTAL_DISABLED`. Wave 5 sẽ flip true cho instances đã ký PDPL. |
| BR-PARENT-005 | Tenant isolation | Mọi truy vấn lọc theo `instance_id` qua Hibernate `tenantFilter`. Parent chỉ thấy children trong cùng tenant; cross-tenant redemption bị từ chối với `404 PARENT_INVITATION_NOT_FOUND` (defense in depth). |
| BR-PARENT-006 | Soft delete only | Tất cả entities kế thừa `BaseEntity` với cờ `deleted`. Repository methods chỉ trả rows có `deletedFalse`. |
| BR-PARENT-007 | Identity tách 2 service | Profile (`parents` table) ở Core. Credential + JWT (`users` table) ở Gateway. Liên kết qua `users.reference_id = parents.id` AND `users.user_type = PARENT`. |
| BR-PARENT-008 | Phone optional, format VN 10 số | Pattern `^0\d{9}$` validate ở entity + DTO. |
| BR-PARENT-009 | Full name 2–100 ký tự | `@Size(min=2, max=100)` áp dụng cho `Parent.fullName` + `RedeemInvitationRequest.fullName`. |

**Parent statuses:** PENDING, ACTIVE, INACTIVE
**Parent relationships:** FATHER, MOTHER, GUARDIAN (default GUARDIAN nếu không truyền)

---

## 3. Parent ↔ Student Link Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-PARENT-LINK-001 | M-to-N parent ↔ student | 1 parent có nhiều children; 1 student có nhiều parents (cha + mẹ). Materialised qua `ParentStudentLink`, **không** plain `@ManyToMany` để giữ per-edge metadata. |
| BR-PARENT-LINK-002 | Unique edge (parent, student) | Unique constraint `uk_parent_student` trên `(parent_id, student_id)`. Idempotent — gọi link 2 lần không tạo duplicate. |
| BR-PARENT-LINK-003 | Link type mặc định PRIMARY | Edge đầu tiên (qua invitation redemption) = PRIMARY. SECONDARY dành cho parent thứ 2 thêm sau (chưa ship UI ở Wave 2). |
| BR-PARENT-LINK-004 | Notifications "1 parent only" target PRIMARY | Hành vi áp dụng ở Wave 5 notification service; rule được khai báo sẵn để consumer phụ thuộc đúng. |

**Link types:** PRIMARY, SECONDARY

---

## 4. Invitation Flow Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-PARENT-INV-001 | Token entropy 128-bit | `UUID.randomUUID().toString()` lưu ở `parent_invitations.token`, unique index `idx_inv_token`. |
| BR-PARENT-INV-002 | Reject invite nếu Parent ACTIVE đã tồn tại | Nếu email đã có Parent row (không soft-deleted) trong tenant → `409 PARENT_EMAIL_EXISTS`. PENDING invitations cho cùng email vẫn được tạo lại (user mất email cũ). |
| BR-PARENT-INV-003 | Status state machine | `PENDING → REDEEMED` (parent set password) hoặc `PENDING → EXPIRED` (sweeper) hoặc `PENDING → REVOKED` (admin huỷ — endpoint REVOKE chưa ship Wave 2). Không có transition nào khác. |
| BR-PARENT-INV-004 | Redemption idempotent — 2 children cùng parent | Nếu Parent ACTIVE đã tồn tại cho email khi redeem token thứ 2 (e.g., child #2), service **re-use** Parent row, chỉ tạo `ParentStudentLink` mới. |
| BR-PARENT-INV-005 | Profile chỉ ghi đè khi PENDING → ACTIVE | Lần redemption đầu (status `PENDING`), parent-supplied (fullName, phone, relationship) ghi đè placeholder. Lần thứ 2 (Parent đã ACTIVE), profile giữ nguyên. |
| BR-PARENT-INV-006 | Email phát hành best-effort | Invitation row là source of truth. Nếu RabbitMQ down, log warn nhưng vẫn return 201; admin có thể "resend" sau. |
| BR-PARENT-INV-007 | Sweeper hourly mặc định | `@Scheduled(fixedRateString = "${kiteclass.parent-portal.expire-sweep-ms:3600000}")`. Single-instance MVP — Wave 5 sẽ wire scheduler-lock nếu cần. Sweeper không chạy khi feature flag `enabled=false`. |
| BR-PARENT-INV-008 | Token chỉ trả khi tạo | `ParentInvitationResponse.token` chỉ populate ở response của `POST /api/v1/parent-invitations`; list endpoints không trả token (chống leak qua admin UI logs). |
| BR-PARENT-INV-009 | Inviter id required | `POST /api/v1/parent-invitations` yêu cầu `X-User-Id` (Gateway populate). Thiếu → `401 AUTH_REQUIRED`. Avoids un-attributed invitations. |
| BR-PARENT-INV-010 | Cross-tenant redemption từ chối với 404 | Nếu `invitation.instanceId != X-Tenant-Id`, return `404 PARENT_INVITATION_NOT_FOUND` (không 403 — chống enumeration). |

**Invitation statuses:** PENDING, REDEEMED, EXPIRED, REVOKED

---

## 5. Password Policy (Redemption)

Khi parent submit `RedeemInvitationRequest.password`:

| ID | Rule | Detail |
|----|------|--------|
| BR-PARENT-PWD-001 | Length 8–100 ký tự | `@Size(min=8, max=100)` |
| BR-PARENT-PWD-002 | Phải có chữ hoa, thường, số, ký tự đặc biệt | Regex `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#])[A-Za-z\d@$!%*?&#]{8,}$` |
| BR-PARENT-PWD-003 | Mirror Gateway `AuthServiceImpl.PASSWORD_PATTERN` | Duplicated intentionally — tránh cross-module dep, validate sớm ở Core trước khi Gateway thấy request. |

---

## 6. Identity & Access Pattern

| ID | Rule | Detail |
|----|------|--------|
| BR-PARENT-AUTH-001 | Self-service header `X-User-Reference-Id` | Gateway populate từ `users.reference_id` cho user có `userType = PARENT`. Core không có `users` table. |
| BR-PARENT-AUTH-002 | Header missing → 401 | `ParentController.requireParentId(...)` ném `AUTH_REQUIRED` nếu header rỗng. |
| BR-PARENT-AUTH-003 | Internal endpoint dùng HMAC | `/internal/parents/{id}` qua `InternalRequestFilter` (HMAC signature). Hidden khỏi public Swagger. |
| BR-PARENT-AUTH-004 | "Parent enumeration" trong cùng tenant chấp nhận được | `getChildrenOfParent(...)` load Parent trước khi list children → nếu parent thuộc tenant khác → 404. Chấp nhận leak "id thuộc tenant này hay không" vì id 64-bit. |

---

## 7. Config Keys

| Key | Default | Env Override | Description |
|-----|---------|--------------|-------------|
| `kiteclass.parent-portal.enabled` | `false` | `PARENT_PORTAL_ENABLED` | Master feature flag (BR-PARENT-004). |
| `kiteclass.parent-portal.invitation-ttl-hours` | `24` | — | Token lifetime giờ (BR-PARENT-003). |
| `kiteclass.parent-portal.redeem-base-url` | `https://app.kiteclass.vn/parent-invite/` | `PARENT_PORTAL_REDEEM_BASE_URL` | URL prefix gắn vào email (token append at send time). |
| `kiteclass.parent-portal.expire-sweep-ms` | `3600000` (1h) | — | Sweeper interval (BR-PARENT-INV-007). |

**Bound by:** `ParentPortalProperties` (record). Defaults được apply ở compact constructor để test minimal config không phải khai báo đủ.

---

## 8. Database Schema

### Tables
- `parents` — profile, status, relationship
- `parent_invitations` — token, status, expires_at, redeemed_at, redeemed_parent_id
- `parent_student_links` — join table với link_type

### Indexes
- `idx_parents_email`, `idx_parents_instance`, `idx_parents_status`
- `idx_inv_token` (unique), `idx_inv_email`, `idx_inv_status`
- `idx_psl_parent`, `idx_psl_student`

### Unique constraints
- `uk_parents_email_tenant` — `(instance_id, email)` (BR-PARENT-001)
- `uk_parent_student` — `(parent_id, student_id)` (BR-PARENT-LINK-002)

---

## 9. Email Integration

| Trigger | Template | Recipient | Routing |
|---------|----------|-----------|---------|
| Invitation created | `parent-invitation` | `invitation.email` | RabbitMQ exchange `email.exchange`, routing key `email.send` |

**Variables:** `parentEmail`, `studentName`, `redeemUrl`, `expiresAt`, `tokenTtlHours`

**Subject (vi):** "Lời mời liên kết tài khoản phụ huynh - KiteClass"

**Resilience:** Publish best-effort (BR-PARENT-INV-006). Email worker là `kitehub-subscription EmailQueueConfig`.

---

## 10. Future Work (Wave 5 — out of scope)

- Revoke endpoint cho admin (`POST /api/v1/parent-invitations/{id}/revoke`)
- Resend email endpoint
- Class/grade enrichment trong `ChildSummaryResponse` (currently null)
- Notification preferences per parent
- PDPL consent capture trước khi flip `enabled=true`
- Scheduler-lock cho expire sweeper khi multi-instance
- Push notifications + parent messaging
