/**
 * TodayClassesWidget component tests.
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { TodayClassesWidget } from '../today-classes-widget';
import { mockTodayClassSessions } from '@/__tests__/fixtures/attendance';

// Mock Next.js Link
vi.mock('next/link', () => ({
  default: ({ children, href }: { children: React.ReactNode; href: string }) => (
    <a href={href}>{children}</a>
  ),
}));

describe('TodayClassesWidget', () => {
  describe('Loading State', () => {
    it('renders loading skeleton when isLoading is true', () => {
      render(<TodayClassesWidget sessions={[]} isLoading={true} />);

      expect(screen.getByText('Đang tải...')).toBeInTheDocument();
      // Check for skeleton loaders
      const skeletons = screen.getAllByRole('generic').filter((el) =>
        el.className.includes('animate-pulse')
      );
      expect(skeletons.length).toBeGreaterThan(0);
    });

    it('does not show sessions while loading', () => {
      render(
        <TodayClassesWidget sessions={mockTodayClassSessions} isLoading={true} />
      );

      expect(screen.queryByText('Toán Lớp 10A')).not.toBeInTheDocument();
    });
  });

  describe('Empty State', () => {
    it('shows empty state when no sessions', () => {
      render(<TodayClassesWidget sessions={[]} isLoading={false} />);

      expect(screen.getByText('Không có lớp học nào hôm nay')).toBeInTheDocument();
    });

    it('shows calendar emoji in empty state', () => {
      render(<TodayClassesWidget sessions={[]} isLoading={false} />);

      expect(screen.getByText('📅')).toBeInTheDocument();
    });
  });

  describe('Session List', () => {
    it('renders all sessions', () => {
      render(
        <TodayClassesWidget sessions={mockTodayClassSessions} isLoading={false} />
      );

      expect(screen.getByText('Toán Lớp 10A')).toBeInTheDocument();
      expect(screen.getByText('Lý Lớp 11B')).toBeInTheDocument();
      expect(screen.getByText('Hóa Lớp 12C')).toBeInTheDocument();
    });

    it('displays session numbers', () => {
      render(
        <TodayClassesWidget sessions={mockTodayClassSessions} isLoading={false} />
      );

      expect(screen.getByText('Buổi 5')).toBeInTheDocument();
      expect(screen.getByText('Buổi 3')).toBeInTheDocument();
      expect(screen.getByText('Buổi 8')).toBeInTheDocument();
    });

    it('displays student counts', () => {
      render(
        <TodayClassesWidget sessions={mockTodayClassSessions} isLoading={false} />
      );

      expect(screen.getByText('30 học viên')).toBeInTheDocument();
      expect(screen.getByText('25 học viên')).toBeInTheDocument();
      expect(screen.getByText('28 học viên')).toBeInTheDocument();
    });

    it('displays session times', () => {
      render(
        <TodayClassesWidget sessions={mockTodayClassSessions} isLoading={false} />
      );

      // Times should be formatted (specific format depends on locale)
      const timeElements = screen.getAllByText(/\d{2}:\d{2}/);
      expect(timeElements.length).toBeGreaterThan(0);
    });
  });

  describe('Pending Badge', () => {
    it('shows pending badge when there are unmarked sessions', () => {
      render(
        <TodayClassesWidget sessions={mockTodayClassSessions} isLoading={false} />
      );

      // 2 sessions are not marked
      expect(screen.getByText(/Chưa điểm danh: 2/)).toBeInTheDocument();
    });

    it('does not show pending badge when all sessions are marked', () => {
      const allMarked = mockTodayClassSessions.map((s) => ({
        ...s,
        attendanceMarked: true,
      }));

      render(<TodayClassesWidget sessions={allMarked} isLoading={false} />);

      expect(screen.queryByText(/Chưa điểm danh/)).not.toBeInTheDocument();
    });

    it('shows correct pending count', () => {
      const sessions = [
        { ...mockTodayClassSessions[0], attendanceMarked: false },
        { ...mockTodayClassSessions[1], attendanceMarked: true },
        { ...mockTodayClassSessions[2], attendanceMarked: false },
      ];

      render(<TodayClassesWidget sessions={sessions} isLoading={false} />);

      expect(screen.getByText(/Chưa điểm danh: 2/)).toBeInTheDocument();
    });
  });

  describe('Attendance Status', () => {
    it('shows "Đã điểm danh" for marked sessions', () => {
      render(
        <TodayClassesWidget sessions={mockTodayClassSessions} isLoading={false} />
      );

      const markedTexts = screen.getAllByText(/Đã điểm danh/);
      expect(markedTexts.length).toBe(1); // Only one session is marked
    });

    it('shows "Chưa điểm danh" for unmarked sessions', () => {
      render(
        <TodayClassesWidget sessions={mockTodayClassSessions} isLoading={false} />
      );

      const unmarkedTexts = screen.getAllByText(/^Chưa điểm danh$/);
      expect(unmarkedTexts.length).toBe(2); // Two sessions not marked
    });

    it('shows attendance counts for marked sessions', () => {
      render(
        <TodayClassesWidget sessions={mockTodayClassSessions} isLoading={false} />
      );

      // Session 2 has 23/25 attendance
      expect(screen.getByText(/23\/25/)).toBeInTheDocument();
    });
  });

  describe('Action Buttons', () => {
    it('shows "Điểm danh" button for unmarked sessions', () => {
      render(
        <TodayClassesWidget sessions={mockTodayClassSessions} isLoading={false} />
      );

      const markButtons = screen.getAllByRole('button', { name: /Điểm danh/ });
      expect(markButtons.length).toBe(2); // Two unmarked sessions
    });

    it('shows "Xem" button for marked sessions', () => {
      render(
        <TodayClassesWidget sessions={mockTodayClassSessions} isLoading={false} />
      );

      const viewButtons = screen.getAllByRole('button', { name: /Xem/ });
      expect(viewButtons.length).toBe(1); // One marked session
    });

    it('links to correct attendance page', () => {
      const { container } = render(
        <TodayClassesWidget sessions={mockTodayClassSessions} isLoading={false} />
      );

      // Check for correct href
      const link = container.querySelector(
        'a[href*="/classes/1/attendance?session=1"]'
      );
      expect(link).toBeInTheDocument();
    });
  });

  describe('Session Counts', () => {
    it('displays total session count in header', () => {
      render(
        <TodayClassesWidget sessions={mockTodayClassSessions} isLoading={false} />
      );

      expect(screen.getByText(/Lớp học hôm nay \(3\)/)).toBeInTheDocument();
    });

    it('updates count when sessions change', () => {
      const { rerender } = render(
        <TodayClassesWidget sessions={[mockTodayClassSessions[0]]} isLoading={false} />
      );

      expect(screen.getByText(/Lớp học hôm nay \(1\)/)).toBeInTheDocument();

      rerender(
        <TodayClassesWidget sessions={mockTodayClassSessions} isLoading={false} />
      );

      expect(screen.getByText(/Lớp học hôm nay \(3\)/)).toBeInTheDocument();
    });
  });
});
