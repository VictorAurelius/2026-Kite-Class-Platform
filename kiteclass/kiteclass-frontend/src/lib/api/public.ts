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
const publicApiClient = axios.create({
  baseURL: isServer
    ? (process.env.INTERNAL_API_URL || 'http://kite-gateway:9000')
    : (process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9000'),
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
   * Submit contact form (lead capture).
   */
  submitContactForm: async (data: {
    name: string;
    email: string;
    phone?: string;
    message: string;
  }) => {
    const response = await publicApiClient.post(
      '/api/v1/marketing/contact',
      data
    );
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
};
