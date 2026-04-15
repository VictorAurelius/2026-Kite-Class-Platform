'use client';

import type { WizardContext, WizardEvent } from '../types';
import { RegenerateCounter } from '../RegenerateCounter';

interface Props {
  context: WizardContext;
  send: (e: WizardEvent) => void;
  onSubmit: () => void;
}

export function PreviewStep({ context, send, onSubmit }: Props) {
  const { inputs, regenerateCount, regenerateLimit, tier } = context;
  const blocked = regenerateCount >= regenerateLimit && Number.isFinite(regenerateLimit);

  return (
    <div className="space-y-6">
      <header>
        <h2 className="text-2xl font-semibold">Review & deploy</h2>
        <p className="mt-2 text-muted-foreground">
          Xác nhận thông tin. Sau khi deploy, branding sẽ được generate trong 2–5 phút.
          Bạn có thể regenerate từng asset riêng lẻ.
        </p>
      </header>

      <section className="rounded-xl border p-4" aria-label="Tóm tắt wizard inputs">
        <dl className="grid gap-3 text-sm md:grid-cols-2">
          <Entry label="Segment" value={inputs.segment} />
          <Entry label="Audience" value={inputs.audiences.join(', ') || '—'} />
          <Entry label="Tone" value={inputs.tone} />
          <Entry label="Template" value={inputs.templateId} />
          {inputs.colorHint && <Entry label="Color hint" value={inputs.colorHint} />}
          {inputs.typographyHint && (
            <Entry label="Typography" value={inputs.typographyHint} />
          )}
          {inputs.contentDensity && (
            <Entry label="Content density" value={inputs.contentDensity} />
          )}
          {inputs.customPrompt && (
            <Entry label="Custom prompt" value={inputs.customPrompt} />
          )}
        </dl>
      </section>

      <section aria-label="Live preview" className="space-y-2">
        <div className="text-sm font-medium">Live preview</div>
        <div className="aspect-video w-full overflow-hidden rounded-xl border bg-muted">
          <iframe
            title="Branding preview"
            className="h-full w-full"
            src={`about:blank`}
          />
        </div>
      </section>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <RegenerateCounter tier={tier} used={regenerateCount} limit={regenerateLimit} />
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => send({ type: 'BACK' })}
            className="text-sm text-muted-foreground"
          >
            ← Chỉnh sửa
          </button>
          <button
            type="button"
            disabled={blocked}
            onClick={() => send({ type: 'REGENERATE' })}
            className="rounded-md border px-4 py-2 text-sm disabled:opacity-40"
          >
            🔄 Regenerate
          </button>
          <button
            type="button"
            onClick={onSubmit}
            className="rounded-md bg-primary px-4 py-2 font-medium text-primary-foreground"
          >
            Deploy
          </button>
        </div>
      </div>
    </div>
  );
}

function Entry({ label, value }: { label: string; value: unknown }) {
  return (
    <div className="flex gap-2">
      <dt className="w-32 text-muted-foreground">{label}:</dt>
      <dd className="font-medium">{String(value ?? '—')}</dd>
    </div>
  );
}
