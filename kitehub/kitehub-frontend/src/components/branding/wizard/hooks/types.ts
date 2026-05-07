/**
 * Wave 34 Bucket D — shared types for AI Branding wizard hooks.
 *
 * Schema source-of-truth: `documents/01-business/kitehub/ai-branding/api-contract.md`
 * Wave 34 endpoints. Types here describe the WIRE shapes returned by the
 * v1 endpoints; some upstream endpoints (Bucket A's POST /regenerate vs
 * Bucket B's GET /jobs/{id}) currently return slightly different envelopes
 * — see the relevant hook for the adapter handling.
 */

/** Wizard job lifecycle status — superset matching api-contract §Wave 34. */
export type WizardJobStatus =
  | 'INITIALIZING'
  | 'GENERATING'
  | 'REVIEWING'
  | 'DEPLOYED'
  | 'REGENERATING'
  | 'FAILED';

/** Wave 34 wire-level brand colors (sub-GAP-272k). */
export interface WizardBrandColors {
  primary: string;
  secondary: string;
  accent?: string;
  neutral?: string;
  background?: string;
  source?: 'TEMPLATE' | 'AI_ANALYSIS' | 'USER_OVERRIDE';
}

/**
 * `BrandingJobResponse` — wrapper returned by GET `/api/v1/branding/jobs/{jobId}`
 * (Bucket B). Bucket A's POST `/regenerate` returns the full underlying
 * `BrandingJob` entity which is structurally similar but NOT guaranteed to
 * include `brandColors`. The `useRegenerateQuota` hook accepts either shape
 * loosely typed via `Partial<...>` to absorb the inconsistency until the
 * follow-up gap aligns A on B's wrapper.
 */
export interface BrandingJobResponse {
  jobId: string;
  instanceId?: number | string;
  tenantId?: string;
  status: WizardJobStatus;
  regenerateCount?: number;
  brandingVersion?: number;
  createdAt?: string;
  updatedAt?: string;
  brandColors?: WizardBrandColors;
  templateId?: string | null;
  audience?: string | null;
  tone?: string | null;
  previewUrl?: string;
}

export interface SlugAvailabilityResponse {
  available: boolean;
  suggestions: string[];
}

export interface RegenerateQuotaResponse {
  tier: 'FREE' | 'PRO' | 'PREMIUM' | 'ENTERPRISE' | 'BASIC';
  used: number;
  /** -1 sentinel = unlimited (ENTERPRISE per `ai-branding-guidelines.md` §4.3). */
  limit: number;
  /** ISO-8601; null when limit = -1 (unlimited). */
  resetAt: string | null;
}

export interface QualityScoreResponse {
  jobId: string;
  score: number;
  passed: boolean;
  threshold: number;
  subscores: {
    contrast: number;
    brokenLinks: number;
    cssVarsApplied: number;
    visualRegression: number;
    logoPlacement: number;
  };
  issues: Array<{
    severity: 'INFO' | 'WARN' | 'ERROR';
    check: string;
    message: string;
  }>;
  computedAt: string;
}

export type LifecycleEventType =
  | 'state-change'
  | 'regenerate-requested'
  | 'quality-score-computed'
  | 'deploy-completed'
  | 'failed'
  | 'manual-override';

export interface WizardLifecycleEvent {
  id: string;
  ts: string;
  type: LifecycleEventType;
  fromState?: WizardJobStatus;
  toState?: WizardJobStatus;
  actor: { kind: 'user' | 'system' | 'admin'; id: string };
  metadata?: Record<string, unknown>;
}

export interface LifecycleEventsResponse {
  instanceId: number | string;
  events: WizardLifecycleEvent[];
  /** v1 always returns null; reserved for paginated v2. */
  nextCursor: string | null;
}

/** SSE event names emitted by GET `/jobs/{jobId}/deploy-stream` (Bucket A). */
export type DeployStreamEventName =
  | 'log'
  | 'progress'
  | 'state-change'
  | 'complete'
  | 'error'
  | 'heartbeat';

export interface DeployStreamEvent {
  name: DeployStreamEventName;
  data: unknown;
}
