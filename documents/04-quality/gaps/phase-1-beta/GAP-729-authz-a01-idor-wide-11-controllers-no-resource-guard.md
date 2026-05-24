# GAP-729: 11/19 controllers no per-resource authz guard — A01 OWASP IDOR wide

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (kiteclass-core)
**Detected:** 2026-05-24 (Wave beta-readiness-1 Bucket D audit, PR #1763)
**Affects:** 11/19 controllers — potential IDOR (Insecure Direct Object Reference) per A01 OWASP Top 10 2021

## Problem

Wave beta-readiness-1 Bucket D audit scanned 19 controllers trong kiteclass-core; 11/19 chỉ có tenant-level Hibernate filter (`tenant_id = ?`) NHƯNG KHÔNG có per-resource `@PreAuthorize` guard.

Hậu quả: user trong tenant A có thể access resource Y trong tenant A (mà không thuộc về họ) — chỉ blocked nếu Hibernate filter restrictive enough (tenant scope). Resource scope (e.g., own child / own class / own invoice) NOT enforced.

## Evidence (PR #1763 Bucket D)

Bucket D audit matrix (full list trong PR body):

**Protected (8 controllers):**
- ParentPaymentController (✓ @PreAuthorize per parent's own children)
- PaymentController (✓ Wave 105 Bucket E0 fix userId from UserContext)
- TeacherClassController (⚠️ uses `hasAccessToClass` which is broken per GAP-727)
- ... (5 others)

**Tenant-only (11 controllers — A01 IDOR risk):**
- EnrollmentController
- ClassController (CRUD endpoints)
- StudentController
- TeacherController (basic CRUD)
- AttendanceController
- GradeController
- CourseController
- BillingController (read endpoints)
- InvoiceController (read endpoints)
- ScheduleController
- ReportController

(Exact 11-list trong PR #1763 audit matrix body)

## Root Cause

Pattern: dev adds tenant-level Hibernate `@Filter` for multi-tenant isolation but forgets per-resource `@PreAuthorize` for finer-grained access. Reasoning often "user is in this tenant so they can access" — valid for SHARED tenant resources (class list visible to all in tenant) but WRONG for OWNED resources (parent's child, teacher's class).

OWASP A01 (Broken Access Control) #1 attack vector trong web apps theo 2021 Top 10.

## Proposed Fix (Wave beta-readiness-2+)

Per controller in 11-list, classify:
- **SHARED resource** (e.g., class list visible to tenant) → tenant-filter sufficient, document explicit
- **OWNED resource** (e.g., parent's child grades, teacher's own class) → add `@PreAuthorize("@authz.canAccessOwn{Resource}(#id)")` + service-layer check

Cross-reference V2 audit `failure-mode-matrix-v2-state-checked.md` A5 partial finding (5 @PreAuthorize parent module verified).

## Acceptance Criteria

- [ ] Per 11 controller, classify SHARED vs OWNED
- [ ] OWNED controllers: add `@PreAuthorize` guard + `@authz.canAccessOwn{Resource}()` helper methods
- [ ] IT tests cross-user same-tenant cho mỗi OWNED endpoint (user A view own → 200; view user B → 403)
- [ ] Document SHARED scope per controller javadoc (explicit "shared by design, tenant-filter sufficient")
- [ ] Re-run `CrossUserAuthzTest.java` cover all OWNED endpoints
- [ ] Audit matrix in PR body update post-fix

### Out-of-scope

- Production data exploit verification — gated GAP-612 AWS restore
- Performance impact assessment (`@PreAuthorize` overhead) — defer if measurable

## Priority Rationale (P1, not P0)

P1 thay vì P0 vì:
- A01 IDOR cần intra-tenant user lateral movement (lower attack surface than cross-tenant)
- Beta cohort small (5 tenants × ~1-3 users initial) — risk window limited
- GAP-727 (teacher lock-out) P0 dominant blocker

Upgrade to P0 nếu Wave beta-readiness-7 closure quality audit detect actual exploit path.

## Related

- PR #1763 Wave beta-readiness-1 Bucket D audit finding A01-IDOR-WIDE
- GAP-727 (related — broken class guard subset of this issue)
- GAP-728 (related — test infrastructure gap enables this to slip through CI)
- V2 audit `2026-05-24-outside-in-phase-1-closure-failure-mode-matrix-v2-state-checked.md` A5
- OWASP Top 10 2021 A01 Broken Access Control
