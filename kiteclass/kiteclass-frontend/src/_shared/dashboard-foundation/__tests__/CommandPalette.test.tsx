/**
 * CommandPalette tests — fuzzy match + activation paths.
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { CommandPalette } from '../CommandPalette';
import type { DashboardCommand } from '../types';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
  usePathname: () => '/',
}));

const COMMANDS: DashboardCommand[] = [
  { id: 'home', label: 'Tổng quan', section: 'Điều hướng', href: '/' },
  { id: 'students', label: 'Học viên', section: 'Điều hướng', href: '/students' },
  { id: 'create', label: 'Tạo lớp mới', section: 'Hành động', onSelect: vi.fn() },
];

describe('CommandPalette', () => {
  it('renders all commands when open with empty query', () => {
    render(
      <CommandPalette open onOpenChange={() => {}} commands={COMMANDS} />,
    );
    expect(screen.getByText('Tổng quan')).toBeInTheDocument();
    expect(screen.getByText('Học viên')).toBeInTheDocument();
    expect(screen.getByText('Tạo lớp mới')).toBeInTheDocument();
  });

  it('filters commands by fuzzy substring match', () => {
    render(
      <CommandPalette open onOpenChange={() => {}} commands={COMMANDS} />,
    );
    const input = screen.getByTestId('command-palette-input') as HTMLInputElement;
    fireEvent.change(input, { target: { value: 'học' } });
    expect(screen.getByText('Học viên')).toBeInTheDocument();
    expect(screen.queryByText('Tổng quan')).not.toBeInTheDocument();
  });

  it('invokes onSelect when an action command is clicked', () => {
    const onOpenChange = vi.fn();
    const onSelect = vi.fn();
    const commands: DashboardCommand[] = [
      { id: 'create', label: 'Tạo lớp mới', section: 'Hành động', onSelect },
    ];
    render(
      <CommandPalette open onOpenChange={onOpenChange} commands={commands} />,
    );
    fireEvent.click(screen.getByText('Tạo lớp mới'));
    expect(onSelect).toHaveBeenCalledTimes(1);
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it('shows empty state when no command matches', () => {
    render(
      <CommandPalette open onOpenChange={() => {}} commands={COMMANDS} />,
    );
    const input = screen.getByTestId('command-palette-input') as HTMLInputElement;
    fireEvent.change(input, { target: { value: 'zzzznotfound' } });
    expect(screen.getByText(/Không tìm thấy/)).toBeInTheDocument();
  });
});
