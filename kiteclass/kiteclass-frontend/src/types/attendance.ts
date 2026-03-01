/**
 * Attendance domain types.
 *
 * @author KiteClass Team
 * @since 2.7.0 (PR 3.8)
 */

/**
 * Attendance status enum.
 * Maps to backend AttendanceStatus.
 */
export enum AttendanceStatus {
  PRESENT = 'PRESENT',   // Có mặt
  ABSENT = 'ABSENT',     // Vắng
  LATE = 'LATE',         // Đi trễ
  EXCUSED = 'EXCUSED',   // Có phép
  MAKEUP = 'MAKEUP',     // Học bù
}

/**
 * Attendance status display names (Vietnamese).
 */
export const AttendanceStatusLabels: Record<AttendanceStatus, string> = {
  [AttendanceStatus.PRESENT]: 'Có mặt',
  [AttendanceStatus.ABSENT]: 'Vắng',
  [AttendanceStatus.LATE]: 'Đi trễ',
  [AttendanceStatus.EXCUSED]: 'Có phép',
  [AttendanceStatus.MAKEUP]: 'Học bù',
};

/**
 * Attendance status colors for UI.
 */
export const AttendanceStatusColors: Record<AttendanceStatus, string> = {
  [AttendanceStatus.PRESENT]: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300',
  [AttendanceStatus.ABSENT]: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300',
  [AttendanceStatus.LATE]: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-300',
  [AttendanceStatus.EXCUSED]: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300',
  [AttendanceStatus.MAKEUP]: 'bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-300',
};

/**
 * Attendance record (matches backend AttendanceResponse).
 */
export interface Attendance {
  id: number;
  enrollmentId: number;
  studentName: string;
  sessionId: number;
  sessionNumber?: number;
  status: AttendanceStatus;
  markedDate: string; // ISO datetime
  markedBy?: number;
  markedByName?: string;
  notes?: string;
  pointsAwarded: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * Request to mark single attendance.
 */
export interface CreateAttendanceRequest {
  enrollmentId: number;
  sessionId: number;
  status: AttendanceStatus;
  notes?: string;
}

/**
 * Request to mark bulk attendance for a session.
 */
export interface BulkAttendanceRequest {
  sessionId: number;
  records: AttendanceRecord[];
}

/**
 * Single record in bulk attendance request.
 */
export interface AttendanceRecord {
  enrollmentId: number;
  status: AttendanceStatus;
  notes?: string;
}

/**
 * Request to update attendance status.
 */
export interface UpdateAttendanceStatusRequest {
  status: AttendanceStatus;
  notes?: string;
}

/**
 * Attendance statistics response.
 */
export interface AttendanceStatsResponse {
  targetId: number;
  targetType: 'STUDENT' | 'CLASS';
  totalSessions: number;
  presentCount: number;
  absentCount: number;
  lateCount: number;
  excusedCount: number;
  makeupCount: number;
  attendanceRate: number; // percentage (0-100)
}

/**
 * Search/filter parameters for attendance queries.
 */
export interface AttendanceSearchParams {
  enrollmentId?: number;
  sessionId?: number;
  status?: AttendanceStatus;
  startDate?: string; // ISO date
  endDate?: string; // ISO date
  page?: number;
  size?: number;
  sort?: string;
}

/**
 * Attendance calendar day info.
 */
export interface AttendanceCalendarDay {
  date: string; // ISO date
  sessionId?: number;
  hasSessions: boolean;
  attendanceTaken: boolean;
  totalStudents: number;
  presentCount: number;
  absentCount: number;
  lateCount: number;
}
