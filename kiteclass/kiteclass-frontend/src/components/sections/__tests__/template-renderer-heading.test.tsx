/**
 * TemplateRenderer heading-flow tests (GAP-1208).
 *
 * Root cause fixed: section config `heading`/`subheading` overrides now flow into
 * each section's rendered <h2>. The personal template must read as ONE independent
 * teacher (e.g. "Giáo viên đồng hành", "Học phí") — not a center ("Đội ngũ giáo
 * viên", "Bảng giá"). The organization template keeps the center voice (component
 * defaults, no override).
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/utils';
import { TemplateRenderer } from '../TemplateRenderer';
import { PERSONAL_TEMPLATE, ORGANIZATION_TEMPLATE } from '@/lib/template/configs';
import type { SlotItem } from '@/lib/template/slots';

const teachers: SlotItem[] = [
  { title: 'Cô Hà', description: 'Giáo viên Toán', items: ['10 năm kinh nghiệm'] },
];
const plans: SlotItem[] = [
  { title: 'Theo buổi', description: '200.000đ / buổi', items: ['Học 1-1'] },
];

describe('TemplateRenderer — personal template voice (GAP-1208)', () => {
  it('renders independent-teacher headings, not center voice', () => {
    render(<TemplateRenderer template={PERSONAL_TEMPLATE} data={{}} slots={{ teachers: { teachers }, pricing: { plans } }} />);
    expect(screen.getByRole('heading', { level: 2, name: /giáo viên đồng hành/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: /^học phí$/i })).toBeInTheDocument();
    // Center-voice headings must be absent for a personal landing.
    expect(screen.queryByRole('heading', { name: /^đội ngũ giáo viên$/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: /^bảng giá$/i })).not.toBeInTheDocument();
  });

  it('renders a single centered teacher profile (solo layout) without a multi-column roster heading', () => {
    render(<TemplateRenderer template={PERSONAL_TEMPLATE} data={{}} slots={{ teachers: { teachers } }} />);
    expect(screen.getAllByRole('article').length).toBe(1);
    expect(screen.getByText(/cô hà/i)).toBeInTheDocument();
  });
});

describe('TemplateRenderer — organization template keeps center voice', () => {
  it('renders center-voice headings (component defaults, no override)', () => {
    render(<TemplateRenderer template={ORGANIZATION_TEMPLATE} data={{}} slots={{ teachers: { teachers }, pricing: { plans } }} />);
    expect(screen.getByRole('heading', { level: 2, name: /^đội ngũ giáo viên$/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: /^bảng giá$/i })).toBeInTheDocument();
  });
});
