# Student Portal — Use Cases

**Domain:** kc-student frontend production routes consuming kiteclass-core read APIs
**Status:** Phase 1 v1 — endpoint contracts published; full data joins follow when FE consumer PR lands

---

## UC-STUDENT-PORTAL-01: Today screen — schedule + assignments due today

**Actor:** Student (authenticated, persona kc-student)
**Precondition:** Student có active enrollment in ≥1 class

**Steps:**
1. Student mở route `(dashboard)/student/today`
2. FE gọi `GET /api/v1/students/me/today` (header `X-User-Reference-Id` Gateway re-inject từ KC-native token `referenceId` claim = `auth_credentials.entity_id`, Option B Wave auth-1; client value bị strip)
3. BE `StudentPortalService.getToday(studentReferenceId)`:
   - Phase 1 v1: returns `{date: today, schedulePeriods: [], assignmentsDueToday: []}`
   - Future: joins ClassSchedule (by enrollment + day-of-week) + Assignment (due-date filter)
4. FE renders schedulePeriods (sorted by `periodNo`) + assignmentsDueToday (sorted by created)
5. Empty state shown khi cả 2 lists empty

**Postcondition:** Student sees today's class lineup + immediate assignment deadlines on a single screen

**Errors:**
- `401 AUTH_REQUIRED` — header missing (broken Gateway integration)

**Notes:**
- "Today" = server-side `LocalDate.now()` (Asia/Ho_Chi_Minh timezone implicit qua JVM default)
- Phase 1 v1 returns empty payload để FE wire shape stable; full join lands cùng FE consumer PR

---

## UC-STUDENT-PORTAL-02: Grades index — per-subject summary

**Actor:** Student
**Precondition:** Student có enrollment in ≥1 subject với published grades

**Steps:**
1. Student mở route `(dashboard)/student/grades`
2. FE gọi `GET /api/v1/students/me/grades`
3. BE returns array of `StudentGradeOverview` rows — one per enrolled subject
4. Each row: `{subjectId, subjectName, average, highest, lowest, entryCount}`
5. FE renders sortable table; click row → navigate to UC-STUDENT-PORTAL-03

**Postcondition:** Student sees performance overview across all subjects

**Errors:** Same as UC-01 401 path

---

## UC-STUDENT-PORTAL-03: Grade detail per subject

**Actor:** Student
**Precondition:** Student enrolled in `subjectId`

**Steps:**
1. Student click subject row OR navigates to `(dashboard)/student/grades/[subjectId]`
2. FE gọi `GET /api/v1/students/me/grades/{subjectId}`
3. BE service:
   - Phase 1 v1: returns `{subjectId, subjectName: null, average: null, entries: []}`
   - Future: validate enrollment scope (BR-STUDENT-PORTAL-004) → 404 `STUDENT_PORTAL_SUBJECT_NOT_FOUND` if not enrolled → else join SubjectGrade entries
4. FE renders entry list with type badges (TX / GK / CK per TT 22/2021/TT-BGDĐT)

**Postcondition:** Student sees full grade history for one subject

**Errors:**
- `401 AUTH_REQUIRED` — auth header missing
- `404 STUDENT_PORTAL_SUBJECT_NOT_FOUND` — student không enrolled vào subjectId (Phase 2+)

---

## UC-STUDENT-PORTAL-04: Payments — invoice list

**Actor:** Student
**Precondition:** Student có active enrollment với invoice records

**Steps:**
1. Student mở route `(dashboard)/student/payments`
2. FE gọi `GET /api/v1/students/me/payments`
3. BE returns array of `StudentPaymentSummary` — student's own invoices only (PDPL data-minimization per BR-STUDENT-PORTAL-001)
4. FE renders status-grouped list (PENDING / PARTIALLY_PAID / PAID / OVERDUE / CANCELLED)
5. Settlement action delegates to owner-side payment flow (read-only here per BR-STUDENT-PORTAL-005)

**Postcondition:** Student sees billing transparency without ability to mutate

**Errors:** 401 (header missing)

---

## UC-STUDENT-PORTAL-05: Notifications — paginated feed

**Actor:** Student
**Precondition:** Student có ≥1 notification in feed (or empty state acceptable)

**Steps:**
1. Student mở route `(dashboard)/student/notifications`
2. FE gọi `GET /api/v1/students/me/notifications?limit=20`
3. BE returns `{items: [...], nextCursor: "..." | null}`
4. FE renders newest-first; on scroll → fetch with `?cursor=<nextCursor>&limit=20`
5. FE stops fetching khi `nextCursor` null

**Postcondition:** Student catches up on grade-published / attendance-flag / payment-due / generic announcements

**Errors:**
- `401 AUTH_REQUIRED`
- `400` — `limit` clamping in service (limit > 100 → silently clamped to 100; not an error)

**Notes:**
- Cursor opaqueness: FE MUST NOT interpret cursor value (BR-STUDENT-PORTAL-003)
- Mark-read action future scope (separate mutation endpoint per BR-STUDENT-PORTAL-005)

---

## Cross-references

- **Rules:** `BR-STUDENT-PORTAL-001 .. 005` (see `rules.md`)
- **API Contract:** `api-contract.md` (5 endpoints)
- **Parent surface (sibling persona):** `documents/01-business/kiteclass/parent-portal/` — Parent Portal uses `/api/v1/parent/children/{childId}/**` shape
- **Wave 49:** kc-student FE shipped với mock fixtures (PR #1093) — Wave 51 Bucket B adds backend endpoints; FE swap-to-real-data follow-up
