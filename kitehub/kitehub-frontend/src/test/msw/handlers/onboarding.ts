/**
 * MSW handlers for Onboarding Progress endpoints (Wave 78 Bucket 0 Foundation).
 *
 * Schema: `documents/01-business/kitehub/onboarding/api-contract.md`
 *
 * Cross-layer foundation per `.claude/rules/contract-first-for-cross-layer.md`:
 * Bucket B (GAP-538) FE onboarding checklist component will consume these
 * handlers in unit tests before BE module lands.
 *
 * Endpoints covered:
 *   - GET /api/v1/onboarding-progress (fetch current tenant checklist state)
 *   - PUT /api/v1/onboarding-progress (update step completion)
 *
 * Per-test overrides via `server.use(http.X(...))` in individual specs.
 *
 * @author KiteHub Team
 * @since Wave 78 Bucket 0
 */

import { http, HttpResponse } from 'msw';
import type { HttpHandler } from 'msw';

const STEP_IDS = [
  'PROFILE_SETUP',
  'INVITE_TEAM',
  'IMPORT_DATA',
  'CREATE_FIRST_CLASS',
  'EXPLORE_FEATURES',
] as const;

type StepId = (typeof STEP_IDS)[number];

interface StepState {
  stepId: StepId;
  completed: boolean;
  completedAt: string | null;
}

// In-memory state for test session — handlers reset between tests via server.resetHandlers().
const defaultSteps = (): StepState[] =>
  STEP_IDS.map((id) => ({ stepId: id, completed: false, completedAt: null }));

let stepState: StepState[] = defaultSteps();
const TENANT_ID = 'tenant-test-uuid';

function computeResponse() {
  const completedSteps = stepState.filter((s) => s.completed).length;
  const totalSteps = stepState.length;
  return {
    tenantId: TENANT_ID,
    completionPercent: Math.round((completedSteps / totalSteps) * 100),
    totalSteps,
    completedSteps,
    lastUpdatedAt: '2026-05-14T08:30:00Z',
    steps: stepState,
  };
}

interface OnboardingUpdatePayload {
  stepId?: string;
  completed?: boolean;
}

export const onboardingHandlers: HttpHandler[] = [
  // ---------------------------------------------------------------
  // GET /api/v1/onboarding-progress
  // ---------------------------------------------------------------
  http.get('*/api/v1/onboarding-progress', ({ request }) => {
    const auth = request.headers.get('Authorization');
    if (!auth) {
      return HttpResponse.json(
        { error: 'UNAUTHENTICATED', message: 'Bearer token required' },
        { status: 401 }
      );
    }
    // Reset to default on first call simulation — real BE lazy-inits row.
    return HttpResponse.json(computeResponse(), { status: 200 });
  }),

  // ---------------------------------------------------------------
  // PUT /api/v1/onboarding-progress
  // ---------------------------------------------------------------
  http.put('*/api/v1/onboarding-progress', async ({ request }) => {
    const auth = request.headers.get('Authorization');
    if (!auth) {
      return HttpResponse.json(
        { error: 'UNAUTHENTICATED', message: 'Bearer token required' },
        { status: 401 }
      );
    }

    let body: OnboardingUpdatePayload;
    try {
      body = (await request.json()) as OnboardingUpdatePayload;
    } catch {
      return HttpResponse.json(
        { error: 'ONBOARDING_INVALID_PAYLOAD', message: 'Malformed JSON' },
        { status: 400 }
      );
    }

    if (!body.stepId || !(STEP_IDS as readonly string[]).includes(body.stepId)) {
      return HttpResponse.json(
        {
          error: 'ONBOARDING_INVALID_STEP_ID',
          message: `stepId must be one of: ${STEP_IDS.join(', ')}`,
          field: 'stepId',
        },
        { status: 400 }
      );
    }

    if (typeof body.completed !== 'boolean') {
      return HttpResponse.json(
        {
          error: 'ONBOARDING_INVALID_PAYLOAD',
          message: 'completed must be boolean',
          field: 'completed',
        },
        { status: 400 }
      );
    }

    const step = stepState.find((s) => s.stepId === body.stepId);
    if (step) {
      step.completed = body.completed;
      step.completedAt = body.completed ? '2026-05-14T09:00:00Z' : null;
    }

    return HttpResponse.json(computeResponse(), { status: 200 });
  }),
];

/**
 * Test helper — reset in-memory step state between specs.
 * Call from beforeEach() if your test mutates state.
 */
export function resetOnboardingHandlerState(): void {
  stepState = defaultSteps();
}
