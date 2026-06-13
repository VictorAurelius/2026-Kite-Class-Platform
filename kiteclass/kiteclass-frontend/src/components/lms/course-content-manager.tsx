/**
 * Teacher LMS content authoring — the "Nội dung" surface (GAP-1113 Increment A, Bucket A).
 *
 * Consumes the kiteclass-core `/api/v1/lms` teacher endpoints to let a course owner
 * author the module → lesson → resource tree:
 *   - module CRUD + reorder (move up/down → atomic full-set reorder)
 *   - lesson CRUD + reorder + trial toggle
 *   - per-lesson resource add (by URL) + delete
 *
 * Mounted in the course-detail page (dashboard) so owner/staff/teacher authenticated
 * actors can manage content. The current user id is sent as `X-Teacher-Id` (must be
 * the course owner — server enforces BR-LMS-006/010/015).
 *
 * Drag-drop reorder (wave plan) is implemented as move-up / move-down buttons here —
 * the project has no drag-drop dependency (@dnd-kit absent), so buttons keep the build
 * dependency-free while still using the atomic full-ordered-set reorder endpoint.
 *
 * @author KiteClass Team
 * @since GAP-1113 (Wave RBAC-LMS-FE Increment A)
 */
'use client';

import { useMemo, useState } from 'react';
import {
  BookOpen,
  Plus,
  Pencil,
  Trash2,
  ChevronUp,
  ChevronDown,
  Loader2,
  Paperclip,
  FileText,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Switch } from '@/components/ui/switch';
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
import { useCourseStructure, useLmsAuthoring, useLessonForManage } from '@/hooks/use-lms';
import type {
  CourseModuleDetail,
  Lesson,
  ReorderItem,
  ResourceType,
} from '@/types/lms';

const RESOURCE_TYPES: { value: ResourceType; label: string }[] = [
  { value: 'VIDEO', label: 'Video' },
  { value: 'PDF', label: 'PDF' },
  { value: 'SLIDE', label: 'Slide' },
  { value: 'AUDIO', label: 'Âm thanh' },
  { value: 'LINK', label: 'Liên kết' },
  { value: 'CODE', label: 'Mã nguồn' },
  { value: 'OTHER', label: 'Khác' },
];

/** Build a full-ordered-set reorder payload after moving `index` by `delta` (-1/+1). */
function buildReorder<T extends { id: number }>(items: T[], index: number, delta: number): ReorderItem[] | null {
  const target = index + delta;
  if (target < 0 || target >= items.length) return null;
  const next = [...items];
  // Bounds already checked above; swap via temps (avoids noUncheckedIndexedAccess T|undefined on tuple destructure).
  const a = next[index]!;
  const b = next[target]!;
  next[index] = b;
  next[target] = a;
  return next.map((it, i) => ({ id: it.id, orderNumber: i + 1 }));
}

interface CourseContentManagerProps {
  courseId: number;
}

