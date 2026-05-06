'use client';

import { useState, useEffect, useRef } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { ArrowRight, AlertCircle, Check, Loader2, Sparkles } from 'lucide-react';
import { WizardCard, WizardStepHeader } from './wizard-shared';

// ---------------------------------------------------------------------------
// Slug utilities
// ---------------------------------------------------------------------------

/** Convert a free-text name to a URL-safe slug. */
function nameToSlug(name: string): string {
  return name
    .toLowerCase()
    .normalize('NFD')
    // Remove diacritics (Vietnamese characters → ASCII base)
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/[^a-z0-9\s-]/g, '')
    .trim()
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-');
}

// ---------------------------------------------------------------------------
// Sub-state machine for slug validation
// ---------------------------------------------------------------------------

type SlugStatus = 'idle' | 'validating' | 'available' | 'conflict';

// Mock taken slugs — in production this calls the real availability endpoint.
const MOCK_TAKEN_SLUGS = new Set(['toan-master', 'hoc-vien-abc', 'trung-tam-anh-ngu']);
const MOCK_SUGGESTIONS = (slug: string) => [
  `${slug}-2026`,
  `${slug}-edu`,
  `tt-${slug}`,
  `${slug}-vn`,
];

// ---------------------------------------------------------------------------
// WelcomeStep — Step 1 of the 6-step wizard
// ---------------------------------------------------------------------------

export interface WelcomeStepData {
  tenantName: string;
  slug: string;
}

interface WelcomeStepProps {
  /** Initial values (populated when user navigates back). */
  initialData?: Partial<WelcomeStepData>;
  onNext: (data: WelcomeStepData) => void;
}

/**
 * Step 1 — Welcome & Center Info.
 *
 * Sub-states: default | validating | conflict
 * Spec: `ai-branding-wizard-v2/screens/step1-welcome-{default,validating,conflict}.html`
 * Rule: ai-branding-guidelines.md §4.1
 */
