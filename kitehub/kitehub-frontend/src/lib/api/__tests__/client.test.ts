/**
 * Unit tests for API client.
 *
 * @since PR 5.10
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';

// Mock axios before importing apiClient
vi.mock('axios', () => {
  const mockAxios = {
    create: vi.fn(() => mockAxios),
    interceptors: {
      request: {
        use: vi.fn(),
      },
      response: {
        use: vi.fn(),
      },
    },
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  };
  return {
    default: mockAxios,
    ...mockAxios,
  };
});

describe('apiClient', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // GAP-599 Wave 92 Bucket B: clear both storages so legacy + new isolated.
    if (typeof window !== 'undefined') {
      localStorage.clear();
      sessionStorage.clear();
    }
  });

  describe('configuration', () => {
    it('creates axios instance with correct config', async () => {
      // Re-import to test creation
      vi.resetModules();
      await import('../client');

      expect(axios.create).toHaveBeenCalledWith(
        expect.objectContaining({
          baseURL: expect.any(String),
          timeout: 15000,
          headers: {
            'Content-Type': 'application/json',
            'Accept-Language': 'vi',
          },
        })
      );
    });

    it('registers request interceptor', async () => {
      vi.resetModules();
      await import('../client');

      expect(axios.create().interceptors.request.use).toHaveBeenCalled();
    });

    it('registers response interceptor', async () => {
      vi.resetModules();
      await import('../client');

      expect(axios.create().interceptors.response.use).toHaveBeenCalled();
    });
  });

  describe('request interceptor', () => {
    it('adds Authorization header when accessToken exists', async () => {
      // GAP-599 Wave 92 Bucket B: api client reads from sessionStorage now.
      const sessionStorageMock = {
        getItem: vi.fn((key: string) => {
          if (key === 'accessToken') return 'test-token';
          return null;
        }),
        setItem: vi.fn(),
        removeItem: vi.fn(),
        clear: vi.fn(),
      };
      Object.defineProperty(window, 'sessionStorage', { value: sessionStorageMock });

      vi.resetModules();
      await import('../client');

      // Get the request interceptor function
      const mockCalls = (axios.create().interceptors.request.use as ReturnType<typeof vi.fn>).mock.calls;
      const interceptorCall = mockCalls[0];
      if (!interceptorCall) throw new Error('No interceptor registered');
      const requestInterceptor = interceptorCall[0];

      const config = {
        headers: {} as Record<string, string>,
      };

      const result = requestInterceptor(config);
      expect(result.headers.Authorization).toBe('Bearer test-token');
    });

    it('does not add Authorization header when accessToken is missing', async () => {
      // GAP-599 Wave 92 Bucket B: api client reads from sessionStorage.
      const sessionStorageMock = {
        getItem: vi.fn(() => null),
        setItem: vi.fn(),
        removeItem: vi.fn(),
        clear: vi.fn(),
      };
      Object.defineProperty(window, 'sessionStorage', { value: sessionStorageMock });

      vi.resetModules();
      await import('../client');

      const mockCalls = (axios.create().interceptors.request.use as ReturnType<typeof vi.fn>).mock.calls;
      const interceptorCall = mockCalls[0];
      if (!interceptorCall) throw new Error('No interceptor registered');
      const requestInterceptor = interceptorCall[0];

      const config = {
        headers: {},
      };

      const result = requestInterceptor(config);
      expect(result.headers.Authorization).toBeUndefined();
    });
  });

  describe('exports', () => {
    it('exports apiClient as default', async () => {
      vi.resetModules();
      const clientModule = await import('../client');
      expect(clientModule.default).toBeDefined();
    });

    it('exports named apiClient', async () => {
      vi.resetModules();
      const clientModule = await import('../client');
      expect(clientModule.apiClient).toBeDefined();
    });
  });
});
