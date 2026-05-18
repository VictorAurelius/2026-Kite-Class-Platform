/**
 * Footer component tests — GAP-540 Wave 78 Bucket F.
 *
 * Verifies support channel discoverability per outside-in audit N7:
 *  - support@kitehub.me visible as mailto: link
 *  - "Trung tâm trợ giúp" Help link to /help
 *  - "Trạng thái Beta" link to /beta-status
 *
 * @since Wave 78 Bucket F
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Footer } from '../Footer';

describe('Footer — GAP-540 support channel discoverability', () => {
  it('renders footer landmark', () => {
    render(<Footer />);
    expect(screen.getByTestId('public-footer')).toBeInTheDocument();
  });

  it('shows support email as mailto: link', () => {
    render(<Footer />);
    const link = screen.getByTestId('footer-support-email');
    expect(link).toHaveAttribute('href', 'mailto:support@kitehub.me');
    expect(link).toHaveTextContent('support@kitehub.me');
  });

  it('shows Help center link to /help', () => {
    render(<Footer />);
    const link = screen.getByTestId('footer-help-link');
    expect(link).toHaveAttribute('href', '/help');
    expect(link).toHaveTextContent('Trung tâm trợ giúp');
  });

  it('shows beta status link to /beta-status', () => {
    render(<Footer />);
    const link = screen.getByTestId('footer-status-link');
    expect(link).toHaveAttribute('href', '/beta-status');
    expect(link).toHaveTextContent('Trạng thái Beta');
  });

  it('shows Zalo OA link (Wave 98 B6 GAP-660)', () => {
    render(<Footer />);
    const link = screen.getByTestId('footer-zalo-oa-link');
    // Default placeholder fallback when NEXT_PUBLIC_KITEHUB_ZALO_OA_ID not set
    expect(link).toHaveAttribute('href', expect.stringMatching(/^https:\/\/zalo\.me\//));
    expect(link).toHaveTextContent('Hỗ trợ qua Zalo OA');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('shows privacy + terms legal links', () => {
    render(<Footer />);
    // Privacy and terms links by href (no testid needed)
    const links = screen.getAllByRole('link');
    const hrefs = links.map((l) => l.getAttribute('href'));
    expect(hrefs).toContain('/legal/privacy');
    expect(hrefs).toContain('/legal/terms');
  });
});
