/**
 * Attendance history table component.
 * Displays paginated attendance history with filters.
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

'use client';

import { DataTable } from '@/components/common/data-table';
import { getAttendanceHistoryColumns } from '@/components/tables/columns/attendance-columns';
import type { Attendance } from '@/types/attendance';

interface AttendanceHistoryTableProps {
  data: Attendance[];
  isLoading?: boolean;
  totalElements?: number;
  page?: number;
  size?: number;
  onPageChange?: (page: number) => void;
}

export function AttendanceHistoryTable({
  data,
  isLoading = false,
  totalElements = 0,
  page = 0,
  size = 20,
  onPageChange,
}: AttendanceHistoryTableProps) {
  const columns = getAttendanceHistoryColumns();

  if (isLoading) {
    return (
      <div className="rounded-lg border">
        <div className="p-8">
          <div className="space-y-3">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="h-12 animate-pulse rounded bg-muted" />
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className="rounded-lg border bg-card">
        <div className="flex flex-col items-center justify-center p-12 text-center">
          <div className="mb-4 text-6xl">📋</div>
          <h3 className="mb-2 text-lg font-semibold">
            Chưa có lịch sử điểm danh
          </h3>
          <p className="text-sm text-muted-foreground">
            Lịch sử điểm danh sẽ hiển thị tại đây khi có dữ liệu
          </p>
        </div>
      </div>
    );
  }

  const totalPages = Math.ceil(totalElements / size);

  return (
    <div className="space-y-4">
      <DataTable columns={columns} data={data} />

      {/* Pagination */}
      {totalPages > 1 && onPageChange && (
        <div data-testid="attendance-pagination" className="flex items-center justify-between">
          <div className="text-sm text-muted-foreground">
            Hiển thị {page * size + 1} - {page * size + data.length}{' '}
            trong tổng số {totalElements} bản ghi
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => onPageChange(page - 1)}
              disabled={page === 0}
              className="rounded-md border px-3 py-1 text-sm disabled:opacity-50"
            >
              Trước
            </button>
            <div className="flex items-center gap-1">
              {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                let pageNum = i;
                if (totalPages > 5) {
                  if (page < 3) {
                    pageNum = i;
                  } else if (page > totalPages - 3) {
                    pageNum = totalPages - 5 + i;
                  } else {
                    pageNum = page - 2 + i;
                  }
                }
                return (
                  <button
                    key={pageNum}
                    onClick={() => onPageChange(pageNum)}
                    className={`rounded-md px-3 py-1 text-sm ${
                      page === pageNum
                        ? 'bg-primary text-primary-foreground'
                        : 'border hover:bg-muted'
                    }`}
                  >
                    {pageNum + 1}
                  </button>
                );
              })}
            </div>
            <button
              onClick={() => onPageChange(page + 1)}
              disabled={page >= totalPages - 1}
              className="rounded-md border px-3 py-1 text-sm disabled:opacity-50"
            >
              Tiếp
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
