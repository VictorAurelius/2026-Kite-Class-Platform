'use client';

import { cn } from '@/lib/utils';

const statusStyles: Record<string, string> = {
  TRIAL: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200',
  ACTIVE: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
  SUSPENDED: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200',
  EXPIRED: 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-200',
  PENDING: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200',
  COMPLETED: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
  FAILED: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200',
  CANCELLED: 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-200',
  PROCESSING: 'bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200',
};

interface StatusBadgeProps {
  status: string;
  className?: string;
}

export function StatusBadge({ status, className }: StatusBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
        statusStyles[status] ?? 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-200',
        className
      )}
    >
      {status}
    </span>
  );
}
