/**
 * Dashboard Foundation Types — kiteclass-pro v2 production port (Wave 30 Bucket A).
 *
 * These types are the contract between Bucket A (foundation) and Buckets B/C/D
 * (consumers). Ship in Commit 1 alone so consumer agents type-check independently.
 *
 * Source spec:
 *   documents/02-architecture/design-system/ui_kits/kiteclass-pro-v2/screens/*.html
 *   GAP-266 (Track 2 production port)
 */

import type { ComponentType, ReactNode } from 'react';

/**
 * Theme mode — typed alternative to the loose `string` exposed by next-themes.
 * Foundation wraps next-themes to enforce this domain type at consumer sites.
 */
export type ThemeMode = 'light' | 'dark';

/**
 * A single command surfaced in the ⌘K palette.
 * Section groups commands in the palette UI (e.g. "Navigation", "Actions").
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
  /** Localized title line (e.g. "Bạn đã tạo lớp IELTS 7.5"). */
  title: string;
  /** Optional supporting detail (subject/teacher/etc). */
  detail?: string;
  /** ISO timestamp — caller decides format; consumers may use date-fns. */
  timestamp: string;
  /** Lucide-react icon component (e.g. UserPlus, BookOpen). */
  icon?: ComponentType<{ className?: string }>;
  /** Tone tag for the icon background. Defaults to 'neutral'. */
  tone?: 'positive' | 'negative' | 'neutral' | 'warning';
}
