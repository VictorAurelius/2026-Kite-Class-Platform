/**
 * Create new class page (within a course context).
 *
 * GAP-290 Wave 18a — adds optional "Lặp lại" toggle that, after class creation,
 * generates sessions from an RFC 5545 RRULE subset (WEEKLY only Phase 1).
 *
 * @author KiteClass Team
 * @since 3.7.0 (Wave 18a extension 2026-05-04)
 */

'use client';

import { use, useState } from 'react';
import { DashboardLayout } from '@/components/layout';
import { ClassForm } from '@/components/forms/dynamic-class-form';
import { RecurrenceForm } from '@/components/forms/recurrence-form';
import { LoadingSpinner, ErrorAlert } from '@/components/common';
import {
  useCreateClass,
  useGenerateSessionsFromRecurrence,
} from '@/hooks/use-classes';
import { useCourse } from '@/hooks/use-courses';
import type {
  CreateClassRequest,
  UpdateClassRequest,
  RecurrenceRule,
} from '@/types/class';

export default function NewClassPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const courseId = parseInt(id);

  const { data: course, isLoading: courseLoading, error: courseError } = useCourse(courseId);
  const createMutation = useCreateClass(courseId);
  const generateRecurrence = useGenerateSessionsFromRecurrence();

  const [recurrenceEnabled, setRecurrenceEnabled] = useState(false);
  const [pendingRecurrence, setPendingRecurrence] = useState<RecurrenceRule | null>(null);

  const handleSubmit = (data: CreateClassRequest | UpdateClassRequest) => {
    createMutation.mutate(data as CreateClassRequest, {
      onSuccess: (created) => {
        if (recurrenceEnabled && pendingRecurrence) {
          generateRecurrence.mutate({ id: created.id, rule: pendingRecurrence });
        }
      },
    });
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
            isSubmitting={createMutation.isPending || generateRecurrence.isPending}
            initialData={{
              locationType: 'IN_PERSON',
              maxStudents: 30,
            }}
          />
        </div>

        {/* GAP-290 Wave 18a: optional recurrence panel */}
        <div className="rounded-lg border bg-card p-6">
          <label className="flex items-center gap-3 mb-4">
            <input
              type="checkbox"
              checked={recurrenceEnabled}
              onChange={(e) => setRecurrenceEnabled(e.target.checked)}
              data-testid="toggle-recurrence"
              className="h-4 w-4"
            />
            <span className="font-medium">Lặp lại theo lịch (tuần)</span>
            <span className="text-xs text-muted-foreground">
              Tự động tạo nhiều buổi học cho lớp này
            </span>
          </label>

          {recurrenceEnabled && (
            <RecurrenceForm
              onSubmit={(rule) => setPendingRecurrence(rule)}
              isSubmitting={generateRecurrence.isPending}
            />
          )}

          {pendingRecurrence && (
            <p className="mt-3 text-sm text-muted-foreground">
              Quy tắc đã sẵn sàng — sẽ áp dụng sau khi tạo lớp học.
            </p>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
