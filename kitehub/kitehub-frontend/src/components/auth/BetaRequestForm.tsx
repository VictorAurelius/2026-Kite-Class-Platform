/**
 * BetaRequestForm — submission form cho `/auth/request-beta-access`.
 *
 * Phase 1 BETA invite-only model (GAP-372 Wave 33). User submits → coordinator
 * approves → invite email với signup token. Includes anti-spam honeypot
 * (hidden text field expected to remain empty).
 *
 * @since Wave 33 — GAP-372
 */
'use client';

import { useState, FormEvent } from 'react';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';

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
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const validate = (): string | null => {
    if (!email.trim() || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
      return 'Email không hợp lệ.';
    }
    if (!name.trim()) return 'Vui lòng nhập họ tên.';
    if (!orgName.trim()) return 'Vui lòng nhập tên tổ chức.';
    return null;
  };

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    const v = validate();
    if (v) {
      setError(v);
      return;
    }
    setLoading(true);
    try {
      await apiClient.post(endpoints.auth.requestBetaAccess, {
        email,
        name,
        orgName,
        persona,
        referralSource: referralSource || null,
        honeypot,
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

      <button
        type="submit"
        disabled={loading}
        className="w-full rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground disabled:opacity-60"
      >
        {loading ? 'Đang gửi...' : 'Gửi yêu cầu beta'}
      </button>
    </form>
  );
}
