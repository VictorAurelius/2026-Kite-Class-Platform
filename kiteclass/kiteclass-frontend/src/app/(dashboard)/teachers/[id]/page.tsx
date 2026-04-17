/**
 * Teacher detail page.
 *
 * @author KiteClass Team
 * @since 3.5.0
 */

'use client';

export const dynamic = 'force-dynamic';

import { use } from 'react';
import { Pencil, Trash2 } from 'lucide-react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { DashboardLayout } from '@/components/layout';
import { StatusBadge, LoadingSpinner, ErrorAlert } from '@/components/common';
import { Button } from '@/components/ui/button';
import { useTeacher, useDeleteTeacher } from '@/hooks/use-teachers';
import { TeacherStatus } from '@/types/auth';

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

export default function TeacherDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const router = useRouter();
  const { data: teacher, isLoading, error } = useTeacher(parseInt(id));
  const deleteMutation = useDeleteTeacher();

  const handleDelete = () => {
    if (window.confirm('Bạn có chắc chắn muốn xóa giáo viên này? Thao tác này không thể hoàn tác.')) {
      deleteMutation.mutate(parseInt(id), {
        onSuccess: () => router.push('/teachers'),
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

  if (error || !teacher) {
    return (
      <DashboardLayout>
        <ErrorAlert title="Lỗi" message="Không tìm thấy thông tin giáo viên" backHref="/teachers" backLabel="Quay lại danh sách giáo viên" />
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="mx-auto max-w-4xl space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold">{teacher.name}</h1>
            <p className="text-muted-foreground">Thông tin chi tiết giáo viên</p>
          </div>
          <div className="flex gap-2">
            <Link href={`/teachers/${id}/edit`}>
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
                <p className="text-sm font-medium text-muted-foreground">Trạng thái</p>
                <StatusBadge
                  status={statusLabels[teacher.status]}
                  variant={statusVariants[teacher.status]}
                  className="mt-1"
                />
              </div>
              <div>
                <p className="text-sm font-medium text-muted-foreground">Email</p>
                <p className="mt-1">{teacher.email}</p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-muted-foreground">Số điện thoại</p>
                <p className="mt-1">{teacher.phoneNumber || '—'}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-muted-foreground">Chuyên môn</p>
                <p className="mt-1">{teacher.specialization || '—'}</p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-muted-foreground">Bằng cấp / Chứng chỉ</p>
                <p className="mt-1">{teacher.qualification || '—'}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-muted-foreground">Kinh nghiệm</p>
                <p className="mt-1">
                  {teacher.experienceYears != null
                    ? `${teacher.experienceYears} năm`
                    : '—'}
                </p>
              </div>
            </div>

            {teacher.bio && (
              <div>
                <p className="text-sm font-medium text-muted-foreground">Giới thiệu</p>
                <p className="mt-1 whitespace-pre-line">{teacher.bio}</p>
              </div>
            )}

            <div className="grid grid-cols-2 gap-4 border-t pt-4">
              <div>
                <p className="text-sm font-medium text-muted-foreground">Ngày tạo</p>
                <p className="mt-1">{new Date(teacher.createdAt).toLocaleString('vi-VN')}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-muted-foreground">Cập nhật lần cuối</p>
                <p className="mt-1">{new Date(teacher.updatedAt).toLocaleString('vi-VN')}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}
