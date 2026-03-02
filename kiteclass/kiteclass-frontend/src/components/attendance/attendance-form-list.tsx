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
}

export function AttendanceFormList({
  rows,
  onStatusChange,
  onNotesChange,
  title = 'Danh sách học viên',
  showCount = true,
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

          {rows.length === 0 && (
            <div className="py-12 text-center text-muted-foreground">
              <Users className="mx-auto h-12 w-12 opacity-20" />
              <p className="mt-4">Không có học viên nào trong lớp này</p>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
