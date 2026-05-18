'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useEffect, useState } from 'react';
import { loginSchema, type LoginFormData } from '@/lib/validations/auth';
import { useAuthStore } from '@/stores/auth-store';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import { AlertCircle, Clock, Loader2 } from 'lucide-react';
import { KiteLogo } from '@/components/brand/KiteLogo';
import { isPlatformAdmin } from '@/lib/auth-helpers';
import { setTokens, clearLegacyLocalStorageTokens } from '@/lib/auth/jwt-storage';

/**
 * GAP-515 Wave 78 Bucket C — parse Retry-After (RFC 7231 §7.1.3) which may be:
 *   (a) an HTTP-date — RFC 1123 absolute timestamp
 *   (b) a delta-seconds integer
 * Returns seconds remaining, or 0 if unparseable / already elapsed.
 */
function parseRetryAfterSeconds(headerValue: string | undefined): number {
  if (!headerValue) return 0;
  const trimmed = headerValue.trim();
  // delta-seconds
  if (/^\d+$/.test(trimmed)) {
    const n = Number.parseInt(trimmed, 10);
    return Number.isFinite(n) && n > 0 ? n : 0;
  }
  // HTTP-date
  const parsed = Date.parse(trimmed);
  if (!Number.isNaN(parsed)) {
    const diffMs = parsed - Date.now();
    return diffMs > 0 ? Math.ceil(diffMs / 1000) : 0;
  }
  return 0;
}

/** Format seconds to "mm:ss" for short windows or "Hh Mm Ss" for long. */
function formatCountdown(seconds: number): string {
  if (seconds <= 0) return '0:00';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  if (h > 0) return `${h}h ${String(m).padStart(2, '0')}m ${String(s).padStart(2, '0')}s`;
  return `${m}:${String(s).padStart(2, '0')}`;
}

