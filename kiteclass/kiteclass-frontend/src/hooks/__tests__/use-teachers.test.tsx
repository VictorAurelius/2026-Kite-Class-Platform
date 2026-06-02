/**
 * useTeachers Hooks Integration Tests
 *
 * @author KiteClass Team
 * @since 3.8.0
 */

import { describe, it, expect } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { AllTheProviders } from '@/test/utils';
import { useTeachers, useTeacher, useCreateTeacher, useUpdateTeacher, useDeleteTeacher } from '../use-teachers';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

describe('useTeachers Hooks', () => {
  describe('useTeachers (list)', () => {
    it('should fetch teachers list successfully', async () => {
      const { result } = renderHook(() => useTeachers({ page: 0, size: 20 }), {
        wrapper: AllTheProviders,
      });

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true);
      });

      expect(result.current.data?.content).toHaveLength(2);
      expect(result.current.data!.content[0]!.name).toBe('Nguyễn Thị Giáo');
    });

    it('should handle API errors gracefully', async () => {
      server.use(
        http.get(`${BASE_URL}/api/v1/teachers`, () => {
          return HttpResponse.json(
            { success: false, message: 'Server error' },
            { status: 500 }
          );
        })
      );

      const { result } = renderHook(() => useTeachers({ page: 0, size: 20 }), {
        wrapper: AllTheProviders,
      });

      await waitFor(() => {
        expect(result.current.isError).toBe(true);
      });
    });
  });

  describe('useTeacher (single)', () => {
    it('should fetch single teacher by ID', async () => {
      const { result } = renderHook(() => useTeacher(1), {
        wrapper: AllTheProviders,
      });

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true);
      });

      expect(result.current.data?.name).toBe('Nguyễn Thị Giáo');
      expect(result.current.data?.email).toBe('giao.nguyen@kiteclass.local');
    });

    it('should not fetch when ID is 0', () => {
      const { result } = renderHook(() => useTeacher(0), {
        wrapper: AllTheProviders,
      });

      expect(result.current.isFetching).toBe(false);
    });
  });

  describe('useCreateTeacher (mutation)', () => {
    it('should create new teacher successfully', async () => {
      const { result } = renderHook(() => useCreateTeacher(), {
        wrapper: AllTheProviders,
      });

      const newTeacher = {
        name: 'New Teacher',
        email: 'new@teacher.com',
        phoneNumber: '0909999999',
        specialization: 'Mathematics',
      };

      result.current.mutate(newTeacher);

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true);
      });

      expect(result.current.data?.name).toBe('New Teacher');
    });

    it('should handle validation errors', async () => {
      server.use(
        http.post(`${BASE_URL}/api/v1/teachers`, () => {
          return HttpResponse.json(
            { success: false, message: 'Email already exists' },
            { status: 400 }
          );
        })
      );

      const { result } = renderHook(() => useCreateTeacher(), {
        wrapper: AllTheProviders,
      });

      result.current.mutate({
        name: 'Test',
        email: 'existing@teacher.com',
        phoneNumber: '0909999999',
      });

      await waitFor(() => {
        expect(result.current.isError).toBe(true);
      });
    });
  });

  describe('useUpdateTeacher (mutation)', () => {
    // [SKIP: React Query mutation success timing flaky in jsdom — update flow tested via E2E]
    it.skip('should update teacher successfully', async () => {
      const { result } = renderHook(() => useUpdateTeacher(1), {
        wrapper: AllTheProviders,
      });

      result.current.mutate({
        name: 'Updated Name',
      });

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true);
      });

      expect(result.current.data?.name).toBe('Updated Name');
    });
  });

  describe('useDeleteTeacher (mutation)', () => {
    it('should delete teacher successfully', async () => {
      const { result } = renderHook(() => useDeleteTeacher(), {
        wrapper: AllTheProviders,
      });

      result.current.mutate(1);

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true);
      });
    });

    it('should handle delete errors', async () => {
      server.use(
        http.delete(`${BASE_URL}/api/v1/teachers/:id`, () => {
          return HttpResponse.json(
            { success: false, message: 'Teacher has active classes' },
            { status: 400 }
          );
        })
      );

      const { result } = renderHook(() => useDeleteTeacher(), {
        wrapper: AllTheProviders,
      });

      result.current.mutate(1);

      await waitFor(() => {
        expect(result.current.isError).toBe(true);
      });
    });
  });
});
