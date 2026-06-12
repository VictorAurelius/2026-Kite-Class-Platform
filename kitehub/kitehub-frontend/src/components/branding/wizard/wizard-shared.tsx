'use client';

/**
 * Wave 32 Bucket A — Wizard shared types, state machine, and primitives.
 *
 * This module is the SINGLE SOURCE OF TRUTH for the Wave 32 AI Branding Wizard v2
 * (Direction C 6-step refactor) per:
 *   - documents/03-planning/waves/wave-2026-05-06-32-ai-branding-wizard-v2.md §3 Bucket A
 *   - documents/03-planning/waves/wave-2026-05-07-32-ai-branding-wizard-v2-rework.md §4
 *   - .claude/rules/ai-branding-guidelines.md §4 (wizard pattern + per-resource approve)
 *
 * What lives here:
 *   1. WizardState + WizardAction types — the FULL state including
 *      `approvedResources: string[]` (rework §4 mandate — Bucket C consumes via
 *      APPROVE_RESOURCE / UNAPPROVE_RESOURCE actions, NOT local useState).
 *   2. wizardReducer — pure reducer covering all transitions.
 *   3. useWizardReducer — convenience hook returning [state, dispatch].
 *   4. WizardCard / WizardStepHeader — common visual primitives matching kit
 *      `.wiz-card` + `.wiz-eyebrow/title/subtitle` patterns.
 *   5. Type stubs for downstream Bucket B/C/D step components — they consume
 *      these props but Bucket A does NOT render their components (anti-cross-bucket
 *      leak per rework §3.2).
 */

import { useReducer } from 'react';
import { Card } from '@/components/ui/card';
import type { GenerationMode } from './GenerationModeSelector';

// ---------------------------------------------------------------------------
// Step / slug types
// ---------------------------------------------------------------------------

/**
 * GAP-1216 / GAP-1212 — output-first reorder (kit v3 §2.5, hội tụ 3 audit 2026-06-11).
 * The wizard collapsed from 7 input steps to 5 OUTPUT-FIRST steps:
 *   1. Welcome + Mode  — tenant name + slug + org-type + TEMPLATE/FULL_AI pick
 *   2. Brand personality — Audience + Tone merged onto one page (cards kept)
 *   3. Assets          — Logo + Portrait merged, optional/skip; Portrait only FULL_AI
 *   4. Tạo & Duyệt     — generate on entry → live preview + quality gate +
 *                        per-resource approve + variant pick (Step6Preview)
 *   5. Triển khai       — EXPLICIT deploy step: SSE lifecycle + FAILED recovery
 *
 * The old standalone "Template" step is REMOVED (kit v3): the template is
 * auto-derived from tone/audience (see `deriveTemplateId`) and edited later in
 * the content editor. Both TEMPLATE and FULL_AI modes therefore walk the same
 * linear 5 steps (no skip). `mode` (GAP-1142) still lives in WizardState and
 * drives the step-3 Portrait branch + FULL_AI banner generation.
 */
export type WizardStep = 1 | 2 | 3 | 4 | 5;

/**
 * Organisation type (GAP-1133) — the SECOND orthogonal axis alongside
 * `audience`. `audience` drives THEME (colours/imagery/voice); `orgType`
 * drives ASSET STRATEGY (how many portraits — GAP-1134) + tier hinting.
 *
 * A solo English teacher and a large English centre share the same theme
 * ("english-center" audience) but differ on org structure → portrait count.
 * Constrained preset (no free text) per `ai-branding-guidelines.md` §2.1.
 */
export type OrgType = 'SOLO_TEACHER' | 'SMALL_CENTER' | 'LARGE_CENTER';

export interface OrgTypeOption {
  id: OrgType;
  emoji: string;
  label: string;
  description: string;
  /**
   * Suggested portrait count (UI hint only — the portrait step still accepts
   * 1..N regardless). Solo → 1 person; centres → multiple teachers/staff.
   */
  portraitHint: number;
}

