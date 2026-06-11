'use client';

/**
 * Wave 32 Bucket A — AI Branding Wizard v2 orchestrator (Direction C 6-step refactor).
 *
 * Replaces the legacy 4-step wizard (Upload → Analyze → Generate → Review) with
 * the Direction C 7-step flow per `ai-branding-guidelines.md` §4.1:
 *   1. Welcome   — tenant name + slug + org-type select (GAP-1133)
 *   2. Logo      — upload OR AI-generate fork
 *   3. Portrait  — 1..N teacher headshots (GAP-1134; count hint by org-type)
 *   4. Audience  — 4 VN audience cards   (Bucket B)
 *   5. Tone      — 4 tone cards          (Bucket B)
 *   6. Template  — grid + Enterprise custom-prompt (Bucket C)
 *   7. Preview   — per-resource approve + full-screen preview + deploy (Bucket C+D / GAP-1136)
 *
 * Bucket A shipped Steps 1-2 (#883). Buckets B (#889 audience+tone), C (#888
 * template), and C+D (#890 preview / quality gate / deploy) shipped the
 * remaining step components. This file is the orchestrator wire that replaced
 * the Bucket A placeholders with real `dynamic()` imports — Phase B of the
 * locked Post-Wave-32 sequence (see project memory
 * `project_post_wave_32_sequence_plan.md`).
 *
 * Bundle-size strategy (preserved from Wave GAP-236 Sub-PR B): each step is
 * loaded via `next/dynamic` so only the active step's chunk is shipped.
 *
 * State: a single useReducer in this orchestrator owns ALL wizard state per
 * `wizard-shared.tsx`. Step components dispatch — they do NOT keep their own
 * shadow copies of canonical fields (rework §3.1 anti-pattern guard).
 */

import { useEffect, useMemo, useState } from 'react';
import dynamic from 'next/dynamic';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { EmptyState } from '@/components/common/EmptyState';
import { Button } from '@/components/ui/button';
import { ArrowLeft, Sparkles } from 'lucide-react';
import { StepIndicator } from '@/components/branding/wizard/StepIndicator';
import { useWizardReducer, type WizardStep } from '@/components/branding/wizard/wizard-shared';

const stepLoading = () => <LoadingSpinner className="my-12" />;

// Bucket A — Welcome + Logo
const WelcomeStep = dynamic(
  () =>
    import('@/components/branding/wizard/WelcomeStep').then((m) => ({
      default: m.WelcomeStep,
    })),
  { ssr: false, loading: stepLoading }
);
const LogoStep = dynamic(
  () =>
    import('@/components/branding/wizard/LogoStep').then((m) => ({
      default: m.LogoStep,
    })),
  { ssr: false, loading: stepLoading }
);

// GAP-1134 — Portrait upload step (Step 3). Reuses the asset upload + library
// pattern; count hint driven by the org-type chosen in Step 1.
const PortraitStep = dynamic(
  () =>
    import('@/components/branding/wizard/PortraitStep').then((m) => ({
      default: m.PortraitStep,
    })),
  { ssr: false, loading: stepLoading }
);

// Bucket B — Audience + Tone (Wave 32 rework B). Their `onNext(selected)` is
// adapted by this orchestrator (see currentStep === 3/4 below) so SET_AUDIENCE /
// SET_TONE dispatch happens here, not inside the step.
const AudienceStep = dynamic(
  () =>
    import('@/components/branding/wizard/AudienceStep').then((m) => ({
      default: m.AudienceStep,
    })),
  { ssr: false, loading: stepLoading }
);
const ToneStep = dynamic(
  () =>
    import('@/components/branding/wizard/ToneStep').then((m) => ({
      default: m.ToneStep,
    })),
  { ssr: false, loading: stepLoading }
);

// Bucket C — Template grid + Step 6 preview/approve.
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

  // Bucket B step components (Audience step 4 / Tone step 5) emit the selected
  // id as the onNext payload — adapt to the orchestrator's "dispatch SET_* then
  // advance" contract here instead of changing Bucket B's local signature.
  const handleAudienceNext = useMemo(
    () => (audience: string) => {
      dispatch({ type: 'SET_AUDIENCE', audience });
      dispatch({ type: 'NEXT_STEP' });
    },
    [dispatch]
  );
  const handleToneNext = useMemo(
    () => (tone: string) => {
      dispatch({ type: 'SET_TONE', tone });
      dispatch({ type: 'NEXT_STEP' });
    },
    [dispatch]
  );

  const handleDeploy = useMemo(
    () => () => router.push('/branding'),
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

      <StepIndicator currentStep={currentStep} />

      <div className="mt-2">
        {currentStep === 1 && (
          <WelcomeStep
            wizardState={state}
            dispatch={dispatch}
            onNext={handleNext}
          />
        )}

        {currentStep === 2 && (
          <LogoStep
            wizardState={state}
            dispatch={dispatch}
            instanceId={instanceId}
            onNext={handleNext}
            onBack={handleBack}
          />
        )}

        {currentStep === 3 && (
          <PortraitStep
            wizardState={state}
            dispatch={dispatch}
            instanceId={instanceId}
            onNext={handleNext}
            onBack={handleBack}
          />
        )}

        {currentStep === 4 && (
          <AudienceStep
            wizardState={state}
            onNext={handleAudienceNext}
            onBack={handleBack}
          />
        )}

        {currentStep === 5 && (
          <ToneStep
            wizardState={state}
            onNext={handleToneNext}
            onBack={handleBack}
          />
        )}

        {currentStep === 6 && (
          <TemplateStep
            wizardState={state}
            dispatch={dispatch}
            instanceId={instanceId}
            onNext={handleNext}
            onBack={handleBack}
          />
        )}

        {currentStep === 7 && (
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
