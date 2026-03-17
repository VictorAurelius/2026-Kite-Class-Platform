/**
 * Unit tests for error-handler utilities.
 *
 * @since PR 5.10
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { toast } from 'sonner';
import {
  handleApiError,
  showErrorToast,
  showSuccessToast,
  isUnauthorizedError,
  isForbiddenError,
  isNotFoundError,
  isValidationError,
} from '../error-handler';

// Mock sonner toast
vi.mock('sonner', () => ({
  toast: {
    error: vi.fn(),
    success: vi.fn(),
  },
}));

describe('error-handler', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('handleApiError', () => {
    it('returns validation error message from first field', () => {
      const error = {
        response: {
          data: {
            errors: {
              email: ['Email is invalid', 'Email is required'],
              name: ['Name is required'],
            },
          },
        },
      };
      expect(handleApiError(error)).toBe('Email is invalid');
    });

    it('returns general error message from response', () => {
      const error = {
        response: {
          data: {
            message: 'Server error occurred',
          },
        },
      };
      expect(handleApiError(error)).toBe('Server error occurred');
    });

    it('returns network error message for Network Error', () => {
      const error = {
        message: 'Network Error',
      };
      expect(handleApiError(error)).toBe('Không thể kết nối đến server');
    });

    it('returns error.message for generic errors', () => {
      const error = {
        message: 'Something went wrong',
      };
      expect(handleApiError(error)).toBe('Something went wrong');
    });

    it('returns fallback message when no error info available', () => {
      const error = {};
      expect(handleApiError(error)).toBe('Đã xảy ra lỗi');
    });

    it('returns custom fallback message', () => {
      const error = {};
      expect(handleApiError(error, 'Custom fallback')).toBe('Custom fallback');
    });

    it('prioritizes validation errors over message', () => {
      const error = {
        response: {
          data: {
            message: 'Validation failed',
            errors: {
              field: ['Field error'],
            },
          },
        },
      };
      expect(handleApiError(error)).toBe('Field error');
    });

    it('handles empty errors object', () => {
      const error = {
        response: {
          data: {
            errors: {},
          },
        },
      };
      expect(handleApiError(error)).toBe('Đã xảy ra lỗi');
    });

    it('handles empty error array', () => {
      const error = {
        response: {
          data: {
            errors: {
              field: [],
            },
          },
        },
      };
      expect(handleApiError(error)).toBe('Đã xảy ra lỗi');
    });
  });

  describe('showErrorToast', () => {
    it('calls toast.error with error message', () => {
      const error = { message: 'Test error' };
      showErrorToast(error);
      expect(toast.error).toHaveBeenCalledWith('Test error');
    });

    it('uses fallback message when error has no info', () => {
      showErrorToast({});
      expect(toast.error).toHaveBeenCalledWith('Đã xảy ra lỗi');
    });

    it('uses custom fallback message', () => {
      showErrorToast({}, 'Custom error');
      expect(toast.error).toHaveBeenCalledWith('Custom error');
    });
  });

  describe('showSuccessToast', () => {
    it('calls toast.success with message', () => {
      showSuccessToast('Operation successful');
      expect(toast.success).toHaveBeenCalledWith('Operation successful');
    });
  });

  describe('isUnauthorizedError', () => {
    it('returns true for 401 status', () => {
      const error = { response: { status: 401 } };
      expect(isUnauthorizedError(error)).toBe(true);
    });

    it('returns false for other status codes', () => {
      expect(isUnauthorizedError({ response: { status: 200 } })).toBe(false);
      expect(isUnauthorizedError({ response: { status: 403 } })).toBe(false);
      expect(isUnauthorizedError({ response: { status: 500 } })).toBe(false);
    });

    it('returns false when response is undefined', () => {
      expect(isUnauthorizedError({})).toBe(false);
      expect(isUnauthorizedError({ message: 'error' })).toBe(false);
    });
  });

  describe('isForbiddenError', () => {
    it('returns true for 403 status', () => {
      const error = { response: { status: 403 } };
      expect(isForbiddenError(error)).toBe(true);
    });

    it('returns false for other status codes', () => {
      expect(isForbiddenError({ response: { status: 200 } })).toBe(false);
      expect(isForbiddenError({ response: { status: 401 } })).toBe(false);
      expect(isForbiddenError({ response: { status: 500 } })).toBe(false);
    });

    it('returns false when response is undefined', () => {
      expect(isForbiddenError({})).toBe(false);
    });
  });

  describe('isNotFoundError', () => {
    it('returns true for 404 status', () => {
      const error = { response: { status: 404 } };
      expect(isNotFoundError(error)).toBe(true);
    });

    it('returns false for other status codes', () => {
      expect(isNotFoundError({ response: { status: 200 } })).toBe(false);
      expect(isNotFoundError({ response: { status: 500 } })).toBe(false);
    });

    it('returns false when response is undefined', () => {
      expect(isNotFoundError({})).toBe(false);
    });
  });

  describe('isValidationError', () => {
    it('returns truthy for 400 status with errors object', () => {
      const error = {
        response: {
          status: 400,
          data: { errors: { field: ['error'] } },
        },
      };
      expect(isValidationError(error)).toBeTruthy();
    });

    it('returns falsy for 400 without errors', () => {
      const error = {
        response: {
          status: 400,
          data: { message: 'Bad request' },
        },
      };
      expect(isValidationError(error)).toBeFalsy();
    });

    it('returns falsy for non-400 status', () => {
      const error = {
        response: {
          status: 500,
          data: { errors: { field: ['error'] } },
        },
      };
      expect(isValidationError(error)).toBeFalsy();
    });

    it('returns falsy when response is undefined', () => {
      expect(isValidationError({})).toBeFalsy();
    });
  });
});
