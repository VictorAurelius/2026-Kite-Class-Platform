import { describe, it, expect } from 'vitest';

import { buildLandingPreviewHtml } from '../buildLandingPreviewHtml';

const base = {
  brand: {
    primary: '#123456',
    secondary: '#abcdef',
    background: '#ffffff',
    foreground: '#000000',
  },
  accent: '#ff8800',
  orgName: 'Trung tâm Sky',
  slug: 'sky-edu',
  logoUrl: null,
};

describe('buildLandingPreviewHtml', () => {
  it('themes :root with the brand colours', () => {
    const html = buildLandingPreviewHtml(base);
    expect(html).toContain('--primary:#123456');
    expect(html).toContain('--accent:#ff8800');
  });

  it('renders the org name HTML-escaped', () => {
    const html = buildLandingPreviewHtml({ ...base, orgName: 'A & B <Center>' });
    expect(html).toContain('A &amp; B &lt;Center&gt;');
  });

  it('escapes a script payload in the org name (XSS guard)', () => {
    const html = buildLandingPreviewHtml({ ...base, orgName: '<script>alert(1)</script>' });
    expect(html).not.toContain('<script>alert(1)</script>');
    expect(html).toContain('&lt;script&gt;');
  });

  it('uses the banner image as hero when bannerUrl present', () => {
    const html = buildLandingPreviewHtml({ ...base, bannerUrl: 'https://cdn.example.com/x.webp' });
    expect(html).toContain('hero--banner');
    expect(html).toContain('src="https://cdn.example.com/x.webp"');
  });

  it('uses the gradient hero when no banner', () => {
    const html = buildLandingPreviewHtml(base);
    expect(html).toContain('hero--gradient');
    // The `.hero-banner-img` CSS selector is always in <style>; assert the
    // actual <img> element is NOT rendered in gradient mode.
    expect(html).not.toContain('<img class="hero-banner-img"');
  });

  it('mirrors the wave-landing-100 section set', () => {
    const html = buildLandingPreviewHtml(base);
    expect(html).toContain('class="nav"'); // nav
    expect(html).toContain('Vì sao chọn'); // Problem
    expect(html).toContain('Cách hoạt động'); // HowItWorks
    expect(html).toContain('class="trust"'); // TrustStrip
    expect(html).toContain('Chat Zalo'); // FloatingCTA
    expect(html).toContain('sky-edu.kiteclass.vn'); // footer slug
  });

  it('coerces an invalid colour to the safe fallback (no CSS injection)', () => {
    const html = buildLandingPreviewHtml({
      ...base,
      brand: { ...base.brand, primary: 'red; } body { display:none' },
    });
    expect(html).toContain('--primary:#1E40AF');
    expect(html).not.toContain('display:none');
  });

  it('is script-free (safe for sandboxed srcDoc)', () => {
    const html = buildLandingPreviewHtml({ ...base, bannerUrl: 'https://cdn.example.com/x.webp' });
    expect(html).not.toContain('<script');
  });
});
