/**
 * Tests for mobile dashboard navigation.
 * Verifies hamburger button exists and sidebar Sheet opens correctly.
 *
 * @since 2026-04-11
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import { Header } from '../header';
import { Sidebar } from '../sidebar';

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({ logout: vi.fn(), user: null }),
}));

vi.mock('next/navigation', () => ({
  usePathname: () => '/dashboard',
  useRouter: () => ({ push: vi.fn() }),
}));

describe('Header mobile navigation', () => {
  it('renders hamburger button', () => {
    render(
      <Header
        mobileSidebarOpen={false}
        onMobileSidebarToggle={vi.fn()}
        onMobileSidebarClose={vi.fn()}
      />
    );

    expect(screen.getByRole('button', { name: /Mở menu điều hướng/i })).toBeInTheDocument();
  });

  it('calls onMobileSidebarToggle when hamburger clicked', async () => {
    const user = userEvent.setup();
    const onToggle = vi.fn();

    render(
      <Header
        mobileSidebarOpen={false}
        onMobileSidebarToggle={onToggle}
        onMobileSidebarClose={vi.fn()}
      />
    );

    await user.click(screen.getByRole('button', { name: /Mở menu điều hướng/i }));
    expect(onToggle).toHaveBeenCalledOnce();
  });

  it('shows nav items in Sheet when mobileSidebarOpen is true', () => {
    render(
      <Header
        mobileSidebarOpen={true}
        onMobileSidebarToggle={vi.fn()}
        onMobileSidebarClose={vi.fn()}
      />
    );

    expect(screen.getByText('Tổng quan')).toBeInTheDocument();
    expect(screen.getByText('Học viên')).toBeInTheDocument();
    expect(screen.getByText('Cài đặt')).toBeInTheDocument();
  });
});

describe('Sidebar', () => {
  it('renders all nav items', () => {
    render(<Sidebar />);

    expect(screen.getByText('Tổng quan')).toBeInTheDocument();
    expect(screen.getByText('Học viên')).toBeInTheDocument();
    expect(screen.getByText('Giáo viên')).toBeInTheDocument();
    expect(screen.getByText('Lớp học')).toBeInTheDocument();
  });
});
