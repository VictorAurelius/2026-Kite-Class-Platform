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
import { useRouter } from 'next/navigation';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import { HelpLink } from '@/components/support/HelpLink';

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
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [orgName, setOrgName] = useState('');
  const [persona, setPersona] = useState<BetaPersona>('P2_CENTER_OWNER');
  const [referralSource, setReferralSource] = useState('');
  // Wave beta-prep-1 Bucket F7 — multi-branch filter per ADR-036.
  // Phase 1 BETA scope = single-branch tenants only; multi-branch redirects to waitlist.
  const [branchCount, setBranchCount] = useState<number>(1);
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
    // Wave beta-prep-1 Bucket F7 — multi-branch filter per ADR-036.
    // branchCount must be ≥ 1 integer; > 1 triggers waitlist redirect (handled in onSubmit).
    if (!Number.isInteger(branchCount) || branchCount < 1) {
      return 'Số chi nhánh phải là số nguyên ≥ 1.';
    }
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
    // Wave beta-prep-1 Bucket F7 — multi-branch filter redirect per ADR-036.
    // Phase 1 BETA invite-only scope = single-branch tenants only. Multi-branch
    // tenants get a polite waitlist page explaining Phase 1.5 timeline instead
    // of submitting to the BE (which would still accept but Phase 1 ops cannot
    // support multi-branch features yet). Server-side filter mirror tracked
    // in follow-up GAP — defense-in-depth pending.
    if (branchCount > 1) {
      router.push(`/waitlist?reason=multi-branch&branches=${branchCount}`);
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
        branchCount,
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

      {/*
        Wave beta-prep-1 Bucket F7 — multi-branch filter per ADR-036.
        Phase 1 BETA scope: single-branch tenants only. Values > 1 redirect to
        /waitlist?reason=multi-branch in onSubmit (FE-side filter). Server-side
        mirror tracked in follow-up gap (defense-in-depth).
      */}
      <div>
        <label
          htmlFor="beta-branch-count"
          className="flex items-center text-sm font-medium mb-1.5"
        >
          Số chi nhánh trung tâm
          <HelpLink topic="branch" inline />
        </label>
        <input
          id="beta-branch-count"
          type="number"
          min={1}
          max={50}
          step={1}
          value={branchCount}
          onChange={(e) =>
            setBranchCount(Math.max(1, parseInt(e.target.value, 10) || 1))
          }
          className="w-full rounded-xl border bg-background px-4 py-3 text-sm"
          required
          data-testid="beta-branch-count"
          aria-describedby="beta-branch-help"
        />
        <p id="beta-branch-help" className="mt-1.5 text-xs text-muted-foreground">
          Phase 1 BETA chỉ hỗ trợ trung tâm 1 chi nhánh. Trung tâm nhiều chi nhánh
          sẽ được mời ở Phase 1.5 (dự kiến Q3 2026) — vẫn vui lòng điền số thực
          tế để chúng tôi liên hệ đúng thời điểm.
        </p>
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
          <HelpLink topic="consent" inline />
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
