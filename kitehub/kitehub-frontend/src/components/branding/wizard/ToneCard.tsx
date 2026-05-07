'use client';

// ---------------------------------------------------------------------------
// ToneCard — single tone card for Step 4 (Wave 32 Bucket B)
//
// Each card renders TINY preview of (headline + button) using the tone's
// design tokens — strong "see before you choose" signal per ai-branding-
// guidelines.md §2.2 (template-first decision visualisation).
//
// Spec source: ui_kits/ai-branding-wizard-v2/screens/step4-tone-{default,selected}.html
// ---------------------------------------------------------------------------

import { CheckCircle2 } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface TonePreviewSwatch {
  /** Card background color (hex or hsl). */
  cardBg: string;
  /** Card border color. */
  cardBorder: string;
  /** Mini preview block background. */
  previewBg: string;
  /** Mini preview headline color. */
  previewHeadingColor: string;
  /** Mini preview button background. */
  previewBtnBg: string;
  /** Mini preview button text color. */
  previewBtnColor: string;
  /** Border radius CSS value (e.g. "4px" for pro, "999px" for friend). */
  previewBtnRadius: string;
}

export interface ToneOption {
  /** Stable persistence key (e.g. "professional", "friendly"). */
  id: string;
  /** Decorative emoji. */
  emoji: string;
  /** VN-language card title. */
  title: string;
  /** Short pitch (sub-line). */
  description: string;
  /** Sample heading for the tiny preview block. */
  sampleHeadline: string;
  /** Sample button label. */
  sampleButtonLabel: string;
  /** "Phù hợp ..." footer hint. */
  fitHint: string;
  /** Per-tone visual palette. */
  swatch: TonePreviewSwatch;
}

interface ToneCardProps {
  option: ToneOption;
  selected: boolean;
  onSelect: (id: string) => void;
}

export function ToneCard({ option, selected, onSelect }: ToneCardProps) {
  const { swatch } = option;
  return (
    <button
      type="button"
      role="radio"
      aria-checked={selected}
      data-selected={selected}
      data-tone={option.id}
      onClick={() => onSelect(option.id)}
      style={{
        backgroundColor: swatch.cardBg,
        borderColor: selected ? 'hsl(var(--primary))' : swatch.cardBorder,
      }}
      className={cn(
        'group relative flex flex-col gap-2 rounded-xl border p-5 text-left transition',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2',
        selected ? 'shadow-md ring-2 ring-primary/40' : 'hover:shadow-sm',
      )}
    >
      <div className="flex items-start justify-between">
        <div className="text-3xl leading-none" aria-hidden="true">
          {option.emoji}
        </div>
        {selected && (
          <CheckCircle2 className="h-5 w-5 text-emerald-600" aria-hidden="true" />
        )}
      </div>
      <div className="text-base font-semibold text-foreground">{option.title}</div>
      <p className="text-sm text-muted-foreground">{option.description}</p>

      {/* Tiny rendered preview block — the differentiator vs text-only cards.
          Renders sample heading + sample button in tone-specific tokens. */}
      <div
        data-testid={`tone-preview-${option.id}`}
        className="mt-2 flex flex-col items-start gap-2 rounded-lg p-3"
        style={{ backgroundColor: swatch.previewBg }}
      >
        <div
          className="text-sm font-semibold"
          style={{ color: swatch.previewHeadingColor }}
        >
          {option.sampleHeadline}
        </div>
        <span
          className="inline-flex items-center px-3 py-1 text-xs font-medium"
          style={{
            backgroundColor: swatch.previewBtnBg,
            color: swatch.previewBtnColor,
            borderRadius: swatch.previewBtnRadius,
          }}
        >
          {option.sampleButtonLabel}
        </span>
      </div>

      <div className="mt-1 text-xs text-muted-foreground">{option.fitHint}</div>
    </button>
  );
}
