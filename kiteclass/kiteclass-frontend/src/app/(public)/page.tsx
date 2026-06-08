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

export const metadata: Metadata = {
  title: 'Trang chủ',
  description:
    'Hệ thống quản lý trung tâm tiếng Anh toàn diện với LMS, quản lý học viên, điểm danh tự động.',
  openGraph: {
    title: 'KiteClass - Quản lý Trung tâm Tiếng Anh Chuyên nghiệp',
    description:
      'Nền tảng quản lý toàn diện giúp tối ưu hóa vận hành trung tâm tiếng Anh.',
    type: 'website',
    locale: 'vi_VN',
  },
};

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
    return {
      heroTitle: 'Quản lý Trung tâm Tiếng Anh Chuyên nghiệp & Hiệu quả',
      heroSubtitle:
        'Nền tảng quản lý toàn diện giúp tối ưu hóa vận hành trung tâm tiếng Anh với LMS, quản lý học viên, điểm danh tự động và thanh toán online.',
      heroImageUrl: null,
      tagline: 'Nâng tầm giáo dục, tối ưu quản lý',
      primaryColor: '#3B82F6',
      secondaryColor: '#8B5CF6',
      contactEmail: process.env.NEXT_PUBLIC_CONTACT_EMAIL || 'support@kiteclass.com',
      contactPhone: process.env.NEXT_PUBLIC_CONTACT_PHONE || '1900 xxxx',
      address: 'Hà Nội, Việt Nam',
    };
  }
};

export default async function LandingPage({
  searchParams,
}: {
  searchParams: Promise<{ tenant?: string; template?: string; primary?: string; secondary?: string; accent?: string }>;
}) {
  const params = await searchParams;
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

  const aboutText = (typeof ld.aboutText === 'string' && ld.aboutText.trim())
    ? (ld.aboutText as string)
    : undefined;

  const slots: SectionSlotMap = {
    hero: { image: ld.heroImageUrl as string | undefined },
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
        name="KiteClass"
        description={landingData.heroSubtitle}
        url={process.env.NEXT_PUBLIC_APP_URL || 'https://kiteclass.com'}
        email={landingData.contactEmail}
        telephone={landingData.contactPhone}
        address={landingData.address || 'Hà Nội'}
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
