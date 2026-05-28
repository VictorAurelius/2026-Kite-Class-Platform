/**
 * /admin/staff — Owner-only dashboard listing staff invitations (Wave 80, GAP-561b).
 *
 * Shows pending + active + revoked invitations for the current tenant, with
 * actions to resend (PENDING) or revoke (PENDING). Owner-only at route layer;
 * RoleGuard wiring tracked in sister gap GAP-562b (Wave 80 Bucket C).
 *
 * @since Wave 80 — GAP-561b
 */
'use client';

import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';

type Status = 'PENDING' | 'ACCEPTED' | 'EXPIRED' | 'REVOKED';

interface StaffInvitation {
  id: string;
  tenantId: string;
  email: string;
  fullName: string;
  role: string;
  status: Status;
  invitedBy: string;
  createdAt: string;
  expiresAt: string;
  acceptedAt: string | null;
}

const STATUS_LABEL: Record<Status, string> = {
  PENDING: 'Đang chờ',
  ACCEPTED: 'Đã tham gia',
  EXPIRED: 'Hết hạn',
  REVOKED: 'Đã thu hồi',
};

const STATUS_BADGE: Record<Status, string> = {
  PENDING: 'bg-yellow-100 text-yellow-800',
  ACCEPTED: 'bg-emerald-100 text-emerald-800',
  EXPIRED: 'bg-gray-200 text-gray-700',
  REVOKED: 'bg-rose-100 text-rose-800',
};

export default function AdminStaffListPage() {
  const [rows, setRows] = useState<StaffInvitation[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actingOn, setActingOn] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      // BE wraps in ApiResponse: { success, data: StaffInvitation[], timestamp }
      // Unwrap .data.data; fallback to .data if response is unwrapped (defensive).
      const resp = await apiClient.get(endpoints.staffInvitations.list);
      const body: unknown = resp.data;
      let list: StaffInvitation[] = [];
      if (Array.isArray(body)) {
        list = body as StaffInvitation[];
      } else if (body && typeof body === 'object' && 'data' in body && Array.isArray((body as { data: unknown }).data)) {
        list = (body as { data: StaffInvitation[] }).data;
      }
      setRows(list);
    } catch {
      setError('Không tải được danh sách lời mời. Vui lòng thử lại sau.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const onResend = async (id: string) => {
    setActingOn(id);
    setToast(null);
    try {
      await apiClient.post(endpoints.staffInvitations.resend(id));
      setToast('Đã gửi lại email lời mời.');
      await fetchData();
    } catch {
      setError(`Không gửi lại được lời mời #${id}.`);
    } finally {
      setActingOn(null);
    }
  };

  const onRevoke = async (id: string) => {
    if (!window.confirm('Thu hồi lời mời này? Hành động không thể hoàn tác.')) {
      return;
    }
    setActingOn(id);
    setToast(null);
    try {
      await apiClient.delete(endpoints.staffInvitations.revoke(id));
      setToast('Đã thu hồi lời mời.');
      await fetchData();
    } catch {
      setError(`Không thu hồi được lời mời #${id}.`);
    } finally {
      setActingOn(null);
    }
  };

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold">Quản lý nhân viên</h1>
          <p className="text-sm text-muted-foreground">
            Quản lý lời mời nhân viên (Staff) tham gia trung tâm.
          </p>
        </div>
        <Link
          href="/admin/staff/invite"
          className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
          data-testid="invite-staff-cta"
        >
          + Mời nhân viên mới
        </Link>
      </header>

      {toast && (
        <div role="status" className="rounded-xl bg-emerald-50 p-3 text-sm text-emerald-800">
          {toast}
        </div>
      )}
      {error && (
        <div role="alert" className="rounded-xl bg-destructive/10 p-3 text-sm text-destructive">
          {error}
        </div>
      )}

      <div className="overflow-hidden rounded-2xl border bg-background">
        <table className="min-w-full text-sm" data-testid="staff-invitations-table">
          <thead className="bg-muted/50 text-left">
            <tr>
              <th className="px-4 py-3 font-medium">Email</th>
              <th className="px-4 py-3 font-medium">Họ tên</th>
              <th className="px-4 py-3 font-medium">Vai trò</th>
              <th className="px-4 py-3 font-medium">Trạng thái</th>
              <th className="px-4 py-3 font-medium">Hết hạn</th>
              <th className="px-4 py-3 font-medium">Hành động</th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-muted-foreground">
                  Đang tải...
                </td>
              </tr>
            )}
            {!loading && rows?.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-muted-foreground">
                  Chưa có lời mời nào. Bấm “Mời nhân viên mới” để bắt đầu.
                </td>
              </tr>
            )}
            {rows?.map((row) => (
              <tr key={row.id} className="border-t">
                <td className="px-4 py-3">{row.email}</td>
                <td className="px-4 py-3">{row.fullName}</td>
                <td className="px-4 py-3">{row.role}</td>
                <td className="px-4 py-3">
                  <span
                    className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_BADGE[row.status]}`}
                  >
                    {STATUS_LABEL[row.status]}
                  </span>
                </td>
                <td className="px-4 py-3 text-muted-foreground">
                  {new Date(row.expiresAt).toLocaleString('vi-VN')}
                </td>
                <td className="px-4 py-3">
                  {row.status === 'PENDING' ? (
                    <div className="flex gap-2">
                      <button
                        type="button"
                        onClick={() => onResend(row.id)}
                        disabled={actingOn === row.id}
                        className="rounded-lg border px-3 py-1.5 text-xs disabled:opacity-60"
                      >
                        Gửi lại
                      </button>
                      <button
                        type="button"
                        onClick={() => onRevoke(row.id)}
                        disabled={actingOn === row.id}
                        className="rounded-lg border border-destructive px-3 py-1.5 text-xs text-destructive disabled:opacity-60"
                      >
                        Thu hồi
                      </button>
                    </div>
                  ) : (
                    <span className="text-xs text-muted-foreground">—</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
