/**
 * Contact form — lazy-loaded body of the per-tenant `/contact` page (GAP-274 phase-2).
 *
 * VN-realistic validation per kiteclass-public kit spec (plain controlled state — the
 * kit validates with vanilla JS; replicated 1:1 here for deterministic behaviour):
 *   - Họ tên : required, ≥2 ký tự sau trim
 *   - SĐT    : required, /^0\d{9}$/ (lọc non-digit, maxLength 10)
 *   - Email  : OPTIONAL, nếu nhập phải hợp lệ
 *   - Lời nhắn: required, ≥10 ký tự sau trim
 * Inline error + aria-invalid + aria-describedby + role="alert" + focus field lỗi đầu
 * tiên; xóa lỗi khi user sửa; submit → success panel (không reload).
 *
 * Wired to publicApi.submitContactForm → POST /api/v1/contact (real BE endpoint).
 *
 * @author KiteClass Team
 */

'use client';

import { useRef, useState } from 'react';
import { CheckCircle } from 'lucide-react';
import { publicApi } from '@/lib/api/public';

const PHONE_RE = /^0\d{9}$/;
const EMAIL_RE = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

type Field = 'name' | 'phone' | 'email' | 'message';
type Errors = Partial<Record<Field, string>>;

const MESSAGES: Record<Field, string> = {
  name: 'Vui lòng nhập họ tên (ít nhất 2 ký tự).',
  phone: 'Số điện thoại phải gồm đúng 10 chữ số và bắt đầu bằng 0.',
  email: 'Email không hợp lệ. Để trống nếu không có.',
  message: 'Vui lòng nhập lời nhắn (ít nhất 10 ký tự).',
};

const ORDER: Field[] = ['name', 'phone', 'email', 'message'];

