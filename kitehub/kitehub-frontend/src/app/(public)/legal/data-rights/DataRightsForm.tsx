'use client';

import { useState, type FormEvent } from 'react';

/**
 * DSAR (Data Subject Access Request) self-service intake form — KiteHub.
 *
 * Phase 2 v1 (Wave 26 Bucket A, GAP-353c) — Vietnamese-first, EN deferred to GAP-182 Phase 2.
 *
 * Per BR-PDPL-DSAR-001..005 in `documents/01-business/kitehub/marketing/rules.md`:
 * - 6 PDPL Art 14 rights (radio)
 * - Identity: requester_name + requester_email + national_id_last4 (4 digits)
 * - Optional: scope free text + reason + contact preference
 * - Honeypot field (`companyWebsite`) — kept hidden via Tailwind `hidden` + tabindex=-1
 *
 * Posts to `${NEXT_PUBLIC_API_URL}/api/v1/dsar/request` (gateway :9000 in dev);
 * on success shows ticket UUID + 20-day SLA. GAP-1438: must hit the gateway, NOT
 * the FE origin (`next.config` has no proxy `rewrites()`), and must surface a
 * status-specific Vietnamese message instead of dumping a raw HTML error body.
 */

interface FormState {
  rightType: string;
  requesterName: string;
  requesterEmail: string;
  nationalIdLast4: string;
  scope: string;
  reason: string;
  contactPreference: string;
  companyWebsite: string; // honeypot
}

const RIGHT_OPTIONS: { value: string; label: string; description: string }[] = [
  { value: 'ACCESS', label: 'Quyền truy cập', description: 'Yêu cầu xem dữ liệu cá nhân mà chúng tôi đang lưu trữ.' },
  { value: 'RECTIFICATION', label: 'Quyền chỉnh sửa', description: 'Yêu cầu sửa thông tin sai hoặc không đầy đủ.' },
  { value: 'ERASURE', label: 'Quyền xoá', description: 'Yêu cầu xoá dữ liệu cá nhân (right to be forgotten).' },
  { value: 'PORTABILITY', label: 'Quyền chuyển dữ liệu', description: 'Yêu cầu xuất bản dữ liệu ở định dạng máy đọc được.' },
  { value: 'RESTRICT', label: 'Quyền hạn chế xử lý', description: 'Yêu cầu tạm dừng xử lý dữ liệu.' },
  { value: 'OBJECT', label: 'Quyền phản đối xử lý', description: 'Phản đối việc sử dụng dữ liệu cho mục đích cụ thể.' },
];

// GAP-1438: mirror beta-status.ts — DSAR is a public endpoint reached via the
// gateway (:9000 in dev, NEXT_PUBLIC_API_URL in prod), NOT the FE origin.
const DSAR_ENDPOINT = '/api/v1/dsar/request';

/**
 * Maps a failed DSAR submit response to a friendly Vietnamese message.
 * Never returns the raw response body (could be a 404 HTML page) — only a
 * status-specific message, optionally enriched by a JSON `message`/`errorCode`.
 */
async function describeError(res: Response): Promise<string> {
  // Only trust the body when the server says it is JSON; an HTML 404 page must
  // never leak into the alert (GAP-1438, sibling generic-catch class GAP-926).
  const contentType = res.headers.get('content-type') ?? '';
  let detail = '';
  if (contentType.includes('application/json')) {
    try {
      const body = (await res.json()) as { message?: string; errorCode?: string };
      detail = body.message?.trim() ?? '';
    } catch {
      detail = '';
    }
  }
  switch (res.status) {
    case 400:
      return detail || 'Thông tin gửi lên không hợp lệ. Vui lòng kiểm tra lại các trường bắt buộc.';
    case 401:
    case 403:
      return 'Bạn không có quyền thực hiện yêu cầu này. Vui lòng thử lại sau hoặc liên hệ DPO.';
    case 404:
      return 'Không tìm thấy dịch vụ xử lý yêu cầu (lỗi định tuyến). Vui lòng thử lại sau ít phút.';
    case 429:
      return 'Bạn đã gửi quá nhiều yêu cầu. Vui lòng đợi một lát rồi thử lại.';
    default:
      if (res.status >= 500) {
        return 'Hệ thống đang gặp sự cố, vui lòng thử lại sau ít phút.';
      }
      return detail || `Yêu cầu không thành công (mã lỗi ${res.status}). Vui lòng thử lại.`;
  }
}

