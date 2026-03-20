/**
 * GradientText Component Tests
 *
 * Tests for the gradient text component.
 *
 * @since PR-Q4
 */

import { describe, it, expect } from 'vitest';
import { screen } from '@testing-library/react';
import { render } from '@/__tests__/test-utils';
import { GradientText } from '../gradient-text';

describe('GradientText', () => {
  it('renders as span by default', () => {
    render(<GradientText>Gradient Text</GradientText>);

    const text = screen.getByText('Gradient Text');
    expect(text).toBeInTheDocument();
    expect(text.tagName).toBe('SPAN');
  });

  it('renders as different HTML elements', () => {
    const { rerender } = render(<GradientText as="h1">Heading 1</GradientText>);
    let element = screen.getByText('Heading 1');
    expect(element.tagName).toBe('H1');

    rerender(<GradientText as="h2">Heading 2</GradientText>);
    element = screen.getByText('Heading 2');
    expect(element.tagName).toBe('H2');

    rerender(<GradientText as="h3">Heading 3</GradientText>);
    element = screen.getByText('Heading 3');
    expect(element.tagName).toBe('H3');

    rerender(<GradientText as="p">Paragraph</GradientText>);
    element = screen.getByText('Paragraph');
    expect(element.tagName).toBe('P');
  });

  it('applies gradient classes', () => {
    render(<GradientText>Text</GradientText>);

    const element = screen.getByText('Text');
    expect(element).toHaveClass('bg-gradient-to-r');
    expect(element).toHaveClass('from-primary');
    expect(element).toHaveClass('via-accent');
    expect(element).toHaveClass('to-primary');
    expect(element).toHaveClass('bg-clip-text');
    expect(element).toHaveClass('text-transparent');
  });

  it('accepts and applies custom className', () => {
    render(<GradientText className="custom-class">Text</GradientText>);

    const element = screen.getByText('Text');
    expect(element).toHaveClass('custom-class');
    // Should still have gradient classes
    expect(element).toHaveClass('bg-gradient-to-r');
  });

  it('passes through HTML attributes', () => {
    render(
      <GradientText data-testid="gradient-text" id="my-gradient">
        Text
      </GradientText>
    );

    const element = screen.getByTestId('gradient-text');
    expect(element).toHaveAttribute('id', 'my-gradient');
  });
});
