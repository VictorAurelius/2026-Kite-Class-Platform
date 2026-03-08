/**
 * API response types and utilities.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  timestamp: string;
}

export interface ApiError {
  success: false;
  errorCode: string;
  message: string;
  timestamp: string;
  details?: Record<string, unknown>;
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
}

export interface ValidationError {
  field: string;
  message: string;
  rejectedValue?: unknown;
}

// Common error codes
export enum ErrorCode {
  // Authentication
  INVALID_CREDENTIALS = 'INVALID_CREDENTIALS',
  TOKEN_EXPIRED = 'TOKEN_EXPIRED',
  UNAUTHORIZED = 'UNAUTHORIZED',

  // Authorization
  FORBIDDEN = 'FORBIDDEN',
  INSUFFICIENT_PERMISSIONS = 'INSUFFICIENT_PERMISSIONS',

  // Resource
  NOT_FOUND = 'NOT_FOUND',
  ALREADY_EXISTS = 'ALREADY_EXISTS',

  // Validation
  VALIDATION_ERROR = 'VALIDATION_ERROR',
  INVALID_INPUT = 'INVALID_INPUT',

  // Business Logic
  DUPLICATE_EMAIL = 'DUPLICATE_EMAIL',
  STUDENT_NOT_FOUND = 'STUDENT_NOT_FOUND',
  TEACHER_NOT_FOUND = 'TEACHER_NOT_FOUND',
  COURSE_NOT_FOUND = 'COURSE_NOT_FOUND',

  // Multi-tenant
  TENANT_NOT_FOUND = 'TENANT_NOT_FOUND',
  TENANT_SUSPENDED = 'TENANT_SUSPENDED',
  FEATURE_NOT_AVAILABLE = 'FEATURE_NOT_AVAILABLE',
  LIMIT_EXCEEDED = 'LIMIT_EXCEEDED',

  // System
  INTERNAL_SERVER_ERROR = 'INTERNAL_SERVER_ERROR',
  SERVICE_UNAVAILABLE = 'SERVICE_UNAVAILABLE',
}
