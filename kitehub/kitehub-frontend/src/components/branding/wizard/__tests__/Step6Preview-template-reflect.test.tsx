/**
 * GAP-1231 (un-skipped) — Step6Preview preview-source contract, rewritten for
 * GAP-1215 (preview-source = deploy-source).
 *
 * The old contract (#2279/#2289 era) asserted a per-template `<iframe srcDoc>`
 * HTML body with a `data-preview-template` marker — a SECOND render path that
 * drifted from the real landing (the WYSIWYG bug GAP-1215 fixes). The preview is
 * now the REAL kiteclass landing render path: `<iframe src>` → `/preview` themed
 * by the wizard's draft brand + selected palette variant.
 *
 * This locks in the §3a spirit ("preview reflects the user's choice → different
 * preview"): picking a different palette variant (GAP-1212) produces a visibly
 * different preview URL, and the live brand colours flow into that URL.
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { Step6Preview } from '../Step6Preview';
import { INITIAL_WIZARD_STATE, type WizardState } from '../wizard-shared';

// Stub heavy ThemePreview — irrelevant to the preview URL under test.
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
    currentStep: 5,
    tenantName: 'Trung tâm Toán Master',
    slug: 'toan-master',
    audience: 'exam-prep',
    tone: 'professional',
    templateId: templateId ?? '',
    jobId: 'job-test-001',
    approvedResources: ['logo', 'colors', 'banner', 'hero'],
  };
}

// Deterministic brand colours so the preview URL only varies by template/variant.
const BRAND = {
  primary: '#0F1E3D',
  secondary: '#F59E0B',
  background: '#FFFFFF',
  foreground: '#0F172A',
};

function renderPreview(templateId: string | null) {
  const Wrapper = makeWrapper();
  return render(
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
}

function previewSrc(templateId: string | null): string {
  const { unmount } = renderPreview(templateId);
  const iframe = screen.getByTestId('step6-preview-iframe');
  const src = iframe.getAttribute('src') ?? '';
  unmount();
  return src;
}

describe('Step6Preview — preview = landing render path (GAP-1215/1231)', () => {
  it('iframe src targets the real kiteclass /preview route', () => {
    const src = previewSrc('template-t1-navy-focus');
    expect(src).toContain('/preview?');
    expect(src).not.toContain('srcdoc');
  });

  it('carries the live brand colours into the preview URL', () => {
    const u = new URL(previewSrc('template-t1-navy-focus'));
    // paletteVariants normalises to lowercase; bareHex strips the leading '#'.
    expect(u.searchParams.get('primary')).toBe('0f1e3d');
    expect(u.searchParams.get('secondary')).toBe('f59e0b');
  });

  it('carries the draft org name into the preview URL', () => {
    const u = new URL(previewSrc('template-t1-navy-focus'));
    expect(u.searchParams.get('orgName')).toBe('Trung tâm Toán Master');
  });

  it('reflects the selected template — different template → different preview URL', () => {
    const t1 = previewSrc('template-t1-navy-focus');
    const t2 = previewSrc('template-t2-score-board');
    expect(t1).not.toBe(t2);
  });

  it('multi-variant pick re-themes the preview (variant B changes the URL palette)', () => {
    const { unmount } = renderPreview('template-t1-navy-focus');
    const iframe = screen.getByTestId('step6-preview-iframe');
    const before = new URL(iframe.getAttribute('src') ?? '');
    expect(before.searchParams.get('primary')).toBe('0f1e3d'); // variant A (base)

    // 3 variant cards rendered + variant B is a real alternative.
    expect(screen.getByTestId('step6-variant-a')).toBeInTheDocument();
    const variantB = screen.getByTestId('step6-variant-b');
    expect(screen.getByTestId('step6-variant-c')).toBeInTheDocument();

    fireEvent.click(variantB);

    const after = new URL(iframe.getAttribute('src') ?? '');
    expect(after.searchParams.get('primary')).not.toBe('0f1e3d'); // re-themed
    expect(screen.getByTestId('step6-variant-b').getAttribute('data-selected')).toBe('true');
    unmount();
  });
});
