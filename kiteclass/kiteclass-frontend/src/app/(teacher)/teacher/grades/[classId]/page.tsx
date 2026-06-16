/**
 * Grade entry detail — wraps G3 GradebookEntryGrid from `@kite/shared-ui`.
 *
 * VN 10pt scale (Thông tư 22/2021/TT-BGDĐT) validation handled natively by
 * G3's `validateGrade`. Excel paste supported via G3's `parseExcelPaste`
 * helper. Hosts state machine: default / editing / saving / saved /
 * finalize-confirm / finalized / validation-error.
 *
 * Wave KC-6 (GAP-1430): mock roster + simulated save replaced with the live
 * kiteclass-core grade API. Real students come from the class enrollment roster
 * ({@code useActiveEnrollmentsByClass}); existing scores are read per student
 * via {@code useStudentGrades}; entered scores are persisted through
 * {@code useSaveClassGrades} (initialize → upsert component → recalculate). Each
 * gradebook column maps to a stable {@code (componentType, componentRefId)} pair
 * so re-saving a cell updates the same BE component instead of duplicating it.
 *
 * @since Wave 49 Bucket B (GAP-268); Wave KC-6 real-data wiring (GAP-1430)
 */

'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import {
  GradebookEntryGrid,
  type GradebookCell,
  type GradebookCellStatus,
  type GradebookEntryGridProps,
  validateGrade,
} from '@kite/shared-ui';

// `GradeColumn`, `GradebookStudent`, `GradebookGridState` are not directly
// re-exported from the package root yet (only nested module path). Derive
// them from the `GradebookEntryGridProps` shape which IS re-exported.
type GradeColumn = GradebookEntryGridProps['columns'][number];
type GradebookStudent = GradebookEntryGridProps['students'][number];
type GradebookGridState = GradebookEntryGridProps['state'];
import { useAuth } from '@/hooks/useAuth';
import { useClass } from '@/hooks/use-classes';
import { useActiveEnrollmentsByClass } from '@/hooks/use-enrollments';
import {
  useSaveClassGrades,
  useStudentGrades,
  type SaveGradeEntry,
} from '@/hooks/use-grades';
import type { GradeComponentType } from '@/types/grade';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { LoadingSpinner, ErrorAlert } from '@/components/common';

const MAX_SCORE = 10; // VN 10-point scale (Thông tư 22/2021/TT-BGDĐT).

/**
 * Gradebook column ↔ BE grade-component mapping (GAP-1430).
 *
 * `componentRefId` is the stable disambiguator in the BE upsert key
 * `(gradeId, componentType, componentRefId)` — required because several columns
 * share a `componentType` (e.g. the two KT 15' quizzes). `weightPercent` sums to
 * 100 across columns so the BE final-score calculation is valid.
 */
interface GradeColumnMeta extends GradeColumn {
  componentType: GradeComponentType;
  componentRefId: number;
  weightPercent: number;
}

const COLUMN_META: GradeColumnMeta[] = [
  { id: 'kt15-1', label: "KT 15' #1", weight: 1, componentType: 'QUIZ', componentRefId: 1, weightPercent: 10 },
  { id: 'kt15-2', label: "KT 15' #2", weight: 1, componentType: 'QUIZ', componentRefId: 2, weightPercent: 10 },
  { id: 'kt1tiet', label: 'KT 1 tiết', weight: 2, componentType: 'QUIZ', componentRefId: 3, weightPercent: 20 },
  { id: 'midterm', label: 'Giữa kỳ', weight: 2, componentType: 'MIDTERM', componentRefId: 1, weightPercent: 20 },
  { id: 'final', label: 'Cuối kỳ', weight: 3, componentType: 'FINAL', componentRefId: 1, weightPercent: 40 },
];

const COLUMNS: GradeColumn[] = COLUMN_META.map(({ id, label, weight }) => ({
  id,
  label,
  weight,
}));

/** Match a loaded BE component to its gradebook column. */
function columnIdFor(
  componentType: string,
  componentRefId: number | null,
): string | undefined {
  return COLUMN_META.find(
    (c) => c.componentType === componentType && c.componentRefId === componentRefId,
  )?.id;
}

/** A locally-edited cell, before it is persisted. */
interface EditCell {
  value: number | null;
  valid: boolean;
  error?: string;
}

