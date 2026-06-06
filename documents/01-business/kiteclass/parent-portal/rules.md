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
| BR-PARENT-004 | Feature flag `enabled` mặc định **true** (Wave auth-1) | `kiteclass.parent-portal.enabled` (env: `PARENT_PORTAL_ENABLED`). **Default flip false→true tại Wave auth-1** (Bucket B redeem provisioning) để parent login được sau redeem. Khi false, các invite + self-service endpoint trả `503 PARENT_PORTAL_DISABLED`. **Lưu ý PDPL gate:** default true bỏ qua gate "instance đã ký PDPL" của thiết kế Wave 2 cũ — nếu PDPL consent vẫn là điều kiện bắt buộc cho 1 instance, set `PARENT_PORTAL_ENABLED=false` per-instance cho tới khi consent ký. Theo dõi quyết định PDPL trong follow-up gap. |
| BR-PARENT-005 | Tenant isolation | Mọi truy vấn lọc theo `instance_id` qua Hibernate `tenantFilter`. Parent chỉ thấy children trong cùng tenant; cross-tenant redemption bị từ chối với `404 PARENT_INVITATION_NOT_FOUND` (defense in depth). |
| BR-PARENT-006 | Soft delete only | Tất cả entities kế thừa `BaseEntity` với cờ `deleted`. Repository methods chỉ trả rows có `deletedFalse`. |
| BR-PARENT-007 | Identity — KC-native (Option B, Wave auth-1) | **Option A (superseded):** Credential + JWT ở Gateway `users` table, liên kết qua `users.reference_id = parents.id`. **Option B (hiện tại):** Profile (`parents` table) + credential (`auth_credentials` table, entity_type=PARENT, entity_id=parents.id) đều ở Core; Core tự mint JWT (HS512) với claim `referenceId = auth_credentials.entity_id`. Không còn cross-service `users.reference_id` population. Xem `tenant-auth/rules.md` BR-AUTH-001/003. |
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
| BR-PARENT-AUTH-001 | Self-service header `X-User-Reference-Id` (Option B) | Gateway re-inject từ claim `referenceId` của KC-native token (= `auth_credentials.entity_id`), sau khi strip giá trị client gửi (anti-spoof). **Option A (superseded):** populate từ `users.reference_id`. Core không có `users` table. Xem `tenant-auth/rules.md` BR-AUTH-HDR-001/002. |
| BR-PARENT-AUTH-002 | Header missing → 401 | `ParentController.requireParentId(...)` ném `AUTH_REQUIRED` nếu header rỗng. |
| BR-PARENT-AUTH-003 | Internal endpoint dùng HMAC | `/internal/parents/{id}` qua `InternalRequestFilter` (HMAC signature). Hidden khỏi public Swagger. |
| BR-PARENT-AUTH-004 | "Parent enumeration" trong cùng tenant chấp nhận được | `getChildrenOfParent(...)` load Parent trước khi list children → nếu parent thuộc tenant khác → 404. Chấp nhận leak "id thuộc tenant này hay không" vì id 64-bit. |

---

## 7. Config Keys

| Key | Default | Env Override | Description |
|-----|---------|--------------|-------------|
| `kiteclass.parent-portal.enabled` | `true` (Wave auth-1; was `false`) | `PARENT_PORTAL_ENABLED` | Master feature flag (BR-PARENT-004). Set false per-instance nếu PDPL consent chưa ký. |
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

---

## 11. K-12 LEGAL Phase 1A — Transcript read-only (Wave 18b1 Bucket D — GAP-321)

**Phase:** 1A (transcript read-only) — extends Wave 2 GAP-052a foundation
**Last-Reviewed:** 2026-05-04
**Reviewer-Approver:** @nguyenvankiet (acting Product Owner + acting Legal scout, solo-dev, 2026-05-04). Formal legal counsel review queued — see GAP-321b/c.
**Source:** GAP-321 Phase 1A (Wave 18b1 Bucket D); P5 K-12 persona review (1800 PH / 1200 HS scenario)
**Compliance:** **Compliant** — Luật Giáo dục 2019 Đ.83 K2 (parent right-to-information); PDPL Decree 13/2023 Art 16 (children-data special protection); Luật Trẻ em 2016 Đ.21+Đ.33
**Review-Cadence:** Annual + event-driven on Luật GD 2019 amendment OR Decree 13/2023 implementing-decree publication. **Next review:** 2027-05-04.

### 11.1 Scope of Phase 1A

> Phase 1A ships **transcript read-only** (1 facet) on top of Wave 2 GAP-052a (sections 1–10 above). Five other facets — điểm danh / học phí / hạnh kiểm / notifications / kỷ luật — and the multi-children selector polish, Zalo OTP login flow, per-read audit log are **deferred to GAP-321b (Phase 1B)**. PDPL parental-consent-flag tracking + Phase 2 write actions (complaints, RSVP, absence excuse) are **deferred to GAP-321c (Phase 1C)**.

Phase 1A proves the **end-to-end scope-guard pattern** that all subsequent facets reuse: a parent may only read data for children they are linked to via a non-deleted `ParentStudentLink` edge.

### 11.2 Rationale (why this exact scope)

- **Why transcript first?** Highest-frequency parent request in K-12 (báo điểm cuối kỳ / báo cáo tháng) and most legally-sensitive read (Đ.83 K2 explicitly enumerates "quá trình học tập"). Proving the guard on transcript = proving it on the most exposed surface first.
- **Why read-only?** Phase 1A's purpose is the boundary, not the workflow.
- **Why no Zalo OTP yet?** Existing Gateway PARENT user-type already issues JWT via email/password (V42 schema); Zalo is UX upgrade, not legal requirement. Deferred to GAP-321b.
- **Why no audit-log yet?** PDPL Art 16 spirit needs read-audit, but `BaseEntity` audits writes only; read-log table schema depends on Phase 1B facet shapes. Deferred to GAP-321b.

### 11.3 Phase 1A Rules

