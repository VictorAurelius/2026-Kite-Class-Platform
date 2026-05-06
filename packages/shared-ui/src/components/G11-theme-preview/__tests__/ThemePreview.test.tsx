/**
 * G11 ThemePreview — RTL coverage of the 5 spec'd states (default,
 * brand-applied, dark-morph, mobile-preview, wcag-warning) + the reflexive
 * coverage test (component shows + fixes its own contrast violation).
 *
 * Spec source: `ui_kits/components/G11-theme-preview/README.md` + 5 HTML
 * state files under `states/`. Vietnamese-only labels per CLAUDE.md.
 */

import { describe, expect, it } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { ThemePreview } from '../ThemePreview';
import { calculateContrast, WCAG_AA_NORMAL } from '../utils';
import type { BrandColors } from '../types';

// Passing AA brand pair (black-on-white surface) — used by states that should
// render WITHOUT a contrast warning.
const PASSING_BRAND: BrandColors = {
  primary: '#2563eb',
  secondary: '#7c3aed',
  background: '#ffffff',
  foreground: '#0f172a',
};

// Failing AA brand pair from `states/wcag-warning.html` — ~3.2:1.
const FAILING_BRAND: BrandColors = {
  primary: '#fbbf24',
  secondary: '#7c3aed',
  background: '#fef3c7',
  foreground: '#fbbf24',
};

describe('ThemePreview — 5 spec states', () => {
  it('default state — passing pair, light mode, no warning', () => {
    render(<ThemePreview brandColors={PASSING_BRAND} initialMode="light" />);
    expect(screen.getByText('Xem trước giao diện')).toBeInTheDocument();
    expect(screen.queryByTestId('theme-preview-warning')).not.toBeInTheDocument();
    expect(screen.getByTestId('theme-preview-aa-pill')).toBeInTheDocument();
    expect(screen.getByTestId('theme-preview-root').dataset.mode).toBe('light');
  });

  it('dark-morph state — passing pair, dark mode', () => {
    render(<ThemePreview brandColors={PASSING_BRAND} initialMode="dark" />);
    expect(screen.getByTestId('theme-preview-root').dataset.mode).toBe('dark');
    expect(screen.queryByTestId('theme-preview-warning')).not.toBeInTheDocument();
  });

  it('brand-applied state — before/after panels both render', () => {
    render(<ThemePreview brandColors={PASSING_BRAND} />);
    expect(screen.getByTestId('theme-preview-before')).toBeInTheDocument();
    expect(screen.getByTestId('theme-preview-after')).toBeInTheDocument();
  });

  it('wcag-warning state (light) — failing pair surfaces warning + autofix CTA', () => {
    render(<ThemePreview brandColors={FAILING_BRAND} initialMode="light" />);
    expect(screen.getByTestId('theme-preview-warning')).toBeInTheDocument();
    expect(screen.getByText('Cảnh báo độ tương phản')).toBeInTheDocument();
    expect(screen.getByTestId('theme-preview-autofix')).toBeInTheDocument();
  });

  it('wcag-warning state (dark) — failing pair still surfaces warning in dark mode', () => {
    render(<ThemePreview brandColors={FAILING_BRAND} initialMode="dark" />);
    expect(screen.getByTestId('theme-preview-warning')).toBeInTheDocument();
    expect(screen.getByTestId('theme-preview-root').dataset.mode).toBe('dark');
  });
});

describe('ThemePreview — light/dark toggle', () => {
  it('clicking the dark mode option flips data-mode', () => {
    render(<ThemePreview brandColors={PASSING_BRAND} initialMode="light" />);
    const root = screen.getByTestId('theme-preview-root');
    expect(root.dataset.mode).toBe('light');

    fireEvent.click(screen.getByTestId('theme-preview-mode-dark'));
    expect(root.dataset.mode).toBe('dark');

    fireEvent.click(screen.getByTestId('theme-preview-mode-light'));
    expect(root.dataset.mode).toBe('light');
  });
});

describe('ThemePreview — WCAG fail demonstration + reflexive auto-fix', () => {
  it('failing pair surfaces warning badge', () => {
    render(<ThemePreview brandColors={FAILING_BRAND} />);
    const warning = screen.getByTestId('theme-preview-warning');
    expect(warning).toBeInTheDocument();
    expect(warning).toHaveAttribute('role', 'alert');
    expect(warning).toHaveAttribute('aria-live', 'polite');
  });

  it('auto-fix CTA visible only when contrast fails', () => {
    const { unmount } = render(<ThemePreview brandColors={FAILING_BRAND} />);
    expect(screen.getByTestId('theme-preview-autofix')).toBeInTheDocument();
    unmount();

    render(<ThemePreview brandColors={PASSING_BRAND} />);
    expect(screen.queryByTestId('theme-preview-autofix')).not.toBeInTheDocument();
  });

  it('reflexive coverage — clicking auto-fix removes the warning AND applies AA-compliant pair', () => {
    render(<ThemePreview brandColors={FAILING_BRAND} />);

    // Pre-condition: warning visible because the brand pair fails AA.
    expect(screen.getByTestId('theme-preview-warning')).toBeInTheDocument();
    const startRatio = calculateContrast(
      FAILING_BRAND.foreground,
      FAILING_BRAND.background,
    );
    expect(startRatio).toBeLessThan(WCAG_AA_NORMAL);

    // Action: click auto-fix.
    fireEvent.click(screen.getByTestId('theme-preview-autofix'));

    // Post-condition: warning gone, AA pill visible, applied confirmation
    // surfaced. This is the reflexive guarantee — the same component that
    // demonstrates the WCAG fail also fixes it on its own preview surface.
    expect(screen.queryByTestId('theme-preview-warning')).not.toBeInTheDocument();
    expect(screen.getByTestId('theme-preview-aa-pill')).toBeInTheDocument();
    expect(screen.getByTestId('theme-preview-autofix-applied')).toBeInTheDocument();
  });
});

describe('ThemePreview — Vietnamese labels per spec README §28-37', () => {
  it('renders Vietnamese heading + mode labels', () => {
    render(<ThemePreview brandColors={PASSING_BRAND} />);
    expect(screen.getByText('Xem trước giao diện')).toBeInTheDocument();
    expect(screen.getByText('Sáng')).toBeInTheDocument();
    expect(screen.getByText('Tối')).toBeInTheDocument();
  });

  it('renders Vietnamese warning text on failing pair', () => {
    render(<ThemePreview brandColors={FAILING_BRAND} />);
    expect(screen.getByText('Cảnh báo độ tương phản')).toBeInTheDocument();
    expect(screen.getByText('Tự động sửa')).toBeInTheDocument();
  });

  it('default lang attribute is "vi"', () => {
    render(<ThemePreview brandColors={PASSING_BRAND} />);
    expect(screen.getByTestId('theme-preview-root')).toHaveAttribute('lang', 'vi');
  });
});

describe('ThemePreview — accessibility', () => {
  it('mode toggle is a radiogroup with aria-checked per option', () => {
    render(<ThemePreview brandColors={PASSING_BRAND} initialMode="light" />);
    const group = screen.getByRole('radiogroup');
    expect(group).toHaveAttribute('aria-label', 'Chọn chế độ xem');

    const lightRadio = screen.getByTestId('theme-preview-mode-light');
    const darkRadio = screen.getByTestId('theme-preview-mode-dark');
    expect(lightRadio).toHaveAttribute('aria-checked', 'true');
    expect(darkRadio).toHaveAttribute('aria-checked', 'false');
  });
});
