import { Metadata } from 'next';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { GraduationCap } from 'lucide-react';
import { ErrorBoundary } from '@/components/error-boundary';

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
};

export default function PublicLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen flex flex-col">
      {/* Skip to main content (accessibility) */}
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:px-4 focus:py-2 focus:bg-primary focus:text-primary-foreground focus:rounded"
      >
        Chuyển đến nội dung chính
      </a>

      {/* Public Header */}
      <header className="border-b bg-white sticky top-0 z-50" role="banner">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between">
          {/* Logo */}
          <Link href="/" className="flex items-center gap-2">
            <GraduationCap className="h-8 w-8 text-primary" />
            <span className="text-2xl font-bold">KiteClass</span>
          </Link>

          {/* Navigation */}
          <nav
            className="hidden md:flex items-center gap-6"
            role="navigation"
            aria-label="Điều hướng chính"
          >
            <Link
              href="/"
              className="text-sm font-medium hover:text-primary transition-colors"
            >
              Trang chủ
            </Link>
            <Link
              href="/courses"
              className="text-sm font-medium hover:text-primary transition-colors"
            >
              Khóa học
            </Link>
            <Link
              href="/about"
              className="text-sm font-medium hover:text-primary transition-colors"
            >
              Giới thiệu
            </Link>
            <Link
              href="/contact"
              className="text-sm font-medium hover:text-primary transition-colors"
            >
              Liên hệ
            </Link>
          </nav>

          {/* CTA Buttons */}
          <div className="flex items-center gap-3">
            <Button variant="ghost" asChild>
              <Link href="/login">Đăng nhập</Link>
            </Button>
            <Button asChild>
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
                <GraduationCap className="h-6 w-6 text-primary" />
                <span className="font-bold text-lg">KiteClass</span>
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
                    href="/courses"
                    className="text-muted-foreground hover:text-primary"
                  >
                    Khóa học
                  </Link>
                </li>
                <li>
                  <Link
                    href="/about"
                    className="text-muted-foreground hover:text-primary"
                  >
                    Giới thiệu
                  </Link>
                </li>
                <li>
                  <Link
                    href="/contact"
                    className="text-muted-foreground hover:text-primary"
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
                    className="text-muted-foreground hover:text-primary"
                  >
                    Đăng nhập
                  </Link>
                </li>
                <li>
                  <Link
                    href="/register"
                    className="text-muted-foreground hover:text-primary"
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
                <li>Email: support@kiteclass.com</li>
                <li>Hotline: 1900 xxxx</li>
              </ul>
            </div>
          </div>

          {/* Copyright */}
          <div className="border-t mt-8 pt-6 text-center text-sm text-muted-foreground">
            <p>
              © {new Date().getFullYear()} KiteClass. Phát triển bởi KiteClass
              Team.
            </p>
            <p className="mt-1 text-xs">
              Powered by{' '}
              <a
                href="https://claude.ai"
                target="_blank"
                rel="noopener noreferrer"
                className="hover:text-primary"
              >
                Claude Code
              </a>
            </p>
          </div>
        </div>
      </footer>
    </div>
  );
}
