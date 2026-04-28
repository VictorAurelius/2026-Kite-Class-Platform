/**
 * Create new course page.
 *
 * @author KiteClass Team
 * @since 3.6.0
 */

'use client';

import { DashboardLayout } from '@/components/layout';
import { CourseForm } from '@/components/forms/dynamic-course-form';
import { useCreateCourse } from '@/hooks/use-courses';
import type { CreateCourseRequest, UpdateCourseRequest } from '@/types/course';

export default function NewCoursePage() {
  const createMutation = useCreateCourse();

  const handleSubmit = (data: CreateCourseRequest | UpdateCourseRequest) => {
    createMutation.mutate(data as CreateCourseRequest);
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold">Thêm khóa học</h1>
          <p className="text-muted-foreground">Tạo khóa học mới cho trung tâm</p>
        </div>
        {createMutation.isError && (
          <div className="rounded-lg border border-destructive/50 bg-destructive/10 p-4">
            <p className="text-sm text-destructive">
              Không thể tạo khóa học. Vui lòng kiểm tra lại thông tin và thử lại.
            </p>
          </div>
        )}

        <div className="rounded-lg border bg-card p-6">
          <CourseForm
            onSubmit={handleSubmit}
            isSubmitting={createMutation.isPending}
          />
        </div>
      </div>
    </DashboardLayout>
  );
}
