import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import { JsonLd, OrganizationJsonLd, CourseJsonLd } from '../JsonLd';

describe('JsonLd', () => {
  it('renders script tag with @context and data', () => {
    const { container } = render(
      <JsonLd data={{ '@type': 'Thing', name: 'Test' }} />
    );
    const script = container.querySelector('script[type="application/ld+json"]');
    expect(script).not.toBeNull();

    const parsed = JSON.parse(script!.textContent!);
    expect(parsed['@context']).toBe('https://schema.org');
    expect(parsed['@type']).toBe('Thing');
    expect(parsed.name).toBe('Test');
  });

  it('escapes </script> in values to prevent JSON-injection breakout (GAP-829)', () => {
    const { container } = render(
      <JsonLd data={{ '@type': 'Thing', name: 'evil</script><script>alert(1)</script>' }} />
    );
    const script = container.querySelector('script[type="application/ld+json"]')!;
    // Raw payload must NOT contain a literal </script sequence (would close the tag early)
    expect(script.innerHTML).not.toMatch(/<\/script/i);
    // ...yet JSON still parses back to the original value (\/ is a valid JSON escape)
    const parsed = JSON.parse(script.textContent!);
    expect(parsed.name).toBe('evil</script><script>alert(1)</script>');
  });
});

describe('OrganizationJsonLd', () => {
  it('renders EducationalOrganization schema', () => {
    const { container } = render(
      <OrganizationJsonLd
        name="KiteClass"
        description="Education platform"
        url="https://kiteclass.com"
        email="test@kiteclass.com"
        telephone="1900xxxx"
        address="Ha Noi"
      />
    );
    const script = container.querySelector('script[type="application/ld+json"]');
    const parsed = JSON.parse(script!.textContent!);

    expect(parsed['@type']).toBe('EducationalOrganization');
    expect(parsed.name).toBe('KiteClass');
    expect(parsed.email).toBe('test@kiteclass.com');
    expect(parsed.address['@type']).toBe('PostalAddress');
    expect(parsed.address.addressCountry).toBe('VN');
  });

  it('omits optional fields when not provided', () => {
    const { container } = render(
      <OrganizationJsonLd
        name="KiteClass"
        description="Test"
        url="https://kiteclass.com"
      />
    );
    const parsed = JSON.parse(
      container.querySelector('script[type="application/ld+json"]')!.textContent!
    );

    expect(parsed.email).toBeUndefined();
    expect(parsed.telephone).toBeUndefined();
    expect(parsed.address).toBeUndefined();
  });
});

describe('CourseJsonLd', () => {
  it('renders Course schema with all fields', () => {
    const { container } = render(
      <CourseJsonLd
        name="English Advanced"
        description="Advanced English course"
        provider="KiteClass"
        url="https://kiteclass.com/courses/1"
        price={5000000}
        priceCurrency="VND"
        duration="PT12W"
        level="Advanced"
      />
    );
    const parsed = JSON.parse(
      container.querySelector('script[type="application/ld+json"]')!.textContent!
    );

    expect(parsed['@type']).toBe('Course');
    expect(parsed.name).toBe('English Advanced');
    expect(parsed.provider['@type']).toBe('EducationalOrganization');
    expect(parsed.provider.name).toBe('KiteClass');
    expect(parsed.offers.price).toBe(5000000);
    expect(parsed.offers.priceCurrency).toBe('VND');
    expect(parsed.timeRequired).toBe('PT12W');
    expect(parsed.educationalLevel).toBe('Advanced');
  });

  it('omits price when not provided', () => {
    const { container } = render(
      <CourseJsonLd
        name="Basic Course"
        description="Test"
        provider="KiteClass"
        url="https://kiteclass.com/courses/2"
      />
    );
    const parsed = JSON.parse(
      container.querySelector('script[type="application/ld+json"]')!.textContent!
    );

    expect(parsed.offers).toBeUndefined();
    expect(parsed.timeRequired).toBeUndefined();
  });
});
