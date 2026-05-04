/**
 * use-parent hook tests — Phase 1A (GAP-321 Wave 18b1 Bucket D).
 *
 * Verifies React Query wiring for the new parent-portal endpoints:
 * - useMyChildren: 200 happy path + 500 error
 * - useChildTranscript: 200 + 403 PARENT_NOT_LINKED + disabled-when-no-id
 *
 * The 403 path is the most important — it asserts the server's scope guard
 * (BR-PARENT-PORTAL-001) surfaces as a React Query error rather than silently
 * returning empty data (which would mask IDOR probes).
 */

import { describe, expect, it } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { AllTheProviders } from '@/test/utils';
import { server } from '@/mocks/server';
import { useChildTranscript, useMyChildren } from '../use-parent';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

describe('useMyChildren', () => {
  it('returns linked children list on 200', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/parent/me/children`, () =>
        HttpResponse.json({
          success: true,
          data: [
            {
              studentId: 100,
              studentName: 'Con A',
              className: null,
              grade: null,
              linkType: 'PRIMARY',
            },
          ],
          timestamp: '2026-05-04T11:00:00Z',
        }),
      ),
    );

    const { result } = renderHook(() => useMyChildren(), {
      wrapper: AllTheProviders,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toHaveLength(1);
    expect(result.current.data![0]!.studentName).toBe('Con A');
    expect(result.current.data![0]!.linkType).toBe('PRIMARY');
  });

  it('surfaces server error', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/parent/me/children`, () =>
        HttpResponse.json(
          { success: false, message: 'Server error' },
          { status: 500 },
        ),
      ),
    );

    const { result } = renderHook(() => useMyChildren(), {
      wrapper: AllTheProviders,
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});

describe('useChildTranscript', () => {
  it('returns transcripts when parent is linked (BR-PARENT-PORTAL-001 happy path)', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/parent/children/100/transcript`, () =>
        HttpResponse.json({
          success: true,
          data: [
            {
              transcriptId: 1,
              studentId: 100,
              semester: 'Spring 2026',
              academicYear: 2026,
              totalCredits: 12.0,
              semesterGpa: 3.45,
              cumulativeGpa: 3.52,
              totalCourses: 4,
              passedCourses: 4,
              failedCourses: 0,
            },
          ],
          timestamp: '2026-05-04T11:00:00Z',
        }),
      ),
    );

    const { result } = renderHook(() => useChildTranscript(100), {
      wrapper: AllTheProviders,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data![0]!.semester).toBe('Spring 2026');
    expect(result.current.data![0]!.semesterGpa).toBe(3.45);
  });

  it('surfaces 403 PARENT_NOT_LINKED as error (no silent leak)', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/parent/children/999/transcript`, () =>
        HttpResponse.json(
          {
            success: false,
            errorCode: 'PARENT_NOT_LINKED',
            message: 'PARENT_NOT_LINKED',
          },
          { status: 403 },
        ),
      ),
    );

    const { result } = renderHook(() => useChildTranscript(999), {
      wrapper: AllTheProviders,
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    // Critical: data MUST stay undefined — never an empty array that could
    // be mistaken for "child has no transcripts" (would mask the scope guard).
    expect(result.current.data).toBeUndefined();
  });

  it('does not fire when childId is undefined', () => {
    const { result } = renderHook(() => useChildTranscript(undefined), {
      wrapper: AllTheProviders,
    });
    expect(result.current.isFetching).toBe(false);
  });

  it('does not fire when childId is 0 or negative', () => {
    const { result } = renderHook(() => useChildTranscript(0), {
      wrapper: AllTheProviders,
    });
    expect(result.current.isFetching).toBe(false);
  });
});