| ID | Rule | Detail | Phase |
|----|------|--------|-------|
| BR-PARENT-PORTAL-001 | Scope guard via ParentStudentLink | Every parent-side read endpoint MUST call `ParentStudentLinkRepository.existsByParentIdAndStudentIdAndDeletedFalse(parentId, childId)` BEFORE any data fetch. Returns 403 `PARENT_NOT_LINKED` if false. The boolean form (not the join-fetch) MUST be used so a non-linked caller never reaches the data table. | 1A |
| BR-PARENT-PORTAL-002 | Identity from Gateway header (Option B) | Authenticated parent id is read from the `X-User-Reference-Id` header. **Option B (hiện tại):** Gateway re-inject header từ verified JWT claim `referenceId` (= `auth_credentials.entity_id`), sau khi strip client-supplied value (anti-spoof). **Option A (superseded):** populate từ `users.reference_id` when `userType = PARENT`. Missing header → 401 `AUTH_REQUIRED`. Core never touches Gateway identity tables. See `tenant-auth/rules.md` BR-AUTH-HDR. | 1A |
| BR-PARENT-PORTAL-003 | Multi-tenant isolation | Hibernate `tenantFilter` applied via `@Transactional(readOnly = true)` ensures the parent + child + transcript rows all belong to the active `instance_id`. Cross-tenant snooping blocked at the filter layer in addition to the scope guard. | 1A |
| BR-PARENT-PORTAL-004 | Soft-deleted edge revokes access | A `ParentStudentLink` with `deleted = true` is treated as if it never existed for read access. Removing a parent from a child's record (e.g., divorce, custody change) MUST soft-delete the edge — never hard-delete (audit retention). | 1A |
| BR-PARENT-PORTAL-005 | Read-only Phase 1A API | Only `GET /api/v1/parent/children/{childId}/transcript`. POST/PUT/DELETE for write actions deferred to GAP-321c. | 1A |
| BR-PARENT-PORTAL-006 | Minimal projection per Đ.83 K2 | `TranscriptResponse` exposes only fields a parent has the legal right to see: semester, academicYear, totalCredits, semesterGpa, cumulativeGpa, course counts. Internal grade-component breakdown, teacher remarks, audit metadata are NOT projected. | 1A |
| BR-PARENT-PORTAL-007 | Linked children listed via existing `/me/children` | Children selector reuses the Wave 2 `GET /api/v1/parent/me/children` endpoint (returns `ChildSummaryResponse`). No new endpoint added in Phase 1A. | 1A |
| BR-PARENT-PORTAL-008 | Per-read audit log deferred | Every parent-side data read SHOULD emit an `AuditLog` entry. Phase 1A does NOT ship — schema design requires Phase 1B facet shapes. Deferred to **GAP-321b**. | 1B |
| BR-PARENT-PORTAL-009 | PDPL parental-consent flag deferred | `ParentStudentLink.parental_consent_granular` JSONB field for per-field-per-linkType visibility per Decree 13/2023 Art 16 deferred. Phase 1A treats PRIMARY = SECONDARY identically. Deferred to **GAP-321c**. | 1C |

### 11.4 Compliance citations

| Statute | Article | What it requires | How Phase 1A complies |
|---------|---------|------------------|------------------------|
| Luật Giáo dục 2019 | Đ.83 K2 | "Cha mẹ học sinh có quyền yêu cầu nhà trường, cơ sở giáo dục cung cấp đầy đủ thông tin về quá trình học tập, rèn luyện của con." | `GET /children/{childId}/transcript` exposes academic record. Phase 1A delivers transcript facet; Phase 1B (GAP-321b) delivers remaining 5 facets to fully satisfy "đầy đủ thông tin". |
| Decree 13/2023 (PDPL) | Art 16 | Special protection for children's personal data; minimization; parental consent. | `BR-PARENT-PORTAL-001` (scope guard) prevents access to non-linked children's data. `BR-PARENT-PORTAL-006` (minimal projection) ships only legally-required fields. Granular consent flag (`BR-PARENT-PORTAL-009`) deferred to GAP-321c with explicit acknowledgment. |
| Luật Trẻ em 2016 | Đ.21, Đ.33 | Children's privacy right; parents' duty to protect. | Scope guard operationalizes parents-only-see-their-own-children boundary. |

### 11.5 Out of Phase 1A scope (sister gaps)

| Item | Where |
|------|-------|
| 5 other facets (điểm danh, học phí, hạnh kiểm, notifications, kỷ luật) | **GAP-321b** Phase 1B |
| Multi-children selector polish | **GAP-321b** Phase 1B |
| Zalo OTP login flow | **GAP-321b** Phase 1B |
| Per-read audit log (BR-PARENT-PORTAL-008) | **GAP-321b** Phase 1B |
| PDPL granular parental-consent flag (BR-PARENT-PORTAL-009) | **GAP-321c** Phase 1C |
| Phase 2 write actions: complaints (GAP-339), RSVP (GAP-338), absence excuse | **GAP-321c** Phase 1C |
| Phase 3 multi-channel notification (GAP-063 wiring) | depends Wave 18a Bucket B |

### 11.6 Log

- **2026-05-04** Phase 1A K-12 LEGAL extension shipped. Wave 18b1 Bucket D (GAP-321 Phase 1A — transcript read-only on top of Wave 2 GAP-052a foundation). Source: Luật Giáo dục 2019 Đ.83 K2 + PDPL Decree 13/2023 Art 16. Reviewer: @nguyenvankiet (acting Product Owner + acting Legal scout, solo-dev). Compliance: Compliant; formal legal counsel review queued via GAP-321b/c. Cadence: Annual + event-driven.

---

## 12. K-12 LEGAL Phase 1B foundation — 4 facets + audit log skeleton (Wave 18b2 Bucket C — GAP-321b)

**Phase:** 1B foundation — extends §11 Phase 1A with 4 sibling read-only facets + per-read audit row skeleton
**Last-Reviewed:** 2026-05-04
**Reviewer-Approver:** @nguyenvankiet (acting Product Owner + acting Legal scout, solo-dev, 2026-05-04). Formal legal counsel review queued — see GAP-321b.
**Source:** Luật Giáo dục 2019 Đ.83 K2 (parent right-to-information requires "đầy đủ thông tin" — needs all facets, not transcript alone) + PDPL Decree 13/2023 Art 16 (audit traceability for children's data) + Luật Trẻ em 2016 Đ.21 (children's privacy right)
**Compliance:** **Compliant** — same statutory framework as §11; this section satisfies Đ.83 K2 "đầy đủ" requirement for the four most-requested facets and operationalizes the Art 16 traceability promise via the new audit row.
**Review-Cadence:** Annual + event-driven on Luật GD 2019 amendment OR Decree 13/2023 implementing-decree publication. **Next review:** 2027-05-04.

