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
    mutationFn: (data: BulkAttendanceRequest) =>
      attendanceApi.markBulkAttendance(data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: [ATTENDANCE_QUERY_KEY] });
      queryClient.invalidateQueries({
        queryKey: [ATTENDANCE_QUERY_KEY, 'session', variables.sessionId],
      });
      toast({
        title: 'Thành công',
        description: `Đã điểm danh cho ${variables.records.length} học viên`,
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
