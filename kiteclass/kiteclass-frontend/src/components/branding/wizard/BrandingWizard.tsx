'use client';

import { useBrandingWizard } from './useBrandingWizard';
import { WizardProgress } from './WizardProgress';
import { WelcomeStep } from './steps/WelcomeStep';
import { LogoStep } from './steps/LogoStep';
import { AudienceStep } from './steps/AudienceStep';
import { ToneStep } from './steps/ToneStep';
import { TemplateStep } from './steps/TemplateStep';
import { PreviewStep } from './steps/PreviewStep';
import type { StepName, Tier } from './types';

interface Props {
  tier: Tier;
  tenantId: string;
  slug: string;
}

export function BrandingWizard({ tier, tenantId, slug }: Props) {
  const { state, send, submit } = useBrandingWizard(tier, tenantId, slug);
  const { context } = state;

  return (
    <div className="mx-auto max-w-3xl space-y-8 p-6">
      <WizardProgress current={stepOrCurrent(state.name)} />

      {state.name === 'welcome' && <WelcomeStep context={context} send={send} />}
      {state.name === 'logo' && <LogoStep context={context} send={send} />}
      {state.name === 'audience' && <AudienceStep context={context} send={send} />}
      {state.name === 'tone' && <ToneStep context={context} send={send} />}
      {state.name === 'template' && <TemplateStep context={context} send={send} />}
      {state.name === 'preview' && (
        <PreviewStep context={context} send={send} onSubmit={submit} />
      )}
      {state.name === 'submitting' && (
        <div className="rounded-xl border p-10 text-center">
          <div className="text-3xl">⏳</div>
          <p className="mt-2 text-muted-foreground">Đang gửi tới pipeline AI…</p>
        </div>
      )}
      {state.name === 'done' && (
        <div className="rounded-xl border border-primary/40 bg-primary/5 p-10 text-center">
          <div className="text-3xl">✅</div>
          <p className="mt-2 font-medium">Đã gửi thành công!</p>
          <p className="text-sm text-muted-foreground">
            Instance #{context.instanceId} đang được provision. Bạn sẽ nhận email khi DEPLOYED.
          </p>
        </div>
      )}
      {state.name === 'error' && (
        <div
          role="alert"
          className="rounded-xl border border-destructive/40 bg-destructive/5 p-6"
        >
          <div className="font-medium text-destructive">Deploy thất bại</div>
          <p className="mt-1 text-sm text-muted-foreground">
            {context.errorMessage ?? 'Unknown error'}
          </p>
          <div className="mt-3 flex gap-2">
            <button
              type="button"
              onClick={() => send({ type: 'BACK' })}
              className="text-sm text-muted-foreground"
            >
              ← Về preview
            </button>
            <button
              type="button"
              onClick={submit}
              className="rounded-md bg-primary px-4 py-2 text-primary-foreground"
            >
              Thử lại
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

function stepOrCurrent(name: string): StepName {
  switch (name) {
    case 'welcome':
    case 'logo':
    case 'audience':
    case 'tone':
    case 'template':
    case 'preview':
      return name;
    default:
      return 'preview';
  }
}