### 12.1 Scope of Phase 1B foundation (this PR)

Phase 1B foundation ships **4 sibling read-only facets** mirroring the Phase 1A scope-guard pattern + a **per-read audit log skeleton** (entity + service + V53 migration). Items explicitly NOT in this PR (carried forward inside GAP-321b for follow-up sub-PRs): Zalo OTP login, multi-children selector polish, the discipline (kỷ luật) facet, write actions, and the admin/safeguarding query surface for the audit log. Concrete data-source for the conduct + notifications facets is also deferred — those endpoints ship as v1 stubs returning empty results so the FE can wire against the contract immediately.

### 12.2 Phase 1B foundation Rules

| ID | Rule | Detail | Phase |
|----|------|--------|-------|
| BR-PARENT-AUDIT-001 | Per-read audit row required for every parent-side facet read | Every successful parent-side facet endpoint (transcript / attendance / fees / conduct / notifications) MUST emit one row in `parent_read_audit_log` (parent_id, child_id, facet, read_at, instance_id) BEFORE returning data. The write is best-effort — if the audit store is unavailable, the read still returns 200 (logged warn). PDPL Decree 13/2023 Art 16 traceability + Luật Trẻ em 2016 Đ.21 children's-privacy traceability. | 1B foundation |
| BR-PARENT-FACET-ATT-001 | Attendance facet scope guard | `GET /api/v1/parent/children/{id}/attendance` rejects with 403 `PARENT_FACET_FORBIDDEN` if no active `ParentStudentLink` edge between authenticated parent and `childId`. The boolean `existsByParentIdAndStudentIdAndDeletedFalse` query is used so a non-linked caller never reaches `attendance_period`. Reuses BR-PARENT-PORTAL-001 pattern; same 401/400/403 ladder. | 1B foundation |
| BR-PARENT-FACET-FEES-001 | Fees facet scope guard | `GET /api/v1/parent/children/{id}/fees` rejects with 403 `PARENT_FACET_FORBIDDEN` if no active link. v1 maps from existing `Invoice` rows; date-range narrowing, instalment + payment-history join deferred to GAP-321b.1. Minimum projection: invoiceNumber, status, totalAmount, balanceDue, dueDate (Đ.83 K2 right-to-information for fees parents owe). | 1B foundation |
| BR-PARENT-FACET-CONDUCT-001 | Conduct facet scope guard | `GET /api/v1/parent/children/{id}/conduct` rejects with 403 `PARENT_FACET_FORBIDDEN` if no active link. v1 returns an empty list — backing schema for digital hạnh kiểm rating is not yet present. Concrete source-of-truth lands in GAP-321b.1. | 1B foundation |
| BR-PARENT-FACET-NOTIFY-001 | Notifications facet scope guard | `GET /api/v1/parent/children/{id}/notifications` rejects with 403 `PARENT_FACET_FORBIDDEN` if no active link. v1 returns an empty page — cross-cutting notification engine ships in Wave 18a Bucket B (GAP-063b). | 1B foundation |

**Source (5-attribute frontmatter applied):**
- **Source:** Luật Giáo dục 2019 Đ.83 K2 + PDPL Decree 13/2023 Art 16 + Luật Trẻ em 2016 Đ.21 (statute citations); P5 K-12 persona-review notes (1800 PH / 1200 HS scenario, Wave 18b1 Bucket D)
- **Rationale:** Why these 4 facets (not 1, not 6)? Đ.83 K2 enumerates "quá trình học tập, rèn luyện" — academic transcript (Phase 1A) + period attendance + fee status + conduct rating + notifications cover ≥80% of parent inquiries observed in pilot data. Discipline (kỷ luật) is sensitive enough to require Phase 1C consent flag work + write surface, deferred. Why audit row skeleton (not full enrichment)? PDPL Art 16 demands traceability; v1 minimum (parent_id, child_id, facet, read_at) answers the legal question "who read what when"; IP/UA/request_id enrichment is operational, deferable.
- **Reviewer:** @nguyenvankiet (acting Product Owner + acting Legal scout, solo-dev, 2026-05-04). Formal legal counsel review queued — GAP-321b acceptance criteria.
- **Compliance check:** **Compliant** — Luật GD 2019 Đ.83 K2; PDPL Decree 13/2023 Art 16; Luật Trẻ em 2016 Đ.21+Đ.33.
- **Review cadence:** Annual + event-driven on Luật GD 2019 amendment OR Decree 13/2023 implementing-decree publication.

### 12.3 Phase 1B foundation API surface

```
GET /api/v1/parent/children/{childId}/attendance?from=&to=
GET /api/v1/parent/children/{childId}/fees?from=&to=
GET /api/v1/parent/children/{childId}/conduct?period=
GET /api/v1/parent/children/{childId}/notifications?from=&to=
```

All four:
- Require `X-User-Reference-Id` header (Gateway-injected)
- Apply Hibernate `tenantFilter` via `@Transactional(readOnly = true)`
- Reject unlinked parents with 403 `PARENT_FACET_FORBIDDEN` (BR-PARENT-FACET-{ATT,FEES,CONDUCT,NOTIFY}-001)
- Emit one `parent_read_audit_log` row on success (BR-PARENT-AUDIT-001)

### 12.4 Database schema — `parent_read_audit_log` (V53)

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| instance_id | UUID NOT NULL | Multi-tenant isolation |
| parent_id | BIGINT NOT NULL | Authenticated parent id |
| child_id | BIGINT NOT NULL | Whose data was read |
| facet | VARCHAR(20) CHECK in enum | TRANSCRIPT / ATTENDANCE / FEES / CONDUCT / NOTIFICATIONS |
| read_at | TIMESTAMP NOT NULL | Server-side timestamp at the moment 200 returned |
| audit cols | per BaseEntity | created_at / updated_at / created_by / updated_by / deleted / version |

