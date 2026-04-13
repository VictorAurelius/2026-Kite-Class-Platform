/**
 * Edit teacher page.
 *
 * @author KiteClass Team
 * @since 3.5.0
 */

'use client';

export const dynamic = 'force-dynamic';

import { use } from 'react';
import { DashboardLayout } from '@/components/layout';
import { TeacherForm } from '@/components/forms/teacher-form';
import { LoadingSpinner, ErrorAlert } from '@/components/common';
import { useTeacher, useUpdateTeacher } from '@/hooks/use-teachers';
import type { UpdateTeacherRequest } from '@/types/teacher';

export default function EditTeacherPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const teacherId = parseInt(id);
  const { data: teacher, isLoading, error } = useTeacher(teacherId);
  const updateMutation = useUpdateTeacher(teacherId);

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

  const handleSubmit = (data: UpdateTeacherRequest) => {
    updateMutation.mutate(data);
  };

  return (
    <DashboardLayout>
      <div className="mx-auto max-w-4xl space-y-6">
        <div>
          <h1 className="text-3xl font-bold">Chỉnh sửa giáo viên</h1>
          <p className="text-muted-foreground">
            Cập nhật thông tin cho {teacher.name}
          </p>
        </div>

        <div className="rounded-lg border bg-card p-6">
          <TeacherForm
            initialData={{
              name: teacher.name,
              email: teacher.email,
              phoneNumber: teacher.phoneNumber,
              specialization: teacher.specialization,
              bio: teacher.bio,
              qualification: teacher.qualification,
              experienceYears: teacher.experienceYears,
              status: teacher.status,
            }}
            onSubmit={handleSubmit}
            isSubmitting={updateMutation.isPending}
            isEditing
          />
        </div>
      </div>
    </DashboardLayout>
  );
}