export function ContactForm() {
  const [values, setValues] = useState({ name: '', phone: '', email: '', message: '' });
  const [errors, setErrors] = useState<Errors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const refs: Record<Field, React.RefObject<HTMLInputElement | HTMLTextAreaElement | null>> = {
    name: useRef<HTMLInputElement>(null),
    phone: useRef<HTMLInputElement>(null),
    email: useRef<HTMLInputElement>(null),
    message: useRef<HTMLTextAreaElement>(null),
  };

  const validate = (v: typeof values): Errors => {
    const e: Errors = {};
    if (v.name.trim().length < 2) e.name = MESSAGES.name;
    if (!PHONE_RE.test(v.phone.trim())) e.phone = MESSAGES.phone;
    if (v.email.trim() !== '' && !EMAIL_RE.test(v.email.trim())) e.email = MESSAGES.email;
    if (v.message.trim().length < 10) e.message = MESSAGES.message;
    return e;
  };

  const setField = (field: Field, raw: string) => {
    const value = field === 'phone' ? raw.replace(/\D/g, '').slice(0, 10) : raw;
    setValues((prev) => ({ ...prev, [field]: value }));
    // Clear the field's error as the user edits it.
    setErrors((prev) => (prev[field] ? { ...prev, [field]: undefined } : prev));
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setSubmitError(null);
    const found = validate(values);
    if (Object.keys(found).length > 0) {
      setErrors(found);
      const first = ORDER.find((f) => found[f]);
      if (first) refs[first].current?.focus();
      return;
    }
    setErrors({});
    setIsSubmitting(true);
    try {
      await publicApi.submitContactForm({
        name: values.name.trim(),
        phone: values.phone.trim(),
        email: values.email.trim() || undefined,
        message: values.message.trim(),
      });
      setIsSuccess(true);
    } catch {
      setSubmitError('Không thể gửi lời nhắn lúc này. Vui lòng thử lại sau ít phút.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isSuccess) {
    return (
      <div className="rounded-2xl border bg-white p-7 text-center shadow-sm" role="status">
        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-emerald-50 text-emerald-600">
          <CheckCircle className="h-8 w-8" aria-hidden="true" />
        </div>
        <h2 className="mb-2 text-2xl font-extrabold">Đã gửi thành công!</h2>
        <p className="mx-auto max-w-sm text-muted-foreground">
          Cảm ơn anh/chị. Chúng tôi sẽ liên hệ lại trong hôm nay qua số điện thoại hoặc Zalo anh/chị
          để lại.
        </p>
      </div>
    );
  }

  const fieldClass = (field: Field) =>
    `w-full rounded-xl border bg-muted/40 px-3.5 py-3 text-sm outline-none focus:border-theme-primary focus:bg-white ${
      errors[field] ? 'border-destructive bg-red-50' : ''
    }`;

  return (
    <section className="rounded-2xl border bg-white p-7 shadow-sm" aria-labelledby="contact-form-title">
      <h2 id="contact-form-title" className="mb-1 text-xl font-extrabold">
        Đăng ký / gửi lời nhắn
      </h2>
      <p className="mb-5 text-sm text-muted-foreground">
        Trường có dấu <span className="text-destructive">*</span> là bắt buộc.
      </p>

      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        {/* Họ tên */}
        <div>
          <label htmlFor="name" className="mb-1.5 block text-sm font-bold">
            Họ và tên phụ huynh <span className="text-destructive">*</span>
          </label>
          <input
            id="name"
            ref={refs.name as React.RefObject<HTMLInputElement>}
            type="text"
            autoComplete="name"
            placeholder="VD: Trần Thị Hồng"
            value={values.name}
            onChange={(e) => setField('name', e.target.value)}
            aria-required="true"
            aria-invalid={errors.name ? 'true' : 'false'}
            aria-describedby={errors.name ? 'err-name' : undefined}
            className={fieldClass('name')}
          />
          {errors.name && (
            <p id="err-name" role="alert" className="mt-1.5 text-sm font-semibold text-destructive">
              ⚠ {errors.name}
            </p>
          )}
        </div>

        {/* SĐT */}
        <div>
          <label htmlFor="phone" className="mb-1.5 block text-sm font-bold">
            Số điện thoại <span className="text-destructive">*</span>
          </label>
          <input
            id="phone"
            ref={refs.phone as React.RefObject<HTMLInputElement>}
            type="tel"
            inputMode="numeric"
            maxLength={10}
            autoComplete="tel"
            placeholder="VD: 0912345678"
            value={values.phone}
            onChange={(e) => setField('phone', e.target.value)}
            aria-required="true"
            aria-invalid={errors.phone ? 'true' : 'false'}
            aria-describedby={`hint-phone${errors.phone ? ' err-phone' : ''}`}
            className={fieldClass('phone')}
          />
          <p id="hint-phone" className="mt-1.5 text-xs text-muted-foreground">
            Số di động 10 chữ số, bắt đầu bằng 0.
          </p>
          {errors.phone && (
            <p id="err-phone" role="alert" className="mt-1 text-sm font-semibold text-destructive">
              ⚠ {errors.phone}
            </p>
          )}
        </div>

        {/* Email (optional) */}
        <div>
          <label htmlFor="email" className="mb-1.5 block text-sm font-bold">
            Email <span className="text-xs font-semibold text-muted-foreground">(không bắt buộc)</span>
          </label>
          <input
            id="email"
            ref={refs.email as React.RefObject<HTMLInputElement>}
            type="email"
            autoComplete="email"
            placeholder="VD: hong.tran@gmail.com"
            value={values.email}
            onChange={(e) => setField('email', e.target.value)}
            aria-invalid={errors.email ? 'true' : 'false'}
            aria-describedby={errors.email ? 'err-email' : undefined}
            className={fieldClass('email')}
          />
          {errors.email && (
            <p id="err-email" role="alert" className="mt-1.5 text-sm font-semibold text-destructive">
              ⚠ {errors.email}
            </p>
          )}
        </div>

        {/* Lời nhắn */}
        <div>
          <label htmlFor="message" className="mb-1.5 block text-sm font-bold">
            Lời nhắn <span className="text-destructive">*</span>
          </label>
          <textarea
            id="message"
            ref={refs.message as React.RefObject<HTMLTextAreaElement>}
            rows={5}
            placeholder="VD: Cháu đang học lớp 5, hơi yếu phân số, nhờ tư vấn giúp lớp phù hợp ạ."
            value={values.message}
            onChange={(e) => setField('message', e.target.value)}
            aria-required="true"
            aria-invalid={errors.message ? 'true' : 'false'}
            aria-describedby={errors.message ? 'err-message' : undefined}
            className={`${fieldClass('message')} resize-y`}
          />
          {errors.message && (
            <p id="err-message" role="alert" className="mt-1.5 text-sm font-semibold text-destructive">
              ⚠ {errors.message}
            </p>
          )}
        </div>

        {submitError && (
          <p role="alert" className="text-sm font-semibold text-destructive">
            {submitError}
          </p>
        )}

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded-xl bg-theme-cta py-3 font-bold text-white disabled:opacity-60"
        >
          {isSubmitting ? 'Đang gửi...' : 'Gửi đăng ký'}
        </button>

        <p className="text-xs leading-relaxed text-muted-foreground">
          Bằng việc gửi, anh/chị đồng ý để chúng tôi liên hệ tư vấn. Thông tin chỉ dùng cho mục đích
          liên hệ, không chia sẻ cho bên thứ ba (tuân thủ PDPL).
        </p>
      </form>
    </section>
  );
}
