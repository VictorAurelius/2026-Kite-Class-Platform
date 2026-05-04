/**
 * Parent portal domain types.
 *
 * Phase 1A (GAP-321 — Wave 18b1 Bucket D): transcript read-only.
 * Other facets (attendance, fees, conduct, notifications, discipline)
 * deferred to GAP-321b.
 *
 * @author KiteClass Team
 * @since 2.18.0 (Wave 18b1 — GAP-321 Phase 1A)
 */

/**
 * Minimal child summary for the parent dashboard children selector
 * (mirror of {@code ChildSummaryResponse} record from Wave 2 GAP-052a).
 *
 * <p>className + grade are nullable in Phase 1A — Wave 5 / GAP-321b will
 * enrich them via homeroom + subject-grade joins.
 */
export interface ChildSummary {
  studentId: number;
  studentName: string;
  className: string | null;
  grade: string | null;
  linkType: 'PRIMARY' | 'SECONDARY';
}

/**
 * One semester's transcript snapshot (mirror of {@code TranscriptResponse}
 * record). Per BR-PARENT-PORTAL-006: minimal projection — only fields a
 * parent has the legal right to see per Luật GD 2019 Đ.83 K2.
 */
export interface TranscriptEntry {
  transcriptId: number;
  studentId: number;
  semester: string | null;
  academicYear: number | null;
  totalCredits: number;
  semesterGpa: number | null;
  cumulativeGpa: number | null;
  totalCourses: number;
  passedCourses: number;
  failedCourses: number;
}

/** Self profile mirror — Wave 2 GAP-052a (returned by `/api/v1/parent/me`). */
export interface ParentProfile {
  id: number;
  fullName: string;
  email: string;
  phoneNumber: string | null;
  relationship: 'FATHER' | 'MOTHER' | 'GUARDIAN';
  status: 'PENDING' | 'ACTIVE' | 'INACTIVE';
}
