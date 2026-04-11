/**
 * Tests for Contact page — form renders and contact info displayed.
 *
 * @since 2026-04-11
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@/test/utils';
import ContactPage from '../page';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

vi.mock('@/lib/api/public', () => ({
  publicApi: {
    submitContactForm: vi.fn().mockResolvedValue({ success: true }),
  },
}));

describe('ContactPage', () => {
  it('renders contact form with required fields', () => {
    render(<ContactPage />);
    expect(screen.getByRole('heading', { name: /Liên hệ/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/Họ và tên/i)).toBeInTheDocument();
  });

  it('displays email contact info', () => {
    render(<ContactPage />);
    expect(screen.getByText(/support@kiteclass\.com/i)).toBeInTheDocument();
  });

  it('displays hotline contact info', () => {
    render(<ContactPage />);
    expect(screen.getByText(/1900 xxxx/i)).toBeInTheDocument();
  });
});
