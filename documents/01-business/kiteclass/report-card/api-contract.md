# Report Card (Bảng điểm VN K-12) — API Contract

> Source: `ReportCardController` (Phase 1 — single-card endpoints only)
> Base path: `/api/v1/reportcards`
> Reuses: `DocumentGenerationService` facade (Wave 5 GAP-047), `BrandingService` (composite branding package)

## Phase 1 endpoints

### POST `/api/v1/reportcards/{studentId}/{semesterId}/pdf/preview`

Render single-student per-semester report card and return inline PDF (browser opens in new tab).

- **Path params:**
  - `studentId` (Long, required) — must belong to current tenant
  - `semesterId` (Long, required) — must belong to current tenant
- **Request body:** none (data aggregated server-side from existing `SubjectGrade` rows)
- **Response:** `200 OK`
  - `Content-Type: application/pdf`
  - `Content-Disposition: inline; filename*=UTF-8''bang-diem-<student-slug>-<semester>.pdf`
  - Body: PDF bytes
- **Errors:**

| Status | Error code | Trigger |
|:------:|------------|---------|
| 401 | `AUTH_REQUIRED` | Missing / invalid auth token |
| 403 | `TENANT_MISMATCH` | (defensive — should not normally fire because tenant filter returns 404) |
| 404 | `REPORT_CARD_STUDENT_NOT_FOUND` | studentId not found in current tenant (incl. soft-deleted, incl. cross-tenant attack) |
| 404 | `REPORT_CARD_SEMESTER_NOT_FOUND` | semesterId not found in current tenant |
| 404 | `REPORT_CARD_NO_GRADES` | Student exists but has zero `SubjectGrade` rows for semester |
| 500 | `REPORT_CARD_RENDER_FAILED` | Renderer exception (DejaVuSans missing, template parse error, etc.) — surfaces in logs |

### POST `/api/v1/reportcards/{studentId}/{semesterId}/pdf/download`

Same as `/preview` but with `Content-Disposition: attachment` so the browser triggers a save dialog.

- **Path params:** same as `/preview`
- **Response:** `200 OK`
  - `Content-Type: application/pdf`
  - `Content-Disposition: attachment; filename*=UTF-8''bang-diem-<student-slug>-<semester>.pdf`
  - Body: PDF bytes
- **Errors:** same set as `/preview`

## Filename convention

```
bang-diem-{slug(student.fullName)}-{semester.label}.pdf
```

Where:
- `slug(name)` = lowercase, accent-stripped, hyphen-separated (e.g., "Nguyễn Văn A" → "nguyen-van-a")
- `semester.label` = compact form like `2025-2026-hk1`

RFC-5987 UTF-8 encoding ensures VN diacritics in filenames survive (BR-DOC-014 from doc-gen rules).

## Required headers (request)

| Header | Required | Purpose |
|--------|:--------:|---------|
| `Authorization: Bearer <jwt>` | yes | Auth — establishes `TenantContext` via filter |
| `X-Tenant-ID` | derived | Set by gateway from JWT claim, not by client |

## Required headers (response)

| Header | Set by | Notes |
|--------|--------|-------|
| `Content-Type` | renderer | always `application/pdf` |
| `Content-Disposition` | controller | `inline` or `attachment` per endpoint |
| `Cache-Control: no-store` | controller | report cards may contain student PII; do not cache |

## Out-of-scope (Phase 2)

- `POST /api/v1/reportcards/class/{classId}/{semesterId}/batch` — batch 30 cards as ZIP
- `GET /api/v1/reportcards/verify?token={qr-token}` — QR verification redirect
- `POST /api/v1/reportcards/.../with-signature` — apply tenant digital signature

## Verification chain

```
BR-RC-AGG-002 (tenant isolation)
  → UC-RC-05 (cross-tenant guard)
    → ReportCardController.preview / .download
      → @PreAuthorize + TenantContext check
        → ReportCardControllerIT.cross_tenant_access_returns_404
```

```
BR-RC-AGG-005 (empty grade set → 404)
  → UC-RC-01 step "Errors: 404 REPORT_CARD_NO_GRADES"
    → ReportCardServiceImpl.generateReportCard throws ReportCardNoGradesException
      → ReportCardServiceTest.no_grades_throws_404
```

## Log

- 2026-04-28 — Initial Phase 1 contract (GAP-055)
