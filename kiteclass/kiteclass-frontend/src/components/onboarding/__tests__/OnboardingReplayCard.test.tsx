/**
 * Tests for OnboardingReplayCard (settings replay CTA).
 *
 * @author KiteClass Team
 * @since 4.0.0 — GAP-288
 */

import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { OnboardingReplayCard } from '../OnboardingReplayCard';
import { getOnboardingEvents } from '@/lib/onboarding-telemetry';
import { STORAGE_KEY } from '../OnboardingWizard';

let store: Record<string, string> = {};

const localStorageMock = {
  getItem: vi.fn((key: string) => store[key] ?? null),
  setItem: vi.fn((key: string, value: string) => {
    store[key] = value;
  }),
  removeItem: vi.fn((key: string) => {
    delete store[key];
  }),
  clear: vi.fn(() => {
    store = {};
  }),
  get length() {
    return Object.keys(store).length;
  },
  key: vi.fn((_index: number) => null),
};

Object.defineProperty(window, 'localStorage', { value: localStorageMock });

const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
    replace: vi.fn(),
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => '/settings',
}));

describe('OnboardingReplayCard', () => {
  beforeEach(() => {
    store = {};
    vi.clearAllMocks();
  });

  it('renders the replay CTA', () => {
    render(<OnboardingReplayCard />);
    expect(
      screen.getByRole('button', { name: /Xem lại hướng dẫn/ })
    ).toBeInTheDocument();
  });

  it('resets onboarding progress + navigates to dashboard on click', () => {
    store[STORAGE_KEY] = JSON.stringify({ currentStep: 5, completed: true });
    render(<OnboardingReplayCard />);

    fireEvent.click(screen.getByRole('button', { name: /Xem lại hướng dẫn/ }));

    expect(localStorageMock.removeItem).toHaveBeenCalledWith(STORAGE_KEY);
    expect(mockPush).toHaveBeenCalledWith('/dashboard');
  });

  it('records a replay telemetry event', () => {
    render(<OnboardingReplayCard />);
    fireEvent.click(screen.getByRole('button', { name: /Xem lại hướng dẫn/ }));
    expect(
      getOnboardingEvents().some((e) => e.type === 'onboarding_replay')
    ).toBe(true);
  });
});
