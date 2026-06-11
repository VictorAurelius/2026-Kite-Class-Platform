/**
 * Tests for the hero banner carousel (GAP-826).
 *
 * Contract:
 * - ≥2 banner images (slots.images) → rotating carousel: dots (tablist) + prev/next arrows.
 * - exactly 1 image → static single banner, NO carousel chrome.
 * - no images array → falls back to the legacy single `image` slot (heroImageUrl) → static.
 * - manual nav (dots / arrows) changes the selected slide.
 *
 * @since wave landing (GAP-826)
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import { HeroSection } from '../HeroSection';
import { HeroBannerCarousel } from '../HeroBannerCarousel';

const TWO = ['/demo-banners/a.webp', '/demo-banners/b.webp'];

describe('HeroSection — banner carousel (GAP-826)', () => {
  it('renders the carousel chrome (dots + arrows) when given ≥2 images', () => {
    render(<HeroSection slots={{ title: 'Trung tâm A', images: TWO }} />);

    // Carousel region present.
    expect(screen.getByRole('group', { name: /banner/i })).toBeInTheDocument();
    // One dot (tab) per slide.
    expect(screen.getAllByRole('tab')).toHaveLength(2);
    // Prev/next arrows.
    expect(screen.getByRole('button', { name: /ảnh tiếp theo/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /ảnh trước/i })).toBeInTheDocument();
  });

  it('renders a static banner (no dots/arrows) when given exactly 1 image', () => {
    render(<HeroSection slots={{ title: 'Trung tâm A', images: ['/demo-banners/a.webp'] }} />);

    expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /ảnh tiếp theo/i })).not.toBeInTheDocument();
  });

  it('falls back to the legacy single `image` slot when no images array is provided', () => {
    render(<HeroSection slots={{ title: 'Trung tâm A', image: '/demo-banners/legacy.webp' }} />);

    // Single static banner → no carousel chrome (backward-compat).
    expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /ảnh tiếp theo/i })).not.toBeInTheDocument();
  });

  it('renders the text-gradient fallback (no frame) when there is no image at all', () => {
    render(<HeroSection slots={{ title: 'Trung tâm A' }} />);

    expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
    // Title still renders as the H1.
    expect(screen.getByRole('heading', { level: 1, name: 'Trung tâm A' })).toBeInTheDocument();
  });
});

describe('HeroBannerCarousel — manual navigation', () => {
  it('selects the clicked dot', async () => {
    const user = userEvent.setup();
    render(<HeroBannerCarousel images={TWO} label="Trung tâm A" />);

    const tabs = screen.getAllByRole('tab');
    expect(tabs[0]).toHaveAttribute('aria-selected', 'true');

    await user.click(tabs[1]!);
    expect(tabs[1]).toHaveAttribute('aria-selected', 'true');
    expect(tabs[0]).toHaveAttribute('aria-selected', 'false');
  });

  it('advances to the next slide on the next arrow and wraps around', async () => {
    const user = userEvent.setup();
    render(<HeroBannerCarousel images={TWO} label="Trung tâm A" />);

    await user.click(screen.getByRole('button', { name: /ảnh tiếp theo/i }));
    expect(screen.getAllByRole('tab')[1]).toHaveAttribute('aria-selected', 'true');

    // Wrap: next again → back to slide 0.
    await user.click(screen.getByRole('button', { name: /ảnh tiếp theo/i }));
    expect(screen.getAllByRole('tab')[0]).toHaveAttribute('aria-selected', 'true');
  });
});
