/**
 * Attendance statistics cards component - displays attendance summary stats.
 *
 * @author KiteClass Team
 * @since 2.7.0 (PR 3.8)
 */

'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

interface AttendanceStats {
  total: number;
  present: number;
  absent: number;
  late: number;
  excused: number;
  makeup?: number;
}

interface AttendanceStatsCardsProps {
  stats: AttendanceStats;
  showMakeup?: boolean;
}

export function AttendanceStatsCards({
  stats,
  showMakeup = false,
}: AttendanceStatsCardsProps) {
  const cards = [
    {
      title: 'Tổng số',
      value: stats.total,
      color: 'text-foreground',
    },
    {
      title: 'Có mặt',
      value: stats.present,
      color: 'text-green-600',
    },
    {
      title: 'Vắng',
      value: stats.absent,
      color: 'text-red-600',
    },
    {
      title: 'Đi trễ',
      value: stats.late,
      color: 'text-yellow-600',
    },
    {
      title: 'Có phép',
      value: stats.excused,
      color: 'text-blue-600',
    },
  ];

  if (showMakeup && stats.makeup !== undefined) {
    cards.push({
      title: 'Học bù',
      value: stats.makeup,
      color: 'text-purple-600',
    });
  }

  return (
    <div className={`grid gap-4 ${showMakeup ? 'md:grid-cols-6' : 'md:grid-cols-5'}`}>
      {cards.map((card) => (
        <Card key={card.title}>
          <CardHeader className="pb-3">
            <CardTitle className={`text-sm font-medium ${card.color}`}>
              {card.title}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className={`text-2xl font-bold ${card.color}`}>
              {card.value}
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
