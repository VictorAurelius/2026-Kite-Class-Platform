/**
 * Public landing page (server component).
 *
 * The actual landing UI is a heavy 'use client' component that pulls in
 * `framer-motion` (~130 KB gz). To keep First Load JS small we delegate to
 * `LandingShell`, a client wrapper that uses `next/dynamic({ ssr: false })`
 * so the framer-motion bundle ships as a separate chunk that hydrates after
 * the static SEO shell paints.
 *
 * GAP-127 Wave 7-Perf — code-splitting for landing page.
 */

import { JsonLd } from '@/components/seo/JsonLd';
import { organizationSchema, websiteSchema } from '@/components/seo/schemas';
import LandingShell from './LandingShell';

export default function HomePage() {
  return (
    <>
      <JsonLd json={JSON.stringify(organizationSchema())} />
      <JsonLd json={JSON.stringify(websiteSchema())} />
      <LandingShell />
    </>
  );
}
