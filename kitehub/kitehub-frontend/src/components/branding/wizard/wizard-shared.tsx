'use client';

import { useReducer } from 'react';
import { Card } from '@/components/ui/card';

// ---------------------------------------------------------------------------
// WizardState — shared state for the 6-step AI Branding Wizard v2.
//
// NOTE (Wave 32 REWORK Bucket C): defined LOCALLY in this file as a
// transitional measure while Bucket A's wizard-shared lands. Once Bucket A
// merges, the orchestrator + step components import the canonical export
// from there. The shape mirrors the Bucket A contract per the rework plan
// §4 and adds `approvedResources: string[]` to satisfy
// `ai-branding-guidelines.md` §4.2 — per-resource approve state lives in
// the reducer, not local component state.
// ---------------------------------------------------------------------------

export type WizardStep = 1 | 2 | 3 | 4 | 5 | 6;

/**
 * One of the four Step-6 brand resources the user can approve / un-approve
 * individually before deploy.
 */
export type ApprovableResource = 'logo' | 'colors' | 'banner' | 'hero';

export interface WizardState {
  /** Active step (1-indexed). */
  currentStep: WizardStep;
  /** Center name entered in Step 1. */
  tenantName: string;
  /** Slug chosen / validated in Step 1. */
  slug: string;
  /** Logo URL after a successful upload (Step 2). `null` if skipped. */
  logoUrl: string | null;
  /** Whether the user explicitly chose the AI-generated logo path. */
  aiLogo: boolean;
  /** Audience preset selected in Step 3. */
  audience: string | null;
  /** Tone preset selected in Step 4. */
  tone: string | null;
  /** Template ID selected in Step 5. */
  templateId: string | null;
  /** Custom prompt entered in Step 5 (Enterprise Advanced Mode only). */
  customPrompt: string;
  /** Branding job ID returned from the backend — set when generate starts. */
  jobId: string | null;
  /**
   * IDs of resources the user has approved at Step 6. Reducer-controlled
   * per `ai-branding-guidelines.md` §4.2 (per-resource approve before commit).
   */
  approvedResources: ApprovableResource[];
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
  customPrompt: '',
  jobId: null,
  approvedResources: [],
};

// ---------------------------------------------------------------------------
// WizardAction — union of all state mutations.
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
  | { type: 'SET_CUSTOM_PROMPT'; payload: string }
  | { type: 'SET_JOB_ID'; payload: string }
  | { type: 'APPROVE_RESOURCE'; payload: ApprovableResource }
  | { type: 'UNAPPROVE_RESOURCE'; payload: ApprovableResource }
  | { type: 'RESET_APPROVALS' }
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

    case 'SET_CUSTOM_PROMPT':
      return { ...state, customPrompt: action.payload };

    case 'SET_JOB_ID':
      return { ...state, jobId: action.payload };

    case 'APPROVE_RESOURCE':
      if (state.approvedResources.includes(action.payload)) return state;
      return {
        ...state,
        approvedResources: [...state.approvedResources, action.payload],
      };

    case 'UNAPPROVE_RESOURCE':
      return {
        ...state,
        approvedResources: state.approvedResources.filter(
          (r) => r !== action.payload,
        ),
      };

    case 'RESET_APPROVALS':
      return { ...state, approvedResources: [] };

    case 'RESET':
      return { ...INITIAL_WIZARD_STATE };

    default:
      return state;
  }
}

export function useWizardReducer() {
  return useReducer(wizardReducer, INITIAL_WIZARD_STATE);
}

// ---------------------------------------------------------------------------
// WizardCard — consistent card wrapper matching the spec's `.wiz-card` style.
// ---------------------------------------------------------------------------

interface WizardCardProps {
  children: React.ReactNode;
  className?: string;
}

export function WizardCard({ children, className = '' }: WizardCardProps) {
  return (
    <Card className={`p-6 md:p-8 max-w-3xl w-full mx-auto ${className}`}>
      {children}
    </Card>
  );
}

// ---------------------------------------------------------------------------
// WizardStepHeader — eyebrow + title + subtitle matching the spec HTML pattern.
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
      {subtitle && <p className="text-muted-foreground">{subtitle}</p>}
    </div>
  );
}

// ---------------------------------------------------------------------------
// useBrandingTier — local stub matching Bucket D's hook signature.
//
// Bucket D ships the canonical `kitehub-frontend/src/hooks/use-branding-tier.ts`
// hook in parallel. Until that lands we expose a stub here so this bucket's
// imports stay resolved. Once Bucket D merges, callers switch the import to
// `@/hooks/use-branding-tier` (sed-replace; no API surface change).
//
// TODO(GAP-272m): replace with real `useActiveSubscription(instanceId)` call
// from Bucket D once the canonical hook lands.
// ---------------------------------------------------------------------------

export type BrandingTier = 'FREE' | 'PRO' | 'PREMIUM' | 'ENTERPRISE';

export interface BrandingTierInfo {
  tier: BrandingTier;
  /** Per `ai-branding-guidelines.md` §4.3 regenerate quotas. */
  regenerateQuota: number;
  advancedModeEnabled: boolean;
  /** Per `ai-branding-guidelines.md` §2.1 free-form prompt only on Enterprise. */
  canUseCustomPrompt: boolean;
}

export function useBrandingTierStub(initialTier: BrandingTier = 'FREE'): BrandingTierInfo {
  const tierToQuota: Record<BrandingTier, number> = {
    FREE: 3,
    PRO: 10,
    PREMIUM: 30,
    ENTERPRISE: -1, // unlimited
  };
  return {
    tier: initialTier,
    regenerateQuota: tierToQuota[initialTier],
    advancedModeEnabled: initialTier === 'ENTERPRISE',
    canUseCustomPrompt: initialTier === 'ENTERPRISE',
  };
}

// ---------------------------------------------------------------------------
// Step props contracts — exported for cross-bucket consumers.
// ---------------------------------------------------------------------------

export interface TemplateStepProps {
  wizardState: WizardState;
  dispatch: React.Dispatch<WizardAction>;
  /** Override tier for tests / Storybook. Default: FREE. */
  tierOverride?: BrandingTier;
  onNext: () => void;
  onBack: () => void;
}

export interface Step6PreviewProps {
  wizardState: WizardState;
  dispatch: React.Dispatch<WizardAction>;
  /** Brand colours derived from the chosen template + brand state. */
  brandColors?: {
    primary: string;
    secondary: string;
    background: string;
    foreground: string;
  };
  /** Mock-or-real preview URL the iframe loads (data: URI for v1). */
  previewUrl?: string;
  onDeploy?: () => void;
  onBack?: () => void;
}
