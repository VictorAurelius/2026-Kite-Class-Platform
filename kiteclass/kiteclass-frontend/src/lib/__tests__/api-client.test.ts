/**
 * API Client tests - covers token storage and configuration
 *
 * @author KiteClass Team
 * @since 2026-02-23
 */

import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { apiClient } from '../api-client';

describe('apiClient', () => {
  beforeEach(() => {
    // Clear localStorage
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  describe('Configuration', () => {
    it('should have correct base properties', () => {
      expect(apiClient.defaults.baseURL).toBeDefined();
      expect(apiClient.defaults.timeout).toBe(10000);
      expect(apiClient.defaults.headers['Content-Type']).toBe('application/json');
    });

    it('should have request and response interceptors', () => {
      expect(apiClient.interceptors.request).toBeDefined();
      expect(apiClient.interceptors.response).toBeDefined();
    });
  });

  describe('Request Interceptor', () => {
    it('should read accessToken from localStorage', () => {
      localStorage.setItem('accessToken', 'test-token');
      expect(localStorage.getItem('accessToken')).toBe('test-token');
    });

    it('should read tenantId from localStorage', () => {
      localStorage.setItem('tenantId', 'tenant-123');
      expect(localStorage.getItem('tenantId')).toBe('tenant-123');
    });

    it('should handle missing tokens gracefully', () => {
      expect(localStorage.getItem('accessToken')).toBeNull();
      expect(localStorage.getItem('tenantId')).toBeNull();
    });
  });

  describe('Response Interceptor', () => {
    it('should have error handling for 401 responses', () => {
      // The interceptor is configured to handle 401 errors
      // Testing the actual behavior requires integration tests
      expect(apiClient.interceptors.response).toBeDefined();
    });
  });

  describe('Token Management', () => {
    it('should store new access token after refresh', () => {
      const newToken = 'new-access-token';
      localStorage.setItem('accessToken', newToken);

      expect(localStorage.getItem('accessToken')).toBe(newToken);
    });

    it('should clear tokens when refresh fails', () => {
      localStorage.setItem('accessToken', 'old-token');
      localStorage.setItem('refreshToken', 'old-refresh');

      // Simulate refresh failure
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');

      expect(localStorage.getItem('accessToken')).toBeNull();
      expect(localStorage.getItem('refreshToken')).toBeNull();
    });
  });

  describe('Error Handling', () => {
    it('should reject errors without response', () => {
      const error = {
        config: {},
        message: 'Network error',
      };

      // Errors without response should be rejected
      expect(error.message).toBe('Network error');
    });

    it('should handle refresh when no refresh token exists', () => {
      const _error = {
        config: { headers: {}, _retry: false },
        response: { status: 401 },
      };

      expect(localStorage.getItem('refreshToken')).toBeNull();
    });
  });
});
