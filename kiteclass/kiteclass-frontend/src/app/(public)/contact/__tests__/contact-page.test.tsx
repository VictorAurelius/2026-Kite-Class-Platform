/**
 * Tests for the per-tenant Contact page — async server component + landing-driven aside.
 *
 * The page is an async server component that fetches the tenant landing payload; tests
 * render its resolved output and mock the landing helper. The form body loads via
 * next/dynamic (form-field validation is covered in contact-form.test.tsx).
 *
 * @since 2026-04-11 (GAP-274 phase-2 kit port: async + landing-driven Zalo)
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@/test/utils';
import ContactPage from '../page';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
  usePathname: () => '/contact',
}));

vi.mock('@/lib/api/public', () => ({
  publicApi: { submitContactForm: vi.fn().mockResolvedValue({ success: true }) },
}));

const landing: Record<string, unknown> = {};
vi.mock('@/lib/api/tenant-landing', () => ({
  getTenantLanding: () => Promise.resolve(landing),
  landingStr: (ld: Record<string, unknown> | null, k: string) =>
    ld && typeof ld[k] === 'string' && (ld[k] as string).trim() ? (ld[k] as string).trim() : null,
}));

describe('ContactPage', () => {
  it('renders the contact heading and form', async () => {
    landing.centerName = 'Lớp Toán cô Hà';
    landing.zaloUrl = undefined;
    landing.contactPhone = undefined;
    landing.contactEmail = undefined;
    render(await ContactPage());
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(/lớp toán cô hà/i);
    expect(await screen.findByLabelText(/họ và tên/i)).toBeInTheDocument();
  });

  it('renders the Zalo button only when the tenant configured zaloUrl', async () => {
    landing.centerName = 'Lớp Toán cô Hà';
    landing.zaloUrl = 'https://zalo.me/0912345678';
    render(await ContactPage());
    const zalo = screen.getByRole('link', { name: /nhắn zalo/i });
    expect(zalo).toHaveAttribute('href', 'https://zalo.me/0912345678');
  });

  it('hides the Zalo button when zaloUrl is absent (anti-fabrication)', async () => {
    landing.centerName = 'Lớp Toán cô Hà';
    landing.zaloUrl = undefined;
    render(await ContactPage());
    expect(screen.queryByRole('link', { name: /nhắn zalo/i })).not.toBeInTheDocument();
  });

  it('does not render placeholder contact info', async () => {
    landing.centerName = 'Lớp Toán cô Hà';
    landing.zaloUrl = undefined;
    landing.contactPhone = undefined;
    landing.contactEmail = undefined;
    render(await ContactPage());
    expect(screen.queryByText(/support@kiteclass\.com/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/1900 xxxx/i)).not.toBeInTheDocument();
  });
});
