/**
 * Tests for the period attendance API client (Phase 1B v1, GAP-323b).
 *
 * @since 4.x.x (Wave 18b2 Bucket A)
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { apiClient } from '@/lib/api-client';
import {
  attendancePeriodApi,
  type AttendancePeriodBatchCreateRequest,
  type AttendancePeriodResponse,
} from '../attendance-period';

vi.mock('@/lib/api-client', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
  },
}));

const sampleRow: AttendancePeriodResponse = {
  id: 1234,
  studentId: 101,
  classId: 202,
  subjectSectionId: 303,
  periodNo: 2,
  date: '2026-09-05',
  status: 'PRESENT',
  recordedBy: 404,
  recordedAt: '2026-09-05T07:05:00',
  notes: null,
  version: 1,
  createdAt: '2026-09-05T07:05:00.123Z',
  updatedAt: null,
};

describe('attendancePeriodApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getDailyRoster', () => {
    it('GETs the class daily roster with date param and returns the list', async () => {
      vi.mocked(apiClient.get).mockResolvedValueOnce({ data: [sampleRow] });

      const result = await attendancePeriodApi.getDailyRoster(202, '2026-09-05');

      expect(apiClient.get).toHaveBeenCalledWith(
        '/api/v1/attendance/periods/classes/202',
        { params: { date: '2026-09-05' } },
      );
      expect(result).toEqual([sampleRow]);
    });

    it('unwraps an ApiResponse-style envelope when the backend returns one', async () => {
      vi.mocked(apiClient.get).mockResolvedValueOnce({
        data: { success: true, data: [sampleRow], timestamp: 'now' },
      });

      const result = await attendancePeriodApi.getDailyRoster(202, '2026-09-05');

      expect(result).toEqual([sampleRow]);
    });
  });

  describe('upsertBatch', () => {
    it('POSTs the batch with X-Teacher-Id header and returns inserted/updated rows', async () => {
      const body: AttendancePeriodBatchCreateRequest = {
        entries: [
          {
            studentId: 101,
            classId: 202,
            subjectSectionId: 303,
            periodNo: 2,
            date: '2026-09-05',
            status: 'PRESENT',
            notes: null,
          },
        ],
      };
      vi.mocked(apiClient.post).mockResolvedValueOnce({ data: [sampleRow] });

      const result = await attendancePeriodApi.upsertBatch(body, { teacherId: 404 });

      expect(apiClient.post).toHaveBeenCalledWith(
        '/api/v1/attendance/periods',
        body,
        { headers: { 'X-Teacher-Id': '404' } },
      );
      expect(result).toEqual([sampleRow]);
    });
  });

  describe('updateOne', () => {
    it('PATCHes a single row with version + X-Teacher-Id header', async () => {
      vi.mocked(apiClient.patch).mockResolvedValueOnce({
        data: { ...sampleRow, status: 'EXCUSED', notes: 'ốm', version: 2 },
      });

      const result = await attendancePeriodApi.updateOne(
        1234,
        { status: 'EXCUSED', notes: 'ốm', version: 1 },
        { teacherId: 404 },
      );

      expect(apiClient.patch).toHaveBeenCalledWith(
        '/api/v1/attendance/periods/1234',
        { status: 'EXCUSED', notes: 'ốm', version: 1 },
        { headers: { 'X-Teacher-Id': '404' } },
      );
      expect(result.status).toBe('EXCUSED');
      expect(result.notes).toBe('ốm');
      expect(result.version).toBe(2);
    });
  });
});
