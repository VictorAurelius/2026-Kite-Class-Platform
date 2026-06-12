'use client';

/**
 * AI Branding Wizard — output-first orchestrator (GAP-1216, 5-step reorder).
 *
 * Replaces the Direction-C 7-step flow with the output-first 5-step flow per
 * kit v3 §2.5 (hội tụ 3 audit 2026-06-11):
 *   1. Welcome + Mode  — tenant name + slug + org-type (GAP-1133) + TEMPLATE/FULL_AI
 *   2. Brand personality — Audience + Tone merged onto one page (BrandPersonalityStep)
 *   3. Assets          — Logo + Portrait merged, optional/skip (AssetsStep);
 *                        Portrait sub-section only in FULL_AI mode (GAP-1134)
 *   4. Template        — TEMPLATE route only; FULL_AI skips this step
 *   5. Preview/Generate — live preview + per-resource approve + deploy (Step6Preview)
 *
 * Mode-branching lives in the `wizard-shared.tsx` reducer (NEXT/PREV skip step 4
 * for FULL_AI). The escape-ramp (GAP-1219) jumps to step 4/5 depending on mode.
 *
 * Bundle-size strategy (preserved from Wave GAP-236 Sub-PR B): each step is
 * loaded via `next/dynamic` so only the active step's chunk is shipped.
 *
 * State: a single useReducer owns ALL wizard state per `wizard-shared.tsx`. Step
 * components dispatch — they do NOT keep shadow copies of canonical fields
 * (rework §3.1 anti-pattern guard).
 */

import { useEffect, useMemo, useState } from 'react';
import dynamic from 'next/dynamic';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { useBrandingTier } from '@/hooks/use-branding-tier';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { EmptyState } from '@/components/common/EmptyState';
import { Button } from '@/components/ui/button';
import { ArrowLeft, Sparkles } from 'lucide-react';
import { StepIndicator } from '@/components/branding/wizard/StepIndicator';
import { useWizardReducer, type WizardStep } from '@/components/branding/wizard/wizard-shared';

const stepLoading = () => <LoadingSpinner className="my-12" />;

// Step 1 — Welcome + Mode
const WelcomeStep = dynamic(
  () =>
    import('@/components/branding/wizard/WelcomeStep').then((m) => ({
      default: m.WelcomeStep,
    })),
  { ssr: false, loading: stepLoading }
);

// Step 2 — Brand personality (Audience + Tone merged, GAP-1216).
const BrandPersonalityStep = dynamic(
  () =>
    import('@/components/branding/wizard/BrandPersonalityStep').then((m) => ({
      default: m.BrandPersonalityStep,
    })),
  { ssr: false, loading: stepLoading }
);

// Step 3 — Assets (Logo + Portrait merged, optional/skip, GAP-1216 / GAP-1134).
const AssetsStep = dynamic(
  () =>
    import('@/components/branding/wizard/AssetsStep').then((m) => ({
      default: m.AssetsStep,
    })),
  { ssr: false, loading: stepLoading }
);

// Step 4 — Template grid (TEMPLATE route only) + Step 5 preview/approve.
const TemplateStep = dynamic(
  () =>
    import('@/components/branding/wizard/TemplateStep').then((m) => ({
      default: m.TemplateStep,
    })),
  { ssr: false, loading: stepLoading }
);
const Step6Preview = dynamic(
  () =>
    import('@/components/branding/wizard/Step6Preview').then((m) => ({
      default: m.Step6Preview,
    })),
  { ssr: false, loading: stepLoading }
);

export default function BrandingWizardPage() {
  const router = useRouter();
  const user = useAuthStore((state) => state.user);
  const { data: instances, isError: instancesError } = useOwnerInstances(user?.id);
  const instanceId = instances?.[0]?.id;

  // GAP-1216: tier drives FULL_AI eligibility in the Step-1 mode selector.
  const { tier } = useBrandingTier(instanceId);

  const [loadingTimedOut, setLoadingTimedOut] = useState(false);
  const [state, dispatch] = useWizardReducer();

  // Timeout for initial loading state (preserved from legacy)
  useEffect(() => {
    if (!instanceId && !instancesError) {
      const timer = setTimeout(() => setLoadingTimedOut(true), 10000);
      return () => clearTimeout(timer);
    }
    return undefined;
  }, [instanceId, instancesError]);

  const currentStep: WizardStep = state.currentStep;

  const handleNext = useMemo(
    () => () => dispatch({ type: 'NEXT_STEP' }),
    [dispatch]
  );
  const handleBack = useMemo(
    () => () => dispatch({ type: 'PREV_STEP' }),
    [dispatch]
  );

  const handleDeploy = useMemo(
    () => () => router.push('/branding'),
    [router]
  );

  const handleUpgrade = useMemo(
    () => () => router.push('/billing/upgrade'),
    [router]
  );

  if (instancesError || loadingTimedOut) {
    return (
      <EmptyState
        icon={<Sparkles className="w-12 h-12" />}
        title="Không thể tải thông tin"
        description={
          instancesError
            ? 'Đã xảy ra lỗi khi tải dữ liệu. Vui lòng thử lại sau.'
            : 'Kết nối mất quá lâu. Vui lòng kiểm tra kết nối mạng và thử lại.'
        }
        action={<Button onClick={() => window.location.reload()}>Thử lại</Button>}
      />
    );
  }

  if (!instanceId) {
    return (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      {/* Page Header */}
      <div className="rounded-2xl bg-gradient-to-r from-purple-500/10 via-primary/5 to-accent/10 dark:from-purple-500/20 dark:via-primary/10 dark:to-accent/20 border p-6">
        <div className="flex items-center gap-4 mb-3">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => router.push('/branding')}
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            Quay lại
          </Button>
        </div>
        <div className="flex items-center gap-3">
          <div className="rounded-xl bg-purple-500/10 dark:bg-purple-500/20 p-3 text-purple-600 dark:text-purple-400">
            <Sparkles className="h-5 w-5" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Trình hướng dẫn AI Branding</h1>
            <p className="text-muted-foreground">
              Tạo bộ nhận diện thương hiệu hoàn chỉnh từ logo của bạn
            </p>
          </div>
        </div>
      </div>

      <StepIndicator currentStep={currentStep} mode={state.mode} />

      <div className="mt-2">
        {currentStep === 1 && (
          <WelcomeStep
            wizardState={state}
            dispatch={dispatch}
            onNext={handleNext}
            tier={tier}
            onUpgradeClick={handleUpgrade}
          />
        )}

        {currentStep === 2 && (
          <BrandPersonalityStep
            wizardState={state}
            dispatch={dispatch}
            onNext={handleNext}
            onBack={handleBack}
          />
        )}

        {currentStep === 3 && (
          <AssetsStep
            wizardState={state}
            dispatch={dispatch}
            instanceId={instanceId}
            onNext={handleNext}
            onBack={handleBack}
          />
        )}

        {currentStep === 4 && (
          <TemplateStep
            wizardState={state}
            dispatch={dispatch}
            instanceId={instanceId}
            onNext={handleNext}
            onBack={handleBack}
          />
        )}

        {currentStep === 5 && (
          <Step6Preview
            wizardState={state}
            dispatch={dispatch}
            assetInstanceId={instanceId}
            onBack={handleBack}
            onDeploy={handleDeploy}
          />
        )}
      </div>
    </div>
  );
}
