/**
 * Register page - Choose account type.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { AuthLayout } from '@/components/layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { GraduationCap, Building2 } from 'lucide-react';

export default function RegisterPage() {
  const router = useRouter();
  const kitehubUrl = process.env.NEXT_PUBLIC_KITEHUB_URL || 'https://kitehub.me';
  // Phase 1 BETA invite-only — direct centers to beta access request form
  // instead of public self-signup. See GAP-372 closure follow-up #2.
  const kitehubRegisterUrl = `${kitehubUrl}/auth/request-beta-access`;

  return (
    <AuthLayout>
      <div className="space-y-6">
        <div className="space-y-2 text-center">
          <h1 className="text-3xl font-bold">Tạo tài khoản</h1>
          <p className="text-muted-foreground">
            Chọn loại tài khoản bạn muốn đăng ký
          </p>
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <Card className="cursor-pointer hover:border-primary transition-colors" onClick={() => router.push('/register/student')}>
            <CardHeader>
              <div className="flex items-center space-x-2">
                <GraduationCap className="h-6 w-6 text-primary" />
                <CardTitle>Học viên</CardTitle>
              </div>
              <CardDescription>
                Dành cho học viên muốn đăng ký khóa học
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Button className="w-full" onClick={(e) => {
                e.stopPropagation();
                router.push('/register/student');
              }}>
                Đăng ký học viên
              </Button>
            </CardContent>
          </Card>

          <Card
            className="cursor-pointer hover:border-primary transition-colors"
            onClick={() => window.open(kitehubRegisterUrl, '_blank')}
          >
            <CardHeader>
              <div className="flex items-center space-x-2">
                <Building2 className="h-6 w-6 text-primary" />
                <CardTitle>Trung tâm</CardTitle>
              </div>
              <CardDescription>
                Dành cho trung tâm giáo dục — gửi yêu cầu Beta để được mời sử dụng
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Button
                className="w-full"
                variant="outline"
                onClick={(e) => {
                  e.stopPropagation();
                  window.open(kitehubRegisterUrl, '_blank');
                }}
              >
                Yêu cầu Beta ↗
              </Button>
            </CardContent>
          </Card>
        </div>

        <div className="text-center text-sm">
          <span className="text-muted-foreground">Đã có tài khoản? </span>
          <Link href="/login" className="font-medium text-primary hover:underline">
            Đăng nhập
          </Link>
        </div>
      </div>
    </AuthLayout>
  );
}
