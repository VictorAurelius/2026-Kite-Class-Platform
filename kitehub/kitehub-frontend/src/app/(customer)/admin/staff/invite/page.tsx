/**
 * /admin/staff/invite — Owner-only form to issue a new staff invitation
 * (Wave 80, GAP-561b).
 *
 * Per business rule BR-ROLE-INVITE-001..005: 1 email per tenant at a time
 * (idempotency revokes old + creates new on re-invite). Role fixed to STAFF
 * in Phase 1 BETA. Email dispatched async; row appears in /admin/staff list.
 *
 * @since Wave 80 — GAP-561b
 */
'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import { AxiosError } from 'axios';

interface ApiError {
  error?: string;
  message?: string;
}

export default function AdminInviteStaffPage() {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [fullName, setFullName] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!email.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) {
      setError('Email không hợp lệ.');
      return;
    }
    if (fullName.trim().length < 2) {
      setError('Vui lòng nhập họ tên đầy đủ (ít nhất 2 ký tự).');
      return;
    }

    setSubmitting(true);
    try {
      // BE CreateStaffInvitationRequest schema (kitehub-subscription
      // /staff/dto): { email, fullName } — both required, NO role field.
      // Phase 1 BETA = 2-role MVP (OWNER + STAFF per
      // documents/01-business/roles/api-contract.md §19-20): every staff
      // invitation creates a STAFF user (hardcoded BE-side at accept-time).
      // TEACHER/MANAGER roles are Phase 2+ scope — GAP-784 confirmed BE does
      // NOT accept a role param, so we do NOT send one (avoids a misleading
      // role picker that promises roles the backend can't honor).
      await apiClient.post(endpoints.staffInvitations.create, {
        email: email.trim().toLowerCase(),
        fullName: fullName.trim(),
      });
      // Defer toast to list page so user sees the row appear.
      router.push('/admin/staff?invited=1');
    } catch (e) {
      const axErr = e as AxiosError<ApiError>;
      const code = axErr.response?.data?.error;
      switch (code) {
        case 'EMAIL_ALREADY_INVITED':
        case 'INVITATION_ALREADY_PENDING':
          setError('Email này đã có lời mời đang chờ. Bạn có thể gửi lại từ danh sách.');
          break;
        case 'STAFF_LIMIT_REACHED':
        case 'STAFF_CAP_REACHED':
          setError('Trung tâm đã đạt giới hạn 50 nhân viên. Vui lòng liên hệ KiteHub để nâng giới hạn.');
          break;
        case 'INVALID_REQUEST':
          setError('Dữ liệu không hợp lệ. Vui lòng kiểm tra lại email + họ tên.');
          break;
        case 'FORBIDDEN':
        case 'UNAUTHORIZED':
          setError('Bạn không có quyền mời nhân viên. Chỉ Chủ trung tâm thực hiện được.');
          break;
        default:
          setError('Không gửi được lời mời. Vui lòng thử lại sau.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <header>
        <Link
          href="/admin/staff"
          className="text-sm text-muted-foreground hover:underline"
        >
          ← Quay lại danh sách
        </Link>
        <h1 className="mt-2 text-2xl font-semibold">Mời nhân viên mới</h1>
        <p className="text-sm text-muted-foreground">
          Nhân viên sẽ nhận email kèm liên kết để tạo tài khoản. Liên kết có
          hiệu lực <strong>7 ngày</strong>.
        </p>
      </header>

      <form
        onSubmit={onSubmit}
        className="space-y-5 rounded-2xl border bg-background p-6"
        data-testid="invite-staff-form"
      >
        <div className="space-y-2">
          <label htmlFor="email" className="block text-sm font-medium">
            Email nhân viên <span className="text-destructive">*</span>
          </label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="staff@example.edu.vn"
            required
            autoComplete="email"
            className="w-full rounded-lg border bg-background px-3 py-2 text-sm focus:ring-2 focus:ring-primary"
            data-testid="invite-email-input"
          />
        </div>

        <div className="space-y-2">
          <label htmlFor="fullName" className="block text-sm font-medium">
            Họ tên đầy đủ <span className="text-destructive">*</span>
          </label>
          <input
            id="fullName"
            type="text"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            placeholder="Trần Thị Hồng"
            required
            minLength={2}
            maxLength={255}
            autoComplete="name"
            className="w-full rounded-lg border bg-background px-3 py-2 text-sm focus:ring-2 focus:ring-primary"
            data-testid="invite-full-name-input"
          />
        </div>

        <div className="space-y-2">
          <span className="block text-sm font-medium">Vai trò</span>
          <div
            className="w-full rounded-lg border bg-muted px-3 py-2 text-sm text-muted-foreground"
            data-testid="invite-role-display"
          >
            Nhân viên trung tâm (STAFF)
          </div>
          <p className="text-xs text-muted-foreground">
            Phiên bản hiện tại chỉ hỗ trợ mời <strong>Nhân viên (STAFF)</strong>. Các vai trò
            Giáo viên và Quản lý sẽ có ở bản nâng cấp tiếp theo. Vai trò Chủ trung tâm (OWNER)
            không mời được — mỗi trung tâm chỉ có 1 chủ.
          </p>
        </div>

        {error && (
          <div role="alert" className="rounded-lg bg-destructive/10 p-3 text-sm text-destructive">
            {error}
          </div>
        )}

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={submitting}
            className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground disabled:opacity-60"
            data-testid="invite-submit"
          >
            {submitting ? 'Đang gửi...' : 'Gửi lời mời'}
          </button>
          <Link
            href="/admin/staff"
            className="rounded-lg border px-4 py-2 text-sm font-medium"
          >
            Hủy
          </Link>
        </div>
      </form>
    </div>
  );
}
