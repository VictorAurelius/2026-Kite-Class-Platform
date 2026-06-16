/**
 * React Query hooks for attendance operations.
 *
 * @author KiteClass Team
 * @since 2.7.0 (PR 3.8)
 */

'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { attendanceApi } from '@/lib/api/attendance';
import type {
  CreateAttendanceRequest,
  BulkAttendanceRequest,
  UpdateAttendanceStatusRequest,
  AttendanceSearchParams,
} from '@/types/attendance';
import { toast } from '@/hooks/use-toast';

const ATTENDANCE_QUERY_KEY = 'attendance';

/**
 * Get attendance by ID.
 */
export function useAttendance(id: number) {
  return useQuery({
    queryKey: [ATTENDANCE_QUERY_KEY, id],
    queryFn: () => attendanceApi.getAttendance(id),
    enabled: !!id,
  });
}

/**
 * Get attendance by enrollment (student history).
 */
export function useAttendanceByEnrollment(
  enrollmentId: number,
  params: AttendanceSearchParams = {}
) {
  return useQuery({
    queryKey: [ATTENDANCE_QUERY_KEY, 'enrollment', enrollmentId, params],
    queryFn: () => attendanceApi.getAttendanceByEnrollment(enrollmentId, params),
    enabled: !!enrollmentId,
  });
}

/**
 * Get all attendance records for a class (across all sessions).
 */
export function useAttendanceByClass(
  classId: number,
  params: AttendanceSearchParams = {},
  options?: { enabled?: boolean }
) {
  return useQuery({
    queryKey: [ATTENDANCE_QUERY_KEY, 'class', classId, params],
    queryFn: () => attendanceApi.getAttendanceByClass(classId, params),
    enabled: options?.enabled !== false && !!classId,
  });
}

/**
 * Get attendance by session (class roster).
 */
export function useAttendanceBySession(
  sessionId: number,
  params: AttendanceSearchParams = {}
) {
  return useQuery({
    queryKey: [ATTENDANCE_QUERY_KEY, 'session', sessionId, params],
    queryFn: () => attendanceApi.getAttendanceBySession(sessionId, params),
    enabled: !!sessionId,
  });
}

/**
 * Mark single attendance.
 */
export function useMarkAttendance() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateAttendanceRequest) =>
      attendanceApi.markAttendance(data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: [ATTENDANCE_QUERY_KEY] });
      queryClient.invalidateQueries({
        queryKey: [ATTENDANCE_QUERY_KEY, 'session', variables.sessionId],
      });
      queryClient.invalidateQueries({
        queryKey: [ATTENDANCE_QUERY_KEY, 'enrollment', variables.enrollmentId],
      });
      toast({
        title: 'Thành công',
        description: 'Đã điểm danh',
      });
    },
    onError: (error: AxiosError<{ message?: string; error?: string }>) => {
      const errorMessage =
        error.response?.data?.message ||
        error.response?.data?.error ||
        error.message ||
        'Không thể điểm danh';

      const errorCode = error.response?.status;
      const errorTitle =
        errorCode === 400
          ? 'Lỗi 400 - Dữ liệu không hợp lệ'
          : errorCode === 404
          ? 'Lỗi 404 - Không tìm thấy'
          : errorCode === 403
          ? 'Lỗi 403 - Không có quyền'
          : 'Lỗi';

      toast({
        title: errorTitle,
        description: errorMessage,
        variant: 'destructive',
      });
    },
  });
}

/**
 * Mark bulk attendance for a session.
 */
export function useMarkBulkAttendance() {
  const queryClient = useQueryClient();

  return useMutation({
    // GAP-1426: bulk mark needs classId for the BE class+session path.
    mutationFn: (vars: { classId: number; data: BulkAttendanceRequest }) =>
      attendanceApi.markBulkAttendance(vars.classId, vars.data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: [ATTENDANCE_QUERY_KEY] });
      queryClient.invalidateQueries({
        queryKey: [ATTENDANCE_QUERY_KEY, 'session', variables.data.sessionId],
      });
      toast({
        title: 'Thành công',
        description: `Đã điểm danh cho ${variables.data.records.length} học viên`,
      });
    },
    onError: (error: AxiosError<{ message?: string; error?: string }>) => {
      const errorMessage =
        error.response?.data?.message ||
        error.response?.data?.error ||
        error.message ||
        'Không thể điểm danh hàng loạt';

      toast({
        title: 'Lỗi',
        description: errorMessage,
        variant: 'destructive',
      });
    },
  });
}

/**
 * Update attendance status.
 */
