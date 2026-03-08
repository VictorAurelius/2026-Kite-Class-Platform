/**
 * FormTextarea Component Tests
 *
 * @author KiteClass Team
 * @since 3.8.0
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/utils';
import { FormTextarea } from '../form-textarea';

describe('FormTextarea', () => {
  it('should render textarea with label', () => {
    render(<FormTextarea label="Mô tả" />);

    expect(screen.getByLabelText(/mô tả/i)).toBeInTheDocument();
  });

  it('should show required asterisk when required', () => {
    render(<FormTextarea label="Description" required />);

    const label = screen.getByText(/description/i);
    expect(label.textContent).toContain('*');
  });

  it('should display error message', () => {
    render(<FormTextarea label="Description" error="Mô tả không được để trống" />);

    expect(screen.getByText('Mô tả không được để trống')).toBeInTheDocument();
    expect(screen.getByText('Mô tả không được để trống')).toHaveClass('text-destructive');
  });

  it('should display helper text when no error', () => {
    render(<FormTextarea label="Notes" helperText="Tối đa 500 ký tự" />);

    expect(screen.getByText('Tối đa 500 ký tự')).toBeInTheDocument();
    expect(screen.getByText('Tối đa 500 ký tự')).toHaveClass('text-muted-foreground');
  });

  it('should not show helper text when error exists', () => {
    render(
      <FormTextarea
        label="Notes"
        error="Quá dài"
        helperText="Tối đa 500 ký tự"
      />
    );

    expect(screen.getByText('Quá dài')).toBeInTheDocument();
    expect(screen.queryByText('Tối đa 500 ký tự')).not.toBeInTheDocument();
  });

  it('should pass through HTML textarea props', () => {
    render(
      <FormTextarea
        label="Notes"
        rows={5}
        maxLength={500}
        placeholder="Nhập ghi chú"
      />
    );

    const textarea = screen.getByLabelText(/notes/i);
    expect(textarea).toHaveAttribute('rows', '5');
    expect(textarea).toHaveAttribute('maxlength', '500');
    expect(textarea).toHaveAttribute('placeholder', 'Nhập ghi chú');
  });

  it('should support rows prop', () => {
    const { rerender } = render(<FormTextarea label="Notes" rows={3} />);
    expect(screen.getByLabelText(/notes/i)).toHaveAttribute('rows', '3');

    rerender(<FormTextarea label="Notes" rows={10} />);
    expect(screen.getByLabelText(/notes/i)).toHaveAttribute('rows', '10');
  });

  it('should apply custom className', () => {
    const { container } = render(<FormTextarea label="Notes" className="custom-textarea" />);

    expect(container.querySelector('.custom-textarea')).toBeInTheDocument();
  });

  it('should set aria-invalid when error exists', () => {
    render(<FormTextarea label="Notes" error="Invalid input" />);

    const textarea = screen.getByLabelText(/notes/i);
    expect(textarea).toHaveAttribute('aria-invalid', 'true');
  });

  it('should disable textarea when disabled prop is true', () => {
    render(<FormTextarea label="Notes" disabled />);

    expect(screen.getByLabelText(/notes/i)).toBeDisabled();
  });
});
