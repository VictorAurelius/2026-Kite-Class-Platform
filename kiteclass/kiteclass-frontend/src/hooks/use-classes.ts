/**
 * React Query hooks for class operations.
 *
 * @author KiteClass Team
 * @since 3.7.0
 */

'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { AxiosError } from 'axios';
import { classesApi } from '@/lib/api/classes';
import type {
  CreateClassRequest,
  UpdateClassRequest,
  CancelClassRequest,
  RescheduleClassRequest,
  ClassSearchCriteria,
  CreateScheduleRequest,
  GenerateClassCodeRequest,
  RecurrenceRule,
} from '@/types/class';
import { toast } from '@/hooks/use-toast';

const CLASSES_KEY = 'classes';
const SESSIONS_KEY = 'sessions';

/**
 * Get classes for a course (paginated)
 */
export function useClasses(
  courseId: number,
  params: Omit<ClassSearchCriteria, 'courseId'> = {}
) {
  return useQuery({
    queryKey: [CLASSES_KEY, courseId, params],
    queryFn: () => classesApi.getByCourse(courseId, params),
    enabled: !!courseId,
  });
}

/**
 * Get class by ID
 */
export function useClass(id: number) {
  return useQuery({
    queryKey: [CLASSES_KEY, id],
    queryFn: () => classesApi.getById(id),
    enabled: !!id,
  });
}

/**
 * Get sessions for a class
 */
export function useClassSessions(classId: number) {
  return useQuery({
    queryKey: [SESSIONS_KEY, classId],
    queryFn: () => classesApi.getSessions(classId),
    enabled: !!classId,
  });
}

function useErrorHandler(fallback: string) {
  return (error: AxiosError<{ message?: string; error?: string }>) => {
    const message =
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      fallback;
    toast({ title: 'Lỗi', description: message, variant: 'destructive' });
  };
}

/**
 * Create a new class
 */
export function useCreateClass(courseId: number) {
  const router = useRouter();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateClassRequest) => classesApi.create(courseId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [CLASSES_KEY, courseId] });
      toast({ title: 'Thành công', description: 'Đã tạo lớp học mới' });
      router.push(`/courses/${courseId}`);
    },
    onError: useErrorHandler('Không thể tạo lớp học'),
  });
}

/**
 * Update a class
 */
export function useUpdateClass(id: number) {
  const router = useRouter();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateClassRequest) => classesApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [CLASSES_KEY] });
      toast({ title: 'Thành công', description: 'Đã cập nhật lớp học' });
      router.push(`/classes/${id}`);
    },
    onError: useErrorHandler('Không thể cập nhật lớp học'),
  });
}

/**
 * Delete a class
 */
export function useDeleteClass() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => classesApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [CLASSES_KEY] });
      toast({ title: 'Thành công', description: 'Đã xóa lớp học' });
    },
    onError: useErrorHandler('Không thể xóa lớp học'),
  });
}

// =========================================================================
// Lifecycle actions
// =========================================================================

/**
 * Start a class (SCHEDULED → IN_PROGRESS)
 */
export function useStartClass() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => classesApi.start(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [CLASSES_KEY] });
      toast({ title: 'Thành công', description: 'Lớp học đã bắt đầu' });
    },
    onError: useErrorHandler('Không thể bắt đầu lớp học'),
  });
}

/**
 * Complete a class (IN_PROGRESS → COMPLETED)
 */
export function useCompleteClass() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => classesApi.complete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [CLASSES_KEY] });
      toast({ title: 'Thành công', description: 'Lớp học đã hoàn thành' });
    },
    onError: useErrorHandler('Không thể hoàn thành lớp học'),
  });
}

/**
 * Cancel a class (any → CANCELLED)
 */
export function useCancelClass() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: CancelClassRequest }) =>
      classesApi.cancel(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [CLASSES_KEY] });
      toast({ title: 'Thành công', description: 'Lớp học đã bị hủy' });
    },
    onError: useErrorHandler('Không thể hủy lớp học'),
  });
}

/**
 * Reschedule a class — preserves status SCHEDULED, mutates start/end dates,
 * writes audit log, publishes Outbox event (Wave beta-readiness-4 Bucket D — GAP-291).
 */
export function useRescheduleClass() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: RescheduleClassRequest }) =>
      classesApi.reschedule(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [CLASSES_KEY] });
      toast({ title: 'Thành công', description: 'Đã đổi lịch lớp học thành công' });
    },
    onError: useErrorHandler('Không thể đổi lịch lớp học'),
  });
}

// =========================================================================
// Class code & Schedule
// =========================================================================

/**
 * Generate class enrollment code
 */
export function useGenerateClassCode() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data?: GenerateClassCodeRequest }) =>
      classesApi.generateCode(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [CLASSES_KEY] });
      toast({ title: 'Thành công', description: 'Đã tạo mã lớp học' });
    },
    onError: useErrorHandler('Không thể tạo mã lớp học'),
  });
}

/**
 * Create class schedule and generate sessions
 */
export function useCreateSchedule() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: CreateScheduleRequest }) =>
      classesApi.createSchedule(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: [CLASSES_KEY, id] });
      queryClient.invalidateQueries({ queryKey: [SESSIONS_KEY, id] });
      toast({ title: 'Thành công', description: 'Đã tạo lịch học' });
    },
    onError: useErrorHandler('Không thể tạo lịch học'),
  });
}

/**
 * Generate ClassSession entries from a recurrence rule (GAP-290 Wave 18a).
 *
 * Idempotent on edit per BR-CLASS-009 — preserves attended sessions, regenerates
 * future SCHEDULED ones from the new rule.
 */
export function useGenerateSessionsFromRecurrence() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, rule }: { id: number; rule: RecurrenceRule }) =>
      classesApi.generateSessionsFromRecurrence(id, rule),
    onSuccess: (data, { id }) => {
      queryClient.invalidateQueries({ queryKey: [CLASSES_KEY, id] });
      queryClient.invalidateQueries({ queryKey: [SESSIONS_KEY, id] });
      toast({
        title: 'Thành công',
        description: `Đã tạo ${data.length} buổi học (lịch lặp lại)`,
      });
    },
    onError: useErrorHandler('Không thể tạo buổi học từ quy tắc lặp lại'),
  });
}

// =========================================================================
// Aggregate hooks
// =========================================================================

/**
 * Get all active classes across all courses
 * This is a workaround since backend doesn't have a direct endpoint for all classes.
 * It fetches all courses first, then fetches classes for each course, and merges them.
 */
export function useAllActiveClasses() {
  const { data: coursesData } = useQuery({
    queryKey: ['courses'],
    queryFn: async () => {
      const { coursesApi } = await import('@/lib/api/courses');
      return coursesApi.getAll({ page: 0, size: 100 });
    },
  });

  return useQuery({
    queryKey: [CLASSES_KEY, 'all-active'],
    queryFn: async () => {
      if (!coursesData?.content) return [];

      // Fetch classes for all courses in parallel
      const classPromises = coursesData.content.map((course) =>
        classesApi.getByCourse(course.id, { page: 0, size: 100 })
      );

      const classesResults = await Promise.all(classPromises);

      // Merge all classes and filter for active ones (SCHEDULED or IN_PROGRESS)
      const allClasses = classesResults.flatMap((result) => result.content);
      return allClasses.filter(
        (c) => c.status === 'SCHEDULED' || c.status === 'IN_PROGRESS'
      );
    },
    enabled: !!coursesData?.content,
  });
}
