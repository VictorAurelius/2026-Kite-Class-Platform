/**
 * Tests for OnboardingWizard multi-step onboarding component.
 *
 * @author KiteClass Team
 * @since 3.16.0
 */

import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { OnboardingWizard, resetOnboardingProgress, STORAGE_KEY } from '../OnboardingWizard';
import { getOnboardingEvents } from '@/lib/onboarding-telemetry';

// Shared store for localStorage mock
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

// Mock next/navigation
const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
    replace: vi.fn(),
    back: vi.fn(),
    prefetch: vi.fn(),
    pathname: '/',
  }),
  usePathname: () => '/',
  useSearchParams: () => new URLSearchParams(),
}));

/** Helper to navigate to a specific step */
function navigateToStep(targetStep: number) {
  for (let i = 1; i < targetStep; i++) {
    fireEvent.click(screen.getByRole('button', { name: /Tiếp theo/ }));
  }
}

describe('OnboardingWizard', () => {
  beforeEach(() => {
    // Reset store completely
    store = {};
    // Clear call history but keep implementations
    vi.clearAllMocks();
  });

  describe('Initial Rendering', () => {
    it('renders step 1 by default with title', () => {
      render(<OnboardingWizard />);

      expect(screen.getByText(/Bước 1\/5/)).toBeInTheDocument();
      expect(screen.getByText(/Xác nhận thông tin trường/)).toBeInTheDocument();
    });

    it('renders progress indicator', () => {
      render(<OnboardingWizard />);

      expect(screen.getByRole('progressbar')).toBeInTheDocument();
    });

    it('shows Skip and Next buttons', () => {
      render(<OnboardingWizard />);

      expect(screen.getByRole('button', { name: /Bỏ qua/ })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Tiếp theo/ })).toBeInTheDocument();
    });

    it('does not show Back button on first step', () => {
      render(<OnboardingWizard />);

      expect(screen.queryByRole('button', { name: /Quay lại/ })).not.toBeInTheDocument();
    });
  });

  describe('Step Navigation', () => {
    it('navigates to step 2 when Next is clicked', () => {
      render(<OnboardingWizard />);

      fireEvent.click(screen.getByRole('button', { name: /Tiếp theo/ }));

      expect(screen.getByText(/Bước 2\/5/)).toBeInTheDocument();
      expect(screen.getByText(/Thêm giáo viên đầu tiên/)).toBeInTheDocument();
    });

    it('shows Back button on step 2+', () => {
      render(<OnboardingWizard />);

      fireEvent.click(screen.getByRole('button', { name: /Tiếp theo/ }));

      expect(screen.getByRole('button', { name: /Quay lại/ })).toBeInTheDocument();
    });

    it('navigates back to step 1 from step 2', () => {
      render(<OnboardingWizard />);

      fireEvent.click(screen.getByRole('button', { name: /Tiếp theo/ }));
      fireEvent.click(screen.getByRole('button', { name: /Quay lại/ }));

      expect(screen.getByText(/Bước 1\/5/)).toBeInTheDocument();
    });

    it('navigates through all 5 steps', () => {
      render(<OnboardingWizard />);

      fireEvent.click(screen.getByRole('button', { name: /Tiếp theo/ }));
      expect(screen.getByText(/Bước 2\/5/)).toBeInTheDocument();

      fireEvent.click(screen.getByRole('button', { name: /Tiếp theo/ }));
      expect(screen.getByText(/Bước 3\/5/)).toBeInTheDocument();

      fireEvent.click(screen.getByRole('button', { name: /Tiếp theo/ }));
      expect(screen.getByText(/Bước 4\/5/)).toBeInTheDocument();

      fireEvent.click(screen.getByRole('button', { name: /Tiếp theo/ }));
      expect(screen.getByText(/Bước 5\/5/)).toBeInTheDocument();
    });
  });

  describe('Step Content', () => {
    it('step 1 has link to settings page', () => {
      render(<OnboardingWizard />);

      const link = screen.getByRole('link', { name: /Xem cài đặt trường/ });
      expect(link).toHaveAttribute('href', '/settings');
    });

    it('step 2 has link to teachers page', () => {
      render(<OnboardingWizard />);
      navigateToStep(2);

      const link = screen.getByRole('link', { name: /Thêm giáo viên/ });
      expect(link).toHaveAttribute('href', '/teachers');
    });

    it('step 3 has link to courses page', () => {
      render(<OnboardingWizard />);
      navigateToStep(3);

      const link = screen.getByRole('link', { name: /Tạo khóa học/ });
      expect(link).toHaveAttribute('href', '/courses');
    });

    it('step 4 has link to students page', () => {
      render(<OnboardingWizard />);
      navigateToStep(4);

      const link = screen.getByRole('link', { name: /Thêm học sinh/ });
      expect(link).toHaveAttribute('href', '/students');
    });

    it('step 5 shows completion and finish button', () => {
      render(<OnboardingWizard />);
      navigateToStep(5);

      expect(screen.getByText(/Hoàn thành/)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Bắt đầu sử dụng/ })).toBeInTheDocument();
    });
  });

  describe('Progress Tracking', () => {
    it('saves progress to localStorage on step change', () => {
      render(<OnboardingWizard />);

      fireEvent.click(screen.getByRole('button', { name: /Tiếp theo/ }));

      expect(localStorageMock.setItem).toHaveBeenCalledWith(
        'kiteclass-onboarding-progress',
        expect.stringContaining('"currentStep":2')
      );
    });

    it('restores progress from localStorage on mount', () => {
      store['kiteclass-onboarding-progress'] = JSON.stringify({
        currentStep: 3,
        completed: false,
      });

      render(<OnboardingWizard />);

      expect(screen.getByText(/Bước 3\/5/)).toBeInTheDocument();
    });

    it('marks as completed on final step finish', () => {
      render(<OnboardingWizard />);
      navigateToStep(5);

      fireEvent.click(screen.getByRole('button', { name: /Bắt đầu sử dụng/ }));

      expect(localStorageMock.setItem).toHaveBeenCalledWith(
        'kiteclass-onboarding-progress',
        expect.stringContaining('"completed":true')
      );
    });
  });

  describe('Skip Functionality', () => {
    it('skips to next step when Skip is clicked', () => {
      render(<OnboardingWizard />);

      fireEvent.click(screen.getByRole('button', { name: /Bỏ qua/ }));

      expect(screen.getByText(/Bước 2\/5/)).toBeInTheDocument();
    });

    it('step 5 does not have skip button, only finish button', () => {
      render(<OnboardingWizard />);
      navigateToStep(5);

      expect(screen.queryByRole('button', { name: /Bỏ qua/ })).not.toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Bắt đầu sử dụng/ })).toBeInTheDocument();
    });
  });

  describe('Completion State', () => {
    it('does not render when wizard was previously completed', () => {
      store['kiteclass-onboarding-progress'] = JSON.stringify({
        currentStep: 5,
        completed: true,
      });

      render(<OnboardingWizard />);

      expect(screen.queryByText(/Bước/)).not.toBeInTheDocument();
    });

    it('calls onComplete callback when wizard finishes', () => {
      const onComplete = vi.fn();
      render(<OnboardingWizard onComplete={onComplete} />);
      navigateToStep(5);

      fireEvent.click(screen.getByRole('button', { name: /Bắt đầu sử dụng/ }));

      expect(onComplete).toHaveBeenCalled();
    });
  });

  describe('Progress Bar', () => {
    it('shows 20% progress on step 1', () => {
      render(<OnboardingWizard />);

      const progressBar = screen.getByRole('progressbar');
      expect(progressBar).toHaveAttribute('aria-valuenow', '20');
    });

    it('shows 100% progress on step 5', () => {
      render(<OnboardingWizard />);
      navigateToStep(5);

      const progressBar = screen.getByRole('progressbar');
      expect(progressBar).toHaveAttribute('aria-valuenow', '100');
    });
  });

  describe('Skip Whole Tour', () => {
    it('shows "Đóng hướng dẫn" control on every step', () => {
      render(<OnboardingWizard />);
      expect(
        screen.getByRole('button', { name: /Đóng hướng dẫn/ })
      ).toBeInTheDocument();
      navigateToStep(3);
      expect(
        screen.getByRole('button', { name: /Đóng hướng dẫn/ })
      ).toBeInTheDocument();
    });

    it('marks complete + calls onComplete when whole tour skipped', () => {
      const onComplete = vi.fn();
      render(<OnboardingWizard onComplete={onComplete} />);

      fireEvent.click(screen.getByRole('button', { name: /Đóng hướng dẫn/ }));

      expect(localStorageMock.setItem).toHaveBeenCalledWith(
        'kiteclass-onboarding-progress',
        expect.stringContaining('"completed":true')
      );
      expect(onComplete).toHaveBeenCalled();
      expect(screen.queryByText(/Bước/)).not.toBeInTheDocument();
    });
  });

  describe('Telemetry', () => {
    it('records start + step_view on mount', () => {
      render(<OnboardingWizard />);
      const events = getOnboardingEvents();
      expect(events.some((e) => e.type === 'onboarding_start')).toBe(true);
      expect(
        events.some((e) => e.type === 'onboarding_step_view' && e.step === 1)
      ).toBe(true);
    });

    it('records step_skip on per-step skip', () => {
      render(<OnboardingWizard />);
      fireEvent.click(screen.getByRole('button', { name: /Bỏ qua bước/ }));
      expect(
        getOnboardingEvents().some((e) => e.type === 'onboarding_step_skip')
      ).toBe(true);
    });

    it('records complete on finish', () => {
      render(<OnboardingWizard />);
      navigateToStep(5);
      fireEvent.click(screen.getByRole('button', { name: /Bắt đầu sử dụng/ }));
      expect(
        getOnboardingEvents().some((e) => e.type === 'onboarding_complete')
      ).toBe(true);
    });

    it('does not record start when already completed', () => {
      store[STORAGE_KEY] = JSON.stringify({ currentStep: 5, completed: true });
      render(<OnboardingWizard />);
      expect(
        getOnboardingEvents().some((e) => e.type === 'onboarding_start')
      ).toBe(false);
    });
  });

  describe('resetOnboardingProgress', () => {
    it('removes progress so wizard re-fires', () => {
      store[STORAGE_KEY] = JSON.stringify({ currentStep: 5, completed: true });
      resetOnboardingProgress();
      expect(localStorageMock.removeItem).toHaveBeenCalledWith(STORAGE_KEY);
      expect(store[STORAGE_KEY]).toBeUndefined();
    });
  });
});