export function useUpdateAttendanceStatus(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UpdateAttendanceStatusRequest) =>
      attendanceApi.updateAttendanceStatus(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [ATTENDANCE_QUERY_KEY] });
      toast({
        title: 'Thành công',
        description: 'Đã cập nhật trạng thái điểm danh',
      });
    },
    onError: (error: AxiosError<{ message?: string; error?: string }>) => {
      const errorMessage =
        error.response?.data?.message ||
        error.response?.data?.error ||
        error.message ||
        'Không thể cập nhật điểm danh';

      toast({
        title: 'Lỗi',
        description: errorMessage,
        variant: 'destructive',
      });
    },
  });
}

/**
 * Delete attendance.
 */
export function useDeleteAttendance() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => attendanceApi.deleteAttendance(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [ATTENDANCE_QUERY_KEY] });
      toast({
        title: 'Thành công',
        description: 'Đã xóa điểm danh',
      });
    },
    onError: (error: AxiosError<{ message?: string; error?: string }>) => {
      const errorMessage =
        error.response?.data?.message ||
        error.response?.data?.error ||
        error.message ||
        'Không thể xóa điểm danh';

      toast({
        title: 'Lỗi',
        description: errorMessage,
        variant: 'destructive',
      });
    },
  });
}

/**
 * Get student attendance statistics.
 */
export function useStudentAttendanceStats(studentId: number) {
  return useQuery({
    queryKey: [ATTENDANCE_QUERY_KEY, 'stats', 'student', studentId],
    queryFn: () => attendanceApi.getStudentStats(studentId),
    enabled: !!studentId,
  });
}

/**
 * Get class attendance statistics.
 */
export function useClassAttendanceStats(classId: number) {
  return useQuery({
    queryKey: [ATTENDANCE_QUERY_KEY, 'stats', 'class', classId],
    queryFn: () => attendanceApi.getClassStats(classId),
    enabled: !!classId,
  });
}

/**
 * Get system-wide attendance statistics across all active classes.
 */
export function useSystemAttendanceStats(
  dateRange?: { startDate: string; endDate: string }
) {
  // Import classes hook dynamically to avoid circular dependencies
  const { data: _classes } = useQuery({
    queryKey: ['classes', 'all-active'],
    queryFn: async () => {
      const { useAllActiveClasses: _useAllActiveClasses } = await import('./use-classes');
      return [];
    },
  });

  return useQuery({
    queryKey: [ATTENDANCE_QUERY_KEY, 'system-stats', dateRange],
    queryFn: async () => {
      const { useAllActiveClasses: _useAllActiveClasses } = await import('./use-classes');
      const { classesApi } = await import('@/lib/api/classes');

      // Get all courses and their classes
      const { coursesApi } = await import('@/lib/api/courses');
      const coursesData = await coursesApi.getAll({ page: 0, size: 100 });

      if (!coursesData?.content) {
        return {
          totalClasses: 0,
          totalSessions: 0,
          totalStudents: 0,
          overallAttendanceRate: 0,
          presentCount: 0,
          absentCount: 0,
          lateCount: 0,
          excusedCount: 0,
          makeupCount: 0,
        };
      }

      // Fetch classes for all courses
      const classPromises = coursesData.content.map((course) =>
        classesApi.getByCourse(course.id, { page: 0, size: 100 })
      );
      const classesResults = await Promise.all(classPromises);
      const allClasses = classesResults.flatMap((result) => result.content);
      const activeClasses = allClasses.filter(
        (c) => c.status === 'SCHEDULED' || c.status === 'IN_PROGRESS'
      );

      // Fetch stats for all active classes
      const statsPromises = activeClasses.map((c) =>
        attendanceApi.getClassStats(c.id).catch(() => null)
      );
      const statsResults = await Promise.all(statsPromises);
      const validStats = statsResults.filter((s) => s !== null);

      // Aggregate stats
      const totalSessions = validStats.reduce((sum, s) => sum + (s?.totalSessions || 0), 0);
      const presentCount = validStats.reduce((sum, s) => sum + (s?.presentCount || 0), 0);
      const absentCount = validStats.reduce((sum, s) => sum + (s?.absentCount || 0), 0);
      const lateCount = validStats.reduce((sum, s) => sum + (s?.lateCount || 0), 0);
      const excusedCount = validStats.reduce((sum, s) => sum + (s?.excusedCount || 0), 0);
      const makeupCount = validStats.reduce((sum, s) => sum + (s?.makeupCount || 0), 0);

      const totalMarked = presentCount + absentCount + lateCount + excusedCount + makeupCount;
      const overallAttendanceRate = totalMarked > 0
        ? (presentCount + lateCount + makeupCount) / totalMarked * 100
        : 0;

      return {
        totalClasses: activeClasses.length,
        totalSessions,
        totalStudents: validStats.length, // Approximate
        overallAttendanceRate,
        presentCount,
        absentCount,
        lateCount,
        excusedCount,
        makeupCount,
      };
    },
  });
}

