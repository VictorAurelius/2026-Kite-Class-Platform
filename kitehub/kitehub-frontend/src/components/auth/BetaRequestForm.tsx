/**
 * BetaRequestForm — submission form cho `/auth/request-beta-access`.
 *
 * Phase 1 BETA invite-only model (GAP-372 Wave 33). User submits → coordinator
 * approves → invite email với signup token. Includes anti-spam honeypot
 * (hidden text field expected to remain empty).
 *
 * Wave 35 GAP-385: thêm PDPL 2023 Art 11 explicit consent checkbox. Submit
 * button DISABLED đến khi user check consent. `consentGiven=true` send trong
 * POST body — server reject với HTTP 400 + error `BETA_CONSENT_REQUIRED` nếu
 * thiếu hoặc false.
 *
 * Wave 105 Bucket A (failure-mode matrix A1 double-submit hardening): FE
 * debounce 1s + in-flight guard. Pair với BE V55 partial unique index on
 * `beta_access_request(email) WHERE status='PENDING'` cho defense in depth.
 *
 * @since Wave 33 — GAP-372
 */
'use client';

import { useState, useRef, FormEvent } from 'react';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';

/**
 * Wave 105 Bucket A (failure-mode matrix A1) — FE button debounce window in ms.
 *
 * Why 1000ms: typical double-click latency ~200-500ms; 1s safely covers the
 * race window between first POST dispatch and server response without making
 * the form feel sluggish for honest single-clickers.
 */
const SUBMIT_DEBOUNCE_MS = 1000;

export type BetaPersona = 'P1_SOLO_TEACHER' | 'P2_CENTER_OWNER';

export interface BetaRequestFormProps {
  onSuccess?: () => void;
}

