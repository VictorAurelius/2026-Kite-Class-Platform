/**
 * Tests for landing page sections — anti-fabrication contract (GAP-958).
 *
 * Data-driven sections (Teachers / Certificates / Pricing) render ONLY real
 * tenant-provided content and HIDE entirely when no slot data is configured —
 * never invent fictitious teachers, prices, or programs. EnrollmentSection keeps
 * generic process steps (no fabricated social proof) and still renders defaults.
 *
 * @since 2026-04-04 (anti-fabrication rewrite 2026-06-09)
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/utils';
import { TeachersSection } from '../TeachersSection';
import { CertificatesSection } from '../CertificatesSection';
import { EnrollmentSection } from '../EnrollmentSection';
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
  it('renders section heading', () => {
    render(<EnrollmentSection />);
    expect(screen.getByText(/tuyển sinh/i)).toBeInTheDocument();
  });

  it('renders enrollment steps', () => {
    render(<EnrollmentSection />);
    const steps = screen.getAllByRole('listitem');
    expect(steps.length).toBeGreaterThanOrEqual(3);
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
  it('renders heading + default pain/solution cards', () => {
    render(<ProblemSolutionSection />);
    expect(screen.getByRole('heading', { level: 2 })).toBeInTheDocument();
    expect(screen.getByText(/điểm danh thủ công/i)).toBeInTheDocument();
    expect(screen.getByText(/học phí dễ tính nhầm/i)).toBeInTheDocument();
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
    expect(screen.queryByText(/vận hành trung tâm/i)).not.toBeInTheDocument();
  });
});

describe('HowItWorksSection', () => {
  it('renders heading + 3 default steps', () => {
    render(<HowItWorksSection />);
    expect(screen.getByText(/bắt đầu trong ba bước/i)).toBeInTheDocument();
    const steps = screen.getAllByRole('listitem');
    expect(steps).toHaveLength(3);
  });

  it('hides when steps explicitly empty', () => {
    render(<HowItWorksSection slots={{ steps: [] }} />);
    expect(screen.queryByText(/bắt đầu trong ba bước/i)).not.toBeInTheDocument();
  });
});

describe('TrustStripSection', () => {
  it('renders factual default trust signals (no fake partners)', () => {
    render(<TrustStripSection />);
    expect(screen.getByText(/nghị định 13\/2023/i)).toBeInTheDocument();
    expect(screen.getByText(/hỗ trợ tiếng việt/i)).toBeInTheDocument();
  });

  it('hides when signals explicitly cleared', () => {
    render(<TrustStripSection slots={{ signals: [] }} />);
    expect(screen.queryByText(/nghị định 13\/2023/i)).not.toBeInTheDocument();
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
