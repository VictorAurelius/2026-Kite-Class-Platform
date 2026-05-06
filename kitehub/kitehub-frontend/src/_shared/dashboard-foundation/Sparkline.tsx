/**
 * Sparkline — pure SVG sparkline rendering.
 *
 * Lightweight (no charting lib). Caller passes oldest-first numeric array.
 * Empty array → renders inline skeleton (dashed baseline).
 *
 * Source spec: kitehub-pro-v2/screens/dashboard-default.html (KPI cards row).
 * Mirror of Wave 30 KC primitive (verbatim — pure SVG, no app coupling).
 */

'use client';

import { useId } from 'react';
import type { KPIData } from './types';

export interface SparklineProps {
  /** Oldest-first numeric series. Length 0 renders skeleton. */
  data: NonNullable<KPIData['sparkline']>;
  /** Width in px. Defaults to 96. */
  width?: number;
  /** Height in px. Defaults to 28. */
  height?: number;
  /** Stroke color (hex/rgb/css var). Defaults to currentColor. */
  stroke?: string;
  /** Optional area fill below the line. */
  fill?: string;
  /** Stroke width in px. Defaults to 1.5. */
  strokeWidth?: number;
  /** Optional aria-label override. */
  ariaLabel?: string;
}

export function Sparkline({
  data,
  width = 96,
  height = 28,
  stroke = 'currentColor',
  fill,
  strokeWidth = 1.5,
  ariaLabel,
}: SparklineProps) {
  const gradientId = useId();

  if (data.length === 0) {
    return (
      <svg
        width={width}
        height={height}
        viewBox={`0 0 ${width} ${height}`}
        aria-label={ariaLabel ?? 'No sparkline data available'}
        role="img"
        data-testid="sparkline-empty"
      >
        <line
          x1={0}
          y1={height / 2}
          x2={width}
          y2={height / 2}
          stroke={stroke}
          strokeOpacity={0.2}
          strokeDasharray="3 3"
          strokeWidth={1}
        />
      </svg>
    );
  }

  const min = Math.min(...data);
  const max = Math.max(...data);
  const range = max - min || 1;
  const stepX = data.length > 1 ? width / (data.length - 1) : 0;

  // Map each point into SVG space — y inverted (top is 0).
  const points = data.map((v, i) => {
    const x = i * stepX;
    const y = height - ((v - min) / range) * height;
    return [x, y] as const;
  });

  const linePath = points
    .map(([x, y], i) => `${i === 0 ? 'M' : 'L'} ${x.toFixed(2)} ${y.toFixed(2)}`)
    .join(' ');

  const areaPath = fill
    ? `${linePath} L ${(width).toFixed(2)} ${height} L 0 ${height} Z`
    : null;

  return (
    <svg
      width={width}
      height={height}
      viewBox={`0 0 ${width} ${height}`}
      aria-label={ariaLabel ?? `Sparkline trend, ${data.length} data points`}
      role="img"
      data-testid="sparkline"
    >
      {areaPath && fill && (
        <>
          <defs>
            <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor={fill} stopOpacity={0.4} />
              <stop offset="100%" stopColor={fill} stopOpacity={0} />
            </linearGradient>
          </defs>
          <path d={areaPath} fill={`url(#${gradientId})`} stroke="none" />
        </>
      )}
      <path
        d={linePath}
        stroke={stroke}
        strokeWidth={strokeWidth}
        strokeLinecap="round"
        strokeLinejoin="round"
        fill="none"
      />
    </svg>
  );
}