export default function LoginPage() {
  const router = useRouter();
  const { setAuth } = useAuthStore();
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  // GAP-515 Wave 78 Bucket C — when login returns 423 (account locked) OR
  // gateway 429 (rate-limited), parse Retry-After + countdown so user knows
  // exactly when retry is allowed instead of brute-force re-submitting.
  const [lockoutSecondsRemaining, setLockoutSecondsRemaining] = useState(0);

  // Tick down the lockout countdown once per second while > 0.
  useEffect(() => {
    if (lockoutSecondsRemaining <= 0) return;
    const handle = window.setInterval(() => {
      setLockoutSecondsRemaining((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => window.clearInterval(handle);
  }, [lockoutSecondsRemaining]);

  // GAP-599 Wave 92 Bucket B: sweep legacy localStorage tokens left over from
  // pre-migration builds. One-shot per mount; safe to call repeatedly.
  useEffect(() => {
    clearLegacyLocalStorageTokens();
  }, []);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginFormData) => {
    setError(null);
    setLoading(true);
    try {
      const response = await apiClient.post(endpoints.auth.login, data);
      const body = response.data ?? {};

      // GAP-516 Wave 72b: branch on 2FA flags before consuming tokens.
      // Per `documents/01-business/kitehub/auth/api-contract.md` login response extension,
      // backend may return one of 3 shapes:
      //   (a) { access_token, refresh_token, user }                        — no 2FA enrolled
      //   (b) { requires2fa: true, challenge_token }                       — TOTP gate
      //   (c) { requires2fa_enrollment: true, challenge_token }            — first-time admin
      // Existing production responses may still use camelCase accessToken/refreshToken;
      // we keep that compatibility branch alongside the contract-spec snake_case.
      if (body.requires2fa_enrollment === true && body.challenge_token) {
        router.push(`/2fa-setup?token=${encodeURIComponent(body.challenge_token)}`);
        return;
      }
      if (body.requires2fa === true && body.challenge_token) {
        router.push(`/2fa-challenge?token=${encodeURIComponent(body.challenge_token)}`);
        return;
      }

      const user = body.user;
      const accessToken = body.access_token ?? body.accessToken;
      const refreshToken = body.refresh_token ?? body.refreshToken;
      if (!user || !accessToken || !refreshToken) {
        throw new Error('Invalid login response shape');
      }
      setAuth(user, accessToken, refreshToken);
      // GAP-599 Wave 92 Bucket B: sessionStorage (per-tab isolation).
      setTokens(accessToken, refreshToken);
      // GAP-518: route both PLATFORM_ADMIN (canonical) and legacy ADMIN to /admin.
      router.push(isPlatformAdmin(user.role) ? '/admin' : '/dashboard');
    } catch (err) {
      const response = (err as { response?: { status?: number; headers?: Record<string, string> } })?.response;
      const status = response?.status;
      const retryAfterHeader = response?.headers?.['retry-after'] ?? response?.headers?.['Retry-After'];
      if (status === 423) {
        const seconds = parseRetryAfterSeconds(retryAfterHeader);
        setLockoutSecondsRemaining(seconds);
        setError(
          seconds > 0
            ? `Tài khoản đã bị khóa tạm thời do nhập sai mật khẩu nhiều lần. Thử lại sau ${formatCountdown(seconds)}.`
            : 'Tài khoản đã bị khóa tạm thời do nhập sai mật khẩu nhiều lần. Vui lòng thử lại sau.'
        );
      } else if (status === 429) {
        const seconds = parseRetryAfterSeconds(retryAfterHeader);
        setLockoutSecondsRemaining(seconds);
        setError(
          seconds > 0
            ? `Quá nhiều yêu cầu. Thử lại sau ${formatCountdown(seconds)}.`
            : 'Quá nhiều yêu cầu. Vui lòng thử lại sau.'
        );
      } else {
        setError('Email hoặc mật khẩu không đúng');
      }
    } finally {
      setLoading(false);
    }
  };

  const isLockedOut = lockoutSecondsRemaining > 0;
  const submitDisabled = loading || isLockedOut;

  return (
    <div>
      <div className="mb-8">
        <Link href="/"><KiteLogo size="md" /></Link>
        <h1 className="mt-6 text-2xl font-bold tracking-tight">Đăng nhập</h1>
        <p className="mt-1 text-sm text-muted-foreground">Chào mừng bạn quay lại! Nhập thông tin để tiếp tục.</p>
      </div>

      {error && (
        <div
          className="mb-6 flex items-center gap-2 rounded-xl bg-destructive/10 p-4 text-sm text-destructive"
          role="alert"
        >
          {isLockedOut ? (
            <Clock className="h-4 w-4 shrink-0" aria-hidden="true" />
          ) : (
            <AlertCircle className="h-4 w-4 shrink-0" aria-hidden="true" />
          )}
          <span data-testid="login-error-message">{error}</span>
        </div>
      )}
      {isLockedOut && (
        <div
          className="mb-6 rounded-xl border border-destructive/30 bg-destructive/5 px-4 py-3 text-xs text-destructive"
          aria-live="polite"
          data-testid="login-retry-countdown"
        >
          Có thể thử lại sau:{' '}
          <span className="font-mono font-semibold">{formatCountdown(lockoutSecondsRemaining)}</span>
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        <div>
          <label className="block text-sm font-medium mb-1.5">Email</label>
          <input
            type="email"
            {...register('email')}
            className="w-full rounded-xl border bg-background px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-colors"
            placeholder="email@example.com"
          />
          {errors.email && (
            <p className="mt-1.5 text-xs text-destructive">{errors.email.message}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium mb-1.5">Mật khẩu</label>
          <input
            type="password"
            {...register('password')}
            className="w-full rounded-xl border bg-background px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-colors"
          />
          {errors.password && (
            <p className="mt-1.5 text-xs text-destructive">{errors.password.message}</p>
          )}
        </div>

        <button
          type="submit"
          disabled={submitDisabled}
          aria-disabled={submitDisabled}
          data-testid="login-submit"
          className="w-full rounded-xl bg-primary py-3 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center justify-center gap-2"
        >
          {loading && <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />}
          {isLockedOut
            ? `Tạm khóa — ${formatCountdown(lockoutSecondsRemaining)}`
            : loading
              ? 'Đang đăng nhập...'
              : 'Đăng nhập'}
        </button>
      </form>

      <p className="mt-8 text-center text-sm text-muted-foreground">
        Chưa có tài khoản?{' '}
        <Link href="/register" className="font-medium text-primary hover:underline">
          Đăng ký dùng thử
        </Link>
      </p>
    </div>
  );
}
