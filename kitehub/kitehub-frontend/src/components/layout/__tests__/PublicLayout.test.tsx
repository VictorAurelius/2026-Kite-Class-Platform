/**
 * Component tests for PublicLayout.
 *
 * @since PR 5.10
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/test-utils';
import { PublicLayout } from '../PublicLayout';

describe('PublicLayout', () => {
  describe('structure', () => {
    it('renders children in main content area', () => {
      render(
        <PublicLayout>
          <div data-testid="child-content">Test Content</div>
        </PublicLayout>
      );
      expect(screen.getByTestId('child-content')).toBeInTheDocument();
    });

    it('has min-h-screen and flex column layout', () => {
      const { container } = render(
        <PublicLayout>
          <div>Content</div>
        </PublicLayout>
      );
      const wrapper = container.firstChild;
      expect(wrapper).toHaveClass('min-h-screen', 'flex', 'flex-col');
    });
  });

  describe('header', () => {
    it('renders header with sticky positioning', () => {
      render(<PublicLayout><div>Content</div></PublicLayout>);
      const header = document.querySelector('header');
      expect(header).toHaveClass('sticky', 'top-0', 'z-50');
    });

    it('renders logo link to home', () => {
      render(<PublicLayout><div>Content</div></PublicLayout>);
      // GAP-1373: a skip-to-content link is now the first link in the DOM, so
      // the logo is the first link that points to "/".
      const logoLink = screen
        .getAllByRole('link')
        .find((el) => el.getAttribute('href') === '/');
      expect(logoLink).toHaveAttribute('href', '/');
    });

    // GAP-1373: skip-to-content link present for keyboard/SR users (WCAG 2.4.1).
    it('renders a skip-to-content link targeting #main-content', () => {
      render(<PublicLayout><div>Content</div></PublicLayout>);
      const skipLink = screen.getByRole('link', { name: /Chuyển đến nội dung chính/i });
      expect(skipLink).toHaveAttribute('href', '#main-content');
      expect(document.getElementById('main-content')).toBeInTheDocument();
    });

    it('renders navigation links', () => {
      render(<PublicLayout><div>Content</div></PublicLayout>);
      // Links appear in both header and footer, so use getAllByRole
      const pricingLinks = screen.getAllByRole('link', { name: 'Bảng giá' });
      const loginLinks = screen.getAllByRole('link', { name: 'Đăng nhập' });
      const registerLinks = screen.getAllByRole('link', { name: 'Dùng thử miễn phí' });

      expect(pricingLinks[0]).toHaveAttribute('href', '/pricing');
      expect(loginLinks[0]).toHaveAttribute('href', '/login');
      expect(registerLinks[0]).toHaveAttribute('href', '/register');
    });
  });

  describe('footer', () => {
    it('renders footer', () => {
      render(<PublicLayout><div>Content</div></PublicLayout>);
      const footer = document.querySelector('footer');
      expect(footer).toBeInTheDocument();
    });

    it('renders footer sections', () => {
      // GAP-540 Wave 78 Bucket F: Footer extracted to Footer.tsx with new
      // "Hỗ trợ" column replacing legacy "Tính năng" — support channel
      // discoverability per outside-in N7 retention signal.
      render(<PublicLayout><div>Content</div></PublicLayout>);
      expect(screen.getByText('Sản phẩm')).toBeInTheDocument();
      expect(screen.getByText('Hỗ trợ')).toBeInTheDocument();
      expect(screen.getByText('Liên hệ')).toBeInTheDocument();
    });

    it('renders footer product links', () => {
      render(<PublicLayout><div>Content</div></PublicLayout>);
      const footerLinks = screen.getAllByRole('link');
      const pricingLinks = footerLinks.filter(link => link.getAttribute('href') === '/pricing');
      const registerLinks = footerLinks.filter(link => link.getAttribute('href') === '/register');
      const loginLinks = footerLinks.filter(link => link.getAttribute('href') === '/login');

      expect(pricingLinks.length).toBeGreaterThanOrEqual(1);
      expect(registerLinks.length).toBeGreaterThanOrEqual(1);
      expect(loginLinks.length).toBeGreaterThanOrEqual(1);
    });

    it('renders support links (GAP-540 Wave 78)', () => {
      render(<PublicLayout><div>Content</div></PublicLayout>);
      expect(screen.getByTestId('footer-support-email')).toBeInTheDocument();
      expect(screen.getByTestId('footer-help-link')).toBeInTheDocument();
      expect(screen.getByTestId('footer-status-link')).toBeInTheDocument();
    });

    it('renders contact column links', () => {
      // Contact column now has: support email (mailto:) + Privacy + Terms.
      // Phone hotline removed pending PR Phase 1.5 (placeholder until real
      // hotline contracted).
      render(<PublicLayout><div>Content</div></PublicLayout>);
      const links = screen.getAllByRole('link');
      const hrefs = links.map((l) => l.getAttribute('href'));
      expect(hrefs).toContain('/legal/privacy');
      expect(hrefs).toContain('/legal/terms');
    });

    it('renders copyright with current year', () => {
      render(<PublicLayout><div>Content</div></PublicLayout>);
      const currentYear = new Date().getFullYear();
      expect(screen.getByText(new RegExp(`${currentYear}`))).toBeInTheDocument();
      expect(screen.getByText(/KiteHub. All rights reserved/)).toBeInTheDocument();
    });
  });

  describe('main content', () => {
    it('renders main element with flex-1', () => {
      render(<PublicLayout><div>Content</div></PublicLayout>);
      const main = document.querySelector('main');
      expect(main).toHaveClass('flex-1');
    });
  });
});
