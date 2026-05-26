/**
 * BetaSignupForm — completes signup once the invitee opens the email link.
 *
 * Flow:
 *  1. Page parses ?token=XXX from URL
 *  2. On mount, calls /api/v1/auth/beta-signup/validate to pre-fill email/name
 *  3. User chooses subdomain + password + consent (3 granular checkboxes per PDPL Art 11)
 *  4. Submit → /api/v1/auth/beta-signup → tenant provisioning kicks off
 *
 * PDPL Compliance (Wave beta-prep-1 Bucket A):
 *  - acceptTosPrivacy: REQUIRED — accept ToS + Privacy Notice (PDPL Art 11 informed consent)
 *  - acceptMarketing: OPTIONAL — marketing email consent (granular per Decree 13 Art 4)
 *  - acceptAnalytics: OPTIONAL — analytics tracking consent (granular per Decree 13 Art 4)
 *
 * Consent records persisted via consent_record_immutable table (V56 migration —
 * PDPL Art 11 immutable hash chain). BE integration ships in follow-up gap;
 * FE captures consent state same-PR per `local-fix-production-parity-check.md`.
 *
 * @since Wave 33 — GAP-372 (initial)
 * @since Wave beta-prep-1 Bucket A — GAP-PDPL-COMPLIANCE-MIN (PDPL consent + ToS)
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
  // PDPL Wave beta-prep-1 Bucket A — 3 granular consent checkboxes per Decree 13 Art 4
  const [acceptTosPrivacy, setAcceptTosPrivacy] = useState(false);
  const [acceptMarketing, setAcceptMarketing] = useState(false);
  const [acceptAnalytics, setAcceptAnalytics] = useState(false);
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
    if (!acceptTosPrivacy) {
      setError('Bạn cần đồng ý Điều khoản sử dụng và Thông báo bảo mật để tiếp tục.');
      return;
    }
    setSubmitting(true);
    try {
      await apiClient.post(endpoints.auth.completeBetaSignup, {
        token,
        ownerPassword: password,
        subdomain,
        // PDPL Wave beta-prep-1 Bucket A — granular consent per Decree 13 Art 4
        // BE persist via consent_record_immutable table (V56) follows in follow-up gap
        consent: {
          tosPrivacy: acceptTosPrivacy,
          marketing: acceptMarketing,
          analytics: acceptAnalytics,
          version: 'v0.9.0-beta',
          signedAt: new Date().toISOString(),
        },
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

      {/*
        PDPL Wave beta-prep-1 Bucket A — Granular consent checkboxes per Decree 13 Art 4.
        Required: tosPrivacy (informed consent). Optional: marketing + analytics.
      */}
      <fieldset className="space-y-3 rounded-xl border border-border bg-muted/30 p-4">
        <legend className="px-2 text-sm font-medium">Đồng ý xử lý dữ liệu (PDPL)</legend>

        <label htmlFor="consent-tos-privacy" className="flex items-start gap-3 text-sm">
          <input
            id="consent-tos-privacy"
            type="checkbox"
            checked={acceptTosPrivacy}
            onChange={(e) => setAcceptTosPrivacy(e.target.checked)}
            className="mt-1"
            required
            aria-required="true"
          />
          <span>
            Tôi đã đọc và đồng ý{' '}
            <a href="/terms" target="_blank" rel="noopener noreferrer" className="text-primary underline">
              Điều khoản sử dụng
            </a>{' '}
            và{' '}
            <a href="/privacy" target="_blank" rel="noopener noreferrer" className="text-primary underline">
              Thông báo bảo mật
            </a>{' '}
            (bắt buộc)
          </span>
        </label>

        <label htmlFor="consent-marketing" className="flex items-start gap-3 text-sm">
          <input
            id="consent-marketing"
            type="checkbox"
            checked={acceptMarketing}
            onChange={(e) => setAcceptMarketing(e.target.checked)}
            className="mt-1"
          />
          <span>
            Tôi đồng ý nhận email marketing về tính năng mới, ưu đãi (tùy chọn — có thể hủy bất kỳ lúc nào)
          </span>
        </label>

        <label htmlFor="consent-analytics" className="flex items-start gap-3 text-sm">
          <input
            id="consent-analytics"
            type="checkbox"
            checked={acceptAnalytics}
            onChange={(e) => setAcceptAnalytics(e.target.checked)}
            className="mt-1"
          />
          <span>
            Tôi đồng ý KiteHub thu thập dữ liệu sử dụng (analytics) để cải thiện sản phẩm (tùy chọn)
          </span>
        </label>
      </fieldset>

      <button
        type="submit"
        disabled={submitting || !acceptTosPrivacy}
        className="w-full rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground disabled:opacity-60"
      >
        {submitting ? 'Đang xử lý...' : 'Hoàn tất đăng ký'}
      </button>
    </form>
  );
}
