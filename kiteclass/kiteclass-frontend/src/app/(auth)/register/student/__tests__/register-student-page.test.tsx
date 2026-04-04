/**
 * Tests for student registration page — date format hint.
 *
 * @since 2026-04-04
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/utils';
import StudentRegisterPage from '../page';

describe('StudentRegisterPage', () => {
  it('renders date of birth field with VN format hint', () => {
    render(<StudentRegisterPage />);

    const dateInput = screen.getByLabelText(/ngày sinh/i);
    expect(dateInput).toBeInTheDocument();

    // Format hint must be visible to guide VN users
    expect(
      screen.getByText(/ngày\/tháng\/năm/i)
    ).toBeInTheDocument();
  });
});
