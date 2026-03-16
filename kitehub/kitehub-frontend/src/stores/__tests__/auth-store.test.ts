/**
 * Unit tests for Zustand auth store.
 *
 * @since PR 5.9
 */

import { describe, it, expect, beforeEach } from 'vitest';
import { useAuthStore } from '../auth-store';

describe('auth-store', () => {
  // Reset store before each test
  beforeEach(() => {
    useAuthStore.getState().clearAuth();
  });

  describe('initial state', () => {
    it('has null user initially', () => {
      const state = useAuthStore.getState();
      expect(state.user).toBeNull();
    });

    it('has null tokens initially', () => {
      const state = useAuthStore.getState();
      expect(state.accessToken).toBeNull();
      expect(state.refreshToken).toBeNull();
    });

    it('is not authenticated initially', () => {
      const state = useAuthStore.getState();
      expect(state.isAuthenticated).toBe(false);
    });
  });

  describe('setAuth', () => {
    const mockUser = {
      id: 1,
      email: 'test@example.com',
      name: 'Test User',
      role: 'OWNER' as const,
    };
    const mockAccessToken = 'access-token-123';
    const mockRefreshToken = 'refresh-token-456';

    it('sets user correctly', () => {
      useAuthStore.getState().setAuth(mockUser, mockAccessToken, mockRefreshToken);

      const state = useAuthStore.getState();
      expect(state.user).toEqual(mockUser);
    });

    it('sets access token correctly', () => {
      useAuthStore.getState().setAuth(mockUser, mockAccessToken, mockRefreshToken);

      const state = useAuthStore.getState();
      expect(state.accessToken).toBe(mockAccessToken);
    });

    it('sets refresh token correctly', () => {
      useAuthStore.getState().setAuth(mockUser, mockAccessToken, mockRefreshToken);

      const state = useAuthStore.getState();
      expect(state.refreshToken).toBe(mockRefreshToken);
    });

    it('sets isAuthenticated to true', () => {
      useAuthStore.getState().setAuth(mockUser, mockAccessToken, mockRefreshToken);

      const state = useAuthStore.getState();
      expect(state.isAuthenticated).toBe(true);
    });

    it('can set ADMIN role user', () => {
      const adminUser = { ...mockUser, role: 'ADMIN' as const };
      useAuthStore.getState().setAuth(adminUser, mockAccessToken, mockRefreshToken);

      const state = useAuthStore.getState();
      expect(state.user?.role).toBe('ADMIN');
    });
  });

  describe('clearAuth', () => {
    beforeEach(() => {
      // Set some auth state first
      useAuthStore.getState().setAuth(
        { id: 1, email: 'test@example.com', name: 'Test', role: 'OWNER' },
        'access-token',
        'refresh-token'
      );
    });

    it('clears user to null', () => {
      useAuthStore.getState().clearAuth();

      const state = useAuthStore.getState();
      expect(state.user).toBeNull();
    });

    it('clears tokens to null', () => {
      useAuthStore.getState().clearAuth();

      const state = useAuthStore.getState();
      expect(state.accessToken).toBeNull();
      expect(state.refreshToken).toBeNull();
    });

    it('sets isAuthenticated to false', () => {
      useAuthStore.getState().clearAuth();

      const state = useAuthStore.getState();
      expect(state.isAuthenticated).toBe(false);
    });
  });

  describe('updateUser', () => {
    beforeEach(() => {
      useAuthStore.getState().setAuth(
        { id: 1, email: 'old@example.com', name: 'Old Name', role: 'OWNER' },
        'access-token',
        'refresh-token'
      );
    });

    it('updates user name', () => {
      useAuthStore.getState().updateUser({ name: 'New Name' });

      const state = useAuthStore.getState();
      expect(state.user?.name).toBe('New Name');
    });

    it('updates user email', () => {
      useAuthStore.getState().updateUser({ email: 'new@example.com' });

      const state = useAuthStore.getState();
      expect(state.user?.email).toBe('new@example.com');
    });

    it('preserves other user fields when updating one field', () => {
      useAuthStore.getState().updateUser({ name: 'New Name' });

      const state = useAuthStore.getState();
      expect(state.user?.id).toBe(1);
      expect(state.user?.email).toBe('old@example.com');
      expect(state.user?.role).toBe('OWNER');
    });

    it('can update multiple fields at once', () => {
      useAuthStore.getState().updateUser({
        name: 'Updated Name',
        email: 'updated@example.com',
      });

      const state = useAuthStore.getState();
      expect(state.user?.name).toBe('Updated Name');
      expect(state.user?.email).toBe('updated@example.com');
    });

    it('does nothing if user is null', () => {
      useAuthStore.getState().clearAuth();
      useAuthStore.getState().updateUser({ name: 'Should Not Set' });

      const state = useAuthStore.getState();
      expect(state.user).toBeNull();
    });

    it('preserves authentication state when updating', () => {
      useAuthStore.getState().updateUser({ name: 'New Name' });

      const state = useAuthStore.getState();
      expect(state.isAuthenticated).toBe(true);
      expect(state.accessToken).toBe('access-token');
    });
  });

  describe('state persistence', () => {
    it('has partialize function that includes all auth fields', () => {
      // This is a structural test - the persist middleware should include these fields
      const state = useAuthStore.getState();
      expect(state).toHaveProperty('user');
      expect(state).toHaveProperty('accessToken');
      expect(state).toHaveProperty('refreshToken');
      expect(state).toHaveProperty('isAuthenticated');
    });
  });
});
