/**
 * Enrollment domain types.
 *
 * @author KiteClass Team
 * @since 2.6.0
 */

export enum EnrollmentStatus {
  PENDING_PAYMENT = 'PENDING_PAYMENT',
  ACTIVE = 'ACTIVE',
  COMPLETED = 'COMPLETED',
  WITHDRAWN = 'WITHDRAWN',
  SUSPENDED = 'SUSPENDED',
}

export const EnrollmentStatusLabels: Record<EnrollmentStatus, string> = {
  [EnrollmentStatus.PENDING_PAYMENT]: 'Chờ thanh toán',
  [EnrollmentStatus.ACTIVE]: 'Đang học',
  [EnrollmentStatus.COMPLETED]: 'Hoàn thành',
  [EnrollmentStatus.WITHDRAWN]: 'Đã rút',
  [EnrollmentStatus.SUSPENDED]: 'Tạm ngưng',
};

export interface Enrollment {
  id: number;
  studentId: number;
  studentName: string;
  classId: number;
  className: string;
  status: EnrollmentStatus;
  enrollmentDate: string;
  tuitionAmount: number;
  discountPercent: number;
  finalAmount: number;
  createdAt: string;
  updatedAt: string;
}

export interface EnrollmentSearchParams {
  studentId?: number;
  classId?: number;
  status?: EnrollmentStatus;
  page?: number;
  size?: number;
  sort?: string;
}

/**
 * Student-self enrollment row, enriched with class + course names.
 *
 * Mirrors BE {@code MyEnrollmentResponse} (GAP-1285), returned by
 * {@code GET /api/v1/enrollments/me}. {@code className} / {@code courseId} /
 * {@code courseName} are nullable: they degrade to null if the referenced class
 * or course was soft-deleted (display-only — never a cross-tenant leak).
 */
export interface MyEnrollment {
  id: number;
  studentId: number;
  classId: number;
  className: string | null;
  courseId: number | null;
  courseName: string | null;
  enrollmentDate: string;
  status: EnrollmentStatus;
  tuitionAmount: number;
  discountPercent: number;
  finalAmount: number;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

/**
 * Request body for creating a single enrollment.
 *
 * Mirrors BE {@code CreateEnrollmentRequest}
 * (com.kiteclass.core.module.enrollment.dto). Used by the "Thêm học sinh vào lớp"
 * dialog (GAP-1103). {@code discountPercent} defaults to 0 on the server when
 * omitted; {@code notes} is optional.
 */
export interface CreateEnrollmentRequest {
  studentId: number;
  classId: number;
  tuitionAmount: number;
  discountPercent?: number;
  notes?: string;
}
