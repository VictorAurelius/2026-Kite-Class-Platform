/**
 * BetaClaimCodeForm — alternate signup path khi user nhận claim code qua
 * Zalo/WhatsApp/support thay vì email link (GAP-609 Wave 91).
 *
 * Flow:
 *  1. User nhập 6-digit code
 *  2. Submit → POST /api/v1/auth/beta-signup/exchange-claim-code
 *  3. On success → redirect /beta-signup?token=<UUID> (reuses existing form)
 *  4. On error → display Vietnamese message theo errorCode map
 *
 * UX choice: single numeric input maxLength=6 cho v1 simplicity. Phase 2 polish
 * có thể upgrade thành 6-box OTP-style với paste-from-clipboard.
 *
 * @since Wave 91 — GAP-609
 */
'use client';

import { useState, useEffect, FormEvent } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';

interface ExchangeResponse {
  valid: boolean;
  inviteToken?: string;
  errorCode?: string;
}

interface ErrorPayload {
  response?: { data?: ExchangeResponse };
}

const ERROR_MESSAGES: Record<string, string> = {
  CODE_NOT_FOUND: 'Mã không hợp lệ. Vui lòng kiểm tra lại.',
  CODE_EXPIRED: 'Mã đã hết hạn. Yêu cầu mã mới qua email.',
  ALREADY_USED: 'Mã đã được sử dụng. Đăng nhập nếu bạn đã có tài khoản.',
};

const DEFAULT_ERROR = 'Lỗi không xác định. Vui lòng thử lại.';
const CODE_PATTERN = /^\d{6}$/;

export default function BetaClaimCodeForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [code, setCode] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // GAP-801: prefill the 6-digit code from the email link (?code=119397).
  useEffect(() => {
    const prefill = searchParams.get('code');
    if (prefill) setCode(prefill.replace(/\D/g, '').slice(0, 6));
  }, [searchParams]);

  const isValid = CODE_PATTERN.test(code);

  const onChangeCode = (value: string) => {
    // Chỉ cho phép digit + tối đa 6 ký tự (input mode numeric + maxLength backup)
    const digits = value.replace(/\D/g, '').slice(0, 6);
    setCode(digits);
    if (error) setError(null);
  };

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!isValid) {
      setError('Mã phải gồm đúng 6 chữ số.');
      return;
    }
    setSubmitting(true);
    try {
      const resp = await apiClient.post(endpoints.auth.exchangeClaimCode, {
        claimCode: code,
      });
      const data = resp.data as ExchangeResponse;
      if (data.valid && data.inviteToken) {
        router.push(`/beta-signup?token=${data.inviteToken}`);
        return;
      }
      setError(ERROR_MESSAGES[data.errorCode ?? ''] ?? DEFAULT_ERROR);
    } catch (err) {
      const payload = err as ErrorPayload;
      const code = payload?.response?.data?.errorCode;
      setError(ERROR_MESSAGES[code ?? ''] ?? DEFAULT_ERROR);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form
      onSubmit={onSubmit}
      className="space-y-5"
      aria-label="beta-claim-code-form"
    >
      {error && (
        <div
          role="alert"
          className="rounded-xl bg-destructive/10 p-4 text-sm text-destructive"
        >
          {error}
        </div>
      )}

      <div>
        <label
          htmlFor="claim-code"
          className="block text-sm font-medium mb-1.5"
        >
          Mã invite (6 chữ số)
        </label>
        <input
          id="claim-code"
          name="claimCode"
          value={code}
          onChange={(e) => onChangeCode(e.target.value)}
          inputMode="numeric"
          autoComplete="one-time-code"
          pattern="\d{6}"
          maxLength={6}
          placeholder="123456"
          className="w-full rounded-xl border bg-background px-4 py-3 text-center text-2xl tracking-[0.5em] font-mono"
          required
          autoFocus
        />
        <p className="mt-2 text-xs text-muted-foreground">
          Mã được gửi trong email mời. Nếu không tìm thấy email, hãy liên hệ
          support để được hỗ trợ.
        </p>
      </div>

      <button
        type="submit"
        disabled={!isValid || submitting}
        className="w-full rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {submitting ? 'Đang xác thực...' : 'Tiếp tục'}
      </button>
    </form>
  );
}
