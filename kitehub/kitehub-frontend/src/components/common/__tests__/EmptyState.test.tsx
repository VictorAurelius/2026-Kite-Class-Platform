/**
 * Component tests for EmptyState.
 *
 * @since PR 5.10
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/test-utils';
import { EmptyState } from '../EmptyState';

describe('EmptyState', () => {
  describe('rendering', () => {
    it('renders title', () => {
      render(<EmptyState title="No data" description="No data available" />);
      expect(screen.getByText('No data')).toBeInTheDocument();
    });

    it('renders description', () => {
      render(<EmptyState title="Title" description="This is the description" />);
      expect(screen.getByText('This is the description')).toBeInTheDocument();
    });

    it('renders default icon when not provided', () => {
      render(<EmptyState title="Title" description="Description" />);
      expect(screen.getByText('📭')).toBeInTheDocument();
    });

    it('renders custom icon when provided', () => {
      render(
        <EmptyState
          title="Title"
          description="Description"
          icon={<span data-testid="custom-icon">🎉</span>}
        />
      );
      expect(screen.getByTestId('custom-icon')).toBeInTheDocument();
      expect(screen.queryByText('📭')).not.toBeInTheDocument();
    });

    it('applies centered flex container styles', () => {
      const { container } = render(<EmptyState title="Title" description="Description" />);
      const wrapper = container.firstChild;
      expect(wrapper).toHaveClass('flex', 'flex-col', 'items-center', 'justify-center', 'py-12', 'text-center');
    });
  });

  describe('action', () => {
    it('does not render action section when action is not provided', () => {
      const { container } = render(<EmptyState title="Title" description="Description" />);
      // Should only have icon, title, and description - no action div
      const children = container.firstChild?.childNodes;
      expect(children?.length).toBe(3); // icon, h3, p
    });

    it('renders action when provided', () => {
      render(
        <EmptyState
          title="Title"
          description="Description"
          action={<button>Click me</button>}
        />
      );
      expect(screen.getByRole('button', { name: 'Click me' })).toBeInTheDocument();
    });

    it('action is wrapped in a mt-4 div', () => {
      render(
        <EmptyState
          title="Title"
          description="Description"
          action={<button data-testid="action-btn">Action</button>}
        />
      );
      const button = screen.getByTestId('action-btn');
      expect(button.parentElement).toHaveClass('mt-4');
    });
  });

  describe('typography', () => {
    it('title is rendered as h3 with correct styles', () => {
      render(<EmptyState title="My Title" description="Description" />);
      const heading = screen.getByRole('heading', { level: 3 });
      expect(heading).toHaveTextContent('My Title');
      expect(heading).toHaveClass('text-lg', 'font-medium');
    });

    it('description has correct styles', () => {
      render(<EmptyState title="Title" description="My description" />);
      const description = screen.getByText('My description');
      expect(description).toHaveClass('text-sm', 'text-muted-foreground', 'max-w-sm');
    });

    it('icon has correct styles', () => {
      const { container } = render(<EmptyState title="Title" description="Description" />);
      const iconDiv = container.querySelector('.text-4xl');
      expect(iconDiv).toHaveClass('mb-4', 'text-muted-foreground');
    });
  });
});
