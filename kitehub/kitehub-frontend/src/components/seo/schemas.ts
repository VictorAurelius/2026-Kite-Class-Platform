/**
 * Canonical Schema.org JSON-LD builders for KiteHub public site.
 *
 * Use these helpers via the `<JsonLd>` component to inject structured data
 * into server-rendered pages. All schemas follow schema.org 2024 types.
 *
 * @see https://schema.org
 * @since Wave 9 (GAP-190)
 */

const SITE_URL = 'https://kitehub.vn';
const SITE_NAME = 'KiteHub';
const LOGO_URL = `${SITE_URL}/logo.png`;
const DEFAULT_LOCALE = 'vi-VN';

/**
 * Organization schema — identifies KiteHub as a legal entity.
 *
 * Wire once on the landing page (and any "about" page if added later).
 * Google surfaces this in knowledge panels and sitelinks search box.
 */
export function organizationSchema(): Record<string, unknown> {
  return {
    '@context': 'https://schema.org',
    '@type': 'Organization',
    '@id': `${SITE_URL}/#organization`,
    name: SITE_NAME,
    legalName: 'KiteHub JSC',
    url: SITE_URL,
    logo: LOGO_URL,
    description:
      'Nền tảng SaaS quản lý trung tâm giáo dục: website, tuyển sinh, học phí, AI Branding cho thị trường Việt Nam.',
    foundingDate: '2026',
    areaServed: {
      '@type': 'Country',
      name: 'Vietnam',
    },
    sameAs: [
      // TODO(GAP-174): add official social profiles once marketing validates
    ],
    contactPoint: [
      {
        '@type': 'ContactPoint',
        contactType: 'customer support',
        email: 'support@kitehub.vn',
        availableLanguage: ['Vietnamese', 'English'],
      },
    ],
  };
}

/**
 * WebSite schema — enables sitelinks search box in Google SERP.
 */
export function websiteSchema(): Record<string, unknown> {
  return {
    '@context': 'https://schema.org',
    '@type': 'WebSite',
    '@id': `${SITE_URL}/#website`,
    url: SITE_URL,
    name: SITE_NAME,
    inLanguage: DEFAULT_LOCALE,
    publisher: { '@id': `${SITE_URL}/#organization` },
    potentialAction: {
      '@type': 'SearchAction',
      target: {
        '@type': 'EntryPoint',
        urlTemplate: `${SITE_URL}/search?q={search_term_string}`,
      },
      'query-input': 'required name=search_term_string',
    },
  };
}

/**
 * FAQPage schema — wraps a list of Q&A pairs.
 *
 * Eligible for the "People also ask" rich result in Google SERP.
 * Use on /pricing (billing FAQ) and any future /faq page.
 */
export function faqPageSchema(
  faqs: ReadonlyArray<{ q: string; a: string }>
): Record<string, unknown> {
  return {
    '@context': 'https://schema.org',
    '@type': 'FAQPage',
    mainEntity: faqs.map((faq) => ({
      '@type': 'Question',
      name: faq.q,
      acceptedAnswer: {
        '@type': 'Answer',
        text: faq.a,
      },
    })),
  };
}

/**
 * BreadcrumbList schema — helps Google render breadcrumb trails in SERP.
 *
 * Pass items from root to leaf. The last item is the current page.
 */
export function breadcrumbListSchema(
  items: ReadonlyArray<{ name: string; path: string }>
): Record<string, unknown> {
  return {
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: items.map((item, index) => ({
      '@type': 'ListItem',
      position: index + 1,
      name: item.name,
      item: item.path.startsWith('http') ? item.path : `${SITE_URL}${item.path}`,
    })),
  };
}

export interface BlogPostingInput {
  slug: string;
  title: string;
  description: string;
  author: string;
  datePublished: string; // ISO date
  dateModified?: string;
  image?: string;
  tags?: string[];
}

/**
 * BlogPosting schema — individual blog article.
 *
 * Wire on /blog/[slug] via generateMetadata flow or server component.
 * Article rich result requires: headline, author, datePublished, publisher, image.
 */
export function blogPostingSchema(post: BlogPostingInput): Record<string, unknown> {
  const url = `${SITE_URL}/blog/${post.slug}`;
  return {
    '@context': 'https://schema.org',
    '@type': 'BlogPosting',
    '@id': `${url}#blogposting`,
    mainEntityOfPage: { '@type': 'WebPage', '@id': url },
    url,
    headline: post.title,
    description: post.description,
    inLanguage: DEFAULT_LOCALE,
    datePublished: post.datePublished,
    dateModified: post.dateModified ?? post.datePublished,
    author: {
      '@type': 'Person',
      name: post.author,
    },
    publisher: {
      '@type': 'Organization',
      name: SITE_NAME,
      logo: {
        '@type': 'ImageObject',
        url: LOGO_URL,
      },
    },
    image: post.image ? `${SITE_URL}${post.image}` : `${SITE_URL}/og-image.png`,
    keywords: post.tags?.join(', '),
  };
}
