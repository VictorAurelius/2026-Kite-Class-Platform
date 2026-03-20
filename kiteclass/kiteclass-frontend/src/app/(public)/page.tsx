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
import { publicApi } from '@/lib/api/public';
import { ThemeSync } from '@/components/theme/ThemeSync';
import { TemplateRenderer } from '@/components/sections/TemplateRenderer';
import { getTemplate } from '@/lib/template/configs';

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
    const tenantId: string = tenantOverride
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
      contactEmail: 'support@kiteclass.com',
      contactPhone: '1900 xxxx',
      address: 'Hà Nội, Việt Nam',
    };
  }
};

export default async function LandingPage({
  searchParams,
}: {
  searchParams: Promise<{ tenant?: string; template?: string }>;
}) {
  const params = await searchParams;
  const landingData = await getLandingPageData(params.tenant);
  const template = getTemplate(params.template);

  // SEO structured data
  const structuredData = {
    '@context': 'https://schema.org',
    '@type': 'EducationalOrganization',
    name: 'KiteClass',
    description: landingData.heroSubtitle,
    url: process.env.NEXT_PUBLIC_APP_URL || 'https://kiteclass.com',
    email: landingData.contactEmail,
    telephone: landingData.contactPhone,
    address: {
      '@type': 'PostalAddress',
      addressLocality: landingData.address || 'Hà Nội',
      addressCountry: 'VN',
    },
  };

  return (
    <>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(structuredData) }}
      />

      <ThemeSync
        primaryColor={landingData.primaryColor}
        secondaryColor={landingData.secondaryColor}
      />

      <TemplateRenderer template={template} data={landingData} />
    </>
  );
}