/** Constrained preset cards for the user-type axis (GAP-1133). */
export const ORG_TYPE_OPTIONS: readonly OrgTypeOption[] = [
  {
    id: 'SOLO_TEACHER',
    emoji: '👩‍🏫',
    label: 'Giáo viên đơn lẻ',
    description: 'Bạn tự dạy, tự quản lý lớp. Trang web xoay quanh cá nhân bạn.',
    portraitHint: 1,
  },
  {
    id: 'SMALL_CENTER',
    emoji: '🏠',
    label: 'Trung tâm nhỏ',
    description: 'Vài giáo viên, một cơ sở. Trang web giới thiệu đội ngũ nhỏ gọn.',
    portraitHint: 3,
  },
  {
    id: 'LARGE_CENTER',
    emoji: '🏢',
    label: 'Trung tâm lớn',
    description: 'Nhiều giáo viên, nhiều cơ sở. Cần giới thiệu đội ngũ đầy đủ.',
    portraitHint: 6,
  },
] as const;

/**
 * Suggested max number of portraits to upload for a given org type
 * (GAP-1134 count hint). Returns a sensible default when orgType is unset.
 */
export function portraitCountHint(orgType: OrgType | null): number {
  const opt = ORG_TYPE_OPTIONS.find((o) => o.id === orgType);
  return opt?.portraitHint ?? 1;
}

/**
 * Kit v3 — the standalone Template step was removed; the template is now
 * auto-derived from the chosen tone (with an audience nudge) so the user reaches
 * the live preview faster. The user can still swap templates later in the content
 * editor (out of wizard scope). Returns a stable `template-*` id from `TemplateGrid`.
 */
export function deriveTemplateId(
  tone: string | null,
  _audience?: string | null,
): string {
  switch (tone) {
    case 'professional':
    case 'luxury':
      return 'template-t1-navy-focus';
    case 'friendly':
      return 'template-t3-coach-card';
    case 'energetic':
      return 'template-t4-result-stripes';
    default:
      return 'template-t1-navy-focus';
  }
}

/**
 * Slug validation status (Step 1):
 *   - default: untouched / cleared
 *   - validating: debounced API call in flight
 *   - conflict: server returned 409 — `conflictSuggestions` populated
 *   - available: server confirmed available — Continue button enables
 */
export type SlugStatus = 'default' | 'validating' | 'conflict' | 'available';

// ---------------------------------------------------------------------------
// Facts — optional "real info shown on the landing" (GAP-1234, kit v3
// `personality.html` "Tùy chọn" disclosure). Progressive-disclosure: collapsed
// by default; AI uses these REAL values for the hero/about/pricing/FAQ instead
// of fabricating numbers ("AI chỉ dùng thông tin thật chị nhập"). Empty is fine
// — the wizard ships the safe default template.
//
// On a successful deploy the non-empty facts are best-effort PATCH'd into the
// tenant's KiteClass landing (`PUT /api/v1/tenants/{tenantId}/landing`) — see
// `buildLandingFactsPayload`. Contact fields map 1:1 to the BE DTO; tuition rows
// map to `pricingTiers` (the BE landing field that actually carries a `price` —
// `programs` has no price field, verified against LandingPage entity).
// ---------------------------------------------------------------------------

/** A single tuition / price row (course name + VND price). */
export interface TuitionRow {
  /** Stable client id for list keying + edit/remove (component-supplied). */
  id: string;
  /** Course / class name, e.g. "Tiếng Anh giao tiếp". */
  name: string;
  /** Raw price digits the user types (e.g. "1500000"); formatted on display. */
  price: string;
}

