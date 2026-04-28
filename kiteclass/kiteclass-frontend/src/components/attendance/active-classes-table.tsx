/**
 * Active classes table — extracted from `/attendance/page.tsx` so it can be
 * lazy-loaded behind the summary cards that render above the fold.
 *
 * GAP-236 Sub-PR B (Wave) — code-split admin + attendance + billing routes.
 *
 * @author KiteClass Team
 */

'use client';

import Link from 'next/link';
import { Calendar, Users, Clock } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import type { Class } from '@/types/class';

export interface ActiveClassesTableProps {
  classes: Class[];
  isLoading: boolean;
}

export function ActiveClassesTable({ classes, isLoading }: ActiveClassesTableProps) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Tên lớp</TableHead>
          <TableHead>Mã lớp</TableHead>
          <TableHead>Số học viên</TableHead>
          <TableHead>Trạng thái</TableHead>
          <TableHead>Ngày bắt đầu</TableHead>
          <TableHead className="text-right">Thao tác</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {isLoading ? (
          <TableRow>
            <TableCell colSpan={6} className="py-12 text-center text-muted-foreground">
              <p>Đang tải...</p>
            </TableCell>
          </TableRow>
        ) : classes.length > 0 ? (
          classes.map((classItem) => (
            <TableRow key={classItem.id}>
              <TableCell className="font-medium">{classItem.name}</TableCell>
              <TableCell>
                <code className="rounded bg-muted px-2 py-1 text-sm">
                  {classItem.classCode || 'N/A'}
                </code>
              </TableCell>
              <TableCell>
                <div className="flex items-center gap-2">
                  <Users className="h-4 w-4 text-muted-foreground" />
                  {classItem.currentEnrolled}/{classItem.maxStudents}
                </div>
              </TableCell>
              <TableCell>
                <span
                  className={`inline-flex items-center rounded-full px-2 py-1 text-xs font-medium ${
                    classItem.status === 'IN_PROGRESS'
                      ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300'
                      : 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300'
                  }`}
                >
                  {classItem.status === 'IN_PROGRESS' ? 'Đang học' : 'Sắp học'}
                </span>
              </TableCell>
              <TableCell>{classItem.startDate || 'N/A'}</TableCell>
              <TableCell className="text-right">
                <Link href={`/classes/${classItem.id}/attendance`}>
                  <Button size="sm">
                    <Clock className="mr-2 h-4 w-4" />
                    Điểm danh
                  </Button>
                </Link>
              </TableCell>
            </TableRow>
          ))
        ) : (
          <TableRow>
            <TableCell colSpan={6} className="py-12 text-center text-muted-foreground">
              <Calendar className="mx-auto h-12 w-12 opacity-20" />
              <p className="mt-4">Không có lớp học nào đang hoạt động</p>
            </TableCell>
          </TableRow>
        )}
      </TableBody>
    </Table>
  );
}
