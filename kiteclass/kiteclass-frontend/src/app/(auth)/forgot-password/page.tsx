/**
 * Forgot password page.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

export const dynamic = 'force-dynamic';

import { useState } from 'react';
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
import { ArrowLeft, CheckCircle } from 'lucide-react';

const forgotPasswordSchema = z.object({
  email: z.string().email('Email không hợp lệ'),
});

type ForgotPasswordFormData = z.infer<typeof forgotPasswordSchema>;

export default function ForgotPasswordPage() {
  const { forgotPassword } = useAuth();
  const [emailSent, setEmailSent] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
    getValues,
  } = useForm<ForgotPasswordFormData>({
    resolver: zodResolver(forgotPasswordSchema),
  });

  const onSubmit = async (data: ForgotPasswordFormData) => {
    setIsLoading(true);
    try {
      await forgotPassword(data.email);
      setEmailSent(true);
    } finally {
      setIsLoading(false);
    }
  };

  if (emailSent) {
    return (
      <AuthLayout>
        <div className="space-y-6">
          <div className="flex justify-center">
            <div className="rounded-full bg-green-100 p-3 dark:bg-green-900">
              <CheckCircle className="h-8 w-8 text-green-600 dark:text-green-400" />
            </div>
          </div>

          <div className="space-y-2 text-center">
            <h1 className="text-3xl font-bold">Kiểm tra email của bạn</h1>
            <p className="text-muted-foreground">
              Chúng tôi đã gửi hướng dẫn đặt lại mật khẩu tới{' '}
              <span className="font-medium text-foreground">{getValues('email')}</span>
            </p>
          </div>

          <Alert>
            <AlertDescription>
              Nếu không thấy email, hãy kiểm tra thư mục spam hoặc thử gửi lại.
            </AlertDescription>
          </Alert>

          <div className="space-y-3">
            <Button
              variant="outline"
              className="w-full"
              onClick={() => setEmailSent(false)}
            >
              Gửi lại
            </Button>

            <Link href="/login" className="block">
              <Button variant="ghost" className="w-full">
                <ArrowLeft className="mr-2 h-4 w-4" />
                Quay lại đăng nhập
              </Button>
            </Link>
          </div>
        </div>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout>
      <div className="space-y-6">
        <div className="space-y-2 text-center">
          <h1 className="text-3xl font-bold">Quên mật khẩu?</h1>
          <p className="text-muted-foreground">
            Đừng lo, chúng tôi sẽ gửi hướng dẫn đặt lại mật khẩu tới email của bạn.
          </p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <FormInput
            label="Email"
            type="email"
            placeholder="ban@example.com"
            error={errors.email?.message}
            disabled={isLoading}
            {...register('email')}
          />

          <Button type="submit" className="w-full" disabled={isLoading}>
            {isLoading ? (
              <>
                <LoadingSpinner size="sm" className="mr-2" />
                Đang gửi...
              </>
            ) : (
              'Gửi hướng dẫn đặt lại mật khẩu'
            )}
          </Button>
        </form>

        <Link href="/login" className="block">
          <Button variant="ghost" className="w-full">
            <ArrowLeft className="mr-2 h-4 w-4" />
            Quay lại đăng nhập
          </Button>
        </Link>
      </div>
    </AuthLayout>
  );
}
