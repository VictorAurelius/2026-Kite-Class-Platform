/**
 * Component tests for StatusBadge.
 *
 * @since PR 5.9
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/test-utils';
import { StatusBadge } from '../StatusBadge';

describe('StatusBadge', () => {
  describe('rendering', () => {
    it('renders status text', () => {
      render(<StatusBadge status="ACTIVE" />);
      expect(screen.getByText('ACTIVE')).toBeInTheDocument();
    });

    it('applies base styles', () => {
      render(<StatusBadge status="ACTIVE" />);
      const badge = screen.getByText('ACTIVE');
      expect(badge).toHaveClass('inline-flex', 'items-center', 'rounded-full');
    });

    it('applies custom className', () => {
      render(<StatusBadge status="ACTIVE" className="custom-class" />);
      const badge = screen.getByText('ACTIVE');
      expect(badge).toHaveClass('custom-class');
    });
  });

  describe('status colors', () => {
    it('applies TRIAL styles (blue)', () => {
      render(<StatusBadge status="TRIAL" />);
      const badge = screen.getByText('TRIAL');
      expect(badge).toHaveClass('bg-blue-100', 'text-blue-800');
    });

    it('applies ACTIVE styles (green)', () => {
      render(<StatusBadge status="ACTIVE" />);
      const badge = screen.getByText('ACTIVE');
      expect(badge).toHaveClass('bg-green-100', 'text-green-800');
    });

    it('applies SUSPENDED styles (red)', () => {
      render(<StatusBadge status="SUSPENDED" />);
      const badge = screen.getByText('SUSPENDED');
      expect(badge).toHaveClass('bg-red-100', 'text-red-800');
    });

    it('applies EXPIRED styles (gray)', () => {
      render(<StatusBadge status="EXPIRED" />);
      const badge = screen.getByText('EXPIRED');
      expect(badge).toHaveClass('bg-gray-100', 'text-gray-800');
    });

    it('applies PENDING styles (yellow)', () => {
      render(<StatusBadge status="PENDING" />);
      const badge = screen.getByText('PENDING');
      expect(badge).toHaveClass('bg-yellow-100', 'text-yellow-800');
    });

    it('applies COMPLETED styles (green)', () => {
      render(<StatusBadge status="COMPLETED" />);
      const badge = screen.getByText('COMPLETED');
      expect(badge).toHaveClass('bg-green-100', 'text-green-800');
    });

    it('applies FAILED styles (red)', () => {
      render(<StatusBadge status="FAILED" />);
      const badge = screen.getByText('FAILED');
      expect(badge).toHaveClass('bg-red-100', 'text-red-800');
    });

    it('applies CANCELLED styles (gray)', () => {
      render(<StatusBadge status="CANCELLED" />);
      const badge = screen.getByText('CANCELLED');
      expect(badge).toHaveClass('bg-gray-100', 'text-gray-800');
    });

    it('applies PROCESSING styles (purple)', () => {
      render(<StatusBadge status="PROCESSING" />);
      const badge = screen.getByText('PROCESSING');
      expect(badge).toHaveClass('bg-purple-100', 'text-purple-800');
    });

    it('applies default styles for unknown status', () => {
      render(<StatusBadge status="UNKNOWN" />);
      const badge = screen.getByText('UNKNOWN');
      expect(badge).toHaveClass('bg-gray-100', 'text-gray-800');
    });
  });
});
