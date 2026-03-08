/**
 * LoadingSpinner Component Tests
 *
 * @author KiteClass Team
 * @since 3.8.0
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/utils';
import { LoadingSpinner, LoadingOverlay } from '../loading-spinner';

describe('LoadingSpinner', () => {
  it('should render spinner with default medium size', () => {
    const { container } = render(<LoadingSpinner />);
    const spinner = container.querySelector('.h-8.w-8');
    expect(spinner).toBeInTheDocument();
  });

  it('should render small size when specified', () => {
    const { container } = render(<LoadingSpinner size="sm" />);
    const spinner = container.querySelector('.h-4.w-4');
    expect(spinner).toBeInTheDocument();
  });

  it('should render large size when specified', () => {
    const { container } = render(<LoadingSpinner size="lg" />);
    const spinner = container.querySelector('.h-12.w-12');
    expect(spinner).toBeInTheDocument();
  });

  it('should display text when provided', () => {
    render(<LoadingSpinner text="Loading data..." />);
    expect(screen.getByText('Loading data...')).toBeInTheDocument();
  });

  it('should not display text when not provided', () => {
    const { container } = render(<LoadingSpinner />);
    expect(container.querySelector('p')).not.toBeInTheDocument();
  });

  it('should apply custom className', () => {
    const { container } = render(<LoadingSpinner className="custom-class" />);
    expect(container.querySelector('.custom-class')).toBeInTheDocument();
  });
});

describe('LoadingOverlay', () => {
  it('should render overlay with default text', () => {
    render(<LoadingOverlay />);
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('should render overlay with custom text', () => {
    render(<LoadingOverlay text="Processing..." />);
    expect(screen.getByText('Processing...')).toBeInTheDocument();
  });

  it('should use large spinner in overlay', () => {
    const { container } = render(<LoadingOverlay />);
    const spinner = container.querySelector('.h-12.w-12');
    expect(spinner).toBeInTheDocument();
  });
});
