/**
 * Tests for DangerZone (GAP-1436 — cancel-subscription fake success).
 *
 * Bug: handleCancelSubscription skipped the DELETE when subscriptionId was
 * absent (TRIAL/FREE owner) but STILL redirected to /billing?success=cancelled
 * → fake "đã hủy" success. Fix: disable the cancel card + show "chưa có gói để
 * hủy" when there is no active subscription.
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@/__tests__/test-utils';
import { DangerZone } from '../DangerZone';
import type { Instance } from '@/types/instance';

const pushMock = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: pushMock,
    replace: vi.fn(),
    back: vi.fn(),
    forward: vi.fn(),
    refresh: vi.fn(),
    prefetch: vi.fn(),
  }),
}));

const deleteMock = vi.fn((..._args: unknown[]) => Promise.resolve({ data: {} }));
vi.mock('@/lib/api/client', () => ({
  default: {
    delete: (...args: unknown[]) => deleteMock(...args),
    get: vi.fn(() => Promise.resolve({ data: {} })),
  },
}));

function makeInstance(overrides: Partial<Instance>): Instance {
  return {
    id: 1,
    organizationName: 'Trung tâm Anh ngữ Sky Education',
    subdomain: 'sky',
    subscriptionId: null,
    ...overrides,
  } as Instance;
}

describe('DangerZone — GAP-1436 cancel without subscription', () => {
  beforeEach(() => {
    pushMock.mockClear();
    deleteMock.mockClear();
  });

  it('owner WITHOUT active subscription: shows "chưa có gói để hủy", no cancel dialog', () => {
    render(<DangerZone instance={makeInstance({ subscriptionId: null })} />);

    const note = screen.getByTestId('no-subscription-to-cancel');
    expect(note).toBeInTheDocument();
    expect(note.textContent).toContain('Bạn chưa có gói đăng ký để hủy');

    // The "Hủy đăng ký" button in the cancel card is disabled (no fake-success path).
    const cancelButtons = screen.getAllByRole('button', { name: /Hủy đăng ký/i });
    expect(cancelButtons[0]).toBeDisabled();
  });

  it('owner WITH active subscription: cancel card is interactive (dialog trigger enabled)', () => {
    render(<DangerZone instance={makeInstance({ subscriptionId: 'sub-123' })} />);

    // No "chưa có gói để hủy" message when a subscription exists.
    expect(screen.queryByTestId('no-subscription-to-cancel')).not.toBeInTheDocument();

    const cancelButtons = screen.getAllByRole('button', { name: /Hủy đăng ký/i });
    expect(cancelButtons[0]).not.toBeDisabled();
  });
});
