/**
 * Tests for DashboardWelcome onboarding component.
 *
 * @author KiteClass Team
 * @since 3.15.0
 */

import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { DashboardWelcome } from '../DashboardWelcome';

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: vi.fn((key: string) => store[key] ?? null),
    setItem: vi.fn((key: string, value: string) => {
      store[key] = value;
    }),
    clear: () => {
      store = {};
    },
  };
})();

Object.defineProperty(window, 'localStorage', { value: localStorageMock });

describe('DashboardWelcome', () => {
  beforeEach(() => {
    localStorageMock.clear();
    localStorageMock.getItem.mockClear();
    localStorageMock.setItem.mockClear();
  });

  it('renders welcome banner for first-time users', () => {
    render(<DashboardWelcome />);

    expect(screen.getByText(/Chào mừng đến với KiteClass/)).toBeInTheDocument();
    expect(screen.getByText(/Bắt đầu với các bước sau/)).toBeInTheDocument();
  });

  it('renders quick action links', () => {
    render(<DashboardWelcome />);

    expect(screen.getByRole('link', { name: /Thêm học sinh/ })).toHaveAttribute('href', '/students');
    expect(screen.getByRole('link', { name: /Thêm giáo viên/ })).toHaveAttribute('href', '/teachers');
    expect(screen.getByRole('link', { name: /Tạo khóa học/ })).toHaveAttribute('href', '/courses');
  });

  it('dismisses banner and persists to localStorage', () => {
    render(<DashboardWelcome />);

    const dismissButton = screen.getByRole('button', { name: /Ẩn hướng dẫn/ });
    fireEvent.click(dismissButton);

    expect(localStorageMock.setItem).toHaveBeenCalledWith('kiteclass-welcome-dismissed', 'true');
    expect(screen.queryByText(/Chào mừng đến với KiteClass/)).not.toBeInTheDocument();
  });

  it('does not render when previously dismissed', () => {
    localStorageMock.getItem.mockReturnValue('true');

    render(<DashboardWelcome />);

    expect(screen.queryByText(/Chào mừng đến với KiteClass/)).not.toBeInTheDocument();
  });

  it('checks localStorage on mount', () => {
    render(<DashboardWelcome />);

    expect(localStorageMock.getItem).toHaveBeenCalledWith('kiteclass-welcome-dismissed');
  });
});
