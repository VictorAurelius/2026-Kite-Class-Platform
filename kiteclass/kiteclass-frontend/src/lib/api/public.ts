/**
 * Public API functions (no authentication required).
 * Used for guest/public routes like landing pages, course catalog, etc.
 *
 * @author KiteClass Team
 * @since 3.4.0
 */

import axios from 'axios';
import type { Course, CourseSearchParams } from '@/types/course';
import type { ApiResponse, PaginatedResponse } from '@/types/api';

// Public API client (no auth headers).
// SSR-aware baseURL (GAP-809): server-side runs INSIDE the Next container where
// localhost:9000 has nothing — it must reach the gateway via the docker-network DNS
// (INTERNAL_API_URL=http://kite-gateway:9000). Browser-side uses the host-mapped
// NEXT_PUBLIC_API_URL. Without this split, SSR landing fetch threw ECONNREFUSED
// 127.0.0.1:9000 → public homepage fell back to the generic default branding.
const isServer = typeof window === 'undefined';

/**
 * Browser-side baseURL must PRESERVE the tenant Host (GAP-1207).
 *
 * Tenant-scoped public endpoints (e.g. /api/v1/courses) get their tenant from
 * the gateway's Host-based TenantResolver — the gateway strips any client
 * X-Tenant-Id (GAP-814 anti-spoofing), so the Host header is the only signal.
 * A static NEXT_PUBLIC_API_URL like http://localhost:9000 sends Host=localhost
 * → gateway can't resolve → core rejects with 400.
 *
 * When the page is being served from a tenant host (subdomain — production
 * *.kitehub.me or local nip.io walk), call the gateway on the SAME hostname
 * (only the port comes from NEXT_PUBLIC_API_URL). localhost/IP keeps the
 * configured URL — dev fallback unchanged.
 */
function browserBaseUrl(): string {
  const configured = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9000';
  const { hostname, protocol } = window.location;
  const isIp = /^\d+\.\d+\.\d+\.\d+$/.test(hostname);
  const hasSubdomain = !isIp && hostname !== 'localhost' && hostname.split('.').length >= 3;
  if (!hasSubdomain) return configured;
  let port = '9000';
  try {
    port = new URL(configured).port || port;
  } catch {
    /* keep default gateway port */
  }
  return `${protocol}//${hostname}:${port}`;
}

const publicApiClient = axios.create({
  baseURL: isServer
    ? (process.env.INTERNAL_API_URL || 'http://kite-gateway:9000')
    : browserBaseUrl(),
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const publicApi = {
  /**
   * Get published courses for public catalog.
   * Automatically filters by status=PUBLISHED.
   */
  getCourses: async (
    params: Omit<CourseSearchParams, 'status'> = {}
  ): Promise<PaginatedResponse<Course>> => {
    const response = await publicApiClient.get<
      ApiResponse<PaginatedResponse<Course>>
    >('/api/v1/courses', {
      params: {
        ...params,
        status: 'PUBLISHED', // Only show published courses
      },
    });
    return response.data.data!;
  },

  /**
   * Get course details by ID.
   * Only returns published courses.
   */
  getCourseById: async (id: number): Promise<Course> => {
    const response = await publicApiClient.get<ApiResponse<Course>>(
      `/api/v1/courses/${id}`
    );
    return response.data.data!;
  },

  /**
   * Get landing page data for a tenant.
   */
  getLandingPage: async (tenantId: string) => {
    const response = await publicApiClient.get(
      `/api/v1/tenants/${tenantId}/landing`
    );
    return response.data.data;
  },

  /**
   * Submit a contact message (public lead capture).
   *
   * Wired to the real backend endpoint POST /api/v1/contact
   * (ContactMessageController — public, tenant resolved from the gateway-injected
   * X-Tenant-Id header). The previous path `/api/v1/marketing/contact` did not exist
   * on kiteclass-core → silent 404 (GAP-274 phase-2 contract-drift fix).
   *
   * Backend `CreateContactMessageRequest` requires `subject` (@NotBlank). The contact
   * form (per kiteclass-public kit spec) collects họ tên / SĐT / email (optional) /
   * lời nhắn — no subject field — so a subject is synthesized from the sender name.
   *
   * KNOWN BE CONTRACT GAP (reported, not fabricated): the BE marks `email` as
   * @NotBlank @Email while the kit makes email OPTIONAL. Empty-email submissions
   * will be rejected (400) until the BE relaxes the constraint. Email is sent as-is.
   */
  submitContactForm: async (data: {
    name: string;
    email?: string;
    phone?: string;
    message: string;
    subject?: string;
  }) => {
    const response = await publicApiClient.post('/api/v1/contact', {
      name: data.name,
      email: data.email ?? '',
      phone: data.phone,
      message: data.message,
      subject: data.subject?.trim() || `Liên hệ từ ${data.name}`,
    });
    return response.data;
  },

  /**
   * Get course structure (LMS modules/lessons) for guest preview.
   * No X-User-Id header = guest mode (trial lessons only).
   */
  getCourseStructure: async (courseId: number) => {
    const response = await publicApiClient.get(
      `/api/v1/lms/courses/${courseId}/modules`
    );
    return response.data.data;
  },

  /**
   * Get a single lesson for the guest trial viewer (GAP-1113 Bucket B).
   * No X-User-Id header = guest mode — backend returns trial lessons only; a paid
   * lesson id resolves to 403/404 (caller must show a paywall CTA, never raw error).
   */
  getTrialLesson: async (lessonId: number): Promise<PublicLesson> => {
    const response = await publicApiClient.get<ApiResponse<PublicLesson>>(
      `/api/v1/lms/lessons/${lessonId}`
    );
    return response.data.data!;
  },

  /**
   * Get the open classes (lịch lớp đang mở) for a course — public preview.
   * Used by the course-detail "Lịch lớp đang mở" section. The section hides itself
   * when this returns empty/throws (anti-fabrication: never render fake schedule).
   */
  getCourseClasses: async (
    courseId: number
  ): Promise<PaginatedResponse<PublicClass>> => {
    const response = await publicApiClient.get<
      ApiResponse<PaginatedResponse<PublicClass>>
    >(`/api/v1/courses/${courseId}/classes`, { params: { page: 0, size: 20 } });
    return response.data.data!;
  },
};

/** Subset of the backend ClassResponse used by the public course-detail schedule. */
export interface PublicClass {
  id: number;
  name: string;
  schedule?: string;
  startDate?: string;
  maxStudents?: number;
  currentEnrolled?: number;
  status?: string;
}

/** A trial lesson surfaced to guests (GAP-1113 Bucket B trial viewer). */
export interface PublicLesson {
  id: number;
  moduleId?: number;
  title: string;
  content?: string | null;
  videoUrl?: string | null;
  isTrial?: boolean;
  orderNumber?: number;
  estimatedDuration?: number | null;
}
