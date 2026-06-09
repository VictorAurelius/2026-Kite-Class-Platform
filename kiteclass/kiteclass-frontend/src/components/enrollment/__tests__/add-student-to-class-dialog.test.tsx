/**
 * Tests for AddStudentToClassDialog (GAP-1103) — single enroll dialog.
 *
 * Verifies: renders student options, validation blocks empty student, and the
 * happy path calls createEnrollment + shows a success toast + closes.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@/test/utils';
import { AddStudentToClassDialog } from '../add-student-to-class-dialog';
import { useStudents } from '@/hooks/use-students';
import { useCreateEnrollment } from '@/hooks/use-enrollments';
import { toast } from '@/hooks/use-toast';

vi.mock('@/hooks/use-students', () => ({ useStudents: vi.fn() }));
vi.mock('@/hooks/use-enrollments', () => ({ useCreateEnrollment: vi.fn() }));
vi.mock('@/hooks/use-toast', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/hooks/use-toast')>();
  return { ...actual, toast: vi.fn() };
});

// Mock shadcn Select to a deterministic native <select> (Radix Select pointer
// interaction is flaky in jsdom).
vi.mock('@/components/ui/select', async () => {
  const React = await import('react');
  const Ctx = React.createContext<{
    value: string;
    onValueChange: (v: string) => void;
  } | null>(null);
  return {
    Select: ({ value, onValueChange, children }: { value: string; onValueChange: (v: string) => void; children: React.ReactNode }) =>
      React.createElement(Ctx.Provider, { value: { value, onValueChange } }, children),
    SelectTrigger: ({ children, ...props }: { children: React.ReactNode }) =>
      React.createElement('div', props, children),
    SelectValue: ({ placeholder }: { placeholder?: string }) =>
      React.createElement('span', null, placeholder),
    SelectContent: ({ children }: { children: React.ReactNode }) => {
      const ctx = React.useContext(Ctx);
      return React.createElement(
        'select',
        {
          'data-testid': 'student-select',
          value: ctx?.value ?? '',
          onChange: (e: React.ChangeEvent<HTMLSelectElement>) => ctx?.onValueChange(e.target.value),
        },
        children,
      );
    },
    SelectItem: ({ value, children }: { value: string; children: React.ReactNode }) =>
      React.createElement('option', { value }, children),
  };
});

const mockMutate = vi.fn();

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(useStudents).mockReturnValue({
    data: {
      content: [
        { id: 5, name: 'Nguyễn Văn An', email: 'an@test.vn', status: 'ACTIVE' },
        { id: 6, name: 'Trần Thị Hồng', email: 'hong@test.vn', status: 'ACTIVE' },
      ],
    },
    isLoading: false,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } as any);
  vi.mocked(useCreateEnrollment).mockReturnValue({
    mutate: mockMutate,
    isPending: false,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } as any);
});

describe('AddStudentToClassDialog', () => {
  it('renders the dialog title and student options', () => {
    render(
      <AddStudentToClassDialog classId={7} open onOpenChange={vi.fn()} />,
    );

    expect(screen.getByText('Thêm học sinh vào lớp')).toBeInTheDocument();
    expect(screen.getByRole('option', { name: /Nguyễn Văn An/ })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: /Trần Thị Hồng/ })).toBeInTheDocument();
  });

  it('blocks submit and shows an error when no student is selected', () => {
    render(
      <AddStudentToClassDialog classId={7} open onOpenChange={vi.fn()} />,
    );

    fireEvent.click(screen.getByRole('button', { name: /Thêm vào lớp/ }));

    expect(mockMutate).not.toHaveBeenCalled();
    expect(toast).toHaveBeenCalledWith(
      expect.objectContaining({ description: 'Vui lòng chọn học sinh' }),
    );
  });

  it('calls createEnrollment and shows success toast on the happy path', () => {
    mockMutate.mockImplementation((_req, opts) => opts?.onSuccess?.());
    const onOpenChange = vi.fn();

    render(
      <AddStudentToClassDialog classId={7} open onOpenChange={onOpenChange} />,
    );

    fireEvent.change(screen.getByTestId('student-select'), {
      target: { value: '5' },
    });
    fireEvent.change(screen.getByLabelText(/Học phí/), {
      target: { value: '1500000' },
    });
    fireEvent.click(screen.getByRole('button', { name: /Thêm vào lớp/ }));

    expect(mockMutate).toHaveBeenCalledWith(
      {
        studentId: 5,
        classId: 7,
        tuitionAmount: 1500000,
        discountPercent: 0,
        notes: undefined,
      },
      expect.objectContaining({
        onSuccess: expect.any(Function),
        onError: expect.any(Function),
      }),
    );
    expect(toast).toHaveBeenCalledWith(
      expect.objectContaining({ title: 'Thành công' }),
    );
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });
});
