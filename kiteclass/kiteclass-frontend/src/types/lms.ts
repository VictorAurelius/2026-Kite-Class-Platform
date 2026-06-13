/**
 * LMS (Learning Management System) types — KiteClass content-delivery.
 *
 * Mirrors the kiteclass-core `/api/v1/lms` contract
 * (`documents/01-business/kiteclass/lms/api-contract.md`). Consumed by the teacher
 * authoring surface (GAP-1113 Increment A, Bucket A) + the guest catalog paywall
 * (Bucket B). Shapes are contract-first — do NOT invent fields.
 *
 * @author KiteClass Team
 * @since GAP-1113 (Wave RBAC-LMS-FE Increment A)
 */

/** Learning resource type (mirrors core `ResourceType` enum — LMS-relevant subset). */
export type ResourceType = 'VIDEO' | 'PDF' | 'SLIDE' | 'AUDIO' | 'LINK' | 'CODE' | 'OTHER';

/** A lesson resource (PDF / slide / video link / etc.) attached to a lesson. */
export interface LearningResource {
  id: number;
  lessonId: number;
  type: ResourceType;
  url: string;
  title: string;
  fileSize?: number | null;
  createdAt?: string;
  updatedAt?: string;
}

/** A lesson inside a module (structure-level — content may be stripped for guests). */
export interface Lesson {
  id: number;
  moduleId: number;
  title: string;
  content?: string | null;
  videoUrl?: string | null;
  isTrial: boolean;
  orderNumber: number;
  estimatedDuration?: number | null;
  createdAt?: string;
  updatedAt?: string;
}

/** A lesson with its full detail (resources + counts) — `/lessons/{id}/manage`. */
export interface LessonDetail extends Lesson {
  resourceCount?: number;
  resources?: LearningResource[];
}

/** A module (chapter) of a course, with its ordered lessons. */
export interface CourseModuleDetail {
  id: number;
  courseId: number;
  title: string;
  description?: string | null;
  orderNumber: number;
  lessonCount?: number;
  lessons: Lesson[];
  createdAt?: string;
  updatedAt?: string;
}

/** A module summary (create/update/reorder return — no nested lessons). */
export interface CourseModuleSummary {
  id: number;
  courseId: number;
  title: string;
  description?: string | null;
  orderNumber: number;
  createdAt?: string;
  updatedAt?: string;
}

// ---- Requests ----

export interface CreateModuleRequest {
  title: string;
  description?: string;
  orderNumber: number;
}

export interface UpdateModuleRequest {
  title?: string;
  description?: string;
  orderNumber?: number;
}

export interface CreateLessonRequest {
  title: string;
  content?: string;
  videoUrl?: string;
  isTrial?: boolean;
  orderNumber: number;
  estimatedDuration?: number;
}

export interface UpdateLessonRequest {
  title?: string;
  content?: string;
  videoUrl?: string;
  isTrial?: boolean;
  orderNumber?: number;
  estimatedDuration?: number;
}

export interface CreateResourceRequest {
  type: ResourceType;
  url: string;
  title: string;
  fileSize?: number;
}

/** Move / reorder — FE sends the FULL ordered set of siblings (server applies atomically). */
export interface ReorderItem {
  id: number;
  orderNumber: number;
}

export interface ReorderRequest {
  items: ReorderItem[];
}

// ---- Resource upload (2-phase presigned, UC-LMS-10) ----

export interface PresignedUploadRequest {
  fileName: string;
  fileSize: number;
  mimeType: string;
  fileType: string;
  accessLevel: string;
}

export interface PresignedUploadResponse {
  fileId: number;
  uploadUrl: string;
  expiresAt: string;
}

// ---- Completion roster (UC-LMS-11) ----

export interface CompletionRosterStudent {
  userId: number;
  completedLessons: number;
  progressPercent: number;
  completedLessonIds: number[];
}

export interface CompletionRoster {
  courseId: number;
  totalLessons: number;
  students: CompletionRosterStudent[];
}
