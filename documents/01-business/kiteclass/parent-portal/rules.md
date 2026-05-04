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
| BR-PARENT-PORTAL-002 | Identity from Gateway header | Authenticated parent id is read from the `X-User-Reference-Id` header populated by the Gateway from `users.reference_id` when `userType = PARENT`. Missing header → 401 `AUTH_REQUIRED`. Core never touches Gateway identity tables. | 1A |
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
