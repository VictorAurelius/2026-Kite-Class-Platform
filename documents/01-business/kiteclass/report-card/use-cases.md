# Report Card (Bảng điểm VN K-12) — Use Cases

## Scope

Phase 1 MVP single-card use cases only. Phase 2 batch + verification UCs deferred to follow-up gap.

## UC-RC-01: Generate single-student per-semester report card (download)

- **Actor:** GVCN (homeroom teacher), Subject teacher, School admin
- **Precondition:**
  - Tenant has K-12 model enabled (BR-HRC + BR-SG rules in effect)
  - Student exists and is enrolled in a `HomeroomClass`
  - Semester exists and at least 1 `SubjectGrade` row recorded for (student, semester)
- **Steps:**
  1. Actor opens student's grade page → clicks "Xuất bảng điểm"
  2. FE calls `POST /api/v1/reportcards/{studentId}/{semesterId}/pdf/download`
  3. Backend: `ReportCardService.generateReportCard(studentId, semesterId)` → aggregates grades
  4. Backend: `ReportCardRenderer.generate(DocumentRequest)` → renders PDF
  5. Backend: returns 200 with `Content-Type: application/pdf` + `Content-Disposition: attachment; filename*=UTF-8''bang-diem-<student-slug>-<semester>.pdf`
  6. Browser triggers download
- **Postcondition:** PDF saved to actor's device. No server-side persistence in Phase 1 (stateless).
- **Errors:**
  - 401 — no auth
  - 403 — actor not in same tenant as student
  - 404 `REPORT_CARD_STUDENT_NOT_FOUND` — student doesn't exist or soft-deleted
  - 404 `REPORT_CARD_SEMESTER_NOT_FOUND` — semester doesn't exist for tenant
  - 404 `REPORT_CARD_NO_GRADES` — student has no `SubjectGrade` rows for semester
- **FE behavior:** disable "Xuất bảng điểm" button until at least 1 grade exists; on 404_NO_GRADES show toast "Học sinh chưa có điểm nào trong học kỳ này".

## UC-RC-02: Preview single-student per-semester report card (inline)

- **Actor:** Same as UC-RC-01
- **Precondition:** Same as UC-RC-01
- **Steps:**
  1. Actor opens student's grade page → clicks "Xem trước bảng điểm"
  2. FE calls `POST /api/v1/reportcards/{studentId}/{semesterId}/pdf/preview`
  3. Backend: same aggregation + rendering as UC-RC-01
  4. Backend: returns 200 with `Content-Type: application/pdf` + `Content-Disposition: inline; filename*=UTF-8''bang-diem-<student-slug>-<semester>.pdf`
  5. Browser opens PDF in new tab / embedded viewer
- **Errors:** Same as UC-RC-01.
- **FE behavior:** Open in new tab so the existing grade page is preserved.

## UC-RC-03: Render gracefully when conduct (hạnh kiểm) not yet recorded

- **Actor:** Any caller of UC-RC-01 / UC-RC-02
- **Trigger:** GAP-059 not yet shipped → `conduct` data is null
- **Steps:**
  1. Service computes report card data with `conduct = null`
  2. Template renders conduct row text "Chưa cập nhật" instead of empty
  3. PDF still produced successfully (200, not 500)
- **Postcondition:** PDF is valid even without conduct grade; no error to caller.
- **Cross-ref:** When GAP-059 ships, `conduct` field swaps from null → real value with no template change required.

## UC-RC-04: Render gracefully when tenant has no logo

- **Actor:** Any caller of UC-RC-01 / UC-RC-02
- **Trigger:** Tenant's `branding.logoUrl` is null
- **Steps:**
  1. `DocumentBrandingAssembler.enrich()` returns `branding` map with `logoUrl = null`
  2. Template's `th:if="${brand.logoUrl != null}"` skips logo `<img>` tag
  3. Header renders centered school name only — no broken image icon
  4. PDF still produced (200)
- **Postcondition:** Same behavior as `invoice.html` (BR-DOC-016 fallback). Per `output-review-mandate.md` §3 doc-gen row.

## UC-RC-05: Tenant isolation guard

- **Actor:** Authenticated user from tenant A
- **Trigger:** User attempts to fetch report card for student belonging to tenant B (path-parameter manipulation attack)
- **Steps:**
  1. Request lands on `ReportCardController` with `studentId` belonging to tenant B
  2. `ReportCardService` queries `StudentRepository.findByIdAndDeletedFalse(studentId)` — but tenant filter is active via Hibernate `@Filter`
  3. Filter sees student's `instance_id ≠ TenantContext.getCurrentInstanceId()` → returns empty
  4. Service throws 404 `REPORT_CARD_STUDENT_NOT_FOUND` (NOT 403 — leaks no existence info)
- **Postcondition:** Cross-tenant read blocked. Critical per CLAUDE.md multi-tenant rule + `two-stage-code-review.md` Stage 1 BLOCKING.
- **Test required:** `ReportCardControllerIT.cross_tenant_access_returns_404` MUST exist before merge.

## Out-of-scope (Phase 2 follow-up UCs — deferred gap)

- UC-RC-06: Batch generate 30 cards (1-click per class)
- UC-RC-07: QR-code verification flow (scan → verify URL → display authoritative card)
- UC-RC-08: Digital signature application + tenant key management
- UC-RC-09: Live conduct integration (when GAP-059 ships)
- UC-RC-10: Annual / multi-semester transcript variants

## Log

- 2026-04-28 — Initial Phase 1 use cases (GAP-055)
