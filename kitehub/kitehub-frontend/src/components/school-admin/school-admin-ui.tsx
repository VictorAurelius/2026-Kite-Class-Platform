/**
 * Shared UI primitives for school-admin route group.
 *
 * Wave 50 Bucket A (GAP-271).
 */

import { type ReactNode } from 'react';
import Link from 'next/link';
import { ArrowUp, ArrowDown, Minus, ShieldCheck, ChevronRight, Home } from 'lucide-react';

export function PageHeader({
  title,
  subtitle,
  actions,
  breadcrumbs,
}: {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
  breadcrumbs?: { label: string; href?: string }[];
}) {
  return (
    <div className="mb-6">
      {breadcrumbs && breadcrumbs.length > 0 && (
        <nav aria-label="Đường dẫn" className="mb-2 flex items-center gap-1 text-xs text-muted-foreground">
          <Link href="/school-admin/dashboard" className="hover:text-foreground" aria-label="Trang chủ">
            <Home className="h-3 w-3" aria-hidden="true" />
          </Link>
          {breadcrumbs.map((c, i) => (
            <span key={`${c.label}-${i}`} className="flex items-center gap-1">
              <ChevronRight className="h-3 w-3" aria-hidden="true" />
              {c.href ? (
                <Link href={c.href} className="hover:text-foreground">
                  {c.label}
                </Link>
              ) : (
                <span className="font-medium text-foreground">{c.label}</span>
              )}
            </span>
          ))}
        </nav>
      )}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
          {subtitle && <p className="mt-1 text-sm text-muted-foreground">{subtitle}</p>}
        </div>
        {actions && <div className="flex items-center gap-2">{actions}</div>}
      </div>
    </div>
  );
}

export function KpiCard({
  label,
  value,
  delta,
  deltaDirection,
  sub,
}: {
  label: string;
  value: string;
  delta: string;
  deltaDirection: 'up' | 'down' | 'flat';
  sub: string;
}) {
  const Icon = deltaDirection === 'up' ? ArrowUp : deltaDirection === 'down' ? ArrowDown : Minus;
  const color =
    deltaDirection === 'up'
      ? 'text-emerald-600 dark:text-emerald-400'
      : deltaDirection === 'down'
        ? 'text-amber-600 dark:text-amber-400'
        : 'text-muted-foreground';
  return (
    <div className="rounded-lg border bg-background p-3">
      <div className="text-[11px] font-medium uppercase tracking-wide text-muted-foreground">{label}</div>
      <div className="mt-1 text-2xl font-bold tabular-nums">{value}</div>
      <div className={`mt-1 flex items-center gap-1 text-xs ${color}`}>
        <Icon className="h-3 w-3" aria-hidden="true" />
        <span>{delta}</span>
      </div>
      <div className="mt-0.5 text-[11px] text-muted-foreground">{sub}</div>
    </div>
  );
}

export function MoetStamp({ size = 'sm' }: { size?: 'sm' | 'md' }) {
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-md border-2 border-amber-600/30 bg-amber-50 px-2 py-1 font-semibold text-amber-900 dark:border-amber-400/30 dark:bg-amber-950/40 dark:text-amber-200 ${
        size === 'sm' ? 'text-[10px]' : 'text-xs'
      }`}
      aria-label="Tuân thủ MoET"
    >
      <ShieldCheck className={size === 'sm' ? 'h-3 w-3' : 'h-3.5 w-3.5'} aria-hidden="true" />
      {size === 'sm' ? 'MoET' : 'Bắt buộc MoET'}
    </span>
  );
}

const PILL_STYLES: Record<string, string> = {
  success: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20 dark:bg-emerald-950/40 dark:text-emerald-300',
  warning: 'bg-amber-50 text-amber-800 ring-amber-600/20 dark:bg-amber-950/40 dark:text-amber-300',
  danger: 'bg-red-50 text-red-700 ring-red-600/20 dark:bg-red-950/40 dark:text-red-300',
  info: 'bg-blue-50 text-blue-700 ring-blue-600/20 dark:bg-blue-950/40 dark:text-blue-300',
  neutral: 'bg-gray-50 text-gray-700 ring-gray-600/20 dark:bg-gray-950/40 dark:text-gray-300',
};

export function Pill({
  children,
  kind = 'neutral',
}: {
  children: ReactNode;
  kind?: 'success' | 'warning' | 'danger' | 'info' | 'neutral';
}) {
  return (
    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-[11px] font-medium ring-1 ring-inset ${PILL_STYLES[kind]}`}>
      {children}
    </span>
  );
}

export function SlaIndicator({ hoursLeft }: { hoursLeft: number }) {
  let kind: 'success' | 'warning' | 'danger' = 'success';
  let label = '';
  if (hoursLeft < 0) {
    kind = 'danger';
    label = `SLA: trễ ${Math.abs(hoursLeft).toFixed(1)}h`;
  } else if (hoursLeft < 2) {
    kind = 'warning';
    label = `Còn ${hoursLeft.toFixed(1)}h`;
  } else {
    kind = 'success';
    label = `Còn ${hoursLeft.toFixed(1)}h`;
  }
  return <Pill kind={kind}>{label}</Pill>;
}

export function EmptyState({
  icon,
  title,
  description,
  action,
}: {
  icon?: ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center rounded-lg border border-dashed bg-muted/20 px-6 py-12 text-center">
      {icon && <div className="mb-3 text-muted-foreground">{icon}</div>}
      <h3 className="text-base font-semibold">{title}</h3>
      {description && <p className="mt-1 max-w-md text-sm text-muted-foreground">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}
