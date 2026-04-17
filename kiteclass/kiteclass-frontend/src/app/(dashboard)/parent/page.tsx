/**
 * Parent portal — dashboard skeleton (Wave 2 MVP, GAP-052a).
 *
 * <p>MVP scope: greet the signed-in parent and render the list of linked
 * children with their names. Wave 5 will layer attendance, grades, invoices,
 * and messaging widgets on top of this shell.
 *
 * <p>Route is gated by the {@code (dashboard)/layout.tsx} auth check; we
 * additionally verify that the hydrated JWT carries the {@code PARENT} role
 * and redirect non-parents back to the generic dashboard.
 *
 * @author KiteClass Team
 * @since 3.14.0 (Wave 2 — GAP-052a)
 */

'use client';

export const dynamic = 'force-dynamic';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Users } from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import { useAuthStore } from '@/stores/auth-store';
import { UserType } from '@/types/auth';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { LoadingSpinner } from '@/components/common/loading-spinner';

interface ParentProfile {
  id: number;
  fullName: string;
  email: string;
  phoneNumber: string | null;
  relationship: 'FATHER' | 'MOTHER' | 'GUARDIAN';
  status: 'PENDING' | 'ACTIVE' | 'INACTIVE';
}

interface ChildSummary {
  studentId: number;
  studentName: string;
  className: string | null;
  grade: string | null;
  linkType: 'PRIMARY' | 'SECONDARY';
}

interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  message?: string;
}

export default function ParentDashboardPage() {
  const router = useRouter();
  const userType = useAuthStore((state) => state.user?.userType);
  const [profile, setProfile] = useState<ParentProfile | null>(null);
  const [children, setChildren] = useState<ChildSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Guard: parent accounts only. Non-parents land on the main dashboard.
  useEffect(() => {
    if (userType && userType !== UserType.PARENT) {
      router.replace('/dashboard');
    }
  }, [userType, router]);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const [meResp, kidsResp] = await Promise.all([
          apiClient.get<ApiEnvelope<ParentProfile>>('/api/v1/parent/me'),
          apiClient.get<ApiEnvelope<ChildSummary[]>>('/api/v1/parent/me/children'),
        ]);
        if (cancelled) return;
        setProfile(meResp.data.data);
        setChildren(kidsResp.data.data ?? []);
      } catch (err) {
        if (cancelled) return;
        const message =
          err instanceof Error ? err.message : 'Không thể tải dữ liệu phụ huynh';
        setError(message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-6">
      <header className="space-y-1">
        <h1 className="text-3xl font-bold">
          Xin chào{profile ? `, ${profile.fullName}` : ''}
        </h1>
        <p className="text-muted-foreground">
          {children.length > 0
            ? `Bạn đang liên kết với ${children.length} con.`
            : 'Chưa có con nào được liên kết với tài khoản của bạn.'}
        </p>
      </header>

      {error ? (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      ) : null}

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Users className="h-5 w-5" />
            Danh sách con
          </CardTitle>
        </CardHeader>
        <CardContent>
          {children.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              Bạn chưa có con nào. Liên hệ trường để được thêm liên kết.
            </p>
          ) : (
            <ul className="divide-y">
              {children.map((child) => (
                <li key={child.studentId} className="flex items-center justify-between py-3">
                  <div>
                    <p className="font-medium">{child.studentName}</p>
                    <p className="text-xs text-muted-foreground">
                      {child.className ?? 'Chưa có lớp'} ·{' '}
                      {child.grade ?? 'Chưa có khối'} ·{' '}
                      {child.linkType === 'PRIMARY' ? 'Liên kết chính' : 'Liên kết phụ'}
                    </p>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>

      <p className="text-center text-xs text-muted-foreground">
        Các widget điểm danh, học lực và học phí sẽ có trong Wave 5.
      </p>
    </div>
  );
}
