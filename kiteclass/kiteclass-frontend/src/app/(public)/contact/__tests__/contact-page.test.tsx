/**
 * Tests for Contact page — env-driven contact info.
 *
 * @since 2026-04-11
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen } from '@/test/utils';
import ContactPage from '../page';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

describe('ContactPage', () => {
  it('renders contact form', () => {
    render(<ContactPage />);
    expect(screen.getByRole('heading', { name: /Liên hệ/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/Họ và tên/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Email/i)).toBeInTheDocument();
  });

  it('shows NEXT_PUBLIC_CONTACT_EMAIL when set', () => {
    const originalEnv = process.env.NEXT_PUBLIC_CONTACT_EMAIL;
    process.env.NEXT_PUBLIC_CONTACT_EMAIL = 'contact@myschool.edu.vn';

    render(<ContactPage />);
    expect(screen.getByText('contact@myschool.edu.vn')).toBeInTheDocument();

    process.env.NEXT_PUBLIC_CONTACT_EMAIL = originalEnv;
  });

  it('shows NEXT_PUBLIC_CONTACT_PHONE when set', () => {
    const originalEnv = process.env.NEXT_PUBLIC_CONTACT_PHONE;
    process.env.NEXT_PUBLIC_CONTACT_PHONE = '028 3456 7890';

    render(<ContactPage />);
    expect(screen.getByText(/028 3456 7890/)).toBeInTheDocument();

    process.env.NEXT_PUBLIC_CONTACT_PHONE = originalEnv;
  });
});
