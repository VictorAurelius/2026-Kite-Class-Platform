/**
 * Today's classes widget for teacher dashboard.
 * Shows today's classes with attendance status and quick actions.
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

'use client';

import Link from 'next/link';
import { Clock, Users, CheckCircle, AlertCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import type { TodayClassSession } from '@/types/attendance';

interface TodayClassesWidgetProps {
  sessions: TodayClassSession[];
  isLoading?: boolean;
}

export function TodayClassesWidget({
  sessions,
  isLoading = false,
}: TodayClassesWidgetProps) {
  const pendingCount = sessions.filter((s) => !s.attendanceMarked).length;

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center justify-between">
            <span>Lớp học hôm nay</span>
            <Badge variant="secondary">Đang tải...</Badge>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-24 animate-pulse rounded-lg bg-muted" />
            ))}
          </div>
        </CardContent>
      </Card>
    );
  }

  if (sessions.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Lớp học hôm nay</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col items-center justify-center py-8 text-center">
            <div className="mb-2 text-4xl">📅</div>
            <p className="text-sm text-muted-foreground">
              Không có lớp học nào hôm nay
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  const formatTime = (datetime: string) => {
    return new Date(datetime).toLocaleTimeString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center justify-between">
          <span>Lớp học hôm nay ({sessions.length})</span>
          {pendingCount > 0 && (
            <Badge variant="destructive" className="gap-1">
              <AlertCircle className="h-3 w-3" />
              Chưa điểm danh: {pendingCount}
            </Badge>
          )}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          {sessions.map((session) => (
            <Card key={session.sessionId} className="border">
              <CardContent className="p-4">
                <div className="flex items-start justify-between gap-4">
                  {/* Class Info */}
                  <div className="flex-1 space-y-2">
                    <div>
                      <h4 className="font-semibold">{session.className}</h4>
                      <p className="text-sm text-muted-foreground">
                        Buổi {session.sessionNumber}
                      </p>
                    </div>

                    <div className="flex flex-wrap gap-3 text-sm text-muted-foreground">
                      <div className="flex items-center gap-1">
                        <Clock className="h-4 w-4" />
                        <span>
                          {formatTime(session.startTime)} -{' '}
                          {formatTime(session.endTime)}
                        </span>
                      </div>
                      <div className="flex items-center gap-1">
                        <Users className="h-4 w-4" />
                        <span>{session.totalStudents} học viên</span>
                      </div>
                    </div>

                    {/* Attendance Status */}
                    {session.attendanceMarked ? (
                      <div className="flex items-center gap-2 text-sm text-green-600">
                        <CheckCircle className="h-4 w-4" />
                        <span>
                          Đã điểm danh ({session.presentCount}/{session.totalStudents})
                        </span>
                      </div>
                    ) : (
                      <div className="flex items-center gap-2 text-sm text-orange-600">
                        <AlertCircle className="h-4 w-4" />
                        <span>Chưa điểm danh</span>
                      </div>
                    )}
                  </div>

                  {/* Action Button */}
                  <div className="flex-shrink-0">
                    <Link
                      href={`/classes/${session.classId}/attendance?session=${session.sessionId}`}
                    >
                      <Button
                        size="sm"
                        variant={session.attendanceMarked ? 'outline' : 'default'}
                      >
                        {session.attendanceMarked ? 'Xem' : 'Điểm danh'}
                      </Button>
                    </Link>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
