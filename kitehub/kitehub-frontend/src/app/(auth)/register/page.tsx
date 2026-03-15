'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { registerSchema, type RegisterFormData } from '@/lib/validations/auth';
import { useAuthStore } from '@/stores/auth-store';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';

export default function RegisterPage() {
  const router = useRouter();
  const { setAuth } = useAuthStore();
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
  });

  const subdomain = watch('subdomain');

  const onSubmit = async (data: RegisterFormData) => {
    setError(null);
    setLoading(true);
    try {
      const response = await apiClient.post(endpoints.instances.create, {
        organizationName: data.organizationName,
        subdomain: data.subdomain,
        ownerEmail: data.ownerEmail,
        ownerPassword: data.ownerPassword,
      });
      const { user, accessToken, refreshToken } = response.data.data;
      setAuth(user, accessToken, refreshToken);
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      router.push('/dashboard');
    } catch {
      setError('Đăng ký thất bại. Subdomain hoặc email có thể đã được sử dụng.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="rounded-lg border bg-card p-8 shadow-sm">
      <div className="text-center">
        <Link href="/" className="text-2xl font-bold text-primary">KiteHub</Link>
        <h1 className="mt-4 text-xl font-semibold">Đăng ký dùng thử 14 ngày</h1>
        <p className="mt-1 text-sm text-muted-foreground">Không cần thẻ tín dụng</p>
      </div>

      {error && (
        <div className="mt-4 rounded-md bg-destructive/10 p-3 text-sm text-destructive">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4">
        <div>
          <label className="block text-sm font-medium">Tên tổ chức</label>
          <input
            {...register('organizationName')}
            className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            placeholder="Trung tâm Anh ngữ ABC"
          />
          {errors.organizationName && (
            <p className="mt-1 text-xs text-destructive">{errors.organizationName.message}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium">Subdomain</label>
          <div className="mt-1 flex items-center">
            <input
              {...register('subdomain')}
              className="w-full rounded-l-md border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              placeholder="abc-center"
            />
            <span className="rounded-r-md border border-l-0 bg-muted px-3 py-2 text-sm text-muted-foreground">
              .kiteclass.com
            </span>
          </div>
          {subdomain && (
            <p className="mt-1 text-xs text-muted-foreground">
              URL: https://{subdomain}.kiteclass.com
            </p>
          )}
          {errors.subdomain && (
            <p className="mt-1 text-xs text-destructive">{errors.subdomain.message}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium">Email</label>
          <input
            type="email"
            {...register('ownerEmail')}
            className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            placeholder="email@example.com"
          />
          {errors.ownerEmail && (
            <p className="mt-1 text-xs text-destructive">{errors.ownerEmail.message}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium">Mật khẩu</label>
          <input
            type="password"
            {...register('ownerPassword')}
            className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
          />
          {errors.ownerPassword && (
            <p className="mt-1 text-xs text-destructive">{errors.ownerPassword.message}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium">Xác nhận mật khẩu</label>
          <input
            type="password"
            {...register('confirmPassword')}
            className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
          />
          {errors.confirmPassword && (
            <p className="mt-1 text-xs text-destructive">{errors.confirmPassword.message}</p>
          )}
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-md bg-primary py-2.5 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
        >
          {loading ? 'Đang tạo...' : 'Tạo tài khoản dùng thử'}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-muted-foreground">
        Đã có tài khoản?{' '}
        <Link href="/login" className="font-medium text-primary hover:underline">
          Đăng nhập
        </Link>
      </p>
    </div>
  );
}
