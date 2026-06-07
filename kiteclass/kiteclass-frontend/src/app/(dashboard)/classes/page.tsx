/**
 * Classes list page with course selector.
 *
 * @author KiteClass Team
 * @since 3.7.0
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useCourses } from '@/hooks/use-courses';
import { useClasses, useDeleteClass } from '@/hooks/use-classes';
import { getClassColumns } from '@/components/tables/columns/class-columns';
import type { ClassSearchCriteria } from '@/types/class';

export default function ClassesPage() {
  const [selectedCourseId, setSelectedCourseId] = useState<number | null>(null);
  const [searchParams, setSearchParams] = useState<Omit<ClassSearchCriteria, 'courseId'>>({
    page: 0,
    size: 20,
  });

  // Fetch courses for the selector
  const { data: coursesData } = useCourses({ page: 0, size: 100 });

  // Fetch classes for selected course
  const {
    data: classesData,
    isLoading,
    error,
  } = useClasses(selectedCourseId!, searchParams);

  const deleteMutation = useDeleteClass();

  const handleCourseChange = (value: string) => {
    setSelectedCourseId(parseInt(value));
    setSearchParams({ page: 0, size: 20 }); // Reset pagination
  };

  const handleSearch = (query: string) => {
    setSearchParams((prev) => ({ ...prev, search: query, page: 0 }));
  };

  const handleDelete = (id: number) => {
    if (
      window.confirm(
        'Bạn có chắc chắn muốn xóa lớp học này? Chỉ lớp SCHEDULED với 0 học viên mới có thể xóa.'
      )
    ) {
      deleteMutation.mutate(id);
    }
  };

  const columns = getClassColumns(handleDelete);

  return (
    <DashboardLayout>
      {/* Wave 30 Bucket B — kiteclass-pro-v2 token application: refined header
          spacing + tracking-tight title per design system tokens. */}
      <div className="space-y-6">
        <div className="flex flex-col gap-4 border-b pb-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-1">
            <h1 className="text-3xl font-semibold tracking-tight">Lớp học</h1>
            <p className="text-sm text-muted-foreground">
              Quản lý danh sách lớp học theo từng khóa học
            </p>
          </div>
          {selectedCourseId && (
            <Link href={`/courses/${selectedCourseId}/classes/new`}>
              <Button className="w-full sm:w-auto">
                <Plus className="mr-2 h-4 w-4" />
                Thêm lớp học
              </Button>
            </Link>
          )}
        </div>

        {/* Course Selector */}
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
          <div className="w-full sm:w-80">
            <Select onValueChange={handleCourseChange}>
              <SelectTrigger>
                <SelectValue placeholder="Chọn khóa học..." />
              </SelectTrigger>
              <SelectContent>
                {coursesData?.content.map((course) => (
                  <SelectItem key={course.id} value={course.id.toString()}>
                    {course.name} ({course.code})
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {selectedCourseId && (
            <div className="max-w-md flex-1">
              <SearchInput
                placeholder="Tìm kiếm theo tên lớp, mã lớp..."
                onSearch={handleSearch}
              />
            </div>
          )}
        </div>

        {!selectedCourseId && (
          <div className="rounded-lg border border-dashed p-12 text-center">
            <p className="text-muted-foreground">
              Vui lòng chọn khóa học để xem danh sách lớp học
            </p>
          </div>
        )}

        {selectedCourseId && isLoading && (
          <div className="flex justify-center py-12">
            <LoadingSpinner />
          </div>
        )}

        {selectedCourseId && error && (
          <ErrorAlert
            title="Lỗi tải dữ liệu"
            message="Không thể tải danh sách lớp học. Vui lòng thử lại."
          />
        )}

        {selectedCourseId && classesData && (
          <>
            {classesData.content.length === 0 ? (
              <div className="rounded-lg border border-dashed p-12 text-center">
                <p className="text-muted-foreground">
                  Chưa có lớp học nào cho khóa học này
                </p>
                <Link href={`/courses/${selectedCourseId}/classes/new`}>
                  <Button className="mt-4">
                    <Plus className="mr-2 h-4 w-4" />
                    Tạo lớp học đầu tiên
                  </Button>
                </Link>
              </div>
            ) : (
              <DataTable
                columns={columns}
                data={classesData.content}
                pageCount={classesData.totalPages}
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
          </>
        )}
      </div>
    </DashboardLayout>
  );
}
