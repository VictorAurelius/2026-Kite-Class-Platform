/**
 * BetaSignupForm — completes signup once the invitee opens the email link.
 *
 * Flow:
 *  1. Page parses ?token=XXX from URL
 *  2. On mount, calls /api/v1/auth/beta-signup/validate to pre-fill email/name
 *  3. User chooses subdomain + password
 *  4. Submit → /api/v1/auth/beta-signup → tenant provisioning kicks off
 *
 * @since Wave 33 — GAP-372
 */
'use client';

import { useEffect, useState, FormEvent } from 'react';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';

export interface BetaTokenStatus {
  valid: boolean;
  email?: string;
  name?: string;
  orgName?: string;
  persona?: string;
  errorCode?: string;
}

export interface BetaSignupFormProps {
  token: string | null;
}

export default function BetaSignupForm({ token }: BetaSignupFormProps) {
  const [tokenStatus, setTokenStatus] = useState<BetaTokenStatus | null>(null);
  const [validating, setValidating] = useState(true);
  const [subdomain, setSubdomain] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      setTokenStatus({ valid: false, errorCode: 'TOKEN_NOT_FOUND' });
      setValidating(false);
      return;
    }
    apiClient
      .get(endpoints.auth.validateBetaToken, { params: { token } })
      .then((resp) => setTokenStatus(resp.data as BetaTokenStatus))
      .catch((err) => {
        const data = err?.response?.data as BetaTokenStatus | undefined;
        setTokenStatus(data ?? { valid: false, errorCode: 'TOKEN_NOT_FOUND' });
      })
      .finally(() => setValidating(false));
  }, [token]);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!token) return;
    if (password.length < 8) {
      setError('Mật khẩu phải tối thiểu 8 ký tự.');
      return;
    }
    if (!subdomain.match(/^[a-z0-9-]{3,50}$/)) {
      setError('Subdomain chỉ chứa chữ thường, số, dấu gạch ngang (3-50 ký tự).');
      return;
    }
    setSubmitting(true);
    try {
      await apiClient.post(endpoints.auth.completeBetaSignup, {
        token,
        ownerPassword: password,
        subdomain,
      });
      setSubmitted(true);
    } catch {
      setError('Hoàn tất đăng ký thất bại. Token có thể đã hết hạn hoặc đã được sử dụng.');
    } finally {
      setSubmitting(false);
    }
  };

  if (validating) {
    return <p className="text-sm text-muted-foreground">Đang kiểm tra liên kết...</p>;
  }

  if (!tokenStatus?.valid) {
    const reason =
      tokenStatus?.errorCode === 'TOKEN_EXPIRED'
        ? 'Liên kết kích hoạt đã hết hạn. Hãy liên hệ đội ngũ KiteClass để được cấp lại.'
        : tokenStatus?.errorCode === 'ALREADY_USED'
        ? 'Liên kết đã được sử dụng. Vui lòng đăng nhập trực tiếp.'
        : 'Liên kết không hợp lệ.';
    return (
      <div role="alert" className="rounded-xl bg-destructive/10 p-6 text-sm text-destructive">
        {reason}
      </div>
    );
  }

  if (submitted) {
    return (
      <div role="status" className="rounded-xl bg-green-50 p-6 text-sm text-green-900">
        <h2 className="text-lg font-semibold">Tạo tài khoản thành công</h2>
        <p className="mt-2">Bạn có thể đăng nhập với email <strong>{tokenStatus.email}</strong>.</p>
      </div>
    );
  }

  return (
    <form onSubmit={onSubmit} className="space-y-5" aria-label="beta-signup-form">
      <div className="rounded-xl bg-muted p-4 text-sm">
        <div>Email: <strong>{tokenStatus.email}</strong></div>
        <div>Tổ chức: <strong>{tokenStatus.orgName}</strong></div>
      </div>

      {error && (
        <div role="alert" className="rounded-xl bg-destructive/10 p-4 text-sm text-destructive">
          {error}
        </div>
      )}

      <div>
        <label htmlFor="signup-subdomain" className="block text-sm font-medium mb-1.5">
          Subdomain
        </label>
        <input
          id="signup-subdomain"
          value={subdomain}
          onChange={(e) => setSubdomain(e.target.value.toLowerCase())}
          className="w-full rounded-xl border bg-background px-4 py-3 text-sm"
          placeholder="ten-truong"
          required
          maxLength={50}
        />
      </div>

      <div>
        <label htmlFor="signup-password" className="block text-sm font-medium mb-1.5">
          Mật khẩu
        </label>
        <input
          id="signup-password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="w-full rounded-xl border bg-background px-4 py-3 text-sm"
          required
          minLength={8}
          maxLength={200}
        />
      </div>

      <button
        type="submit"
        disabled={submitting}
        className="w-full rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground disabled:opacity-60"
      >
        {submitting ? 'Đang xử lý...' : 'Hoàn tất đăng ký'}
      </button>
    </form>
  );
}
