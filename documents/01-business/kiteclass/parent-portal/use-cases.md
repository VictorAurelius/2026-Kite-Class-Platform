# Parent Portal — Use Cases

**Domain:** KiteClass Core / Parent Portal
**Version:** 1.0 (Wave 2 MVP)
**Updated:** 2026-04-19

---

## Use Cases

### UC-PARENT-01: Admin/Teacher Invite Parent

**Actor:** Admin / Teacher (role check ở Gateway)
**Precondition:**
- User authenticated, có quyền trên tenant
- Feature flag `kiteclass.parent-portal.enabled = true` (BR-PARENT-004)
- Student tồn tại và chưa soft-deleted trong tenant

**Steps:**
1. FE (admin): Mở student detail → click "Mời phụ huynh" → input email
2. FE → Gateway: `POST /api/v1/parent-invitations` với `{studentId, parentEmail}`, kèm Bearer token
3. Gateway: Forward request kèm `X-User-Id` (inviter), `X-Tenant-Id`
4. Core: Validate `enabled=true` (BR-PARENT-004), nếu false → `503 PARENT_PORTAL_DISABLED`
5. Core: Load student bằng `findByIdAndDeletedFalse` — thiếu → `404 STUDENT_NOT_FOUND`
6. Core: Check `parentRepository.existsByEmailAndInstanceIdAndDeletedFalse(email, tenantId)` — đã có Parent ACTIVE → `409 PARENT_EMAIL_EXISTS` (BR-PARENT-INV-002)
7. Core: Tạo `ParentInvitation` (token UUID 128-bit, status PENDING, expiresAt = now + 24h, invitedByUserId)
8. Core: Publish `ParentInvitationEmailEvent` lên RabbitMQ best-effort (BR-PARENT-INV-006); failure log warn nhưng vẫn return 201
9. Core → FE: Trả `ParentInvitationResponse` kèm `token` (chỉ ở response create — BR-PARENT-INV-008)
10. FE: Toast "Lời mời đã được gửi", hiển thị link copy (cho trường hợp email worker down)

**Postcondition:**
- Row `parent_invitations` mới với status=PENDING
- Email enqueued (best-effort)
- Admin UI hiển thị invitation status trong list

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 401 | `X-User-Id` thiếu | "AUTH_REQUIRED" |
| 503 | Feature flag tắt | "PARENT_PORTAL_DISABLED" |
| 404 | Student không tồn tại | "STUDENT_NOT_FOUND" |
| 409 | Parent ACTIVE đã có cho email | "PARENT_EMAIL_EXISTS" |
| 400 | Email/studentId validation | "Email là bắt buộc" / "studentId là bắt buộc" |

---

### UC-PARENT-02: Parent Redeem Invitation Token

**Actor:** Phụ huynh (chưa có tài khoản)
**Precondition:**
- Phụ huynh nhận email với link `https://{tenant}.kiteclass.vn/parent-invite/{token}`
- Token còn hạn (status PENDING, `expiresAt > now`)

