'use client';

/**
 * G11 — Theme Customization Live Preview.
 *
 * Wizard step 5 of the AI Branding flow (`/branding/wizard`). Per
 * `.claude/rules/ai-branding-guidelines.md` §4.2 the preview is MANDATORY
 * before deploy; per §4.3 + §5 the WCAG fail demonstration is mandatory and
 * blocks deploy until the foreground/background pair reaches AA (≥4.5:1).
 *
 * Reflexive coverage (per the spec README §"WCAG warning — reflexive
 * coverage"): when the picker yields a failing pair, the component renders
 * the failing pair INSIDE the demonstration sandbox, surfaces the warning,
 * and offers an auto-fix CTA whose output passes AA. The same component
 * therefore SHOWS the violation it would catch — meta-correct + the
 * self-test demonstrates live remediation.
 *
 * Spec sources:
 *   - `dossier/04-component-gaps.md` §G11
 *   - `ui_kits/components/G11-theme-preview/README.md` (110 lines)
 *   - `ui_kits/components/G11-theme-preview/states/*.html` (5 spec'd states)
 *
 * No new deps — Tailwind tokens already shipped with consumer apps. The
 * light/dark toggle uses CSS class swap on the sandbox surface, NOT
 * `document.documentElement` mutation, so the wizard preview never bleeds
 * into the host page's theme.
 *
 * Accessibility (WCAG AA):
 *   - Mode toggle is `role="radiogroup"` with `aria-checked` per option.
 *   - Warning panel carries `role="alert"` + `aria-live="polite"`.
 *   - Auto-fix CTA is a real `<button>` with `aria-label`.
 *   - Sandbox surface carries `aria-label` so screen readers announce the
 *     preview region.
 */

import type React from 'react';
import { useMemo, useState } from 'react';
import type {
  BrandColors,
  ContrastWarning,
  ThemeMode,
  ThemePreviewProps,
} from './types';
import { WCAG_AA_NORMAL, calculateContrast, suggestFix } from './utils';

const COPY = {
  heading: 'Xem trước giao diện',
  modeLight: 'Sáng',
  modeDark: 'Tối',
  modeGroupLabel: 'Chọn chế độ xem',
  beforeLabel: 'Trước',
  afterLabel: 'Sau',
  sandboxLabel: 'Vùng xem trước trực tiếp',
  primaryLabel: 'Màu chính',
  secondaryLabel: 'Màu phụ',
  bgLabel: 'Nền',
  fgLabel: 'Chữ',
  warningTitle: 'Cảnh báo độ tương phản',
  warningBody: (ratio: number, required: number) =>
    `Tỉ lệ tương phản hiện tại là ${ratio.toFixed(2)}:1 — cần ≥ ${required.toFixed(1)}:1 để đạt WCAG AA.`,
  autoFixCta: 'Tự động sửa',
  autoFixApplied: 'Đã áp dụng cặp màu đạt AA',
  passingPill: (ratio: number) => `Đạt AA (${ratio.toFixed(2)}:1)`,
} as const;

/**
 * Compute current warning from the active brand colours, OR honour the
 * caller-supplied initial warning (used by tests / Storybook to demonstrate
 * the failing state without picking exact failing hexes).
 */
function deriveWarning(
  active: BrandColors,
  initial: ContrastWarning | undefined,
): ContrastWarning | undefined {
  if (initial) return initial;
  const ratio = calculateContrast(active.foreground, active.background);
  if (ratio >= WCAG_AA_NORMAL) return undefined;
  return {
    fg: active.foreground,
    bg: active.background,
    ratio,
    required: WCAG_AA_NORMAL,
  };
}

