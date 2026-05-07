'use client';

/**
 * Wave 32 Bucket A — AI Branding Wizard v2 orchestrator (Direction C 6-step refactor).
 *
 * Replaces the legacy 4-step wizard (Upload → Analyze → Generate → Review) with
 * the Direction C 6-step flow per `ai-branding-guidelines.md` §4.1:
 *   1. Welcome   — tenant name + slug validation
 *   2. Logo      — upload OR AI-generate fork
 *   3. Audience  — 4 VN audience cards   (Bucket B)
 *   4. Tone      — 4 tone cards          (Bucket B)
 *   5. Template  — grid + Enterprise custom-prompt (Bucket C)
 *   6. Preview   — per-resource approve + quality gate + deploy (Bucket C+D)
 *
 * Bucket A (this PR) ships the SHELL + Steps 1-2. Steps 3-6 render placeholder
 * cards until Bucket B/C/D land — see `wizard-shared.tsx` for the prop type
 * stubs they will implement.
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
          <BucketPlaceholder bucket="B" stepLabel="Đối tượng" onBack={handleBack} />
        )}

        {currentStep === 4 && (
          <BucketPlaceholder bucket="B" stepLabel="Phong cách" onBack={handleBack} />
        )}

        {currentStep === 5 && (
          <BucketPlaceholder bucket="C" stepLabel="Mẫu thiết kế" onBack={handleBack} />
        )}

        {currentStep === 6 && (
          <BucketPlaceholder
            bucket="C/D"
            stepLabel="Phê duyệt"
            onBack={handleBack}
          />
        )}
      </div>
    </div>
  );
}

/**
 * BucketPlaceholder — temporary card shown for steps 3-6 until Bucket B/C/D
 * agents land. Once those PRs merge, the orchestrator will replace each
 * placeholder with the real `dynamic()` import of the corresponding step
 * component (props contracts are already declared in `wizard-shared.tsx`).
 *
 * This is NOT a cross-bucket scope leak (rework §3.2): we render a generic
 * placeholder, NOT a stub of the future component.
 */
function BucketPlaceholder({
  bucket,
  stepLabel,
  onBack,
}: {
  bucket: 'B' | 'C' | 'C/D';
  stepLabel: string;
  onBack: () => void;
}) {
  return (
    <div
      data-testid={`wizard-bucket-placeholder-${bucket}`}
      className="max-w-2xl mx-auto rounded-lg border border-dashed border-muted-foreground/40 p-8 text-center"
    >
      <Sparkles className="w-8 h-8 mx-auto mb-3 text-muted-foreground" />
      <h2 className="text-lg font-semibold mb-1">
        Bước &ldquo;{stepLabel}&rdquo; — Bucket {bucket} chưa shipped
      </h2>
      <p className="text-sm text-muted-foreground mb-4">
        Phần này sẽ có khi bucket {bucket} của Wave 32 hoàn tất.
      </p>
      <Button variant="ghost" onClick={onBack}>
        <ArrowLeft className="mr-2 h-4 w-4" />
        Quay lại bước trước
      </Button>
    </div>
  );
}
