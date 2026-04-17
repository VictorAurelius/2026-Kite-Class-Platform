/**
 * Create new teacher page.
 *
 * @author KiteClass Team
 * @since 3.5.0
 */

'use client';

import { DashboardLayout } from '@/components/layout';
import { TeacherForm } from '@/components/forms/teacher-form';
import { useCreateTeacher } from '@/hooks/use-teachers';
import type { CreateTeacherRequest, UpdateTeacherRequest } from '@/types/teacher';

export default function NewTeacherPage() {
  const createMutation = useCreateTeacher();

  const handleSubmit = (data: CreateTeacherRequest | UpdateTeacherRequest) => {
    createMutation.mutate(data as CreateTeacherRequest);
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold">Thêm giáo viên</h1>
          <p className="text-muted-foreground">
            Nhập thông tin giáo viên mới
          </p>
        </div>

        {createMutation.isError && (
          <div className="rounded-lg border border-destructive/50 bg-destructive/10 p-4">
            <p className="text-sm text-destructive">
              Không thể tạo giáo viên. Vui lòng kiểm tra lại thông tin và thử lại.
            </p>
          </div>
        )}

        <div className="rounded-lg border bg-card p-6">
          <TeacherForm
            onSubmit={handleSubmit}
            isSubmitting={createMutation.isPending}
          />
        </div>
      </div>
    </DashboardLayout>
  );
}
