/**
 * Teachers list page.
 *
 * Wave 30 Bucket C — Phase 4 KC pro v2 port (GAP-266):
 *  - Apply kiteclass-pro-v2 design tokens via existing shadcn semantic classes
 *  - Wire @kite/shared-ui `BulkActionsBar` (G12, Wave 29 Bucket D) for the
 *    4 supported bulk actions (EXPORT_CSV / ARCHIVE / ASSIGN / DELETE).
 *  - Destructive `DELETE` action is gated by D1 ConfirmDialog (consumed
 *    internally by `BulkActionsBar`).
 *
 * @author KiteClass Team
 * @since 3.5.0
 */

'use client';

export const dynamic = 'force-dynamic';

import { useCallback, useMemo, useState } from 'react';
import { Plus, GraduationCap } from 'lucide-react';
import Link from 'next/link';
import { BulkActionsBar, type BulkAction } from '@kite/shared-ui';
import type { ColumnDef } from '@tanstack/react-table';
import { DashboardLayout } from '@/components/layout';
import { SearchInput, LoadingSpinner, ErrorAlert } from '@/components/common';
import { DataTable } from '@/components/common/dynamic-data-table';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { useTeachers, useDeleteTeacher } from '@/hooks/use-teachers';
import { getTeacherColumns } from '@/components/tables/columns/teacher-columns';
import type { Teacher, TeacherSearchParams } from '@/types/teacher';

export default function TeachersPage() {
  const [searchParams, setSearchParams] = useState<TeacherSearchParams>({
    page: 0,
    size: 20,
  });
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());

  const { data, isLoading, error } = useTeachers(searchParams);
  const deleteMutation = useDeleteTeacher();

  const handleSearch = (query: string) => {
    setSearchParams((prev) => ({ ...prev, query, page: 0 }));
    setSelectedIds(new Set());
  };

  const handleDelete = (id: number) => {
    if (window.confirm('Bạn có chắc chắn muốn xóa giáo viên này? Thao tác này không thể hoàn tác.')) {
      deleteMutation.mutate(id);
    }
  };

  const toggleRow = useCallback((id: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }, []);

  const toggleAll = useCallback((rows: Teacher[], checked: boolean) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (checked) {
        rows.forEach((r) => next.add(r.id));
      } else {
        rows.forEach((r) => next.delete(r.id));
      }
      return next;
    });
  }, []);

  const clearSelection = useCallback(() => {
    setSelectedIds(new Set());
  }, []);

  const handleBulkAction = useCallback(
    (action: BulkAction) => {
      const ids = Array.from(selectedIds);
      if (ids.length === 0) return;
      switch (action) {
        case 'DELETE':
          ids.forEach((id) => deleteMutation.mutate(id));
          setSelectedIds(new Set());
          break;
        case 'EXPORT_CSV':
        case 'ARCHIVE':
        case 'ASSIGN':
          // eslint-disable-next-line no-console
          console.info(`[Wave 30 Bucket C] Bulk action ${action} for teachers ids:`, ids);
          break;
      }
    },
    [selectedIds, deleteMutation],
  );

  const baseColumns = useMemo(() => getTeacherColumns(handleDelete), []);

  const columns: ColumnDef<Teacher>[] = useMemo(() => {
    const rows = data?.content ?? [];
    const allChecked =
      rows.length > 0 && rows.every((r) => selectedIds.has(r.id));
    return [
      {
        id: 'select',
        size: 32,
        header: () => (
          <Checkbox
            aria-label="Chọn tất cả giáo viên trong trang"
            checked={allChecked}
            onCheckedChange={(value) => toggleAll(rows, Boolean(value))}
          />
        ),
        cell: ({ row }) => (
          <Checkbox
            aria-label={`Chọn giáo viên ${row.original.name}`}
            checked={selectedIds.has(row.original.id)}
            onCheckedChange={() => toggleRow(row.original.id)}
          />
        ),
      },
      ...baseColumns,
    ];
  }, [baseColumns, data?.content, selectedIds, toggleAll, toggleRow]);

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-3xl font-bold">Giáo viên</h1>
            <p className="text-muted-foreground">
              Quản lý danh sách giáo viên của trung tâm
            </p>
          </div>
          <Link href="/teachers/new">
            <Button className="w-full sm:w-auto">
              <Plus className="mr-2 h-4 w-4" />
              Thêm giáo viên
            </Button>
          </Link>
        </div>

        <div className="flex items-center gap-4">
          <div className="w-full max-w-md">
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

        {!isLoading && !error && !data && (
          <div className="flex flex-col items-center justify-center py-12 text-center">
            <GraduationCap className="mb-4 h-12 w-12 text-muted-foreground" />
            <p className="text-lg font-medium">Chưa có giáo viên nào</p>
            <p className="text-sm text-muted-foreground mt-1">Thêm giáo viên mới để bắt đầu.</p>
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

        {selectedIds.size > 0 && (
          <BulkActionsBar
            selectedCount={selectedIds.size}
            onAction={handleBulkAction}
            onClearSelection={clearSelection}
            sticky="bottom"
          />
        )}
      </div>
    </DashboardLayout>
  );
}