/** Optional landing facts captured in Step 2 (progressive disclosure). */
export interface WizardFacts {
  /** Center address shown on the landing (BE `address`). */
  address: string;
  /** Contact phone (BE `contactPhone`). */
  contactPhone: string;
  /** Contact email (BE `contactEmail`). */
  contactEmail: string;
  /** Zalo OA link / id (BE `zaloUrl`). */
  zaloUrl: string;
  /** Simple tuition rows → BE `pricingTiers`. */
  tuitions: TuitionRow[];
}

/** Scalar (non-tuition) fact keys — used by the SET_FACT action. */
export type WizardFactKey = 'address' | 'contactPhone' | 'contactEmail' | 'zaloUrl';

export const INITIAL_WIZARD_FACTS: WizardFacts = {
  address: '',
  contactPhone: '',
  contactEmail: '',
  zaloUrl: '',
  tuitions: [],
};

// ---------------------------------------------------------------------------
// WizardState — shared state for the 6-step AI Branding Wizard v2
// ---------------------------------------------------------------------------

export interface WizardState {
  /** Active step (1-indexed). */
  currentStep: WizardStep;
  /** Center name entered in Step 1 (display-only — slug is the routing key). */
  tenantName: string;
  /** Slug chosen/validated in Step 1 (kebab-case). */
  slug: string;
  /** Validation lifecycle for `slug` (Step 1). */
  slugStatus: SlugStatus;
  /** Server-suggested alternative slugs when `slugStatus === 'conflict'`. */
  conflictSuggestions: string[];
  /**
   * Organisation type chosen in Step 1 (GAP-1133). Orthogonal to `audience` —
   * drives portrait-count strategy (GAP-1134) + tier hinting, NOT theme.
   * `null` until the user picks a card.
   */
  orgType: OrgType | null;
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
  /** Audience preset selected in Step 2 (Brand personality). */
  audience: string | null;
  /** Tone preset selected in Step 2 (Brand personality). */
  tone: string | null;
  /**
   * Generation mode (GAP-1142 / GAP-1216) picked in Step 1. TEMPLATE = free
   * template route (Step 4 visible). FULL_AI = AI-generated route (Step 4
   * Template skipped; Portrait sub-section shown in Step 3). Tier-gating is
   * enforced by the selector + backend; this is the user's intent.
   */
  mode: GenerationMode;
  /** Template ID selected in Step 4 (TEMPLATE route only). */
  templateId: string | null;
  /** Branding job ID returned from the backend — set when generate starts (Step 5→6). */
  jobId: string | null;
  /**
   * Real instance ID (= tenant claim) returned alongside the branding job.
   * Used for `/instances/{instanceId}/lifecycle/events` polling — MUST NOT be
   * the jobId (GAP-1105: deploy "Tiến trình" panel stayed stuck because the FE
   * polled lifecycle/events with jobId → 0 events).
   */
  instanceId: string | null;
  /**
   * Per-resource approval flags (Step 6) — implements `ai-branding-guidelines.md`
   * §4.2 "User approve từng resource (logo, colors, banner, hero) riêng lẻ".
   *
   * MUST be persisted in WizardState (NOT local component useState) — Bucket C
   * `Step6Preview`/`ResourceToggle` dispatch APPROVE_RESOURCE/UNAPPROVE_RESOURCE
   * to mutate this array.
   */
  approvedResources: string[];
  /**
   * Optional landing facts (GAP-1234) entered via the Step-2 progressive
   * disclosure. Best-effort PATCH'd into the tenant landing after deploy.
   */
  facts: WizardFacts;
}

export const INITIAL_WIZARD_STATE: WizardState = {
  currentStep: 1,
  tenantName: '',
  slug: '',
  slugStatus: 'default',
  conflictSuggestions: [],
  // GAP-1231 — org-type selector dropped from the Step-1 UI per kit v3 (the kit
  // has no org-type card). The field stays in state because the generate request
  // still carries it; default SMALL_CENTER is the safe centre-shaped value.
  orgType: 'SMALL_CENTER',
  logoUrl: null,
  aiLogo: false,
  audience: null,
  tone: null,
  mode: 'TEMPLATE',
  templateId: null,
  jobId: null,
  instanceId: null,
  approvedResources: [],
  facts: INITIAL_WIZARD_FACTS,
};

