/**
 * ClassForm Integration Tests
 *
 * @author KiteClass Team
 * @since 3.8.0
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/utils';
import { ClassForm } from '../class-form';

describe('ClassForm', () => {
  it('should render all required fields', () => {
    render(<ClassForm onSubmit={vi.fn()} isSubmitting={false} />);

    // Basic info
    expect(screen.getByLabelText(/tên lớp học/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/sĩ số tối đa/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/mô tả/i)).toBeInTheDocument();

    // Schedule & Location
    expect(screen.getByLabelText(/lịch học/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/ngày bắt đầu/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/ngày kết thúc/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/loại địa điểm/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/chi tiết địa điểm/i)).toBeInTheDocument();
  });

  it('should have submit button with correct label', () => {
    render(<ClassForm onSubmit={vi.fn()} isSubmitting={false} />);

    const submitButton = screen.getByRole('button', { name: /tạo lớp học/i });
    expect(submitButton).toBeInTheDocument();
    expect(submitButton).not.toBeDisabled();
  });

  it('should disable submit button when submitting', () => {
    render(<ClassForm onSubmit={vi.fn()} isSubmitting={true} />);

    const submitButton = screen.getByRole('button', { name: /đang tạo/i });
    expect(submitButton).toBeDisabled();
  });

  it('should pre-fill form in edit mode', () => {
    const initialData = {
      name: 'Test Class',
      maxStudents: 25,
      description: 'Test description',
      schedule: 'Mon, Wed, Fri: 18:00-20:00',
      locationDetail: 'Room A101',
      locationType: 'IN_PERSON' as const,
    };

    render(
      <ClassForm
        onSubmit={vi.fn()}
        isSubmitting={false}
        initialData={initialData}
        isEditing={true}
      />
    );

    expect(screen.getByDisplayValue('Test Class')).toBeInTheDocument();
    expect(screen.getByDisplayValue('25')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Test description')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Mon, Wed, Fri: 18:00-20:00')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Room A101')).toBeInTheDocument();
  });

  it('should allow all fields to be edited in DRAFT status', () => {
    render(
      <ClassForm
        onSubmit={vi.fn()}
        isSubmitting={false}
        isEditing={true}
        classStatus={'DRAFT'}
        initialData={{ name: 'Test', maxStudents: 20, locationType: 'IN_PERSON' }}
      />
    );

    // All fields should be enabled in DRAFT mode
    const nameInput = screen.getByLabelText(/tên lớp học/i);
    const maxStudentsInput = screen.getByLabelText(/sĩ số tối đa/i);
    const descriptionTextarea = screen.getByLabelText(/mô tả/i);
    const locationDetailInput = screen.getByLabelText(/chi tiết địa điểm/i);

    expect(nameInput).not.toBeDisabled();
    expect(maxStudentsInput).not.toBeDisabled();
    expect(descriptionTextarea).not.toBeDisabled();
    expect(locationDetailInput).not.toBeDisabled();
  });

  it('should allow all fields to be edited in SCHEDULED status', () => {
    render(
      <ClassForm
        onSubmit={vi.fn()}
        isSubmitting={false}
        isEditing={true}
        classStatus={'SCHEDULED'}
        initialData={{ name: 'Test', maxStudents: 20, locationType: 'IN_PERSON' }}
      />
    );

    // All fields should be enabled in SCHEDULED mode
    const nameInput = screen.getByLabelText(/tên lớp học/i);
    const maxStudentsInput = screen.getByLabelText(/sĩ số tối đa/i);
    const scheduleInput = screen.getByLabelText(/lịch học/i);

    expect(nameInput).not.toBeDisabled();
    expect(maxStudentsInput).not.toBeDisabled();
    expect(scheduleInput).not.toBeDisabled();
  });

  it('should show warning banner and restrict fields when status is IN_PROGRESS', () => {
    render(
      <ClassForm
        onSubmit={vi.fn()}
        isSubmitting={false}
        isEditing={true}
        classStatus={'IN_PROGRESS'}
        initialData={{ name: 'Test', maxStudents: 20, locationType: 'IN_PERSON' }}
      />
    );

    // Warning banner
    expect(screen.getByText(/lớp học đang diễn ra/i)).toBeInTheDocument();

    // These fields should be disabled
    const nameInput = screen.getByLabelText(/tên lớp học/i);
    const maxStudentsInput = screen.getByLabelText(/sĩ số tối đa/i);
    const scheduleInput = screen.getByLabelText(/lịch học/i);

    expect(nameInput).toBeDisabled();
    expect(maxStudentsInput).toBeDisabled();
    expect(scheduleInput).toBeDisabled();

    // These fields should be enabled (editable in IN_PROGRESS)
    const descriptionTextarea = screen.getByLabelText(/mô tả/i);
    const locationDetailInput = screen.getByLabelText(/chi tiết địa điểm/i);

    expect(descriptionTextarea).not.toBeDisabled();
    expect(locationDetailInput).not.toBeDisabled();
  });

  it('should show completed banner and make all fields read-only when status is COMPLETED', () => {
    render(
      <ClassForm
        onSubmit={vi.fn()}
        isSubmitting={false}
        isEditing={true}
        classStatus={'COMPLETED'}
        initialData={{ name: 'Test', maxStudents: 20, locationType: 'IN_PERSON' }}
      />
    );

    // Completed banner
    expect(screen.getByText(/lớp học đã hoàn thành/i)).toBeInTheDocument();

    // All fields should be disabled
    const nameInput = screen.getByLabelText(/tên lớp học/i);
    const maxStudentsInput = screen.getByLabelText(/sĩ số tối đa/i);
    const descriptionTextarea = screen.getByLabelText(/mô tả/i);
    const locationDetailInput = screen.getByLabelText(/chi tiết địa điểm/i);

    expect(nameInput).toBeDisabled();
    expect(maxStudentsInput).toBeDisabled();
    expect(descriptionTextarea).toBeDisabled();
    expect(locationDetailInput).toBeDisabled();

    // Submit button should not exist
    expect(screen.queryByRole('button', { name: /cập nhật|tạo/i })).not.toBeInTheDocument();
  });

  it('should show cancelled banner and make all fields read-only when status is CANCELLED', () => {
    render(
      <ClassForm
        onSubmit={vi.fn()}
        isSubmitting={false}
        isEditing={true}
        classStatus={'CANCELLED'}
        initialData={{ name: 'Test', maxStudents: 20, locationType: 'IN_PERSON' }}
      />
    );

    // Cancelled banner
    expect(screen.getByText(/lớp học đã bị hủy/i)).toBeInTheDocument();

    // All fields should be disabled
    const nameInput = screen.getByLabelText(/tên lớp học/i);
    const descriptionTextarea = screen.getByLabelText(/mô tả/i);

    expect(nameInput).toBeDisabled();
    expect(descriptionTextarea).toBeDisabled();

    // Submit button should not exist
    expect(screen.queryByRole('button', { name: /cập nhật|tạo/i })).not.toBeInTheDocument();
  });
});
