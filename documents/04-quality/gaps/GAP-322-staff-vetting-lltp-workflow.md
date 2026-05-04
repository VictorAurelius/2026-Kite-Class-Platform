# GAP-322: Bulk Staff Vetting + LLTP Background Check Workflow

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 LEGAL
**Domain:** Backend (KiteClass Core) + Compliance
**Detected:** 2026-05-04 (P5 K-12 persona review Round 1)
**Related Docs:**
- `documents/00-brd/persona-criteria/P5-k12-school.md` AC-ONBOARD-005
- `documents/00-brd/persona-criteria/secondary/teacher-employee-in-P5.md` AC-ONBOARD-002
- `documents/00-brd/persona-criteria/secondary/admin-in-P5.md` AC-ONBOARD-002
- `documents/00-brd/child-protection-policy.md` §6 staff vetting

## Current State (verified 2026-05-04)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Teacher entity | `kiteclass-core/src/main/java/.../module/teacher/entity/Teacher.java` | ✅ shipped (basic) |
| LLTP / CCCD / diploma upload | nowhere | ❌ missing |
| Encrypted storage (MinIO) | `module/storage/` (generic) | 🟡 partial |
| Vetting verify queue | nowhere | ❌ missing |
| 3-year recheck scheduler | nowhere | ❌ missing |
| Block teacher access until verified | nowhere | ❌ missing |

**Grep commands run:**
```bash
grep -rli "lltp\|background.check\|vetting\|safeguard" kiteclass/ kitehub/ documents/
# returns: only mentions in BRD docs / policy file, no code
```

## Problem

Per Luật Trẻ em 2016 Đ.25 + Decree 56/2017, anyone with access to minors must pass background checks. K-12 schools have 50+ teachers + 15+ staff with daily HS access. Currently teachers can be created via admin endpoints with NO LLTP / CCCD / diploma evidence stored or verified — direct legal violation.

## Proposed Fix

1. Bulk vetting workflow:
   - Admin uploads XLSX of staff with metadata
   - Per-staff zip upload (CCCD scan + bằng tốt nghiệp + LLTP số 2 ≤6 months + ảnh 3×4)
   - Encrypted MinIO storage with per-file audit trail
2. New entity `StaffVetting` (teacher_id, status PENDING/PASSED/FAILED, evidence_urls, verified_by, verified_at, expiry_date, recheck_due_date)
3. Admin verify queue (Kite admin → tenant admin chain)
4. RBAC: `Teacher.has_minor_access = false` until `StaffVetting.status = PASSED AND expiry_date > now()`
5. Scheduled recheck alert 90d before expiry (3y default)
6. Audit log retention 7y

## Acceptance Criteria

- [ ] Bulk XLSX + zip upload supported, ≤7d completion for 50 GVs
- [ ] Encrypted storage (MinIO + at-rest encryption)
- [ ] Verify queue admin UI in `(dashboard)/admin/staff-vetting/`
- [ ] Teacher cannot access HS data until vetting PASSED (RBAC enforcement test)
- [ ] Scheduled recheck alert ships
- [ ] 7y audit log retention configured (`RetentionBucket` extension)

## Related

- GAP-186 (child protection — parent gap)
- GAP-184 (data retention)
- GAP-058 (role hierarchy)
- Policy: `documents/00-brd/child-protection-policy.md` §6

## Log

- 2026-05-04 — Filed by Wave 17 Bucket D. State-check: only policy doc + Teacher entity skeleton.
