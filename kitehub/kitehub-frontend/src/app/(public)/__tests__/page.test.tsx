/**
 * Component tests for HomePage (landing page).
 *
 * @since PR 5.10
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/test-utils';
// GAP-127 — page.tsx now wraps LandingClient via next/dynamic. Tests target
// the underlying client component directly to avoid the async dynamic boundary.
import HomePage from '../LandingClient';

describe('HomePage', () => {
  describe('Hero Section', () => {
    it('renders main headline with all parts', () => {
      render(<HomePage />);
      // Headline is split across multiple elements
      // Use getByRole to target the h1 specifically
      const heading = screen.getByRole('heading', { level: 1 });
      expect(heading).toHaveTextContent(/Quản lý trung tâm/);
      expect(heading).toHaveTextContent(/giáo dục/);
      expect(heading).toHaveTextContent(/thông minh hơn/);
    });

    it('renders platform badge', () => {
      render(<HomePage />);
      expect(screen.getByText(/Nền tảng quản lý giáo dục #1 Việt Nam/)).toBeInTheDocument();
    });

    it('renders CTA buttons', () => {
      render(<HomePage />);
      // "Dùng thử miễn phí 14 ngày" appears multiple times (hero + CTA sections)
      expect(screen.getAllByText(/Dùng thử miễn phí 14 ngày/i).length).toBeGreaterThan(0);
      expect(screen.getByText(/Xem bảng giá/i)).toBeInTheDocument();
    });

    it('renders trust indicators', () => {
      render(<HomePage />);
      // Trust indicators in single line with checkmarks
      expect(screen.getByText(/Không cần thẻ tín dụng/)).toBeInTheDocument();
      expect(screen.getByText(/Hủy bất kỳ lúc nào/)).toBeInTheDocument();
      expect(screen.getByText(/Hỗ trợ tiếng Việt/)).toBeInTheDocument();
    });
  });

  describe('Stats Section', () => {
    it('renders stat values', () => {
      render(<HomePage />);
      expect(screen.getByText('500')).toBeInTheDocument();
      expect(screen.getByText('50,000')).toBeInTheDocument();
      expect(screen.getByText('99')).toBeInTheDocument();
      expect(screen.getByText('4')).toBeInTheDocument();
    });

    it('renders stat labels', () => {
      render(<HomePage />);
      expect(screen.getByText('Trung tâm tin dùng')).toBeInTheDocument();
      expect(screen.getByText('Học viên quản lý')).toBeInTheDocument();
      expect(screen.getByText('Uptime cam kết')).toBeInTheDocument();
      expect(screen.getByText('Đánh giá trung bình')).toBeInTheDocument();
    });
  });

  describe('Features Section', () => {
    it('renders section title', () => {
      render(<HomePage />);
      expect(screen.getByText(/Tất cả tính năng bạn cần/)).toBeInTheDocument();
    });

    it('renders all 6 feature cards', () => {
      render(<HomePage />);
      // Feature titles may appear in both features section and pricing features
      // Use getAllByText for features that appear in pricing tiers
      expect(screen.getAllByText('Quản lý học viên').length).toBeGreaterThan(0);
      expect(screen.getAllByText('Lịch học & Điểm danh').length).toBeGreaterThan(0);
      expect(screen.getAllByText('Thanh toán & Hóa đơn').length).toBeGreaterThan(0);
      expect(screen.getAllByText('AI Branding').length).toBeGreaterThan(0);
      expect(screen.getAllByText('Báo cáo & Thống kê').length).toBeGreaterThan(0);
      expect(screen.getAllByText('Đa chi nhánh').length).toBeGreaterThan(0);
    });
  });

  describe('Testimonials Section', () => {
    it('renders section title', () => {
      render(<HomePage />);
      expect(screen.getByText(/Khách hàng nói gì về KiteClass/)).toBeInTheDocument();
    });

    it('renders testimonial names', () => {
      render(<HomePage />);
      expect(screen.getByText('Nguyễn Thị Minh Anh')).toBeInTheDocument();
      expect(screen.getByText('Trần Văn Đức')).toBeInTheDocument();
      expect(screen.getByText('Lê Hoàng Phương')).toBeInTheDocument();
    });

    it('renders testimonial content', () => {
      render(<HomePage />);
      // Check first testimonial text exists
      expect(screen.getByText(/Trước đây tôi quản lý bằng Excel/)).toBeInTheDocument();
    });
  });

  describe('Pricing Section', () => {
    it('renders section title', () => {
      render(<HomePage />);
      expect(screen.getByText(/Bảng giá/)).toBeInTheDocument();
    });

    it('renders pricing tiers', () => {
      render(<HomePage />);
      expect(screen.getByText('FREE')).toBeInTheDocument();
      expect(screen.getByText('BASIC')).toBeInTheDocument();
      expect(screen.getByText('PREMIUM')).toBeInTheDocument();
      expect(screen.getByText('ENTERPRISE')).toBeInTheDocument();
    });
  });

  describe('Navigation Links', () => {
    it('links to register page', () => {
      render(<HomePage />);
      const registerLinks = screen.getAllByRole('link', { name: /miễn phí/i });
      expect(registerLinks.length).toBeGreaterThan(0);
      registerLinks.forEach(link => {
        expect(link).toHaveAttribute('href', '/register');
      });
    });

    it('links to pricing page', () => {
      render(<HomePage />);
      const pricingLinks = screen.getAllByRole('link', { name: /bảng giá/i });
      expect(pricingLinks.length).toBeGreaterThan(0);
      pricingLinks.forEach(link => {
        expect(link).toHaveAttribute('href', '/pricing');
      });
    });
  });
});
