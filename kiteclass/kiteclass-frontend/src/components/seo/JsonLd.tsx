/**
 * Reusable JSON-LD structured data component.
 * Renders schema.org structured data for SEO.
 *
 * @since 2026-03-24
 */

interface JsonLdProps {
  data: Record<string, unknown>;
}

export function JsonLd({ data }: JsonLdProps) {
  return (
    <script
      type="application/ld+json"
      dangerouslySetInnerHTML={{
        __html: JSON.stringify({
          '@context': 'https://schema.org',
          ...data,
        }),
      }}
    />
  );
}

/**
 * EducationalOrganization structured data.
 */
export function OrganizationJsonLd({
  name,
  description,
  url,
  email,
  telephone,
  address,
}: {
  name: string;
  description: string;
  url: string;
  email?: string;
  telephone?: string;
  address?: string;
}) {
  return (
    <JsonLd
      data={{
        '@type': 'EducationalOrganization',
        name,
        description,
        url,
        ...(email && { email }),
        ...(telephone && { telephone }),
        ...(address && {
          address: {
            '@type': 'PostalAddress',
            addressLocality: address,
            addressCountry: 'VN',
          },
        }),
      }}
    />
  );
}

/**
 * Course structured data for course detail pages.
 */
export function CourseJsonLd({
  name,
  description,
  provider,
  url,
  price,
  priceCurrency = 'VND',
  duration,
  level,
}: {
  name: string;
  description: string;
  provider: string;
  url: string;
  price?: number;
  priceCurrency?: string;
  duration?: string;
  level?: string;
}) {
  return (
    <JsonLd
      data={{
        '@type': 'Course',
        name,
        description,
        provider: {
          '@type': 'EducationalOrganization',
          name: provider,
        },
        url,
        ...(price !== undefined && {
          offers: {
            '@type': 'Offer',
            price,
            priceCurrency,
            availability: 'https://schema.org/InStock',
          },
        }),
        ...(duration && { timeRequired: duration }),
        ...(level && { educationalLevel: level }),
      }}
    />
  );
}
