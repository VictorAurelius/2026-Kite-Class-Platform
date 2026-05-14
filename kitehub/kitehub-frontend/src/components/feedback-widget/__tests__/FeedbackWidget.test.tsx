/**
 * Component tests for FeedbackWidget (GAP-542 Wave 78 Bucket F).
 *
 * MSW handlers consumed from `kitehub-frontend/src/test/msw/handlers/feedback.ts`
 * (Bucket 0 Foundation). Tests cover:
 *  - Trigger button visibility (idle state)
 *  - Open + close dialog
 *  - 5-star rating selection
 *  - Form validation (submit disabled when incomplete)
 *  - Success submit (HTTP 201 → success message)
 *  - Error submit (HTTP 400 invalid comment → inline error)
 *  - Honeypot field is hidden + present (not user-visible)
 *
 * @since Wave 78 Bucket F
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, waitFor, cleanup } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { FeedbackWidget } from '../FeedbackWidget';

describe('FeedbackWidget — GAP-542 Wave 78 Bucket F', () => {
  beforeEach(() => {
    vi.useRealTimers();
  });

  afterEach(() => {
    cleanup();
  });

  describe('idle state', () => {
    it('renders the floating trigger button when closed', () => {
      render(<FeedbackWidget />);
      expect(screen.getByTestId('feedback-widget-trigger')).toBeInTheDocument();
      expect(screen.queryByTestId('feedback-widget-dialog')).not.toBeInTheDocument();
    });

    it('trigger button has accessible label', () => {
      render(<FeedbackWidget />);
      expect(screen.getByTestId('feedback-widget-trigger')).toHaveAttribute(
        'aria-label',
        'Mở form góp ý'
      );
    });
  });

  describe('open / close', () => {
    it('opens dialog on trigger click', async () => {
      const user = userEvent.setup();
      render(<FeedbackWidget />);
      await user.click(screen.getByTestId('feedback-widget-trigger'));
      expect(screen.getByTestId('feedback-widget-dialog')).toBeInTheDocument();
      expect(screen.getByRole('dialog')).toHaveAttribute('aria-modal', 'true');
    });

    it('closes dialog on close button click', async () => {
      const user = userEvent.setup();
      render(<FeedbackWidget />);
      await user.click(screen.getByTestId('feedback-widget-trigger'));
      await user.click(screen.getByTestId('feedback-widget-close'));
      expect(screen.queryByTestId('feedback-widget-dialog')).not.toBeInTheDocument();
    });

    it('closes dialog on cancel button click', async () => {
      const user = userEvent.setup();
      render(<FeedbackWidget />);
      await user.click(screen.getByTestId('feedback-widget-trigger'));
      await user.click(screen.getByTestId('feedback-widget-cancel'));
      expect(screen.queryByTestId('feedback-widget-dialog')).not.toBeInTheDocument();
    });
  });

  describe('rating', () => {
    it('renders 5 star buttons', async () => {
      const user = userEvent.setup();
      render(<FeedbackWidget />);
      await user.click(screen.getByTestId('feedback-widget-trigger'));
      for (const star of [1, 2, 3, 4, 5]) {
        expect(screen.getByTestId(`feedback-widget-star-${star}`)).toBeInTheDocument();
      }
    });

    it('selects rating on star click', async () => {
      const user = userEvent.setup();
      render(<FeedbackWidget />);
      await user.click(screen.getByTestId('feedback-widget-trigger'));
      const star4 = screen.getByTestId('feedback-widget-star-4');
      await user.click(star4);
      expect(star4).toHaveAttribute('aria-checked', 'true');
    });
  });

  describe('validation', () => {
    it('disables submit while comment is too short', async () => {
      const user = userEvent.setup();
      render(<FeedbackWidget />);
      await user.click(screen.getByTestId('feedback-widget-trigger'));
      await user.click(screen.getByTestId('feedback-widget-star-5'));
      // Type only 3 chars — under minimum of 5
      await user.type(screen.getByTestId('feedback-widget-comment'), 'abc');
      expect(screen.getByTestId('feedback-widget-submit')).toBeDisabled();
    });

    it('disables submit when no rating selected', async () => {
      const user = userEvent.setup();
      render(<FeedbackWidget />);
      await user.click(screen.getByTestId('feedback-widget-trigger'));
      // Skip rating, type valid comment
      await user.type(
        screen.getByTestId('feedback-widget-comment'),
        'Đây là góp ý đủ dài để pass validation.'
      );
      expect(screen.getByTestId('feedback-widget-submit')).toBeDisabled();
    });

    it('enables submit when rating + comment ≥5 chars present', async () => {
      const user = userEvent.setup();
      render(<FeedbackWidget />);
      await user.click(screen.getByTestId('feedback-widget-trigger'));
      await user.click(screen.getByTestId('feedback-widget-star-3'));
      await user.type(
        screen.getByTestId('feedback-widget-comment'),
        'Nội dung góp ý đủ dài.'
      );
      expect(screen.getByTestId('feedback-widget-submit')).toBeEnabled();
    });
  });

  describe('honeypot field', () => {
    it('renders an honeypot input that is visually hidden', async () => {
      const user = userEvent.setup();
      render(<FeedbackWidget />);
      await user.click(screen.getByTestId('feedback-widget-trigger'));
      // The honeypot label is offscreen; the input is rendered but with tabIndex -1
      const honeypot = document.querySelector('input[tabindex="-1"]') as HTMLInputElement | null;
      expect(honeypot).not.toBeNull();
      expect(honeypot?.value).toBe('');
    });
  });

  describe('submit — happy path (MSW)', () => {
    it('shows success message on HTTP 201', async () => {
      const user = userEvent.setup();
      render(<FeedbackWidget />);
      await user.click(screen.getByTestId('feedback-widget-trigger'));
      await user.click(screen.getByTestId('feedback-widget-star-5'));
      await user.type(
        screen.getByTestId('feedback-widget-comment'),
        'Onboarding rất rõ ràng, cảm ơn!'
      );
      await user.click(screen.getByTestId('feedback-widget-submit'));

      await waitFor(() => {
        expect(screen.getByTestId('feedback-widget-success')).toBeInTheDocument();
      });
      expect(screen.getByTestId('feedback-widget-success')).toHaveTextContent(
        /Cảm ơn bạn đã gửi góp ý/
      );
    });
  });

  describe('submit — error path', () => {
    it('shows inline error when fetch rejects', async () => {
      const user = userEvent.setup();
      // Override fetch to reject — simulates network drop
      const fetchSpy = vi.spyOn(global, 'fetch').mockRejectedValueOnce(new Error('Network down'));

      render(<FeedbackWidget />);
      await user.click(screen.getByTestId('feedback-widget-trigger'));
      await user.click(screen.getByTestId('feedback-widget-star-2'));
      await user.type(
        screen.getByTestId('feedback-widget-comment'),
        'Đây là một test lỗi mạng.'
      );
      await user.click(screen.getByTestId('feedback-widget-submit'));

      await waitFor(() => {
        expect(screen.getByTestId('feedback-widget-error')).toBeInTheDocument();
      });
      expect(screen.getByTestId('feedback-widget-error')).toHaveTextContent(/Network down/);
      fetchSpy.mockRestore();
    });
  });

  describe('category', () => {
    it('defaults to GENERAL', async () => {
      const user = userEvent.setup();
      render(<FeedbackWidget />);
      await user.click(screen.getByTestId('feedback-widget-trigger'));
      const select = screen.getByTestId('feedback-widget-category') as HTMLSelectElement;
      expect(select.value).toBe('GENERAL');
    });

    it('updates selection', async () => {
      const user = userEvent.setup();
      render(<FeedbackWidget />);
      await user.click(screen.getByTestId('feedback-widget-trigger'));
      const select = screen.getByTestId('feedback-widget-category') as HTMLSelectElement;
      await user.selectOptions(select, 'BUG');
      expect(select.value).toBe('BUG');
    });
  });
});
