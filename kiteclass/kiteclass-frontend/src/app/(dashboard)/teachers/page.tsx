/**
 * Teachers list page.
 *
 * @author KiteClass Team
 * @since 3.5.0
 */

'use client';

export const dynamic = 'force-dynamic';

import { useState } from 'react';
import { Plus } from 'lucide-react';
import Link from 'next/link';
import { DashboardLayout } from '@/components/layout';
import { DataTable, SearchInput, LoadingSpinner, ErrorAlert } from '@/components/common';
import { Button } from '@/components/ui/button';
import { useTeachers, useDeleteTeacher } from '@/hooks/use-teachers';
import { getTeacherColumns } from '@/components/tables/columns/teacher-columns';
import type { TeacherSearchParams } from '@/types/teacher';

export default function TeachersPage() {
  const [searchParams, setSearchParams] = useState<TeacherSearchParams>({
    page: 0,
    size: 20,
  });

  const { data, isLoading, error } = useTeachers(searchParams);
  const deleteMutation = useDeleteTeacher();

  const handleSearch = (query: string) => {
    setSearchParams((prev) => ({ ...prev, query, page: 0 }));
  };

  const handleDelete = (id: number) => {
    if (window.confirm('Bạn có chắc chắn muốn xóa giáo viên này? Thao tác này không thể hoàn tác.')) {
      deleteMutation.mutate(id);
    }
  };

  const columns = getTeacherColumns(handleDelete);

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold">Giáo viên</h1>
            <p className="text-muted-foreground">
              Quản lý danh sách giáo viên của trung tâm
            </p>
          </div>
          <Link href="/teachers/new">
            <Button>
              <Plus className="mr-2 h-4 w-4" />
              Thêm giáo viên
            </Button>
          </Link>
        </div>

        <div className="flex items-center gap-4">
          <div className="max-w-md">
            <SearchInput
              placeholder="Tìm kiếm theo tên, email, chuyên môn..."
              onSearch={handleSearch}
            />
          </div>
        </div>

        {isLoading && (
          <div className="flex justify-center py-12">
            <LoadingSpinner />
          </div>
        )}

        {error && (
          <ErrorAlert
            title="Lỗi tải dữ liệu"
            message="Không thể tải danh sách giáo viên. Vui lòng thử lại."
          />
        )}

        {data && (
          <DataTable
            columns={columns}
            data={data.content}
            pageCount={data.totalPages}
            pageSize={searchParams.size || 20}
            onPaginationChange={(pagination) =>
              setSearchParams((prev) => ({
                ...prev,
                page: pagination.pageIndex,
                size: pagination.pageSize,
              }))
            }
          />
        )}
      </div>
    </DashboardLayout>
  );
}
