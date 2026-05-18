'use client';

import { Suspense, useEffect, useState } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { CheckCircle2, XCircle, Loader2 } from 'lucide-react';
import { endpoints } from '@/lib/api/endpoints';
import axios from 'axios';
import { setTokens } from '@/lib/auth/jwt-storage';

function VerifyEmailContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const verifyCode = searchParams.get('token');
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    if (!verifyCode) {
      setStatus('error');
      setErrorMessage('Token không hợp lệ');
      return;
    }

    const verify = async () => {
      try {
        const response = await axios.post(
          `${endpoints.auth.verifyEmail}?token=${verifyCode}`
        );

        const data = response.data;
        if (data.accessToken) {
          // GAP-599 Wave 92 Bucket B: sessionStorage (per-tab isolation).
          setTokens(data.accessToken, data.refreshToken);
        }

        setStatus('success');
        setTimeout(() => router.push('/dashboard'), 2000);
      } catch (error: unknown) {
        setStatus('error');
        if (axios.isAxiosError(error) && error.response?.data?.detail) {
          setErrorMessage(error.response.data.detail);
        } else {
          setErrorMessage('Xác nhận email thất bại. Vui lòng thử lại.');
        }
      }
    };

    verify();
  }, [verifyCode, router]);

  return (
    <div className="flex items-center justify-center min-h-[60vh]">
      <Card className="w-full max-w-md shadow-soft">
        <CardContent className="pt-8 pb-8 text-center">
          {status === 'loading' && (
            <>
              <Loader2 className="h-12 w-12 animate-spin text-primary mx-auto mb-4" />
              <h2 className="text-xl font-bold">Đang xác nhận email...</h2>
              <p className="mt-2 text-muted-foreground">Vui lòng đợi trong giây lát</p>
            </>
          )}

          {status === 'success' && (
            <>
              <div className="rounded-full bg-green-100 dark:bg-green-950/30 p-4 w-fit mx-auto mb-4">
                <CheckCircle2 className="h-12 w-12 text-green-600" />
              </div>
              <h2 className="text-xl font-bold">Email đã được xác nhận!</h2>
              <p className="mt-2 text-muted-foreground">
                Trung tâm của bạn đang được khởi tạo. Đang chuyển đến Dashboard...
              </p>
            </>
          )}

          {status === 'error' && (
            <>
              <div className="rounded-full bg-red-100 dark:bg-red-950/30 p-4 w-fit mx-auto mb-4">
                <XCircle className="h-12 w-12 text-red-600" />
              </div>
              <h2 className="text-xl font-bold">Xác nhận thất bại</h2>
              <p className="mt-2 text-muted-foreground">{errorMessage}</p>
              <div className="mt-6 flex gap-3 justify-center">
                <Button variant="outline" onClick={() => router.push('/login')}>
                  Đăng nhập
                </Button>
                <Button onClick={() => router.push('/register')}>
                  Đăng ký lại
                </Button>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

export default function VerifyEmailPage() {
  return (
    <Suspense fallback={
      <div className="flex items-center justify-center min-h-[60vh]">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    }>
      <VerifyEmailContent />
    </Suspense>
  );
}
