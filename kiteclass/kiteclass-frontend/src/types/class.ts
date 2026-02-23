/**
 * Class module types - aligned with backend PR 2.5
 */

/**
 * Class status enum
 */
export type ClassStatus =
  | 'DRAFT'
  | 'SCHEDULED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED';

/**
 * Location type enum
 */
export type LocationType = 'IN_PERSON' | 'ONLINE';

/**
 * Session status enum
 */
export type SessionStatus =
  | 'SCHEDULED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'MAKEUP';

/**
 * Class response from API
 */
export interface Class {
  id: number;
  courseId: number;
  name: string;
  description: string | null;
  schedule: string | null;
  locationType: LocationType;
  locationDetail: string | null;
  startDate: string | null; // ISO date string
  endDate: string | null; // ISO date string
  maxStudents: number;
  currentEnrolled: number;
  classCode: string | null;
  codeExpiresAt: string | null; // ISO datetime string
  status: ClassStatus;
  startedAt: string | null; // ISO datetime string
  completedAt: string | null; // ISO datetime string
  cancelledAt: string | null; // ISO datetime string
  createdAt: string; // ISO datetime string
  updatedAt: string; // ISO datetime string
}

/**
 * Create class request
 */
export interface CreateClassRequest {
  name: string;
  description?: string;
  schedule?: string;
  locationType?: LocationType;
  locationDetail?: string;
  startDate?: string; // ISO date string
  endDate?: string; // ISO date string
  maxStudents: number;
}

/**
 * Update class request (all fields optional for partial update)
 */
export interface UpdateClassRequest {
  name?: string;
  description?: string;
  schedule?: string;
  locationType?: LocationType;
  locationDetail?: string;
  startDate?: string; // ISO date string
  endDate?: string; // ISO date string
  maxStudents?: number;
}

/**
 * Cancel class request
 */
export interface CancelClassRequest {
  reason: string;
}

/**
 * Class session response
 */
export interface ClassSession {
  id: number;
  classId: number;
  sessionNumber: number;
  sessionDate: string; // ISO date string
  startTime: string; // HH:mm format
  endTime: string; // HH:mm format
  location: string | null;
  topic: string | null;
  status: SessionStatus;
  attendanceTaken: boolean;
}

/**
 * Class code response
 */
export interface ClassCodeResponse {
  classCode: string;
  expiresAt: string | null; // ISO datetime string
}

/**
 * Create schedule request
 */
export interface CreateScheduleRequest {
  daysOfWeek: number[]; // 1=Monday, 7=Sunday
  startTime: string; // HH:mm format
  endTime: string; // HH:mm format
  startDate: string; // ISO date string
  endDate: string; // ISO date string
  excludeDates?: string[]; // ISO date strings
}

/**
 * Generate class code request
 */
export interface GenerateClassCodeRequest {
  customCode?: string;
  expiresInDays?: number;
}

/**
 * Class search criteria
 */
export interface ClassSearchCriteria {
  courseId?: number;
  status?: ClassStatus;
  search?: string;
  page?: number;
  size?: number;
}
