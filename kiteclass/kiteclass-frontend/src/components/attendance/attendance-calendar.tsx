/**
 * Attendance calendar component - displays attendance in calendar view.
 *
 * @author KiteClass Team
 * @since 2.7.0 (PR 3.8)
 */

'use client';

import { useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { Attendance } from '@/types/attendance';

interface AttendanceCalendarProps {
  attendanceRecords: Attendance[];
  onDateClick?: (date: Date) => void;
}

interface DayData {
  date: Date;
  isCurrentMonth: boolean;
  attendanceCount: number;
  presentCount: number;
  absentCount: number;
}

export function AttendanceCalendar({
  attendanceRecords,
  onDateClick,
}: AttendanceCalendarProps) {
  const [currentDate, setCurrentDate] = useState(new Date());

  // Get calendar data for the current month
  const getCalendarDays = (): DayData[] => {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();

    // First day of the month
    const firstDay = new Date(year, month, 1);
    // Last day of the month
    const lastDay = new Date(year, month + 1, 0);

    // Start from the first Monday before or on the first day
    const startDate = new Date(firstDay);
    const dayOfWeek = firstDay.getDay();
    const diff = dayOfWeek === 0 ? 6 : dayOfWeek - 1; // Monday is 0
    startDate.setDate(firstDay.getDate() - diff);

    // Generate 42 days (6 weeks)
    const days: DayData[] = [];
    const current = new Date(startDate);

    for (let i = 0; i < 42; i++) {
      const dateStr = current.toISOString().split('T')[0];

      // Filter attendance for this date
      const dayAttendance = attendanceRecords.filter((record) => {
        const recordDate = new Date(record.markedDate).toISOString().split('T')[0];
        return recordDate === dateStr;
      });

      days.push({
        date: new Date(current),
        isCurrentMonth: current.getMonth() === month,
        attendanceCount: dayAttendance.length,
        presentCount: dayAttendance.filter((r) => r.status === 'PRESENT').length,
        absentCount: dayAttendance.filter((r) => r.status === 'ABSENT').length,
      });

      current.setDate(current.getDate() + 1);
    }

    return days;
  };

  const days = getCalendarDays();

  const previousMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1));
  };

  const nextMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1));
  };

  const today = () => {
    setCurrentDate(new Date());
  };

  const monthName = currentDate.toLocaleDateString('vi-VN', {
    month: 'long',
    year: 'numeric',
  });

  const isToday = (date: Date) => {
    const now = new Date();
    return (
      date.getDate() === now.getDate() &&
      date.getMonth() === now.getMonth() &&
      date.getFullYear() === now.getFullYear()
    );
  };

  const getAttendanceColor = (day: DayData) => {
    if (day.attendanceCount === 0) return 'bg-background';

    const presentRate = day.presentCount / day.attendanceCount;

    if (presentRate >= 0.9) return 'bg-green-100 dark:bg-green-900';
    if (presentRate >= 0.7) return 'bg-green-50 dark:bg-green-950';
    if (presentRate >= 0.5) return 'bg-yellow-50 dark:bg-yellow-950';
    return 'bg-red-50 dark:bg-red-950';
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle>{monthName}</CardTitle>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={today}>
              Hôm nay
            </Button>
            <Button variant="outline" size="icon" onClick={previousMonth}>
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <Button variant="outline" size="icon" onClick={nextMonth}>
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        {/* Weekday Headers */}
        <div className="mb-2 grid grid-cols-7 gap-1">
          {['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'].map((day) => (
            <div
              key={day}
              className="py-2 text-center text-sm font-medium text-muted-foreground"
            >
              {day}
            </div>
          ))}
        </div>

        {/* Calendar Grid */}
        <div className="grid grid-cols-7 gap-1">
          {days.map((day, idx) => (
            <button
              key={idx}
              onClick={() => onDateClick?.(day.date)}
              disabled={!day.isCurrentMonth || day.attendanceCount === 0}
              className={`
                aspect-square min-h-[60px] rounded-lg border p-1 text-left transition-colors
                ${day.isCurrentMonth ? 'border-border' : 'border-transparent'}
                ${!day.isCurrentMonth && 'text-muted-foreground opacity-40'}
                ${isToday(day.date) && 'ring-2 ring-primary'}
                ${getAttendanceColor(day)}
                ${
                  day.isCurrentMonth && day.attendanceCount > 0
                    ? 'cursor-pointer hover:opacity-80'
                    : 'cursor-default'
                }
              `}
            >
              <div className="flex flex-col h-full">
                <span
                  className={`text-sm ${
                    isToday(day.date) ? 'font-bold text-primary' : ''
                  }`}
                >
                  {day.date.getDate()}
                </span>

                {day.attendanceCount > 0 && (
                  <div className="mt-auto space-y-0.5">
                    <div className="text-[10px] leading-none text-muted-foreground">
                      {day.attendanceCount} lần
                    </div>
                    {day.presentCount > 0 && (
                      <div className="text-[10px] leading-none text-green-600">
                        ✓ {day.presentCount}
                      </div>
                    )}
                    {day.absentCount > 0 && (
                      <div className="text-[10px] leading-none text-red-600">
                        ✗ {day.absentCount}
                      </div>
                    )}
                  </div>
                )}
              </div>
            </button>
          ))}
        </div>

        {/* Legend */}
        <div className="mt-4 flex flex-wrap gap-4 text-xs text-muted-foreground">
          <div className="flex items-center gap-2">
            <div className="h-4 w-4 rounded bg-green-100 dark:bg-green-900" />
            <span>≥90% có mặt</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="h-4 w-4 rounded bg-green-50 dark:bg-green-950" />
            <span>70-89% có mặt</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="h-4 w-4 rounded bg-yellow-50 dark:bg-yellow-950" />
            <span>50-69% có mặt</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="h-4 w-4 rounded bg-red-50 dark:bg-red-950" />
            <span>&lt;50% có mặt</span>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
