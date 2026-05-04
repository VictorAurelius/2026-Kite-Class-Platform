# Payroll — API Contract

**Domain:** KiteClass Core / Finance
**Version:** 0.1 (Phase 1)
**Updated:** 2026-05-04
**Source:** GAP-057 Phase 1 (Wave 18a Bucket C)

---

## Phase 1 endpoints (read-only)

All endpoints require `@PreAuthorize("hasRole('ADMIN')")` and tenant context via `X-Tenant-Id` header.

### GET `/api/v1/admin/payroll/configs`

**Description:** Paged list of teacher payroll configurations.

**Query params:**

| Name | Type | Required | Default | Notes |
|------|------|:-------:|---------|-------|
| `page` | int | no | `0` | Page index, 0-based |
| `size` | int | no | `20` | Page size |
| `sort` | string | no | `id,asc` | `field,direction` (e.g. `id,desc`) |

**Response (200):**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "teacherId": 42,
        "type": "HOURLY",
        "hourlyRate": 200000.00,
        "baseSalary": null,
        "commissionPercent": null,
        "gvcnAllowance": null
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

**Errors:**

| Status | Error code | When |
|--------|-----------|------|
| 401 | `UNAUTHORIZED` | No valid auth token |
| 403 | `FORBIDDEN` | Not ADMIN role |

---

### GET `/api/v1/admin/payroll/periods`

**Description:** Paged list of payroll periods, filterable by teacher and date range.

**Query params:**

| Name | Type | Required | Default | Notes |
|------|------|:-------:|---------|-------|
| `teacherId` | long | no | — | Filter by teacher FK |
| `startDate` | ISO date | no | — | Inclusive lower bound on `period.startDate` |
| `endDate` | ISO date | no | — | Inclusive upper bound on `period.endDate` |
| `page` | int | no | `0` | Page index |
| `size` | int | no | `20` | Page size |
| `sort` | string | no | `startDate,desc` | `field,direction` |

**Response (200):**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 100,
        "teacherId": 42,
        "startDate": "2026-05-01",
        "endDate": "2026-05-31",
        "hoursWorked": 40.00,
        "grossAmount": 8000000.00,
        "deductions": 0.00,
        "netAmount": 8000000.00,
        "status": "DRAFT"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

**Errors:**

| Status | Error code | When |
|--------|-----------|------|
| 400 | `BAD_REQUEST` | Malformed `startDate`/`endDate` (not ISO yyyy-MM-dd) |
| 401 | `UNAUTHORIZED` | No valid auth token |
| 403 | `FORBIDDEN` | Not ADMIN role |

---

### GET `/api/v1/admin/payroll/periods/{id}`

**Description:** Single payroll period detail.

**Path params:**

| Name | Type | Notes |
|------|------|-------|
| `id` | long | Period PK |

**Response (200):**

```json
{
  "success": true,
  "data": {
    "id": 100,
    "teacherId": 42,
    "startDate": "2026-05-01",
    "endDate": "2026-05-31",
    "hoursWorked": 40.00,
    "grossAmount": 8000000.00,
    "deductions": 0.00,
    "netAmount": 8000000.00,
    "status": "DRAFT"
  }
}
```

**Errors:**

| Status | Error code | When |
|--------|-----------|------|
| 401 | `UNAUTHORIZED` | No valid auth token |
| 403 | `FORBIDDEN` | Not ADMIN role |
| 404 | `PAYROLL_PERIOD_NOT_FOUND` | Period does not exist or belongs to a different tenant |

---

## Internal service contract (called by tests + future Phase 2)

The Phase 1 calculation engine is exposed as a Java interface — not a REST endpoint — to keep the run/approve/pay workflow consistent for Phase 2. Phase 2 will wrap this into a `POST /runs` endpoint.

### `PayrollService.calculate(Long teacherId, LocalDate startDate, LocalDate endDate)`

**Returns:** `PayrollPeriod` (persisted with status=DRAFT).

**Throws:**

| Exception | Error code | When |
|-----------|-----------|------|
| `ValidationException` | `PAYROLL_PERIOD_DATES_REQUIRED` | startDate or endDate is null |
| `ValidationException` | `PAYROLL_PERIOD_END_BEFORE_START` | endDate < startDate |
| `EntityNotFoundException` | `PAYROLL_CONFIG_NOT_FOUND` | No PayrollConfig for teacher |
| `ValidationException` | `PAYROLL_HOURLY_RATE_REQUIRED` | HOURLY config has null/non-positive rate |
| `UnsupportedOperationException` | (no code; message names GAP-057b) | Type is SALARY / COMMISSION / HYBRID |

---

## Phase 2 endpoints (NOT in Phase 1 — listed for forward compatibility)

> Tracked in **GAP-057b**. These contracts are illustrative; final shape will follow Phase 2 design.

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/admin/payroll/runs` | Trigger payroll run for date range + optional teacher filter |
| POST | `/api/v1/admin/payroll/periods/{id}/approve` | DRAFT → APPROVED |
| POST | `/api/v1/admin/payroll/periods/{id}/pay` | APPROVED → PAID |
| GET | `/api/v1/payroll/periods/{id}/payslip.pdf` | Download PDF (depends on GAP-047) |
| GET | `/api/v1/admin/payroll/runs/{runId}/bank-export` | Bank batch transfer file |
| POST | `/api/v1/admin/payroll/configs` | Create teacher payroll config |
| PUT | `/api/v1/admin/payroll/configs/{id}` | Update teacher payroll config |
| DELETE | `/api/v1/admin/payroll/configs/{id}` | Soft-delete config |

---

## OpenAPI generation

Spring controllers are annotated with `@Operation` + `@Parameter` (springdoc). The Phase 1 OpenAPI fragment is auto-generated when the kiteclass-core service starts and exposed at `/v3/api-docs`. Phase 1 endpoints appear under tag **"Admin / Payroll"**.

---

## Log
- **2026-05-04 (v0.1)** — Initial Phase 1 ship: 3 read-only endpoints + internal `calculate(...)` contract. Phase 2 endpoints stubbed for forward compatibility.
