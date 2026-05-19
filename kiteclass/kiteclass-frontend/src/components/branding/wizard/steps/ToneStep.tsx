'use client';

import type { Tone, WizardContext, WizardEvent } from '../types';
import { VISIBLE_FIELDS_BY_TIER } from '../types';
import { UseDefaultsButton } from '../UseDefaultsButton';

interface Props {
  context: WizardContext;
  send: (e: WizardEvent) => void;
}

const TONES: { id: Tone; label: string; description: string }[] = [
  { id: 'professional', label: 'Chuyên nghiệp', description: 'Nghiêm túc, đáng tin cậy' },
  { id: 'friendly', label: 'Thân thiện', description: 'Ấm áp, gần gũi' },
  { id: 'energetic', label: 'Năng động', description: 'Tươi sáng, nhiệt huyết' },
  { id: 'luxurious', label: 'Sang trọng', description: 'Cao cấp, tinh tế' },
  { id: 'academic', label: 'Học thuật', description: 'Nghiên cứu, chỉn chu' },
];

export function ToneStep({ context, send }: Props) {
  const fields = VISIBLE_FIELDS_BY_TIER[context.tier];
  const selected = context.inputs.tone;

  const showColorHint = fields.includes('colorHint');
  const showTypography = fields.includes('typographyHint');
  const showContentDensity = fields.includes('contentDensity');
  const showCustomPrompt = fields.includes('customPrompt');

  return (
    <div className="space-y-6">
      <header>
        <h2 className="text-2xl font-semibold">Chọn tone</h2>
        <p className="mt-2 text-muted-foreground">
          Tone là cảm xúc chung của branding. Các field bổ sung (color, typography, v.v.)
          unlock theo tier.
        </p>
      </header>

      <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3" role="radiogroup" aria-label="Phong cách">
        {TONES.map((t) => (
          <button
            key={t.id}
            type="button"
            role="radio"
            aria-checked={selected === t.id}
            onClick={() => send({ type: 'SET_TONE', tone: t.id })}
            className={`rounded-xl border p-4 text-left transition ${
              selected === t.id
                ? 'border-primary bg-primary/5 ring-2 ring-primary/30'
                : 'border-border hover:border-primary/50 hover:bg-muted/50'
            }`}
          >
            <div className="font-medium">{t.label}</div>
            <div className="mt-1 text-sm text-muted-foreground">{t.description}</div>
          </button>
        ))}
      </div>

      {(showColorHint || showTypography || showContentDensity || showCustomPrompt) && (
        <section className="space-y-4 rounded-xl border bg-muted/20 p-4">
          <div className="text-sm font-medium">
            Tinh chỉnh nâng cao ({context.tier})
          </div>

          {showColorHint && (
            <label className="block text-sm">
              <span className="mb-1 block text-muted-foreground">Gợi ý màu chủ đạo (hex)</span>
              <input
                type="text"
                placeholder="#2563EB"
                defaultValue={context.inputs.colorHint ?? ''}
                onChange={(e) =>
                  send({ type: 'SET_INPUT', field: 'colorHint', value: e.target.value })
                }
                className="w-full rounded-md border px-3 py-2"
              />
            </label>
          )}

          {showTypography && (
            <label className="block text-sm">
              <span className="mb-1 block text-muted-foreground">Kiểu typography</span>
              <select
                defaultValue={context.inputs.typographyHint ?? 'sans-serif'}
                onChange={(e) =>
                  send({ type: 'SET_INPUT', field: 'typographyHint', value: e.target.value })
                }
                className="w-full rounded-md border px-3 py-2"
              >
                <option value="sans-serif">Sans-serif (hiện đại)</option>
                <option value="serif">Serif (cổ điển)</option>
                <option value="rounded">Rounded (thân thiện)</option>
              </select>
            </label>
          )}

          {showContentDensity && (
            <label className="block text-sm">
              <span className="mb-1 block text-muted-foreground">Mật độ nội dung</span>
              <select
                defaultValue={context.inputs.contentDensity ?? 'balanced'}
                onChange={(e) =>
                  send({ type: 'SET_INPUT', field: 'contentDensity', value: e.target.value })
                }
                className="w-full rounded-md border px-3 py-2"
              >
                <option value="spacious">Thoáng</option>
                <option value="balanced">Cân bằng</option>
                <option value="dense">Dày</option>
              </select>
            </label>
          )}

          {showCustomPrompt && (
            <label className="block text-sm">
              <span className="mb-1 block text-muted-foreground">
                Prompt tùy chỉnh (Enterprise)
              </span>
              <textarea
                rows={3}
                placeholder="Mô tả thêm nếu muốn..."
                defaultValue={context.inputs.customPrompt ?? ''}
                onChange={(e) =>
                  send({ type: 'SET_INPUT', field: 'customPrompt', value: e.target.value })
                }
                className="w-full rounded-md border px-3 py-2"
              />
            </label>
          )}
        </section>
      )}

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
            Tiếp tục →
          </button>
        </div>
      </div>
    </div>
  );
}
