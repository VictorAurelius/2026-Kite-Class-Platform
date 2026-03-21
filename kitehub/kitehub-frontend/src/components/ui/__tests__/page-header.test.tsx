/**
 * PageHeader Component Tests
 *
 * Tests for the page header component.
 *
 * @since PR-Q4
 */

import { describe, it, expect } from 'vitest';
import { screen } from '@testing-library/react';
import { render } from '@/__tests__/test-utils';
import { PageHeader } from '../page-header';

describe('PageHeader', () => {
  it('renders with title only', () => {
    render(<PageHeader title="Page Title" />);

    expect(screen.getByRole('heading', { name: 'Page Title' })).toBeInTheDocument();
  });

  it('renders title and description', () => {
    render(
      <PageHeader title="Page Title" description="This is the page description" />
    );

    expect(screen.getByRole('heading', { name: 'Page Title' })).toBeInTheDocument();
    expect(screen.getByText('This is the page description')).toBeInTheDocument();
  });

  it('does not render description when not provided', () => {
    render(<PageHeader title="Page Title" />);

    // Should not have any paragraph elements
    const paragraphs = screen.queryAllByRole('paragraph');
    expect(paragraphs).toHaveLength(0);
  });

  it('renders children in action area', () => {
    render(
      <PageHeader title="Page Title">
        <button>Action Button</button>
      </PageHeader>
    );

    expect(screen.getByRole('heading', { name: 'Page Title' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Action Button' })).toBeInTheDocument();
  });

  it('accepts and applies custom className', () => {
    const { container } = render(
      <PageHeader title="Page Title" className="custom-class" />
    );

    // The outer div should have the custom class
    const wrapper = container.firstChild as HTMLElement;
    expect(wrapper).toHaveClass('custom-class');
    expect(wrapper).toHaveClass('flex');
    expect(wrapper).toHaveClass('items-center');
  });

  it('renders all props together', () => {
    render(
      <PageHeader
        title="Complete Header"
        description="With description"
        className="my-custom-class"
      >
        <button>Primary Action</button>
        <button>Secondary Action</button>
      </PageHeader>
    );

    expect(screen.getByRole('heading', { name: 'Complete Header' })).toBeInTheDocument();
    expect(screen.getByText('With description')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Primary Action' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Secondary Action' })).toBeInTheDocument();
  });
});