/**
 * Get attendance trends over time for specified classes.
 */
export function useAttendanceTrends(
  classIds: number[],
  dateRange?: { startDate: string; endDate: string }
) {
  return useQuery({
    queryKey: [ATTENDANCE_QUERY_KEY, 'trends', classIds, dateRange],
    queryFn: async () => {
      // Fetch attendance for all classes
      const attendancePromises = classIds.map((classId) =>
        attendanceApi.getAttendanceByClass(classId, {
          startDate: dateRange?.startDate,
          endDate: dateRange?.endDate,
          page: 0,
          size: 1000,
        }).catch(() => ({ content: [] }))
      );

      const results = await Promise.all(attendancePromises);
      const allAttendance = results.flatMap((r) => r.content);

      // Group by date
      const dateMap = new Map<string, { present: number; total: number }>();

      allAttendance.forEach((attendance) => {
        const date = attendance.markedDate.split('T')[0] || attendance.markedDate; // Extract date part
        const existing = dateMap.get(date) || { present: 0, total: 0 };

        existing.total += 1;
        if (
          attendance.status === 'PRESENT' ||
          attendance.status === 'LATE' ||
          attendance.status === 'MAKEUP'
        ) {
          existing.present += 1;
        }

        dateMap.set(date, existing);
      });

      // Convert to trend points array
      return Array.from(dateMap.entries())
        .map(([date, stats]) => ({
          date,
          attendanceRate: stats.total > 0 ? (stats.present / stats.total) * 100 : 0,
          presentCount: stats.present,
          totalSessions: stats.total,
        }))
        .sort((a, b) => a.date.localeCompare(b.date));
    },
    enabled: classIds.length > 0,
  });
}

/**
 * Get today's class sessions.
 * Note: This is a client-side filter since backend doesn't have a direct endpoint.
 */
export function useTodayClassSessions() {
  return useQuery({
    queryKey: [ATTENDANCE_QUERY_KEY, 'today-sessions'],
    queryFn: async () => {
      const { classesApi } = await import('@/lib/api/classes');
      const { coursesApi } = await import('@/lib/api/courses');

      // Get all courses and their classes
      const coursesData = await coursesApi.getAll({ page: 0, size: 100 });
      if (!coursesData?.content) return [];

      // Fetch classes for all courses
      const classPromises = coursesData.content.map((course) =>
        classesApi.getByCourse(course.id, { page: 0, size: 100 })
      );
      const classesResults = await Promise.all(classPromises);
      const allClasses = classesResults.flatMap((result) => result.content);
      const activeClasses = allClasses.filter(
        (c) => c.status === 'SCHEDULED' || c.status === 'IN_PROGRESS'
      );

      // Fetch sessions for all active classes
      const sessionPromises = activeClasses.map(async (classItem) => {
        const sessions = await classesApi.getSessions(classItem.id).catch(() => []);
        return sessions.map((session) => ({
          ...session,
          className: classItem.name,
          classId: classItem.id,
        }));
      });

      const sessionResults = await Promise.all(sessionPromises);
      const allSessions = sessionResults.flat();

      // Filter for today's sessions
      const today = new Date().toISOString().split('T')[0] || '';
      const todaySessions = allSessions.filter((session) => {
        const sessionDate = session.startTime?.split('T')[0] || '';
        return sessionDate === today;
      });

      // Check attendance status for each session
      const sessionsWithStatus = await Promise.all(
        todaySessions.map(async (session) => {
          const attendance = await attendanceApi
            .getAttendanceBySession(session.id, { page: 0, size: 1 })
            .catch(() => ({ content: [], totalElements: 0 }));

          return {
            sessionId: session.id,
            sessionNumber: session.sessionNumber || 0,
            classId: session.classId,
            className: session.className,
            startTime: session.startTime || '',
            endTime: session.endTime || '',
            totalStudents: 0, // Enrollment count not yet available from session endpoint
            attendanceMarked: attendance.totalElements > 0,
            presentCount: attendance.content.filter((a) => a.status === 'PRESENT').length,
            absentCount: attendance.content.filter((a) => a.status === 'ABSENT').length,
          };
        })
      );

      return sessionsWithStatus;
    },
  });
}
