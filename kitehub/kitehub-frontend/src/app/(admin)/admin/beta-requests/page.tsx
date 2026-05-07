/**
 * /admin/beta-requests — coordinator dashboard cho Phase 1 BETA invite queue (GAP-372).
 *
 * Lists requests by status (PENDING by default), inline approve/reject actions,
 * paginated. Approve triggers Outbox publish → invite email per BetaAccessService.
 *
 * @since Wave 33 — GAP-372
 */
'use client';

import { useEffect, useState, useCallback } from 'react';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';

type BetaStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'SIGNED_UP';

interface BetaRequest {
  id: number;
  email: string;
  name: string;
  orgName: string;
  persona: string;
  referralSource: string | null;
  status: BetaStatus;
  createdAt: string;
  approvedAt: string | null;
  rejectedAt: string | null;
  rejectionReason: string | null;
}

interface BetaRequestPageDto {
  content: BetaRequest[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

const STATUS_LABEL: Record<BetaStatus, string> = {
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Đã từ chối',
  SIGNED_UP: 'Đã đăng ký',
};

export default function AdminBetaRequestsPage() {
  const [statusFilter, setStatusFilter] = useState<BetaStatus>('PENDING');
  const [data, setData] = useState<BetaRequestPageDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actingOn, setActingOn] = useState<number | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const resp = await apiClient.get(endpoints.betaRequests.list, {
        params: { status: statusFilter, page: 0, size: 50 },
      });
      setData(resp.data as BetaRequestPageDto);
    } catch {
      setError('Không tải được danh sách yêu cầu beta.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const onApprove = async (id: number) => {
    setActingOn(id);
    try {
      await apiClient.post(endpoints.betaRequests.approve(id), {
        approverId: 'coordinator',
      });
      await fetchData();
    } catch {
      setError(`Duyệt yêu cầu #${id} thất bại.`);
    } finally {
      setActingOn(null);
    }
  };

  const onReject = async (id: number) => {
    const reason = window.prompt('Lý do từ chối:');
    if (!reason || !reason.trim()) return;
    setActingOn(id);
    try {
      await apiClient.post(endpoints.betaRequests.reject(id), {
        approverId: 'coordinator',
        rejectionReason: reason,
      });
      await fetchData();
    } catch {
      setError(`Từ chối yêu cầu #${id} thất bại.`);
    } finally {
      setActingOn(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="rounded-2xl bg-gradient-to-r from-primary/10 to-accent/10 border p-6">
        <h1 className="text-2xl font-bold">Yêu cầu Beta</h1>
        <p className="mt-1 text-muted-foreground">
          Phase 1 BETA invite queue — duyệt thủ công, gửi email mời sau khi duyệt.
        </p>
      </div>

      <div className="flex items-center gap-2">
        <label htmlFor="status-filter" className="text-sm font-medium">
          Trạng thái:
        </label>
        <select
          id="status-filter"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as BetaStatus)}
          className="rounded-lg border bg-background px-3 py-2 text-sm"
        >
          {(['PENDING', 'APPROVED', 'REJECTED', 'SIGNED_UP'] as BetaStatus[]).map(
            (s) => (
              <option key={s} value={s}>
                {STATUS_LABEL[s]}
              </option>
            ),
          )}
        </select>
        <button
          type="button"
          onClick={fetchData}
          disabled={loading}
          className="rounded-lg border px-3 py-2 text-sm disabled:opacity-60"
        >
          {loading ? 'Đang tải...' : 'Làm mới'}
        </button>
      </div>

      {error && (
        <div role="alert" className="rounded-xl bg-destructive/10 p-4 text-sm text-destructive">
          {error}
        </div>
      )}

      <div className="rounded-2xl border bg-background overflow-hidden">
        <table className="min-w-full text-sm" data-testid="beta-requests-table">
          <thead className="bg-muted/50 text-left">
            <tr>
              <th className="px-4 py-3 font-medium">Email</th>
              <th className="px-4 py-3 font-medium">Họ tên</th>
              <th className="px-4 py-3 font-medium">Tổ chức</th>
              <th className="px-4 py-3 font-medium">Vai trò</th>
              <th className="px-4 py-3 font-medium">Tạo lúc</th>
              <th className="px-4 py-3 font-medium">Hành động</th>
            </tr>
          </thead>
          <tbody>
            {data?.content.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-muted-foreground">
                  Không có yêu cầu nào.
                </td>
              </tr>
            )}
            {data?.content.map((req) => (
              <tr key={req.id} className="border-t">
                <td className="px-4 py-3">{req.email}</td>
                <td className="px-4 py-3">{req.name}</td>
                <td className="px-4 py-3">{req.orgName}</td>
                <td className="px-4 py-3">{req.persona}</td>
                <td className="px-4 py-3">
                  {new Date(req.createdAt).toLocaleString('vi-VN')}
                </td>
                <td className="px-4 py-3">
                  {req.status === 'PENDING' ? (
                    <div className="flex gap-2">
                      <button
                        type="button"
                        onClick={() => onApprove(req.id)}
                        disabled={actingOn === req.id}
                        className="rounded-lg bg-primary px-3 py-1.5 text-xs text-primary-foreground disabled:opacity-60"
                      >
                        Duyệt
                      </button>
                      <button
                        type="button"
                        onClick={() => onReject(req.id)}
                        disabled={actingOn === req.id}
                        className="rounded-lg border px-3 py-1.5 text-xs disabled:opacity-60"
                      >
                        Từ chối
                      </button>
                    </div>
                  ) : (
                    <span className="text-xs text-muted-foreground">
                      {STATUS_LABEL[req.status]}
                    </span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {data && (
        <p className="text-xs text-muted-foreground">
          Tổng: {data.totalElements} yêu cầu
        </p>
      )}
    </div>
  );
}
