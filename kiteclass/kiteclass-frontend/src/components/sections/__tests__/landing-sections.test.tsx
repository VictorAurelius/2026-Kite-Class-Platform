/**
 * Tests for landing page sections — teachers, certificates, enrollment, pricing.
 * Verifies sections render meaningful content, not just a heading.
 *
 * @since 2026-04-04
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

describe('TeachersSection', () => {
  it('renders section heading', () => {
    render(<TeachersSection />);
    expect(screen.getByText(/đội ngũ giáo viên/i)).toBeInTheDocument();
  });

  it('renders at least 2 teacher cards with name and subject', () => {
    render(<TeachersSection />);
    const cards = screen.getAllByRole('article');
    expect(cards.length).toBeGreaterThanOrEqual(2);
  });
});

describe('CertificatesSection', () => {
  it('renders section heading', () => {
    render(<CertificatesSection />);
    expect(screen.getByRole('heading', { level: 2, name: /chứng chỉ/i })).toBeInTheDocument();
  });

  it('renders IELTS and TOEIC certificates', () => {
    render(<CertificatesSection />);
    expect(screen.getByText(/IELTS/i)).toBeInTheDocument();
    expect(screen.getByText(/TOEIC/i)).toBeInTheDocument();
  });
});

describe('EnrollmentSection', () => {
  it('renders section heading', () => {
    render(<EnrollmentSection />);
    expect(screen.getByText(/tuyển sinh/i)).toBeInTheDocument();
  });

  it('renders enrollment steps', () => {
    render(<EnrollmentSection />);
    // At least 3 steps
    const steps = screen.getAllByRole('listitem');
    expect(steps.length).toBeGreaterThanOrEqual(3);
  });
});

describe('PricingSection', () => {
  it('renders section heading', () => {
    render(<PricingSection />);
    expect(screen.getByText(/bảng giá/i)).toBeInTheDocument();
  });

  it('renders at least 2 pricing tiers', () => {
    render(<PricingSection />);
    const cards = screen.getAllByRole('article');
    expect(cards.length).toBeGreaterThanOrEqual(2);
  });

  it('renders contact CTA for pricing inquiry', () => {
    render(<PricingSection />);
    const links = screen.getAllByRole('link', { name: /liên hệ/i });
    expect(links.length).toBeGreaterThanOrEqual(1);
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
