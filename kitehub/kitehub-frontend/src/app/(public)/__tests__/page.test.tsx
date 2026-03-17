/**
 * Component tests for HomePage (landing page).
 *
 * @since PR 5.10
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/test-utils';
import HomePage from '../page';

describe('HomePage', () => {
  describe('Hero Section', () => {
    it('renders main headline', () => {
      render(<HomePage />);
      expect(screen.getByText(/Để học viên của bạn/)).toBeInTheDocument();
      expect(screen.getByText('bay cao')).toBeInTheDocument();
    });

    it('renders trial badge', () => {
      render(<HomePage />);
      expect(screen.getByText(/Dùng thử miễn phí 14 ngày/)).toBeInTheDocument();
    });

    it('renders CTA buttons', () => {
      render(<HomePage />);
      // CTA buttons appear in multiple sections (hero and bottom CTA)
      expect(screen.getAllByRole('link', { name: /Bắt đầu miễn phí/i }).length).toBeGreaterThan(0);
      expect(screen.getAllByRole('link', { name: /bảng giá/i }).length).toBeGreaterThan(0);
    });

    it('renders trust indicators', () => {
      render(<HomePage />);
      // Trust indicators appear in hero section - use getAllByText since text may appear elsewhere
      expect(screen.getAllByText(/Không cần thẻ tín dụng/).length).toBeGreaterThan(0);
      expect(screen.getByText(/Setup trong 5 phút/)).toBeInTheDocument();
      expect(screen.getByText(/Hỗ trợ 24\/7/)).toBeInTheDocument();
    });
  });

  describe('Features Section', () => {
    it('renders section title', () => {
      render(<HomePage />);
      expect(screen.getByText(/Mọi thứ bạn cần để vận hành hiệu quả/)).toBeInTheDocument();
    });

    it('renders all 6 feature cards', () => {
      render(<HomePage />);
      expect(screen.getByText('Quản lý học viên')).toBeInTheDocument();
      expect(screen.getByText('Quản lý giảng viên')).toBeInTheDocument();
      expect(screen.getByText('Khóa học & Lớp học')).toBeInTheDocument();
      expect(screen.getByText('Điểm danh thông minh')).toBeInTheDocument();
      expect(screen.getByText('Thanh toán & Hóa đơn')).toBeInTheDocument();
      expect(screen.getByText('Branding AI')).toBeInTheDocument();
    });
  });

  describe('Stats Section', () => {
    it('renders stat values', () => {
      render(<HomePage />);
      expect(screen.getByText('500+')).toBeInTheDocument();
      expect(screen.getByText('50,000+')).toBeInTheDocument();
      expect(screen.getByText('99.9%')).toBeInTheDocument();
    });

    it('renders stat labels', () => {
      render(<HomePage />);
      expect(screen.getByText('Trung tâm tin dùng')).toBeInTheDocument();
      expect(screen.getByText('Học viên đang học')).toBeInTheDocument();
      expect(screen.getByText('Uptime cam kết')).toBeInTheDocument();
    });
  });

  describe('Testimonials Section', () => {
    it('renders section title', () => {
      render(<HomePage />);
      expect(screen.getByText(/Được tin dùng bởi hàng trăm trung tâm/)).toBeInTheDocument();
    });

    it('renders testimonial names', () => {
      render(<HomePage />);
      expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument();
      expect(screen.getByText('Trần Thị B')).toBeInTheDocument();
      expect(screen.getByText('Lê Văn C')).toBeInTheDocument();
    });

    it('renders testimonial content', () => {
      render(<HomePage />);
      expect(screen.getByText(/KiteHub giúp chúng tôi tiết kiệm 10 giờ/)).toBeInTheDocument();
    });
  });

  describe('CTA Section', () => {
    it('renders final CTA title', () => {
      render(<HomePage />);
      expect(screen.getByText(/Sẵn sàng nâng tầm trung tâm của bạn/)).toBeInTheDocument();
    });

    it('renders register link', () => {
      render(<HomePage />);
      const registerLinks = screen.getAllByRole('link', { name: /Đăng ký miễn phí/i });
      expect(registerLinks.length).toBeGreaterThan(0);
    });
  });

  describe('Navigation Links', () => {
    it('links to register page', () => {
      render(<HomePage />);
      const registerLinks = screen.getAllByRole('link', { name: /miễn phí/i });
      registerLinks.forEach(link => {
        expect(link).toHaveAttribute('href', '/register');
      });
    });

    it('links to pricing page', () => {
      render(<HomePage />);
      const pricingLinks = screen.getAllByRole('link', { name: /bảng giá/i });
      pricingLinks.forEach(link => {
        expect(link).toHaveAttribute('href', '/pricing');
      });
    });
  });
});
