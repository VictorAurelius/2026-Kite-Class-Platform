/**
 * Class table columns configuration.
 *
 * @author KiteClass Team
 * @since 3.7.0
 */

'use client';

import type { ColumnDef } from '@tanstack/react-table';
import Link from 'next/link';
import { Eye, Pencil, Trash2, MapPin, Users } from 'lucide-react';
import { Class, ClassStatus } from '@/types/class';
import { StatusBadge } from '@/components/common';
import { Button } from '@/components/ui/button';
import { formatDate } from '@/lib/utils';

const statusVariants: Record<
  ClassStatus,
  'success' | 'warning' | 'default' | 'error' | 'info'
> = {
  DRAFT: 'default',
  SCHEDULED: 'info',
  IN_PROGRESS: 'warning',
  COMPLETED: 'success',
  CANCELLED: 'error',
};

const statusLabels: Record<ClassStatus, string> = {
  DRAFT: 'Nháp',
  SCHEDULED: 'Đã lên lịch',
  IN_PROGRESS: 'Đang diễn ra',
  COMPLETED: 'Đã hoàn thành',
  CANCELLED: 'Đã hủy',
};

export const getClassColumns = (
  onDelete: (id: number) => void
): ColumnDef<Class>[] => [
  {
    accessorKey: 'name',
    header: 'Tên lớp học',
    cell: ({ row }) => (
      <div>
        <div className="font-medium">{row.original.name}</div>
        {row.original.classCode && (
          <div className="text-xs text-muted-foreground font-mono">
            Mã: {row.original.classCode}
          </div>
        )}
      </div>
    ),
  },
  {
    accessorKey: 'schedule',
    header: 'Lịch học',
    cell: ({ row }) => (
      <div className="text-sm">
        {row.original.schedule || '—'}
        {row.original.startDate && (
          <div className="text-xs text-muted-foreground">
            {formatDate(row.original.startDate)}
            {row.original.endDate && ` - ${formatDate(row.original.endDate)}`}
          </div>
        )}
      </div>
    ),
  },
  {
    accessorKey: 'locationType',
    header: 'Địa điểm',
    cell: ({ row }) => (
      <div className="flex items-center gap-1 text-sm">
        <MapPin className="h-3 w-3" />
        <div>
          <div>
            {row.original.locationType === 'IN_PERSON' ? 'Trực tiếp' : 'Trực tuyến'}
          </div>
          {row.original.locationDetail && (
            <div className="text-xs text-muted-foreground">
              {row.original.locationDetail}
            </div>
          )}
        </div>
      </div>
    ),
  },
  {
    accessorKey: 'maxStudents',
    header: 'Sĩ số',
    cell: ({ row }) => (
      <div className="flex items-center gap-1 text-sm">
        <Users className="h-3 w-3" />
        <span>
          {row.original.currentEnrolled}/{row.original.maxStudents}
        </span>
      </div>
    ),
  },
  {
    accessorKey: 'status',
    header: 'Trạng thái',
    cell: ({ row }) => (
      <StatusBadge
        status={statusLabels[row.original.status]}
        variant={statusVariants[row.original.status]}
      />
    ),
  },
  {
    id: 'actions',
    header: 'Thao tác',
    cell: ({ row }) => (
      <div className="flex items-center gap-2">
        <Link href={`/classes/${row.original.id}`}>
          <Button variant="ghost" size="icon" title="Xem chi tiết">
            <Eye className="h-4 w-4" />
          </Button>
        </Link>
        {(row.original.status === 'DRAFT' || row.original.status === 'SCHEDULED') && (
          <Link href={`/classes/${row.original.id}/edit`}>
            <Button variant="ghost" size="icon" title="Chỉnh sửa">
              <Pencil className="h-4 w-4" />
            </Button>
          </Link>
        )}
        {row.original.status === 'SCHEDULED' &&
          row.original.currentEnrolled === 0 && (
            <Button
              variant="ghost"
              size="icon"
              onClick={() => onDelete(row.original.id)}
              title="Xóa lớp học"
            >
              <Trash2 className="h-4 w-4 text-destructive" />
            </Button>
          )}
      </div>
    ),
  },
];
