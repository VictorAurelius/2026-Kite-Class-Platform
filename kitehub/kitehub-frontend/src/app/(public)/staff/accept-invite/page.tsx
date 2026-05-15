/**
 * /staff/accept-invite — Public landing page for staff invitation acceptance
 * (Wave 80, GAP-561b).
 *
 * Recipient lands here from the invite email with `?token=<hmac>`. Page:
 *  1. Calls GET /by-token/{token} to preview invite (tenant + role).
 *  2. Shows password-set form (≥12 chars + mixed-case + digit per A07).
 *  3. POSTs to /accept → success page with login CTA.
 *
 * Public route — NO auth guard. Token validates identity.
 *
 * @since Wave 80 — GAP-561b
 */
'use client';

import { useCallback, useEffect, useState } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import { AxiosError } from 'axios';

interface InvitationPreview {
  id: string;
  tenantId: string;
  email: string;
  fullName: string;
  role: string;
  status: string;
  expiresAt: string;
}

interface ApiError {
  error?: string;
  message?: string;
}

const PASSWORD_RULE_LEN = 12;

export default function StaffAcceptInvitePage() {
  const params = useSearchParams();
  const router = useRouter();
  const token = params.get('token');

  const [preview, setPreview] = useState<InvitationPreview | null>(null);
  const [loading, setLoading] = useState(true);
  const [previewError, setPreviewError] = useState<string | null>(null);

  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [accepted, setAccepted] = useState(false);

  const fetchPreview = useCallback(async () => {
    if (!token) {
      setPreviewError('Liên kết không hợp lệ — thiếu mã token.');
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const resp = await apiClient.get<InvitationPreview>(
        endpoints.staffInvitations.byToken(token),
      );
      setPreview(resp.data);
      setFullName(resp.data.fullName);
    } catch (e) {
      const axErr = e as AxiosError<ApiError>;
      const status = axErr.response?.status;
      if (status === 410) {
        setPreviewError('Lời mời đã hết hạn hoặc đã được dùng. Vui lòng yêu cầu Chủ trung tâm gửi lại.');
      } else if (status === 404) {
        setPreviewError('Lời mời không tồn tại hoặc đã bị thu hồi.');
      } else {
        setPreviewError('Không tải được thông tin lời mời. Vui lòng thử lại sau.');
      }
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    void fetchPreview();
  }, [fetchPreview]);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitError(null);

    if (!token) {
      setSubmitError('Token thiếu — không thể chấp nhận.');
      return;
    }
    if (password.length < PASSWORD_RULE_LEN) {
      setSubmitError(`Mật khẩu cần ít nhất ${PASSWORD_RULE_LEN} ký tự.`);
      return;
    }
    if (!(/[A-Z]/.test(password) && /[a-z]/.test(password) && /\d/.test(password))) {
      setSubmitError('Mật khẩu cần có chữ hoa, chữ thường và chữ số.');
      return;
    }
    if (password !== confirmPassword) {
      setSubmitError('Mật khẩu nhập lại không khớp.');
      return;
    }

    setSubmitting(true);
    try {
      await apiClient.post(endpoints.staffInvitations.accept(token), {
        password,
        fullName: fullName.trim() || preview?.fullName,
      });
      setAccepted(true);
    } catch (e) {
      const axErr = e as AxiosError<ApiError>;
      const code = axErr.response?.data?.error;
      switch (code) {
        case 'INVALID_OR_EXPIRED_TOKEN':
        case 'INVITATION_EXPIRED':
        case 'INVITATION_ALREADY_USED':
          setSubmitError('Lời mời không còn hiệu lực. Vui lòng yêu cầu Chủ trung tâm gửi lại.');
          break;
        case 'WEAK_PASSWORD':
          setSubmitError('Mật khẩu chưa đủ mạnh. Vui lòng dùng ≥12 ký tự, có chữ hoa, chữ thường và chữ số.');
          break;
        case 'USER_ALREADY_EXISTS':
          setSubmitError('Email này đã được đăng ký. Vui lòng đăng nhập trực tiếp.');
          break;
        default:
          setSubmitError('Không hoàn tất được lời mời. Vui lòng thử lại sau.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="mx-auto max-w-md py-16 text-center text-muted-foreground">
        Đang xác thực lời mời...
      </div>
    );
  }

  if (previewError) {
    return (
      <div className="mx-auto max-w-md py-16 text-center">
        <h1 className="text-xl font-semibold">Không thể chấp nhận lời mời</h1>
        <p className="mt-3 text-sm text-muted-foreground">{previewError}</p>
        <button
          type="button"
          onClick={() => router.push('/')}
          className="mt-6 rounded-lg border px-4 py-2 text-sm"
        >
          Về trang chủ
        </button>
      </div>
    );
  }

  if (accepted) {
    return (
      <div className="mx-auto max-w-md space-y-4 py-16 text-center">
        <div className="text-4xl">🎉</div>
        <h1 className="text-xl font-semibold">Tham gia thành công</h1>
        <p className="text-sm text-muted-foreground">
          Tài khoản nhân viên đã được tạo. Bạn có thể đăng nhập bằng email{' '}
          <strong>{preview?.email}</strong> và mật khẩu vừa đặt.
        </p>
        <button
          type="button"
          onClick={() => router.push('/login')}
          className="mt-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
          data-testid="accept-success-login"
        >
          Đến trang đăng nhập
        </button>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-md space-y-6 py-12">
      <header>
        <h1 className="text-2xl font-semibold">Chấp nhận lời mời</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Bạn được mời tham gia trung tâm trên KiteHub với vai trò{' '}
          <strong>{preview?.role}</strong>. Đặt mật khẩu để hoàn tất.
        </p>
      </header>

      <div className="rounded-2xl border bg-muted/30 p-4 text-sm">
        <div className="mb-1">
          <span className="text-muted-foreground">Email: </span>
          <strong>{preview?.email}</strong>
        </div>
        <div className="text-xs text-muted-foreground">
          Hết hạn: {preview?.expiresAt && new Date(preview.expiresAt).toLocaleString('vi-VN')}
        </div>
      </div>

      <form
        onSubmit={onSubmit}
        className="space-y-4 rounded-2xl border bg-background p-6"
        data-testid="accept-invite-form"
      >
        <div className="space-y-2">
          <label htmlFor="fullName" className="block text-sm font-medium">
            Họ tên đầy đủ
          </label>
          <input
            id="fullName"
            type="text"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            className="w-full rounded-lg border bg-background px-3 py-2 text-sm"
            autoComplete="name"
            required
          />
        </div>

        <div className="space-y-2">
          <label htmlFor="password" className="block text-sm font-medium">
            Mật khẩu mới <span className="text-destructive">*</span>
          </label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full rounded-lg border bg-background px-3 py-2 text-sm"
            autoComplete="new-password"
            required
            data-testid="accept-password"
          />
          <p className="text-xs text-muted-foreground">
            Tối thiểu {PASSWORD_RULE_LEN} ký tự, có chữ hoa, chữ thường, và chữ số.
          </p>
        </div>

        <div className="space-y-2">
          <label htmlFor="confirmPassword" className="block text-sm font-medium">
            Nhập lại mật khẩu <span className="text-destructive">*</span>
          </label>
          <input
            id="confirmPassword"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            className="w-full rounded-lg border bg-background px-3 py-2 text-sm"
            autoComplete="new-password"
            required
            data-testid="accept-password-confirm"
          />
        </div>

        {submitError && (
          <div role="alert" className="rounded-lg bg-destructive/10 p-3 text-sm text-destructive">
            {submitError}
          </div>
        )}

        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground disabled:opacity-60"
          data-testid="accept-submit"
        >
          {submitting ? 'Đang xử lý...' : 'Hoàn tất + Đăng ký'}
        </button>
      </form>
    </div>
  );
}
