/**
 * Tests for TierSelector (GAP-1435 — BASIC owner downgrade-to-FREE path).
 *
 * Design intent: BE rejects PATCH /downgrade → FREE (SubscriptionService per
 * SUB-01/GAP-1018 — cancel ends a subscription, downgrade only moves between
 * paid tiers). FE must NOT offer FREE as a downgrade target for a paid owner;
 * instead it shows guidance to cancel the subscription.
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@/__tests__/test-utils';
import { TierSelector } from '../TierSelector';

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

describe('TierSelector — GAP-1435 downgrade-to-FREE guidance', () => {
  beforeEach(() => {
    pushMock.mockClear();
  });

  it('paid owner (BASIC): FREE is NOT a selectable downgrade target — shows cancel guidance', () => {
    const onSelect = vi.fn();
    render(<TierSelector currentTier="BASIC" selectedTier={null} onSelect={onSelect} />);

    // FREE shows cancel guidance instead of "Hạ gói" target.
    const guidance = screen.getByTestId('downgrade-to-free-guidance');
    expect(guidance).toBeInTheDocument();
    expect(guidance.textContent).toContain('hủy đăng ký');
  });

  it('paid owner (BASIC): clicking FREE card does NOT trigger onSelect (no 400 generic toast path)', () => {
    const onSelect = vi.fn();
    render(<TierSelector currentTier="BASIC" selectedTier={null} onSelect={onSelect} />);

    // Click the FREE plan card title region — FREE is disabled, must not select.
    // ("Miễn phí" appears as both card title + price label → take the title.)
    const freeTitle = screen.getAllByText('Miễn phí')[0]!;
    fireEvent.click(freeTitle);
    expect(onSelect).not.toHaveBeenCalledWith('FREE');
  });

  it('paid owner (BASIC): cancel guidance button routes to /settings (Danger Zone)', () => {
    const onSelect = vi.fn();
    render(<TierSelector currentTier="BASIC" selectedTier={null} onSelect={onSelect} />);

    const cancelBtn = screen.getByRole('button', { name: /Hủy đăng ký/i });
    fireEvent.click(cancelBtn);
    expect(pushMock).toHaveBeenCalledWith('/settings');
    // Card onClick must not also fire onSelect (stopPropagation).
    expect(onSelect).not.toHaveBeenCalled();
  });

  it('TRIAL/FREE owner (currentTier=FREE): FREE is current (disabled), paid tiers selectable', () => {
    const onSelect = vi.fn();
    render(<TierSelector currentTier="FREE" selectedTier={null} onSelect={onSelect} />);

    // No downgrade-to-FREE guidance when owner is already on FREE.
    expect(screen.queryByTestId('downgrade-to-free-guidance')).not.toBeInTheDocument();

    // BASIC is an upgrade candidate → selectable.
    fireEvent.click(screen.getByText('Cơ bản'));
    expect(onSelect).toHaveBeenCalledWith('BASIC');
  });
});
