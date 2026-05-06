'use client';

import dynamic from 'next/dynamic';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { EmptyState } from '@/components/common/EmptyState';
import { Button } from '@/components/ui/button';
import { Sparkles } from 'lucide-react';

import { StepIndicator } from '@/components/branding/wizard/StepIndicator';
import { useWizardReducer } from '@/components/branding/wizard/wizard-shared';
import type { WelcomeStepData } from '@/components/branding/wizard/WelcomeStep';
import type { LogoStepData } from '@/components/branding/wizard/LogoStep';

// ---------------------------------------------------------------------------
// Lazy-load step components — only the active step chunk is fetched on demand.
// Steps 3-6 are placeholders owned by other Bucket agents (B/C/D).
// ---------------------------------------------------------------------------

const stepLoading = () => <LoadingSpinner className="my-12" />;

const WelcomeStep = dynamic(
  () =>
    import('@/components/branding/wizard/WelcomeStep').then((m) => ({
      default: m.WelcomeStep,
    })),
  { ssr: false, loading: stepLoading },
);

const LogoStep = dynamic(
  () =>
    import('@/components/branding/wizard/LogoStep').then((m) => ({
      default: m.LogoStep,
    })),
  { ssr: false, loading: stepLoading },
);

// Steps 3-6 — lazy placeholders; real implementations owned by Buckets B/C/D
const AudienceStep = dynamic(
  () =>
    import('@/components/branding/wizard/AudienceStep').then((m) => ({
      default: m.AudienceStep,
    })),
  { ssr: false, loading: stepLoading },
);

const ToneStep = dynamic(
  () =>
    import('@/components/branding/wizard/ToneStep').then((m) => ({
      default: m.ToneStep,
    })),
  { ssr: false, loading: stepLoading },
);

const TemplateStep = dynamic(
  () =>
    import('@/components/branding/wizard/TemplateStep').then((m) => ({
      default: m.TemplateStep,
    })),
  { ssr: false, loading: stepLoading },
);

const ApprovalStep = dynamic(
  () =>
    import('@/components/branding/wizard/ApprovalStep').then((m) => ({
      default: m.ApprovalStep,
    })),
  { ssr: false, loading: stepLoading },
);

// ---------------------------------------------------------------------------
// BrandingWizardPage — 6-step orchestrator
// ---------------------------------------------------------------------------

export default function BrandingWizardPage() {
  const router = useRouter();
  const user = useAuthStore((state) => state.user);
  const { data: instances, isError: instancesError } = useOwnerInstances(user?.id);
  const instanceId = instances?.[0]?.id;

  const [wizardState, dispatch] = useWizardReducer();

  // ---------------------------------------------------------------------------
  // Step transition handlers
  // ---------------------------------------------------------------------------

  const handleWelcomeNext = (data: WelcomeStepData) => {
    dispatch({ type: 'SET_TENANT_NAME', payload: data.tenantName });
    dispatch({ type: 'SET_SLUG', payload: data.slug });
    dispatch({ type: 'NEXT_STEP' });
  };

  const handleLogoNext = (data: LogoStepData) => {
    dispatch({ type: 'SET_LOGO_URL', payload: data.logoUrl });
    dispatch({ type: 'SET_AI_LOGO', payload: data.aiLogo });
    dispatch({ type: 'NEXT_STEP' });
  };

  // ---------------------------------------------------------------------------
  // Loading / error guards
  // ---------------------------------------------------------------------------

  if (instancesError) {
    return (
      <EmptyState
        icon={<Sparkles className="w-12 h-12" />}
        title="Không thể tải thông tin"
        description="Đã xảy ra lỗi khi tải dữ liệu. Vui lòng thử lại sau."
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

  // ---------------------------------------------------------------------------
  // Shell
  // ---------------------------------------------------------------------------

  return (
    <div className="space-y-4 max-w-4xl mx-auto py-4 px-2 sm:px-0">
      {/* Step indicator */}
      <StepIndicator currentStep={wizardState.currentStep} />

      {/* Active step */}
      <div className="mt-2">
        {wizardState.currentStep === 1 && (
          <WelcomeStep
            initialData={{
              tenantName: wizardState.tenantName,
              slug: wizardState.slug,
            }}
            onNext={handleWelcomeNext}
          />
        )}

        {wizardState.currentStep === 2 && (
          <LogoStep
            instanceId={instanceId}
            initialData={{
              logoUrl: wizardState.logoUrl,
              aiLogo: wizardState.aiLogo,
            }}
            onNext={handleLogoNext}
            onBack={() => dispatch({ type: 'PREV_STEP' })}
          />
        )}

        {wizardState.currentStep === 3 && (
          <AudienceStep
            wizardState={wizardState}
            onNext={(audience: string) => {
              dispatch({ type: 'SET_AUDIENCE', payload: audience });
              dispatch({ type: 'NEXT_STEP' });
            }}
            onBack={() => dispatch({ type: 'PREV_STEP' })}
          />
        )}

        {wizardState.currentStep === 4 && (
          <ToneStep
            wizardState={wizardState}
            onNext={(tone: string) => {
              dispatch({ type: 'SET_TONE', payload: tone });
              dispatch({ type: 'NEXT_STEP' });
            }}
            onBack={() => dispatch({ type: 'PREV_STEP' })}
          />
        )}

        {wizardState.currentStep === 5 && (
          <TemplateStep
            wizardState={wizardState}
            instanceId={instanceId}
            onNext={(templateId: string, jobId: string) => {
              dispatch({ type: 'SET_TEMPLATE_ID', payload: templateId });
              dispatch({ type: 'SET_JOB_ID', payload: jobId });
              dispatch({ type: 'NEXT_STEP' });
            }}
            onBack={() => dispatch({ type: 'PREV_STEP' })}
          />
        )}

        {wizardState.currentStep === 6 && wizardState.jobId && (
          <ApprovalStep
            wizardState={wizardState}
            jobId={wizardState.jobId}
            onPublish={() => router.push('/branding?success=true')}
            onBack={() => dispatch({ type: 'PREV_STEP' })}
          />
        )}
      </div>
    </div>
  );
}
