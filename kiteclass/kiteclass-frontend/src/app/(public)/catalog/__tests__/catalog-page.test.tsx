/**
 * Tests for public catalog page — error/empty state and retry behavior.
 *
 * @since 2026-04-04
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@/test/utils';
import CoursesPage from '../page';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';

const CATALOG_URL = '*/api/v1/courses*';

describe('CatalogPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows error state when API fails', async () => {
    server.use(
      http.get(CATALOG_URL, () => HttpResponse.error())
    );

    render(<CoursesPage />);

    await waitFor(
      () => {
        expect(
          screen.getByText(/không thể tải danh sách khóa học/i)
        ).toBeInTheDocument();
      },
      { timeout: 10000 }
    );
  });

  it('shows empty state when no courses returned', async () => {
    server.use(
      http.get(CATALOG_URL, () =>
        HttpResponse.json({
          data: { content: [], totalElements: 0, totalPages: 0 },
        })
      )
    );

    render(<CoursesPage />);

    await waitFor(() => {
      expect(
        screen.getByText(/không tìm thấy khóa học nào phù hợp/i)
      ).toBeInTheDocument();
    });
  });

  it('shows loading spinner initially', () => {
    server.use(
      http.get(CATALOG_URL, async () => {
        await new Promise(() => {}); // never resolves
      })
    );

    render(<CoursesPage />);

    expect(screen.getByText(/đang tải khóa học/i)).toBeInTheDocument();
  });
});
