/**
 * XSS sanitization tests for TemplateFullscreen (Wave beta-readiness-1 Bucket A).
 *
 * Verifies that DOMPurify with SVG_PURIFY_CONFIG strips malicious payloads
 * from template SVG before rendering into the DOM.
 *
 * NOTE: Radix UI Dialog uses a portal that mounts into document.body, NOT
 * into the container returned by render(). Queries use document.body.
 */

import { describe, it, expect } from 'vitest';
import { render } from '@/test/test-utils';
import { TemplateFullscreen } from '../TemplateFullscreen';
import type { TemplateDescriptor } from '../TemplateGrid';

const SAFE_SVG = `<svg viewBox="0 0 320 200" xmlns="http://www.w3.org/2000/svg">
  <rect width="320" height="200" fill="#0F1E3D"/>
  <text x="160" y="100" fill="white" font-size="20" text-anchor="middle">Safe</text>
</svg>`;

const makeTemplate = (svg: string): TemplateDescriptor => ({
  id: 'test-template',
  code: 'TX',
  name: 'Test Template',
  tag: 'Test',
  audiences: ['mixed'],
  tones: ['professional'],
  svg,
});

const noop = () => {};

describe('TemplateFullscreen — XSS sanitization (Bucket A)', () => {
  it('renders safe SVG content intact', () => {
    render(
      <TemplateFullscreen
        template={makeTemplate(SAFE_SVG)}
        onClose={noop}
        onConfirm={noop}
      />,
    );
    // Dialog mounts into document.body via portal
    const preview = document.body.querySelector('[data-testid="template-fullscreen-preview"]');
    expect(preview).toBeTruthy();
    expect(preview!.querySelector('svg')).toBeTruthy();
    expect(preview!.querySelector('rect')).toBeTruthy();
  });

  it('strips <script> tags from malicious SVG payload', () => {
    const maliciousSvg = `<svg viewBox="0 0 320 200" xmlns="http://www.w3.org/2000/svg">
      <rect width="320" height="200" fill="#fff"/>
      <script>alert('xss-script-tag')</script>
    </svg>`;

    render(
      <TemplateFullscreen
        template={makeTemplate(maliciousSvg)}
        onClose={noop}
        onConfirm={noop}
      />,
    );
    const preview = document.body.querySelector('[data-testid="template-fullscreen-preview"]');
    expect(preview!.querySelector('script')).toBeNull();
    // text content of script should not be present
    expect(preview!.innerHTML).not.toContain('alert(');
  });

  it('strips onerror inline event handler from SVG image element', () => {
    const maliciousSvg = `<svg viewBox="0 0 320 200" xmlns="http://www.w3.org/2000/svg">
      <image href="x" onerror="alert('xss-onerror')" width="100" height="100"/>
    </svg>`;

    render(
      <TemplateFullscreen
        template={makeTemplate(maliciousSvg)}
        onClose={noop}
        onConfirm={noop}
      />,
    );
    const preview = document.body.querySelector('[data-testid="template-fullscreen-preview"]');
    // onerror attribute must be stripped
    const imageEl = preview!.querySelector('image');
    expect(imageEl?.getAttribute('onerror')).toBeNull();
    expect(preview!.innerHTML).not.toContain('alert(');
  });

  it('strips onclick handler from SVG element', () => {
    const maliciousSvg = `<svg viewBox="0 0 320 200" xmlns="http://www.w3.org/2000/svg">
      <rect width="320" height="200" fill="red" onclick="document.cookie='stolen'"/>
    </svg>`;

    render(
      <TemplateFullscreen
        template={makeTemplate(maliciousSvg)}
        onClose={noop}
        onConfirm={noop}
      />,
    );
    const preview = document.body.querySelector('[data-testid="template-fullscreen-preview"]');
    const rect = preview!.querySelector('rect');
    expect(rect?.getAttribute('onclick')).toBeNull();
  });

  it('strips classic img-src-x-onerror payload embedded in SVG foreignObject', () => {
    const maliciousSvg = `<svg viewBox="0 0 320 200" xmlns="http://www.w3.org/2000/svg">
      <foreignObject width="320" height="200">
        <img src="x" onerror="alert('xss-img')" xmlns="http://www.w3.org/1999/xhtml"/>
      </foreignObject>
    </svg>`;

    render(
      <TemplateFullscreen
        template={makeTemplate(maliciousSvg)}
        onClose={noop}
        onConfirm={noop}
      />,
    );
    const preview = document.body.querySelector('[data-testid="template-fullscreen-preview"]');
    // DOMPurify strips the entire foreignObject (not in SVG+filters profile).
    // Verify neither the onerror attribute nor the alert payload survives.
    expect(preview!.querySelector('foreignObject')).toBeNull();
    expect(preview!.innerHTML).not.toContain('onerror');
    expect(preview!.innerHTML).not.toContain('alert(');
  });

  it('preserves legitimate SVG presentation attributes after sanitization', () => {
    render(
      <TemplateFullscreen
        template={makeTemplate(SAFE_SVG)}
        onClose={noop}
        onConfirm={noop}
      />,
    );
    const preview = document.body.querySelector('[data-testid="template-fullscreen-preview"]');
    const svg = preview!.querySelector('svg');
    expect(svg?.getAttribute('viewBox')).toBe('0 0 320 200');
    const rect = preview!.querySelector('rect');
    expect(rect?.getAttribute('fill')).toBe('#0F1E3D');
  });
});
