'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useState, useRef } from 'react';
import HCaptcha from '@hcaptcha/react-hcaptcha';
import { registerSchema, type RegisterFormData } from '@/lib/validations/auth';
import { useAuthStore } from '@/stores/auth-store';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import { KiteLogo } from '@/components/brand/KiteLogo';

export default function RegisterPage() {
  const router = useRouter();
  const { setAuth } = useAuthStore();
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [captchaToken, setCaptchaToken] = useState<string | null>(null);
  const captchaRef = useRef<HCaptcha>(null);

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
      const response = await apiClient.post(endpoints.auth.register, {
        organizationName: data.organizationName,
        subdomain: data.subdomain,
        ownerEmail: data.ownerEmail,
        ownerPassword: data.ownerPassword,
        captchaToken: captchaToken,
      });
      const { user, accessToken, refreshToken } = response.data;
      setAuth(user, accessToken, refreshToken);
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      router.push('/dashboard');
    } catch {
      setError('Đăng ký thất bại. Subdomain hoặc email có thể đã được sử dụng.');
      // Reset captcha on error
      captchaRef.current?.resetCaptcha();
      setCaptchaToken(null);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="mb-8">
        <Link href="/"><KiteLogo size="md" /></Link>
        <h1 className="mt-6 text-2xl font-bold tracking-tight">Đăng ký dùng thử 14 ngày</h1>
        <p className="mt-1 text-sm text-muted-foreground">Không cần thẻ tín dụng • Hủy bất kỳ lúc nào</p>
      </div>

      {error && (
        <div className="mb-6 flex items-center gap-2 rounded-xl bg-destructive/10 p-4 text-sm text-destructive">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        <div>
          <label className="block text-sm font-medium mb-1.5">Tên tổ chức</label>
          <input
            {...register('organizationName')}
            className="w-full rounded-xl border bg-background px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-colors"
            placeholder="Trung tâm Anh ngữ ABC"
          />
          {errors.organizationName && (
            <p className="mt-1.5 text-xs text-destructive">{errors.organizationName.message}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium mb-1.5">Subdomain</label>
          <div className="flex items-center">
            <input
              {...register('subdomain')}
              className="w-full rounded-l-xl border bg-background px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-colors"
              placeholder="abc-center"
            />
            <span className="rounded-r-xl border border-l-0 bg-muted px-4 py-3 text-sm text-muted-foreground whitespace-nowrap">
              .kiteclass.com
            </span>
          </div>
          {subdomain && (
            <p className="mt-1.5 text-xs text-primary font-medium">
              🌐 https://{subdomain}.kiteclass.com
            </p>
          )}
          {errors.subdomain && (
            <p className="mt-1.5 text-xs text-destructive">{errors.subdomain.message}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium mb-1.5">Email</label>
          <input
            type="email"
            {...register('ownerEmail')}
            className="w-full rounded-xl border bg-background px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-colors"
            placeholder="email@example.com"
          />
          {errors.ownerEmail && (
            <p className="mt-1.5 text-xs text-destructive">{errors.ownerEmail.message}</p>
          )}
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium mb-1.5">Mật khẩu</label>
            <input
              type="password"
              {...register('ownerPassword')}
              className="w-full rounded-xl border bg-background px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-colors"
            />
            {errors.ownerPassword && (
              <p className="mt-1.5 text-xs text-destructive">{errors.ownerPassword.message}</p>
            )}
          </div>
          <div>
            <label className="block text-sm font-medium mb-1.5">Xác nhận mật khẩu</label>
            <input
              type="password"
              {...register('confirmPassword')}
              className="w-full rounded-xl border bg-background px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-colors"
            />
            {errors.confirmPassword && (
              <p className="mt-1.5 text-xs text-destructive">{errors.confirmPassword.message}</p>
            )}
          </div>
        </div>

        {process.env.NEXT_PUBLIC_HCAPTCHA_SITE_KEY && (
          <div className="flex justify-center">
            <HCaptcha
              ref={captchaRef}
              sitekey={process.env.NEXT_PUBLIC_HCAPTCHA_SITE_KEY}
              onVerify={(token) => setCaptchaToken(token)}
              onExpire={() => setCaptchaToken(null)}
              onError={() => setCaptchaToken(null)}
            />
          </div>
        )}

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-xl bg-primary py-3 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-50 transition-colors"
        >
          {loading ? 'Đang tạo tài khoản...' : 'Tạo tài khoản dùng thử'}
        </button>
      </form>

      <p className="mt-8 text-center text-sm text-muted-foreground">
        Đã có tài khoản?{' '}
        <Link href="/login" className="font-medium text-primary hover:underline">
          Đăng nhập
        </Link>
      </p>
    </div>
  );
}
