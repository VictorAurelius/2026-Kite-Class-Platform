'use client';

import type { WizardContext, WizardEvent, TemplateCandidate, Segment } from '../types';
import { UseDefaultsButton } from '../UseDefaultsButton';

interface Props {
  context: WizardContext;
  send: (e: WizardEvent) => void;
}

// Scaffold templates — replaced with backend-fetched library when GAP-011 lands.
const TEMPLATES: TemplateCandidate[] = [
  {
    id: 'k12-warm-v1',
    label: 'K-12 Warm · Red accent',
    previewUrl: '/templates/k12-warm-v1.png',
    segments: ['K12'],
  },
  {
    id: 'k12-calm-v1',
    label: 'K-12 Calm · Blue accent',
    previewUrl: '/templates/k12-calm-v1.png',
    segments: ['K12'],
  },
  {
    id: 'center-energy-v1',
    label: 'Center Energy · Vibrant',
    previewUrl: '/templates/center-energy-v1.png',
    segments: ['CENTER'],
  },
  {
    id: 'univ-formal-v1',
    label: 'University Formal · Muted',
    previewUrl: '/templates/univ-formal-v1.png',
    segments: ['UNIV'],
  },
  {
    id: 'corp-clean-v1',
    label: 'Corporate Clean · Minimal',
    previewUrl: '/templates/corp-clean-v1.png',
    segments: ['CORP'],
  },
  {
    id: 'default-template-v1',
    label: 'Default · Works everywhere',
    previewUrl: '/templates/default-template-v1.png',
    segments: ['K12', 'CENTER', 'UNIV', 'CORP', 'OTHER'],
  },
];

export function TemplateStep({ context, send }: Props) {
  const segment: Segment = context.inputs.segment ?? 'OTHER';
  const filtered = TEMPLATES.filter((t) => t.segments.includes(segment));
  const selected = context.inputs.templateId;

  return (
    <div className="space-y-6">
      <header>
        <h2 className="text-2xl font-semibold">Chọn template</h2>
        <p className="mt-2 text-muted-foreground">
          Gợi ý theo segment đã chọn. Không có free-form prompt — các preset dưới đây
          được review kỹ về brand quality.
        </p>
      </header>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3" role="radiogroup" aria-label="Mẫu">
        {filtered.map((t) => (
          <button
            key={t.id}
            type="button"
            role="radio"
            aria-checked={selected === t.id}
            onClick={() => send({ type: 'SET_TEMPLATE', templateId: t.id })}
            className={`overflow-hidden rounded-xl border text-left transition ${
              selected === t.id
                ? 'border-primary ring-2 ring-primary/30'
                : 'border-border hover:border-primary/50'
            }`}
          >
            <div
              className="aspect-video w-full bg-muted"
              style={{
                backgroundImage: `url(${t.previewUrl})`,
                backgroundSize: 'cover',
                backgroundPosition: 'center',
              }}
              aria-label={`Xem trước ${t.label}`}
            />
            <div className="p-3">
              <div className="font-medium">{t.label}</div>
              <div className="mt-1 text-xs text-muted-foreground">{t.id}</div>
            </div>
          </button>
        ))}
      </div>

      <div className="flex flex-wrap items-center justify-between gap-2">
        <button type="button" onClick={() => send({ type: 'BACK' })} className="text-sm text-muted-foreground">
          ← Quay lại
        </button>
        <div className="flex flex-wrap gap-2">
          <UseDefaultsButton send={send} />
          <button
            type="button"
            disabled={!selected}
            onClick={() => send({ type: 'NEXT' })}
            className="rounded-md bg-primary px-4 py-2 text-primary-foreground disabled:opacity-40"
          >
            Xem preview →
          </button>
        </div>
      </div>
    </div>
  );
}
