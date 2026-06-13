/**
 * Teacher assignment give/grade surface (GAP-1113 Increment A, Bucket D).
 *
 * Per-class assignment management: pick course → class → create / publish / close
 * assignments + view submissions + grade. Mounted in the teacher-shell `(teacher)/*`
 * route group (TEACHER RoleGuard). Backend `/api/v1/assignments` (give/grade =
 * X-Teacher-Id = current user). Student submit is gated KC-9 (not built here).
 *
 * @author KiteClass Team
 * @since GAP-1113 (Wave RBAC-LMS-FE Increment A)
 */
'use client';

import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ClipboardList, Plus, Send, Lock, Trash2, Loader2, FileCheck2 } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { useAuthStore } from '@/stores/auth-store';
import { useCourses } from '@/hooks/use-courses';
import { classesApi } from '@/lib/api/classes';
import {
  useClassAssignments,
  useAssignmentSubmissions,
  useAssignmentMutations,
} from '@/hooks/use-assignments';
import type { Assignment, CreateAssignmentRequest } from '@/types/assignment';

const STATUS_LABEL: Record<string, { label: string; variant: 'default' | 'secondary' | 'outline' }> = {
  DRAFT: { label: 'Bản nháp', variant: 'secondary' },
  PUBLISHED: { label: 'Đã giao', variant: 'default' },
};

const SUB_STATUS_LABEL: Record<string, string> = {
  PENDING: 'Chờ chấm',
  GRADED: 'Đã chấm',
  RETURNED: 'Đã trả',
};

