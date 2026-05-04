/**
 * Tests for PeriodBulkActions (Phase 1B v1, GAP-323b).
 *
 * @since 4.x.x (Wave 18b2 Bucket A)
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { PeriodBulkActions } from '../period-bulk-actions';

describe('PeriodBulkActions', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders "Mark all present" + "Reset" + "Save" buttons', () => {
    render(
      <PeriodBulkActions
        onMarkAllPresent={vi.fn()}
        onReset={vi.fn()}
        onSave={vi.fn()}
      />,
    );

    expect(
      screen.getByRole('button', { name: /Đánh dấu tất cả có mặt/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /Xoá lựa chọn/i }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Lưu/i })).toBeInTheDocument();
  });

  it('fires onMarkAllPresent when "Mark all present" clicked', () => {
    const onMarkAllPresent = vi.fn();
    render(
      <PeriodBulkActions
        onMarkAllPresent={onMarkAllPresent}
        onReset={vi.fn()}
        onSave={vi.fn()}
      />,
    );

    fireEvent.click(
      screen.getByRole('button', { name: /Đánh dấu tất cả có mặt/i }),
    );
    expect(onMarkAllPresent).toHaveBeenCalledTimes(1);
  });

  it('fires onReset when reset clicked', () => {
    const onReset = vi.fn();
    render(
      <PeriodBulkActions
        onMarkAllPresent={vi.fn()}
        onReset={onReset}
        onSave={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: /Xoá lựa chọn/i }));
    expect(onReset).toHaveBeenCalledTimes(1);
  });

  it('fires onSave when save clicked', () => {
    const onSave = vi.fn();
    render(
      <PeriodBulkActions
        onMarkAllPresent={vi.fn()}
        onReset={vi.fn()}
        onSave={onSave}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: /Lưu/i }));
    expect(onSave).toHaveBeenCalledTimes(1);
  });

  it('disables Save while saving', () => {
    render(
      <PeriodBulkActions
        onMarkAllPresent={vi.fn()}
        onReset={vi.fn()}
        onSave={vi.fn()}
        isSaving={true}
      />,
    );

    const saveBtn = screen.getByRole('button', { name: /Đang lưu/i });
    expect(saveBtn).toBeDisabled();
  });
});
