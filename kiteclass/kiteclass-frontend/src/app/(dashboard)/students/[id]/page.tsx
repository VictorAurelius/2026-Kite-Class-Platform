/**
 * Student detail page.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

export const dynamic = 'force-dynamic';

import { use } from 'react';
import { Pencil, Trash2 } from 'lucide-react';
import Link from 'next/link';
import { DashboardLayout } from '@/components/layout';
import { StatusBadge, LoadingSpinner, ErrorAlert } from '@/components/common';
import { Button } from '@/components/ui/button';
import { useStudent, useDeleteStudent } from '@/hooks/use-students';
import { StudentStatus } from '@/types/auth';
import { useRouter } from 'next/navigation';

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

export default function StudentDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const router = useRouter();
  const { data: student, isLoading, error } = useStudent(parseInt(id));
  const deleteMutation = useDeleteStudent();

  const handleDelete = () => {
    if (window.confirm('Bạn có chắc chắn muốn xóa học viên này? Thao tác này không thể hoàn tác.')) {
      deleteMutation.mutate(parseInt(id), {
        onSuccess: () => router.push('/students'),
      });
    }
  };

  if (isLoading) {
    return (
      <DashboardLayout>
        <div className="flex justify-center py-12">
          <LoadingSpinner />
        </div>
      </DashboardLayout>
    );
  }

  if (error || !student) {
    return (
      <DashboardLayout>
        <ErrorAlert
          title="Lỗi"
          message="Không tìm thấy thông tin học viên"
        />
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="mx-auto max-w-4xl space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold">{student.name}</h1>
            <p className="text-muted-foreground">Thông tin chi tiết học viên</p>
          </div>
          <div className="flex gap-2">
            <Link href={`/students/${id}/edit`}>
              <Button variant="outline">
                <Pencil className="mr-2 h-4 w-4" />
                Chỉnh sửa
              </Button>
            </Link>
            <Button variant="destructive" onClick={handleDelete}>
              <Trash2 className="mr-2 h-4 w-4" />
              Xóa
            </Button>
          </div>
        </div>

        <div className="rounded-lg border bg-card">
          <div className="grid gap-6 p-6">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-muted-foreground">
                  Trạng thái
                </p>
                <StatusBadge
                  status={statusLabels[student.status]}
                  variant={statusVariants[student.status]}
                  className="mt-1"
                />
              </div>
              <div>
                <p className="text-sm font-medium text-muted-foreground">
                  Email
                </p>
                <p className="mt-1">{student.email}</p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-muted-foreground">
                  Số điện thoại
                </p>
                <p className="mt-1">{student.phone || '—'}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-muted-foreground">
                  Ngày sinh
                </p>
                <p className="mt-1">
                  {student.dateOfBirth
                    ? new Date(student.dateOfBirth).toLocaleDateString('vi-VN')
                    : '—'}
                </p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-muted-foreground">
                  Giới tính
                </p>
                <p className="mt-1">
                  {student.gender === 'MALE'
                    ? 'Nam'
                    : student.gender === 'FEMALE'
                    ? 'Nữ'
                    : student.gender === 'OTHER'
                    ? 'Khác'
                    : '—'}
                </p>
              </div>
              <div>
                <p className="text-sm font-medium text-muted-foreground">
                  Ngày nhập học
                </p>
                <p className="mt-1">
                  {student.enrollmentDate
                    ? new Date(student.enrollmentDate).toLocaleDateString(
                        'vi-VN'
                      )
                    : '—'}
                </p>
              </div>
            </div>

            {student.address && (
              <div>
                <p className="text-sm font-medium text-muted-foreground">
                  Địa chỉ
                </p>
                <p className="mt-1">{student.address}</p>
              </div>
            )}

            <div className="grid grid-cols-2 gap-4 border-t pt-4">
              <div>
                <p className="text-sm font-medium text-muted-foreground">
                  Ngày tạo
                </p>
                <p className="mt-1">
                  {new Date(student.createdAt).toLocaleString('vi-VN')}
                </p>
              </div>
              <div>
                <p className="text-sm font-medium text-muted-foreground">
                  Cập nhật lần cuối
                </p>
                <p className="mt-1">
                  {new Date(student.updatedAt).toLocaleString('vi-VN')}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}
