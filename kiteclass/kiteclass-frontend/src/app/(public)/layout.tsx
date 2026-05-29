import { Metadata } from 'next';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { GraduationCap } from 'lucide-react';
import { ErrorBoundary } from '@/components/error-boundary';
import { ConsentBanner } from '@kite/shared-ui';
import { publicApi } from '@/lib/api/public';

// Resolve the tenant's display name + logo for the public nav/footer.
// Production: each tenant's FE deploy sets NEXT_PUBLIC_TENANT_ID to its own tenant
// (the layout cannot read ?tenant= searchParams — those are page-scoped). Falls back
// to a generic platform identity when no tenant resolves. GAP-808 follow-up: nav was
// hardcoded "KiteClass" regardless of tenant.
async function getTenantIdentity(): Promise<{ name: string; logoUrl: string | null }> {
  const tenantId =
    process.env.NEXT_PUBLIC_TENANT_ID ?? '11111111-1111-1111-1111-111111111111';
  try {
    const landing = await publicApi.getLandingPage(tenantId);
    return {
      name: landing.heroTitle || 'KiteClass',
      logoUrl: landing.logoUrl ?? null,
    };
  } catch {
    return { name: 'KiteClass', logoUrl: null };
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
  const { name: tenantName, logoUrl: tenantLogo } = await getTenantIdentity();
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
          {/* Logo */}
          <Link href="/" className="flex items-center gap-2">
            {tenantLogo ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={tenantLogo} alt={tenantName} className="h-8 w-8 object-contain" />
            ) : (
              <GraduationCap className="h-8 w-8 text-theme-primary" />
            )}
            <span className="text-2xl font-bold">{tenantName}</span>
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
              <div className="flex items-center gap-2 mb-4">
                {tenantLogo ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img src={tenantLogo} alt={tenantName} className="h-6 w-6 object-contain" />
                ) : (
                  <GraduationCap className="h-6 w-6 text-theme-primary" />
                )}
                <span className="font-bold text-lg">{tenantName}</span>
              </div>
              <p className="text-sm text-muted-foreground">
                Nền tảng quản lý trung tâm tiếng Anh toàn diện, giúp tối ưu hóa
                vận hành và nâng cao chất lượng giảng dạy.
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

            {/* Contact */}
            <div>
              <h3 className="font-semibold mb-4">Liên hệ</h3>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li>Email: {process.env.NEXT_PUBLIC_CONTACT_EMAIL || 'support@kiteclass.com'}</li>
                <li>Hotline: {process.env.NEXT_PUBLIC_CONTACT_PHONE || '1900 xxxx'}</li>
              </ul>
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