// ---------------------------------------------------------------------------
// WizardAction — union of all state mutations
// ---------------------------------------------------------------------------

export type WizardAction =
  | { type: 'NEXT_STEP' }
  | { type: 'PREV_STEP' }
  | { type: 'GO_TO_STEP'; step: WizardStep }
  | { type: 'SET_TENANT_NAME'; tenantName: string }
  | { type: 'SET_SLUG'; slug: string }
  | { type: 'SET_SLUG_STATUS'; status: SlugStatus; suggestions?: string[] }
  | { type: 'SET_ORG_TYPE'; orgType: OrgType }
  | { type: 'SET_LOGO'; url: string; aiLogo: boolean }
  | { type: 'CLEAR_LOGO' }
  | { type: 'SET_AUDIENCE'; audience: string }
  | { type: 'SET_TONE'; tone: string }
  | { type: 'SET_MODE'; mode: GenerationMode }
  | { type: 'SET_TEMPLATE'; templateId: string; jobId: string }
  | { type: 'SET_JOB_ID'; jobId: string; instanceId?: string }
  | { type: 'APPROVE_RESOURCE'; resource: string }
  | { type: 'UNAPPROVE_RESOURCE'; resource: string }
  | { type: 'RESET_APPROVALS' }
  // Facts (GAP-1234) — Step-2 progressive disclosure.
  | { type: 'SET_FACT'; key: WizardFactKey; value: string }
  | { type: 'ADD_TUITION'; id: string }
  | { type: 'REMOVE_TUITION'; id: string }
  | { type: 'SET_TUITION'; id: string; field: 'name' | 'price'; value: string }
  | { type: 'RESET' };

// ---------------------------------------------------------------------------
// wizardReducer — pure transition function
// ---------------------------------------------------------------------------

