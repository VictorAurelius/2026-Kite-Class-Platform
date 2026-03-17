/**
 * Component tests for LoadingSpinner.
 *
 * @since PR 5.10
 */

import { describe, it, expect } from 'vitest';
import { render } from '@/test/test-utils';
import { LoadingSpinner } from '../LoadingSpinner';

describe('LoadingSpinner', () => {
  describe('rendering', () => {
    it('renders spinner element', () => {
      const { container } = render(<LoadingSpinner />);
      const spinner = container.querySelector('.animate-spin');
      expect(spinner).toBeInTheDocument();
    });

    it('applies flex container styles', () => {
      const { container } = render(<LoadingSpinner />);
      const wrapper = container.firstChild;
      expect(wrapper).toHaveClass('flex', 'items-center', 'justify-center');
    });

    it('applies spinner animation and border styles', () => {
      const { container } = render(<LoadingSpinner />);
      const spinner = container.querySelector('.animate-spin');
      expect(spinner).toHaveClass('rounded-full', 'border-2', 'border-muted', 'border-t-primary');
    });
  });

  describe('sizes', () => {
    it('renders small size (h-4 w-4)', () => {
      const { container } = render(<LoadingSpinner size="sm" />);
      const spinner = container.querySelector('.animate-spin');
      expect(spinner).toHaveClass('h-4', 'w-4');
    });

    it('renders medium size (h-8 w-8) by default', () => {
      const { container } = render(<LoadingSpinner />);
      const spinner = container.querySelector('.animate-spin');
      expect(spinner).toHaveClass('h-8', 'w-8');
    });

    it('renders large size (h-12 w-12)', () => {
      const { container } = render(<LoadingSpinner size="lg" />);
      const spinner = container.querySelector('.animate-spin');
      expect(spinner).toHaveClass('h-12', 'w-12');
    });
  });

  describe('customization', () => {
    it('applies custom className to wrapper', () => {
      const { container } = render(<LoadingSpinner className="my-custom-class" />);
      const wrapper = container.firstChild;
      expect(wrapper).toHaveClass('my-custom-class');
    });

    it('merges custom className with default classes', () => {
      const { container } = render(<LoadingSpinner className="p-4" />);
      const wrapper = container.firstChild;
      expect(wrapper).toHaveClass('flex', 'items-center', 'justify-center', 'p-4');
    });
  });
});
