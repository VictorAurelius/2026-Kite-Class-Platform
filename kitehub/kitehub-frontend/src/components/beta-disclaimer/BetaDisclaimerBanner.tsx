'use client';

/**
 * Beta Disclaimer Banner (Wave 78 GAP-539 + Wave 98 Bucket B3 finishing stroke).
 *
 * <p>Dismissible banner that surfaces the beta status disclaimer to authenticated
 * users on the dashboard. Dismissal persists via cookie (1-year expiry) so the
 * user doesn't see it every page load after one explicit dismissal.</p>
 *
 * <p>Wave 78 §1 Brainstorm Q2: dismissible (not permanent) — signal that dataset
 * can reset, not perpetual UX noise.</p>
 *
 * <p>Wave 98 Bucket B3 finishing stroke:
 * <ul>
 *   <li>Version chip top-right (<code>v{APP_VERSION}-beta</code>) — Linear-style
 *       changelog signal.</li>
 *   <li>PDPL consent line + link to /legal/privacy — VN PDPL 2023 Art 9-15
 *       compliance hook for invite cohort.</li>
 *   <li>Link to /beta-status surfaces a Linear-style "Trạng thái beta" page
 *       summarising recent shipped changes + known issues.</li>
 * </ul>
 *
 * @since Wave 78 — GAP-539 (Wave 98 B3 polish)
 */

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { AlertTriangle, X } from 'lucide-react';

export const BETA_DISCLAIMER_COOKIE = 'kitehub_beta_disclaimer_dismissed';
const COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 365; // 1 year
const DEFAULT_APP_VERSION = 'v0.9.0-beta';

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

function resolveAppVersion(): string {
  const fromEnv =
    typeof process !== 'undefined' && process.env?.NEXT_PUBLIC_APP_VERSION
      ? process.env.NEXT_PUBLIC_APP_VERSION
      : null;
  if (fromEnv && fromEnv.trim().length > 0) {
    // Normalise "0.9.0" -> "v0.9.0-beta" so the chip always reads as a beta tag.
    if (/^v?\d+\.\d+\.\d+/i.test(fromEnv)) {
      const withV = fromEnv.startsWith('v') || fromEnv.startsWith('V') ? fromEnv : `v${fromEnv}`;
      return /-beta\b/i.test(withV) ? withV : `${withV}-beta`;
    }
    return fromEnv;
  }
  return DEFAULT_APP_VERSION;
}

export interface BetaDisclaimerBannerProps {
  /** Optional override to force visibility in tests. */
  forceShow?: boolean;
  /** Optional override for the version chip (useful for tests / Storybook). */
  appVersion?: string;
}

export function BetaDisclaimerBanner({ forceShow, appVersion }: BetaDisclaimerBannerProps = {}) {
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

  const version = appVersion ?? resolveAppVersion();

  return (
    <div
      role="status"
      aria-label="Beta disclaimer"
      data-testid="beta-disclaimer-banner"
      className="relative flex items-start gap-3 rounded-lg border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-900 dark:border-amber-700 dark:bg-amber-950 dark:text-amber-100"
    >
      <AlertTriangle className="mt-0.5 size-5 shrink-0" aria-hidden />
      <div className="flex-1">
        <div className="flex items-start justify-between gap-3">
          <p className="font-medium">KiteHub đang trong giai đoạn Beta — không reset tự động.</p>
          <span
            data-testid="beta-disclaimer-version-chip"
            aria-label={`Phiên bản KiteHub ${version}`}
            className="ml-2 shrink-0 rounded-full border border-amber-400 bg-amber-100 px-2 py-0.5 text-xs font-semibold uppercase tracking-wide text-amber-900 dark:border-amber-600 dark:bg-amber-900 dark:text-amber-100"
          >
            {version}
          </span>
        </div>
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
        <p
          data-testid="beta-disclaimer-pdpl-consent"
          className="mt-2 text-xs text-amber-800 dark:text-amber-200"
        >
          Bằng cách tiếp tục sử dụng KiteHub, bạn đồng ý với việc xử lý dữ liệu cá nhân theo{' '}
          <Link
            href="/legal/privacy"
            data-testid="beta-disclaimer-privacy-link"
            className="font-medium underline underline-offset-2"
          >
            Chính sách Bảo mật
          </Link>{' '}
          và Luật Bảo vệ dữ liệu cá nhân 2023 (Điều 9-15). Để rút lại đồng ý, liên hệ{' '}
          <a
            href="mailto:support@kitehub.me?subject=R%C3%BAt%20%C4%91%E1%BB%93ng%20%C3%BD%20x%E1%BB%AD%20l%C3%BD%20d%E1%BB%AF%20li%E1%BB%87u%20c%C3%A1%20nh%C3%A2n"
            className="font-medium underline underline-offset-2"
          >
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
