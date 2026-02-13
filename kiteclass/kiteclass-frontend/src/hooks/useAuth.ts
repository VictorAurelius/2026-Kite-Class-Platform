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
import type { LoginRequest } from '@/types/auth';
import { toast } from '@/hooks/use-toast';

export function useAuth() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { user, isAuthenticated, setAuth, clearAuth } = useAuthStore();

  const loginMutation = useMutation({
    mutationFn: (credentials: LoginRequest) => authApi.login(credentials),
    onSuccess: (data) => {
      // Construct User object from AuthResponse
      const user = {
        id: data.user.id,
        email: data.user.email,
        name: data.user.name,
        userType: data.user.roles[0] as UserType, // Use first role as userType
        referenceId: data.user.profile?.id.toString(),
      };

      // Extract tenantId from JWT access token (for now use a placeholder)
      const tenantId = '11111111-1111-1111-1111-111111111111'; // TODO: Extract from JWT

      setAuth(user, data.accessToken, data.refreshToken, tenantId);

      // Store tokens in localStorage for API client interceptor
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('refreshToken', data.refreshToken);
      localStorage.setItem('tenantId', tenantId);

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
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('tenantId');
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
