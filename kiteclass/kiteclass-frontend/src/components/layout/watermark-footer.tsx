/**
 * Watermark Footer Component
 *
 * Displays a subtle watermark footer on all pages to indicate the platform is built with KiteClass.
 *
 * @author KiteClass Team
 * @since 3.15
 */

import Link from 'next/link';

export function WatermarkFooter() {
  return (
    <footer className="border-t border-border/40 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="container flex h-14 max-w-screen-2xl items-center justify-center px-4 sm:px-6 lg:px-8">
        <div className="flex items-center gap-2 text-xs text-muted-foreground">
          <span>Powered by</span>
          <Link
            href="https://kitehub.me"
            target="_blank"
            rel="noopener noreferrer"
            className="font-semibold text-primary hover:underline"
          >
            KiteClass
          </Link>
          <span className="hidden sm:inline">•</span>
          <span className="hidden sm:inline">
            Learning Management System Platform
          </span>
        </div>
      </div>
    </footer>
  );
}
