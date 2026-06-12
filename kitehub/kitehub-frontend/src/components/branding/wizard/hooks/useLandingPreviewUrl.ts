// ---------------------------------------------------------------------------
// useLandingPreviewUrl — WYSIWYG preview source = landing render path (GAP-1215).
//
// Replaces the ad-hoc `buildLandingPreviewHtml` srcDoc composer. Instead of a
// SECOND HTML document that drifts from the real landing, the wizard preview
// iframe now points at the REAL KiteClass landing render path
// (`kiteclass-frontend (public)/page.tsx` → TemplateRenderer + sections) via a
// framing-allowed `/preview` route, themed by draft query params. One render
// path → preview == deploy source; new sections/themes auto-appear with no
// hand-sync (GAP-1215 AC).
//
// The wizard runs at kitehub-frontend (:3001); the landing at kiteclass-frontend
// (:3000). The iframe loads cross-origin — only the framed page must allow it
// (kiteclass `/preview` frame-ancestors KH origin); CORS does not apply to
// iframe document loads.
// ---------------------------------------------------------------------------

import { useMemo } from 'react';

/** Base URL of the KiteClass landing app (env-driven; local dev default). */
export const KITECLASS_PREVIEW_BASE_URL =
  process.env.NEXT_PUBLIC_KITECLASS_URL || 'http://localhost:3000';

export interface LandingPreviewUrlParams {
  /** Override the base URL (defaults to NEXT_PUBLIC_KITECLASS_URL). */
  baseUrl?: string;
  /** Tenant id/slug to preview real data; omit for a fresh (pre-deploy) draft. */
  tenant?: string | null;
  /** 'personal' | 'organization' template override (?template=). */
  templateType?: string | null;
  /** Brand colours (with or without leading '#'). */
  primary: string;
  secondary: string;
  accent: string;
  /** Draft org name shown in nav/hero before the tenant exists (?orgName=). */
  orgName?: string | null;
  /** Draft logo URL (?logo=). */
  logoUrl?: string | null;
  /** Freshly-generated preview banner → landing hero image (?heroImage=). */
  heroImage?: string | null;
}

/** Strip a leading '#' — the landing reads `#${params.primary}`. */
function bareHex(value: string | undefined | null): string | undefined {
  if (!value) return undefined;
  const v = value.startsWith('#') ? value.slice(1) : value;
  return /^[0-9A-Fa-f]{3,8}$/.test(v) ? v : undefined;
}

/**
 * Compose the landing `/preview` URL with draft-theme query params.
 *
 * Pure + deterministic so it can be unit-tested without a DOM.
 */
export function buildLandingPreviewUrl(params: LandingPreviewUrlParams): string {
  const base = (params.baseUrl || KITECLASS_PREVIEW_BASE_URL).replace(/\/+$/, '');
  const qs = new URLSearchParams();

  const primary = bareHex(params.primary);
  const secondary = bareHex(params.secondary);
  const accent = bareHex(params.accent);
  if (primary) qs.set('primary', primary);
  if (secondary) qs.set('secondary', secondary);
  if (accent) qs.set('accent', accent);

  if (params.templateType) qs.set('template', params.templateType);
  if (params.tenant) qs.set('tenant', params.tenant);

  const org = (params.orgName ?? '').trim();
  if (org) qs.set('orgName', org);
  if (params.logoUrl) qs.set('logo', params.logoUrl);
  if (params.heroImage) qs.set('heroImage', params.heroImage);

  const query = qs.toString();
  return query ? `${base}/preview?${query}` : `${base}/preview`;
}

/** React hook wrapper — memoised on the param values. */
export function useLandingPreviewUrl(params: LandingPreviewUrlParams): string {
  return useMemo(
    () => buildLandingPreviewUrl(params),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [
      params.baseUrl,
      params.tenant,
      params.templateType,
      params.primary,
      params.secondary,
      params.accent,
      params.orgName,
      params.logoUrl,
      params.heroImage,
    ],
  );
}

export default useLandingPreviewUrl;
