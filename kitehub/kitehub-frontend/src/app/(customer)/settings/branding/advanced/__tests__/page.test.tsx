import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import type { PricingTier } from '@/types/subscription';

// Mock useRouter — Settings page uses router.push for breadcrumb
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

// Mock auth-store — Settings page reads `user.id` to look up tier
vi.mock('@/stores/auth-store', () => ({
  useAuthStore: <T,>(selector: (s: { user: { id: string } | null }) => T) =>
    selector({ user: { id: 'user-test-001' } }),
}));

// Mock the tier hook — drives tier-gating
const mockUseBrandingTier = vi.fn();
vi.mock('@/hooks/use-branding-tier', () => ({
  useBrandingTier: (id: string | undefined) => mockUseBrandingTier(id),
}));

// Mock owner-instances lookup (React Query hook) — GAP-1091b resolves instanceId
// for useBrandingTier from owner's instances. Without this the page's useOwnerInstances
// calls useQuery with no QueryClientProvider → "No QueryClient set".
vi.mock('@/hooks/use-instances', () => ({
  useOwnerInstances: () => ({ data: [{ id: 'instance-test-001' }] }),
}));

import BrandingAdvancedModePage from '../page';

function tierResult(tier: PricingTier) {
  return {
    tier,
    regenerateQuota: tier === 'ENTERPRISE' ? -1 : 3,
    advancedModeEnabled: tier === 'ENTERPRISE',
    canUseCustomPrompt: tier === 'ENTERPRISE',
    isLoading: false,
  };
}

describe('Settings → Branding → Advanced Mode page', () => {
  beforeEach(() => {
    mockUseBrandingTier.mockReset();
    // Reset localStorage between tests so toggle state doesn't leak
    if (typeof window !== 'undefined') {
      window.localStorage.clear();
    }
  });

  afterEach(() => {
    if (typeof window !== 'undefined') {
      window.localStorage.clear();
    }
  });

  it('hides toggle + shows tier-required notice when tier !== ENTERPRISE', () => {
    mockUseBrandingTier.mockReturnValue(tierResult('FREE'));
    render(<BrandingAdvancedModePage />);
    expect(
      screen.getByTestId('advanced-mode-tier-required-notice')
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId('advanced-mode-toggle-card')
    ).not.toBeInTheDocument();
    expect(screen.queryByTestId('advanced-mode-toggle')).not.toBeInTheDocument();
  });

  it('shows toggle when tier === ENTERPRISE', () => {
    mockUseBrandingTier.mockReturnValue(tierResult('ENTERPRISE'));
    render(<BrandingAdvancedModePage />);
    expect(
      screen.queryByTestId('advanced-mode-tier-required-notice')
    ).not.toBeInTheDocument();
    expect(
      screen.getByTestId('advanced-mode-toggle-card')
    ).toBeInTheDocument();
    expect(screen.getByTestId('advanced-mode-toggle')).toBeInTheDocument();
    expect(screen.getByTestId('advanced-mode-status-text')).toHaveTextContent(
      'ĐANG TẮT'
    );
  });

  it('toggling ON opens disclaimer modal (does not toggle yet)', () => {
    mockUseBrandingTier.mockReturnValue(tierResult('ENTERPRISE'));
    render(<BrandingAdvancedModePage />);
    const toggle = screen.getByTestId('advanced-mode-toggle');
    fireEvent.click(toggle);
    // Modal opened
    expect(
      screen.getByTestId('advanced-mode-disclaimer-modal')
    ).toBeInTheDocument();
    // Status text still TẮT — toggle not yet committed
    expect(screen.getByTestId('advanced-mode-status-text')).toHaveTextContent(
      'ĐANG TẮT'
    );
  });

  it('confirming disclaimer flips toggle to ON and persists to localStorage', () => {
    mockUseBrandingTier.mockReturnValue(tierResult('ENTERPRISE'));
    render(<BrandingAdvancedModePage />);
    fireEvent.click(screen.getByTestId('advanced-mode-toggle'));
    // Tick + confirm
    fireEvent.click(screen.getByTestId('advanced-mode-disclaimer-checkbox'));
    fireEvent.click(screen.getByTestId('advanced-mode-disclaimer-confirm-button'));
    expect(screen.getByTestId('advanced-mode-status-text')).toHaveTextContent(
      'ĐANG BẬT'
    );
    expect(window.localStorage.getItem('kitehub.branding.advanced-mode')).toBe('true');
  });
});
