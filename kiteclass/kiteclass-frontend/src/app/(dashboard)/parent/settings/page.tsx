/**
 * Parent settings — profile, notifications (Zalo OA + Web Push), language,
 * PWA install prompt. Consolidates `settings.html`,
 * `push-notification-card.html`, `pwa-install-prompt.html`,
 * `web-push-permission.html` from the kc-parent kit into one tabbed page
 * with anchored sub-sections.
 *
 * Wave 49 Bucket A (GAP-267).
 */

'use client';

export const dynamic = 'force-dynamic';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { ChevronRight, LogOut, User } from 'lucide-react';
import { useAuthStore } from '@/stores/auth-store';
import { UserType } from '@/types/auth';
import { useParentMe } from '@/hooks/use-parent';
import { ParentShell } from '@/components/parent/parent-shell';
import { PushNotificationCard } from '@/components/parent/push-notification-card';
import { PwaInstallPrompt } from '@/components/parent/pwa-install-prompt';
import { WebPushPermission } from '@/components/parent/web-push-permission';

export default function ParentSettingsPage() {
  const router = useRouter();
  const userType = useAuthStore((state) => state.user?.userType);
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const { data: profile } = useParentMe();

  useEffect(() => {
    if (userType && userType !== UserType.PARENT) {
      router.replace('/dashboard');
    }
  }, [userType, router]);

  return (
    <ParentShell title="Cài đặt" subtitle="Quản lý tài khoản phụ huynh">
      <PwaInstallPrompt />

      <section
        className="mx-4 mt-4 rounded-2xl border bg-card p-4 shadow-sm"
        data-testid="parent-settings-profile"
        id="profile"
      >
        <div className="flex items-center gap-3">
          <span
            className="flex h-12 w-12 items-center justify-center rounded-full bg-primary/10 text-primary"
            aria-hidden
          >
            <User className="h-6 w-6" />
          </span>
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-bold">
              {profile?.fullName ?? 'Phụ huynh KiteClass'}
            </p>
            <p className="truncate text-xs text-muted-foreground">
              {profile?.email ?? '—'}
            </p>
            {profile?.phoneNumber && (
              <p className="truncate text-xs text-muted-foreground">
                {profile.phoneNumber}
              </p>
            )}
          </div>
        </div>
      </section>

      <PushNotificationCard />
      <WebPushPermission />

      <section
        id="notifications"
        className="mx-4 mt-4"
        aria-labelledby="parent-settings-other-heading"
      >
        <h2
          id="parent-settings-other-heading"
          className="mb-2 text-sm font-bold"
        >
          Tùy chọn khác
        </h2>
        <ul
          className="rounded-2xl border bg-card"
          data-testid="parent-settings-other-list"
        >
          {[
            { label: 'Ngôn ngữ', value: 'Tiếng Việt' },
            { label: 'Múi giờ', value: 'GMT+7 (Hà Nội)' },
            { label: 'Trợ giúp & FAQ', value: 'Liên hệ nhà trường' },
            { label: 'Chính sách bảo mật', value: 'Xem' },
          ].map((row) => (
            <li
              key={row.label}
              className="flex items-center justify-between gap-3 border-b p-4 last:border-b-0"
            >
              <div>
                <p className="text-sm font-semibold">{row.label}</p>
                <p className="text-xs text-muted-foreground">{row.value}</p>
              </div>
              <ChevronRight
                className="h-4 w-4 text-muted-foreground"
                aria-hidden
              />
            </li>
          ))}
        </ul>
      </section>

      <section className="mx-4 mt-6">
        <button
          type="button"
          onClick={() => {
            clearAuth();
            router.replace('/auth/login');
          }}
          className="flex h-12 w-full items-center justify-center gap-2 rounded-2xl border-2 border-red-200 bg-card text-sm font-bold text-red-700 transition-colors hover:bg-red-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          data-testid="parent-settings-logout"
        >
          <LogOut className="h-4 w-4" aria-hidden />
          Đăng xuất
        </button>
      </section>
    </ParentShell>
  );
}
