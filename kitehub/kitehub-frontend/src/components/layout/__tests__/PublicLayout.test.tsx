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
      const logoLink = screen.getAllByRole('link')[0];
      expect(logoLink).toHaveAttribute('href', '/');
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
      render(<PublicLayout><div>Content</div></PublicLayout>);
      expect(screen.getByText('Sản phẩm')).toBeInTheDocument();
      expect(screen.getByText('Tính năng')).toBeInTheDocument();
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

    it('renders feature list', () => {
      render(<PublicLayout><div>Content</div></PublicLayout>);
      expect(screen.getByText('Quản lý học viên')).toBeInTheDocument();
      expect(screen.getByText('Quản lý khóa học')).toBeInTheDocument();
      expect(screen.getByText('Điểm danh tự động')).toBeInTheDocument();
      expect(screen.getByText('Thanh toán online')).toBeInTheDocument();
    });

    it('renders contact info', () => {
      render(<PublicLayout><div>Content</div></PublicLayout>);
      expect(screen.getByText('support@kitehub.me')).toBeInTheDocument();
      expect(screen.getByText('1900 xxxx xx')).toBeInTheDocument();
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
