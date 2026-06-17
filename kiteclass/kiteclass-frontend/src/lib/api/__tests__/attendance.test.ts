/**
 * Tests for the attendance API client (GAP-1477).
 *
 * Regression guard for the envelope-drift class: the whole kiteclass-core
 * AttendanceController returns `ResponseEntity<X>` UNWRAPPED (no { success, data }
 * envelope). The FE must read `response.data`, NOT `response.data.data` — the latter
 * silently returns `undefined` for every read. GAP-1476 fixed getAttendanceBySession;
 * GAP-1477 fixed the 7 remaining fns covered here.
 *
 * @since GAP-1477 (wave-flow-kc3)
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { apiClient } from '@/lib/api-client';
import { attendanceApi } from '../attendance';
import type { Attendance, AttendanceStatsResponse } from '@/types/attendance';
import { AttendanceStatus } from '@/types/attendance';
import type { PaginatedResponse } from '@/types/api';

vi.mock('@/lib/api-client', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

const sampleRecord: Attendance = {
  id: 802,
  enrollmentId: 109,
  studentName: 'Trần Thị Hồng',
  sessionId: 27,
  sessionNumber: 3,
  status: AttendanceStatus.PRESENT,
  markedDate: '2026-09-05T07:05:00',
  markedBy: 404,
  markedByName: 'Cô Mai',
  notes: undefined,
  pointsAwarded: 1,
  createdAt: '2026-09-05T07:05:00.123Z',
  updatedAt: '2026-09-05T07:05:00.123Z',
};

const sampleStats: AttendanceStatsResponse = {
  targetId: 27,
  targetType: 'CLASS',
  totalSessions: 10,
  presentCount: 8,
  absentCount: 1,
  lateCount: 1,
  excusedCount: 0,
  makeupCount: 0,
  attendanceRate: 90,
};

const samplePage: PaginatedResponse<Attendance> = {
  content: [sampleRecord],
  totalElements: 1,
  totalPages: 1,
  size: 20,
  page: 0,
  first: true,
  last: true,
};

describe('attendanceApi — unwrapped envelope (GAP-1477)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('getAttendance returns the unwrapped record body (not undefined)', async () => {
    // BE returns ResponseEntity<AttendanceResponse> unwrapped → axios body IS the record.
    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: sampleRecord });

    const result = await attendanceApi.getAttendance(802);

    expect(apiClient.get).toHaveBeenCalledWith('/api/v1/attendance/802');
    expect(result).toEqual(sampleRecord);
    expect(result).not.toBeUndefined();
  });

  it('getClassStats returns the unwrapped stats body', async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: sampleStats });

    const result = await attendanceApi.getClassStats(27);

    expect(apiClient.get).toHaveBeenCalledWith('/api/v1/attendance/stats/class/27');
    expect(result.attendanceRate).toBe(90);
    expect(result.totalSessions).toBe(10);
  });

  it('getStudentStats returns the unwrapped stats body', async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({
      data: { ...sampleStats, targetType: 'STUDENT', targetId: 109 },
    });

    const result = await attendanceApi.getStudentStats(109);

    expect(apiClient.get).toHaveBeenCalledWith('/api/v1/attendance/stats/student/109');
    expect(result.targetType).toBe('STUDENT');
  });

  it('getAttendanceByEnrollment returns the unwrapped page body', async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: samplePage });

    const result = await attendanceApi.getAttendanceByEnrollment(109);

    expect(apiClient.get).toHaveBeenCalledWith(
      '/api/v1/attendance/enrollment/109',
      { params: {} },
    );
    expect(result.content).toHaveLength(1);
    expect(result.totalElements).toBe(1);
  });

  it('markAttendance returns the unwrapped created record', async () => {
    vi.mocked(apiClient.post).mockResolvedValueOnce({ data: sampleRecord });

    const result = await attendanceApi.markAttendance({
      enrollmentId: 109,
      sessionId: 27,
      status: AttendanceStatus.PRESENT,
    });

    expect(apiClient.post).toHaveBeenCalledWith('/api/v1/attendance', {
      enrollmentId: 109,
      sessionId: 27,
      status: AttendanceStatus.PRESENT,
    });
    expect(result.id).toBe(802);
  });

  it('markBulkAttendance returns the unwrapped list', async () => {
    vi.mocked(apiClient.post).mockResolvedValueOnce({ data: [sampleRecord] });

    const result = await attendanceApi.markBulkAttendance(27, {
      sessionId: 27,
      records: [{ enrollmentId: 109, status: AttendanceStatus.PRESENT }],
    });

    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/v1/attendance/classes/27/sessions/27/attendance',
      { sessionId: 27, records: [{ enrollmentId: 109, status: AttendanceStatus.PRESENT }] },
    );
    expect(result).toEqual([sampleRecord]);
  });

  it('updateAttendanceStatus returns the unwrapped updated record', async () => {
    vi.mocked(apiClient.patch).mockResolvedValueOnce({
      data: { ...sampleRecord, status: AttendanceStatus.EXCUSED },
    });

    const result = await attendanceApi.updateAttendanceStatus(802, {
      status: AttendanceStatus.EXCUSED,
    });

    expect(apiClient.patch).toHaveBeenCalledWith('/api/v1/attendance/802', {
      status: AttendanceStatus.EXCUSED,
    });
    expect(result.status).toBe('EXCUSED');
  });
});