export default function BetaRequestForm({ onSuccess }: BetaRequestFormProps) {
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [orgName, setOrgName] = useState('');
  const [persona, setPersona] = useState<BetaPersona>('P2_CENTER_OWNER');
  const [referralSource, setReferralSource] = useState('');
  const [honeypot, setHoneypot] = useState('');
  const [consentGiven, setConsentGiven] = useState(false);
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // Wave 105 Bucket A — A1 debounce. Tracks last submit dispatch time; rejects
  // re-submits within SUBMIT_DEBOUNCE_MS window. useRef to avoid stale-closure
  // issues and to skip re-render churn.
  const lastSubmitAtRef = useRef<number>(0);

  const validate = (): string | null => {
    if (!email.trim() || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
      return 'Email không hợp lệ.';
    }
    if (!name.trim()) return 'Vui lòng nhập họ tên.';
    if (!orgName.trim()) return 'Vui lòng nhập tên tổ chức.';
    if (!consentGiven) {
      return 'Vui lòng đồng ý với Chính sách Quyền riêng tư và Điều khoản Sử dụng.';
    }
    return null;
  };

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    // Wave 105 Bucket A (A1) — FE debounce window. Bail silently on rapid
    // re-submit; BE V55 partial unique index is the canonical guard for
    // server-side double-submits but FE bounce eliminates the round trip.
    const nowMs = Date.now();
    if (nowMs - lastSubmitAtRef.current < SUBMIT_DEBOUNCE_MS) {
      return;
    }
    if (loading) {
      // Hard guard against in-flight resubmit (covers Enter-key spam during
      // POST in flight; loading state takes ~1 React tick to propagate).
      return;
    }
    const v = validate();
    if (v) {
      setError(v);
      return;
    }
    lastSubmitAtRef.current = nowMs;
    setLoading(true);
    try {
      await apiClient.post(endpoints.auth.requestBetaAccess, {
        email,
        name,
        orgName,
        persona,
        referralSource: referralSource || null,
        honeypot,
        consentGiven,
      });
      setSubmitted(true);
      onSuccess?.();
    } catch {
      setError('Gửi yêu cầu thất bại. Vui lòng thử lại sau.');
    } finally {
      setLoading(false);
    }
  };

  if (submitted) {
    return (
      <div
        role="status"
        className="rounded-xl bg-green-50 p-6 text-sm text-green-900"
      >
        <h2 className="text-lg font-semibold">Đã nhận yêu cầu beta</h2>
        <p className="mt-2">
          Đội ngũ KiteClass sẽ xem xét yêu cầu của bạn và gửi email mời kèm liên
          kết kích hoạt khi được duyệt. Token kích hoạt có hiệu lực 24 giờ.
        </p>
      </div>
    );
  }

  return (
    <form onSubmit={onSubmit} className="space-y-5" aria-label="beta-request-form">
      {error && (
        <div
          role="alert"
          className="rounded-xl bg-destructive/10 p-4 text-sm text-destructive"
        >
          {error}
        </div>
      )}

      <div>
        <label htmlFor="beta-email" className="block text-sm font-medium mb-1.5">
          Email
        </label>
        <input
          id="beta-email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="w-full rounded-xl border bg-background px-4 py-3 text-sm"
          required
          maxLength={320}
        />
      </div>

      <div>
        <label htmlFor="beta-name" className="block text-sm font-medium mb-1.5">
          Họ và tên
        </label>
        <input
          id="beta-name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className="w-full rounded-xl border bg-background px-4 py-3 text-sm"
          required
          maxLength={200}
        />
      </div>

      <div>
        <label htmlFor="beta-org" className="block text-sm font-medium mb-1.5">
          Tên tổ chức / trung tâm
        </label>
        <input
          id="beta-org"
          value={orgName}
          onChange={(e) => setOrgName(e.target.value)}
          className="w-full rounded-xl border bg-background px-4 py-3 text-sm"
          required
          maxLength={200}
        />
      </div>

      <div>
        <label htmlFor="beta-persona" className="block text-sm font-medium mb-1.5">
          Vai trò
        </label>
        <select
          id="beta-persona"
          value={persona}
          onChange={(e) => setPersona(e.target.value as BetaPersona)}
          className="w-full rounded-xl border bg-background px-4 py-3 text-sm"
        >
          <option value="P1_SOLO_TEACHER">P1 — Giáo viên độc lập</option>
          <option value="P2_CENTER_OWNER">P2 — Chủ trung tâm nhỏ</option>
        </select>
      </div>

      <div>
        <label htmlFor="beta-referral" className="block text-sm font-medium mb-1.5">
          Nguồn giới thiệu (tuỳ chọn)
        </label>
        <input
          id="beta-referral"
          value={referralSource}
          onChange={(e) => setReferralSource(e.target.value)}
          className="w-full rounded-xl border bg-background px-4 py-3 text-sm"
          maxLength={500}
          placeholder="Bạn biết KiteClass qua đâu?"
        />
      </div>

      {/* Honeypot — must remain empty; bots fill it */}
      <input
        type="text"
        name="honeypot"
        value={honeypot}
        onChange={(e) => setHoneypot(e.target.value)}
        aria-hidden="true"
        tabIndex={-1}
        autoComplete="off"
        style={{ position: 'absolute', left: '-9999px', width: 0, height: 0 }}
        data-testid="beta-honeypot"
      />

      {/* PDPL 2023 Art 11 — explicit consent (Wave 35 GAP-385) */}
      <div className="flex items-start gap-2 rounded-xl border bg-muted/30 p-3 text-sm">
        <input
          id="beta-consent"
          type="checkbox"
          checked={consentGiven}
          onChange={(e) => setConsentGiven(e.target.checked)}
          className="mt-1 h-4 w-4"
          required
          data-testid="beta-consent-checkbox"
        />
        <label htmlFor="beta-consent" className="leading-snug">
          Tôi đồng ý cho Kite xử lý dữ liệu cá nhân của tôi theo{' '}
          <a href="/legal/privacy" className="underline" target="_blank" rel="noopener noreferrer">
            Chính sách Quyền riêng tư
          </a>{' '}
          và{' '}
          <a href="/legal/terms" className="underline" target="_blank" rel="noopener noreferrer">
            Điều khoản Sử dụng
          </a>{' '}
          (PDPL 2023).
        </label>
      </div>

      <button
        type="submit"
        disabled={loading || !consentGiven}
        data-testid="beta-submit"
        className="w-full rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground disabled:opacity-60"
      >
        {loading ? 'Đang gửi...' : 'Gửi yêu cầu beta'}
      </button>
    </form>
  );
}
