/**
 * Public landing page (homepage).
 * Renders dynamic sections based on template config.
 *
 * Architecture:
 * - Data: GET /api/v1/tenants/{tenantId}/landing
 * - Template: Personal (7 sections) or Organization (13 sections)
 * - Theme: CSS Variables from ThemeSync + AI Branding
 *
 * @author KiteClass Team
 * @since PR-THEME-2
 */

import { Metadata } from 'next';
import { headers } from 'next/headers';
import { publicApi } from '@/lib/api/public';
import { ThemeSync } from '@/components/theme/ThemeSync';
import { TemplateRenderer, type SectionSlotMap } from '@/components/sections/TemplateRenderer';
import { getTemplate } from '@/lib/template/configs';
import { OrganizationJsonLd } from '@/components/seo/JsonLd';
import { NotFoundTenant } from '@/components/tenant/NotFoundTenant';

const getLandingPageData = async (tenantOverride?: string) => {
  try {
    // Resolve tenant in priority order:
    // 1. ?tenant= dev/preview override (page-scoped searchParams)
    // 2. x-tenant-id header injected by host→tenant middleware (GAP-811/GAP-1077)
    // 3. NEXT_PUBLIC_TENANT_ID (1-tenant-per-deploy fallback)
    // 4. hardcoded default tenant
    const headerTenantId = (await headers()).get('x-tenant-id') ?? undefined;
    const tenantId: string = tenantOverride
      ?? headerTenantId
      ?? process.env.NEXT_PUBLIC_TENANT_ID
      ?? '11111111-1111-1111-1111-111111111111';

    const response = await publicApi.getLandingPage(tenantId);
    return response;
  } catch (error) {
    console.error('Failed to fetch landing page data:', error);
    // Degraded fallback (backend unreachable). Anti-fabrication (GAP-958): NO fake
    // contact placeholders (`1900 xxxx` / `support@kiteclass.com`). Contact is left
    // null so contact surfaces hide rather than show a placeholder that misleads.
    return {
      heroTitle: 'Trung tâm giáo dục',
      heroSubtitle: undefined,
      heroImageUrl: null,
      tagline: undefined,
      primaryColor: '#3B82F6',
      secondaryColor: '#8B5CF6',
      contactEmail: undefined,
      contactPhone: undefined,
      address: undefined,
    };
  }
};

/**
 * Tenant display name for the landing page. Anti-fabrication (GAP-958) + Bucket B:
 * prefer the dedicated `centerName` field (when the backend supplies it) over the
 * marketing `heroTitle` slogan. Falls back to heroTitle, then a neutral generic.
 */
function resolveCenterName(ld: Record<string, unknown>): string {
  const centerName = typeof ld.centerName === 'string' ? ld.centerName.trim() : '';
  const heroTitle = typeof ld.heroTitle === 'string' ? ld.heroTitle.trim() : '';
  return centerName || heroTitle || 'Trung tâm giáo dục';
}

const APP_URL = process.env.NEXT_PUBLIC_APP_URL || 'https://kiteclass.com';

/**
 * Per-tenant SEO metadata (Bucket E / GAP-958). Title + description + OpenGraph
 * derive from the tenant's own name + tagline + logo — not a hardcoded KiteClass
 * brand. Canonical points at the tenant's own deploy URL (NEXT_PUBLIC_APP_URL).
 */
export async function generateMetadata({
  searchParams,
}: {
  searchParams: Promise<{ tenant?: string }>;
}): Promise<Metadata> {
  const { tenant } = await searchParams;

  // Unknown subdomain (middleware tried to resolve, BE 404 — GAP-1200): emit
  // generic metadata, don't fetch the env/default tenant's landing.
  if ((await headers()).get('x-tenant-not-found')) {
    return {
      title: 'Không tìm thấy trung tâm | KiteClass',
      description: 'Địa chỉ này chưa gắn với trung tâm nào trên KiteClass.',
      robots: { index: false, follow: false },
    };
  }

  const ld = (await getLandingPageData(tenant)) as Record<string, unknown>;
  const name = resolveCenterName(ld);
  const tagline = typeof ld.tagline === 'string' ? ld.tagline.trim() : '';
  const heroSubtitle = typeof ld.heroSubtitle === 'string' ? ld.heroSubtitle.trim() : '';
  const description =
    heroSubtitle || tagline || `${name} — đăng ký học, xem khóa học và lịch khai giảng.`;
  const logoUrl = typeof ld.logoUrl === 'string' ? ld.logoUrl : undefined;

  return {
    title: { default: name, template: `%s | ${name}` },
    description,
    alternates: { canonical: APP_URL },
    openGraph: {
      title: name,
      description,
      type: 'website',
      locale: 'vi_VN',
      siteName: name,
      url: APP_URL,
      // next/og opengraph-image (per-tenant) is the primary social card; an explicit
      // logo is added only when the tenant configured one.
      ...(logoUrl ? { images: [{ url: logoUrl, alt: name }] } : {}),
    },
    twitter: { card: 'summary_large_image', title: name, description },
  };
}

