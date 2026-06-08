/**
 * Authentication state management with Zustand.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { User } from '@/types/auth';
import { tenantScopedStateStorage } from '@/lib/auth/jwt-storage';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  tenantId: string | null;
  isAuthenticated: boolean;

  // Actions
  setAuth: (user: User, accessToken: string, refreshToken: string, tenantId: string) => void;
  clearAuth: () => void;
  updateUser: (user: Partial<User>) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      tenantId: null,
      isAuthenticated: false,

      setAuth: (user, accessToken, refreshToken, tenantId) =>
        set({
          user,
          accessToken,
          refreshToken,
          tenantId,
          isAuthenticated: true,
        }),

      clearAuth: () =>
        set({
          user: null,
          accessToken: null,
          refreshToken: null,
          tenantId: null,
          isAuthenticated: false,
        }),

      updateUser: (updatedUser) =>
        set((state) => ({
          user: state.user ? { ...state.user, ...updatedUser } : null,
        })),
    }),
    {
      name: 'auth-storage',
      // GAP-1074 (Option B, supersedes GAP-830): tenant-scoped localStorage. Persists
      // cross-tab (no re-login when opening a URL in a new tab) while namespacing the
      // blob per tenant (`kc:<tenantId>:auth-store`) so two tabs on different tenants
      // never clobber each other — the exact GAP-830 collision concern, solved without
      // losing cross-tab UX. See `tenantScopedStateStorage` in lib/auth/jwt-storage.
      storage: createJSONStorage(() => tenantScopedStateStorage),
      partialize: (state) => ({
        user: state.user,
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        tenantId: state.tenantId,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
