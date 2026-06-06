# Student Portal — Business Rules

**Domain:** kc-student frontend (production routes `(dashboard)/student/**`) + kiteclass-core read APIs
**Source of truth:** `documents/01-business/kiteclass/student-portal/`
**Created:** 2026-05-10 (Wave 51 Bucket B — GAP-269b foundation)
**Status:** Phase 1 v1 — endpoint contracts published; full data joins follow when FE consumer PR lands

---

## 1. Rules

### BR-STUDENT-PORTAL-001: Authorization scope = own data only

- **Value:** Mọi endpoint trong `/api/v1/students/me/**` PHẢI scope reads xuống đúng `studentReferenceId` từ `X-User-Reference-Id` header. **Option B (Wave auth-1):** Gateway re-inject header từ `referenceId` claim của KC-native token (= `auth_credentials.entity_id` với entity_type=STUDENT), sau khi strip client value (anti-spoof). **Option A (superseded):** populate từ `users.reference_id` khi `userType = STUDENT`. Xem `tenant-auth/rules.md` BR-AUTH-HDR-001/002.
- **Rationale:** Student persona không có quyền xem data của student khác. Kế thừa pattern từ `ParentTranscriptController` (Wave 18b1 GAP-321 Phase 1A) — header-based identity + service-side scope guard.
- **Source:** `informed gut` + Parent Portal precedent (Wave 18b1).
- **Reviewer:** @nguyenvankiet (acting Product Owner + Compliance, solo-dev, 2026-05-10). Legal counsel review queued via GAP-156.
- **Compliance check:** **Compliant** — PDPL 2023 Art 23 (data minimization: chỉ trả về data của chính chủ thể), Luật An ninh mạng 2018 (data localization N/A — VN region).
- **Review cadence:** Annual. **Next review:** 2027-05-10. Event triggers: PDPL implementing decree publication, security incident.

### BR-STUDENT-PORTAL-002: Auth header missing → 401

- **Value:** Request thiếu `X-User-Reference-Id` → HTTP 401 + `BusinessException("AUTH_REQUIRED")`.
- **Rationale:** Fail-closed — không có identity = không có data. Gateway responsibility là inject header; absence = broken integration / direct call bypass gateway.
- **Source:** Pattern shared với Parent Portal controllers.
- **Reviewer:** @nguyenvankiet (acting Tech Lead, solo-dev, 2026-05-10).
- **Compliance check:** N/A — security baseline.
- **Review cadence:** Annual. **Next review:** 2027-05-10.

### BR-STUDENT-PORTAL-003: Notifications feed pagination

- **Value:** Cursor-paginated; `limit` clamped vào `[1..100]`; default 20; opaque cursor.
- **Rationale:** Cursor-pagination là pattern đúng cho timeline-shaped data (notifications); offset-pagination không scale với rapidly-growing feeds. Cap 100 = giới hạn payload size; default 20 ≈ first viewport trên mobile.
- **Source:** `informed gut` + standard timeline-pagination practice.
- **Reviewer:** @nguyenvankiet (acting Product Owner + Tech Lead, solo-dev, 2026-05-10).
- **Compliance check:** N/A — implementation detail.
- **Review cadence:** Quarterly. **Next review:** 2026-08-10. Event triggers: complaint về limit quá nhỏ; FE perf finding.

### BR-STUDENT-PORTAL-004: Subject access = enrollment-scoped

- **Value:** `GET /api/v1/students/me/grades/{subjectId}` chỉ trả data nếu student được enrolled vào subject đó; otherwise 404 `STUDENT_PORTAL_SUBJECT_NOT_FOUND`.
- **Rationale:** Students không thể "fish" cho subjectId khác để probe schema. Khớp với BR-STUDENT-PORTAL-001 scoping.
- **Source:** Standard authorization scoping; Parent Portal Wave 18b1 precedent.
- **Reviewer:** @nguyenvankiet (acting Product Owner + Compliance, solo-dev, 2026-05-10).
- **Compliance check:** **Compliant** — PDPL 2023 (data minimization).
- **Review cadence:** Annual. **Next review:** 2027-05-10.
- **Implementation status:** Phase 1 v1 — service returns empty body; full enrollment join + 404 handling lands cùng FE consumer PR.

### BR-STUDENT-PORTAL-005: Read-only surface

- **Value:** Tất cả endpoints `/api/v1/students/me/**` là `GET` (read-only). Settlement của payments + submission của assignments + read-receipt của notifications đi qua surface khác (owner payment flow / assignment submission API / dedicated notification mark-read endpoint future scope).
- **Rationale:** Separation of concerns — student portal = read; mutations = dedicated endpoints với additional validation surface (e.g. payment requires anti-fraud, submission requires content moderation).
- **Source:** Architectural choice; pattern matches Parent Portal (Wave 18b1 cũng read-only).
- **Reviewer:** @nguyenvankiet (acting Tech Lead, solo-dev, 2026-05-10).
- **Compliance check:** N/A — architectural rule.
- **Review cadence:** Annual. **Next review:** 2027-05-10.

---

## 2. Permissions

| Action | Student | Teacher | Admin | Parent |
|--------|:-------:|:-------:|:-----:|:------:|
| GET `/me/today` | ✅ own | ❌ | ❌ | ❌ |
| GET `/me/grades` | ✅ own | ❌ | ❌ | ❌ |
| GET `/me/grades/{subjectId}` | ✅ own + enrolled | ❌ | ❌ | ❌ |
| GET `/me/payments` | ✅ own invoices | ❌ | ❌ | ❌ |
| GET `/me/notifications` | ✅ own feed | ❌ | ❌ | ❌ |

Note: Parent persona uses `/api/v1/parent/children/{childId}/**` surface (Wave 18b1 GAP-321 Phase 1A) — separate scope guard chain.

---

## 3. Config

| Key | Default | Description |
|-----|---------|-------------|
| `student-portal.notifications.default-limit` | `20` | Default page size khi `limit` không được supply |
| `student-portal.notifications.max-limit` | `100` | Hard cap cho `limit` query param |

---

## 4. Five-attribute review per `business-logic-review.md`

Per-rule attributes đã được populate inline cho từng BR (§1) per `business-logic-review.md` §2 standard.

- **Source:** Combination of Parent Portal Wave 18b1 precedent (`ParentTranscriptController` / `ParentNotificationsFacetController`) + standard REST authorization patterns + PDPL data minimization principle.
- **Rationale:** Per-rule rationale documented inline.
- **Reviewer:** @nguyenvankiet (acting Product Owner + Tech Lead + Compliance scout, solo-dev, 2026-05-10). Formal stakeholder + legal counsel sign-off queued via GAP-156.
- **Compliance check:** **Considered/Compliant** — PDPL 2023 Art 23 data minimization (BR-001/BR-004), Luật An ninh mạng 2018 N/A (VN region).
- **Review cadence:** Mixed (Annual cho stable architectural rules; Quarterly cho operational caps).

---

## 5. Log

- **2026-05-10** (initial): File created Wave 51 Bucket B (GAP-269b). 5 BR documented covering authorization scope (own data only), 401-on-missing-header, cursor pagination cap, subject-access enrollment-scoping, read-only surface invariant. All BRs reflect Phase 1 v1 contract — full join logic deferred to FE consumer PR.