export default async function LandingPage({
  searchParams,
}: {
  searchParams: Promise<{ tenant?: string; template?: string; primary?: string; secondary?: string; accent?: string }>;
}) {
  const params = await searchParams;

  // Unknown subdomain (middleware resolved a real slug but BE returned 404 —
  // GAP-1200): render a friendly not-found page instead of silently falling
  // back to the env/default tenant landing (which would show a different
  // center's brand + content). Localhost/IP without a subdomain never set this
  // header, so the dev/1-tenant-per-deploy fallback below is preserved.
  const notFoundSlug = (await headers()).get('x-tenant-not-found');
  if (notFoundSlug) {
    return <NotFoundTenant slug={notFoundSlug} />;
  }

  const landingData = await getLandingPageData(params.tenant);
  // Template type bound per-tenant (landing_pages.template_type): GV độc lập → 'personal'.
  // Query param ?template= override cho preview; cuối cùng fallback default (organization).
  const tenantTemplateType = (landingData as Record<string, unknown>).templateType as string | undefined;
  const template = getTemplate(params.template ?? tenantTemplateType);

  // Override colors from query params (for testing themes)
  if (params.primary) landingData.primaryColor = `#${params.primary}`;
  if (params.secondary) landingData.secondaryColor = `#${params.secondary}`;
  if (params.accent) (landingData as Record<string, unknown>).accentColor = `#${params.accent}`;

  // Build per-section slot data from the landing payload. Previously the renderer
  // received no `slots`, so every section fell back to hardcoded defaults and the
  // backend's heroImageUrl / teachers never rendered (root cause of "no images").
  //
  // Each map only emits a slot when the backend actually returned non-empty data,
  // so missing/null fields preserve each section's hardcoded VN default (backward
  // compat). Field names follow the GET /api/v1/tenants/{id}/landing contract.
  const ld = landingData as Record<string, unknown>;

  const nonEmptyArray = (v: unknown): Array<Record<string, unknown>> | undefined =>
    Array.isArray(v) && v.length > 0 ? (v as Array<Record<string, unknown>>) : undefined;

  // teachers: [{ name, subject, photoUrl?, credentials[] }] → TeachersSection items
  const teachers = nonEmptyArray(ld.teachers)?.map((t) => ({
    title: t.name as string,
    description: t.subject as string,
    image: t.photoUrl as string | undefined,
    items: (t.credentials as string[] | undefined) ?? [],
  }));

  // programs: [{ name, description, detail[] }] → CertificatesSection cards
  const programs = nonEmptyArray(ld.programs)?.map((p) => ({
    title: p.name as string,
    description: p.description as string,
    items: (p.detail as string[] | undefined) ?? [],
  }));

  // pricingTiers: [{ name, price, period, features[], highlighted }] → PricingSection plans
  const pricingTiers = nonEmptyArray(ld.pricingTiers)?.map((tier) => {
    const price = tier.price as string | undefined;
    const period = tier.period as string | undefined;
    return {
      title: tier.name as string,
      description: [price, period].filter(Boolean).join(' / '),
      items: (tier.features as string[] | undefined) ?? [],
    };
  });

  // testimonials: [{ author, role, content, rating }] → TestimonialsSection items
  const testimonials = nonEmptyArray(ld.testimonials)?.map((t) => ({
    title: t.author as string,
    description: t.role as string,
    items: [t.content as string],
  }));

  // faqs: [{ question, answer }] → FaqSection questions
  const faqs = nonEmptyArray(ld.faqs)?.map((f) => ({
    title: f.question as string,
    description: f.answer as string,
  }));

  // stats: [{ value, label }] → StatsSection items
  const stats = nonEmptyArray(ld.stats)?.map((s) => ({
    title: s.value as string,
    description: s.label as string,
  }));

  // landing-100 F-sections (GAP-1083). Backend stores each as a JSONB array already
  // shaped for the matching section component (ProblemSolution items / HowItWorks steps /
  // TrustStrip signals). Only emit a slot when the backend returned non-empty data, so a
  // null/empty field preserves the section's hardcoded VN default (generic platform copy,
  // not fabricated partner data — cf. GAP-958 empty-state spirit).

  // problemSolution: [{ title (pain), description (problem), items[0] (fix) }] → SlotItem[]
  const problemSolution = nonEmptyArray(ld.problemSolution)?.map((p) => ({
    title: p.title as string,
    description: p.description as string | undefined,
    items: (p.items as string[] | undefined) ?? [],
  }));

  // howItWorks: [{ title (step), description }] → SlotItem[]
  const howItWorks = nonEmptyArray(ld.howItWorks)?.map((s) => ({
    title: s.title as string,
    description: s.description as string | undefined,
  }));

  // trustStrip: [{ icon, title, description }] → SlotItem[]
  const trustStrip = nonEmptyArray(ld.trustStrip)?.map((sig) => ({
    icon: sig.icon as string | undefined,
    title: sig.title as string,
    description: sig.description as string | undefined,
  }));

  const aboutText = (typeof ld.aboutText === 'string' && ld.aboutText.trim())
    ? (ld.aboutText as string)
    : undefined;

  // Hero CTA slots (Đợt-1 Hero slot C, wired here). Optional tenant-configured CTA
  // labels/hrefs; when the backend doesn't supply them HeroSection falls back to its
  // built-in real routes (/register, /catalog) — no fabricated content emitted.
  const str = (v: unknown): string | undefined =>
    typeof v === 'string' && v.trim() ? (v as string) : undefined;

  // Hero banner carousel (GAP-826). Emit the `images` slot only when the backend returned
  // a non-empty array of URL strings; otherwise HeroSection falls back to the single `image`
  // slot (heroImageUrl) → single-banner behaviour unchanged (backward-compat).
  const heroImages = Array.isArray(ld.heroImages)
    ? (ld.heroImages.filter((u): u is string => typeof u === 'string' && u.trim().length > 0))
    : undefined;

  const heroSlot: Record<string, string | string[] | undefined> = {
    image: ld.heroImageUrl as string | undefined,
    ...(heroImages && heroImages.length > 0 ? { images: heroImages } : {}),
    ...(str(ld.ctaPrimaryLabel) ? { ctaPrimaryLabel: str(ld.ctaPrimaryLabel) } : {}),
    ...(str(ld.ctaPrimaryHref) ? { ctaPrimaryHref: str(ld.ctaPrimaryHref) } : {}),
    ...(str(ld.ctaSecondaryLabel) ? { ctaSecondaryLabel: str(ld.ctaSecondaryLabel) } : {}),
    ...(str(ld.ctaSecondaryHref) ? { ctaSecondaryHref: str(ld.ctaSecondaryHref) } : {}),
  };

  const slots: SectionSlotMap = {
    hero: heroSlot,
    ...(problemSolution ? { problemSolution: { items: problemSolution } } : {}),
    ...(howItWorks ? { howItWorks: { steps: howItWorks } } : {}),
    ...(trustStrip ? { trustStrip: { signals: trustStrip } } : {}),
    ...(aboutText ? { about: { content: aboutText } } : {}),
    ...(teachers ? { teachers: { teachers } } : {}),
    ...(programs ? { certificates: { certificates: programs } } : {}),
    ...(pricingTiers ? { pricing: { plans: pricingTiers } } : {}),
    ...(testimonials ? { testimonials: { testimonials } } : {}),
    ...(faqs ? { faq: { questions: faqs } } : {}),
    ...(stats ? { stats: { stats } } : {}),
  };

  return (
    <>
      <OrganizationJsonLd
        name={resolveCenterName(ld)}
        description={landingData.heroSubtitle || landingData.tagline || ''}
        url={APP_URL}
        email={landingData.contactEmail}
        telephone={landingData.contactPhone}
        address={landingData.address}
      />

      <ThemeSync
        primaryColor={landingData.primaryColor}
        secondaryColor={landingData.secondaryColor}
        accentColor={(landingData as Record<string, unknown>).accentColor as string | undefined}
      />

      <TemplateRenderer template={template} data={landingData} slots={slots} />
    </>
  );
}
