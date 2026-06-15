/**
 * Monthly bar chart — responsive inline SVG (no chart-lib dependency).
 *
 * Mirrors the inline-SVG chart convention used by
 * `attendance/attendance-trends-chart.tsx`. Renders a 12-month series of
 * bars with VN month labels (`T1`, `T2`, ...) and a graceful empty state
 * when every value is 0.
 *
 * @since GAP-865 (KC reports FE page)
 */

'use client';

import { useMemo } from 'react';

export interface MonthlyBarPoint {
  /** ISO `YYYY-MM`. */
  month: string;
  /** Bar value (revenue VND or present-rate %). */
  value: number;
}

interface MonthlyBarChartProps {
  data: MonthlyBarPoint[];
  /** Format a value for the tooltip/label (e.g. VND or percent). */
  formatValue: (v: number) => string;
  /** Bar + accent color (Tailwind rgb). */
  color?: string;
  height?: number;
  /** Shown when all values are 0 (no real data yet). */
  emptyHint?: string;
  /**
   * Accessible label describing what the chart measures (e.g. "Doanh thu theo
   * tháng"). Used as the sr-only data-table caption + value column header so a
   * screen-reader user reads the real per-month values (WCAG 1.1.1). Falls back
   * to a generic label when not provided.
   */
  label?: string;
}

/** `2026-06` → `T6` (VN convention: tháng = T). */
function monthLabel(iso: string): string {
  const parts = iso.split('-');
  const m = parts[1];
  return m ? `T${Number(m)}` : iso;
}

export function MonthlyBarChart({
  data,
  formatValue,
  color = 'rgb(37, 99, 235)',
  height = 240,
  emptyHint = 'Chưa có dữ liệu',
  label = 'Biểu đồ cột theo tháng',
}: MonthlyBarChartProps) {
  const allZero = useMemo(
    () => data.length === 0 || data.every((d) => d.value === 0),
    [data]
  );

  const bars = useMemo(() => {
    if (data.length === 0) return [];
    const maxValue = Math.max(...data.map((d) => d.value), 0);
    const range = maxValue || 1; // avoid /0 when all zero
    const n = data.length;
    const slot = 100 / n;
    const barWidth = slot * 0.6;
    const gap = slot * 0.2;

    return data.map((point, i) => {
      const barHeight = (point.value / range) * 90; // leave 10% headroom
      return {
        x: i * slot + gap,
        y: 100 - barHeight,
        width: barWidth,
        barHeight,
        point,
      };
    });
  }, [data]);

  if (allZero) {
    return (
      <div
        className="flex flex-col items-center justify-center text-center"
        style={{ height: `${height}px` }}
      >
        <div className="mb-3 text-5xl">📊</div>
        <p className="text-sm text-muted-foreground">{emptyHint}</p>
      </div>
    );
  }

  return (
    <div>
      {/* GAP-1378: sr-only data table conveys the real per-month values to
          screen readers (WCAG 1.1.1). The SVG below is marked aria-hidden so
          it's treated as decorative — its child <title> tooltips were masked
          once the parent had role="img" + a generic aria-label. */}
      <table className="sr-only">
        <caption>{label}</caption>
        <thead>
          <tr>
            <th scope="col">Tháng</th>
            <th scope="col">Giá trị</th>
          </tr>
        </thead>
        <tbody>
          {data.map((point) => (
            <tr key={point.month}>
              <th scope="row">{monthLabel(point.month)}</th>
              <td>{formatValue(point.value)}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="relative" style={{ height: `${height}px` }} aria-hidden="true">
        <svg
          viewBox="0 0 100 100"
          preserveAspectRatio="none"
          className="h-full w-full"
        >
          {/* Grid lines */}
          {[0, 25, 50, 75, 100].map((pct) => (
            <line
              key={pct}
              x1="0"
              y1={pct}
              x2="100"
              y2={pct}
              stroke="currentColor"
              strokeOpacity="0.08"
              strokeWidth="0.3"
            />
          ))}

          {/* Bars */}
          {bars.map((bar, i) => (
            <rect
              key={i}
              x={bar.x}
              y={bar.y}
              width={bar.width}
              height={bar.barHeight}
              rx="0.5"
              fill={color}
              fillOpacity="0.85"
            >
              <title>
                {monthLabel(bar.point.month)}: {formatValue(bar.point.value)}
              </title>
            </rect>
          ))}
        </svg>
      </div>

      {/* X-axis month labels — decorative (values already in the sr-only table) */}
      <div className="mt-2 flex justify-between text-[10px] text-muted-foreground" aria-hidden="true">
        {data.map((point) => (
          <span key={point.month} className="flex-1 text-center">
            {monthLabel(point.month)}
          </span>
        ))}
      </div>
    </div>
  );
}