const initialState: FormState = {
  rightType: 'ACCESS',
  requesterName: '',
  requesterEmail: '',
  nationalIdLast4: '',
  scope: '',
  reason: '',
  contactPreference: 'email',
  companyWebsite: '',
};

interface SuccessState {
  ticketId: string;
  status: string;
  slaDeadline: string;
}

export function DataRightsForm() {
  const [state, setState] = useState<FormState>(initialState);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<SuccessState | null>(null);

  function handleChange(field: keyof FormState, value: string) {
    setState((s) => ({ ...s, [field]: value }));
  }

  function validate(s: FormState): string | null {
    if (!s.rightType) return 'Vui lòng chọn quyền muốn thực hiện.';
    if (!s.requesterName.trim()) return 'Vui lòng nhập họ và tên.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(s.requesterEmail)) return 'Email không hợp lệ.';
    if (!/^[0-9]{4}$/.test(s.nationalIdLast4)) return 'CCCD/CMND 4 chữ số cuối phải đúng 4 chữ số.';
    return null;
  }

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    const v = validate(state);
    if (v) {
      setError(v);
      return;
    }
    setSubmitting(true);
    try {
      const baseURL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9000';
      const res = await fetch(`${baseURL}${DSAR_ENDPOINT}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          rightType: state.rightType,
          requesterName: state.requesterName,
          requesterEmail: state.requesterEmail,
          nationalIdLast4: state.nationalIdLast4,
          scope: state.scope || null,
          reason: state.reason || null,
          contactPreference: state.contactPreference,
          companyWebsite: state.companyWebsite, // honeypot
        }),
      });
      if (!res.ok) {
        setError(await describeError(res));
        return;
      }
      const body = (await res.json()) as SuccessState;
      setSuccess(body);
      setState(initialState);
    } catch {
      // Network / CORS / parse failure — no raw body leaked.
      setError('Không thể kết nối tới máy chủ. Vui lòng kiểm tra kết nối mạng và thử lại.');
    } finally {
      setSubmitting(false);
    }
  }

  if (success) {
    return (
      <div
        role="status"
        aria-live="polite"
        className="rounded-md border border-emerald-200 bg-emerald-50 p-6 text-emerald-900 dark:border-emerald-800 dark:bg-emerald-950/30 dark:text-emerald-200"
      >
        <h2 className="text-lg font-semibold">Đã ghi nhận yêu cầu DSAR</h2>
        <p className="mt-2">Mã yêu cầu của bạn (xin lưu lại để tra cứu sau):</p>
        <p className="mt-1 break-all font-mono text-sm">{success.ticketId}</p>
        <p className="mt-3 text-sm">
          DPO sẽ phản hồi trong vòng tối đa <strong>20 ngày</strong> theo Điều 14 Nghị định 13/2023/NĐ-CP. Hạn xử
          lý dự kiến: {new Date(success.slaDeadline).toLocaleDateString('vi-VN')}.
        </p>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6" noValidate>
      <fieldset className="space-y-3">
        <legend className="text-base font-semibold">Quyền bạn muốn thực hiện</legend>
        <div className="space-y-2">
          {RIGHT_OPTIONS.map((opt) => (
            <label key={opt.value} className="flex cursor-pointer items-start gap-3 rounded-md border p-3 hover:bg-muted/40">
              <input
                type="radio"
                name="rightType"
                value={opt.value}
                checked={state.rightType === opt.value}
                onChange={(e) => handleChange('rightType', e.target.value)}
                className="mt-1"
                required
              />
              <span className="flex-1">
                <span className="block font-medium">{opt.label}</span>
                <span className="block text-sm text-muted-foreground">{opt.description}</span>
              </span>
            </label>
          ))}
        </div>
      </fieldset>

      <div className="grid gap-4 sm:grid-cols-2">
        <label className="flex flex-col gap-1 text-sm">
          <span className="font-medium">Họ và tên *</span>
          <input
            type="text"
            value={state.requesterName}
            onChange={(e) => handleChange('requesterName', e.target.value)}
            maxLength={200}
            required
            className="rounded-md border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
          />
        </label>

        <label className="flex flex-col gap-1 text-sm">
          <span className="font-medium">Email *</span>
          <input
            type="email"
            value={state.requesterEmail}
            onChange={(e) => handleChange('requesterEmail', e.target.value)}
            maxLength={320}
            required
            autoComplete="email"
            className="rounded-md border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
          />
        </label>

        <label className="flex flex-col gap-1 text-sm">
          <span className="font-medium">4 chữ số cuối CCCD/CMND *</span>
          <input
            type="text"
            inputMode="numeric"
            pattern="[0-9]{4}"
            value={state.nationalIdLast4}
            onChange={(e) => handleChange('nationalIdLast4', e.target.value.replace(/[^0-9]/g, '').slice(0, 4))}
            maxLength={4}
            required
            aria-describedby="nationalId-help"
            className="rounded-md border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
          />
          <span id="nationalId-help" className="text-xs text-muted-foreground">
            Dùng để xác minh danh tính. Không thu thập số đầy đủ.
          </span>
        </label>

        <label className="flex flex-col gap-1 text-sm">
          <span className="font-medium">Phương thức liên hệ ưu tiên</span>
          <select
            value={state.contactPreference}
            onChange={(e) => handleChange('contactPreference', e.target.value)}
            className="rounded-md border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option value="email">Email</option>
            <option value="phone">Điện thoại (DPO sẽ gọi lại)</option>
          </select>
        </label>
      </div>

      <label className="flex flex-col gap-1 text-sm">
        <span className="font-medium">Phạm vi dữ liệu (tuỳ chọn)</span>
        <textarea
          value={state.scope}
          onChange={(e) => handleChange('scope', e.target.value)}
          maxLength={4000}
          rows={3}
          placeholder="VD: Tất cả dữ liệu marketing, thông tin profile, lịch sử thanh toán..."
          className="rounded-md border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
        />
      </label>

      <label className="flex flex-col gap-1 text-sm">
        <span className="font-medium">Lý do (tuỳ chọn)</span>
        <textarea
          value={state.reason}
          onChange={(e) => handleChange('reason', e.target.value)}
          maxLength={4000}
          rows={3}
          placeholder="Bạn có thể bổ sung lý do hoặc bối cảnh để DPO xử lý nhanh hơn."
          className="rounded-md border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
        />
      </label>

      {/* Honeypot — hidden from real users; bots blindly fill all fields */}
      <div aria-hidden="true" className="hidden" style={{ position: 'absolute', left: '-9999px' }}>
        <label>
          Company Website
          <input
            type="text"
            tabIndex={-1}
            autoComplete="off"
            value={state.companyWebsite}
            onChange={(e) => handleChange('companyWebsite', e.target.value)}
          />
        </label>
      </div>

      {error && (
        <div role="alert" className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-900 dark:border-red-800 dark:bg-red-950/30 dark:text-red-200">
          {error}
        </div>
      )}

      <button
        type="submit"
        disabled={submitting}
        className="inline-flex items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow disabled:cursor-not-allowed disabled:opacity-60 hover:bg-primary/90"
      >
        {submitting ? 'Đang gửi...' : 'Gửi yêu cầu DSAR'}
      </button>

      <p className="text-xs text-muted-foreground">
        Bằng việc gửi, bạn xác nhận thông tin trên là chính xác. DPO sẽ phản hồi trong tối đa 20 ngày làm việc theo
        Điều 14 Nghị định 13/2023/NĐ-CP. Bản ghi yêu cầu được giữ trong 36 tháng theo BR-PDPL-DSAR-004 trước khi xoá.
      </p>
    </form>
  );
}
