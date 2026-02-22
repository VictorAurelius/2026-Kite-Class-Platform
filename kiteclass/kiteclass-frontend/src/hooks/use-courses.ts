/**
 * React Query hooks for course operations.
 *
 * @author KiteClass Team
 * @since 3.6.0
 */

'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { AxiosError } from 'axios';
import { coursesApi } from '@/lib/api/courses';
import type {
  CreateCourseRequest,
  UpdateCourseRequest,
  CourseSearchParams,
} from '@/types/course';
import { toast } from '@/hooks/use-toast';

const COURSES_KEY = 'courses';

export function useCourses(params: CourseSearchParams = {}) {
  return useQuery({
    queryKey: [COURSES_KEY, params],
    queryFn: () => coursesApi.getAll(params),
  });
}

export function useCourse(id: number) {
  return useQuery({
    queryKey: [COURSES_KEY, id],
    queryFn: () => coursesApi.getById(id),
    enabled: !!id,
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

export function useCreateCourse() {
  const router = useRouter();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateCourseRequest) => coursesApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [COURSES_KEY] });
      toast({ title: 'Thành công', description: 'Đã tạo khóa học mới' });
      router.push('/courses');
    },
    onError: useErrorHandler('Không thể tạo khóa học'),
  });
}

export function useUpdateCourse(id: number) {
  const router = useRouter();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateCourseRequest) => coursesApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [COURSES_KEY] });
      toast({ title: 'Thành công', description: 'Đã cập nhật khóa học' });
      router.push(`/courses/${id}`);
    },
    onError: useErrorHandler('Không thể cập nhật khóa học'),
  });
}

export function usePublishCourse() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => coursesApi.publish(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [COURSES_KEY] });
      toast({ title: 'Thành công', description: 'Đã xuất bản khóa học' });
    },
    onError: useErrorHandler('Không thể xuất bản khóa học'),
  });
}

export function useArchiveCourse() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => coursesApi.archive(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [COURSES_KEY] });
      toast({ title: 'Thành công', description: 'Đã lưu trữ khóa học' });
    },
    onError: useErrorHandler('Không thể lưu trữ khóa học'),
  });
}

export function useDeleteCourse() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => coursesApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [COURSES_KEY] });
      toast({ title: 'Thành công', description: 'Đã xóa khóa học' });
    },
    onError: useErrorHandler('Không thể xóa khóa học'),
  });
}
