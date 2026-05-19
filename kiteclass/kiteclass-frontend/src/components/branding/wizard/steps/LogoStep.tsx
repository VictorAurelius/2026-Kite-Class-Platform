'use client';

import type { ChangeEvent } from 'react';
import type { WizardContext, WizardEvent } from '../types';
import { UseDefaultsButton } from '../UseDefaultsButton';

interface Props {
  context: WizardContext;
  send: (e: WizardEvent) => void;
}

/**
 * Optional logo upload. Tenant can skip — AI can generate from scratch.
 * Upload itself is handled by the existing storage module; here we just capture
 * the filename into wizard context so downstream saga knows what's available.
 */
export function LogoStep({ context, send }: Props) {
  const onChange = (e: ChangeEvent<HTMLInputElement>) => {
    send({ type: 'SET_LOGO_FILENAME', filename: e.target.files?.[0]?.name });
  };

  return (
    <div className="space-y-6">
      <header>
        <h2 className="text-2xl font-semibold">Upload logo (tùy chọn)</h2>
        <p className="mt-2 text-muted-foreground">
          Kéo-thả file SVG/PNG hoặc bỏ qua bước này — AI sẽ tạo logo mới từ gợi ý của bạn.
        </p>
      </header>

      <label className="flex cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed p-8 hover:bg-muted/30">
        <input type="file" accept="image/svg+xml,image/png" className="sr-only" onChange={onChange} />
        <div className="text-3xl">📁</div>
        <div className="font-medium">
          {context.logoFilename ?? 'Chọn file logo'}
        </div>
        <div className="text-xs text-muted-foreground">SVG hoặc PNG, tối đa 2 MB</div>
      </label>

      <div className="flex flex-wrap items-center justify-between gap-2">
        <button type="button" onClick={() => send({ type: 'BACK' })} className="text-sm text-muted-foreground">
          ← Quay lại
        </button>
        <div className="flex flex-wrap gap-2">
          <UseDefaultsButton send={send} />
          <button
            type="button"
            onClick={() => send({ type: 'NEXT' })}
            className="rounded-md bg-primary px-4 py-2 text-primary-foreground"
          >
            Tiếp tục →
          </button>
        </div>
      </div>
    </div>
  );
}
