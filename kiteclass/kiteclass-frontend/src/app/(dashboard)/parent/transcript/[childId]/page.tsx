/**
 * Read-only transcript view for one of the parent's linked children.
 *
 * Phase 1A (GAP-321 — Wave 18b1 Bucket D): single facet, semester cards
 * showing GPA + course summary. Server enforces BR-PARENT-PORTAL-001 scope
 * guard — 403 PARENT_NOT_LINKED on unlinked children, surfaced via React Query
 * `isError` and rendered as ErrorAlert (no information leak).
 *
 * @author KiteClass Team
 * @since 2.18.0 (Wave 18b1 — GAP-321 Phase 1A)
 */

// shell-exempt: full-width học bạ document/print view (max-w-5xl) with self-contained header + back-nav; ParentShell mobile 480px shell incompatible by design
'use client';

export const dynamic = 'force-dynamic';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { ArrowLeft, FileText } from 'lucide-react';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { LoadingSpinner } from '@/components/common/loading-spinner';
import { ErrorAlert } from '@/components/common/error-alert';
import { useChildTranscript } from '@/hooks/use-parent';

export default function ChildTranscriptPage() {
  const params = useParams();
  const rawChildId = params?.childId;
  const childIdNum = Array.isArray(rawChildId)
    ? Number(rawChildId[0])
    : Number(rawChildId);
  const childId =
    Number.isFinite(childIdNum) && childIdNum > 0 ? childIdNum : undefined;

  const {
    data: transcripts,
    isLoading,
    isError,
    error,
    refetch,
  } = useChildTranscript(childId);

  if (childId === undefined) {
    return (
      <div className="mx-auto max-w-5xl p-6">
        <ErrorAlert
          title="ID không hợp lệ"
          message="Không thể xác định con cần xem học bạ."
          backHref="/parent"
          backLabel="Về trang chủ"
        />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-6">
      <header>
        <Button asChild variant="ghost" size="sm">
          <Link href="/parent">
            <ArrowLeft className="mr-1 h-4 w-4" />
            Quay lại danh sách con
          </Link>
        </Button>
        <h1 className="mt-2 flex items-center gap-2 text-3xl font-bold">
          <FileText className="h-8 w-8 text-primary" />
          Học bạ
        </h1>
        <p className="text-muted-foreground">
          Danh sách học bạ theo học kỳ, mới nhất hiển thị đầu tiên.
        </p>
      </header>

      {isLoading && (
        <div className="flex min-h-[30vh] items-center justify-center">
          <LoadingSpinner size="lg" text="Đang tải học bạ..." />
        </div>
      )}

      {isError && (
        <ErrorAlert
          title="Không tải được học bạ"
          message={
            error instanceof Error
              ? error.message
              : 'Bạn có thể không có quyền xem học bạ này (chỉ phụ huynh đã được liên kết với con mới có quyền).'
          }
          onRetry={() => refetch()}
          backHref="/parent"
          backLabel="Về trang chủ"
        />
      )}

      {!isLoading && !isError && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {(transcripts ?? []).length === 0 ? (
            <Card className="col-span-full">
              <CardHeader>
                <CardTitle>Chưa có học bạ</CardTitle>
                <CardDescription>
                  Học bạ sẽ được cập nhật khi nhà trường hoàn tất tổng kết học
                  kỳ.
                </CardDescription>
              </CardHeader>
            </Card>
          ) : (
            (transcripts ?? []).map((t) => (
              <Card
                key={t.transcriptId}
                data-testid={`transcript-card-${t.transcriptId}`}
              >
                <CardHeader>
                  <CardTitle className="text-lg">
                    {t.semester || `HK ${t.academicYear ?? '—'}`}
                  </CardTitle>
                  <CardDescription>
                    Năm học {t.academicYear ?? '—'} · {t.totalCredits} tín chỉ
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">GPA học kỳ</span>
                    <span className="font-semibold">
                      {t.semesterGpa !== null ? t.semesterGpa.toFixed(2) : '—'}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">GPA tích luỹ</span>
                    <span className="font-semibold">
                      {t.cumulativeGpa !== null
                        ? t.cumulativeGpa.toFixed(2)
                        : '—'}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Số môn</span>
                    <span>{t.totalCourses}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Đạt</span>
                    <span className="text-emerald-600">{t.passedCourses}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Không đạt</span>
                    <span className="text-destructive">{t.failedCourses}</span>
                  </div>
                </CardContent>
              </Card>
            ))
          )}
        </div>
      )}
    </div>
  );
}
