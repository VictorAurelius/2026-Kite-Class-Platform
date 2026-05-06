/**
 * Branding gateway page tests — Wave 30 Bucket D.
 *
 * Verifies the gateway renders feature explainer + CTA link to the AI Branding
 * wizard at `/branding/wizard`.
 *
 * @since Wave 30 (GAP-266)
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@/test/utils';
import BrandingGatewayPage from '../page';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn() }),
  usePathname: () => '/branding',
}));

describe('BrandingGatewayPage (Wave 30 gateway → wizard)', () => {
  it('renders heading + AI Branding feature explainer', () => {
    render(<BrandingGatewayPage />);

    expect(screen.getByRole('heading', { level: 1, name: 'AI Branding' })).toBeInTheDocument();

    // 3 feature cards rendered
    expect(screen.getByText('Wizard 6 bước')).toBeInTheDocument();
    expect(screen.getByText('Preview trước khi triển khai')).toBeInTheDocument();
    expect(screen.getByText('Template-first')).toBeInTheDocument();
  });

  it('exposes CTA link to /branding/wizard (AI Branding wizard)', () => {
    render(<BrandingGatewayPage />);

    const cta = screen.getByTestId('branding-wizard-cta');
    expect(cta).toHaveAttribute('href', '/branding/wizard');
  });

  it('cross-links to /settings for theme preview tab', () => {
    render(<BrandingGatewayPage />);

    const settingsLink = screen.getByRole('link', { name: /Mở Cài đặt → Theme preview/ });
    expect(settingsLink).toHaveAttribute('href', '/settings');
  });
});
