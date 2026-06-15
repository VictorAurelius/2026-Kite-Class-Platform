'use client';

import Link from 'next/link';
import type { ReactNode } from 'react';
import { ConsentBanner } from '@kite/shared-ui';
import { KiteLogo } from '@/components/brand/KiteLogo';
import { SkipToContent } from '@/components/a11y/SkipToContent';
import { Footer } from './Footer';

export function PublicLayout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen flex flex-col">
      {/* Skip to main content (accessibility — WCAG 2.4.1) */}
      <SkipToContent />
      {/* Header */}
      <header className="sticky top-0 z-50 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container flex h-16 items-center justify-between">
          <Link href="/" className="transition-opacity hover:opacity-80">
            <KiteLogo size="md" />
          </Link>
          <nav className="flex items-center gap-6">
            <Link
              href="/pricing"
              className="text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
            >
              Bảng giá
            </Link>
            <Link
              href="/login"
              className="text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
            >
              Đăng nhập
            </Link>
            <Link
              href="/register"
              className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow-sm transition-all hover:bg-primary/90 hover:shadow-md"
            >
              Dùng thử miễn phí
            </Link>
          </nav>
        </div>
      </header>

      {/* Main content */}
      <main id="main-content" className="flex-1" role="main">{children}</main>

      {/* Footer — GAP-540 Wave 78 Bucket F: support channel discoverability */}
      <Footer />

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
