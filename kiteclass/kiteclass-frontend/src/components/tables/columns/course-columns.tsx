/**
 * Course table columns configuration.
 *
 * @author KiteClass Team
 * @since 3.6.0
 */

'use client';

import { ColumnDef } from '@tanstack/react-table';
import Link from 'next/link';
import { Eye, Pencil, Trash2 } from 'lucide-react';
import { Course, CourseStatus } from '@/types/course';
import { StatusBadge } from '@/components/common';
import { Button } from '@/components/ui/button';

const statusVariants: Record<CourseStatus, 'success' | 'warning' | 'default' | 'error'> = {
  [CourseStatus.DRAFT]: 'warning',
  [CourseStatus.PUBLISHED]: 'success',
  [CourseStatus.ARCHIVED]: 'default',
};

const statusLabels: Record<CourseStatus, string> = {
  [CourseStatus.DRAFT]: 'Bản nháp',
  [CourseStatus.PUBLISHED]: 'Đã xuất bản',
  [CourseStatus.ARCHIVED]: 'Đã lưu trữ',
};

export const getCourseColumns = (
  onDelete: (id: number) => void
): ColumnDef<Course>[] => [
  {
    accessorKey: 'code',
    header: 'Mã khóa học',
    cell: ({ row }) => (
      <span className="font-mono text-sm">{row.original.code}</span>
    ),
  },
  {
    accessorKey: 'name',
    header: 'Tên khóa học',
  },
  {
    accessorKey: 'durationWeeks',
    header: 'Thời lượng',
    cell: ({ row }) =>
      row.original.durationWeeks != null
        ? `${row.original.durationWeeks} tuần`
        : '—',
  },
  {
    accessorKey: 'totalSessions',
    header: 'Số buổi',
    cell: ({ row }) => row.original.totalSessions ?? '—',
  },
  {
    accessorKey: 'price',
    header: 'Học phí',
    cell: ({ row }) =>
      row.original.price != null
        ? new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND',
          }).format(row.original.price)
        : 'Miễn phí',
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
        <Link href={`/courses/${row.original.id}`}>
          <Button variant="ghost" size="icon">
            <Eye className="h-4 w-4" />
          </Button>
        </Link>
        <Link href={`/courses/${row.original.id}/edit`}>
          <Button variant="ghost" size="icon">
            <Pencil className="h-4 w-4" />
          </Button>
        </Link>
        {row.original.status === CourseStatus.DRAFT && (
          <Button
            variant="ghost"
            size="icon"
            onClick={() => onDelete(row.original.id)}
          >
            <Trash2 className="h-4 w-4 text-destructive" />
          </Button>
        )}
      </div>
    ),
  },
];
