/**
 * Dashboard Foundation — barrel export.
 *
 * Stable surface for Bucket B/C/D consumers. Importers should pull from
 * `@/_shared/dashboard-foundation` rather than reaching into individual files.
 */

export type {
  ThemeMode,
  DashboardCommand,
  KPIData,
  ActivityItem,
} from './types';

export { Sparkline } from './Sparkline';
export type { SparklineProps } from './Sparkline';

export { KPICard } from './KPICard';
export type { KPICardProps } from './KPICard';

export {
  useDashboardTheme,
  DashboardThemeBoundary,
  DASHBOARD_THEME_STORAGE_KEY,
} from './ThemeProvider';

export { useCommandPalette } from './useCommandPalette';
export type { UseCommandPaletteResult } from './useCommandPalette';

export { CommandPalette } from './CommandPalette';
export type { CommandPaletteProps } from './CommandPalette';

export { SuccessConfetti, fireConfetti } from './SuccessConfetti';
export type { SuccessConfettiProps } from './SuccessConfetti';
