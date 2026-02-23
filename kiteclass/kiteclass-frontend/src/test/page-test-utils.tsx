/**
 * Shared test utilities for page-level integration tests.
 * Provides common mocking and helper functions.
 *
 * @since 2026-02-23
 */

import { vi } from 'vitest';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';

/**
 * Mock window.confirm for delete confirmations.
 *
 * @param returnValue - Whether the confirmation should return true or false
 * @returns Spy function for assertions
 */
export function mockConfirm(returnValue = true) {
  const spy = vi.fn(() => returnValue);
  window.confirm = spy;
  return spy;
}

/**
 * Mock a 404 Not Found response for a specific endpoint.
 *
 * @param endpoint - API endpoint pattern (supports wildcards)
 */
export function mock404(endpoint: string) {
  server.use(
    http.get(endpoint, () => {
      return HttpResponse.json(
        {
          status: 404,
          error: 'NOT_FOUND',
          message: 'Resource not found',
        },
        { status: 404 }
      );
    })
  );
}

/**
 * Mock a 500 Internal Server Error response.
 *
 * @param endpoint - API endpoint pattern (supports wildcards)
 */
export function mock500(endpoint: string) {
  server.use(
    http.get(endpoint, () => {
      return HttpResponse.json(
        {
          status: 500,
          error: 'INTERNAL_SERVER_ERROR',
          message: 'Đã xảy ra lỗi từ máy chủ',
        },
        { status: 500 }
      );
    })
  );
}

/**
 * Mock an empty list response for pagination endpoints.
 *
 * @param endpoint - API endpoint pattern (supports wildcards)
 */
export function mockEmptyList(endpoint: string) {
  server.use(
    http.get(endpoint, () => {
      return HttpResponse.json({
        success: true,
        data: {
          content: [],
          totalElements: 0,
          totalPages: 0,
          size: 10,
          number: 0,
        },
      });
    })
  );
}

/**
 * Mock a Vietnamese error message response.
 *
 * @param endpoint - API endpoint pattern (supports wildcards)
 * @param message - Vietnamese error message
 * @param status - HTTP status code (default: 400)
 */
export function mockVietnameseError(
  endpoint: string,
  message: string,
  status = 400
) {
  server.use(
    http.post(endpoint, () => {
      return HttpResponse.json(
        {
          status,
          error: 'VALIDATION_ERROR',
          message,
        },
        { status }
      );
    })
  );
}

/**
 * Wait for loading spinner to disappear.
 * Useful for waiting for async operations to complete.
 *
 * @param timeout - Maximum wait time in ms (default: 3000)
 */
export async function waitForLoadingToFinish(timeout = 3000) {
  const { waitFor, screen } = await import('@testing-library/react');

  await waitFor(
    () => {
      const spinner = screen.queryByTestId('loading-spinner');
      if (spinner) {
        throw new Error('Still loading');
      }
    },
    { timeout }
  );
}

/**
 * Mock a duplicate email error (409 Conflict).
 *
 * @param endpoint - API endpoint pattern
 * @param email - Duplicate email address
 */
export function mockDuplicateEmailError(endpoint: string, email: string) {
  server.use(
    http.post(endpoint, () => {
      return HttpResponse.json(
        {
          status: 409,
          error: 'DUPLICATE_EMAIL',
          message: `Email ${email} đã tồn tại trong hệ thống`,
        },
        { status: 409 }
      );
    })
  );
}

/**
 * Mock a validation error response with field-specific errors.
 *
 * @param endpoint - API endpoint pattern
 * @param fieldErrors - Map of field names to error messages (converts strings to arrays)
 */
export function mockValidationError(
  endpoint: string,
  fieldErrors: Record<string, string>
) {
  // Convert string values to arrays to match backend API format
  const formattedErrors = Object.fromEntries(
    Object.entries(fieldErrors).map(([key, value]) => [key, [value]])
  );

  server.use(
    http.post(endpoint, () => {
      return HttpResponse.json(
        {
          status: 400,
          error: 'VALIDATION_ERROR',
          message: 'Dữ liệu không hợp lệ',
          fieldErrors: formattedErrors,
        },
        { status: 400 }
      );
    })
  );
}

/**
 * Mock router push for navigation testing.
 * Must be used with vi.mock('next/navigation').
 *
 * @returns Mock router object with push spy
 */
export function mockRouterPush() {
  const mockPush = vi.fn();
  const mockRouter = {
    push: mockPush,
    replace: vi.fn(),
    back: vi.fn(),
    forward: vi.fn(),
    refresh: vi.fn(),
    prefetch: vi.fn(),
  };

  return { mockPush, mockRouter };
}

/**
 * Student form field labels for consistent testing.
 */
export const STUDENT_FORM_LABELS = {
  name: /tên học viên/i,
  email: /email/i,
  phone: /số điện thoại/i,
  dateOfBirth: /ngày sinh/i,
  address: /địa chỉ/i,
  gender: /giới tính/i,
  status: /trạng thái/i,
} as const;
