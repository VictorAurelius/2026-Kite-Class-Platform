/**
 * StudentForm Integration Tests
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import { StudentForm } from '../student-form';

describe('StudentForm', () => {
  it('should render all required fields', () => {
    render(<StudentForm onSubmit={vi.fn()} isSubmitting={false} />);

    // Vietnamese labels
    expect(screen.getByLabelText(/tên học viên/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/số điện thoại|phone/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/giới tính|gender/i)).toBeInTheDocument();
  });

  it('should validate required fields on submit', async () => {
    const onSubmit = vi.fn();
    render(<StudentForm onSubmit={onSubmit} isSubmitting={false} />);

    const submitButton = screen.getByRole('button', { name: /tạo|thêm|create|add/i });
    await userEvent.click(submitButton);

    await waitFor(() => {
      // Vietnamese error message
      expect(screen.getByText(/tên.*bắt buộc|name.*required/i)).toBeInTheDocument();
    });

    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('should validate email format', async () => {
    const onSubmit = vi.fn();
    render(<StudentForm onSubmit={onSubmit} isSubmitting={false} />);

    await userEvent.type(screen.getByLabelText(/email/i), 'invalid-email');

    const submitButton = screen.getByRole('button', { name: /tạo|thêm|create|add/i });
    await userEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText(/email.*không hợp lệ|invalid email/i)).toBeInTheDocument();
    });
  });

  it('should submit valid form data', async () => {
    const onSubmit = vi.fn();
    render(<StudentForm onSubmit={onSubmit} isSubmitting={false} />);

    await userEvent.type(screen.getByLabelText(/tên học viên/i), 'John Doe');
    await userEvent.type(screen.getByLabelText(/email/i), 'john@example.com');
    await userEvent.type(screen.getByLabelText(/số điện thoại|phone/i), '0901234567');

    const submitButton = screen.getByRole('button', { name: /tạo|thêm|create|add/i });
    await userEvent.click(submitButton);

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'John Doe',
          email: 'john@example.com',
          phone: '0901234567',
        })
      );
    });
  });

  it('should disable submit button when submitting', () => {
    render(<StudentForm onSubmit={vi.fn()} isSubmitting={true} />);

    const submitButton = screen.getByRole('button', { name: /đang|creating|adding/i });
    expect(submitButton).toBeDisabled();
  });

  it('should pre-fill form in edit mode', () => {
    const initialData = {
      name: 'Jane Smith',
      email: 'jane@example.com',
      phone: '0907654321',
      gender: 'FEMALE' as const,
    };

    render(<StudentForm onSubmit={vi.fn()} isSubmitting={false} initialData={initialData} />);

    expect(screen.getByDisplayValue('Jane Smith')).toBeInTheDocument();
    expect(screen.getByDisplayValue('jane@example.com')).toBeInTheDocument();
    expect(screen.getByDisplayValue('0907654321')).toBeInTheDocument();
  });
});
