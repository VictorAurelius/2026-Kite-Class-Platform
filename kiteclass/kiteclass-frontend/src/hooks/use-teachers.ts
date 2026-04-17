/**
 * React Query hooks for teacher operations.
 *
 * @author KiteClass Team
 * @since 3.5.0
 */

'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { AxiosError } from 'axios';
import { teachersApi } from '@/lib/api/teachers';
import type {
  CreateTeacherRequest,
  UpdateTeacherRequest,
  TeacherSearchParams,
} from '@/types/teacher';
import { toast } from '@/hooks/use-toast';

const TEACHERS_QUERY_KEY = 'teachers';

function useErrorHandler(fallback: string) {
  return (error: AxiosError<{
    message?: string;
    error?: string;
    fieldErrors?: Record<string, string[]>;
  }>) => {
    // Extract field-level errors if available (validation errors)
    const fieldErrors = error.response?.data?.fieldErrors;
    let message = error.response?.data?.message || error.message || fallback;

    // Format field errors into readable message
    if (fieldErrors && Object.keys(fieldErrors).length > 0) {
      const fieldMessages = Object.entries(fieldErrors)
        .map(([field, errors]) => `${field}: ${errors.join(', ')}`)
        .join('\n');
      message = fieldMessages;
    }

    toast({ title: 'Lỗi', description: message, variant: 'destructive' });
  };
}

export function useTeachers(params: TeacherSearchParams = {}) {
  return useQuery({
    queryKey: [TEACHERS_QUERY_KEY, params],
    queryFn: () => teachersApi.getTeachers(params),
  });
}

export function useTeacher(id: number) {
  return useQuery({
    queryKey: [TEACHERS_QUERY_KEY, id],
    queryFn: () => teachersApi.getTeacher(id),
    enabled: !!id,
  });
}

export function useCreateTeacher() {
  const router = useRouter();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateTeacherRequest) => teachersApi.createTeacher(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [TEACHERS_QUERY_KEY] });
      toast({ title: 'Thành công', description: 'Đã tạo giáo viên mới' });
      router.push('/teachers');
    },
    onError: useErrorHandler('Không thể tạo giáo viên'),
  });
}

export function useUpdateTeacher(id: number) {
  const router = useRouter();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UpdateTeacherRequest) => teachersApi.updateTeacher(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [TEACHERS_QUERY_KEY] });
      toast({ title: 'Thành công', description: 'Đã cập nhật thông tin giáo viên' });
      router.push(`/teachers/${id}`);
    },
    onError: useErrorHandler('Không thể cập nhật giáo viên'),
  });
}

export function useDeleteTeacher() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => teachersApi.deleteTeacher(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [TEACHERS_QUERY_KEY] });
      toast({ title: 'Thành công', description: 'Đã xóa giáo viên' });
    },
    onError: useErrorHandler('Không thể xóa giáo viên'),
  });
}
