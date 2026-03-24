/**
 * Reusable JSON-LD structured data component for Schema.org markup.
 *
 * Renders a <script type="application/ld+json"> tag with the provided data.
 * Use this component to add structured data to any page for better SEO.
 *
 * @since SAAS-10
 */
export function JsonLd({ data }: { data: Record<string, unknown> }) {
  return (
    <script
      type="application/ld+json"
      dangerouslySetInnerHTML={{ __html: JSON.stringify(data) }}
    />
  );
}
