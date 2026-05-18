'use client';

import { Suspense, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { AlertCircle, Loader2, ShieldCheck, KeyRound } from 'lucide-react';
import apiClient from '@/lib/api/client';
import { useAuthStore } from '@/stores/auth-store';
import { isPlatformAdmin } from '@/lib/auth-helpers';
import { KiteLogo } from '@/components/brand/KiteLogo';
import { TotpInput } from '@/components/auth/TotpInput';
import { setTokens } from '@/lib/auth/jwt-storage';

/**
 * 2FA Challenge page (subsequent-login TOTP gate).
 *
 * Wave 72b Bucket B (GAP-516 FE half) per
 * `documents/01-business/kitehub/auth/api-contract.md`:
 *   - POST /api/auth/2fa/verify
 *     - Request: { challenge_token, totp_code }  OR  { challenge_token, recovery_code }
 *     - Response 200: { access_token, refresh_token, user, [regenerate_recommended, codes_remaining] }
 *
 * Entry point: invoked when /login response is `{ requires2fa: true, challenge_token }`.
 *
 * UX:
 *   - Default mode: 6-digit TOTP via `<TotpInput>`
 *   - Toggle "Dùng mã khôi phục" switches to single text field (8-char recovery code)
 *   - On 401 INVALID_TOTP / INVALID_RECOVERY_CODE — show error, clear input, focus first box
 *   - On 410 CHALLENGE_EXPIRED — kick back to /login
 *   - On 423 ACCOUNT_LOCKED — show lockout-until time + Retry-After
 *   - On 200 — persist auth + redirect (admin → /admin, else /dashboard)
 *
 * @author KiteHub Team
 * @since Wave 72b Bucket B (GAP-516)
 */
function TwoFactorChallengePageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const challengeToken = searchParams.get('token');
  const { setAuth } = useAuthStore();

  const [mode, setMode] = useState<'totp' | 'recovery'>('totp');
  const [totpCode, setTotpCode] = useState('');
  const [recoveryCode, setRecoveryCode] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  if (!challengeToken) {
    return (
      <div>
        <KiteLogo size="md" />
        <h1 className="mt-6 text-2xl font-bold tracking-tight">Phiên không hợp lệ</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Liên kết xác thực 2FA không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.
        </p>
        <button
          type="button"
          onClick={() => router.push('/login')}
          className="mt-6 rounded-xl bg-primary px-5 py-3 text-sm font-semibold text-primary-foreground hover:bg-primary/90 transition-colors"
        >
          Về trang đăng nhập
        </button>
      </div>
    );
  }

  const handleSubmit = async (e?: React.FormEvent) => {
    e?.preventDefault();
    setError(null);
    setInfo(null);

    const payload: Record<string, string> = { challenge_token: challengeToken };
    if (mode === 'totp') {
      if (!/^\d{6}$/.test(totpCode)) return;
      payload.totp_code = totpCode;
    } else {
      if (!recoveryCode.trim()) return;
      payload.recovery_code = recoveryCode.trim().toLowerCase();
    }

    setSubmitting(true);
    try {
      const response = await apiClient.post('/api/auth/2fa/verify', payload);
      const { access_token, refresh_token, user, regenerate_recommended, codes_remaining } = response.data;

      setAuth(user, access_token, refresh_token);
      // GAP-599 Wave 92 Bucket B: sessionStorage (per-tab isolation).
      setTokens(access_token, refresh_token);

      if (regenerate_recommended) {
        // Surface info briefly via setInfo so user knows before redirect — keep in sessionStorage
        // so the landing page can show a banner; deferred to follow-up gap if not needed.
        sessionStorage.setItem(
          'recovery_codes_remaining',
          String(codes_remaining ?? 0)
        );
      }

      router.push(isPlatformAdmin(user.role) ? '/admin' : '/dashboard');
    } catch (err) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      const errCode = (err as { response?: { data?: { error?: string } } })?.response?.data?.error;

      if (status === 401 && errCode === 'INVALID_TOTP') {
        setError('Mã TOTP không đúng. Vui lòng kiểm tra app Authenticator và thử lại.');
        setTotpCode('');
      } else if (status === 401 && errCode === 'INVALID_RECOVERY_CODE') {
        setError('Mã khôi phục không hợp lệ hoặc đã dùng. Mỗi mã chỉ dùng được 1 lần.');
        setRecoveryCode('');
      } else if (status === 401 && errCode === 'INVALID_CHALLENGE') {
        setError('Phiên xác thực không hợp lệ. Vui lòng đăng nhập lại.');
      } else if (status === 410) {
        setError('Phiên xác thực 2FA đã hết hạn (>5 phút). Vui lòng đăng nhập lại.');
      } else if (status === 423) {
        const data = (err as { response?: { data?: { lockedUntil?: string } } })?.response?.data;
        setError(
          data?.lockedUntil
            ? `Tài khoản đã bị khóa tạm thời (đến ${new Date(data.lockedUntil).toLocaleString('vi-VN')}). Vui lòng thử lại sau.`
            : 'Tài khoản đã bị khóa do nhập sai nhiều lần. Vui lòng thử lại sau.'
        );
      } else {
        setError('Không thể xác thực 2FA. Vui lòng thử lại.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <div className="mb-8">
        <KiteLogo size="md" />
        <h1 className="mt-6 flex items-center gap-2 text-2xl font-bold tracking-tight">
          <ShieldCheck className="h-6 w-6 text-primary" />
          Xác thực 2 lớp
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {mode === 'totp'
            ? 'Nhập mã 6 số từ app Authenticator để hoàn tất đăng nhập.'
            : 'Nhập 1 mã khôi phục đã lưu khi thiết lập 2FA.'}
        </p>
      </div>

      {error && (
        <div role="alert" className="mb-6 flex items-start gap-2 rounded-xl bg-destructive/10 p-4 text-sm text-destructive">
          <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      {info && (
        <div role="status" className="mb-6 flex items-start gap-2 rounded-xl bg-blue-50 dark:bg-blue-950/20 p-4 text-sm text-blue-700 dark:text-blue-300">
          <span>{info}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-5">
        {mode === 'totp' ? (
          <div>
            <label className="mb-2 block text-sm font-medium">Nhập mã 6 số</label>
            <TotpInput
              value={totpCode}
              onChange={setTotpCode}
              disabled={submitting}
              aria-label="Mã TOTP 6 số xác thực"
              autoFocus
            />
          </div>
        ) : (
          <div>
            <label htmlFor="recovery-code-input" className="mb-2 block text-sm font-medium">
              Mã khôi phục (8 ký tự)
            </label>
            <input
              id="recovery-code-input"
              type="text"
              inputMode="text"
              autoComplete="off"
              autoFocus
              value={recoveryCode}
              onChange={(e) => setRecoveryCode(e.target.value)}
              disabled={submitting}
              className="w-full rounded-xl border bg-background px-4 py-3 font-mono text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-colors"
              placeholder="ví dụ: ab23cd45"
              aria-describedby="recovery-code-hint"
            />
            <p id="recovery-code-hint" className="mt-1.5 text-xs text-muted-foreground">
              Mã khôi phục là 8 ký tự bạn đã lưu khi thiết lập 2FA. Mỗi mã chỉ dùng được 1 lần.
            </p>
          </div>
        )}

        <button
          type="submit"
          disabled={
            submitting ||
            (mode === 'totp' ? !/^\d{6}$/.test(totpCode) : !recoveryCode.trim())
          }
          className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-primary py-3 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-50 transition-colors"
        >
          {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
          {submitting ? 'Đang xác thực…' : 'Xác thực'}
        </button>

        <div className="flex items-center justify-between text-sm">
          <button
            type="button"
            onClick={() => {
              setMode(mode === 'totp' ? 'recovery' : 'totp');
              setError(null);
              setTotpCode('');
              setRecoveryCode('');
            }}
            disabled={submitting}
            className="inline-flex items-center gap-1.5 text-primary hover:underline"
          >
            <KeyRound className="h-3.5 w-3.5" />
            {mode === 'totp' ? 'Dùng mã khôi phục thay TOTP' : 'Dùng mã TOTP từ app'}
          </button>

          <button
            type="button"
            onClick={() => router.push('/login')}
            disabled={submitting}
            className="text-muted-foreground hover:text-foreground"
          >
            Quay lại đăng nhập
          </button>
        </div>
      </form>
    </div>
  );
}

export default function TwoFactorChallengePage() {
  return (
    <Suspense
      fallback={
        <div className="flex items-center gap-3 text-sm text-muted-foreground">
          <Loader2 className="h-5 w-5 animate-spin" />
          Đang tải…
        </div>
      }
    >
      <TwoFactorChallengePageContent />
    </Suspense>
  );
}
