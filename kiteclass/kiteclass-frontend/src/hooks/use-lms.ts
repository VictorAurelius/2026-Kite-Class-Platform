/**
 * React Query hooks for LMS content authoring + structure reads.
 *
 * @author KiteClass Team
 * @since GAP-1113 (Wave RBAC-LMS-FE Increment A)
 */

'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { lmsApi } from '@/lib/api/lms';
import { toast } from '@/hooks/use-toast';
import type {
  CreateModuleRequest,
  UpdateModuleRequest,
  CreateLessonRequest,
  UpdateLessonRequest,
  CreateResourceRequest,
  ReorderRequest,
} from '@/types/lms';

const LMS_KEY = 'lms';

function onErr(fallback: string) {
  return (error: AxiosError<{ message?: string; code?: string }>) => {
    const message = error.response?.data?.message || error.message || fallback;
    toast({ title: 'Lỗi', description: message, variant: 'destructive' });
  };
}

/** Course structure (modules + lessons). `userId` present → full lessons. */
export function useCourseStructure(courseId: number, userId?: number) {
  return useQuery({
    queryKey: [LMS_KEY, 'structure', courseId, userId ?? 'guest'],
    queryFn: () => lmsApi.getCourseStructure(courseId, userId),
    enabled: !!courseId,
  });
}

/** Full lesson detail for teacher editing. */
export function useLessonForManage(lessonId: number | null, teacherId: number) {
  return useQuery({
    queryKey: [LMS_KEY, 'lesson-manage', lessonId],
    queryFn: () => lmsApi.getLessonForManage(lessonId as number, teacherId),
    enabled: !!lessonId && !!teacherId,
  });
}

/** Teacher completion roster for a course. */
export function useCompletionRoster(courseId: number, teacherId: number, enabled = true) {
  return useQuery({
    queryKey: [LMS_KEY, 'roster', courseId],
    queryFn: () => lmsApi.getCompletionRoster(courseId, teacherId),
    enabled: enabled && !!courseId && !!teacherId,
  });
}

/**
 * Authoring mutations bundled for a single course (invalidate its structure on success).
 * `teacherId` is the current user id (X-Teacher-Id, must be course owner).
 */
export function useLmsAuthoring(courseId: number, teacherId: number) {
  const qc = useQueryClient();
  const invalidate = () =>
    qc.invalidateQueries({ queryKey: [LMS_KEY, 'structure', courseId] });

  const createModule = useMutation({
    mutationFn: (data: CreateModuleRequest) => lmsApi.createModule(courseId, data, teacherId),
    onSuccess: () => {
      invalidate();
      toast({ title: 'Thành công', description: 'Đã thêm chương mới' });
    },
    onError: onErr('Không thể thêm chương'),
  });

  const updateModule = useMutation({
    mutationFn: (vars: { moduleId: number; data: UpdateModuleRequest }) =>
      lmsApi.updateModule(vars.moduleId, vars.data, teacherId),
    onSuccess: () => {
      invalidate();
      toast({ title: 'Thành công', description: 'Đã cập nhật chương' });
    },
    onError: onErr('Không thể cập nhật chương'),
  });

  const deleteModule = useMutation({
    mutationFn: (moduleId: number) => lmsApi.deleteModule(moduleId, teacherId),
    onSuccess: () => {
      invalidate();
      toast({ title: 'Thành công', description: 'Đã xóa chương' });
    },
    onError: onErr('Không thể xóa chương (chương còn bài học?)'),
  });

  const reorderModules = useMutation({
    mutationFn: (data: ReorderRequest) => lmsApi.reorderModules(courseId, data, teacherId),
    onSuccess: invalidate,
    onError: onErr('Không thể sắp xếp lại chương'),
  });

  const createLesson = useMutation({
    mutationFn: (vars: { moduleId: number; data: CreateLessonRequest }) =>
      lmsApi.createLesson(vars.moduleId, vars.data, teacherId),
    onSuccess: () => {
      invalidate();
      toast({ title: 'Thành công', description: 'Đã thêm bài học' });
    },
    onError: onErr('Không thể thêm bài học'),
  });

  const updateLesson = useMutation({
    mutationFn: (vars: { lessonId: number; data: UpdateLessonRequest }) =>
      lmsApi.updateLesson(vars.lessonId, vars.data, teacherId),
    onSuccess: () => {
      invalidate();
      toast({ title: 'Thành công', description: 'Đã cập nhật bài học' });
    },
    onError: onErr('Không thể cập nhật bài học'),
  });

  const deleteLesson = useMutation({
    mutationFn: (lessonId: number) => lmsApi.deleteLesson(lessonId, teacherId),
    onSuccess: () => {
      invalidate();
      toast({ title: 'Thành công', description: 'Đã xóa bài học' });
    },
    onError: onErr('Không thể xóa bài học'),
  });

  const reorderLessons = useMutation({
    mutationFn: (vars: { moduleId: number; data: ReorderRequest }) =>
      lmsApi.reorderLessons(vars.moduleId, vars.data, teacherId),
    onSuccess: invalidate,
    onError: onErr('Không thể sắp xếp lại bài học'),
  });

  const createResource = useMutation({
    mutationFn: (vars: { lessonId: number; data: CreateResourceRequest }) =>
      lmsApi.createResource(vars.lessonId, vars.data, teacherId),
    onSuccess: () => {
      invalidate();
      toast({ title: 'Thành công', description: 'Đã thêm tài nguyên' });
    },
    onError: onErr('Không thể thêm tài nguyên'),
  });

  const deleteResource = useMutation({
    mutationFn: (resourceId: number) => lmsApi.deleteResource(resourceId, teacherId),
    onSuccess: () => {
      invalidate();
      toast({ title: 'Thành công', description: 'Đã xóa tài nguyên' });
    },
    onError: onErr('Không thể xóa tài nguyên'),
  });

  return {
    createModule,
    updateModule,
    deleteModule,
    reorderModules,
    createLesson,
    updateLesson,
    deleteLesson,
    reorderLessons,
    createResource,
    deleteResource,
  };
}
