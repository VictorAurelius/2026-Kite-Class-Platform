/**
 * Student table columns configuration.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import { ColumnDef } from '@tanstack/react-table';
import { Student } from '@/types/student';
import { StudentStatus } from '@/types/auth';
import { StatusBadge } from '@/components/common';
import { Button } from '@/components/ui/button';
import { Eye, Pencil, Trash2 } from 'lucide-react';
import Link from 'next/link';

const statusVariants: Record<StudentStatus, 'success' | 'warning' | 'default' | 'error'> = {
  [StudentStatus.ACTIVE]: 'success',
  [StudentStatus.INACTIVE]: 'warning',
  [StudentStatus.GRADUATED]: 'default',
  [StudentStatus.SUSPENDED]: 'error',
};

const statusLabels: Record<StudentStatus, string> = {
  [StudentStatus.ACTIVE]: 'Đang học',
  [StudentStatus.INACTIVE]: 'Không hoạt động',
  [StudentStatus.GRADUATED]: 'Đã tốt nghiệp',
  [StudentStatus.SUSPENDED]: 'Đình chỉ',
};

export const getStudentColumns = (
  onDelete: (id: number) => void
): ColumnDef<Student>[] => [
  {
    accessorKey: 'name',
    header: 'Tên học viên',
  },
  {
    accessorKey: 'email',
    header: 'Email',
  },
  {
    accessorKey: 'phone',
    header: 'Số điện thoại',
    cell: ({ row }) => row.original.phone || '—',
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
    accessorKey: 'enrollmentDate',
    header: 'Ngày nhập học',
    cell: ({ row }) =>
      row.original.enrollmentDate
        ? new Date(row.original.enrollmentDate).toLocaleDateString('vi-VN')
        : '—',
  },
  {
    id: 'actions',
    header: 'Thao tác',
    cell: ({ row }) => (
      <div className="flex items-center gap-2">
        <Link href={`/students/${row.original.id}`}>
          <Button variant="ghost" size="icon">
            <Eye className="h-4 w-4" />
          </Button>
        </Link>
        <Link href={`/students/${row.original.id}/edit`}>
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
