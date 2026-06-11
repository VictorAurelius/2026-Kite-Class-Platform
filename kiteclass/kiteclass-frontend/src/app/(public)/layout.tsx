import { Metadata } from 'next';
import Link from 'next/link';
import { headers } from 'next/headers';
import { Button } from '@/components/ui/button';
import { GraduationCap } from 'lucide-react';
import { ErrorBoundary } from '@/components/error-boundary';
import { ConsentBanner } from '@kite/shared-ui';
import { publicApi } from '@/lib/api/public';

// Resolve the tenant's display name + logo + contact for the public nav/footer.
// Tenant resolution priority:
// 1. x-tenant-id header injected by host→tenant middleware (GAP-811/GAP-1077) —
//    enables 1-FE-many-tenant by Host (the layout cannot read ?tenant= searchParams).
// 2. NEXT_PUBLIC_TENANT_ID (1-tenant-per-deploy fallback).
// 3. hardcoded default tenant.
// Falls back to a generic platform identity when no tenant resolves. GAP-808.
//
// Bucket B (GAP-958): nav/footer use the dedicated `centerName` field (the center's
// own name) in preference to the marketing `heroTitle` slogan. Contact is surfaced
// only when the tenant actually configured it — anti-fabrication: never show a
// `1900 xxxx` / `support@kiteclass.com` placeholder.
interface TenantIdentity {
  name: string;
  logoUrl: string | null;
  tagline: string | null;
  contactEmail: string | null;
  contactPhone: string | null;
}

async function getTenantIdentity(): Promise<TenantIdentity> {
  const hdrs = await headers();

  // Unknown subdomain (middleware tried to resolve, BE returned 404 — GAP-1200).
  // Render generic KiteClass chrome rather than the env/default tenant's brand:
  // showing a DIFFERENT center's name/logo on a mistyped subdomain is confusing
  // and a mild content-leak. The page itself renders the friendly not-found body.
  if (hdrs.get('x-tenant-not-found')) {
    return { name: 'KiteClass', logoUrl: null, tagline: null, contactEmail: null, contactPhone: null };
  }

  const headerTenantId = hdrs.get('x-tenant-id') ?? undefined;
  const tenantId =
    headerTenantId ??
    process.env.NEXT_PUBLIC_TENANT_ID ??
    '11111111-1111-1111-1111-111111111111';
  try {
    const landing = (await publicApi.getLandingPage(tenantId)) as {
      centerName?: string;
      heroTitle?: string;
      logoUrl?: string;
      tagline?: string;
      contactEmail?: string;
      contactPhone?: string;
    };
    const name = landing.centerName?.trim() || landing.heroTitle?.trim() || 'Trung tâm giáo dục';
    return {
      name,
      logoUrl: landing.logoUrl ?? null,
      tagline: landing.tagline?.trim() || null,
      contactEmail: landing.contactEmail?.trim() || null,
      contactPhone: landing.contactPhone?.trim() || null,
    };
  } catch {
    return { name: 'Trung tâm giáo dục', logoUrl: null, tagline: null, contactEmail: null, contactPhone: null };
  }
}

export const metadata: Metadata = {
  title: {
    default: 'KiteClass - Hệ thống Quản lý Trung tâm Tiếng Anh',
    template: '%s | KiteClass',
  },
  description:
    'Nền tảng quản lý trung tâm tiếng Anh toàn diện với LMS, quản lý học viên, điểm danh tự động và thanh toán trực tuyến.',
  keywords: [
    'trung tâm tiếng anh',
    'quản lý học viên',
    'LMS',
    'điểm danh tự động',
    'thanh toán online',
  ],
  openGraph: {
    type: 'website',
    locale: 'vi_VN',
    siteName: 'KiteClass',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'KiteClass - Hệ thống Quản lý Trung tâm Tiếng Anh',
    description:
      'Nền tảng quản lý trung tâm tiếng Anh toàn diện với LMS, quản lý học viên, điểm danh tự động và thanh toán trực tuyến.',
  },
};

