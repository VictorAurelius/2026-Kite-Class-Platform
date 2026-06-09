/**
 * Authentication hook.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { authApi } from '@/lib/api/auth';
import { useAuthStore } from '@/stores/auth-store';
import { setTokens, clearTokens } from '@/lib/auth/jwt-storage';
import type { LoginRequest } from '@/types/auth';
import { UserType } from '@/types/auth';
import { normalizeRole, roleHome } from '@/lib/auth/roles';
import { toast } from '@/hooks/use-toast';

export function useAuth() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { user, isAuthenticated, setAuth, clearAuth } = useAuthStore();

  const loginMutation = useMutation({
    mutationFn: (credentials: LoginRequest) => authApi.login(credentials),
    onSuccess: (data) => {
      // KH /api/auth/login returns flat shape: { user: {id,email,name,role (singular)}, accessToken, refreshToken, instances[] }
      // Wave 105 RST UI 2026-05-23 GAP-724: adapt to actual server contract (was assuming roles[]/profile{}).
      const u = data.user as unknown as { id: string | number; email: string; name: string; role?: string; roles?: string[] };
      // Wave RBAC-Shell 1 Bucket A (GAP-1122): normalize the BE role token (any of
      // the tenant-auth / hierarchical / @PreAuthorize vocabularies) into the FE
      // canonical role so guards + redirect agree. `null` = unrecognized role.
      const rawRole = (u.role ?? u.roles?.[0]) as string | undefined;
      const role = normalizeRole(rawRole);
      // KH returns UUID string for user.id; KC User type historically expected number.
      // Cast keeps store happy until User.id is broadened (tracked GAP-724 follow-up).
      const user = {
        id: u.id as unknown as number,
        email: u.email,
        name: u.name,
        userType: (role ?? (rawRole as UserType) ?? UserType.STUDENT),
        referenceId: undefined,
      };

      // Note: tenantId requires backend JWT update (JwtTokenProvider.java) to include tenantId claim.
      // Using development default until Gateway adds tenantId to JWT claims.
      const tenantId = data.accessToken
        ? (JSON.parse(atob(data.accessToken.split('.')[1] || '{}'))?.tenantId ?? '11111111-1111-1111-1111-111111111111')
        : '11111111-1111-1111-1111-111111111111';

      // Bind the tab + persist tokens FIRST (GAP-1074: tenant-scoped localStorage).
      // Must precede setAuth so the zustand persist blob lands in this tenant's
      // namespace (`kc:<tenantId>:auth-store`) rather than the anon fallback.
      setTokens(data.accessToken, data.refreshToken, tenantId);

      setAuth(user, data.accessToken, data.refreshToken, tenantId);

      toast({
        title: 'Login successful',
        description: `Welcome back, ${user.name}!`,
      });

      // Role-based redirect (GAP-1122): land each role on its own home. Unknown
      // roles fall back to the shared dashboard shell (never guess a persona route).
      router.push(role ? roleHome(role) : '/dashboard');
    },
    onError: (error: Error) => {
      toast({
        title: 'Login failed',
        description: error.message || 'Invalid email or password',
        variant: 'destructive',
      });
    },
  });

  const logoutMutation = useMutation({
    mutationFn: () => {
      const refreshToken = useAuthStore.getState().refreshToken;
      return refreshToken ? authApi.logout(refreshToken) : Promise.resolve();
    },
    // onSettled (not onSuccess): local logout MUST always complete even if a future
    // server-side revocation call (GAP-1075) fails — never strand the user logged in.
    onSettled: () => {
      clearAuth();
      clearTokens();
      queryClient.clear();

      toast({
        title: 'Logged out',
        description: 'You have been logged out successfully.',
      });

      router.push('/login');
    },
  });

  const forgotPasswordMutation = useMutation({
    mutationFn: (email: string) => authApi.forgotPassword(email),
    onSuccess: () => {
      toast({
        title: 'Email sent',
        description: 'Password reset instructions have been sent to your email.',
      });
    },
    onError: (error: Error) => {
      toast({
        title: 'Failed to send email',
        description: error.message,
        variant: 'destructive',
      });
    },
  });

  const resetPasswordMutation = useMutation({
    mutationFn: ({ token, newPassword }: { token: string; newPassword: string }) =>
      authApi.resetPassword(token, newPassword),
    onSuccess: () => {
      toast({
        title: 'Password reset successful',
        description: 'You can now login with your new password.',
      });
      router.push('/login');
    },
    onError: (error: Error) => {
      toast({
        title: 'Failed to reset password',
        description: error.message,
        variant: 'destructive',
      });
    },
  });

  return {
    user,
    isAuthenticated,
    login: loginMutation.mutate,
    logout: logoutMutation.mutate,
    forgotPassword: forgotPasswordMutation.mutate,
    resetPassword: resetPasswordMutation.mutate,
    isLoggingIn: loginMutation.isPending,
    isLoggingOut: logoutMutation.isPending,
  };
}
