'use client';

/**
 * Day-1 Onboarding Checklist (Wave 78 GAP-538).
 *
 * <p>5-step checklist component for tenants on first login. State persisted via
 * BE `GET/PUT /api/v1/onboarding-progress` per
 * `documents/01-business/kitehub/onboarding/api-contract.md`.</p>
 *
 * <p>Sample/demo data seed (step {@code IMPORT_DATA}) requires explicit user
 * opt-in: clicking the step's "Bật dữ liệu mẫu" button triggers a confirmation
 * dialog before issuing the PUT. Seed runs server-side gated by
 * {@code tenant.metadata.is_beta_demo_data} flag.</p>
 *
 * @since Wave 78 — GAP-538
 */

import { useCallback, useEffect, useState } from 'react';
import { CheckCircle2, Circle, Loader2, AlertCircle, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  getOnboardingProgress,
  updateOnboardingStep,
  ONBOARDING_STEP_LABELS_VI,
  type OnboardingProgressResponse,
  type OnboardingStepId,
} from '@/lib/api/onboarding';
import { getTenantIdFromToken } from '@/lib/auth/jwt-storage';

interface OnboardingChecklistProps {
  /** Inject initial state for SSR/tests (skips first fetch). */
  initialState?: OnboardingProgressResponse;
}

