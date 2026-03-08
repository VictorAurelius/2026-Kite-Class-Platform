/**
 * Attendance detail dialog component.
 * Shows detailed attendance records for a selected date.
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

'use client';

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import {
  AttendanceStatusLabels,
  AttendanceStatusColors,
  type Attendance,
} from '@/types/attendance';

interface AttendanceDetailDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  date: Date | null;
  records: Attendance[];
}

export function AttendanceDetailDialog({
  open,
  onOpenChange,
  date,
  records,
}: AttendanceDetailDialogProps) {
  if (!date || records.length === 0) {
    return null;
  }

  const formattedDate = date.toLocaleDateString('vi-VN', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  const stats = {
    total: records.length,
    present: records.filter((r) => r.status === 'PRESENT').length,
    absent: records.filter((r) => r.status === 'ABSENT').length,
    late: records.filter((r) => r.status === 'LATE').length,
    excused: records.filter((r) => r.status === 'EXCUSED').length,
    makeup: records.filter((r) => r.status === 'MAKEUP').length,
  };

  // Group records by session
  const sessionGroups = records.reduce((acc, record) => {
    const key = record.sessionId;
    if (!acc[key]) {
      acc[key] = [];
    }
    acc[key].push(record);
    return acc;
  }, {} as Record<number, Attendance[]>);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[80vh] overflow-y-auto sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle>Chi tiết điểm danh</DialogTitle>
          <DialogDescription>{formattedDate}</DialogDescription>
        </DialogHeader>

        {/* Stats Summary */}
        <div className="grid grid-cols-3 gap-2 rounded-lg bg-muted p-4 sm:grid-cols-6">
          <div className="text-center">
            <div className="text-2xl font-bold">{stats.total}</div>
            <div className="text-xs text-muted-foreground">Tổng</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-green-600">{stats.present}</div>
            <div className="text-xs text-muted-foreground">Có mặt</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-red-600">{stats.absent}</div>
            <div className="text-xs text-muted-foreground">Vắng</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-yellow-600">{stats.late}</div>
            <div className="text-xs text-muted-foreground">Đi trễ</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-blue-600">{stats.excused}</div>
            <div className="text-xs text-muted-foreground">Có phép</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-purple-600">{stats.makeup}</div>
            <div className="text-xs text-muted-foreground">Học bù</div>
          </div>
        </div>

        <Separator />

        {/* Records by Session */}
        <div className="space-y-4">
          {Object.entries(sessionGroups).map(([sessionId, sessionRecords]) => (
            <div key={sessionId} className="space-y-2">
              <h4 className="font-semibold">
                Buổi {sessionRecords[0]?.sessionNumber || sessionId}
              </h4>
              <div className="space-y-2">
                {sessionRecords.map((record) => (
                  <div
                    key={record.id}
                    className="flex items-start justify-between gap-4 rounded-lg border p-3"
                  >
                    <div className="flex-1">
                      <div className="font-medium">{record.studentName}</div>
                      {record.notes && (
                        <p className="mt-1 text-sm text-muted-foreground">
                          Ghi chú: {record.notes}
                        </p>
                      )}
                      {record.markedByName && (
                        <p className="mt-1 text-xs text-muted-foreground">
                          Điểm danh bởi: {record.markedByName}
                        </p>
                      )}
                    </div>
                    <div className="flex-shrink-0">
                      <Badge className={AttendanceStatusColors[record.status]}>
                        {AttendanceStatusLabels[record.status]}
                      </Badge>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      </DialogContent>
    </Dialog>
  );
}
