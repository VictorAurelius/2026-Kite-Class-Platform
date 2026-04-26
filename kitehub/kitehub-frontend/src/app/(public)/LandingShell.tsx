/**
 * Client shell that lazy-loads the heavy landing client component.
 *
 * Wrapping `next/dynamic({ ssr: false })` requires a 'use client' boundary.
 * This shell stays tiny — just the dynamic import wiring — so the JSON-LD and
 * SEO meta from the server `page.tsx` ship without paying the framer-motion
 * cost up-front.
 *
 * GAP-127 Wave 7-Perf — code-splitting for landing page.
 */

'use client';

import dynamic from 'next/dynamic';

const LandingClient = dynamic(() => import('./LandingClient'), {
  ssr: false,
  loading: () => (
    <div className="min-h-screen flex items-center justify-center bg-background">
      <div className="text-muted-foreground text-sm">Đang tải trang chủ…</div>
    </div>
  ),
});

export default function LandingShell() {
  return <LandingClient />;
}
