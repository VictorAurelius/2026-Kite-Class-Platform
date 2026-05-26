# GAP-232: API Contract Drift — attendance domain

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (Business-Logic — wrong API contract = wrong product per `meta-gap-priority.md` §3)
**Domain:** Backend / API contract documentation
**Found:** 2026-04-26 (post-wave-7 API contract audit, score 42/100 F)
**Affects:** Daily-use feature — teachers (roll-call), homeroom managers (late-mark), parents (attendance reports), gamification engine (presence-based points), frontend attendance screens

## Problem

Post-wave-7 API audit flagged **attendance** as second-highest-impact domain (daily-use, parent-visible). Verified counts via grep on `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/attendance/`:

| Controller | Method-level mappings | Class base path |
|---|---:|---|
| `AttendanceController` | 9 | `/api/v1/attendance` |
| **Total** | **9** | — |

(Audit count of 9 endpoints CONFIRMED.)

Sample endpoints verified in code:
- `POST /api/v1/attendance` (`AttendanceController:62`) — single mark
- `POST /api/v1/attendance/classes/{classId}/sessions/{sessionId}/attendance` (`AttendanceController:84`) — bulk mark
- `GET /api/v1/attendance/stats/student/{studentId}` (`AttendanceController:169`)
- `PATCH /api/v1/attendance/{id}` (`AttendanceController:206`) — correct mistake
- `DELETE /api/v1/attendance/{id}` (`AttendanceController:227`)

## Current State (verified 2026-04-26)

`documents/01-business/kiteclass/attendance/api-contract.md` — **EXISTS**, 5.0K, all 9 endpoints have `### {VERB} /api/v1/attendance/...` headings. Quality of stubs is moderately high for this domain (better than payment-invoice):

- ✅ Each endpoint has 1-line summary
- ✅ Request DTO names referenced (`CreateAttendanceRequest`, `BulkAttendanceRequest`, `UpdateAttendanceStatusRequest`)
- ✅ Response DTO field tables present (`AttendanceResponse`, `AttendanceStatsResponse`)
- ✅ Status enum listed (`PRESENT`, `ABSENT`, `LATE`, `EXCUSED`, `MAKEUP`)
- ❌ Auth header requirements not specified per endpoint (Bearer JWT, X-Tenant-Id, role gating)
- ❌ Error codes only partial (some endpoints have `404 not found`, `409 already marked`; others missing entirely)
- ❌ UC references (`UC-ATT-XX`) absent — needs cross-link from use-cases.md (6.6K)
- ❌ Pagination contract (page/size/sort defaults + maxima) underspecified
- ❌ Request DTO field-level validation (required flags, range, enum values) inconsistent

(Audit reported "0 documented" — INCORRECT. State-check shows attendance is the most complete of the three flagged domains; gap scope is auth + error matrix + UC linkage, not greenfield documentation.)

`rules.md` (4.6K) — likely defines roll-call window, late threshold, MAKEUP eligibility; api-contract.md should cite specific business rule IDs for status transitions and validation.

## Root Cause

Wave 4–6 attendance feature ramp-up shipped controller + DTO + initial doc together (good practice), but audit + auth conventions were not yet standardized across kiteclass-core when this domain landed. Subsequent waves never circled back to add the auth/error matrix as conventions hardened. Audit-gate hook does not currently validate auth-block presence in api-contract.md.

## Proposed Fix

1. For each of the 9 endpoints, add to `documents/01-business/kiteclass/attendance/api-contract.md`:
   - **Auth block**: `Bearer JWT (role: TEACHER|HOMEROOM|ADMIN|STUDENT-readonly|PARENT-readonly)`, `X-Tenant-Id: {slug}`
   - **Error codes**: `400 VALIDATION_ERROR`, `401 UNAUTHENTICATED`, `403 FORBIDDEN_ROLE`, `404 NOT_FOUND_*`, `409 ALREADY_MARKED|SESSION_NOT_OPEN`, `422 OUT_OF_WINDOW`, `500 INTERNAL`
   - **UC reference**: `UC-ATT-XX` per endpoint (define in use-cases.md if missing)
   - **Validation rules**: required flags, enum allowed values, max-length on `notes`
2. Document pagination defaults: `page=0, size=20, max=100, sort=markedDate,DESC`
3. Document gamification side-effect: which statuses award points (`pointsAwarded` field) — link to `rules.md` config key
4. Document idempotency key for bulk-mark (replay-safety)
5. Run `/api-contract-audit` skill — target ≥90/100 for this domain post-fix

## Acceptance Criteria

- [ ] All 9 endpoints have explicit auth block (header + role)
- [ ] All 9 endpoints have explicit error code matrix (≥3 standard + domain-specific)
- [ ] All 9 endpoints reference a UC ID from `use-cases.md`
- [ ] Request DTO validation rules listed field-by-field, matching real Java validation annotations
- [ ] Pagination contract documented with defaults + maxima
- [ ] Gamification points awarded per status documented + linked to `rules.md`
- [ ] Bulk-mark idempotency contract documented
- [ ] `/api-contract-audit` re-run scores ≥90 for this domain

## Related

- Audit: `documents/04-quality/audits/api/api-contract-audit-2026-04-26-post-wave7.md`
- Rule: `.claude/rules/audit-to-gap-pipeline.md` Step 3 + Step 2.5 state-check
- Rule: `.claude/rules/output-review-mandate.md` §3 matrix (API contracts row)
- Living Documents: `CLAUDE.md` §"CRITICAL: Living Documents"
- Sibling gaps: GAP-231 (payment-invoice), GAP-233 (student-enrollment)

## Log

- 2026-04-26 — Filed during post-wave-7 audit retrospective. Source: API contract audit. Endpoint count of 9 CONFIRMED via grep. State-check found api-contract.md exists with all 9 endpoint headings + DTO tables — drift is depth (auth + errors + UC linkage), not breadth. Gap scope narrowed accordingly per `feedback_gap_state_check_required.md`.
