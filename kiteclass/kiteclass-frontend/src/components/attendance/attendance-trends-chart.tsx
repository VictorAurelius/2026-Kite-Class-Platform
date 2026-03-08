/**
 * Attendance trends chart component.
 * Simple SVG line chart showing attendance rate over time.
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

'use client';

import { useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { AttendanceTrendPoint } from '@/types/attendance';

interface AttendanceTrendsChartProps {
  data: AttendanceTrendPoint[];
  height?: number;
  showGrid?: boolean;
}

export function AttendanceTrendsChart({
  data,
  height = 300,
  showGrid = true,
}: AttendanceTrendsChartProps) {
  const { points, xLabels, yLabels } = useMemo(() => {
    if (!data || data.length === 0) {
      return { points: [], xLabels: [], yLabels: [] };
    }

    const width = 100; // Use percentage for responsive design
    const chartHeight = 80; // Leave space for labels
    const padding = { top: 10, right: 5, bottom: 10, left: 5 };

    // Find min/max values
    const maxRate = Math.max(...data.map((d) => d.attendanceRate));
    const minRate = Math.min(...data.map((d) => d.attendanceRate));
    const rateRange = maxRate - minRate || 1;

    // Generate points
    const points = data.map((point, index) => {
      const x = padding.left + ((width - padding.left - padding.right) / (data.length - 1 || 1)) * index;
      const y =
        padding.top +
        chartHeight -
        (((point.attendanceRate - minRate) / rateRange) * chartHeight);

      return { x, y, data: point };
    });

    // Generate X labels (show max 7 labels)
    const xLabelCount = Math.min(7, data.length);
    const xLabelStep = Math.floor(data.length / xLabelCount) || 1;
    const xLabels = data
      .filter((_, i) => i % xLabelStep === 0)
      .map((point, index) => ({
        x: padding.left + ((width - padding.left - padding.right) / (data.length - 1 || 1)) * (index * xLabelStep),
        label: new Date(point.date).toLocaleDateString('vi-VN', {
          day: '2-digit',
          month: '2-digit',
        }),
      }));

    // Generate Y labels
    const yLabels = [0, 25, 50, 75, 100].map((value) => ({
      y: padding.top + chartHeight - ((value / 100) * chartHeight),
      label: `${value}%`,
    }));

    return { points, xLabels, yLabels };
  }, [data]);

  if (!data || data.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Xu hướng điểm danh</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col items-center justify-center py-12 text-center">
            <div className="mb-4 text-6xl">📈</div>
            <p className="text-sm text-muted-foreground">
              Chưa có đủ dữ liệu để hiển thị xu hướng
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  // Generate SVG path
  const pathD = points
    .map((point, index) => {
      if (index === 0) {
        return `M ${point.x} ${point.y}`;
      }
      return `L ${point.x} ${point.y}`;
    })
    .join(' ');

  // Generate area path (for gradient fill)
  const areaD = `${pathD} L ${points[points.length - 1].x} 90 L ${points[0].x} 90 Z`;

  return (
    <Card>
      <CardHeader>
        <CardTitle>Xu hướng điểm danh</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="relative" style={{ height: `${height}px` }}>
          <svg
            viewBox="0 0 100 100"
            preserveAspectRatio="none"
            className="h-full w-full"
          >
            <defs>
              <linearGradient id="areaGradient" x1="0" x2="0" y1="0" y2="1">
                <stop offset="0%" stopColor="rgb(34, 197, 94)" stopOpacity="0.3" />
                <stop offset="100%" stopColor="rgb(34, 197, 94)" stopOpacity="0" />
              </linearGradient>
            </defs>

            {/* Grid lines */}
            {showGrid &&
              yLabels.map((label, i) => (
                <line
                  key={i}
                  x1="5"
                  y1={label.y}
                  x2="95"
                  y2={label.y}
                  stroke="currentColor"
                  strokeOpacity="0.1"
                  strokeDasharray="2,2"
                />
              ))}

            {/* Area fill */}
            <path d={areaD} fill="url(#areaGradient)" />

            {/* Line */}
            <path
              d={pathD}
              fill="none"
              stroke="rgb(34, 197, 94)"
              strokeWidth="0.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />

            {/* Data points */}
            {points.map((point, index) => (
              <circle
                key={index}
                cx={point.x}
                cy={point.y}
                r="1"
                fill="rgb(34, 197, 94)"
                className="transition-all hover:r-2"
              >
                <title>
                  {new Date(point.data.date).toLocaleDateString('vi-VN')}:{' '}
                  {point.data.attendanceRate.toFixed(1)}%
                </title>
              </circle>
            ))}
          </svg>

          {/* Y-axis labels */}
          <div className="absolute inset-y-0 left-0 flex flex-col justify-between py-2 text-xs text-muted-foreground">
            {[100, 75, 50, 25, 0].map((value) => (
              <div key={value}>{value}%</div>
            ))}
          </div>

          {/* X-axis labels */}
          <div className="absolute bottom-0 left-0 right-0 flex justify-between px-4 text-xs text-muted-foreground">
            {xLabels.map((label, i) => (
              <div key={i}>{label.label}</div>
            ))}
          </div>
        </div>

        {/* Legend */}
        <div className="mt-4 flex items-center justify-center gap-4 text-xs text-muted-foreground">
          <div className="flex items-center gap-2">
            <div className="h-3 w-3 rounded-full bg-green-500" />
            <span>Tỷ lệ điểm danh</span>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
