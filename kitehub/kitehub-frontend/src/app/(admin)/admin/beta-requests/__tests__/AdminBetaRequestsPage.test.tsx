/**
 * Tests for AdminBetaRequestsPage (GAP-372 Wave 33).
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@/test/test-utils';
import AdminBetaRequestsPage from '../page';

vi.mock('@/lib/api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import apiClient from '@/lib/api/client';

const mockData = {
  content: [
    {
      id: 1,
      email: 'a@x.com',
      name: 'A',
      orgName: 'AO',
      persona: 'P1_SOLO_TEACHER',
      referralSource: null,
      status: 'PENDING',
      createdAt: '2026-05-07T03:00:00Z',
      approvedAt: null,
      rejectedAt: null,
      rejectionReason: null,
    },
  ],
  page: 0,
  size: 50,
  totalElements: 1,
  totalPages: 1,
};

describe('AdminBetaRequestsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the queue with PENDING data', async () => {
    (apiClient.get as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockData });

    render(<AdminBetaRequestsPage />);

    await waitFor(() => {
      expect(screen.getByText('a@x.com')).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /^Duyệt$/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^Từ chối$/i })).toBeInTheDocument();
    expect(apiClient.get).toHaveBeenCalledWith(
      '/api/v1/admin/beta-requests',
      expect.objectContaining({ params: expect.objectContaining({ status: 'PENDING' }) }),
    );
  });

  it('renders empty-state when no requests', async () => {
    (apiClient.get as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: { content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 },
    });

    render(<AdminBetaRequestsPage />);

    await waitFor(() => {
      expect(screen.getByText(/Không có yêu cầu nào/i)).toBeInTheDocument();
    });
  });
});
