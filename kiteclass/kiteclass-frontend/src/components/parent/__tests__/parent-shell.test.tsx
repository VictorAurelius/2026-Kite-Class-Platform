/**
 * ParentShell smoke test — bottom 4-tab nav + active state + a11y semantics.
 *
 * Wave 49 Bucket A (GAP-267).
 */

import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ParentShell } from '../parent-shell';

vi.mock('next/navigation', () => ({
  usePathname: () => '/parent',
}));

describe('ParentShell', () => {
  it('renders header title + subtitle and unread badge', () => {
    render(
      <ParentShell title="Cổng phụ huynh" subtitle="Tháng 4" unreadCount={3}>
        <p>Body</p>
      </ParentShell>,
    );

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(
      'Cổng phụ huynh',
    );
    expect(screen.getByText('Tháng 4')).toBeInTheDocument();
    expect(
      screen.getByLabelText('Thông báo (3 chưa đọc)'),
    ).toBeInTheDocument();
  });

  it('renders 4 bottom-nav tabs', () => {
    render(
      <ParentShell title="x">
        <span />
      </ParentShell>,
    );
    const nav = screen.getByLabelText('Điều hướng chính');
    expect(nav).toBeInTheDocument();
    expect(screen.getByText('Trang chủ')).toBeInTheDocument();
    expect(screen.getByText('Học bạ')).toBeInTheDocument();
    expect(screen.getByText('Học phí')).toBeInTheDocument();
    expect(screen.getByText('Cài đặt')).toBeInTheDocument();
  });

  it('marks home tab active for /parent path', () => {
    render(
      <ParentShell title="x">
        <span />
      </ParentShell>,
    );
    const homeLink = screen.getByRole('link', { name: /Trang chủ/ });
    expect(homeLink).toHaveAttribute('aria-current', 'page');
  });
});