export function wizardReducer(state: WizardState, action: WizardAction): WizardState {
  switch (action.type) {
    case 'NEXT_STEP': {
      const s = state.currentStep;
      if (s >= 5) return state;
      // Kit v3 — Template step removed; both modes walk the linear 5 steps.
      return { ...state, currentStep: (s + 1) as WizardStep };
    }

    case 'PREV_STEP': {
      const s = state.currentStep;
      if (s <= 1) return state;
      // Kit v3 — Template step removed; both modes walk the linear 5 steps.
      return { ...state, currentStep: (s - 1) as WizardStep };
    }

    case 'GO_TO_STEP':
      return { ...state, currentStep: action.step };

    case 'SET_TENANT_NAME':
      return { ...state, tenantName: action.tenantName };

    case 'SET_SLUG':
      // New slug input invalidates previous validation result
      return {
        ...state,
        slug: action.slug,
        slugStatus: 'default',
        conflictSuggestions: [],
      };

    case 'SET_SLUG_STATUS':
      return {
        ...state,
        slugStatus: action.status,
        conflictSuggestions: action.suggestions ?? [],
      };

    case 'SET_ORG_TYPE':
      return { ...state, orgType: action.orgType };

    case 'SET_LOGO':
      return {
        ...state,
        logoUrl: action.url,
        aiLogo: action.aiLogo,
      };

    case 'CLEAR_LOGO':
      return { ...state, logoUrl: null, aiLogo: false };

    case 'SET_AUDIENCE':
      return { ...state, audience: action.audience };

    case 'SET_TONE':
      return { ...state, tone: action.tone };

    case 'SET_MODE':
      return { ...state, mode: action.mode };

    case 'SET_TEMPLATE':
      return {
        ...state,
        templateId: action.templateId,
        // Preserve an existing jobId when the caller passes the legacy empty
        // sentinel ('' from TemplateStep). A real jobId is set via SET_JOB_ID
        // once Step 6 creates the branding job (GAP-1021). Only overwrite when a
        // non-empty jobId is explicitly provided.
        jobId: action.jobId && action.jobId !== '' ? action.jobId : state.jobId,
        // New template selection invalidates previous approvals
        approvedResources: [],
      };

    case 'SET_JOB_ID':
      return {
        ...state,
        jobId: action.jobId,
        instanceId: action.instanceId ?? state.instanceId,
      };

    case 'APPROVE_RESOURCE': {
      if (state.approvedResources.includes(action.resource)) return state;
      return {
        ...state,
        approvedResources: [...state.approvedResources, action.resource],
      };
    }

    case 'UNAPPROVE_RESOURCE':
      return {
        ...state,
        approvedResources: state.approvedResources.filter((r) => r !== action.resource),
      };

    case 'RESET_APPROVALS':
      return { ...state, approvedResources: [] };

    case 'SET_FACT':
      return {
        ...state,
        facts: { ...state.facts, [action.key]: action.value },
      };

    case 'ADD_TUITION':
      return {
        ...state,
        facts: {
          ...state.facts,
          tuitions: [
            ...state.facts.tuitions,
            { id: action.id, name: '', price: '' },
          ],
        },
      };

    case 'REMOVE_TUITION':
      return {
        ...state,
        facts: {
          ...state.facts,
          tuitions: state.facts.tuitions.filter((t) => t.id !== action.id),
        },
      };

    case 'SET_TUITION':
      return {
        ...state,
        facts: {
          ...state.facts,
          tuitions: state.facts.tuitions.map((t) =>
            t.id === action.id ? { ...t, [action.field]: action.value } : t,
          ),
        },
      };

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
    <Card className={`p-6 md:p-8 max-w-2xl w-full mx-auto ${className}`}>
      {children}
    </Card>
  );
}

// ---------------------------------------------------------------------------
// WizardStepHeader — eyebrow + title + subtitle matching the kit pattern
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
// Type stubs for downstream buckets (Bucket B/C/D)
//
// Bucket A exports ONLY the prop contracts so wizard/page.tsx can describe
// the orchestrator's API surface without rendering placeholder components.
// Per rework §3.2 — anti-cross-bucket scope leak — Bucket A MUST NOT ship
// `AudienceStep` / `ToneStep` / `TemplateStep` / `Step6Preview` components.
// ---------------------------------------------------------------------------

export interface AudienceStepProps {
  /** Wizard state (read-only for the step component). */
  wizardState: WizardState;
  /** Dispatch — step components dispatch actions back to the orchestrator. */
  dispatch: React.Dispatch<WizardAction>;
  /** Advance handler — the orchestrator decides what NEXT_STEP means. */
  onNext: () => void;
  /** Back handler. */
  onBack: () => void;
}

export interface ToneStepProps {
  wizardState: WizardState;
  dispatch: React.Dispatch<WizardAction>;
  onNext: () => void;
  onBack: () => void;
}

export interface TemplateStepProps {
  wizardState: WizardState;
  dispatch: React.Dispatch<WizardAction>;
  onNext: () => void;
  onBack: () => void;
}

export interface Step6PreviewProps {
  wizardState: WizardState;
  dispatch: React.Dispatch<WizardAction>;
  onBack: () => void;
  /** Called when the user clicks "Triển khai" with all required resources approved. */
  onDeploy: () => void;
}

/** Props for the Portrait upload step (GAP-1134). */
export interface PortraitStepProps {
  wizardState: WizardState;
  dispatch: React.Dispatch<WizardAction>;
  /** Tenant instance — required for the asset upload/list endpoint path. */
  instanceId: string;
  onNext?: () => void;
  onBack?: () => void;
  /** GAP-1216 — embedded inside the merged Assets step (3): drop own footer. */
  embedded?: boolean;
}
