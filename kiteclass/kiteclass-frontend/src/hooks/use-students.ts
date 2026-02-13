/**
 * React Query hooks for student operations.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { AxiosError } from 'axios';
import { studentsApi } from '@/lib/api/students';
import type {
  CreateStudentRequest,
  UpdateStudentRequest,
  StudentSearchParams,
} from '@/types/student';
import { toast } from '@/hooks/use-toast';

const STUDENTS_QUERY_KEY = 'students';

export function useStudents(params: StudentSearchParams = {}) {
  return useQuery({
    queryKey: [STUDENTS_QUERY_KEY, params],
    queryFn: () => studentsApi.getStudents(params),
  });
}

export function useStudent(id: number) {
  return useQuery({
    queryKey: [STUDENTS_QUERY_KEY, id],
    queryFn: () => studentsApi.getStudent(id),
    enabled: !!id,
  });
}

export function useCreateStudent() {
  const router = useRouter();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateStudentRequest) => studentsApi.createStudent(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [STUDENTS_QUERY_KEY] });
      toast({
        title: 'Thành công',
        description: 'Đã tạo học viên mới',
      });
      router.push('/students');
    },
    onError: (error: AxiosError<{message?: string; error?: string}>) => {
      const errorMessage = error.response?.data?.message
        || error.response?.data?.error
        || error.message
        || 'Không thể tạo học viên';

      const errorCode = error.response?.status;
      const errorTitle = errorCode === 403
        ? 'Lỗi 403 - Không có quyền'
        : errorCode === 401
        ? 'Lỗi 401 - Chưa đăng nhập'
        : 'Lỗi';

      console.error('Create student error:', {
        status: errorCode,
        message: errorMessage,
        fullError: error
      });

      toast({
        title: errorTitle,
        description: errorMessage,
        variant: 'destructive',
      });
    },
  });
}

export function useUpdateStudent(id: number) {
  const router = useRouter();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UpdateStudentRequest) =>
      studentsApi.updateStudent(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [STUDENTS_QUERY_KEY] });
      toast({
        title: 'Thành công',
        description: 'Đã cập nhật thông tin học viên',
      });
      router.push(`/students/${id}`);
    },
    onError: (error: AxiosError<{message?: string; error?: string}>) => {
      const errorMessage = error.response?.data?.message
        || error.response?.data?.error
        || error.message
        || 'Không thể cập nhật học viên';

      toast({
        title: 'Lỗi',
        description: errorMessage,
        variant: 'destructive',
      });
    },
  });
}

export function useDeleteStudent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => studentsApi.deleteStudent(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [STUDENTS_QUERY_KEY] });
      toast({
        title: 'Thành công',
        description: 'Đã xóa học viên',
      });
    },
    onError: (error: AxiosError<{message?: string; error?: string}>) => {
      const errorMessage = error.response?.data?.message
        || error.response?.data?.error
        || error.message
        || 'Không thể xóa học viên';

      toast({
        title: 'Lỗi',
        description: errorMessage,
        variant: 'destructive',
      });
    },
  });
}
