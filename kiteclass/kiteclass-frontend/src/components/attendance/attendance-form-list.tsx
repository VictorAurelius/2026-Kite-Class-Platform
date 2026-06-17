/**
 * Attendance form list component - displays a list of attendance form rows.
 *
 * @author KiteClass Team
 * @since 2.7.0 (PR 3.8)
 */

'use client';

import { Users } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { AttendanceFormRow } from './attendance-form-row';
import { AttendanceStatus } from '@/types/attendance';

export interface StudentAttendanceRow {
  enrollmentId: number;
  studentName: string;
  status: AttendanceStatus;
  notes: string;
}

interface AttendanceFormListProps {
  rows: StudentAttendanceRow[];
  onStatusChange: (enrollmentId: number, status: AttendanceStatus) => void;
  onNotesChange: (enrollmentId: number, notes: string) => void;
  title?: string;
  showCount?: boolean;
  /**
   * GAP-1474: number of PENDING_PAYMENT enrollments in the class. When the
   * ACTIVE-only attendance roster is empty (BR-ATTEND-001) but this is > 0, the
   * empty state explains that students are awaiting payment confirmation —
   * instead of a silent "no students" message that hides the real reason.
   */
  pendingPaymentCount?: number;
}

export function AttendanceFormList({
  rows,
  onStatusChange,
  onNotesChange,
  title = 'Danh sách học viên',
  showCount = true,
  pendingPaymentCount = 0,
}: AttendanceFormListProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Users className="h-5 w-5" />
          {title} {showCount && `(${rows.length})`}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {rows.map((row) => (
            <AttendanceFormRow
              key={row.enrollmentId}
              enrollmentId={row.enrollmentId}
              studentName={row.studentName}
              status={row.status}
              notes={row.notes}
              onStatusChange={(status) => onStatusChange(row.enrollmentId, status)}
              onNotesChange={(notes) => onNotesChange(row.enrollmentId, notes)}
            />
          ))}

          {rows.length === 0 &&
            (pendingPaymentCount > 0 ? (
              // GAP-1474 Part B: explain the silent empty roster — students exist
              // but are PENDING_PAYMENT, excluded from attendance per BR-ATTEND-001.
              <div className="py-12 text-center text-muted-foreground">
                <Users className="mx-auto h-12 w-12 opacity-20" />
                <p className="mt-4 font-medium text-foreground">
                  Chưa có học sinh nào đã kích hoạt để điểm danh
                </p>
                <p className="mx-auto mt-2 max-w-md text-sm">
                  {pendingPaymentCount} học sinh đang chờ xác nhận thanh toán. Hãy
                  xác nhận thanh toán để đưa học sinh vào danh sách điểm danh.
                </p>
              </div>
            ) : (
              <div className="py-12 text-center text-muted-foreground">
                <Users className="mx-auto h-12 w-12 opacity-20" />
                <p className="mt-4">Không có học viên nào trong lớp này</p>
              </div>
            ))}
        </div>
      </CardContent>
    </Card>
  );
}
