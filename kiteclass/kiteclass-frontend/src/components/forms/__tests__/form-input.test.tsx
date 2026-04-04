/**
 * FormInput Component Tests
 *
 * @author KiteClass Team
 * @since 3.8.0
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/utils';
import { FormInput } from '../form-input';

describe('FormInput', () => {
  it('should render input with label', () => {
    render(<FormInput label="Tên học viên" />);

    expect(screen.getByLabelText(/tên học viên/i)).toBeInTheDocument();
  });

  it('should show required asterisk when required', () => {
    render(<FormInput label="Email" required />);

    const label = screen.getByText(/email/i);
    expect(label.textContent).toContain('*');
  });

  it('should display error message', () => {
    render(<FormInput label="Email" error="Email không hợp lệ" />);

    expect(screen.getByText('Email không hợp lệ')).toBeInTheDocument();
    expect(screen.getByText('Email không hợp lệ')).toHaveClass('text-destructive');
  });

  it('should display helper text when no error', () => {
    render(<FormInput label="Password" helperText="Tối thiểu 8 ký tự" />);

    expect(screen.getByText('Tối thiểu 8 ký tự')).toBeInTheDocument();
    expect(screen.getByText('Tối thiểu 8 ký tự')).toHaveClass('text-muted-foreground');
  });

  it('should not show helper text when error exists', () => {
    render(
      <FormInput
        label="Password"
        error="Password quá ngắn"
        helperText="Tối thiểu 8 ký tự"
      />
    );

    expect(screen.getByText('Password quá ngắn')).toBeInTheDocument();
    expect(screen.queryByText('Tối thiểu 8 ký tự')).not.toBeInTheDocument();
  });

  it('should pass through HTML input props', () => {
    render(
      <FormInput
        label="Age"
        type="number"
        min={0}
        max={100}
        placeholder="Nhập tuổi"
      />
    );

    const input = screen.getByLabelText(/age/i);
    expect(input).toHaveAttribute('type', 'number');
    expect(input).toHaveAttribute('min', '0');
    expect(input).toHaveAttribute('max', '100');
    expect(input).toHaveAttribute('placeholder', 'Nhập tuổi');
  });

  it('should support different input types', () => {
    const { rerender } = render(<FormInput label="Email" type="email" />);
    expect(screen.getByLabelText(/email/i)).toHaveAttribute('type', 'email');

    rerender(<FormInput label="Password" type="password" />);
    expect(screen.getByLabelText(/password/i)).toHaveAttribute('type', 'password');

    rerender(<FormInput label="Phone" type="tel" />);
    expect(screen.getByLabelText(/phone/i)).toHaveAttribute('type', 'tel');
  });

  it('should apply custom className', () => {
    const { container } = render(<FormInput label="Name" className="custom-input" />);

    expect(container.querySelector('.custom-input')).toBeInTheDocument();
  });

  it('should set aria-invalid when error exists', () => {
    render(<FormInput label="Email" error="Invalid email" />);

    const input = screen.getByLabelText(/email/i);
    expect(input).toHaveAttribute('aria-invalid', 'true');
  });

  it('should have aria-live on error message for screen readers', () => {
    render(<FormInput label="Email" error="Email không hợp lệ" />);

    const errorEl = screen.getByText('Email không hợp lệ');
    expect(errorEl).toHaveAttribute('aria-live', 'polite');
  });

  it('should disable input when disabled prop is true', () => {
    render(<FormInput label="Name" disabled />);

    expect(screen.getByLabelText(/name/i)).toBeDisabled();
  });
});
