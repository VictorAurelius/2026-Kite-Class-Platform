/**
 * Edit student page.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

export const dynamic = 'force-dynamic';

import { use } from 'react';
import { DashboardLayout } from '@/components/layout';
import { StudentForm } from '@/components/forms/student-form';
import { LoadingSpinner, ErrorAlert } from '@/components/common';
import { useStudent, useUpdateStudent } from '@/hooks/use-students';

export default function EditStudentPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const studentId = parseInt(id);
  const { data: student, isLoading, error } = useStudent(studentId);
  const updateMutation = useUpdateStudent(studentId);

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
        <div>
          <h1 className="text-3xl font-bold">Chỉnh sửa học viên</h1>
          <p className="text-muted-foreground">
            Cập nhật thông tin cho {student.name}
          </p>
        </div>

        <div className="rounded-lg border bg-card p-6">
          <StudentForm
            initialData={student}
            onSubmit={updateMutation.mutate}
            isSubmitting={updateMutation.isPending}
            isEditing
          />
        </div>
      </div>
    </DashboardLayout>
  );
}
