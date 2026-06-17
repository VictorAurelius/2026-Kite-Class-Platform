/**
 * Class roster section — lists every enrolled student in a class with their
 * enrollment status (GAP-1474 Part A).
 *
 * The class-detail page is already role-gated (Owner/Admin/Staff); this gives
 * those actors a read view of WHO is in the class. It distinguishes ACTIVE
 * ("Đang học") vs PENDING_PAYMENT ("Chờ thanh toán") so it is clear which
 * students are still awaiting payment confirmation and are therefore NOT yet on
 * the attendance roster (BR-ATTEND-001: attendance is ACTIVE-only).
 *
 * The roster endpoint (GET /api/v1/enrollments/class/{classId}) returns
 * studentId + status but NOT the student name, so names are resolved client-side
 * from the students list — the same source the "Thêm học sinh" dialog uses.
 *
 * @author KiteClass Team
 */

'use client';

import { useMemo } from 'react';
import { Users } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { StatusBadge, LoadingSpinner } from '@/components/common';
import { useEnrollmentsByClass } from '@/hooks/use-enrollments';
import { useStudents } from '@/hooks/use-students';
import { EnrollmentStatus, EnrollmentStatusLabels } from '@/types/enrollment';

interface ClassRosterSectionProps {
  classId: number;
}

type StatusVariant = 'success' | 'warning' | 'info' | 'error' | 'default';

const STATUS_VARIANTS: Record<EnrollmentStatus, StatusVariant> = {
  [EnrollmentStatus.ACTIVE]: 'success',
  [EnrollmentStatus.PENDING_PAYMENT]: 'warning',
  [EnrollmentStatus.COMPLETED]: 'info',
  [EnrollmentStatus.WITHDRAWN]: 'error',
  [EnrollmentStatus.SUSPENDED]: 'default',
};

export function ClassRosterSection({ classId }: ClassRosterSectionProps) {
  const {
    data: enrollmentsPage,
    isLoading: enrollmentsLoading,
    error,
  } = useEnrollmentsByClass(classId, { size: 100 });
  // Resolve student names: the class-roster endpoint returns only studentId.
  const { data: studentsPage } = useStudents({ size: 200 });

  const studentNameById = useMemo(() => {
    const map = new Map<number, string>();
    for (const s of studentsPage?.content ?? []) {
      map.set(s.id, s.name);
    }
    return map;
  }, [studentsPage?.content]);

  const enrollments = enrollmentsPage?.content ?? [];

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Users className="h-5 w-5" />
          Danh sách học sinh
          {!enrollmentsLoading && !error && enrollments.length > 0
            ? ` (${enrollments.length})`
            : ''}
        </CardTitle>
      </CardHeader>
      <CardContent>
        {enrollmentsLoading ? (
          <div className="flex justify-center py-8">
            <LoadingSpinner />
          </div>
        ) : error ? (
          <p className="py-8 text-center text-sm text-destructive">
            Không tải được danh sách học sinh. Vui lòng thử lại.
          </p>
        ) : enrollments.length === 0 ? (
          <div className="py-8 text-center text-muted-foreground">
            <Users className="mx-auto h-10 w-10 opacity-20" />
            <p className="mt-3">Lớp chưa có học sinh nào</p>
          </div>
        ) : (
          <ul className="space-y-2">
            {enrollments.map((enrollment) => (
              <li
                key={enrollment.id}
                className="flex items-center justify-between rounded-md border p-3"
              >
                <span className="font-medium">
                  {enrollment.studentName ||
                    studentNameById.get(enrollment.studentId) ||
                    `Học sinh #${enrollment.studentId}`}
                </span>
                <StatusBadge
                  status={
                    EnrollmentStatusLabels[enrollment.status] ?? enrollment.status
                  }
                  variant={STATUS_VARIANTS[enrollment.status] ?? 'default'}
                />
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}
