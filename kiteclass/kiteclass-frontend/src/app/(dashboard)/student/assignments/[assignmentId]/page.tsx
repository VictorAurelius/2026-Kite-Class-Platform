/**
 * Student PWA — Chi tiết bài tập + nộp bài (real-data).
 *
 * Wave rbac-lms-student-fe (GAP-1113 Increment B / PART 3): rewired from the mock
 * + fake-endpoint offline queue to the real KiteClass assignment API:
 *   - `useAssignment`       → assignment detail (title / instructions / due / score)
 *   - `useMySubmission`     → the student's existing submission (status + grade + feedback)
 *   - `useSubmitAssignment` → `POST /api/v1/assignments/submit` (X-User-Id)
 *
 * Submit is gated by `isAcceptingSubmissions`; a graded/returned submission is
 * read-only and shows the score + teacher feedback.
 *
 * @author KiteClass Team
 * @since Wave 49 Bucket C (GAP-269) mock; Wave rbac-lms-student-fe real-data
 */
'use client';

import { use, useState } from 'react';
import Link from 'next/link';
import { ArrowLeft, CheckCircle2, FileText, Send } from 'lucide-react';
import { useAuthStore } from '@/stores/auth-store';
import { useAssignment, useMySubmission, useSubmitAssignment } from '@/hooks/use-assignments';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { LoadingSpinner } from '@/components/common/loading-spinner';
import { StudentMobileShell } from '@/components/student/mobile-shell';

interface PageProps {
  params: Promise<{ assignmentId: string }>;
}

function formatDue(iso?: string | null): string {
  if (!iso) return 'Không có hạn';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

export default function StudentAssignmentDetailPage({ params }: PageProps) {
  const { assignmentId: idStr } = use(params);
  const assignmentId = Number(idStr);
  const userId = useAuthStore((s) => s.user?.id);

  const { data: assignment, isLoading } = useAssignment(assignmentId);
  const { data: submission } = useMySubmission(assignmentId, userId);
  const submit = useSubmitAssignment(userId);

  const [notes, setNotes] = useState('');
  const [contentUrl, setContentUrl] = useState('');

  const graded = submission?.status === 'GRADED' || submission?.status === 'RETURNED';
  const accepting = assignment?.isAcceptingSubmissions ?? false;
  // Can submit when: assignment accepting AND not already graded.
  const canSubmit = accepting && !graded;
  const score = submission?.adjustedScore ?? submission?.score;

  function handleSubmit() {
    if (!notes.trim() && !contentUrl.trim()) return;
    submit.mutate(
      {
        assignmentId,
        notes: notes.trim() || undefined,
        contentUrl: contentUrl.trim() || undefined,
      },
      {
        onSuccess: () => {
          setNotes('');
          setContentUrl('');
        },
      },
    );
  }

  return (
    <StudentMobileShell
      title={assignment?.title ?? 'Bài tập'}
      subtitle={assignment ? `Hạn ${formatDue(assignment.dueDate)}` : undefined}
      headerRight={
        <Link
          href="/student/assignments"
          aria-label="Quay lại danh sách bài tập"
          className="flex h-9 w-9 items-center justify-center rounded-full text-muted-foreground hover:bg-accent"
        >
          <ArrowLeft className="h-5 w-5" aria-hidden />
        </Link>
      }
    >
      {isLoading ? (
        <div className="flex justify-center py-12">
          <LoadingSpinner size="lg" />
        </div>
      ) : !assignment ? (
        <div className="rounded-lg border border-destructive/40 bg-destructive/5 p-4 text-sm text-destructive">
          Không tải được bài tập. Vui lòng thử lại.
        </div>
      ) : (
        <div className="space-y-4">
          {/* Đề bài */}
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="flex items-center gap-2 text-base">
                <FileText className="h-4 w-4" aria-hidden /> Đề bài
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-2 text-sm text-muted-foreground">
              {assignment.description ? <p>{assignment.description}</p> : null}
              {assignment.instructions ? (
                <p className="whitespace-pre-wrap">{assignment.instructions}</p>
              ) : null}
              <div className="flex flex-wrap gap-2 pt-1">
                <Badge variant="outline">Hạn {formatDue(assignment.dueDate)}</Badge>
                {assignment.maxScore != null ? (
                  <Badge variant="outline">Điểm tối đa: {assignment.maxScore}</Badge>
                ) : null}
                {assignment.weightPercent != null ? (
                  <Badge variant="outline">Trọng số: {assignment.weightPercent}%</Badge>
                ) : null}
                {assignment.isOverdue ? <Badge variant="destructive">Quá hạn</Badge> : null}
              </div>
            </CardContent>
          </Card>

          {/* Existing submission state */}
          {submission ? (
            <Card
              className={
                graded
                  ? 'border-emerald-300 bg-emerald-50 dark:border-emerald-700 dark:bg-emerald-950/30'
                  : 'border-blue-300 bg-blue-50 dark:border-blue-700 dark:bg-blue-950/30'
              }
            >
              <CardContent className="space-y-2 p-4">
                <div className="flex items-center gap-2 text-sm font-medium">
                  <CheckCircle2 className="h-4 w-4 shrink-0" aria-hidden />
                  {graded ? 'Bài đã được chấm' : 'Bạn đã nộp bài'}
                </div>
                {graded && score != null ? (
                  <p className="text-sm">
                    Điểm:{' '}
                    <span className="font-semibold">
                      {score}
                      {assignment.maxScore != null ? `/${assignment.maxScore}` : ''}
                    </span>
                  </p>
                ) : null}
                {submission.feedback ? (
                  <p className="text-sm text-muted-foreground">
                    Nhận xét: {submission.feedback}
                  </p>
                ) : null}
                {submission.notes ? (
                  <p className="text-xs text-muted-foreground">
                    Bài làm đã nộp: {submission.notes}
                  </p>
                ) : null}
              </CardContent>
            </Card>
          ) : null}

          {/* Submit form */}
          {canSubmit ? (
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-base">
                  {submission ? 'Nộp lại bài làm' : 'Bài làm của em'}
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <Textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Nhập nội dung bài làm…"
                  rows={8}
                  className="resize-y text-base"
                />
                <Input
                  value={contentUrl}
                  onChange={(e) => setContentUrl(e.target.value)}
                  placeholder="Đường dẫn tệp bài làm (tùy chọn)"
                  type="url"
                />
                <div className="flex items-center justify-between text-xs text-muted-foreground">
                  <span>{notes.length} ký tự</span>
                  <Button
                    onClick={handleSubmit}
                    disabled={submit.isPending || (!notes.trim() && !contentUrl.trim())}
                    className="min-h-[44px]"
                  >
                    <Send className="mr-1 h-4 w-4" aria-hidden />
                    {submit.isPending ? 'Đang nộp…' : 'Nộp bài'}
                  </Button>
                </div>
              </CardContent>
            </Card>
          ) : !graded ? (
            <div className="rounded-lg border border-dashed border-border p-4 text-center text-sm text-muted-foreground">
              Bài tập này hiện không nhận bài nộp.
            </div>
          ) : null}
        </div>
      )}
    </StudentMobileShell>
  );
}
