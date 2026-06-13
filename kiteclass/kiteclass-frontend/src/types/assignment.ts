/**
 * Assignment + submission types — KiteClass per-class assignments (GAP-1113 Bucket D).
 *
 * Mirrors kiteclass-core `AssignmentController` (`/api/v1/assignments`). The teacher
 * give/grade surface consumes these; student submit is a thin stub (full student
 * surface gated KC-9 per wave plan).
 *
 * BigDecimal BE fields map to `number`; LocalDateTime maps to ISO `string`.
 *
 * @author KiteClass Team
 * @since GAP-1113 (Wave RBAC-LMS-FE Increment A)
 */

export type AssignmentStatus = 'DRAFT' | 'PUBLISHED';
export type SubmissionStatus = 'PENDING' | 'GRADED' | 'RETURNED';

export interface Assignment {
  id: number;
  classId: number;
  title: string;
  description?: string | null;
  instructions?: string | null;
  dueDate?: string | null;
  maxScore?: number | null;
  weightPercent?: number | null;
  allowLateSubmission?: boolean | null;
  latePenaltyPercent?: number | null;
  status: AssignmentStatus;
  createdBy?: string | null;
  createdAt?: string;
  updatedAt?: string;
  isOverdue?: boolean | null;
  isAcceptingSubmissions?: boolean | null;
}

export interface Submission {
  id: number;
  assignmentId: number;
  studentId: number;
  submissionDate?: string | null;
  contentUrl?: string | null;
  notes?: string | null;
  score?: number | null;
  adjustedScore?: number | null;
  status: SubmissionStatus;
  gradedBy?: number | null;
  gradedAt?: string | null;
  feedback?: string | null;
  createdAt?: string;
  isLate?: boolean | null;
  penaltyApplied?: number | null;
}

export interface CreateAssignmentRequest {
  classId: number;
  title: string;
  description?: string;
  instructions?: string;
  dueDate?: string;
  maxScore?: number;
  weightPercent?: number;
  allowLateSubmission?: boolean;
  latePenaltyPercent?: number;
}

export type UpdateAssignmentRequest = Partial<Omit<CreateAssignmentRequest, 'classId'>>;

export interface GradeSubmissionRequest {
  score: number;
  feedback?: string;
}

/** Student submit (thin stub — full student surface gated KC-9). */
export interface SubmitAssignmentRequest {
  assignmentId: number;
  contentUrl?: string;
  notes?: string;
}
