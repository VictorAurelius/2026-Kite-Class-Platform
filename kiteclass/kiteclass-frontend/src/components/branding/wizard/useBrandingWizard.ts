'use client';

import { useCallback, useReducer } from 'react';
import { initialState, reducer } from './wizard-machine';
import type { Tier, WizardEvent } from './types';
import { submitWizard } from '@/lib/branding-wizard-api';

/**
 * React binding for the wizard FSM.
 *
 * Returns the current state + typed dispatchers. The SUBMIT side effect is handled
 * here (the reducer itself stays pure) — success drives SUBMIT_OK, failure drives
 * SUBMIT_FAIL so the reducer owns all transitions.
 *
 * @since Wave 3 Sub-PR 3.7
 */
export function useBrandingWizard(tier: Tier, tenantId: string, slug: string) {
  const [state, rawDispatch] = useReducer(reducer, initialState(tier));

  const send = useCallback((event: WizardEvent) => {
    rawDispatch(event);
  }, []);

  const submit = useCallback(async () => {
    rawDispatch({ type: 'SUBMIT' });
    try {
      const snapshot = await submitWizard(tenantId, slug, state.context.inputs);
      rawDispatch({ type: 'SUBMIT_OK', instanceId: snapshot.id });
    } catch (err) {
      rawDispatch({
        type: 'SUBMIT_FAIL',
        message: err instanceof Error ? err.message : 'submit failed',
      });
    }
  }, [tenantId, slug, state.context.inputs]);

  return { state, send, submit };
}
