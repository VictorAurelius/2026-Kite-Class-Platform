/**
 * Edit class page.
 *
 * @author KiteClass Team
 * @since 3.7.0
 */

'use client';

export const dynamic = 'force-dynamic';

import { use } from 'react';
import { DashboardLayout } from '@/components/layout';
import { ClassForm } from '@/components/forms/class-form';
import { LoadingSpinner, ErrorAlert } from '@/components/common';
import { useClass, useUpdateClass } from '@/hooks/use-classes';
import type { UpdateClassRequest } from '@/types/class';

export default function EditClassPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const classId = parseInt(id);
  const { data: classData, isLoading, error } = useClass(classId);
  const updateMutation = useUpdateClass(classId);

  if (isLoading) {
    return (
      <DashboardLayout>
        <div className="flex justify-center py-12">
          <LoadingSpinner />
        </div>
      </DashboardLayout>
    );
  }

  if (error || !classData) {
    return (
      <DashboardLayout>
        <ErrorAlert title="Lỗi" message="Không tìm thấy lớp học" />
      </DashboardLayout>
    );
  }

  const handleSubmit = (data: UpdateClassRequest) => {
    updateMutation.mutate(data);
  };

  return (
    <DashboardLayout>
      <div className="mx-auto max-w-4xl space-y-6">
        <div>
          <h1 className="text-3xl font-bold">Chỉnh sửa lớp học</h1>
          <p className="text-muted-foreground">Cập nhật thông tin cho {classData.name}</p>
        </div>
        <div className="rounded-lg border bg-card p-6">
          <ClassForm
            initialData={{
              name: classData.name,
              description: classData.description || undefined,
              schedule: classData.schedule || undefined,
              locationType: classData.locationType,
              locationDetail: classData.locationDetail || undefined,
              startDate: classData.startDate || undefined,
              endDate: classData.endDate || undefined,
              maxStudents: classData.maxStudents,
            }}
            onSubmit={handleSubmit}
            isSubmitting={updateMutation.isPending}
            isEditing
            classStatus={classData.status}
          />
        </div>
      </div>
    </DashboardLayout>
  );
}
