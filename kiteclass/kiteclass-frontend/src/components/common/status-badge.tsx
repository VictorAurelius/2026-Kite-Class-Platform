/**
 * Status badge component for displaying status indicators.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';

type StatusVariant = 'default' | 'success' | 'warning' | 'error' | 'info';

interface StatusBadgeProps {
  status: string;
  variant?: StatusVariant;
  className?: string;
}

const variantStyles: Record<StatusVariant, string> = {
  default: 'bg-secondary text-secondary-foreground',
  success: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-100',
  warning: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-100',
  error: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-100',
  info: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-100',
};

// Auto-detect variant based on status value
const getVariantFromStatus = (status: string): StatusVariant => {
  const normalizedStatus = status.toLowerCase();

  if (
    normalizedStatus.includes('active') ||
    normalizedStatus.includes('published') ||
    normalizedStatus.includes('completed') ||
    normalizedStatus.includes('success') ||
    normalizedStatus.includes('paid')
  ) {
    return 'success';
  }

  if (
    normalizedStatus.includes('pending') ||
    normalizedStatus.includes('draft') ||
    normalizedStatus.includes('warning')
  ) {
    return 'warning';
  }

  if (
    normalizedStatus.includes('inactive') ||
    normalizedStatus.includes('cancelled') ||
    normalizedStatus.includes('failed') ||
    normalizedStatus.includes('error') ||
    normalizedStatus.includes('terminated')
  ) {
    return 'error';
  }

  if (normalizedStatus.includes('archived') || normalizedStatus.includes('on_leave')) {
    return 'info';
  }

  return 'default';
};

export function StatusBadge({ status, variant, className }: StatusBadgeProps) {
  const badgeVariant = variant || getVariantFromStatus(status);

  return (
    <Badge className={cn(variantStyles[badgeVariant], className)}>
      {status.replace(/_/g, ' ')}
    </Badge>
  );
}