export function ThemePreview(props: ThemePreviewProps): React.JSX.Element {
  const {
    brandColors: initialColors,
    initialMode = 'light',
    initialWarning,
    lang = 'vi',
  } = props;

  // Mode toggle (light/dark) — owned by the component so the preview is
  // self-contained.
  const [mode, setMode] = useState<ThemeMode>(initialMode);

  // The actively previewed brand pair. Auto-fix replaces this with the
  // AA-compliant suggestion; the original colours stay visible in the
  // "Trước" panel for before/after comparison.
  const [activeColors, setActiveColors] = useState<BrandColors>(initialColors);
  const [autoFixed, setAutoFixed] = useState(false);

  const warning = useMemo(
    () => deriveWarning(activeColors, autoFixed ? undefined : initialWarning),
    [activeColors, autoFixed, initialWarning],
  );

  const currentRatio = useMemo(
    () => calculateContrast(activeColors.foreground, activeColors.background),
    [activeColors],
  );

  const handleAutoFix = () => {
    const fixed = suggestFix({
      foreground: activeColors.foreground,
      background: activeColors.background,
    });
    setActiveColors({
      ...activeColors,
      foreground: fixed.fg,
      background: fixed.bg,
    });
    setAutoFixed(true);
  };

  // Sandbox surfaces the ACTIVE pair on its own surface — this is the
  // reflexive demo. Light vs dark only swaps the wrapper background tone,
  // not the brand pair (which is the subject of measurement).
  const sandboxStyle: React.CSSProperties = {
    backgroundColor: activeColors.background,
    color: activeColors.foreground,
    transition: 'background-color 200ms ease, color 200ms ease',
  };

  // GAP-1148: the component wrote `dark:` utility variants on descendants
  // (swatch panels, toggle pills) but never set a local `.dark` ancestor, so
  // clicking "Tối" only swapped the wrapper tone — the dominant surfaces stayed
  // light → user perceived the toggle as "không bật được". Setting `dark` on the
  // root (consumer apps use Tailwind `darkMode: ['class']`) makes those existing
  // `dark:` variants fire, so the whole preview visibly flips light↔dark. This
  // is a LOCAL class on the preview root — it never bleeds into the host page.
  const wrapperClass =
    mode === 'dark'
      ? 'dark bg-slate-900 text-slate-100'
      : 'bg-slate-50 text-slate-900';

  return (
    <div
      data-testid="theme-preview-root"
      data-mode={mode}
      lang={lang}
      className={`min-h-full p-6 ${wrapperClass}`}
    >
      <header className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-semibold">{COPY.heading}</h2>

        {/* Mode toggle — light/dark side-by-side preview switch */}
        <div
          role="radiogroup"
          aria-label={COPY.modeGroupLabel}
          className="inline-flex rounded-full border border-current/20 bg-white/60 p-1 backdrop-blur dark:bg-slate-800/60"
        >
          {(['light', 'dark'] as const).map((m) => (
            <button
              key={m}
              type="button"
              role="radio"
              aria-checked={mode === m}
              data-testid={`theme-preview-mode-${m}`}
              onClick={() => setMode(m)}
              className={`rounded-full px-3 py-1 text-sm transition ${
                mode === m
                  ? 'bg-slate-900 text-white shadow-soft dark:bg-white dark:text-slate-900'
                  : 'text-current/70 hover:text-current'
              }`}
            >
              {m === 'light' ? COPY.modeLight : COPY.modeDark}
            </button>
          ))}
        </div>
      </header>

      {/* Before / After side-by-side — "Trước" pins the original picker
          output; "Sau" reflects the active (possibly auto-fixed) pair. */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <SwatchPanel
          label={COPY.beforeLabel}
          colors={initialColors}
          dataTestid="theme-preview-before"
        />
        <SwatchPanel
          label={COPY.afterLabel}
          colors={activeColors}
          dataTestid="theme-preview-after"
          highlight={autoFixed}
        />
      </div>

      {/* Live demo sandbox — renders the ACTIVE pair on its own surface so
          the contrast violation (or its absence) is visible to the eye. */}
      <section
        aria-label={COPY.sandboxLabel}
        data-testid="theme-preview-sandbox"
        className="mt-5 rounded-2xl border p-6 shadow-soft"
        style={sandboxStyle}
      >
        <p className="text-base font-semibold">
          KiteClass — {mode === 'dark' ? COPY.modeDark : COPY.modeLight}
        </p>
        <p className="mt-1 text-sm opacity-90">
          Đây là vùng xem trước theo bộ màu thương hiệu hiện tại. Mọi văn bản
          ở đây dùng cặp <code>{activeColors.foreground}</code> trên{' '}
          <code>{activeColors.background}</code>.
        </p>

        {!warning && (
          <span
            data-testid="theme-preview-aa-pill"
            className="mt-3 inline-flex items-center gap-2 rounded-full bg-emerald-500/15 px-3 py-1 text-xs font-medium text-emerald-600"
          >
            ● {COPY.passingPill(currentRatio)}
          </span>
        )}
      </section>

      {/* Reflexive WCAG warning — visible only when the active pair fails AA */}
      {warning && (
        <div
          role="alert"
          aria-live="polite"
          data-testid="theme-preview-warning"
          className="mt-5 rounded-xl border border-destructive/30 bg-destructive/5 p-4"
        >
          <p className="font-semibold text-destructive">{COPY.warningTitle}</p>
          <p className="mt-1 text-sm text-destructive/90">
            {COPY.warningBody(warning.ratio, warning.required)}
          </p>
          <div className="mt-3 flex flex-wrap items-center gap-3">
            <button
              type="button"
              data-testid="theme-preview-autofix"
              aria-label={COPY.autoFixCta}
              onClick={handleAutoFix}
              className="inline-flex items-center gap-2 rounded-full bg-destructive px-4 py-2 text-sm font-semibold text-destructive-foreground shadow-soft transition hover:opacity-90"
            >
              {COPY.autoFixCta}
            </button>
            <span className="font-mono text-xs text-destructive/80">
              {warning.fg} / {warning.bg} — {warning.ratio.toFixed(2)}:1
            </span>
          </div>
        </div>
      )}

      {autoFixed && !warning && (
        <p
          data-testid="theme-preview-autofix-applied"
          className="mt-3 text-sm text-emerald-600"
        >
          {COPY.autoFixApplied}
        </p>
      )}
    </div>
  );
}

function SwatchPanel(props: {
  label: string;
  colors: BrandColors;
  dataTestid: string;
  highlight?: boolean;
}): React.JSX.Element {
  const { label, colors, dataTestid, highlight } = props;
  return (
    <div
      data-testid={dataTestid}
      className={`rounded-2xl border bg-white/70 p-4 shadow-soft backdrop-blur dark:bg-slate-800/60 ${
        highlight ? 'ring-2 ring-emerald-500/60' : ''
      }`}
    >
      <p className="mb-3 text-xs font-semibold uppercase tracking-wider opacity-70">
        {label}
      </p>
      <ul className="grid grid-cols-2 gap-2 text-xs">
        <Swatch label={COPY.primaryLabel} hex={colors.primary} />
        <Swatch label={COPY.secondaryLabel} hex={colors.secondary} />
        <Swatch label={COPY.bgLabel} hex={colors.background} />
        <Swatch label={COPY.fgLabel} hex={colors.foreground} />
      </ul>
    </div>
  );
}

function Swatch(props: { label: string; hex: string }): React.JSX.Element {
  return (
    <li className="flex items-center gap-2">
      <span
        aria-hidden="true"
        className="inline-block h-6 w-6 rounded-md border border-current/10"
        style={{ backgroundColor: props.hex }}
      />
      <span className="flex flex-col">
        <span className="font-medium">{props.label}</span>
        <span className="font-mono opacity-70">{props.hex}</span>
      </span>
    </li>
  );
}
