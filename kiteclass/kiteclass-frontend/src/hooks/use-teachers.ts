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
    onError: (error: AxiosError<{ message?: string; error?: string }>) => {
      const message =
        error.response?.data?.message ||
        error.response?.data?.error ||
        error.message ||
        'Không thể tạo giáo viên';
      toast({ title: 'Lỗi', description: message, variant: 'destructive' });
    },
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
    onError: (error: AxiosError<{ message?: string; error?: string }>) => {
      const message =
        error.response?.data?.message ||
        error.response?.data?.error ||
        error.message ||
        'Không thể cập nhật giáo viên';
      toast({ title: 'Lỗi', description: message, variant: 'destructive' });
    },
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
    onError: (error: AxiosError<{ message?: string; error?: string }>) => {
      const message =
        error.response?.data?.message ||
        error.response?.data?.error ||
        error.message ||
        'Không thể xóa giáo viên';
      toast({ title: 'Lỗi', description: message, variant: 'destructive' });
    },
  });
}
