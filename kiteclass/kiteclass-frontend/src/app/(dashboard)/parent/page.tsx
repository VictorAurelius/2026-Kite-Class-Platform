/**
 * Parent portal landing — children selector + transcript drill-in.
 *
 * Phase 1A (GAP-321 — Wave 18b1 Bucket D): replaces the Wave 2 GAP-052a
 * skeleton with a proper React Query hook (`useMyChildren`) and a
 * "Xem học bạ" CTA per child that links to `/parent/transcript/[childId]`.
 *
 * Phase 1B (GAP-321b) deliverables NOT in this page yet:
 * - Per-child cards for điểm danh / học phí / hạnh kiểm / notifications / kỷ luật
 * - className + grade enrichment (currently null until subject-grade joins ship)
 *
 * @author KiteClass Team
 * @since 2.18.0 (Wave 18b1 — GAP-321 Phase 1A; replaces 3.14.0 Wave 2 skeleton)
 */

'use client';

export const dynamic = 'force-dynamic';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { FileText, GraduationCap, Users } from 'lucide-react';
import { useAuthStore } from '@/stores/auth-store';
import { UserType } from '@/types/auth';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { LoadingSpinner } from '@/components/common/loading-spinner';
import { ErrorAlert } from '@/components/common/error-alert';
import { useMyChildren, useParentMe } from '@/hooks/use-parent';

export default function ParentDashboardPage() {
  const router = useRouter();
  const userType = useAuthStore((state) => state.user?.userType);

  // Guard: parent accounts only. Non-parents bounce back to the main dashboard.
  useEffect(() => {
    if (userType && userType !== UserType.PARENT) {
      router.replace('/dashboard');
    }
  }, [userType, router]);

  const { data: profile, isLoading: loadingMe } = useParentMe();
  const {
    data: children,
    isLoading: loadingKids,
    isError,
    error,
    refetch,
  } = useMyChildren();

  if (loadingMe || loadingKids) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <LoadingSpinner size="lg" text="Đang tải dữ liệu phụ huynh..." />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="mx-auto max-w-4xl p-6">
        <ErrorAlert
          title="Không tải được danh sách con"
          message={error instanceof Error ? error.message : 'Lỗi hệ thống'}
          onRetry={() => refetch()}
        />
      </div>
    );
  }

  const list = children ?? [];

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-6">
      <header className="space-y-2">
        <h1 className="flex items-center gap-2 text-3xl font-bold">
          <GraduationCap className="h-8 w-8 text-primary" />
          Cổng phụ huynh{profile ? ` — ${profile.fullName}` : ''}
        </h1>
        <p className="text-muted-foreground">
          Xem học bạ và thông tin học tập của con. Phụ huynh chỉ có quyền xem dữ
          liệu các con đã được liên kết với tài khoản của mình.
        </p>
      </header>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Users className="h-5 w-5" />
            Danh sách con
          </CardTitle>
          <CardDescription>
            {list.length > 0
              ? `Bạn đang liên kết với ${list.length} con.`
              : 'Chưa có con nào được liên kết với tài khoản của bạn.'}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {list.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              Liên hệ nhà trường để được gửi lại lời mời liên kết tài khoản phụ
              huynh.
            </p>
          ) : (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {list.map((child) => (
                <Card
                  key={child.studentId}
                  data-testid={`child-card-${child.studentId}`}
                >
                  <CardHeader>
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0">
                        <CardTitle className="text-lg">
                          {child.studentName}
                        </CardTitle>
                        {(child.className || child.grade) && (
                          <CardDescription>
                            {[child.grade, child.className]
                              .filter(Boolean)
                              .join(' · ')}
                          </CardDescription>
                        )}
                      </div>
                      <Badge
                        variant={
                          child.linkType === 'PRIMARY' ? 'default' : 'secondary'
                        }
                      >
                        {child.linkType === 'PRIMARY' ? 'Chính' : 'Phụ'}
                      </Badge>
                    </div>
                  </CardHeader>
                  <CardContent className="text-sm text-muted-foreground">
                    {/* Phase 1B (GAP-321b) sẽ thêm điểm danh / học phí / hạnh kiểm / kỷ luật. */}
                    Hiện bạn có thể xem học bạ. Các mục khác (điểm danh, học phí,
                    hạnh kiểm, thông báo, kỷ luật) sẽ ra mắt trong các đợt tiếp
                    theo.
                  </CardContent>
                  <CardFooter>
                    <Button
                      asChild
                      className="w-full"
                      data-testid={`view-transcript-${child.studentId}`}
                    >
                      <Link href={`/parent/transcript/${child.studentId}`}>
                        <FileText className="mr-2 h-4 w-4" />
                        Xem học bạ
                      </Link>
                    </Button>
                  </CardFooter>
                </Card>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <p className="text-center text-xs text-muted-foreground">
        Phase 1A — học bạ. Các widget điểm danh, học phí, hạnh kiểm, thông báo,
        kỷ luật sẽ có trong Phase 1B (GAP-321b).
      </p>
    </div>
  );
}
