/**
 * Tests for landing page sections — anti-fabrication contract (GAP-958) +
 * audience-fit hide-when-empty for ported marketing sections (GAP-1205).
 *
 * Data-driven sections (Teachers / Certificates / Pricing) render ONLY real
 * tenant-provided content and HIDE entirely when no slot data is configured —
 * never invent fictitious teachers, prices, or programs.
 *
 * F-sections ported from the platform marketing kit (ProblemSolution /
 * HowItWorks / TrustStrip / Features / Enrollment) previously fell back to
 * platform-pitch copy aimed at center OWNERS — wrong audience for a tenant
 * landing (visitors are parents/students). Per GAP-1205 they now also
 * hide-when-empty and render only tenant slot data.
 *
 * @since 2026-04-04 (anti-fabrication rewrite 2026-06-09; GAP-1205 2026-06-11)
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/utils';
import { TeachersSection } from '../TeachersSection';
import { CertificatesSection } from '../CertificatesSection';
import { EnrollmentSection } from '../EnrollmentSection';
import { FeaturesSection } from '../FeaturesSection';
import { PricingSection } from '../PricingSection';
import { ProblemSolutionSection } from '../ProblemSolutionSection';
import { HowItWorksSection } from '../HowItWorksSection';
import { TrustStripSection } from '../TrustStripSection';
import { FloatingCTA } from '../FloatingCTA';
import type { SlotItem } from '@/lib/template/slots';

describe('TeachersSection', () => {
  it('hides when no teacher data is configured (anti-fabrication)', () => {
    render(<TeachersSection />);
    expect(screen.queryByText(/đội ngũ giáo viên/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('article')).not.toBeInTheDocument();
  });

  it('renders real teacher cards when slot data is provided', () => {
    const teachers: SlotItem[] = [
      { title: 'Trần Thị Hồng', description: 'Tiếng Anh giao tiếp', items: ['IELTS 8.0'] },
      { title: 'Nguyễn Văn An', description: 'Toán tư duy', items: ['Thạc sĩ Sư phạm'] },
    ];
    render(<TeachersSection slots={{ teachers }} />);
    expect(screen.getByText(/đội ngũ giáo viên/i)).toBeInTheDocument();
    expect(screen.getAllByRole('article').length).toBe(2);
  });
});

describe('CertificatesSection', () => {
  it('hides when no programs configured (anti-fabrication)', () => {
    render(<CertificatesSection />);
    expect(screen.queryByRole('heading', { name: /chương trình giảng dạy/i })).not.toBeInTheDocument();
  });

  it('renders real programs when slot data is provided', () => {
    const certificates: SlotItem[] = [
      { title: 'Luyện thi IELTS', description: 'Lộ trình 3-12 tháng', items: ['Band 5.0–7.5'] },
    ];
    render(<CertificatesSection slots={{ certificates }} />);
    expect(screen.getByRole('heading', { level: 2, name: /chương trình giảng dạy/i })).toBeInTheDocument();
    expect(screen.getByText(/Luyện thi IELTS/i)).toBeInTheDocument();
  });
});

describe('EnrollmentSection', () => {
  it('hides when no enrollment steps configured (GAP-1205 audience-fit)', () => {
    render(<EnrollmentSection />);
    expect(screen.queryByText(/tuyển sinh/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/đăng ký tài khoản/i)).not.toBeInTheDocument();
  });

  it('renders tenant enrollment steps when slot data is provided', () => {
    const steps: SlotItem[] = [
      { title: 'Đăng ký học thử', description: 'Điền form đăng ký', icon: '1' },
      { title: 'Xếp lớp', description: 'Kiểm tra đầu vào', icon: '2' },
      { title: 'Vào học', description: 'Tham gia buổi đầu', icon: '3' },
    ];
    render(<EnrollmentSection slots={{ steps }} />);
    expect(screen.getByText(/tuyển sinh/i)).toBeInTheDocument();
    expect(screen.getAllByRole('listitem').length).toBe(3);
  });
});

describe('FeaturesSection', () => {
  it('hides when no features configured (GAP-1205 — no platform LMS pitch)', () => {
    render(<FeaturesSection />);
    expect(screen.queryByText(/tính năng nổi bật/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/hệ thống lms/i)).not.toBeInTheDocument();
  });

  it('renders tenant features when slot data is provided', () => {
    const features: SlotItem[] = [
      { icon: 'book', title: 'Lộ trình cá nhân hóa', description: 'Theo trình độ từng em' },
    ];
    render(<FeaturesSection slots={{ features }} />);
    expect(screen.getByText(/tính năng nổi bật/i)).toBeInTheDocument();
    expect(screen.getByText(/lộ trình cá nhân hóa/i)).toBeInTheDocument();
  });
});

describe('PricingSection', () => {
  it('hides when no pricing configured (anti-fabrication)', () => {
    render(<PricingSection />);
    expect(screen.queryByText(/bảng giá/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('article')).not.toBeInTheDocument();
  });

  it('renders real pricing tiers + contact CTA when slot data is provided', () => {
    const plans: SlotItem[] = [
      { title: 'Cơ bản', description: '1.500.000đ / tháng', items: ['2 buổi/tuần'] },
      { title: 'Nâng cao', description: '4.500.000đ / tháng', items: ['Kèm 1-1'] },
    ];
    render(<PricingSection slots={{ plans }} />);
    expect(screen.getByText(/bảng giá/i)).toBeInTheDocument();
    expect(screen.getAllByRole('article').length).toBe(2);
    expect(screen.getAllByRole('link', { name: /liên hệ/i }).length).toBeGreaterThanOrEqual(1);
  });
});

describe('ProblemSolutionSection', () => {
  it('hides when no slot data (GAP-1205 — no owner-facing platform pitch)', () => {
    render(<ProblemSolutionSection />);
    expect(screen.queryByRole('heading', { level: 2 })).not.toBeInTheDocument();
    expect(screen.queryByText(/điểm danh thủ công/i)).not.toBeInTheDocument();
  });

  it('renders slot data when provided', () => {
    render(
      <ProblemSolutionSection
        slots={{ items: [{ title: 'Vấn đề tùy chỉnh', description: 'Mô tả', items: ['Cách giải'] }] }}
      />,
    );
    expect(screen.getByText('Vấn đề tùy chỉnh')).toBeInTheDocument();
    expect(screen.getByText('Cách giải')).toBeInTheDocument();
  });

  it('hides when items explicitly empty', () => {
    render(<ProblemSolutionSection slots={{ items: [] }} />);
    expect(screen.queryByText(/trăn trở quen thuộc/i)).not.toBeInTheDocument();
  });
});

describe('HowItWorksSection', () => {
  it('hides when no slot data (GAP-1205 — no owner onboarding pitch)', () => {
    render(<HowItWorksSection />);
    expect(screen.queryByText(/bắt đầu học/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('listitem')).not.toBeInTheDocument();
  });

  it('renders tenant steps when slot data is provided', () => {
    const steps: SlotItem[] = [
      { title: 'Đăng ký học thử', description: 'Điền form' },
      { title: 'Xếp lớp', description: 'Kiểm tra đầu vào' },
      { title: 'Vào học', description: 'Buổi đầu tiên' },
    ];
    render(<HowItWorksSection slots={{ steps }} />);
    expect(screen.getByText(/bắt đầu học/i)).toBeInTheDocument();
    expect(screen.getAllByRole('listitem')).toHaveLength(3);
  });

  it('hides when steps explicitly empty', () => {
    render(<HowItWorksSection slots={{ steps: [] }} />);
    expect(screen.queryByText(/bắt đầu học/i)).not.toBeInTheDocument();
  });
});

describe('TrustStripSection', () => {
  it('hides when no slot data (GAP-1205 — no platform value-prop default)', () => {
    render(<TrustStripSection />);
    expect(screen.queryByText(/nghị định 13\/2023/i)).not.toBeInTheDocument();
  });

  it('renders tenant trust signals when provided', () => {
    const signals: SlotItem[] = [
      { icon: 'shield', title: 'Cam kết đầu ra IELTS', description: 'Hoàn phí nếu không đạt' },
    ];
    render(<TrustStripSection slots={{ signals }} />);
    expect(screen.getByText(/cam kết đầu ra ielts/i)).toBeInTheDocument();
  });

  it('hides when signals explicitly cleared', () => {
    render(<TrustStripSection slots={{ signals: [] }} />);
    expect(screen.queryByText(/cam kết đầu ra/i)).not.toBeInTheDocument();
  });
});

describe('FloatingCTA', () => {
  it('always renders the primary register CTA', () => {
    render(<FloatingCTA />);
    expect(screen.getByRole('link', { name: /đăng ký học thử/i })).toBeInTheDocument();
  });

  it('hides Zalo + call buttons when not configured', () => {
    render(<FloatingCTA />);
    expect(screen.queryByRole('link', { name: /zalo/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /gọi điện/i })).not.toBeInTheDocument();
  });

  it('renders tel + zalo deep-links when configured', () => {
    render(<FloatingCTA phone="0901 234 567" zaloUrl="0901234567" />);
    const tel = screen.getByRole('link', { name: /gọi điện/i });
    expect(tel).toHaveAttribute('href', 'tel:0901234567');
    const zalo = screen.getByRole('link', { name: /zalo/i });
    expect(zalo).toHaveAttribute('href', 'https://zalo.me/0901234567');
  });

  it('passes through a full zalo.me URL unchanged', () => {
    render(<FloatingCTA zaloUrl="https://zalo.me/g/abc123" />);
    expect(screen.getByRole('link', { name: /zalo/i })).toHaveAttribute(
      'href',
      'https://zalo.me/g/abc123',
    );
  });
});
