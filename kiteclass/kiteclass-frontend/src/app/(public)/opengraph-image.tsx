/**
 * Per-tenant OpenGraph image (Bucket E / GAP-958).
 *
 * Renders a 1200×630 social card branded with the tenant's own name + brand
 * colour (read from the landing/branding API). Applies to all `(public)` routes.
 *
 * `force-dynamic`: the card is generated at request time, NOT at build. This
 * avoids a build-time fetch to the backend (which may be unreachable during
 * `next build`) causing a prerender failure — the exact prerender-bailout class
 * the OG route must not introduce.
 */
import { ImageResponse } from 'next/og';
import { publicApi } from '@/lib/api/public';

export const dynamic = 'force-dynamic';
export const runtime = 'nodejs';

export const alt = 'Trang chủ trung tâm';
export const size = { width: 1200, height: 630 };
export const contentType = 'image/png';

// Be Vietnam Pro covers full Vietnamese diacritics. Fetched at request time and
// wrapped in try/catch so a font-fetch failure degrades gracefully (default font)
// rather than breaking image generation.
async function loadVietnameseFont(): Promise<ArrayBuffer | null> {
  try {
    const css = await fetch(
      'https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@700&display=swap',
      { headers: { 'User-Agent': 'Mozilla/5.0' } },
    ).then((r) => r.text());
    const url = css.match(/src:\s*url\(([^)]+)\)\s*format\('(?:truetype|opentype|woff2?)'\)/)?.[1]
      ?? css.match(/url\((https:[^)]+\.(?:ttf|otf|woff2?))\)/)?.[1];
    if (!url) return null;
    return await fetch(url).then((r) => r.arrayBuffer());
  } catch {
    return null;
  }
}

export default async function OpengraphImage() {
  const tenantId = process.env.NEXT_PUBLIC_TENANT_ID ?? '11111111-1111-1111-1111-111111111111';

  let name = 'Trung tâm giáo dục';
  let tagline = 'Đăng ký học · Xem khóa học · Lịch khai giảng';
  let primary = '#3B82F6';
  let secondary = '#1E293B';
  try {
    const ld = (await publicApi.getLandingPage(tenantId)) as {
      centerName?: string;
      heroTitle?: string;
      tagline?: string;
      primaryColor?: string;
      secondaryColor?: string;
    };
    name = ld.centerName?.trim() || ld.heroTitle?.trim() || name;
    if (ld.tagline?.trim()) tagline = ld.tagline.trim();
    if (ld.primaryColor) primary = ld.primaryColor;
    if (ld.secondaryColor) secondary = ld.secondaryColor;
  } catch {
    // Backend unreachable → neutral branded fallback (no fabricated tenant data).
  }

  const font = await loadVietnameseFont();

  return new ImageResponse(
    (
      <div
        style={{
          width: '100%',
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          padding: '80px',
          background: `linear-gradient(135deg, ${secondary} 0%, ${primary} 100%)`,
          color: '#ffffff',
          fontFamily: font ? 'Be Vietnam Pro' : 'sans-serif',
        }}
      >
        <div style={{ fontSize: 34, opacity: 0.85, display: 'flex' }}>KiteClass</div>
        <div style={{ fontSize: 76, fontWeight: 700, lineHeight: 1.1, marginTop: 24, display: 'flex' }}>
          {name}
        </div>
        <div style={{ fontSize: 34, opacity: 0.9, marginTop: 28, display: 'flex' }}>{tagline}</div>
      </div>
    ),
    {
      ...size,
      ...(font
        ? { fonts: [{ name: 'Be Vietnam Pro', data: font, weight: 700 as const, style: 'normal' as const }] }
        : {}),
    },
  );
}
