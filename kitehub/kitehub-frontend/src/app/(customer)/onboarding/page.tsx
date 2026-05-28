'use client';

/**
 * Day-1 Onboarding page (Wave 78 GAP-538).
 *
 * Authenticated route; uses customer dashboard layout (which guards auth).
 *
 * @since Wave 78 — GAP-538
 */

import Link from 'next/link';
import { ChevronLeft } from 'lucide-react';
import { OnboardingChecklist } from '@/components/onboarding-checklist';

export default function OnboardingPage() {
  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6 px-4 py-8">
      <Link
        href="/dashboard"
        className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ChevronLeft className="size-4" aria-hidden />
        Quay lại Dashboard
      </Link>

      <div>
        <h1 className="text-2xl font-bold tracking-tight">Bắt đầu với KiteHub</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Hoàn thành các bước dưới đây để khởi động trung tâm của bạn. Bạn có thể quay lại trang
          này bất kỳ lúc nào từ Dashboard.
        </p>
      </div>

      <OnboardingChecklist />
    </div>
  );
}
