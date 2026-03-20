/**
 * GradientButton Component Tests
 *
 * Tests for the gradient button component.
 *
 * @since PR-Q4
 */

import { describe, it, expect, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from '@/__tests__/test-utils';
import { GradientButton } from '../gradient-button';

describe('GradientButton', () => {
  it('renders with default props', () => {
    render(<GradientButton>Click me</GradientButton>);

    const button = screen.getByRole('button', { name: /click me/i });
    expect(button).toBeInTheDocument();
    expect(button).toHaveTextContent('Click me');
  });

  it('applies size variants correctly', () => {
    const { rerender } = render(<GradientButton size="sm">Small</GradientButton>);
    let button = screen.getByRole('button', { name: /small/i });
    expect(button).toHaveClass('h-9', 'px-4', 'text-sm');

    rerender(<GradientButton size="default">Default</GradientButton>);
    button = screen.getByRole('button', { name: /default/i });
    expect(button).toHaveClass('h-11', 'px-6', 'text-sm');

    rerender(<GradientButton size="lg">Large</GradientButton>);
    button = screen.getByRole('button', { name: /large/i });
    expect(button).toHaveClass('h-14', 'px-8', 'text-base');
  });

  it('accepts and applies custom className', () => {
    render(<GradientButton className="custom-class">Button</GradientButton>);

    const button = screen.getByRole('button', { name: /button/i });
    expect(button).toHaveClass('custom-class');
  });

  it('handles onClick events', async () => {
    const handleClick = vi.fn();
    const user = userEvent.setup();

    render(<GradientButton onClick={handleClick}>Click me</GradientButton>);

    const button = screen.getByRole('button', { name: /click me/i });
    await user.click(button);

    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('disables button when disabled prop is true', async () => {
    const handleClick = vi.fn();
    const user = userEvent.setup();

    render(
      <GradientButton disabled onClick={handleClick}>
        Disabled
      </GradientButton>
    );

    const button = screen.getByRole('button', { name: /disabled/i });
    expect(button).toBeDisabled();

    // Try to click - should not trigger handler
    await user.click(button);
    expect(handleClick).not.toHaveBeenCalled();
  });

  it('forwards ref to button element', () => {
    const ref = vi.fn();
    render(<GradientButton ref={ref}>Button</GradientButton>);

    expect(ref).toHaveBeenCalled();
  });

  it('passes through additional HTML button attributes', () => {
    render(
      <GradientButton type="submit" data-testid="submit-btn">
        Submit
      </GradientButton>
    );

    const button = screen.getByTestId('submit-btn');
    expect(button).toHaveAttribute('type', 'submit');
  });
});
