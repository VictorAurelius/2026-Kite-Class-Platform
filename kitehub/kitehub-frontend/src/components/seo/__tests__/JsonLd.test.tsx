/**
 * Tests for JsonLd structured data component.
 *
 * @since SAAS-10
 */

import { describe, it, expect } from 'vitest';
import { render } from '@/test/test-utils';
import { JsonLd } from '../JsonLd';

describe('JsonLd', () => {
  it('renders a script tag with type application/ld+json', () => {
    const data = {
      '@context': 'https://schema.org',
      '@type': 'Organization',
      name: 'KiteHub',
    };

    const { container } = render(<JsonLd data={data} />);
    const script = container.querySelector('script[type="application/ld+json"]');

    expect(script).toBeInTheDocument();
  });

  it('serializes data as JSON in the script content', () => {
    const data = {
      '@context': 'https://schema.org',
      '@type': 'SoftwareApplication',
      name: 'KiteHub',
      applicationCategory: 'BusinessApplication',
    };

    const { container } = render(<JsonLd data={data} />);
    const script = container.querySelector('script[type="application/ld+json"]');

    expect(script).toBeTruthy();
    const parsed = JSON.parse(script!.innerHTML);
    expect(parsed['@context']).toBe('https://schema.org');
    expect(parsed['@type']).toBe('SoftwareApplication');
    expect(parsed.name).toBe('KiteHub');
    expect(parsed.applicationCategory).toBe('BusinessApplication');
  });

  it('handles nested objects correctly', () => {
    const data = {
      '@context': 'https://schema.org',
      '@type': 'SoftwareApplication',
      name: 'KiteHub',
      offers: {
        '@type': 'AggregateOffer',
        lowPrice: '0',
        priceCurrency: 'VND',
      },
    };

    const { container } = render(<JsonLd data={data} />);
    const script = container.querySelector('script[type="application/ld+json"]');

    const parsed = JSON.parse(script!.innerHTML);
    expect(parsed.offers['@type']).toBe('AggregateOffer');
    expect(parsed.offers.lowPrice).toBe('0');
    expect(parsed.offers.priceCurrency).toBe('VND');
  });

  it('renders empty object without errors', () => {
    const { container } = render(<JsonLd data={{}} />);
    const script = container.querySelector('script[type="application/ld+json"]');

    expect(script).toBeInTheDocument();
    expect(JSON.parse(script!.innerHTML)).toEqual({});
  });
});
