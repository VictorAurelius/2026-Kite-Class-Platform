'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { loginSchema, type LoginFormData } from '@/lib/validations/auth';
import { useAuthStore } from '@/stores/auth-store';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';

export default function LoginPage() {
  const router = useRouter();
  const { setAuth } = useAuthStore();
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginFormData) => {
    setError(null);
    setLoading(true);
    try {
      const response = await apiClient.post(endpoints.auth.login, data);
      const { user, accessToken, refreshToken } = response.data;
      setAuth(user, accessToken, refreshToken);
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      router.push(user.role === 'ADMIN' ? '/admin' : '/dashboard');
    } catch {
      setError('Email hoặc mật khẩu không đúng');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="rounded-lg border bg-card p-8 shadow-sm">
      <div className="text-center">
        <Link href="/" className="text-2xl font-bold text-primary">KiteHub</Link>
        <h1 className="mt-4 text-xl font-semibold">Đăng nhập</h1>
      </div>

      {error && (
        <div className="mt-4 rounded-md bg-destructive/10 p-3 text-sm text-destructive">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4">
        <div>
          <label className="block text-sm font-medium">Email</label>
          <input
            type="email"
            {...register('email')}
            className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            placeholder="email@example.com"
          />
          {errors.email && (
            <p className="mt-1 text-xs text-destructive">{errors.email.message}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium">Mật khẩu</label>
          <input
            type="password"
            {...register('password')}
            className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
          />
          {errors.password && (
            <p className="mt-1 text-xs text-destructive">{errors.password.message}</p>
          )}
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-md bg-primary py-2.5 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
        >
          {loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-muted-foreground">
        Chưa có tài khoản?{' '}
        <Link href="/register" className="font-medium text-primary hover:underline">
          Đăng ký dùng thử
        </Link>
      </p>
    </div>
  );
}
