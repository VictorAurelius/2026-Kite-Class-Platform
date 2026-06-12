import { describe, it, expect, vi, beforeEach } from 'vitest';
import { INITIAL_WIZARD_STATE, type WizardState } from '../wizard-shared';

// Mock the apiClient + toast BEFORE importing the unit under test.
const putMock = vi.fn();
vi.mock('@/lib/api/client', () => ({
  apiClient: { put: (...args: unknown[]) => putMock(...args) },
}));
const warnMock = vi.fn();
vi.mock('sonner', () => ({ toast: { warning: (...a: unknown[]) => warnMock(...a), success: vi.fn(), error: vi.fn() } }));

import { submitLandingFacts } from '../hooks/useWizardDeploy';

// ---------------------------------------------------------------------------
// GAP-1234 — best-effort PUT of landing facts after deploy.
// ---------------------------------------------------------------------------

function stateWith(partial: Partial<WizardState>): WizardState {
  return { ...INITIAL_WIZARD_STATE, ...partial };
}

describe('submitLandingFacts (GAP-1234 deploy → landing PATCH)', () => {
  beforeEach(() => {
    putMock.mockReset();
    warnMock.mockReset();
    putMock.mockResolvedValue({ data: {} });
  });

  it('PUTs to the tenant landing endpoint with the mapped facts payload', async () => {
    await submitLandingFacts(
      stateWith({
        instanceId: 'tenant-123',
        facts: {
          ...INITIAL_WIZARD_STATE.facts,
          address: '123 Lê Lợi',
          contactPhone: '0901234567',
          tuitions: [{ id: 't1', name: 'IELTS', price: '1500000' }],
        },
      }),
    );
    expect(putMock).toHaveBeenCalledTimes(1);
    expect(putMock).toHaveBeenCalledWith('/api/v1/tenants/tenant-123/landing', {
      address: '123 Lê Lợi',
      contactPhone: '0901234567',
      pricingTiers: [{ name: 'IELTS', price: '1.500.000đ', period: '/tháng' }],
    });
  });

  it('skips the PUT when there is no tenant id', async () => {
    await submitLandingFacts(
      stateWith({ instanceId: null, facts: { ...INITIAL_WIZARD_STATE.facts, address: 'x' } }),
    );
    expect(putMock).not.toHaveBeenCalled();
  });

  it('skips the PUT when no facts were entered', async () => {
    await submitLandingFacts(stateWith({ instanceId: 'tenant-123' }));
    expect(putMock).not.toHaveBeenCalled();
  });

  it('swallows a PUT failure and shows a warning toast (best-effort, never throws)', async () => {
    putMock.mockRejectedValue(new Error('403'));
    await expect(
      submitLandingFacts(
        stateWith({
          instanceId: 'tenant-123',
          facts: { ...INITIAL_WIZARD_STATE.facts, contactEmail: 'a@b.vn' },
        }),
      ),
    ).resolves.toBeUndefined();
    expect(warnMock).toHaveBeenCalledTimes(1);
  });
});