Indexes: `(parent_id, child_id, read_at)` primary; `(instance_id, facet)` aggregations; `deleted` standard.

**Retention:** 5 years (financial-record class per Nghị định 13/2023). Sweeper deferred to GAP-321b follow-up.

### 12.5 Out of Phase 1B foundation scope (sister gaps + this gap follow-up sub-PRs)

| Item | Where |
|------|-------|
| Zalo OTP login flow | **GAP-321b.2** |
| Multi-children selector polish | **GAP-321b.3** |
| Discipline (kỷ luật) facet | **GAP-321c** |
| Audit log: IP / user_agent / request_id capture | **GAP-321b.4** |
| Audit log: 5-year retention sweeper | **GAP-321b.4** |
| Audit log: admin/safeguarding-officer query surface | **GAP-321b.4** |
| Conduct facet concrete data source | **GAP-321b.1** |
| Notifications facet wiring (depends GAP-063b) | **GAP-321b.1** |
| Fees facet date-range narrowing + instalment join | **GAP-321b.1** |
| PDPL granular parental-consent flag | **GAP-321c** |

### 12.6 Log

- **2026-05-04** Phase 1B foundation shipped — Wave 18b2 Bucket C (GAP-321b foundation): 4 read-only facets (attendance / fees / conduct / notifications) + per-read audit log skeleton (V53 migration + entity + service). 5 new BR rules (BR-PARENT-AUDIT-001 + BR-PARENT-FACET-{ATT,FEES,CONDUCT,NOTIFY}-001) added with 5-attribute frontmatter. Conduct + notifications facets ship as v1 stubs returning empty results; concrete sources deferred to GAP-321b.1. Reviewer: @nguyenvankiet (acting Product Owner + acting Legal scout, solo-dev). Compliance: Compliant; formal legal counsel review queued via GAP-321b. Cadence: Annual + event-driven.

---

## 13. K-12 LEGAL Phase 1B remainder — fees facet real wiring + N+1 protection (Wave 18b3 Bucket C — GAP-321b)

**Phase:** 1B remainder — extends §12 foundation by replacing the fees v1 stub query with a date-range-narrowing JPQL + `@EntityGraph` to prevent N+1; conduct + notifications facets stay v1 stubs (state-check below) with explicit follow-up sub-gaps.
**Last-Reviewed:** 2026-05-04
**Reviewer-Approver:** @nguyenvankiet (acting Product Owner + acting Legal scout, solo-dev, 2026-05-04). Formal legal counsel review queued — see GAP-321b.
**Source:** Audit-to-gap state-check (per `audit-to-gap-pipeline.md` Step 2.5 hardened protocol) on `Incident.visibilityScope` + `Notification` entity — both confirmed missing from current schema; wiring to a non-existent column / entity is impossible. State-check log inlined in §13.3 below.
**Compliance:** **Compliant** — same Đ.83 K2 + PDPL Art 16 framing as §11/§12. Adding date-range narrowing + N+1 protection to fees does NOT loosen the scope guard (BR-PARENT-FACET-FEES-001 unchanged); the change is a pure performance + query-precision improvement.
**Review-Cadence:** Annual + event-driven on Luật GD 2019 amendment OR Decree 13/2023 implementing-decree publication. **Next review:** 2027-05-04.

### 13.1 Scope of Phase 1B remainder (this PR)

Phase 1B remainder ships **fees facet real wiring** (date-range narrowing + `@EntityGraph` for items + adjustments + assertion test ≤3 prepared statements). Conduct + notifications facets remain v1 stubs because the upstream artifacts they depend on do not yet exist:

