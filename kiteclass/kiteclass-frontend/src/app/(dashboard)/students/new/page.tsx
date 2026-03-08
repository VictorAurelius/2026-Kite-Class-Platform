/**
 * Create student page.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

export const dynamic = 'force-dynamic';

import { DashboardLayout } from '@/components/layout';
import { StudentForm } from '@/components/forms/student-form';
import { useCreateStudent } from '@/hooks/use-students';
import type { CreateStudentRequest, UpdateStudentRequest } from '@/types/student';

export default function NewStudentPage() {
  const createMutation = useCreateStudent();

  const handleSubmit = (data: CreateStudentRequest | UpdateStudentRequest) => {
    createMutation.mutate(data as CreateStudentRequest);
  };

  return (
    <DashboardLayout>
      <div className="mx-auto max-w-4xl space-y-6">
        <div>
          <h1 className="text-3xl font-bold">Thêm học viên mới</h1>
          <p className="text-muted-foreground">
            Nhập thông tin học viên để tạo hồ sơ mới
          </p>
        </div>

        <div className="rounded-lg border bg-card p-6">
          <StudentForm
            onSubmit={handleSubmit}
            isSubmitting={createMutation.isPending}
          />
        </div>
      </div>
    </DashboardLayout>
  );
}
