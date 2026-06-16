'use client';

/**
 * Dashboard onboarding CTA card (Wave 79 Bucket D — GAP-559).
 *
 * <p>Shows "Hoàn tất setup (N/5)" progress card on the customer dashboard so
 * first-login Owners discover the onboarding flow without having to memorize
 * the /onboarding URL. Hides itself when all steps are complete (100%) so
 * returning Owners aren't shown a redundant CTA.</p>
 *
 * <p>Reads onboarding state from the same BE endpoint as
 * {@link OnboardingChecklist} so the two surfaces stay in sync.</p>
 *
 * @since Wave 79 — GAP-559
 */

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Rocket, ArrowRight } from 'lucide-react';
import {
  getOnboardingProgress,
  type OnboardingProgressResponse,
} from '@/lib/api/onboarding';
import { getTenantIdFromToken } from '@/lib/auth/jwt-storage';

export interface OnboardingDashboardCTAProps {
  /** Inject for tests / SSR — skip the first fetch. */
  initialState?: OnboardingProgressResponse;
}

export function OnboardingDashboardCTA({ initialState }: OnboardingDashboardCTAProps = {}) {
  const [state, setState] = useState<OnboardingProgressResponse | null>(initialState ?? null);

  useEffect(() => {
    if (initialState) return;
    // GAP-1445: onboarding is per-tenant — skip the fetch for a tenantless
    // platform owner (no tenantId JWT claim) so the dashboard doesn't fire a
    // request the tenant-scoped endpoint will reject. CTA stays hidden.
    if (!getTenantIdFromToken()) return;
    let cancelled = false;
    void (async () => {
      try {
        const data = await getOnboardingProgress();
        if (!cancelled) setState(data);
      } catch {
        // Silently degrade — CTA is non-critical; checklist surface handles errors.
        if (!cancelled) setState(null);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [initialState]);

  if (!state) {
    return null;
  }

  // Hide once user has finished every step — no redundant nudge.
  if (state.completedSteps >= state.totalSteps && state.totalSteps > 0) {
    return null;
  }

  const remaining = state.totalSteps - state.completedSteps;

  return (
    <Link
      href="/onboarding"
      data-testid="dashboard-onboarding-cta"
      className="block rounded-2xl border bg-gradient-to-br from-primary/5 via-background to-accent/5 p-5 shadow-sm transition-all hover:border-primary hover:shadow-md"
    >
      <div className="flex items-center gap-4">
        <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-primary/10">
          <Rocket className="h-6 w-6 text-primary" aria-hidden />
        </div>
        <div className="flex-1">
          <h3 className="text-base font-semibold">
            Hoàn tất setup ({state.completedSteps}/{state.totalSteps})
          </h3>
          <p className="mt-1 text-sm text-muted-foreground">
            Bạn còn {remaining} bước để khởi động trung tâm — mở danh sách checklist để tiếp tục.
          </p>
          <div className="mt-3 h-1.5 w-full overflow-hidden rounded-full bg-muted">
            <div
              className="h-full bg-primary transition-all"
              style={{ width: `${state.completionPercent}%` }}
              data-testid="dashboard-onboarding-cta-progress"
              aria-valuenow={state.completionPercent}
              aria-valuemin={0}
              aria-valuemax={100}
              role="progressbar"
            />
          </div>
        </div>
        <ArrowRight className="h-5 w-5 shrink-0 text-muted-foreground" aria-hidden />
      </div>
    </Link>
  );
}
