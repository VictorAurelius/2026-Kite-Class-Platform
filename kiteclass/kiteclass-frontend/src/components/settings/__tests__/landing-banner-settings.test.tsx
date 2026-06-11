/**
 * Tests for the landing banner management card (GAP-826 Lớp 3).
 *
 * Covers the local list editing logic: seed from server, add-by-URL, remove, reorder,
 * and save (passes the ordered list to the update mutation).
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@/test/utils';
import userEvent from '@testing-library/user-event';

const mutate = vi.fn();
let landingData: { heroImages?: string[] } | undefined = { heroImages: ['https://h/a.webp', 'https://h/b.webp'] };

vi.mock('@/hooks/use-landing', () => ({
  useLanding: () => ({ data: landingData, isLoading: false }),
  useUpdateLanding: () => ({ mutate, isPending: false }),
}));

import { LandingBannerSettings } from '../landing-banner-settings';

describe('LandingBannerSettings (GAP-826 Lớp 3)', () => {
  beforeEach(() => {
    mutate.mockClear();
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
});
