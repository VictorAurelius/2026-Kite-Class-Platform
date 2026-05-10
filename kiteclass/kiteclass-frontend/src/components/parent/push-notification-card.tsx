/**
 * Push notification card — Zalo OA primary + Web Push fallback.
 *
 * Wave 49 Bucket A (GAP-267). Ports `push-notification-card.html`. Per
 * `dossier/02-vietnamese-ux-musts.md` §5, Zalo OA reaches ~95% of VN parents
 * so it sits visually above Web Push (which iOS Safari pre-16.4 doesn't
 * support).
 */

'use client';

import { useEffect, useState } from 'react';
import { Bell, Check, MessageCircle } from 'lucide-react';
import {
  getSubscriptionState,
  isPushSupported,
  subscribe,
  unsubscribe,
  type WebPushOutcome,
} from '@/lib/web-push';

type SubState = 'subscribed' | 'not-subscribed' | 'denied' | 'unsupported' | 'unknown';

export function PushNotificationCard() {
  const [zaloLinked, setZaloLinked] = useState(true);
  const [webState, setWebState] = useState<SubState>('unknown');
  const [busy, setBusy] = useState(false);
  const [lastOutcome, setLastOutcome] = useState<WebPushOutcome | null>(null);

  useEffect(() => {
    if (!isPushSupported()) {
      setWebState('unsupported');
      return;
    }
    getSubscriptionState().then(setWebState).catch(() => setWebState('unknown'));
  }, []);

  async function handleToggleWeb() {
    if (busy) return;
    setBusy(true);
    try {
      if (webState === 'subscribed') {
        const result = await unsubscribe();
        setLastOutcome(result.outcome);
        setWebState(result.outcome === 'unsubscribed' ? 'not-subscribed' : webState);
      } else {
        const result = await subscribe();
        setLastOutcome(result.outcome);
        setWebState(
          result.outcome === 'subscribed'
            ? 'subscribed'
            : result.outcome === 'denied'
              ? 'denied'
              : webState,
        );
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <section
      className="mx-4 mt-4 space-y-3"
      aria-labelledby="parent-push-card-heading"
      data-testid="parent-push-card"
    >
      <h2 id="parent-push-card-heading" className="text-sm font-bold">
        Thông báo
      </h2>

      {/* Zalo OA — primary, recommended for VN parents. */}
      <div
        className="rounded-2xl border-2 border-sky-500 bg-card p-4 shadow-sm"
        data-testid="parent-push-zalo-card"
      >
        <div className="flex items-start gap-3">
          <span
            className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-[#0068FF] text-white"
            aria-hidden
          >
            <MessageCircle className="h-5 w-5" />
          </span>
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2">
              <p className="text-sm font-bold">Zalo OA — Khuyên dùng</p>
              <span className="rounded-full bg-sky-100 px-2 py-0.5 text-[10px] font-bold text-sky-700">
                CHÍNH
              </span>
            </div>
            <p className="mt-0.5 text-xs text-muted-foreground">
              Nhận thông báo qua Zalo — nhanh và đáng tin cậy nhất ở Việt Nam.
            </p>
          </div>
        </div>
        <div className="mt-3 flex items-center justify-between">
          <p
            className={`text-xs font-semibold ${zaloLinked ? 'text-emerald-700' : 'text-muted-foreground'}`}
          >
            {zaloLinked ? (
              <>
                <Check className="mr-1 inline h-3.5 w-3.5" aria-hidden />
                Đã liên kết Zalo OA
              </>
            ) : (
              'Chưa liên kết'
            )}
          </p>
          <button
            type="button"
            onClick={() => setZaloLinked((s) => !s)}
            className="h-9 rounded-xl border bg-card px-3 text-xs font-semibold transition-colors hover:bg-muted/40 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            data-testid="parent-push-zalo-toggle"
          >
            {zaloLinked ? 'Hủy liên kết' : 'Liên kết Zalo'}
          </button>
        </div>
      </div>

      {/* Web Push — fallback. */}
      <div
        className="rounded-2xl border bg-card p-4 shadow-sm"
        data-testid="parent-push-web-card"
      >
        <div className="flex items-start gap-3">
          <span
            className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-muted text-muted-foreground"
            aria-hidden
          >
            <Bell className="h-5 w-5" />
          </span>
          <div className="min-w-0 flex-1">
            <p className="text-sm font-bold">Thông báo trình duyệt (Web Push)</p>
            <p className="mt-0.5 text-xs text-muted-foreground">
              Phương án phụ — nhận thông báo trực tiếp trên thiết bị của bạn.
            </p>
          </div>
        </div>

        <div className="mt-3 flex items-center justify-between">
          <p
            role="status"
            aria-live="polite"
            className="text-xs font-semibold"
            data-testid="parent-push-web-state"
          >
            {webState === 'subscribed' && (
              <span className="text-emerald-700">
                <Check className="mr-1 inline h-3.5 w-3.5" aria-hidden />
                Đang nhận thông báo
              </span>
            )}
            {webState === 'not-subscribed' && (
              <span className="text-muted-foreground">Chưa bật</span>
            )}
            {webState === 'denied' && (
              <span className="text-red-700">
                Đã chặn — vui lòng vào cài đặt trình duyệt để mở
              </span>
            )}
            {webState === 'unsupported' && (
              <span className="text-muted-foreground">
                Trình duyệt không hỗ trợ Web Push
              </span>
            )}
            {webState === 'unknown' && <span>Đang kiểm tra…</span>}
          </p>
          <button
            type="button"
            onClick={handleToggleWeb}
            disabled={
              busy || webState === 'unsupported' || webState === 'denied'
            }
            className="h-9 rounded-xl bg-primary px-3 text-xs font-bold text-primary-foreground transition-colors hover:bg-primary/90 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-60"
            data-testid="parent-push-web-toggle"
          >
            {busy
              ? 'Đang xử lý…'
              : webState === 'subscribed'
                ? 'Tắt'
                : 'Bật'}
          </button>
        </div>

        {lastOutcome && lastOutcome !== 'subscribed' && lastOutcome !== 'unsubscribed' && (
          <p
            role="alert"
            className="mt-2 text-[11px] font-medium text-amber-700"
            data-testid="parent-push-web-outcome"
          >
            Kết quả: {lastOutcome}
            {lastOutcome === 'not-configured' &&
              ' — VAPID key chưa được cấu hình; vui lòng liên hệ quản trị viên.'}
          </p>
        )}
      </div>
    </section>
  );
}
