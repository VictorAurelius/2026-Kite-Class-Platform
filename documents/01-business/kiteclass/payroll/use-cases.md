# Payroll — Use Cases

**Domain:** KiteClass Core / Finance
**Version:** 0.1 (Phase 1)
**Updated:** 2026-05-04
**Source:** GAP-057 Phase 1 (Wave 18a Bucket C)

---

## Phase 1 use cases (read-only)

> Phase 1 ships read-only views + the calculation engine (callable by other services or tests). **Run / approve / pay** workflows are Phase 2 (GAP-057b).

---

### UC-PAYROLL-VIEW-CONFIGS

**Actor:** Admin (role `ADMIN`)
**Goal:** List all teacher payroll configurations to verify rates / types / coverage.

**Preconditions:**
- Authenticated as ADMIN
- Tenant context is set (X-Tenant-Id header)

**Steps:**
1. Admin navigates to `/admin/payroll` page
2. Frontend calls `GET /api/v1/admin/payroll/configs?page=0&size=20`
3. Backend returns paged `PayrollConfigResponse` (BR-PAYROLL-003 tenant filter applied)
4. UI shows table: teacher_id / type / hourly_rate / base_salary placeholders / etc.

**Postconditions:**
- No data mutation
- Audit log: read-access logged via standard request log (Phase 2 expands payroll-specific audit)

**Errors:**
- 401 — not authenticated
- 403 — not ADMIN

**FE behavior:**
- Empty state: "No payroll configs yet — Phase 2 (GAP-057b) will ship the create-config UI"
- Phase 1 shows placeholder for non-HOURLY rows (baseSalary / commissionPercent visible but greyed with "Phase 2" badge)

---

### UC-PAYROLL-VIEW-PERIODS

**Actor:** Admin (role `ADMIN`)
**Goal:** Browse payroll periods filtered by teacher and/or date range.

**Preconditions:**
- Authenticated as ADMIN
- Tenant context set

**Steps:**
1. Admin opens `/admin/payroll` and uses filter controls (teacher dropdown, start/end date pickers)
2. Frontend calls `GET /api/v1/admin/payroll/periods?teacherId={id}&startDate=2026-05-01&endDate=2026-05-31&page=0&size=20`
3. Backend applies JPQL filters in `PayrollPeriodRepository.findByFilters` (BR-PAYROLL-003 tenant filter)
4. UI shows table: teacher / period / hours / gross / deductions / net / status

**Postconditions:**
- No data mutation

**Errors:**
- 401 — not authenticated
- 403 — not ADMIN
- 400 — malformed date params (handled by Spring's `@DateTimeFormat`)

**FE behavior:**
- Status column shows `DRAFT` only (Phase 1 produces only DRAFT)
- Sort by `startDate,desc` default
- Empty state: "No payroll periods for filters — periods are created by `PayrollService.calculate(...)` (Phase 2 will ship the run-payroll button)"

---

### UC-PAYROLL-VIEW-PERIOD-DETAIL

**Actor:** Admin (role `ADMIN`)
**Goal:** Drill into a single payroll period to verify the calculation breakdown.

**Preconditions:**
- Authenticated as ADMIN
- Period exists for current tenant

**Steps:**
1. Admin clicks a row in the period list (or directly opens `/admin/payroll/periods/{id}`)
2. Frontend calls `GET /api/v1/admin/payroll/periods/{id}`
3. Backend returns `PayrollPeriodResponse`
4. UI shows: period dates / hours_worked / gross / deductions / net / status

**Postconditions:**
- No data mutation

**Errors:**
- 401 — not authenticated
- 403 — not ADMIN
- 404 — period not found OR belongs to a different tenant (filtered out)

**FE behavior:**
- Phase 1: shows "Phase 2 will ship payslip PDF download here" placeholder
- Phase 1: deductions row shows "0 đ (Phase 1 — TNCN/BHXH/BHYT in Phase 2 GAP-057b)"

---

## Phase 2 use cases (deferred to GAP-057b)

> The following use cases are documented here to make the Phase 2 scope explicit. **Not implemented in Phase 1.** Listed so Phase 2 starts from a known scope without re-discovery.

### UC-PAYROLL-RUN (Phase 2 — GAP-057b)

**Actor:** Admin
**Goal:** Trigger monthly payroll run for all (or filtered) teachers.

**Phase 2 design notes:**
- POST `/api/v1/admin/payroll/runs` with `{ startDate, endDate, teacherIds?: number[] }`
- Calls `PayrollService.calculate(...)` per teacher (HOURLY in Phase 1; SALARY / COMMISSION / HYBRID added Phase 2)
- Returns batch result: `{ created: count, errors: [{teacherId, reason}] }`

### UC-PAYROLL-APPROVE (Phase 2 — GAP-057b)

**Actor:** Admin
**Goal:** Approve a DRAFT period after reviewing the calculation.

**Phase 2 design notes:**
- POST `/api/v1/admin/payroll/periods/{id}/approve`
- Transition: `DRAFT → APPROVED`; locked from re-calculation
- Outbox event `payroll.period.approved`
- Audit log: who approved, when, optional comment

### UC-PAYROLL-PAY (Phase 2 — GAP-057b)

**Actor:** Admin
**Goal:** Mark approved periods as PAID after bank transfer is sent.

**Phase 2 design notes:**
- POST `/api/v1/admin/payroll/periods/{id}/pay` (or batch endpoint)
- Transition: `APPROVED → PAID`
- Optional: bank export file generation (CSV/XML per VN bank batch format)

### UC-PAYROLL-PAYSLIP-PDF (Phase 2 — GAP-057b)

**Actor:** Admin or Teacher
**Goal:** Download per-period payslip PDF.

**Phase 2 design notes:**
- GET `/api/v1/payroll/periods/{id}/payslip.pdf`
- Depends on **GAP-047** (PDF generation infrastructure)
- Per `documents/05-guides/payslip-template.md` (to be authored Phase 2)

### UC-PAYROLL-BANK-EXPORT (Phase 2 — GAP-057b)

**Actor:** Admin
**Goal:** Export approved periods as a bank batch transfer file.

**Phase 2 design notes:**
- GET `/api/v1/admin/payroll/runs/{runId}/bank-export?format=BIDV|VCB|TCB`
- File format per Vietnamese bank specs (CSV/XML)

---

## Cross-cutting

### Tenant isolation
All Phase 1 endpoints enforce tenant via BaseEntity `tenantFilter` (BR-PAYROLL-003). Period/config from other tenants are invisible.

### Empty state guidance
Phase 1 ships read-only with no UI to seed configs. Phase 1 acceptance allows the admin list to be empty in dev/staging — content arrives in Phase 2 when admin can create configs + run payroll. For Phase 1 testing, configs are seeded via direct DB insert or test fixtures.

---

## Log
- **2026-05-04 (v0.1)** — Initial Phase 1 ship (UC-VIEW-CONFIGS / UC-VIEW-PERIODS / UC-VIEW-PERIOD-DETAIL). Phase 2 use cases stubbed for traceability.