- **Conduct:** `Incident.visibilityScope` column does not exist (state-check 2026-05-04). The wave plan §3 Bucket C originally proposed filtering `Incident` by `visibilityScope IN (PARENT_VISIBLE, PUBLIC)` per BR-CHILD-PROTECT-005, but neither the field nor that BR exist in the codebase or `documents/01-business/kiteclass/child-protection/rules.md`. Querying `Incident` by `subjectStudentId` *without* a visibility filter risks PDPL Art 16 violation (special protection for children's data) — surfacing unverified `REPORTED` incidents to parents could leak unfounded accusations. Honest path: keep stub, file follow-up.
- **Notifications:** No `Notification` entity exists in `kiteclass-core`; KiteHub's `NotificationPreference` is unrelated tier-preference data. Per BR-PARENT-FACET-NOTIFY-001 the cross-cutting notification engine ships in Wave 18a Bucket B (GAP-063b) which has not yet shipped. Honest path: keep stub, file follow-up.

### 13.2 Phase 1B remainder Rules

| ID | Rule | Detail | Phase |
|----|------|--------|-------|
| BR-PARENT-FACET-FEES-002 | Fees facet date-range narrowing | The fees JPQL query MUST narrow by `dueDate BETWEEN :from AND :to` (inclusive) and apply `@EntityGraph(attributePaths = {"items", "adjustments"})` so the parent-portal fee-period drill-down avoids N+1. Hibernate Statistics test asserts ≤3 prepared statements per facet call (1 count + 1 page + ≤1 collection-prefetch coalesce). | 1B remainder |
| BR-PARENT-FACET-CONDUCT-002 | Conduct facet visibility-scope filter | The conduct facet JPQL query MUST filter `Incident` by `subjectStudentId = :childId AND visibilityScope IN ('PARENT_VISIBLE', 'PUBLIC') AND deleted = false` per **BR-CHILD-PROTECT-005** (Phase 1C v1, Wave 19 Bucket A). The default `STAFF_ONLY` (set on every legacy + newly-created Incident per V54 migration) ensures abuse / grooming / CSAM records cannot leak through this surface. The query carries `@EntityGraph(attributePaths = {})` + Hibernate Statistics test asserts ≤3 prepared statements per facet call (1 SELECT + ≤2 internal coalesce). The encrypted `description` field is NOT projected — only the plaintext `title` (as `remark`) + a coarse hạnh kiểm rating projected from `severity` (LOW→TỐT / MEDIUM→KHÁ / HIGH→TRUNG_BÌNH / CRITICAL→YẾU). Real wiring shipped Wave 19 Bucket D (GAP-321b-1-conduct). | 1C v1 (real wiring) |
| BR-PARENT-FACET-NOTIFY-002 | Notifications stub stays until GAP-063b | The notifications facet returns empty UNTIL the cross-cutting notification engine ships and exposes a parent-audience-scoped read API. The audience-scope contract (`PARENT` / `ALL_PARENTS`) will be validated then. Tracked in **GAP-321b.1-notifications-engine-wiring**. | 1B remainder |

**Source (5-attribute frontmatter applied):**
- **Source:** Audit-to-gap state-check 2026-05-04 (per `audit-to-gap-pipeline.md` Step 2.5 hardened protocol — `grep -rn 'visibilityScope\|audienceScope' kiteclass/kiteclass-core/src/main/java kiteclass/kiteclass-core/src/main/resources/db/migration documents/01-business/kiteclass/child-protection` returned 0 matches; `find kiteclass/kiteclass-core/src/main/java -name 'Notification.java'` returned 0 matches in module-domain scope) + Wave 18b3 plan §3 Bucket C trade-off note "join-heavy SQL via JPQL vs native queries → JPQL + @EntityGraph"
- **Rationale:** Fees facet has the source data (`Invoice` + `InvoiceItem` + `InvoiceAdjustment` already shipped Wave 2.8.0; `dueDate` already indexed) — narrowing + N+1 protection is a pure perf upgrade with measurable verification (assertSelectCount). Conduct + notifications lack their upstream dependency and fabricating a query against absent schema would be drift, not progress. Why explicit 5-attribute BR rather than silent stub-stay? Visibility (BR-PARENT-FACET-CONDUCT-002 + BR-PARENT-FACET-NOTIFY-002 tell future readers the rationale + sub-gap).
- **Reviewer:** @nguyenvankiet (acting Product Owner + acting Legal scout, solo-dev, 2026-05-04). Formal legal counsel review queued — GAP-321b acceptance criteria.
- **Compliance check:** **Compliant** — Luật GD 2019 Đ.83 K2 (right-to-information for fees parents owe, served by date-range narrowing); PDPL Decree 13/2023 Art 16 (children's data minimization — stub-stay for conduct prevents over-disclosure of unverified incidents); Luật Trẻ em 2016 Đ.21 (children's privacy right — same).
- **Review cadence:** Annual + event-driven on Luật GD 2019 amendment OR Decree 13/2023 implementing-decree publication.

### 13.3 State-check log (per `audit-to-gap-pipeline.md` Step 2.5 hardened protocol)

| Artifact | Command | Match count | Conclusion |
|----------|---------|------------|-----------|
| `Incident.visibilityScope` field | `grep -rn "visibilityScope\|visibility_scope" kiteclass/kiteclass-core/src/main/java kiteclass/kiteclass-core/src/main/resources/db/migration documents/01-business/kiteclass/child-protection` | 0 | Field does not exist anywhere — schema, code, business rules. Querying it would fail at compile time (Java) and at runtime (no SQL column). |
| `BR-CHILD-PROTECT-005` rule | `grep -rn "BR-CHILD-PROTECT-005" documents/01-business/kiteclass/child-protection/rules.md` | 0 | Rule does not exist in source-of-truth. The wave plan §3 Bucket C reference was aspirational; rule must be authored first (out of scope for this bucket — would touch `documents/01-business/kiteclass/child-protection/rules.md` outside this bucket's allowlist). |
| `Notification` entity in `kiteclass-core` | `find kiteclass/kiteclass-core/src/main/java -name "*Notification*.java"` (full output, no head per Step 2.5 ban) | 0 in `module/` domain scope | No parent-targeted notification entity. KiteHub's `NotificationPreference` (tier preferences) is unrelated. Per BR-PARENT-FACET-NOTIFY-001 the cross-cutting engine ships in GAP-063b (Wave 18a Bucket B, not yet shipped). |
| `audienceScope` field | `grep -rn "audienceScope\|audience_scope" kiteclass/kiteclass-core/src/main/java` | 0 | Same conclusion as above — field cannot be filtered against absent entity. |

State-check verdict: 2 of 3 facets (conduct + notifications) cannot ship real wiring this bucket without out-of-allowlist work (Incident schema migration, child-protection rules.md edits, Notification entity authoring). Honest scope-cut + sub-gap filing per `gap-done-discipline.md` §3 PARTIAL exit-ramp.

### 13.4 Out of Phase 1B remainder scope (sister sub-gaps filed this PR)

| Item | Where | Why deferred |
|------|-------|--------------|
| Conduct facet real query against `Incident` filtered by `visibilityScope` | ✅ Shipped Wave 19 Bucket D — **GAP-321b-1-conduct** | Wave 19 Bucket A (GAP-322c v1) shipped `IncidentVisibilityScope` enum + V54 migration + BR-CHILD-PROTECT-005 in same wave; Bucket D consumed those upstream artifacts and real-wired `ParentConductFacetServiceImpl` |
| Notifications facet real query against `Notification` filtered by `audienceScope` | **GAP-321b.1-notifications-engine-wiring** | Requires GAP-063b cross-cutting notification engine to ship first |
| Fees facet instalment join + payment-history projection | **GAP-321b.1-fees-instalment-payment-history** | Phase 1B remainder ships date-range narrowing + items/adjustments graph; instalment + payment-history join is incremental v2 work |

### 13.5 Log

- **2026-05-05** Phase 1C v1 — Wave 19 Bucket D (GAP-321b-1-conduct): conduct facet real wiring shipped. `ParentConductFacetServiceImpl` now queries `IncidentRepository.findVisibleForParentList(childId, [PARENT_VISIBLE, PUBLIC])` per BR-CHILD-PROTECT-005 (consumed from Bucket A). BR-PARENT-FACET-CONDUCT-002 flipped from "stub stays until visibility schema" to "real wiring with visibility-scope filter." Hạnh kiểm rating projected coarsely from `Incident.severity` (LOW→TỐT / MEDIUM→KHÁ / HIGH→TRUNG_BÌNH / CRITICAL→YẾU) until digital rating store ships. Encrypted `description` never projected — only plaintext `title` surfaces as `remark`. New IT `ParentConductFacetEntityGraphIT` asserts assertSelectCount ≤3 prepared statements + STAFF_ONLY exclusion regression. Reviewer: @nguyenvankiet (acting Product Owner + acting Legal scout, solo-dev). Compliance: Compliant — Luật GD 2019 Đ.83 K2 (right-to-information served) + PDPL Decree 13/2023 Art 16 (children's data minimization preserved via STAFF_ONLY default + scope filter). Cadence: Annual + event-driven.
- **2026-05-04** Phase 1B remainder shipped — Wave 18b3 Bucket C (GAP-321b remainder): fees facet real JPQL with `@EntityGraph(items, adjustments)` + date-range narrowing + Hibernate Statistics assertSelectCount ≤3 test. Conduct + notifications facets stay v1 stubs per state-check (§13.3). 3 follow-up sub-gaps filed (GAP-321b.1-{conduct,notifications,fees-v2}). 3 new BR rules (BR-PARENT-FACET-FEES-002, BR-PARENT-FACET-CONDUCT-002, BR-PARENT-FACET-NOTIFY-002) with 5-attribute frontmatter. Bucket ships PARTIAL not DONE — fees fully wired, conduct + notifications honestly deferred. Reviewer: @nguyenvankiet (acting Product Owner + acting Legal scout, solo-dev). Compliance: Compliant; formal legal counsel review queued via GAP-321b. Cadence: Annual + event-driven.

---

## 14. K-12 LEGAL Phase 1C v1 — PDPL granular consent + 1 write action (Wave 19 Bucket C — GAP-321c)

**Phase:** 1C v1 — extends §11 + §12 + §13 with PDPL Decree 13/2023 Art 16 granular per-field consent + 1 write surface (complaint) wiring
**Last-Reviewed:** 2026-05-05
**Reviewer-Approver:** @nguyenvankiet (acting Product Owner + acting Legal scout, solo-dev, 2026-05-05). Formal legal counsel review queued — see GAP-321c follow-up filed in this PR.
**Source:** PDPL Decree 13/2023 (Nghị định 13/2023/NĐ-CP) Art 16 — special protection for children's personal data; minimization; parental consent. Luật Giáo dục 2019 Đ.83 K2 — implicit communication right (complaint write surface).
**Compliance:** **Compliant** — PDPL Art 16 (granular consent flag is the operationalization of the consent obligation; default is fail-safe deny); Đ.83 K2 satisfied for complaint capture.
**Review-Cadence:** Annual + event-driven on Decree 13/2023 implementing-decree publication. **Next review:** 2027-05-05.

### 14.1 Phase 1C v1 Rules

| ID | Rule | Detail | Phase |
|----|------|--------|-------|
| BR-PARENT-PORTAL-011 | PDPL granular consent gate (per-field) | Every parent-side facet API MUST call `ConsentService.checkConsent(parentId, childId, field)` AFTER the existing scope guard (`existsByParentIdAndStudentIdAndDeletedFalse`) and BEFORE returning data. Default-deny: missing key in the JSONB `parental_consent.fields` map → 403 `PARENT_CONSENT_REQUIRED`. Wave 19 Bucket C wires fees facet end-to-end; remaining facets gate-deferred to GAP-321c follow-up. | 1C v1 |
| BR-PARENT-PORTAL-012 | Consent versioning + sparse PUT | `PUT /api/v1/parent/consent?childId=...` accepts a sparse `{updates: {field: bool}}` map; ConsentService merges over existing flags, bumps `parental_consent.version`, and stamps `updatedAt`. Re-consent prompt on policy version bump deferred to GAP-321c follow-up. | 1C v1 |
| BR-PARENT-PORTAL-013 | Complaint write v1 (linked-child only) | `POST /api/v1/parent/complaints` accepts `{studentId, complaintText}` (10–2000 chars). Same scope-guard pattern as facet reads — 403 `PARENT_FACET_FORBIDDEN` if no link. Persists row in `parent_complaint_queue` with status=PENDING. Workflow (4-level escalation, attachments, resolver UI) deferred to GAP-339. | 1C v1 |

**Source (5-attribute frontmatter applied):**
- **Source:** PDPL Decree 13/2023 Art 16 (statute citation — special protection for children's personal data; minimization; parental consent); Luật Giáo dục 2019 Đ.83 K2 (statute citation — communication right covers parent-initiated complaints); P5 K-12 persona-review notes (1800 PH / 1200 HS scenario, Wave 18b1 Bucket D); Wave 18b3 closure deferral list flagged BR-PARENT-PORTAL-009 + 4 write actions for Phase 1C.
- **Rationale:** Why per-field gate (not link-level)? Art 16 demands minimization — granting consent for transcript ≠ granting consent for fees data. Per-field flag in JSONB future-proofs additions (new facet → new key, no migration). Why default-deny? PDPL is fail-safe: implementing-decree Art 12 K2 b says "consent must be specific and explicit"; missing flag = no explicit consent. Why JSONB (not separate table)? Per-field count is small (≤10 facets), shape is read-heavy (1 read per facet API call), Postgres JSONB GIN-index support exists if hot. Why ship 1 write action (not 4)? Complaints have NO upstream dependency (existing `parents`+`students` schema sufficient) — 3 others (conduct-confirm, meeting RSVP, absence-excuse upload) need GAP-338 (meeting), GAP-339 (full complaint workflow), or MinIO encrypted bucket pattern. Honest scope-cut.
- **Reviewer:** @nguyenvankiet (acting Product Owner + acting Legal scout, solo-dev, 2026-05-05). Formal legal counsel review queued — GAP-321c follow-up acceptance criteria.
- **Compliance check:** **Compliant** — PDPL Decree 13/2023 Art 16 (granular consent operationalized via per-field JSONB flag + version bump); Luật Giáo dục 2019 Đ.83 K2 (complaint write surface satisfies "communication" leg of right-to-information); Luật Trẻ em 2016 Đ.21 (children's privacy traceability already covered by §12 BR-PARENT-AUDIT-001).
- **Review cadence:** Annual + event-driven on Decree 13/2023 implementing-decree publication OR PDPL Art 16 amendment.

### 14.2 Database schema — V56 migration

| Change | Detail |
|--------|--------|
| `parent_student_links.parental_consent` | New JSONB column, NOT NULL, default `{"fields":{}, "version":1, "updatedAt":null}`. Existing rows from V42 backfilled by the DEFAULT clause (backward-compatible — no manual ETL). |
| `parent_complaint_queue` | New table (id, instance_id, parent_id, student_id, complaint_text, status, resolved_at, audit cols). Status state machine v1: PENDING → IN_REVIEW → RESOLVED/REJECTED. Indexes on parent_id, student_id, instance_id, status. |

### 14.3 Phase 1C v1 API surface

```
GET  /api/v1/parent/consent?childId=                              ← read consent
PUT  /api/v1/parent/consent?childId=        body: {updates: {...}} ← sparse update + bump
POST /api/v1/parent/complaints              body: {studentId, complaintText} ← write
```

All three:
- Require `X-User-Reference-Id` header (Gateway-injected) → 401 `AUTH_REQUIRED` if missing
- Apply Hibernate `tenantFilter`
- PUT consent → 404 `PARENT_CONSENT_LINK_NOT_FOUND` if no link (vs 403 — informational, not authorization)
- POST complaint → 403 `PARENT_FACET_FORBIDDEN` if no link

### 14.4 Out of Phase 1C v1 scope (sister sub-gaps + GAP-321c follow-up)

| Item | Where | Why deferred |
|------|-------|--------------|
| 3 remaining write actions (`conduct-confirm`, meeting `rsvp`, `absence-excuse` upload) | **GAP-321c follow-up (filed this PR)** | Depends GAP-338 (meeting entity) + GAP-339 (full complaint workflow) + MinIO encrypted bucket per Phase 1B GAP-322b pattern |
| Apply consent gate to remaining 4 facets (transcript, attendance, conduct, notifications) | **GAP-321c follow-up** | Bucket scope was 1 facet end-to-end; remaining facets need same wiring + IT updates per facet |
| Re-consent flow on policy version bump | **GAP-321c follow-up** | Needs admin tooling to bulk-bump policy version + FE prompt UX |
| i18n EN + zh-CN catalogs | **GAP-321c follow-up** | International schools (FIS, BIS) need EN/zh-CN; v1 ships Vietnamese-only |
| Settings page UI (consent toggles per child) | **GAP-321c follow-up** | This bucket ships endpoints; FE consumer ships separately |

### 14.5 Log

- **2026-05-05** Phase 1C v1 shipped — Wave 19 Bucket C (GAP-321c v1): PDPL granular consent JSONB column on `parent_student_links` + ConsentService gate (checkConsent / getConsent / getConsentVersion / bumpConsent) + ParentConsentController GET/PUT settings endpoints + ParentComplaintController POST `/complaints` v1 with scope guard + V56 migration (additive JSONB + complaint queue table) + 3 new BR rules (BR-PARENT-PORTAL-011..013) with 5-attribute frontmatter. Fees facet wired end-to-end with consent gate (BR-PARENT-PORTAL-011 — `PARENT_CONSENT_REQUIRED` 403 when consent missing); 4 remaining facets + 3 write actions + i18n + UI honestly deferred via GAP-321c follow-up. Bucket ships PARTIAL not DONE — fees gated end-to-end, complaint write v1 capturing data, consent settings endpoints live; remaining write actions + multi-facet gate + UI to follow. Reviewer: @nguyenvankiet (acting Product Owner + acting Legal scout, solo-dev). Compliance: Compliant; formal legal counsel review queued via GAP-321c follow-up. Cadence: Annual + event-driven on Decree 13/2023 amendment.

---

## 15. K-12 LEGAL Phase 1C v1.5 — uniform consent gate × 5 facets + re-consent flow (Wave 24 Bucket C — GAP-361)

**Phase:** 1C v1.5 — extends §14 v1 by (a) wiring the per-field consent gate uniformly across all 5 parent-side facets (transcript / attendance / fees / conduct / notifications) and (b) adding a re-consent flow (admin bulk-bump endpoint + middleware version check on every facet impl).
**Last-Reviewed:** 2026-05-06
**Reviewer-Approver:** @nguyenvankiet (acting Product Owner + acting Legal scout, solo-dev, 2026-05-06). Formal legal counsel review queued — GAP-361 follow-up.
**Source:** PDPL Decree 13/2023 (Nghị định 13/2023/NĐ-CP) Art 16 K2 d — when scope of personal-data processing changes, parental consent must be re-obtained; Wave 19 Bucket C closure deferral list (GAP-361 §B + §C).
**Compliance:** **Compliant** — Art 16 K2 d operationalized via stored consent version + required-policy version + admin bulk-bump tooling.
**Review-Cadence:** Annual + event-driven on Decree 13/2023 implementing-decree publication. **Next review:** 2027-05-06.

### 15.1 Phase 1C v1.5 Rules

| ID | Rule | Detail | Phase |
|----|------|--------|-------|
| BR-PARENT-PORTAL-014 | Uniform consent gate across all 5 facets | The PDPL granular consent gate (BR-PARENT-PORTAL-011) MUST be applied uniformly across ALL 5 parent-side facet endpoints (transcript / attendance / fees / conduct / notifications). Each facet impl calls `ConsentService.checkConsent(parentId, childId, "<facetName>")` AFTER the existing scope guard (`existsByParentIdAndStudentIdAndDeletedFalse`) and BEFORE returning data. Default-deny: missing key in the JSONB `parental_consent.fields` map → 403 `PARENT_CONSENT_REQUIRED`. The exact field-name strings are exposed as `public static final String CONSENT_FIELD_*` constants on each facet impl so the FE settings page + tests reference one symbol per facet. | 1C v1.5 |
| BR-PARENT-PORTAL-015 | Re-consent on policy version bump | Every facet impl MUST verify `ConsentService.getConsentVersion(parentId, childId) >= ConsentService.getRequiredVersion()` AFTER `checkConsent` and BEFORE returning data. If the stored version is strictly below the required policy version → 403 `RECONSENT_REQUIRED` (FE prompts re-confirmation). The required-policy version is read from configuration key `kite.parent.consent.required-version` (default `1`). Admin bumps the version via the bulk-bump endpoint (BR-PARENT-PORTAL-016) when the privacy policy is amended (e.g., a new facet is added). PDPL Decree 13/2023 Art 16 K2 d operationalization. | 1C v1.5 |
| BR-PARENT-PORTAL-016 | Admin bulk-bump authorization | The `POST /api/v1/admin/parent/consent/bulk-bump` endpoint requires `ADMIN`, `PRINCIPAL`, or `OWNER` role (per `@PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL','OWNER')")`). Tenant scope is enforced via `TenantContext` — the bump always targets the active tenant, never cross-tenant. The endpoint is idempotent — links already at or above the requested version are left untouched (handled by the SQL `WHERE COALESCE((parental_consent ->> 'version')::int, 1) < :newVersion` clause). | 1C v1.5 |

**Source (5-attribute frontmatter applied):**
- **Source:** PDPL Decree 13/2023 Art 16 K2 d (statute citation — re-consent obligation when scope changes); Wave 19 Bucket C closure deferral list (GAP-361 §B + §C); Luật Giáo dục 2019 Đ.83 K2 (uniform parent right-to-information across all facets per Đ.83 K2 "đầy đủ thông tin").
- **Rationale:** Why uniform gate (not selective)? Granting consent for `fees` ≠ granting for `transcript` per Art 16 minimization — but the GATE itself must apply to every facet so an attacker cannot bypass via a less-defended facet. Why version-based re-consent (not time-based)? PDPL Art 16 K2 d triggers on scope change (new facet, new processing purpose), not calendar time — version bump captures the scope-change semantics. Why admin RBAC for bulk-bump (not OWNER-only)? PRINCIPAL is the school role responsible for privacy-policy decisions in K-12 deployments; ADMIN handles the platform-side rollout. Why default required-version `1` (not zero)? Matches V56 migration's seeded version — Wave 19 v1 records start at version 1 + required-version 1, so existing parents are not unnecessarily re-prompted.
- **Reviewer:** @nguyenvankiet (acting Product Owner + acting Legal scout, solo-dev, 2026-05-06). Formal legal counsel review queued — GAP-361 follow-up.
- **Compliance check:** **Compliant** — PDPL Decree 13/2023 Art 16 K2 d (re-consent on scope change, operationalized via version bump); Luật Giáo dục 2019 Đ.83 K2 (uniform "đầy đủ thông tin" parent right satisfied via uniform gate across all 5 facets); Luật Trẻ em 2016 Đ.21 (children's privacy — uniform gate prevents bypass-via-weaker-facet).
- **Review cadence:** Annual + event-driven on Decree 13/2023 implementing-decree publication OR PDPL Art 16 amendment.

### 15.2 Phase 1C v1.5 API surface

```
POST /api/v1/admin/parent/consent/bulk-bump   body: {newVersion, reason, effectiveAt}
                                              ← admin-only; bumps every link in
                                                active tenant whose version is
                                                strictly below newVersion
```

Response shape: `{bumpedCount, newVersion, effectiveAt}`.

The 5 facet endpoints (`/api/v1/parent/children/{id}/{transcript|attendance|fees|conduct|notifications}`) gain two additional 403 paths:
- `PARENT_CONSENT_REQUIRED` — per-field consent missing
- `RECONSENT_REQUIRED` — stored version below required policy version

### 15.3 Configuration keys

| Key | Default | Description |
|-----|---------|-------------|
| `kite.parent.consent.required-version` | `1` | Current required policy version (BR-PARENT-PORTAL-015). Bumped via admin endpoint when privacy policy changes; values < 1 are clamped to 1 by `ConsentServiceImpl`. |

### 15.4 Out of Phase 1C v1.5 scope

| Item | Where | Why deferred |
|------|-------|--------------|
| 3 remaining write actions (conduct-confirm, meeting RSVP, absence-excuse) | **GAP-361 §A** | Depends GAP-338 (meeting entity) + GAP-339 (full complaint workflow) + MinIO encrypted bucket |
| FE re-consent modal UX | **GAP-361 §C continuation** | FE consumer ships separately |
| Settings page UI (`/parent/privacy`) | **GAP-361 §E** | This bucket ships endpoints; FE consumer ships separately |
| i18n EN + zh-CN catalogs | **GAP-361 §D** | International tenant signup feature-flag |

### 15.5 Log

- **2026-05-06** Phase 1C v1.5 shipped — Wave 24 Bucket C (GAP-361 §B + §C): consent gate wired uniformly across 4 remaining facets (transcript / attendance / conduct / notifications) — Fees facet (Wave 19 v1) extended with re-consent version check. ConsentService extended with `getRequiredVersion()` + `bulkBumpVersion(instanceId, newVersion, reason)`. New ParentConsentAdminController (`POST /api/v1/admin/parent/consent/bulk-bump`) gated by `@PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL','OWNER')")`. New native SQL repository method `bulkBumpConsentVersion` using PostgreSQL `jsonb_set` for single-round-trip bulk update. 3 new BR rules (BR-PARENT-PORTAL-014..016) with 5-attribute frontmatter. 2 new properties keys (`PARENT_CONSENT_REQUIRED`, `RECONSENT_REQUIRED`, `PARENT_CONSENT_BULK_BUMP_OK`) in en + vi. New tests: 4 facet `consentMissing_throws403` + 4 facet `consentStale_throwsReconsentRequired` + `ConsentServiceImplTest.bulkBumpVersion_*` + `ParentConsentAdminControllerTest.bulkBump_*`. Bucket ships PARTIAL not DONE — gate + re-consent backbone live; remaining write actions + FE modal + settings UI + i18n tracked in GAP-361 §A/§D/§E. Reviewer: @nguyenvankiet (acting Product Owner + acting Legal scout, solo-dev). Compliance: Compliant — PDPL Decree 13/2023 Art 16 K2 d. Cadence: Annual + event-driven on Decree 13/2023 amendment.
