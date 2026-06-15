/**
 * Grade entry detail — wraps G3 GradebookEntryGrid from `@kite/shared-ui`.
 *
 * VN 10pt scale (Thông tư 22/2021/TT-BGDĐT) validation handled natively by
 * G3's `validateGrade`. Excel paste supported via G3's `parseExcelPaste`
 * helper. Hosts state machine: default / editing / saving / saved /
 * finalize-confirm / finalized / validation-error.
 *
 * @since Wave 49 Bucket B (GAP-268)
 */

'use client';

import { useCallback, useMemo, useState } from 'react';
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
import {
  TEACHER_PROFILE,
  TEACHER_REPORT_ROWS,
} from '@/components/teacher/teacher-mock-data';
import { buildSampleStudents } from '@/components/teacher/teacher-mock-data';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';

const COLUMNS: GradeColumn[] = [
  { id: 'kt15-1', label: "KT 15' #1", weight: 1 },
  { id: 'kt15-2', label: "KT 15' #2", weight: 1 },
  { id: 'kt1tiet', label: 'KT 1 tiết', weight: 2 },
  { id: 'midterm', label: 'Giữa kỳ', weight: 2 },
  { id: 'final', label: 'Cuối kỳ', weight: 3 },
];

export default function TeacherGradesClassPage() {
  const params = useParams<{ classId: string }>();
  const classId = params?.classId ?? 'cls-10a2';
  const classMeta =
    TEACHER_REPORT_ROWS.find((c) => c.classId === classId) ??
    TEACHER_REPORT_ROWS[0]!;

  const [students, setStudents] = useState<GradebookStudent[]>(() =>
    buildSampleStudents()
      .slice(0, 15)
      .map((s) => ({
        studentCode: s.studentCode,
        fullName: s.fullName,
        grades: {},
      })),
  );
  const [cellStatuses, setCellStatuses] = useState<GradebookCellStatus[]>([]);
  const [state, setState] = useState<GradebookGridState>('default');
  const [errorMessage, setErrorMessage] = useState<string | undefined>(undefined);
  const [savedAt, setSavedAt] = useState<string | undefined>(undefined);
  const [showFinalize, setShowFinalize] = useState(false);

  const dirtyCount = useMemo(
    () => cellStatuses.filter((s) => s.state === 'dirty').length,
    [cellStatuses],
  );

  const handleCellChange = useCallback(
    (studentCode: string, columnId: string, rawValue: string) => {
      const result = validateGrade(rawValue);
      setStudents((prev) =>
        prev.map((s) =>
          s.studentCode === studentCode
            ? { ...s, grades: { ...s.grades, [columnId]: result.value } }
            : s,
        ),
      );
      setCellStatuses((prev) => {
        const filtered = prev.filter(
          (cs) => !(cs.studentCode === studentCode && cs.columnId === columnId),
        );
        return [
          ...filtered,
          {
            studentCode,
            columnId,
            state: result.valid ? 'dirty' : 'error',
            error: result.error,
          },
        ];
      });
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
    setState('saving');
    setErrorMessage(undefined);
    try {
      // TODO(GAP-1394): pending BE kc-core gradebook API (batch save grades +
      // roster fetch). LMS domain — endpoint not yet shipped; grid uses sample
      // data until wired. Save is simulated client-side.
      await new Promise((resolve) => setTimeout(resolve, 600));
      setSavedAt(
        `${new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })} bởi ${TEACHER_PROFILE.shortName}`,
      );
      setCellStatuses((prev) =>
        prev.map((cs) => (cs.state === 'dirty' ? { ...cs, state: 'saved' } : cs)),
      );
      setState('saved');
    } catch (err) {
      setErrorMessage(
        err instanceof Error ? err.message : 'Không thể lưu sổ điểm.',
      );
      setState('error');
    }
  }, []);

  return (
    <div className="space-y-4">
      <nav className="text-sm text-muted-foreground" aria-label="Breadcrumb">
        <Link href="/teacher/grades" className="hover:text-foreground">
          Sổ điểm
        </Link>{' '}
        / <span className="text-foreground">{classMeta.className}</span>
      </nav>

      <div className="rounded-md border-l-4 border-orange-400 bg-orange-50 px-3 py-2 text-sm dark:bg-orange-950">
        <span className="font-medium">Quy tắc trừ điểm trễ hạn:</span> 10%/ngày,
        tối đa 50%. Sau khi chốt, chỉ hiệu trưởng có thể chỉnh sửa.
      </div>

      <GradebookEntryGrid
        session={{
          className: `${classMeta.className} - ${classMeta.subject}`,
          term: `${TEACHER_PROFILE.semester} · ${TEACHER_PROFILE.schoolYear}`,
          teacherName: TEACHER_PROFILE.fullName,
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
            Khi đã hoàn tất nhập điểm, bấm <strong>Chốt điểm</strong> để khóa sổ
            điểm + gửi báo cáo về cho hiệu trưởng.
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
