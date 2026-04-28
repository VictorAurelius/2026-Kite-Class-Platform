/**
 * Tests for student registration page — date format hint.
 *
 * The form body is loaded via `next/dynamic`, so the test uses
 * async queries (`findByLabelText` / `findByText`) to wait for the
 * lazy chunk to resolve before asserting.
 *
 * @since 2026-04-04
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/utils';
import StudentRegisterPage from '../page';

describe('StudentRegisterPage', () => {
  it('renders date of birth field with VN format hint', async () => {
    render(<StudentRegisterPage />);

    const dateInput = await screen.findByLabelText(/ngày sinh/i);
    expect(dateInput).toBeInTheDocument();

    // Format hint must be visible to guide VN users
    expect(
      await screen.findByText(/ngày\/tháng\/năm/i)
    ).toBeInTheDocument();
  });
});
