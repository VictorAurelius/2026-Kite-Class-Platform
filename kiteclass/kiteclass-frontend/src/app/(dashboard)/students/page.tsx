/**
 * Students list page.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

export const dynamic = 'force-dynamic';

import { useState } from 'react';
import { Plus, Users } from 'lucide-react';
import Link from 'next/link';
import { DashboardLayout } from '@/components/layout';
import { SearchInput, LoadingSpinner, ErrorAlert } from '@/components/common';
import { DataTable } from '@/components/common/dynamic-data-table';
import { Button } from '@/components/ui/button';
import { useStudents, useDeleteStudent } from '@/hooks/use-students';
import { getStudentColumns } from '@/components/tables/columns/student-columns';
import type { StudentSearchParams } from '@/types/student';

export default function StudentsPage() {
  const [searchParams, setSearchParams] = useState<StudentSearchParams>({
    page: 0,
    size: 20,
  });

  const { data, isLoading, error } = useStudents(searchParams);
  const deleteMutation = useDeleteStudent();

  const handleSearch = (query: string) => {
    setSearchParams((prev) => ({ ...prev, query, page: 0 }));
  };

  const handleDelete = (id: number) => {
    if (window.confirm('Bạn có chắc chắn muốn xóa học viên này? Thao tác này không thể hoàn tác.')) {
      deleteMutation.mutate(id);
    }
  };

  const columns = getStudentColumns(handleDelete);

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold">Học viên</h1>
            <p className="text-muted-foreground">
              Quản lý danh sách học viên của trung tâm
            </p>
          </div>
          <Link href="/students/new">
            <Button>
              <Plus className="mr-2 h-4 w-4" />
              Thêm học viên
            </Button>
          </Link>
        </div>

        <div className="flex items-center gap-4">
          <div className="max-w-md">
            <SearchInput
              placeholder="Tìm kiếm theo tên, email..."
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
            message="Không thể tải danh sách học viên. Vui lòng thử lại."
          />
        )}

        {!isLoading && !error && !data && (
          <div className="flex flex-col items-center justify-center py-12 text-center">
            <Users className="mb-4 h-12 w-12 text-muted-foreground" />
            <p className="text-lg font-medium">Chưa có học viên nào</p>
            <p className="text-sm text-muted-foreground mt-1">Thêm học viên mới để bắt đầu.</p>
          </div>
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
