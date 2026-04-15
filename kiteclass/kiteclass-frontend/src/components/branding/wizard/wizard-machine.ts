/**
 * Pure reducer for the branding wizard FSM.
 *
 * No React, no side effects — just `(state, event) → state`. All tests target this
 * function directly for determinism.
 *
 * @since Wave 3 Sub-PR 3.7
 */

import type {
  BrandInputs,
  StepName,
  WizardContext,
  WizardEvent,
  WizardState,
} from './types';
import { ORDERED_STEPS, REGENERATE_LIMIT_BY_TIER } from './types';

export function initialState(
  tier: WizardContext['tier'],
  instanceId?: number,
): WizardState {
  return {
    name: 'welcome',
    context: {
      tier,
      instanceId,
      inputs: { audiences: [] },
      regenerateCount: 0,
      regenerateLimit: REGENERATE_LIMIT_BY_TIER[tier],
    },
  };
}

function stepIndex(name: StepName): number {
  return ORDERED_STEPS.indexOf(name);
}

function isReadyFor(next: StepName, ctx: WizardContext): boolean {
  const inputs = ctx.inputs;
  switch (next) {
    case 'welcome':
    case 'logo':
      return true;
    case 'audience':
      return !!inputs.segment;
    case 'tone':
      return inputs.audiences.length > 0;
    case 'template':
      return !!inputs.tone;
    case 'preview':
      return !!inputs.templateId;
    default:
      return true;
  }
}

export function reducer(state: WizardState, event: WizardEvent): WizardState {
  const ctx = state.context;

  if (event.type === 'RESET') {
    return initialState(ctx.tier, ctx.instanceId);
  }

  switch (state.name) {
    case 'welcome':
    case 'logo':
    case 'audience':
    case 'tone':
    case 'template':
    case 'preview':
      return handleStepEvent(state, event);
    case 'submitting':
      return handleSubmittingEvent(state, event);
    case 'done':
      return handleDoneEvent(state, event);
    case 'error':
      return handleErrorEvent(state, event);
    default: {
      const _exhaustive: never = state;
      return _exhaustive;
    }
  }
}

function handleStepEvent(state: WizardState, event: WizardEvent): WizardState {
  const ctx = state.context;

  switch (event.type) {
    case 'NEXT': {
      const idx = stepIndex(state.name as StepName);
      const nextName = ORDERED_STEPS[idx + 1];
      if (!nextName) {
        return { name: 'submitting', context: { ...ctx, errorMessage: undefined } };
      }
      if (!isReadyFor(nextName, ctx)) {
        return { name: state.name, context: ctx };
      }
      return { name: nextName, context: ctx } as WizardState;
    }
    case 'BACK': {
      const idx = stepIndex(state.name as StepName);
      const prevName = ORDERED_STEPS[idx - 1];
      if (!prevName) return state;
      return { name: prevName, context: ctx } as WizardState;
    }
    case 'SET_SEGMENT':
      return withInputs(state, { segment: event.segment });
    case 'SET_LOGO_FILENAME':
      return { name: state.name, context: { ...ctx, logoFilename: event.filename } } as WizardState;
    case 'TOGGLE_AUDIENCE':
      return withInputs(state, {
        audiences: ctx.inputs.audiences.includes(event.audience)
          ? ctx.inputs.audiences.filter((a) => a !== event.audience)
          : [...ctx.inputs.audiences, event.audience],
      });
    case 'SET_TONE':
      return withInputs(state, { tone: event.tone });
    case 'SET_TEMPLATE':
      return withInputs(state, { templateId: event.templateId });
    case 'SET_INPUT':
      return withInputs(state, { [event.field]: event.value as never });
    case 'SUBMIT':
      return { name: 'submitting', context: { ...ctx, errorMessage: undefined } };
    case 'REGENERATE':
      if (ctx.regenerateCount >= ctx.regenerateLimit) return state;
      return {
        name: 'submitting',
        context: {
          ...ctx,
          regenerateCount: ctx.regenerateCount + 1,
          errorMessage: undefined,
        },
      };
    default:
      return state;
  }
}

function handleSubmittingEvent(state: WizardState, event: WizardEvent): WizardState {
  if (event.type === 'SUBMIT_OK') {
    return {
      name: 'done',
      context: { ...state.context, instanceId: event.instanceId, errorMessage: undefined },
    };
  }
  if (event.type === 'SUBMIT_FAIL') {
    return { name: 'error', context: { ...state.context, errorMessage: event.message } };
  }
  return state;
}

function handleDoneEvent(state: WizardState, event: WizardEvent): WizardState {
  if (event.type === 'REGENERATE') {
    if (state.context.regenerateCount >= state.context.regenerateLimit) return state;
    return {
      name: 'submitting',
      context: {
        ...state.context,
        regenerateCount: state.context.regenerateCount + 1,
        errorMessage: undefined,
      },
    };
  }
  return state;
}

function handleErrorEvent(state: WizardState, event: WizardEvent): WizardState {
  if (event.type === 'BACK') {
    return { name: 'preview', context: { ...state.context, errorMessage: undefined } };
  }
  if (event.type === 'SUBMIT') {
    return { name: 'submitting', context: { ...state.context, errorMessage: undefined } };
  }
  return state;
}

function withInputs(state: WizardState, patch: Partial<BrandInputs>): WizardState {
  return {
    name: state.name,
    context: { ...state.context, inputs: { ...state.context.inputs, ...patch } },
  } as WizardState;
}
