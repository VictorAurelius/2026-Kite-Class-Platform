/**
 * Courses list page.
 *
 * @author KiteClass Team
 * @since 3.6.0
 */

'use client';

export const dynamic = 'force-dynamic';

import { useState } from 'react';
import { Plus } from 'lucide-react';
import Link from 'next/link';
import { DashboardLayout } from '@/components/layout';
import { SearchInput, LoadingSpinner, ErrorAlert } from '@/components/common';
import { DataTable } from '@/components/common/dynamic-data-table';
import { Button } from '@/components/ui/button';
import { useCourses, useDeleteCourse } from '@/hooks/use-courses';
import { getCourseColumns } from '@/components/tables/columns/course-columns';
import type { CourseSearchParams } from '@/types/course';

export default function CoursesPage() {
  const [searchParams, setSearchParams] = useState<CourseSearchParams>({
    page: 0,
    size: 20,
  });

  const { data, isLoading, error } = useCourses(searchParams);
  const deleteMutation = useDeleteCourse();

  const handleSearch = (query: string) => {
    setSearchParams((prev) => ({ ...prev, query, page: 0 }));
  };

  const handleDelete = (id: number) => {
    if (window.confirm('Bạn có chắc chắn muốn xóa khóa học này?')) {
      deleteMutation.mutate(id);
    }
  };

  const columns = getCourseColumns(handleDelete);

  return (
    <DashboardLayout>
      {/* Wave 30 Bucket B — kiteclass-pro-v2 token application: refined header
          spacing + tracking-tight title per design system tokens. */}
      <div className="space-y-6">
        <div className="flex flex-col gap-4 border-b pb-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-1">
            <h1 className="text-3xl font-semibold tracking-tight">Khóa học</h1>
            <p className="text-sm text-muted-foreground">
              Quản lý danh sách khóa học của trung tâm
            </p>
          </div>
          <Link href="/courses/new">
            <Button className="w-full sm:w-auto">
              <Plus className="mr-2 h-4 w-4" />
              Thêm khóa học
            </Button>
          </Link>
        </div>

        <div className="flex items-center gap-4">
          <div className="w-full max-w-md">
            <SearchInput
              placeholder="Tìm kiếm theo tên, mã khóa học..."
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
            message="Không thể tải danh sách khóa học. Vui lòng thử lại."
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
