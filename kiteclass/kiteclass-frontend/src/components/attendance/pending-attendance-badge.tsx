/**
 * Pending attendance badge component.
 * Shows pending attendance count with red indicator.
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

'use client';

import { Badge } from '@/components/ui/badge';
import { AlertCircle } from 'lucide-react';

interface PendingAttendanceBadgeProps {
  count: number;
  variant?: 'default' | 'compact';
  showIcon?: boolean;
}

export function PendingAttendanceBadge({
  count,
  variant = 'default',
  showIcon = true,
}: PendingAttendanceBadgeProps) {
  if (count === 0) {
    return null;
  }

  if (variant === 'compact') {
    return (
      <Badge
        variant="destructive"
        className="ml-2 h-5 min-w-5 rounded-full px-1.5 text-xs"
      >
        {count}
      </Badge>
    );
  }

  return (
    <Badge variant="destructive" className="gap-1">
      {showIcon && <AlertCircle className="h-3 w-3" />}
      <span>Chưa điểm danh: {count}</span>
    </Badge>
  );
}
