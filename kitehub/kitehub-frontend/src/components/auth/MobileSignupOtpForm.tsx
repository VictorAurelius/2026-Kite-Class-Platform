/**
 * MobileSignupOtpForm — 2-step phone → OTP signup (GAP-286, mobile-first).
 *
 * Flow (UC-OTP-01 → UC-OTP-02):
 *  Step "phone": VN phone input → POST /api/v1/auth/signup/request-otp
 *                200 → advance to OTP step + start 5-min expiry countdown
 *                400 INVALID_PHONE → inline error
 *                429 RATE_LIMITED  → disable + cooldown from retryAfterSeconds
 *  Step "otp":   6-digit code (SMS Web-OTP autofill) → POST .../verify-otp
 *                200 verified → store signupToken (sessionStorage) + success state
 *                400 INVALID_CODE | EXPIRED | TOO_MANY_ATTEMPTS → mapped VN message
 *
 * PRE-AUTH flow — there is no tenant yet, so apiClient attaches NO
 * Authorization / X-Tenant-Id headers (interceptor only adds them when an
 * access token is present in sessionStorage).
 *
 * Contract: documents/01-business/kitehub/signup-otp/api-contract.md
 * @since Wave OTP — GAP-286
 */
'use client';

import { FormEvent, useCallback, useEffect, useRef, useState } from 'react';
import { Loader2, Phone, ShieldCheck, CheckCircle2 } from 'lucide-react';

import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';

const PHONE_RE = /^0\d{9,10}$/; // VN format (BR-OTP-005): 0 + 9–10 digits
const OTP_LEN = 6;
const SIGNUP_TOKEN_KEY = 'kh_signup_token';

type Step = 'phone' | 'otp';

interface RequestOtpResponse {
  requestId: string;
  channel: string;
  expiresInSeconds: number;
  mock: boolean;
}

interface VerifyOtpResponse {
  verified: boolean;
  signupToken?: string;
  reason?: 'INVALID_CODE' | 'EXPIRED' | 'TOO_MANY_ATTEMPTS';
}

function getStatus(err: unknown): number | undefined {
  return (err as { response?: { status?: number } })?.response?.status;
}
function getData<T>(err: unknown): T | undefined {
  return (err as { response?: { data?: T } })?.response?.data;
}

function formatMMSS(total: number): string {
  const m = Math.floor(total / 60);
  const s = total % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
}

