'use client';

import type { Audience, WizardContext, WizardEvent } from '../types';
import { UseDefaultsButton } from '../UseDefaultsButton';

interface Props {
  context: WizardContext;
  send: (e: WizardEvent) => void;
}

const OPTIONS: { id: Audience; label: string; icon: string }[] = [
  { id: 'students', label: 'Học sinh / Sinh viên', icon: '🧑‍🎓' },
  { id: 'parents', label: 'Phụ huynh', icon: '👨‍👩‍👧' },
  { id: 'teachers', label: 'Giáo viên', icon: '👩‍🏫' },
  { id: 'staff', label: 'Nhân viên / Quản trị', icon: '💼' },
];

export function AudienceStep({ context, send }: Props) {
  const selected = context.inputs.audiences;
  const canProceed = selected.length > 0;

  return (
    <div className="space-y-6">
      <header>
        <h2 className="text-2xl font-semibold">Đối tượng chính</h2>
        <p className="mt-2 text-muted-foreground">
          Chọn nhóm người xem branding nhiều nhất (có thể chọn nhiều).
        </p>
      </header>

      <div className="grid gap-3 md:grid-cols-2" role="group" aria-label="Đối tượng">
        {OPTIONS.map((o) => {
          const checked = selected.includes(o.id);
          return (
            <button
              key={o.id}
              type="button"
              role="checkbox"
              aria-checked={checked}
              onClick={() => send({ type: 'TOGGLE_AUDIENCE', audience: o.id })}
              className={`rounded-xl border p-4 text-left transition ${
                checked
                  ? 'border-primary bg-primary/5 ring-2 ring-primary/30'
                  : 'border-border hover:border-primary/50 hover:bg-muted/50'
              }`}
            >
              <div className="text-2xl">{o.icon}</div>
              <div className="mt-2 font-medium">{o.label}</div>
            </button>
          );
        })}
      </div>

      <div className="flex flex-wrap items-center justify-between gap-2">
        <button type="button" onClick={() => send({ type: 'BACK' })} className="text-sm text-muted-foreground">
          ← Quay lại
        </button>
        <div className="flex flex-wrap gap-2">
          <UseDefaultsButton send={send} />
          <button
            type="button"
            disabled={!canProceed}
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
