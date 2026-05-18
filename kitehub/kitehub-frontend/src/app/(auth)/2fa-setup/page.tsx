'use client';

import { Suspense, useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { QRCodeSVG } from 'qrcode.react';
import { AlertCircle, Loader2, ShieldCheck } from 'lucide-react';
import apiClient from '@/lib/api/client';
import { useAuthStore } from '@/stores/auth-store';
import { isPlatformAdmin } from '@/lib/auth-helpers';
import { KiteLogo } from '@/components/brand/KiteLogo';
import { TotpInput } from '@/components/auth/TotpInput';
import { RecoveryCodesDisplay } from '@/components/auth/RecoveryCodesDisplay';
import { setTokens } from '@/lib/auth/jwt-storage';

/**
 * 2FA Setup page (mandatory enrollment landing for PLATFORM_ADMIN first-login).
 *
 * Wave 72b Bucket B (GAP-516 FE half) per
 * `documents/01-business/kitehub/auth/api-contract.md`:
 *   - POST /api/auth/2fa/enroll-init  → returns secret + qr_uri + 10 recovery codes (ONCE)
 *   - POST /api/auth/2fa/enroll-confirm  → user submits first TOTP code; on success returns
 *     access_token + refresh_token + user profile
 *
 * Flow:
 *   1. Mount: read `challenge_token` from query string, call /enroll-init
 *   2. Step A: display QR + recovery codes; gate confirm-button until user clicks "Tôi đã lưu mã"
 *   3. Step B: user enters first TOTP code; submit to /enroll-confirm
 *   4. On 200: store auth tokens via authStore + redirect to admin home (PLATFORM_ADMIN) or /dashboard
 *
 * Entry point: invoked when /login response is `requires2fa_enrollment: true` (first-time admin
 * post Wave 72c when policy enforced server-side).
 *
 * @author KiteHub Team
 * @since Wave 72b Bucket B (GAP-516)
 */
function TwoFactorSetupPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const challengeToken = searchParams.get('token');
  const { setAuth } = useAuthStore();

  const [phase, setPhase] = useState<'init-loading' | 'show-codes' | 'enter-code' | 'submitting' | 'error'>(
    'init-loading'
  );
  const [error, setError] = useState<string | null>(null);
  const [secret, setSecret] = useState<string | null>(null);
  const [qrUri, setQrUri] = useState<string | null>(null);
  const [recoveryCodes, setRecoveryCodes] = useState<string[]>([]);
  const [codesAcknowledged, setCodesAcknowledged] = useState(false);
  const [totpCode, setTotpCode] = useState('');

  // Call /enroll-init on mount
  useEffect(() => {
    if (!challengeToken) {
      setError('Liên kết thiết lập 2FA không hợp lệ. Vui lòng đăng nhập lại.');
      setPhase('error');
      return;
    }

    let cancelled = false;
    (async () => {
      try {
        const response = await apiClient.post(
          '/api/auth/2fa/enroll-init',
          {},
          { headers: { Authorization: `Bearer ${challengeToken}` } }
        );
        if (cancelled) return;
        const { secret: s, qr_uri: q, recovery_codes: codes } = response.data;
        setSecret(s);
        setQrUri(q);
        setRecoveryCodes(codes);
        setPhase('show-codes');
      } catch (err) {
        if (cancelled) return;
        const status = (err as { response?: { status?: number } })?.response?.status;
        if (status === 410) {
          setError('Phiên thiết lập 2FA đã hết hạn (>5 phút). Vui lòng đăng nhập lại.');
        } else if (status === 409) {
          setError('Tài khoản đã thiết lập 2FA. Bạn không cần thiết lập lại.');
        } else {
          setError('Không thể bắt đầu thiết lập 2FA. Vui lòng thử lại sau.');
        }
        setPhase('error');
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [challengeToken]);

  const handleConfirmSubmit = async (e?: React.FormEvent) => {
    e?.preventDefault();
    if (!challengeToken || !/^\d{6}$/.test(totpCode)) return;

    setPhase('submitting');
    setError(null);
    try {
      const response = await apiClient.post(
        '/api/auth/2fa/enroll-confirm',
        { first_totp_code: totpCode },
        { headers: { Authorization: `Bearer ${challengeToken}` } }
      );
      const { access_token, refresh_token, user } = response.data;

      // Map snake_case API response to camelCase store shape
      setAuth(user, access_token, refresh_token);
      // GAP-599 Wave 92 Bucket B: sessionStorage (per-tab isolation).
      setTokens(access_token, refresh_token);

      router.push(isPlatformAdmin(user.role) ? '/admin' : '/dashboard');
    } catch (err) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      const errCode = (err as { response?: { data?: { error?: string } } })?.response?.data?.error;
      if (status === 401 && errCode === 'INVALID_TOTP') {
        setError('Mã TOTP không đúng. Vui lòng kiểm tra app Authenticator và thử lại.');
      } else if (status === 410) {
        setError('Phiên thiết lập đã hết hạn. Vui lòng đăng nhập lại.');
      } else if (status === 409) {
        setError('Tài khoản đã thiết lập 2FA trên phiên khác.');
      } else {
        setError('Không thể xác nhận thiết lập 2FA. Vui lòng thử lại.');
      }
      setPhase('enter-code');
      setTotpCode('');
    }
  };

  return (
    <div>
      <div className="mb-8">
        <KiteLogo size="md" />
        <h1 className="mt-6 flex items-center gap-2 text-2xl font-bold tracking-tight">
          <ShieldCheck className="h-6 w-6 text-primary" />
          Thiết lập xác thực 2 lớp
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Tài khoản quản trị PHẢI bật 2FA trước khi truy cập trang quản trị.
        </p>
      </div>

      {error && (
        <div role="alert" className="mb-6 flex items-start gap-2 rounded-xl bg-destructive/10 p-4 text-sm text-destructive">
          <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      {phase === 'init-loading' && (
        <div className="flex items-center gap-3 text-sm text-muted-foreground">
          <Loader2 className="h-5 w-5 animate-spin" />
          Đang khởi tạo phiên thiết lập 2FA…
        </div>
      )}

      {phase === 'error' && (
        <button
          type="button"
          onClick={() => router.push('/login')}
          className="rounded-xl bg-primary px-5 py-3 text-sm font-semibold text-primary-foreground hover:bg-primary/90 transition-colors"
        >
          Về trang đăng nhập
        </button>
      )}

      {phase === 'show-codes' && qrUri && secret && (
        <div className="space-y-6">
          <section aria-labelledby="qr-heading" className="rounded-2xl border bg-card p-6 shadow-sm">
            <h2 id="qr-heading" className="text-lg font-semibold">
              Bước 1: Quét mã QR bằng app Authenticator
            </h2>
            <p className="mt-1 mb-4 text-sm text-muted-foreground">
              Mở app như Google Authenticator, Microsoft Authenticator, hoặc 1Password, sau đó quét mã QR bên dưới.
            </p>
            <div className="flex flex-col items-center gap-4 sm:flex-row sm:items-start">
              <div className="rounded-xl bg-white p-4 shadow-sm" aria-label="Mã QR TOTP">
                <QRCodeSVG value={qrUri} size={192} level="M" />
              </div>
              <div className="flex-1 text-sm">
                <p className="font-medium">Không quét được QR?</p>
                <p className="mt-1 text-muted-foreground">
                  Nhập thủ công khóa bí mật vào app:
                </p>
                <code className="mt-2 inline-block break-all rounded-md bg-muted px-2 py-1 font-mono text-sm select-all">
                  {secret}
                </code>
              </div>
            </div>
          </section>

          <section aria-labelledby="codes-heading">
            <h2 id="codes-heading" className="sr-only">Bước 2: Lưu mã khôi phục</h2>
            <RecoveryCodesDisplay
              codes={recoveryCodes}
              heading="Bước 2: Lưu mã khôi phục"
              onSaveAction={() => setCodesAcknowledged(true)}
            />
          </section>

          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <label className="flex items-start gap-2 text-sm text-foreground">
              <input
                type="checkbox"
                checked={codesAcknowledged}
                onChange={(e) => setCodesAcknowledged(e.target.checked)}
                className="mt-1 h-4 w-4 rounded border-input"
              />
              <span>Tôi đã lưu mã khôi phục ở nơi an toàn</span>
            </label>
            <button
              type="button"
              disabled={!codesAcknowledged}
              onClick={() => setPhase('enter-code')}
              className="rounded-xl bg-primary px-5 py-3 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-50 transition-colors"
            >
              Tiếp tục: Xác nhận mã TOTP
            </button>
          </div>
        </div>
      )}

      {(phase === 'enter-code' || phase === 'submitting') && (
        <form onSubmit={handleConfirmSubmit} className="space-y-5">
          <div className="rounded-2xl border bg-card p-6 shadow-sm">
            <h2 className="text-lg font-semibold">Bước 3: Nhập mã 6 số từ app Authenticator</h2>
            <p className="mt-1 mb-4 text-sm text-muted-foreground">
              Mở app Authenticator vừa cài, lấy mã 6 số hiện tại cho tài khoản KiteHub và nhập vào đây.
            </p>
            <TotpInput
              value={totpCode}
              onChange={setTotpCode}
              disabled={phase === 'submitting'}
              aria-label="Mã 6 số xác nhận thiết lập"
              autoFocus
            />
          </div>

          <div className="flex items-center justify-between gap-3">
            <button
              type="button"
              onClick={() => {
                setPhase('show-codes');
                setTotpCode('');
              }}
              disabled={phase === 'submitting'}
              className="text-sm text-muted-foreground hover:text-foreground"
            >
              Quay lại
            </button>
            <button
              type="submit"
              disabled={phase === 'submitting' || !/^\d{6}$/.test(totpCode)}
              className="inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-3 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-50 transition-colors"
            >
              {phase === 'submitting' && <Loader2 className="h-4 w-4 animate-spin" />}
              {phase === 'submitting' ? 'Đang xác nhận…' : 'Xác nhận và kích hoạt 2FA'}
            </button>
          </div>
        </form>
      )}
    </div>
  );
}

export default function TwoFactorSetupPage() {
  return (
    <Suspense
      fallback={
        <div className="flex items-center gap-3 text-sm text-muted-foreground">
          <Loader2 className="h-5 w-5 animate-spin" />
          Đang tải…
        </div>
      }
    >
      <TwoFactorSetupPageContent />
    </Suspense>
  );
}
