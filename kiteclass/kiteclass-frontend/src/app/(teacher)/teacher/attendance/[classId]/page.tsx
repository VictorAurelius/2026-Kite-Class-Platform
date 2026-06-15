/**
 * Attendance per-class detail — wraps G2 AttendanceRoster from `@kite/shared-ui`.
 *
 * Maps to 6 prototype states: default / marking / saving / saved / error /
 * empty / loading. Hosts the controlled G2 component + handles save dispatch
 * via existing kc-core endpoints (TODO(GAP-1394): wire `attendance-period` API
 * for the overview-by-class shape; UI ships state machine + optimistic updates
 * here). The per-period route already wires attendancePeriodApi.upsertBatch;
 * the overview-by-class shape needs a kc-core endpoint not yet shipped.
 *
 * @since Wave 49 Bucket B (GAP-268)
 */

'use client';

import { useCallback, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import {
  AttendanceRoster,
  type AttendanceChangeOpts,
  type AttendanceRosterState,
  type AttendanceStatus,
  type StudentRecord,
} from '@kite/shared-ui';
import {
  TEACHER_SAMPLE_CLASS_SESSION,
  TEACHER_TODAY_QUEUE,
  buildSampleStudents,
} from '@/components/teacher/teacher-mock-data';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';

export default function TeacherAttendanceClassPage() {
  const params = useParams<{ classId: string }>();
  const classId = params?.classId ?? 'cls-10a2-math';
  const classMeta =
    TEACHER_TODAY_QUEUE.find((c) => c.id === classId) ??
    TEACHER_TODAY_QUEUE[0]!;

  const [students, setStudents] = useState<StudentRecord[]>(() =>
    buildSampleStudents(),
  );
  const [state, setState] = useState<AttendanceRosterState>('default');
  const [errorMessage, setErrorMessage] = useState<string | undefined>(undefined);

  const dirtyCount = students.filter((s) => s.status !== null).length;

  const handleChange = useCallback(
    (
      studentId: string,
      status: AttendanceStatus,
      opts?: AttendanceChangeOpts,
    ) => {
      setStudents((prev) =>
        prev.map((s) =>
          s.id === studentId
            ? {
                ...s,
                status,
                lateMinutes: opts?.lateMinutes,
                note: opts?.note,
              }
            : s,
        ),
      );
      setState('marking');
    },
    [],
  );

  const handleMarkAllPresent = useCallback(() => {
    setStudents((prev) => prev.map((s) => ({ ...s, status: 'P' as const })));
    setState('marking');
  }, []);

  const handleSave = useCallback(async () => {
    setState('saving');
    setErrorMessage(undefined);
    try {
      // TODO(GAP-1394): pending BE kc-core attendance-period overview-by-class
      // shape. The (teacher)/attendance/period/[classId]/[periodNo]/[date] route
      // already calls `attendancePeriodApi.upsertBatch`; extending to the
      // overview-by-class shape needs a kc-core endpoint not yet shipped.
      await new Promise((resolve) => setTimeout(resolve, 600));
      setState('saved');
    } catch (err) {
      setErrorMessage(
        err instanceof Error ? err.message : 'Không thể lưu. Vui lòng thử lại.',
      );
      setState('error');
    }
  }, []);

  return (
    <div className="space-y-4">
      <nav
        className="text-sm text-muted-foreground"
        aria-label="Breadcrumb"
      >
        <Link href="/teacher/attendance" className="hover:text-foreground">
          Điểm danh
        </Link>{' '}
        / <span className="text-foreground">{classMeta.className}</span>
      </nav>

      <AttendanceRoster
        classSession={{
          ...TEACHER_SAMPLE_CLASS_SESSION,
          className: `${classMeta.className} - ${classMeta.subject}`,
        }}
        students={students}
        state={state}
        onChange={handleChange}
        onSave={handleSave}
        onMarkAllPresent={handleMarkAllPresent}
        errorMessage={errorMessage}
        dirtyCount={dirtyCount}
      />

      {state === 'saved' && (
        <Card>
          <CardContent className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center sm:justify-between">
            <p className="text-sm text-green-700">
              Đã lưu điểm danh tiết {classMeta.periodNo} cho{' '}
              {classMeta.className}.
            </p>
            <Button asChild variant="outline" size="sm">
              <Link href={`/teacher/grades/${classMeta.id}`}>
                Sang vào điểm
              </Link>
            </Button>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
