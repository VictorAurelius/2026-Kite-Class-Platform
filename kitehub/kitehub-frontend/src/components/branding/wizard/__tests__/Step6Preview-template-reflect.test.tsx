/**
 * GAP-272 §3a — Step6Preview "preview phải theo template" tests.
 *
 * Before the fix, buildPreviewHtml rendered ONE hard-coded hero+features layout
 * for every template (the template only changed a palette text label). These
 * tests lock in that picking a different template now produces a visibly
 * different `<iframe srcDoc>` body — each of the 6 wizard templates maps to its
 * own landing archetype (see renderTemplateBody in Step6Preview.tsx).
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { Step6Preview } from '../Step6Preview';
import { INITIAL_WIZARD_STATE, type WizardState } from '../wizard-shared';

// Stub heavy ThemePreview — irrelevant to the srcDoc composition under test.
vi.mock('@kite/shared-ui', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@kite/shared-ui')>();
  return { ...actual, ThemePreview: () => <div data-testid="theme-preview-stub" /> };
});

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });
  const Wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  Wrapper.displayName = 'TestQueryClientWrapper';
  return Wrapper;
}

function makeState(templateId: string | null): WizardState {
  return {
    ...INITIAL_WIZARD_STATE,
    currentStep: 6,
    tenantName: 'Trung tâm Toán Master',
    slug: 'toan-master',
    audience: 'exam-prep',
    tone: 'professional',
    templateId: templateId ?? '',
    jobId: 'job-test-001',
    approvedResources: ['logo', 'colors', 'banner', 'hero'],
  };
}

// Deterministic brand colours so the srcDoc only varies by template.
const BRAND = {
  primary: '#0F1E3D',
  secondary: '#F59E0B',
  background: '#FFFFFF',
  foreground: '#0F172A',
};

function renderSrcDoc(templateId: string | null): string {
  const Wrapper = makeWrapper();
  const { unmount } = render(
    <Wrapper>
      <Step6Preview
        wizardState={makeState(templateId)}
        dispatch={() => {}}
        brandColors={BRAND}
        onBack={() => {}}
        onDeploy={() => {}}
      />
    </Wrapper>,
  );
  const iframe = screen.getByTestId('step6-preview-iframe');
  const srcDoc = iframe.getAttribute('srcdoc') ?? '';
  unmount();
  return srcDoc;
}

describe('Step6Preview — preview reflects selected template (GAP-272 §3a)', () => {
  it('renders T1 Navy Focus body (centered exam hero)', () => {
    const html = renderSrcDoc('template-t1-navy-focus');
    expect(html).toContain('data-preview-template="template-t1-navy-focus"');
    expect(html).toContain('98% học viên đạt điểm 9+');
  });

  it('renders T2 Score Board body (score bars)', () => {
    const html = renderSrcDoc('template-t2-score-board');
    expect(html).toContain('data-preview-template="template-t2-score-board"');
    expect(html).toContain('Bảng điểm 2025');
    expect(html).toContain('t-bars');
  });

  it('renders T4 Result Stripes body (3 stat columns)', () => {
    const html = renderSrcDoc('template-t4-result-stripes');
    expect(html).toContain('data-preview-template="template-t4-result-stripes"');
    expect(html).toContain('điểm trung bình');
    expect(html).toContain('t-stat');
  });

  it('renders T5 Schedule Grid body (seats-left badges)', () => {
    const html = renderSrcDoc('template-t5-schedule-grid');
    expect(html).toContain('data-preview-template="template-t5-schedule-grid"');
    expect(html).toContain('Lịch khai giảng');
    expect(html).toContain('Còn 8 chỗ');
  });

  it('renders T6 Roadmap Vertical body (milestone timeline)', () => {
    const html = renderSrcDoc('template-t6-roadmap-vertical');
    expect(html).toContain('data-preview-template="template-t6-roadmap-vertical"');
    expect(html).toContain('Lộ trình 12 tháng');
  });

  it('falls back to a generic body when no template selected', () => {
    const html = renderSrcDoc(null);
    expect(html).toContain('data-preview-template="default"');
    expect(html).toContain('class="features"');
  });

  it('produces a DIFFERENT srcDoc per template (the core §3a guarantee)', () => {
    const t1 = renderSrcDoc('template-t1-navy-focus');
    const t2 = renderSrcDoc('template-t2-score-board');
    const t4 = renderSrcDoc('template-t4-result-stripes');
    const t6 = renderSrcDoc('template-t6-roadmap-vertical');
    const variants = new Set([t1, t2, t4, t6]);
    expect(variants.size).toBe(4); // all distinct — picking a template changes the preview
  });

  it('still carries live brand colours through every template body', () => {
    const html = renderSrcDoc('template-t4-result-stripes');
    expect(html).toContain('--primary:#0F1E3D');
    expect(html).toContain('--accent:#F59E0B');
  });
});
