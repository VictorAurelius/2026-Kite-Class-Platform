/**
 * Edit course page.
 *
 * @author KiteClass Team
 * @since 3.6.0
 */

'use client';

export const dynamic = 'force-dynamic';

import { use } from 'react';
import { DashboardLayout } from '@/components/layout';
import { CourseForm } from '@/components/forms/course-form';
import { LoadingSpinner, ErrorAlert } from '@/components/common';
import { useCourse, useUpdateCourse } from '@/hooks/use-courses';
import type { UpdateCourseRequest } from '@/types/course';

export default function EditCoursePage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const courseId = parseInt(id);
  const { data: course, isLoading, error } = useCourse(courseId);
  const updateMutation = useUpdateCourse(courseId);

  if (isLoading) {
    return (
      <DashboardLayout>
        <div className="flex justify-center py-12"><LoadingSpinner /></div>
      </DashboardLayout>
    );
  }

  if (error || !course) {
    return (
      <DashboardLayout>
        <ErrorAlert title="Lỗi" message="Không tìm thấy khóa học" />
      </DashboardLayout>
    );
  }

  const handleSubmit = (data: UpdateCourseRequest) => {
    updateMutation.mutate(data);
  };

  return (
    <DashboardLayout>
      <div className="mx-auto max-w-4xl space-y-6">
        <div>
          <h1 className="text-3xl font-bold">Chỉnh sửa khóa học</h1>
          <p className="text-muted-foreground">Cập nhật thông tin cho {course.name}</p>
        </div>
        <div className="rounded-lg border bg-card p-6">
          <CourseForm
            initialData={{
              name: course.name,
              code: course.code,
              teacherId: course.teacherId,
              description: course.description,
              syllabus: course.syllabus,
              objectives: course.objectives,
              prerequisites: course.prerequisites,
              targetAudience: course.targetAudience,
              durationWeeks: course.durationWeeks,
              totalSessions: course.totalSessions,
              price: course.price,
              coverImageUrl: course.coverImageUrl,
            }}
            onSubmit={handleSubmit}
            isSubmitting={updateMutation.isPending}
            isEditing
            courseStatus={course.status}
          />
        </div>
      </div>
    </DashboardLayout>
  );
}
