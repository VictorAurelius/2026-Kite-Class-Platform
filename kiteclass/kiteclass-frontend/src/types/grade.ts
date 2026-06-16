/**
 * Grade domain types.
 *
 * Mirrors the kiteclass-core grade module DTOs
 * ({@code com.kiteclass.core.module.grade.dto}). Used by the teacher gradebook
 * (KC-6) to read/enter real grade components via the live BE API instead of
 * mock sample data (GAP-1430).
 *
 * @author KiteClass Team
 * @since 2.7.2
 */

/**
 * Grade component type — mirrors BE {@code GradeComponentType} enum.
 */
export type GradeComponentType =
  | 'ATTENDANCE'
  | 'ASSIGNMENT'
  | 'MIDTERM'
  | 'FINAL'
  | 'QUIZ'
  | 'PROJECT'
  | 'PARTICIPATION';

/**
 * Grade lifecycle status — mirrors BE {@code GradeStatus} enum.
 */
export type GradeStatus = 'IN_PROGRESS' | 'FINALIZED' | 'PASSED' | 'FAILED';

/**
 * Single grade component (one assessment column for one student).
 *
 * Mirrors BE {@code GradeComponentResponse}.
 */
export interface GradeComponent {
  id: number;
  gradeId: number;
  componentType: GradeComponentType;
  componentName: string;
  componentRefId: number | null;
  score: number | null;
  maxScore: number | null;
  weightPercent: number | null;
  weightedScore: number | null;
  createdAt: string;
  updatedAt: string;
  percentage: number | null;
}

/**
 * Full grade for one student in a class, with all components.
 *
 * Mirrors BE {@code GradeResponse}.
 */
export interface Grade {
  id: number;
  studentId: number;
  classId: number;
  finalScore: number | null;
  letterGrade: string | null;
  gpa: number | null;
  status: GradeStatus;
  passThreshold: number | null;
  comments: string | null;
  calculatedAt: string | null;
  finalizedAt: string | null;
  finalizedBy: number | null;
  createdAt: string;
  updatedAt: string;
  components: GradeComponent[] | null;
  isFinalized: boolean | null;
  isPassed: boolean | null;
  isFailed: boolean | null;
  totalWeight: number | null;
  isWeightValid: boolean | null;
}

/**
 * Request body for creating/updating a grade component.
 *
 * Mirrors BE {@code CreateGradeComponentRequest}. The upsert key on the server
 * is {@code (gradeId, componentType, componentRefId)} — so a stable
 * {@code componentRefId} per gradebook column is required to update the same
 * cell instead of creating duplicates.
 */
export interface CreateGradeComponentRequest {
  gradeId: number;
  componentType: GradeComponentType;
  componentName: string;
  componentRefId?: number;
  score: number;
  maxScore: number;
  weightPercent: number;
}
