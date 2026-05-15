'use client';

/**
 * Beta Disclaimer Banner (Wave 78 GAP-539).
 *
 * <p>Dismissible banner that surfaces the beta status disclaimer to authenticated
 * users on the dashboard. Dismissal persists via cookie (1-year expiry) so the
 * user doesn't see it every page load after one explicit dismissal.</p>
 *
 * <p>Per Wave 78 plan §1 Brainstorm Q2: dismissible (not permanent) — banner
 * is signal that dataset can reset, not perpetual UX noise.</p>
 *
 * @since Wave 78 — GAP-539
 */

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { AlertTriangle, X } from 'lucide-react';

export const BETA_DISCLAIMER_COOKIE = 'kitehub_beta_disclaimer_dismissed';
const COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 365; // 1 year

function readCookie(name: string): string | null {
  if (typeof document === 'undefined') {
    return null;
  }
  const cookies = document.cookie ? document.cookie.split('; ') : [];
  for (const c of cookies) {
    const [key, ...rest] = c.split('=');
    if (key === name) {
      return decodeURIComponent(rest.join('='));
    }
  }
  return null;
}

function writeCookie(name: string, value: string): void {
  if (typeof document === 'undefined') {
    return;
  }
  document.cookie = `${name}=${encodeURIComponent(value)}; Max-Age=${COOKIE_MAX_AGE_SECONDS}; Path=/; SameSite=Lax`;
}

export interface BetaDisclaimerBannerProps {
  /** Optional override to force visibility in tests. */
  forceShow?: boolean;
}

export function BetaDisclaimerBanner({ forceShow }: BetaDisclaimerBannerProps = {}) {
  const [visible, setVisible] = useState<boolean>(false);

  useEffect(() => {
    if (forceShow) {
      setVisible(true);
      return;
    }
    const dismissed = readCookie(BETA_DISCLAIMER_COOKIE);
    setVisible(dismissed !== '1');
  }, [forceShow]);

  if (!visible) {
    return null;
  }

  const handleDismiss = () => {
    writeCookie(BETA_DISCLAIMER_COOKIE, '1');
    setVisible(false);
  };

  return (
    <div
      role="status"
      aria-label="Beta disclaimer"
      data-testid="beta-disclaimer-banner"
      className="relative flex items-start gap-3 rounded-lg border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-900 dark:border-amber-700 dark:bg-amber-950 dark:text-amber-100"
    >
      <AlertTriangle className="mt-0.5 size-5 shrink-0" aria-hidden />
      <div className="flex-1">
        <p className="font-medium">KiteHub đang trong giai đoạn Beta — không reset tự động.</p>
        <p className="mt-1 text-amber-800 dark:text-amber-200">
          KiteHub <strong>không tự ý reset</strong> dữ liệu tenant; mọi reset (migration breaking
          hoặc exit-BETA cutover) sẽ được báo trước tối thiểu <strong>7 ngày</strong> qua email và
          dashboard banner, kèm <strong>backup 30 ngày</strong>. Dữ liệu audit log + payment +
          subscription không bao giờ bị reset. Chi tiết:{' '}
          <Link
            href="/docs/data-reset-policy"
            data-testid="beta-disclaimer-policy-link"
            className="font-medium underline underline-offset-2"
          >
            Chính sách reset dữ liệu Beta
          </Link>
          . Gặp vấn đề?{' '}
          <Link href="/beta-status" className="font-medium underline underline-offset-2">
            Xem trạng thái Beta
          </Link>{' '}
          hoặc email{' '}
          <a href="mailto:support@kitehub.me" className="font-medium underline underline-offset-2">
            support@kitehub.me
          </a>
          .
        </p>
      </div>
      <button
        type="button"
        onClick={handleDismiss}
        aria-label="Đóng thông báo"
        className="ml-2 rounded p-1 hover:bg-amber-100 dark:hover:bg-amber-900"
      >
        <X className="size-4" aria-hidden />
      </button>
    </div>
  );
}
