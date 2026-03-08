/**
 * PendingAttendanceBadge component tests.
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { PendingAttendanceBadge } from '../pending-attendance-badge';

describe('PendingAttendanceBadge', () => {
  describe('Visibility', () => {
    it('renders badge when count is greater than 0', () => {
      render(<PendingAttendanceBadge count={3} />);

      expect(screen.getByText(/Chưa điểm danh: 3/)).toBeInTheDocument();
    });

    it('does not render when count is 0', () => {
      const { container } = render(<PendingAttendanceBadge count={0} />);

      expect(container.firstChild).toBeNull();
    });

    it('renders with count of 1', () => {
      render(<PendingAttendanceBadge count={1} />);

      expect(screen.getByText(/Chưa điểm danh: 1/)).toBeInTheDocument();
    });

    it('renders with large count', () => {
      render(<PendingAttendanceBadge count={99} />);

      expect(screen.getByText(/Chưa điểm danh: 99/)).toBeInTheDocument();
    });
  });

  describe('Variants', () => {
    it('renders default variant with full text', () => {
      render(<PendingAttendanceBadge count={5} variant="default" />);

      expect(screen.getByText(/Chưa điểm danh: 5/)).toBeInTheDocument();
    });

    it('renders compact variant with just number', () => {
      render(<PendingAttendanceBadge count={5} variant="compact" />);

      expect(screen.getByText('5')).toBeInTheDocument();
      expect(screen.queryByText(/Chưa điểm danh/)).not.toBeInTheDocument();
    });
  });

  describe('Icon', () => {
    it('shows icon when showIcon is true (default)', () => {
      const { container } = render(<PendingAttendanceBadge count={3} />);

      const icon = container.querySelector('svg');
      expect(icon).toBeInTheDocument();
    });

    it('hides icon when showIcon is false', () => {
      const { container } = render(
        <PendingAttendanceBadge count={3} showIcon={false} />
      );

      const icon = container.querySelector('svg');
      expect(icon).not.toBeInTheDocument();
    });
  });

  describe('Styling', () => {
    it('applies destructive variant styling', () => {
      const { container } = render(<PendingAttendanceBadge count={3} />);

      const badge = container.querySelector('[class*="destructive"]');
      expect(badge).toBeInTheDocument();
    });

    it('applies compact styling for compact variant', () => {
      const { container } = render(
        <PendingAttendanceBadge count={3} variant="compact" />
      );

      const badge = container.querySelector('[class*="ml-2"]');
      expect(badge).toBeInTheDocument();
    });
  });
});
