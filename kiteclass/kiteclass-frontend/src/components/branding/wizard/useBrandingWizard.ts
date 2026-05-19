'use client';

import { useCallback, useEffect, useReducer, useRef } from 'react';
import { initialState, reducer } from './wizard-machine';
import type { BrandInputs, Tier, WizardEvent } from './types';
import { submitWizard } from '@/lib/branding-wizard-api';

/**
 * React binding for the wizard FSM.
 *
 * Returns the current state + typed dispatchers. The SUBMIT side effect is handled
 * here (the reducer itself stays pure) — success drives SUBMIT_OK, failure drives
 * SUBMIT_FAIL so the reducer owns all transitions.
 *
 * GAP-287: when reducer transitions to `submitting` via `USE_DEFAULTS` (skip path),
 * a useEffect fires the same submitWizard side-effect that explicit Triển khai uses.
 * This keeps the reducer pure (state-only) while ensuring the skip path actually
 * persists inputs + advances state on the backend.
 *
 * @since Wave 3 Sub-PR 3.7
 */
export function useBrandingWizard(tier: Tier, tenantId: string, slug: string) {
  const [state, rawDispatch] = useReducer(reducer, initialState(tier));
  const submissionInFlight = useRef(false);

  const send = useCallback((event: WizardEvent) => {
    rawDispatch(event);
  }, []);

  const runSubmission = useCallback(
    async (inputs: BrandInputs) => {
      if (submissionInFlight.current) return;
      submissionInFlight.current = true;
      try {
        const snapshot = await submitWizard(tenantId, slug, inputs);
        rawDispatch({ type: 'SUBMIT_OK', instanceId: snapshot.id });
      } catch (err) {
        rawDispatch({
          type: 'SUBMIT_FAIL',
          message: err instanceof Error ? err.message : 'submit failed',
        });
      } finally {
        submissionInFlight.current = false;
      }
    },
    [tenantId, slug],
  );

  const submit = useCallback(() => {
    rawDispatch({ type: 'SUBMIT' });
    // Side-effect runs from useEffect below (single code path cho Triển khai +
    // USE_DEFAULTS skip + error-retry).
  }, []);

  // Fire side-effect khi reducer transitions vào `submitting` từ bất kỳ path:
  //   - SUBMIT (Triển khai button) — PreviewStep.onSubmit
  //   - USE_DEFAULTS (Sử dụng mặc định button) — Logo/Audience/Tone/Template
  //   - SUBMIT (retry từ error state)
  //   - REGENERATE (regenerate flow)
  // submissionInFlight ref guards re-entry trong same submitting window.
  useEffect(() => {
    if (state.name === 'submitting' && !submissionInFlight.current) {
      void runSubmission(state.context.inputs);
    }
  }, [state.name, state.context.inputs, runSubmission]);

  return { state, send, submit };
}
