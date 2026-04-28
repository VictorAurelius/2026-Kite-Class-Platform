/**
 * Reset-password form — lazy-loaded body of `/reset-password` page.
 *
 * Reads the `token` query param via `useSearchParams` (must run inside
 * a Suspense boundary; the page wrapper provides it). Defers
 * react-hook-form + zod past the initial route payload.
 *
 * GAP-236 Sub-PR B Agent A — code-splitting for auth pages.
 *
 * @author KiteClass Team
 */

'use client';

import { useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { FormInput } from '@/components/forms';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { useAuth } from '@/hooks/useAuth';
import { LoadingSpinner } from '@/components/common';

const resetPasswordSchema = z
  .object({
    password: z
      .string()
      .min(8, 'Mật khẩu phải có ít nhất 8 ký tự')
      .regex(/[A-Z]/, 'Mật khẩu phải có ít nhất một chữ hoa')
      .regex(/[a-z]/, 'Mật khẩu phải có ít nhất một chữ thường')
      .regex(/[0-9]/, 'Mật khẩu phải có ít nhất một chữ số'),
    confirmPassword: z.string(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Mật khẩu không khớp',
    path: ['confirmPassword'],
  });

type ResetPasswordFormData = z.infer<typeof resetPasswordSchema>;

export function ResetPasswordForm() {
  const searchParams = useSearchParams();
  const token = searchParams.get('token');
  const { resetPassword } = useAuth();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordFormData>({
    resolver: zodResolver(resetPasswordSchema),
  });

  const onSubmit = async (data: ResetPasswordFormData) => {
    if (!token) return;
    await resetPassword({ token, newPassword: data.password });
  };

  if (!token) {
    return (
      <div className="space-y-6">
        <div className="space-y-2 text-center">
          <h1 className="text-3xl font-bold">Liên kết không hợp lệ</h1>
          <p className="text-muted-foreground">
            Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.
          </p>
        </div>

        <Alert variant="destructive">
          <AlertDescription>
            Vui lòng yêu cầu liên kết đặt lại mật khẩu mới.
          </AlertDescription>
        </Alert>

        <Link href="/forgot-password" className="block">
          <Button className="w-full">Yêu cầu liên kết mới</Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="space-y-2 text-center">
        <h1 className="text-3xl font-bold">Đặt lại mật khẩu</h1>
        <p className="text-muted-foreground">
          Nhập mật khẩu mới của bạn bên dưới.
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <FormInput
          label="Mật khẩu mới"
          type="password"
          placeholder="••••••••"
          error={errors.password?.message}
          helperText="Tối thiểu 8 ký tự, có chữ hoa, chữ thường và số"
          disabled={isSubmitting}
          {...register('password')}
        />

        <FormInput
          label="Xác nhận mật khẩu"
          type="password"
          placeholder="••••••••"
          error={errors.confirmPassword?.message}
          disabled={isSubmitting}
          {...register('confirmPassword')}
        />

        <Button type="submit" className="w-full" disabled={isSubmitting}>
          {isSubmitting ? (
            <>
              <LoadingSpinner size="sm" className="mr-2" />
              Đang đặt lại mật khẩu...
            </>
          ) : (
            'Đặt lại mật khẩu'
          )}
        </Button>
      </form>

      <Link href="/login" className="block text-center">
        <Button variant="link">Quay lại đăng nhập</Button>
      </Link>
    </div>
  );
}
