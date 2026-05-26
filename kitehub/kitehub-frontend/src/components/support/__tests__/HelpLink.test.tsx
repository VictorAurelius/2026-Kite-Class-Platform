/**
 * Tests for HelpLink (Wave beta-prep-1 Bucket G3 — inline contextual help link).
 */
import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/test-utils';
import { HelpLink } from '../HelpLink';

describe('HelpLink', () => {
  it('renders for a known topic with aria-label + tooltip', () => {
    render(<HelpLink topic="signup" />);
    const link = screen.getByTestId('help-link-signup');
    expect(link).toBeInTheDocument();
    expect(link.getAttribute('aria-label')).toMatch(/đăng ký/i);
    expect(link.getAttribute('title')).toMatch(/hỗ trợ/i);
  });

  it('opens in new tab with rel noopener noreferrer (security)', () => {
    render(<HelpLink topic="consent" />);
    const link = screen.getByTestId('help-link-consent');
    expect(link.getAttribute('target')).toBe('_blank');
    expect(link.getAttribute('rel')).toContain('noopener');
    expect(link.getAttribute('rel')).toContain('noreferrer');
  });

  it('renders branch topic with Phase 1.5 messaging', () => {
    render(<HelpLink topic="branch" />);
    const link = screen.getByTestId('help-link-branch');
    expect(link.getAttribute('title')).toMatch(/1 chi nhánh/i);
    expect(link.getAttribute('title')).toMatch(/Phase 1\.5/i);
  });

  it('inline variant applies smaller class (5x5 inline vs 6x6 default)', () => {
    const { rerender } = render(<HelpLink topic="signup" />);
    let link = screen.getByTestId('help-link-signup');
    expect(link.className).toContain('h-6');

    rerender(<HelpLink topic="signup" inline />);
    link = screen.getByTestId('help-link-signup');
    expect(link.className).toContain('h-5');
  });

  it('href points to user manual route per topic registry', () => {
    render(<HelpLink topic="first-class" />);
    const link = screen.getByTestId('help-link-first-class');
    expect(link.getAttribute('href')).toBe('/help/p2-owner/first-class-wizard');
  });
});
