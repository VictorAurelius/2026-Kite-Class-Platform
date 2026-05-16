'use client';

/**
 * OpenConsentBannerButton — Withdraw mechanism cho PDPL Decree 13/2023/NĐ-CP Art 12.
 *
 * Wave 86 Bucket E E-AC6 (GAP-585) — closes withdraw-UI gap surfaced by audit:
 * Cookies policy page §6.1 đã document có button "Quản lý cookie" nhưng chưa
 * render thực tế. User trước đây chỉ có thể withdraw consent qua DevTools clear
 * LocalStorage (không user-friendly + vi phạm Art 11.3 "rút lại với cùng mức
 * dễ dàng như đồng ý").
 *
 * Behavior:
 * 1. Click button → call `useConsent().revoke()` → clear `kite.consent.v1`
 *    LocalStorage + call `POST /api/v1/consent/{visitorId}/revoke` (best-effort,
 *    non-fatal — server audit trail per BR-PDPL-CONSENT-003)
 * 2. `useConsent` setState(null) → triggers ConsentBanner re-mount on next render
 *    (banner detects `state === null && hydrated === true` → render)
 * 3. User sees banner again, can re-pick categories
 *
 * SSR safety: useConsent returns hydrated=false on server; button rendered but
 * `disabled` until hydrated. Avoids hydration mismatch.
 *
 * Accessibility (WCAG AA):
 * - `<button>` element với explicit type="button"
 * - Visible focus ring (focus-visible:ring-2)
 * - Status text after click announced via aria-live="polite"
 *
 * Closes GAP-585 (Wave 86 Bucket E — PDPL Decree 13/2023 Art 12 withdraw).
 * Builds on GAP-353/GAP-353b (banner + server API) + GAP-368 (cookies page).
 *
 * @since Wave 86 — GAP-585
 */

import { useState } from 'react';
import { useConsent } from '@kite/shared-ui';

export function OpenConsentBannerButton() {
  const { revoke, hydrated, state } = useConsent();
  const [justRevoked, setJustRevoked] = useState(false);

  const handleClick = () => {
    revoke();
    setJustRevoked(true);
    // Scroll to bottom so user sees the banner that just re-mounted
    if (typeof window !== 'undefined') {
      window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }
  };

  // Render disabled placeholder pre-hydration to avoid SSR mismatch
  if (!hydrated) {
    return (
      <button
        type="button"
        disabled
        className="inline-flex items-center justify-center rounded-md border bg-muted px-4 py-2 text-sm font-medium text-muted-foreground"
      >
        Mở lại cài đặt cookie
      </button>
    );
  }

  const hasConsent = state !== null;

  return (
    <div className="flex flex-col gap-2">
      <button
        type="button"
        onClick={handleClick}
        data-testid="reopen-consent-banner-btn"
        className="inline-flex items-center justify-center rounded-md border bg-background px-4 py-2 text-sm font-medium text-foreground hover:bg-muted/50 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
      >
        {hasConsent ? 'Mở lại cài đặt cookie' : 'Mở banner cookie'}
      </button>
      <div role="status" aria-live="polite" className="text-xs text-muted-foreground">
        {justRevoked && hasConsent === false
          ? 'Đã xoá lựa chọn cookie trước đó. Banner sẽ hiển thị lại ở cuối trang để bạn chọn lại.'
          : hasConsent
            ? 'Click để rút lại đồng ý hiện tại và chọn lại các loại cookie.'
            : 'Banner cookie sẽ hiển thị ở cuối trang.'}
      </div>
    </div>
  );
}

export default OpenConsentBannerButton;
