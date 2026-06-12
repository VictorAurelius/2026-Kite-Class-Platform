'use client';

/**
 * Wave 32 Bucket A — Step 1 Welcome: tenant name + slug validation.
 *
 * Spec source:
 *   - documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/step1-welcome-default.html
 *   - documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/step1-welcome-validating.html
 *   - documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/step1-welcome-conflict.html
 *
 * Sub-states (driven by WizardState.slugStatus):
 *   - default     : empty / typing — Continue disabled
 *   - validating  : 600ms debounced check in flight
 *   - conflict    : taken — alternative suggestions shown, click pill to adopt
 *   - available   : confirmed available — Continue enabled
 *
 * Wave 34 Bucket D (GAP-272i): real `/api/v1/branding/slug-availability`
 * endpoint shipped Bucket A. WelcomeStep consumes via `useSlugAvailability`
 * hook → `apiClient.get()`. MSW handler in tests (`brandingHandlers`)
 * matches the contract; production calls hit the live backend.
 */

import { useEffect, useMemo, useRef } from 'react';
import { Button } from '@/components/ui/button';
import { ArrowRight, AlertCircle, CheckCircle2, Loader2, Sparkles } from 'lucide-react';
import {
  WizardCard,
  WizardStepHeader,
  type WizardState,
  type WizardAction,
} from './wizard-shared';
import { GenerationModeSelector } from './GenerationModeSelector';
import { useSlugAvailability } from './hooks';
import type { PricingTier } from '@/types/subscription';

// Debounce window for slug validation (ms). Matches kit's "validating" sub-state expectation.
const DEBOUNCE_MS = 600;