export function WelcomeStep({ initialData, onNext }: WelcomeStepProps) {
  const [tenantName, setTenantName] = useState(initialData?.tenantName ?? '');
  const [slug, setSlug] = useState(initialData?.slug ?? '');
  const [slugStatus, setSlugStatus] = useState<SlugStatus>('idle');
  const [suggestions, setSuggestions] = useState<string[]>([]);

  // Debounce ref so we don't fire on every keystroke
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Auto-derive slug when tenant name changes (only if slug hasn't been
  // manually edited yet — once user edits slug directly, stop auto-fill).
  // If initialData.slug is provided, treat it as manually edited so the
  // auto-derive effect doesn't overwrite the restored value.
  const [slugManuallyEdited, setSlugManuallyEdited] = useState(
    !!(initialData?.slug),
  );

  useEffect(() => {
    if (!slugManuallyEdited && tenantName) {
      setSlug(nameToSlug(tenantName));
    }
  }, [tenantName, slugManuallyEdited]);

  // Validate slug with debounce whenever slug changes
  useEffect(() => {
    if (!slug) {
      setSlugStatus('idle');
      return;
    }

    // Reset status while user is typing
    setSlugStatus('validating');
    setSuggestions([]);

    if (debounceRef.current) clearTimeout(debounceRef.current);

    debounceRef.current = setTimeout(() => {
      // Mock validation — replace with real API call in production
      const isTaken = MOCK_TAKEN_SLUGS.has(slug);
      if (isTaken) {
        setSlugStatus('conflict');
        setSuggestions(MOCK_SUGGESTIONS(slug));
      } else {
        setSlugStatus('available');
      }
    }, 600);

    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [slug]);

  const handleSlugChange = (value: string) => {
    setSlugManuallyEdited(true);
    // Only allow lowercase letters, numbers, hyphens
    setSlug(value.toLowerCase().replace(/[^a-z0-9-]/g, ''));
  };

  const canProceed =
    tenantName.trim().length >= 2 &&
    slug.length >= 2 &&
    slugStatus === 'available';

  const handleNext = () => {
    if (!canProceed) return;
    onNext({ tenantName: tenantName.trim(), slug });
  };

  return (
    <div className="space-y-6">
      <WizardStepHeader
        eyebrow="Bước 1 / 6"
        title="Chào mừng đến với Kite Branding Studio"
        subtitle="Hệ thống AI sẽ tạo trang web cho trung tâm của bạn dựa trên 4 lựa chọn nhỏ. Bạn không cần kỹ năng thiết kế — chỉ cần chọn vài tuỳ chọn, AI sẽ lo phần còn lại."
      />

      <WizardCard>
        <div className="space-y-5">
          {/* Tenant name */}
          <div className="space-y-1.5">
            <Label htmlFor="tenant-name">
              Tên trung tâm <span className="text-destructive" aria-label="bắt buộc">*</span>
            </Label>
            <Input
              id="tenant-name"
              type="text"
              value={tenantName}
              onChange={(e) => setTenantName(e.target.value)}
              placeholder="VD: Trung tâm Toán Master"
              autoComplete="organization"
              autoFocus
            />
            <p className="text-xs text-muted-foreground">
              Tên này sẽ xuất hiện trên trang web, hoá đơn, và email gửi học viên.
            </p>
          </div>

          {/* Slug */}
          <div className="space-y-1.5">
            <Label>Đường dẫn website</Label>
            <div
              className={[
                'flex items-center rounded-md border bg-background text-sm transition-all overflow-hidden',
                slugStatus === 'conflict'
                  ? 'border-destructive ring-2 ring-destructive/20'
                  : slugStatus === 'available'
                  ? 'border-emerald-500 ring-2 ring-emerald-500/20'
                  : 'border-input',
              ].join(' ')}
            >
              <span className="px-3 py-2 bg-muted text-muted-foreground border-r border-input shrink-0 text-xs">
                https://
              </span>
              <input
                className="flex-1 px-3 py-2 bg-transparent outline-none text-sm placeholder:text-muted-foreground"
                type="text"
                value={slug}
                onChange={(e) => handleSlugChange(e.target.value)}
                placeholder="ten-trung-tam"
                aria-describedby="slug-status"
              />
              <span className="px-3 py-2 bg-muted text-muted-foreground border-l border-input shrink-0 text-xs">
                .kiteclass.vn
              </span>
            </div>

            {/* Slug status indicator */}
            <SlugStatusMessage
              status={slugStatus}
              slug={slug}
              suggestions={suggestions}
              onSuggestionClick={(s) => {
                setSlug(s);
                setSlugManuallyEdited(true);
              }}
            />

            <p className="text-xs text-muted-foreground">
              Chỉ chữ thường, số, và dấu gạch nối. AI sẽ gợi ý nếu trùng.
            </p>
          </div>

          {/* Tip banner */}
          <div
            className="bg-sky-50 dark:bg-sky-950/30 border border-sky-200 dark:border-sky-800 rounded-lg p-4 flex gap-3"
            role="note"
          >
            <Sparkles className="w-5 h-5 text-sky-600 dark:text-sky-400 mt-0.5 shrink-0" aria-hidden="true" />
            <div className="text-sm text-sky-900 dark:text-sky-100">
              <p className="font-semibold mb-1">Mẹo cho người mới</p>
              <p>
                Bạn có thể bỏ qua bước upload logo — AI sẽ tự tạo logo từ tên trung tâm.
                Nâng cấp hoặc đổi sau lúc nào cũng được.
              </p>
            </div>
          </div>
        </div>
      </WizardCard>

      {/* Footer nav */}
      <div className="flex items-center justify-between max-w-2xl mx-auto px-1">
        <Button variant="ghost" disabled aria-disabled="true">
          Quay lại
        </Button>
        <p className="text-xs text-muted-foreground hidden sm:block">Bước 1 / 6 · Mất ~5 phút</p>
        <Button
          onClick={handleNext}
          disabled={!canProceed}
        >
          Tiếp tục
          <ArrowRight className="ml-2 w-4 h-4" aria-hidden="true" />
        </Button>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// SlugStatusMessage — renders the appropriate sub-state indicator
// ---------------------------------------------------------------------------

interface SlugStatusMessageProps {
  status: SlugStatus;
  slug: string;
  suggestions: string[];
  onSuggestionClick: (slug: string) => void;
}

function SlugStatusMessage({
  status,
  slug,
  suggestions,
  onSuggestionClick,
}: SlugStatusMessageProps) {
  if (status === 'idle') return null;

  if (status === 'validating') {
    return (
      <div
        id="slug-status"
        className="flex items-center gap-2 text-sm text-sky-600 dark:text-sky-400"
        role="status"
        aria-live="polite"
      >
        <Loader2 className="w-3.5 h-3.5 animate-spin" aria-hidden="true" />
        <span>
          Đang kiểm tra{' '}
          <strong>
            {slug}.kiteclass.vn
          </strong>
          …
        </span>
      </div>
    );
  }

  if (status === 'available') {
    return (
      <div
        id="slug-status"
        className="flex items-center gap-2 text-sm text-emerald-600 dark:text-emerald-400"
        role="status"
        aria-live="polite"
      >
        <Check className="w-3.5 h-3.5" aria-hidden="true" />
        <span>
          <strong>{slug}.kiteclass.vn</strong> có thể sử dụng.
        </span>
      </div>
    );
  }

  // conflict
  return (
    <div id="slug-status" aria-live="assertive">
      <div className="flex items-start gap-2 text-sm text-destructive">
        <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" aria-hidden="true" />
        <span role="alert">
          <strong>{slug}.kiteclass.vn</strong> đã được trung tâm khác sử dụng.
        </span>
      </div>

      {suggestions.length > 0 && (
        <div className="mt-2">
          <p className="text-xs font-semibold text-foreground mb-1.5">Gợi ý cho bạn:</p>
          <div className="flex flex-wrap gap-1.5">
            {suggestions.map((s) => (
              <button
                key={s}
                type="button"
                onClick={() => onSuggestionClick(s)}
                className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-muted hover:bg-accent border border-input text-xs font-mono transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
              >
                <Check className="w-3 h-3 text-emerald-600" aria-hidden="true" />
                {s}
              </button>
            ))}
          </div>
          <p className="text-xs text-muted-foreground mt-1.5">
            Bấm vào tên gợi ý để dùng. Hoặc tự nhập tên mới phía trên.
          </p>
        </div>
      )}
    </div>
  );
}
