/**
 * TeacherForm Integration Tests
 *
 * @author KiteClass Team
 * @since 3.8.0
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import { TeacherForm } from '../teacher-form';
import { TeacherStatus } from '@/types/auth';

describe('TeacherForm', () => {
  it('should render all required fields', () => {
    render(<TeacherForm onSubmit={vi.fn()} isSubmitting={false} />);

    // Required fields
    expect(screen.getByLabelText(/tên giáo viên/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();

    // Optional fields
    expect(screen.getByLabelText(/số điện thoại/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/chuyên môn/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/bằng cấp|chứng chỉ/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/số năm kinh nghiệm/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/giới thiệu/i)).toBeInTheDocument();
  });

  it('should submit valid form data', async () => {
    const onSubmit = vi.fn();
    render(<TeacherForm onSubmit={onSubmit} isSubmitting={false} />);

    await userEvent.type(screen.getByLabelText(/tên giáo viên/i), 'Nguyễn Văn A');
    await userEvent.type(screen.getByLabelText(/email/i), 'teacher@example.com');
    await userEvent.type(screen.getByLabelText(/số điện thoại/i), '0912345678');
    await userEvent.type(screen.getByLabelText(/chuyên môn/i), 'Toán học');

    const submitButton = screen.getByRole('button', { name: /tạo mới/i });
    await userEvent.click(submitButton);

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalled();
    });

    // Verify the submitted data contains our fields
    const submittedData = onSubmit.mock.calls[0][0];
    expect(submittedData).toMatchObject({
      name: 'Nguyễn Văn A',
      email: 'teacher@example.com',
      phoneNumber: '0912345678',
      specialization: 'Toán học',
    });
  });

  it('should disable submit button when submitting', () => {
    render(<TeacherForm onSubmit={vi.fn()} isSubmitting={true} />);

    const submitButton = screen.getByRole('button', { name: /đang tạo/i });
    expect(submitButton).toBeDisabled();
  });

  it('should pre-fill form in edit mode', () => {
    const initialData = {
      name: 'Jane Smith',
      email: 'jane@example.com',
      phoneNumber: '0987654321',
      specialization: 'Tiếng Anh',
      bio: 'Experienced teacher',
      qualification: 'Master of Education',
      experienceYears: 10,
    };

    render(
      <TeacherForm
        onSubmit={vi.fn()}
        isSubmitting={false}
        initialData={initialData}
        isEditing={true}
      />
    );

    expect(screen.getByDisplayValue('Jane Smith')).toBeInTheDocument();
    expect(screen.getByDisplayValue('jane@example.com')).toBeInTheDocument();
    expect(screen.getByDisplayValue('0987654321')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Tiếng Anh')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Experienced teacher')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Master of Education')).toBeInTheDocument();
    expect(screen.getByDisplayValue('10')).toBeInTheDocument();
  });

  it('should show status selector in edit mode', () => {
    const initialData = {
      name: 'John Doe',
      email: 'john@example.com',
      status: TeacherStatus.ACTIVE,
    };

    // First verify status field is NOT visible in create mode
    const { unmount: unmountCreate } = render(
      <TeacherForm onSubmit={vi.fn()} isSubmitting={false} />
    );

    expect(screen.queryByLabelText(/trạng thái/i)).not.toBeInTheDocument();

    unmountCreate();

    // Then verify status field IS visible in edit mode
    render(
      <TeacherForm
        onSubmit={vi.fn()}
        isSubmitting={false}
        initialData={initialData}
        isEditing={true}
      />
    );

    expect(screen.getByLabelText(/trạng thái/i)).toBeInTheDocument();
  });
});
