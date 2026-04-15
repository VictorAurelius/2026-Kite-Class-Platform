'use client';

import { SegmentPicker } from '../SegmentPicker';
import type { WizardContext, WizardEvent } from '../types';

interface Props {
  context: WizardContext;
  send: (e: WizardEvent) => void;
}

export function WelcomeStep({ context, send }: Props) {
  const canProceed = !!context.inputs.segment;
  return (
    <div className="space-y-6">
      <header>
        <h2 className="text-2xl font-semibold">Chào mừng đến với Branding Wizard</h2>
        <p className="mt-2 text-muted-foreground">
          6 bước nhanh để tạo bộ branding chuyên nghiệp cho trường của bạn. Trước tiên,
          chọn loại tổ chức để chúng tôi gợi ý template phù hợp.
        </p>
      </header>

      <SegmentPicker
        value={context.inputs.segment}
        onChange={(segment) => send({ type: 'SET_SEGMENT', segment })}
      />

      <div className="flex justify-end">
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
  );
}
