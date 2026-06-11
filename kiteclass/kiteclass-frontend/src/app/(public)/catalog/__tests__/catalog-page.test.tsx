/**
 * Tests for public catalog page — states, client filtering, and persona reco.
 *
 * @since 2026-04-04 (GAP-274 phase-2 kit port: filter + reco coverage added)
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor, within } from '@/test/utils';
import CatalogPage from '../page';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';

const CATALOG_URL = '*/api/v1/courses*';

const COURSES = [
  {
    id: 1,
    name: 'Toán lớp 4 — Nền tảng vững',
    code: 'MATH-L4-01',
    description: 'Củng cố 4 phép tính cho học sinh lớp 4.',
    level: 'Beginner',
    price: 800000,
    durationWeeks: 8,
    status: 'PUBLISHED',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 2,
    name: 'Ôn thi vào lớp 6 — Nâng cao',
    code: 'MATH-O6-01',
    description: 'Luyện đề ôn thi vào 6 cho học sinh khá giỏi.',
    level: 'Advanced',
    price: 1500000,
    durationWeeks: 12,
    status: 'PUBLISHED',
    createdAt: '2026-02-01T00:00:00Z',
    updatedAt: '2026-02-01T00:00:00Z',
  },
];

function mockCourses(content: unknown[]) {
  server.use(
    http.get(CATALOG_URL, () =>
      HttpResponse.json({
        data: { content, totalElements: content.length, totalPages: 1 },
      })
    )
  );
}

describe('CatalogPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows error state when API fails', async () => {
    server.use(http.get(CATALOG_URL, () => HttpResponse.error()));
    render(<CatalogPage />);
    await waitFor(
      () => expect(screen.getByText(/không thể tải danh sách khóa học/i)).toBeInTheDocument(),
      { timeout: 10000 }
    );
  });

  it('shows empty state when no courses returned', async () => {
    mockCourses([]);
    render(<CatalogPage />);
    await waitFor(() =>
      expect(screen.getByText(/chưa có khóa nào khớp bộ lọc/i)).toBeInTheDocument()
    );
  });

  it('shows loading spinner initially', () => {
    server.use(
      http.get(CATALOG_URL, async () => {
        await new Promise(() => {});
      })
    );
    render(<CatalogPage />);
    expect(screen.getByText(/đang tải khóa học/i)).toBeInTheDocument();
  });

  it('renders fetched courses and filters by grade level (client-side)', async () => {
    mockCourses(COURSES);
    const user = userEvent.setup();
    render(<CatalogPage />);

    const grid = () => within(screen.getByLabelText('Danh sách khóa học'));
    await waitFor(() =>
      expect(grid().getByText('Toán lớp 4 — Nền tảng vững')).toBeInTheDocument()
    );
    expect(grid().getByText('Ôn thi vào lớp 6 — Nâng cao')).toBeInTheDocument();

    // Filter to "Ôn thi vào 6" → only the matching course remains in the grid.
    await user.click(screen.getByRole('button', { name: 'Ôn thi vào 6' }));
    await waitFor(() =>
      expect(grid().queryByText('Toán lớp 4 — Nền tảng vững')).not.toBeInTheDocument()
    );
    expect(grid().getByText('Ôn thi vào lớp 6 — Nâng cao')).toBeInTheDocument();
  });

  it('renders the course cover image when seeded (GAP-1225)', async () => {
    mockCourses([
      {
        ...COURSES[0],
        coverImageUrl: '/demo-banners/co-ha-toan.webp',
      },
    ]);
    render(<CatalogPage />);
    const grid = () => within(screen.getByLabelText('Danh sách khóa học'));
    const cover = await waitFor(() => grid().getByAltText(/ảnh khóa học toán lớp 4/i));
    expect(cover).toBeInTheDocument();
    expect(cover.getAttribute('src')).toContain('co-ha-toan.webp');
  });

  it('persona recommendation maps situation to a real course', async () => {
    mockCourses(COURSES);
    const user = userEvent.setup();
    render(<CatalogPage />);

    await waitFor(() => expect(screen.getByText(/gợi ý cho con anh\/chị/i)).toBeInTheDocument());

    // "Chuẩn bị thi vào 6" → should surface the ôn-thi course in the reco panel.
    await user.click(screen.getByRole('button', { name: /chuẩn bị thi vào 6/i }));
    const recoRegion = screen.getByText(/xem khóa/i).closest('div');
    expect(recoRegion).toBeTruthy();
    await waitFor(() =>
      expect(screen.getAllByText('Ôn thi vào lớp 6 — Nâng cao').length).toBeGreaterThan(0)
    );
  });
});