export function OnboardingChecklist({ initialState }: OnboardingChecklistProps = {}) {
  const [state, setState] = useState<OnboardingProgressResponse | null>(initialState ?? null);
  const [loading, setLoading] = useState<boolean>(!initialState);
  const [pendingStep, setPendingStep] = useState<OnboardingStepId | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [showDemoConfirm, setShowDemoConfirm] = useState<boolean>(false);

  const refresh = useCallback(async () => {
    // GAP-1445: onboarding is per-tenant (BR-ONBOARD-001). A platform owner with
    // no tenant context (no tenantId JWT claim) has no checklist — skip the fetch
    // instead of hitting a tenant-scoped endpoint that rejects (TENANT_CONTEXT_MISSING)
    // and surfacing a scary error. Renders nothing (state stays null).
    if (!getTenantIdFromToken()) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const data = await getOnboardingProgress();
      setState(data);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Không tải được tiến độ.';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!initialState) {
      void refresh();
    }
  }, [initialState, refresh]);

  const handleToggle = useCallback(
    async (stepId: OnboardingStepId, nextCompleted: boolean) => {
      setPendingStep(stepId);
      setError(null);
      try {
        const updated = await updateOnboardingStep({ stepId, completed: nextCompleted });
        setState(updated);
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Cập nhật thất bại.';
        setError(message);
      } finally {
        setPendingStep(null);
      }
    },
    []
  );

  const handleImportDataOptIn = useCallback(() => {
    setShowDemoConfirm(false);
    void handleToggle('IMPORT_DATA', true);
  }, [handleToggle]);

  if (loading && !state) {
    return (
      <div
        data-testid="onboarding-checklist-loading"
        className="flex items-center gap-2 rounded-lg border bg-muted/30 p-4 text-sm text-muted-foreground"
      >
        <Loader2 className="size-4 animate-spin" aria-hidden />
        Đang tải tiến độ onboarding...
      </div>
    );
  }

  if (error && !state) {
    return (
      <div
        role="alert"
        data-testid="onboarding-checklist-error"
        className="flex items-center gap-2 rounded-lg border border-destructive/50 bg-destructive/10 p-4 text-sm text-destructive"
      >
        <AlertCircle className="size-4" aria-hidden />
        <span>{error}</span>
        <Button size="sm" variant="ghost" onClick={() => void refresh()}>
          Thử lại
        </Button>
      </div>
    );
  }

  if (!state) {
    return null;
  }

  return (
    <section
      data-testid="onboarding-checklist"
      aria-labelledby="onboarding-checklist-heading"
      className="rounded-xl border bg-card p-6 shadow-sm"
    >
      <header className="mb-4 flex items-start justify-between gap-4">
        <div>
          <h2
            id="onboarding-checklist-heading"
            className="text-lg font-semibold tracking-tight"
          >
            Bắt đầu với KiteHub
          </h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Hoàn thành {state.completedSteps}/{state.totalSteps} bước để khởi động trung tâm của bạn.
          </p>
        </div>
        <div className="text-right">
          <div className="text-2xl font-bold text-primary" data-testid="onboarding-progress-percent">
            {state.completionPercent}%
          </div>
          <p className="text-xs text-muted-foreground">tiến độ</p>
        </div>
      </header>

      <div
        className="mb-4 h-2 w-full overflow-hidden rounded-full bg-muted"
        role="progressbar"
        aria-valuenow={state.completionPercent}
        aria-valuemin={0}
        aria-valuemax={100}
      >
        <div
          className="h-full bg-primary transition-all"
          style={{ width: `${state.completionPercent}%` }}
        />
      </div>

      <ul className="space-y-3" data-testid="onboarding-step-list">
        {state.steps.map((step) => {
          const labels = ONBOARDING_STEP_LABELS_VI[step.stepId];
          const isPending = pendingStep === step.stepId;
          const isImportData = step.stepId === 'IMPORT_DATA';

          return (
            <li
              key={step.stepId}
              data-step-id={step.stepId}
              className={`flex items-start gap-3 rounded-lg border p-3 transition ${
                step.completed ? 'border-emerald-300 bg-emerald-50/40 dark:border-emerald-700 dark:bg-emerald-950/30' : 'border-border bg-background'
              }`}
            >
              <button
                type="button"
                onClick={() => {
                  if (isImportData && !step.completed) {
                    setShowDemoConfirm(true);
                    return;
                  }
                  void handleToggle(step.stepId, !step.completed);
                }}
                disabled={isPending}
                aria-label={`Đánh dấu bước ${labels.title} ${step.completed ? 'chưa hoàn tất' : 'đã hoàn tất'}`}
                className="mt-0.5 shrink-0 rounded-full disabled:opacity-50"
              >
                {isPending ? (
                  <Loader2 className="size-5 animate-spin text-muted-foreground" aria-hidden />
                ) : step.completed ? (
                  <CheckCircle2 className="size-5 text-emerald-600 dark:text-emerald-400" aria-hidden />
                ) : (
                  <Circle className="size-5 text-muted-foreground" aria-hidden />
                )}
              </button>
              <div className="flex-1">
                <p className="font-medium">{labels.title}</p>
                <p className="text-sm text-muted-foreground">{labels.description}</p>
              </div>
            </li>
          );
        })}
      </ul>

      {error && (
        <p
          role="alert"
          data-testid="onboarding-checklist-inline-error"
          className="mt-3 text-sm text-destructive"
        >
          {error}
        </p>
      )}

      {/* GAP-545: Radix Dialog provides focus-trap + Escape + scroll-lock per WCAG 2.1.1 + 2.4.3 */}
      <Dialog open={showDemoConfirm} onOpenChange={setShowDemoConfirm}>
        <DialogContent
          data-testid="onboarding-demo-confirm"
          className="sm:max-w-md"
        >
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Sparkles className="size-5 text-primary" aria-hidden />
              Bật dữ liệu mẫu cho tài khoản?
            </DialogTitle>
            <DialogDescription>
              KiteHub sẽ tạo một bộ dữ liệu mẫu (lớp học, học viên, lịch học) để bạn khám phá nhanh
              tính năng. Bạn có thể xoá dữ liệu mẫu này bất kỳ lúc nào trong phần Cài đặt.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setShowDemoConfirm(false)}>
              Bỏ qua
            </Button>
            <Button onClick={handleImportDataOptIn} data-testid="onboarding-demo-confirm-cta">
              Bật dữ liệu mẫu
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </section>
  );
}