export default function MobileSignupOtpForm() {
  const [step, setStep] = useState<Step>('phone');
  const [phone, setPhone] = useState('');
  const [code, setCode] = useState('');
  const [channel, setChannel] = useState<string>('ZALO');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [expiredHint, setExpiredHint] = useState(false);
  const [success, setSuccess] = useState(false);

  // Two independent countdowns, decremented once per second by a single interval.
  const [otpExpiry, setOtpExpiry] = useState(0); // OTP validity remaining (5 min)
  const [cooldown, setCooldown] = useState(0); // resend / rate-limit lockout (429)

  useEffect(() => {
    if (otpExpiry <= 0 && cooldown <= 0) return;
    const id = setInterval(() => {
      setOtpExpiry((v) => (v > 0 ? v - 1 : 0));
      setCooldown((v) => (v > 0 ? v - 1 : 0));
    }, 1000);
    return () => clearInterval(id);
  }, [otpExpiry, cooldown]);

  // Mark the code expired once the validity countdown hits zero on the OTP step.
  useEffect(() => {
    if (step === 'otp' && otpExpiry === 0) setExpiredHint(true);
  }, [step, otpExpiry]);

  const otpInputRef = useRef<HTMLInputElement>(null);
  useEffect(() => {
    if (step === 'otp') otpInputRef.current?.focus();
  }, [step]);

  // ── request-otp (initial send + resend) ─────────────────────────────────
  const requestOtp = useCallback(
    async (advance: boolean) => {
      setError(null);
      if (!PHONE_RE.test(phone)) {
        setError('Số điện thoại không hợp lệ');
        return;
      }
      setLoading(true);
      try {
        const { data } = await apiClient.post<RequestOtpResponse>(
          endpoints.auth.requestSignupOtp,
          { phone, channel }
        );
        setChannel(data.channel ?? channel);
        setOtpExpiry(data.expiresInSeconds ?? 300);
        setCooldown(60); // soft throttle before next resend is offered
        setExpiredHint(false);
        setCode('');
        if (advance) setStep('otp');
      } catch (err) {
        const status = getStatus(err);
        if (status === 400) {
          setError('Số điện thoại không hợp lệ');
        } else if (status === 429) {
          const retry = getData<{ retryAfterSeconds?: number }>(err)?.retryAfterSeconds ?? 60;
          setCooldown(retry);
          setError(`Bạn đã yêu cầu quá nhiều lần. Thử lại sau ${retry} giây.`);
        } else if (status == null) {
          setError('Không thể kết nối tới máy chủ. Vui lòng kiểm tra mạng và thử lại.');
        } else {
          setError('Gửi mã thất bại. Vui lòng thử lại.');
        }
      } finally {
        setLoading(false);
      }
    },
    [phone, channel]
  );

  // ── verify-otp ──────────────────────────────────────────────────────────
  const verifyOtp = useCallback(async () => {
    setError(null);
    if (code.length !== OTP_LEN) {
      setError('Vui lòng nhập đủ 6 chữ số.');
      return;
    }
    setLoading(true);
    try {
      const { data } = await apiClient.post<VerifyOtpResponse>(
        endpoints.auth.verifySignupOtp,
        { phone, code }
      );
      if (data.verified && data.signupToken) {
        // Persist proof-of-phone-ownership token for the Phase 2 create-tenant step.
        if (typeof window !== 'undefined') {
          sessionStorage.setItem(SIGNUP_TOKEN_KEY, data.signupToken);
        }
        // TODO (Phase 2 / GAP-286 fast-provisioning, UC-OTP-03): navigate to the
        // create-tenant step (POST /api/v1/auth/signup/create-tenant) carrying the
        // signupToken. Out of scope this sprint → show success state for now.
        setSuccess(true);
      } else {
        setError('Xác thực thất bại. Vui lòng thử lại.');
      }
    } catch (err) {
      const status = getStatus(err);
      const reason = getData<VerifyOtpResponse>(err)?.reason;
      if (status === 400 && reason === 'INVALID_CODE') {
        setError('Mã không đúng. Vui lòng kiểm tra lại.');
        setCode('');
      } else if (status === 400 && reason === 'EXPIRED') {
        setError('Mã đã hết hạn. Vui lòng nhấn "Gửi lại mã".');
        setExpiredHint(true);
        setOtpExpiry(0);
      } else if (status === 400 && reason === 'TOO_MANY_ATTEMPTS') {
        setError('Quá số lần thử. Vui lòng gửi lại mã mới.');
        setExpiredHint(true);
        setOtpExpiry(0);
        setCode('');
      } else if (status == null) {
        setError('Không thể kết nối tới máy chủ. Vui lòng kiểm tra mạng và thử lại.');
      } else {
        setError('Xác thực thất bại. Vui lòng thử lại.');
      }
    } finally {
      setLoading(false);
    }
  }, [phone, code]);

  // ── success state ───────────────────────────────────────────────────────
  if (success) {
    return (
      <div
        role="status"
        className="rounded-xl border border-green-200 bg-green-50 p-6 text-center text-green-900 dark:border-green-900/40 dark:bg-green-950/30 dark:text-green-100"
      >
        <CheckCircle2 className="mx-auto h-10 w-10 text-green-600" aria-hidden />
        <h2 className="mt-3 text-lg font-semibold">Xác thực thành công</h2>
        <p className="mt-1 text-sm">
          Số <strong>{phone}</strong> đã được xác minh. Bước tạo trung tâm sẽ sớm
          khả dụng (Phase 2).
        </p>
      </div>
    );
  }

  const phoneValid = PHONE_RE.test(phone);

  return (
    <div className="space-y-5">
      {error && (
        <Alert variant="destructive" aria-live="assertive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {step === 'phone' && (
        <form
          onSubmit={(e: FormEvent) => {
            e.preventDefault();
            if (!loading && cooldown === 0) requestOtp(true);
          }}
          className="space-y-4"
          aria-label="signup-phone-form"
        >
          <div className="space-y-1.5">
            <Label htmlFor="signup-phone">Số điện thoại</Label>
            <div className="relative">
              <Phone
                className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground"
                aria-hidden
              />
              <Input
                id="signup-phone"
                name="phone"
                type="tel"
                inputMode="numeric"
                autoComplete="tel"
                placeholder="0901234567"
                value={phone}
                onChange={(e) => setPhone(e.target.value.replace(/\D/g, ''))}
                maxLength={11}
                required
                aria-invalid={phone.length > 0 && !phoneValid}
                className="pl-9"
              />
            </div>
            <p className="text-xs text-muted-foreground">
              Chúng tôi sẽ gửi mã xác thực 6 số qua Zalo.
            </p>
          </div>

          <Button
            type="submit"
            size="lg"
            className="w-full"
            disabled={loading || !phoneValid || cooldown > 0}
          >
            {loading && <Loader2 className="h-4 w-4 animate-spin" aria-hidden />}
            {cooldown > 0 ? `Thử lại sau ${cooldown} giây` : 'Gửi mã'}
          </Button>
        </form>
      )}

      {step === 'otp' && (
        <form
          onSubmit={(e: FormEvent) => {
            e.preventDefault();
            if (!loading) verifyOtp();
          }}
          className="space-y-4"
          aria-label="signup-otp-form"
        >
          <div className="space-y-1.5">
            <Label htmlFor="signup-otp">Mã xác thực</Label>
            <Input
              ref={otpInputRef}
              id="signup-otp"
              name="otp"
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              pattern="\d{6}"
              placeholder="● ● ● ● ● ●"
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, OTP_LEN))}
              maxLength={OTP_LEN}
              required
              className="text-center text-lg tracking-[0.5em]"
            />
            <p className="text-xs text-muted-foreground">
              Đã gửi mã tới <strong>{phone}</strong> qua {channel}.{' '}
              {otpExpiry > 0 ? (
                <span>Mã hết hạn sau {formatMMSS(otpExpiry)}.</span>
              ) : (
                <span className="text-destructive">Mã đã hết hạn.</span>
              )}
            </p>
          </div>

          <Button
            type="submit"
            size="lg"
            className="w-full"
            disabled={loading || code.length !== OTP_LEN}
          >
            {loading && <Loader2 className="h-4 w-4 animate-spin" aria-hidden />}
            <ShieldCheck className="h-4 w-4" aria-hidden />
            Xác thực
          </Button>

          <div className="flex items-center justify-between text-sm">
            <button
              type="button"
              onClick={() => {
                setStep('phone');
                setError(null);
                setCode('');
              }}
              className="text-muted-foreground hover:text-foreground"
            >
              ← Đổi số khác
            </button>
            <button
              type="button"
              onClick={() => requestOtp(false)}
              disabled={loading || cooldown > 0}
              className="font-medium text-primary hover:underline disabled:opacity-50 disabled:no-underline"
            >
              {cooldown > 0 ? `Gửi lại sau ${cooldown}s` : 'Gửi lại mã'}
            </button>
          </div>

          {expiredHint && cooldown === 0 && (
            <p className="text-center text-xs text-muted-foreground">
              Không nhận được mã? Hãy nhấn &quot;Gửi lại mã&quot;.
            </p>
          )}
        </form>
      )}
    </div>
  );
}
