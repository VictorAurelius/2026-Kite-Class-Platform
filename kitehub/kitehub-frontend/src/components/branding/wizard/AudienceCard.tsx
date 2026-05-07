'use client';

// ---------------------------------------------------------------------------
// AudienceCard — single audience card for Step 3 (Bucket B)
//
// Renders emoji + label + description + example + selected check icon.
// Spec source: ui_kits/ai-branding-wizard-v2/screens/step3-audience-{default,selected}.html
// Per ai-branding-guidelines.md §2.1 (constrained presets, not free-form).
// ---------------------------------------------------------------------------

import { CheckCircle2 } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface AudienceOption {
  /** Stable persistence key (e.g. "preschool", "secondary"). */
  id: string;
  /** Decorative emoji. */
  emoji: string;
  /** VN-language card title. */
  title: string;
  /** Short pitch (sub-line on card). */
  description: string;
  /** Example tenant names ("VD: ..."). */
  example: string;
}

interface AudienceCardProps {
  option: AudienceOption;
  selected: boolean;
  onSelect: (id: string) => void;
}

export function AudienceCard({ option, selected, onSelect }: AudienceCardProps) {
  return (
    <button
      type="button"
      role="radio"
      aria-checked={selected}
      data-selected={selected}
      onClick={() => onSelect(option.id)}
      className={cn(
        'group relative flex flex-col gap-2 rounded-xl border bg-card p-5 text-left transition',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2',
        selected
          ? 'border-primary ring-2 ring-primary/40 shadow-sm'
          : 'border-border hover:border-primary/50 hover:shadow-sm',
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
      <div className="mt-2 text-xs text-muted-foreground/80">{option.example}</div>
    </button>
  );
}