export function CourseContentManager({ courseId }: CourseContentManagerProps) {
  const teacherId = useAuthStore((s) => s.user?.id) ?? 0;
  const { data: modules, isLoading, error } = useCourseStructure(courseId, teacherId || undefined);
  const a = useLmsAuthoring(courseId, teacherId);

  const sortedModules = useMemo(
    () => [...(modules ?? [])].sort((m1, m2) => m1.orderNumber - m2.orderNumber),
    [modules],
  );

  // Dialog state
  const [moduleDialog, setModuleDialog] = useState<{ mode: 'add' | 'edit'; module?: CourseModuleDetail } | null>(null);
  const [lessonDialog, setLessonDialog] = useState<{ mode: 'add' | 'edit'; moduleId: number; lesson?: Lesson } | null>(null);
  const [resourceLesson, setResourceLesson] = useState<{ lessonId: number; lessonTitle: string } | null>(null);
  const [confirmDel, setConfirmDel] = useState<{ kind: 'module' | 'lesson'; id: number; label: string } | null>(null);

  const reorderModule = (index: number, delta: number) => {
    const items = buildReorder(sortedModules, index, delta);
    if (items) a.reorderModules.mutate({ items });
  };

  const reorderLesson = (moduleId: number, lessons: Lesson[], index: number, delta: number) => {
    const items = buildReorder(lessons, index, delta);
    if (items) a.reorderLessons.mutate({ moduleId, data: { items } });
  };

  if (teacherId === 0) {
    return (
      <p className="rounded-lg border border-amber-300 bg-amber-50 p-4 text-sm text-amber-900">
        Không xác định được tài khoản giáo viên. Vui lòng đăng nhập lại để quản lý nội dung.
      </p>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="flex items-center gap-2 text-xl font-semibold">
          <BookOpen className="h-5 w-5 text-primary" /> Nội dung khóa học
        </h2>
        <Button size="sm" onClick={() => setModuleDialog({ mode: 'add' })}>
          <Plus className="mr-1.5 h-4 w-4" /> Thêm chương
        </Button>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-8">
          <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
        </div>
      ) : error ? (
        <p className="rounded-lg border border-destructive/40 bg-destructive/5 p-4 text-sm text-destructive">
          Không tải được nội dung khóa học. Vui lòng thử lại.
        </p>
      ) : sortedModules.length === 0 ? (
        <p className="rounded-lg border border-dashed p-8 text-center text-sm text-muted-foreground">
          Chưa có chương nào. Bấm &quot;Thêm chương&quot; để bắt đầu xây dựng nội dung.
        </p>
      ) : (
        <div className="space-y-4">
          {sortedModules.map((m, mi) => {
            const lessons = [...(m.lessons ?? [])].sort((l1, l2) => l1.orderNumber - l2.orderNumber);
            return (
              <div key={m.id} className="rounded-lg border">
                {/* Module header */}
                <div className="flex flex-wrap items-center justify-between gap-2 border-b bg-muted/40 p-3">
                  <div className="flex items-center gap-2">
                    <div className="flex flex-col">
                      <button
                        type="button"
                        aria-label="Di chuyển chương lên"
                        disabled={mi === 0 || a.reorderModules.isPending}
                        className="text-muted-foreground hover:text-foreground disabled:opacity-30"
                        onClick={() => reorderModule(mi, -1)}
                      >
                        <ChevronUp className="h-4 w-4" />
                      </button>
                      <button
                        type="button"
                        aria-label="Di chuyển chương xuống"
                        disabled={mi === sortedModules.length - 1 || a.reorderModules.isPending}
                        className="text-muted-foreground hover:text-foreground disabled:opacity-30"
                        onClick={() => reorderModule(mi, 1)}
                      >
                        <ChevronDown className="h-4 w-4" />
                      </button>
                    </div>
                    <div>
                      <p className="font-medium">
                        Chương {mi + 1}: {m.title}
                      </p>
                      {m.description && (
                        <p className="text-sm text-muted-foreground">{m.description}</p>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <Button size="sm" variant="outline" onClick={() => setLessonDialog({ mode: 'add', moduleId: m.id })}>
                      <Plus className="mr-1 h-3.5 w-3.5" /> Bài học
                    </Button>
                    <Button size="sm" variant="ghost" aria-label="Sửa chương" onClick={() => setModuleDialog({ mode: 'edit', module: m })}>
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      aria-label="Xóa chương"
                      onClick={() => setConfirmDel({ kind: 'module', id: m.id, label: m.title })}
                    >
                      <Trash2 className="h-4 w-4 text-destructive" />
                    </Button>
                  </div>
                </div>

                {/* Lessons */}
                {lessons.length === 0 ? (
                  <p className="p-3 text-sm text-muted-foreground">Chưa có bài học trong chương này.</p>
                ) : (
                  <ul className="divide-y">
                    {lessons.map((l, li) => (
                      <li key={l.id} className="flex flex-wrap items-center justify-between gap-2 p-3">
                        <div className="flex items-center gap-2">
                          <div className="flex flex-col">
                            <button
                              type="button"
                              aria-label="Di chuyển bài học lên"
                              disabled={li === 0 || a.reorderLessons.isPending}
                              className="text-muted-foreground hover:text-foreground disabled:opacity-30"
                              onClick={() => reorderLesson(m.id, lessons, li, -1)}
                            >
                              <ChevronUp className="h-3.5 w-3.5" />
                            </button>
                            <button
                              type="button"
                              aria-label="Di chuyển bài học xuống"
                              disabled={li === lessons.length - 1 || a.reorderLessons.isPending}
                              className="text-muted-foreground hover:text-foreground disabled:opacity-30"
                              onClick={() => reorderLesson(m.id, lessons, li, 1)}
                            >
                              <ChevronDown className="h-3.5 w-3.5" />
                            </button>
                          </div>
                          <FileText className="h-4 w-4 shrink-0 text-muted-foreground" />
                          <span className="text-sm">
                            {li + 1}. {l.title}
                          </span>
                          {l.isTrial && (
                            <Badge variant="secondary" className="text-[10px]">
                              Học thử
                            </Badge>
                          )}
                          {l.estimatedDuration != null && (
                            <span className="text-xs text-muted-foreground">{l.estimatedDuration} phút</span>
                          )}
                        </div>
                        <div className="flex items-center gap-1.5">
                          <Button
                            size="sm"
                            variant="ghost"
                            aria-label="Tài nguyên bài học"
                            onClick={() => setResourceLesson({ lessonId: l.id, lessonTitle: l.title })}
                          >
                            <Paperclip className="h-4 w-4" />
                          </Button>
                          <Button
                            size="sm"
                            variant="ghost"
                            aria-label="Sửa bài học"
                            onClick={() => setLessonDialog({ mode: 'edit', moduleId: m.id, lesson: l })}
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>
                          <Button
                            size="sm"
                            variant="ghost"
                            aria-label="Xóa bài học"
                            onClick={() => setConfirmDel({ kind: 'lesson', id: l.id, label: l.title })}
                          >
                            <Trash2 className="h-4 w-4 text-destructive" />
                          </Button>
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Module add/edit dialog */}
      {moduleDialog && (
        <ModuleDialog
          mode={moduleDialog.mode}
          module={moduleDialog.module}
          nextOrder={sortedModules.length + 1}
          pending={a.createModule.isPending || a.updateModule.isPending}
          onClose={() => setModuleDialog(null)}
          onSubmit={(data) => {
            if (moduleDialog.mode === 'add') {
              a.createModule.mutate(data, { onSuccess: () => setModuleDialog(null) });
            } else if (moduleDialog.module) {
              a.updateModule.mutate(
                { moduleId: moduleDialog.module.id, data },
                { onSuccess: () => setModuleDialog(null) },
              );
            }
          }}
        />
      )}

      {/* Lesson add/edit dialog */}
      {lessonDialog && (
        <LessonDialog
          mode={lessonDialog.mode}
          lesson={lessonDialog.lesson}
          nextOrder={
            (sortedModules.find((m) => m.id === lessonDialog.moduleId)?.lessons?.length ?? 0) + 1
          }
          pending={a.createLesson.isPending || a.updateLesson.isPending}
          onClose={() => setLessonDialog(null)}
          onSubmit={(data) => {
            if (lessonDialog.mode === 'add') {
              a.createLesson.mutate(
                { moduleId: lessonDialog.moduleId, data },
                { onSuccess: () => setLessonDialog(null) },
              );
            } else if (lessonDialog.lesson) {
              a.updateLesson.mutate(
                { lessonId: lessonDialog.lesson.id, data },
                { onSuccess: () => setLessonDialog(null) },
              );
            }
          }}
        />
      )}

      {/* Resource manager dialog */}
      {resourceLesson && (
        <ResourceDialog
          lessonId={resourceLesson.lessonId}
          lessonTitle={resourceLesson.lessonTitle}
          teacherId={teacherId}
          addPending={a.createResource.isPending}
          delPending={a.deleteResource.isPending}
          onClose={() => setResourceLesson(null)}
          onAdd={(data, done) =>
            a.createResource.mutate({ lessonId: resourceLesson.lessonId, data }, { onSuccess: done })
          }
          onDelete={(resourceId, done) => a.deleteResource.mutate(resourceId, { onSuccess: done })}
        />
      )}

      {/* Delete confirm */}
      <ConfirmDialog
        open={!!confirmDel}
        onOpenChange={(open) => !open && setConfirmDel(null)}
        onConfirm={() => {
          if (!confirmDel) return;
          if (confirmDel.kind === 'module') {
            a.deleteModule.mutate(confirmDel.id, { onSuccess: () => setConfirmDel(null) });
          } else {
            a.deleteLesson.mutate(confirmDel.id, { onSuccess: () => setConfirmDel(null) });
          }
        }}
        title={confirmDel?.kind === 'module' ? 'Xóa chương' : 'Xóa bài học'}
        description={
          confirmDel
            ? `Bạn có chắc muốn xóa "${confirmDel.label}"? Hành động này không thể hoàn tác.`
            : ''
        }
        confirmText="Xóa"
        variant="destructive"
      />
    </div>
  );
}

// ---------- Module dialog ----------

function ModuleDialog({
  mode,
  module,
  nextOrder,
  pending,
  onClose,
  onSubmit,
}: {
  mode: 'add' | 'edit';
  module?: CourseModuleDetail;
  nextOrder: number;
  pending: boolean;
  onClose: () => void;
  onSubmit: (data: { title: string; description?: string; orderNumber: number }) => void;
}) {
  const [title, setTitle] = useState(module?.title ?? '');
  const [description, setDescription] = useState(module?.description ?? '');
  const [err, setErr] = useState<string | null>(null);

  const submit = () => {
    if (!title.trim()) {
      setErr('Vui lòng nhập tên chương.');
      return;
    }
    onSubmit({
      title: title.trim(),
      description: description.trim() || undefined,
      orderNumber: module?.orderNumber ?? nextOrder,
    });
  };

  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{mode === 'add' ? 'Thêm chương mới' : 'Sửa chương'}</DialogTitle>
          <DialogDescription>Chương nhóm các bài học theo chủ đề.</DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="module-title">Tên chương</Label>
            <Input id="module-title" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="VD: Chương 1 — Nhập môn" />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="module-desc">Mô tả (tùy chọn)</Label>
            <Textarea id="module-desc" value={description} onChange={(e) => setDescription(e.target.value)} rows={3} />
          </div>
          {err && <p className="text-sm text-destructive">{err}</p>}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Hủy</Button>
          <Button onClick={submit} disabled={pending}>
            {pending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {mode === 'add' ? 'Thêm' : 'Lưu'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

// ---------- Lesson dialog ----------

function LessonDialog({
  mode,
  lesson,
  nextOrder,
  pending,
  onClose,
  onSubmit,
}: {
  mode: 'add' | 'edit';
  lesson?: Lesson;
  nextOrder: number;
  pending: boolean;
  onClose: () => void;
  onSubmit: (data: {
    title: string;
    content?: string;
    videoUrl?: string;
    isTrial?: boolean;
    orderNumber: number;
    estimatedDuration?: number;
  }) => void;
}) {
  const [title, setTitle] = useState(lesson?.title ?? '');
  const [content, setContent] = useState(lesson?.content ?? '');
  const [videoUrl, setVideoUrl] = useState(lesson?.videoUrl ?? '');
  const [isTrial, setIsTrial] = useState(lesson?.isTrial ?? false);
  const [duration, setDuration] = useState(lesson?.estimatedDuration != null ? String(lesson.estimatedDuration) : '');
  const [err, setErr] = useState<string | null>(null);

  const submit = () => {
    if (!title.trim()) {
      setErr('Vui lòng nhập tên bài học.');
      return;
    }
    const dur = duration.trim() ? Number(duration.trim()) : undefined;
    if (dur != null && (Number.isNaN(dur) || dur < 1)) {
      setErr('Thời lượng phải là số phút hợp lệ.');
      return;
    }
    onSubmit({
      title: title.trim(),
      content: content.trim() || undefined,
      videoUrl: videoUrl.trim() || undefined,
      isTrial,
      orderNumber: lesson?.orderNumber ?? nextOrder,
      estimatedDuration: dur,
    });
  };

  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{mode === 'add' ? 'Thêm bài học' : 'Sửa bài học'}</DialogTitle>
          <DialogDescription>Nội dung bài học cho học viên.</DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="lesson-title">Tên bài học</Label>
            <Input id="lesson-title" value={title} onChange={(e) => setTitle(e.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="lesson-video">Video URL (tùy chọn — YouTube/Vimeo)</Label>
            <Input id="lesson-video" value={videoUrl} onChange={(e) => setVideoUrl(e.target.value)} placeholder="https://youtube.com/watch?v=..." />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="lesson-content">Nội dung (markdown, tùy chọn)</Label>
            <Textarea id="lesson-content" value={content} onChange={(e) => setContent(e.target.value)} rows={5} />
          </div>
          <div className="flex items-center gap-6">
            <div className="space-y-1.5">
              <Label htmlFor="lesson-duration">Thời lượng (phút)</Label>
              <Input id="lesson-duration" inputMode="numeric" value={duration} onChange={(e) => setDuration(e.target.value)} className="w-28" />
            </div>
            <div className="flex items-center gap-2 pt-5">
              <Switch id="lesson-trial" checked={isTrial} onCheckedChange={setIsTrial} />
              <Label htmlFor="lesson-trial">Cho học thử miễn phí</Label>
            </div>
          </div>
          {err && <p className="text-sm text-destructive">{err}</p>}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Hủy</Button>
          <Button onClick={submit} disabled={pending}>
            {pending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {mode === 'add' ? 'Thêm' : 'Lưu'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

// ---------- Resource dialog ----------

function ResourceDialog({
  lessonId,
  lessonTitle,
  teacherId,
  addPending,
  delPending,
  onClose,
  onAdd,
  onDelete,
}: {
  lessonId: number;
  lessonTitle: string;
  teacherId: number;
  addPending: boolean;
  delPending: boolean;
  onClose: () => void;
  onAdd: (data: { type: ResourceType; url: string; title: string }, done: () => void) => void;
  onDelete: (resourceId: number, done: () => void) => void;
}) {
  // Load the lesson's full detail (resources) via react-query; refetch after mutate.
  const { data: detail, isLoading, isError, refetch } = useLessonForManage(lessonId, teacherId);
  const resources = detail?.resources ?? [];
  const [type, setType] = useState<ResourceType>('LINK');
  const [url, setUrl] = useState('');
  const [title, setTitle] = useState('');
  const [err, setErr] = useState<string | null>(null);

  const submitAdd = () => {
    setErr(null);
    if (!title.trim() || !url.trim()) {
      setErr('Vui lòng nhập tiêu đề và URL tài nguyên.');
      return;
    }
    onAdd({ type, title: title.trim(), url: url.trim() }, () => {
      setTitle('');
      setUrl('');
      void refetch();
    });
  };

  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Tài nguyên — {lessonTitle}</DialogTitle>
          <DialogDescription>Thêm liên kết tài liệu (PDF, slide, video, mã nguồn...).</DialogDescription>
        </DialogHeader>

        {/* Existing resources */}
        <div className="space-y-2">
          {isError ? (
            <p className="text-sm text-destructive">Không tải được danh sách tài nguyên.</p>
          ) : isLoading ? (
            <div className="flex justify-center py-4">
              <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
            </div>
          ) : resources.length === 0 ? (
            <p className="text-sm text-muted-foreground">Chưa có tài nguyên nào.</p>
          ) : (
            <ul className="divide-y rounded-md border">
              {resources.map((r) => (
                <li key={r.id} className="flex items-center justify-between gap-2 p-2.5">
                  <div className="flex items-center gap-2 truncate">
                    <Badge variant="outline" className="shrink-0">{r.type}</Badge>
                    <a href={r.url} target="_blank" rel="noopener noreferrer" className="truncate text-sm text-primary hover:underline">
                      {r.title}
                    </a>
                  </div>
                  <Button
                    size="sm"
                    variant="ghost"
                    aria-label="Xóa tài nguyên"
                    disabled={delPending}
                    onClick={() => onDelete(r.id, () => void refetch())}
                  >
                    <Trash2 className="h-4 w-4 text-destructive" />
                  </Button>
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* Add resource form */}
        <div className="space-y-3 border-t pt-4">
          <p className="text-sm font-medium">Thêm tài nguyên</p>
          <div className="flex gap-2">
            <select
              aria-label="Loại tài nguyên"
              className="h-10 rounded-md border border-input bg-background px-3 text-sm"
              value={type}
              onChange={(e) => setType(e.target.value as ResourceType)}
            >
              {RESOURCE_TYPES.map((rt) => (
                <option key={rt.value} value={rt.value}>{rt.label}</option>
              ))}
            </select>
            <Input placeholder="Tiêu đề" value={title} onChange={(e) => setTitle(e.target.value)} />
          </div>
          <Input placeholder="https://..." value={url} onChange={(e) => setUrl(e.target.value)} />
          {err && <p className="text-sm text-destructive">{err}</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Đóng</Button>
          <Button onClick={submitAdd} disabled={addPending}>
            {addPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Thêm tài nguyên
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
