/**
 * Tests for the landing banner management card (GAP-826 Lớp 3).
 *
 * Covers the local list editing logic: seed from server, add-by-URL, remove, reorder,
 * and save (passes the ordered list to the update mutation).
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@/test/utils';
import userEvent from '@testing-library/user-event';

const mutate = vi.fn();
let landingData: { heroImages?: string[] } | undefined = { heroImages: ['https://h/a.webp', 'https://h/b.webp'] };

// GAP-1211: mock the banner upload API so the file-picker tests don't hit the network.
const uploadBanner = vi.hoisted(() => vi.fn());

vi.mock('@/hooks/use-landing', () => ({
  useLanding: () => ({ data: landingData, isLoading: false }),
  useUpdateLanding: () => ({ mutate, isPending: false }),
}));

vi.mock('@/lib/api/branding', () => ({
  brandingApi: { uploadBanner },
}));

import { LandingBannerSettings } from '../landing-banner-settings';

describe('LandingBannerSettings (GAP-826 Lớp 3)', () => {
  beforeEach(() => {
    mutate.mockClear();
    uploadBanner.mockReset();
    landingData = { heroImages: ['https://h/a.webp', 'https://h/b.webp'] };
  });

  it('seeds the list from the server landing heroImages', () => {
    render(<LandingBannerSettings />);
    expect(screen.getByText('https://h/a.webp')).toBeInTheDocument();
    expect(screen.getByText('https://h/b.webp')).toBeInTheDocument();
  });

  it('adds a banner by URL', async () => {
    const user = userEvent.setup();
    render(<LandingBannerSettings />);
    await user.type(screen.getByLabelText(/URL banner mới/i), 'https://h/c.webp');
    await user.click(screen.getByRole('button', { name: /thêm/i }));
    expect(screen.getByText('https://h/c.webp')).toBeInTheDocument();
  });

  it('removes a banner', async () => {
    const user = userEvent.setup();
    render(<LandingBannerSettings />);
    await user.click(screen.getByRole('button', { name: /xóa banner 1/i }));
    expect(screen.queryByText('https://h/a.webp')).not.toBeInTheDocument();
    expect(screen.getByText('https://h/b.webp')).toBeInTheDocument();
  });

  it('saves the ordered list after a reorder', async () => {
    const user = userEvent.setup();
    render(<LandingBannerSettings />);
    // Move banner 2 up → order becomes [b, a].
    await user.click(screen.getByRole('button', { name: /di chuyển banner 2 lên/i }));
    await user.click(screen.getByRole('button', { name: /lưu banner/i }));
    expect(mutate).toHaveBeenCalledWith({ heroImages: ['https://h/b.webp', 'https://h/a.webp'] });
  });

  it('appends the uploaded banner URL to the list on success (GAP-1211)', async () => {
    const url = 'https://minio.local/kite-branding-assets/static/t/banner/abc.png?sig=x';
    uploadBanner.mockResolvedValue({ url });
    render(<LandingBannerSettings />);

    const input = screen.getByLabelText(/Chọn ảnh banner để tải lên/i);
    const file = new File(['fake'], 'banner.png', { type: 'image/png' });
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => expect(screen.getByText(url)).toBeInTheDocument());
    expect(uploadBanner).toHaveBeenCalledWith(file);
  });

  it('shows an error message when the upload is rejected (GAP-1211)', async () => {
    uploadBanner.mockRejectedValue({
      response: { data: { message: 'Định dạng ảnh không được hỗ trợ' } },
    });
    render(<LandingBannerSettings />);

    const input = screen.getByLabelText(/Chọn ảnh banner để tải lên/i);
    const file = new File(['fake'], 'banner.txt', { type: 'text/plain' });
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(/Định dạng ảnh không được hỗ trợ/i)
    );
  });
});