**Steps:**
1. Phụ huynh: Click link trong email → FE route `/parent-invite/[token]`
2. FE: Hiển thị form (password, fullName, phoneNumber optional, relationship)
3. Phụ huynh: Submit form
4. FE → Gateway: `POST /api/v1/auth/register-parent/{token}` với body `RedeemInvitationRequest`
5. Gateway: Forward → Core `POST /api/v1/parent-invitations/redeem/{token}` kèm `X-Tenant-Id`
6. Core: Load invitation bằng `findByTokenAndDeletedFalse` — thiếu → `404 PARENT_INVITATION_NOT_FOUND`
7. Core: Check `invitation.instanceId == tenantId` — không khớp → `404` (BR-PARENT-INV-010, defense in depth)
8. Core: Check status — không phải PENDING → `400 PARENT_INVITATION_ALREADY_USED`
9. Core: Check `expiresAt > now` — quá hạn → set status EXPIRED, save, `400 PARENT_INVITATION_EXPIRED`
10. Core: Lookup Parent bằng email + tenantId
    - **Nếu chưa có:** `createParent(...)` với status ACTIVE
    - **Nếu PENDING:** promote ACTIVE + ghi đè profile từ request (BR-PARENT-INV-005)
    - **Nếu ACTIVE:** giữ nguyên profile (BR-PARENT-INV-004 — child #2 cùng parent)
11. Core: Tạo `ParentStudentLink` nếu chưa có cho `(parent, student)` với linkType=PRIMARY (idempotent, BR-PARENT-LINK-002)
12. Core: Set invitation status REDEEMED, redeemedAt, redeemedParentId
13. Core → Gateway: Trả `RedeemInvitationResult { parentId, email, fullName, phoneNumber, relationship, linkedStudentIds }`
14. Gateway (Wave 5): Tạo `users` row với `userType=PARENT, referenceId=parentId`, hash password, mint JWT với claim `linked_student_ids`
15. FE: Auto-login, redirect dashboard `/parent`

**Postcondition:**
- Parent ACTIVE trong tenant
- Có `ParentStudentLink` cho student tương ứng
- Invitation REDEEMED
- (Wave 5) Gateway User tồn tại, JWT issued

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 503 | Feature flag tắt | "PARENT_PORTAL_DISABLED" |
| 404 | Token không tồn tại / cross-tenant | "PARENT_INVITATION_NOT_FOUND" |
| 400 | Token đã dùng (REDEEMED/EXPIRED/REVOKED) | "PARENT_INVITATION_ALREADY_USED" |
| 400 | Token quá hạn | "PARENT_INVITATION_EXPIRED" |
| 400 | Password yếu | "Mật khẩu phải có ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt" |
| 400 | Tên < 2 hoặc > 100 ký tự | "Tên phải từ 2-100 ký tự" |
| 400 | Phone sai format | "Số điện thoại không hợp lệ (phải là 10 số bắt đầu bằng 0)" |
| 400 | Relationship không hợp lệ | "Quan hệ phải là FATHER, MOTHER hoặc GUARDIAN" |

**FE behavior:**
- Side-effect khi token đã dùng: hiển thị CTA "Quay lại trang đăng nhập" thay vì retry
- Side-effect khi expired: hiển thị CTA "Liên hệ giáo viên để xin link mới"

---

### UC-PARENT-03: Parent Xem Profile Của Mình

**Actor:** Parent đã ACTIVE và đăng nhập
**Precondition:** JWT hợp lệ với `userType=PARENT`, Gateway populate `X-User-Reference-Id`

**Steps:**
1. FE (parent dashboard): Sau login, fetch `GET /api/v1/parent/me`
2. Gateway: Forward request kèm `X-User-Reference-Id = users.reference_id`
3. Core: `parentService.getParentById(parentId)` (read-only txn)
4. Core: Load Parent bằng `findByIdAndDeletedFalse` — thiếu → `404 PARENT_NOT_FOUND`
5. Core: Map sang `ParentResponse` (id, fullName, email, phoneNumber, relationship, status)
6. FE: Render profile card

**Postcondition:** Profile hiển thị

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 401 | `X-User-Reference-Id` thiếu | "AUTH_REQUIRED" |
| 404 | Parent không tồn tại / cross-tenant / soft-deleted | "PARENT_NOT_FOUND" |

---

### UC-PARENT-04: Parent Xem Danh Sách Children

**Actor:** Parent đã ACTIVE
**Precondition:** Parent có ≥1 `ParentStudentLink` trong tenant hiện tại

**Steps:**
1. FE: Fetch `GET /api/v1/parent/me/children`
2. Gateway: Forward kèm `X-User-Reference-Id`
3. Core: Load Parent (assert tenant scope, BR-PARENT-AUTH-004) — thiếu → `404 PARENT_NOT_FOUND`
4. Core: Query `linkRepository.findByParentIdWithStudent(parentId)` (JOIN FETCH student để tránh N+1)
5. Core: Map sang `List<ChildSummaryResponse>` — `className` + `grade` để null (Wave 5 sẽ enrich)
6. FE: Render list children, mỗi card có name + linkType badge (PRIMARY/SECONDARY)

**Postcondition:** List children hiển thị (có thể rỗng nếu chưa link)

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 401 | Header thiếu | "AUTH_REQUIRED" |
| 404 | Parent không thuộc tenant | "PARENT_NOT_FOUND" |

**FE behavior:**
- Empty state: "Chưa có học sinh nào được liên kết. Liên hệ giáo viên/admin để được mời."
- `className/grade = null`: hiển thị placeholder "—" (Wave 5 sẽ populate)

---

### UC-PARENT-05: Hourly Sweep Expire Pending Invitations

**Actor:** System scheduled job (`scheduledExpireStale`)
**Precondition:** Feature flag `enabled=true` (BR-PARENT-004); else early return

**Steps:**
1. Trigger: `@Scheduled(fixedRateString = "${kiteclass.parent-portal.expire-sweep-ms:3600000}")` mỗi giờ
2. Service: Check `properties.enabled()` — false → return (BR-PARENT-INV-007)
3. Service: Query `findByStatusAndExpiresAtBeforeAndDeletedFalse(PENDING, now)`
4. Service: Cho mỗi row → set status EXPIRED
5. Service: `saveAll(stale)`, log info `"Expired N stale parent invitations"`
6. Service: Catch + log error nếu exception (sweeper không được fail silent)

**Postcondition:** Tất cả PENDING invitations quá hạn chuyển EXPIRED

**Errors:** Internal — không trả ra API caller. Errors log ở mức ERROR.

**Note:** Single-instance MVP. Wave 5 sẽ wire scheduler-lock cho multi-instance deployment.

---

### UC-PARENT-06: Internal Gateway JWT Enrichment

**Actor:** kiteclass-gateway (system-to-system)
**Precondition:** HMAC signed request via `InternalRequestFilter`

**Steps:**
1. Login flow Wave 5: Gateway authenticate parent bằng password
2. Gateway: Cần `linked_student_ids` cho JWT claim + display profile
3. Gateway → Core: `GET /internal/parents/{id}` kèm HMAC signature
4. Core `InternalParentController`: Verify HMAC qua filter
5. Core: Load Parent → load `linkRepository.findStudentIdsByParentId(parentId)`
6. Core: Trả `ParentInternalResponse { id, email, fullName, phoneNumber, relationship, status, linkedStudentIds }`
7. Gateway: Mint JWT với `linked_student_ids`, populate `LoginResponse.profile = ParentProfileResponse`

**Postcondition:** Gateway có đủ data để issue JWT cho parent

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 401 | HMAC sai | "INVALID_SIGNATURE" (filter-level) |
| 404 | Parent không tồn tại | "PARENT_NOT_FOUND" |

**Hidden từ public Swagger** (`@Hidden`).

---

## Cross-cutting Behaviors

### Tenant Filter
Tất cả read transaction (`@Transactional(readOnly = true)`) tự động kích hoạt Hibernate `tenantFilter` (BR-PARENT-005). Caller không cần truyền `instance_id` — interceptor lấy từ `TenantContext`.

### Soft Delete
Repository methods chỉ trả `deletedFalse = true`. Không có hard delete trong domain này.

### Audit Fields
`BaseEntity` cung cấp `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `deleted`, `instanceId` cho mọi entity.
