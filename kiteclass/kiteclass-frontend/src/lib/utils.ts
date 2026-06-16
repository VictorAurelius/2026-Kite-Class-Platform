import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * Format ISO date string to Vietnamese locale
 * Uses UTC timezone to match ISO timestamps from backend
 */
export function formatDate(dateString: string): string {
  // BUG-KC3-2: null/unparseable date hiển thị "Invalid Date" cho user (dashboard
  // "Học viên mới nhất" + 6 call site khác). Trả em-dash placeholder thay vì literal lỗi.
  if (!dateString) return '—';
  const date = new Date(dateString);
  if (isNaN(date.getTime())) {
    return '—';
  }
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    timeZone: 'UTC',
  }).format(date);
}

/**
 * Format ISO datetime string to Vietnamese locale
 * Uses UTC timezone to match ISO timestamps from backend
 */
export function formatDateTime(dateTimeString: string): string {
  // BUG-KC3-2 sister: same "Invalid Date" leak cho datetime call sites.
  if (!dateTimeString) return '—';
  const date = new Date(dateTimeString);
  if (isNaN(date.getTime())) {
    return '—';
  }
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'UTC',
  }).format(date);
}

/**
 * Format number to Vietnamese currency (VND)
 */
export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(amount);
}
