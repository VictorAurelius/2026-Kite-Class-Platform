# GAP-301: Tenant Data Export Bundle Completeness Verification

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (kiteclass-core retention module) + ops runbook
**Found:** 2026-05-04 (Wave 17 Bucket B — P2 persona review)
**Affects:** P2, P3, P5 — every tenant offboarding flow; PDPL Art 15 compliance (data subject access right)

---

## Problem

P2 owner closes center → expects "Export all data" within 7 days containing: students roster, full attendance log, full grade log, full invoice/payment ledger, commission history. Format: standard (xlsx + PDF combined).

Current state:

- `DataExportService.java` ✅ exists in `module/retention/`
- `DeletionRequest.java` + V38 migration ✅
- **Completeness across all required categories NOT verified end-to-end**
- 7-day SLA NOT verified
- Format compliance (Excel/Google Sheets readable, PDF properly rendered) NOT verified
- PII handling on export (audit log of who accessed) NOT verified

P2 review evidence: AC-EXIT-003 PARTIAL.

## Root Cause

`DataExportService` shipped as scaffold (likely Wave 4 or Wave Legal-BRD). Per `feedback_phase_0_governance_violation.md` and scaffold-as-DONE pattern (GAP-225), need explicit completeness audit.

## Proposed Fix

1. Audit existing `DataExportService` against AC-EXIT-003 checklist:
   - Students roster (with parent contacts)
   - Attendance: full per-session log spanning all classes
   - Grades: per-subject + weighted summary
   - Invoices + payments + reconciliation history
   - Commission history (when GAP-057 lands — placeholder for now)
2. Add missing categories; ensure all data tenant-scoped (no leak across tenants).
3. SLA enforcement: async job queue with 7-day max; status visible to tenant via `/api/exports/{id}/status`.
4. Audit log per export request (who, when, IP, downloaded).
5. PDPL Art 16 minor (≤6mo retention) integration: GAP-184.

## Acceptance Criteria

- [ ] Completeness checklist per AC-EXIT-003 verified end-to-end (integration test seeding tenant + 60 students + 8 classes + 6 months attendance/grades/invoices)
- [ ] Export bundle opens correctly in Excel + Google Sheets + Numbers (manual QA)
- [ ] Export job completes in <7 days (target: <24h for P2 scale; <72h for P5 K-12)
- [ ] Audit log per export
- [ ] Tenant-scoping test: no foreign tenant data leaks into export
- [ ] PII redaction policy documented (full data goes to legal owner; redact when shared with 3rd party)

## Related

- Parent review: `documents/00-brd/persona-reviews/P2-small-center-round-1-2026-05-04.md` AC-EXIT-003
- Cross-link: [GAP-184](GAP-184-data-retention-deletion-policy.md) for retention/deletion side
- Cross-link: [GAP-057](GAP-057-payroll-teacher-commission.md) — commission history will need to be added to export when ready
