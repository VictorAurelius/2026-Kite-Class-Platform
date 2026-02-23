/**
 * Teacher table columns configuration.
 *
 * @author KiteClass Team
 * @since 3.5.0
 */

'use client';

import { ColumnDef } from '@tanstack/react-table';
import Link from 'next/link';
import { Eye, Pencil, Trash2 } from 'lucide-react';
import { Teacher } from '@/types/teacher';
import { TeacherStatus } from '@/types/auth';
import { StatusBadge } from '@/components/common';
import { Button } from '@/components/ui/button';

const statusVariants: Record<TeacherStatus, 'success' | 'warning' | 'default' | 'error'> = {
  [TeacherStatus.ACTIVE]: 'success',
  [TeacherStatus.INACTIVE]: 'warning',
  [TeacherStatus.ON_LEAVE]: 'default',
};

const statusLabels: Record<TeacherStatus, string> = {
  [TeacherStatus.ACTIVE]: 'Đang hoạt động',
  [TeacherStatus.INACTIVE]: 'Tạm ngưng',
  [TeacherStatus.ON_LEAVE]: 'Nghỉ phép',
};

export const getTeacherColumns = (
  onDelete: (id: number) => void
): ColumnDef<Teacher>[] => [
  {
    accessorKey: 'name',
    header: 'Tên giáo viên',
  },
  {
    accessorKey: 'email',
    header: 'Email',
  },
  {
    accessorKey: 'phoneNumber',
    header: 'Số điện thoại',
    cell: ({ row }) => row.original.phoneNumber || '—',
  },
  {
    accessorKey: 'specialization',
    header: 'Chuyên môn',
    cell: ({ row }) => row.original.specialization || '—',
  },
  {
    accessorKey: 'experienceYears',
    header: 'Kinh nghiệm',
    cell: ({ row }) =>
      row.original.experienceYears != null
        ? `${row.original.experienceYears} năm`
        : '—',
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
        <Link href={`/teachers/${row.original.id}`}>
          <Button variant="ghost" size="icon">
            <Eye className="h-4 w-4" />
          </Button>
        </Link>
        <Link href={`/teachers/${row.original.id}/edit`}>
          <Button variant="ghost" size="icon">
            <Pencil className="h-4 w-4" />
          </Button>
        </Link>
        <Button
          variant="ghost"
          size="icon"
          onClick={() => onDelete(row.original.id)}
        >
          <Trash2 className="h-4 w-4 text-destructive" />
        </Button>
      </div>
    ),
  },
];
