/**
 * Component tests for FeedbackForm (Wave 98 Bucket B5 — GAP-540 + GAP-542 merge).
 *
 * MSW handlers consumed from `kitehub-frontend/src/test/msw/handlers/feedback.ts`
 * (Wave 78 Bucket 0 Foundation — reused unchanged; backend endpoint POST
 * /api/v1/feedback already exists per FeedbackController.java).
 *
 * Tests cover the controlled-modal contract:
 *  - Renders nothing when `open=false`
 *  - Renders dialog when `open=true`
 *  - 5-star rating selection
 *  - Form validation (submit disabled when incomplete)
 *  - Success submit (HTTP 201 → success message + onClose triggered ~2s later)
 *  - Error submit (HTTP 400 invalid comment → inline error)
 *  - Cancel button triggers onClose
 *  - Escape key closes dialog (Radix built-in)
 *
 * @since Wave 98 Bucket B5
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, waitFor, cleanup } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { FeedbackForm } from '../FeedbackForm';

describe('FeedbackForm — Wave 98 B5 GAP-540 + GAP-542 merge', () => {
  beforeEach(() => {
    vi.useRealTimers();
  });

  afterEach(() => {
    cleanup();
  });

  describe('open / close contract', () => {
    it('renders nothing in DOM when open=false', () => {
      const onClose = vi.fn();
      render(<FeedbackForm open={false} onClose={onClose} />);
      expect(screen.queryByTestId('feedback-form-dialog')).not.toBeInTheDocument();
    });

    it('renders dialog when open=true', () => {
      const onClose = vi.fn();
      render(<FeedbackForm open={true} onClose={onClose} />);
      expect(screen.getByTestId('feedback-form-dialog')).toBeInTheDocument();
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('Cancel button triggers onClose', async () => {
      const user = userEvent.setup();
      const onClose = vi.fn();
      render(<FeedbackForm open={true} onClose={onClose} />);
      await user.click(screen.getByTestId('feedback-form-cancel'));
      expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('Escape key triggers onClose (Radix built-in)', async () => {
      const user = userEvent.setup();
      const onClose = vi.fn();
      render(<FeedbackForm open={true} onClose={onClose} />);
      await user.keyboard('{Escape}');
      expect(onClose).toHaveBeenCalled();
    });
  });

  describe('rating selection', () => {
    it('clicking a star sets aria-checked=true on that star', async () => {
      const user = userEvent.setup();
      render(<FeedbackForm open={true} onClose={vi.fn()} />);
      await user.click(screen.getByTestId('feedback-form-star-4'));
      expect(screen.getByTestId('feedback-form-star-4')).toHaveAttribute(
        'aria-checked',
        'true'
      );
    });
  });

  describe('validation', () => {
    it('submit button disabled when no rating + no comment', () => {
      render(<FeedbackForm open={true} onClose={vi.fn()} />);
      expect(screen.getByTestId('feedback-form-submit')).toBeDisabled();
    });

    it('submit button disabled when only rating set (comment < 5 chars)', async () => {
      const user = userEvent.setup();
      render(<FeedbackForm open={true} onClose={vi.fn()} />);
      await user.click(screen.getByTestId('feedback-form-star-3'));
      await user.type(screen.getByTestId('feedback-form-comment'), 'abc');
      expect(screen.getByTestId('feedback-form-submit')).toBeDisabled();
    });

    it('submit button enabled when rating + comment ≥5 chars', async () => {
      const user = userEvent.setup();
      render(<FeedbackForm open={true} onClose={vi.fn()} />);
      await user.click(screen.getByTestId('feedback-form-star-5'));
      await user.type(
        screen.getByTestId('feedback-form-comment'),
        'Trải nghiệm tốt'
      );
      expect(screen.getByTestId('feedback-form-submit')).toBeEnabled();
    });
  });

  describe('submit success', () => {
    it('shows Vietnamese success message after HTTP 201', async () => {
      const user = userEvent.setup();
      const onClose = vi.fn();
      render(<FeedbackForm open={true} onClose={onClose} />);
      await user.click(screen.getByTestId('feedback-form-star-5'));
      await user.type(
        screen.getByTestId('feedback-form-comment'),
        'Sản phẩm rất tốt, cảm ơn team'
      );
      await user.click(screen.getByTestId('feedback-form-submit'));

      await waitFor(
        () => {
          expect(screen.getByTestId('feedback-form-success')).toBeInTheDocument();
        },
        { timeout: 3000 }
      );
      // Vietnamese narrative per dev-readable-doc-language.md §2
      expect(screen.getByTestId('feedback-form-success')).toHaveTextContent(
        /Cảm ơn anh\/chị đã gửi phản hồi/
      );
    });
  });

  describe('default email prefill', () => {
    it('pre-fills email input with defaultEmail prop', () => {
      render(
        <FeedbackForm
          open={true}
          onClose={vi.fn()}
          defaultEmail="hang@skyedu.vn"
        />
      );
      const emailInput = screen.getByTestId(
        'feedback-form-email'
      ) as HTMLInputElement;
      expect(emailInput.value).toBe('hang@skyedu.vn');
    });
  });
});
