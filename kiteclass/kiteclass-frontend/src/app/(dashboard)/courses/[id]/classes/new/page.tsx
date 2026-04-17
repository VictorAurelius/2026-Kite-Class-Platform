/**
 * Create new class page (within a course context).
 *
 * @author KiteClass Team
 * @since 3.7.0
 */

'use client';

import { use } from 'react';
import { DashboardLayout } from '@/components/layout';
import { ClassForm } from '@/components/forms/class-form';
import { LoadingSpinner, ErrorAlert } from '@/components/common';
import { useCreateClass } from '@/hooks/use-classes';
import { useCourse } from '@/hooks/use-courses';
import type { CreateClassRequest, UpdateClassRequest } from '@/types/class';

export default function NewClassPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const courseId = parseInt(id);

  const { data: course, isLoading: courseLoading, error: courseError } = useCourse(courseId);
  const createMutation = useCreateClass(courseId);

  const handleSubmit = (data: CreateClassRequest | UpdateClassRequest) => {
    createMutation.mutate(data as CreateClassRequest);
  };

  if (courseLoading) {
    return (
      <DashboardLayout>
        <div className="flex justify-center py-12">
          <LoadingSpinner />
        </div>
      </DashboardLayout>
    );
  }

  if (courseError || !course) {
    return (
      <DashboardLayout>
        <ErrorAlert title="Lỗi" message="Không tìm thấy khóa học" backHref="/courses" backLabel="Quay lại danh sách khóa học" />
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold">Thêm lớp học</h1>
          <p className="text-muted-foreground">
            Tạo lớp học mới cho khóa học: <span className="font-medium">{course.name}</span>
          </p>
        </div>
        <div className="rounded-lg border bg-card p-6">
          <ClassForm
            onSubmit={handleSubmit}
            isSubmitting={createMutation.isPending}
            initialData={{
              locationType: 'IN_PERSON',
              maxStudents: 30,
            }}
          />
        </div>
      </div>
    </DashboardLayout>
  );
}
