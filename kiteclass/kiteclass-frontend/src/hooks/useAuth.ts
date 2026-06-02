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
      const role = (u.role ?? u.roles?.[0] ?? 'STUDENT') as string;
      // KH returns UUID string for user.id; KC User type historically expected number.
      // Cast keeps store happy until User.id is broadened (tracked GAP-724 follow-up).
      const user = {
        id: u.id as unknown as number,
        email: u.email,
        name: u.name,
        userType: role as UserType,
        referenceId: undefined,
      };

      // Note: tenantId requires backend JWT update (JwtTokenProvider.java) to include tenantId claim.
      // Using development default until Gateway adds tenantId to JWT claims.
      const tenantId = data.accessToken
        ? (JSON.parse(atob(data.accessToken.split('.')[1] || '{}'))?.tenantId ?? '11111111-1111-1111-1111-111111111111')
        : '11111111-1111-1111-1111-111111111111';

      setAuth(user, data.accessToken, data.refreshToken, tenantId);

      // Store tokens in sessionStorage (per-tab isolation, GAP-830) for API client interceptor
      setTokens(data.accessToken, data.refreshToken, tenantId);

      toast({
        title: 'Login successful',
        description: `Welcome back, ${user.name}!`,
      });

      router.push('/dashboard');
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
    onSuccess: () => {
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
