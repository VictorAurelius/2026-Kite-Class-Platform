# School MIS Integration — Use Cases

**Domain:** KiteClass Core / Integration / MIS Roster Import
**Version:** 1.0 (Phase 1 — GAP-200)
**Updated:** 2026-04-21
**Module:** `kiteclass-core` package `com.kiteclass.core.integration.mis`

---

## UC-MIS-01 — Configure MIS Provider Credentials

**Actor:** Tenant Admin (principal / IT manager)
**Preconditions:**
- Tenant has signed PDPL DPA (BR-MIS-PDPL-001)
- Partnership MoU active with provider (checked by KiteHub billing)
- Feature flag `kiteclass.mis.enabled=true`

**Main flow:**
1. Admin opens `Settings → Integrations → School MIS`
2. Selects provider from dropdown (VNEDU / SMAS / Base.vn / …)
3. Enters API key + tenant-specific identifier (e.g. VNEDU school code)
4. Clicks **Test Connection** → FE calls `POST /api/v1/mis/credentials/test`
5. BE invokes `MisRosterSource.ping()` through the matching adapter
6. On success, BE persists credentials to `mis_credentials` (encrypted per BR-MIS-SEC-001)
7. FE shows green badge "Connected"

**Errors:**
- `400 MIS_INVALID_CREDENTIALS` — test call returned auth failure
- `503 MIS_DISABLED` — feature flag off (BR-MIS-005)
- `403 MIS_DPA_REQUIRED` — DPA not on file (BR-MIS-PDPL-001)

**FE behavior:**
- Masked input for API key (show/hide toggle)
- Clear success/failure state with specific error code displayed
- Credentials never rendered after save (write-once UX)

---

## UC-MIS-02 — Dry-Run Roster Import

**Actor:** Tenant Admin
**Preconditions:** UC-MIS-01 complete

**Main flow:**
1. Admin opens `Settings → Integrations → School MIS → Import`
2. Clicks **Preview Import**
3. FE calls `POST /api/v1/mis/import?dryRun=true`
4. BE calls `VneduAdapter.fetchRoster(academicYear)` → returns `RosterImport` DTO
5. BE returns summary: `{students: 1242, parents: 1180, teachers: 87, classes: 42, enrollments: 1242}`
   plus first 10 records per entity (BR-MIS-IMPORT-006)
6. FE renders preview table; admin reviews for obvious errors

**Errors:**
- `503 MIS_ADAPTER_UNREACHABLE` — circuit breaker open, MIS API down
- `504 MIS_IMPORT_TIMEOUT` — fetch exceeded `timeout-seconds`
- `429 MIS_REIMPORT_COOLDOWN` — cooldown active (BR-MIS-IMPORT-005)

**FE behavior:**
- Loading spinner with estimated ETA (1-5 min typical)
- Results cached 5 min so admin can tab around without re-fetching
- Red banner if any record has validation warnings (duplicate emails, etc.)

---

## UC-MIS-03 — Commit Roster Import

**Actor:** Tenant Admin
**Preconditions:** UC-MIS-02 dry-run reviewed and acceptable

**Main flow:**
1. Admin clicks **Commit Import** on dry-run preview
2. FE calls `POST /api/v1/mis/import` (no `dryRun` param)
3. BE creates `MisImportJob` row with status `PENDING` → enqueues async job to RabbitMQ (`mis.import.{tenant}` queue, per ADR-014)
4. BE returns `202 Accepted` + `{jobId, statusUrl}`
5. FE polls `GET /api/v1/mis/import/{jobId}` every 5 sec showing progress
6. Async worker:
   - Calls `VneduAdapter.fetchRoster(academicYear)`
   - For each entity, upserts into KiteClass tables (students/parents/teachers/classes/enrollments)
   - Applies conflict strategy per BR-MIS-CONFLICT-001..005
   - Writes result counts + conflicts to `MisImportJob` row
   - Transitions status `RUNNING` → `COMPLETED` | `FAILED` | `PARTIAL`
7. FE shows summary: `{imported: 1240, updated: 2, conflicts: 3, rejected: 5}` + link to conflict queue (if PARTIAL)

**Errors / outcomes:**
- `FAILED` — adapter error before any records persisted. Nothing written.
- `PARTIAL` — some records rejected (duplicate email, malformed data, conflicts requiring `MANUAL_REVIEW`). Successes committed.
- `COMPLETED` — all records applied cleanly.

**FE behavior:**
- Job progress bar with live record count
- Navigate-away-safe: admin can close tab; resume on return via jobId in URL
- Detailed log viewer for debugging rejected records

---

## UC-MIS-04 — Resolve Import Conflict (MANUAL_REVIEW mode)

**Actor:** Tenant Admin
**Preconditions:** Tenant `conflict-strategy = MANUAL_REVIEW`; `MisImportJob` completed with status `PARTIAL`

**Main flow:**
1. Admin opens `Settings → Integrations → School MIS → Conflicts`
2. FE calls `GET /api/v1/mis/conflicts?status=PENDING`
3. BE returns list of `MisConflict` rows with side-by-side MIS-value / KiteClass-value
4. Admin chooses per row: **Use MIS** / **Use KiteClass** / **Skip**
5. FE calls `POST /api/v1/mis/conflicts/{id}/resolve` with decision
6. BE applies decision, updates target entity, marks conflict `RESOLVED`

**Errors:**
- `404 MIS_CONFLICT_NOT_FOUND` — id invalid or already resolved
- `403 MIS_CONFLICT_READONLY` — status != `PENDING`

**FE behavior:**
- Diff view highlighting field-level changes
- Bulk actions: "Use MIS for all remaining"
- Audit entry created per resolution (BR-MIS-IMPORT-007)

**Phase gating:** Conflict UI is Phase 2 — Phase 1 ships data model + API only.

---

## UC-MIS-05 — Disable MIS Integration

**Actor:** Tenant Admin
**Preconditions:** Credentials exist; no active import job

**Main flow:**
1. Admin opens `Settings → Integrations → School MIS`
2. Clicks **Disconnect**
3. Confirmation modal: "This will stop future imports. Existing data stays."
4. FE calls `DELETE /api/v1/mis/credentials`
5. BE soft-deletes credentials (audit trail preserved per BR-MIS-IMPORT-007)
6. FE shows empty state

**Errors:**
- `409 MIS_IMPORT_IN_PROGRESS` — active `RUNNING` job exists; must cancel first

**FE behavior:**
- Clear copy explains imported records will NOT be deleted
- Separate "Purge all MIS-sourced data" action (destructive, double-confirm)

---

## Notes on Phase 1 Scope

UC-MIS-01 … UC-MIS-05 are **documented** in this Phase 1 deliverable to anchor
the API contract and catch design flaws early. Implementation of the service
layer + controllers is **Phase 2**. Only the `MisRosterSource` interface,
`RosterImport` DTO, `VneduAdapter` skeleton, and its unit test land in the
current PR.

---

## References

- Rules: `rules.md`
- API contract: `api-contract.md`
- ADR: `documents/02-architecture/adr/ADR-017-mis-sync-strategy.md`
- Gap: `documents/04-quality/gaps/GAP-200-school-mis-integration.md`
