'use client';

/**
 * BrandPersonalityStep — Step 2 of the output-first AI Branding Wizard (GAP-1216).
 *
 * Merges the former Audience (Step 4) + Tone (Step 5) screens onto ONE page so
 * the user expresses the full "brand personality" before the wizard generates a
 * real preview (output-first per kit v3 §2.5, hội tụ 3 audit 2026-06-11).
 *
 * "Components giữ": the card primitives (`AudienceCard` / `ToneCard`) + the
 * option data (`AUDIENCE_OPTIONS` / `TONE_OPTIONS`) + reasoning maps are reused
 * verbatim from the original step components — only the page-level composition +
 * the single shared footer are new here.
 *
 * Selections dispatch to WizardState immediately (SET_AUDIENCE / SET_TONE) so the
 * orchestrator reducer owns the canonical fields (rework §3.1 anti-pattern guard
 * — no shadow useState copies of canonical fields).
 */

import { ArrowLeft, ArrowRight, Sparkles, Palette } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { AudienceCard } from './AudienceCard';
import { ToneCard } from './ToneCard';
import { AUDIENCE_OPTIONS, AUDIENCE_REASONING } from './AudienceStep';
import { TONE_OPTIONS, TONE_REASONING } from './ToneStep';
import type { WizardState, WizardAction } from './wizard-shared';

export interface BrandPersonalityStepProps {
  wizardState: WizardState;
  dispatch: React.Dispatch<WizardAction>;
  onNext: () => void;
  onBack: () => void;
}

export function BrandPersonalityStep({
  wizardState,
  dispatch,
  onNext,
  onBack,
}: BrandPersonalityStepProps) {
  const { audience, tone } = wizardState;

  const audienceReasoning = audience ? AUDIENCE_REASONING[audience] : null;
  const selectedAudience = AUDIENCE_OPTIONS.find((o) => o.id === audience);
  const toneReasoning = tone ? TONE_REASONING[tone] : null;
  const selectedTone = TONE_OPTIONS.find((t) => t.id === tone);

  const canContinue = Boolean(audience) && Boolean(tone);

  return (
    <div className="mx-auto w-full max-w-3xl space-y-8" data-testid="brand-personality-step">
      <div className="space-y-2">
        <p className="text-sm font-semibold uppercase tracking-wide text-primary">Bước 2 / 5</p>
        <h1 className="text-2xl font-bold text-foreground">Phong cách thương hiệu</h1>
        <p className="text-muted-foreground">
          Chọn <strong>đối tượng học viên</strong> và <strong>phong cách</strong> — AI sẽ dùng cả
          hai để tạo bản xem trước thật ở bước tiếp theo. Đổi sau lúc nào cũng được.
        </p>
      </div>

      {/* ---- Audience ---- */}
      <section className="space-y-4" data-testid="personality-audience">
        <h2 className="text-base font-bold text-foreground">1 · Trung tâm bạn dạy ai?</h2>
        <div
          role="radiogroup"
          aria-label="Đối tượng học viên"
          className="grid grid-cols-1 gap-4 sm:grid-cols-2"
        >
          {AUDIENCE_OPTIONS.map((option) => (
            <AudienceCard
              key={option.id}
              option={option}
              selected={audience === option.id}
              onSelect={(id) => dispatch({ type: 'SET_AUDIENCE', audience: id })}
            />
          ))}
        </div>
        {audienceReasoning && selectedAudience && (
          <div
            data-testid="audience-reasoning"
            className="rounded-lg border border-primary/25 bg-primary/[0.06] p-4"
          >
            <div className="flex items-start gap-3">
              <Sparkles className="mt-0.5 h-5 w-5 shrink-0 text-primary" aria-hidden="true" />
              <div className="text-sm">
                <p className="mb-2 font-semibold">AI đã hiểu hướng đi</p>
                <p className="text-muted-foreground">{audienceReasoning.headline}</p>
                <ul className="mt-2 space-y-1 text-muted-foreground">
                  {audienceReasoning.bullets.map((line) => (
                    <li key={line}>• {line}</li>
                  ))}
                </ul>
              </div>
            </div>
          </div>
        )}
      </section>

      {/* ---- Tone ---- */}
      <section className="space-y-4" data-testid="personality-tone">
        <h2 className="text-base font-bold text-foreground">2 · Chọn phong cách</h2>
        <div
          role="radiogroup"
          aria-label="Phong cách thương hiệu"
          className="grid grid-cols-1 gap-4 sm:grid-cols-2"
        >
          {TONE_OPTIONS.map((option) => (
            <ToneCard
              key={option.id}
              option={option}
              selected={tone === option.id}
              onSelect={(id) => dispatch({ type: 'SET_TONE', tone: id })}
            />
          ))}
        </div>
        {toneReasoning && selectedTone && (
          <div
            data-testid="tone-reasoning"
            className="rounded-lg border border-primary/25 bg-primary/[0.06] p-4"
          >
            <div className="flex items-start gap-3">
              <Palette className="mt-0.5 h-5 w-5 shrink-0 text-primary" aria-hidden="true" />
              <div className="text-sm">
                <p className="mb-1 font-semibold">
                  {toneReasoning.shortHeadline}
                  {selectedAudience ? ` + ${selectedAudience.title}` : ''}
                </p>
                <p className="text-muted-foreground">{toneReasoning.body}</p>
              </div>
            </div>
          </div>
        )}
      </section>

      <div className="flex items-center justify-between border-t pt-4">
        <Button variant="ghost" onClick={onBack} data-testid="personality-back">
          <ArrowLeft className="mr-2 h-4 w-4" aria-hidden="true" />
          Quay lại
        </Button>
        <p className="text-xs text-muted-foreground">
          Bước 2 / 5 ·{' '}
          {canContinue
            ? 'Đã chọn đối tượng + phong cách'
            : 'Chọn cả đối tượng và phong cách để tiếp tục'}
        </p>
        <Button onClick={onNext} disabled={!canContinue} data-testid="personality-continue">
          Tiếp tục
          <ArrowRight className="ml-2 h-4 w-4" aria-hidden="true" />
        </Button>
      </div>
    </div>
  );
}
