'use client';

import { useReducer } from 'react';
import { Card } from '@/components/ui/card';

// ---------------------------------------------------------------------------
// WizardState — shared state for the 6-step AI Branding Wizard v2
// ---------------------------------------------------------------------------

export type WizardStep = 1 | 2 | 3 | 4 | 5 | 6;

export interface WizardState {
  /** Active step (1-indexed). */
  currentStep: WizardStep;
  /** Center name entered in Step 1. */
  tenantName: string;
  /** Slug chosen/validated in Step 1. */
  slug: string;
  /**
   * Logo URL after a successful upload (Step 2).
   * `null` means the user skipped or hasn't uploaded yet.
   */
  logoUrl: string | null;
  /**
   * Whether the user explicitly chose the "AI-generated logo" path.
   * When true the upload drop-zone is hidden and FULL_AI classification
   * is used for the logo resource per ai-branding-guidelines.md §1.
   */
  aiLogo: boolean;
  /** Audience preset selected in Step 3 (e.g. "children", "adults", "mixed"). */
  audience: string | null;
  /** Tone preset selected in Step 4 (e.g. "professional", "friendly", "energetic"). */
  tone: string | null;
  /** Template ID selected in Step 5. */
  templateId: string | null;
  /** Branding job ID returned from the backend — set when generate starts (Step 5→6). */
  jobId: string | null;
}

export const INITIAL_WIZARD_STATE: WizardState = {
  currentStep: 1,
  tenantName: '',
  slug: '',
  logoUrl: null,
  aiLogo: false,
  audience: null,
  tone: null,
  templateId: null,
  jobId: null,
};

// ---------------------------------------------------------------------------
// WizardAction — union of all state mutations
// ---------------------------------------------------------------------------

export type WizardAction =
  | { type: 'NEXT_STEP' }
  | { type: 'PREV_STEP' }
  | { type: 'GO_TO_STEP'; payload: WizardStep }
  | { type: 'SET_TENANT_NAME'; payload: string }
  | { type: 'SET_SLUG'; payload: string }
  | { type: 'SET_LOGO_URL'; payload: string | null }
  | { type: 'SET_AI_LOGO'; payload: boolean }
  | { type: 'SET_AUDIENCE'; payload: string }
  | { type: 'SET_TONE'; payload: string }
  | { type: 'SET_TEMPLATE_ID'; payload: string }
  | { type: 'SET_JOB_ID'; payload: string }
  | { type: 'RESET' };

// ---------------------------------------------------------------------------
// wizardReducer
// ---------------------------------------------------------------------------

export function wizardReducer(state: WizardState, action: WizardAction): WizardState {
  switch (action.type) {
    case 'NEXT_STEP':
      if (state.currentStep >= 6) return state;
      return { ...state, currentStep: (state.currentStep + 1) as WizardStep };

    case 'PREV_STEP':
      if (state.currentStep <= 1) return state;
      return { ...state, currentStep: (state.currentStep - 1) as WizardStep };

    case 'GO_TO_STEP':
      return { ...state, currentStep: action.payload };

    case 'SET_TENANT_NAME':
      return { ...state, tenantName: action.payload };

    case 'SET_SLUG':
      return { ...state, slug: action.payload };

    case 'SET_LOGO_URL':
      return { ...state, logoUrl: action.payload };

    case 'SET_AI_LOGO':
      return { ...state, aiLogo: action.payload };

    case 'SET_AUDIENCE':
      return { ...state, audience: action.payload };

    case 'SET_TONE':
      return { ...state, tone: action.payload };

    case 'SET_TEMPLATE_ID':
      return { ...state, templateId: action.payload };

    case 'SET_JOB_ID':
      return { ...state, jobId: action.payload };

    case 'RESET':
      return { ...INITIAL_WIZARD_STATE };

    default:
      return state;
  }
}

// ---------------------------------------------------------------------------
// useWizardReducer — convenience hook for consumers
// ---------------------------------------------------------------------------

export function useWizardReducer() {
  return useReducer(wizardReducer, INITIAL_WIZARD_STATE);
}

// ---------------------------------------------------------------------------
// WizardCard — consistent card wrapper matching the spec's `.wiz-card` style
// ---------------------------------------------------------------------------

interface WizardCardProps {
  children: React.ReactNode;
  className?: string;
}

export function WizardCard({ children, className = '' }: WizardCardProps) {
  return (
    <Card
      className={`p-6 md:p-8 max-w-2xl w-full mx-auto ${className}`}
    >
      {children}
    </Card>
  );
}

// ---------------------------------------------------------------------------
// WizardStepHeader — eyebrow + title + subtitle matching the spec HTML pattern
// ---------------------------------------------------------------------------

interface WizardStepHeaderProps {
  eyebrow: string;
  title: string;
  subtitle?: string;
}

export function WizardStepHeader({ eyebrow, title, subtitle }: WizardStepHeaderProps) {
  return (
    <div className="mb-6">
      <p className="text-sm font-semibold text-primary uppercase tracking-wide mb-1">
        {eyebrow}
      </p>
      <h1 className="text-2xl font-bold text-foreground mb-2">{title}</h1>
      {subtitle && (
        <p className="text-muted-foreground">{subtitle}</p>
      )}
    </div>
  );
}
