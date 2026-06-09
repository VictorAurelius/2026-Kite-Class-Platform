/**
 * Tests for Contact page — form renders and contact info displayed.
 *
 * The form body is loaded via `next/dynamic`, so the form-field test
 * uses async queries (`findByLabelText`) to wait for the lazy chunk
 * to resolve. Static info (email, hotline) lives in the page wrapper
 * and is still queryable synchronously.
 *
 * @since 2026-04-11
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@/test/utils';
import ContactPage from '../page';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
  usePathname: () => '/contact',
}));

vi.mock('@/lib/api/public', () => ({
  publicApi: {
    submitContactForm: vi.fn().mockResolvedValue({ success: true }),
  },
}));

describe('ContactPage', () => {
  it('renders contact form with required fields', async () => {
    render(<ContactPage />);
    expect(screen.getByRole('heading', { name: /Liên hệ/i })).toBeInTheDocument();
    expect(await screen.findByLabelText(/Họ và tên/i)).toBeInTheDocument();
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
