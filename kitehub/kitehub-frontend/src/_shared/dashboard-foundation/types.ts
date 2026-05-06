/**
 * Dashboard Foundation Types — kitehub-pro v2 production port (Wave 31 Bucket A).
 *
 * Mirror of `kiteclass-frontend/src/_shared/dashboard-foundation/types.ts`
 * (Wave 30 Bucket A) with KH-specific framing in comments. Decision B
 * (duplicate) — primitives copied verbatim because the only KC coupling in
 * the source was the localStorage key namespace, which we re-namespace below
 * in `ThemeProvider.tsx`.
 *
 * Future work: factor sang `packages/shared-ui-app/dashboard-foundation/` if
 * KC + KH foundations stay in lock-step (Wave 32+ candidate).
 *
 * Source spec:
 *   documents/02-architecture/design-system/ui_kits/kitehub-pro-v2/screens/*.html
 *   GAP-270 (Track 2 KH production port)
 */

import type { ComponentType, ReactNode } from 'react';

/**
 * Theme mode — typed alternative to the loose `string` exposed by next-themes.
 * Foundation wraps next-themes to enforce this domain type at consumer sites.
 */
export type ThemeMode = 'light' | 'dark';

/**
 * A single command surfaced in the ⌘K palette.
 * Section groups commands in the palette UI (e.g. "Tổng quan", "Tài chính").
 */
export interface DashboardCommand {
  /** Stable id used as React key + analytics event name. */
  id: string;
  /** Localized display label (Vietnamese primary). */
  label: string;
  /** Optional short hint shown right-aligned (e.g. "G then D"). */
  hint?: string;
  /** Group bucket — palette renders headers per section. */
  section: string;
  /** Route href to push when command activates. Mutually exclusive with onSelect. */
  href?: string;
  /** Imperative handler — used when no href applies (e.g. open dialog). */
  onSelect?: () => void;
  /** Optional lucide-react icon component for visual cue. */
  icon?: ComponentType<{ className?: string }>;
  /**
   * Optional keywords to extend fuzzy match beyond the label
   * (e.g. ['tổng quan','overview','home'] for a "Dashboard" command).
   */
  keywords?: string[];
}

/**
 * KPI tile data — consumed by KPICard.
 * `delta` is the percent change vs the prior period (positive = up).
 * `sparkline` is an array of recent values, oldest → newest.
 */
export interface KPIData {
  /** Localized label, shown above the value. */
  label: string;
  /** Current value (formatted by caller — string allows "428" or "+₫42M"). */
  value: string;
  /** Optional percent delta vs prior period (e.g. 8.2 means +8.2%). */
  delta?: number;
  /** Optional series for sparkline (oldest first). Empty array → render skeleton. */
  sparkline?: number[];
  /** Optional tone — drives accent color. Defaults to 'neutral'. */
  tone?: 'positive' | 'negative' | 'neutral' | 'warning';
  /** Optional icon for the tile header. */
  icon?: ReactNode;
}

/**
 * Activity feed item — consumed by the home dashboard recent-activity widget.
 */
export interface ActivityItem {
  id: string;
  /** Localized title line (e.g. "Trung tâm Toán Master đã thanh toán"). */
  title: string;
  /** Optional supporting detail (instance/tier/etc). */
  detail?: string;
  /** ISO timestamp — caller decides format; consumers may use date-fns. */
  timestamp: string;
  /** Lucide-react icon component (e.g. CreditCard, Sparkles). */
  icon?: ComponentType<{ className?: string }>;
  /** Tone tag for the icon background. Defaults to 'neutral'. */
  tone?: 'positive' | 'negative' | 'neutral' | 'warning';
}
