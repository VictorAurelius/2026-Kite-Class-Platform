/**
 * Landing preview route — WYSIWYG source for the AI Branding wizard (GAP-1215).
 *
 * Renders the EXACT same component as the public landing `/` (one render path →
 * preview == deploy source; sections/themes that land on the real landing
 * auto-appear here with no hand-sync). The ONLY differences vs `/`:
 *   1. This route is framing-allowed — kiteclass `next.config.js` exempts
 *      `/preview` from `X-Frame-Options: DENY` + sets `frame-ancestors` to the
 *      KiteHub wizard origin so the wizard (`:3001`) can embed it cross-origin.
 *      `/` keeps DENY (no clickjacking surface change for the real homepage).
 *   2. `noindex` — draft-theme previews must never be crawled.
 *
 * Draft-theme query params (?primary=&secondary=&accent=&template=&orgName=
 * &logo=&heroImage=&tenant=) are consumed by the shared LandingPage component.
 */
import type { Metadata } from 'next';
import LandingPage, { generateMetadata as landingMetadata } from '../page';

export async function generateMetadata(args: {
  searchParams: Promise<{ tenant?: string }>;
}): Promise<Metadata> {
  const base = await landingMetadata(args);
  return { ...base, robots: { index: false, follow: false } };
}

export default LandingPage;
