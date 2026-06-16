/**
 * React Query hooks for grade operations (teacher gradebook KC-6).
 *
 * Replaces the mock gradebook (GAP-1430): real per-student grade components are
 * read via {@link useStudentGrades} and entered scores are persisted via
 * {@link useSaveClassGrades} against the live kiteclass-core grade API.
 *
 * @author KiteClass Team
 * @since 2.7.2
 */

'use client';

import { useMutation, useQueries, useQueryClient } from '@tanstack/react-query';
import { gradesApi } from '@/lib/api/grades';
import type { GradeComponentType } from '@/types/grade';

const GRADES_QUERY_KEY = 'grades';

/**
 * One cell to persist: a single assessment score for one student.
 */
export interface SaveGradeCell {
  componentType: GradeComponentType;
  componentName: string;
  componentRefId: number;
  score: number;
  maxScore: number;
  weightPercent: number;
}

/**
 * All dirty cells for one student, to persist in one grade row.
 */
export interface SaveGradeEntry {
  studentId: number;
  cells: SaveGradeCell[];
}

/**
 * Fetch every student's grade (with components) for a class in parallel.
 *
 * Each query degrades to {@code null} when the student has no grade row yet
 * (BE 404 before first save) — see {@link gradesApi.getStudentGradeSafe}.
 */
export function useStudentGrades(classId: number, studentIds: number[]) {
  return useQueries({
    queries: studentIds.map((studentId) => ({
      queryKey: [GRADES_QUERY_KEY, 'student', studentId, 'class', classId],
      queryFn: () => gradesApi.getStudentGradeSafe(studentId, classId),
      enabled: !!classId && !!studentId,
      staleTime: 30_000,
    })),
  });
}

/**
 * Persist entered gradebook scores against the live BE.
 *
 * For each student: resolve the gradeId via idempotent {@code initialize}, then
 * upsert each dirty component, then recalculate the final score. On success the
 * grade queries are invalidated so the grid reflects server-side state.
 */
export function useSaveClassGrades(classId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (entries: SaveGradeEntry[]) => {
      for (const entry of entries) {
        if (entry.cells.length === 0) continue;
        // Idempotent: returns existing grade if already initialized.
        const grade = await gradesApi.initializeGrade(entry.studentId, classId);
        for (const cell of entry.cells) {
          await gradesApi.addOrUpdateComponent({
            gradeId: grade.id,
            componentType: cell.componentType,
            componentName: cell.componentName,
            componentRefId: cell.componentRefId,
            score: cell.score,
            maxScore: cell.maxScore,
            weightPercent: cell.weightPercent,
          });
        }
        // Refresh the derived final score after all components are saved.
        await gradesApi.calculateFinalScore(grade.id);
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [GRADES_QUERY_KEY] });
    },
  });
}