const SLUG_PATTERN = /^[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$/;
const SLUG_MIN_LENGTH = 3;

export interface WelcomeStepProps {
  wizardState: WizardState;
  dispatch: React.Dispatch<WizardAction>;
  onNext: () => void;
  /**
   * GAP-1216 — subscription tier drives FULL_AI eligibility in the embedded
   * mode selector. Optional (defaults FREE) so unit tests can render the step
   * without a tier source.
   */
  tier?: PricingTier;
  /** FULL_AI upgrade CTA handler (FREE/BASIC). */
  onUpgradeClick?: () => void;
}

export function WelcomeStep({
  wizardState,
  dispatch,
  onNext,
  tier = 'FREE',
  onUpgradeClick,
}: WelcomeStepProps) {
  const { tenantName, slug, slugStatus, conflictSuggestions, mode } = wizardState;
  const { checkSlug } = useSlugAvailability();

  // Track active in-flight slug to discard stale responses.
  const activeSlugRef = useRef<string>('');

  const slugSyntaxValid = useMemo(
    () => slug.length >= SLUG_MIN_LENGTH && SLUG_PATTERN.test(slug),
    [slug]
  );

  // Debounced validation. Only fires when slug syntax is valid; otherwise we
  // stay in 'default' and let the inline hint guide the user.
  useEffect(() => {
    if (!slugSyntaxValid) {
      activeSlugRef.current = '';
      return;
    }

    const target = slug;
    activeSlugRef.current = target;
    dispatch({ type: 'SET_SLUG_STATUS', status: 'validating' });

    const timer = window.setTimeout(async () => {
      try {
        const result = await checkSlug(target);
        if (activeSlugRef.current !== target) return; // stale
        if (result.available) {
          dispatch({ type: 'SET_SLUG_STATUS', status: 'available' });
        } else {
          dispatch({
            type: 'SET_SLUG_STATUS',
            status: 'conflict',
            suggestions: result.suggestions,
          });
        }
      } catch {
        // On error, keep user in default state (do not block).
        if (activeSlugRef.current === target) {
          dispatch({ type: 'SET_SLUG_STATUS', status: 'default' });
        }
      }
    }, DEBOUNCE_MS);

    return () => window.clearTimeout(timer);
  }, [slug, slugSyntaxValid, dispatch, checkSlug]);

  const handleAdoptSuggestion = (suggestion: string) => {
    dispatch({ type: 'SET_SLUG', slug: suggestion });
  };

  const canContinue =
    tenantName.trim().length > 0 && slugStatus === 'available';

  return (
    <div className="space-y-6">
      <WizardCard>
        <WizardStepHeader
          eyebrow="Bước 1 / 5 · Bắt đầu"
          title="Cùng tạo thương hiệu cho trung tâm của bạn 👋"
          subtitle="Chỉ cần tên trung tâm và đường dẫn — AI sẽ tạo bản xem trước trang giới thiệu thật. Bạn không cần kỹ năng thiết kế; chỉnh lại lúc nào cũng được."
        />

        <div className="space-y-5">
          {/* Tenant name */}
          <div>
            <label
              htmlFor="wizard-tenant-name"
              className="block text-sm font-semibold mb-1"
            >
              Tên hiển thị của trung tâm <span className="text-destructive">*</span>
            </label>
            <input
              id="wizard-tenant-name"
              type="text"
              autoComplete="organization"
              placeholder="VD: Trung tâm Tiếng Anh Cô Hà"
              value={tenantName}
              onChange={(e) =>
                dispatch({ type: 'SET_TENANT_NAME', tenantName: e.target.value })
              }
              className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            />
            <p className="text-xs text-muted-foreground mt-1">
              Tên này hiển thị trên trang giới thiệu, hóa đơn và thông báo Zalo gửi phụ huynh.
            </p>
          </div>

          {/* Slug */}
          <div>
            <label
              htmlFor="wizard-slug"
              className="block text-sm font-semibold mb-1"
            >
              Địa chỉ trang giới thiệu
            </label>
            <div
              data-testid="wizard-slug-row"
              className={[
                'flex items-stretch border rounded-md overflow-hidden transition-colors',
                slugStatus === 'conflict'
                  ? 'border-destructive ring-2 ring-destructive/30'
                  : slugStatus === 'available'
                    ? 'border-emerald-500/60'
                    : 'border-input',
              ].join(' ')}
            >
              <span className="px-3 py-2 text-sm bg-muted text-muted-foreground border-r border-input">
                https://
              </span>
              <input
                id="wizard-slug"
                type="text"
                placeholder="ten-trung-tam"
                value={slug}
                onChange={(e) =>
                  dispatch({ type: 'SET_SLUG', slug: e.target.value.toLowerCase() })
                }
                className="flex-1 px-3 py-2 text-sm bg-background text-foreground focus:outline-none"
              />
              <span className="px-3 py-2 text-sm bg-muted text-muted-foreground border-l border-input">
                .kitehub.me
              </span>
            </div>

            {/* Sub-state messaging */}
            {slug.length > 0 && !slugSyntaxValid && (
              <p
                data-testid="wizard-slug-syntax-hint"
                className="text-xs text-muted-foreground mt-1"
              >
                Chỉ chữ thường, số, và dấu gạch nối. Tối thiểu {SLUG_MIN_LENGTH} ký tự.
              </p>
            )}

            {slugStatus === 'validating' && (
              <p
                data-testid="wizard-slug-validating"
                role="status"
                className="flex items-center gap-2 text-xs text-muted-foreground mt-2"
              >
                <Loader2 className="w-3 h-3 animate-spin" />
                Đang kiểm tra đường dẫn…
              </p>
            )}

            {slugStatus === 'available' && (
              <p
                data-testid="wizard-slug-available"
                className="flex items-center gap-2 text-xs text-emerald-600 dark:text-emerald-400 mt-2"
              >
                <CheckCircle2 className="w-3 h-3" />
                <span>
                  <strong>{slug}.kitehub.me</strong> còn trống — bạn dùng được.
                </span>
              </p>
            )}

            {slugStatus === 'conflict' && (
              <div data-testid="wizard-slug-conflict" className="mt-2">
                <p
                  role="alert"
                  className="flex items-center gap-2 text-xs text-destructive"
                >
                  <AlertCircle className="w-3 h-3" />
                  <span>
                    <strong>{slug}.kitehub.me</strong> đã được trung tâm khác sử
                    dụng.
                  </span>
                </p>
                {conflictSuggestions.length > 0 && (
                  <>
                    <p className="text-xs font-semibold text-foreground mt-3 mb-2">
                      Gợi ý cho bạn:
                    </p>
                    <div
                      data-testid="wizard-slug-suggestions"
                      className="flex flex-wrap gap-2"
                    >
                      {conflictSuggestions.map((sug) => (
                        <button
                          key={sug}
                          type="button"
                          onClick={() => handleAdoptSuggestion(sug)}
                          className="px-3 py-1 text-xs font-mono bg-muted hover:bg-primary/10 border border-input rounded-full transition-colors"
                        >
                          {sug}
                        </button>
                      ))}
                    </div>
                  </>
                )}
              </div>
            )}
          </div>

          {/* Generation mode (GAP-1216 / GAP-1142) — moved to Step 1 so the
              TEMPLATE/FULL_AI choice is made up front (output-first flow). It
              drives the Step-3 Portrait branch + Step-4 Template skip.
              GAP-1231: org-type card dropped per kit v3 (no org-type in the kit);
              `orgType` defaults to SMALL_CENTER in WizardState. */}
          <div data-testid="welcome-generation-mode">
            <p className="block text-sm font-semibold mb-1">Cách AI tạo banner cho trung tâm</p>
            <GenerationModeSelector
              tier={tier}
              value={mode}
              onChange={(m) => dispatch({ type: 'SET_MODE', mode: m })}
              onUpgradeClick={onUpgradeClick}
            />
          </div>

          {/* Tip */}
          <div className="bg-sky-50 dark:bg-sky-950/30 border border-sky-200 dark:border-sky-900 rounded-md p-3">
            <p className="text-sm font-semibold text-sky-900 dark:text-sky-200 mb-1">
              Mẹo cho người mới
            </p>
            <p className="text-xs text-sky-900/90 dark:text-sky-200/90">
              Bạn có thể bỏ qua bước upload logo — hệ thống tự tạo logo chữ lồng
              (monogram) từ tên trung tâm + màu thương hiệu. Đổi sau lúc nào cũng được.
            </p>
          </div>
        </div>
      </WizardCard>

      {/* Footer actions */}
      <div className="flex items-center justify-between max-w-2xl mx-auto px-1">
        {/* GAP-1219(c): escape-ramp ngay từ Welcome — benchmark norm "logo không
            bắt buộc, defaults trước, refine sau". Áp defaults an toàn theo orgType
            rồi nhảy thẳng: TEMPLATE → chọn Mẫu (bước 4); FULL_AI → Tạo & Duyệt
            (bước 5, Template được bỏ qua per GAP-1216). */}
        <Button
          variant="ghost"
          disabled={!canContinue}
          data-testid="wizard-step1-use-defaults"
          onClick={() => {
            dispatch({
              type: 'SET_AUDIENCE',
              audience: wizardState.orgType === 'SOLO_TEACHER' ? 'exam-prep' : 'english-center',
            });
            dispatch({ type: 'SET_TONE', tone: 'professional' });
            dispatch({ type: 'GO_TO_STEP', step: mode === 'FULL_AI' ? 5 : 4 });
          }}
        >
          <Sparkles className="mr-2 h-4 w-4" />
          Tạo ngay với mặc định
        </Button>
        <p className="text-xs text-muted-foreground">Bước 1 / 5 · Mất ~5 phút</p>
        <Button onClick={onNext} disabled={!canContinue} data-testid="wizard-step1-continue">
          Tiếp tục
          <ArrowRight className="ml-2 h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}
