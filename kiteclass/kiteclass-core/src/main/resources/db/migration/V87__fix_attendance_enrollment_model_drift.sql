-- V87: Fix attendance table schema drift vs entity model (GAP-996, Wave flow-kc5)
--
-- The Attendance entity moved from a student_id-keyed model to an enrollment_id-keyed
-- model (enrollment_id added V79; entity no longer maps studentId). But V1's legacy
-- schema still had:
--   1. student_id BIGINT NOT NULL        → entity never sets it → every INSERT fails
--   2. chk_attendance_status allowing only lowercase 'present|absent|late|excused'
--      → enum stores UPPERCASE (PRESENT/.../MAKEUP) → CHECK violation + MAKEUP missing
--   3. uk_attendance UNIQUE (session_id, student_id) → stale (student_id now null)
--
-- These were invisible to integration tests because the test profile uses
-- ddl-auto=create-drop (flyway disabled) → test schema generated from the entity
-- (no student_id column). Surfaced by KC-5 G1 production-equivalent walk 2026-06-05.

-- 1. student_id is now legacy/optional (enrollment_id is the canonical key)
ALTER TABLE attendance ALTER COLUMN student_id DROP NOT NULL;

-- 2. Replace lowercase-only status CHECK with one matching AttendanceStatus enum
--    (UPPERCASE values + MAKEUP). App layer validates enum too (defense in depth).
ALTER TABLE attendance DROP CONSTRAINT IF EXISTS chk_attendance_status;
ALTER TABLE attendance ADD CONSTRAINT chk_attendance_status
    CHECK (status IN ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED', 'MAKEUP'));

-- 3. Enforce BR-ATTEND-001 (one record per enrollment per session) at DB level,
--    matching the service-layer duplicate check (existsByEnrollmentIdAndSessionId).
--    Stale uk_attendance (session_id, student_id) is now ineffective (student_id null)
--    but left in place — harmless with null student_id. Add the real constraint.
ALTER TABLE attendance DROP CONSTRAINT IF EXISTS uk_attendance_enrollment_session;
ALTER TABLE attendance ADD CONSTRAINT uk_attendance_enrollment_session
    UNIQUE (enrollment_id, session_id);
