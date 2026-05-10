/**
 * Client shell that lazy-loads the heavy landing client component.
 *
 * Wrapping `next/dynamic({ ssr: false })` requires a 'use client' boundary.
 * This shell stays tiny — just the dynamic import wiring — so the JSON-LD and
 * SEO meta from the server `page.tsx` ship without paying the framer-motion
 * cost up-front.
 *
 * The `loading` fallback renders `LandingShellSSR`, a static above-fold shell
 * with real content. Next.js renders the loading state into the initial HTML
 * for `ssr: false` dynamic imports, so bots / headless reviewers without JS
 * see a complete hero + CTAs + value props instead of the prior
 * "Đang tải trang chủ…" spinner that caused AWS Activate denial 2026-05-10
 * (GAP-459).
 *
 * GAP-127 Wave 7-Perf — code-splitting for landing page.
 * GAP-459 — replace loading spinner with semantic SSR shell.
 */

'use client';

import dynamic from 'next/dynamic';
import LandingShellSSR from './LandingShellSSR';

const LandingClient = dynamic(() => import('./LandingClient'), {
  ssr: false,
  loading: () => <LandingShellSSR />,
});

export default function LandingShell() {
  return <LandingClient />;
}
