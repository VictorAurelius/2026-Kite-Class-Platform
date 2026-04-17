/**
 * Attendance form row component - represents a single student's attendance entry.
 *
 * @author KiteClass Team
 * @since 2.7.0 (PR 3.8)
 */

'use client';

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import {
  AttendanceStatus,
  AttendanceStatusLabels,
  AttendanceStatusColors,
} from '@/types/attendance';

interface AttendanceFormRowProps {
  enrollmentId: number;
  studentName: string;
  status: AttendanceStatus;
  notes: string;
  onStatusChange: (status: AttendanceStatus) => void;
  onNotesChange: (notes: string) => void;
}

export function AttendanceFormRow({
  enrollmentId,
  studentName,
  status,
  notes,
  onStatusChange,
  onNotesChange,
}: AttendanceFormRowProps) {
  return (
    <div
      className="flex items-start gap-4 rounded-lg border p-4"
      data-enrollment-id={enrollmentId}
    >
      {/* Student Name */}
      <div className="flex-1">
        <p className="font-medium">{studentName}</p>
      </div>

      {/* Status Selector */}
      <div className="w-48">
        <Select
          value={status}
          onValueChange={(value) => onStatusChange(value as AttendanceStatus)}
        >
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {Object.values(AttendanceStatus).map((statusOption) => (
              <SelectItem key={statusOption} value={statusOption}>
                <span
                  className={`inline-flex items-center rounded-full px-2 py-1 text-xs font-medium ${AttendanceStatusColors[statusOption]}`}
                >
                  {AttendanceStatusLabels[statusOption]}
                </span>
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* Notes */}
      <div className="w-64">
        <Textarea
          placeholder="Ghi chú (nếu có)..."
          value={notes}
          onChange={(e) => onNotesChange(e.target.value)}
          className="min-h-[60px]"
        />
      </div>
    </div>
  );
}
