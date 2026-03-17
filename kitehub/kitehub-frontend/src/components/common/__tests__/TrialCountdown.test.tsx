/**
 * Component tests for TrialCountdown.
 *
 * @since PR 5.9
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/test-utils';
import { TrialCountdown } from '../TrialCountdown';
import type { TrialStatus } from '@/types/instance';

describe('TrialCountdown', () => {
  const baseTrial: TrialStatus = {
    instanceId: 'inst-uuid-1',
    trialEndDate: '2026-03-30T00:00:00Z',
    daysRemaining: 14,
    warningLevel: 'NONE',
    expired: false,
  };

  describe('rendering', () => {
    it('renders Trial label', () => {
      render(<TrialCountdown trial={baseTrial} />);
      expect(screen.getByText('Trial')).toBeInTheDocument();
    });

    it('displays days remaining', () => {
      render(<TrialCountdown trial={baseTrial} />);
      expect(screen.getByText('Còn 14 ngày')).toBeInTheDocument();
    });

    it('renders progress bar container', () => {
      const { container } = render(<TrialCountdown trial={baseTrial} />);
      const progressBar = container.querySelector('.rounded-full.bg-muted');
      expect(progressBar).toBeInTheDocument();
    });
  });

  describe('days remaining display', () => {
    it('shows singular day correctly', () => {
      const trial = { ...baseTrial, daysRemaining: 1 };
      render(<TrialCountdown trial={trial} />);
      expect(screen.getByText('Còn 1 ngày')).toBeInTheDocument();
    });

    it('shows 0 days remaining', () => {
      const trial = { ...baseTrial, daysRemaining: 0, warningLevel: 'EXPIRED' as const };
      render(<TrialCountdown trial={trial} />);
      expect(screen.getByText('Còn 0 ngày')).toBeInTheDocument();
    });

    it('shows "Hết hạn" when expired', () => {
      const trial = { ...baseTrial, expired: true, daysRemaining: 0, warningLevel: 'EXPIRED' as const };
      render(<TrialCountdown trial={trial} />);
      expect(screen.getByText('Hết hạn')).toBeInTheDocument();
    });
  });

  describe('warning levels and colors', () => {
    it('applies blue color for NONE warning level', () => {
      const { container } = render(<TrialCountdown trial={baseTrial} />);
      const progressFill = container.querySelector('.bg-blue-500');
      expect(progressFill).toBeInTheDocument();
    });

    it('applies blue color for LOW warning level', () => {
      const trial = { ...baseTrial, warningLevel: 'LOW' as const, daysRemaining: 10 };
      const { container } = render(<TrialCountdown trial={trial} />);
      const progressFill = container.querySelector('.bg-blue-500');
      expect(progressFill).toBeInTheDocument();
    });

    it('applies yellow color for MEDIUM warning level', () => {
      const trial = { ...baseTrial, warningLevel: 'MEDIUM' as const, daysRemaining: 5 };
      const { container } = render(<TrialCountdown trial={trial} />);
      const progressFill = container.querySelector('.bg-yellow-500');
      expect(progressFill).toBeInTheDocument();
    });

    it('applies orange color for HIGH warning level', () => {
      const trial = { ...baseTrial, warningLevel: 'HIGH' as const, daysRemaining: 2 };
      const { container } = render(<TrialCountdown trial={trial} />);
      const progressFill = container.querySelector('.bg-orange-500');
      expect(progressFill).toBeInTheDocument();
    });

    it('applies red color for EXPIRED warning level', () => {
      const trial = { ...baseTrial, warningLevel: 'EXPIRED' as const, daysRemaining: 0, expired: true };
      const { container } = render(<TrialCountdown trial={trial} />);
      const progressFill = container.querySelector('.bg-red-500');
      expect(progressFill).toBeInTheDocument();
    });
  });

  describe('progress bar calculation', () => {
    it('shows 100% progress for 14 days remaining', () => {
      const { container } = render(<TrialCountdown trial={baseTrial} />);
      const progressFill = container.querySelector('[style*="width"]');
      expect(progressFill).toHaveStyle({ width: '100%' });
    });

    it('shows 50% progress for 7 days remaining', () => {
      const trial = { ...baseTrial, daysRemaining: 7 };
      const { container } = render(<TrialCountdown trial={trial} />);
      const progressFill = container.querySelector('[style*="width"]');
      expect(progressFill).toHaveStyle({ width: '50%' });
    });

    it('shows 0% progress for 0 days remaining', () => {
      const trial = { ...baseTrial, daysRemaining: 0 };
      const { container } = render(<TrialCountdown trial={trial} />);
      const progressFill = container.querySelector('[style*="width"]');
      expect(progressFill).toHaveStyle({ width: '0%' });
    });

    it('caps progress at 100% for more than 14 days', () => {
      const trial = { ...baseTrial, daysRemaining: 20 };
      const { container } = render(<TrialCountdown trial={trial} />);
      const progressFill = container.querySelector('[style*="width"]');
      expect(progressFill).toHaveStyle({ width: '100%' });
    });

    it('caps progress at 0% for negative days', () => {
      const trial = { ...baseTrial, daysRemaining: -5 };
      const { container } = render(<TrialCountdown trial={trial} />);
      const progressFill = container.querySelector('[style*="width"]');
      expect(progressFill).toHaveStyle({ width: '0%' });
    });
  });

  describe('expired state styling', () => {
    it('applies destructive text color when expired', () => {
      const trial = { ...baseTrial, expired: true, daysRemaining: 0 };
      render(<TrialCountdown trial={trial} />);
      const expiredText = screen.getByText('Hết hạn');
      expect(expiredText).toHaveClass('text-destructive');
    });

    it('applies normal text color when not expired', () => {
      render(<TrialCountdown trial={baseTrial} />);
      const daysText = screen.getByText('Còn 14 ngày');
      expect(daysText).toHaveClass('text-foreground');
    });
  });
});
