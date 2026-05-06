/**
 * Type definitions for G11 Theme Customization Live Preview.
 *
 * Mirrors `ui_kits/components/G11-theme-preview/README.md` + 5 state HTML files
 * (`states/{default,brand-applied,dark-morph,mobile-preview,wcag-warning}.html`)
 * and `dossier/04-component-gaps.md` §G11.
 *
 * Production binding: KH `/branding/wizard` step 5 (preview-approve before
 * deploy). Per `.claude/rules/ai-branding-guidelines.md` §4.2 the live preview
 * is MANDATORY before deploy; per §4.3 + §5 the WCAG fail demonstration is
 * mandatory + blocks deploy until ratio ≥ 4.5:1.
 */

/**
 * Light vs dark visualization mode. The component renders a light/dark
 * side-by-side pair on the brand-applied + dark-morph states, but the active
 * preview surface (the demonstration sandbox) only renders one mode at a time.
 */
export type ThemeMode = 'light' | 'dark';

/**
 * Brand colour palette captured by the wizard's color picker. All 4 are
 * required so the component can render a meaningful preview without falling
 * back to defaults silently.
 */
export type BrandColors = {
  /** Primary brand colour (e.g. CTA button background). */
  primary: string;
  /** Secondary brand colour (e.g. card accent / icon). */
  secondary: string;
  /** Surface colour for cards / sandbox preview. */
  background: string;
  /** Foreground colour rendered ON `background` — the AA-contrast subject. */
  foreground: string;
};

/**
 * Single contrast warning emitted when a fg-on-bg pair fails WCAG AA (≥4.5:1
 * for normal text). The component shows one of these for each failing pair
 * the picker yields, plus the auto-fix CTA.
 */
export type ContrastWarning = {
  /** Foreground hex (the failing colour). */
  fg: string;
  /** Background hex (the failing colour). */
  bg: string;
  /** Measured contrast ratio (1-21). */
  ratio: number;
  /** Required minimum ratio per WCAG AA — 4.5 for normal text. */
  required: number;
};

export type ThemePreviewProps = {
  /** Brand colours from the wizard's picker. */
  brandColors: BrandColors;
  /**
   * Initial mode. Defaults to `'light'`. The component owns toggle state
   * after mount so the demo is self-contained.
   */
  initialMode?: ThemeMode;
  /**
   * Optional explicit failing pair for tests / Storybook. When omitted,
   * the component computes contrast from `brandColors.foreground` on
   * `brandColors.background` and emits a warning if it fails.
   */
  initialWarning?: ContrastWarning;
  /**
   * Override the wrapper `lang` attribute. Defaults to `'vi'`.
   * `'en'` falls back to `'vi'` for v1 (Vietnamese-first per CLAUDE.md).
   */
  lang?: 'vi' | 'en';
};