export default function TeacherAssignmentsPage() {
  const teacherId = useAuthStore((s) => s.user?.id) ?? 0;

  const { data: coursesPage } = useCourses({ page: 0, size: 100 });
  const courses = coursesPage?.content ?? [];

  const [courseId, setCourseId] = useState<number | null>(null);
  const [classId, setClassId] = useState<number | null>(null);

  const { data: classesPage, isLoading: classesLoading } = useQuery({
    queryKey: ['classes', 'by-course', courseId],
    queryFn: () => classesApi.getByCourse(courseId as number, { size: 100 }),
    enabled: !!courseId,
  });
  const classes = useMemo(() => classesPage?.content ?? [], [classesPage]);

  const { data: assignments, isLoading: assignmentsLoading } = useClassAssignments(classId);
  const m = useAssignmentMutations(classId, teacherId);

  const [createOpen, setCreateOpen] = useState(false);
  const [submissionsFor, setSubmissionsFor] = useState<Assignment | null>(null);
  const [confirmDel, setConfirmDel] = useState<Assignment | null>(null);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="flex items-center gap-2 text-2xl font-bold">
          <ClipboardList className="h-6 w-6 text-primary" /> Bài tập
        </h1>
        <p className="text-muted-foreground">Giao bài, theo dõi bài nộp và chấm điểm theo lớp.</p>
      </div>

      {/* Course + class selector */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Chọn lớp</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4 sm:flex-row">
          <div className="flex-1 space-y-1.5">
            <Label htmlFor="course-sel">Khóa học</Label>
            <select
              id="course-sel"
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
              value={courseId ?? ''}
              onChange={(e) => {
                const v = e.target.value ? Number(e.target.value) : null;
                setCourseId(v);
                setClassId(null);
              }}
            >
              <option value="">— Chọn khóa học —</option>
              {courses.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>
          <div className="flex-1 space-y-1.5">
            <Label htmlFor="class-sel">Lớp</Label>
            <select
              id="class-sel"
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 text-sm disabled:opacity-50"
              value={classId ?? ''}
              disabled={!courseId || classesLoading}
              onChange={(e) => setClassId(e.target.value ? Number(e.target.value) : null)}
            >
              <option value="">{classesLoading ? 'Đang tải...' : '— Chọn lớp —'}</option>
              {classes.map((cl) => (
                <option key={cl.id} value={cl.id}>{cl.name}</option>
              ))}
            </select>
          </div>
        </CardContent>
      </Card>

      {/* Assignment list */}
      {classId && (
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="text-base">Danh sách bài tập</CardTitle>
              <Button size="sm" onClick={() => setCreateOpen(true)}>
                <Plus className="mr-1.5 h-4 w-4" /> Giao bài tập
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            {assignmentsLoading ? (
              <div className="flex justify-center py-6">
                <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
              </div>
            ) : (assignments ?? []).length === 0 ? (
              <p className="py-6 text-center text-sm text-muted-foreground">
                Chưa có bài tập nào cho lớp này.
              </p>
            ) : (
              <div className="space-y-3">
                {(assignments ?? []).map((as) => {
                  const st = STATUS_LABEL[as.status] ?? { label: as.status, variant: 'outline' as const };
                  return (
                    <div key={as.id} className="flex flex-wrap items-center justify-between gap-2 rounded-lg border p-3">
                      <div>
                        <div className="flex items-center gap-2">
                          <p className="font-medium">{as.title}</p>
                          <Badge variant={st.variant}>{st.label}</Badge>
                        </div>
                        <p className="text-sm text-muted-foreground">
                          {as.dueDate ? `Hạn nộp: ${new Date(as.dueDate).toLocaleString('vi-VN')}` : 'Không có hạn nộp'}
                          {as.maxScore != null && ` · Điểm tối đa: ${as.maxScore}`}
                        </p>
                      </div>
                      <div className="flex items-center gap-1.5">
                        <Button size="sm" variant="ghost" onClick={() => setSubmissionsFor(as)}>
                          <FileCheck2 className="mr-1 h-4 w-4" /> Bài nộp
                        </Button>
                        {as.status === 'DRAFT' && (
                          <Button size="sm" variant="outline" disabled={m.publish.isPending} onClick={() => m.publish.mutate(as.id)}>
                            <Send className="mr-1 h-4 w-4" /> Giao
                          </Button>
                        )}
                        {as.status === 'PUBLISHED' && (
                          <Button size="sm" variant="outline" disabled={m.close.isPending} onClick={() => m.close.mutate(as.id)}>
                            <Lock className="mr-1 h-4 w-4" /> Đóng
                          </Button>
                        )}
                        <Button size="sm" variant="ghost" aria-label="Xóa bài tập" onClick={() => setConfirmDel(as)}>
                          <Trash2 className="h-4 w-4 text-destructive" />
                        </Button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Create dialog */}
      {createOpen && classId && (
        <CreateAssignmentDialog
          classId={classId}
          pending={m.create.isPending}
          onClose={() => setCreateOpen(false)}
          onSubmit={(data) => m.create.mutate(data, { onSuccess: () => setCreateOpen(false) })}
        />
      )}

      {/* Submissions + grade dialog */}
      {submissionsFor && (
        <SubmissionsDialog
          assignment={submissionsFor}
          gradePending={m.grade.isPending}
          returnPending={m.returnGraded.isPending}
          onClose={() => setSubmissionsFor(null)}
          onGrade={(submissionId, score, feedback, done) =>
            m.grade.mutate({ submissionId, data: { score, feedback } }, { onSuccess: done })
          }
          onReturn={(submissionId, done) => m.returnGraded.mutate(submissionId, { onSuccess: done })}
        />
      )}

      {/* Delete confirm */}
      <ConfirmDialog
        open={!!confirmDel}
        onOpenChange={(open) => !open && setConfirmDel(null)}
        onConfirm={() => {
          if (confirmDel) m.remove.mutate(confirmDel.id, { onSuccess: () => setConfirmDel(null) });
        }}
        title="Xóa bài tập"
        description={confirmDel ? `Xóa bài tập "${confirmDel.title}"? Hành động này không thể hoàn tác.` : ''}
        confirmText="Xóa"
        variant="destructive"
      />
    </div>
  );
}

// ---------- Create assignment dialog ----------

function CreateAssignmentDialog({
  classId,
  pending,
  onClose,
  onSubmit,
}: {
  classId: number;
  pending: boolean;
  onClose: () => void;
  onSubmit: (data: CreateAssignmentRequest) => void;
}) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [instructions, setInstructions] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [maxScore, setMaxScore] = useState('10');
  const [err, setErr] = useState<string | null>(null);

  const submit = () => {
    setErr(null);
    if (!title.trim()) {
      setErr('Vui lòng nhập tiêu đề bài tập.');
      return;
    }
    const score = maxScore.trim() ? Number(maxScore.trim()) : undefined;
    if (score != null && (Number.isNaN(score) || score <= 0)) {
      setErr('Điểm tối đa phải là số dương.');
      return;
    }
    onSubmit({
      classId,
      title: title.trim(),
      description: description.trim() || undefined,
      instructions: instructions.trim() || undefined,
      dueDate: dueDate || undefined,
      maxScore: score,
    });
  };

  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Giao bài tập mới</DialogTitle>
          <DialogDescription>Bài tập sẽ ở trạng thái nháp cho đến khi bạn bấm &quot;Giao&quot;.</DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="as-title">Tiêu đề</Label>
            <Input id="as-title" value={title} onChange={(e) => setTitle(e.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="as-desc">Mô tả (tùy chọn)</Label>
            <Textarea id="as-desc" rows={2} value={description} onChange={(e) => setDescription(e.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="as-instr">Hướng dẫn làm bài (tùy chọn)</Label>
            <Textarea id="as-instr" rows={3} value={instructions} onChange={(e) => setInstructions(e.target.value)} />
          </div>
          <div className="flex gap-4">
            <div className="flex-1 space-y-1.5">
              <Label htmlFor="as-due">Hạn nộp (tùy chọn)</Label>
              <Input id="as-due" type="datetime-local" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
            </div>
            <div className="w-32 space-y-1.5">
              <Label htmlFor="as-score">Điểm tối đa</Label>
              <Input id="as-score" inputMode="decimal" value={maxScore} onChange={(e) => setMaxScore(e.target.value)} />
            </div>
          </div>
          {err && <p className="text-sm text-destructive">{err}</p>}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Hủy</Button>
          <Button onClick={submit} disabled={pending}>
            {pending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Tạo bài tập
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

// ---------- Submissions + grade dialog ----------

function SubmissionsDialog({
  assignment,
  gradePending,
  returnPending,
  onClose,
  onGrade,
  onReturn,
}: {
  assignment: Assignment;
  gradePending: boolean;
  returnPending: boolean;
  onClose: () => void;
  onGrade: (submissionId: number, score: number, feedback: string | undefined, done: () => void) => void;
  onReturn: (submissionId: number, done: () => void) => void;
}) {
  const { data: submissions, isLoading, isError, refetch } = useAssignmentSubmissions(assignment.id);
  const [gradingId, setGradingId] = useState<number | null>(null);
  const [score, setScore] = useState('');
  const [feedback, setFeedback] = useState('');
  const [err, setErr] = useState<string | null>(null);

  const startGrade = (submissionId: number, current?: number | null) => {
    setGradingId(submissionId);
    setScore(current != null ? String(current) : '');
    setFeedback('');
    setErr(null);
  };

  const submitGrade = () => {
    setErr(null);
    const s = Number(score.trim());
    if (!score.trim() || Number.isNaN(s) || s < 0) {
      setErr('Điểm phải là số ≥ 0.');
      return;
    }
    if (assignment.maxScore != null && s > assignment.maxScore) {
      setErr(`Điểm không được vượt quá ${assignment.maxScore}.`);
      return;
    }
    if (gradingId != null) {
      onGrade(gradingId, s, feedback.trim() || undefined, () => {
        setGradingId(null);
        void refetch();
      });
    }
  };

  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Bài nộp — {assignment.title}</DialogTitle>
          <DialogDescription>
            Điểm tối đa: {assignment.maxScore ?? '—'}. Chấm điểm rồi trả bài cho học viên.
          </DialogDescription>
        </DialogHeader>

        {isError ? (
          <p className="text-sm text-destructive">Không tải được danh sách bài nộp.</p>
        ) : isLoading ? (
          <div className="flex justify-center py-6">
            <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
          </div>
        ) : (submissions ?? []).length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">Chưa có học viên nào nộp bài.</p>
        ) : (
          <ul className="divide-y rounded-md border">
            {(submissions ?? []).map((sub) => (
              <li key={sub.id} className="space-y-2 p-3">
                <div className="flex items-center justify-between gap-2">
                  <div>
                    <p className="text-sm font-medium">Học viên #{sub.studentId}</p>
                    <p className="text-xs text-muted-foreground">
                      {SUB_STATUS_LABEL[sub.status] ?? sub.status}
                      {sub.isLate && ' · Nộp muộn'}
                      {sub.score != null && ` · Điểm: ${sub.adjustedScore ?? sub.score}`}
                      {sub.submissionDate && ` · ${new Date(sub.submissionDate).toLocaleString('vi-VN')}`}
                    </p>
                    {sub.contentUrl && (
                      <a href={sub.contentUrl} target="_blank" rel="noopener noreferrer" className="text-xs text-primary hover:underline">
                        Xem bài làm
                      </a>
                    )}
                  </div>
                  <div className="flex items-center gap-1.5">
                    <Button size="sm" variant="outline" onClick={() => startGrade(sub.id, sub.score)}>
                      Chấm
                    </Button>
                    {sub.status === 'GRADED' && (
                      <Button size="sm" variant="ghost" disabled={returnPending} onClick={() => onReturn(sub.id, () => void refetch())}>
                        Trả bài
                      </Button>
                    )}
                  </div>
                </div>

                {gradingId === sub.id && (
                  <div className="space-y-2 rounded-md bg-muted/40 p-3">
                    <div className="flex gap-2">
                      <Input
                        className="w-28"
                        inputMode="decimal"
                        placeholder="Điểm"
                        value={score}
                        onChange={(e) => setScore(e.target.value)}
                      />
                      <Input
                        placeholder="Nhận xét (tùy chọn)"
                        value={feedback}
                        onChange={(e) => setFeedback(e.target.value)}
                      />
                    </div>
                    {err && <p className="text-sm text-destructive">{err}</p>}
                    <div className="flex gap-2">
                      <Button size="sm" onClick={submitGrade} disabled={gradePending}>
                        {gradePending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                        Lưu điểm
                      </Button>
                      <Button size="sm" variant="ghost" onClick={() => setGradingId(null)}>Hủy</Button>
                    </div>
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Đóng</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
