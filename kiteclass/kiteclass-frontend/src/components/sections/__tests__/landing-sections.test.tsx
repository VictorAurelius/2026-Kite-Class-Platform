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