export default async function PublicLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const {
    name: tenantName,
    logoUrl: tenantLogo,
    tagline: tenantTagline,
    contactEmail,
    contactPhone,
  } = await getTenantIdentity();
  return (
    <div className="min-h-screen flex flex-col">
      {/* Skip to main content (accessibility) */}
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:px-4 focus:py-2 focus:bg-primary focus:text-theme-primary-foreground focus:rounded"
      >
        Chuyển đến nội dung chính
      </a>

      {/* Public Header */}
      <header className="border-b bg-white sticky top-0 z-50" role="banner">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between">
          {/* Logo + brand. The brand can be a long slogan when the tenant has not
              set a short `centerName` (GAP-1206) — clamp to a single line + cap
              width so a long heroTitle never wraps to 3-4 lines on mobile and
              shoves the header to ~150px tall. `min-w-0` lets the span truncate
              inside the flex row; the logo stays fixed via `shrink-0`. */}
          <Link href="/" className="flex min-w-0 items-center gap-2">
            {tenantLogo ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={tenantLogo} alt={tenantName} className="h-8 w-8 shrink-0 object-contain" />
            ) : (
              <GraduationCap className="h-8 w-8 shrink-0 text-theme-primary" />
            )}
            <span className="max-w-[60vw] truncate text-lg font-bold sm:max-w-none sm:text-2xl">
              {tenantName}
            </span>
          </Link>

          {/* Navigation */}
          <nav
            className="hidden md:flex items-center gap-6"
            role="navigation"
            aria-label="Điều hướng chính"
          >
            <Link
              href="/"
              className="text-sm font-medium hover:text-theme-primary transition-colors"
            >
              Trang chủ
            </Link>
            <Link
              href="/catalog"
              className="text-sm font-medium hover:text-theme-primary transition-colors"
            >
              Khóa học
            </Link>
            <Link
              href="/about"
              className="text-sm font-medium hover:text-theme-primary transition-colors"
            >
              Giới thiệu
            </Link>
            <Link
              href="/contact"
              className="text-sm font-medium hover:text-theme-primary transition-colors"
            >
              Liên hệ
            </Link>
          </nav>

          {/* CTA Buttons */}
          <div className="flex items-center gap-3">
            <Button variant="ghost" asChild>
              <Link href="/login">Đăng nhập</Link>
            </Button>
            <Button asChild className="bg-theme-primary hover:bg-theme-primary/90 text-white">
              <Link href="/register">Đăng ký</Link>
            </Button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main id="main-content" className="flex-1" role="main">
        <ErrorBoundary>{children}</ErrorBoundary>
      </main>

      {/* Public Footer */}
      <footer className="border-t bg-muted/50 mt-auto" role="contentinfo">
        <div className="container mx-auto px-4 py-8">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
            {/* About */}
            <div>
              <div className="flex items-center gap-2 mb-4 min-w-0">
                {tenantLogo ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img src={tenantLogo} alt={tenantName} className="h-6 w-6 shrink-0 object-contain" />
                ) : (
                  <GraduationCap className="h-6 w-6 shrink-0 text-theme-primary" />
                )}
                {/* Clamp long brand (heroTitle fallback) to 2 lines — GAP-1206. */}
                <span className="font-bold text-lg line-clamp-2">{tenantName}</span>
              </div>
              <p className="text-sm text-muted-foreground">
                {tenantTagline || 'Nền tảng giáo dục giúp tối ưu vận hành lớp học và nâng cao chất lượng giảng dạy.'}
              </p>
            </div>

            {/* Quick Links */}
            <div>
              <h3 className="font-semibold mb-4">Liên kết nhanh</h3>
              <ul className="space-y-2 text-sm">
                <li>
                  <Link
                    href="/catalog"
                    className="text-muted-foreground hover:text-theme-primary"
                  >
                    Khóa học
                  </Link>
                </li>
                <li>
                  <Link
                    href="/about"
                    className="text-muted-foreground hover:text-theme-primary"
                  >
                    Giới thiệu
                  </Link>
                </li>
                <li>
                  <Link
                    href="/contact"
                    className="text-muted-foreground hover:text-theme-primary"
                  >
                    Liên hệ
                  </Link>
                </li>
              </ul>
            </div>

            {/* Resources */}
            <div>
              <h3 className="font-semibold mb-4">Tài nguyên</h3>
              <ul className="space-y-2 text-sm">
                <li>
                  <Link
                    href="/login"
                    className="text-muted-foreground hover:text-theme-primary"
                  >
                    Đăng nhập
                  </Link>
                </li>
                <li>
                  <Link
                    href="/register"
                    className="text-muted-foreground hover:text-theme-primary"
                  >
                    Đăng ký
                  </Link>
                </li>
              </ul>
            </div>

            {/* Contact — surfaced only when the tenant configured it (no placeholder). */}
            <div>
              <h3 className="font-semibold mb-4">Liên hệ</h3>
              {contactEmail || contactPhone ? (
                <ul className="space-y-2 text-sm text-muted-foreground">
                  {contactEmail && (
                    <li>
                      Email:{' '}
                      <a href={`mailto:${contactEmail}`} className="hover:text-theme-primary">
                        {contactEmail}
                      </a>
                    </li>
                  )}
                  {contactPhone && (
                    <li>
                      Hotline:{' '}
                      <a href={`tel:${contactPhone}`} className="hover:text-theme-primary">
                        {contactPhone}
                      </a>
                    </li>
                  )}
                </ul>
              ) : (
                <p className="text-sm text-muted-foreground">
                  Liên hệ qua{' '}
                  <Link href="/contact" className="hover:text-theme-primary">
                    trang liên hệ
                  </Link>
                  .
                </p>
              )}
            </div>
          </div>

          {/* Copyright */}
          <div className="border-t mt-8 pt-6 text-center text-sm text-muted-foreground">
            <p>
              © {new Date().getFullYear()} {tenantName}. Vận hành trên nền tảng KiteClass.
            </p>
          </div>
        </div>
      </footer>

      {/* PDPL 2023 Articles 11-13 cookie/consent — GAP-353 Wave 23 Bucket BC */}
      <ConsentBanner
        privacyHref="/legal/privacy"
        cookieHref="/legal/cookies"
        termsHref="/legal/terms"
        lang="vi"
      />
    </div>
  );
}
