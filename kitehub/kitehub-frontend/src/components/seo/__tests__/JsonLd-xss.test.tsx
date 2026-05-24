/**
 * XSS / JSON-injection tests for JsonLd (Wave beta-readiness-1 Bucket A).
 *
 * JSON-LD is rendered inside <script type="application/ld+json">.
 * Browsers treat the content as raw text — DOMPurify is not applicable.
 * Defense targets JSON-injection: </script> sequences are escaped so a
 * crafted JSON value cannot prematurely close the script tag and inject
 * a new <script> block.
 *
 * The threat: a JSON value containing `</script><script>...` causes the
 * browser HTML parser to treat everything after `</script>` as raw HTML
 * and execute the injected script block.
 *
 * The fix: replace `</script` with `<\/script` (escaped solidus).
 * This is valid JSON string encoding and browsers will NOT close the tag.
 */

import { describe, it, expect } from 'vitest';
import { render } from '@/test/test-utils';
import { JsonLd } from '../JsonLd';

describe('JsonLd — JSON-injection defense (Bucket A)', () => {
  it('escapes </script> closing sequence in data object value', () => {
    const data = {
      '@context': 'https://schema.org',
      '@type': 'WebPage',
      // Attacker crafts a description containing </script> to close the
      // surrounding script tag and inject a new script block.
      description: 'Crafted: </script><script>alert("xss")</script>',
    };

    const { container } = render(<JsonLd data={data} />);
    const script = container.querySelector('script[type="application/ld+json"]');
    expect(script).toBeTruthy();

    // The closing tag must be escaped so the browser parser never sees </script>
    expect(script!.innerHTML).not.toContain('</script>');
    // The escaped form must be present instead
    expect(script!.innerHTML).toContain('<\\/script');
  });

  it('escapes </script> sequence in pre-stringified json prop', () => {
    const json = JSON.stringify({
      '@context': 'https://schema.org',
      '@type': 'WebPage',
      name: 'Injected: </script><script>alert(1)</script>',
    });

    const { container } = render(<JsonLd json={json} />);
    const script = container.querySelector('script[type="application/ld+json"]');
    expect(script).toBeTruthy();

    // Closing tag must be escaped — browser parser cannot inject new script
    expect(script!.innerHTML).not.toContain('</script>');
    expect(script!.innerHTML).toContain('<\\/script');
  });

  it('escapes HTML comment opener <!-- to prevent legacy parser tricks', () => {
    const data = {
      '@context': 'https://schema.org',
      '@type': 'WebPage',
      name: '<!-- legacy comment injection attempt -->',
    };

    const { container } = render(<JsonLd data={data} />);
    const script = container.querySelector('script[type="application/ld+json"]');
    expect(script).toBeTruthy();

    // <!-- must not appear verbatim
    expect(script!.innerHTML).not.toContain('<!--');
  });

  it('leaves normal JSON values untouched', () => {
    const data = {
      '@context': 'https://schema.org',
      '@type': 'SoftwareApplication',
      name: 'KiteHub',
      applicationCategory: 'BusinessApplication',
    };

    const { container } = render(<JsonLd data={data} />);
    const script = container.querySelector('script[type="application/ld+json"]');
    expect(script).toBeTruthy();

    // Normal values must survive as valid JSON
    const parsed = JSON.parse(script!.innerHTML);
    expect(parsed.name).toBe('KiteHub');
    expect(parsed.applicationCategory).toBe('BusinessApplication');
  });

  it('does not break round-trip JSON for safe pre-stringified payload', () => {
    const json = JSON.stringify({
      '@context': 'https://schema.org',
      name: 'Safe title',
    });

    const { container } = render(<JsonLd json={json} />);
    const script = container.querySelector('script[type="application/ld+json"]');
    expect(script).toBeTruthy();

    // Must still be parseable valid JSON after the escape pass
    const parsed = JSON.parse(script!.innerHTML);
    expect(parsed.name).toBe('Safe title');
  });
});