export default function TeacherGradesClassPage() {
  const params = useParams<{ classId: string }>();
  const classIdNum = Number(params?.classId);
  const validClassId = Number.isFinite(classIdNum) && classIdNum > 0;

  const { user } = useAuth();
  const { data: classData } = useClass(validClassId ? classIdNum : 0);
  const {
    data: enrollments,
    isLoading: enrollmentsLoading,
    isError: enrollmentsError,
    error: enrollmentsErr,
  } = useActiveEnrollmentsByClass(validClassId ? classIdNum : 0, { size: 100 });

  // Stable roster (studentId + display name) derived from active enrollments.
  const roster = useMemo(
    () =>
      (enrollments?.content ?? []).map((e) => ({
        studentId: e.studentId,
        studentCode: String(e.studentId),
        fullName: e.studentName,
      })),
    [enrollments],
  );

  const studentIds = useMemo(() => roster.map((r) => r.studentId), [roster]);
  const gradeQueries = useStudentGrades(
    validClassId ? classIdNum : 0,
    studentIds,
  );
  const gradesLoading = gradeQueries.some((q) => q.isLoading);

  // Server snapshot: each student's scores as currently persisted in the BE.
  const serverStudents = useMemo<GradebookStudent[]>(() => {
    return roster.map((r, idx) => {
      const grade = gradeQueries[idx]?.data;
      const grades: Record<string, number> = {};
      grade?.components?.forEach((comp) => {
        const colId = columnIdFor(comp.componentType, comp.componentRefId);
        if (colId && comp.score != null) grades[colId] = Number(comp.score);
      });
      return { studentCode: r.studentCode, fullName: r.fullName, grades };
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roster, gradeQueries.map((q) => q.dataUpdatedAt).join(',')]);

  // Local overlay of user edits, keyed studentCode → columnId.
  const [edits, setEdits] = useState<Record<string, Record<string, EditCell>>>(
    {},
  );
  const [state, setState] = useState<GradebookGridState>('default');
  const [errorMessage, setErrorMessage] = useState<string | undefined>(undefined);
  const [savedAt, setSavedAt] = useState<string | undefined>(undefined);
  const [showFinalize, setShowFinalize] = useState(false);

  const saveMutation = useSaveClassGrades(validClassId ? classIdNum : 0);

  // Render = server snapshot merged with the local edit overlay.
  const students = useMemo<GradebookStudent[]>(() => {
    return serverStudents.map((s) => {
      const studentEdits = edits[s.studentCode];
      if (!studentEdits) return s;
      const merged = { ...s.grades };
      Object.entries(studentEdits).forEach(([colId, cell]) => {
        if (cell.value == null) delete merged[colId];
        else merged[colId] = cell.value;
      });
      return { ...s, grades: merged };
    });
  }, [serverStudents, edits]);

  const cellStatuses = useMemo<GradebookCellStatus[]>(() => {
    const out: GradebookCellStatus[] = [];
    Object.entries(edits).forEach(([studentCode, cols]) => {
      Object.entries(cols).forEach(([columnId, cell]) => {
        out.push({
          studentCode,
          columnId,
          state: cell.valid ? (state === 'saved' ? 'saved' : 'dirty') : 'error',
          error: cell.error,
        });
      });
    });
    return out;
  }, [edits, state]);

  const dirtyCount = useMemo(
    () =>
      Object.values(edits).reduce(
        (acc, cols) =>
          acc + Object.values(cols).filter((c) => c.valid).length,
        0,
      ),
    [edits],
  );

  const handleCellChange = useCallback(
    (studentCode: string, columnId: string, rawValue: string) => {
      const result = validateGrade(rawValue);
      setEdits((prev) => ({
        ...prev,
        [studentCode]: {
          ...prev[studentCode],
          [columnId]: {
            value: result.value ?? null,
            valid: result.valid,
            error: result.error,
          },
        },
      }));
      setState('editing');
    },
    [],
  );

  const handleBulkPaste = useCallback(
    (cells: ReadonlyArray<GradebookCell>) => {
      cells.forEach((c) => handleCellChange(c.studentCode, 'kt15-1', c.rawValue));
    },
    [handleCellChange],
  );

  const handleSave = useCallback(async () => {
    // Reject save while any cell is invalid (BE rejects out-of-range scores too).
    const hasInvalid = Object.values(edits).some((cols) =>
      Object.values(cols).some((c) => !c.valid),
    );
    if (hasInvalid) {
      setState('error');
      setErrorMessage('Có ô điểm không hợp lệ. Vui lòng kiểm tra lại.');
      return;
    }

    // Build per-student save entries from the edit overlay.
    const entries: SaveGradeEntry[] = [];
    Object.entries(edits).forEach(([studentCode, cols]) => {
      const student = roster.find((r) => r.studentCode === studentCode);
      if (!student) return;
      const cells = Object.entries(cols)
        .filter(([, cell]) => cell.value != null)
        .map(([columnId, cell]) => {
          const meta = COLUMN_META.find((m) => m.id === columnId)!;
          return {
            componentType: meta.componentType,
            componentName: meta.label,
            componentRefId: meta.componentRefId,
            score: cell.value as number,
            maxScore: MAX_SCORE,
            weightPercent: meta.weightPercent,
          };
        });
      if (cells.length > 0) entries.push({ studentId: student.studentId, cells });
    });

    if (entries.length === 0) {
      setState('saved');
      return;
    }

    setState('saving');
    setErrorMessage(undefined);
    try {
      await saveMutation.mutateAsync(entries);
      setSavedAt(
        `${new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })} bởi ${user?.name ?? 'bạn'}`,
      );
      // Server snapshot will refetch (query invalidation) → clear local overlay.
      setEdits({});
      setState('saved');
    } catch (err) {
      setErrorMessage(
        err instanceof Error ? err.message : 'Không thể lưu sổ điểm.',
      );
      setState('error');
    }
  }, [edits, roster, saveMutation, user?.name]);

  // Surface load errors back to the default state once resolved.
  useEffect(() => {
    if (!enrollmentsLoading && !gradesLoading && state === 'default') {
      setState('default');
    }
  }, [enrollmentsLoading, gradesLoading, state]);

  const className = classData?.name ?? `Lớp #${params?.classId ?? ''}`;

  if (!validClassId) {
    return (
      <div className="space-y-4">
        <ErrorAlert message="Mã lớp không hợp lệ." />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <nav className="text-sm text-muted-foreground" aria-label="Breadcrumb">
        <Link href="/teacher/grades" className="hover:text-foreground">
          Sổ điểm
        </Link>{' '}
        / <span className="text-foreground">{className}</span>
      </nav>

      <div className="rounded-md border-l-4 border-orange-400 bg-orange-50 px-3 py-2 text-sm dark:bg-orange-950">
        <span className="font-medium">Quy tắc trừ điểm trễ hạn:</span> 10%/ngày,
        tối đa 50%. Sau khi chốt, chỉ hiệu trưởng có thể chỉnh sửa.
      </div>

      {enrollmentsError ? (
        <ErrorAlert
          message={
            enrollmentsErr instanceof Error
              ? enrollmentsErr.message
              : 'Không thể tải danh sách học sinh.'
          }
        />
      ) : enrollmentsLoading ? (
        <LoadingSpinner />
      ) : roster.length === 0 ? (
        <Card>
          <CardContent className="py-8 text-center text-sm text-muted-foreground">
            Lớp này chưa có học sinh nào đang học. Hãy thêm học sinh vào lớp
            trước khi nhập điểm.
          </CardContent>
        </Card>
      ) : (
        <>
          <GradebookEntryGrid
            session={{
              className,
              term: 'Học kỳ hiện tại',
              teacherName: user?.name ?? '',
            }}
            columns={COLUMNS}
            students={students}
            state={state}
            cellStatuses={cellStatuses}
            onCellChange={handleCellChange}
            onSave={handleSave}
            onBulkPaste={handleBulkPaste}
            dirtyCount={dirtyCount}
            errorMessage={errorMessage}
            savedAt={savedAt}
          />

          <Card>
            <CardContent className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-sm text-muted-foreground">
                Khi đã hoàn tất nhập điểm, bấm <strong>Chốt điểm</strong> để khóa
                sổ điểm + gửi báo cáo về cho hiệu trưởng.
              </p>
              <Button
                variant="outline"
                onClick={() => setShowFinalize(true)}
                disabled={state === 'saving'}
              >
                Chốt điểm học kỳ
              </Button>
            </CardContent>
          </Card>
        </>
      )}

      {showFinalize && (
        <Card>
          <CardContent className="space-y-3 py-4">
            <h3 className="font-semibold text-orange-700">
              Xác nhận chốt sổ điểm
            </h3>
            <p className="text-sm text-muted-foreground">
              Sau khi chốt, sổ điểm sẽ chuyển sang trạng thái{' '}
              <strong>Đã chốt</strong>. Mọi thay đổi sau đó cần được hiệu trưởng
              phê duyệt. Hành động này không thể hoàn tác trực tiếp.
            </p>
            <div className="flex gap-2">
              <Button
                onClick={() => {
                  // TODO(GAP-1430 remaining): wire POST /api/v1/grades/:id/finalize
                  // per student (needs teacherId + weights summing to 100 + all
                  // components present). Deferred — grade ENTRY is now real; the
                  // finalize/lock transition is the remaining BE wiring.
                  setShowFinalize(false);
                  setState('saved');
                }}
              >
                Xác nhận chốt điểm
              </Button>
              <Button variant="ghost" onClick={() => setShowFinalize(false)}>
                Hủy
              </Button>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
