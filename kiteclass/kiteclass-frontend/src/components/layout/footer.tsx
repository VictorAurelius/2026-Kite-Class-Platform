/**
 * Footer component with watermark (always visible on all tiers).
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import Link from 'next/link';

export function Footer() {
  return (
    <footer className="border-t bg-background py-6">
      <div className="container mx-auto px-4">
        <div className="flex flex-col items-center justify-between gap-4 md:flex-row">
          {/* Branding */}
          <div className="text-center md:text-left">
            <p className="text-sm text-muted-foreground">
              © {new Date().getFullYear()} KiteClass Platform. All rights reserved.
            </p>
          </div>

          {/* Watermark - Always visible on all tiers */}
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <span>Powered by</span>
            <Link
              href="https://kitehub.me"
              target="_blank"
              rel="noopener noreferrer"
              className="font-semibold text-primary hover:underline"
            >
              KiteClass
            </Link>
          </div>

          {/* Links */}
          <div className="flex gap-4 text-sm">
            <Link href="/help" className="text-muted-foreground hover:text-foreground">
              Trợ giúp
            </Link>
            <Link href="/legal/privacy" className="text-muted-foreground hover:text-foreground">
              Quyền riêng tư
            </Link>
            <Link href="/legal/terms" className="text-muted-foreground hover:text-foreground">
              Điều khoản
            </Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
