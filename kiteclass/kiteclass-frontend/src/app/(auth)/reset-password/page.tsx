/**
 * Reset password page.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import { useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { AuthLayout } from '@/components/layout';
import { FormInput } from '@/components/forms';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { useAuth } from '@/hooks/useAuth';
import { LoadingSpinner } from '@/components/common';

const resetPasswordSchema = z
  .object({
    password: z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
      .regex(/[a-z]/, 'Password must contain at least one lowercase letter')
      .regex(/[0-9]/, 'Password must contain at least one number'),
    confirmPassword: z.string(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords don't match",
    path: ['confirmPassword'],
  });

type ResetPasswordFormData = z.infer<typeof resetPasswordSchema>;

export default function ResetPasswordPage() {
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
      <AuthLayout>
        <div className="space-y-6">
          <div className="space-y-2 text-center">
            <h1 className="text-3xl font-bold">Invalid reset link</h1>
            <p className="text-muted-foreground">
              This password reset link is invalid or has expired.
            </p>
          </div>

          <Alert variant="destructive">
            <AlertDescription>
              Please request a new password reset link.
            </AlertDescription>
          </Alert>

          <Link href="/forgot-password" className="block">
            <Button className="w-full">Request new link</Button>
          </Link>
        </div>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout>
      <div className="space-y-6">
        <div className="space-y-2 text-center">
          <h1 className="text-3xl font-bold">Reset password</h1>
          <p className="text-muted-foreground">
            Enter your new password below.
          </p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <FormInput
            label="New password"
            type="password"
            placeholder="••••••••"
            error={errors.password?.message}
            helperText="Must be at least 8 characters with uppercase, lowercase, and number"
            disabled={isSubmitting}
            {...register('password')}
          />

          <FormInput
            label="Confirm password"
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
                Resetting password...
              </>
            ) : (
              'Reset password'
            )}
          </Button>
        </form>

        <Link href="/login" className="block text-center">
          <Button variant="link">Back to login</Button>
        </Link>
      </div>
    </AuthLayout>
  );
}
