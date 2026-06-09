/**
 * ContactForm — KiteHub PLATFORM sales lead submit form (GAP-1101).
 *
 * Client component: VN-localized form (Họ và tên / Email / Số điện thoại /
 * Tên trung tâm / Nội dung cần tư vấn) → POST /api/platform/sales-leads (public,
 * no auth). On success shows a confirmation state.
 *
 * `planInterest` is passed from the server page (parsed from ?plan query) so
 * this component does NOT call useSearchParams (avoids Suspense bailout at
 * production build per .claude/rules/fe-build-local-verify.md).
 *
 * Anti-spam: hidden honeypot field (must stay empty) + FE debounce + maxLength
 * caps. Server enforces the canonical guard.
 */
'use client';

import { useState, useRef, FormEvent } from 'react';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';

const SUBMIT_DEBOUNCE_MS = 1000;

export interface ContactFormProps {
  /** Plan the prospect is interested in (from ?plan query). Defaults ENTERPRISE. */
  planInterest: string;
}

export function ContactForm({ planInterest }: ContactFormProps) {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [organizationName, setOrganizationName] = useState('');
  const [message, setMessage] = useState('');
  const [honeypot, setHoneypot] = useState('');
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const lastSubmitAtRef = useRef<number>(0);

  const validate = (): string | null => {
    if (!fullName.trim()) return 'Vui lòng nhập họ và tên.';
    if (!email.trim() || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
      return 'Email không hợp lệ.';
    }
    if (!phone.trim() || !/^[0-9+().\s-]{8,20}$/.test(phone)) {
      return 'Số điện thoại không hợp lệ (8-20 chữ số).';
    }
    if (!organizationName.trim()) return 'Vui lòng nhập tên trung tâm.';
    return null;
  };

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    const nowMs = Date.now();
    if (nowMs - lastSubmitAtRef.current < SUBMIT_DEBOUNCE_MS) {
      return;
    }
    if (loading) return;
    const v = validate();
    if (v) {
      setError(v);
      return;
    }
    lastSubmitAtRef.current = nowMs;
    setLoading(true);
    try {
      await apiClient.post(endpoints.salesLeads.create, {
        fullName,
        email,
        phone,
        organizationName,
        message: message || null,
        planInterest,
        honeypot,
      });
      setSubmitted(true);
    } catch {
      setError('Gửi yêu cầu thất bại. Vui lòng thử lại sau hoặc email support@kitehub.me.');
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
        <h2 className="text-lg font-semibold">Đã gửi yêu cầu!</h2>
        <p className="mt-2">
          Cảm ơn bạn đã quan tâm KiteHub. Đội ngũ KiteHub sẽ liên hệ tư vấn trong
          vòng 24 giờ qua email hoặc số điện thoại bạn cung cấp.
        </p>
      </div>
    );
  }

  return (
    <form onSubmit={onSubmit} noValidate className="space-y-5" aria-label="contact-sales-form">
      {error && (
        <div
          role="alert"
          className="rounded-xl bg-destructive/10 p-4 text-sm text-destructive"
        >
          {error}
        </div>
      )}

      <div>
        <label htmlFor="contact-name" className="block text-sm font-medium mb-1.5">
          Họ và tên
        </label>
        <input
          id="contact-name"
          value={fullName}
          onChange={(e) => setFullName(e.target.value)}
          className="w-full rounded-xl border bg-background px-4 py-3 text-sm"
          required
          maxLength={200}
          placeholder="Nguyễn Văn An"
        />
      </div>

      <div>
        <label htmlFor="contact-email" className="block text-sm font-medium mb-1.5">
          Email
        </label>
        <input
          id="contact-email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="w-full rounded-xl border bg-background px-4 py-3 text-sm"
          required
          maxLength={320}
          placeholder="an.nguyen@trungtam.edu.vn"
        />
      </div>

      <div>
        <label htmlFor="contact-phone" className="block text-sm font-medium mb-1.5">
          Số điện thoại
        </label>
        <input
          id="contact-phone"
          type="tel"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
          className="w-full rounded-xl border bg-background px-4 py-3 text-sm"
          required
          maxLength={20}
          placeholder="0901 234 567"
        />
      </div>

      <div>
        <label htmlFor="contact-org" className="block text-sm font-medium mb-1.5">
          Tên trung tâm
        </label>
        <input
          id="contact-org"
          value={organizationName}
          onChange={(e) => setOrganizationName(e.target.value)}
          className="w-full rounded-xl border bg-background px-4 py-3 text-sm"
          required
          maxLength={200}
          placeholder="Trung tâm Anh ngữ Sky Education"
        />
      </div>

      <div>
        <label htmlFor="contact-message" className="block text-sm font-medium mb-1.5">
          Nội dung cần tư vấn (tuỳ chọn)
        </label>
        <textarea
          id="contact-message"
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          className="w-full rounded-xl border bg-background px-4 py-3 text-sm"
          rows={4}
          maxLength={2000}
          placeholder="Quy mô trung tâm, số chi nhánh, nhu cầu cụ thể..."
        />
      </div>

      {/* Honeypot — must remain empty; bots fill it */}
      <input
        type="text"
        name="company_website"
        value={honeypot}
        onChange={(e) => setHoneypot(e.target.value)}
        aria-hidden="true"
        tabIndex={-1}
        autoComplete="off"
        style={{ position: 'absolute', left: '-9999px', width: 0, height: 0 }}
        data-testid="contact-honeypot"
      />

      <button
        type="submit"
        disabled={loading}
        data-testid="contact-submit"
        className="w-full rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground disabled:opacity-60"
      >
        {loading ? 'Đang gửi...' : 'Gửi yêu cầu tư vấn'}
      </button>
    </form>
  );
}
