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
 *
 * XSS note: JSON-LD is rendered inside <script type="application/ld+json">.
 * Browsers treat this as raw text (not HTML), so DOMPurify is not applicable.
 * Defense instead targets JSON-injection: `</script>` sequences are escaped
 * so a crafted JSON value cannot prematurely close the script tag.
 */

/**
 * Escapes `</script>` (and `<!--`) in a JSON string so an injected value
 * cannot break out of the enclosing <script> tag.
 */
function escapeScriptContent(raw: string): string {
  return raw
    .replace(/<\/script/gi, '<\\/script')
    .replace(/<!--/g, '<\\!--');
}

export function JsonLd(props: { json: string } | { data: Record<string, unknown> }) {
  const raw = 'json' in props ? props.json : JSON.stringify(props.data);
  const payload = escapeScriptContent(raw);
  return (
    <script
      type="application/ld+json"
      dangerouslySetInnerHTML={{ __html: payload }}
    />
  );
}
