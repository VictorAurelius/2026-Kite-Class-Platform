/**
 * Web Push permission inline explanation — full-page version surfaced at
 * /parent/settings#web-push for users who tap the deep link from the
 * push-notification-card.
 *
 * Wave 49 Bucket A (GAP-267). Ports `web-push-permission.html`.
 */

'use client';

import { useEffect, useState } from 'react';
import { Bell, ShieldCheck, Smartphone } from 'lucide-react';
import {
  getSubscriptionState,
  isPushSupported,
  subscribe,
  unsubscribe,
  type WebPushSubscribeResult,
} from '@/lib/web-push';

export function WebPushPermission() {
  const [supported, setSupported] = useState(true);
  const [state, setState] =
    useState<'subscribed' | 'not-subscribed' | 'denied' | 'unknown'>('unknown');
  const [busy, setBusy] = useState(false);
  const [last, setLast] = useState<WebPushSubscribeResult | null>(null);

  useEffect(() => {
    const ok = isPushSupported();
    setSupported(ok);
    if (!ok) return;
    getSubscriptionState().then((s) => {
      if (s === 'unsupported') {
        setSupported(false);
        return;
      }
      setState(s);
    });
  }, []);

  async function handleClick() {
    setBusy(true);
    try {
      const result = state === 'subscribed' ? await unsubscribe() : await subscribe();
      setLast(result);
      if (result.outcome === 'subscribed') setState('subscribed');
      else if (result.outcome === 'unsubscribed') setState('not-subscribed');
      else if (result.outcome === 'denied') setState('denied');
    } finally {
      setBusy(false);
    }
  }

  return (
    <section
      id="web-push"
      className="mx-4 mt-4 rounded-2xl border bg-card p-5 shadow-sm"
      aria-labelledby="parent-web-push-title"
      data-testid="parent-web-push-permission"
    >
      <span
        className="mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-primary/10 text-primary"
        aria-hidden
      >
        <Bell className="h-6 w-6" />
      </span>
      <h3 id="parent-web-push-title" className="text-base font-bold">
        Bật thông báo trình duyệt
      </h3>
      <p className="mt-1 text-xs text-muted-foreground">
        Web Push giúp bạn nhận thông báo từ KiteClass mà không cần mở ứng dụng.
        Đây là phương án phụ — Zalo OA vẫn là kênh chính ở Việt Nam.
      </p>

      <ul className="mt-4 space-y-2 text-sm">
        <li className="flex items-start gap-2">
          <ShieldCheck
            className="mt-0.5 h-4 w-4 flex-shrink-0 text-emerald-600"
            aria-hidden
          />
          <span>
            <strong>Bảo mật:</strong> chỉ KiteClass mới gửi được thông báo qua
            kênh này.
          </span>
        </li>
        <li className="flex items-start gap-2">
          <Smartphone
            className="mt-0.5 h-4 w-4 flex-shrink-0 text-sky-600"
            aria-hidden
          />
          <span>
            <strong>Ngoại lệ iOS:</strong> iPhone cần Safari 16.4 trở lên + đã
            cài đặt PWA vào màn hình chính.
          </span>
        </li>
      </ul>

      {!supported && (
        <p
          className="mt-4 rounded-xl bg-muted p-3 text-xs"
          role="alert"
          data-testid="parent-web-push-unsupported"
        >
          Trình duyệt hiện tại không hỗ trợ Web Push. Vui lòng dùng Chrome,
          Firefox, hoặc Safari mới hơn.
        </p>
      )}

      {supported && (
        <button
          type="button"
          onClick={handleClick}
          disabled={busy || state === 'denied'}
          className="mt-4 flex h-12 w-full items-center justify-center rounded-2xl bg-primary text-sm font-bold text-primary-foreground transition-colors hover:bg-primary/90 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-60"
          data-testid="parent-web-push-cta"
        >
          {busy
            ? 'Đang xử lý…'
            : state === 'subscribed'
              ? 'Tắt thông báo trình duyệt'
              : 'Bật thông báo trình duyệt'}
        </button>
      )}

      {last?.error && (
        <p
          role="alert"
          className="mt-2 text-[11px] text-red-700"
          data-testid="parent-web-push-error"
        >
          {last.error}
        </p>
      )}

      {state === 'denied' && (
        <p
          className="mt-3 text-[11px] text-amber-700"
          data-testid="parent-web-push-denied"
        >
          Bạn đã chặn quyền thông báo. Mở Cài đặt → KiteClass → Thông báo để
          bật lại.
        </p>
      )}
    </section>
  );
}
