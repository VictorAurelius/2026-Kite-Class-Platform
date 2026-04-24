/**
 * Reusable JSON-LD structured data component for Schema.org markup.
 *
 * Renders a <script type="application/ld+json"> tag with the provided data.
 * Use this component to add structured data to any page for better SEO.
 *
 * Accepts EITHER:
 * - `json: string` — pre-stringified JSON (preferred, avoids RSC array serialization)
 * - `data: object` — schema object (will be JSON.stringify'd internally)
 *
 * GAP-204: Prefer pre-stringified form in server components. Passing objects
 * with nested arrays (e.g. `mainEntity: [...]`) triggers Next.js 15.1.7+ RSC
 * Array.toJSON regression during prerender.
 */
export function JsonLd(props: { json: string } | { data: Record<string, unknown> }) {
  const payload = 'json' in props ? props.json : JSON.stringify(props.data);
  return (
    <script
      type="application/ld+json"
      dangerouslySetInnerHTML={{ __html: payload }}
    />
  );
}
