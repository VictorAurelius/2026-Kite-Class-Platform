import { toast } from 'sonner';

export interface ApiError {
  message: string;
  statusCode?: number;
  errors?: Record<string, string[]>;
}

interface AxiosLikeError {
  response?: {
    status?: number;
    data?: {
      message?: string;
      errors?: Record<string, string[]>;
    };
  };
  message?: string;
}

export function handleApiError(error: AxiosLikeError, fallbackMessage = 'Đã xảy ra lỗi'): string {
  // Axios error response
  if (error.response?.data) {
    const data = error.response.data;

    // Validation errors
    if (data.errors && typeof data.errors === 'object') {
      const firstError = Object.values(data.errors)[0];
      if (Array.isArray(firstError) && firstError.length > 0) {
        return firstError[0] as string;
      }
    }

    // General error message
    if (data.message) {
      return data.message;
    }
  }

  // Network error
  if (error.message === 'Network Error') {
    return 'Không thể kết nối đến server';
  }

  // Generic error
  if (error.message) {
    return error.message;
  }

  return fallbackMessage;
}

export function showErrorToast(error: AxiosLikeError, fallbackMessage = 'Đã xảy ra lỗi') {
  const message = handleApiError(error, fallbackMessage);
  toast.error(message);
}

export function showSuccessToast(message: string) {
  toast.success(message);
}

export function isUnauthorizedError(error: AxiosLikeError): boolean {
  return error.response?.status === 401;
}

export function isForbiddenError(error: AxiosLikeError): boolean {
  return error.response?.status === 403;
}

export function isNotFoundError(error: AxiosLikeError): boolean {
  return error.response?.status === 404;
}

export function isValidationError(error: AxiosLikeError): boolean {
  return error.response?.status === 400 && error.response?.data?.errors !== undefined;
}
