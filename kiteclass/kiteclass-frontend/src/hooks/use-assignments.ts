/**
 * React Query hooks for the teacher assignment give/grade surface (GAP-1113 Bucket D).
 *
 * @author KiteClass Team
 * @since GAP-1113 (Wave RBAC-LMS-FE Increment A)
 */

'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { assignmentsApi } from '@/lib/api/assignments';
import { toast } from '@/hooks/use-toast';
import type {
  CreateAssignmentRequest,
  UpdateAssignmentRequest,
  GradeSubmissionRequest,
} from '@/types/assignment';

const ASSIGN_KEY = 'assignments';

function onErr(fallback: string) {
  return (error: AxiosError<{ message?: string; code?: string }>) => {
    const message = error.response?.data?.message || error.message || fallback;
    toast({ title: 'Lỗi', description: message, variant: 'destructive' });
  };
}

/** Teacher view of a class's assignments (includes drafts). */
export function useClassAssignments(classId: number | null) {
  return useQuery({
    queryKey: [ASSIGN_KEY, 'class', classId],
    queryFn: () => assignmentsApi.getByClass(classId as number),
    enabled: !!classId,
  });
}

/** Submissions for one assignment. */
export function useAssignmentSubmissions(assignmentId: number | null) {
  return useQuery({
    queryKey: [ASSIGN_KEY, 'submissions', assignmentId],
    queryFn: () => assignmentsApi.getSubmissions(assignmentId as number),
    enabled: !!assignmentId,
  });
}

/** Bundled teacher mutations for a class (X-Teacher-Id = current user). */
export function useAssignmentMutations(classId: number | null, teacherId: number) {
  const qc = useQueryClient();
  const invalidate = () => qc.invalidateQueries({ queryKey: [ASSIGN_KEY, 'class', classId] });

  const create = useMutation({
    mutationFn: (data: CreateAssignmentRequest) => assignmentsApi.create(data, teacherId),
    onSuccess: () => {
      invalidate();
      toast({ title: 'Thành công', description: 'Đã tạo bài tập' });
    },
    onError: onErr('Không thể tạo bài tập'),
  });

  const update = useMutation({
    mutationFn: (vars: { id: number; data: UpdateAssignmentRequest }) =>
      assignmentsApi.update(vars.id, vars.data, teacherId),
    onSuccess: () => {
      invalidate();
      toast({ title: 'Thành công', description: 'Đã cập nhật bài tập' });
    },
    onError: onErr('Không thể cập nhật bài tập'),
  });

  const publish = useMutation({
    mutationFn: (id: number) => assignmentsApi.publish(id, teacherId),
    onSuccess: () => {
      invalidate();
      toast({ title: 'Thành công', description: 'Đã giao bài tập cho học viên' });
    },
    onError: onErr('Không thể giao bài tập'),
  });

  const close = useMutation({
    mutationFn: (id: number) => assignmentsApi.close(id, teacherId),
    onSuccess: () => {
      invalidate();
      toast({ title: 'Thành công', description: 'Đã đóng bài tập' });
    },
    onError: onErr('Không thể đóng bài tập'),
  });

  const remove = useMutation({
    mutationFn: (id: number) => assignmentsApi.remove(id, teacherId),
    onSuccess: () => {
      invalidate();
      toast({ title: 'Thành công', description: 'Đã xóa bài tập' });
    },
    onError: onErr('Không thể xóa bài tập'),
  });

  const grade = useMutation({
    mutationFn: (vars: { submissionId: number; data: GradeSubmissionRequest }) =>
      assignmentsApi.grade(vars.submissionId, vars.data, teacherId),
    onSuccess: (_d, vars) => {
      qc.invalidateQueries({ queryKey: [ASSIGN_KEY, 'submissions'] });
      void vars;
      toast({ title: 'Thành công', description: 'Đã chấm điểm' });
    },
    onError: onErr('Không thể chấm điểm'),
  });

  const returnGraded = useMutation({
    mutationFn: (submissionId: number) => assignmentsApi.returnSubmission(submissionId, teacherId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [ASSIGN_KEY, 'submissions'] });
      toast({ title: 'Thành công', description: 'Đã trả bài cho học viên' });
    },
    onError: onErr('Không thể trả bài'),
  });

  return { create, update, publish, close, remove, grade, returnGraded };
}
