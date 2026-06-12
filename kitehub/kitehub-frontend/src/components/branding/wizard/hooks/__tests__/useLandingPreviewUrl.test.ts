import { describe, it, expect } from 'vitest';
import { buildLandingPreviewUrl } from '../useLandingPreviewUrl';

describe('buildLandingPreviewUrl (GAP-1215 preview = landing render path)', () => {
  const base = { baseUrl: 'http://localhost:3000', primary: '#2563EB', secondary: '#F59E0B', accent: '#10B981' };

  it('targets the framing-allowed /preview route', () => {
    const url = buildLandingPreviewUrl(base);
    expect(url.startsWith('http://localhost:3000/preview?')).toBe(true);
  });

  it('strips the leading # from colours (landing reads `#${param}`)', () => {
    const u = new URL(buildLandingPreviewUrl(base));
    expect(u.searchParams.get('primary')).toBe('2563EB');
    expect(u.searchParams.get('secondary')).toBe('F59E0B');
    expect(u.searchParams.get('accent')).toBe('10B981');
  });

  it('url-encodes the draft org name', () => {
    const u = new URL(buildLandingPreviewUrl({ ...base, orgName: 'Trung tâm Toán Master' }));
    expect(u.searchParams.get('orgName')).toBe('Trung tâm Toán Master');
    expect(buildLandingPreviewUrl({ ...base, orgName: 'Trung tâm Toán Master' })).toContain('orgName=Trung+t%C3%A2m');
  });

  it('includes heroImage / logo / template / tenant when present, omits when absent', () => {
    const withAll = new URL(
      buildLandingPreviewUrl({
        ...base,
        templateType: 'organization',
        tenant: 't-1',
        logoUrl: 'http://localhost:9100/logo.svg',
        heroImage: 'http://localhost:9100/banner.webp',
      }),
    );
    expect(withAll.searchParams.get('template')).toBe('organization');
    expect(withAll.searchParams.get('tenant')).toBe('t-1');
    expect(withAll.searchParams.get('logo')).toBe('http://localhost:9100/logo.svg');
    expect(withAll.searchParams.get('heroImage')).toBe('http://localhost:9100/banner.webp');

    const minimal = new URL(buildLandingPreviewUrl(base));
    expect(minimal.searchParams.has('heroImage')).toBe(false);
    expect(minimal.searchParams.has('logo')).toBe(false);
    expect(minimal.searchParams.has('orgName')).toBe(false);
  });

  it('produces a different url per template (preview reflects template choice)', () => {
    const personal = buildLandingPreviewUrl({ ...base, templateType: 'personal' });
    const org = buildLandingPreviewUrl({ ...base, templateType: 'organization' });
    expect(personal).not.toBe(org);
  });

  it('produces a different url per palette variant (multi-variant pick)', () => {
    const a = buildLandingPreviewUrl({ ...base, primary: '#2563EB' });
    const b = buildLandingPreviewUrl({ ...base, primary: '#0EA5E9' });
    expect(a).not.toBe(b);
  });

  it('falls back to localhost:3000 when no baseUrl supplied', () => {
    const url = buildLandingPreviewUrl({ primary: '#2563EB', secondary: '#F59E0B', accent: '#10B981' });
    expect(url).toContain('http://localhost:3000/preview');
  });
});
