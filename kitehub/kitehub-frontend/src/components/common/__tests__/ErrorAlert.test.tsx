/**
 * Component tests for ErrorAlert.
 *
 * @since PR 5.10
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@/test/test-utils';
import { ErrorAlert } from '../ErrorAlert';

describe('ErrorAlert', () => {
  describe('rendering', () => {
    it('renders error message', () => {
      render(<ErrorAlert message="Something went wrong" />);
      expect(screen.getByText('Something went wrong')).toBeInTheDocument();
    });

    it('renders default title "Lỗi"', () => {
      render(<ErrorAlert message="Error message" />);
      expect(screen.getByText('Lỗi')).toBeInTheDocument();
    });

    it('renders custom title', () => {
      render(<ErrorAlert title="Custom Error" message="Error message" />);
      expect(screen.getByText('Custom Error')).toBeInTheDocument();
      expect(screen.queryByText('Lỗi')).not.toBeInTheDocument();
    });

    it('applies destructive border and background styles', () => {
      const { container } = render(<ErrorAlert message="Error" />);
      const alertDiv = container.firstChild;
      expect(alertDiv).toHaveClass('rounded-lg', 'border', 'border-destructive/50', 'bg-destructive/10', 'p-4');
    });
  });

  describe('retry button', () => {
    it('does not render retry button when onRetry is not provided', () => {
      render(<ErrorAlert message="Error message" />);
      expect(screen.queryByText('Thử lại')).not.toBeInTheDocument();
    });

    it('renders retry button when onRetry is provided', () => {
      render(<ErrorAlert message="Error message" onRetry={() => {}} />);
      expect(screen.getByText('Thử lại')).toBeInTheDocument();
    });

    it('calls onRetry when retry button is clicked', () => {
      const onRetry = vi.fn();
      render(<ErrorAlert message="Error message" onRetry={onRetry} />);

      fireEvent.click(screen.getByText('Thử lại'));
      expect(onRetry).toHaveBeenCalledTimes(1);
    });

    it('retry button has correct styles', () => {
      render(<ErrorAlert message="Error message" onRetry={() => {}} />);
      const button = screen.getByText('Thử lại');
      expect(button).toHaveClass('text-sm', 'font-medium', 'text-destructive');
    });
  });

  describe('accessibility', () => {
    it('title is rendered as h3 heading', () => {
      render(<ErrorAlert message="Error message" />);
      const heading = screen.getByRole('heading', { level: 3 });
      expect(heading).toHaveTextContent('Lỗi');
    });

    it('retry button is accessible', () => {
      render(<ErrorAlert message="Error message" onRetry={() => {}} />);
      const button = screen.getByRole('button');
      expect(button).toHaveTextContent('Thử lại');
    });
  });
});
